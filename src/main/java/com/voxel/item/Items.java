package com.voxel.item;

import com.voxel.block.Blocks;
import com.voxel.registry.Identifier;
import com.voxel.registry.Registries;

public final class Items {
    private Items() {}

    public static Item get(Identifier id) {
        return Registries.ITEM.get(id);
    }

    public static Item get(int rawId) {
        return Registries.ITEM.get(rawId);
    }

    public static Item get(String id) {
        return Registries.ITEM.get(id);
    }

    public static void init() {
        Blocks.init();
    }
}
