package com.chenchen.yishu.liuyao;

import com.chenchen.yishu.YishuConfig;
import com.chenchen.yishu.core.StableKey;
import com.chenchen.yishu.core.YishuRandom;
import com.chenchen.yishu.fate.GanZhi.Gan;
import com.chenchen.yishu.fate.GanZhi.Pair;
import com.chenchen.yishu.fate.GanZhi.WuXing;
import com.chenchen.yishu.fate.GanZhi.Zhi;
import com.chenchen.yishu.liuyao.Gua.LiuQin;
import com.chenchen.yishu.liuyao.Gua.LiuShen;
import com.chenchen.yishu.liuyao.Gua.NaJia;
import com.chenchen.yishu.liuyao.Hexagram.Palace;

import java.util.ArrayList;
import java.util.List;

/**
 * 易枢 · 六爻起卦与解卦引擎。
 *
 * <p>这个文件回答一个问题：<b>「我该往哪走」</b>。
 *
 * <p>完整流程：
 * <pre>
 *   起卦（三法）  →  装卦（纳甲 / 六亲 / 六神 / 世应 / 空亡 / 旺衰）
 *                 →  取用神（按问事类型）
 *                 →  断旺衰生克
 *                 →  输出线索（方位 / 群系 / 层高 / 时窗 / 风险）
 * </pre>
 *
 * <p><b>确定性的落点</b>：起卦的输入键是
 * {@code worldSeed + 维度 + 主体 + 问题 + 游戏日 + 时辰}。
 * 这六个量在规范里被明确列出 —— 也就是说，只要你、别人、三个月后的你
 * 站在同一个种子的同一个游戏日同一时辰问同一件事，摇出来的就是同一卦。
 * <b>这一条是整个模组能被「分享」的基础</b>：玩家可以把自己的卦贴到论坛上，
 * 别人在同样的世界里能复现它。
 *
 * <p><b>刻意不给坐标</b>：解卦只输出方位、群系、Y 层范围、时间窗口与风险，
 * 并且按配置的「随机性等级」进一步模糊化。这不是偷懒 —— 直接给坐标会让探索
 * 变成执行任务清单，把「占卜」这件事的意义抹掉。
 */
public final class Divination {

    private Divination() {
    }

    // ==================================================================== 问事

    /**
     * 问事类型。
     *
     * <p>{@code yongShen} 是这个类型的「用神」——也就是卦里哪一类六亲代表你要问的事。
     * 取错用神是初学者最常犯的错：问「找矿」却去看官鬼，看到的全是凶。
     */
    public enum Question {
        FIND_ORE("寻矿探脉", LiuQin.WEALTH, "问地脉走向，看妻财"),
        FIND_PATH("寻路问向", LiuQin.PARENT, "问前路通塞，看父母"),
        BATTLE("战守胜负", LiuQin.OFFICER, "问拼杀吉凶，看官鬼"),
        TRADE("交易买卖", LiuQin.WEALTH, "问物价盈亏，看妻财"),
        ENCHANT("附魔炼器", LiuQin.PARENT, "问器道成败，看父母"),
        WEATHER("晴雨风云", LiuQin.PARENT, "问天时变化，看父母"),
        FIND_PERSON("寻人问踪", LiuQin.BROTHER, "问同辈去向，看兄弟"),
        FIND_RUINS("寻幽访古", LiuQin.CHILD, "问遗迹所在，看子孙"),
        HEAL("疗疾续命", LiuQin.CHILD, "问医药效力，看子孙"),
        SAFE("祈求平安", LiuQin.CHILD, "问出行安危，看子孙");

        private final String zh;
        private final LiuQin yongShen;
        private final String note;

        Question(String zh, LiuQin yongShen, String note) {
            this.zh = zh;
            this.yongShen = yongShen;
            this.note = note;
        }

        public String zh() {
            return this.zh;
        }

        public LiuQin yongShen() {
            return this.yongShen;
        }

        public String note() {
            return this.note;
        }

        public static final Question[] VALUES = values();

        /** 界面顺序。 */
        public static List<Question> all() {
            return List.of(VALUES);
        }
    }

