package com.chenchen.yishu.integration;

import com.chenchen.yishu.registry.YsRegistry;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * 原版整合 —— 让「易枢」真正长在原版世界里，而不是一个孤立的系统。
 *
 * <p>两条路：
 * <ol>
 *   <li><b>箱子战利品注入</b>：原版矿井、地牢、神庙、要塞、村庄、远古城市、
 *       堡垒遗迹、末地城的箱子里能捡到模组的铜钱、蓍草、符箓等物品——
 *       玩家探索原版结构时会自然「遇到」这个模组。</li>
 *   <li><b>怪物掉落</b>：女巫掉蓍草与吉符、唤魔者掉龟甲、猪灵对铜钱感兴趣、
 *       烈焰人掉烈焰碎片、末影人偶尔掉虚空碎片——击杀流程里就有模组产物。</li>
 * </ol>
 *
 * <p>注意确定性边界：命盘、风水、卦象仍然是纯种子的函数；而箱子与掉落
 * 走原版战利品随机（level.getRandom()），这与原版行为一致，不算破坏约定。
 */
public final class VanillaIntegration {

    private VanillaIntegration() {
    }

    /** 一条带权重的战利品条目。 */
    private record Entry(ItemLike item, int weight) {
    }

    /** 箱子表 → 注入条目。 */
    private static final Map<ResourceKey<LootTable>, List<Entry>> CHEST_INJECTIONS = new HashMap<>();

    public static void register() {
        buildChestTable();
        registerChestInjections();
        registerMobDrops();
    }

    // ==================================================================== 箱子

