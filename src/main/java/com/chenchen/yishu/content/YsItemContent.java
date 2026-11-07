package com.chenchen.yishu.content;

import com.chenchen.yishu.YishuMod;
import com.chenchen.yishu.building.BuildingFate;
import com.chenchen.yishu.building.Ruins;
import com.chenchen.yishu.client.YishuClientBridge;
import com.chenchen.yishu.core.StableKey;
import com.chenchen.yishu.core.YishuRandom;
import com.chenchen.yishu.core.YsReg;
import com.chenchen.yishu.dimension.DimensionProfile;
import com.chenchen.yishu.fate.Bazi;
import com.chenchen.yishu.fate.FateCalendar;
import com.chenchen.yishu.fate.FateProfile;
import com.chenchen.yishu.fate.GanZhi.WuXing;
import com.chenchen.yishu.fate.Yun;
import com.chenchen.yishu.fengshui.FengShuiField;
import com.chenchen.yishu.fengshui.TerrainQi;
import com.chenchen.yishu.fengshui.FengShuiSystems;
import com.chenchen.yishu.fengshui.HouseScanner;
import com.chenchen.yishu.liuyao.Divination;
import com.chenchen.yishu.liuyao.Gua;
import com.chenchen.yishu.spirit.MobFate;
import com.chenchen.yishu.system.PlainSpeak;
import com.chenchen.yishu.system.TutorialSystem;
import com.chenchen.yishu.util.YsText;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * 易枢 · 物品内容 —— 玩家手上的那一套「法器」。
 *
 * <p>全部物品都放在这一个文件里，是有意的：它们的行为都很短，
 * 拆成十几个文件之后，反而看不出它们之间的<b>家族关系</b>。
 * 按家族分组写在下面，每一族前面都有一段说明。
 *
 * <p>所有物品都遵守同一条纪律：<b>不在客户端做权威判断</b>。
 * 客户端只负责「打开界面」，真正的占算、扫描、激活一律在服务端跑，
 * 结果再发回来显示。这样多人游戏里每个人看到的都是同一份结果。
 */
public final class YsItemContent {

    private YsItemContent() {
    }

    // ==================================================================== 家族一：命盘

    /**
     * 命盘玉简。
     *
     * <p>模组的第一件物品，也是玩法的起点。右键（或按 B）就能看到自己的八字。
     * 它是<b>纯客户端</b>的：命盘由种子与自己的名字推出来，不需要问服务端要任何东西。
     * 这也是「无指令交互」这一条设计原则最直接的体现 —— 想看命，用东西，不用打字。
     */
    public static class FateSlip extends Item {

        public FateSlip(Properties properties) {
            super(properties);
        }

        @Override
        public InteractionResult use(Level level, Player player, InteractionHand hand) {
            if (level.isClientSide()) {
                // 只在客户端开界面；服务端什么都不做（没有权威状态需要改）
                YishuClientBridge.openBazi(player);
                return InteractionResult.SUCCESS;
            }
            if (!(player instanceof ServerPlayer serverPlayer)) {
                return InteractionResult.SUCCESS_SERVER;
            }
            if (!TutorialSystem.checkGate(serverPlayer, this)) {
                return InteractionResult.SUCCESS_SERVER;
            }
            // 内置帮助：Shift+右键 = "这是什么"
            if (player.isShiftKeyDown()) {
                YsText.send(serverPlayer, "这是什么",
                        PlainSpeak.fateHelp(FateProfile.of(serverPlayer.level().getSeed(),
                                player.getGameProfile().name()).active()));
            }
            return InteractionResult.SUCCESS_SERVER;
        }
    }

    /**
     * 窥命符。
     *
     * <p>看别人的命。对着生物右键可以看到它的命盘摘要；对着玩家右键（潜行时）
     * 可以看到对方的命盘 —— 但对方如果处于「伪命」状态，你看到的就是假的。
     *
     * <p>窥命是要付代价的：每天看太多会开始反噬。见 {@link MobFate#peekingBacklash}。
     */
    public static class PeekCharm extends Item {

        public PeekCharm(Properties properties) {
            super(properties);
        }

