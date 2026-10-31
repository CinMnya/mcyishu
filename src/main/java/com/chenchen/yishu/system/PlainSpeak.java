package com.chenchen.yishu.system;

import com.chenchen.yishu.core.StableKey;
import com.chenchen.yishu.dimension.DimensionProfile;
import com.chenchen.yishu.fate.Bazi;
import com.chenchen.yishu.fate.GanZhi.WuXing;
import com.chenchen.yishu.fengshui.FengShuiField;
import com.chenchen.yishu.liuyao.Divination;
import com.chenchen.yishu.liuyao.Gua.LiuQin;
import com.chenchen.yishu.liuyao.Gua.LiuShen;

import java.util.ArrayList;
import java.util.List;

/**
 * 易枢 · 白话翻译系统 —— 让不懂命理的人也能玩。
 *
 * <p><b>核心原则：玩家不需要懂命理，只需要会玩游戏。</b>
 * 系统在后台算，前台只给玩家能懂的反馈。懂的人可以深挖，不懂的人照样通关。
 *
 * <p>每个系统都分三层，玩家按需接触：
 * <ol>
 *   <li><b>第一层 · 直感</b>：颜色、图标、音效、粒子 —— 不识字也能懂；</li>
 *   <li><b>第二层 · 白话</b>：一句人话提示 —— "往西走，地下有矿，小心岩浆"；</li>
 *   <li><b>第三层 · 术语</b>：八字、卦象、六亲六神 —— 只在专业模式显示。</li>
 * </ol>
 * 默认只显示前两层。
 *
 * <p>这个类是<b>左列到右列的翻译器</b>。所有术语在这里换成白话，
 * 上层的展示代码只管问"人话怎么说"，不自己拼术语。
 */
public final class PlainSpeak {

    private PlainSpeak() {
    }

    // ==================================================================== 一句话原则

    /** 界面上方那句总纲 —— 让玩家第一眼就明白这个模组的态度。 */
    public static final String MOTTO = "系统帮你算，你只管玩。想看细节，开专业模式。";

    // ==================================================================== 五行

    /** 五行的白话说法。 */
    public static String wuxingPlain(WuXing element) {
        return switch (element) {
            case MU -> "木";
            case HUO -> "火";
            case TU -> "土";
            case JIN -> "金";
            case SHUI -> "水";
        };
    }

    /** 五行的性格比喻 —— 帮玩家把"属性"和"自己"联系起来。 */
    public static String wuxingCharacter(WuXing element) {
        return switch (element) {
            case MU -> "像树木一样爱生长，适合往有植物的地方去";
            case HUO -> "像火焰一样有冲劲，住在暖和亮堂的地方更舒服";
            case TU -> "像大地一样稳重，适合定居、种田、慢慢积累";
            case JIN -> "像金属一样锋利，挖矿和打造对你格外顺手";
            case SHUI -> "像流水一样灵活，靠近水边你会更有精神";
        };
    }

    /** 日主的一句话："你是土属性"。 */
    public static String dayMasterPlain(Bazi bazi) {
        return "你是" + wuxingPlain(bazi.dayMasterElement()) + "属性的人";
    }

    /** 喜忌的一句话："火和土对你有利，木和水对你不利"。 */
    public static String yongShenPlain(Bazi bazi) {
        Bazi.YongShen yong = bazi.yongShen();
        StringBuilder builder = new StringBuilder();
        List<String> good = new ArrayList<>();
        for (WuXing element : yong.favorable()) {
            good.add(wuxingPlain(element));
        }
        List<String> bad = new ArrayList<>();
        for (WuXing element : yong.unfavorable()) {
            bad.add(wuxingPlain(element));
        }
        if (!good.isEmpty()) {
            builder.append(String.join("和", good)).append("对你有利");
        }
        if (!bad.isEmpty()) {
            if (builder.length() > 0) {
                builder.append("，");
            }
            builder.append(String.join("和", bad)).append("对你不利");
        }
        if (builder.length() == 0) {
            builder.append("各方面都还算平顺");
        }
        return builder.toString();
    }

    /** 日主的一句话 + 一句怎么用的建议。 */
    public static String fatePlain(Bazi bazi) {
        WuXing element = bazi.dayMasterElement();
        return dayMasterPlain(bazi) + "。" + wuxingCharacter(element)
                + "。具体说：" + yongShenPlain(bazi) + "。";
    }

