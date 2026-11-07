package com.chenchen.yishu.registry;

import com.chenchen.yishu.YishuMod;
import com.chenchen.yishu.content.YsBlockContent;
import com.chenchen.yishu.content.YsItemContent;
import com.chenchen.yishu.content.YsPortalContent;
import com.chenchen.yishu.core.YsReg;
import com.chenchen.yishu.fate.GanZhi.WuXing;
import com.chenchen.yishu.fluid.YsFluids;
import com.chenchen.yishu.spirit.YsMobs;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.fabricmc.fabric.api.registry.FlammableBlockRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.effect.MobEffect;
import com.chenchen.yishu.divination.Oracle;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.vehicle.boat.Boat;
import net.minecraft.world.entity.vehicle.boat.ChestBoat;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BoatItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.DoubleHighBlockItem;
import net.minecraft.world.item.SignItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FlowerBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.PressurePlateBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StandingSignBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.UntintedParticleLeavesBlock;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 易枢 · 注册中枢。
 *
 * <p>26.x 的关键写法：所有 {@code Item.Properties} / {@code BlockBehaviour.Properties}
 * 都必须显式 {@code setId(ResourceKey)}，否则注册阶段直接抛异常。
 * 这里把「造 key → 注册 → 返回实例」收成一个方法，内容类里就不必重复样板。
 *
 * <p>注册顺序刻意按<b>玩法循环</b>而不是按字母：玩家打开创造模式物品栏，
 * 从头往下翻一遍，就大致明白这个模组该怎么玩了。
 */
public final class YsRegistry {

    private YsRegistry() {
    }

    // ==================================================================== 维度键
    //
    // 三个术数界域的 ResourceKey。传送门的目标、旅行的目的地、群系的归属，
    // 全部从这里取 —— 与数据文件 data/yishu/dimension/*.json 一一对应。

    public static final ResourceKey<Level> FATE_REALM_KEY =
            ResourceKey.create(Registries.DIMENSION, YishuMod.id("fate_realm"));
    public static final ResourceKey<Level> FENGSHUI_REALM_KEY =
            ResourceKey.create(Registries.DIMENSION, YishuMod.id("fengshui_realm"));
    public static final ResourceKey<Level> LIUYAO_REALM_KEY =
            ResourceKey.create(Registries.DIMENSION, YishuMod.id("liuyao_realm"));

    // ---- 第 16 轮：三大术数各自的界域 ----

    /** 紫微界 —— 紫微斗数的天穹，浮空岛与紫金星光。 */
    public static final ResourceKey<Level> ZIWEI_REALM_KEY =
            ResourceKey.create(Registries.DIMENSION, YishuMod.id("ziwei_realm"));
    /** 六壬界 —— 大六壬的水天，青碧汪洋与陡崖。 */
    public static final ResourceKey<Level> LIUREN_REALM_KEY =
            ResourceKey.create(Registries.DIMENSION, YishuMod.id("liuren_realm"));
    /** 奇门界 —— 奇门遁甲的九宫大地，赤金与炉火。 */
    public static final ResourceKey<Level> QIMEN_REALM_KEY =
            ResourceKey.create(Registries.DIMENSION, YishuMod.id("qimen_realm"));

    /** 太乙界 —— 三式之三，浮空九宫星台。 */
    public static final ResourceKey<Level> TAIYI_REALM_KEY =
            ResourceKey.create(Registries.DIMENSION, YishuMod.id("taiyi_realm"));

    // ==================================================================== 物品

    /** 命盘玉简 —— 玩法的起点。 */
    public static final Item FATE_SLIP = item("fate_slip",
            new YsItemContent.FateSlip(props("fate_slip").stacksTo(1).rarity(Rarity.RARE)));

    /** 风水罗盘 —— 勘测工具。 */
    public static final Item FENG_SHUI_COMPASS = item("feng_shui_compass",
            new YsItemContent.FengShuiCompass(props("feng_shui_compass").stacksTo(1)));

    /** 寻龙尺 —— 找龙脉。 */
    public static final Item DRAGON_RULER = item("dragon_ruler",
            new YsItemContent.DragonRuler(props("dragon_ruler").stacksTo(1)));

    /** 窥命符 —— 看别人的命。 */
    public static final Item PEEK_CHARM = item("peek_charm",
            new YsItemContent.PeekCharm(props("peek_charm").stacksTo(16)));

    // ---- 起卦器具 ----

    public static final Item BRONZE_COINS = item("bronze_coins",
            new YsItemContent.DivinationTool(props("bronze_coins").stacksTo(1), false));
    public static final Item YARROW_STALKS = item("yarrow_stalks",
            new YsItemContent.DivinationTool(props("yarrow_stalks").stacksTo(1), false));
    public static final Item TURTLE_SHELL = item("turtle_shell",
            new YsItemContent.DivinationTool(props("turtle_shell").stacksTo(1), true));
    public static final Item SUNDIAL = item("sundial",
            new YsItemContent.DivinationTool(props("sundial").stacksTo(1), true));

    // ---- 符箓 ----

    public static final Item WARD_TALISMAN = item("ward_talisman",
            new YsItemContent.Talisman(props("ward_talisman").stacksTo(16), "ward"));
    public static final Item FORTUNE_TALISMAN = item("fortune_talisman",
            new YsItemContent.Talisman(props("fortune_talisman").stacksTo(16), "fortune"));
    public static final Item CLARITY_TALISMAN = item("clarity_talisman",
            new YsItemContent.Talisman(props("clarity_talisman").stacksTo(16), "clarity"));
    public static final Item TAISUI_TALISMAN = item("taisui_talisman",
            new YsItemContent.Talisman(props("taisui_talisman").stacksTo(16), "taisui"));

    // ---- 五行灵石 ----

    public static final Item WOOD_STONE = elementStone("wood_stone", WuXing.MU);
    public static final Item FIRE_STONE = elementStone("fire_stone", WuXing.HUO);
    public static final Item EARTH_STONE = elementStone("earth_stone", WuXing.TU);
    public static final Item METAL_STONE = elementStone("metal_stone", WuXing.JIN);
    public static final Item WATER_STONE = elementStone("water_stone", WuXing.SHUI);

    // ---- 材料 ----

    public static final Item YINYANG_STONE = item("yinyang_stone",
            new YsItemContent.YinYangStone(props("yinyang_stone").stacksTo(16)));
    public static final Item DRAGON_CORE_SHARD = item("dragon_core_shard",
            new YsItemContent.Material(props("dragon_core_shard").stacksTo(16).rarity(Rarity.UNCOMMON),
                    "item.yishu.dragon_core_shard.note"));
    public static final Item FLAME_FRAGMENT = item("flame_fragment",
            new YsItemContent.Material(props("flame_fragment").stacksTo(16).fireResistant(),
                    "item.yishu.flame_fragment.note"));
    public static final Item SOUL_CORE = item("soul_core",
            new YsItemContent.Material(props("soul_core").stacksTo(16).fireResistant()
                    .rarity(Rarity.UNCOMMON), "item.yishu.soul_core.note"));
    public static final Item VOID_SHARD = item("void_shard",
            new YsItemContent.Material(props("void_shard").stacksTo(16).rarity(Rarity.UNCOMMON),
                    "item.yishu.void_shard.note"));
    public static final Item CHORUS_CORE = item("chorus_core",
            new YsItemContent.Material(props("chorus_core").stacksTo(16).rarity(Rarity.UNCOMMON),
                    "item.yishu.chorus_core.note"));
    public static final Item DRAGON_BREATH_CORE = item("dragon_breath_core",
            new YsItemContent.Material(props("dragon_breath_core").stacksTo(16).rarity(Rarity.RARE)
                    .fireResistant(), "item.yishu.dragon_breath_core.note"));

    /** 命理书 —— 对讲台右键导出命盘成书。 */
    public static final Item FATE_BOOK = item("fate_book",
            new com.chenchen.yishu.content.FateBookItem(props("fate_book").stacksTo(1).rarity(Rarity.UNCOMMON),
                    "item.yishu.fate_book.note"));

    /** 古卷 —— 遗迹战利品，展开读《山河志》的下一卷。 */
    public static final Item ANCIENT_SCROLL = item("ancient_scroll",
            new com.chenchen.yishu.content.AncientScrollItem(
                    props("ancient_scroll").stacksTo(16).rarity(Rarity.UNCOMMON),
                    "item.yishu.ancient_scroll.note"));

    // ---- 丹道（第 11 轮：服食获效） ----

    /** 归元丹 —— 把散掉的元气收回来。 */
    public static final Item QI_PILL = item("qi_pill",
            new YsItemContent.Pill(props("qi_pill").stacksTo(16),
                    new YsItemContent.Dose(MobEffects.REGENERATION, 200, 1)));

    /** 明目丹 —— 夜里也看得见。 */
    public static final Item CLARITY_PILL = item("clarity_pill",
            new YsItemContent.Pill(props("clarity_pill").stacksTo(16),
                    new YsItemContent.Dose(MobEffects.NIGHT_VISION, 4800, 0)));

    /** 定神丹 —— 心定，则外邪难侵。 */
    public static final Item CALM_PILL = item("calm_pill",
            new YsItemContent.Pill(props("calm_pill").stacksTo(16),
                    new YsItemContent.Dose(MobEffects.RESISTANCE, 1800, 0)));

    /** 疾行丹 —— 脚下生风。 */
    public static final Item SWIFT_PILL = item("swift_pill",
            new YsItemContent.Pill(props("swift_pill").stacksTo(16),
                    new YsItemContent.Dose(MobEffects.SPEED, 1800, 1)));

    /** 辟邪丹 —— 火气不侵。 */
    public static final Item WARD_PILL = item("ward_pill",
            new YsItemContent.Pill(props("ward_pill").stacksTo(16).rarity(Rarity.UNCOMMON),
                    new YsItemContent.Dose(MobEffects.FIRE_RESISTANCE, 3600, 0)));

    /** 太岁丹 —— 三味同炉，一年只该吃一颗。 */
    public static final Item TAISUI_PILL = item("taisui_pill",
            new YsItemContent.Pill(props("taisui_pill").stacksTo(8).rarity(Rarity.RARE),
                    new YsItemContent.Dose(MobEffects.REGENERATION, 200, 0),
                    new YsItemContent.Dose(MobEffects.RESISTANCE, 2400, 0),
                    new YsItemContent.Dose(MobEffects.SATURATION, 200, 0)));

    // ---- 灵物（第 11 轮） ----

    /** 空白符纸 —— 符箓的底子。 */
    public static final Item TALISMAN_PAPER = item("talisman_paper",
            new YsItemContent.Material(props("talisman_paper").stacksTo(64),
                    "item.yishu.talisman_paper.note"));

    /** 星狐绒 —— 星狐与观星者身上的一层柔光。 */
    public static final Item STAR_FUR = item("star_fur",
            new YsItemContent.Material(props("star_fur").stacksTo(64),
                    "item.yishu.star_fur.note"));

    /** 玉兔绒 —— 青玉界最细的一层绒毛。 */
    public static final Item JADE_WOOL = item("jade_wool",
            new YsItemContent.Material(props("jade_wool").stacksTo(64),
                    "item.yishu.jade_wool.note"));

    /** 爻蝠翼膜 —— 半透明的墨色薄翼。 */
    public static final Item INK_MEMBRANE = item("ink_membrane",
            new YsItemContent.Material(props("ink_membrane").stacksTo(64),
                    "item.yishu.ink_membrane.note"));

    /** 地脉鳞 —— 地脉龟与卦石守卫的甲片。 */
    public static final Item VEIN_SCALE = item("vein_scale",
            new YsItemContent.Material(props("vein_scale").stacksTo(64),
                    "item.yishu.vein_scale.note"));

    /** 寻脉图 —— 展开一次，指出最近遗迹的方向。 */
    public static final Item VEIN_MAP = item("vein_map",
            new YsItemContent.VeinMap(props("vein_map").stacksTo(1).rarity(Rarity.UNCOMMON)));

    /** 八卦镜 —— 照见周围生灵的「形」。 */
    public static final Item BAGUA_MIRROR = item("bagua_mirror",
            new YsItemContent.BaguaMirror(props("bagua_mirror").stacksTo(1).rarity(Rarity.UNCOMMON)));


    // ==================================================================== 三大术数（第 15 轮）
    //
    // 紫微斗数、大六壬、奇门遁甲 —— 三张盘、九道符、三样料。
    // 排盘逻辑在 com.chenchen.yishu.divination.Oracle，这里只做注册。

    /** 紫微命盘 —— 十四主星落十二宫。看的是「你是谁」。 */
    public static final Item ZIWEI_CHART = item("ziwei_chart",
            new YsItemContent.OracleDisc(props("ziwei_chart").stacksTo(1).rarity(Rarity.RARE),
                    Oracle.Kind.ZIWEI));

    /** 六壬式盘 —— 四课三传、十二天将。看的是「事怎么走」。 */
    public static final Item LIUREN_PLATE = item("liuren_plate",
            new YsItemContent.OracleDisc(props("liuren_plate").stacksTo(1).rarity(Rarity.RARE),
                    Oracle.Kind.LIUREN));

    /** 奇门局盘 —— 九宫八门九星八神。看的是「往哪走」。 */
    public static final Item QIMEN_DISC = item("qimen_disc",
            new YsItemContent.OracleDisc(props("qimen_disc").stacksTo(1).rarity(Rarity.RARE),
                    Oracle.Kind.QIMEN));

    // ---- 紫微三令 ----

    /** 紫微令 —— 帝星护体，诸煞不侵。 */
    public static final Item ZIWEI_TOKEN = charm("ziwei_token", "item.yishu.ziwei_token.note",
            effect(MobEffects.RESISTANCE, 2400, 1),
            effect(MobEffects.HEALTH_BOOST, 2400, 0));

    /** 天府令 —— 库星丰足，衣食自足。 */
    public static final Item TIANFU_TOKEN = charm("tianfu_token", "item.yishu.tianfu_token.note",
            effect(MobEffects.SATURATION, 200, 0),
            effect(MobEffects.LUCK, 2400, 1));

    /** 七杀令 —— 将星肃杀，决断如刀。 */
    public static final Item QISHA_TOKEN = charm("qisha_token", "item.yishu.qisha_token.note",
            effect(MobEffects.STRENGTH, 2400, 1),
            effect(MobEffects.SPEED, 1200, 0));

    // ---- 六壬三符 ----

    /** 贵人符 —— 诸神之首，遇难有人扶。 */
    public static final Item GUIREN_TALISMAN = charm("guiren_talisman",
            "item.yishu.guiren_talisman.note",
            effect(MobEffects.LUCK, 2400, 0),
            effect(MobEffects.REGENERATION, 800, 0));

    /** 青龙符 —— 财喜尊贵，行进如龙。 */
    public static final Item QINGLONG_TALISMAN = charm("qinglong_talisman",
            "item.yishu.qinglong_talisman.note",
            effect(MobEffects.SPEED, 2400, 1),
            effect(MobEffects.JUMP_BOOST, 2400, 0));

    /** 白虎符 —— 道路丧伤，杀气最重。 */
    public static final Item BAIHU_TALISMAN = charm("baihu_talisman",
            "item.yishu.baihu_talisman.note",
            effect(MobEffects.STRENGTH, 2400, 1),
            effect(MobEffects.HASTE, 2400, 1));

