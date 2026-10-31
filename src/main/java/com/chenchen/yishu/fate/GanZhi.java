package com.chenchen.yishu.fate;

import java.util.List;

/**
 * 易枢 · 干支五行体系 —— 整套命理、六爻、风水共用的最底层语言。
 *
 * <p>这个文件把「东方术数的基本字母表」一次性定义清楚：
 * {@link WuXing 五行}、{@link YinYang 阴阳}、{@link Gan 天干}、{@link Zhi 地支}、
 * {@link Pair 干支}。上层的八字、大运、六亲、六神、风水属性全部由它们组合而成。
 *
 * <p>之所以全塞在一个文件里：这些类型彼此高度耦合（天干要知道自己的五行，
 * 地支要知道自己的藏干，干支要知道自己是六十甲子第几位），拆开反而要来回跳。
 */
public final class GanZhi {

    private GanZhi() {
    }

    // ==================================================================== 阴阳

    public enum YinYang {
        YANG("阳", "阳"),
        YIN("阴", "阴");

        private final String zh;
        private final String en;

        YinYang(String zh, String en) {
            this.zh = zh;
            this.en = en;
        }

        public String zh() {
            return this.zh;
        }

        public String en() {
            return this.en;
        }

        public YinYang flip() {
            return this == YANG ? YIN : YANG;
        }
    }

    // ==================================================================== 五行

    /**
     * 五行。
     *
     * <p>相生（{@link #generates()}）与相克（{@link #overcomes()}）是命理判断的主干：
     * 喜用神靠它推、六亲靠它排、风水五行偏向靠它配。
     */
    public enum WuXing {
        MU("木", "Wood", 0x4C9A4C),
        HUO("火", "Fire", 0xD0503C),
        TU("土", "Earth", 0xB08A40),
        JIN("金", "Metal", 0xC8C8D0),
        SHUI("水", "Water", 0x3E6FB0);

        private final String zh;
        private final String en;
        private final int color;

        WuXing(String zh, String en, int color) {
            this.zh = zh;
            this.en = en;
            this.color = color;
        }

        public String zh() {
            return this.zh;
        }

        public String en() {
            return this.en;
        }

        /** 用于界面显示的 ARGB 颜色。 */
        public int color() {
            return 0xFF000000 | this.color;
        }

        /** 我生者（木生火）。 */
        public WuXing generates() {
            return VALUES[(ordinal() + 1) % 5];
        }

        /** 生我者。 */
        public WuXing generatedBy() {
            return VALUES[(ordinal() + 4) % 5];
        }

        /** 我克者（木克土）。 */
        public WuXing overcomes() {
            return VALUES[(ordinal() + 2) % 5];
        }

        /** 克我者。 */
        public WuXing overcomeBy() {
            return VALUES[(ordinal() + 3) % 5];
        }

        public boolean supports(WuXing other) {
            return generates() == other;
        }

        public static final WuXing[] VALUES = values();

        public static WuXing byIndex(int index) {
            return VALUES[Math.floorMod(index, VALUES.length)];
        }
    }

    // ==================================================================== 天干

    /** 十天干。序号 0..9 = 甲..癸。 */
    public enum Gan {
        JIA("甲", "Jiǎ", WuXing.MU, YinYang.YANG),
        YI("乙", "Yǐ", WuXing.MU, YinYang.YIN),
        BING("丙", "Bǐng", WuXing.HUO, YinYang.YANG),
        DING("丁", "Dīng", WuXing.HUO, YinYang.YIN),
        WU("戊", "Wù", WuXing.TU, YinYang.YANG),
        JI("己", "Jǐ", WuXing.TU, YinYang.YIN),
        GENG("庚", "Gēng", WuXing.JIN, YinYang.YANG),
        XIN("辛", "Xīn", WuXing.JIN, YinYang.YIN),
        REN("壬", "Rén", WuXing.SHUI, YinYang.YANG),
        GUI("癸", "Guǐ", WuXing.SHUI, YinYang.YIN);

        private final String zh;
        private final String pinyin;
        private final WuXing wuXing;
        private final YinYang yinYang;

