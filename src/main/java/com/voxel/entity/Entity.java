package com.voxel.entity;

import com.voxel.registry.Identifier;
import com.voxel.world.World;
import org.joml.Vector3d;
import org.joml.Vector3f;

public abstract class Entity {
    protected final EntityType<?> type;
    protected World world;
    public final Vector3d position = new Vector3d();
    public final Vector3f velocity = new Vector3f();
    public float yaw = 0.0f;
    public float pitch = 0.0f;
    public boolean onGround = false;
    public boolean removed = false;

    public Entity(EntityType<?> type, World world) {
        this.type = type;
        this.world = world;
    }

    public EntityType<?> getType() {
        return type;
    }

    public World getWorld() {
        return world;
    }

    public void setWorld(World world) {
        this.world = world;
    }

    public void setPosition(double x, double y, double z) {
        this.position.set(x, y, z);
    }

    public Vector3d getPosition() {
        return position;
    }

    public Vector3f getVelocity() {
        return velocity;
    }

    public abstract void tick(float dt);

    public boolean isRemoved() {
        return removed;
    }

    public void remove() {
        this.removed = true;
    }
}