    // ---- 奇门三门 ----

    /** 生门符 —— 生生不息。 */
    public static final Item SHENGMEN_TALISMAN = charm("shengmen_talisman",
            "item.yishu.shengmen_talisman.note",
            effect(MobEffects.REGENERATION, 1200, 1),
            effect(MobEffects.SATURATION, 200, 0));

    /** 开门符 —— 开张大吉。 */
    public static final Item KAIMEN_TALISMAN = charm("kaimen_talisman",
            "item.yishu.kaimen_talisman.note",
            effect(MobEffects.LUCK, 2400, 1),
            effect(MobEffects.NIGHT_VISION, 2400, 0));

    /** 休门符 —— 休养生息。 */
    public static final Item XIUMEN_TALISMAN = charm("xiumen_talisman",
            "item.yishu.xiumen_talisman.note",
            effect(MobEffects.RESISTANCE, 2400, 1),
            effect(MobEffects.ABSORPTION, 2400, 1));

    // ---- 三样术料 ----

    /** 紫微星尘 —— 从星辉矿石里筛出来的细粉。 */
    public static final Item ZIWEI_STAR_DUST = item("ziwei_star_dust",
            new YsItemContent.Material(props("ziwei_star_dust").stacksTo(64),
                    "item.yishu.ziwei_star_dust.note"));

    /** 课甲 —— 六壬起课用的甲片，一片只够起一课。 */
    public static final Item LIUREN_SHELL = item("liuren_shell",
            new YsItemContent.Material(props("liuren_shell").stacksTo(64),
                    "item.yishu.liuren_shell.note"));

    /** 遁甲残片 —— 九宫格上崩下来的一角。 */
    public static final Item QIMEN_SHARD = item("qimen_shard",
            new YsItemContent.Material(props("qimen_shard").stacksTo(64),
                    "item.yishu.qimen_shard.note"));

    // ==================================================================== 界域作物
    //
    // 第 13 轮：三个界域各自的粮食。它们长在自己村庄的田里，
    // 也是三界村民交易链的起点（村民会收购）。

    public static final Block ASTRAL_WHEAT = blockOnly("astral_wheat",
            properties -> new YsBlockContent.YsCrop(properties, () -> YsRegistry.ASTRAL_SEEDS),
            blockProps("astral_wheat", MapColor.COLOR_PURPLE)
                    .noCollision().randomTicks().instabreak().sound(SoundType.CROP));
    public static final Item ASTRAL_SEEDS = item("astral_seeds",
            new BlockItem(ASTRAL_WHEAT, props("astral_seeds")));

    public static final Block JADE_RICE = blockOnly("jade_rice",
            properties -> new YsBlockContent.YsCrop(properties, () -> YsRegistry.JADE_SEEDS),
            blockProps("jade_rice", MapColor.COLOR_GREEN)
                    .noCollision().randomTicks().instabreak().sound(SoundType.CROP));
    public static final Item JADE_SEEDS = item("jade_seeds",
            new BlockItem(JADE_RICE, props("jade_seeds")));

    public static final Block YAO_BEAN = blockOnly("yao_bean",
            properties -> new YsBlockContent.YsCrop(properties, () -> YsRegistry.YAO_SEEDS),
            blockProps("yao_bean", MapColor.COLOR_GRAY)
                    .noCollision().randomTicks().instabreak().sound(SoundType.CROP));
    public static final Item YAO_SEEDS = item("yao_seeds",
            new BlockItem(YAO_BEAN, props("yao_seeds")));

    /** 星辉穗 —— 吸饱了星光的穗子，嚼起来有点甜。 */
    public static final Item ASTRAL_GRAIN = item("astral_grain",
            new YsItemContent.Material(props("astral_grain").stacksTo(64)
                    .food(simpleFood(3, 0.5F)), "item.yishu.astral_grain.note"));

    /** 玉粒稻 —— 一粒一粒像小玉珠。 */
    public static final Item JADE_GRAIN = item("jade_grain",
            new YsItemContent.Material(props("jade_grain").stacksTo(64)
                    .food(simpleFood(5, 0.6F)), "item.yishu.jade_grain.note"));

    /** 爻纹豆 —— 豆荚上有一道天生的断纹。 */
    public static final Item YAO_POD = item("yao_pod",
            new YsItemContent.Material(props("yao_pod").stacksTo(64)
                    .food(simpleFood(4, 0.55F)), "item.yishu.yao_pod.note"));

    // ---- 第 16 轮：三术界的粮食 ----

    public static final Block ZIWEI_WHEAT = blockOnly("ziwei_wheat",
            properties -> new YsBlockContent.YsCrop(properties, () -> YsRegistry.ZIWEI_SEEDS),
            blockProps("ziwei_wheat", MapColor.COLOR_PURPLE)
                    .noCollision().randomTicks().instabreak().sound(SoundType.CROP));
    public static final Item ZIWEI_SEEDS = item("ziwei_seeds",
            new BlockItem(ZIWEI_WHEAT, props("ziwei_seeds")));

    public static final Block TAIYI_WHEAT = blockOnly("taiyi_wheat",
            properties -> new YsBlockContent.YsCrop(properties, () -> YsRegistry.TAIYI_SEEDS),
            blockProps("taiyi_wheat", MapColor.COLOR_LIGHT_BLUE)
                    .noCollision().randomTicks().instabreak().sound(SoundType.CROP));
    public static final Item TAIYI_SEEDS = item("taiyi_seeds",
            new BlockItem(TAIYI_WHEAT, props("taiyi_seeds")));

    public static final Block LIUREN_RICE = blockOnly("liuren_rice",
            properties -> new YsBlockContent.YsCrop(properties, () -> YsRegistry.LIUREN_SEEDS),
            blockProps("liuren_rice", MapColor.COLOR_CYAN)
                    .noCollision().randomTicks().instabreak().sound(SoundType.CROP));
    public static final Item LIUREN_SEEDS = item("liuren_seeds",
            new BlockItem(LIUREN_RICE, props("liuren_seeds")));

    public static final Block QIMEN_BEAN = blockOnly("qimen_bean",
            properties -> new YsBlockContent.YsCrop(properties, () -> YsRegistry.QIMEN_SEEDS),
            blockProps("qimen_bean", MapColor.COLOR_ORANGE)
                    .noCollision().randomTicks().instabreak().sound(SoundType.CROP));
    public static final Item QIMEN_SEEDS = item("qimen_seeds",
            new BlockItem(QIMEN_BEAN, props("qimen_seeds")));

    /** 紫微穗 —— 穗尖凝着一点星芒。 */
    public static final Item ZIWEI_GRAIN = item("ziwei_grain",
            new YsItemContent.Material(props("ziwei_grain").stacksTo(64)
                    .food(simpleFood(4, 0.5F)), "item.yishu.ziwei_grain.note"));

    /** 壬水稻 —— 米粒半透明，像含着一口水。 */
    public static final Item LIUREN_GRAIN = item("liuren_grain",
            new YsItemContent.Material(props("liuren_grain").stacksTo(64)
                    .food(simpleFood(5, 0.6F)), "item.yishu.liuren_grain.note"));

    /** 八门豆 —— 豆上有八道浅纹，据说是门。 */
    public static final Item QIMEN_GRAIN = item("qimen_grain",
            new YsItemContent.Material(props("qimen_grain").stacksTo(64)
                    .food(simpleFood(3, 0.55F)), "item.yishu.qimen_grain.note"));

    // ==================================================================== 方块

    public static final Block QI_OBSERVATORY = block("qi_observatory",
            YsBlockContent.QiObservatory::new,
            blockProps("qi_observatory", MapColor.COLOR_BROWN)
                    .strength(3.0F, 8.0F).requiresCorrectToolForDrops().sound(SoundType.WOOD)
                    .lightLevel(state -> 6));

    public static final Block CENSER = block("censer",
            YsBlockContent.Censer::new,
            blockProps("censer", MapColor.COLOR_BROWN)
                    .strength(2.0F, 6.0F).sound(SoundType.COPPER)
                    .lightLevel(state -> state.getValue(YsBlockContent.Censer.LIT) ? 12 : 3));

    public static final Block SHI_GANDANG = block("shi_gandang",
            YsBlockContent.ShiGandang::new,
            blockProps("shi_gandang", MapColor.STONE)
                    .strength(3.5F, 12.0F).requiresCorrectToolForDrops().sound(SoundType.STONE));

    public static final Block GUA_BOARD = block("gua_board",
            YsBlockContent.GuaBoard::new,
            blockProps("gua_board", MapColor.WOOD)
                    .strength(2.2F, 6.0F).sound(SoundType.WOOD).lightLevel(state -> 5));

    public static final Block BAGUA_PLATFORM = block("bagua_platform",
            YsBlockContent.BaguaPlatform::new,
            blockProps("bagua_platform", MapColor.SAND)
                    .strength(2.6F, 9.0F).requiresCorrectToolForDrops().sound(SoundType.STONE));

    public static final Block TAIJI_DIAGRAM = block("taiji_diagram",
            YsBlockContent.TaijiDiagram::new,
            blockProps("taiji_diagram", MapColor.QUARTZ)
                    .strength(2.4F, 9.0F).requiresCorrectToolForDrops().sound(SoundType.STONE)
                    .lightLevel(state -> 4));

    public static final Block ALTAR = block("altar",
            YsBlockContent.Altar::new,
            blockProps("altar", MapColor.DEEPSLATE)
                    .strength(3.2F, 12.0F).requiresCorrectToolForDrops().sound(SoundType.DEEPSLATE)
                    .lightLevel(state -> 7));

    // ---- 三座术坛（第 15 轮）----

    /** 紫微星台 —— 星命师的工位。 */
    public static final Block ZIWEI_TERRACE = block("ziwei_terrace",
            properties -> new YsBlockContent.OracleAltar(properties, Oracle.Kind.ZIWEI),
            blockProps("ziwei_terrace", MapColor.COLOR_PURPLE)
                    .strength(3.4F, 16.0F).requiresCorrectToolForDrops().sound(SoundType.AMETHYST)
                    .lightLevel(state -> 9));

    /** 六壬式坛 —— 课师的工位。 */
    public static final Block LIUREN_ALTAR = block("liuren_altar",
            properties -> new YsBlockContent.OracleAltar(properties, Oracle.Kind.LIUREN),
            blockProps("liuren_altar", MapColor.COLOR_BROWN)
                    .strength(3.2F, 16.0F).requiresCorrectToolForDrops().sound(SoundType.COPPER)
                    .lightLevel(state -> 7));

    /** 奇门局阵 —— 局师的工位。 */
    // ---- 第 16 轮：三术界的第二座工位 ----

    /** 斗枢台 —— 紫微界的第二工位，斗枢师认领它。 */
    public static final Block DOU_ALTAR = block("dou_altar",
            properties -> new YsBlockContent.OracleAltar(properties, Oracle.Kind.ZIWEI),
            blockProps("dou_altar", MapColor.COLOR_PURPLE).strength(3.5F, 9.0F)
                    .requiresCorrectToolForDrops().sound(SoundType.STONE)
                    .lightLevel(state -> 11));

    /** 太乙星台 / 文昌台 —— 太乙界的观星工位。 */
    public static final Block TAIYI_TERRACE = dimensionStone("taiyi_terrace", MapColor.COLOR_LIGHT_BLUE, 3.0F, 9.0F);
    public static final Block WENCHANG_ALTAR = dimensionStone("wenchang_altar", MapColor.COLOR_CYAN, 3.0F, 9.0F);

    /** 贵人台 —— 六壬界的第二工位，贵人师认领它。 */
    public static final Block GUIREN_ALTAR = block("guiren_altar",
            properties -> new YsBlockContent.OracleAltar(properties, Oracle.Kind.LIUREN),
            blockProps("guiren_altar", MapColor.COLOR_CYAN).strength(3.5F, 9.0F)
                    .requiresCorrectToolForDrops().sound(SoundType.STONE)
                    .lightLevel(state -> 11));

    /** 八门台 —— 奇门界的第二工位，八门师认领它。 */
    public static final Block BAMEN_ALTAR = block("bamen_altar",
            properties -> new YsBlockContent.OracleAltar(properties, Oracle.Kind.QIMEN),
            blockProps("bamen_altar", MapColor.COLOR_ORANGE).strength(3.5F, 9.0F)
                    .requiresCorrectToolForDrops().sound(SoundType.STONE)
                    .lightLevel(state -> 11));

    public static final Block QIMEN_ARRAY = block("qimen_array",
            properties -> new YsBlockContent.OracleAltar(properties, Oracle.Kind.QIMEN),
            blockProps("qimen_array", MapColor.DEEPSLATE)
                    .strength(3.6F, 18.0F).requiresCorrectToolForDrops().sound(SoundType.STONE)
                    .lightLevel(state -> 8));

    public static final Block RUIN_CORE = block("ruin_core",
            YsBlockContent.RuinCore::new,
            blockProps("ruin_core", MapColor.COLOR_PURPLE)
                    .strength(4.0F, 24.0F).requiresCorrectToolForDrops().sound(SoundType.AMETHYST)
                    .lightLevel(state -> state.getValue(YsBlockContent.RuinCore.ACTIVE) ? 15 : 6));

    public static final Block YINYANG_STELE = block("yinyang_stele",
            YsBlockContent.YinyangStele::new,
            blockProps("yinyang_stele", MapColor.COLOR_BLACK)
                    .strength(3.6F, 14.0F).requiresCorrectToolForDrops().sound(SoundType.DEEPSLATE)
                    .lightLevel(state -> state.getValue(YsBlockContent.YinyangStele.OPEN) ? 11 : 3));

    public static final Block DRAGON_CORE_BLOCK = block("dragon_core_block",
            YsBlockContent.DragonCoreBlock::new,
            blockProps("dragon_core_block", MapColor.COLOR_CYAN)
                    .strength(4.5F, 20.0F).requiresCorrectToolForDrops().sound(SoundType.AMETHYST)
                    .lightLevel(state -> 10));

    // ---- 五行灵石块 ----

    public static final Block WOOD_STONE_BLOCK = stoneBlock("wood_stone_block", WuXing.MU, MapColor.PLANT, 0x4C9A4C);
    public static final Block FIRE_STONE_BLOCK = stoneBlock("fire_stone_block", WuXing.HUO, MapColor.FIRE, 0xD0503C);
    public static final Block EARTH_STONE_BLOCK = stoneBlock("earth_stone_block", WuXing.TU, MapColor.DIRT, 0xB08A40);
    public static final Block METAL_STONE_BLOCK = stoneBlock("metal_stone_block", WuXing.JIN, MapColor.METAL, 0xC8C8D0);
    public static final Block WATER_STONE_BLOCK = stoneBlock("water_stone_block", WuXing.SHUI, MapColor.WATER, 0x3E6FB0);

    // ==================================================================== 维度建材
    //
    // 三个术数维度各自的「石头」：地表、遗迹、建筑统一用这三套材料，
    // 玩家一眼就能从建材的颜色认出自己站在哪个维度。

    // ---- 命理维度 · 星辉石 ----

