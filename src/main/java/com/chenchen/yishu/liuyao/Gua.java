package com.chenchen.yishu.liuyao;

import com.chenchen.yishu.fate.GanZhi.Gan;
import com.chenchen.yishu.fate.GanZhi.WuXing;
import com.chenchen.yishu.fate.GanZhi.Zhi;

import java.util.List;

/**
 * 易枢 · 八卦与纳甲 —— 六爻装卦的两块基石。
 *
 * <p>六爻断卦的流程是：先摇出六个爻 → 组成上下两个三爻卦 → 由三爻卦查出
 * <b>纳甲</b>（每个爻配一个天干地支）→ 由干支五行与「卦宫五行」的关系定出<b>六亲</b>
 * → 再由日干定出<b>六神</b>。走完这一步，「卦」才变成一张能读的表。
 *
 * <p>本文件负责其中的三爻卦部分：
 * <ul>
 *   <li>{@link Trigram} 八卦本体与二进制编码；</li>
 *   <li>{@link #najia(Trigram, boolean)} 纳甲表；</li>
 *   <li>{@link LiuQin} 六亲、{@link LiuShen} 六神。</li>
 * </ul>
 *
 * <p><b>命名说明</b>：外层类叫 {@code Gua}（卦），内层枚举叫 {@link Trigram}（三爻卦／八卦）。
 * 两者刻意不重名 —— Java 里同名嵌套类型会遮蔽外层类型，写 {@code Gua.najia(...)}
 * 时编译器会把它当成枚举去找方法，直接报「找不到符号」。
 */
public final class Gua {

    private Gua() {
    }

    // ==================================================================== 八卦

    /**
     * 八卦（三爻卦）。
     *
     * <p>{@code bits} 是从<b>初爻（最下）</b>往上数的三爻：bit0 = 初爻、bit1 = 中爻、bit2 = 上爻，
     * 1 为阳、0 为阴。这样编码的好处是：爻位编号与二进制位号一致，
     * 「第 i 爻动」直接就是 {@code bits ^ (1 << i)}，不用来回换算。
     *
     * <p>{@code order} 是先天八卦数（乾一、兑二、离三、震四、巽五、坎六、艮七、坤八）。
     */
    public enum Trigram {
        QIAN("乾", "☰", 0b111, 1, WuXing.JIN, "天", "健"),
        DUI("兑", "☱", 0b011, 2, WuXing.JIN, "泽", "悦"),
        LI("离", "☲", 0b101, 3, WuXing.HUO, "火", "丽"),
        ZHEN("震", "☳", 0b001, 4, WuXing.MU, "雷", "动"),
        XUN("巽", "☴", 0b110, 5, WuXing.MU, "风", "入"),
        KAN("坎", "☵", 0b010, 6, WuXing.SHUI, "水", "陷"),
        GEN("艮", "☶", 0b100, 7, WuXing.TU, "山", "止"),
        KUN("坤", "☷", 0b000, 8, WuXing.TU, "地", "顺");

        private final String zh;
        private final String symbol;
        private final int bits;
        private final int order;
        private final WuXing wuXing;
        private final String image;
        private final String quality;

        Trigram(String zh, String symbol, int bits, int order, WuXing wuXing, String image, String quality) {
            this.zh = zh;
            this.symbol = symbol;
            this.bits = bits;
            this.order = order;
            this.wuXing = wuXing;
            this.image = image;
            this.quality = quality;
        }

        public String zh() {
            return this.zh;
        }

        /** Unicode 卦符，例如 ☰。 */
        public String symbol() {
            return this.symbol;
        }

        /** 三爻位掩码，bit0 为初爻。 */
        public int bits() {
            return this.bits;
        }

        public int order() {
            return this.order;
        }

        public WuXing wuXing() {
            return this.wuXing;
        }

        /** 卦象（天、泽、火……）。 */
        public String image() {
            return this.image;
        }

        /** 卦德（健、悦、丽……）。 */
        public String quality() {
            return this.quality;
        }

