package com.chenchen.yishu.building;

import com.chenchen.yishu.YishuConfig;
import com.chenchen.yishu.core.StableKey;
import com.chenchen.yishu.core.YishuRandom;
import com.chenchen.yishu.dimension.DimensionProfile;
import com.chenchen.yishu.fate.Bazi;
import com.chenchen.yishu.fate.GanZhi.Pair;
import com.chenchen.yishu.fate.GanZhi.WuXing;
import com.chenchen.yishu.fate.Yun;
import com.chenchen.yishu.fengshui.FengShuiField;

import java.util.List;

/**
 * 易枢 · 建筑命理 —— 房子也有命。
 *
 * <p>一座村庄、一座要塞、一间试炼密室，都不是背景板。它们有自己的八字、
 * 自己的本命卦、自己的风水，以及<b>每天都在变的当日状态</b>。
 *
 * <p>「同种子同建筑命盘固定」的落点：命盘由
 * {@code 种子 + 建筑类型 + 维度 + 建筑坐标} 推出。
 * 坐标取的是建筑锚点（村庄的钟楼、要塞的入口之类），
 * 所以同一座村庄在任何人眼里都是同一个命。
 *
 * <p><b>渡劫</b>是这套系统的高潮：当一座建筑连续多日处于「大凶」，
 * 就会进入渡劫状态 —— 怪物强化、结构受损、战利品变质。
 * 玩家可以赶去化解（放置祭品、上香、补墙），化解成功则建筑转运。
 */
public final class BuildingFate {

    private BuildingFate() {
    }

    // ==================================================================== 类型

    /**
     * 建筑类型。
     *
     * <p>{@code structureIds} 是原版结构 id 的匹配片段；识别时只看这个，
     * 不看方块 —— 这样跨版本时结构 id 不变就还能用。
     */
    public enum BuildingType {
        VILLAGE("村庄", "village", WuXing.TU, "人气所聚，宜安定"),
        TEMPLE("神庙", "temple", WuXing.HUO, "香火之地，煞气自消"),
        FORTRESS("要塞", "fortress", WuXing.JIN, "铁石为骨，攻气极重"),
        MINESHAFT("废弃矿井", "mineshaft", WuXing.JIN, "深入地脉，多金亦多险"),
        OUTPOST("掠夺者前哨", "outpost", WuXing.HUO, "兵戈之气，血光不散"),
        OCEAN_MONUMENT("海底神殿", "monument", WuXing.SHUI, "深水之宫，阴气盘结"),
        WOODLAND_MANSION("林地府邸", "mansion", WuXing.MU, "幽林秘境，诡谲丛生"),
        ANCIENT_CITY("远古城市", "ancient_city", WuXing.TU, "暗影沉埋，静得可怕"),
        TRIAL_CHAMBER("试炼密室", "trial_chambers", WuXing.JIN, "机关密布，试炼之所"),
        RUINED_PORTAL("废弃传送门", "ruined_portal", WuXing.HUO, "维度裂口，气机紊乱"),
        NETHER_FORTRESS("下界要塞", "nether_fortress", WuXing.HUO, "烈焰铸就，火煞冲天"),
        BASTION("堡垒遗迹", "bastion", WuXing.JIN, "金铁之声不绝"),
        END_CITY("末地城", "end_city", WuXing.TU, "紫颂环绕，虚空之民"),
        END_SHIP("末地船", "end_ship", WuXing.JIN, "泊于虚空，载着龙气"),
        YISHU_RUIN("易枢遗迹", "yishu", WuXing.TU, "术数遗存，机锋深藏");

        private final String zh;
        private final String structureId;
        private final WuXing element;
        private final String note;

        BuildingType(String zh, String structureId, WuXing element, String note) {
            this.zh = zh;
            this.structureId = structureId;
            this.element = element;
            this.note = note;
        }

        public String zh() {
            return this.zh;
        }

        public String structureId() {
            return this.structureId;
        }

        public WuXing element() {
            return this.element;
        }

        public String note() {
            return this.note;
        }

        public static final BuildingType[] VALUES = values();

        /** 由结构 id 反查；找不到时返回 {@code null}。 */
        public static BuildingType byStructureId(String id) {
            if (id == null) {
                return null;
            }
            for (BuildingType type : VALUES) {
                if (id.contains(type.structureId)) {
                    return type;
                }
            }
            return null;
        }
    }

