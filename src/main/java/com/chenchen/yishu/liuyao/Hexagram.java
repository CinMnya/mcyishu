package com.chenchen.yishu.liuyao;

import com.chenchen.yishu.fate.GanZhi.WuXing;
import com.chenchen.yishu.liuyao.Gua.Trigram;

/**
 * 易枢 · 六十四卦 —— 一整套完整的卦表。
 *
 * <p>六爻断卦不是查一句卦辞就完事，它要先把卦「装」起来：
 * <ol>
 *   <li>由上下两个三爻卦确定这是哪一卦（{@link #of}）；</li>
 *   <li>由它落在哪一个<b>宫</b>，确定卦宫五行（{@link Palace}）；</li>
 *   <li>由它在宫里的位次，确定<b>世爻</b>与<b>应爻</b>（{@link #worldYao()} / {@link #responseYao()}）；</li>
 *   <li>再由纳甲给每一爻配干支，用干支五行与宫五行比出六亲。</li>
 * </ol>
 * 走完这四步，六爻卦才有资格被「读」。本文件负责前三步，第四步在
 * {@link Divination} 里完成。
 *
 * <p><b>八宫</b>是汉代京房定下的分类法：每一宫有一个「本宫卦」（八个纯卦之一），
 * 其余七卦由它逐爻变出 —— 一世、二世、三世、四世、五世、游魂、归魂。
 * 因为是「变」出来的，所以同一个宫里八卦的世爻位置有规律，这正是判断
 * 「事情发生在我身上还是对方身上」的依据。
 */
public record Hexagram(Trigram upper, Trigram lower) {

    /** 六十四卦表：索引 = 上卦序 × 8 + 下卦序。 */
    private static final Entry[] TABLE = new Entry[64];

