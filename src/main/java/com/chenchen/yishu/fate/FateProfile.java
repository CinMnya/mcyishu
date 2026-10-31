package com.chenchen.yishu.fate;

import com.chenchen.yishu.core.StableKey;
import com.chenchen.yishu.core.YishuRandom;
import net.minecraft.nbt.CompoundTag;

/**
 * 易枢 · 命盘档案 —— 一个主体的完整命理状态。
 *
 * <p>它把「本命」与「被改过的命」分开存：
 * <ul>
 *   <li>{@link #base()} 是<b>从种子推出来的本命</b>，不可更改，也不保存 ——
 *       每次需要时重新算一遍就行，这符合规范里「基础数据不保存，每次从种子重算」。</li>
 *   <li>{@link #active()} 是<b>当前生效的命</b>。正常情况下它就等于本命；
 *       一旦玩家动用了伪命、改命、轮回之类的手段，它就和本命分道扬镳了。</li>
 * </ul>
 * 只有 {@link #toTag()} 写出来的那几个字段会进存档 —— 也就是说，
 * <b>存档里存的只有「你改过什么」，而不是「你是什么」</b>。
 *
 * <p>四种改命手段的语义（它们不是同义词）：
 * <table border="1">
 *   <tr><th>手段</th><th>含义</th><th>代价</th></tr>
 *   <tr><td>伪命</td><td>伪造一张命盘给别人看</td><td>只对外生效，自身不改；被窥破会损运势</td></tr>
 *   <tr><td>逆命</td><td>违逆本命行事，把忌神当喜神用</td><td>短期爆发，长期积煞</td></tr>
 *   <tr><td>轮回</td><td>重掷命盘（本命被彻底换掉）</td><td>次数累加，每次换来的命更极端</td></tr>
 *   <tr><td>改命</td><td>指定一个新的生辰</td><td>需要大量祭品，且改后不可逆</td></tr>
 * </table>
 */
public final class FateProfile {

    private final String owner;
    private final long worldSeed;
    private final Bazi base;

    /** 当前生效的八字；未改命时与 {@link #base} 相同。 */
    private Bazi active;

    /** 是否处于伪命状态（对外展示的是假命盘）。 */
    private boolean pseudo;
    /** 对外展示的伪命八字。 */
    private Bazi pseudoBazi;
    /** 轮回次数。 */
    private int rebirths;
    /** 是否处于逆命状态。 */
    private boolean defying;

    private FateProfile(String owner, long worldSeed, Bazi base) {
        this.owner = owner;
        this.worldSeed = worldSeed;
        this.base = base;
        this.active = base;
    }

    /**
     * 取一个主体的命盘档案。
     *
     * @param owner 玩家名（<b>不是 UUID</b> —— UUID 会随离线模式与登录方式变化，
     *              不符合「可复现」这条铁律；名字是稳定的）
     */
    public static FateProfile of(long worldSeed, String owner) {
        return new FateProfile(owner, worldSeed, Bazi.forSubject(worldSeed, "player:" + owner));
    }

    // ==================================================================== 查询

    public String owner() {
        return this.owner;
    }

    public long worldSeed() {
        return this.worldSeed;
    }

    /** 本命八字（不可改）。 */
    public Bazi base() {
        return this.base;
    }

    /** 当前生效的八字。 */
    public Bazi active() {
        return this.active;
    }

    /** 对外展示的八字 —— 他人用窥命符看到的就是这一张。 */
    public Bazi presented() {
        return this.pseudo && this.pseudoBazi != null ? this.pseudoBazi : this.active;
    }

    public boolean isPseudo() {
        return this.pseudo;
    }

    public boolean isDefying() {
        return this.defying;
    }

    public int rebirths() {
        return this.rebirths;
    }

    /** 是否动过命 —— 存档里真正需要保存的就是这个判断。 */
    public boolean isModified() {
        return this.active != this.base || this.pseudo || this.rebirths > 0 || this.defying;
    }

    // ==================================================================== 改命

    /**
     * 轮回：重掷一张命盘。
     *
     * <p>轮回次数越多，新命盘越「极端」——实现方式是拿一个带轮回次数盐的键去推八字。
     * 第一次轮回换来的命和第十次不一样，而且第十次会明显更偏。
     */
    public void reincarnate() {
        this.rebirths++;
        YishuRandom random = StableKey.of(this.worldSeed, "reincarnate")
                .add(this.owner).add(this.rebirths).random();
        this.active = Bazi.forSubject(this.worldSeed ^ random.nextLong(), "player:" + this.owner);
        this.pseudo = false;
        this.pseudoBazi = null;
    }

    /**
     * 改命：直接把生辰换掉。
     *
     * @param year  新年份
     * @param month 新月份 1..12
     * @param day   新日期 1..28
     * @param hour  新时辰 0..23
     */
    public void rewrite(int year, int month, int day, int hour) {
        this.active = Bazi.of(year, month, day, hour);
        this.pseudo = false;
        this.pseudoBazi = null;
    }

