package com.chenchen.yishu.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

/**
 * 易枢 · 客户端桥。
 *
 * <p>通用端（服务端也会加载的那些类）需要「打开某个界面」时，不能直接 import
 * 客户端类 —— 专用服务器上没有 {@code Minecraft} 这个类，一 import 就会在
 * 启动阶段炸掉。所以这里放一个空实现，由客户端入口在初始化时把它换掉。
 *
 * <p>这是 Fabric 模组里最经典的一处坑，值得写清楚：
 * <b>能不能 import 一个类，取决于「服务端会不会加载到你」</b>，
 * 而不是「你写没写 if (client)」。
 */
public final class YishuClientBridge {

    private YishuClientBridge() {
    }

    /** 打开命盘界面的动作；服务端环境下是空操作。 */
    private static Runnable baziOpener = () -> {
    };

    /** 打开起卦界面的动作；参数表示是否允许外应起卦。 */
    private static java.util.function.Consumer<Boolean> divinationOpener = allowOmen -> {
    };

    public static void setBaziOpener(Runnable opener) {
        baziOpener = opener == null ? () -> {
        } : opener;
    }

    public static void setDivinationOpener(java.util.function.Consumer<Boolean> opener) {
        divinationOpener = opener == null ? allowOmen -> {
        } : opener;
    }

    /** 打开命盘界面。 */
    public static void openBazi(Player player) {
        try {
            baziOpener.run();
        } catch (Throwable throwable) {
            com.chenchen.yishu.YishuMod.LOGGER.warn("[Yishu] 命盘界面打开失败：{}", throwable.toString());
        }
    }

    /** 打开起卦界面。 */
    public static void openDivination(Player player, boolean allowOmen) {
        try {
            divinationOpener.accept(allowOmen);
        } catch (Throwable throwable) {
            com.chenchen.yishu.YishuMod.LOGGER.warn("[Yishu] 起卦界面打开失败：{}", throwable.toString());
        }
    }

    /** 客户端是否就绪（供通用端做无害的兜底判断）。 */
    public static boolean clientReady() {
        return Minecraft.getInstance() != null;
    }
}