    // ==================================================================== 风水

    /** 吉凶等级的白话。 */
    public static String gradePlain(FengShuiField.Grade grade) {
        return switch (grade) {
            case GREAT_LUCK -> "风水宝地";
            case LUCK -> "这地方不错";
            case PLAIN -> "此地平平";
            case BAD -> "这地方不太行";
            case DIRE -> "这地方危险";
        };
    }

    /** 吉凶等级的下一步建议。 */
    public static String gradeAdvice(FengShuiField.Grade grade) {
        return switch (grade) {
            case GREAT_LUCK -> "就是这里，建家吧";
            case LUCK -> "可以在这里安家";
            case PLAIN -> "能用，但可以再走走看";
            case BAD -> "别在这儿住太久";
            case DIRE -> "赶紧离开，或者先摆个香炉";
        };
    }

    /** 气性白话：气聚 / 煞气重 / 阴气重。 */
    public static String qiPlain(FengShuiField.Sample sample) {
        List<String> notes = new ArrayList<>();
        if (sample.qi() >= 70) {
            notes.add("气聚，这地方舒服");
        } else if (sample.qi() <= 30) {
            notes.add("气散，待着不舒坦");
        }
        if (sample.sha() >= 70) {
            notes.add("煞气重，这地方危险");
        }
        if (sample.yin() - sample.yang() >= 30) {
            notes.add("阴气重，晚上容易刷怪");
        } else if (sample.yang() - sample.yin() >= 30) {
            notes.add("阳气足，白天很安心");
        }
        if (notes.isEmpty()) {
            notes.add("各方面都中等");
        }
        return String.join("；", notes);
    }

    /**
     * 哪个方向的地更好。
     *
     * <p>实测八方向各走一段距离，返回最好那个方向的白话 —— "往南走走看"。
     * 这是罗盘第二层提示的核心：它把"评分低"翻译成"该往哪走"。
     *
     * @param step 探测距离（方块）
     * @return 白话方向；四周都差不多时返回 {@code null}
     */
    public static String betterDirection(long seed, DimensionProfile dimension,
                                         int x, int z, int step) {
        FengShuiField.Sample here = FengShuiField.sample(seed, dimension, x, z);
        int hereScore = here.score(dimension);

        String[] names = {"北", "东北", "东", "东南", "南", "西南", "西", "西北"};
        int[][] offsets = {
                {0, -step}, {step, -step}, {step, 0}, {step, step},
                {0, step}, {-step, step}, {-step, 0}, {-step, -step}
        };

        int bestIndex = -1;
        int bestScore = hereScore;
        for (int i = 0; i < 8; i++) {
            FengShuiField.Sample probe = FengShuiField.sample(seed, dimension,
                    x + offsets[i][0], z + offsets[i][1]);
            int score = probe.score(dimension);
            if (score > bestScore + 6) {      // 明显更好才推荐，避免来回指
                bestScore = score;
                bestIndex = i;
            }
        }
        if (bestIndex < 0) {
            return null;
        }
        return "往" + names[bestIndex] + "走走看，那边更好";
    }

    // ==================================================================== 六爻

    /** 六亲的白话。 */
    public static String liuQinPlain(LiuQin liuQin, boolean isYongShen) {
        return switch (liuQin) {
            case WEALTH -> isYongShen ? "财运好" : "跟钱财有关";
            case OFFICER -> isYongShen ? "有危险" : "跟危险或官府有关";
            case CHILD -> isYongShen ? "平安" : "跟福气和帮手有关";
            case PARENT -> isYongShen ? "有消息和庇护" : "跟文书住处有关";
            case BROTHER -> isYongShen ? "同伴会帮你" : "跟同辈竞争有关";
        };
    }

    /** 六神的白话。 */
    public static String liuShenPlain(LiuShen liuShen) {
        return switch (liuShen) {
            case QING_LONG -> "运气好";
            case ZHU_QUE -> "会有消息或口舌";
            case GOU_CHEN -> "事情会拖一阵子";
            case TENG_SHE -> "可能有意外的怪事";
            case BAI_HU -> "容易受伤";
            case XUAN_WU -> "小心被偷被抢";
        };
    }

    /** 空亡的白话。 */
    public static String kongPlain() {
        return "这事可能落空";
    }

