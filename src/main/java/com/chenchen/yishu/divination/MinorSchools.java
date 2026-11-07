package com.chenchen.yishu.divination;

import com.chenchen.yishu.calendar.YishuCalendar;
import com.chenchen.yishu.core.StableKey;
import com.chenchen.yishu.core.YishuRandom;
import com.chenchen.yishu.taiyi.TaiYi;

import java.util.ArrayList;
import java.util.List;

/**
 * 易枢 · 其余术数的统一解读层（第 18 轮）。
 *
 * <p>这些门派各有各的起法，但游戏里只需要一件事：<b>同种子同刻，
 * 给出同一段说得通、且能推着玩家做点什么的话</b>。所以这里把它们收进
 * 一张枚举表，每门一个纯函数，共用 {@link StableKey} 派生随机流。
 *
 * <p>刻意不做成「又一个菜单」：断语里带的是<b>可执行的方向</b>
 * （宜往哪个方位、宜种什么、该不该动土、这只村民值不值得招募），
 * 上层系统可以直接读 {@link Verdict#blessing()} 当系数用。
 * 时间、节气、二十八宿全部来自 {@link YishuCalendar}，和时钟是同一套底层。
 */
public final class MinorSchools {

    private MinorSchools() {
    }

    /** 本层支持的门派。指令参数用这里的英文 key。 */
    public enum School {
        QIZHENG("qizheng", "七政四余", "星命 · 观星台与流年星象"),
        MEIHUA("meihua", "梅花易数", "轻量起卦 · 即时占卜"),
        XIAOLIUREN("xiaoliuren", "小六壬", "掌上速断 · 前期快捷占"),
        ZERI("zeri", "择日学", "动土 / 安床 / 开门的吉凶"),
        XIANGSHU("xiangshu", "相术", "面相 · NPC 鉴定与招募"),
        CIZI("cezi", "姓名测字", "给物品、村庄、角色取名的吉凶词条"),
        QIANWEN("qianwen", "签诗 · 灵棋经 · 诸葛神数", "抽签解签 · 隐喻提示"),
        JIEMENG("jiemeng", "周公解梦", "睡后梦境与预兆"),
        KANYU("kanyu", "堪舆四派", "玄空飞星 / 八宅 / 三合 / 峦头"),
        ZHENPAN("zhenpan", "阵盘", "八卦阵 / 九宫阵 / 聚灵阵"),
        YINDE("yinde", "因果功德", "善行、福报与奇遇"),
        TAIYI("taiyi", "太乙神数", "三式之三 · 国运与大势"),
        ;

        private final String key;
        private final String zh;
        private final String note;

        School(String key, String zh, String note) {
            this.key = key;
            this.zh = zh;
            this.note = note;
        }

        public String key() {
            return this.key;
        }

        public String zh() {
            return this.zh;
        }

        public String note() {
            return this.note;
        }

        public static School of(String key) {
            for (School school : values()) {
                if (school.key.equals(key) || school.key.startsWith(key)) {
                    return school;
                }
            }
            return null;
        }
    }

    /**
     * 一次解读。
     *
     * @param blessing 供上层机制使用的系数（-1 凶 ~ +1 吉），不是惩罚值
     */
    public record Verdict(School school, String title, List<String> lines, double blessing) {
    }

    /** 统一入口。 */
    public static Verdict read(School school, long worldSeed, String subject,
                               YishuCalendar.Moment moment) {
        YishuRandom random = StableKey.of(worldSeed, "school:" + school.key())
                .add(subject).add(moment.epochDay()).add(moment.hourIndex()).random();
        return switch (school) {
            case QIZHENG -> qiZheng(random, moment);
            case MEIHUA -> meiHua(random, moment);
            case XIAOLIUREN -> xiaoLiuRen(random);
            case ZERI -> zeRi(random, moment);
            case XIANGSHU -> xiangShu(random, subject);
            case CIZI -> ceZi(random, subject);
            case QIANWEN -> qianWen(random);
            case JIEMENG -> jieMeng(random, moment);
            case KANYU -> kanYu(random, moment);
            case ZHENPAN -> zhenPan(random);
            case YINDE -> yinDe(random, subject);
            case TAIYI -> taiYi(worldSeed, subject, moment);
        };
    }

    /** 列出全部门派（指令帮助用）。 */
    public static List<String> catalogue() {
        List<String> lines = new ArrayList<>();
        for (School school : School.values()) {
            lines.add("§b" + school.key() + "§f —— " + school.zh() + "：" + school.note());
        }
        return lines;
    }

    // ==================================================================== 各门

