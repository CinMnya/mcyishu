package com.chenchen.yishu.calendar;

import com.chenchen.yishu.dimension.DimensionProfile;
import com.chenchen.yishu.fate.Bazi;
import com.chenchen.yishu.fate.FateCalendar;
import com.chenchen.yishu.fate.FateProfile;
import com.chenchen.yishu.fate.GanZhi.Pair;
import com.chenchen.yishu.fate.Yun;
import com.chenchen.yishu.fengshui.FengShuiSystems.Almanac;
import com.chenchen.yishu.fengshui.FengShuiSystems.TaiSui;
import com.chenchen.yishu.system.YishuData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * 易枢 · 时间历法底层（第 18 轮）。
 *
 * <p>此前模组到处都在用「epochDay」当时间，但没有一个统一的「今天到底是
 * 哪年哪月哪日哪个时辰」。这一层把游戏内时钟翻译成一整套黄历：
 * 年号、农历月日、十二时辰、四柱干支、二十四节气、二十八宿、宜忌、
 * 冲煞、太岁方位，全都落在同一个 {@link Moment} 上。
 *
 * <p><b>三条设计约定</b>
 * <ol>
 *   <li><b>小时与主世界严格对应</b> —— 时辰直接由 {@code level.getDayTime() % 24000}
 *       推出来，主世界天亮天黑，黄历上的时辰就跟着走，不做第二套时钟。</li>
 *   <li><b>每个世界单独定纪元</b> —— 世界创建时按种子派生一个起始年号
 *       （默认 2900，范围 2100~2900），之后每 360 游戏日递进一年。
 *       纪元存在 {@link YishuData} 里，所以每个维度各有自己的历法，
 *       也能用 {@code /yishu calendar setyear} 随时改。</li>
 *   <li><b>纯函数优先</b> —— 同一个世界、同一刻，谁问都是同一份黄历。
 *       只有纪元/偏移是持久化的状态，其余全部现算。</li>
 * </ol>
 */
public final class YishuCalendar {

    private YishuCalendar() {
    }

    /** 年号可设定范围。传统黄历以「岁次」纪年，这里给一个足够长的窗口。 */
    public static final int YEAR_MIN = 2100;
    public static final int YEAR_MAX = 2900;
    public static final int YEAR_DEFAULT = 2900;

    public static final long TICKS_PER_DAY = 24000L;
    public static final int DAYS_PER_YEAR = (int) Yun.DAYS_PER_YEAR;   // 360
    public static final int DAYS_PER_MONTH = (int) Yun.DAYS_PER_MONTH; // 30

    private static final String KEY_EPOCH_YEAR = "cal:epoch_year";
    private static final String KEY_DAY_OFFSET = "cal:day_offset";

    /** 二十四节气（从立春起，每 15 日一气）。 */
    public static final String[] SOLAR_TERMS = {
            "立春", "雨水", "惊蛰", "春分", "清明", "谷雨",
            "立夏", "小满", "芒种", "夏至", "小暑", "大暑",
            "立秋", "处暑", "白露", "秋分", "寒露", "霜降",
            "立冬", "小雪", "大雪", "冬至", "小寒", "大寒",
    };

    /** 二十八宿（按日轮值）。 */
    public static final String[] MANSIONS = {
            "角", "亢", "氐", "房", "心", "尾", "箕",
            "斗", "牛", "女", "虚", "危", "室", "壁",
            "奎", "娄", "胃", "昴", "毕", "觜", "参",
            "井", "鬼", "柳", "星", "张", "翼", "轸",
    };

    /** 二十八宿对应的七曜（日金木水火土月，四组循环）。 */
    private static final String[] MANSION_LUMINARIES = {
            "木", "金", "土", "日", "月", "火", "水",
    };

    /** 地支六冲：子午、丑未、寅申、卯酉、辰戌、巳亥。 */
    private static final String[] BRANCHES = {
            "子", "丑", "寅", "卯", "辰", "巳", "午", "未", "申", "酉", "戌", "亥",
    };
    private static final String[] CLASH_SHA = {
            "马", "羊", "猴", "鸡", "狗", "猪", "鼠", "牛", "虎", "兔", "龙", "蛇",
    };

    /** 十二时辰（主世界）。 */
    private static final String[] HOUR_NAMES = FateCalendar.TWELVE_HOURS;

    // ==================================================================== 时刻