        /** 取第 {@code yao}（1..3）的阴阳：true 为阳。 */
        public boolean yangAt(int yao) {
            return ((this.bits >> (yao - 1)) & 1) != 0;
        }

        public static final Trigram[] VALUES = values();

        /** 由三爻位掩码反查。 */
        public static Trigram ofBits(int bits) {
            for (Trigram trigram : VALUES) {
                if (trigram.bits == (bits & 0b111)) {
                    return trigram;
                }
            }
            return KUN;
        }

        /**
         * 由三爻的阴阳反查。
         *
         * @param yao 长度 3，索引 0 为最下那一爻
         */
        public static Trigram fromYao(boolean[] yao) {
            int bits = 0;
            for (int i = 0; i < yao.length && i < 3; i++) {
                if (yao[i]) {
                    bits |= 1 << i;
                }
            }
            return ofBits(bits);
        }
    }

    // ==================================================================== 纳甲

    /** 一个爻的纳甲结果。 */
    public record NaJia(Gan gan, Zhi zhi) {

        public WuXing wuXing() {
            return this.zhi.wuXing();
        }

        public String zh() {
            return this.gan.zh() + this.zhi.zh();
        }
    }

    /**
     * 纳甲：给三爻卦的每一个爻配上天干地支。
     *
     * <p>这是六爻断卦里最容易被记错的一步，所以整张表直接写死在代码里，
     * 一行一卦，方便核对。规则是「乾纳甲壬、坤纳乙癸、震纳庚、巽纳辛、
     * 坎纳戊、离纳己、艮纳丙、兑纳丁」，而地支的排列有两种走法：
     * 阳卦（乾震坎艮）顺行，阴卦（坤巽离兑）逆行。
     *
     * @param trigram 三爻卦
     * @param outer   true 表示它在六爻卦里处于<b>外卦</b>（上三爻）位置
     * @return 从该卦初爻到三爻的干支，长度 3
     */
    public static NaJia[] najia(Trigram trigram, boolean outer) {
        return switch (trigram) {
            // 乾：内甲子甲寅甲辰 / 外壬午壬申壬戌
            case QIAN -> outer
                    ? new NaJia[]{na(Gan.REN, Zhi.WU), na(Gan.REN, Zhi.SHEN), na(Gan.REN, Zhi.XU)}
                    : new NaJia[]{na(Gan.JIA, Zhi.ZI), na(Gan.JIA, Zhi.YIN), na(Gan.JIA, Zhi.CHEN)};
            // 坤：内乙未乙巳乙卯 / 外癸丑癸亥癸酉
            case KUN -> outer
                    ? new NaJia[]{na(Gan.GUI, Zhi.CHOU), na(Gan.GUI, Zhi.HAI), na(Gan.GUI, Zhi.YOU)}
                    : new NaJia[]{na(Gan.YI, Zhi.WEI), na(Gan.YI, Zhi.SI), na(Gan.YI, Zhi.MAO)};
            // 震：庚，内外同干
            case ZHEN -> outer
                    ? new NaJia[]{na(Gan.GENG, Zhi.WU), na(Gan.GENG, Zhi.SHEN), na(Gan.GENG, Zhi.XU)}
                    : new NaJia[]{na(Gan.GENG, Zhi.ZI), na(Gan.GENG, Zhi.YIN), na(Gan.GENG, Zhi.CHEN)};
            // 巽：辛，内丑亥酉 / 外未巳卯
            case XUN -> outer
                    ? new NaJia[]{na(Gan.XIN, Zhi.WEI), na(Gan.XIN, Zhi.SI), na(Gan.XIN, Zhi.MAO)}
                    : new NaJia[]{na(Gan.XIN, Zhi.CHOU), na(Gan.XIN, Zhi.HAI), na(Gan.XIN, Zhi.YOU)};
            // 坎：戊，内寅辰午 / 外申戌子
            case KAN -> outer
                    ? new NaJia[]{na(Gan.WU, Zhi.SHEN), na(Gan.WU, Zhi.XU), na(Gan.WU, Zhi.ZI)}
                    : new NaJia[]{na(Gan.WU, Zhi.YIN), na(Gan.WU, Zhi.CHEN), na(Gan.WU, Zhi.WU)};
            // 离：己，内卯丑亥 / 外酉未巳
            case LI -> outer
                    ? new NaJia[]{na(Gan.JI, Zhi.YOU), na(Gan.JI, Zhi.WEI), na(Gan.JI, Zhi.SI)}
                    : new NaJia[]{na(Gan.JI, Zhi.MAO), na(Gan.JI, Zhi.CHOU), na(Gan.JI, Zhi.HAI)};
            // 艮：丙，内辰午申 / 外戌子寅
            case GEN -> outer
                    ? new NaJia[]{na(Gan.BING, Zhi.XU), na(Gan.BING, Zhi.ZI), na(Gan.BING, Zhi.YIN)}
                    : new NaJia[]{na(Gan.BING, Zhi.CHEN), na(Gan.BING, Zhi.WU), na(Gan.BING, Zhi.SHEN)};
            // 兑：丁，内巳卯丑 / 外亥酉未
            case DUI -> outer
                    ? new NaJia[]{na(Gan.DING, Zhi.HAI), na(Gan.DING, Zhi.YOU), na(Gan.DING, Zhi.WEI)}
                    : new NaJia[]{na(Gan.DING, Zhi.SI), na(Gan.DING, Zhi.MAO), na(Gan.DING, Zhi.CHOU)};
        };
    }

