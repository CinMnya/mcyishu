package com.chenchen.yishu.registry;

import com.chenchen.yishu.YishuMod;
import com.chenchen.yishu.content.YsBlockContent;
import com.chenchen.yishu.content.YsItemContent;
import com.chenchen.yishu.content.YsPortalContent;
import com.chenchen.yishu.core.YsReg;
import com.chenchen.yishu.fate.GanZhi.WuXing;
import com.chenchen.yishu.fluid.YsFluids;
import com.chenchen.yishu.spirit.YsMobs;
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
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FlowerBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.UntintedParticleLeavesBlock;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

import java.util.ArrayList;
import java.util.List;
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
}
