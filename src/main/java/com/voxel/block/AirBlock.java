package com.voxel.block;

import com.voxel.registry.Identifier;

public class AirBlock extends Block {
    public AirBlock(Identifier id, int rawId) {
        super(id, rawId, Properties.of().solid(false).transparent(true));
    }
}
