package com.voxel.crafting;

import com.voxel.block.BlockType;

public final class CraftingManager {
    public record CraftResult(BlockType result, int count) {}

    private CraftingManager() {}

    public static CraftResult findMatchingRecipe(BlockType[][] grid) {
        if (grid == null || grid.length < 2 || grid[0].length < 2) return null;

        int nonAirCount = 0;
        BlockType singleBlock = null;
        for (int r = 0; r < 2; r++) {
            for (int c = 0; c < 2; c++) {
                BlockType b = grid[r][c];
                if (b != null && b != BlockType.AIR) {
                    nonAirCount++;
                    singleBlock = b;
                }
            }
        }

        if (nonAirCount == 0) return null;

        // 1. Single-item shapeless recipes (1 ingredient anywhere in 2x2 grid)
        if (nonAirCount == 1) {
            // Wood logs -> 4 Planks
            if (singleBlock == BlockType.WOOD || singleBlock == BlockType.BIRCH_WOOD
                    || singleBlock == BlockType.SPRUCE_WOOD || singleBlock == BlockType.JUNGLE_WOOD) {
                return new CraftResult(BlockType.PLANKS, 4);
            }
            // Clay block -> 2 Bricks
            if (singleBlock == BlockType.CLAY) {
                return new CraftResult(BlockType.BRICK, 2);
            }
            // Sandstone -> 4 Sand
            if (singleBlock == BlockType.SANDSTONE) {
                return new CraftResult(BlockType.SAND, 4);
            }
            // Wool / Sugar cane -> Tall grass fibers
            if (singleBlock == BlockType.SUGAR_CANE) {
                return new CraftResult(BlockType.TALL_GRASS, 2);
            }
        }

        // 2. 2-item shapeless recipes
        if (nonAirCount == 2) {
            boolean hasSand = false, hasDirt = false, hasCobble = false;
            for (int r = 0; r < 2; r++) {
                for (int c = 0; c < 2; c++) {
                    BlockType b = grid[r][c];
                    if (b == BlockType.SAND || b == BlockType.RED_SAND) hasSand = true;
                    if (b == BlockType.DIRT) hasDirt = true;
                    if (b == BlockType.COBBLESTONE) hasCobble = true;
                }
            }
            // Sand + Dirt -> Clay
            if (hasSand && hasDirt) return new CraftResult(BlockType.CLAY, 2);
            // Cobblestone + Sand -> Gravel
            if (hasCobble && hasSand) return new CraftResult(BlockType.GRAVEL, 2);
        }

        // 3. Full 2x2 shaped recipes
        if (nonAirCount == 4) {
            BlockType b00 = grid[0][0], b01 = grid[0][1], b10 = grid[1][0], b11 = grid[1][1];
            if (b00 != null && b00 == b01 && b00 == b10 && b00 == b11) {
                // 4 Planks -> 1 Log
                if (b00 == BlockType.PLANKS) return new CraftResult(BlockType.WOOD, 1);
                // 4 Cobblestone -> 4 Stone
                if (b00 == BlockType.COBBLESTONE) return new CraftResult(BlockType.STONE, 4);
                // 4 Stone -> 4 Brick
                if (b00 == BlockType.STONE) return new CraftResult(BlockType.BRICK, 4);
                // 4 Sand -> 4 Sandstone
                if (b00 == BlockType.SAND) return new CraftResult(BlockType.SANDSTONE, 4);
                // 4 Red Sand -> 4 Terracotta
                if (b00 == BlockType.RED_SAND) return new CraftResult(BlockType.TERRACOTTA, 4);
                // 4 Snow -> 4 Ice
                if (b00 == BlockType.SNOW) return new CraftResult(BlockType.ICE, 4);
            }
        }

        return null;
    }
}