    // ==================================================================== 命盘

    /**
     * 一座建筑的命盘。
     *
     * @param type      类型
     * @param anchorX   锚点坐标
     * @param bazi      八字
     * @param hexagram  本命卦
     * @param field     所在地风水
     */
    public record Fate(BuildingType type, int anchorX, int anchorZ, String dimension,
                       Bazi bazi, String hexagram, FengShuiField.Sample field) {

        public String summary() {
            return this.type.zh() + "｜日主 " + this.bazi.dayMaster().zh()
                    + this.bazi.dayMasterElement().zh()
                    + "｜本命卦 " + this.hexagram
                    + "｜" + this.field.grade(DimensionProfile.of(this.dimension)).zh()
                    + "｜气质：" + temperament();
        }

        /** 建筑气质 —— 由日主五行决定，影响内部生物强度与战利品倾向。 */
        public String temperament() {
            return switch (this.bazi.dayMasterElement()) {
                case JIN -> "肃杀：内部生物攻击更高，战利品偏矿物";
                case MU -> "生发：内部生物更多，战利品偏植物与食物";
                case SHUI -> "幽深：内部生物更隐蔽，战利品偏药材与附魔";
                case HUO -> "暴烈：内部生物更激进，战利品偏冶炼与火药";
                case TU -> "厚重：内部生物更耐打，战利品偏建材与陶器";
            };
        }

        /** 内部生物强度倍率（1.0 为基准）。 */
        public double mobStrength() {
            return switch (this.bazi.dayMasterElement()) {
                case JIN -> 1.25D;
                case HUO -> 1.18D;
                case TU -> 1.10D;
                case SHUI -> 1.05D;
                case MU -> 1.00D;
            };
        }

        /** 战利品倾向的五行。 */
        public WuXing lootFlavor() {
            return this.bazi.dayMasterElement();
        }
    }

    /**
     * 推一座建筑的命盘。
     *
     * <p>{@code anchorX/anchorZ} 应当是建筑的稳定锚点。玩家随便在建筑内部走两步
     * 不应该改变它的命 —— 所以调用方应当传「结构生成的锚点」，
     * 而不是玩家当前站的位置。
     */
    public static Fate fateOf(long worldSeed, DimensionProfile dimension, BuildingType type,
                              int anchorX, int anchorZ) {
        StableKey key = StableKey.of(worldSeed, "building")
                .add(dimension.shortKey())
                .add(type.structureId())
                .add(anchorX)
                .add(anchorZ);

        Bazi bazi = Bazi.forSubject(key.hash(), type.structureId() + "@" + anchorX + "," + anchorZ);
        String hexagram = com.chenchen.yishu.spirit.MobFate.hexagramName(key);
        FengShuiField.Sample field = FengShuiField.sample(worldSeed, dimension, anchorX, anchorZ);

        return new Fate(type, anchorX, anchorZ, dimension.shortKey(), bazi, hexagram, field);
    }

    // ==================================================================== 每日状态

    /** 建筑的当日状态。 */
    public enum DailyState {
        AUSPICIOUS("吉", 0xFF7ED98A, "气机顺畅，内部生物温驯，战利品丰厚"),
        NORMAL("平", 0xFFD6D6A0, "如常"),
        INAUSPICIOUS("凶", 0xFFD99A5B, "气机滞涩，怪物躁动，出货变少"),
        CALAMITOUS("大凶", 0xFFD9575B, "灾气笼罩，此地将出事");

        private final String zh;
        private final int color;
        private final String note;

        DailyState(String zh, int color, String note) {
            this.zh = zh;
            this.color = color;
            this.note = note;
        }

        public String zh() {
            return this.zh;
        }

        public int color() {
            return this.color;
        }

        public String note() {
            return this.note;
        }
    }

