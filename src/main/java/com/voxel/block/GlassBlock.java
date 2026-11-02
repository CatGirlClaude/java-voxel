package com.voxel.block;

import com.voxel.registry.Identifier;

public class GlassBlock extends Block {
    public GlassBlock(Identifier id, int rawId, Properties props) {
        super(id, rawId, props.solid(true).transparent(true));
    }
}
