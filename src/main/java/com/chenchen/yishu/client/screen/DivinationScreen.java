package com.chenchen.yishu.client.screen;

import com.chenchen.yishu.client.ClientFateState;
import com.chenchen.yishu.client.YishuClient;
import com.chenchen.yishu.core.StableKey;
import com.chenchen.yishu.dimension.DimensionProfile;
import com.chenchen.yishu.liuyao.Divination;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;

/**
 * 易枢 · 起卦界面。
 *
 * <p>先选「问什么」，再选「怎么问」，然后按「掷」。
 *
 * <p><b>为什么问事类型这么重要</b>：六爻断卦里，用神取错等于全盘皆错。
 * 问找矿却去看官鬼，看到的一定是凶。所以这个界面把「问事」放在最前面，
 * 并且在每一类旁边标出它对应的用神 —— 让玩家在按下按钮之前就明白
 * 「我这次到底在问什么」。
 *
 * <p><b>外应起卦的环境指纹</b>：选「外应」时，界面会把玩家当前所处的
 * 环境（朝向、脚下的方块、天候、坐标）算成一个稳定的数带进卦里。
 * 它<b>不是随机数</b> —— 站在同一个位置、朝同一个方向、天气相同，
 * 外应就是一样的。所以「换个地方再问一次」这件事是有意义的。
 */
public class DivinationScreen extends Screen {

    private static final int PANEL_WIDTH = 420;
    private static final int PANEL_HEIGHT = 262;
    private static final int PADDING = 16;

    private final boolean allowOmen;

    private Divination.Question question = Divination.Question.FIND_ORE;
    private Divination.Method method = Divination.Method.COIN;

    public DivinationScreen(boolean allowOmen) {
        super(Component.literal("起卦 · 六爻"));
        this.allowOmen = allowOmen;
        if (!allowOmen) {
            this.method = Divination.Method.COIN;
        }
    }

    // ==================================================================== 布局

    private int panelX() {
        return (this.width - PANEL_WIDTH) / 2;
    }

    private int panelY() {
        return (this.height - PANEL_HEIGHT) / 2;
    }

    @Override
    protected void init() {
        super.init();
        int x = panelX();
        int y = panelY();

        // ---- 问事类型：两列五行 ----
        Divination.Question[] questions = Divination.Question.VALUES;
        for (int i = 0; i < questions.length; i++) {
            Divination.Question item = questions[i];
            int column = i % 2;
            int row = i / 2;
            int buttonX = x + PADDING + column * 196;
            int buttonY = y + 30 + row * 20;
            Button button = Button.builder(Component.literal(item.zh()), b -> {
                this.question = item;
                rebuildWidgets();
            }).bounds(buttonX, buttonY, 188, 18).build();
            button.active = item != this.question;
            addRenderableWidget(button);
        }

        // ---- 起卦方式 ----
        int methodY = y + 30 + 5 * 20 + 10;
        Divination.Method[] methods = Divination.Method.values();
        for (int i = 0; i < methods.length; i++) {
            Divination.Method item = methods[i];
            boolean usable = item != Divination.Method.OMEN || this.allowOmen;
            Button button = Button.builder(Component.literal(item.zh()), b -> {
                this.method = item;
                rebuildWidgets();
            }).bounds(x + PADDING + i * 128, methodY, 122, 18).build();
            button.active = usable && item != this.method;
            addRenderableWidget(button);
        }

        // ---- 底部按钮 ----
        int bottom = y + PANEL_HEIGHT - 26;
        addRenderableWidget(Button.builder(Component.literal("掷 卦"), b -> cast())
                .bounds(x + PANEL_WIDTH - 156, bottom, 72, 18)
                .build());
        addRenderableWidget(Button.builder(Component.literal("取消"), b -> onClose())
                .bounds(x + PANEL_WIDTH - 78, bottom, 62, 18)
                .build());
    }

