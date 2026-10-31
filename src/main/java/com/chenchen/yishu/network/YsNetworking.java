package com.chenchen.yishu.network;

import com.chenchen.yishu.YishuConfig;
import com.chenchen.yishu.YishuMod;
import com.chenchen.yishu.building.BuildingFate;
import com.chenchen.yishu.building.Ruins;
import com.chenchen.yishu.content.YsItemContent;
import com.chenchen.yishu.dimension.DimensionProfile;
import com.chenchen.yishu.fate.FateProfile;
import com.chenchen.yishu.fate.Yun;
import com.chenchen.yishu.fengshui.FengShuiField;
import com.chenchen.yishu.liuyao.Divination;
import com.chenchen.yishu.system.BacklashSystem;
import com.chenchen.yishu.system.PlainSpeak;
import com.chenchen.yishu.system.TutorialSystem;
import com.chenchen.yishu.system.YishuData;
import com.chenchen.yishu.util.YsText;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

/**
 * 易枢 · 网络通道。
 *
 * <p>这个模组只有五个包 —— 依然很克制：
 *
 * <table border="1">
 *   <tr><th>方向</th><th>包</th><th>用途</th></tr>
 *   <tr><td>S2C</td><td>{@link SeedPayload}</td>
 *       <td>世界种子 + 专业模式 + 游戏日，客户端据此本地推算</td></tr>
 *   <tr><td>S2C</td><td>{@link DisplayPayload}</td>
 *       <td>通用显示面板（卦象 / 户型图 / 命盘 / 报告全用它）</td></tr>
 *   <tr><td>S2C</td><td>{@link AmbiencePayload}</td>
 *       <td>玩家所在地的吉凶颜色，驱动屏幕边缘边框</td></tr>
 *   <tr><td>C2S</td><td>{@link CastRequestPayload}</td>
 *       <td>玩家在起卦界面选好了「问什么」</td></tr>
 *   <tr><td>C2S</td><td>{@link SettingsPayload}</td>
 *       <td>切换专业模式</td></tr>
 * </table>
 *
 * <p><b>信息分层在哪一层做</b>：不在网络层。服务端始终把「全量」算出来，
 * 然后按玩家的专业模式决定发<b>哪些行</b> —— 这样同一个数据结构可以
 * 服务两种玩家，而不用做两套协议。
 */
public final class YsNetworking {

    private YsNetworking() {
    }

    // ==================================================================== 数据包

    /**
     * S2C：世界种子 + 玩家设置。
     *
     * <p>客户端的命盘、神煞、大运、黄历全部本地算；专业模式与游戏日也一并带上，
     * 让客户端能独立判断「哪些信息该显示」。
     */
    public record SeedPayload(long seed, int divinationLimit, boolean professional, long epochDay)
            implements CustomPacketPayload {

        public static final CustomPacketPayload.Type<SeedPayload> TYPE =
                new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(YishuMod.MOD_ID, "seed"));

        public static final StreamCodec<RegistryFriendlyByteBuf, SeedPayload> CODEC =
                new StreamCodec<>() {
                    @Override
                    public SeedPayload decode(RegistryFriendlyByteBuf buf) {
                        return new SeedPayload(buf.readLong(), buf.readVarInt(),
                                buf.readBoolean(), buf.readLong());
                    }

                    @Override
                    public void encode(RegistryFriendlyByteBuf buf, SeedPayload payload) {
                        buf.writeLong(payload.seed());
                        buf.writeVarInt(payload.divinationLimit());
                        buf.writeBoolean(payload.professional());
                        buf.writeLong(payload.epochDay());
                    }
                };

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /**
     * S2C：环境边框颜色。
     *
     * <p>这是「第一层 · 直感」的载体：玩家站在什么风水里，屏幕边缘就泛什么颜色。
     * 不用读一个字就知道这地方好不好。0 表示无（平地）。
     */
    public record AmbiencePayload(int argb) implements CustomPacketPayload {

        public static final CustomPacketPayload.Type<AmbiencePayload> TYPE =
                new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(YishuMod.MOD_ID, "ambience"));

