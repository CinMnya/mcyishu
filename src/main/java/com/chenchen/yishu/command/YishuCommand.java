package com.chenchen.yishu.command;

import com.chenchen.yishu.YishuConfig;
import com.chenchen.yishu.calendar.YishuCalendar;
import com.chenchen.yishu.divination.MinorSchools;
import com.chenchen.yishu.content.YsItemContent;
import com.chenchen.yishu.core.YsReg;
import com.chenchen.yishu.dimension.DimensionProfile;
import com.chenchen.yishu.fate.Bazi;
import com.chenchen.yishu.fate.FateProfile;
import com.chenchen.yishu.fate.GanZhi;
import com.chenchen.yishu.fate.Yun;
import com.chenchen.yishu.fengshui.FengShuiField;
import com.chenchen.yishu.fengshui.TerrainQi;
import com.chenchen.yishu.fengshui.FengShuiSystems;
import com.chenchen.yishu.liuyao.Divination;
import com.chenchen.yishu.network.YsNetworking;
import com.chenchen.yishu.spirit.MobFate;
import com.chenchen.yishu.system.PlainSpeak;
import com.chenchen.yishu.system.TutorialSystem;
import com.chenchen.yishu.util.YsText;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * The {@code /yishu} command tree. <b>All command text is English.</b>
 *
 * <p>设计哲学是「不需要指令也能玩」—— 一切都能通过物品与方块触及。
 * through items and blocks. This tree is only a faster keyboard path and never
 * introduces new game logic.
 *
 * <pre>
 * /yishu help                 — this page
 * /yishu bazi                 — your fate chart, in plain words
 * /yishu divine &lt;question&gt;    — cast a hexagram (uses the daily limit)
 * /yishu fengshui             — feng shui of the ground you stand on
 * /yishu almanac              — today's do's and don'ts
 * /yishu tutorial             — tutorial progress
 * /yishu professional         — toggle professional (term) mode
 * /yishu ruins [name]         — list / locate the mod's ruins
 * /yishu taisui               — this year's Tai Sui direction
 * /yishu merit                — your karma / merit standing
 * /yishu vein                 — are you on a dragon vein?
 * /yishu mob                  — read the nearest mob's fate
 * /yishu travel &lt;realm&gt;       — travel to one of the six realms
 * /yishu admin ...            — operator commands
 * </pre>
 */
public final class YishuCommand {