    private static NaJia na(Gan gan, Zhi zhi) {
        return new NaJia(gan, zhi);
    }

    // ==================================================================== 六亲

    /**
     * 六亲。
     *
     * <p>六亲是「卦里的这一爻，相对卦宫是什么身份」。判断方法与八字的十神同源，
     * 只是合并成了五类（少了正偏之分）：
     * <ul>
     *   <li>{@link #PARENT 父母} —— 生我者。主文书、庇护、消息、屋宅。</li>
     *   <li>{@link #BROTHER 兄弟} —— 同我者。主同辈、竞争、耗财。</li>
     *   <li>{@link #CHILD 子孙} —— 我生者。主福气、医药、解厄、牲畜。</li>
     *   <li>{@link #WEALTH 妻财} —— 我克者。主财物、妻妾、收获。</li>
     *   <li>{@link #OFFICER 官鬼} —— 克我者。主权柄、疾病、盗贼、争斗。</li>
     * </ul>
     */
    public enum LiuQin {
        PARENT("父母", "文书 · 庇护 · 消息"),
        BROTHER("兄弟", "同辈 · 竞争 · 耗散"),
        CHILD("子孙", "福荫 · 医药 · 解厄"),
        WEALTH("妻财", "财物 · 收获 · 妻妾"),
        OFFICER("官鬼", "权柄 · 疾病 · 争斗");

        private final String zh;
        private final String note;

        LiuQin(String zh, String note) {
            this.zh = zh;
            this.note = note;
        }

        public String zh() {
            return this.zh;
        }

        public String note() {
            return this.note;
        }

        /** 以 {@code palace} 为卦宫五行，判断 {@code yaoElement} 这一爻的六亲。 */
        public static LiuQin of(WuXing palace, WuXing yaoElement) {
            if (palace == yaoElement) {
                return BROTHER;
            }
            if (yaoElement.generates() == palace) {
                return PARENT;
            }
            if (palace.generates() == yaoElement) {
                return CHILD;
            }
            if (palace.overcomes() == yaoElement) {
                return WEALTH;
            }
            return OFFICER;
        }
    }

    // ==================================================================== 六神

