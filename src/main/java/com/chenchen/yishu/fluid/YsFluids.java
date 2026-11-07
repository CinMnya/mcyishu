package com.chenchen.yishu.fluid;

import com.chenchen.yishu.YishuMod;
import com.chenchen.yishu.registry.YsRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.FluidState;

import java.util.Optional;

/**
 * 易枢 · 三种维度液体。
 *
 * <p>三个术数维度各有一片自己的水：命理界的<b>星辉露</b>、风水界的<b>玉髓浆</b>、
 * 六爻界的<b>玄墨水</b>。它们都长得像水，但走起来不一样快、发不一样的光、
 * 泛不一样的涟漪 —— 玩家舀一桶带回家，就是「到过那个世界」的凭证。
 *
 * <p>实现方式沿用原版 {@code WaterFluid} 的骨架：每种液体两个实例
 * （源 / 流水），以构造参数区分「我是源头还是流水」。流水变体
 * （{@code *.Flowing}）额外带一份含液位属性（{@code level}）的状态定义 ——
 * 原版水也这么做，缺了它 {@code LiquidBlock} 构造时就会直接抛异常。
 * 渲染在客户端 {@code YishuClient} 注册，贴图由 {@code tools/generate_assets.py} 生成。
 *
 * <p>谨慎起见这里不用任何随机源 —— 粒子的随机只影响视觉，不参与任何判定。
 */
public final class YsFluids {

    private YsFluids() {
    }

    // ================================================================ 星辉露（命理界）

    /** 星辉露 —— 源头。 */
    public static final RealmFluid ASTRAL_DEW = register("astral_dew", new AstralDew(true));
    /** 星辉露 —— 流水。 */
    public static final RealmFluid FLOWING_ASTRAL_DEW = register("flowing_astral_dew", new AstralDew.Flowing());

    // ================================================================ 玉髓浆（风水界）

    /** 玉髓浆 —— 源头。 */
    public static final RealmFluid JADE_ICHOR = register("jade_ichor", new JadeIchor(true));
    /** 玉髓浆 —— 流水。 */
    public static final RealmFluid FLOWING_JADE_ICHOR = register("flowing_jade_ichor", new JadeIchor.Flowing());

    // ================================================================ 玄墨水（六爻界）

    /** 玄墨水 —— 源头。 */
    public static final RealmFluid YAO_INK = register("yao_ink", new YaoInk(true));
    /** 玄墨水 —— 流水。 */
    public static final RealmFluid FLOWING_YAO_INK = register("flowing_yao_ink", new YaoInk.Flowing());


    // ================================================================ 第 16 轮：三术界液体

    /** 紫微露 —— 紫微界的甘露，舀起来会映出星子。 */
    public static final RealmFluid ZIWEI_AMBROSIA = register("ziwei_ambrosia", new ZiweiAmbrosia(true));
    public static final RealmFluid FLOWING_ZIWEI_AMBROSIA =
            register("flowing_ziwei_ambrosia", new ZiweiAmbrosia.Flowing());

    /** 壬水 —— 六壬界的重水，沉得像一整片海。 */
    public static final RealmFluid LIUREN_WATER = register("liuren_water", new LiurenWater(true));
    public static final RealmFluid FLOWING_LIUREN_WATER =
            register("flowing_liuren_water", new LiurenWater.Flowing());

    /** 遁甲汞 —— 奇门界的液态金，滚烫、沉重、发亮。 */
    public static final RealmFluid QIMEN_MERCURY = register("qimen_mercury", new QimenMercury(true));
    public static final RealmFluid FLOWING_QIMEN_MERCURY =
            register("flowing_qimen_mercury", new QimenMercury.Flowing());

    /** 太乙玄泉 —— 太乙界的星泉，饮之明目、入之澄神。 */
    public static final RealmFluid TAIYI_SPRING = register("taiyi_spring", new TaiyiSpring(true));
    public static final RealmFluid FLOWING_TAIYI_SPRING =
            register("flowing_taiyi_spring", new TaiyiSpring.Flowing());

    // ---- 第 21 轮：七界凶液 —— 界域的另一副面孔，泡进去要付代价 ----

    /** 星陨熔渣 —— 星轨界的残星余温，滚烫。 */
    public static final RealmFluid ASTRAL_SLAG = register("astral_slag", new AstralSlag(true));
    public static final RealmFluid FLOWING_ASTRAL_SLAG =
            register("flowing_astral_slag", new AstralSlag.Flowing());