    /** 动爻的白话。 */
    public static String movingPlain(int count) {
        if (count == 0) {
            return "事情会照现在的样子继续";
        }
        if (count == 1) {
            return "有一处会起变化";
        }
        return "变数不少，宜随机应变";
    }

    /**
     * 一卦的白话 —— 这是整个系统最重要的一句输出。
     *
     * <p>目标形态：
     * <pre>
     *   往西南走，地下有矿，小心岩浆。
     *   财运好，这事能成。
     * </pre>
     * 而不是：
     * <pre>
     *   地天泰变雷天大壮，妻财持世，白虎临二爻。
     * </pre>
     */
    public static List<String> readingPlain(Divination.Reading reading) {
        List<String> lines = new ArrayList<>();
        Divination.Question question = reading.input().question();

        // ---- 第一句：往哪走、去找什么 ----
        StringBuilder action = new StringBuilder();
        String direction = null;
        String target = null;
        String caution = null;
        for (Divination.Clue clue : reading.clues()) {
            switch (clue.label()) {
                case "方位" -> direction = clue.value();
                case "趋向" -> target = clue.value();
                case "风险" -> caution = clue.value();
                default -> {
                }
            }
        }
        if (direction != null) {
            action.append("往").append(cleanDirection(direction)).append("走");
        }
        if (target != null) {
            if (action.length() > 0) {
                action.append("，");
            }
            action.append(targetPlain(question, target));
        }
        if (caution != null) {
            if (action.length() > 0) {
                action.append("，");
            }
            action.append(cautionPlain(caution));
        }
        if (action.length() > 0) {
            lines.add(action + "。");
        }

        // ---- 第二句：这事顺不顺 ----
        int score = reading.score();
        if (score >= 45) {
            lines.add("这事很顺，放手去做。");
        } else if (score >= 15) {
            lines.add("这事能成，路上留个心眼。");
        } else if (score >= -14) {
            lines.add("这事不急，等等再说也行。");
        } else if (score >= -44) {
            lines.add("这事不顺，最好换条路。");
        } else {
            lines.add("这事先放下，别硬来。");
        }

        // ---- 第三句：一句话点出关键状态 ----
        Divination.Yao yong = reading.yongShenYao();
        List<String> notes = new ArrayList<>();
        notes.add(liuQinPlain(reading.yongShen(), true));
        if (yong.kong()) {
            notes.add(kongPlain());
        }
        for (Divination.Yao yao : reading.yaos()) {
            if (yao.moving() && (yao.liuShen() == LiuShen.BAI_HU || yao.liuShen() == LiuShen.XUAN_WU)) {
                notes.add(liuShenPlain(yao.liuShen()));
                break;
            }
        }
        notes.add(movingPlain(reading.movingYao().size()));
        lines.add(String.join("，", notes) + "。");

        return lines;
    }

    /** 把卦里给的方位描述收成两三个字。 */
    private static String cleanDirection(String direction) {
        if (direction == null) {
            return "附近";
        }
        for (String name : new String[]{"东北", "东南", "西南", "西北", "北", "南", "东", "西"}) {
            if (direction.contains(name)) {
                return name;
            }
        }
        return "附近";
    }

    /** 问事类型 → "去找什么"的白话。 */
    private static String targetPlain(Divination.Question question, String target) {
        return switch (question) {
            case FIND_ORE -> "地下有矿";
            case FIND_PATH -> "路能走通";
            case BATTLE -> "那边有仗要打";
            case TRADE -> "那边做买卖划算";
            case ENCHANT -> "适合附魔";
            case WEATHER -> "天气会有变化";
            case FIND_PERSON -> "能找到你要找的人";
            case FIND_RUINS -> "有遗迹";
            case HEAL -> "能恢复";
            case SAFE -> "平平安安";
        };
    }

    /** 风险描述 → 白话。 */
    private static String cautionPlain(String caution) {
        if (caution == null) {
            return "";
        }
        if (caution.contains("太平")) {
            return "不会有什么危险";
        }
        if (caution.contains("小伤")) {
            return "小心别受伤";
        }
        if (caution.contains("甲胄")) {
            return "带上装备再去";
        }
        if (caution.contains("白虎") || caution.contains("大劫")) {
            return "那里很危险，做好万全准备";
        }
        return caution;
    }

