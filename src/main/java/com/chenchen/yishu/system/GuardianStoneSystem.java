package com.chenchen.yishu.system;

import com.chenchen.yishu.registry.YsRegistry;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;

/**
 * 易枢 · 镇物（第 20 轮）。
 *
 * <p>镇宅、避邪、招财、化煞四件事，这里落地其中三件半：
 *
 * <ul>
 *   <li><b>镇宅石</b>：立起之后，方圆 24 格内的邪祟受镇 ——
 *       迟滞、虚弱，邪风进不了家门；</li>
 *   <li><b>化煞碑</b>：方圆 16 格内的凶地被化去一层
 *       （与香炉同一条评级通道，香炉在上、化煞碑在下）；</li>
 *   <li><b>招财</b>归阵盘系统的招财阵管；</li>
 *   <li><b>避邪</b>并入镇宅石（一块石头管两件事，玩家不用记两块石头）。</li>
 * </ul>
 *
 * <p>石头的位置记在 {@link YishuData}（每维度一张清单）。
 * 清单会自我治愈：循环里发现石头被人拆了（或被爆炸轰了），
 * 就把它从清单里划掉 —— 不需要额外的破坏事件也能保持准确。
 */
public final class GuardianStoneSystem {

    private GuardianStoneSystem() {
    }

    /** 镇宅石作用半径。 */
    public static final int WARD_RADIUS = 24;
    /** 化煞碑作用半径。 */
    public static final int NEUTRAL_RADIUS = 16;
    /** 检查间隔（刻）。 */
    private static final int INTERVAL = 80;

    private static final String KEY_WARD = "stones:ward:";
    private static final String KEY_NEUTRAL = "stones:neutral:";

    public static void register() {
        // 记录立石
        UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
            if (level.isClientSide() || !(player instanceof ServerPlayer)) {
                return net.minecraft.world.InteractionResult.PASS;
            }
            ItemStack stack = player.getItemInHand(hand);
            if (!(stack.getItem() instanceof BlockItem blockItem)) {
                return net.minecraft.world.InteractionResult.PASS;
            }
            if (blockItem.getBlock() == YsRegistry.ZHENZHAI_STONE
                    || blockItem.getBlock() == YsRegistry.HUASHA_STONE) {
                String key = blockItem.getBlock() == YsRegistry.ZHENZHAI_STONE
                        ? KEY_WARD : KEY_NEUTRAL;
                BlockPos pos = hit.getBlockPos();
                addStone((ServerLevel) level, key, pos);
                player.sendSystemMessage(Component.literal(
                        blockItem.getBlock() == YsRegistry.ZHENZHAI_STONE
                                ? "§b【易枢·镇物】§f镇宅石已立，方圆 " + WARD_RADIUS
                                + " 格邪祟受镇。"
                                : "§b【易枢·镇物】§f化煞碑已立，方圆 " + NEUTRAL_RADIUS
                                + " 格凶地渐化。"));
            }
            return net.minecraft.world.InteractionResult.PASS;
        });
        // 拆石即刻除名（爆炸轰掉的由周期循环自愈）
        PlayerBlockBreakEvents.AFTER.register((level, player, pos, state, blockEntity) -> {
            if (level instanceof ServerLevel server) {
                removeStone(server, KEY_WARD, pos);
                removeStone(server, KEY_NEUTRAL, pos);
            }
        });
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_LEVEL_TICK
                .register(GuardianStoneSystem::tickLevel);
    }

    // ==================================================================== 周期效果

    private static void tickLevel(ServerLevel level) {
        if (level.getLevelData().getGameTime() % INTERVAL != 0L) {
            return;
        }
        List<BlockPos> wardStones = healList(level, KEY_WARD, YsRegistry.ZHENZHAI_STONE);
        for (BlockPos stone : wardStones) {
            suppress(level, stone);
        }
        // 化煞碑只需保持清单干净（评级通道在 FengShuiEffectSystem 里查）
        healList(level, KEY_NEUTRAL, YsRegistry.HUASHA_STONE);
    }

    /** 镇宅石镇邪：半径内邪祟迟滞、虚弱。 */
    private static void suppress(ServerLevel level, BlockPos stone) {
        AABB box = new AABB(stone).inflate(WARD_RADIUS);
        List<Mob> mobs = level.getEntitiesOfClass(Mob.class, box,
                mob -> mob instanceof Enemy && mob.isAlive());
        for (Mob mob : mobs) {
            mob.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, INTERVAL * 2, 0, true, false, true));
            mob.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, INTERVAL * 2, 0, true, false, true));
        }
        if (level.getLevelData().getGameTime() % (INTERVAL * 10) == 0L && !mobs.isEmpty()) {
            level.sendParticles(ParticleTypes.GLOW, stone.getX() + 0.5D,
                    stone.getY() + 1.2D, stone.getZ() + 0.5D, 4, 0.3D, 0.3D, 0.3D, 0.0D);
        }
    }

    // ==================================================================== 化煞查询（供风水效果系统调用）

    /**
     * 附近有没有化煞碑。
     *
     * <p>清单里记的每块碑都现场验明正身，不在了就顺手划掉。
     */
    public static boolean neutralized(ServerLevel level, BlockPos pos) {
        for (BlockPos stone : healList(level, KEY_NEUTRAL, YsRegistry.HUASHA_STONE)) {
            if (stone.closerThan(pos, NEUTRAL_RADIUS)) {
                return true;
            }
        }
        return false;
    }

    // ==================================================================== 清单读写

    private static List<BlockPos> stones(ServerLevel level, String key) {
        List<BlockPos> result = new ArrayList<>();
        String raw = YishuData.of(level).get(key + level.dimension().identifier());
        if (raw == null || raw.isBlank()) {
            return result;
        }
        for (String part : raw.split(";")) {
            String[] xyz = part.split(",");
            if (xyz.length != 3) {
                continue;
            }
            try {
                result.add(new BlockPos(Integer.parseInt(xyz[0]),
                        Integer.parseInt(xyz[1]), Integer.parseInt(xyz[2])));
            } catch (NumberFormatException ignored) {
                // 坏条目直接丢
            }
        }
        return result;
    }

    private static void saveStones(ServerLevel level, String key, List<BlockPos> list) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) {
                builder.append(';');
            }
            BlockPos pos = list.get(i);
            builder.append(pos.getX()).append(',').append(pos.getY()).append(',').append(pos.getZ());
        }
        YishuData.of(level).put(key + level.dimension().identifier(), builder.toString());
    }

    private static void addStone(ServerLevel level, String key, BlockPos pos) {
        List<BlockPos> list = stones(level, key);
        if (!list.contains(pos)) {
            list.add(pos);
            saveStones(level, key, list);
        }
    }

    private static void removeStone(ServerLevel level, String key, BlockPos pos) {
        List<BlockPos> list = stones(level, key);
        if (list.remove(pos)) {
            saveStones(level, key, list);
        }
    }

    /** 清单自愈：把已经不再是这块石头的条目划掉。 */
    private static List<BlockPos> healList(ServerLevel level, String key, net.minecraft.world.level.block.Block block) {
        List<BlockPos> list = stones(level, key);
        boolean changed = list.removeIf(pos ->
                !level.getBlockState(pos).is(block));
        if (changed) {
            saveStones(level, key, list);
        }
        return list;
    }
}
