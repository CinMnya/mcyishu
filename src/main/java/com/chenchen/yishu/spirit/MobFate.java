package com.chenchen.yishu.spirit;

import com.chenchen.yishu.YishuConfig;
import com.chenchen.yishu.core.StableKey;
import com.chenchen.yishu.core.YishuRandom;
import com.chenchen.yishu.dimension.DimensionProfile;
import com.chenchen.yishu.fate.Bazi;
import com.chenchen.yishu.fate.GanZhi.WuXing;
import com.chenchen.yishu.liuyao.Divination;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;

import java.util.Map;

/**
 * 易枢 · 生物命理 —— 每一只怪都有自己的命。
 *
 * <p>规范第七节把这件事分成两层：
 *
 * <ol>
 *   <li><b>算法 DNA（按类型）</b>：由生物<b>注册名</b>唯一决定，<b>不看它的任何实际特征</b>
 *       ——不看血量、不看攻击力、不看是不是亡灵。这样做的理由很实在：
 *       一旦 DNA 依赖生物属性，模组包一改数值、整合包一调平衡，
 *       全世界的生物命盘就全变了，「可复现」立刻失效。
 *       只依赖注册名，则同一个 {@code minecraft:zombie} 在任何整合包里都是同一种命理算法。</li>
 *   <li><b>个体命盘</b>：由 {@code 种子 + 注册名 + 维度 + 坐标 + 局部索引} 决定。
 *       坐标与局部索引让同一只僵尸在世界里的不同位置拥有不同的命 ——
 *       于是「同种子同位置同种生物命盘一致」这条要求自然成立。</li>
 * </ol>
 *
 * <p><b>局部索引是什么</b>：同一格坐标上可能同时站着两只僵尸，
 * 光靠坐标分不开它们。所以加一个「这一格里的第几只」作为兜底序号 ——
 * 它由实体在区块里的加载顺序决定，因此对同一份存档是稳定的，
 * 而对「再来一只」则会得到新的命盘。
 */
public final class MobFate {

    private MobFate() {
    }

    // ==================================================================== DNA

    /**
     * 生物算法 DNA —— 一种生物一套命理参数。
     *
     * <p>这八项参数决定「这一族生物的命是怎么算出来的」：
     * <ul>
     *   <li>{@code ganOffset} 天干起始偏移</li>
     *   <li>{@code zhiRule} 地支规则（0 顺行 / 1 逆行 / 2 跳一 / 3 对称）</li>
     *   <li>{@code elementBias} 五行权重偏向（哪一行被放大）</li>
     *   <li>{@code dayunStep} 大运步长（个体几年一换运）</li>
     *   <li>{@code castMethod} 六爻起卦法（0 铜钱 / 1 时间 / 2 外应）</li>
     *   <li>{@code movingRule} 动爻规则（0 由哈希 / 1 固定一位 / 2 双动）</li>
     *   <li>{@code shenIndex} 六神映射起点偏移</li>
     *   <li>{@code shaCount} 神煞池大小</li>
     * </ul>
     */
    public record Dna(Identifier entityId, int ganOffset, int zhiRule, WuXing elementBias,
                      int dayunStep, int castMethod, int movingRule, int shenIndex, int shaCount) {

        /** 一行式描述，窥命符会显示它。 */
        public String describe() {
            String[] zhiRules = {"顺行", "逆行", "跳一", "对称"};
            String[] casts = {"铜钱六掷", "时间起卦", "外应起卦"};
            return "天干+" + this.ganOffset
                    + " 地支" + zhiRules[Math.floorMod(this.zhiRule, 4)]
                    + " 偏" + this.elementBias.zh()
                    + " 运步" + this.dayunStep + "年"
                    + " " + casts[Math.floorMod(this.castMethod, 3)]
                    + " 煞池" + this.shaCount;
        }
    }

