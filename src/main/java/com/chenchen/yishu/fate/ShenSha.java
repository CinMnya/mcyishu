package com.chenchen.yishu.fate;

import com.chenchen.yishu.fate.GanZhi.Gan;
import com.chenchen.yishu.fate.GanZhi.WuXing;
import com.chenchen.yishu.fate.GanZhi.Zhi;

import java.util.ArrayList;
import java.util.List;

/**
 * 易枢 · 神煞 —— 命盘上的「标记点」。
 *
 * <p>神煞不是独立的判断体系，而是<b>标签</b>：它会告诉你「这个人身上带着什么味道」。
 * 比如带天乙贵人的命，走到哪都有人帮；带驿马的人注定奔波；带羊刃的人脾气锐利。
 * 在模组里，神煞被映射成具体的玩法倾向 —— 见 {@link #effects(java.util.List)}。
 *
 * <p>所有查表规则都是传统术数里最通行的那一版（以日干查者、以年支三合查者），
 * 写成静态表而不是公式，就是为了让人一眼能核对。
 */
public final class ShenSha {

    private ShenSha() {
    }

    /**
     * 一个神煞。
     *
     * @param key       稳定标识（用于本地化键 {@code shensha.yishu.<key>}）
     * @param name      中文名
     * @param auspice   +1 吉 / 0 中性 / -1 凶
     * @param note      一句解释
     */
    public record Mark(String key, String name, int auspice, String note) {
    }

    // ---- 吉神 ----
    public static final Mark TIAN_YI = new Mark("tianyi", "天乙贵人", 1, "命中带贵，遇难有人相扶，交易更易得让价");
    public static final Mark WEN_CHANG = new Mark("wenchang", "文昌贵人", 1, "聪慧好学，附魔与解读卦象更省力");
    public static final Mark LU_SHEN = new Mark("lushen", "禄神", 1, "衣食自来，采集与掉落略有加成");
    public static final Mark JIANG_XING = new Mark("jiangxing", "将星", 1, "统御之才，同伴生物更易跟随");
    public static final Mark HUA_GAI = new Mark("huagai", "华盖", 1, "孤高有才艺，独处时悟性更佳");

    // ---- 凶煞 ----
    public static final Mark YANG_REN = new Mark("yangren", "羊刃", -1, "性烈易折，攻击更狠但受伤更重");
    public static final Mark JIE_SHA = new Mark("jiesha", "劫煞", -1, "多意外之失，被抢夺与被爆的几率上升");
    public static final Mark WANG_SHEN = new Mark("wangshen", "亡神", -1, "易迷路、易中埋伏，探索时风险更高");
    public static final Mark TAO_HUA = new Mark("taohua", "桃花", 0, "人缘极佳，但夜里也更容易招惹是非");
    public static final Mark YI_MA = new Mark("yima", "驿马", 0, "注定奔波，移动更快、跑图更省力");

    private static final List<Mark> ALL = List.of(
            TIAN_YI, WEN_CHANG, LU_SHEN, JIANG_XING, HUA_GAI,
            YANG_REN, JIE_SHA, WANG_SHEN, TAO_HUA, YI_MA);

    public static List<Mark> all() {
        return ALL;
    }

    // ==================================================================== 查表

    /** 天乙贵人（以日干查）。 */
    private static Zhi[] tianYi(Gan dayMaster) {
        return switch (dayMaster) {
            case JIA, WU, GENG -> new Zhi[]{Zhi.CHOU, Zhi.WEI};
            case YI, JI -> new Zhi[]{Zhi.ZI, Zhi.SHEN};
            case BING, DING -> new Zhi[]{Zhi.HAI, Zhi.YOU};
            case REN, GUI -> new Zhi[]{Zhi.MAO, Zhi.SI};
            case XIN -> new Zhi[]{Zhi.WU, Zhi.YIN};
        };
    }

    /** 文昌贵人（以日干查）。 */
    private static Zhi wenChang(Gan dayMaster) {
        return switch (dayMaster) {
            case JIA -> Zhi.SI;
            case YI -> Zhi.WU;
            case BING, WU -> Zhi.SHEN;
            case DING, JI -> Zhi.YOU;
            case GENG -> Zhi.HAI;
            case XIN -> Zhi.ZI;
            case REN -> Zhi.YIN;
            case GUI -> Zhi.MAO;
        };
    }

    /** 禄神（以日干查）。 */
    private static Zhi luShen(Gan dayMaster) {
        return switch (dayMaster) {
            case JIA -> Zhi.YIN;
            case YI -> Zhi.MAO;
            case BING, WU -> Zhi.SI;
            case DING, JI -> Zhi.WU;
            case GENG -> Zhi.SHEN;
            case XIN -> Zhi.YOU;
            case REN -> Zhi.HAI;
            case GUI -> Zhi.ZI;
        };
    }

    /** 羊刃（以日干查）。 */
    private static Zhi yangRen(Gan dayMaster) {
        return switch (dayMaster) {
            case JIA -> Zhi.MAO;
            case YI -> Zhi.CHEN;
            case BING, WU -> Zhi.WU;
            case DING, JI -> Zhi.WEI;
            case GENG -> Zhi.YOU;
            case XIN -> Zhi.XU;
            case REN -> Zhi.ZI;
            case GUI -> Zhi.CHOU;
        };
    }

