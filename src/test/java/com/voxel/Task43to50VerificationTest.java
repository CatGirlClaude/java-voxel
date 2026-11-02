package com.voxel;

import com.voxel.config.GameSettings;
import com.voxel.ui.OptionsMenu;
import com.voxel.ui.PauseMenu;
import org.lwjgl.glfw.GLFW;

public class Task43to50VerificationTest {

    public static void main(String[] args) {
        System.out.println("Running Task 43, 44, 48, 50 verification...");
        testPauseMenuCleanLayout();
        testDefaultHudScaleIsOne();
        testGuiScaleAntiSoftlockClamping();
        testInteractiveKeybindRemapping();
        System.out.println(">>> ALL TASK 43, 44, 48, 50 TESTS PASSED! <<<");
    }

    private static void testPauseMenuCleanLayout() {
        PauseMenu pauseMenu = new PauseMenu();
        pauseMenu.setOpen(true);
        pauseMenu.resetCooldown();

        // Click top button (resume) -> Action.RESUME
        // Centered window at 800x600:
        // winW = 380, btnW = 340, btnH = 38, btnSpacing = 10
        // totalBtnsH = 4 * 38 + 3 * 10 = 182; winH = 182 + 110 = 292
        // winX = (800 - 380) / 2 = 210; winY = (600 - 292) / 2 = 154
        // startY = 154 + 68 = 222
        // Button 0 (Resume) Y: 222..260
        // Button 1 (Options) Y: 270..308
        // Button 2 (Save) Y: 318..356
        // Button 3 (Save & Quit) Y: 366..404
        float clickX = 210.0f + 50.0f;

        PauseMenu.Action a0 = pauseMenu.handleMouseClick(clickX, 230.0f, GLFW.GLFW_MOUSE_BUTTON_LEFT, 800, 600);
        if (a0 != PauseMenu.Action.RESUME) {
            throw new AssertionError("First pause menu button should be RESUME, got " + a0);
        }

        PauseMenu.Action a1 = pauseMenu.handleMouseClick(clickX, 280.0f, GLFW.GLFW_MOUSE_BUTTON_LEFT, 800, 600);
        if (a1 != PauseMenu.Action.OPEN_OPTIONS) {
            throw new AssertionError("Second pause menu button should be OPEN_OPTIONS, got " + a1);
        }

        PauseMenu.Action a2 = pauseMenu.handleMouseClick(clickX, 330.0f, GLFW.GLFW_MOUSE_BUTTON_LEFT, 800, 600);
        if (a2 != PauseMenu.Action.SAVE) {
            throw new AssertionError("Third pause menu button should be SAVE, got " + a2);
        }

        PauseMenu.Action a3 = pauseMenu.handleMouseClick(clickX, 380.0f, GLFW.GLFW_MOUSE_BUTTON_LEFT, 800, 600);
        if (a3 != PauseMenu.Action.MAIN_MENU) {
            throw new AssertionError("Fourth pause menu button should be MAIN_MENU, got " + a3);
        }

        // Below 4th button: should return NONE
        PauseMenu.Action a4 = pauseMenu.handleMouseClick(clickX, 430.0f, GLFW.GLFW_MOUSE_BUTTON_LEFT, 800, 600);
        if (a4 != PauseMenu.Action.NONE) {
            throw new AssertionError("There should only be 4 buttons in pause menu, got action: " + a4);
        }
        System.out.println("  [PASS] Clean 4-button pause menu layout verified.");
    }

    private static void testDefaultHudScaleIsOne() {
        GameSettings.resetToDefaults();
        if (GameSettings.guiScale != 1) {
            throw new AssertionError("Default guiScale should be 1, but got: " + GameSettings.guiScale);
        }
        System.out.println("  [PASS] Default HUD scale is 1 verified.");
    }

    private static void testGuiScaleAntiSoftlockClamping() {
        // High GUI scale (4x) on modest window (800x600):
        // Without clamp, 800 / 4 = 200, 600 / 4 = 150 (softlock: menus do not fit)
        // With anti-softlock clamp: Math.min(800 / 420, 600 / 260) = Math.min(1, 2) = 1
        GameSettings.guiScale = 4;
        float effScale = GameSettings.getEffectiveGuiScale(800, 600);
        if (effScale > 2.0f) {
            throw new AssertionError("Effective scale should be capped to prevent softlock, got: " + effScale);
        }

        float vW = 800.0f / effScale;
        float vH = 600.0f / effScale;
        if (vW < 400.0f || vH < 260.0f) {
            throw new AssertionError("Virtual dimensions too small for menus: " + vW + "x" + vH);
        }

        GameSettings.resetToDefaults();
        System.out.println("  [PASS] Anti-softlock GUI scale clamping verified.");
    }

    private static void testInteractiveKeybindRemapping() {
        GameSettings.resetToDefaults();
        OptionsMenu menu = new OptionsMenu();
        menu.setOpen(true, OptionsMenu.Source.MAIN_MENU);

        // Test remapping forward key
        menu.setRebindingAction(OptionsMenu.KeyAction.FORWARD);
        if (menu.getRebindingAction() != OptionsMenu.KeyAction.FORWARD) {
            throw new AssertionError("Rebinding action should be FORWARD");
        }

        // Press UP arrow key to bind
        menu.handleKey(GLFW.GLFW_KEY_UP, null, null);
        if (GameSettings.keyForward != GLFW.GLFW_KEY_UP) {
            throw new AssertionError("keyForward should now be GLFW_KEY_UP, got: " + GameSettings.keyForward);
        }
        if (menu.getRebindingAction() != null) {
            throw new AssertionError("Rebinding action should be cleared after binding");
        }

        // Test ESC cancels rebinding without changing key
        menu.setRebindingAction(OptionsMenu.KeyAction.BACK);
        menu.handleKey(GLFW.GLFW_KEY_ESCAPE, null, null);
        if (GameSettings.keyBack != GLFW.GLFW_KEY_S) {
            throw new AssertionError("keyBack should remain S after cancel, got: " + GameSettings.keyBack);
        }

        // Test slot key rebinding (Hotbar Slot 1 rebound to key R)
        menu.setRebindingAction(OptionsMenu.KeyAction.SLOT_1);
        menu.handleKey(GLFW.GLFW_KEY_R, null, null);
        if (GameSettings.keyHotbar[0] != GLFW.GLFW_KEY_R) {
            throw new AssertionError("keyHotbar[0] should be GLFW_KEY_R, got: " + GameSettings.keyHotbar[0]);
        }

        GameSettings.resetToDefaults();
        if (GameSettings.keyForward != GLFW.GLFW_KEY_W || GameSettings.keyHotbar[0] != GLFW.GLFW_KEY_1) {
            throw new AssertionError("Reset defaults should restore W and 1");
        }
        System.out.println("  [PASS] Interactive keybinding remapping verified.");
    }
}
