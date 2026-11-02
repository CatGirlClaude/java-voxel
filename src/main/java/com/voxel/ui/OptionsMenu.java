package com.voxel.ui;

import com.voxel.config.DebugHudConfig;
import com.voxel.config.GameSettings;
import com.voxel.entity.Player;
import com.voxel.i18n.I18n;
import com.voxel.util.Logger;
import com.voxel.world.World;
import com.voxel.world.WorldSaveManager;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;

public class OptionsMenu {

    public enum Source {
        MAIN_MENU,
        PAUSE_MENU
    }

    public enum Tab {
        GRAPHICS("options.tab.graphics"),
        CONTROLS("options.tab.controls"),
        DEBUG_HUD("options.tab.debughud"),
        OVERRIDES("options.tab.overrides"),
        SYSTEM("options.tab.system"),
        KEYBINDS("options.tab.keybinds");

        public final String titleKey;
        Tab(String titleKey) {
            this.titleKey = titleKey;
        }

        public String getTitle() {
            String val = I18n.t(titleKey);
            return val.startsWith("options.tab.") ? name() : val;
        }
    }

    private boolean open = false;
    private Source returnSource = Source.MAIN_MENU;
    private Tab currentTab = Tab.GRAPHICS;

    private String statusMessage = "";
    private float statusTimer = 0.0f;
    private long openCooldownUntil = 0;
    private float hudScroll = 0.0f;

    public enum KeyAction {
        FORWARD("Forward:"),
        BACK("Back:"),
        LEFT("Left:"),
        RIGHT("Right:"),
        JUMP("Jump / Up:"),
        CROUCH("Crouch / Down:"),
        SPRINT("Sprint:"),
        INVENTORY("Inventory / Creative:"),
        CHAT("Open Chat:"),
        COMMAND("Command (/):"),
        NOISE_MAP("Biome Map [M]:"),
        DEBUG("Debug HUD [F3]:"),
        TOGGLE_HUD("Toggle HUD [F1]:"),
        PAUSE("Pause [ESC]:"),
        SLOT_1("Hotbar Slot 1:"),
        SLOT_2("Hotbar Slot 2:"),
        SLOT_3("Hotbar Slot 3:"),
        SLOT_4("Hotbar Slot 4:"),
        SLOT_5("Hotbar Slot 5:"),
        SLOT_6("Hotbar Slot 6:"),
        SLOT_7("Hotbar Slot 7:"),
        SLOT_8("Hotbar Slot 8:"),
        SLOT_9("Hotbar Slot 9:");

        public final String label;
        KeyAction(String label) { this.label = label; }
    }

    private KeyAction rebindingAction = null;
    private final float[] tabScrolls = new float[Tab.values().length];

    public KeyAction getRebindingAction() {
        return rebindingAction;
    }

    public void setRebindingAction(KeyAction action) {
        this.rebindingAction = action;
    }

    public int getBoundKey(KeyAction action) {
        return switch (action) {
            case FORWARD -> GameSettings.keyForward;
            case BACK -> GameSettings.keyBack;
            case LEFT -> GameSettings.keyLeft;
            case RIGHT -> GameSettings.keyRight;
            case JUMP -> GameSettings.keyJump;
            case CROUCH -> GameSettings.keyCrouch;
            case SPRINT -> GameSettings.keySprint;
            case INVENTORY -> GameSettings.keyInventory;
            case CHAT -> GameSettings.keyChat;
            case COMMAND -> GameSettings.keyCommand;
            case NOISE_MAP -> GameSettings.keyNoiseMap;
            case DEBUG -> GameSettings.keyDebug;
            case TOGGLE_HUD -> GameSettings.keyToggleHud;
            case PAUSE -> GameSettings.keyPause;
            case SLOT_1 -> GameSettings.keyHotbar[0];
            case SLOT_2 -> GameSettings.keyHotbar[1];
            case SLOT_3 -> GameSettings.keyHotbar[2];
            case SLOT_4 -> GameSettings.keyHotbar[3];
            case SLOT_5 -> GameSettings.keyHotbar[4];
            case SLOT_6 -> GameSettings.keyHotbar[5];
            case SLOT_7 -> GameSettings.keyHotbar[6];
            case SLOT_8 -> GameSettings.keyHotbar[7];
            case SLOT_9 -> GameSettings.keyHotbar[8];
        };
    }

    public void setBoundKey(KeyAction action, int key) {
        switch (action) {
            case FORWARD -> GameSettings.keyForward = key;
            case BACK -> GameSettings.keyBack = key;
            case LEFT -> GameSettings.keyLeft = key;
            case RIGHT -> GameSettings.keyRight = key;
            case JUMP -> GameSettings.keyJump = key;
            case CROUCH -> GameSettings.keyCrouch = key;
            case SPRINT -> GameSettings.keySprint = key;
            case INVENTORY -> GameSettings.keyInventory = key;
            case CHAT -> GameSettings.keyChat = key;
            case COMMAND -> GameSettings.keyCommand = key;
            case NOISE_MAP -> GameSettings.keyNoiseMap = key;
            case DEBUG -> GameSettings.keyDebug = key;
            case TOGGLE_HUD -> GameSettings.keyToggleHud = key;
            case PAUSE -> GameSettings.keyPause = key;
            case SLOT_1 -> GameSettings.keyHotbar[0] = key;
            case SLOT_2 -> GameSettings.keyHotbar[1] = key;
            case SLOT_3 -> GameSettings.keyHotbar[2] = key;
            case SLOT_4 -> GameSettings.keyHotbar[3] = key;
            case SLOT_5 -> GameSettings.keyHotbar[4] = key;
            case SLOT_6 -> GameSettings.keyHotbar[5] = key;
            case SLOT_7 -> GameSettings.keyHotbar[6] = key;
            case SLOT_8 -> GameSettings.keyHotbar[7] = key;
            case SLOT_9 -> GameSettings.keyHotbar[8] = key;
        }
    }

    public float getMaxScroll(float listH, Tab tab) {
        float totalH = switch (tab) {
            case GRAPHICS -> 8 * 38.0f + 20.0f;
            case CONTROLS -> 5 * 38.0f + 20.0f;
            case DEBUG_HUD -> DebugHudConfig.getItems().size() * 34.0f + 36.0f;
            case OVERRIDES -> 6 * 38.0f + 20.0f;
            case SYSTEM -> 4 * 36.0f + 160.0f;
            case KEYBINDS -> 11 * 26.0f + 56.0f;
        };
        return Math.max(0.0f, totalH - listH);
    }

    public enum ActiveSlider {
        NONE,
        RENDER_DISTANCE,
        FOV,
        BRIGHTNESS,
        SENSITIVITY,
        HOLD_DELAY,
        MASTER_VOLUME
    }
    private ActiveSlider activeSlider = ActiveSlider.NONE;

    public float getWinW(int screenW) {
        return Math.min(840.0f, Math.max(580.0f, screenW - 32.0f));
    }

    public float getWinH(int screenH) {
        return Math.min(540.0f, Math.max(340.0f, screenH - 24.0f));
    }

    public void onResize(int screenW, int screenH) {
        float winH = getWinH(screenH);
        float listH = winH - 180.0f;
        int count = DebugHudConfig.getItems().size();
        float maxScroll = Math.max(0.0f, count * 36.0f - listH);
        hudScroll = Math.max(0.0f, Math.min(maxScroll, hudScroll));
    }

    // Available render distances
    private static final int[] RENDER_DISTANCES = {2, 4, 6, 8, 10, 12, 14, 16};
    // Available max FPS limits
    private static final int[] FPS_LIMITS = {0, 30, 60, 90, 120, 144, 240};
    // Available fog modes
    private static final String[] FOG_MODES = {"WYLACZONA", "BLISKA", "NORMALNA", "DALEKA"};
    // Log levels
    private static final String[] LOG_LEVELS = {"INFO", "DEBUG", "TRACE"};
    // Time presets (0.30=Day, 0.50=Noon, 0.77=Sunset, 0.0=Night)
    private static final float[] TIME_PRESETS = {0.30f, 0.50f, 0.77f, 0.0f};
    private static final String[] TIME_PRESET_NAMES = {"07:12 (Morning)", "12:00 (Noon)", "18:28 (Sunset)", "00:00 (Night)"};

    public OptionsMenu() {}

    public boolean isOpen() {
        return open;
    }

    public void setOpen(boolean open, Source source) {
        this.open = open;
        this.returnSource = source;
        if (open) {
            this.statusMessage = "";
            this.openCooldownUntil = System.currentTimeMillis() + 250;
        }
    }

    public void resetCooldown() {
        this.openCooldownUntil = 0;
    }

    public Source getReturnSource() {
        return returnSource;
    }