    /** 起卦方式。 */
    public enum Method {
        COIN("铜钱六掷", "三枚铜钱摇六次，最正统也最慢"),
        TIME("时间起卦", "以当下时辰与日期成卦，无需铜钱"),
        OMEN("外应起卦", "以所处环境（天候、脚下的方块、朝向）为卦");

        private final String zh;
        private final String note;

        Method(String zh, String note) {
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

    // ==================================================================== 起卦输入

    /**
     * 起卦的输入。
     *
     * <p>{@code omen} 是外应起卦用的环境熵 —— 注意它<b>不是</b>随机数，
     * 而是「你站的位置、脚下方块、面前的朝向」这些东西的稳定编码。
     * 同样的位置再问一次，外应是一样的。
     */
    public record Input(long worldSeed, String dimension, String subject, Question question,
                        Method method, long gameDay, int hourIndex, long omen) {

        /** 起卦用的稳定键 —— 这六个量决定了唯一结果。 */
        public StableKey key() {
            StableKey key = StableKey.of(this.worldSeed, "divination")
                    .add(this.dimension)
                    .add(this.subject)
                    .add(this.question.name())
                    .add(this.gameDay)
                    .add(this.hourIndex);
            if (this.method == Method.OMEN) {
                key.add("omen").add(this.omen);
            } else if (this.method == Method.TIME) {
                key.add("time");
            } else {
                key.add("coin");
            }
            return key;
        }
    }

    // ==================================================================== 结果

    /**
     * 一个爻的装卦结果。
     *
     * @param position 爻位 1..6（初爻为 1）
     * @param yang     阴阳
     * @param moving   是否动爻
     * @param najia    纳甲干支
     * @param liuQin   六亲
     * @param liuShen  六神
     * @param world    是否世爻
     * @param response 是否应爻
     * @param kong     是否空亡
     * @param wangShuai 旺衰名（旺 / 相 / 休 / 囚 / 死）
     */
    public record Yao(int position, boolean yang, boolean moving, NaJia najia, LiuQin liuQin,
                      LiuShen liuShen, boolean world, boolean response, boolean kong, String wangShuai) {

        /** 爻的符号：阳爻 ▬▬▬ / 阴爻 ▬ ▬，动爻加圈。 */
        public String glyph() {
            String base = this.yang ? "▬▬▬▬▬" : "▬▬ ▬▬";
            return this.moving ? base + " ○" : base;
        }

        public String line() {
            return this.position + " 爻  " + this.najia.zh() + " " + this.liuQin.zh()
                    + " " + this.liuShen.zh()
                    + (this.world ? " 【世】" : "")
                    + (this.response ? " 【应】" : "")
                    + (this.moving ? " 【动】" : "")
                    + (this.kong ? " 【空】" : "");
        }
    }

    /** 一条线索。 */
    public record Clue(String label, String value) {
    }

    /**
     * 一卦的完整解读。
     */
    public record Reading(Input input, Hexagram original, Hexagram changed, List<Yao> yaos,
                          List<Integer> movingYao, Pair dayPillar, Pair monthPillar,
                          LiuQin yongShen, String verdict, int score, List<Clue> clues,
                          boolean vague, String omenNote) {

        /** 用神所在的那一爻（取第一个匹配；都不匹配则取世爻）。 */
        public Yao yongShenYao() {
            for (Yao yao : this.yaos) {
                if (yao.liuQin() == this.yongShen) {
                    return yao;
                }
            }
            for (Yao yao : this.yaos) {
                if (yao.world()) {
                    return yao;
                }
            }
            return this.yaos.get(0);
        }

        public Yao worldYao() {
            for (Yao yao : this.yaos) {
                if (yao.world()) {
                    return yao;
                }
            }
            return this.yaos.get(0);
        }

        public Yao responseYao() {
            for (Yao yao : this.yaos) {
                if (yao.response()) {
                    return yao;
                }
            }
            return this.yaos.get(0);
        }

        public boolean hasMoving() {
            return !this.movingYao.isEmpty();
        }
    }

    // ==================================================================== 起卦

    /**
     * 完整起一卦。
     *
     * @param input 起卦输入
     * @param fuzz  额外模糊度（叠加在配置的随机性等级之上，用于跨维度占卜等因素）
     */
    public static Reading cast(Input input, double fuzz) {
        YishuRandom random = input.key().random();

        // ---- 第一步：摇出六个爻 ----
        boolean[] yang = new boolean[6];
        boolean[] moving = new boolean[6];
        switch (input.method()) {
            case COIN -> {
                for (int i = 0; i < 6; i++) {
                    // 三枚铜钱：正面数 0..3 → 老阴(6) 少阳(7) 少阴(8) 老阳(9)
                    int heads = 0;
                    for (int coin = 0; coin < 3; coin++) {
                        heads += random.nextBoolean() ? 1 : 0;
                    }
                    int value = switch (heads) {
                        case 0 -> 6;    // 三背 → 老阴，动
                        case 1 -> 7;    // 一正 → 少阳
                        case 2 -> 8;    // 二正 → 少阴
                        default -> 9;   // 三正 → 老阳，动
                    };
                    yang[i] = value == 7 || value == 9;
                    moving[i] = value == 6 || value == 9;
                }
            }
            case TIME, OMEN -> {
                // 时间 / 外应：直接由键落成六个爻，动爻由哈希决定 0~2 个
                for (int i = 0; i < 6; i++) {
                    yang[i] = random.nextBoolean();
                }
                int movingCount = random.nextInt(3);      // 0、1、2 个动爻
                int[] picks = random.sample(6, movingCount);
                for (int index : picks) {
                    moving[index] = true;
                }
            }
            default -> {
            }
        }

        return assemble(input, yang, moving, random, fuzz);
    }

    // ==================================================================== 装卦

    private static Reading assemble(Input input, boolean[] yang, boolean[] moving,
                                    YishuRandom random, double fuzz) {
        Hexagram original = Hexagram.ofYao(yang);

        // 变卦：所有动爻同时翻转
        boolean[] changedYang = yang.clone();
        List<Integer> movingList = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            if (moving[i]) {
                changedYang[i] = !changedYang[i];
                movingList.add(i + 1);
            }
        }
        Hexagram changed = movingList.isEmpty() ? original : Hexagram.ofYao(changedYang);

        // ---- 日柱与月柱（流日、流月）----
        Pair dayPillar = com.chenchen.yishu.fate.Yun.liuRi(input.worldSeed(), input.dimension(), input.gameDay());
        Pair monthPillar = com.chenchen.yishu.fate.Yun.liuYue(input.worldSeed(), input.dimension(), input.gameDay());

        // ---- 六神 ----
        LiuShen[] liuShen = LiuShen.arrange(dayPillar.gan(), input.dimension());

        // ---- 空亡（旬空）----
        Zhi[] kongWang = kongWang(dayPillar);

        // ---- 逐爻装卦 ----
        NaJia[] najia = original.najia();
        Palace palace = original.palace();
        WuXing palaceElement = palace.wuXing();
        int worldYao = original.worldYao();
        int responseYao = original.responseYao();

        List<Yao> yaos = new ArrayList<>(6);
        for (int i = 0; i < 6; i++) {
            int position = i + 1;
            LiuQin liuQin = LiuQin.of(palaceElement, najia[i].wuXing());
            boolean kong = kongWang[0] == najia[i].zhi() || kongWang[1] == najia[i].zhi();
            yaos.add(new Yao(position, yang[i], moving[i], najia[i], liuQin, liuShen[i],
                    position == worldYao, position == responseYao, kong,
                    wangShuai(najia[i].wuXing(), monthPillar.zhi().wuXing())));
        }

        // ---- 取用神与断卦 ----
        LiuQin yongShen = input.question().yongShen();
        Score scored = judge(yaos, yongShen, dayPillar, monthPillar, original, changed);

        double totalFuzz = Math.min(1.0D, YishuConfig.get().randomness.fuzz() + fuzz);
        boolean vague = random.chance(totalFuzz * 0.6D);

        List<Clue> clues = clues(input, yaos, yongShen, dayPillar, monthPillar, random, totalFuzz, scored);

        String omenNote = input.method() == Method.OMEN
                ? "外应已入卦：以你所立之处为凭"
                : input.method() == Method.TIME
                ? "以时起卦：此刻的时辰即是卦气"
                : "铜钱六掷：三枚铜钱已定六爻";

        return new Reading(input, original, changed, List.copyOf(yaos), List.copyOf(movingList),
                dayPillar, monthPillar, yongShen, scored.verdict, scored.score,
                List.copyOf(clues), vague, omenNote);
    }

