package com.voxel.entity;

import com.voxel.world.World;

public class MobEntity extends LivingEntity {
    protected LivingEntity target = null;

    public MobEntity(EntityType<?> type, World world) {
        super(type, world);
    }

    public LivingEntity getTarget() {
        return target;
    }

    public void setTarget(LivingEntity target) {
        this.target = target;
    }

    @Override
    public void tick(float dt) {
        if (hurtTime > 0.0f) {
            hurtTime -= dt;
        }
    }
}