    public void update(float dt) {
        if (statusTimer > 0.0f) {
            statusTimer -= dt;
            if (statusTimer <= 0.0f) statusMessage = "";
        }
    }

    public void showStatus(String msg) {
        this.statusMessage = msg;
        this.statusTimer = 3.5f;
    }

    public boolean handleKey(int key, World world, Player player) {
        if (!open) return false;

        if (rebindingAction != null) {
            if (key == GLFW_KEY_ESCAPE) {
                rebindingAction = null;
                showStatus("Keybinding cancelled");
                return true;
            }
            setBoundKey(rebindingAction, key);
            String kName = GameSettings.getKeyName(key);
            showStatus("Bound " + rebindingAction.label + " " + kName);
            rebindingAction = null;
            GameSettings.save();
            return true;
        }

        if (key == GLFW_KEY_ESCAPE) {
            saveAndClose(world);
            return true;
        }
        Tab[] tabs = Tab.values();
        if (key >= GLFW_KEY_1 && key < GLFW_KEY_1 + tabs.length) {
            currentTab = tabs[key - GLFW_KEY_1];
            return true;
        }

        return false;
    }

    public boolean handleScroll(double yoffset) {
        if (!open) return false;
        if (currentTab == Tab.DEBUG_HUD) {
            List<DebugHudConfig.HudItem> items = DebugHudConfig.getItems();
            float rowH = 34.0f;
            float totalH = items.size() * rowH;
            float listH = 330.0f;
            float maxScroll = Math.max(0.0f, totalH - listH);
            hudScroll = Math.max(0.0f, Math.min(maxScroll, hudScroll - (float) yoffset * 28.0f));
            tabScrolls[Tab.DEBUG_HUD.ordinal()] = hudScroll;
            return true;
        }
        float listH = 330.0f;
        float maxScroll = getMaxScroll(listH, currentTab);
        if (maxScroll > 0.0f) {
            int idx = currentTab.ordinal();
            tabScrolls[idx] = Math.max(0.0f, Math.min(maxScroll, tabScrolls[idx] - (float) yoffset * 26.0f));
            return true;
        }
        return false;
    }

    public boolean handleMouseClick(double mouseX, double mouseY, int button, int screenW, int screenH,
                                    World world, Player player) {
        if (!open || button != GLFW_MOUSE_BUTTON_LEFT) return false;
        if (System.currentTimeMillis() < openCooldownUntil) return false;

        float mx = (float) mouseX;
        float my = (float) mouseY;

        float winW = getWinW(screenW);
        float winH = getWinH(screenH);
        float winX = (screenW - winW) * 0.5f;
        float winY = (screenH - winH) * 0.5f;

        // 1. Tab Bar Buttons with spacing
        Tab[] tabs = Tab.values();
        float tabSpacing = 4.0f;
        float tabW = (winW - 32.0f - (tabs.length - 1) * tabSpacing) / tabs.length;
        float tabH = 32.0f;
        float tabY = winY + 54.0f;

        for (int i = 0; i < tabs.length; i++) {
            float tx = winX + 16.0f + i * (tabW + tabSpacing);
            if (mx >= tx && mx <= tx + tabW && my >= tabY && my <= tabY + tabH) {
                currentTab = tabs[i];
                activeSlider = ActiveSlider.NONE;
                return true;
            }
        }

        // 2. Tab Content Interactions
        float contentY = tabY + tabH + 16.0f;
        if (currentTab == Tab.GRAPHICS) {
            handleGraphicsClick(mx, my, winX, contentY, winW, world);
        } else if (currentTab == Tab.CONTROLS) {
            handleControlsClick(mx, my, winX, contentY, winW);
        } else if (currentTab == Tab.DEBUG_HUD) {
            handleDebugHudClick(mx, my, winX, contentY, winW);
        } else if (currentTab == Tab.OVERRIDES) {
            handleOverridesClick(mx, my, winX, contentY, winW, world, player);
        } else if (currentTab == Tab.SYSTEM) {
            handleSystemClick(mx, my, winX, contentY, winW, world, player);
        } else if (currentTab == Tab.KEYBINDS) {
            handleKeybindsClick(mx, my, winX, contentY, winW);
        }
        if (activeSlider == ActiveSlider.NONE) {
            GameSettings.save();
        }

        // 3. Bottom Action Buttons: [ DONE / ESC ]  [ RESET DEFAULTS ]
        float bottomY = winY + winH - 46.0f;
        float btnW = 200.0f;
        float btnH = 34.0f;
        float btnSaveX = winX + winW - btnW - 20.0f;
        float btnDefX = winX + 20.0f;

        if (mx >= btnSaveX && mx <= btnSaveX + btnW && my >= bottomY && my <= bottomY + btnH) {
            activeSlider = ActiveSlider.NONE;
            saveAndClose(world);
            return true;
        }

        if (mx >= btnDefX && mx <= btnDefX + btnW && my >= bottomY && my <= bottomY + btnH) {
            activeSlider = ActiveSlider.NONE;
            GameSettings.resetToDefaults();
            if (world != null) {
                world.setRenderDistance(GameSettings.renderDistance);
                if (player != null) {
                    WorldSaveManager.applyGameruleOverrides(world, player);
                }
            }
            GameSettings.save();
            showStatus(I18n.t("options.defaults"));
            return true;
        }

        return true;
    }

    public boolean handleMouseMove(double mouseX, double mouseY, int screenW, int screenH, World world) {
        if (!open || activeSlider == ActiveSlider.NONE) return false;

        float winW = getWinW(screenW);
        float winX = (screenW - winW) * 0.5f;
        float btnW = 360.0f;
        float btnX = winX + (winW - btnW) * 0.5f;
        float mx = (float) mouseX;
        float progress = Math.max(0.0f, Math.min(1.0f, (mx - btnX) / btnW));

        switch (activeSlider) {
            case RENDER_DISTANCE -> {
                int dist = 2 + Math.round(progress * 14.0f);
                dist = Math.max(2, Math.min(16, dist));
                GameSettings.renderDistance = dist;
                if (world != null) {
                    world.setRenderDistance(dist);
                }
            }
            case FOV -> {
                float fov = 60.0f + progress * 50.0f;
                GameSettings.fov = Math.round(fov);
            }
            case BRIGHTNESS -> {
                float b = 0.50f + progress * 1.0f;
                GameSettings.brightness = Math.round(b * 100.0f) / 100.0f;
            }
            case SENSITIVITY -> {
                float s = 0.04f + progress * 0.36f;
                GameSettings.mouseSensitivity = Math.round(s * 100.0f) / 100.0f;
            }
            case HOLD_DELAY -> {
                float h = 0.05f + progress * 0.40f;
                GameSettings.mouseHoldDelay = Math.round(h * 100.0f) / 100.0f;
            }
            case MASTER_VOLUME -> {
                int vol = Math.round(progress * 100.0f);
                GameSettings.masterVolume = Math.max(0, Math.min(100, vol));
            }
            default -> {}
        }
        return true;
    }

    public void handleMouseRelease() {
        if (activeSlider != ActiveSlider.NONE) {
            activeSlider = ActiveSlider.NONE;
            GameSettings.save();
        }
    }

