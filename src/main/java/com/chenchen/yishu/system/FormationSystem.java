package com.chenchen.yishu.system;

import com.chenchen.yishu.dimension.DimensionProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * 易枢 · 阵盘（第 20 轮）。
 *
 * <p>阵盘布下的不是方块，是「势」：阵眼的位置记进存档，
 * 当日有效（次日天亮阵散），阵内之人得阵法之利：
 *
 * <ul>
 *   <li><b>八卦阵</b>：阵内抗性、缓慢回血；邪祟入阵则迟滞受抑；</li>
 *   <li><b>北斗阵</b>：阵内夜视疾行，夜路如白昼；</li>
 *   <li><b>招财阵</b>：阵内幸运 II —— 招财一条归它管。</li>
 * </ul>
 *
 * <p>每人同一时刻只有一座阵：再布新的，旧的即散。
 * 效果走 60 刻一查的周期循环，与风水效果系统同一节奏。
 */
public final class FormationSystem {

    private FormationSystem() {
    }

    private static final String KEY_FORMATION = "formation:";

    /** 阵的半径。 */
    public static final int RADIUS = 12;
    /** 检查间隔（刻）。 */
    private static final int INTERVAL = 60;

    /** 挂上周期循环。 */
    public static void register() {
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_LEVEL_TICK
                .register(FormationSystem::tickLevel);
    }

    /** 一座已布之阵。 */
    public record Formation(String kind, String dimension, int x, int y, int z, long expiryDay) {

        public boolean isHere(ServerLevel level, BlockPos pos) {
            return this.dimension.equals(level.dimension().identifier().toString())
                    && Math.abs(pos.getX() - this.x) <= RADIUS
                    && Math.abs(pos.getY() - this.y) <= RADIUS
                    && Math.abs(pos.getZ() - this.z) <= RADIUS;
        }

        public boolean expired(long epochDay) {
            return epochDay > this.expiryDay;
        }
    }

    // ==================================================================== 布阵

    /**
     * 布一座阵。
     *
     * @return 反馈文字（成功）；{@code null} = 失败（物品不消耗）
     */
    public static String place(ServerLevel level, ServerPlayer player, BlockPos pos, String kind) {
        long epochDay = level.getOverworldClockTime() / 24000L;
        Formation formation = new Formation(kind,
                level.dimension().identifier().toString(),
                pos.getX(), pos.getY(), pos.getZ(), epochDay);
        YishuData.of(level).put(KEY_FORMATION + player.getGameProfile().name(), encode(formation));

        String name = switch (kind) {
            case "bagua" -> "八卦阵";
            case "beidou" -> "北斗阵";
            case "zhaocai" -> "招财阵";
            default -> "无名之阵";
        };
        return name + "已布下：阵眼 (" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ()
                + ")，方圆 " + RADIUS + " 格，天明即散。";
    }

    /** 某玩家当前有没有站在自己的阵里；阵过期会顺手清掉。 */
    public static Formation activeFormation(ServerLevel level, ServerPlayer player) {
        YishuData data = YishuData.of(level);
        String raw = data.get(KEY_FORMATION + player.getGameProfile().name());
        if (raw == null) {
            return null;
        }
        Formation formation = decode(raw);
        long epochDay = level.getOverworldClockTime() / 24000L;
        if (formation.expired(epochDay)) {
            data.put(KEY_FORMATION + player.getGameProfile().name(), "");
            player.sendSystemMessage(Component.literal("§7【易枢】日轮西沉，你的阵法已散。"));
            return null;
        }
        return formation.isHere(level, player.blockPosition()) ? formation : null;
    }

    // ==================================================================== 周期效果

    public static void tickLevel(ServerLevel level) {
        if (level.getLevelData().getGameTime() % INTERVAL != 0L) {
            return;
        }
        for (ServerPlayer player : level.players()) {
            if (player.isSpectator() || !player.isAlive()) {
                continue;
            }
            Formation formation = activeFormation(level, player);
            if (formation == null) {
                continue;
            }
            BlockPos center = new BlockPos(formation.x(), formation.y(), formation.z());
            switch (formation.kind()) {
                case "bagua" -> {
                    player.addEffect(new MobEffectInstance(
                            MobEffects.RESISTANCE, INTERVAL * 2, 0, true, false, true));
                    player.addEffect(new MobEffectInstance(
                            MobEffects.REGENERATION, INTERVAL * 2, 0, true, false, true));
                    suppressHostiles(level, center);
                }
                case "beidou" -> {
                    player.addEffect(new MobEffectInstance(
                            MobEffects.NIGHT_VISION, INTERVAL * 4, 0, true, false, true));
                    player.addEffect(new MobEffectInstance(
                            MobEffects.SPEED, INTERVAL * 2, 1, true, false, true));
                }
                case "zhaocai" -> player.addEffect(new MobEffectInstance(
                        MobEffects.LUCK, INTERVAL * 4, 1, true, false, true));
                default -> {
                }
            }
        }
    }

    /** 八卦阵镇邪：阵内邪祟迟滞受抑。 */
    private static void suppressHostiles(ServerLevel level, BlockPos center) {
        AABB box = new AABB(center).inflate(RADIUS);
        List<Mob> mobs = level.getEntitiesOfClass(Mob.class, box,
                mob -> mob instanceof Enemy && mob.isAlive());
        for (Mob mob : mobs) {
            mob.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, INTERVAL * 2, 1, true, false, true));
            mob.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, INTERVAL * 2, 1, true, false, true));
        }
    }

    // ==================================================================== 序列化

    private static String encode(Formation f) {
        return f.kind() + "|" + f.dimension() + "|" + f.x() + "|" + f.y() + "|" + f.z()
                + "|" + f.expiryDay();
    }

    private static Formation decode(String raw) {
        if (raw.isBlank()) {
            return new Formation("none", "", 0, 0, 0, 0L);
        }
        String[] parts = raw.split("\\|");
        if (parts.length != 6) {
            return new Formation("none", "", 0, 0, 0, 0L);
        }
        try {
            return new Formation(parts[0], parts[1],
                    Integer.parseInt(parts[2]), Integer.parseInt(parts[3]),
                    Integer.parseInt(parts[4]), Long.parseLong(parts[5]));
        } catch (NumberFormatException ignored) {
            return new Formation("none", "", 0, 0, 0, 0L);
        }
    }
}
