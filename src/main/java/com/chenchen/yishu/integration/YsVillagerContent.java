package com.chenchen.yishu.integration;

import com.chenchen.yishu.core.YsReg;
import com.chenchen.yishu.registry.YsRegistry;
import com.google.common.collect.ImmutableSet;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.trading.TradeSet;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.Map;

/**
 * 界域职业 —— 三个界域、六位术数手艺人。
 *
 * <p>原版村民认领工作方块就会转职。六位手艺人的工位分别是：
 * <ul>
 *   <li>命理界：星象师（观气台）· 司命（香炉）</li>
 *   <li>风水界：风水师（八卦台）· 堪舆师（石敢当）</li>
 *   <li>六爻界：卜者（卦盘）· 蓍师（祭坛）</li>
 * </ul>
 *
 * <p>交易走数据包：{@code data/yishu/trade_set/<id>/level_<n>.json}
 * 引用 {@code #yishu:<id>/level_<n>} 标签，标签里列的是
 * {@code data/yishu/villager_trade/<id>/<n>/<名字>.json} 定义的具体交易。
 * 这条链路和原版职业一模一样，运行时不改任何交易列表。
 *
 * <p><b>POI 桥接（关键修复）</b>：26.2 的 {@code PoiTypes} 用一张私有静态表
 * {@code TYPE_BY_STATE} 做「方块状态 → 兴趣点类型」的查询，而这张表只在
 * 游戏引导阶段为原版方块填过 —— 模组注册的兴趣点类型不在表里，
 * 村民就永远「看不见」界域工作方块，也就永远无法转职。
 * 所以这里在注册后把工作方块的每一种状态反射写进那张表。
 *
 * <p>纹理：村民外观按职业 id 找
 * {@code textures/entity/villager/profession/<id>.png}（命名空间随职业键），
 * 等级徽章复用原版的 {@code profession_level/*.png}。
 */
public final class YsVillagerContent {

    private static final Logger LOGGER = LoggerFactory.getLogger("Yishu/Villager");

    /** POI 状态表是否已成功桥接（只提示一次）。 */
    private static boolean poiBridgeReady = false;

    private YsVillagerContent() {
    }

    /** 六个界域职业的工位与工声。 */
    public static void register() {
        add("astrologer", "星象师", YsRegistry.QI_OBSERVATORY,
                SoundEvents.VILLAGER_WORK_CLERIC);
        add("fate_master", "司命", YsRegistry.CENSER,
                SoundEvents.VILLAGER_WORK_CLERIC);
        add("geomancer", "风水师", YsRegistry.BAGUA_PLATFORM,
                SoundEvents.VILLAGER_WORK_CARTOGRAPHER);
        add("surveyor", "堪舆师", YsRegistry.SHI_GANDANG,
                SoundEvents.VILLAGER_WORK_MASON);
        add("diviner", "卜者", YsRegistry.GUA_BOARD,
                SoundEvents.VILLAGER_WORK_LIBRARIAN);
        add("yarrow_master", "蓍师", YsRegistry.ALTAR,
                SoundEvents.VILLAGER_WORK_FLETCHER);

        // ---- 第 15 轮：三大术数的三位手艺人 ----
        add("star_master", "星命师", YsRegistry.ZIWEI_TERRACE,
                SoundEvents.VILLAGER_WORK_CLERIC);
        add("lesson_master", "课师", YsRegistry.LIUREN_ALTAR,
                SoundEvents.VILLAGER_WORK_LIBRARIAN);
        add("array_master", "局师", YsRegistry.QIMEN_ARRAY,
                SoundEvents.VILLAGER_WORK_FLETCHER);

        // ---- 第 16 轮：三术界的第二工位 ----
        add("dou_master", "斗枢师", YsRegistry.DOU_ALTAR,
                SoundEvents.VILLAGER_WORK_CLERIC);
        add("guiren_master", "贵人师", YsRegistry.GUIREN_ALTAR,
                SoundEvents.VILLAGER_WORK_LIBRARIAN);
        add("bamen_master", "八门师", YsRegistry.BAMEN_ALTAR,
                SoundEvents.VILLAGER_WORK_FLETCHER);
    }

    /**
     * 注册一个职业：工作方块 → 兴趣点 → 职业本体。
     *
     * @param id        职业 id（同时是兴趣点 id 与交易池目录名）
     * @param zhName    显示名（中文，直接写进 Component）
     * @param jobSite   工作方块（村民认领它就会转职）
     * @param workSound 工作时播放的音效
     */
    private static void add(String id, String zhName, Block jobSite, SoundEvent workSound) {
        // ---- 兴趣点：村民靠它找到「工位」 ----
        ResourceKey<PoiType> poiKey =
                ResourceKey.create(Registries.POINT_OF_INTEREST_TYPE, YsReg.id(id));
        PoiType poi = new PoiType(
                ImmutableSet.copyOf(jobSite.getStateDefinition().getPossibleStates()), 1, 1);
        Holder.Reference<PoiType> poiHolder =
                Registry.registerForHolder(BuiltInRegistries.POINT_OF_INTEREST_TYPE, poiKey, poi);
        bridgePoiStates(poiHolder, jobSite);

        // ---- 等级 → 交易池（1..3 级）----
        Int2ObjectMap<ResourceKey<TradeSet>> tradeSets = new Int2ObjectOpenHashMap<>();
        for (int level = 1; level <= 3; level++) {
            tradeSets.put(level, ResourceKey.create(
                    Registries.TRADE_SET, YsReg.id(id + "/level_" + level)));
        }

        // ---- 职业本体 ----
        VillagerProfession profession = new VillagerProfession(
                Component.literal(zhName),
                holder -> holder.is(poiKey),   // 已认领的工位
                holder -> holder.is(poiKey),   // 可认领的工位
                ImmutableSet.of(),             // 想要捡起的物品（无）
                ImmutableSet.of(),             // 副工位（无）
                workSound,
                tradeSets);
        Registry.register(BuiltInRegistries.VILLAGER_PROFESSION, YsReg.id(id), profession);
        LOGGER.info("[Yishu] 村民职业已注册：{}（yishu:{}）", zhName, id);
    }

    /**
     * 把工作方块的每一种状态写进 {@code PoiTypes.TYPE_BY_STATE}。
     *
     * <p>这张表是私有的，且游戏引导阶段之后就没人再填 —— 只能用反射。
     * 失败不影响模组启动，只是村民会认不出工位（会打警告）。
     */
    @SuppressWarnings("unchecked")
    private static void bridgePoiStates(Holder<PoiType> holder, Block jobSite) {
        try {
            Field field = PoiTypes.class.getDeclaredField("TYPE_BY_STATE");
            field.setAccessible(true);
            Map<BlockState, Holder<PoiType>> byState =
                    (Map<BlockState, Holder<PoiType>>) field.get(null);
            for (BlockState state : jobSite.getStateDefinition().getPossibleStates()) {
                byState.put(state, holder);
            }
            if (!poiBridgeReady) {
                poiBridgeReady = true;
                LOGGER.info("[Yishu] 兴趣点状态表已桥接：村民可以认领界域工作方块了");
            }
        } catch (Throwable t) {
            LOGGER.warn("[Yishu] 兴趣点状态表桥接失败，村民将无法认领工作方块", t);
        }
    }

    /** 调试与文档用：一句话描述这套职业。 */
    public static String describe() {
        return "界域职业：星象师（观气台）· 司命（香炉）· 风水师（八卦台）· "
                + "堪舆师（石敢当）· 卜者（卦盘）· 蓍师（祭坛）；"
                + "交易走数据包注入，村民认领工作方块即可转职。";
    }
}
