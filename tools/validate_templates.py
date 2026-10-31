#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""校验生成的结构模板与数据 JSON。

对每个 NBT 模板：
  * 能否解析（结构模板格式）
  * size 是否与预期一致
  * 方块 / 实体的坐标是否越界
  * 方块引用的 palette 序号是否合法
  * 实体 nbt 是否带 id
并统计方块数与实体数。

对数据 JSON：
  * 全部可解析
  * 生物群系 features 长度为 11
  * 结构 JSON 的关键字段存在
"""

import gzip
import json
import os
import struct
import sys

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
DATA = os.path.join(ROOT, "src", "main", "resources", "data")


# ------------------------------------------------------------------ NBT 解析

def parse(b, i):
    t = b[i]; i += 1
    if t == 0:
        return None, i
    nl = struct.unpack(">H", b[i:i + 2])[0]; i += 2
    name = b[i:i + nl].decode("utf-8"); i += nl
    v, i = payload(b, i, t)
    return (name, t, v), i


def payload(b, i, t):
    if t == 1: return struct.unpack(">b", b[i:i + 1])[0], i + 1
    if t == 2: return struct.unpack(">h", b[i:i + 2])[0], i + 2
    if t == 3: return struct.unpack(">i", b[i:i + 4])[0], i + 4
    if t == 4: return struct.unpack(">q", b[i:i + 8])[0], i + 8
    if t == 5: return struct.unpack(">f", b[i:i + 4])[0], i + 4
    if t == 6: return struct.unpack(">d", b[i:i + 8])[0], i + 8
    if t == 7:
        n = struct.unpack(">i", b[i:i + 4])[0]
        return list(b[i + 4:i + 4 + n]), i + 4 + n
    if t == 8:
        n = struct.unpack(">H", b[i:i + 2])[0]
        return b[i + 2:i + 2 + n].decode("utf-8"), i + 2 + n
    if t == 9:
        et = b[i]; n = struct.unpack(">i", b[i + 1:i + 5])[0]; i += 5
        out = []
        for _ in range(n):
            v, i = payload(b, i, et)
            out.append(v)
        return out, i
    if t == 10:
        out = {}
        while True:
            if b[i] == 0:
                i += 1
                break
            (nm, tt, vv), i = parse(b, i)
            out[nm] = vv
        return out, i
    if t == 11:
        n = struct.unpack(">i", b[i:i + 4])[0]
        return [struct.unpack(">i", b[i + 4 + k * 4:i + 8 + k * 4])[0] for k in range(n)], i + 4 + n * 4
    if t == 12:
        n = struct.unpack(">i", b[i:i + 4])[0]
        return [struct.unpack(">q", b[i + 4 + k * 8:i + 12 + k * 8])[0] for k in range(n)], i + 4 + n * 8
    raise ValueError("未知 tag: %d" % t)


def load_nbt(path):
    d = open(path, "rb").read()
    if d[:2] == b"\x1f\x8b":
        d = gzip.decompress(d)
    (name, t, root), end = parse(d, 0)
    assert t == 10, "根标签不是 compound"
    assert end == len(d), "文件尾部有残留字节（%d/%d）" % (end, len(d))
    return root


# ------------------------------------------------------------------ 校验

def validate_templates():
    base = os.path.join(DATA, "yishu", "structure")
    failures = []
    total_blocks = 0
    total_entities = 0
    groups = {}
    for group in sorted(os.listdir(base)):
        gdir = os.path.join(base, group)
        if not os.path.isdir(gdir):
            continue
        for name in sorted(os.listdir(gdir)):
            path = os.path.join(gdir, name)
            label = "%s/%s" % (group, name)
            try:
                root = load_nbt(path)
                w, h, d = root["size"]
                assert w > 0 and h > 0 and d > 0, "size 非法"
                palette = root["palette"]
                blocks = root["blocks"]
                ents = root["entities"]
                for b in blocks:
                    x, y, z = b["pos"]
                    assert 0 <= x < w and 0 <= y < h and 0 <= z < d, \
                        "方块越界 (%d,%d,%d) size=(%d,%d,%d)" % (x, y, z, w, h, d)
                    assert 0 <= b["state"] < len(palette), "palette 序号越界"
                for e in ents:
                    x, y, z = e["blockPos"]
                    assert 0 <= x < w and 0 <= y < h and 0 <= z < d, "实体越界"
                    assert "id" in e["nbt"], "实体缺少 id"
                    assert isinstance(e["pos"], list) and len(e["pos"]) == 3, "实体 pos 非法"
                total_blocks += len(blocks)
                total_entities += len(ents)
                g = groups.setdefault(group, [0, 0, 0])
                g[0] += 1
                g[1] += len(blocks)
                g[2] += len(ents)
            except Exception as exc:  # noqa: BLE001
                failures.append((label, repr(exc)))
    print("模板统计：")
    for g, (n, bl, en) in sorted(groups.items()):
        print("  %-24s %d 个 | 方块 %6d | 实体 %3d" % (g, n, bl, en))
    print("  合计：方块 %d，实体 %d" % (total_blocks, total_entities))
    return failures


def validate_json():
    failures = []
    checked = 0
    for root, _dirs, files in os.walk(DATA):
        for name in files:
            if not name.endswith(".json"):
                continue
            path = os.path.join(root, name)
            rel = os.path.relpath(path, DATA)
            checked += 1
            try:
                obj = json.load(open(path, encoding="utf-8"))
            except Exception as exc:  # noqa: BLE001
                failures.append((rel, "JSON 解析失败: %r" % exc))
                continue
            if "/worldgen/biome/" in rel.replace("\\", "/"):
                if "features" in obj and len(obj["features"]) != 11:
                    failures.append((rel, "features 长度应为 11，实为 %d" % len(obj["features"])))
            flat = rel.replace("\\", "/")
            if "/worldgen/structure/" in flat and "/tags/" not in flat:
                for key in ("type", "biomes", "start_pool", "start_height", "max_distance_from_center"):
                    if key not in obj:
                        failures.append((rel, "缺少字段 %s" % key))
                if "max_distance_from_center" in obj:
                    pad = 0 if obj.get("terrain_adaptation", "none") == "none" else 12
                    if obj["max_distance_from_center"] + pad > 128:
                        failures.append((rel, "max_distance_from_center 超限"))
    print("JSON 检查：%d 个文件" % checked)
    return failures


if __name__ == "__main__":
    fails = []
    fails += validate_templates()
    fails += validate_json()
    if fails:
        print("\n!!! 发现 %d 个问题：" % len(fails))
        for label, msg in fails:
            print("  -", label, "|", msg)
        sys.exit(1)
    print("\n全部校验通过。")
