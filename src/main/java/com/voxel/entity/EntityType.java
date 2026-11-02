package com.voxel.entity;

import com.voxel.registry.Identifier;
import com.voxel.world.World;

public class EntityType<T extends Entity> {
    @FunctionalInterface
    public interface EntityFactory<T extends Entity> {
        T create(EntityType<T> type, World world);
    }

    private final Identifier identifier;
    private final EntityFactory<T> factory;

    public EntityType(Identifier identifier, EntityFactory<T> factory) {
        this.identifier = identifier;
        this.factory = factory;
    }

    public Identifier getIdentifier() {
        return identifier;
    }

    public T create(World world) {
        return factory != null ? factory.create(this, world) : null;
    }

    @Override
    public String toString() {
        return identifier.toString();
    }
}
