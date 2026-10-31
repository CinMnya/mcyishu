package com.chenchen.yishu.client.screen;

import com.chenchen.yishu.client.ClientFateState;
import com.chenchen.yishu.client.YishuClient;
import com.chenchen.yishu.core.StableKey;
import com.chenchen.yishu.dimension.DimensionProfile;
import com.chenchen.yishu.fate.Bazi;
import com.chenchen.yishu.fate.FateProfile;
import com.chenchen.yishu.fate.GanZhi;
import com.chenchen.yishu.fate.ShenSha;
import com.chenchen.yishu.system.PlainSpeak;
import com.chenchen.yishu.fate.Yun;
import com.chenchen.yishu.fengshui.FengShuiSystems;
import com.chenchen.yishu.content.YsItemContent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 易枢 · 命盘界面。
 *
 * <p>按 B 键或使用命盘玉简打开。整个界面<b>完全在客户端本地计算</b> ——
 * 只用了服务端推过来的世界种子，一次网络往返都没有。
 *
 * <p>版面按「一眼看懂一个人」的顺序排：
 * <ol>
 *   <li>顶上一行：日主 + 强弱 + 喜用神 —— 这是命盘里最重要的三个结论；</li>
 *   <li>中间四柱：年月日时，每柱带十神与藏干；</li>
 *   <li>左侧五行柱状图：一眼看出这个人的五行偏在哪里；</li>
 *   <li>右下神煞：命中带的标记；</li>
 *   <li>底部大运与流运：现在走在哪一步。</li>
 * </ol>
 *
 * <p><b>为什么不做「改命」按钮</b>：改命这件事在设定上是要动祭坛、献祭品的，
 * 在菜单里点一下就能改会彻底破坏仪式感。界面上只读，改命去轮回台。
 */
public class BaziScreen extends Screen {

    private static final int PADDING = 16;
    private static final int PANEL_WIDTH = 460;
    private static final int PANEL_HEIGHT = 250;

    private final FateProfile profile;
    private final Bazi bazi;

    public BaziScreen() {
        super(Component.literal("命盘 · 四柱推命"));
        FateProfile current = ClientFateState.profile();
        this.profile = current;
        this.bazi = current == null ? null : current.active();
    }

    @Override
    protected void init() {
        super.init();
        int x = panelX();
        int y = panelY();
        addRenderableWidget(Button.builder(
                        Component.literal(ClientFateState.professional() ? "专业模式:开" : "专业模式:关"),
                        button -> YishuClient.requestToggleProfessional())
                .bounds(x + PADDING, y + PANEL_HEIGHT - 26, 96, 18)
                .build());
        addRenderableWidget(Button.builder(Component.literal("导出全文"),
                        button -> openFullText())
                .bounds(x + PANEL_WIDTH - 152, y + PANEL_HEIGHT - 26, 68, 18)
                .build());
        addRenderableWidget(Button.builder(Component.literal("关闭"), button -> onClose())
                .bounds(x + PANEL_WIDTH - 78, y + PANEL_HEIGHT - 26, 62, 18)
                .build());
    }

    private int panelX() {
        return (this.width - PANEL_WIDTH) / 2;
    }

    private int panelY() {
        return (this.height - PANEL_HEIGHT) / 2;
    }

    private void openFullText() {
        if (this.profile == null) {
            return;
        }
        if (this.minecraft != null) {
            this.minecraft.setScreenAndShow(
                    new TextPanelScreen("命盘全文 · " + this.profile.owner(),
                            YsItemContent.fateText(this.profile)));
        }
    }

