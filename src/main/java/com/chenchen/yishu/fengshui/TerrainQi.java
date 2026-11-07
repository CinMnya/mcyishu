package com.chenchen.yishu.fengshui;

import com.chenchen.yishu.dimension.DimensionProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 易枢 · 地形气场（第 18 轮）。
 *
 * <p>老版 {@link FengShuiField} 是纯函数：只看世界种子和坐标，
 * 所以「把一座山搬走，那里的风水还是老样子」—— 玩家当然会觉得假。
 * 这一层把真实地形叠上去：峦头（起伏）、水法（聚气）、草木（生气）、
 * 开凿（气散）、人工营造（藏风），一起修正原来的域场。
 *
 * <p><b>为什么用短 TTL 缓存而不是监听每个方块变更</b>：
 * 放置方块、活塞推拉、爆炸、结构生成、/fill 都会改地形，
 * 逐个挂事件既挂不全又容易漏客户端侧改动。缓存按 16×16 区块存，
 * 每 {@value #CACHE_TICKS} 刻（3 秒）自然过期重新扫描 ——
 * 搬完山最多 3 秒，脚下的风水就会变，而且不需要任何事件钩子。
 *
 * <p>修正幅度刻意压在 ±25：域场仍由世界种子决定，地形只是「加减分」，
 * 不会把一张世代相传的凶地一夜之间翻成大吉。
 */
public final class TerrainQi {

    private TerrainQi() {
    }

    /** 缓存寿命：3 秒。 */
    public static final long CACHE_TICKS = 60L;
    /** 扫描半径（格），取 3×3 共 9 个采样点。 */
    public static final int SCAN_RADIUS = 8;

    private record Cache(long gameTime, int relief, double water, double green,
                         double solid, double built) {
    }

    private static final Map<String, Cache> CACHE = new ConcurrentHashMap<>();

    // ==================================================================== 对外

    /**
     * 查询带地形修正的风水。所有玩家可感知的入口都该走这个，
     * 而不是直接用 {@link FengShuiField#sample(long, DimensionProfile, double, double)}。
     */
    public static FengShuiField.Sample sample(ServerLevel level, DimensionProfile dimension,
                                              int x, int z) {
        FengShuiField.Sample base = FengShuiField.sample(level.getSeed(), dimension, x, z);
        Cache cache = readCache(level, x >> 4, z >> 4);
        if (cache == null) {
            return base;
        }
        int qi = clamp(base.qi() + qiDelta(cache));
        int sha = clamp(base.sha() + shaDelta(cache));
        int yin = clamp(base.yin() + yinDelta(cache));
        int yang = clamp(base.yang() + yangDelta(cache));
        return new FengShuiField.Sample(base.domainX(), base.domainZ(), qi, sha,
                yin, yang, base.element(), base.elementStrength());
    }

    /** 手动让某个区块的缓存立刻失效（方块变更事件可以用，也能给指令调用）。 */
    public static void invalidate(ServerLevel level, BlockPos pos) {
        CACHE.remove(key(level, pos.getX() >> 4, pos.getZ() >> 4));
    }

    /** 清空所有缓存（世界重载 / 指令调试时用）。 */
    public static void clear() {
        CACHE.clear();
    }

    /** 给界面用的一句话：这块地为什么是这个分。 */
    public static String terrainNote(ServerLevel level, int x, int z) {
        Cache cache = readCache(level, x >> 4, z >> 4);
        if (cache == null) {
            return "地形未载入，暂以域场本气论。";
        }
        StringBuilder sb = new StringBuilder("峦头起伏 ").append(cache.relief()).append(" 格；");
        if (cache.water() > 0.25D) {
            sb.append("近水聚气；");
        } else if (cache.water() < 0.02D) {
            sb.append("燥土无水，气不聚；");
        }
        if (cache.green() > 0.30D) {
            sb.append("草木葱茏，生气旺；");
        } else if (cache.green() < 0.05D) {
            sb.append("草木稀疏，生气薄；");
        }
        if (cache.built() > 0.35D) {
            sb.append("人工营造多，藏风而气滞；");
        }
        if (cache.relief() > 16) {
            sb.append("峰峦陡峭，尖角带煞。");
        } else if (cache.relief() < 2) {
            sb.append("地势过平，气散不收。");
        } else {
            sb.append("起伏得宜，藏风聚气。");
        }
        return sb.toString();
    }

    // ==================================================================== 扫描

    private static Cache readCache(ServerLevel level, int chunkX, int chunkZ) {
        String key = key(level, chunkX, chunkZ);
        long now = level.getGameTime();
        Cache cached = CACHE.get(key);
        if (cached != null && now - cached.gameTime() < CACHE_TICKS) {
            return cached;
        }
        Cache fresh = scan(level, chunkX * 16 + 8, chunkZ * 16 + 8, now);
        CACHE.put(key, fresh);
        return fresh;
    }

    private static Cache scan(ServerLevel level, int cx, int cz, long gameTime) {
        int minHeight = Integer.MAX_VALUE;
        int maxHeight = Integer.MIN_VALUE;
        int water = 0;
        int green = 0;
        int solid = 0;
        int built = 0;
        int total = 0;

        for (int dz = -SCAN_RADIUS; dz <= SCAN_RADIUS; dz += SCAN_RADIUS) {
            for (int dx = -SCAN_RADIUS; dx <= SCAN_RADIUS; dx += SCAN_RADIUS) {
                int px = cx + dx;
                int pz = cz + dz;
                int height = level.getHeight(Heightmap.Types.MOTION_BLOCKING, px, pz);
                minHeight = Math.min(minHeight, height);
                maxHeight = Math.max(maxHeight, height);

                BlockState state = level.getBlockState(new BlockPos(px, Math.max(height - 1, level.getMinY()), pz));
                if (!state.getFluidState().isEmpty() || state.is(Blocks.WATER)) {
                    water++;
                }
                if (state.is(BlockTags.LEAVES)) {
                    green++;
                }
                if (!state.isAir()) {
                    solid++;
                }
                if (isBuilt(state)) {
                    built++;
                }
                total++;
            }
        }
        if (total == 0) {
            return new Cache(gameTime, 0, 0.0D, 0.0D, 0.0D, 0.0D);
        }
        int relief = maxHeight - minHeight;
        return new Cache(gameTime, relief,
                (double) water / total, (double) green / total,
                (double) solid / total, (double) built / total);
    }

    /** 非自然表层 = 人工营造。粗略但够用，不引入额外标签依赖。 */
    private static boolean isBuilt(BlockState state) {
        if (state.isAir() || state.is(Blocks.WATER)) {
            return false;
        }
        return !(state.is(Blocks.STONE) || state.is(Blocks.DIRT) || state.is(Blocks.GRASS_BLOCK)
                || state.is(Blocks.SAND) || state.is(Blocks.GRAVEL) || state.is(Blocks.CLAY)
                || state.is(Blocks.SNOW) || state.is(BlockTags.LEAVES) || state.is(BlockTags.LOGS)
                || state.is(Blocks.TALL_GRASS) || state.is(Blocks.FERN));
    }

    // ==================================================================== 修正

    private static int qiDelta(Cache c) {
        double delta = (c.green() - 0.30D) * 18.0D
                + (c.water() - 0.20D) * 12.0D
                - Math.max(0, c.relief() - 16) * 1.2D
                - (c.built() > 0.60D ? 8.0D : 0.0D);
        return (int) Math.round(Math.max(-25.0D, Math.min(25.0D, delta)));
    }

    private static int shaDelta(Cache c) {
        double delta = Math.max(0, c.relief() - 14) * 1.5D
                + (c.solid() > 0.85D ? 10.0D : 0.0D)
                - c.water() * 8.0D
                - c.green() * 6.0D;
        return (int) Math.round(Math.max(-20.0D, Math.min(25.0D, delta)));
    }

    private static int yinDelta(Cache c) {
        double delta = c.water() * 14.0D - c.green() * 4.0D;
        return (int) Math.round(Math.max(-18.0D, Math.min(18.0D, delta)));
    }

    private static int yangDelta(Cache c) {
        double delta = c.green() * 10.0D - c.water() * 6.0D;
        return (int) Math.round(Math.max(-18.0D, Math.min(18.0D, delta)));
    }

    private static int clamp(int value) {
        return Math.max(1, Math.min(100, value));
    }

    private static String key(ServerLevel level, int chunkX, int chunkZ) {
        return level.dimension().identifier() + ":" + chunkX + ":" + chunkZ;
    }
}
