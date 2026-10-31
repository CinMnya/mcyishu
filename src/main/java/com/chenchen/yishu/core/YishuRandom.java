package com.chenchen.yishu.core;

import java.util.List;

/**
 * 易枢 · 确定性随机流（SplitMix64）。
 *
 * <p>注意它<b>不是</b>一个「随机数生成器」——它是一条由某个 {@link StableKey} 完全决定的
 * 数字序列。同样的键，永远得到同样的序列，无论在谁的电脑上、什么时候算的。
 * 之所以还要做成流的形式，是因为很多算法（洗牌、加权抽取、泊松采样）天然需要
 * 「连续取几个数」，写成流更好读。
 *
 * <p>实现选 SplitMix64 而不是 {@code java.util.Random}：
 * <ul>
 *   <li>状态就一个 {@code long}，没有隐藏的 seed 补偿逻辑；</li>
 *   <li>输出质量好（通过了 BigCrush），且 Java 版本之间不会变；</li>
 *   <li>{@code java.util.Random} 已经被 {@code RandomGenerator} 接口化，
 *       未来若默认实现改变，跨版本可复现性就有风险。</li>
 * </ul>
 */
public final class YishuRandom {

    private long state;

    public YishuRandom(long seed) {
        this.state = seed;
    }

    /** 从字符串派生（会先经过稳定哈希，保持跨平台一致）。 */
    public static YishuRandom of(String salt) {
        return new YishuRandom(StableKey.of(salt).hash());
    }

    // ------------------------------------------------------------------ 基本

    public long nextLong() {
        this.state += 0x9E3779B97F4A7C15L;
        long z = this.state;
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }

    /** @return {@code [0, bound)}；{@code bound <= 0} 返回 0。 */
    public int nextInt(int bound) {
        if (bound <= 0) {
            return 0;
        }
        // 用无符号模运算，拒绝采样会引入分支不确定性，这里直接取模（bound 都很小，偏差可忽略）
        return (int) Long.remainderUnsigned(nextLong(), bound);
    }

    /** @return {@code [min, max]} 闭区间。 */
    public int nextInt(int min, int max) {
        if (max <= min) {
            return min;
        }
        return min + nextInt(max - min + 1);
    }

    public double nextDouble() {
        return (nextLong() >>> 11) * 0x1.0p-53;
    }

    public float nextFloat() {
        return (float) ((nextLong() >>> 40) * 0x1.0p-24);
    }

    public boolean nextBoolean() {
        return (nextLong() & 1L) != 0L;
    }

    public boolean chance(double probability) {
        if (probability <= 0.0D) {
            return false;
        }
        if (probability >= 1.0D) {
            return true;
        }
        return nextDouble() < probability;
    }

    /** 近似正态（12 个均匀分布求和），返回 {@code [mean - 3*sigma, mean + 3*sigma]} 附近的值。 */
    public double nextGaussian(double mean, double sigma) {
        double sum = 0.0D;
        for (int i = 0; i < 12; i++) {
            sum += nextDouble();
        }
        return mean + (sum - 6.0D) * sigma;
    }

    // ------------------------------------------------------------------ 抽取

    public <T> T pick(T[] values) {
        if (values == null || values.length == 0) {
            return null;
        }
        return values[nextInt(values.length)];
    }

    public <T> T pick(List<T> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        return values.get(nextInt(values.size()));
    }

    /** 按权重抽取。{@code weights} 需与 {@code values} 等长且非负。 */
    public <T> T pickWeighted(List<T> values, double[] weights) {
        if (values == null || values.isEmpty() || weights == null || weights.length != values.size()) {
            return null;
        }
        double total = 0.0D;
        for (double weight : weights) {
            total += Math.max(0.0D, weight);
        }
        if (total <= 0.0D) {
            return pick(values);
        }
        double roll = nextDouble() * total;
        for (int i = 0; i < values.size(); i++) {
            roll -= Math.max(0.0D, weights[i]);
            if (roll <= 0.0D) {
                return values.get(i);
            }
        }
        return values.get(values.size() - 1);
    }

    /** Fisher–Yates 洗牌（原地）。 */
    public <T> void shuffle(List<T> list) {
        for (int i = list.size() - 1; i > 0; i--) {
            int j = nextInt(i + 1);
            T tmp = list.get(i);
            list.set(i, list.get(j));
            list.set(j, tmp);
        }
    }

    /** 从 {@code [0, bound)} 里取 {@code count} 个不重复下标（升序返回）。 */
    public int[] sample(int bound, int count) {
        if (bound <= 0 || count <= 0) {
            return new int[0];
        }
        int n = Math.min(bound, count);
        boolean[] taken = new boolean[bound];
        int[] result = new int[n];
        int filled = 0;
        while (filled < n) {
            int candidate = nextInt(bound);
            if (!taken[candidate]) {
                taken[candidate] = true;
                result[filled++] = candidate;
            }
        }
        java.util.Arrays.sort(result);
        return result;
    }
}