    private void handleGraphicsClick(float mx, float my, float winX, float startY, float winW, World world) {
        float btnW = 360.0f;
        float btnH = 30.0f;
        float btnX = winX + (winW - btnW) * 0.5f;
        float stepY = 38.0f;

        // Row 0: Render Distance Slider
        float y0 = startY;
        if (mx >= btnX && mx <= btnX + btnW && my >= y0 && my <= y0 + btnH) {
            activeSlider = ActiveSlider.RENDER_DISTANCE;
            float progress = Math.max(0.0f, Math.min(1.0f, (mx - btnX) / btnW));
            int dist = 2 + Math.round(progress * 14.0f);
            GameSettings.renderDistance = Math.max(2, Math.min(16, dist));
            if (world != null) {
                world.setRenderDistance(GameSettings.renderDistance);
            }
            return;
        }

        // Row 1: FOV Slider
        float y1 = y0 + stepY;
        if (mx >= btnX && mx <= btnX + btnW && my >= y1 && my <= y1 + btnH) {
            activeSlider = ActiveSlider.FOV;
            float progress = Math.max(0.0f, Math.min(1.0f, (mx - btnX) / btnW));
            GameSettings.fov = Math.round(60.0f + progress * 50.0f);
            return;
        }

        // Row 2: V-Sync
        float y2 = y1 + stepY;
        if (mx >= btnX && mx <= btnX + btnW && my >= y2 && my <= y2 + btnH) {
            GameSettings.vsync = !GameSettings.vsync;
            glfwSwapInterval(GameSettings.vsync ? 1 : 0);
            return;
        }

        // Row 3: FPS Limit
        float y3 = y2 + stepY;
        if (mx >= btnX && mx <= btnX + btnW && my >= y3 && my <= y3 + btnH) {
            int curIdx = 0;
            for (int i = 0; i < FPS_LIMITS.length; i++) {
                if (FPS_LIMITS[i] == GameSettings.maxFps) { curIdx = i; break; }
            }
            boolean isLeft = mx < btnX + btnW * 0.5f;
            curIdx = isLeft ? (curIdx - 1 + FPS_LIMITS.length) % FPS_LIMITS.length : (curIdx + 1) % FPS_LIMITS.length;
            GameSettings.maxFps = FPS_LIMITS[curIdx];
            return;
        }

        // Row 4: Fog Mode
        float y4 = y3 + stepY;
        if (mx >= btnX && mx <= btnX + btnW && my >= y4 && my <= y4 + btnH) {
            GameSettings.fogMode = (GameSettings.fogMode + 1) % FOG_MODES.length;
            return;
        }

        // Row 5: Brightness Slider
        float y5 = y4 + stepY;
        if (mx >= btnX && mx <= btnX + btnW && my >= y5 && my <= y5 + btnH) {
            activeSlider = ActiveSlider.BRIGHTNESS;
            float progress = Math.max(0.0f, Math.min(1.0f, (mx - btnX) / btnW));
            GameSettings.brightness = Math.round((0.50f + progress * 1.0f) * 100.0f) / 100.0f;
            return;
        }

        // Row 6: View Bobbing
        float y6 = y5 + stepY;
        if (mx >= btnX && mx <= btnX + btnW && my >= y6 && my <= y6 + btnH) {
            GameSettings.viewBobbing = !GameSettings.viewBobbing;
            return;
        }

        // Row 7: GUI Scale
        float y7 = y6 + stepY;
        if (mx >= btnX && mx <= btnX + btnW && my >= y7 && my <= y7 + btnH) {
            boolean isLeft = mx < btnX + btnW * 0.5f;
            GameSettings.guiScale = isLeft ? (GameSettings.guiScale - 1 + 5) % 5 : (GameSettings.guiScale + 1) % 5;
            return;
        }
    }

    private static final float[] EYE_PRESETS = { 1.40f, 1.48f, 1.54f, 1.62f, 1.70f };
    private static final float[] MOUSE_HOLD_PRESETS = { 0.10f, 0.15f, 0.18f, 0.22f, 0.25f, 0.30f, 0.35f, 0.40f };

    private void handleControlsClick(float mx, float my, float winX, float startY, float winW) {
        float btnW = 360.0f;
        float btnH = 30.0f;
        float btnX = winX + (winW - btnW) * 0.5f;
        float stepY = 38.0f;

        // Row 0: Mouse Sensitivity Slider
        float y0 = startY;
        if (mx >= btnX && mx <= btnX + btnW && my >= y0 && my <= y0 + btnH) {
            activeSlider = ActiveSlider.SENSITIVITY;
            float progress = Math.max(0.0f, Math.min(1.0f, (mx - btnX) / btnW));
            GameSettings.mouseSensitivity = Math.round((0.04f + progress * 0.36f) * 100.0f) / 100.0f;
            return;
        }

        // Row 1: Eye Height
        float y1 = y0 + stepY;
        if (mx >= btnX && mx <= btnX + btnW && my >= y1 && my <= y1 + btnH) {
            boolean isLeft = mx < btnX + btnW * 0.5f;
            int curIdx = 2; // default 1.54f
            float bestDiff = Float.MAX_VALUE;
            for (int i = 0; i < EYE_PRESETS.length; i++) {
                float diff = Math.abs(GameSettings.eyeHeight - EYE_PRESETS[i]);
                if (diff < bestDiff) {
                    bestDiff = diff;
                    curIdx = i;
                }
            }
            if (isLeft) {
                curIdx = Math.max(0, curIdx - 1);
            } else {
                curIdx = Math.min(EYE_PRESETS.length - 1, curIdx + 1);
            }
            GameSettings.eyeHeight = EYE_PRESETS[curIdx];
            return;
        }

        // Row 2: Mouse Hold Delay Slider
        float y2 = y1 + stepY;
        if (mx >= btnX && mx <= btnX + btnW && my >= y2 && my <= y2 + btnH) {
            activeSlider = ActiveSlider.HOLD_DELAY;
            float progress = Math.max(0.0f, Math.min(1.0f, (mx - btnX) / btnW));
            GameSettings.mouseHoldDelay = Math.round((0.05f + progress * 0.40f) * 100.0f) / 100.0f;
            return;
        }
        if (mx >= btnX && mx <= btnX + btnW && my >= y2 && my <= y2 + btnH) {
            boolean isLeft = mx < btnX + btnW * 0.5f;
            int curIdx = 3; // default 0.22f
            float bestDiff = Float.MAX_VALUE;
            for (int i = 0; i < MOUSE_HOLD_PRESETS.length; i++) {
                float diff = Math.abs(GameSettings.mouseHoldDelay - MOUSE_HOLD_PRESETS[i]);
                if (diff < bestDiff) {
                    bestDiff = diff;
                    curIdx = i;
                }
            }
            if (isLeft) {
                curIdx = Math.max(0, curIdx - 1);
            } else {
                curIdx = Math.min(MOUSE_HOLD_PRESETS.length - 1, curIdx + 1);
            }
            GameSettings.mouseHoldDelay = MOUSE_HOLD_PRESETS[curIdx];
            return;
        }

        // Row 3: Invert Mouse Y
        float y3 = y2 + stepY;
        if (mx >= btnX && mx <= btnX + btnW && my >= y3 && my <= y3 + btnH) {
            GameSettings.invertMouseY = !GameSettings.invertMouseY;
            return;
        }

        // Row 4: Fly by default
        float y4 = y3 + stepY;
        if (mx >= btnX && mx <= btnX + btnW && my >= y4 && my <= y4 + btnH) {
            GameSettings.flyByDefault = !GameSettings.flyByDefault;
            return;
        }

        // Row 5: Show FPS in HUD
        float y5 = y4 + stepY;
        if (mx >= btnX && mx <= btnX + btnW && my >= y5 && my <= y5 + btnH) {
            GameSettings.showFps = !GameSettings.showFps;
            return;
        }
    }

    private void handleDebugHudClick(float mx, float my, float winX, float startY, float winW) {
        List<DebugHudConfig.HudItem> items = DebugHudConfig.getItems();

        // Reset button
        float topBtnY = startY;
        float resetX = winX + winW - 200.0f;
        float resetW = 170.0f;
        float topBtnH = 26.0f;

        if (mx >= resetX && mx <= resetX + resetW && my >= topBtnY && my <= topBtnY + topBtnH) {
            DebugHudConfig.resetToDefaults();
            showStatus(I18n.t("hud.debughud.reset"));
            return;
        }

        // Scroll list metrics
        float listStartY = startY + 36.0f;
        float rowH = 34.0f;
        float listH = 330.0f;
        float totalH = items.size() * rowH;
        float maxScroll = Math.max(0.0f, totalH - listH);

        // Click on scrollbar track to jump scroll
        float sbX = winX + winW - 22.0f;
        if (maxScroll > 0.0f && mx >= sbX - 4.0f && mx <= sbX + 10.0f && my >= listStartY && my <= listStartY + listH) {
            hudScroll = Math.max(0.0f, Math.min(maxScroll, ((my - listStartY) / listH) * maxScroll));
            return;
        }

        for (int i = 0; i < items.size(); i++) {
            float ry = listStartY + i * rowH - hudScroll;
            if (ry < listStartY - 4.0f || ry + 28.0f > listStartY + listH + 4.0f) {
                continue;
            }

            // 1. Toggle Checkbox button
            float chkX = winX + 30.0f;
            float chkW = 30.0f;
            if (mx >= chkX && mx <= chkX + chkW && my >= ry && my <= ry + 28.0f) {
                DebugHudConfig.toggleEnabled(i);
                return;
            }

            // 2. Side Toggle button (< Left > / < Right >)
            float sideX = winX + winW - 250.0f;
            float sideW = 90.0f;
            if (mx >= sideX && mx <= sideX + sideW && my >= ry && my <= ry + 28.0f) {
                DebugHudConfig.toggleSide(i);
                return;
            }

            // 3. Move Up button
            float upX = winX + winW - 145.0f;
            float upW = 40.0f;
            if (mx >= upX && mx <= upX + upW && my >= ry && my <= ry + 28.0f) {
                DebugHudConfig.moveUp(i);
                return;
            }

            // 4. Move Down button
            float downX = winX + winW - 95.0f;
            float downW = 40.0f;
            if (mx >= downX && mx <= downX + downW && my >= ry && my <= ry + 28.0f) {
                DebugHudConfig.moveDown(i);
                return;
            }
        }
    }

