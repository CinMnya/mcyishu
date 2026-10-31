package com.chenchen.yishu.content;

import com.chenchen.yishu.YishuMod;
import com.chenchen.yishu.util.YsText;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.tags.BlockTags;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Portal;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * 易枢 · 三座维度门户。
 *
 * <p>三条规矩，贯穿这个文件：
 * <ul>
 *   <li><b>门是站进去就走的。</b>没有火的仪式、没有嘶嘶作响的四秒 ——
 *       走进门槛即启程。传送时间取默认值 0，快得像一阵风。</li>
 *   <li><b>坐标是对齐的。</b>在 (100, y, 200) 穿门，到达那个维度同样的
 *       (100, ?, 200)；从那个维度穿回来，又落在主世界同样的 (100, ?, 200)。
 *       迷路是玩家自己的事，不是门的。</li>
 *   <li><b>门是双向的。</b>站在别人家里的门，把你送回主世界；
 *       站在主世界的门，把你送去那座界域。回到人间，永远是对的默认。</li>
 * </ul>
 *
 * <p>灵引（星枢引 / 玉髓引 / 爻纹引）负责「立门」：在玩家面前自动搭起
 * 一座 4 宽 4 高的牌坊，框架用该维度的砖，中间就是门。三把钥匙长得几乎一样，
 * 只有颜色和去处不同 —— 就像真正的钥匙。
 */
public final class YsPortalContent {

    private YsPortalContent() {
    }

    // ================================================================ 门方块

    /**
     * 维度之门。
     *
     * <p>不挡路、不遮天，只有一层流转的光。玩家、生物、掉落物都能穿过它 ——
     * 但只有「能使用门」的实体才会被传送。
     */
    public static class RealmGateBlock extends Block implements Portal {

        private final ResourceKey<Level> realm;
        private final String realmZh;
        private final String gateName;
        private final ParticleOptions particle;

        public RealmGateBlock(ResourceKey<Level> realm, String realmZh, String gateName,
                              ParticleOptions particle, Properties properties) {
            super(properties);
            this.realm = realm;
            this.realmZh = realmZh;
            this.gateName = gateName;
            this.particle = particle;
        }

        @Override
        protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity,
                                    InsideBlockEffectApplier applier, boolean inside) {
            if (entity.canUsePortal(false)) {
                entity.setAsInsidePortal(this, pos);
            }
        }

        /**
         * 决定这扇门通往哪里。
         *
         * <p>规则一句话：<b>「此岸」永远指回人间。</b>
         * 已经在目标界域里 → 回主世界；否则 → 去该界域。
         * 坐标对齐，落点取目标维度对应 (x,z) 的地表。
         */
        @Override
        public TeleportTransition getPortalDestination(ServerLevel level, Entity entity, BlockPos pos) {
            boolean homebound = level.dimension().equals(this.realm);
            ResourceKey<Level> targetKey = homebound ? Level.OVERWORLD : this.realm;
            ServerLevel target = level.getServer().getLevel(targetKey);
            if (target == null) {
                return null;
            }
            int x = Mth.floor(entity.getX());
            int z = Mth.floor(entity.getZ());
            Vec3 landing = findLandingSpot(target, x, z);
            if (entity instanceof ServerPlayer player) {
                YsText.chat(player, homebound
                        ? "§b你穿过" + this.gateName + "，回到了主世界。"
                        : "§b你穿过" + this.gateName + "，抵达" + this.realmZh + "。");
            }
            return new TeleportTransition(target, landing, Vec3.ZERO,
                    entity.getYRot(), entity.getXRot(), TeleportTransition.PLAY_PORTAL_SOUND);
        }

