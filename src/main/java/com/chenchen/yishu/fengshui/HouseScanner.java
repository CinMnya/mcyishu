package com.chenchen.yishu.fengshui;

import com.chenchen.yishu.core.StableKey;
import com.chenchen.yishu.dimension.DimensionProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 易枢 · 户型扫描器 —— 把一栋房子「读」成一张户型图。
 *
 * <p>玩家拿着观气台右键一扇门，就会触发这里。整个过程分五步：
 *
 * <ol>
 *   <li><b>圈定室内</b>：从门口开始做三维洪水填充，只走「不是整块实心」的格子。
 *       遇到墙就停，于是填出来的正好是户型内部的空腔。</li>
 *   <li><b>求楼板与顶</b>：每一列（x,z）取室内格的最低点为楼板、最高点为顶。</li>
 *   <li><b>切房间</b>：把「门」当作墙，在平面格子上再做一次二维洪水填充。
 *       这样两间用门连起来的屋子自然被切成两间，而没有门的大厅仍然是一间 ——
 *       这条规则简单、可预测，也不会因为家具摆设而误判。</li>
 *   <li><b>认功能</b>：数每个房间里出现了哪些家具方块，按优先级最高的那个命名。</li>
 *   <li><b>评风水</b>：跑六条阳宅规则（大门、中宫、卧室、厨房、卫浴、格局），
 *       得出分数、问题清单与改造建议。</li>
 * </ol>
 *
 * <p>扫描结果<b>不写入存档</b>：它每次都是从方块状态现算出来的。
 * 所以你把墙拆了重建，下一次扫描立刻就是新户型 —— 不需要「刷新」这类操作。
 */
public final class HouseScanner {

    /** 单次扫描的室内格数上限。超过就判定「这不是一栋房子」。 */
    public static final int MAX_INTERIOR_VOLUME = 6000;
    /** 水平扫描半径。 */
    public static final int MAX_RADIUS = 28;
    /** 垂直扫描范围（相对门口）。 */
    public static final int MAX_UP = 24;
    public static final int MAX_DOWN = 16;

    private HouseScanner() {
    }

    // ==================================================================== 入口

