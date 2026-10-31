# -*- coding: utf-8 -*-
"""worldgen 数据校验 —— 对照 26.2 原版结构，专治「建世界卡在正在准备生成世界」。

为什么需要这个脚本
------------------
第 10 轮把 `multi_noise` 群系源的 `offset` 写成了 `[0.0, 0.0]`，
而它在 `Climate.ParameterPoint` 里是**单个 long**（已用 javap 反汇编证实）——
整份 dimension JSON 解析失败，三个维度全部加载不了，进世界就卡死。
普通的 JSON 语法检查查不出这类「语法对、类型错」的问题，
所以这里拿 26.2.jar 里的原版文件当基准做结构比对。

用法
----
    python tools/validate_worldgen.py
退出码非 0 表示有硬性错误，构建前必须消掉。
"""
import json
import os
import sys
import zipfile

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DATA = os.path.join(ROOT, "src/main/resources/data/yishu")
VANILLA_JAR = os.environ.get("YISHU_MC_JAR") or os.path.join(
    os.path.expanduser("~"), "AppData/Roaming/.minecraft/versions/26.2/26.2.jar")
NS = "yishu"

ERRORS = []
WARNINGS = []


def err(msg):
    ERRORS.append(msg)
    print("[错误] " + msg)


def warn(msg):
    WARNINGS.append(msg)
    print("[警告] " + msg)


def load(path):
    with open(path, encoding="utf-8") as handle:
        return json.load(handle)


def is_number(v):
    return isinstance(v, (int, float)) and not isinstance(v, bool)


def is_range(v):
    """[min, max] 两元素数字数组。"""
    return (isinstance(v, list) and len(v) == 2
            and all(is_number(x) for x in v))


# ==================================================================== 基准数据
def vanilla_baseline():
    """从原版 jar 里取校验基准。"""
    base = {"attributes": set(), "noise_fields": {}, "biome_attrs": set()}
    if not os.path.exists(VANILLA_JAR):
        warn(f"找不到原版 jar，跳过基准比对：{VANILLA_JAR}")
        return base
    z = zipfile.ZipFile(VANILLA_JAR)
    names = z.namelist()

    # 1) dimension_type 的 attributes 键全集
    for n in names:
        if n.startswith("data/minecraft/dimension_type/") and n.endswith(".json"):
            d = json.loads(z.read(n))
            base["attributes"] |= set(d.get("attributes", {}).keys())

    # 2) timeline 轨道里出现的属性键（star_brightness / sunrise_sunset_color 等）
    for n in names:
        if n.startswith("data/minecraft/timeline/") and n.endswith(".json"):
            tracks = json.loads(z.read(n)).get("tracks", [])
            if isinstance(tracks, dict):
                # tracks 以属性键为 key 的字典形式
                base["attributes"] |= set(tracks.keys())
            elif isinstance(tracks, list):
                for track in tracks:
                    if isinstance(track, dict):
                        key = track.get("attribute") or track.get("type")
                    elif isinstance(track, str):
                        key = track
                    else:
                        key = None
                    if key:
                        base["attributes"].add(key)

    # 3) 各原版噪声设置的字段集（按文件名索引）
    for n in names:
        if n.startswith("data/minecraft/worldgen/noise_settings/") and n.endswith(".json"):
            base["noise_fields"][os.path.basename(n)] = set(json.loads(z.read(n)).keys())

    # 4) 原版群系 attributes 键全集
    for n in names:
        if n.startswith("data/minecraft/worldgen/biome/") and n.endswith(".json"):
            base["biome_attrs"] |= set(json.loads(z.read(n)).get("attributes", {}).keys())

    return base


BASE = vanilla_baseline()

# 已用 javap 反汇编 EnvironmentAttributes 确认存在、但原版 dimension_type / timeline
# 文件里没出现过的属性键（补进白名单，避免误报）。
KNOWN_EXTRA_ATTRIBUTES = {
    "minecraft:visual/sky_fog_end_distance",     # EnvironmentAttributes.SKY_FOG_END_DISTANCE
    "minecraft:visual/cloud_fog_end_distance",   # EnvironmentAttributes.CLOUD_FOG_END_DISTANCE
    "minecraft:visual/water_fog_color",          # EnvironmentAttributes.WATER_FOG_COLOR
    "minecraft:visual/water_fog_start_distance",
    "minecraft:visual/water_fog_end_distance",
    "minecraft:visual/block_light_tint",
    "minecraft:visual/night_vision_color",
    "minecraft:visual/moon_phase",
    "minecraft:audio/music_volume",
    "minecraft:gameplay/increased_fire_burnout",
    "minecraft:gameplay/surface_slime_spawn_chance",
    "minecraft:gameplay/can_pillager_patrol_spawn",
    "minecraft:gameplay/villager_activity",
    "minecraft:gameplay/baby_villager_activity",
}
BASE["attributes"] |= KNOWN_EXTRA_ATTRIBUTES