        Gan(String zh, String pinyin, WuXing wuXing, YinYang yinYang) {
            this.zh = zh;
            this.pinyin = pinyin;
            this.wuXing = wuXing;
            this.yinYang = yinYang;
        }

        public String zh() {
            return this.zh;
        }

        public String pinyin() {
            return this.pinyin;
        }

        public WuXing wuXing() {
            return this.wuXing;
        }

        public YinYang yinYang() {
            return this.yinYang;
        }

        /** 与另一个天干的关系名（用于十神）：同我 / 我生 / 我克 / 克我 / 生我。 */
        public Relation relationTo(Gan other) {
            return Relation.between(this, other);
        }

        public static final Gan[] VALUES = values();

        public static Gan byIndex(int index) {
            return VALUES[Math.floorMod(index, VALUES.length)];
        }

        public static Gan byName(String zh) {
            for (Gan gan : VALUES) {
                if (gan.zh.equals(zh)) {
                    return gan;
                }
            }
            return JIA;
        }
    }

    // ==================================================================== 地支

    /** 十二地支。序号 0..11 = 子..亥。 */
    public enum Zhi {
        ZI("子", "Zǐ", WuXing.SHUI, YinYang.YANG, "鼠", 23, 1),
        CHOU("丑", "Chǒu", WuXing.TU, YinYang.YIN, "牛", 1, 3),
        YIN("寅", "Yín", WuXing.MU, YinYang.YANG, "虎", 3, 5),
        MAO("卯", "Mǎo", WuXing.MU, YinYang.YIN, "兔", 5, 7),
        CHEN("辰", "Chén", WuXing.TU, YinYang.YANG, "龙", 7, 9),
        SI("巳", "Sì", WuXing.HUO, YinYang.YIN, "蛇", 9, 11),
        WU("午", "Wǔ", WuXing.HUO, YinYang.YANG, "马", 11, 13),
        WEI("未", "Wèi", WuXing.TU, YinYang.YIN, "羊", 13, 15),
        SHEN("申", "Shēn", WuXing.JIN, YinYang.YANG, "猴", 15, 17),
        YOU("酉", "Yǒu", WuXing.JIN, YinYang.YIN, "鸡", 17, 19),
        XU("戌", "Xū", WuXing.TU, YinYang.YANG, "狗", 19, 21),
        HAI("亥", "Hài", WuXing.SHUI, YinYang.YIN, "猪", 21, 23);

        private final String zh;
        private final String pinyin;
        private final WuXing wuXing;
        private final YinYang yinYang;
        private final String animal;
        private final int startHour;
        private final int endHour;

        Zhi(String zh, String pinyin, WuXing wuXing, YinYang yinYang,
            String animal, int startHour, int endHour) {
            this.zh = zh;
            this.pinyin = pinyin;
            this.wuXing = wuXing;
            this.yinYang = yinYang;
            this.animal = animal;
            this.startHour = startHour;
            this.endHour = endHour;
        }

        public String zh() {
            return this.zh;
        }

        public String pinyin() {
            return this.pinyin;
        }

        public WuXing wuXing() {
            return this.wuXing;
        }

        public YinYang yinYang() {
            return this.yinYang;
        }

        /** 生肖。 */
        public String animal() {
            return this.animal;
        }

        /** 该地支时辰的起始整点（24 小时制，子时跨日故为 23）。 */
        public int startHour() {
            return this.startHour;
        }

        /**
         * 藏干（本气在前）。
         *
         * <p>四柱推命里，地支不是「一个五行」而是一包天干 —— 这叫藏干。
         * 例如寅藏甲丙戊，所以寅月出生的人不只有木，还带着一点火和土的底子。
         * 判断旺衰与取用神时，藏干的权重（本气 0.6 / 中气 0.3 / 余气 0.1）非常关键。
         */
        public List<Gan> hiddenGan() {
            return switch (this) {
                case ZI -> List.of(Gan.GUI);
                case CHOU -> List.of(Gan.JI, Gan.GUI, Gan.XIN);
                case YIN -> List.of(Gan.JIA, Gan.BING, Gan.WU);
                case MAO -> List.of(Gan.YI);
                case CHEN -> List.of(Gan.WU, Gan.YI, Gan.GUI);
                case SI -> List.of(Gan.BING, Gan.WU, Gan.GENG);
                case WU -> List.of(Gan.DING, Gan.JI);
                case WEI -> List.of(Gan.JI, Gan.DING, Gan.YI);
                case SHEN -> List.of(Gan.GENG, Gan.REN, Gan.WU);
                case YOU -> List.of(Gan.XIN);
                case XU -> List.of(Gan.WU, Gan.XIN, Gan.DING);
                case HAI -> List.of(Gan.REN, Gan.JIA);
            };
        }

