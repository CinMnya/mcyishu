package com.chenchen.yishu.divination;

import com.chenchen.yishu.dimension.DimensionProfile;
import com.chenchen.yishu.fate.Bazi;
import com.chenchen.yishu.fate.FateProfile;
import com.chenchen.yishu.fate.GanZhi.Pair;
import com.chenchen.yishu.fate.Yun;
import com.chenchen.yishu.liuren.LiuRen;
import com.chenchen.yishu.qimen.QiMen;
import com.chenchen.yishu.ziwei.ZiWei;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * 三大术数的统一入口 —— 紫微斗数、大六壬、奇门遁甲。
 *
 * <p>把「起盘」和「给效果」放在一起，是因为它们本来就是一件事：
 * 术数在游戏里要有用，就不能只吐一堆文字 —— 它得推着玩家做点什么。
 * 所以每一起盘都配一组状态效果，效果的方向由盘面自己决定：
 * <ul>
 *   <li><b>紫微</b>看「哪一宫最盛」—— 格局强给抗性、财帛强给幸运、官禄强给力量、迁移强给速度。</li>
 *   <li><b>六壬</b>看「三传吉凶」—— 课体通顺给恢复与幸运，滞涩则只延长冷却，不惩罚。</li>
 *   <li><b>奇门</b>看「三吉门」—— 生门给恢复、开门给幸运、休门给抗性。</li>
 * </ul>
 *
 * <p>三条铁律：
 * <ol>
 *   <li><b>同一个人、同一个时辰，结果完全一致</b> —— 全部由世界种子 + 名号 + 时间派生。</li>
 *   <li><b>只在服务端算</b> —— 客户端拿到的永远是服务端算好的文本。</li>
 *   <li><b>不惩罚过频</b> —— 用多了只会冷却变长，不会给负面效果。</li>
 * </ol>
 */
public final class Oracle {

    private Oracle() {
    }

    /** 三门术数。 */
    public enum Kind {
        ZIWEI("紫微斗数", "紫微命盘", "推命之盘。看的是「你是谁」。"),
        LIUREN("大六壬", "六壬式盘", "占事之盘。看的是「事怎么走」。"),
        QIMEN("奇门遁甲", "奇门局盘", "择时择方之盘。看的是「往哪走」。");

        private final String zh;
        private final String itemName;
        private final String note;

        Kind(String zh, String itemName, String note) {
            this.zh = zh;
            this.itemName = itemName;
            this.note = note;
        }

        public String zh() {
            return this.zh;
        }

        public String itemName() {
            return this.itemName;
        }

        public String note() {
            return this.note;
        }
    }

    /**
     * 一次解读。
     *
     * @param kind     术数
     * @param title    面板标题
     * @param lines    面板正文
     * @param effects  随盘附赠的状态效果（服务端构造，调用方负责施加）
     * @param summary  一句话摘要
     * @param cooldown 建议冷却（刻）
     */
    public record Reading(Kind kind, String title, List<String> lines,
                          List<MobEffectInstance> effects, String summary, int cooldown) {
    }

    /**
     * 起一盘。
     *
     * @param kind   术数
     * @param level  服务端世界
     * @param player 求测的玩家
     */
    public static Reading perform(Kind kind, ServerLevel level, Player player) {
        long seed = level.getSeed();
        String owner = player.getGameProfile().name();
        long clock = level.getOverworldClockTime();
        long epochDay = clock / 24000L;
        DimensionProfile dimension = DimensionProfile.of(level.dimension().identifier().getPath());
        int hour24 = (int) (Math.floorMod(clock, 24000L) / 1000L);

        return switch (kind) {
            case ZIWEI -> ziwei(seed, owner);
            case LIUREN -> liuren(seed, dimension, epochDay, clock);
            case QIMEN -> qimen(seed, dimension, epochDay, hour24);
        };
    }

    // ==================================================================== 紫微

