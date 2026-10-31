package com.chenchen.yishu.ziwei;

import com.chenchen.yishu.fate.Bazi;
import com.chenchen.yishu.fate.GanZhi.Gan;
import com.chenchen.yishu.fate.GanZhi.Pair;
import com.chenchen.yishu.fate.GanZhi.WuXing;
import com.chenchen.yishu.fate.GanZhi.Zhi;

import java.util.ArrayList;
import java.util.List;

/**
 * 紫微斗数 —— 十四主星落十二宫。
 *
 * <p>紫微斗数和八字是两条完全不同的路：八字看的是<b>五行生克</b>，
 * 紫微看的是<b>星曜落在哪一宫</b>。同样一个人，八字说他「身弱喜印」，
 * 紫微可能说他「命宫七杀、官禄破军」—— 一个讲气，一个讲位。
 *
 * <p>本实现覆盖排盘的主干：
 * <ol>
 *   <li><b>定命身宫</b>：寅起正月顺数到生月，再从生月宫起子时逆数到生时（命宫），
 *       顺数则是身宫。</li>
 *   <li><b>定五行局</b>：命宫干支纳音 → 水二局 / 木三局 / 金四局 / 土五局 / 火六局。</li>
 *   <li><b>起紫微</b>：以局数与生日定紫微星宫位（商余法：余数为偶则顺、为奇则逆）。</li>
 *   <li><b>安十四主星</b>：紫微系逆行六星、天府系顺行八星。</li>
 *   <li><b>布十二宫</b>：命宫起逆布兄弟、夫妻、子女、财帛、疾厄、迁移、交友、官禄、田宅、福德、父母。</li>
 * </ol>
 *
 * <p>生日用<b>八字反推</b>（日干支序号取模三十），而不是真农历 ——
 * 游戏里没有农历历法，用八字反推能保证「同一个命盘永远得到同一张紫微盘」，
 * 这条可复现性比历法精度更重要。
 */
public final class ZiWei {

    private ZiWei() {
    }

    // ==================================================================== 星曜

    /** 十四主星。{@code order} 是安星顺序，仅用于展示时的稳定排序。 */
    public enum Star {
        ZIWEI("紫微", "帝座。主尊贵、主宰，落命宫则气度自成。", 0),
        TIANJI("天机", "谋略之星。主思虑、机变，多谋而善动。", 1),
        TAIYANG("太阳", "光耀之星。主名声、施予，落陷则劳而少功。", 2),
        WUQU("武曲", "财帛之星。主刚毅、执行，宜实不宜虚。", 3),
        TIANTONG("天同", "福德之星。主安逸、享受，最忌与煞同宫。", 4),
        LIANZHEN("廉贞", "囚星。主纪律、边界，得用则清正，失用则偏激。", 5),
        TIANFU("天府", "库星。主积蓄、包容，是十四星里最稳的一颗。", 6),
        TAIYIN("太阴", "月之精。主内敛、细腻，夜生人尤得其力。", 7),
        TANLANG("贪狼", "欲望之星。主才艺、交际，得制则成大器。", 8),
        JUMEN("巨门", "暗星。主口舌、思辨，善辩者得之如利器。", 9),
        TIANXIANG("天相", "印星。主辅佐、平衡，为众星之佐。", 10),
        TIANLIANG("天梁", "荫星。主庇护、寿元，最喜落于福德。", 11),
        QISHA("七杀", "将星。主决断、肃杀，成败皆在一念之间。", 12),
        POJUN("破军", "耗星。主破旧立新，动荡之中见真章。", 13);

        private final String zh;
        private final String note;
        private final int order;

        Star(String zh, String note, int order) {
            this.zh = zh;
            this.note = note;
            this.order = order;
        }

        public String zh() {
            return this.zh;
        }

        public String note() {
            return this.note;
        }

        public int order() {
            return this.order;
        }

        public static final Star[] VALUES = values();
    }

    /** 十二宫的宫名（从命宫起逆布）。 */
    public static final String[] PALACE_NAMES = {
            "命宫", "兄弟", "夫妻", "子女", "财帛", "疾厄",
            "迁移", "交友", "官禄", "田宅", "福德", "父母"
    };

    /** 五行局：局数决定起紫微的步长。 */
    public enum Bureau {
        WATER(2, "水二局"), WOOD(3, "木三局"), METAL(4, "金四局"),
        EARTH(5, "土五局"), FIRE(6, "火六局");

        private final int steps;
        private final String zh;

        Bureau(int steps, String zh) {
            this.steps = steps;
            this.zh = zh;
        }

        public int steps() {
            return this.steps;
        }

