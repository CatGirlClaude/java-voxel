package com.voxel.block;

import com.voxel.registry.Identifier;
import com.voxel.world.World;

public class SugarCaneBlock extends PlantBlock {
    public SugarCaneBlock(Identifier id, int rawId, Properties props) {
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
            return true; // Stacking sugar cane
        }
        // Soil must be grass, dirt, sand, or red sand, and horizontally adjacent to water (at ground level y-1 or plant level y)
        if (ground == BlockType.GRASS.id || ground == BlockType.DIRT.id
                || ground == BlockType.SAND.id || ground == BlockType.RED_SAND.id) {
            long[][] sides = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
            for (long[] s : sides) {
                byte sideBlockGround = world.getBlock(x + s[0], y - 1, z + s[1]);
                if (BlockType.isWater(sideBlockGround)) {
                    return true;
                }
                byte sideBlockY = world.getBlock(x + s[0], y, z + s[1]);
                if (BlockType.isWater(sideBlockY)) {
                    return true;
                }
            }
        }
        return false;
    }
}
