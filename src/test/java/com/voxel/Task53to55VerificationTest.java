package com.voxel;

import com.voxel.block.BlockType;
import com.voxel.block.Blocks;
import com.voxel.block.SugarCaneBlock;
import com.voxel.block.UnderwaterPlantBlock;
import com.voxel.render.TextureAtlas;
import com.voxel.world.Biome;
import com.voxel.world.World;
import org.joml.Vector3d;

import java.lang.reflect.Method;

public class Task53to55VerificationTest {

    public static void main(String[] args) {
        System.out.println("Running Task 53, 54, 55 verification...");
        testStackablePlants();
        testWaterWaveShaderAtlasRange();
        testClusteredFlowerPatches();
        System.out.println(">>> ALL TASK 53, 54, 55 TESTS PASSED! <<<");
    }

    private static void testStackablePlants() {
        Blocks.init();

        // 1. Verify isStackable() property
        if (!Blocks.SUGAR_CANE.isStackable()) {
            throw new AssertionError("Blocks.SUGAR_CANE must be stackable");
        }
        if (!Blocks.KELP.isStackable()) {
            throw new AssertionError("Blocks.KELP must be stackable");
        }
        if (Blocks.GRASS.isStackable()) {
            throw new AssertionError("Blocks.GRASS should not be stackable");
        }

        // 2. Verify canSurvive stacking behavior for Sugar Cane
        // When ground block is sugar cane, canSurvive should return true
        MockWorld mockWorld = new MockWorld();
        long testX = 10;
        long testY = 64;
        long testZ = 10;

        mockWorld.setBlockDirect(testX, testY - 1, testZ, (byte) Blocks.SUGAR_CANE.getRawId());
        if (!Blocks.SUGAR_CANE.canSurvive(mockWorld, testX, testY, testZ)) {
            throw new AssertionError("Sugar Cane should survive stacked on top of existing Sugar Cane block");
        }

        // 3. Verify canSurvive stacking behavior for Kelp
        mockWorld.setBlockDirect(testX, testY - 1, testZ, (byte) Blocks.KELP.getRawId());
        if (!Blocks.KELP.canSurvive(mockWorld, testX, testY, testZ)) {
            throw new AssertionError("Kelp should survive stacked on top of existing Kelp block");
        }

        System.out.println("  [PASS] Stackable plants (Sugar Cane & Kelp) logic verified.");
    }

    private static void testWaterWaveShaderAtlasRange() {
        // Tile 9 is water
        float u0 = TextureAtlas.getU0(9);
        float v0 = TextureAtlas.getV0(9);
        float u1 = TextureAtlas.getU1(9);
        float v1 = TextureAtlas.getV1(9);

        // Verify that tile 9 UV coordinates are contained within [0.120, 0.255]
        if (u0 < 0.120f || u1 > 0.255f || v0 < 0.120f || v1 > 0.255f) {
            throw new AssertionError(String.format("Water tile UVs [%f, %f, %f, %f] outside expected shader detection range [0.120, 0.255]",
                    u0, u1, v0, v1));
        }

        System.out.println("  [PASS] Water wave shader atlas detection range [0.120, 0.255] verified.");
    }

    private static void testClusteredFlowerPatches() {
        try {
            Class<?> genClass = Class.forName("com.voxel.world.gen.TerrainGenerator");
            Method patchMethod = genClass.getDeclaredMethod("getFlowerPatchSpecies", long.class, long.class, Biome.class);
            patchMethod.setAccessible(true);

            // Test that within the same patch cell (e.g. wx=5, wz=5 and wx=8, wz=8 in a 20x20 cell),
            // the patch species is IDENTICAL (cluster of identical flowers)
            byte species1 = (Byte) patchMethod.invoke(null, 5L, 5L, Biome.PLAINS);
            byte species2 = (Byte) patchMethod.invoke(null, 8L, 8L, Biome.PLAINS);
            byte species3 = (Byte) patchMethod.invoke(null, 12L, 10L, Biome.PLAINS);

            if (species1 != species2 || species2 != species3) {
                throw new AssertionError(String.format("Expected uniform patch species in same patch cell, but got %d, %d, %d",
                        species1, species2, species3));
            }

            // Test that moving to a distant cell (e.g. wx=150) samples different patch properties
            boolean foundDifferent = false;
            for (long offset = 20L; offset <= 200L; offset += 20L) {
                byte otherSpecies = (Byte) patchMethod.invoke(null, 5L + offset, 5L + offset, Biome.PLAINS);
                if (otherSpecies != species1) {
                    foundDifferent = true;
                    break;
                }
            }
            if (!foundDifferent) {
                throw new AssertionError("Expected different flower patch species in distant cells across the world");
            }

            System.out.println("  [PASS] Clustered flower patch generation verified.");
        } catch (Exception e) {
            throw new RuntimeException("Flower patch verification failed: " + e.getMessage(), e);
        }
    }

    // Lightweight mock world for Block.canSurvive survival tests
    private static class MockWorld extends World {
        private final java.util.Map<String, Byte> blocks = new java.util.HashMap<>();

        public MockWorld() {
            super(1337L);
        }

        public void setBlockDirect(long x, long y, long z, byte id) {
            blocks.put(x + "," + y + "," + z, id);
        }

        @Override
        public byte getBlock(long x, long y, long z) {
            return blocks.getOrDefault(x + "," + y + "," + z, (byte) 0);
        }
    }
}