    /**
     * 六神（六兽）。
     *
     * <p>六神由<b>日干</b>决定起点，然后按初爻到上爻的顺序循环排列。
     * 它给每一爻加一层「色彩」——同样是官鬼，配青龙是升迁，配白虎是刑伤。
     *
     * <p>三个维度各有一套名字（见规范第六节「维度差异」）：
     * <ul>
     *   <li>主世界：青龙 朱雀 勾陈 螣蛇 白虎 玄武</li>
     *   <li>下界：烈焰 灵魂 金铁 幽魂 玄武岩 熔岩</li>
     *   <li>末地：虚空 紫颂 龙息 末影 潜影 折跃</li>
     * </ul>
     */
    public enum LiuShen {
        QING_LONG("青龙", "主喜庆、财禄、酒色", 0x4FC3F7),
        ZHU_QUE("朱雀", "主口舌、文书、信息", 0xE57373),
        GOU_CHEN("勾陈", "主田土、迟滞、羁绊", 0xA1887F),
        TENG_SHE("螣蛇", "主惊恐、怪异、缠绕", 0xBA68C8),
        BAI_HU("白虎", "主凶伤、疾病、道路", 0xE0E0E0),
        XUAN_WU("玄武", "主暗昧、盗贼、水厄", 0x546E7A);

        private final String zh;
        private final String note;
        private final int color;

        LiuShen(String zh, String note, int color) {
            this.zh = zh;
            this.note = note;
            this.color = color;
        }

        public String zh() {
            return this.zh;
        }

        public String note() {
            return this.note;
        }

        public int color() {
            return 0xFF000000 | this.color;
        }

        public static final LiuShen[] VALUES = values();

        /**
         * 给一盘六爻排六神。
         *
         * @param dayGan    当日的日干（流日的天干）
         * @param dimension 维度键，决定用哪套显示名
         * @return 长度 6，索引 0 为初爻
         */
        public static LiuShen[] arrange(Gan dayGan, String dimension) {
            int start = switch (dayGan) {
                case JIA, YI -> 0;           // 甲乙起青龙
                case BING, DING -> 1;        // 丙丁起朱雀
                case WU -> 2;                // 戊起勾陈
                case JI -> 3;                // 己起螣蛇
                case GENG, XIN -> 4;         // 庚辛起白虎
                case REN, GUI -> 5;          // 壬癸起玄武
            };
            LiuShen[] result = new LiuShen[6];
            for (int yao = 0; yao < 6; yao++) {
                // 名字随维度变，位次不变 —— 维度差异由 displayName 处理
                result[yao] = VALUES[(start + yao) % 6];
            }
            return result;
        }

        /**
         * 该维度下的显示名。
         *
         * <p>下界与末地各有一套对应物，位次与主世界一一对应。
         */
        public String displayName(String dimension) {
            return switch (dimension) {
                case "the_nether" -> switch (this) {
                    case QING_LONG -> "烈焰";
                    case ZHU_QUE -> "灵魂";
                    case GOU_CHEN -> "金铁";
                    case TENG_SHE -> "幽魂";
                    case BAI_HU -> "玄武岩";
                    case XUAN_WU -> "熔岩";
                };
                case "the_end" -> switch (this) {
                    case QING_LONG -> "虚空";
                    case ZHU_QUE -> "紫颂";
                    case GOU_CHEN -> "龙息";
                    case TENG_SHE -> "末影";
                    case BAI_HU -> "潜影";
                    case XUAN_WU -> "折跃";
                };
                default -> this.zh;
            };
        }
    }

    // ==================================================================== 辅助

    /**
     * 爻与日辰的生克关系 —— 断吉凶最核心的一步。
     *
     * @return 描述字符串，例如「日辰生爻」
     */
    public static String relationName(WuXing yao, WuXing day) {
        if (yao == day) {
            return "比和";
        }
        if (day.generates() == yao) {
            return "日辰生爻";
        }
        if (yao.generates() == day) {
            return "爻生日辰";
        }
        if (day.overcomes() == yao) {
            return "日辰克爻";
        }
        return "爻克日辰";
    }

    /** 列出所有八卦，供界面画卦象。 */
    public static List<Trigram> all() {
        return List.of(Trigram.VALUES);
    }
}
