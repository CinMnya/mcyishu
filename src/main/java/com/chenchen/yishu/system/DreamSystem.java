package com.chenchen.yishu.system;

import com.chenchen.yishu.core.StableKey;
import net.fabricmc.fabric.api.entity.event.v1.EntitySleepEvents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Vex;

import java.util.List;

/**
 * 易枢 · 梦境（第 20 轮）。
 *
 * <p>睡醒那一刻起梦：每张床、每个世界、每一天只做一场梦。
 * 梦象由「种子 + 玩家 + 当日」纯函数推出 —— 同一天重睡多少遍，
 * 梦都是那一场，但<b>预兆只有头一次兑现</b>。
 *
 * <ul>
 *   <li><b>吉梦</b>：晨起得吉兆，带buff，另记小功德；</li>
 *   <li><b>凶梦</b>：留下「心魔」—— 当夜心魔化形（Vex）来劫，
 *       一次了结；</li>
 *   <li><b>平梦</b>：只是一场梦，醒来时床边的月亮还在。</li>
 * </ul>
 */
public final class DreamSystem {

    private DreamSystem() {
    }

    /** 检查间隔（刻）—— 心魔在夜里现身。 */
    private static final int INTERVAL = 100;

    private static final String KEY_DREAM = "dream:";
    private static final String KEY_OMEN = "omen:";

    /** 一场梦。 */
    record Dream(String sign, String explain, boolean lucky, boolean ominous) {
    }

    private static final List<Dream> DREAMS = List.of(
            new Dream("梦见白鹤衔书落庭前", "文星入梦，近日所学必有所成。", true, false),
            new Dream("梦见大水漫金山", "财如水至，宜买卖收获。", true, false),
            new Dream("梦见故人踏月而来", "故人念你，善缘将续。", true, false),
            new Dream("梦见麦浪翻金，粒粒饱满", "收成之兆，宜收获作物。", true, false),
            new Dream("梦见身生双翼，扶摇而上", "青云之兆，胆气正旺，宜远行开拓。", true, false),
            new Dream("梦见落花满地，无人扫", "缘起缘灭，不必强留。", false, false),
            new Dream("梦见江上行舟，风平浪静", "平稳一日，按部就班即是。", false, false),
            new Dream("梦见屋后古井无波", "静守为上，今日不宜动土远行。", false, false),
            new Dream("梦见黑影立于床头，不语", "心魔初生，今夜或来相试。", false, true),
            new Dream("梦见坠入深渊，呼救无声", "心结未解，恐有心魔趁夜而来。", false, true),
            new Dream("梦见大火烧尽旧宅", "破而后立，只是过程烫手。今夜小心。", false, true)
    );

    public static void register() {
        EntitySleepEvents.STOP_SLEEPING.register(DreamSystem::onWake);
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_LEVEL_TICK
                .register(DreamSystem::tickLevel);
    }

    // ==================================================================== 起梦

    private static void onWake(net.minecraft.world.entity.LivingEntity entity,
                               net.minecraft.core.BlockPos sleepingPos) {
        if (entity.level().isClientSide() || !(entity instanceof ServerPlayer player)
                || !(entity.level() instanceof ServerLevel level)) {
            return;
        }
        long epochDay = level.getOverworldClockTime() / 24000L;
        String name = player.getGameProfile().name();
        YishuData data = YishuData.of(level);

        String dreamKey = KEY_DREAM + name + ":" + epochDay;
        if (data.get(dreamKey) != null) {
            return; // 一天一场梦
        }
        data.put(dreamKey, "1");

        Dream dream = dreamOf(level, player, epochDay);
        List<String> lines = List.of(
                "§7" + dream.sign(),
                "§f解梦：§b" + dream.explain(),
                dream.lucky() ? "§a此为吉梦。"
                        : dream.ominous() ? "§c此为凶梦，今夜防心魔。"
                        : "§7此为平梦。");
        com.chenchen.yishu.util.YsText.send(player, "梦境", lines);
        level.playSound(null, player.blockPosition(),
                SoundEvents.AMBIENT_CAVE.value(), SoundSource.PLAYERS, 0.6F, 1.6F);

        if (dream.lucky()) {
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 20 * 120, 0));
            player.addEffect(new MobEffectInstance(MobEffects.LUCK, 20 * 180, 0));
            MeritSystem.addMerit(level, player, 5, null);
        } else if (dream.ominous()) {
            data.put(KEY_OMEN + name + ":" + epochDay, "1");
        }
    }

    /** 梦由天定：种子 + 玩家 + 当日。 */
    private static Dream dreamOf(ServerLevel level, ServerPlayer player, long epochDay) {
        int index = StableKey.of(level.getSeed(), "dream")
                .add(player.getGameProfile().name())
                .add(epochDay)
                .random().nextInt(DREAMS.size());
        return DREAMS.get(index);
    }

    // ==================================================================== 心魔

    private static void tickLevel(ServerLevel level) {
        if (level.getLevelData().getGameTime() % INTERVAL != 0L) {
            return;
        }
        boolean night = level.getSkyDarken() >= 10;
        if (!night) {
            return;
        }
        long epochDay = level.getOverworldClockTime() / 24000L;
        for (ServerPlayer player : level.players()) {
            if (player.isSpectator() || !player.isAlive() || player.isCreative()) {
                continue;
            }
            String name = player.getGameProfile().name();
            YishuData data = YishuData.of(level);
            String omenKey = KEY_OMEN + name + ":" + epochDay;
            if (!"1".equals(data.get(omenKey))) {
                continue;
            }
            data.put(omenKey, "done");
            spawnXinMo(level, player, epochDay);
        }
    }

    /** 心魔化形：一至二只 Vex，从上空坠入战场。 */
    private static void spawnXinMo(ServerLevel level, ServerPlayer player, long epochDay) {
        int count = 1 + level.getRandom().nextInt(2);
        for (int i = 0; i < count; i++) {
            Vex vex = net.minecraft.world.entity.EntityTypes.VEX.create(level, EntitySpawnReason.EVENT);
            if (vex == null) {
                continue;
            }
            double angle = level.getRandom().nextDouble() * Math.PI * 2;
            double dist = 8.0D + level.getRandom().nextDouble() * 10.0D;
            vex.snapTo(
                    player.getX() + Math.cos(angle) * dist,
                    player.getY() + 6.0D + level.getRandom().nextDouble() * 4.0D,
                    player.getZ() + Math.sin(angle) * dist,
                    0.0F, 0.0F);
            vex.setCustomName(Component.literal("心魔"));
            vex.setPersistenceRequired();
            level.addFreshEntity(vex);
        }
        player.sendSystemMessage(Component.literal(
                "§5【易枢·心魔】§f昨夜凶梦成真 —— 心魔化形而来，破之则心安。"));
        level.playSound(null, player.blockPosition(),
                SoundEvents.WARDEN_HEARTBEAT, SoundSource.PLAYERS, 1.0F, 0.6F);
        level.sendParticles(ParticleTypes.LARGE_SMOKE,
                player.getX(), player.getY() + 4.0D, player.getZ(),
                20, 3.0D, 2.0D, 3.0D, 0.02D);
    }

    /** 存活的心魔数（供反噬系统或成就扩展用）。 */
    public static int nearbyXinMo(ServerLevel level, ServerPlayer player, double radius) {
        return level.getEntitiesOfClass(Mob.class,
                player.getBoundingBox().inflate(radius),
                mob -> "心魔".equals(mob.getName().getString())).size();
    }
}
