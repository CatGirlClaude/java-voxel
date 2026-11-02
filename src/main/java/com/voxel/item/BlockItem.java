package com.voxel.item;

import com.voxel.block.Block;

public class BlockItem extends Item {
    private final Block block;

    public BlockItem(Block block, Properties properties) {
        super(block.getIdentifier(), block.getRawId(), properties);
        this.block = block;
    }

    public Block getBlock() {
        return block;
    }
}
