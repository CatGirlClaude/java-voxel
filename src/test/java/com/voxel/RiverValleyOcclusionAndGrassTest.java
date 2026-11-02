package com.voxel;

import com.voxel.block.BlockType;
import com.voxel.world.Chunk;
import com.voxel.world.World;
import org.joml.Vector3d;

public class RiverValleyOcclusionAndGrassTest {

    private static final long SEED = -4717798643180316884L;

    public static void main(String[] args) {
        System.out.println("Starting RiverValleyOcclusionAndGrassTest...");

        testRiverValleyChunksNotOccluded();
        testDeepUndergroundChunkIsStillOccluded();
        testRiverHillsHaveGrassAndWildflowers();

        System.out.println("ALL TESTS PASSED SUCCESSFULLY!");
    }

    private static void testRiverValleyChunksNotOccluded() {
        World world = new World(SEED);
        world.setRenderDistance(8);

        // Player location from screenshot: XYZ: 126.49 / 37.00 / -33.23 in River biome looking down valley
        Vector3d eyePos = new Vector3d(126.49, 37.00 + 1.62, -33.23);
        long pcx = Math.floorDiv((long) Math.floor(126.49), (long) Chunk.SIZE_X);
        long pcy = Math.floorDiv((long) Math.floor(37.00), (long) Chunk.SIZE_Y);
        long pcz = Math.floorDiv((long) Math.floor(-33.23), (long) Chunk.SIZE_Z);

        if (pcx != 7 || pcy != 2 || pcz != -3) {
            throw new AssertionError(String.format("Expected pcx=7, pcy=2, pcz=-3, but got %d, %d, %d", pcx, pcy, pcz));
        }

        // Pre-generate chunks along the river valley
        for (int dx = 0; dx <= 6; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                world.getOrCreateChunk(pcx + dx, 1, pcz + dz);
                world.getOrCreateChunk(pcx + dx, 2, pcz + dz);
            }
        }

        // River bed & valley floor chunks at chunkY=1 (Y=16..31) in front of player
        // Previously falsely occluded by midpoint mountain ridge heuristic:
        Chunk c10_1_m2 = world.getOrCreateChunk(10, 1, -2);
        Chunk c11_1_m2 = world.getOrCreateChunk(11, 1, -2);
        Chunk c12_1_m2 = world.getOrCreateChunk(12, 1, -2);

        if (world.isChunkOccluded(c10_1_m2, pcx, pcy, pcz, eyePos)) {
            throw new AssertionError("Chunk (10, 1, -2) down the river valley must NOT be occluded!");
        }
        if (world.isChunkOccluded(c11_1_m2, pcx, pcy, pcz, eyePos)) {
            throw new AssertionError("Chunk (11, 1, -2) down the river valley must NOT be occluded!");
        }
        if (world.isChunkOccluded(c12_1_m2, pcx, pcy, pcz, eyePos)) {
            throw new AssertionError("Chunk (12, 1, -2) down the river valley must NOT be occluded!");
        }

        System.out.println("  [PASS] River valley chunks correctly visible without false occlusion");
        world.cleanup();
    }

    private static void testDeepUndergroundChunkIsStillOccluded() {
        World world = new World(SEED);
        world.setRenderDistance(8);

        Vector3d eyePos = new Vector3d(126.49, 37.00 + 1.62, -33.23);
        long pcx = 7, pcy = 2, pcz = -3;

        // Chunk (7, -2, -3) is deep underground: Y = [-32..-17], player is on surface at Y=37
        Chunk deepChunk = world.getOrCreateChunk(pcx, -2, pcz);
        if (!world.isChunkOccluded(deepChunk, pcx, pcy, pcz, eyePos)) {
            throw new AssertionError("Deep subterranean chunk must be occluded when player is on surface");
        }

        System.out.println("  [PASS] Deep subterranean chunk correctly occluded");
        world.cleanup();
    }

    private static void testRiverHillsHaveGrassAndWildflowers() {
        World world = new World(SEED);
        world.setRenderDistance(8);

        // Generate chunk with riverbank / river hills
        Chunk c = world.getOrCreateChunk(8, 2, -3);

        int grassSurfaceCount = 0;
        int foliageCount = 0;

        for (int lx = 0; lx < Chunk.SIZE_X; lx++) {
            for (int lz = 0; lz < Chunk.SIZE_Z; lz++) {
                long wx = c.chunkX * Chunk.SIZE_X + lx;
                long wz = c.chunkZ * Chunk.SIZE_Z + lz;
                int surfY = world.getSurfaceHeight(wx, wz);

                // Check surface block on river hills (above water level SEA_LEVEL = 28)
                if (surfY > 28) {
                    byte block = world.getBlock(wx, surfY, wz);
                    if (block == BlockType.GRASS.id) {
                        grassSurfaceCount++;
                    }

                    byte decor = world.getBlock(wx, surfY + 1L, wz);
                    if (decor == BlockType.TALL_GRASS.id || decor == BlockType.DANDELION.id
                            || decor == BlockType.ROSE.id || decor == BlockType.BLUE_ORCHID.id
                            || decor == BlockType.ALLIUM.id || decor == BlockType.FLOWER.id) {
                        foliageCount++;
                    }
                }
            }
        }

        if (grassSurfaceCount <= 0) {
            throw new AssertionError("River hills must have grass surface blocks!");
        }
        if (foliageCount <= 0) {
            throw new AssertionError("River hills must have tall grass / wildflower foliage!");
        }

        System.out.printf("  [PASS] River hills grass surface count: %d, foliage count: %d%n",
                grassSurfaceCount, foliageCount);
        world.cleanup();
    }
}
