package com.chenchen.yishu.system;

import com.chenchen.yishu.calendar.YishuCalendar;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;

/**
 * 易枢 · 节气事件（第 20 轮）。
 *
 * <p>二十四节气，每 15 日一气（{@link YishuCalendar} 定历）。
 * 换气那一刻向全服播报；一气之内，人人得此气的小加持 ——
 * 立春生发、夏至阳极、冬至阴极，各有各的味道。
 *
 * <p>「四立」与「二分二至」是大节点，播报更隆重、加持更强。
 */
public final class SolarTermSystem {

    private SolarTermSystem() {
    }

    /** 检查间隔（刻）。 */
    private static final int INTERVAL = 100;
    /** 效果刷新间隔（刻）。 */
    private static final int EFFECT_INTERVAL = 1200;

    /** 上次播报过的节气。 */
    private static final Map<String, Integer> LAST_TERM = new HashMap<>();

    public static void register() {
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_LEVEL_TICK
                .register(SolarTermSystem::tickLevel);
    }

    /** 一气的加持：效果 + 说明。 */
    private record TermEffect(net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> effect,
                              int amplifier, String zh) {
    }

    private static TermEffect termEffect(int term) {
        return switch (term) {
            case 0 -> new TermEffect(MobEffects.REGENERATION, 0, "立春 · 万物生发，气血自复");
            case 1 -> new TermEffect(MobEffects.WATER_BREATHING, 0, "雨水 · 水气滋润，水底可行");
            case 2 -> new TermEffect(MobEffects.SPEED, 0, "惊蛰 · 雷动虫醒，脚下生风");
            case 3 -> new TermEffect(MobEffects.LUCK, 1, "春分 · 阴阳相半，运道持平而偏吉");
            case 4 -> new TermEffect(MobEffects.NIGHT_VISION, 0, "清明 · 慎终追远，目明可辨幽微");
            case 5 -> new TermEffect(MobEffects.SATURATION, 0, "谷雨 · 百谷得雨，饱腹不饥");
            case 6 -> new TermEffect(MobEffects.HEALTH_BOOST, 0, "立夏 · 长养之气，血气充盈");
            case 7 -> new TermEffect(MobEffects.REGENERATION, 0, "小满 · 将熟未熟，绵绵不绝");
            case 8 -> new TermEffect(MobEffects.HASTE, 0, "芒种 · 忙收忙种，手下生辉");
            case 9 -> new TermEffect(MobEffects.STRENGTH, 0, "夏至 · 阳极之至，气力正盛");
            case 10 -> new TermEffect(MobEffects.FIRE_RESISTANCE, 0, "小暑 · 暑气蒸人，炉火不侵");
            case 11 -> new TermEffect(MobEffects.FIRE_RESISTANCE, 1, "大暑 · 燥热至极，烈焰难伤");
            case 12 -> new TermEffect(MobEffects.RESISTANCE, 0, "立秋 · 肃杀之气始起，皮糙肉厚");
            case 13 -> new TermEffect(MobEffects.ABSORPTION, 0, "处暑 · 暑气敛藏，身披余荫");
            case 14 -> new TermEffect(MobEffects.INVISIBILITY, 0, "白露 · 露凝而白，形迹渐藏");
            case 15 -> new TermEffect(MobEffects.LUCK, 1, "秋分 · 昼夜均分，收获正当其时");
            case 16 -> new TermEffect(MobEffects.SLOW_FALLING, 0, "寒露 · 露寒而降，身轻如叶");
            case 17 -> new TermEffect(MobEffects.RESISTANCE, 0, "霜降 · 霜肃杀降，皮糙肉厚");
            case 18 -> new TermEffect(MobEffects.ABSORPTION, 1, "立冬 · 藏养之气，身负余甲");
            case 19 -> new TermEffect(MobEffects.REGENERATION, 0, "小雪 · 阴敛阳藏，气息自养");
            case 20 -> new TermEffect(MobEffects.NIGHT_VISION, 1, "大雪 · 雪夜反明，暗处如昼");
            case 21 -> new TermEffect(MobEffects.NIGHT_VISION, 0, "冬至 · 阴极阳生，暗夜有光");
            case 22 -> new TermEffect(MobEffects.ABSORPTION, 0, "小寒 · 寒气尚浅，微甲护身");
            default -> new TermEffect(MobEffects.RESISTANCE, 1, "大寒 · 岁末极寒，坚甲不摧");
        };
    }

    private static void tickLevel(ServerLevel level) {
        // 只在主世界播报与施效，避免七界各来一遍
        if (!level.dimension().equals(Level.OVERWORLD)) {
            return;
        }
        if (level.getLevelData().getGameTime() % INTERVAL != 0L) {
            return;
        }
        YishuCalendar.Moment moment = YishuCalendar.now(level);
        int term = moment.solarTerm();

        // 换气播报
        for (ServerPlayer player : level.players()) {
            String name = player.getGameProfile().name();
            Integer last = LAST_TERM.get(name);
            if (last == null || last != term) {
                LAST_TERM.put(name, term);
                boolean major = moment.isSeasonStart() || moment.isSolsticeOrEquinox();
                player.sendSystemMessage(Component.literal(
                        (major ? "§6【易枢·节气】§e" : "§b【易枢·节气】§f")
                                + moment.solarTermText() + "已至 §7—— "
                                + termEffect(term).zh()));
            }
        }

        // 一气之内的加持
        if (level.getLevelData().getGameTime() % EFFECT_INTERVAL != 0L) {
            return;
        }
        TermEffect effect = termEffect(term);
        int duration = majorTerm(term) ? EFFECT_INTERVAL * 3 : EFFECT_INTERVAL * 2;
        for (ServerPlayer player : level.players()) {
            if (player.isSpectator() || !player.isAlive()) {
                continue;
            }
            player.addEffect(new MobEffectInstance(effect.effect(), duration,
                    effect.amplifier(), true, false, true));
        }
    }

    private static boolean majorTerm(int term) {
        return term % 6 == 0 || term == 3 || term == 9 || term == 15 || term == 21;
    }
}