    private void handleOverridesClick(float mx, float my, float winX, float startY, float winW,
                                      World world, Player player) {
        float btnW = 380.0f;
        float btnH = 32.0f;
        float btnX = winX + (winW - btnW) * 0.5f;
        float stepY = 40.0f;

        // Row 0: Master "Disable All Overrides" button
        float y0 = startY;
        if (mx >= btnX && mx <= btnX + btnW && my >= y0 && my <= y0 + btnH) {
            GameSettings.disableAllOverrides();
            if (world != null && player != null) {
                WorldSaveManager.applyGameruleOverrides(world, player);
            }
            showStatus(I18n.t("options.overrides.disable_all"));
            return;
        }

        // Row 1: Master Override switch
        float y1 = y0 + stepY + 4.0f;
        if (mx >= btnX && mx <= btnX + btnW && my >= y1 && my <= y1 + btnH) {
            GameSettings.enableOverrides = !GameSettings.enableOverrides;
            if (world != null && player != null) {
                WorldSaveManager.applyGameruleOverrides(world, player);
            }
            return;
        }

        // Row 2: Override Time
        float y2 = y1 + stepY;
        if (mx >= btnX && mx <= btnX + btnW && my >= y2 && my <= y2 + btnH) {
            boolean isLeft = mx < btnX + btnW * 0.5f;
            if (isLeft) {
                GameSettings.overrideTime = !GameSettings.overrideTime;
            } else {
                // Cycle time presets
                int cur = 0;
                for (int i = 0; i < TIME_PRESETS.length; i++) {
                    if (Math.abs(GameSettings.overrideTimeValue - TIME_PRESETS[i]) < 0.05f) {
                        cur = i;
                        break;
                    }
                }
                cur = (cur + 1) % TIME_PRESETS.length;
                GameSettings.overrideTimeValue = TIME_PRESETS[cur];
            }
            if (world != null && player != null) {
                WorldSaveManager.applyGameruleOverrides(world, player);
            }
            return;
        }

        // Row 3: Day/Night Cycle override
        float y3 = y2 + stepY;
        if (mx >= btnX && mx <= btnX + btnW && my >= y3 && my <= y3 + btnH) {
            if (!GameSettings.overrideDayCycle) {
                GameSettings.overrideDayCycle = true;
                GameSettings.overrideDayCycleValue = true; // pause
            } else if (GameSettings.overrideDayCycleValue) {
                GameSettings.overrideDayCycleValue = false; // running
            } else {
                GameSettings.overrideDayCycle = false; // default
            }
            if (world != null && player != null) {
                WorldSaveManager.applyGameruleOverrides(world, player);
            }
            return;
        }

        // Row 4: Fly override
        float y4 = y3 + stepY;
        if (mx >= btnX && mx <= btnX + btnW && my >= y4 && my <= y4 + btnH) {
            GameSettings.overrideFly = (GameSettings.overrideFly + 1) % 3;
            if (world != null && player != null) {
                WorldSaveManager.applyGameruleOverrides(world, player);
            }
            return;
        }

        // Row 5: Noclip override
        float y5 = y4 + stepY;
        if (mx >= btnX && mx <= btnX + btnW && my >= y5 && my <= y5 + btnH) {
            GameSettings.overrideNoclip = (GameSettings.overrideNoclip + 1) % 3;
            if (world != null && player != null) {
                WorldSaveManager.applyGameruleOverrides(world, player);
            }
            return;
        }
    }

    private void handleSystemClick(float mx, float my, float winX, float startY, float winW,
                                   World world, Player player) {
        float btnW = 360.0f;
        float btnH = 30.0f;
        float btnX = winX + (winW - btnW) * 0.5f;
        float stepY = 36.0f;

        // Row 0: Master Volume Slider
        float y0 = startY;
        if (mx >= btnX && mx <= btnX + btnW && my >= y0 && my <= y0 + btnH) {
            activeSlider = ActiveSlider.MASTER_VOLUME;
            float progress = Math.max(0.0f, Math.min(1.0f, (mx - btnX) / btnW));
            GameSettings.masterVolume = Math.round(progress * 100.0f);
            return;
        }

        // Row 1: Language toggle
        float y1 = y0 + stepY;
        if (mx >= btnX && mx <= btnX + btnW && my >= y1 && my <= y1 + btnH) {
            GameSettings.language = I18n.getNextLang(GameSettings.language);
            I18n.load(GameSettings.language);
            showStatus(I18n.t("options.language", I18n.getLanguageDisplayName(GameSettings.language)));
            return;
        }

        // Row 2: Save Format Toggle
        float y2 = y1 + stepY;
        if (mx >= btnX && mx <= btnX + btnW && my >= y2 && my <= y2 + btnH) {
            GameSettings.saveFormat = (GameSettings.saveFormat + 1) % 2;
            showStatus(I18n.t("menu.save_format") + ": " + GameSettings.getSaveFormatName());
            return;
        }

        // Row 3: Log Level Toggle
        float y3 = y2 + stepY;
        if (mx >= btnX && mx <= btnX + btnW && my >= y3 && my <= y3 + btnH) {
            int curIdx = 0;
            for (int i = 0; i < LOG_LEVELS.length; i++) {
                if (LOG_LEVELS[i].equalsIgnoreCase(GameSettings.logLevel)) { curIdx = i; break; }
            }
            curIdx = (curIdx + 1) % LOG_LEVELS.length;
            GameSettings.logLevel = LOG_LEVELS[curIdx];
            Logger.setMinLevel(Logger.Level.fromString(GameSettings.logLevel, Logger.Level.INFO));
            return;
        }

        // Row 4: Dump Diagnostics Report
        float y4 = y3 + stepY;
        if (mx >= btnX && mx <= btnX + btnW && my >= y4 && my <= y4 + btnH) {
            Map<String, String> ctx = new HashMap<>();
            if (world != null) {
                ctx.put("Seed", String.valueOf(world.getSeed()));
                ctx.put("Loaded Chunks", String.valueOf(world.getLoadedChunkCount()));
                ctx.put("Rendered Chunks", String.valueOf(world.getLastRenderedChunkCount()));
                ctx.put("Time of Day", String.format("%.2f", world.timeOfDay));
                ctx.put("Save Format", GameSettings.getSaveFormatName());
            }
            if (player != null) {
                ctx.put("Player Position", String.format("%.2f, %.2f, %.2f", player.position.x, player.position.y, player.position.z));
                ctx.put("Flying / Noclip", player.flying + " / " + player.noclip);
                ctx.put("Selected Block", player.getSelectedBlock().name);
            }
            Path path = Logger.dumpCrashReport("Manual Diagnostics Dump", null, ctx);
            showStatus("Dump saved: " + path.getFileName());
        }
    }

    private void handleKeybindsClick(float mx, float my, float winX, float startY, float winW) {
        float colW = 310.0f;
        float col1X = winX + 38.0f;
        float col2X = winX + winW - colW - 38.0f;
        float rowH = 26.0f;
        float scroll = tabScrolls[Tab.KEYBINDS.ordinal()];

        KeyAction[] leftKeys = {
            KeyAction.FORWARD, KeyAction.BACK, KeyAction.LEFT, KeyAction.RIGHT,
            KeyAction.JUMP, KeyAction.CROUCH, KeyAction.SPRINT,
            KeyAction.INVENTORY, KeyAction.CHAT, KeyAction.COMMAND, KeyAction.NOISE_MAP
        };

        KeyAction[] rightKeys = {
            KeyAction.DEBUG, KeyAction.TOGGLE_HUD,
            KeyAction.SLOT_1, KeyAction.SLOT_2, KeyAction.SLOT_3,
            KeyAction.SLOT_4, KeyAction.SLOT_5, KeyAction.SLOT_6,
            KeyAction.SLOT_7, KeyAction.SLOT_8, KeyAction.SLOT_9
        };

        for (int i = 0; i < leftKeys.length; i++) {
            float ry = startY + i * rowH - scroll;
            float kw = 120.0f;
            float kx = col1X + colW - kw;
            if (mx >= kx && mx <= kx + kw && my >= ry && my <= ry + 22.0f) {
                rebindingAction = (rebindingAction == leftKeys[i]) ? null : leftKeys[i];
                return;
            }
        }

        for (int i = 0; i < rightKeys.length; i++) {
            float ry = startY + i * rowH - scroll;
            float kw = 120.0f;
            float kx = col2X + colW - kw;
            if (mx >= kx && mx <= kx + kw && my >= ry && my <= ry + 22.0f) {
                rebindingAction = (rebindingAction == rightKeys[i]) ? null : rightKeys[i];
                return;
            }
        }

        float btnW = 280.0f;
        float btnH = 30.0f;
        float btnX = winX + (winW - btnW) * 0.5f;
        float btnY = startY + Math.max(leftKeys.length, rightKeys.length) * rowH + 10.0f - scroll;

        if (mx >= btnX && mx <= btnX + btnW && my >= btnY && my <= btnY + btnH) {
            GameSettings.keyForward = GLFW_KEY_W;
            GameSettings.keyBack = GLFW_KEY_S;
            GameSettings.keyLeft = GLFW_KEY_A;
            GameSettings.keyRight = GLFW_KEY_D;
            GameSettings.keyJump = GLFW_KEY_SPACE;
            GameSettings.keyCrouch = GLFW_KEY_LEFT_SHIFT;
            GameSettings.keySprint = GLFW_KEY_LEFT_CONTROL;
            GameSettings.keyInventory = GLFW_KEY_E;
            GameSettings.keyChat = GLFW_KEY_T;
            GameSettings.keyCommand = GLFW_KEY_SLASH;
            GameSettings.keyNoiseMap = GLFW_KEY_M;
            GameSettings.keyDebug = GLFW_KEY_F3;
            GameSettings.keyToggleHud = GLFW_KEY_F1;
            GameSettings.keyPause = GLFW_KEY_ESCAPE;
            for (int i = 0; i < GameSettings.keyHotbar.length; i++) {
                GameSettings.keyHotbar[i] = GLFW_KEY_1 + i;
            }
            rebindingAction = null;
            GameSettings.save();
            showStatus("Reset keybinds to defaults!");
        }
    }

