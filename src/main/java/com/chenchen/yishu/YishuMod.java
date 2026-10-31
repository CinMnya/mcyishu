package com.chenchen.yishu;

import com.chenchen.yishu.building.BuildingFate;
import com.chenchen.yishu.dimension.DimensionProfile;
import com.chenchen.yishu.fengshui.FengShuiField;
import com.chenchen.yishu.fengshui.FengShuiSystems;
import com.chenchen.yishu.network.YsNetworking;
import com.chenchen.yishu.registry.YsRegistry;
import com.chenchen.yishu.spirit.MobFate;
import com.chenchen.yishu.system.PlainSpeak;
import com.chenchen.yishu.system.TutorialSystem;
import com.chenchen.yishu.system.YishuData;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 易枢 —— 主入口。
 *
 * <p>这个类做三件事：<b>注册内容</b>、<b>接上事件</b>、<b>跑周期系统</b>。
 *
 * <p>初始化顺序有讲究，不能随便调：
 * <ol>
 *   <li>配置（后面所有系统都要读它）</li>
 *   <li>物品与方块（注册表内容）</li>
 *   <li>网络通道（必须在玩家连接之前）</li>
 *   <li>世界系统（tick 钩子）</li>
 * </ol>
 *
 * <p>每个注册点单独 try/catch：任何一个模块在某个小版本上失效，
 * 也只让那一个功能消失，而不是让整个游戏起不来。
 * 这条纪律在 26.x 这种快速演进的版本线上尤其值钱。
 */
public class YishuMod implements ModInitializer {

    public static final String MOD_ID = "yishu";
    public static final Logger LOGGER = LoggerFactory.getLogger("Yishu");

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        LOGGER.info("[Yishu] 易枢加载中 —— 命理、风水、六爻三盘将起。");

        attempt("配置", YishuConfig::get);
        attempt("灵兽", com.chenchen.yishu.spirit.YsMobs::register);
        attempt("物品与方块", YsRegistry::bootstrap);
        attempt("村民职业", com.chenchen.yishu.integration.YsVillagerContent::register);
        attempt("网络通道", YsNetworking::registerCommon);
        attempt("玩家命盘", YishuSystems::register);
        attempt("生物命理", MobFateSystem::register);
        attempt("风水效果", FengShuiEffectSystem::register);
        attempt("每日宜忌", DailyAlmanacSystem::register);
        attempt("新手引导", TutorialSystem::register);
        attempt("原版整合", com.chenchen.yishu.integration.VanillaIntegration::register);
        attempt("指令", com.chenchen.yishu.command.YishuCommand::register);
        attempt("主题变种", com.chenchen.yishu.integration.ThemedVariants::register);