    public static final Block ASTRAL_STONE = dimensionStone("astral_stone", MapColor.COLOR_PURPLE, 1.5F, 6.0F);
    public static final Block ASTRAL_BRICKS = dimensionStone("astral_bricks", MapColor.COLOR_PURPLE, 1.8F, 6.0F);
    public static final Block ASTRAL_LAMP = dimensionLamp("astral_lamp", MapColor.COLOR_MAGENTA);

    // ---- 风水维度 · 青玉石 ----

    public static final Block JADE_STONE = dimensionStone("jade_stone", MapColor.COLOR_GREEN, 1.5F, 6.0F);
    public static final Block JADE_BRICKS = dimensionStone("jade_bricks", MapColor.COLOR_GREEN, 1.8F, 6.0F);
    public static final Block JADE_LAMP = dimensionLamp("jade_lamp", MapColor.COLOR_CYAN);

    // ---- 六爻维度 · 爻纹石 ----

    public static final Block YAO_STONE = dimensionStone("yao_stone", MapColor.COLOR_GRAY, 1.5F, 6.0F);
    public static final Block YAO_BRICKS = dimensionStone("yao_bricks", MapColor.COLOR_GRAY, 1.8F, 6.0F);
    public static final Block YAO_LAMP = dimensionLamp("yao_lamp", MapColor.COLOR_RED);

    // ---- 维度建筑扩展：柱 / 台阶 / 楼梯 ----
    //
    // 有了石头和砖，还差能「搭出形状」的三件套。柱子做门楣与回廊，
    // 台阶与楼梯让神殿的每一层都能走上去 —— 三个维度，各来一套。

    public static final Block ASTRAL_PILLAR = pillar("astral_pillar", MapColor.COLOR_PURPLE);
    public static final Block ASTRAL_SLAB = slab("astral_slab", MapColor.COLOR_PURPLE);
    public static final Block ASTRAL_STAIRS = stairs("astral_stairs", MapColor.COLOR_PURPLE, ASTRAL_BRICKS);

    public static final Block JADE_PILLAR = pillar("jade_pillar", MapColor.COLOR_GREEN);
    public static final Block JADE_SLAB = slab("jade_slab", MapColor.COLOR_GREEN);
    public static final Block JADE_STAIRS = stairs("jade_stairs", MapColor.COLOR_GREEN, JADE_BRICKS);

    public static final Block YAO_PILLAR = pillar("yao_pillar", MapColor.COLOR_GRAY);
    public static final Block YAO_SLAB = slab("yao_slab", MapColor.COLOR_GRAY);
    public static final Block YAO_STAIRS = stairs("yao_stairs", MapColor.COLOR_GRAY, YAO_BRICKS);

    // ---- 第 16 轮：三术界建材（石 / 砖 / 灯）----

    public static final Block ZIWEI_STONE = dimensionStone("ziwei_stone", MapColor.COLOR_PURPLE, 1.5F, 6.0F);
    public static final Block ZIWEI_BRICKS = dimensionStone("ziwei_bricks", MapColor.COLOR_PURPLE, 1.8F, 6.0F);
    public static final Block ZIWEI_LAMP = dimensionLamp("ziwei_lamp", MapColor.COLOR_MAGENTA);

    public static final Block LIUREN_STONE = dimensionStone("liuren_stone", MapColor.COLOR_CYAN, 1.5F, 6.0F);
    public static final Block LIUREN_BRICKS = dimensionStone("liuren_bricks", MapColor.COLOR_CYAN, 1.8F, 6.0F);
    public static final Block LIUREN_LAMP = dimensionLamp("liuren_lamp", MapColor.COLOR_LIGHT_BLUE);

    public static final Block QIMEN_STONE = dimensionStone("qimen_stone", MapColor.COLOR_ORANGE, 1.5F, 6.0F);
    public static final Block QIMEN_BRICKS = dimensionStone("qimen_bricks", MapColor.COLOR_ORANGE, 1.8F, 6.0F);
    public static final Block QIMEN_LAMP = dimensionLamp("qimen_lamp", MapColor.COLOR_YELLOW);

    // ---- 第 19 轮：太乙界建材 ----

    public static final Block TAIYI_STONE = dimensionStone("taiyi_stone", MapColor.COLOR_LIGHT_BLUE, 1.5F, 6.0F);
    public static final Block TAIYI_BRICKS = dimensionStone("taiyi_bricks", MapColor.COLOR_LIGHT_BLUE, 1.8F, 6.0F);
    public static final Block TAIYI_LAMP = dimensionLamp("taiyi_lamp", MapColor.COLOR_CYAN);

    // ==================================================================== 维度矿石与灵植
    //
    // 每个维度的地下埋着它自己的矿脉，地表长着它自己的花与草。
    // 矿石挖出来的粗料（尘 / 砂 / 砾）既是材料，也是合成灵引的关键。

    // ---- 命理界 · 星辉矿脉（四矿）----

    public static final Block ASTRAL_ORE = oreBlock("astral_ore", MapColor.COLOR_PURPLE);
    public static final Block METEOR_ORE = oreBlock("meteor_ore", MapColor.COLOR_PURPLE);
    public static final Block STAR_SAND_ORE = oreBlock("star_sand_ore", MapColor.COLOR_YELLOW);
    public static final Block CRYSTAL_ORE = oreBlock("crystal_ore", MapColor.COLOR_PURPLE);
    public static final Item ASTRAL_DUST = materialItem("astral_dust", Rarity.COMMON);
    public static final Item METEOR_IRON = materialItem("meteor_iron", Rarity.COMMON);
    public static final Item STAR_SAND = materialItem("star_sand", Rarity.COMMON);
    public static final Item STAR_CRYSTAL = materialItem("star_crystal", Rarity.UNCOMMON);
    public static final Block ASTRAL_BLOOM = flower("astral_bloom", MobEffects.LUCK, MapColor.COLOR_PURPLE);
    public static final Block ASTRAL_GRASS = grass("astral_grass", MapColor.COLOR_PURPLE);

    // ---- 风水界 · 青玉矿脉（四矿）----

    public static final Block JADE_ORE = oreBlock("jade_ore", MapColor.COLOR_GREEN);
    public static final Block VERDANT_ORE = oreBlock("verdant_ore", MapColor.COLOR_GREEN);
    public static final Block MICA_ORE = oreBlock("mica_ore", MapColor.COLOR_LIGHT_BLUE);
    public static final Block VEIN_ORE = oreBlock("vein_ore", MapColor.COLOR_GREEN);
    public static final Item JADE_GRIT = materialItem("jade_grit", Rarity.COMMON);
    public static final Item VERDANT_GEM = materialItem("verdant_gem", Rarity.COMMON);
    public static final Item MICA = materialItem("mica", Rarity.COMMON);
    public static final Item VEIN_CRYSTAL = materialItem("vein_crystal", Rarity.UNCOMMON);
    public static final Block JADE_LILY = flower("jade_lily", MobEffects.SATURATION, MapColor.COLOR_GREEN);
    public static final Block JADE_GRASS = grass("jade_grass", MapColor.COLOR_GREEN);

    // ---- 六爻界 · 爻纹矿脉（四矿）----

    public static final Block YAO_ORE = oreBlock("yao_ore", MapColor.COLOR_GRAY);
    public static final Block INK_IRON_ORE = oreBlock("ink_iron_ore", MapColor.COLOR_GRAY);
    public static final Block ONYX_ORE = oreBlock("onyx_ore", MapColor.COLOR_BLACK);
    public static final Block ORACLE_ORE = oreBlock("oracle_ore", MapColor.COLOR_GRAY);
    public static final Item YAO_SHARD = materialItem("yao_shard", Rarity.COMMON);
    public static final Item INK_IRON = materialItem("ink_iron", Rarity.COMMON);
    public static final Item ONYX = materialItem("onyx", Rarity.COMMON);
    public static final Item ORACLE_BONE = materialItem("oracle_bone", Rarity.UNCOMMON);
    public static final Block YAO_BLOOM = flower("yao_bloom", MobEffects.NIGHT_VISION, MapColor.COLOR_RED);
    public static final Block YAO_GRASS = grass("yao_grass", MapColor.COLOR_GRAY);

    // ---- 第 16 轮 · 紫微界矿脉（四矿）----

    public static final Block ZIWEI_CRYSTAL_ORE = oreBlock("ziwei_crystal_ore", MapColor.COLOR_PURPLE);
    public static final Block DOU_ORE = oreBlock("dou_ore", MapColor.COLOR_BLUE);
    public static final Block JI_ORE = oreBlock("ji_ore", MapColor.COLOR_YELLOW);
    public static final Block YUAN_ORE = oreBlock("yuan_ore", MapColor.COLOR_MAGENTA);
    public static final Item ZIWEI_CRYSTAL = materialItem("ziwei_crystal", Rarity.COMMON);
    public static final Item ZIWEI_ESSENCE = materialItem("ziwei_essence", Rarity.UNCOMMON);
    public static final Block ZIWEI_BLOOM = flower("ziwei_bloom", MobEffects.NIGHT_VISION, MapColor.COLOR_PURPLE);
    public static final Block ZIWEI_GRASS = grass("ziwei_grass", MapColor.COLOR_PURPLE);

    // ---- 第 16 轮 · 六壬界矿脉（四矿）----

    public static final Block REN_ORE = oreBlock("ren_ore", MapColor.COLOR_CYAN);
    public static final Block GUI_ORE = oreBlock("gui_ore", MapColor.COLOR_LIGHT_BLUE);
    public static final Block SHE_ORE = oreBlock("she_ore", MapColor.COLOR_RED);
    public static final Block XUAN_ORE = oreBlock("xuan_ore", MapColor.COLOR_BLACK);
    public static final Item LIUREN_CRYSTAL = materialItem("liuren_crystal", Rarity.COMMON);
    public static final Item LIUREN_ESSENCE = materialItem("liuren_essence", Rarity.UNCOMMON);
    public static final Block LIUREN_LILY = flower("liuren_lily", MobEffects.WATER_BREATHING, MapColor.COLOR_CYAN);
    public static final Block LIUREN_GRASS = grass("liuren_grass", MapColor.COLOR_CYAN);

    // ---- 第 16 轮 · 奇门界矿脉（四矿）----

    public static final Block MEN_ORE = oreBlock("men_ore", MapColor.COLOR_ORANGE);
    public static final Block XING_ORE = oreBlock("xing_ore", MapColor.COLOR_YELLOW);
    public static final Block DUN_ORE = oreBlock("dun_ore", MapColor.COLOR_RED);
    public static final Block JIA_ORE = oreBlock("jia_ore", MapColor.COLOR_LIGHT_GRAY);
    public static final Item QIMEN_CRYSTAL = materialItem("qimen_crystal", Rarity.COMMON);
    public static final Item QIMEN_ESSENCE = materialItem("qimen_essence", Rarity.UNCOMMON);
    public static final Block QIMEN_BLOOM = flower("qimen_bloom", MobEffects.STRENGTH, MapColor.COLOR_ORANGE);
    public static final Block QIMEN_GRASS = grass("qimen_grass", MapColor.COLOR_YELLOW);

    // ---- 第 19 轮 · 太乙界矿脉（四矿）----

    public static final Block TAIYI_ORE = oreBlock("taiyi_ore", MapColor.COLOR_LIGHT_BLUE);
    public static final Block WENCHANG_ORE = oreBlock("wenchang_ore", MapColor.COLOR_CYAN);
    public static final Block SHIJI_ORE = oreBlock("shiji_ore", MapColor.COLOR_BLUE);
    public static final Block ZHUKE_ORE = oreBlock("zhuke_ore", MapColor.COLOR_YELLOW);
    public static final Item TAIYI_CRYSTAL = materialItem("taiyi_crystal", Rarity.COMMON);
    public static final Item TAIYI_ESSENCE = materialItem("taiyi_essence", Rarity.UNCOMMON);
    public static final Item TAIYI_SHARD = materialItem("taiyi_shard", Rarity.COMMON);
    public static final Item TAIYI_GRAIN = materialItem("taiyi_grain", Rarity.COMMON);
    public static final Block TAIYI_BLOOM = flower("taiyi_bloom", MobEffects.NIGHT_VISION, MapColor.COLOR_LIGHT_BLUE);
    public static final Block TAIYI_GRASS = grass("taiyi_grass", MapColor.COLOR_LIGHT_BLUE);

    // ==================================================================== 维度林木
    //
    // 每个界域三种树：原木、树叶、树苗。树苗可以种下去长大 ——
    // 长的就是界域自己的树，与原生树苗走同一条生长管线。

    // ---- 命理界林木（星辉 / 陨枝 / 月华）----

    public static final Block ASTRAL_WOOD_LOG = woodLog("astral_wood_log", MapColor.COLOR_PURPLE);
    public static final Block ASTRAL_WOOD_LEAVES =
            leaves("astral_wood_leaves", MapColor.COLOR_PURPLE, ParticleTypes.END_ROD);
    public static final Block ASTRAL_WOOD_SAPLING = sapling("astral_wood", MapColor.COLOR_PURPLE);

    public static final Block METEOR_WOOD_LOG = woodLog("meteor_wood_log", MapColor.COLOR_BLUE);
    public static final Block METEOR_WOOD_LEAVES =
            leaves("meteor_wood_leaves", MapColor.COLOR_BLUE, ParticleTypes.END_ROD);
    public static final Block METEOR_WOOD_SAPLING = sapling("meteor_wood", MapColor.COLOR_BLUE);

    public static final Block MOON_WOOD_LOG = woodLog("moon_wood_log", MapColor.COLOR_LIGHT_BLUE);
    public static final Block MOON_WOOD_LEAVES =
            leaves("moon_wood_leaves", MapColor.COLOR_LIGHT_BLUE, ParticleTypes.END_ROD);
    public static final Block MOON_WOOD_SAPLING = sapling("moon_wood", MapColor.COLOR_LIGHT_BLUE);

    // ---- 风水界林木（青玉 / 云松 / 灵桃）----

    public static final Block JADE_WOOD_LOG = woodLog("jade_wood_log", MapColor.COLOR_GREEN);
    public static final Block JADE_WOOD_LEAVES =
            leaves("jade_wood_leaves", MapColor.COLOR_GREEN, ParticleTypes.CHERRY_LEAVES);
    public static final Block JADE_WOOD_SAPLING = sapling("jade_wood", MapColor.COLOR_GREEN);

    public static final Block CLOUD_WOOD_LOG = woodLog("cloud_wood_log", MapColor.COLOR_CYAN);
    public static final Block CLOUD_WOOD_LEAVES =
            leaves("cloud_wood_leaves", MapColor.COLOR_CYAN, ParticleTypes.CHERRY_LEAVES);
    public static final Block CLOUD_WOOD_SAPLING = sapling("cloud_wood", MapColor.COLOR_CYAN);

    public static final Block PEACH_WOOD_LOG = woodLog("peach_wood_log", MapColor.COLOR_PINK);
    public static final Block PEACH_WOOD_LEAVES =
            leaves("peach_wood_leaves", MapColor.COLOR_PINK, ParticleTypes.CHERRY_LEAVES);
    public static final Block PEACH_WOOD_SAPLING = sapling("peach_wood", MapColor.COLOR_PINK);

    // ---- 六爻界林木（爻纹 / 玄墨 / 幽玄）----