        public static final StreamCodec<RegistryFriendlyByteBuf, AmbiencePayload> CODEC =
                new StreamCodec<>() {
                    @Override
                    public AmbiencePayload decode(RegistryFriendlyByteBuf buf) {
                        return new AmbiencePayload(buf.readInt());
                    }

                    @Override
                    public void encode(RegistryFriendlyByteBuf buf, AmbiencePayload payload) {
                        buf.writeInt(payload.argb());
                    }
                };

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /**
     * S2C：通用显示面板。
     *
     * <p>标题 + 若干行文本。行内可以带 § 颜色代码，客户端的玄览面板会识别它来分主次。
     */
    public record DisplayPayload(String title, List<String> lines) implements CustomPacketPayload {

        public static final CustomPacketPayload.Type<DisplayPayload> TYPE =
                new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(YishuMod.MOD_ID, "display"));

        public static final StreamCodec<RegistryFriendlyByteBuf, DisplayPayload> CODEC =
                new StreamCodec<>() {
                    @Override
                    public DisplayPayload decode(RegistryFriendlyByteBuf buf) {
                        String title = buf.readUtf(256);
                        int count = buf.readVarInt();
                        List<String> lines = new ArrayList<>(count);
                        for (int i = 0; i < count; i++) {
                            lines.add(buf.readUtf(1024));
                        }
                        return new DisplayPayload(title, lines);
                    }

                    @Override
                    public void encode(RegistryFriendlyByteBuf buf, DisplayPayload payload) {
                        buf.writeUtf(payload.title(), 256);
                        buf.writeVarInt(payload.lines().size());
                        for (String line : payload.lines()) {
                            buf.writeUtf(line == null ? "" : line, 1024);
                        }
                    }
                };

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /**
     * C2S：起卦请求。
     *
     * <p>{@code omen} 是外应起卦的环境指纹（朝向、脚下方块、天候、坐标）。
     * 它<b>不是随机数</b> —— 同样的位置、同样的朝向、同样的天气，外应就是一样的。
     */
    public record CastRequestPayload(int questionOrdinal, int methodOrdinal, long omen)
            implements CustomPacketPayload {

        public static final CustomPacketPayload.Type<CastRequestPayload> TYPE =
                new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(YishuMod.MOD_ID, "cast"));

        public static final StreamCodec<RegistryFriendlyByteBuf, CastRequestPayload> CODEC =
                new StreamCodec<>() {
                    @Override
                    public CastRequestPayload decode(RegistryFriendlyByteBuf buf) {
                        return new CastRequestPayload(buf.readVarInt(), buf.readVarInt(), buf.readLong());
                    }

                    @Override
                    public void encode(RegistryFriendlyByteBuf buf, CastRequestPayload payload) {
                        buf.writeVarInt(payload.questionOrdinal());
                        buf.writeVarInt(payload.methodOrdinal());
                        buf.writeLong(payload.omen());
                    }
                };

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /**
     * C2S：切换专业模式。
     *
     * <p>不带参数 —— 开/关由服务端决定（因为要检查玩家是否已解锁阶段 6）。
     */
    public record SettingsPayload() implements CustomPacketPayload {

        public static final CustomPacketPayload.Type<SettingsPayload> TYPE =
                new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(YishuMod.MOD_ID, "settings"));

        public static final SettingsPayload INSTANCE = new SettingsPayload();

        public static final StreamCodec<RegistryFriendlyByteBuf, SettingsPayload> CODEC =
                new StreamCodec<>() {
                    @Override
                    public SettingsPayload decode(RegistryFriendlyByteBuf buf) {
                        return INSTANCE;
                    }

                    @Override
                    public void encode(RegistryFriendlyByteBuf buf, SettingsPayload payload) {
                    }
                };

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    // ==================================================================== 注册

