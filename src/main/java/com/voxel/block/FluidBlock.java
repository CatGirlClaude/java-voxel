package com.voxel.block;

import com.voxel.block.state.IntegerProperty;
import com.voxel.registry.Identifier;
import com.voxel.world.World;

public class FluidBlock extends Block {
    public static final IntegerProperty LEVEL = IntegerProperty.of("level", 1, 8);

    public FluidBlock(Identifier id, int rawId, Properties props) {
        super(id, rawId, props.solid(false).transparent(true), LEVEL);
        for (BlockState state : stateMap.values()) {
            if (state.get(LEVEL) == 8) {
                this.defaultState = state;
                break;
            }
        }
    }

    public BlockState getSourceState() {
        return getStateForLevel(8);
    }

    public BlockState getStateForLevel(int level) {
        int clamped = Math.max(1, Math.min(8, level));
        return defaultState.with(LEVEL, clamped);
    }

    public boolean isSource(BlockState state) {
        return state.get(LEVEL) == 8;
    }

    @Override
    public void tick(World world, long x, long y, long z) {
        byte blockId = world.getBlock(x, y, z);
        if (!BlockType.isWater(blockId)) return;

        byte below = world.getBlock(x, y - 1, z);
        String mode = com.voxel.config.GameSettings.waterPhysicsMode;

        if ("terraria".equalsIgnoreCase(mode)) {
            // Terraria Mode: Finite volume liquid conservation
            if (below == BlockType.AIR.id || BlockType.isWashableDecoration(below)) {
                world.setBlock(x, y, z, BlockType.AIR.id, true);
                world.setBlock(x, y - 1, z, BlockType.WATER.id, true);
                world.triggerFluid(x, y - 1, z);
                world.triggerFluid(x, y + 1, z);
            } else if (!BlockType.isWater(below)) {
                long[][] sides = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
                int offset = (int) Math.abs((x * 31L ^ z * 17L ^ y) % 4);
                for (int i = 0; i < 4; i++) {
                    int idx = (offset + i) % 4;
                    long sx = x + sides[idx][0];
                    long sz = z + sides[idx][1];
                    byte sideBlock = world.getBlock(sx, y, sz);
                    if (sideBlock == BlockType.AIR.id || BlockType.isWashableDecoration(sideBlock)) {
                        world.setBlock(x, y, z, BlockType.AIR.id, true);
                        world.setBlock(sx, y, sz, BlockType.WATER.id, true);
                        world.triggerFluid(sx, y, sz);
                        world.triggerFluid(x, y, z);
                        break;
                    }
                }
            }
        } else if ("preclassic".equalsIgnoreCase(mode) || "classic".equalsIgnoreCase(mode)) {
            // Pre-classic Minecraft: infinite spreading of source blocks co-tick
            if (below == BlockType.AIR.id || BlockType.isWashableDecoration(below)) {
                world.setBlock(x, y - 1, z, BlockType.WATER.id, true);
                world.triggerFluid(x, y - 1, z);
            } else {
                long[][] sides = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
                for (long[] s : sides) {
                    long sx = x + s[0];
                    long sz = z + s[1];
                    byte sideBlock = world.getBlock(sx, y, sz);
                    if (sideBlock == BlockType.AIR.id || BlockType.isWashableDecoration(sideBlock)) {
                        world.setBlock(sx, y, sz, BlockType.WATER.id, true);
                        world.triggerFluid(sx, y, sz);
                    }
                }
            }
        } else {
            // Modern Minecraft Mode: Flow levels 1..7 and source block (level 8) co-tick
            int curLevel = BlockType.getWaterLevel(blockId);

            // A. Flowing water decay check: if upstream feed is gone, recede back to air
            if (curLevel < 8) {
                byte above = world.getBlock(x, y + 1, z);
                boolean fedFromAbove = BlockType.isWater(above);
                boolean fedFromSide = false;
                long[][] sides = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
                for (long[] s : sides) {
                    byte nId = world.getBlock(x + s[0], y, z + s[1]);
                    if (BlockType.isWater(nId) && BlockType.getWaterLevel(nId) > curLevel) {
                        fedFromSide = true;
                        break;
                    }
                }

                if (!fedFromAbove && !fedFromSide) {
                    world.setBlock(x, y, z, BlockType.AIR.id, true);
                    for (long[] s : sides) {
                        world.triggerFluid(x + s[0], y, z + s[1]);
                    }
                    world.triggerFluid(x, y - 1, z);
                    return;
                }
            }

            // B. Downward flow (falling water takes priority)
            if (below == BlockType.AIR.id || BlockType.isWashableDecoration(below)) {
                world.setBlock(x, y - 1, z, BlockType.WATER_FLOW_7.id, true);
                world.triggerFluid(x, y - 1, z);
            } else if (BlockType.isWater(below) && BlockType.getWaterLevel(below) < 7) {
                world.setBlock(x, y - 1, z, BlockType.WATER_FLOW_7.id, true);
                world.triggerFluid(x, y - 1, z);
            } else {
                // C. Horizontal spreading when supported by solid ground or full water underneath
                if (curLevel > 1) {
                    int nextLevel = (curLevel == 8) ? 7 : (curLevel - 1);
                    byte nextBlockId = BlockType.getFlowBlockId(nextLevel);
                    long[][] sides = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
                    for (long[] s : sides) {
                        long sx = x + s[0];
                        long sz = z + s[1];
                        byte nId = world.getBlock(sx, y, sz);
                        if (nId == BlockType.AIR.id || BlockType.isWashableDecoration(nId)) {
                            world.setBlock(sx, y, sz, nextBlockId, true);
                            world.triggerFluid(sx, y, sz);
                        } else if (BlockType.isWater(nId) && BlockType.getWaterLevel(nId) < nextLevel) {
                            world.setBlock(sx, y, sz, nextBlockId, true);
                            world.triggerFluid(sx, y, sz);
                        }
                    }
                }
            }
        }
    }
}
