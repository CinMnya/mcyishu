package com.chenchen.yishu.fengshui;

import com.chenchen.yishu.core.StableKey;
import com.chenchen.yishu.core.YishuRandom;
import com.chenchen.yishu.dimension.DimensionProfile;
import com.chenchen.yishu.fate.GanZhi.WuXing;

/**
 * 易枢 · 风水域场。
 *
 * <p>风水域<b>不按区块划分</b>。区块是 16×16 的正方形，太规整了 —— 真实的地理
 * 从不是方块拼出来的。所以这里用的是<b>确定性 Voronoi + 噪声扭曲</b>：
 *
 * <ol>
 *   <li>把世界切成一格 {@value #CELL_SIZE} 方块的隐形晶格；</li>
 *   <li>每一格放一个「脉眼」，位置由哈希在该格内抖动（所以不是网格，是不规则点阵）；</li>
 *   <li>查询某点属于哪个域时，先把这个点用<b>分形噪声</b>推歪几十格，
 *       再去找最近的脉眼 —— 这一步让域界变成蜿蜒的曲线，而不是规整的直线段；</li>
 *   <li>域的属性（气、煞、阴、阳、五行偏向）由脉眼坐标决定。</li>
 * </ol>
 *
 * <p>全部输入只有 {@code worldSeed + 坐标}。这意味着：
 * <b>你可以把某个位置的坐标发给朋友，他在同一个种子的世界里走过去，
 * 看到的会是完全一样的风水。</b>这是「可分享」这条原则在风水系统上的兑现。
 */
public final class FengShuiField {

    private FengShuiField() {
    }

    /** Voronoi 晶格边长（方块）。 */
    public static final int CELL_SIZE = 192;
    /** 噪声扭曲的最大偏移（方块）。 */
    public static final double WARP_STRENGTH = 46.0D;

    // ==================================================================== 采样

    /**
     * 一个风水采样点。
     *
     * @param domainX   所属域的脉眼 X（<b>不是</b>查询点）
     * @param domainZ   所属域的脉眼 Z
     * @param qi        气（0~100，越高越好）
     * @param sha       煞（0~100，越高越坏）
     * @param yin       阴（0~100）
     * @param yang      阳（0~100）
     * @param element   五行偏向
     * @param elementStrength 偏向强度（0~1，越高则这一行越纯）
     */
    public record Sample(int domainX, int domainZ, int qi, int sha, int yin, int yang,
                         WuXing element, double elementStrength) {

        /** 阴阳净度：0 = 完全失衡，50 = 完全平衡。 */
        public int balance() {
            return 50 - Math.abs(this.yin - this.yang) / 2;
        }

        /** 综合评分，0~100。 */
        public int score(DimensionProfile dimension) {
            double raw = this.qi * 1.05D
                    - this.sha * 1.15D
                    + balance() * 0.45D
                    + this.yang * 0.12D;
            if (dimension == DimensionProfile.NETHER) {
                // 下界本身就煞，评分整体下压，但仍保留相对高低
                raw -= 18.0D;
            } else if (dimension == DimensionProfile.END) {
                raw = this.qi * 0.6D - this.sha * 0.8D + balance() * 0.3D;
            }
            return (int) Math.max(0.0D, Math.min(100.0D, Math.round(raw)));
        }

        /** 吉凶等级（五档）。 */
        public Grade grade(DimensionProfile dimension) {
            int value = score(dimension);
            if (value >= 78) {
                return Grade.GREAT_LUCK;
            }
            if (value >= 58) {
                return Grade.LUCK;
            }
            if (value >= 34) {
                return Grade.PLAIN;
            }
            if (value >= 18) {
                return Grade.BAD;
            }
            return Grade.DIRE;
        }

        /** 域眼坐标的显示串，界面上用来当「地块编号」。 */
        public String domainLabel() {
            return "脉眼 " + this.domainX + ", " + this.domainZ;
        }
    }

    /** 吉凶五档。 */
    public enum Grade {
        GREAT_LUCK("大吉", 0xFF5BE3A0, "作物加速，刷怪减少，交易得让价，幸运常伴"),
        LUCK("吉", 0xFF8FD46A, "顺遂之地，诸事稍有加成"),
        PLAIN("平", 0xFFD6D6A0, "无甚吉凶，寻常所在"),
        BAD("凶", 0xFFD99A5B, "作物减产，夜色之中易生阴煞"),
        DIRE("大凶", 0xFFD9575B, "煞气盘结，久留必伤，夜有异动");

        private final String zh;
        private final int color;
        private final String effect;

        Grade(String zh, int color, String effect) {
            this.zh = zh;
            this.color = color;
            this.effect = effect;
        }

        public String zh() {
            return this.zh;
        }

        public int color() {
            return this.color;
        }

        public String effect() {
            return this.effect;
        }
    }

    // ==================================================================== 主查询

