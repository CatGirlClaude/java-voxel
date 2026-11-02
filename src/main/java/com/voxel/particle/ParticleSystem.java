package com.voxel.particle;

import com.voxel.block.BlockType;
import com.voxel.render.ShaderProgram;
import com.voxel.render.TextureAtlas;
import com.voxel.world.Biome;
import com.voxel.world.World;
import org.joml.Matrix4f;
import org.joml.Vector3d;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

public class ParticleSystem {
    public static final int MAX_PARTICLES = 2048;
    private static final int FLOATS_PER_VERTEX = 11; // pos(3), uv(2), color(4), light(2)
    private static final int VERTICES_PER_PARTICLE = 6;
    private static final int FLOATS_PER_PARTICLE = VERTICES_PER_PARTICLE * FLOATS_PER_VERTEX;

    private final List<Particle> particles = new ArrayList<>(MAX_PARTICLES);
    private final FloatBuffer vertexBuffer = BufferUtils.createFloatBuffer(MAX_PARTICLES * FLOATS_PER_PARTICLE);
    private final Random rng = new Random();

    private int vao;
    private int vbo;
    private ShaderProgram particleShader;
    private boolean initialized = false;

    public ParticleSystem() {
        initGl();
    }

    private void initGl() {
        try {
            particleShader = ShaderProgram.createParticleShader();

            vao = glGenVertexArrays();
            vbo = glGenBuffers();

            glBindVertexArray(vao);
            glBindBuffer(GL_ARRAY_BUFFER, vbo);
            glBufferData(GL_ARRAY_BUFFER, (long) MAX_PARTICLES * FLOATS_PER_PARTICLE * Float.BYTES, GL_DYNAMIC_DRAW);

            int stride = FLOATS_PER_VERTEX * Float.BYTES;

            // 0: aPos (vec3)
            glEnableVertexAttribArray(0);
            glVertexAttribPointer(0, 3, GL_FLOAT, false, stride, 0);

            // 1: aTexCoord (vec2)
            glEnableVertexAttribArray(1);
            glVertexAttribPointer(1, 2, GL_FLOAT, false, stride, 3 * Float.BYTES);

            // 2: aColor (vec4)
            glEnableVertexAttribArray(2);
            glVertexAttribPointer(2, 4, GL_FLOAT, false, stride, 5 * Float.BYTES);

            // 3: aLight (vec2)
            glEnableVertexAttribArray(3);
            glVertexAttribPointer(3, 2, GL_FLOAT, false, stride, 9 * Float.BYTES);

            glBindVertexArray(0);
            glBindBuffer(GL_ARRAY_BUFFER, 0);

            initialized = true;
        } catch (Exception e) {
            initialized = false;
        }
    }

    public void update(float dt, World world) {
        dt = Math.min(dt, 0.05f);

        for (int i = particles.size() - 1; i >= 0; i--) {
            Particle p = particles.get(i);
            p.lifetime -= dt;

            if (p.isDead()) {
                int lastIdx = particles.size() - 1;
                particles.set(i, particles.get(lastIdx));
                particles.remove(lastIdx);
                continue;
            }

            p.vy -= p.gravity * dt;
            float dragFactor = Math.max(0.0f, 1.0f - (1.0f - p.drag) * dt * 60.0f);
            p.vx *= dragFactor;
            p.vz *= dragFactor;

            double nx = p.x + p.vx * dt;
            double ny = p.y + p.vy * dt;
            double nz = p.z + p.vz * dt;

            if (world != null) {
                // Vertical collision
                long by = (long) Math.floor(ny);
                long bx = (long) Math.floor(p.x);
                long bz = (long) Math.floor(p.z);
                if (p.vy < 0.0f && BlockType.fromId(world.getBlock(bx, by, bz)).solid) {
                    p.y = by + 1.0;
                    p.vy = 0.0f;
                    p.onGround = true;
                    p.vx *= 0.65f;
                    p.vz *= 0.65f;
                } else if (p.vy > 0.0f && BlockType.fromId(world.getBlock(bx, (long) Math.floor(ny + p.size), bz)).solid) {
                    p.vy = 0.0f;
                } else {
                    p.y = ny;
                }

                // Horizontal X collision
                long nbx = (long) Math.floor(nx);
                if (BlockType.fromId(world.getBlock(nbx, (long) Math.floor(p.y), bz)).solid) {
                    p.vx = -p.vx * 0.35f;
                } else {
                    p.x = nx;
                }

                // Horizontal Z collision
                long nbz = (long) Math.floor(nz);
                if (BlockType.fromId(world.getBlock((long) Math.floor(p.x), (long) Math.floor(p.y), nbz)).solid) {
                    p.vz = -p.vz * 0.35f;
                } else {
                    p.z = nz;
                }
            } else {
                p.x = nx;
                p.y = ny;
                p.z = nz;
            }

            // Smooth fade-out in final 0.25 seconds
            if (p.lifetime < 0.25f) {
                p.a = Math.max(0.0f, p.lifetime / 0.25f);
            }
        }
    }

