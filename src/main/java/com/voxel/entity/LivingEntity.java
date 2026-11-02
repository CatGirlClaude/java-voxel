package com.voxel.entity;

import com.voxel.world.World;
import org.joml.Vector3d;
import org.joml.Vector3f;

public abstract class LivingEntity extends Entity {
    protected float health = 20.0f;
    protected float maxHealth = 20.0f;
    protected float hurtTime = 0.0f;
    public double currentEyeHeight = 1.54;

    public LivingEntity(EntityType<?> type, World world) {
        super(type, world);
    }

    public float getHealth() { return health; }
    public float getMaxHealth() { return maxHealth; }
    public boolean isAlive() { return health > 0.0f && !removed; }

    public void heal(float amount) {
        if (amount > 0) {
            health = Math.min(maxHealth, health + amount);
        }
    }

    public void damage(float amount) {
        if (amount > 0) {
            health = Math.max(0.0f, health - amount);
            hurtTime = 0.5f;
            if (health <= 0.0f) {
                onDeath();
            }
        }
    }

    protected void onDeath() {}

    public Vector3d getEyePosition() {
        return new Vector3d(position.x, position.y + currentEyeHeight, position.z);
    }

    public Vector3f getLookDirection() {
        float radYaw = (float) Math.toRadians(yaw);
        float radPitch = (float) Math.toRadians(pitch);
        float cosPitch = (float) Math.cos(radPitch);
        return new Vector3f(
                (float) (Math.cos(radYaw) * cosPitch),
                (float) Math.sin(radPitch),
                (float) (Math.sin(radYaw) * cosPitch)
        ).normalize();
    }
}