    /**
     * 查询某个坐标的风水。
     *
     * @param worldSeed 世界种子（唯一随机源）
     * @param dimension 维度
     * @param x         方块 X（浮点，允许亚格精度）
     * @param z         方块 Z
     */
    public static Sample sample(long worldSeed, DimensionProfile dimension, double x, double z) {
        String dim = dimension.shortKey();

        // ---- 1. 噪声扭曲：把查询点推歪 ----
        double warpX = fbm(worldSeed, dim, x / 128.0D, z / 128.0D, 3) * WARP_STRENGTH * 2.0D - WARP_STRENGTH;
        double warpZ = fbm(worldSeed, dim, x / 128.0D + 91.7D, z / 128.0D - 47.3D, 3) * WARP_STRENGTH * 2.0D - WARP_STRENGTH;
        double wx = x + warpX;
        double wz = z + warpZ;

        // ---- 2. 在 3x3 晶格邻域里找最近的脉眼 ----
        int baseX = (int) Math.floor(wx / CELL_SIZE);
        int baseZ = (int) Math.floor(wz / CELL_SIZE);

        double bestDistance = Double.MAX_VALUE;
        int bestX = 0;
        int bestZ = 0;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                int cellX = baseX + dx;
                int cellZ = baseZ + dz;
                YishuRandom jitter = StableKey.of(worldSeed, "fs:seed")
                        .add(dim).add(cellX).add(cellZ).random();
                // 脉眼在该格内抖动，但留出 15% 边距，避免两点贴得过近形成尖角
                double px = (cellX + 0.15D + jitter.nextDouble() * 0.70D) * CELL_SIZE;
                double pz = (cellZ + 0.15D + jitter.nextDouble() * 0.70D) * CELL_SIZE;
                double distance = (px - wx) * (px - wx) + (pz - wz) * (pz - wz);
                if (distance < bestDistance) {
                    bestDistance = distance;
                    bestX = (int) Math.round(px);
                    bestZ = (int) Math.round(pz);
                }
            }
        }

        // ---- 3. 由脉眼推属性 ----
        YishuRandom attr = StableKey.of(worldSeed, "fs:attr")
                .add(dim).add(bestX).add(bestZ).random();

        int qi = attr.nextInt(5, 96);
        int sha = attr.nextInt(2, 88);
        int yang = attr.nextInt(8, 92);
        int yin = attr.nextInt(8, 92);

        // 下界：煞气整体上调，阳气下调；末地：阴阳都弱，煞偏阴
        switch (dimension) {
            case NETHER -> {
                sha = Math.min(100, sha + 22);
                yang = Math.max(2, yang - 26);
                yin = Math.min(100, yin + 14);
            }
            case END -> {
                qi = Math.max(2, qi - 16);
                yin = Math.min(100, yin + 20);
                yang = Math.max(2, yang - 24);
            }
            default -> {
            }
        }

        WuXing element = WuXing.byIndex(attr.nextInt(5));
        double strength = 0.35D + attr.nextDouble() * 0.60D;

        return new Sample(bestX, bestZ, qi, sha, yin, yang, element, strength);
    }

    /** 便捷重载：整数坐标。 */
    public static Sample sample(long worldSeed, DimensionProfile dimension, int x, int z) {
        return sample(worldSeed, dimension, x + 0.5D, z + 0.5D);
    }

    /** 这个坐标是否处在两个风水域的「交界线上」（用于寻找龙脉与特殊地貌）。 */
    public static boolean nearBoundary(long worldSeed, DimensionProfile dimension, int x, int z) {
        Sample here = sample(worldSeed, dimension, x, z);
        Sample east = sample(worldSeed, dimension, x + 24, z);
        Sample south = sample(worldSeed, dimension, x, z + 24);
        return here.domainX() != east.domainX() || here.domainZ() != east.domainZ()
                || here.domainX() != south.domainX() || here.domainZ() != south.domainZ();
    }

    // ==================================================================== 噪声

    /**
     * 二维值噪声。
     *
     * <p>用四次哈希 + 平滑插值（smoothstep），足够让域界显得自然，
     * 又不需要引入任何外部依赖。刻意不用 Perlin —— 值噪声更短、更容易核对。
     */
    private static double valueNoise(long worldSeed, String dimension, double x, double z) {
        int x0 = (int) Math.floor(x);
        int z0 = (int) Math.floor(z);
        double fx = x - x0;
        double fz = z - z0;
        double sx = fx * fx * (3.0D - 2.0D * fx);
        double sz = fz * fz * (3.0D - 2.0D * fz);

        double n00 = hashUnit(worldSeed, dimension, x0, z0);
        double n10 = hashUnit(worldSeed, dimension, x0 + 1, z0);
        double n01 = hashUnit(worldSeed, dimension, x0, z0 + 1);
        double n11 = hashUnit(worldSeed, dimension, x0 + 1, z0 + 1);

        double top = n00 + (n10 - n00) * sx;
        double bottom = n01 + (n11 - n01) * sx;
        return top + (bottom - top) * sz;
    }

    /** 分形叠加（fBm）：多八度值噪声，让扭曲既有大尺度也有细节。 */
    private static double fbm(long worldSeed, String dimension, double x, double z, int octaves) {
        double sum = 0.0D;
        double amplitude = 0.5D;
        double frequency = 1.0D;
        double norm = 0.0D;
        for (int i = 0; i < octaves; i++) {
            sum += valueNoise(worldSeed, dimension, x * frequency, z * frequency) * amplitude;
            norm += amplitude;
            amplitude *= 0.5D;
            frequency *= 2.0D;
        }
        return norm <= 0.0D ? 0.5D : sum / norm;
    }

    private static double hashUnit(long worldSeed, String dimension, int x, int z) {
        long bits = StableKey.of(worldSeed, "fs:noise").add(dimension).add(x).add(z).hash();
        return (bits >>> 11) * 0x1.0p-53;
    }
}