    /**
     * 一份完整黄历时刻。
     *
     * @param epochDay    游戏绝对日（{@code dayTime / 24000 + 偏移}）
     * @param year        年号（2100~2900）
     * @param month       农历月（1~12）
     * @param day         农历日（1~30）
     * @param hour        24 小时制钟点（0~23），与主世界一致
     * @param hourIndex   十二时辰序号（0=子时）
     * @param solarTerm   节气序号（0=立春）
     * @param mansion     值日星宿序号（0=角）
     * @param pillars     年月日时四柱
     * @param almanac     今日宜忌
     * @param taiSuiDir   太岁方位
     * @param clash       冲煞文字
     */
    public record Moment(long epochDay, int year, int month, int day,
                         int hour, int hourIndex, int solarTerm, int mansion,
                         List<Pair> pillars, List<Almanac.Item> almanac,
                         String taiSuiDir, String clash) {

        public String dateText() {
            return this.year + " 年 " + this.month + " 月 " + this.day + " 日";
        }

        public String hourText() {
            return HOUR_NAMES[this.hourIndex] + "（" + this.hour + ":00）";
        }

        public String solarTermText() {
            return SOLAR_TERMS[this.solarTerm];
        }

        public String mansionText() {
            return MANSIONS[this.mansion] + "宿 · " + MANSION_LUMINARIES[this.mansion % 7] + "曜";
        }

        public String pillarsText() {
            return this.pillars.get(0).zh() + " " + this.pillars.get(1).zh() + " "
                    + this.pillars.get(2).zh() + " " + this.pillars.get(3).zh();
        }

        /** 当前节气是否属于「四立」—— 季节交替，事件权重最高。 */
        public boolean isSeasonStart() {
            return this.solarTerm % 6 == 0;
        }

        public boolean isSolsticeOrEquinox() {
            return this.solarTerm == 3 || this.solarTerm == 9
                    || this.solarTerm == 15 || this.solarTerm == 21;
        }
    }

    // ==================================================================== 世界纪元

    /**
     * 本世界（本维度）的起始年号。首次访问时按世界种子派生并写入存档，
     * 所以「每个世界的时间在创建世界的时候被定义」，之后不会跳变。
     */
    public static int epochYear(ServerLevel level) {
        YishuData data = YishuData.of(level);
        String raw = data.get(KEY_EPOCH_YEAR);
        if (raw != null) {
            return clampYear(parseInt(raw, YEAR_DEFAULT));
        }
        // 派生：落在 2100~2900 之间，默认锚在 2900
        int span = YEAR_MAX - YEAR_MIN + 1;
        int derived = (int) Math.floorMod(level.getSeed() ^ 0x5A17C0DEL, span) + YEAR_MIN;
        int year = level.dimension().identifier().getPath().equals("overworld")
                ? YEAR_DEFAULT : derived;
        data.put(KEY_EPOCH_YEAR, Integer.toString(year));
        return year;
    }

    /** 指令改纪元。 */
    public static void setEpochYear(ServerLevel level, int year) {
        YishuData.of(level).put(KEY_EPOCH_YEAR, Integer.toString(clampYear(year)));
    }

    /** 额外日偏移：指令可以把历法整体往前/往后拨。 */
    public static long dayOffset(ServerLevel level) {
        String raw = YishuData.of(level).get(KEY_DAY_OFFSET);
        if (raw == null) {
            return 0L;
        }
        try {
            return Long.parseLong(raw);
        } catch (NumberFormatException ignored) {
            return 0L;
        }
    }

    public static void setDayOffset(ServerLevel level, long offset) {
        YishuData.of(level).put(KEY_DAY_OFFSET, Long.toString(offset));
    }

    private static int clampYear(int year) {
        return Math.max(YEAR_MIN, Math.min(YEAR_MAX, year));
    }

