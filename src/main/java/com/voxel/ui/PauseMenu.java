package com.voxel.ui;

import com.voxel.entity.Player;
import com.voxel.i18n.I18n;
import com.voxel.world.World;
import com.voxel.world.WorldSaveManager;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;

public class PauseMenu {
    public enum Action {
        NONE,
        RESUME,
        OPEN_INVENTORY,
        OPEN_NOISE_MAP,
        OPEN_OPTIONS,
        SAVE,
        RESET_WORLD,
        MAIN_MENU,
        QUIT
    }

    private boolean open = false;
    private long openCooldownUntil = 0;
    private String statusMessage = "";
    private float statusTimer = 0.0f;

    private static final String[] BUTTON_KEYS = {
        "pause.resume",
        "pause.options",
        "menu.save_game",
        "pause.save_quit"
    };

    private static final String[] BUTTON_FALLBACKS = {
        "Back to Game",
        "Options...",
        "Save World",
        "Save and Quit to Title"
    };

    private static final Action[] BUTTON_ACTIONS = {
        Action.RESUME,
        Action.OPEN_OPTIONS,
        Action.SAVE,
        Action.MAIN_MENU
    };

    public boolean isOpen() {
        return open;
    }

    public void onResize(int screenW, int screenH) {
        // Dynamic centering is calculated per frame and event
    }

    public void setOpen(boolean open) {
        this.open = open;
        if (open) {
            this.openCooldownUntil = System.currentTimeMillis() + 250;
        } else {
            statusMessage = "";
        }
    }

    public void resetCooldown() {
        this.openCooldownUntil = 0;
    }

    public void toggle() {
        setOpen(!open);
    }

    public void setStatusMessage(String msg) {
        this.statusMessage = msg;
        this.statusTimer = 3.0f;
    }

    public void update(float dt) {
        if (statusTimer > 0.0f) {
            statusTimer -= dt;
            if (statusTimer <= 0.0f) {
                statusMessage = "";
            }
        }
    }

    public Action handleMouseClick(double mouseX, double mouseY, int button, int screenW, int screenH) {
        if (!open || button != GLFW_MOUSE_BUTTON_LEFT) return Action.NONE;
        if (System.currentTimeMillis() < openCooldownUntil) return Action.NONE;

        float mx = (float) mouseX;
        float my = (float) mouseY;

        float winW = 380.0f;
        float btnW = 340.0f;
        float btnH = 38.0f;
        float btnSpacing = 10.0f;
        float totalBtnsH = BUTTON_KEYS.length * btnH + (BUTTON_KEYS.length - 1) * btnSpacing;
        float winH = totalBtnsH + 110.0f;

        float winX = (screenW - winW) * 0.5f;
        float winY = (screenH - winH) * 0.5f;
        float startY = winY + 68.0f;

        for (int i = 0; i < BUTTON_KEYS.length; i++) {
            float bx = winX + (winW - btnW) * 0.5f;
            float by = startY + i * (btnH + btnSpacing);
            if (mx >= bx && mx <= bx + btnW && my >= by && my <= by + btnH) {
                return BUTTON_ACTIONS[i];
            }
        }
        return Action.NONE;
    }

