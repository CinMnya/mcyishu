package com.chenchen.yishu.content;

import com.chenchen.yishu.building.BuildingFate;
import com.chenchen.yishu.building.Ruins;
import com.chenchen.yishu.core.StableKey;
import com.chenchen.yishu.core.YishuRandom;
import com.chenchen.yishu.dimension.DimensionProfile;
import com.chenchen.yishu.fate.GanZhi;
import com.chenchen.yishu.fate.GanZhi.WuXing;
import com.chenchen.yishu.fate.Yun;
import com.chenchen.yishu.fengshui.FengShuiField;
import com.chenchen.yishu.fengshui.TerrainQi;
import com.chenchen.yishu.fengshui.FengShuiSystems;
import com.chenchen.yishu.fengshui.HouseScanner;
import com.chenchen.yishu.liuyao.Divination;
import com.chenchen.yishu.liuyao.Hexagram;
import com.chenchen.yishu.system.PlainSpeak;
import com.chenchen.yishu.system.TutorialSystem;
import com.chenchen.yishu.system.YishuData;
import com.chenchen.yishu.util.YsText;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.TallGrassBlock;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * 易枢 · 方块内容 —— 立在世界里的那一套「器物」。
 *
 * <p>与物品一样放在同一个文件里，理由也一样：它们是一个家庭，
 * 拆开就看不出彼此的关系了。下面按功能分组。
 *
 * <p>所有方块的交互一律遵守同一条纪律：<b>客户端只负责「按下去有反应」，
 * 真正的计算在服务端</b>。{@code useWithoutItem} 在客户端返回 {@code SUCCESS}
 * 只是为了不让手空挥，实际逻辑走服务端那一侧。
 */
public final class YsBlockContent {

    private YsBlockContent() {
    }

    // ==================================================================== 卦盘

    /**
     * 卦盘 —— 模组的红石接口。
     *
     * <p>它把一个六爻卦的<b>六个爻位直接输出成六路红石信号</b>：
     * 每个方向对应一个爻位，爻为阳则输出 15，为阴则输出 0。
     * 于是「六爻的阴阳」变成了一组可以接线的物理信号，红石电路可以直接读它。
     *
     * <p>方向与爻位的对应是固定的（按水平方向与上下）：北=初爻、东=二爻、
     * 南=三爻、西=四爻、上=五爻、下=六爻。固定的对应关系很重要 ——
     * 否则玩家每次换朝向都要重新记一遍。
     *
     * <p>卦象本身由方块状态里的 {@code gua} 属性（0~63）承载，
     * 也就是「上卦序号 × 8 + 下卦序号」。右键可以重掷一卦。
     */
    public static class GuaBoard extends Block {

        /** 0~63，对应六十四卦。 */
        public static final IntegerProperty GUA = IntegerProperty.create("gua", 0, 63);
        public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

        public GuaBoard(Properties properties) {
            super(properties);
            registerDefaultState(this.stateDefinition.any()
                    .setValue(GUA, 0)
                    .setValue(FACING, Direction.NORTH));
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(GUA, FACING);
        }

        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
        }

        @Override
        protected boolean isSignalSource(BlockState state) {
            return true;
        }

