package com.chenchen.yishu.client;

import com.chenchen.yishu.YishuMod;
import com.chenchen.yishu.spirit.YsMobs;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.renderer.entity.AllayRenderer;
import net.minecraft.client.renderer.entity.BatRenderer;
import net.minecraft.client.renderer.entity.EndermanRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.FoxRenderer;
import net.minecraft.client.renderer.entity.GoatRenderer;
import net.minecraft.client.renderer.entity.IronGolemRenderer;
import net.minecraft.client.renderer.entity.PhantomRenderer;
import net.minecraft.client.renderer.entity.RabbitRenderer;
import net.minecraft.client.renderer.entity.SlimeRenderer;
import net.minecraft.client.renderer.entity.TurtleRenderer;
import net.minecraft.client.renderer.entity.VexRenderer;
import net.minecraft.client.renderer.entity.WolfRenderer;
import net.minecraft.client.renderer.entity.state.AllayRenderState;
import net.minecraft.client.renderer.entity.state.BatRenderState;
import net.minecraft.client.renderer.entity.state.EndermanRenderState;
import net.minecraft.client.renderer.entity.state.FoxRenderState;
import net.minecraft.client.renderer.entity.state.GoatRenderState;
import net.minecraft.client.renderer.entity.state.IronGolemRenderState;
import net.minecraft.client.renderer.entity.state.PhantomRenderState;
import net.minecraft.client.renderer.entity.state.RabbitRenderState;
import net.minecraft.client.renderer.entity.state.SlimeRenderState;
import net.minecraft.client.renderer.entity.state.TurtleRenderState;
import net.minecraft.client.renderer.entity.state.VexRenderState;
import net.minecraft.client.renderer.entity.state.WolfRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.animal.allay.Allay;
import net.minecraft.world.entity.animal.fox.Fox;
import net.minecraft.world.entity.animal.goat.Goat;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.animal.rabbit.Rabbit;
import net.minecraft.world.entity.animal.turtle.Turtle;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.entity.monster.cubemob.Slime;

/**
 * 易枢 · 灵兽渲染。
 *
 * <p>十二种灵兽全部复用原版模型（恼鬼 / 悦灵 / 铁傀儡 / 末影人 / 狐狸 / 史莱姆 /
 * 兔子 / 山羊 / 海龟 / 狼 / 蝙蝠 / 幻翼），只把贴图换成易枢自己的换色版，
 * 所以渲染器只需要继承原版渲染器、覆写 {@code getTextureLocation}。
 *
 * <p>注册时把泛型参数给到<b>基类</b>（如 {@code EntityRendererRegistry.<Fox>register}）——
 * 渲染表在运行时是按「精确的 EntityType 实例」索引的，而我们的灵兽本来就是
 * 对应原版类的子类，实体类型擦除之后一切正常。
 * 这是复用原版渲染管线的最短路径：原版动画一升级，我们跟着升。
 */
public final class YsEntityRenderers {

    private YsEntityRenderers() {
    }

    public static void register() {
        // —— 三位前辈 ——
        EntityRendererRegistry.<Vex>register(YsMobs.FATE_WISP, FateWispRenderer::new);
        EntityRendererRegistry.<Allay>register(YsMobs.JADE_SPIRIT, JadeSpiritRenderer::new);
        EntityRendererRegistry.<IronGolem>register(YsMobs.STONE_GUARDIAN, StoneGuardianRenderer::new);
        // —— 第 10 轮九灵兽 ——
        EntityRendererRegistry.<EnderMan>register(YsMobs.STAR_WATCHER, StarWatcherRenderer::new);
        EntityRendererRegistry.<Fox>register(YsMobs.STAR_FOX, StarFoxRenderer::new);
        EntityRendererRegistry.<Slime>register(YsMobs.STAR_SLIME, StarSlimeRenderer::new);
        EntityRendererRegistry.<Rabbit>register(YsMobs.JADE_RABBIT, JadeRabbitRenderer::new);
        EntityRendererRegistry.<Goat>register(YsMobs.JADE_GOAT, JadeGoatRenderer::new);
        EntityRendererRegistry.<Turtle>register(YsMobs.VEIN_TURTLE, VeinTurtleRenderer::new);
        EntityRendererRegistry.<Wolf>register(YsMobs.INK_WOLF, InkWolfRenderer::new);
        EntityRendererRegistry.<Bat>register(YsMobs.INK_BAT, InkBatRenderer::new);
        EntityRendererRegistry.<Phantom>register(YsMobs.HEX_PHANTOM, HexPhantomRenderer::new);

        // —— 第 16 轮：三术界十二灵兽 ——
        EntityRendererRegistry.<EnderMan>register(YsMobs.ZIWEI_STAR, ZiweiStarRenderer::new);
        EntityRendererRegistry.<Wolf>register(YsMobs.DOU_BEAST, DouBeastRenderer::new);
        EntityRendererRegistry.<Fox>register(YsMobs.TIANJI_FOX, TianjiFoxRenderer::new);
        EntityRendererRegistry.<Vex>register(YsMobs.ZIWEI_MOTH, ZiweiMothRenderer::new);
        EntityRendererRegistry.<Turtle>register(YsMobs.REN_SERPENT, RenSerpentRenderer::new);
        EntityRendererRegistry.<Allay>register(YsMobs.GUI_CRANE, GuiCraneRenderer::new);
        EntityRendererRegistry.<Wolf>register(YsMobs.SHE_HOUND, SheHoundRenderer::new);
        EntityRendererRegistry.<Turtle>register(YsMobs.XUAN_TURTLE, XuanTurtleRenderer::new);
        EntityRendererRegistry.<IronGolem>register(YsMobs.MEN_GUARD, MenGuardRenderer::new);
        EntityRendererRegistry.<Fox>register(YsMobs.XING_FOX, XingFoxRenderer::new);
        EntityRendererRegistry.<Rabbit>register(YsMobs.DUN_RAT, DunRatRenderer::new);
        EntityRendererRegistry.<Bat>register(YsMobs.JIA_BIRD, JiaBirdRenderer::new);
    }