    /**
     * 扫描一栋房子。
     *
     * @param level    世界
     * @param doorPos  被右键的门（或其上方一格的室内起点）
     * @param worldSeed 世界种子
     * @return 报告；若门口之外不是封闭空间则返回 {@code null}
     */
    public static FloorPlan.Report scan(Level level, BlockPos doorPos, long worldSeed,
                                        DimensionProfile dimension) {
        BlockPos start = findStart(level, doorPos);
        if (start == null) {
            return null;
        }

        Set<BlockPos> interior = floodInterior(level, start);
        if (interior.size() < 12 || interior.size() > MAX_INTERIOR_VOLUME) {
            return null;
        }

        // ---- 逐列求楼板 / 顶 / 是否门柱 ----
        Map<Long, Column> columns = new LinkedHashMap<>();
        for (BlockPos pos : interior) {
            long key = key(pos.getX(), pos.getZ());
            columns.computeIfAbsent(key, k -> new Column(pos.getX(), pos.getZ()))
                    .accept(pos, isDoorColumn(level, pos));
        }
        if (columns.isEmpty()) {
            return null;
        }

        // ---- 网格范围 ----
        int minX = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxZ = Integer.MIN_VALUE;
        for (Column column : columns.values()) {
            minX = Math.min(minX, column.x);
            maxX = Math.max(maxX, column.x);
            minZ = Math.min(minZ, column.z);
            maxZ = Math.max(maxZ, column.z);
        }
        // 四边各留一格给墙
        int originX = minX - 1;
        int originZ = minZ - 1;
        int width = maxX - minX + 3;
        int depth = maxZ - minZ + 3;
        if (width > 64 || depth > 64) {
            return null;
        }

        byte[] cells = new byte[width * depth];
        int[] floors = new int[width * depth];

        // ---- 先把室内与门画上 ----
        for (Column column : columns.values()) {
            int gx = column.x - originX;
            int gz = column.z - originZ;
            int index = gz * width + gx;
            cells[index] = column.door ? FloorPlan.DOOR : FloorPlan.INTERIOR;
            floors[index] = column.floorY();
        }
        // ---- 再把墙画上（室内以外、且在楼板高度是实心的格子）----
        for (int gz = 0; gz < depth; gz++) {
            for (int gx = 0; gx < width; gx++) {
                int index = gz * width + gx;
                if (cells[index] != FloorPlan.VOID) {
                    continue;
                }
                BlockPos probe = new BlockPos(originX + gx, averageFloor(columns), originZ + gz);
                BlockState state = level.getBlockState(probe);
                if (state.isCollisionShapeFullBlock(level, probe)) {
                    cells[index] = FloorPlan.WALL;
                } else if (hasWindowLikeBlock(level, probe)) {
                    cells[index] = FloorPlan.WINDOW;
                }
            }
        }

        // ---- 中宫标记 ----
        int centerColumn = width / 3;
        int centerRow = depth / 3;
        for (int gz = centerRow; gz < centerRow + 1 + Math.max(0, depth - centerRow * 3 - 2); gz++) {
            for (int gx = centerColumn; gx < centerColumn + 1 + Math.max(0, width - centerColumn * 3 - 2); gx++) {
                if (gx < 0 || gz < 0 || gx >= width || gz >= depth) {
                    continue;
                }
                int index = gz * width + gx;
                if (cells[index] == FloorPlan.INTERIOR) {
                    cells[index] = FloorPlan.PALACE;
                }
            }
        }

        FloorPlan.Grid grid = new FloorPlan.Grid(originX, originZ, width, depth, cells, floors);

        // ---- 切房间 ----
        List<FloorPlan.Room> rooms = segmentRooms(level, grid, dimension);

        // ---- 评风水 ----
        List<FloorPlan.Issue> issues = new ArrayList<>();
        int score = evaluate(level, grid, rooms, issues, doorPos, dimension);

        return new FloorPlan.Report(grid, List.copyOf(rooms), List.copyOf(issues), score,
                FengShuiField.sample(worldSeed, dimension, doorPos.getX(), doorPos.getZ()));
    }

    // ==================================================================== 第一步：圈定室内

    /** 找扫描起点：优先用「门上方那一格」，因为它必然在室内。 */
    private static BlockPos findStart(Level level, BlockPos doorPos) {
        if (isOpen(level, doorPos.above())) {
            return doorPos.above();
        }
        if (isOpen(level, doorPos)) {
            return doorPos;
        }
        if (isOpen(level, doorPos.above(2))) {
            return doorPos.above(2);
        }
        return null;
    }

    private static Set<BlockPos> floodInterior(Level level, BlockPos start) {
        Set<BlockPos> visited = new HashSet<>();
        Deque<BlockPos> queue = new ArrayDeque<>();
        visited.add(start);
        queue.add(start);

        int minY = start.getY() - MAX_DOWN;
        int maxY = start.getY() + MAX_UP;

        while (!queue.isEmpty()) {
            if (visited.size() > MAX_INTERIOR_VOLUME) {
                break;
            }
            BlockPos current = queue.poll();
            for (Direction direction : Direction.values()) {
                BlockPos next = current.relative(direction);
                if (visited.contains(next)) {
                    continue;
                }
                if (Math.abs(next.getX() - start.getX()) > MAX_RADIUS
                        || Math.abs(next.getZ() - start.getZ()) > MAX_RADIUS) {
                    continue;
                }
                if (next.getY() < minY || next.getY() > maxY) {
                    continue;
                }
                if (!isOpen(level, next)) {
                    continue;
                }
                visited.add(next);
                queue.add(next);
            }
        }
        return visited;
    }