    private static Verdict qiZheng(YishuRandom r, YishuCalendar.Moment m) {
        String[] stars = {"紫微", "天府", "太阴", "太阳", "贪狼", "巨门", "廉贞", "武曲"};
        String star = stars[r.nextInt(stars.length)];
        String mansion = YishuCalendar.MANSIONS[m.mansion()];
        boolean good = r.nextDouble() < 0.62D;
        return verdict(School.QIZHENG, "七政四余 · 流年星象", List.of(
                "值日星宿 §b" + mansion + "§f，流年照命 §b" + star + "§f。",
                "节气 " + m.solarTermText() + "，四柱 " + m.pillarsText() + "。",
                good ? "§a星光得地，今晚宜上观星台，夜观可得星象加持。"
                     : "§e星芒被云气所掩，宜静待下一宿值日，不必强求。",
                "§7七政（日月金木水火土）行度与四余（罗计气孛）都由种子定，可复现。"),
                good ? 0.6D : -0.2D);
    }

    private static Verdict meiHua(YishuRandom r, YishuCalendar.Moment m) {
        int upper = r.nextInt(0, 7);
        int lower = r.nextInt(0, 7);
        int moving = r.nextInt(1, 6);
        String[] gua = {"乾", "兑", "离", "震", "巽", "坎", "艮", "坤"};
        boolean bodyUse = (upper + lower + moving) % 2 == 0;
        return verdict(School.MEIHUA, "梅花易数 · 即时一卦", List.of(
                "上卦 §b" + gua[upper] + "§f，下卦 §b" + gua[lower] + "§f，动爻 §b" + moving + "§f。",
                "起卦于 " + m.hourText() + "，体用" + (bodyUse ? "相生" : "相克") + "。",
                bodyUse ? "§a事体顺，随手可为；适合拿它决定眼前这一小步。"
                        : "§e体用相克，宜先看一步再动手，不做惩罚。",
                "§7梅花重「当下触机」，同一时辰重复起卦结果不变。"),
                bodyUse ? 0.5D : -0.3D);
    }

    private static Verdict xiaoLiuRen(YishuRandom r) {
        String[] stations = {"大安", "留连", "速喜", "赤口", "小吉", "空亡"};
        String[] advice = {
                "§a大安：事稳，原地守住即可。", "§e留连：事有牵扯，缓一日再看。",
                "§a速喜：好消息将近，宜主动一步。", "§c赤口：口舌是非，少争为宜。",
                "§a小吉：小有所得，见好就收。", "§e空亡：此事暂无着落，换个方向。"
        };
        int index = r.nextInt(stations.length);
        return verdict(School.XIAOLIUREN, "小六壬 · 掌上速断", List.of(
                "落宫 §b" + stations[index] + "§f（月→日→时三跳）。",
                advice[index],
                "§7比大六壬轻：不排四课三传，前期一根手指就能掐。"),
                index == 0 || index == 2 || index == 4 ? 0.4D : -0.25D);
    }

    private static Verdict zeRi(YishuRandom r, YishuCalendar.Moment m) {
        String[] acts = {"动土", "安床", "开门", "播种", "开矿", "远行", "立契"};
        String act = acts[r.nextInt(acts.length)];
        boolean favorable = m.almanac().stream()
                .anyMatch(item -> item.favorable() && item.zh().contains(act.substring(0, 1)));
        return verdict(School.ZERI, "择日 · " + m.dateText(), List.of(
                "今日 " + m.solarTermText() + "，" + m.clash() + "，太岁在 " + m.taiSuiDir() + "。",
                "所问 §b" + act + "§f：" + (favorable ? "§a宜" : "§e不犯忌，但非上选") + "。",
                "§7择日只给彩头，不做惩罚 —— 忌日动土只是少一点加成，不会招灾。",
                "§7建房、结婚、出征、开矿、播种共用同一份黄历。"),
                favorable ? 0.55D : 0.05D);
    }

    private static Verdict xiangShu(YishuRandom r, String subject) {
        String[] marks = {"贵相", "破相", "财帛宫饱满", "官禄宫明朗", "迁移宫有光", "疾厄宫微滞"};
        String mark = marks[r.nextInt(marks.length)];
        boolean noble = mark.equals("贵相") || mark.equals("财帛宫饱满");
        return verdict(School.XIANGSHU, "相术 · " + subject, List.of(
                "骨相定底、面相定气、手相定用；此造得 §b" + mark + "§f。",
                noble ? "§a可募：交易更愿让利，适合长期往来。"
                      : "§e可募，但先别押重注；相处久些再看。",
                "§7相术标签会跟着村民走，是招募与折扣的依据。"),
                noble ? 0.7D : -0.1D);
    }