        public String zh() {
            return this.zh;
        }

        public static Bureau byElement(WuXing element) {
            return switch (element) {
                case SHUI -> WATER;
                case MU -> WOOD;
                case JIN -> METAL;
                case TU -> EARTH;
                case HUO -> FIRE;
            };
        }
    }

    // ==================================================================== 排盘

    /**
     * 一张紫微命盘。
     *
     * @param lifeBranch   命宫地支（0=子 … 11=亥）
     * @param bodyBranch   身宫地支
     * @param bureau       五行局
     * @param ziweiBranch  紫微星所在宫
     * @param palaces      十二宫，按「命宫、兄弟、夫妻 …」顺序
     * @param lifeMaster   命主（按命宫地支）
     * @param bodyMaster   身主（按生年支）
     * @param lunarMonth   排盘用的「月」（1~12，由月建反推）
     * @param lunarDay     排盘用的「日」（1~30，由日干支反推）
     * @param hourBranch   生时地支
     * @param yearPillar   生年干支（用于展示）
     */
    public record Chart(int lifeBranch, int bodyBranch, Bureau bureau, int ziweiBranch,
                        List<Palace> palaces, String lifeMaster, String bodyMaster,
                        int lunarMonth, int lunarDay, int hourBranch, Pair yearPillar) {

        /** 命宫那一格。 */
        public Palace lifePalace() {
            return this.palaces.get(0);
        }

        /** 命宫主星（取排序最靠前的一颗；空宫返回 null）。 */
        public Star mainStar() {
            List<Star> stars = lifePalace().stars();
            return stars.isEmpty() ? null : stars.get(0);
        }

        /** 一句话概括这张盘的气质。 */
        public String tone() {
            Star star = mainStar();
            if (star == null) {
                return "命宫无主星 —— 借对宫之力，一生随境而转，反而不受一处所限。";
            }
            return "命宫坐 " + star.zh() + "：" + star.note();
        }

        /** 命宫若有双星同宫，这是第二颗。 */
        public Star secondStar() {
            List<Star> stars = lifePalace().stars();
            return stars.size() > 1 ? stars.get(1) : null;
        }
    }

    /** 一宫。 */
    public record Palace(int branch, String name, List<Star> stars) {

        public String branchName() {
            return Zhi.byIndex(this.branch).zh();
        }

        /** 宫内星曜的名字串（空宫显示「空」）。 */
        public String starText() {
            if (this.stars.isEmpty()) {
                return "空";
            }
            StringBuilder sb = new StringBuilder();
            for (Star star : this.stars) {
                if (sb.length() > 0) {
                    sb.append(' ');
                }
                sb.append(star.zh());
            }
            return sb.toString();
        }
    }

    /**
     * 由一张八字排出紫微盘。
     *
     * <p>同一个人（同八字）永远得到同一张盘 —— 这是铁律。
     */
    public static Chart cast(Bazi bazi) {
        Gan yearGan = bazi.year().gan();
        Zhi yearZhi = bazi.year().zhi();
        Zhi monthZhi = bazi.month().zhi();
        Zhi hourZhi = bazi.hour().zhi();

        // ---- 月、日、时 ----
        // 月建以寅为正月，所以「月」= 月支相对寅的偏移 + 1
        int month = Math.floorMod(monthZhi.ordinal() - Zhi.YIN.ordinal(), 12) + 1;
        // 日：用日干支序号取模三十，落在 1~30
        int day = Math.floorMod(bazi.day().cycleIndex(), 30) + 1;
        int hour = hourZhi.ordinal();

        // ---- 命宫 / 身宫 ----
        // 寅起正月顺数到生月，再从生月宫起子时逆数到生时
        int life = Math.floorMod(Zhi.YIN.ordinal() + (month - 1) - hour, 12);
        int body = Math.floorMod(Zhi.YIN.ordinal() + (month - 1) + hour, 12);

        // ---- 命宫天干（五虎遁） ----
        // 甲己之年丙作首，乙庚之岁戊为头，丙辛必定寻庚起，丁壬壬位顺行流，戊癸之年甲寅求
        int tigerGan = (Math.floorMod(yearGan.ordinal(), 5) * 2 + 2) % 10;
        int lifeGan = Math.floorMod(tigerGan + Math.floorMod(life - Zhi.YIN.ordinal(), 12), 10);

        // ---- 五行局：命宫干支纳音 ----
        Bureau bureau = Bureau.byElement(Pair.of(Gan.byIndex(lifeGan), Zhi.byIndex(life)).nayin());

        // ---- 起紫微：商余法 ----
        int steps = bureau.steps();
        int quotient = (day + steps - 1) / steps;       // 向上取整
        int remainder = quotient * steps - day;
        int ziwei = (remainder & 1) == 0
                ? Math.floorMod(Zhi.YIN.ordinal() + (quotient - 1) + remainder, 12)
                : Math.floorMod(Zhi.YIN.ordinal() + (quotient - 1) - remainder, 12);

        // ---- 安十四主星 ----
        int[] byBranch = new int[12];   // 该宫的主星数量（用于排序稳定性）
        List<List<Star>> slots = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            slots.add(new ArrayList<>());
        }

