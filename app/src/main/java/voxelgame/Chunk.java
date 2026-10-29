package voxelgame;

import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL33.*;

public class Chunk {
    public static final int CHUNK_SIZE = 16;
    private int[][][] voxels;
    private int vao, vbo;
    private int vertexCount = 0;

    public Chunk() {
        voxels = new int[CHUNK_SIZE][CHUNK_SIZE][CHUNK_SIZE];
        for (int x = 0; x < CHUNK_SIZE; x++) {
            for (int y = 0; y < CHUNK_SIZE; y++) {
                for (int z = 0; z < CHUNK_SIZE; z++) {
                    // Simple terrain generation
                    if (y < 8) {
                        voxels[x][y][z] = 1; // 1 = solid block
                    } else {
                        voxels[x][y][z] = 0; // 0 = air
                    }
                }
            }
        }

        vao = glGenVertexArrays();
        vbo = glGenBuffers();
    }

    public void generateMesh() {
        List<Float> vertices = new ArrayList<>();

        for (int x = 0; x < CHUNK_SIZE; x++) {
            for (int y = 0; y < CHUNK_SIZE; y++) {
                for (int z = 0; z < CHUNK_SIZE; z++) {
                    if (voxels[x][y][z] != 0) {
                        addVoxelToMesh(vertices, x, y, z);
                    }
                }
            }
        }

        float[] vertexArray = new float[vertices.size()];
        for (int i = 0; i < vertices.size(); i++) {
            vertexArray[i] = vertices.get(i);
        }

        vertexCount = vertexArray.length / 6; // 3 for pos, 3 for color

        FloatBuffer vertexBuffer = BufferUtils.createFloatBuffer(vertexArray.length);
        vertexBuffer.put(vertexArray).flip();

        glBindVertexArray(vao);

        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, vertexBuffer, GL_STATIC_DRAW);

        // Position attribute
        glVertexAttribPointer(0, 3, GL_FLOAT, false, 6 * Float.BYTES, 0);
        glEnableVertexAttribArray(0);

        // Color attribute
        glVertexAttribPointer(1, 3, GL_FLOAT, false, 6 * Float.BYTES, 3 * Float.BYTES);
        glEnableVertexAttribArray(1);

        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);
    }

    private void addVoxelToMesh(List<Float> vertices, int x, int y, int z) {
        float r = 0.2f + (x % 2) * 0.1f;
        float g = 0.8f;
        float b = 0.2f + (z % 2) * 0.1f;

        // Front face
        if (z == CHUNK_SIZE - 1 || voxels[x][y][z + 1] == 0) {
            float[] face = {
                x, y, z + 1, r, g, b,
                x + 1, y, z + 1, r, g, b,
                x + 1, y + 1, z + 1, r, g, b,
                x + 1, y + 1, z + 1, r, g, b,
                x, y + 1, z + 1, r, g, b,
                x, y, z + 1, r, g, b
            };
            addFace(vertices, face);
        }

        // Back face
        if (z == 0 || voxels[x][y][z - 1] == 0) {
            float[] face = {
                x + 1, y, z, r, g, b,
                x, y, z, r, g, b,
                x, y + 1, z, r, g, b,
                x, y + 1, z, r, g, b,
                x + 1, y + 1, z, r, g, b,
                x + 1, y, z, r, g, b
            };
            addFace(vertices, face);
        }

        // Left face
        if (x == 0 || voxels[x - 1][y][z] == 0) {
            float[] face = {
                x, y, z, r, g, b,
                x, y, z + 1, r, g, b,
                x, y + 1, z + 1, r, g, b,
                x, y + 1, z + 1, r, g, b,
                x, y + 1, z, r, g, b,
                x, y, z, r, g, b
            };
            addFace(vertices, face);
        }

        // Right face
        if (x == CHUNK_SIZE - 1 || voxels[x + 1][y][z] == 0) {
            float[] face = {
                x + 1, y, z + 1, r, g, b,
                x + 1, y, z, r, g, b,
                x + 1, y + 1, z, r, g, b,
                x + 1, y + 1, z, r, g, b,
                x + 1, y + 1, z + 1, r, g, b,
                x + 1, y, z + 1, r, g, b
            };
            addFace(vertices, face);
        }

        // Top face
        if (y == CHUNK_SIZE - 1 || voxels[x][y + 1][z] == 0) {
            float[] face = {
                x, y + 1, z + 1, r, g, b,
                x + 1, y + 1, z + 1, r, g, b,
                x + 1, y + 1, z, r, g, b,
                x + 1, y + 1, z, r, g, b,
                x, y + 1, z, r, g, b,
                x, y + 1, z + 1, r, g, b
            };
            addFace(vertices, face);
        }

        // Bottom face
        if (y == 0 || voxels[x][y - 1][z] == 0) {
            float[] face = {
                x, y, z, r, g, b,
                x + 1, y, z, r, g, b,
                x + 1, y, z + 1, r, g, b,
                x + 1, y, z + 1, r, g, b,
                x, y, z + 1, r, g, b,
                x, y, z, r, g, b
            };
            addFace(vertices, face);
        }
    }

    private void addFace(List<Float> vertices, float[] face) {
        for (float f : face) {
            vertices.add(f);
        }
    }

    public int getVao() {
        return vao;
    }

    public int getVertexCount() {
        return vertexCount;
    }

    public void cleanup() {
        glDeleteVertexArrays(vao);
        glDeleteBuffers(vbo);
    }
}
