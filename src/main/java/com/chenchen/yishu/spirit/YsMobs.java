package com.chenchen.yishu.spirit;

import com.chenchen.yishu.YishuMod;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementType;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.animal.fox.Fox;
import net.minecraft.world.entity.animal.goat.Goat;
import net.minecraft.world.entity.animal.rabbit.Rabbit;
import net.minecraft.world.entity.animal.turtle.Turtle;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.monster.cubemob.Slime;
import net.minecraft.world.entity.animal.allay.Allay;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.levelgen.Heightmap;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.Map;

/**
 * 易枢 · 灵兽 —— 三大术数维度各自的独特生物。
 *
 * <p>设计原则：<b>每一只灵兽都是它所在维度「气」的化身</b>。
 * <ul>
 *   <li><b>星辉符灵</b>（命理维度）—— 由一张写坏的命盘烧成的符纸灵体，
 *       穿墙而行，会主动攻击闯入者。</li>
 *   <li><b>青玉灵</b>（风水维度）—— 成精的青玉碎片，性情温顺，
 *       在灵气充足的山谷之间游荡。</li>
 *   <li><b>卦石守卫</b>（六爻维度 + 各处遗迹）—— 以卦象驱动的石像，
 *       守着遗迹里的秘密，谁动它的东西它就动谁。</li>
 * </ul>
 *
 * <p>实现方式：分别继承原版的 {@link Vex}、{@link Allay}、{@link IronGolem}，
 * 复用原版成熟的模型、动画与寻路，只覆写「目标选择」并换上易枢自己的贴图。
 * 这样在 26.x 这种快速演进的版本线上最稳 —— 原版模型一升级，我们跟着升。
 *
 * <p>26.2 注意：{@code SpawnPlacements.register} 变成了私有方法，这里用反射
 * 把放置规则写进它内部的 {@code DATA_BY_TYPE}。若反射失败也只降级为
 * 「无限制放置」，不影响生物存在。
 */
public final class YsMobs {

    private YsMobs() {
    }

    // ==================================================================== 实体类型

    /** 星辉符灵 —— 命理维度的符纸灵体。 */
    public static final EntityType<FateWisp> FATE_WISP = register("fate_wisp",
            EntityType.Builder.of(FateWisp::new, MobCategory.MONSTER)
                    .sized(0.4F, 0.8F)
                    .clientTrackingRange(8)
                    .notInPeaceful());

    /** 青玉灵 —— 风水维度的玉魄精魅。 */
    public static final EntityType<JadeSpirit> JADE_SPIRIT = register("jade_spirit",
            EntityType.Builder.of(JadeSpirit::new, MobCategory.CREATURE)
                    .sized(0.6F, 0.6F)
                    .clientTrackingRange(8));

    /** 卦石守卫 —— 六爻维度的石像守卫，也镇守在遗迹里。 */
    public static final EntityType<StoneGuardian> STONE_GUARDIAN = register("stone_guardian",
            EntityType.Builder.of(StoneGuardian::new, MobCategory.MONSTER)
                    .sized(1.4F, 2.7F)
                    .clientTrackingRange(10)
                    .notInPeaceful());

    // ---- 第 10 轮：每个界域的「一桌四位」皆已坐满 ----

    /** 观星者 —— 命理界的夜空守望者（末影人的紫夜亲族）。 */
    public static final EntityType<StarWatcher> STAR_WATCHER = register("star_watcher",
            EntityType.Builder.of(StarWatcher::new, MobCategory.MONSTER)
                    .sized(0.6F, 2.9F)
                    .clientTrackingRange(8)
                    .notInPeaceful());

    /** 星狐 —— 命理界的紫夜小狐。 */
    public static final EntityType<StarFox> STAR_FOX = register("star_fox",
            EntityType.Builder.of(StarFox::new, MobCategory.CREATURE)
                    .sized(0.6F, 0.7F)
                    .clientTrackingRange(10));

    /** 星屑史莱姆 —— 命理界会跳的星屑团子。 */
    public static final EntityType<StarSlime> STAR_SLIME = register("star_slime",
            EntityType.Builder.of(StarSlime::new, MobCategory.MONSTER)
                    .sized(0.52F, 0.52F)
                    .clientTrackingRange(10)
                    .notInPeaceful());