    /** 施展伪命：对外展示一张假命盘。 */
    public void fakeIdentity() {
        YishuRandom random = StableKey.of(this.worldSeed, "pseudo")
                .add(this.owner).add(this.rebirths).random();
        this.pseudo = true;
        this.pseudoBazi = Bazi.of(
                FateCalendar.MIN_YEAR + random.nextInt(160),
                random.nextInt(1, 12),
                random.nextInt(1, 28),
                random.nextInt(0, 23));
    }

    /** 取消伪命。 */
    public void unmask() {
        this.pseudo = false;
        this.pseudoBazi = null;
    }

    /** 开启或关闭逆命。 */
    public void setDefying(boolean value) {
        this.defying = value;
    }

    /** 把本命恢复回来（只有管理员类的手段才应该调用）。 */
    public void restoreBase() {
        this.active = this.base;
        this.pseudo = false;
        this.pseudoBazi = null;
        this.defying = false;
    }

    /**
     * 当前生效的喜用神。
     *
     * <p>逆命状态下，喜忌<b>整个翻转</b>：原本的忌神被当作喜神用。
     * 这就是逆命「短期爆发、长期积煞」的机制来源 —— 玩家可以主动走进
     * 对自己最不利的地形来换取爆发力。
     */
    public Bazi.YongShen yongShen() {
        return this.active.yongShen();
    }

    /**
     * 环境增益系数。
     *
     * <p>正常状态下用喜用神表；逆命状态下取倒数区间 —— 忌神反而变成加分项。
     */
    public double environmentMultiplier(com.chenchen.yishu.fate.GanZhi.WuXing element) {
        Bazi.YongShen yong = yongShen();
        double normal = yong.multiplier(element);
        if (!this.defying) {
            return normal;
        }
        // 逆命：把增益与减益对调，幅度压缩到 1.0 附近，避免变成纯粹的数值碾压
        return 2.0D - normal;
    }

    // ==================================================================== 存档

    private static final String KEY_ACTIVE = "Active";
    private static final String KEY_PSEUDO = "Pseudo";
    private static final String KEY_PSEUDO_BAZI = "PseudoBazi";
    private static final String KEY_REBIRTHS = "Rebirths";
    private static final String KEY_DEFYING = "Defying";

    /** 只写出「被改动过的部分」；未改动时返回空 tag，存档里几乎不占空间。 */
    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        if (this.active != this.base) {
            tag.putIntArray(KEY_ACTIVE, new int[]{
                    this.active.year().cycleIndex(), this.active.month().cycleIndex(),
                    this.active.day().cycleIndex(), this.active.hour().cycleIndex()});
        }
        tag.putBoolean(KEY_PSEUDO, this.pseudo);
        if (this.pseudoBazi != null) {
            tag.putIntArray(KEY_PSEUDO_BAZI, new int[]{
                    this.pseudoBazi.year().cycleIndex(), this.pseudoBazi.month().cycleIndex(),
                    this.pseudoBazi.day().cycleIndex(), this.pseudoBazi.hour().cycleIndex()});
        }
        tag.putInt(KEY_REBIRTHS, this.rebirths);
        tag.putBoolean(KEY_DEFYING, this.defying);
        return tag;
    }

    /** 从存档恢复。{@code base} 由种子重算，所以这里只需要读回「改动」。 */
    public void readTag(CompoundTag tag) {
        if (tag == null) {
            return;
        }
        int[] active = tag.getIntArray(KEY_ACTIVE).orElse(null);
        if (active != null && active.length == 4) {
            this.active = fromCycleIndices(active);
        }
        this.pseudo = tag.getBooleanOr(KEY_PSEUDO, false);
        int[] pseudoBazi = tag.getIntArray(KEY_PSEUDO_BAZI).orElse(null);
        if (pseudoBazi != null && pseudoBazi.length == 4) {
            this.pseudoBazi = fromCycleIndices(pseudoBazi);
        }
        this.rebirths = tag.getIntOr(KEY_REBIRTHS, 0);
        this.defying = tag.getBooleanOr(KEY_DEFYING, false);
    }

    private static Bazi fromCycleIndices(int[] indices) {
        return new Bazi(
                GanZhi.Pair.of(indices[0]),
                GanZhi.Pair.of(indices[1]),
                GanZhi.Pair.of(indices[2]),
                GanZhi.Pair.of(indices[3]));
    }

    // ==================================================================== 摘要

    /** 一行式摘要。 */
    public String summary() {
        Bazi shown = presented();
        StringBuilder builder = new StringBuilder(shown.summary());
        if (this.rebirths > 0) {
            builder.append(" · 已轮回 ").append(this.rebirths).append(" 次");
        }
        if (this.defying) {
            builder.append(" · 逆命");
        }
        if (this.pseudo) {
            builder.append(" · 伪命");
        }
        return builder.toString();
    }
}
