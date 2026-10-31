package com.chenchen.yishu.qimen;

import com.chenchen.yishu.fate.FateCalendar;
import com.chenchen.yishu.fate.GanZhi.Gan;
import com.chenchen.yishu.fate.GanZhi.Pair;

import java.util.ArrayList;
import java.util.List;

/**
 * 奇门遁甲 —— 九宫、八门、九星、八神。
 *
 * <p>奇门是三大术数里最「工程化」的一门：它把时间和空间叠在一张九宫格上，
 * 于是「什么时候、往哪个方向去、做什么」变成了一道可以查表的题。
 * 六壬问「事怎么走」，奇门问「<b>往哪走</b>」。
 *
 * <p>本实现覆盖：
 * <ol>
 *   <li><b>定阴阳遁与局数</b>：冬至后阳遁、夏至后阴遁，五日一元，一元换一局。</li>
 *   <li><b>布地盘三奇六仪</b>：戊己庚辛壬癸丁丙乙，阳遁顺布九宫、阴遁逆布。</li>
 *   <li><b>定值符值使</b>：时干所落之宫的本位星为值符，本位门为值使。</li>
 *   <li><b>布九星八门八神</b>：值符值使加临，其余顺逆布。</li>
 * </ol>
 *
 * <p>「甲遁六仪」这条规则保留了：时干是甲时，用旬首所遁的仪来定位 ——
 * 这是奇门最容易被忽略、也最影响结果的一步。
 */
public final class QiMen {

    private QiMen() {
    }

    // ==================================================================== 九宫

    /**
     * 九宫。{@code number} 是洛书数，{@code direction} 是它在罗盘上的方位。
     */
    public enum Palace {
        KAN(1, "坎", "北"), KUN(2, "坤", "西南"), ZHEN(3, "震", "东"), XUN(4, "巽", "东南"),
        CENTER(5, "中", "中宫"), QIAN(6, "乾", "西北"), DUI(7, "兑", "西"), GEN(8, "艮", "东北"),
        LI(9, "离", "南");

        private final int number;
        private final String zh;
        private final String direction;

        Palace(int number, String zh, String direction) {
            this.number = number;
            this.zh = zh;
            this.direction = direction;
        }

        public int number() {
            return this.number;
        }

        public String zh() {
            return this.zh;
        }

        public String direction() {
            return this.direction;
        }

        public static Palace byNumber(int number) {
            for (Palace palace : values()) {
                if (palace.number == number) {
                    return palace;
                }
            }
            return CENTER;
        }

        /** 3×3 布局顺序：上排 巽离坤、中排 震中兑、下排 艮坎乾（上南下北）。 */
        public static final int[] LAYOUT = {4, 9, 2, 3, 5, 7, 8, 1, 6};
    }

    // ==================================================================== 九星八门八神

    /** 九星。本位宫即它的 number。 */
    public enum Star {
        TIANPENG(1, "天蓬", "水", "主盗贼、水险，亦主智谋深沉。"),
        TIANRUI(2, "天芮", "土", "主疾病、迟缓，宜守不宜攻。"),
        TIANCHONG(3, "天冲", "木", "主勇猛、速动，利于突进。"),
        TIANFU(4, "天辅", "木", "主文教、辅佐，利于求师问道。"),
        TIANQIN(5, "天禽", "土", "主中正、居中调度，最宜坐镇。"),
        TIANXIN(6, "天心", "金", "主医药、决断，利于疗伤与定计。"),
        TIANZHU(7, "天柱", "金", "主口舌、毁折，利于辩论不利合作。"),
        TIANREN(8, "天任", "土", "主稳重、承载，利于守成。"),
        TIANYING(9, "天英", "火", "主光明、虚华，利于扬名不利藏事。");

        private final int home;
        private final String zh;
        private final String element;
        private final String note;

        Star(int home, String zh, String element, String note) {
            this.home = home;
            this.zh = zh;
            this.element = element;
            this.note = note;
        }

        public int home() {
            return this.home;
        }

        public String zh() {
            return this.zh;
        }

        public String note() {
            return this.note;
        }

        public static Star homeOf(int palace) {
            for (Star star : values()) {
                if (star.home == palace) {
                    return star;
                }
            }
            return TIANQIN;
        }

