#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""灵兽贴图：读取原版 vex / allay / iron_golem 贴图，做色相转换后输出。

产物：src/main/resources/assets/yishu/textures/entity/*.png
（渲染器里 getTextureLocation 指向这三个文件）

原版贴图是 8-bit 调色板 PNG（colortype 3），这里自带一个最小 PNG 解码器
（支持 0/1/2/3/4/5 滤波器与调色板 + tRNS），不依赖任何第三方库。
"""

import os
import struct
import sys
import zlib

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
OUT = os.path.join(ROOT, "src", "main", "resources", "assets", "yishu", "textures", "entity")

# 26.2 游戏 jar：优先读环境变量 YISHU_MC_JAR，
# 否则在常见位置找一个（本机 Minecraft 目录 / Loom 缓存）。
# 开源版不写死任何人的绝对路径。
def _find_jar():
    env = os.environ.get("YISHU_MC_JAR")
    if env and os.path.exists(env):
        return env
    home = os.path.expanduser("~")
    candidates = [
        os.path.join(home, "AppData/Roaming/.minecraft/versions/26.2/26.2.jar"),
        os.path.join(home, "Library/Application Support/minecraft/versions/26.2/26.2.jar"),
        os.path.join(home, ".minecraft/versions/26.2/26.2.jar"),
    ]
    for path in candidates:
        if os.path.exists(path):
            return path
    return candidates[0]


DEFAULT_JAR = _find_jar()

# (原版路径, 输出名, 色相旋转度, 饱和度倍率, 明度增量[, 强制色相, 最低饱和])
# 后两个参数用于「近灰色的贴图」：先给灰调出可用色相，再整体旋转。
JOBS = [
    ("assets/minecraft/textures/entity/illager/vex.png", "fate_wisp.png", 40.0, 1.25, 0.10),
    ("assets/minecraft/textures/entity/allay/allay.png", "jade_spirit.png", -60.0, 1.15, 0.06),
    ("assets/minecraft/textures/entity/iron_golem/iron_golem.png", "stone_guardian.png", 12.0, 0.55, -0.04),
    # —— 第 10 轮：九灵兽 ——
    ("assets/minecraft/textures/entity/enderman/enderman.png", "star_watcher.png", 0.0, 1.1, 0.05, 275.0, 0.45),
    ("assets/minecraft/textures/entity/fox/fox.png", "star_fox.png", 225.0, 1.15, 0.06),
    ("assets/minecraft/textures/entity/slime/slime.png", "star_slime.png", 130.0, 1.25, -0.02),
    ("assets/minecraft/textures/entity/rabbit/rabbit_white.png", "jade_rabbit.png", 0.0, 1.0, 0.0, 145.0, 0.40),
    ("assets/minecraft/textures/entity/goat/goat.png", "jade_goat.png", 90.0, 0.95, 0.02),
    ("assets/minecraft/textures/entity/turtle/turtle.png", "vein_turtle.png", -12.0, 1.1, 0.0),
    ("assets/minecraft/textures/entity/wolf/wolf.png", "ink_wolf.png", 0.0, 1.0, -0.04, 225.0, 0.32),
    ("assets/minecraft/textures/entity/bat/bat.png", "ink_bat.png", 40.0, 0.6, -0.05),
    ("assets/minecraft/textures/entity/phantom/phantom.png", "hex_phantom.png", 25.0, 0.9, -0.02),
    # —— 第 13 轮：界域职业（村民外观按职业 id 找 profession/<id>.png）——
    ("assets/minecraft/textures/entity/villager/profession/cleric.png",
     "villager/profession/astrologer.png", 18.0, 1.20, 0.04),
    ("assets/minecraft/textures/entity/villager/profession/cartographer.png",
     "villager/profession/geomancer.png", -55.0, 1.15, 0.02),
    ("assets/minecraft/textures/entity/villager/profession/cleric.png",
     "villager/profession/diviner.png", 0.0, 0.35, -0.05),
    # —— 第 14 轮：三职业扩充（司命 / 堪舆师 / 蓍师）——
    ("assets/minecraft/textures/entity/villager/profession/mason.png",
     "villager/profession/fate_master.png", 240.0, 1.10, 0.02),
    ("assets/minecraft/textures/entity/villager/profession/fletcher.png",
     "villager/profession/surveyor.png", 115.0, 1.10, 0.01),
    ("assets/minecraft/textures/entity/villager/profession/butcher.png",
     "villager/profession/yarrow_master.png", 55.0, 1.05, 0.03),
    # —— 第 15 轮：三大术数三职业（星命师 / 课师 / 局师）——
    ("assets/minecraft/textures/entity/villager/profession/librarian.png",
     "villager/profession/star_master.png", 270.0, 1.18, 0.03, 285.0, 0.45),
    ("assets/minecraft/textures/entity/villager/profession/leatherworker.png",
     "villager/profession/lesson_master.png", 195.0, 1.15, 0.02, 205.0, 0.40),
    ("assets/minecraft/textures/entity/villager/profession/armorer.png",
     "villager/profession/array_master.png", 320.0, 1.12, 0.01, 330.0, 0.42),

    # —— 第 16 轮：三术界十二灵兽 ——
    ("assets/minecraft/textures/entity/enderman/enderman.png",
     "ziwei_star.png", 270.0, 1.20, 0.05, 275.0, 0.50),
    ("assets/minecraft/textures/entity/wolf/wolf.png",
     "dou_beast.png", 250.0, 1.15, 0.02, 260.0, 0.45),
    ("assets/minecraft/textures/entity/fox/fox.png",
     "tianji_fox.png", 285.0, 1.20, 0.03, 290.0, 0.50),
    ("assets/minecraft/textures/entity/illager/vex.png",
     "ziwei_moth.png", 265.0, 1.25, 0.04, 270.0, 0.55),
    ("assets/minecraft/textures/entity/turtle/turtle.png",
     "ren_serpent.png", 170.0, 1.15, 0.02, 180.0, 0.45),
    ("assets/minecraft/textures/entity/allay/allay.png",
     "gui_crane.png", 185.0, 1.15, 0.05, 190.0, 0.40),
    ("assets/minecraft/textures/entity/wolf/wolf.png",
     "she_hound.png", 155.0, 1.10, 0.00, 160.0, 0.45),
    ("assets/minecraft/textures/entity/turtle/turtle.png",
     "xuan_turtle.png", 200.0, 1.20, -0.05, 210.0, 0.50),
    ("assets/minecraft/textures/entity/iron_golem/iron_golem.png",
     "men_guard.png", 30.0, 1.20, 0.00, 35.0, 0.45),
    ("assets/minecraft/textures/entity/fox/fox.png",
     "xing_fox.png", 45.0, 1.20, 0.03, 50.0, 0.50),
    ("assets/minecraft/textures/entity/rabbit/rabbit_brown.png",
     "dun_rat.png", 20.0, 1.15, -0.02, 25.0, 0.45),
    ("assets/minecraft/textures/entity/bat/bat.png",
     "jia_bird.png", 340.0, 1.10, 0.00, 345.0, 0.40),
    # —— 第 16 轮：三术界三职业 ——
    ("assets/minecraft/textures/entity/villager/profession/librarian.png",
     "villager/profession/dou_master.png", 250.0, 1.16, 0.02, 258.0, 0.45),
    ("assets/minecraft/textures/entity/villager/profession/leatherworker.png",
     "villager/profession/guiren_master.png", 165.0, 1.16, 0.02, 172.0, 0.42),
    ("assets/minecraft/textures/entity/villager/profession/armorer.png",
     "villager/profession/bamen_master.png", 30.0, 1.14, 0.01, 36.0, 0.44),
]


# ------------------------------------------------------------------ PNG 解码

def png_decode(data):
    """返回 (w, h, [ (r,g,b,a), ... ])"""
    assert data[:8] == b"\x89PNG\r\n\x1a\n", "不是 PNG"
    i = 8
    w = h = depth = ctype = None
    plte = []
    trns = None
    idat = b""
    while i < len(data):
        ln = struct.unpack(">I", data[i:i + 4])[0]
        tag = data[i + 4:i + 8]
        chunk = data[i + 8:i + 8 + ln]
        i += 12 + ln
        if tag == b"IHDR":
            w, h, depth, ctype = struct.unpack(">IIBB", chunk[:10])
        elif tag == b"PLTE":
            plte = [(chunk[k], chunk[k + 1], chunk[k + 2]) for k in range(0, len(chunk), 3)]
        elif tag == b"tRNS":
            trns = list(chunk)
        elif tag == b"IDAT":
            idat += chunk
        elif tag == b"IEND":
            break
    assert (ctype == 3 and depth in (1, 2, 4, 8)) or depth == 8, \
        "只支持 8 位深（调色板允许 1/2/4 位）"
    raw = zlib.decompress(idat)
    channels = {0: 1, 2: 3, 3: 1, 4: 2, 6: 4}[ctype]
    if ctype == 3:
        # 调色板：每行按位打包（1/2/4 位是压缩行），滤波仍按字节
        row_bytes = (w * depth + 7) // 8
        bpp = 1
    else:
        row_bytes = w * channels
        bpp = channels
    out = bytearray()
    prev = bytearray(row_bytes)
    pos = 0
    for _y in range(h):
        f = raw[pos]; pos += 1
        line = bytearray(raw[pos:pos + row_bytes]); pos += row_bytes
        if f == 1:
            for k in range(bpp, row_bytes):
                line[k] = (line[k] + line[k - bpp]) & 0xFF
        elif f == 2:
            for k in range(row_bytes):
                line[k] = (line[k] + prev[k]) & 0xFF
        elif f == 3:
            for k in range(row_bytes):
                a = line[k - bpp] if k >= bpp else 0
                line[k] = (line[k] + ((a + prev[k]) >> 1)) & 0xFF
        elif f == 4:
            for k in range(row_bytes):
                a = line[k - bpp] if k >= bpp else 0
                b = prev[k]
                c = prev[k - bpp] if k >= bpp else 0
                p = a + b - c
                pa, pb, pc = abs(p - a), abs(p - b), abs(p - c)
                pr = a if (pa <= pb and pa <= pc) else (b if pb <= pc else c)
                line[k] = (line[k] + pr) & 0xFF
        out += line
        prev = line

    # 调色板图：把位流展开成索引流（对 8 位深同样成立）
    if ctype == 3:
        mask = (1 << depth) - 1
        index_stream = []
        for y in range(h):
            line = out[y * row_bytes:(y + 1) * row_bytes]
            for x in range(w):
                bit = x * depth
                byte = line[bit >> 3]
                shift = 8 - depth - (bit & 7)
                index_stream.append((byte >> shift) & mask)
    else:
        index_stream = out

    px = []
    for k in range(w * h):
        if ctype == 3:
            idx = index_stream[k]
            r, g, b = plte[idx]
            a = trns[idx] if trns and idx < len(trns) else 255
        elif ctype == 2:
            r, g, b = index_stream[k * 3:k * 3 + 3]
            a = 255
        elif ctype == 6:
            r, g, b, a = out[k * 4:k * 4 + 4]
        elif ctype == 0:
            r = g = b = out[k]; a = 255
        else:  # 4 = 灰度+alpha
            g = out[k * 2]; r = b = g; a = out[k * 2 + 1]
        px.append((r, g, b, a))
    return w, h, px


# ------------------------------------------------------------------ PNG 编码

def png_encode(path, w, h, px):
    raw = bytearray()
    for y in range(h):
        raw.append(0)
        for x in range(w):
            r, g, b, a = px[y * w + x]
            raw += bytes((r, g, b, a))

    def chunk(tag, data):
        return (struct.pack(">I", len(data)) + tag + data
                + struct.pack(">I", zlib.crc32(tag + data) & 0xFFFFFFFF))

    blob = b"\x89PNG\r\n\x1a\n"
    blob += chunk(b"IHDR", struct.pack(">IIBBBBB", w, h, 8, 6, 0, 0, 0))
    blob += chunk(b"IDAT", zlib.compress(bytes(raw), 9))
    blob += chunk(b"IEND", b"")
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "wb") as f:
        f.write(blob)


# ------------------------------------------------------------------ 色相转换

def rgb_to_hsl(r, g, b):
    r, g, b = r / 255.0, g / 255.0, b / 255.0
    mx, mn = max(r, g, b), min(r, g, b)
    l = (mx + mn) / 2
    if mx == mn:
        return 0.0, 0.0, l
    d = mx - mn
    sm = d / (2 - mx - mn) if l > 0.5 else d / (mx + mn)
    if mx == r:
        h = (g - b) / d + (6 if g < b else 0)
    elif mx == g:
        h = (b - r) / d + 2
    else:
        h = (r - g) / d + 4
    return h * 60.0, sm, l


def hsl_to_rgb(h, s, l):
    h = h % 360 / 360.0
    if s == 0:
        v = int(round(l * 255))
        return v, v, v
    q = l * (1 + s) if l < 0.5 else l + s - l * s
    p = 2 * l - q

    def hue(t):
        t = t % 1.0
        if t < 1 / 6: return p + (q - p) * 6 * t
        if t < 1 / 2: return q
        if t < 2 / 3: return p + (q - p) * (2 / 3 - t) * 6
        return p

    return (int(round(hue(h + 1 / 3) * 255)),
            int(round(hue(h) * 255)),
            int(round(hue(h - 1 / 3) * 255)))


def shift(px, ddeg, smul, ladd, force_hue=None, smin=0.0):
    out = []
    for (r, g, b, a) in px:
        if a == 0:
            out.append((r, g, b, a))
            continue
        h, s, l = rgb_to_hsl(r, g, b)
        if force_hue is not None and s < 0.15:
            # 近灰色的像素：先给它一个色相与最低饱和度，再整体旋转
            h = force_hue
            s = max(s, smin)
        h = h + ddeg
        s = max(0.0, min(1.0, s * smul))
        l = max(0.0, min(1.0, l + ladd))
        nr, ng, nb = hsl_to_rgb(h, s, l)
        out.append((nr, ng, nb, a))
    return out


# ------------------------------------------------------------------ 主流程

def read_from_jar(jar, inner):
    import zipfile
    with zipfile.ZipFile(jar) as z:
        return z.read(inner)


def main():
    jar = sys.argv[1] if len(sys.argv) > 1 else DEFAULT_JAR
    if not os.path.exists(jar):
        print("找不到游戏 jar：", jar)
        print("用法：python tools/generate_entity_textures.py [26.2.jar 路径]")
        sys.exit(1)
    for job in JOBS:
        inner, out_name, ddeg, smul, ladd = job[:5]
        force_hue = job[5] if len(job) > 5 else None
        smin = job[6] if len(job) > 6 else 0.0
        data = read_from_jar(jar, inner)
        w, h, px = png_decode(data)
        shifted = shift(px, ddeg, smul, ladd, force_hue, smin)
        path = os.path.join(OUT, out_name)
        os.makedirs(os.path.dirname(path), exist_ok=True)
        png_encode(path, w, h, shifted)
        print("  + %s（%dx%d，色相 %+0.0f°，饱和 x%.2f，明度 %+0.2f%s）"
              % (out_name, w, h, ddeg, smul, ladd,
                 "，强制色相 %.0f°" % force_hue if force_hue is not None else ""))

    print("灵兽贴图完成。")


if __name__ == "__main__":
    main()
