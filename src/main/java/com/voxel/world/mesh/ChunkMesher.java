package com.voxel.world.mesh;

import com.voxel.block.BlockType;
import com.voxel.render.TextureAtlas;
import com.voxel.world.Biome;
import com.voxel.world.Chunk;
import com.voxel.world.World;

import java.util.Arrays;

import static com.voxel.world.Chunk.*;

public final class ChunkMesher {
    public static final int GRID_SIZE = SIZE_X + LIGHT_PAD * 2;
    public static final int GRID_VOL = GRID_SIZE * GRID_SIZE * GRID_SIZE;

    public static final class MesherContext {
        public final byte[] scratchBlocks = new byte[GRID_VOL];
        public final boolean[] scratchOccluder = new boolean[GRID_VOL];
        public final byte[] scratchSkyLight = new byte[GRID_VOL];
        public final byte[] scratchBlockLight = new byte[GRID_VOL];
        public final int[] scratchQueue = new int[GRID_VOL];
        public final short[][] scratchTopOpaque = new short[GRID_SIZE][GRID_SIZE];
        public final FloatList opaqueData = new FloatList(4096);
        public final FloatList transData = new FloatList(1024);
    }

    private static final ThreadLocal<MesherContext> THREAD_CONTEXT = ThreadLocal.withInitial(MesherContext::new);

    public record BuiltChunkMesh(Chunk chunk, FloatList opaqueData, FloatList transData) {}

    private static final float[] FACE_SHADE = {
        1.00f, // +Y (top)
        0.52f, // -Y (bottom)
        0.84f, // +Z (south)
        0.84f, // -Z (north)
        0.70f, // +X (east)
        0.70f  // -X (west)
    };

    private ChunkMesher() {}

    private static int gridIndex(int gx, int gy, int gz) {
        return (gy * GRID_SIZE + gz) * GRID_SIZE + gx;
    }

    public static void rebuildChunkMesh(Chunk chunk, World world) {
        chunk.dirty = false;
        BuiltChunkMesh built = buildChunkMesh(chunk, world);
        chunk.uploadMeshes(built.opaqueData(), built.transData());
    }

