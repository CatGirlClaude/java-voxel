package com.voxel.block;

import com.voxel.registry.Identifier;

public class FluidBlock extends Block {
    public FluidBlock(Identifier id, int rawId, Properties props) {
        super(id, rawId, props.solid(false).transparent(true));
    }
}
