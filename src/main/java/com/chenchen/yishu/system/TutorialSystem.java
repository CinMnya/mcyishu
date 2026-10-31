package com.chenchen.yishu.system;

import com.chenchen.yishu.YishuConfig;
import com.chenchen.yishu.YishuMod;
import com.chenchen.yishu.dimension.DimensionProfile;
import com.chenchen.yishu.fate.FateProfile;
import com.chenchen.yishu.fengshui.FengShuiSystems;
import com.chenchen.yishu.network.YsNetworking;
import com.chenchen.yishu.registry.YsRegistry;
import com.chenchen.yishu.util.YsText;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * 易枢 · 新手引导 —— 不弹长篇教程，用任务链一步步教。
 *
 * <p><b>不解释八字是什么</b>。第一天只告诉玩家"你是土属性，适合住在有火的地方"，
 * 然后说一句"按 B 随时查看"，就结束了。
 *
 * <p>节奏（按游戏日，不是现实时间，离线不计时）：
 * <table border="1">
 *   <tr><th>阶段</th><th>游戏日</th><th>发生什么</th></tr>
 *   <tr><td>0</td><td>进入世界</td><td>自动获得命盘玉简，一句话提示</td></tr>
 *   <tr><td>1</td><td>第 2 天</td><td>获得风水罗盘</td></tr>
 *   <tr><td>2</td><td>第 4 天</td><td>获得三枚铜钱</td></tr>
 *   <tr><td>3</td><td>第 9 天</td><td>获得观气台与寻龙尺</td></tr>
 *   <tr><td>4</td><td>第 14 天</td><td>命盘显示完整四柱</td></tr>
 *   <tr><td>5</td><td>第 19 天</td><td>卦象显示六亲六神</td></tr>
 *   <tr><td>6</td><td>第 29 天</td><td>解锁专业模式</td></tr>
 * </table>
 *
 * <p><b>渐进式解锁</b>是同一张表反过来用的：还没到的阶段，对应物品用了会提示
 * "还没到时候"，对应信息不会显示。<b>每解锁一层，弹一次简短提示</b>。
 *
 * <p>所有"给过了吗"的记录都写进 {@link YishuData} —— 这属于规范里允许保存的
 * 「玩家修改」，不是可以从种子重算的基础数据。
 */
public final class TutorialSystem {

    /** 每个阶段开始的游戏日。 */
    public static final int[] STAGE_DAYS = {0, 2, 4, 9, 14, 19, 29};
    public static final int MAX_STAGE = STAGE_DAYS.length - 1;

    /** 新手保护期：前 3 个游戏日，风水减益减半。 */
    public static final int GRACE_DAYS = 3;

    /** 物品 → 解锁所需阶段。未登记的物品不设门槛。 */
    private static final Map<Item, Integer> GATES = new IdentityHashMap<>();

    private TutorialSystem() {
    }

    // ==================================================================== 阶段

    /**
     * 由游戏日算出当前阶段。
     *
     * <p>这是<b>纯函数</b>：只依赖游戏日数，客户端和服务端各自算一遍结果必然一致，
     * 所以信息层的"可见性"不需要任何网络同步。
     */
    public static int stageForDay(long epochDay) {
        int stage = 0;
        for (int i = 0; i < STAGE_DAYS.length; i++) {
            if (epochDay >= STAGE_DAYS[i]) {
                stage = i;
            }
        }
        return Math.min(stage, MAX_STAGE);
    }

    /** 某个内容是否已解锁。 */
    public static boolean unlocked(long epochDay, int requiredStage) {
        if (!YishuConfig.get().tutorialEnabled) {
            return true;        // 关掉引导 = 全部开放，给老玩家和服务器管理员留后门
        }
        return stageForDay(epochDay) >= requiredStage;
    }

    /** 还没解锁时的提示。 */
    public static String lockHint(int requiredStage) {
        long day = STAGE_DAYS[requiredStage];
        return "还没到时候 —— 这东西要到第 " + day + " 天才用得上，先用好手里的。";
    }

