package com.voxel.entity;

import com.voxel.block.BlockType;
import com.voxel.render.ShaderProgram;
import com.voxel.render.TextureAtlas;
import com.voxel.world.World;
import org.joml.Matrix4f;
import org.joml.Vector3d;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

public class ItemEntityManager {
    public static final int MAX_ITEMS = 512;
    private static final int FLOATS_PER_VERTEX = 11;
    private static final int VERTICES_PER_ITEM = 6;
    private static final int FLOATS_PER_ITEM = VERTICES_PER_ITEM * FLOATS_PER_VERTEX;

    private final List<ItemEntity> items = new ArrayList<>(MAX_ITEMS);
    private final FloatBuffer vertexBuffer = BufferUtils.createFloatBuffer(MAX_ITEMS * FLOATS_PER_ITEM);

    private int vao;
    private int vbo;
    private ShaderProgram itemShader;
    private boolean initialized = false;

    public ItemEntityManager() {
        initGl();
    }

    private void initGl() {
        try {
            itemShader = ShaderProgram.createParticleShader();

            vao = glGenVertexArrays();
            vbo = glGenBuffers();

            glBindVertexArray(vao);
            glBindBuffer(GL_ARRAY_BUFFER, vbo);
            glBufferData(GL_ARRAY_BUFFER, (long) MAX_ITEMS * FLOATS_PER_ITEM * Float.BYTES, GL_DYNAMIC_DRAW);

            int stride = FLOATS_PER_VERTEX * Float.BYTES;

            glEnableVertexAttribArray(0);
            glVertexAttribPointer(0, 3, GL_FLOAT, false, stride, 0);

            glEnableVertexAttribArray(1);
            glVertexAttribPointer(1, 2, GL_FLOAT, false, stride, 3 * Float.BYTES);

            glEnableVertexAttribArray(2);
            glVertexAttribPointer(2, 4, GL_FLOAT, false, stride, 5 * Float.BYTES);

            glEnableVertexAttribArray(3);
            glVertexAttribPointer(3, 2, GL_FLOAT, false, stride, 9 * Float.BYTES);

            glBindVertexArray(0);
            glBindBuffer(GL_ARRAY_BUFFER, 0);

            initialized = true;
        } catch (Exception e) {
            initialized = false;
        }
    }

    public ItemEntity spawnItem(World world, double x, double y, double z, BlockType block, int count) {
        if (block == null || block == BlockType.AIR) return null;
        if (items.size() >= MAX_ITEMS) {
            items.remove(0);
        }
        ItemEntity item = new ItemEntity(world, x, y, z, block, count);
        items.add(item);
        return item;
    }

    public ItemEntity spawnItemWithVelocity(World world, double x, double y, double z, Vector3d vel, BlockType block, int count) {
        if (block == null || block == BlockType.AIR) return null;
        if (items.size() >= MAX_ITEMS) {
            items.remove(0);
        }
        ItemEntity item = new ItemEntity(world, x, y, z, vel, block, count);
        items.add(item);
        return item;
    }

    public void tick(float dt, World world, Player player) {
        for (int i = items.size() - 1; i >= 0; i--) {
            ItemEntity item = items.get(i);
            item.tick(dt, world, player);
            if (item.removed) {
                int lastIdx = items.size() - 1;
                items.set(i, items.get(lastIdx));
                items.remove(lastIdx);
            }
        }
    }

