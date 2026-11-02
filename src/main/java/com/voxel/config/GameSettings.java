package com.voxel.config;

import com.voxel.util.Logger;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import static org.lwjgl.glfw.GLFW.*;

public final class GameSettings {

    private static final Path CONFIG_DIR = com.voxel.util.GamePaths.resolve("config");
    private static final Path OPTIONS_FILE = CONFIG_DIR.resolve("options.properties");

    // Graphics & Video
    public static int renderDistance = 6;     // 2..16 chunks
    public static float fov = 72.0f;           // 60..110 degrees
    public static boolean vsync = true;
    public static int maxFps = 0;              // 0 = unlimited, 30, 60, 90, 120, 144, 240
    public static int fogMode = 2;             // 0=Wylaczona, 1=Bliska, 2=Normalna, 3=Daleka
    public static float brightness = 1.0f;     // 0.5 .. 1.5
    public static boolean viewBobbing = true;
    public static boolean showFps = true;
    public static int guiScale = 1;            // 0 = Auto, 1 = 1x (default), 2 = 2x, 3 = 3x, 4 = 4x

    // Controls & Mouse
    public static float mouseSensitivity = 0.12f; // 0.04 .. 0.40
    public static float mouseHoldDelay = 0.22f;   // 0.10 .. 0.40 s (Minecraft continuous placement/mining rate)
    public static boolean invertMouseY = false;
    public static boolean flyByDefault = false;
    public static float eyeHeight = 1.54f; // 1.40 .. 1.70 m (Minecraft standard)

    // Audio & System
    public static int masterVolume = 100;      // 0 .. 100%
    public static String logLevel = "INFO";
    public static int saveFormat = 0;          // 0 = Roznice (.dat / Block Diffs), 1 = Regiony (.mca / Region Storage)
    public static String language = "en_us";

    // World & Physics Simulation
    public static String waterPhysicsMode = "minecraft"; // "minecraft" or "terraria"
    public static boolean blockPhysicsEnabled = true;

    // Gamerule & Player State Overrides (Can be temporarily disabled via master switch)
    public static boolean enableOverrides = false;
    public static boolean overrideTime = false;
    public static float overrideTimeValue = 0.50f;     // 12:00
    public static boolean overrideDayCycle = false;
    public static boolean overrideDayCycleValue = false; // paused or running
    public static int overrideFly = 0;                 // 0 = Default (save), 1 = Forced ON, 2 = Forced OFF
    public static int overrideNoclip = 0;              // 0 = Default (save), 1 = Forced ON, 2 = Forced OFF

    // Keybindings
    public static int keyForward = GLFW_KEY_W;
    public static int keyBack = GLFW_KEY_S;
    public static int keyLeft = GLFW_KEY_A;
    public static int keyRight = GLFW_KEY_D;
    public static int keyJump = GLFW_KEY_SPACE;
    public static int keyCrouch = GLFW_KEY_LEFT_SHIFT;
    public static int keySprint = GLFW_KEY_LEFT_CONTROL;
    public static int keyInventory = GLFW_KEY_E;
    public static int keyChat = GLFW_KEY_T;
    public static int keyCommand = GLFW_KEY_SLASH;
    public static int keyNoiseMap = GLFW_KEY_M;
    public static int keyDebug = GLFW_KEY_F3;
    public static int keyToggleHud = GLFW_KEY_F1;
    public static int keyPause = GLFW_KEY_ESCAPE;

    public static int[] keyHotbar = {
        GLFW_KEY_1, GLFW_KEY_2, GLFW_KEY_3, GLFW_KEY_4, GLFW_KEY_5,
        GLFW_KEY_6, GLFW_KEY_7, GLFW_KEY_8, GLFW_KEY_9
    };