        public static final Star[] VALUES = values();
    }

    /** 八门。中宫无门。 */
    public enum Gate {
        XIU(1, "休门", true, "休养生息，宜静养、宜结亲。"),
        SI(2, "死门", false, "死气沉沉，诸事不宜。"),
        SHANG(3, "伤门", false, "主伤害，宜捕猎、宜争讼，忌远行。"),
        DU(4, "杜门", false, "主闭塞，宜藏匿、宜避祸。"),
        KAI(6, "开门", true, "开张大吉，宜求财、宜见贵。"),
        JING(7, "惊门", false, "主惊扰，宜设伏、宜察言观色。"),
        SHENG(8, "生门", true, "生生不息，宜求财、宜谋事、宜出行。"),
        JING2(9, "景门", false, "主虚像，宜宣传、宜文书，忌签约。");

        private final int home;
        private final String zh;
        private final boolean auspicious;
        private final String note;

        Gate(int home, String zh, boolean auspicious, String note) {
            this.home = home;
            this.zh = zh;
            this.auspicious = auspicious;
            this.note = note;
        }

        public int home() {
            return this.home;
        }

        public String zh() {
            return this.zh;
        }

        public boolean auspicious() {
            return this.auspicious;
        }

        public String note() {
            return this.note;
        }

        public static Gate homeOf(int palace) {
            for (Gate gate : values()) {
                if (gate.home == palace) {
                    return gate;
                }
            }
            return null;
        }

        public static final Gate[] VALUES = values();
    }

    /** 八神。 */
    public enum Spirit {
        ZHIFU("值符", true), TENGSHE("螣蛇", false), TAIYIN("太阴", true), LIUHE("六合", true),
        BAIHU("白虎", false), XUANWU("玄武", false), JIUDI("九地", true), JIUTIAN("九天", true);

        private final String zh;
        private final boolean auspicious;

        Spirit(String zh, boolean auspicious) {
            this.zh = zh;
            this.auspicious = auspicious;
        }

        public String zh() {
            return this.zh;
        }

        public boolean auspicious() {
            return this.auspicious;
        }

        public static final Spirit[] VALUES = values();
    }

    /** 一宫里的东西。 */
    public record Cell(int palace, String gan, Star star, Gate gate, Spirit spirit) {

        public String direction() {
            return Palace.byNumber(this.palace).direction();
        }

        public String compact() {
            return Palace.byNumber(this.palace).zh() + this.palace
                    + " " + (this.gan == null ? "·" : this.gan)
                    + " " + (this.star == null ? "·" : this.star.zh())
                    + " " + (this.gate == null ? "门" : this.gate.zh())
                    + " " + (this.spirit == null ? "·" : this.spirit.zh());
        }
    }

    /**
     * 一个完整的局。
     *
     * @param yang    是否阳遁
     * @param bureau  局数（1~9）
     * @param cells   九宫，按 {@link Palace#LAYOUT} 顺序
     * @param hourGan 用于定位的时干（甲遁之后的仪）
     */
    public record Layout(boolean yang, int bureau, List<Cell> cells, String hourGan,
                         Pair hourPillar, String dayText) {

        public Cell at(int palace) {
            for (Cell cell : this.cells) {
                if (cell.palace() == palace) {
                    return cell;
                }
            }
            return null;
        }

        /** 生门、开门、休门各自落在哪一宫 —— 这三门是玩家最该去的方向。 */
        public List<Cell> goodGates() {
            List<Cell> result = new ArrayList<>();
            for (Cell cell : this.cells) {
                if (cell.gate() != null && cell.gate().auspicious()) {
                    result.add(cell);
                }
            }
            return result;
        }

        /** 最推荐的方向：优先生门，其次开门，再次休门。 */
        public Cell best() {
            for (Gate gate : new Gate[]{Gate.SHENG, Gate.KAI, Gate.XIU}) {
                for (Cell cell : this.cells) {
                    if (cell.gate() == gate) {
                        return cell;
                    }
                }
            }
            return this.cells.get(0);
        }

        public String verdict() {
            Cell best = best();
            Gate gate = best.gate();
            if (gate == null) {
                return "此局门星错位，宜静守。";
            }
            return "此局" + (this.yang ? "阳遁" : "阴遁") + bureau(this.bureau)
                    + "，" + best.direction() + "方" + gate.zh() + "最旺 —— " + gate.note();
        }

        private static String bureau(int value) {
            String[] names = {"", "一", "二", "三", "四", "五", "六", "七", "八", "九"};
            return names[Math.max(1, Math.min(9, value))] + "局";
        }
    }

