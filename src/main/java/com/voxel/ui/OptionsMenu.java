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

        float winW = 740.0f;
        float winH = 520.0f;
        float winX = (screenW - winW) * 0.5f;
        float winY = (screenH - winH) * 0.5f;

        // 1. Tab Bar Buttons
        Tab[] tabs = Tab.values();
        float tabW = (winW - 32.0f) / tabs.length;
        float tabH = 32.0f;
        float tabY = winY + 54.0f;

        for (int i = 0; i < tabs.length; i++) {
            float tx = winX + 16.0f + i * tabW;
            if (mx >= tx && mx <= tx + tabW - 4.0f && my >= tabY && my <= tabY + tabH) {
                currentTab = tabs[i];
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

        // 3. Bottom Action Buttons: [ DONE / ESC ]  [ RESET DEFAULTS ]
        float bottomY = winY + winH - 46.0f;
        float btnW = 200.0f;
        float btnH = 34.0f;
        float btnSaveX = winX + winW - btnW - 20.0f;
        float btnDefX = winX + 20.0f;

        if (mx >= btnSaveX && mx <= btnSaveX + btnW && my >= bottomY && my <= bottomY + btnH) {
            saveAndClose(world);
            return true;
        }

        if (mx >= btnDefX && mx <= btnDefX + btnW && my >= bottomY && my <= bottomY + btnH) {
            GameSettings.resetToDefaults();
            if (world != null) {
                world.setRenderDistance(GameSettings.renderDistance);
                if (player != null) {
                    WorldSaveManager.applyGameruleOverrides(world, player);
                }
            }
            showStatus(I18n.t("options.defaults"));
            return true;
        }

        return true;
    }

    private void handleGraphicsClick(float mx, float my, float winX, float startY, float winW, World world) {
        float btnW = 340.0f;
        float btnH = 30.0f;
        float btnX = winX + (winW - btnW) * 0.5f;
        float stepY = 38.0f;

        // Row 0: Render Distance
        float y0 = startY;
        if (mx >= btnX && mx <= btnX + btnW && my >= y0 && my <= y0 + btnH) {
            int curIdx = getRenderDistanceIndex(GameSettings.renderDistance);
            boolean isLeft = mx < btnX + btnW * 0.5f;
            curIdx = isLeft ? Math.max(0, curIdx - 1) : Math.min(RENDER_DISTANCES.length - 1, curIdx + 1);
            GameSettings.renderDistance = RENDER_DISTANCES[curIdx];
            if (world != null) {
                world.setRenderDistance(GameSettings.renderDistance);
            }
            return;
        }

        // Row 1: FOV
        float y1 = y0 + stepY;
        if (mx >= btnX && mx <= btnX + btnW && my >= y1 && my <= y1 + btnH) {
            boolean isLeft = mx < btnX + btnW * 0.5f;
            if (isLeft) {
                GameSettings.fov = Math.max(60.0f, GameSettings.fov - 5.0f);
            } else {
                GameSettings.fov = Math.min(110.0f, GameSettings.fov + 5.0f);
            }
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

        // Row 5: Brightness
        float y5 = y4 + stepY;
        if (mx >= btnX && mx <= btnX + btnW && my >= y5 && my <= y5 + btnH) {
            boolean isLeft = mx < btnX + btnW * 0.5f;
            if (isLeft) {
                GameSettings.brightness = Math.max(0.5f, Math.round((GameSettings.brightness - 0.1f) * 10.0f) / 10.0f);
            } else {
                GameSettings.brightness = Math.min(1.5f, Math.round((GameSettings.brightness + 0.1f) * 10.0f) / 10.0f);
            }
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
        float btnW = 340.0f;
        float btnH = 30.0f;
        float btnX = winX + (winW - btnW) * 0.5f;
        float stepY = 38.0f;

        // Row 0: Mouse Sensitivity
        float y0 = startY;
        if (mx >= btnX && mx <= btnX + btnW && my >= y0 && my <= y0 + btnH) {
            boolean isLeft = mx < btnX + btnW * 0.5f;
            if (isLeft) {
                GameSettings.mouseSensitivity = Math.max(0.04f, Math.round((GameSettings.mouseSensitivity - 0.02f) * 100.0f) / 100.0f);
            } else {
                GameSettings.mouseSensitivity = Math.min(0.40f, Math.round((GameSettings.mouseSensitivity + 0.02f) * 100.0f) / 100.0f);
            }
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

        // Row 2: Mouse Hold Delay (Minecraft continuous place/break rate)
        float y2 = y1 + stepY;
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

        // Row 0: Language toggle
        float y0 = startY;
        if (mx >= btnX && mx <= btnX + btnW && my >= y0 && my <= y0 + btnH) {
            GameSettings.language = I18n.getNextLang(GameSettings.language);
            I18n.load(GameSettings.language);
            showStatus(I18n.t("options.language", I18n.getLanguageDisplayName(GameSettings.language)));
            return;
        }

        // Row 1: Save Format Toggle
        float y1 = y0 + stepY;
        if (mx >= btnX && mx <= btnX + btnW && my >= y1 && my <= y1 + btnH) {
            GameSettings.saveFormat = (GameSettings.saveFormat + 1) % 2;
            showStatus(I18n.t("menu.save_format") + ": " + GameSettings.getSaveFormatName());
            return;
        }

        // Row 2: Log Level Toggle
        float y2 = y1 + stepY;
        if (mx >= btnX && mx <= btnX + btnW && my >= y2 && my <= y2 + btnH) {
            int curIdx = 0;
            for (int i = 0; i < LOG_LEVELS.length; i++) {
                if (LOG_LEVELS[i].equalsIgnoreCase(GameSettings.logLevel)) { curIdx = i; break; }
            }
            curIdx = (curIdx + 1) % LOG_LEVELS.length;
            GameSettings.logLevel = LOG_LEVELS[curIdx];
            Logger.setMinLevel(Logger.Level.fromString(GameSettings.logLevel, Logger.Level.INFO));
            return;
        }

        // Row 3: Dump Diagnostics Report
        float y3 = y2 + stepY;
        if (mx >= btnX && mx <= btnX + btnW && my >= y3 && my <= y3 + btnH) {
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
        float btnW = 280.0f;
        float btnH = 32.0f;
        float btnX = winX + (winW - btnW) * 0.5f;
        float btnY = startY + 230.0f;

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

        // 2. Main Window Box
        float winW = 740.0f;
        float winH = 520.0f;
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

        // 3. Tab Buttons
        Tab[] tabs = Tab.values();
        float tabW = (winW - 32.0f) / tabs.length;
        float tabH = 32.0f;
        float tabY = winY + 54.0f;

        for (int i = 0; i < tabs.length; i++) {
            float tx = winX + 16.0f + i * tabW;
            boolean isCur = (tabs[i] == currentTab);
            boolean hovered = (mx >= tx && mx <= tx + tabW - 4.0f && my >= tabY && my <= tabY + tabH);

            hud.getColorShader().bind();
            if (isCur) {
                hud.getColorShader().setUniform("uColor", new Vector4f(0.20f, 0.55f, 0.90f, 0.95f));
            } else if (hovered) {
                hud.getColorShader().setUniform("uColor", new Vector4f(0.18f, 0.26f, 0.38f, 0.90f));
            } else {
                hud.getColorShader().setUniform("uColor", new Vector4f(0.12f, 0.16f, 0.24f, 0.85f));
            }
            hud.drawRect2D(tx, tabY, tabW - 4.0f, tabH);
            hud.getColorShader().unbind();

            Vector4f tabColor = isCur ? new Vector4f(1.0f, 1.0f, 1.0f, 1.0f) : (hovered ? new Vector4f(0.9f, 0.9f, 0.9f, 0.9f) : new Vector4f(0.65f, 0.75f, 0.85f, 0.85f));
            hud.getTextRenderer().drawTextCenteredInBox(ortho, "[" + (i + 1) + "] " + tabs[i].getTitle(), tx, tabY, tabW - 4.0f, tabH, 1.10f, 6.0f, tabColor);
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

        // Render Distance
        int rDist = GameSettings.renderDistance;
        String rDistStr = "< " + I18n.t("options.render_distance", rDist) + String.format(" (%d m) >", rDist * 16);
        drawOptionButton(hud, ortho, btnX, startY, btnW, btnH, rDistStr, mx, my);

        // FOV
        String fovStr = "< " + I18n.t("options.fov", GameSettings.fov) + " >";
        drawOptionButton(hud, ortho, btnX, startY + stepY, btnW, btnH, fovStr, mx, my);

        // V-Sync
        String vsyncStr = I18n.t("options.vsync", GameSettings.vsync ? I18n.t("options.on") : I18n.t("options.off"));
        drawOptionButton(hud, ortho, btnX, startY + stepY * 2, btnW, btnH, vsyncStr, mx, my);

        // FPS Limit
        String fpsVal = GameSettings.maxFps == 0 ? I18n.t("options.fps_unlimited") : (GameSettings.maxFps + " FPS");
        String fpsStr = "< " + I18n.t("options.max_fps", fpsVal) + " >";
        drawOptionButton(hud, ortho, btnX, startY + stepY * 3, btnW, btnH, fpsStr, mx, my);

        // Fog
        String fogVal = switch (GameSettings.fogMode) {
            case 0 -> I18n.t("options.fog.off");
            case 1 -> I18n.t("options.fog.near");
            case 2 -> I18n.t("options.fog.normal");
            case 3 -> I18n.t("options.fog.far");
            default -> I18n.t("options.fog.normal");
        };
        String fogStr = "< " + I18n.t("options.fog", fogVal) + " >";
        drawOptionButton(hud, ortho, btnX, startY + stepY * 4, btnW, btnH, fogStr, mx, my);

        // Brightness
        String brightStr = "< " + I18n.t("options.brightness", GameSettings.brightness * 100.0f) + " >";
        drawOptionButton(hud, ortho, btnX, startY + stepY * 5, btnW, btnH, brightStr, mx, my);

        // View Bobbing
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

        // Row 0: Mouse Sensitivity
        String sensStr = "< " + I18n.t("options.sensitivity", GameSettings.mouseSensitivity) + " >";
        drawOptionButton(hud, ortho, btnX, startY, btnW, btnH, sensStr, mx, my);

        // Row 1: Eye Height
        String eyeStr = "< " + I18n.t("options.eye_height", GameSettings.eyeHeight) + " >";
        drawOptionButton(hud, ortho, btnX, startY + stepY, btnW, btnH, eyeStr, mx, my);

        // Row 2: Mouse Hold Delay (Minecraft continuous mining / placement)
        int holdMs = Math.round(GameSettings.mouseHoldDelay * 1000.0f);
        String holdStr = "< " + I18n.t("options.mouse_hold_delay", holdMs) + " >";
        drawOptionButton(hud, ortho, btnX, startY + stepY * 2, btnW, btnH, holdStr, mx, my);

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

        // Row 0: Language toggle
        String langName = I18n.getLanguageDisplayName(GameSettings.language);
        String langStr = "< " + I18n.t("options.language", langName) + " >";
        drawOptionButton(hud, ortho, btnX, startY, btnW, btnH, langStr, mx, my,
                new Vector4f(0.20f, 0.40f, 0.60f, 0.95f), new Vector4f(0.30f, 0.55f, 0.85f, 1.0f));

        // Row 1: Save Format Toggle
        String formatStr = "< " + I18n.t("menu.save_format") + ": " + GameSettings.getSaveFormatName() + " >";
        drawOptionButton(hud, ortho, btnX, startY + stepY, btnW, btnH, formatStr, mx, my);

        // Row 2: Log Level Toggle
        String levelStr = "< " + I18n.t("options.log_level", GameSettings.logLevel) + " >";
        drawOptionButton(hud, ortho, btnX, startY + stepY * 2, btnW, btnH, levelStr, mx, my);

        // Row 3: Manual Crash Dump
        drawOptionButton(hud, ortho, btnX, startY + stepY * 3, btnW, btnH, I18n.t("options.dump_report"), mx, my);

        // Row 4: Live Log preview
        float logBoxY = startY + stepY * 4 + 8.0f;
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

        String[][] leftKeys = {
                {"Forward:", GameSettings.getKeyName(GameSettings.keyForward)},
                {"Back:", GameSettings.getKeyName(GameSettings.keyBack)},
                {"Left:", GameSettings.getKeyName(GameSettings.keyLeft)},
                {"Right:", GameSettings.getKeyName(GameSettings.keyRight)},
                {"Jump / Up:", GameSettings.getKeyName(GameSettings.keyJump)},
                {"Crouch / Down:", GameSettings.getKeyName(GameSettings.keyCrouch)},
                {"Sprint:", GameSettings.getKeyName(GameSettings.keySprint)}
        };

        String[][] rightKeys = {
                {"Inventory:", GameSettings.getKeyName(GameSettings.keyInventory)},
                {"Open Chat:", GameSettings.getKeyName(GameSettings.keyChat)},
                {"Command:", GameSettings.getKeyName(GameSettings.keyCommand)},
                {"Diagnostics / Map:", GameSettings.getKeyName(GameSettings.keyNoiseMap) + " / F4"},
                {"Debug Overlay:", GameSettings.getKeyName(GameSettings.keyDebug)},
                {"Toggle HUD:", GameSettings.getKeyName(GameSettings.keyToggleHud)},
                {"Pause / Menu:", GameSettings.getKeyName(GameSettings.keyPause)}
        };

        for (int i = 0; i < leftKeys.length; i++) {
            float ry = startY + i * rowH;
            drawKeyRow(hud, ortho, col1X, ry, colW, leftKeys[i][0], leftKeys[i][1]);
        }

        for (int i = 0; i < rightKeys.length; i++) {
            float ry = startY + i * rowH;
            drawKeyRow(hud, ortho, col2X, ry, colW, rightKeys[i][0], rightKeys[i][1]);
        }

        float btnW = 280.0f;
        float btnH = 32.0f;
        float btnX = winX + (winW - btnW) * 0.5f;
        float btnY = startY + 200.0f;
        drawOptionButton(hud, ortho, btnX, btnY, btnW, btnH, I18n.t("options.reset_keybinds"), mx, my);
    }

    private void drawKeyRow(HudRenderer hud, Matrix4f ortho, float x, float y, float w, String label, String key) {
        hud.getTextRenderer().drawTextWithShadow(ortho, label, x, y + 4.0f, 1.2f, new Vector4f(0.80f, 0.88f, 0.95f, 0.90f));

        float kw = 110.0f;
        float kx = x + w - kw;
        hud.getColorShader().bind();
        hud.getColorShader().setUniform("uColor", new Vector4f(0.18f, 0.24f, 0.35f, 0.90f));
        hud.drawRect2D(kx, y, kw, 22.0f);
        hud.getColorShader().unbind();

        hud.getTextRenderer().drawTextWithShadow(ortho, key, kx + 8.0f, y + 4.0f, 1.15f, new Vector4f(1.0f, 0.90f, 0.35f, 1.0f));
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
        hud.getTextRenderer().drawTextCenteredInBox(ortho, text, x, y, w, h, 1.25f, 8.0f, txtCol);
    }

    private int getRenderDistanceIndex(int val) {
        for (int i = 0; i < RENDER_DISTANCES.length; i++) {
            if (RENDER_DISTANCES[i] == val) return i;
        }
        return 2;
    }
}
