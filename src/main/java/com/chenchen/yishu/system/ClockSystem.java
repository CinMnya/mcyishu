package com.chenchen.yishu.system;

import com.chenchen.yishu.calendar.YishuCalendar;
import com.chenchen.yishu.util.YsText;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.Items;

/**
 * 易枢 · 时钟与黄历（第 18 轮）。
 *
 * <p>玩家想看时间，不该先去翻指令表。两条最自然的路径都接上了：
 * <ul>
 *   <li><b>手持时钟右键</b> —— 直接展开黄历。</li>
 *   <li><b>点物品展示框里的时钟</b> —— 墙上的挂钟也能看，而且不会把钟转歪。</li>
 * </ul>
 *
 * <p>面板走现成的 {@code YsText.panel}（服务端算好文本 → DisplayPayload →
 * TextPanelScreen），不用新增网络包。历法只在服务端算，
 * 客户端拿到的永远是已经对好主世界时辰的结果。
 */
public final class ClockSystem {

    private ClockSystem() {
    }

    public static void register() {
        UseItemCallback.EVENT.register(ClockSystem::onUseItem);
        UseEntityCallback.EVENT.register(ClockSystem::onUseEntity);
    }

    private static InteractionResult onUseItem(
            net.minecraft.world.entity.player.Player player,
            net.minecraft.world.level.Level level,
            net.minecraft.world.InteractionHand hand) {
        if (level.isClientSide() || !player.getItemInHand(hand).is(Items.CLOCK)) {
            return InteractionResult.PASS;
        }
        if (!(player instanceof ServerPlayer serverPlayer)
                || !(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.PASS;
        }
        open(serverPlayer, serverLevel);
        // 吃掉这次右键：原版时钟本来就只有「看天」的动作，
        // 不吞掉也不会怎样，但吞掉能避免和别的模组抢同一次交互。
        return InteractionResult.SUCCESS_SERVER;
    }

    private static InteractionResult onUseEntity(
            net.minecraft.world.entity.player.Player player,
            net.minecraft.world.level.Level level,
            net.minecraft.world.InteractionHand hand,
            net.minecraft.world.entity.Entity entity,
            net.minecraft.world.phys.EntityHitResult hit) {
        if (level.isClientSide() || !(entity instanceof ItemFrame frame)) {
            return InteractionResult.PASS;
        }
        if (!frame.getItem().is(Items.CLOCK)) {
            return InteractionResult.PASS;
        }
        if (!(player instanceof ServerPlayer serverPlayer)
                || !(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.PASS;
        }
        open(serverPlayer, serverLevel);
        // 返回 SUCCESS 会阻止展示框的默认「旋转物品」行为 ——
        // 玩家点了挂钟就是想看时间，不该顺手把钟转个角度。
        return InteractionResult.SUCCESS_SERVER;
    }

    private static void open(ServerPlayer player, ServerLevel level) {
        YishuCalendar.Moment moment = YishuCalendar.now(level);
        YsText.panel(player, "黄历 · " + moment.dateText(),
                YishuCalendar.clockPanel(level, player));
    }
}
