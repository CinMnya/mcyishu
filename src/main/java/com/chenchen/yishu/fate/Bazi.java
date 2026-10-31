package com.chenchen.yishu.fate;

import com.chenchen.yishu.core.YishuRandom;
import com.chenchen.yishu.fate.GanZhi.Gan;
import com.chenchen.yishu.fate.GanZhi.Pair;
import com.chenchen.yishu.fate.GanZhi.Relation;
import com.chenchen.yishu.fate.GanZhi.WuXing;
import com.chenchen.yishu.fate.GanZhi.Zhi;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 易枢 · 八字 —— 一个人（或一个生物、一座建筑）的「命运指纹」。
 *
 * <p>四柱八字 = 年柱 + 月柱 + 日柱 + 时柱，每柱一个天干一个地支，共八个字。
 * 其中<b>日干</b>是核心，叫「日主」或「日元」，代表「我」本身。其余七个字
 * 都是相对日主而言的环境 —— 谁在生我、谁在克我、谁在消耗我，全靠它们。
 *
 * <p>本类做三件事：
 * <ol>
 *   <li>{@link #chart()} —— 把八个字换算成五行权重（含地支藏干，月令加权）；</li>
 *   <li>{@link #strength()} —— 判定身强身弱；</li>
 *   <li>{@link #yongShen()} —— 推出喜用神与忌神，这是模组里「环境增益 / 减益」的依据。</li>
 * </ol>
 *
 * <p><b>关于「同种子同结果」</b>：八字本身不带随机性 —— 它是从一个虚拟生辰推导出来的。
 * 而那个虚拟生辰由 {@code worldSeed + 主体标识} 经稳定哈希决定，见 {@link #forSubject}。
 * 因此同一种子下，同一个玩家的八字永远不变，并且可以被别人复现出来。
 */
public record Bazi(Pair year, Pair month, Pair day, Pair hour) {

    /** 日主：日柱的天干，代表命主本身。 */
    public Gan dayMaster() {
        return this.day.gan();
    }

    public WuXing dayMasterElement() {
        return dayMaster().wuXing();
    }

    @Override
    public String toString() {
        return this.year + " " + this.month + " " + this.day + " " + this.hour;
    }

    /** 四柱的排列顺序，界面上按这个顺序画。 */
    public List<Pillar> pillars() {
        return List.of(
                new Pillar("年柱", this.year, "祖上 · 少年 · 根基"),
                new Pillar("月柱", this.month, "父母 · 青年 · 事业"),
                new Pillar("日柱", this.day, "自身 · 中年 · 婚姻"),
                new Pillar("时柱", this.hour, "子女 · 晚年 · 归宿")
        );
    }

    /** 一柱。 */
    public record Pillar(String label, Pair ganZhi, String meaning) {
    }

    // ==================================================================== 生成

    /**
     * 为一个「主体」推导八字。
     *
     * <p>{@code subject} 可以是玩家名、生物个体键、建筑坐标键 —— 任何稳定的字符串。
     * 同一个 {@code seed + subject} 永远得到同一个八字。
     *
     * @param seed    世界种子
     * @param subject 稳定主体标识（<b>不要传 UUID</b>，用名字或坐标）
     */
    public static Bazi forSubject(long seed, String subject) {
        YishuRandom random = com.chenchen.yishu.core.StableKey.of(seed, "bazi")
                .add(subject)
                .random();
        // 虚拟生辰：年份跨度取 1900..2059（正好一个六十甲子循环，保证年柱分布均匀）
        int year = FateCalendar.MIN_YEAR + random.nextInt(160);
        int month = random.nextInt(1, 12);
        int day = random.nextInt(1, 28);          // 统一用 28 天，回避月长差异
        int hour = random.nextInt(0, 23);
        return of(year, month, day, hour);
    }

    /**
     * 从一个明确的生辰推四柱。
     *
     * <p>配置项「允许输入生辰」打开后，玩家可以在命盘界面上填进去自己的真实生日；
     * 这条路径与 {@link #forSubject} 共用同一套历法，所以结果依然完全可复现。
     */
    public static Bazi of(int year, int month, int day, int hour) {
        Pair yearPillar = FateCalendar.yearPillar(year);
        Pair monthPillar = FateCalendar.monthPillar(year, month);
        Pair dayPillar = FateCalendar.dayPillar(year, month, day);
        Pair hourPillar = FateCalendar.hourPillar(dayPillar, hour);
        return new Bazi(yearPillar, monthPillar, dayPillar, hourPillar);
    }

    // ==================================================================== 十神

    /**
     * 某天干相对日主的十神名。
     *
     * <p>十神是命理里描述「关系」的标准词表，六个字就能说清一个人和周边世界的关系。
     */
    public String tenGod(Gan other) {
        Relation relation = Relation.between(dayMaster(), other);
        boolean samePolarity = dayMaster().yinYang() == other.yinYang();
        return switch (relation) {
            case SAME -> samePolarity ? "比肩" : "劫财";
            case OUT -> samePolarity ? "食神" : "伤官";
            case WEALTH -> samePolarity ? "偏财" : "正财";
            case OFFICER -> samePolarity ? "七杀" : "正官";
            case RESOURCE -> samePolarity ? "偏印" : "正印";
        };
    }

    /** 十神的五行归属（用于界面配色）。 */
    public WuXing tenGodElement(Gan other) {
        return other.wuXing();
    }

    // ==================================================================== 五行权重

    /**
     * 五行权重表。
     *
     * <p>算法说明（写在代码里，因为这是整套命理最容易被质疑的一步）：
     * <ul>
     *   <li>天干：按柱位加权 —— 年 0.8 / 月 1.2 / 日 1.0 / 时 0.8。
     *       <b>月令最重</b>，因为月柱代表出生时的季节，是「气的来源」。</li>
     *   <li>地支：不按单个五行算，而要展开成<b>藏干</b>，按本气 0.6 / 中气 0.3 / 余气 0.1 分配，
     *       再乘上同样的柱位权重。</li>
     *   <li>日主自身也计入（1.0），否则「身强身弱」会缺少基准。</li>
     * </ul>
     * 这套权重是术数界的常见近似，本模组取它的目的是<b>可解释、可复现</b>，
     * 而不是与任何一本古籍的数值逐位对齐。
     */
    public Map<WuXing, Double> chart() {
        Map<WuXing, Double> scores = new EnumMap<>(WuXing.class);
        for (WuXing element : WuXing.VALUES) {
            scores.put(element, 0.0D);
        }
        addGan(scores, this.year.gan(), 0.8D);
        addGan(scores, this.month.gan(), 1.2D);
        addGan(scores, this.day.gan(), 1.0D);
        addGan(scores, this.hour.gan(), 0.8D);

        addZhi(scores, this.year.zhi(), 0.8D);
        addZhi(scores, this.month.zhi(), 1.2D);
        addZhi(scores, this.day.zhi(), 1.0D);
        addZhi(scores, this.hour.zhi(), 0.8D);
        return scores;
    }

    private static void addGan(Map<WuXing, Double> scores, Gan gan, double weight) {
        scores.merge(gan.wuXing(), weight, Double::sum);
    }

    private static void addZhi(Map<WuXing, Double> scores, Zhi zhi, double weight) {
        List<Gan> hidden = zhi.hiddenGan();
        double[] ratio = {0.6D, 0.3D, 0.1D};
        for (int i = 0; i < hidden.size(); i++) {
            double share = i < ratio.length ? ratio[i] : 0.1D;
            scores.merge(hidden.get(i).wuXing(), weight * share, Double::sum);
        }
    }

    // ==================================================================== 旺衰

    /** 身强身弱。 */
    public enum Strength {
        STRONG("身强", "日主得令得地，气盛有余"),
        BALANCED("中和", "日主不亢不卑，五行流通"),
        WEAK("身弱", "日主失令少助，气薄待扶");

        private final String zh;
        private final String note;

        Strength(String zh, String note) {
            this.zh = zh;
            this.note = note;
        }

        public String zh() {
            return this.zh;
        }

        public String note() {
            return this.note;
        }
    }

    /**
     * 判定身强身弱。
     *
     * <p>比较「帮我的力量」（比劫 + 印）与「耗我的力量」（食伤 + 财 + 官杀）。
     * 比值落在 0.85~1.18 之间算「中和」—— 这个区间是刻意留宽的，
     * 因为模组的目的是让大多数玩家处在「有优有劣」的可玩区间，
     * 而不是把所有人推到两个极端。
     */
    public Strength strength() {
        Map<WuXing, Double> scores = chart();
        WuXing me = dayMasterElement();
        double support = scores.get(me) + scores.get(me.generatedBy());
        double drain = scores.get(me.generates()) + scores.get(me.overcomes()) + scores.get(me.overcomeBy());
        if (drain <= 0.0D) {
            return Strength.STRONG;
        }
        double ratio = support / drain;
        if (ratio >= 1.18D) {
            return Strength.STRONG;
        }
        if (ratio <= 0.85D) {
            return Strength.WEAK;
        }
        return Strength.BALANCED;
    }

    /** 帮身 / 耗身的力量比，界面上的「日主强度」条用它。 */
    public double strengthRatio() {
        Map<WuXing, Double> scores = chart();
        WuXing me = dayMasterElement();
        double support = scores.get(me) + scores.get(me.generatedBy());
        double drain = scores.get(me.generates()) + scores.get(me.overcomes()) + scores.get(me.overcomeBy());
        return drain <= 0.0D ? 9.99D : support / drain;
    }

    // ==================================================================== 用神

    /**
     * 喜用神 / 忌神。
     *
     * <p>取用神走两步：
     * <ol>
     *   <li><b>扶抑</b>：身强则克泄耗（食伤、财、官杀）为喜；身弱则生扶（印、比劫）为喜。</li>
     *   <li><b>调候</b>：冬月（亥子丑）出生缺水之外的暖意，火为喜；夏月（巳午未）燥烈，水为喜。
     *       调候只在扶抑结论之外补一个「次喜」，不推翻主判 —— 这是保守但有据的处理。</li>
     * </ol>
     */
    public record YongShen(WuXing primary, List<WuXing> favorable, List<WuXing> unfavorable, String reason) {

        public boolean isFavorable(WuXing element) {
            return this.favorable.contains(element);
        }

        public boolean isUnfavorable(WuXing element) {
            return this.unfavorable.contains(element);
        }

        /**
         * 对指定五行的环境增益系数。
         *
         * <p>主用神 ×1.30、次喜 ×1.15、忌神 ×0.78、其余 1.00。
         * 这几条系数直接决定「站在什么环境里回血快、作物长得快、运气好」。
         */
        public double multiplier(WuXing element) {
            if (element == this.primary) {
                return 1.30D;
            }
            if (this.favorable.contains(element)) {
                return 1.15D;
            }
            if (this.unfavorable.contains(element)) {
                return 0.78D;
            }
            return 1.00D;
        }
    }

    public YongShen yongShen() {
        Map<WuXing, Double> scores = chart();
        WuXing me = dayMasterElement();
        Strength strength = strength();

        List<WuXing> favorable = new ArrayList<>();
        List<WuXing> unfavorable = new ArrayList<>();

        if (strength == Strength.WEAK) {
            favorable.add(me.generatedBy());   // 印
            favorable.add(me);                 // 比劫
            unfavorable.add(me.overcomes());   // 财
            unfavorable.add(me.overcomeBy());  // 官杀
            unfavorable.add(me.generates());   // 食伤
        } else if (strength == Strength.STRONG) {
            favorable.add(me.generates());     // 食伤
            favorable.add(me.overcomes());     // 财
            favorable.add(me.overcomeBy());    // 官杀
            unfavorable.add(me.generatedBy()); // 印
            unfavorable.add(me);               // 比劫
        } else {
            // 中和：谁弱补谁 —— 找出权重最低的两行作为喜神
            List<WuXing> sorted = new ArrayList<>(List.of(WuXing.VALUES));
            sorted.sort((a, b) -> Double.compare(scores.get(a), scores.get(b)));
            favorable.add(sorted.get(0));
            favorable.add(sorted.get(1));
            unfavorable.add(sorted.get(4));
        }

        // ---- 调候补充 ----
        boolean winter = this.month.zhi() == Zhi.HAI || this.month.zhi() == Zhi.ZI || this.month.zhi() == Zhi.CHOU;
        boolean summer = this.month.zhi() == Zhi.SI || this.month.zhi() == Zhi.WU || this.month.zhi() == Zhi.WEI;
        String seasonNote;
        if (winter) {
            seasonNote = "冬月生人，天寒地冻，喜火调候";
            if (!favorable.contains(WuXing.HUO)) {
                favorable.add(WuXing.HUO);
            }
        } else if (summer) {
            seasonNote = "夏月生人，火燥土裂，喜水润局";
            if (!favorable.contains(WuXing.SHUI)) {
                favorable.add(WuXing.SHUI);
            }
        } else {
            seasonNote = "春生木旺、秋生金清，气候中和，以扶抑为主";
        }

        WuXing primary = favorable.isEmpty() ? me.generates() : favorable.get(0);
        String reason = strength.zh() + " —— " + strength.note() + "；" + seasonNote;
        return new YongShen(primary, List.copyOf(favorable), List.copyOf(unfavorable), reason);
    }

    // ==================================================================== 摘要

    /** 一行式摘要，用于物品 tooltip 与聊天提示。 */
    public String summary() {
        YongShen yong = yongShen();
        return "日主" + dayMaster().zh() + dayMasterElement().zh()
                + " · " + strength().zh()
                + " · 喜" + yong.primary().zh()
                + " · " + this.year + " " + this.month + " " + this.day + " " + this.hour;
    }
}
