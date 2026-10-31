package com.chenchen.yishu.fengshui;

import java.util.ArrayList;
import java.util.List;

/**
 * 易枢 · 户型图 —— 扫描结果的载体。
 *
 * <p>一张户型图要同时承载三样东西：
 * <ol>
 *   <li>{@link Grid 平面网格} —— 给玩家看的 2D 俯视图（墙、室内、门、窗）；</li>
 *   <li>{@link Room 房间列表} —— 每个房间的功能判定与范围；</li>
 *   <li>{@link Issue 问题清单} —— 按阳宅风水规则挑出来的毛病与改造建议。</li>
 * </ol>
 *
 * <p>为什么需要单独一个文件：{@link HouseScanner} 的算法很啰嗦，把数据结构混在里面
 * 会让两边都难读。这里只放「扫描完的东西长什么样」，不放「怎么扫出来的」。
 */
public final class FloorPlan {

    private FloorPlan() {
    }

    // ==================================================================== 平面网格

    /** 网格单元类型。 */
    public static final byte VOID = 0;
    public static final byte INTERIOR = 1;
    public static final byte WALL = 2;
    public static final byte DOOR = 3;
    public static final byte WINDOW = 4;
    /** 中宫（户型中心三宫），风水上最要紧的位置。 */
    public static final byte PALACE = 5;

    /**
     * 2D 平面网格。
     *
     * @param originX 左上角的世界 X
     * @param originZ 左上角的世界 Z
     * @param width   宽（X 方向）
     * @param depth   深（Z 方向）
     * @param cells   长度 width × depth，行优先（先 Z 后 X）
     * @param floorY  每个单元的楼板高度，长度同 cells
     */
    public record Grid(int originX, int originZ, int width, int depth, byte[] cells, int[] floorY) {

        public byte at(int x, int z) {
            if (x < 0 || z < 0 || x >= this.width || z >= this.depth) {
                return VOID;
            }
            return this.cells[z * this.width + x];
        }

        public int floorAt(int x, int z) {
            if (x < 0 || z < 0 || x >= this.width || z >= this.depth) {
                return 0;
            }
            return this.floorY[z * this.width + x];
        }

        /** 中宫中心格（用于界面画八卦九宫）。 */
        public int centerX() {
            return this.width / 2;
        }

        public int centerZ() {
            return this.depth / 2;
        }

        /**
         * 把某一格映射到八卦九宫之一。
         *
         * <p>九宫是后天八卦的布局：离（南）在上、坎（北）在下、震（东）在左、兑（西）在右，
         * 与地图坐标的对应关系依「上北下南」的制图习惯排布，
         * 因此屏幕上看到的方位和游戏里按 F3 看到的方位是一致的。
         *
         * @return 0..8，顺序为 巽 离 坤 / 震 中 兑 / 艮 坎 乾
         */
        public static int palaceOf(int column, int row, int width, int depth) {
            int cx = column < width / 3 ? 0 : column < width * 2 / 3 ? 1 : 2;
            int cz = row < depth / 3 ? 0 : row < depth * 2 / 3 ? 1 : 2;
            return cz * 3 + cx;
        }

        /** 九宫名。 */
        public static final String[] PALACE_NAMES = {
                "巽宫", "离宫", "坤宫",
                "震宫", "中宫", "兑宫",
                "艮宫", "坎宫", "乾宫"
        };
    }

    // ==================================================================== 房间

    /**
     * 房间功能。
     *
     * <p>{@code priority} 决定识别时的优先级：一个房间里既有床又有熔炉时，
     * 更高的优先级胜出。「祭坛」排在最高，因为那是最不该被忽略的功能。
     */
    public enum RoomType {
        ALTAR("祭坛", 100, "沟通天地之所，宜净宜静"),
        ENCHANT("附魔室", 90, "藏书引气，宜藏于内"),
        KITCHEN("厨房", 80, "火气之宅，忌与净处相邻"),
        BATHROOM("卫浴", 75, "水秽之地，最忌居中"),
        BEDROOM("卧室", 70, "养气之所，宜静宜正"),
        FARM("农场", 60, "生养之地，宜向阳"),
        WORKSHOP("工坊", 55, "金铁交鸣，宜在偏处"),
        STORAGE("储藏", 50, "仓廪之属，宜实宜闭"),
        STUDY("书房", 45, "讲读之所，宜明宜雅"),
        LIVING("起居", 40, "待客明堂，宜敞宜亮"),
        EMPTY("空房", 0, "四壁徒然，尚有可为");

        private final String zh;
        private final int priority;
        private final String note;

        RoomType(String zh, int priority, String note) {
            this.zh = zh;
            this.priority = priority;
            this.note = note;
        }

        public String zh() {
            return this.zh;
        }

        public int priority() {
            return this.priority;
        }

        public String note() {
            return this.note;
        }
    }