        // 紫微系：逆行
        place(slots, ziwei, 0, Star.ZIWEI);
        place(slots, ziwei, -1, Star.TIANJI);
        place(slots, ziwei, -3, Star.TAIYANG);
        place(slots, ziwei, -4, Star.WUQU);
        place(slots, ziwei, -5, Star.TIANTONG);
        place(slots, ziwei, -8, Star.LIANZHEN);

        // 天府：与紫微关于寅申轴对称
        int tianfu = Math.floorMod(4 - ziwei, 12);
        // 天府系：顺行
        place(slots, tianfu, 0, Star.TIANFU);
        place(slots, tianfu, 1, Star.TAIYIN);
        place(slots, tianfu, 2, Star.TANLANG);
        place(slots, tianfu, 3, Star.JUMEN);
        place(slots, tianfu, 4, Star.TIANXIANG);
        place(slots, tianfu, 5, Star.TIANLIANG);
        place(slots, tianfu, 6, Star.QISHA);
        place(slots, tianfu, 10, Star.POJUN);

        // ---- 布十二宫 ----
        List<Palace> palaces = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            int branch = Math.floorMod(life - i, 12);
            List<Star> stars = slots.get(branch);
            stars.sort((a, b) -> Integer.compare(a.order(), b.order()));
            palaces.add(new Palace(branch, PALACE_NAMES[i], List.copyOf(stars)));
        }

        return new Chart(life, body, bureau, ziwei, List.copyOf(palaces),
                lifeMaster(life), bodyMaster(yearZhi), month, day, hour, bazi.year());
    }

    /** 把一颗星安到「基准宫 + 偏移」上。 */
    private static void place(List<List<Star>> slots, int base, int offset, Star star) {
        slots.get(Math.floorMod(base + offset, 12)).add(star);
    }

    // ==================================================================== 命主 / 身主

    /** 命主：按命宫地支。禄存与文曲不在十四主星之列，但传统命主表里确实有它们。 */
    public static String lifeMaster(int branch) {
        return switch (branch) {
            case 0 -> "贪狼";
            case 1 -> "巨门";
            case 2 -> "禄存";
            case 3 -> "文曲";
            case 4 -> "廉贞";
            case 5 -> "武曲";
            case 6 -> "破军";
            case 7 -> "武曲";
            case 8 -> "廉贞";
            case 9 -> "文曲";
            case 10 -> "禄存";
            default -> "巨门";
        };
    }

    /** 身主：按生年地支。 */
    public static String bodyMaster(Zhi yearZhi) {
        return switch (yearZhi) {
            case ZI -> "火星";
            case CHOU -> "天相";
            case YIN -> "天梁";
            case MAO -> "天同";
            case CHEN -> "文昌";
            case SI -> "天机";
            case WU -> "火星";
            case WEI -> "天相";
            case SHEN -> "天梁";
            case YOU -> "天同";
            case XU -> "文昌";
            case HAI -> "天机";
        };
    }

    // ==================================================================== 四种「力」

    /**
     * 把十二宫压缩成四个维度，用于给游戏内的短期状态效果。
     *
     * <p>这不是传统斗数的判词，而是把「宫位强不强」翻译成玩家能感知的四件事：
     * <b>格局</b>（抗性）、<b>财帛</b>（幸运）、<b>官禄</b>（力量）、<b>迁移</b>（速度）。
     */
    public record Strength(int structure, int wealth, int career, int travel) {

        public String best() {
            int max = Math.max(Math.max(this.structure, this.wealth), Math.max(this.career, this.travel));
            if (max == this.structure) {
                return "格局";
            }
            if (max == this.wealth) {
                return "财帛";
            }
            if (max == this.career) {
                return "官禄";
            }
            return "迁移";
        }
    }

    /** 计算四个维度的强弱（各 0~100）。 */
    public static Strength strength(Chart chart) {
        int structure = 0;
        int wealth = 0;
        int career = 0;
        int travel = 0;
        for (Palace palace : chart.palaces()) {
            int power = 0;
            for (Star star : palace.stars()) {
                // 帝座与库星权重最高，杀破狼次之
                power += switch (star) {
                    case ZIWEI, TIANFU -> 34;
                    case QISHA, POJUN, TANLANG -> 26;
                    case TIANLIANG, TIANXIANG -> 22;
                    default -> 18;
                };
            }
            if (power == 0) {
                continue;
            }
            switch (palace.name()) {
                case "命宫", "福德" -> structure += power;
                case "财帛", "田宅" -> wealth += power;
                case "官禄" -> career += power;
                case "迁移", "交友" -> travel += power;
                default -> {
                    structure += power / 3;
                    wealth += power / 3;
                    career += power / 3;
                }
            }
        }
        return new Strength(clamp(structure), clamp(wealth), clamp(career), clamp(travel));
    }

    private static int clamp(int value) {
        return Math.max(0, Math.min(100, value));
    }

    // ==================================================================== 渲染

    /** 把命盘排成传统的四方盘（上南下北、左东右西的逆时针布局）。 */
    public static List<String> render(Chart chart) {
        List<String> lines = new ArrayList<>();
        lines.add("── 紫微命盘 ──");
        lines.add("生年 " + chart.yearPillar().toString()
                + " ｜ 月 " + chart.lunarMonth() + " 日 " + chart.lunarDay()
                + " ｜ 生时 " + Zhi.byIndex(chart.hourBranch()).zh() + "时");
        lines.add("命宫 " + Zhi.byIndex(chart.lifeBranch()).zh()
                + " ｜ 身宫 " + Zhi.byIndex(chart.bodyBranch()).zh()
                + " ｜ " + chart.bureau().zh());
        lines.add("命主 " + chart.lifeMaster() + " ｜ 身主 " + chart.bodyMaster());
        lines.add("");

        // 宫格：上方 巳午未申 / 左列 辰卯寅 / 下方 亥戌酉？—— 采用标准盘式
        //   巳  午  未  申
        //   辰          酉
        //   卯          戌
        //   寅  丑  子  亥
        int[][] layout = {
                {5, 6, 7, 8},
                {4, -1, -1, 9},
                {3, -1, -1, 10},
                {2, 1, 0, 11},
        };
        for (int[] row : layout) {
            List<String> cells = new ArrayList<>();
            for (int branch : row) {
                if (branch < 0) {
                    cells.add("            ");
                    continue;
                }
                Palace palace = palaceAt(chart, branch);
                String label = Zhi.byIndex(branch).zh() + palace.name();
                cells.add(pad(label, 6) + pad(palace.starText(), 8));
            }
            lines.add(joinCells(cells));
        }

        lines.add("");
        lines.add("── 解读 ──");
        lines.add(chart.tone());
        Star second = chart.secondStar();
        if (second != null) {
            lines.add("双星同宫：" + second.zh() + " —— " + second.note());
        }
        Strength strength = strength(chart);
        lines.add("格局 " + bar(strength.structure()) + " ｜ 财帛 " + bar(strength.wealth()));
        lines.add("官禄 " + bar(strength.career()) + " ｜ 迁移 " + bar(strength.travel()));
        lines.add("此盘最盛在「" + strength.best() + "」。");
        return lines;
    }

    private static Palace palaceAt(Chart chart, int branch) {
        for (Palace palace : chart.palaces()) {
            if (palace.branch() == branch) {
                return palace;
            }
        }
        return new Palace(branch, "　", List.of());
    }

    /** 固定宽度填充（按字符数，中文也算一列 —— 面板用的是等宽字体近似）。 */
    private static String pad(String text, int width) {
        StringBuilder sb = new StringBuilder(text);
        while (sb.length() < width) {
            sb.append(' ');
        }
        return sb.length() > width ? sb.substring(0, width) : sb.toString();
    }

    private static String joinCells(List<String> cells) {
        StringBuilder sb = new StringBuilder("│");
        for (String cell : cells) {
            sb.append(cell).append('│');
        }
        return sb.toString();
    }

    /** 把 0~100 画成十个方块。 */
    public static String bar(int value) {
        int filled = Math.max(0, Math.min(10, Math.round(value / 10.0F)));
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 10; i++) {
            sb.append(i < filled ? '█' : '░');
        }
        return sb.toString();
    }

    /** 供文档/指令使用的一句话摘要。 */
    public static String summary(Chart chart) {
        return "紫微在" + Zhi.byIndex(chart.ziweiBranch()).zh() + "宫，"
                + chart.bureau().zh() + "，命主" + chart.lifeMaster()
                + "，身主" + chart.bodyMaster();
    }
}