    private void saveAndClose(World world) {
        GameSettings.save();
        if (world != null) {
            world.setRenderDistance(GameSettings.renderDistance);
        }
        open = false;
    }

    public void render(HudRenderer hud, int screenW, int screenH, double mouseX, double mouseY,
                       World world, Player player) {
        if (!open) return;

        glDisable(GL_DEPTH_TEST);
        glDisable(GL_CULL_FACE);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        Matrix4f ortho = new Matrix4f().ortho(0, screenW, screenH, 0, -1, 1);
        float mx = (float) mouseX;
        float my = (float) mouseY;

        // 1. Dim full screen background
        hud.getColorShader().bind();
        hud.getColorShader().setUniform("uMVP", ortho);
        hud.getColorShader().setUniform("uColor", new Vector4f(0.04f, 0.06f, 0.10f, 0.88f));
        hud.drawRect2D(0, 0, screenW, screenH);

        // 2. Main Window Box: dynamically responsive to screen size
        float winW = getWinW(screenW);
        float winH = getWinH(screenH);
        float winX = (screenW - winW) * 0.5f;
        float winY = (screenH - winH) * 0.5f;

        // Border glow & Container
        hud.getColorShader().setUniform("uColor", new Vector4f(0.25f, 0.55f, 0.90f, 0.95f));
        hud.drawRect2D(winX - 2, winY - 2, winW + 4, winH + 4);
        hud.getColorShader().setUniform("uColor", new Vector4f(0.08f, 0.11f, 0.17f, 0.98f));
        hud.drawRect2D(winX, winY, winW, winH);

        // Header
        hud.getColorShader().setUniform("uColor", new Vector4f(0.12f, 0.18f, 0.28f, 0.98f));
        hud.drawRect2D(winX, winY, winW, 46.0f);
        hud.getColorShader().unbind();

        hud.getTextRenderer().drawTextWithShadow(ortho, I18n.t("options.title"), winX + 18.0f, winY + 12.0f, 1.8f,
                new Vector4f(0.40f, 0.85f, 1.0f, 1.0f));

        // 3. Tab Buttons with clean spacing and no overlapping
        Tab[] tabs = Tab.values();
        float tabSpacing = 4.0f;
        float tabW = (winW - 32.0f - (tabs.length - 1) * tabSpacing) / tabs.length;
        float tabH = 32.0f;
        float tabY = winY + 54.0f;

        for (int i = 0; i < tabs.length; i++) {
            float tx = winX + 16.0f + i * (tabW + tabSpacing);
            boolean isCur = (tabs[i] == currentTab);
            boolean hovered = (mx >= tx && mx <= tx + tabW && my >= tabY && my <= tabY + tabH);

            hud.getColorShader().bind();
            if (isCur) {
                hud.getColorShader().setUniform("uColor", new Vector4f(0.20f, 0.55f, 0.90f, 0.95f));
            } else if (hovered) {
                hud.getColorShader().setUniform("uColor", new Vector4f(0.18f, 0.26f, 0.38f, 0.90f));
            } else {
                hud.getColorShader().setUniform("uColor", new Vector4f(0.12f, 0.16f, 0.24f, 0.85f));
            }
            hud.drawRect2D(tx, tabY, tabW, tabH);
            hud.getColorShader().unbind();

            Vector4f tabColor = isCur ? new Vector4f(1.0f, 1.0f, 1.0f, 1.0f) : (hovered ? new Vector4f(0.9f, 0.9f, 0.9f, 0.9f) : new Vector4f(0.65f, 0.75f, 0.85f, 0.85f));
            hud.getTextRenderer().drawTextCenteredInBox(ortho, "[" + (i + 1) + "] " + tabs[i].getTitle(), tx, tabY, tabW, tabH, 1.10f, 8.0f, tabColor);
        }

        // 4. Tab Content Area
        float contentY = tabY + tabH + 16.0f;
        if (currentTab == Tab.GRAPHICS) {
            renderGraphicsTab(hud, ortho, winX, contentY, winW, mx, my);
        } else if (currentTab == Tab.CONTROLS) {
            renderControlsTab(hud, ortho, winX, contentY, winW, mx, my);
        } else if (currentTab == Tab.DEBUG_HUD) {
            renderDebugHudTab(hud, ortho, winX, contentY, winW, mx, my);
        } else if (currentTab == Tab.OVERRIDES) {
            renderOverridesTab(hud, ortho, winX, contentY, winW, mx, my);
        } else if (currentTab == Tab.SYSTEM) {
            renderSystemTab(hud, ortho, winX, contentY, winW, mx, my);
        } else if (currentTab == Tab.KEYBINDS) {
            renderKeybindsTab(hud, ortho, winX, contentY, winW, mx, my);
        }

        // 5. Status message if any
        if (!statusMessage.isEmpty()) {
            float smW = hud.getTextRenderer().getTextWidth(statusMessage, 1.3f);
            hud.getTextRenderer().drawTextWithShadow(ortho, statusMessage, winX + (winW - smW) * 0.5f, winY + winH - 74.0f, 1.3f,
                    new Vector4f(0.40f, 0.95f, 0.55f, 1.0f));
        }

        // 6. Bottom Action Buttons: [ DONE / ESC ]  [ RESET DEFAULTS ]
        float bottomY = winY + winH - 46.0f;
        float btnW = 200.0f;
        float btnH = 34.0f;
        float btnSaveX = winX + winW - btnW - 20.0f;
        float btnDefX = winX + 20.0f;

        drawOptionButton(hud, ortho, btnSaveX, bottomY, btnW, btnH, I18n.t("options.done") + " [ESC]", mx, my,
                new Vector4f(0.18f, 0.55f, 0.35f, 0.95f), new Vector4f(0.25f, 0.70f, 0.45f, 1.0f));
        drawOptionButton(hud, ortho, btnDefX, bottomY, btnW, btnH, I18n.t("options.defaults"), mx, my,
                new Vector4f(0.45f, 0.20f, 0.20f, 0.90f), new Vector4f(0.65f, 0.25f, 0.25f, 1.0f));

        glEnable(GL_CULL_FACE);
        glEnable(GL_DEPTH_TEST);
    }

