package com.chenchen.yishu.dimension;

import com.chenchen.yishu.fate.GanZhi.WuXing;

import java.util.List;

/**
 * 易枢 · 维度差异 —— 同一套术数，在三个世界里长出三张脸。
 *
 * <p>规范第六节把三个维度写成了三种「文明的底色」：
 * <table border="1">
 *   <tr><th></th><th>主世界</th><th>下界</th><th>末地</th></tr>
 *   <tr><td>时间</td><td>十二时辰</td><td>十二火时</td><td>虚空脉</td></tr>
 *   <tr><td>五行</td><td>均衡</td><td>火土极旺，水绝</td><td>无五行，用虚空属性</td></tr>
 *   <tr><td>风水</td><td>阳宅风水</td><td>火煞、灵魂、金气、阴气</td><td>虚空、紫颂、龙气、阴煞</td></tr>
 *   <tr><td>六神</td><td>青龙朱雀勾陈螣蛇白虎玄武</td><td>烈焰灵魂金铁幽魂玄武岩熔岩</td><td>虚空紫颂龙息末影潜影折跃</td></tr>
 * </table>
 *
 * <p>这个类是这些差异的<b>唯一真源</b>。任何地方要问「下界的五行偏向是什么」，
 * 都从这里取，不允许在别处硬编码字符串 —— 否则改一处忘一处，
 * 三维度就会慢慢长成同一副样子。
 */
public enum DimensionProfile {

    /** 主世界：五行均衡，是唯一适合「阳宅风水」的地方。 */
    OVERWORLD("minecraft:overworld", "主世界", "十二时辰",
            List.of("子时", "丑时", "寅时", "卯时", "辰时", "巳时", "午时", "未时", "申时", "酉时", "戌时", "亥时"),
            WuXing.TU, 1.0D, List.of("阳宅", "山水", "道路", "明堂")),

    /** 下界：火土极旺、水绝。这里的「时间」是十二火时，没有昼夜，只有温度在变。 */
    NETHER("minecraft:the_nether", "下界", "十二火时",
            List.of("炽时", "灼时", "焚时", "炼时", "熔时", "炎时", "燚时", "烈时", "焦时", "烬时", "熄时", "冷时"),
            WuXing.HUO, 1.35D, List.of("火煞", "灵魂", "金气", "阴气")),

    /** 末地：无五行，一切以「虚空」论。时间不成钟点，只有虚空脉的起伏。 */
    END("minecraft:the_end", "末地", "虚空脉",
            List.of("空脉", "无脉", "寂脉", "灭脉", "湮脉", "冥脉", "幽脉", "玄脉", "虚脉", "极脉", "终脉", "始脉"),
            WuXing.SHUI, 0.75D, List.of("虚空", "紫颂", "龙气", "阴煞")),

    /**
     * 命理维度：时间按「十二长生」流转（长生→养），是唯一让天干效力全开的地方。
     * 在这里问卦、看命，一切都格外清晰 —— 因为整个维度就是一座巨大的命盘。
     */
    FATE_REALM("yishu:fate_realm", "命理维度", "十二长生",
            List.of("长生", "沐浴", "冠带", "临官", "帝旺", "衰", "病", "死", "墓", "绝", "胎", "养"),
            WuXing.TU, 1.20D, List.of("星斗", "命灯", "天干", "元辰")),

    /**
     * 风水维度：时间按「十二山水」流转（起脉→归藏），龙脉密布、气场浓稠。
     * 在这里安家，风水的每一分增益都被放大；但煞气也一样。
     */
    FENGSHUI_REALM("yishu:fengshui_realm", "风水维度", "十二山水",
            List.of("起脉", "行龙", "过峡", "束气", "结穴", "明堂", "案山", "朝山", "水口", "罗城", "藏风", "归藏"),
            WuXing.MU, 1.30D, List.of("龙脉", "明堂", "水口", "藏风")),