    /** 三合局定位：由年支定「组」，再映射到目标地支。 */
    private static Zhi trineTarget(Zhi anchor, Zhi shenZiChen, Zhi yinWuXu, Zhi siYouChou, Zhi haiMaoWei) {
        return switch (anchor) {
            case SHEN, ZI, CHEN -> shenZiChen;
            case YIN, WU, XU -> yinWuXu;
            case SI, YOU, CHOU -> siYouChou;
            case HAI, MAO, WEI -> haiMaoWei;
        };
    }

    private static Zhi taoHua(Zhi anchor) {
        return trineTarget(anchor, Zhi.YOU, Zhi.MAO, Zhi.WU, Zhi.ZI);
    }

    private static Zhi yiMa(Zhi anchor) {
        return trineTarget(anchor, Zhi.YIN, Zhi.SHEN, Zhi.HAI, Zhi.SI);
    }

    private static Zhi huaGai(Zhi anchor) {
        return trineTarget(anchor, Zhi.CHEN, Zhi.XU, Zhi.CHOU, Zhi.WEI);
    }

    private static Zhi jieSha(Zhi anchor) {
        return trineTarget(anchor, Zhi.SI, Zhi.HAI, Zhi.YIN, Zhi.SHEN);
    }

    private static Zhi wangShen(Zhi anchor) {
        return trineTarget(anchor, Zhi.HAI, Zhi.SI, Zhi.SHEN, Zhi.YIN);
    }

    // ==================================================================== 推算

    /**
     * 推一盘八字的神煞。
     *
     * <p>规则归属：
     * <ul>
     *   <li>天乙、文昌、禄神、羊刃 —— 看<b>日干</b>，落在四柱任一支上即算带煞；</li>
     *   <li>桃花、驿马、华盖、劫煞、亡神 —— 看<b>年支</b>（三合局定位），同理。</li>
     * </ul>
     */
    public static List<Mark> of(Bazi bazi) {
        List<Zhi> branches = List.of(
                bazi.year().zhi(), bazi.month().zhi(), bazi.day().zhi(), bazi.hour().zhi());

        List<Mark> result = new ArrayList<>();
        Gan dayMaster = bazi.dayMaster();

        if (branches.contains(anyOf(tianYi(dayMaster), branches))) {
            result.add(TIAN_YI);
        }
        if (branches.contains(wenChang(dayMaster))) {
            result.add(WEN_CHANG);
        }
        if (branches.contains(luShen(dayMaster))) {
            result.add(LU_SHEN);
        }
        if (branches.contains(yangRen(dayMaster))) {
            result.add(YANG_REN);
        }

        Zhi anchor = bazi.year().zhi();
        if (branches.contains(taoHua(anchor))) {
            result.add(TAO_HUA);
        }
        if (branches.contains(yiMa(anchor))) {
            result.add(YI_MA);
        }
        if (branches.contains(huaGai(anchor))) {
            result.add(HUA_GAI);
        }
        if (branches.contains(jieSha(anchor))) {
            result.add(JIE_SHA);
        }
        if (branches.contains(wangShen(anchor))) {
            result.add(WANG_SHEN);
        }
        return result;
    }

    /** 天乙贵人有两个落点，只要命中其一即可。 */
    private static Zhi anyOf(Zhi[] candidates, List<Zhi> branches) {
        for (Zhi candidate : candidates) {
            if (branches.contains(candidate)) {
                return candidate;
            }
        }
        return candidates[0];
    }

    /** 将星：以年支三合局的「中神」为准（三合局的第二个字）。 */
    public static Mark jiangXingIf(Bazi bazi) {
        Zhi anchor = bazi.year().zhi();
        Zhi target = trineTarget(anchor, Zhi.ZI, Zhi.WU, Zhi.YOU, Zhi.MAO);
        List<Zhi> branches = List.of(
                bazi.year().zhi(), bazi.month().zhi(), bazi.day().zhi(), bazi.hour().zhi());
        return branches.contains(target) ? JIANG_XING : null;
    }

    // ==================================================================== 效果

    /**
     * 神煞在模组里的具体效果。
     *
     * <p>把「吉凶」翻译成可玩的数字，是这套系统能落地的关键。这里只列会被实际读取的项，
     * 没有被读到的神煞就只是命盘上的一行字 —— 那也是有价值的（它是社交谈资）。
     */
    public record Effects(double tradeDiscount, double lootBonus, double speedBonus,
                          double incomingDamage, double xpBonus, double mobThreat) {
    }

    public static Effects effects(List<Mark> marks) {
        double trade = 1.0D;
        double loot = 1.0D;
        double speed = 1.0D;
        double incoming = 1.0D;
        double xp = 1.0D;
        double threat = 1.0D;
        for (Mark mark : marks) {
            switch (mark.key()) {
                case "tianyi" -> trade -= 0.06D;
                case "wenchang" -> xp += 0.10D;
                case "lushen" -> loot += 0.08D;
                case "jiangxing" -> threat -= 0.05D;
                case "huagai" -> xp += 0.05D;
                case "yangren" -> {
                    incoming += 0.10D;
                    threat += 0.05D;
                }
                case "jiesha" -> loot -= 0.08D;
                case "wangshen" -> threat += 0.10D;
                case "taohua" -> threat += 0.06D;
                case "yima" -> speed += 0.05D;
                default -> {
                }
            }
        }
        return new Effects(trade, loot, speed, incoming, xp, threat);
    }

    /** 神煞命中的五行提示：太多凶煞时命盘偏「浊」。 */
    public static WuXing dominantFlavor(List<Mark> marks) {
        int negative = 0;
        for (Mark mark : marks) {
            if (mark.auspice() < 0) {
                negative++;
            }
        }
        return negative >= 3 ? WuXing.TU : WuXing.JIN;
    }
}