    /**
     * 旬空（空亡）：日柱所在的那一旬里，配不到天干的两个地支。
     *
     * <p>六十甲子分六旬，每旬十天干配十二地支，必然多出两个地支轮空 —— 那就是空亡。
     * 空亡的爻「力量虚浮」，吉不成吉、凶不成凶，是断卦时最需要注意的一层。
     */
    public static Zhi[] kongWang(Pair dayPillar) {
        int cycle = dayPillar.cycleIndex();
        int xunStart = (cycle / 10) * 10;
        int first = Math.floorMod(xunStart % 12 + 10, 12);
        return new Zhi[]{Zhi.byIndex(first), Zhi.byIndex(first + 1)};
    }

    /**
     * 旺衰：以月令为基准判断这一爻的气。
     *
     * <p>规则是传统的「旺相休囚死」：
     * <ul>
     *   <li>同月令者 <b>旺</b></li>
     *   <li>月令所生者 <b>相</b></li>
     *   <li>生月令者 <b>休</b></li>
     *   <li>克月令者 <b>囚</b></li>
     *   <li>月令所克者 <b>死</b></li>
     * </ul>
     */
    public static String wangShuai(WuXing yao, WuXing month) {
        if (yao == month) {
            return "旺";
        }
        if (month.generates() == yao) {
            return "相";
        }
        if (yao.generates() == month) {
            return "休";
        }
        if (yao.overcomes() == month) {
            return "囚";
        }
        return "死";
    }

