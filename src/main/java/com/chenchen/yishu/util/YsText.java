package com.chenchen.yishu.util;

import com.chenchen.yishu.YishuMod;
import com.chenchen.yishu.network.YsNetworking.DisplayPayload;
import com.chenchen.yishu.network.YsNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * 易枢 · 文本通道 —— 模组所有「输出」的唯一出口。
 *
 * <p>玩家要看到的东西有两类：聊天栏里的一句话，和一块能细看的牌子。
 * 这里把两件事一次做完：
 * <ol>
 *   <li>往聊天栏发一行带颜色的标题（聊天栏是「即时反馈」）；</li>
 *   <li>把完整内容包成 {@link DisplayPayload} 发给客户端，弹出 {@code 玄览} 面板
 *       （面板是「可以慢慢读的部分」）。</li>
 * </ol>
 *
 * <p>为什么不把所有东西都塞进聊天栏：卦象、户型图、命盘这些东西有<b>结构</b>，
 * 一坨文字刷过去根本读不了。而全做成自定义 GUI 又会把开发量炸掉。
 * 「一行提示 + 一块可滚动面板」是这两者之间性价比最高的折中 ——
 * 而且这块面板是通用的，命盘、卦象、户型图、遗迹报告全都用它，
 * 以后加新系统不需要再写界面。
 */
public final class YsText {

    private YsText() {
    }

    /** 面板标题的前缀色。 */
    private static final String ACCENT = "§b";
    private static final String DIM = "§7";

    /**
     * 发一条「标题 + 面板」。
     *
     * @param player 接收者
     * @param title  面板标题（也会出现在聊天栏）
     * @param lines  面板正文
     */
    public static void send(Player player, String title, List<String> lines) {
        // 26.2 里 useWithoutItem / useOn 给的是 Player，收窄成 ServerPlayer 才能发系统消息
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        player = serverPlayer;
        serverPlayer.sendSystemMessage(Component.literal(ACCENT + "【易枢】§f" + title));
        for (String line : lines) {
            // 聊天栏只放前几行，避免刷屏；完整内容在面板里
            if (lines.indexOf(line) < 3) {
                serverPlayer.sendSystemMessage(Component.literal(DIM + "  " + line));
            }
        }
        if (lines.size() > 3) {
            serverPlayer.sendSystemMessage(Component.literal(DIM + "  …（完整内容见玄览面板）"));
        }
        YsNetworking.sendDisplay(player, new DisplayPayload(title, List.copyOf(lines)));
    }

    /** 只发聊天，不弹面板。 */
    public static void chat(Player player, String message) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        serverPlayer.sendSystemMessage(Component.literal(ACCENT + "【易枢】§f" + message));
    }

    /** 只弹面板，不在聊天栏留字。 */
    public static void panel(Player player, String title, List<String> lines) {
        YsNetworking.sendDisplay(player, new DisplayPayload(title, List.copyOf(lines)));
    }

    /** 拼接多段正文。 */
    public static List<String> join(List<String>... parts) {
        List<String> result = new ArrayList<>();
        for (List<String> part : parts) {
            result.addAll(part);
        }
        return result;
    }

    /** 画一条分隔线。 */
    public static String rule(String label) {
        if (label == null || label.isEmpty()) {
            return "────────────────";
        }
        return "── " + label + " ──";
    }

    /** 把一串键值对渲染成对齐的两列。 */
    public static List<String> table(List<String> keys, List<String> values) {
        int width = 0;
        for (String key : keys) {
            width = Math.max(width, displayWidth(key));
        }
        List<String> lines = new ArrayList<>();
        for (int i = 0; i < keys.size(); i++) {
            String key = keys.get(i);
            String value = i < values.size() ? values.get(i) : "";
            lines.add(key + " ".repeat(Math.max(1, width - displayWidth(key) + 2)) + value);
        }
        return lines;
    }

    /** 粗略计算显示宽度（中文按 2 计）。 */
    private static int displayWidth(String text) {
        int width = 0;
        for (int i = 0; i < text.length(); i++) {
            width += text.charAt(i) > 0x2E80 ? 2 : 1;
        }
        return width;
    }

    /** 模组命名空间。 */
    public static String modId() {
        return YishuMod.MOD_ID;
    }
}