    /** 组装「原版哪里能捡到什么」。 */
    private static void buildChestTable() {
        // 简单地牢 / 废弃矿井：入门级物品，铜钱蓍草为主
        List<Entry> common = List.of(
                new Entry(YsRegistry.BRONZE_COINS, 30),
                new Entry(YsRegistry.YARROW_STALKS, 25),
                new Entry(YsRegistry.WARD_TALISMAN, 10),
                new Entry(YsRegistry.FORTUNE_TALISMAN, 10),
                new Entry(YsRegistry.CLARITY_TALISMAN, 8),
                new Entry(YsRegistry.PEEK_CHARM, 6),
                new Entry(YsRegistry.EARTH_STONE, 6),
                new Entry(YsRegistry.ANCIENT_SCROLL, 5),
                new Entry(YsRegistry.TURTLE_SHELL, 5),
                new Entry(YsRegistry.FATE_SLIP, 8),
                new Entry(YsRegistry.FATE_BOOK, 4),
                new Entry(YsRegistry.ASTRAL_DUST, 8),
                new Entry(YsRegistry.QI_PILL, 8),
                new Entry(YsRegistry.TALISMAN_PAPER, 8),
                new Entry(YsRegistry.STAR_FUR, 6),
                new Entry(YsRegistry.INK_MEMBRANE, 6));
        CHEST_INJECTIONS.put(BuiltInLootTables.SIMPLE_DUNGEON, common);
        CHEST_INJECTIONS.put(BuiltInLootTables.ABANDONED_MINESHAFT, common);
        CHEST_INJECTIONS.put(BuiltInLootTables.STRONGHOLD_CROSSING, common);
        CHEST_INJECTIONS.put(BuiltInLootTables.VILLAGE_TEMPLE, common);
        CHEST_INJECTIONS.put(BuiltInLootTables.IGLOO_CHEST, common);
        CHEST_INJECTIONS.put(BuiltInLootTables.SHIPWRECK_TREASURE, common);

        // 沙漠神庙 / 丛林神庙：偏向灵石与符箓
        List<Entry> temple = List.of(
                new Entry(YsRegistry.BRONZE_COINS, 25),
                new Entry(YsRegistry.WARD_TALISMAN, 15),
                new Entry(YsRegistry.TAISUI_TALISMAN, 10),
                new Entry(YsRegistry.WOOD_STONE, 8),
                new Entry(YsRegistry.FIRE_STONE, 8),
                new Entry(YsRegistry.METAL_STONE, 8),
                new Entry(YsRegistry.WATER_STONE, 8),
                new Entry(YsRegistry.YINYANG_STONE, 5),
                new Entry(YsRegistry.TURTLE_SHELL, 8),
                new Entry(YsRegistry.FENG_SHUI_COMPASS, 3),
                new Entry(YsRegistry.SUNDIAL, 6),
                new Entry(YsRegistry.DRAGON_RULER, 4),
                new Entry(YsRegistry.VERDANT_GEM, 6),
                new Entry(YsRegistry.MICA, 6),
                new Entry(YsRegistry.CLARITY_PILL, 8),
                new Entry(YsRegistry.CALM_PILL, 8),
                new Entry(YsRegistry.SWIFT_PILL, 6),
                new Entry(YsRegistry.BAGUA_MIRROR, 4),
                new Entry(YsRegistry.VEIN_MAP, 5));
        CHEST_INJECTIONS.put(BuiltInLootTables.DESERT_PYRAMID, temple);
        CHEST_INJECTIONS.put(BuiltInLootTables.JUNGLE_TEMPLE, temple);

        // 远古城市：窥命符与阴阳石（深渊与命理的呼应）
        CHEST_INJECTIONS.put(BuiltInLootTables.ANCIENT_CITY, List.of(
                new Entry(YsRegistry.PEEK_CHARM, 15),
                new Entry(YsRegistry.CLARITY_TALISMAN, 12),
                new Entry(YsRegistry.YINYANG_STONE, 10),
                new Entry(YsRegistry.BRONZE_COINS, 20),
                new Entry(YsRegistry.SOUL_CORE, 8),
                new Entry(YsRegistry.STAR_CRYSTAL, 5),
                new Entry(YsRegistry.ASTRAL_KEY, 6),
                new Entry(YsRegistry.VEIN_SCALE, 6),
                new Entry(YsRegistry.JADE_WOOL, 6),
                new Entry(YsRegistry.VEIN_MAP, 5)));

        // 下界堡垒：铜钱、太岁符、火灵石
        CHEST_INJECTIONS.put(BuiltInLootTables.BASTION_TREASURE, List.of(
                new Entry(YsRegistry.BRONZE_COINS, 20),
                new Entry(YsRegistry.TAISUI_TALISMAN, 12),
                new Entry(YsRegistry.FIRE_STONE, 10),
                new Entry(YsRegistry.EARTH_STONE, 10),
                new Entry(YsRegistry.PEEK_CHARM, 6),
                new Entry(YsRegistry.FLAME_FRAGMENT, 8),
                new Entry(YsRegistry.SOUL_CORE, 6),
                new Entry(YsRegistry.METEOR_IRON, 6),
                new Entry(YsRegistry.ONYX, 5),
                new Entry(YsRegistry.YAO_KEY, 5),
                new Entry(YsRegistry.WARD_PILL, 8),
                new Entry(YsRegistry.INK_MEMBRANE, 6)));

        // 末地城：虚空碎片与龙核碎片
        CHEST_INJECTIONS.put(BuiltInLootTables.END_CITY_TREASURE, List.of(
                new Entry(YsRegistry.VOID_SHARD, 12),
                new Entry(YsRegistry.CHORUS_CORE, 8),
                new Entry(YsRegistry.DRAGON_CORE_SHARD, 4),
                new Entry(YsRegistry.PEEK_CHARM, 10),
                new Entry(YsRegistry.BRONZE_COINS, 18),
                new Entry(YsRegistry.DRAGON_BREATH_CORE, 6),
                new Entry(YsRegistry.DRAGON_RULER, 8),
                new Entry(YsRegistry.STAR_CRYSTAL, 6),
                new Entry(YsRegistry.INK_IRON, 5),
                new Entry(YsRegistry.STAR_FUR, 6),
                new Entry(YsRegistry.TAISUI_PILL, 4),
                new Entry(YsRegistry.JADE_WOOL, 5)));

        // 第 10 轮：让「遗」味铺满更多原版结构
        CHEST_INJECTIONS.put(BuiltInLootTables.WOODLAND_MANSION, List.of(
                new Entry(YsRegistry.JADE_KEY, 6),
                new Entry(YsRegistry.ORACLE_BONE, 6),
                new Entry(YsRegistry.YAO_SHARD, 8),
                new Entry(YsRegistry.CLARITY_TALISMAN, 8),
                new Entry(YsRegistry.PEEK_CHARM, 10),
                new Entry(YsRegistry.CALM_PILL, 6),
                new Entry(YsRegistry.TALISMAN_PAPER, 8)));
        CHEST_INJECTIONS.put(BuiltInLootTables.PILLAGER_OUTPOST, List.of(
                new Entry(YsRegistry.BRONZE_COINS, 20),
                new Entry(YsRegistry.STAR_SAND, 6),
                new Entry(YsRegistry.FIRE_STONE, 6),
                new Entry(YsRegistry.TAISUI_TALISMAN, 8)));
        CHEST_INJECTIONS.put(BuiltInLootTables.BURIED_TREASURE, List.of(
                new Entry(YsRegistry.MICA, 8),
                new Entry(YsRegistry.JADE_GRIT, 8),
                new Entry(YsRegistry.TURTLE_SHELL, 8),
                new Entry(YsRegistry.BRONZE_COINS, 16)));
        CHEST_INJECTIONS.put(BuiltInLootTables.RUINED_PORTAL, List.of(
                new Entry(YsRegistry.SUNDIAL, 4),
                new Entry(YsRegistry.FATE_SLIP, 8),
                new Entry(YsRegistry.YARROW_STALKS, 8),
                new Entry(YsRegistry.FLAME_FRAGMENT, 4)));
    }