    /** 玉蚀酸液 —— 风水界的玉浆腐汁，蚀骨。 */
    public static final RealmFluid JADE_ACID = register("jade_acid", new JadeAcid(true));
    public static final RealmFluid FLOWING_JADE_ACID =
            register("flowing_jade_acid", new JadeAcid.Flowing());

    /** 妖瘴浓雾 —— 六爻界的瘴气沉液，蚀魂。 */
    public static final RealmFluid YAO_MIASMA = register("yao_miasma", new YaoMiasma(true));
    public static final RealmFluid FLOWING_YAO_MIASMA =
            register("flowing_yao_miasma", new YaoMiasma.Flowing());

    /** 星煞玄露 —— 紫微界的煞气凝露，侵神。 */
    public static final RealmFluid ZIWEI_BANE = register("ziwei_bane", new ZiweiBane(true));
    public static final RealmFluid FLOWING_ZIWEI_BANE =
            register("flowing_ziwei_bane", new ZiweiBane.Flowing());

    /** 玄冥渊水 —— 六壬界的深渊死水，缚身。 */
    public static final RealmFluid LIUREN_ABYSS = register("liuren_abyss", new LiurenAbyss(true));
    public static final RealmFluid FLOWING_LIUREN_ABYSS =
            register("flowing_liuren_abyss", new LiurenAbyss.Flowing());

    /** 灼魂炎浆 —— 奇门界的死门阴火，焚身。 */
    public static final RealmFluid QIMEN_HELLFIRE =
            register("qimen_hellfire", new QimenHellfire(true));
    public static final RealmFluid FLOWING_QIMEN_HELLFIRE =
            register("flowing_qimen_hellfire", new QimenHellfire.Flowing());

    /** 太乙寒渊 —— 太乙界的星渊寒水，彻骨。 */
    public static final RealmFluid TAIYI_FROST = register("taiyi_frost", new TaiyiFrost(true));
    public static final RealmFluid FLOWING_TAIYI_FROST =
            register("flowing_taiyi_frost", new TaiyiFrost.Flowing());

    // ================================================================ 共用骨架

    /**
     * 维度液体的共同骨架。
     *
     * <p>比水更稠（tick 更慢、铺得更近），比水更静（不会像水一样填满整片洼地），
     * 但都会照亮脚下、都能被桶舀起来。
     */
    public abstract static class RealmFluid extends FlowingFluid {

        /** 液面升腾的灵气粒子，供子类指定。 */
        protected abstract ParticleOptions particle();

        @Override
        protected boolean canConvertToSource(ServerLevel level) {
            return false;
        }

        @Override
        protected int getSlopeFindDistance(LevelReader level) {
            return 3;
        }

        @Override
        protected int getDropOff(LevelReader level) {
            return 1;
        }

        @Override
        public int getTickDelay(LevelReader level) {
            return 8;
        }

        @Override
        protected float getExplosionResistance() {
            return 100.0F;
        }

        @Override
        protected void beforeDestroyingBlock(net.minecraft.world.level.LevelAccessor level,
                                             BlockPos pos, BlockState state) {
            // 维度液体不冲走方块 —— 它们只是缓缓漫过，不拆家
        }

        @Override
        public Optional<SoundEvent> getPickupSound() {
            return Optional.of(SoundEvents.BUCKET_FILL);
        }

        @Override
        public boolean canBeReplacedWith(FluidState state, BlockGetter level, BlockPos pos,
                                         Fluid fluid, Direction direction) {
            return direction == Direction.DOWN && !fluid.isSame(this);
        }

        /** 入液的界域效果 —— 三种液体各自的「脾气」。 */
        protected abstract void realmTouch(LivingEntity living);

        /**
         * 泡在液体里：每秒续一次界域效果。
         *
         * <p>只碰状态、不碰物理 —— 走得快慢早由方块属性定了，
         * 这里管的是「泡久了人会变成什么样」。
         */
        @Override
        protected void entityInside(Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier applier) {
            if (level.isClientSide() || !(entity instanceof LivingEntity living)) {
                return;
            }
            if (level.getLevelData().getGameTime() % 20L != 0L) {
                return;
            }
            realmTouch(living);
        }

