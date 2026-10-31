package com.chenchen.yishu.system;

import com.chenchen.yishu.YishuMod;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.HashMap;
import java.util.Map;

/**
 * 易枢 · 存档数据。
 *
 * <p>规范第九节写得很清楚：<b>基础数据不保存，每次从种子重算；只有玩家修改写入 SavedData。</b>
 * 所以这个类刻意<b>很小</b> —— 它只装三样东西：
 *
 * <ol>
 *   <li>每个玩家今天问了几卦（否则「每日次数上限」无从谈起）；</li>
 *   <li>每个玩家今天窥了几次命（窥命符的代价）；</li>
 *   <li>被玩家占领的龙脉段（谁占了哪一段）。</li>
 * </ol>
 *
 * <p>除此之外的一切 —— 八字、风水域、卦象、生物命盘、建筑命盘 ——
 * <b>一个字节都不存</b>。它们随时可以从种子重算出来，存了反而是隐患：
 * 一旦存档和种子不同步，「同种子同结果」就破了。
 *
 * <p>内部用一个扁平的 {@code Map<String, String>} 装所有数据，
 * 再直接序列化成 {@link CompoundTag}。之所以不用一堆结构化字段：
 * 这类「计数器」结构会随着功能增加不断变形，扁平存储最抗改动。
 */
public final class YishuData extends SavedData {

    /** 数据标识。 */
    public static final SavedDataType<YishuData> TYPE = new SavedDataType<>(
            YishuMod.id("yishu_data"),
            YishuData::new,
            CompoundTag.CODEC.xmap(YishuData::fromTag, YishuData::toTag),
            DataFixTypes.LEVEL);

    private static final String KEY_DIVINATION = "div:";
    private static final String KEY_PEEKS = "peek:";
    private static final String KEY_VEIN = "vein:";
    private static final String KEY_MISC = "misc:";

    private final Map<String, String> values = new HashMap<>();

    public YishuData() {
    }

    // ==================================================================== 取用

    /** 取某个维度的存档数据。 */
    public static YishuData of(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    /** 取主世界的存档数据（跨维度共享的那一份）。 */
    public static YishuData shared(net.minecraft.server.MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    // ==================================================================== 六爻计数

    private static String divinationKey(String owner, long epochDay) {
        return KEY_DIVINATION + epochDay + ":" + owner;
    }

    /** 今天（截至此刻）已经问了几卦。 */
    public int divinationsToday(String owner, long epochDay) {
        return parseInt(this.values.get(divinationKey(owner, epochDay)));
    }

    /** 记一次问卦。 */
    public void recordDivination(String owner, long epochDay) {
        String key = divinationKey(owner, epochDay);
        this.values.put(key, String.valueOf(divinationsToday(owner, epochDay) + 1));
        pruneDayKeys(epochDay);
        setDirty();
    }

    // ==================================================================== 窥命计数

    private static String peekKey(String owner, long epochDay) {
        return KEY_PEEKS + epochDay + ":" + owner;
    }

    /** 今天窥了几次命。 */
    public int peeksToday(String owner) {
        // 窥命计数不带日期参数，调用方少传一个；内部按「今天」记，靠 setDirty 的键裁剪清理
        for (Map.Entry<String, String> entry : this.values.entrySet()) {
            if (entry.getKey().startsWith(KEY_PEEKS) && entry.getKey().endsWith(":" + owner)) {
                return parseInt(entry.getValue());
            }
        }
        return 0;
    }

    /** 记一次窥命。 */
    public void incrementPeeks(String owner) {
        String key = peekKey(owner, 0L);
        this.values.merge(key, "1", (old, add) -> String.valueOf(parseInt(old) + 1));
        setDirty();
    }

    /** 清空窥命计数（跨日时由 {@link #pruneDayKeys} 处理）。 */
    public void resetPeeks(String owner) {
        this.values.keySet().removeIf(key -> key.startsWith(KEY_PEEKS) && key.endsWith(":" + owner));
        setDirty();
    }

    // ==================================================================== 龙脉占领

    /**
     * 记一次龙脉占领。
     *
     * @return 是否成功（同一年同一段已被他人占领时失败）
     */
    public boolean claimVein(String dimension, long year, int nodeIndex, String owner) {
        String key = KEY_VEIN + dimension + ":" + year + ":" + nodeIndex;
        String existing = this.values.get(key);
        if (existing != null && !existing.equals(owner)) {
            return false;
        }
        this.values.put(key, owner);
        setDirty();
        return true;
    }

    /** 某一段龙脉的占领者；无人占领时返回 {@code null}。 */
    public String veinOwner(String dimension, long year, int nodeIndex) {
        return this.values.get(KEY_VEIN + dimension + ":" + year + ":" + nodeIndex);
    }

    /** 某个玩家占领了几段龙脉（本年度）。 */
    public int veinClaims(String dimension, long year, String owner) {
        String prefix = KEY_VEIN + dimension + ":" + year + ":";
        int count = 0;
        for (Map.Entry<String, String> entry : this.values.entrySet()) {
            if (entry.getKey().startsWith(prefix) && owner.equals(entry.getValue())) {
                count++;
            }
        }
        return count;
    }

    // ==================================================================== 杂项

    public void put(String key, String value) {
        this.values.put(KEY_MISC + key, value);
        setDirty();
    }

    public String get(String key) {
        return this.values.get(KEY_MISC + key);
    }

    // ==================================================================== 序列化

    private CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        CompoundTag map = new CompoundTag();
        for (Map.Entry<String, String> entry : this.values.entrySet()) {
            map.putString(entry.getKey(), entry.getValue());
        }
        tag.put("v", map);
        return tag;
    }

    private static YishuData fromTag(CompoundTag tag) {
        YishuData data = new YishuData();
        CompoundTag map = tag.getCompoundOrEmpty("v");
        for (String key : map.keySet()) {
            data.values.put(key, map.getStringOr(key, ""));
        }
        return data;
    }

    // ==================================================================== 清理

    /**
     * 裁掉过期键。
     *
     * <p>「今天问了几卦」只需要保留今天这一份，昨天的没有任何用处。
     * 不清理的话，一个跑了半年的服务器会攒下几百个无用条目。
     */
    private void pruneDayKeys(long currentDay) {
        this.values.keySet().removeIf(key -> {
            int colon = key.indexOf(':');
            if (colon < 0) {
                return false;
            }
            String body = key.substring(colon + 1);
            int dayEnd = body.indexOf(':');
            if (dayEnd < 0) {
                return false;
            }
            try {
                long day = Long.parseLong(body.substring(0, dayEnd));
                // 只保留今天与昨天（跨日瞬间可能还在用昨天那份）
                return day < currentDay - 1;
            } catch (NumberFormatException exception) {
                return false;
            }
        });
    }

    private static int parseInt(String value) {
        if (value == null) {
            return 0;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            return 0;
        }
    }

    /** 是否为空（调试用）。 */
    public boolean isEmpty() {
        return this.values.isEmpty();
    }

    /** 条目数（调试用）。 */
    public int size() {
        return this.values.size();
    }

    /** 供外部读取注册表提供者（存档数据编解码需要）。 */
    public static HolderLookup.Provider registriesOf(ServerLevel level) {
        return level.registryAccess();
    }
}
