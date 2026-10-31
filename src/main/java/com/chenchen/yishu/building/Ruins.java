package com.chenchen.yishu.building;

import com.chenchen.yishu.YishuConfig;
import com.chenchen.yishu.core.StableKey;
import com.chenchen.yishu.core.YishuRandom;
import com.chenchen.yishu.dimension.DimensionProfile;
import com.chenchen.yishu.fate.GanZhi.WuXing;
import com.chenchen.yishu.fate.Yun;

import java.util.ArrayList;
import java.util.List;

/**
 * 易枢 · 十二处专属遗迹。
 *
 * <p>规范第八节给主世界、下界、末地各安排了一组遗迹，每一处都有自己的机关原理。
 * 这里把它们的<b>规则</b>完整实现：
 * <ul>
 *   <li>每处遗迹有一个确定性坐标（由种子推出，同种子永远在同一处）；</li>
 *   <li>每处遗迹有独立的命盘、风水、本命卦（复用 {@link BuildingFate}）；</li>
 *   <li>每处遗迹有一道「机关」——用五行属性或物品把它激活，激活后给出奖励；</li>
 *   <li>激活条件里刻意掺入「当日卦象」这类随时间变化的要素，
 *       所以同一处遗迹不是随时都能开的 —— 你得等到对的日子。</li>
 * </ul>
 *
 * <p><b>关于生成</b>：遗迹的坐标与规则是完整可用的，但地形级的结构生成
 * （真正在地表立起九层塔、铺出八卦石柱阵）属于下一阶段。
 * 目前的做法是：遗迹位置附近会生成一个「遗迹核心」方块作为入口标记，
 * 玩家按六爻线索找到它、按规则激活它。这样机制先跑起来，
 * 美术与结构可以后面再补，而不必把整套玩法卡在那里。
 */
public final class Ruins {

    private Ruins() {
    }

    /** 分组：主世界 6 / 下界 3 / 末地 3。 */
    public enum Group {
        OVERWORLD("主世界"),
        NETHER("下界"),
        END("末地");

        private final String zh;

        Group(String zh) {
            this.zh = zh;
        }

        public String zh() {
            return this.zh;
        }
    }

    /**
     * 一处遗迹的定义。
     *
     * @param key        稳定标识
     * @param zh         名称
     * @param group      所属维度组
     * @param mechanism  机关原理（给玩家看的说明）
     * @param catalyst   激活所需的主属性
     * @param rewardZh   奖励说明
     * @param requiresDayHexagram 是否要求「今日卦象」满足条件才能开启
     */
    public record RuinType(String key, String zh, Group group, String mechanism, WuXing catalyst,
                           String rewardZh, boolean requiresDayHexagram) {
    }