    /**
     * 一个房间。
     *
     * @param id       编号（1 起，按发现顺序）
     * @param type     功能判定
     * @param minX     世界坐标边界
     * @param maxZ     世界坐标边界
     * @param cellCount 室内格数
     * @param floorY   楼板高度（众数）
     * @param ceilY    天花板高度
     * @param lit      是否明亮
     */
    public record Room(int id, RoomType type, int minX, int minZ, int maxX, int maxZ,
                       int cellCount, int floorY, int ceilY, boolean lit) {

        public int centerX() {
            return (this.minX + this.maxX) / 2;
        }

        public int centerZ() {
            return (this.minZ + this.maxZ) / 2;
        }

        public int area() {
            return this.cellCount;
        }

        public String describe() {
            return "#" + this.id + " " + this.type.zh() + "（" + this.cellCount + " 格，"
                    + (this.lit ? "明" : "暗") + "）";
        }
    }

    // ==================================================================== 问题

    /**
     * 一条风水问题。
     *
     * @param severity 严重度：0 提示 / 1 小凶 / 2 凶 / 3 大凶
     * @param title    问题名
     * @param detail   为什么是问题
     * @param advice   怎么改
     * @param delta    对总分的加减（负数为扣分）
     */
    public record Issue(int severity, String title, String detail, String advice, int delta) {

        public String severityName() {
            return switch (this.severity) {
                case 3 -> "大凶";
                case 2 -> "凶";
                case 1 -> "小凶";
                default -> "提示";
            };
        }

        public int severityColor() {
            return switch (this.severity) {
                case 3 -> 0xFFD9575B;
                case 2 -> 0xFFD9905B;
                case 1 -> 0xFFD6C05B;
                default -> 0xFF8FB0D4;
            };
        }
    }

    // ==================================================================== 报告

    /**
     * 一份完整的扫描报告。
     *
     * @param grid   平面网格
     * @param rooms  房间列表
     * @param issues 问题列表
     * @param score  阳宅总分 0~100
     * @param field  所在地的风水域采样
     */
    public record Report(Grid grid, List<Room> rooms, List<Issue> issues, int score,
                         FengShuiField.Sample field) {

        /** 阳宅吉凶等级。 */
        public FengShuiField.Grade grade() {
            if (this.score >= 80) {
                return FengShuiField.Grade.GREAT_LUCK;
            }
            if (this.score >= 60) {
                return FengShuiField.Grade.LUCK;
            }
            if (this.score >= 40) {
                return FengShuiField.Grade.PLAIN;
            }
            if (this.score >= 22) {
                return FengShuiField.Grade.BAD;
            }
            return FengShuiField.Grade.DIRE;
        }

        /** 按房间类型查找。 */
        public List<Room> roomsOf(RoomType type) {
            List<Room> result = new ArrayList<>();
            for (Room room : this.rooms) {
                if (room.type() == type) {
                    result.add(room);
                }
            }
            return result;
        }

        /** 严重度 ≥ {@code level} 的问题数。 */
        public int countIssues(int level) {
            int count = 0;
            for (Issue issue : this.issues) {
                if (issue.severity() >= level) {
                    count++;
                }
            }
            return count;
        }

        /** 一句话总结。 */
        public String summary() {
            return grade().zh() + "（" + this.score + "/100）· 共 " + this.rooms.size()
                    + " 个房间 · " + countIssues(2) + " 处凶位";
        }
    }

    // ==================================================================== 八卦九宫

    /** 九宫对应的方位名（配合 {@link Grid#PALACE_NAMES} 使用）。 */
    public static final String[] PALACE_DIRECTIONS = {
            "东南", "正南", "西南",
            "正东", "中央", "正西",
            "东北", "正北", "西北"
    };

    /**
     * 某方位对应的「宜」与「忌」。
     *
     * <p>这是给界面上的改造建议用的：玩家看到一个房间落在坤宫，
     * 就能知道这里适合放什么、不适合放什么。
     */
    public static String adviceForPalace(int palace) {
        return switch (palace) {
            case 0 -> "巽宫（东南）主财位，宜置财箱、货栈、水景";
            case 1 -> "离宫（正南）主名声，宜明亮开阔，忌堆杂物";
            case 2 -> "坤宫（西南）主母位，宜静宜柔，忌动土与利器";
            case 3 -> "震宫（正东）主长子与新生，宜生机之物";
            case 4 -> "中宫为本宅之心，宜空旷通畅，最忌卫浴与楼梯";
            case 5 -> "兑宫（正西）主口舌与喜悦，宜待客之所";
            case 6 -> "艮宫（东北）主少男与止，宜储物与闭关";
            case 7 -> "坎宫（正北）主水与事业，宜置水道，忌火炉";
            default -> "乾宫（西北）主父与尊位，宜置神位与主卧";
        };
    }
}