    static {
        // ---- 上卦 乾 ----
        put(Trigram.QIAN, Trigram.QIAN, "乾为天", "元亨利贞。天行健，君子以自强不息。", 2);
        put(Trigram.QIAN, Trigram.DUI, "天泽履", "履虎尾，不咥人，亨。战战兢兢而终得通。", 1);
        put(Trigram.QIAN, Trigram.LI, "天火同人", "同人于野，亨。与人同心，利于远征。", 2);
        put(Trigram.QIAN, Trigram.ZHEN, "天雷无妄", "元亨利贞。不妄为则无灾，妄动则生咎。", 0);
        put(Trigram.QIAN, Trigram.XUN, "天风姤", "女壮，勿用取女。不期而遇，慎防暗损。", -1);
        put(Trigram.QIAN, Trigram.KAN, "天水讼", "有孚窒惕，中吉终凶。争讼之事，宜和中求。", -2);
        put(Trigram.QIAN, Trigram.GEN, "天山遁", "亨，小利贞。退避者昌，恋战者伤。", 0);
        put(Trigram.QIAN, Trigram.KUN, "天地否", "否之匪人，不利君子贞。天地不交，闭塞不通。", -2);

        // ---- 上卦 兑 ----
        put(Trigram.DUI, Trigram.QIAN, "泽天夬", "扬于王庭，孚号有厉。决断之事，果决则通。", 1);
        put(Trigram.DUI, Trigram.DUI, "兑为泽", "亨，利贞。和悦相说，利于前行。", 2);
        put(Trigram.DUI, Trigram.LI, "泽火革", "己日乃孚，元亨利贞，悔亡。变革之时，顺势则吉。", 1);
        put(Trigram.DUI, Trigram.ZHEN, "泽雷随", "元亨利贞，无咎。随顺时势，随人得道。", 2);
        put(Trigram.DUI, Trigram.XUN, "泽风大过", "栋桡，利有攸往。所担过重，须寻支撑。", -1);
        put(Trigram.DUI, Trigram.KAN, "泽水困", "亨，贞，大人吉，无咎。困于险中，守正待时。", -1);
        put(Trigram.DUI, Trigram.GEN, "泽山咸", "亨，利贞，取女吉。感应相通，所求多应。", 2);
        put(Trigram.DUI, Trigram.KUN, "泽地萃", "亨，王假有庙，利见大人。聚集之象，利于会盟。", 1);

        // ---- 上卦 离 ----
        put(Trigram.LI, Trigram.QIAN, "火天大有", "元亨。所有者大，丰收之象。", 2);
        put(Trigram.LI, Trigram.DUI, "火泽睽", "小事吉。同床异梦，小处尚可。", -1);
        put(Trigram.LI, Trigram.LI, "离为火", "利贞，亨，畜牝牛吉。光明附丽，然须守正。", 1);
        put(Trigram.LI, Trigram.ZHEN, "火雷噬嗑", "亨，利用狱。咬合去梗，宜除障碍。", 1);
        put(Trigram.LI, Trigram.XUN, "火风鼎", "元吉，亨。鼎新之象，利于创制。", 2);
        put(Trigram.LI, Trigram.KAN, "火水未济", "亨，小狐汔济，濡其尾。事未竟，尚需努力。", 0);
        put(Trigram.LI, Trigram.GEN, "火山旅", "小亨，旅贞吉。行旅在外，小事可成。", 0);
        put(Trigram.LI, Trigram.KUN, "火地晋", "康侯用锡马蕃庶。晋升之象，宜进取。", 2);

        // ---- 上卦 震 ----
        put(Trigram.ZHEN, Trigram.QIAN, "雷天大壮", "利贞。刚壮之象，然壮而不可妄动。", 1);
        put(Trigram.ZHEN, Trigram.DUI, "雷泽归妹", "征凶，无攸利。名分不正，所行多阻。", -2);
        put(Trigram.ZHEN, Trigram.LI, "雷火丰", "亨，王假之，勿忧，宜日中。丰盛之极，盛极须防衰。", 1);
        put(Trigram.ZHEN, Trigram.ZHEN, "震为雷", "亨。震来虩虩，笑言哑哑。惊而后定，先惧后安。", 0);
        put(Trigram.ZHEN, Trigram.XUN, "雷风恒", "亨，无咎，利贞，利有攸往。恒久之道，守常则吉。", 1);
        put(Trigram.ZHEN, Trigram.KAN, "雷水解", "利西南。险难解散，宜速往。", 2);
        put(Trigram.ZHEN, Trigram.GEN, "雷山小过", "亨，利贞，可小事，不可大事。小事可为，大事勿贪。", 0);
        put(Trigram.ZHEN, Trigram.KUN, "雷地豫", "利建侯行师。豫乐之象，先备后动。", 1);

        // ---- 上卦 巽 ----
        put(Trigram.XUN, Trigram.QIAN, "风天小畜", "亨。密云不雨，尚往也。小有积蓄，未及大成。", 0);
        put(Trigram.XUN, Trigram.DUI, "风泽中孚", "豚鱼吉，利涉大川。中心诚信，可越险阻。", 2);
        put(Trigram.XUN, Trigram.LI, "风火家人", "利女贞。家道正，则事无不成。", 1);
        put(Trigram.XUN, Trigram.ZHEN, "风雷益", "利有攸往，利涉大川。损上益下，进则得益。", 2);
        put(Trigram.XUN, Trigram.XUN, "巽为风", "小亨，利有攸往。顺入之象，随风而行。", 1);
        put(Trigram.XUN, Trigram.KAN, "风水涣", "亨，王假有庙，利涉大川。涣散之象，宜聚人心。", 0);
        put(Trigram.XUN, Trigram.GEN, "风山渐", "女归吉，利贞。循序渐进，缓则得之。", 1);
        put(Trigram.XUN, Trigram.KUN, "风地观", "盥而不荐，有孚颙若。观望之象，静以待变。", 0);

        // ---- 上卦 坎 ----
        put(Trigram.KAN, Trigram.QIAN, "水天需", "有孚，光亨，贞吉，利涉大川。等待之象，需时而动。", 1);
        put(Trigram.KAN, Trigram.DUI, "水泽节", "亨。苦节不可贞。节制有度则通。", 1);
        put(Trigram.KAN, Trigram.LI, "水火既济", "亨小，利贞，初吉终乱。已成之象，守成不易。", 0);
        put(Trigram.KAN, Trigram.ZHEN, "水雷屯", "元亨利贞，勿用有攸往，利建侯。初生艰难，宜固本。", -1);
        put(Trigram.KAN, Trigram.XUN, "水风井", "改邑不改井，无丧无得。井养不穷，守常得利。", 1);
        put(Trigram.KAN, Trigram.KAN, "坎为水", "习坎，有孚，维心亨。重险之象，唯诚可渡。", -2);
        put(Trigram.KAN, Trigram.GEN, "水山蹇", "利西南，不利东北，利见大人。跛行难进，宜求助。", -2);
        put(Trigram.KAN, Trigram.KUN, "水地比", "吉。比者辅也，亲附则吉。", 2);

        // ---- 上卦 艮 ----
        put(Trigram.GEN, Trigram.QIAN, "山天大畜", "利贞，不家食吉，利涉大川。厚积待发。", 2);
        put(Trigram.GEN, Trigram.DUI, "山泽损", "有孚，元吉，无咎，可贞。损下益上，减损得吉。", 1);
        put(Trigram.GEN, Trigram.LI, "山火贲", "亨，小利有攸往。文饰之象，外表可观。", 1);
        put(Trigram.GEN, Trigram.ZHEN, "山雷颐", "贞吉，观颐，自求口实。颐养之象，宜自谋食。", 1);
        put(Trigram.GEN, Trigram.XUN, "山风蛊", "元亨，利涉大川。器久生虫，宜整饬旧弊。", 0);
        put(Trigram.GEN, Trigram.KAN, "山水蒙", "亨。匪我求童蒙，童蒙求我。蒙昧之象，宜求教。", 0);
        put(Trigram.GEN, Trigram.GEN, "艮为山", "艮其背，不获其身。止而不动，安守其位。", 0);
        put(Trigram.GEN, Trigram.KUN, "山地剥", "不利有攸往。剥落之象，宜静守待复。", -2);

        // ---- 上卦 坤 ----
        put(Trigram.KUN, Trigram.QIAN, "地天泰", "小往大来，吉亨。天地交通，诸事咸宜。", 2);
        put(Trigram.KUN, Trigram.DUI, "地泽临", "元亨利贞，至于八月有凶。居高临下，宜抚众。", 1);
        put(Trigram.KUN, Trigram.LI, "地火明夷", "利艰贞。光明受损，宜韬光养晦。", -2);
        put(Trigram.KUN, Trigram.ZHEN, "地雷复", "亨，出入无疾，朋来无咎。一阳来复，否极泰来。", 2);
        put(Trigram.KUN, Trigram.XUN, "地风升", "元亨，用见大人，勿恤，南征吉。上升之象，宜进取。", 2);
        put(Trigram.KUN, Trigram.KAN, "地水师", "贞，丈人吉，无咎。兴师之象，需有统御。", 0);
        put(Trigram.KUN, Trigram.GEN, "地山谦", "亨，君子有终。谦谦君子，无往不利。", 2);
        put(Trigram.KUN, Trigram.KUN, "坤为地", "元亨，利牝马之贞。厚德载物，宜静宜顺。", 1);
    }

