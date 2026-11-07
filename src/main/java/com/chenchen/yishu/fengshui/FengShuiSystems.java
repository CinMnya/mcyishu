package com.chenchen.yishu.fengshui;

import com.chenchen.yishu.YishuConfig;
import com.chenchen.yishu.core.StableKey;
import com.chenchen.yishu.core.YishuRandom;
import com.chenchen.yishu.dimension.DimensionProfile;
import com.chenchen.yishu.fate.GanZhi.Pair;
import com.chenchen.yishu.fate.GanZhi.Zhi;
import com.chenchen.yishu.fate.Yun;

import java.util.ArrayList;
import java.util.List;

/**
 * 易枢 · 风水三事 —— 龙脉、太岁、每日宜忌。
 *
 * <p>风水域（{@link FengShuiField}）回答的是「这块地怎么样」，
 * 而这个文件回答的是「<b>现在</b>这块地怎么样」—— 它把时间轴接进了空间。
 *
 * <ul>
 *   <li>{@link DragonVein 龙脉}：一条不规则的带状区域，逐年游走。
 *       站在上面一切采集皆有加成，且可以被玩家「占领」立契。</li>
 *   <li>{@link TaiSui 太岁}：每年坐镇一个方位。在那方位上动土会招煞，
 *       但立一块石敢当就能化解。</li>
 *   <li>{@link Almanac 每日宜忌}：从流日干支推出来的当日宜忌。
 *       照着做顺风顺水，反着来平地生煞。</li>
 * </ul>
 */
public final class FengShuiSystems {

    private FengShuiSystems() {
    }

    /** 一个游戏年 = 360 天，与 {@link Yun#DAYS_PER_YEAR} 保持一致。 */
    public static final long DAYS_PER_YEAR = Yun.DAYS_PER_YEAR;

    // ==================================================================== 龙脉

    /**
     * 龙脉。
     *
     * <p>龙脉的走法：从一个由种子决定的起点出发，沿一条「方向随机游走但带惯性」
     * 的折线前进。每一步都以上一步的方向为基础小幅转向 —— 这样画出来的是一条
     * <b>蜿蜒的带子</b>，而不是折线锯齿，也不像圆那么规整。
     *
     * <p>「每年游走」的实现：把年份作为键的一部分。所以第 0 年的龙脉和第 1 年的
     * 龙脉是两条不同的曲线。玩家想找龙脉，得先问清楚「今年的龙脉在哪」——
     * 这正是六爻起卦里「寻遗迹」和「寻矿」这两类问题的价值所在。
     */
    public static final class DragonVein {

        /** 一条龙脉由 {@value #SEGMENTS} 段构成。 */
        public static final int SEGMENTS = 14;
        /** 每段长度（方块）。 */
        public static final int STEP = 220;
        /** 带子半宽（方块）。站在这之内算「在龙脉上」。 */
        public static final double WIDTH = 26.0D;
        /** 世界原点附近的总跨度约 3000 格，足够覆盖常见活动范围。 */

        private DragonVein() {
        }

        /** 龙脉上的一点。 */
        public record Node(int index, int x, int z, int aura) {
        }

        /**
         * 生成某一年的龙脉。
         *
         * @param year 游戏年（{@code epochDay / 360}）
         */
        public static List<Node> path(long worldSeed, DimensionProfile dimension, long year) {
            YishuRandom random = StableKey.of(worldSeed, "dragonvein")
                    .add(dimension.shortKey()).add(year).random();

            List<Node> nodes = new ArrayList<>(SEGMENTS);
            double x = random.nextInt(-800, 800);
            double z = random.nextInt(-800, 800);
            double angle = random.nextDouble() * Math.PI * 2.0D;

            for (int i = 0; i < SEGMENTS; i++) {
                nodes.add(new Node(i, (int) Math.round(x), (int) Math.round(z),
                        55 + random.nextInt(0, 45)));
                // 惯性转向：每次最多偏 ±35°
                angle += (random.nextDouble() - 0.5D) * (Math.PI * 35.0D / 180.0D);
                x += Math.cos(angle) * STEP;
                z += Math.sin(angle) * STEP;
            }
            return nodes;
        }

