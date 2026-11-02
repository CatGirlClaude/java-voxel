package com.voxel.block;

import com.voxel.registry.Identifier;
import com.voxel.world.Biome;
import com.voxel.world.World;

public class PlantBlock extends Block {
    protected final boolean hasGrassTint;

    public PlantBlock(Identifier id, int rawId, Properties props, boolean hasGrassTint) {
        super(id, rawId, props.solid(false).transparent(true).crossModel(true));
        this.hasGrassTint = hasGrassTint;
    }

    public PlantBlock(Identifier id, int rawId, Properties props) {
        this(id, rawId, props, false);
    }

    @Override
    public boolean canSurvive(World world, long x, long y, long z) {
        if (world == null) return true;
        byte ground = world.getBlock(x, y - 1, z);
        Block groundBlock = Blocks.get(ground);
        return groundBlock != null && groundBlock.isSolid() && ground != Blocks.AIR.getRawId();
    }

    @Override
    public void neighborChanged(World world, long x, long y, long z, long fromX, long fromY, long fromZ) {
        if (fromY == y - 1 && fromX == x && fromZ == z) {
            if (!canSurvive(world, x, y, z)) {
                world.setBlock(x, y, z, (byte) Blocks.AIR.getRawId(), true);
            }
        }
    }

    @Override
    public float[] getColorMultiplier(World world, long x, long y, long z, int face) {
        if (hasGrassTint && world != null) {
            Biome biome = world.getBiome(x, z);
            if (biome != null) {
                return biome.getGrassColor();
            }
        }
        return null;
    }
}