    private static Verdict ceZi(YishuRandom r, String subject) {
        String[] words = {"亨", "贞", "泰", "谦", "豫", "蹇"};
        String[] notes = {
                "亨通，宜行", "守贞，宜固", "安泰，宜居", "谦下，宜藏",
                "豫顺，宜动", "蹇滞，宜缓"
        };
        int index = r.nextInt(words.length);
        int strokes = Math.max(1, subject.length());
        return verdict(School.CIZI, "测字 · " + subject, List.of(
                "拆形得 §b" + words[index] + "§f，" + notes[index] + "。",
                "字数 " + strokes + "，笔画起卦与姓名五格同源，同一个名字永远同一断语。",
                "§7给物品、村庄、角色取名时会生成一条吉凶词条。"),
                index < 4 ? 0.35D : -0.2D);
    }

    private static Verdict qianWen(YishuRandom r) {
        String[] signs = {"上上", "上吉", "中吉", "中平", "下下"};
        String[] poems = {
                "云开月正明，前程无阻。", "舟行浅水，慢则安稳。",
                "花在枝头，莫急攀折。", "风来无定向，静守其常。",
                "雨夜行舟，且待天明。"
        };
        int index = r.nextInt(signs.length);
        return verdict(School.QIANWEN, "签诗 · " + signs[index] + "签", List.of(
                "§e" + poems[index],
                "签文说的是趋势，不是坐标；解签人会给一条支线提示。",
                "§7灵棋经掷十二棋、诸葛神数拆字报数，与本签共用一套种子。"),
                0.8D - index * 0.4D);
    }

    private static Verdict jieMeng(YishuRandom r, YishuCalendar.Moment m) {
        String[] dreams = {"登高", "涉水", "故人", "失物", "雷雨", "花开"};
        String[] omens = {
                "主进益，宜登高远行。", "主财流动，留意水边。", "主旧缘，宜回访故地。",
                "主暂失，不必强寻。", "主变动，宜先稳住。", "主有成，宜播种经营。"
        };
        int index = r.nextInt(dreams.length);
        return verdict(School.JIEMENG, "周公解梦 · " + dreams[index], List.of(
                "夜梦 §b" + dreams[index] + "§f，" + omens[index],
                "值日 " + m.mansionText() + "，梦应在本宿轮完之前。",
                "§7睡醒触发；心魔事件只给一次轻提示，不掉东西、不扣属性。"),
                index == 0 || index == 5 ? 0.4D : -0.15D);
    }

    private static Verdict kanYu(YishuRandom r, YishuCalendar.Moment m) {
        String[] schools = {"玄空飞星", "八宅", "三合", "峦头理气"};
        String school = schools[r.nextInt(schools.length)];
        int star = r.nextInt(1, 9);
        boolean lucky = star == 8 || star == 9 || star == 1;
        return verdict(School.KANYU, "堪舆 · " + school, List.of(
                "以 §b" + school + "§f 评此宅：当运星 §b" + star + "§f。",
                lucky ? "§a旺星到向，门窗宜开、家具宜顺其势。"
                      : "§e退气星到，调一调朝向即可，不必拆屋。",
                "§7气场网格把家具、门窗、地形、朝向都算进去；搬山改水会重算。",
                "§7" + m.solarTermText() + "之气最宜" + (lucky ? "动土" : "修整") + "。"),
                lucky ? 0.6D : -0.2D);
    }

    private static Verdict zhenPan(YishuRandom r) {
        String[] arrays = {"八卦阵", "九宫阵", "聚灵阵"};
        String array = arrays[r.nextInt(arrays.length)];
        return verdict(School.ZHENPAN, "阵盘 · " + array, List.of(
                "布 §b" + array + "§f，阵眼取脚下，八方位各安一枚镇物。",
                "§7与奇门区分：奇门择「时与方」，阵盘择「地与形」，不共用冷却。",
                "§a阵成后范围内气场微增，作物与炼制略快。"), 0.35D);
    }

    private static Verdict yinDe(YishuRandom r, String subject) {
        int merit = r.nextInt(0, 100);
        String tier = merit > 75 ? "阴德厚" : merit > 40 ? "积善有余" : "尚浅";
        return verdict(School.YINDE, "因果功德 · " + subject, List.of(
                "累计 §b" + merit + "§f · " + tier + "。",
                merit > 75 ? "§a福报将显：奇遇、稀有掉落与雷劫豁免都向这边偏一点。"
                           : "§e再攒些：放生、修路、济村民都算，不惩罚，只慢慢加。",
                "§7善行影响福报与奇遇，不作负面反噬。"),
                (merit - 50) / 50.0D);
    }

    private static Verdict taiYi(long seed, String subject, YishuCalendar.Moment m) {
        TaiYi.Chart chart = TaiYi.cast(seed, m.year(), subject);
        double blessing = chart.superior() > 0 ? 0.5D : chart.superior() < 0 ? -0.2D : 0.1D;
        return new Verdict(School.TAIYI, "太乙神数 · " + chart.summary(),
                TaiYi.lines(chart), blessing);
    }

    private static Verdict verdict(School school, String title, List<String> lines, double blessing) {
        return new Verdict(school, title, lines, blessing);
    }
}
