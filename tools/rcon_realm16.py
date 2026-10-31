# -*- coding: utf-8 -*-
"""第 16 轮实测：三个新维度能否真实生成、方块能否放置、村民能否转职。"""
import os
import sys
import time

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from rcon_probe import HOST, PORT, PASSWORD, Rcon

r = Rcon(HOST, PORT, PASSWORD)

def run(cmd, quiet=False):
    out = r.cmd(cmd)
    if not quiet:
        print(f"> {cmd}")
        print(f"   {out}\n")
    return out

print("===== 0) 就绪 =====")
run("tick query", quiet=True)

REALMS = [
    ("ziwei_realm", "ziwei_stone", "ziwei_field"),
    ("liuren_realm", "liuren_stone", "liuren_field"),
    ("qimen_realm", "qimen_stone", "qimen_field"),
]

print("===== 1) 三个新维度：访问 + 生成 + 方块放置 =====")
for realm, stone, biome in REALMS:
    print(f"--- {realm} ---")
    out = run(f"execute in yishu:{realm} run forceload add -16 -16 16 16", quiet=True)
    print(f"  forceload : {out.strip()[:90]}")
    time.sleep(1.5)
    out = run(f"execute in yishu:{realm} run setblock 0 100 0 yishu:{stone}", quiet=True)
    print(f"  setblock  : {out.strip()[:90]}")
    out = run(f"execute in yishu:{realm} run execute if block 0 100 0 yishu:{stone}", quiet=True)
    print(f"  校验      : {out.strip()[:90]}")
    out = run(f"execute in yishu:{realm} run locate biome yishu:{biome}", quiet=True)
    print(f"  locate    : {out.strip()[:110]}")
    print()

print("===== 2) 维度类型是否生效（时间/天穹）=====")
for realm, _, _ in REALMS:
    out = run(f"execute in yishu:{realm} run time query daytime", quiet=True)
    print(f"  {realm:14s} daytime: {out.strip()[:60]}")

print("\n===== 3) 新职业认领（三座新术坛）=====")
run("forceload add 96 96 144 144", quiet=True)
run("fill 100 99 100 140 99 140 minecraft:stone", quiet=True)
for i, (altar, tag) in enumerate([("dou_altar", "p1"), ("guiren_altar", "p2"), ("bamen_altar", "p3")]):
    x = 104 + i * 8
    run(f"setblock {x} 100 104 yishu:{altar}[facing=north]", quiet=True)
    run(f'summon minecraft:villager {x}.5 100 105.5 '
        f'{{Tags:["{tag}"],VillagerData:{{type:"minecraft:plains",profession:"minecraft:none",level:1}}}}',
        quiet=True)
print("  三座术坛已放置、三只村民已召唤，等待 26 秒认领……")
time.sleep(26)

for tag, label in [("p1", "斗枢师"), ("p2", "贵人师"), ("p3", "八门师")]:
    out = run(f"data get entity @e[tag={tag},limit=1] VillagerData", quiet=True)
    print(f"  {label}: {out.strip()[:120]}")

print("\n===== 4) 新灵兽是否可召唤 =====")
for mob_id in ("ziwei_star", "ren_serpent", "men_guard", "jia_bird"):
    out = run(f"summon yishu:{mob_id} 120 100 120", quiet=True)
    ok = "Summoned" in out or "summon" in out.lower()
    print(f"  {mob_id:14s} {'✅' if ok else '❌'} {out.strip()[:70]}")