    /** 玉兔 —— 风水界的青玉兔子。 */
    public static final EntityType<JadeRabbit> JADE_RABBIT = register("jade_rabbit",
            EntityType.Builder.of(JadeRabbit::new, MobCategory.CREATURE)
                    .sized(0.4F, 0.5F)
                    .clientTrackingRange(8));

    /** 玉羊 —— 风水界的山崖行者。 */
    public static final EntityType<JadeGoat> JADE_GOAT = register("jade_goat",
            EntityType.Builder.of(JadeGoat::new, MobCategory.CREATURE)
                    .sized(0.9F, 1.3F)
                    .clientTrackingRange(10));

    /** 地脉龟 —— 风水界的水泽行者。 */
    public static final EntityType<VeinTurtle> VEIN_TURTLE = register("vein_turtle",
            EntityType.Builder.of(VeinTurtle::new, MobCategory.CREATURE)
                    .sized(1.2F, 0.4F)
                    .clientTrackingRange(10));

    /** 爻狼 —— 六爻界的玄色猎手。 */
    public static final EntityType<InkWolf> INK_WOLF = register("ink_wolf",
            EntityType.Builder.of(InkWolf::new, MobCategory.CREATURE)
                    .sized(0.6F, 0.85F)
                    .clientTrackingRange(10));

    /** 爻蝠 —— 六爻界的夜色使者。 */
    public static final EntityType<InkBat> INK_BAT = register("ink_bat",
            EntityType.Builder.of(InkBat::new, MobCategory.AMBIENT)
                    .sized(0.5F, 0.9F)
                    .clientTrackingRange(8));

    /** 卦影 —— 六爻界的夜巡之影。 */
    public static final EntityType<HexPhantom> HEX_PHANTOM = register("hex_phantom",
            EntityType.Builder.of(HexPhantom::new, MobCategory.MONSTER)
                    .sized(0.9F, 0.5F)
                    .clientTrackingRange(10)
                    .notInPeaceful());

