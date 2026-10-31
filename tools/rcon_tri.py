# -*- coding: utf-8 -*-
"""第 15 轮实机验证：三座术坛能放、三只村民能转职。"""
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
run("tick query")
run("forceload add 24 24 72 72", quiet=True)
run("fill 30 100 30 70 100 70 minecraft:stone", quiet=True)
print("  平台已铺好\n")

print("===== 1) 三座术坛放置（验证 blockstate 合法）=====")
for label, cmd in [
    ("紫微星台", "setblock 40 101 40 yishu:ziwei_terrace[facing=north]"),
    ("六壬式坛", "setblock 44 101 40 yishu:liuren_altar[facing=east]"),
    ("奇门局阵", "setblock 48 101 40 yishu:qimen_array[facing=south]"),
]:
    out = run(cmd, quiet=True)
    print(f"  {label:8s} {out.strip()}")

print("\n===== 2) 方块真的在那儿吗 =====")
for label, pos, block in [
    ("紫微星台", "40 101 40", "yishu:ziwei_terrace"),
    ("六壬式坛", "44 101 40", "yishu:liuren_altar"),
    ("奇门局阵", "48 101 40", "yishu:qimen_array"),
]:
    out = run(f"execute if block {pos} {block}", quiet=True)
    print(f"  {label:8s} {out.strip()}")

print("\n===== 3) 召唤三只失业村民 =====")
for tag, x in [("sm", 41), ("lm", 45), ("am", 49)]:
    run(f'summon minecraft:villager {x}.5 101 40.5 '
        f'{{Tags:["{tag}"],VillagerData:{{type:"minecraft:plains",'
        f'profession:"minecraft:none",level:1}}}}', quiet=True)
print("  已召唤，等待 28 秒认领……")
time.sleep(28)

print("\n===== 4) 职业结果 =====")
for tag, label in [("sm", "星命师"), ("lm", "课师"), ("am", "局师")]:
    out = run(f"data get entity @e[tag={tag},limit=1] VillagerData", quiet=True)
    print(f"  {label}: {out.strip()}")

print("\n===== 5) 术坛朝向属性 =====")
for pos in ["40 101 40", "44 101 40", "48 101 40"]:
    out = run(f"execute if block {pos} #minecraft:mineable/pickaxe", quiet=True)
    print(f"  {pos}: {out.strip()}")
