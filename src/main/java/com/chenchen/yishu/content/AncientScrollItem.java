package com.chenchen.yishu.content;

import com.chenchen.yishu.system.Chronicle;
import com.chenchen.yishu.system.YishuData;
import com.chenchen.yishu.util.YsText;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

/**
 * 古卷 —— 遗迹里开出来的残卷，记载《山河志》。
 *
 * <p>玩家每右键展开一卷，就会读到编年史的下一章；进度存在存档数据里，
 * 所有古卷共用同一条进度线，读完全部十三卷为止。
 */
public class AncientScrollItem extends YsItemContent.Material {

    public AncientScrollItem(Properties properties, String noteKey) {
        super(properties, noteKey);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!(level instanceof ServerLevel server) || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.SUCCESS;
        }
        YishuData data = YishuData.of(server);
        String key = "chronicle:" + serverPlayer.getGameProfile().name();
        String raw = data.get(key);
        int readIndex;
        try {
            readIndex = raw == null ? 0 : Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            readIndex = 0;
        }

        Chronicle.Volume volume = Chronicle.volume(readIndex);
        List<String> lines = new ArrayList<>();
        lines.add("§8" + volume.sub());
        lines.add("");
        lines.addAll(volume.lines());
        lines.add("");
        if (readIndex >= Chronicle.count() - 1) {
            lines.add("§7《山河志》至此合卷。");
        } else {
            lines.add("§7（下一卷还在某个遗迹里等着。）");
        }
        YsText.panel(player, "山河志 · " + volume.zh(), lines);

        server.playSound(null, player.blockPosition(), SoundEvents.BOOK_PAGE_TURN,
                SoundSource.PLAYERS, 0.9F, 1.0F);
        if (readIndex < Chronicle.count() - 1) {
            data.put(key, String.valueOf(readIndex + 1));
        }
        return InteractionResult.SUCCESS_SERVER;
    }
}
