#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""第 21 轮数据补丁。

职责（生成器管不到的「手写数据」与「跨界补丁」都在这）：
1. 方块掉落补全：扫描 YsRegistry 注册的全部方块，凡没有 loot_table/blocks/<名>.json
   的（液体/门户除外）一律补「自身掉落」；三种老作物补种子+产物表。
2. 矿物原矿：25 种矿石战利品改掉原矿（精准采集掉方块本体，时运走 ore_drops），
   并写熔炉/高炉配方（原矿 → 成品材料）。
3. 主世界矿物入界：铁/金/钻石三套 configured + placed 特征，被所有界域群系引用。
4. 危险液体湖：老三界（星轨/风水/六爻）各写一座凶液湖特征并挂到泽/沙群系。
5. 老三界补海洋群系 + 维度群系源加海洋分区 + fate_isle 噪声路由接回真实气候。

新四界（紫微/六壬/奇门/太乙）由 gen_oracle_realms.py 负责，本脚本只管老三界。
"""
import json
import os
import re

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
DATA = os.path.join(ROOT, "src", "main", "resources", "data")


def write_json(relpath, obj):
    path = os.path.join(DATA, relpath)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(obj, f, indent=1, ensure_ascii=False)
        f.write("\n")


def read_json(relpath):
    with open(os.path.join(DATA, relpath), encoding="utf-8") as f:
        return json.load(f)


def loot_exists(name):
    return os.path.exists(os.path.join(DATA, "yishu", "loot_table", "blocks", name + ".json"))


# ==================================================================== 1. 方块掉落

# 不该有掉落的：液体（noLootTable）与门户（免破坏）
NO_DROP = {
    "astral_dew", "jade_ichor", "yao_ink",
    "ziwei_ambrosia", "liuren_water", "qimen_mercury", "taiyi_spring",
    # 第 22 轮：七界凶液液体方块（无物品形式，掉落表会引用不存在的物品）
    "astral_slag", "jade_acid", "yao_miasma",
    "ziwei_bane", "liuren_abyss", "qimen_hellfire", "taiyi_frost",
    "astral_gate", "jade_gate", "yao_gate",
    "ziwei_gate", "liuren_gate", "qimen_gate", "taiyi_gate",
}

# 三种老作物：种子 + 成熟产物（与 liuren_rice 同款）
CROPS = {
    "astral_wheat": ("astral_seeds", "astral_grain", 1),
    "jade_rice": ("jade_seeds", "jade_grain", 1),
    "yao_bean": ("yao_seeds", "yao_pod", 1),
}


def scan_registered_blocks():
    """从 YsRegistry.java 扫出所有注册过的方块 id。"""
    src_path = os.path.join(ROOT, "src", "main", "java", "com", "chenchen", "yishu",
                            "registry", "YsRegistry.java")
    with open(src_path, encoding="utf-8") as f:
        src = f.read()
    pattern = re.compile(
        r'\b(?:blockOnly|dimensionStone|dimensionLamp|oreBlock|woodLog|leaves'
        r'|sapling|flower|grass|block|gate|liquidBlock)\(\s*"([a-z0-9_]+)"')
    found = set(pattern.findall(src))
    # sapling("species") 注册的方块 id 是 species+"_sapling"，
    # 物种参数本身不是方块（第 22 轮修正：之前误写孤儿掉落表）
    sap = re.compile(r'\bsapling\(\s*"([a-z0-9_]+)"')
    for species in sap.findall(src):
        found.discard(species)
        found.add(species + "_sapling")
    return sorted(found)


def crop_loot(crop, seed, produce):
    return {
        "type": "minecraft:block",
        "pools": [
            {"rolls": 1.0, "entries": [{"type": "minecraft:item", "name": "yishu:" + seed}]},
            {"rolls": 1.0,
             "conditions": [{"condition": "minecraft:block_state_property",
                             "block": "yishu:" + crop,
                             "properties": {"age": "3"}}],
             "entries": [{"type": "minecraft:item", "name": "yishu:" + produce,
                          "functions": [
                              {"function": "minecraft:set_count", "count": {"min": 1, "max": 2}},
                              {"function": "minecraft:apply_bonus",
                               "enchantment": "minecraft:fortune",
                               "formula": "minecraft:binomial_with_bonus_count",
                               "parameters": {"extra": 2, "probability": 0.5714286}},
                          ]}]},
        ],
    }


def self_drop(block):
    return {
        "type": "minecraft:block",
        "pools": [{
            "rolls": 1.0,
            "entries": [{"type": "minecraft:item", "name": "yishu:" + block}],
            "conditions": [{"condition": "minecraft:survives_explosion"}],
        }],
    }


def gen_block_drops():
    print("== 方块掉落补全 ==")
    blocks = scan_registered_blocks()
    made = 0
    for b in blocks:
        if b in NO_DROP or loot_exists(b):
            continue
        if b in CROPS:
            seed, produce, _ = CROPS[b]
            write_json("yishu/loot_table/blocks/%s.json" % b, crop_loot(b, seed, produce))
        else:
            write_json("yishu/loot_table/blocks/%s.json" % b, self_drop(b))
        made += 1
    print("   扫描 %d 个注册方块，补写 %d 张掉落表" % (len(blocks), made))


# ==================================================================== 2. 矿物原矿

# (矿石方块, 原矿物品, 熔炼产物)
RAW_ORES = [
    ("astral_ore", "astral_raw", "astral_dust", "星辉原矿", "Raw Astral Ore"),
    ("jade_ore", "jade_raw", "jade_grit", "青玉原矿", "Raw Jade Ore"),
    ("yao_ore", "yao_raw", "yao_shard", "爻纹原矿", "Raw Hexagram Ore"),
    ("meteor_ore", "meteor_raw", "meteor_iron", "陨星原矿", "Raw Meteor Ore"),
    ("ink_iron_ore", "ink_iron_raw", "ink_iron", "玄铁原矿", "Raw Ink Iron"),
    ("mica_ore", "mica_raw", "mica", "云母原矿", "Raw Mica"),
    ("onyx_ore", "onyx_raw", "onyx", "墨玉原矿", "Raw Onyx"),
    ("star_sand_ore", "star_sand_raw", "star_sand", "星砂原矿", "Raw Star Sand"),
    ("vein_ore", "vein_raw", "vein_crystal", "灵脉原矿", "Raw Vein Ore"),
    ("verdant_ore", "verdant_raw", "verdant_gem", "翠玉原矿", "Raw Verdant Ore"),
    ("oracle_ore", "oracle_raw", "oracle_bone", "卜骨原矿", "Raw Oracle Bone"),
    ("crystal_ore", "crystal_raw", "star_crystal", "星晶原矿", "Raw Star Crystal"),
    ("ziwei_crystal_ore", "ziwei_crystal_raw", "ziwei_crystal", "紫微原矿", "Raw Zi Wei Crystal"),
    ("ren_ore", "ren_raw", "liuren_crystal", "壬水原矿", "Raw Ren Ore"),
    ("gui_ore", "gui_raw", "liuren_crystal", "贵人原矿", "Raw Gui Ore"),
    ("she_ore", "she_raw", "liuren_essence", "螣蛇原矿", "Raw Teng She Ore"),
    ("xuan_ore", "xuan_raw", "liuren_essence", "玄武原矿", "Raw Xuan Wu Ore"),
    ("men_ore", "men_raw", "qimen_crystal", "八门原矿", "Raw Ba Men Ore"),
    ("xing_ore", "xing_raw", "qimen_crystal", "九星原矿", "Raw Jiu Xing Ore"),
    ("dun_ore", "dun_raw", "qimen_essence", "遁甲原矿", "Raw Dun Jia Ore"),
    ("jia_ore", "jia_raw", "qimen_essence", "甲子原矿", "Raw Jia Zi Ore"),
    ("taiyi_ore", "taiyi_raw", "taiyi_crystal", "太乙原矿", "Raw Tai Yi Ore"),
    ("wenchang_ore", "wenchang_raw", "taiyi_essence", "文昌原矿", "Raw Wen Chang Ore"),
    ("shiji_ore", "shiji_raw", "taiyi_shard", "始击原矿", "Raw Shi Ji Ore"),
    ("zhuke_ore", "zhuke_raw", "taiyi_grain", "主客原矿", "Raw Zhu Ke Ore"),
]

RAW_NOTE_ZH = "沉甸甸的一块原矿，回炉炼一炼才知道成色。"
RAW_NOTE_EN = "A heavy raw chunk; smelt it to see what it is really made of."


def ore_loot(ore, raw):
    """精准采集掉方块本体，否则掉原矿（时运 ore_drops）。"""
    return {
        "type": "minecraft:block",
        "pools": [{
            "rolls": 1.0,
            "entries": [{
                "type": "minecraft:alternatives",
                "children": [
                    {
                        "type": "minecraft:item",
                        "name": "yishu:" + ore,
                        "conditions": [{
                            "condition": "minecraft:match_tool",
                            "predicate": {"predicates": {
                                "minecraft:enchantments": [{
                                    "enchantments": "minecraft:silk_touch",
                                    "levels": {"min": 1},
                                }],
                            }},
                        }],
                    },
                    {
                        "type": "minecraft:item",
                        "name": "yishu:" + raw,
                        "functions": [
                            {"function": "minecraft:apply_bonus",
                             "enchantment": "minecraft:fortune",
                             "formula": "minecraft:ore_drops"},
                            {"function": "minecraft:explosion_decay"},
                        ],
                    },
                ],
            }],
        }],
    }


def cooking(kind, raw, material):
    return {
        "type": "minecraft:" + kind,
        "category": "misc",
        "ingredient": "yishu:" + raw,
        "result": {"id": "yishu:" + material, "count": 1},
        "experience": 0.7,
        "cookingtime": 200 if kind == "smelting" else 100,
    }


def gen_raw_ores():
    print("== 矿物原矿（战利品 + 熔炼/高炉）==")
    for ore, raw, material, _zh, _en in RAW_ORES:
        write_json("yishu/loot_table/blocks/%s.json" % ore, ore_loot(ore, raw))
        write_json("yishu/recipe/%s_smelting.json" % raw, cooking("smelting", raw, material))
        write_json("yishu/recipe/%s_blasting.json" % raw, cooking("blasting", raw, material))
    print("   %d 种矿：战利品改掉原矿，熔炉/高炉配方各一份" % len(RAW_ORES))


# ==================================================================== 3. 主世界矿物入界

OVERWORLD_ORES = {
    "iron": {"ore": "minecraft:iron_ore", "size": 12, "rarity": 2, "count": 4,
             "ymin": 0, "ymax": 200},
    "gold": {"ore": "minecraft:gold_ore", "size": 9, "rarity": 4, "count": 3,
             "ymin": 0, "ymax": 140},
    "diamond": {"ore": "minecraft:diamond_ore", "size": 6, "rarity": 6, "count": 2,
                "ymin": 0, "ymax": 60},
}
OVERWORLD_ORE_IDS = ["yishu:overworld_%s_ore" % k for k in OVERWORLD_ORES]


def gen_overworld_ore_features():
    print("== 主世界矿物入界特征（铁/金/钻石）==")
    for key, cfg in OVERWORLD_ORES.items():
        state = {"Name": cfg["ore"]}
        write_json("yishu/worldgen/configured_feature/overworld_%s_ore.json" % key, {
            "type": "minecraft:ore",
            "config": {
                "discard_chance_on_air_exposure": 0.0,
                "size": cfg["size"],
                "targets": [
                    {"state": state, "target": {"predicate_type": "minecraft:tag_match",
                                                "tag": "minecraft:stone_ore_replaceables"}},
                    {"state": state, "target": {"predicate_type": "minecraft:tag_match",
                                                "tag": "minecraft:deepslate_ore_replaceables"}},
                ],
            },
        })
        write_json("yishu/worldgen/placed_feature/overworld_%s_ore.json" % key, {
            "feature": "yishu:overworld_%s_ore" % key,
            "placement": [
                {"type": "minecraft:rarity_filter", "chance": cfg["rarity"]},
                {"type": "minecraft:count", "count": cfg["count"]},
                {"type": "minecraft:in_square"},
                {"type": "minecraft:height_range", "height": {
                    "type": "minecraft:trapezoid",
                    "min_inclusive": {"above_bottom": cfg["ymin"]},
                    "max_inclusive": {"above_bottom": cfg["ymax"]},
                }},
                {"type": "minecraft:biome"},
            ],
        })
    print("   3 套特征写好（所有界域群系统一引用）")


# ==================================================================== 4. 老三界：凶液湖 / 海洋群系 / 路由

# 老三界凶液（方块与流体在 YsFluids/YsRegistry 注册）
OLD_BANE = {
    "astral_slag": ("yishu:astral_stone", "astral_marsh", "astral_dunes"),
    "jade_acid": ("yishu:jade_stone", "jade_shore", "wind_plateau"),
    "yao_miasma": ("yishu:yao_stone", "ink_marsh", "twilight_plain"),
}

# (维度文件, 海洋群系, 克隆源群系)
OLD_REALMS = [
    ("fate_realm", "astral_ocean", "astral_field"),
    ("fengshui_realm", "jade_ocean", "vein_valley"),
    ("liuyao_realm", "ink_ocean", "hexagram_plain"),
]

OCEAN_ZONE = {"temperature": [-0.3, 0.4], "humidity": [0.0, 0.6],
              "continentalness": [-2.0, 2.0], "erosion": [-2.0, 2.0],
              "depth": [-2.0, 2.0], "weirdness": [-2.0, 2.0], "offset": 0}

CLIMATE_ROUTER = {
    "temperature": {"type": "minecraft:shifted_noise", "noise": "minecraft:temperature",
                    "shift_x": "minecraft:shift_x", "shift_y": 0.0,
                    "shift_z": "minecraft:shift_z", "xz_scale": 0.25, "y_scale": 0.0},
    "vegetation": {"type": "minecraft:shifted_noise", "noise": "minecraft:vegetation",
                   "shift_x": "minecraft:shift_x", "shift_y": 0.0,
                   "shift_z": "minecraft:shift_z", "xz_scale": 0.25, "y_scale": 0.0},
    "continents": "minecraft:overworld/continents",
    "erosion": "minecraft:overworld/erosion",
    "depth": "minecraft:overworld/depth",
    "ridges": "minecraft:overworld/ridges",
}


def gen_old_bane_lakes():
    print("== 老三界凶液湖 ==")
    for fluid, (stone, marsh, dunes) in OLD_BANE.items():
        cfg = read_json("yishu/worldgen/configured_feature/astral_dew_lake.json")
        cfg["config"]["barrier"]["state"]["Name"] = stone
        cfg["config"]["fluid"]["state"]["Name"] = "yishu:" + fluid
        write_json("yishu/worldgen/configured_feature/%s_lake.json" % fluid, cfg)
        placed = read_json("yishu/worldgen/placed_feature/astral_dew_lake.json")
        placed["feature"] = "yishu:%s_lake" % fluid
        write_json("yishu/worldgen/placed_feature/%s_lake.json" % fluid, placed)
        for biome in (marsh, dunes):
            d = read_json("yishu/worldgen/biome/%s.json" % biome)
            if "yishu:%s_lake" % fluid not in d["features"][1]:
                d["features"][1].append("yishu:%s_lake" % fluid)
            write_json("yishu/worldgen/biome/%s.json" % biome, d)
        print("   %s：湖特征 + %s / %s" % (fluid, marsh, dunes))


def patch_old_biomes():
    print("== 老三界群系补丁（主世界矿脉）==")
    patched = 0
    for biome in sorted(ALL_OLD_BIOMES):
        d = read_json("yishu/worldgen/biome/%s.json" % biome)
        changed = False
        ores = d["features"][6]
        for oid in OVERWORLD_ORE_IDS:
            if oid not in ores:
                ores.append(oid)
                changed = True
        if changed:
            write_json("yishu/worldgen/biome/%s.json" % biome, d)
            patched += 1
    print("   %d 个群系补入铁/金/钻石矿脉" % patched)


def gen_old_oceans():
    print("== 老三界海洋群系 ==")
    for dim, ocean, source in OLD_REALMS:
        d = read_json("yishu/worldgen/biome/%s.json" % source)
        d["features"][9] = []                       # 海里不长树
        d["temperature"] = 0.5
        ores = d["features"][6]
        for oid in OVERWORLD_ORE_IDS:
            if oid not in ores:
                ores.append(oid)
        write_json("yishu/worldgen/biome/%s.json" % ocean, d)
        # 维度群系源：缩小 field 的湿度带、给海洋腾出 [0.0, 0.6]
        dim_d = read_json("yishu/dimension/%s.json" % dim)
        entries = dim_d["generator"]["biome_source"]["biomes"]
        for e in entries:
            p = e["parameters"]
            if p.get("temperature") == [-0.3, 0.4] and p.get("humidity") == [-0.7, 0.6]:
                p["humidity"] = [-0.7, 0.0]
        if not any(e["biome"] == "yishu:" + ocean for e in entries):
            entries.append({"biome": "yishu:" + ocean, "parameters": dict(OCEAN_ZONE)})
        write_json("yishu/dimension/%s.json" % dim, dim_d)
        print("   %s（克隆自 %s）+ %s 群系源更新" % (ocean, source, dim))


def fix_fate_isle_router():
    print("== fate_isle 噪声路由接回真实气候 ==")
    d = read_json("yishu/worldgen/noise_settings/fate_isle.json")
    d["noise_router"].update(CLIMATE_ROUTER)
    write_json("yishu/worldgen/noise_settings/fate_isle.json", d)
    print("   temperature/vegetation/continents/erosion/depth/ridges 已接线")


# 老三界全部群系（给主世界矿脉补丁用）
ALL_OLD_BIOMES = [
    "astral_field", "astral_grove", "astral_peaks", "astral_marsh", "astral_dunes",
    "vein_valley", "cloud_forest", "jade_ridge", "jade_shore", "wind_plateau",
    "hexagram_plain", "oracle_woods", "stone_steppe", "ink_marsh", "twilight_plain",
]
OLD_BANE_LAKE_BIOMES = [b for _f, _s, _d in OLD_BANE.values() for b in (_s, _d)]


WOOD_SPECIES = [
    "astral_wood", "meteor_wood", "moon_wood", "jade_wood", "cloud_wood",
    "peach_wood", "hexa_wood", "ink_wood", "mystery_wood", "ziwei_wood",
    "dou_wood", "tianji_wood", "ren_wood", "gui_wood", "she_wood",
    "men_wood", "xing_wood", "taiyi_wood", "wenchang_wood", "jishen_wood",
    "dun_wood",
]

SILK_OR_SHEARS = [
    {
        "condition": "minecraft:any_of",
        "terms": [
            {"condition": "minecraft:match_tool", "predicate": {"items": "minecraft:shears"}},
            {"condition": "minecraft:match_tool", "predicate": {"predicates": {
                "minecraft:enchantments": [
                    {"enchantments": "minecraft:silk_touch", "levels": {"min": 1}}],
            }}},
        ],
    },
]


def gen_leaf_loots():
    """第 22 轮：原版式树叶掉落 —— 丝绸之触/剪刀掉树叶本体，
    否则按幸运附魔概率掉树苗（与原版 oak_leaves 同款概率曲线）。"""
    print("== 树叶掉落表（原版式，21 种）==")
    for species in WOOD_SPECIES:
        leaves = "yishu:%s_leaves" % species
        sapling = "yishu:%s_sapling" % species
        write_json("yishu/loot_table/blocks/%s_leaves.json" % species, {
            "type": "minecraft:block",
            "random_sequence": "yishu:blocks/%s_leaves" % species,
            "pools": [
                {
                    "rolls": 1.0,
                    "entries": [{
                        "type": "minecraft:alternatives",
                        "children": [
                            {
                                "type": "minecraft:item",
                                "name": leaves,
                                "conditions": SILK_OR_SHEARS,
                            },
                            {
                                "type": "minecraft:item",
                                "name": sapling,
                                "conditions": [
                                    {"condition": "minecraft:survives_explosion"},
                                    {"condition": "minecraft:table_bonus",
                                     "enchantment": "minecraft:fortune",
                                     "chances": [0.05, 0.0625, 0.083333336, 0.1]},
                                ],
                            },
                        ],
                    }],
                },
            ],
        })
    print("   21 张树叶表写好（剪刀/丝绸之触掉本体，否则幸运加成掉树苗）")


def gen_wood_loots():
    """第 22 轮：木板/台阶/楼梯自掉落。"""
    print("== 木三件套掉落表（21 × 3）==")
    for species in WOOD_SPECIES:
        for name in ("%s_planks" % species, "%s_slab" % species,
                     "%s_stairs" % species):
            write_json("yishu/loot_table/blocks/%s.json" % name, self_drop(name))
    print("   63 张自掉落表写好")


def gen_wood_recipes():
    """第 22 轮：原木→木板→台阶/楼梯 合成链。"""
    print("== 木三件套配方（21 × 3）==")
    for species in WOOD_SPECIES:
        log = "yishu:%s_log" % species
        planks = "yishu:%s_planks" % species
        slab = "yishu:%s_slab" % species
        stairs = "yishu:%s_stairs" % species
        write_json("yishu/recipe/%s_planks.json" % species, {
            "type": "minecraft:crafting_shapeless",
            "category": "building",
            "group": "planks",
            "ingredients": [log],
            "result": {"count": 4, "id": planks},
        })
        write_json("yishu/recipe/%s_slab.json" % species, {
            "type": "minecraft:crafting_shaped",
            "category": "building",
            "group": "wooden_slab",
            "pattern": ["###"],
            "key": {"#": planks},
            "result": {"count": 6, "id": slab},
        })
        write_json("yishu/recipe/%s_stairs.json" % species, {
            "type": "minecraft:crafting_shaped",
            "category": "building",
            "group": "wooden_stairs",
            "pattern": ["#  ", "## ", "###"],
            "key": {"#": planks},
            "result": {"count": 4, "id": stairs},
        })
    print("   63 份配方写好")


def patch_wood_mineable():
    """第 22 轮：木三件套进 mineable/axe 标签（不进标签挖不掉）。"""
    rel = "minecraft/tags/block/mineable/axe.json"
    try:
        tag = read_json(rel)
    except FileNotFoundError:
        tag = {"replace": False, "values": []}
    values = tag.setdefault("values", [])
    added = 0
    for species in WOOD_SPECIES:
        for name in ("%s_planks" % species, "%s_slab" % species,
                     "%s_stairs" % species):
            full = "yishu:" + name
            if full not in values:
                values.append(full)
                added += 1
    write_json(rel, tag)
    print("== mineable/axe +%d 木方块 ==" % added)


def main():
    gen_block_drops()
    gen_raw_ores()
    gen_overworld_ore_features()
    gen_old_bane_lakes()
    patch_old_biomes()
    gen_old_oceans()
    fix_fate_isle_router()
    gen_leaf_loots()
    gen_wood_loots()
    gen_wood_recipes()
    patch_wood_mineable()
    print("第 21/22 轮数据补丁完成")


if __name__ == "__main__":
    main()