print(f"基准：attributes {len(BASE['attributes'])} 键 · "
      f"noise_settings {len(BASE['noise_fields'])} 份 · "
      f"群系 attributes {len(BASE['biome_attrs'])} 键\n")

# ==================================================================== 1. dimension
CLIMATE_AXES = ["temperature", "humidity", "continentalness",
                "erosion", "depth", "weirdness"]

dim_dir = os.path.join(DATA, "dimension")
for fname in sorted(os.listdir(dim_dir)) if os.path.isdir(dim_dir) else []:
    if not fname.endswith(".json"):
        continue
    path = os.path.join(dim_dir, fname)
    d = load(path)
    tag = f"dimension/{fname}"

    # --- 顶层 ---
    if "type" not in d:
        err(f"{tag}: 缺少 type")
    else:
        dt = d["type"].split(":", 1)[-1]
        if not os.path.exists(os.path.join(DATA, "dimension_type", dt + ".json")):
            err(f"{tag}: type 引用的 dimension_type「{d['type']}」不存在")

    gen = d.get("generator")
    if not isinstance(gen, dict):
        err(f"{tag}: generator 必须是对象")
        continue

    # --- 噪声设置引用 ---
    settings = gen.get("settings")
    if settings:
        ns_file = settings.split(":", 1)[-1]
        if settings.startswith(NS + ":") and not os.path.exists(
                os.path.join(DATA, "worldgen/noise_settings", ns_file + ".json")):
            err(f"{tag}: settings 引用的噪声设置「{settings}」不存在")

    # --- 生物群系源（重点）---
    bs = gen.get("biome_source")
    if not isinstance(bs, dict):
        err(f"{tag}: generator.biome_source 必须是对象")
        continue
    if bs.get("type") == "minecraft:multi_noise":
        biomes = bs.get("biomes")
        if not isinstance(biomes, list) or not biomes:
            err(f"{tag}: multi_noise 的 biomes 必须是非空列表")
        else:
            param_keys = None
            for i, entry in enumerate(biomes):
                bid = entry.get("biome", f"#{i}")
                p = entry.get("parameters")
                if not isinstance(p, dict):
                    err(f"{tag}: biomes[{i}]（{bid}）缺少 parameters")
                    continue
                param_keys = set(p.keys())
                # 六轴必须是 [min, max]
                for axis in CLIMATE_AXES:
                    if axis not in p:
                        err(f"{tag}: biomes[{i}]（{bid}）缺少参数轴 {axis}")
                    elif not is_range(p[axis]):
                        err(f"{tag}: biomes[{i}]（{bid}）的 {axis} 必须是 "
                            f"[min, max] 两元素数字数组，实际是 {p[axis]!r}")
                # offset 必须是单个数字 —— 本轮踩的坑
                if "offset" not in p:
                    err(f"{tag}: biomes[{i}]（{bid}）缺少 offset")
                elif not is_number(p["offset"]):
                    err(f"{tag}: biomes[{i}]（{bid}）的 offset 必须是单个数字"
                        f"（Climate.ParameterPoint.offset 是 long），"
                        f"实际是 {type(p['offset']).__name__} {p['offset']!r}")
                # 群系本身存在吗
                if bid.startswith(NS + ":"):
                    bfile = os.path.join(DATA, "worldgen/biome", bid.split(":", 1)[1] + ".json")
                    if not os.path.exists(bfile):
                        err(f"{tag}: biomes[{i}] 引用了不存在的群系 {bid}")
            if param_keys is not None:
                extra = param_keys - set(CLIMATE_AXES) - {"offset"}
                if extra:
                    warn(f"{tag}: parameters 有多余键 {sorted(extra)}")

# ==================================================================== 2. dimension_type
dt_dir = os.path.join(DATA, "dimension_type")
REQUIRED_DT = ["min_y", "height", "logical_height", "coordinate_scale",
               "has_skylight", "has_ceiling", "ambient_light", "infiniburn"]
