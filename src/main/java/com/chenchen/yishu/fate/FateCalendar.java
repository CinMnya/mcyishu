package com.chenchen.yishu.fate;

import com.chenchen.yishu.fate.GanZhi.Gan;
import com.chenchen.yishu.fate.GanZhi.Pair;
import com.chenchen.yishu.fate.GanZhi.Zhi;

/**
 * 易枢 · 干支历法 —— 把「一个时间点」换算成「四柱干支」。
 *
 * <p>这是命理系统里唯一需要严谨算法的地方，其余都是查表。四条规则分别是：
 * <ol>
 *   <li><b>年柱</b>：公元 4 年为甲子年，所以 {@code index = (year - 4) mod 60}。</li>
 *   <li><b>月柱</b>：月支固定（立春起寅月、惊蛰起卯月……），月干用「五虎遁」——
 *       <i>甲己之年丙作首，乙庚之岁戊为头，丙辛必定寻庚起，丁壬壬位顺行流，戊癸何方发，甲寅之上好追求。</i>
 *       化简成公式就是 {@code 月干 = (年干 × 2 + 2 + 距寅月数) mod 10}。</li>
 *   <li><b>日柱</b>：以儒略日数（JDN）为轴。2000-01-01 是戊午日（六十甲子第 54 位），
 *       所以 {@code index = (jdn + 49) mod 60}。</li>
 *   <li><b>时柱</b>：时支由钟点决定（子时 23:00–00:59，两小时一个），时干用「五鼠遁」——
 *       <i>甲己还加甲，乙庚丙作初……</i> 化简为 {@code 时干 = (日干 × 2 + 时支) mod 10}。</li>
 * </ol>
 *
 * <p>关于节气：真正的月柱换月点是「节」而非公历月初（例如立春在 2 月 4 日前后）。
 * 本模组<b>刻意采用公历月份的简化划分</b>，理由是：世界种子里的时间本来就是虚构的，
 * 引入真太阳时与紫金山天文台的历表既无必要，也会让「同种子同结果」这条铁律
 * 被一张会随年份修订的历表绑死。这个取舍写在代码里，免得后来者以为是算错了。
 */
public final class FateCalendar {

    private FateCalendar() {
    }

    /** 虚拟历法的纪元：为了确定性，所有推算都在一个纯虚构的日历上进行。 */
    public static final int MIN_YEAR = 1900;

    // ------------------------------------------------------------------ 儒略日

    /**
     * 公历 → 儒略日数（整数 JDN，正午为界）。
     *
     * <p>使用 Fliegel–Van Flandern 公式，对格里高利历全域有效。
     */
    public static long toJulianDay(int year, int month, int day) {
        long a = (14L - month) / 12L;
        long y = year + 4800L - a;
        long m = month + 12L * a - 3L;
        return day + (153L * m + 2L) / 5L + 365L * y + y / 4L - y / 100L + y / 400L - 32045L;
    }

    // ------------------------------------------------------------------ 四柱

    /** 年柱：立春分界的简化版按公历年直接取。 */
    public static Pair yearPillar(int year) {
        return Pair.of(year - 4);
    }

    /**
     * 月柱。
     *
     * @param month 公历月 1..12
     */
    public static Pair monthPillar(int year, int month) {
        int yearGan = yearPillar(year).gan().ordinal();
        // 月支：2 月→寅、3 月→卯 …… 12 月→子、1 月→丑
        int zhiIndex = Math.floorMod(month, 12);
        int offsetFromYin = Math.floorMod(zhiIndex - Zhi.YIN.ordinal(), 12);
        int ganIndex = Math.floorMod(yearGan * 2 + 2 + offsetFromYin, 10);
        return Pair.of(Gan.byIndex(ganIndex), Zhi.byIndex(zhiIndex));
    }

    /** 日柱：以儒略日为轴。 */
    public static Pair dayPillar(int year, int month, int day) {
        return Pair.of((int) ((toJulianDay(year, month, day) + 49L) % 60L));
    }

    /**
     * 时柱。
     *
     * @param hour 24 小时制的钟点；23 点属于次日的子时（本模组按「子时归当日」的通俗算法处理）
     */
    public static Pair hourPillar(Pair dayPillar, int hour) {
        Zhi hourZhi = Zhi.byHour(hour);
        int ganIndex = Math.floorMod(dayPillar.gan().ordinal() * 2 + hourZhi.ordinal(), 10);
        return Pair.of(Gan.byIndex(ganIndex), hourZhi);
    }

    // ------------------------------------------------------------------ 时辰

    /**
     * 十二时辰名（主世界用）。
     *
     * <p>顺序与 {@link Zhi} 一致：子丑寅卯辰巳午未申酉戌亥。
     */
    public static final String[] TWELVE_HOURS = {
            "子时", "丑时", "寅时", "卯时", "辰时", "巳时",
            "午时", "未时", "申时", "酉时", "戌时", "亥时"
    };

    /** 时辰的「时宜」简述，供界面显示。 */
    public static final String[] HOUR_MOOD = {
            "一阳初动，宜静坐养气",
            "夜半沉潜，宜守不宜攻",
            "晓光微起，宜启程远行",
            "日升于东，宜动土兴工",
            "晨雾未散，宜交易谋事",
            "气盛于中，宜举事进取",
            "日中则昃，宜祭祀祈福",
            "日昳未时，宜休整蓄力",
            "晡时金旺，宜决断刑罚",
            "日落酉时，宜收束归藏",
            "黄昏戌时，宜闭门避煞",
            "人定亥时，宜安眠养元"
    };

    /**
     * 给一个公历时间点，取它在游戏里显示用的「时辰」名。
     *
     * <p>注意这里的 {@code hour} 不是现实时间，而是由世界时间映射过来的虚拟钟点 ——
     * 见 {@code dimension.TimeSystem}。
     */
    public static String hourName(int hour) {
        return TWELVE_HOURS[Math.floorMod(Zhi.byHour(hour).ordinal(), 12)];
    }

    public static String hourMood(int hour) {
        return HOUR_MOOD[Math.floorMod(Zhi.byHour(hour).ordinal(), 12)];
    }
}
