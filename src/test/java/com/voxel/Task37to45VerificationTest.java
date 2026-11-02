package com.voxel;

import com.voxel.block.BlockType;
import com.voxel.config.GameSettings;
import com.voxel.entity.Player;
import com.voxel.ui.InventoryMenu;
import org.lwjgl.glfw.GLFW;

public class Task37to45VerificationTest {

    public static void main(String[] args) {
        System.out.println("Running Task 37, 42, 45 verification...");
        testHotbarKeyBindingResolution();
        testCreativeMenuHotbarSlotAssignmentViaKey();
        System.out.println(">>> ALL TASK 37, 42, 45 TESTS PASSED! <<<");
    }

    private static void testHotbarKeyBindingResolution() {
        GameSettings.resetToDefaults();

        if (GameSettings.getHotbarSlotForKey(GLFW.GLFW_KEY_1) != 0) {
            throw new AssertionError("Key 1 should map to slot 0");
        }
        if (GameSettings.getHotbarSlotForKey(GLFW.GLFW_KEY_5) != 4) {
            throw new AssertionError("Key 5 should map to slot 4");
        }
        if (GameSettings.getHotbarSlotForKey(GLFW.GLFW_KEY_9) != 8) {
            throw new AssertionError("Key 9 should map to slot 8");
        }

        // Custom keybind (e.g. key R bound to slot 3, index 2)
        GameSettings.keyHotbar[2] = GLFW.GLFW_KEY_R;
        if (GameSettings.getHotbarSlotForKey(GLFW.GLFW_KEY_R) != 2) {
            throw new AssertionError("Key R should map to slot 2");
        }

        GameSettings.resetToDefaults();
    }

    private static void testCreativeMenuHotbarSlotAssignmentViaKey() {
        GameSettings.resetToDefaults();
        Player player = new Player();
        InventoryMenu menu = new InventoryMenu();
        menu.setOpen(true);
        menu.onResize(1280, 720);

        // Compute coordinate for the first creative slot
        // winW = 9 * 48 + 10 * 6 = 492
        // winH = 5 * 48 + 6 * 6 + 120 = 396
        // winX = (1280 - 492) / 2 = 394
        // winY = (720 - 396) / 2 = 162
        // First slot: slotX = 394 + 6 = 400, slotY = 162 + 44 = 206
        float mouseX = 400.0f + 10.0f;
        float mouseY = 206.0f + 10.0f;

        BlockType hovered = menu.getHoveredBlock(mouseX, mouseY);
        if (hovered == null) {
            throw new AssertionError("Hovered block in creative menu slot 0 should not be null");
        }

        // Set hotbar slot 0 to AIR initially
        player.setHotbarSlot(0, BlockType.AIR);
        if (player.hotbar[0] != BlockType.AIR) {
            throw new AssertionError("Slot 0 should be AIR");
        }

        // Press key 1 over hovered block
        boolean handled = menu.handleKey(GLFW.GLFW_KEY_1, mouseX, mouseY, player);
        if (!handled) {
            throw new AssertionError("Handle key should consume slot selection key");
        }
        if (player.hotbar[0] != hovered) {
            throw new AssertionError("Player hotbar slot 0 should receive the hovered block, but got " + player.hotbar[0]);
        }

        // Now test custom bound key: bind slot 4 (index 3) to GLFW_KEY_R
        GameSettings.keyHotbar[3] = GLFW.GLFW_KEY_R;
        player.setHotbarSlot(3, BlockType.AIR);
        handled = menu.handleKey(GLFW.GLFW_KEY_R, mouseX, mouseY, player);
        if (!handled) {
            throw new AssertionError("Handle key should consume key R");
        }
        if (player.hotbar[3] != hovered) {
            throw new AssertionError("Player hotbar slot 3 should receive block assigned via custom key R");
        }

        GameSettings.resetToDefaults();
    }
}
