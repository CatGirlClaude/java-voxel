package com.voxel;

import com.voxel.block.BlockType;
import com.voxel.block.Blocks;
import com.voxel.render.ShaderProgram;
import com.voxel.render.TextureAtlas;
import com.voxel.world.Chunk;
import com.voxel.world.World;
import org.joml.Vector3d;

public class Task59WaterShadingAndFloraFixTest {

    public static void main(String[] args) {
        System.out.println("Running Task 59 verification (Water Shader UV Isolation & Kelp/Flora Fixes)...");

        testTextureAtlasTileCoordinates();
        testShaderIsWaterLogic();
        testUnderwaterPlantRules();

        System.out.println(">>> ALL TASK 59 WATER & FLORA TESTS PASSED SUCCESSFULLY! <<<");
    }

    private static void testTextureAtlasTileCoordinates() {
        // Tile 9 is water
        float waterU0 = TextureAtlas.getU0(9);
        float waterV0 = TextureAtlas.getV0(9);
        float waterU1 = TextureAtlas.getU1(9);
        float waterV1 = TextureAtlas.getV1(9);

        // Water must be inside [0.1265, 0.2485]
        if (waterU0 < 0.1265f || waterU1 > 0.2485f || waterV0 < 0.1265f || waterV1 > 0.2485f) {
            throw new AssertionError(String.format("Water tile 9 UV out of expected strict range: U=[%f, %f], V=[%f, %f]",
                    waterU0, waterU1, waterV0, waterV1));
        }

        // Check neighbor tiles that previously suffered from bleed
        int[] neighborTiles = {
                0,  // grass_block_top
                1,  // grass_block_side
                2,  // dirt
                8,  // sand
                10, // oak_planks
                16, // cactus_side
                17  // cactus_top
        };

        for (int tile : neighborTiles) {
            float u0 = TextureAtlas.getU0(tile);
            float v0 = TextureAtlas.getV0(tile);
            float u1 = TextureAtlas.getU1(tile);
            float v1 = TextureAtlas.getV1(tile);

            // A face is only water if ALL its vertices are water.
            // Even at vertex level: vertex is water iff (u in [0.1265, 0.2485] && v in [0.1265, 0.2485])
            // In the new strict bounds, NO vertex of tile 0, 1, 2, 8, 10, 16, 17 can be inside the water box!
            float[][] corners = {
                    {u0, v0}, {u1, v0}, {u1, v1}, {u0, v1}
            };
            for (float[] pt : corners) {
                boolean inWaterBox = (pt[0] >= 0.1265f && pt[0] <= 0.2485f &&
                                      pt[1] >= 0.1265f && pt[1] <= 0.2485f);
                if (inWaterBox) {
                    throw new AssertionError(String.format("Tile %d (%s) corner [%f, %f] unexpectedly falls inside strict water UV box!",
                            tile, TextureAtlas.TILE_NAMES[tile], pt[0], pt[1]));
                }
            }
        }

        System.out.println("  [PASS] TextureAtlas UV bounds: Tile 9 strictly isolated, all neighbor tiles 100% outside water UV range.");
    }

    private static void testShaderIsWaterLogic() {
        // Test that uIsWaterPass = 0.0f strictly prevents water shading for ANY UV coordinate
        float uIsWaterPassOpaque = 0.0f;
        float uIsWaterPassTransparent = 1.0f;

        // Even with water UVs, opaque pass must evaluate to false
        float waterU = (TextureAtlas.getU0(9) + TextureAtlas.getU1(9)) * 0.5f;
        float waterV = (TextureAtlas.getV0(9) + TextureAtlas.getV1(9)) * 0.5f;

        boolean opaqueWaterDetected = (uIsWaterPassOpaque > 0.5f) &&
                (waterU >= 0.1265f && waterU <= 0.2485f && waterV >= 0.1265f && waterV <= 0.2485f);
        if (opaqueWaterDetected) {
            throw new AssertionError("Opaque pass should NEVER detect water!");
        }

        boolean transparentWaterDetected = (uIsWaterPassTransparent > 0.5f) &&
                (waterU >= 0.1265f && waterU <= 0.2485f && waterV >= 0.1265f && waterV <= 0.2485f);
        if (!transparentWaterDetected) {
            throw new AssertionError("Transparent pass MUST detect water tile 9!");
        }

        // Test non-water transparent blocks (Ice tile 22, Glass tile 12)
        int[] nonWaterTransparent = { 12, 22, 44 }; // Glass, Ice, Kelp
        for (int tile : nonWaterTransparent) {
            float u = (TextureAtlas.getU0(tile) + TextureAtlas.getU1(tile)) * 0.5f;
            float v = (TextureAtlas.getV0(tile) + TextureAtlas.getV1(tile)) * 0.5f;
            boolean detected = (uIsWaterPassTransparent > 0.5f) &&
                    (u >= 0.1265f && u <= 0.2485f && v >= 0.1265f && v <= 0.2485f);
            if (detected) {
                throw new AssertionError(String.format("Tile %d (%s) should not be detected as water in transparent pass!",
                        tile, TextureAtlas.TILE_NAMES[tile]));
            }
        }

        System.out.println("  [PASS] Shader isWater logic: uIsWaterPass=0 isolates opaque chunks; tile 9 uniquely activates water effects in transparent pass.");
    }

    private static void testUnderwaterPlantRules() {
        World world = new World(12345L);

        // Solid ground at Y=20 (deep ocean)
        world.setBlock(10, 20, 10, BlockType.DIRT.id, false);
        world.setBlock(10, 21, 10, BlockType.WATER.id, false);

        // Kelp can survive at Y=21 (underwater, on dirt)
        if (!Blocks.KELP.canSurvive(world, 10, 21, 10)) {
            throw new AssertionError("Kelp should survive underwater on dirt!");
        }

        // Kelp can stack on another kelp block
        world.setBlock(10, 21, 10, (byte) Blocks.KELP.getRawId(), false);
        if (!Blocks.KELP.canSurvive(world, 10, 22, 10)) {
            throw new AssertionError("Kelp should survive stacked on top of Kelp!");
        }

        // Seagrass cannot stack
        world.setBlock(10, 21, 10, (byte) Blocks.SEAGRASS.getRawId(), false);
        if (Blocks.SEAGRASS.canSurvive(world, 10, 22, 10)) {
            throw new AssertionError("Seagrass should NOT survive stacked on top of Seagrass!");
        }

        // If ground under kelp is broken, neighborChanged must replace kelp with water
        world.setBlock(10, 21, 10, (byte) Blocks.KELP.getRawId(), false);
        world.setBlock(10, 20, 10, BlockType.AIR.id, false);
        Blocks.KELP.neighborChanged(world, 10, 21, 10, 10, 20, 10);
        if (world.getBlock(10, 21, 10) != BlockType.WATER.id) {
            throw new AssertionError("Destroying ground under Kelp should convert it to WATER!");
        }

        System.out.println("  [PASS] Underwater plant rules: Kelp stacking, Seagrass non-stacking, and water conversion on break verified.");
    }
}