        /**
         * 找一个「真正踩得住」的落点。
         *
         * <p>先看同坐标列：从地表往下扫，找「实体方块承托 + 头顶两格净空」的位置；
         * 树叶、原木与液体都不算地面（谁也不想降落在树冠上或水底）。
         * 若这一列是虚空或整列不可站，就一圈一圈向外找 ——
         * 浮空岛与大海之上，总有块能站的地方。
         *
         * <p><b>关键经验：目标维度的区块在传送时尚未加载，高度图会返回世界底部；
         * 必须先让服务端同步加载区块，查询才是真的。</b>
         */
        private static Vec3 findLandingSpot(ServerLevel level, int x, int z) {
            Vec3 here = safeColumn(level, x, z);
            if (here != null) {
                return here;
            }
            for (int r = 3; r <= 33; r += 3) {
                for (int dx = -r; dx <= r; dx += r) {
                    for (int dz = -r; dz <= r; dz += r) {
                        if (Math.abs(dx) != r && Math.abs(dz) != r) {
                            continue;
                        }
                        Vec3 spot = safeColumn(level, x + dx, z + dz);
                        if (spot != null) {
                            return spot;
                        }
                    }
                }
            }
            // 兜底：中心区块此刻必然已加载，高度图可信
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            return new Vec3(x + 0.5D, Math.max(y, level.getMinY() + 2), z + 0.5D);
        }

        /**
         * 单列搜索：从地表向下 48 格内，找第一个可站立的实地面。
         *
         * <p>第一步是<b>同步加载/生成该列所在区块</b>——
         * 未加载区块的高度图恒等于世界底部，不加载就等于在虚空里找地。
         */
        private static Vec3 safeColumn(ServerLevel level, int x, int z) {
            if (!level.hasChunkAt(new BlockPos(x, level.getMinY(), z))) {
                level.getChunk(x >> 4, z >> 4);
            }
            int top = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            if (top <= level.getMinY() + 2) {
                return null;   // 虚空列
            }
            int floor = Math.max(level.getMinY() + 1, top - 48);
            for (int y = top; y > floor; y--) {
                BlockPos ground = new BlockPos(x, y - 1, z);
                BlockState below = level.getBlockState(ground);
                if (below.isAir() || !below.blocksMotion()
                        || below.is(BlockTags.LEAVES) || below.is(BlockTags.LOGS)
                        || !below.getFluidState().isEmpty()) {
                    continue;
                }
                BlockPos feet = ground.above();
                BlockPos head = feet.above();
                BlockState feetState = level.getBlockState(feet);
                BlockState headState = level.getBlockState(head);
                if (feetState.getCollisionShape(level, feet).isEmpty()
                        && headState.getCollisionShape(level, head).isEmpty()
                        && feetState.getFluidState().isEmpty()
                        && headState.getFluidState().isEmpty()) {
                    return new Vec3(x + 0.5D, y, z + 0.5D);
                }
            }
            return null;
        }

        @Override
        public Portal.Transition getLocalTransition() {
            return Portal.Transition.CONFUSION;
        }

