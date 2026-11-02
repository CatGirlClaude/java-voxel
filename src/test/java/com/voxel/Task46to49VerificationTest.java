package com.voxel;

import com.voxel.config.GameSettings;
import com.voxel.i18n.I18n;
import com.voxel.ui.MainMenu;

public class Task46to49VerificationTest {

    public static void main(String[] args) {
        System.out.println("Running Task 46, 47, 49 verification...");
        testDefaultWorldNameIsEnglish();
        testSaveFormatLocalization();
        System.out.println(">>> ALL TASK 46, 47, 49 TESTS PASSED! <<<");
    }

    private static void testDefaultWorldNameIsEnglish() {
        MainMenu menu = new MainMenu();
        String defaultName = menu.getNewWorldName();
        if (!defaultName.startsWith("World_")) {
            throw new AssertionError("Default new world name should start with 'World_', but got: " + defaultName);
        }
        if (defaultName.contains("Swiat")) {
            throw new AssertionError("Default new world name must not contain Polish word 'Swiat'!");
        }
        System.out.println("  [PASS] Default new world name 'World_1' verified.");
    }

    private static void testSaveFormatLocalization() {
        // In English:
        I18n.load("en_us");
        GameSettings.saveFormat = 1;
        String mcaEn = GameSettings.getSaveFormatName();
        if (!mcaEn.equals("Region Storage (.mca)")) {
            throw new AssertionError("Expected 'Region Storage (.mca)' in en_us, got: " + mcaEn);
        }

        GameSettings.saveFormat = 0;
        String datEn = GameSettings.getSaveFormatName();
        if (!datEn.equals("Block Diffs (.dat)")) {
            throw new AssertionError("Expected 'Block Diffs (.dat)' in en_us, got: " + datEn);
        }

        // In Polish:
        I18n.load("pl_pl");
        GameSettings.saveFormat = 1;
        String mcaPl = GameSettings.getSaveFormatName();
        if (!mcaPl.equals("Regiony (.mca)")) {
            throw new AssertionError("Expected 'Regiony (.mca)' in pl_pl, got: " + mcaPl);
        }

        GameSettings.saveFormat = 0;
        String datPl = GameSettings.getSaveFormatName();
        if (!datPl.equals("Roznice blokow (.dat)")) {
            throw new AssertionError("Expected 'Roznice blokow (.dat)' in pl_pl, got: " + datPl);
        }

        // Restore English
        I18n.load("en_us");
        System.out.println("  [PASS] System & Lang save format localization verified.");
    }
}