        @Override
        protected void animateTick(Level level, BlockPos pos, FluidState state, RandomSource random) {
            if (random.nextInt(7) != 0) {
                return;
            }
            double x = pos.getX() + random.nextDouble();
            double y = pos.getY() + random.nextDouble() * 0.5D + 0.3D;
            double z = pos.getZ() + random.nextDouble();
            level.addParticle(particle(), x, y, z, 0.0D, 0.015D, 0.0D);
        }
    }

    // ================================================================ 具体液体

    /** 星辉露：淡紫，会往上飘星屑。 */
    public static class AstralDew extends RealmFluid {

        private final boolean source;

        private AstralDew(boolean source) {
            this.source = source;
        }

        @Override
        public Fluid getSource() {
            return ASTRAL_DEW;
        }

        @Override
        public Fluid getFlowing() {
            return FLOWING_ASTRAL_DEW;
        }

        @Override
        public boolean isSource(FluidState state) {
            return this.source;
        }

        @Override
        public int getAmount(FluidState state) {
            return this.source ? FluidState.AMOUNT_FULL : state.getValue(LEVEL);
        }

        @Override
        public Item getBucket() {
            return YsRegistry.ASTRAL_DEW_BUCKET;
        }

        @Override
        public BlockState createLegacyBlock(FluidState state) {
            return YsRegistry.ASTRAL_DEW_BLOCK.defaultBlockState()
                    .setValue(LiquidBlock.LEVEL, getLegacyLevel(state));
        }

        @Override
        public boolean isSame(Fluid fluid) {
            return fluid == ASTRAL_DEW || fluid == FLOWING_ASTRAL_DEW;
        }

        @Override
        protected ParticleOptions particle() {
            return ParticleTypes.END_ROD;
        }

