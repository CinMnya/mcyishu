#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""易枢 · 数据生成器（第 14 轮重建版）

⚠️ 重建说明（2026-10-16）：
  原约 3000 行的完整生成器被一次补丁脚本的写盘事故截断清空。
  本文件是重建后的「可运行子集」，负责仍在演进的增量资产，并且
  所有写盘改为「临时文件 + 改名」的原子写，永不再出现半截文件。

本脚本负责（幂等，可反复运行）：
  1. 界域村庄模板 NBT（3 村 × 3 变种）—— 第 14 轮重做：
     修好「室内空气填充吃掉地板」的悬空 bug、三种屋型按变种错位、
     围墙门楼、床 / 社坛 / 双工位工坊、村民预置职业。
  2. 维度裂隙模板 NBT（3 裂隙 × 3 变种）—— 回收自第 13 轮补丁。
  3. 流浪商人交易（gen_trades）+ 原版村民主题交易（gen_villager_trades）
     —— 回收自第 12 轮补丁。
  4. 六职业交易链（gen_realm_jobs，第 14 轮扩到 6 职业 × 3 级）。
  5. 六座工位配方（gen_workstation_recipes）。
  6. 林木标签：把界域树叶 / 原木并入原版 leaves / logs（gen_wood_tags）。

仍可从 tools/archive/ 回收的分支代码（需要重建其它内容时再取材）：
  · patch10_data.py     → realm_dimension_type / gen_realm_noise / gen_realms /
                          zone_params / gen_biomes / gen_trees / gen_ore_drops_and_recipes
  · patch10_data2.py    → gen_loot
  · patch11_data.py     → gen_pill_recipes、战利品增量
  · patch13_crops_loot.py → 作物入箱
  · patch13_villages.py → build_village 旧版（第 13 轮）与结构接线
  · patch13_jobs_data.py → gen_realm_jobs 旧版（3 职业）
  · patch13_rifts.py    → build_rift（本文件已并入）

