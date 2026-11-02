package com.voxel.block;

import com.voxel.registry.Identifier;

public class LeavesBlock extends Block {
    public LeavesBlock(Identifier id, int rawId, Properties props) {
        super(id, rawId, props.solid(true).transparent(true));
    }
}