for fname in sorted(os.listdir(dt_dir)) if os.path.isdir(dt_dir) else []:
    if not fname.endswith(".json"):
        continue
    d = load(os.path.join(dt_dir, fname))
    tag = f"dimension_type/{fname}"
    for key in REQUIRED_DT:
        if key not in d:
            err(f"{tag}: 缺少必填字段 {key}")
    if d.get("min_y") is not None and d.get("height") is not None:
        if d["min_y"] % 16 != 0:
            err(f"{tag}: min_y（{d['min_y']}）必须是 16 的倍数")
        if d["height"] % 16 != 0:
            err(f"{tag}: height（{d['height']}）必须是 16 的倍数")
        if d["height"] > 4064:
            err(f"{tag}: height（{d['height']}）超出上限 4064")
    if BASE["attributes"]:
        unknown = set(d.get("attributes", {}).keys()) - BASE["attributes"]
        if unknown:
            warn(f"{tag}: attributes 用了基准之外的键 {sorted(unknown)}"
                 f"（合法但未被原版文件覆盖，请确认拼写）")

# ==================================================================== 3. noise_settings
ns_dir = os.path.join(DATA, "worldgen/noise_settings")
for fname in sorted(os.listdir(ns_dir)) if os.path.isdir(ns_dir) else []:
    if not fname.endswith(".json"):
        continue
    d = load(os.path.join(ns_dir, fname))
    tag = f"noise_settings/{fname}"
    ours = set(d.keys())
    # 用字段最全的原版文件做超集基准
    if BASE["noise_fields"]:
        ref_name, ref = max(BASE["noise_fields"].items(), key=lambda kv: len(kv[1]))
        missing = ref - ours
        if missing:
            err(f"{tag}: 对照原版 {ref_name} 缺少字段 {sorted(missing)}")
        for key in ("default_block", "default_fluid"):
            v = d.get(key)
            if isinstance(v, dict) and not v.get("Name", "").startswith(("minecraft:", NS + ":")):
                err(f"{tag}: {key}.Name 不是合法的方块 id：{v.get('Name')!r}")

# ==================================================================== 4. biome
biome_dir = os.path.join(DATA, "worldgen/biome")
for fname in sorted(os.listdir(biome_dir)) if os.path.isdir(biome_dir) else []:
    if not fname.endswith(".json"):
        continue
    d = load(os.path.join(biome_dir, fname))
    tag = f"biome/{fname}"
    features = d.get("features")
    if not isinstance(features, list):
        err(f"{tag}: features 必须是列表")
    elif len(features) != 11:
        err(f"{tag}: features 必须正好 11 步（原版 GenerationStep.Decoration），"
            f"实际 {len(features)}")
    if BASE["biome_attrs"]:
        unknown = set(d.get("attributes", {}).keys()) - BASE["biome_attrs"]
        if unknown:
            warn(f"{tag}: attributes 用了基准之外的键 {sorted(unknown)}")
    if "carvers" not in d:
        err(f"{tag}: 缺少 carvers（必须是列表，可以为空）")
    elif not isinstance(d["carvers"], list):
        err(f"{tag}: carvers 必须是列表")

# ==================================================================== 5. 引用完整性
pf_dir = os.path.join(DATA, "worldgen/placed_feature")
cf_dir = os.path.join(DATA, "worldgen/configured_feature")


def collect_refs(node, out):
    if isinstance(node, dict):
        fid = node.get("feature")
        if isinstance(fid, str) and fid.startswith(NS + ":"):
            out.add(fid.split(":", 1)[1])
        for v in node.values():
            collect_refs(v, out)
    elif isinstance(node, list):
        for v in node:
            collect_refs(v, out)


refs = set()
for fname in sorted(os.listdir(pf_dir)) if os.path.isdir(pf_dir) else []:
    if fname.endswith(".json"):
        collect_refs(load(os.path.join(pf_dir, fname)), refs)
for r in sorted(refs):
    if not os.path.exists(os.path.join(cf_dir, r + ".json")):
        err(f"placed_feature 引用了不存在的 configured_feature：{NS}:{r}")

for fname in sorted(os.listdir(biome_dir)) if os.path.isdir(biome_dir) else []:
    if not fname.endswith(".json"):
        continue
    d = load(os.path.join(biome_dir, fname))
    for step in d.get("features", []):
        for fid in step if isinstance(step, list) else []:
            if isinstance(fid, str) and fid.startswith(NS + ":"):
                p = fid.split(":", 1)[1]
                if not os.path.exists(os.path.join(pf_dir, p + ".json")):
                    err(f"biome/{fname} 引用了不存在的 placed_feature：{fid}")

# ==================================================================== 汇总
print()
if ERRORS:
    print("=" * 60)
    print(f"校验失败：{len(ERRORS)} 个错误，{len(WARNINGS)} 个警告")
    print("=" * 60)
    sys.exit(1)
print(f"worldgen 校验通过（{len(WARNINGS)} 个警告）。")
