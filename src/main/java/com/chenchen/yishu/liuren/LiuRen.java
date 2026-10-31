package com.chenchen.yishu.liuren;

import com.chenchen.yishu.fate.GanZhi.Pair;
import com.chenchen.yishu.fate.GanZhi.WuXing;
import com.chenchen.yishu.fate.GanZhi.YinYang;
import com.chenchen.yishu.fate.GanZhi.Zhi;

import java.util.ArrayList;
import java.util.List;

/**
 * 大六壬 —— 四课三传与十二天将。
 *
 * <p>六爻问的是「这件事会怎样」，六壬问的是「这件事<b>怎么走</b>」。
 * 它的骨架是「月将加时」：把月将摁在时辰那一格上，整个天盘就转起来了 ——
 * 转出来的四课是「现状」，三传是「发展」，十二天将是「参与其中的力量」。
 *
 * <p>本实现覆盖：
 * <ol>
 *   <li><b>天地盘</b>：月将加时，天盘随地盘顺布。</li>
 *   <li><b>四课</b>：日干寄宫起一课二课，日支起三课四课。</li>
 *   <li><b>三传</b>：贼克 → 比用 → 遥克 → 昴星，四级发用；伏吟返吟单独处理。</li>
 *   <li><b>十二天将</b>：昼贵夜贵定贵人，贵人临阳宫顺布、临阴宫逆布。</li>
 * </ol>
 *
 * <p>课体判定做了简化：按三传所乘天将的吉凶属性打分，
 * 再叠加「三传递生」与「三传递克」两条常格。真正的六壬判词有几百种格局，
 * 游戏里不需要那么多 —— 但「吉将连传则事顺」这条直觉是对的，保留它。
 */
public final class LiuRen {

    private LiuRen() {
    }

    // ==================================================================== 十二天将

    /** 十二天将。{@code auspicious} 只影响课体评分，不影响排盘。 */
    public enum General {
        GUIREN("贵人", true, "诸神之首，主扶持、引荐。"),
        TENGSHE("螣蛇", false, "虚惊怪梦，主缠绕、反复。"),
        ZHUQUE("朱雀", false, "口舌文书，主争辩、传递。"),
        LIUHE("六合", true, "和合媒妁，主成交、结盟。"),
        GOUCHEN("勾陈", false, "田土争讼，主拖延、纠缠。"),
        QINGLONG("青龙", true, "财喜尊贵，主进益、升迁。"),
        TIANKONG("天空", false, "虚诈不实，主落空、欺瞒。"),
        BAIHU("白虎", false, "道路丧伤，主凶险、急迫。"),
        TAICHANG("太常", true, "衣食宴乐，主丰足、日常。"),
        XUANWU("玄武", false, "盗贼阴私，主暗昧、遗失。"),
        TAIYIN("太阴", true, "阴私庇护，主暗中相助。"),
        TIANHOU("天后", true, "妇女恩泽，主柔顺、护持。");

        private final String zh;
        private final boolean auspicious;
        private final String note;

        General(String zh, boolean auspicious, String note) {
            this.zh = zh;
            this.auspicious = auspicious;
            this.note = note;
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

        public static final General[] VALUES = values();
    }

    // ==================================================================== 四课

    /**
     * 一课。
     *
     * @param lower 下神（地盘）
     * @param upper 上神（天盘）
     */
    public record Lesson(int lower, int upper) {

        public String text() {
            return Zhi.byIndex(this.lower).zh() + " → " + Zhi.byIndex(this.upper).zh();
        }

        /** 下贼上：下神克上神。 */
        public boolean lowerAttacks() {
            return overcomes(Zhi.byIndex(this.lower).wuXing(), Zhi.byIndex(this.upper).wuXing());
        }

        /** 上克下：上神克下神。 */
        public boolean upperAttacks() {
            return overcomes(Zhi.byIndex(this.upper).wuXing(), Zhi.byIndex(this.lower).wuXing());
        }
    }

    /** 一传。 */
    public record Transmission(String name, int branch, General general) {

