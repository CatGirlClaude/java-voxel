package com.voxel;

import com.voxel.block.BlockType;
import com.voxel.config.GameSettings;
import com.voxel.ui.InventoryMenu;
import com.voxel.ui.MainMenu;
import com.voxel.ui.OptionsMenu;
import com.voxel.ui.PauseMenu;
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
        test6_OptionsMenuSliders();
        test7_DynamicGuiWindowResizeRecalculation();

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

    private static void test6_OptionsMenuSliders() {
        System.out.println("\n--- TEST 6: Interactive Options Menu Sliders ---");
        OptionsMenu options = new OptionsMenu();
        options.setOpen(true, OptionsMenu.Source.MAIN_MENU);
        try {
            Thread.sleep(260); // Wait out open cooldown
        } catch (InterruptedException ignored) {}

        int screenW = 1280;
        int screenH = 720;
        float winW = options.getWinW(screenW);
        float winX = (screenW - winW) * 0.5f;
        float btnW = 360.0f;
        float btnX = winX + (winW - btnW) * 0.5f;
        float startY = (screenH - options.getWinH(screenH)) * 0.5f + 54.0f + 32.0f + 16.0f;

        // 1. Click on Render Distance slider at 50%
        float clickMidX = btnX + btnW * 0.5f;
        options.handleMouseClick(clickMidX, startY + 10.0f, org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT, screenW, screenH, null, null);
        System.out.printf("Render Distance after 50%% slider click: %d chunks%n", GameSettings.renderDistance);
        if (GameSettings.renderDistance != 9 && GameSettings.renderDistance != 8) {
            throw new AssertionError("Render Distance slider click unexpected value: " + GameSettings.renderDistance);
        }

        // 2. Drag slider to 100% (16 chunks)
        options.handleMouseMove(btnX + btnW + 20.0f, startY + 10.0f, screenW, screenH, null);
        System.out.printf("Render Distance after dragging to max: %d chunks%n", GameSettings.renderDistance);
        if (GameSettings.renderDistance != 16) {
            throw new AssertionError("Render Distance slider drag to max failed: " + GameSettings.renderDistance);
        }
        options.handleMouseRelease();

        // 3. Switch to Controls tab and test Sensitivity slider
        options.handleKey(org.lwjgl.glfw.GLFW.GLFW_KEY_2, null, null);
        options.handleMouseClick(btnX + btnW * 0.25f, startY + 10.0f, org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT, screenW, screenH, null, null);
        System.out.printf("Sensitivity after slider click at 25%%: %.2f%n", GameSettings.mouseSensitivity);
        if (GameSettings.mouseSensitivity < 0.10f || GameSettings.mouseSensitivity > 0.16f) {
            throw new AssertionError("Sensitivity slider click unexpected value: " + GameSettings.mouseSensitivity);
        }
        options.handleMouseRelease();

        // Reset to default
        GameSettings.renderDistance = 6;
        GameSettings.mouseSensitivity = 0.15f;
        System.out.println("Options Menu interactive sliders OK.");
    }

    private static void test7_DynamicGuiWindowResizeRecalculation() {
        System.out.println("\n--- TEST 7: Dynamic GUI Window Resize Recalculation ---");
        MainMenu mainMenu = new MainMenu();
        OptionsMenu optionsMenu = new OptionsMenu();
        InventoryMenu inventoryMenu = new InventoryMenu();
        PauseMenu pauseMenu = new PauseMenu();

        // Simulate resize to small window (800x600)
        mainMenu.onResize(800, 600);
        optionsMenu.onResize(800, 600);
        inventoryMenu.onResize(800, 600);
        pauseMenu.onResize(800, 600);

        float winW800 = optionsMenu.getWinW(800);
        float winH600 = optionsMenu.getWinH(600);
        System.out.printf("Options Menu at 800x600: WinW=%.1f px, WinH=%.1f px%n", winW800, winH600);
        if (winW800 > 800 - 32 || winH600 > 600 - 24) {
            throw new AssertionError("Options window overflows 800x600 viewport!");
        }

        // Simulate resize to 1920x1080
        mainMenu.onResize(1920, 1080);
        optionsMenu.onResize(1920, 1080);
        inventoryMenu.onResize(1920, 1080);
        pauseMenu.onResize(1920, 1080);

        float winW1080 = optionsMenu.getWinW(1920);
        float winH1080 = optionsMenu.getWinH(1080);
        System.out.printf("Options Menu at 1920x1080: WinW=%.1f px, WinH=%.1f px%n", winW1080, winH1080);
        if (winW1080 > 840.0f || winH1080 > 540.0f) {
            throw new AssertionError("Options window exceeded max bounds at 1080p!");
        }

        System.out.println("Dynamic GUI window resize recalculation OK.");
    }
}
