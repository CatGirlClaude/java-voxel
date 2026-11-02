package com.voxel.crafting;

import com.voxel.block.BlockType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class CraftingManager {
    public record CraftResult(BlockType result, int count) {}

    public record RecipeInfo(String name, BlockType result, int count, String description, BlockType[][] previewGrid) {}

    private static final List<RecipeInfo> ALL_RECIPES = new ArrayList<>();

    static {
        registerRecipes();
    }

    private CraftingManager() {}

    private static void registerRecipes() {
        // 1. Logs -> Planks
        ALL_RECIPES.add(new RecipeInfo("Planks", BlockType.PLANKS, 4, "Wood Log -> 4 Oak Planks",
                new BlockType[][]{{BlockType.WOOD, BlockType.AIR}, {BlockType.AIR, BlockType.AIR}}));

        // 2. 4 Planks -> Log
        ALL_RECIPES.add(new RecipeInfo("Wood Log", BlockType.WOOD, 1, "4 Planks -> 1 Wood Log",
                new BlockType[][]{{BlockType.PLANKS, BlockType.PLANKS}, {BlockType.PLANKS, BlockType.PLANKS}}));

        // 3. Cobble + Sand -> Gravel
        ALL_RECIPES.add(new RecipeInfo("Gravel", BlockType.GRAVEL, 2, "Cobblestone + Sand -> 2 Gravel",
                new BlockType[][]{{BlockType.COBBLESTONE, BlockType.SAND}, {BlockType.AIR, BlockType.AIR}}));

        // 4. Sand + Dirt -> Clay
        ALL_RECIPES.add(new RecipeInfo("Clay", BlockType.CLAY, 2, "Sand + Dirt -> 2 Clay",
                new BlockType[][]{{BlockType.SAND, BlockType.DIRT}, {BlockType.AIR, BlockType.AIR}}));

        // 5. Clay -> Brick
        ALL_RECIPES.add(new RecipeInfo("Brick", BlockType.BRICK, 2, "Clay -> 2 Bricks",
                new BlockType[][]{{BlockType.CLAY, BlockType.AIR}, {BlockType.AIR, BlockType.AIR}}));

        // 6. 4 Cobble -> 4 Stone
        ALL_RECIPES.add(new RecipeInfo("Stone", BlockType.STONE, 4, "4 Cobblestone -> 4 Stone",
                new BlockType[][]{{BlockType.COBBLESTONE, BlockType.COBBLESTONE}, {BlockType.COBBLESTONE, BlockType.COBBLESTONE}}));

        // 7. 4 Stone -> 4 Brick
        ALL_RECIPES.add(new RecipeInfo("Stone Brick", BlockType.BRICK, 4, "4 Stone -> 4 Bricks",
                new BlockType[][]{{BlockType.STONE, BlockType.STONE}, {BlockType.STONE, BlockType.STONE}}));

        // 8. 4 Sand -> 4 Sandstone
        ALL_RECIPES.add(new RecipeInfo("Sandstone", BlockType.SANDSTONE, 4, "4 Sand -> 4 Sandstone",
                new BlockType[][]{{BlockType.SAND, BlockType.SAND}, {BlockType.SAND, BlockType.SAND}}));

        // 9. Sandstone -> 4 Sand
        ALL_RECIPES.add(new RecipeInfo("Sand", BlockType.SAND, 4, "Sandstone -> 4 Sand",
                new BlockType[][]{{BlockType.SANDSTONE, BlockType.AIR}, {BlockType.AIR, BlockType.AIR}}));

        // 10. 4 Red Sand -> 4 Terracotta
        ALL_RECIPES.add(new RecipeInfo("Terracotta", BlockType.TERRACOTTA, 4, "4 Red Sand -> 4 Terracotta",
                new BlockType[][]{{BlockType.RED_SAND, BlockType.RED_SAND}, {BlockType.RED_SAND, BlockType.RED_SAND}}));

        // 11. 4 Snow -> 4 Ice
        ALL_RECIPES.add(new RecipeInfo("Ice", BlockType.ICE, 4, "4 Snow -> 4 Ice",
                new BlockType[][]{{BlockType.SNOW, BlockType.SNOW}, {BlockType.SNOW, BlockType.SNOW}}));

        // 12. Sugar Cane -> 2 Fibers
        ALL_RECIPES.add(new RecipeInfo("Grass Fibers", BlockType.TALL_GRASS, 2, "Sugar Cane -> 2 Tall Grass",
                new BlockType[][]{{BlockType.SUGAR_CANE, BlockType.AIR}, {BlockType.AIR, BlockType.AIR}}));

        // 13. 3x3 Hollow Ring: 8 Cobblestone -> 8 Bricks
        ALL_RECIPES.add(new RecipeInfo("Masonry Kiln", BlockType.BRICK, 8, "8 Cobblestone (hollow ring) -> 8 Bricks",
                new BlockType[][]{
                        {BlockType.COBBLESTONE, BlockType.COBBLESTONE, BlockType.COBBLESTONE},
                        {BlockType.COBBLESTONE, BlockType.AIR, BlockType.COBBLESTONE},
                        {BlockType.COBBLESTONE, BlockType.COBBLESTONE, BlockType.COBBLESTONE}
                }));

        // 14. 3x3 Hollow Ring: 8 Planks -> 8 Logs
        ALL_RECIPES.add(new RecipeInfo("Timber Bundle", BlockType.WOOD, 8, "8 Planks (hollow ring) -> 8 Logs",
                new BlockType[][]{
                        {BlockType.PLANKS, BlockType.PLANKS, BlockType.PLANKS},
                        {BlockType.PLANKS, BlockType.AIR, BlockType.PLANKS},
                        {BlockType.PLANKS, BlockType.PLANKS, BlockType.PLANKS}
                }));

        // 15. 3x3 Full Grid: 9 Cobblestone -> 9 Stone
        ALL_RECIPES.add(new RecipeInfo("Bulk Stone", BlockType.STONE, 9, "9 Cobblestone -> 9 Smooth Stone",
                new BlockType[][]{
                        {BlockType.COBBLESTONE, BlockType.COBBLESTONE, BlockType.COBBLESTONE},
                        {BlockType.COBBLESTONE, BlockType.COBBLESTONE, BlockType.COBBLESTONE},
                        {BlockType.COBBLESTONE, BlockType.COBBLESTONE, BlockType.COBBLESTONE}
                }));
    }

    public static List<RecipeInfo> getAllRecipes() {
        return Collections.unmodifiableList(ALL_RECIPES);
    }

    public static CraftResult findMatchingRecipe(BlockType[][] grid) {
        if (grid == null || grid.length < 2 || grid[0].length < 2) return null;

        int rows = grid.length;
        int cols = grid[0].length;

        int nonAirCount = 0;
        BlockType singleBlock = null;

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                BlockType b = grid[r][c];
                if (b != null && b != BlockType.AIR) {
                    nonAirCount++;
                    singleBlock = b;
                }
            }
        }

        if (nonAirCount == 0) return null;

        // 1. Single-item shapeless recipes (1 ingredient anywhere in grid)
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
            // Sugar cane -> Tall grass fibers
            if (singleBlock == BlockType.SUGAR_CANE) {
                return new CraftResult(BlockType.TALL_GRASS, 2);
            }
            // Cactus -> Vine
            if (singleBlock == BlockType.CACTUS) {
                return new CraftResult(BlockType.VINE, 2);
            }
            // Coral -> 2 Roses
            if (singleBlock == BlockType.CORAL) {
                return new CraftResult(BlockType.ROSE, 2);
            }
        }

        // 2. 2-item shapeless recipes
        if (nonAirCount == 2) {
            boolean hasSand = false, hasDirt = false, hasCobble = false, hasSnow = false;
            for (int r = 0; r < rows; r++) {
                for (int c = 0; c < cols; c++) {
                    BlockType b = grid[r][c];
                    if (b == BlockType.SAND || b == BlockType.RED_SAND) hasSand = true;
                    if (b == BlockType.DIRT) hasDirt = true;
                    if (b == BlockType.COBBLESTONE) hasCobble = true;
                    if (b == BlockType.SNOW) hasSnow = true;
                }
            }
            // Sand + Dirt -> Clay
            if (hasSand && hasDirt) return new CraftResult(BlockType.CLAY, 2);
            // Cobblestone + Sand -> Gravel
            if (hasCobble && hasSand) return new CraftResult(BlockType.GRAVEL, 2);
            // Snow + Dirt -> Snowy Grass
            if (hasSnow && hasDirt) return new CraftResult(BlockType.SNOWY_GRASS, 2);
        }

        // 3. 2x2 shaped recipes (check exact 2x2 or subgrid in 3x3)
        if (nonAirCount == 4) {
            CraftResult res2x2 = check2x2Shaped(grid, rows, cols);
            if (res2x2 != null) return res2x2;
        }

        // 4. 3x3 shaped recipes
        if (rows >= 3 && cols >= 3) {
            // 8-item hollow ring (center is AIR, border is all non-air)
            if (nonAirCount == 8 && (grid[1][1] == null || grid[1][1] == BlockType.AIR)) {
                BlockType bRing = grid[0][0];
                boolean isUniformRing = true;
                int[][] ringCoords = {
                        {0, 0}, {0, 1}, {0, 2},
                        {1, 0},         {1, 2},
                        {2, 0}, {2, 1}, {2, 2}
                };
                for (int[] rc : ringCoords) {
                    if (grid[rc[0]][rc[1]] != bRing) {
                        isUniformRing = false;
                        break;
                    }
                }
                if (isUniformRing) {
                    if (bRing == BlockType.COBBLESTONE) return new CraftResult(BlockType.BRICK, 8);
                    if (bRing == BlockType.PLANKS) return new CraftResult(BlockType.WOOD, 8);
                    if (bRing == BlockType.SAND) return new CraftResult(BlockType.SANDSTONE, 8);
                }
            }

            // 9-item full 3x3 fill
            if (nonAirCount == 9) {
                BlockType bFill = grid[0][0];
                boolean isUniform = true;
                for (int r = 0; r < 3; r++) {
                    for (int c = 0; c < 3; c++) {
                        if (grid[r][c] != bFill) {
                            isUniform = false;
                            break;
                        }
                    }
                }
                if (isUniform) {
                    if (bFill == BlockType.COBBLESTONE) return new CraftResult(BlockType.STONE, 9);
                    if (bFill == BlockType.SAND) return new CraftResult(BlockType.SANDSTONE, 9);
                    if (bFill == BlockType.PLANKS) return new CraftResult(BlockType.WOOD, 9);
                    if (bFill == BlockType.DIRT) return new CraftResult(BlockType.CLAY, 9);
                }
            }
        }

        return null;
    }

    private static CraftResult check2x2Shaped(BlockType[][] grid, int rows, int cols) {
        // If 2x2 grid exactly:
        if (rows == 2 && cols == 2) {
            return match2x2Block(grid[0][0], grid[0][1], grid[1][0], grid[1][1]);
        }

        // If 3x3 grid, test all 4 possible 2x2 positions
        for (int sr = 0; sr <= rows - 2; sr++) {
            for (int sc = 0; sc <= cols - 2; sc++) {
                BlockType b00 = grid[sr][sc];
                BlockType b01 = grid[sr][sc + 1];
                BlockType b10 = grid[sr + 1][sc];
                BlockType b11 = grid[sr + 1][sc + 1];

                if (b00 != null && b00 != BlockType.AIR &&
                    b01 != null && b01 != BlockType.AIR &&
                    b10 != null && b10 != BlockType.AIR &&
                    b11 != null && b11 != BlockType.AIR) {
                    // Check if all OTHER cells in the grid are AIR
                    boolean othersAir = true;
                    for (int r = 0; r < rows; r++) {
                        for (int c = 0; c < cols; c++) {
                            if ((r < sr || r > sr + 1 || c < sc || c > sc + 1) &&
                                grid[r][c] != null && grid[r][c] != BlockType.AIR) {
                                othersAir = false;
                                break;
                            }
                        }
                    }
                    if (othersAir) {
                        CraftResult res = match2x2Block(b00, b01, b10, b11);
                        if (res != null) return res;
                    }
                }
            }
        }

        return null;
    }

    private static CraftResult match2x2Block(BlockType b00, BlockType b01, BlockType b10, BlockType b11) {
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
            // 4 Glass -> 4 Glowstone
            if (b00 == BlockType.GLASS) return new CraftResult(BlockType.GLOWSTONE, 4);
        }
        return null;
    }
}