        public String text() {
            return this.name + " " + Zhi.byIndex(this.branch).zh() + "（" + this.general.zh() + "）";
        }
    }

    /**
     * 一课完整解读。
     *
     * @param dayPillar    日干支
     * @param hourBranch   时支
     * @param monthBranch  月建
     * @param general      月将（登明亥将之类，这里直接存地支）
     * @param lessons      四课
     * @param transmissions 三传（初、中、末）
     * @param method       发用所用法门
     * @param generals     十二天将的落宫（下标为地盘宫，值为天将）
     */
    public record Reading(Pair dayPillar, int hourBranch, int monthBranch, int general,
                          List<Lesson> lessons, List<Transmission> transmissions,
                          String method, General[] generals, int score) {

        /** 初传。 */
        public Transmission first() {
            return this.transmissions.get(0);
        }

        public String verdict() {
            if (this.score >= 6) {
                return "课体通顺 —— 事情有人推、有路走，宜进不宜守。";
            }
            if (this.score >= 2) {
                return "课体平中带吉 —— 能成，但要顺着势头走，硬顶会耗。";
            }
            if (this.score >= -1) {
                return "课体平平 —— 吉凶各半，成事在人。";
            }
            if (this.score >= -5) {
                return "课体滞涩 —— 有阻，宜缓、宜守、宜换个方向。";
            }
            return "课体凶险 —— 三传皆凶，此时莫强求，先保住已有之物。";
        }
    }

    // ==================================================================== 起课

    /**
     * 起一课。
     *
     * @param dayPillar   日干支
     * @param hourBranch  时支（0~11）
     * @param monthBranch 月建地支（0~11，正月建寅）
     */
    public static Reading cast(Pair dayPillar, int hourBranch, int monthBranch) {
        // ---- 月将：正月（寅）用亥将，逐月退一位 ----
        int general = Math.floorMod(13 - monthBranch, 12);

        int[] heaven = heavenPlate(general, hourBranch);
        int ganPalace = ganLodge(dayPillar.gan().ordinal());
        int zhiPalace = dayPillar.zhi().ordinal();

        // ---- 四课 ----
        int l1 = heaven[ganPalace];
        int l2 = heaven[l1];
        int l3 = heaven[zhiPalace];
        int l4 = heaven[l3];
        List<Lesson> lessons = List.of(
                new Lesson(ganPalace, l1),
                new Lesson(l1, l2),
                new Lesson(zhiPalace, l3),
                new Lesson(l3, l4));

        // ---- 发用 ----
        int offset = Math.floorMod(general - hourBranch, 12);
        int first;
        String method;
        if (offset == 0) {
            // 伏吟：天地盘重合，取日支上神（即日支本身）
            first = zhiPalace;
            method = "伏吟";
        } else if (offset == 6) {
            // 返吟：天地盘全冲，取日支之冲（井栏射）
            first = Math.floorMod(zhiPalace + 6, 12);
            method = "返吟";
        } else {
            int[] resolved = resolveFirst(lessons, dayPillar);
            first = resolved[0];
            method = METHOD_NAMES[resolved[1]];
        }

        // ---- 三传：初传之上神为中传，中传之上神为末传 ----
        int second = heaven[first];
        int third = heaven[second];

        // ---- 十二天将 ----
        General[] generals = arrangeGenerals(dayPillar, hourBranch);

        List<Transmission> transmissions = List.of(
                new Transmission("初传", first, generals[first]),
                new Transmission("中传", second, generals[second]),
                new Transmission("末传", third, generals[third]));

        int score = score(transmissions, lessons);

        return new Reading(dayPillar, hourBranch, monthBranch, general, lessons,
                transmissions, method, generals, score);
    }

    private static final String[] METHOD_NAMES = {"贼克", "比用", "遥克", "昴星"};