    /** 旺衰的数值权重，用于打分。 */
    private static double wangShuaiWeight(String name) {
        return switch (name) {
            case "旺" -> 1.6D;
            case "相" -> 1.25D;
            case "休" -> 0.85D;
            case "囚" -> 0.65D;
            default -> 0.45D;
        };
    }

    // ==================================================================== 断卦

    private record Score(int score, String verdict) {
    }

    /**
     * 断卦 —— 把一桌子的信息压成一个分数和一句话。
     *
     * <p>评分逻辑（这是本模组的诠释，不是古籍原文，写清楚以免误解）：
     * <ul>
     *   <li>用神爻本身的气：旺相加分、囚死扣分；</li>
     *   <li>日辰与用神的关系：日辰生爻大吉、日辰克爻大凶；</li>
     *   <li>用神空亡：分数整体打折（虚而不实）；</li>
     *   <li>动爻是否生用神：生则助、克则损；</li>
     *   <li>世应关系：世应相生则顺、相克则逆；</li>
     *   <li>之卦（变卦）吉凶倾向的加权修正。</li>
     * </ul>
     */
    private static Score judge(List<Yao> yaos, LiuQin yongShen, Pair dayPillar, Pair monthPillar,
                               Hexagram original, Hexagram changed) {
        double score = 0.0D;

        Yao yong = null;
        for (Yao yao : yaos) {
            if (yao.liuQin() == yongShen) {
                yong = yao;
                break;
            }
        }
        if (yong == null) {
            // 用神不上卦 —— 事情本就无从谈起，这是「不利」的强信号
            return new Score(-30, "用神不上卦，事无所主，宜缓不宜急");
        }

        score += (wangShuaiWeight(yong.wangShuai()) - 1.0D) * 40.0D;

        String relation = Gua.relationName(yong.najia().wuXing(), dayPillar.zhi().wuXing());
        score += switch (relation) {
            case "日辰生爻" -> 25.0D;
            case "比和" -> 12.0D;
            case "爻克日辰" -> -8.0D;
            case "爻生日辰" -> -15.0D;
            default -> -25.0D;      // 日辰克爻
        };

        if (yong.kong()) {
            score *= 0.55D;
        }

        for (Yao yao : yaos) {
            if (!yao.moving()) {
                continue;
            }
            WuXing from = yao.najia().wuXing();
            WuXing to = yong.najia().wuXing();
            if (from.generates() == to) {
                score += 12.0D;
            } else if (from.overcomes() == to) {
                score -= 14.0D;
            }
        }

        Yao world = yaos.get(original.worldYao() - 1);
        Yao response = yaos.get(original.responseYao() - 1);
        if (world.najia().wuXing().generates() == response.najia().wuXing()) {
            score += 8.0D;
        } else if (world.najia().wuXing().overcomes() == response.najia().wuXing()) {
            score -= 8.0D;
        }

        score += changed.auspice() * 6.0D;
        score -= original.auspice() * 3.0D;

        int rounded = (int) Math.max(-100.0D, Math.min(100.0D, Math.round(score)));
        String verdict;
        if (rounded >= 45) {
            verdict = "大吉——卦气助你，放手去做";
        } else if (rounded >= 15) {
            verdict = "吉——顺中有阻，可行但有需留意之处";
        } else if (rounded >= -14) {
            verdict = "平——此卦不明，宜守常待时";
        } else if (rounded >= -44) {
            verdict = "凶——用神受制，强行恐有损耗";
        } else {
            verdict = "大凶——六爻皆逆，此路不通";
        }
        return new Score(rounded, verdict);
    }

