package com.voxel.entity;

import com.voxel.audio.SoundSystem;
import com.voxel.block.BlockType;
import com.voxel.world.World;
import org.joml.Vector3d;
import org.joml.Vector3f;

public class ItemEntity extends Entity {
    public BlockType itemBlock;
    public int count;
    public float age = 0.0f;
    public float pickupDelay = 0.5f;

    public ItemEntity(World world, double x, double y, double z, BlockType itemBlock, int count) {
        super(EntityTypes.register("item", (type, w) -> new ItemEntity(w, 0, 0, 0, BlockType.DIRT, 1)), world);
        this.position.set(x, y, z);
        this.velocity.set(0, 0, 0);
        this.itemBlock = itemBlock;
        this.count = count;
    }

    public ItemEntity(World world, double x, double y, double z, Vector3d initialVel, BlockType itemBlock, int count) {
        this(world, x, y, z, itemBlock, count);
        this.velocity.set((float) initialVel.x, (float) initialVel.y, (float) initialVel.z);
    }

    @Override
    public void tick(float dt) {
        tick(dt, this.world, null);
    }

    public void tick(float dt, World world, Player player) {
        if (removed) return;
        dt = Math.min(dt, 0.05f);
        age += dt;
        if (pickupDelay > 0.0f) {
            pickupDelay -= dt;
        }

        // Despawn after 5 minutes (300 seconds)
        if (age > 300.0f) {
            removed = true;
            return;
        }

        // Physics & Gravity
        boolean inWater = false;
        if (world != null) {
            byte b = world.getBlock((long) Math.floor(position.x), (long) Math.floor(position.y), (long) Math.floor(position.z));
            inWater = BlockType.isWater(b);
        }

        if (inWater) {
            velocity.y = Math.min(velocity.y + 8.0f * dt, 1.2f);
            velocity.x *= (1.0f - 2.5f * dt);
            velocity.z *= (1.0f - 2.5f * dt);
        } else {
            velocity.y -= 20.0f * dt;
            velocity.x *= (1.0f - 1.2f * dt);
            velocity.z *= (1.0f - 1.2f * dt);
        }

        double nx = position.x + (double) velocity.x * dt;
        double ny = position.y + (double) velocity.y * dt;
        double nz = position.z + (double) velocity.z * dt;

        if (world != null) {
            // Vertical collision
            long by = (long) Math.floor(ny);
            long bx = (long) Math.floor(position.x);
            long bz = (long) Math.floor(position.z);
            if (velocity.y < 0.0f && BlockType.fromId(world.getBlock(bx, by, bz)).solid) {
                position.y = by + 1.0;
                velocity.y = 0.0f;
                onGround = true;
                velocity.x *= 0.7f;
                velocity.z *= 0.7f;
            } else if (velocity.y > 0.0f && BlockType.fromId(world.getBlock(bx, (long) Math.floor(ny + 0.35), bz)).solid) {
                velocity.y = 0.0f;
            } else {
                position.y = ny;
            }

            // Horizontal X collision
            long nbx = (long) Math.floor(nx);
            if (BlockType.fromId(world.getBlock(nbx, (long) Math.floor(position.y), bz)).solid) {
                velocity.x = -velocity.x * 0.3f;
            } else {
                position.x = nx;
            }

            // Horizontal Z collision
            long nbz = (long) Math.floor(nz);
            if (BlockType.fromId(world.getBlock((long) Math.floor(position.x), (long) Math.floor(position.y), nbz)).solid) {
                velocity.z = -velocity.z * 0.3f;
            } else {
                position.z = nz;
            }
        } else {
            position.x = nx;
            position.y = ny;
            position.z = nz;
        }

        // Magnet / Pickup check
        if (player != null && pickupDelay <= 0.0f) {
            double targetY = player.position.y + 0.5;
            double dx = player.position.x - position.x;
            double dy = targetY - position.y;
            double dz = player.position.z - position.z;
            double distSq = dx * dx + dy * dy + dz * dz;

            if (distSq < 2.25 * 2.25) {
                // Smoothly attract towards player
                double dist = Math.sqrt(distSq);
                if (dist > 0.001) {
                    double speed = 9.5;
                    position.x += (dx / dist) * speed * dt;
                    position.y += (dy / dist) * speed * dt;
                    position.z += (dz / dist) * speed * dt;
                }

                // Collection collision check
                double newDistSq = (player.position.x - position.x) * (player.position.x - position.x)
                        + (targetY - position.y) * (targetY - position.y)
                        + (player.position.z - position.z) * (player.position.z - position.z);
                if (newDistSq < 0.85 * 0.85 || distSq < 0.85 * 0.85) {
                    if (player.giveItem(itemBlock, count)) {
                        SoundSystem.playClickSound();
                        removed = true;
                    }
                }
            }
        }
    }

    public double getVisualY() {
        return position.y + Math.sin(age * 3.6) * 0.06;
    }

    public float getVisualRotation() {
        return (age * 135.0f) % 360.0f;
    }
}