    /**
     * 发用四级：贼克 → 比用 → 遥克 → 昴星。
     *
     * @return {@code [上神宫位, 法门序号]}
     */
    private static int[] resolveFirst(List<Lesson> lessons, Pair dayPillar) {
        // 一级：下贼上（下神克上神）；取「与日干比」者优先
        List<Integer> steals = new ArrayList<>();
        List<Integer> controls = new ArrayList<>();
        for (Lesson lesson : lessons) {
            if (lesson.lowerAttacks()) {
                steals.add(lesson.upper());
            } else if (lesson.upperAttacks()) {
                controls.add(lesson.upper());
            }
        }
        int[] picked = pickByYinYang(steals.isEmpty() ? controls : steals, dayPillar);
        if (picked != null) {
            return picked;
        }

        // 二级：无克则遥克（上神克日干，或日干克上神）
        WuXing dayElement = dayPillar.gan().wuXing();
        for (Lesson lesson : lessons) {
            WuXing upper = Zhi.byIndex(lesson.upper()).wuXing();
            if (overcomes(upper, dayElement) || overcomes(dayElement, upper)) {
                return new int[]{lesson.upper(), 2};
            }
        }

        // 三级：昴星 —— 阳日取天盘酉下之宫，阴日取地盘酉上之神
        int you = Zhi.YOU.ordinal();
        boolean yang = dayPillar.gan().yinYang() == YinYang.YANG;
        return new int[]{yang ? you : Math.floorMod(you + 6, 12), 3};
    }

    /** 在候选里挑与日干同阴阳的；只有一个候选就直接取。 */
    private static int[] pickByYinYang(List<Integer> candidates, Pair dayPillar) {
        if (candidates.isEmpty()) {
            return null;
        }
        if (candidates.size() == 1) {
            return new int[]{candidates.get(0), 0};
        }
        boolean yang = dayPillar.gan().yinYang() == YinYang.YANG;
        for (int candidate : candidates) {
            boolean candidateYang = Zhi.byIndex(candidate).yinYang() == YinYang.YANG;
            if (candidateYang == yang) {
                return new int[]{candidate, 1};
            }
        }
        return new int[]{candidates.get(0), 0};
    }

    // ==================================================================== 天地盘

    /** 天盘：{@code 天盘[地盘宫] = 天盘支}。月将加时，其余顺布。 */
    public static int[] heavenPlate(int general, int hourBranch) {
        int[] plate = new int[12];
        for (int ground = 0; ground < 12; ground++) {
            plate[ground] = Math.floorMod(general + ground - hourBranch, 12);
        }
        return plate;
    }

    /** 日干寄宫 —— 甲寄寅、乙寄辰、丙戊寄巳、丁己寄未、庚寄申、辛寄戌、壬寄亥、癸寄丑。 */
    public static int ganLodge(int ganIndex) {
        return switch (ganIndex) {
            case 0 -> 2;    // 甲 → 寅
            case 1 -> 4;    // 乙 → 辰
            case 2, 4 -> 5; // 丙、戊 → 巳
            case 3, 5 -> 7; // 丁、己 → 未
            case 6 -> 8;    // 庚 → 申
            case 7 -> 10;   // 辛 → 戌
            case 8 -> 11;   // 壬 → 亥
            default -> 1;   // 癸 → 丑
        };
    }

    // ==================================================================== 天将

    /**
     * 布十二天将。
     *
     * <p>贵人由日干定，昼用阳贵、夜用阴贵；贵人临地盘亥至辰（阳）顺布，
     * 临巳至戌（阴）逆布。
     */
    public static General[] arrangeGenerals(Pair dayPillar, int hourBranch) {
        int gan = dayPillar.gan().ordinal();
        // 甲戊庚牛羊、乙己鼠猴乡、丙丁猪鸡位、壬癸兔蛇藏、六辛逢马虎
        int[] pair = switch (gan) {
            case 0, 4, 6 -> new int[]{1, 7};    // 甲戊庚：丑未
            case 1, 5 -> new int[]{0, 8};       // 乙己：子申
            case 2, 3 -> new int[]{11, 9};      // 丙丁：亥酉
            case 8, 9 -> new int[]{3, 5};       // 壬癸：卯巳
            default -> new int[]{6, 2};         // 辛：午寅
        };
        int hour = Zhi.byIndex(hourBranch).startHour();
        boolean day = hour >= 5 && hour < 19;
        int noble = day ? pair[0] : pair[1];

        boolean forward = noble >= Zhi.HAI.ordinal() || noble <= Zhi.CHEN.ordinal();
        General[] result = new General[12];
        for (int i = 0; i < 12; i++) {
            int palace = Math.floorMod(noble + (forward ? i : -i), 12);
            result[palace] = General.VALUES[i];
        }
        return result;
    }