    // ==================================================================== 三术界（第 16 轮）

    private static final class ZiweiStarRenderer extends EndermanRenderer {

        private static final Identifier TEXTURE = YishuMod.id("textures/entity/ziwei_star.png");

        ZiweiStarRenderer(EntityRendererProvider.Context context) {
            super(context);
        }

        @Override
        public Identifier getTextureLocation(EndermanRenderState state) {
            return TEXTURE;
        }
    }

    private static final class DouBeastRenderer extends WolfRenderer {

        private static final Identifier TEXTURE = YishuMod.id("textures/entity/dou_beast.png");

        DouBeastRenderer(EntityRendererProvider.Context context) {
            super(context);
        }

        @Override
        public Identifier getTextureLocation(WolfRenderState state) {
            return TEXTURE;
        }
    }

    private static final class TianjiFoxRenderer extends FoxRenderer {

        private static final Identifier TEXTURE = YishuMod.id("textures/entity/tianji_fox.png");

        TianjiFoxRenderer(EntityRendererProvider.Context context) {
            super(context);
        }

        @Override
        public Identifier getTextureLocation(FoxRenderState state) {
            return TEXTURE;
        }
    }

    private static final class ZiweiMothRenderer extends VexRenderer {

        private static final Identifier TEXTURE = YishuMod.id("textures/entity/ziwei_moth.png");

        ZiweiMothRenderer(EntityRendererProvider.Context context) {
            super(context);
        }

        @Override
        public Identifier getTextureLocation(VexRenderState state) {
            return TEXTURE;
        }
    }

    private static final class RenSerpentRenderer extends TurtleRenderer {

        private static final Identifier TEXTURE = YishuMod.id("textures/entity/ren_serpent.png");

        RenSerpentRenderer(EntityRendererProvider.Context context) {
            super(context);
        }

        @Override
        public Identifier getTextureLocation(TurtleRenderState state) {
            return TEXTURE;
        }
    }

    private static final class GuiCraneRenderer extends AllayRenderer {

        private static final Identifier TEXTURE = YishuMod.id("textures/entity/gui_crane.png");

        GuiCraneRenderer(EntityRendererProvider.Context context) {
            super(context);
        }

        @Override
        public Identifier getTextureLocation(AllayRenderState state) {
            return TEXTURE;
        }
    }

    private static final class SheHoundRenderer extends WolfRenderer {

        private static final Identifier TEXTURE = YishuMod.id("textures/entity/she_hound.png");

        SheHoundRenderer(EntityRendererProvider.Context context) {
            super(context);
        }

        @Override
        public Identifier getTextureLocation(WolfRenderState state) {
            return TEXTURE;
        }
    }

    private static final class XuanTurtleRenderer extends TurtleRenderer {

        private static final Identifier TEXTURE = YishuMod.id("textures/entity/xuan_turtle.png");

        XuanTurtleRenderer(EntityRendererProvider.Context context) {
            super(context);
        }

        @Override
        public Identifier getTextureLocation(TurtleRenderState state) {
            return TEXTURE;
        }
    }

    private static final class MenGuardRenderer extends IronGolemRenderer {

        private static final Identifier TEXTURE = YishuMod.id("textures/entity/men_guard.png");

        MenGuardRenderer(EntityRendererProvider.Context context) {
            super(context);
        }

        @Override
        public Identifier getTextureLocation(IronGolemRenderState state) {
            return TEXTURE;
        }
    }

    private static final class XingFoxRenderer extends FoxRenderer {

        private static final Identifier TEXTURE = YishuMod.id("textures/entity/xing_fox.png");

        XingFoxRenderer(EntityRendererProvider.Context context) {
            super(context);
        }

        @Override
        public Identifier getTextureLocation(FoxRenderState state) {
            return TEXTURE;
        }
    }

    private static final class DunRatRenderer extends RabbitRenderer {

        private static final Identifier TEXTURE = YishuMod.id("textures/entity/dun_rat.png");

        DunRatRenderer(EntityRendererProvider.Context context) {
            super(context);
        }

