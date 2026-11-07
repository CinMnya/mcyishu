package com.chenchen.yishu.taiyi;

import com.chenchen.yishu.calendar.YishuCalendar;
import com.chenchen.yishu.core.StableKey;
import com.chenchen.yishu.core.YishuRandom;

import java.util.ArrayList;
import java.util.List;

/**
 * 易枢 · 太乙神数（第 18 轮）。
 *
 * <p>太乙与奇门、大六壬并称「三式」，前两式已在模组里，这一门补上，
 * 三式才算齐。太乙看的是<b>国运与大势</b>：以积年定阴阳遁，
 * 太乙行九宫而不入中宫，文昌、始击分主客，主算客算定胜负，
 * 再看掩、迫、击、囚四格局。
 *
 * <p>与另外两式一样，这是<b>纯函数</b>：输入世界种子、年号、问事名号，
 * 输出一张盘；同一个种子同一年，谁起盘都是同一张。
 * 不做随机数摇签，也不给惩罚性效果。
 */
public final class TaiYi {

    private TaiYi() {
    }

    /** 太乙行宫所经的八宫（中五宫不入，这是太乙区别于奇门的关键）。 */
    private static final String[] PALACES = {
            "乾一", "离二", "艮三", "震四", "巽六", "兑七", "坤八", "坎九",
    };

    /** 十六神：八方各一正神，间以八间神。 */
    private static final String[] SIXTEEN_GODS = {
            "文昌", "始击", "主大将", "客大将", "主参将", "客参将",
            "计神", "太阴", "合神", "天目", "地目", "天乙",
            "直符", "定目", "天时", "四维",
    };

    /** 四类格局。 */
    private enum Pattern {
        MASK("掩", "主被客掩，宜守不宜进；先稳住根基再谈进取。"),
        PRESS("迫", "太乙受迫，事有外力相催；缓一步反而看清。"),
        STRIKE("击", "主客相击，动静皆有利弊；不宜孤注一掷。"),
        CAGE("囚", "太乙入囚，气机暂塞；宜蓄势、修整、待时。"),
        OPEN("和", "太乙得位，主客相安；大势顺，可放心推进。");

        private final String zh;
        private final String note;

        Pattern(String zh, String note) {
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

    /** 一张太乙盘。 */
    public record Chart(int year, boolean yangDun, int taiyiPalace,
                        String wenchang, String shiji,
                        int hostCount, int guestCount,
                        Pattern pattern, int superior) {

        public String summary() {
            return (this.yangDun ? "阳遁" : "阴遁") + " · 太乙入" + PALACES[this.taiyiPalace]
                    + " · " + this.pattern.zh + "格";
        }
    }

    /**
     * 起盘。
     *
     * @param worldSeed 世界种子
     * @param year      年号（黄历纪元）
     * @param subject   问事名号（玩家名 / 建筑名）
     */
    public static Chart cast(long worldSeed, int year, String subject) {
        // 积年：太乙以 72 年为一元、24 年为一纪；这里折成可复现的局数
        long accumulated = Math.floorMod(year - YishuCalendar.YEAR_MIN, 72L);
        boolean yangDun = (accumulated / 24L) % 2L == 0L;

        YishuRandom random = StableKey.of(worldSeed, "taiyi")
                .add(year).add(subject).random();

        // 太乙三年一移宫，八宫轮转（不入中五）
        int palace = (int) ((accumulated / 3L + random.nextInt(0, 7)) % PALACES.length);

        String wenchang = SIXTEEN_GODS[(palace + 2) % SIXTEEN_GODS.length];
        String shiji = SIXTEEN_GODS[(palace + 9) % SIXTEEN_GODS.length];

        // 主算 / 客算：太乙以数定胜负，主算取宫数，客算取十六神序
        int hostCount = 1 + palace + (yangDun ? 3 : 0);
        int guestCount = 1 + ((palace + 9) % SIXTEEN_GODS.length);
        int superior = Integer.compare(hostCount, guestCount);

        Pattern pattern;
        if (hostCount < 3 && guestCount > 8) {
            pattern = Pattern.MASK;
        } else if (guestCount > hostCount + 6) {
            pattern = Pattern.PRESS;
        } else if (Math.abs(hostCount - guestCount) <= 1) {
            pattern = Pattern.STRIKE;
        } else if (hostCount > 10 && guestCount > 10) {
            pattern = Pattern.CAGE;
        } else {
            pattern = Pattern.OPEN;
        }
        return new Chart(year, yangDun, palace, wenchang, shiji,
                hostCount, guestCount, pattern, superior);
    }

    /** 盘面正文（给面板 / 指令用）。 */
    public static List<String> lines(Chart chart) {
        List<String> lines = new ArrayList<>();
        lines.add("§e" + chart.year() + " 年 · " + chart.summary());
        lines.add("积年定局：" + (chart.yangDun() ? "阳遁顺行" : "阴遁逆行")
                + "；太乙行九宫而不入中五。");
        lines.add("太乙居 §b" + PALACES[chart.taiyiPalace()] + "§f；"
                + "文昌在 §b" + chart.wenchang() + "§f，始击在 §b" + chart.shiji() + "§f。");
        lines.add("主算 §b" + chart.hostCount() + "§f ｜ 客算 §b" + chart.guestCount() + "§f ｜ "
                + (chart.superior() > 0 ? "主胜" : chart.superior() < 0 ? "客胜" : "主客均势"));
        lines.add("");
        lines.add("§6" + chart.pattern.zh() + "格§f：" + chart.pattern.note);
        lines.add("§7太乙问的是大势，不点具体坐标；真要找方向，请另起奇门盘。");
        return lines;
    }
}
