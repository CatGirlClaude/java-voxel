package com.voxel.block;

import com.voxel.block.state.IntegerProperty;
import com.voxel.registry.Identifier;

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
}