        /** 本气天干。 */
        public Gan primaryGan() {
            return hiddenGan().get(0);
        }

        /** 该地支是否与另一地支相冲（子午冲、丑未冲、寅申冲、卯酉冲、辰戌冲、巳亥冲）。 */
        public boolean clashes(Zhi other) {
            return Math.floorMod(other.ordinal() - ordinal(), 12) == 6;
        }

        /** 六合（子丑合、寅亥合、卯戌合、辰酉合、巳申合、午未合）。 */
        public boolean combines(Zhi other) {
            return Math.floorMod(ordinal() + other.ordinal(), 12) == 1;
        }

        /** 三合局成员判定：申子辰水、亥卯未木、寅午戌火、巳酉丑金。 */
        public WuXing trineGroup() {
            return switch (Math.floorMod(ordinal(), 12) % 4) {
                case 0 -> WuXing.SHUI;   // 子辰(申)系
                case 1 -> WuXing.JIN;
                case 2 -> WuXing.HUO;
                default -> WuXing.MU;
            };
        }

        public static final Zhi[] VALUES = values();

        public static Zhi byIndex(int index) {
            return VALUES[Math.floorMod(index, VALUES.length)];
        }

        /** 按 24 小时制的钟点取时辰地支（子时 23:00–00:59）。 */
        public static Zhi byHour(int hour) {
            return byIndex((Math.floorMod(hour, 24) + 1) / 2);
        }

        /** 该地支在十二时辰体系里的中文名，例如 {@code 子时}。 */
        public String hourName() {
            return this.zh + "时";
        }
    }

    // ==================================================================== 干支

    /**
     * 干支组合。
     *
     * <p>天干 10 个、地支 12 个，两两相配以 60 为周期循环（因为 10 与 12 的最小公倍数是 60），
     * 这就是「六十甲子」。{@link #cycleIndex()} 返回 0..59 的序号 ——
     * 它是整个模组里最常用的一个数字。
     */
    public record Pair(Gan gan, Zhi zhi) {

        private static final Pair[] SIXTY = new Pair[60];

        static {
            for (int i = 0; i < 60; i++) {
                SIXTY[i] = new Pair(Gan.byIndex(i), Zhi.byIndex(i));
            }
        }

        /** 六十甲子第 {@code index} 位（自动取模，负数也安全）。 */
        public static Pair of(int index) {
            return SIXTY[Math.floorMod(index, 60)];
        }

        public static Pair of(Gan gan, Zhi zhi) {
            return of(cycleIndexOf(gan, zhi));
        }

        /**
         * 反解干支在六十甲子中的序号。
         *
         * <p>用中国剩余定理：{@code t ≡ ganIndex (mod 10)} 且 {@code t ≡ zhiIndex (mod 12)}。
         * 天干与地支的奇偶性必须一致，否则是个不存在的组合 —— 这时回落到天干序号。
         */
        private static int cycleIndexOf(Gan gan, Zhi zhi) {
            int g = gan.ordinal();
            int z = zhi.ordinal();
            if ((g & 1) != (z & 1)) {
                return g;
            }
            for (int i = 0; i < 60; i++) {
                if (i % 10 == g && i % 12 == z) {
                    return i;
                }
            }
            return g;
        }

        /** 六十甲子序号 0..59。 */
        public int cycleIndex() {
            return cycleIndexOf(this.gan, this.zhi);
        }

        /** 纳音五行（六十甲子每两位一组，共三十种）。 */
        public WuXing nayin() {
            return NAYIN[cycleIndex() / 2];
        }

        /** 纳音名称，例如「海中金」。 */
        public String nayinName() {
            return NAYIN_NAMES[cycleIndex() / 2];
        }

        public WuXing wuXing() {
            return this.gan.wuXing();
        }

