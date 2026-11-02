package com.voxel;

import com.voxel.audio.SoundSystem;
import com.voxel.block.BlockType;
import com.voxel.config.GameSettings;

public class Task61AudioSystemTest {

    public static void main(String[] args) {
        System.out.println("Running Task 61 verification (Procedural Audio System & Sound Triggers)...");

        testMaterialMapping();
        testAudioInitializationAndPlayback();
        testVolumeMutingAndEdgeCases();

        System.out.println(">>> ALL TASK 61 AUDIO SYSTEM TESTS PASSED! <<<");
    }

    private static void testMaterialMapping() {
        if (SoundSystem.getMaterial(BlockType.STONE) != SoundSystem.SoundMaterial.STONE) {
            throw new AssertionError("STONE should map to STONE material");
        }
        if (SoundSystem.getMaterial(BlockType.WOOD) != SoundSystem.SoundMaterial.WOOD) {
            throw new AssertionError("WOOD should map to WOOD material");
        }
        if (SoundSystem.getMaterial(BlockType.SAND) != SoundSystem.SoundMaterial.SAND) {
            throw new AssertionError("SAND should map to SAND material");
        }
        if (SoundSystem.getMaterial(BlockType.GRAVEL) != SoundSystem.SoundMaterial.GRAVEL) {
            throw new AssertionError("GRAVEL should map to GRAVEL material");
        }
        if (SoundSystem.getMaterial(BlockType.GLASS) != SoundSystem.SoundMaterial.GLASS) {
            throw new AssertionError("GLASS should map to GLASS material");
        }
        if (SoundSystem.getMaterial(BlockType.GRASS) != SoundSystem.SoundMaterial.GRASS) {
            throw new AssertionError("GRASS should map to GRASS material");
        }
        if (SoundSystem.getMaterial(BlockType.WATER) != SoundSystem.SoundMaterial.WATER) {
            throw new AssertionError("WATER should map to WATER material");
        }
        if (SoundSystem.getMaterial(BlockType.KELP) != SoundSystem.SoundMaterial.PLANT) {
            throw new AssertionError("KELP should map to PLANT material");
        }
        if (SoundSystem.getMaterial(BlockType.SEAGRASS) != SoundSystem.SoundMaterial.PLANT) {
            throw new AssertionError("SEAGRASS should map to PLANT material");
        }
        if (SoundSystem.getMaterial(BlockType.LEAVES) != SoundSystem.SoundMaterial.PLANT) {
            throw new AssertionError("LEAVES should map to PLANT material");
        }
    }

    private static void testAudioInitializationAndPlayback() {
        SoundSystem.init();

        // Test playing various sounds with volume enabled
        GameSettings.masterVolume = 80;

        SoundSystem.playDigSound(BlockType.STONE);
        SoundSystem.playDigSound(BlockType.GRASS);
        SoundSystem.playPlaceSound(BlockType.WOOD);
        SoundSystem.playPlaceSound(BlockType.SAND);
        SoundSystem.playStepSound(BlockType.DIRT);
        SoundSystem.playStepSound(BlockType.GRAVEL);
        SoundSystem.playSplashSound();
        SoundSystem.playClickSound();
        SoundSystem.playCraftSound();

        // Allow audio worker thread to process queue
        try {
            Thread.sleep(50);
        } catch (InterruptedException ignored) {}
    }

    private static void testVolumeMutingAndEdgeCases() {
        // Muted volume should silently skip playback
        GameSettings.masterVolume = 0;
        SoundSystem.playDigSound(BlockType.STONE);
        SoundSystem.playPlaceSound(BlockType.WOOD);
        SoundSystem.playStepSound(BlockType.GRASS);
        SoundSystem.playSplashSound();
        SoundSystem.playClickSound();
        SoundSystem.playCraftSound();

        // Null block type should fall back gracefully
        SoundSystem.playDigSound(null);
        SoundSystem.playPlaceSound(null);
        SoundSystem.playStepSound(null);

        SoundSystem.cleanup();
    }
}