    public void render(Vector3d eyePos, Matrix4f projView, TextureAtlas atlas, float sunIntensity, Vector3f lookDir, World world) {
        if (!initialized || items.isEmpty() || itemShader == null) return;

        Vector3f forward = new Vector3f(lookDir).normalize();
        Vector3f right = new Vector3f(forward).cross(0.0f, 1.0f, 0.0f).normalize();
        if (right.lengthSquared() < 0.001f) {
            right = new Vector3f(1.0f, 0.0f, 0.0f);
        }
        Vector3f up = new Vector3f(right).cross(forward).normalize();

        vertexBuffer.clear();
        int count = 0;

        for (int i = 0; i < items.size(); i++) {
            ItemEntity item = items.get(i);
            if (item.removed || item.itemBlock == null) continue;

            float rx = (float) (item.position.x - eyePos.x);
            float ry = (float) (item.getVisualY() - eyePos.y);
            float rz = (float) (item.position.z - eyePos.z);
            float hs = 0.16f; // Dropped item size (32cm)

            float rx_r = right.x * hs;
            float ry_r = right.y * hs;
            float rz_r = right.z * hs;

            float rx_u = up.x * hs;
            float ry_u = up.y * hs;
            float rz_u = up.z * hs;

            float x0 = rx - rx_r - rx_u;
            float y0 = ry - ry_r - ry_u;
            float z0 = rz - rz_r - rz_u;

            float x1 = rx + rx_r - rx_u;
            float y1 = ry + ry_r - ry_u;
            float z1 = rz + rz_r - rz_u;

            float x2 = rx + rx_r + rx_u;
            float y2 = ry + ry_r + ry_u;
            float z2 = rz + rz_r + rz_u;

            float x3 = rx - rx_r + rx_u;
            float y3 = ry - ry_r + ry_u;
            float z3 = rz - rz_r + rz_u;

            int tex = item.itemBlock.sideTex;
            float u0 = TextureAtlas.getU0(tex);
            float v0 = TextureAtlas.getV0(tex);
            float u1 = TextureAtlas.getU1(tex);
            float v1 = TextureAtlas.getV1(tex);

            float skyLight = 1.0f;
            float blockLight = 0.25f;
            if (world != null) {
                skyLight = (item.position.y >= world.getTopOpaqueY((long) item.position.x, (long) item.position.z)) ? 1.0f : 0.45f;
            }

            // Triangle 1
            addVertex(x0, y0, z0, u0, v1, 1.0f, 1.0f, 1.0f, 1.0f, skyLight, blockLight);
            addVertex(x1, y1, z1, u1, v1, 1.0f, 1.0f, 1.0f, 1.0f, skyLight, blockLight);
            addVertex(x2, y2, z2, u1, v0, 1.0f, 1.0f, 1.0f, 1.0f, skyLight, blockLight);

            // Triangle 2
            addVertex(x2, y2, z2, u1, v0, 1.0f, 1.0f, 1.0f, 1.0f, skyLight, blockLight);
            addVertex(x3, y3, z3, u0, v0, 1.0f, 1.0f, 1.0f, 1.0f, skyLight, blockLight);
            addVertex(x0, y0, z0, u0, v1, 1.0f, 1.0f, 1.0f, 1.0f, skyLight, blockLight);

            count++;
            if (count >= MAX_ITEMS) break;
        }

        if (count == 0) return;

        vertexBuffer.flip();

        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferSubData(GL_ARRAY_BUFFER, 0, vertexBuffer);
        glBindBuffer(GL_ARRAY_BUFFER, 0);

        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glEnable(GL_DEPTH_TEST);

        itemShader.bind();
        itemShader.setUniform("uProjView", projView);
        itemShader.setUniform("uTexture", 0);
        itemShader.setUniform("uSunIntensity", sunIntensity);

        glActiveTexture(GL_TEXTURE0);
        if (atlas != null) atlas.bind();

        glBindVertexArray(vao);
        glDrawArrays(GL_TRIANGLES, 0, count * VERTICES_PER_ITEM);
        glBindVertexArray(0);

        itemShader.unbind();
        glDisable(GL_BLEND);
    }

    private void addVertex(float x, float y, float z,
                           float u, float v,
                           float r, float g, float b, float a,
                           float sky, float blk) {
        vertexBuffer.put(x).put(y).put(z);
        vertexBuffer.put(u).put(v);
        vertexBuffer.put(r).put(g).put(b).put(a);
        vertexBuffer.put(sky).put(blk);
    }

    public int getItemCount() {
        return items.size();
    }

    public void clear() {
        items.clear();
    }

    public void cleanup() {
        if (vao != 0) {
            glDeleteVertexArrays(vao);
            vao = 0;
        }
        if (vbo != 0) {
            glDeleteBuffers(vbo);
            vbo = 0;
        }
        if (itemShader != null) {
            itemShader.cleanup();
            itemShader = null;
        }
        items.clear();
        initialized = false;
    }
}
