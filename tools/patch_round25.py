#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""第 25 轮数据补丁：木变体家族 + 运输船的配方、掉落、标签。

21 种树 × 8 类方块（栅栏/栅栏门/门/活板门/按钮/压力板/立牌/挂牌）
+ 21 条运输船。注册在 YsRegistry.variantSet()/chestBoatSet()。

本补丁负责（全部幂等，可重跑）：
  1. 配方 8×21=168 份（原版同款图案）；
  2. 掉落表：除立牌/挂牌掉 yishu:<树种>_sign、其余全部掉自身；
  3. mineable/axe 补录（不进 = 挖不掉，血泪教训）；
  4. 原版方块/物品标签：
     fences+wooden_fences / fence_gates / wooden_doors / wooden_trapdoors /
     wooden_buttons / wooden_pressure_plates / signs+standing_signs+wall_signs /
     boats（运输船）。

跑法：python tools/patch_round25.py
"""
import json
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from patch_round21 import DATA, WOOD_SPECIES, write_json
from patch_round24 import merge_tag


def self_drop_item(item_id):
    """掉落表：掉一个指定物品（告示牌/门这类方块没有同名物品时用）。"""
    return {
        "type": "minecraft:block",
        "pools": [{
            "rolls": 1.0,
            "entries": [{"type": "minecraft:item", "name": item_id}],
            "conditions": [{"condition": "minecraft:survives_explosion"}],
        }],
    }


def shaped(pattern, key, result, count, group, category="building"):
    return {
        "type": "minecraft:crafting_shaped",
        "category": category,
        "group": group,
        "pattern": pattern,
        "key": key,
        "result": {"count": count, "id": result},
    }


def shapeless(ingredients, result, count, group, category="building"):
    return {
        "type": "minecraft:crafting_shapeless",
        "category": category,
        "group": group,
        "ingredients": ingredients,
        "result": {"count": count, "id": result},
    }


def main():
    added = 0

    # ---- 1. 配方（原版同款图案） ----
    recipes = {
        "_fence": shaped(["PSP", "PSP"],
                         {"P": "yishu:%s_planks", "S": "minecraft:stick"},
                         "yishu:%s_fence", 3, "wooden_fence"),
        "_fence_gate": shaped(["SPS", "SPS"],
                              {"P": "yishu:%s_planks", "S": "minecraft:stick"},
                              "yishu:%s_fence_gate", 1, "wooden_fence_gate"),
        "_door": shaped(["PP", "PP", "PP"],
                        {"P": "yishu:%s_planks"},
                        "yishu:%s_door", 3, "wooden_door"),
        "_trapdoor": shaped(["PPP", "PPP"],
                            {"P": "yishu:%s_planks"},
                            "yishu:%s_trapdoor", 2, "wooden_trapdoor"),
        "_button": shapeless(["yishu:%s_planks"],
                             "yishu:%s_button", 1, "wooden_buttons"),
        "_pressure_plate": shaped(["PP"],
                                  {"P": "yishu:%s_planks"},
                                  "yishu:%s_pressure_plate", 1, "wooden_pressure_plates"),
        "_sign": shaped(["PPP", "PPP", " S "],
                        {"P": "yishu:%s_planks", "S": "minecraft:stick"},
                        "yishu:%s_sign", 3, "wooden_signs"),
    }
    for species in WOOD_SPECIES:
        for suffix, template in recipes.items():
            json_str = json.dumps(template)
            json_str = json_str.replace("%s", species)
            write_json("yishu/recipe/%s%s.json" % (species, suffix),
                       json.loads(json_str))
            added += 1
        # 运输船：普通船 + 箱子
        write_json("yishu/recipe/%s_chest_boat.json" % species, shapeless(
            ["yishu:%s_boat" % species, "minecraft:chest"],
            "yishu:%s_chest_boat" % species, 1, "chest_boat", "misc"))
        added += 1

    # ---- 2. 掉落表 ----
    variants = ["_fence", "_fence_gate", "_door", "_trapdoor", "_button",
                "_pressure_plate"]
    for species in WOOD_SPECIES:
        for suffix in variants:
            rel = "yishu/loot_table/blocks/%s%s.json" % (species, suffix)
            if not os.path.exists(os.path.join(DATA, rel)):
                write_json(rel, self_drop_item("yishu:%s%s" % (species, suffix)))
                added += 1
        # 立牌/挂牌没有同名物品，掉 yishu:<树种>_sign
        for suffix in ("_standing_sign", "_wall_sign"):
            rel = "yishu/loot_table/blocks/%s%s.json" % (species, suffix)
            if not os.path.exists(os.path.join(DATA, rel)):
                write_json(rel, self_drop_item("yishu:%s_sign" % species))
                added += 1

    # ---- 3. mineable/axe（不进 = 挖不掉） ----
    axe_blocks = []
    for species in WOOD_SPECIES:
        axe_blocks += ["yishu:%s%s" % (species, s) for s in
                       ("_fence", "_fence_gate", "_door", "_trapdoor", "_button",
                        "_pressure_plate", "_standing_sign", "_wall_sign")]
    added += merge_tag("minecraft/tags/block/mineable/axe.json", axe_blocks)

    # ---- 4. 原版标签 ----
    tag_groups = [
        # (标签 relpath（不含 minecraft/tags 前缀后的部分按 DATA 根）, 值清单)
        ("item/fences.json", [None, "yishu:%s_fence"]),
        ("block/fences.json", [None, "yishu:%s_fence"]),
        ("item/wooden_fences.json", [None, "yishu:%s_fence"]),
        ("block/wooden_fences.json", [None, "yishu:%s_fence"]),
        ("item/fence_gates.json", [None, "yishu:%s_fence_gate"]),
        ("block/fence_gates.json", [None, "yishu:%s_fence_gate"]),
        ("item/wooden_doors.json", [None, "yishu:%s_door"]),
        ("block/wooden_doors.json", [None, "yishu:%s_door"]),
        ("item/wooden_trapdoors.json", [None, "yishu:%s_trapdoor"]),
        ("block/wooden_trapdoors.json", [None, "yishu:%s_trapdoor"]),
        ("item/wooden_buttons.json", [None, "yishu:%s_button"]),
        ("block/wooden_buttons.json", [None, "yishu:%s_button"]),
        ("item/wooden_pressure_plates.json", [None, "yishu:%s_pressure_plate"]),
        ("block/wooden_pressure_plates.json", [None, "yishu:%s_pressure_plate"]),
        ("item/signs.json", [None, "yishu:%s_sign"]),
        ("block/signs.json", [None, "yishu:%s_standing_sign", "yishu:%s_wall_sign"]),
        ("block/standing_signs.json", [None, "yishu:%s_standing_sign"]),
        ("block/wall_signs.json", [None, "yishu:%s_wall_sign"]),
        ("item/boats.json", [None, "yishu:%s_boat", "yishu:%s_chest_boat"]),
    ]
    for rel, templates in tag_groups:
        values = []
        for tpl in templates[1:]:
            for species in WOOD_SPECIES:
                values.append(tpl % species)
        added += merge_tag("minecraft/tags/" + rel, values)

    print("第 25 轮补丁完成：新增/补录 %d 项" % added)


if __name__ == "__main__":
    main()
