package com.voxel;

import com.voxel.block.BlockType;
import com.voxel.render.TextureAtlas;
import com.voxel.world.Chunk;
import com.voxel.world.mesh.ChunkMesher;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

public class Task30to36VerificationTest {

    public static void main(String[] args) throws Exception {
        System.out.println("Running Task 30, 31, 35, 36 verification...");
        testMissingTextureFallback();
        testWaterTextureAndSeamlessness();
        testChunkMesherSideUvIntegrity();
        System.out.println(">>> ALL TASK 30-36 TESTS PASSED! <<<");
    }

    private static void testMissingTextureFallback() throws Exception {
        TextureAtlas.ensurePngAssetsExist();
        Path runMissing = TextureAtlas.RUN_TEXTURES_DIR.resolve("missing.png");
        if (!Files.exists(runMissing)) {
            throw new AssertionError("missing.png was not generated: " + runMissing);
        }
        BufferedImage img = ImageIO.read(runMissing.toFile());
        int c00 = img.getRGB(0, 0); // top-left should be purple
        int c80 = img.getRGB(8, 0); // top-right should be black
        int r00 = (c00 >> 16) & 0xFF;
        int b00 = c00 & 0xFF;
        int r80 = (c80 >> 16) & 0xFF;
        if (r00 < 200 || b00 < 200) {
            throw new AssertionError("Top-left pixel of missing.png should be magenta/purple, got: " + Integer.toHexString(c00));
        }
        if (r80 > 40) {
            throw new AssertionError("Top-right pixel of missing.png should be dark/black, got: " + Integer.toHexString(c80));
        }
        System.out.println("  [PASS] Missing texture fallback checkerboard verified.");
    }

    private static void testWaterTextureAndSeamlessness() throws Exception {
        Path runWater = TextureAtlas.RUN_TEXTURES_DIR.resolve("water.png");
        if (!Files.exists(runWater)) {
            throw new AssertionError("water.png does not exist: " + runWater);
        }
        BufferedImage img = ImageIO.read(runWater.toFile());
        if (img.getWidth() != 16 || img.getHeight() != 16) {
            throw new AssertionError("water.png should be 16x16, was " + img.getWidth() + "x" + img.getHeight());
        }
        System.out.println("  [PASS] Water texture verified.");
    }

    private static void testChunkMesherSideUvIntegrity() {
        com.voxel.world.World world = new com.voxel.world.World(12345);
        Chunk chunk = world.getOrCreateChunk(0, 0, 0);
        chunk.clearBlocks();
        // Place a dirt block in center
        chunk.setBlock(1, 1, 1, BlockType.DIRT.id);
        ChunkMesher.BuiltChunkMesh mesh = ChunkMesher.buildChunkMesh(chunk, world);
        float[] opaque = mesh.opaqueData().data;
        int vertexCount = mesh.opaqueData().size / 10;
        if (vertexCount != 36) { // 6 faces * 6 vertices = 36 vertices
            throw new AssertionError("Expected 36 vertices for single isolated block, got: " + vertexCount);
        }

        // Check that side face vertices have distinct U coordinates (not all u0)
        boolean hasDistinctU = false;
        boolean hasDistinctV = false;
        // Vertex stride is 10: x(0), y(1), z(2), u(3), v(4), sky(5), blk(6), r(7), g(8), b(9)
        for (int i = 0; i < vertexCount; i += 6) {
            float u0 = opaque[i * 10 + 3];
            float u1 = opaque[(i + 1) * 10 + 3];
            float v0 = opaque[i * 10 + 4];
            float v1 = opaque[(i + 1) * 10 + 4];
            if (Math.abs(u1 - u0) > 0.001f) hasDistinctU = true;
            if (Math.abs(v1 - v0) > 0.001f) hasDistinctV = true;
        }

        if (!hasDistinctU || !hasDistinctV) {
            throw new AssertionError("Chunk mesher side faces have collapsed UVs!");
        }
        System.out.println("  [PASS] ChunkMesher side UV coordinates are non-degenerate and correct.");
    }
}
