package com.voxel;

import com.voxel.block.BlockType;
import com.voxel.config.GameSettings;
import com.voxel.entity.Player;
import com.voxel.world.Chunk;
import com.voxel.world.World;
import com.voxel.world.WorldSaveManager;

import java.nio.file.Files;
import java.nio.file.Path;

public class WorldSystemVerification {

    public static void main(String[] args) {
        System.out.println("=== TEST 1: Weryfikacja priorytetyzacji generowania chunkow ===");
        testPriorityScoring();

        System.out.println("\n=== TEST 2: Weryfikacja zapisu i odczytu formatu Roznic (.dat) ===");
        testBlockDiffsSaveLoad();

        System.out.println("\n=== TEST 3: Weryfikacja zapisu i odczytu formatu Regionow (.mca) ===");
        testRegionStorageSaveLoad();

        System.out.println("\n=== TEST 4: Kompatybilnosc wsteczna i przelaczanie formatow ===");
        testBackwardCompatibilityAndSwitching();

        System.out.println("\n>>> WSZYSTKIE TESTY ZAKONCZONE SUKCESEM (100% PASS)! <<<");
    }

    private static void testPriorityScoring() {
        int scoreEye = World.calculateChunkPriorityScore(0, 0, 0);
        int scoreFeet = World.calculateChunkPriorityScore(0, -1, 0);
        int scoreNearSurface = World.calculateChunkPriorityScore(1, 0, 0);
        int scoreDeepCave4 = World.calculateChunkPriorityScore(0, -4, 0);
        int scoreDeepCave5 = World.calculateChunkPriorityScore(0, -5, 0);

        System.out.printf("Score Eye (0,0,0): %d\n", scoreEye);
        System.out.printf("Score Feet (0,-1,0): %d\n", scoreFeet);
        System.out.printf("Score Near Surface (1,0,0): %d\n", scoreNearSurface);
        System.out.printf("Score Cave (0,-4,0): %d\n", scoreDeepCave4);
        System.out.printf("Score Cave (0,-5,0): %d\n", scoreDeepCave5);

        if (scoreEye >= scoreFeet && scoreFeet != 1) {
            throw new AssertionError("Oczekiwano, ze poziom stop (dy=-1) ma bezposredni wysoki priorytet!");
        }
        if (scoreNearSurface >= scoreDeepCave4) {
            throw new AssertionError("Oczekiwano, ze pobliska powierzchnia generuje sie przed jaskiniami na poziomie dy=-4!");
        }
        if (scoreDeepCave4 >= scoreDeepCave5) {
            throw new AssertionError("Glebsze jaskinie powinny miec wyzszy wynik kary (generowac sie pozniej)!");
        }

        World world = new World(12345L);
        world.setRenderDistance(6);
        System.out.println("Priorytetyzacja chunkow dziala poprawnie.");
    }

    private static void testBlockDiffsSaveLoad() {
        String testSave = "test_diff_save";
        WorldSaveManager.deleteSave(testSave);

        GameSettings.saveFormat = 0; // Block Diffs
        Player player = new Player();
        player.position.set(42.5, 65.0, -88.5);
        player.flying = true;

        World world = new World(987654321L);
        // Modyfikacja kilku blokow
        world.setBlock(42, 64, -88, BlockType.GLOWSTONE.id, true);
        world.setBlock(43, 64, -88, BlockType.BRICK.id, true);

        boolean saved = WorldSaveManager.saveWorld(world, player, testSave);
        if (!saved) throw new AssertionError("Zapis do .dat nie powiodl sie!");

        Path savePath = com.voxel.util.GamePaths.resolve("saves", testSave + ".dat");
        if (!Files.exists(savePath)) throw new AssertionError("Plik .dat nie zostal utworzony!");

        // Wczytanie swiata
        Player loadedPlayer = new Player();
        World loadedWorld = new World(1L);
        boolean loaded = WorldSaveManager.loadWorld(loadedWorld, loadedPlayer, testSave);
        if (!loaded) throw new AssertionError("Wczytanie z .dat nie powiodlo sie!");

        if (loadedWorld.getSeed() != 987654321L) {
            throw new AssertionError("Niezgodny seed: " + loadedWorld.getSeed());
        }
        if (Math.abs(loadedPlayer.position.x - 42.5) > 0.001 || !loadedPlayer.flying) {
            throw new AssertionError("Niezgodny stan gracza po wczytaniu!");
        }
        if (loadedWorld.getBlock(42, 64, -88) != BlockType.GLOWSTONE.id) {
            throw new AssertionError("Zmodyfikowany blok (Glowstone) nie zostal przywrocony!");
        }
        if (loadedWorld.getBlock(43, 64, -88) != BlockType.BRICK.id) {
            throw new AssertionError("Zmodyfikowany blok (Brick) nie zostal przywrocony!");
        }

        WorldSaveManager.deleteSave(testSave);
        System.out.println("Zapis i odczyt w formacie Roznic (.dat) przeszedl pomyslnie.");
    }