    public static final Block HEXA_WOOD_LOG = woodLog("hexa_wood_log", MapColor.COLOR_GRAY);
    public static final Block HEXA_WOOD_LEAVES =
            leaves("hexa_wood_leaves", MapColor.COLOR_GRAY, ParticleTypes.ASH);
    public static final Block HEXA_WOOD_SAPLING = sapling("hexa_wood", MapColor.COLOR_GRAY);

    public static final Block INK_WOOD_LOG = woodLog("ink_wood_log", MapColor.COLOR_BLACK);
    public static final Block INK_WOOD_LEAVES =
            leaves("ink_wood_leaves", MapColor.COLOR_BLACK, ParticleTypes.ASH);
    public static final Block INK_WOOD_SAPLING = sapling("ink_wood", MapColor.COLOR_BLACK);

    public static final Block MYSTERY_WOOD_LOG = woodLog("mystery_wood_log", MapColor.COLOR_BLUE);
    public static final Block MYSTERY_WOOD_LEAVES =
            leaves("mystery_wood_leaves", MapColor.COLOR_BLUE, ParticleTypes.ASH);
    public static final Block MYSTERY_WOOD_SAPLING = sapling("mystery_wood", MapColor.COLOR_BLUE);

    // ---- 第 16 轮 · 紫微界林木（紫微 / 斗枢 / 天机）----

    public static final Block ZIWEI_WOOD_LOG = woodLog("ziwei_wood_log", MapColor.COLOR_PURPLE);
    public static final Block ZIWEI_WOOD_LEAVES =
            leaves("ziwei_wood_leaves", MapColor.COLOR_PURPLE, ParticleTypes.END_ROD);
    public static final Block ZIWEI_WOOD_SAPLING = sapling("ziwei_wood", MapColor.COLOR_PURPLE);

    public static final Block DOU_WOOD_LOG = woodLog("dou_wood_log", MapColor.COLOR_BLUE);
    public static final Block DOU_WOOD_LEAVES =
            leaves("dou_wood_leaves", MapColor.COLOR_BLUE, ParticleTypes.END_ROD);
    public static final Block DOU_WOOD_SAPLING = sapling("dou_wood", MapColor.COLOR_BLUE);

    public static final Block TIANJI_WOOD_LOG = woodLog("tianji_wood_log", MapColor.COLOR_MAGENTA);
    public static final Block TIANJI_WOOD_LEAVES =
            leaves("tianji_wood_leaves", MapColor.COLOR_MAGENTA, ParticleTypes.END_ROD);
    public static final Block TIANJI_WOOD_SAPLING = sapling("tianji_wood", MapColor.COLOR_MAGENTA);

    // ---- 第 16 轮 · 六壬界林木（壬水 / 贵人 / 螣蛇）----

    public static final Block REN_WOOD_LOG = woodLog("ren_wood_log", MapColor.COLOR_CYAN);
    public static final Block REN_WOOD_LEAVES =
            leaves("ren_wood_leaves", MapColor.COLOR_CYAN, ParticleTypes.CHERRY_LEAVES);
    public static final Block REN_WOOD_SAPLING = sapling("ren_wood", MapColor.COLOR_CYAN);

    public static final Block GUI_WOOD_LOG = woodLog("gui_wood_log", MapColor.COLOR_LIGHT_BLUE);
    public static final Block GUI_WOOD_LEAVES =
            leaves("gui_wood_leaves", MapColor.COLOR_LIGHT_BLUE, ParticleTypes.CHERRY_LEAVES);
    public static final Block GUI_WOOD_SAPLING = sapling("gui_wood", MapColor.COLOR_LIGHT_BLUE);

    public static final Block SHE_WOOD_LOG = woodLog("she_wood_log", MapColor.COLOR_RED);
    public static final Block SHE_WOOD_LEAVES =
            leaves("she_wood_leaves", MapColor.COLOR_RED, ParticleTypes.CHERRY_LEAVES);
    public static final Block SHE_WOOD_SAPLING = sapling("she_wood", MapColor.COLOR_RED);

    // ---- 第 16 轮 · 奇门界林木（八门 / 九星 / 遁甲）----

    public static final Block MEN_WOOD_LOG = woodLog("men_wood_log", MapColor.COLOR_ORANGE);
    public static final Block MEN_WOOD_LEAVES =
            leaves("men_wood_leaves", MapColor.COLOR_ORANGE, ParticleTypes.ASH);
    public static final Block MEN_WOOD_SAPLING = sapling("men_wood", MapColor.COLOR_ORANGE);

    public static final Block XING_WOOD_LOG = woodLog("xing_wood_log", MapColor.COLOR_YELLOW);
    public static final Block XING_WOOD_LEAVES =
            leaves("xing_wood_leaves", MapColor.COLOR_YELLOW, ParticleTypes.ASH);
    public static final Block XING_WOOD_SAPLING = sapling("xing_wood", MapColor.COLOR_YELLOW);

    public static final Block DUN_WOOD_LOG = woodLog("dun_wood_log", MapColor.COLOR_RED);
    public static final Block DUN_WOOD_LEAVES =
            leaves("dun_wood_leaves", MapColor.COLOR_RED, ParticleTypes.ASH);
    public static final Block DUN_WOOD_SAPLING = sapling("dun_wood", MapColor.COLOR_RED);

    // ---- 第 19 轮 · 太乙界林木（太乙 / 文昌 / 计神）----

    public static final Block TAIYI_WOOD_LOG = woodLog("taiyi_wood_log", MapColor.COLOR_LIGHT_BLUE);
    public static final Block TAIYI_WOOD_LEAVES =
            leaves("taiyi_wood_leaves", MapColor.COLOR_LIGHT_BLUE, ParticleTypes.END_ROD);
    public static final Block TAIYI_WOOD_SAPLING = sapling("taiyi_wood", MapColor.COLOR_LIGHT_BLUE);

    public static final Block WENCHANG_WOOD_LOG = woodLog("wenchang_wood_log", MapColor.COLOR_CYAN);
    public static final Block WENCHANG_WOOD_LEAVES =
            leaves("wenchang_wood_leaves", MapColor.COLOR_CYAN, ParticleTypes.END_ROD);
    public static final Block WENCHANG_WOOD_SAPLING = sapling("wenchang_wood", MapColor.COLOR_CYAN);

    public static final Block JISHEN_WOOD_LOG = woodLog("jishen_wood_log", MapColor.COLOR_BLUE);
    public static final Block JISHEN_WOOD_LEAVES =
            leaves("jishen_wood_leaves", MapColor.COLOR_BLUE, ParticleTypes.END_ROD);
    public static final Block JISHEN_WOOD_SAPLING = sapling("jishen_wood", MapColor.COLOR_BLUE);

    // ==================================================================== 维度液体
    //
    // 三个维度各有一片自己的水：星辉露、玉髓浆、玄墨水。
    // 舀一桶带回去，就是「到此一游」的凭证。

    public static final Block ASTRAL_DEW_BLOCK =
            liquidBlock("astral_dew", YsFluids.ASTRAL_DEW, MapColor.COLOR_PURPLE, 12, 1.0F);
    public static final Block JADE_ICHOR_BLOCK =
            liquidBlock("jade_ichor", YsFluids.JADE_ICHOR, MapColor.COLOR_GREEN, 8, 1.15F);
    public static final Block YAO_INK_BLOCK =
            liquidBlock("yao_ink", YsFluids.YAO_INK, MapColor.COLOR_BLACK, 3, 0.7F);

    public static final Item ASTRAL_DEW_BUCKET = bucket("astral_dew_bucket", YsFluids.ASTRAL_DEW);
    public static final Item JADE_ICHOR_BUCKET = bucket("jade_ichor_bucket", YsFluids.JADE_ICHOR);
    public static final Item YAO_INK_BUCKET = bucket("yao_ink_bucket", YsFluids.YAO_INK);

    // ---- 第 16 轮：三术界的液体 ----

    public static final Block ZIWEI_AMBROSIA_BLOCK =
            liquidBlock("ziwei_ambrosia", YsFluids.ZIWEI_AMBROSIA, MapColor.COLOR_PURPLE, 11, 1.0F);
    public static final Block LIUREN_WATER_BLOCK =
            liquidBlock("liuren_water", YsFluids.LIUREN_WATER, MapColor.COLOR_CYAN, 7, 1.1F);
    public static final Block QIMEN_MERCURY_BLOCK =
            liquidBlock("qimen_mercury", YsFluids.QIMEN_MERCURY, MapColor.COLOR_ORANGE, 9, 1.25F);

    public static final Item ZIWEI_AMBROSIA_BUCKET =
            bucket("ziwei_ambrosia_bucket", YsFluids.ZIWEI_AMBROSIA);
    public static final Item LIUREN_WATER_BUCKET =
            bucket("liuren_water_bucket", YsFluids.LIUREN_WATER);
    public static final Item QIMEN_MERCURY_BUCKET =
            bucket("qimen_mercury_bucket", YsFluids.QIMEN_MERCURY);

    /** 第 19 轮：太乙玄泉。 */
    public static final Block TAIYI_SPRING_BLOCK =
            liquidBlock("taiyi_spring", YsFluids.TAIYI_SPRING, MapColor.COLOR_LIGHT_BLUE, 10, 1.0F);
    public static final Item TAIYI_SPRING_BUCKET =
            bucket("taiyi_spring_bucket", YsFluids.TAIYI_SPRING);

    // ---- 第 21 轮：七界凶液 —— 与界域液体同源，但入液即伤 ----

    public static final Block ASTRAL_SLAG_BLOCK =
            liquidBlock("astral_slag", YsFluids.ASTRAL_SLAG, MapColor.COLOR_ORANGE, 9, 0.6F);
    public static final Item ASTRAL_SLAG_BUCKET = bucket("astral_slag_bucket", YsFluids.ASTRAL_SLAG);

    public static final Block JADE_ACID_BLOCK =
            liquidBlock("jade_acid", YsFluids.JADE_ACID, MapColor.COLOR_GREEN, 6, 0.9F);
    public static final Item JADE_ACID_BUCKET = bucket("jade_acid_bucket", YsFluids.JADE_ACID);

    public static final Block YAO_MIASMA_BLOCK =
            liquidBlock("yao_miasma", YsFluids.YAO_MIASMA, MapColor.COLOR_MAGENTA, 2, 0.5F);
    public static final Item YAO_MIASMA_BUCKET = bucket("yao_miasma_bucket", YsFluids.YAO_MIASMA);

    public static final Block ZIWEI_BANE_BLOCK =
            liquidBlock("ziwei_bane", YsFluids.ZIWEI_BANE, MapColor.COLOR_MAGENTA, 5, 0.8F);
    public static final Item ZIWEI_BANE_BUCKET = bucket("ziwei_bane_bucket", YsFluids.ZIWEI_BANE);

    public static final Block LIUREN_ABYSS_BLOCK =
            liquidBlock("liuren_abyss", YsFluids.LIUREN_ABYSS, MapColor.COLOR_BLUE, 0, 0.7F);
    public static final Item LIUREN_ABYSS_BUCKET =
            bucket("liuren_abyss_bucket", YsFluids.LIUREN_ABYSS);

    public static final Block QIMEN_HELLFIRE_BLOCK =
            liquidBlock("qimen_hellfire", YsFluids.QIMEN_HELLFIRE, MapColor.COLOR_RED, 13, 0.7F);
    public static final Item QIMEN_HELLFIRE_BUCKET =
            bucket("qimen_hellfire_bucket", YsFluids.QIMEN_HELLFIRE);

    public static final Block TAIYI_FROST_BLOCK =
            liquidBlock("taiyi_frost", YsFluids.TAIYI_FROST, MapColor.COLOR_BLUE, 4, 0.9F);
    public static final Item TAIYI_FROST_BUCKET = bucket("taiyi_frost_bucket", YsFluids.TAIYI_FROST);

    // ---- 第 21 轮：矿物原矿 —— 挖矿先得原矿，回炉才有成品 ----

    public static final Item ASTRAL_RAW = materialItem("astral_raw", Rarity.COMMON);
    public static final Item JADE_RAW = materialItem("jade_raw", Rarity.COMMON);
    public static final Item YAO_RAW = materialItem("yao_raw", Rarity.COMMON);
    public static final Item METEOR_RAW = materialItem("meteor_raw", Rarity.COMMON);
    public static final Item INK_IRON_RAW = materialItem("ink_iron_raw", Rarity.COMMON);
    public static final Item MICA_RAW = materialItem("mica_raw", Rarity.COMMON);
    public static final Item ONYX_RAW = materialItem("onyx_raw", Rarity.COMMON);
    public static final Item STAR_SAND_RAW = materialItem("star_sand_raw", Rarity.COMMON);
    public static final Item VEIN_RAW = materialItem("vein_raw", Rarity.COMMON);
    public static final Item VERDANT_RAW = materialItem("verdant_raw", Rarity.COMMON);
    public static final Item ORACLE_RAW = materialItem("oracle_raw", Rarity.COMMON);
    public static final Item CRYSTAL_RAW = materialItem("crystal_raw", Rarity.COMMON);
    public static final Item ZIWEI_CRYSTAL_RAW = materialItem("ziwei_crystal_raw", Rarity.COMMON);
    public static final Item REN_RAW = materialItem("ren_raw", Rarity.COMMON);
    public static final Item GUI_RAW = materialItem("gui_raw", Rarity.COMMON);
    public static final Item SHE_RAW = materialItem("she_raw", Rarity.COMMON);
    public static final Item XUAN_RAW = materialItem("xuan_raw", Rarity.COMMON);
    public static final Item MEN_RAW = materialItem("men_raw", Rarity.COMMON);
    public static final Item XING_RAW = materialItem("xing_raw", Rarity.COMMON);
    public static final Item DUN_RAW = materialItem("dun_raw", Rarity.COMMON);
    public static final Item JIA_RAW = materialItem("jia_raw", Rarity.COMMON);
    public static final Item TAIYI_RAW = materialItem("taiyi_raw", Rarity.COMMON);
    public static final Item WENCHANG_RAW = materialItem("wenchang_raw", Rarity.COMMON);
    public static final Item SHIJI_RAW = materialItem("shiji_raw", Rarity.COMMON);
    public static final Item ZHUKE_RAW = materialItem("zhuke_raw", Rarity.COMMON);

    // ==================================================================== 维度门户
    //
    // 三座传送门：星门、玉门、玄门。用灵引立门，走进去就出发。
    // 门本身没有物品形式 —— 钥匙才是你该拿在手里的东西。

    public static final YsPortalContent.RealmGateBlock ASTRAL_GATE = gate("astral_gate",
            FATE_REALM_KEY, "命理界", "星门", MapColor.COLOR_PURPLE, YsPortalContent.astralParticle());
    public static final YsPortalContent.RealmGateBlock JADE_GATE = gate("jade_gate",
            FENGSHUI_REALM_KEY, "风水界", "玉门", MapColor.COLOR_GREEN, YsPortalContent.jadeParticle());
    public static final YsPortalContent.RealmGateBlock YAO_GATE = gate("yao_gate",
            LIUYAO_REALM_KEY, "六爻界", "玄门", MapColor.COLOR_GRAY, YsPortalContent.inkParticle());

