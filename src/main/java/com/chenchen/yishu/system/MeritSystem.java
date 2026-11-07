package com.chenchen.yishu.system;

import com.chenchen.yishu.YishuMod;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.zombie.ZombieVillager;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.EntityHitResult;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 易枢 · 因果功德（第 20 轮）。
 *
 * <p>「积善之家必有余庆，积不善之家必有余殃」——
 * 这一轮把这句话做成可玩的东西：
 *
 * <ul>
 *   <li><b>善行积德</b>：喂饱牲畜 +2，以金苹果引僵尸村民回头 +10，
 *       凶梦醒后仍守本心（不迁怒）也另有小功；</li>
 *   <li><b>恶行损德</b>：滥杀村民、灵兽与牲畜 −6；</li>
 *   <li><b>福报</b>：功德 ≥ 200 者常怀幸运；</li>
 *   <li><b>雷劫</b>：阴德 ≤ −100 者在雷雨里要小心天打雷劈。</li>
 * </ul>
 *
 * <p>数值存 {@link YishuData}（键 {@code merit:<玩家>}）。功德是修行，
 * 不随重进游戏清零 —— 所以它进存档，跟「同种子同结果」的纯函数体系
 * 分开：命由天定，德由己修。
 */
public final class MeritSystem {

    private MeritSystem() {
    }

    private static final String KEY_MERIT = "merit:";

    /** 检查间隔（刻）。 */
    private static final int INTERVAL = 100;
    /** 雷劫概率基数：每次检查 1/N。 */
    private static final int THUNDER_DENOMINATOR = 40;

    /** 每个玩家上次播报过的功德阶位（跨阶才播报，免得刷屏）。 */
    private static final Map<String, Integer> LAST_TIER = new HashMap<>();

    public static void register() {
        UseEntityCallback.EVENT.register(MeritSystem::onUseEntity);
        ServerLivingEntityEvents.AFTER_DEATH.register(MeritSystem::onDeath);
        ServerTickEvents.END_LEVEL_TICK.register(MeritSystem::tickLevel);
    }

    // ==================================================================== 善恶

    /** 喂养牲畜 / 点化僵尸村民。 */
    private static net.minecraft.world.InteractionResult onUseEntity(Player player,
                                                                     net.minecraft.world.level.Level level,
                                                                     InteractionHand hand,
                                                                     Entity entity,
                                                                     EntityHitResult hit) {
        if (level.isClientSide() || !(player instanceof ServerPlayer serverPlayer)) {
            return net.minecraft.world.InteractionResult.PASS;
        }
        ItemStack stack = player.getItemInHand(hand);
        if (stack.isEmpty()) {
            return net.minecraft.world.InteractionResult.PASS;
        }

        // 金苹果点化僵尸村民：大功德
        if (entity instanceof ZombieVillager && stack.is(net.minecraft.world.item.Items.GOLDEN_APPLE)) {
            addMerit((ServerLevel) level, serverPlayer, 10,
                    "以金引阳，僵尸村民有望复为人身。功德 +10");
            return net.minecraft.world.InteractionResult.PASS;
        }

        // 喂饱牲畜（喂养成功与否原版说了算，这里只记心意的份）
        if (entity instanceof Animal animal && animal.isFood(stack) && animal.getAge() == 0) {
            addMerit((ServerLevel) level, serverPlayer, 2, null);
        }
        return net.minecraft.world.InteractionResult.PASS;
    }

    /** 滥杀无辜：村民、铁傀儡与牲畜。 */
    private static void onDeath(LivingEntity entity, net.minecraft.world.damagesource.DamageSource source) {
        if (!(source.getEntity() instanceof ServerPlayer killer)) {
            return;
        }
        int loss = 0;
        if (entity instanceof Villager || entity instanceof IronGolem) {
            loss = -6;
        } else if (com.chenchen.yishu.spirit.YsMobs.isModdedSpirit(entity)) {
            // 灵兽（模组生物）通灵性，杀之损德
            loss = -6;
        } else if (entity instanceof Animal) {
            loss = -2;
        }
        if (loss != 0) {
            addMerit((ServerLevel) entity.level(), killer, loss, null);
        }
    }

