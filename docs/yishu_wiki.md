# 易枢 · 百科 Wiki

> 一款把中国术数文化做成可玩体系的 Minecraft 模组 —— 紫微、六壬、奇门、太乙、六爻、风水、命理，七界一盘。

> Fabric 26.2 · 本文档由 `tools/generate_wiki_md.py` 从模组数据自动生成，内容与游戏内数据一致。

## 目录

1. [新手指南](#1-新手指南)  2. [术语速查](#2-术语速查)  3. [七界维度](#3-七界维度)  4. [遗迹与结构](#4-遗迹与结构)  5. [矿物与生成](#5-矿物与生成)  6. [方块图鉴](#6-方块图鉴)  7. [木头体系](#7-木头体系)  8. [物品图鉴](#8-物品图鉴)  9. [灵引与界门](#9-灵引与界门)  10. [作物与食物](#10-作物与食物)  11. [液体与桶](#11-液体与桶)  12. [船只](#12-船只)  13. [生物图鉴](#13-生物图鉴)  14. [术数系统](#14-术数系统)  15. [机制详解](#15-机制详解)  16. [村民与交易](#16-村民与交易)  17. [成就](#17-成就)  18. [指令参考](#18-指令参考)  19. [数据一览](#19-数据一览)

## 1. 新手指南

五步入坑：从捡到玉简，到踏遍七界。

1. **命盘玉简** —— 获得它的那一刻，成就树与你的命数开始记录。按 J（若已绑定）或 `/yishu bazi` 看自己的八字。
2. **三枚铜钱 + 空白符纸** —— 六爻问天的开端。用 `/yishu divine` 试起第一卦，注意**每日问卦有上限，问多了会反噬**。
3. **风水罗盘 + 观气台** —— 罗盘看脚下的气，观气台读懂整块地。想安家先看风水：凶地的怪会更强。
4. **灵引** —— 遗迹宝箱、流浪商人，或「四份界域料 + 阴阳石」在工作台铸造。手持灵引**对地面右键**，立起一座界门牌坊——穿过去就是另一个世界（不消耗，可反复用）。
5. **七界巡游** —— 每个界域有自己的时间体系、矿物、作物、村民与凶液。目标：达成「七界巡游」成就。

> 💡 主世界地表散落着七座「裂隙」，每座裂隙里就有一扇现成的界门——找不到灵引时，跟着寻脉图的墨迹走。

## 2. 术语速查

模组内建术语卡（游戏内亦有对应提示）。

| 术语 | 释义                       |
| -- | ------------------------ |
| 五行 | 五行：木火土金水，相生相克，是整套系统的共同语言 |
| 八字 | 八字：年月日时四柱，共八个字，日干为「我」    |
| 六爻 | 六爻：以六次掷钱成卦，配六亲六神断事       |
| 风水 | 风水：以气煞阴阳与五行偏向评价一块地       |
| 龙脉 | 龙脉：每年游走的带状地气，可占领         |
| 太岁 | 太岁：每年坐镇一方，动土招煞，石敢当可化     |

## 3. 七界维度

七个手工设计的界域 + 三套主世界之外的独特时间体系。

### 命理界（fate_realm）

浮空的原野之上，命盘正在缓缓转动。天干效力全开，问卦看命格外清晰。

- **时间体系**：十二长生（长生→养），一昼夜走完一轮命气循环　**界域特性**：十二长生时辰；天干效力 ×1.2
- **灵引**：`astral_key`　**界门**：星门 astral_gate
- **群系**：astral_field 星野、astral_grove 星林、astral_peaks 星峰、astral_marsh 星泽、astral_dunes 星丘
- **矿石**：astral_ore 星辉矿石、star_sand_ore 星砂矿石、crystal_ore 星晶矿石、meteor_ore 陨星矿石
- **液体**：星辉露 astral_dew（灵液）、星陨熔渣 astral_slag（凶液）
- **作物**：星辉穗 astral_wheat → 星辉穗实/星辉种子
- **结构**：星辉观星台 astral_observatory（宝箱）、星辉村 astral_village、主世界的星辉裂隙 astral_rift
- **生物**：星辉符灵、星狐、星屑史莱姆、观星者

### 风水界（fengshui_realm）

幽谷里的每一道山脊都是龙的行踪。龙脉密布、气场浓稠，风水增益放大，煞气也一样。

- **时间体系**：十二山水（起脉→归藏），一昼夜走完一条龙脉的起结　**界域特性**：风水增益 ×1.3；龙脉最密集
- **灵引**：`jade_key`　**界门**：玉门 jade_gate
- **群系**：vein_valley 灵脉谷、jade_ridge 青玉岭、jade_shore 青玉岸、cloud_forest 云雾林、wind_plateau 风台原、stone_steppe 石砾原、jade_ocean 青玉海
- **矿石**：jade_ore 青玉矿石、vein_ore 灵脉矿石、verdant_ore 翠玉矿石、mica_ore 云母矿石
- **液体**：玉髓浆 jade_ichor（灵液）、玉蚀酸液 jade_acid（凶液）
- **作物**：玉粒稻 jade_rice → 玉粒/玉粒种子
- **结构**：青玉宝塔 jade_pagoda（宝箱）、青玉村 jade_village、主世界的玉辉裂隙 jade_rift
- **生物**：青玉灵、玉兔、玉羊、地脉龟、卦石守卫

### 六爻界（liuyao_realm）

平野如盘，一昼夜就是一卦。在这里起卦卦气最真，但一天只有一卦，问多了反噬翻倍。

- **时间体系**：十二消息卦（复→坤），一昼夜就是一场完整的卦象呼吸　**界域特性**：卦气最真；每日问卦上限减半、反噬翻倍
- **灵引**：`yao_key`　**界门**：玄门 yao_gate
- **群系**：hexagram_plain 卦象平原、oracle_woods 卜骨林、ink_marsh 玄墨泽、twilight_plain 曙昏原、ink_ocean 玄墨海
- **矿石**：yao_ore 爻纹矿石、ink_iron_ore 玄铁矿石、onyx_ore 墨玉矿石、oracle_ore 卜骨矿石
- **液体**：玄墨水 yao_ink（灵液）、妖瘴浓雾 yao_miasma（凶液）
- **作物**：爻纹豆 yao_bean → 爻纹豆荚/爻纹种子
- **结构**：爻坛 yao_altar（宝箱）、爻纹村 yao_village、主世界的爻纹裂隙 yao_rift
- **生物**：爻狼、爻蝠、卦影

### 紫微界（ziwei_realm）

浮空岛悬在星海里，十四主星各归其位。帝星坐镇的星官之界。

- **时间体系**：十二长生（与命理界同源）　**界域特性**：紫微帝星之界；村民与灵兽为四小界模板
- **灵引**：`ziwei_key`　**界门**：紫微门 ziwei_gate
- **群系**：ziwei_field 紫微原、ziwei_grove 斗垣林、ziwei_peaks 北斗崖、ziwei_marsh 紫微泽、ziwei_dunes 天机沙、ziwei_ocean 紫微海
- **矿石**：ziwei_crystal_ore 紫微晶矿、dou_ore 斗枢矿、ji_ore 天机砂矿、yuan_ore 紫垣矿
- **液体**：紫微露 ziwei_ambrosia（灵液）、星煞玄露 ziwei_bane（凶液）
- **作物**：紫微穗 ziwei_wheat → 紫微穗/紫微穗种子
- **结构**：紫微观星台 ziwei_observatory（宝箱）、紫微村 ziwei_village、主世界的紫微裂隙 ziwei_rift
- **生物**：紫微星官、斗宿兽、天机狐、紫垣蛾

### 六壬界（liuren_realm）

山脊像一道道课式，月将加时，四课分明。大六壬的课式之界。

- **时间体系**：月将加时，四课三传　**界域特性**：水气最盛；六壬起课材料产地
- **灵引**：`liuren_key`　**界门**：壬水门 liuren_gate
- **群系**：liuren_field 壬水原、liuren_grove 贵人林、liuren_peaks 螣蛇崖、liuren_marsh 玄武泽、liuren_dunes 勾陈沙、liuren_ocean 玄冥海
- **矿石**：ren_ore 壬水矿、gui_ore 贵人矿、she_ore 螣蛇矿、xuan_ore 玄武矿
- **液体**：壬水 liuren_water（灵液）、玄冥渊水 liuren_abyss（凶液）
- **作物**：壬水稻 liuren_rice → 壬水稻/壬水稻种子
- **结构**：壬水课坛 liuren_hall（宝箱）、六壬村 liuren_village、主世界的六壬裂隙 liuren_rift
- **生物**：壬水蛟、贵人鹤、螣蛇犬、玄武龟

### 奇门界（qimen_realm）

开阔台地铺成九宫，八门随时而转。遁甲布局之界，也是七界中最「烫」的一界。

- **时间体系**：八门随时而转　**界域特性**：炎海灼魂；八门符箓产地
- **灵引**：`qimen_key`　**界门**：八门 qimen_gate
- **群系**：qimen_field 八门原、qimen_grove 九星林、qimen_peaks 天柱峰、qimen_marsh 景门泽、qimen_dunes 遁甲沙、qimen_ocean 炎海
- **矿石**：men_ore 八门矿、xing_ore 九星矿、dun_ore 遁甲矿、jia_ore 甲子矿
- **液体**：遁甲汞 qimen_mercury（灵液）、灼魂炎浆 qimen_hellfire（凶液）
- **作物**：八门豆 qimen_bean → 八门豆/八门豆种子
- **结构**：奇门局塔 qimen_array_tower（宝箱）、奇门村 qimen_village、主世界的奇门裂隙 qimen_rift
- **生物**：门卫、星魁狐、遁甲鼠、甲子鸟

### 太乙界（taiyi_realm）

浮空九宫悬在星海里，太乙行宫不入中五。三式之末、国运大势之界。

- **时间体系**：太乙行宫，不入中五　**界域特性**：第 7 界；太乙神数的独立算法包驱动
- **灵引**：`taiyi_key`　**界门**：太乙门 taiyi_gate
- **群系**：taiyi_field 太乙原、taiyi_grove 九宫林、taiyi_peaks 太一崖、taiyi_marsh 五将泽、taiyi_dunes 十六神沙、taiyi_ocean 星汉海
- **矿石**：taiyi_ore 太乙晶矿、wenchang_ore 文昌石矿、shiji_ore 始击砂矿、zhuke_ore 主客金矿
- **液体**：太乙玄泉 taiyi_spring（灵液）、太乙寒渊 taiyi_frost（凶液）
- **作物**：太乙穗 taiyi_wheat → 太乙穗粒/太乙穗种
- **结构**：太乙观星台 taiyi_tower（宝箱）、太乙村 taiyi_village、主世界的太乙裂隙 taiyi_rift
- **生物**：（复用紫微界生物）

> ⏳ 主世界按十二时辰流转；下界「十二火时」、末地「虚空脉」也各有其历。命理界走十二长生、风水界走十二山水、六爻界走十二消息卦——同一份黄历，在不同界域有不同结果。

## 4. 遗迹与结构

共 27 座结构：主世界遗迹与裂隙、下界/末地祭坛、七界主殿与村落。

| 名称    | id                              | 所在  | 群系标签                    | 介绍                                                            |
| ----- | ------------------------------- | --- | ----------------------- | ------------------------------------------------------------- |
| 八卦遗迹  | <code>bagua_ruins</code>        | 主世界 | #minecraft:is_overworld | 主世界最常见的遗迹，八卦布局的残垣。地下密室藏宝箱（ruin / ruin_vault），开出古卷·山河志、铜钱与界域料。 |
| 龙脉祭坛  | <code>dragon_altar</code>       | 主世界 | #minecraft:is_overworld | 五行灵石砌成的祭坛。以龙脉核心碎片在此立契，可占领游走的龙脉。                               |
| 太岁祠   | <code>taisui_shrine</code>      | 主世界 | #minecraft:is_overworld | 供奉太岁的祠堂。太岁符与太岁丹相关玩法的地标。                                       |
| 星辉裂隙  | <code>astral_rift</code>        | 主世界 | #minecraft:is_overworld | 通往命理界的裂隙，内有星门（astral_gate）。                                   |
| 玉辉裂隙  | <code>jade_rift</code>          | 主世界 | #minecraft:is_overworld | 通往风水界的裂隙，内有玉门（jade_gate）。                                     |
| 爻纹裂隙  | <code>yao_rift</code>           | 主世界 | #minecraft:is_overworld | 通往六爻界的裂隙，内有玄门（yao_gate）。                                      |
| 紫微裂隙  | <code>ziwei_rift</code>         | 主世界 | #minecraft:is_overworld | 通往紫微界的裂隙，内有紫微门（ziwei_gate）。                                   |
| 六壬裂隙  | <code>liuren_rift</code>        | 主世界 | #minecraft:is_overworld | 通往六壬界的裂隙，内有壬水门（liuren_gate）。                                  |
| 奇门裂隙  | <code>qimen_rift</code>         | 主世界 | #minecraft:is_overworld | 通往奇门界的裂隙，内有八门（qimen_gate）。                                    |
| 太乙裂隙  | <code>taiyi_rift</code>         | 主世界 | #minecraft:is_overworld | 通往太乙界的裂隙，内有太乙门（taiyi_gate）。                                   |
| 火煞祭坛  | <code>flame_altar</code>        | 下界  | #minecraft:is_nether    | 下界火煞的凝晶之地，出烈焰碎片（flame_fragment）。                              |
| 灵魂祭坛  | <code>soul_altar</code>         | 下界  | #minecraft:is_nether    | 灵魂沙中沉眠的核心之地，出灵魂核心（soul_core）。                                 |
| 虚空祭坛  | <code>void_altar</code>         | 末地  | #minecraft:is_end       | 末地虚空碎片（void_shard）的所在，唯有实体之物才能触及；紫颂核心以虚空之力催生。                 |
| 星辉观星台 | <code>astral_observatory</code> | 命理界 | #yishu:astral_field     | 命理界主结构，顶层宝箱（astral_observatory）。七政四余观星的场所。                    |
| 紫微观星台 | <code>ziwei_observatory</code>  | 紫微界 | #yishu:ziwei_field      | 紫微界主结构，宝箱（ziwei_terrace）。十四主星布局的观星之台。                         |
| 青玉宝塔  | <code>jade_pagoda</code>        | 风水界 | #yishu:vein_valley      | 风水界主结构，宝箱（jade_pagoda）。龙脉汇聚处的镇地之塔。                            |
| 爻坛    | <code>yao_altar</code>          | 六爻界 | #yishu:hexagram_plain   | 六爻界主结构，宝箱（yao_altar）。卦象平原中心的断卦之所。                             |
| 六壬课坛  | <code>liuren_hall</code>        | 六壬界 | #yishu:liuren_field     | 六壬界主结构，宝箱（liuren_altar）。月将加时的课式大殿。                            |
| 奇门局塔  | <code>qimen_array_tower</code>  | 奇门界 | #yishu:qimen_field      | 奇门界主结构，宝箱（qimen_array）。九宫布局的局塔。                               |
| 太乙观星台 | <code>taiyi_tower</code>        | 太乙界 | #yishu:taiyi_field      | 太乙界主结构，宝箱（taiyi_terrace）。太乙行宫的观星高台。                           |
| 星辉村   | <code>astral_village</code>     | 命理界 | #yishu:astral_field     | 命理界村落，村民职业：星命师（astrologer）、蓍草商（yarrow_master）。                |
| 青玉村   | <code>jade_village</code>       | 风水界 | #yishu:vein_valley      | 风水界村落，村民职业：地理师（geomancer）、相地师（surveyor）。                      |
| 爻纹村   | <code>yao_village</code>        | 六爻界 | #yishu:hexagram_plain   | 六爻界村落，村民职业：卦师（diviner）。                                       |
| 紫微村   | <code>ziwei_village</code>      | 紫微界 | #yishu:ziwei_field      | 紫微界村落，村民职业：星官（star_master）、斗数家（fate_master、dou_master）。       |
| 六壬村   | <code>liuren_village</code>     | 六壬界 | #yishu:liuren_field     | 六壬界村落，村民职业：课师（lesson_master）。                                 |
| 奇门村   | <code>qimen_village</code>      | 奇门界 | #yishu:qimen_field      | 奇门界村落，村民职业：阵法家（array_master）、八门师（bamen_master）。               |
| 太乙村   | <code>taiyi_village</code>      | 太乙界 | #yishu:taiyi_field      | 太乙界村落，村民复用紫微 id。                                              |

## 5. 矿物与生成

主世界：模组矿脉以铁/铜级概率散布（裸露概率已调优，洞穴可见）。七界：每一界的群系有各自的矿物分布（山地偏多、沙丘偏少），共 25 种原矿。

| 原矿   | 物品 id               | 熔炼产物     | 图鉴                   |
| ---- | ------------------- | -------- | -------------------- |
| 星辉原矿 | `astral_raw`        | astral   | 沉甸甸的一块原矿，回炉炼一炼才知道成色。 |
| 星晶原矿 | `crystal_raw`       | crystal  | 沉甸甸的一块原矿，回炉炼一炼才知道成色。 |
| 遁甲原矿 | `dun_raw`           | dun      | 沉甸甸的一块原矿，回炉炼一炼才知道成色。 |
| 贵人原矿 | `gui_raw`           | gui      | 沉甸甸的一块原矿，回炉炼一炼才知道成色。 |
| 玄铁原矿 | `ink_iron_raw`      | 玄铁       | 沉甸甸的一块原矿，回炉炼一炼才知道成色。 |
| 青玉原矿 | `jade_raw`          | jade     | 沉甸甸的一块原矿，回炉炼一炼才知道成色。 |
| 甲子原矿 | `jia_raw`           | jia      | 沉甸甸的一块原矿，回炉炼一炼才知道成色。 |
| 八门原矿 | `men_raw`           | men      | 沉甸甸的一块原矿，回炉炼一炼才知道成色。 |
| 陨星原矿 | `meteor_raw`        | meteor   | 沉甸甸的一块原矿，回炉炼一炼才知道成色。 |
| 云母原矿 | `mica_raw`          | 云母       | 沉甸甸的一块原矿，回炉炼一炼才知道成色。 |
| 墨玉原矿 | `onyx_raw`          | 墨玉       | 沉甸甸的一块原矿，回炉炼一炼才知道成色。 |
| 卜骨原矿 | `oracle_raw`        | oracle   | 沉甸甸的一块原矿，回炉炼一炼才知道成色。 |
| 壬水原矿 | `ren_raw`           | ren      | 沉甸甸的一块原矿，回炉炼一炼才知道成色。 |
| 螣蛇原矿 | `she_raw`           | she      | 沉甸甸的一块原矿，回炉炼一炼才知道成色。 |
| 始击原矿 | `shiji_raw`         | shiji    | 沉甸甸的一块原矿，回炉炼一炼才知道成色。 |
| 星砂原矿 | `star_sand_raw`     | 星砂       | 沉甸甸的一块原矿，回炉炼一炼才知道成色。 |
| 太乙原矿 | `taiyi_raw`         | taiyi    | 沉甸甸的一块原矿，回炉炼一炼才知道成色。 |
| 灵脉原矿 | `vein_raw`          | vein     | 沉甸甸的一块原矿，回炉炼一炼才知道成色。 |
| 翠玉原矿 | `verdant_raw`       | verdant  | 沉甸甸的一块原矿，回炉炼一炼才知道成色。 |
| 文昌原矿 | `wenchang_raw`      | wenchang | 沉甸甸的一块原矿，回炉炼一炼才知道成色。 |
| 九星原矿 | `xing_raw`          | xing     | 沉甸甸的一块原矿，回炉炼一炼才知道成色。 |
| 玄武原矿 | `xuan_raw`          | xuan     | 沉甸甸的一块原矿，回炉炼一炼才知道成色。 |
| 爻纹原矿 | `yao_raw`           | yao      | 沉甸甸的一块原矿，回炉炼一炼才知道成色。 |
| 主客原矿 | `zhuke_raw`         | zhuke    | 沉甸甸的一块原矿，回炉炼一炼才知道成色。 |
| 紫微原矿 | `ziwei_crystal_raw` | 紫微晶      | 沉甸甸的一块原矿，回炉炼一炼才知道成色。 |


## 6. 方块图鉴

共 398 个注册方块：界域建材、矿石、木系、功能方块。功能性方块右键可读说明。

### 功能与特殊方块

| id                  | 名称   |
| ------------------- | ---- |
| `altar`             | 祭坛   |
| `bagua_platform`    | 八卦台  |
| `gua_board`         | 卦盘   |
| `taiji_diagram`     | 太极图  |
| `qi_observatory`    | 观气台  |
| `censer`            | 香炉   |
| `ruin_core`         | 遗迹核心 |
| `dragon_core_block` | 龙脉核心 |
| `zhenzhai_stone`    | 镇宅石  |
| `huasha_stone`      | 化煞碑  |
| `shi_gandang`       | 石敢当  |
| `yinyang_stele`     | 阴阳界碑 |
| `wood_stone_block`  | 木灵石块 |
| `fire_stone_block`  | 火灵石块 |
| `earth_stone_block` | 土灵石块 |
| `metal_stone_block` | 金灵石块 |
| `water_stone_block` | 水灵石块 |

### 命理界（界域建材与矿石）

| id              | 名称   |
| --------------- | ---- |
| `astral_bloom`  | 星辉花  |
| `astral_bricks` | 星辉砖  |
| `astral_dew`    | 星辉露  |
| `astral_gate`   | 星门   |
| `astral_grass`  | 星纹草  |
| `astral_lamp`   | 星辉灯  |
| `astral_ore`    | 星辉矿石 |
| `astral_pillar` | 星辉柱  |
| `astral_slab`   | 星辉台阶 |
| `astral_slag`   | 星陨熔渣 |
| `astral_stairs` | 星辉楼梯 |
| `astral_stone`  | 星辉石  |
| `astral_wheat`  | 星辉穗  |

### 风水界（界域建材与矿石）

| id            | 名称   |
| ------------- | ---- |
| `jade_acid`   | 玉蚀酸液 |
| `jade_bricks` | 青玉砖  |
| `jade_gate`   | 玉门   |
| `jade_grass`  | 玉须草  |
| `jade_ichor`  | 玉髓浆  |
| `jade_lamp`   | 青玉灯  |
| `jade_lily`   | 青玉兰  |
| `jade_ore`    | 青玉矿石 |
| `jade_pillar` | 青玉柱  |
| `jade_rice`   | 玉粒稻  |
| `jade_slab`   | 青玉台阶 |
| `jade_stairs` | 青玉楼梯 |
| `jade_stone`  | 青玉石  |

### 六爻界（界域建材与矿石）

| id           | 名称   |
| ------------ | ---- |
| `yao_bean`   | 爻纹豆  |
| `yao_bloom`  | 爻纹花  |
| `yao_bricks` | 爻纹砖  |
| `yao_gate`   | 玄门   |
| `yao_grass`  | 爻草   |
| `yao_ink`    | 玄墨水  |
| `yao_lamp`   | 爻纹灯  |
| `yao_miasma` | 妖瘴浓雾 |
| `yao_ore`    | 爻纹矿石 |
| `yao_pillar` | 爻纹柱  |
| `yao_slab`   | 爻纹台阶 |
| `yao_stairs` | 爻纹楼梯 |
| `yao_stone`  | 爻纹石  |

### 紫微界（界域建材与矿石）

| id                  | 名称   |
| ------------------- | ---- |
| `ziwei_ambrosia`    | 紫微露  |
| `ziwei_bane`        | 星煞玄露 |
| `ziwei_bloom`       | 紫微花  |
| `ziwei_bricks`      | 紫微砖  |
| `ziwei_crystal_ore` | 紫微晶矿 |
| `ziwei_gate`        | 紫微门  |
| `ziwei_grass`       | 紫微草  |
| `ziwei_lamp`        | 紫微灯  |
| `ziwei_stone`       | 紫微石  |
| `ziwei_terrace`     | 紫微星台 |
| `ziwei_wheat`       | 紫微穗  |

### 六壬界（界域建材与矿石）

| id              | 名称   |
| --------------- | ---- |
| `liuren_abyss`  | 玄冥渊水 |
| `liuren_altar`  | 六壬式坛 |
| `liuren_bricks` | 壬水砖  |
| `liuren_gate`   | 壬水门  |
| `liuren_grass`  | 壬水草  |
| `liuren_lamp`   | 壬水灯  |
| `liuren_lily`   | 壬水百合 |
| `liuren_rice`   | 壬水稻  |
| `liuren_stone`  | 壬水石  |
| `liuren_water`  | 壬水   |

### 奇门界（界域建材与矿石）

| id               | 名称   |
| ---------------- | ---- |
| `qimen_array`    | 奇门局阵 |
| `qimen_bean`     | 八门豆  |
| `qimen_bloom`    | 八门花  |
| `qimen_bricks`   | 遁甲砖  |
| `qimen_gate`     | 八门   |
| `qimen_grass`    | 八门草  |
| `qimen_hellfire` | 灼魂炎浆 |
| `qimen_lamp`     | 遁甲灯  |
| `qimen_mercury`  | 遁甲汞  |
| `qimen_stone`    | 遁甲石  |

### 太乙界（界域建材与矿石）

| id              | 名称   |
| --------------- | ---- |
| `taiyi_bloom`   | 太乙花  |
| `taiyi_bricks`  | 太乙砖  |
| `taiyi_frost`   | 太乙寒渊 |
| `taiyi_gate`    | 太乙门  |
| `taiyi_grass`   | 太乙草  |
| `taiyi_lamp`    | 太乙灯  |
| `taiyi_ore`     | 太乙晶矿 |
| `taiyi_spring`  | 太乙玄泉 |
| `taiyi_stone`   | 太乙石  |
| `taiyi_terrace` | 太乙星台 |
| `taiyi_wheat`   | 太乙穗  |

## 7. 木头体系

21 种树 × 15 类方块 + 船只，完整对接原版配方/燃烧/燃料体系。

| 树种 | 原木 | 木头 | 树叶 | 树苗 | 木板 | 台阶   | 楼梯   | 栅栏 | 栅栏门 | 门 | 活板门 | 按钮 | 压力板 | 立式告示牌 | 挂墙告示牌 |
| -- | -- | -- | -- | -- | -- | ---- | ---- | -- | --- | - | --- | -- | --- | ----- | ----- |
| 星辉 | —  | —  | —  | —  | —  | 星辉台阶 | 星辉楼梯 | —  | —   | — | —   | —  | —   | —     | —     |
| 陨枝 | —  | —  | —  | —  | —  | —    | —    | —  | —   | — | —   | —  | —   | —     | —     |
| 月华 | —  | —  | —  | —  | —  | —    | —    | —  | —   | — | —   | —  | —   | —     | —     |
| 青玉 | —  | —  | —  | —  | —  | 青玉台阶 | 青玉楼梯 | —  | —   | — | —   | —  | —   | —     | —     |
| 云松 | —  | —  | —  | —  | —  | —    | —    | —  | —   | — | —   | —  | —   | —     | —     |
| 灵桃 | —  | —  | —  | —  | —  | —    | —    | —  | —   | — | —   | —  | —   | —     | —     |
| 爻纹 | —  | —  | —  | —  | —  | —    | —    | —  | —   | — | —   | —  | —   | —     | —     |
| 玄墨 | —  | —  | —  | —  | —  | —    | —    | —  | —   | — | —   | —  | —   | —     | —     |
| 幽玄 | —  | —  | —  | —  | —  | —    | —    | —  | —   | — | —   | —  | —   | —     | —     |
| 紫微 | —  | —  | —  | —  | —  | —    | —    | —  | —   | — | —   | —  | —   | —     | —     |
| 斗枢 | —  | —  | —  | —  | —  | —    | —    | —  | —   | — | —   | —  | —   | —     | —     |
| 天机 | —  | —  | —  | —  | —  | —    | —    | —  | —   | — | —   | —  | —   | —     | —     |
| 壬水 | —  | —  | —  | —  | —  | —    | —    | —  | —   | — | —   | —  | —   | —     | —     |
| 贵人 | —  | —  | —  | —  | —  | —    | —    | —  | —   | — | —   | —  | —   | —     | —     |
| 螣蛇 | —  | —  | —  | —  | —  | —    | —    | —  | —   | — | —   | —  | —   | —     | —     |
| 八门 | —  | —  | —  | —  | —  | —    | —    | —  | —   | — | —   | —  | —   | —     | —     |
| 九星 | —  | —  | —  | —  | —  | —    | —    | —  | —   | — | —   | —  | —   | —     | —     |
| 太乙 | —  | —  | —  | —  | —  | —    | —    | —  | —   | — | —   | —  | —   | —     | —     |
| 文昌 | —  | —  | —  | —  | —  | —    | —    | —  | —   | — | —   | —  | —   | —     | —     |
| 计神 | —  | —  | —  | —  | —  | —    | —    | —  | —   | — | —   | —  | —   | —     | —     |
| 遁甲 | —  | —  | —  | —  | —  | —    | —    | —  | —   | — | —   | —  | —   | —     | —     |

> 🔥 原木可烧木炭（300 刻/个）、可当燃料；木板进 `#minecraft:planks`（工作台/木棍/箱子/床等 34 配方通用）；全部木方块可燃（原木 5/5、板材 20/5、树叶树苗 30/60）；树叶随机掉树苗，精准采集/剪刀掉树叶本体。

## 8. 物品图鉴

工具、法器、丹药、符箓——图鉴文案取自游戏内。

### 核心道具

| 物品 id                           | 名称     | 图鉴                      |
| ------------------------------- | ------ | ----------------------- |
| <code>fate_slip</code>          | 命盘玉简   |                         |
| <code>fate_book</code>          | 命理书    | 记录着一张命盘的竹纸书，可放在讲台上供人阅读。 |
| <code>bronze_coins</code>       | 三枚铜钱   |                         |
| <code>ancient_scroll</code>     | 古卷·山河志 | 遗迹里开出的残卷，展开可读《山河志》的下一卷。 |
| <code>yinyang_stone</code>      | 阴阳石    |                         |
| <code>vein_map</code>           | 寻脉图    | 摊开来，墨迹会自己挪向最近的遗迹。       |
| <code>dragon_core_shard</code>  | 龙脉核心碎片 | 龙脉凝结之物，可用于立契占领龙脉。       |
| <code>dragon_breath_core</code> | 龙气核心   | 末影龙陨落时留下的气息。            |
| <code>chorus_core</code>        | 紫颂核心   | 紫颂的种子，虚空之力方能唤其生长。       |
| <code>void_shard</code>         | 虚空碎片   | 末地虚空的碎片，唯有实体之物才能触及。     |
| <code>flame_fragment</code>     | 烈焰碎片   | 下界火煞的凝晶，压制之后才肯现身。       |
| <code>soul_core</code>          | 灵魂核心   | 灵魂沙中沉眠的核心，阳气可令其显形。      |

### 法器

| 物品 id                          | 名称   | 图鉴                     |
| ------------------------------ | ---- | ---------------------- |
| <code>dragon_ruler</code>      | 寻龙尺  |                        |
| <code>feng_shui_compass</code> | 风水罗盘 |                        |
| <code>face_mirror</code>       | 相面镜  | 照人面相。贵相可潜行照镜为其传名，交易折价。 |
| <code>bagua_mirror</code>      | 八卦镜  | 举镜一照，方圆十六格内的活物都会显形。    |
| <code>sundial</code>           | 日晷   |                        |

### 三式盘

| 物品 id                        | 名称   | 图鉴                     |
| ---------------------------- | ---- | ---------------------- |
| <code>ziwei_chart</code>     | 紫微命盘 |                        |
| <code>liuren_plate</code>    | 六壬式盘 |                        |
| <code>qimen_disc</code>      | 奇门局盘 |                        |
| <code>liuren_shell</code>    | 课甲   | 起课用的甲片，一片只够起一课。        |
| <code>yarrow_stalks</code>   | 蓍草   |                        |
| <code>ziwei_star_dust</code> | 紫微星尘 | 星辉矿石筛出的细粉，紫微盘就以它为骨。    |
| <code>turtle_shell</code>    | 龟甲   |                        |
| <code>oracle_bone</code>     | 卜骨   | 六爻界的卜骨。四片加一方阴阳石，可铸爻纹引。 |

### 灵引（七界之钥）

| 物品 id                   | 名称   | 图鉴 |
| ----------------------- | ---- | -- |
| <code>astral_key</code> | 星枢引  |    |
| <code>jade_key</code>   | 玉髓引  |    |
| <code>yao_key</code>    | 爻纹引  |    |
| <code>ziwei_key</code>  | 紫微灵引 |    |
| <code>liuren_key</code> | 壬水灵引 |    |
| <code>qimen_key</code>  | 八门灵引 |    |
| <code>taiyi_key</code>  | 太乙灵引 |    |

### 丹药

| 物品 id                     | 名称  | 图鉴                 |
| ------------------------- | --- | ------------------ |
| <code>qi_pill</code>      | 归元丹 | 以蓍草引星辉尘炼成，服下伤口自收。  |
| <code>calm_pill</code>    | 定神丹 | 心一定，外邪就难近身。        |
| <code>clarity_pill</code> | 明目丹 | 薄如蝉翼的一片，夜路也看得分明。   |
| <code>swift_pill</code>   | 疾行丹 | 脚下生风，跑起来像被谁推着走。    |
| <code>ward_pill</code>    | 辟邪丹 | 辟火气的丹药，岩浆边上也敢站一会儿。 |
| <code>taisui_pill</code>  | 太岁丹 | 太岁符入炉，一年只该吃一颗。     |

### 符箓

| 物品 id                          | 名称   | 图鉴                   |
| ------------------------------ | ---- | -------------------- |
| <code>talisman_paper</code>    | 空白符纸 | 裁好的黄纸，能承接符力。         |
| <code>qinglong_talisman</code> | 青龙符  | 青龙之符。风从背后起，一步当两步。    |
| <code>baihu_talisman</code>    | 白虎符  | 白虎之符。杀气上了手，做什么都快而狠。  |
| <code>guiren_talisman</code>   | 贵人符  | 贵人之符。走投无路时，总有人恰好路过。  |
| <code>kaimen_talisman</code>   | 开门符  | 开门之符。眼前忽然亮了，连运气都松了松。 |
| <code>shengmen_talisman</code> | 生门符  | 生门之符。伤口自己在合，饿意也退了。   |
| <code>xiumen_talisman</code>   | 休门符  | 休门之符。什么都不做，就是最好的做。   |
| <code>fortune_talisman</code>  | 转运符  |                      |
| <code>ward_talisman</code>     | 护身符  |                      |
| <code>clarity_talisman</code>  | 明目符  |                      |
| <code>taisui_talisman</code>   | 太岁符  |                      |
| <code>peek_charm</code>        | 窥命符  |                      |

### 将星令牌

| 物品 id                     | 名称  | 图鉴                         |
| ------------------------- | --- | -------------------------- |
| <code>qisha_token</code>  | 七杀令 | 七杀将星之令。手会自己握紧，脚下也不再犹豫。     |
| <code>tianfu_token</code> | 天府令 | 天府库星之令。不求暴富，只求仓里永远有余。      |
| <code>ziwei_token</code>  | 紫微令 | 紫微帝星之令。撕开的一瞬，身上像多了一层看不见的甲。 |

### 阵盘

| 物品 id                           | 名称   | 图鉴                         |
| ------------------------------- | ---- | -------------------------- |
| <code>bagua_array_disc</code>   | 八卦阵盘 | 右键地面布八卦阵：阵内抗气回血，邪祟受抑，天明阵散。 |
| <code>beidou_array_disc</code>  | 北斗阵盘 | 右键地面布北斗阵：阵内夜视疾行，夜路如白昼。     |
| <code>zhaocai_array_disc</code> | 招财阵盘 | 右键地面布招财阵：阵内幸运 II，收获更丰。     |

### 五行灵石

| 物品 id                    | 名称  | 图鉴 |
| ------------------------ | --- | -- |
| <code>wood_stone</code>  | 木灵石 |    |
| <code>fire_stone</code>  | 火灵石 |    |
| <code>earth_stone</code> | 土灵石 |    |
| <code>metal_stone</code> | 金灵石 |    |
| <code>water_stone</code> | 水灵石 |    |

## 9. 灵引与界门

灵引是打开七界的钥匙：对地面右键立起界门牌坊（2宽3高门洞+柱+梁），可反复使用。

| 界域  | 灵引                      | 界门  | 铸造材料（任选其一）                                                               |
| --- | ----------------------- | --- | ------------------------------------------------------------------------ |
| 命理界 | <code>astral_key</code> | 星门  | 星辉尘 astral_dust、星砂 star_sand、星晶 star_crystal、陨铁 meteor_iron              |
| 风水界 | <code>jade_key</code>   | 玉门  | 青玉砂 jade_grit、翠玉 verdant_gem、灵脉晶 vein_crystal、云母 mica                    |
| 六爻界 | <code>yao_key</code>    | 玄门  | 爻纹砾 yao_shard、玄铁 ink_iron、墨玉 onyx、卜骨 oracle_bone                         |
| 紫微界 | <code>ziwei_key</code>  | 紫微门 | 紫微星尘 ziwei_star_dust、紫微晶 ziwei_crystal、紫微精 ziwei_essence、紫微穗 ziwei_grain |
| 六壬界 | <code>liuren_key</code> | 壬水门 | 壬水珠 liuren_crystal、壬水精 liuren_essence、壬水稻粒 liuren_grain、课甲 liuren_shell  |
| 奇门界 | <code>qimen_key</code>  | 八门  | 八门金 qimen_crystal、九星精 qimen_essence、遁甲残片 qimen_shard、八门豆 qimen_grain     |
| 太乙界 | <code>taiyi_key</code>  | 太乙门 | 太乙晶 taiyi_crystal、太乙精 taiyi_essence、始击砂 taiyi_shard、太乙穗粒 taiyi_grain     |

> 🚪 界门规则：「此岸」永远指回人间——已经在该界域里穿门，回主世界；否则进入该界域。坐标对齐，落点自动取目标维度的地表。玩家、生物、掉落物都能过。

## 10. 作物与食物

| 物品 id          | 名称    | 图鉴                          |
| -------------- | ----- | --------------------------- |
| `astral_grain` | 星辉穗实  | 星辉穗打下来的谷粒，嚼着有股甜味。           |
| `astral_seeds` | 星辉种子  | 种在耕地上，会长出一株吸星光的小麦。          |
| `jade_grain`   | 玉粒    | 一粒一粒像小玉珠，管饱。                |
| `jade_seeds`   | 玉粒种子  | 种在耕地上，稻穗会分出玉色的小粒。           |
| `liuren_grain` | 壬水稻   | 六壬界水田的稻粒。四捧加一方阴阳石，也能铸壬水灵引。  |
| `liuren_seeds` | 壬水稻种子 |                             |
| `qimen_grain`  | 八门豆   | 奇门界田里的豆子。四把加一方阴阳石，也能铸八门灵引。  |
| `qimen_seeds`  | 八门豆种子 |                             |
| `taiyi_grain`  | 太乙穗粒  |                             |
| `taiyi_seeds`  | 太乙穗种  |                             |
| `yao_pod`      | 爻纹豆荚  | 掰开豆荚，断纹在光下会动。               |
| `yao_seeds`    | 爻纹种子  | 种在耕地上，豆荚上有一道天生的断纹。          |
| `ziwei_grain`  | 紫微穗   | 紫微界田里收的穗子。四束加一方阴阳石，也能铸紫微灵引。 |
| `ziwei_seeds`  | 紫微穗种子 |                             |

## 11. 液体与桶

七界各有「灵液」与「凶液」两种液体：灵液有用、凶液有害（灼烧/中毒/迟滞）。

| 物品 id                   | 名称    | 图鉴 |
| ----------------------- | ----- | -- |
| `astral_dew_bucket`     | 星辉露桶  |    |
| `astral_slag_bucket`    | 星陨熔渣桶 |    |
| `jade_acid_bucket`      | 玉蚀酸液桶 |    |
| `jade_ichor_bucket`     | 玉髓浆桶  |    |
| `liuren_abyss_bucket`   | 玄冥渊水桶 |    |
| `liuren_water_bucket`   | 壬水桶   |    |
| `qimen_hellfire_bucket` | 灼魂炎浆桶 |    |
| `qimen_mercury_bucket`  | 遁甲汞桶  |    |
| `taiyi_frost_bucket`    | 太乙寒渊桶 |    |
| `taiyi_spring_bucket`   | 太乙玄泉桶 |    |
| `yao_ink_bucket`        | 玄墨水桶  |    |
| `yao_miasma_bucket`     | 妖瘴浓雾桶 |    |
| `ziwei_ambrosia_bucket` | 紫微露桶  |    |
| `ziwei_bane_bucket`     | 星煞玄露桶 |    |


## 12. 船只

21 种木船 + 21 种运输船（带木箱，可运货），复用原版船机制与水花渲染。

| 普通船 id               | 运输船 id                     | 名称        |
| -------------------- | -------------------------- | --------- |
| `astral_wood_boat`   | `astral_wood_chest_boat`   | 星辉船（/运输船） |
| `meteor_wood_boat`   | `meteor_wood_chest_boat`   | 陨枝船（/运输船） |
| `moon_wood_boat`     | `moon_wood_chest_boat`     | 月华船（/运输船） |
| `jade_wood_boat`     | `jade_wood_chest_boat`     | 青玉船（/运输船） |
| `cloud_wood_boat`    | `cloud_wood_chest_boat`    | 云松船（/运输船） |
| `peach_wood_boat`    | `peach_wood_chest_boat`    | 灵桃船（/运输船） |
| `hexa_wood_boat`     | `hexa_wood_chest_boat`     | 爻纹船（/运输船） |
| `ink_wood_boat`      | `ink_wood_chest_boat`      | 玄墨船（/运输船） |
| `mystery_wood_boat`  | `mystery_wood_chest_boat`  | 幽玄船（/运输船） |
| `ziwei_wood_boat`    | `ziwei_wood_chest_boat`    | 紫微船（/运输船） |
| `dou_wood_boat`      | `dou_wood_chest_boat`      | 斗枢船（/运输船） |
| `tianji_wood_boat`   | `tianji_wood_chest_boat`   | 天机船（/运输船） |
| `ren_wood_boat`      | `ren_wood_chest_boat`      | 壬水船（/运输船） |
| `gui_wood_boat`      | `gui_wood_chest_boat`      | 贵人船（/运输船） |
| `she_wood_boat`      | `she_wood_chest_boat`      | 螣蛇船（/运输船） |
| `men_wood_boat`      | `men_wood_chest_boat`      | 八门船（/运输船） |
| `xing_wood_boat`     | `xing_wood_chest_boat`     | 九星船（/运输船） |
| `taiyi_wood_boat`    | `taiyi_wood_chest_boat`    | 太乙船（/运输船） |
| `wenchang_wood_boat` | `wenchang_wood_chest_boat` | 文昌船（/运输船） |
| `jishen_wood_boat`   | `jishen_wood_chest_boat`   | 计神船（/运输船） |
| `dun_wood_boat`      | `dun_wood_chest_boat`      | 遁甲船（/运输船） |

## 13. 生物图鉴

24 种模组生物（太乙界复用紫微界生物），刷怪蛋在创造栏可取。

| 生物                  | 出没  | 习性 | 说明                             |
| ------------------- | --- | -- | ------------------------------ |
| 星辉符灵 fate_wisp      | 命理界 | 敌对 | 命理界游荡的星光之灵。                    |
| 星狐 star_fox         | 命理界 | 被动 | 掉落星狐绒（star_fur），微微烫手的柔光。       |
| 星屑史莱姆 star_slime    | 命理界 | 敌对 | 星尘凝成的小团子。                      |
| 观星者 star_watcher    | 命理界 | 敌对 | 仰望星空的守夜者。                      |
| 青玉灵 jade_spirit     | 风水界 | 被动 | 风水界的灵韵化身。                      |
| 玉兔 jade_rabbit      | 风水界 | 被动 | 掉落玉兔绒（jade_wool），据说能安神。        |
| 玉羊 jade_goat        | 风水界 | 被动 | 青玉山坡的食草灵兽。                     |
| 地脉龟 vein_turtle     | 风水界 | 被动 | 掉落地脉鳞（vein_scale），一圈一圈都是年轮。    |
| 卦石守卫 stone_guardian | 遗迹  | 敌对 | 守护遗迹与祭坛的石像卫士。                  |
| 爻狼 ink_wolf         | 六爻界 | 被动 | 玄墨色的狼，群居。                      |
| 爻蝠 ink_bat          | 六爻界 | 环境 | 掉落爻蝠翼膜（ink_membrane），对着光能看见爻纹。 |
| 卦影 hex_phantom      | 六爻界 | 敌对 | 卦象化成的影子。                       |
| 紫微星官 ziwei_star     | 紫微界 | 敌对 | 帝星座下的星官。                       |
| 斗宿兽 dou_beast       | 紫微界 | 被动 | 北斗七星曜变的灵兽。                     |
| 天机狐 tianji_fox      | 紫微界 | 被动 | 通晓天机的狐。                        |
| 紫垣蛾 ziwei_moth      | 紫微界 | 敌对 | 绕着星官飞舞的巨蛾。                     |
| 壬水蛟 ren_serpent     | 六壬界 | 被动 | 壬水中的蛟龙。                        |
| 贵人鹤 gui_crane       | 六壬界 | 被动 | 贵人星的仙鹤。                        |
| 螣蛇犬 she_hound       | 六壬界 | 被动 | 螣蛇化形的猎犬。                       |
| 玄武龟 xuan_turtle     | 六壬界 | 被动 | 玄武之相的巨龟。                       |
| 门卫 men_guard        | 奇门界 | 敌对 | 守门的甲士。                         |
| 星魁狐 xing_fox        | 奇门界 | 被动 | 九星之狐。                          |
| 遁甲鼠 dun_rat         | 奇门界 | 被动 | 遁入甲缝的机灵鼠。                      |
| 甲子鸟 jia_bird        | 奇门界 | 环境 | 报甲子时辰的鸟。                       |

### 刷怪蛋

| id                         | 名称       |
| -------------------------- | -------- |
| `dou_beast_spawn_egg`      | 斗宿兽刷怪蛋   |
| `dun_rat_spawn_egg`        | 遁甲鼠刷怪蛋   |
| `fate_wisp_spawn_egg`      | 星辉符灵刷怪蛋  |
| `gui_crane_spawn_egg`      | 贵人鹤刷怪蛋   |
| `hex_phantom_spawn_egg`    | 卦影刷怪蛋    |
| `ink_bat_spawn_egg`        | 爻蝠刷怪蛋    |
| `ink_wolf_spawn_egg`       | 爻狼刷怪蛋    |
| `jade_goat_spawn_egg`      | 玉羊刷怪蛋    |
| `jade_rabbit_spawn_egg`    | 玉兔刷怪蛋    |
| `jade_spirit_spawn_egg`    | 青玉灵刷怪蛋   |
| `jia_bird_spawn_egg`       | 甲子鸟刷怪蛋   |
| `men_guard_spawn_egg`      | 门卫刷怪蛋    |
| `ren_serpent_spawn_egg`    | 壬水蛟刷怪蛋   |
| `she_hound_spawn_egg`      | 螣蛇犬刷怪蛋   |
| `star_fox_spawn_egg`       | 星狐刷怪蛋    |
| `star_slime_spawn_egg`     | 星屑史莱姆刷怪蛋 |
| `star_watcher_spawn_egg`   | 观星者刷怪蛋   |
| `stone_guardian_spawn_egg` | 卦石守卫刷怪蛋  |
| `tianji_fox_spawn_egg`     | 天机狐刷怪蛋   |
| `vein_turtle_spawn_egg`    | 地脉龟刷怪蛋   |
| `xing_fox_spawn_egg`       | 星魁狐刷怪蛋   |
| `xuan_turtle_spawn_egg`    | 玄武龟刷怪蛋   |
| `ziwei_moth_spawn_egg`     | 紫垣蛾刷怪蛋   |
| `ziwei_star_spawn_egg`     | 紫微星官刷怪蛋  |

## 14. 术数系统

「三式」+ 命理书 + 12 门小术数，全部服务端纯函数计算——同一天问同一件事，答案永远一致。

### 三式（大三门）

| 术式   | 道具             | 定位                          |
| ---- | -------------- | --------------------------- |
| 紫微斗数 | `ziwei_chart`  | 推命之盘。看的是「你是谁」——命身财官、十四主星。   |
| 大六壬  | `liuren_plate` | 占事之盘。看的是「事怎么走」——月将加时、四课三传。  |
| 奇门遁甲 | `qimen_disc`   | 择时择方之盘。看的是「往哪走」——九宫八门、三奇六仪。 |

|                                         |        |          |
| --------------------------------------- | ------ | -------- |
| 三式盘由对应的界域材料合成；起卦走 \`/yishu oracle ziwei | liuren | qimen\`。 |

### 12 门小术数（MinorSchools）

| 参数           | 门派              | 定位                              |
| ------------ | --------------- | ------------------------------- |
| `qizheng`    | 七政四余            | 星命 · 观星台与流年星象；值日星宿与流年照命         |
| `meihua`     | 梅花易数            | 轻量起卦 · 即时占卜，时间起卦、体用生克           |
| `xiaoliuren` | 小六壬             | 掌上速断 · 前期快捷占（大安/留连/速喜/赤口/小吉/空亡） |
| `zeri`       | 择日学             | 动土 / 安床 / 开门的吉凶判断               |
| `xiangshu`   | 相术              | 面相 · NPC 鉴定与招募                  |
| `cezi`       | 姓名测字            | 给物品、村庄、角色取名的吉凶词条；笔画起卦与五格同源      |
| `qianwen`    | 签诗 · 灵棋经 · 诸葛神数 | 抽签解签 · 隐喻提示                     |
| `jiemeng`    | 周公解梦            | 睡后梦境与预兆                         |
| `kanyu`      | 堪舆四派            | 玄空飞星 / 八宅 / 三合 / 峦头             |
| `zhenpan`    | 阵盘              | 八卦阵 / 九宫阵 / 聚灵阵                 |
| `yinde`      | 因果功德            | 善行、福报与奇遇                        |
| `taiyi`      | 太乙神数            | 三式之三 · 国运与大势                    |

> ⚠️ **反噬机制**：每日问卦有上限，超限后惩罚依次加重；六爻界内上限减半、反噬翻倍。占卜是工具，不是雷达。

## 15. 机制详解

17 个核心系统，全部注释在源码里，这里按玩法整理。

- **黄历时钟**（`ClockSystem`）：手持时钟右键直接展开黄历面板；点物品展示框里的时钟同样有效（墙上的挂钟也不会被转歪）。历法只在服务端计算，面板显示对齐主世界时辰。模组纪年 2100～2900 年，可看大运流年。
- **模组历法**（`YishuCalendar`）：自定历法：一年 12 个月、每 15 日一节气，二十四节气由此驱动。年月日时四柱八字由真实历法推算（含干支），是所有术数的「时间轴」。
- **每日宜忌**（`FengShuiSystems.Almanac`）：从流日干支推出当日宜忌：宜 2～3 条、忌 2～3 条。同一种子、同一维度、同一天永远是同一份黄历。宜：动土兴建、入地采掘、交易买卖、附魔炼器、祭祀祈福、安床落帐、安门立户、四方远游、征伐战斗、收获作物、繁育牲畜；忌：动土、采掘、交易、安床、安门、远行、战斗、夜间外出、涉水渡河、近火用炉。
- **动土吉凶**（`DayActionHook`）：黄历落在手上：玩家放置方块的那一刻查当日宜忌——宜动土则顺，忌动土则招煞（凶液池、雷劈、怪声）。安床、开门同理。
- **风水域**（`FengShuiField / TerrainQi`）：以 3×3 采样、60 刻缓存计算脚下的气：气煞阴阳、五行偏向。站在生气之地回复更快；怪物也会随地形改变强弱——凶地出凶怪。/yishu fengshui 随时查看。
- **龙脉**（`FengShuiSystems.DragonVein`）：一条不规则的带状地气，逐年游走。站在龙脉上一切采集皆有加成。用龙脉核心碎片（dragon_core_shard）在龙脉祭坛立契，即可占领一条龙脉。/yishu vein 查看方位，寻脉图（vein_map）的墨迹会自己挪向最近的遗迹。
- **太岁**（`FengShuiSystems.TaiSui`）：每年坐镇一个方位。在太岁方位动土会招煞，立一块石敢当（shi_gandang）就能化解；太岁符/太岁丹提供一年一度的护持。
- **镇物**（`GuardianStoneSystem`）：镇宅石（zhenzhai_stone）：立起后方圆 24 格内的邪祟受镇——迟滞、虚弱。化煞碑（huasha_stone）：压制地里的煞气。五行灵石块可作为祭坛基座、压制维度煞气。
- **阵盘**（`FormationSystem`）：阵盘右键地面布阵：阵眼记入存档、当日有效（次日天亮阵散）。八卦阵：阵内抗性+缓慢回血，邪祟入阵迟滞受抑；北斗阵：阵内夜视+疾行，夜路如白昼；招财阵：阵内幸运 II，收获更丰。
- **梦境**（`DreamSystem`）：睡醒那一刻起梦：每张床、每个世界、每一天只做一场梦，梦象由「种子+玩家+当日」纯函数推出——重睡多少遍都是那一场，但预兆只有头一次兑现。吉梦：晨起得吉兆带 buff、记小功德；凶梦：当夜心魔化形（Vex）来劫，一次了结。
- **节气**（`SolarTermSystem`）：二十四节气每 15 日一换，换气时全服播报；一气之内人人得小加持——立春生发、夏至阳极、冬至阴极。「四立」与「二分二至」是大节点，播报更隆重、加持更强。
- **功德阴德**（`MeritSystem`）：积善之家必有余庆：喂饱牲畜 +2，金苹果引僵尸村民回头 +10，凶梦醒后守本心另记小功。功德高者得福报（雷劫劈邪、奇遇）；作恶者有余殃。/yishu merit 查看账本。
- **相面招募**（`Physiognomy`）：相面镜照村民面相：面相由村民 UUID 确定——拿什么镜子照、谁来照、照多少遍都是同一个面相。贵相村民可潜行照镜为其「传名」，获得交易折价（gossip 正面传闻）。
- **反噬**（`BacklashSystem`）：占卜不是雷达：每日问卦有上限，超限后惩罚依次加重（耳鸣、迷途、煞气缠身）。六爻界内问卦上限减半、反噬翻倍——卦气最真，代价也最真。
- **教程与史册**（`TutorialSystem / Chronicle / PlainSpeak`）：游戏内引导（/yishu tutorial，可 admin 开关）随进度讲述玩法；Chronicle 记录你的术数生涯；PlainSpeak 把玄学术语翻译成大白话，新人也能看懂卦辞。
- **木头经济**（`Round 22–25`）：21 种模组木完整的原版级体验：木板进 #minecraft:planks（木棍/工作台/箱子/床等 34 配方白拿）；原木进 #minecraft:logs / #minecraft:logs_that_burn（熔炉烧木炭、可当燃料）；全部木方块可被火焰点燃蔓延（原木 5/5、板材 20/5、树叶树苗 30/60）；21 种木船+运输船；栅栏/门/告示牌等全套变体。
- **矿物生成**（`Round 21–22`）：主世界新增模组矿脉（铁矿级概率+）；七界各有独立矿系（紫微晶矿/斗枢矿/壬水矿…共 25 种原矿），矿山裸露概率已调优（discard_chance_on_air_exposure=0），洞穴里能直接看到矿脉。

## 16. 村民与交易

12 种新村民职业（全部走原版 gossip/交易数据包通道），每种职业 3 级交易树。

| 职业     | 英文 id                                          | 所在村落 |
| ------ | ---------------------------------------------- | ---- |
| 星命师    | astrologer                                     | 星辉村  |
| 蓍草商    | yarrow_master                                  | 星辉村  |
| 地理师    | geomancer                                      | 青玉村  |
| 相地师    | surveyor                                       | 青玉村  |
| 卦师     | diviner                                        | 爻纹村  |
| 星官     | star_master                                    | 紫微村  |
| 斗数家    | fate_master / dou_master                       | 紫微村  |
| 课师     | lesson_master                                  | 六壬村  |
| 阵法家    | array_master                                   | 奇门村  |
| 八门师    | bamen_master                                   | 奇门村  |
| （复用）   | 太乙村村民复用紫微职业                                    | 太乙村  |
| 原版职业扩展 | butcher / cartographer / cleric / farmer 等新增交易 | 各村   |

## 17. 成就

18 项成就，从「天机初现」到「七界巡游」。

| 成就        | id               | 描述                                   |
| --------- | ---------------- | ------------------------------------ |
| 易枢 · 天机初现 | `root`           | 获得命盘玉简——你的命数，从这一刻开始记录。               |
| 掷地有声      | `divination`     | 获得三枚铜钱，六爻问天的开端。                      |
| 寻龙点穴      | `compass`        | 获得风水罗盘，脚下每一块地都开始有话要说。                |
| 格物观气      | `observing`      | 放置一座观气台，读懂你家的户型。                     |
| 万界之钥      | `key`            | 拿到一把灵引。遗迹宝箱与流浪商人带着它，也能用四份界域料在工台上铸出来。 |
| 破界之门      | `first_gate`     | 用灵引立起第一扇门——走进去，就是另一个世界。              |
| 星命之约      | `realm_fate`     | 初次踏入命理维度。                            |
| 龙脉之约      | `realm_fengshui` | 初次踏入风水维度。                            |
| 卦象之约      | `realm_liuyao`   | 初次踏入六爻维度。                            |
| 紫垣之约      | `realm_ziwei`    | 初次踏入紫微界。                             |
| 月将之约      | `realm_liuren`   | 初次踏入六壬界。                             |
| 八门之约      | `realm_qimen`    | 初次踏入奇门界。                             |
| 太乙之约      | `realm_taiyi`    | 初次踏入太乙界。                             |
| 七界巡游      | `realm_all`      | 七个界域都留下一串脚印。                         |
| 界外奇珍      | `realm_bounty`   | 带回一份异界的矿物。                           |
| 七界灵泉      | `realm_brew`     | 用桶舀起一份异界的液体。                         |
| 深入地宫      | `ruin`           | 找到一处遗迹的地下密室，并活着出来。                   |
| 逆天改命之始    | `dragon_core`    | 获得龙脉核心碎片——命，是可以改的。                   |

## 18. 指令参考

所有玩法都有指令兜底——忘了怎么玩就 /yishu help。

| 指令                             | 说明                                        |
| ------------------------------ | ----------------------------------------- |
| `/yishu help`                  | 玩法总览与指引                                   |
| `/yishu bazi`                  | 查看自己的八字命盘                                 |
| `/yishu fengshui`              | 查看脚下风水域（气煞阴阳、五行偏向）                        |
| `/yishu almanac`               | 查看今日黄历宜忌                                  |
| `/yishu tutorial`              | 查看教程进度                                    |
| `/yishu professional`          | 术数生涯档案（史册）                                |
| `/yishu divine <学派>`           | 起一卦：meihua / xiaoliuren / qianwen / cezi… |
| `/yishu oracle <式>`            | 三式起局：ziwei / liuren / qimen               |
| `/yishu ruins`                 | 感知最近的遗迹方位                                 |
| `/yishu vein`                  | 感知龙脉方位                                    |
| `/yishu taisui`                | 查看今年太岁方位                                  |
| `/yishu merit`                 | 查看功德账本                                    |
| `/yishu mob`                   | 查看身边生物的卦气                                 |
| `/yishu calendar`              | 查看模组历法（可 admin 调年调日）                      |
| `/yishu travel`                | 跨维度旅行相关信息                                 |
| `/yishu admin tutorial on/off` | 开关新手教程                                    |
| `/yishu admin limit <n>`       | 调整每日问卦上限                                  |
| 快捷键（默认未绑定）                     | 打开命盘面板（设置-按键绑定里改）                         |

## 19. 数据一览

| 统计项    | 数量                  |
| ------ | ------------------- |
| 注册物品   | 336                 |
| 注册方块   | 398                 |
| 合成配方   | 354                 |
| 结构     | 27                  |
| 群系（七界） | 42                  |
| 维度     | 7（另有主世界/下界/末地的时间体系） |
| 生物     | 24                  |
| 术数门派   | 3 式 + 12 小术数        |
| 语言文件键  | 743                 |
