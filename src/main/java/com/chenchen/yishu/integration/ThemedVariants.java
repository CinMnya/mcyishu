package com.chenchen.yishu.integration;

import com.chenchen.yishu.core.StableKey;
import com.chenchen.yishu.core.YsReg;
import com.chenchen.yishu.registry.YsRegistry;
import com.chenchen.yishu.system.YishuData;
import com.chenchen.yishu.util.YsText;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import java.util.Map;

/**
 * Themed variants (命理 / 风水 / 六爻) for vanilla content.
 *
 * <p>User request: naturally generated buildings, villagers and wandering
 * traders all have a chance to carry one of the mod's three themes.
 *
 * <ul>
 *   <li><b>Buildings</b>: when a player stands inside a vanilla structure, the
 *       structure rolls a deterministic theme. Themed sites announce themselves
 *       once and carry themed particles.</li>
 *   <li><b>Villagers / wandering traders</b>: each one rolls a theme and, if
 *       themed, emits themed particles. Themed <i>trades</i> are no longer
 *       injected at runtime — they ship as datapack entries in
 *       {@code data/yishu/villager_trade} and are merged into the vanilla
 *       profession pools via {@code data/minecraft/tags/villager_trade}.</li>
 * </ul>
 *
 * <p>The roll is deterministic: world seed + structure id (or entity type) +
 * chunk coordinates. Same world, same outcome. The "already themed" marker is
 * stored as a Fabric data attachment, so no save data grows with population.
 */
public final class ThemedVariants {

    private ThemedVariants() {
    }

    /** The three themes. */
    public enum Theme {
        FATE("命理", "Fate"),
        FENGSHUI("风水", "Feng Shui"),
        LIUYAO("六爻", "Liu Yao");

        private final String zh;
        private final String en;

        Theme(String zh, String en) {
            this.zh = zh;
            this.en = en;
        }

        public String zh() {
            return this.zh;
        }

        public String en() {
            return this.en;
        }
    }

    /** Probability (in percent) that any given site or trader is themed at all. */
    public static final int THEME_CHANCE = 45;

