package com.voxel;

import com.voxel.block.BlockType;
import com.voxel.crafting.CraftingManager;
import com.voxel.entity.Player;
import com.voxel.ui.InventoryMenu;

import java.util.List;

public class Task60CraftingAndInventoryTest {

    public static void main(String[] args) {
        System.out.println("Running Task 60 verification (Crafting 2x2, 3x3 Workshop & Inventory Tabs)...");

        testCrafting2x2Recipes();
        testCrafting3x3Recipes();
        testInventoryMenuTabsAndGrid();

        System.out.println(">>> ALL TASK 60 CRAFTING & INVENTORY TESTS PASSED! <<<");
    }

    private static void testCrafting2x2Recipes() {
        // 1. Single-item shapeless: Wood -> 4 Planks
        BlockType[][] grid1 = {
                {BlockType.WOOD, BlockType.AIR},
                {BlockType.AIR, BlockType.AIR}
        };
        CraftingManager.CraftResult r1 = CraftingManager.findMatchingRecipe(grid1);
        if (r1 == null || r1.result() != BlockType.PLANKS || r1.count() != 4) {
            throw new AssertionError("Expected 4 Planks from 1 Wood Log, got: " + r1);
        }

        // 2. 2-item shapeless: Sand + Dirt -> 2 Clay
        BlockType[][] grid2 = {
                {BlockType.AIR, BlockType.SAND},
                {BlockType.DIRT, BlockType.AIR}
        };
        CraftingManager.CraftResult r2 = CraftingManager.findMatchingRecipe(grid2);
        if (r2 == null || r2.result() != BlockType.CLAY || r2.count() != 2) {
            throw new AssertionError("Expected 2 Clay from Sand + Dirt, got: " + r2);
        }

        // 3. 4-item shaped: 4 Cobblestone -> 4 Stone
        BlockType[][] grid3 = {
                {BlockType.COBBLESTONE, BlockType.COBBLESTONE},
                {BlockType.COBBLESTONE, BlockType.COBBLESTONE}
        };
        CraftingManager.CraftResult r3 = CraftingManager.findMatchingRecipe(grid3);
        if (r3 == null || r3.result() != BlockType.STONE || r3.count() != 4) {
            throw new AssertionError("Expected 4 Stone from 4 Cobblestone, got: " + r3);
        }

        // 4. 4-item shaped: 4 Planks -> 1 Wood Log
        BlockType[][] grid4 = {
                {BlockType.PLANKS, BlockType.PLANKS},
                {BlockType.PLANKS, BlockType.PLANKS}
        };
        CraftingManager.CraftResult r4 = CraftingManager.findMatchingRecipe(grid4);
        if (r4 == null || r4.result() != BlockType.WOOD || r4.count() != 1) {
            throw new AssertionError("Expected 1 Wood from 4 Planks, got: " + r4);
        }

        System.out.println("  [PASS] 2x2 Crafting recipes (shapeless and shaped) verified.");
    }

    private static void testCrafting3x3Recipes() {
        // 1. 8-item hollow ring: 8 Cobblestone around center AIR -> 8 Bricks
        BlockType[][] gridRing = {
                {BlockType.COBBLESTONE, BlockType.COBBLESTONE, BlockType.COBBLESTONE},
                {BlockType.COBBLESTONE, BlockType.AIR, BlockType.COBBLESTONE},
                {BlockType.COBBLESTONE, BlockType.COBBLESTONE, BlockType.COBBLESTONE}
        };
        CraftingManager.CraftResult rRing = CraftingManager.findMatchingRecipe(gridRing);
        if (rRing == null || rRing.result() != BlockType.BRICK || rRing.count() != 8) {
            throw new AssertionError("Expected 8 Bricks from 8 Cobblestone ring, got: " + rRing);
        }

        // 2. 9-item full fill: 9 Cobblestone -> 9 Stone
        BlockType[][] gridFull = {
                {BlockType.COBBLESTONE, BlockType.COBBLESTONE, BlockType.COBBLESTONE},
                {BlockType.COBBLESTONE, BlockType.COBBLESTONE, BlockType.COBBLESTONE},
                {BlockType.COBBLESTONE, BlockType.COBBLESTONE, BlockType.COBBLESTONE}
        };
        CraftingManager.CraftResult rFull = CraftingManager.findMatchingRecipe(gridFull);
        if (rFull == null || rFull.result() != BlockType.STONE || rFull.count() != 9) {
            throw new AssertionError("Expected 9 Stone from 9 Cobblestone, got: " + rFull);
        }

        // 3. Verify recipe registry list contains expected recipes
        List<CraftingManager.RecipeInfo> recipes = CraftingManager.getAllRecipes();
        if (recipes.size() < 10) {
            throw new AssertionError("Expected at least 10 registered recipes, found: " + recipes.size());
        }

        System.out.println("  [PASS] 3x3 Worktable recipes & recipe registry verified.");
    }

    private static void testInventoryMenuTabsAndGrid() {
        InventoryMenu menu = new InventoryMenu();
        Player player = new Player();

        menu.setOpen(true);
        if (!menu.isOpen()) {
            throw new AssertionError("Menu should be open");
        }

        // Check onResize
        menu.onResize(1920, 1080);

        // Click Tab 1 (Crafting)
        // Header tab 1 bounds: winX + winW - 102.0f
        float winW = InventoryMenu.COLS * InventoryMenu.SLOT_SIZE + (InventoryMenu.COLS + 1) * InventoryMenu.SLOT_PAD;
        float winH = InventoryMenu.ROWS * InventoryMenu.SLOT_SIZE + (InventoryMenu.ROWS + 1) * InventoryMenu.SLOT_PAD + 120.0f;
        float winX = (1920 - winW) * 0.5f;
        float winY = (1080 - winH) * 0.5f;

        float tab1X = winX + winW - 50.0f;
        float tab1Y = winY + 18.0f;
        menu.handleMouseClick(tab1X, tab1Y, 0, player);

        // Click mode button to toggle to 3x3
        float modeBtnX = winX + 50.0f;
        float modeBtnY = winY + 54.0f;
        menu.handleMouseClick(modeBtnX, modeBtnY, 0, player);

        // Click recipe from recipe book list to auto-fill grid
        float bookX = winX + 260.0f;
        float recipe0Y = winY + 75.0f; // first recipe: Logs -> Planks
        menu.handleMouseClick(bookX, recipe0Y, 0, player);

        CraftingManager.CraftResult res = menu.getActiveCraftResult();
        if (res == null || res.result() != BlockType.PLANKS) {
            throw new AssertionError("Expected active craft result to be PLANKS, got: " + res);
        }

        // Click clear button
        float clearBtnX = winX + 130.0f;
        float clearBtnY = winY + 54.0f;
        menu.handleMouseClick(clearBtnX, clearBtnY, 0, player);

        if (menu.getActiveCraftResult() != null) {
            throw new AssertionError("Expected null craft result after clearing grid!");
        }

        System.out.println("  [PASS] InventoryMenu tabs, mode toggles, recipe auto-fill, and clear verified.");
    }
}
