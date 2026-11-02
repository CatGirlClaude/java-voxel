package com.voxel.world;

import com.voxel.block.BlockType;
import com.voxel.math.SimplexNoise;
import com.voxel.world.gen.TerrainGenerator;
import com.voxel.world.mesh.ChunkMesher;
import com.voxel.world.mesh.FloatList;
import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;
import java.util.Arrays;

import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL11.GL_TRIANGLES;
import static org.lwjgl.opengl.GL11.glDrawArrays;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;
import static org.lwjgl.opengl.GL30.*;

public class Chunk {
    public static final int SIZE_X = 16;
    public static final int SIZE_Y = 16;
    public static final int SIZE_Z = 16;
    public static final int SEA_LEVEL = 28;
    public static final int LIGHT_PAD = 3;

    public final long chunkX;
    public final long chunkY;
    public final long chunkZ;

    private final byte[] blocks = new byte[SIZE_X * SIZE_Y * SIZE_Z];
    private boolean allAir = true;
    private int solidOpaqueCount = 0;

    private int opaqueVao = 0;
    private int opaqueVbo = 0;
    private int opaqueVertexCount = 0;

    private int transVao = 0;
    private int transVbo = 0;
    private int transVertexCount = 0;

    public boolean dirty = true;

    public Chunk(long chunkX, long chunkY, long chunkZ) {
        this.chunkX = chunkX;
        this.chunkY = chunkY;
        this.chunkZ = chunkZ;
    }

    private static int index(int x, int y, int z) {
        return (y * SIZE_Z + z) * SIZE_X + x;
    }

    public void clearBlocks() {
        Arrays.fill(blocks, BlockType.AIR.id);
        allAir = true;
        solidOpaqueCount = 0;
    }

    public byte getBlock(int x, int y, int z) {
        if (x < 0 || x >= SIZE_X || y < 0 || y >= SIZE_Y || z < 0 || z >= SIZE_Z) {
            return BlockType.AIR.id;
        }
        return blocks[index(x, y, z)];
    }

    public void setBlock(int x, int y, int z, byte id) {
        if (x < 0 || x >= SIZE_X || y < 0 || y >= SIZE_Y || z < 0 || z >= SIZE_Z) {
            return;
        }
        int idx = index(x, y, z);
        byte old = blocks[idx];
        if (old == id) return;

        BlockType oldBt = BlockType.fromId(old);
        if (oldBt.solid && !oldBt.transparent) {
            solidOpaqueCount--;
        }

        blocks[idx] = id;
        BlockType newBt = BlockType.fromId(id);
        if (newBt.solid && !newBt.transparent) {
            solidOpaqueCount++;
        }

        if (id != BlockType.AIR.id) {
            allAir = false;
        }
        dirty = true;
    }

    public boolean isAllAir() {
        return allAir;
    }

    public boolean isAllSolidOpaque() {
        return solidOpaqueCount == SIZE_X * SIZE_Y * SIZE_Z;
    }

    public byte[] getRawBlocks() {
        return blocks.clone();
    }

    public void setRawBlocks(byte[] newBlocks) {
        if (newBlocks == null || newBlocks.length != blocks.length) return;
        System.arraycopy(newBlocks, 0, blocks, 0, blocks.length);
        allAir = true;
        solidOpaqueCount = 0;
        for (int i = 0; i < blocks.length; i++) {
            byte id = blocks[i];
            if (id != BlockType.AIR.id) {
                allAir = false;
            }
            BlockType bt = BlockType.fromId(id);
            if (bt.solid && !bt.transparent) {
                solidOpaqueCount++;
            }
        }
        dirty = true;
    }

    public void generateTerrain(SimplexNoise noise) {
        TerrainGenerator.generateChunkTerrain(this, noise);
    }

    public void rebuildMesh(World world) {
        ChunkMesher.rebuildChunkMesh(this, world);
    }

    public void clearMeshCounts() {
        opaqueVertexCount = 0;
        transVertexCount = 0;
    }

    public void uploadMeshes(FloatList opaqueData, FloatList transData) {
        opaqueVertexCount = opaqueData.size / 10;
        transVertexCount = transData.size / 10;

        try {
            org.lwjgl.opengl.GL.getCapabilities();
        } catch (Throwable t) {
            // Brak aktywnego kontekstu OpenGL (np. srodowisko testowe, serwerowe lub headless)
            return;
        }

        uploadSingleMesh(opaqueData, true);
        uploadSingleMesh(transData, false);
    }

    private void uploadSingleMesh(FloatList list, boolean opaque) {
        if (list.size == 0) return;

        int vao = opaque ? opaqueVao : transVao;
        int vbo = opaque ? opaqueVbo : transVbo;

        if (vao == 0) {
            vao = glGenVertexArrays();
            vbo = glGenBuffers();
            if (opaque) {
                opaqueVao = vao;
                opaqueVbo = vbo;
            } else {
                transVao = vao;
                transVbo = vbo;
            }
        }

        FloatBuffer fb = BufferUtils.createFloatBuffer(list.size);
        fb.put(list.data, 0, list.size);
        fb.flip();

        int stride = 10 * Float.BYTES;
        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, fb, GL_STATIC_DRAW);

        glVertexAttribPointer(0, 3, GL_FLOAT, false, stride, 0);
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(1, 2, GL_FLOAT, false, stride, 3L * Float.BYTES);
        glEnableVertexAttribArray(1);
        glVertexAttribPointer(2, 1, GL_FLOAT, false, stride, 5L * Float.BYTES);
        glEnableVertexAttribArray(2);
        glVertexAttribPointer(3, 1, GL_FLOAT, false, stride, 6L * Float.BYTES);
        glEnableVertexAttribArray(3);
        glVertexAttribPointer(4, 3, GL_FLOAT, false, stride, 7L * Float.BYTES);
        glEnableVertexAttribArray(4);

        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);
    }

    public boolean hasOpaque() {
        return opaqueVertexCount > 0 && opaqueVao != 0;
    }

    public boolean hasTransparent() {
        return transVertexCount > 0 && transVao != 0;
    }

    public boolean hasAnyMesh() {
        return (opaqueVertexCount > 0 && opaqueVao != 0) || (transVertexCount > 0 && transVao != 0);
    }

    public void renderOpaque() {
        if (opaqueVertexCount > 0 && opaqueVao != 0) {
            glBindVertexArray(opaqueVao);
            glDrawArrays(GL_TRIANGLES, 0, opaqueVertexCount);
        }
    }

    public void renderTransparent() {
        if (transVertexCount > 0 && transVao != 0) {
            glBindVertexArray(transVao);
            glDrawArrays(GL_TRIANGLES, 0, transVertexCount);
        }
    }

    public void cleanup() {
        try {
            org.lwjgl.opengl.GL.getCapabilities();
        } catch (Throwable t) {
            opaqueVao = 0;
            opaqueVbo = 0;
            transVao = 0;
            transVbo = 0;
            return;
        }

        if (opaqueVbo != 0) glDeleteBuffers(opaqueVbo);
        if (opaqueVao != 0) glDeleteVertexArrays(opaqueVao);
        if (transVbo != 0) glDeleteBuffers(transVbo);
        if (transVao != 0) glDeleteVertexArrays(transVao);
        opaqueVao = 0;
        opaqueVbo = 0;
        transVao = 0;
        transVbo = 0;
    }
}