    private void renderGraphicsTab(HudRenderer hud, Matrix4f ortho, float winX, float startY, float winW, float mx, float my) {
        float btnW = 360.0f;
        float btnH = 30.0f;
        float btnX = winX + (winW - btnW) * 0.5f;
        float stepY = 38.0f;

        // Row 0: Render Distance (Slider: 2 to 16 chunks)
        float rDistProg = (GameSettings.renderDistance - 2) / 14.0f;
        int rDist = GameSettings.renderDistance;
        String rDistStr = I18n.t("options.render_distance", rDist + " Chunks (" + (rDist * 16) + "m)");
        drawSlider(hud, ortho, btnX, startY, btnW, btnH, rDistStr, rDistProg, mx, my);

        // Row 1: FOV (Slider: 60 to 110 deg)
        float fovProg = (GameSettings.fov - 60.0f) / 50.0f;
        int fovVal = Math.round(GameSettings.fov);
        String fovSuffix = (fovVal == 70) ? " (Normal)" : (fovVal == 110 ? " (Quake Pro)" : "");
        String fovStr = I18n.t("options.fov", fovVal + " deg") + fovSuffix;
        drawSlider(hud, ortho, btnX, startY + stepY, btnW, btnH, fovStr, fovProg, mx, my);

        // Row 2: V-Sync
        String vsyncStr = I18n.t("options.vsync", GameSettings.vsync ? I18n.t("options.on") : I18n.t("options.off"));
        drawOptionButton(hud, ortho, btnX, startY + stepY * 2, btnW, btnH, vsyncStr, mx, my);

        // Row 3: FPS Limit
        String fpsVal = GameSettings.maxFps == 0 ? I18n.t("options.fps_unlimited") : (GameSettings.maxFps + " FPS");
        String fpsStr = "< " + I18n.t("options.max_fps", fpsVal) + " >";
        drawOptionButton(hud, ortho, btnX, startY + stepY * 3, btnW, btnH, fpsStr, mx, my);

        // Row 4: Fog
        String fogVal = switch (GameSettings.fogMode) {
            case 0 -> I18n.t("options.fog.off");
            case 1 -> I18n.t("options.fog.near");
            case 2 -> I18n.t("options.fog.normal");
            case 3 -> I18n.t("options.fog.far");
            default -> I18n.t("options.fog.normal");
        };
        String fogStr = "< " + I18n.t("options.fog", fogVal) + " >";
        drawOptionButton(hud, ortho, btnX, startY + stepY * 4, btnW, btnH, fogStr, mx, my);

        // Row 5: Brightness (Slider: 50% to 150%)
        float brightProg = (GameSettings.brightness - 0.50f) / 1.0f;
        int brightPercent = Math.round(GameSettings.brightness * 100.0f);
        String brightSuffix = (brightPercent == 100) ? " (Normal)" : (brightPercent >= 140 ? " (Bright)" : (brightPercent <= 60 ? " (Moody)" : ""));
        String brightStr = I18n.t("options.brightness", brightPercent + "%") + brightSuffix;
        drawSlider(hud, ortho, btnX, startY + stepY * 5, btnW, btnH, brightStr, brightProg, mx, my);

        // Row 6: View Bobbing
        String bobStr = I18n.t("options.view_bobbing", GameSettings.viewBobbing ? I18n.t("options.on") : I18n.t("options.off"));
        drawOptionButton(hud, ortho, btnX, startY + stepY * 6, btnW, btnH, bobStr, mx, my);

        // Row 7: GUI Scale
        String scaleName;
        if (GameSettings.guiScale == 0) {
            scaleName = I18n.t("options.gui_scale_auto");
        } else {
            scaleName = GameSettings.guiScale + "x";
        }
        String scaleStr = "< " + I18n.t("options.gui_scale", scaleName) + " >";
        drawOptionButton(hud, ortho, btnX, startY + stepY * 7, btnW, btnH, scaleStr, mx, my);
    }

    private void renderControlsTab(HudRenderer hud, Matrix4f ortho, float winX, float startY, float winW, float mx, float my) {
        float btnW = 360.0f;
        float btnH = 30.0f;
        float btnX = winX + (winW - btnW) * 0.5f;
        float stepY = 38.0f;

        // Row 0: Mouse Sensitivity (Slider: 0.04 to 0.40)
        float sensProg = (GameSettings.mouseSensitivity - 0.04f) / 0.36f;
        int sensPercent = Math.round(sensProg * 100.0f);
        String sensStr = I18n.t("options.sensitivity", String.format("%.2f (%d%%)", GameSettings.mouseSensitivity, sensPercent));
        drawSlider(hud, ortho, btnX, startY, btnW, btnH, sensStr, sensProg, mx, my);

        // Row 1: Eye Height
        String eyeStr = "< " + I18n.t("options.eye_height", String.format("%.2f m", GameSettings.eyeHeight)) + " >";
        drawOptionButton(hud, ortho, btnX, startY + stepY, btnW, btnH, eyeStr, mx, my);

        // Row 2: Mouse Hold Delay (Slider: 50ms to 450ms)
        float holdProg = (GameSettings.mouseHoldDelay - 0.05f) / 0.40f;
        int holdMs = Math.round(GameSettings.mouseHoldDelay * 1000.0f);
        String holdStr = I18n.t("options.mouse_hold_delay", holdMs + "ms");
        drawSlider(hud, ortho, btnX, startY + stepY * 2, btnW, btnH, holdStr, holdProg, mx, my);

        // Row 3: Invert Y
        String invStr = I18n.t("options.invert_y", GameSettings.invertMouseY ? I18n.t("options.on") : I18n.t("options.off"));
        drawOptionButton(hud, ortho, btnX, startY + stepY * 3, btnW, btnH, invStr, mx, my);

        // Row 4: Fly by default
        String flyStr = I18n.t("options.fly_default", GameSettings.flyByDefault ? I18n.t("options.on") : I18n.t("options.off"));
        drawOptionButton(hud, ortho, btnX, startY + stepY * 4, btnW, btnH, flyStr, mx, my);

        // Row 5: Show FPS in HUD
        String fpsStr = I18n.t("options.show_fps", GameSettings.showFps ? I18n.t("options.on") : I18n.t("options.off"));
        drawOptionButton(hud, ortho, btnX, startY + stepY * 5, btnW, btnH, fpsStr, mx, my);
    }

    private void renderDebugHudTab(HudRenderer hud, Matrix4f ortho, float winX, float startY, float winW, float mx, float my) {
        List<DebugHudConfig.HudItem> items = DebugHudConfig.getItems();

        // Header hint and Reset button
        float topBtnY = startY;
        float resetX = winX + winW - 200.0f;

        hud.getTextRenderer().drawTextWithShadow(ortho, I18n.t("hud.debughud.scroll_hint"), winX + 30.0f, topBtnY + 7.0f, 1.25f,
                new Vector4f(0.70f, 0.85f, 0.95f, 0.90f));
        drawOptionButton(hud, ortho, resetX, topBtnY, 170.0f, 26.0f, I18n.t("hud.debughud.reset"), mx, my,
                new Vector4f(0.35f, 0.22f, 0.22f, 0.90f), new Vector4f(0.55f, 0.28f, 0.28f, 1.0f));

        float listStartY = startY + 36.0f;
        float rowH = 34.0f;
        float listH = 330.0f;
        float totalH = items.size() * rowH;
        float maxScroll = Math.max(0.0f, totalH - listH);
        hudScroll = Math.max(0.0f, Math.min(maxScroll, hudScroll));

        for (int i = 0; i < items.size(); i++) {
            DebugHudConfig.HudItem it = items.get(i);
            float ry = listStartY + i * rowH - hudScroll;
            if (ry + 28.0f < listStartY || ry > listStartY + listH) {
                continue;
            }

            // Row background container
            hud.getColorShader().bind();
            hud.getColorShader().setUniform("uColor", new Vector4f(0.12f, 0.15f, 0.22f, 0.85f));
            hud.drawRect2D(winX + 30.0f, ry, winW - 68.0f, 28.0f);
            hud.getColorShader().unbind();

            // 1. Checkbox toggle
            float chkX = winX + 30.0f;
            drawOptionButton(hud, ortho, chkX, ry, 30.0f, 28.0f, it.enabled ? "[X]" : "[ ]", mx, my,
                    it.enabled ? new Vector4f(0.18f, 0.55f, 0.30f, 0.95f) : new Vector4f(0.25f, 0.28f, 0.35f, 0.90f),
                    new Vector4f(0.30f, 0.70f, 0.40f, 1.0f));

            // 2. Element Name
            String name = (i + 1) + ". " + it.getDisplayName();
            hud.getTextRenderer().drawTextWithShadow(ortho, name, winX + 68.0f, ry + 7.0f, 1.2f,
                    it.enabled ? new Vector4f(0.95f, 0.96f, 0.98f, 1.0f) : new Vector4f(0.55f, 0.60f, 0.70f, 0.80f));

            // 3. Side Toggle (< Left > / < Right >)
            float sideX = winX + winW - 250.0f;
            String sideStr = "< " + it.side.getDisplayName() + " >";
            drawOptionButton(hud, ortho, sideX, ry, 90.0f, 28.0f, sideStr, mx, my,
                    it.side == DebugHudConfig.Side.LEFT ? new Vector4f(0.20f, 0.45f, 0.65f, 0.90f) : new Vector4f(0.50f, 0.35f, 0.60f, 0.90f),
                    new Vector4f(0.28f, 0.60f, 0.85f, 1.0f));

            // 4. Move Up / Down
            float upX = winX + winW - 145.0f;
            drawOptionButton(hud, ortho, upX, ry, 40.0f, 28.0f, "^", mx, my);
            float downX = winX + winW - 95.0f;
            drawOptionButton(hud, ortho, downX, ry, 40.0f, 28.0f, "v", mx, my);
        }

        // Vertical Scrollbar
        if (maxScroll > 0.0f) {
            float sbX = winX + winW - 32.0f;
            float sbW = 6.0f;

            hud.getColorShader().bind();
            hud.getColorShader().setUniform("uColor", new Vector4f(0.10f, 0.13f, 0.18f, 0.85f));
            hud.drawRect2D(sbX, listStartY, sbW, listH);

            float thumbH = Math.max(26.0f, listH * (listH / totalH));
            float thumbY = listStartY + (hudScroll / maxScroll) * (listH - thumbH);
            boolean sbHover = (mx >= sbX - 4.0f && mx <= sbX + sbW + 4.0f && my >= listStartY && my <= listStartY + listH);
            hud.getColorShader().setUniform("uColor", sbHover ? new Vector4f(0.35f, 0.70f, 1.0f, 0.95f) : new Vector4f(0.24f, 0.45f, 0.72f, 0.85f));
            hud.drawRect2D(sbX, thumbY, sbW, thumbH);
            hud.getColorShader().unbind();
        }
    }