    public void spawnBlockBreak(double bx, double by, double bz, BlockType block, World world) {
        if (block == null || block == BlockType.AIR) return;

        int tex = block.sideTex;
        float baseU0 = TextureAtlas.getU0(tex);
        float baseV0 = TextureAtlas.getV0(tex);
        float baseU1 = TextureAtlas.getU1(tex);
        float baseV1 = TextureAtlas.getV1(tex);
        float tileW = baseU1 - baseU0;
        float tileH = baseV1 - baseV0;

        float cr = 1.0f, cg = 1.0f, cb = 1.0f;
        if (block == BlockType.GRASS && world != null) {
            Biome b = world.getBiome((long) bx, (long) bz);
            cr = b.grassR;
            cg = b.grassG;
            cb = b.grassB;
        }

        float skyLight = 1.0f;
        float blockLight = 0.25f;
        if (world != null) {
            skyLight = (by >= world.getTopOpaqueY((long) bx, (long) bz)) ? 1.0f : 0.45f;
        }
        skyLight = Math.max(0.2f, skyLight);

        int count = 16 + rng.nextInt(9);
        for (int i = 0; i < count; i++) {
            if (particles.size() >= MAX_PARTICLES) break;

            double px = bx + 0.15 + rng.nextDouble() * 0.70;
            double py = by + 0.15 + rng.nextDouble() * 0.70;
            double pz = bz + 0.15 + rng.nextDouble() * 0.70;

            float vx = (rng.nextFloat() - 0.5f) * 3.8f;
            float vy = rng.nextFloat() * 3.2f + 1.2f;
            float vz = (rng.nextFloat() - 0.5f) * 3.8f;

            // Sample a 4x4 micro-texture patch inside the 16x16 block tile
            float subU = baseU0 + rng.nextFloat() * (tileW * 0.70f);
            float subV = baseV0 + rng.nextFloat() * (tileH * 0.70f);
            float subW = tileW * 0.25f;
            float subH = tileH * 0.25f;

            float size = 0.07f + rng.nextFloat() * 0.05f;
            float lifetime = 0.65f + rng.nextFloat() * 0.40f;

            // Random slight shading variance
            float shade = 0.85f + rng.nextFloat() * 0.25f;

            Particle p = new Particle(px, py, pz, vx, vy, vz,
                    subU, subV, subU + subW, subV + subH,
                    cr * shade, cg * shade, cb * shade, 1.0f,
                    size, lifetime, skyLight, blockLight);
            particles.add(p);
        }
    }

    public void spawnWaterSplash(double x, double y, double z, float speed) {
        int tex = 9; // Water tile
        float baseU0 = TextureAtlas.getU0(tex);
        float baseV0 = TextureAtlas.getV0(tex);
        float baseU1 = TextureAtlas.getU1(tex);
        float baseV1 = TextureAtlas.getV1(tex);

        int count = 12 + rng.nextInt(6);
        for (int i = 0; i < count; i++) {
            if (particles.size() >= MAX_PARTICLES) break;

            double px = x + (rng.nextDouble() - 0.5) * 0.6;
            double py = y + 0.1 + rng.nextDouble() * 0.2;
            double pz = z + (rng.nextDouble() - 0.5) * 0.6;

            float angle = rng.nextFloat() * 6.28318f;
            float spd = 1.0f + rng.nextFloat() * Math.max(1.5f, speed * 0.8f);
            float vx = (float) Math.cos(angle) * spd;
            float vy = 2.2f + rng.nextFloat() * 2.8f;
            float vz = (float) Math.sin(angle) * spd;

            float size = 0.05f + rng.nextFloat() * 0.04f;
            float lifetime = 0.45f + rng.nextFloat() * 0.30f;

            Particle p = new Particle(px, py, pz, vx, vy, vz,
                    baseU0, baseV0, baseU1, baseV1,
                    0.75f, 0.90f, 1.00f, 0.85f,
                    size, lifetime, 1.0f, 0.0f);
            p.gravity = 22.0f;
            particles.add(p);
        }
    }

