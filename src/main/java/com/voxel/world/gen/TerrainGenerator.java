package com.voxel.world.gen;

import com.voxel.block.BlockType;
import com.voxel.math.SimplexNoise;
import com.voxel.world.Biome;
import com.voxel.world.Chunk;

import static com.voxel.world.Chunk.*;

public final class TerrainGenerator {

    // Low frequencies for large, expansive biomes without tiny micro-biome islands
    private static final double OCEAN_FREQ = 0.0009;
    private static final double CLIMATE_FREQ = 0.00075;
    private static final double MOUNTAIN_FREQ = 0.0014;
    private static final double RIVER_FREQ = 0.0022;
    private static final double RIVER_THRESHOLD = 0.046;

    // Grid cell size for trees/flora: 1 candidate per 5x5 cell with internal offset 1..2
    // guarantees minimum 4-block and average 5-block spacing between adjacent trees
    private static final int TREE_CELL_SIZE = 5;
    private static final int TREE_SEARCH_MARGIN = 4;

    private TerrainGenerator() {}

    private static double smoothstep(double edge0, double edge1, double x) {
        double t = Math.max(0.0, Math.min(1.0, (x - edge0) / (edge1 - edge0)));
        return t * t * (3.0 - 2.0 * t);
    }

    public static double sampleRiver(SimplexNoise noise, long wx, long wz) {
        double r1 = noise.noise2D(wx * RIVER_FREQ + 1400.0, wz * RIVER_FREQ - 800.0);
        double r2 = noise.noise2D(wx * (RIVER_FREQ * 2.0) - 950.0, wz * (RIVER_FREQ * 2.0) + 1250.0);
        return Math.abs((r1 + 0.35 * r2) / 1.35);
    }

    public static double sampleTemperature(SimplexNoise noise, long wx, long wz) {
        // Smooth 2-octave climate with low 2nd octave weight to prevent small local spikes/blotches
        double n1 = noise.noise2D(wx * CLIMATE_FREQ + 500.0, wz * CLIMATE_FREQ - 300.0);
        double n2 = noise.noise2D(wx * (CLIMATE_FREQ * 2.0) + 1100.0, wz * (CLIMATE_FREQ * 2.0) - 700.0);
        return (n1 + 0.15 * n2) / 1.15;
    }

    public static double sampleHumidity(SimplexNoise noise, long wx, long wz) {
        double n1 = noise.noise2D(wx * CLIMATE_FREQ - 700.0, wz * CLIMATE_FREQ + 900.0);
        double n2 = noise.noise2D(wx * (CLIMATE_FREQ * 2.0) - 1400.0, wz * (CLIMATE_FREQ * 2.0) + 1700.0);
        return (n1 + 0.15 * n2) / 1.15;
    }

    public static double sampleContinental(SimplexNoise noise, long wx, long wz) {
        return noise.fbm2D(wx * OCEAN_FREQ, wz * OCEAN_FREQ, 3, 0.45, 2.0);
    }