    // ==================================================================== 宫

    /**
     * 八宫。
     *
     * <p>每一宫的五行是「本宫卦」的五行，六亲全靠它比出来。
     * 宫里的八卦按「本宫、一世、二世、三世、四世、五世、游魂、归魂」排序。
     */
    public enum Palace {
        QIAN("乾宫", WuXing.JIN, "刚健肃杀，主决断与权威"),
        KAN("坎宫", WuXing.SHUI, "险陷流动，主隐伏与迁徙"),
        GEN("艮宫", WuXing.TU, "止静厚重，主停滞与守成"),
        ZHEN("震宫", WuXing.MU, "震动奋起，主惊变与开创"),
        XUN("巽宫", WuXing.MU, "顺入柔韧，主渗透与渐进"),
        LI("离宫", WuXing.HUO, "明丽外显，主文采与虚火"),
        KUN("坤宫", WuXing.TU, "柔顺包容，主承载与蓄养"),
        DUI("兑宫", WuXing.JIN, "和悦口舌，主交易与言论");

        private final String zh;
        private final WuXing wuXing;
        private final String note;

        Palace(String zh, WuXing wuXing, String note) {
            this.zh = zh;
            this.wuXing = wuXing;
            this.note = note;
        }

        public String zh() {
            return this.zh;
        }

        public WuXing wuXing() {
            return this.wuXing;
        }

        public String note() {
            return this.note;
        }

        public static final Palace[] VALUES = values();
    }