    // ==================================================================== 绘制

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float partialTick) {
        drawBackdrop(context);
        super.extractRenderState(context, mouseX, mouseY, partialTick);
        if (this.bazi == null) {
            context.text(this.font, "尚未与世界建立联系（等待种子同步）",
                    panelX() + PADDING, panelY() + 40, 0xFFC88A8A, false);
            return;
        }
        drawHeader(context);
        drawPillars(context);
        drawElements(context);
        drawShenSha(context);
        drawYun(context);
    }

    private void drawBackdrop(GuiGraphicsExtractor context) {
        context.fill(0, 0, this.width, this.height, 0xC0080A10);
        int x = panelX();
        int y = panelY();
        context.fill(x - 1, y - 1, x + PANEL_WIDTH + 1, y + PANEL_HEIGHT + 1, 0xFF4A4030);
        context.fill(x, y, x + PANEL_WIDTH, y + PANEL_HEIGHT, 0xF4141118);
        context.fill(x, y, x + PANEL_WIDTH, y + 24, 0xFF1E1926);
        context.fill(x, y + 23, x + PANEL_WIDTH, y + 24, 0xFF6B5A3C);
    }

    private void drawHeader(GuiGraphicsExtractor context) {
        int x = panelX();
        int y = panelY();
        context.text(this.font, this.title.getString(), x + PADDING, y + 8, 0xFFD8C08A, true);

        // ---- 第二层 · 白话：先说人话 ----
        context.text(this.font, PlainSpeak.fatePlain(this.bazi),
                x + PADDING, y + 30, 0xFFE8DCC0, false);
        context.text(this.font, "住在对你有利属性的地方，你会更强。",
                x + PADDING, y + 42, 0xFF9FB8A0, false);

        // ---- 第三层 · 术语（只给专业模式）----
        if (ClientFateState.professional()) {
            Bazi.YongShen yong = this.bazi.yongShen();
            String line = this.profile.owner() + "　日主 " + this.bazi.dayMaster().zh()
                    + this.bazi.dayMasterElement().zh()
                    + "　" + this.bazi.strength().zh()
                    + "　喜用 " + yong.primary().zh();
            context.text(this.font, line, x + PADDING, y + 54, 0xFFC8BFA0, false);
            context.text(this.font, yong.reason(), x + PADDING, y + 65, 0xFF8A8070, false);
        }

        if (this.profile.isModified()) {
            context.text(this.font, "※ " + this.profile.summary(),
                    x + PADDING, y + 54, 0xFFE8A87A, false);
        }
    }

    private void drawPillars(GuiGraphicsExtractor context) {
        int x = panelX() + PADDING;
        int y = panelY() + 82;
        // 渐进式解锁：完整四柱要到第 14 天
        if (!ClientFateState.unlocked(4)) {
            context.text(this.font, "§7（完整四柱会在第 14 天解锁，现在先记住自己的属性就好）",
                    x, y, 0xFF8A8070, false);
            return;
        }
        int columnWidth = 92;

        List<Bazi.Pillar> pillars = this.bazi.pillars();
        for (int i = 0; i < pillars.size(); i++) {
            Bazi.Pillar pillar = pillars.get(i);
            int cx = x + i * columnWidth;

            context.text(this.font, pillar.label(), cx, y, 0xFF8A8070, false);

            // 天干
            GanZhi.Gan gan = pillar.ganZhi().gan();
            context.text(this.font, gan.zh() + " " + this.bazi.tenGod(gan),
                    cx, y + 12, gan.wuXing().color(), true);

            // 地支
            GanZhi.Zhi zhi = pillar.ganZhi().zhi();
            context.text(this.font, zhi.zh() + " " + zhi.animal(),
                    cx, y + 24, zhi.wuXing().color(), true);

            // 藏干
            StringBuilder hidden = new StringBuilder("藏 ");
            for (GanZhi.Gan item : zhi.hiddenGan()) {
                hidden.append(item.zh());
            }
            context.text(this.font, hidden.toString(), cx, y + 36, 0xFF7A7A6A, false);

            // 纳音
            context.text(this.font, pillar.ganZhi().nayinName(), cx, y + 46, 0xFF6A6A5A, false);

            // 日主加个框，标出「这是我」
            if (i == 2) {
                context.fill(cx - 3, y - 2, cx + columnWidth - 8, y + 58, 0x22E8D8A0);
            }
        }
    }

    private void drawElements(GuiGraphicsExtractor context) {
        int x = panelX() + PADDING;
        int y = panelY() + 140;
        if (!ClientFateState.unlocked(4)) {
            y = panelY() + 108;
        }

        context.text(this.font, "五行权重", x, y, 0xFF8A8070, false);

        Map<GanZhi.WuXing, Double> chart = this.bazi.chart();
        double max = 1.0D;
        for (double value : chart.values()) {
            max = Math.max(max, value);
        }

        int barX = x + 54;
        int barY = y + 12;
        int barMaxWidth = 130;
        Bazi.YongShen yong = this.bazi.yongShen();

        for (GanZhi.WuXing element : GanZhi.WuXing.VALUES) {
            double value = chart.getOrDefault(element, 0.0D);
            context.text(this.font, element.zh(), x, barY, element.color(), false);

            int width = (int) Math.round(barMaxWidth * value / max);
            context.fill(barX, barY, barX + barMaxWidth, barY + 7, 0xFF1E1A22);
            context.fill(barX, barY, barX + Math.max(1, width), barY + 7, element.color());

            String tag = "";
            if (element == yong.primary()) {
                tag = " 用神";
            } else if (yong.favorable().contains(element)) {
                tag = " 喜";
            } else if (yong.unfavorable().contains(element)) {
                tag = " 忌";
            }
            context.text(this.font, String.format("%.2f", value) + tag,
                    barX + barMaxWidth + 8, barY, 0xFFA89A80, false);
            barY += 12;
        }

        // 日主强度条
        int strengthY = barY + 6;
        context.text(this.font, "身强弱", x, strengthY, 0xFF8A8070, false);
        double ratio = this.bazi.strengthRatio();
        int ratioWidth = (int) Math.round(barMaxWidth * Math.min(2.0D, ratio) / 2.0D);
        context.fill(barX, strengthY, barX + barMaxWidth, strengthY + 7, 0xFF1E1A22);
        context.fill(barX, strengthY, barX + Math.max(1, ratioWidth), strengthY + 7,
                ratio >= 1.18D ? 0xFFD08A5A : ratio <= 0.85D ? 0xFF5A8AD0 : 0xFF7AC88A);
        context.text(this.font, String.format("%.2f", ratio) + "（1.00 为中和）",
                barX + barMaxWidth + 8, strengthY, 0xFFA89A80, false);
    }

    private void drawShenSha(GuiGraphicsExtractor context) {
        int x = panelX() + 244;
        int y = panelY() + 140;
        // 神煞是术语层 —— 专业模式才显示
        if (!ClientFateState.professional()) {
            return;
        }
        context.text(this.font, "神煞", x, y, 0xFF8A8070, false);

        List<ShenSha.Mark> marks = new ArrayList<>(ShenSha.of(this.bazi));
        ShenSha.Mark jiangXing = ShenSha.jiangXingIf(this.bazi);
        if (jiangXing != null) {
            marks.add(jiangXing);
        }

        int lineY = y + 12;
        int shown = 0;
        for (ShenSha.Mark mark : marks) {
            if (shown++ >= 6) {
                context.text(this.font, "… 共 " + marks.size() + " 项",
                        x, lineY, 0xFF6A6A5A, false);
                break;
            }
            int color = mark.auspice() > 0 ? 0xFF8AD4A0
                    : mark.auspice() < 0 ? 0xFFD48888 : 0xFFC8C8A0;
            context.text(this.font, mark.name(), x, lineY, color, false);
            context.text(this.font, mark.note(), x + 52, lineY, 0xFF7A7A6A, false);
            lineY += 11;
        }
        if (marks.isEmpty()) {
            context.text(this.font, "命中无煞，平淡是真", x, lineY, 0xFF7A7A6A, false);
        }
    }

    private void drawYun(GuiGraphicsExtractor context) {
        int x = panelX() + PADDING;
        int y = panelY() + 208;

        long seed = ClientFateState.seed();
        DimensionProfile dimension = currentDimension();
        long epochDay = currentEpochDay(seed);
        int age = Yun.age(epochDay);

        // 渐进式解锁：大运流年要到第 19 天
        if (!ClientFateState.unlocked(5)) {
            context.text(this.font, "§7（流年大运会在第 19 天解锁）",
                    x, y, 0xFF8A8070, false);
            return;
        }
        context.text(this.font, "大运", x, y, 0xFF8A8070, false);
        List<Yun.DaYun> steps = Yun.daYun(this.bazi, seed, "player:" + this.profile.owner());
        Yun.DaYun current = Yun.current(steps, age);

        StringBuilder builder = new StringBuilder();
        for (Yun.DaYun step : steps) {
            boolean isCurrent = current != null && step.index() == current.index();
            builder.append(step.ganZhi().zh());
            builder.append(isCurrent ? '【' : ' ');
            if (isCurrent) {
                builder.append('】');
            }
        }
        context.text(this.font, builder.toString(), x + 36, y, 0xFFC8BFA0, false);

        Yun.Moment moment = Yun.moment(this.bazi, seed, dimension.shortKey(), epochDay,
                dimension.hourIndex(currentDayTime()), currentHourName(dimension));
        context.text(this.font,
                "流年 " + moment.liuNian() + "　流月 " + moment.liuYue()
                        + "　流日 " + moment.liuRi() + "　流时 " + moment.liuShi(),
                x, y + 13, 0xFFC8BFA0, false);
        context.text(this.font,
                "岁数 " + age + "　维度 " + dimension.zh()
                        + "　时辰 " + dimension.hourName(currentDayTime())
                        + "　太岁在 " + FengShuiSystems.TaiSui.directionName(age),
                x, y + 25, 0xFF8A8070, false);

        // 本命卦
        String hexagram = com.chenchen.yishu.spirit.MobFate.hexagramName(
                StableKey.of(seed, "playerhex").add(this.profile.owner()));
        context.text(this.font, "本命卦 " + hexagram, x, y + 37, 0xFFB09AD8, false);
    }

    // ==================================================================== 环境

    private DimensionProfile currentDimension() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return DimensionProfile.OVERWORLD;
        }
        return DimensionProfile.of(minecraft.level.dimension().identifier().toString());
    }

    private long currentEpochDay(long seed) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return 0L;
        }
        return minecraft.level.getOverworldClockTime() / 24000L;
    }

    private long currentDayTime() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.level == null ? 0L : minecraft.level.getOverworldClockTime();
    }

    private String currentHourName(DimensionProfile dimension) {
        return dimension.hourName(currentDayTime());
    }

    // ==================================================================== 交互

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.isEscape()) {
            onClose();
            return true;
        }
        return super.keyPressed(event);
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