    private static final List<RuinType> TYPES = List.of(
            // ---------------- 主世界 6 ----------------
            new RuinType("bagua_array", "八卦阵遗址", Group.OVERWORLD,
                    "按当日卦象的动爻顺序踩亮石柱，顺序错则重来",
                    WuXing.MU, "开启密室，内有五行灵石与太极图", true),
            new RuinType("dragon_altar", "龙脉祭坛", Group.OVERWORLD,
                    "在坛上放置五行灵石，激活后大范围提升所在地风水",
                    WuXing.TU, "大范围风水提升，可持续数个游戏日", false),
            new RuinType("tianji_pavilion", "天机阁", Group.OVERWORLD,
                    "九层塔，每层解一道卦象谜题，全解方可登顶",
                    WuXing.JIN, "顶层可查看天机（本区域完整命盘与未来七日运数）", true),
            new RuinType("taisui_temple", "太岁庙", Group.OVERWORLD,
                    "祭祀以降低当岁太岁之煞",
                    WuXing.HUO, "当岁太岁煞气大幅下降，动土不再招煞", false),
            new RuinType("yinyang_stele", "阴阳界碑", Group.OVERWORLD,
                    "同时献上阴属性与阳属性之物，阴阳平衡乃开",
                    WuXing.SHUI, "开启密室，内有阴阳石", false),
            new RuinType("reincarnation_altar", "轮回台", Group.OVERWORLD,
                    "站上高台重掷命盘，结果完全随机且不可逆",
                    WuXing.TU, "一次轮回机会", false),

            // ---------------- 下界 3 ----------------
            new RuinType("fire_altar", "火煞祭坛", Group.NETHER,
                    "以下界最缺的「阴」压制火煞",
                    WuXing.SHUI, "获得烈焰碎片", false),
            new RuinType("soul_altar", "灵魂祭坛", Group.NETHER,
                    "以「阳」压制过盛的灵魂之气",
                    WuXing.HUO, "获得灵魂核心", false),
            new RuinType("metal_bastion", "金气堡垒", Group.NETHER,
                    "以火激活金气，火金相成",
                    WuXing.HUO, "获得下界合金", false),

            // ---------------- 末地 3 ----------------
            new RuinType("void_altar", "虚空祭坛", Group.END,
                    "以「实体」之物激活虚空 —— 用最有形的东西去撬动最无形的东西",
                    WuXing.TU, "获得虚空碎片", false),
            new RuinType("chorus_altar", "紫颂祭坛", Group.END,
                    "以虚空属性激活紫颂",
                    WuXing.SHUI, "获得紫颂核心", false),
            new RuinType("dragon_altar_end", "龙气祭坛", Group.END,
                    "击败末影龙后方可激活",
                    WuXing.JIN, "获得龙气", false)
    );

    public static List<RuinType> all() {
        return TYPES;
    }

    public static RuinType byKey(String key) {
        for (RuinType type : TYPES) {
            if (type.key().equals(key)) {
                return type;
            }
        }
        return TYPES.get(0);
    }

    public static List<RuinType> ofGroup(Group group) {
        List<RuinType> result = new ArrayList<>();
        for (RuinType type : TYPES) {
            if (type.group() == group) {
                result.add(type);
            }
        }
        return result;
    }

    public static Group groupOf(DimensionProfile dimension) {
        return switch (dimension) {
            case NETHER -> Group.NETHER;
            case END -> Group.END;
            default -> Group.OVERWORLD;
        };
    }

    // ==================================================================== 位置

    /**
     * 一处遗迹的落点。
     *
     * @param type  类型
     * @param x     世界坐标
     * @param z     世界坐标
     * @param fate  命盘
     */
    public record Site(RuinType type, int x, int z, String dimension, BuildingFate.Fate fate) {

        public String summary() {
            return this.type.zh() + " @ " + this.x + ", " + this.z + " —— " + this.type.mechanism();
        }
    }

    /**
     * 生成某个维度里所有遗迹的落点。
     *
     * <p>落点完全由种子决定，所以「第 5 号八卦阵遗址在哪」是一个可以写在论坛上的事实。
     */
    public static List<Site> sites(long worldSeed, DimensionProfile dimension) {
        List<RuinType> types = ofGroup(groupOf(dimension));
        List<Site> sites = new ArrayList<>(types.size());
        for (RuinType type : types) {
            YishuRandom random = StableKey.of(worldSeed, "ruin")
                    .add(dimension.shortKey()).add(type.key()).random();
            int x = random.nextInt(-2600, 2600);
            int z = random.nextInt(-2600, 2600);
            BuildingFate.Fate fate = BuildingFate.fateOf(worldSeed, dimension,
                    BuildingFate.BuildingType.YISHU_RUIN, x, z);
            sites.add(new Site(type, x, z, dimension.shortKey(), fate));
        }
        return sites;
    }