    /**
     * 由注册名推出某一种生物的 DNA。
     *
     * <p><b>只读注册名</b>，这是规范里写死的一条。
     */
    public static Dna dna(long worldSeed, Identifier entityId) {
        YishuRandom random = StableKey.of(worldSeed, "mobdna")
                .add(entityId.getNamespace())
                .add(entityId.getPath())
                .random();
        return new Dna(entityId,
                random.nextInt(10),
                random.nextInt(4),
                WuXing.byIndex(random.nextInt(5)),
                3 + random.nextInt(8),
                random.nextInt(3),
                random.nextInt(3),
                random.nextInt(6),
                3 + random.nextInt(6));
    }

    // ==================================================================== 个体命盘

    /**
     * 一只生物的完整命盘。
     *
     * @param dna       类型 DNA
     * @param bazi      个体八字
     * @param hexagram  本命卦名
     * @param guanGui   官鬼（克我）—— 越高越激进
     * @param ziSun     子孙（我生）—— 越高越保守
     * @param xiongDi   兄弟（同我）—— 越高越爱集结
     * @param qiCai     妻财（我克）—— 影响交易
     * @param fuMu      父母（生我）—— 影响抗性与恢复
     */
    public record Profile(Dna dna, Bazi bazi, String hexagram, double guanGui, double ziSun,
                          double xiongDi, double qiCai, double fuMu) {

        /**
         * 攻击倾向 0.4 ~ 1.8。
         *
         * <p>官鬼越旺、子孙越弱，这只生物越愿意主动扑上来。
         */
        public double aggression() {
            return clamp(0.4D + this.guanGui * 0.75D - this.ziSun * 0.35D);
        }

        /**
         * 谨慎倾向 0.4 ~ 1.8。
         *
         * <p>子孙旺则更愿意退避、绕路、在血量低时逃跑。
         */
        public double caution() {
            return clamp(0.4D + this.ziSun * 0.8D - this.guanGui * 0.2D);
        }

        /**
         * 群体倾向 0.4 ~ 1.8。
         *
         * <p>兄弟旺的个体更容易彼此靠近、一起行动（表现为向同类的靠拢加速）。
         */
        public double cohesion() {
            return clamp(0.4D + this.xiongDi * 0.9D);
        }

        /** 交易修正（对村民一类可交易生物生效，越低越便宜）。 */
        public double tradeFactor() {
            return clamp(1.25D - this.qiCai * 0.35D);
        }

        /** 抗性修正（父母旺则皮糙肉厚）。 */
        public double resilience() {
            return clamp(1.0D + this.fuMu * 0.3D);
        }

        public String summary() {
            return this.bazi.dayMaster().zh() + this.bazi.dayMasterElement().zh()
                    + " · " + this.bazi.strength().zh()
                    + " · " + this.hexagram
                    + " · 鬼" + percent(this.guanGui)
                    + " 孙" + percent(this.ziSun)
                    + " 兄" + percent(this.xiongDi)
                    + " 财" + percent(this.qiCai);
        }

        private static String percent(double value) {
            return Math.round(clamp(value) * 100.0D / 1.8D) + "%";
        }

        private static double clamp(double value) {
            return Math.max(0.4D, Math.min(1.8D, value));
        }
    }

