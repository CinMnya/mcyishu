# -*- coding: utf-8 -*-
"""
易枢 —— 资源生成器
====================================================
一个脚本生成整套模组资源，保证「代码里注册了什么，资源里就有什么」：

  · 像素纹理（PNG，纯标准库写出，不依赖 Pillow）
  · 物品模型    assets/yishu/items/*.json     （1.21.4+ 的新格式）
  · 物品模型    assets/yishu/models/item/*.json
  · 方块模型    assets/yishu/models/block/*.json
  · 方块状态    assets/yishu/blockstates/*.json（含带属性的多方块状态）
  · 语言文件    assets/yishu/lang/zh_cn.json 与 en_us.json
  · 模组图标    assets/yishu/icon.png

设计语言
----------------------------------------------------
模组的视觉母题是「玉 + 墨 + 朱砂」：
    · 玉   #7FBFA8  底色，用于命盘、灵石一类「器物」
    · 墨   #1E2430  暗部，用于罗盘、卦盘的盘面
    · 朱砂 #C0392B  点缀，符箓与朱批
    · 金   #C8A24A  高光，用于阳爻与贵重之物
    · 纸   #E8E0C8  符纸底色
五行本色沿用 GanZhi.WuXing 里定义的那五个值，保证界面与方块是同一套色。
"""
import json
import math
import os
import struct
import zlib

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
NS = "yishu"
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", NS)


# ------------------------------------------------------------------ PNG 编码器


def write_png(path, pixels, width, height):
    """pixels: 行优先的 [(r,g,b,a), ...]，长度 = width*height"""
    raw = bytearray()
    for y in range(height):
        raw.append(0)
        for x in range(width):
            r, g, b, a = pixels[y * width + x]
            raw += bytes((r, g, b, a))

    def chunk(tag, data):
        return (struct.pack(">I", len(data)) + tag + data
                + struct.pack(">I", zlib.crc32(tag + data) & 0xFFFFFFFF))

    blob = b"\x89PNG\r\n\x1a\n"
    blob += chunk(b"IHDR", struct.pack(">IIBBBBB", width, height, 8, 6, 0, 0, 0))
    blob += chunk(b"IDAT", zlib.compress(bytes(raw), 9))
    blob += chunk(b"IEND", b"")
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "wb") as handle:
        handle.write(blob)


