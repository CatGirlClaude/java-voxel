package com.voxel.block;

import com.voxel.registry.Identifier;

public class LogBlock extends Block {
    public LogBlock(Identifier id, int rawId, Properties props) {
        super(id, rawId, props.solid(true));
    }
}