    private static <T extends Entity> EntityType<T> register(String name,
                                                             EntityType.Builder<T> builder) {
        ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, YishuMod.id(name));
        return Registry.register(BuiltInRegistries.ENTITY_TYPE, YishuMod.id(name), builder.build(key));
    }


    // ---- 第 16 轮：三术界十二灵兽 ----

    /** 紫微星官 —— 紫微界的执法者，紫袍高冠。 */
    public static final EntityType<ZiweiStar> ZIWEI_STAR = register("ziwei_star",
            EntityType.Builder.of(ZiweiStar::new, MobCategory.MONSTER)
                    .sized(0.6F, 2.9F)
                    .clientTrackingRange(8)
                    .notInPeaceful());

    /** 斗宿兽 —— 跟着北斗走的巨狼。 */
    public static final EntityType<DouBeast> DOU_BEAST = register("dou_beast",
            EntityType.Builder.of(DouBeast::new, MobCategory.CREATURE)
                    .sized(0.7F, 1.1F)
                    .clientTrackingRange(8));

    /** 天机狐 —— 会算的小狐，眼睛很亮。 */
    public static final EntityType<TianjiFox> TIANJI_FOX = register("tianji_fox",
            EntityType.Builder.of(TianjiFox::new, MobCategory.CREATURE)
                    .sized(0.6F, 0.7F)
                    .clientTrackingRange(8));

    /** 紫垣蛾 —— 绕着星台飞的紫翼蛾。 */
    public static final EntityType<ZiweiMoth> ZIWEI_MOTH = register("ziwei_moth",
            EntityType.Builder.of(ZiweiMoth::new, MobCategory.MONSTER)
                    .sized(0.4F, 0.8F)
                    .clientTrackingRange(8)
                    .notInPeaceful());

    /** 壬水蛟 —— 水天之间游的长蛟。 */
    public static final EntityType<RenSerpent> REN_SERPENT = register("ren_serpent",
            EntityType.Builder.of(RenSerpent::new, MobCategory.CREATURE)
                    .sized(1.2F, 0.5F)
                    .clientTrackingRange(8));

    /** 贵人鹤 —— 六壬界的吉兆，见者一日顺遂。 */
    public static final EntityType<GuiCrane> GUI_CRANE = register("gui_crane",
            EntityType.Builder.of(GuiCrane::new, MobCategory.CREATURE)
                    .sized(0.6F, 1.3F)
                    .clientTrackingRange(10));

    /** 螣蛇犬 —— 蛇性的猎犬，嗅得到「课」的错处。 */
    public static final EntityType<SheHound> SHE_HOUND = register("she_hound",
            EntityType.Builder.of(SheHound::new, MobCategory.CREATURE)
                    .sized(0.7F, 0.9F)
                    .clientTrackingRange(8));

    /** 玄武龟 —— 背甲上刻着水纹的老龟。 */
    public static final EntityType<XuanTurtle> XUAN_TURTLE = register("xuan_turtle",
            EntityType.Builder.of(XuanTurtle::new, MobCategory.CREATURE)
                    .sized(1.4F, 0.5F)
                    .clientTrackingRange(10));

    /** 门卫 —— 守着八门石像的铜人。 */
    public static final EntityType<MenGuard> MEN_GUARD = register("men_guard",
            EntityType.Builder.of(MenGuard::new, MobCategory.MONSTER)
                    .sized(1.4F, 2.7F)
                    .clientTrackingRange(10)
                    .notInPeaceful());

    /** 星魁狐 —— 九星林里的金狐。 */
    public static final EntityType<XingFox> XING_FOX = register("xing_fox",
            EntityType.Builder.of(XingFox::new, MobCategory.CREATURE)
                    .sized(0.6F, 0.7F)
                    .clientTrackingRange(8));

    /** 遁甲鼠 —— 会在阵眼里打洞的鼠。 */
    public static final EntityType<DunRat> DUN_RAT = register("dun_rat",
            EntityType.Builder.of(DunRat::new, MobCategory.CREATURE)
                    .sized(0.4F, 0.5F)
                    .clientTrackingRange(8));

    /** 甲子鸟 —— 报时的黑鸟，六十日一轮。 */
    public static final EntityType<JiaBird> JIA_BIRD = register("jia_bird",
            EntityType.Builder.of(JiaBird::new, MobCategory.AMBIENT)
                    .sized(0.5F, 0.9F)
                    .clientTrackingRange(8));
    // ==================================================================== 注册入口

    /** 属性 + 刷怪放置规则。在模组主入口调用，缺一不可。 */
    public static void register() {
        attempt("星辉符灵属性", () ->
                FabricDefaultAttributeRegistry.register(FATE_WISP, FateWisp.createAttributes()));
        attempt("青玉灵属性", () ->
                FabricDefaultAttributeRegistry.register(JADE_SPIRIT, JadeSpirit.createAttributes()));
        attempt("卦石守卫属性", () ->
                FabricDefaultAttributeRegistry.register(STONE_GUARDIAN, StoneGuardian.createAttributes()));
        attempt("观星者属性", () ->
                FabricDefaultAttributeRegistry.register(STAR_WATCHER, EnderMan.createAttributes()));
        attempt("星狐属性", () ->
                FabricDefaultAttributeRegistry.register(STAR_FOX, Fox.createAttributes()));
        attempt("星屑史莱姆属性", () ->
                FabricDefaultAttributeRegistry.register(STAR_SLIME, Monster.createMonsterAttributes()));
        attempt("玉兔属性", () ->
                FabricDefaultAttributeRegistry.register(JADE_RABBIT, Rabbit.createAttributes()));
        attempt("玉羊属性", () ->
                FabricDefaultAttributeRegistry.register(JADE_GOAT, Goat.createAttributes()));
        attempt("地脉龟属性", () ->
                FabricDefaultAttributeRegistry.register(VEIN_TURTLE, Turtle.createAttributes()));
        attempt("爻狼属性", () ->
                FabricDefaultAttributeRegistry.register(INK_WOLF, Wolf.createAttributes()));
        attempt("爻蝠属性", () ->
                FabricDefaultAttributeRegistry.register(INK_BAT, Bat.createAttributes()));
        attempt("卦影属性", () ->
                FabricDefaultAttributeRegistry.register(HEX_PHANTOM, Monster.createMonsterAttributes()));

        // 符灵：和原版怪物同一条明暗规则（黑暗处才能生成）
        spawnPlacement(FATE_WISP, SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Monster::checkMonsterSpawnRules);
        // 玉灵、守卫：站在实心方块上、脚下有地面即可
        spawnPlacement(JADE_SPIRIT, SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, YsMobs::onSolidGround);
        spawnPlacement(STONE_GUARDIAN, SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, YsMobs::onSolidGround);


        // ---- 第 16 轮：三术界十二灵兽 ----
        attempt("紫微星官属性", () ->
                FabricDefaultAttributeRegistry.register(ZIWEI_STAR, EnderMan.createAttributes()));
        attempt("斗宿兽属性", () ->
                FabricDefaultAttributeRegistry.register(DOU_BEAST, Wolf.createAttributes()));
        attempt("天机狐属性", () ->
                FabricDefaultAttributeRegistry.register(TIANJI_FOX, Fox.createAttributes()));
        attempt("紫垣蛾属性", () ->
                FabricDefaultAttributeRegistry.register(ZIWEI_MOTH, Monster.createMonsterAttributes()));
        attempt("壬水蛟属性", () ->
                FabricDefaultAttributeRegistry.register(REN_SERPENT, Turtle.createAttributes()));
        attempt("贵人鹤属性", () ->
                FabricDefaultAttributeRegistry.register(GUI_CRANE, GuiCrane.createAttributes()));
        attempt("螣蛇犬属性", () ->
                FabricDefaultAttributeRegistry.register(SHE_HOUND, Wolf.createAttributes()));
        attempt("玄武龟属性", () ->
                FabricDefaultAttributeRegistry.register(XUAN_TURTLE, Turtle.createAttributes()));
        attempt("门卫属性", () ->
                FabricDefaultAttributeRegistry.register(MEN_GUARD, MenGuard.createAttributes()));
        attempt("星魁狐属性", () ->
                FabricDefaultAttributeRegistry.register(XING_FOX, Fox.createAttributes()));
        attempt("遁甲鼠属性", () ->
                FabricDefaultAttributeRegistry.register(DUN_RAT, Rabbit.createAttributes()));
        attempt("甲子鸟属性", () ->
                FabricDefaultAttributeRegistry.register(JIA_BIRD, Bat.createAttributes()));

        // 刷怪规则：陆行的站在实地上，飞行的与怪物走原版判据
        spawnPlacement(ZIWEI_STAR, SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Monster::checkMonsterSpawnRules);
        spawnPlacement(DOU_BEAST, SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, YsMobs::onSolidGround);
        spawnPlacement(TIANJI_FOX, SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, YsMobs::onSolidGround);
        spawnPlacement(ZIWEI_MOTH, SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Monster::checkMonsterSpawnRules);
        spawnPlacement(REN_SERPENT, SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, YsMobs::onSolidGround);
        spawnPlacement(GUI_CRANE, SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, YsMobs::onSolidGround);
        spawnPlacement(SHE_HOUND, SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, YsMobs::onSolidGround);
        spawnPlacement(XUAN_TURTLE, SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, YsMobs::onSolidGround);
        spawnPlacement(MEN_GUARD, SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, YsMobs::onSolidGround);
        spawnPlacement(XING_FOX, SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, YsMobs::onSolidGround);
        spawnPlacement(DUN_RAT, SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, YsMobs::onSolidGround);
        spawnPlacement(JIA_BIRD, SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, YsMobs::onSolidGround);
        // ---- 第 10 轮九灵兽 ----
        spawnPlacement(STAR_WATCHER, SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Monster::checkMonsterSpawnRules);
        spawnPlacement(STAR_FOX, SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, YsMobs::onSolidGround);
        spawnPlacement(STAR_SLIME, SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Monster::checkMonsterSpawnRules);
        spawnPlacement(JADE_RABBIT, SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, YsMobs::onSolidGround);
        spawnPlacement(JADE_GOAT, SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, YsMobs::onSolidGround);
        spawnPlacement(VEIN_TURTLE, SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, YsMobs::onSolidGround);
        spawnPlacement(INK_WOLF, SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, YsMobs::onSolidGround);
        spawnPlacement(INK_BAT, SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, YsMobs::onSolidGround);
        spawnPlacement(HEX_PHANTOM, SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Monster::checkMonsterSpawnRules);
    }

    private static <T extends Entity> boolean onSolidGround(EntityType<T> type,
                                                            ServerLevelAccessor level,
                                                            EntitySpawnReason reason,
                                                            BlockPos pos,
                                                            RandomSource random) {
        return level.getBlockState(pos).isAir()
                && level.getBlockState(pos.below()).blocksMotion();
    }

    /**
     * 把放置规则写进 {@code SpawnPlacements} 的私有表。
     *
     * <p>26.2 把这个注册入口封成了私有方法 + 私有 record（{@code SpawnPlacements$Data}），
     * 而模组侧没有官方钩子；这里是官方补回公共 API 之前最稳妥的做法：
     * 直接反射进内部表。字段名在未混淆的 26.x 上是稳定的。
     */
    private static <T extends Mob> void spawnPlacement(EntityType<T> type,
                                                       SpawnPlacementType placement,
                                                       Heightmap.Types heightmap,
                                                       SpawnPlacements.SpawnPredicate<T> predicate) {
        try {
            Field field = SpawnPlacements.class.getDeclaredField("DATA_BY_TYPE");
            field.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<EntityType<?>, Object> byType = (Map<EntityType<?>, Object>) field.get(null);

            Class<?> dataClass = Class.forName("net.minecraft.world.entity.SpawnPlacements$Data");
            Constructor<?> constructor = dataClass.getDeclaredConstructor(
                    Heightmap.Types.class, SpawnPlacementType.class, SpawnPlacements.SpawnPredicate.class);
            constructor.setAccessible(true);
            byType.put(type, constructor.newInstance(heightmap, placement, predicate));
        } catch (Throwable throwable) {
            YishuMod.LOGGER.warn("[Yishu] 刷怪放置规则注册失败（{}），该生物将按「无限制」生成",
                    EntityType.getKey(type), throwable);
        }
    }

    // ==================================================================== 实体实现

    /**
     * 星辉符灵。
     *
     * <p>继承恼鬼：飘行、可以穿墙（恼鬼的移动控制会穿透方块），
     * 额外补上一条「主动攻击玩家」的目标 —— 原版恼鬼只有被唤魔者指挥时才找人，
     * 我们需要它是独立生存的守卫。
     */
    public static class FateWisp extends Vex {

        public FateWisp(EntityType<? extends Vex> type, Level level) {
            super(type, level);
        }

        @Override
        protected void registerGoals() {
            super.registerGoals();
            this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Player.class, true));
        }
    }

    /**
     * 青玉灵。
     *
     * <p>继承悦灵：飘行、好奇、无害。全部行为保持原版，
     * 它存在的意义是「让风水维度活着」—— 一座山谷如果只有石头，风水是死的。
     */
    public static class JadeSpirit extends Allay {

        public JadeSpirit(EntityType<? extends Allay> type, Level level) {
            super(type, level);
        }
    }

    /**
     * 卦石守卫。
     *
     * <p>继承铁傀儡：硬、重、一拳一个。在原版行为之上补一条
     * 「把玩家也列入目标」——它守的是遗迹的密室，不是村子。
     */
    public static class StoneGuardian extends IronGolem {

        public StoneGuardian(EntityType<? extends IronGolem> type, Level level) {
            super(type, level);
        }

        @Override
        protected void registerGoals() {
            super.registerGoals();
            this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        }
    }

    // ---- 第 10 轮九灵兽 ----

    /** 观星者 —— 末影人的紫夜亲族，会瞬移、会凝视。 */
    public static class StarWatcher extends EnderMan {

        public StarWatcher(EntityType<? extends EnderMan> type, Level level) {
            super(type, level);
        }
    }

    /** 星狐 —— 含光的小狐，昼伏夜出。 */
    public static class StarFox extends Fox {

        public StarFox(EntityType<? extends Fox> type, Level level) {
            super(type, level);
        }
    }

    /** 星屑史莱姆 —— 会跳的星屑，撞上会疼。 */
    public static class StarSlime extends Slime {

        public StarSlime(EntityType<? extends Slime> type, Level level) {
            super(type, level);
        }
    }

    /** 玉兔 —— 三瓣嘴啃玉草的小家伙。 */
    public static class JadeRabbit extends Rabbit {

        public JadeRabbit(EntityType<? extends Rabbit> type, Level level) {
            super(type, level);
        }
    }

    /** 玉羊 —— 会顶人的山崖行者（原版行为照旧）。 */
    public static class JadeGoat extends Goat {

        public JadeGoat(EntityType<? extends Goat> type, Level level) {
            super(type, level);
        }
    }

    /** 地脉龟 —— 慢慢走、慢慢看。 */
    public static class VeinTurtle extends Turtle {

        public VeinTurtle(EntityType<? extends Turtle> type, Level level) {
            super(type, level);
        }
    }

    /** 爻狼 —— 玄色的猎手，见人就咬。 */
    public static class InkWolf extends Wolf {

        public InkWolf(EntityType<? extends Wolf> type, Level level) {
            super(type, level);
        }

        @Override
        protected void registerGoals() {
            super.registerGoals();
            this.targetSelector.addGoal(5, new NearestAttackableTargetGoal<>(this, Player.class, true));
        }
    }

    /** 爻蝠 —— 夜里的墨点。 */
    public static class InkBat extends Bat {

        public InkBat(EntityType<? extends Bat> type, Level level) {
            super(type, level);
        }
    }

    /** 卦影 —— 在三界夜色里巡游的影。 */
    public static class HexPhantom extends Phantom {

        public HexPhantom(EntityType<? extends Phantom> type, Level level) {
            super(type, level);
        }
    }


    /** 紫微星官 —— 紫袍高冠的执法者，见者屏息。 */
    public static class ZiweiStar extends EnderMan {

        public ZiweiStar(EntityType<? extends EnderMan> type, Level level) {
            super(type, level);
        }

        @Override
        protected void registerGoals() {
            super.registerGoals();
            this.targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, Player.class, true));
        }
    }

    /** 斗宿兽 —— 跟着北斗走的巨狼。 */
    public static class DouBeast extends Wolf {

        public DouBeast(EntityType<? extends Wolf> type, Level level) {
            super(type, level);
        }
    }

    /** 天机狐 —— 会算的小狐，眼睛很亮。 */
    public static class TianjiFox extends Fox {

        public TianjiFox(EntityType<? extends Fox> type, Level level) {
            super(type, level);
        }
    }

    /** 紫垣蛾 —— 绕着星台飞的紫翼蛾。 */
    public static class ZiweiMoth extends Vex {

        public ZiweiMoth(EntityType<? extends Vex> type, Level level) {
            super(type, level);
        }

        @Override
        protected void registerGoals() {
            super.registerGoals();
            this.targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, Player.class, true));
        }
    }

    /** 壬水蛟 —— 水天之间游的长蛟。 */
    public static class RenSerpent extends Turtle {

        public RenSerpent(EntityType<? extends Turtle> type, Level level) {
            super(type, level);
        }
    }

    /** 贵人鹤 —— 六壬界的吉兆，见者一日顺遂。 */
    public static class GuiCrane extends Allay {

        public GuiCrane(EntityType<? extends Allay> type, Level level) {
            super(type, level);
        }
    }

    /** 螣蛇犬 —— 蛇性的猎犬，嗅得到课的错处。 */
    public static class SheHound extends Wolf {

        public SheHound(EntityType<? extends Wolf> type, Level level) {
            super(type, level);
        }

        @Override
        protected void registerGoals() {
            super.registerGoals();
            this.targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, Player.class, true));
        }
    }

    /** 玄武龟 —— 背甲上刻着水纹的老龟。 */
    public static class XuanTurtle extends Turtle {

        public XuanTurtle(EntityType<? extends Turtle> type, Level level) {
            super(type, level);
        }
    }

    /** 门卫 —— 守着八门石像的铜人。 */
    public static class MenGuard extends IronGolem {

        public MenGuard(EntityType<? extends IronGolem> type, Level level) {
            super(type, level);
        }

        @Override
        protected void registerGoals() {
            super.registerGoals();
            this.targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, Player.class, true));
        }
    }

    /** 星魁狐 —— 九星林里的金狐。 */
    public static class XingFox extends Fox {

        public XingFox(EntityType<? extends Fox> type, Level level) {
            super(type, level);
        }
    }

    /** 遁甲鼠 —— 会在阵眼里打洞的鼠。 */
    public static class DunRat extends Rabbit {

        public DunRat(EntityType<? extends Rabbit> type, Level level) {
            super(type, level);
        }
    }

    /** 甲子鸟 —— 报时的黑鸟，六十日一轮。 */
    public static class JiaBird extends Bat {

        public JiaBird(EntityType<? extends Bat> type, Level level) {
            super(type, level);
        }
    }
    // ==================================================================== 小工具

    /** 是否为模组灵兽（易枢自己注册的生物）—— 灵兽通灵性，滥杀损阴德。 */
    public static boolean isModdedSpirit(net.minecraft.world.entity.LivingEntity entity) {
        return BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).getNamespace()
                .equals(YishuMod.MOD_ID);
    }

    private static void attempt(String name, Runnable action) {
        try {
            action.run();
        } catch (Throwable throwable) {
            YishuMod.LOGGER.error("[Yishu] 灵兽 {} 注册失败（其余灵兽继续加载）", name, throwable);
        }
    }
}
