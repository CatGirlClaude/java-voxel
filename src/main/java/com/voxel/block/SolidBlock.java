package com.voxel.block;

import com.voxel.registry.Identifier;

public class SolidBlock extends Block {
    public SolidBlock(Identifier id, int rawId, Properties props) {
        super(id, rawId, props.solid(true));
    }
}
