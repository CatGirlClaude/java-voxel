package com.voxel.registry;

import com.voxel.block.Block;
import com.voxel.entity.EntityType;
import com.voxel.item.Item;

public final class Registries {
    public static final Registry<Block> BLOCK = new SimpleRegistry<>("block");
    public static final Registry<Item> ITEM = new SimpleRegistry<>("item");
    public static final Registry<EntityType<?>> ENTITY_TYPE = new SimpleRegistry<>("entity_type");

    private Registries() {}

    public static void init() {
        // Forces static initialization of all registries
    }
}