    /**
     * 每一宫的八卦序列（用「上卦/下卦」写出来，一眼可核对）。
     *
     * <p>顺序固定为：本宫、一世、二世、三世、四世、五世、游魂、归魂。
     */
    private static final Trigram[][] PALACE_ORDER = {
            // 乾宫
            {Trigram.QIAN, Trigram.QIAN}, {Trigram.QIAN, Trigram.XUN}, {Trigram.QIAN, Trigram.GEN}, {Trigram.QIAN, Trigram.KUN},
            {Trigram.XUN, Trigram.KUN}, {Trigram.GEN, Trigram.KUN}, {Trigram.LI, Trigram.KUN}, {Trigram.LI, Trigram.QIAN},
            // 坎宫
            {Trigram.KAN, Trigram.KAN}, {Trigram.KAN, Trigram.DUI}, {Trigram.KAN, Trigram.ZHEN}, {Trigram.KAN, Trigram.LI},
            {Trigram.DUI, Trigram.LI}, {Trigram.ZHEN, Trigram.LI}, {Trigram.KUN, Trigram.LI}, {Trigram.KUN, Trigram.KAN},
            // 艮宫
            {Trigram.GEN, Trigram.GEN}, {Trigram.GEN, Trigram.LI}, {Trigram.GEN, Trigram.QIAN}, {Trigram.GEN, Trigram.DUI},
            {Trigram.LI, Trigram.DUI}, {Trigram.QIAN, Trigram.DUI}, {Trigram.XUN, Trigram.DUI}, {Trigram.XUN, Trigram.GEN},
            // 震宫
            {Trigram.ZHEN, Trigram.ZHEN}, {Trigram.ZHEN, Trigram.KUN}, {Trigram.ZHEN, Trigram.KAN}, {Trigram.ZHEN, Trigram.XUN},
            {Trigram.KUN, Trigram.XUN}, {Trigram.KAN, Trigram.XUN}, {Trigram.DUI, Trigram.XUN}, {Trigram.DUI, Trigram.ZHEN},
            // 巽宫
            {Trigram.XUN, Trigram.XUN}, {Trigram.XUN, Trigram.QIAN}, {Trigram.XUN, Trigram.LI}, {Trigram.XUN, Trigram.ZHEN},
            {Trigram.QIAN, Trigram.ZHEN}, {Trigram.LI, Trigram.ZHEN}, {Trigram.GEN, Trigram.ZHEN}, {Trigram.GEN, Trigram.XUN},
            // 离宫
            {Trigram.LI, Trigram.LI}, {Trigram.LI, Trigram.GEN}, {Trigram.LI, Trigram.XUN}, {Trigram.LI, Trigram.KAN},
            {Trigram.GEN, Trigram.KAN}, {Trigram.XUN, Trigram.KAN}, {Trigram.QIAN, Trigram.KAN}, {Trigram.QIAN, Trigram.LI},
            // 坤宫
            {Trigram.KUN, Trigram.KUN}, {Trigram.KUN, Trigram.ZHEN}, {Trigram.KUN, Trigram.DUI}, {Trigram.KUN, Trigram.QIAN},
            {Trigram.ZHEN, Trigram.QIAN}, {Trigram.DUI, Trigram.QIAN}, {Trigram.KAN, Trigram.QIAN}, {Trigram.KAN, Trigram.KUN},
            // 兑宫
            {Trigram.DUI, Trigram.DUI}, {Trigram.DUI, Trigram.KAN}, {Trigram.DUI, Trigram.KUN}, {Trigram.DUI, Trigram.GEN},
            {Trigram.KAN, Trigram.GEN}, {Trigram.KUN, Trigram.GEN}, {Trigram.ZHEN, Trigram.GEN}, {Trigram.ZHEN, Trigram.DUI},
    };

    /** 宫位 → (宫, 宫内位次) 反查表，索引 = 上卦序 × 8 + 下卦序。 */
    private static final int[] PALACE_OF = new int[64];
    private static final int[] POSITION_IN_PALACE = new int[64];

    static {
        java.util.Arrays.fill(PALACE_OF, -1);
        for (int palace = 0; palace < PALACE_ORDER.length; palace++) {
            Trigram[] members = PALACE_ORDER[palace];
            for (int pos = 0; pos < members.length; pos += 2) {
                int index = members[pos].ordinal() * 8 + members[pos + 1].ordinal();
                PALACE_OF[index] = palace;
                POSITION_IN_PALACE[index] = pos / 2;
            }
        }
    }

    /**
     * 世爻位置（1..6），宫内位次直接决定。
     *
     * <p>本宫卦世在六爻、一世卦世在初爻……游魂卦世在四爻、归魂卦世在三爻。
     */
    private static final int[] WORLD_YAO = {6, 1, 2, 3, 4, 5, 4, 3};
    /** 应爻位置，与世爻隔两位（世 1 应 4、世 2 应 5……）。 */
    private static final int[] RESPONSE_YAO = {3, 4, 5, 6, 1, 2, 1, 6};

    // ==================================================================== 构造

    /** 由上下卦取六爻卦。 */
    public static Hexagram of(Trigram upper, Trigram lower) {
        return new Hexagram(upper, lower);
    }