    // ==================================================================== 内置帮助

    /** 命盘玉简的长按帮助。 */
    public static List<String> fateHelp(Bazi bazi) {
        List<String> lines = new ArrayList<>();
        lines.add("【命盘】");
        if (bazi != null) {
            lines.add(fatePlain(bazi));
            lines.add("");
            lines.add("简单说：住在对你有利属性的地方，你会更强。");
            lines.add("比如喜火，就住在岩浆或火把多的基地附近。");
        }
        lines.add("");
        lines.add("（按 B 随时查看完整命盘）");
        return lines;
    }

    /** 罗盘的 Shift+右键图例。 */
    public static List<String> compassHelp() {
        return List.of(
                "【罗盘怎么看】",
                "指针变绿 = 好地方，变红 = 差地方。",
                "右键空气：看看脚下这块地怎么样。",
                "对着一扇门右键：看看你家布局有没有问题。",
                "",
                "颜色含义：金边=风水宝地，绿=不错，灰=一般，",
                "橙=不太好，红=危险。"
        );
    }

    /** 起卦器具的帮助。 */
    public static List<String> divinationHelp() {
        return List.of(
                "【怎么问卦】",
                "右键 → 选你要问的事 → 掷卦。",
                "卦会告诉你：往哪走、那儿有什么、危不危险。",
                "按着卦的提示去走，多半不会错。",
                "",
                "每天能问的次数有限，问多了卦会变得模糊。"
        );
    }

    /** 观气台的帮助。 */
    public static List<String> observatoryHelp() {
        return List.of(
                "【观气台】",
                "放在家里右键，它会告诉你：",
                "  1. 这块地风水好不好（条形图越长越好）",
                "  2. 你家布局有没有问题（会给出改法）",
                "",
                "条形图：气=舒适度，煞=危险度（越短越好），",
                "阴/阳=平衡度（两边一样长最好）。"
        );
    }

    /** 香炉的帮助。 */
    public static List<String> censerHelp() {
        return List.of(
                "【香炉】",
                "在风水差的地方点一炷香，能把那片地方的煞气压下去。",
                "多上几次香效果更好，最多七炷。",
                "盖在凶地旁边的家里特别有用。"
        );
    }

    /** 石敢当的帮助。 */
    public static List<String> shiGandangHelp(String direction) {
        return List.of(
                "【石敢当】",
                "每年有一个方向不能随便挖土挖矿（叫太岁方），",
                "挖了会倒霉。今年的太岁方在" + direction + "。",
                "在那个方向附近立一块石敢当，就能放心挖了。"
        );
    }

    /** 卦盘的帮助。 */
    public static List<String> guaBoardHelp() {
        return List.of(
                "【卦盘】",
                "右键掷一卦，六个面各输出一路红石信号：",
                "亮 = 阳爻，灭 = 阴爻。",
                "懂红石的人可以拿它做自动机关。"
        );
    }

    // ==================================================================== 颜色

    /** 吉凶等级对应的界面主色（第一层的"颜色"）。 */
    public static int gradeColor(FengShuiField.Grade grade) {
        return switch (grade) {
            case GREAT_LUCK -> 0xFFE8C860;   // 金
            case LUCK -> 0xFF6ED08A;         // 绿
            case PLAIN -> 0xFFB8B8B0;        // 灰
            case BAD -> 0xFFE09A50;          // 橙
            case DIRE -> 0xFFE05A5A;         // 红
        };
    }

    /** 卦的吉凶背景色（金大吉 / 绿吉 / 灰平 / 橙凶 / 红大凶）。 */
    public static int scoreColor(int score) {
        if (score >= 45) {
            return 0xFFE8C860;
        }
        if (score >= 15) {
            return 0xFF6ED08A;
        }
        if (score >= -14) {
            return 0xFFB8B8B0;
        }
        if (score >= -44) {
            return 0xFFE09A50;
        }
        return 0xFFE05A5A;
    }

    /** 生成一个稳定的"这地方怎么形容"短语，避免每次提示措辞都变。 */
    public static String placePhrase(long seed, DimensionProfile dimension, int x, int z) {
        int pick = StableKey.of(seed, "phrase").add(dimension.shortKey()).add(x).add(z).index(4);
        String[] phrases = {"这一带", "这片地", "这附近", "此处"};
        return phrases[pick];
    }
}
