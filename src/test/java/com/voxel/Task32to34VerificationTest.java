package com.voxel;

import com.voxel.block.BlockState;
import com.voxel.block.BlockType;
import com.voxel.block.Blocks;
import com.voxel.block.FluidBlock;
import com.voxel.registry.Registries;
import com.voxel.world.World;
import org.joml.Vector3d;
import org.joml.Vector3f;

public class Task32to34VerificationTest {

    public static void main(String[] args) {
        System.out.println("Running Task 32, 33, 34 verification...");
        testBlockStateFluidProperties();
        testNoSeparateBlockIdsForWaterFlow();
        testRaycastIgnoresWaterHighlight();
        testModernWaterSpreadsCoTick();
        System.out.println(">>> ALL TASK 32-34 TESTS PASSED! <<<");
    }

    private static void testBlockStateFluidProperties() {
        BlockState defaultState = Blocks.WATER.getDefaultState();
        if (defaultState.get(FluidBlock.LEVEL) != 8) {
            throw new AssertionError("Default water state must be level 8 (source), was: " + defaultState.get(FluidBlock.LEVEL));
        }

        BlockState level3 = Blocks.WATER.getStateForLevel(3);
        if (level3.get(FluidBlock.LEVEL) != 3) {
            throw new AssertionError("State for level 3 must have LEVEL=3, was: " + level3.get(FluidBlock.LEVEL));
        }

        // Raw ID mapping verification
        byte rawId8 = Blocks.getRawIdForState(defaultState);
        if (rawId8 != 8) {
            throw new AssertionError("Expected raw ID 8 for level 8, got: " + rawId8);
        }

        BlockState fromId = Blocks.getStateFromRawId((byte) 37);
        if (fromId.getBlock() != Blocks.WATER || fromId.get(FluidBlock.LEVEL) != 5) {
            throw new AssertionError("Expected WATER level 5 for raw ID 37, got: " + fromId);
        }
        System.out.println("  [PASS] BlockState properties for fluid levels verified.");
    }

    private static void testNoSeparateBlockIdsForWaterFlow() {
        // Registry should only have "water" and NOT "water_flow_7", etc.
        if (Registries.BLOCK.containsId(com.voxel.registry.Identifier.of("water_flow_7"))) {
            throw new AssertionError("water_flow_7 should not be a separate registered block in Registries.BLOCK!");
        }
        BlockType[] creativeBlocks = BlockType.getCreativeBlocks();
        for (BlockType bt : creativeBlocks) {
            if (bt.name().startsWith("WATER_FLOW")) {
                throw new AssertionError("Creative blocks contains separate flow entry: " + bt);
            }
        }
        System.out.println("  [PASS] Consolidated single WATER block verified (no separate flow IDs).");
    }

    private static void testRaycastIgnoresWaterHighlight() {
        World world = new World(9999);
        // Place stone at y=5, water at y=6, flowing water at y=7
        world.setBlock(10, 5, 10, BlockType.STONE.id, false);
        world.setBlock(10, 6, 10, BlockType.WATER.id, false);
        world.setBlock(10, 7, 10, BlockType.getFlowBlockId(4), false);

        // Raycast straight down from y=9
        Vector3d origin = new Vector3d(10.5, 9.0, 10.5);
        Vector3f dir = new Vector3f(0.0f, -1.0f, 0.0f);
        World.RaycastHit hit = world.raycast(origin, dir, 10.0f);

        if (hit == null) {
            throw new AssertionError("Raycast hit was null!");
        }
        if (BlockType.isWater(hit.blockId())) {
            throw new AssertionError("Raycast must ignore water and flowing water so it has NO block highlight! Hit: " + hit.blockId());
        }
        if (hit.blockId() != BlockType.STONE.id) {
            throw new AssertionError("Raycast should hit stone through water, hit: " + hit.blockId());
        }
        System.out.println("  [PASS] Water in other states (and source) has NO block highlight.");
    }

    private static void testModernWaterSpreadsCoTick() {
        com.voxel.config.GameSettings.waterPhysicsMode = "modern";
        com.voxel.config.GameSettings.blockPhysicsEnabled = true;
        World world = new World(8888);

        // Solid floor at y=99, air at y=100
        for (int x = -5; x <= 5; x++) {
            for (int z = -5; z <= 5; z++) {
                world.setBlock(x, 99, z, BlockType.STONE.id, false);
                world.setBlock(x, 100, z, BlockType.AIR.id, false);
            }
        }

        // Place water source at (0, 100, 0)
        world.setBlock(0, 100, 0, BlockType.WATER.id, true);

        // Before ticking fluid, neighbor (1, 100, 0) should NOT immediately have water!
        byte immediately = world.getBlock(1, 100, 0);
        if (BlockType.isWater(immediately)) {
            throw new AssertionError("Water spread immediately before any tick occurred!");
        }

        // Tick fluid once (0.15s dt)
        world.update(new Vector3d(0, 100, 0), 0.15f);

        byte afterOneTick = world.getBlock(1, 100, 0);
        if (!BlockType.isWater(afterOneTick)) {
            throw new AssertionError("Water did not spread after 1 fluid tick! Got: " + afterOneTick);
        }

        // But 2 blocks away (2, 100, 0) should NOT be filled yet after only 1 tick!
        byte twoBlocksAway = world.getBlock(2, 100, 0);
        if (BlockType.isWater(twoBlocksAway)) {
            throw new AssertionError("Water spread 2 blocks in 1 tick! It must spread step-by-step co-tick.");
        }

        // Tick fluid second time
        world.update(new Vector3d(0, 100, 0), 0.15f);
        byte afterTwoTicks = world.getBlock(2, 100, 0);
        if (!BlockType.isWater(afterTwoTicks)) {
            throw new AssertionError("Water should spread to distance 2 on second tick! Got: " + afterTwoTicks);
        }

        System.out.println("  [PASS] Modern water spreads step-by-step co-tick, not instantly.");
    }
}