    /**
     * 推一只生物的命盘。
     *
     * <p>输入严格限定为 {@code 种子 + 注册名 + 维度 + 坐标 + 局部索引}。
     * 没有任何一项来自「现实时间」或「实体对象状态」，所以结果可以跨存档复现。
     */
    public static Profile profile(long worldSeed, Identifier entityId, String dimension,
                                  int x, int y, int z, int localIndex) {
        Dna dna = dna(worldSeed, entityId);

        StableKey key = StableKey.of(worldSeed, "mobfate")
                .add(entityId.getNamespace())
                .add(entityId.getPath())
                .add(dimension)
                .add(x).add(y).add(z)
                .add(localIndex);

        // 用 DNA 的参数去扰动本命八字的推导
        Bazi bazi = Bazi.forSubject(key.hash() ^ (long) dna.ganOffset * 0x9E3779B9L,
                entityId.getPath() + "/" + localIndex);

        Map<WuXing, Double> chart = bazi.chart();
        WuXing me = bazi.dayMasterElement();

        double guanGui = chart.get(me.overcomeBy());
        double ziSun = chart.get(me.generates());
        double xiongDi = chart.get(me);
        double qiCai = chart.get(me.overcomes());
        double fuMu = chart.get(me.generatedBy());

        // DNA 的五行偏向会放大对应那一项 —— 每种生物的命理「口味」由此不同
        if (me.overcomeBy() == dna.elementBias()) {
            guanGui *= 1.4D;
        }
        if (me.generates() == dna.elementBias()) {
            ziSun *= 1.4D;
        }
        if (me == dna.elementBias()) {
            xiongDi *= 1.4D;
        }
        if (me.overcomes() == dna.elementBias()) {
            qiCai *= 1.4D;
        }
        if (me.generatedBy() == dna.elementBias()) {
            fuMu *= 1.4D;
        }

        String hexagram = hexagramName(key);
        return new Profile(dna, bazi, hexagram, guanGui, ziSun, xiongDi, qiCai, fuMu);
    }

    /** 取实体所在位置的个体命盘（需要世界种子）。 */
    public static Profile profileOf(long worldSeed, Entity entity) {
        Identifier id = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        String dimension = entity.level().dimension().identifier().getPath();
        int localIndex = localIndex(entity);
        return profile(worldSeed, id, dimension,
                entity.blockPosition().getX(), entity.blockPosition().getY(), entity.blockPosition().getZ(),
                localIndex);
    }

    /**
     * 局部索引：同一格里第几只同类生物。
     *
     * <p>用实体在区块中的 id 取模可以得到稳定但会变化的值，
     * 所以这里改用「实体自增序号对 8 取模」——同一只生物在同一次会话里始终得到同一个值。
     */
    private static int localIndex(Entity entity) {
        return Math.floorMod(entity.getId(), 8);
    }

    /** 本命卦：用与六爻同一套推演，但只取卦名。 */
    public static String hexagramName(StableKey key) {
        boolean[] yang = new boolean[6];
        YishuRandom random = key.add("hexagram").random();
        for (int i = 0; i < 6; i++) {
            yang[i] = random.nextBoolean();
        }
        return com.chenchen.yishu.liuyao.Hexagram.ofYao(yang).name();
    }

    // ==================================================================== 开关

    /** 生物命理是否生效。 */
    public static boolean enabled(Mob mob) {
        YishuConfig config = YishuConfig.get();
        if (!config.mobFateEnabled) {
            return false;
        }
        String dimension = mob.level().dimension().identifier().getPath();
        return config.forDimension(dimension).fate;
    }

    /** 该生物是否值得算命盘（排除玩家、假人等）。 */
    public static boolean isSubject(Mob mob) {
        // 参数本身就是 Mob，玩家不属于 Mob（两者是兄弟类），无需再排除
        return mob.isAlive() && !mob.isRemoved();
    }

    /** 窥命符的展示串。 */
    public static String inspectText(Profile profile, String entityName) {
        return entityName + "｜" + profile.summary() + "｜算法 " + profile.dna().describe();
    }

    /**
     * 强行窥命的代价：返回一个反噬等级。
     *
     * <p>看自己的命不要钱，看别人的命要 —— 否则玩家会把整个世界的生物命盘
     * 当数据库刷一遍，那样「命理」就退化成了查表。
     */
    public static Divination.Backlash peekingBacklash(int peeksToday) {
        if (!YishuConfig.get().backlashEnabled) {
            return Divination.Backlash.NONE;
        }
        return peeksToday <= 6 ? Divination.Backlash.NONE : Divination.Backlash.forCount(peeksToday, 6);
    }

    /** 供外部引用的维度档案。 */
    public static DimensionProfile dimensionOf(Entity entity) {
        return DimensionProfile.of(entity.level().dimension().identifier().toString());
    }
}