    /**
     * 六爻维度：时间按「十二消息卦」流转（复→坤），一昼夜就是一场完整的卦象呼吸。
     * 在这里起卦，卦气最真；但一天只有一卦，问多了反噬翻倍。
     */
    LIUYAO_REALM("yishu:liuyao_realm", "六爻维度", "十二消息卦",
            List.of("复", "临", "泰", "大壮", "夬", "乾", "姤", "遁", "否", "观", "剥", "坤"),
            WuXing.JIN, 1.10D, List.of("卦气", "爻位", "世应", "旬空"));

    private final String key;
    private final String zh;
    private final String timeSystemName;
    private final List<String> hourNames;
    private final WuXing dominant;
    private final double elementBias;
    private final List<String> fengShuiFlavors;

    DimensionProfile(String key, String zh, String timeSystemName, List<String> hourNames,
                     WuXing dominant, double elementBias, List<String> fengShuiFlavors) {
        this.key = key;
        this.zh = zh;
        this.timeSystemName = timeSystemName;
        this.hourNames = hourNames;
        this.dominant = dominant;
        this.elementBias = elementBias;
        this.fengShuiFlavors = fengShuiFlavors;
    }

    /** 维度键，与 {@code level.dimension().identifier().toString()} 对齐。 */
    public String key() {
        return this.key;
    }

    /** 简写键（{@code overworld} / {@code the_nether} / {@code the_end}），用于稳定哈希。 */
    public String shortKey() {
        int index = this.key.indexOf(':');
        return index < 0 ? this.key : this.key.substring(index + 1);
    }

    public String zh() {
        return this.zh;
    }

    /** 时间体系名：十二时辰 / 十二火时 / 虚空脉。 */
    public String timeSystemName() {
        return this.timeSystemName;
    }

    /** 时间体系的一句话说明，界面上跟在时辰后面显示。 */
    public String hourSystemNote() {
        return switch (this) {
            case OVERWORLD -> "昼夜十二时辰，子时起于夜半";
            case NETHER -> "无昼夜，唯火气盛衰十二变";
            case END -> "无钟点，唯虚空脉之起伏";
            case FATE_REALM -> "一昼夜走完十二长生，命气循环不息";
            case FENGSHUI_REALM -> "一昼夜走完一条龙脉的起结";
            case LIUYAO_REALM -> "一昼夜走完十二消息卦，卦气随之呼吸";
        };
    }

    public List<String> hourNames() {
        return this.hourNames;
    }

    /** 该维度的主气五行。 */
    public WuXing dominant() {
        return this.dominant;
    }

    /**
     * 五行偏向系数。
     *
     * <p>主世界 1.0（均衡），下界 1.35（火土格外有效，水被压制），末地 0.75（一切五行都弱）。
     * 这个数字会被风水与命理的效果计算乘上去 —— 也就是说，
     * 一个喜水的人在下界会格外难受，在末地则「大家都一样弱」。
     */
    public double elementBias() {
        return this.elementBias;
    }

    /** 该维度特有的风水气性。 */
    public List<String> fengShuiFlavors() {
        return this.fengShuiFlavors;
    }

    /** 该维度下某个五行是否被压制到近似不存在（下界的水）。 */
    public boolean isSuppressed(WuXing element) {
        return this == NETHER && element == WuXing.SHUI;
    }

    /** 该维度下某个五行是否格外旺盛。 */
    public boolean isAmplified(WuXing element) {
        return switch (this) {
            case NETHER -> element == WuXing.HUO || element == WuXing.TU;
            case END -> false;
            case FATE_REALM -> element == WuXing.TU;
            case FENGSHUI_REALM -> element == WuXing.MU || element == WuXing.SHUI;
            case LIUYAO_REALM -> element == WuXing.JIN;
            default -> false;
        };
    }

    // ==================================================================== 映射

    public static final DimensionProfile[] VALUES = values();

    /** 由维度键取档案；未知维度一律按主世界处理（模组只支持原版三维度）。 */
    public static DimensionProfile of(String dimensionKey) {
        if (dimensionKey == null) {
            return OVERWORLD;
        }
        String normalized = dimensionKey.contains(":") ? dimensionKey : "minecraft:" + dimensionKey;
        for (DimensionProfile profile : VALUES) {
            if (profile.key.equals(normalized) || profile.shortKey().equals(dimensionKey)) {
                return profile;
            }
        }
        return OVERWORLD;
    }

