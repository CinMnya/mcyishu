#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""第 24 轮数据补丁：木头燃料与火焰体系。

背景：26.2 的熔炉燃料表（FuelValues）和木炭配方都按「物品标签」取材 ——
模组原木之前只有方块标签没有物品标签，所以：
  - 原木烧不出木炭（charcoal.json 的原料是 #minecraft:logs_that_burn 物品标签）
  - 原木进不了燃料表（FuelValues 按 #minecraft:logs 等物品标签给燃烧时长）
  - 木板上一轮已进 #minecraft:planks 所以其实已经能烧

本补丁把木头家族补进原版物品/方块标签（全部 merge 不覆盖）：
  原木      → item+block 的 logs / logs_that_burn
  木板      → item+block 的 planks（上一轮已做，此处幂等兜底）
  台阶/楼梯 → item+block 的 wooden_slabs / wooden_stairs（进燃料表）
  树苗      → item 的 saplings（进燃料表）
  船        → item 的 boats（原版船本身就是燃料，燃烧 1200 刻）
  树叶      → item 的 leaves（方块标签已有一份，物品侧补齐）

火焰蔓延（FireBlock 逐方块硬编码）不走数据，由 YsRegistry.registerWoodFlammability()
经 Fabric FlammableBlockRegistry 补录。

跑法：python tools/patch_round24.py（幂等，可重跑）。
"""
import json
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from patch_round21 import DATA, WOOD_SPECIES, write_json


def merge_tag(relpath, values):
    """往标签里 merge 一批值（不覆盖已有内容）。返回新增数量。"""
    path = os.path.join(DATA, relpath)
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
    write_json(relpath, tag)
    return added


def main():
    logs = ["yishu:%s_log" % s for s in WOOD_SPECIES]
    planks = ["yishu:%s_planks" % s for s in WOOD_SPECIES]
    slabs = ["yishu:%s_slab" % s for s in WOOD_SPECIES]
    stairs = ["yishu:%s_stairs" % s for s in WOOD_SPECIES]
    saplings = ["yishu:%s_sapling" % s for s in WOOD_SPECIES]
    leaves = ["yishu:%s_leaves" % s for s in WOOD_SPECIES]
    boats = ["yishu:%s_boat" % s for s in WOOD_SPECIES]

    total = 0
    # 物品侧（燃料表与木炭配方全看物品标签）
    for rel, values in (
            ("minecraft/tags/item/logs.json", logs),
            ("minecraft/tags/item/logs_that_burn.json", logs),
            ("minecraft/tags/item/planks.json", planks),
            ("minecraft/tags/item/wooden_slabs.json", slabs),
            ("minecraft/tags/item/wooden_stairs.json", stairs),
            ("minecraft/tags/item/saplings.json", saplings),
            ("minecraft/tags/item/leaves.json", leaves),
            ("minecraft/tags/item/boats.json", boats),
    ):
        n = merge_tag(rel, values)
        total += n
        print("== %s +%d ==" % (rel, n))
    # 方块侧（保留 vanilla 行为一致性：可燃木种排除绯红/诡异）
    for rel, values in (
            ("minecraft/tags/block/logs.json", logs),
            ("minecraft/tags/block/logs_that_burn.json", logs),
            ("minecraft/tags/block/planks.json", planks),
            ("minecraft/tags/block/wooden_slabs.json", slabs),
            ("minecraft/tags/block/wooden_stairs.json", stairs),
            ("minecraft/tags/block/leaves.json", leaves),
    ):
        n = merge_tag(rel, values)
        total += n
        print("== %s +%d ==" % (rel, n))
    print("第 24 轮数据补丁完成：共 +%d 条标签值" % total)


if __name__ == "__main__":
    main()
