#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""第 23 轮数据补丁：木板接入原版功能体系。

职责：
1. 木板进原版标签：把 21 种模组木板写进 minecraft:planks 的 item/block 标签。
   原版凡是引用 #minecraft:planks 的配方（木棍 / 工作台 / 箱子 / 床 / 碗 /
   木桶 / 书架 / 砂轮 / 织布机 / 活塞 / 音符盒 …… 共 34 个）即刻全部解锁，
   一行配方都不用手写 —— 标签就是原版留给模组木头的官方接口。
2. 船配方：每种树 1 条船（原版同款 "P P / PPP" 图案）。

跑法：python tools/patch_round23.py（幂等，可重跑）。
"""
import json
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from patch_round21 import DATA, WOOD_SPECIES, write_json


def gen_planks_tags():
    """21 种模组木板并入原版 minecraft:planks 标签（物品 + 方块两份）。"""
    values = ["yishu:%s_planks" % s for s in WOOD_SPECIES]
    for rel in ("minecraft/tags/item/planks.json",
                "minecraft/tags/block/planks.json"):
        path = os.path.join(DATA, rel)
        if os.path.exists(path):
            with open(path, encoding="utf-8") as f:
                tag = json.load(f)
        else:
            tag = {"replace": False, "values": []}
        merged = list(tag.get("values", []))
        added = 0
        for v in values:
            if v not in merged:
                merged.append(v)
                added += 1
        tag["replace"] = False
        tag["values"] = merged
        write_json(rel, tag)
        print("== %s +%d 木板 ==" % (rel, added))


def gen_boat_recipes():
    """每种树一条船：原版同款图案，3 板 + 2 空位。"""
    print("== 木船配方（21 条）==")
    for species in WOOD_SPECIES:
        planks = "yishu:%s_planks" % species
        write_json("yishu/recipe/%s_boat.json" % species, {
            "type": "minecraft:crafting_shaped",
            "category": "misc",
            "group": "boat",
            "pattern": ["# #", "###"],
            "key": {"#": planks},
            "result": {"count": 1, "id": "yishu:%s_boat" % species},
        })
    print("   21 份配方写好")


def main():
    gen_planks_tags()
    gen_boat_recipes()
    print("第 23 轮数据补丁完成")


if __name__ == "__main__":
    main()
