#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""易枢百科 Wiki 生成器。

从模组真实数据（lang 键、配方 JSON、维度/结构/群系文件）自动生成图鉴表格，
配合手写的机制/维度/结构条目，产出单文件 docs/yishu_wiki.html。

幂等可重跑。用法：python tools/generate_wiki.py
"""

import json
import os
import re

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)
RES = os.path.join(ROOT, "src", "main", "resources")
OUT = os.path.join(ROOT, "docs", "yishu_wiki.html")

LANG = json.load(open(
    os.path.join(RES, "assets", "yishu", "lang", "zh_cn.json"), encoding="utf-8"))


def zh(key):
    return LANG.get(key, key)


def items():
    """全部物品（排除船/刷怪蛋/门/告示牌等由专门章节处理的内容）。"""
    out = {}
    for k, v in LANG.items():
        if k.startswith("item.yishu."):
            name = k[len("item.yishu."):]
            out[name] = v
    return out


def blocks():
    out = {}
    for k, v in LANG.items():
        if k.startswith("block.yishu."):
            out[k[len("block.yishu."):]] = v
    return out


TERM_ZH = {
    "yishu.term.wuxing": "五行", "yishu.term.bazi": "八字",
    "yishu.term.liuyao": "六爻", "yishu.term.fengshui": "风水",
    "yishu.term.longmai": "龙脉", "yishu.term.taisui": "太岁",
}

IT = items()
BK = blocks()
NOTES = {n: LANG.get("item.yishu.%s.note" % n) for n in IT}

# ============================================================ 工具函数


def table(headers, rows, cls="wiki-table"):
    h = "".join("<th>%s</th>" % c for c in headers)
    body = "".join("<tr>%s</tr>" % "".join("<td>%s</td>" % c for c in r)
                   for r in rows)
    return '<table class="%s"><thead><tr>%s</tr></thead><tbody>%s</tbody></table>' % (
        cls, h, body)


def item_rows(names, with_note=True):
    rows = []
    for n in names:
        if n not in IT:
            continue
        row = ["<code>%s</code>" % n, IT[n]]
        if with_note:
            note = NOTES.get(n) or ""
            row.append(note)
        rows.append(row)
    return rows


def block_rows(names):
    rows = []
    for n in names:
        if n in BK:
            rows.append(["<code>%s</code>" % n, BK[n]])
    return rows


def sec(icon, id_, title, desc, inner):
    d = '<p class="sec-desc">%s</p>' % desc if desc else ""
    return ('<section class="card" id="%s"><h2><span class="ico">%s</span>%s</h2>'
            '%s%s</section>') % (id_, icon, title, d, inner)


# ============================================================ 自动图鉴数据

WOOD_SPECIES = ["astral", "meteor", "moon", "jade", "cloud", "peach", "hexa",
                "ink", "mystery", "ziwei", "dou", "tianji", "ren", "gui", "she",
                "men", "xing", "taiyi", "wenchang", "jishen", "dun"]
WOOD_ZH = {s: BK.get(s + "_wood_log", s).replace("原木", "") for s in WOOD_SPECIES}

WOOD_BLOCK_KINDS = [
    ("_log", "原木"), ("_wood", "木头"), ("_leaves", "树叶"), ("_sapling", "树苗"),
    ("_planks", "木板"), ("_slab", "台阶"), ("_stairs", "楼梯"),
    ("_fence", "栅栏"), ("_fence_gate", "栅栏门"), ("_door", "门"),
    ("_trapdoor", "活板门"), ("_button", "按钮"), ("_pressure_plate", "压力板"),
    ("_standing_sign", "立式告示牌"), ("_wall_sign", "挂墙告示牌"),
]


def wood_table():
    rows = []
    for s in WOOD_SPECIES:
        cells = ['<span class="chip">%s</span>' % WOOD_ZH[s]]
        for suf, _zhname in WOOD_BLOCK_KINDS:
            key = s + suf if suf != "_wood" else s + "_wood"
            if key in BK:
                cells.append(BK[key])
            else:
                cells.append('<span class="dim">—</span>')
        rows.append(cells)
    headers = ["树种"] + [k[1] for k in WOOD_BLOCK_KINDS]
    return table(headers, rows, cls="wiki-table wide")


def boat_table():
    rows = []
    for s in WOOD_SPECIES:
        rows.append(["<code>%s_wood_boat</code>" % s, WOOD_ZH[s] + "船",
                     "<code>%s_wood_chest_boat</code>" % s, WOOD_ZH[s] + "运输船"])
    return table(["普通船 id", "名称", "运输船 id", "名称"], rows)


def egg_table():
    rows = []
    for n in sorted(IT):
        if n.endswith("_spawn_egg"):
            rows.append(["<code>%s</code>" % n, IT[n]])
    return table(["物品 id", "名称"], rows)


def bucket_table():
    rows = []
    for n in sorted(IT):
        if n.endswith("_bucket"):
            note = NOTES.get(n) or ""
            rows.append(["<code>%s</code>" % n, IT[n], note])
    return table(["物品 id", "名称", "图鉴"], rows)


def crop_table():
    rows = []
    for n in sorted(IT):
        if n.endswith(("_seeds", "_grain")) or n in (
                "yao_pod", "ziwei_grain", "taiyi_grain", "liuren_grain",
                "qimen_grain", "astral_grain", "jade_grain"):
            if n in IT:
                rows.append(["<code>%s</code>" % n, IT[n], NOTES.get(n) or ""])
    # 去重
    seen, out = set(), []
    for r in rows:
        if r[0] not in seen:
            seen.add(r[0])
            out.append(r)
    return table(["物品 id", "名称", "图鉴"], out)


# ============================================================ 手写内容

REALMS = [
    dict(id="fate_realm", zh="命理界", prefix="astral", color="#6a5acd",
         gate="星门 astral_gate", key="星枢引 astral_key",
         time="十二长生（长生→养），一昼夜走完一轮命气循环",
         theme="浮空的原野之上，命盘正在缓缓转动。天干效力全开，问卦看命格外清晰。",
         biomes=["astral_field 星野", "astral_grove 星林", "astral_peaks 星峰",
                 "astral_marsh 星泽", "astral_dunes 星丘"],
         ores=["astral_ore 星辉矿石", "star_sand_ore 星砂矿石",
               "crystal_ore 星晶矿石", "meteor_ore 陨星矿石"],
         keyMats=["星辉尘 astral_dust", "星砂 star_sand", "星晶 star_crystal",
                  "陨铁 meteor_iron"],
         liquids=["星辉露 astral_dew（灵液）", "星陨熔渣 astral_slag（凶液）"],
         structures=["星辉观星台 astral_observatory（宝箱）", "星辉村 astral_village",
                     "主世界的星辉裂隙 astral_rift"],
         mobs=["星辉符灵", "星狐", "星屑史莱姆", "观星者"],
         crops=["星辉穗 astral_wheat → 星辉穗实/星辉种子"],
         special="十二长生时辰；天干效力 ×1.2"),
    dict(id="fengshui_realm", zh="风水界", prefix="jade", color="#2e8b57",
         gate="玉门 jade_gate", key="玉髓引 jade_key",
         time="十二山水（起脉→归藏），一昼夜走完一条龙脉的起结",
         theme="幽谷里的每一道山脊都是龙的行踪。龙脉密布、气场浓稠，风水增益放大，煞气也一样。",
         biomes=["vein_valley 灵脉谷", "jade_ridge 青玉岭", "jade_shore 青玉岸",
                 "cloud_forest 云雾林", "wind_plateau 风台原",
                 "stone_steppe 石砾原", "jade_ocean 青玉海"],
         ores=["jade_ore 青玉矿石", "vein_ore 灵脉矿石", "verdant_ore 翠玉矿石",
               "mica_ore 云母矿石"],
         keyMats=["青玉砂 jade_grit", "翠玉 verdant_gem", "灵脉晶 vein_crystal",
                  "云母 mica"],
         liquids=["玉髓浆 jade_ichor（灵液）", "玉蚀酸液 jade_acid（凶液）"],
         structures=["青玉宝塔 jade_pagoda（宝箱）", "青玉村 jade_village",
                     "主世界的玉辉裂隙 jade_rift"],
         mobs=["青玉灵", "玉兔", "玉羊", "地脉龟", "卦石守卫"],
         crops=["玉粒稻 jade_rice → 玉粒/玉粒种子"],
         special="风水增益 ×1.3；龙脉最密集"),
    dict(id="liuyao_realm", zh="六爻界", prefix="yao", color="#444444",
         gate="玄门 yao_gate", key="爻纹引 yao_key",
         time="十二消息卦（复→坤），一昼夜就是一场完整的卦象呼吸",
         theme="平野如盘，一昼夜就是一卦。在这里起卦卦气最真，但一天只有一卦，问多了反噬翻倍。",
         biomes=["hexagram_plain 卦象平原", "oracle_woods 卜骨林",
                 "ink_marsh 玄墨泽", "twilight_plain 曙昏原", "ink_ocean 玄墨海"],
         ores=["yao_ore 爻纹矿石", "ink_iron_ore 玄铁矿石", "onyx_ore 墨玉矿石",
               "oracle_ore 卜骨矿石"],
         keyMats=["爻纹砾 yao_shard", "玄铁 ink_iron", "墨玉 onyx",
                  "卜骨 oracle_bone"],
         liquids=["玄墨水 yao_ink（灵液）", "妖瘴浓雾 yao_miasma（凶液）"],
         structures=["爻坛 yao_altar（宝箱）", "爻纹村 yao_village",
                     "主世界的爻纹裂隙 yao_rift"],
         mobs=["爻狼", "爻蝠", "卦影"],
         crops=["爻纹豆 yao_bean → 爻纹豆荚/爻纹种子"],
         special="卦气最真；每日问卦上限减半、反噬翻倍"),
    dict(id="ziwei_realm", zh="紫微界", prefix="ziwei", color="#9b59b6",
         gate="紫微门 ziwei_gate", key="紫微灵引 ziwei_key",
         time="十二长生（与命理界同源）",
         theme="浮空岛悬在星海里，十四主星各归其位。帝星坐镇的星官之界。",
         biomes=["ziwei_field 紫微原", "ziwei_grove 斗垣林", "ziwei_peaks 北斗崖",
                 "ziwei_marsh 紫微泽", "ziwei_dunes 天机沙", "ziwei_ocean 紫微海"],
         ores=["ziwei_crystal_ore 紫微晶矿", "dou_ore 斗枢矿", "ji_ore 天机砂矿",
               "yuan_ore 紫垣矿"],
         keyMats=["紫微星尘 ziwei_star_dust", "紫微晶 ziwei_crystal",
                  "紫微精 ziwei_essence", "紫微穗 ziwei_grain"],
         liquids=["紫微露 ziwei_ambrosia（灵液）", "星煞玄露 ziwei_bane（凶液）"],
         structures=["紫微观星台 ziwei_observatory（宝箱）", "紫微村 ziwei_village",
                     "主世界的紫微裂隙 ziwei_rift"],
         mobs=["紫微星官", "斗宿兽", "天机狐", "紫垣蛾"],
         crops=["紫微穗 ziwei_wheat → 紫微穗/紫微穗种子"],
         special="紫微帝星之界；村民与灵兽为四小界模板"),
    dict(id="liuren_realm", zh="六壬界", prefix="liuren", color="#1f6fb2",
         gate="壬水门 liuren_gate", key="壬水灵引 liuren_key",
         time="月将加时，四课三传",
         theme="山脊像一道道课式，月将加时，四课分明。大六壬的课式之界。",
         biomes=["liuren_field 壬水原", "liuren_grove 贵人林", "liuren_peaks 螣蛇崖",
                 "liuren_marsh 玄武泽", "liuren_dunes 勾陈沙", "liuren_ocean 玄冥海"],
         ores=["ren_ore 壬水矿", "gui_ore 贵人矿", "she_ore 螣蛇矿",
               "xuan_ore 玄武矿"],
         keyMats=["壬水珠 liuren_crystal", "壬水精 liuren_essence",
                  "壬水稻粒 liuren_grain", "课甲 liuren_shell"],
         liquids=["壬水 liuren_water（灵液）", "玄冥渊水 liuren_abyss（凶液）"],
         structures=["六壬课坛 liuren_hall（宝箱）", "六壬村 liuren_village",
                     "主世界的六壬裂隙 liuren_rift"],
         mobs=["壬水蛟", "贵人鹤", "螣蛇犬", "玄武龟"],
         crops=["壬水稻 liuren_rice → 壬水稻/壬水稻种子"],
         special="水气最盛；六壬起课材料产地"),
    dict(id="qimen_realm", zh="奇门界", prefix="qimen", color="#c0392b",
         gate="八门 qimen_gate", key="八门灵引 qimen_key",
         time="八门随时而转",
         theme="开阔台地铺成九宫，八门随时而转。遁甲布局之界，也是七界中最「烫」的一界。",
         biomes=["qimen_field 八门原", "qimen_grove 九星林", "qimen_peaks 天柱峰",
                 "qimen_marsh 景门泽", "qimen_dunes 遁甲沙", "qimen_ocean 炎海"],
         ores=["men_ore 八门矿", "xing_ore 九星矿", "dun_ore 遁甲矿",
               "jia_ore 甲子矿"],
         keyMats=["八门金 qimen_crystal", "九星精 qimen_essence",
                  "遁甲残片 qimen_shard", "八门豆 qimen_grain"],
         liquids=["遁甲汞 qimen_mercury（灵液）", "灼魂炎浆 qimen_hellfire（凶液）"],
         structures=["奇门局塔 qimen_array_tower（宝箱）", "奇门村 qimen_village",
                     "主世界的奇门裂隙 qimen_rift"],
         mobs=["门卫", "星魁狐", "遁甲鼠", "甲子鸟"],
         crops=["八门豆 qimen_bean → 八门豆/八门豆种子"],
         special="炎海灼魂；八门符箓产地"),
    dict(id="taiyi_realm", zh="太乙界", prefix="taiyi", color="#0e7c7b",
         gate="太乙门 taiyi_gate", key="太乙灵引 taiyi_key",
         time="太乙行宫，不入中五",
         theme="浮空九宫悬在星海里，太乙行宫不入中五。三式之末、国运大势之界。",
         biomes=["taiyi_field 太乙原", "taiyi_grove 九宫林", "taiyi_peaks 太一崖",
                 "taiyi_marsh 五将泽", "taiyi_dunes 十六神沙", "taiyi_ocean 星汉海"],
         ores=["taiyi_ore 太乙晶矿", "wenchang_ore 文昌石矿", "shiji_ore 始击砂矿",
               "zhuke_ore 主客金矿"],
         keyMats=["太乙晶 taiyi_crystal", "太乙精 taiyi_essence",
                  "始击砂 taiyi_shard", "太乙穗粒 taiyi_grain"],
         liquids=["太乙玄泉 taiyi_spring（灵液）", "太乙寒渊 taiyi_frost（凶液）"],
         structures=["太乙观星台 taiyi_tower（宝箱）", "太乙村 taiyi_village",
                     "主世界的太乙裂隙 taiyi_rift"],
         mobs=["（复用紫微界生物）"],
         crops=["太乙穗 taiyi_wheat → 太乙穗粒/太乙穗种"],
         special="第 7 界；太乙神数的独立算法包驱动"),
]

REALM_CARDS = ""
for r in REALMS:
    inner = """