    private static Reading ziwei(long seed, String owner) {
        Bazi bazi = FateProfile.of(seed, owner).active();
        ZiWei.Chart chart = ZiWei.cast(bazi);
        List<String> lines = ZiWei.render(chart);

        ZiWei.Strength strength = ZiWei.strength(chart);
        List<MobEffectInstance> effects = new ArrayList<>();
        String best = strength.best();
        int duration = 2400;
        switch (best) {
            case "格局" -> effects.add(new MobEffectInstance(MobEffects.RESISTANCE, duration, 1, true, false));
            case "财帛" -> effects.add(new MobEffectInstance(MobEffects.LUCK, duration, 0, true, false));
            case "官禄" -> effects.add(new MobEffectInstance(MobEffects.STRENGTH, duration, 1, true, false));
            default -> effects.add(new MobEffectInstance(MobEffects.SPEED, duration, 1, true, false));
        }
        lines.add("");
        lines.add("§7盘面最盛在「" + best + "」，已随盘附赠一分气运。");
        return new Reading(Kind.ZIWEI, "紫微命盘", lines, effects, ZiWei.summary(chart), 1200);
    }

    // ==================================================================== 六壬

    private static Reading liuren(long seed, DimensionProfile dimension, long epochDay, long clock) {
        Pair dayPillar = Yun.liuRi(seed, dimension.shortKey(), epochDay);
        Pair monthPillar = Yun.liuYue(seed, dimension.shortKey(), epochDay);
        int hourBranch = Math.floorMod(dimension.hourIndex(clock), 12);
        int monthBranch = monthPillar.zhi().ordinal();

        LiuRen.Reading reading = LiuRen.cast(dayPillar, hourBranch, monthBranch);
        List<String> lines = LiuRen.render(reading);

        List<MobEffectInstance> effects = new ArrayList<>();
        int cooldown = 1200;
        if (reading.score() >= 4) {
            effects.add(new MobEffectInstance(MobEffects.LUCK, 2400, 1, true, false));
            effects.add(new MobEffectInstance(MobEffects.REGENERATION, 600, 0, true, false));
            lines.add("");
            lines.add("§7三传通顺，气随事走 —— 附赠一分运气。");
        } else if (reading.score() >= 0) {
            effects.add(new MobEffectInstance(MobEffects.REGENERATION, 600, 0, true, false));
            lines.add("");
            lines.add("§7课体平平，尚可一用。");
        } else {
            cooldown = 3600;
            lines.add("");
            lines.add("§7课体滞涩 —— 此盘不再添气，且需静待时辰流转（冷却延长）。");
        }
        return new Reading(Kind.LIUREN, "六壬课盘", lines, effects, LiuRen.summary(reading), cooldown);
    }

    // ==================================================================== 奇门

    private static Reading qimen(long seed, DimensionProfile dimension, long epochDay, int hour24) {
        Pair dayPillar = Yun.liuRi(seed, dimension.shortKey(), epochDay);
        QiMen.Layout layout = QiMen.cast(epochDay, dayPillar, hour24);
        List<String> lines = QiMen.render(layout);

        List<MobEffectInstance> effects = new ArrayList<>();
        QiMen.Cell best = layout.best();
        QiMen.Gate gate = best.gate();
        int duration = 2400;
        if (gate == QiMen.Gate.SHENG) {
            effects.add(new MobEffectInstance(MobEffects.REGENERATION, 1200, 1, true, false));
            effects.add(new MobEffectInstance(MobEffects.SATURATION, 200, 0, true, false));
        } else if (gate == QiMen.Gate.KAI) {
            effects.add(new MobEffectInstance(MobEffects.LUCK, duration, 1, true, false));
        } else if (gate == QiMen.Gate.XIU) {
            effects.add(new MobEffectInstance(MobEffects.RESISTANCE, duration, 1, true, false));
        } else {
            effects.add(new MobEffectInstance(MobEffects.SPEED, duration, 0, true, false));
        }
        lines.add("");
        lines.add("§7依「" + best.direction() + "」方行，可得一分助力。");
        return new Reading(Kind.QIMEN, "奇门局盘", lines, effects, QiMen.summary(layout), 1200);
    }

    /** 把效果施加到玩家身上。 */
    public static void apply(Player player, Reading reading) {
        for (MobEffectInstance effect : reading.effects()) {
            player.addEffect(new MobEffectInstance(effect));
        }
    }
}
