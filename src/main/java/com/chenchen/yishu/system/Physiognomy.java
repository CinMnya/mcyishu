package com.chenchen.yishu.system;

import com.chenchen.yishu.YishuMod;
import com.chenchen.yishu.core.StableKey;
import com.chenchen.yishu.registry.YsRegistry;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.gossip.GossipContainer;
import net.minecraft.world.entity.ai.gossip.GossipType;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.EntityHitResult;

import java.util.List;
import java.util.UUID;

/**
 * 易枢 · 相面招募（第 20 轮）。
 *
 * <p>相面镜照村民面相：面相由村民 UUID 确定 —— 同一个村民，
 * 拿什么镜子照、谁来照、照多少遍，都是一个面相。命由天定。
 *
 * <p><b>招募</b>走的是原版好感度（gossip）这条官方通道：
 * 对贵相 / 富相村民按住潜行照镜，为其传一段美名
 * （{@code GossipContainer.add} = MAJOR_POSITIVE），
 * 从此他的交易对你大幅折价 —— 这是村民交易折扣的正统机制，
 * 与交易列表本身毫不相干（绝不直接动 MerchantOffers，第 13 轮的教训）。
 */
public final class Physiognomy {

    private Physiognomy() {
    }

    private static final String KEY_RECRUITED = "face:";

    /** 一张面相。 */
    public record FaceTag(String zh, String desc, int luck) {
    }

    private static final List<FaceTag> FACES = List.of(
            new FaceTag("贵相", "额阔眉清，双目有神。此人大贵，与其相交，事事便宜。", 3),
            new FaceTag("富相", "鼻丰颧挺，耳厚垂珠。此人积财有道，买卖上最见诚意。", 2),
            new FaceTag("寿相", "眉长过目，面色温润。此人福寿绵长，与他处得久便知。", 2),
            new FaceTag("慧相", "目秀而长，静而有光。此人一点即通，学问上肯让利。", 1),
            new FaceTag("平相", "五官端正，不骄不矜。寻常人，寻常日子，寻常买卖。", 0),
            new FaceTag("破相", "眉乱眼浮，法令紧绷。此人命途多舛，买卖上要留个心眼。", -1),
            new FaceTag("贫相", "面薄唇掀，行坐不安。此人身在困顿，讨价还价锱铢必较。", -2)
    );

    public static void register() {
        UseEntityCallback.EVENT.register(Physiognomy::onUseEntity);
    }

    /** 面相由 UUID 定死：同一个村民永远同一张脸。 */
    public static FaceTag faceOf(UUID id) {
        int index = StableKey.of("face").add(id.toString()).random().nextInt(FACES.size());
        return FACES.get(index);
    }

    private static InteractionResult onUseEntity(Player player, net.minecraft.world.level.Level level,
                                                 InteractionHand hand, Entity entity,
                                                 EntityHitResult hit) {
        if (level.isClientSide() || !(player instanceof ServerPlayer serverPlayer)
                || !(entity instanceof Villager villager)) {
            return InteractionResult.PASS;
        }
        ItemStack stack = player.getItemInHand(hand);
        if (!stack.is(YsRegistry.FACE_MIRROR)) {
            return InteractionResult.PASS;
        }

        ServerLevel server = (ServerLevel) level;
        FaceTag face = faceOf(villager.getUUID());
        String villagerName = villager.getName().getString();
        java.util.List<String> lines = new java.util.ArrayList<>();
        lines.add("§6" + villagerName + "§f 面呈 §b" + face.zh() + "§f。");
        lines.add("§7" + face.desc());

        // 潜行照镜 = 招募：为贵人传名，走好感度官方通道
        if (player.isShiftKeyDown() && face.luck() >= 2) {
            if (recruit(server, serverPlayer, villager)) {
                lines.add("§e你为他在乡里传了美名 —— 此后他的买卖，对你另眼相看。");
                lines.add("§7（好感大增，交易折价是村民的正统机制。）");
            } else {
                lines.add("§7美名早已传过，不必再传。");
            }
        } else if (player.isShiftKeyDown()) {
            lines.add("§7此人面相寻常，不值得为他传名。");
        } else if (face.luck() >= 2) {
            lines.add("§7（潜行照镜可为贵人传名招募，交易折价。）");
        }

        server.playSound(null, villager.blockPosition(),
                SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.7F, 1.2F);
        com.chenchen.yishu.util.YsText.send(serverPlayer, "相面", lines);
        return InteractionResult.SUCCESS_SERVER;
    }

    /**
     * 招募：给这位村民记一笔「永久好感」。
     *
     * <p>{@code GossipContainer.add} 是村民折扣的官方数据源，
     * 每个村民同一个人只记一次（记号存 {@link YishuData}）。
     */
    private static boolean recruit(ServerLevel server, ServerPlayer player, Villager villager) {
        String key = KEY_RECRUITED + villager.getUUID() + ":" + player.getGameProfile().name();
        YishuData data = YishuData.of(server);
        if (data.get(key) != null) {
            return false;
        }
        data.put(key, "1");
        GossipContainer gossips = villager.getGossips();
        gossips.add(player.getUUID(), GossipType.MAJOR_POSITIVE, 25);
        gossips.add(player.getUUID(), GossipType.MINOR_POSITIVE, 25);
        server.sendParticles(net.minecraft.core.particles.ParticleTypes.HAPPY_VILLAGER,
                villager.getX(), villager.getY() + 2.0D, villager.getZ(),
                8, 0.4D, 0.5D, 0.4D, 0.0D);
        player.sendSystemMessage(Component.literal(
                "§d【易枢·相面】§f已招募 §b" + villager.getName().getString() + "§f。"));
        return true;
    }
}