    public static final Item ASTRAL_KEY = item("astral_key",
            new YsPortalContent.RealmKeyItem(ASTRAL_GATE, ASTRAL_BRICKS, "命理界", "星门",
                    YsPortalContent.keyProps("astral_key")));
    public static final Item JADE_KEY = item("jade_key",
            new YsPortalContent.RealmKeyItem(JADE_GATE, JADE_BRICKS, "风水界", "玉门",
                    YsPortalContent.keyProps("jade_key")));
    public static final Item YAO_KEY = item("yao_key",
            new YsPortalContent.RealmKeyItem(YAO_GATE, YAO_BRICKS, "六爻界", "玄门",
                    YsPortalContent.keyProps("yao_key")));

    // ---- 第 16 轮：三术界的门 ----

    public static final YsPortalContent.RealmGateBlock ZIWEI_GATE = gate("ziwei_gate",
            ZIWEI_REALM_KEY, "紫微界", "紫微门", MapColor.COLOR_PURPLE, YsPortalContent.ziweiParticle());
    public static final YsPortalContent.RealmGateBlock LIUREN_GATE = gate("liuren_gate",
            LIUREN_REALM_KEY, "六壬界", "壬水门", MapColor.COLOR_CYAN, YsPortalContent.liurenParticle());
    public static final YsPortalContent.RealmGateBlock QIMEN_GATE = gate("qimen_gate",
            QIMEN_REALM_KEY, "奇门界", "八门", MapColor.COLOR_ORANGE, YsPortalContent.qimenParticle());

    public static final Item ZIWEI_KEY = item("ziwei_key",
            new YsPortalContent.RealmKeyItem(ZIWEI_GATE, ZIWEI_BRICKS, "紫微界", "紫微门",
                    YsPortalContent.keyProps("ziwei_key")));
    public static final Item LIUREN_KEY = item("liuren_key",
            new YsPortalContent.RealmKeyItem(LIUREN_GATE, LIUREN_BRICKS, "六壬界", "壬水门",
                    YsPortalContent.keyProps("liuren_key")));
    public static final Item QIMEN_KEY = item("qimen_key",
            new YsPortalContent.RealmKeyItem(QIMEN_GATE, QIMEN_BRICKS, "奇门界", "八门",
                    YsPortalContent.keyProps("qimen_key")));

    // ---- 第 19 轮：太乙门 ----

    public static final YsPortalContent.RealmGateBlock TAIYI_GATE = gate("taiyi_gate",
            TAIYI_REALM_KEY, "太乙界", "太乙门", MapColor.COLOR_LIGHT_BLUE,
            YsPortalContent.taiyiParticle());
    public static final Item TAIYI_KEY = item("taiyi_key",
            new YsPortalContent.RealmKeyItem(TAIYI_GATE, TAIYI_BRICKS, "太乙界", "太乙门",
                    YsPortalContent.keyProps("taiyi_key")));

    // ---- 第 20 轮：相面 · 阵盘 · 镇物 ----

    /** 相面镜：照村民面相，贵相可招募（走好感度的官方通道）。 */
    public static final Item FACE_MIRROR = item("face_mirror",
            new Item(props("face_mirror").stacksTo(1).rarity(Rarity.UNCOMMON)));
    /** 八卦阵盘：阵内抗性回血，邪祟受抑。 */
    public static final Item BAGUA_ARRAY_DISC = item("bagua_array_disc",
            new YsItemContent.FormationDisc(
                    props("bagua_array_disc").stacksTo(4).rarity(Rarity.UNCOMMON), "bagua"));
    /** 北斗阵盘：阵内夜视疾行。 */
    public static final Item BEIDOU_ARRAY_DISC = item("beidou_array_disc",
            new YsItemContent.FormationDisc(
                    props("beidou_array_disc").stacksTo(4).rarity(Rarity.UNCOMMON), "beidou"));
    /** 招财阵盘：阵内幸运（招财一条归它管）。 */
    public static final Item ZHAOCAI_ARRAY_DISC = item("zhaocai_array_disc",
            new YsItemContent.FormationDisc(
                    props("zhaocai_array_disc").stacksTo(4).rarity(Rarity.UNCOMMON), "zhaocai"));
    /** 镇宅石：镇宅避邪，半径内邪祟受抑。 */
    public static final Block ZHENZHAI_STONE = block("zhenzhai_stone", Block::new,
            BlockBehaviour.Properties.of().setId(YsReg.blockKey("zhenzhai_stone"))
                    .mapColor(MapColor.COLOR_GRAY).strength(3.0F, 9.0F)
                    .requiresCorrectToolForDrops().sound(SoundType.DEEPSLATE));
    /** 化煞碑：化煞，半径内凶地上抬一档。 */
    public static final Block HUASHA_STONE = block("huasha_stone", Block::new,
            BlockBehaviour.Properties.of().setId(YsReg.blockKey("huasha_stone"))
                    .mapColor(MapColor.COLOR_BLACK).strength(3.0F, 9.0F)
                    .requiresCorrectToolForDrops().sound(SoundType.DEEPSLATE));

    // ==================================================================== 灵兽刷怪蛋

    public static final Item FATE_WISP_SPAWN_EGG = spawnEgg("fate_wisp_spawn_egg", YsMobs.FATE_WISP);
    public static final Item STAR_WATCHER_SPAWN_EGG = spawnEgg("star_watcher_spawn_egg", YsMobs.STAR_WATCHER);
    public static final Item STAR_FOX_SPAWN_EGG = spawnEgg("star_fox_spawn_egg", YsMobs.STAR_FOX);
    public static final Item STAR_SLIME_SPAWN_EGG = spawnEgg("star_slime_spawn_egg", YsMobs.STAR_SLIME);
    public static final Item JADE_SPIRIT_SPAWN_EGG = spawnEgg("jade_spirit_spawn_egg", YsMobs.JADE_SPIRIT);
    public static final Item JADE_RABBIT_SPAWN_EGG = spawnEgg("jade_rabbit_spawn_egg", YsMobs.JADE_RABBIT);
    public static final Item JADE_GOAT_SPAWN_EGG = spawnEgg("jade_goat_spawn_egg", YsMobs.JADE_GOAT);
    public static final Item VEIN_TURTLE_SPAWN_EGG = spawnEgg("vein_turtle_spawn_egg", YsMobs.VEIN_TURTLE);
    public static final Item STONE_GUARDIAN_SPAWN_EGG = spawnEgg("stone_guardian_spawn_egg", YsMobs.STONE_GUARDIAN);
    public static final Item INK_WOLF_SPAWN_EGG = spawnEgg("ink_wolf_spawn_egg", YsMobs.INK_WOLF);
    public static final Item INK_BAT_SPAWN_EGG = spawnEgg("ink_bat_spawn_egg", YsMobs.INK_BAT);
    public static final Item HEX_PHANTOM_SPAWN_EGG = spawnEgg("hex_phantom_spawn_egg", YsMobs.HEX_PHANTOM);

    // ---- 第 16 轮：三术界十二灵兽 ----

    public static final Item ZIWEI_STAR_SPAWN_EGG = spawnEgg("ziwei_star_spawn_egg", YsMobs.ZIWEI_STAR);
    public static final Item DOU_BEAST_SPAWN_EGG = spawnEgg("dou_beast_spawn_egg", YsMobs.DOU_BEAST);
    public static final Item TIANJI_FOX_SPAWN_EGG = spawnEgg("tianji_fox_spawn_egg", YsMobs.TIANJI_FOX);
    public static final Item ZIWEI_MOTH_SPAWN_EGG = spawnEgg("ziwei_moth_spawn_egg", YsMobs.ZIWEI_MOTH);

    public static final Item REN_SERPENT_SPAWN_EGG = spawnEgg("ren_serpent_spawn_egg", YsMobs.REN_SERPENT);
    public static final Item GUI_CRANE_SPAWN_EGG = spawnEgg("gui_crane_spawn_egg", YsMobs.GUI_CRANE);
    public static final Item SHE_HOUND_SPAWN_EGG = spawnEgg("she_hound_spawn_egg", YsMobs.SHE_HOUND);
    public static final Item XUAN_TURTLE_SPAWN_EGG = spawnEgg("xuan_turtle_spawn_egg", YsMobs.XUAN_TURTLE);

    public static final Item MEN_GUARD_SPAWN_EGG = spawnEgg("men_guard_spawn_egg", YsMobs.MEN_GUARD);
    public static final Item XING_FOX_SPAWN_EGG = spawnEgg("xing_fox_spawn_egg", YsMobs.XING_FOX);
    public static final Item DUN_RAT_SPAWN_EGG = spawnEgg("dun_rat_spawn_egg", YsMobs.DUN_RAT);
    public static final Item JIA_BIRD_SPAWN_EGG = spawnEgg("jia_bird_spawn_egg", YsMobs.JIA_BIRD);

    // ==================================================================== 木板三件套（第 22 轮）

    /** 各树种 → 木板。 */
    public static final Map<String, Block> WOOD_PLANKS = new LinkedHashMap<>();
    /** 各树种 → 台阶。 */
    public static final Map<String, Block> WOOD_SLABS = new LinkedHashMap<>();
    /** 各树种 → 楼梯。 */
    public static final Map<String, Block> WOOD_STAIRS = new LinkedHashMap<>();

    /** 一组木三件套：木板 + 台阶 + 楼梯，返回木板。 */
    private static Block plankSet(String species, MapColor color) {
        Block planks = block(species + "_planks", Block::new,
                blockProps(species + "_planks", color).strength(2.0F, 3.0F)
                        .sound(SoundType.WOOD).ignitedByLava());
        Block slab = block(species + "_slab", SlabBlock::new,
                blockProps(species + "_slab", color).strength(2.0F, 3.0F)
                        .sound(SoundType.WOOD).ignitedByLava());
        Block stairs = block(species + "_stairs",
                p -> new YsBlockContent.YsStairs(planks.defaultBlockState(), p),
                blockProps(species + "_stairs", color).strength(2.0F, 3.0F)
                        .sound(SoundType.WOOD).ignitedByLava());
        WOOD_PLANKS.put(species, planks);
        WOOD_SLABS.put(species, slab);
        WOOD_STAIRS.put(species, stairs);
        return planks;
    }

    /** 二十一种树的三件套，配色与原木一致。 */
    public static final Block ASTRAL_WOOD_PLANKS = plankSet("astral_wood", MapColor.COLOR_PURPLE);
    public static final Block METEOR_WOOD_PLANKS = plankSet("meteor_wood", MapColor.COLOR_BLUE);
    public static final Block MOON_WOOD_PLANKS = plankSet("moon_wood", MapColor.COLOR_LIGHT_BLUE);
    public static final Block JADE_WOOD_PLANKS = plankSet("jade_wood", MapColor.COLOR_GREEN);
    public static final Block CLOUD_WOOD_PLANKS = plankSet("cloud_wood", MapColor.COLOR_CYAN);
    public static final Block PEACH_WOOD_PLANKS = plankSet("peach_wood", MapColor.COLOR_PINK);
    public static final Block HEXA_WOOD_PLANKS = plankSet("hexa_wood", MapColor.COLOR_GRAY);
    public static final Block INK_WOOD_PLANKS = plankSet("ink_wood", MapColor.COLOR_BLACK);
    public static final Block MYSTERY_WOOD_PLANKS = plankSet("mystery_wood", MapColor.COLOR_BLUE);
    public static final Block ZIWEI_WOOD_PLANKS = plankSet("ziwei_wood", MapColor.COLOR_PURPLE);
    public static final Block DOU_WOOD_PLANKS = plankSet("dou_wood", MapColor.COLOR_BLUE);
    public static final Block TIANJI_WOOD_PLANKS = plankSet("tianji_wood", MapColor.COLOR_MAGENTA);
    public static final Block REN_WOOD_PLANKS = plankSet("ren_wood", MapColor.COLOR_CYAN);
    public static final Block GUI_WOOD_PLANKS = plankSet("gui_wood", MapColor.COLOR_LIGHT_BLUE);
    public static final Block SHE_WOOD_PLANKS = plankSet("she_wood", MapColor.COLOR_RED);
    public static final Block MEN_WOOD_PLANKS = plankSet("men_wood", MapColor.COLOR_ORANGE);
    public static final Block XING_WOOD_PLANKS = plankSet("xing_wood", MapColor.COLOR_YELLOW);
    public static final Block TAIYI_WOOD_PLANKS = plankSet("taiyi_wood", MapColor.COLOR_LIGHT_BLUE);
    public static final Block WENCHANG_WOOD_PLANKS = plankSet("wenchang_wood", MapColor.COLOR_CYAN);
    public static final Block JISHEN_WOOD_PLANKS = plankSet("jishen_wood", MapColor.COLOR_BLUE);
    public static final Block DUN_WOOD_PLANKS = plankSet("dun_wood", MapColor.COLOR_RED);

    // ==================================================================== 木船（第 23 轮）

    /** 各树种 → 船物品。 */
    public static final Map<String, Item> WOOD_BOATS = new LinkedHashMap<>();
    /** 各树种 → 船实体类型（客户端渲染注册用）。 */
    public static final Map<String, EntityType<Boat>> WOOD_BOAT_TYPES = new LinkedHashMap<>();

    /**
     * 一条船：船实体 + 船物品。
     *
     * <p>掉落物走 {@code Supplier<Item>} 惰性解析 —— 原版也是这么绕开
     * 「实体要物品做掉落物、物品要实体做放置」这对循环引用的。
     */
    private static Item boatSet(String species) {
        EntityType<Boat> type = EntityType.Builder
                .<Boat>of((t, level) -> new Boat(t, level, () -> WOOD_BOATS.get(species)),
                        MobCategory.MISC)
                .sized(1.375F, 0.5625F).eyeHeight(0.4375F).clientTrackingRange(10)
                .build(ResourceKey.create(Registries.ENTITY_TYPE, YishuMod.id(species + "_boat")));
        Registry.register(BuiltInRegistries.ENTITY_TYPE, YishuMod.id(species + "_boat"), type);
        WOOD_BOAT_TYPES.put(species, type);
        Item boat = item(species + "_boat",
                new BoatItem(type, props(species + "_boat").stacksTo(1)));
        WOOD_BOATS.put(species, boat);
        return boat;
    }

    /** 二十一种树各一条船 —— 过河要用本界的木头，才不虚水里的煞。 */
    public static final Item ASTRAL_WOOD_BOAT = boatSet("astral_wood");
    public static final Item METEOR_WOOD_BOAT = boatSet("meteor_wood");
    public static final Item MOON_WOOD_BOAT = boatSet("moon_wood");
    public static final Item JADE_WOOD_BOAT = boatSet("jade_wood");
    public static final Item CLOUD_WOOD_BOAT = boatSet("cloud_wood");
    public static final Item PEACH_WOOD_BOAT = boatSet("peach_wood");
    public static final Item HEXA_WOOD_BOAT = boatSet("hexa_wood");
    public static final Item INK_WOOD_BOAT = boatSet("ink_wood");
    public static final Item MYSTERY_WOOD_BOAT = boatSet("mystery_wood");
    public static final Item ZIWEI_WOOD_BOAT = boatSet("ziwei_wood");
    public static final Item DOU_WOOD_BOAT = boatSet("dou_wood");
    public static final Item TIANJI_WOOD_BOAT = boatSet("tianji_wood");
    public static final Item REN_WOOD_BOAT = boatSet("ren_wood");
    public static final Item GUI_WOOD_BOAT = boatSet("gui_wood");
    public static final Item SHE_WOOD_BOAT = boatSet("she_wood");
    public static final Item MEN_WOOD_BOAT = boatSet("men_wood");
    public static final Item XING_WOOD_BOAT = boatSet("xing_wood");
    public static final Item TAIYI_WOOD_BOAT = boatSet("taiyi_wood");
    public static final Item WENCHANG_WOOD_BOAT = boatSet("wenchang_wood");
    public static final Item JISHEN_WOOD_BOAT = boatSet("jishen_wood");
    public static final Item DUN_WOOD_BOAT = boatSet("dun_wood");