    // ==================================================================== 周期：福报与雷劫

    private static void tickLevel(ServerLevel level) {
        if (level.getLevelData().getGameTime() % INTERVAL != 0L) {
            return;
        }
        for (ServerPlayer player : level.players()) {
            if (player.isSpectator() || !player.isAlive()) {
                continue;
            }
            long merit = meritOf(level, player);
            int tier = tierOf(merit);
            announceTier(level, player, merit, tier);

            // 福报：积善者常怀幸运
            if (merit >= 200) {
                player.addEffect(new MobEffectInstance(MobEffects.LUCK, INTERVAL * 3, 0,
                        true, false, true));
            } else if (merit <= -50) {
                player.addEffect(new MobEffectInstance(MobEffects.UNLUCK, INTERVAL * 3, 0,
                        true, false, true));
            }

            // 雷劫：大恶之人在雷雨里要小心
            if (merit <= -100 && level.isThundering() && !player.isCreative()) {
                if (level.getRandom().nextInt(THUNDER_DENOMINATOR) == 0) {
                    LightningBolt bolt = new LightningBolt(
                            net.minecraft.world.entity.EntityTypes.LIGHTNING_BOLT, level);
                    bolt.snapTo(player.getX(), player.getY(), player.getZ(), 0.0F, 0.0F);
                    bolt.setCause(player);
                    level.addFreshEntity(bolt);
                    player.sendSystemMessage(Component.literal(
                            "§d【易枢·因果】§f一道天雷循着业力劈下 —— 阴德深重，天怒人怨。"));
                }
            }
        }
    }

    // ==================================================================== 阶位播报

    private static int tierOf(long merit) {
        if (merit >= 500) {
            return 4;
        }
        if (merit >= 200) {
            return 3;
        }
        if (merit >= 50) {
            return 2;
        }
        if (merit >= -49) {
            return 1;
        }
        if (merit >= -99) {
            return 0;
        }
        return -1;
    }

    private static String tierName(int tier) {
        return switch (tier) {
            case 4 -> "§e功德圆满";
            case 3 -> "§6福报深厚";
            case 2 -> "§a善缘渐结";
            case 1 -> "§7因果寻常";
            case 0 -> "§c恶名渐著";
            default -> "§4业火缠身";
        };
    }

    private static void announceTier(ServerLevel level, ServerPlayer player, long merit, int tier) {
        String name = player.getGameProfile().name();
        Integer last = LAST_TIER.get(name);
        if (last != null && last == tier) {
            return;
        }
        LAST_TIER.put(name, tier);
        player.sendSystemMessage(Component.literal(
                "§d【易枢·因果】§f" + tierName(tier) + " §7（功德 " + merit + "）"));
        level.playSound(null, player.blockPosition(),
                tier >= 2 ? SoundEvents.PLAYER_LEVELUP : SoundEvents.WARDEN_HEARTBEAT,
                SoundSource.PLAYERS, 0.6F, tier >= 2 ? 1.4F : 0.7F);
    }

    // ==================================================================== 读写

    /** 当前功德值。 */
    public static long meritOf(ServerLevel level, Player player) {
        YishuData data = YishuData.of(level);
        String raw = data.get(KEY_MERIT + player.getGameProfile().name());
        if (raw == null) {
            return 0L;
        }
        try {
            return Long.parseLong(raw);
        } catch (NumberFormatException ignored) {
            return 0L;
        }
    }

    /** 记一笔功德；{@code note} 非空时同步在聊天栏提示。 */
    public static void addMerit(ServerLevel level, ServerPlayer player, long delta, String note) {
        YishuData data = YishuData.of(level);
        long merit = meritOf(level, player) + delta;
        data.put(KEY_MERIT + player.getGameProfile().name(), Long.toString(merit));
        if (note != null) {
            player.sendSystemMessage(Component.literal("§d【易枢·因果】§f" + note));
            level.sendParticles(ParticleTypes.HEART,
                    player.getX(), player.getY() + 2.2D, player.getZ(),
                    3, 0.4D, 0.3D, 0.4D, 0.0D);
        }
    }
}