        public YinYang yinYang() {
            return this.gan.yinYang();
        }

        @Override
        public String toString() {
            return this.gan.zh() + this.zhi.zh();
        }

        /** 中文全称，例如「甲子」。 */
        public String zh() {
            return toString();
        }

        /** 带拼音的显示串，例如「甲子 (Jiǎ Zǐ)」。 */
        public String display() {
            return this.gan.zh() + this.zhi.zh() + " (" + this.gan.pinyin() + " " + this.zhi.pinyin() + ")";
        }
    }

    /**
     * 纳音五行表（三十组，按六十甲子每两柱一组）。
     *
     * <p>顺序：海中金、炉中火、大林木、路旁土、剑锋金、山头火、涧下水、城头土、白蜡金、杨柳木、
     * 泉中水、屋上土、霹雳火、松柏木、长流水、沙中金、山下火、平地木、壁上土、金箔金、
     * 覆灯火、天河水、大驿土、钗钏金、桑柘木、大溪水、沙中土、天上火、石榴木、大海水。
     */
    private static final WuXing[] NAYIN = {
            WuXing.JIN, WuXing.HUO, WuXing.MU, WuXing.TU, WuXing.JIN,
            WuXing.HUO, WuXing.SHUI, WuXing.TU, WuXing.JIN, WuXing.MU,
            WuXing.SHUI, WuXing.TU, WuXing.HUO, WuXing.MU, WuXing.SHUI,
            WuXing.JIN, WuXing.HUO, WuXing.MU, WuXing.TU, WuXing.JIN,
            WuXing.HUO, WuXing.SHUI, WuXing.TU, WuXing.JIN, WuXing.MU,
            WuXing.SHUI, WuXing.TU, WuXing.HUO, WuXing.MU, WuXing.SHUI
    };

    private static final String[] NAYIN_NAMES = {
            "海中金", "炉中火", "大林木", "路旁土", "剑锋金",
            "山头火", "涧下水", "城头土", "白蜡金", "杨柳木",
            "泉中水", "屋上土", "霹雳火", "松柏木", "长流水",
            "沙中金", "山下火", "平地木", "壁上土", "金箔金",
            "覆灯火", "天河水", "大驿土", "钗钏金", "桑柘木",
            "大溪水", "沙中土", "天上火", "石榴木", "大海水"
    };

    // ==================================================================== 十神关系

    /**
     * 天干之间的五种基本关系 —— 十神的骨架。
     *
     * <p>以「日主」为我，另一干与我的关系决定它是什么十神：
     * <ul>
     *   <li>{@link #SAME} 同我 → 比肩 / 劫财</li>
     *   <li>{@link #OUT} 我生 → 食神 / 伤官</li>
     *   <li>{@link #WEALTH} 我克 → 偏财 / 正财</li>
     *   <li>{@link #OFFICER} 克我 → 七杀 / 正官</li>
     *   <li>{@link #RESOURCE} 生我 → 偏印 / 正印</li>
     * </ul>
     * 阴阳相同为「偏」，阴阳相异为「正」。六爻里的六亲（父母 / 兄弟 / 子孙 / 妻财 / 官鬼）
     * 用的是同一套映射，只是少了「正 / 偏」的细分。
     */
    public enum Relation {
        SAME("同我", "比劫"),
        OUT("我生", "食伤"),
        WEALTH("我克", "财"),
        OFFICER("克我", "官杀"),
        RESOURCE("生我", "印");

        private final String relation;
        private final String group;

        Relation(String relation, String group) {
            this.relation = relation;
            this.group = group;
        }

        public String relation() {
            return this.relation;
        }

        public String group() {
            return this.group;
        }

        /** 计算「以 {@code self} 为我，{@code other} 对我而言是什么关系」。 */
        public static Relation between(Gan self, Gan other) {
            WuXing mine = self.wuXing();
            WuXing theirs = other.wuXing();
            if (mine == theirs) {
                return SAME;
            }
            if (mine.generates() == theirs) {
                return OUT;
            }
            if (mine.overcomes() == theirs) {
                return WEALTH;
            }
            if (theirs.overcomes() == mine) {
                return OFFICER;
            }
            return RESOURCE;
        }
    }
}
