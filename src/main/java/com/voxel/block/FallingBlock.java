package com.voxel.block;

import com.voxel.registry.Identifier;
import com.voxel.world.World;

public class FallingBlock extends Block {
    public FallingBlock(Identifier id, int rawId, Properties props) {
        super(id, rawId, props.solid(true));
    }

    @Override
    public void neighborChanged(World world, long x, long y, long z, long fromX, long fromY, long fromZ) {
        if (fromY == y - 1 && fromX == x && fromZ == z) {
            checkFall(world, x, y, z);
        }
    }

    public void checkFall(World world, long x, long y, long z) {
        if (world == null) return;
        byte below = world.getBlock(x, y - 1, z);
        if (below == Blocks.AIR.getRawId() || below == Blocks.WATER.getRawId()) {
            world.queuePhysicsCheck(x, y, z);
        }
    }
}