    /** 离给定坐标最近的一处遗迹。 */
    public static Site nearest(long worldSeed, DimensionProfile dimension, double x, double z) {
        List<Site> sites = sites(worldSeed, dimension);
        Site best = sites.get(0);
        double bestDistance = Double.MAX_VALUE;
        for (Site site : sites) {
            double distance = Math.hypot(site.x() - x, site.z() - z);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = site;
            }
        }
        return best;
    }

    // ==================================================================== 机关

    /**
     * 一处遗迹的「当日可开启性」。
     *
     * <p>对 {@link RuinType#requiresDayHexagram()} 为真的遗迹，
     * 需要当日卦象满足条件才开 —— 具体判据是「当日之卦不属大凶且动爻数为偶数」，
     * 这样玩家得挑日子，遗迹才不像自动售货机。
     */
    public static boolean isOpenToday(long worldSeed, DimensionProfile dimension, RuinType type, long epochDay) {
        if (!type.requiresDayHexagram()) {
            return true;
        }
        boolean[] yang = new boolean[6];
        YishuRandom random = StableKey.of(worldSeed, "ruin:day")
                .add(dimension.shortKey()).add(type.key()).add(epochDay).random();
        for (int i = 0; i < 6; i++) {
            yang[i] = random.nextBoolean();
        }
        com.chenchen.yishu.liuyao.Hexagram hexagram = com.chenchen.yishu.liuyao.Hexagram.ofYao(yang);
        int moving = random.nextInt(3);
        return hexagram.auspice() > -2 && moving % 2 == 0;
    }

    /**
     * 激活遗迹。
     *
     * @param catalyst 玩家提供的属性
     * @return 是否激活成功；失败时会给出原因（由调用方展示）
     */
    public static Result activate(long worldSeed, DimensionProfile dimension, Site site,
                                  WuXing catalyst, long epochDay, boolean dragonDefeated) {
        RuinType type = site.type();

        if (!"dragon_altar_end".equals(type.key()) && type.catalyst() != catalyst
                && !type.catalyst().generates().equals(catalyst)) {
            return new Result(false, "此坛需以「" + type.catalyst().zh() + "」属性之物相引，你手上的不对。", List.of());
        }
        if ("dragon_altar_end".equals(type.key()) && !dragonDefeated) {
            return new Result(false, "龙气未散，末影龙尚在，此坛不启。", List.of());
        }
        if (!isOpenToday(worldSeed, dimension, type, epochDay)) {
            return new Result(false, "今日卦象不合，此机关不受。换个日子再来。", List.of());
        }

        BuildingFate.DailyState state = BuildingFate.dailyState(worldSeed, site.fate(), epochDay);
        List<String> rewards = new ArrayList<>();
        rewards.add(type.rewardZh());

        if (state == BuildingFate.DailyState.CALAMITOUS) {
            rewards.add("（劫气未散，收获减半）");
        }
        if (state == BuildingFate.DailyState.AUSPICIOUS) {
            rewards.add("（吉日加成，额外获得一份）");
        }
        return new Result(true, type.zh() + " 已启 —— " + type.mechanism(), List.copyOf(rewards));
    }

    /**
     * 激活结果。
     *
     * @param success 是否成功
     * @param message 给玩家的一句话
     * @param rewards 奖励清单
     */
    public record Result(boolean success, String message, List<String> rewards) {
    }

    // ==================================================================== 开关

    public static boolean enabled() {
        return YishuConfig.get().ruinsEnabled;
    }

    /** 遗迹在当前游戏年里的运势提示（六爻「寻遗迹」会用）。 */
    public static String fortune(long worldSeed, DimensionProfile dimension, long epochDay) {
        Site site = nearest(worldSeed, dimension, 0.0D, 0.0D);
        BuildingFate.DailyState state = BuildingFate.dailyState(worldSeed, site.fate(), epochDay);
        long year = epochDay / Yun.DAYS_PER_YEAR;
        BuildingFate.Calamity calamity = BuildingFate.Calamity.evaluate(worldSeed, site.fate(), epochDay);
        return site.type().zh() + " 今日 " + state.zh()
                + (calamity.active() ? "（渡劫中）" : "")
                + " · 第 " + year + " 年";
    }
}