        @Override
        public InteractionResult interactLivingEntity(ItemStack stack, Player player,
                                                      LivingEntity target, InteractionHand hand) {
            if (player.level().isClientSide()) {
                return InteractionResult.SUCCESS;
            }
            if (!(player.level() instanceof ServerLevel server)) {
                return InteractionResult.PASS;
            }
            long seed = server.getSeed();

            if (target instanceof Player other) {
                FateProfile profile = FateProfile.of(seed, other.getGameProfile().name());
                int seen = com.chenchen.yishu.system.YishuData.of(server).peeksToday(player.getGameProfile().name());
                Divination.Backlash backlash = MobFate.peekingBacklash(seen);
                if (backlash != Divination.Backlash.NONE) {
                    com.chenchen.yishu.system.BacklashSystem.apply(server, player, backlash);
                    return InteractionResult.SUCCESS_SERVER;
                }
                YsText.send(player, other.getGameProfile().name() + " 的命盘", List.of(
                        profile.presented().summary(),
                        "本命卦：" + MobFate.hexagramName(StableKey.of(seed, "playerhex")
                                .add(other.getGameProfile().name())),
                        profile.isPseudo() ? "（此命有伪，真假难辨）" : "（此命可信）"
                ));
                return InteractionResult.SUCCESS_SERVER;
            }

            if (target instanceof Mob mob && MobFate.enabled(mob)) {
                int seen = com.chenchen.yishu.system.YishuData.of(server).peeksToday(player.getGameProfile().name());
                Divination.Backlash backlash = MobFate.peekingBacklash(seen);
                if (backlash != Divination.Backlash.NONE) {
                    com.chenchen.yishu.system.BacklashSystem.apply(server, player, backlash);
                    return InteractionResult.SUCCESS_SERVER;
                }
                com.chenchen.yishu.system.YishuData.of(server).incrementPeeks(player.getGameProfile().name());
                MobFate.Profile profile = MobFate.profileOf(seed, mob);
                YsText.send(player, target.getName().getString() + " 的命盘", List.of(
                        profile.summary(),
                        "算法 DNA：" + profile.dna().describe(),
                        "倾向：攻击 " + round(profile.aggression())
                                + " / 谨慎 " + round(profile.caution())
                                + " / 群聚 " + round(profile.cohesion())
                ));
                return InteractionResult.SUCCESS_SERVER;
            }
            return InteractionResult.PASS;
        }

        @Override
        public InteractionResult use(Level level, Player player, InteractionHand hand) {
            if (level.isClientSide()) {
                YishuClientBridge.openBazi(player);
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.SUCCESS_SERVER;
        }
    }

    // ==================================================================== 家族二：风水

    /**
     * 风水罗盘。
     *
     * <p>两种用法：
     * <ul>
     *   <li><b>对着门右键</b> —— 触发一次完整的户型扫描；</li>
     *   <li><b>对着空气右键</b> —— 报出脚下这块地属于哪个风水域、气煞如何。</li>
     * </ul>
     */
    public static class FengShuiCompass extends Item {

        public FengShuiCompass(Properties properties) {
            super(properties);
        }

        @Override
        public InteractionResult useOn(UseOnContext context) {
            Player player = context.getPlayer();
            Level level = context.getLevel();
            if (player == null || level.isClientSide()) {
                return InteractionResult.SUCCESS;
            }
            if (!(level instanceof ServerLevel server)) {
                return InteractionResult.PASS;
            }
            if (!TutorialSystem.checkGate(player, this)) {
                return InteractionResult.SUCCESS_SERVER;
            }
            BlockPos pos = context.getClickedPos();
            DimensionProfile dimension = DimensionProfile.of(level.dimension().identifier().toString());
            if (!FengShuiSystems.enabled(dimension)) {
                YsText.send(player, "风水未启", List.of("本维度的风水系统已被配置关闭。"));
                return InteractionResult.SUCCESS_SERVER;
            }
            BlockState state = level.getBlockState(pos);
            if (!(state.getBlock() instanceof DoorBlock)) {
                return reportField(server, player, pos, dimension);
            }

            // 内置帮助：潜行 + 对门右键 = 图例
            if (player.isShiftKeyDown()) {
                YsText.send(player, "罗盘图例", PlainSpeak.compassHelp());
                return InteractionResult.SUCCESS_SERVER;
            }

            var report = HouseScanner.scan(level, pos, server.getSeed(), dimension);
            if (report == null) {
                YsText.send(player, "扫描失败", List.of(
                        "这座门之后不是一处封闭的居所。",
                        "请站在屋内、对准一扇门再试，或确认屋子有完整的墙与屋顶。"));
                return InteractionResult.SUCCESS_SERVER;
            }
            // 分层：白话结论在前，图纸与批断在后
            List<String> lines = new ArrayList<>();
            lines.add(report.grade().zh() + "（" + report.score() + "/100）。"
                    + PlainSpeak.gradeAdvice(report.grade()) + "。");
            int serious = report.countIssues(2);
            if (serious == 0) {
                lines.add("没发现大问题，住着舒心。");
            } else {
                lines.add("发现 " + serious + " 处要改的地方，往下看有具体改法。");
            }
            lines.add("");
            lines.addAll(renderReport(report));
            YsText.send(player, "户型扫描 · " + report.grade().zh(), lines);
            level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.7F, 1.4F);
            return InteractionResult.SUCCESS_SERVER;
        }

        @Override
        public InteractionResult use(Level level, Player player, InteractionHand hand) {
            if (level.isClientSide()) {
                return InteractionResult.SUCCESS;
            }
            if (!(level instanceof ServerLevel server)) {
                return InteractionResult.PASS;
            }
            DimensionProfile dimension = DimensionProfile.of(level.dimension().identifier().toString());
            return reportField(server, player, player.blockPosition(), dimension);
        }

