package com.chenchen.yishu.content;

import com.chenchen.yishu.fate.Bazi;
import com.chenchen.yishu.fate.FateProfile;
import com.chenchen.yishu.fate.GanZhi;
import com.chenchen.yishu.fate.Yun;
import com.chenchen.yishu.system.PlainSpeak;
import com.chenchen.yishu.util.YsText;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.Filterable;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.WrittenBookContent;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/**
 * 命理书 —— 对讲台右键，把命盘导出成一本真正的「成书」放在讲台上。
 *
 * <p>这是规范里「讲台 + 命书：导出八字文本」的实现：书放在讲台上后
 * 任何玩家都能翻阅、多人共享，也可以直接拿走。导出的书是原版
 * written_book，所以即使卸载模组，书还在。
 */
public class FateBookItem extends YsItemContent.Material {

    public FateBookItem(Properties properties, String noteKey) {
        super(properties, noteKey);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);

        // ---- 对讲台右键：导出命盘成书 ----
        if (state.getBlock() instanceof LecternBlock) {
            if (level instanceof ServerLevel server && context.getPlayer() instanceof ServerPlayer player) {
                exportToLectern(server, player, pos, context.getItemInHand());
            }
            return InteractionResult.SUCCESS_SERVER;
        }

        // ---- 潜行右键其他方块：白话帮助 ----
        if (context.getPlayer() != null && context.getPlayer().isShiftKeyDown()
                && context.getPlayer() instanceof ServerPlayer player) {
            YsText.send(player, "这是什么", List.of(
                    "这本书记录着你的命数。",
                    "对着讲台右键，命盘就会变成一本放在讲台上的书，",
                    "朋友们都能翻看。",
                    "（不认识讲台？用四个木板加一个书架合成。）"));
            return InteractionResult.SUCCESS_SERVER;
        }
        return InteractionResult.PASS;
    }

    /** 组命盘文本 → 写成原版成书 → 放上讲台。 */
    private void exportToLectern(ServerLevel server, ServerPlayer player, BlockPos pos, ItemStack bookItem) {
        BlockEntity be = server.getBlockEntity(pos);
        if (!(be instanceof LecternBlockEntity lectern)) {
            return;
        }
        if (lectern.hasBook()) {
            YsText.chat(player, "讲台上已经有一本书了，先取走它。");
            return;
        }

        FateProfile profile = FateProfile.of(server.getSeed(), player.getGameProfile().name());
        Bazi bazi = profile.active();
        List<String> lines = new ArrayList<>();

        // 第一页：白话
        lines.add("【" + player.getGameProfile().name() + " 的命盘】");
        lines.add("");
        lines.add(PlainSpeak.fatePlain(bazi) + "。");
        lines.add("");
        lines.add("住在对你有利属性的地方，你会更强。");

        // 第二页：四柱与流日（术语但可读）
        long day = server.getOverworldClockTime() / 24000L;
        GanZhi.Pair today = Yun.liuRi(server.getSeed(),
                server.dimension().identifier().toString(), day);
        lines.add("【四柱】");
        for (Bazi.Pillar pillar : bazi.pillars()) {
            GanZhi.Pair gz = pillar.ganZhi();
            lines.add(pillar.label() + "：" + gz.gan().zh() + gz.zhi().zh());
        }
        lines.add("");
        lines.add("【今日】" + today.gan().zh() + today.zhi().zh());

        // 第三页：神煞（若有）
        List<String> shenSha = ShenShaExporter.of(bazi);
        if (!shenSha.isEmpty()) {
            lines.add("【神煞】");
            lines.addAll(shenSha);
        }

        // 组装成书：每 14 行一页
        List<Filterable<Component>> pages = new ArrayList<>();
        for (int i = 0; i < lines.size(); i += 14) {
            StringBuilder page = new StringBuilder();
            for (String line : lines.subList(i, Math.min(i + 14, lines.size()))) {
                page.append(line).append('\n');
            }
            pages.add(Filterable.passThrough(Component.literal(page.toString())));
        }

        ItemStack writtenBook = new ItemStack(Items.WRITTEN_BOOK);
        writtenBook.set(net.minecraft.core.component.DataComponents.WRITTEN_BOOK_CONTENT,
                new WrittenBookContent(
                        Filterable.passThrough("命盘 · " + player.getGameProfile().name()),
                        "易枢", 0, pages, false));

        lectern.setBook(writtenBook);
        server.setBlock(pos, server.getBlockState(pos).setValue(LecternBlock.HAS_BOOK, true), 3);

        server.playSound(null, pos, SoundEvents.BOOK_PAGE_TURN, SoundSource.BLOCKS, 1.0F, 1.2F);
        YsText.chat(player, "命盘已导出到讲台，朋友们都能翻阅了。");
    }

    /** 小工具：把神煞列表转成书页文本（避免命书依赖完整命盘界面逻辑）。 */
    private static final class ShenShaExporter {
        static List<String> of(Bazi bazi) {
            List<String> out = new ArrayList<>();
            for (com.chenchen.yishu.fate.ShenSha.Mark sha : com.chenchen.yishu.fate.ShenSha.of(bazi)) {
                out.add(sha.name() + "：" + sha.note());
            }
            return out;
        }

        private ShenShaExporter() {
        }
    }
}
