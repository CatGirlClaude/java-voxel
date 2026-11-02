package com.voxel.block;

import com.voxel.registry.Identifier;

public class LightBlock extends Block {
    public LightBlock(Identifier id, int rawId, Properties props) {
        super(id, rawId, props.solid(true).emissive(true));
    }
}
