package com.voxel.world;

import com.voxel.block.BlockType;
import com.voxel.entity.Player;
import com.voxel.math.SimplexNoise;
import com.voxel.render.ShaderProgram;
import com.voxel.util.Logger;
import com.voxel.world.gen.TerrainGenerator;
import org.joml.FrustumIntersection;
import org.joml.Matrix4f;
import org.joml.Vector3d;
import org.joml.Vector3f;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class World {
    public static int renderDistance = 6;
    public static int renderDistanceY = 4;

    public record ChunkOffset(int dx, int dy, int dz, int score) implements Comparable<ChunkOffset> {
        @Override
        public int compareTo(ChunkOffset o) {
            return Integer.compare(this.score, o.score);
        }
    }

    private ChunkOffset[] sortedChunkOffsets = new ChunkOffset[0];

    public static int calculateChunkPriorityScore(int dx, int dy, int dz) {
        int distXZ2 = dx * dx + dz * dz;
        int verticalPenalty;
        if (dy == 0) {
            verticalPenalty = 0;       // Eye level - najwyzszy priorytet
        } else if (dy == -1) {
            verticalPenalty = 1;       // Poziom stop / grunt bezposrednio pod graczem
        } else if (dy == 1) {
            verticalPenalty = 4;       // Nad glowa (korony drzew, klify, wzgorza)
        } else if (dy == -2) {
            verticalPenalty = 16;      // Plytkie podziemia
        } else if (dy < -2) {
            verticalPenalty = 32 + (-dy) * 40; // Glebokie jaskinie - odroczone w tle
        } else {
            verticalPenalty = 28 + dy * 35;    // Wysokie niebo / chmury
        }
        return distXZ2 * 12 + verticalPenalty;
    }

    public synchronized void rebuildSortedChunkOffsets() {
        List<ChunkOffset> list = new ArrayList<>();
        int maxDistSq = (renderDistance + 1) * (renderDistance + 1);
        for (int dy = -renderDistanceY; dy <= renderDistanceY; dy++) {
            for (int dx = -renderDistance; dx <= renderDistance; dx++) {
                for (int dz = -renderDistance; dz <= renderDistance; dz++) {
                    if (dx * dx + dz * dz > maxDistSq) continue;
                    list.add(new ChunkOffset(dx, dy, dz, calculateChunkPriorityScore(dx, dy, dz)));
                }
            }
        }
        Collections.sort(list);
        sortedChunkOffsets = list.toArray(new ChunkOffset[0]);
    }

    public void setRenderDistance(int dist) {
        renderDistance = Math.max(2, Math.min(16, dist));
        renderDistanceY = Math.max(2, Math.min(8, (int) Math.ceil(renderDistance * 0.65)));
        rebuildSortedChunkOffsets();
        Logger.info("World", "Zmieniono dystans renderowania: %d chunkow (Y: %d)", renderDistance, renderDistanceY);
    }

    private long seed;
    private SimplexNoise noise;

    public record ChunkPos(long x, long y, long z) {}
    public record ColumnPos(long x, long z) {}
    public record BlockPos(long x, long y, long z) {}
    public record RaycastHit(long x, long y, long z, long prevX, long prevY, long prevZ, byte blockId) {}

    // Pula watkow roboczych dla wielowatkowej generacji terenu i budowy siatek (Multithreaded Meshing)
    private final int numThreads;
    private final ExecutorService workerPool;
    private final ConcurrentLinkedQueue<com.voxel.world.mesh.ChunkMesher.BuiltChunkMesh> readyMeshes = new ConcurrentLinkedQueue<>();
    private final Set<ChunkPos> generatingChunks = ConcurrentHashMap.newKeySet();
    private final Set<ChunkPos> meshingChunks = ConcurrentHashMap.newKeySet();

    // Metryki wydajnosci i live-profiler
    private final AtomicLong totalChunksGenerated = new AtomicLong();
    private final AtomicLong totalMeshesBuilt = new AtomicLong();
    private final AtomicLong totalMeshesUploaded = new AtomicLong();
    private volatile double avgGenTimeMs = 0.22;
    private volatile double avgMeshTimeMs = 0.75;
    private volatile double avgUploadTimeMs = 0.03;

    private final Map<ChunkPos, Chunk> chunks = new ConcurrentHashMap<>();
    private final Map<ColumnPos, int[]> columnHeightCache = new ConcurrentHashMap<>();
    private final Map<ColumnPos, short[]> columnTopOpaqueCache = new ConcurrentHashMap<>();
    private final Map<BlockPos, Byte> modifiedBlocks = new ConcurrentHashMap<>();
    private final Map<ChunkPos, byte[]> savedRegionChunks = new ConcurrentHashMap<>();
    private final Set<ChunkPos> modifiedChunkPositions = ConcurrentHashMap.newKeySet();
    private final Queue<BlockPos> physicsQueue = new ConcurrentLinkedQueue<>();
    private final Set<BlockPos> inPhysicsQueue = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final Queue<BlockPos> fluidQueueCurrent = new ConcurrentLinkedQueue<>();
    private final Queue<BlockPos> fluidQueueNext = new ConcurrentLinkedQueue<>();
    private final Set<BlockPos> inFluidQueue = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private float fluidTickTimer = 0.0f;
    public static final float FLUID_TICK_INTERVAL = 0.12f;
    private int lastRenderedChunkCount = 0;

    private final FrustumIntersection frustum = new FrustumIntersection();
    private final List<Chunk> visibleOpaque = new ArrayList<>();
    private final List<Chunk> visibleTransparent = new ArrayList<>();

    public float timeOfDay = 0.28f; // 0.0..1.0 (0.25 = sunrise, 0.5 = noon, 0.75 = sunset, 0.0 = midnight)
    public boolean dayCyclePaused = false;

    public World(long seed) {
        this.seed = seed;
        this.noise = new SimplexNoise(seed);
        rebuildSortedChunkOffsets();

        this.numThreads = Math.max(2, Math.min(8, Runtime.getRuntime().availableProcessors() - 1));
        this.workerPool = Executors.newFixedThreadPool(numThreads, new ThreadFactory() {
            private final AtomicInteger id = new AtomicInteger(1);
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, "Voxel-Worker-" + id.getAndIncrement());
                t.setDaemon(true);
                t.setPriority(Thread.NORM_PRIORITY - 1);
                return t;
            }
        });
        Logger.info("World", "Zainicjalizowano wielowatkowa pule robocza: %d watkow CPU", numThreads);
    }

    public long getSeed() {
        return seed;
    }

    public SimplexNoise getNoise() {
        return noise;
    }

    public void resetSeed(long newSeed) {
        this.seed = newSeed;
        this.noise = new SimplexNoise(newSeed);
        this.columnHeightCache.clear();
        this.columnTopOpaqueCache.clear();
    }

    public Map<BlockPos, Byte> getModifiedBlocks() {
        return modifiedBlocks;
    }

    public Map<ChunkPos, byte[]> getSavedRegionChunks() {
        return savedRegionChunks;
    }

    public Set<ChunkPos> getModifiedChunkPositions() {
        return modifiedChunkPositions;
    }

    public void clearSavedRegionChunks() {
        savedRegionChunks.clear();
        modifiedChunkPositions.clear();
    }

    public void clearLoadedChunks() {
        generatingChunks.clear();
        meshingChunks.clear();
        readyMeshes.clear();
        for (Chunk c : chunks.values()) {
            c.cleanup();
        }
        chunks.clear();
    }

    public void resetWorld(Player player, long newSeed) {
        modifiedBlocks.clear();
        modifiedChunkPositions.clear();
        savedRegionChunks.clear();
        resetSeed(newSeed);
        clearLoadedChunks();
        timeOfDay = 0.28f;
        dayCyclePaused = false;

        long spawnY = findSpawnY(0, 0);
        long spawnCy = Math.floorDiv(spawnY, (long) Chunk.SIZE_Y);
        generateInitialChunks(0, spawnCy, 0, 2, 2);
        player.position.set(0.5, spawnY, 0.5);
        player.velocity.set(0, 0, 0);
        saveWorld(player);
    }

    public Biome getBiome(long wx, long wz) {
        return TerrainGenerator.computeBiome(noise, wx, wz);
    }

    public long[] locateBiome(Biome target, long startX, long startZ) {
        if (getBiome(startX, startZ) == target) {
            return new long[]{startX, startZ};
        }
        int step = 48;
        for (int radius = step; radius <= 16000; radius += step) {
            for (int dx = -radius; dx <= radius; dx += step) {
                if (getBiome(startX + dx, startZ - radius) == target) return new long[]{startX + dx, startZ - radius};
                if (getBiome(startX + dx, startZ + radius) == target) return new long[]{startX + dx, startZ + radius};
            }
            for (int dz = -radius + step; dz <= radius - step; dz += step) {
                if (getBiome(startX - radius, startZ + dz) == target) return new long[]{startX - radius, startZ + dz};
                if (getBiome(startX + radius, startZ + dz) == target) return new long[]{startX + radius, startZ + dz};
            }
        }
        return null;
    }

    public int getSurfaceHeight(long wx, long wz) {
        long cx = Math.floorDiv(wx, (long) Chunk.SIZE_X);
        long cz = Math.floorDiv(wz, (long) Chunk.SIZE_Z);
        int lx = (int) Math.floorMod(wx, (long) Chunk.SIZE_X);
        int lz = (int) Math.floorMod(wz, (long) Chunk.SIZE_Z);

        ColumnPos colKey = new ColumnPos(cx, cz);
        int[] heights = columnHeightCache.computeIfAbsent(colKey, k -> {
            int[] h = new int[Chunk.SIZE_X * Chunk.SIZE_Z];
            long baseX = k.x() * Chunk.SIZE_X;
            long baseZ = k.z() * Chunk.SIZE_Z;
            for (int x = 0; x < Chunk.SIZE_X; x++) {
                for (int z = 0; z < Chunk.SIZE_Z; z++) {
                    h[z * Chunk.SIZE_X + x] = TerrainGenerator.computeSurfaceHeight(noise, baseX + x, baseZ + z);
                }
            }
            return h;
        });
        return heights[lz * Chunk.SIZE_X + lx];
    }

    public int getTopOpaqueY(long wx, long wz) {
        long cx = Math.floorDiv(wx, (long) Chunk.SIZE_X);
        long cz = Math.floorDiv(wz, (long) Chunk.SIZE_Z);
        int lx = (int) Math.floorMod(wx, (long) Chunk.SIZE_X);
        int lz = (int) Math.floorMod(wz, (long) Chunk.SIZE_Z);

        ColumnPos colKey = new ColumnPos(cx, cz);
        short[] topArray = columnTopOpaqueCache.computeIfAbsent(colKey, k -> computeColumnTopOpaque(k.x(), k.z()));
        return topArray[lz * Chunk.SIZE_X + lx];
    }

    private void setColumnTopOpaque(long wx, long wz, short topY) {
        long cx = Math.floorDiv(wx, (long) Chunk.SIZE_X);
        long cz = Math.floorDiv(wz, (long) Chunk.SIZE_Z);
        int lx = (int) Math.floorMod(wx, (long) Chunk.SIZE_X);
        int lz = (int) Math.floorMod(wz, (long) Chunk.SIZE_Z);

        ColumnPos colKey = new ColumnPos(cx, cz);
        short[] topArray = columnTopOpaqueCache.computeIfAbsent(colKey, k -> computeColumnTopOpaque(k.x(), k.z()));
        topArray[lz * Chunk.SIZE_X + lx] = topY;
    }

    private short[] computeColumnTopOpaque(long cx, long cz) {
        short[] topArray = new short[Chunk.SIZE_X * Chunk.SIZE_Z];
        long baseX = cx * Chunk.SIZE_X;
        long baseZ = cz * Chunk.SIZE_Z;

        for (int z = 0; z < Chunk.SIZE_Z; z++) {
            for (int x = 0; x < Chunk.SIZE_X; x++) {
                long wx = baseX + x;
                long wz = baseZ + z;
                topArray[z * Chunk.SIZE_X + x] = (short) findTopOpaqueYAt(wx, wz);
            }
        }
        return topArray;
    }

    public int findTopOpaqueYAt(long wx, long wz) {
        int naturalSurf = getSurfaceHeight(wx, wz);
        int scanTop = Math.max(naturalSurf, Chunk.SEA_LEVEL);

        long cx = Math.floorDiv(wx, (long) Chunk.SIZE_X);
        long cz = Math.floorDiv(wz, (long) Chunk.SIZE_Z);

        boolean columnModified = false;
        if (!modifiedChunkPositions.isEmpty()) {
            for (ChunkPos cp : modifiedChunkPositions) {
                if (cp.x() == cx && cp.z() == cz) {
                    columnModified = true;
                    break;
                }
            }
        }

        // Fast-path: natural untouched terrain has solid ground or ocean water at scanTop
        if (!columnModified && !TerrainGenerator.isSurfaceCarvedByCave(noise, wx, naturalSurf, wz)) {
            return scanTop;
        }

        // Check if player placed blocks above natural surface in this column
        if (columnModified) {
            for (Map.Entry<BlockPos, Byte> entry : modifiedBlocks.entrySet()) {
                BlockPos pos = entry.getKey();
                if (pos.x() == wx && pos.z() == wz && pos.y() > scanTop) {
                    Byte id = entry.getValue();
                    if (id != null) {
                        BlockType bt = BlockType.fromId(id);
                        if (bt.solid && !bt.transparent) {
                            scanTop = (int) pos.y();
                        }
                    }
                }
            }
        }

        // Fast scan downwards from scanTop to find highest solid/opaque block
        int bottomLimit = Math.max(-64, naturalSurf - 128);
        for (int y = scanTop; y >= bottomLimit; y--) {
            byte bId = getBlock(wx, y, wz);
            if (bId != BlockType.AIR.id) {
                BlockType bt = BlockType.fromId(bId);
                if ((bt.solid && !bt.transparent) || bId == BlockType.WATER.id || bId == BlockType.ICE.id) {
                    return y;
                }
            }
        }
        return bottomLimit;
    }

    public Chunk getChunk(long cx, long cy, long cz) {
        return chunks.get(new ChunkPos(cx, cy, cz));
    }

    public boolean areDirectNeighborsSolidOpaque(long cx, long cy, long cz) {
        Chunk c1 = getChunk(cx + 1, cy, cz);
        if (c1 == null || !c1.isAllSolidOpaque()) return false;
        Chunk c2 = getChunk(cx - 1, cy, cz);
        if (c2 == null || !c2.isAllSolidOpaque()) return false;
        Chunk c3 = getChunk(cx, cy + 1, cz);
        if (c3 == null || !c3.isAllSolidOpaque()) return false;
        Chunk c4 = getChunk(cx, cy - 1, cz);
        if (c4 == null || !c4.isAllSolidOpaque()) return false;
        Chunk c5 = getChunk(cx, cy, cz + 1);
        if (c5 == null || !c5.isAllSolidOpaque()) return false;
        Chunk c6 = getChunk(cx, cy, cz - 1);
        return c6 != null && c6.isAllSolidOpaque();
    }

    public Chunk getOrCreateChunk(long cx, long cy, long cz) {
        ChunkPos key = new ChunkPos(cx, cy, cz);
        Chunk chunk = chunks.get(key);
        if (chunk == null) {
            chunk = new Chunk(cx, cy, cz);
            byte[] savedData = savedRegionChunks.get(key);
            if (savedData != null) {
                chunk.setRawBlocks(savedData);
            } else {
                chunk.generateTerrain(noise);
                applyModifiedBlocksToChunk(chunk);
            }
            chunks.put(key, chunk);
        }
        return chunk;
    }

    public byte getBlock(long wx, long wy, long wz) {
        long cx = Math.floorDiv(wx, (long) Chunk.SIZE_X);
        long cy = Math.floorDiv(wy, (long) Chunk.SIZE_Y);
        long cz = Math.floorDiv(wz, (long) Chunk.SIZE_Z);
        Chunk c = chunks.get(new ChunkPos(cx, cy, cz));
        if (c == null) {
            Byte mod = modifiedBlocks.get(new BlockPos(wx, wy, wz));
            if (mod != null) return mod;
            int surfaceY = getSurfaceHeight(wx, wz);
            if (wy > surfaceY) {
                if (wy <= Chunk.SEA_LEVEL) {
                    if (wy == Chunk.SEA_LEVEL) {
                        Biome b = getBiome(wx, wz);
                        return (b == Biome.SNOWY_TUNDRA || b == Biome.FROZEN_OCEAN)
                                ? BlockType.ICE.id
                                : BlockType.WATER.id;
                    }
                    return BlockType.WATER.id;
                }
                return BlockType.AIR.id;
            }
            return BlockType.STONE.id;
        }
        int lx = (int) Math.floorMod(wx, (long) Chunk.SIZE_X);
        int ly = (int) Math.floorMod(wy, (long) Chunk.SIZE_Y);
        int lz = (int) Math.floorMod(wz, (long) Chunk.SIZE_Z);
        return c.getBlock(lx, ly, lz);
    }

    public void setBlock(long wx, long wy, long wz, byte id, boolean recordModification) {
        long cx = Math.floorDiv(wx, (long) Chunk.SIZE_X);
        long cy = Math.floorDiv(wy, (long) Chunk.SIZE_Y);
        long cz = Math.floorDiv(wz, (long) Chunk.SIZE_Z);
        Chunk c = getOrCreateChunk(cx, cy, cz);
        int lx = (int) Math.floorMod(wx, (long) Chunk.SIZE_X);
        int ly = (int) Math.floorMod(wy, (long) Chunk.SIZE_Y);
        int lz = (int) Math.floorMod(wz, (long) Chunk.SIZE_Z);
        c.setBlock(lx, ly, lz, id);

        if (recordModification) {
            modifiedBlocks.put(new BlockPos(wx, wy, wz), id);
            modifiedChunkPositions.add(new ChunkPos(cx, cy, cz));
            if (com.voxel.config.GameSettings.blockPhysicsEnabled) {
                triggerPhysics(wx, wy, wz);
            }
            updateNeighbors(wx, wy, wz);
        }

        int oldTop = getTopOpaqueY(wx, wz);
        int newTop = oldTop;

        BlockType bt = BlockType.fromId(id);
        boolean isOpaque = (bt.solid && !bt.transparent) || id == BlockType.WATER.id || id == BlockType.ICE.id;

        if (isOpaque && wy >= oldTop) {
            newTop = (int) wy;
            setColumnTopOpaque(wx, wz, (short) newTop);
        } else if (!isOpaque && wy >= oldTop) {
            newTop = findTopOpaqueYAt(wx, wz);
            setColumnTopOpaque(wx, wz, (short) newTop);
        }

        if (newTop != oldTop) {
            long minCy = Math.floorDiv(Math.min(oldTop, newTop), (long) Chunk.SIZE_Y);
            long maxCy = Math.floorDiv(Math.max(oldTop, newTop), (long) Chunk.SIZE_Y);
            for (long vcy = minCy; vcy <= maxCy; vcy++) {
                markDirty(cx, vcy, cz);
                markDirty(cx - 1, vcy, cz);
                markDirty(cx + 1, vcy, cz);
                markDirty(cx, vcy, cz - 1);
                markDirty(cx, vcy, cz + 1);
            }
        }

        int pad = Chunk.LIGHT_PAD;
        int minDx = (lx < pad) ? -1 : 0;
        int maxDx = (lx >= Chunk.SIZE_X - pad) ? 1 : 0;
        int minDy = (ly < pad) ? -1 : 0;
        int maxDy = (ly >= Chunk.SIZE_Y - pad) ? 1 : 0;
        int minDz = (lz < pad) ? -1 : 0;
        int maxDz = (lz >= Chunk.SIZE_Z - pad) ? 1 : 0;

        for (int dx = minDx; dx <= maxDx; dx++) {
            for (int dy = minDy; dy <= maxDy; dy++) {
                for (int dz = minDz; dz <= maxDz; dz++) {
                    if (dx != 0 || dy != 0 || dz != 0) {
                        markDirty(cx + dx, cy + dy, cz + dz);
                    }
                }
            }
        }
    }

    private void markDirty(long cx, long cy, long cz) {
        Chunk c = chunks.get(new ChunkPos(cx, cy, cz));
        if (c != null) {
            c.dirty = true;
        }
    }

    public void triggerPhysics(long wx, long wy, long wz) {
        queuePhysicsCheck(wx, wy, wz);
        queuePhysicsCheck(wx, wy + 1, wz);
        queuePhysicsCheck(wx, wy - 1, wz);
        queuePhysicsCheck(wx + 1, wy, wz);
        queuePhysicsCheck(wx - 1, wy, wz);
        queuePhysicsCheck(wx, wy, wz + 1);
        queuePhysicsCheck(wx, wy, wz - 1);
    }

    public void updateNeighbors(long wx, long wy, long wz) {
        notifyNeighbor(wx, wy + 1, wz, wx, wy, wz);
        notifyNeighbor(wx, wy - 1, wz, wx, wy, wz);
        notifyNeighbor(wx + 1, wy, wz, wx, wy, wz);
        notifyNeighbor(wx - 1, wy, wz, wx, wy, wz);
        notifyNeighbor(wx, wy, wz + 1, wx, wy, wz);
        notifyNeighbor(wx, wy, wz - 1, wx, wy, wz);
    }

    public void notifyNeighbor(long targetX, long targetY, long targetZ, long fromX, long fromY, long fromZ) {
        byte blockId = getBlock(targetX, targetY, targetZ);
        if (blockId != 0) {
            com.voxel.block.Block block = com.voxel.block.Blocks.get(blockId);
            if (block != null && block != com.voxel.block.Blocks.AIR) {
                block.neighborChanged(this, targetX, targetY, targetZ, fromX, fromY, fromZ);
            }
        }
    }

    public void queuePhysicsCheck(long wx, long wy, long wz) {
        byte b = getBlock(wx, wy, wz);
        if (BlockType.isWater(b)) {
            triggerFluid(wx, wy, wz);
            return;
        }
        BlockPos p = new BlockPos(wx, wy, wz);
        if (inPhysicsQueue.add(p)) {
            physicsQueue.offer(p);
        }
    }

    public void triggerFluid(long wx, long wy, long wz) {
        BlockPos p = new BlockPos(wx, wy, wz);
        if (inFluidQueue.add(p)) {
            fluidQueueNext.offer(p);
        }
    }

    public void tickPhysics(int maxSteps) {
        int processed = 0;
        while (processed < maxSteps && !physicsQueue.isEmpty()) {
            BlockPos p = physicsQueue.poll();
            if (p == null) break;
            inPhysicsQueue.remove(p);
            processed++;

            long x = p.x();
            long y = p.y();
            long z = p.z();
            byte blockId = getBlock(x, y, z);
            if (blockId == BlockType.AIR.id) continue;

            // 1. Falling blocks (Sand, Red Sand, Gravel)
            if (blockId == BlockType.SAND.id || blockId == BlockType.RED_SAND.id || blockId == BlockType.GRAVEL.id) {
                byte below = getBlock(x, y - 1, z);
                if (below == BlockType.AIR.id) {
                    setBlock(x, y, z, BlockType.AIR.id, true);
                    setBlock(x, y - 1, z, blockId, true);
                    triggerPhysics(x, y - 1, z);
                    triggerPhysics(x, y + 1, z);
                } else if (BlockType.isWater(below)) {
                    setBlock(x, y, z, BlockType.WATER.id, true);
                    setBlock(x, y - 1, z, blockId, true);
                    triggerPhysics(x, y - 1, z);
                    triggerPhysics(x, y, z);
                }
            } else if (BlockType.isWater(blockId)) {
                triggerFluid(x, y, z);
            }
        }
    }

    public void tickFluid(int maxSteps) {
        while (!fluidQueueNext.isEmpty()) {
            fluidQueueCurrent.offer(fluidQueueNext.poll());
        }

        int processed = 0;
        while (processed < maxSteps && !fluidQueueCurrent.isEmpty()) {
            BlockPos p = fluidQueueCurrent.poll();
            if (p == null) break;
            inFluidQueue.remove(p);
            processed++;

            long x = p.x();
            long y = p.y();
            long z = p.z();
            byte blockId = getBlock(x, y, z);
            if (!BlockType.isWater(blockId)) continue;

            byte below = getBlock(x, y - 1, z);
            String mode = com.voxel.config.GameSettings.waterPhysicsMode;

            if ("terraria".equalsIgnoreCase(mode)) {
                // Terraria Mode: Finite volume liquid conservation
                if (below == BlockType.AIR.id || BlockType.isWashableDecoration(below)) {
                    setBlock(x, y, z, BlockType.AIR.id, true);
                    setBlock(x, y - 1, z, BlockType.WATER.id, true);
                    triggerFluid(x, y - 1, z);
                    triggerFluid(x, y + 1, z);
                } else if (!BlockType.isWater(below)) {
                    long[][] sides = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
                    int offset = (int) Math.abs((x * 31L ^ z * 17L ^ y) % 4);
                    for (int i = 0; i < 4; i++) {
                        int idx = (offset + i) % 4;
                        long sx = x + sides[idx][0];
                        long sz = z + sides[idx][1];
                        byte sideBlock = getBlock(sx, y, sz);
                        if (sideBlock == BlockType.AIR.id || BlockType.isWashableDecoration(sideBlock)) {
                            setBlock(x, y, z, BlockType.AIR.id, true);
                            setBlock(sx, y, sz, BlockType.WATER.id, true);
                            triggerFluid(sx, y, sz);
                            triggerFluid(x, y, z);
                            break;
                        }
                    }
                }
            } else if ("preclassic".equalsIgnoreCase(mode) || "classic".equalsIgnoreCase(mode)) {
                // Pre-classic Minecraft: infinite spreading of source blocks co-tick
                if (below == BlockType.AIR.id || BlockType.isWashableDecoration(below)) {
                    setBlock(x, y - 1, z, BlockType.WATER.id, true);
                    triggerFluid(x, y - 1, z);
                } else {
                    long[][] sides = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
                    for (long[] s : sides) {
                        long sx = x + s[0];
                        long sz = z + s[1];
                        byte sideBlock = getBlock(sx, y, sz);
                        if (sideBlock == BlockType.AIR.id || BlockType.isWashableDecoration(sideBlock)) {
                            setBlock(sx, y, sz, BlockType.WATER.id, true);
                            triggerFluid(sx, y, sz);
                        }
                    }
                }
            } else {
                // Modern Minecraft Mode: Flow levels 1..7 and source block (level 8) co-tick
                int curLevel = BlockType.getWaterLevel(blockId);

                // A. Flowing water decay check: if upstream feed is gone, recede back to air
                if (curLevel < 8) {
                    byte above = getBlock(x, y + 1, z);
                    boolean fedFromAbove = BlockType.isWater(above);
                    boolean fedFromSide = false;
                    long[][] sides = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
                    for (long[] s : sides) {
                        byte nId = getBlock(x + s[0], y, z + s[1]);
                        if (BlockType.isWater(nId) && BlockType.getWaterLevel(nId) > curLevel) {
                            fedFromSide = true;
                            break;
                        }
                    }

                    if (!fedFromAbove && !fedFromSide) {
                        setBlock(x, y, z, BlockType.AIR.id, true);
                        for (long[] s : sides) {
                            triggerFluid(x + s[0], y, z + s[1]);
                        }
                        triggerFluid(x, y - 1, z);
                        continue;
                    }
                }

                // B. Downward flow (falling water takes priority)
                if (below == BlockType.AIR.id || BlockType.isWashableDecoration(below)) {
                    setBlock(x, y - 1, z, BlockType.WATER_FLOW_7.id, true);
                    triggerFluid(x, y - 1, z);
                } else if (BlockType.isWater(below) && BlockType.getWaterLevel(below) < 7) {
                    setBlock(x, y - 1, z, BlockType.WATER_FLOW_7.id, true);
                    triggerFluid(x, y - 1, z);
                } else {
                    // C. Horizontal spreading when supported by solid ground or full water underneath
                    if (curLevel > 1) {
                        int nextLevel = (curLevel == 8) ? 7 : (curLevel - 1);
                        byte nextBlockId = BlockType.getFlowBlockId(nextLevel);
                        long[][] sides = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
                        for (long[] s : sides) {
                            long sx = x + s[0];
                            long sz = z + s[1];
                            byte nId = getBlock(sx, y, sz);
                            if (nId == BlockType.AIR.id || BlockType.isWashableDecoration(nId)) {
                                setBlock(sx, y, sz, nextBlockId, true);
                                triggerFluid(sx, y, sz);
                            } else if (BlockType.isWater(nId) && BlockType.getWaterLevel(nId) < nextLevel) {
                                setBlock(sx, y, sz, nextBlockId, true);
                                triggerFluid(sx, y, sz);
                            }
                        }
                    }
                }
            }
        }
    }

    public void update(Vector3d playerPos, float dt) {
        if (com.voxel.config.GameSettings.enableOverrides && com.voxel.config.GameSettings.overrideDayCycle) {
            dayCyclePaused = com.voxel.config.GameSettings.overrideDayCycleValue;
        }
        if (com.voxel.config.GameSettings.enableOverrides && com.voxel.config.GameSettings.overrideTime) {
            timeOfDay = com.voxel.config.GameSettings.overrideTimeValue;
        } else if (!dayCyclePaused) {
            timeOfDay = (timeOfDay + dt / 240.0f) % 1.0f;
        }

        if (dt > 0.0f && com.voxel.config.GameSettings.blockPhysicsEnabled) {
            if (!physicsQueue.isEmpty()) {
                tickPhysics(128);
            }
            fluidTickTimer += dt;
            if (fluidTickTimer >= FLUID_TICK_INTERVAL) {
                fluidTickTimer -= FLUID_TICK_INTERVAL;
                tickFluid(128);
            }
        }

        long pcx = Math.floorDiv((long) Math.floor(playerPos.x), (long) Chunk.SIZE_X);
        long pcy = Math.floorDiv((long) Math.floor(playerPos.y), (long) Chunk.SIZE_Y);
        long pcz = Math.floorDiv((long) Math.floor(playerPos.z), (long) Chunk.SIZE_Z);

        // 1. Unload distant 3D cubic chunks
        long unloadXZ = renderDistance + 3;
        long unloadY = renderDistanceY + 3;
        Iterator<Map.Entry<ChunkPos, Chunk>> it = chunks.entrySet().iterator();
        while (it.hasNext()) {
            Chunk c = it.next().getValue();
            long dx = c.chunkX - pcx;
            long dy = c.chunkY - pcy;
            long dz = c.chunkZ - pcz;
            if (Math.abs(dx) > unloadXZ || Math.abs(dy) > unloadY || Math.abs(dz) > unloadXZ) {
                c.cleanup();
                it.remove();
                meshingChunks.remove(new ChunkPos(c.chunkX, c.chunkY, c.chunkZ));
            }
        }

        // Cache trimming
        if (columnHeightCache.size() > 4096) columnHeightCache.clear();
        if (columnTopOpaqueCache.size() > 4096) columnTopOpaqueCache.clear();

        // 2. Upload completed meshes from worker pool to GPU on main render thread
        int uploaded = 0;
        long uploadStart = System.nanoTime();
        long uploadBudget = 2_500_000L; // 2.5 ms budget on render thread
        com.voxel.world.mesh.ChunkMesher.BuiltChunkMesh built;
        while ((built = readyMeshes.poll()) != null) {
            Chunk c = built.chunk();
            ChunkPos pos = new ChunkPos(c.chunkX, c.chunkY, c.chunkZ);
            if (chunks.containsKey(pos)) {
                long t0 = System.nanoTime();
                c.uploadMeshes(built.opaqueData(), built.transData());
                c.dirty = false;
                updateAvgUploadTime(System.nanoTime() - t0);
            }
            meshingChunks.remove(pos);
            uploaded++;
            if (uploaded >= 64 || System.nanoTime() - uploadStart > uploadBudget) {
                break;
            }
        }

        // 3. Stream chunk generation in priority order to worker pool
        ChunkOffset[] offsets = this.sortedChunkOffsets;
        if (offsets == null || offsets.length == 0) {
            rebuildSortedChunkOffsets();
            offsets = this.sortedChunkOffsets;
        }

        int maxQueuedGen = numThreads * 16;
        int submittedGen = 0;
        for (ChunkOffset offset : offsets) {
            if (generatingChunks.size() >= maxQueuedGen) {
                break;
            }
            long cx = pcx + offset.dx();
            long cy = pcy + offset.dy();
            long cz = pcz + offset.dz();
            ChunkPos pos = new ChunkPos(cx, cy, cz);
            if (!chunks.containsKey(pos) && generatingChunks.add(pos)) {
                workerPool.submit(() -> {
                    try {
                        long t0 = System.nanoTime();
                        Chunk chunk = new Chunk(cx, cy, cz);
                        byte[] saved = savedRegionChunks.get(pos);
                        if (saved != null) {
                            chunk.setRawBlocks(saved);
                        } else {
                            chunk.generateTerrain(noise);
                            applyModifiedBlocksToChunk(chunk);
                        }
                        chunks.put(pos, chunk);
                        updateAvgGenTime(System.nanoTime() - t0);

                        // Queue meshing immediately
                        queueMeshBuild(chunk);

                        // Mark neighbors dirty if not all air
                        if (!chunk.isAllAir()) {
                            markDirty(cx - 1, cy, cz);
                            markDirty(cx + 1, cy, cz);
                            markDirty(cx, cy - 1, cz);
                            markDirty(cx, cy + 1, cz);
                            markDirty(cx, cy, cz - 1);
                            markDirty(cx, cy, cz + 1);
                        }
                    } finally {
                        generatingChunks.remove(pos);
                    }
                });
                submittedGen++;
                if (submittedGen >= 48) {
                    break;
                }
            }
        }

        // 4. Dispatch dirty chunks meshing to worker pool in priority order
        long maxXZ = renderDistance + 1;
        long maxY = renderDistanceY + 1;
        List<Chunk> dirtyList = new ArrayList<>();
        for (Chunk c : chunks.values()) {
            if (!c.dirty) continue;
            ChunkPos pos = new ChunkPos(c.chunkX, c.chunkY, c.chunkZ);
            if (meshingChunks.contains(pos)) continue;

            long dx = c.chunkX - pcx;
            long dy = c.chunkY - pcy;
            long dz = c.chunkZ - pcz;
            if (Math.abs(dx) <= maxXZ && Math.abs(dy) <= maxY && Math.abs(dz) <= maxXZ) {
                dirtyList.add(c);
            }
        }

        if (!dirtyList.isEmpty()) {
            dirtyList.sort(Comparator.comparingInt(c -> {
                int dx = (int) (c.chunkX - pcx);
                int dy = (int) (c.chunkY - pcy);
                int dz = (int) (c.chunkZ - pcz);
                return calculateChunkPriorityScore(dx, dy, dz);
            }));

            int submittedMesh = 0;
            for (Chunk c : dirtyList) {
                if (queueMeshBuild(c)) {
                    submittedMesh++;
                    if (submittedMesh >= 48) {
                        break;
                    }
                }
            }
        }
    }

    public boolean queueMeshBuild(Chunk chunk) {
        if (chunk == null) return false;
        ChunkPos pos = new ChunkPos(chunk.chunkX, chunk.chunkY, chunk.chunkZ);
        if (meshingChunks.add(pos)) {
            workerPool.submit(() -> {
                try {
                    long t0 = System.nanoTime();
                    com.voxel.world.mesh.ChunkMesher.BuiltChunkMesh b = com.voxel.world.mesh.ChunkMesher.buildChunkMesh(chunk, this);
                    updateAvgMeshTime(System.nanoTime() - t0);
                    readyMeshes.add(b);
                } catch (Throwable t) {
                    meshingChunks.remove(pos);
                }
            });
            return true;
        }
        return false;
    }

    public void updateAvgGenTime(long nanoDuration) {
        totalChunksGenerated.incrementAndGet();
        double ms = nanoDuration / 1_000_000.0;
        avgGenTimeMs = avgGenTimeMs * 0.96 + ms * 0.04;
    }

    public void updateAvgMeshTime(long nanoDuration) {
        totalMeshesBuilt.incrementAndGet();
        double ms = nanoDuration / 1_000_000.0;
        avgMeshTimeMs = avgMeshTimeMs * 0.96 + ms * 0.04;
    }

    public void updateAvgUploadTime(long nanoDuration) {
        totalMeshesUploaded.incrementAndGet();
        double ms = nanoDuration / 1_000_000.0;
        avgUploadTimeMs = avgUploadTimeMs * 0.96 + ms * 0.04;
    }

    public int getGeneratingChunksCount() { return generatingChunks.size(); }
    public int getMeshingChunksCount() { return meshingChunks.size(); }
    public int getReadyMeshesCount() { return readyMeshes.size(); }
    public int getWorkerThreadCount() { return numThreads; }
    public double getAvgGenTimeMs() { return avgGenTimeMs; }
    public double getAvgMeshTimeMs() { return avgMeshTimeMs; }
    public double getAvgUploadTimeMs() { return avgUploadTimeMs; }
    public long getTotalChunksGenerated() { return totalChunksGenerated.get(); }
    public long getTotalMeshesBuilt() { return totalMeshesBuilt.get(); }
    public long getTotalMeshesUploaded() { return totalMeshesUploaded.get(); }

    public BenchmarkResult runBenchmark(int chunkCount) {
        int threads = this.numThreads;
        SimplexNoise benchNoise = new SimplexNoise(seed ^ 0x55AA55AAL);
        Chunk[] benchChunks = new Chunk[chunkCount];
        for (int i = 0; i < chunkCount; i++) {
            long cx = 10000 + (i % 32);
            long cy = (i / 32) % 8;
            long cz = 10000 + (i / 256);
            benchChunks[i] = new Chunk(cx, cy, cz);
        }

        // 1. Terrain Gen
        long startGen = System.nanoTime();
        List<Future<?>> genFutures = new ArrayList<>(chunkCount);
        for (Chunk c : benchChunks) {
            genFutures.add(workerPool.submit(() -> c.generateTerrain(benchNoise)));
        }
        for (Future<?> f : genFutures) {
            try { f.get(); } catch (Exception ignored) {}
        }
        long endGen = System.nanoTime();
        double genMs = (endGen - startGen) / 1_000_000.0;
        double genRate = (genMs > 0) ? (chunkCount / (genMs / 1000.0)) : 0;
        double avgGen = (chunkCount > 0) ? (genMs / chunkCount) : 0;

        // 2. Meshing
        List<Chunk> toMesh = new ArrayList<>();
        for (Chunk c : benchChunks) {
            if (!c.isAllAir()) toMesh.add(c);
        }
        long startMesh = System.nanoTime();
        List<Future<com.voxel.world.mesh.ChunkMesher.BuiltChunkMesh>> meshFutures = new ArrayList<>(toMesh.size());
        for (Chunk c : toMesh) {
            meshFutures.add(workerPool.submit(() -> com.voxel.world.mesh.ChunkMesher.buildChunkMesh(c, this)));
        }
        for (Future<?> f : meshFutures) {
            try { f.get(); } catch (Exception ignored) {}
        }
        long endMesh = System.nanoTime();
        double meshMs = (endMesh - startMesh) / 1_000_000.0;
        double meshRate = (meshMs > 0) ? (toMesh.size() / (meshMs / 1000.0)) : 0;
        double avgMesh = toMesh.isEmpty() ? 0 : (meshMs / toMesh.size());

        double totalMs = genMs + meshMs;
        double totalRate = (totalMs > 0) ? (chunkCount / (totalMs / 1000.0)) : 0;

        String rating;
        if (totalRate >= 2000) rating = "A+ (WYBITNA / ULTRA)";
        else if (totalRate >= 1200) rating = "A (BARDZO DOBRA / FAST)";
        else if (totalRate >= 600) rating = "B (DOBRA / MEDIUM)";
        else rating = "C (WYSTARCZAJACA / BASIC)";

        return new BenchmarkResult(chunkCount, threads, genMs, genRate, avgGen,
                toMesh.size(), meshMs, meshRate, avgMesh, totalMs, totalRate, rating);
    }

    public void runBenchmarkAsync(int chunkCount, java.util.function.Consumer<BenchmarkResult> callback) {
        Thread t = new Thread(() -> {
            BenchmarkResult res = runBenchmark(chunkCount);
            if (callback != null) {
                callback.accept(res);
            }
        }, "Voxel-Benchmark-Runner");
        t.setDaemon(true);
        t.start();
    }

    public void generateInitialChunks(long centerCx, long centerCy, long centerCz, int radiusXZ, int radiusY) {
        List<ChunkOffset> initOffsets = new ArrayList<>();
        for (int dy = -radiusY; dy <= radiusY; dy++) {
            for (int dx = -radiusXZ; dx <= radiusXZ; dx++) {
                for (int dz = -radiusXZ; dz <= radiusXZ; dz++) {
                    initOffsets.add(new ChunkOffset(dx, dy, dz, calculateChunkPriorityScore(dx, dy, dz)));
                }
            }
        }
        Collections.sort(initOffsets);

        List<Future<?>> genFutures = new ArrayList<>();
        for (ChunkOffset o : initOffsets) {
            long cx = centerCx + o.dx();
            long cy = centerCy + o.dy();
            long cz = centerCz + o.dz();
            ChunkPos pos = new ChunkPos(cx, cy, cz);
            if (!chunks.containsKey(pos)) {
                genFutures.add(workerPool.submit(() -> {
                    getOrCreateChunk(cx, cy, cz);
                }));
            }
        }
        for (Future<?> f : genFutures) {
            try { f.get(); } catch (Exception ignored) {}
        }

        List<Future<com.voxel.world.mesh.ChunkMesher.BuiltChunkMesh>> meshFutures = new ArrayList<>();
        for (ChunkOffset o : initOffsets) {
            Chunk c = getChunk(centerCx + o.dx(), centerCy + o.dy(), centerCz + o.dz());
            if (c != null) {
                meshFutures.add(workerPool.submit(() -> com.voxel.world.mesh.ChunkMesher.buildChunkMesh(c, this)));
            }
        }
        for (Future<com.voxel.world.mesh.ChunkMesher.BuiltChunkMesh> f : meshFutures) {
            try {
                com.voxel.world.mesh.ChunkMesher.BuiltChunkMesh builtMesh = f.get();
                builtMesh.chunk().uploadMeshes(builtMesh.opaqueData(), builtMesh.transData());
                builtMesh.chunk().dirty = false;
            } catch (Exception ignored) {}
        }
    }

    private void applyModifiedBlocksToChunk(Chunk chunk) {
        if (modifiedBlocks.isEmpty()) return;
        ChunkPos cpos = new ChunkPos(chunk.chunkX, chunk.chunkY, chunk.chunkZ);
        if (!modifiedChunkPositions.contains(cpos)) return;

        long minX = chunk.chunkX * Chunk.SIZE_X;
        long maxX = minX + Chunk.SIZE_X - 1;
        long minY = chunk.chunkY * Chunk.SIZE_Y;
        long maxY = minY + Chunk.SIZE_Y - 1;
        long minZ = chunk.chunkZ * Chunk.SIZE_Z;
        long maxZ = minZ + Chunk.SIZE_Z - 1;

        for (Map.Entry<BlockPos, Byte> entry : modifiedBlocks.entrySet()) {
            BlockPos pos = entry.getKey();
            if (pos.x() >= minX && pos.x() <= maxX
                    && pos.y() >= minY && pos.y() <= maxY
                    && pos.z() >= minZ && pos.z() <= maxZ) {
                chunk.setBlock((int) (pos.x() - minX), (int) (pos.y() - minY), (int) (pos.z() - minZ), entry.getValue());
            }
        }
    }

    public long findSpawnY(long wx, long wz) {
        int surfaceY = getSurfaceHeight(wx, wz);
        return Math.max(surfaceY, Chunk.SEA_LEVEL) + 2L;
    }

    public RaycastHit raycast(Vector3d origin, Vector3f dir, float maxDist) {
        long x = (long) Math.floor(origin.x);
        long y = (long) Math.floor(origin.y);
        long z = (long) Math.floor(origin.z);

        int stepX = dir.x > 0 ? 1 : (dir.x < 0 ? -1 : 0);
        int stepY = dir.y > 0 ? 1 : (dir.y < 0 ? -1 : 0);
        int stepZ = dir.z > 0 ? 1 : (dir.z < 0 ? -1 : 0);

        double tDeltaX = stepX != 0 ? Math.abs(1.0 / dir.x) : Double.MAX_VALUE;
        double tDeltaY = stepY != 0 ? Math.abs(1.0 / dir.y) : Double.MAX_VALUE;
        double tDeltaZ = stepZ != 0 ? Math.abs(1.0 / dir.z) : Double.MAX_VALUE;

        double fracX = origin.x - x;
        double fracY = origin.y - y;
        double fracZ = origin.z - z;

        double tMaxX = stepX > 0 ? (1.0 - fracX) * tDeltaX : (stepX < 0 ? fracX * tDeltaX : Double.MAX_VALUE);
        double tMaxY = stepY > 0 ? (1.0 - fracY) * tDeltaY : (stepY < 0 ? fracY * tDeltaY : Double.MAX_VALUE);
        double tMaxZ = stepZ > 0 ? (1.0 - fracZ) * tDeltaZ : (stepZ < 0 ? fracZ * tDeltaZ : Double.MAX_VALUE);

        long prevX = x, prevY = y, prevZ = z;
        double dist = 0.0;

        while (dist <= maxDist) {
            byte b = getBlock(x, y, z);
            if (b != BlockType.AIR.id && !BlockType.isWater(b)) {
                return new RaycastHit(x, y, z, prevX, prevY, prevZ, b);
            }

            prevX = x;
            prevY = y;
            prevZ = z;

            if (tMaxX < tMaxY) {
                if (tMaxX < tMaxZ) {
                    x += stepX;
                    dist = tMaxX;
                    tMaxX += tDeltaX;
                } else {
                    z += stepZ;
                    dist = tMaxZ;
                    tMaxZ += tDeltaZ;
                }
            } else {
                if (tMaxY < tMaxZ) {
                    y += stepY;
                    dist = tMaxY;
                    tMaxY += tDeltaY;
                } else {
                    z += stepZ;
                    dist = tMaxZ;
                    tMaxZ += tDeltaZ;
                }
            }
        }
        return null;
    }

    public float getSunIntensity() {
        float angle = (float) (timeOfDay * Math.PI * 2.0);
        float raw = (float) (-Math.cos(angle));
        float normalized = (raw + 1.0f) * 0.5f;
        return 0.18f + 0.82f * smoothstep(0.15f, 0.85f, normalized);
    }

    public Vector3f getSkyColor() {
        float angle = (float) (timeOfDay * Math.PI * 2.0);
        float sunHeight = (float) (-Math.cos(angle));

        Vector3f nightSky = new Vector3f(0.04f, 0.06f, 0.14f);
        Vector3f sunsetSky = new Vector3f(0.88f, 0.48f, 0.30f);
        Vector3f daySky = new Vector3f(0.46f, 0.72f, 0.98f);

        if (sunHeight > 0.25f) {
            float t = Math.min(1.0f, (sunHeight - 0.25f) / 0.55f);
            return new Vector3f(sunsetSky).lerp(daySky, t);
        } else if (sunHeight > -0.25f) {
            float t = (sunHeight + 0.25f) / 0.50f;
            float horizonGlow = 1.0f - Math.abs(t * 2.0f - 1.0f);
            Vector3f base = new Vector3f(nightSky).lerp(daySky, t);
            return base.lerp(sunsetSky, horizonGlow * 0.65f);
        } else {
            return nightSky;
        }
    }

    private static float smoothstep(float edge0, float edge1, float x) {
        float t = Math.max(0.0f, Math.min(1.0f, (x - edge0) / (edge1 - edge0)));
        return t * t * (3.0f - 2.0f * t);
    }

    public void renderChunks(Vector3d playerPos, Vector3d eyePos, ShaderProgram chunkShader) {
        renderChunks(playerPos, eyePos, null, chunkShader);
    }

    public void renderChunks(Vector3d playerPos, Vector3d eyePos, Matrix4f projView, ShaderProgram chunkShader) {
        long pcx = Math.floorDiv((long) Math.floor(playerPos.x), (long) Chunk.SIZE_X);
        long pcy = Math.floorDiv((long) Math.floor(playerPos.y), (long) Chunk.SIZE_Y);
        long pcz = Math.floorDiv((long) Math.floor(playerPos.z), (long) Chunk.SIZE_Z);
        long maxXZ = renderDistance + 1;
        long maxY = renderDistanceY + 1;

        boolean useFrustum = (projView != null);
        if (useFrustum) {
            frustum.set(projView);
        }

        visibleOpaque.clear();
        visibleTransparent.clear();

        for (Chunk c : chunks.values()) {
            if (c.isAllAir() || !c.hasAnyMesh()) continue;

            long dx = c.chunkX - pcx;
            long dy = c.chunkY - pcy;
            long dz = c.chunkZ - pcz;
            if (Math.abs(dx) > maxXZ || Math.abs(dy) > maxY || Math.abs(dz) > maxXZ
                    || dx * dx + dz * dz > maxXZ * maxXZ) {
                continue;
            }

            // High-performance SIMD Frustum Culling
            if (useFrustum) {
                float minX = (float) ((double) c.chunkX * Chunk.SIZE_X - eyePos.x);
                float minY = (float) ((double) c.chunkY * Chunk.SIZE_Y - eyePos.y);
                float minZ = (float) ((double) c.chunkZ * Chunk.SIZE_Z - eyePos.z);
                float maxX = minX + (float) Chunk.SIZE_X;
                float maxYBox = minY + (float) Chunk.SIZE_Y;
                float maxZ = minZ + (float) Chunk.SIZE_Z;

                if (!frustum.testAab(minX, minY, minZ, maxX, maxYBox, maxZ)) {
                    continue;
                }
            }

            // Occlusion Culling: cull chunks buried completely underground or behind ridges
            if (isChunkOccluded(c, pcx, pcy, pcz, eyePos)) {
                continue;
            }

            if (c.hasOpaque()) {
                visibleOpaque.add(c);
            }
            if (c.hasTransparent()) {
                visibleTransparent.add(c);
            }
        }

        lastRenderedChunkCount = visibleOpaque.size() + visibleTransparent.size();

        // 1. Render visible opaque chunks (Early-Z hardware optimization)
        for (Chunk c : visibleOpaque) {
            setChunkOffsetUniform(c, eyePos, chunkShader);
            c.renderOpaque();
        }

        // 2. Sort and render visible transparent chunks (only the few chunks with water/ice in view)
        if (!visibleTransparent.isEmpty()) {
            visibleTransparent.sort((a, b) -> {
                double dax = (a.chunkX * Chunk.SIZE_X + 8.0) - eyePos.x;
                double day = (a.chunkY * Chunk.SIZE_Y + 8.0) - eyePos.y;
                double daz = (a.chunkZ * Chunk.SIZE_Z + 8.0) - eyePos.z;
                double dbx = (b.chunkX * Chunk.SIZE_X + 8.0) - eyePos.x;
                double dby = (b.chunkY * Chunk.SIZE_Y + 8.0) - eyePos.y;
                double dbz = (b.chunkZ * Chunk.SIZE_Z + 8.0) - eyePos.z;
                return Double.compare(dbx * dbx + dby * dby + dbz * dbz, dax * dax + day * day + daz * daz);
            });

            for (Chunk c : visibleTransparent) {
                setChunkOffsetUniform(c, eyePos, chunkShader);
                c.renderTransparent();
            }
        }
    }

    private void setChunkOffsetUniform(Chunk c, Vector3d eyePos, ShaderProgram chunkShader) {
        double baseX = (double) c.chunkX * Chunk.SIZE_X;
        double baseY = (double) c.chunkY * Chunk.SIZE_Y;
        double baseZ = (double) c.chunkZ * Chunk.SIZE_Z;
        float offsetX = (float) (baseX - eyePos.x);
        float offsetY = (float) (baseY - eyePos.y);
        float offsetZ = (float) (baseZ - eyePos.z);
        chunkShader.setUniform("uChunkOffset", new Vector3f(offsetX, offsetY, offsetZ));
    }

    public boolean isChunkOccluded(Chunk c, long pcx, long pcy, long pcz, Vector3d eyePos) {
        long dx = c.chunkX - pcx;
        long dy = c.chunkY - pcy;
        long dz = c.chunkZ - pcz;
        long distSq = dx * dx + dz * dz;

        // Immediate player neighborhood is never occluded
        if (distSq <= 2 && Math.abs(dy) <= 1) return false;

        double chunkTopY = (c.chunkY + 1) * Chunk.SIZE_Y;

        // 1. Subterranean occlusion: chunk is completely buried beneath solid ground
        if (eyePos.y > chunkTopY + 8.0) {
            long centerWx = c.chunkX * Chunk.SIZE_X + 8;
            long centerWz = c.chunkZ * Chunk.SIZE_Z + 8;
            int surfaceY = findTopOpaqueYAt(centerWx, centerWz);
            if (surfaceY >= chunkTopY + 4 && eyePos.y > surfaceY) {
                long playerWx = (long) Math.floor(eyePos.x);
                long playerWz = (long) Math.floor(eyePos.z);
                int playerGroundY = findTopOpaqueYAt(playerWx, playerWz);
                if (eyePos.y >= playerGroundY) {
                    return true;
                }
            }
        }

        // 2. Mountain ridge occlusion for distant chunks (at least 3 chunks away)
        if (distSq >= 9) {
            long midCx = pcx + dx / 2;
            long midCz = pcz + dz / 2;
            long midWx = midCx * Chunk.SIZE_X + 8;
            long midWz = midCz * Chunk.SIZE_Z + 8;
            int ridgeY = findTopOpaqueYAt(midWx, midWz);
            if (ridgeY > chunkTopY + 8 && ridgeY > eyePos.y + 6) {
                return true;
            }
        }

        return false;
    }

    public boolean saveWorld(Player player) {
        return WorldSaveManager.saveWorld(this, player);
    }

    public boolean saveWorld(Player player, String saveName) {
        return WorldSaveManager.saveWorld(this, player, saveName);
    }

    public boolean loadWorld(Player player) {
        columnTopOpaqueCache.clear();
        return WorldSaveManager.loadWorld(this, player);
    }

    public boolean loadWorld(Player player, String saveName) {
        columnTopOpaqueCache.clear();
        return WorldSaveManager.loadWorld(this, player, saveName);
    }

    public boolean hasSaveFile() {
        return WorldSaveManager.hasSaveFile();
    }

    public boolean hasSaveFile(String saveName) {
        return WorldSaveManager.hasSaveFile(saveName);
    }

    public int getLoadedChunkCount() {
        return chunks.size();
    }

    public int getLastRenderedChunkCount() {
        return lastRenderedChunkCount;
    }

    public void cleanup() {
        if (workerPool != null && !workerPool.isShutdown()) {
            workerPool.shutdownNow();
        }
        clearLoadedChunks();
    }
}