    private void renderOverridesTab(HudRenderer hud, Matrix4f ortho, float winX, float startY, float winW, float mx, float my) {
        float btnW = 420.0f;
        float btnH = 32.0f;
        float btnX = winX + (winW - btnW) * 0.5f;
        float stepY = 40.0f;

        // Row 0: Master Disable All Overrides
        drawOptionButton(hud, ortho, btnX, startY, btnW, btnH, I18n.t("options.overrides.disable_all"), mx, my,
                new Vector4f(0.60f, 0.22f, 0.22f, 0.95f), new Vector4f(0.80f, 0.30f, 0.30f, 1.0f));

        // Row 1: Master Overrides Toggle
        String masterStatus = GameSettings.enableOverrides ? I18n.t("options.overrides.forced_on") : I18n.t("options.overrides.forced_off");
        String masterStr = I18n.t("options.overrides.master", masterStatus);
        drawOptionButton(hud, ortho, btnX, startY + stepY + 4.0f, btnW, btnH, masterStr, mx, my,
                GameSettings.enableOverrides ? new Vector4f(0.20f, 0.55f, 0.35f, 0.95f) : new Vector4f(0.25f, 0.28f, 0.35f, 0.90f),
                new Vector4f(0.30f, 0.70f, 0.45f, 1.0f));

        // Row 2: Override Time
        String timePresetName = "12:00 (Noon)";
        for (int i = 0; i < TIME_PRESETS.length; i++) {
            if (Math.abs(GameSettings.overrideTimeValue - TIME_PRESETS[i]) < 0.05f) {
                timePresetName = TIME_PRESET_NAMES[i];
                break;
            }
        }
        String timeStatus = GameSettings.overrideTime ? I18n.t("options.overrides.forced_on") : I18n.t("options.overrides.default");
        String timeStr = "< " + I18n.t("options.overrides.time", timeStatus, timePresetName) + " >";
        drawOptionButton(hud, ortho, btnX, startY + stepY * 2 + 4.0f, btnW, btnH, timeStr, mx, my);

        // Row 3: Day Cycle override
        String cycleStatus = !GameSettings.overrideDayCycle ? I18n.t("options.overrides.default")
                : (GameSettings.overrideDayCycleValue ? "FORCE PAUSED" : "FORCE RUNNING");
        String cycleStr = "< " + I18n.t("options.overrides.daycycle", cycleStatus) + " >";
        drawOptionButton(hud, ortho, btnX, startY + stepY * 3 + 4.0f, btnW, btnH, cycleStr, mx, my);

        // Row 4: Fly override
        String flyStatus = (GameSettings.overrideFly == 0) ? I18n.t("options.overrides.default")
                : ((GameSettings.overrideFly == 1) ? I18n.t("options.overrides.forced_on") : I18n.t("options.overrides.forced_off"));
        String flyStr = "< " + I18n.t("options.overrides.fly", flyStatus) + " >";
        drawOptionButton(hud, ortho, btnX, startY + stepY * 4 + 4.0f, btnW, btnH, flyStr, mx, my);

        // Row 5: Noclip override
        String ncStatus = (GameSettings.overrideNoclip == 0) ? I18n.t("options.overrides.default")
                : ((GameSettings.overrideNoclip == 1) ? I18n.t("options.overrides.forced_on") : I18n.t("options.overrides.forced_off"));
        String ncStr = "< " + I18n.t("options.overrides.noclip", ncStatus) + " >";
        drawOptionButton(hud, ortho, btnX, startY + stepY * 5 + 4.0f, btnW, btnH, ncStr, mx, my);
    }

    private void renderSystemTab(HudRenderer hud, Matrix4f ortho, float winX, float startY, float winW, float mx, float my) {
        float btnW = 380.0f;
        float btnH = 30.0f;
        float btnX = winX + (winW - btnW) * 0.5f;
        float stepY = 36.0f;

        // Row 0: Master Volume Slider
        float volProg = Math.max(0.0f, Math.min(1.0f, GameSettings.masterVolume / 100.0f));
        String volStr = I18n.t("options.volume", GameSettings.masterVolume);
        drawSlider(hud, ortho, btnX, startY, btnW, btnH, volStr, volProg, mx, my);

        // Row 1: Language toggle
        String langName = I18n.getLanguageDisplayName(GameSettings.language);
        String langStr = "< " + I18n.t("options.language", langName) + " >";
        drawOptionButton(hud, ortho, btnX, startY + stepY, btnW, btnH, langStr, mx, my,
                new Vector4f(0.20f, 0.40f, 0.60f, 0.95f), new Vector4f(0.30f, 0.55f, 0.85f, 1.0f));

        // Row 2: Save Format Toggle
        String formatStr = "< " + I18n.t("menu.save_format") + ": " + GameSettings.getSaveFormatName() + " >";
        drawOptionButton(hud, ortho, btnX, startY + stepY * 2, btnW, btnH, formatStr, mx, my);

        // Row 3: Log Level Toggle
        String levelStr = "< " + I18n.t("options.log_level", GameSettings.logLevel) + " >";
        drawOptionButton(hud, ortho, btnX, startY + stepY * 3, btnW, btnH, levelStr, mx, my);

        // Row 4: Manual Crash Dump
        drawOptionButton(hud, ortho, btnX, startY + stepY * 4, btnW, btnH, I18n.t("options.dump_report"), mx, my);

        // Row 5: Live Log preview
        float logBoxY = startY + stepY * 5 + 8.0f;
        float logBoxW = winW - 50.0f;
        float logBoxH = 100.0f;
        float logBoxX = winX + 25.0f;

        hud.getColorShader().bind();
        hud.getColorShader().setUniform("uColor", new Vector4f(0.04f, 0.05f, 0.08f, 0.95f));
        hud.drawRect2D(logBoxX, logBoxY, logBoxW, logBoxH);
        hud.getColorShader().setUniform("uColor", new Vector4f(0.25f, 0.35f, 0.50f, 0.85f));
        hud.drawRect2D(logBoxX, logBoxY, logBoxW, 20.0f);
        hud.getColorShader().unbind();

        hud.getTextRenderer().drawTextWithShadow(ortho, "LOG PREVIEW (logs/latest.log):",
                logBoxX + 8.0f, logBoxY + 4.0f, 1.15f, new Vector4f(0.50f, 0.85f, 1.0f, 1.0f));

        List<String> recent = Logger.getRecentLogs(4);
        for (int i = 0; i < recent.size(); i++) {
            String line = recent.get(i);
            if (line.length() > 92) line = line.substring(0, 89) + "...";

            Vector4f color = new Vector4f(0.75f, 0.85f, 0.95f, 0.85f);
            if (line.contains("/ERROR]") || line.contains("/FATAL]")) {
                color = new Vector4f(1.0f, 0.40f, 0.40f, 1.0f);
            } else if (line.contains("/WARN]")) {
                color = new Vector4f(1.0f, 0.85f, 0.35f, 1.0f);
            }

            hud.getTextRenderer().drawTextWithShadow(ortho, line, logBoxX + 8.0f, logBoxY + 24.0f + i * 18.0f, 1.05f, color);
        }
    }