    /** Attachment marking a villager as already themed (value = theme name). */
    private static final AttachmentType<String> MARK =
            AttachmentRegistry.createDefaulted(YsReg.id("theme"), () -> "");

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTickCount() % 100 != 0) {
                return;
            }
            for (ServerLevel level : server.getAllLevels()) {
                for (ServerPlayer player : level.players()) {
                    try {
                        scanVillagers(level, player);
                        scanStructure(level, player);
                    } catch (RuntimeException ignored) {
                        // 单个扫描失败不能拖垮整个 tick
                    }
                }
            }
        });
    }

    // ==================================================================== rolls

    /** Deterministic roll: returns null when the site is simply not themed. */
    public static Theme roll(long worldSeed, String key, int cx, int cz) {
        long h = StableKey.of("theme")
                .add(worldSeed)
                .add(key)
                .add(cx)
                .add(cz)
                .hash();
        int bucket = (int) Math.floorMod(h, 100L);
        if (bucket >= THEME_CHANCE) {
            return null;
        }
        return Theme.values()[(int) Math.floorMod(h / 100L, Theme.values().length)];
    }

    // ==================================================================== villagers

    private static void scanVillagers(ServerLevel level, ServerPlayer player) {
        for (AbstractVillager villager : level.getEntitiesOfClass(AbstractVillager.class,
                player.getBoundingBox().inflate(48.0D))) {
            String mark = villager.getAttachedOrElse(MARK, "");
            if (!mark.isEmpty()) {
                continue;
            }
            String key = "villager:" + villager.getType().toString();
            Theme theme = roll(level.getSeed(), key,
                    villager.getBlockX() >> 4, villager.getBlockZ() >> 4);
            if (theme == null) {
                // 未主题化也要打标记，避免每 100 刻重复掷骰
                villager.setAttached(MARK, "none");
                continue;
            }
            villager.setAttached(MARK, theme.name());
            applyTheme(level, villager, theme);
        }
    }

    /**
     * 主题化的可见表现 —— 只发信号，<b>不动交易列表</b>。
     *
     * <p>⚠️ 这里曾经直接往 {@code villager.getOffers()} 里 add 一笔自定义交易。
     * 那是个错误做法：{@code MerchantOffers} 是带 {@code STREAM_CODEC} 的同步对象，
     * 绕过官方的 {@code overrideOffers()} 直接改列表，会和服务端菜单持有的
     * 索引 / 快照打架 —— 玩家一打开交易界面就被服务端判定为失效而关闭，
     * 表现为「交易界面秒闪退」。
     *
     * <p>主题商品现在改走 <b>数据包</b>：
     * {@code data/yishu/villager_trade} 定义交易本体，
     * {@code data/minecraft/tags/villager_trade} 把它注进原版职业池 ——
     * 这是 26.2 官方的路子，绝对安全，也不怕版本更新。
     *
     * <p>这个方法只负责「让主题看得见」：按主题撒一把粒子。
     */
    private static void applyTheme(ServerLevel level, AbstractVillager villager, Theme theme) {
        if (level.getRandom().nextFloat() >= 0.6F) {
            return;
        }
        var particle = switch (theme) {
            case FATE -> ParticleTypes.END_ROD;
            case FENGSHUI -> ParticleTypes.HAPPY_VILLAGER;
            case LIUYAO -> ParticleTypes.SOUL_FIRE_FLAME;
        };
        level.sendParticles(particle,
                villager.getX(), villager.getY() + 1.6D, villager.getZ(),
                8, 0.35D, 0.45D, 0.35D, 0.01D);
    }

    // ==================================================================== buildings

    private static void scanStructure(ServerLevel level, ServerPlayer player) {
        Map<?, ?> structures = level.structureManager()
                .getAllStructuresAt(player.blockPosition());
        if (structures.isEmpty()) {
            return;
        }
        Object structure = structures.keySet().iterator().next();
        String id = idOf(level, structure);
        if (id == null || !id.startsWith("minecraft:")) {
            return;   // 模组自己的遗迹另有主题系统
        }
        int cx = player.getBlockX() >> 4;
        int cz = player.getBlockZ() >> 4;
        Theme theme = roll(level.getSeed(), "building:" + id, cx, cz);
        if (theme == null) {
            return;
        }
        YishuData data = YishuData.of(level);
        String key = "theme-seen:" + player.getGameProfile().name() + ":" + id + ":" + cx + ":" + cz;
        if (data.get(key) != null) {
            return;
        }
        data.put(key, theme.name());

        // 发现提示：这是一处被主题化的原版建筑
        String path = id.substring("minecraft:".length());
        YsText.chat(player, "这座§e" + VANILLA_STRUCTURE_ZH.getOrDefault(path, path)
                + "§f带着 §b" + theme.zh() + "§f 的气息。");
        level.sendParticles(ParticleTypes.END_ROD,
                player.getX(), player.getY() + 2.0D, player.getZ(),
                24, 1.4D, 0.8D, 1.4D, 0.03D);
    }

    private static String idOf(ServerLevel level, Object structure) {
        try {
            var registry = level.registryAccess().lookupOrThrow(Registries.STRUCTURE);
            if (structure instanceof net.minecraft.world.level.levelgen.structure.Structure s) {
                var key = registry.getKey(s);
                return key == null ? null : key.toString();
            }
        } catch (RuntimeException ignored) {
        }
        return null;
    }

    /** Debug helper: describe the current theme system in one line. */
    public static String describe() {
        return "主题变种（命理 / 风水 / 六爻）出现概率 " + THEME_CHANCE + "%；"
                + "作用于原版建筑、村民与流浪商人（主题商品走数据包注入职业交易池）。";
    }

    /** 原版结构的中文名（26.2 没有提供结构名的本地化键，这里手工映射常用项）。 */
    private static final java.util.Map<String, String> VANILLA_STRUCTURE_ZH = java.util.Map.ofEntries(
            java.util.Map.entry("pillager_outpost", "掠夺者前哨站"),
            java.util.Map.entry("woodland_mansion", "林地府邸"),
            java.util.Map.entry("ocean_monument", "海底神殿"),
            java.util.Map.entry("desert_pyramid", "沙漠神殿"),
            java.util.Map.entry("jungle_pyramid", "丛林神庙"),
            java.util.Map.entry("swamp_hut", "沼泽小屋"),
            java.util.Map.entry("igloo", "雪屋"),
            java.util.Map.entry("stronghold", "要塞"),
            java.util.Map.entry("mineshaft", "废弃矿井"),
            java.util.Map.entry("ancient_city", "远古城市"),
            java.util.Map.entry("trial_chambers", "试炼密室"),
            java.util.Map.entry("ruined_portal", "废弃传送门"),
            java.util.Map.entry("shipwreck", "沉船"),
            java.util.Map.entry("ocean_ruin", "海底废墟"),
            java.util.Map.entry("buried_treasure", "埋藏的宝藏"),
            java.util.Map.entry("nether_fortress", "下界要塞"),
            java.util.Map.entry("bastion_remnant", "堡垒遗迹"),
            java.util.Map.entry("end_city", "末地城"),
            java.util.Map.entry("trail_ruins", "古迹废墟"),
            java.util.Map.entry("desert_well", "沙漠水井"),
            java.util.Map.entry("village_plains", "平原村庄"),
            java.util.Map.entry("village_desert", "沙漠村庄"),
            java.util.Map.entry("village_savanna", "热带草原村庄"),
            java.util.Map.entry("village_snowy", "雪原村庄"),
            java.util.Map.entry("village_taiga", "针叶林村庄"));

    /** Ensure the class is loadable even before first tick. */
    public static void bootstrap() {
        // no-op; keeps Component import honest for translated senders
        Component.empty();
    }
}
