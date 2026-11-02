package com.voxel;

import com.voxel.block.BlockType;
import com.voxel.config.DebugHudConfig;
import com.voxel.config.GameSettings;
import com.voxel.entity.Player;
import com.voxel.i18n.I18n;
import com.voxel.ui.ChatSystem;
import com.voxel.ui.InventoryMenu;
import com.voxel.world.World;
import com.voxel.world.WorldSaveManager;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class FeatureSetVerification {

    public static void main(String[] args) {
        System.out.println("=================================================");
        System.out.println("  RUNNING COMPREHENSIVE FEATURE SET VERIFICATION  ");
        System.out.println("=================================================");

        testLocalization();
        testDebugHudConfiguration();
        testCreativeMenuDimensions();
        testChatAutocompletion();
        testWorldGamerulesAndOverrides();

        System.out.println("\n>>> ALL NEW FEATURES VERIFIED SUCCESSFULLY (100% PASS)! <<<");
    }

    private static void testLocalization() {
        System.out.println("\n--- TEST: JSON Localization (I18n) ---");

        // 1. English (Default)
        I18n.load("en_us");
        if (!"en_us".equals(I18n.getCurrentLang())) {
            throw new AssertionError("Expected current language to be en_us");
        }
        String enTitle = I18n.t("menu.title");
        String enResume = I18n.t("pause.resume");
        String enStone = I18n.t("block.stone");
        System.out.printf("[en_us] menu.title='%s', pause.resume='%s', block.stone='%s'\n", enTitle, enResume, enStone);

        if (!"Back to Game".equals(enResume)) {
            throw new AssertionError("Expected 'Back to Game', got: " + enResume);
        }
        if (!"Stone".equals(enStone)) {
            throw new AssertionError("Expected 'Stone', got: " + enStone);
        }

        // 2. Polish
        I18n.load("pl_pl");
        String plResume = I18n.t("pause.resume");
        String plStone = I18n.t("block.stone");
        System.out.printf("[pl_pl] pause.resume='%s', block.stone='%s'\n", plResume, plStone);

        if (!"Wroc do gry".equals(plResume)) {
            throw new AssertionError("Expected 'Wroc do gry', got: " + plResume);
        }
        if (!"Kamien".equals(plStone)) {
            throw new AssertionError("Expected 'Kamien', got: " + plStone);
        }

        // Reset to English as default
        I18n.load("en_us");
        System.out.println("Localization system OK.");
    }

    private static void testDebugHudConfiguration() {
        System.out.println("\n--- TEST: Debug HUD Configuration & Benchmark Placement ---");
        DebugHudConfig.resetToDefaults();
        List<DebugHudConfig.HudItem> items = DebugHudConfig.getItems();

        // Verify benchmark exists and is by default on the right side under specs
        DebugHudConfig.HudItem benchItem = null;
        for (DebugHudConfig.HudItem it : items) {
            if (it.id == DebugHudConfig.ElementId.BENCHMARK) {
                benchItem = it;
                break;
            }
        }
        if (benchItem == null) {
            throw new AssertionError("Benchmark element not found in DebugHudConfig!");
        }
        if (benchItem.side != DebugHudConfig.Side.RIGHT) {
            throw new AssertionError("Benchmark element expected on RIGHT side by default!");
        }

        // Test toggle side
        int benchIdx = items.indexOf(benchItem);
        DebugHudConfig.toggleSide(benchIdx);
        if (benchItem.side != DebugHudConfig.Side.LEFT) {
            throw new AssertionError("Expected benchmark side to toggle to LEFT");
        }

        // Test toggle enabled
        DebugHudConfig.toggleEnabled(benchIdx);
        if (benchItem.enabled) {
            throw new AssertionError("Expected benchmark enabled to toggle to false");
        }

        // Test serialization & deserialization
        String serialized = DebugHudConfig.serialize();
        DebugHudConfig.resetToDefaults();
        DebugHudConfig.deserialize(serialized);

        DebugHudConfig.HudItem restored = DebugHudConfig.getItems().get(benchIdx);
        if (restored.id != DebugHudConfig.ElementId.BENCHMARK || restored.enabled || restored.side != DebugHudConfig.Side.LEFT) {
            throw new AssertionError("Deserialization did not preserve state!");
        }

        DebugHudConfig.resetToDefaults();
        System.out.println("Debug HUD configuration & serialization OK.");
    }

    private static void testCreativeMenuDimensions() {
        System.out.println("\n--- TEST: Creative Menu 9-slot Rows & Hotbar Alignment ---");
        if (InventoryMenu.COLS != 9) {
            throw new AssertionError("Creative menu must have 9 columns per row, got: " + InventoryMenu.COLS);
        }
        float totalGridW = InventoryMenu.COLS * InventoryMenu.SLOT_SIZE + (InventoryMenu.COLS + 1) * InventoryMenu.SLOT_PAD;
        float totalHotbarW = 9 * InventoryMenu.HOTBAR_SLOT_SIZE + 8 * InventoryMenu.HOTBAR_SLOT_PAD;
        System.out.printf("Creative Grid Width: %.1f px | Hotbar Width: %.1f px\n", totalGridW, totalHotbarW);

        if (totalHotbarW > totalGridW) {
            throw new AssertionError("Hotbar must fit inside the creative menu width!");
        }
        System.out.println("Creative menu grid & hotbar metrics OK.");
    }

    private static void testChatAutocompletion() {
        System.out.println("\n--- TEST: Chat Suggestions & Autocompletion ---");
        ChatSystem chat = new ChatSystem();

        // 1. Typing command prefix "/t"
        chat.openChat("/t", false);
        if (!chat.hasSuggestions()) {
            throw new AssertionError("Expected suggestions for '/t'");
        }
        boolean hasTp = false;
        boolean hasTime = false;
        for (ChatSystem.Suggestion s : chat.getSuggestions()) {
            if (s.label().equals("/tp")) hasTp = true;
            if (s.label().equals("/time")) hasTime = true;
        }
        if (!hasTp || !hasTime) {
            throw new AssertionError("Expected '/tp' and '/time' in suggestions for '/t'");
        }

        // 2. Tab completion
        chat.onTabComplete();
        String currentInput = chat.getInputText();
        System.out.printf("After Tab completion on '/t': '%s'\n", currentInput);
        if (!currentInput.startsWith("/tp") && !currentInput.startsWith("/time")) {
            throw new AssertionError("Tab completion failed to complete command!");
        }

        // 3. Argument completion for "/time "
        chat.openChat("/time ", false);
        if (!chat.hasSuggestions()) {
            throw new AssertionError("Expected argument suggestions for '/time '");
        }
        boolean hasDay = false;
        boolean hasNoon = false;
        for (ChatSystem.Suggestion s : chat.getSuggestions()) {
            if (s.label().equals("day")) hasDay = true;
            if (s.label().equals("noon")) hasNoon = true;
        }
        if (!hasDay || !hasNoon) {
            throw new AssertionError("Expected 'day' and 'noon' in /time suggestions");
        }

        chat.closeChat();
        System.out.println("Chat suggestions and autocompletion OK.");
    }

    private static void testWorldGamerulesAndOverrides() {
        System.out.println("\n--- TEST: World Save Gamerules & Settings Overrides ---");
        String testSaveName = "test_gamerules_world";
        World world = new World(777888L);
        Player player = new Player();

        world.timeOfDay = 0.77f; // sunset
        world.dayCyclePaused = true;
        player.position.set(10.0, 65.0, 20.0);
        player.flying = true;
        player.noclip = true;
        player.hotbar[0] = BlockType.GLOWSTONE;

        // Save world
        boolean saved = world.saveWorld(player, testSaveName);
        if (!saved) {
            throw new AssertionError("Failed to save world!");
        }

        // Load into fresh world and player without overrides
        GameSettings.enableOverrides = false;
        World loadedWorld = new World(1L);
        Player loadedPlayer = new Player();
        boolean loaded = loadedWorld.loadWorld(loadedPlayer, testSaveName);
        if (!loaded) {
            throw new AssertionError("Failed to load world!");
        }

        if (Math.abs(loadedWorld.timeOfDay - 0.77f) > 0.01f) {
            throw new AssertionError("Time of day not preserved: " + loadedWorld.timeOfDay);
        }
        if (!loadedWorld.dayCyclePaused) {
            throw new AssertionError("dayCyclePaused not preserved in world save!");
        }
        if (!loadedPlayer.noclip) {
            throw new AssertionError("player.noclip not preserved in world save!");
        }
        if (!loadedPlayer.flying) {
            throw new AssertionError("player.flying not preserved in world save!");
        }
        if (loadedPlayer.hotbar[0] != BlockType.GLOWSTONE) {
            throw new AssertionError("player.hotbar not preserved in world save!");
        }

        // Now test settings overrides
        GameSettings.enableOverrides = true;
        GameSettings.overrideTime = true;
        GameSettings.overrideTimeValue = 0.50f; // noon
        GameSettings.overrideDayCycle = true;
        GameSettings.overrideDayCycleValue = false; // running
        GameSettings.overrideNoclip = 2; // forced OFF

        WorldSaveManager.applyGameruleOverrides(loadedWorld, loadedPlayer);
        if (Math.abs(loadedWorld.timeOfDay - 0.50f) > 0.01f) {
            throw new AssertionError("Override time not applied: " + loadedWorld.timeOfDay);
        }
        if (loadedWorld.dayCyclePaused) {
            throw new AssertionError("Override day cycle not applied!");
        }
        if (loadedPlayer.noclip) {
            throw new AssertionError("Override noclip not applied!");
        }

        // Test "Disable All Overrides" master button
        GameSettings.disableAllOverrides();
        if (GameSettings.enableOverrides) {
            throw new AssertionError("disableAllOverrides() failed to set enableOverrides=false");
        }

        // Clean up test file
        WorldSaveManager.deleteSave(testSaveName);
        System.out.println("Gamerules persistence and overrides system OK.");
    }
}