    private YishuCommand() {
    }

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, context, selection) ->
                registerTree(dispatcher));
    }

    // ==================================================================== tree

    private static void registerTree(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("yishu");

        root.then(Commands.literal("help").executes(YishuCommand::help));
        root.then(Commands.literal("bazi").executes(YishuCommand::bazi));
        root.then(Commands.literal("fengshui").executes(YishuCommand::fengshui));
        root.then(Commands.literal("almanac").executes(YishuCommand::almanac));
        root.then(Commands.literal("tutorial").executes(YishuCommand::tutorial));
        root.then(Commands.literal("professional").executes(YishuCommand::professional));
        root.then(Commands.literal("divine")
                .then(Commands.argument("question", StringArgumentType.word())
                        .suggests(QUESTION_SUGGESTIONS)
                        .executes(YishuCommand::divine)));
        root.then(Commands.literal("ruins")
                .executes(YishuCommand::ruinsList)
                .then(Commands.argument("name", StringArgumentType.word())
                        .suggests(RUINS_SUGGESTIONS)
                        .executes(YishuCommand::ruinsLocate)));
        root.then(Commands.literal("taisui").executes(YishuCommand::taisui));
        root.then(Commands.literal("merit").executes(YishuCommand::merit));
        root.then(Commands.literal("vein").executes(YishuCommand::vein));
        root.then(Commands.literal("mob").executes(YishuCommand::mob));
        root.then(Commands.literal("oracle")
                .then(Commands.argument("school", StringArgumentType.word())
                        .executes(YishuCommand::oracleSchool)));
        root.then(Commands.literal("calendar").executes(YishuCommand::calendar)
                .then(Commands.literal("setyear")
                        .then(Commands.argument("year", IntegerArgumentType.integer(
                                YishuCalendar.YEAR_MIN, YishuCalendar.YEAR_MAX))
                                .executes(YishuCommand::calendarSetYear)))
                .then(Commands.literal("setday")
                        .then(Commands.argument("offset", IntegerArgumentType.integer())
                                .executes(YishuCommand::calendarSetDay))));
        root.then(Commands.literal("travel")
                .then(Commands.argument("realm", StringArgumentType.word())
                        .suggests(REALM_SUGGESTIONS)
                        .executes(YishuCommand::travel)));

        // ---- operator section ----
        LiteralArgumentBuilder<CommandSourceStack> admin = Commands.literal("admin");
        // 26.2 permission system: COMMANDS_ADMIN ~= legacy op level 2
        admin.requires(source -> source.permissions().hasPermission(
                net.minecraft.server.permissions.Permissions.COMMANDS_ADMIN));
        admin.then(Commands.literal("tutorial")
                .then(Commands.literal("on").executes(ctx -> setTutorial(ctx, true)))
                .then(Commands.literal("off").executes(ctx -> setTutorial(ctx, false))));
        admin.then(Commands.literal("limit")
                .then(Commands.argument("count", IntegerArgumentType.integer(0, 64))
                        .executes(YishuCommand::setLimit)));
        root.then(admin);

        dispatcher.register(root);
    }

    // ==================================================================== player commands

    /** The command index. */
    private static int help(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        List<String> lines = List.of(
                "§b/yishu help §f—— 这一页。",
                "§b/yishu bazi §f—— 看你的命：五行，以及什么对你有利。",
                "§b/yishu divine <问题> §f—— 起一卦，例如 §e/yishu divine find_ore§f。",
                "§b/yishu fengshui §f—— 测一测你脚下的这块地。",
                "§b/yishu almanac §f—— 今日宜与忌。",
                "§b/yishu calendar [setyear|setday] §f—— 黄历时刻 / 设定本界域纪元。",
                "§b/yishu oracle <school> §f—— 起一门术数；不写名字则列出全部。",
                "§b/yishu tutorial §f—— 教学走到哪一步了。",
                "§b/yishu professional §f—— 切换专业模式（术语层）。",
                "§b/yishu ruins <名称> §f—— 定位一处遗迹；§e/yishu ruins§f 列出全部。",
                "§b/yishu taisui §f—— 今年太岁方位；那个方向不宜动土。",
                "§b/yishu merit §f—— 你的因果功德；积善有福报，作恶有雷劫。",
                "§b/yishu vein §f—— 你是否站在龙脉上，最近的气节点在哪。",
                "§b/yishu mob §f—— 读最近一只生物的命。",
                "§b/yishu travel <界域> §f—— 前往六大界域之一。",
                "§7以上每一条都与对应物品做的事完全相同 —— 只是更快。");
        YsText.panel(ctx.getSource().getPlayerOrException(), "易枢指令", lines);
        return 1;
    }

    /** Fate chart summary. */
    private static int bazi(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        ServerLevel server = player.level();
        FateProfile profile = FateProfile.of(server.getSeed(), player.getGameProfile().name());
        Bazi bazi = profile.active();

        List<String> lines = new ArrayList<>();
        lines.add(PlainSpeak.fatePlain(bazi) + "。");
        lines.add("");
        lines.addAll(PlainSpeak.fateHelp(bazi));
        YsText.panel(player, "你的命盘", lines);
        return 1;
    }

    /** Feng shui underfoot. */
    private static int fengshui(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        ServerLevel server = player.level();
        DimensionProfile dimension = DimensionProfile.of(server.dimension().identifier().getPath());
        FengShuiField.Sample sample = TerrainQi.sample(server, dimension,
                player.getBlockX(), player.getBlockZ());
        FengShuiField.Grade grade = sample.grade(dimension);

        List<String> lines = new ArrayList<>();
        lines.add(PlainSpeak.gradePlain(grade) + "。" + PlainSpeak.gradeAdvice(grade) + "。");
        lines.add(PlainSpeak.qiPlain(sample) + "。");
        lines.add("§7地形：" + TerrainQi.terrainNote(server,
                player.getBlockX(), player.getBlockZ()));
        lines.add("§7挖山、引水、种树都会改这里的气（约 3 秒后重算）。");
        String better = PlainSpeak.betterDirection(server.getSeed(), dimension,
                player.getBlockX(), player.getBlockZ(), 48);
        if (better != null && grade != FengShuiField.Grade.GREAT_LUCK) {
            lines.add("提示：" + better + "。");
        }
        if (TutorialSystem.professional(player)) {
            lines.add("");
            lines.add("§7---- 专业模式 ----");
            lines.add("气 " + sample.qi() + " ｜ 煞 " + sample.sha()
                    + " ｜ 阴 " + sample.yin() + " ｜ 阳 " + sample.yang());
            lines.add("评级：" + grade.zh() + "（" + sample.score(dimension) + "/100）");
        }
        YsText.panel(player, "风水", lines);
        return 1;
    }

    /** Today's almanac. */
    private static int almanac(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        ServerLevel server = player.level();
        DimensionProfile dimension = DimensionProfile.of(server.dimension().identifier().getPath());
        long epochDay = server.getOverworldClockTime() / 24000L;
        List<FengShuiSystems.Almanac.Item> items =
                FengShuiSystems.Almanac.ofDay(server.getSeed(), dimension, epochDay);

        List<String> lines = new ArrayList<>();
        lines.add("§a宜§f：" + summaryOf(items, true));
        lines.add("§c忌§f：" + summaryOf(items, false));
        lines.add("");
        lines.add("§7顺之则吉，逆之小有磕碰 —— 绝不致命。");
        if (TutorialSystem.professional(player)) {
            for (FengShuiSystems.Almanac.Item item : items) {
                lines.add("§8- " + item.zh() + "（" + (item.favorable() ? "宜" : "忌")
                        + "，强度 " + item.magnitude() + "）");
            }
        }
        YsText.panel(player, "黄历 · 第 " + (epochDay + 1) + " 天", lines);
        return 1;
    }

    /** Tutorial progress. */
    private static int tutorial(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        ServerLevel server = player.level();
        long epochDay = server.getOverworldClockTime() / 24000L;
        int stage = TutorialSystem.stageForDay(epochDay);

        List<String> lines = new ArrayList<>();
        if (!TutorialSystem.enabled()) {
            lines.add("本服务器关闭了教学，内容直接全部解锁。");
        } else {
            lines.add("第 §e" + (epochDay + 1) + "§f 天，阶段 §e" + (stage + 1) + "/"
                    + (TutorialSystem.MAX_STAGE + 1) + "§f。");
            lines.add("");
            lines.add("§7已解锁：" + TutorialSystem.stageHint(stage));
            if (stage < TutorialSystem.MAX_STAGE) {
                lines.add("§8下一步：" + TutorialSystem.stageHint(stage + 1));
            }
        }
        if (TutorialSystem.professional(player)) {
            lines.add("§b专业模式：开启");
        }
        YsText.panel(player, "教学", lines);
        return 1;
    }

    /** Toggle professional mode. */
    private static int professional(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        ServerLevel server = player.level();
        long epochDay = server.getOverworldClockTime() / 24000L;
        int requiredStage = TutorialSystem.STAGE_DAYS.length - 1;
        boolean op = ctx.getSource().permissions().hasPermission(
                net.minecraft.server.permissions.Permissions.COMMANDS_ADMIN);
        if (!TutorialSystem.unlocked(epochDay, requiredStage) && !op) {
            YsText.chat(player, "专业模式将在第 §e" + (requiredStage + 1)
                    + "§f 天解锁（管理员不受限）。");
            return 0;
        }
        boolean now = TutorialSystem.toggleProfessional(player);
        YsNetworking.sendSeed(player, server);
        YsText.chat(player, now
                ? "§b专业模式已开启：命盘、卦象与风水将附带术语。"
                : "专业模式已关闭：只讲白话。");
        return 1;
    }

    /** Quick hexagram — same code path as the bronze coins, same results. */
    private static int divine(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        String arg = StringArgumentType.getString(ctx, "question");
        Divination.Question question = matchQuestion(arg);
        if (question == null) {
            YsText.chat(player, "不认识这个问题「" + arg + "」。可选：§e"
                    + QUESTIONS_LIST + "§f（均为英文名）。");
            return 0;
        }
        // Time method: no coins consumed, but the daily limit still applies.
        YsNetworking.handleCast(player, new YsNetworking.CastRequestPayload(
                question.ordinal(), Divination.Method.TIME.ordinal(), 0L));
        return 1;
    }

    // ==================================================================== ruins

    /** id、英文名（指令输入用）、中文名、所在维度 —— 与 README 同步。 */
    private static final List<String[]> RUINS = List.of(
            new String[]{"bagua_ruins", "Bagua Ring Ruins", "八卦环墟", "主世界"},
            new String[]{"taisui_shrine", "Taisui Shrine", "太岁祠", "主世界"},
            new String[]{"dragon_altar", "Dragon Vein Altar", "龙脉祭坛", "主世界"},
            new String[]{"soul_altar", "Soul Altar", "灵魂祭坛", "下界"},
            new String[]{"flame_altar", "Flame Altar", "烈焰祭坛", "下界"},
            new String[]{"void_altar", "Void Altar", "虚空祭坛", "末地"},
            new String[]{"astral_observatory", "Astral Observatory", "观星台", "命理界"},
            new String[]{"jade_pagoda", "Jade Pagoda", "聚灵塔", "风水界"},
            new String[]{"yao_altar", "Hexagram Altar", "卦台", "六爻界"});

    private static final SuggestionProvider<CommandSourceStack> RUINS_SUGGESTIONS =
            (ctx, builder) -> {
                List<String> options = new ArrayList<>();
                for (String[] r : RUINS) {
                    options.add(r[1]);
                    options.add(r[0]);
                }
                return SharedSuggestionProvider.suggest(options, builder);
            };

    private static int ruinsList(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        List<String> lines = new ArrayList<>();
        lines.add("世界各地埋着六座巨型遗迹，三个术数界域各有一座神殿。");
        lines.add("每座都超过一百格宽：外壳一致、内部各不相同，地下还藏着密室。");
        lines.add("");
        for (String[] r : RUINS) {
            lines.add("§b" + r[2] + "§f · " + r[1] + " §8(" + r[0] + ") §f—— " + r[3]);
        }
        lines.add("");
        lines.add("用 §e/yishu ruins <名称>§f 定位最近的一座，例如 §e/yishu ruins bagua§f。");
        YsText.panel(ctx.getSource().getPlayerOrException(), "遗迹与神殿", lines);
        return 1;
    }

    private static int ruinsLocate(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        ServerLevel server = player.level();
        String arg = StringArgumentType.getString(ctx, "name").trim();
        String[] found = matchRuin(arg);
        if (found == null) {
            YsText.chat(player, "没有叫「" + arg + "」的遗迹，用 §e/yishu ruins§f 看列表。");
            return 0;
        }
        String id = found[0];
        String name = found[2];
        String dimension = found[3];

        TagKey<Structure> tag = TagKey.create(Registries.STRUCTURE, YsReg.id(id));
        BlockPos pos = server.findNearestMapStructure(tag, player.blockPosition(), 100, false);
        if (pos == null) {
            YsText.chat(player, "附近找不到 §b" + name + "§f —— 它生成在§e"
                    + dimension + "§f，先去那里吧。");
            return 0;
        }
        int dx = pos.getX() - player.getBlockX();
        int dz = pos.getZ() - player.getBlockZ();
        int dist = (int) Math.sqrt((double) dx * dx + (double) dz * dz);
        String dir = YsItemContent.compass(0, 0, dx, dz);
        List<String> lines = new ArrayList<>();
        lines.add("§b" + name + "§f（" + dimension + "）就在外面：");
        lines.add("§e" + dir + "，约 " + dist + " 格§f之外。");
        lines.add("坐标：X " + pos.getX() + "  Z " + pos.getZ()
                + "（地表 Y 约 " + pos.getY() + "）。");
        if (dist > 300) {
            lines.add("§7路途不近 —— 带上床和食物。");
        }
        YsText.panel(player, "定位 · " + name, lines);
        return 1;
    }

    private static String[] matchRuin(String input) {
        String normalized = input.trim();
        String lower = normalized.toLowerCase(Locale.ROOT);
        for (String[] r : RUINS) {
            if (r[1].equalsIgnoreCase(normalized) || r[0].equalsIgnoreCase(normalized)
                    || r[0].startsWith(lower)
                    || r[1].toLowerCase(Locale.ROOT).contains(lower)) {
                return r;
            }
        }
        return null;
    }

    // ==================================================================== taisui / vein / mob

    private static int taisui(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        ServerLevel server = player.level();
        long year = Yun.age(server.getOverworldClockTime() / 24000L);
        String dir = FengShuiSystems.TaiSui.directionName(year);
        GanZhi.Pair gz = FengShuiSystems.TaiSui.ganZhi(year);

        List<String> lines = new ArrayList<>();
        lines.add("今年是 §e" + gz.gan().zh() + gz.zhi().zh()
                + "§f 年，太岁坐镇§e" + dir + "§f。");
        lines.add("");
        lines.add("今年不宜朝" + dir + "破土 —— 会招来煞气。");
        lines.add("非建不可的话，立一块石敢当即可化解。");
        YsText.panel(player, "太岁", lines);
        return 1;
    }

    /** /yishu merit —— 因果功德（第 20 轮）。 */
    private static int merit(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        ServerLevel server = player.level();
        long value = com.chenchen.yishu.system.MeritSystem.meritOf(server, player);
        int tier = value >= 500 ? 4 : value >= 200 ? 3 : value >= 50 ? 2
                : value >= -49 ? 1 : value >= -99 ? 0 : -1;
        String tierZh = switch (tier) {
            case 4 -> "§e功德圆满";
            case 3 -> "§6福报深厚";
            case 2 -> "§a善缘渐结";
            case 1 -> "§7因果寻常";
            case 0 -> "§c恶名渐著";
            default -> "§4业火缠身";
        };

        List<String> lines = new ArrayList<>();
        lines.add("当前功德：§e" + value + "§f　§7（" + tierZh + "§7）");
        lines.add("");
        lines.add("§7积德之道：喂饱牲畜、点化僵尸村民、善待灵兽。");
        lines.add("§7损德之行：滥杀村民、灵兽与牲畜。");
        if (value >= 200) {
            lines.add("§a福报深厚：常怀幸运。");
        }
        if (value <= -100) {
            lines.add("§4业火缠身：雷雨天谨防天谴。");
        }
        YsText.panel(player, "因果", lines);
        return 1;
    }

    private static int vein(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        ServerLevel server = player.level();
        DimensionProfile dimension = DimensionProfile.of(server.dimension().identifier().getPath());
        long year = Yun.age(server.getOverworldClockTime() / 24000L);
        int x = player.getBlockX();
        int z = player.getBlockZ();

        List<String> lines = new ArrayList<>();
        if (FengShuiSystems.DragonVein.isOnVein(server.getSeed(), dimension, year, x, z)) {
            double aura = FengShuiSystems.DragonVein.auraStrength(server.getSeed(), dimension, year, x, z);
            lines.add("§a* 你正站在龙脉上！§f 气机强度 §e"
                    + Math.round(aura * 100) + "%§f。");
            lines.add("在这里安家，加成拉满。");
        } else {
            FengShuiSystems.DragonVein.Node node = FengShuiSystems.DragonVein.nearestNode(
                    server.getSeed(), dimension, year, x, z);
            int dx = node.x() - x;
            int dz = node.z() - z;
            int dist = (int) Math.sqrt((double) dx * dx + (double) dz * dz);
            lines.add("脚下没有龙气。最近的气节点：");
            lines.add("§e" + YsItemContent.compass(0, 0, dx, dz) + "，约 " + dist + " 格§f。");
            lines.add("§7龙脉每年游走 —— 此结果仅今年有效。");
        }
        YsText.panel(player, "龙脉", lines);
        return 1;
    }

    private static int mob(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        ServerLevel server = player.level();
        Mob nearest = null;
        double best = Double.MAX_VALUE;
        for (Mob candidate : server.getEntitiesOfClass(Mob.class,
                player.getBoundingBox().inflate(16.0D), MobFate::isSubject)) {
            double d = candidate.distanceToSqr(player);
            if (d < best) {
                best = d;
                nearest = candidate;
            }
        }
        if (nearest == null) {
            YsText.chat(player, "16 格内没有可读的活物，再靠近一点。");
            return 0;
        }
        var profile = MobFate.profileOf(server.getSeed(), nearest);
        String name = nearest.getName().getString();
        YsText.panel(player, "生物命理 · " + name,
                List.of(MobFate.inspectText(profile, name)));
        return 1;
    }

    // ==================================================================== travel

    /** realm id, display, dimension key. */
    private static final List<String[]> REALMS = List.of(
            new String[]{"overworld", "Overworld", "minecraft:overworld"},
            new String[]{"nether", "Nether", "minecraft:the_nether"},
            new String[]{"end", "End", "minecraft:the_end"},
            new String[]{"fate_realm", "Fate Realm", "yishu:fate_realm"},
            new String[]{"fengshui_realm", "Feng Shui Realm", "yishu:fengshui_realm"},
            new String[]{"liuyao_realm", "Liu Yao Realm", "yishu:liuyao_realm"},
            new String[]{"ziwei_realm", "Zi Wei Realm", "yishu:ziwei_realm"},
            new String[]{"liuren_realm", "Liu Ren Realm", "yishu:liuren_realm"},
            new String[]{"qimen_realm", "Qi Men Realm", "yishu:qimen_realm"},
            new String[]{"taiyi_realm", "Tai Yi Realm", "yishu:taiyi_realm"});

    private static final SuggestionProvider<CommandSourceStack> REALM_SUGGESTIONS =
            (ctx, builder) -> {
                List<String> options = new ArrayList<>();
                for (String[] r : REALMS) {
                    options.add(r[0]);
                }
                return SharedSuggestionProvider.suggest(options, builder);
            };

    /** Travel between the six realms (plain survival teleport, no portal required). */
    private static int travel(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        String arg = StringArgumentType.getString(ctx, "realm").trim().toLowerCase(Locale.ROOT);
        String[] realm = null;
        for (String[] r : REALMS) {
            if (r[0].equals(arg) || (arg.length() >= 2 && r[0].startsWith(arg))) {
                realm = r;
                break;
            }
        }
        if (realm == null) {
            YsText.chat(player, "没有叫「" + arg
                    + "」的界域。可选：overworld / nether / end / fate_realm / fengshui_realm / "
                    + "liuyao_realm / ziwei_realm / liuren_realm / qimen_realm / taiyi_realm。");
            return 0;
        }
        Identifier id = Identifier.parse(realm[2]);
        ServerLevel target = ctx.getSource().getServer().getLevel(
                ResourceKey.create(Registries.DIMENSION, id));
        if (target == null) {
            YsText.chat(player, "界域 §e" + realm[1]
                    + "§f 不可用 —— 维度数据没有加载？");
            return 0;
        }
        BlockPos surface = target.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING,
                player.blockPosition());
        double tx = surface.getX() + 0.5D;
        double ty = surface.getY() + 1.0D;
        double tz = surface.getZ() + 0.5D;
        player.teleportTo(target, tx, ty, tz, Set.<Relative>of(),
                player.getYRot(), player.getXRot(), false);
        YsText.chat(player, "§b已抵达 " + realm[1] + "§f。（"
                + (int) tx + ", " + (int) ty + ", " + (int) tz + "）");
        return 1;
    }

    // ==================================================================== admin

    private static int setTutorial(CommandContext<CommandSourceStack> ctx, boolean on) {
        YishuConfig.get().tutorialEnabled = on;
        YishuConfig.get().save();
        ctx.getSource().sendSuccess(() -> net.minecraft.network.chat.Component.literal(
                "§b[易枢]§f 教学已" + (on ? "开启" : "关闭")), true);
        return 1;
    }

    private static int setLimit(CommandContext<CommandSourceStack> ctx) {
        int count = IntegerArgumentType.getInteger(ctx, "count");
        YishuConfig.get().divinationDailyLimit = count;
        YishuConfig.get().save();
        ctx.getSource().sendSuccess(() -> net.minecraft.network.chat.Component.literal(
                "§b[易枢]§f 每日起卦上限已设为 §e" + count + "§f。"), true);
        return 1;
    }

    // ==================================================================== arguments

    private static final String QUESTIONS_LIST;

    static {
        StringBuilder sb = new StringBuilder();
        for (Divination.Question q : Divination.Question.all()) {
            if (sb.length() > 0) {
                sb.append(" / ");
            }
            sb.append(q.name().toLowerCase(Locale.ROOT));
        }
        QUESTIONS_LIST = sb.toString();
    }

    private static final SuggestionProvider<CommandSourceStack> QUESTION_SUGGESTIONS =
            (ctx, builder) -> {
                List<String> options = new ArrayList<>();
                for (Divination.Question q : Divination.Question.all()) {
                    options.add(q.name().toLowerCase(Locale.ROOT));
                }
                return SharedSuggestionProvider.suggest(options, builder);
            };

    private static Divination.Question matchQuestion(String input) {
        String normalized = input.trim();
        for (Divination.Question q : Divination.Question.all()) {
            if (q.name().equalsIgnoreCase(normalized)
                    || q.name().toLowerCase(Locale.ROOT).startsWith(normalized.toLowerCase(Locale.ROOT))) {
                return q;
            }
        }
        return null;
    }

    /** One of the minor divination schools; no argument prints the catalogue. */
    private static int oracleSchool(CommandContext<CommandSourceStack> ctx)
            throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        ServerLevel level = player.level();
        String key = StringArgumentType.getString(ctx, "school").trim().toLowerCase(Locale.ROOT);
        MinorSchools.School school = MinorSchools.School.of(key);
        if (school == null) {
            YsText.panel(player, "术数目录", MinorSchools.catalogue());
            return 0;
        }
        YishuCalendar.Moment moment = YishuCalendar.now(level);
        MinorSchools.Verdict verdict = MinorSchools.read(school, level.getSeed(),
                player.getGameProfile().name(), moment);
        List<String> lines = new ArrayList<>(verdict.lines());
        lines.add("");
        lines.add("§7" + moment.dateText() + " " + moment.hourText()
                + " · " + moment.solarTermText() + " · " + moment.mansionText());
        YsText.panel(player, verdict.title(), lines);
        return 1;
    }

    /** Almanac clock: show the full calendar + personal fortune. */
    private static int calendar(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        ServerLevel level = player.level();
        YishuCalendar.Moment moment = YishuCalendar.now(level);
        YsText.panel(player, "黄历 · " + moment.dateText(),
                YishuCalendar.clockPanel(level, player));
        return 1;
    }

    private static int calendarSetYear(CommandContext<CommandSourceStack> ctx)
            throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        ServerLevel level = player.level();
        int year = IntegerArgumentType.getInteger(ctx, "year");
        YishuCalendar.setEpochYear(level, year);
        ctx.getSource().sendSuccess(() -> net.minecraft.network.chat.Component.literal(
                "§b[易枢]§f 本界域起始年号已设为 §e" + year + "§f。"), true);
        return 1;
    }

    private static int calendarSetDay(CommandContext<CommandSourceStack> ctx)
            throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        ServerLevel level = player.level();
        long offset = IntegerArgumentType.getInteger(ctx, "offset");
        YishuCalendar.setDayOffset(level, offset);
        ctx.getSource().sendSuccess(() -> net.minecraft.network.chat.Component.literal(
                "§b[易枢]§f 本界域历法偏移已设为 §e" + offset + "§f 日。"), true);
        return 1;
    }

    private static String summaryOf(List<FengShuiSystems.Almanac.Item> items, boolean favorable) {
        StringBuilder sb = new StringBuilder();
        for (FengShuiSystems.Almanac.Item item : items) {
            if (item.favorable() == favorable) {
                if (sb.length() > 0) {
                    sb.append(", ");
                }
                sb.append(item.zh());
            }
        }
        return sb.length() == 0 ? "none" : sb.toString();
    }
}
