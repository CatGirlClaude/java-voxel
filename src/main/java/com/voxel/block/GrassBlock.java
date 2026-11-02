package com.voxel.block;

import com.voxel.registry.Identifier;
import com.voxel.world.Biome;
import com.voxel.world.World;

public class GrassBlock extends Block {
    public GrassBlock(Identifier id, int rawId, Properties props) {
        super(id, rawId, props.solid(true));
    }

    @Override
    public float[] getColorMultiplier(World world, long x, long y, long z, int face) {
        if (face == 0 && world != null) {
            Biome biome = world.getBiome(x, z);
            if (biome != null) {
                return biome.getGrassColor();
            }
        }
        return null;
    }

    @Override
    public void neighborChanged(World world, long x, long y, long z, long fromX, long fromY, long fromZ) {
        if (fromY == y + 1 && fromX == x && fromZ == z) {
            byte aboveId = world.getBlock(x, y + 1, z);
            if (aboveId == Blocks.SNOW.getRawId()) {
                world.setBlock(x, y, z, (byte) Blocks.SNOWY_GRASS.getRawId(), false);
            }
        }
    }
}
