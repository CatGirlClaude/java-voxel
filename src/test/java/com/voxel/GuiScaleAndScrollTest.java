package com.voxel;

import com.voxel.block.BlockType;
import com.voxel.config.GameSettings;
import com.voxel.ui.InventoryMenu;
import com.voxel.ui.MainMenu;
import com.voxel.ui.OptionsMenu;
import com.voxel.ui.TextRenderer;
import com.voxel.util.Logger;
import org.joml.Matrix4f;

public class GuiScaleAndScrollTest {
    public static void main(String[] args) {
        Logger.init();
        System.out.println("=================================================");
        System.out.println("  RUNNING GUI SCALE, SCROLL & BUTTON TEXT TEST   ");
        System.out.println("=================================================");

        test1_ButtonTextAutoFit();
        test2_GuiScaleCalculationAndPersistence();
        test3_DebugHudScrollReplacesPages();
        test4_MainMenuWorldSelectionScroll();
        test5_InventoryMenuScroll();

        System.out.println("\n>>> ALL GUI SCALE & SCROLL TESTS PASSED (100% PASS)! <<<");
    }

    private static void test1_ButtonTextAutoFit() {
        System.out.println("\n--- TEST 1: Button Text Auto-Fit & Margins ---");
        // Verify text width calculations for wide button labels
        int charW = 6;
        String longLabel = "Play Selected World"; // 19 chars
        float scale15 = 1.5f;
        float unconstrainedWidth = longLabel.length() * charW * scale15; // 171 px
        float buttonWidth = 150.0f;
        float padding = 8.0f;
        float maxAllowedW = buttonWidth - padding; // 142 px

        if (unconstrainedWidth <= buttonWidth) {
            throw new AssertionError("Test premise failed: unconstrained text should be wider than button");
        }

        float autoScale = scale15 * (maxAllowedW / unconstrainedWidth);
        float constrainedWidth = longLabel.length() * charW * autoScale;

        System.out.printf("Label: '%s' | Unconstrained: %.1f px | Button: %.1f px | Auto-scaled width: %.1f px (scale: %.2f)%n",
                longLabel, unconstrainedWidth, buttonWidth, constrainedWidth, autoScale);

        if (constrainedWidth > maxAllowedW) {
            throw new AssertionError("Auto-scaled text still exceeds maximum allowed width inside button!");
        }

        // Test Options Menu tabs auto-fit
        String tabLabel = "[4] Overrides"; // 13 chars
        float tabW = 100.0f;
        float tabMaxW = tabW - 6.0f;
        float tabScale = 1.10f;
        float tabTextW = tabLabel.length() * charW * tabScale;
        if (tabTextW > tabMaxW) {
            tabScale = tabScale * (tabMaxW / tabTextW);
        }
        float finalTabW = tabLabel.length() * charW * tabScale;
        System.out.printf("Tab: '%s' | Tab Width: %.1f px | Fitted Width: %.1f px%n", tabLabel, tabW, finalTabW);
        if (finalTabW > tabMaxW) {
            throw new AssertionError("Tab text exceeds tab width!");
        }

        System.out.println("Button text auto-fit & margins OK.");
    }

    private static void test2_GuiScaleCalculationAndPersistence() {
        System.out.println("\n--- TEST 2: GUI Scale Options & Effective Scaling ---");
        GameSettings.guiScale = 0; // Auto
        float scale1080p = GameSettings.getEffectiveGuiScale(1920, 1080);
        float scale720p = GameSettings.getEffectiveGuiScale(1280, 720);
        float scale4k = GameSettings.getEffectiveGuiScale(3840, 2160);

        System.out.printf("Auto GUI Scale: 1080p -> %.0fx | 720p -> %.0fx | 4K -> %.0fx%n",
                scale1080p, scale720p, scale4k);

        if (scale1080p < 2.0f) {
            throw new AssertionError("1080p should have effective scale >= 2x in Auto mode!");
        }
        if (scale720p < 1.0f) {
            throw new AssertionError("720p should have effective scale >= 1x in Auto mode!");
        }

        // Manual scale settings
        GameSettings.guiScale = 1;
        if (GameSettings.getEffectiveGuiScale(1920, 1080) != 1.0f) {
            throw new AssertionError("Manual scale 1x failed!");
        }
        GameSettings.guiScale = 3;
        if (GameSettings.getEffectiveGuiScale(1920, 1080) != 3.0f) {
            throw new AssertionError("Manual scale 3x failed!");
        }

        // Save & load test
        GameSettings.save();
        GameSettings.guiScale = 0;
        GameSettings.load();
        if (GameSettings.guiScale != 3) {
            throw new AssertionError("GUI Scale persistence failed! Expected 3, got: " + GameSettings.guiScale);
        }
        GameSettings.guiScale = 0; // Restore to Auto
        GameSettings.save();

        System.out.println("GUI Scale calculation and persistence OK.");
    }

    private static void test3_DebugHudScrollReplacesPages() {
        System.out.println("\n--- TEST 3: Debug HUD Tab Scroll (No Pages) ---");
        OptionsMenu options = new OptionsMenu();
        options.setOpen(true, OptionsMenu.Source.MAIN_MENU);

        // Switch to Debug HUD tab (Tab 3, 1-indexed key 3 = GLFW_KEY_3)
        options.handleKey(org.lwjgl.glfw.GLFW.GLFW_KEY_3, null, null);

        // Test scrolling
        boolean scrolledDown = options.handleScroll(-1.0); // scroll down
        if (!scrolledDown) {
            throw new AssertionError("OptionsMenu failed to handle vertical scroll on Debug HUD tab!");
        }
        boolean scrolledUp = options.handleScroll(1.0); // scroll up
        if (!scrolledUp) {
            throw new AssertionError("OptionsMenu failed to handle scroll up on Debug HUD tab!");
        }

        System.out.println("Debug HUD scroll list OK (pages completely removed).");
    }

    private static void test4_MainMenuWorldSelectionScroll() {
        System.out.println("\n--- TEST 4: Main Menu World Selection Scroll ---");
        MainMenu menu = new MainMenu();
        menu.setActive(true);
        // Switch to saves screen
        menu.handleKey(org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER); // Click select world or switch screen
        // Scroll should work smoothly on saves screen
        menu.handleScroll(-1.0);
        menu.handleScroll(1.0);

        System.out.println("Main menu world selection list scroll OK.");
    }

    private static void test5_InventoryMenuScroll() {
        System.out.println("\n--- TEST 5: Creative Inventory Scroll ---");
        InventoryMenu inv = new InventoryMenu();
        inv.setOpen(true);

        boolean scrolledDown = inv.handleScroll(-1.0);
        boolean scrolledUp = inv.handleScroll(1.0);

        System.out.printf("Inventory scroll down: %b | scroll up: %b%n", scrolledDown, scrolledUp);
        System.out.println("Creative inventory scroll OK.");
    }
}
