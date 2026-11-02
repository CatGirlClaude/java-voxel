package com.voxel.block;

import com.voxel.registry.Identifier;
import com.voxel.world.World;

public class UnderwaterPlantBlock extends PlantBlock {
    public UnderwaterPlantBlock(Identifier id, int rawId, Properties props) {
        super(id, rawId, props.solid(false).transparent(true).crossModel(true), false);
    }

    @Override
    public boolean isStackable() {
        return true;
    }

    @Override
    public boolean canSurvive(World world, long x, long y, long z) {
        if (world == null) return true;
        byte ground = world.getBlock(x, y - 1, z);
        if (ground == (byte) getRawId()) {
            return true; // Kelp can stack upwards
        }
        Block groundBlock = Blocks.get(ground);
        return groundBlock != null && groundBlock.isSolid() && ground != Blocks.AIR.getRawId();
    }
}