        /**
         * 某点到龙脉主轴的最近距离。
         *
         * <p>用「点到各线段」的距离取最小值，所以带子会拐弯，而不是一串圆。
         */
        public static double distance(long worldSeed, DimensionProfile dimension, long year,
                                      double x, double z) {
            List<Node> nodes = path(worldSeed, dimension, year);
            double best = Double.MAX_VALUE;
            for (int i = 0; i < nodes.size() - 1; i++) {
                Node a = nodes.get(i);
                Node b = nodes.get(i + 1);
                best = Math.min(best, pointSegmentDistance(x, z, a.x(), a.z(), b.x(), b.z()));
            }
            return best;
        }

        public static boolean isOnVein(long worldSeed, DimensionProfile dimension, long year,
                                       double x, double z) {
            return distance(worldSeed, dimension, year, x, z) <= WIDTH;
        }

        /**
         * 站在龙脉上时，龙气的浓度（0~1）。
         *
         * <p>越靠轴心越浓。浓度决定采集加成与风水评分的额外提升。
         */
        public static double auraStrength(long worldSeed, DimensionProfile dimension, long year,
                                          double x, double z) {
            double distance = distance(worldSeed, dimension, year, x, z);
            if (distance >= WIDTH) {
                return 0.0D;
            }
            return 1.0D - distance / WIDTH;
        }

        /** 最近的那个节点 —— 寻龙尺指向它。 */
        public static Node nearestNode(long worldSeed, DimensionProfile dimension, long year,
                                       double x, double z) {
            List<Node> nodes = path(worldSeed, dimension, year);
            Node best = nodes.get(0);
            double bestDistance = Double.MAX_VALUE;
            for (Node node : nodes) {
                double distance = (node.x() - x) * (node.x() - x) + (node.z() - z) * (node.z() - z);
                if (distance < bestDistance) {
                    bestDistance = distance;
                    best = node;
                }
            }
            return best;
        }

        /** 由坐标反查「最近的节点序号」，用于把占领记录归到某一段上。 */
        public static int nodeIndexOf(long worldSeed, DimensionProfile dimension, long year,
                                      double x, double z) {
            return nearestNode(worldSeed, dimension, year, x, z).index();
        }

        private static double pointSegmentDistance(double px, double pz, double ax, double az,
                                                   double bx, double bz) {
            double dx = bx - ax;
            double dz = bz - az;
            double lengthSq = dx * dx + dz * dz;
            if (lengthSq <= 0.0D) {
                return Math.hypot(px - ax, pz - az);
            }
            double t = ((px - ax) * dx + (pz - az) * dz) / lengthSq;
            t = Math.max(0.0D, Math.min(1.0D, t));
            return Math.hypot(px - (ax + t * dx), pz - (az + t * dz));
        }
    }

    // ==================================================================== 太岁

    /**
     * 太岁。
     *
     * <p>太岁是「今年的岁星所在方位」。在它的方位上动土（挖方、放方、拆方）
     * 会招来煞气，累积到一定程度会有实打实的惩罚。
     *
     * <p>方位由年份的地支决定：十二地支对应十二方位，
     * 子北、卯东、午南、酉西，其余按 30° 均分。
     */
    public static final class TaiSui {

        private TaiSui() {
        }

        /** 太岁方位（0 = 正北，顺时针 45° 一格，共 8 个方向）。 */
        public static int direction8(long year) {
            Zhi zhi = Zhi.byIndex((int) Math.floorMod(year, 12));
            // 子(0)→北、丑寅→东北、卯→东、辰巳→东南、午→南、未申→西南、酉→西、戌亥→西北
            return switch (zhi) {
                case ZI -> 0;
                case CHOU, YIN -> 1;
                case MAO -> 2;
                case CHEN, SI -> 3;
                case WU -> 4;
                case WEI, SHEN -> 5;
                case YOU -> 6;
                case XU, HAI -> 7;
            };
        }

        public static final String[] DIRECTION_NAMES = {
                "正北", "东北", "正东", "东南", "正南", "西南", "正西", "西北"
        };