    /** 挂 LootTableEvents.MODIFY，把每个表的小池子塞进去。 */
    private static void registerChestInjections() {
        LootTableEvents.MODIFY.register((key, builder, source, registries) -> {
            List<Entry> entries = CHEST_INJECTIONS.get(key);
            if (entries == null) {
                return;
            }
            LootPool.Builder pool = LootPool.lootPool()
                    .setRolls(ConstantValue.exactly(1.0F));
            for (Entry entry : entries) {
                pool.add(LootItem.lootTableItem(entry.item()).setWeight(entry.weight()));
            }
            // 编译期 LootTable.Builder 还没有 FabricLootTableBuilder 接口，运行时由 mixin 注入
            ((net.fabricmc.fabric.api.loot.v3.FabricLootTableBuilder) (Object) builder).pool(pool.build());
        });
    }

    // ==================================================================== 怪物掉落

    /** 挂 ServerLivingEntityEvents.AFTER_DEATH，按生物类型给模组产物。 */
    private static void registerMobDrops() {
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, damageSource) -> {
            if (!(entity.level() instanceof ServerLevel server)) {
                return;
            }
            RandomSource random = server.getRandom();
            // 26.2 里常量在非泛型的 EntityTypes 上，泛型类型仍是 EntityType
            EntityType<?> type = entity.getType();

            if (type == EntityTypes.WITCH) {
                // 女巫：蓍草（她懂草药）+ 偶尔吉符
                drop(server, entity, YsRegistry.YARROW_STALKS, 1, 2, 0.5F);
                drop(server, entity, YsRegistry.FORTUNE_TALISMAN, 1, 1, 0.12F);
            } else if (type == EntityTypes.EVOKER) {
                // 唤魔者：龟甲（他懂仪式）+ 清心符
                drop(server, entity, YsRegistry.TURTLE_SHELL, 1, 1, 0.5F);
                drop(server, entity, YsRegistry.CLARITY_TALISMAN, 1, 1, 0.3F);
            } else if (type == EntityTypes.PIGLIN || type == EntityTypes.PIGLIN_BRUTE) {
                // 猪灵：对铜钱很有兴趣（掉的不是金子是「更老的钱」）
                drop(server, entity, YsRegistry.BRONZE_COINS, 1, 3, 0.3F);
            } else if (type == EntityTypes.BLAZE) {
                // 烈焰人：烈焰碎片
                drop(server, entity, YsRegistry.FLAME_FRAGMENT, 1, 1, 0.15F);
            } else if (type == EntityTypes.ENDERMAN) {
                // 末影人：偶尔夹带虚空碎片
                drop(server, entity, YsRegistry.VOID_SHARD, 1, 1, 0.04F);
            } else if (type == EntityTypes.WITHER_SKELETON) {
                // 凋灵骷髅：护身符
                drop(server, entity, YsRegistry.WARD_TALISMAN, 1, 1, 0.1F);
            } else if (type == EntityTypes.SHULKER) {
                // 潜影贝：紫颂核心
                drop(server, entity, YsRegistry.CHORUS_CORE, 1, 1, 0.1F);
            }
        });
    }

    /** 概率掉落 [min, max] 个某物品。 */
    private static void drop(ServerLevel level, LivingEntity entity, Item item,
                             int min, int max, float chance) {
        if (level.getRandom().nextFloat() >= chance) {
            return;
        }
        int count = min + (max > min ? level.getRandom().nextInt(max - min + 1) : 0);
        if (count <= 0) {
            return;
        }
        entity.spawnAtLocation(level, new net.minecraft.world.item.ItemStack(item, count));
    }

    /** 调试用：列出当前注入了哪些原版箱子表。 */
    public static List<String> injectedTables() {
        List<String> names = new ArrayList<>();
        for (ResourceKey<LootTable> key : CHEST_INJECTIONS.keySet()) {
            names.add(key.identifier().toString());
        }
        java.util.Collections.sort(names);
        return names;
    }
}
