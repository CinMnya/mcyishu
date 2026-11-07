package com.chenchen.yishu.system;

import com.chenchen.yishu.calendar.YishuCalendar;
import com.chenchen.yishu.dimension.DimensionProfile;
import com.chenchen.yishu.fengshui.FengShuiSystems;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 易枢 · 动土吉凶 hook（第 20 轮）。
 *
 * <p>黄历不能只写在面板里 —— 要落在手上。玩家放方块的那一刻，
 * 查当日宜忌：
 *
 * <ul>
 *   <li><b>安床</b>：宜安床之日落床，晨起神清气爽（再生）；忌日落床，
 *       一夜不安（黑瞳）；</li>
 *   <li><b>安门</b>：宜日安门，家宅顺遂（急迫小赠）；忌日安门，
 *       门楣发涩（短时疲劳）；</li>
 *   <li><b>动土</b>：任何玩家放置方块都算动土。忌动土之日动土，
 *       偶有土石反噬（迟缓）；有石敢当或镇宅石在旁则镇住。</li>
 * </ul>
 *
 * <p>hook 只「评吉凶」，从不拦手 —— 宜忌是提示，不是围栏。
 * 动土提示有 30 秒节流，不会每放一块土都刷一遍屏。
 */
public final class DayActionHook {

    private DayActionHook() {
    }

    /** 动土提示的节流（毫秒）。 */
    private static final long DIG_THROTTLE_MS = 30_000L;
    /** 忌日动土的反噬概率。 */
    private static final int BACKLASH_DENOMINATOR = 4;

    private static final Map<String, Long> LAST_DIG_NOTICE = new HashMap<>();

    /** 一条吉凶判定。 */
    private record Verdict(String key, String zh) {
    }

    public static void register() {
        UseBlockCallback.EVENT.register(DayActionHook::onUseBlock);
    }

    private static net.minecraft.world.InteractionResult onUseBlock(
            net.minecraft.world.entity.player.Player player,
            net.minecraft.world.level.Level level,
            net.minecraft.world.InteractionHand hand,
            net.minecraft.world.phys.BlockHitResult hit) {
        if (level.isClientSide() || !(player instanceof ServerPlayer serverPlayer)
                || !(level instanceof ServerLevel server)) {
            return net.minecraft.world.InteractionResult.PASS;
        }
        ItemStack stack = player.getItemInHand(hand);
        if (!(stack.getItem() instanceof BlockItem blockItem)) {
            return net.minecraft.world.InteractionResult.PASS;
        }
        Block block = blockItem.getBlock();
        Verdict verdict;
        if (block instanceof BedBlock) {
            verdict = new Verdict("bed", "安床");
        } else if (block instanceof DoorBlock || block instanceof TrapDoorBlock) {
            verdict = new Verdict("door", "安门");
        } else {
            verdict = new Verdict("dig", "动土");
        }
        evaluate(server, serverPlayer, hit.getBlockPos(), verdict);
        return net.minecraft.world.InteractionResult.PASS;
    }

    /** 查当日宜忌，落吉凶。 */
    private static void evaluate(ServerLevel level, ServerPlayer player, BlockPos pos, Verdict verdict) {
        YishuCalendar.Moment moment = YishuCalendar.now(level);
        boolean favorable = FengShuiSystems.Almanac.isFavorable(moment.almanac(), verdict.key());
        boolean taboo = FengShuiSystems.Almanac.isTaboo(moment.almanac(), verdict.key());

        if (verdict.key().equals("dig")) {
            evaluateDig(level, player, pos, favorable, taboo);
            return;
        }

        List<String> lines = new java.util.ArrayList<>();
        if (favorable && !taboo) {
            lines.add("§a今日宜" + verdict.zh() + "，落得安稳。");
            if (verdict.key().equals("bed")) {
                player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 20 * 60, 0));
            } else {
                player.addEffect(new MobEffectInstance(MobEffects.HASTE, 20 * 60, 0));
            }
        } else if (taboo && !favorable) {
            lines.add("§c今日忌" + verdict.zh() + "，强行落定，恐有不安。");
            if (verdict.key().equals("bed")) {
                player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 20 * 15, 0));
            } else {
                player.addEffect(new MobEffectInstance(MobEffects.MINING_FATIGUE, 20 * 30, 0));
            }
        } else {
            lines.add("§7今日黄历未提" + verdict.zh() + "，寻常一日。");
        }
        com.chenchen.yishu.util.YsText.send(player, "黄历", lines);
    }

    /** 动土：节流提示 + 忌日反噬。 */
    private static void evaluateDig(ServerLevel level, ServerPlayer player, BlockPos pos,
                                    boolean favorable, boolean taboo) {
        String name = player.getGameProfile().name();
        long now = System.currentTimeMillis();
        Long last = LAST_DIG_NOTICE.get(name);

        if (taboo && !favorable
                && !com.chenchen.yishu.content.YsBlockContent.Warding.isWarded(level, pos)
                && level.getRandom().nextInt(BACKLASH_DENOMINATOR) == 0) {
            player.addEffect(new MobEffectInstance(MobEffects.MINING_FATIGUE, 20 * 20, 0));
            player.sendSystemMessage(Component.literal(
                    "§c【易枢·黄历】§f今日忌动土，土石间有煞气反噬 §7（石敢当或镇宅石可镇）"));
        } else if ((last == null || now - last > DIG_THROTTLE_MS) && favorable) {
            LAST_DIG_NOTICE.put(name, now);
            player.sendSystemMessage(Component.literal(
                    "§a【易枢·黄历】§f今日宜动土兴建，放手去造。"));
        } else if (last == null || now - last > DIG_THROTTLE_MS) {
            LAST_DIG_NOTICE.put(name, now);
        }
    }
}