        public static String directionName(long year) {
            return DIRECTION_NAMES[direction8(year)];
        }

        /** 该年份的太岁干支。 */
        public static Pair ganZhi(long year) {
            return Pair.of((int) Math.floorMod(year, 60));
        }

        /**
         * 某个坐标是否落在太岁方位上。
         *
         * <p>判据是「以世界原点为心的扇形 + 距离门槛」。
         * 用扇形而不是「整个北方」，是为了让玩家能靠走位绕开太岁 ——
         * 一个无法规避的惩罚不叫规则，叫骚扰。
         */
        public static boolean inTaiSuiSector(long year, double x, double z) {
            double distance = Math.hypot(x, z);
            if (distance < 64.0D) {
                return false;
            }
            double angle = Math.toDegrees(Math.atan2(x, -z));
            if (angle < 0.0D) {
                angle += 360.0D;
            }
            double target = direction8(year) * 45.0D;
            double delta = Math.abs(((angle - target + 540.0D) % 360.0D) - 180.0D);
            return delta <= 22.5D;
        }

        /** 动土造成的煞气增量（0 表示不在太岁位，无惩罚）。 */
        public static int soilBreakingSha(long year, boolean wardedNearby) {
            if (wardedNearby) {
                // 石敢当化解：完全抵消
                return 0;
            }
            return 12;
        }
    }

    // ==================================================================== 每日宜忌

    /**
     * 每日宜忌。
     *
     * <p>从当日流日干支推出来的一张「今天适合做什么」的清单。
     * 遵守了有加成，违反了会积煞。
     *
     * <p>它的作用不是惩罚玩家，而是<b>给每天一个不同的节奏</b>：
     * 今天宜动土，那就修修房子；今天忌远行，那就在家附魔。
     * 有了它，同样的世界不会每天都长得一样。
     */
    public static final class Almanac {

        private Almanac() {
        }

        /** 一条宜忌。 */
        public record Item(String key, String zh, boolean favorable, int magnitude) {

            public String describe() {
                return (this.favorable ? "宜 " : "忌 ") + this.zh;
            }
        }

        /** 全部可出现的条目。 */
        private static final List<Item> POOL = List.of(
                new Item("dig", "动土兴建", true, 8),
                new Item("mine", "入地采掘", true, 8),
                new Item("trade", "交易买卖", true, 7),
                new Item("enchant", "附魔炼器", true, 7),
                new Item("ritual", "祭祀祈福", true, 9),
                new Item("bed", "安床落帐", true, 6),
                new Item("door", "安门立户", true, 6),
                new Item("west", "西行远游", true, 6),
                new Item("east", "东行远游", true, 6),
                new Item("north", "北行远游", true, 6),
                new Item("south", "南行远游", true, 6),
                new Item("battle", "征伐战斗", true, 6),
                new Item("harvest", "收获作物", true, 7),
                new Item("breed", "繁育牲畜", true, 6),
                new Item("dig_forbid", "动土兴建", false, 8),
                new Item("mine_forbid", "入地采掘", false, 8),
                new Item("trade_forbid", "交易买卖", false, 7),
                new Item("bed_forbid", "安床落帐", false, 6),
                new Item("door_forbid", "安门立户", false, 6),
                new Item("travel_forbid", "远行出游", false, 9),
                new Item("battle_forbid", "征伐战斗", false, 8),
                new Item("night_forbid", "夜间外出", false, 9),
                new Item("water_forbid", "涉水渡河", false, 7),
                new Item("fire_forbid", "近火用炉", false, 7)
        );