    private void renderKeybindsTab(HudRenderer hud, Matrix4f ortho, float winX, float startY, float winW, float mx, float my) {
        float colW = 310.0f;
        float col1X = winX + 38.0f;
        float col2X = winX + winW - colW - 38.0f;
        float rowH = 26.0f;
        float scroll = tabScrolls[Tab.KEYBINDS.ordinal()];

        KeyAction[] leftKeys = {
            KeyAction.FORWARD, KeyAction.BACK, KeyAction.LEFT, KeyAction.RIGHT,
            KeyAction.JUMP, KeyAction.CROUCH, KeyAction.SPRINT,
            KeyAction.INVENTORY, KeyAction.CHAT, KeyAction.COMMAND, KeyAction.NOISE_MAP
        };

        KeyAction[] rightKeys = {
            KeyAction.DEBUG, KeyAction.TOGGLE_HUD,
            KeyAction.SLOT_1, KeyAction.SLOT_2, KeyAction.SLOT_3,
            KeyAction.SLOT_4, KeyAction.SLOT_5, KeyAction.SLOT_6,
            KeyAction.SLOT_7, KeyAction.SLOT_8, KeyAction.SLOT_9
        };

        for (int i = 0; i < leftKeys.length; i++) {
            float ry = startY + i * rowH - scroll;
            drawKeyRow(hud, ortho, col1X, ry, colW, leftKeys[i], mx, my);
        }

        for (int i = 0; i < rightKeys.length; i++) {
            float ry = startY + i * rowH - scroll;
            drawKeyRow(hud, ortho, col2X, ry, colW, rightKeys[i], mx, my);
        }

        float btnW = 280.0f;
        float btnH = 30.0f;
        float btnX = winX + (winW - btnW) * 0.5f;
        float btnY = startY + Math.max(leftKeys.length, rightKeys.length) * rowH + 10.0f - scroll;
        drawOptionButton(hud, ortho, btnX, btnY, btnW, btnH, I18n.t("options.reset_keybinds"), mx, my);

        // Scrollbar if needed
        float listH = 330.0f;
        float maxScroll = getMaxScroll(listH, Tab.KEYBINDS);
        if (maxScroll > 0.0f) {
            float sbX = winX + winW - 32.0f;
            float sbW = 6.0f;
            float totalH = listH + maxScroll;
            float thumbH = Math.max(26.0f, listH * (listH / totalH));
            float thumbY = startY + (scroll / maxScroll) * (listH - thumbH);
            boolean sbHover = (mx >= sbX - 4.0f && mx <= sbX + sbW + 4.0f && my >= startY && my <= startY + listH);

            hud.getColorShader().bind();
            hud.getColorShader().setUniform("uColor", new Vector4f(0.10f, 0.13f, 0.18f, 0.85f));
            hud.drawRect2D(sbX, startY, sbW, listH);
            hud.getColorShader().setUniform("uColor", sbHover ? new Vector4f(0.35f, 0.70f, 1.0f, 0.95f) : new Vector4f(0.24f, 0.45f, 0.72f, 0.85f));
            hud.drawRect2D(sbX, thumbY, sbW, thumbH);
            hud.getColorShader().unbind();
        }
    }

    private void drawKeyRow(HudRenderer hud, Matrix4f ortho, float x, float y, float w, KeyAction action, float mx, float my) {
        hud.getTextRenderer().drawTextWithShadow(ortho, action.label, x, y + 4.0f, 1.2f, new Vector4f(0.80f, 0.88f, 0.95f, 0.90f));

        float kw = 120.0f;
        float kx = x + w - kw;
        float kh = 22.0f;
        boolean hover = (mx >= kx && mx <= kx + kw && my >= y && my <= y + kh);
        boolean rebinding = (rebindingAction == action);

        hud.getColorShader().bind();
        if (rebinding) {
            hud.getColorShader().setUniform("uColor", new Vector4f(1.0f, 0.70f, 0.20f, 1.0f));
            hud.drawRect2D(kx - 2, y - 2, kw + 4, kh + 4);
            hud.getColorShader().setUniform("uColor", new Vector4f(0.35f, 0.22f, 0.08f, 0.95f));
        } else if (hover) {
            hud.getColorShader().setUniform("uColor", new Vector4f(0.35f, 0.55f, 0.85f, 1.0f));
            hud.drawRect2D(kx - 1, y - 1, kw + 2, kh + 2);
            hud.getColorShader().setUniform("uColor", new Vector4f(0.25f, 0.35f, 0.50f, 0.95f));
        } else {
            hud.getColorShader().setUniform("uColor", new Vector4f(0.18f, 0.24f, 0.35f, 0.90f));
        }
        hud.drawRect2D(kx, y, kw, kh);
        hud.getColorShader().unbind();

        String keyText = rebinding ? "> PRESS KEY <" : GameSettings.getKeyName(getBoundKey(action));
        Vector4f textCol = rebinding
                ? new Vector4f(1.0f, 0.92f, 0.30f, 1.0f)
                : (hover ? new Vector4f(1.0f, 1.0f, 1.0f, 1.0f) : new Vector4f(1.0f, 0.90f, 0.35f, 1.0f));
        hud.getTextRenderer().drawTextCenteredInBox(ortho, keyText, kx, y, kw, kh, 1.15f, 4.0f, textCol);
    }

    private void drawSlider(HudRenderer hud, Matrix4f ortho, float x, float y, float w, float h,
                            String text, float progress, float mx, float my) {
        progress = Math.max(0.0f, Math.min(1.0f, progress));
        boolean hovered = (mx >= x && mx <= x + w && my >= y && my <= y + h);

        // 1. Background track
        hud.getColorShader().bind();
        hud.getColorShader().setUniform("uColor", new Vector4f(0.10f, 0.14f, 0.22f, 0.95f));
        hud.drawRect2D(x, y, w, h);

        // 2. Filled track portion
        float filledW = w * progress;
        hud.getColorShader().setUniform("uColor", hovered ? new Vector4f(0.20f, 0.45f, 0.75f, 0.70f) : new Vector4f(0.16f, 0.35f, 0.60f, 0.60f));
        hud.drawRect2D(x, y, filledW, h);

        // 3. Draggable thumb handle (12px wide)
        float thumbW = 12.0f;
        float thumbX = x + (w - thumbW) * progress;
        hud.getColorShader().setUniform("uColor", hovered ? new Vector4f(0.40f, 0.75f, 1.0f, 1.0f) : new Vector4f(0.28f, 0.55f, 0.85f, 0.95f));
        hud.drawRect2D(thumbX, y - 1, thumbW, h + 2);

        // 4. Border outline
        hud.getColorShader().setUniform("uColor", hovered ? new Vector4f(0.45f, 0.80f, 1.0f, 1.0f) : new Vector4f(0.25f, 0.35f, 0.50f, 0.85f));
        hud.drawRect2D(x, y, w, 1);
        hud.drawRect2D(x, y + h - 1, w, 1);
        hud.drawRect2D(x, y, 1, h);
        hud.drawRect2D(x + w - 1, y, 1, h);
        hud.getColorShader().unbind();

        // 5. Centered auto-fitting text
        Vector4f txtCol = hovered ? new Vector4f(1.0f, 1.0f, 1.0f, 1.0f) : new Vector4f(0.92f, 0.95f, 0.98f, 0.95f);
        hud.getTextRenderer().drawTextCenteredInBox(ortho, text, x, y, w, h, 1.25f, 16.0f, txtCol);
    }

    private void drawOptionButton(HudRenderer hud, Matrix4f ortho, float x, float y, float w, float h, String text,
                                  float mx, float my) {
        drawOptionButton(hud, ortho, x, y, w, h, text, mx, my,
                new Vector4f(0.16f, 0.22f, 0.32f, 0.92f), new Vector4f(0.24f, 0.45f, 0.72f, 0.98f));
    }

    private void drawOptionButton(HudRenderer hud, Matrix4f ortho, float x, float y, float w, float h, String text,
                                  float mx, float my, Vector4f baseCol, Vector4f hoverCol) {
        boolean hovered = (mx >= x && mx <= x + w && my >= y && my <= y + h);

        hud.getColorShader().bind();
        hud.getColorShader().setUniform("uColor", hovered ? hoverCol : baseCol);
        hud.drawRect2D(x, y, w, h);

        // 1px Border
        hud.getColorShader().setUniform("uColor", hovered ? new Vector4f(0.40f, 0.75f, 1.0f, 1.0f) : new Vector4f(0.30f, 0.40f, 0.55f, 0.85f));
        hud.drawRect2D(x, y, w, 1);
        hud.drawRect2D(x, y + h - 1, w, 1);
        hud.drawRect2D(x, y, 1, h);
        hud.drawRect2D(x + w - 1, y, 1, h);
        hud.getColorShader().unbind();

        Vector4f txtCol = hovered ? new Vector4f(1.0f, 1.0f, 1.0f, 1.0f) : new Vector4f(0.90f, 0.94f, 0.98f, 0.90f);
        hud.getTextRenderer().drawTextCenteredInBox(ortho, text, x, y, w, h, 1.25f, 12.0f, txtCol);
    }

    private int getRenderDistanceIndex(int val) {
        for (int i = 0; i < RENDER_DISTANCES.length; i++) {
            if (RENDER_DISTANCES[i] == val) return i;
        }
        return 2;
    }
}