    /**
     * 建筑的当日状态。
     *
     * <p>算法：拿当日流日干支与建筑的日主做十神判断。
     * 流日生助建筑日主则吉，克泄则凶；再叠加上建筑所在地的风水等级。
     * 结果完全由「种子 + 建筑 + 游戏日」决定，所以今天凶的地方，
     * 别人今天去看也是凶的。
     */
    public static DailyState dailyState(long worldSeed, Fate fate, long epochDay) {
        Pair day = Yun.liuRi(worldSeed, fate.dimension(), epochDay);
        WuXing dayElement = day.gan().wuXing();
        WuXing building = fate.bazi().dayMasterElement();

        int score = 0;
        if (dayElement.generates() == building) {
            score += 3;         // 日辰生我
        } else if (dayElement == building) {
            score += 1;         // 比和
        } else if (building.generates() == dayElement) {
            score -= 1;         // 我生日辰，耗气
        } else if (building.overcomes() == dayElement) {
            score -= 1;
        } else {
            score -= 3;         // 日辰克我
        }

        score += switch (fate.field().grade(DimensionProfile.of(fate.dimension()))) {
            case GREAT_LUCK -> 3;
            case LUCK -> 1;
            case PLAIN -> 0;
            case BAD -> -2;
            case DIRE -> -3;
        };

        // 固定的扰动，避免所有建筑的状态整齐划一
        int jitter = StableKey.of(worldSeed, "building:state")
                .add(fate.dimension()).add(fate.anchorX()).add(fate.anchorZ()).add(epochDay)
                .range(-1, 1);
        score += jitter;

        if (score >= 5) {
            return DailyState.AUSPICIOUS;
        }
        if (score >= 1) {
            return DailyState.NORMAL;
        }
        if (score >= -3) {
            return DailyState.INAUSPICIOUS;
        }
        return DailyState.CALAMITOUS;
    }

    // ==================================================================== 渡劫

    /**
     * 渡劫状态。
     *
     * <p>当一座建筑<b>连续</b> {@value #CALAMITY_THRESHOLD} 天处于大凶，
     * 就进入渡劫：刷怪率大幅上升、结构开始被侵蚀、稀有战利品反而更容易出现
     * （这是给敢去的人的回报）。
     *
     * <p>读取连续天数的窗口是固定的 5 天，所以这个判断同样不依赖任何缓存状态 ——
     * 每次从种子和日期重算就行。
     */
    public static final int CALAMITY_THRESHOLD = 3;

    public record Calamity(boolean active, int consecutiveDays, String note) {

        public static Calamity evaluate(long worldSeed, Fate fate, long epochDay) {
            int consecutive = 0;
            for (int i = 0; i < CALAMITY_THRESHOLD; i++) {
                if (dailyState(worldSeed, fate, epochDay - i) == DailyState.CALAMITOUS) {
                    consecutive++;
                } else {
                    break;
                }
            }
            if (consecutive < CALAMITY_THRESHOLD) {
                return new Calamity(false, consecutive, "");
            }
            return new Calamity(true, consecutive,
                    "此地已连续 " + consecutive + " 日大凶，劫气成形 —— 渡劫中。");
        }

        /** 渡劫期间内部生物强度再乘的倍率。 */
        public double mobMultiplier() {
            return this.active ? 1.45D : 1.0D;
        }

        /** 渡劫期间稀有战利品的额外概率。 */
        public double rareLootBonus() {
            return this.active ? 0.25D : 0.0D;
        }
    }

    // ==================================================================== 开关

    public static boolean enabled(String dimension) {
        YishuConfig config = YishuConfig.get();
        return config.buildingFateEnabled && config.forDimension(dimension).fate;
    }

    /** 玩家可以用来自查的一句话报告。 */
    public static List<String> report(long worldSeed, Fate fate, long epochDay) {
        DailyState state = dailyState(worldSeed, fate, epochDay);
        Calamity calamity = Calamity.evaluate(worldSeed, fate, epochDay);
        return List.of(
                fate.summary(),
                "今日：" + state.zh() + " —— " + state.note(),
                calamity.active() ? calamity.note() : "未见劫气");
    }

    /** 由原版结构 id 与锚点直接生成报告的便捷入口。 */
    public static Fate fromStructure(long worldSeed, DimensionProfile dimension,
                                     String structureId, int anchorX, int anchorZ) {
        BuildingType type = BuildingType.byStructureId(structureId);
        return fateOf(worldSeed, dimension, type == null ? BuildingType.VILLAGE : type, anchorX, anchorZ);
    }

    /** 一个随日期变化的随机流，供建筑内部战利品使用。 */
    public static YishuRandom lootRandom(long worldSeed, Fate fate, long epochDay, int salt) {
        return StableKey.of(worldSeed, "building:loot")
                .add(fate.dimension()).add(fate.anchorX()).add(fate.anchorZ())
                .add(epochDay).add(salt).random();
    }
}