    // ==================================================================== 线索

    /**
     * 生成探索线索。
     *
     * <p>五类线索对应规范里写的「只给线索：方位、群系、Y 层范围、时间窗口、风险，
     * 不给精确坐标」。
     */
    private static List<Clue> clues(Input input, List<Yao> yaos, LiuQin yongShen,
                                    Pair dayPillar, Pair monthPillar,
                                    YishuRandom random, double fuzz, Score scored) {
        List<Clue> clues = new ArrayList<>();
        Yao yong = null;
        for (Yao yao : yaos) {
            if (yao.liuQin() == yongShen) {
                yong = yao;
                break;
            }
        }
        if (yong == null) {
            clues.add(new Clue("方位", "用神不上卦，方位无从定"));
            clues.add(new Clue("风险", "此事本无主，行之多空耗"));
            return clues;
        }

        // ---- 方位：由用神爻的爻位定先天八卦方位 ----
        String[] directionNames = {"北", "东北", "东", "东南", "南", "西南", "西", "西北"};
        String direction = directionNames[Math.floorMod(yong.position() + (fuzz > 0.5D ? 0 : 1), 8)];
        if (fuzz > 0.45D) {
            direction = "偏" + direction + "，但卦气飘忽，不敢定准";
        }
        clues.add(new Clue("方位", direction));

        // ---- 群系：由用神五行给候选池 ----
        List<String> biomes = biomePool(yong.najia().wuXing(), input.dimension());
        String biome = biomes.get(Math.floorMod(yong.position() * 7 + monthPillar.cycleIndex(), biomes.size()));
        clues.add(new Clue("趋向", biome));

        // ---- Y 层：由五行定高度带 ----
        clues.add(new Clue("层高", yBand(yong.najia().wuXing(), input.dimension(), fuzz)));

        // ---- 时间窗口：由日辰与动爻定 ----
        clues.add(new Clue("时窗", timeWindow(yaos, input, fuzz)));

        // ---- 风险：由官鬼与白虎（或对应维度的凶神）定 ----
        int riskLevel = 0;
        for (Yao yao : yaos) {
            if (yao.liuQin() == LiuQin.OFFICER) {
                riskLevel += yao.wangShuai().equals("旺") ? 2 : 1;
            }
            if (yao.liuShen() == LiuShen.BAI_HU || yao.liuShen() == LiuShen.XUAN_WU) {
                riskLevel += 1;
            }
        }
        String risk = switch (Math.min(riskLevel, 5)) {
            case 0 -> "卦中无鬼，此行太平";
            case 1 -> "略有官鬼，提防小伤";
            case 2 -> "官鬼现形，宜备甲胄";
            case 3 -> "鬼旺临白虎，凶险不小";
            default -> "官鬼重叠，此去恐有大劫";
        };
        clues.add(new Clue("风险", risk));

        // ---- 附加：总分 ----
        clues.add(new Clue("断语", scored.verdict()));

        if (fuzz > 0.6D) {
            clues.set(0, new Clue("方位", "卦气纷乱，方位不明"));
        }
        return clues;
    }

