package com.voxel.audio;

import com.voxel.block.BlockType;
import com.voxel.config.GameSettings;
import com.voxel.util.Logger;

import javax.sound.sampled.*;
import java.io.ByteArrayInputStream;
import java.util.EnumMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class SoundSystem {
    public static final int SAMPLE_RATE = 22050;
    private static final AudioFormat FORMAT = new AudioFormat(SAMPLE_RATE, 16, 1, true, false);

    public enum SoundCategory {
        DIG, PLACE, STEP, SPLASH, CLICK, CRAFT
    }

    public enum SoundMaterial {
        GRASS, STONE, WOOD, SAND, GRAVEL, GLASS, PLANT, WATER
    }

    private static final Map<SoundMaterial, byte[][]> DIG_CACHE = new EnumMap<>(SoundMaterial.class);
    private static final Map<SoundMaterial, byte[][]> PLACE_CACHE = new EnumMap<>(SoundMaterial.class);
    private static final Map<SoundMaterial, byte[][]> STEP_CACHE = new EnumMap<>(SoundMaterial.class);
    private static byte[][] SPLASH_CACHE;
    private static byte[] CLICK_CACHE;
    private static byte[] CRAFT_CACHE;

    private static final Random RNG = new Random(42);
    private static ExecutorService audioExecutor;
    private static boolean initialized = false;

    private SoundSystem() {}

    public static synchronized void init() {
        if (initialized) return;
        try {
            audioExecutor = Executors.newFixedThreadPool(2, r -> {
                Thread t = new Thread(r, "Audio-Worker");
                t.setDaemon(true);
                return t;
            });

            generateAllSounds();
            initialized = true;
            Logger.info("SoundSystem", "Initialized procedural audio engine (Sample rate: %d Hz)", SAMPLE_RATE);
        } catch (Exception e) {
            Logger.warn("SoundSystem", "Failed to initialize sound engine: %s", e.getMessage());
        }
    }

    private static void generateAllSounds() {
        for (SoundMaterial mat : SoundMaterial.values()) {
            DIG_CACHE.put(mat, new byte[][]{
                    generateDigSound(mat, 0.94f),
                    generateDigSound(mat, 1.00f),
                    generateDigSound(mat, 1.06f)
            });
            PLACE_CACHE.put(mat, new byte[][]{
                    generatePlaceSound(mat, 0.95f),
                    generatePlaceSound(mat, 1.00f),
                    generatePlaceSound(mat, 1.05f)
            });
            STEP_CACHE.put(mat, new byte[][]{
                    generateStepSound(mat, 0.94f),
                    generateStepSound(mat, 1.00f),
                    generateStepSound(mat, 1.06f)
            });
        }
        SPLASH_CACHE = new byte[][]{
                generateSplashSound(0.95f),
                generateSplashSound(1.05f)
        };
        CLICK_CACHE = generateClickSound();
        CRAFT_CACHE = generateCraftSound();
    }

    public static SoundMaterial getMaterial(BlockType b) {
        if (b == null) return SoundMaterial.STONE;
        if (b.isWater()) return SoundMaterial.WATER;

        return switch (b) {
            case WOOD, BIRCH_WOOD, SPRUCE_WOOD, JUNGLE_WOOD, PLANKS -> SoundMaterial.WOOD;
            case STONE, COBBLESTONE, BRICK, SANDSTONE, TERRACOTTA -> SoundMaterial.STONE;
            case SAND, RED_SAND -> SoundMaterial.SAND;
            case GRAVEL, CLAY -> SoundMaterial.GRAVEL;
            case GLASS, ICE, GLOWSTONE -> SoundMaterial.GLASS;
            case TALL_GRASS, ROSE, DANDELION, BLUE_ORCHID, ALLIUM, SEAGRASS, KELP, VINE, SUGAR_CANE, CACTUS,
                 LEAVES, JUNGLE_LEAVES, SPRUCE_LEAVES, BIRCH_LEAVES, CORAL -> SoundMaterial.PLANT;
            case GRASS, DIRT, SNOW, SNOWY_GRASS, TAIGA_GRASS, JUNGLE_GRASS -> SoundMaterial.GRASS;
            default -> SoundMaterial.STONE;
        };
    }

    public static void playDigSound(BlockType b) {
        if (GameSettings.masterVolume <= 0 || !initialized) return;
        SoundMaterial mat = getMaterial(b);
        byte[][] variations = DIG_CACHE.get(mat);
        if (variations != null && variations.length > 0) {
            byte[] pcm = variations[RNG.nextInt(variations.length)];
            playPcmAsync(pcm, 0.75f);
        }
    }

    public static void playPlaceSound(BlockType b) {
        if (GameSettings.masterVolume <= 0 || !initialized) return;
        SoundMaterial mat = getMaterial(b);
        byte[][] variations = PLACE_CACHE.get(mat);
        if (variations != null && variations.length > 0) {
            byte[] pcm = variations[RNG.nextInt(variations.length)];
            playPcmAsync(pcm, 0.70f);
        }
    }

    public static void playStepSound(BlockType b) {
        if (GameSettings.masterVolume <= 0 || !initialized) return;
        SoundMaterial mat = getMaterial(b);
        byte[][] variations = STEP_CACHE.get(mat);
        if (variations != null && variations.length > 0) {
            byte[] pcm = variations[RNG.nextInt(variations.length)];
            playPcmAsync(pcm, 0.40f);
        }
    }

    public static void playSplashSound() {
        if (GameSettings.masterVolume <= 0 || !initialized) return;
        if (SPLASH_CACHE != null && SPLASH_CACHE.length > 0) {
            byte[] pcm = SPLASH_CACHE[RNG.nextInt(SPLASH_CACHE.length)];
            playPcmAsync(pcm, 0.65f);
        }
    }

    public static void playClickSound() {
        if (GameSettings.masterVolume <= 0 || !initialized) return;
        if (CLICK_CACHE != null) {
            playPcmAsync(CLICK_CACHE, 0.50f);
        }
    }

    public static void playCraftSound() {
        if (GameSettings.masterVolume <= 0 || !initialized) return;
        if (CRAFT_CACHE != null) {
            playPcmAsync(CRAFT_CACHE, 0.70f);
        }
    }

    private static void playPcmAsync(byte[] pcm, float baseGain) {
        if (audioExecutor == null || audioExecutor.isShutdown()) return;
        float vol = (GameSettings.masterVolume / 100.0f) * baseGain;
        if (vol <= 0.001f) return;

        audioExecutor.submit(() -> {
            try {
                byte[] scaled = scaleVolume(pcm, vol);
                AudioInputStream ais = new AudioInputStream(new ByteArrayInputStream(scaled), FORMAT, scaled.length / 2);
                Clip clip = AudioSystem.getClip();
                clip.open(ais);
                clip.addLineListener(event -> {
                    if (event.getType() == LineEvent.Type.STOP) {
                        clip.close();
                    }
                });
                clip.start();
            } catch (Exception ignored) {
                // Audio hardware busy or absent; fails silently without impacting gameplay
            }
        });
    }

    private static byte[] scaleVolume(byte[] src, float volume) {
        byte[] dst = new byte[src.length];
        for (int i = 0; i < src.length; i += 2) {
            short sample = (short) ((src[i] & 0xFF) | (src[i + 1] << 8));
            int scaled = Math.round(sample * volume);
            scaled = Math.max(-32768, Math.min(32767, scaled));
            dst[i] = (byte) (scaled & 0xFF);
            dst[i + 1] = (byte) ((scaled >> 8) & 0xFF);
        }
        return dst;
    }

    // --- Procedural PCM Synthesis Generators ---

    private static byte[] generateDigSound(SoundMaterial mat, float pitchMod) {
        int durationSamples = (int) (SAMPLE_RATE * 0.11f);
        short[] samples = new short[durationSamples];

        float centerFreq = switch (mat) {
            case STONE -> 520.0f * pitchMod;
            case WOOD -> 210.0f * pitchMod;
            case SAND -> 380.0f * pitchMod;
            case GRAVEL -> 330.0f * pitchMod;
            case GLASS -> 1600.0f * pitchMod;
            case PLANT -> 440.0f * pitchMod;
            case WATER -> 280.0f * pitchMod;
            case GRASS -> 260.0f * pitchMod;
        };

        for (int i = 0; i < durationSamples; i++) {
            float t = (float) i / SAMPLE_RATE;
            float env = (float) Math.exp(-t * 32.0f);
            float noise = (RNG.nextFloat() * 2.0f - 1.0f);
            float tone = (float) Math.sin(2.0 * Math.PI * centerFreq * t);
            float s = (noise * 0.7f + tone * 0.3f) * env;
            samples[i] = (short) (s * 24000);
        }
        return toPcmBytes(samples);
    }

    private static byte[] generatePlaceSound(SoundMaterial mat, float pitchMod) {
        int durationSamples = (int) (SAMPLE_RATE * 0.08f);
        short[] samples = new short[durationSamples];

        float centerFreq = switch (mat) {
            case STONE -> 620.0f * pitchMod;
            case WOOD -> 280.0f * pitchMod;
            case SAND -> 440.0f * pitchMod;
            case GRAVEL -> 360.0f * pitchMod;
            case GLASS -> 1800.0f * pitchMod;
            case PLANT -> 520.0f * pitchMod;
            case WATER -> 320.0f * pitchMod;
            case GRASS -> 300.0f * pitchMod;
        };

        for (int i = 0; i < durationSamples; i++) {
            float t = (float) i / SAMPLE_RATE;
            float env = (float) Math.exp(-t * 45.0f);
            float noise = (RNG.nextFloat() * 2.0f - 1.0f);
            float tone = (float) Math.sin(2.0 * Math.PI * centerFreq * t);
            float s = (tone * 0.6f + noise * 0.4f) * env;
            samples[i] = (short) (s * 26000);
        }
        return toPcmBytes(samples);
    }

    private static byte[] generateStepSound(SoundMaterial mat, float pitchMod) {
        int durationSamples = (int) (SAMPLE_RATE * 0.06f);
        short[] samples = new short[durationSamples];

        float centerFreq = switch (mat) {
            case STONE -> 420.0f * pitchMod;
            case WOOD -> 180.0f * pitchMod;
            case SAND -> 300.0f * pitchMod;
            case GRAVEL -> 280.0f * pitchMod;
            case GLASS -> 800.0f * pitchMod;
            case PLANT -> 340.0f * pitchMod;
            case WATER -> 220.0f * pitchMod;
            case GRASS -> 200.0f * pitchMod;
        };

        for (int i = 0; i < durationSamples; i++) {
            float t = (float) i / SAMPLE_RATE;
            float env = (float) Math.exp(-t * 60.0f);
            float noise = (RNG.nextFloat() * 2.0f - 1.0f);
            float tone = (float) Math.sin(2.0 * Math.PI * centerFreq * t);
            float s = (tone * 0.4f + noise * 0.6f) * env;
            samples[i] = (short) (s * 18000);
        }
        return toPcmBytes(samples);
    }

    private static byte[] generateSplashSound(float pitchMod) {
        int durationSamples = (int) (SAMPLE_RATE * 0.22f);
        short[] samples = new short[durationSamples];

        for (int i = 0; i < durationSamples; i++) {
            float t = (float) i / SAMPLE_RATE;
            float env = (float) Math.exp(-t * 14.0f);
            float noise = (RNG.nextFloat() * 2.0f - 1.0f);
            float bubble = (float) Math.sin(2.0 * Math.PI * (280.0f * pitchMod + Math.sin(t * 70.0f) * 80.0f) * t);
            float s = (noise * 0.5f + bubble * 0.5f) * env;
            samples[i] = (short) (s * 22000);
        }
        return toPcmBytes(samples);
    }

    private static byte[] generateClickSound() {
        int durationSamples = (int) (SAMPLE_RATE * 0.02f);
        short[] samples = new short[durationSamples];

        for (int i = 0; i < durationSamples; i++) {
            float t = (float) i / SAMPLE_RATE;
            float env = (float) Math.exp(-t * 180.0f);
            float tone = (float) Math.sin(2.0 * Math.PI * 1350.0f * t);
            samples[i] = (short) (tone * env * 22000);
        }
        return toPcmBytes(samples);
    }

    private static byte[] generateCraftSound() {
        int durationSamples = (int) (SAMPLE_RATE * 0.18f);
        short[] samples = new short[durationSamples];

        for (int i = 0; i < durationSamples; i++) {
            float t = (float) i / SAMPLE_RATE;
            float env = (float) Math.exp(-t * 18.0f);
            float tone1 = (float) Math.sin(2.0 * Math.PI * 880.0f * t);
            float tone2 = (float) Math.sin(2.0 * Math.PI * 1320.0f * t);
            float s = (tone1 * 0.5f + tone2 * 0.5f) * env;
            samples[i] = (short) (s * 25000);
        }
        return toPcmBytes(samples);
    }

    private static byte[] toPcmBytes(short[] samples) {
        byte[] bytes = new byte[samples.length * 2];
        for (int i = 0; i < samples.length; i++) {
            short s = samples[i];
            bytes[i * 2] = (byte) (s & 0xFF);
            bytes[i * 2 + 1] = (byte) ((s >> 8) & 0xFF);
        }
        return bytes;
    }

    public static synchronized void cleanup() {
        if (audioExecutor != null) {
            audioExecutor.shutdownNow();
            audioExecutor = null;
        }
        initialized = false;
    }
}