    /**
     * 「这一格能不能站人」。
     *
     * <p>判据是「不是整块实心」。用这个标准而不是「是空气」，是因为门、活板门、
     * 栅栏门、火把、地毯这些都不该把屋子堵死 —— 而整块实心的石头、木头、箱子应该。
     */
    private static boolean isOpen(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) {
            return true;
        }
        return !state.isCollisionShapeFullBlock(level, pos);
    }

    private static boolean isDoor(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return isDoorState(state);
    }

    private static boolean isDoorState(BlockState state) {
        String path = BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath();
        return state.getBlock() instanceof TrapDoorBlock
                || state.getBlock() instanceof FenceGateBlock
                || path.endsWith("_door");
    }

    private static boolean isDoorColumn(Level level, BlockPos pos) {
        return isDoor(level, pos);
    }

    private static boolean hasWindowLikeBlock(Level level, BlockPos pos) {
        String path = BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos).getBlock()).getPath();
        return path.endsWith("_glass") || path.contains("glass_pane");
    }

    private static int averageFloor(Map<Long, Column> columns) {
        long sum = 0L;
        for (Column column : columns.values()) {
            sum += column.floorY();
        }
        return (int) (sum / Math.max(1, columns.size()));
    }

    // ==================================================================== 第三步：切房间

    private static List<FloorPlan.Room> segmentRooms(Level level, FloorPlan.Grid grid,
                                                     DimensionProfile dimension) {
        boolean[] visited = new boolean[grid.width() * grid.depth()];
        List<FloorPlan.Room> rooms = new ArrayList<>();
        int nextId = 1;

        for (int gz = 0; gz < grid.depth(); gz++) {
            for (int gx = 0; gx < grid.width(); gx++) {
                int index = gz * grid.width() + gx;
                byte cell = grid.cells()[index];
                if (visited[index] || (cell != FloorPlan.INTERIOR && cell != FloorPlan.PALACE)) {
                    continue;
                }
                List<int[]> cluster = floodRoom(grid, gx, gz, visited);
                if (cluster.isEmpty()) {
                    continue;
                }
                rooms.add(buildRoom(level, grid, cluster, nextId++, dimension));
            }
        }
        return rooms;
    }

    private static List<int[]> floodRoom(FloorPlan.Grid grid, int startX, int startZ, boolean[] visited) {
        List<int[]> cluster = new ArrayList<>();
        Deque<int[]> queue = new ArrayDeque<>();
        queue.add(new int[]{startX, startZ});
        visited[startZ * grid.width() + startX] = true;

        while (!queue.isEmpty()) {
            int[] current = queue.poll();
            cluster.add(current);
            int[][] neighbors = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
            for (int[] offset : neighbors) {
                int nx = current[0] + offset[0];
                int nz = current[1] + offset[1];
                if (nx < 0 || nz < 0 || nx >= grid.width() || nz >= grid.depth()) {
                    continue;
                }
                int index = nz * grid.width() + nx;
                if (visited[index]) {
                    continue;
                }
                byte cell = grid.cells()[index];
                // 门是分隔符：不跨越它，房间才会被正确切开
                if (cell != FloorPlan.INTERIOR && cell != FloorPlan.PALACE) {
                    continue;
                }
                visited[index] = true;
                queue.add(new int[]{nx, nz});
            }
        }
        return cluster;
    }

    private static FloorPlan.Room buildRoom(Level level, FloorPlan.Grid grid, List<int[]> cluster,
                                            int id, DimensionProfile dimension) {
        int minX = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxZ = Integer.MIN_VALUE;
        int[] floorHistogram = new int[384];
        boolean lit = false;

        for (int[] cell : cluster) {
            int worldX = grid.originX() + cell[0];
            int worldZ = grid.originZ() + cell[1];
            minX = Math.min(minX, worldX);
            maxX = Math.max(maxX, worldX);
            minZ = Math.min(minZ, worldZ);
            maxZ = Math.max(maxZ, worldZ);
            int floorY = grid.floorAt(cell[0], cell[1]);
            if (floorY >= 0 && floorY < floorHistogram.length) {
                floorHistogram[floorY]++;
            }
            if (!lit && level.getMaxLocalRawBrightness(new BlockPos(worldX, floorY + 1, worldZ)) >= 8) {
                lit = true;
            }
        }

        int floorY = 64;
        int best = -1;
        for (int y = 0; y < floorHistogram.length; y++) {
            if (floorHistogram[y] > best) {
                best = floorHistogram[y];
                floorY = y;
            }
        }

        FloorPlan.RoomType type = detectType(level, minX, minZ, maxX, maxZ, floorY);
        return new FloorPlan.Room(id, type, minX, minZ, maxX, maxZ, cluster.size(),
                floorY, floorY + 3, lit);
    }

    // ==================================================================== 第四步：认功能

    /**
     * 房间功能识别 —— 靠数家具。
     *
     * <p>用方块注册名做关键词匹配而不是 {@code instanceof}：这样任何模组的
     * 沙发、桌子、熔炉只要名字起得正常，都能被认出来，兼容性白送。
     */
    private static FloorPlan.RoomType detectType(Level level, int minX, int minZ, int maxX, int maxZ, int floorY) {
        Map<FloorPlan.RoomType, Integer> votes = new LinkedHashMap<>();

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int y = floorY; y <= floorY + 4; y++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    String path = BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos).getBlock()).getPath();
                    FloorPlan.RoomType hit = classify(path);
                    if (hit != null) {
                        votes.merge(hit, 1, Integer::sum);
                    }
                }
            }
        }
        if (votes.isEmpty() || maxX - minX < 1 || maxZ - minZ < 1) {
            return votes.isEmpty() ? FloorPlan.RoomType.EMPTY : bestVote(votes);
        }
        return bestVote(votes);
    }

    private static FloorPlan.RoomType bestVote(Map<FloorPlan.RoomType, Integer> votes) {
        FloorPlan.RoomType best = FloorPlan.RoomType.EMPTY;
        int bestScore = 0;
        for (Map.Entry<FloorPlan.RoomType, Integer> entry : votes.entrySet()) {
            // 优先级 ×2 + 票数 —— 让「祭坛」这种东西只要有一个就能定性
            int score = entry.getKey().priority() * 2 + Math.min(entry.getValue(), 12) * 6;
            if (score > bestScore) {
                bestScore = score;
                best = entry.getKey();
            }
        }
        return best;
    }

    private static FloorPlan.RoomType classify(String path) {
        if (contains(path, "altar", "bagua", "censer", "taiji", "shigandang", "yishu")) {
            return FloorPlan.RoomType.ALTAR;
        }
        if (contains(path, "enchanting_table") || path.equals("bookshelf") || path.contains("chiseled_bookshelf")) {
            return FloorPlan.RoomType.ENCHANT;
        }
        if (contains(path, "furnace", "smoker", "campfire", "blast_furnace")) {
            return FloorPlan.RoomType.KITCHEN;
        }
        if (contains(path, "cauldron", "water", "bubble_column", "sponge")) {
            return FloorPlan.RoomType.BATHROOM;
        }
        if (contains(path, "bed")) {
            return FloorPlan.RoomType.BEDROOM;
        }
        if (contains(path, "farmland", "wheat", "carrots", "potatoes", "beetroots",
                "pumpkin_stem", "melon_stem", "sugar_cane", "nether_wart", "composter")) {
            return FloorPlan.RoomType.FARM;
        }
        if (contains(path, "anvil", "smithing_table", "grindstone", "stonecutter", "fletching_table")) {
            return FloorPlan.RoomType.WORKSHOP;
        }
        if (contains(path, "chest", "barrel", "shulker_box", "hopper", "dispenser", "dropper")) {
            return FloorPlan.RoomType.STORAGE;
        }
        if (contains(path, "lectern", "cartography_table", "loom")) {
            return FloorPlan.RoomType.STUDY;
        }
        if (contains(path, "lantern", "soul_lantern", "candle", "torch")
                || contains(path, "stairs", "slab", "carpet")) {
            return FloorPlan.RoomType.LIVING;
        }
        return null;
    }

    private static boolean contains(String path, String... needles) {
        for (String needle : needles) {
            if (path.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    // ==================================================================== 第五步：评风水

    /**
     * 阳宅风水评分。
     *
     * <p>六条规则，每条都对应一句能说清楚的道理，而不是玄学黑箱：
     * <ol>
     *   <li><b>大门朝向</b>：坐北朝南为上，朝北者阴气直入。</li>
     *   <li><b>中宫</b>：户型的几何中心宜空旷透气，落厕所或楼梯为凶。</li>
     *   <li><b>卧室</b>：床头不应正对主门（冲门），卧室宜有光。</li>
     *   <li><b>厨房</b>：火气之地忌与卫浴相邻或相对（水火相冲）。</li>
     *   <li><b>卫浴</b>：水秽之处最忌居中，也忌正对大门。</li>
     *   <li><b>格局</b>：缺角、采光、房间数。</li>
     * </ol>
     * 起始分 50，按条加减，最后夹在 0~100。
     */
    private static int evaluate(Level level, FloorPlan.Grid grid, List<FloorPlan.Room> rooms,
                                List<FloorPlan.Issue> issues, BlockPos doorPos,
                                DimensionProfile dimension) {
        int score = 50;

        List<FloorPlan.Room> bedrooms = filter(rooms, FloorPlan.RoomType.BEDROOM);
        List<FloorPlan.Room> kitchens = filter(rooms, FloorPlan.RoomType.KITCHEN);
        List<FloorPlan.Room> bathrooms = filter(rooms, FloorPlan.RoomType.BATHROOM);
        List<FloorPlan.Room> livingRooms = filter(rooms, FloorPlan.RoomType.LIVING);
        List<FloorPlan.Room> altars = filter(rooms, FloorPlan.RoomType.ALTAR);

        // ---- 规则一：大门朝向 ----
        Direction facing = null;
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos side = doorPos.relative(direction);
            if (isDoor(level, side)) {
                facing = direction;
                break;
            }
        }
        if (facing == null && isDoor(level, doorPos)) {
            facing = level.getBlockState(doorPos).getValue(net.minecraft.world.level.block.DoorBlock.FACING);
        }
        if (facing == Direction.SOUTH) {
            score += 12;
            issues.add(new FloorPlan.Issue(0, "大门朝南（得位）",
                    "坐北朝南，采光与气流通畅，是阳宅最正的朝向。",
                    "保持现状，门前勿堆杂物。", 12));
        } else if (facing == Direction.NORTH) {
            score -= 14;
            issues.add(new FloorPlan.Issue(2, "大门朝北（气寒）",
                    "朝北的门阴气直入，冬季尤甚，屋内易聚湿寒。",
                    "在门内设一道影壁或屏风，并于门侧置长明灯一盏。", -14));
        } else if (facing != null) {
            score += 3;
            issues.add(new FloorPlan.Issue(0, "大门朝" + facingName(facing),
                    "非正南亦非正北，属中平之向。",
                    "无大碍，可在门内布置明亮之物以引气。", 3));
        }

        // ---- 规则二：中宫 ----
        int centerGx = grid.centerX();
        int centerGz = grid.centerZ();
        int centerWorldX = grid.originX() + centerGx;
        int centerWorldZ = grid.originZ() + centerGz;
        boolean centerOccupiedByBad = false;
        FloorPlan.RoomType centerType = null;
        for (FloorPlan.Room room : rooms) {
            if (centerWorldX >= room.minX() && centerWorldX <= room.maxX()
                    && centerWorldZ >= room.minZ() && centerWorldZ <= room.maxZ()) {
                centerType = room.type();
                centerOccupiedByBad = room.type() == FloorPlan.RoomType.BATHROOM
                        || room.type() == FloorPlan.RoomType.STORAGE;
                break;
            }
        }
        if (centerOccupiedByBad) {
            score -= 20;
            issues.add(new FloorPlan.Issue(3, "中宫被占（" + centerType.zh() + "）",
                    "户型的几何中心是全宅气口，最忌被卫浴、仓储之类的浊气压住。",
                    "把中宫腾空，或改作过厅、天井；实在不行至少保持通透与明亮。", -20));
        } else if (centerType == null) {
            score -= 6;
            issues.add(new FloorPlan.Issue(1, "中心无着落",
                    "扫描范围内没有覆盖到户型中心，可能是房子太小或形状过于狭长。",
                    "把主要房间布置得方正一些，或把中宫规划成厅堂。", -6));
        } else {
            score += 10;
            issues.add(new FloorPlan.Issue(0, "中宫清明",
                    "中心区域为" + centerType.zh() + "，气口未被浊气占压。",
                    "保持下去，这是好格局。", 10));
        }

        // ---- 规则三：卧室 ----
        if (bedrooms.isEmpty()) {
            score -= 4;
            issues.add(new FloorPlan.Issue(1, "无卧房",
                    "此宅未见床铺，长期居住者无处养气。",
                    "至少布置一间卧房，床的位置要避开正对大门。", -4));
        } else {
            for (FloorPlan.Room bedroom : bedrooms) {
                if (isFacingDoor(level, bedroom, doorPos)) {
                    score -= 9;
                    issues.add(new FloorPlan.Issue(2, "卧室冲门（" + bedroom.describe() + "）",
                            "床与大门成一直线，气流直冲，主不安眠。",
                            "把床移到侧墙，或在门与床之间加一道屏风、一列柜子。", -9));
                } else {
                    score += 6;
                }
                if (!bedroom.lit()) {
                    score -= 5;
                    issues.add(new FloorPlan.Issue(1, "卧房昏暗（" + bedroom.describe() + "）",
                            "阴气偏重，久居易倦。",
                            "加一盏灯（灯笼或海晶灯皆可），让亮度提到 8 以上。", -5));
                }
            }
        }

        // ---- 规则四：厨房 ----
        for (FloorPlan.Room kitchen : kitchens) {
            boolean adjacentToBath = false;
            for (FloorPlan.Room bathroom : bathrooms) {
                if (roomDistance(kitchen, bathroom) <= 2) {
                    adjacentToBath = true;
                    break;
                }
            }
            if (adjacentToBath) {
                score -= 13;
                issues.add(new FloorPlan.Issue(2, "厨卫相邻（" + kitchen.describe() + "）",
                        "灶为火、厕为水，水火相冲，主家人多病。",
                        "把两者隔开，中间至少留一间房的距离；或用一道实墙完全断开。", -13));
            } else {
                score += 5;
            }
        }

        // ---- 规则五：卫浴 ----
        for (FloorPlan.Room bathroom : bathrooms) {
            int distanceToDoor = (int) Math.sqrt(doorPos.distToCenterSqr(
                    bathroom.centerX() + 0.5D, doorPos.getY(), bathroom.centerZ() + 0.5D));
            if (distanceToDoor <= 4) {
                score -= 11;
                issues.add(new FloorPlan.Issue(2, "卫浴近门（" + bathroom.describe() + "）",
                        "秽气直冲入口，是最常见的阳宅之忌。",
                        "把卫浴移离入口，或至少让入口不正对它。", -11));
            }
        }
        if (bathrooms.isEmpty()) {
            score += 3;
        }

        // ---- 规则六：格局 ----
        int solidCells = 0;
        int voidCells = 0;
        for (byte cell : grid.cells()) {
            if (cell == FloorPlan.WALL) {
                solidCells++;
            } else if (cell == FloorPlan.VOID) {
                voidCells++;
            }
        }
        double fillRatio = grid.cells().length == 0 ? 0
                : (double) (solidCells) / grid.cells().length;
        if (fillRatio > 0.55D) {
            score -= 8;
            issues.add(new FloorPlan.Issue(1, "户型过密",
                    "实心部分占比过高，空间局促，气不流通。",
                    "打通一到两处隔墙，或开几扇窗。", -8));
        } else if (fillRatio < 0.18D) {
            score -= 5;
            issues.add(new FloorPlan.Issue(1, "围合不足",
                    "外围敞开过多，不能藏风聚气。",
                    "把外墙补完整，尤其注意四角不要漏。", -5));
        } else {
            score += 5;
        }

        // 缺角：四角若有两角以上是空的，视为缺角煞
        int missingCorners = 0;
        int[][] corners = {{1, 1}, {grid.width() - 2, 1}, {1, grid.depth() - 2},
                {grid.width() - 2, grid.depth() - 2}};
        for (int[] corner : corners) {
            byte cell = grid.at(corner[0], corner[1]);
            if (cell == FloorPlan.VOID) {
                missingCorners++;
            }
        }
        if (missingCorners >= 2) {
            score -= 7;
            issues.add(new FloorPlan.Issue(2, "宅形缺角",
                    "屋角缺失（缺 " + missingCorners + " 角），主家运不稳、人丁有损。",
                    "把缺掉的角补成方形；无法补时，在缺角处放置石敢当或盆栽以镇之。", -7));
        } else if (missingCorners == 0 && rooms.size() >= 3) {
            score += 6;
            issues.add(new FloorPlan.Issue(0, "宅形方正",
                    "四角齐整，是有福的屋形。",
                    "保持，不要在角上凿洞开门。", 6));
        }

        // ---- 附加：祭坛 ----
        if (!altars.isEmpty()) {
            score += 8;
            issues.add(new FloorPlan.Issue(0, "设有祭坛",
                    "宅中设坛，可自行化解煞气，是上等的布局。",
                    "坛位宜静宜洁，定期上香（右键香炉）可维持其效。", 8));
        }

        // ---- 维度修正 ----
        if (dimension == DimensionProfile.NETHER) {
            score -= 10;
            issues.add(new FloorPlan.Issue(1, "下界阳宅本难",
                    "此界火煞横生，本就不宜居住，任何屋子都要打折扣。",
                    "多用阴属性材料（灵魂沙、雪、冰）压制火煞。", -10));
        } else if (dimension == DimensionProfile.END) {
            score -= 6;
        }

        return Math.max(0, Math.min(100, score));
    }

    private static List<FloorPlan.Room> filter(List<FloorPlan.Room> rooms, FloorPlan.RoomType type) {
        List<FloorPlan.Room> result = new ArrayList<>();
        for (FloorPlan.Room room : rooms) {
            if (room.type() == type) {
                result.add(room);
            }
        }
        return result;
    }

    private static boolean isFacingDoor(Level level, FloorPlan.Room room, BlockPos doorPos) {
        // 粗判：卧室中心与门的连线是否近似落在某个水平轴上（正对）
        int dx = Math.abs(room.centerX() - doorPos.getX());
        int dz = Math.abs(room.centerZ() - doorPos.getZ());
        return (dx <= 1 && dz >= 3) || (dz <= 1 && dx >= 3);
    }

    private static int roomDistance(FloorPlan.Room a, FloorPlan.Room b) {
        int dx = Math.max(0, Math.max(a.minX() - b.maxX(), b.minX() - a.maxX()));
        int dz = Math.max(0, Math.max(a.minZ() - b.maxZ(), b.minZ() - a.maxZ()));
        return Math.max(dx, dz);
    }

    private static String facingName(Direction direction) {
        return switch (direction) {
            case NORTH -> "北";
            case SOUTH -> "南";
            case EAST -> "东";
            case WEST -> "西";
            default -> direction.getName();
        };
    }

    // ==================================================================== 辅助

    private static long key(int x, int z) {
        return ((long) x << 32) ^ (z & 0xFFFFFFFFL);
    }

    /** 扫描过程中每一列的中间状态。 */
    private static final class Column {
        private final int x;
        private final int z;
        private int minY = Integer.MAX_VALUE;
        private int maxY = Integer.MIN_VALUE;
        private boolean door;
        private int sampleCount;

        private Column(int x, int z) {
            this.x = x;
            this.z = z;
        }

        private void accept(BlockPos pos, boolean isDoor) {
            this.minY = Math.min(this.minY, pos.getY());
            this.maxY = Math.max(this.maxY, pos.getY());
            this.door |= isDoor;
            this.sampleCount++;
        }

        private int floorY() {
            return this.minY == Integer.MAX_VALUE ? 64 : this.minY;
        }
    }
}