        @Override
        public Identifier getTextureLocation(RabbitRenderState state) {
            return TEXTURE;
        }
    }

    private static final class JiaBirdRenderer extends BatRenderer {

        private static final Identifier TEXTURE = YishuMod.id("textures/entity/jia_bird.png");

        JiaBirdRenderer(EntityRendererProvider.Context context) {
            super(context);
        }

        @Override
        public Identifier getTextureLocation(BatRenderState state) {
            return TEXTURE;
        }
    }

    // ==================================================================== 命理界

    private static final class FateWispRenderer extends VexRenderer {

        private static final Identifier TEXTURE = YishuMod.id("textures/entity/fate_wisp.png");

        FateWispRenderer(EntityRendererProvider.Context context) {
            super(context);
        }

        @Override
        public Identifier getTextureLocation(VexRenderState state) {
            return TEXTURE;
        }
    }

    private static final class StarWatcherRenderer extends EndermanRenderer {

        private static final Identifier TEXTURE = YishuMod.id("textures/entity/star_watcher.png");

        StarWatcherRenderer(EntityRendererProvider.Context context) {
            super(context);
        }

        @Override
        public Identifier getTextureLocation(EndermanRenderState state) {
            return TEXTURE;
        }
    }

    private static final class StarFoxRenderer extends FoxRenderer {

        private static final Identifier TEXTURE = YishuMod.id("textures/entity/star_fox.png");

        StarFoxRenderer(EntityRendererProvider.Context context) {
            super(context);
        }

        @Override
        public Identifier getTextureLocation(FoxRenderState state) {
            return TEXTURE;
        }
    }

    private static final class StarSlimeRenderer extends SlimeRenderer {

        private static final Identifier TEXTURE = YishuMod.id("textures/entity/star_slime.png");

        StarSlimeRenderer(EntityRendererProvider.Context context) {
            super(context);
        }

        @Override
        public Identifier getTextureLocation(SlimeRenderState state) {
            return TEXTURE;
        }
    }

    // ==================================================================== 风水界

    private static final class JadeSpiritRenderer extends AllayRenderer {

        private static final Identifier TEXTURE = YishuMod.id("textures/entity/jade_spirit.png");

        JadeSpiritRenderer(EntityRendererProvider.Context context) {
            super(context);
        }

        @Override
        public Identifier getTextureLocation(AllayRenderState state) {
            return TEXTURE;
        }
    }

    private static final class JadeRabbitRenderer extends RabbitRenderer {

        private static final Identifier TEXTURE = YishuMod.id("textures/entity/jade_rabbit.png");

        JadeRabbitRenderer(EntityRendererProvider.Context context) {
            super(context);
        }

        @Override
        public Identifier getTextureLocation(RabbitRenderState state) {
            return TEXTURE;
        }
    }

    private static final class JadeGoatRenderer extends GoatRenderer {

        private static final Identifier TEXTURE = YishuMod.id("textures/entity/jade_goat.png");

        JadeGoatRenderer(EntityRendererProvider.Context context) {
            super(context);
        }

        @Override
        public Identifier getTextureLocation(GoatRenderState state) {
            return TEXTURE;
        }
    }

    private static final class VeinTurtleRenderer extends TurtleRenderer {

        private static final Identifier TEXTURE = YishuMod.id("textures/entity/vein_turtle.png");

        VeinTurtleRenderer(EntityRendererProvider.Context context) {
            super(context);
        }

        @Override
        public Identifier getTextureLocation(TurtleRenderState state) {
            return TEXTURE;
        }
    }

    // ==================================================================== 六爻界

    private static final class StoneGuardianRenderer extends IronGolemRenderer {

        private static final Identifier TEXTURE = YishuMod.id("textures/entity/stone_guardian.png");

        StoneGuardianRenderer(EntityRendererProvider.Context context) {
            super(context);
        }

        @Override
        public Identifier getTextureLocation(IronGolemRenderState state) {
            return TEXTURE;
        }
    }

    private static final class InkWolfRenderer extends WolfRenderer {

        private static final Identifier TEXTURE = YishuMod.id("textures/entity/ink_wolf.png");

        InkWolfRenderer(EntityRendererProvider.Context context) {
            super(context);
        }

        @Override
        public Identifier getTextureLocation(WolfRenderState state) {
            return TEXTURE;
        }
    }

    private static final class InkBatRenderer extends BatRenderer {

        private static final Identifier TEXTURE = YishuMod.id("textures/entity/ink_bat.png");

        InkBatRenderer(EntityRendererProvider.Context context) {
            super(context);
        }

        @Override
        public Identifier getTextureLocation(BatRenderState state) {
            return TEXTURE;
        }
    }

    private static final class HexPhantomRenderer extends PhantomRenderer {

        private static final Identifier TEXTURE = YishuMod.id("textures/entity/hex_phantom.png");

        HexPhantomRenderer(EntityRendererProvider.Context context) {
            super(context);
        }

        @Override
        public Identifier getTextureLocation(PhantomRenderState state) {
            return TEXTURE;
        }
    }
}