class Canvas:
    """16×16 小画布工具。"""

    def __init__(self, size=16, rgba=(0, 0, 0, 0)):
        self.size = size
        self.px = [rgba for _ in range(size * size)]

    def put(self, x, y, color):
        if 0 <= x < self.size and 0 <= y < self.size:
            self.px[y * self.size + x] = color

    def get(self, x, y):
        if 0 <= x < self.size and 0 <= y < self.size:
            return self.px[y * self.size + x]
        return (0, 0, 0, 0)

    def rect(self, x0, y0, x1, y1, color):
        for y in range(y0, y1 + 1):
            for x in range(x0, x1 + 1):
                self.put(x, y, color)

    def frame(self, x0, y0, x1, y1, color):
        for x in range(x0, x1 + 1):
            self.put(x, y0, color)
            self.put(x, y1, color)
        for y in range(y0, y1 + 1):
            self.put(x0, y, color)
            self.put(x1, y, color)

    def hline(self, x0, x1, y, color):
        for x in range(x0, x1 + 1):
            self.put(x, y, color)

    def vline(self, x, y0, y1, color):
        for y in range(y0, y1 + 1):
            self.put(x, y, color)

    def fill_from(self, pattern, mapping):
        for y, row in enumerate(pattern):
            for x, char in enumerate(row):
                if char in mapping:
                    self.put(x, y, mapping[char])

    def mask(self, condition, color):
        for y in range(self.size):
            for x in range(self.size):
                if condition(x, y):
                    self.put(x, y, color)

    def to_pixels(self):
        return [(r, g, b, a) for (r, g, b, a) in self.px]

    def shadow_bottom(self, color, alpha=110):
        """给底部两行压暗，做出轻微的体积感。"""
        for y in range(self.size - 2, self.size):
            for x in range(self.size):
                r, g, b, a = self.get(x, y)
                if a > 0:
                    self.put(x, y, (r * (255 - alpha) // 255,
                                    g * (255 - alpha) // 255,
                                    b * (255 - alpha) // 255, a))


class Anim:
    """竖排多帧动画贴图。

    Minecraft 的动画贴图约定：PNG 按「宽不变、高 = 单帧高 × 帧数」竖排，
    旁边放一个同名 <name>.png.mcmeta 声明 animation。这里只做竖排，
    帧内容交给调用方逐帧画。
    """

    def __init__(self, frames, frametime=4, interpolate=True):
        assert frames, "动画至少需要一帧"
        self.frames = frames
        self.width = frames[0].size
        self.height = frames[0].size * len(frames)
        self.frametime = frametime
        self.interpolate = interpolate

    def to_pixels(self):
        out = []
        for frame in self.frames:
            out.extend(frame.to_pixels())
        return out


def write_texture(path, canvas):
    """写一张贴图。canvas 可以是 Canvas，也可以是 Anim（自动补 .mcmeta）。"""
    if isinstance(canvas, Anim):
        write_png(path, canvas.to_pixels(), canvas.width, canvas.height)
        with open(path + ".mcmeta", "w", encoding="utf-8") as handle:
            json.dump({"animation": {"frametime": canvas.frametime,
                                     "interpolate": canvas.interpolate}},
                      handle, indent=1)
        return
    write_png(path, canvas.to_pixels(), canvas.size, canvas.size)
    # 这张图曾经是动画、现在不是 → 清掉残留的 mcmeta，免得客户端乱读
    stale = path + ".mcmeta"
    if os.path.exists(stale):
        os.remove(stale)


# ------------------------------------------------------------------ 调色板

def rgb(hexcode, alpha=255):
    hexcode = hexcode.lstrip("#")
    return (int(hexcode[0:2], 16), int(hexcode[2:4], 16), int(hexcode[4:6], 16), alpha)


JADE = rgb("7FBFA8")
JADE_DARK = rgb("4E8A78")
JADE_LIGHT = rgb("B0E0CE")
INK = rgb("1E2430")
INK_LIGHT = rgb("38445A")
CINNABAR = rgb("C0392B")
CINNABAR_LIGHT = rgb("E0604A")
GOLD = rgb("C8A24A")
GOLD_LIGHT = rgb("E8C878")
PAPER = rgb("E8E0C8")
PAPER_SHADE = rgb("C4BCA0")
BONE = rgb("D8D0C0")
STONE = rgb("8A8A8A")
STONE_DARK = rgb("5A5A5A")
WOOD = rgb("8A5A34")
WOOD_DARK = rgb("5A3A20")
COPPER = rgb("B07040")
COPPER_DARK = rgb("7A4A28")

WOOD_COLORS = {
    # species: (原木底/暗/亮, 树叶底/暗/亮)
    "astral_wood":  (rgb("6B4E9C"), rgb("463370"), rgb("8F72C8"),
                     rgb("7A62C8"), rgb("5A46A0"), rgb("A490E8")),
    "meteor_wood":  (rgb("4C4C86"), rgb("32325C"), rgb("6E6EB0"),
                     rgb("5C6EC0"), rgb("3E4C96"), rgb("8898E0")),
    "moon_wood":    (rgb("9898C2"), rgb("6E6E96"), rgb("C4C4E4"),
                     rgb("B0B8E0"), rgb("8890C0"), rgb("D8DCF4")),
    "jade_wood":    (rgb("5FA88C"), rgb("3C7460"), rgb("8AC8A8"),
                     rgb("58C08C"), rgb("389868"), rgb("8AE0B4")),
    "cloud_wood":   (rgb("4E8C7A"), rgb("326056"), rgb("78B8A2"),
                     rgb("64C8A8"), rgb("3E9C80"), rgb("9CE4C8")),
    "peach_wood":   (rgb("C88CA8"), rgb("9A647C"), rgb("E8B8CC"),
                     rgb("E8A8C8"), rgb("BC7CA4"), rgb("F8D0E4")),
    "hexa_wood":    (rgb("8A8A92"), rgb("5C5C66"), rgb("B6B6C2"),
                     rgb("9AA0B4"), rgb("6E7488"), rgb("C6CCE0")),
    "ink_wood":     (rgb("4A4A56"), rgb("2E2E3A"), rgb("6E6E80"),
                     rgb("5A5E74"), rgb("3C4054"), rgb("8488A0")),
    "mystery_wood": (rgb("5A6A8C"), rgb("3C4864"), rgb("8494B4"),
                     rgb("6E80A8"), rgb("4C5C80"), rgb("9CB0D8")),

    # ---- 第 16 轮：三术界九树种 ----
    "ziwei_wood":   (rgb("7A5AB0"), rgb("523C7A"), rgb("A888E0"),
                     rgb("8A6AD8"), rgb("6248A8"), rgb("B8A0F0")),
    "dou_wood":     (rgb("4A5AA0"), rgb("303C70"), rgb("7A8AD0"),
                     rgb("5A6AC8"), rgb("3E4A96"), rgb("8A9AE8")),
    "tianji_wood":  (rgb("A06AA8"), rgb("704878"), rgb("C898D0"),
                     rgb("B87AC0"), rgb("8A4E92"), rgb("D8A0E0")),
    "ren_wood":     (rgb("4A8A98"), rgb("305C68"), rgb("78B8C8"),
                     rgb("58B0C0"), rgb("387C8A"), rgb("8AD8E8")),
    "gui_wood":     (rgb("6AA8C0"), rgb("487890"), rgb("98D8F0"),
                     rgb("7AC8E0"), rgb("5096B0"), rgb("A8E8F8")),
    "she_wood":     (rgb("A85A5A"), rgb("783C3C"), rgb("D08A8A"),
                     rgb("C07070"), rgb("944848"), rgb("E8A0A0")),
    "men_wood":     (rgb("A87A48"), rgb("785430"), rgb("D0A878"),
                     rgb("C09060"), rgb("906A40"), rgb("E8B888")),
    "xing_wood":    (rgb("B0A050"), rgb("807030"), rgb("D8C880"),
                     rgb("C8B060"), rgb("988038"), rgb("E8D898")),
    "dun_wood":     (rgb("A05A40"), rgb("703828"), rgb("C88868"),
                     rgb("B87050"), rgb("884830"), rgb("E89878")),
}

ORE_TEX = {
    "meteor_ore": (rgb("9A9ACC"), rgb("52528A")),
    "star_sand_ore": (rgb("E8D8A0"), rgb("9A8450")),
    "crystal_ore": (rgb("D8B8F0"), rgb("8860B0")),
    "verdant_ore": (rgb("7AD8A8"), rgb("2E8A60")),
    "mica_ore": (rgb("C8E8E0"), rgb("5E9A90")),
    "vein_ore": (rgb("E0F0C8"), rgb("7AA860")),
    "ink_iron_ore": (rgb("B0B0C0"), rgb("4A4A5E")),
    "onyx_ore": (rgb("6E6E82"), rgb("2A2A38")),
    "oracle_ore": (rgb("E8E0C8"), rgb("9A8E6E")),
    # ---- 第 16 轮：三术界十二矿 ----
    "ziwei_crystal_ore": (rgb("D8B8FF"), rgb("7A50B8")),
    "dou_ore": (rgb("9AA8FF"), rgb("4A54A8")),
    "ji_ore": (rgb("F0E0A0"), rgb("9A8448")),
    "yuan_ore": (rgb("F0A8E8"), rgb("9A4A90")),
    "ren_ore": (rgb("8AE0E8"), rgb("3A7A88")),
    "gui_ore": (rgb("B8E8FF"), rgb("5A8AB8")),
    "she_ore": (rgb("F09090"), rgb("963838")),
    "xuan_ore": (rgb("6E6E88"), rgb("2A2A3A")),
    "men_ore": (rgb("F0B070"), rgb("9A5A28")),
    "xing_ore": (rgb("F8E080"), rgb("A08A28")),
    "dun_ore": (rgb("E08060"), rgb("8A4028")),
    "jia_ore": (rgb("D8D8C0"), rgb("8A8A70")),
}

ASTRAL = rgb("6B5AA8")
ASTRAL_D = rgb("463A78")
ASTRAL_L = rgb("A292E8")
JADE_2 = rgb("5FA88C")
JADE_2D = rgb("3E7A66")
JADE_2L = rgb("A8DCC4")
YAO = rgb("8A8A92")
YAO_D = rgb("5A5A64")
YAO_L = rgb("C2C2CE")
YAO_RED = rgb("B05040")

# ---- 第 16 轮：三术界主色 ----
ZW = rgb("6B4E9C")
ZW_D = rgb("453070")
ZW_L = rgb("A888E0")
LR = rgb("3E7E8E")
LR_D = rgb("285866")
LR_L = rgb("7AC8D8")
QM = rgb("9A6A38")
QM_D = rgb("6E4820")
QM_L = rgb("D8A868")

# 遁甲汞（液态金属）——「汞」是水银，本色是冷银白，
# 不能沿用奇门界的土黄主色，否则一桶棕水看不出是水银。
# 星辉露（液态）——露水本色是清亮的淡蓝白。
# 命理界的方块主色是紫，但两种维度的液体都做成紫色就分不清了，
# 这里单独给液体一套「露」的色，不挪用 ASTRAL 系列。
# 星枢引的宝石色：北斗的冷蓝。
# 命理界方块主色是紫，但紫微界的灵引也是紫，两把紫钥匙在物品栏里认不出，
# 所以星枢引单独走「星蓝」，紫微灵引保留紫。
# 壬水引的宝石色：比青玉引更深一档的青蓝，
# 否则玉门与壬水门两把钥匙在物品栏里几乎是同一个青色。
LR_KEY = rgb("2E5E78")
LR_KEY_L = rgb("6AA8C8")

ASTRAL_KEY = rgb("5A7AC8")
ASTRAL_KEY_L = rgb("A8C4F0")

ASTRAL_DEW = rgb("9EC4E8")
ASTRAL_DEW_D = rgb("5E88B8")
ASTRAL_DEW_L = rgb("E4F4FF")

QM_HG = rgb("B4BAC8")
QM_HG_D = rgb("787E8C")
QM_HG_L = rgb("ECF2FA")

ELEMENT_COLORS = {
    "wood": rgb("4C9A4C"),
    "fire": rgb("D0503C"),
    "earth": rgb("B08A40"),
    "metal": rgb("C8C8D0"),
    "water": rgb("3E6FB0"),
}


def shade(color, factor):
    r, g, b, a = color
    return (max(0, min(255, int(r * factor))),
            max(0, min(255, int(g * factor))),
            max(0, min(255, int(b * factor))), a)


# ------------------------------------------------------------------ 爻 / 卦形

def draw_yao(canvas, y, is_yang, color, thickness=2):
    """在指定行画一个爻：阳为整条，阴为断成两截。"""
    if is_yang:
        canvas.hline(4, 11, y, color)
        if thickness > 1:
            canvas.hline(4, 11, y + 1, shade(color, 0.8))
    else:
        canvas.hline(4, 6, y, color)
        canvas.hline(9, 11, y, color)
        if thickness > 1:
            canvas.hline(4, 6, y + 1, shade(color, 0.8))
            canvas.hline(9, 11, y + 1, shade(color, 0.8))


def draw_hexagram(canvas, x0, y0, pattern, color):
    """在画布上画一个六爻卦（自下而上）。"""
    for index, is_yang in enumerate(pattern):
        y = y0 + (5 - index)
        if is_yang:
            canvas.hline(x0, x0 + 2, y, color)
        else:
            canvas.put(x0, y, color)
            canvas.put(x0 + 2, y, color)


# ------------------------------------------------------------------ 物品纹理

def tex_fate_slip():
    """命盘玉简：一块竖立的玉牌，上刻六爻。"""
    c = Canvas()
    c.rect(4, 1, 11, 14, JADE)
    c.frame(4, 1, 11, 14, JADE_DARK)
    c.vline(4, 1, 14, JADE_LIGHT)
    c.hline(4, 11, 1, JADE_LIGHT)
    # 顶部挂孔
    c.rect(7, 2, 8, 3, (0, 0, 0, 0))
    c.frame(7, 2, 8, 3, JADE_DARK)
    # 六爻：自下而上 阳阴阳阴阳阴
    pattern = [True, False, True, False, True, False]
    for index, is_yang in enumerate(pattern):
        y = 12 - index
        if is_yang:
            c.hline(5, 10, y, INK)
        else:
            c.hline(5, 6, y, INK)
            c.hline(9, 10, y, INK)
    c.shadow_bottom(GOLD, 40)
    return c


def tex_compass():
    """风水罗盘：圆盘 + 磁针 + 八卦刻度。"""
    c = Canvas()
    c.mask(lambda x, y: 3 <= (x - 7.5) ** 2 + (y - 7.5) ** 2 <= 42, INK)
    c.mask(lambda x, y: 3 <= (x - 7.5) ** 2 + (y - 7.5) ** 2 <= 30, JADE_DARK)
    c.mask(lambda x, y: (x - 7.5) ** 2 + (y - 7.5) ** 2 <= 10, INK_LIGHT)
    # 八向刻度
    for dx, dy in [(0, -5), (0, 5), (-5, 0), (5, 0), (3, -3), (-3, -3), (3, 3), (-3, 3)]:
        c.put(7 + dx, 7 + dy, GOLD)
    # 磁针：红头指向北（上）
    c.vline(7, 4, 7, CINNABAR)
    c.vline(8, 4, 7, CINNABAR_LIGHT)
    c.vline(7, 8, 11, BONE)
    c.vline(8, 8, 11, STONE)
    c.put(7, 7, GOLD_LIGHT)
    c.put(8, 7, GOLD)
    return c


def tex_dragon_ruler():
    """寻龙尺：一把带龙纹的尺。"""
    c = Canvas()
    c.rect(2, 6, 13, 10, WOOD)
    c.frame(2, 6, 13, 10, WOOD_DARK)
    c.hline(3, 12, 7, shade(WOOD, 1.2))
    # 刻度
    for x in range(3, 13, 2):
        c.put(x, 10, INK)
    # 龙纹（简化成一道波浪 + 一点朱砂）
    c.put(5, 8, JADE)
    c.put(6, 8, JADE_LIGHT)
    c.put(7, 9, JADE)
    c.put(8, 8, JADE_LIGHT)
    c.put(9, 8, JADE)
    c.put(10, 9, JADE)
    c.put(4, 9, CINNABAR)
    return c


def tex_peek_charm():
    """窥命符：一张符纸，上有朱砂眼纹。"""
    c = Canvas()
    c.rect(5, 1, 11, 14, PAPER)
    c.frame(5, 1, 11, 14, PAPER_SHADE)
    # 顶部朱砂印
    c.rect(7, 2, 9, 3, CINNABAR)
    # 眼纹
    c.hline(6, 10, 6, INK)
    c.mask(lambda x, y: (x - 8) ** 2 / 4.0 + (y - 8) ** 2 / 1.6 <= 1, INK)
    c.put(8, 8, CINNABAR_LIGHT)
    # 下部符脚
    c.vline(8, 10, 12, CINNABAR)
    c.put(6, 11, CINNABAR)
    c.put(10, 11, CINNABAR)
    c.put(6, 13, CINNABAR)
    c.put(10, 13, CINNABAR)
    return c


def tex_coins():
    """三枚铜钱：品字排列。"""
    c = Canvas()

    def coin(cx, cy):
        c.mask(lambda x, y, cx=cx, cy=cy: (x - cx) ** 2 + (y - cy) ** 2 <= 10, GOLD)
        c.mask(lambda x, y, cx=cx, cy=cy: (x - cx) ** 2 + (y - cy) ** 2 <= 5, GOLD_LIGHT)
        for dx, dy in [(0, 0), (1, 0), (0, 1), (1, 1)]:
            c.put(cx + dx - (1 if dx else 0), cy + dy - (1 if dy else 0), INK)

    coin(5, 5)
    coin(11, 6)
    coin(8, 12)
    return c


def tex_yarrow():
    """蓍草：一束草茎。"""
    c = Canvas()
    for x, top, color in [(4, 3, JADE_DARK), (6, 1, JADE), (8, 2, JADE_DARK),
                          (10, 4, JADE), (12, 5, JADE_DARK)]:
        c.vline(x, top, 14, color)
        c.put(x, top - 1, shade(color, 1.35))
    # 捆绳
    c.hline(3, 13, 10, CINNABAR)
    c.hline(3, 13, 11, shade(CINNABAR, 0.75))
    return c


def tex_turtle_shell():
    """龟甲：六边形甲壳，带裂纹。"""
    c = Canvas()
    c.mask(lambda x, y: 1 <= (x - 7.5) ** 2 / 40.0 + (y - 8) ** 2 / 46.0 <= 1, BONE)
    c.mask(lambda x, y: (x - 7.5) ** 2 / 40.0 + (y - 8) ** 2 / 46.0 < 0.45, shade(BONE, 0.85))
    # 甲纹
    for dx, dy in [(0, -6), (0, 6), (-5, 0), (5, 0)]:
        c.put(7 + dx, 8 + dy, shade(BONE, 0.6))
    c.mask(lambda x, y: 3 <= (x - 7.5) ** 2 / 12.0 + (y - 8) ** 2 / 8.0 <= 4, INK_LIGHT)
    # 灼烧的裂纹（占卜用）
    c.put(7, 8, INK)
    c.put(6, 7, INK)
    c.put(9, 9, INK)
    return c


def tex_sundial():
    """日晷：底座 + 晷针 + 刻度。"""
    c = Canvas()
    c.mask(lambda x, y: (x - 7.5) ** 2 / 44.0 + (y - 9) ** 2 / 26.0 <= 1, STONE)
    c.mask(lambda x, y: (x - 7.5) ** 2 / 44.0 + (y - 9) ** 2 / 26.0 <= 0.55, shade(STONE, 1.2))
    # 刻度
    for dx, dy in [(-4, 0), (4, 0), (0, -3), (0, 3), (-3, -2), (3, -2), (-3, 2), (3, 2)]:
        c.put(7 + dx, 9 + dy, INK)
    # 晷针
    c.vline(7, 4, 9, GOLD)
    c.put(7, 3, GOLD_LIGHT)
    return c


def tex_talisman(kind):
    """符箓：四种颜色主题的符纸。"""
    c = Canvas()
    c.rect(4, 1, 11, 14, PAPER)
    c.frame(4, 1, 11, 14, PAPER_SHADE)
    accent = {
        "ward": JADE,
        "fortune": GOLD,
        "clarity": rgb("5AA0D0"),
        "taisui": CINNABAR,
    }[kind]
    # 顶部符头
    c.rect(6, 2, 9, 4, accent)
    c.put(7, 3, PAPER)
    c.put(8, 3, PAPER)
    # 中部符胆
    c.vline(7, 6, 12, CINNABAR)
    c.vline(8, 6, 12, CINNABAR)
    c.hline(6, 9, 8, CINNABAR)
    c.hline(6, 9, 11, CINNABAR)
    c.put(5, 9, accent)
    c.put(10, 9, accent)
    # 下部符脚
    c.put(6, 14, CINNABAR)
    c.put(9, 14, CINNABAR)
    return c


def tex_element_stone(kind):
    """五行灵石：菱形宝石，颜色按行。"""
    c = Canvas()
    base = ELEMENT_COLORS[kind]
    c.mask(lambda x, y: abs(x - 7.5) / 6.0 + abs(y - 8) / 6.0 <= 1, base)
    c.mask(lambda x, y: abs(x - 7.5) / 6.0 + abs(y - 8) / 6.0 <= 0.62, shade(base, 1.18))
    c.mask(lambda x, y: abs(x - 7.5) / 6.0 + abs(y - 8) / 6.0 <= 0.28, shade(base, 1.4))
    c.frame(3, 8, 12, 8, shade(base, 0.7))
    c.put(5, 4, (255, 255, 255, 200))
    c.put(6, 5, (255, 255, 255, 140))
    return c


def tex_yinyang_stone():
    """阴阳石：一半白一半黑。"""
    c = Canvas()
    c.mask(lambda x, y: (x - 7.5) ** 2 + (y - 7.5) ** 2 <= 34, BONE)
    for y in range(16):
        for x in range(16):
            if (x - 7.5) ** 2 + (y - 7.5) ** 2 <= 34 and x > 7:
                c.put(x, y, INK)
    c.mask(lambda x, y: (x - 7.5) ** 2 + (y - 7.5) ** 2 <= 8 and y < 7, BONE)
    c.mask(lambda x, y: (x - 7.5) ** 2 + (y - 7.5) ** 2 <= 8 and y > 8, INK)
    c.put(7, 5, INK)
    c.put(8, 10, BONE)
    return c


def tex_shard(kind):
    """碎片类（龙脉核 / 烈焰 / 灵魂 / 虚空 / 紫颂 / 龙息）。"""
    colors = {
        "dragon_core_shard": rgb("5AD0C8"),
        "flame_fragment": rgb("E06030"),
        "soul_core": rgb("7FB0D0"),
        "void_shard": rgb("6A4A9A"),
        "chorus_core": rgb("A070B0"),
        "dragon_breath_core": rgb("C060C0"),
        # 第 15 轮：三术的材料
        "liuren_shell": rgb("C8A878"),
        "qimen_shard": rgb("8A8AA0"),
    }
    base = colors[kind]
    c = Canvas()
    pts = [(6, 1), (9, 1), (12, 4), (11, 9), (8, 14), (4, 11), (2, 6), (5, 3)]
    # 用多边形填充近似
    c.mask(lambda x, y: (2 <= x <= 12 and 1 <= y <= 14
                         and abs(x - 7.5) * 0.75 + abs(y - 7.5) * 0.95 <= 6.2), base)
    c.mask(lambda x, y: (2 <= x <= 12 and 1 <= y <= 14
                         and abs(x - 7.5) * 0.75 + abs(y - 7.5) * 0.95 <= 3.6), shade(base, 1.25))
    c.mask(lambda x, y: (2 <= x <= 12 and 1 <= y <= 14
                         and abs(x - 7.5) * 0.75 + abs(y - 7.5) * 0.95 <= 1.6), shade(base, 1.5))
    for x, y in pts:
        if 0 <= x < 16 and 0 <= y < 16:
            c.put(x, y, shade(base, 1.3))
    return c


def tex_fate_book():
    """命理书：翻开的古书。"""
    c = Canvas()
    # 书脊
    c.rect(6, 2, 9, 14, WOOD_DARK)
    # 左右页
    c.rect(1, 3, 6, 14, PAPER)
    c.rect(9, 3, 14, 14, PAPER)
    c.frame(1, 3, 6, 14, PAPER_SHADE)
    c.frame(9, 3, 14, 14, PAPER_SHADE)
    # 书页上的爻纹
    for y in (5, 6, 7):
        c.hline(2, 5, y, INK_LIGHT)
    for y in (9, 10, 12):
        c.hline(10, 13, y, INK_LIGHT)
    draw_hexagram(c, 11, 4, [True, False, False, True, False, True], CINNABAR)
    return c



def tex_scroll():
    """古卷：半展开的竹纸卷轴，木轴在两端。"""
    c = Canvas()
    # 卷身（右上斜放）
    c.rect(3, 4, 12, 12, PAPER)
    c.frame(3, 4, 12, 12, PAPER_SHADE)
    # 左端木轴
    c.rect(2, 3, 3, 13, WOOD)
    c.vline(2, 3, 13, WOOD_DARK)
    # 右端卷起部分
    c.rect(12, 3, 13, 13, PAPER_SHADE)
    c.vline(13, 3, 13, WOOD)
    # 卷面上墨字
    for y in (6, 7, 9, 10):
        c.hline(5, 9, y, INK_LIGHT)
    c.hline(5, 7, 11, CINNABAR)
    # 印章
    c.rect(9, 10, 10, 11, CINNABAR)
    return c



# ------------------------------------------------------------------ 第 11 轮：丹道与灵物

def tex_pill(base, glow=None):
    """丹丸：一颗圆润的药丸，左上高光、右下暗面。"""
    if glow is None:
        glow = shade(base, 1.45)
    c = Canvas()
    c.mask(lambda x, y: (x - 7.5) ** 2 + (y - 8.0) ** 2 <= 23.0, shade(base, 0.75))
    c.mask(lambda x, y: (x - 7.5) ** 2 + (y - 7.6) ** 2 <= 18.0, base)
    c.mask(lambda x, y: (x - 6.5) ** 2 + (y - 6.5) ** 2 <= 6.2, glow)
    c.mask(lambda x, y: (x - 6.1) ** 2 + (y - 6.1) ** 2 <= 2.2, shade(base, 1.8))
    c.mask(lambda x, y: ((x - 9.4) ** 2 + (y - 9.4) ** 2 <= 10.0
                         and (x - 7.5) ** 2 + (y - 7.6) ** 2 <= 18.0), shade(base, 0.6))
    return c


def tex_paper_sheet():
    """空白符纸：一沓裁好的黄纸，角上压着朱砂印。"""
    c = Canvas()
    c.rect(3, 2, 12, 13, PAPER)
    c.frame(3, 2, 12, 13, PAPER_SHADE)
    c.frame(4, 3, 11, 12, rgb("F0EAD6"))
    c.put(12, 2, rgb("D9D2B8"))
    c.put(12, 3, rgb("D9D2B8"))
    c.rect(9, 10, 11, 12, CINNABAR)
    c.rect(10, 11, 10, 11, rgb("8A2018"))
    return c


def tex_fur(base, tip):
    """绒毛团：蓬松的一球毛。"""
    c = Canvas()
    c.mask(lambda x, y: (x - 7.5) ** 2 + (y - 8.5) ** 2 <= 27.0, shade(base, 0.85))
    c.mask(lambda x, y: (x - 7.5) ** 2 + (y - 7.8) ** 2 <= 17.0, base)
    for x, y in [(3, 5), (12, 4), (2, 9), (13, 9), (5, 13), (10, 13), (7, 2), (8, 3)]:
        c.put(x, y, tip)
        c.put(x + 1, y, shade(tip, 0.88))
    c.mask(lambda x, y: (x - 6.4) ** 2 + (y - 6.6) ** 2 <= 4.4, tip)
    return c


def tex_membrane():
    """爻蝠翼膜：一片半透明的墨色薄翼，脉络分明。"""
    c = Canvas()
    c.mask(lambda x, y: (2 <= x <= 13 and 3 <= y <= 12
                         and abs(x - 7.5) * 0.55 + abs(y - 7.5) <= 5.6), rgb("38384A"))
    c.mask(lambda x, y: (2 <= x <= 13 and 3 <= y <= 12
                         and abs(x - 7.5) * 0.55 + abs(y - 7.5) <= 3.4), rgb("4A4A64"))
    for x, y in [(4, 6), (5, 7), (6, 8), (9, 8), (10, 7), (11, 6), (7, 5), (8, 10)]:
        c.put(x, y, rgb("6E6E8E"))
    c.hline(3, 12, 5, rgb("8A8AA8"))
    return c


def tex_scale():
    """地脉鳞：一片带年轮的青玉色甲片。"""
    c = Canvas()
    c.mask(lambda x, y: (2 <= x <= 13 and 2 <= y <= 13
                         and abs(x - 7.5) * 0.9 + abs(y - 7.5) * 0.9 <= 7.4), JADE_DARK)
    c.mask(lambda x, y: (3 <= x <= 12 and 3 <= y <= 12
                         and abs(x - 7.5) * 0.9 + abs(y - 7.5) * 0.9 <= 5.2), JADE)
    c.mask(lambda x, y: (5 <= x <= 10 and 5 <= y <= 10), JADE_LIGHT)
    for i, r in enumerate([6.4, 4.6, 2.6]):
        c.mask(lambda x, y, r=r: (abs((x - 7.5) ** 2 + (y - 7.5) ** 2 - r * r) <= r * 0.6
                                  and 3 <= x <= 12 and 3 <= y <= 12),
               shade(JADE_DARK, 0.82 + i * 0.12))
    return c


def tex_vein_map():
    """寻脉图：铺开的图纸，木轴在两侧，墨线勾出山与水。"""
    c = Canvas()
    c.rect(1, 2, 14, 13, PAPER)
    c.frame(1, 2, 14, 13, PAPER_SHADE)
    c.rect(0, 2, 0, 13, WOOD)
    c.rect(15, 2, 15, 13, WOOD)
    for x, y in [(3, 6), (4, 5), (5, 6), (6, 7), (7, 6), (8, 5), (9, 6)]:
        c.put(x, y, INK_LIGHT)
        c.put(x, y + 1, shade(INK_LIGHT, 0.78))
    for x, y in [(4, 10), (5, 11), (6, 10), (7, 11), (8, 10), (9, 11), (10, 10)]:
        c.put(x, y, rgb("4A7AA8"))
    c.put(11, 5, CINNABAR)
    c.put(12, 5, CINNABAR)
    c.put(11, 6, CINNABAR)
    return c


def tex_bagua_mirror():
    """八卦镜：八角铜框，中央镜面透光。"""
    c = Canvas()
    c.mask(lambda x, y: (abs(x - 7.5) <= 6.5 and abs(y - 7.5) <= 6.5
                         and abs(x - 7.5) + abs(y - 7.5) <= 10.5), COPPER_DARK)
    c.mask(lambda x, y: (abs(x - 7.5) <= 5.5 and abs(y - 7.5) <= 5.5
                         and abs(x - 7.5) + abs(y - 7.5) <= 9.0), COPPER)
    c.mask(lambda x, y: (x - 7.5) ** 2 + (y - 7.5) ** 2 <= 12.5, rgb("9AB8D0"))
    c.mask(lambda x, y: (x - 7.5) ** 2 + (y - 7.5) ** 2 <= 7.0, rgb("C8E0F0"))
    c.mask(lambda x, y: (x - 6.4) ** 2 + (y - 6.4) ** 2 <= 2.6, rgb("EFF8FF"))
    for x, y in [(7, 2), (13, 7), (8, 13), (2, 8), (3, 3), (12, 3), (12, 12), (3, 12)]:
        c.put(x, y, GOLD_LIGHT)
    return c


# ------------------------------------------------------------------ 方块纹理

def tex_block_stone(base, speckle=True):
    c = Canvas()
    c.rect(0, 0, 15, 15, base)
    c.frame(0, 0, 15, 15, shade(base, 0.72))
    # 砖缝
    c.hline(0, 15, 7, shade(base, 0.85))
    c.vline(7, 0, 7, shade(base, 0.85))
    c.vline(3, 8, 15, shade(base, 0.85))
    c.vline(11, 8, 15, shade(base, 0.85))
    if speckle:
        for x, y in [(2, 2), (5, 4), (9, 2), (13, 5), (4, 11), (8, 12), (12, 13), (6, 9)]:
            c.put(x, y, shade(base, 1.18))
    return c


def tex_block_wood():
    c = Canvas()
    c.rect(0, 0, 15, 15, WOOD)
    c.frame(0, 0, 15, 15, WOOD_DARK)
    for y in (3, 8, 12):
        c.hline(1, 14, y, shade(WOOD, 1.15))
    # 隼卯
    c.vline(7, 0, 15, WOOD_DARK)
    return c


def tex_block_qi_observatory():
    """观气台：木架上嵌一块玉盘。"""
    c = Canvas()
    c.rect(0, 0, 15, 15, WOOD)
    c.frame(0, 0, 15, 15, WOOD_DARK)
    for y in (4, 8, 12):
        c.hline(1, 14, y, shade(WOOD, 1.12))
    # 玉盘
    c.mask(lambda x, y: (x - 7.5) ** 2 + (y - 7.5) ** 2 <= 26, JADE_DARK)
    c.mask(lambda x, y: (x - 7.5) ** 2 + (y - 7.5) ** 2 <= 16, JADE)
    # 盘上六爻
    for index, is_yang in enumerate([True, False, True, False, True, False]):
        y = 10 - index
        if is_yang:
            c.hline(5, 10, y, INK)
        else:
            c.hline(5, 6, y, INK)
            c.hline(9, 10, y, INK)
    return c


def tex_block_censer():
    """香炉：三足铜炉，上有一缕烟。"""
    c = Canvas()
    c.rect(0, 0, 15, 15, STONE_DARK)
    c.frame(0, 0, 15, 15, shade(STONE_DARK, 0.8))
    # 炉身
    c.mask(lambda x, y: 2 <= (x - 7.5) ** 2 / 30.0 + (y - 10) ** 2 / 14.0 <= 1, COPPER)
    c.mask(lambda x, y: (x - 7.5) ** 2 / 30.0 + (y - 10) ** 2 / 14.0 <= 0.45, COPPER_DARK)
    # 炉口
    c.hline(4, 11, 6, GOLD)
    c.hline(4, 11, 7, shade(GOLD, 0.7))
    # 三足
    for x in (4, 7, 11):
        c.vline(x, 13, 14, COPPER_DARK)
    # 烟
    c.put(7, 4, (200, 200, 200, 190))
    c.put(8, 3, (200, 200, 200, 150))
    c.put(6, 2, (200, 200, 200, 110))
    return c


def tex_block_shi_gandang():
    """石敢当：立石，上刻「石敢當」三字的简化竖纹。"""
    c = Canvas()
    c.rect(0, 0, 15, 15, STONE)
    c.frame(0, 0, 15, 15, STONE_DARK)
    # 石碑
    c.rect(3, 1, 12, 15, shade(STONE, 1.12))
    c.frame(3, 1, 12, 15, shade(STONE, 0.7))
    c.rect(4, 2, 11, 14, shade(STONE, 1.2))
    # 刻纹：三条竖痕 + 朱砂印
    c.vline(7, 3, 13, STONE_DARK)
    c.vline(9, 3, 13, STONE_DARK)
    c.hline(5, 11, 5, STONE_DARK)
    c.rect(6, 10, 10, 12, rgb("9A3A30"))
    c.put(7, 11, CINNABAR_LIGHT)
    c.put(9, 11, CINNABAR_LIGHT)
    return c


def tex_block_gua_board():
    """卦盘：暗色盘面，刻六爻与八个方位。"""
    c = Canvas()
    c.rect(0, 0, 15, 15, INK)
    c.frame(0, 0, 15, 15, INK_LIGHT)
    c.mask(lambda x, y: (x - 7.5) ** 2 + (y - 7.5) ** 2 <= 42, shade(INK, 1.35))
    # 八个方位刻度
    for dx, dy in [(0, -6), (0, 6), (-6, 0), (6, 0), (4, -4), (-4, -4), (4, 4), (-4, 4)]:
        c.put(7 + dx, 7 + dy, GOLD)
    # 中心六爻
    for index, is_yang in enumerate([True, True, False, False, True, False]):
        y = 10 - index
        if is_yang:
            c.hline(5, 10, y, GOLD_LIGHT)
        else:
            c.hline(5, 6, y, GOLD_LIGHT)
            c.hline(9, 10, y, GOLD_LIGHT)
    # 四角红石触点
    for x, y in [(1, 1), (14, 1), (1, 14), (14, 14)]:
        c.put(x, y, CINNABAR)
    return c


def tex_block_bagua_platform():
    """八卦台：石台，中央太极，四周八卦符号。"""
    c = Canvas()
    c.rect(0, 0, 15, 15, shade(STONE, 1.05))
    c.frame(0, 0, 15, 15, STONE_DARK)
    c.mask(lambda x, y: (x - 7.5) ** 2 + (y - 7.5) ** 2 <= 50, shade(STONE, 1.15))
    c.mask(lambda x, y: (x - 7.5) ** 2 + (y - 7.5) ** 2 <= 20, INK)
    # 太极
    c.mask(lambda x, y: (x - 7.5) ** 2 + (y - 7.5) ** 2 <= 12 and x < 8, BONE)
    c.put(7, 5, BONE)
    c.put(8, 9, INK)
    # 四向卦符
    for x, y in [(7, 1), (7, 14), (1, 7), (14, 7)]:
        c.put(x, y, GOLD)
    return c


def tex_block_taiji():
    """太极图方块：黑白双鱼。"""
    c = Canvas()
    c.rect(0, 0, 15, 15, rgb("2A2A32"))
    c.frame(0, 0, 15, 15, INK)
    c.mask(lambda x, y: (x - 7.5) ** 2 + (y - 7.5) ** 2 <= 42, BONE)
    for y in range(16):
        for x in range(16):
            if (x - 7.5) ** 2 + (y - 7.5) ** 2 <= 42 and x > 7:
                c.put(x, y, INK)
    c.mask(lambda x, y: (x - 7.5) ** 2 + (y - 5.5) ** 2 <= 5, BONE)
    c.mask(lambda x, y: (x - 7.5) ** 2 + (y - 9.5) ** 2 <= 5, INK)
    c.put(7, 4, INK)
    c.put(8, 11, BONE)
    return c


def tex_block_altar():
    """祭坛：方形石座，四角火盆，中央凹槽。"""
    c = Canvas()
    c.rect(0, 0, 15, 15, STONE_DARK)
    c.frame(0, 0, 15, 15, shade(STONE_DARK, 0.7))
    c.rect(2, 2, 13, 13, shade(STONE_DARK, 1.2))
    c.frame(2, 2, 13, 13, STONE)
    c.rect(5, 5, 10, 10, INK)
    c.frame(5, 5, 10, 10, GOLD)
    # 四角火盆
    for x, y in [(3, 3), (12, 3), (3, 12), (12, 12)]:
        c.rect(x - 1, y - 1, x + 1, y + 1, COPPER_DARK)
        c.put(x, y, CINNABAR)
        c.put(x, y - 1, GOLD_LIGHT)
    return c


def tex_block_ruin_core():
    """遗迹核心：会发光的晶体，内含旋转的卦纹。"""
    c = Canvas()
    c.rect(0, 0, 15, 15, rgb("241A38"))
    c.frame(0, 0, 15, 15, rgb("3A2A58"))
    c.mask(lambda x, y: abs(x - 7.5) / 6.5 + abs(y - 7.5) / 6.5 <= 1, rgb("503A78"))
    c.mask(lambda x, y: abs(x - 7.5) / 6.5 + abs(y - 7.5) / 6.5 <= 0.7, rgb("7050A8"))
    for index, is_yang in enumerate([True, False, True, True, False, True]):
        y = 10 - index
        if is_yang:
            c.hline(5, 10, y, rgb("C8A0F0"))
        else:
            c.hline(5, 6, y, rgb("C8A0F0"))
            c.hline(9, 10, y, rgb("C8A0F0"))
    c.put(7, 3, rgb("E8D0FF"))
    c.put(8, 12, rgb("E8D0FF"))
    return c


def tex_block_yinyang_stele():
    """阴阳界碑：碑面刻阴阳双鱼，一半暗一半明。"""
    c = Canvas()
    c.rect(0, 0, 15, 15, rgb("2A2E38"))
    c.frame(0, 0, 15, 15, INK)
    c.rect(3, 1, 12, 15, shade(BONE, 0.9))
    c.frame(3, 1, 12, 15, shade(BONE, 0.55))
    c.mask(lambda x, y: (x - 7.5) ** 2 + (y - 7) ** 2 <= 14 and x > 7, INK)
    c.mask(lambda x, y: 3 <= x <= 12 and 1 <= y <= 15, shade(BONE, 1.0))
    c.mask(lambda x, y: (x - 7.5) ** 2 + (y - 7) ** 2 <= 14 and x > 7, INK)
    c.mask(lambda x, y: (x - 7.5) ** 2 + (y - 5) ** 2 <= 3, BONE)
    c.mask(lambda x, y: (x - 7.5) ** 2 + (y - 9) ** 2 <= 3, INK)
    return c


def tex_block_dragon_core():
    """龙脉核心块：青玉与流动的龙气纹。"""
    c = Canvas()
    c.rect(0, 0, 15, 15, rgb("1A3A38"))
    c.frame(0, 0, 15, 15, rgb("2A5A56"))
    c.mask(lambda x, y: (x - 7.5) ** 2 + (y - 7.5) ** 2 <= 46, rgb("2E6E66"))
    c.mask(lambda x, y: (x - 7.5) ** 2 + (y - 7.5) ** 2 <= 26, rgb("48A08E"))
    # 龙气旋纹
    for index, y in enumerate(range(2, 14, 2)):
        offset = (index % 3) * 2
        c.hline(2 + offset, 13 - offset, y, rgb("7FE0C8"))
    c.put(7, 7, rgb("E0FFF4"))
    return c


def tex_element_block(kind):
    base = ELEMENT_COLORS[kind]
    return tex_block_stone(base)


def tex_dim_stone(base, dark, light, fleck=None):
    """维度基础石：石质底 + 星点/斑纹。"""
    c = Canvas()
    c.rect(0, 0, 15, 15, base)
    c.frame(0, 0, 15, 15, dark)
    for x, y in [(2, 2), (5, 4), (9, 2), (13, 5), (4, 11), (8, 12), (12, 13), (6, 9),
                 (3, 6), (11, 8), (14, 3), (1, 12)]:
        c.put(x, y, light)
    if fleck:
        for x, y in [(7, 3), (10, 6), (3, 9), (13, 11), (6, 14), (2, 7)]:
            c.put(x, y, fleck)
    return c


def tex_dim_bricks(base, dark, light):
    """维度砖：错缝砖 + 顶部受光。"""
    c = Canvas()
    c.rect(0, 0, 15, 15, base)
    mortar = shade(base, 0.55)
    for y in (0, 5, 10, 15):
        c.hline(0, 15, y, mortar)
    for row, y0 in enumerate((0, 5, 10)):
        offset = 0 if row % 2 == 0 else 4
        for x in range(offset, 16, 8):
            for y in range(y0, min(y0 + 5, 16)):
                c.put(x % 16, y, mortar)
    # 受光棱
    for y in (1, 6, 11):
        for x in range(0, 16):
            if c.get(x, y)[3] and c.get(x, y) == base:
                c.put(x, y, light)
    c.frame(0, 0, 15, 15, dark)
    return c


def tex_dim_lamp(core, halo, frame):
    """维度明灯：方框 + 发光核心。"""
    c = Canvas()
    c.rect(0, 0, 15, 15, frame)
    c.mask(lambda x, y: (x - 7.5) ** 2 + (y - 7.5) ** 2 <= 42, halo)
    c.mask(lambda x, y: (x - 7.5) ** 2 + (y - 7.5) ** 2 <= 20, core)
    c.put(7, 6, rgb("FFFFFF"))
    c.put(8, 7, rgb("FFFFFF"))
    return c


def tex_spawn_egg(base, spot, hi):
    """刷怪蛋：蛋形轮廓 + 斑点（16×16）。"""
    c = Canvas()
    widths = [3, 5, 7, 9, 11, 11, 11, 11, 11, 11, 9, 7, 5, 3]
    for idx, w in enumerate(widths):
        y = idx + 1
        half = w // 2
        for x in range(8 - half, 8 + half):
            c.put(x, y, base)
    for x, y in [(6, 3), (9, 5), (5, 6), (10, 8), (7, 9), (6, 11), (9, 12), (8, 6), (4, 8)]:
        c.put(x, y, spot)
    for x, y in [(6, 2), (5, 3), (10, 6)]:
        c.put(x, y, hi)
    return c


def tex_icon():
    """模组图标：玉牌 + 六爻 + 朱砂边。"""
    size = 128
    c = Canvas(size)
    c.rect(0, 0, size - 1, size - 1, (18, 22, 30, 255))
    c.rect(4, 4, size - 5, size - 5, (30, 38, 50, 255))
    # 玉牌
    c.rect(16, 10, size - 17, size - 11, JADE)
    c.frame(16, 10, size - 17, size - 11, JADE_DARK)
    c.frame(18, 12, size - 19, size - 13, JADE_LIGHT)
    # 六爻
    bars = [True, False, True, True, False, False]
    for index, is_yang in enumerate(bars):
        y = 30 + index * 12
        if is_yang:
            c.hline(28, size - 29, y, INK)
            c.hline(28, size - 29, y + 1, INK)
        else:
            c.hline(28, 54, y, INK)
            c.hline(28, 54, y + 1, INK)
            c.hline(size - 55, size - 29, y, INK)
            c.hline(size - 55, size - 29, y + 1, INK)
    # 四角朱砂
    for x, y in [(10, 10), (size - 11, 10), (10, size - 11), (size - 11, size - 11)]:
        c.rect(x - 2, y - 2, x + 2, y + 2, CINNABAR)
    return c




# ------------------------------------------------------------------ 第 9 轮：维度门户与灵植

GATE_DEEP = {"astral": rgb("3A2E66"), "jade": rgb("2E5C4C"), "yao": rgb("33333A"),
             "ziwei": rgb("2E2250"), "liuren": rgb("1E4A56"), "qimen": rgb("4A2A10")}
GATE_GLOW = {"astral": rgb("B9A8F0"), "jade": rgb("8FE0C0"), "yao": rgb("E09070"),
             "ziwei": rgb("C8A8F0"), "liuren": rgb("8AD8E8"), "qimen": rgb("F0B870")}


def tex_ore(ore, ore_hi):
    """矿石：统一的石底 + 三个彩色矿脉团。"""
    base = rgb("7E7E86")
    c = Canvas()
    c.rect(0, 0, 15, 15, base)
    for x, y in [(2, 3), (5, 7), (12, 2), (9, 12), (3, 11), (14, 8), (7, 13), (0, 6)]:
        c.put(x, y, shade(base, 0.88))
        c.put((x + 1) % 16, (y + 2) % 16, shade(base, 1.08))
    for cx, cy in ((4, 4), (10, 9), (11, 3)):
        c.put(cx, cy, ore)
        c.put(cx + 1, cy, ore_hi)
        c.put(cx, cy + 1, ore_hi)
        c.put(cx + 1, cy + 1, ore)
        c.put(cx - 1, cy + 1, ore)
        c.put(cx + 2, cy, shade(ore, 0.8))
        c.put(cx + 1, cy + 2, shade(ore, 0.8))
    return c


def tex_pillar_side(base, dark, light):
    """柱侧面：竖条纹 + 上下线脚。"""
    c = Canvas()
    c.rect(0, 0, 15, 15, base)
    for x in (1, 5, 9, 13):
        c.vline(x, 0, 15, shade(base, 0.8))
    for x in (3, 7, 11, 15):
        c.vline(x, 0, 15, light)
    c.hline(0, 15, 0, dark)
    c.hline(0, 15, 1, light)
    c.hline(0, 15, 15, dark)
    c.hline(0, 15, 14, shade(dark, 1.2))
    return c


def tex_pillar_top(base, dark, light):
    """柱顶：同心方框的切面纹。"""
    c = Canvas()
    c.rect(0, 0, 15, 15, base)
    c.frame(0, 0, 15, 15, dark)
    c.frame(2, 2, 13, 13, shade(base, 0.82))
    c.frame(4, 4, 11, 11, light)
    c.rect(6, 6, 9, 9, shade(base, 0.92))
    c.put(7, 7, dark)
    c.put(8, 8, dark)
    return c


def tex_flower(core, petal, petal_d, stem):
    """十字花：五瓣花冠 + 花心 + 茎叶（透明底）。"""
    c = Canvas()
    for y in (2, 6):
        for x in (6, 7, 8, 9):
            c.put(x, y, petal)
    for y in (3, 4, 5):
        for x in (5, 6, 7, 8, 9, 10):
            c.put(x, y, petal)
    for x in (9, 6):
        c.put(x, 6, petal_d)
    c.put(10, 5, petal_d)
    c.put(5, 6, petal_d)
    c.rect(7, 4, 8, 5, core)
    for y in range(7, 16):
        c.put(7, y, stem)
        c.put(8, y, shade(stem, 0.85))
    c.put(6, 10, stem)
    c.put(5, 10, shade(stem, 1.35))
    c.put(9, 11, stem)
    c.put(10, 11, shade(stem, 1.35))
    return c


def tex_blade(base, light, tip):
    """草簇：五束竖直草叶，顶端有色（透明底）。"""
    c = Canvas()
    for x, y0, zz in ((4, 9, 14), (6, 5, 14), (8, 4, 14), (10, 6, 14), (12, 9, 14)):
        c.vline(x, y0, zz, base)
        c.put(x + 1, y0 + 1, light)
        c.put(x, y0 - 1, tip)
    c.put(5, 11, base)
    c.put(5, 12, light)
    c.put(11, 12, base)
    c.put(11, 13, light)
    return c


def tex_gate(deep, glow, spark):
    """门面：半透明的星云漩涡。"""
    import math
    c = Canvas()
    c.rect(0, 0, 15, 15, (deep[0], deep[1], deep[2], 168))
    for t in range(0, 90):
        ang = t * 0.34
        r = 1.0 + t * 0.11
        x = int(7.5 + r * math.cos(ang))
        y = int(7.5 + r * math.sin(ang))
        c.put(x, y, (glow[0], glow[1], glow[2], 225))
        if t % 6 == 0:
            c.put(x + 1, y, (spark[0], spark[1], spark[2], 240))
    for x, y in ((3, 3), (12, 4), (4, 12), (13, 11), (8, 2), (2, 8)):
        c.put(x, y, (255, 255, 255, 235))
    return c


def tex_key(metal, gem, gem_hi):
    """灵引：带宝石的钥匙。"""
    c = Canvas()
    c.frame(3, 2, 7, 6, metal)
    c.put(4, 3, shade(metal, 1.2))
    for y in range(6, 14):
        c.put(5, y, metal)
        c.put(6, y, shade(metal, 0.8))
    c.put(7, 11, metal)
    c.put(7, 13, metal)
    c.put(8, 13, metal)
    c.rect(4, 3, 6, 5, gem)
    c.put(4, 3, gem_hi)
    c.put(5, 4, gem_hi)
    return c


def tex_fluid_bucket(fluid, fluid_hi):
    """桶：上宽下窄的金属提桶，桶口内侧可见液面。

    几何约定（16×16，第 17 轮修正）：
        y=3         桶口后沿（只有金属，两侧各露出一点）
        y=4..6      桶口内壁——左右两条金属壁夹住中间的液面
        y=7         桶身上沿（一条压暗的金属线，把桶口和桶身分开）
        y=8..13     桶身，逐层收窄
        y=13        桶底

    旧版把液体画在 y=3..4 的整行上，那两行没有左右桶壁，
    于是液体像一条横杠浮在桶口外面 —— 这次从结构上修掉：
    液面只出现在「被两侧金属壁包住」的 x 区间里。
    """
    c = Canvas()
    metal = rgb("C8C8D4")
    metal_d = rgb("8E8E9C")
    metal_l = rgb("E8E8F2")

    # 桶身轮廓：y -> (左壁 x, 右壁 x)
    body = {
        4: (4, 11), 5: (4, 11), 6: (4, 11),
        7: (4, 11), 8: (4, 11),
        9: (5, 11), 10: (5, 11),
        11: (6, 11), 12: (6, 10),
        13: (7, 9),
    }

    # 桶口后沿：只在顶上一行，短于桶口，做出「后面那一圈」
    c.hline(5, 10, 3, metal_d)

    # 桶身：每行左高光 / 中金属 / 右阴影
    for y, (x0, x1) in body.items():
        c.hline(x0, x1, y, metal)
        c.put(x0, y, metal_l)
        c.put(x1, y, metal_d)

    # 桶口与桶身的分界线：压暗一整行
    c.hline(4, 11, 7, metal_d)
    c.put(4, 7, metal_l)

    # 液面：只填在左右桶壁之间（x0+1 .. x1-1），y=4..6
    for y in (4, 5, 6):
        x0, x1 = body[y]
        fill = fluid if y <= 5 else shade(fluid, 0.86)
        for x in range(x0 + 1, x1):
            c.put(x, y, fill)

    # 液面高光：两三点就够，太多会花
    c.put(6, 4, fluid_hi)
    c.put(9, 4, fluid_hi)
    c.put(7, 5, fluid_hi)

    # 桶底压暗一点，让它「坐得住」
    for x in range(7, 10):
        c.put(x, 13, shade(metal, 0.78))
    return c


# 16 像素一个周期 —— 液体贴图必须**严格无缝平铺**，
# 所以所有正弦项的角频率只能是 TAU/16 的整数倍：
# x 从 0 走到 16，相位正好走完整数个 2π，左右边缘自然接上。
# 之前用 0.52 / 1.02 这类「看起来差不多」的小数，池塘一大就露出 16 像素的网格断层。
FLUID_K = math.tau / 16.0


def tint(color, amount):
    """向白（amount>0）或向黑（amount<0）线性混合，保持色相不跑偏。

    shade() 是乘法，暗色提亮很吃力、亮色又容易顶到 255；
    液体要高光/中间调/暗部三档都受控，用混合更稳。
    """
    r, g, b, a = color
    if amount >= 0:
        return (int(r + (255 - r) * amount),
                int(g + (255 - g) * amount),
                int(b + (255 - b) * amount), a)
    t = -amount
    return (int(r * (1 - t)), int(g * (1 - t)), int(b * (1 - t)), a)


def tex_fluid_still(base, hi, dark, phase=0.0):
    """静止液面：缓慢起伏的波光。

    三层正弦叠加：主干是沿 y 的两道横向波脊，中间那道 sin(x) 让波脊弯起来，
    再加一道斜波打散规律 —— 整体读起来是「水面上一道道弯曲的亮纹」。

    闭合性（决定了平铺有没有接缝）：
        y * FLUID_K * 2      y=0→16 走 2 个整周期   ✓
        x * FLUID_K          x=0→16 走 1 个整周期   ✓
        x*FLUID_K*2 + y*FLUID_K                     ✓
    三个方向都整周期收尾，所以 4×4 铺开是一片连续的水，不是瓷砖。

    hi/dark 参数保留只为兼容老调用点：三档明暗统一由 base 派生，
    六种液体才不会有的刺眼、有的发灰。
    """
    c = Canvas()
    c.rect(0, 0, 15, 15, base)
    high = tint(base, 0.32)
    pale = tint(base, 0.14)
    low = tint(base, -0.24)
    for y in range(16):
        for x in range(16):
            value = (math.sin(y * FLUID_K * 2 + 1.6 * math.sin(x * FLUID_K + phase))
                     + 0.60 * math.sin(x * FLUID_K * 2 + y * FLUID_K - phase))
            if value > 0.72:
                c.put(x, y, high)
            elif value > 0.24:
                c.put(x, y, pale)
            elif value < -0.74:
                c.put(x, y, low)
    return c


def tex_fluid_flow(base, hi, dark, phase=0.0):
    """流动液面：竖向蛇形的水流。

    主轴放在 x 上 → 条纹是竖的；扰动项里带 (y*K + phase)，
    相位推进时整组竖纹沿着 y 平移 —— 播放起来就是「水往下淌」。

    循环闭合：phase 走满一整圈（8 帧 × TAU/8）时，
    y 方向的相位正好前进 16，也就是**整整一个波长** ——
    第 7 帧接回第 0 帧时图案刚好对齐，不会有一下明显的「跳帧」。
    （早先这里乘 5.0，一循环跑掉两个波长、每帧位移近 4 像素，
    看着像在闪而不是在流。）

    平铺闭合：x 项走 2 个整周期、y 项走 1 个整周期，单帧自身无缝。
    """
    c = Canvas()
    c.rect(0, 0, 15, 15, base)
    high = tint(base, 0.30)
    pale = tint(base, 0.12)
    low = tint(base, -0.20)
    for y in range(16):
        for x in range(16):
            flow = y * FLUID_K + phase
            value = (math.sin(x * FLUID_K * 2 + 1.5 * math.sin(flow))
                     + 0.52 * math.sin(x * FLUID_K + flow))
            if value > 0.70:
                c.put(x, y, high)
            elif value > 0.22:
                c.put(x, y, pale)
            elif value < -0.72:
                c.put(x, y, low)
    return c


def fluid_still_anim(base, hi, dark, frames=8):
    """液面动画：相位走满一整圈，接缝处正好首尾相接。"""
    return Anim([tex_fluid_still(base, hi, dark, phase=i * math.tau / frames)
                 for i in range(frames)], frametime=4)


def fluid_flow_anim(base, hi, dark, frames=8):
    """水流动画：相位沿 y 推进，看起来在往下流。

    **帧必须是 32×32，不是 16×16。** 原版 water_flow.png 是 32 宽，
    一帧里按 2×2 切成四格；FluidRenderer 用 getU(0.5)/getV(0.5) 选其中
    一格（四个方向各取一格）。如果只给 16 宽的帧，UV 会越界跑到图集里
    别的贴图上，流动的液体就是一片花屏。
    所以这里把 16×16 的波流图铺满 2×2 四格（四格内容一致）——
    32×32 帧内部连续，方块之间平铺也不会有拼贴感。
    """
    # 四格用同一张 16×16 —— 这样 32×32 帧内部也连续，
    # 流体在方块之间平铺时不会在 16 像素处出现「拼贴感」。
    tiles = [(0, 0), (16, 0), (0, 16), (16, 16)]
    out = []
    for i in range(frames):
        tile = tex_fluid_flow(base, hi, dark, phase=i * math.tau / frames)
        frame = Canvas(32)
        for (ox, oy) in tiles:
            for y in range(16):
                for x in range(16):
                    frame.put(ox + x, oy + y, tile.get(x, y))
        out.append(frame)
    return Anim(out, frametime=3)


def tex_log_side(base, dark, light):
    """原木侧面：竖向树皮纹。"""
    c = Canvas()
    c.rect(0, 0, 15, 15, base)
    for x in range(16):
        if x % 4 == 1:
            c.vline(x, 0, 15, dark)
        elif x % 4 == 3:
            c.vline(x, 0, 15, light)
    for y in (3, 7, 11, 14):
        for x in range(16):
            if (x + y) % 7 == 0:
                c.put(x, y, dark)
    return c


def tex_log_top(base, dark, light):
    """原木横截面：年轮。"""
    c = Canvas()
    c.rect(0, 0, 15, 15, base)
    c.frame(0, 0, 15, 15, dark)
    c.frame(3, 3, 12, 12, dark)
    c.frame(6, 6, 9, 9, light)
    c.put(7, 7, dark)
    c.put(8, 8, light)
    return c


def tex_leaf(base, dark, light):
    """树叶：细碎斑驳 + 全透明镂空（走 cutout 渲染）。"""
    c = Canvas()
    c.rect(0, 0, 15, 15, base)
    for y in range(16):
        for x in range(16):
            h = (x * 7 + y * 11 + (x * y) % 5) % 13
            if h < 3:
                c.put(x, y, dark)
            elif h < 5:
                c.put(x, y, light)
    for (x, y) in ((1, 2), (4, 1), (7, 3), (11, 2), (14, 5), (2, 6), (5, 8),
                   (8, 6), (12, 9), (3, 11), (6, 13), (9, 12), (13, 13),
                   (0, 8), (15, 11), (6, 4), (10, 14), (4, 15)):
        c.put(x, y, (0, 0, 0, 0))
    return c


def tex_sapling_plant(leaf, dark, stem):
    """树苗：一根小茎 + 一团新叶。"""
    c = Canvas()
    c.vline(8, 10, 14, stem)
    c.put(7, 14, stem)
    c.put(9, 14, stem)
    for (x, y) in ((6, 5), (7, 4), (8, 4), (9, 5), (5, 6), (10, 6),
                   (5, 7), (10, 7), (6, 8), (9, 8), (7, 9), (8, 9),
                   (6, 6), (9, 6), (7, 7), (8, 7)):
        c.put(x, y, leaf)
    for (x, y) in ((6, 5), (8, 4), (5, 6), (9, 8)):
        c.put(x, y, dark)
    return c


def stairs_blockstate(name):
    """完整覆盖 4×2×5 共 40 种状态的楼梯 blockstate（旋转表照原版规律推导）。"""
    base_map = {"east": 0, "south": 90, "west": 180, "north": 270}
    variant_model = {
        "straight": f"{NS}:block/{name}",
        "inner_left": f"{NS}:block/{name}_inner",
        "inner_right": f"{NS}:block/{name}_inner",
        "outer_left": f"{NS}:block/{name}_outer",
        "outer_right": f"{NS}:block/{name}_outer",
    }
    variants = {}
    for facing, base in base_map.items():
        for half in ("bottom", "top"):
            for shape, model in variant_model.items():
                if shape == "straight" or shape.endswith("right"):
                    y = base
                else:
                    y = (base + 270) % 360
                if half == "top" and shape != "straight":
                    y = (y + 90) % 360
                entry = {"model": model}
                if half == "top":
                    entry["x"] = 180
                if y:
                    entry["y"] = y
                if "x" in entry or "y" in entry:
                    entry["uvlock"] = True
                variants[f"facing={facing},half={half},shape={shape}"] = entry
    return {"variants": variants}


def gen_realm_assets():
    """第 9 轮全部资源：矿石 / 粗矿 / 花 / 草 / 柱 / 台阶 / 楼梯 / 门户 / 灵引 / 桶 / 液体。"""
    written = 0

    item_tex = [
        ("astral_dust", lambda: tex_dust_ore(ASTRAL_L, rgb("D8CCFF"), ASTRAL_D)),
        ("jade_grit", lambda: tex_dust_ore(JADE_2L, rgb("D8F4E4"), JADE_2D)),
        ("yao_shard", lambda: tex_dust_ore(YAO_L, rgb("E8E8F0"), YAO_D)),
        ("astral_key", lambda: tex_key(GOLD, ASTRAL_KEY, ASTRAL_KEY_L)),
        ("jade_key", lambda: tex_key(GOLD, JADE_2, JADE_2L)),
        ("yao_key", lambda: tex_key(GOLD, YAO_RED, YAO_L)),
        ("astral_dew_bucket", lambda: tex_fluid_bucket(ASTRAL_DEW, ASTRAL_DEW_L)),
        ("jade_ichor_bucket", lambda: tex_fluid_bucket(JADE_2L, rgb("E8FFF4"))),
        ("yao_ink_bucket", lambda: tex_fluid_bucket(rgb("4A4A56"), rgb("9A9AA8"))),
        ("meteor_iron", lambda: tex_dust_ore(rgb("C0C0D4"), rgb("E4E4F4"), rgb("74748C"))),
        ("star_sand", lambda: tex_dust_ore(rgb("E0C880"), rgb("F4E4B0"), rgb("9A8448"))),
        ("star_crystal", lambda: tex_dust_ore(rgb("C8A8E8"), rgb("E8D0FF"), rgb("8460A8"))),
        ("verdant_gem", lambda: tex_dust_ore(rgb("68C898"), rgb("9AE8BC"), rgb("348060"))),
        ("mica", lambda: tex_dust_ore(rgb("B8DCD4"), rgb("E0F4F0"), rgb("689A90"))),
        ("vein_crystal", lambda: tex_dust_ore(rgb("C8E0A0"), rgb("E8F4C8"), rgb("7A9850"))),
        ("ink_iron", lambda: tex_dust_ore(rgb("A8A8BC"), rgb("D0D0E0"), rgb("565668"))),
        ("onyx", lambda: tex_dust_ore(rgb("7A7A90"), rgb("A8A8C0"), rgb("3A3A4A"))),
        ("oracle_bone", lambda: tex_dust_ore(rgb("E0D8B8"), rgb("F4EED0"), rgb("968C68"))),
        # ---- 第 16 轮：三术界材料 ----
        ("ziwei_crystal", lambda: tex_dust_ore(rgb("C8A8F0"), rgb("E8D0FF"), rgb("7A50B8"))),
        ("ziwei_essence", lambda: tex_dust_ore(rgb("B8A0E8"), rgb("DCC8FF"), rgb("6A4890"))),
        ("liuren_crystal", lambda: tex_dust_ore(rgb("8AD0E0"), rgb("C0F0F8"), rgb("3A7A88"))),
        ("liuren_essence", lambda: tex_dust_ore(rgb("A0C8D8"), rgb("D0E8F0"), rgb("4A7A88"))),
        ("qimen_crystal", lambda: tex_dust_ore(rgb("E8A868"), rgb("F8D0A0"), rgb("9A5A28"))),
        ("qimen_essence", lambda: tex_dust_ore(rgb("D8B888"), rgb("F0DCC0"), rgb("8A6A40"))),
        # ---- 第 16 轮：三术界灵引与桶 ----
        ("ziwei_key", lambda: tex_key(GOLD, ZW, ZW_L)),
        ("liuren_key", lambda: tex_key(GOLD, LR_KEY, LR_KEY_L)),
        ("qimen_key", lambda: tex_key(GOLD, QM, QM_L)),
        ("ziwei_ambrosia_bucket", lambda: tex_fluid_bucket(ZW_L, rgb("F0E4FF"))),
        ("liuren_water_bucket", lambda: tex_fluid_bucket(LR_L, rgb("E0FAFF"))),
        ("qimen_mercury_bucket", lambda: tex_fluid_bucket(QM_HG, QM_HG_L)),
    ]
    block_tex = [
        ("astral_ore", lambda: tex_ore(ASTRAL_L, ASTRAL_D)),
        ("jade_ore", lambda: tex_ore(JADE_2L, JADE_2D)),
        ("yao_ore", lambda: tex_ore(YAO_RED, YAO_D)),
        ("astral_pillar_side", lambda: tex_pillar_side(ASTRAL, ASTRAL_D, ASTRAL_L)),
        ("astral_pillar_top", lambda: tex_pillar_top(ASTRAL, ASTRAL_D, ASTRAL_L)),
        ("jade_pillar_side", lambda: tex_pillar_side(JADE_2, JADE_2D, JADE_2L)),
        ("jade_pillar_top", lambda: tex_pillar_top(JADE_2, JADE_2D, JADE_2L)),
        ("yao_pillar_side", lambda: tex_pillar_side(YAO, YAO_D, YAO_L)),
        ("yao_pillar_top", lambda: tex_pillar_top(YAO, YAO_D, YAO_L)),
        ("astral_bloom", lambda: tex_flower(GOLD, ASTRAL_L, ASTRAL_D, rgb("4C9A5C"))),
        ("jade_lily", lambda: tex_flower(PAPER, JADE_2L, JADE_2D, rgb("4C9A5C"))),
        ("yao_bloom", lambda: tex_flower(GOLD, YAO_RED, YAO_D, rgb("5C8A4C"))),
        ("astral_grass", lambda: tex_blade(ASTRAL_L, rgb("D8CCFF"), ASTRAL)),
        ("jade_grass", lambda: tex_blade(JADE_2L, rgb("D8F4E4"), JADE_2)),
        ("yao_grass", lambda: tex_blade(YAO_L, rgb("E8E8F0"), YAO)),
        ("astral_gate", lambda: tex_gate(GATE_DEEP["astral"], GATE_GLOW["astral"], rgb("FFFFFF"))),
        ("jade_gate", lambda: tex_gate(GATE_DEEP["jade"], GATE_GLOW["jade"], rgb("FFFFFF"))),
        ("yao_gate", lambda: tex_gate(GATE_DEEP["yao"], GATE_GLOW["yao"], rgb("FFFFFF"))),
        ("astral_dew_still", lambda: fluid_still_anim(ASTRAL_DEW, ASTRAL_DEW_L, ASTRAL_DEW_D)),
        ("astral_dew_flow", lambda: fluid_flow_anim(ASTRAL_DEW, ASTRAL_DEW_L, ASTRAL_DEW_D)),
        ("jade_ichor_still", lambda: fluid_still_anim(JADE_2, JADE_2L, JADE_2D)),
        ("jade_ichor_flow", lambda: fluid_flow_anim(JADE_2, JADE_2L, JADE_2D)),
        ("yao_ink_still", lambda: fluid_still_anim(rgb("3A3A46"), rgb("8A8A9A"), rgb("22222A"))),
        ("yao_ink_flow", lambda: fluid_flow_anim(rgb("3A3A46"), rgb("8A8A9A"), rgb("22222A"))),
        # ---- 第 16 轮：三术界花 / 草 / 门 / 液体 ----
        ("ziwei_bloom", lambda: tex_flower(rgb("F0D890"), ZW_L, ZW_D, rgb("4C9A5C"))),
        ("liuren_lily", lambda: tex_flower(PAPER, LR_L, LR_D, rgb("4C9A5C"))),
        ("qimen_bloom", lambda: tex_flower(rgb("FFD8A0"), QM, QM_D, rgb("6A8A4C"))),
        ("ziwei_grass", lambda: tex_blade(ZW_L, rgb("E8D8FF"), ZW)),
        ("liuren_grass", lambda: tex_blade(LR_L, rgb("D0F8FF"), LR)),
        ("qimen_grass", lambda: tex_blade(QM_L, rgb("FFE0B0"), QM)),
        ("ziwei_gate", lambda: tex_gate(GATE_DEEP["ziwei"], GATE_GLOW["ziwei"], rgb("FFFFFF"))),
        ("liuren_gate", lambda: tex_gate(GATE_DEEP["liuren"], GATE_GLOW["liuren"], rgb("FFFFFF"))),
        ("qimen_gate", lambda: tex_gate(GATE_DEEP["qimen"], GATE_GLOW["qimen"], rgb("FFFFFF"))),
        ("ziwei_ambrosia_still", lambda: fluid_still_anim(ZW, ZW_L, ZW_D)),
        ("ziwei_ambrosia_flow", lambda: fluid_flow_anim(ZW, ZW_L, ZW_D)),
        ("liuren_water_still", lambda: fluid_still_anim(LR, LR_L, LR_D)),
        ("liuren_water_flow", lambda: fluid_flow_anim(LR, LR_L, LR_D)),
        ("qimen_mercury_still", lambda: fluid_still_anim(QM_HG, QM_HG_L, QM_HG_D)),
        ("qimen_mercury_flow", lambda: fluid_flow_anim(QM_HG, QM_HG_L, QM_HG_D)),
    ]
    for name, factory in item_tex:
        write_texture(os.path.join(ASSETS, "textures", "item", f"{name}.png"),
                      factory())
        written += 1
    for name, factory in block_tex:
        write_texture(os.path.join(ASSETS, "textures", "block", f"{name}.png"),
                      factory())
        written += 1

    # ---- 第 10 轮纹理：原木（侧/顶）、树叶、树苗、新矿 ----
    for species, (lb, ld, ll, kb, kd, kl) in WOOD_COLORS.items():
        for suffix, canvas in (
                ("log_side", tex_log_side(lb, ld, ll)),
                ("log_top", tex_log_top(lb, ld, ll)),
                ("leaves", tex_leaf(kb, kd, kl)),
                ("sapling", tex_sapling_plant(kb, kd, ld))):
            write_png(os.path.join(ASSETS, "textures", "block", f"{species}_{suffix}.png"),
                      canvas.to_pixels(), canvas.size, canvas.size)
            written += 1
    for ore, (hi, dark) in ORE_TEX.items():
        canvas = tex_ore(hi, dark)
        write_png(os.path.join(ASSETS, "textures", "block", f"{ore}.png"),
                  canvas.to_pixels(), canvas.size, canvas.size)
        written += 1

    def put_json(rel, obj):
        path = os.path.join(ASSETS, rel)
        os.makedirs(os.path.dirname(path), exist_ok=True)
        with open(path, "w", encoding="utf-8") as handle:
            json.dump(obj, handle, indent=1)

    # 矿石（12 种）
    for name in ("astral_ore", "jade_ore", "yao_ore", "meteor_ore", "star_sand_ore",
                 "crystal_ore", "verdant_ore", "mica_ore", "vein_ore",
                 "ink_iron_ore", "onyx_ore", "oracle_ore",
                 # 第 16 轮：三术界十二矿
                 "ziwei_crystal_ore", "dou_ore", "ji_ore", "yuan_ore",
                 "ren_ore", "gui_ore", "she_ore", "xuan_ore",
                 "men_ore", "xing_ore", "dun_ore", "jia_ore"):
        put_json(f"models/block/{name}.json", {
            "parent": "minecraft:block/cube_all",
            "textures": {"all": f"{NS}:block/{name}"},
        })
        put_json(f"blockstates/{name}.json", {"variants": {"": {"model": f"{NS}:block/{name}"}}})
        put_json(f"items/{name}.json", {"model": {"type": "minecraft:model", "model": f"{NS}:block/{name}"}})
        put_json(f"models/item/{name}.json", {"parent": f"{NS}:block/{name}"})
        written += 4

    # 柱
    for realm in ("astral", "jade", "yao"):
        name = f"{realm}_pillar"
        put_json(f"models/block/{name}.json", {
            "parent": "minecraft:block/cube_column",
            "textures": {"side": f"{NS}:block/{name}_side", "end": f"{NS}:block/{name}_top"},
        })
        put_json(f"blockstates/{name}.json", {"variants": {
            "axis=y": {"model": f"{NS}:block/{name}"},
            "axis=z": {"model": f"{NS}:block/{name}", "x": 90},
            "axis=x": {"model": f"{NS}:block/{name}", "x": 90, "y": 90},
        }})
        put_json(f"items/{name}.json", {"model": {"type": "minecraft:model", "model": f"{NS}:block/{name}"}})
        put_json(f"models/item/{name}.json", {"parent": f"{NS}:block/{name}"})
        written += 4

    # 花与草
    for name in ("astral_bloom", "jade_lily", "yao_bloom",
                 "astral_grass", "jade_grass", "yao_grass",
                 "ziwei_bloom", "liuren_lily", "qimen_bloom",
                 "ziwei_grass", "liuren_grass", "qimen_grass"):
        put_json(f"models/block/{name}.json", {
            "parent": "minecraft:block/cross",
            "textures": {"cross": f"{NS}:block/{name}"},
        })
        put_json(f"blockstates/{name}.json", {"variants": {"": {"model": f"{NS}:block/{name}"}}})
        put_json(f"items/{name}.json", {"model": {"type": "minecraft:model", "model": f"{NS}:block/{name}"}})
        put_json(f"models/item/{name}.json", {"parent": f"{NS}:block/{name}"})
        written += 4

    # 门（没有物品形式，只需要世界渲染资源）
    for realm in ("astral", "jade", "yao", "ziwei", "liuren", "qimen"):
        name = f"{realm}_gate"
        put_json(f"models/block/{name}.json", {
            "parent": "minecraft:block/cube_all",
            "textures": {"all": {"sprite": f"{NS}:block/{name}", "force_translucent": True}},
        })
        put_json(f"blockstates/{name}.json", {"variants": {"": {"model": f"{NS}:block/{name}"}}})
        written += 2

    # 台阶
    for realm in ("astral", "jade", "yao"):
        name = f"{realm}_slab"
        bricks = f"{NS}:block/{realm}_bricks"
        put_json(f"models/block/{name}.json", {
            "parent": "minecraft:block/slab",
            "textures": {"bottom": bricks, "side": bricks, "top": bricks},
        })
        put_json(f"models/block/{name}_top.json", {
            "parent": "minecraft:block/slab_top",
            "textures": {"bottom": bricks, "side": bricks, "top": bricks},
        })
        put_json(f"blockstates/{name}.json", {"variants": {
            "type=bottom": {"model": f"{NS}:block/{name}"},
            "type=double": {"model": bricks},
            "type=top": {"model": f"{NS}:block/{name}_top"},
        }})
        put_json(f"items/{name}.json", {"model": {"type": "minecraft:model", "model": f"{NS}:block/{name}"}})
        put_json(f"models/item/{name}.json", {"parent": f"{NS}:block/{name}"})
        written += 5

    # 楼梯
    for realm in ("astral", "jade", "yao"):
        name = f"{realm}_stairs"
        bricks = f"{NS}:block/{realm}_bricks"
        for suffix, parent in (("", "stairs"), ("_inner", "stairs_inner"), ("_outer", "stairs_outer")):
            put_json(f"models/block/{name}{suffix}.json", {
                "parent": f"minecraft:block/{parent}",
                "textures": {"bottom": bricks, "side": bricks, "top": bricks},
            })
        put_json(f"blockstates/{name}.json", stairs_blockstate(name))
        put_json(f"items/{name}.json", {"model": {"type": "minecraft:model", "model": f"{NS}:block/{name}"}})
        put_json(f"models/item/{name}.json", {"parent": f"{NS}:block/{name}"})
        written += 6

    # 原木（九种）
    for species in WOOD_COLORS:
        name = f"{species}_log"
        put_json(f"models/block/{name}.json", {
            "parent": "minecraft:block/cube_column",
            "textures": {"side": f"{NS}:block/{species}_log_side",
                         "end": f"{NS}:block/{species}_log_top"},
        })
        put_json(f"blockstates/{name}.json", {"variants": {
            "axis=y": {"model": f"{NS}:block/{name}"},
            "axis=z": {"model": f"{NS}:block/{name}", "x": 90},
            "axis=x": {"model": f"{NS}:block/{name}", "x": 90, "y": 90},
        }})
        put_json(f"items/{name}.json", {"model": {"type": "minecraft:model", "model": f"{NS}:block/{name}"}})
        put_json(f"models/item/{name}.json", {"parent": f"{NS}:block/{name}"})
        written += 4

    # 树叶（九种）：不带 tintindex 的自绘立方体 —— 颜色画在贴图里，
    # 附带暗色裁剪 mipmap（与vanilla树叶同款，避免远景发灰）。
    for species in WOOD_COLORS:
        name = f"{species}_leaves"
        tex = f"{NS}:block/{name}"
        faces = {d: {"uv": [0, 0, 16, 16], "texture": "#all", "cullface": d}
                 for d in ("down", "up", "north", "south", "west", "east")}
        put_json(f"models/block/{name}.json", {
            "parent": "minecraft:block/block",
            "textures": {"particle": tex, "all": tex},
            "elements": [{"from": [0, 0, 0], "to": [16, 16, 16], "faces": faces}],
        })
        put_json(f"blockstates/{name}.json", {"variants": {"": {"model": tex}}})
        put_json(f"textures/block/{name}.png.mcmeta",
                 {"texture": {"mipmap_strategy": "dark_cutout"}})
        put_json(f"items/{name}.json", {"model": {"type": "minecraft:model", "model": tex}})
        put_json(f"models/item/{name}.json", {"parent": tex})
        written += 5

    # 树苗（九种）
    for species in WOOD_COLORS:
        name = f"{species}_sapling"
        put_json(f"models/block/{name}.json", {
            "parent": "minecraft:block/cross",
            "textures": {"cross": f"{NS}:block/{name}"},
        })
        put_json(f"blockstates/{name}.json", {"variants": {"": {"model": f"{NS}:block/{name}"}}})
        put_json(f"items/{name}.json", {"model": {"type": "minecraft:model", "model": f"{NS}:block/{name}"}})
        put_json(f"models/item/{name}.json", {"parent": f"{NS}:block/{name}"})
        written += 4

    # 纯物品
    for name, _factory in item_tex:
        put_json(f"items/{name}.json", {"model": {"type": "minecraft:model", "model": f"{NS}:item/{name}"}})
        put_json(f"models/item/{name}.json", {
            "parent": "minecraft:item/generated",
            "textures": {"layer0": f"{NS}:item/{name}"},
        })
        written += 2

    # 液体方块：单变体 blockstate + 只管粒子的空模型（照原版 water.json 的样子）
    for name in ("astral_dew", "jade_ichor", "yao_ink",
                 "ziwei_ambrosia", "liuren_water", "qimen_mercury"):
        put_json(f"blockstates/{name}.json", {"variants": {"": {"model": f"{NS}:block/{name}"}}})
        put_json(f"models/block/{name}.json", {
            "textures": {"particle": f"{NS}:block/{name}_still"},
        })
        written += 2

    return written


REALM_LANG_ZH = {
    # 旧作物方块名（无物品形式，但 F3 与调试信息会显示）
    "block.yishu.astral_wheat": "星辉穗",
    "block.yishu.jade_rice": "玉粒稻",
    "block.yishu.yao_bean": "爻纹豆",
    # ---- 第 16 轮：三术界 ----
    "block.yishu.ziwei_crystal_ore": "紫微晶矿",
    "block.yishu.dou_ore": "斗枢矿",
    "block.yishu.ji_ore": "天机砂矿",
    "block.yishu.yuan_ore": "紫垣矿",
    "block.yishu.ziwei_wood_log": "紫微原木",
    "block.yishu.ziwei_wood_leaves": "紫微树叶",
    "block.yishu.ziwei_wood_sapling": "紫微树苗",
    "block.yishu.dou_wood_log": "斗枢原木",
    "block.yishu.dou_wood_leaves": "斗枢树叶",
    "block.yishu.dou_wood_sapling": "斗枢树苗",
    "block.yishu.tianji_wood_log": "天机原木",
    "block.yishu.tianji_wood_leaves": "天机树叶",
    "block.yishu.tianji_wood_sapling": "天机树苗",
    "block.yishu.ziwei_bloom": "紫微花",
    "block.yishu.ziwei_grass": "紫微草",
    "block.yishu.ziwei_wheat": "紫微穗",
    "block.yishu.ziwei_gate": "紫微门",
    "block.yishu.ziwei_ambrosia": "紫微露",
    "block.yishu.guiren_altar": "贵人台",
    "block.yishu.bamen_altar": "八门台",
    "item.yishu.ziwei_crystal": "紫微晶",
    "item.yishu.ziwei_essence": "紫微精",
    "item.yishu.ziwei_key": "紫微灵引",
    "item.yishu.ziwei_ambrosia_bucket": "紫微露桶",
    "block.yishu.ren_ore": "壬水矿",
    "block.yishu.gui_ore": "贵人矿",
    "block.yishu.she_ore": "螣蛇矿",
    "block.yishu.xuan_ore": "玄武矿",
    "block.yishu.ren_wood_log": "壬水原木",
    "block.yishu.ren_wood_leaves": "壬水树叶",
    "block.yishu.ren_wood_sapling": "壬水树苗",
    "block.yishu.gui_wood_log": "贵人原木",
    "block.yishu.gui_wood_leaves": "贵人树叶",
    "block.yishu.gui_wood_sapling": "贵人树苗",
    "block.yishu.she_wood_log": "螣蛇原木",
    "block.yishu.she_wood_leaves": "螣蛇树叶",
    "block.yishu.she_wood_sapling": "螣蛇树苗",
    "block.yishu.liuren_lily": "壬水百合",
    "block.yishu.liuren_grass": "壬水草",
    "block.yishu.liuren_rice": "壬水稻",
    "block.yishu.liuren_gate": "壬水门",
    "block.yishu.liuren_water": "壬水",
    "item.yishu.liuren_crystal": "壬水珠",
    "item.yishu.liuren_essence": "壬水精",
    "item.yishu.liuren_key": "壬水灵引",
    "item.yishu.liuren_water_bucket": "壬水桶",
    "block.yishu.men_ore": "八门矿",
    "block.yishu.xing_ore": "九星矿",
    "block.yishu.dun_ore": "遁甲矿",
    "block.yishu.jia_ore": "甲子矿",
    "block.yishu.men_wood_log": "八门原木",
    "block.yishu.men_wood_leaves": "八门树叶",
    "block.yishu.men_wood_sapling": "八门树苗",
    "block.yishu.xing_wood_log": "九星原木",
    "block.yishu.xing_wood_leaves": "九星树叶",
    "block.yishu.xing_wood_sapling": "九星树苗",
    "block.yishu.dun_wood_log": "遁甲原木",
    "block.yishu.dun_wood_leaves": "遁甲树叶",
    "block.yishu.dun_wood_sapling": "遁甲树苗",
    "block.yishu.qimen_bloom": "八门花",
    "block.yishu.qimen_grass": "八门草",
    "block.yishu.qimen_bean": "八门豆",
    "block.yishu.qimen_gate": "八门",
    "block.yishu.qimen_mercury": "遁甲汞",
    "item.yishu.qimen_crystal": "八门金",
    "item.yishu.qimen_essence": "九星精",
    "item.yishu.qimen_key": "八门灵引",
    "item.yishu.qimen_mercury_bucket": "遁甲汞桶",
    "block.yishu.astral_ore": "星辉矿石",
    "block.yishu.jade_ore": "青玉矿石",
    "block.yishu.yao_ore": "爻纹矿石",
    "block.yishu.astral_pillar": "星辉柱",
    "block.yishu.jade_pillar": "青玉柱",
    "block.yishu.yao_pillar": "爻纹柱",
    "block.yishu.astral_slab": "星辉台阶",
    "block.yishu.jade_slab": "青玉台阶",
    "block.yishu.yao_slab": "爻纹台阶",
    "block.yishu.astral_stairs": "星辉楼梯",
    "block.yishu.jade_stairs": "青玉楼梯",
    "block.yishu.yao_stairs": "爻纹楼梯",
    "block.yishu.astral_bloom": "星辉花",
    "block.yishu.jade_lily": "青玉兰",
    "block.yishu.yao_bloom": "爻纹花",
    "block.yishu.astral_grass": "星纹草",
    "block.yishu.jade_grass": "玉须草",
    "block.yishu.yao_grass": "爻草",
    "block.yishu.astral_gate": "星门",
    "block.yishu.jade_gate": "玉门",
    "block.yishu.yao_gate": "玄门",
    "block.yishu.astral_dew": "星辉露",
    "block.yishu.jade_ichor": "玉髓浆",
    "block.yishu.yao_ink": "玄墨水",
    "item.yishu.astral_dust": "星辉尘",
    "item.yishu.jade_grit": "青玉砂",
    "item.yishu.yao_shard": "爻纹砾",
    "item.yishu.astral_dust.note": "命理界的矿尘。四粒加一方阴阳石，可铸成星枢引。",
    "item.yishu.jade_grit.note": "风水界的矿砂。四粒加一方阴阳石，可铸成玉髓引。",
    "item.yishu.yao_shard.note": "六爻界的矿砾。四粒加一方阴阳石，可铸成爻纹引。",
    "item.yishu.astral_key": "星枢引",
    "item.yishu.jade_key": "玉髓引",
    "item.yishu.yao_key": "爻纹引",
    "item.yishu.astral_dew_bucket": "星辉露桶",
    "item.yishu.jade_ichor_bucket": "玉髓浆桶",
    "item.yishu.yao_ink_bucket": "玄墨水桶",
    # —— 第 10 轮：树种 / 新矿 / 新材料 / 新灵兽蛋 ——
    "block.yishu.astral_wood_log": "星辉原木",
    "block.yishu.meteor_wood_log": "陨枝原木",
    "block.yishu.moon_wood_log": "月华原木",
    "block.yishu.jade_wood_log": "青玉原木",
    "block.yishu.cloud_wood_log": "云松原木",
    "block.yishu.peach_wood_log": "灵桃原木",
    "block.yishu.hexa_wood_log": "爻纹原木",
    "block.yishu.ink_wood_log": "玄墨原木",
    "block.yishu.mystery_wood_log": "幽玄原木",
    "block.yishu.astral_wood_leaves": "星辉树叶",
    "block.yishu.meteor_wood_leaves": "陨枝树叶",
    "block.yishu.moon_wood_leaves": "月华树叶",
    "block.yishu.jade_wood_leaves": "青玉树叶",
    "block.yishu.cloud_wood_leaves": "云松树叶",
    "block.yishu.peach_wood_leaves": "灵桃树叶",
    "block.yishu.hexa_wood_leaves": "爻纹树叶",
    "block.yishu.ink_wood_leaves": "玄墨树叶",
    "block.yishu.mystery_wood_leaves": "幽玄树叶",
    "block.yishu.astral_wood_sapling": "星辉树苗",
    "block.yishu.meteor_wood_sapling": "陨枝树苗",
    "block.yishu.moon_wood_sapling": "月华树苗",
    "block.yishu.jade_wood_sapling": "青玉树苗",
    "block.yishu.cloud_wood_sapling": "云松树苗",
    "block.yishu.peach_wood_sapling": "灵桃树苗",
    "block.yishu.hexa_wood_sapling": "爻纹树苗",
    "block.yishu.ink_wood_sapling": "玄墨树苗",
    "block.yishu.mystery_wood_sapling": "幽玄树苗",
    "block.yishu.meteor_ore": "陨星矿石",
    "block.yishu.star_sand_ore": "星砂矿石",
    "block.yishu.crystal_ore": "星晶矿石",
    "block.yishu.verdant_ore": "翠玉矿石",
    "block.yishu.mica_ore": "云母矿石",
    "block.yishu.vein_ore": "灵脉矿石",
    "block.yishu.ink_iron_ore": "玄铁矿石",
    "block.yishu.onyx_ore": "墨玉矿石",
    "block.yishu.oracle_ore": "卜骨矿石",
    "item.yishu.meteor_iron": "陨铁",
    "item.yishu.star_sand": "星砂",
    "item.yishu.star_crystal": "星晶",
    "item.yishu.verdant_gem": "翠玉",
    "item.yishu.mica": "云母",
    "item.yishu.vein_crystal": "灵脉晶",
    "item.yishu.ink_iron": "玄铁",
    "item.yishu.onyx": "墨玉",
    "item.yishu.oracle_bone": "卜骨",
    "item.yishu.meteor_iron.note": "命理界的陨铁。四块加一方阴阳石，可铸星枢引。",
    "item.yishu.star_sand.note": "命理界的星砂。四粒加一方阴阳石，可铸星枢引。",
    "item.yishu.star_crystal.note": "命理界的星晶。四块加一方阴阳石，可铸星枢引。",
    "item.yishu.verdant_gem.note": "风水界的翠玉。四颗加一方阴阳石，可铸玉髓引。",
    "item.yishu.mica.note": "风水界的云母。四片加一方阴阳石，可铸玉髓引。",
    "item.yishu.vein_crystal.note": "风水界的灵脉晶。四枚加一方阴阳石，可铸玉髓引。",
    "item.yishu.ink_iron.note": "六爻界的玄铁。四块加一方阴阳石，可铸爻纹引。",
    "item.yishu.onyx.note": "六爻界的墨玉。四枚加一方阴阳石，可铸爻纹引。",
    "item.yishu.oracle_bone.note": "六爻界的卜骨。四片加一方阴阳石，可铸爻纹引。",
    # ---- 三术界：能铸灵引的十二种材料（第 17 轮补）----
    "item.yishu.ziwei_crystal.note": "紫微界的晶石。四块加一方阴阳石，可铸成紫微灵引。",
    "item.yishu.ziwei_essence.note": "紫微界矿石里的一缕精粹。四枚加一方阴阳石，可铸成紫微灵引。",
    "item.yishu.ziwei_grain.note": "紫微界田里收的穗子。四束加一方阴阳石，也能铸紫微灵引。",
    "item.yishu.liuren_crystal.note": "六壬界的凝珠。四颗加一方阴阳石，可铸成壬水灵引。",
    "item.yishu.liuren_essence.note": "六壬界的精粹。四枚加一方阴阳石，可铸成壬水灵引。",
    "item.yishu.liuren_grain.note": "六壬界水田的稻粒。四捧加一方阴阳石，也能铸壬水灵引。",
    "item.yishu.qimen_crystal.note": "奇门界的金晶。四块加一方阴阳石，可铸成八门灵引。",
    "item.yishu.qimen_essence.note": "九星凝下的精粹。四枚加一方阴阳石，可铸成八门灵引。",
    "item.yishu.qimen_grain.note": "奇门界田里的豆子。四把加一方阴阳石，也能铸八门灵引。",
    "item.yishu.star_watcher_spawn_egg": "观星者刷怪蛋",
    "item.yishu.star_fox_spawn_egg": "星狐刷怪蛋",
    "item.yishu.star_slime_spawn_egg": "星屑史莱姆刷怪蛋",
    "item.yishu.jade_rabbit_spawn_egg": "玉兔刷怪蛋",
    "item.yishu.jade_goat_spawn_egg": "玉羊刷怪蛋",
    "item.yishu.vein_turtle_spawn_egg": "地脉龟刷怪蛋",
    "item.yishu.ink_wolf_spawn_egg": "爻狼刷怪蛋",
    "item.yishu.ink_bat_spawn_egg": "爻蝠刷怪蛋",
    "item.yishu.hex_phantom_spawn_egg": "卦影刷怪蛋",
}

REALM_LANG_EN = {
    # Legacy crop block names
    "block.yishu.astral_wheat": "Astral Wheat",
    "block.yishu.jade_rice": "Jade Rice",
    "block.yishu.yao_bean": "Yao Bean",
    # ---- 第 16 轮：三术界 ----
    "block.yishu.ziwei_crystal_ore": "Zi Wei Crystal Ore",
    "block.yishu.dou_ore": "Dou Shu Ore",
    "block.yishu.ji_ore": "Tian Ji Sand Ore",
    "block.yishu.yuan_ore": "Zi Yuan Ore",
    "block.yishu.ziwei_wood_log": "Zi Wei Log",
    "block.yishu.ziwei_wood_leaves": "Zi Wei Leaves",
    "block.yishu.ziwei_wood_sapling": "Zi Wei Sapling",
    "block.yishu.dou_wood_log": "Dou Shu Log",
    "block.yishu.dou_wood_leaves": "Dou Shu Leaves",
    "block.yishu.dou_wood_sapling": "Dou Shu Sapling",
    "block.yishu.tianji_wood_log": "Tian Ji Log",
    "block.yishu.tianji_wood_leaves": "Tian Ji Leaves",
    "block.yishu.tianji_wood_sapling": "Tian Ji Sapling",
    "block.yishu.ziwei_bloom": "Zi Wei Bloom",
    "block.yishu.ziwei_grass": "Zi Wei Grass",
    "block.yishu.ziwei_wheat": "Zi Wei Wheat",
    "block.yishu.ziwei_gate": "Zi Wei Gate",
    "block.yishu.ziwei_ambrosia": "Zi Wei Ambrosia",
    "block.yishu.guiren_altar": "Gui Ren Altar",
    "block.yishu.bamen_altar": "Ba Men Altar",
    "item.yishu.ziwei_crystal": "Zi Wei Crystal",
    "item.yishu.ziwei_essence": "Zi Wei Essence",
    "item.yishu.ziwei_key": "Zi Wei Key",
    "item.yishu.ziwei_ambrosia_bucket": "Zi Wei Ambrosia Bucket",
    "block.yishu.ren_ore": "Ren Water Ore",
    "block.yishu.gui_ore": "Gui Ren Ore",
    "block.yishu.she_ore": "Teng She Ore",
    "block.yishu.xuan_ore": "Xuan Wu Ore",
    "block.yishu.ren_wood_log": "Ren Water Log",
    "block.yishu.ren_wood_leaves": "Ren Water Leaves",
    "block.yishu.ren_wood_sapling": "Ren Water Sapling",
    "block.yishu.gui_wood_log": "Gui Ren Log",
    "block.yishu.gui_wood_leaves": "Gui Ren Leaves",
    "block.yishu.gui_wood_sapling": "Gui Ren Sapling",
    "block.yishu.she_wood_log": "Teng She Log",
    "block.yishu.she_wood_leaves": "Teng She Leaves",
    "block.yishu.she_wood_sapling": "Teng She Sapling",
    "block.yishu.liuren_lily": "Liu Ren Lily",
    "block.yishu.liuren_grass": "Liu Ren Grass",
    "block.yishu.liuren_rice": "Liu Ren Rice",
    "block.yishu.liuren_gate": "Liu Ren Gate",
    "block.yishu.liuren_water": "Liu Ren Water",
    "item.yishu.liuren_crystal": "Liu Ren Crystal",
    "item.yishu.liuren_essence": "Liu Ren Essence",
    "item.yishu.liuren_key": "Liu Ren Key",
    "item.yishu.liuren_water_bucket": "Liu Ren Water Bucket",
    "block.yishu.men_ore": "Ba Men Ore",
    "block.yishu.xing_ore": "Jiu Xing Ore",
    "block.yishu.dun_ore": "Dun Jia Ore",
    "block.yishu.jia_ore": "Jia Zi Ore",
    "block.yishu.men_wood_log": "Ba Men Log",
    "block.yishu.men_wood_leaves": "Ba Men Leaves",
    "block.yishu.men_wood_sapling": "Ba Men Sapling",
    "block.yishu.xing_wood_log": "Jiu Xing Log",
    "block.yishu.xing_wood_leaves": "Jiu Xing Leaves",
    "block.yishu.xing_wood_sapling": "Jiu Xing Sapling",
    "block.yishu.dun_wood_log": "Dun Jia Log",
    "block.yishu.dun_wood_leaves": "Dun Jia Leaves",
    "block.yishu.dun_wood_sapling": "Dun Jia Sapling",
    "block.yishu.qimen_bloom": "Qi Men Bloom",
    "block.yishu.qimen_grass": "Qi Men Grass",
    "block.yishu.qimen_bean": "Qi Men Bean",
    "block.yishu.qimen_gate": "Qi Men Gate",
    "block.yishu.qimen_mercury": "Qi Men Mercury",
    "item.yishu.qimen_crystal": "Qi Men Crystal",
    "item.yishu.qimen_essence": "Qi Men Essence",
    "item.yishu.qimen_key": "Qi Men Key",
    "item.yishu.qimen_mercury_bucket": "Qi Men Mercury Bucket",
    "block.yishu.astral_ore": "Astral Ore",
    "block.yishu.jade_ore": "Jade Ore",
    "block.yishu.yao_ore": "Hexagram Ore",
    "block.yishu.astral_pillar": "Astral Pillar",
    "block.yishu.jade_pillar": "Jade Pillar",
    "block.yishu.yao_pillar": "Hexagram Pillar",
    "block.yishu.astral_slab": "Astral Slab",
    "block.yishu.jade_slab": "Jade Slab",
    "block.yishu.yao_slab": "Hexagram Slab",
    "block.yishu.astral_stairs": "Astral Stairs",
    "block.yishu.jade_stairs": "Jade Stairs",
    "block.yishu.yao_stairs": "Hexagram Stairs",
    "block.yishu.astral_bloom": "Astral Bloom",
    "block.yishu.jade_lily": "Jade Lily",
    "block.yishu.yao_bloom": "Hexagram Bloom",
    "block.yishu.astral_grass": "Astral Grass",
    "block.yishu.jade_grass": "Jade Grass",
    "block.yishu.yao_grass": "Hexagram Grass",
    "block.yishu.astral_gate": "Astral Gate",
    "block.yishu.jade_gate": "Jade Gate",
    "block.yishu.yao_gate": "Ink Gate",
    "block.yishu.astral_dew": "Astral Dew",
    "block.yishu.jade_ichor": "Jade Ichor",
    "block.yishu.yao_ink": "Ink of Hexagrams",
    "item.yishu.astral_dust": "Astral Dust",
    "item.yishu.jade_grit": "Jade Grit",
    "item.yishu.yao_shard": "Hexagram Shard",
    "item.yishu.astral_dust.note": "Ore dust of the Fate Realm. Four plus a yin-yang stone forge an Astral Key.",
    "item.yishu.jade_grit.note": "Ore grit of the Feng Shui Realm. Four plus a yin-yang stone forge a Jade Key.",
    "item.yishu.yao_shard.note": "Ore shard of the Liu Yao Realm. Four plus a yin-yang stone forge the Ink Key.",
    "item.yishu.astral_key": "Astral Key",
    "item.yishu.jade_key": "Jade Key",
    "item.yishu.yao_key": "Hexagram Key",
    "item.yishu.astral_dew_bucket": "Bucket of Astral Dew",
    "item.yishu.jade_ichor_bucket": "Bucket of Jade Ichor",
    "item.yishu.yao_ink_bucket": "Bucket of Hexagram Ink",
    "block.yishu.astral_wood_log": "Astral Wood Log",
    "block.yishu.meteor_wood_log": "Meteor Wood Log",
    "block.yishu.moon_wood_log": "Moonlit Wood Log",
    "block.yishu.jade_wood_log": "Jade Wood Log",
    "block.yishu.cloud_wood_log": "Cloud Pine Log",
    "block.yishu.peach_wood_log": "Spirit Peach Log",
    "block.yishu.hexa_wood_log": "Hexagram Wood Log",
    "block.yishu.ink_wood_log": "Inkwood Log",
    "block.yishu.mystery_wood_log": "Mystic Wood Log",
    "block.yishu.astral_wood_leaves": "Astral Wood Leaves",
    "block.yishu.meteor_wood_leaves": "Meteor Wood Leaves",
    "block.yishu.moon_wood_leaves": "Moonlit Wood Leaves",
    "block.yishu.jade_wood_leaves": "Jade Wood Leaves",
    "block.yishu.cloud_wood_leaves": "Cloud Pine Leaves",
    "block.yishu.peach_wood_leaves": "Spirit Peach Leaves",
    "block.yishu.hexa_wood_leaves": "Hexagram Wood Leaves",
    "block.yishu.ink_wood_leaves": "Inkwood Leaves",
    "block.yishu.mystery_wood_leaves": "Mystic Wood Leaves",
    "block.yishu.astral_wood_sapling": "Astral Wood Sapling",
    "block.yishu.meteor_wood_sapling": "Meteor Wood Sapling",
    "block.yishu.moon_wood_sapling": "Moonlit Wood Sapling",
    "block.yishu.jade_wood_sapling": "Jade Wood Sapling",
    "block.yishu.cloud_wood_sapling": "Cloud Pine Sapling",
    "block.yishu.peach_wood_sapling": "Spirit Peach Sapling",
    "block.yishu.hexa_wood_sapling": "Hexagram Wood Sapling",
    "block.yishu.ink_wood_sapling": "Inkwood Sapling",
    "block.yishu.mystery_wood_sapling": "Mystic Wood Sapling",
    "block.yishu.meteor_ore": "Meteor Ore",
    "block.yishu.star_sand_ore": "Star Sand Ore",
    "block.yishu.crystal_ore": "Star Crystal Ore",
    "block.yishu.verdant_ore": "Verdant Ore",
    "block.yishu.mica_ore": "Mica Ore",
    "block.yishu.vein_ore": "Vein Crystal Ore",
    "block.yishu.ink_iron_ore": "Ink Iron Ore",
    "block.yishu.onyx_ore": "Onyx Ore",
    "block.yishu.oracle_ore": "Oracle Bone Ore",
    "item.yishu.meteor_iron": "Meteor Iron",
    "item.yishu.star_sand": "Star Sand",
    "item.yishu.star_crystal": "Star Crystal",
    "item.yishu.verdant_gem": "Verdant Gem",
    "item.yishu.mica": "Mica",
    "item.yishu.vein_crystal": "Vein Crystal",
    "item.yishu.ink_iron": "Ink Iron",
    "item.yishu.onyx": "Onyx",
    "item.yishu.oracle_bone": "Oracle Bone",
    "item.yishu.meteor_iron.note": "Ore of the Fate Realm. Four plus a yin-yang stone forge an Astral Key.",
    "item.yishu.star_sand.note": "Ore of the Fate Realm. Four plus a yin-yang stone forge an Astral Key.",
    "item.yishu.star_crystal.note": "Ore of the Fate Realm. Four plus a yin-yang stone forge an Astral Key.",
    "item.yishu.verdant_gem.note": "Ore of the Feng Shui Realm. Four plus a yin-yang stone forge a Jade Key.",
    "item.yishu.mica.note": "Ore of the Feng Shui Realm. Four plus a yin-yang stone forge a Jade Key.",
    "item.yishu.vein_crystal.note": "Ore of the Feng Shui Realm. Four plus a yin-yang stone forge a Jade Key.",
    "item.yishu.ink_iron.note": "Ore of the Liu Yao Realm. Four plus a yin-yang stone forge the Ink Key.",
    "item.yishu.onyx.note": "Ore of the Liu Yao Realm. Four plus a yin-yang stone forge the Ink Key.",
    "item.yishu.oracle_bone.note": "Ore of the Liu Yao Realm. Four plus a yin-yang stone forge the Ink Key.",
    "item.yishu.ziwei_crystal.note": "Crystal of the Zi Wei Realm. Four plus a yin-yang stone forge a Zi Wei Key.",
    "item.yishu.ziwei_essence.note": "Essence of the Zi Wei Realm. Four plus a yin-yang stone forge a Zi Wei Key.",
    "item.yishu.ziwei_grain.note": "Grain of the Zi Wei Realm. Four plus a yin-yang stone forge a Zi Wei Key.",
    "item.yishu.liuren_crystal.note": "Bead of the Liu Ren Realm. Four plus a yin-yang stone forge a Liu Ren Key.",
    "item.yishu.liuren_essence.note": "Essence of the Liu Ren Realm. Four plus a yin-yang stone forge a Liu Ren Key.",
    "item.yishu.liuren_grain.note": "Rice of the Liu Ren Realm. Four plus a yin-yang stone forge a Liu Ren Key.",
    "item.yishu.qimen_crystal.note": "Metal of the Qi Men Realm. Four plus a yin-yang stone forge a Qi Men Key.",
    "item.yishu.qimen_essence.note": "Essence of the Nine Stars. Four plus a yin-yang stone forge a Qi Men Key.",
    "item.yishu.qimen_grain.note": "Beans of the Qi Men Realm. Four plus a yin-yang stone forge a Qi Men Key.",
    "item.yishu.star_watcher_spawn_egg": "Star Watcher Spawn Egg",
    "item.yishu.star_fox_spawn_egg": "Star Fox Spawn Egg",
    "item.yishu.star_slime_spawn_egg": "Stardust Slime Spawn Egg",
    "item.yishu.jade_rabbit_spawn_egg": "Jade Rabbit Spawn Egg",
    "item.yishu.jade_goat_spawn_egg": "Jade Goat Spawn Egg",
    "item.yishu.vein_turtle_spawn_egg": "Vein Turtle Spawn Egg",
    "item.yishu.ink_wolf_spawn_egg": "Ink Wolf Spawn Egg",
    "item.yishu.ink_bat_spawn_egg": "Ink Bat Spawn Egg",
    "item.yishu.hex_phantom_spawn_egg": "Hex Phantom Spawn Egg",
}


def tex_dust_ore(base, hi, dark):
    """粗矿：一小堆颗粒。"""
    c = Canvas()
    spots = [(5, 10), (7, 9), (9, 10), (6, 12), (8, 12), (10, 12), (7, 11),
             (9, 8), (6, 8), (8, 10), (10, 9), (5, 12)]
    for x, y in spots:
        c.put(x, y, base)
    for x, y in ((6, 9), (8, 9), (7, 10), (9, 11)):
        c.put(x, y, hi)
    for x, y in ((5, 11), (9, 13), (11, 11)):
        c.put(x, y, dark)
    c.put(7, 8, hi)
    c.put(8, 8, base)
    return c




# ------------------------------------------------------------------ 三大术数（第 15 轮）

# 三张盘的配色：(深底, 中框, 亮盘)
ORACLE_DISC_STYLE = {
    "ziwei": (rgb("2E2450"), rgb("8E7ED8"), rgb("E8E0FF")),
    "liuren": (rgb("3A2A1E"), rgb("C89A50"), rgb("F0DCB0")),
    "qimen": (rgb("26262E"), rgb("7A7A8E"), rgb("C8C8D8")),
}


def tex_oracle_disc(kind):
    """术盘：八边形外框 + 内圆盘，三种术三套纹样。"""
    deep, mid, hi = ORACLE_DISC_STYLE[kind]
    c = Canvas()
    # 外框（方形切角 = 八边形）
    c.mask(lambda x, y: abs(x - 7.5) <= 7.2 and abs(y - 7.5) <= 7.2
           and abs(x - 7.5) + abs(y - 7.5) <= 11.6, deep)
    c.mask(lambda x, y: abs(x - 7.5) <= 6.0 and abs(y - 7.5) <= 6.0
           and abs(x - 7.5) + abs(y - 7.5) <= 9.4, mid)
    # 盘面
    c.mask(lambda x, y: (x - 7.5) ** 2 + (y - 7.5) ** 2 <= 17.0, hi)

    if kind == "ziwei":
        # 北斗七星
        for x, y in [(4, 5), (5, 4), (6, 5), (7, 6), (8, 7), (9, 8), (10, 9)]:
            c.put(x, y, deep)
        c.put(5, 10, mid)
        c.put(6, 11, mid)
    elif kind == "liuren":
        # 四正四隅刻度
        for x, y in [(7, 2), (8, 2), (7, 13), (8, 13),
                     (2, 7), (2, 8), (13, 7), (13, 8),
                     (4, 4), (11, 4), (4, 11), (11, 11)]:
            c.put(x, y, deep)
        c.put(7, 7, deep)
        c.put(8, 8, deep)
        c.put(7, 8, mid)
        c.put(8, 7, mid)
    else:
        # 九宫格
        for i in (4, 7, 10):
            c.hline(4, 11, i, deep)
            c.vline(i, 4, 11, deep)
        c.put(7, 7, deep)
        c.put(8, 8, deep)

    c.shadow_bottom(deep, 55)
    return c


# 九道术符的符头配色
ORACLE_CHARM_STYLE = {
    "ziwei": rgb("8E7ED8"),
    "tianfu": rgb("C8A040"),
    "qisha": rgb("B03A3A"),
    "guiren": rgb("D8B040"),
    "qinglong": rgb("3E9A78"),
    "baihu": rgb("C8C8D0"),
    "shengmen": rgb("5FA85F"),
    "kaimen": rgb("D8A030"),
    "xiumen": rgb("5A7AB0"),
}


def tex_oracle_charm(kind):
    """术符：与普通符箓同一版式，符头颜色分辨门派。"""
    accent = ORACLE_CHARM_STYLE[kind]
    c = Canvas()
    c.rect(4, 1, 11, 14, PAPER)
    c.frame(4, 1, 11, 14, PAPER_SHADE)
    c.rect(6, 2, 9, 4, accent)
    c.put(7, 3, PAPER)
    c.put(8, 3, PAPER)
    c.vline(7, 6, 12, CINNABAR)
    c.vline(8, 6, 12, CINNABAR)
    c.hline(6, 9, 8, CINNABAR)
    c.hline(6, 9, 11, CINNABAR)
    c.put(5, 9, accent)
    c.put(10, 9, accent)
    c.put(6, 14, CINNABAR)
    c.put(9, 14, CINNABAR)
    return c


# 三座术坛的配色：(底座深色, 台面, 亮饰)
ORACLE_ALTAR_STYLE = {
    "ziwei": (rgb("221A3C"), rgb("4A3E78"), rgb("C8BCF4")),
    "liuren": (rgb("2E2118"), rgb("5E462A"), rgb("D8B878")),
    "qimen": (rgb("1E1E26"), rgb("3E3E4A"), rgb("9E9EB0")),
}


def tex_block_oracle_altar(kind):
    """术坛：石台上嵌一面盘，三种术三套中心纹。"""
    dark, mid, hi = ORACLE_ALTAR_STYLE[kind]
    c = Canvas()
    c.rect(0, 0, 15, 15, dark)
    c.rect(1, 1, 14, 14, mid)
    c.frame(1, 1, 14, 14, dark)
    # 中央盘
    c.mask(lambda x, y: (x - 7.5) ** 2 + (y - 7.5) ** 2 <= 24.0, dark)
    c.mask(lambda x, y: (x - 7.5) ** 2 + (y - 7.5) ** 2 <= 16.0, hi)
    c.mask(lambda x, y: (x - 7.5) ** 2 + (y - 7.5) ** 2 <= 5.0, mid)

    if kind == "ziwei":
        for x, y in [(6, 5), (9, 6), (7, 9), (9, 9)]:
            c.put(x, y, hi)
    elif kind == "liuren":
        c.hline(5, 10, 7, hi)
        c.vline(7, 4, 11, hi)
        c.put(8, 8, hi)
    else:
        for i in (4, 7, 10):
            c.hline(4, 11, i, mid)
            c.vline(i, 4, 11, mid)
        c.put(7, 7, dark)
        c.put(8, 8, dark)

    for x, y in [(2, 2), (13, 2), (2, 13), (13, 13)]:
        c.put(x, y, hi)
    c.shadow_bottom(dark, 45)
    return c


# ------------------------------------------------------------------ 资源清单

# (id, 生成函数, 中文名, 英文名, 是否方块)
ITEM_TEXTURES = [
    ("fate_slip", tex_fate_slip, "命盘玉简", "Fate Slip"),
    ("feng_shui_compass", tex_compass, "风水罗盘", "Feng Shui Compass"),
    ("dragon_ruler", tex_dragon_ruler, "寻龙尺", "Dragon Ruler"),
    ("peek_charm", tex_peek_charm, "窥命符", "Peeking Charm"),
    ("bronze_coins", tex_coins, "三枚铜钱", "Three Bronze Coins"),
    ("yarrow_stalks", tex_yarrow, "蓍草", "Yarrow Stalks"),
    ("turtle_shell", tex_turtle_shell, "龟甲", "Turtle Shell"),
    ("sundial", tex_sundial, "日晷", "Sundial"),
    ("ward_talisman", lambda: tex_talisman("ward"), "护身符", "Warding Talisman"),
    ("fortune_talisman", lambda: tex_talisman("fortune"), "转运符", "Fortune Talisman"),
    ("clarity_talisman", lambda: tex_talisman("clarity"), "明目符", "Clarity Talisman"),
    ("taisui_talisman", lambda: tex_talisman("taisui"), "太岁符", "Tai Sui Talisman"),
    ("wood_stone", lambda: tex_element_stone("wood"), "木灵石", "Wood Spirit Stone"),
    ("fire_stone", lambda: tex_element_stone("fire"), "火灵石", "Fire Spirit Stone"),
    ("earth_stone", lambda: tex_element_stone("earth"), "土灵石", "Earth Spirit Stone"),
    ("metal_stone", lambda: tex_element_stone("metal"), "金灵石", "Metal Spirit Stone"),
    ("water_stone", lambda: tex_element_stone("water"), "水灵石", "Water Spirit Stone"),
    ("yinyang_stone", tex_yinyang_stone, "阴阳石", "Yin-Yang Stone"),
    ("dragon_core_shard", lambda: tex_shard("dragon_core_shard"), "龙脉核心碎片", "Dragon Vein Shard"),
    ("flame_fragment", lambda: tex_shard("flame_fragment"), "烈焰碎片", "Flame Fragment"),
    ("soul_core", lambda: tex_shard("soul_core"), "灵魂核心", "Soul Core"),
    ("void_shard", lambda: tex_shard("void_shard"), "虚空碎片", "Void Shard"),
    ("chorus_core", lambda: tex_shard("chorus_core"), "紫颂核心", "Chorus Core"),
    ("dragon_breath_core", lambda: tex_shard("dragon_breath_core"), "龙气核心", "Dragon Breath Core"),
    ("fate_book", tex_fate_book, "命理书", "Book of Fate"),
    ("ancient_scroll", tex_scroll, "古卷·山河志", "Ancient Scroll"),
    ("fate_wisp_spawn_egg", lambda: tex_spawn_egg(rgb("6E5C9E"), rgb("E8E0FF"), rgb("C8BCF4")),
     "星辉符灵刷怪蛋", "Fate Wisp Spawn Egg"),
    ("jade_spirit_spawn_egg", lambda: tex_spawn_egg(rgb("5FA88C"), rgb("D8F4E4"), rgb("A8DCC4")),
     "青玉灵刷怪蛋", "Jade Spirit Spawn Egg"),
    ("stone_guardian_spawn_egg", lambda: tex_spawn_egg(rgb("7A7A82"), rgb("B8B8C0"), rgb("9E9EA8")),
     "卦石守卫刷怪蛋", "Stone Guardian Spawn Egg"),
    ("star_watcher_spawn_egg", lambda: tex_spawn_egg(rgb("5A4E9E"), rgb("CFC4FF"), rgb("8E7ED8")),
     "观星者刷怪蛋", "Star Watcher Spawn Egg"),
    ("star_fox_spawn_egg", lambda: tex_spawn_egg(rgb("7A62C8"), rgb("E8E0FF"), rgb("B0A0E8")),
     "星狐刷怪蛋", "Star Fox Spawn Egg"),
    ("star_slime_spawn_egg", lambda: tex_spawn_egg(rgb("5A5AA8"), rgb("C8C8F4"), rgb("9090D8")),
     "星屑史莱姆刷怪蛋", "Stardust Slime Spawn Egg"),
    ("jade_rabbit_spawn_egg", lambda: tex_spawn_egg(rgb("5FA88C"), rgb("E8FFF4"), rgb("A8DCC4")),
     "玉兔刷怪蛋", "Jade Rabbit Spawn Egg"),
    ("jade_goat_spawn_egg", lambda: tex_spawn_egg(rgb("4E9C80"), rgb("D8F4E8"), rgb("8ED0B4")),
     "玉羊刷怪蛋", "Jade Goat Spawn Egg"),
    ("vein_turtle_spawn_egg", lambda: tex_spawn_egg(rgb("3E8A6C"), rgb("C8F0DC"), rgb("78C0A0")),
     "地脉龟刷怪蛋", "Vein Turtle Spawn Egg"),
    ("ink_wolf_spawn_egg", lambda: tex_spawn_egg(rgb("3E3E4E"), rgb("A8A8C0"), rgb("6E6E88")),
     "爻狼刷怪蛋", "Ink Wolf Spawn Egg"),
    ("ink_bat_spawn_egg", lambda: tex_spawn_egg(rgb("4A4A5E"), rgb("B8B8D0"), rgb("7A7A94")),
     "爻蝠刷怪蛋", "Ink Bat Spawn Egg"),
    ("hex_phantom_spawn_egg", lambda: tex_spawn_egg(rgb("52709C"), rgb("C0D4F0"), rgb("88A8D0")),
     "卦影刷怪蛋", "Hex Phantom Spawn Egg"),
    # —— 第 11 轮：丹道 ——
    ("qi_pill", lambda: tex_pill(rgb("B85A4A")), "归元丹", "Origin Pill"),
    ("clarity_pill", lambda: tex_pill(rgb("8EC8D8")), "明目丹", "Clarity Pill"),
    ("calm_pill", lambda: tex_pill(rgb("7E8CB8")), "定神丹", "Calm Pill"),
    ("swift_pill", lambda: tex_pill(rgb("9AC86A")), "疾行丹", "Swift Pill"),
    ("ward_pill", lambda: tex_pill(rgb("D8804A")), "辟邪丹", "Warding Pill"),
    ("taisui_pill", lambda: tex_pill(rgb("D8B44A"), rgb("FFF0B0")), "太岁丹", "Tai Sui Pill"),
    # —— 第 11 轮：灵物 ——
    ("talisman_paper", tex_paper_sheet, "空白符纸", "Blank Talisman Paper"),
    ("star_fur", lambda: tex_fur(rgb("8E7ED8"), rgb("E0D8FF")), "星狐绒", "Star Fox Fur"),
    ("jade_wool", lambda: tex_fur(rgb("8ED0B4"), rgb("E8FFF4")), "玉兔绒", "Jade Rabbit Wool"),
    ("ink_membrane", tex_membrane, "爻蝠翼膜", "Hex Bat Membrane"),
    ("vein_scale", tex_scale, "地脉鳞", "Vein Scale"),
    ("vein_map", tex_vein_map, "寻脉图", "Vein Map"),
    ("bagua_mirror", tex_bagua_mirror, "八卦镜", "Bagua Mirror"),
    # —— 第 13 轮：界域作物 ——
    ("astral_seeds", lambda: tex_seeds(rgb("C8BCF4"), rgb("E8E0FF")),
     "星辉种子", "Astral Seeds"),
    ("astral_grain", lambda: tex_grain(rgb("B9A8F0"), rgb("E8E0FF")),
     "星辉穗实", "Astral Grain"),
    ("jade_seeds", lambda: tex_seeds(rgb("8ED0B4"), rgb("E8FFF4")),
     "玉粒种子", "Jade Seeds"),
    ("jade_grain", lambda: tex_grain(rgb("B0E0CE"), rgb("E8FFF4")),
     "玉粒", "Jade Grain"),
    ("yao_seeds", lambda: tex_seeds(rgb("8A8AA8"), rgb("E8E0FF")),
     "爻纹种子", "Hex Seeds"),
    ("ziwei_seeds", lambda: tex_seeds(rgb("C0A8F0"), rgb("F0E4FF")),
     "紫微穗种子", "Zi Wei Seeds"),
    ("ziwei_grain", lambda: tex_grain(rgb("C8A8F0"), rgb("F4E8C0")),
     "紫微穗", "Zi Wei Grain"),
    ("liuren_seeds", lambda: tex_seeds(rgb("8AD0E0"), rgb("E4FAFF")),
     "壬水稻种子", "Liu Ren Seeds"),
    ("liuren_grain", lambda: tex_grain(rgb("9AD8E4"), rgb("E4FAFF")),
     "壬水稻", "Liu Ren Grain"),
    ("qimen_seeds", lambda: tex_seeds(rgb("C89A60"), rgb("F8E0B0")),
     "八门豆种子", "Qi Men Seeds"),
    ("qimen_grain", lambda: tex_grain(rgb("D8A868"), rgb("F8DCA8")),
     "八门豆", "Qi Men Beans"),
    ("ziwei_star_spawn_egg", lambda: tex_spawn_egg(rgb("6B4E9C"), rgb("D8C8FF"), rgb("A888E0")),
     "紫微星官刷怪蛋", "Zi Wei Star Spawn Egg"),
    ("dou_beast_spawn_egg", lambda: tex_spawn_egg(rgb("4A5AA0"), rgb("C8D0FF"), rgb("7A8AD0")),
     "斗宿兽刷怪蛋", "Dou Beast Spawn Egg"),
    ("tianji_fox_spawn_egg", lambda: tex_spawn_egg(rgb("A06AA8"), rgb("F0D8F8"), rgb("C898D0")),
     "天机狐刷怪蛋", "Tian Ji Fox Spawn Egg"),
    ("ziwei_moth_spawn_egg", lambda: tex_spawn_egg(rgb("7A5AB0"), rgb("E8DCFF"), rgb("B8A0F0")),
     "紫垣蛾刷怪蛋", "Zi Wei Moth Spawn Egg"),
    ("ren_serpent_spawn_egg", lambda: tex_spawn_egg(rgb("3E7E8E"), rgb("C8F4FA"), rgb("7AC8D8")),
     "壬水蛟刷怪蛋", "Ren Serpent Spawn Egg"),
    ("gui_crane_spawn_egg", lambda: tex_spawn_egg(rgb("6AA8C0"), rgb("E4FAFF"), rgb("98D8F0")),
     "贵人鹤刷怪蛋", "Gui Ren Crane Spawn Egg"),
    ("she_hound_spawn_egg", lambda: tex_spawn_egg(rgb("A85A5A"), rgb("F4D0D0"), rgb("D08A8A")),
     "螣蛇犬刷怪蛋", "She Hound Spawn Egg"),
    ("xuan_turtle_spawn_egg", lambda: tex_spawn_egg(rgb("3A3A46"), rgb("A8A8C0"), rgb("6E6E88")),
     "玄武龟刷怪蛋", "Xuan Wu Turtle Spawn Egg"),
    ("men_guard_spawn_egg", lambda: tex_spawn_egg(rgb("9A6A38"), rgb("F4D8A8"), rgb("D8A868")),
     "门卫刷怪蛋", "Gate Guard Spawn Egg"),
    ("xing_fox_spawn_egg", lambda: tex_spawn_egg(rgb("B0A050"), rgb("F8F0C0"), rgb("D8C880")),
     "星魁狐刷怪蛋", "Xing Kui Fox Spawn Egg"),
    ("dun_rat_spawn_egg", lambda: tex_spawn_egg(rgb("A05A40"), rgb("F0C8B0"), rgb("C88868")),
     "遁甲鼠刷怪蛋", "Dun Jia Rat Spawn Egg"),
    ("jia_bird_spawn_egg", lambda: tex_spawn_egg(rgb("5A5A66"), rgb("D0D0E0"), rgb("8A8AA0")),
     "甲子鸟刷怪蛋", "Jia Zi Bird Spawn Egg"),
    ("yao_pod", lambda: tex_pod(rgb("5A5A6E"), rgb("C0392B")),
     "爻纹豆荚", "Hex Pod"),
    # ---- 三大术数（第 15 轮）----
    ("ziwei_chart", lambda: tex_oracle_disc("ziwei"), "紫微命盘", "Zi Wei Chart"),
    ("liuren_plate", lambda: tex_oracle_disc("liuren"), "六壬式盘", "Liu Ren Plate"),
    ("qimen_disc", lambda: tex_oracle_disc("qimen"), "奇门局盘", "Qi Men Disc"),
    ("ziwei_token", lambda: tex_oracle_charm("ziwei"), "紫微令", "Zi Wei Token"),
    ("tianfu_token", lambda: tex_oracle_charm("tianfu"), "天府令", "Tian Fu Token"),
    ("qisha_token", lambda: tex_oracle_charm("qisha"), "七杀令", "Qi Sha Token"),
    ("guiren_talisman", lambda: tex_oracle_charm("guiren"), "贵人符", "Noble Talisman"),
    ("qinglong_talisman", lambda: tex_oracle_charm("qinglong"), "青龙符", "Azure Dragon Talisman"),
    ("baihu_talisman", lambda: tex_oracle_charm("baihu"), "白虎符", "White Tiger Talisman"),
    ("shengmen_talisman", lambda: tex_oracle_charm("shengmen"), "生门符", "Gate of Life Talisman"),
    ("kaimen_talisman", lambda: tex_oracle_charm("kaimen"), "开门符", "Gate of Opening Talisman"),
    ("xiumen_talisman", lambda: tex_oracle_charm("xiumen"), "休门符", "Gate of Rest Talisman"),
    ("ziwei_star_dust", lambda: tex_dust_ore(rgb("8E7ED8"), rgb("E8E0FF"), rgb("4A3E78")),
     "紫微星尘", "Zi Wei Star Dust"),
    ("liuren_shell", lambda: tex_shard("liuren_shell"), "课甲", "Divination Shell"),
    ("qimen_shard", lambda: tex_shard("qimen_shard"), "遁甲残片", "Qi Men Shard"),
]

BLOCK_TEXTURES = [
    ("qi_observatory", tex_block_qi_observatory, "观气台", "Qi Observatory"),
    ("censer", tex_block_censer, "香炉", "Censer"),
    ("shi_gandang", tex_block_shi_gandang, "石敢当", "Shi Gandang"),
    ("gua_board", tex_block_gua_board, "卦盘", "Hexagram Board"),
    ("bagua_platform", tex_block_bagua_platform, "八卦台", "Bagua Platform"),
    ("taiji_diagram", tex_block_taiji, "太极图", "Taiji Diagram"),
    ("altar", tex_block_altar, "祭坛", "Altar"),
    ("ruin_core", tex_block_ruin_core, "遗迹核心", "Ruin Core"),
    ("yinyang_stele", tex_block_yinyang_stele, "阴阳界碑", "Yin-Yang Stele"),
    ("dragon_core_block", tex_block_dragon_core, "龙脉核心", "Dragon Vein Core"),
    ("wood_stone_block", lambda: tex_element_block("wood"), "木灵石块", "Wood Spirit Stone Block"),
    ("fire_stone_block", lambda: tex_element_block("fire"), "火灵石块", "Fire Spirit Stone Block"),
    ("earth_stone_block", lambda: tex_element_block("earth"), "土灵石块", "Earth Spirit Stone Block"),
    ("metal_stone_block", lambda: tex_element_block("metal"), "金灵石块", "Metal Spirit Stone Block"),
    ("water_stone_block", lambda: tex_element_block("water"), "水灵石块", "Water Spirit Stone Block"),
    ("astral_stone", lambda: tex_dim_stone(ASTRAL, ASTRAL_D, ASTRAL_L), "星辉石", "Astral Stone"),
    ("astral_bricks", lambda: tex_dim_bricks(ASTRAL, ASTRAL_D, ASTRAL_L), "星辉砖", "Astral Bricks"),
    ("astral_lamp", lambda: tex_dim_lamp(rgb("EFE8FF"), rgb("B9A8F0"), ASTRAL_D), "星辉灯", "Astral Lamp"),
    ("jade_stone", lambda: tex_dim_stone(JADE_2, JADE_2D, JADE_2L), "青玉石", "Jade Stone"),
    ("jade_bricks", lambda: tex_dim_bricks(JADE_2, JADE_2D, JADE_2L), "青玉砖", "Jade Bricks"),
    ("jade_lamp", lambda: tex_dim_lamp(rgb("E8FFF4"), rgb("8FE0C0"), JADE_2D), "青玉灯", "Jade Lamp"),
    ("yao_stone", lambda: tex_dim_stone(YAO, YAO_D, YAO_L, YAO_RED), "爻纹石", "Hexagram Stone"),
    ("yao_bricks", lambda: tex_dim_bricks(YAO, YAO_D, YAO_L), "爻纹砖", "Hexagram Bricks"),
    ("yao_lamp", lambda: tex_dim_lamp(rgb("FFE8D8"), rgb("E09070"), YAO_D), "爻纹灯", "Hexagram Lamp"),
    # ---- 三大术数（第 15 轮）----
    ("ziwei_terrace", lambda: tex_block_oracle_altar("ziwei"), "紫微星台", "Zi Wei Terrace"),
    ("liuren_altar", lambda: tex_block_oracle_altar("liuren"), "六壬式坛", "Liu Ren Altar"),
    ("qimen_array", lambda: tex_block_oracle_altar("qimen"), "奇门局阵", "Qi Men Array"),
    # ---- 第 16 轮：三术界建材 ----
    ("ziwei_stone", lambda: tex_dim_stone(ZW, ZW_D, ZW_L, GOLD), "紫微石", "Zi Wei Stone"),
    ("ziwei_bricks", lambda: tex_dim_bricks(ZW, ZW_D, ZW_L), "紫微砖", "Zi Wei Bricks"),
    ("ziwei_lamp", lambda: tex_dim_lamp(rgb("F4E8FF"), rgb("C8A8F0"), ZW_D), "紫微灯", "Zi Wei Lamp"),
    ("liuren_stone", lambda: tex_dim_stone(LR, LR_D, LR_L, rgb("E8F8FF")), "壬水石", "Liu Ren Stone"),
    ("liuren_bricks", lambda: tex_dim_bricks(LR, LR_D, LR_L), "壬水砖", "Liu Ren Bricks"),
    ("liuren_lamp", lambda: tex_dim_lamp(rgb("E8FBFF"), rgb("8AD8E8"), LR_D), "壬水灯", "Liu Ren Lamp"),
    ("qimen_stone", lambda: tex_dim_stone(QM, QM_D, QM_L, rgb("FFE0A0")), "遁甲石", "Qi Men Stone"),
    ("qimen_bricks", lambda: tex_dim_bricks(QM, QM_D, QM_L), "遁甲砖", "Qi Men Bricks"),
    ("qimen_lamp", lambda: tex_dim_lamp(rgb("FFF0C8"), rgb("F0B870"), QM_D), "遁甲灯", "Qi Men Lamp"),
    # ---- 第 16 轮：三术界第二工位 ----
    ("dou_altar", lambda: tex_block_oracle_altar("ziwei"), "斗枢台", "Dou Shu Altar"),
    ("guiren_altar", lambda: tex_block_oracle_altar("liuren"), "贵人台", "Gui Ren Altar"),
    ("bamen_altar", lambda: tex_block_oracle_altar("qimen"), "八门台", "Ba Men Altar"),
]


# ------------------------------------------------------------------ 方块状态

def blockstate_json(name):
    """为带属性的方块生成覆盖全部状态的 blockstates。"""
    model = {"model": f"{NS}:block/{name}"}

    if name in ("ziwei_terrace", "liuren_altar", "qimen_array",
                "dou_altar", "guiren_altar", "bamen_altar"):
        # 术坛有朝向 —— 术数讲方位，一块不辨方向的石头不叫坛
        yaws = {"north": 0, "east": 90, "south": 180, "west": 270}
        return {"variants": {
            f"facing={facing}": {"model": f"{NS}:block/{name}", "y": yaw}
            for facing, yaw in yaws.items()
        }}

    if name == "gua_board":
        # gua 0..63 × facing 4 —— 全部指向同一个模型（模型是重复的，这是有意的：
        # 卦盘的视觉变化由纹理承担，方块状态只需要「合法覆盖」即可）
        variants = {}
        for gua in range(64):
            for facing in ("north", "east", "south", "west"):
                variants[f"gua={gua},facing={facing}"] = model
        return {"variants": variants}

    if name == "censer":
        variants = {}
        for incense in range(8):
            for lit in ("false", "true"):
                variants[f"incense={incense},lit={lit}"] = model
        return {"variants": variants}

    if name == "ruin_core":
        return {"variants": {
            "lit=false": model,
            "lit=true": model,
        }}

    if name == "yinyang_stele":
        variants = {}
        for yin in ("false", "true"):
            for yang in ("false", "true"):
                for open_ in ("false", "true"):
                    variants[f"yin={yin},yang={yang},lit={open_}"] = model
        return {"variants": variants}

    # 其余方块没有属性，用空键覆盖全部状态
    return {"variants": {"": model}}



# ------------------------------------------------------------------ 第 13 轮：界域作物

# (茎, 叶, 穗/果, 高光)
CROP_COLORS = {
    "astral_wheat": (rgb("4E4266"), rgb("8E7ED8"), rgb("C8BCF4"), rgb("E8E0FF")),
    "jade_rice": (rgb("3E6E5C"), rgb("6FB89C"), rgb("B0E0CE"), rgb("E8FFF4")),
    "yao_bean": (rgb("2E2E3C"), rgb("5A5A6E"), rgb("8A8AA8"), rgb("C0392B")),
    # ---- 第 16 轮：三术界作物 ----
    "ziwei_wheat": (rgb("4A3A6E"), rgb("8A72C8"), rgb("C0A8F0"), rgb("F0E4B8")),
    "liuren_rice": (rgb("2E5A62"), rgb("5A9AA8"), rgb("9AD8E4"), rgb("E4FAFF")),
    "qimen_bean": (rgb("5A3A1E"), rgb("946A38"), rgb("C89A60"), rgb("F0C070")),
}


def tex_crop(kind, stage):
    """四阶段作物：破土 → 半高 → 抽穗 → 成熟。底端固定在 y=14。"""
    stem, leaf, fruit, hi = CROP_COLORS[kind]
    c = Canvas()
    top = (12, 9, 6, 3)[stage]

    # 主茎（带一点左侧阴影）
    for y in range(top, 15):
        c.put(8, y, stem)
        if y > top:
            c.put(7, y, shade(stem, 0.82))

    # 叶片：阶段越高，长的位置越高
    leaf_rows = [(12,), (11, 13), (11, 9, 13), (11, 9, 7, 13)][stage]
    for ly in leaf_rows:
        c.put(6, ly, leaf)
        c.put(7, ly, shade(leaf, 0.85))
        c.put(9, ly, leaf)
        c.put(10, ly, shade(leaf, 0.85))

    # 顶端分叉
    if stage >= 1:
        c.put(7, top, leaf)
        c.put(9, top, leaf)

    # 穗 / 荚
    if stage >= 2:
        for y in range(top, min(top + 3, 15)):
            c.put(8, y, fruit)
    if stage == 3:
        for y in range(top, min(top + 4, 15)):
            c.put(7, y, fruit)
            c.put(9, y, fruit)
        c.put(8, top, hi)
    return c


def tex_seeds(base, hi):
    """种子：三粒小籽。"""
    c = Canvas()
    for x, y in [(5, 6), (9, 5), (7, 11)]:
        c.put(x, y, base)
        c.put(x + 1, y, base)
        c.put(x, y + 1, shade(base, 0.8))
        c.put(x + 1, y + 1, shade(base, 0.85))
        c.put(x, y, base)
    c.put(5, 6, hi)
    c.put(9, 5, hi)
    c.put(7, 11, hi)
    return c


def tex_grain(base, hi):
    """穗实：一小捆穗子。"""
    c = Canvas()
    for x in (5, 8, 11):
        for y in range(4, 12):
            c.put(x, y, base if y % 2 == 0 else shade(base, 0.85))
        c.put(x, 4, hi)
        c.put(x, 6, hi)
    c.hline(4, 12, 12, shade(base, 0.7))
    return c


def tex_pod(base, accent):
    """豆荚：弯弯的一条荚，上有一道天生的断纹。"""
    c = Canvas()
    pts = [(4, 5), (5, 5), (6, 6), (7, 7), (8, 8), (9, 9), (10, 10), (11, 10), (12, 10)]
    for x, y in pts:
        c.put(x, y, base)
        c.put(x, y + 1, shade(base, 0.8))
        c.put(x, y - 1, shade(base, 1.15))
    # 爻纹：一道断开的横线
    for x, y in [(6, 6), (7, 7), (10, 10), (11, 10)]:
        c.put(x, y, accent)
    c.put(5, 6, accent)
    return c


def gen_crop_assets():
    """界域作物的纹理 / 模型 / 方块状态（四阶段，参照原版甜菜）。"""
    print("== 界域作物资源 ==")
    count = 0
    for kind in CROP_COLORS:
        for stage in range(4):
            canvas = tex_crop(kind, stage)
            write_png(os.path.join(ASSETS, "textures", "block", f"{kind}_stage{stage}.png"),
                      canvas.to_pixels(), canvas.size, canvas.size)
            count += 1
            mpath = os.path.join(ASSETS, "models", "block", f"{kind}_stage{stage}.json")
            os.makedirs(os.path.dirname(mpath), exist_ok=True)
            with open(mpath, "w", encoding="utf-8") as handle:
                json.dump({"parent": "minecraft:block/crop",
                           "textures": {"crop": f"{NS}:block/{kind}_stage{stage}"}},
                          handle, indent=1)
            count += 1
        bpath = os.path.join(ASSETS, "blockstates", f"{kind}.json")
        os.makedirs(os.path.dirname(bpath), exist_ok=True)
        with open(bpath, "w", encoding="utf-8") as handle:
            json.dump({"variants": {f"age={s}": {"model": f"{NS}:block/{kind}_stage{s}"}
                                    for s in range(4)}}, handle, indent=1)
        count += 1
    print(f"  作物 {len(CROP_COLORS)} 种 × 4 阶段 = {count} 个文件")
    return count


# ------------------------------------------------------------------ 生成主流程

def main():
    written = 0

    # ---------- 纹理 ----------
    for name, factory, _zh, _en in ITEM_TEXTURES:
        write_texture(os.path.join(ASSETS, "textures", "item", f"{name}.png"),
                      factory())
        written += 1

    for name, factory, _zh, _en in BLOCK_TEXTURES:
        write_texture(os.path.join(ASSETS, "textures", "block", f"{name}.png"),
                      factory())
        written += 1

    # ---------- 界域作物（纹理 / 模型 / 方块状态）----------
    written += gen_crop_assets()

    # ---------- 图标 ----------
    icon = tex_icon()
    write_png(os.path.join(ASSETS, "icon.png"), icon.to_pixels(), icon.size, icon.size)
    written += 1

    # ---------- 物品模型（1.21.4+ 的 items/*.json 定义层）----------
    for name, _factory, _zh, _en in ITEM_TEXTURES:
        path = os.path.join(ASSETS, "items", f"{name}.json")
        os.makedirs(os.path.dirname(path), exist_ok=True)
        with open(path, "w", encoding="utf-8") as handle:
            json.dump({"model": {"type": "minecraft:model",
                                 "model": f"{NS}:item/{name}"}}, handle, indent=1)
        written += 1

    # ---------- 物品模型（models/item）----------
    for name, _factory, _zh, _en in ITEM_TEXTURES:
        path = os.path.join(ASSETS, "models", "item", f"{name}.json")
        os.makedirs(os.path.dirname(path), exist_ok=True)
        with open(path, "w", encoding="utf-8") as handle:
            json.dump({"parent": "minecraft:item/generated",
                       "textures": {"layer0": f"{NS}:item/{name}"}}, handle, indent=1)
        written += 1

    for name, _factory, _zh, _en in BLOCK_TEXTURES:
        path = os.path.join(ASSETS, "models", "item", f"{name}.json")
        os.makedirs(os.path.dirname(path), exist_ok=True)
        with open(path, "w", encoding="utf-8") as handle:
            json.dump({"parent": f"{NS}:block/{name}"}, handle, indent=1)
        written += 1

    # ---------- 方块物品的 items/*.json 定义层 ----------
    # 1.21.4+ 里 BlockItem 也需要 items/<name>.json，缺了物品栏里就是错误贴图
    for name, _factory, _zh, _en in BLOCK_TEXTURES:
        path = os.path.join(ASSETS, "items", f"{name}.json")
        os.makedirs(os.path.dirname(path), exist_ok=True)
        with open(path, "w", encoding="utf-8") as handle:
            json.dump({"model": {"type": "minecraft:model",
                                 "model": f"{NS}:block/{name}"}}, handle, indent=1)
        written += 1

    # ---------- 方块模型 ----------
    for name, _factory, _zh, _en in BLOCK_TEXTURES:
        path = os.path.join(ASSETS, "models", "block", f"{name}.json")
        os.makedirs(os.path.dirname(path), exist_ok=True)
        with open(path, "w", encoding="utf-8") as handle:
            json.dump({"parent": "minecraft:block/cube_all",
                       "textures": {"all": f"{NS}:block/{name}"}}, handle, indent=1)
        written += 1

    # ---------- 方块状态 ----------
    for name, _factory, _zh, _en in BLOCK_TEXTURES:
        path = os.path.join(ASSETS, "blockstates", f"{name}.json")
        os.makedirs(os.path.dirname(path), exist_ok=True)
        with open(path, "w", encoding="utf-8") as handle:
            json.dump(blockstate_json(name), handle, indent=1)
        written += 1

    # ---------- 第 9 轮：维度门户与灵植 ----------
    written += gen_realm_assets()

    # ---------- 语言文件 ----------
    zh = {"itemGroup.yishu.main": "易枢",
          "key.yishu.fate": "打开命盘",
          "key.categories.yishu.main": "易枢"}
    en = {"itemGroup.yishu.main": "Yishu",
          "key.yishu.fate": "Open Fate Chart",
          "key.categories.yishu.main": "Yishu"}

    for name, _factory, zh_name, en_name in ITEM_TEXTURES:
        zh[f"item.{NS}.{name}"] = zh_name
        en[f"item.{NS}.{name}"] = en_name

    for name, _factory, zh_name, en_name in BLOCK_TEXTURES:
        zh[f"block.{NS}.{name}"] = zh_name
        en[f"block.{NS}.{name}"] = en_name

    # 材料的说明文字
    notes_zh = {
        "dragon_core_shard": "龙脉凝结之物，可用于立契占领龙脉。",
        "flame_fragment": "下界火煞的凝晶，压制之后才肯现身。",
        "soul_core": "灵魂沙中沉眠的核心，阳气可令其显形。",
        "void_shard": "末地虚空的碎片，唯有实体之物才能触及。",
        "chorus_core": "紫颂的种子，虚空之力方能唤其生长。",
        "dragon_breath_core": "末影龙陨落时留下的气息。",
        "fate_book": "记录着一张命盘的竹纸书，可放在讲台上供人阅读。",
        "ancient_scroll": "遗迹里开出的残卷，展开可读《山河志》的下一卷。",
        "qi_pill": "以蓍草引星辉尘炼成，服下伤口自收。",
        "clarity_pill": "薄如蝉翼的一片，夜路也看得分明。",
        "calm_pill": "心一定，外邪就难近身。",
        "swift_pill": "脚下生风，跑起来像被谁推着走。",
        "ward_pill": "辟火气的丹药，岩浆边上也敢站一会儿。",
        "taisui_pill": "太岁符入炉，一年只该吃一颗。",
        "talisman_paper": "裁好的黄纸，能承接符力。",
        "star_fur": "星狐身上落下的一层柔光，捏起来微微烫手。",
        "jade_wool": "青玉界最细的一层绒毛，据说能安神。",
        "ink_membrane": "半透明的墨色薄膜，对着光能看见爻纹。",
        "vein_scale": "地脉龟的甲片，一圈一圈都是年轮。",
        "vein_map": "摊开来，墨迹会自己挪向最近的遗迹。",
        "bagua_mirror": "举镜一照，方圆十六格内的活物都会显形。",
        "astral_seeds": "种在耕地上，会长出一株吸星光的小麦。",
        "astral_grain": "星辉穗打下来的谷粒，嚼着有股甜味。",
        "jade_seeds": "种在耕地上，稻穗会分出玉色的小粒。",
        "jade_grain": "一粒一粒像小玉珠，管饱。",
        "yao_seeds": "种在耕地上，豆荚上有一道天生的断纹。",
        "yao_pod": "掰开豆荚，断纹在光下会动。",
        "ziwei_token": "紫微帝星之令。撕开的一瞬，身上像多了一层看不见的甲。",
        "tianfu_token": "天府库星之令。不求暴富，只求仓里永远有余。",
        "qisha_token": "七杀将星之令。手会自己握紧，脚下也不再犹豫。",
        "guiren_talisman": "贵人之符。走投无路时，总有人恰好路过。",
        "qinglong_talisman": "青龙之符。风从背后起，一步当两步。",
        "baihu_talisman": "白虎之符。杀气上了手，做什么都快而狠。",
        "shengmen_talisman": "生门之符。伤口自己在合，饿意也退了。",
        "kaimen_talisman": "开门之符。眼前忽然亮了，连运气都松了松。",
        "xiumen_talisman": "休门之符。什么都不做，就是最好的做。",
        "ziwei_star_dust": "星辉矿石筛出的细粉，紫微盘就以它为骨。",
        "liuren_shell": "起课用的甲片，一片只够起一课。",
        "qimen_shard": "九宫格上崩下来的一角，纹路还在自己走。",
    }
    notes_en = {
        "dragon_core_shard": "Condensed dragon vein essence; used to claim a vein.",
        "flame_fragment": "Crystallised nether fire-sha; only yields once suppressed.",
        "soul_core": "A core sleeping in soul sand; yang energy reveals it.",
        "void_shard": "A shard of End void, touchable only by solid matter.",
        "chorus_core": "Seed of the chorus; only void force can wake it.",
        "dragon_breath_core": "What remains when the Ender Dragon falls.",
        "fate_book": "Bamboo paper recording a fate chart; readable on a lectern.",
        "ancient_scroll": "A ruined scroll from the ruins; read the next volume of the Chronicle.",
        "qi_pill": "Brewed from yarrow and astral dust; wounds close on their own.",
        "clarity_pill": "Thin as a cicada wing; night roads read clearly.",
        "calm_pill": "A settled mind keeps ill winds at bay.",
        "swift_pill": "Wind underfoot; you run as if pushed along.",
        "ward_pill": "Wards off fire-sha; you can stand near lava for a while.",
        "taisui_pill": "Tai Sui talisman in the furnace; one pill a year is enough.",
        "talisman_paper": "Cut yellow paper, ready to carry a charm.",
        "star_fur": "A layer of soft light shed by a star fox; faintly warm.",
        "jade_wool": "The finest wool of the Jade Realm; said to calm the heart.",
        "ink_membrane": "A translucent ink-dark film; hexagram lines show against light.",
        "vein_scale": "A plate from a vein turtle; every ring is a year.",
        "vein_map": "Unfolded, the ink crawls toward the nearest ruin.",
        "bagua_mirror": "Raise it and every living thing within sixteen blocks shows itself.",
        "astral_seeds": "Plant on farmland; grows a wheat that drinks starlight.",
        "astral_grain": "Grain from astral wheat; faintly sweet.",
        "jade_seeds": "Plant on farmland; the ears split into jade beads.",
        "jade_grain": "Beads like little jade; filling.",
        "yao_seeds": "Plant on farmland; the pod bears a natural broken line.",
        "yao_pod": "Split it open — the broken line moves in the light.",
        "ziwei_token": "Token of the Emperor Star. Tear it and an unseen armour settles on you.",
        "tianfu_token": "Token of the Treasury Star. Not sudden wealth — just a granary that never empties.",
        "qisha_token": "Token of the General Star. Your hand closes by itself; your feet stop hesitating.",
        "guiren_talisman": "Charm of the Noble One. When the road ends, someone happens to walk by.",
        "qinglong_talisman": "Charm of the Azure Dragon. Wind at your back, one step for two.",
        "baihu_talisman": "Charm of the White Tiger. Killing intent in hand — quick and merciless.",
        "shengmen_talisman": "Charm of the Gate of Life. Wounds close; hunger fades.",
        "kaimen_talisman": "Charm of the Gate of Opening. The world brightens and luck loosens.",
        "xiumen_talisman": "Charm of the Gate of Rest. Doing nothing is the best doing.",
        "ziwei_star_dust": "Fine powder sieved from astral ore; the bones of a Zi Wei chart.",
        "liuren_shell": "A divination plate; one piece serves one lesson.",
        "qimen_shard": "A corner broken off the nine palaces; the lines still move.",
    }
    for key, text in notes_zh.items():
        zh[f"item.{NS}.{key}.note"] = text
    for key, text in notes_en.items():
        en[f"item.{NS}.{key}.note"] = text

    # 第 9 轮：维度门户与灵植
    for key, text in REALM_LANG_ZH.items():
        zh[key] = text
    for key, text in REALM_LANG_EN.items():
        en[key] = text

    # 界面用不到本地化键，但保留一份「术数名词表」方便玩家查
    for key, text in {
        "yishu.term.wuxing": "五行：木火土金水，相生相克，是整套系统的共同语言",
        "yishu.term.bazi": "八字：年月日时四柱，共八个字，日干为「我」",
        "yishu.term.liuyao": "六爻：以六次掷钱成卦，配六亲六神断事",
        "yishu.term.fengshui": "风水：以气煞阴阳与五行偏向评价一块地",
        "yishu.term.longmai": "龙脉：每年游走的带状地气，可占领",
        "yishu.term.taisui": "太岁：每年坐镇一方，动土招煞，石敢当可化",
    }.items():
        zh[key] = text
        en[key] = text

    lang_dir = os.path.join(ASSETS, "lang")
    os.makedirs(lang_dir, exist_ok=True)
    with open(os.path.join(lang_dir, "zh_cn.json"), "w", encoding="utf-8") as handle:
        json.dump(zh, handle, ensure_ascii=False, indent=1)
    with open(os.path.join(lang_dir, "en_us.json"), "w", encoding="utf-8") as handle:
        json.dump(en, handle, ensure_ascii=False, indent=1)
    written += 2

    print(f"已生成 {written} 个资源文件")
    print(f"  纹理 {len(ITEM_TEXTURES) + len(BLOCK_TEXTURES) + 1} 个")
    print(f"  物品 {len(ITEM_TEXTURES)} 个，方块 {len(BLOCK_TEXTURES)} 个")
    print(f"  语言键 zh={len(zh)} en={len(en)}（键集合一致：{set(zh) == set(en)}）")


if __name__ == "__main__":
    main()
