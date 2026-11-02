package com.voxel.block;

import com.voxel.registry.Identifier;
import com.voxel.world.World;

public class SnowyGrassBlock extends Block {
    public SnowyGrassBlock(Identifier id, int rawId, Properties props) {
        super(id, rawId, props.solid(true));
    }

    @Override
    public void neighborChanged(World world, long x, long y, long z, long fromX, long fromY, long fromZ) {
        if (fromY == y + 1 && fromX == x && fromZ == z) {
            byte aboveId = world.getBlock(x, y + 1, z);
            if (aboveId != Blocks.SNOW.getRawId()) {
                world.setBlock(x, y, z, (byte) Blocks.GRASS_BLOCK.getRawId(), false);
            }
        }
    }
}
