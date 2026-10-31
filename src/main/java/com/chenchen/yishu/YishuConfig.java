package com.chenchen.yishu;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 易枢 · 配置。
 *
 * <p>纯 JSON，落在 {@code config/yishu.json}，第一次启动自动生成。
 * 之所以不用 Cloth Config 之类的依赖：配置项本身就是模组设计的一部分，
 * 而且这些开关（风水、龙脉、太岁、六爻次数……）最好能被服务器管理员直接看懂、直接改。
 *
 * <p>配置写入后<b>不会</b>影响「同种子同结果」这条铁律 —— 它改的是「哪些系统生效」，
 * 不是「算出什么数」。把风水关了，卦还是同一卦；把随机性等级调高，命盘还是同一张命盘。
 * 这两类东西必须严格分开，否则「可复现」就成了空话。
 */
public final class YishuConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static YishuConfig instance;

    // ================================================================ 命理

    /** 随机性等级。 */
    public RandomnessLevel randomness = RandomnessLevel.MEDIUM;

    /** 是否允许玩家在命盘界面里输入自己的真实生辰。 */
    public boolean allowBirthInput = true;

    /** 命盘可见性。 */
    public Visibility fateVisibility = Visibility.SELF;

    /** 新手引导与渐进式解锁总开关。关掉 = 所有内容一开始全部开放（老玩家/服务器用）。 */
    public boolean tutorialEnabled = true;

    /** 专业模式的默认值。玩家可以在游戏里自己切换（需要第 29 天解锁）。 */
    public boolean professionalModeDefault = false;

    /** 是否在吉地/凶地显示地面粒子（第一层直感）。 */
    public boolean groundParticles = true;

    /** 是否在屏幕边缘按所在地吉凶显示颜色边框（第一层直感）。 */
    public boolean ambienceBorder = true;

    // ================================================================ 风水

    public boolean fengShuiEnabled = true;
    public boolean dragonVeinEnabled = true;
    public boolean taiSuiEnabled = true;
    public boolean dailyAlmanacEnabled = true;

    // ================================================================ 六爻

    /** 每日可问卦的次数上限。 */
    public int divinationDailyLimit = 3;

    /** 超过次数是否触发反噬。 */
    public boolean backlashEnabled = true;

    /** 是否允许「外应起卦」（用玩家所处的环境作为卦的输入）。 */
    public boolean externalOmenEnabled = true;

    // ================================================================ 内容

    public boolean mobFateEnabled = true;
    public boolean buildingFateEnabled = true;
    public boolean ruinsEnabled = true;

    // ================================================================ 维度

    /** 每个维度独立配置。 */
    public DimensionConfig overworld = new DimensionConfig(true, true, true);
    public DimensionConfig nether = new DimensionConfig(true, true, true);
    public DimensionConfig end = new DimensionConfig(true, true, true);

    // ================================================================ 枚举

    /**
     * 随机性等级。
     *
     * <p>注意它调节的是<b>解读的模糊度</b>，不是「随机的强弱」——
     * 本模组没有任何东西是随机的。等级越高，卦象给出的线索越含糊、风水评分越保守，
     * 玩家的探索空间越大。
     */
    public enum RandomnessLevel {
        LOW("低", 0.10D, "线索清晰，方位具体，适合先熟悉系统"),
        MEDIUM("中", 0.30D, "默认档位，线索留有余地"),
        HIGH("高", 0.55D, "线索大幅模糊，需要自己参详"),
        CHAOS("混沌", 0.80D, "只给一个大致方向，其余全靠走");

        private final String zh;
        private final double fuzz;
        private final String note;

        RandomnessLevel(String zh, double fuzz, String note) {
            this.zh = zh;
            this.fuzz = fuzz;
            this.note = note;
        }

        public String zh() {
            return this.zh;
        }

        /** 模糊度 0~1，越高线索越含糊。 */
        public double fuzz() {
            return this.fuzz;
        }

        public String note() {
            return this.note;
        }
    }

    /** 命盘可见性。 */
    public enum Visibility {
        SELF("仅自己", "只有自己能看自己的命盘"),
        AUTHORIZED("授权可见", "可以用符箓把自己的命盘授权给他人查看"),
        PUBLIC("公开", "任何人都能用窥命符看到你的命盘摘要");

        private final String zh;
        private final String note;

        Visibility(String zh, String note) {
            this.zh = zh;
            this.note = note;
        }

        public String zh() {
            return this.zh;
        }

        public String note() {
            return this.note;
        }
    }

    /** 单维度开关。 */
    public static final class DimensionConfig {
        public boolean fate = true;
        public boolean fengShui = true;
        public boolean liuYao = true;

        public DimensionConfig() {
        }

        public DimensionConfig(boolean fate, boolean fengShui, boolean liuYao) {
            this.fate = fate;
            this.fengShui = fengShui;
            this.liuYao = liuYao;
        }
    }

    // ================================================================ 读写

    public static YishuConfig get() {
        if (instance == null) {
            instance = load();
        }
        return instance;
    }

    private static Path path() {
        return FabricLoader.getInstance().getConfigDir().resolve("yishu.json");
    }

    private static YishuConfig load() {
        Path file = path();
        if (Files.exists(file)) {
            try {
                String json = Files.readString(file, StandardCharsets.UTF_8);
                YishuConfig loaded = GSON.fromJson(json, YishuConfig.class);
                if (loaded != null) {
                    loaded.sanitize();
                    return loaded;
                }
            } catch (IOException | RuntimeException exception) {
                YishuMod.LOGGER.warn("[Yishu] 配置读取失败，改用默认值：{}", exception.toString());
            }
        }
        YishuConfig fresh = new YishuConfig();
        fresh.save();
        return fresh;
    }

    public void save() {
        try {
            Path file = path();
            Files.createDirectories(file.getParent());
            Files.writeString(file, GSON.toJson(this), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            YishuMod.LOGGER.warn("[Yishu] 配置写入失败：{}", exception.toString());
        }
    }

    /** 把越界的值夹回来，避免手改配置文件把游戏搞崩。 */
    private void sanitize() {
        if (this.randomness == null) {
            this.randomness = RandomnessLevel.MEDIUM;
        }
        if (this.fateVisibility == null) {
            this.fateVisibility = Visibility.SELF;
        }
        this.divinationDailyLimit = Math.max(0, Math.min(64, this.divinationDailyLimit));
        if (this.overworld == null) {
            this.overworld = new DimensionConfig();
        }
        if (this.nether == null) {
            this.nether = new DimensionConfig();
        }
        if (this.end == null) {
            this.end = new DimensionConfig();
        }
    }

    /** 按维度键取子配置。 */
    public DimensionConfig forDimension(String dimensionKey) {
        return switch (dimensionKey) {
            case "the_nether" -> this.nether;
            case "the_end" -> this.end;
            default -> this.overworld;
        };
    }
}
