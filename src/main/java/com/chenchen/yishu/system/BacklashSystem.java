package com.chenchen.yishu.system;

import com.chenchen.yishu.YishuConfig;
import com.chenchen.yishu.dimension.DimensionProfile;
import com.chenchen.yishu.liuyao.Divination;
import com.chenchen.yishu.util.YsText;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * 易枢 · 反噬 —— 问得太多的代价。
 *
 * <p>占卜系统最容易出的设计事故是「变成雷达」：玩家几分钟问一次卦，
 * 把整个地图的矿脉、怪点、遗迹全刷出来，游戏就只剩下执行清单了。
 *
 * <p>所以这里设一道门槛：每日问卦有上限，超限之后惩罚依次加重。
 * 五档反噬在 {@link Divination.Backlash} 里定义，本类负责把它「落实」到世界上：
 *
 * <table border="1">
 *   <tr><th>档位</th><th>表现</th></tr>
 *   <tr><td>信息模糊</td><td>无实体惩罚，但解卦线索大量丢失（在起卦流程里处理）</td></tr>
 *   <tr><td>伪卦</td><td>无实体惩罚，但所得之卦与你问的事无关（在起卦流程里处理）</td></tr>
 *   <tr><td>天雷</td><td>脚下落雷</td></tr>
 *   <tr><td>失明</td><td>失明 + 反胃</td></tr>
 *   <tr><td>引怪</td><td>在附近生成一批敌对生物</td></tr>
 * </table>
 *
 * <p><b>为什么是这些惩罚</b>：全部是短时、可恢复、不掉东西的。
 * 反噬的目的是提醒「你问得太勤了」，不是毁掉玩家的存档。
 * 一个会让人丢装备的惩罚机制，最后只会让人关掉这个模组。
 */
public final class BacklashSystem {

    private BacklashSystem() {
    }

    /**
     * 施加一次反噬。
     *
     * @param level    世界
     * @param player   受罚者
     * @param backlash 档位
     */
    public static void apply(ServerLevel level, Player player, Divination.Backlash backlash) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        if (!YishuConfig.get().backlashEnabled) {
            return;
        }
        BlockPos pos = player.blockPosition();
        DimensionProfile dimension = DimensionProfile.of(level.dimension().identifier().toString());