    public void spawnFootstepDust(double x, double y, double z, BlockType block) {
        if (block == null || !block.solid || block.isWater()) return;

        int tex = block.topTex;
        float baseU0 = TextureAtlas.getU0(tex);
        float baseV0 = TextureAtlas.getV0(tex);
        float baseU1 = TextureAtlas.getU1(tex);
        float baseV1 = TextureAtlas.getV1(tex);

        for (int i = 0; i < 4; i++) {
            if (particles.size() >= MAX_PARTICLES) break;

            double px = x + (rng.nextDouble() - 0.5) * 0.4;
            double py = y + 0.05;
            double pz = z + (rng.nextDouble() - 0.5) * 0.4;

            float vx = (rng.nextFloat() - 0.5f) * 0.8f;
            float vy = 0.5f + rng.nextFloat() * 0.8f;
            float vz = (rng.nextFloat() - 0.5f) * 0.8f;

            float size = 0.04f + rng.nextFloat() * 0.03f;
            float lifetime = 0.30f + rng.nextFloat() * 0.20f;

            Particle p = new Particle(px, py, pz, vx, vy, vz,
                    baseU0, baseV0, baseU1, baseV1,
                    0.80f, 0.80f, 0.80f, 0.70f,
                    size, lifetime, 1.0f, 0.0f);
            p.gravity = 10.0f;
            particles.add(p);
        }
    }

    public void render(Vector3d eyePos, Matrix4f projView, TextureAtlas atlas, float sunIntensity, Vector3f lookDir) {
        if (!initialized || particles.isEmpty() || particleShader == null) return;

        // Calculate camera right and up basis vectors from look direction
        Vector3f forward = new Vector3f(lookDir).normalize();
        Vector3f right = new Vector3f(forward).cross(0.0f, 1.0f, 0.0f).normalize();
        if (right.lengthSquared() < 0.001f) {
            right = new Vector3f(1.0f, 0.0f, 0.0f);
        }
        Vector3f up = new Vector3f(right).cross(forward).normalize();

        vertexBuffer.clear();
        int count = 0;

        for (int i = 0; i < particles.size(); i++) {
            Particle p = particles.get(i);
            if (p.isDead()) continue;

            float rx = (float) (p.x - eyePos.x);
            float ry = (float) (p.y - eyePos.y);
            float rz = (float) (p.z - eyePos.z);
            float hs = p.size * 0.5f;

            float rx_r = right.x * hs;
            float ry_r = right.y * hs;
            float rz_r = right.z * hs;

            float rx_u = up.x * hs;
            float ry_u = up.y * hs;
            float rz_u = up.z * hs;

            // 4 Quad Corners:
            // c0: -right, -up
            float x0 = rx - rx_r - rx_u;
            float y0 = ry - ry_r - ry_u;
            float z0 = rz - rz_r - rz_u;

            // c1: +right, -up
            float x1 = rx + rx_r - rx_u;
            float y1 = ry + ry_r - ry_u;
            float z1 = rz + rz_r - rz_u;

            // c2: +right, +up
            float x2 = rx + rx_r + rx_u;
            float y2 = ry + ry_r + ry_u;
            float z2 = rz + rz_r + rz_u;

            // c3: -right, +up
            float x3 = rx - rx_r + rx_u;
            float y3 = ry - ry_r + ry_u;
            float z3 = rz - rz_r + rz_u;

            // Triangle 1: c0, c1, c2
            addVertex(x0, y0, z0, p.u0, p.v1, p.r, p.g, p.b, p.a, p.skyLight, p.blockLight);
            addVertex(x1, y1, z1, p.u1, p.v1, p.r, p.g, p.b, p.a, p.skyLight, p.blockLight);
            addVertex(x2, y2, z2, p.u1, p.v0, p.r, p.g, p.b, p.a, p.skyLight, p.blockLight);

            // Triangle 2: c2, c3, c0
            addVertex(x2, y2, z2, p.u1, p.v0, p.r, p.g, p.b, p.a, p.skyLight, p.blockLight);
            addVertex(x3, y3, z3, p.u0, p.v0, p.r, p.g, p.b, p.a, p.skyLight, p.blockLight);
            addVertex(x0, y0, z0, p.u0, p.v1, p.r, p.g, p.b, p.a, p.skyLight, p.blockLight);

            count++;
            if (count >= MAX_PARTICLES) break;
        }

        if (count == 0) return;

        vertexBuffer.flip();

        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferSubData(GL_ARRAY_BUFFER, 0, vertexBuffer);
        glBindBuffer(GL_ARRAY_BUFFER, 0);

        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glEnable(GL_DEPTH_TEST);
        glDepthMask(false); // Particles do not write depth to prevent clipping artifacts

        particleShader.bind();
        particleShader.setUniform("uProjView", projView);
        particleShader.setUniform("uTexture", 0);
        particleShader.setUniform("uSunIntensity", sunIntensity);

        glActiveTexture(GL_TEXTURE0);
        if (atlas != null) atlas.bind();

        glBindVertexArray(vao);
        glDrawArrays(GL_TRIANGLES, 0, count * VERTICES_PER_PARTICLE);
        glBindVertexArray(0);

        particleShader.unbind();
        glDepthMask(true);
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

    public int getParticleCount() {
        return particles.size();
    }

    public void clear() {
        particles.clear();
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
        if (particleShader != null) {
            particleShader.cleanup();
            particleShader = null;
        }
        particles.clear();
        initialized = false;
    }
}