        @Override
        protected void realmTouch(LivingEntity living) {
            // 星辉洗眼、也洗命：夜视 + 生命恢复
            living.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 300, 0, true, false, true));
            living.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 0, true, false, true));
        }

        /**
         * 流水变体 —— 结构与原版 {@code WaterFluid$Flowing} 一致。
         *
         * <p>只有「流水」的状态定义里才带液位（{@code level}）属性，源头不需要。
         * 液态方块 {@code LiquidBlock} 构造时会预生成全部液面状态（源头、七级流动、
         * 坠落），流水若缺了这个属性，注册表初始化当场抛
         * 「Cannot set property ... level ... does not exist」并连锁拖垮整个模组。
         */
        private static final class Flowing extends AstralDew {

            private Flowing() {
                super(false);
            }

            @Override
            protected void createFluidStateDefinition(StateDefinition.Builder<Fluid, FluidState> builder) {
                super.createFluidStateDefinition(builder);   // 基础定义：falling
                builder.add(LEVEL);                          // 流水才有液位
            }
        }
    }

    /** 玉髓浆：青碧，泛着丰收般的翠光。 */
    public static class JadeIchor extends RealmFluid {

        private final boolean source;

        private JadeIchor(boolean source) {
            this.source = source;
        }

        @Override
        public Fluid getSource() {
            return JADE_ICHOR;
        }

        @Override
        public Fluid getFlowing() {
            return FLOWING_JADE_ICHOR;
        }

        @Override
        public boolean isSource(FluidState state) {
            return this.source;
        }

        @Override
        public int getAmount(FluidState state) {
            return this.source ? FluidState.AMOUNT_FULL : state.getValue(LEVEL);
        }

        @Override
        public Item getBucket() {
            return YsRegistry.JADE_ICHOR_BUCKET;
        }

        @Override
        public BlockState createLegacyBlock(FluidState state) {
            return YsRegistry.JADE_ICHOR_BLOCK.defaultBlockState()
                    .setValue(LiquidBlock.LEVEL, getLegacyLevel(state));
        }

        @Override
        public boolean isSame(Fluid fluid) {
            return fluid == JADE_ICHOR || fluid == FLOWING_JADE_ICHOR;
        }

        @Override
        protected ParticleOptions particle() {
            return ParticleTypes.HAPPY_VILLAGER;
        }

        @Override
        protected void realmTouch(LivingEntity living) {
            // 玉髓养人：饱和 + 迅捷
            living.addEffect(new MobEffectInstance(MobEffects.SATURATION, 60, 0, true, false, true));
            living.addEffect(new MobEffectInstance(MobEffects.SPEED, 100, 0, true, false, true));
        }

        /** 流水变体：与星辉露同理 —— 状态定义里补上液位。 */
        private static final class Flowing extends JadeIchor {

            private Flowing() {
                super(false);
            }

            @Override
            protected void createFluidStateDefinition(StateDefinition.Builder<Fluid, FluidState> builder) {
                super.createFluidStateDefinition(builder);
                builder.add(LEVEL);
            }
        }
    }

    /** 玄墨水：墨黑透红，偶尔冒一点墨滴。 */
    public static class YaoInk extends RealmFluid {

        private final boolean source;

        private YaoInk(boolean source) {
            this.source = source;
        }

        @Override
        public Fluid getSource() {
            return YAO_INK;
        }

        @Override
        public Fluid getFlowing() {
            return FLOWING_YAO_INK;
        }

        @Override
        public boolean isSource(FluidState state) {
            return this.source;
        }

        @Override
        public int getAmount(FluidState state) {
            return this.source ? FluidState.AMOUNT_FULL : state.getValue(LEVEL);
        }

        @Override
        public Item getBucket() {
            return YsRegistry.YAO_INK_BUCKET;
        }

        @Override
        public BlockState createLegacyBlock(FluidState state) {
            return YsRegistry.YAO_INK_BLOCK.defaultBlockState()
                    .setValue(LiquidBlock.LEVEL, getLegacyLevel(state));
        }

        @Override
        public boolean isSame(Fluid fluid) {
            return fluid == YAO_INK || fluid == FLOWING_YAO_INK;
        }

        @Override
        protected ParticleOptions particle() {
            return ParticleTypes.SQUID_INK;
        }

        @Override
        protected void realmTouch(LivingEntity living) {
            // 墨海无情：黑暗 + 缓慢 —— 这潭水，慎入。
            living.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 100, 0, true, false, true));
            living.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 100, 0, true, false, true));
        }

        /** 流水变体：与星辉露同理 —— 状态定义里补上液位。 */
        private static final class Flowing extends YaoInk {

            private Flowing() {
                super(false);
            }

            @Override
            protected void createFluidStateDefinition(StateDefinition.Builder<Fluid, FluidState> builder) {
                super.createFluidStateDefinition(builder);
                builder.add(LEVEL);
            }
        }
    }


    /** 紫微露 —— 洗眼也洗命：夜视 + 伤害吸收。 */
    public static class TaiyiSpring extends RealmFluid {

        private final boolean source;

        private TaiyiSpring(boolean source) {
            this.source = source;
        }

        @Override
        public Fluid getSource() {
            return TAIYI_SPRING;
        }

        @Override
        public Fluid getFlowing() {
            return FLOWING_TAIYI_SPRING;
        }

        @Override
        public boolean isSource(FluidState state) {
            return this.source;
        }

        @Override
        public int getAmount(FluidState state) {
            return this.source ? FluidState.AMOUNT_FULL : state.getValue(LEVEL);
        }

        @Override
        public Item getBucket() {
            return YsRegistry.TAIYI_SPRING_BUCKET;
        }

        @Override
        public BlockState createLegacyBlock(FluidState state) {
            return YsRegistry.TAIYI_SPRING_BLOCK.defaultBlockState()
                    .setValue(LiquidBlock.LEVEL, getLegacyLevel(state));
        }

        @Override
        public boolean isSame(Fluid fluid) {
            return fluid == TAIYI_SPRING || fluid == FLOWING_TAIYI_SPRING;
        }

        @Override
        protected ParticleOptions particle() {
            return ParticleTypes.END_ROD;
        }

        @Override
        protected void realmTouch(LivingEntity living) {
            living.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 320, 0, true, false, true));
            living.addEffect(new MobEffectInstance(MobEffects.JUMP_BOOST, 200, 0, true, false, true));
        }

        /** 流水变体：状态定义里补上液位（与其它界域液体同理）。 */
        private static final class Flowing extends TaiyiSpring {

            private Flowing() {
                super(false);
            }

            @Override
            protected void createFluidStateDefinition(StateDefinition.Builder<Fluid, FluidState> builder) {
                super.createFluidStateDefinition(builder);
                builder.add(LEVEL);
            }
        }
    }

    /** 壬水 —— 沉进去像回了母胎：水下呼吸 + 迅捷。 */
    /** 紫微露 —— 洗眼也洗命：夜视 + 伤害吸收。 */
    public static class ZiweiAmbrosia extends RealmFluid {

        private final boolean source;

        private ZiweiAmbrosia(boolean source) {
            this.source = source;
        }

        @Override
        public Fluid getSource() {
            return ZIWEI_AMBROSIA;
        }

        @Override
        public Fluid getFlowing() {
            return FLOWING_ZIWEI_AMBROSIA;
        }

        @Override
        public boolean isSource(FluidState state) {
            return this.source;
        }

        @Override
        public int getAmount(FluidState state) {
            return this.source ? FluidState.AMOUNT_FULL : state.getValue(LEVEL);
        }

        @Override
        public Item getBucket() {
            return YsRegistry.ZIWEI_AMBROSIA_BUCKET;
        }

        @Override
        public BlockState createLegacyBlock(FluidState state) {
            return YsRegistry.ZIWEI_AMBROSIA_BLOCK.defaultBlockState()
                    .setValue(LiquidBlock.LEVEL, getLegacyLevel(state));
        }

        @Override
        public boolean isSame(Fluid fluid) {
            return fluid == ZIWEI_AMBROSIA || fluid == FLOWING_ZIWEI_AMBROSIA;
        }

        @Override
        protected ParticleOptions particle() {
            return ParticleTypes.END_ROD;
        }

        @Override
        protected void realmTouch(LivingEntity living) {
            living.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 320, 0, true, false, true));
            living.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 200, 0, true, false, true));
        }

        /** 流水变体：状态定义里补上液位（与星辉露同理）。 */
        private static final class Flowing extends ZiweiAmbrosia {

            private Flowing() {
                super(false);
            }

            @Override
            protected void createFluidStateDefinition(StateDefinition.Builder<Fluid, FluidState> builder) {
                super.createFluidStateDefinition(builder);
                builder.add(LEVEL);
            }
        }
    }

    /** 壬水 —— 沉进去像回了母胎：水下呼吸 + 迅捷。 */
    public static class LiurenWater extends RealmFluid {

        private final boolean source;

        private LiurenWater(boolean source) {
            this.source = source;
        }

        @Override
        public Fluid getSource() {
            return LIUREN_WATER;
        }

        @Override
        public Fluid getFlowing() {
            return FLOWING_LIUREN_WATER;
        }

        @Override
        public boolean isSource(FluidState state) {
            return this.source;
        }

        @Override
        public int getAmount(FluidState state) {
            return this.source ? FluidState.AMOUNT_FULL : state.getValue(LEVEL);
        }

        @Override
        public Item getBucket() {
            return YsRegistry.LIUREN_WATER_BUCKET;
        }

        @Override
        public BlockState createLegacyBlock(FluidState state) {
            return YsRegistry.LIUREN_WATER_BLOCK.defaultBlockState()
                    .setValue(LiquidBlock.LEVEL, getLegacyLevel(state));
        }

        @Override
        public boolean isSame(Fluid fluid) {
            return fluid == LIUREN_WATER || fluid == FLOWING_LIUREN_WATER;
        }

        @Override
        protected ParticleOptions particle() {
            return ParticleTypes.BUBBLE;
        }

        @Override
        protected void realmTouch(LivingEntity living) {
            living.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 320, 0, true, false, true));
            living.addEffect(new MobEffectInstance(MobEffects.SPEED, 200, 0, true, false, true));
        }

        /** 流水变体：状态定义里补上液位（与星辉露同理）。 */
        private static final class Flowing extends LiurenWater {

            private Flowing() {
                super(false);
            }

            @Override
            protected void createFluidStateDefinition(StateDefinition.Builder<Fluid, FluidState> builder) {
                super.createFluidStateDefinition(builder);
                builder.add(LEVEL);
            }
        }
    }

    /** 遁甲汞 —— 汞里淬过的人，硬得像阵眼：力量 + 抗性。 */
    public static class QimenMercury extends RealmFluid {

        private final boolean source;

        private QimenMercury(boolean source) {
            this.source = source;
        }

        @Override
        public Fluid getSource() {
            return QIMEN_MERCURY;
        }

        @Override
        public Fluid getFlowing() {
            return FLOWING_QIMEN_MERCURY;
        }

        @Override
        public boolean isSource(FluidState state) {
            return this.source;
        }

        @Override
        public int getAmount(FluidState state) {
            return this.source ? FluidState.AMOUNT_FULL : state.getValue(LEVEL);
        }

        @Override
        public Item getBucket() {
            return YsRegistry.QIMEN_MERCURY_BUCKET;
        }

        @Override
        public BlockState createLegacyBlock(FluidState state) {
            return YsRegistry.QIMEN_MERCURY_BLOCK.defaultBlockState()
                    .setValue(LiquidBlock.LEVEL, getLegacyLevel(state));
        }

        @Override
        public boolean isSame(Fluid fluid) {
            return fluid == QIMEN_MERCURY || fluid == FLOWING_QIMEN_MERCURY;
        }

        @Override
        protected ParticleOptions particle() {
            return ParticleTypes.FLAME;
        }

        @Override
        protected void realmTouch(LivingEntity living) {
            living.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 260, 0, true, false, true));
            living.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 260, 0, true, false, true));
        }

        /** 流水变体：状态定义里补上液位（与星辉露同理）。 */
        private static final class Flowing extends QimenMercury {

            private Flowing() {
                super(false);
            }

            @Override
            protected void createFluidStateDefinition(StateDefinition.Builder<Fluid, FluidState> builder) {
                super.createFluidStateDefinition(builder);
                builder.add(LEVEL);
            }
        }
    }
    // ================================================================ 工具

    private static <T extends Fluid> T register(String path, T fluid) {
        return Registry.register(BuiltInRegistries.FLUID, YishuMod.id(path), fluid);
    }

    /** 触发类加载。 */
    public static void bootstrap() {
        // 静态字段初始化即完成注册
    }

    // ================================================================ 凶液骨架（第 21 轮）

    /**
     * 凶液骨架：与界域液体同一套骨架，只是 {@link #realmTouch} 全是恶意。
     *
     * <p>每秒结算一次接触效果：灼烧、酸蚀、中毒、凋零、缚身、焚身、冻伤 ——
     * 七界各有一个「劝退点」，让玩家用桶舀水时多想一秒。
     */
    public abstract static class BaneFluid extends RealmFluid {

        private final boolean source;

        protected BaneFluid(boolean source) {
            this.source = source;
        }

        /** 源头实例。 */
        protected abstract Fluid still();

        /** 流水实例。 */
        protected abstract Fluid flowing();

        /** 对应的桶。 */
        protected abstract Item bucketItem();

        /** 对应的液体方块。 */
        protected abstract Block block();

        @Override
        public Fluid getSource() {
            return still();
        }

        @Override
        public Fluid getFlowing() {
            return flowing();
        }

        @Override
        public boolean isSource(FluidState state) {
            return this.source;
        }

        @Override
        public int getAmount(FluidState state) {
            return this.source ? FluidState.AMOUNT_FULL : state.getValue(LEVEL);
        }

        @Override
        public Item getBucket() {
            return bucketItem();
        }

        @Override
        public BlockState createLegacyBlock(FluidState state) {
            return block().defaultBlockState()
                    .setValue(LiquidBlock.LEVEL, getLegacyLevel(state));
        }

        @Override
        public boolean isSame(Fluid fluid) {
            return fluid == still() || fluid == flowing();
        }

        /** 流水变体：状态定义里补上液位。 */
        public abstract static class Flowing extends BaneFluid {

            protected Flowing() {
                super(false);
            }

            @Override
            protected void createFluidStateDefinition(
                    StateDefinition.Builder<Fluid, FluidState> builder) {
                super.createFluidStateDefinition(builder);
                builder.add(LEVEL);
            }
        }
    }

    /** 星陨熔渣：残星余温 —— 着火 + 灼伤。 */
    public static class AstralSlag extends BaneFluid {

        public AstralSlag(boolean source) {
            super(source);
        }

        @Override
        protected Fluid still() {
            return ASTRAL_SLAG;
        }

        @Override
        protected Fluid flowing() {
            return FLOWING_ASTRAL_SLAG;
        }

        @Override
        protected Item bucketItem() {
            return YsRegistry.ASTRAL_SLAG_BUCKET;
        }

        @Override
        protected Block block() {
            return YsRegistry.ASTRAL_SLAG_BLOCK;
        }

        @Override
        protected ParticleOptions particle() {
            return ParticleTypes.FLAME;
        }

        @Override
        protected void realmTouch(LivingEntity living) {
            living.igniteForSeconds(4.0F);
            living.hurt(living.damageSources().magic(), 1.0F);
        }

        /** 流水变体。 */
        public static final class Flowing extends AstralSlag {

            public Flowing() {
                super(false);
            }

            @Override
            protected void createFluidStateDefinition(
                    StateDefinition.Builder<Fluid, FluidState> builder) {
                super.createFluidStateDefinition(builder);
                builder.add(LEVEL);
            }
        }
    }

    /** 玉蚀酸液：蚀骨之浆 —— 酸蚀 + 挖掘疲劳。 */
    public static class JadeAcid extends BaneFluid {

        public JadeAcid(boolean source) {
            super(source);
        }

        @Override
        protected Fluid still() {
            return JADE_ACID;
        }

        @Override
        protected Fluid flowing() {
            return FLOWING_JADE_ACID;
        }

        @Override
        protected Item bucketItem() {
            return YsRegistry.JADE_ACID_BUCKET;
        }

        @Override
        protected Block block() {
            return YsRegistry.JADE_ACID_BLOCK;
        }

        @Override
        protected ParticleOptions particle() {
            return ParticleTypes.COMPOSTER;
        }

        @Override
        protected void realmTouch(LivingEntity living) {
            living.hurt(living.damageSources().magic(), 2.0F);
            living.addEffect(new MobEffectInstance(
                    MobEffects.MINING_FATIGUE, 120, 0, true, false, true));
        }

        /** 流水变体。 */
        public static final class Flowing extends JadeAcid {

            public Flowing() {
                super(false);
            }

            @Override
            protected void createFluidStateDefinition(
                    StateDefinition.Builder<Fluid, FluidState> builder) {
                super.createFluidStateDefinition(builder);
                builder.add(LEVEL);
            }
        }
    }

    /** 妖瘴浓雾：瘴气沉液 —— 中毒。 */
    public static class YaoMiasma extends BaneFluid {

        public YaoMiasma(boolean source) {
            super(source);
        }

        @Override
        protected Fluid still() {
            return YAO_MIASMA;
        }

        @Override
        protected Fluid flowing() {
            return FLOWING_YAO_MIASMA;
        }

        @Override
        protected Item bucketItem() {
            return YsRegistry.YAO_MIASMA_BUCKET;
        }

        @Override
        protected Block block() {
            return YsRegistry.YAO_MIASMA_BLOCK;
        }

        @Override
        protected ParticleOptions particle() {
            return ParticleTypes.SMOKE;
        }

        @Override
        protected void realmTouch(LivingEntity living) {
            living.addEffect(new MobEffectInstance(
                    MobEffects.POISON, 140, 0, true, false, true));
        }

        /** 流水变体。 */
        public static final class Flowing extends YaoMiasma {

            public Flowing() {
                super(false);
            }

            @Override
            protected void createFluidStateDefinition(
                    StateDefinition.Builder<Fluid, FluidState> builder) {
                super.createFluidStateDefinition(builder);
                builder.add(LEVEL);
            }
        }
    }

    /** 星煞玄露：煞气凝露 —— 凋零。 */
    public static class ZiweiBane extends BaneFluid {

        public ZiweiBane(boolean source) {
            super(source);
        }

        @Override
        protected Fluid still() {
            return ZIWEI_BANE;
        }

        @Override
        protected Fluid flowing() {
            return FLOWING_ZIWEI_BANE;
        }

        @Override
        protected Item bucketItem() {
            return YsRegistry.ZIWEI_BANE_BUCKET;
        }

        @Override
        protected Block block() {
            return YsRegistry.ZIWEI_BANE_BLOCK;
        }

        @Override
        protected ParticleOptions particle() {
            return ParticleTypes.WITCH;
        }

        @Override
        protected void realmTouch(LivingEntity living) {
            living.addEffect(new MobEffectInstance(
                    MobEffects.WITHER, 90, 0, true, false, true));
        }

        /** 流水变体。 */
        public static final class Flowing extends ZiweiBane {

            public Flowing() {
                super(false);
            }

            @Override
            protected void createFluidStateDefinition(
                    StateDefinition.Builder<Fluid, FluidState> builder) {
                super.createFluidStateDefinition(builder);
                builder.add(LEVEL);
            }
        }
    }

    /** 玄冥渊水：深渊死水 —— 缚身（重缓 + 虚弱）。 */
    public static class LiurenAbyss extends BaneFluid {

        public LiurenAbyss(boolean source) {
            super(source);
        }

        @Override
        protected Fluid still() {
            return LIUREN_ABYSS;
        }

        @Override
        protected Fluid flowing() {
            return FLOWING_LIUREN_ABYSS;
        }

        @Override
        protected Item bucketItem() {
            return YsRegistry.LIUREN_ABYSS_BUCKET;
        }

        @Override
        protected Block block() {
            return YsRegistry.LIUREN_ABYSS_BLOCK;
        }

        @Override
        protected ParticleOptions particle() {
            return ParticleTypes.BUBBLE;
        }

        @Override
        protected void realmTouch(LivingEntity living) {
            living.hurt(living.damageSources().magic(), 1.0F);
            living.addEffect(new MobEffectInstance(
                    MobEffects.SLOWNESS, 140, 2, true, false, true));
            living.addEffect(new MobEffectInstance(
                    MobEffects.WEAKNESS, 140, 0, true, false, true));
        }

        /** 流水变体。 */
        public static final class Flowing extends LiurenAbyss {

            public Flowing() {
                super(false);
            }

            @Override
            protected void createFluidStateDefinition(
                    StateDefinition.Builder<Fluid, FluidState> builder) {
                super.createFluidStateDefinition(builder);
                builder.add(LEVEL);
            }
        }
    }

    /** 灼魂炎浆：死门阴火 —— 焚身。 */
    public static class QimenHellfire extends BaneFluid {

        public QimenHellfire(boolean source) {
            super(source);
        }

        @Override
        protected Fluid still() {
            return QIMEN_HELLFIRE;
        }

        @Override
        protected Fluid flowing() {
            return FLOWING_QIMEN_HELLFIRE;
        }

        @Override
        protected Item bucketItem() {
            return YsRegistry.QIMEN_HELLFIRE_BUCKET;
        }

        @Override
        protected Block block() {
            return YsRegistry.QIMEN_HELLFIRE_BLOCK;
        }

        @Override
        protected ParticleOptions particle() {
            return ParticleTypes.FLAME;
        }

        @Override
        protected void realmTouch(LivingEntity living) {
            living.igniteForSeconds(6.0F);
            living.hurt(living.damageSources().magic(), 2.0F);
        }

        /** 流水变体。 */
        public static final class Flowing extends QimenHellfire {

            public Flowing() {
                super(false);
            }

            @Override
            protected void createFluidStateDefinition(
                    StateDefinition.Builder<Fluid, FluidState> builder) {
                super.createFluidStateDefinition(builder);
                builder.add(LEVEL);
            }
        }
    }

    /** 太乙寒渊：星渊寒水 —— 冻彻骨髓（重缓 + 冻伤）。 */
    public static class TaiyiFrost extends BaneFluid {

        public TaiyiFrost(boolean source) {
            super(source);
        }

        @Override
        protected Fluid still() {
            return TAIYI_FROST;
        }

        @Override
        protected Fluid flowing() {
            return FLOWING_TAIYI_FROST;
        }

        @Override
        protected Item bucketItem() {
            return YsRegistry.TAIYI_FROST_BUCKET;
        }

        @Override
        protected Block block() {
            return YsRegistry.TAIYI_FROST_BLOCK;
        }

        @Override
        protected ParticleOptions particle() {
            return ParticleTypes.SNOWFLAKE;
        }

        @Override
        protected void realmTouch(LivingEntity living) {
            living.hurt(living.damageSources().freeze(), 1.0F);
            living.addEffect(new MobEffectInstance(
                    MobEffects.SLOWNESS, 200, 2, true, false, true));
        }

        /** 流水变体。 */
        public static final class Flowing extends TaiyiFrost {

            public Flowing() {
                super(false);
            }

            @Override
            protected void createFluidStateDefinition(
                    StateDefinition.Builder<Fluid, FluidState> builder) {
                super.createFluidStateDefinition(builder);
                builder.add(LEVEL);
            }
        }
    }
}