    /**
     * Determines whether (wx, wz) is a coastal shoreline directly bordering ocean water.
     * Restricts beaches to a realistic, narrow strip (2-4 blocks wide) along the actual waterline,
     * preventing beaches from invading inland lowlands or appearing behind ridges of grass.
     */
    public static boolean isNearOceanWater(SimplexNoise noise, long wx, long wz) {
        double cont = sampleContinental(noise, wx, wz);
        // Ocean ends around -0.05; coastal transition zone is strictly bounded
        if (cont < -0.055 || cont > -0.010) {
            return false;
        }

        int hC = computeSurfaceHeight(noise, wx, wz);
        // Beach exists strictly at sea level shoreline (between SEA_LEVEL and SEA_LEVEL + 2)
        if (hC < SEA_LEVEL || hC > SEA_LEVEL + 2) {
            return false;
        }

        // Must directly border actual deep ocean water (within 1..3 blocks)
        for (int d = 1; d <= 3; d++) {
            if (isOceanWaterAt(noise, wx - d, wz) || isOceanWaterAt(noise, wx + d, wz)
             || isOceanWaterAt(noise, wx, wz - d) || isOceanWaterAt(noise, wx, wz + d)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isOceanWaterAt(SimplexNoise noise, long wx, long wz) {
        double cont = sampleContinental(noise, wx, wz);
        if (cont < -0.05) return true;
        return cont < -0.045 && computeSurfaceHeight(noise, wx, wz) < SEA_LEVEL;
    }

    public static double sampleMountain(SimplexNoise noise, long wx, long wz) {
        return noise.noise2D(wx * MOUNTAIN_FREQ - 200.0, wz * MOUNTAIN_FREQ - 200.0);
    }

    public static Biome computeBiome(SimplexNoise noise, long wx, long wz) {
        double oceanContinental = sampleContinental(noise, wx, wz);
        double temp = sampleTemperature(noise, wx, wz);

        if (oceanContinental < -0.22) {
            return Biome.DEEP_OCEAN;
        } else if (oceanContinental < -0.05) {
            return (temp < -0.16) ? Biome.FROZEN_OCEAN : Biome.OCEAN;
        }

        // Coastal shoreline: only if directly bordering actual ocean water
        if (isNearOceanWater(noise, wx, wz)) {
            if (temp < -0.16) {
                return Biome.SNOWY_TUNDRA; // Arctic icy shore
            } else if (temp < -0.06) {
                return Biome.TAIGA; // Cold rocky/gravelly northern shore
            } else {
                return Biome.BEACH; // Temperate/warm sandy beach
            }
        }

        double mountainVal = sampleMountain(noise, wx, wz);
        if (mountainVal > 0.27) {
            return Biome.MOUNTAINS;
        }

        // River biome for carved inland waterways
        if (sampleRiver(noise, wx, wz) < 0.038) {
            return Biome.RIVER;
        }

        double humidity = sampleHumidity(noise, wx, wz);

        if (temp < -0.16) {
            // Cold polar climate
            return (humidity < -0.02) ? Biome.SNOWY_TUNDRA : Biome.TAIGA;
        } else if (temp > 0.24) {
            // Hot tropical / arid climate
            if (humidity < -0.14) {
                return Biome.DESERT;
            } else if (humidity < -0.03) {
                return Biome.MESA;
            } else if (humidity < 0.12) {
                return Biome.PLAINS; // Wide buffer zone between arid and jungle
            } else {
                return Biome.JUNGLE;
            }
        } else {
            // Temperate climate (-0.16 <= temp <= 0.24)
            // Low humidity is always PLAINS - forests never appear in dry areas!
            if (humidity < 0.06) {
                return Biome.PLAINS;
            } else if (humidity < 0.16) {
                return Biome.BIRCH_FOREST;
            } else {
                return Biome.FOREST;
            }
        }
    }

    public static int computeSurfaceHeight(SimplexNoise noise, long wx, long wz) {
        double oceanContinental = sampleContinental(noise, wx, wz);
        double detail = noise.fbm2D(wx * 0.026 + 100.0, wz * 0.026 + 100.0, 3, 0.5, 2.0);

        // Ocean basins: exponential descent - gentle shallow shelf near coast, plunging drop-off into deep ocean
        double microDither = noise.noise2D(wx * 0.15 + 43.0, wz * 0.15 - 87.0) * 0.35;
        if (oceanContinental < -0.05) {
            double u = Math.min(1.0, Math.max(0.0, (-0.05 - oceanContinental) / 0.25));
            // Immediate shoreline (u < 0.05): strictly 1-block wading depth (floor at SEA_LEVEL - 1)
            // Halved distance before 2-block depth drop: steps to 2 blocks at u >= 0.05
            double shelfDepth = (u < 0.05) ? 1.0 : (1.0 + smoothstep(0.04, 0.06, u) * 1.0 + 1.2 * Math.pow(Math.max(0.0, u - 0.05), 1.1));
            // Continental slope plunge: sharp drop-off away from shelf edge into ocean bed
            double drop = smoothstep(0.08, 0.42, u);
            double deepOceanDepth = 24.0 + detail * 3.5;
            double totalDepth = shelfDepth * (1.0 - drop) + deepOceanDepth * drop;
            double detailScale = smoothstep(0.04, 0.12, u);
            double oceanFloor = SEA_LEVEL - totalDepth + detail * 0.8 * detailScale;
            return (int) Math.round(oceanFloor + microDither * 0.4 * detailScale);
        }

        double continental = noise.fbm2D(wx * 0.0040, wz * 0.0040, 4, 0.5, 2.0);
        double mountainRaw = noise.noise2D(wx * MOUNTAIN_FREQ - 200.0, wz * MOUNTAIN_FREQ - 200.0);
        double mountainWeight = smoothstep(-0.06, 0.42, mountainRaw);

        double temp = sampleTemperature(noise, wx, wz);
        double humidity = sampleHumidity(noise, wx, wz);

        // Smooth C1 transition from shoreline (-0.05) to inland (+0.01)
        // Shoreline land edge at -0.05 is exactly SEA_LEVEL (28), guaranteeing <= 1 block drop to water floor (27)
        double tCoast = smoothstep(-0.05, 0.01, oceanContinental);
        double coastEdge = SEA_LEVEL + 0.25 + detail * 0.20;
        double inlandElevation = SEA_LEVEL + 4.8 + continental * 16.0 + detail * 4.0 + mountainWeight * mountainWeight * 72.0;
        double baseElevation = coastEdge * (1.0 - tCoast) + inlandElevation * tCoast;

        // Smoothly blended Mesa plateaus (only inside core Mesa climate region)
        double mesaTempW = smoothstep(0.24, 0.30, temp);
        double mesaHumW = smoothstep(-0.15, -0.11, humidity) * (1.0 - smoothstep(-0.06, -0.02, humidity));
        double mesaWeight = mesaTempW * mesaHumW * tCoast;
        if (mesaWeight > 0.001) {
            double step = detail * 3.6;
            double fl = Math.floor(step);
            double smoothTerrace = fl + smoothstep(0.30, 0.70, step - fl);
            double plateau = Math.max(0.0, smoothTerrace * 4.2);
            baseElevation += plateau * mesaWeight;
        }

        // Anti-terracing micro-dither: eliminates flat concentric steps on shallow slopes
        double elevation = baseElevation;

        // Smoothly carve riverbeds into inland terrain down to SEA_LEVEL - 4
        if (oceanContinental >= -0.055) {
            double riverVal = sampleRiver(noise, wx, wz);
            if (riverVal < RIVER_THRESHOLD) {
                double rFactor = smoothstep(0.0, 1.0, (RIVER_THRESHOLD - riverVal) / RIVER_THRESHOLD);
                double mDamp = 1.0 - smoothstep(0.28, 0.60, mountainWeight) * 0.40;
                rFactor *= mDamp;

                double riverBedFloor = SEA_LEVEL - 3.8 + detail * 0.5;
                if (elevation > riverBedFloor) {
                    elevation = elevation * (1.0 - rFactor) + riverBedFloor * rFactor;
                }
            }
        }

        return (int) Math.round(elevation + microDither);
    }

    /**
     * Pure 2D check for whether a surface cave entrance is allowed at (wx, wz).
     */
    private static boolean isCaveEntranceColumn(SimplexNoise noise, long wx, long wz) {
        return noise.noise2D(wx * 0.012 + 410.0, wz * 0.012 - 370.0) > 0.31;
    }

    /**
     * Checks whether the surface block itself (wx, surfaceY, wz) is actually carved out by a 3D cave entrance.
     * Independent of chunkY so all vertical chunks agree on tree placement without creating large treeless patches.
     */
    public static boolean isSurfaceCarvedByCave(SimplexNoise noise, long wx, int surfaceY, long wz) {
        if (surfaceY <= SEA_LEVEL + 3 || !isCaveEntranceColumn(noise, wx, wz)) {
            return false;
        }
        for (int dy = 0; dy <= 1; dy++) {
            long wy = surfaceY - dy;
            double n1 = noise.noise3D(wx * 0.032, wy * 0.040, wz * 0.032);
            double n2 = noise.noise3D(wx * 0.032 + 73.1, wy * 0.040 + 39.7, wz * 0.032 - 51.3);
            double entranceScale = 0.45 + 0.55 * (dy / 11.0);
            if (n1 * n1 + n2 * n2 < 0.015 * entranceScale) {
                return true;
            }
        }
        return false;
    }

    private static boolean isCaveFromSamples(double n1, double n2, double cavern,
                                             long wy, int surfaceHeight, int minChunkSurfaceY,
                                             boolean allowSurfaceEntrance) {
        if (wy > surfaceHeight) return false;
        // Prevent caves from opening holes into ocean water or sloped shorelines
        if (surfaceHeight <= SEA_LEVEL + 3 && wy > Math.min(surfaceHeight - 6, minChunkSurfaceY - 4)) {
            return false;
        }

        long depth = surfaceHeight - wy;
        if (depth < 11 && !allowSurfaceEntrance) {
            return false;
        }

        double entranceScale = (depth < 11) ? (0.45 + 0.55 * (depth / 11.0)) : 1.0;

        double tunnel = n1 * n1 + n2 * n2;
        if (tunnel < 0.015 * entranceScale) {
            return true;
        }

        if (depth > 8 && cavern > 0.44) {
            return true;
        }

        return false;
    }

    public static final class GeneratorContext {
        public final int[] heightMap = new int[SIZE_X * SIZE_Z];
        public final Biome[] biomeMap = new Biome[SIZE_X * SIZE_Z];
        public final boolean[] entranceMask = new boolean[SIZE_X * SIZE_Z];
        public final double[] sampleN1 = new double[5 * 5 * 5];
        public final double[] sampleN2 = new double[5 * 5 * 5];
        public final double[] sampleCav = new double[5 * 5 * 5];
        public final boolean[] caveGrid = new boolean[SIZE_X * SIZE_Y * SIZE_Z];
        public final boolean[] steepCliff = new boolean[SIZE_X * SIZE_Z];

        public long lastColX = Long.MIN_VALUE;
        public long lastColZ = Long.MIN_VALUE;
    }

    private static final ThreadLocal<GeneratorContext> TL_GEN_CTX = ThreadLocal.withInitial(GeneratorContext::new);

    public static void generateChunkTerrain(Chunk chunk, SimplexNoise noise) {
        chunk.clearBlocks();

        long baseX = chunk.chunkX * SIZE_X;
        long baseY = chunk.chunkY * SIZE_Y;
        long baseZ = chunk.chunkZ * SIZE_Z;
        long topY = baseY + SIZE_Y - 1;

        GeneratorContext ctx = TL_GEN_CTX.get();
        int maxSurfaceY = Integer.MIN_VALUE;
        int minSurfaceY = Integer.MAX_VALUE;

        if (ctx.lastColX == chunk.chunkX && ctx.lastColZ == chunk.chunkZ) {
            for (int i = 0; i < SIZE_X * SIZE_Z; i++) {
                int h = ctx.heightMap[i];
                if (h > maxSurfaceY) maxSurfaceY = h;
                if (h < minSurfaceY) minSurfaceY = h;
            }
        } else {
            ctx.lastColX = chunk.chunkX;
            ctx.lastColZ = chunk.chunkZ;
            for (int lx = 0; lx < SIZE_X; lx++) {
                long wx = baseX + lx;
                int rowOffset = lx << 4;
                for (int lz = 0; lz < SIZE_Z; lz++) {
                    long wz = baseZ + lz;
                    int h = computeSurfaceHeight(noise, wx, wz);
                    int idx = rowOffset | lz;
                    ctx.heightMap[idx] = h;
                    ctx.biomeMap[idx] = computeBiome(noise, wx, wz);
                    if (h > maxSurfaceY) maxSurfaceY = h;
                    if (h < minSurfaceY) minSurfaceY = h;
                    ctx.entranceMask[idx] = isCaveEntranceColumn(noise, wx, wz);
                }
            }
        }

        // Fast path: chunk is high in the sky above terrain, water, and tallest trees
        if (baseY > Math.max(maxSurfaceY + 16, SEA_LEVEL + 1)) {
            chunk.dirty = false;
            return;
        }

        for (int sx = 0; sx <= 4; sx++) {
            long wx = baseX + sx * 4;
            int sxOff = sx * 25;
            for (int sy = 0; sy <= 4; sy++) {
                long wy = baseY + sy * 4;
                int syOff = sxOff + sy * 5;
                for (int sz = 0; sz <= 4; sz++) {
                    long wz = baseZ + sz * 4;
                    int idx = syOff + sz;
                    ctx.sampleN1[idx] = noise.noise3D(wx * 0.032, wy * 0.040, wz * 0.032);
                    ctx.sampleN2[idx] = noise.noise3D(wx * 0.032 + 73.1, wy * 0.040 + 39.7, wz * 0.032 - 51.3);
                    ctx.sampleCav[idx] = noise.noise3D(wx * 0.022 - 120.0, wy * 0.028, wz * 0.022 + 95.0);
                }
            }
        }

        java.util.Arrays.fill(ctx.caveGrid, false);

        for (int lx = 0; lx < SIZE_X; lx++) {
            int sx = lx >> 2;
            double fx = (lx & 3) * 0.25;
            int lxOff = lx << 8;
            int rowOffset = lx << 4;
            for (int lz = 0; lz < SIZE_Z; lz++) {
                int sz = lz >> 2;
                double fz = (lz & 3) * 0.25;
                int idx2 = rowOffset | lz;
                int height = ctx.heightMap[idx2];
                boolean allowEntrance = ctx.entranceMask[idx2];
                int colIdx = lxOff | lz;

                for (int ly = 0; ly < SIZE_Y; ly++) {
                    long wy = baseY + ly;
                    if (wy > height) continue;

                    int sy = ly >> 2;
                    double fy = (ly & 3) * 0.25;

                    double n1 = trilinear(ctx.sampleN1, sx, sy, sz, fx, fy, fz);
                    double n2 = trilinear(ctx.sampleN2, sx, sy, sz, fx, fy, fz);
                    double cav = trilinear(ctx.sampleCav, sx, sy, sz, fx, fy, fz);

                    ctx.caveGrid[colIdx | (ly << 4)] = isCaveFromSamples(n1, n2, cav, wy, height, minSurfaceY, allowEntrance);
                }
            }
        }

        for (int lx = 0; lx < SIZE_X; lx++) {
            int rowOffset = lx << 4;
            for (int lz = 0; lz < SIZE_Z; lz++) {
                int h = ctx.heightMap[rowOffset | lz];
                int maxD = 0;
                if (lx > 0) maxD = Math.max(maxD, Math.abs(h - ctx.heightMap[((lx - 1) << 4) | lz]));
                if (lx < SIZE_X - 1) maxD = Math.max(maxD, Math.abs(h - ctx.heightMap[((lx + 1) << 4) | lz]));
                if (lz > 0) maxD = Math.max(maxD, Math.abs(h - ctx.heightMap[rowOffset | (lz - 1)]));
                if (lz < SIZE_Z - 1) maxD = Math.max(maxD, Math.abs(h - ctx.heightMap[rowOffset | (lz + 1)]));
                ctx.steepCliff[rowOffset | lz] = (maxD >= 4);
            }
        }

        for (int lx = 0; lx < SIZE_X; lx++) {
            long wx = baseX + lx;
            int lxOff = lx << 8;
            int rowOffset = lx << 4;
            for (int lz = 0; lz < SIZE_Z; lz++) {
                long wz = baseZ + lz;
                int idx2 = rowOffset | lz;
                int height = ctx.heightMap[idx2];
                Biome biome = ctx.biomeMap[idx2];
                int colIdx = lxOff | lz;

                for (int ly = 0; ly < SIZE_Y; ly++) {
                    long wy = baseY + ly;

                    if (wy > height) {
                        if (wy <= SEA_LEVEL) {
                            if (wy == SEA_LEVEL && (biome == Biome.SNOWY_TUNDRA || biome == Biome.FROZEN_OCEAN)) {
                                chunk.setBlock(lx, ly, lz, BlockType.ICE.id);
                            } else {
                                chunk.setBlock(lx, ly, lz, BlockType.WATER.id);
                            }
                        }
                        continue;
                    }

                    if (ctx.caveGrid[colIdx | (ly << 4)]) {
                        continue;
                    }

                    byte blockId = (ctx.steepCliff[idx2] && wy == height && height > SEA_LEVEL)
                            ? BlockType.STONE.id
                            : selectBiomeTerrainBlock(noise, biome, wx, wy, wz, height, ly, ctx, lx, lz);
                    chunk.setBlock(lx, ly, lz, blockId);
                }
            }
        }

        // Plant biome-specific vegetation using a jittered 5x5 world grid (~4-5 block spacing)
        // Includes a 4-block search margin around the chunk so large canopies cross X/Y/Z chunk borders seamlessly
        if (topY >= SEA_LEVEL && baseY <= 90) {
            long minWx = baseX - TREE_SEARCH_MARGIN;
            long maxWx = baseX + SIZE_X - 1 + TREE_SEARCH_MARGIN;
            long minWz = baseZ - TREE_SEARCH_MARGIN;
            long maxWz = baseZ + SIZE_Z - 1 + TREE_SEARCH_MARGIN;

            long minCellX = Math.floorDiv(minWx, (long) TREE_CELL_SIZE);
            long maxCellX = Math.floorDiv(maxWx, (long) TREE_CELL_SIZE);
            long minCellZ = Math.floorDiv(minWz, (long) TREE_CELL_SIZE);
            long maxCellZ = Math.floorDiv(maxWz, (long) TREE_CELL_SIZE);

            for (long cellX = minCellX; cellX <= maxCellX; cellX++) {
                for (long cellZ = minCellZ; cellZ <= maxCellZ; cellZ++) {
                    int cellHash = hashCoords(cellX, cellZ);
                    // Offset 1..2 inside each 5x5 cell guarantees distance between trees is always 4..6 blocks
                    long treeWx = cellX * TREE_CELL_SIZE + 1L + (cellHash & 1);
                    long treeWz = cellZ * TREE_CELL_SIZE + 1L + ((cellHash >>> 2) & 1);

                    if (treeWx >= minWx && treeWx <= maxWx && treeWz >= minWz && treeWz <= maxWz) {
                        int lx = (int) (treeWx - baseX);
                        int lz = (int) (treeWz - baseZ);

                        int surfaceY;
                        Biome biome;
                        if (lx >= 0 && lx < SIZE_X && lz >= 0 && lz < SIZE_Z) {
                            int idx2 = (lx << 4) | lz;
                            surfaceY = ctx.heightMap[idx2];
                            biome = ctx.biomeMap[idx2];
                        } else {
                            surfaceY = computeSurfaceHeight(noise, treeWx, treeWz);
                            biome = computeBiome(noise, treeWx, treeWz);
                        }

                        // Place trees on terrain up to subalpine mountain zone (underwater/beaches excluded)
                        if (surfaceY > SEA_LEVEL && surfaceY < 105
                                && biome != Biome.BEACH
                                && !isSurfaceCarvedByCave(noise, treeWx, surfaceY, treeWz)) {
                            int treeHash = hashCoords(treeWx, treeWz);
                            generateBiomeFlora(chunk, biome, treeHash, lx, surfaceY + 1L, lz, baseY, topY);
                        }
                    }
                }
            }
        }

        // Dense natural ground foliage pass (Tall Grass & Flowers) covering all columns
        if (topY >= SEA_LEVEL && baseY <= 90) {
            for (int lz = 0; lz < SIZE_Z; lz++) {
                for (int lx = 0; lx < SIZE_X; lx++) {
                    int idx2 = (lx << 4) | lz;
                    int surfaceY = ctx.heightMap[idx2];
                    long decorY = surfaceY + 1L;
                    if (decorY < baseY || decorY > topY) continue;
                    if (surfaceY <= SEA_LEVEL || surfaceY >= 86) continue;

                    int ly = (int) (decorY - baseY);
                    if (chunk.getBlock(lx, ly, lz) != BlockType.AIR.id) continue;

                    long wx = baseX + lx;
                    long wz = baseZ + lz;
                    if (isSurfaceCarvedByCave(noise, wx, surfaceY, wz)) continue;

                    // Sugar cane along water edges (rivers, shores, lake borders)
                    if (surfaceY >= SEA_LEVEL && surfaceY <= SEA_LEVEL + 1) {
                        byte checkBelow = (ly > 0) ? chunk.getBlock(lx, ly - 1, lz)
                                : selectBiomeTerrainBlock(noise, ctx.biomeMap[idx2], wx, surfaceY, wz, surfaceY, Chunk.SIZE_Y - 1, null, lx, lz);
                        if (checkBelow == BlockType.SAND.id || checkBelow == BlockType.DIRT.id
                                || checkBelow == BlockType.GRASS.id || checkBelow == BlockType.RED_SAND.id) {
                            boolean nearWater = false;
                            long[][] cardinal = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
                            for (long[] d : cardinal) {
                                int nH = computeSurfaceHeight(noise, wx + d[0], wz + d[1]);
                                if (nH < SEA_LEVEL) { // neighbor is water
                                    nearWater = true;
                                    break;
                                }
                            }
                            if (nearWater) {
                                int caneHash = hashCoords(wx, wz);
                                if (((caneHash >>> 7) & 0xFF) < 60) { // ~23% frequency along banks
                                    int caneH = ((caneHash >>> 12) & 1) == 0 ? 2 : 3;
                                    for (int dy = 0; dy < caneH; dy++) {
                                        int cLy = ly + dy;
                                        if (cLy < Chunk.SIZE_Y) {
                                            chunk.setBlock(lx, cLy, lz, BlockType.SUGAR_CANE.id);
                                        }
                                    }
                                    continue;
                                }
                            }
                        }
                    }

                    // Trawa NIE MOŻE rosnąć na kamieniu ani podłożu niesprzyjającym
                    if (ctx.steepCliff[idx2]) continue;

                    byte belowBlock;
                    if (ly > 0) {
                        belowBlock = chunk.getBlock(lx, ly - 1, lz);
                    } else {
                        belowBlock = selectBiomeTerrainBlock(noise, ctx.biomeMap[idx2], wx, surfaceY, wz, surfaceY, Chunk.SIZE_Y - 1, null, lx, lz);
                    }

                    boolean isBeachSand = (belowBlock == BlockType.SAND.id && ctx.biomeMap[idx2] == Biome.BEACH);

                    if (belowBlock == BlockType.STONE.id || belowBlock == BlockType.COBBLESTONE.id
                            || (belowBlock == BlockType.SAND.id && !isBeachSand) || belowBlock == BlockType.SANDSTONE.id
                            || belowBlock == BlockType.TERRACOTTA.id || belowBlock == BlockType.RED_SAND.id
                            || belowBlock == BlockType.AIR.id || belowBlock == BlockType.WATER.id
                            || belowBlock == BlockType.ICE.id) {
                        continue;
                    }

                    if (belowBlock != BlockType.GRASS.id && belowBlock != BlockType.JUNGLE_GRASS.id
                            && belowBlock != BlockType.TAIGA_GRASS.id && belowBlock != BlockType.SNOWY_GRASS.id
                            && belowBlock != BlockType.DIRT.id
                            && !isBeachSand) {
                        continue;
                    }

                    Biome biome = ctx.biomeMap[idx2];
                    int hash = hashCoords(wx, wz);
                    int chance = (hash >>> 8) & 0xFF; // 0..255

                    byte decorId = 0;
                    switch (biome) {
                        case BEACH -> {
                            // Sparse coastal beach marram grass (~5% coverage)
                            if (chance < 14) decorId = BlockType.TALL_GRASS.id;
                        }
                        case JUNGLE -> {
                            // Extra lush, overgrown tropical vegetation (68% total coverage)
                            if (chance < 135) decorId = BlockType.TALL_GRASS.id;
                            else if (chance < 175) {
                                decorId = ((hash & 0x400) == 0) ? BlockType.FLOWER.id : BlockType.BLUE_ORCHID.id;
                            }
                        }
                        case PLAINS -> {
                            // Dense waving prairie grass (58% coverage) with flower meadow patches
                            double flowerNoise = noise.noise2D(wx * 0.06, wz * 0.06);
                            if (flowerNoise > 0.32) {
                                if (chance < 160) {
                                    if ((hash & 3) == 0) {
                                        decorId = BlockType.TALL_GRASS.id;
                                    } else {
                                        int fType = (hash >>> 10) & 7;
                                        decorId = switch (fType) {
                                            case 0, 1 -> BlockType.DANDELION.id;
                                            case 2, 3 -> BlockType.ROSE.id;
                                            case 4 -> BlockType.BLUE_ORCHID.id;
                                            case 5 -> BlockType.ALLIUM.id;
                                            default -> BlockType.FLOWER.id;
                                        };
                                    }
                                }
                            } else {
                                if (chance < 130) decorId = BlockType.TALL_GRASS.id;
                                else if (chance < 148) {
                                    int fType = (hash >>> 10) & 3;
                                    decorId = (fType == 0) ? BlockType.DANDELION.id : ((fType == 1) ? BlockType.ROSE.id : BlockType.FLOWER.id);
                                }
                            }
                        }
                        case FOREST, BIRCH_FOREST -> {
                            // Lush woodland undergrowth (44% coverage)
                            if (chance < 100) decorId = BlockType.TALL_GRASS.id;
                            else if (chance < 114) {
                                int fType = (hash >>> 10) & 3;
                                decorId = switch (fType) {
                                    case 0 -> BlockType.ROSE.id;
                                    case 1 -> BlockType.ALLIUM.id;
                                    case 2 -> BlockType.DANDELION.id;
                                    default -> BlockType.FLOWER.id;
                                };
                            }
                        }
                        case TAIGA -> {
                            // Pine needles, ferns and wild tall grass (36% coverage)
                            if (chance < 90) decorId = BlockType.TALL_GRASS.id;
                            else if (chance < 96) {
                                int fType = (hash >>> 10) & 1;
                                decorId = (fType == 0) ? BlockType.BLUE_ORCHID.id : BlockType.FLOWER.id;
                            }
                        }
                        case MOUNTAINS -> {
                            // Green mountain meadows below rock peaks (30% coverage)
                            if (chance < 70) decorId = BlockType.TALL_GRASS.id;
                            else if (chance < 82) {
                                int fType = (hash >>> 10) & 3;
                                decorId = switch (fType) {
                                    case 0 -> BlockType.BLUE_ORCHID.id;
                                    case 1 -> BlockType.ROSE.id;
                                    default -> BlockType.DANDELION.id;
                                };
                            }
                        }
                        case SNOWY_TUNDRA -> {
                            // Cold tundra grass tufts (14% coverage)
                            if (chance < 36) decorId = BlockType.TALL_GRASS.id;
                        }
                        default -> {
                            // DESERT, MESA, OCEANS
                        }
                    }

                    if (decorId != 0) {
                        chunk.setBlock(lx, ly, lz, decorId);
                    }
                }
            }
        }

        // Underwater flora pass (Seagrass, Kelp, Coral) in oceans and rivers
        if (baseY <= SEA_LEVEL && topY >= SEA_LEVEL - 24) {
            for (int lz = 0; lz < SIZE_Z; lz++) {
                for (int lx = 0; lx < SIZE_X; lx++) {
                    int idx2 = (lx << 4) | lz;
                    int surfaceY = ctx.heightMap[idx2];
                    if (surfaceY >= SEA_LEVEL - 1) continue; // must be submerged underwater

                    long decorY = surfaceY + 1L;
                    if (decorY < baseY || decorY > topY) continue;
                    int ly = (int) (decorY - baseY);
                    if (chunk.getBlock(lx, ly, lz) != BlockType.WATER.id) continue;

                    long wx = baseX + lx;
                    long wz = baseZ + lz;
                    Biome biome = ctx.biomeMap[idx2];
                    if (biome == Biome.SNOWY_TUNDRA || biome == Biome.FROZEN_OCEAN) continue;

                    int waterDepth = SEA_LEVEL - surfaceY;
                    int uHash = hashCoords(wx, wz);
                    int chance = (uHash >>> 8) & 0xFF;

                    double temp = sampleTemperature(noise, wx, wz);

                    // 1. Coral reefs in warm ocean waters
                    if (temp > 0.16 && (biome == Biome.OCEAN || biome == Biome.DEEP_OCEAN) && waterDepth >= 3 && waterDepth <= 10) {
                        double coralNoise = noise.noise2D(wx * 0.08, wz * 0.08);
                        if (coralNoise > 0.28) {
                            chunk.setBlock(lx, ly, lz, BlockType.CORAL.id);
                            if (waterDepth >= 5 && ly + 1 < Chunk.SIZE_Y && ((uHash & 1) == 0)) {
                                chunk.setBlock(lx, ly + 1, lz, BlockType.CORAL.id);
                            }
                            continue;
                        }
                    }

                    // 2. Kelp forest columns in deeper ocean waters (3 to 7 blocks tall)
                    if (waterDepth >= 5 && (biome == Biome.OCEAN || biome == Biome.DEEP_OCEAN)) {
                        double kelpNoise = noise.noise2D(wx * 0.045, wz * 0.045);
                        if (kelpNoise > 0.20 && chance < 170) {
                            int kelpH = Math.min(waterDepth - 2, 3 + (uHash & 3));
                            for (int dy = 0; dy < kelpH; dy++) {
                                int kLy = ly + dy;
                                if (kLy < Chunk.SIZE_Y && (baseY + kLy) < SEA_LEVEL) {
                                    chunk.setBlock(lx, kLy, lz, BlockType.KELP.id);
                                }
                            }
                            continue;
                        }
                    }

                    // 3. Seagrass on shallow to mid ocean floor and riverbeds
                    if (waterDepth >= 2 && waterDepth <= 14) {
                        if (chance < 85) { // ~33% coverage
                            chunk.setBlock(lx, ly, lz, BlockType.SEAGRASS.id);
                        }
                    }
                }
            }
        }

        chunk.dirty = !chunk.isAllAir();
    }

    private static byte selectBiomeTerrainBlock(SimplexNoise noise, Biome biome, long wx, long wy, long wz,
                                                int height, int ly, GeneratorContext ctx, int lx, int lz) {
        int hJitter = (hashCoords(wx, wz) & 3) - 1; // -1..2 dither for natural snow/stone height transitions
        int dither = hashCoords(wx, wz) & 7;
        if (wy == height) {
            // Underwater floor (oceans and submerged lake/river beds beneath sea level)
            if (height < SEA_LEVEL) {
                if (biome == Biome.DEEP_OCEAN && height < SEA_LEVEL - 10) {
                    int h = hashCoords(wx, wz);
                    if ((h & 0xFF) == 0) return BlockType.GLOWSTONE.id;
                    return ((h & 3) == 0) ? BlockType.GRAVEL.id : BlockType.SAND.id;
                }
                if (biome == Biome.FROZEN_OCEAN) {
                    return ((dither & 1) == 0) ? BlockType.GRAVEL.id : BlockType.DIRT.id; // Cold stony/gravel seabed
                }
                if (biome == Biome.OCEAN || biome == Biome.BEACH) {
                    return ((dither & 3) == 0) ? BlockType.GRAVEL.id : BlockType.SAND.id;
                }
                if (biome == Biome.RIVER) {
                    int mod = dither & 7;
                    if (mod < 3) return BlockType.GRAVEL.id;
                    if (mod == 3) return BlockType.CLAY.id;
                    if (mod < 6) return BlockType.SAND.id;
                    return BlockType.DIRT.id;
                }
                // Inland submerged waterbed (Minecraft-style: gravel/clay/sand/dirt)
                int mod = dither & 3;
                if (mod == 0) return BlockType.GRAVEL.id;
                if (mod == 1) return BlockType.CLAY.id;
                return BlockType.SAND.id;
            }

            // Mountain peaks (high altitude perpetual snow and exposed rock)
            if (height + hJitter >= 94) return BlockType.SNOW.id;
            if (height + hJitter >= 84) return BlockType.STONE.id;

            // Coastal beach with natural edge dithering into grass and pebble patches
            if (biome == Biome.BEACH) {
                if (height >= SEA_LEVEL + 2 && dither < 3) {
                    return BlockType.GRASS.id; // Feathered organic blend into land
                }
                if (height == SEA_LEVEL + 1 && dither == 0) {
                    return BlockType.COBBLESTONE.id; // Natural pebble patch on beach
                }
                return BlockType.SAND.id;
            }

            return switch (biome) {
                case DESERT -> BlockType.SAND.id;
                case MESA -> (wy > SEA_LEVEL + 11) ? BlockType.TERRACOTTA.id : BlockType.RED_SAND.id;
                case MOUNTAINS -> (height >= 50) ? BlockType.TAIGA_GRASS.id : BlockType.GRASS.id;
                case SNOWY_TUNDRA -> {
                    if (height <= SEA_LEVEL + 2) {
                        // Arctic shore: pack ice, snow, and frozen stone boulders
                        if (dither < 4) yield BlockType.ICE.id;
                        if (dither < 6) yield BlockType.SNOW.id;
                        yield BlockType.COBBLESTONE.id;
                    }
                    yield (height + hJitter >= 32) ? BlockType.SNOWY_GRASS.id : BlockType.SNOW.id;
                }
                case TAIGA -> {
                    if (height <= SEA_LEVEL + 2) {
                        // Cold rocky shore (Stony Shore): cobblestone boulders, gravel, and stone
                        if (dither < 4) yield BlockType.COBBLESTONE.id;
                        if (dither < 6) yield BlockType.STONE.id;
                    }
                    yield BlockType.TAIGA_GRASS.id;
                }
                case JUNGLE -> BlockType.JUNGLE_GRASS.id;
                case PLAINS, FOREST, BIRCH_FOREST -> {
                    // Natural dithering of sand tufts into coastal grassland edge
                    if (height <= SEA_LEVEL + 2 && dither == 7) {
                        yield BlockType.SAND.id;
                    }
                    yield BlockType.GRASS.id;
                }
                default -> BlockType.GRASS.id;
            };
        } else if (wy >= height - 4) {
            if (height + hJitter >= 94) return (wy >= height - 2) ? BlockType.SNOW.id : BlockType.STONE.id;
            if (height + hJitter >= 84) return BlockType.STONE.id;
            return switch (biome) {
                case BEACH, DESERT -> (wy >= height - 2) ? BlockType.SAND.id : BlockType.SANDSTONE.id;
                case MESA -> (wy > SEA_LEVEL + 9) ? BlockType.TERRACOTTA.id : BlockType.RED_SAND.id;
                default -> {
                    // Coastal scarp transition: prevent raw dirt faces from showing on 1-block steps next to beach
                    if (height <= SEA_LEVEL + 3 && wy >= height - 3) {
                        double cont = sampleContinental(noise, wx, wz);
                        if (cont >= -0.055 && cont <= -0.010) {
                            yield (wy >= height - 1) ? BlockType.SAND.id : BlockType.SANDSTONE.id;
                        }
                    }
                    yield (height < SEA_LEVEL && (biome == Biome.OCEAN || biome == Biome.DEEP_OCEAN))
                            ? BlockType.SAND.id : BlockType.DIRT.id;
                }
            };
        } else {
            int h3 = hash3D(wx, wy, wz);
            boolean caveBelow = (ly > 0) && (ctx != null) && ctx.caveGrid[(lx << 8) | ((ly - 1) << 4) | lz];
            if (wy < height - 8 && caveBelow && (h3 & 0x7F) == 0) {
                return BlockType.GLOWSTONE.id;
            } else if (biome == Biome.MESA && wy >= height - 10) {
                return BlockType.TERRACOTTA.id;
            } else if (biome == Biome.DESERT && wy >= height - 8) {
                return BlockType.SANDSTONE.id;
            } else if ((h3 & 0x1F) == 0) {
                return BlockType.COBBLESTONE.id;
            } else {
                return BlockType.STONE.id;
            }
        }
    }

    private static void generateBiomeFlora(Chunk chunk, Biome biome, int hash,
                                           int lx, long floraBaseY, int lz, long baseY, long topY) {
        int chance256 = (hash >>> 4) & 0xFF; // 0..255

        switch (biome) {
            case DESERT, MESA -> {
                int threshold = (biome == Biome.DESERT) ? 68 : 38;
                if (chance256 < threshold) {
                    int cactusHeight = 2 + ((hash >>> 12) % 3);
                    if (topY >= floraBaseY && baseY <= floraBaseY + cactusHeight) {
                        placeCactusSlice(chunk, lx, floraBaseY, lz, cactusHeight, baseY);
                    }
                }
            }
            case TAIGA, SNOWY_TUNDRA -> {
                int threshold = (biome == Biome.TAIGA) ? 180 : 42;
                if (chance256 < threshold) {
                    int trunkHeight = 6 + ((hash >>> 12) & 1);
                    if (topY >= floraBaseY - 1 && baseY <= floraBaseY + trunkHeight + 2) {
                        placeSpruceTreeSlice(chunk, lx, floraBaseY, lz, trunkHeight, baseY);
                    }
                }
            }
            case BIRCH_FOREST -> {
                if (chance256 < 172) {
                    int trunkHeight = 5 + ((hash >>> 12) & 1);
                    if (topY >= floraBaseY - 1 && baseY <= floraBaseY + trunkHeight + 2) {
                        placeStandardTreeSlice(chunk, lx, floraBaseY, lz, trunkHeight, baseY,
                                BlockType.BIRCH_WOOD.id, BlockType.BIRCH_LEAVES.id);
                    }
                }
            }
            case FOREST -> {
                if (chance256 < 185) {
                    int trunkHeight = 4 + ((hash >>> 12) & 1);
                    boolean isBirch = ((hash & 0x4000) != 0);
                    byte woodId = isBirch ? BlockType.BIRCH_WOOD.id : BlockType.WOOD.id;
                    byte leavesId = isBirch ? BlockType.BIRCH_LEAVES.id : BlockType.LEAVES.id;
                    if (topY >= floraBaseY - 1 && baseY <= floraBaseY + trunkHeight + 2) {
                        placeStandardTreeSlice(chunk, lx, floraBaseY, lz, trunkHeight, baseY,
                                woodId, leavesId);
                    }
                }
            }
            case JUNGLE -> {
                // Standout Jungle: Giant towering trees with hanging vines, medium trees, and bushes!
                int jungleTreeType = (hash >>> 10) % 10;
                if (jungleTreeType < 5) {
                    // 50% Giant jungle tree (Height 13-18 blocks, canopy radius 3-4, hanging vines!)
                    int trunkHeight = 13 + ((hash >>> 14) % 6);
                    if (topY >= floraBaseY - 1 && baseY <= floraBaseY + trunkHeight + 3) {
                        placeGiantJungleTreeSlice(chunk, lx, floraBaseY, lz, trunkHeight, baseY);
                    }
                } else if (jungleTreeType < 8) {
                    // 30% Medium jungle tree (Height 7-9 blocks with vines)
                    int trunkHeight = 7 + ((hash >>> 14) % 3);
                    if (topY >= floraBaseY - 1 && baseY <= floraBaseY + trunkHeight + 2) {
                        placeJungleTreeSlice(chunk, lx, floraBaseY, lz, trunkHeight, baseY);
                    }
                } else {
                    // 20% Jungle bush (dense undergrowth)
                    if (topY >= floraBaseY - 1 && baseY <= floraBaseY + 3) {
                        placeJungleBushSlice(chunk, lx, floraBaseY, lz, baseY);
                    }
                }
            }
            case MOUNTAINS -> {
                // Taiga spruce trees on mountains (dense on slopes, rarer on high snow peaks)
                boolean isHighSnowy = (floraBaseY >= 88);
                int threshold = isHighSnowy ? 28 : 145;
                if (chance256 < threshold) {
                    int trunkHeight = 6 + ((hash >>> 12) & 1);
                    if (topY >= floraBaseY - 1 && baseY <= floraBaseY + trunkHeight + 2) {
                        placeSpruceTreeSlice(chunk, lx, floraBaseY, lz, trunkHeight, baseY);
                    }
                }
            }
            case PLAINS -> {
                if (chance256 < 22) {
                    int trunkHeight = 4 + ((hash >>> 12) & 1);
                    if (topY >= floraBaseY - 1 && baseY <= floraBaseY + trunkHeight + 2) {
                        placeStandardTreeSlice(chunk, lx, floraBaseY, lz, trunkHeight, baseY,
                                BlockType.WOOD.id, BlockType.LEAVES.id);
                    }
                }
            }
            default -> {
                // OCEAN, DEEP_OCEAN, FROZEN_OCEAN
            }
        }
    }

    private static void placeGiantJungleTreeSlice(Chunk chunk, int lx, long treeBaseY, int lz, int trunkHeight, long chunkBaseY) {
        // Broad Canopy of JUNGLE_LEAVES
        for (int dy = trunkHeight - 2; dy <= trunkHeight + 1; dy++) {
            long ly = (treeBaseY + dy) - chunkBaseY;
            if (ly < 0 || ly >= SIZE_Y) continue;
            int radius;
            if (dy == trunkHeight + 1) radius = 2;
            else if (dy == trunkHeight) radius = 3;
            else if (dy == trunkHeight - 1) radius = 3;
            else radius = 2;

            for (int dx = -radius; dx <= radius; dx++) {
                int bx = lx + dx;
                if (bx < 0 || bx >= SIZE_X) continue;
                for (int dz = -radius; dz <= radius; dz++) {
                    int bz = lz + dz;
                    if (bz < 0 || bz >= SIZE_Z) continue;
                    if (Math.abs(dx) == radius && Math.abs(dz) == radius && dy != trunkHeight) {
                        continue;
                    }
                    if (chunk.getBlock(bx, (int) ly, bz) == BlockType.AIR.id) {
                        chunk.setBlock(bx, (int) ly, bz, BlockType.JUNGLE_LEAVES.id);
                    }
                }
            }
        }

        // Hanging Vines under the perimeter of the canopy
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                if (Math.abs(dx) == 3 || Math.abs(dz) == 3) {
                    if (Math.abs(dx) == 3 && Math.abs(dz) == 3) continue;
                    int vHash = hashCoords(lx + dx, lz + dz);
                    if ((vHash & 3) != 0) { // 75% chance of vine hanging from canopy border
                        int vineLength = 2 + ((vHash >>> 3) % 4);
                        for (int vdy = 1; vdy <= vineLength; vdy++) {
                            long ly = (treeBaseY + trunkHeight - 2 - vdy) - chunkBaseY;
                            if (ly >= 0 && ly < SIZE_Y) {
                                int bx = lx + dx;
                                int bz = lz + dz;
                                if (bx >= 0 && bx < SIZE_X && bz >= 0 && bz < SIZE_Z) {
                                    if (chunk.getBlock(bx, (int) ly, bz) == BlockType.AIR.id) {
                                        chunk.setBlock(bx, (int) ly, bz, BlockType.VINE.id);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Trunk of JUNGLE_WOOD
        if (lx >= 0 && lx < SIZE_X && lz >= 0 && lz < SIZE_Z) {
            long dirtY = treeBaseY - 1 - chunkBaseY;
            if (dirtY >= 0 && dirtY < SIZE_Y) {
                chunk.setBlock(lx, (int) dirtY, lz, BlockType.DIRT.id);
            }
            for (int dy = 0; dy < trunkHeight; dy++) {
                long ly = (treeBaseY + dy) - chunkBaseY;
                if (ly >= 0 && ly < SIZE_Y) {
                    chunk.setBlock(lx, (int) ly, lz, BlockType.JUNGLE_WOOD.id);
                }
            }
        }

        // Buttress roots at base (dy = 0..1)
        int[][] rootOffsets = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        for (int[] ro : rootOffsets) {
            int rx = lx + ro[0];
            int rz = lz + ro[1];
            if (rx >= 0 && rx < SIZE_X && rz >= 0 && rz < SIZE_Z) {
                for (int dy = 0; dy <= 1; dy++) {
                    long ly = (treeBaseY + dy) - chunkBaseY;
                    if (ly >= 0 && ly < SIZE_Y) {
                        if (chunk.getBlock(rx, (int) ly, rz) == BlockType.AIR.id) {
                            chunk.setBlock(rx, (int) ly, rz, BlockType.JUNGLE_WOOD.id);
                        }
                    }
                }
            }
        }
    }

    private static void placeJungleTreeSlice(Chunk chunk, int lx, long treeBaseY, int lz, int trunkHeight, long chunkBaseY) {
        for (int dy = trunkHeight - 2; dy <= trunkHeight + 1; dy++) {
            long ly = (treeBaseY + dy) - chunkBaseY;
            if (ly < 0 || ly >= SIZE_Y) continue;
            int radius = (dy >= trunkHeight) ? 1 : 2;
            for (int dx = -radius; dx <= radius; dx++) {
                int bx = lx + dx;
                if (bx < 0 || bx >= SIZE_X) continue;
                for (int dz = -radius; dz <= radius; dz++) {
                    int bz = lz + dz;
                    if (bz < 0 || bz >= SIZE_Z) continue;
                    if (Math.abs(dx) == radius && Math.abs(dz) == radius && dy != trunkHeight - 1) {
                        continue;
                    }
                    if (chunk.getBlock(bx, (int) ly, bz) == BlockType.AIR.id) {
                        chunk.setBlock(bx, (int) ly, bz, BlockType.JUNGLE_LEAVES.id);
                    }
                }
            }
        }

        // Hanging vines under perimeter of leaves
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                if (Math.abs(dx) == 2 || Math.abs(dz) == 2) {
                    int vHash = hashCoords(lx + dx, lz + dz);
                    if ((vHash & 3) == 0) {
                        int vineLength = 1 + ((vHash >>> 3) & 1);
                        for (int vdy = 1; vdy <= vineLength; vdy++) {
                            long ly = (treeBaseY + trunkHeight - 2 - vdy) - chunkBaseY;
                            if (ly >= 0 && ly < SIZE_Y) {
                                int bx = lx + dx;
                                int bz = lz + dz;
                                if (bx >= 0 && bx < SIZE_X && bz >= 0 && bz < SIZE_Z) {
                                    if (chunk.getBlock(bx, (int) ly, bz) == BlockType.AIR.id) {
                                        chunk.setBlock(bx, (int) ly, bz, BlockType.VINE.id);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (lx >= 0 && lx < SIZE_X && lz >= 0 && lz < SIZE_Z) {
            long dirtY = treeBaseY - 1 - chunkBaseY;
            if (dirtY >= 0 && dirtY < SIZE_Y) {
                chunk.setBlock(lx, (int) dirtY, lz, BlockType.DIRT.id);
            }
            for (int dy = 0; dy < trunkHeight; dy++) {
                long ly = (treeBaseY + dy) - chunkBaseY;
                if (ly >= 0 && ly < SIZE_Y) {
                    chunk.setBlock(lx, (int) ly, lz, BlockType.JUNGLE_WOOD.id);
                }
            }
        }
    }

    private static void placeJungleBushSlice(Chunk chunk, int lx, long bushBaseY, int lz, long chunkBaseY) {
        if (lx >= 0 && lx < SIZE_X && lz >= 0 && lz < SIZE_Z) {
            long trunkLy = bushBaseY - chunkBaseY;
            if (trunkLy >= 0 && trunkLy < SIZE_Y) {
                chunk.setBlock(lx, (int) trunkLy, lz, BlockType.JUNGLE_WOOD.id);
            }
        }
        for (int dy = 0; dy <= 1; dy++) {
            long ly = (bushBaseY + dy) - chunkBaseY;
            if (ly < 0 || ly >= SIZE_Y) continue;
            int radius = (dy == 1) ? 1 : 2;
            for (int dx = -radius; dx <= radius; dx++) {
                int bx = lx + dx;
                if (bx < 0 || bx >= SIZE_X) continue;
                for (int dz = -radius; dz <= radius; dz++) {
                    int bz = lz + dz;
                    if (bz < 0 || bz >= SIZE_Z) continue;
                    if (Math.abs(dx) == radius && Math.abs(dz) == radius) continue;
                    if (chunk.getBlock(bx, (int) ly, bz) == BlockType.AIR.id) {
                        chunk.setBlock(bx, (int) ly, bz, BlockType.JUNGLE_LEAVES.id);
                    }
                }
            }
        }
    }

    private static void placeCactusSlice(Chunk chunk, int lx, long cactusBaseY, int lz, int height, long chunkBaseY) {
        if (lx < 0 || lx >= SIZE_X || lz < 0 || lz >= SIZE_Z) return;
        for (int dy = 0; dy < height; dy++) {
            long ly = (cactusBaseY + dy) - chunkBaseY;
            if (ly >= 0 && ly < SIZE_Y) {
                chunk.setBlock(lx, (int) ly, lz, BlockType.CACTUS.id);
            }
        }
    }

    private static void placeStandardTreeSlice(Chunk chunk, int lx, long treeBaseY, int lz, int trunkHeight,
                                               long chunkBaseY, byte woodId, byte leavesId) {
        for (int dy = trunkHeight - 2; dy <= trunkHeight + 1; dy++) {
            long ly = (treeBaseY + dy) - chunkBaseY;
            if (ly < 0 || ly >= SIZE_Y) continue;
            int radius = (dy >= trunkHeight) ? 1 : 2;
            for (int dx = -radius; dx <= radius; dx++) {
                int bx = lx + dx;
                if (bx < 0 || bx >= SIZE_X) continue;
                for (int dz = -radius; dz <= radius; dz++) {
                    int bz = lz + dz;
                    if (bz < 0 || bz >= SIZE_Z) continue;
                    if (Math.abs(dx) == radius && Math.abs(dz) == radius && dy != trunkHeight - 1) {
                        continue;
                    }
                    if (chunk.getBlock(bx, (int) ly, bz) == BlockType.AIR.id) {
                        chunk.setBlock(bx, (int) ly, bz, leavesId);
                    }
                }
            }
        }

        if (lx >= 0 && lx < SIZE_X && lz >= 0 && lz < SIZE_Z) {
            long dirtY = treeBaseY - 1 - chunkBaseY;
            if (dirtY >= 0 && dirtY < SIZE_Y) {
                chunk.setBlock(lx, (int) dirtY, lz, BlockType.DIRT.id);
            }

            for (int dy = 0; dy < trunkHeight; dy++) {
                long ly = (treeBaseY + dy) - chunkBaseY;
                if (ly >= 0 && ly < SIZE_Y) {
                    chunk.setBlock(lx, (int) ly, lz, woodId);
                }
            }
        }
    }

    private static void placeSpruceTreeSlice(Chunk chunk, int lx, long treeBaseY, int lz, int trunkHeight, long chunkBaseY) {
        for (int dy = 2; dy <= trunkHeight + 1; dy++) {
            long ly = (treeBaseY + dy) - chunkBaseY;
            if (ly < 0 || ly >= SIZE_Y) continue;
            int radius;
            if (dy == trunkHeight + 1) radius = 0;
            else if (dy == trunkHeight) radius = 1;
            else radius = ((dy % 2 == 0) ? 2 : 1);

            for (int dx = -radius; dx <= radius; dx++) {
                int bx = lx + dx;
                if (bx < 0 || bx >= SIZE_X) continue;
                for (int dz = -radius; dz <= radius; dz++) {
                    int bz = lz + dz;
                    if (bz < 0 || bz >= SIZE_Z) continue;
                    if (radius > 0 && Math.abs(dx) == radius && Math.abs(dz) == radius) {
                        continue;
                    }
                    if (chunk.getBlock(bx, (int) ly, bz) == BlockType.AIR.id) {
                        chunk.setBlock(bx, (int) ly, bz, BlockType.SPRUCE_LEAVES.id);
                    }
                }
            }
        }

        if (lx >= 0 && lx < SIZE_X && lz >= 0 && lz < SIZE_Z) {
            long dirtY = treeBaseY - 1 - chunkBaseY;
            if (dirtY >= 0 && dirtY < SIZE_Y) {
                chunk.setBlock(lx, (int) dirtY, lz, BlockType.DIRT.id);
            }

            for (int dy = 0; dy < trunkHeight; dy++) {
                long ly = (treeBaseY + dy) - chunkBaseY;
                if (ly >= 0 && ly < SIZE_Y) {
                    chunk.setBlock(lx, (int) ly, lz, BlockType.SPRUCE_WOOD.id);
                }
            }
        }
    }

    private static double trilinear(double[] grid, int sx, int sy, int sz, double fx, double fy, double fz) {
        int i000 = sx * 25 + sy * 5 + sz;
        int i100 = i000 + 25;
        int i010 = i000 + 5;
        int i110 = i100 + 5;

        double c00 = grid[i000] * (1.0 - fx) + grid[i100] * fx;
        double c01 = grid[i000 + 1] * (1.0 - fx) + grid[i100 + 1] * fx;
        double c10 = grid[i010] * (1.0 - fx) + grid[i110] * fx;
        double c11 = grid[i010 + 1] * (1.0 - fx) + grid[i110 + 1] * fx;
        double c0 = c00 * (1.0 - fy) + c10 * fy;
        double c1 = c01 * (1.0 - fy) + c11 * fy;
        return c0 * (1.0 - fz) + c1 * fz;
    }

    private static int hashCoords(long x, long z) {
        long h = x * 374761393L + z * 668265263L;
        h = (h ^ (h >> 13)) * 1274126177L;
        return (int) (h ^ (h >> 16));
    }

    private static int hash3D(long x, long y, long z) {
        long h = x * 374761393L + y * 668265263L + z * 2147483647L;
        h = (h ^ (h >> 13)) * 1274126177L;
        return (int) (h ^ (h >> 16));
    }
}