已生成的世界数据（结构 NBT、群系、维度、战利品、进度等）直接保留在
src/main/resources 中，是运行时的唯一事实来源；本脚本不会删除任何既有文件。
"""

import gzip
import json
import os
import struct

RES = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources")
DATA = os.path.join(RES, "data")
DATA_VERSION = 4903  # 26.2

# ==================================================================== NBT 写入器


def write_string(s):
    b = s.encode("utf-8")
    return struct.pack(">H", len(b)) + b


def nbt_int(v):
    return struct.pack(">i", v)


def write_named(root_name, compound):
    return struct.pack(">b", 10) + write_string(root_name) + compound


def compound_of(entries):
    out = bytearray()
    for name, tag_id, payload in entries:
        out += struct.pack(">b", tag_id)
        out += write_string(name)
        out += payload
    out += b"\x00"      # TAG_End
    return bytes(out)


def list_of(tag_id, payloads):
    """TAG_List：元素类型 + 数量 + 依次写 payload。
    payload 可以带 (tag_id, payload) 元组壳（元组壳只取第二项，两种写法输出一致）。"""
    out = bytearray(struct.pack(">b", tag_id))
    out += struct.pack(">i", len(payloads))
    for p in payloads:
        out += p[1] if isinstance(p, tuple) else p
    return bytes(out)


def int_list(values):
    return list_of(3, [nbt_int(v) for v in values])


# ---- 通用 NBT 值编码（给实体 NBT 用）--------------------------------


class Byte(int):
    """TAG_Byte 包装（布尔/计数用）。"""


class Flt(float):
    """TAG_Float 包装。"""


def nbt_value(v):
    """Python 值 -> (tag_id, payload)。规则：
    Byte -> 1, bool -> 1, int -> 3, Flt -> 5, float -> 6, str -> 8,
    dict -> 10（复合）, list -> 9（按首元素推断元素类型，空列表按复合处理）
    """
    if isinstance(v, Byte):
        return 1, struct.pack(">b", int(v))
    if isinstance(v, bool):
        return 1, b"\x01" if v else b"\x00"
    if isinstance(v, int):
        if -2147483648 <= v <= 2147483647:
            return 3, struct.pack(">i", v)
        return 4, struct.pack(">q", v)
    if isinstance(v, Flt):
        return 5, struct.pack(">f", float(v))
    if isinstance(v, float):
        return 6, struct.pack(">d", v)
    if isinstance(v, str):
        return 8, write_string(v)
    if isinstance(v, dict):
        return 10, compound_of([(k, *nbt_value(x)) for k, x in v.items()])
    if isinstance(v, list):
        if not v:
            return 9, list_of(10, [])
        tid, _ = nbt_value(v[0])
        return 9, list_of(tid, [nbt_value(x)[1] for x in v])
    raise TypeError("不能编码为 NBT: %r" % (v,))


# ==================================================================== 安全写盘

_WRITTEN = []


def write(path, data):
    """写二进制产物。原子写：先写 .tmp 再改名 —— 中途出错也不会截断原文件。"""
    full = os.path.join(DATA, path)
    parent = os.path.dirname(full)
    if parent:
        os.makedirs(parent, exist_ok=True)
    tmp = full + ".tmp"
    with open(tmp, "wb") as f:
        f.write(data)
    os.replace(tmp, full)
    _WRITTEN.append(path)


def write_json(path, obj):
    """写 JSON（2 空格缩进、不转义中文、CRLF 行尾，与原产物格式一致）。"""
    text = json.dumps(obj, ensure_ascii=False, indent=2)
    write(path, text.replace("\n", "\r\n").encode("utf-8"))


# ==================================================================== 结构模板


class Palette:
    """自动去重的调色板：ref("minecraft:stone_bricks") -> state 序号。"""

    def __init__(self):
        self.entries = []          # [(name, props or None)]
        self.index = {}

    def ref(self, name, **props):
        key = (name, tuple(sorted(props.items())))
        if key not in self.index:
            self.index[key] = len(self.entries)
            self.entries.append((name, props or None))
        return self.index[key]


def build_template(w, h, d, palette, blocks, entities=None):
    """palette: [(name, props)]；blocks: [(x,y,z,idx,nbt)]；entities: [(x,y,z,nbt_dict)]"""
    dedup = {}
    for x, y, z, idx, nbt in blocks:
        dedup[(x, y, z)] = (x, y, z, idx, nbt)
    blocks = list(dedup.values())

    pal_entries = []
    for name, props in palette:
        ent = [("Name", 8, write_string(name))]
        if props:
            prop_entries = [(k, 8, write_string(str(v))) for k, v in props.items()]
            ent.append(("Properties", 10, compound_of(prop_entries)))
        pal_entries.append((10, compound_of(ent)))

    block_entries = []
    for x, y, z, idx, nbt in blocks:
        assert 0 <= x < w and 0 <= y < h and 0 <= z < d, "方块越界: %s" % ((x, y, z),)
        b = compound_of([("pos", 9, int_list([x, y, z])), ("state", 3, nbt_int(idx))]
                        + ([("nbt", 10, compound_of(
                            [(k, *nbt_value(v)) for k, v in nbt.items()]))] if nbt else []))
        block_entries.append((10, b))

    ent_entries = []
    for x, y, z, nbt in (entities or []):
        assert 0 <= x < w and 0 <= y < h and 0 <= z < d, "实体越界: %s" % ((x, y, z),)
        pos = [x + 0.5, float(y), z + 0.5]
        nbt = dict(nbt)
        nbt.setdefault("Pos", list(pos))      # 与原版模板一致：nbt 里也带 Pos
        e = compound_of([
            ("pos", 9, list_of(6, [struct.pack(">d", p) for p in pos])),
            ("blockPos", 9, int_list([x, y, z])),
            ("nbt", 10, compound_of([(k, *nbt_value(v)) for k, v in nbt.items()])),
        ])
        ent_entries.append((10, e))

    root = compound_of([
        ("DataVersion", 3, nbt_int(DATA_VERSION)),
        ("size", 9, int_list([w, h, d])),
        ("palette", 9, list_of(10, pal_entries)),
        ("blocks", 9, list_of(10, block_entries)),
        ("entities", 9, list_of(10, ent_entries)),
    ])
    return gzip.compress(write_named("", root), 9)


def mob_nbt(mob_id, yaw=0.0, extra=None):
    """一份最小可用的实体 NBT（结构模板实体）。"""
    nbt = {
        "id": mob_id,
        "PersistenceRequired": Byte(1),
        "Rotation": [Flt(yaw), Flt(0.0)],
    }
    if extra:
        nbt.update(extra)
    return nbt


# ==================================================================== 主题调色板


# 界域 → (石块, 砖块, 灯, 核心)
REALM_KITS = {
    "astral": ("astral_stone", "astral_bricks", "astral_lamp", "ruin_core"),
    "jade": ("jade_stone", "jade_bricks", "jade_lamp", "dragon_core_block"),
    "yao": ("yao_stone", "yao_bricks", "yao_lamp", "ruin_core"),
    # 第 16 轮：紫微 / 六壬 / 奇门 三界
    "ziwei": ("ziwei_stone", "ziwei_bricks", "ziwei_lamp", "ruin_core"),
    "liuren": ("liuren_stone", "liuren_bricks", "liuren_lamp", "ruin_core"),
    "qimen": ("qimen_stone", "qimen_bricks", "qimen_lamp", "ruin_core"),
    "taiyi": ("taiyi_stone", "taiyi_bricks", "taiyi_lamp", "ruin_core"),
}


def temple_palette(kind):
    """kind: astral / jade / yao / ziwei / liuren / qimen"""
    pal = Palette()
    s, b, l, c = REALM_KITS[kind]
    stone, brick, lamp, core = ("yishu:" + s, "yishu:" + b, "yishu:" + l, "yishu:" + c)
    refs = dict(
        AIR=pal.ref("minecraft:air"),
        CHEST=pal.ref("minecraft:chest", facing="north", type="single", waterlogged="false"),
        STONE=pal.ref(stone), BRICK=pal.ref(brick), LAMP=pal.ref(lamp),
        CORE=pal.ref(core),
        CIN=pal.ref("minecraft:cinnabar"), CIN_P=pal.ref("minecraft:polished_cinnabar"),
        CIN_C=pal.ref("minecraft:chiseled_cinnabar"), CIN_B=pal.ref("minecraft:cinnabar_bricks"),
        PILLAR=pal.ref("minecraft:end_rod") if kind == "astral" else pal.ref("minecraft:quartz_pillar"),
        GOLD=pal.ref("minecraft:gold_block"),
    )
    return pal, refs


# ==================================================================== 界域村庄（第 14 轮）

VILLAGE_SIZE = 32

# 界域 → 专属花（村庄庭院与裂隙用）
REALM_FLOWERS = {
    "astral": "yishu:astral_bloom",
    "jade": "yishu:jade_lily",
    "yao": "yishu:yao_bloom",
    "ziwei": "yishu:ziwei_bloom",
    "liuren": "yishu:liuren_lily",
    "qimen": "yishu:qimen_bloom",
    "taiyi": "yishu:taiyi_bloom",
}

# 界域 → 村床颜色
REALM_BEDS = {
    "astral": "minecraft:purple_bed",
    "jade": "minecraft:cyan_bed",
    "yao": "minecraft:black_bed",
    "ziwei": "minecraft:magenta_bed",
    "liuren": "minecraft:blue_bed",
    "qimen": "minecraft:orange_bed",
    "taiyi": "minecraft:light_blue_bed",
}

# 界域 → (作物, [(工作方块, 职业 id), ...], 箱子战利品表)
VILLAGE_KIT = {
    "astral": ("yishu:astral_wheat",
               [("yishu:qi_observatory", "yishu:astrologer"),
                ("yishu:censer", "yishu:fate_master")],
               "yishu:chests/astral_observatory"),
    "jade": ("yishu:jade_rice",
             [("yishu:bagua_platform", "yishu:geomancer"),
              ("yishu:shi_gandang", "yishu:surveyor")],
             "yishu:chests/jade_pagoda"),
    "yao": ("yishu:yao_bean",
            [("yishu:gua_board", "yishu:diviner"),
             ("yishu:altar", "yishu:yarrow_master")],
            "yishu:chests/yao_altar"),
    # —— 第 16 轮：三术界 ——
    "ziwei": ("yishu:ziwei_wheat",
              [("yishu:ziwei_terrace", "yishu:star_master"),
               ("yishu:dou_altar", "yishu:dou_master")],
              "yishu:chests/ziwei_terrace"),
    "liuren": ("yishu:liuren_rice",
               [("yishu:liuren_altar", "yishu:lesson_master"),
                ("yishu:guiren_altar", "yishu:guiren_master")],
               "yishu:chests/liuren_altar"),
    "qimen": ("yishu:qimen_bean",
              [("yishu:qimen_array", "yishu:array_master"),
               ("yishu:bamen_altar", "yishu:bamen_master")],
              "yishu:chests/qimen_array"),
    # —— 第 18 轮：太乙界 ——
    "taiyi": ("yishu:taiyi_wheat",
              [("yishu:ziwei_terrace", "yishu:star_master"),
               ("yishu:dou_altar", "yishu:dou_master")],
              "yishu:chests/taiyi_terrace"),
}


def build_village(kind, variant):
    """界域村庄（第 14 轮重做）：四座风格各异的民居 + 两块农田 + 双工位工坊 + 围墙村口 + 村民。

    布局（32×32；y=0 地基、y=1 地面、y=2..3 室内、y=4 屋顶）：

        z↑  民居A ┊  农田  ┊ 民居B
            道路 ┊ 广场+社坛 ┊ 道路
            民居C ┊   工坊   ┊ 民居D

    · 三种屋型（小屋 / 长屋 / 阁楼）按变种错位组合，三个变种各不相同；
    · 每户配床、柜、桌、灯——村民会睡、会用、会聚；
    · 工坊里立着该界域两位手艺人的工位，村民认领后即可交易；
    · 四边围墙 + 四面门楼，让台地的高差看起来像设计过的村墙。
    """
    pal, R = temple_palette(kind)
    crop, jobs, loot = VILLAGE_KIT[kind]
    S = VILLAGE_SIZE
    blocks = []
    entities = []

    def put(x, y, z, ref, nbt=None):
        blocks.append((x, y, z, ref, nbt))

    def fill(x0, y0, z0, x1, y1, z1, ref):
        for x in range(x0, x1 + 1):
            for y in range(y0, y1 + 1):
                for z in range(z0, z1 + 1):
                    put(x, y, z, ref)

    def ring(x0, y0, z0, x1, y1, z1, ref):
        for x in range(x0, x1 + 1):
            for y in range(y0, y1 + 1):
                put(x, y, z0, ref)
                put(x, y, z1, ref)
        for z in range(z0, z1 + 1):
            for y in range(y0, y1 + 1):
                put(x0, y, z, ref)
                put(x1, y, z, ref)

    # ---- 专用状态 ----
    AIR = R["AIR"]
    FARM = pal.ref("minecraft:farmland", moisture=7)
    WATER = pal.ref("minecraft:water", level=0)
    CROP = pal.ref(crop, age=3)
    TORCH = pal.ref("minecraft:torch")
    TABLE = pal.ref("minecraft:crafting_table")
    FLOWER = pal.ref(REALM_FLOWERS[kind])
    BED_ID = REALM_BEDS[kind]

    def work_ref(block_id):
        if block_id == "yishu:gua_board":
            return pal.ref(block_id, gua=str(variant * 21), facing="north")
        return pal.ref(block_id)

    def bed(x, y, z, facing):
        """床：foot 在下，head = foot 沿 facing 方向一格。"""
        off = {"north": (0, -1), "south": (0, 1), "east": (1, 0), "west": (-1, 0)}[facing]
        put(x, y, z, pal.ref(BED_ID, part="foot", facing=facing, occupied="false"))
        put(x + off[0], y, z + off[1],
            pal.ref(BED_ID, part="head", facing=facing, occupied="false"))

    def doorway(x0, z0, x1, z1, side):
        """门洞：在指定边开一个 2 宽、2 高的口子（走进去不撞头）。"""
        if side == "east":
            fill(x1, 2, z0 + 3, x1, 3, z0 + 4, AIR)
        elif side == "west":
            fill(x0, 2, z0 + 3, x0, 3, z0 + 4, AIR)
        elif side == "south":
            fill(x0 + 3, 2, z1, x0 + 4, 3, z1, AIR)
        else:
            fill(x0 + 3, 2, z0, x0 + 4, 3, z0, AIR)

    def roof_plate(x0, z0, x1, z1, y):
        """屋顶：向四外挑檐一格（但不越出围墙边界）。"""
        fill(max(1, x0 - 1), y, max(1, z0 - 1),
             min(S - 2, x1 + 1), y, min(S - 2, z1 + 1), R["BRICK"])

    # ================================================= 地基与地面
    fill(0, 0, 0, S - 1, 0, S - 1, R["BRICK"])          # 地基
    fill(0, 1, 0, S - 1, 1, S - 1, R["STONE"])          # 地面
    fill(14, 1, 0, 17, 1, S - 1, R["BRICK"])            # 南北主路
    fill(0, 1, 14, S - 1, 1, 17, R["BRICK"])            # 东西主路

    # ================================================= 围墙与四面门楼
    for x in range(1, S - 1):
        if 14 <= x <= 17:
            continue
        for z in (0, S - 1):
            put(x, 1, z, R["BRICK"])
            put(x, 2, z, R["BRICK"])
    for z in range(1, S - 1):
        if 14 <= z <= 17:
            continue
        for x in (0, S - 1):
            put(x, 1, z, R["BRICK"])
            put(x, 2, z, R["BRICK"])
    # 门楼：立柱 + 灯
    for (gx, gz) in ((13, 0), (18, 0), (13, S - 1), (18, S - 1),
                     (0, 13), (0, 18), (S - 1, 13), (S - 1, 18)):
        for y in (1, 2, 3):
            put(gx, y, gz, R["BRICK"])
        put(gx, 4, gz, R["LAMP"])
    # 门楣横梁
    fill(14, 4, 0, 17, 4, 0, R["BRICK"])
    fill(14, 4, S - 1, 17, 4, S - 1, R["BRICK"])
    fill(0, 4, 14, 0, 4, 17, R["BRICK"])
    fill(S - 1, 4, 14, S - 1, 4, 17, R["BRICK"])
    # 四角望柱
    for (cx, cz) in ((0, 0), (S - 1, 0), (0, S - 1), (S - 1, S - 1)):
        for y in (1, 2, 3):
            put(cx, y, cz, R["BRICK"])
        put(cx, 4, cz, R["LAMP"])

    # ================================================= 中央广场 + 社坛
    fill(12, 1, 12, 19, 1, 19, R["CIN_P"])
    for (lx, lz) in ((12, 12), (19, 12), (12, 19), (19, 19)):
        put(lx, 2, lz, R["LAMP"])
    for (cx, cz) in ((13, 13), (18, 13), (13, 18), (18, 18)):
        for y in (1, 2, 3):
            put(cx, y, cz, R["PILLAR"])
    fill(14, 4, 14, 17, 4, 17, R["BRICK"])
    put(15, 5, 15, R["CORE"])
    put(16, 5, 16, R["LAMP"])

    # ================================================= 四座民居（三种屋型 × 变种错位）
    designs = [
        ["hut", "long", "hut", "tower"],
        ["long", "tower", "long", "hut"],
        ["tower", "hut", "tower", "long"],
    ][variant % 3]
    corners = ["NW", "NE", "SW", "SE"]
    DOORS = {"NW": ("east", "south"), "NE": ("west", "south"),
             "SW": ("east", "north"), "SE": ("west", "north")}

    def house_box(corner, design):
        w, d = {"hut": (7, 7), "long": (9, 7), "tower": (7, 7)}[design]
        if corner == "NW":
            x0, z0 = 2, 2
        elif corner == "NE":
            x0, z0 = 31 - w, 2
        elif corner == "SW":
            x0, z0 = 2, 31 - d
        else:
            x0, z0 = 31 - w, 31 - d
        return x0, z0, x0 + w - 1, z0 + d - 1

    def house_hut(x0, z0, x1, z1, doors):
        """小屋：单层，平顶挑檐，屋顶一盏灯。"""
        fill(x0, 1, z0, x1, 1, z1, R["STONE"])
        ring(x0, 1, z0, x1, 3, z1, R["BRICK"])
        fill(x0 + 1, 2, z0 + 1, x1 - 1, 3, z1 - 1, AIR)
        roof_plate(x0, z0, x1, z1, 4)
        put(x0 + 3, 5, z0 + 3, R["LAMP"])
        for side in doors:
            doorway(x0, z0, x1, z1, side)
        bed(x0 + 1, 2, z0 + 1, "south")
        put(x0 + 5, 2, z0 + 1, R["CHEST"], {"LootTable": loot})
        put(x0 + 5, 2, z0 + 2, TABLE)
        put(x0 + 3, 2, z0 + 4, TORCH)
        put(x0, 2, z0 + 2, R["LAMP"])
        put(x1, 2, z0 + 2, R["LAMP"])

    def house_long(x0, z0, x1, z1, doors):
        """长屋：9×7，屋脊 + 双卧 + 壁灯窗。"""
        fill(x0, 1, z0, x1, 1, z1, R["STONE"])
        ring(x0, 1, z0, x1, 3, z1, R["BRICK"])
        fill(x0 + 1, 2, z0 + 1, x1 - 1, 3, z1 - 1, AIR)
        roof_plate(x0, z0, x1, z1, 4)
        for x in range(x0 + 1, x1):
            put(x, 5, z0 + 3, R["LAMP"] if x == x0 + 4 else R["BRICK"])
        for side in doors:
            doorway(x0, z0, x1, z1, side)
        bed(x0 + 1, 2, z0 + 1, "south")
        bed(x0 + 7, 2, z0 + 1, "south")
        put(x0 + 3, 2, z0 + 1, R["CHEST"], {"LootTable": loot})
        put(x0 + 5, 2, z0 + 1, TABLE)
        put(x0, 2, z0 + 2, R["LAMP"])
        put(x1, 2, z0 + 2, R["LAMP"])

    def house_tower(x0, z0, x1, z1, doors):
        """阁楼：两层，上层挑窗，屋脊加高。"""
        fill(x0, 1, z0, x1, 1, z1, R["STONE"])            # 一层地板
        ring(x0, 1, z0, x1, 3, z1, R["BRICK"])            # 一层墙
        fill(x0 + 1, 2, z0 + 1, x1 - 1, 3, z1 - 1, AIR)   # 一层净空
        fill(x0, 4, z0, x1, 4, z1, R["BRICK"])            # 二层楼板
        ring(x0, 4, z0, x1, 6, z1, R["BRICK"])            # 二层墙
        fill(x0 + 1, 5, z0 + 1, x1 - 1, 6, z1 - 1, AIR)   # 二层净空
        roof_plate(x0, z0, x1, z1, 7)                     # 屋顶
        for x in range(x0 + 1, x1):
            put(x, 8, z0 + 3, R["LAMP"] if x == x0 + 3 else R["BRICK"])
        for side in doors:
            doorway(x0, z0, x1, z1, side)
        put(x0, 5, z0 + 3, R["LAMP"])                     # 二层挑窗
        put(x1, 5, z0 + 3, R["LAMP"])
        put(x0 + 1, 2, z0 + 1, R["CHEST"], {"LootTable": loot})
        put(x0 + 5, 2, z0 + 1, TABLE)
        put(x0 + 3, 2, z0 + 3, TORCH)
        bed(x0 + 1, 5, z0 + 1, "south")
        put(x0 + 5, 5, z0 + 5, TORCH)

    build_house = {"hut": house_hut, "long": house_long, "tower": house_tower}
    for corner, design in zip(corners, designs):
        hx0, hz0, hx1, hz1 = house_box(corner, design)
        build_house[design](hx0, hz0, hx1, hz1, DOORS[corner])

    # ================================================= 两块农田
    for (fx0, fz0) in ((13, 2), (13, 22)):
        for dx in range(7):
            for dz in range(7):
                x, z = fx0 + dx, fz0 + dz
                if dx == 3:                       # 中央水渠
                    put(x, 1, z, WATER)
                    continue
                put(x, 1, z, FARM)
                if 0 < dx < 6 and 0 < dz < 6:
                    put(x, 2, z, CROP)
        put(fx0 - 1, 2, fz0 - 1, FLOWER)
        put(fx0 + 7, 2, fz0 + 7, FLOWER)

    # ================================================= 工坊（双工位）
    gx0, gz0, gx1, gz1 = 12, 9, 19, 12
    fill(gx0, 1, gz0, gx1, 1, gz1, R["STONE"])
    ring(gx0, 1, gz0, gx1, 3, gz1, R["BRICK"])
    fill(gx0 + 1, 2, gz0 + 1, gx1 - 1, 3, gz1 - 1, AIR)
    fill(gx0 - 1, 4, gz0 - 1, gx1 + 1, 4, gz1 + 1, R["BRICK"])
    fill(gx0 + 3, 2, gz1, gx0 + 4, 3, gz1, AIR)          # 朝广场开门
    put(14, 2, gz0 + 1, work_ref(jobs[0][0]))            # 工位一
    put(17, 2, gz0 + 1, work_ref(jobs[1][0]))            # 工位二
    put(gx0 + 1, 2, gz1 - 1, R["CHEST"], {"LootTable": loot})
    put(gx1 - 1, 2, gz1 - 1, R["CHEST"], {"LootTable": loot})
    put(gx0, 2, gz0 + 1, R["LAMP"])
    put(gx1, 2, gz0 + 1, R["LAMP"])

    # ================================================= 村民
    def villager(x, z, yaw, profession):
        extra = {
            "VillagerData": {
                "type": "minecraft:plains",
                "profession": profession or "minecraft:none",
                "level": 1,
            },
            "AssignProfessionWhenSpawned": Byte(1),
        }
        if profession:
            # 手艺人带一点经验：避免「有职业但尚未认领工位」的瞬间被
            # ResetProfession 清回无业（它只重置 0 经验、1 级的村民）。
            extra["Xp"] = 1
        entities.append((x, 2, z, mob_nbt("minecraft:villager", yaw, extra)))

    villager(15, 11, 180.0, jobs[0][1])       # 工坊内 · 手艺甲
    villager(16, 11, 0.0, jobs[1][1])         # 工坊内 · 手艺乙
    villager(15, 18, 90.0, None)              # 广场上 · 待业
    villager(16, 20, 270.0, None)             # 南街 · 待业

    return build_template(S, 12, S, pal.entries, blocks, entities)


VILLAGE_STRUCTURES = [
    # (key, kind, 生物群系标签, spacing, separation, salt)
    ("astral_village", "astral", "#yishu:astral_field", 44, 15, 20261031),
    ("jade_village", "jade", "#yishu:vein_valley", 44, 15, 20261032),
    ("yao_village", "yao", "#yishu:hexagram_plain", 44, 15, 20261033),
    # 三术界村庄
    ("ziwei_village", "ziwei", "#yishu:ziwei_field", 44, 15, 20261034),
    ("liuren_village", "liuren", "#yishu:liuren_field", 44, 15, 20261035),
    ("qimen_village", "qimen", "#yishu:qimen_field", 44, 15, 20261036),
    ("taiyi_village", "taiyi", "#yishu:taiyi_field", 44, 15, 20261037),
]


# ==================================================================== 维度裂隙（入侵感）

RIFT_STRUCTURES = [
    # (key, kind, 生物群系标签, spacing, separation, salt)
    ("astral_rift", "astral", "#minecraft:is_overworld", 30, 9, 20261041),
    ("jade_rift", "jade", "#minecraft:is_overworld", 30, 9, 20261042),
    ("yao_rift", "yao", "#minecraft:is_overworld", 30, 9, 20261043),
    # 三术界裂隙（主世界的入侵感）
    ("ziwei_rift", "ziwei", "#minecraft:is_overworld", 30, 9, 20261044),
    ("liuren_rift", "liuren", "#minecraft:is_overworld", 30, 9, 20261045),
    ("qimen_rift", "qimen", "#minecraft:is_overworld", 30, 9, 20261046),
    ("taiyi_rift", "taiyi", "#minecraft:is_overworld", 30, 9, 20261047),
]

# 界域 → (液体, 花)
RIFT_KIT = {
    "astral": ("yishu:astral_dew", "yishu:astral_bloom"),
    "jade": ("yishu:jade_ichor", "yishu:jade_lily"),
    "yao": ("yishu:yao_ink", "yishu:yao_bloom"),
    "ziwei": ("yishu:ziwei_ambrosia", "yishu:ziwei_bloom"),
    "liuren": ("yishu:liuren_water", "yishu:liuren_lily"),
    "qimen": ("yishu:qimen_mercury", "yishu:qimen_grass"),
    "taiyi": ("yishu:taiyi_spring", "yishu:taiyi_bloom"),
}


def build_rift(kind, variant):
    """维度裂隙 —— 主世界地表被别界「顶」出来的一小片。

    它是「入侵感」最直接的载体：走在主世界的野地里，
    忽然遇到一圈不属于这里的石头、一汪不属于这里的水，
    抬头还立着一盏不属于这里的灯。裂隙不大，但它告诉你：
    那三个界域，其实一直贴在这层世界的背面。
    """
    pal, R = temple_palette(kind)
    fluid, flower = RIFT_KIT[kind]
    S = 9
    blocks = []

    def put(x, y, z, ref, nbt=None):
        blocks.append((x, y, z, ref, nbt))

    def fill(x0, y0, z0, x1, y1, z1, ref):
        for x in range(x0, x1 + 1):
            for y in range(y0, y1 + 1):
                for z in range(z0, z1 + 1):
                    put(x, y, z, ref)

    FLUID = pal.ref(fluid, level=0)
    FLOWER = pal.ref(flower)
    CRACK = pal.ref("minecraft:cracked_stone_bricks")

    # 斜向的裂口：一条从角落通向中心的裂缝
    cells = [(1, 1), (2, 2), (3, 2), (2, 3), (4, 3), (3, 4), (4, 4),
             (5, 4), (4, 5), (5, 5), (6, 5), (5, 6), (6, 6)]
    for x, z in cells:
        put(x, 0, z, R["BRICK"])
        if (x + z) % 3 == 0:
            put(x, 0, z, CRACK)

    # 中心：界域石 + 核心
    fill(3, 0, 3, 5, 0, 5, R["STONE"])
    put(4, 1, 4, R["CORE"])

    # 灯柱（三格高，顶上一盏界域灯）
    put(4, 2, 4, R["PILLAR"])
    put(4, 3, 4, R["PILLAR"])
    put(4, 4, 4, R["LAMP"])

    # 液体池（斜对角的一小汪）
    for x, z in ((6, 2), (7, 2), (6, 3), (7, 3), (7, 4)):
        put(x, 0, z, FLUID)

    # 边缘的界域花草
    for x, z in ((1, 6), (2, 7), (7, 6), (6, 7), (1, 2)):
        put(x, 1, z, FLOWER)

    # 把最外圈挖开一点，让裂隙看起来是「顶出来」的
    for i in range(S):
        put(i, 1, 0, R["AIR"])
        put(i, 1, S - 1, R["AIR"])
        put(0, 1, i, R["AIR"])
        put(S - 1, 1, i, R["AIR"])

    return build_template(S, 6, S, pal.entries, blocks, [])


# ==================================================================== 流浪商人交易


def trade_json(gives_id, gives_count, wants_id, wants_count=1):
    return {
        "gives": {"id": gives_id, "count": gives_count},
        "reputation_discount": 0.05,
        "wants": {"id": wants_id, "count": wants_count},
    }


def gen_trades():
    print("== 流浪商人交易 ==")
    write_json("yishu/villager_trade/wandering_trader/emerald_bronze_coins.json",
               trade_json("yishu:bronze_coins", 4, "minecraft:emerald"))
    write_json("yishu/villager_trade/wandering_trader/emerald_yarrow_stalks.json",
               trade_json("yishu:yarrow_stalks", 1, "minecraft:emerald"))
    write_json("yishu/villager_trade/wandering_trader/bronze_coins_emerald.json",
               trade_json("minecraft:emerald", 1, "yishu:bronze_coins", 6))
    write_json("yishu/villager_trade/wandering_trader/emerald_peek_charm.json",
               trade_json("yishu:peek_charm", 1, "minecraft:emerald", 8))
    write_json("yishu/villager_trade/wandering_trader/emerald_cinnabar.json",
               trade_json("minecraft:cinnabar", 4, "minecraft:emerald"))
    write_json("yishu/villager_trade/wandering_trader/emerald_astral_key.json",
               trade_json("yishu:astral_key", 1, "minecraft:emerald", 24))
    write_json("yishu/villager_trade/wandering_trader/emerald_jade_key.json",
               trade_json("yishu:jade_key", 1, "minecraft:emerald", 24))
    write_json("yishu/villager_trade/wandering_trader/emerald_yao_key.json",
               trade_json("yishu:yao_key", 1, "minecraft:emerald", 24))

    write_json("minecraft/tags/villager_trade/wandering_trader/common.json", {
        "replace": False,
        "values": [
            "yishu:wandering_trader/emerald_bronze_coins",
            "yishu:wandering_trader/emerald_yarrow_stalks",
            "yishu:wandering_trader/bronze_coins_emerald",
        ],
    })
    write_json("minecraft/tags/villager_trade/wandering_trader/uncommon.json", {
        "replace": False,
        "values": [
            "yishu:wandering_trader/emerald_peek_charm",
            "yishu:wandering_trader/emerald_cinnabar",
            "yishu:wandering_trader/emerald_astral_key",
            "yishu:wandering_trader/emerald_jade_key",
            "yishu:wandering_trader/emerald_yao_key",
        ],
    })


def gen_villager_trades():
    """主题商品注入原版村民职业交易池（数据包方式）。

    ⚠️ 背景：曾经用 Java 在运行时 {@code villager.getOffers().add(...)} 直接加交易，
    结果是玩家一打开交易界面就被服务端判定失效而关闭（秒闪退）。
    现在改走 26.2 官方机制：
      · 交易本体 → data/yishu/villager_trade/<职业>/<等级>/<名字>.json
      · 注入职业池 → data/minecraft/tags/villager_trade/<职业>/level_<n>.json
        （同名 tag 文件会被游戏合并，replace=false 保证不动原版内容）
    """
    print("== 村民主题交易（数据包注入）==")
    # (职业, 等级, 名字, 给出 id, 给出数量, 想要 id, 想要数量, 最大使用, 声望折扣)
    trades = [
        # 图书管理员：命理（书与卷）
        ("librarian", 2, "emerald_fate_slip", "yishu:fate_slip", 1, "minecraft:emerald", 12, 4.0, 0.10),
        ("librarian", 3, "emerald_ancient_scroll", "yishu:ancient_scroll", 1, "minecraft:emerald", 10, 6.0, 0.10),
        # 制图师：风水（图与罗盘）
        ("cartographer", 2, "emerald_vein_map", "yishu:vein_map", 1, "minecraft:emerald", 14, 3.0, 0.10),
        ("cartographer", 3, "emerald_feng_shui_compass",
         "yishu:feng_shui_compass", 1, "minecraft:emerald", 18, 3.0, 0.10),
        # 神职：符箓与丹药
        ("cleric", 1, "emerald_talisman_paper", "yishu:talisman_paper", 4, "minecraft:emerald", 2, 12.0, 0.05),
        ("cleric", 2, "emerald_clarity_pill", "yishu:clarity_pill", 1, "minecraft:emerald", 6, 6.0, 0.05),
        # 农民：蓍草（六爻起卦的本草）
        ("farmer", 1, "emerald_yarrow_stalks", "yishu:yarrow_stalks", 1, "minecraft:emerald", 3, 12.0, 0.05),
        # 石匠：界域建材
        ("mason", 1, "emerald_astral_bricks", "yishu:astral_bricks", 8, "minecraft:emerald", 4, 8.0, 0.05),
        ("mason", 2, "emerald_jade_bricks", "yishu:jade_bricks", 8, "minecraft:emerald", 4, 8.0, 0.05),
        # 武器匠：护身符
        ("weaponsmith", 2, "emerald_ward_talisman", "yishu:ward_talisman", 2, "minecraft:emerald", 7, 6.0, 0.05),
        # 屠夫：归元丹
        ("butcher", 2, "emerald_qi_pill", "yishu:qi_pill", 1, "minecraft:emerald", 8, 6.0, 0.05),
        # 牧羊人：界域绒材
        ("shepherd", 1, "emerald_jade_wool", "yishu:jade_wool", 2, "minecraft:emerald", 8, 6.0, 0.05),
    ]
    by_pool = {}
    for prof, level, name, give, gcount, want, wcount, uses, disc in trades:
        write_json(f"yishu/villager_trade/{prof}/{level}/{name}.json", {
            "gives": {"id": give, "count": gcount},
            "max_uses": uses,
            "reputation_discount": disc,
            "wants": {"id": want, "count": wcount},
        })
        by_pool.setdefault((prof, level), []).append(f"yishu:{prof}/{level}/{name}")
    for (prof, level), ids in sorted(by_pool.items()):
        write_json(f"minecraft/tags/villager_trade/{prof}/level_{level}.json", {
            "replace": False,
            "values": sorted(ids),
        })


def gen_realm_jobs():
    """界域职业的交易链（第 13 轮建立，第 14 轮扩充到六职业 × 三级）。

    三层结构，和原版职业完全一致：
      · villager_trade/<职业>/<等级>/<名字>.json —— 交易本体
      · tags/villager_trade/<职业>/level_<n>.json —— 该等级有哪些交易
      · trade_set/<职业>/level_<n>.json —— 从标签里抽几条

    职业本体在 Java 侧注册（YsVillagerContent），村民认领工作方块即转职。
    """
    print("== 界域职业交易 ==")
    # (等级, 名字, 给出 id, 给出数量, 想要 id, 想要数量, 最大使用, 声望折扣)
    jobs = {
        # 星象师 · 观气台 —— 观星者的草木与命书
        "astrologer": [
            (1, "emerald_yarrow_stalks", "yishu:yarrow_stalks", 1, "minecraft:emerald", 3, 12.0, 0.05),
            (1, "astral_grain_emerald", "minecraft:emerald", 1, "yishu:astral_grain", 6, 16.0, 0.05),
            (2, "emerald_fate_slip", "yishu:fate_slip", 1, "minecraft:emerald", 12, 4.0, 0.10),
            (2, "emerald_astral_dust", "yishu:astral_dust", 4, "minecraft:emerald", 4, 8.0, 0.05),
            (3, "star_crystal_emerald", "minecraft:emerald", 1, "yishu:star_crystal", 2, 10.0, 0.10),
            (3, "emerald_fate_book", "yishu:fate_book", 1, "minecraft:emerald", 14, 3.0, 0.10),
        ],
        # 司命 · 香炉 —— 掌命数者的符与丹
        "fate_master": [
            (1, "astral_grain_emerald", "minecraft:emerald", 1, "yishu:astral_grain", 6, 16.0, 0.05),
            (1, "emerald_peek_charm", "yishu:peek_charm", 1, "minecraft:emerald", 4, 12.0, 0.05),
            (2, "star_fur_emerald", "minecraft:emerald", 1, "yishu:star_fur", 4, 10.0, 0.05),
            (2, "emerald_taisui_pill", "yishu:taisui_pill", 1, "minecraft:emerald", 18, 3.0, 0.10),
            (3, "oracle_bone_emerald", "minecraft:emerald", 1, "yishu:oracle_bone", 3, 8.0, 0.10),
            (3, "emerald_ward_pill", "yishu:ward_pill", 1, "minecraft:emerald", 10, 8.0, 0.05),
        ],
        # 风水师 · 八卦台 —— 望气者的图与盘
        "geomancer": [
            (1, "emerald_astral_bricks", "yishu:astral_bricks", 8, "minecraft:emerald", 4, 8.0, 0.05),
            (1, "jade_grain_emerald", "minecraft:emerald", 1, "yishu:jade_grain", 6, 16.0, 0.05),
            (2, "emerald_feng_shui_compass", "yishu:feng_shui_compass", 1, "minecraft:emerald", 18, 3.0, 0.10),
            (2, "emerald_vein_map", "yishu:vein_map", 1, "minecraft:emerald", 14, 3.0, 0.10),
            (3, "jade_grit_emerald", "minecraft:emerald", 1, "yishu:jade_grit", 4, 10.0, 0.05),
            (3, "emerald_yinyang_stone", "yishu:yinyang_stone", 2, "minecraft:emerald", 6, 8.0, 0.05),
        ],
        # 堪舆师 · 石敢当 —— 勘地者的尺与镇石
        "surveyor": [
            (1, "jade_grain_emerald", "minecraft:emerald", 1, "yishu:jade_grain", 6, 16.0, 0.05),
            (1, "emerald_shi_gandang", "yishu:shi_gandang", 2, "minecraft:emerald", 6, 8.0, 0.05),
            (2, "vein_crystal_emerald", "minecraft:emerald", 1, "yishu:vein_crystal", 2, 8.0, 0.10),
            (2, "emerald_dragon_ruler", "yishu:dragon_ruler", 1, "minecraft:emerald", 20, 3.0, 0.10),
            (3, "meteor_iron_emerald", "minecraft:emerald", 1, "yishu:meteor_iron", 4, 8.0, 0.05),
            (3, "emerald_vein_map", "yishu:vein_map", 1, "minecraft:emerald", 12, 3.0, 0.10),
        ],
        # 卜者 · 卦盘 —— 起卦者的纸与药
        "diviner": [
            (1, "emerald_talisman_paper", "yishu:talisman_paper", 4, "minecraft:emerald", 2, 12.0, 0.05),
            (1, "yao_pod_emerald", "minecraft:emerald", 1, "yishu:yao_pod", 6, 16.0, 0.05),
            (2, "emerald_qi_pill", "yishu:qi_pill", 1, "minecraft:emerald", 8, 6.0, 0.05),
            (2, "emerald_clarity_pill", "yishu:clarity_pill", 1, "minecraft:emerald", 6, 6.0, 0.05),
            (3, "yao_shard_emerald", "minecraft:emerald", 1, "yishu:yao_shard", 4, 10.0, 0.05),
            (3, "emerald_ancient_scroll", "yishu:ancient_scroll", 1, "minecraft:emerald", 16, 3.0, 0.10),
        ],
        # 蓍师 · 祭坛 —— 蓍草与通灵法器
        "yarrow_master": [
            (1, "yao_pod_emerald", "minecraft:emerald", 1, "yishu:yao_pod", 6, 16.0, 0.05),
            (1, "emerald_yarrow_stalks", "yishu:yarrow_stalks", 1, "minecraft:emerald", 3, 12.0, 0.05),
            (2, "yao_shard_emerald", "minecraft:emerald", 1, "yishu:yao_shard", 3, 10.0, 0.05),
            (2, "emerald_bagua_mirror", "yishu:bagua_mirror", 1, "minecraft:emerald", 16, 3.0, 0.10),
            (3, "turtle_shell_emerald", "minecraft:emerald", 1, "yishu:turtle_shell", 2, 8.0, 0.10),
            (3, "emerald_taisui_talisman", "yishu:taisui_talisman", 1, "minecraft:emerald", 14, 3.0, 0.10),
        ],
    }
    for prof, entries in sorted(jobs.items()):
        by_level = {}
        for level, name, give, gcount, want, wcount, uses, disc in entries:
            write_json(f"yishu/villager_trade/{prof}/{level}/{name}.json", {
                "gives": {"id": give, "count": gcount},
                "max_uses": uses,
                "reputation_discount": disc,
                "wants": {"id": want, "count": wcount},
            })
            by_level.setdefault(level, []).append(f"yishu:{prof}/{level}/{name}")
        for level, ids in sorted(by_level.items()):
            write_json(f"yishu/tags/villager_trade/{prof}/level_{level}.json", {
                "replace": False,
                "values": sorted(ids),
            })
            write_json(f"yishu/trade_set/{prof}/level_{level}.json", {
                "amount": 2.0,
                "random_sequence": f"yishu:trade_set/{prof}/level_{level}",
                "trades": f"#yishu:{prof}/level_{level}",
            })


def gen_workstation_recipes():
    """六座工位的合成配方 —— 想自己搭一间工坊，就自己砌。"""
    print("== 工位配方 ==")
    recipes = {
        "qi_observatory": ("yishu:astral_stone", "yishu:star_crystal"),
        "censer": ("minecraft:stone_bricks", "yishu:bronze_coins"),
        "bagua_platform": ("yishu:jade_stone", "yishu:bagua_mirror"),
        "shi_gandang": ("minecraft:stone", "yishu:yinyang_stone"),
        "gua_board": ("yishu:yao_stone", "yishu:oracle_bone"),
        "altar": ("minecraft:stone_bricks", "yishu:yarrow_stalks"),
        # 第 16 轮：三术界第二工位
        "dou_altar": ("yishu:ziwei_stone", "yishu:ziwei_crystal"),
        "guiren_altar": ("yishu:liuren_stone", "yishu:liuren_crystal"),
        "bamen_altar": ("yishu:qimen_stone", "yishu:qimen_crystal"),
    }
    for name, (shell, core) in sorted(recipes.items()):
        write_json(f"yishu/recipe/{name}.json", {
            "type": "minecraft:crafting_shaped",
            "category": "misc",
            "key": {"S": shell, "C": core},
            "pattern": ["SSS", "SCS", "SSS"],
            "result": {"id": f"yishu:{name}"},
        })


def gen_mineable_tags():
    """模组方块并入原版挖掘标签（第 15 轮修复）。

    ⚠️ 关键坑：`requiresCorrectToolForDrops()` 只声明「需要正确工具」，
    「什么是正确工具」则完全由 `#minecraft:mineable/pickaxe` 这类标签说了算。
    只设属性、不进标签 = 拿镐也挖不掉。矿石、砖块、工位统统踩这个坑。
    """
    print("== 挖掘标签（并入原版 mineable/*）==")

    pickaxe = [
        # 十二种矿
        "astral_ore", "meteor_ore", "star_sand_ore", "crystal_ore",
        "jade_ore", "verdant_ore", "mica_ore", "vein_ore",
        "yao_ore", "ink_iron_ore", "onyx_ore", "oracle_ore",
        # 三界建材（石 / 砖 / 柱 / 台阶 / 楼梯 / 灯）
        "astral_stone", "astral_bricks", "astral_pillar", "astral_slab", "astral_stairs",
        "jade_stone", "jade_bricks", "jade_pillar", "jade_slab", "jade_stairs",
        "yao_stone", "yao_bricks", "yao_pillar", "yao_slab", "yao_stairs",
        "astral_lamp", "jade_lamp", "yao_lamp",
        # 五行灵石块
        "wood_stone_block", "fire_stone_block", "earth_stone_block",
        "metal_stone_block", "water_stone_block",
        # 三座门户（石质）
        "astral_gate", "jade_gate", "yao_gate",
        # 工位与法器（石头 / 铜）
        "shi_gandang", "bagua_platform", "taiji_diagram", "altar", "censer",
        "ziwei_terrace", "liuren_altar", "qimen_array",
        # 遗迹与龙脉
        "ruin_core", "yinyang_stele", "dragon_core_block",
        # ---- 第 16 轮：三术界矿石 ----
        "ziwei_crystal_ore", "dou_ore", "ji_ore", "yuan_ore",
        "ren_ore", "gui_ore", "she_ore", "xuan_ore",
        "men_ore", "xing_ore", "dun_ore", "jia_ore",
        # 三术界建材与术坛
        "ziwei_stone", "ziwei_bricks", "ziwei_lamp",
        "liuren_stone", "liuren_bricks", "liuren_lamp",
        "qimen_stone", "qimen_bricks", "qimen_lamp",
        "dou_altar", "guiren_altar", "bamen_altar",
        # 三术界门户
        "ziwei_gate", "liuren_gate", "qimen_gate",
        # ---- 第 19 轮：太乙界 ----
        "taiyi_ore", "wenchang_ore", "shiji_ore", "zhuke_ore",
        "taiyi_stone", "taiyi_bricks", "taiyi_lamp",
        "taiyi_gate", "taiyi_terrace", "wenchang_altar",
        # ---- 第 20 轮：镇物 ----
        "zhenzhai_stone", "huasha_stone",
    ]
    axe = [
        # 九种界域原木
        "astral_wood_log", "meteor_wood_log", "moon_wood_log",
        "jade_wood_log", "cloud_wood_log", "peach_wood_log",
        "hexa_wood_log", "ink_wood_log", "mystery_wood_log",
        # 木质工位
        "qi_observatory", "gua_board",
        # ---- 第 16 轮：三术界九树种原木 ----
        "ziwei_wood_log", "dou_wood_log", "tianji_wood_log",
        "ren_wood_log", "gui_wood_log", "she_wood_log",
        "men_wood_log", "xing_wood_log", "dun_wood_log",
    ]
    hoe = [
        # 九种界域树叶（与原版 leaves 一致：锄头最快）
        "astral_wood_leaves", "meteor_wood_leaves", "moon_wood_leaves",
        "jade_wood_leaves", "cloud_wood_leaves", "peach_wood_leaves",
        "hexa_wood_leaves", "ink_wood_leaves", "mystery_wood_leaves",
        # ---- 第 16 轮：三术界九树种树叶 ----
        "ziwei_wood_leaves", "dou_wood_leaves", "tianji_wood_leaves",
        "ren_wood_leaves", "gui_wood_leaves", "she_wood_leaves",
        "men_wood_leaves", "xing_wood_leaves", "dun_wood_leaves",
    ]

    for tool, names in (("pickaxe", pickaxe), ("axe", axe), ("hoe", hoe)):
        write_json(f"minecraft/tags/block/mineable/{tool}.json", {
            "replace": False,
            "values": sorted(f"yishu:{n}" for n in names),
        })


def gen_wood_tags():
    """把界域林木并入原版方块标签 —— 树木识别、落点判断、树冠统计都靠它。"""
    print("== 林木标签（并入原版 leaves / logs）==")
    woods = ["astral_wood", "meteor_wood", "moon_wood",
             "jade_wood", "cloud_wood", "peach_wood",
             "hexa_wood", "ink_wood", "mystery_wood",
             # 第 16 轮
             "ziwei_wood", "dou_wood", "tianji_wood",
             "ren_wood", "gui_wood", "she_wood",
             "men_wood", "xing_wood", "dun_wood",
             # 第 19 轮
             "taiyi_wood", "wenchang_wood", "jishen_wood"]
    write_json("minecraft/tags/block/leaves.json", {
        "replace": False,
        "values": sorted(f"yishu:{w}_leaves" for w in woods),
    })
    write_json("minecraft/tags/block/logs.json", {
        "replace": False,
        "values": sorted(f"yishu:{w}_log" for w in woods),
    })


def gen_poi_tags():
    """把界域工位并入原版「可认领工位」POI 标签（第 14 轮的关键修复）。

    ⚠️ 无业村民寻找工位时用的谓词是 `#minecraft:acquirable_job_site` 标签 ——
    不进这个标签的兴趣点类型，村民永远「看不见」，也就永远无法转职；
    而且身怀职业却认不到工位的村民会被 ResetProfession 重置回无业。
    `#minecraft:village` 引用本标签，所以补这里就够了。
    """
    print("== 工位 POI 标签（并入原版 acquirable_job_site）==")
    write_json("minecraft/tags/point_of_interest_type/acquirable_job_site.json", {
        "replace": False,
        "values": [
            "yishu:astrologer",
            "yishu:fate_master",
            "yishu:geomancer",
            "yishu:surveyor",
            "yishu:diviner",
            "yishu:yarrow_master",
            "yishu:star_master",
            "yishu:lesson_master",
            "yishu:array_master",
            "yishu:dou_master",
            "yishu:guiren_master",
            "yishu:bamen_master",
        ],
    })


def gen_oracle_jobs():
    """三大术数职业的交易链（第 15 轮）。

    星命师卖紫微系的令，课师卖六壬系的符，局师卖奇门系的符。
    三张盘是各自三级的主打货 —— 想自己起盘，就得先跟手艺人混熟。

    三层结构照旧：villager_trade 定义交易本体，tags 汇总该等级的交易，
    trade_set 从标签里抽两条。和原版职业一模一样。
    """
    print("== 三大术数职业交易 ==")
    jobs = {
        # 星命师 · 紫微星台
        "star_master": [
            (1, "emerald_ziwei_star_dust", "yishu:ziwei_star_dust", 3, "minecraft:emerald", 2, 12.0, 0.05),
            (1, "emerald_ziwei_token", "yishu:ziwei_token", 1, "minecraft:emerald", 6, 8.0, 0.05),
            (2, "emerald_ziwei_chart", "yishu:ziwei_chart", 1, "minecraft:emerald", 18, 3.0, 0.10),
            (2, "emerald_qisha_token", "yishu:qisha_token", 1, "minecraft:emerald", 12, 4.0, 0.10),
            (3, "emerald_tianfu_token", "yishu:tianfu_token", 1, "minecraft:emerald", 14, 4.0, 0.10),
        ],
        # 课师 · 六壬式坛
        "lesson_master": [
            (1, "emerald_liuren_shell", "yishu:liuren_shell", 4, "minecraft:emerald", 2, 12.0, 0.05),
            (1, "emerald_guiren_talisman", "yishu:guiren_talisman", 1, "minecraft:emerald", 5, 8.0, 0.05),
            (2, "emerald_liuren_plate", "yishu:liuren_plate", 1, "minecraft:emerald", 18, 3.0, 0.10),
            (2, "emerald_qinglong_talisman", "yishu:qinglong_talisman", 1, "minecraft:emerald", 10, 4.0, 0.10),
            (3, "emerald_baihu_talisman", "yishu:baihu_talisman", 1, "minecraft:emerald", 14, 4.0, 0.10),
        ],
        # 斗枢师 · 斗枢台（紫微界第二工位）
        "dou_master": [
            (1, "emerald_ziwei_crystal", "yishu:ziwei_crystal", 3, "minecraft:emerald", 2, 12.0, 0.05),
            (1, "emerald_ziwei_seeds", "yishu:ziwei_seeds", 4, "minecraft:emerald", 1, 14.0, 0.05),
            (2, "emerald_ziwei_essence", "yishu:ziwei_essence", 2, "minecraft:emerald", 8, 6.0, 0.10),
            (2, "emerald_ziwei_key", "yishu:ziwei_key", 1, "minecraft:emerald", 24, 2.0, 0.10),
            (3, "emerald_ziwei_star_dust", "yishu:ziwei_star_dust", 6, "minecraft:emerald", 5, 4.0, 0.10),
        ],
        # 贵人师 · 贵人台（六壬界第二工位）
        "guiren_master": [
            (1, "emerald_liuren_crystal", "yishu:liuren_crystal", 3, "minecraft:emerald", 2, 12.0, 0.05),
            (1, "emerald_liuren_seeds", "yishu:liuren_seeds", 4, "minecraft:emerald", 1, 14.0, 0.05),
            (2, "emerald_liuren_essence", "yishu:liuren_essence", 2, "minecraft:emerald", 8, 6.0, 0.10),
            (2, "emerald_liuren_key", "yishu:liuren_key", 1, "minecraft:emerald", 24, 2.0, 0.10),
            (3, "emerald_liuren_shell", "yishu:liuren_shell", 8, "minecraft:emerald", 5, 4.0, 0.10),
        ],
        # 八门师 · 八门台（奇门界第二工位）
        "bamen_master": [
            (1, "emerald_qimen_crystal", "yishu:qimen_crystal", 3, "minecraft:emerald", 2, 12.0, 0.05),
            (1, "emerald_qimen_seeds", "yishu:qimen_seeds", 4, "minecraft:emerald", 1, 14.0, 0.05),
            (2, "emerald_qimen_essence", "yishu:qimen_essence", 2, "minecraft:emerald", 8, 6.0, 0.10),
            (2, "emerald_qimen_key", "yishu:qimen_key", 1, "minecraft:emerald", 24, 2.0, 0.10),
            (3, "emerald_qimen_shard", "yishu:qimen_shard", 8, "minecraft:emerald", 5, 4.0, 0.10),
        ],
        # 局师 · 奇门局阵
        "array_master": [
            (1, "emerald_qimen_shard", "yishu:qimen_shard", 4, "minecraft:emerald", 2, 12.0, 0.05),
            (1, "emerald_shengmen_talisman", "yishu:shengmen_talisman", 1, "minecraft:emerald", 5, 8.0, 0.05),
            (2, "emerald_qimen_disc", "yishu:qimen_disc", 1, "minecraft:emerald", 20, 3.0, 0.10),
            (2, "emerald_kaimen_talisman", "yishu:kaimen_talisman", 1, "minecraft:emerald", 10, 4.0, 0.10),
            (3, "emerald_xiumen_talisman", "yishu:xiumen_talisman", 1, "minecraft:emerald", 14, 4.0, 0.10),
        ],
    }
    for prof, entries in sorted(jobs.items()):
        by_level = {}
        for level, name, give, gcount, want, wcount, uses, disc in entries:
            write_json(f"yishu/villager_trade/{prof}/{level}/{name}.json", {
                "gives": {"id": give, "count": gcount},
                "max_uses": uses,
                "reputation_discount": disc,
                "wants": {"id": want, "count": wcount},
            })
            by_level.setdefault(level, []).append(f"yishu:{prof}/{level}/{name}")
        for level, ids in sorted(by_level.items()):
            write_json(f"yishu/tags/villager_trade/{prof}/level_{level}.json", {
                "replace": False,
                "values": sorted(ids),
            })
            write_json(f"yishu/trade_set/{prof}/level_{level}.json", {
                "amount": 2.0,
                "random_sequence": f"yishu:trade_set/{prof}/level_{level}",
                "trades": f"#yishu:{prof}/level_{level}",
            })


def gen_key_recipes():
    """六把灵引的工作台配方（第 17 轮补齐）。

    每一把钥匙都给四种材料变体 —— 玩家手上有哪一种界域料，
    就能用哪一种把钥匙「铸」出来，不至于卡在某一粒矿石上。
    形状统一是十字：四方材料托中央一方阴阳石。

    阴阳石本身不在此列：它是易枢的硬通货，只从遗迹宝箱与灵兽身上出，
    不做成可无限合成的中间物。
    """
    print("== 六把灵引配方 ==")
    plans = {
        # 钥匙 id: [(配方后缀, 材料 id), ...]
        "astral_key": [
            ("", "yishu:astral_dust"),
            ("_meteor_iron", "yishu:meteor_iron"),
            ("_star_crystal", "yishu:star_crystal"),
            ("_star_sand", "yishu:star_sand"),
        ],
        "jade_key": [
            ("", "yishu:jade_grit"),
            ("_mica", "yishu:mica"),
            ("_vein_crystal", "yishu:vein_crystal"),
            ("_verdant_gem", "yishu:verdant_gem"),
        ],
        "yao_key": [
            ("", "yishu:yao_shard"),
            ("_ink_iron", "yishu:ink_iron"),
            ("_onyx", "yishu:onyx"),
            ("_oracle_bone", "yishu:oracle_bone"),
        ],
        # —— 第 16 轮加入的三个界域，此前只有掉落、没有配方 ——
        "ziwei_key": [
            ("", "yishu:ziwei_star_dust"),
            ("_crystal", "yishu:ziwei_crystal"),
            ("_essence", "yishu:ziwei_essence"),
            ("_grain", "yishu:ziwei_grain"),
        ],
        "liuren_key": [
            ("", "yishu:liuren_crystal"),
            ("_essence", "yishu:liuren_essence"),
            ("_shell", "yishu:liuren_shell"),
            ("_grain", "yishu:liuren_grain"),
        ],
        # ---- 第 19 轮：太乙界灵引 ----
        "taiyi_key": [
            ("", "yishu:taiyi_crystal"),
            ("_essence", "yishu:taiyi_essence"),
            ("_shard", "yishu:taiyi_shard"),
            ("_grain", "yishu:taiyi_grain"),
        ],
        "qimen_key": [
            ("", "yishu:qimen_crystal"),
            ("_essence", "yishu:qimen_essence"),
            ("_shard", "yishu:qimen_shard"),
            ("_grain", "yishu:qimen_grain"),
        ],
    }
    count = 0
    for key, variants in sorted(plans.items()):
        for suffix, material in variants:
            write_json(f"yishu/recipe/{key}{suffix}.json", {
                "type": "minecraft:crafting_shaped",
                "category": "misc",
                "key": {"D": material, "X": "yishu:yinyang_stone"},
                "pattern": [" D ", "DXD", " D "],
                "result": {"id": f"yishu:{key}"},
            })
            count += 1
    print(f"  {len(plans)} 把钥匙 × 4 种材料 = {count} 个配方")


def gen_oracle_recipes():
    """三张盘与三座坛的配方（第 15 轮）。

    盘是「九宫围一核」：八枚术料包住一件旧物，合起来就是一面盘。
    坛再拿盘去镶：八块界域砖托一面盘 —— 所以想自己开一间术馆，
    得先有盘，想有盘，得先有料。这条链是刻意做长的。
    """
    print("== 三术盘与术坛配方 ==")
    discs = {
        "ziwei_chart": ("yishu:ziwei_star_dust", "yishu:fate_slip"),
        "liuren_plate": ("yishu:liuren_shell", "yishu:turtle_shell"),
        "qimen_disc": ("yishu:qimen_shard", "yishu:yinyang_stone"),
    }
    for name, (shell, core) in sorted(discs.items()):
        write_json(f"yishu/recipe/{name}.json", {
            "type": "minecraft:crafting_shaped",
            "category": "misc",
            "key": {"S": shell, "C": core},
            "pattern": ["SSS", "SCS", "SSS"],
            "result": {"id": f"yishu:{name}"},
        })
    altars = {
        "ziwei_terrace": ("yishu:astral_bricks", "yishu:ziwei_chart"),
        "liuren_altar": ("yishu:jade_bricks", "yishu:liuren_plate"),
        "qimen_array": ("yishu:yao_bricks", "yishu:qimen_disc"),
    }
    for name, (shell, core) in sorted(altars.items()):
        write_json(f"yishu/recipe/{name}.json", {
            "type": "minecraft:crafting_shaped",
            "category": "misc",
            "key": {"S": shell, "C": core},
            "pattern": ["SSS", "SCS", "SSS"],
            "result": {"id": f"yishu:{name}"},
        })


# ==================================================================== 模板生成


def gen_village_templates():
    print("== 结构模板 NBT（3 村庄 × 3 变种）==")
    for name, kind, *_ in VILLAGE_STRUCTURES:
        for variant in range(3):
            write(f"yishu/structure/{name}/v{variant}.nbt",
                  build_village(kind, variant))


def gen_rift_templates():
    print("== 结构模板 NBT（3 裂隙 × 3 变种）==")
    for name, kind, *_ in RIFT_STRUCTURES:
        for variant in range(3):
            write(f"yishu/structure/{name}/v{variant}.nbt",
                  build_rift(kind, variant))


# ==================================================================== 主流程


if __name__ == "__main__":
    os.makedirs(DATA, exist_ok=True)
    gen_village_templates()
    gen_rift_templates()
    gen_trades()
    gen_villager_trades()
    gen_realm_jobs()
    gen_workstation_recipes()
    gen_key_recipes()
    gen_oracle_jobs()
    gen_oracle_recipes()
    gen_mineable_tags()
    gen_wood_tags()
    gen_poi_tags()
    print(f"\n完成，共写入 {len(_WRITTEN)} 个文件。")