    public static BuiltChunkMesh buildChunkMesh(Chunk chunk, World world) {
        if (chunk.isAllAir() || (chunk.isAllSolidOpaque() && world.areDirectNeighborsSolidOpaque(chunk.chunkX, chunk.chunkY, chunk.chunkZ))) {
            return new BuiltChunkMesh(chunk, new FloatList(0), new FloatList(0));
        }

        MesherContext ctx = THREAD_CONTEXT.get();
        ctx.opaqueData.clear();
        ctx.transData.clear();

        long baseX = chunk.chunkX * SIZE_X;
        long baseY = chunk.chunkY * SIZE_Y;
        long baseZ = chunk.chunkZ * SIZE_Z;

        Chunk[][][] neighborChunks = new Chunk[3][3][3];
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    neighborChunks[dx + 1][dy + 1][dz + 1] = world.getChunk(chunk.chunkX + dx, chunk.chunkY + dy, chunk.chunkZ + dz);
                }
            }
        }

        Arrays.fill(ctx.scratchSkyLight, (byte) 0);
        Arrays.fill(ctx.scratchBlockLight, (byte) 0);

        long originX = baseX - LIGHT_PAD;
        long originY = baseY - LIGHT_PAD;
        long originZ = baseZ - LIGHT_PAD;

        int minTopInGrid = Integer.MAX_VALUE;
        for (int gx = 0; gx < GRID_SIZE; gx++) {
            long wx = originX + gx;
            for (int gz = 0; gz < GRID_SIZE; gz++) {
                long wz = originZ + gz;
                int topY = world.getTopOpaqueY(wx, wz);
                ctx.scratchTopOpaque[gx][gz] = (short) topY;
                if (topY < minTopInGrid) minTopInGrid = topY;
            }
        }

        int skyTail = 0;
        int blockTail = 0;

        for (int gy = 0; gy < GRID_SIZE; gy++) {
            long wy = originY + gy;
            int ly = gy - LIGHT_PAD;
            int ncy = (ly < 0) ? 0 : (ly >= SIZE_Y ? 2 : 1);
            int cly = (ly < 0) ? (ly + SIZE_Y) : (ly >= SIZE_Y ? (ly - SIZE_Y) : ly);

            for (int gz = 0; gz < GRID_SIZE; gz++) {
                int lz = gz - LIGHT_PAD;
                int ncz = (lz < 0) ? 0 : (lz >= SIZE_Z ? 2 : 1);
                int clz = (lz < 0) ? (lz + SIZE_Z) : (lz >= SIZE_Z ? (lz - SIZE_Z) : lz);

                for (int gx = 0; gx < GRID_SIZE; gx++) {
                    int lx = gx - LIGHT_PAD;
                    int ncx = (lx < 0) ? 0 : (lx >= SIZE_X ? 2 : 1);
                    int clx = (lx < 0) ? (lx + SIZE_X) : (lx >= SIZE_X ? (lx - SIZE_X) : lx);

                    Chunk nc = neighborChunks[ncx][ncy][ncz];
                    byte id;
                    if (nc != null) {
                        id = nc.getBlock(clx, cly, clz);
                    } else {
                        int topY = ctx.scratchTopOpaque[gx][gz];
                        if (wy > topY) {
                            if (wy <= SEA_LEVEL) {
                                Biome b = world.getBiome(originX + gx, originZ + gz);
                                id = (b == Biome.SNOWY_TUNDRA || b == Biome.FROZEN_OCEAN)
                                        ? BlockType.ICE.id
                                        : BlockType.WATER.id;
                            } else {
                                id = BlockType.AIR.id;
                            }
                        } else {
                            id = BlockType.STONE.id;
                        }
                    }

                    int gIdx = gridIndex(gx, gy, gz);
                    ctx.scratchBlocks[gIdx] = id;
                    BlockType bt = BlockType.fromId(id);
                    boolean occ = bt.solid && !bt.transparent;
                    ctx.scratchOccluder[gIdx] = occ;

                    if (bt.emissive) {
                        ctx.scratchBlockLight[gIdx] = 15;
                        ctx.scratchQueue[blockTail++] = gIdx;
                    }
                }
            }
        }

        if (blockTail > 0) {
            propagateLight(ctx.scratchBlockLight, ctx.scratchOccluder, ctx.scratchQueue, 0, blockTail, false);
        }

        // Sunlight injection: activates for ANY chunk penetrated by open sky / vertical shafts
        if (originY + GRID_SIZE - 1 > minTopInGrid) {
            for (int gy = 0; gy < GRID_SIZE; gy++) {
                long wy = originY + gy;
                for (int gz = 0; gz < GRID_SIZE; gz++) {
                    for (int gx = 0; gx < GRID_SIZE; gx++) {
                        int gIdx = gridIndex(gx, gy, gz);
                        if (!ctx.scratchOccluder[gIdx] && wy > ctx.scratchTopOpaque[gx][gz]) {
                            ctx.scratchSkyLight[gIdx] = 15;
                            ctx.scratchQueue[skyTail++] = gIdx;
                        }
                    }
                }
            }
            if (skyTail > 0) {
                propagateLight(ctx.scratchSkyLight, ctx.scratchOccluder, ctx.scratchQueue, 0, skyTail, true);
            }
        }

        for (int y = 0; y < SIZE_Y; y++) {
            int gy = y + LIGHT_PAD;
            for (int z = 0; z < SIZE_Z; z++) {
                int gz = z + LIGHT_PAD;
                for (int x = 0; x < SIZE_X; x++) {
                    byte id = chunk.getBlock(x, y, z);
                    if (id == BlockType.AIR.id) continue;

                    int gx = x + LIGHT_PAD;
                    long wx = baseX + x;
                    long wy = baseY + y;
                    long wz = baseZ + z;
                    BlockType block = BlockType.fromId(id);
                    FloatList target = block.transparent ? ctx.transData : ctx.opaqueData;

                    if (block.crossModel) {
                        emitCrossQuads(target, block, x, y, z, gx, gy, gz, wx, wy, wz, world, ctx);
                        if (block == BlockType.SEAGRASS || block == BlockType.KELP) {
                            emitWaterloggedFaces(ctx.transData, x, y, z, gx, gy, gz, wx, wy, wz, world, ctx);
                        }
                        continue;
                    }

                    // 0: +Y, 1: -Y, 2: +Z, 3: -Z, 4: +X, 5: -X
                    if (shouldRenderFaceFast(block, gx, gy + 1, gz, ctx)) {
                        emitFaceFast(target, block, 0, x, y, z, gx, gy, gz, wx, wy, wz, world, ctx);
                    }
                    if (shouldRenderFaceFast(block, gx, gy - 1, gz, ctx)) {
                        emitFaceFast(target, block, 1, x, y, z, gx, gy, gz, wx, wy, wz, world, ctx);
                    }
                    if (shouldRenderFaceFast(block, gx, gy, gz + 1, ctx)) {
                        emitFaceFast(target, block, 2, x, y, z, gx, gy, gz, wx, wy, wz, world, ctx);
                    }
                    if (shouldRenderFaceFast(block, gx, gy, gz - 1, ctx)) {
                        emitFaceFast(target, block, 3, x, y, z, gx, gy, gz, wx, wy, wz, world, ctx);
                    }
                    if (shouldRenderFaceFast(block, gx + 1, gy, gz, ctx)) {
                        emitFaceFast(target, block, 4, x, y, z, gx, gy, gz, wx, wy, wz, world, ctx);
                    }
                    if (shouldRenderFaceFast(block, gx - 1, gy, gz, ctx)) {
                        emitFaceFast(target, block, 5, x, y, z, gx, gy, gz, wx, wy, wz, world, ctx);
                    }
                }
            }
        }

        return new BuiltChunkMesh(chunk, ctx.opaqueData.copy(), ctx.transData.copy());
    }

    private static boolean shouldRenderFaceFast(BlockType current, int ngx, int ngy, int ngz, MesherContext ctx) {
        byte nId = ctx.scratchBlocks[gridIndex(ngx, ngy, ngz)];
        if (nId == BlockType.AIR.id) return true;
        BlockType neighbor = BlockType.fromId(nId);
        if (neighbor.crossModel) {
            if (current.isWater() && (neighbor == BlockType.SEAGRASS || neighbor == BlockType.KELP)) {
                return false;
            }
            return true;
        }
        if (current.isWater() && (neighbor.isWater() || neighbor == BlockType.SEAGRASS || neighbor == BlockType.KELP)) {
            return false;
        }
        if (current == neighbor && current.transparent) {
            return false;
        }
        // Avoid duplicate coplanar transparent faces where WATER touches ICE
        if (current.isWater() && neighbor == BlockType.ICE) {
            return false;
        }
        return neighbor.transparent;
    }

    private static void emitWaterloggedFaces(FloatList transData,
                                             int x, int y, int z, int gx, int gy, int gz,
                                             long wx, long wy, long wz, World world, MesherContext ctx) {
        byte aboveId = ctx.scratchBlocks[gridIndex(gx, gy + 1, gz)];
        if (aboveId == BlockType.AIR.id) {
            emitFaceFast(transData, BlockType.WATER, 0, x, y, z, gx, gy, gz, wx, wy, wz, world, ctx);
        }
    }

    private static void emitCrossQuads(FloatList out, BlockType block,
                                       int lx, int ly, int lz, int gx, int gy, int gz,
                                       long wx, long wy, long wz, World world, MesherContext ctx) {
        int tex = block.sideTex;
        float u0 = TextureAtlas.getU0(tex);
        float v0 = TextureAtlas.getV0(tex);
        float u1 = TextureAtlas.getU1(tex);
        float v1 = TextureAtlas.getV1(tex);

        int i0 = gridIndex(gx, gy, gz);
        long p = packVertexLight(0.95f, block.emissive, i0, i0, i0, i0, ctx);

        float x0 = lx, x1 = lx + 1.0f;
        float y0 = ly, y1 = ly + 1.0f;
        float z0 = lz, z1 = lz + 1.0f;

        float r = 1.0f, g = 1.0f, b = 1.0f;
        if (block == BlockType.TALL_GRASS && world != null) {
            float sumR = 0, sumG = 0, sumB = 0;
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    Biome bm = world.getBiome(wx + dx, wz + dz);
                    if (bm != null) {
                        float[] c = bm.getGrassColor();
                        sumR += c[0];
                        sumG += c[1];
                        sumB += c[2];
                    }
                }
            }
            r = sumR / 9.0f;
            g = sumG / 9.0f;
            b = sumB / 9.0f;
        }

        // Diagonal 1: (x0, y0, z0) -> (x1, y1, z1) (both front & back)
        addQuadPacked(out, x0, y0, z0, u0, v1, p, x1, y0, z1, u1, v1, p, x1, y1, z1, u1, v0, p, x0, y1, z0, u0, v0, p, r, g, b);
        addQuadPacked(out, x1, y0, z1, u0, v1, p, x0, y0, z0, u1, v1, p, x0, y1, z0, u1, v0, p, x1, y1, z1, u0, v0, p, r, g, b);

        // Diagonal 2: (x0, y0, z1) -> (x1, y0, z0) (both front & back)
        addQuadPacked(out, x0, y0, z1, u0, v1, p, x1, y0, z0, u1, v1, p, x1, y1, z0, u1, v0, p, x0, y1, z1, u0, v0, p, r, g, b);
        addQuadPacked(out, x1, y0, z0, u0, v1, p, x0, y0, z1, u1, v1, p, x0, y1, z1, u1, v0, p, x1, y1, z0, u0, v0, p, r, g, b);
    }

    private static void propagateLight(byte[] lightGrid, boolean[] occluderGrid,
                                       int[] queue, int head, int tail, boolean isSky) {
        final int strideY = GRID_SIZE * GRID_SIZE;
        final int strideZ = GRID_SIZE;

        while (head < tail) {
            int idx = queue[head++];
            int curLight = lightGrid[idx] & 0xFF;
            if (curLight <= 1) continue;

            int gy = idx / strideY;
            int rem = idx - gy * strideY;
            int gz = rem / strideZ;
            int gx = rem - gz * strideZ;

            int nextHorizontal = curLight - 1;
            int nextDown = (isSky && curLight == 15) ? 15 : curLight - 1;

            if (gx + 1 < GRID_SIZE) {
                int nIdx = idx + 1;
                if (!occluderGrid[nIdx] && (lightGrid[nIdx] & 0xFF) < nextHorizontal) {
                    lightGrid[nIdx] = (byte) nextHorizontal;
                    queue[tail++] = nIdx;
                }
            }
            if (gx - 1 >= 0) {
                int nIdx = idx - 1;
                if (!occluderGrid[nIdx] && (lightGrid[nIdx] & 0xFF) < nextHorizontal) {
                    lightGrid[nIdx] = (byte) nextHorizontal;
                    queue[tail++] = nIdx;
                }
            }
            if (gz + 1 < GRID_SIZE) {
                int nIdx = idx + strideZ;
                if (!occluderGrid[nIdx] && (lightGrid[nIdx] & 0xFF) < nextHorizontal) {
                    lightGrid[nIdx] = (byte) nextHorizontal;
                    queue[tail++] = nIdx;
                }
            }
            if (gz - 1 >= 0) {
                int nIdx = idx - strideZ;
                if (!occluderGrid[nIdx] && (lightGrid[nIdx] & 0xFF) < nextHorizontal) {
                    lightGrid[nIdx] = (byte) nextHorizontal;
                    queue[tail++] = nIdx;
                }
            }
            if (gy + 1 < GRID_SIZE) {
                int nIdx = idx + strideY;
                if (!occluderGrid[nIdx] && (lightGrid[nIdx] & 0xFF) < nextHorizontal) {
                    lightGrid[nIdx] = (byte) nextHorizontal;
                    queue[tail++] = nIdx;
                }
            }
            if (gy - 1 >= 0) {
                int nIdx = idx - strideY;
                if (!occluderGrid[nIdx] && (lightGrid[nIdx] & 0xFF) < nextDown) {
                    lightGrid[nIdx] = (byte) nextDown;
                    queue[tail++] = nIdx;
                }
            }
        }
    }

    private static void emitFaceFast(FloatList out, BlockType block, int face,
                                     int lx, int ly, int lz, int gx, int gy, int gz,
                                     long wx, long wy, long wz, World world, MesherContext ctx) {
        int tex = block.getTextureForFace(face);
        float u0 = TextureAtlas.getU0(tex);
        float v0 = TextureAtlas.getV0(tex);
        float u1 = TextureAtlas.getU1(tex);
        float v1 = TextureAtlas.getV1(tex);

        float baseShade = FACE_SHADE[face];
        boolean isEmissive = block.emissive;

        float x0 = lx, x1 = lx + 1.0f;
        float y0 = ly, y1 = ly + 1.0f;
        float z0 = lz, z1 = lz + 1.0f;

        float r = 1.0f, g = 1.0f, b = 1.0f;
        if (face == 0 && (block == BlockType.GRASS || block == BlockType.TAIGA_GRASS || block == BlockType.JUNGLE_GRASS) && world != null) {
            float sumR = 0, sumG = 0, sumB = 0;
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    Biome bm = world.getBiome(wx + dx, wz + dz);
                    if (bm != null) {
                        float[] c = bm.getGrassColor();
                        sumR += c[0];
                        sumG += c[1];
                        sumB += c[2];
                    }
                }
            }
            r = sumR / 9.0f;
            g = sumG / 9.0f;
            b = sumB / 9.0f;
        }

        if (block.isWater()) {
            byte aboveId = ctx.scratchBlocks[gridIndex(gx, gy + 1, gz)];
            if (!BlockType.isWater(aboveId)) {
                int level = BlockType.getWaterLevel(block.id);
                float hFactor = (level >= 8) ? 0.90f : ((level / 8.0f) * 0.72f + 0.18f);
                y1 = ly + hFactor;
            }
        }

        switch (face) {
            case 0 -> { // +Y (top)
                int i0 = gridIndex(gx, gy + 1, gz);
                long p00 = packVertexLight(baseShade, isEmissive, i0, gridIndex(gx - 1, gy + 1, gz), gridIndex(gx, gy + 1, gz - 1), gridIndex(gx - 1, gy + 1, gz - 1), ctx);
                long p01 = packVertexLight(baseShade, isEmissive, i0, gridIndex(gx - 1, gy + 1, gz), gridIndex(gx, gy + 1, gz + 1), gridIndex(gx - 1, gy + 1, gz + 1), ctx);
                long p11 = packVertexLight(baseShade, isEmissive, i0, gridIndex(gx + 1, gy + 1, gz), gridIndex(gx, gy + 1, gz + 1), gridIndex(gx + 1, gy + 1, gz + 1), ctx);
                long p10 = packVertexLight(baseShade, isEmissive, i0, gridIndex(gx + 1, gy + 1, gz), gridIndex(gx, gy + 1, gz - 1), gridIndex(gx + 1, gy + 1, gz - 1), ctx);
                addQuadPacked(out, x0, y1, z0, u0, v0, p00, x0, y1, z1, u0, v1, p01, x1, y1, z1, u1, v1, p11, x1, y1, z0, u1, v0, p10, r, g, b);
            }
            case 1 -> { // -Y (bottom)
                int i0 = gridIndex(gx, gy - 1, gz);
                long p01 = packVertexLight(baseShade, isEmissive, i0, gridIndex(gx - 1, gy - 1, gz), gridIndex(gx, gy - 1, gz + 1), gridIndex(gx - 1, gy - 1, gz + 1), ctx);
                long p00 = packVertexLight(baseShade, isEmissive, i0, gridIndex(gx - 1, gy - 1, gz), gridIndex(gx, gy - 1, gz - 1), gridIndex(gx - 1, gy - 1, gz - 1), ctx);
                long p10 = packVertexLight(baseShade, isEmissive, i0, gridIndex(gx + 1, gy - 1, gz), gridIndex(gx, gy - 1, gz - 1), gridIndex(gx + 1, gy - 1, gz - 1), ctx);
                long p11 = packVertexLight(baseShade, isEmissive, i0, gridIndex(gx + 1, gy - 1, gz), gridIndex(gx, gy - 1, gz + 1), gridIndex(gx + 1, gy - 1, gz + 1), ctx);
                addQuadPacked(out, x0, y0, z1, u0, v0, p01, x0, y0, z0, u0, v1, p00, x1, y0, z0, u1, v1, p10, x1, y0, z1, u1, v0, p11, r, g, b);
            }
            case 2 -> { // +Z (south)
                int i0 = gridIndex(gx, gy, gz + 1);
                long p00 = packVertexLight(baseShade, isEmissive, i0, gridIndex(gx - 1, gy, gz + 1), gridIndex(gx, gy - 1, gz + 1), gridIndex(gx - 1, gy - 1, gz + 1), ctx);
                long p10 = packVertexLight(baseShade, isEmissive, i0, gridIndex(gx + 1, gy, gz + 1), gridIndex(gx, gy - 1, gz + 1), gridIndex(gx + 1, gy - 1, gz + 1), ctx);
                long p11 = packVertexLight(baseShade, isEmissive, i0, gridIndex(gx + 1, gy, gz + 1), gridIndex(gx, gy + 1, gz + 1), gridIndex(gx + 1, gy + 1, gz + 1), ctx);
                long p01 = packVertexLight(baseShade, isEmissive, i0, gridIndex(gx - 1, gy, gz + 1), gridIndex(gx, gy + 1, gz + 1), gridIndex(gx - 1, gy + 1, gz + 1), ctx);
                addQuadPacked(out, x0, y0, z1, u0, v1, p00, x1, y0, z1, u1, v1, p10, x1, y1, z1, u1, v0, p11, x0, y1, z1, u0, v0, p01, r, g, b);
            }
            case 3 -> { // -Z (north)
                int i0 = gridIndex(gx, gy, gz - 1);
                long p10 = packVertexLight(baseShade, isEmissive, i0, gridIndex(gx + 1, gy, gz - 1), gridIndex(gx, gy - 1, gz - 1), gridIndex(gx + 1, gy - 1, gz - 1), ctx);
                long p00 = packVertexLight(baseShade, isEmissive, i0, gridIndex(gx - 1, gy, gz - 1), gridIndex(gx, gy - 1, gz - 1), gridIndex(gx - 1, gy - 1, gz - 1), ctx);
                long p01 = packVertexLight(baseShade, isEmissive, i0, gridIndex(gx - 1, gy, gz - 1), gridIndex(gx, gy + 1, gz - 1), gridIndex(gx - 1, gy + 1, gz - 1), ctx);
                long p11 = packVertexLight(baseShade, isEmissive, i0, gridIndex(gx + 1, gy, gz - 1), gridIndex(gx, gy + 1, gz - 1), gridIndex(gx + 1, gy + 1, gz - 1), ctx);
                addQuadPacked(out, x1, y0, z0, u0, v1, p10, x0, y0, z0, u1, v1, p00, x0, y1, z0, u1, v0, p01, x1, y1, z0, u0, v0, p11, r, g, b);
            }
            case 4 -> { // +X (east)
                int i0 = gridIndex(gx + 1, gy, gz);
                long p1 = packVertexLight(baseShade, isEmissive, i0, gridIndex(gx + 1, gy, gz + 1), gridIndex(gx + 1, gy - 1, gz), gridIndex(gx + 1, gy - 1, gz + 1), ctx);
                long p2 = packVertexLight(baseShade, isEmissive, i0, gridIndex(gx + 1, gy, gz - 1), gridIndex(gx + 1, gy - 1, gz), gridIndex(gx + 1, gy - 1, gz - 1), ctx);
                long p3 = packVertexLight(baseShade, isEmissive, i0, gridIndex(gx + 1, gy, gz - 1), gridIndex(gx + 1, gy + 1, gz), gridIndex(gx + 1, gy + 1, gz - 1), ctx);
                long p4 = packVertexLight(baseShade, isEmissive, i0, gridIndex(gx + 1, gy, gz + 1), gridIndex(gx + 1, gy + 1, gz), gridIndex(gx + 1, gy + 1, gz + 1), ctx);
                addQuadPacked(out, x1, y0, z1, u0, v1, p1, x1, y0, z0, u1, v1, p2, x1, y1, z0, u1, v0, p3, x1, y1, z1, u0, v0, p4, r, g, b);
            }
            case 5 -> { // -X (west)
                int i0 = gridIndex(gx - 1, gy, gz);
                long p1 = packVertexLight(baseShade, isEmissive, i0, gridIndex(gx - 1, gy, gz - 1), gridIndex(gx - 1, gy - 1, gz), gridIndex(gx - 1, gy - 1, gz - 1), ctx);
                long p2 = packVertexLight(baseShade, isEmissive, i0, gridIndex(gx - 1, gy, gz + 1), gridIndex(gx - 1, gy - 1, gz), gridIndex(gx - 1, gy - 1, gz + 1), ctx);
                long p3 = packVertexLight(baseShade, isEmissive, i0, gridIndex(gx - 1, gy, gz + 1), gridIndex(gx - 1, gy + 1, gz), gridIndex(gx - 1, gy + 1, gz + 1), ctx);
                long p4 = packVertexLight(baseShade, isEmissive, i0, gridIndex(gx - 1, gy, gz - 1), gridIndex(gx - 1, gy + 1, gz), gridIndex(gx - 1, gy + 1, gz - 1), ctx);
                addQuadPacked(out, x0, y0, z0, u0, v1, p1, x0, y0, z1, u1, v1, p2, x0, y1, z1, u1, v0, p3, x0, y1, z0, u0, v0, p4, r, g, b);
            }
        }
    }

    private static long packVertexLight(float baseShade, boolean isEmissive, int i0, int i1, int i2, int ic, MesherContext ctx) {
        if (isEmissive) {
            return (long) Float.floatToRawIntBits(1.0f) & 0xFFFFFFFFL;
        }

        boolean b1 = ctx.scratchOccluder[i1];
        boolean b2 = ctx.scratchOccluder[i2];
        boolean bc = ctx.scratchOccluder[ic];

        int occ = (b1 && b2) ? 3 : ((b1 ? 1 : 0) + (b2 ? 1 : 0) + (bc ? 1 : 0));
        float ao = switch (occ) {
            case 0 -> 1.00f;
            case 1 -> 0.84f;
            case 2 -> 0.68f;
            default -> 0.54f;
        };

        int skySum = ctx.scratchSkyLight[i0] & 0xFF;
        int blkSum = ctx.scratchBlockLight[i0] & 0xFF;
        int samples = 1;

        if (!b1) {
            skySum += ctx.scratchSkyLight[i1] & 0xFF;
            blkSum += ctx.scratchBlockLight[i1] & 0xFF;
            samples++;
        }
        if (!b2) {
            skySum += ctx.scratchSkyLight[i2] & 0xFF;
            blkSum += ctx.scratchBlockLight[i2] & 0xFF;
            samples++;
        }
        if (!(b1 && b2) && !bc) {
            skySum += ctx.scratchSkyLight[ic] & 0xFF;
            blkSum += ctx.scratchBlockLight[ic] & 0xFF;
            samples++;
        }

        float inv = 1.0f / (samples * 15.0f);
        float skyNorm = skySum * inv;
        float blkNorm = blkSum * inv;

        float vSky = baseShade * ao * skyNorm;
        float vBlk = baseShade * ao * Math.max(0.28f, blkNorm);

        long hi = ((long) Float.floatToRawIntBits(vSky)) << 32;
        long lo = ((long) Float.floatToRawIntBits(vBlk)) & 0xFFFFFFFFL;
        return hi | lo;
    }

    private static void addQuadPacked(FloatList out,
                                      float x0, float y0, float z0, float u0, float v0, long p0,
                                      float x1, float y1, float z1, float u1, float v1, long p1,
                                      float x2, float y2, float z2, float u2, float v2, long p2,
                                      float x3, float y3, float z3, float u3, float v3, long p3,
                                      float r, float g, float b) {
        float sky0 = Float.intBitsToFloat((int) (p0 >>> 32));
        float blk0 = Float.intBitsToFloat((int) p0);
        float sky1 = Float.intBitsToFloat((int) (p1 >>> 32));
        float blk1 = Float.intBitsToFloat((int) p1);
        float sky2 = Float.intBitsToFloat((int) (p2 >>> 32));
        float blk2 = Float.intBitsToFloat((int) p2);
        float sky3 = Float.intBitsToFloat((int) (p3 >>> 32));
        float blk3 = Float.intBitsToFloat((int) p3);

        out.addVertex(x0, y0, z0, u0, v0, sky0, blk0, r, g, b);
        out.addVertex(x1, y1, z1, u0, v1, sky1, blk1, r, g, b);
        out.addVertex(x2, y2, z2, u1, v1, sky2, blk2, r, g, b);
        out.addVertex(x2, y2, z2, u1, v1, sky2, blk2, r, g, b);
        out.addVertex(x3, y3, z3, u1, v0, sky3, blk3, r, g, b);
        out.addVertex(x0, y0, z0, u0, v0, sky0, blk0, r, g, b);
    }
}