    public void render(HudRenderer hud, int screenW, int screenH, double mouseX, double mouseY,
                       World world, Player player) {
        if (!open) return;

        glDisable(GL_DEPTH_TEST);
        glDisable(GL_CULL_FACE);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        float mx = (float) mouseX;
        float my = (float) mouseY;

        Matrix4f ortho = new Matrix4f().ortho(0, screenW, screenH, 0, -1, 1);

        // 1. Semi-transparent black backdrop
        hud.getColorShader().bind();
        hud.getColorShader().setUniform("uMVP", ortho);
        hud.getColorShader().setUniform("uColor", new Vector4f(0.02f, 0.04f, 0.08f, 0.72f));
        hud.drawRect2D(0, 0, screenW, screenH);

        // 2. Window container
        float winW = 380.0f;
        float btnW = 340.0f;
        float btnH = 38.0f;
        float btnSpacing = 10.0f;
        float totalBtnsH = BUTTON_KEYS.length * btnH + (BUTTON_KEYS.length - 1) * btnSpacing;
        float winH = totalBtnsH + 110.0f;

        float winX = (screenW - winW) * 0.5f;
        float winY = (screenH - winH) * 0.5f;

        // Border
        hud.getColorShader().setUniform("uColor", new Vector4f(0.85f, 0.70f, 0.25f, 0.95f));
        hud.drawRect2D(winX - 2, winY - 2, winW + 4, winH + 4);

        // Background
        hud.getColorShader().setUniform("uColor", new Vector4f(0.09f, 0.12f, 0.16f, 0.96f));
        hud.drawRect2D(winX, winY, winW, winH);

        // Header bar
        hud.getColorShader().setUniform("uColor", new Vector4f(0.15f, 0.19f, 0.26f, 0.98f));
        hud.drawRect2D(winX, winY, winW, 52.0f);
        hud.getColorShader().unbind();

        // Title and info
        String pauseTitle = I18n.t("pause.title") + " (ESC)";
        hud.getTextRenderer().drawTextWithShadow(ortho, pauseTitle, winX + 16.0f, winY + 10.0f, 2.0f,
                new Vector4f(1.0f, 0.92f, 0.40f, 1.0f));
        String worldInfo = I18n.t("pause.world_info", WorldSaveManager.getCurrentSaveName(), world.getSeed());
        hud.getTextRenderer().drawTextWithShadow(ortho, worldInfo, winX + 16.0f, winY + 32.0f, 1.2f,
                new Vector4f(0.70f, 0.85f, 0.95f, 0.90f));

        // Buttons
        float startY = winY + 68.0f;
        for (int i = 0; i < BUTTON_KEYS.length; i++) {
            float bx = winX + (winW - btnW) * 0.5f;
            float by = startY + i * (btnH + btnSpacing);
            boolean hover = (mx >= bx && mx <= bx + btnW && my >= by && my <= by + btnH);

            hud.getColorShader().bind();
            hud.getColorShader().setUniform("uMVP", ortho);
            if (hover) {
                hud.getColorShader().setUniform("uColor", new Vector4f(1.0f, 0.85f, 0.30f, 1.0f));
                hud.drawRect2D(bx - 2, by - 2, btnW + 4, btnH + 4);
                hud.getColorShader().setUniform("uColor", new Vector4f(0.24f, 0.30f, 0.40f, 0.98f));
            } else {
                hud.getColorShader().setUniform("uColor", new Vector4f(0.18f, 0.22f, 0.30f, 0.92f));
            }
            hud.drawRect2D(bx, by, btnW, btnH);
            hud.getColorShader().unbind();

            String label = I18n.t(BUTTON_KEYS[i]);
            if (label.equals(BUTTON_KEYS[i])) {
                label = BUTTON_FALLBACKS[i];
            }
            Vector4f col = hover ? new Vector4f(1.0f, 0.95f, 0.4f, 1.0f) : new Vector4f(0.92f, 0.94f, 0.98f, 1.0f);
            hud.getTextRenderer().drawTextCenteredInBox(ortho, label, bx, by, btnW, btnH, 1.6f, 10.0f, col);
        }

        // Status message (e.g. "Zapisano gre!")
        if (!statusMessage.isEmpty()) {
            float tw = hud.getTextRenderer().getTextWidth(statusMessage, 1.4f);
            float tx = winX + (winW - tw) * 0.5f;
            float ty = winY + winH - 24.0f;
            hud.getTextRenderer().drawTextWithShadow(ortho, statusMessage, tx, ty, 1.4f,
                    new Vector4f(0.45f, 0.98f, 0.55f, 1.0f));
        }

        glEnable(GL_CULL_FACE);
        glEnable(GL_DEPTH_TEST);
    }
}
