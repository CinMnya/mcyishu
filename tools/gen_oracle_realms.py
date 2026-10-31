#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""易枢 · 三术界维度生成器（第 16 轮）

为紫微斗数 / 大六壬 / 奇门遁甲各造一个完整维度，规格与已有的
命理界 / 风水界 / 六爻界完全对齐：

  · 1 份 dimension_type（独立天穹、雾色、云色、星光、环境粒子）
  · 1 份 dimension（multi_noise 五分区生物群系）
  · 1 份 noise_settings（克隆原版噪声，default_fluid 换成该界液体）
  · 5 个生物群系（各带自己的颜色、生成物、刷怪表）
  · 3 种树（原木 / 树叶 / 树苗 + configured/placed feature）
  · 4 种矿石（configured/placed feature）
  · 1 汪界域液体湖 + 花与草的地物
  · 1 座村庄（模板由 generate_vanilla_data.build_village 生成）
  · 1 座专属神殿（克隆既有神殿模板并换调色板与守护灵兽）
  · 1 条主世界裂隙（入侵感）
  · 1 张村庄箱子战利品表

幂等：可反复运行，只覆盖自己生成的文件。
"""

import json
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)

from generate_vanilla_data import (  # noqa: E402
    DATA, Palette, build_template, write, write_json,
)
from validate_templates import load_nbt  # noqa: E402

STRUCT = os.path.join(DATA, "yishu", "structure")

# ==================================================================== 界域定义

REALMS = [
    dict(
        id="ziwei_realm",
        zh="紫微界",
        prefix="ziwei",
        kit=("ziwei_stone", "ziwei_bricks", "ziwei_lamp"),
        core="ruin_core",
        fluid="ziwei_ambrosia",
        noise="ziwei_sky",
        base_noise="floating_islands",
        sea_level=-64,
        biomes=["ziwei_field", "ziwei_grove", "ziwei_peaks", "ziwei_marsh", "ziwei_dunes"],
        biome_zh=["紫微原", "斗垣林", "北斗崖", "紫微泽", "天机沙"],
        trees=[("ziwei_wood", "oak"), ("dou_wood", "tall"), ("tianji_wood", "fancy")],
        ores=["ziwei_crystal_ore", "dou_ore", "ji_ore", "yuan_ore"],
        ores_per_biome={
            "ziwei_field": ["ziwei_crystal_ore", "dou_ore"],
            "ziwei_grove": ["ji_ore", "ziwei_crystal_ore"],
            "ziwei_peaks": ["yuan_ore", "dou_ore", "ziwei_crystal_ore"],
            "ziwei_marsh": ["ji_ore"],
            "ziwei_dunes": ["ji_ore", "yuan_ore"],
        },
        flower="ziwei_bloom",
        grass="ziwei_grass",
        mobs=["ziwei_star", "dou_beast", "tianji_fox", "ziwei_moth"],
        village="ziwei_village",
        temple="ziwei_observatory",
        temple_src="astral_observatory",
        temple_zh="紫微观星台",
        rift="ziwei_rift",
        chest="ziwei_terrace",
        sky=dict(
            ambient_light=0.15,
            sky="#1b0f36", fog="#2e1f52", cloud="#ccb183ff",
            star=0.95, sky_light="#b18bff", sky_light_factor=0.90,
            sunrise="#66e8c88a", water_fog="#2a1a52",
            fog_start=16.0, fog_end=190.0, sky_fog_end=230.0, cloud_h=200.0,
            ambient_light_color="#0d0818",
            particle=("minecraft:end_rod", 0.022),
        ),
        effect=dict(water_color="#3a2470", grass_color="#a98fe8", foliage_color="#8f74d6"),
        grasses=["#7fe0ff"],  # 不用，占位
        mob_profile={
            "creature": [("tianji_fox", 9, 2, 3), ("dou_beast", 4, 1, 2)],
            "monster": [("ziwei_star", 6, 1, 2), ("ziwei_moth", 10, 2, 4)],
        },
    ),
    dict(
        id="liuren_realm",
        zh="六壬界",
        prefix="liuren",
        kit=("liuren_stone", "liuren_bricks", "liuren_lamp"),
        core="ruin_core",
        fluid="liuren_water",
        noise="liuren_sky",
        base_noise="amplified",
        sea_level=-64,
        biomes=["liuren_field", "liuren_grove", "liuren_peaks", "liuren_marsh", "liuren_dunes"],
        biome_zh=["壬水原", "贵人林", "螣蛇崖", "玄武泽", "勾陈沙"],
        trees=[("ren_wood", "oak"), ("gui_wood", "tall"), ("she_wood", "fancy")],
        ores=["ren_ore", "gui_ore", "she_ore", "xuan_ore"],
        ores_per_biome={
            "liuren_field": ["ren_ore", "gui_ore"],
            "liuren_grove": ["gui_ore", "she_ore"],
            "liuren_peaks": ["xuan_ore", "she_ore", "ren_ore"],
            "liuren_marsh": ["ren_ore"],
            "liuren_dunes": ["xuan_ore", "gui_ore"],
        },
        flower="liuren_lily",
        grass="liuren_grass",
        mobs=["ren_serpent", "gui_crane", "she_hound", "xuan_turtle"],
        village="liuren_village",
        temple="liuren_hall",
        temple_src="jade_pagoda",
        temple_zh="六壬课坛",
        rift="liuren_rift",
        chest="liuren_altar",
        sky=dict(
            ambient_light=0.14,
            sky="#0c2b34", fog="#16454f", cloud="#cc8fd4d0",
            star=0.50, sky_light="#7fe0d8", sky_light_factor=0.80,
            sunrise="#55c8b48a", water_fog="#123c45",
            fog_start=20.0, fog_end=175.0, sky_fog_end=215.0, cloud_h=190.0,
            ambient_light_color="#06171c",
            particle=("minecraft:bubble", 0.018),
        ),
        effect=dict(water_color="#1a6a74", grass_color="#5fc9a8", foliage_color="#4fb894"),
        grasses=["#5fe0c0"],
        mob_profile={
            "creature": [("gui_crane", 8, 2, 3), ("xuan_turtle", 3, 1, 2)],
            "monster": [("she_hound", 7, 1, 3), ("ren_serpent", 5, 1, 1)],
        },
    ),
    dict(
        id="qimen_realm",
        zh="奇门界",
        prefix="qimen",
        kit=("qimen_stone", "qimen_bricks", "qimen_lamp"),
        core="ruin_core",
        fluid="qimen_mercury",
        noise="qimen_sky",
        base_noise="overworld",
        sea_level=-64,
        biomes=["qimen_field", "qimen_grove", "qimen_peaks", "qimen_marsh", "qimen_dunes"],
        biome_zh=["八门原", "九星林", "天柱峰", "景门泽", "遁甲沙"],
        trees=[("men_wood", "oak"), ("xing_wood", "tall"), ("dun_wood", "fancy")],
        ores=["men_ore", "xing_ore", "dun_ore", "jia_ore"],
        ores_per_biome={
            "qimen_field": ["men_ore", "xing_ore"],
            "qimen_grove": ["dun_ore", "men_ore"],
            "qimen_peaks": ["jia_ore", "xing_ore", "men_ore"],
            "qimen_marsh": ["dun_ore"],
            "qimen_dunes": ["jia_ore", "dun_ore"],
        },
        flower="qimen_bloom",
        grass="qimen_grass",
        mobs=["men_guard", "xing_fox", "dun_rat", "jia_bird"],
        village="qimen_village",
        temple="qimen_array_tower",
        temple_src="yao_altar",
        temple_zh="奇门局塔",
        rift="qimen_rift",
        chest="qimen_array",
        sky=dict(
            ambient_light=0.13,
            sky="#2a1206", fog="#5c2a10", cloud="#cccc5a2a",
            star=0.35, sky_light="#ffb46b", sky_light_factor=0.78,
            sunrise="#66ff9b42", water_fog="#4a1c0c",
            fog_start=14.0, fog_end=160.0, sky_fog_end=200.0, cloud_h=180.0,
            ambient_light_color="#170a04",
            particle=("minecraft:flame", 0.016),
        ),
        effect=dict(water_color="#b04a1a", grass_color="#d6a257", foliage_color="#c08a3e"),
        grasses=["#ffb46b"],
        mob_profile={
            "creature": [("xing_fox", 7, 1, 3), ("dun_rat", 9, 2, 4)],
            "monster": [("men_guard", 3, 1, 1), ("jia_bird", 10, 2, 3)],
        },
    ),
]

# 群系五分区（与既有三界一致的铺满方案）
ZONE_PARAMS = [
    # (温度区间, 湿度区间)
    ([-0.3, 0.4], [-0.7, 0.6]),    # 核心
    ([-0.3, 0.4], [0.6, 2.0]),     # 湿润
    ([0.4, 2.0], [-0.7, 2.0]),     # 炎热
    ([-2.0, -0.3], [-0.7, 2.0]),   # 寒冷
    ([-2.0, 2.0], [-2.0, -0.7]),   # 干燥
]

FULL = [-2.0, 2.0]

# 树形态参数：名字 -> (trunk 基础高, 随机高, 树冠半径, 树冠高度)
TREE_SHAPES = {
    "oak": (4, 2, 2, 3),
    "tall": (7, 3, 2, 4),
    "fancy": (3, 1, 3, 2),
}


# ==================================================================== 维度类型

def dimension_type(realm):
    s = realm["sky"]
    part = s["particle"]
    return {
        "ambient_light": s["ambient_light"],
        "attributes": {
            "minecraft:audio/ambient_sounds": {
                "mood": {
                    "block_search_extent": 8,
                    "offset": 2.0,
                    "sound": "minecraft:ambient.cave",
                    "tick_delay": 6000,
                }
            },
            "minecraft:audio/background_music": {
                "default": {
                    "max_delay": 24000,
                    "min_delay": 12000,
                    "sound": "minecraft:music.game",
                }
            },
            "minecraft:gameplay/bed_rule": {
                "can_set_spawn": "always",
                "can_sleep": "when_dark",
                "error_message": {"translate": "block.minecraft.bed.no_sleep"},
            },
            "minecraft:gameplay/nether_portal_spawns_piglin": False,
            "minecraft:gameplay/respawn_anchor_works": False,
            "minecraft:gameplay/water_evaporates": True,
            "minecraft:visual/ambient_light_color": s["ambient_light_color"],
            "minecraft:visual/ambient_particles": [
                {"particle": {"type": part[0]}, "probability": part[1]}
            ],
            "minecraft:visual/cloud_color": s["cloud"],
            "minecraft:visual/cloud_height": s["cloud_h"],
            "minecraft:visual/fog_color": s["fog"],
            "minecraft:visual/fog_end_distance": s["fog_end"],
            "minecraft:visual/fog_start_distance": s["fog_start"],
            "minecraft:visual/sky_color": s["sky"],
            "minecraft:visual/sky_fog_end_distance": s["sky_fog_end"],
            "minecraft:visual/sky_light_color": s["sky_light"],
            "minecraft:visual/sky_light_factor": s["sky_light_factor"],
            "minecraft:visual/star_brightness": s["star"],
            "minecraft:visual/sunrise_sunset_color": s["sunrise"],
            "minecraft:visual/water_fog_color": s["water_fog"],
        },
        "coordinate_scale": 1.0,
        "default_clock": "minecraft:overworld",
        "has_ceiling": False,
        "has_ender_dragon_fight": False,
        "has_skylight": True,
        "height": 384,
        "infiniburn": "#minecraft:infiniburn_overworld",
        "logical_height": 384,
        "min_y": -64,
        "monster_spawn_block_light_limit": 0,
        "monster_spawn_light_level": {
            "type": "minecraft:uniform",
            "max_inclusive": 7,
            "min_inclusive": 0,
        },
        "timelines": "#minecraft:in_overworld",
    }


def gen_dimension_types():
    print("== 维度类型（3 份：独立天穹与雾色）==")
    for realm in REALMS:
        write_json("yishu/dimension_type/%s.json" % realm["id"], dimension_type(realm))


# ==================================================================== 维度主体

def gen_dimensions():
    print("== 维度（3 份：multi_noise 五分区）==")
    for realm in REALMS:
        biomes = []
        for name, (t0, t1), (h0, h1) in zip(realm["biomes"], *zip(*ZONE_PARAMS)):
            biomes.append({
                "biome": "yishu:" + name,
                "parameters": {
                    "temperature": [t0, t1],
                    "humidity": [h0, h1],
                    "continentalness": list(FULL),
                    "erosion": list(FULL),
                    "depth": list(FULL),
                    "weirdness": list(FULL),
                    "offset": 0,          # ← 单个 long，不是数组！
                },
            })
        write_json("yishu/dimension/%s.json" % realm["id"], {
            "type": "yishu:" + realm["id"],
            "generator": {
                "type": "minecraft:noise",
                "settings": "yishu:" + realm["noise"],
                "biome_source": {"type": "minecraft:multi_noise", "biomes": biomes},
            },
        })


# ==================================================================== 噪声设置

# 各界克隆的既有噪声设置 + 质感微调
# 说明：曾试过让六壬界克隆 amplified（jade_highland）并把 size 调到 (2,3)，
# 结果单区块生成超过 60 秒，被服务端看门狗强杀（"A single server tick took 60.00 seconds"）。
# 极端地形噪声 + 三维度连续生成 = 不可用。故三界统一走已验证的温和源，
# 靠 size_horizontal / size_vertical 制造地形质感差异。
NOISE_SRC = {
    "ziwei_realm": ("fate_isle", None),          # 浮空岛：独特地貌
    "liuren_realm": ("hex_expanse", (1, 2)),     # 常规山地：起伏最深
    "qimen_realm": ("hex_expanse", (2, 1)),      # 开阔平缓：大尺度台地
}


def gen_noise_settings():
    print("== 噪声设置（3 份：default_fluid 换成界域液体）==")
    for realm in REALMS:
        src, tweak = NOISE_SRC[realm["id"]]
        with open(os.path.join(DATA, "yishu", "worldgen", "noise_settings", src + ".json"),
                  encoding="utf-8") as f:
            d = json.load(f)
        d["default_fluid"] = {"Name": "yishu:" + realm["fluid"], "Properties": {"level": "0"}}
        d["sea_level"] = realm["sea_level"]
        if tweak:
            d["noise"]["size_horizontal"] = tweak[0]
            d["noise"]["size_vertical"] = tweak[1]
        write_json("yishu/worldgen/noise_settings/%s.json" % realm["noise"], d)


# ==================================================================== 树木

def gen_tree_features():
    print("== 树木特征（9 种树 × configured + placed）==")
    for realm in REALMS:
        for wood, shape in realm["trees"]:
            base, rand_a, radius, fh = TREE_SHAPES[shape]
            write_json("yishu/worldgen/configured_feature/%s_tree.json" % wood, {
                "type": "minecraft:tree",
                "config": {
                    "below_trunk_provider": {
                        "type": "minecraft:rule_based_state_provider",
                        "rules": [{
                            "if_true": {
                                "type": "minecraft:not",
                                "predicate": {
                                    "type": "minecraft:matching_block_tag",
                                    "tag": "minecraft:cannot_replace_below_tree_trunk",
                                },
                            },
                            "then": {
                                "type": "minecraft:simple_state_provider",
                                "state": {"Name": "minecraft:dirt"},
                            },
                        }],
                    },
                    "decorators": [],
                    "foliage_placer": {
                        "type": "minecraft:blob_foliage_placer",
                        "height": fh, "offset": 0, "radius": radius,
                    },
                    "foliage_provider": {
                        "type": "minecraft:simple_state_provider",
                        "state": {
                            "Name": "yishu:%s_leaves" % wood,
                            "Properties": {"distance": "7", "persistent": "false",
                                           "waterlogged": "false"},
                        },
                    },
                    "ignore_vines": True,
                    "minimum_size": {"type": "minecraft:two_layers_feature_size"},
                    "trunk_placer": {
                        "type": "minecraft:straight_trunk_placer",
                        "base_height": base, "height_rand_a": rand_a, "height_rand_b": 0,
                    },
                    "trunk_provider": {
                        "type": "minecraft:simple_state_provider",
                        "state": {"Name": "yishu:%s_log" % wood, "Properties": {"axis": "y"}},
                    },
                },
            })
            write_json("yishu/worldgen/placed_feature/%s_trees.json" % wood, {
                "feature": "yishu:%s_tree" % wood,
                "placement": [
                    {"type": "minecraft:count", "count": 6 if shape == "oak" else (3 if shape == "tall" else 8)},
                    {"type": "minecraft:in_square"},
                    {"type": "minecraft:surface_water_depth_filter", "max_water_depth": 0},
                    {"type": "minecraft:heightmap", "heightmap": "OCEAN_FLOOR"},
                    {"type": "minecraft:block_predicate_filter", "predicate": {
                        "type": "minecraft:would_survive",
                        "state": {"Name": "yishu:%s_sapling" % wood, "Properties": {"stage": "0"}},
                    }},
                    {"type": "minecraft:biome"},
                ],
            })


# ==================================================================== 矿石

# 每种矿的 (每区块矿脉数, 最低高度, 最高高度, 矿脉大小)
ORE_SHAPE = {
    0: (5, -60, 60, 6),    # 主矿：多而浅
    1: (3, -60, 20, 4),    # 次矿：中层
    2: (4, -20, 110, 5),   # 地表矿
    3: (2, -60, -10, 3),   # 深矿：稀少
}


def gen_ore_features():
    print("== 矿石特征（12 种矿 × configured + placed）==")
    for realm in REALMS:
        for i, ore in enumerate(realm["ores"]):
            count, ymin, ymax, size = ORE_SHAPE[i]
            state = {"Name": "yishu:" + ore}
            write_json("yishu/worldgen/configured_feature/%s.json" % ore, {
                "type": "minecraft:ore",
                "config": {
                    "discard_chance_on_air_exposure": 0.3,
                    "size": size,
                    "targets": [
                        {"state": state, "target": {"predicate_type": "minecraft:tag_match",
                                                    "tag": "minecraft:stone_ore_replaceables"}},
                        {"state": state, "target": {"predicate_type": "minecraft:tag_match",
                                                    "tag": "minecraft:deepslate_ore_replaceables"}},
                    ],
                },
            })
            write_json("yishu/worldgen/placed_feature/%s.json" % ore, {
                "feature": "yishu:" + ore,
                "placement": [
                    {"type": "minecraft:count", "count": count},
                    {"type": "minecraft:in_square"},
                    {"type": "minecraft:height_range", "height": {
                        "type": "minecraft:trapezoid",
                        "min_inclusive": {"above_bottom": ymin},
                        "max_inclusive": {"above_bottom": ymax},
                    }},
                    {"type": "minecraft:biome"},
                ],
            })


# ==================================================================== 液体湖 / 花 / 草

def gen_fluid_and_flora():
    print("== 液体湖 · 花 · 草（3 界各一套）==")
    for realm in REALMS:
        stone = "yishu:" + realm["kit"][0]
        fluid = "yishu:" + realm["fluid"]

        write_json("yishu/worldgen/configured_feature/%s_lake.json" % realm["fluid"], {
            "type": "minecraft:lake",
            "config": {
                "barrier": {"type": "minecraft:simple_state_provider", "state": {"Name": stone}},
                "can_place_feature": {"type": "minecraft:true"},
                "can_replace_with_air_or_fluid": {
                    "type": "minecraft:not",
                    "predicate": {"type": "minecraft:matching_block_tag",
                                  "tag": "minecraft:features_cannot_replace"},
                },
                "can_replace_with_barrier": {
                    "type": "minecraft:not",
                    "predicate": {"type": "minecraft:matching_block_tag",
                                  "tag": "minecraft:lava_pool_stone_cannot_replace"},
                },
                "fluid": {"type": "minecraft:simple_state_provider",
                          "state": {"Name": fluid, "Properties": {"level": "0"}}},
                "barrier_disk_radius": 2,
                "barrier_ring_radius": 5,
                "border": {"type": "minecraft:simple_state_provider", "state": {"Name": stone}},
            },
        })
        write_json("yishu/worldgen/placed_feature/%s_lake.json" % realm["fluid"], {
            "feature": "yishu:%s_lake" % realm["fluid"],
            "placement": [
                {"type": "minecraft:count", "count": 1},
                {"type": "minecraft:in_square"},
                {"type": "minecraft:height_range", "height": {
                    "type": "minecraft:trapezoid",
                    "min_inclusive": {"above_bottom": 20},
                    "max_inclusive": {"above_bottom": 90},
                }},
                {"type": "minecraft:biome"},
            ],
        })

        for kind, block, count in (("bloom", realm["flower"], 8), ("grass", realm["grass"], 14)):
            write_json("yishu/worldgen/configured_feature/%s.json" % block, {
                "type": "minecraft:simple_block",
                "config": {"to_place": {"type": "minecraft:simple_state_provider",
                                        "state": {"Name": "yishu:" + block}}},
            })
            write_json("yishu/worldgen/placed_feature/%s_patch.json" % block, {
                "feature": "yishu:" + block,
                "placement": [
                    {"type": "minecraft:count", "count": count},
                    {"type": "minecraft:in_square"},
                    {"type": "minecraft:heightmap", "heightmap": "MOTION_BLOCKING"},
                    {"type": "minecraft:block_predicate_filter", "predicate": {
                        "type": "minecraft:matching_block_tag", "tag": "minecraft:air"}},
                    {"type": "minecraft:biome"},
                ],
            })


# ==================================================================== 生物群系

def gen_biomes():
    print("== 生物群系（15 个）==")
    for realm in REALMS:
        # ⚠️ FeatureSorter 会在「同一维度的全部群系」之间求特征全序：
        # 只要两个群系把同一对特征的先后排反了，就会抛
        # 「Feature order cycle found」并让区块生成彻底卡死。
        # 所以每个 step 内部一律用「该界域的全序」筛选，缺的跳过 —— 顺序恒定。
        all_ores = ["yishu:" + o for o in realm["ores"]]
        all_trees = ["yishu:%s_trees" % w for w, _ in realm["trees"]]
        for name in realm["biomes"]:
            wanted_ores = {"yishu:" + o for o in realm["ores_per_biome"][name]}
            ores = [o for o in all_ores if o in wanted_ores]
            steps = [[] for _ in range(11)]
            steps[1] = ["yishu:%s_lake" % realm["fluid"]]
            steps[2] = ["minecraft:amethyst_geode"]
            steps[6] = ores
            steps[9] = ["minecraft:glow_lichen",
                        "yishu:%s_patch" % realm["flower"],
                        "yishu:%s_patch" % realm["grass"]] + list(all_trees)
            spawners = {
                "ambient": [], "axolotls": [], "misc": [],
                "underground_water_creature": [], "water_ambient": [], "water_creature": [],
                "creature": [{"type": "yishu:" + m, "weight": w,
                              "minCount": a, "maxCount": b}
                             for m, w, a, b in realm["mob_profile"]["creature"]],
                "monster": [{"type": "yishu:" + m, "weight": w,
                             "minCount": a, "maxCount": b}
                            for m, w, a, b in realm["mob_profile"]["monster"]],
            }
            write_json("yishu/worldgen/biome/%s.json" % name, {
                "attributes": {
                    "minecraft:visual/sky_color": realm["sky"]["sky"],
                    "minecraft:visual/fog_color": realm["sky"]["fog"],
                    "minecraft:visual/water_fog_color": realm["sky"]["water_fog"],
                    "minecraft:audio/background_music": {"default": {
                        "sound": "minecraft:music.game", "min_delay": 12000, "max_delay": 24000}},
                    "minecraft:visual/ambient_particles": [{
                        "particle": {"type": realm["sky"]["particle"][0]},
                        "probability": round(realm["sky"]["particle"][1] * 0.8, 4)}],
                },
                "carvers": [],
                "downfall": 0.4,
                "effects": dict(realm["effect"]),
                "features": steps,
                "has_precipitation": False,
                "spawn_costs": {},
                "spawners": spawners,
                "temperature": 0.7 if "peaks" in name else (0.1 if "marsh" in name else 0.4),
            })


# ==================================================================== 结构 JSON

def structure_json(pool, biome_tag, start_height):
    return {
        "type": "minecraft:jigsaw",
        "biomes": biome_tag,
        "step": "surface_structures",
        "spawn_overrides": {},
        "terrain_adaptation": "beard_thin",
        "start_pool": pool,
        "size": 1,
        "start_height": {"absolute": start_height},
        "project_start_to_heightmap": "WORLD_SURFACE_WG",
        "max_distance_from_center": 116,     # 硬上限：带地形适应时 ≤ 116
        "use_expansion_hack": False,
    }


def structure_set_json(structure, spacing, separation, salt):
    return {
        "structures": [{"structure": structure, "weight": 1}],
        "placement": {
            "type": "minecraft:random_spread",
            "spacing": spacing,
            "separation": separation,
            "salt": salt,
            "spread_type": "linear",
        },
    }


def start_pool_json(folder):
    return {
        "fallback": "minecraft:empty",
        "elements": [{
            "weight": 1,
            "element": {
                "element_type": "minecraft:single_pool_element",
                "location": "yishu:%s/v%d" % (folder, v),
                "processors": "minecraft:empty",
                "projection": "rigid",
            },
        } for v in range(3)],
    }


def gen_structure_json():
    print("== 结构 JSON（3 村庄 + 3 神殿 + 3 裂隙）==")
    salt = 20261101
    for realm in REALMS:
        tag = "#yishu:%s_field" % realm["prefix"]
        for key, height, spacing, sep in (
                (realm["village"], 0, 44, 15),
                (realm["temple"], -8, 48, 16),
                (realm["rift"], -1, 30, 9)):
            biome = "#minecraft:is_overworld" if key == realm["rift"] else tag
            write_json("yishu/worldgen/structure/%s.json" % key,
                       structure_json("yishu:%s/start" % key, biome, height))
            write_json("yishu/worldgen/structure_set/%s.json" % key,
                       structure_set_json("yishu:" + key, spacing, sep, salt))
            write_json("yishu/worldgen/template_pool/%s/start.json" % key,
                       start_pool_json(key))
            salt += 1


# ==================================================================== 神殿模板克隆

def clone_temple(realm):
    """克隆既有神殿模板，换掉调色板里的界域方块与守卫灵兽。"""
    src = realm["temple_src"]
    dst = realm["temple"]
    old_prefix = {"astral_observatory": "astral", "jade_pagoda": "jade",
                  "yao_altar": "yao"}[src]
    new_prefix = realm["prefix"]
    mobs = realm["mobs"]
    swap = {
        "yishu:stone_guardian": "yishu:" + mobs[0],
        "yishu:fate_wisp": "yishu:" + mobs[3],
        "yishu:jade_spirit": "yishu:" + mobs[1],
    }

    for v in range(3):
        path = os.path.join(STRUCT, src, "v%d.nbt" % v)
        root = load_nbt(path)
        w, h, d = root["size"]

        palette = []
        for entry in root["palette"]:
            name = entry["Name"]
            if name.startswith("yishu:" + old_prefix + "_"):
                name = "yishu:" + new_prefix + "_" + name[len("yishu:" + old_prefix + "_"):]
            palette.append((name, entry.get("Properties")))

        blocks = [(b["pos"][0], b["pos"][1], b["pos"][2], b["state"], b.get("nbt"))
                  for b in root["blocks"]]

        entities = []
        for e in root.get("entities", []):
            nbt = dict(e.get("nbt") or {})
            nbt["id"] = swap.get(nbt.get("id"), nbt.get("id"))
            entities.append((int(e["pos"][0]), int(e["pos"][1]), int(e["pos"][2]), nbt))

        write("yishu/structure/%s/v%d.nbt" % (dst, v),
              build_template(w, h, d, palette, blocks, entities))
    print("   + %s ← 克隆 %s（3 变种）" % (dst, src))


def gen_temple_templates():
    print("== 神殿模板（3 座，克隆 + 换色）==")
    for realm in REALMS:
        clone_temple(realm)


# ==================================================================== 战利品表

def gen_chests():
    """三界村庄箱子：本界特产 + 通用消耗品，保证「本界一切都能开出来」。"""
    print("== 村庄箱子战利品表（3 张）==")
    for realm in REALMS:
        p = realm["prefix"]
        entries = [
            # 矿石直接产出（材料）
            {"type": "minecraft:item", "name": "yishu:%s_crystal" % p,
             "weight": 10, "functions": [{"function": "minecraft:set_count",
                                          "count": {"min": 1, "max": 4}}]},
            {"type": "minecraft:item", "name": "yishu:%s_essence" % p,
             "weight": 8, "functions": [{"function": "minecraft:set_count",
                                         "count": {"min": 1, "max": 3}}]},
            {"type": "minecraft:item", "name": "yishu:%s_seeds" % p,
             "weight": 12, "functions": [{"function": "minecraft:set_count",
                                          "count": {"min": 2, "max": 5}}]},
            {"type": "minecraft:item", "name": "yishu:%s_key" % p, "weight": 3},
            {"type": "minecraft:item", "name": "yishu:%s_bucket" % realm["fluid"], "weight": 2},
            {"type": "minecraft:item", "name": "yishu:%s_stone" % p, "weight": 10,
             "functions": [{"function": "minecraft:set_count",
                            "count": {"min": 4, "max": 12}}]},
            {"type": "minecraft:item", "name": "yishu:%s_bricks" % p, "weight": 8,
             "functions": [{"function": "minecraft:set_count",
                            "count": {"min": 4, "max": 10}}]},
            {"type": "minecraft:item", "name": "yishu:%s_lamp" % p, "weight": 5},
        ]
        for wood, _ in realm["trees"]:
            entries.append({"type": "minecraft:item", "name": "yishu:%s_sapling" % wood, "weight": 6})
        # 通用：丹药 / 符纸 / 铜钱
        entries += [
            {"type": "minecraft:item", "name": "yishu:talisman_paper", "weight": 10,
             "functions": [{"function": "minecraft:set_count", "count": {"min": 1, "max": 4}}]},
            {"type": "minecraft:item", "name": "yishu:qi_pill", "weight": 5},
            {"type": "minecraft:item", "name": "yishu:clarity_pill", "weight": 4},
            {"type": "minecraft:item", "name": "yishu:bronze_coins", "weight": 12,
             "functions": [{"function": "minecraft:set_count", "count": {"min": 2, "max": 8}}]},
            {"type": "minecraft:empty", "weight": 6},
        ]
        write_json("yishu/loot_table/chests/%s.json" % realm["chest"], {
            "type": "minecraft:chest",
            "pools": [{
                "rolls": {"type": "minecraft:uniform", "min": 3, "max": 6},
                "bonus_rolls": 0.0,
                "entries": entries,
            }],
            "random_sequence": "yishu:chests/%s" % realm["chest"],
        })


# ==================================================================== 标签

def gen_biome_tags():
    print("== 群系标签（3 份）==")
    for realm in REALMS:
        write_json("yishu/tags/worldgen/biome/%s_field.json" % realm["prefix"], {
            "replace": False,
            "values": ["yishu:" + b for b in realm["biomes"]],
        })


def gen_structure_tag():
    print("== 结构标签（并入 #yishu:ruins）==")
    path = os.path.join(DATA, "yishu", "tags", "worldgen", "structure", "ruins.json")
    with open(path, encoding="utf-8") as f:
        d = json.load(f)
    values = list(d["values"])
    for realm in REALMS:
        for key in (realm["village"], realm["temple"], realm["rift"]):
            entry = "yishu:" + key
            if entry not in values:
                values.append(entry)
    d["values"] = values
    write_json("yishu/tags/worldgen/structure/ruins.json", d)


# ==================================================================== 主流程

if __name__ == "__main__":
    gen_dimension_types()
    gen_dimensions()
    gen_noise_settings()
    gen_tree_features()
    gen_ore_features()
    gen_fluid_and_flora()
    gen_biomes()
    gen_temple_templates()
    gen_structure_json()
    gen_chests()
    gen_biome_tags()
    gen_structure_tag()
    print("完成。")