        /**
         * 取某一天的宜忌（宜 2~3 条、忌 2~3 条）。
         *
         * <p>同一种子、同一维度、同一天，永远是同一份黄历。
         */
        public static List<Item> ofDay(long worldSeed, DimensionProfile dimension, long epochDay) {
            YishuRandom random = StableKey.of(worldSeed, "almanac")
                    .add(dimension.shortKey()).add(epochDay).random();

            List<Item> favorable = new ArrayList<>();
            List<Item> unfavorable = new ArrayList<>();
            for (Item item : POOL) {
                if (item.favorable()) {
                    favorable.add(item);
                } else {
                    unfavorable.add(item);
                }
            }
            random.shuffle(favorable);
            random.shuffle(unfavorable);

            List<Item> result = new ArrayList<>();
            int luckCount = 2 + random.nextInt(2);
            int tabooCount = 2 + random.nextInt(2);
            result.addAll(favorable.subList(0, Math.min(luckCount, favorable.size())));
            result.addAll(unfavorable.subList(0, Math.min(tabooCount, unfavorable.size())));
            return List.copyOf(result);
        }

        /** 某一天是否「宜」做某件事。 */
        public static boolean isFavorable(List<Item> almanac, String key) {
            for (Item item : almanac) {
                if (item.key().equals(key)) {
                    return item.favorable();
                }
            }
            return false;
        }

        /** 某一天是否「忌」做某件事。 */
        public static boolean isTaboo(List<Item> almanac, String key) {
            for (Item item : almanac) {
                if (item.key().equals(key + "_forbid")) {
                    return true;
                }
            }
            return false;
        }

        /** 一句话总结今日运势。 */
        public static String summary(List<Item> almanac) {
            StringBuilder builder = new StringBuilder();
            for (Item item : almanac) {
                if (builder.length() > 0) {
                    builder.append('，');
                }
                builder.append(item.describe());
            }
            return builder.toString();
        }
    }

    // ==================================================================== 风水效果

    /**
     * 把风水等级翻译成实际生效的数值。
     *
     * <p>规范里定的效果：
     * <ul>
     *   <li><b>大吉</b>：作物加速、刷怪减少、交易折扣、幸运提升</li>
     *   <li><b>大凶</b>：作物减产、刷怪增加、夜晚出阴煞、恢复下降</li>
     * </ul>
     * 中间的吉、平、凶按线性插值。
     */
    public record Effects(double cropGrowth, double mobSpawn, double tradePrice,
                          double luck, double regeneration, boolean nightSha) {

        /** 中性（平）。 */
        public static final Effects NEUTRAL = new Effects(1.0D, 1.0D, 1.0D, 1.0D, 1.0D, false);

        public static Effects of(FengShuiField.Grade grade) {
            return switch (grade) {
                case GREAT_LUCK -> new Effects(1.35D, 0.65D, 0.90D, 1.30D, 1.20D, false);
                case LUCK -> new Effects(1.15D, 0.85D, 0.96D, 1.15D, 1.08D, false);
                case PLAIN -> NEUTRAL;
                case BAD -> new Effects(0.82D, 1.25D, 1.06D, 0.85D, 0.90D, true);
                case DIRE -> new Effects(0.62D, 1.65D, 1.12D, 0.70D, 0.78D, true);
            };
        }

        /**
         * 叠加命主喜用神的修正。
         *
         * <p>一块地再好，也不一定适合你 —— 如果这块地的五行正好是你的忌神，
         * 那么对你的加成会大打折扣。这一层让「看风水」变成一件很个人的事。
         */
        public Effects withPersonal(FengShuiField.Sample sample, com.chenchen.yishu.fate.Bazi.YongShen yongShen) {
            double multiplier = yongShen.multiplier(sample.element());
            // 只让「利好项」跟着五行走，「惩罚项」保留 —— 否则忌神地就变成了双重惩罚
            return new Effects(
                    this.cropGrowth * multiplier,
                    this.mobSpawn,
                    this.tradePrice / Math.max(0.8D, multiplier),
                    this.luck * multiplier,
                    this.regeneration * multiplier,
                    this.nightSha);
        }

        /** 是否是「有问题」的风水（夜晚会出阴煞）。 */
        public boolean isHostile() {
            return this.nightSha;
        }
    }

    /** 便捷：把开关串起来，判断风水是否在当前配置下生效。 */
    public static boolean enabled(DimensionProfile dimension) {
        YishuConfig config = YishuConfig.get();
        return config.fengShuiEnabled && config.forDimension(dimension.shortKey()).fengShui;
    }
}