    // ==================================================================== 木变体家族（第 25 轮）

    /** 各树种 → 栅栏 / 栅栏门 / 门 / 活板门 / 按钮 / 压力板 / 立牌 / 挂牌。 */
    public static final Map<String, Block> WOOD_FENCES = new LinkedHashMap<>();
    public static final Map<String, Block> WOOD_FENCE_GATES = new LinkedHashMap<>();
    public static final Map<String, Block> WOOD_DOORS = new LinkedHashMap<>();
    public static final Map<String, Block> WOOD_TRAPDOORS = new LinkedHashMap<>();
    public static final Map<String, Block> WOOD_BUTTONS = new LinkedHashMap<>();
    public static final Map<String, Block> WOOD_PLATES = new LinkedHashMap<>();
    public static final Map<String, Block> WOOD_STANDING_SIGNS = new LinkedHashMap<>();
    public static final Map<String, Block> WOOD_WALL_SIGNS = new LinkedHashMap<>();
    /** 各树种 → 告示牌物品（挂牌与立牌共用一件物品）。 */
    public static final Map<String, Item> WOOD_SIGN_ITEMS = new LinkedHashMap<>();
    /** 各树种 → 运输船物品。 */
    public static final Map<String, Item> WOOD_CHEST_BOATS = new LinkedHashMap<>();
    /** 各树种 → 运输船实体类型（客户端渲染注册用）。 */
    public static final Map<String, EntityType<ChestBoat>> WOOD_CHEST_BOAT_TYPES = new LinkedHashMap<>();
    /** 告示牌方块清单（挂到模组告示牌方块实体类型上）。 */
    private static final List<Block> SIGN_BLOCKS = new ArrayList<>();

    /**
     * 一组木变体：栅栏、栅栏门、门、活板门、按钮、压力板、立式告示牌 + 挂墙告示牌。
     *
     * <p>门/活板门/按钮/压力板/告示牌的原版构造器是 protected，用本类底部的壳子类接住；
     * BlockSetType 与 WoodType 直接复用原版 OAK —— 26.2 的 register 方法是 private
     * 没法注册自定义木种，而音效与贴图本来就不由它决定（贴图走我们自己的模型）。
     *
     * <p>门与告示牌没有默认 BlockItem（门要 DoubleHighBlockItem 才能正确放置上半格，
     * 告示牌是立牌/挂牌两方块共用一件物品），所以走 blockOnly + 手动注册物品。
     */
    private static Block variantSet(String species, MapColor color) {
        Block fence = block(species + "_fence", FenceBlock::new,
                blockProps(species + "_fence", color).strength(2.0F, 3.0F)
                        .sound(SoundType.WOOD).ignitedByLava());
        WOOD_FENCES.put(species, fence);
        Block gate = block(species + "_fence_gate", p -> new FenceGateBlock(WoodType.OAK, p),
                blockProps(species + "_fence_gate", color).strength(2.0F, 3.0F)
                        .sound(SoundType.WOOD).ignitedByLava());
        WOOD_FENCE_GATES.put(species, gate);
        Block door = blockOnly(species + "_door", p -> new YsDoor(BlockSetType.OAK, p),
                blockProps(species + "_door", color).strength(3.0F)
                        .sound(SoundType.WOOD).ignitedByLava().noOcclusion());
        WOOD_DOORS.put(species, door);
        item(species + "_door", new DoubleHighBlockItem(door, props(species + "_door").stacksTo(16)));
        Block trapdoor = block(species + "_trapdoor", p -> new YsTrapdoor(BlockSetType.OAK, p),
                blockProps(species + "_trapdoor", color).strength(3.0F)
                        .sound(SoundType.WOOD).ignitedByLava().noOcclusion());
        WOOD_TRAPDOORS.put(species, trapdoor);
        Block button = block(species + "_button", p -> new YsWoodButton(BlockSetType.OAK, 30, p),
                blockProps(species + "_button", color).noCollision().strength(0.5F)
                        .sound(SoundType.WOOD));
        WOOD_BUTTONS.put(species, button);
        Block plate = block(species + "_pressure_plate", p -> new YsWoodPlate(BlockSetType.OAK, p),
                blockProps(species + "_pressure_plate", color).noCollision().strength(0.5F)
                        .sound(SoundType.WOOD).ignitedByLava());
        WOOD_PLATES.put(species, plate);
        Block standingSign = blockOnly(species + "_standing_sign", YsStandingSign::new,
                blockProps(species + "_standing_sign", color).strength(1.0F)
                        .sound(SoundType.WOOD).ignitedByLava());
        WOOD_STANDING_SIGNS.put(species, standingSign);
        SIGN_BLOCKS.add(standingSign);
        Block wallSign = blockOnly(species + "_wall_sign", YsWallSign::new,
                blockProps(species + "_wall_sign", color).strength(1.0F)
                        .sound(SoundType.WOOD).ignitedByLava());
        WOOD_WALL_SIGNS.put(species, wallSign);
        SIGN_BLOCKS.add(wallSign);
        Item signItem = item(species + "_sign",
                new SignItem(standingSign, wallSign, props(species + "_sign").stacksTo(16)));
        WOOD_SIGN_ITEMS.put(species, signItem);
        return fence;
    }

    /** 二十一种树各一组变体（返回值是栅栏，仅作类加载锚点）。 */
    public static final Block ASTRAL_WOOD_VARIANTS = variantSet("astral_wood", MapColor.COLOR_PURPLE);
    public static final Block METEOR_WOOD_VARIANTS = variantSet("meteor_wood", MapColor.COLOR_BLUE);
    public static final Block MOON_WOOD_VARIANTS = variantSet("moon_wood", MapColor.COLOR_LIGHT_BLUE);
    public static final Block JADE_WOOD_VARIANTS = variantSet("jade_wood", MapColor.COLOR_GREEN);
    public static final Block CLOUD_WOOD_VARIANTS = variantSet("cloud_wood", MapColor.COLOR_CYAN);
    public static final Block PEACH_WOOD_VARIANTS = variantSet("peach_wood", MapColor.COLOR_PINK);
    public static final Block HEXA_WOOD_VARIANTS = variantSet("hexa_wood", MapColor.COLOR_GRAY);
    public static final Block INK_WOOD_VARIANTS = variantSet("ink_wood", MapColor.COLOR_BLACK);
    public static final Block MYSTERY_WOOD_VARIANTS = variantSet("mystery_wood", MapColor.COLOR_BLUE);
    public static final Block ZIWEI_WOOD_VARIANTS = variantSet("ziwei_wood", MapColor.COLOR_PURPLE);
    public static final Block DOU_WOOD_VARIANTS = variantSet("dou_wood", MapColor.COLOR_BLUE);
    public static final Block TIANJI_WOOD_VARIANTS = variantSet("tianji_wood", MapColor.COLOR_MAGENTA);
    public static final Block REN_WOOD_VARIANTS = variantSet("ren_wood", MapColor.COLOR_CYAN);
    public static final Block GUI_WOOD_VARIANTS = variantSet("gui_wood", MapColor.COLOR_LIGHT_BLUE);
    public static final Block SHE_WOOD_VARIANTS = variantSet("she_wood", MapColor.COLOR_RED);
    public static final Block MEN_WOOD_VARIANTS = variantSet("men_wood", MapColor.COLOR_ORANGE);
    public static final Block XING_WOOD_VARIANTS = variantSet("xing_wood", MapColor.COLOR_YELLOW);
    public static final Block TAIYI_WOOD_VARIANTS = variantSet("taiyi_wood", MapColor.COLOR_LIGHT_BLUE);
    public static final Block WENCHANG_WOOD_VARIANTS = variantSet("wenchang_wood", MapColor.COLOR_CYAN);
    public static final Block JISHEN_WOOD_VARIANTS = variantSet("jishen_wood", MapColor.COLOR_BLUE);
    public static final Block DUN_WOOD_VARIANTS = variantSet("dun_wood", MapColor.COLOR_RED);

    /** 一条运输船：实体 + 物品（掉落物走 Supplier 惰性解析，与普通船同构）。 */
    private static Item chestBoatSet(String species) {
        EntityType<ChestBoat> type = EntityType.Builder
                .<ChestBoat>of((t, level) -> new ChestBoat(t, level, () -> WOOD_CHEST_BOATS.get(species)),
                        MobCategory.MISC)
                .sized(1.375F, 0.5625F).eyeHeight(0.4375F).clientTrackingRange(10)
                .build(ResourceKey.create(Registries.ENTITY_TYPE, YishuMod.id(species + "_chest_boat")));
        Registry.register(BuiltInRegistries.ENTITY_TYPE, YishuMod.id(species + "_chest_boat"), type);
        WOOD_CHEST_BOAT_TYPES.put(species, type);
        Item boat = item(species + "_chest_boat",
                new BoatItem(type, props(species + "_chest_boat").stacksTo(1)));
        WOOD_CHEST_BOATS.put(species, boat);
        return boat;
    }

    /** 二十一种树各一条运输船。 */
    public static final Item ASTRAL_WOOD_CHEST_BOAT = chestBoatSet("astral_wood");
    public static final Item METEOR_WOOD_CHEST_BOAT = chestBoatSet("meteor_wood");
    public static final Item MOON_WOOD_CHEST_BOAT = chestBoatSet("moon_wood");
    public static final Item JADE_WOOD_CHEST_BOAT = chestBoatSet("jade_wood");
    public static final Item CLOUD_WOOD_CHEST_BOAT = chestBoatSet("cloud_wood");
    public static final Item PEACH_WOOD_CHEST_BOAT = chestBoatSet("peach_wood");
    public static final Item HEXA_WOOD_CHEST_BOAT = chestBoatSet("hexa_wood");
    public static final Item INK_WOOD_CHEST_BOAT = chestBoatSet("ink_wood");
    public static final Item MYSTERY_WOOD_CHEST_BOAT = chestBoatSet("mystery_wood");
    public static final Item ZIWEI_WOOD_CHEST_BOAT = chestBoatSet("ziwei_wood");
    public static final Item DOU_WOOD_CHEST_BOAT = chestBoatSet("dou_wood");
    public static final Item TIANJI_WOOD_CHEST_BOAT = chestBoatSet("tianji_wood");
    public static final Item REN_WOOD_CHEST_BOAT = chestBoatSet("ren_wood");
    public static final Item GUI_WOOD_CHEST_BOAT = chestBoatSet("gui_wood");
    public static final Item SHE_WOOD_CHEST_BOAT = chestBoatSet("she_wood");
    public static final Item MEN_WOOD_CHEST_BOAT = chestBoatSet("men_wood");
    public static final Item XING_WOOD_CHEST_BOAT = chestBoatSet("xing_wood");
    public static final Item TAIYI_WOOD_CHEST_BOAT = chestBoatSet("taiyi_wood");
    public static final Item WENCHANG_WOOD_CHEST_BOAT = chestBoatSet("wenchang_wood");
    public static final Item JISHEN_WOOD_CHEST_BOAT = chestBoatSet("jishen_wood");
    public static final Item DUN_WOOD_CHEST_BOAT = chestBoatSet("dun_wood");

    /** 模组告示牌方块实体类型：42 个告示牌方块共用，客户端用原版 StandingSignRenderer 画字。 */
    private static BlockEntityType<YsSignBlockEntity> signBeHolder;

    public static final BlockEntityType<YsSignBlockEntity> YISHU_SIGN_BE = makeSignBe();