    // ==================================================================== 时辰

    /**
     * 把世界时间换算成这个维度的「时辰序号」0..11。
     *
     * <p>主世界沿用原版昼夜：一天 24000 刻，12 个时辰各占 2000 刻，
     * 子时从 18000 刻（黄昏后）开始 —— 这是 Minecraft 里夜晚的起点，
     * 也让「子时」这个名字真的对应到游戏里的深夜。
     */
    public int hourIndex(long dayTime) {
        long tickOfDay = Math.floorMod(dayTime, 24000L);
        return (int) (Math.floorMod(tickOfDay + 6000L, 24000L) / 2000L);
    }

    /** 这个维度里，当前时辰的名字。 */
    public String hourName(long dayTime) {
        return this.hourNames.get(this.hourIndex(dayTime));
    }

    /** 这个维度里，当前时段的吉凶提示（下界与末地各有一套说法）。 */
    public String hourAdvice(int hourIndex) {
        int index = Math.floorMod(hourIndex, 12);
        return switch (this) {
            case OVERWORLD -> com.chenchen.yishu.fate.FateCalendar.HOUR_MOOD[index];
            case NETHER -> switch (index) {
                case 0, 1, 2 -> "炽气升腾，火煞最盛，宜以阴物镇之";
                case 3, 4, 5 -> "熔流奔涌，金气暗藏，可采矿脉";
                case 6, 7, 8 -> "燚焰当空，灵魂浮动，宜祭不宜行";
                case 9, 10 -> "焦烬渐冷，煞气稍退，可稍作喘息";
                default -> "冷时降临，诸气沉寂，是下界唯一的安宁";
            };
            case END -> switch (index) {
                case 0, 1 -> "空脉初动，末影徘徊";
                case 2, 3, 4 -> "寂灭之气漫溢，虚空最躁";
                case 5, 6, 7 -> "幽玄交叠，龙气微醒";
                case 8, 9 -> "虚极而静，宜静观";
                default -> "终脉归始，天地重开一线";
            };
            case FATE_REALM -> switch (index) {
                case 0, 1 -> "长生沐浴，命气初生，宜始不宜止";
                case 2, 3, 4 -> "冠带临官帝旺，一日之盛，诸事可为";
                case 5, 6, 7 -> "衰病死气渐沉，宜守不宜攻";
                case 8, 9 -> "墓绝之地，旧事了结，宜断舍离";
                default -> "胎养待发，静候新生";
            };
            case FENGSHUI_REALM -> switch (index) {
                case 0, 1, 2 -> "起脉行龙，地气游走，宜勘察不宜动土";
                case 3, 4 -> "过峡束气，气最窄处藏机，宜谨慎";
                case 5, 6, 7 -> "结穴明堂案朝俱全，一日之中风水最盛";
                case 8, 9 -> "水口罗城收束，宜收纳整理";
                default -> "藏风归藏，诸气入库，宜安不宜动";
            };
            case LIUYAO_REALM -> switch (index) {
                case 0, 1, 2 -> "复临泰，阳气始长，问事先吉";
                case 3, 4, 5 -> "大壮夬乾，阳气极盛，卦气至純";
                case 6, 7, 8 -> "姤遁否，阴气渐起，话不宜说太满";
                case 9, 10 -> "观剥之象，宜观不宜断";
                default -> "坤卦归土，万事归藏，宜静思";
            };
        };
    }

    /**
     * 时辰的五行。
     *
     * <p>主世界按十二地支的五行；下界全部偏火（因为整个维度都在烧）；
     * 末地一律返回 {@link WuXing#SHUI} 作为「虚空」的载体五行。
     */
    public WuXing hourElement(int hourIndex) {
        return switch (this) {
            case OVERWORLD -> com.chenchen.yishu.fate.GanZhi.Zhi.byIndex(hourIndex).wuXing();
            case NETHER -> WuXing.HUO;
            case END -> WuXing.SHUI;
            case FATE_REALM -> WuXing.TU;
            case FENGSHUI_REALM -> WuXing.MU;
            case LIUYAO_REALM -> WuXing.JIN;
        };
    }
}
