package com.chenchen.yishu.client;

import com.chenchen.yishu.YishuMod;
import com.chenchen.yishu.client.screen.BaziScreen;
import com.chenchen.yishu.client.screen.DivinationScreen;
import com.chenchen.yishu.client.hud.AmbienceBorder;
import com.chenchen.yishu.client.screen.TextPanelScreen;
import com.chenchen.yishu.fluid.YsFluids;
import com.chenchen.yishu.network.YsNetworking;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderingRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.world.level.material.Fluid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 易枢 · 客户端入口。
 *
 * <p>客户端只做四件事：接种子、开界面、发请求、按键。
 * 所有会改变世界的计算都在服务端 —— 这条界线决定了这个模组
 * 在多人服务器里会不会出现「两个人看到不同的卦」这种致命问题。
 *
 * <p>HUD 刻意<b>不做</b>：命理这东西看多了会腻，常驻在屏幕上反而变成了噪音。
 * 想看的时候按 B，不想看的时候它就不在 —— 这是刻意的克制。
 */
public class YishuClient implements ClientModInitializer {

    public static final Logger LOGGER = LoggerFactory.getLogger("Yishu/Client");

    /** 命盘界面快捷键：B。 */
    private static KeyMapping openFateKey;

    @Override
    public void onInitializeClient() {
        attempt("按键", YishuClient::registerKeybinds);
        attempt("网络接收", YishuClient::registerNetworking);
        attempt("环境边框", YishuClient::registerHud);
        attempt("界面桥", YishuClient::installBridge);
        attempt("灵兽渲染", YsEntityRenderers::register);
        attempt("液体渲染", YishuClient::registerFluidRenderers);
    }

    private static void attempt(String name, Runnable action) {
        try {
            action.run();
        } catch (Throwable throwable) {
            LOGGER.error("[Yishu] 客户端 {} 注册失败（其余功能继续可用）", name, throwable);
        }
    }

    // ==================================================================== 按键

