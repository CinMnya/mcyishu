#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""RCON 实测 —— 在真实服务端里验证第 14 轮的修复。

流程：
  1. 铺一块平台，用 /place template 放一座星辉村（v0）；
  2. 用 execute if block 抽查「箱子落地 / 门洞两格高 / 工位在地板上 / 围墙」；
  3. 查模板村民的职业（应为 yishu:astrologer / yishu:fate_master）；
  4. 在远处放一座观气台 + 召唤一个失业村民，等它认领 →
     职业变成 yishu:astrologer 就说明 POI 反射桥接生效（本轮核心修复）。

用法：先启动测试服务端（enable-rcon=true），再运行本脚本。
"""

import os
import socket
import struct
import sys
import time

# 测试服务端的 RCON 连接信息。密码从环境变量读，不写死在仓库里；
# 本地跑之前先 export YISHU_RCON_PASSWORD=<你在 server.properties 里设的那个>。
HOST = os.environ.get("YISHU_RCON_HOST", "127.0.0.1")
PORT = int(os.environ.get("YISHU_RCON_PORT", "25575"))
PASSWORD = os.environ.get("YISHU_RCON_PASSWORD", "")


class Rcon:
    def __init__(self, host, port, password, timeout=240):
        self.sock = socket.create_connection((host, port), timeout)
        self.rid = 0
        self._auth(password)

    def _send(self, typ, body):
        self.rid += 1
        data = struct.pack("<ii", self.rid, typ) + body.encode("utf-8") + b"\x00\x00"
        self.sock.sendall(struct.pack("<i", len(data)) + data)
        return self.rid

    def _recv(self):
        head = b""
        while len(head) < 4:
            chunk = self.sock.recv(4 - len(head))
            if not chunk:
                raise ConnectionError("RCON 连接被关闭")
            head += chunk
        length = struct.unpack("<i", head)[0]
        payload = b""
        while len(payload) < length:
            chunk = self.sock.recv(length - len(payload))
            if not chunk:
                raise ConnectionError("RCON 连接被关闭")
            payload += chunk
        rid, typ = struct.unpack("<ii", payload[:8])
        return rid, typ, payload[8:-2].decode("utf-8", "replace")

    def _auth(self, password):
        rid = self._send(3, password)
        r, _t, _b = self._recv()
        if r == -1:
            raise RuntimeError("RCON 认证失败")

    def cmd(self, command):
        rid = self._send(2, command)
        while True:
            r, _t, body = self._recv()
            if r == rid:
                return body


def main():
    rcon = None
    for _ in range(80):
        try:
            rcon = Rcon(HOST, PORT, PASSWORD)
            print("[rcon] 已连接")
            break
        except Exception:
            time.sleep(5)
    if rcon is None:
        print("[rcon] 连接失败，放弃")
        sys.exit(1)

    results = []

    def run(cmd, label=None):
        out = rcon.cmd(cmd)
        if label:
            print(f"  [{label}] {cmd}")
            print(f"    -> {out}")
        else:
            print(f"  > {cmd}")
            if out:
                print(f"    {out}")
        return out

    print("\n===== 1) 搭台 + 放置村庄模板（星辉村 v0）=====")
    run("forceload add -16 -16 48 48")
    run("forceload add 48 48 80 80")
    run("fill -8 100 -8 48 100 48 minecraft:stone")
    run("place template yishu:astral_village/v0 0 101 0")
    time.sleep(2)

    print("\n===== 2) 几何抽查（坐标 = 模板局部坐标 + (0,101,0)）=====")
    checks = [
        ("小屋床在 L2", "execute if block 3 103 3 minecraft:purple_bed"),
        ("小屋地板在 L1", "execute if block 5 102 5 yishu:astral_stone"),
        ("小屋箱子落地", "execute if block 7 103 3 minecraft:chest"),
        ("工位一（观气台）", "execute if block 14 103 10 yishu:qi_observatory"),
        ("工位二（香炉）", "execute if block 17 103 10 yishu:censer"),
        ("工坊门洞（下）", "execute if block 15 103 12 minecraft:air"),
        ("工坊门洞（上）", "execute if block 15 104 12 minecraft:air"),
        ("围墙", "execute if block 5 102 0 yishu:astral_bricks"),
        ("广场", "execute if block 15 102 16 minecraft:polished_cinnabar"),
        ("社坛核心", "execute if block 15 106 15 yishu:ruin_core"),
    ]
    passed = 0
    for label, cmd in checks:
        out = run(cmd, label)
        ok = "passed" in out.lower() or "成功" in out or "通过" in out
        results.append((label, ok))
        if ok:
            passed += 1
    print(f"  → 几何抽查：{passed}/{len(checks)} 通过")

    print("\n===== 3) 模板村民职业 =====")
    run("execute if entity @e[type=minecraft:villager,distance=..60]")
    run("data get entity @e[type=minecraft:villager,limit=1,sort=nearest,x=15,y=103,z=11] VillagerData")
    run("data get entity @e[type=minecraft:villager,limit=1,sort=nearest,x=16,y=103,z=11] VillagerData")

    print("\n===== 4) POI 认领测试（本轮核心修复）=====")
    run("fill 52 100 52 64 100 64 minecraft:stone")
    run("setblock 56 101 56 yishu:qi_observatory")
    run("summon minecraft:villager 56.5 101 57.5 "
        "{VillagerData:{type:\"minecraft:plains\",profession:\"minecraft:none\",level:1},"
        "AssignProfessionWhenSpawned:1b}")
    print("  等待村民认领（12 秒）……")
    time.sleep(12)
    out = run("data get entity @e[type=minecraft:villager,limit=1,sort=nearest,x=56,y=101,z=57] VillagerData")
    claimed = "yishu:astrologer" in out
    print(f"  → 认领测试：{'✅ 成功（职业 = 星象师）' if claimed else '❌ 村民没有认领到工作方块'}")

    print("\n===== 5) 自定义职业 id 合法性（召唤即验证注册表）=====")
    run("summon minecraft:villager 60.5 101 60.5 "
        "{VillagerData:{type:\"minecraft:plains\",profession:\"yishu:yarrow_master\",level:1}}")
    out = run("data get entity @e[type=minecraft:villager,limit=1,sort=nearest,x=60,y=101,z=60] VillagerData")
    print(f"  → 自定义职业召唤：{'✅ 成功' if 'yishu:yarrow_master' in out else '❌ 失败'}")

    print("\n===== 汇总 =====")
    for label, ok in results:
        print(f"  {'✅' if ok else '❌'} {label}")
    print(f"  {'✅' if claimed else '❌'} POI 认领")


if __name__ == "__main__":
    main()
