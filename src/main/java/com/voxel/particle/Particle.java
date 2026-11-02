package com.voxel.particle;

public class Particle {
    public double x, y, z;
    public float vx, vy, vz;
    public float u0, v0, u1, v1;
    public float r, g, b, a;
    public float size;
    public float lifetime;
    public float maxLifetime;
    public float skyLight;
    public float blockLight;
    public float gravity = 18.0f;
    public float drag = 0.98f;
    public boolean onGround = false;

    public Particle(double x, double y, double z,
                    float vx, float vy, float vz,
                    float u0, float v0, float u1, float v1,
                    float r, float g, float b, float a,
                    float size, float maxLifetime,
                    float skyLight, float blockLight) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.vx = vx;
        this.vy = vy;
        this.vz = vz;
        this.u0 = u0;
        this.v0 = v0;
        this.u1 = u1;
        this.v1 = v1;
        this.r = r;
        this.g = g;
        this.b = b;
        this.a = a;
        this.size = size;
        this.maxLifetime = maxLifetime;
        this.lifetime = maxLifetime;
        this.skyLight = skyLight;
        this.blockLight = blockLight;
    }

    public boolean isDead() {
        return lifetime <= 0.0f;
    }
}