    private static void testRegionStorageSaveLoad() {
        String testSave = "test_region_save";
        WorldSaveManager.deleteSave(testSave);

        GameSettings.saveFormat = 1; // Region Storage (.mca)
        Player player = new Player();
        player.position.set(16.5, 35.0, 16.5);
        player.flying = false;

        World world = new World(555666777L);
        // Modyfikacja blokow w chunku (1, 2, 1)
        world.setBlock(20, 35, 20, BlockType.GLASS.id, true);
        world.setBlock(21, 35, 20, BlockType.PLANKS.id, true);

        boolean saved = WorldSaveManager.saveWorld(world, player, testSave);
        if (!saved) throw new AssertionError("Zapis do Regionow .mca nie powiodl sie!");

        Path savePath = com.voxel.util.GamePaths.resolve("saves", testSave + ".dat");
        Path regionDir = com.voxel.util.GamePaths.resolve("saves", testSave + "_regions");
        Path mcaFile = regionDir.resolve("r.0.0.mca");

        if (!Files.exists(savePath)) throw new AssertionError("Plik naglowka .dat nie zostal utworzony!");
        if (!Files.exists(regionDir) || !Files.exists(mcaFile)) {
            throw new AssertionError("Plik regionu r.0.0.mca nie zostal utworzony w " + regionDir);
        }

        System.out.printf("Utworzono plik regionu: %s (rozmiar: %d bajtow)\n", mcaFile, mcaFile.toFile().length());

        // Wczytanie swiata ze skompresowanego regionu MCA
        Player loadedPlayer = new Player();
        World loadedWorld = new World(1L);
        boolean loaded = WorldSaveManager.loadWorld(loadedWorld, loadedPlayer, testSave);
        if (!loaded) throw new AssertionError("Wczytanie z Regionow MCA nie powiodlo sie!");

        if (loadedWorld.getSeed() != 555666777L) {
            throw new AssertionError("Niezgodny seed w regionie: " + loadedWorld.getSeed());
        }
        if (loadedWorld.getBlock(20, 35, 20) != BlockType.GLASS.id) {
            throw new AssertionError("Zmodyfikowany blok z regionu (Glass) nie zostal przywrocony!");
        }
        if (loadedWorld.getBlock(21, 35, 20) != BlockType.PLANKS.id) {
            throw new AssertionError("Zmodyfikowany blok z regionu (Planks) nie zostal przywrocony!");
        }

        // Sprawdzenie czy chunk zostal zaladowany bezposrednio z savedRegionChunks
        Chunk c = loadedWorld.getOrCreateChunk(1, 2, 1);
        if (c.getBlock(4, 3, 4) != BlockType.GLASS.id) {
            throw new AssertionError("Blok wewnatrz chunka nie zgadza sie ze stanem zapisanym w MCA!");
        }

        WorldSaveManager.deleteSave(testSave);
        if (Files.exists(regionDir)) {
            throw new AssertionError("Katalog regionow powinien zostac usuniety po deleteSave!");
        }
        System.out.println("Zapis i odczyt w formacie Regionow MCA (.mca) przeszedl pomyslnie.");
    }

    private static void testBackwardCompatibilityAndSwitching() {
        String testSave = "test_switch_save";
        WorldSaveManager.deleteSave(testSave);

        // 1. Tworzymy i zapisujemy w starym formacie VOX4 (Roznice)
        GameSettings.saveFormat = 0;
        Player player = new Player();
        player.position.set(0.5, 64.0, 0.5);
        World world = new World(11223344L);
        world.setBlock(0, 64, 0, BlockType.COBBLESTONE.id, true);
        WorldSaveManager.saveWorld(world, player, testSave);

        // 2. Gracz przelacza format na Regiony w opcjach i wczytuje swiat
        GameSettings.saveFormat = 1;
        World loadedWorld = new World(1L);
        boolean loaded = WorldSaveManager.loadWorld(loadedWorld, player, testSave);
        if (!loaded || loadedWorld.getBlock(0, 64, 0) != BlockType.COBBLESTONE.id) {
            throw new AssertionError("Format VOX4 powinien bezproblemowo wczytywac sie przy aktywnym saveFormat=1!");
        }

        // 3. Zapisujemy w nowym formacie (Regiony MCA)
        world.setBlock(1, 64, 0, BlockType.SANDSTONE.id, true);
        WorldSaveManager.saveWorld(world, player, testSave);

        // 4. Przelaczamy z powrotem na Roznice i wczytujemy swiat zapisany jako Region
        GameSettings.saveFormat = 0;
        World loadedWorld2 = new World(1L);
        boolean loaded2 = WorldSaveManager.loadWorld(loadedWorld2, player, testSave);
        if (!loaded2 || loadedWorld2.getBlock(1, 64, 0) != BlockType.SANDSTONE.id) {
            throw new AssertionError("Format Regionow (VOXR) powinien bezproblemowo wczytywac sie niezaleznie od aktualnego ustawienia opcji!");
        }

        WorldSaveManager.deleteSave(testSave);
        System.out.println("Pelna kompatybilnosc wsteczna i plynne przelaczanie formatow zweryfikowane pomyslnie.");
    }
}