    // ==================================================================== 起局

    /** 奇门用的九干序：戊己庚辛壬癸丁丙乙（甲遁于六仪之下）。 */
    private static final int[] QI_GAN = {
            Gan.WU.ordinal(), Gan.JI.ordinal(), Gan.GENG.ordinal(), Gan.XIN.ordinal(),
            Gan.REN.ordinal(), Gan.GUI.ordinal(), Gan.DING.ordinal(), Gan.BING.ordinal(),
            Gan.YI.ordinal()
    };

    /** 旬首所遁之仪：甲子戊、甲戌己、甲申庚、甲午辛、甲辰壬、甲寅癸。 */
    private static final int[] XUN_DUN = {0, 1, 2, 3, 4, 5};

    /**
     * 起一个局。
     *
     * @param epochDay  已过的游戏日（用来定阴阳遁与局数）
     * @param dayPillar 日干支
     * @param hour      24 小时制的当前小时
     */
    public static Layout cast(long epochDay, Pair dayPillar, int hour) {
        // ---- 阴阳遁：一年 360 日，前 180 日阳遁、后 180 日阴遁 ----
        int yearPos = (int) Math.floorMod(epochDay, 360L);
        boolean yang = yearPos < 180;

        // ---- 局数：五日一元，一元换一局 ----
        int yuan = Math.floorMod(yearPos, 180) / 5;
        int bureau = Math.floorMod(yuan, 9) + 1;

        // ---- 地盘：三奇六仪 ----
        int[] earth = new int[10];      // 宫位 → 奇门干序（0~8）
        java.util.Arrays.fill(earth, -1);
        for (int i = 0; i < 9; i++) {
            int palace = yang
                    ? Math.floorMod(bureau - 1 + i, 9) + 1
                    : Math.floorMod(bureau - 1 - i, 9) + 1;
            earth[palace] = i;
        }

        // ---- 时干：甲则遁于旬首之仪 ----
        Pair hourPillar = FateCalendar.hourPillar(dayPillar, hour);
        int hourGanIndex = hourPillar.gan().ordinal();
        int targetQi;
        if (hourGanIndex == Gan.JIA.ordinal()) {
            targetQi = XUN_DUN[Math.floorMod(hourPillar.cycleIndex(), 60) / 10];
        } else {
            targetQi = qiIndexOf(hourGanIndex);
        }

        int hourPalace = 5;
        for (int palace = 1; palace <= 9; palace++) {
            if (earth[palace] == targetQi) {
                hourPalace = palace;
                break;
            }
        }

        // ---- 九星：值符加时干宫，其余顺逆布 ----
        Star[] stars = new Star[10];
        Star zhifu = Star.homeOf(hourPalace);
        for (int i = 0; i < 9; i++) {
            int palace = step(hourPalace, yang ? i : -i);
            stars[palace] = Star.VALUES[Math.floorMod(zhifu.ordinal() + (yang ? i : -i), 9)];
        }

        // ---- 八门：值使加时宫，跳过中宫 ----
        Gate[] gates = new Gate[10];
        Gate zhishi = Gate.homeOf(hourPalace);
        if (zhishi == null) {
            zhishi = Gate.KAI;
        }
        int[] ring = ringFrom(hourPalace);
        for (int i = 0; i < 8; i++) {
            gates[ring[i]] = Gate.VALUES[Math.floorMod(zhishi.ordinal() + (yang ? i : -i), 8)];
        }

        // ---- 八神：值符起，跳过中宫 ----
        Spirit[] spirits = new Spirit[10];
        for (int i = 0; i < 8; i++) {
            spirits[ring[i]] = Spirit.VALUES[Math.floorMod(i, 8)];
        }
        if (!yang) {
            // 阴遁逆布：把八神在环上反转
            Spirit[] reversed = new Spirit[10];
            for (int i = 0; i < 8; i++) {
                reversed[ring[i]] = Spirit.VALUES[Math.floorMod(-i, 8)];
            }
            spirits = reversed;
        }

        // ---- 组装九宫 ----
        List<Cell> cells = new ArrayList<>();
        for (int palace : Palace.LAYOUT) {
            int qi = earth[palace];
            String gan = qi >= 0 ? Gan.byIndex(QI_GAN[qi]).zh() : null;
            cells.add(new Cell(palace, gan, stars[palace], gates[palace], spirits[palace]));
        }

        return new Layout(yang, bureau, List.copyOf(cells),
                Gan.byIndex(QI_GAN[targetQi]).zh(), hourPillar, dayPillar.toString());
    }