        @Override
        public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
            if (random.nextInt(4) != 0) {
                return;
            }
            double x = pos.getX() + random.nextDouble();
            double y = pos.getY() + random.nextDouble();
            double z = pos.getZ() + random.nextDouble();
            level.addParticle(this.particle, x, y, z, 0.0D, 0.02D, 0.0D);
        }
    }

    // ================================================================ 灵引

    /**
     * 灵引 —— 通往某个界域的钥匙。
     *
     * <p>对地面右键，会在面前立起一座小牌坊：左右两根柱子、一道横梁，
     * 中间嵌着这扇门。不消耗、可反复使用 —— 钥匙是用来开门的，
     * 不是用来烧掉的。
     */
    public static class RealmKeyItem extends Item {

        private final RealmGateBlock gate;
        private final Block frame;
        private final String realmZh;
        private final String gateName;

        public RealmKeyItem(RealmGateBlock gate, Block frame, String realmZh, String gateName,
                            Properties properties) {
            super(properties);
            this.gate = gate;
            this.frame = frame;
            this.realmZh = realmZh;
            this.gateName = gateName;
        }

        @Override
        public InteractionResult useOn(UseOnContext context) {
            Player player = context.getPlayer();
            Level level = context.getLevel();
            if (player == null) {
                return InteractionResult.PASS;
            }
            if (level.isClientSide()) {
                return InteractionResult.SUCCESS;
            }
            if (!(level instanceof ServerLevel)) {
                return InteractionResult.PASS;
            }
            BlockPos base = context.getClickedPos().relative(context.getClickedFace());
            Direction look = player.getDirection();
            if (!this.buildGate(level, base, look)) {
                YsText.send(player, "此处立不起门户", List.of(
                        "需要一块 4 格宽、4 格高的空地。",
                        "退后几步，换个开阔的位置再试。"));
                return InteractionResult.SUCCESS_SERVER;
            }
            level.playSound(null, base, SoundEvents.END_PORTAL_SPAWN, SoundSource.BLOCKS, 0.9F, 1.1F);
            YsText.send(player, this.gateName + " · 门户洞开", List.of(
                    "一座" + this.gateName + "立了起来。",
                    "走进去，便去往" + this.realmZh + "；",
                    "从" + this.realmZh + "再走回门里，便能返回主世界。"));
            if (player instanceof ServerPlayer serverPlayer) {
                awardFirstGate(serverPlayer);
            }
            return InteractionResult.SUCCESS_SERVER;
        }

        /**
         * 立一座门。
         *
         * <p>几何：门洞 2 宽 × 3 高，两侧再各站一根 4 格高的柱子，顶上横一道梁 ——
         * 一座极简的牌坊。所有格子都必须是「可替换」的（草、雪、水都行），
         * 有任意一格被实心方块占着就整体拒绝，绝不啃掉玩家的房子。
         */
        private boolean buildGate(Level level, BlockPos base, Direction look) {
            Direction left = look.getCounterClockWise();
            BlockPos c0 = base;
            BlockPos c1 = base.relative(left);
            BlockPos frameL = base.relative(left.getOpposite());
            BlockPos frameR = c1.relative(left);

            List<BlockPos> frameCells = new ArrayList<>();
            for (int i = 0; i <= 3; i++) {
                frameCells.add(frameL.above(i));
                frameCells.add(frameR.above(i));
            }
            frameCells.add(c0.above(3));
            frameCells.add(c1.above(3));

            List<BlockPos> gateCells = new ArrayList<>();
            for (int i = 0; i <= 2; i++) {
                gateCells.add(c0.above(i));
                gateCells.add(c1.above(i));
            }

            for (BlockPos cell : frameCells) {
                if (!level.getBlockState(cell).canBeReplaced()) {
                    return false;
                }
            }
            for (BlockPos cell : gateCells) {
                if (!level.getBlockState(cell).canBeReplaced()) {
                    return false;
                }
            }

            BlockState frameState = this.frame.defaultBlockState();
            for (BlockPos cell : frameCells) {
                level.setBlockAndUpdate(cell, frameState);
            }
            BlockState gateState = this.gate.defaultBlockState();
            for (BlockPos cell : gateCells) {
                level.setBlockAndUpdate(cell, gateState);
            }
            return true;
        }

        private static void awardFirstGate(ServerPlayer player) {
            var server = player.level().getServer();
            if (server == null) {
                return;
            }
            AdvancementHolder holder = server.getAdvancements().get(YishuMod.id("main/first_gate"));
            if (holder != null) {
                player.getAdvancements().award(holder, "gate");
            }
        }
    }

    // ================================================================ 三把钥匙的样板

    /** 造一扇门的粒子色 —— 与钥匙、界面同色系。 */
    public static ParticleOptions astralParticle() {
        return ParticleTypes.END_ROD;
    }

    public static ParticleOptions jadeParticle() {
        return ParticleTypes.HAPPY_VILLAGER;
    }

    public static ParticleOptions inkParticle() {
        return ParticleTypes.SQUID_INK;
    }

    public static ParticleOptions ziweiParticle() {
        return ParticleTypes.END_ROD;
    }

    public static ParticleOptions liurenParticle() {
        return ParticleTypes.BUBBLE;
    }

    public static ParticleOptions qimenParticle() {
        return ParticleTypes.FLAME;
    }

    /** 灵引的公共属性样板。 */    /** 灵引的公共属性样板。 */
    public static Item.Properties keyProps(String name) {
        return new Item.Properties().stacksTo(1).rarity(Rarity.RARE)
                .setId(com.chenchen.yishu.core.YsReg.itemKey(name));
    }
}
