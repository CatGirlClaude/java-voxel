package com.voxel.entity;

import com.voxel.registry.Identifier;
import com.voxel.registry.Registries;

public final class EntityTypes {
    public static final EntityType<Player> PLAYER = register("player", (type, world) -> new Player(world));
    public static final EntityType<MobEntity> MOB = register("mob", MobEntity::new);

    private EntityTypes() {}

    public static <T extends Entity> EntityType<T> register(String name, EntityType.EntityFactory<T> factory) {
        Identifier id = Identifier.of(name);
        EntityType<T> type = new EntityType<>(id, factory);
        Registries.ENTITY_TYPE.register(id, type);
        return type;
    }

    public static void init() {
        // Triggers static initialization
    }
}