        LOGGER.info("[Yishu] 加载完成。同一个种子下的每一卦、每一张命盘、每一处风水，都将是唯一的。");
    }

    private static void attempt(String name, Runnable action) {
        try {
            action.run();
        } catch (Throwable throwable) {
            LOGGER.error("[Yishu] {} 注册失败（其余模块继续加载）", name, throwable);
        }
    }

    // ==================================================================== 玩家命盘

    /**
     * 玩家命盘系统。
     *
     * <p>玩家一进服就把种子推给客户端，并在聊天栏报出当日黄历。
     * 之所以要推种子：客户端的命盘、神煞、大运、黄历全部是本地算的，
     * 有了种子就不需要任何网络往返 —— 界面响应零延迟。
     */
    public static final class YishuSystems {

        private YishuSystems() {
        }

        public static void register() {
            ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
                ServerPlayer player = handler.getPlayer();
                if (player.level() instanceof ServerLevel level) {
                    YsNetworking.onJoin(player, level);
                }
            });
        }
    }

    // ==================================================================== 生物命理

    /**
     * 生物命理 → AI 权重。
     *
     * <p>这是「命的六爻状态影响 AI」这条规范的实现。做法是：
     * 每隔 {@value #AI_INTERVAL} 刻，给玩家周围一定范围内的生物重新挂一次
     * 属性修正器（AttributeModifier），值由它自己的命盘算出。
     *
     * <p>为什么用属性修正器而不是直接改 AI 目标：
     * <ul>
     *   <li>属性是原版机制，任何模组的怪都吃；</li>
     *   <li>用固定的 {@code Identifier} 做修正器 id，重复挂只更新不叠加，
     *       不会出现「越跑越快」的经典事故；</li>
     *   <li>玩家一走出范围就把它摘掉，性能与体验都好。</li>
     * </ul>
     *
     * <p>权重的含义见 {@link MobFate.Profile}：
     * 官鬼旺 → 速度与攻击上调（更激进）；子孙旺 → 速度与追击范围下调（更保守）；
     * 兄弟旺 → 移动速度额外上调（更爱集结）；父母旺 → 护甲上调。
     */
    public static final class MobFateSystem {

        /** 每隔 40 刻（2 秒）重算一次，够用且不费。 */
        public static final int AI_INTERVAL = 40;
        /** 处理半径。 */
        public static final double RADIUS = 42.0D;
        /** 单次处理上限，避免在怪物海里卡住主线程。 */
        public static final int BATCH_LIMIT = 96;

        private static final Identifier SPEED_ID = id("liuyao/speed");
        private static final Identifier ATTACK_ID = id("liuyao/attack");
        private static final Identifier RANGE_ID = id("liuyao/range");

        private MobFateSystem() {
        }

        public static void register() {
            ServerTickEvents.END_LEVEL_TICK.register(MobFateSystem::tickLevel);
        }

        private static void tickLevel(ServerLevel level) {
            if (level.getLevelData().getGameTime() % AI_INTERVAL != 0L) {
                return;
            }
            if (!YishuConfig.get().mobFateEnabled) {
                return;
            }
            DimensionProfile dimension = DimensionProfile.of(level.dimension().identifier().toString());
            if (!YishuConfig.get().forDimension(dimension.shortKey()).fate) {
                return;
            }

            List<ServerPlayer> players = level.players();
            if (players.isEmpty()) {
                return;
            }
            int budget = BATCH_LIMIT;

            for (Player player : players) {
                if (budget <= 0) {
                    break;
                }
                AABB box = player.getBoundingBox().inflate(RADIUS);
                for (Mob mob : level.getEntitiesOfClass(Mob.class, box)) {
                    if (budget-- <= 0) {
                        break;
                    }
                    if (!MobFate.isSubject(mob) || !MobFate.enabled(mob)) {
                        continue;
                    }
                    apply(level, mob);
                }
            }
        }

        /** 给一只生物挂上它自己的命理权重。 */
        private static void apply(ServerLevel level, Mob mob) {
            MobFate.Profile profile = MobFate.profileOf(level.getSeed(), mob);

            double aggression = profile.aggression() - 1.0D;
            double caution = profile.caution() - 1.0D;
            double cohesion = profile.cohesion() - 1.0D;
            double resilience = profile.resilience() - 1.0D;

            // 速度：激进 + 集结 − 谨慎
            double speed = aggression * 0.10D + cohesion * 0.06D - caution * 0.08D;
            setModifier(mob.getAttribute(Attributes.MOVEMENT_SPEED), SPEED_ID,
                    clamp(speed, -0.18D, 0.22D), AttributeModifier.Operation.ADD_MULTIPLIED_BASE);

            // 攻击：只看官鬼
            damageOrNull(mob.getAttribute(Attributes.ATTACK_DAMAGE), ATTACK_ID,
                    clamp(aggression * 0.22D, -0.15D, 0.35D));

            // 追击范围：激进 +，谨慎 −
            setModifier(mob.getAttribute(Attributes.FOLLOW_RANGE), RANGE_ID,
                    clamp(aggression * 0.20D - caution * 0.18D, -0.25D, 0.35D),
                    AttributeModifier.Operation.ADD_MULTIPLIED_BASE);

            // 护甲：父母旺则厚
            damageOrNull(mob.getAttribute(Attributes.ARMOR), id("liuyao/armor"),
                    clamp(resilience * 0.30D, 0.0D, 0.45D));
        }

        private static void damageOrNull(AttributeInstance instance, Identifier id, double amount) {
            if (instance == null) {
                return;
            }
            setModifier(instance, id, amount, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        }

        private static void setModifier(AttributeInstance instance, Identifier id, double amount,
                                        AttributeModifier.Operation operation) {
            if (instance == null) {
                return;
            }
            // 用固定 id：重复挂只更新数值，不会叠加
            instance.addOrUpdateTransientModifier(new AttributeModifier(id, amount, operation));
        }

        private static double clamp(double value, double min, double max) {
            return Math.max(min, Math.min(max, value));
        }
    }

    // ==================================================================== 风水效果

    /**
     * 风水效果落地。
     *
     * <p>规范里定的效果分「大吉」与「大凶」两类。这里实现其中不需要改动
     * 原版逻辑的那部分 —— 也就是通过原版效果（状态效果）来体现的那些：
     *
     * <ul>
     *   <li><b>幸运提升</b> → {@code LUCK} 状态效果；</li>
     *   <li><b>恢复上升/下降</b> → {@code REGENERATION}（吉）或 {@code WEAKNESS} 类的debuff（凶）；</li>
     *   <li><b>夜晚出阴煞</b> → 夜里在凶地会得到 {@code DARKNESS} 与轻微伤害。</li>
     * </ul>
     *
     * <p>作物加速与刷怪增减需要挂钩原版生长与生成逻辑，属于下一阶段；
     * 这一版先靠状态效果把「这块地对我好不好」这件事做扎实 ——
     * 玩家能<b>感觉到</b>差别，是这一层最重要的目标。
     */
    public static final class FengShuiEffectSystem {

        /** 检查间隔（刻）。 */
        public static final int INTERVAL = 60;

        private FengShuiEffectSystem() {
        }

        public static void register() {
            ServerTickEvents.END_LEVEL_TICK.register(FengShuiEffectSystem::tickLevel);
        }

        private static void tickLevel(ServerLevel level) {
            if (level.getLevelData().getGameTime() % INTERVAL != 0L) {
                return;
            }
            DimensionProfile dimension = DimensionProfile.of(level.dimension().identifier().toString());
            if (!FengShuiSystems.enabled(dimension)) {
                return;
            }
            long seed = level.getSeed();
            long epochDay = level.getOverworldClockTime() / 24000L;
            boolean grace = TutorialSystem.inGracePeriod(epochDay);

            for (ServerPlayer player : level.players()) {
                if (player.isSpectator() || !player.isAlive()) {
                    continue;
                }
                FengShuiField.Sample sample = FengShuiField.sample(seed, dimension,
                        player.blockPosition().getX(), player.blockPosition().getZ());
                FengShuiField.Grade grade = sample.grade(dimension);
                FengShuiSystems.Effects effects = FengShuiSystems.Effects.of(grade);

                // 香炉净化：附近有燃香则把评级往上抬一档
                if (IncenseBonus.isIncensed(level, player.blockPosition())) {
                    effects = FengShuiSystems.Effects.of(IncenseBonus.upgrade(grade));
                }

                // 龙脉加成
                long year = com.chenchen.yishu.fate.Yun.age(epochDay);
                double aura = FengShuiSystems.DragonVein.auraStrength(seed, dimension, year,
                        player.blockPosition().getX(), player.blockPosition().getZ());
                boolean claimed = aura > 0.0D && IncenseBonus.claimedBy(player, level, dimension, year);

                apply(level, player, effects, grade, aura, claimed, dimension, grace);

                // ---- 第一层直感：地面粒子 + 屏幕边框颜色 ----
                ambience(level, player, grade, epochDay);
            }
        }

        /**
         * 视觉反馈：吉地金色粒子、凶地黑色粒子，并把颜色推给客户端画边框。
         *
         * <p>规范第五节"即时反馈"里最重要的一条：玩家<b>不看文字</b>也该知道
         * 这地方好不好。粒子在脚下、边框在屏幕边，都是被动接收的信息。
         */
        private static void ambience(ServerLevel level, ServerPlayer player,
                                     FengShuiField.Grade grade, long epochDay) {
            YsNetworking.sendAmbience(player, grade == FengShuiField.Grade.PLAIN
                    ? 0 : PlainSpeak.gradeColor(grade));

            if (!YishuConfig.get().groundParticles || grade == FengShuiField.Grade.PLAIN) {
                return;
            }
            // 每 100 刻（5 秒）撒一小撮，既有存在感又不至于变成粒子海
            if (level.getLevelData().getGameTime() % 100L != 0L) {
                return;
            }
            switch (grade) {
                case GREAT_LUCK, LUCK -> level.sendParticles(
                        net.minecraft.core.particles.ParticleTypes.END_ROD,
                        player.getX(), player.getY() + 0.2D, player.getZ(),
                        8, 0.5D, 0.1D, 0.5D, 0.01D);
                case BAD, DIRE -> level.sendParticles(
                        net.minecraft.core.particles.ParticleTypes.SMOKE,
                        player.getX(), player.getY() + 0.2D, player.getZ(),
                        8, 0.5D, 0.1D, 0.5D, 0.01D);
                default -> {
                }
            }
        }

        private static void apply(ServerLevel level, ServerPlayer player,
                                  FengShuiSystems.Effects effects, FengShuiField.Grade grade,
                                  double aura, boolean claimed, DimensionProfile dimension,
                                  boolean grace) {
            // ---- 吉：幸运与恢复 ----
            if (effects.luck() > 1.05D) {
                int amplifier = effects.luck() > 1.25D ? 1 : 0;
                player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        net.minecraft.world.effect.MobEffects.LUCK, INTERVAL * 3, amplifier,
                        true, false, true));
            }
            if (effects.regeneration() > 1.05D) {
                player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        net.minecraft.world.effect.MobEffects.REGENERATION, INTERVAL * 2, 0,
                        true, false, true));
            }

            // ---- 龙脉：加成再叠一层 ----
            if (aura > 0.35D) {
                int amplifier = claimed ? 1 : 0;
                player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        net.minecraft.world.effect.MobEffects.HASTE, INTERVAL * 3, amplifier,
                        true, false, true));
            }

            // ---- 凶：夜晚阴煞（新手期只给短时的，不减血）----
            boolean night = level.getSkyDarken() >= 10;
            int debuffTicks = grace ? 60 : INTERVAL * 2;
            if (grade == FengShuiField.Grade.DIRE && night) {
                player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        net.minecraft.world.effect.MobEffects.DARKNESS, debuffTicks, 0,
                        true, false, true));
                // 新手期不掉血 —— 容错设计：风水差只是减益，不是不能玩
                if (!grace && level.getLevelData().getGameTime() % 200L == 0L) {
                    player.hurtServer(level, level.damageSources().magic(), 1.0F);
                }
            } else if (grade == FengShuiField.Grade.BAD && night) {
                player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        net.minecraft.world.effect.MobEffects.WEAKNESS, debuffTicks, 0,
                        true, false, true));
            }

            // ---- 太岁：在太岁方位动土招煞 ----
            if (YishuConfig.get().taiSuiEnabled) {
                long year = com.chenchen.yishu.fate.Yun.age(level.getOverworldClockTime() / 24000L);
                if (FengShuiSystems.TaiSui.inTaiSuiSector(year,
                        player.blockPosition().getX(), player.blockPosition().getZ())
                        && !com.chenchen.yishu.content.YsBlockContent.Warding.isWarded(
                                level, player.blockPosition())) {
                    if (!grace) {
                        player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                                net.minecraft.world.effect.MobEffects.MINING_FATIGUE, INTERVAL * 2, 0,
                                true, false, true));
                    }
                }
            }
        }
    }

    /** 香炉净化的查询。 */
    public static final class IncenseBonus {

        private IncenseBonus() {
        }

        /** 16 格内有已上香的香炉即为已净化。 */
        public static boolean isIncensed(ServerLevel level, net.minecraft.core.BlockPos pos) {
            for (int dx = -16; dx <= 16; dx += 4) {
                for (int dy = -6; dy <= 6; dy += 2) {
                    for (int dz = -16; dz <= 16; dz += 4) {
                        net.minecraft.core.BlockPos probe = pos.offset(dx, dy, dz);
                        var state = level.getBlockState(probe);
                        if (state.getBlock() instanceof com.chenchen.yishu.content.YsBlockContent.Censer
                                && state.getValue(com.chenchen.yishu.content.YsBlockContent.Censer.INCENSE) > 0) {
                            return true;
                        }
                    }
                }
            }
            return false;
        }

        /** 净化把吉凶等级抬一档。 */
        public static FengShuiField.Grade upgrade(FengShuiField.Grade grade) {
            return switch (grade) {
                case DIRE -> FengShuiField.Grade.BAD;
                case BAD -> FengShuiField.Grade.PLAIN;
                case PLAIN -> FengShuiField.Grade.LUCK;
                default -> grade;
            };
        }

        /** 玩家脚下这段龙脉是不是他占领的。 */
        public static boolean claimedBy(ServerPlayer player, ServerLevel level,
                                        DimensionProfile dimension, long year) {
            YishuData data = YishuData.of(level);
            int node = FengShuiSystems.DragonVein.nodeIndexOf(level.getSeed(), dimension, year,
                    player.blockPosition().getX(), player.blockPosition().getZ());
            String owner = data.veinOwner(dimension.shortKey(), year, node);
            return owner != null && owner.equals(player.getGameProfile().name());
        }
    }

    // ==================================================================== 每日宜忌

    /**
     * 每日宜忌播报。
     *
     * <p>每当游戏日翻页，就在聊天栏报一次当天的黄历。
     * 这个是「给世界一个节奏」的机制里最便宜也最有效的一环 ——
     * 玩家每天早上打开游戏，会先看一眼今天宜什么、忌什么。
     */
    public static final class DailyAlmanacSystem {

        /** 每个玩家上次播报过的游戏日。 */
        private static final Map<String, Long> LAST_ANNOUNCED = new HashMap<>();

        private DailyAlmanacSystem() {
        }

        public static void register() {
            ServerTickEvents.END_LEVEL_TICK.register(DailyAlmanacSystem::tickLevel);
            ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
                    LAST_ANNOUNCED.remove(handler.getPlayer().getGameProfile().name()));
        }

        private static void tickLevel(ServerLevel level) {
            if (!YishuConfig.get().dailyAlmanacEnabled) {
                return;
            }
            // 只在主世界播报，避免三个维度各报一遍
            if (!level.dimension().equals(net.minecraft.world.level.Level.OVERWORLD)) {
                return;
            }
            long epochDay = level.getOverworldClockTime() / 24000L;
            DimensionProfile dimension = DimensionProfile.OVERWORLD;

            for (ServerPlayer player : level.players()) {
                String name = player.getGameProfile().name();
                Long last = LAST_ANNOUNCED.get(name);
                if (last != null && last == epochDay) {
                    continue;
                }
                LAST_ANNOUNCED.put(name, epochDay);

                var almanac = FengShuiSystems.Almanac.ofDay(level.getSeed(), dimension, epochDay);
                player.sendSystemMessage(Component.literal(
                        "§b【易枢·黄历】§f第 " + epochDay + " 日　"
                                + com.chenchen.yishu.fate.Yun.liuRi(level.getSeed(),
                                dimension.shortKey(), epochDay).zh() + " 日"));
                player.sendSystemMessage(Component.literal("§7  " + FengShuiSystems.Almanac.summary(almanac)));
                player.sendSystemMessage(Component.literal("§7  太岁在 "
                        + FengShuiSystems.TaiSui.directionName(com.chenchen.yishu.fate.Yun.age(epochDay))
                        + "，于该方位动土需立石敢当。"));
            }
        }
    }

    // ==================================================================== 通用工具

    /** 生物名（用于窥命符显示）。 */
    public static String entityName(LivingEntity entity) {
        return BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).getPath();
    }

    /** 建筑命盘的一行摘要（供 HUD 或调试）。 */
    public static String buildingLine(long seed, DimensionProfile dimension, int x, int z, long epochDay) {
        BuildingFate.Fate fate = BuildingFate.fateOf(seed, dimension,
                BuildingFate.BuildingType.VILLAGE, x, z);
        return fate.type().zh() + "：" + BuildingFate.dailyState(seed, fate, epochDay).zh();
    }
}