    /** 由六个爻的阴阳取六爻卦（索引 0 = 初爻）。 */
    public static Hexagram ofYao(boolean[] yang) {
        Trigram lower = Trigram.ofBits((yang[0] ? 1 : 0) | (yang[1] ? 2 : 0) | (yang[2] ? 4 : 0));
        Trigram upper = Trigram.ofBits((yang[3] ? 1 : 0) | (yang[4] ? 2 : 0) | (yang[5] ? 4 : 0));
        return new Hexagram(upper, lower);
    }

    private static void put(Trigram upper, Trigram lower, String name, String judgment, int auspice) {
        TABLE[upper.ordinal() * 8 + lower.ordinal()] = new Entry(name, judgment, auspice);
    }

    /** 一条卦目。 */
    private record Entry(String name, String judgment, int auspice) {
    }

    private int index() {
        return this.upper.ordinal() * 8 + this.lower.ordinal();
    }

    private Entry entry() {
        return TABLE[index()];
    }

    // ==================================================================== 查询

    /** 卦名，例如「水雷屯」。 */
    public String name() {
        return entry().name();
    }

    /** 卦辞。 */
    public String judgment() {
        return entry().judgment();
    }

    /** 吉凶倾向：-2 大凶 / -1 凶 / 0 平 / +1 吉 / +2 大吉。 */
    public int auspice() {
        return entry().auspice();
    }

    /** 吉凶的中文描述。 */
    public String auspiceName() {
        return switch (this.auspice()) {
            case 2 -> "大吉";
            case 1 -> "吉";
            case -1 -> "凶";
            case -2 -> "大凶";
            default -> "平";
        };
    }

    /** 卦符，用两个三爻符号拼出来，例如 ☵☳。 */
    public String symbol() {
        return this.upper.symbol() + this.lower.symbol();
    }

    /** 所属宫。 */
    public Palace palace() {
        return Palace.VALUES[PALACE_OF[index()]];
    }

    /** 宫五行 —— 六亲的基准。 */
    public WuXing palaceElement() {
        return palace().wuXing();
    }

    /** 宫内位次 0..7（本宫、一世……归魂）。 */
    public int positionInPalace() {
        return POSITION_IN_PALACE[index()];
    }

    /** 宫内位次名。 */
    public String positionName() {
        return switch (positionInPalace()) {
            case 0 -> "本宫卦";
            case 1 -> "一世卦";
            case 2 -> "二世卦";
            case 3 -> "三世卦";
            case 4 -> "四世卦";
            case 5 -> "五世卦";
            case 6 -> "游魂卦";
            default -> "归魂卦";
        };
    }

    /** 世爻位置 1..6。 */
    public int worldYao() {
        return WORLD_YAO[positionInPalace()];
    }

    /** 应爻位置 1..6。 */
    public int responseYao() {
        return RESPONSE_YAO[positionInPalace()];
    }

    /**
     * 六个爻的阴阳。
     *
     * @return 长度 6 的数组，索引 0 = 初爻，true 为阳
     */
    public boolean[] yao() {
        boolean[] result = new boolean[6];
        for (int i = 0; i < 3; i++) {
            result[i] = this.lower.yangAt(i + 1);
            result[i + 3] = this.upper.yangAt(i + 1);
        }
        return result;
    }

    /**
     * 给六个爻配上纳甲干支。
     *
     * @return 长度 6，索引 0 = 初爻（与 {@link #yao()} 对齐）
     */
    public Gua.NaJia[] najia() {
        Gua.NaJia[] lower = Gua.najia(this.lower, false);
        Gua.NaJia[] upper = Gua.najia(this.upper, true);
        return new Gua.NaJia[]{
                lower[0], lower[1], lower[2],
                upper[0], upper[1], upper[2]
        };
    }

    /** 某一爻动之后的变卦（该爻阴阳翻转）。 */
    public Hexagram changeAt(int yao) {
        boolean[] yang = yao();
        int index = Math.floorMod(yao - 1, 6);
        yang[index] = !yang[index];
        return ofYao(yang);
    }

    /** 一卦的中文全称，例如「水雷屯（坎宫 二世卦）」。 */
    public String fullName() {
        return name() + "（" + palace().zh() + " " + positionName() + "）";
    }

    /**
     * 六爻的显示行：从<b>上爻</b>到<b>初爻</b>，这是传统排法。
     *
     * @return 长度 6，索引 0 = 上爻
     */
    public static int[] displayOrder() {
        return new int[]{6, 5, 4, 3, 2, 1};
    }
}
