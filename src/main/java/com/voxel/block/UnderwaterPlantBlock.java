package com.voxel.block;

import com.voxel.registry.Identifier;
import com.voxel.world.World;

public class UnderwaterPlantBlock extends PlantBlock {
    private final boolean stackable;

    public UnderwaterPlantBlock(Identifier id, int rawId, Properties props) {
        this(id, rawId, props, false);
    }

    public UnderwaterPlantBlock(Identifier id, int rawId, Properties props, boolean stackable) {
        super(id, rawId, props.solid(false).transparent(true).crossModel(true), false);
        this.stackable = stackable;
    }

    @Override
    public boolean isStackable() {
        return stackable;
    }

    @Override
    public boolean canSurvive(World world, long x, long y, long z) {
        if (world == null) return true;
        byte ground = world.getBlock(x, y - 1, z);
        if (stackable && ground == (byte) getRawId()) {
            return true; // Kelp can stack upwards
        }
        Block groundBlock = Blocks.get(ground);
        return groundBlock != null && groundBlock.isSolid() && ground != Blocks.AIR.getRawId() && !com.voxel.block.BlockType.isWater(ground);
    }

    @Override
    public void neighborChanged(World world, long x, long y, long z, long fromX, long fromY, long fromZ) {
        if (fromY == y - 1 && fromX == x && fromZ == z) {
            if (!canSurvive(world, x, y, z)) {
                world.setBlock(x, y, z, (byte) com.voxel.block.BlockType.WATER.id, true);
            }
        }
    }
}