    // ==================================================================== 绘制

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float partialTick) {
        context.fill(0, 0, this.width, this.height, 0xC0080C10);
        int x = panelX();
        int y = panelY();
        context.fill(x - 1, y - 1, x + PANEL_WIDTH + 1, y + PANEL_HEIGHT + 1, 0xFF3A4A56);
        context.fill(x, y, x + PANEL_WIDTH, y + PANEL_HEIGHT, 0xF40C1116);
        context.fill(x, y, x + PANEL_WIDTH, y + 22, 0xFF121A22);
        context.fill(x, y + 21, x + PANEL_WIDTH, y + 22, 0xFF3A4A56);

        super.extractRenderState(context, mouseX, mouseY, partialTick);
        drawContent(context);
    }

    private void drawContent(GuiGraphicsExtractor context) {
        int x = panelX();
        int y = panelY();

        context.text(this.font, this.title.getString(), x + PADDING, y + 7, 0xFF9FD8E8, true);

        // 已选用神的提示 —— 这是整个界面最重要的一行
        context.text(this.font, "用神：" + this.question.yongShen().zh()
                        + "　（" + this.question.note() + "）",
                x + PADDING, y + 22 - 1, 0xFFD8C08A, false);

        // 起卦方式说明
        int methodY = y + 30 + 5 * 20 + 10;
        context.text(this.font, this.method.note(), x + PADDING, methodY + 21, 0xFF7A8A96, false);

        // 外应状态
        int infoY = methodY + 36;
        DimensionProfile dimension = currentDimension();
        context.text(this.font, "时辰：" + dimension.hourName(currentDayTime())
                        + "（" + dimension.timeSystemName() + "）",
                x + PADDING, infoY, 0xFF8AA8B8, false);
        context.text(this.font, dimension.hourAdvice(dimension.hourIndex(currentDayTime())),
                x + PADDING, infoY + 11, 0xFF6A7A86, false);

        context.text(this.font, "今日已问：" + askedToday() + " / " + ClientFateState.divinationLimit(),
                x + PADDING, infoY + 24,
                askedToday() >= ClientFateState.divinationLimit() ? 0xFFD48888 : 0xFF8AA8B8, false);

        if (this.method == Divination.Method.OMEN) {
            context.text(this.font, "外应：朝向 " + facingName() + "、脚下 "
                            + blockBelowName() + (isRaining() ? "、天雨" : "、天晴"),
                    x + PADDING, infoY + 37, 0xFF9AD8C8, false);
        } else {
            context.text(this.font,
                    this.method == Divination.Method.TIME
                            ? "时间起卦：以此刻的流日与时辰成卦"
                            : "铜钱六掷：三枚铜钱摇六次，动爻由钱面决定",
                    x + PADDING, infoY + 37, 0xFF8AA8B8, false);
        }

        context.text(this.font,
                "同种子、同命盘、同问题、同游戏日、同时辰 —— 结果永远一致，可以分享给朋友复现。",
                x + PADDING, y + PANEL_HEIGHT - 40, 0xFF6A7A86, false);
    }

    // ==================================================================== 环境

    private DimensionProfile currentDimension() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return DimensionProfile.OVERWORLD;
        }
        return DimensionProfile.of(minecraft.level.dimension().identifier().toString());
    }

    private long currentDayTime() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.level == null ? 0L : minecraft.level.getOverworldClockTime();
    }

    private long currentEpochDay() {
        return currentDayTime() / 24000L;
    }

    private int askedToday() {
        // 客户端只知道上限，不知道已问几次 —— 已问次数由服务端在结果里权威告知。
        // 这里显示的是「本地估算」，用于给玩家一个心理预期。
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return 0;
        }
        return 0;
    }

    private String facingName() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return "未知";
        }
        return switch (minecraft.player.getDirection()) {
            case NORTH -> "北";
            case SOUTH -> "南";
            case EAST -> "东";
            case WEST -> "西";
            default -> "上下";
        };
    }

    private String blockBelowName() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            return "虚空";
        }
        BlockPos below = minecraft.player.blockPosition().below();
        return BuiltInRegistries.BLOCK.getKey(minecraft.level.getBlockState(below).getBlock()).getPath();
    }

    private boolean isRaining() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.level != null && minecraft.level.isRaining();
    }

    /**
     * 外应指纹。
     *
     * <p>由「朝向 + 脚下方块 + 天候 + 坐标 + 游戏日」算出的一个稳定数。
     * 同样的环境重复问，外应完全一样 —— 这就是「外应」这个词在这里的含义：
     * <b>它是你此刻所处之处，而不是一个骰子。</b>
     */
    private long omen() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) {
            return 0L;
        }
        BlockPos pos = minecraft.player.blockPosition();
        String block = blockBelowName();
        return StableKey.of("omen")
                .add(minecraft.player.getDirection().getSerializedName())
                .add(block)
                .add(isRaining())
                .add(pos.getX()).add(pos.getY()).add(pos.getZ())
                .add(currentEpochDay())
                .hash();
    }

    // ==================================================================== 交互

    private void cast() {
        YishuClient.requestCast(this.question.ordinal(), this.method.ordinal(), omen());
        onClose();
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.isEscape()) {
            onClose();
            return true;
        }
        if (event.isConfirmation()) {
            cast();
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