        private InteractionResult reportField(ServerLevel server, Player player, BlockPos pos,
                                              DimensionProfile dimension) {
            FengShuiField.Sample sample = TerrainQi.sample(server, dimension, pos.getX(), pos.getZ());
            FengShuiField.Grade grade = sample.grade(dimension);
            boolean professional = player instanceof ServerPlayer sp && TutorialSystem.professional(sp);

            List<String> lines = new ArrayList<>();

            // ---- 第二层 · 白话（玩家真正要读的）----
            lines.add(PlainSpeak.gradePlain(grade) + "。" + PlainSpeak.gradeAdvice(grade) + "。");
            lines.add(PlainSpeak.qiPlain(sample) + "。");
            String better = PlainSpeak.betterDirection(server.getSeed(), dimension,
                    pos.getX(), pos.getZ(), 48);
            if (better != null && grade != FengShuiField.Grade.GREAT_LUCK) {
                lines.add("提示：" + better + "。");
            }

            // ---- 第三层 · 术语（只给专业模式）----
            if (professional) {
                lines.add("");
                lines.add("§7──── 专业模式 ────");
                lines.add("地块：" + sample.domainLabel());
                lines.add("气 " + sample.qi() + " ｜ 煞 " + sample.sha()
                        + " ｜ 阴 " + sample.yin() + " ｜ 阳 " + sample.yang()
                        + " ｜ 阴阳平衡度 " + sample.balance());
                lines.add("五行偏向：" + sample.element().zh()
                        + "（" + Math.round(sample.elementStrength() * 100) + "%）");
                lines.add("综合：" + grade.zh() + "（" + sample.score(dimension) + "/100）");
                lines.add("效用：" + grade.effect());
            }

            long year = Yun.age(server.getOverworldClockTime() / 24000L);
            if (FengShuiSystems.DragonVein.isOnVein(server.getSeed(), dimension, year,
                    pos.getX(), pos.getZ())) {
                double aura = FengShuiSystems.DragonVein.auraStrength(server.getSeed(), dimension, year,
                        pos.getX(), pos.getZ());
                lines.add("★ 此地正在龙脉之上（风水宝地），龙气浓度 " + Math.round(aura * 100) + "%");
            }
            YsText.send(player, "观气 · " + grade.zh(), lines);

            // ---- 第一层 · 直感：粒子 + 音效 ----
            switch (grade) {
                case GREAT_LUCK, LUCK -> {
                    server.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD,
                            pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D,
                            20, 0.6D, 0.6D, 0.6D, 0.05D);
                    server.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME,
                            SoundSource.BLOCKS, 0.8F, 1.6F);
                }
                case BAD, DIRE -> {
                    server.sendParticles(net.minecraft.core.particles.ParticleTypes.SMOKE,
                            pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D,
                            20, 0.6D, 0.6D, 0.6D, 0.02D);
                    server.playSound(null, pos, SoundEvents.WARDEN_HEARTBEAT,
                            SoundSource.BLOCKS, 0.6F, 0.7F);
                }
                default -> {
                }
            }
            return InteractionResult.SUCCESS_SERVER;
        }
    }

    /**
     * 寻龙尺。
     *
     * <p>指向今年龙脉的方向与距离。它<b>不给坐标</b> —— 只给方向和距离，
     * 玩家还得自己走过去。这跟六爻解卦的克制是同一个道理。
     */
    public static class DragonRuler extends Item {

        public DragonRuler(Properties properties) {
            super(properties);
        }

        @Override
        public InteractionResult use(Level level, Player player, InteractionHand hand) {
            if (level.isClientSide()) {
                return InteractionResult.SUCCESS;
            }
            if (!(level instanceof ServerLevel server)) {
                return InteractionResult.PASS;
            }
            DimensionProfile dimension = DimensionProfile.of(level.dimension().identifier().toString());
            if (!com.chenchen.yishu.YishuConfig.get().dragonVeinEnabled) {
                YsText.send(player, "寻龙尺", List.of("龙脉系统已被配置关闭。"));
                return InteractionResult.SUCCESS_SERVER;
            }
            long epochDay = server.getOverworldClockTime() / 24000L;
            long year = Yun.age(epochDay);
            BlockPos pos = player.blockPosition();

            var node = FengShuiSystems.DragonVein.nearestNode(server.getSeed(), dimension, year,
                    pos.getX(), pos.getZ());
            double distance = FengShuiSystems.DragonVein.distance(server.getSeed(), dimension, year,
                    pos.getX(), pos.getZ());

            List<String> lines = new ArrayList<>();
            lines.add("今年龙脉（第 " + year + " 年）");
            if (distance <= FengShuiSystems.DragonVein.WIDTH) {
                lines.add("你正站在龙脉之上，龙气充盈。");
            } else {
                lines.add("最近的龙脉段位在你 " + compass(pos.getX(), pos.getZ(), node.x(), node.z())
                        + "，约 " + Math.round(distance) + " 格之外。");
                lines.add("龙脉每年游走一次，过了年就不是这里了。");
            }
            lines.add("提示：龙脉上一切采集皆有加成，且可立契占领。");
            YsText.send(player, "寻龙尺", lines);
            level.playSound(null, pos, SoundEvents.LODESTONE_COMPASS_LOCK, SoundSource.PLAYERS, 0.6F, 1.7F);
            return InteractionResult.SUCCESS_SERVER;
        }
    }

    // ==================================================================== 家族三：六爻

    /**
     * 起卦器具。
     *
     * <p>三枚铜钱、蓍草、龟甲、日晷 —— 它们的行为完全一样：右键打开起卦界面。
     * 之所以做成四个物品，是因为<b>仪式感本身就是玩法的一部分</b>：
     * 用龟甲起卦和用铜钱起卦，玩家心里的分量是不一样的。
     *
     * <p>起卦界面在客户端弹出，选好问事类型后发一个
     * {@code CastRequestPayload} 给服务端，由服务端算完再发回结果。
     */
    public static class DivinationTool extends Item {

        private final boolean allowsOmen;

        public DivinationTool(Properties properties, boolean allowsOmen) {
            super(properties);
            this.allowsOmen = allowsOmen;
        }

        /** 这件器具是否支持「外应起卦」。 */
        public boolean allowsOmen() {
            return this.allowsOmen;
        }

        @Override
        public InteractionResult use(Level level, Player player, InteractionHand hand) {
            if (level.isClientSide()) {
                YishuClientBridge.openDivination(player, this.allowsOmen);
                return InteractionResult.SUCCESS;
            }
            if (!(player instanceof ServerPlayer serverPlayer)) {
                return InteractionResult.SUCCESS_SERVER;
            }
            if (!TutorialSystem.checkGate(serverPlayer, this)) {
                return InteractionResult.SUCCESS_SERVER;
            }
            if (player.isShiftKeyDown()) {
                YsText.send(serverPlayer, "这是什么", PlainSpeak.divinationHelp());
            }
            return InteractionResult.SUCCESS_SERVER;
        }
    }

    // ==================================================================== 家族四：符箓

    /**
     * 符箓。
     *
     * <p>一次性的小型术法。右键使用，按当日的「宜忌」决定效果强弱 ——
     * 也就是说，符箓在宜祈福的日子里更灵，在忌祭祀的日子里会哑火。
     */
    public static class Talisman extends Item {

        private final String purpose;

        public Talisman(Properties properties, String purpose) {
            super(properties);
            this.purpose = purpose;
        }

        @Override
        public InteractionResult use(Level level, Player player, InteractionHand hand) {
            if (level.isClientSide()) {
                return InteractionResult.SUCCESS;
            }
            if (!(level instanceof ServerLevel server)) {
                return InteractionResult.PASS;
            }
            DimensionProfile dimension = DimensionProfile.of(level.dimension().identifier().toString());
            long epochDay = server.getOverworldClockTime() / 24000L;
            var almanac = FengShuiSystems.Almanac.ofDay(server.getSeed(), dimension, epochDay);

            boolean favorable = FengShuiSystems.Almanac.isFavorable(almanac, "ritual");
            boolean taboo = FengShuiSystems.Almanac.isTaboo(almanac, "ritual");

            List<String> lines = new ArrayList<>();
            lines.add("今值 " + Yun.liuRi(server.getSeed(), dimension.shortKey(), epochDay)
                    + " 日 · " + FengShuiSystems.Almanac.summary(almanac));

            if (taboo && !favorable) {
                lines.add("今日忌祭祀，符箓未能生效。");
                YsText.send(player, "符箓无功", lines);
                return InteractionResult.SUCCESS_SERVER;
            }

            int potency = favorable ? 2 : 1;
            switch (this.purpose) {
                case "ward" -> {
                    player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                            net.minecraft.world.effect.MobEffects.RESISTANCE, 20 * 60 * potency, 0));
                    lines.add("护身符生效：短时获得抗性。");
                }
                case "fortune" -> {
                    player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                            net.minecraft.world.effect.MobEffects.LUCK, 20 * 120 * potency, 0));
                    lines.add("转运符生效：短时获得幸运。");
                }
                case "clarity" -> {
                    player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                            net.minecraft.world.effect.MobEffects.NIGHT_VISION, 20 * 90 * potency, 0));
                    lines.add("明目符生效：短时获得夜视，可看清卦象。");
                }
                case "taisui" -> {
                    lines.add("太岁符已焚。今岁太岁方位：" + FengShuiSystems.TaiSui.directionName(
                            Yun.age(epochDay)));
                    lines.add("在该方位上动土会招煞，除非附近有石敢当镇之。");
                }
                default -> lines.add("符箓无名，毫无反应。");
            }
            stackShrink(player, hand);
            level.playSound(null, player.blockPosition(), SoundEvents.FIRE_AMBIENT,
                    SoundSource.PLAYERS, 0.8F, 1.5F);
            YsText.send(player, "符箓", lines);
            return InteractionResult.SUCCESS_SERVER;
        }

        public String purpose() {
            return this.purpose;
        }
    }

    // ==================================================================== 家族四·五：阵盘

    /**
     * 阵盘（第 20 轮）。
     *
     * <p>八卦、北斗、招财三盘。右键地面布阵：阵眼记进存档，当日有效，
     * 阵内之人得阵法之利、邪祟受阵法所制 —— 效果由
     * {@link com.chenchen.yishu.system.FormationSystem} 的周期循环落地。
     *
     * <p>刻意不做方块：阵是「势」，不是「物」。一块摆在地上的盘子
     * 很容易被当成装饰家具忘掉，而「今天入夜前阵就散了」才有仪式感。
     */
    public static class FormationDisc extends Item {

        private final String kind;

        public FormationDisc(Properties properties, String kind) {
            super(properties);
            this.kind = kind;
        }

        public String kind() {
            return this.kind;
        }

        @Override
        public InteractionResult useOn(UseOnContext context) {
            Level level = context.getLevel();
            Player player = context.getPlayer();
            if (player == null || level.isClientSide()) {
                return InteractionResult.SUCCESS;
            }
            if (!(level instanceof ServerLevel server) || !(player instanceof ServerPlayer serverPlayer)) {
                return InteractionResult.SUCCESS_SERVER;
            }
            BlockPos pos = context.getClickedPos().relative(context.getClickedFace());
            String message = com.chenchen.yishu.system.FormationSystem.place(
                    server, serverPlayer, pos, this.kind);
            if (message != null) {
                if (!player.getAbilities().instabuild) {
                    context.getItemInHand().shrink(1);
                }
                level.playSound(null, pos, SoundEvents.BELL_BLOCK,
                        SoundSource.PLAYERS, 0.9F, 0.6F);
                YsText.send(serverPlayer, "布阵", List.of(message));
            }
            return InteractionResult.SUCCESS_SERVER;
        }
    }

    // ==================================================================== 家族五：灵石

    /**
     * 五行灵石。
     *
     * <p>材料，也是钥匙。五行灵石是激活遗迹祭坛的通货 ——
     * 哪一座坛要哪一行的灵石，规范里都写死了。
     */
    public static class ElementStone extends Item {

        private final WuXing element;

        public ElementStone(Properties properties, WuXing element) {
            super(properties);
            this.element = element;
        }

        public WuXing element() {
            return this.element;
        }

        @Override
        public InteractionResult useOn(UseOnContext context) {
            Player player = context.getPlayer();
            Level level = context.getLevel();
            if (player == null || level.isClientSide()) {
                return InteractionResult.SUCCESS;
            }
            if (!(level instanceof ServerLevel server)) {
                return InteractionResult.PASS;
            }
            BlockState state = level.getBlockState(context.getClickedPos());
            if (!(state.getBlock() instanceof YsBlockContent.RuinCore)) {
                return InteractionResult.PASS;
            }
            return YsBlockContent.RuinCore.tryActivate(server, player, context.getClickedPos(),
                    this.element, context.getItemInHand());
        }
    }

    /**
     * 阴阳石。
     *
     * <p>激活「阴阳界碑」需要同时献上阴属性与阳属性之物。
     * 阴阳石是这两属性的载体：同一块石头，装在物品栏里看是「阴」还是「阳」，
     * 由它被合成的批次决定（一个确定性的小把戏，见 {@link #isYang}）。
     */
    public static class YinYangStone extends Item {

        public YinYangStone(Properties properties) {
            super(properties);
        }

        /** 这块石头是阴还是阳 —— 由物品自身的属性哈希决定，同一块石头永远一样。 */
        public boolean isYang(ItemStack stack) {
            return StableKey.of("yinyang").add(net.minecraft.core.registries.BuiltInRegistries.ITEM
                    .getKey(stack.getItem()).getPath()).add(stack.getCount()).chance(0.5D);
        }

        @Override
        public InteractionResult useOn(UseOnContext context) {
            Player player = context.getPlayer();
            if (player == null || context.getLevel().isClientSide()) {
                return InteractionResult.SUCCESS;
            }
            if (!(context.getLevel() instanceof ServerLevel server)) {
                return InteractionResult.PASS;
            }
            BlockState state = server.getBlockState(context.getClickedPos());
            if (state.getBlock() instanceof YsBlockContent.YinyangStele) {
                return YsBlockContent.YinyangStele.offer(server, player, context.getClickedPos(),
                        isYang(context.getItemInHand()), context.getItemInHand());
            }
            return InteractionResult.PASS;
        }
    }

    /** 无极材料类物品（无行为，只有 tooltip 价值）。 */
    public static class Material extends Item {

        private final String noteKey;

        public Material(Properties properties, String noteKey) {
            super(properties);
            this.noteKey = noteKey;
        }

        public String noteKey() {
            return this.noteKey;
        }
    }

    // ==================================================================== 家族十：丹道

    /**
     * 丹药 —— 服下即得状态效果。
     *
     * <p>丹道是命理体系里最世俗的一支：把术数里的「气」压进一颗药里，
     * 吃下去就能立刻感到不同。每一炉都是「一味主药 + 一味引子（蓍草）」，
     * 主药决定药性，引子负责把药性引到正路上。
     */
    public static class Pill extends Item {

        private final List<Dose> doses;

        public Pill(Properties properties, Dose... doses) {
            super(properties);
            this.doses = List.of(doses);
        }

        @Override
        public InteractionResult use(Level level, Player player, InteractionHand hand) {
            if (level.isClientSide()) {
                return InteractionResult.SUCCESS;
            }
            for (Dose dose : this.doses) {
                player.addEffect(new MobEffectInstance(dose.effect(), dose.duration(), dose.amplifier()));
            }
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.GENERIC_DRINK.value(), SoundSource.PLAYERS, 0.9F, 1.15F);
            stackShrink(player, hand);
            return InteractionResult.SUCCESS_SERVER;
        }
    }

    /** 一味药：效果 + 时长（刻）+ 等级。 */
    public record Dose(Holder<MobEffect> effect, int duration, int amplifier) {
    }

    // ==================================================================== 家族十一：灵物

    /**
     * 寻脉图 —— 展开一次，替你算出最近的一处遗迹在哪个方向。
     *
     * <p>它读的是世界的「脉」：把全地图的遗迹当成一条条隐线，
     * 图上那团墨按你站的位置自己挪动。不消耗，可以反复摊开来看。
     */
    public static class VeinMap extends Item {

        public VeinMap(Properties properties) {
            super(properties);
        }

        @Override
        public InteractionResult use(Level level, Player player, InteractionHand hand) {
            if (level.isClientSide() || !(player instanceof ServerPlayer server)) {
                return InteractionResult.SUCCESS;
            }
            TagKey<Structure> tag = TagKey.create(Registries.STRUCTURE, YsReg.id("ruins"));
            BlockPos pos = server.level().findNearestMapStructure(tag, server.blockPosition(), 100, false);
            if (pos == null) {
                YsText.chat(player, "图上空空如也 —— 这一带没有遗迹的脉气。");
                return InteractionResult.SUCCESS;
            }
            int dx = pos.getX() - player.getBlockX();
            int dz = pos.getZ() - player.getBlockZ();
            int dist = (int) Math.sqrt((double) dx * dx + (double) dz * dz);
            List<String> lines = new ArrayList<>();
            lines.add("§b最近的遗迹§f在 §e" + compass(0, 0, dx, dz)
                    + "§f，约 §e" + dist + " 格§f之外。");
            lines.add("坐标：X " + pos.getX() + "  Z " + pos.getZ() + "。");
            lines.add("§7纸上的墨迹会自己挪动 —— 这便是「脉」。");
            YsText.panel(player, "寻脉图", lines);
            return InteractionResult.SUCCESS_SERVER;
        }
    }

    /**
     * 八卦镜 —— 照一照，让周围的生灵都显出「形」来。
     *
     * <p>风水的本意是「藏风聚气」，而镜的作用是<b>把气照出来</b>。
     * 举镜一照，方圆十六格内的活物都会短时间发光 —— 藏在暗处的、
     * 躲在墙后的，都藏不住。
     */
    public static class BaguaMirror extends Item {

        public BaguaMirror(Properties properties) {
            super(properties);
        }

        @Override
        public InteractionResult use(Level level, Player player, InteractionHand hand) {
            if (level.isClientSide()) {
                return InteractionResult.SUCCESS;
            }
            List<LivingEntity> nearby = level.getEntitiesOfClass(LivingEntity.class,
                    player.getBoundingBox().inflate(16.0D), e -> e != player);
            for (LivingEntity living : nearby) {
                living.addEffect(new MobEffectInstance(MobEffects.GLOWING, 200, 0, false, false));
            }
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0F, 1.4F);
            player.getCooldowns().addCooldown(player.getItemInHand(hand), 60);
            if (!nearby.isEmpty()) {
                YsText.chat(player, "镜面一晃，§e" + nearby.size() + " §f道形影显了出来。");
            } else {
                YsText.chat(player, "镜面清清明明 —— 周围没有活物。");
            }
            return InteractionResult.SUCCESS_SERVER;
        }
    }

    // ==================================================================== 三大术数
    //
    // 第 15 轮：紫微斗数、大六壬、奇门遁甲。
    // 三张盘各管一件事 —— 紫微看「你是谁」，六壬看「事怎么走」，奇门看「往哪走」。
    // 起盘逻辑全部在 com.chenchen.yishu.divination.Oracle，这里只负责「按下去」。

    /**
     * 术盘 —— 右键起一盘。
     *
     * <p>起盘不消耗道具：法器是可以反复用的，代价是冷却。
     * 冷却由盘面自己决定（六壬课体滞涩时会拉长），所以同一件道具在不同时候
     * 好用程度不一样 —— 这正是术数该有的手感。
     */
    public static class OracleDisc extends Item {

        private final com.chenchen.yishu.divination.Oracle.Kind kind;

        public OracleDisc(Properties properties, com.chenchen.yishu.divination.Oracle.Kind kind) {
            super(properties);
            this.kind = kind;
        }

        public com.chenchen.yishu.divination.Oracle.Kind kind() {
            return this.kind;
        }

        @Override
        public InteractionResult use(Level level, Player player, InteractionHand hand) {
            if (level.isClientSide()) {
                return InteractionResult.SUCCESS;
            }
            if (!(level instanceof ServerLevel server)) {
                return InteractionResult.PASS;
            }
            com.chenchen.yishu.divination.Oracle.Reading reading =
                    com.chenchen.yishu.divination.Oracle.perform(this.kind, server, player);
            com.chenchen.yishu.divination.Oracle.apply(player, reading);
            YsText.send(player, reading.title(), reading.lines());
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.85F, 1.15F);
            player.getCooldowns().addCooldown(player.getItemInHand(hand), reading.cooldown());
            return InteractionResult.SUCCESS_SERVER;
        }
    }

    /**
     * 术符 —— 一次性，撕开就用。
     *
     * <p>每道符都对应盘面上的一颗星、一位天将、一扇门。
     * 效果写得直白：不追求「还原古籍」，追求「玩家一眼知道这张符是干什么的」。
     */
    public static class OracleCharm extends Item {

        private final String noteKey;
        private final MobEffectInstance[] grants;

        public OracleCharm(Properties properties, String noteKey, MobEffectInstance... grants) {
            super(properties);
            this.noteKey = noteKey;
            this.grants = grants;
        }

        @Override
        public InteractionResult use(Level level, Player player, InteractionHand hand) {
            if (level.isClientSide()) {
                return InteractionResult.SUCCESS;
            }
            for (MobEffectInstance grant : this.grants) {
                player.addEffect(new MobEffectInstance(grant));
            }
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 0.7F, 1.45F);
            YsText.chat(player, "符纸一燃即尽 —— " + Component.translatable(this.noteKey).getString());
            stackShrink(player, hand);
            return InteractionResult.SUCCESS_SERVER;
        }
    }

    // ==================================================================== 工具方法


    /** 相对方位描述。 */
    public static String compass(int fromX, int fromZ, int toX, int toZ) {
        int dx = toX - fromX;
        int dz = toZ - fromZ;
        String ns = dz < 0 ? "北" : dz > 0 ? "南" : "";
        String ew = dx < 0 ? "西" : dx > 0 ? "东" : "";
        String direction = ew + ns;
        if (direction.isEmpty()) {
            return "正下方";
        }
        return direction + "方向";
    }

    public static String round(double value) {
        return String.valueOf(Math.round(value * 100.0D) / 100.0D);
    }

    private static void stackShrink(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
    }

    /** 把户型报告渲染成文本行（客户端用同一个文本面板显示）。 */
    public static List<String> renderReport(com.chenchen.yishu.fengshui.FloorPlan.Report report) {
        List<String> lines = new ArrayList<>();
        lines.add("── 户型图（上北下南）──");
        var grid = report.grid();
        for (int gz = 0; gz < grid.depth(); gz++) {
            StringBuilder row = new StringBuilder();
            for (int gx = 0; gx < grid.width(); gx++) {
                row.append(switch (grid.at(gx, gz)) {
                    case com.chenchen.yishu.fengshui.FloorPlan.INTERIOR -> "·";
                    case com.chenchen.yishu.fengshui.FloorPlan.PALACE -> "中";
                    case com.chenchen.yishu.fengshui.FloorPlan.WALL -> "█";
                    case com.chenchen.yishu.fengshui.FloorPlan.DOOR -> "门";
                    case com.chenchen.yishu.fengshui.FloorPlan.WINDOW -> "窗";
                    default -> " ";
                });
            }
            lines.add(row.toString());
        }
        lines.add("");
        lines.add("── 房间 ──");
        for (var room : report.rooms()) {
            lines.add(room.describe() + "  " + room.type().note());
        }
        lines.add("");
        lines.add("── 阳宅批断（" + report.score() + "/100 " + report.grade().zh() + "）──");
        for (var issue : report.issues()) {
            lines.add("[" + issue.severityName() + "] " + issue.title());
            lines.add("    " + issue.detail());
            lines.add("    → " + issue.advice());
        }
        return lines;
    }

    /** 把一卦渲染成文本行。 */
    public static List<String> renderReading(Divination.Reading reading) {
        List<String> lines = new ArrayList<>();
        lines.add("所问：" + reading.input().question().zh()
                + "（用神 " + reading.yongShen().zh() + "）");
        lines.add("本卦：" + reading.original().fullName() + " " + reading.original().symbol());
        lines.add("卦辞：" + reading.original().judgment());
        if (reading.hasMoving()) {
            lines.add("动爻：" + reading.movingYao() + "  →  变卦："
                    + reading.changed().name() + " " + reading.changed().symbol());
        } else {
            lines.add("六爻安静，无变卦。");
        }
        lines.add("日辰 " + reading.dayPillar() + " ｜ 月建 " + reading.monthPillar());
        lines.add("");
        lines.add("── 六爻装卦（上爻在上的传统排法）──");
        for (int position : com.chenchen.yishu.liuyao.Hexagram.displayOrder()) {
            var yao = reading.yaos().get(position - 1);
            lines.add(yao.glyph() + "  " + yao.line()
                    + "  旺衰[" + yao.wangShuai() + "]");
        }
        lines.add("");
        lines.add("── 断 ──");
        lines.add(reading.verdict() + "（" + reading.score() + "）");
        for (var clue : reading.clues()) {
            lines.add("  " + clue.label() + "：" + clue.value());
        }
        lines.add("");
        lines.add(reading.omenNote());
        return lines;
    }

    /** 把建筑的命盘报告渲染成文本行。 */
    public static List<String> renderBuilding(long seed, BuildingFate.Fate fate, long epochDay) {
        return BuildingFate.report(seed, fate, epochDay);
    }

    /** 给玩家看的当前维度时辰。 */
    public static String currentHour(DimensionProfile dimension, long dayTime) {
        return dimension.hourName(dayTime) + "（" + dimension.timeSystemName() + "）";
    }

    /** 当日的流日干支。 */
    public static String todayGanZhi(long seed, DimensionProfile dimension, long epochDay) {
        return Yun.liuRi(seed, dimension.shortKey(), epochDay).zh();
    }

    /** 组装一份「今日黄历」。 */
    public static List<String> almanac(long seed, DimensionProfile dimension, long epochDay) {
        List<String> lines = new ArrayList<>();
        lines.add("流日 " + Yun.liuRi(seed, dimension.shortKey(), epochDay)
                + " ｜ 流月 " + Yun.liuYue(seed, dimension.shortKey(), epochDay)
                + " ｜ 流年 " + Yun.liuNian(seed, dimension.shortKey(), epochDay));
        lines.add(FengShuiSystems.Almanac.summary(
                FengShuiSystems.Almanac.ofDay(seed, dimension, epochDay)));
        lines.add("太岁方位：" + FengShuiSystems.TaiSui.directionName(Yun.age(epochDay)));
        return lines;
    }

    /** 一个稳定的「当天的运气」值，供幸运效果使用。 */
    public static double dailyLuck(long seed, DimensionProfile dimension, long epochDay, String subject) {
        YishuRandom random = StableKey.of(seed, "dailyluck")
                .add(dimension.shortKey()).add(epochDay).add(subject).random();
        return random.nextDouble();
    }

    /** 供其他地方复用的卦符渲染。 */
    public static String hexagramGlyph(boolean[] yang) {
        StringBuilder builder = new StringBuilder();
        for (int i = yang.length - 1; i >= 0; i--) {
            builder.append(yang[i] ? '▬' : '▭');
        }
        return builder.toString();
    }

    /** 当前维度档案。 */
    public static DimensionProfile dimensionOf(Level level) {
        return DimensionProfile.of(level.dimension().identifier().toString());
    }

    /** 准星指向的方块（用于罗盘之类需要「对准」的物品）。 */
    public static BlockPos targetedBlock(Player player) {
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getViewVector(1.0F).scale(8.0D));
        HitResult hit = player.level().clip(new ClipContext(eye, end,
                ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        if (hit instanceof BlockHitResult blockHit && hit.getType() == HitResult.Type.BLOCK) {
            return blockHit.getBlockPos();
        }
        return null;
    }

    /** 由八卦方位名反推方向（六爻线索里用到）。 */
    public static Direction directionOf(String name) {
        if (name == null) {
            return Direction.NORTH;
        }
        if (name.contains("东北")) {
            return Direction.NORTH;
        }
        if (name.contains("东南")) {
            return Direction.EAST;
        }
        if (name.contains("西南")) {
            return Direction.SOUTH;
        }
        if (name.contains("西北")) {
            return Direction.WEST;
        }
        if (name.contains("北")) {
            return Direction.NORTH;
        }
        if (name.contains("南")) {
            return Direction.SOUTH;
        }
        if (name.contains("东")) {
            return Direction.EAST;
        }
        return Direction.WEST;
    }

    /** 保留：供界面显示五行颜色。 */
    public static int elementColor(WuXing element) {
        return element.color();
    }

    /** 保留：八卦符号，界面画卦用。 */
    public static String trigramSymbol(Gua.Trigram trigram) {
        return trigram.symbol();
    }

    /** 命盘文本（供讲台导出）。 */
    public static List<String> fateText(FateProfile profile) {
        Bazi bazi = profile.presented();
        List<String> lines = new ArrayList<>();
        lines.add("── 四柱 ──");
        for (Bazi.Pillar pillar : bazi.pillars()) {
            lines.add(pillar.label() + "：" + pillar.ganZhi().display()
                    + " 纳音" + pillar.ganZhi().nayinName()
                    + "（" + pillar.meaning() + "）");
        }
        lines.add("日主：" + bazi.dayMaster().zh() + bazi.dayMasterElement().zh()
                + "　" + bazi.strength().zh());
        lines.add("喜用神：" + bazi.yongShen().primary().zh()
                + "　忌神：" + (bazi.yongShen().unfavorable().isEmpty() ? "无"
                : bazi.yongShen().unfavorable().get(0).zh()));
        lines.add("取用：" + bazi.yongShen().reason());
        lines.add("");
        lines.add("── 神煞 ──");
        for (var mark : com.chenchen.yishu.fate.ShenSha.of(bazi)) {
            lines.add(mark.name() + "：" + mark.note());
        }
        lines.add("");
        lines.add("── 大运 ──");
        for (var step : Yun.daYun(bazi, profile.worldSeed(), "player:" + profile.owner())) {
            lines.add("第 " + step.index() + " 步 " + step.range() + "：" + step.ganZhi());
        }
        lines.add("");
        lines.add("── 当前 ──");
        lines.add("生于 " + FateCalendar.hourName(0) + "之前的虚拟生辰，"
                + "本命卦 " + MobFate.hexagramName(StableKey.of(profile.worldSeed(), "playerhex")
                .add(profile.owner())));
        if (profile.isModified()) {
            lines.add("※ 此命已被改动：" + profile.summary());
        }
        return lines;
    }

    /** 模组 ID 的便捷转发。 */
    public static Component literal(String text) {
        return Component.literal(text);
    }

    /** 供外部引用的模组命名空间。 */
    public static String modId() {
        return YishuMod.MOD_ID;
    }
}