    private static BlockEntityType<YsSignBlockEntity> makeSignBe() {
        BlockEntityType<YsSignBlockEntity> type = FabricBlockEntityTypeBuilder
                .create((pos, state) -> new YsSignBlockEntity(signBeHolder, pos, state),
                        SIGN_BLOCKS.toArray(new Block[0]))
                .build();
        signBeHolder = type;
        return Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, YishuMod.id("sign"), type);
    }

    // ==================================================================== 创造物品栏

    public static final ResourceKey<CreativeModeTab> TAB_KEY =
            ResourceKey.create(Registries.CREATIVE_MODE_TAB, YishuMod.id("main"));

    public static final CreativeModeTab TAB = Registry.register(
            BuiltInRegistries.CREATIVE_MODE_TAB,
            YishuMod.id("main"),
            net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.yishu.main"))
                    .icon(() -> new ItemStack(FATE_SLIP))
                    .displayItems((parameters, output) -> {
                        // 一、命理
                        output.accept(FATE_SLIP);
                        output.accept(PEEK_CHARM);
                        output.accept(FATE_BOOK);
                        // 二、风水
                        output.accept(FENG_SHUI_COMPASS);
                        output.accept(DRAGON_RULER);
                        output.accept(QI_OBSERVATORY);
                        output.accept(CENSER);
                        output.accept(SHI_GANDANG);
                        output.accept(BAGUA_PLATFORM);
                        output.accept(TAIJI_DIAGRAM);
                        output.accept(YINYANG_STELE);
                        output.accept(ALTAR);
                        output.accept(RUIN_CORE);
                        output.accept(DRAGON_CORE_BLOCK);
                        // 三、六爻
                        output.accept(BRONZE_COINS);
                        output.accept(YARROW_STALKS);
                        output.accept(TURTLE_SHELL);
                        output.accept(SUNDIAL);
                        output.accept(GUA_BOARD);
                        // 四、符箓
                        output.accept(WARD_TALISMAN);
                        output.accept(FORTUNE_TALISMAN);
                        output.accept(CLARITY_TALISMAN);
                        output.accept(TAISUI_TALISMAN);
                        // 五、材料
                        output.accept(WOOD_STONE);
                        output.accept(FIRE_STONE);
                        output.accept(EARTH_STONE);
                        output.accept(METAL_STONE);
                        output.accept(WATER_STONE);
                        output.accept(YINYANG_STONE);
                        output.accept(DRAGON_CORE_SHARD);
                        output.accept(FLAME_FRAGMENT);
                        output.accept(SOUL_CORE);
                        output.accept(VOID_SHARD);
                        output.accept(CHORUS_CORE);
                        output.accept(DRAGON_BREATH_CORE);
                        // 六、建材
                        output.accept(WOOD_STONE_BLOCK);
                        output.accept(FIRE_STONE_BLOCK);
                        output.accept(EARTH_STONE_BLOCK);
                        output.accept(METAL_STONE_BLOCK);
                        output.accept(WATER_STONE_BLOCK);
                        // 七、维度建材（星辉 / 青玉 / 爻纹）
                        output.accept(ASTRAL_STONE);
                        output.accept(ASTRAL_BRICKS);
                        output.accept(ASTRAL_LAMP);
                        output.accept(JADE_STONE);
                        output.accept(JADE_BRICKS);
                        output.accept(JADE_LAMP);
                        output.accept(YAO_STONE);
                        output.accept(YAO_BRICKS);
                        output.accept(YAO_LAMP);
                        // 八、灵兽
                        output.accept(FATE_WISP_SPAWN_EGG);
                        output.accept(STAR_WATCHER_SPAWN_EGG);
                        output.accept(STAR_FOX_SPAWN_EGG);
                        output.accept(STAR_SLIME_SPAWN_EGG);
                        output.accept(JADE_SPIRIT_SPAWN_EGG);
                        output.accept(JADE_RABBIT_SPAWN_EGG);
                        output.accept(JADE_GOAT_SPAWN_EGG);
                        output.accept(VEIN_TURTLE_SPAWN_EGG);
                        output.accept(STONE_GUARDIAN_SPAWN_EGG);
                        output.accept(INK_WOLF_SPAWN_EGG);
                        output.accept(INK_BAT_SPAWN_EGG);
                        output.accept(HEX_PHANTOM_SPAWN_EGG);
                        // 九、维度矿石与灵植（每界域四矿）
                        output.accept(ASTRAL_ORE);
                        output.accept(METEOR_ORE);
                        output.accept(STAR_SAND_ORE);
                        output.accept(CRYSTAL_ORE);
                        output.accept(ASTRAL_DUST);
                        output.accept(METEOR_IRON);
                        output.accept(STAR_SAND);
                        output.accept(STAR_CRYSTAL);
                        output.accept(ASTRAL_BLOOM);
                        output.accept(ASTRAL_GRASS);
                        output.accept(JADE_ORE);
                        output.accept(VERDANT_ORE);
                        output.accept(MICA_ORE);
                        output.accept(VEIN_ORE);
                        output.accept(JADE_GRIT);
                        output.accept(VERDANT_GEM);
                        output.accept(MICA);
                        output.accept(VEIN_CRYSTAL);
                        output.accept(JADE_LILY);
                        output.accept(JADE_GRASS);
                        output.accept(YAO_ORE);
                        output.accept(INK_IRON_ORE);
                        output.accept(ONYX_ORE);
                        output.accept(ORACLE_ORE);
                        output.accept(YAO_SHARD);
                        output.accept(INK_IRON);
                        output.accept(ONYX);
                        output.accept(ORACLE_BONE);
                        output.accept(YAO_BLOOM);
                        output.accept(YAO_GRASS);
                        // 十、维度建筑三件套
                        output.accept(ASTRAL_PILLAR);
                        output.accept(ASTRAL_SLAB);
                        output.accept(ASTRAL_STAIRS);
                        output.accept(JADE_PILLAR);
                        output.accept(JADE_SLAB);
                        output.accept(JADE_STAIRS);
                        output.accept(YAO_PILLAR);
                        output.accept(YAO_SLAB);
                        output.accept(YAO_STAIRS);
                        // 十一、维度液体
                        output.accept(ASTRAL_DEW_BUCKET);
                        output.accept(JADE_ICHOR_BUCKET);
                        output.accept(YAO_INK_BUCKET);
                        // 十二、门户钥匙
                        output.accept(ASTRAL_KEY);
                        output.accept(JADE_KEY);
                        output.accept(YAO_KEY);
                        // 十三、维度林木（三界各三种：原木 / 树叶 / 树苗）
                        output.accept(ASTRAL_WOOD_LOG);
                        output.accept(ASTRAL_WOOD_LEAVES);
                        output.accept(ASTRAL_WOOD_SAPLING);
                        output.accept(METEOR_WOOD_LOG);
                        output.accept(METEOR_WOOD_LEAVES);
                        output.accept(METEOR_WOOD_SAPLING);
                        output.accept(MOON_WOOD_LOG);
                        output.accept(MOON_WOOD_LEAVES);
                        output.accept(MOON_WOOD_SAPLING);
                        output.accept(JADE_WOOD_LOG);
                        output.accept(JADE_WOOD_LEAVES);
                        output.accept(JADE_WOOD_SAPLING);
                        output.accept(CLOUD_WOOD_LOG);
                        output.accept(CLOUD_WOOD_LEAVES);
                        output.accept(CLOUD_WOOD_SAPLING);
                        output.accept(PEACH_WOOD_LOG);
                        output.accept(PEACH_WOOD_LEAVES);
                        output.accept(PEACH_WOOD_SAPLING);
                        output.accept(HEXA_WOOD_LOG);
                        output.accept(HEXA_WOOD_LEAVES);
                        output.accept(HEXA_WOOD_SAPLING);
                        output.accept(INK_WOOD_LOG);
                        output.accept(INK_WOOD_LEAVES);
                        output.accept(INK_WOOD_SAPLING);
                        output.accept(MYSTERY_WOOD_LOG);
                        output.accept(MYSTERY_WOOD_LEAVES);
                        output.accept(MYSTERY_WOOD_SAPLING);
                        // 十四、丹道（第 11 轮）
                        output.accept(QI_PILL);
                        output.accept(CLARITY_PILL);
                        output.accept(CALM_PILL);
                        output.accept(SWIFT_PILL);
                        output.accept(WARD_PILL);
                        output.accept(TAISUI_PILL);
                        // 十五、灵物（第 11 轮）
                        output.accept(TALISMAN_PAPER);
                        output.accept(STAR_FUR);
                        output.accept(JADE_WOOL);
                        output.accept(INK_MEMBRANE);
                        output.accept(VEIN_SCALE);
                        output.accept(VEIN_MAP);
                        output.accept(BAGUA_MIRROR);
                        // 十六、界域作物（第 13 轮）
                        output.accept(ASTRAL_SEEDS);
                        output.accept(ASTRAL_GRAIN);
                        output.accept(JADE_SEEDS);
                        output.accept(JADE_GRAIN);
                        output.accept(YAO_SEEDS);
                        output.accept(YAO_POD);
                        // 十七、三大术数（第 15 轮）
                        output.accept(ZIWEI_CHART);
                        output.accept(LIUREN_PLATE);
                        output.accept(QIMEN_DISC);
                        output.accept(ZIWEI_TERRACE);
                        output.accept(LIUREN_ALTAR);
                        output.accept(QIMEN_ARRAY);
                        output.accept(ZIWEI_TOKEN);
                        output.accept(TIANFU_TOKEN);
                        output.accept(QISHA_TOKEN);
                        output.accept(GUIREN_TALISMAN);
                        output.accept(QINGLONG_TALISMAN);
                        output.accept(BAIHU_TALISMAN);
                        output.accept(SHENGMEN_TALISMAN);
                        output.accept(KAIMEN_TALISMAN);
                        output.accept(XIUMEN_TALISMAN);
                        output.accept(ZIWEI_STAR_DUST);
                        output.accept(LIUREN_SHELL);
                        output.accept(QIMEN_SHARD);
                        // 十八、三术界域（第 16 轮）
                        output.accept(ZIWEI_STONE);
                        output.accept(ZIWEI_BRICKS);
                        output.accept(ZIWEI_LAMP);
                        output.accept(LIUREN_STONE);
                        output.accept(LIUREN_BRICKS);
                        output.accept(LIUREN_LAMP);
                        output.accept(QIMEN_STONE);
                        output.accept(QIMEN_BRICKS);
                        output.accept(QIMEN_LAMP);
                        output.accept(ZIWEI_CRYSTAL_ORE);
                        output.accept(DOU_ORE);
                        output.accept(JI_ORE);
                        output.accept(YUAN_ORE);
                        output.accept(REN_ORE);
                        output.accept(GUI_ORE);
                        output.accept(SHE_ORE);
                        output.accept(XUAN_ORE);
                        output.accept(MEN_ORE);
                        output.accept(XING_ORE);
                        output.accept(DUN_ORE);
                        output.accept(JIA_ORE);
                        output.accept(ZIWEI_CRYSTAL);
                        output.accept(ZIWEI_ESSENCE);
                        output.accept(LIUREN_CRYSTAL);
                        output.accept(LIUREN_ESSENCE);
                        output.accept(QIMEN_CRYSTAL);
                        output.accept(QIMEN_ESSENCE);
                        output.accept(ZIWEI_BLOOM);
                        output.accept(ZIWEI_GRASS);
                        output.accept(LIUREN_LILY);
                        output.accept(LIUREN_GRASS);
                        output.accept(QIMEN_BLOOM);
                        output.accept(QIMEN_GRASS);
                        output.accept(ZIWEI_WOOD_LOG);
                        output.accept(ZIWEI_WOOD_LEAVES);
                        output.accept(ZIWEI_WOOD_SAPLING);
                        output.accept(DOU_WOOD_LOG);
                        output.accept(DOU_WOOD_LEAVES);
                        output.accept(DOU_WOOD_SAPLING);
                        output.accept(TIANJI_WOOD_LOG);
                        output.accept(TIANJI_WOOD_LEAVES);
                        output.accept(TIANJI_WOOD_SAPLING);
                        output.accept(REN_WOOD_LOG);
                        output.accept(REN_WOOD_LEAVES);
                        output.accept(REN_WOOD_SAPLING);
                        output.accept(GUI_WOOD_LOG);
                        output.accept(GUI_WOOD_LEAVES);
                        output.accept(GUI_WOOD_SAPLING);
                        output.accept(SHE_WOOD_LOG);
                        output.accept(SHE_WOOD_LEAVES);
                        output.accept(SHE_WOOD_SAPLING);
                        output.accept(MEN_WOOD_LOG);
                        output.accept(MEN_WOOD_LEAVES);
                        output.accept(MEN_WOOD_SAPLING);
                        output.accept(XING_WOOD_LOG);
                        output.accept(XING_WOOD_LEAVES);
                        output.accept(XING_WOOD_SAPLING);
                        output.accept(DUN_WOOD_LOG);
                        output.accept(DUN_WOOD_LEAVES);
                        output.accept(DUN_WOOD_SAPLING);
                        output.accept(DOU_ALTAR);
                        output.accept(GUIREN_ALTAR);
                        output.accept(BAMEN_ALTAR);
                        output.accept(ZIWEI_SEEDS);
                        output.accept(ZIWEI_GRAIN);
                        output.accept(LIUREN_SEEDS);
                        output.accept(LIUREN_GRAIN);
                        output.accept(QIMEN_SEEDS);
                        output.accept(QIMEN_GRAIN);
                        output.accept(ZIWEI_AMBROSIA_BUCKET);
                        output.accept(LIUREN_WATER_BUCKET);
                        output.accept(QIMEN_MERCURY_BUCKET);
                        output.accept(ZIWEI_KEY);
                        output.accept(LIUREN_KEY);
                        output.accept(QIMEN_KEY);
                        output.accept(ZIWEI_STAR_SPAWN_EGG);
                        output.accept(DOU_BEAST_SPAWN_EGG);
                        output.accept(TIANJI_FOX_SPAWN_EGG);
                        output.accept(ZIWEI_MOTH_SPAWN_EGG);
                        output.accept(REN_SERPENT_SPAWN_EGG);
                        output.accept(GUI_CRANE_SPAWN_EGG);
                        output.accept(SHE_HOUND_SPAWN_EGG);
                        output.accept(XUAN_TURTLE_SPAWN_EGG);
                        output.accept(MEN_GUARD_SPAWN_EGG);
                        output.accept(XING_FOX_SPAWN_EGG);
                        output.accept(DUN_RAT_SPAWN_EGG);
                        output.accept(JIA_BIRD_SPAWN_EGG);
                        // ---- 第 19 轮：太乙界 ----
                        output.accept(TAIYI_STONE);
                        output.accept(TAIYI_BRICKS);
                        output.accept(TAIYI_LAMP);
                        output.accept(TAIYI_TERRACE);
                        output.accept(WENCHANG_ALTAR);
                        output.accept(TAIYI_ORE);
                        output.accept(WENCHANG_ORE);
                        output.accept(SHIJI_ORE);
                        output.accept(ZHUKE_ORE);
                        output.accept(TAIYI_CRYSTAL);
                        output.accept(TAIYI_ESSENCE);
                        output.accept(TAIYI_SHARD);
                        output.accept(TAIYI_GRAIN);
                        output.accept(TAIYI_BLOOM);
                        output.accept(TAIYI_GRASS);
                        output.accept(TAIYI_WOOD_LOG);
                        output.accept(TAIYI_WOOD_LEAVES);
                        output.accept(TAIYI_WOOD_SAPLING);
                        output.accept(WENCHANG_WOOD_LOG);
                        output.accept(WENCHANG_WOOD_LEAVES);
                        output.accept(WENCHANG_WOOD_SAPLING);
                        output.accept(JISHEN_WOOD_LOG);
                        output.accept(JISHEN_WOOD_LEAVES);
                        output.accept(JISHEN_WOOD_SAPLING);
                        output.accept(TAIYI_SEEDS);
                        output.accept(TAIYI_SPRING_BUCKET);
                        // 十一、七界凶液 · 矿物原矿（第 21 轮）
                        output.accept(ASTRAL_SLAG_BUCKET);
                        output.accept(JADE_ACID_BUCKET);
                        output.accept(YAO_MIASMA_BUCKET);
                        output.accept(ZIWEI_BANE_BUCKET);
                        output.accept(LIUREN_ABYSS_BUCKET);
                        output.accept(QIMEN_HELLFIRE_BUCKET);
                        output.accept(TAIYI_FROST_BUCKET);
                        output.accept(ASTRAL_RAW);
                        output.accept(JADE_RAW);
                        output.accept(YAO_RAW);
                        output.accept(METEOR_RAW);
                        output.accept(INK_IRON_RAW);
                        output.accept(MICA_RAW);
                        output.accept(ONYX_RAW);
                        output.accept(STAR_SAND_RAW);
                        output.accept(VEIN_RAW);
                        output.accept(VERDANT_RAW);
                        output.accept(ORACLE_RAW);
                        output.accept(CRYSTAL_RAW);
                        output.accept(ZIWEI_CRYSTAL_RAW);
                        output.accept(REN_RAW);
                        output.accept(GUI_RAW);
                        output.accept(SHE_RAW);
                        output.accept(XUAN_RAW);
                        output.accept(MEN_RAW);
                        output.accept(XING_RAW);
                        output.accept(DUN_RAW);
                        output.accept(JIA_RAW);
                        output.accept(TAIYI_RAW);
                        output.accept(WENCHANG_RAW);
                        output.accept(SHIJI_RAW);
                        output.accept(ZHUKE_RAW);
                        output.accept(TAIYI_KEY);
                        // 十一、木三件套（第 22 轮）
                        for (Block planks : WOOD_PLANKS.values()) {
                            output.accept(planks);
                        }
                        for (Block slab : WOOD_SLABS.values()) {
                            output.accept(slab);
                        }
                        for (Block stairs : WOOD_STAIRS.values()) {
                            output.accept(stairs);
                        }
                        // 十二、木船（第 23 轮）
                        for (Item boat : WOOD_BOATS.values()) {
                            output.accept(boat);
                        }
                        // 十三、运输船与木变体家族（第 25 轮）
                        for (Item chestBoat : WOOD_CHEST_BOATS.values()) {
                            output.accept(chestBoat);
                        }
                        for (Block fence : WOOD_FENCES.values()) {
                            output.accept(fence);
                        }
                        for (Block gate : WOOD_FENCE_GATES.values()) {
                            output.accept(gate);
                        }
                        for (Block door : WOOD_DOORS.values()) {
                            output.accept(door);
                        }
                        for (Block trapdoor : WOOD_TRAPDOORS.values()) {
                            output.accept(trapdoor);
                        }
                        for (Block button : WOOD_BUTTONS.values()) {
                            output.accept(button);
                        }
                        for (Block plate : WOOD_PLATES.values()) {
                            output.accept(plate);
                        }
                        for (Item signItem : WOOD_SIGN_ITEMS.values()) {
                            output.accept(signItem);
                        }
                        // 十、相面 · 阵盘 · 镇物（第 20 轮）
                        output.accept(FACE_MIRROR);
                        output.accept(BAGUA_ARRAY_DISC);
                        output.accept(BEIDOU_ARRAY_DISC);
                        output.accept(ZHAOCAI_ARRAY_DISC);
                        output.accept(ZHENZHAI_STONE);
                        output.accept(HUASHA_STONE);
                    })
                    .build());

    // ==================================================================== 工具方法

    /** 术符：一次性，撕开就给一组状态效果。 */
    private static Item charm(String name, String noteKey, MobEffectInstance... effects) {
        return item(name, new YsItemContent.OracleCharm(props(name).stacksTo(16), noteKey, effects));
    }

    /** 状态效果简写（全都不显示粒子，免得玩家被自己糊一脸）。 */
    private static MobEffectInstance effect(Holder<MobEffect> effect, int duration, int amplifier) {
        return new MobEffectInstance(effect, duration, amplifier, true, false);
    }

    private static Item.Properties props(String name) {
        return new Item.Properties().setId(YsReg.itemKey(name));
    }

    private static BlockBehaviour.Properties blockProps(String name, MapColor color) {
        return BlockBehaviour.Properties.of()
                .setId(YsReg.blockKey(name))
                .mapColor(color)
                .pushReaction(PushReaction.NORMAL);
    }

    private static Item item(String name, Item value) {
        return Registry.register(BuiltInRegistries.ITEM, YsReg.id(name), value);
    }

    /** 只注册方块本体，不给物品形式（作物、液体这类方块用）。 */
    private static Block blockOnly(String name,
                                   java.util.function.Function<BlockBehaviour.Properties, Block> factory,
                                   BlockBehaviour.Properties properties) {
        Block value = factory.apply(properties);
        Registry.register(BuiltInRegistries.BLOCK, YsReg.id(name), value);
        return value;
    }

    private static Item elementStone(String name, WuXing element) {
        return item(name, new YsItemContent.ElementStone(props(name).stacksTo(64), element));
    }

    private static Block stoneBlock(String name, WuXing element, MapColor color, int tint) {
        return block(name,
                properties -> new YsBlockContent.ElementStoneBlock(properties, element),
                blockProps(name, color).strength(2.0F, 9.0F)
                        .requiresCorrectToolForDrops().sound(SoundType.STONE));
    }

    /** 维度建材：普通立方体石块。 */
    private static Block dimensionStone(String name, MapColor color, float hardness, float resistance) {
        return block(name, Block::new,
                blockProps(name, color).strength(hardness, resistance)
                        .requiresCorrectToolForDrops().sound(SoundType.STONE));
    }

    /** 维度明灯：满亮度的立方体光源，地下的眼睛。 */
    private static Block dimensionLamp(String name, MapColor color) {
        return block(name, Block::new,
                blockProps(name, color).strength(0.4F, 4.0F)
                        .sound(SoundType.GLASS).lightLevel(state -> 15));
    }

    /** 维度建筑三件套的公共属性。 */
    private static BlockBehaviour.Properties dimensionBlockProps(String name, MapColor color) {
        return blockProps(name, color).strength(1.8F, 6.0F)
                .requiresCorrectToolForDrops().sound(SoundType.STONE);
    }

    /** 柱子：门楣、廊柱与祭柱。 */
    private static Block pillar(String name, MapColor color) {
        return block(name, RotatedPillarBlock::new, dimensionBlockProps(name, color));
    }

    /** 台阶。 */
    private static Block slab(String name, MapColor color) {
        return block(name, SlabBlock::new, dimensionBlockProps(name, color));
    }

    /** 楼梯：构造时快照基座方块。 */
    private static Block stairs(String name, MapColor color, Block base) {
        return block(name, p -> new YsBlockContent.YsStairs(base.defaultBlockState(), p),
                dimensionBlockProps(name, color));
    }

    /** 原木：三轴可旋转的柱子。 */
    private static Block woodLog(String name, MapColor color) {
        return block(name, RotatedPillarBlock::new,
                blockProps(name, color).strength(2.0F).sound(SoundType.WOOD).ignitedByLava());
    }

    /** 树叶：带落叶粒子；颜色画在贴图里（无 tint）；没有随机刻 —— 永不腐烂。 */
    private static Block leaves(String name, MapColor color, ParticleOptions leafParticle) {
        return block(name, p -> new UntintedParticleLeavesBlock(0.02F, leafParticle, p),
                blockProps(name, color).strength(0.2F).sound(SoundType.GRASS)
                        .noOcclusion().ignitedByLava().pushReaction(PushReaction.DESTROY));
    }

    /** 树苗：种下去能长成本界域的树。 */
    private static Block sapling(String species, MapColor color) {
        String name = species + "_sapling";
        return block(name, p -> new YsBlockContent.YsSapling(treeGrower(species), p),
                blockProps(name, color).noCollision().instabreak().sound(SoundType.GRASS)
                        .offsetType(BlockBehaviour.OffsetType.XZ).pushReaction(PushReaction.DESTROY));
    }

    /** 生长表：把「<树种>_tree」特征键包成 TreeGrower。 */
    private static TreeGrower treeGrower(String species) {
        return new TreeGrower("yishu:" + species, 0.0F,
                Optional.empty(), Optional.empty(),
                Optional.of(ResourceKey.create(Registries.CONFIGURED_FEATURE, YsReg.id(species + "_tree"))),
                Optional.empty(), Optional.empty(), Optional.empty());
    }

    /** 矿石方块：需要石镐，掉落由 data/yishu/loot_table/blocks/*.json 决定。 */
    private static Block oreBlock(String name, MapColor color) {
        return block(name, Block::new, blockProps(name, color).strength(3.0F, 6.0F)
                .requiresCorrectToolForDrops().sound(SoundType.STONE));
    }

    /** 维度矿物（尘 / 砂 / 砾）：普通材料，带一句来历说明。 */
    private static Item materialItem(String name, Rarity rarity) {
        return item(name, new YsItemContent.Material(props(name).stacksTo(64).rarity(rarity),
                "item.yishu." + name + ".note"));
    }

    /** 花：吃下效果汤会生效的装饰花。 */
    private static Block flower(String name, Holder<MobEffect> effect, MapColor color) {
        return block(name, p -> new FlowerBlock(effect, 5.0F, p),
                blockProps(name, color).noCollision().instabreak().sound(SoundType.GRASS)
                        .offsetType(BlockBehaviour.OffsetType.XZ).pushReaction(PushReaction.DESTROY));
    }

    /** 草：不结种、不惧骨粉的装饰草。 */
    private static Block grass(String name, MapColor color) {
        return block(name, YsBlockContent.YsGrass::new,
                blockProps(name, color).noCollision().instabreak().sound(SoundType.GRASS)
                        .offsetType(BlockBehaviour.OffsetType.XZ).pushReaction(PushReaction.DESTROY));
    }

    /**
     * 液体方块：没有物品形式（就像原版的水）。
     *
     * <p>{@code LiquidBlock} 的构造是 protected，用一个私有壳子类接住。
     */
    private static Block liquidBlock(String name, FlowingFluid fluid, MapColor color,
                                     int light, float speed) {
        return blockOnly(name, p -> new YsLiquidShell(fluid, p),
                BlockBehaviour.Properties.of().setId(YsReg.blockKey(name))
                        .mapColor(color).replaceable().noCollision().randomTicks()
                        .strength(100.0F).speedFactor(speed).pushReaction(PushReaction.DESTROY)
                        .noLootTable().liquid().lightLevel(state -> light));
    }

    private static final class YsLiquidShell extends LiquidBlock {

        private YsLiquidShell(FlowingFluid fluid, BlockBehaviour.Properties properties) {
            super(fluid, properties);
        }
    }

    /** 液体桶。 */
    private static Item bucket(String name, FlowingFluid fluid) {
        return item(name, new BucketItem(fluid,
                props(name).stacksTo(1).craftRemainder(Items.BUCKET)));
    }

    /**
     * 传送门方块：无碰撞、免破坏、自带粒子 —— 但没有物品形式，
     * 玩家只能通过灵引立起它。
     */
    private static YsPortalContent.RealmGateBlock gate(String name, ResourceKey<Level> realm,
                                                       String realmZh, String gateName,
                                                       MapColor color, ParticleOptions particle) {
        YsPortalContent.RealmGateBlock value = new YsPortalContent.RealmGateBlock(
                realm, realmZh, gateName, particle,
                BlockBehaviour.Properties.of().setId(YsReg.blockKey(name)).mapColor(color)
                        .noCollision().strength(0.8F).lightLevel(state -> 12)
                        .pushReaction(PushReaction.BLOCK).noLootTable().sound(SoundType.GLASS));
        Registry.register(BuiltInRegistries.BLOCK, YsReg.id(name), value);
        return value;
    }

    /** 灵兽刷怪蛋：26.2 里蛋的实体引用走 ENTITY_DATA 数据组件。 */
    private static Item spawnEgg(String name, EntityType<?> type) {
        return item(name, new SpawnEggItem(props(name).component(
                DataComponents.ENTITY_DATA,
                TypedEntityData.of(type, new CompoundTag()))));
    }

    private static Block block(String name,
                               java.util.function.Function<BlockBehaviour.Properties, Block> factory,
                               BlockBehaviour.Properties properties) {
        Block value = factory.apply(properties);
        Registry.register(BuiltInRegistries.BLOCK, YsReg.id(name), value);
        Item.Properties itemProperties = new Item.Properties()
                .setId(YsReg.itemKey(name))
                .useBlockDescriptionPrefix();
        Registry.register(BuiltInRegistries.ITEM, YsReg.id(name), new BlockItem(value, itemProperties));
        return value;
    }

    /** 触发类加载。 */
    public static void bootstrap() {
        // 静态字段初始化即完成注册
        registerWoodFlammability();
    }

    /**
     * 木头进原版火焰体系（第 24 轮）。
     *
     * <p>原版的可燃表（FireBlock.bootStrap）是逐方块硬编码的，模组方块天生点不着 ——
     * 走 Fabric 的 FlammableBlockRegistry 把易枢木头补录进去，数值与原版对齐：
     * 原木 5/5、板材 20/5、树叶与树苗 30/60（燃烧概率 / 引燃概率）。
     * 石质台阶楼梯不在列（只认 WOOD_* 三张表，不会误伤维度建材）。
     */
    private static void registerWoodFlammability() {
        FlammableBlockRegistry fire = FlammableBlockRegistry.getDefaultInstance();
        for (var entry : BuiltInRegistries.BLOCK.entrySet()) {
            if (!entry.getKey().identifier().getNamespace().equals(YishuMod.MOD_ID)) {
                continue;
            }
            String path = entry.getKey().identifier().getPath();
            if (path.endsWith("_log")) {
                fire.add(entry.getValue(), 5, 5);
            } else if (path.endsWith("_leaves") || path.endsWith("_sapling")) {
                fire.add(entry.getValue(), 30, 60);
            }
        }
        for (Block planks : WOOD_PLANKS.values()) {
            fire.add(planks, 20, 5);
        }
        for (Block slab : WOOD_SLABS.values()) {
            fire.add(slab, 20, 5);
        }
        for (Block stairs : WOOD_STAIRS.values()) {
            fire.add(stairs, 20, 5);
        }
        // 第 25 轮：变体家族也照原版数值可燃（栅栏/门/活板门/按钮/压力板/告示牌）
        for (Map<String, Block> set : List.of(WOOD_FENCES, WOOD_FENCE_GATES, WOOD_DOORS,
                WOOD_TRAPDOORS, WOOD_BUTTONS, WOOD_PLATES, WOOD_STANDING_SIGNS, WOOD_WALL_SIGNS)) {
            for (Block variant : set.values()) {
                fire.add(variant, 20, 5);
            }
        }
    }

    /** 供界面与文档列出全部内容。 */
    public static List<Item> allItems() {
        List<Item> items = new ArrayList<>();
        for (var entry : BuiltInRegistries.ITEM.entrySet()) {
            if (entry.getKey().identifier().getNamespace().equals(YishuMod.MOD_ID)) {
                items.add(entry.getValue());
            }
        }
        return items;
    }

    /** 一份「这是什么吃的」测试用食物（保留接口以便扩展）。 */
    public static FoodProperties simpleFood(int nutrition, float saturation) {
        return new FoodProperties(nutrition, saturation, Boolean.FALSE);
    }

    // ==================================================================== 壳子类（第 25 轮）

    /** 木门：原版 DoorBlock 构造是 protected，接个 public 的。 */
    public static final class YsDoor extends DoorBlock {
        public YsDoor(BlockSetType type, BlockBehaviour.Properties properties) {
            super(type, properties);
        }
    }

    /** 木活板门。 */
    public static final class YsTrapdoor extends TrapDoorBlock {
        public YsTrapdoor(BlockSetType type, BlockBehaviour.Properties properties) {
            super(type, properties);
        }
    }

    /** 木按钮（30 刻 = 原版木按钮按压时长）。 */
    public static final class YsWoodButton extends ButtonBlock {
        public YsWoodButton(BlockSetType type, int pressDuration,
                            BlockBehaviour.Properties properties) {
            super(type, pressDuration, properties);
        }
    }

    /** 木压力板。 */
    public static final class YsWoodPlate extends PressurePlateBlock {
        public YsWoodPlate(BlockSetType type, BlockBehaviour.Properties properties) {
            super(type, properties);
        }
    }

    /** 立式告示牌：方块实体用模组自己的类型（这样原版渲染器照画，无需 mixin）。 */
    public static final class YsStandingSign extends StandingSignBlock {
        public YsStandingSign(BlockBehaviour.Properties properties) {
            super(WoodType.OAK, properties);
        }

        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new YsSignBlockEntity(YISHU_SIGN_BE, pos, state);
        }
    }

    /** 挂墙告示牌。 */
    public static final class YsWallSign extends WallSignBlock {
        public YsWallSign(BlockBehaviour.Properties properties) {
            super(WoodType.OAK, properties);
        }

        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new YsSignBlockEntity(YISHU_SIGN_BE, pos, state);
        }
    }

    /** 模组告示牌方块实体：本体就是原版 SignBlockEntity，换个类型挂我们的方块。 */
    public static final class YsSignBlockEntity extends SignBlockEntity {
        public YsSignBlockEntity(BlockEntityType<? extends SignBlockEntity> type,
                                 BlockPos pos, BlockState state) {
            super(type, pos, state);
        }
    }
}
