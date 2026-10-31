package com.chenchen.yishu.fate;

import com.chenchen.yishu.core.StableKey;
import com.chenchen.yishu.core.YishuRandom;
import com.chenchen.yishu.fate.GanZhi.Pair;

import java.util.ArrayList;
import java.util.List;

/**
 * 易枢 · 大运与流运 —— 命盘的「时间轴」。
 *
 * <p>八字是静态的（出生那一刻就定了），但它要和不断流动的时间发生关系，
 * 才有「今年顺不顺」这回事。这层关系分四档：
 * <table border="1">
 *   <tr><th>层级</th><th>周期</th><th>本模组的映射</th></tr>
 *   <tr><td>大运</td><td>十年</td><td>由命主八字推定起运岁，之后每十年换一柱</td></tr>
 *   <tr><td>流年</td><td>一年</td><td>每 360 个游戏日</td></tr>
 *   <tr><td>流月</td><td>一月</td><td>每 30 个游戏日</td></tr>
 *   <tr><td>流日</td><td>一日</td><td>每 1 个游戏日</td></tr>
 *   <tr><td>流时</td><td>一时辰</td><td>由维度的时间体系决定（主世界 12 时辰 / 下界 12 火时 / 末地 12 虚空脉）</td></tr>
 * </table>
 *
 * <p><b>关键设计</b>：大运流年流月流日的<b>偏移基准按维度重算</b>，而本命八字<b>跨维度不变</b>。
 * 也就是说，一个人还是那个人，但换个世界，时节就变了 —— 这是规范里
 * 「玩家跨维度时本命八字不变，流运按维度重算」的直接实现。
 */
public final class Yun {

    /** 一个游戏年 = 360 个游戏日。 */
    public static final long DAYS_PER_YEAR = 360L;
    /** 一个游戏月 = 30 个游戏日。 */
    public static final long DAYS_PER_MONTH = 30L;

    private Yun() {
    }

    // ==================================================================== 大运

    /**
     * 一步大运。
     *
     * @param index    第几步（1 起）
     * @param startAge 进入这一步时的年龄
     * @param ganZhi   这一步的干支
     */
    public record DaYun(int index, int startAge, int endAge, Pair ganZhi) {

        public String range() {
            return this.startAge + "–" + this.endAge + " 岁";
        }
    }

    /**
     * 排大运。
     *
     * <p>规则：
     * <ol>
     *   <li><b>顺逆</b>：阳年干生人顺行、阴年干生人逆行。
     *       （真实的排法还要看性别，但 Minecraft 里没有性别字段，
     *       所以本模组只用年干阴阳定顺逆，规则单一且可复现。）</li>
     *   <li><b>起运</b>：从命主八字里确定性地取一个 1~9 岁的起运岁。
     *       真实算法要看「出生日到下一个节气的天数 ÷ 3」，那需要一张逐年修订的历表，
     *       与「同种子同结果」这条铁律冲突，故改用稳定哈希。</li>
     *   <li>每一步从月柱开始顺/逆推一柱，共排 {@value #STEPS} 步，覆盖一百年。</li>
     * </ol>
     */
    public static List<DaYun> daYun(Bazi bazi, long worldSeed, String subject) {
        boolean forward = bazi.year().gan().yinYang() == GanZhi.YinYang.YANG;
        int startAge = 1 + StableKey.of(worldSeed, "dayun").add(subject).index(9);
        int monthIndex = bazi.month().cycleIndex();

        List<DaYun> result = new ArrayList<>(STEPS);
        for (int step = 0; step < STEPS; step++) {
            int offset = forward ? (step + 1) : -(step + 1);
            Pair ganZhi = Pair.of(monthIndex + offset);
            int from = startAge + step * 10;
            result.add(new DaYun(step + 1, from, from + 9, ganZhi));
        }
        return result;
    }

    public static final int STEPS = 10;

    /** 取当前年龄所落的那一步大运；未起运时返回 {@code null}。 */
    public static DaYun current(List<DaYun> steps, int age) {
        for (DaYun step : steps) {
            if (age >= step.startAge() && age <= step.endAge()) {
                return step;
            }
        }
        return age < steps.get(0).startAge() ? null : steps.get(steps.size() - 1);
    }

    // ==================================================================== 流运

    /**
     * 某个维度下的流运偏移基准。
     *
     * <p>这就是「流运按维度重算」的落点：主世界、下界、末地各有一套独立的偏移，
     * 于是同一个游戏日，三个维度算出来的流年可能是不同的干支。
     */
    private static long[] offsets(long worldSeed, String dimension) {
        YishuRandom random = StableKey.of(worldSeed, "liuyun").add(dimension).random();
        return new long[]{random.nextInt(60), random.nextInt(60), random.nextInt(60), random.nextInt(60)};
    }

    /** 流年（每 360 游戏日一换）。 */
    public static Pair liuNian(long worldSeed, String dimension, long epochDay) {
        long[] offset = offsets(worldSeed, dimension);
        return Pair.of((int) ((epochDay / DAYS_PER_YEAR) + offset[0]));
    }

    /** 流月（每 30 游戏日一换）。 */
    public static Pair liuYue(long worldSeed, String dimension, long epochDay) {
        long[] offset = offsets(worldSeed, dimension);
        return Pair.of((int) ((epochDay / DAYS_PER_MONTH) + offset[1]));
    }

    /** 流日（每游戏日一换）。 */
    public static Pair liuRi(long worldSeed, String dimension, long epochDay) {
        long[] offset = offsets(worldSeed, dimension);
        return Pair.of((int) (epochDay + offset[2]));
    }

    /**
     * 流时（每时辰一换）。
     *
     * <p>时柱由「流日的日干」+「当前时辰地支」按五鼠遁推出，与四柱里的时柱算法一致。
     */
    public static Pair liuShi(long worldSeed, String dimension, long epochDay, int hourIndex) {
        Pair day = liuRi(worldSeed, dimension, epochDay);
        int ganIndex = Math.floorMod(day.gan().ordinal() * 2 + Math.floorMod(hourIndex, 12), 10);
        return Pair.of(ganIndex);
    }

    /** 玩家在这个世界里的「岁数」（以游戏年计）。 */
    public static int age(long epochDay) {
        return (int) (epochDay / DAYS_PER_YEAR);
    }

    // ==================================================================== 综合

    /** 把某一刻的全部流运打包，界面上一次看完。 */
    public record Moment(Pair daYun, Pair liuNian, Pair liuYue, Pair liuRi, Pair liuShi,
                         int age, String dimension, String hourName) {
    }

    public static Moment moment(Bazi bazi, long worldSeed, String dimension, long epochDay,
                                int hourIndex, String hourName) {
        int age = age(epochDay);
        DaYun currentDaYun = current(daYun(bazi, worldSeed, dimension + ":self"), age);
        return new Moment(
                currentDaYun == null ? bazi.month() : currentDaYun.ganZhi(),
                liuNian(worldSeed, dimension, epochDay),
                liuYue(worldSeed, dimension, epochDay),
                liuRi(worldSeed, dimension, epochDay),
                liuShi(worldSeed, dimension, epochDay, hourIndex),
                age,
                dimension,
                hourName
        );
    }
}