    public static int getHotbarSlotForKey(int key) {
        for (int i = 0; i < keyHotbar.length; i++) {
            if (keyHotbar[i] == key) return i;
        }
        if (key >= GLFW_KEY_1 && key <= GLFW_KEY_9) {
            return key - GLFW_KEY_1;
        }
        return -1;
    }

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(GameSettings::save, "GameSettings-Save-Hook"));
    }

    private GameSettings() {}

    public static void load() {
        if (!Files.exists(OPTIONS_FILE)) {
            Logger.info("Config", "Options file missing (%s). Creating defaults...", OPTIONS_FILE);
            save();
            return;
        }

        Properties props = new Properties();
        try (Reader reader = Files.newBufferedReader(OPTIONS_FILE, StandardCharsets.UTF_8)) {
            props.load(reader);

            renderDistance = getInt(props, "renderDistance", 6, 2, 16);
            fov = getFloat(props, "fov", 72.0f, 60.0f, 110.0f);
            vsync = getBool(props, "vsync", true);
            maxFps = getInt(props, "maxFps", 0, 0, 360);
            fogMode = getInt(props, "fogMode", 2, 0, 3);
            brightness = getFloat(props, "brightness", 1.0f, 0.5f, 1.5f);
            viewBobbing = getBool(props, "viewBobbing", true);
            showFps = getBool(props, "showFps", true);
            guiScale = getInt(props, "guiScale", 1, 0, 4);

            mouseSensitivity = getFloat(props, "mouseSensitivity", 0.12f, 0.02f, 0.60f);
            mouseHoldDelay = getFloat(props, "mouseHoldDelay", 0.22f, 0.08f, 0.50f);
            invertMouseY = getBool(props, "invertMouseY", false);
            flyByDefault = getBool(props, "flyByDefault", false);
            eyeHeight = getFloat(props, "eyeHeight", 1.54f, 1.30f, 1.85f);

            masterVolume = getInt(props, "masterVolume", 100, 0, 100);
            logLevel = props.getProperty("logLevel", "INFO").trim().toUpperCase();
            Logger.setMinLevel(Logger.Level.fromString(logLevel, Logger.Level.INFO));
            saveFormat = getInt(props, "saveFormat", 0, 0, 1);
            language = props.getProperty("language", "en_us").trim().toLowerCase();
            com.voxel.i18n.I18n.load(language);
            waterPhysicsMode = props.getProperty("waterPhysicsMode", "minecraft").trim().toLowerCase();
            blockPhysicsEnabled = getBool(props, "blockPhysicsEnabled", true);

            // Overrides
            enableOverrides = getBool(props, "enableOverrides", false);
            overrideTime = getBool(props, "overrideTime", false);
            overrideTimeValue = getFloat(props, "overrideTimeValue", 0.50f, 0.0f, 1.0f);
            overrideDayCycle = getBool(props, "overrideDayCycle", false);
            overrideDayCycleValue = getBool(props, "overrideDayCycleValue", false);
            overrideFly = getInt(props, "overrideFly", 0, 0, 2);
            overrideNoclip = getInt(props, "overrideNoclip", 0, 0, 2);

            // Debug HUD Layout
            String hudLayout = props.getProperty("debugHudLayout", "");
            DebugHudConfig.deserialize(hudLayout);

            keyForward = getInt(props, "keyForward", GLFW_KEY_W);
            keyBack = getInt(props, "keyBack", GLFW_KEY_S);
            keyLeft = getInt(props, "keyLeft", GLFW_KEY_A);
            keyRight = getInt(props, "keyRight", GLFW_KEY_D);
            keyJump = getInt(props, "keyJump", GLFW_KEY_SPACE);
            keyCrouch = getInt(props, "keyCrouch", GLFW_KEY_LEFT_SHIFT);
            keySprint = getInt(props, "keySprint", GLFW_KEY_LEFT_CONTROL);
            keyInventory = getInt(props, "keyInventory", GLFW_KEY_E);
            keyChat = getInt(props, "keyChat", GLFW_KEY_T);
            keyCommand = getInt(props, "keyCommand", GLFW_KEY_SLASH);
            keyNoiseMap = getInt(props, "keyNoiseMap", GLFW_KEY_M);
            keyDebug = getInt(props, "keyDebug", GLFW_KEY_F3);
            keyToggleHud = getInt(props, "keyToggleHud", GLFW_KEY_F1);
            keyPause = getInt(props, "keyPause", GLFW_KEY_ESCAPE);
            for (int i = 0; i < keyHotbar.length; i++) {
                keyHotbar[i] = getInt(props, "keyHotbar_" + i, GLFW_KEY_1 + i);
            }

            Logger.info("Config", "Loaded game options from: %s", OPTIONS_FILE);
        } catch (Exception e) {
            Logger.error("Config", "Error loading game options: %s", e.getMessage());
        }
    }

    public static void save() {
        Properties props = new Properties();

        props.setProperty("renderDistance", String.valueOf(renderDistance));
        props.setProperty("fov", String.format(java.util.Locale.ROOT, "%.1f", fov));
        props.setProperty("vsync", String.valueOf(vsync));
        props.setProperty("maxFps", String.valueOf(maxFps));
        props.setProperty("fogMode", String.valueOf(fogMode));
        props.setProperty("brightness", String.format(java.util.Locale.ROOT, "%.2f", brightness));
        props.setProperty("viewBobbing", String.valueOf(viewBobbing));
        props.setProperty("showFps", String.valueOf(showFps));
        props.setProperty("guiScale", String.valueOf(guiScale));

        props.setProperty("mouseSensitivity", String.format(java.util.Locale.ROOT, "%.3f", mouseSensitivity));
        props.setProperty("mouseHoldDelay", String.format(java.util.Locale.ROOT, "%.3f", mouseHoldDelay));
        props.setProperty("invertMouseY", String.valueOf(invertMouseY));
        props.setProperty("flyByDefault", String.valueOf(flyByDefault));
        props.setProperty("eyeHeight", String.format(java.util.Locale.ROOT, "%.2f", eyeHeight));

        props.setProperty("masterVolume", String.valueOf(masterVolume));
        props.setProperty("logLevel", logLevel);
        props.setProperty("saveFormat", String.valueOf(saveFormat));
        props.setProperty("language", language);
        props.setProperty("waterPhysicsMode", waterPhysicsMode);
        props.setProperty("blockPhysicsEnabled", String.valueOf(blockPhysicsEnabled));

        props.setProperty("enableOverrides", String.valueOf(enableOverrides));
        props.setProperty("overrideTime", String.valueOf(overrideTime));
        props.setProperty("overrideTimeValue", String.format(java.util.Locale.ROOT, "%.2f", overrideTimeValue));
        props.setProperty("overrideDayCycle", String.valueOf(overrideDayCycle));
        props.setProperty("overrideDayCycleValue", String.valueOf(overrideDayCycleValue));
        props.setProperty("overrideFly", String.valueOf(overrideFly));
        props.setProperty("overrideNoclip", String.valueOf(overrideNoclip));

        props.setProperty("debugHudLayout", DebugHudConfig.serialize());

        props.setProperty("keyForward", String.valueOf(keyForward));
        props.setProperty("keyBack", String.valueOf(keyBack));
        props.setProperty("keyLeft", String.valueOf(keyLeft));
        props.setProperty("keyRight", String.valueOf(keyRight));
        props.setProperty("keyJump", String.valueOf(keyJump));
        props.setProperty("keyCrouch", String.valueOf(keyCrouch));
        props.setProperty("keySprint", String.valueOf(keySprint));
        props.setProperty("keyInventory", String.valueOf(keyInventory));
        props.setProperty("keyChat", String.valueOf(keyChat));
        props.setProperty("keyCommand", String.valueOf(keyCommand));
        props.setProperty("keyNoiseMap", String.valueOf(keyNoiseMap));
        props.setProperty("keyDebug", String.valueOf(keyDebug));
        props.setProperty("keyToggleHud", String.valueOf(keyToggleHud));
        props.setProperty("keyPause", String.valueOf(keyPause));
        for (int i = 0; i < keyHotbar.length; i++) {
            props.setProperty("keyHotbar_" + i, String.valueOf(keyHotbar[i]));
        }

        try {
            if (!Files.exists(CONFIG_DIR)) {
                Files.createDirectories(CONFIG_DIR);
            }
            try (Writer writer = Files.newBufferedWriter(OPTIONS_FILE, StandardCharsets.UTF_8)) {
                props.store(writer, "Java Voxel 3D - Game Settings & Configuration");
            }
            Logger.debug("Config", "Saved game options to %s", OPTIONS_FILE);
        } catch (Exception e) {
            Logger.error("Config", "Error saving game options: %s", e.getMessage());
        }
    }

    public static float getEffectiveGuiScale(int windowWidth, int windowHeight) {
        if (guiScale > 0) {
            // Anti-softlock safety clamp: guarantee virtual screen resolution is at least 420x260
            // so menus and dialog buttons (e.g. Done/Back) never overflow out of viewport
            int maxAllowed = Math.max(1, Math.min(windowWidth / 420, windowHeight / 260));
            return (float) Math.min(guiScale, maxAllowed);
        }
        // Auto scale (Minecraft style): scales UI so virtual resolution is comfortably readable
        int scale = 1;
        while ((windowWidth / (scale + 1)) >= 600 && (windowHeight / (scale + 1)) >= 360) {
            scale++;
        }
        return (float) scale;
    }

    public static void resetToDefaults() {
        renderDistance = 6;
        fov = 72.0f;
        vsync = true;
        maxFps = 0;
        fogMode = 2;
        brightness = 1.0f;
        viewBobbing = true;
        showFps = true;
        guiScale = 1;

        mouseSensitivity = 0.12f;
        mouseHoldDelay = 0.22f;
        invertMouseY = false;
        flyByDefault = false;
        eyeHeight = 1.54f;

        masterVolume = 100;
        logLevel = "INFO";
        Logger.setMinLevel(Logger.Level.INFO);
        saveFormat = 0;
        language = "en_us";
        com.voxel.i18n.I18n.load(language);
        waterPhysicsMode = "minecraft";
        blockPhysicsEnabled = true;

        enableOverrides = false;
        overrideTime = false;
        overrideTimeValue = 0.50f;
        overrideDayCycle = false;
        overrideDayCycleValue = false;
        overrideFly = 0;
        overrideNoclip = 0;

        DebugHudConfig.resetToDefaults();

        keyForward = GLFW_KEY_W;
        keyBack = GLFW_KEY_S;
        keyLeft = GLFW_KEY_A;
        keyRight = GLFW_KEY_D;
        keyJump = GLFW_KEY_SPACE;
        keyCrouch = GLFW_KEY_LEFT_SHIFT;
        keySprint = GLFW_KEY_LEFT_CONTROL;
        keyInventory = GLFW_KEY_E;
        keyChat = GLFW_KEY_T;
        keyCommand = GLFW_KEY_SLASH;
        keyNoiseMap = GLFW_KEY_M;
        keyDebug = GLFW_KEY_F3;
        keyToggleHud = GLFW_KEY_F1;
        keyPause = GLFW_KEY_ESCAPE;
        for (int i = 0; i < keyHotbar.length; i++) {
            keyHotbar[i] = GLFW_KEY_1 + i;
        }

        save();
        Logger.info("Config", "Restored all options to default settings.");
    }

    public static void disableAllOverrides() {
        enableOverrides = false;
        overrideTime = false;
        overrideDayCycle = false;
        overrideFly = 0;
        overrideNoclip = 0;
        save();
        Logger.info("Config", "All gamerule overrides have been disabled.");
    }

    public static String getKeyName(int keycode) {
        String name = glfwGetKeyName(keycode, 0);
        if (name != null && !name.trim().isEmpty()) {
            return name.toUpperCase();
        }
        return switch (keycode) {
            case GLFW_KEY_SPACE -> "SPACJA";
            case GLFW_KEY_LEFT_SHIFT -> "L-SHIFT";
            case GLFW_KEY_RIGHT_SHIFT -> "P-SHIFT";
            case GLFW_KEY_LEFT_CONTROL -> "L-CTRL";
            case GLFW_KEY_RIGHT_CONTROL -> "P-CTRL";
            case GLFW_KEY_LEFT_ALT -> "L-ALT";
            case GLFW_KEY_RIGHT_ALT -> "P-ALT";
            case GLFW_KEY_TAB -> "TAB";
            case GLFW_KEY_ENTER -> "ENTER";
            case GLFW_KEY_ESCAPE -> "ESC";
            case GLFW_KEY_BACKSPACE -> "BACKSPACE";
            case GLFW_KEY_UP -> "STRZALKA GORA";
            case GLFW_KEY_DOWN -> "STRZALKA DOL";
            case GLFW_KEY_LEFT -> "STRZALKA LEWO";
            case GLFW_KEY_RIGHT -> "STRZALKA PRAWO";
            case GLFW_KEY_F1 -> "F1";
            case GLFW_KEY_F2 -> "F2";
            case GLFW_KEY_F3 -> "F3";
            case GLFW_KEY_F4 -> "F4";
            case GLFW_KEY_F5 -> "F5";
            case GLFW_KEY_F11 -> "F11";
            default -> "KEY_" + keycode;
        };
    }

    public static String getSaveFormatName() {
        return saveFormat == 1 ? com.voxel.i18n.I18n.t("menu.format_regions") : com.voxel.i18n.I18n.t("menu.format_diffs");
    }

    private static int getInt(Properties p, String key, int def, int min, int max) {
        try {
            String v = p.getProperty(key);
            if (v == null) return def;
            return Math.max(min, Math.min(max, Integer.parseInt(v.trim())));
        } catch (Exception e) {
            return def;
        }
    }

    private static int getInt(Properties p, String key, int def) {
        try {
            String v = p.getProperty(key);
            if (v == null) return def;
            return Integer.parseInt(v.trim());
        } catch (Exception e) {
            return def;
        }
    }

    private static float getFloat(Properties p, String key, float def, float min, float max) {
        try {
            String v = p.getProperty(key);
            if (v == null) return def;
            return Math.max(min, Math.min(max, Float.parseFloat(v.trim())));
        } catch (Exception e) {
            return def;
        }
    }

    private static boolean getBool(Properties p, String key, boolean def) {
        try {
            String v = p.getProperty(key);
            if (v == null) return def;
            return Boolean.parseBoolean(v.trim());
        } catch (Exception e) {
            return def;
        }
    }
}
