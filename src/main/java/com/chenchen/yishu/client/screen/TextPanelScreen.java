package com.chenchen.yishu.client.screen;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * 易枢 · 玄览面板 —— 模组唯一的通用文本界面。
 *
 * <p>卦象、户型图、命盘、遗迹报告、观气台输出……全都用它显示。
 * 这不是偷懒，是一个刻意的架构决定：
 *
 * <ul>
 *   <li>内容系统一定会不断加新东西，如果每加一个就要写一个 GUI，
 *       开发量会指数级膨胀；</li>
 *   <li>而这些东西有一个共同点 —— 它们都是<b>给人读的文字</b>；</li>
 *   <li>真正需要定制的只有两个界面（命盘与起卦），那两个做了专属界面。</li>
 * </ul>
 *
 * <p>支持滚动：鼠标滚轮、上下方向键、PageUp/PageDown 都能翻。
 * 玩家读长报告（比如一整张户型图加批断）时这一点很重要。
 */
public class TextPanelScreen extends Screen {

    private static final int PADDING = 14;
    private static final int LINE_HEIGHT = 11;
    private static final int TITLE_HEIGHT = 26;
    private static final int MAX_WIDTH = 460;
    private static final int MAX_HEIGHT = 240;

    private final List<String> lines;
    private int scroll;

    public TextPanelScreen(String title, List<String> lines) {
        super(Component.literal(title));
        this.lines = lines;
    }

    @Override
    protected void init() {
        super.init();
        addRenderableWidget(Button.builder(Component.literal("关闭"), button -> onClose())
                .bounds(this.width / 2 - 30, panelY() + panelHeight() - 24, 60, 18)
                .build());
    }

    private int panelWidth() {
        int content = Math.min(MAX_WIDTH, this.width - 40);
        return Math.max(200, content);
    }

    private int panelHeight() {
        return Math.min(MAX_HEIGHT, this.height - 40);
    }

    private int panelX() {
        return (this.width - panelWidth()) / 2;
    }

    private int panelY() {
        return (this.height - panelHeight()) / 2;
    }

    private int visibleLines() {
        return Math.max(1, (panelHeight() - TITLE_HEIGHT - 30) / LINE_HEIGHT);
    }

    private int maxScroll() {
        return Math.max(0, this.lines.size() - visibleLines());
    }

    // ==================================================================== 绘制

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float partialTick) {
        drawBackdrop(context);
        // 必须调 super 才会画出子控件（关闭按钮）
        super.extractRenderState(context, mouseX, mouseY, partialTick);
        drawContent(context);
    }

    /** 这里的遮罩是手动画的 —— 26.x 起 {@code renderBackground} 已被移除。 */
    private void drawBackdrop(GuiGraphicsExtractor context) {
        context.fill(0, 0, this.width, this.height, 0xB00A0E14);
        int x = panelX();
        int y = panelY();
        int w = panelWidth();
        int h = panelHeight();
        // 边框与底板：深色玉石质感
        context.fill(x - 1, y - 1, x + w + 1, y + h + 1, 0xFF3C5A6E);
        context.fill(x, y, x + w, y + h, 0xF00F151C);
        context.fill(x, y, x + w, y + TITLE_HEIGHT, 0xFF16212B);
        context.fill(x, y + TITLE_HEIGHT - 1, x + w, y + TITLE_HEIGHT, 0xFF3C5A6E);
    }

    private void drawContent(GuiGraphicsExtractor context) {
        int x = panelX();
        int y = panelY();
        int w = panelWidth();

        context.text(this.font, this.title.getString(), x + PADDING, y + 9, 0xFF9FD8E8, true);

        // 滚动指示
        String hint = (this.scroll + 1) + "–"
                + Math.min(this.lines.size(), this.scroll + visibleLines())
                + " / " + this.lines.size()
                + "（滚轮或 ↑↓ 翻页）";
        int hintWidth = this.font.width(hint);
        context.text(this.font, hint, x + w - PADDING - hintWidth, y + 9, 0xFF6E8794, false);

        int lineY = y + TITLE_HEIGHT + 4;
        int end = Math.min(this.lines.size(), this.scroll + visibleLines());
        for (int i = this.scroll; i < end; i++) {
            String line = this.lines.get(i);
            context.text(this.font, line, x + PADDING, lineY, colorFor(line), false);
            lineY += LINE_HEIGHT;
        }

        // 底部滚动条
        if (maxScroll() > 0) {
            int trackX = x + w - 6;
            int trackTop = y + TITLE_HEIGHT + 2;
            int trackHeight = panelHeight() - TITLE_HEIGHT - 30;
            context.fill(trackX, trackTop, trackX + 3, trackTop + trackHeight, 0xFF1E2A34);
            int thumbHeight = Math.max(12, trackHeight * visibleLines() / Math.max(1, this.lines.size()));
            int thumbTop = trackTop + (trackHeight - thumbHeight) * this.scroll / Math.max(1, maxScroll());
            context.fill(trackX, thumbTop, trackX + 3, thumbTop + thumbHeight, 0xFF5BA8C4);
        }
    }

    /**
     * 逐行上色。
     *
     * <p>服务端发过来的文本里带有 § 颜色代码，这里做一层粗略识别，
     * 让界面能分出主次 —— 凶位红、吉位绿、说明灰。
     */
    private static int colorFor(String line) {
        if (line == null || line.isEmpty()) {
            return 0xFFBFC9D0;
        }
        if (line.startsWith("──")) {
            return 0xFF7FB6C8;
        }
        if (line.startsWith("§c")) {
            return 0xFFFF8A8A;
        }
        if (line.startsWith("§e") || line.startsWith("§6")) {
            return 0xFFE8C878;
        }
        if (line.startsWith("§a")) {
            return 0xFF8AE09A;
        }
        if (line.startsWith("§b")) {
            return 0xFF8AD4E0;
        }
        if (line.startsWith("§7")) {
            return 0xFF98A8B2;
        }
        if (line.startsWith("★")) {
            return 0xFFE8D27A;
        }
        if (line.startsWith("[大凶]") || line.startsWith("[凶]")) {
            return 0xFFFF8A8A;
        }
        if (line.startsWith("[小凶]")) {
            return 0xFFE0B878;
        }
        if (line.startsWith("  ▬") || line.startsWith("  ▭")) {
            return 0xFFA8C4D4;
        }
        return 0xFFDCE4E8;
    }

    // ==================================================================== 交互

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        this.scroll = clamp(this.scroll - (int) Math.signum(scrollY), 0, maxScroll());
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        switch (event.key()) {
            case InputConstants.KEY_DOWN -> {
                this.scroll = clamp(this.scroll + 1, 0, maxScroll());
                return true;
            }
            case InputConstants.KEY_UP -> {
                this.scroll = clamp(this.scroll - 1, 0, maxScroll());
                return true;
            }
            case InputConstants.KEY_PAGEDOWN -> {
                this.scroll = clamp(this.scroll + visibleLines(), 0, maxScroll());
                return true;
            }
            case InputConstants.KEY_PAGEUP -> {
                this.scroll = clamp(this.scroll - visibleLines(), 0, maxScroll());
                return true;
            }
            default -> {
            }
        }
        if (event.isEscape()) {
            onClose();
            return true;
        }
        return super.keyPressed(event);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    @Override
    public void onClose() {
        Minecraft minecraft = this.minecraft;
        if (minecraft != null) {
            minecraft.setScreenAndShow(null);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