        switch (backlash) {
            case NONE -> {
                return;
            }
            case VAGUE -> {
                level.sendParticles(ParticleTypes.SMOKE, player.getX(), player.getY() + 1.0D, player.getZ(),
                        24, 0.6D, 0.8D, 0.6D, 0.02D);
                YsText.chat(serverPlayer, "卦气隐没，你只看到了一个轮廓。");
            }
            case FALSE_GUA -> {
                level.sendParticles(ParticleTypes.SQUID_INK, player.getX(), player.getY() + 1.0D, player.getZ(),
                        30, 0.5D, 0.6D, 0.5D, 0.03D);
                YsText.chat(serverPlayer, "所得之卦与你所问之事并无干系 —— 它被错答了。");
            }
            case THUNDER -> {
                strike(level, player);
                YsText.chat(serverPlayer, "天机不可多泄。");
            }
            case BLIND -> {
                player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 20 * 12, 0));
                player.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 20 * 8, 0));
                strike(level, player);
                YsText.chat(serverPlayer, "卦气夺目，你暂时看不见了。");
            }
            case HORDE -> {
                summonHorde(level, player, dimension);
                YsText.chat(serverPlayer, "你的卦象外泄，有东西循着它找来了。");
            }
            default -> {
            }
        }
    }

    /** 落雷。脚下那一格，不点燃玩家。 */
    private static void strike(ServerLevel level, Player player) {
        BlockPos target = player.blockPosition();
        if (!level.canSeeSkyFromBelowWater(target)) {
            // 室内放一道闪电音效与粒子，不至于把房子点着
            level.playSound(null, target, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER, 0.7F, 1.4F);
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, player.getX(), player.getY() + 2.0D, player.getZ(),
                    40, 0.5D, 1.0D, 0.5D, 0.15D);
            player.hurtServer(level, level.damageSources().magic(), 4.0F);
            return;
        }
        net.minecraft.world.entity.Entity lightning =
                EntityTypes.LIGHTNING_BOLT.create(level, net.minecraft.world.entity.EntitySpawnReason.TRIGGERED);
        if (lightning != null) {
            lightning.snapTo(net.minecraft.world.phys.Vec3.atBottomCenterOf(target));
            if (lightning instanceof net.minecraft.world.entity.LightningBolt bolt) {
                bolt.setVisualOnly(true);
            }
            level.addFreshEntity(lightning);
        }
    }

    /**
     * 引来一队窥伺者。
     *
     * <p>刷什么怪取决于维度：主世界是僵尸骷髅，下界是烈焰人与猪灵，
     * 末地是末影人。数量固定 4 只，且不会无限追人 —— 它们生成在
     * 玩家 8~14 格外，给玩家一个跑掉的窗口。
     */
    private static void summonHorde(ServerLevel level, Player player, DimensionProfile dimension) {
        EntityType<? extends Mob>[] pool = switch (dimension) {
            case NETHER -> new EntityType[]{
                    EntityTypes.BLAZE, EntityTypes.ZOMBIFIED_PIGLIN, EntityTypes.MAGMA_CUBE};
            case END -> new EntityType[]{EntityTypes.ENDERMAN, EntityTypes.SHULKER};
            default -> new EntityType[]{
                    EntityTypes.ZOMBIE, EntityTypes.SKELETON, EntityTypes.CREEPER};
        };

        for (int i = 0; i < 4; i++) {
            double angle = (Math.PI * 2.0D / 4.0D) * i + level.getRandom().nextDouble() * 0.6D;
            double distance = 8.0D + level.getRandom().nextDouble() * 6.0D;
            double x = player.getX() + Math.cos(angle) * distance;
            double z = player.getZ() + Math.sin(angle) * distance;
            BlockPos spawn = findGround(level, new BlockPos((int) x, player.getBlockY(), (int) z));

            @SuppressWarnings("unchecked")
            EntityType<? extends Mob> type = (EntityType<? extends Mob>) pool[i % pool.length];
            Mob mob = type.create(level, net.minecraft.world.entity.EntitySpawnReason.EVENT);
            if (mob == null) {
                continue;
            }
            mob.snapTo(spawn.getX() + 0.5D, spawn.getY(), spawn.getZ() + 0.5D,
                    level.getRandom().nextFloat() * 360.0F, 0.0F);
            level.addFreshEntity(mob);
            level.sendParticles(ParticleTypes.SCULK_SOUL, spawn.getX() + 0.5D, spawn.getY() + 1.0D,
                    spawn.getZ() + 0.5D, 12, 0.3D, 0.4D, 0.3D, 0.01D);
        }
        level.playSound(null, player.blockPosition(), SoundEvents.WARDEN_NEARBY_CLOSER,
                SoundSource.HOSTILE, 0.6F, 1.2F);
    }

    /** 从上往下找一块能站的地面。 */
    private static BlockPos findGround(ServerLevel level, BlockPos start) {
        BlockPos.MutableBlockPos cursor = start.mutable();
        int minY = level.getMinY();
        for (int i = 0; i < 40 && cursor.getY() > minY; i++) {
            if (!level.getBlockState(cursor).isAir() && level.getBlockState(cursor.above()).isAir()) {
                return cursor.above().immutable();
            }
            cursor.move(0, -1, 0);
        }
        return start;
    }

    /**
     * 统计附近有多少实体（供「引怪」判定是否成功）。
     */
    public static int nearbyHostiles(ServerLevel level, Player player, double radius) {
        AABB box = player.getBoundingBox().inflate(radius);
        List<Mob> mobs = level.getEntitiesOfClass(Mob.class, box);
        return mobs.size();
    }
}
