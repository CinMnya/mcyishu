package com.chenchen.yishu.core;

/**
 * 易枢 · 确定性内核 —— 稳定键构造器。
 *
 * <p><b>这是整个模组的地基。</b>模组的最高原则是：
 * <i>同一个世界种子下，所有结果完全一致、可复现、可分享。</i>
 * 要做到这一点，就不能让任何一次随机数依赖「什么时候算的」「谁算的」
 * 「服务器现在跑到第几刻」。
 *
 * <p>所以本模组<b>不存在真正的随机数生成器</b>。所有「随机」都长这样：
 * <pre>
 *   long key  = StableKey.of("bazi").add(dimension).add(blockX).add(blockZ).add(index).hash();
 *   Bazi bazi = Bazi.from(key);
 * </pre>
 * 也就是说，<b>任何一次结果都是它「是什么」的函数</b>，而不是它「第几次被问」的函数。
 * 玩家 A 在第 3 天问的卦，和玩家 B 在第 900 天走到同一个位置问的卦，是同一卦。
 *
 * <p>键的形状遵循规范里定下的六段式：
 * <pre>
 *   类型 + 维度 + 主体 + 限定 + 子项
 * </pre>
 * 例如「下界 · 堡垒遗迹 · X-1200 Z340 · 战利品 · 第 3 格」写成
 * {@code of("building").add("the_nether").add("fortress").add(-1200).add(340).add("loot").add(3)}。
 *
 * <p><b>明确禁止</b>参与哈希的东西：{@code UUID}、{@code System.currentTimeMillis()}、
 * {@code level.getLevelData().getGameTime()}、玩家行为计数、{@code Random} 实例、集合的迭代顺序。
 */
public final class StableKey {

    /** MurmurHash3 / xxHash 系列常用的 64 位终混合常量。 */
    private static final long C1 = 0x9E3779B97F4A7C15L;
    private static final long C2 = 0xBF58476D1CE4E5B9L;
    private static final long C3 = 0x94D049BB133111EBL;

    private long state;

    private StableKey(long seed) {
        this.state = seed;
    }

    // ------------------------------------------------------------------ 入口

    /** 起一个键，{@code type} 是内容族名，例如 {@code "bazi"} / {@code "fengshui"} / {@code "mob"}。 */
    public static StableKey of(String type) {
        return new StableKey(0x51ED270B5AC1CA7BL).add(type);
    }

    /** 从世界种子起的键。所有内容族最终都要经过这里。 */
    public static StableKey of(long worldSeed, String type) {
        return new StableKey(worldSeed).add(type);
    }

    // ------------------------------------------------------------------ 追加

    public StableKey add(String value) {
        if (value == null) {
            return add(0L);
        }
        // 逐字符混合，避免 hashCode 的碰撞（String.hashCode 在短串上冲突率很高）
        for (int i = 0; i < value.length(); i++) {
            this.state = mix(this.state ^ (value.charAt(i) * C1));
        }
        return add((long) value.length());
    }

    public StableKey add(int value) {
        return add((long) value);
    }

    public StableKey add(long value) {
        this.state = mix(this.state ^ value);
        return this;
    }

    public StableKey add(boolean value) {
        return add(value ? 1L : 0L);
    }

    /** 追加一个有序数列（顺序参与哈希，不允许用 Set 之类的无序容器）。 */
    public StableKey addAll(Iterable<?> values) {
        int index = 0;
        for (Object value : values) {
            add(index++);
            add(String.valueOf(value));
        }
        return add(index);
    }

    // ------------------------------------------------------------------ 出值

    /** 最终混合。对同一个键重复调用返回同一个值（幂等，不改内部状态）。 */
    public long hash() {
        long h = this.state;
        h ^= h >>> 30;
        h *= C2;
        h ^= h >>> 27;
        h *= C3;
        h ^= h >>> 31;
        return h;
    }

    /** 32 位截断，用于需要 int 的场合（如 {@code Random} 的种子、颜色、索引）。 */
    public int hash32() {
        return (int) (hash() ^ (hash() >>> 32));
    }

    /** 以本键为种子开一条确定性随机流。 */
    public YishuRandom random() {
        return new YishuRandom(hash());
    }

    /** 取 {@code [min, max]} 闭区间内的一个确定值。 */
    public int range(int min, int max) {
        return min + Math.floorMod(hash32(), max - min + 1);
    }

    /** 取 {@code [0, bound)} 内的确定下标；{@code bound <= 0} 时返回 0。 */
    public int index(int bound) {
        return bound <= 0 ? 0 : Math.floorMod(hash32(), bound);
    }

    /** 按确定性哈希掷一个概率（{@code chance} 为 0~1）。 */
    public boolean chance(double probability) {
        if (probability <= 0.0D) {
            return false;
        }
        if (probability >= 1.0D) {
            return true;
        }
        // 用无符号 32 位归一化，避免用 double 精度导致边界抖动
        double roll = (hash32() & 0xFFFFFFFFL) / (double) 0xFFFFFFFFL;
        return roll < probability;
    }

    // ------------------------------------------------------------------ 内部

    private static long mix(long value) {
        value ^= value >>> 33;
        value *= 0xFF51AFD7ED558CCDL;
        value ^= value >>> 33;
        value *= 0xC4CEB9FE1A85EC53L;
        value ^= value >>> 33;
        return value;
    }

    @Override
    public String toString() {
        return "StableKey[" + Long.toUnsignedString(hash(), 16) + "]";
    }
}