    /** 当前阶段的人话描述（命盘界面底部那行进度提示）。 */
    public static String stageHint(int stage) {
        return switch (stage) {
            case 0 -> "先学会看自己：按 B 打开命盘。";
            case 1 -> "用手里的罗盘找一块好地安家。";
            case 2 -> "可以用铜钱问卦了，先问问「哪里能挖矿」。";
            case 3 -> "观气台能看户型，寻龙尺能找风水宝地。";
            case 4 -> "你的完整四柱已经解锁，可以细看命盘了。";
            case 5 -> "卦象现在会显示六亲六神，解卦更准了。";
            default -> "专业模式已解锁 —— 想看术语，在命盘界面里打开它。";
        };
    }

    // ==================================================================== 注册

    public static void register() {
        // ---- 物品门槛 ----
        GATES.put(YsRegistry.FATE_SLIP, 0);
        GATES.put(YsRegistry.FENG_SHUI_COMPASS, 1);
        GATES.put(YsRegistry.BRONZE_COINS, 2);
        GATES.put(YsRegistry.YARROW_STALKS, 2);
        GATES.put(YsRegistry.TURTLE_SHELL, 2);
        GATES.put(YsRegistry.SUNDIAL, 2);
        GATES.put(YsRegistry.DRAGON_RULER, 3);
        GATES.put(YsRegistry.QI_OBSERVATORY.asItem(), 3);
        GATES.put(YsRegistry.PEEK_CHARM, 4);
        GATES.put(YsRegistry.GUA_BOARD.asItem(), 5);

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer player = handler.getPlayer();
            if (player.level() instanceof ServerLevel level) {
                // 进来先发一次阶段 0（重复调用是幂等的：给过就不再给）
                grantStage(player, level, 0);
            }
        });

        ServerTickEvents.END_LEVEL_TICK.register(TutorialSystem::tickLevel);
    }

    private static void tickLevel(ServerLevel level) {
        if (!YishuConfig.get().tutorialEnabled) {
            return;
        }
        // 只在主世界跑引导（跨维度时阶段照常生效，但不在下界发玉简）
        if (!level.dimension().equals(net.minecraft.world.level.Level.OVERWORLD)) {
            return;
        }
        long epochDay = level.getOverworldClockTime() / 24000L;
        int stage = stageForDay(epochDay);

        // 每半秒检查一次足够了
        if (level.getLevelData().getGameTime() % 10L != 0L) {
            return;
        }
        for (ServerPlayer player : level.players()) {
            grantStage(player, level, stage);
        }
    }

    /** 把玩家推进到指定阶段；已经到过的什么都不做。 */
    private static void grantStage(ServerPlayer player, ServerLevel level, int targetStage) {
        YishuData data = YishuData.of(level);
        String name = player.getGameProfile().name();
        long epochDay = level.getOverworldClockTime() / 24000L;

        for (int stage = 0; stage <= targetStage; stage++) {
            String key = "gift:" + stage + ":" + name;
            if (data.get(key) != null) {
                continue;
            }
            data.put(key, "1");
            giveStageReward(player, level, data, stage, epochDay);
        }
    }

    /** 每个阶段的实际内容：给东西 + 一句话提示。 */
    private static void giveStageReward(ServerPlayer player, ServerLevel level, YishuData data,
                                        int stage, long epochDay) {
        DimensionProfile dimension = DimensionProfile.OVERWORLD;
        switch (stage) {
            case 0 -> {
                give(player, new ItemStack(YsRegistry.FATE_SLIP));
                // 第一天只说一句人话，不解释八字
                FateProfile profile = FateProfile.of(level.getSeed(), player.getGameProfile().name());
                YsText.send(player, "欢迎来到易枢", List.of(
                        PlainSpeak.fatePlain(profile.active()),
                        "",
                        "按 B 随时查看自己的命盘。"
                ));
            }
            case 1 -> {
                give(player, new ItemStack(YsRegistry.FENG_SHUI_COMPASS));
                YsText.send(player, "获得 · 风水罗盘", List.of(
                        "右键看看脚下这块地怎么样。",
                        "指针变绿就是好地方，变红就换一个方向走。"
                ));
            }
            case 2 -> {
                give(player, new ItemStack(YsRegistry.BRONZE_COINS));
                YsText.send(player, "获得 · 三枚铜钱", List.of(
                        "右键掷一卦，问一件事。",
                        "比如问「哪里能挖矿」，卦会告诉你往哪走。"
                ));
            }
            case 3 -> {
                give(player, new ItemStack(YsRegistry.QI_OBSERVATORY));
                give(player, new ItemStack(YsRegistry.DRAGON_RULER));
                YsText.send(player, "获得 · 观气台与寻龙尺", List.of(
                        "观气台放进家里右键，能看户型有没有问题。",
                        "寻龙尺能指出风水宝地的方向。"
                ));
            }
            case 4 -> YsText.send(player, "命盘已完整", List.of(
                    "现在按 B 能看到完整的四柱八字了。",
                    PlainSpeak.MOTTO
            ));
            case 5 -> YsText.send(player, "卦象已完整", List.of(
                    "卦象现在会显示六亲六神，解卦更细致了。",
                    PlainSpeak.MOTTO
            ));
            case 6 -> {
                YsText.send(player, "专业模式已解锁", List.of(
                        "在命盘界面按「专业模式」可以看全部术语。",
                        PlainSpeak.MOTTO
                ));
                YsNetworking.sendSeed(player, level.getSeed(), professional(player, data), epochDay);
            }
            default -> {
            }
        }
        // 每个阶段都附一句当前阶段的"下一步"提示
        YsText.chat(player, "接下来：" + stageHint(stage));
    }

    private static void give(ServerPlayer player, ItemStack stack) {
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }

    // ==================================================================== 物品门槛

    /**
     * 检查一件物品是否已解锁。
     *
     * @return true 表示可以用；false 表示还没到时候（调用方直接返回，不再往下走）
     */
    public static boolean checkGate(net.minecraft.world.entity.player.Player player, Item item) {
        if (!YishuConfig.get().tutorialEnabled) {
            return true;
        }
        Integer required = GATES.get(item);
        if (required == null) {
            return true;
        }
        long epochDay = player.level().getOverworldClockTime() / 24000L;
        if (unlocked(epochDay, required)) {
            return true;
        }
        YsText.chat(player, lockHint(required));
        return false;
    }

    // ==================================================================== 专业模式

    private static String proKey(String name) {
        return "pro:" + name;
    }

    /** 该玩家是否开了专业模式。 */
    public static boolean professional(ServerPlayer player) {
        YishuData data = YishuData.shared(player.level().getServer());
        return data.get(proKey(player.getGameProfile().name())) != null;
    }

    public static boolean professional(ServerPlayer player, YishuData data) {
        return data.get(proKey(player.getGameProfile().name())) != null;
    }

    /** 切换专业模式；需要阶段 6。 */
    public static boolean toggleProfessional(ServerPlayer player) {
        YishuData data = YishuData.shared(player.level().getServer());
        String name = player.getGameProfile().name();
        long epochDay = player.level().getOverworldClockTime() / 24000L;

        if (!unlocked(epochDay, 6)) {
            YsText.chat(player, lockHint(6));
            return professional(player, data);
        }
        boolean now = data.get(proKey(name)) == null;
        if (now) {
            data.put(proKey(name), "1");
            YsText.chat(player, "专业模式已开 —— 术语都回来了。");
        } else {
            data.put(proKey(name), "0");
            YsText.chat(player, "专业模式已关 —— 回到白话模式。");
        }
        data.setDirty();
        YsNetworking.sendSeed(player, player.level().getSeed(), now, epochDay);
        return now;
    }

    /** 读取专业模式（不写存档）。 */
    public static boolean isProfessional(ServerPlayer player) {
        return professional(player);
    }

    // ==================================================================== 新手保护

    /** 是否还在新手保护期（前 3 个游戏日，风水减益减半）。 */
    public static boolean inGracePeriod(long epochDay) {
        return YishuConfig.get().tutorialEnabled && epochDay < GRACE_DAYS;
    }

    /** 引导总开关。 */
    public static boolean enabled() {
        return YishuConfig.get().tutorialEnabled;
    }

    /** 供日志。 */
    public static String describe() {
        return "[" + YishuMod.MOD_ID + "] 引导链 " + STAGE_DAYS.length + " 阶段已就绪";
    }
}
