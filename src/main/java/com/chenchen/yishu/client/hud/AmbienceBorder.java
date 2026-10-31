package com.chenchen.yishu.client.hud;

import com.chenchen.yishu.client.ClientFateState;
import com.chenchen.yishu.client.YishuClient;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * 易枢 · 环境边框 —— 「第一层 · 直感」的主载体。
 *
 * <p>玩家站在什么风水里，屏幕边缘就泛什么颜色的光：
 * <pre>
 *   金 = 风水宝地   绿 = 不错   （灰 = 一般，不显示）
 *   橙 = 不太好     红 = 危险
 * </pre>
 *
 * <p><b>为什么这是最重要的视觉</b>：它不需要玩家做任何动作、读任何文字。
 * 走着走着屏幕边缘开始发红光，玩家立刻知道"这地方不对劲"——
 * 这比"煞气+42"这种数字有效一百倍，也比让玩家每次都掏出罗盘省事。
 *
 * <p>实现细节：
 * <ul>
 *   <li>颜色由服务端每 3 秒推一次（{@code AmbiencePayload}），
 *       客户端只做插值淡入淡出，不做任何计算；</li>
 *   <li>边框用四条渐变条而不是一整圈描边，视觉上是"光晕"而不是"线框"；</li>
 *   <li>挂在 HOTBAR 之后，自动跟随 F1 隐藏；</li>
 *   <li>透明度带一个缓慢的正弦呼吸，避免长期停留时被眼睛"过滤"掉。</li>
 * </ul>
 */
public class AmbienceBorder implements HudElement {

    private static final int THICKNESS = 14;
    private static final int CORNER = 90;

    /** 当前显示的颜色（含淡入淡出后的 alpha）。 */
    private int currentColor;
    /** 上一帧的颜色，用于淡出。 */
    private int previousColor;

    public AmbienceBorder() {
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, DeltaTracker deltaTracker) {
        int target = ClientFateState.ambienceColor();
        if (target == 0 && this.currentColor == 0) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.options == null) {
            return;
        }

        // ---- 淡入淡出：目标色变化时，从旧色滑向新色 ----
        if (target != this.currentColor) {
            this.previousColor = this.currentColor;
            this.currentColor = target;
        }
        int color = this.currentColor;

        float time = (minecraft.level == null ? 0L : minecraft.level.getGameTime())
                + deltaTracker.getGameTimeDeltaPartialTick(false);
        // 呼吸：0.55 ~ 1.0
        float breathe = 0.55F + 0.45F * (float) Math.sin(time * 0.06D);
        int alpha = (int) (150 * breathe);

        int width = context.guiWidth();
        int height = context.guiHeight();

        // 上、下、左、右四条渐变光带
        gradientBar(context, 0, 0, width, THICKNESS, color, alpha, true, false);
        gradientBar(context, 0, height - THICKNESS, width, height, color, alpha, true, true);
        gradientBar(context, 0, 0, THICKNESS, height, color, alpha, false, false);
        gradientBar(context, width - THICKNESS, 0, width, height, color, alpha, false, true);

        // 四角加重，让"边框"感更明确
        corner(context, 0, 0, color, alpha);
        corner(context, width - CORNER, 0, color, alpha);
        corner(context, 0, height - CORNER, color, alpha);
        corner(context, width - CORNER, height - CORNER, color, alpha);
    }

    /** 一条向内渐隐的光带。 */
    private static void gradientBar(GuiGraphicsExtractor context, int x0, int y0, int x1, int y1,
                                    int color, int alpha, boolean horizontal, boolean fromEnd) {
        int steps = horizontal ? Math.max(1, x1 - x0) : Math.max(1, y1 - y0);
        if (steps > THICKNESS * 4) {
            steps = THICKNESS;      // 别太密，够渐变就行
        }
        for (int i = 0; i < steps; i++) {
            float ratio = 1.0F - (float) i / steps;
            int layerAlpha = (int) (alpha * ratio * ratio);   // 平方衰减更柔和
            int argb = (layerAlpha << 24) | (color & 0x00FFFFFF);
            if (horizontal) {
                int y = fromEnd ? y1 - 1 - i : y0 + i;
                context.fill(x0, y, x1, y + 1, argb);
            } else {
                int x = fromEnd ? x1 - 1 - i : x0 + i;
                context.fill(x, y0, x + 1, y1, argb);
            }
        }
    }

    /** 角部的一小块实色，把光带"焊"在一起。 */
    private static void corner(GuiGraphicsExtractor context, int x, int y, int color, int alpha) {
        int argb = ((alpha * 3 / 4) << 24) | (color & 0x00FFFFFF);
        context.fill(x, y, x + CORNER, y + 3, argb);
        context.fill(x, y, x + 3, y + CORNER, argb);
    }
}