    private static int parseInt(String raw, int fallback) {
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    // ==================================================================== 换算

    /** 当前时刻。 */
    public static Moment now(ServerLevel level) {
        // 26.2 把时间收进了 WorldClock：getOverworldClockTime() 是主世界那条
        // 时钟的累计 tick。不管玩家站在哪个界域，读的都是它 ——
        // 于是「小时和主世界对应」这条要求天然成立，不需要额外同步。
        long clockTicks = level.getOverworldClockTime();
        long epochDay = Math.floorDiv(clockTicks, TICKS_PER_DAY) + dayOffset(level);
        int hour = (int) Math.floorMod(clockTicks, TICKS_PER_DAY) / 1000; // 0~23
        return of(level, epochDay, hour);
    }

    /** 由绝对日与钟点构造（供测试、节气事件、指令回看使用）。 */
    public static Moment of(ServerLevel level, long epochDay, int hour) {
        DimensionProfile dimension = DimensionProfile.of(level.dimension().identifier().getPath());
        long worldSeed = level.getSeed();

        // 年号：从纪元起逐年递进，到 2900 封顶（2100~2900 的窗口）
        long elapsedYears = Math.max(0L, epochDay / DAYS_PER_YEAR);
        int year = clampYear(epochYear(level) + (int) Math.min(elapsedYears, YEAR_MAX - YEAR_MIN));

        long dayInYear = Math.floorMod(epochDay, (long) DAYS_PER_YEAR);
        int month = (int) (dayInYear / DAYS_PER_MONTH) + 1;
        int day = (int) (dayInYear % DAYS_PER_MONTH) + 1;

        int clampedHour = Math.max(0, Math.min(23, hour));
        int hourIndex = clampedHour / 2; // 子时 23~0 在本模组按「归当日」处理

        int solarTerm = (int) (dayInYear / 15) % SOLAR_TERMS.length;
        int mansion = (int) Math.floorMod(epochDay, MANSIONS.length);

        Pair yearPillar = FateCalendar.yearPillar(year);
        Pair monthPillar = FateCalendar.monthPillar(year, month);
        Pair dayPillar = FateCalendar.dayPillar(year, month, day);
        Pair hourPillar = FateCalendar.hourPillar(dayPillar, clampedHour);
        List<Pair> pillars = List.of(yearPillar, monthPillar, dayPillar, hourPillar);

        List<Almanac.Item> almanac = Almanac.ofDay(worldSeed, dimension, epochDay);
        String taiSuiDir = TaiSui.directionName(year);

        int branch = dayPillar.zhi().ordinal() % 12;
        String clash = "冲" + BRANCHES[(branch + 6) % 12] + " · 煞" + CLASH_SHA[branch];

        return new Moment(epochDay, year, month, day, clampedHour, hourIndex,
                solarTerm, mansion, pillars, almanac, taiSuiDir, clash);
    }

    // ==================================================================== 面板文本

    /** 黄历通用面板（时钟 UI 与 {@code /yishu calendar} 共用）。 */
    public static List<String> lines(Moment moment, DimensionProfile dimension) {
        List<String> lines = new ArrayList<>();
        lines.add("§e" + moment.dateText() + "§f · " + moment.hourText());
        lines.add("今日节气 §b" + moment.solarTermText() + "§f · 值日 §b" + moment.mansionText());
        lines.add("四柱 §b" + moment.pillarsText());
        lines.add("太岁在 §b" + moment.taiSuiDir + "§f · " + moment.clash);
        lines.add("");
        lines.add("§6—— 今日宜忌 ——");
        for (Almanac.Item item : moment.almanac()) {
            lines.add((item.favorable() ? "§a" : "§c") + item.describe());
        }
        lines.add("");
        lines.add("§7" + dimension.timeSystemName() + "：" + dimension.hourSystemNote());
        if (moment.isSeasonStart()) {
            lines.add("§d四立之交，气机动荡，野外奇遇与异象更频繁。");
        } else if (moment.isSolsticeOrEquinox()) {
            lines.add("§d二分二至，阴阳易位，宜观星、起盘、祭典。");
        }
        return lines;
    }

    /** 玩家专属运势页：大运、流年，以及今日与自己喜用神的关系。 */
    public static List<String> fortuneLines(ServerLevel level, Player player, Moment moment) {
        List<String> lines = new ArrayList<>();
        String owner = player.getGameProfile().name();
        FateProfile profile = FateProfile.of(level.getSeed(), owner);
        Bazi bazi = profile.active();
        String dim = DimensionProfile.of(level.dimension().identifier().getPath()).shortKey();

        List<Yun.DaYun> steps = Yun.daYun(bazi, level.getSeed(), dim + ":self");
        int age = Yun.age(moment.epochDay());
        Yun.DaYun daYun = Yun.current(steps, age);
        Pair liuNian = Yun.liuNian(level.getSeed(), dim, moment.epochDay());

        Bazi.YongShen yong = profile.yongShen();
        boolean yearFavorable = yong.isFavorable(liuNian.gan().wuXing());

        lines.add("§6—— 我的运势 ——");
        lines.add("本命 §b" + bazi.toString());
        lines.add("喜用 §b" + yong.primary().zh() + "§f · " + yong.reason());
        lines.add("大运 §b" + (daYun == null ? "未起运" : daYun.ganZhi().zh() + "（" + daYun.range() + "）"));
        lines.add("流年 §b" + liuNian.zh() + "§f · 虚岁 " + age);
        lines.add(yearFavorable
                ? "§a流年与你相合，今日行事顺遂，宜动土、远行、立契。"
                : "§e流年与你相冲，稳守为宜；不做惩罚，只少一点彩头。");
        lines.add("§7（同种子同刻，结果完全一致；换界域则各自起运。）");
        return lines;
    }

    /** 时钟/黄历面板的完整内容。 */
    public static List<String> clockPanel(ServerLevel level, Player player) {
        Moment moment = now(level);
        DimensionProfile dimension = DimensionProfile.of(level.dimension().identifier().getPath());
        List<String> lines = new ArrayList<>(lines(moment, dimension));
        lines.add("");
        lines.addAll(fortuneLines(level, player, moment));
        return lines;
    }
}
