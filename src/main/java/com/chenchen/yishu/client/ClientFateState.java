package com.chenchen.yishu.client;

import com.chenchen.yishu.fate.Bazi;
import com.chenchen.yishu.fate.FateProfile;
import com.chenchen.yishu.system.TutorialSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

/**
 * 易枢 · 客户端命理状态。
 *
 * <p>客户端只需要记住三样东西：<b>世界种子</b>、<b>专业模式</b>、<b>游戏日</b>。
 * 有了它们，命盘、神煞、大运、黄历、太岁方位、信息分层全都能在本地算出来，
 * 一次网络往返都不用 —— 因为整套推算本来就是确定性的，
 * 服务端和客户端算出来的结果必然一致。
 *
 * <p>这是一个「把确定性变成性能」的典型案例：不需要同步结果，
 * 只需要同步<b>推导结果所依据的那个种子</b>。
 */
public final class ClientFateState {

    private ClientFateState() {
    }

    /** 未连接时用一个占位种子，保证本地代码永远不会拿到 null。 */
    private static long worldSeed;
    private static boolean seedKnown;

    /** 服务端推过来的每日问卦上限。 */
    private static int divinationLimit = 3;

    /** 专业模式 —— 第三层术语信息的开关。 */
    private static boolean professional;

    /** 游戏日（用于本地判断渐进式解锁阶段）。 */
    private static long epochDay;

    /** 屏幕边框颜色（环境直感）。0 = 无。 */
    private static int ambienceColor;

    private static FateProfile cachedProfile;
    private static String cachedOwner;

    // ==================================================================== 种子

    public static void acceptSeed(long seed, int limit, boolean pro, long day) {
        worldSeed = seed;
        seedKnown = true;
        divinationLimit = Math.max(0, limit);
        professional = pro;
        epochDay = day;
        // 换了种子，命盘缓存立刻作废
        cachedProfile = null;
        cachedOwner = null;
        YishuClient.LOGGER.debug("[Yishu] 已接收世界种子 {}（专业模式：{}）", seed, pro);
    }

    public static long seed() {
        return worldSeed;
    }

    public static boolean seedKnown() {
        return seedKnown;
    }

    public static int divinationLimit() {
        return divinationLimit;
    }

    /** 专业模式是否开启（决定要不要显示第三层术语）。 */
    public static boolean professional() {
        return professional;
    }

    public static void setProfessional(boolean value) {
        professional = value;
    }

    /** 当前游戏日。 */
    public static long epochDay() {
        return epochDay;
    }

    /** 当前解锁阶段（与服务端 {@link TutorialSystem#stageForDay} 同一张表）。 */
    public static int stage() {
        return TutorialSystem.stageForDay(epochDay);
    }

    /** 某个内容是否已解锁（界面用它决定显示多少信息）。 */
    public static boolean unlocked(int requiredStage) {
        return TutorialSystem.unlocked(epochDay, requiredStage);
    }

    /** 屏幕边框颜色。 */
    public static int ambienceColor() {
        return ambienceColor;
    }

    public static void setAmbienceColor(int color) {
        ambienceColor = color;
    }

    /** 断线时清干净，避免单人存档之间互相串味。 */
    public static void reset() {
        seedKnown = false;
        worldSeed = 0L;
        professional = false;
        epochDay = 0L;
        ambienceColor = 0;
        cachedProfile = null;
        cachedOwner = null;
    }

    // ==================================================================== 命盘

    /**
     * 取自己的命盘。
     *
     * <p>纯本地计算 + 缓存。同一帧里反复调用不会重复算，
     * 玩家的名字变了（换账号）也会自动重算。
     */
    public static FateProfile profile() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return null;
        }
        String name = player.getGameProfile().name();
        if (cachedProfile == null || !name.equals(cachedOwner)) {
            cachedProfile = FateProfile.of(worldSeed, name);
            cachedOwner = name;
        }
        return cachedProfile;
    }

    /** 便捷：当前的生效八字。 */
    public static Bazi bazi() {
        FateProfile profile = profile();
        return profile == null ? null : profile.active();
    }

    /** 便捷：玩家名。 */
    public static String playerName() {
        LocalPlayer player = Minecraft.getInstance().player;
        return player == null ? "" : player.getGameProfile().name();
    }
}