<div class="realm" style="border-left:5px solid %s">
<div class="realm-head"><span class="realm-name">%s</span>
<span class="chip">%s</span><span class="chip">灵引：%s</span></div>
<p>%s</p>
<p><b>时间体系：</b>%s　<b>界域特性：</b>%s</p>
<p><b>群系：</b>%s</p>
<p><b>矿石：</b>%s</p>
<p><b>液体：</b>%s</p>
<p><b>作物：</b>%s</p>
<p><b>结构：</b>%s</p>
<p><b>生物：</b>%s</p>
</div>""" % (
        r["color"], r["zh"], r["id"], r["key"], r["theme"], r["time"],
        r["special"], "、".join(r["biomes"]), "、".join(r["ores"]),
        "、".join(r["liquids"]), "、".join(r["crops"]),
        "、".join(r["structures"]), "、".join(r["mobs"]))
    REALM_CARDS += inner

STRUCTURES = [
    # (名称, id, 所在, 群系标签, 简介)
    ("八卦遗迹", "bagua_ruins", "主世界", "#minecraft:is_overworld",
     "主世界最常见的遗迹，八卦布局的残垣。地下密室藏宝箱（ruin / ruin_vault），开出古卷·山河志、铜钱与界域料。"),
    ("龙脉祭坛", "dragon_altar", "主世界", "#minecraft:is_overworld",
     "五行灵石砌成的祭坛。以龙脉核心碎片在此立契，可占领游走的龙脉。"),
    ("太岁祠", "taisui_shrine", "主世界", "#minecraft:is_overworld",
     "供奉太岁的祠堂。太岁符与太岁丹相关玩法的地标。"),
    ("星辉裂隙", "astral_rift", "主世界", "#minecraft:is_overworld",
     "通往命理界的裂隙，内有星门（astral_gate）。"),
    ("玉辉裂隙", "jade_rift", "主世界", "#minecraft:is_overworld",
     "通往风水界的裂隙，内有玉门（jade_gate）。"),
    ("爻纹裂隙", "yao_rift", "主世界", "#minecraft:is_overworld",
     "通往六爻界的裂隙，内有玄门（yao_gate）。"),
    ("紫微裂隙", "ziwei_rift", "主世界", "#minecraft:is_overworld",
     "通往紫微界的裂隙，内有紫微门（ziwei_gate）。"),
    ("六壬裂隙", "liuren_rift", "主世界", "#minecraft:is_overworld",
     "通往六壬界的裂隙，内有壬水门（liuren_gate）。"),
    ("奇门裂隙", "qimen_rift", "主世界", "#minecraft:is_overworld",
     "通往奇门界的裂隙，内有八门（qimen_gate）。"),
    ("太乙裂隙", "taiyi_rift", "主世界", "#minecraft:is_overworld",
     "通往太乙界的裂隙，内有太乙门（taiyi_gate）。"),
    ("火煞祭坛", "flame_altar", "下界", "#minecraft:is_nether",
     "下界火煞的凝晶之地，出烈焰碎片（flame_fragment）。"),
    ("灵魂祭坛", "soul_altar", "下界", "#minecraft:is_nether",
     "灵魂沙中沉眠的核心之地，出灵魂核心（soul_core）。"),
    ("虚空祭坛", "void_altar", "末地", "#minecraft:is_end",
     "末地虚空碎片（void_shard）的所在，唯有实体之物才能触及；紫颂核心以虚空之力催生。"),
    ("星辉观星台", "astral_observatory", "命理界", "#yishu:astral_field",
     "命理界主结构，顶层宝箱（astral_observatory）。七政四余观星的场所。"),
    ("紫微观星台", "ziwei_observatory", "紫微界", "#yishu:ziwei_field",
     "紫微界主结构，宝箱（ziwei_terrace）。十四主星布局的观星之台。"),
    ("青玉宝塔", "jade_pagoda", "风水界", "#yishu:vein_valley",
     "风水界主结构，宝箱（jade_pagoda）。龙脉汇聚处的镇地之塔。"),
    ("爻坛", "yao_altar", "六爻界", "#yishu:hexagram_plain",
     "六爻界主结构，宝箱（yao_altar）。卦象平原中心的断卦之所。"),
    ("六壬课坛", "liuren_hall", "六壬界", "#yishu:liuren_field",
     "六壬界主结构，宝箱（liuren_altar）。月将加时的课式大殿。"),
    ("奇门局塔", "qimen_array_tower", "奇门界", "#yishu:qimen_field",
     "奇门界主结构，宝箱（qimen_array）。九宫布局的局塔。"),
    ("太乙观星台", "taiyi_tower", "太乙界", "#yishu:taiyi_field",
     "太乙界主结构，宝箱（taiyi_terrace）。太乙行宫的观星高台。"),
    ("星辉村", "astral_village", "命理界", "#yishu:astral_field",
     "命理界村落，村民职业：星命师（astrologer）、蓍草商（yarrow_master）。"),
    ("青玉村", "jade_village", "风水界", "#yishu:vein_valley",
     "风水界村落，村民职业：地理师（geomancer）、相地师（surveyor）。"),
    ("爻纹村", "yao_village", "六爻界", "#yishu:hexagram_plain",
     "六爻界村落，村民职业：卦师（diviner）。"),
    ("紫微村", "ziwei_village", "紫微界", "#yishu:ziwei_field",
     "紫微界村落，村民职业：星官（star_master）、斗数家（fate_master、dou_master）。"),
    ("六壬村", "liuren_village", "六壬界", "#yishu:liuren_field",
     "六壬界村落，村民职业：课师（lesson_master）。"),
    ("奇门村", "qimen_village", "奇门界", "#yishu:qimen_field",
     "奇门界村落，村民职业：阵法家（array_master）、八门师（bamen_master）。"),
    ("太乙村", "taiyi_village", "太乙界", "#yishu:taiyi_field",
     "太乙界村落，村民复用紫微 id。"),
]

STRUCT_ROWS = [[n, "<code>%s</code>" % i, loc, bio, desc]
               for n, i, loc, bio, desc in STRUCTURES]

MECHANICS = [
    ("黄历时钟", "ClockSystem",
     "手持时钟右键直接展开黄历面板；点物品展示框里的时钟同样有效（墙上的挂钟也不会被转歪）。"
     "历法只在服务端计算，面板显示对齐主世界时辰。模组纪年 2100～2900 年，可看大运流年。"),
    ("模组历法", "YishuCalendar",
     "自定历法：一年 12 个月、每 15 日一节气，二十四节气由此驱动。"
     "年月日时四柱八字由真实历法推算（含干支），是所有术数的「时间轴」。"),
    ("每日宜忌", "FengShuiSystems.Almanac",
     "从流日干支推出当日宜忌：宜 2～3 条、忌 2～3 条。"
     "同一种子、同一维度、同一天永远是同一份黄历。"
     "宜：动土兴建、入地采掘、交易买卖、附魔炼器、祭祀祈福、安床落帐、安门立户、"
     "四方远游、征伐战斗、收获作物、繁育牲畜；忌：动土、采掘、交易、安床、安门、"
     "远行、战斗、夜间外出、涉水渡河、近火用炉。"),
    ("动土吉凶", "DayActionHook",
     "黄历落在手上：玩家放置方块的那一刻查当日宜忌——"
     "宜动土则顺，忌动土则招煞（凶液池、雷劈、怪声）。安床、开门同理。"),
    ("风水域", "FengShuiField / TerrainQi",
     "以 3×3 采样、60 刻缓存计算脚下的气：气煞阴阳、五行偏向。"
     "站在生气之地回复更快；怪物也会随地形改变强弱——凶地出凶怪。"
     "/yishu fengshui 随时查看。"),
    ("龙脉", "FengShuiSystems.DragonVein",
     "一条不规则的带状地气，逐年游走。站在龙脉上一切采集皆有加成。"
     "用龙脉核心碎片（dragon_core_shard）在龙脉祭坛立契，即可占领一条龙脉。"
     "/yishu vein 查看方位，寻脉图（vein_map）的墨迹会自己挪向最近的遗迹。"),
    ("太岁", "FengShuiSystems.TaiSui",
     "每年坐镇一个方位。在太岁方位动土会招煞，"
     "立一块石敢当（shi_gandang）就能化解；太岁符/太岁丹提供一年一度的护持。"),
    ("镇物", "GuardianStoneSystem",
     "镇宅石（zhenzhai_stone）：立起后方圆 24 格内的邪祟受镇——迟滞、虚弱。"
     "化煞碑（huasha_stone）：压制地里的煞气。五行灵石块可作为祭坛基座、压制维度煞气。"),
    ("阵盘", "FormationSystem",
     "阵盘右键地面布阵：阵眼记入存档、当日有效（次日天亮阵散）。"
     "八卦阵：阵内抗性+缓慢回血，邪祟入阵迟滞受抑；"
     "北斗阵：阵内夜视+疾行，夜路如白昼；招财阵：阵内幸运 II，收获更丰。"),
    ("梦境", "DreamSystem",
     "睡醒那一刻起梦：每张床、每个世界、每一天只做一场梦，"
     "梦象由「种子+玩家+当日」纯函数推出——重睡多少遍都是那一场，但预兆只有头一次兑现。"
     "吉梦：晨起得吉兆带 buff、记小功德；凶梦：当夜心魔化形（Vex）来劫，一次了结。"),
    ("节气", "SolarTermSystem",
     "二十四节气每 15 日一换，换气时全服播报；一气之内人人得小加持——"
     "立春生发、夏至阳极、冬至阴极。「四立」与「二分二至」是大节点，播报更隆重、加持更强。"),
    ("功德阴德", "MeritSystem",
     "积善之家必有余庆：喂饱牲畜 +2，金苹果引僵尸村民回头 +10，凶梦醒后守本心另记小功。"
     "功德高者得福报（雷劫劈邪、奇遇）；作恶者有余殃。/yishu merit 查看账本。"),
    ("相面招募", "Physiognomy",
     "相面镜照村民面相：面相由村民 UUID 确定——拿什么镜子照、谁来照、照多少遍都是同一个面相。"
     "贵相村民可潜行照镜为其「传名」，获得交易折价（gossip 正面传闻）。"),
    ("反噬", "BacklashSystem",
     "占卜不是雷达：每日问卦有上限，超限后惩罚依次加重（耳鸣、迷途、煞气缠身）。"
     "六爻界内问卦上限减半、反噬翻倍——卦气最真，代价也最真。"),
    ("教程与史册", "TutorialSystem / Chronicle / PlainSpeak",
     "游戏内引导（/yishu tutorial，可 admin 开关）随进度讲述玩法；"
     "Chronicle 记录你的术数生涯；PlainSpeak 把玄学术语翻译成大白话，新人也能看懂卦辞。"),
    ("木头经济", "Round 22–25",
     "21 种模组木完整的原版级体验：木板进 #minecraft:planks（木棍/工作台/箱子/床等 34 配方白拿）；"
     "原木进 #minecraft:logs / #minecraft:logs_that_burn（熔炉烧木炭、可当燃料）；"
     "全部木方块可被火焰点燃蔓延（原木 5/5、板材 20/5、树叶树苗 30/60）；"
     "21 种木船+运输船；栅栏/门/告示牌等全套变体。"),
    ("矿物生成", "Round 21–22",
     "主世界新增模组矿脉（铁矿级概率+）；七界各有独立矿系（紫微晶矿/斗枢矿/壬水矿…共 25 种原矿），"
     "矿山裸露概率已调优（discard_chance_on_air_exposure=0），洞穴里能直接看到矿脉。"),
]

MECH_ROWS = "".join(
    '<div class="mech"><h3>%s <span class="chip mono">%s</span></h3><p>%s</p></div>'
    % (n, c, d) for n, c, d in MECHANICS)

MINOR_SCHOOLS = [
    ("qizheng", "七政四余", "星命 · 观星台与流年星象；值日星宿与流年照命"),
    ("meihua", "梅花易数", "轻量起卦 · 即时占卜，时间起卦、体用生克"),
    ("xiaoliuren", "小六壬", "掌上速断 · 前期快捷占（大安/留连/速喜/赤口/小吉/空亡）"),
    ("zeri", "择日学", "动土 / 安床 / 开门的吉凶判断"),
    ("xiangshu", "相术", "面相 · NPC 鉴定与招募"),
    ("cezi", "姓名测字", "给物品、村庄、角色取名的吉凶词条；笔画起卦与五格同源"),
    ("qianwen", "签诗 · 灵棋经 · 诸葛神数", "抽签解签 · 隐喻提示"),
    ("jiemeng", "周公解梦", "睡后梦境与预兆"),
    ("kanyu", "堪舆四派", "玄空飞星 / 八宅 / 三合 / 峦头"),
    ("zhenpan", "阵盘", "八卦阵 / 九宫阵 / 聚灵阵"),
    ("yinde", "因果功德", "善行、福报与奇遇"),
    ("taiyi", "太乙神数", "三式之三 · 国运与大势"),
]

ORACLE_KINDS = [
    ("紫微斗数 ziwei_chart", "推命之盘。看的是「你是谁」——命身财官、十四主星。"),
    ("大六壬 liuren_plate", "占事之盘。看的是「事怎么走」——月将加时、四课三传。"),
    ("奇门遁甲 qimen_disc", "择时择方之盘。看的是「往哪走」——九宫八门、三奇六仪。"),
]

MOB_ROWS = [
    ("星辉符灵 fate_wisp", "命理界", "敌对", "命理界游荡的星光之灵。"),
    ("星狐 star_fox", "命理界", "被动", "掉落星狐绒（star_fur），微微烫手的柔光。"),
    ("星屑史莱姆 star_slime", "命理界", "敌对", "星尘凝成的小团子。"),
    ("观星者 star_watcher", "命理界", "敌对", "仰望星空的守夜者。"),
    ("青玉灵 jade_spirit", "风水界", "被动", "风水界的灵韵化身。"),
    ("玉兔 jade_rabbit", "风水界", "被动", "掉落玉兔绒（jade_wool），据说能安神。"),
    ("玉羊 jade_goat", "风水界", "被动", "青玉山坡的食草灵兽。"),
    ("地脉龟 vein_turtle", "风水界", "被动", "掉落地脉鳞（vein_scale），一圈一圈都是年轮。"),
    ("卦石守卫 stone_guardian", "遗迹", "敌对", "守护遗迹与祭坛的石像卫士。"),
    ("爻狼 ink_wolf", "六爻界", "被动", "玄墨色的狼，群居。"),
    ("爻蝠 ink_bat", "六爻界", "环境", "掉落爻蝠翼膜（ink_membrane），对着光能看见爻纹。"),
    ("卦影 hex_phantom", "六爻界", "敌对", "卦象化成的影子。"),
    ("紫微星官 ziwei_star", "紫微界", "敌对", "帝星座下的星官。"),
    ("斗宿兽 dou_beast", "紫微界", "被动", "北斗七星曜变的灵兽。"),
    ("天机狐 tianji_fox", "紫微界", "被动", "通晓天机的狐。"),
    ("紫垣蛾 ziwei_moth", "紫微界", "敌对", "绕着星官飞舞的巨蛾。"),
    ("壬水蛟 ren_serpent", "六壬界", "被动", "壬水中的蛟龙。"),
    ("贵人鹤 gui_crane", "六壬界", "被动", "贵人星的仙鹤。"),
    ("螣蛇犬 she_hound", "六壬界", "被动", "螣蛇化形的猎犬。"),
    ("玄武龟 xuan_turtle", "六壬界", "被动", "玄武之相的巨龟。"),
    ("门卫 men_guard", "奇门界", "敌对", "守门的甲士。"),
    ("星魁狐 xing_fox", "奇门界", "被动", "九星之狐。"),
    ("遁甲鼠 dun_rat", "奇门界", "被动", "遁入甲缝的机灵鼠。"),
    ("甲子鸟 jia_bird", "奇门界", "环境", "报甲子时辰的鸟。"),
]

ADV_ROWS = [
    ("易枢 · 天机初现", "root", "获得命盘玉简——你的命数，从这一刻开始记录。"),
    ("掷地有声", "divination", "获得三枚铜钱，六爻问天的开端。"),
    ("寻龙点穴", "compass", "获得风水罗盘，脚下每一块地都开始有话要说。"),
    ("格物观气", "observing", "放置一座观气台，读懂你家的户型。"),
    ("万界之钥", "key", "拿到一把灵引。遗迹宝箱与流浪商人带着它，也能用四份界域料在工台上铸出来。"),
    ("破界之门", "first_gate", "用灵引立起第一扇门——走进去，就是另一个世界。"),
    ("星命之约", "realm_fate", "初次踏入命理维度。"),
    ("龙脉之约", "realm_fengshui", "初次踏入风水维度。"),
    ("卦象之约", "realm_liuyao", "初次踏入六爻维度。"),
    ("紫垣之约", "realm_ziwei", "初次踏入紫微界。"),
    ("月将之约", "realm_liuren", "初次踏入六壬界。"),
    ("八门之约", "realm_qimen", "初次踏入奇门界。"),
    ("太乙之约", "realm_taiyi", "初次踏入太乙界。"),
    ("七界巡游", "realm_all", "七个界域都留下一串脚印。"),
    ("界外奇珍", "realm_bounty", "带回一份异界的矿物。"),
    ("七界灵泉", "realm_brew", "用桶舀起一份异界的液体。"),
    ("深入地宫", "ruin", "找到一处遗迹的地下密室，并活着出来。"),
    ("逆天改命之始", "dragon_core", "获得龙脉核心碎片——命，是可以改的。"),
]

COMMANDS = [
    ("/yishu help", "玩法总览与指引"),
    ("/yishu bazi", "查看自己的八字命盘"),
    ("/yishu fengshui", "查看脚下风水域（气煞阴阳、五行偏向）"),
    ("/yishu almanac", "查看今日黄历宜忌"),
    ("/yishu tutorial", "查看教程进度"),
    ("/yishu professional", "术数生涯档案（史册）"),
    ("/yishu divine <学派>", "起一卦：meihua / xiaoliuren / qianwen / cezi…"),
    ("/yishu oracle <式>", "三式起局：ziwei / liuren / qimen"),
    ("/yishu ruins", "感知最近的遗迹方位"),
    ("/yishu vein", "感知龙脉方位"),
    ("/yishu taisui", "查看今年太岁方位"),
    ("/yishu merit", "查看功德账本"),
    ("/yishu mob", "查看身边生物的卦气"),
    ("/yishu calendar", "查看模组历法（可 admin 调年调日）"),
    ("/yishu travel", "跨维度旅行相关信息"),
    ("/yishu admin tutorial on/off", "开关新手教程"),
    ("/yishu admin limit <n>", "调整每日问卦上限"),
    ("快捷键（默认未绑定）", "打开命盘面板（设置-按键绑定里改）"),
]

# ============================================================ 物品分组

GROUPS = [
    ("核心道具", ["fate_slip", "fate_book", "bronze_coins", "ancient_scroll",
                  "yinyang_stone", "vein_map", "dragon_core_shard",
                  "dragon_breath_core", "chorus_core", "void_shard",
                  "flame_fragment", "soul_core"]),
    ("法器", ["dragon_ruler", "feng_shui_compass", "face_mirror", "bagua_mirror",
              "sundial", "qi_observatory"]),
    ("三式盘", ["ziwei_chart", "liuren_plate", "qimen_disc", "liuren_shell",
                "yarrow_stalks", "ziwei_star_dust", "turtle_shell", "oracle_bone"]),
    ("灵引（七界之钥）", ["astral_key", "jade_key", "yao_key", "ziwei_key",
                          "liuren_key", "qimen_key", "taiyi_key"]),
    ("丹药", ["qi_pill", "calm_pill", "clarity_pill", "swift_pill", "ward_pill",
              "taisui_pill"]),
    ("符箓", ["talisman_paper", "qinglong_talisman", "baihu_talisman",
              "guiren_talisman", "kaimen_talisman", "shengmen_talisman",
              "xiumen_talisman", "fortune_talisman", "ward_talisman",
              "clarity_talisman", "taisui_talisman", "peek_charm"]),
    ("将星令牌", ["qisha_token", "tianfu_token", "ziwei_token"]),
    ("阵盘", ["bagua_array_disc", "beidou_array_disc", "zhaocai_array_disc"]),
    ("五行灵石", ["wood_stone", "fire_stone", "earth_stone", "metal_stone",
                  "water_stone"]),
]

KEY_TABLE = []
for r in REALMS:
    KEY_TABLE.append([r["zh"], "<code>%s</code>" % r["key"].split()[1],
                      r["gate"].split()[0], "、".join(r["keyMats"])])

# 界域材料（除灵引/作物外）
realm_mats = []
for pref, mats in [
        ("命理界", ["astral_dust", "star_sand", "star_crystal", "meteor_iron",
                    "astral_raw", "star_sand_raw", "crystal_raw", "meteor_raw"]),
        ("风水界", ["jade_grit", "verdant_gem", "vein_crystal", "mica",
                    "jade_raw", "verdant_raw", "vein_raw", "mica_raw"]),
        ("六爻界", ["yao_shard", "ink_iron", "onyx", "oracle_bone",
                    "yao_raw", "ink_iron_raw", "onyx_raw", "oracle_raw"]),
        ("紫微界", ["ziwei_star_dust", "ziwei_crystal", "ziwei_essence",
                    "ziwei_crystal_raw"]),
        ("六壬界", ["liuren_crystal", "liuren_essence", "ren_raw"]),
        ("奇门界", ["qimen_crystal", "qimen_essence", "qimen_shard",
                    "men_raw", "xing_raw", "dun_raw", "jia_raw"]),
        ("太乙界", ["taiyi_crystal", "taiyi_essence", "taiyi_shard",
                    "taiyi_raw", "wenchang_raw", "shiji_raw", "zhuke_raw"]),
        ("六壬界紫微补", ["gui_raw", "she_raw", "xuan_raw", "dou_raw_unused"])]:
    for m in mats:
        if m in IT:
            realm_mats.append([pref.replace("紫微补", ""), "<code>%s</code>" % m,
                               IT[m], NOTES.get(m) or ""])

# ============================================================ HTML 骨架

CSS = """
:root{--bg:#f6f4ef;--card:#fff;--ink:#2b2a26;--dim:#8a8577;--line:#e5e0d5;
--accent:#7a5c2e;--accent2:#a4762f;--code-bg:#f0ece2;--side:#2b2a26;--side-ink:#d8d2c4}
*{box-sizing:border-box}
body{margin:0;font-family:"Source Han Serif SC","Noto Serif SC",serif;background:var(--bg);color:var(--ink);line-height:1.75}
.layout{display:flex;min-height:100vh}
nav{width:230px;flex:0 0 230px;background:var(--side);color:var(--side-ink);position:sticky;top:0;height:100vh;overflow-y:auto;padding:18px 14px 40px}
nav .brand{font-size:20px;letter-spacing:2px;color:#fff;border-bottom:1px solid #4a463c;padding-bottom:10px;margin-bottom:12px}
nav .brand small{display:block;font-size:11px;color:#98917e;letter-spacing:1px}
nav a{display:block;color:var(--side-ink);text-decoration:none;font-size:13.5px;padding:4px 8px;border-radius:6px}
nav a:hover{background:#3c392f;color:#fff}
nav .grp{margin:12px 0 4px;font-size:11px;color:#8f8871;letter-spacing:2px}
main{flex:1;max-width:1060px;margin:0 auto;padding:34px 40px 90px}
.hero{background:linear-gradient(135deg,#3a332a,#5a4a2e);color:#f3ecd9;border-radius:16px;padding:34px 38px;margin-bottom:26px}
.hero h1{margin:0 0 6px;font-size:34px;letter-spacing:6px}
.hero p{margin:6px 0;color:#cfc4a6}
.stats{display:flex;gap:10px;flex-wrap:wrap;margin-top:16px}
.stat{background:rgba(255,255,255,.09);border:1px solid rgba(255,255,255,.15);border-radius:10px;padding:8px 14px;font-size:13px}
.stat b{font-size:20px;color:#ffd98a;margin-right:4px}
.card{background:var(--card);border:1px solid var(--line);border-radius:14px;padding:26px 30px;margin-bottom:26px;box-shadow:0 1px 3px rgba(60,50,20,.05)}
.card h2{margin:0 0 8px;font-size:23px;letter-spacing:2px;border-bottom:2px solid var(--line);padding-bottom:10px}
.card h2 .ico{margin-right:8px}
.sec-desc{color:var(--dim);margin-top:0}
h3{font-size:17px;margin:22px 0 8px;color:var(--accent)}
table{border-collapse:collapse;width:100%;font-size:13.5px;margin:12px 0}
th{background:var(--code-bg);text-align:left;padding:8px 10px;border:1px solid var(--line);font-weight:700;white-space:nowrap}
td{padding:7px 10px;border:1px solid var(--line);vertical-align:top}
tr:hover td{background:#faf7ef}
code{font-family:Consolas,Menlo,monospace;font-size:12.5px;background:var(--code-bg);border-radius:5px;padding:1px 6px;color:#5a4325}
.wiki-table.wide{font-size:12.5px}
.chip{display:inline-block;background:#efe8d8;border:1px solid var(--line);border-radius:20px;padding:1px 10px;font-size:12px;margin:0 4px 4px 0;color:var(--accent)}
.mono{font-family:Consolas,monospace}
.dim{color:var(--dim)}
.realm{border:1px solid var(--line);border-radius:12px;padding:16px 20px;margin:14px 0;background:#fffdf7}
.realm-head{display:flex;align-items:center;gap:10px;flex-wrap:wrap}
.realm-name{font-size:19px;font-weight:700}
.mech{border-left:3px solid var(--accent2);padding:2px 0 2px 16px;margin:18px 0}
.mech h3{margin:0 0 6px}
.note{color:var(--dim);font-size:12.5px}
#search{width:100%;padding:9px 14px;border:1px solid var(--line);border-radius:10px;font-size:14px;margin:14px 0;background:#fff;font-family:inherit}
.hidden{display:none!important}
.tip{background:#f3eedd;border:1px solid #e3d8ba;border-radius:10px;padding:10px 16px;font-size:13.5px;margin:12px 0}
.step{display:flex;gap:12px;margin:10px 0;align-items:flex-start}
.step .n{flex:0 0 26px;height:26px;border-radius:50%;background:var(--accent);color:#fff;display:flex;align-items:center;justify-content:center;font-size:14px}
@media(max-width:860px){nav{display:none}main{padding:20px 16px}}
"""

JS = """
const q = document.getElementById('search');
q.addEventListener('input', () => {
  const v = q.value.trim().toLowerCase();
  document.querySelectorAll('.card').forEach(card => {
    let vis = !v;
    if (!vis) {
      const text = card.textContent.toLowerCase();
      if (text.includes(v)) {
        vis = true;
        card.querySelectorAll('tbody tr').forEach(tr => {
          tr.classList.toggle('hidden', v && !tr.textContent.toLowerCase().includes(v));
        });
      }
    }
    card.classList.toggle('hidden', !vis);
    if (!v) card.querySelectorAll('.hidden').forEach(el => el.classList.remove('hidden'));
  });
});
"""

# ============================================================ 组装

def build():
    total_items = len(IT)
    total_blocks = len(BK)
    total_recipes = len([f for f in os.listdir(
        os.path.join(RES, "data", "yishu", "recipe")) if f.endswith(".json")])
    total_structs = len([f for f in os.listdir(
        os.path.join(RES, "data", "yishu", "worldgen", "structure")) if f.endswith(".json")])
    total_biomes = len([f for f in os.listdir(
        os.path.join(RES, "data", "yishu", "worldgen", "biome")) if f.endswith(".json")])

    # nav
    nav_groups = [
        ("入门", [("start", "新手指南"), ("terms", "术语速查")]),
        ("世界", [("realms", "七界维度"), ("structures", "遗迹与结构"), ("ores", "矿物与生成")]),
        ("图鉴", [("blocks", "方块图鉴"), ("woods", "木头体系"), ("items", "物品图鉴"),
                  ("keys", "灵引与界门"), ("crops", "作物与食物"), ("buckets", "液体与桶"),
                  ("boats", "船只"), ("mobs", "生物图鉴")]),
        ("系统", [("oracle", "术数系统"), ("mechanics", "机制详解"), ("villagers", "村民与交易")]),
        ("资料", [("advancements", "成就"), ("commands", "指令参考"), ("stats", "数据一览")]),
    ]
    nav = ['<div class="brand">易枢<small>YISHU · WIKI</small></div>']
    for gname, links in nav_groups:
        nav.append('<div class="grp">%s</div>' % gname)
        for id_, label in links:
            nav.append('<a href="#%s">%s</a>' % (id_, label))
    nav_html = "".join(nav)

    # sections
    S = []
    S.append(sec("🧭", "start", "新手指南", "五步入坑：从捡到玉简，到踏遍七界。", """
<div class="step"><div class="n">1</div><div><b>命盘玉简</b> —— 获得它的那一刻，模组的成就树与你的命数开始记录。按 <span class="chip mono">J</span>（若已绑定）或 <code>/yishu bazi</code> 看自己的八字。</div></div>
<div class="step"><div class="n">2</div><div><b>三枚铜钱 + 空白符纸</b> —— 六爻问天的开端。用 <code>/yishu divine</code> 试起第一卦，注意<b>每日问卦有上限，问多了会反噬</b>。</div></div>
<div class="step"><div class="n">3</div><div><b>风水罗盘 + 观气台</b> —— 罗盘看你脚下的气，观气台读懂整块地。想安家？先看风水，凶地的怪会更强。</div></div>
<div class="step"><div class="n">4</div><div><b>灵引</b> —— 找遗迹宝箱、找流浪商人，或者凑齐「四份界域料 + 阴阳石」在工作台上铸一把。手持灵引<b>对地面右键</b>，会立起一座界门牌坊——穿过去，就是另一个世界（不消耗，可反复用）。</div></div>
<div class="step"><div class="n">5</div><div><b>七界巡游</b> —— 每个界域都有自己的时间体系、矿物、作物、村民与凶液。目标：达成「七界巡游」成就。</div></div>
<div class="tip">💡 提示：主世界地表散落着七座「裂隙」，每座裂隙里就有一扇现成的界门——找不到灵引时，跟着寻脉图的墨迹走。</div>
"""))

    S.append(sec("📖", "terms", "术语速查", "模组内建术语卡（游戏内亦有对应提示）。", table(
        ["术语", "释义"],
        [[TERM_ZH.get(k, k), v]
         for k, v in LANG.items() if k.startswith("yishu.term.")])))

    S.append(sec("🌌", "realms", "七界维度", "七个手工设计的界域 + 三套主世界之外的独特时间体系。", REALM_CARDS + """
<div class="tip">⏳ 主世界按十二时辰流转；下界「十二火时」、末地「虚空脉」也各有其历。
命理界走十二长生、风水界走十二山水、六爻界走十二消息卦——同一份黄历，在不同界域有不同结果。</div>"""))

    S.append(sec("🏛️", "structures", "遗迹与结构", "共 %d 座结构：主世界遗迹与裂隙、下界/末地祭坛、七界主殿与村落。" % total_structs,
                 table(["名称", "id", "所在", "群系标签", "介绍"], STRUCT_ROWS)))

    S.append(sec("⛏️", "ores", "矿物与生成", "七界独立矿系 + 主世界模组矿脉，共 25 种原矿。", """
<p>主世界：模组矿脉以铁/铜级概率散布（裸露概率已调优，洞穴可见）。七界：每一界的群系有各自的矿物分布（山地偏多、沙丘偏少）。</p>
""" + table(["原矿", "物品 id", "熔炼产物", "图鉴"],
            [[IT.get(r, ""), "<code>%s</code>" % r, IT.get(r.replace("_raw", ""), r.replace("_raw", "")),
              NOTES.get(r) or ""]
             for r in sorted(IT) if r.endswith("_raw") and "bucket" not in r])))

    # 方块图鉴：杂项 + 按界域分组
    realm_block_html = ""
    for r in REALMS:
        p = r["prefix"]
        names = [n for n in sorted(BK)
                 if n.startswith(p + "_") and not n.startswith(p + "_wood")
                 and n not in (p + "_dunes", p + "_field", p + "_grove",
                               p + "_marsh", p + "_peaks", p + "_ocean")]
        realm_block_html += "<h3>%s</h3>" % r["zh"] + table(
            ["方块 id", "名称"], block_rows(names))
    misc_names = ["altar", "bagua_platform", "gua_board", "taiji_diagram",
                  "qi_observatory", "censer", "ruin_core", "dragon_core_block",
                  "zhenzhai_stone", "huasha_stone", "shi_gandang",
                  "yinyang_stele", "wood_stone_block", "fire_stone_block",
                  "earth_stone_block", "metal_stone_block", "water_stone_block"]
    S.append(sec("🧱", "blocks", "方块图鉴",
                 "共 %d 个注册方块：界域建材、矿石、木系、功能方块。功能性方块右键可读说明。" % total_blocks,
                 "<h3>功能与特殊方块</h3>" + table(["方块 id", "名称"], block_rows(misc_names))
                 + realm_block_html))

    S.append(sec("🪵", "woods", "木头体系", "21 种树 × 15 类方块 + 船只，完整对接原版配方/燃烧/燃料体系。", wood_table() + """
<div class="tip">🔥 原木可烧木炭（300 刻/个）、可当燃料；木板进 <code>#minecraft:planks</code>（工作台/木棍/箱子/床等 34 配方通用）；
全部木方块可燃（原木 5/5、板材 20/5、树叶树苗 30/60）；树叶随机掉树苗，精准采集/剪刀掉树叶本体。</div>"""))

    # 物品图鉴
    item_html = ""
    for gname, names in GROUPS:
        item_html += "<h3>%s</h3>" % gname + table(
            ["物品 id", "名称", "图鉴"], item_rows(names))
    S.append(sec("🎒", "items", "物品图鉴", "工具、法器、丹药、符箓——图鉴文案取自游戏内。", item_html))

    S.append(sec("🗝️", "keys", "灵引与界门",
                 "灵引是打开七界的钥匙：对地面右键立起界门牌坊（2宽3高门洞+柱+梁），可反复使用。",
                 table(["界域", "灵引", "界门", "铸造材料（任选其一）"], KEY_TABLE) + """
<div class="tip">🚪 界门规则：「此岸」永远指回人间——已经在该界域里穿门，回主世界；否则进入该界域。
坐标对齐，落点自动取目标维度的地表。玩家、生物、掉落物都能过。</div>"""))

    S.append(sec("🌾", "crops", "作物与食物", "七界作物 + 丹药。", crop_table()))

    S.append(sec("🧪", "buckets", "液体与桶",
                 "七界各有「灵液」与「凶液」两种液体：灵液有用、凶液有害（灼烧/中毒/迟滞）。", bucket_table()))

    S.append(sec("🛶", "boats", "船只", "21 种木船 + 21 种运输船（带木箱，可运货），复用原版船机制与水花渲染。", boat_table()))

    S.append(sec("🐺", "mobs", "生物图鉴", "24 种模组生物（太乙界复用紫微界生物），刷怪蛋在创造栏可取。",
                 table(["生物", "出没", "习性", "说明"], [list(r) for r in MOB_ROWS]) + egg_table()))

    S.append(sec("🔮", "oracle", "术数系统",
                 "「三式」+ 命理书 + 12 门小术数，全部服务端纯函数计算——同一天问同一件事，答案永远一致。", """
<h3>三式（大三门）</h3>
""" + table(["术式", "道具", "定位"],
            [[k.split()[0], "<code>%s</code>" % k.split()[1], d] for k, d in ORACLE_KINDS]) + """
<p>三式盘由对应的界域材料合成；起卦走 <code>/yishu oracle ziwei|liuren|qimen</code>。</p>
<h3>12 门小术数（MinorSchools）</h3>
""" + table(["参数", "门派", "定位"],
            [["<code>%s</code>" % k, z, n] for k, z, n in MINOR_SCHOOLS]) + """
<div class="tip">⚠️ <b>反噬机制</b>：每日问卦有上限，超限后惩罚依次加重；六爻界内上限减半、反噬翻倍。
占卜是工具，不是雷达。</div>"""))

    S.append(sec("⚙️", "mechanics", "机制详解", "17 个核心系统，全部注释在源码里，这里按玩法整理。", MECH_ROWS))

    S.append(sec("🧑‍🌾", "villagers", "村民与交易",
                 "12 种新村民职业（全部走原版 gossip/交易数据包通道），每种职业 3 级交易树。",
                 table(["职业", "英文 id", "所在村落"],
                       [["星命师", "astrologer", "星辉村"],
                        ["蓍草商", "yarrow_master", "星辉村"],
                        ["地理师", "geomancer", "青玉村"],
                        ["相地师", "surveyor", "青玉村"],
                        ["卦师", "diviner", "爻纹村"],
                        ["星官", "star_master", "紫微村"],
                        ["斗数家", "fate_master / dou_master", "紫微村"],
                        ["课师", "lesson_master", "六壬村"],
                        ["阵法家", "array_master", "奇门村"],
                        ["八门师", "bamen_master", "奇门村"],
                        ["（复用）", "太乙村村民复用紫微职业", "太乙村"],
                        ["原版职业扩展", "butcher / cartographer / cleric / farmer 等新增交易", "各村"]])))

    S.append(sec("🏆", "advancements", "成就", "18 项成就，从「天机初现」到「七界巡游」。",
                 table(["成就", "id", "描述"], [[a, "<code>%s</code>" % b, c] for a, b, c in ADV_ROWS])))

    S.append(sec("⌨️", "commands", "指令参考", "所有玩法都有指令兜底——忘了怎么玩就 /yishu help。",
                 table(["指令", "说明"], [[("<code>%s</code>" % a) if a.startswith("/") else a, b]
                                         for a, b in COMMANDS])))

    S.append(sec("📊", "stats", "数据一览", "以本 Wiki 生成时的资源数据为准。", table(
        ["统计项", "数量"],
        [["注册物品", total_items], ["注册方块", total_blocks],
         ["合成配方", total_recipes], ["结构", total_structs],
         ["群系（七界）", total_biomes], ["维度", "7（另有主世界/下界/末地的时间体系）"],
         ["生物", 24], ["术数门派", "3 式 + 12 小术数"],
         ["语言文件键", len(LANG)]])))

    html = """<!DOCTYPE html>
<html lang="zh-CN">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width,initial-scale=1">
<title>易枢 · 百科 Wiki</title>
<style>%s</style>
</head>
<body>
<div class="layout">
<nav>%s</nav>
<main>
<div class="hero">
<h1>易枢 · 百科</h1>
<p>一款把中国术数文化做成可玩体系的 Minecraft 模组 —— 紫微、六壬、奇门、太乙、六爻、风水、命理，七界一盘。</p>
<div class="stats">
<span class="stat"><b>%d</b>物品</span>
<span class="stat"><b>%d</b>方块</span>
<span class="stat"><b>%d</b>配方</span>
<span class="stat"><b>7</b>界域</span>
<span class="stat"><b>%d</b>结构</span>
<span class="stat"><b>%d</b>群系</span>
<span class="stat"><b>24</b>生物</span>
<span class="stat"><b>18</b>成就</span>
</div>
</div>
<input id="search" type="search" placeholder="🔍 搜索：输入名称 / id / 关键词，过滤当前章节内容…">
%s
<p class="dim" style="text-align:center">易枢 Yishu · Fabric 26.2 · 本 Wiki 由 tools/generate_wiki.py 从模组数据自动生成</p>
</main>
</div>
<script>%s</script>
</body>
</html>""" % (CSS, nav_html, total_items, total_blocks, total_recipes,
              total_structs, total_biomes, "".join(S), JS)

    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    open(OUT, "w", encoding="utf-8").write(html)
    print("written:", OUT, len(html), "bytes")


if __name__ == "__main__":
    build()