    // ==================================================================== 课体评分

    private static int score(List<Transmission> transmissions, List<Lesson> lessons) {
        int total = 0;
        for (Transmission transmission : transmissions) {
            total += transmission.general().auspicious() ? 3 : -3;
        }
        // 三传递生：相邻两传的上神五行相生
        for (int i = 0; i < transmissions.size() - 1; i++) {
            WuXing a = Zhi.byIndex(transmissions.get(i).branch()).wuXing();
            WuXing b = Zhi.byIndex(transmissions.get(i + 1).branch()).wuXing();
            if (generates(a, b)) {
                total += 2;
            }
            if (overcomes(a, b)) {
                total -= 2;
            }
        }
        // 四课有克无制，略降
        long stealing = lessons.stream().filter(Lesson::lowerAttacks).count();
        total -= (int) Math.min(3, stealing);
        return total;
    }

    // ==================================================================== 五行

    private static boolean overcomes(WuXing self, WuXing other) {
        return switch (self) {
            case MU -> other == WuXing.TU;
            case TU -> other == WuXing.SHUI;
            case SHUI -> other == WuXing.HUO;
            case HUO -> other == WuXing.JIN;
            case JIN -> other == WuXing.MU;
        };
    }

    private static boolean generates(WuXing self, WuXing other) {
        return switch (self) {
            case MU -> other == WuXing.HUO;
            case HUO -> other == WuXing.TU;
            case TU -> other == WuXing.JIN;
            case JIN -> other == WuXing.SHUI;
            case SHUI -> other == WuXing.MU;
        };
    }

    // ==================================================================== 渲染

    public static List<String> render(Reading reading) {
        List<String> lines = new ArrayList<>();
        lines.add("── 六壬课盘 ──");
        lines.add("日辰 " + reading.dayPillar().toString()
                + " ｜ 月建 " + Zhi.byIndex(reading.monthBranch()).zh()
                + " ｜ 月将 " + Zhi.byIndex(reading.general()).zh()
                + " ｜ 时辰 " + Zhi.byIndex(reading.hourBranch()).zh());
        lines.add("发用：" + reading.method() + "法");
        lines.add("");

        lines.add("── 四课 ──");
        String[] names = {"一课", "二课", "三课", "四课"};
        for (int i = 0; i < reading.lessons().size(); i++) {
            Lesson lesson = reading.lessons().get(i);
            String mark = lesson.lowerAttacks() ? "  【贼】" : lesson.upperAttacks() ? "  【克】" : "";
            lines.add("  " + names[i] + "  " + lesson.text() + mark);
        }
        lines.add("");

        lines.add("── 三传 ──");
        for (Transmission transmission : reading.transmissions()) {
            lines.add("  " + transmission.text());
        }
        lines.add("");

        lines.add("── 十二天将 ──");
        StringBuilder row = new StringBuilder("  ");
        for (int i = 0; i < 12; i++) {
            row.append(Zhi.byIndex(i).zh());
            General general = reading.generals()[i];
            row.append(general == null ? "—" : general.zh());
            row.append(i == 5 ? "\n  " : (i == 11 ? "" : " "));
        }
        lines.add(row.toString());
        lines.add("");

        lines.add("── 断 ──");
        lines.add("初传乘 " + reading.first().general().zh() + " —— "
                + reading.first().general().note());
        lines.add(reading.verdict());
        return lines;
    }

    public static String summary(Reading reading) {
        return reading.dayPillar().toString() + "日 "
                + Zhi.byIndex(reading.hourBranch()).zh() + "时，"
                + reading.method() + "发用，初传"
                + Zhi.byIndex(reading.first().branch()).zh()
                + "乘" + reading.first().general().zh();
    }
}