    private static List<String> biomePool(WuXing element, String dimension) {
        if ("the_nether".equals(dimension)) {
            return List.of("下界荒原", "灵魂沙谷", "绯红森林", "诡异森林", "玄武岩三角洲", "熔岩海");
        }
        if ("the_end".equals(dimension)) {
            return List.of("末地外岛", "末地高地", "紫颂林", "虚空边缘", "折跃门附近", "末地城周边");
        }
        return switch (element) {
            case JIN -> List.of("石峰与山地", "繁茂洞穴的岩层", "风袭丘陵", "深暗之域");
            case MU -> List.of("森林与林地", "繁花森林", "丛林深处", "红树林沼泽");
            case SHUI -> List.of("河流与海洋", "沼泽", "冰封海域", "繁茂洞穴的水池");
            case HUO -> List.of("沙漠与恶地", "热带草原", "岩浆湖周边", "下界裂缝上方");
            case TU -> List.of("平原与丘陵", "蘑菇岛", "雪原", "溶洞系统");
        };
    }

    private static String yBand(WuXing element, String dimension, double fuzz) {
        if ("the_nether".equals(dimension)) {
            return switch (element) {
                case JIN -> "高处：Y 70 以上的岩石顶盖";
                case MU -> "中层：Y 40–70 的林地夹层";
                case SHUI -> "此处无水，惟有灵魂沙上方的湿气层";
                case HUO -> "低处：Y 32 以下的岩浆带";
                case TU -> "中层：Y 40–80 的荒原";
            };
        }
        if ("the_end".equals(dimension)) {
            return "维度无高下，唯在离岛远近";
        }
        String base = switch (element) {
            case JIN -> "高处：Y 80 以上的山地岩层";
            case MU -> "地表附近：Y 62–90";
            case SHUI -> "低处：Y 30–62 的水系与洞穴";
            case HUO -> "极深处：Y 0–30，贴近岩浆";
            case TU -> "中低层：Y 20–60 的洞穴粘土带";
        };
        return fuzz > 0.5D ? base + "（卦气散，范围可再放宽）" : base;
    }

    private static String timeWindow(List<Yao> yaos, Input input, double fuzz) {
        int movingCount = 0;
        for (Yao yao : yaos) {
            if (yao.moving()) {
                movingCount++;
            }
        }
        if (fuzz > 0.55D) {
            return "时窗模糊，唯在没有定数";
        }
        if (movingCount == 0) {
            return "无动爻，此事长久可行，不争朝夕";
        }
        if (movingCount >= 3) {
            return "动爻众多，宜在三日内决断，迟则生变";
        }
        String[] hours = com.chenchen.yishu.fate.FateCalendar.TWELVE_HOURS;
        int index = Math.floorMod(input.hourIndex() + movingCount * 2, 12);
        return "宜在 " + hours[index] + " 前后行事（约游戏时 "
                + Zhi.byIndex(index).startHour() + " 点）";
    }

    // ==================================================================== 反噬

    /**
     * 反噬 —— 问卦问得太勤的代价。
     *
     * <p>设计意图：预测系统一旦没有成本，玩家就会把它当雷达用，游戏的探索感会消失。
     * 所以每天的问卦次数是有上限的，超限之后依次加重：
     * 线索模糊 → 伪卦 → 天雷 → 失明 → 引来怪物。
     *
     * <p>反噬<b>不影响确定性</b>：它惩罚的是「问得太多的行为」，不是「结果」。
     * 同一卦在同一天问出来还是同一卦，只是你看不清楚了而已。
     */
    public enum Backlash {
        NONE("无", "卦象清晰"),
        VAGUE("信息模糊", "卦气纷乱，只余轮廓"),
        FALSE_GUA("伪卦", "所得之卦与你所问之事无关，是被错答的"),
        THUNDER("天雷", "泄露天机，遭雷击"),
        BLIND("失明", "双眼为卦气所夺，暂时看不见"),
        HORDE("引怪", "卦象外泄，引来窥伺者的爪牙");

        private final String zh;
        private final String note;

        Backlash(String zh, String note) {
            this.zh = zh;
            this.note = note;
        }

        public String zh() {
            return this.zh;
        }

        public String note() {
            return this.note;
        }

        /** 当日第 {@code asked} 次问卦（含本次）对应的反噬等级。 */
        public static Backlash forCount(int asked, int limit) {
            if (asked <= limit) {
                return NONE;
            }
            return switch (asked - limit) {
                case 1 -> VAGUE;
                case 2 -> FALSE_GUA;
                case 3 -> THUNDER;
                case 4 -> BLIND;
                default -> HORDE;
            };
        }
    }
}
