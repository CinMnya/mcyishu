#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""易枢百科 · Markdown 版生成器。

复用 generate_wiki.py 中的手写内容与 lang 数据，产出 docs/yishu_wiki.md。
幂等可重跑。用法：python tools/generate_wiki_md.py
"""

import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)

import generate_wiki as W  # noqa: E402

ROOT = os.path.dirname(HERE)
OUT = os.path.join(ROOT, "docs", "yishu_wiki.md")

L = []


def h1(t):
    L.append("# %s\n" % t)


def h2(t):
    L.append("\n## %s\n" % t)


def h3(t):
    L.append("\n### %s\n" % t)


def p(t=""):
    L.append(t + "\n")


def table(headers, rows):
    L.append("| " + " | ".join(headers) + " |")
    L.append("|" + "|".join(["---"] * len(headers)) + "|")
    for r in rows:
        cells = [str(c).replace("\n", " ") for c in r]
        L.append("| " + " | ".join(cells) + " |")
    L.append("")


def code_list(pairs):
    table(["id", "名称"], [["`%s`" % a, b] for a, b in pairs])


# ============================================================ 组装

IT, BK, NOTES = W.IT, W.BK, W.NOTES
LANG = W.LANG
TERM_ZH = W.TERM_ZH

h1("易枢 · 百科 Wiki")
p("> 一款把中国术数文化做成可玩体系的 Minecraft 模组 —— "
  "紫微、六壬、奇门、太乙、六爻、风水、命理，七界一盘。")
p("> Fabric 26.2 · 本文档由 `tools/generate_wiki_md.py` 从模组数据自动生成，"
  "内容与游戏内数据一致。")

h2("目录")
toc = ["新手指南", "术语速查", "七界维度", "遗迹与结构", "矿物与生成",
       "方块图鉴", "木头体系", "物品图鉴", "灵引与界门", "作物与食物",
       "液体与桶", "船只", "生物图鉴", "术数系统", "机制详解", "村民与交易",
       "成就", "指令参考", "数据一览"]
L.append("  ".join("%d. [%s](#%d-%s)" % (i + 1, t, i + 1, t) for i, t in enumerate(toc)))
L.append("")

# ---- 新手指南
h2("1. 新手指南")
p("五步入坑：从捡到玉简，到踏遍七界。")
p("1. **命盘玉简** —— 获得它的那一刻，成就树与你的命数开始记录。"
  "按 J（若已绑定）或 `/yishu bazi` 看自己的八字。")
p("2. **三枚铜钱 + 空白符纸** —— 六爻问天的开端。用 `/yishu divine` 试起第一卦，"
  "注意**每日问卦有上限，问多了会反噬**。")
p("3. **风水罗盘 + 观气台** —— 罗盘看脚下的气，观气台读懂整块地。"
  "想安家先看风水：凶地的怪会更强。")
p("4. **灵引** —— 遗迹宝箱、流浪商人，或「四份界域料 + 阴阳石」在工作台铸造。"
  "手持灵引**对地面右键**，立起一座界门牌坊——穿过去就是另一个世界（不消耗，可反复用）。")
p("5. **七界巡游** —— 每个界域有自己的时间体系、矿物、作物、村民与凶液。"
  "目标：达成「七界巡游」成就。")
p("> 💡 主世界地表散落着七座「裂隙」，每座裂隙里就有一扇现成的界门——"
  "找不到灵引时，跟着寻脉图的墨迹走。")

# ---- 术语
h2("2. 术语速查")
p("模组内建术语卡（游戏内亦有对应提示）。")
table(["术语", "释义"],
      [[TERM_ZH.get(k, k), v]
       for k, v in LANG.items() if k.startswith("yishu.term.")])

# ---- 七界
h2("3. 七界维度")
p("七个手工设计的界域 + 三套主世界之外的独特时间体系。")
for r in W.REALMS:
    h3("%s（%s）" % (r["zh"], r["id"]))
    p(r["theme"])
    p("- **时间体系**：%s")
    L[-1] = "- **时间体系**：%s　**界域特性**：%s" % (r["time"], r["special"])
    L.append("")
    p("- **灵引**：`%s`　**界门**：%s" % (r["key"].split()[1], r["gate"]))
    p("- **群系**：%s" % "、".join(r["biomes"]))
    p("- **矿石**：%s" % "、".join(r["ores"]))
    p("- **液体**：%s" % "、".join(r["liquids"]))
    p("- **作物**：%s" % "、".join(r["crops"]))
    p("- **结构**：%s" % "、".join(r["structures"]))
    p("- **生物**：%s" % "、".join(r["mobs"]))
p("> ⏳ 主世界按十二时辰流转；下界「十二火时」、末地「虚空脉」也各有其历。"
  "命理界走十二长生、风水界走十二山水、六爻界走十二消息卦——"
  "同一份黄历，在不同界域有不同结果。")

# ---- 结构
h2("4. 遗迹与结构")
p("共 %d 座结构：主世界遗迹与裂隙、下界/末地祭坛、七界主殿与村落。" % 27)
table(["名称", "id", "所在", "群系标签", "介绍"], W.STRUCT_ROWS)

# ---- 矿物
h2("5. 矿物与生成")
p("主世界：模组矿脉以铁/铜级概率散布（裸露概率已调优，洞穴可见）。"
  "七界：每一界的群系有各自的矿物分布（山地偏多、沙丘偏少），共 25 种原矿。")
table(["原矿", "物品 id", "熔炼产物", "图鉴"],
      [[IT.get(r, ""), "`%s`" % r, IT.get(r.replace("_raw", ""), r.replace("_raw", "")),
        NOTES.get(r) or ""]
       for r in sorted(IT) if r.endswith("_raw") and "bucket" not in r])

# ---- 方块
h2("6. 方块图鉴")
p("共 %d 个注册方块：界域建材、矿石、木系、功能方块。功能性方块右键可读说明。" % len(BK))
h3("功能与特殊方块")
code_list([(n, BK[n]) for n in W.__dict__.get("misc_names", []) if n in BK] if False else
          [(n, BK[n]) for n in ["altar", "bagua_platform", "gua_board", "taiji_diagram",
                                "qi_observatory", "censer", "ruin_core", "dragon_core_block",
                                "zhenzhai_stone", "huasha_stone", "shi_gandang",
                                "yinyang_stele", "wood_stone_block", "fire_stone_block",
                                "earth_stone_block", "metal_stone_block", "water_stone_block"]
           if n in BK])
import re  # noqa: E402
TERM_ZH = W.TERM_ZH
for r in W.REALMS:
    pr = r["prefix"]
    names = [n for n in sorted(BK)
             if n.startswith(pr + "_") and not n.startswith(pr + "_wood")
             and n not in (pr + "_dunes", pr + "_field", pr + "_grove",
                           pr + "_marsh", pr + "_peaks", pr + "_ocean")]
    h3(r["zh"] + "（界域建材与矿石）")
    code_list([(n, BK[n]) for n in names])

# ---- 木头
h2("7. 木头体系")
p("21 种树 × 15 类方块 + 船只，完整对接原版配方/燃烧/燃料体系。")
headers = ["树种"] + [k[1] for k in W.WOOD_BLOCK_KINDS]
rows = []
for s in W.WOOD_SPECIES:
    cells = [W.WOOD_ZH[s]]
    for suf, _z in W.WOOD_BLOCK_KINDS:
        key = s + suf
        cells.append(BK.get(key, "—"))
    rows.append(cells)
table(headers, rows)
p("> 🔥 原木可烧木炭（300 刻/个）、可当燃料；木板进 `#minecraft:planks`"
  "（工作台/木棍/箱子/床等 34 配方通用）；全部木方块可燃"
  "（原木 5/5、板材 20/5、树叶树苗 30/60）；树叶随机掉树苗，精准采集/剪刀掉树叶本体。")

# ---- 物品
h2("8. 物品图鉴")
p("工具、法器、丹药、符箓——图鉴文案取自游戏内。")
for gname, names in W.GROUPS:
    h3(gname)
    table(["物品 id", "名称", "图鉴"], W.item_rows(names))

# ---- 灵引
h2("9. 灵引与界门")
p("灵引是打开七界的钥匙：对地面右键立起界门牌坊（2宽3高门洞+柱+梁），可反复使用。")
table(["界域", "灵引", "界门", "铸造材料（任选其一）"], W.KEY_TABLE)
p("> 🚪 界门规则：「此岸」永远指回人间——已经在该界域里穿门，回主世界；"
  "否则进入该界域。坐标对齐，落点自动取目标维度的地表。玩家、生物、掉落物都能过。")

# ---- 作物
h2("10. 作物与食物")
table(["物品 id", "名称", "图鉴"],
      [["`%s`" % n, IT[n], NOTES.get(n) or ""]
       for n in sorted(IT) if n.endswith("_seeds") or n.endswith("_grain") or n in ("yao_pod",)])

# ---- 液体
h2("11. 液体与桶")
p("七界各有「灵液」与「凶液」两种液体：灵液有用、凶液有害（灼烧/中毒/迟滞）。")
table(["物品 id", "名称", "图鉴"],
      [["`%s`" % n, IT[n], NOTES.get(n) or ""]
       for n in sorted(IT) if n.endswith("_bucket")])

# ---- 船
h2("12. 船只")
p("21 种木船 + 21 种运输船（带木箱，可运货），复用原版船机制与水花渲染。")
table(["普通船 id", "运输船 id", "名称"],
      [["`%s_wood_boat`" % s, "`%s_wood_chest_boat`" % s, W.WOOD_ZH[s] + "船（/运输船）"]
       for s in W.WOOD_SPECIES])

# ---- 生物
h2("13. 生物图鉴")
p("24 种模组生物（太乙界复用紫微界生物），刷怪蛋在创造栏可取。")
table(["生物", "出没", "习性", "说明"], [list(r) for r in W.MOB_ROWS])
h3("刷怪蛋")
code_list([(n, IT[n]) for n in sorted(IT) if n.endswith("_spawn_egg")])

# ---- 术数
h2("14. 术数系统")
p("「三式」+ 命理书 + 12 门小术数，全部服务端纯函数计算——"
  "同一天问同一件事，答案永远一致。")
h3("三式（大三门）")
table(["术式", "道具", "定位"],
      [[k.split()[0], "`%s`" % k.split()[1], d] for k, d in W.ORACLE_KINDS])
p("三式盘由对应的界域材料合成；起卦走 `/yishu oracle ziwei|liuren|qimen`。")
h3("12 门小术数（MinorSchools）")
table(["参数", "门派", "定位"],
      [["`%s`" % k, z, n] for k, z, n in W.MINOR_SCHOOLS])
p("> ⚠️ **反噬机制**：每日问卦有上限，超限后惩罚依次加重；六爻界内上限减半、反噬翻倍。"
  "占卜是工具，不是雷达。")

# ---- 机制
h2("15. 机制详解")
p("17 个核心系统，全部注释在源码里，这里按玩法整理。")
for n, c, d in W.MECHANICS:
    L.append("- **%s**（`%s`）：%s" % (n, c, d))
L.append("")

# ---- 村民
h2("16. 村民与交易")
p("12 种新村民职业（全部走原版 gossip/交易数据包通道），每种职业 3 级交易树。")
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
       ["原版职业扩展", "butcher / cartographer / cleric / farmer 等新增交易", "各村"]])

# ---- 成就
h2("17. 成就")
p("18 项成就，从「天机初现」到「七界巡游」。")
table(["成就", "id", "描述"], [[a, "`%s`" % b, c] for a, b, c in W.ADV_ROWS])

# ---- 指令
h2("18. 指令参考")
p("所有玩法都有指令兜底——忘了怎么玩就 /yishu help。")
table(["指令", "说明"],
      [["`%s`" % a if a.startswith("/") else a, b] for a, b in W.COMMANDS])

# ---- 数据
h2("19. 数据一览")
total_recipes = len([f for f in os.listdir(
    os.path.join(ROOT, "src", "main", "resources", "data", "yishu", "recipe"))
    if f.endswith(".json")])
total_structs = len([f for f in os.listdir(
    os.path.join(ROOT, "src", "main", "resources", "data", "yishu", "worldgen", "structure"))
    if f.endswith(".json")])
total_biomes = len([f for f in os.listdir(
    os.path.join(ROOT, "src", "main", "resources", "data", "yishu", "worldgen", "biome"))
    if f.endswith(".json")])
table(["统计项", "数量"],
      [["注册物品", len(IT)], ["注册方块", len(BK)], ["合成配方", total_recipes],
       ["结构", total_structs], ["群系（七界）", total_biomes],
       ["维度", "7（另有主世界/下界/末地的时间体系）"],
       ["生物", 24], ["术数门派", "3 式 + 12 小术数"], ["语言文件键", len(LANG)]])

os.makedirs(os.path.dirname(OUT), exist_ok=True)
open(OUT, "w", encoding="utf-8").write("\n".join(L))
print("written:", OUT, len("\n".join(L)), "bytes")