    /** 奇门干序（0~8）→ 天干序号。 */
    private static int qiIndexOf(int ganIndex) {
        for (int i = 0; i < QI_GAN.length; i++) {
            if (QI_GAN[i] == ganIndex) {
                return i;
            }
        }
        return 0;
    }

    /** 在 1~9 的宫位环上走 {@code steps} 步（跳过中宫的那套环由 {@link #ringFrom} 负责）。 */
    private static int step(int palace, int steps) {
        return Math.floorMod(palace - 1 + steps, 9) + 1;
    }

    /** 从某一宫开始的八宫环（去掉中宫 5），阳遁顺序为 1→2→3→4→6→7→8→9。 */
    private static int[] ringFrom(int start) {
        int[] order = {1, 2, 3, 4, 6, 7, 8, 9};
        int begin = 0;
        for (int i = 0; i < order.length; i++) {
            if (order[i] == start) {
                begin = i;
                break;
            }
        }
        int[] result = new int[8];
        for (int i = 0; i < 8; i++) {
            result[i] = order[(begin + i) % 8];
        }
        return result;
    }

    // ==================================================================== 渲染

    public static List<String> render(Layout layout) {
        List<String> lines = new ArrayList<>();
        lines.add("── 奇门局盘 ──");
        lines.add((layout.yang() ? "阳遁" : "阴遁") + " " + cn(layout.bureau()) + "局"
                + " ｜ 时柱 " + layout.hourPillar().toString()
                + " ｜ 时干 " + layout.hourGan());
        lines.add("（上南下北，格内为：星 门 神 · 干）");
        lines.add("");

        for (int row = 0; row < 3; row++) {
            StringBuilder line = new StringBuilder("  ");
            for (int col = 0; col < 3; col++) {
                Cell cell = layout.cells().get(row * 3 + col);
                line.append(String.format("%-26s", cellText(cell)));
            }
            lines.add(line.toString());
        }

        lines.add("");
        lines.add("── 三吉门 ──");
        for (Cell cell : layout.goodGates()) {
            lines.add("  " + cell.direction() + "（" + Palace.byNumber(cell.palace()).zh() + "宫）"
                    + cell.gate().zh() + " —— " + cell.gate().note());
        }
        lines.add("");
        lines.add("── 断 ──");
        lines.add(layout.verdict());
        Cell best = layout.best();
        if (best.star() != null) {
            lines.add("临 " + best.star().zh() + "：" + best.star().note());
        }
        return lines;
    }

    private static String cellText(Cell cell) {
        String palace = Palace.byNumber(cell.palace()).zh();
        String star = cell.star() == null ? "·" : cell.star().zh();
        String gate = cell.gate() == null ? "中" : cell.gate().zh();
        String spirit = cell.spirit() == null ? "·" : cell.spirit().zh();
        String gan = cell.gan() == null ? "·" : cell.gan();
        return palace + " " + gan + "｜" + star + gate + spirit;
    }

    private static String cn(int value) {
        String[] names = {"", "一", "二", "三", "四", "五", "六", "七", "八", "九"};
        return names[Math.max(1, Math.min(9, value))];
    }

    public static String summary(Layout layout) {
        Cell best = layout.best();
        return (layout.yang() ? "阳遁" : "阴遁") + cn(layout.bureau()) + "局，"
                + best.direction() + "方" + (best.gate() == null ? "门" : best.gate().zh()) + "旺";
    }
}