        /**
         * 六路并行的爻位输出。
         *
         * <p>红石信号强度只有 4 位（0~15），装不下 6 位的信息，
         * 所以这里走的是「<b>六个方向各出一路</b>」的方案：
         * 每一路只有 0 或 15 两种值，正好对应爻的阴与阳。
         * 想读出一整套卦象，就用六条独立的红石线去接六个面。
         */
        @Override
        protected int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
            boolean[] yao = yaoOf(state.getValue(GUA));
            int index = yaoIndexFor(direction);
            return yao[index] ? 15 : 0;
        }

        @Override
        protected int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
            return getSignal(state, level, pos, direction);
        }

        /** 方向 → 爻位下标（0 = 初爻）。 */
        public static int yaoIndexFor(Direction direction) {
            return switch (direction) {
                case NORTH -> 0;   // 初爻
                case EAST -> 1;    // 二爻
                case SOUTH -> 2;   // 三爻
                case WEST -> 3;    // 四爻
                case UP -> 4;      // 五爻
                case DOWN -> 5;    // 上爻
            };
        }

        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                                   Player player, BlockHitResult hitResult) {
            if (level.isClientSide()) {
                return InteractionResult.SUCCESS;
            }
            if (!(level instanceof ServerLevel server)) {
                return InteractionResult.PASS;
            }
            DimensionProfile dimension = DimensionProfile.of(level.dimension().identifier().toString());
            long epochDay = server.getOverworldClockTime() / 24000L;
            int hourIndex = dimension.hourIndex(server.getOverworldClockTime());

            // 重掷一卦：把方块的坐标当作「主体」，于是同一个卦盘永远出同一卦，
            // 除非日期或时辰变了 —— 它是「随时间变化的红石源」，不是随机源。
            YishuRandom random = StableKey.of(server.getSeed(), "guaboard")
                    .add(dimension.shortKey())
                    .add(pos.getX()).add(pos.getY()).add(pos.getZ())
                    .add(epochDay).add(hourIndex)
                    .random();
            boolean[] yao = new boolean[6];
            for (int i = 0; i < 6; i++) {
                yao[i] = random.nextBoolean();
            }
            int value = valueOf(yao);
            level.setBlockAndUpdate(pos, state.setValue(GUA, value));
            level.playSound(null, pos, SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.BLOCKS, 0.8F, 1.2F);

            Hexagram hexagram = Hexagram.ofYao(yao);
            List<String> lines = new ArrayList<>();
            lines.add("卦盘：第 " + epochDay + " 日 " + dimension.hourName(server.getOverworldClockTime()));
            lines.add("本卦 " + hexagram.fullName() + " " + hexagram.symbol());
            lines.add("卦辞：" + hexagram.judgment());
            lines.add("六爻（上爻在上）：");
            for (int position : Hexagram.displayOrder()) {
                boolean isYang = yao[position - 1];
                lines.add("  " + (isYang ? "▬▬▬▬▬" : "▬▬ ▬▬") + "  " + position + " 爻 → "
                        + directionNameOf(position - 1) + " 面输出 " + (isYang ? "15" : "0"));
            }
            lines.add("接线：北=初爻 东=二爻 南=三爻 西=四爻 上=五爻 下=上爻");
            YsText.send(player, "卦盘 · " + hexagram.name(), lines);
            return InteractionResult.SUCCESS_SERVER;
        }

        private static String directionNameOf(int yaoIndex) {
            return switch (yaoIndex) {
                case 0 -> "北";
                case 1 -> "东";
                case 2 -> "南";
                case 3 -> "西";
                case 4 -> "上";
                default -> "下";
            };
        }
    }

    // ==================================================================== 观气台

    /**
     * 观气台 —— 立在宅子里的「风水仪表」。
     *
     * <p>右键它会做两件事：
     * <ol>
     *   <li>报出它所在位置的风水域详情（气煞阴阳、五行偏向、吉凶等级）；</li>
     *   <li>如果附近有可识别的居所，顺带跑一次户型扫描。</li>
     * </ol>
     *
     * <p>与罗盘的分工：罗盘是<b>手持的、对准门用的</b>；观气台是<b>固定在场、长期看着的</b>。
     * 一个是勘测工具，一个是常驻仪表。
     */
    public static class QiObservatory extends Block {

        public QiObservatory(Properties properties) {
            super(properties);
        }

        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                                   Player player, BlockHitResult hitResult) {
            if (level.isClientSide()) {
                return InteractionResult.SUCCESS;
            }
            if (!(level instanceof ServerLevel server)) {
                return InteractionResult.PASS;
            }
            DimensionProfile dimension = DimensionProfile.of(level.dimension().identifier().toString());
            if (!FengShuiSystems.enabled(dimension)) {
                YsText.send(player, "观气台", List.of("本维度的风水系统已被配置关闭。"));
                return InteractionResult.SUCCESS_SERVER;
            }

            long seed = server.getSeed();
            long epochDay = server.getOverworldClockTime() / 24000L;
            long year = Yun.age(epochDay);

            FengShuiField.Sample sample = TerrainQi.sample(server, dimension, pos.getX(), pos.getZ());
            FengShuiField.Grade grade = sample.grade(dimension);

            List<String> lines = new ArrayList<>();

            // ---- 第二层 · 白话：先给结论，再看细节 ----
            lines.add("§b" + PlainSpeak.MOTTO);
            lines.add("");
            lines.add(PlainSpeak.gradePlain(grade) + "。" + PlainSpeak.gradeAdvice(grade) + "。");
            lines.add(PlainSpeak.qiPlain(sample) + "。");
            String better = PlainSpeak.betterDirection(server.getSeed(), dimension,
                    pos.getX(), pos.getZ(), 48);
            if (better != null && grade != FengShuiField.Grade.GREAT_LUCK) {
                lines.add("提示：" + better + "。");
            }
            lines.add("");

            // ---- 第三层 · 术语（只给专业模式）----
            if (!TutorialSystem.professional(serverPlayer(player))) {
                lines.add("§8（以下为专业数据，看不懂可以忽略）");
            }
            lines.add(YsText.rule("此地风水"));
            lines.add("地块：" + sample.domainLabel());
            lines.add("气 " + sample.qi() + " ｜ 煞 " + sample.sha()
                    + " ｜ 阴 " + sample.yin() + " ｜ 阳 " + sample.yang()
                    + " ｜ 阴阳平衡度 " + sample.balance());
            lines.add("五行偏向：" + sample.element().zh()
                    + "（" + Math.round(sample.elementStrength() * 100) + "%）");
            lines.add("综合评分：" + sample.score(dimension) + "/100 → " + grade.zh());
            lines.add("效用：" + grade.effect());

            double aura = FengShuiSystems.DragonVein.auraStrength(seed, dimension, year,
                    pos.getX(), pos.getZ());
            if (aura > 0.0D) {
                lines.add("★ 龙脉过境，龙气浓度 " + Math.round(aura * 100) + "%（第 " + year + " 年）");
            }

            boolean warded = Warding.isWarded(server, pos);
            lines.add(YsText.rule("太岁"));
            lines.add("今年太岁在 " + FengShuiSystems.TaiSui.directionName(year)
                    + (warded ? "　（此处有石敢当镇之，煞气已化）" : "　（动土招煞）"));

            lines.add(YsText.rule("当日宜忌"));
            lines.addAll(YsItemContent.almanac(seed, dimension, epochDay));

            // ---- 顺手扫一次户型 ----
            BlockPos door = findNearbyDoor(level, pos, 12);
            if (door != null) {
                var report = HouseScanner.scan(level, door, seed, dimension);
                if (report != null) {
                    lines.add("");
                    lines.addAll(YsItemContent.renderReport(report));
                }
            } else {
                lines.add("");
                lines.add("（附近未找到门，无法生成户型图。把观气台放在屋内靠近门的地方即可。）");
            }

            YsText.send(player, "观气台 · " + grade.zh(), lines);
            level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.6F, 1.8F);
            return InteractionResult.SUCCESS_SERVER;
        }

        /** 收窄成 ServerPlayer。 */
        static ServerPlayer serverPlayer(Player player) {
            return player instanceof ServerPlayer serverPlayer ? serverPlayer : null;
        }

        /** 在半径内找一个门方块。 */
        static BlockPos findNearbyDoor(Level level, BlockPos origin, int radius) {
            BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dy = -4; dy <= 4; dy++) {
                    for (int dz = -radius; dz <= radius; dz++) {
                        cursor.set(origin.getX() + dx, origin.getY() + dy, origin.getZ() + dz);
                        BlockState state = level.getBlockState(cursor);
                        String path = net.minecraft.core.registries.BuiltInRegistries.BLOCK
                                .getKey(state.getBlock()).getPath();
                        if (path.endsWith("_door")) {
                            return cursor.immutable();
                        }
                    }
                }
            }
            return null;
        }
    }

    // ==================================================================== 香炉

    /**
     * 香炉 —— 宅中的「净化器」。
     *
     * <p>右键上香。上香的即时效果是：<b>降低周围的风水煞气评价</b>
     * （表现为观气台的评分提升），并在一定时间内让起卦的解读更清晰
     * （降低模糊度）。次数存在方块状态里，用一次点一次。
     *
     * <p>它把「祭祀」这件抽象的事做成了一个有反馈的动作：玩家能看见
     * 自己的宅子因为持续上香而慢慢变好。
     */
    public static class Censer extends Block {

        /** 已上香的次数（0~7），影响净化强度。 */
        public static final IntegerProperty INCENSE = IntegerProperty.create("incense", 0, 7);
        public static final BooleanProperty LIT = BlockStateProperties.LIT;

        public Censer(Properties properties) {
            super(properties);
            registerDefaultState(this.stateDefinition.any()
                    .setValue(INCENSE, 0)
                    .setValue(LIT, Boolean.FALSE));
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(INCENSE, LIT);
        }

        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                                   Player player, BlockHitResult hitResult) {
            if (level.isClientSide()) {
                return InteractionResult.SUCCESS;
            }
            if (!(level instanceof ServerLevel server)) {
                return InteractionResult.PASS;
            }
            int incense = state.getValue(INCENSE);
            if (incense >= 7) {
                YsText.send(player, "香炉", List.of(
                        "炉中香灰已满，此处的气已被理顺到极致。",
                        "把它挪到别处，或者拆掉重放，可以重新开始。"));
                return InteractionResult.SUCCESS_SERVER;
            }
            level.setBlockAndUpdate(pos, state.setValue(INCENSE, incense + 1).setValue(LIT, Boolean.TRUE));
            level.playSound(null, pos, SoundEvents.CANDLE_EXTINGUISH, SoundSource.BLOCKS, 0.6F, 1.6F);
            server.sendParticles(net.minecraft.core.particles.ParticleTypes.CAMPFIRE_COSY_SMOKE,
                    pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D,
                    18, 0.25D, 0.6D, 0.25D, 0.008D);

            DimensionProfile dimension = DimensionProfile.of(level.dimension().identifier().toString());
            long epochDay = server.getOverworldClockTime() / 24000L;
            var almanac = FengShuiSystems.Almanac.ofDay(server.getSeed(), dimension, epochDay);
            boolean favorable = FengShuiSystems.Almanac.isFavorable(almanac, "ritual");

            List<String> lines = new ArrayList<>();
            lines.add("香已燃，第 " + (incense + 1) + " 炷。");
            lines.add("净化强度：" + (incense + 1) + "/7（每炷降低本处煞气约 "
                    + (incense + 1) * 4 + "%）");
            if (favorable) {
                lines.add("§a今日宜祭祀，此炷格外灵验。");
            } else if (FengShuiSystems.Almanac.isTaboo(almanac, "ritual")) {
                lines.add("§c今日忌祭祀，香火打了折扣。");
            }
            lines.add("香炉的作用范围是周围 16 格，多置几座可以连片净化。");
            YsText.send(player, "香炉", lines);
            return InteractionResult.SUCCESS_SERVER;
        }
    }

    // ==================================================================== 石敢当

    /**
     * 石敢当 —— 镇太岁用的。
     *
     * <p>太岁方位上动土会招煞，而在那块地方立一块石敢当就能把煞化掉。
     * 这是整套「太岁」玩法里唯一的解法，也是它有意思的地方：
     * <b>你不能阻止太岁移动，但你可以提前布防。</b>
     *
     * <p>放置时会检查自己是否正落在当年的太岁方位上，并给出明确反馈 ——
     * 玩家不需要自己去查方位表，石头会告诉他。
     */
    public static class ShiGandang extends Block {

        public ShiGandang(Properties properties) {
            super(properties);
        }

        @Override
        public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState,
                            boolean movedByPiston) {
            super.onPlace(state, level, pos, oldState, movedByPiston);
            if (level.isClientSide() || !(level instanceof ServerLevel server)) {
                return;
            }
            long year = Yun.age(server.getOverworldClockTime() / 24000L);
            boolean onTaiSui = FengShuiSystems.TaiSui.inTaiSuiSector(year, pos.getX(), pos.getZ());
            if (onTaiSui) {
                server.sendParticles(net.minecraft.core.particles.ParticleTypes.ENCHANT,
                        pos.getX() + 0.5D, pos.getY() + 1.2D, pos.getZ() + 0.5D,
                        24, 0.4D, 0.4D, 0.4D, 0.4D);
                server.playSound(null, pos, SoundEvents.ENCHANTMENT_TABLE_USE,
                        SoundSource.BLOCKS, 0.7F, 1.4F);
            }
        }
    }

    /** 石敢当的镇煞查询。 */
    public static final class Warding {

        private Warding() {
        }

        /** 半径 {@value #RADIUS} 格内有石敢当即为已镇。 */
        public static final int RADIUS = 12;

        public static boolean isWarded(Level level, BlockPos pos) {
            BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
            for (int dx = -RADIUS; dx <= RADIUS; dx++) {
                for (int dy = -6; dy <= 6; dy++) {
                    for (int dz = -RADIUS; dz <= RADIUS; dz++) {
                        cursor.set(pos.getX() + dx, pos.getY() + dy, pos.getZ() + dz);
                        if (level.getBlockState(cursor).getBlock() instanceof ShiGandang) {
                            return true;
                        }
                    }
                }
            }
            return false;
        }
    }

    // ==================================================================== 遗迹核心

    /**
     * 遗迹核心 —— 十二处遗迹的入口标记。
     *
     * <p>它承载 {@link Ruins} 里的机关规则。玩家带着正确的五行灵石右键它，
     * 就会走一次 {@link Ruins#activate} 判定；成功则亮起、给奖励，失败则说明原因。
     *
     * <p>「今日不受机关」这类反馈很重要 —— 如果只是默默不响应，
     * 玩家会以为是 bug 而不是「时候未到」。
     */
    public static class RuinCore extends Block {

        public static final BooleanProperty ACTIVE = BlockStateProperties.LIT;

        public RuinCore(Properties properties) {
            super(properties);
            registerDefaultState(this.stateDefinition.any().setValue(ACTIVE, Boolean.FALSE));
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(ACTIVE);
        }

        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                                   Player player, BlockHitResult hitResult) {
            if (level.isClientSide()) {
                return InteractionResult.SUCCESS;
            }
            if (!(level instanceof ServerLevel server)) {
                return InteractionResult.PASS;
            }
            DimensionProfile dimension = DimensionProfile.of(level.dimension().identifier().toString());
            long epochDay = server.getOverworldClockTime() / 24000L;
            Ruins.Site site = Ruins.nearest(server.getSeed(), dimension, pos.getX(), pos.getZ());

            List<String> lines = new ArrayList<>();
            lines.add("此处为 " + site.type().zh() + " 的核心。");
            lines.add("机关：" + site.type().mechanism());
            lines.add("需以「" + site.type().catalyst().zh() + "」属性之物相引 —— 手持对应灵石右键。");
            lines.add("奖励：" + site.type().rewardZh());
            lines.add("今日状态：" + (Ruins.isOpenToday(server.getSeed(), dimension, site.type(), epochDay)
                    ? "§a机关已开，可启" : "§c今日卦象不合，暂不受启"));
            lines.add("");
            lines.addAll(YsItemContent.renderBuilding(server.getSeed(), site.fate(), epochDay));
            YsText.send(player, site.type().zh(), lines);
            return InteractionResult.SUCCESS_SERVER;
        }

        /** 由 {@link YsItemContent.ElementStone} 调用。 */
        static InteractionResult tryActivate(ServerLevel server, Player player, BlockPos pos,
                                             WuXing element, ItemStack stack) {
            DimensionProfile dimension = DimensionProfile.of(server.dimension().identifier().toString());
            long epochDay = server.getOverworldClockTime() / 24000L;
            Ruins.Site site = Ruins.nearest(server.getSeed(), dimension, pos.getX(), pos.getZ());

            boolean dragonDefeated = com.chenchen.yishu.system.YishuData.of(server)
                    .get("dragon_defeated_any") != null;
            Ruins.Result result = Ruins.activate(server.getSeed(), dimension, site, element,
                    epochDay, dragonDefeated);

            List<String> lines = new ArrayList<>();
            lines.add(result.message());
            lines.addAll(result.rewards());

            if (result.success()) {
                server.setBlockAndUpdate(pos, server.getBlockState(pos).setValue(ACTIVE, Boolean.TRUE));
                server.sendParticles(net.minecraft.core.particles.ParticleTypes.TOTEM_OF_UNDYING,
                        pos.getX() + 0.5D, pos.getY() + 1.2D, pos.getZ() + 0.5D,
                        60, 0.6D, 0.8D, 0.6D, 0.35D);
                server.playSound(null, pos, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 1.0F, 1.0F);
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
            } else {
                server.playSound(null, pos, SoundEvents.VILLAGER_NO, SoundSource.BLOCKS, 0.8F, 1.0F);
            }
            YsText.send(player, site.type().zh() + (result.success() ? " · 已启" : " · 未启"), lines);
            return InteractionResult.SUCCESS_SERVER;
        }
    }

    // ==================================================================== 阴阳界碑

    /**
     * 阴阳界碑。
     *
     * <p>需要同时献上阴属性与阳属性之物（两块阴阳石）才会开启。
     * 它记录「已经献上了哪一半」，所以两个玩家可以各献一半 —— 这一点是刻意的，
     * 让协作成为可能。
     */
    public static class YinyangStele extends Block {

        public static final BooleanProperty YIN = BooleanProperty.create("yin");
        public static final BooleanProperty YANG = BooleanProperty.create("yang");
        public static final BooleanProperty OPEN = BlockStateProperties.LIT;

        public YinyangStele(Properties properties) {
            super(properties);
            registerDefaultState(this.stateDefinition.any()
                    .setValue(YIN, Boolean.FALSE)
                    .setValue(YANG, Boolean.FALSE)
                    .setValue(OPEN, Boolean.FALSE));
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(YIN, YANG, OPEN);
        }

        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                                   Player player, BlockHitResult hitResult) {
            if (level.isClientSide()) {
                return InteractionResult.SUCCESS;
            }
            YsText.send(player, "阴阳界碑", List.of(
                    "碑面刻着两行字：「孤阴不生，独阳不长。」",
                    "阴已献：" + (state.getValue(YIN) ? "是" : "否")
                            + "　阳已献：" + (state.getValue(YANG) ? "是" : "否"),
                    "手持阴阳石右键，即可献上一半。"));
            return InteractionResult.SUCCESS_SERVER;
        }

        /** 由阴阳石调用。 */
        static InteractionResult offer(ServerLevel server, Player player, BlockPos pos,
                                       boolean yang, ItemStack stack) {
            BlockState state = server.getBlockState(pos);
            BlockState updated = state
                    .setValue(yang ? YANG : YIN, Boolean.TRUE);
            boolean complete = updated.getValue(YIN) && updated.getValue(YANG);
            updated = updated.setValue(OPEN, complete);
            server.setBlockAndUpdate(pos, updated);
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }

            List<String> lines = new ArrayList<>();
            lines.add(yang ? "你献上了「阳」的那一半。" : "你献上了「阴」的那一半。");
            if (complete) {
                lines.add("§a阴阳既济，碑面果然裂开一道门。");
                server.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD,
                        pos.getX() + 0.5D, pos.getY() + 1.2D, pos.getZ() + 0.5D,
                        48, 0.7D, 1.0D, 0.7D, 0.16D);
                server.playSound(null, pos, SoundEvents.END_PORTAL_FRAME_FILL,
                        SoundSource.BLOCKS, 1.0F, 0.8F);
            } else {
                lines.add("还差一半。另一半可以是你自己献的，也可以是别人。");
            }
            YsText.send(player, "阴阳界碑", lines);
            return InteractionResult.SUCCESS_SERVER;
        }
    }

    // ==================================================================== 装饰与祭坛

    /** 五行灵石块 —— 用于龙脉祭坛的激活，也是最主要的建材。 */
    public static class ElementStoneBlock extends Block {

        private final WuXing element;

        public ElementStoneBlock(Properties properties, WuXing element) {
            super(properties);
            this.element = element;
        }

        public WuXing element() {
            return this.element;
        }

        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                                   Player player, BlockHitResult hitResult) {
            if (level.isClientSide()) {
                return InteractionResult.SUCCESS;
            }
            if (!(level instanceof ServerLevel server)) {
                return InteractionResult.PASS;
            }
            DimensionProfile dimension = DimensionProfile.of(level.dimension().identifier().toString());
            long year = Yun.age(server.getOverworldClockTime() / 24000L);
            double aura = FengShuiSystems.DragonVein.auraStrength(server.getSeed(), dimension, year,
                    pos.getX(), pos.getZ());
            List<String> lines = new ArrayList<>();
            lines.add(this.element.zh() + "行灵石。");
            lines.add("五行关系：生" + this.element.generates().zh()
                    + "、克" + this.element.overcomes().zh()
                    + "、被" + this.element.overcomeBy().zh() + "克、"
                    + this.element.generatedBy().zh() + "生我。");
            if (aura > 0.0D) {
                lines.add("★ 此石立在龙脉之上（龙气 " + Math.round(aura * 100) + "%），"
                        + "可作为龙脉祭坛的基座。");
            }
            lines.add("用途：激活遗迹祭坛、压制维度煞气、修筑祭坛基座。");
            YsText.send(player, this.element.zh() + "石", lines);
            return InteractionResult.SUCCESS_SERVER;
        }
    }

    /**
     * 祭坛基座。
     *
     * <p>龙脉祭坛、火煞祭坛、灵魂祭坛等都由它搭出来。
     * 右键可以查看这座坛「应该摆什么」，这是新手最容易卡住的地方，
     * 所以把说明直接放在方块上，而不是塞进 wiki。
     */
    public static class Altar extends Block {

        public Altar(Properties properties) {
            super(properties);
        }

        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                                   Player player, BlockHitResult hitResult) {
            if (level.isClientSide()) {
                return InteractionResult.SUCCESS;
            }
            if (!(level instanceof ServerLevel server)) {
                return InteractionResult.PASS;
            }
            DimensionProfile dimension = DimensionProfile.of(level.dimension().identifier().toString());
            long epochDay = server.getOverworldClockTime() / 24000L;
            long year = Yun.age(epochDay);

            List<String> lines = new ArrayList<>();
            lines.add(YsText.rule("坛上"));
            lines.add("本坛坐落于 " + dimension.zh() + "，时辰体系为「"
                    + dimension.timeSystemName() + "」。");
            lines.add("当前 " + dimension.hourName(server.getOverworldClockTime())
                    + "：" + dimension.hourAdvice(dimension.hourIndex(server.getOverworldClockTime())));

            lines.add(YsText.rule("压制"));
            WuXing need = needElementFor(dimension);
            lines.add("此界需以「" + need.zh() + "」压之，献上对应的五行灵石即可生效。");
            lines.add("原理：" + dimension.zh() + "主气为 " + dimension.dominant().zh()
                    + "，以 " + need.zh() + " 制之可平。");

            lines.add(YsText.rule("龙脉"));
            double aura = FengShuiSystems.DragonVein.auraStrength(server.getSeed(), dimension, year,
                    pos.getX(), pos.getZ());
            lines.add(aura > 0.0D
                    ? "★ 此处正当龙脉（第 " + year + " 年），龙气 " + Math.round(aura * 100)
                            + "% —— 在此立坛可占领这一段龙脉。"
                    : "此处不在龙脉上。用寻龙尺找方向，或等明年龙脉游走过来。");

            lines.add(YsText.rule("太岁"));
            lines.add("今年太岁在 " + FengShuiSystems.TaiSui.directionName(year)
                    + "。在太岁方位上动土会招煞，需立石敢当化解。");

            YsText.send(player, "祭坛", lines);
            return InteractionResult.SUCCESS_SERVER;
        }

        /** 每个维度该用哪一行去压制。 */
        public static WuXing needElementFor(DimensionProfile dimension) {
            return switch (dimension) {
                case NETHER -> WuXing.SHUI;   // 火煞炽盛，以水制之（而水正是下界最缺的东西）
                case END -> WuXing.TU;        // 虚空无物，以实体之土去撬
                default -> WuXing.MU;         // 主世界以木生气
            };
        }
    }

    /** 八卦台 —— 用以摆阵、校准方位的基座。 */
    public static class BaguaPlatform extends Block {

        public BaguaPlatform(Properties properties) {
            super(properties);
        }

        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                                   Player player, BlockHitResult hitResult) {
            if (level.isClientSide()) {
                return InteractionResult.SUCCESS;
            }
            if (!(level instanceof ServerLevel server)) {
                return InteractionResult.PASS;
            }
            List<String> lines = new ArrayList<>();
            lines.add("八卦台。上刻后天八卦方位：");
            lines.add("  乾（西北）　坎（正北）　艮（东北）");
            lines.add("  震（正东）　　中宫　　　兑（正西）");
            lines.add("  巽（东南）　离（正南）　坤（西南）");
            lines.add("");
            lines.add("站上台面，可以察知自己所处方位的宜忌。");
            lines.add(YsItemContent.compass(0, 0, pos.getX(), pos.getZ()));
            lines.add(FengShuiSystems.TaiSui.directionName(Yun.age(server.getOverworldClockTime() / 24000L))
                    + " 是今年太岁所在。");
            YsText.send(player, "八卦台", lines);
            return InteractionResult.SUCCESS_SERVER;
        }
    }

    /** 太极图 —— 纯装饰，但也是阴阳界的门面。 */
    public static class TaijiDiagram extends Block {

        public TaijiDiagram(Properties properties) {
            super(properties);
        }

        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                                   Player player, BlockHitResult hitResult) {
            if (level.isClientSide()) {
                return InteractionResult.SUCCESS;
            }
            if (!(level instanceof ServerLevel server)) {
                return InteractionResult.PASS;
            }
            DimensionProfile dimension = DimensionProfile.of(level.dimension().identifier().toString());
            FengShuiField.Sample sample = TerrainQi.sample(server, dimension,
                    pos.getX(), pos.getZ());
            YsText.send(player, "太极图", List.of(
                    "阴阳鱼缓缓转动，与地气相应。",
                    "此处的阴阳：阴 " + sample.yin() + " ｜ 阳 " + sample.yang()
                            + "　平衡度 " + sample.balance(),
                    sample.balance() >= 40
                            ? "阴阳相济，此处气机甚正。"
                            : "阴阳偏枯，宜以五行灵石调剂。"));
            return InteractionResult.SUCCESS_SERVER;
        }
    }

    /** 龙脉核心碎片块 —— 占领龙脉时立下的标记。 */
    public static class DragonCoreBlock extends Block {

        public DragonCoreBlock(Properties properties) {
            super(properties);
        }

        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                                   Player player, BlockHitResult hitResult) {
            if (level.isClientSide()) {
                return InteractionResult.SUCCESS;
            }
            if (!(level instanceof ServerLevel server)) {
                return InteractionResult.PASS;
            }
            DimensionProfile dimension = DimensionProfile.of(level.dimension().identifier().toString());
            long epochDay = server.getOverworldClockTime() / 24000L;
            long year = Yun.age(epochDay);
            int nodeIndex = FengShuiSystems.DragonVein.nodeIndexOf(server.getSeed(), dimension, year,
                    pos.getX(), pos.getZ());
            YishuData data = YishuData.of(server);
            String owner = data.veinOwner(dimension.shortKey(), year, nodeIndex);
            boolean claimed = owner != null;

            if (!claimed) {
                boolean ok = data.claimVein(dimension.shortKey(), year, nodeIndex,
                        player.getGameProfile().name());
                if (ok) {
                    server.sendParticles(net.minecraft.core.particles.ParticleTypes.PORTAL,
                            pos.getX() + 0.5D, pos.getY() + 1.5D, pos.getZ() + 0.5D,
                            80, 1.2D, 1.0D, 1.2D, 0.08D);
                    server.playSound(null, pos, SoundEvents.ENDER_DRAGON_GROWL,
                            SoundSource.BLOCKS, 0.7F, 1.6F);
                }
            }

            List<String> lines = new ArrayList<>();
            lines.add("龙脉核心 · 第 " + year + " 年 · 第 " + nodeIndex + " 段");
            lines.add(claimed
                    ? "此段已由 " + owner + " 立契占领。"
                    : "此段尚无主。你立下了契约，它归你了。");
            lines.add("占领者的收益：在此段龙脉之上，一切采集与农作皆得额外加成。");
            lines.add("龙脉每年游走一次，契约也随之失效 —— 明年要重新去找。");
            YsText.send(player, "龙脉核心", lines);
            return InteractionResult.SUCCESS_SERVER;
        }
    }

    // ==================================================================== 辅助

    /** 由六爻布尔数组算出 0~63 的方块状态值。 */
    public static int valueOf(boolean[] yao) {
        int value = 0;
        for (int i = 0; i < 6 && i < yao.length; i++) {
            if (yao[i]) {
                value |= 1 << i;
            }
        }
        return value;
    }

    /** 由卦盘状态值反推六爻。 */
    public static boolean[] yaoOf(int value) {
        boolean[] yao = new boolean[6];
        for (int i = 0; i < 6; i++) {
            yao[i] = ((value >> i) & 1) != 0;
        }
        return yao;
    }

    /** 供别的类构造本地化名称。 */
    public static Component name(String key) {
        return Component.translatable(key);
    }

    /** 六十四卦的中文名（供方块 tooltip）。 */
    public static String guaName(int value) {
        return Hexagram.ofYao(yaoOf(value)).name();
    }

    /** 让 {@link Pair} 的引用不被优化掉（静态初始化保证顺序）。 */
    static {
        GanZhi.WuXing.VALUES.getClass();
    }

    /** 未使用的兜底：把建筑命理的常量引入编译单元，避免被裁剪。 */
    public static final String BUILDING_NOTE = BuildingFate.CALAMITY_THRESHOLD >= 0
            ? "渡劫判定已启用" : "";

    /** 防止 {@link Divination} 的静态表在极端情况下未初始化。 */
    public static Divination.Question question(int index) {
        return Divination.Question.VALUES[Math.floorMod(index, Divination.Question.VALUES.length)];
    }

    // ==================================================================== 维度建材

    /**
     * 维度之草 —— 会随风摇曳的装饰草。
     *
     * <p>三种维度草共用这一个类，靠贴图区分。骨粉对它们无效：
     * 灵力浇灌的草不必再长，它们本身就是那个世界最细的呼吸。
     */
    public static class YsGrass extends TallGrassBlock {

        public YsGrass(Properties properties) {
            super(properties);
        }

        @Override
        public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
            return false;
        }
    }

    /**
     * 维度楼梯 —— 与维度砖同色的台阶。
     *
     * <p>构造时快照基座方块状态，其余交给原版 {@link StairBlock} 的完整逻辑：
     * 转角相扣、上下翻转、水源包裹，一样都不少。
     */
    public static class YsStairs extends StairBlock {

        public YsStairs(BlockState baseState, Properties properties) {
            super(baseState, properties);
        }
    }

    // ==================================================================== 维度林木

    /**
     * 界域树苗 —— 种下去能长成本界域的树。
     *
     * <p>{@link SaplingBlock} 的构造是 protected，用一个壳子接住；
     * 生长表（TreeGrower）在注册中枢里指向本模组的 {@code <树种>_tree}
     * 配置特征 —— 与原生树苗走的是同一条生长管线。
     */
    public static class YsSapling extends SaplingBlock {

        public YsSapling(TreeGrower treeGrower, Properties properties) {
            super(treeGrower, properties);
        }
    }


    /**
     * 界域作物 —— 四段生长，和原版甜菜一个模子。
     *
     * <p>{@link CropBlock} 只认 {@code AGE_7}（八段），想用四段就得
     * 自己声明 {@code AGE_3} 并覆写取属性的三个方法 —— 这里照抄
     * 26.2 的 {@code BeetrootBlock} 版式。
     *
     * <p>种子用 {@link Supplier} 延迟解析：方块在类初始化时就注册，
     * 而种子物品排在它后面，直接引用会拿到 null。
     */
    public static class YsCrop extends CropBlock {

        private static final int MAX_AGE = 3;
        private static final IntegerProperty AGE = BlockStateProperties.AGE_3;
        private final Supplier<ItemLike> seed;

        public YsCrop(Properties properties, Supplier<ItemLike> seed) {
            super(properties);
            this.seed = seed;
        }

        @Override
        protected IntegerProperty getAgeProperty() {
            return AGE;
        }

        @Override
        public int getMaxAge() {
            return MAX_AGE;
        }

        @Override
        protected ItemLike getBaseSeedId() {
            return this.seed.get();
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(AGE);
        }
    }

    // ==================================================================== 三大术数 · 术坛

    /**
     * 术坛 —— 紫微星台 / 六壬式坛 / 奇门局阵。
     *
     * <p>三座坛对应三张盘。它们同时扮演两个角色：
     * <ul>
     *   <li><b>查询台</b>：右键起一盘，冷却比随身法器短一半 —— 固定装置总比手上那件顺手。</li>
     *   <li><b>工位</b>：摆一座坛，附近的失业村民会转职成对应的术数手艺人
     *       （星命师 / 课师 / 局师），这是第 15 轮新增的三个职业。</li>
     * </ul>
     *
     * <p>坛有朝向 —— 术数讲方位，一块没有朝向的石头不叫坛。
     */
    public static class OracleAltar extends Block {

        public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

        private final com.chenchen.yishu.divination.Oracle.Kind kind;

        public OracleAltar(Properties properties, com.chenchen.yishu.divination.Oracle.Kind kind) {
            super(properties);
            this.kind = kind;
            registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
        }

        public com.chenchen.yishu.divination.Oracle.Kind kind() {
            return this.kind;
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(FACING);
        }

        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
        }

        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                                   Player player, BlockHitResult hitResult) {
            if (level.isClientSide()) {
                return InteractionResult.SUCCESS;
            }
            if (!(level instanceof ServerLevel server)) {
                return InteractionResult.PASS;
            }
            com.chenchen.yishu.divination.Oracle.Reading reading =
                    com.chenchen.yishu.divination.Oracle.perform(this.kind, server, player);
            com.chenchen.yishu.divination.Oracle.apply(player, reading);
            YsText.send(player, reading.title() + " · " + this.kind.zh(), reading.lines());
            level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME,
                    SoundSource.BLOCKS, 1.0F, 1.0F);
            server.sendParticles(net.minecraft.core.particles.ParticleTypes.ENCHANT,
                    pos.getX() + 0.5D, pos.getY() + 1.1D, pos.getZ() + 0.5D,
                    28, 0.7D, 0.7D, 0.7D, 0.35D);
            // 固定装置的手感：冷却只有随身法器的一半
            player.getCooldowns().addCooldown(new ItemStack(state.getBlock().asItem()),
                    Math.max(200, reading.cooldown() / 2));
            return InteractionResult.SUCCESS_SERVER;
        }
    }
}