    private static void registerKeybinds() {
        KeyMapping.Category category = KeyMapping.Category.register(YishuMod.id("main"));
        openFateKey = KeyMappingHelper.registerKeyMapping(
                new KeyMapping("key.yishu.fate", InputConstants.KEY_B, category));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            // 不需要判 screen：有界面打开时，原版根本不会把按键事件派发给 KeyMapping
            //（26.2 里 Minecraft 的 screen 字段也不再是 public）
            if (client.player == null) {
                return;
            }
            while (openFateKey.consumeClick()) {
                openBazi();
            }
        });
    }

    /** 打开命盘界面（键与物品共用同一个入口）。 */
    public static void openBazi() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }
        minecraft.setScreenAndShow(new BaziScreen());
    }

    // ==================================================================== 网络

    private static void registerNetworking() {
        ClientPlayNetworking.registerGlobalReceiver(YsNetworking.SeedPayload.TYPE, (payload, context) ->
                context.client().execute(() ->
                        ClientFateState.acceptSeed(payload.seed(), payload.divinationLimit(),
                                payload.professional(), payload.epochDay())));

        // 环境边框：服务端把所在地的吉凶颜色推过来，客户端只负责画
        ClientPlayNetworking.registerGlobalReceiver(YsNetworking.AmbiencePayload.TYPE, (payload, context) ->
                context.client().execute(() -> ClientFateState.setAmbienceColor(payload.argb())));

        ClientPlayNetworking.registerGlobalReceiver(YsNetworking.DisplayPayload.TYPE, (payload, context) ->
                context.client().execute(() ->
                        Minecraft.getInstance().setScreenAndShow(
                                new TextPanelScreen(payload.title(), payload.lines()))));

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> ClientFateState.reset());
    }

    // ==================================================================== HUD

    /**
     * 环境边框 —— 「第一层 · 直感」的主载体。
     *
     * <p>挂在 HOTBAR 之后，自动跟随 F1 隐藏，也自动跟随任何调整物品栏位置的
     * 资源包。见 {@link AmbienceBorder}。
     */
    private static void registerHud() {
        HudElementRegistry.attachElementAfter(VanillaHudElements.HOTBAR,
                YishuMod.id("ambience_border"), new AmbienceBorder());
    }

    // ==================================================================== 桥

    /**
     * 把「打开界面」这两个动作注册到通用端能调到的地方。
     *
     * <p>通用端的物品类需要开界面，但它在专用服务器上也会被加载，
     * 所以不能直接 import 客户端类 —— 只能通过
     * {@link YishuClientBridge} 这个空实现兜底的回调。
     */
    private static void installBridge() {
        YishuClientBridge.setBaziOpener(YishuClient::openBazi);
        YishuClientBridge.setDivinationOpener(allowOmen -> {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player != null) {
                minecraft.setScreenAndShow(new DivinationScreen(allowOmen));
            }
        });
    }

    /** 供界面回传：切换专业模式（服务端会校验是否已解锁）。 */
    public static void requestToggleProfessional() {
        ClientFateState.setProfessional(!ClientFateState.professional());
        ClientPlayNetworking.send(YsNetworking.SettingsPayload.INSTANCE);
    }

    /** 供界面回传：把一次起卦请求发给服务端。 */
    public static void requestCast(int questionOrdinal, int methodOrdinal, long omen) {
        ClientPlayNetworking.send(new YsNetworking.CastRequestPayload(questionOrdinal, methodOrdinal, omen));
    }

    // ==================================================================== 液体渲染

    /**
     * 六种维度液体的外观。
     *
     * <p>26.2 把流体渲染收进了 {@code FluidStateModelSet}，原版只认识水与岩浆；
     * 模组的液体要走 Fabric 的 {@code FluidRenderingRegistry}，把自己的
     * {@code FluidModel.Unbaked} 掺进烘焙批次。给的东西就两样：静止贴图与流动贴图。
     * 颜色直接画在贴图里，不需要 tint，也不需要 overlay。
     *
     * <p><b>每加一种液体，这里必须补一行。</b>漏注册的后果不是「贴图不对」，
     * 而是这摊液体在客户端整个不画 —— 看上去只是一片空空气。
     */
    private static void registerFluidRenderers() {
        fluid(YsFluids.ASTRAL_DEW, YsFluids.FLOWING_ASTRAL_DEW, "astral_dew");
        fluid(YsFluids.JADE_ICHOR, YsFluids.FLOWING_JADE_ICHOR, "jade_ichor");
        fluid(YsFluids.YAO_INK, YsFluids.FLOWING_YAO_INK, "yao_ink");
        // —— 第 16 轮加的三种，此前漏了这三行 ——
        fluid(YsFluids.ZIWEI_AMBROSIA, YsFluids.FLOWING_ZIWEI_AMBROSIA, "ziwei_ambrosia");
        fluid(YsFluids.LIUREN_WATER, YsFluids.FLOWING_LIUREN_WATER, "liuren_water");
        fluid(YsFluids.QIMEN_MERCURY, YsFluids.FLOWING_QIMEN_MERCURY, "qimen_mercury");
        // —— 第 19 轮：太乙玄泉 ——
        fluid(YsFluids.TAIYI_SPRING, YsFluids.FLOWING_TAIYI_SPRING, "taiyi_spring");

        // 第 21 轮：七界凶液
        fluid(YsFluids.ASTRAL_SLAG, YsFluids.FLOWING_ASTRAL_SLAG, "astral_slag");
        fluid(YsFluids.JADE_ACID, YsFluids.FLOWING_JADE_ACID, "jade_acid");
        fluid(YsFluids.YAO_MIASMA, YsFluids.FLOWING_YAO_MIASMA, "yao_miasma");
        fluid(YsFluids.ZIWEI_BANE, YsFluids.FLOWING_ZIWEI_BANE, "ziwei_bane");
        fluid(YsFluids.LIUREN_ABYSS, YsFluids.FLOWING_LIUREN_ABYSS, "liuren_abyss");
        fluid(YsFluids.QIMEN_HELLFIRE, YsFluids.FLOWING_QIMEN_HELLFIRE, "qimen_hellfire");
        fluid(YsFluids.TAIYI_FROST, YsFluids.FLOWING_TAIYI_FROST, "taiyi_frost");
    }

    private static void fluid(Fluid still, Fluid flowing, String path) {
        FluidRenderingRegistry.register(still, flowing, new FluidModel.Unbaked(
                new Material(YishuMod.id("block/" + path + "_still")),
                new Material(YishuMod.id("block/" + path + "_flow")),
                null,
                null));
    }
}
