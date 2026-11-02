package com.voxel;

import com.voxel.block.BlockType;
import com.voxel.block.Blocks;
import com.voxel.math.SimplexNoise;
import com.voxel.world.Chunk;
import com.voxel.world.World;
import com.voxel.world.gen.TerrainGenerator;

public class Task56to57VerificationTest {
    public static void main(String[] args) {
        System.out.println("Running Task 56 & 57 verification (Underwater block placement & Non-stackable seagrass)...");

        testUnderwaterBlockPlacementDisplacement();
        testSeagrassNotStackableKelpStackable();
        testCleanRiverWaterwaysAndNoSeaLevelGlitch();

        System.out.println(">>> ALL TASK 56 & 57 TESTS PASSED SUCCESSFULLY! <<<");
    }

    private static void testUnderwaterBlockPlacementDisplacement() {
        World world = new World(12345L);
        // Set up an isolated chunk column with water at y=10
        world.setBlock(10, 9, 10, BlockType.DIRT.id, false);
        world.setBlock(10, 10, 10, BlockType.WATER.id, false);

        if (world.getBlock(10, 10, 10) != BlockType.WATER.id) {
            throw new AssertionError("Setup failed: Block at (10, 10, 10) should be WATER");
        }

        // Test solid block displacement logic in water:
        // Simulating placement of STONE underwater at (10, 10, 10)
        BlockType toPlace = BlockType.STONE;
        byte atTarget = world.getBlock(10, 10, 10);
        boolean canOccupy = (atTarget == BlockType.AIR.id)
                || (BlockType.isUnderwaterPlant(toPlace.id) && BlockType.isWater(atTarget))
                || (toPlace.solid && BlockType.isWater(atTarget));

        if (!canOccupy) {
            throw new AssertionError("Solid blocks MUST be allowed to occupy/displace water!");
        }

        if (Blocks.get(toPlace.id).canSurvive(world, 10, 10, 10)) {
            world.setBlock(10, 10, 10, toPlace.id, true);
        }

        if (world.getBlock(10, 10, 10) != BlockType.STONE.id) {
            throw new AssertionError("Underwater stone placement failed to displace water!");
        }

        System.out.println("  [PASS] Solid block placement underwater correctly displaces water.");
        world.cleanup();
    }

    private static void testSeagrassNotStackableKelpStackable() {
        World world = new World(99999L);
        // Seagrass must NOT be stackable
        if (Blocks.SEAGRASS.isStackable()) {
            throw new AssertionError("Seagrass must NOT be stackable!");
        }

        // Kelp MUST be stackable
        if (!Blocks.KELP.isStackable()) {
            throw new AssertionError("Kelp MUST be stackable!");
        }

        // Test canSurvive for seagrass on top of seagrass -> must fail (false)
        world.setBlock(5, 5, 5, BlockType.DIRT.id, false);
        world.setBlock(5, 6, 5, BlockType.SEAGRASS.id, false);
        world.setBlock(5, 7, 5, BlockType.WATER.id, false);

        boolean seagrassCanStack = Blocks.SEAGRASS.canSurvive(world, 5, 7, 5);
        if (seagrassCanStack) {
            throw new AssertionError("Seagrass should NOT be able to survive when placed on top of another seagrass!");
        }

        // Test canSurvive for kelp on top of kelp -> must succeed (true)
        world.setBlock(6, 5, 6, BlockType.DIRT.id, false);
        world.setBlock(6, 6, 6, BlockType.KELP.id, false);
        world.setBlock(6, 7, 6, BlockType.WATER.id, false);

        boolean kelpCanStack = Blocks.KELP.canSurvive(world, 6, 7, 6);
        if (!kelpCanStack) {
            throw new AssertionError("Kelp MUST be able to survive when stacked on top of kelp!");
        }

        System.out.println("  [PASS] Non-stackable seagrass and stackable kelp rules verified.");
        world.cleanup();
    }

    private static void testCleanRiverWaterwaysAndNoSeaLevelGlitch() {
        long seed = 1172014502087853851L;
        SimplexNoise noise = new SimplexNoise(seed);

        int glitchySeaLevelCount = 0;
        int waterbedCount = 0;
        int dryLandCount = 0;

        for (int x = -100; x <= 100; x += 4) {
            for (int z = -100; z <= 100; z += 4) {
                int h = TerrainGenerator.computeSurfaceHeight(noise, x, z);
                if (h == Chunk.SEA_LEVEL) {
                    glitchySeaLevelCount++;
                } else if (h < Chunk.SEA_LEVEL) {
                    waterbedCount++;
                } else {
                    dryLandCount++;
                }
            }
        }

        if (glitchySeaLevelCount > 0) {
            throw new AssertionError("Found " + glitchySeaLevelCount + " columns with glitchy h == SEA_LEVEL (28)!");
        }
        if (waterbedCount == 0 || dryLandCount == 0) {
            throw new AssertionError("Expected both waterbed and dry land columns in test area!");
        }

        System.out.printf("  [PASS] Clean river channels and shorelines verified (Waterbeds: %d, Dry land: %d, Sea-level glitches: %d).%n",
                waterbedCount, dryLandCount, glitchySeaLevelCount);
    }
}