    /** 通用端初始化（专用服务器也会跑）。 */
    public static void registerCommon() {
        PayloadTypeRegistry.serverboundPlay().register(CastRequestPayload.TYPE, CastRequestPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(SettingsPayload.TYPE, SettingsPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(SeedPayload.TYPE, SeedPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(DisplayPayload.TYPE, DisplayPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(AmbiencePayload.TYPE, AmbiencePayload.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(CastRequestPayload.TYPE, (payload, context) ->
                // 26.2 起 ServerPlayer#server 字段是私有的，用 level().getServer() 拿服务器实例
                context.player().level().getServer().execute(() ->
                        handleCast(context.player(), payload)));

        ServerPlayNetworking.registerGlobalReceiver(SettingsPayload.TYPE, (payload, context) ->
                context.player().level().getServer().execute(() ->
                        TutorialSystem.toggleProfessional(context.player())));
    }

    // ==================================================================== 发送

    public static void sendSeed(ServerPlayer player, ServerLevel level) {
        if (!ServerPlayNetworking.canSend(player, SeedPayload.TYPE)) {
            return;
        }
        long epochDay = level.getOverworldClockTime() / 24000L;
        ServerPlayNetworking.send(player, new SeedPayload(level.getSeed(),
                YishuConfig.get().divinationDailyLimit,
                TutorialSystem.professional(player), epochDay));
    }

    /** 兼容旧调用（不带世界实例时用配置默认值）。 */
    public static void sendSeed(ServerPlayer player, long seed) {
        if (ServerPlayNetworking.canSend(player, SeedPayload.TYPE)) {
            ServerPlayNetworking.send(player, new SeedPayload(seed,
                    YishuConfig.get().divinationDailyLimit,
                    YishuConfig.get().professionalModeDefault, 0L));
        }
    }

    /** 显式指定专业模式与游戏日的重载（引导链切换设置时用）。 */
    public static void sendSeed(ServerPlayer player, long seed, boolean professional, long epochDay) {
        if (ServerPlayNetworking.canSend(player, SeedPayload.TYPE)) {
            ServerPlayNetworking.send(player, new SeedPayload(seed,
                    YishuConfig.get().divinationDailyLimit, professional, epochDay));
        }
    }

    public static void sendAmbience(ServerPlayer player, int argb) {
        if (ServerPlayNetworking.canSend(player, AmbiencePayload.TYPE)) {
            ServerPlayNetworking.send(player, new AmbiencePayload(argb));
        }
    }

    public static void sendDisplay(net.minecraft.world.entity.player.Player player, DisplayPayload payload) {
        if (player instanceof ServerPlayer serverPlayer
                && ServerPlayNetworking.canSend(serverPlayer, DisplayPayload.TYPE)) {
            ServerPlayNetworking.send(serverPlayer, payload);
        }
    }

    // ==================================================================== 处理起卦

    /**
     * 处理一次问卦请求（服务端权威路径）。
     *
     * <p>网络包与 /yishu divine 指令都走这里，保证「同种子同命盘同问题
     * 同游戏日同时辰，结果完全一致」——指令只是不消耗铜钱、以时间起卦。
     */
    public static void handleCast(ServerPlayer player, CastRequestPayload payload) {
        if (!(player.level() instanceof ServerLevel server)) {
            return;
        }
        DimensionProfile dimension = DimensionProfile.of(server.dimension().identifier().getPath());
        if (!YishuConfig.get().forDimension(dimension.shortKey()).liuYao) {
            YsText.chat(player, "本维度的六爻系统已被配置关闭。");
            return;
        }

        // ---- 渐进式解锁：铜钱要到第 4 天才给 ----
        if (!TutorialSystem.checkGate(player, player.getMainHandItem().getItem())
                && !TutorialSystem.checkGate(player, player.getOffhandItem().getItem())) {
            return;
        }

        Divination.Question question = Divination.Question.VALUES[
                Math.floorMod(payload.questionOrdinal(), Divination.Question.VALUES.length)];
        Divination.Method method = Divination.Method.values()[
                Math.floorMod(payload.methodOrdinal(), Divination.Method.values().length)];

        long epochDay = server.getOverworldClockTime() / 24000L;
        int hourIndex = dimension.hourIndex(server.getOverworldClockTime());

        // ---- 每日次数与反噬 ----
        YishuData data = YishuData.of(server);
        String owner = player.getGameProfile().name();
        int asked = data.divinationsToday(owner, epochDay) + 1;
        data.recordDivination(owner, epochDay);

        Divination.Backlash backlash = YishuConfig.get().backlashEnabled
                ? Divination.Backlash.forCount(asked, Math.max(0, YishuConfig.get().divinationDailyLimit))
                : Divination.Backlash.NONE;

        // ---- 起卦 ----
        Divination.Input input = new Divination.Input(
                server.getSeed(), dimension.shortKey(), "player:" + owner,
                question, method, epochDay, hourIndex, payload.omen());

        // 跨维度占卜时结果更模糊
        double fuzz = dimension == DimensionProfile.OVERWORLD ? 0.0D : 0.18D;
        Divination.Reading reading = Divination.cast(input, fuzz);

        boolean professional = TutorialSystem.professional(player);
        boolean novice = TutorialSystem.inGracePeriod(epochDay);

        // ---- 组装输出（分层）----
        List<String> lines = new ArrayList<>();

        // 第二层 · 白话：永远在最上面，这是玩家真正要读的东西
        lines.add("§b" + PlainSpeak.MOTTO);
        lines.add("");
        lines.addAll(PlainSpeak.readingPlain(reading));

        // 反噬与新手的安抚
        if (backlash != Divination.Backlash.NONE) {
            lines.add("§c反噬：" + backlash.zh() + " —— " + backlash.note());
            BacklashSystem.apply(server, player, backlash);
        }
        if (reading.vague()) {
            lines.add("§e你今天问得有点多，卦气有点模糊 —— 明天再来问会更清楚。");
        }
        if (novice) {
            lines.add("§7新手期：前几天就算风水不好，惩罚也会轻很多，放心探索。");
        }

        // ---- 保底：连续三次凶卦，必出一次吉兆 ----
        lines.addAll(pity(server, data, owner, reading));

        // ---- 第三层 · 术语：只给开专业模式的人 ----
        if (professional) {
            lines.add("");
            lines.add("§7──── 专业模式 ────");
            lines.addAll(YsItemContent.renderReading(reading));
        } else {
            lines.add("");
            lines.add("§8（想看卦象术语？到第 29 天就能在命盘里开专业模式）");
        }

        YsText.send(player, "起卦 · " + question.zh(), lines);
        data.setDirty();
    }

    /**
     * 保底机制 —— 容错设计的一部分。
     *
     * <p><b>卦本身不动</b>：同一卦在同一天问出来永远一样，这是模组的命根子。
     * 保底做的是<b>额外的补偿</b>：连着几卦都不顺，就送一次实实在在的吉运，
     * 并把计数清零。这样既给了"天无绝人之路"的体验，又不破坏确定性。
     */
    private static List<String> pity(ServerLevel server, YishuData data, String owner,
                                     Divination.Reading reading) {
        List<String> lines = new ArrayList<>();
        String key = "badstreak:" + owner;
        int streak;
        if (reading.score() < 0) {
            streak = parseInt(data.get(key)) + 1;
            data.put(key, String.valueOf(streak));
        } else {
            streak = 0;
            data.put(key, "0");
        }

        if (streak >= 3) {
            data.put(key, "0");
            playerLuck(server, owner);
            lines.add("§e连着几卦都不顺，天无绝人之路 —— 送你一阵好运，接着来。");
        }
        return lines;
    }

    private static void playerLuck(ServerLevel server, String owner) {
        for (ServerPlayer player : server.players()) {
            if (player.getGameProfile().name().equals(owner)) {
                player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        net.minecraft.world.effect.MobEffects.LUCK, 20 * 300, 0, true, true, true));
                server.sendParticles(net.minecraft.core.particles.ParticleTypes.HAPPY_VILLAGER,
                        player.getX(), player.getY() + 1.0D, player.getZ(),
                        30, 0.6D, 1.0D, 0.6D, 0.1D);
            }
        }
    }

    private static int parseInt(String value) {
        if (value == null) {
            return 0;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            return 0;
        }
    }

    // ==================================================================== 环境边框

    /**
     * 把玩家所在地的吉凶颜色推给客户端。
     *
     * <p>这是「进入建筑时屏幕边缘显示颜色边框」那条规范的实现：
     * 金边=大吉，绿边=吉，灰边=平，橙边=凶，红边=大凶。
     * 玩家<b>不需要读一个字</b>就知道这地方好不好。
     */
    public static void sendAmbience(ServerPlayer player, ServerLevel level) {
        if (!YishuConfig.get().ambienceBorder) {
            return;
        }
        DimensionProfile dimension = DimensionProfile.of(level.dimension().identifier().getPath());
        if (!com.chenchen.yishu.fengshui.FengShuiSystems.enabled(dimension)) {
            sendAmbience(player, 0);
            return;
        }
        FengShuiField.Sample sample = FengShuiField.sample(
                level.getSeed(), dimension,
                player.blockPosition().getX(), player.blockPosition().getZ());
        FengShuiField.Grade grade = sample.grade(dimension);
        int color = grade == FengShuiField.Grade.PLAIN ? 0 : PlainSpeak.gradeColor(grade);
        sendAmbience(player, color);
    }

    // ==================================================================== 加入时

    /** 玩家加入时推送种子、设置与当日黄历。 */
    public static void onJoin(ServerPlayer player, ServerLevel server) {
        sendSeed(player, server);
        DimensionProfile dimension = DimensionProfile.of(server.dimension().identifier().getPath());
        long epochDay = server.getOverworldClockTime() / 24000L;

        // 老玩家才报黄历；新玩家由引导链接管第一条消息，避免一进服就刷屏
        if (!YishuConfig.get().tutorialEnabled || TutorialSystem.stageForDay(epochDay) >= 2) {
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                    "§7今日：" + Yun.liuRi(server.getSeed(), dimension.shortKey(), epochDay).zh() + " 日 · "
                            + com.chenchen.yishu.fengshui.FengShuiSystems.Almanac.summary(
                            com.chenchen.yishu.fengshui.FengShuiSystems.Almanac.ofDay(
                                    server.getSeed(), dimension, epochDay))));
        }
    }

    /** 供调试：报告建筑命盘。 */
    public static void reportBuilding(ServerPlayer player, ServerLevel server,
                                      BuildingFate.BuildingType type, int x, int z) {
        DimensionProfile dimension = DimensionProfile.of(server.dimension().identifier().getPath());
        BuildingFate.Fate fate = BuildingFate.fateOf(server.getSeed(), dimension, type, x, z);
        YsText.send(player, fate.type().zh() + " · 命盘",
                BuildingFate.report(server.getSeed(), fate, server.getOverworldClockTime() / 24000L));
    }

    /** 供调试：报告遗迹。 */
    public static void reportRuins(ServerPlayer player, ServerLevel server) {
        DimensionProfile dimension = DimensionProfile.of(server.dimension().identifier().getPath());
        long epochDay = server.getOverworldClockTime() / 24000L;
        List<String> lines = new ArrayList<>();
        for (Ruins.Site site : Ruins.sites(server.getSeed(), dimension)) {
            lines.add("§b" + site.type().zh() + "§f @" + site.x() + ", " + site.z());
            lines.add("    " + site.type().mechanism());
            lines.add("    今日：" + (Ruins.isOpenToday(server.getSeed(), dimension, site.type(), epochDay)
                    ? "可开启" : "§c不受机关")
                    + " · 需以「" + site.type().catalyst().zh() + "」相引");
        }
        YsText.panel(player, "本维度遗迹一览（" + dimension.zh() + "）", lines);
    }

    /** 命盘文本导出。 */
    public static List<String> fateText(ServerPlayer player, ServerLevel server) {
        FateProfile profile = FateProfile.of(server.getSeed(), player.getGameProfile().name());
        return YsItemContent.fateText(profile);
    }
}
