package com.voxel.ui;

import com.voxel.i18n.I18n;
import com.voxel.world.WorldSaveManager;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;

public class MainMenu {
    public enum Screen {
        TITLE,
        SAVES,
        NEW_WORLD
    }

    public enum Result {
        NONE,
        START_GAME,
        OPEN_OPTIONS,
        QUIT
    }

    private boolean active = true;
    private Screen currentScreen = Screen.TITLE;
    private long openCooldownUntil = 0;

    private List<WorldSaveManager.SaveInfo> saveList;
    private int selectedSaveIndex = 0;
    private long lastSaveClickTime = 0;
    private int lastClickedSaveIndex = -1;
    private float saveScroll = 0.0f;

    // New world inputs
    private final StringBuilder inputWorldName = new StringBuilder("World_1");
    private final StringBuilder inputSeed = new StringBuilder();
    private int activeInputField = 0; // 0 = name, 1 = seed

    private String statusMessage = "";
    private float statusTimer = 0.0f;

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    public MainMenu() {
        refreshSaves();
    }

    public boolean isActive() {
        return active;
    }

    public Screen getCurrentScreen() {
        return currentScreen;
    }

    public void setActive(boolean active) {
        this.active = active;
        if (active) {
            currentScreen = Screen.TITLE;
            refreshSaves();
            openCooldownUntil = System.currentTimeMillis() + 250;
        }
    }

    public void refreshSaves() {
        saveList = WorldSaveManager.listSaves();
        if (selectedSaveIndex >= saveList.size()) {
            selectedSaveIndex = Math.max(0, saveList.size() - 1);
        }
        if (saveList != null) {
            float rowStep = 54.0f + 8.0f;
            float maxScroll = Math.max(0.0f, saveList.size() * rowStep - 360.0f);
            saveScroll = Math.max(0.0f, Math.min(maxScroll, saveScroll));
        }
    }

    public void handleScroll(double yoffset) {
        if (!active || currentScreen != Screen.SAVES || saveList == null) return;
        float rowStep = 54.0f + 8.0f;
        float listH = 360.0f;
        float totalH = saveList.size() * rowStep;
        float maxScroll = Math.max(0.0f, totalH - listH);
        saveScroll = Math.max(0.0f, Math.min(maxScroll, saveScroll - (float) yoffset * 36.0f));
    }

    public WorldSaveManager.SaveInfo getSelectedSave() {
        if (saveList != null && !saveList.isEmpty() && selectedSaveIndex >= 0 && selectedSaveIndex < saveList.size()) {
            return saveList.get(selectedSaveIndex);
        }
        return null;
    }

    public String getNewWorldName() {
        String s = inputWorldName.toString().trim();
        return s.isEmpty() ? "world" : WorldSaveManager.sanitizeSaveName(s);
    }

    public long getNewWorldSeed() {
        String s = inputSeed.toString().trim();
        if (s.isEmpty()) {
            return System.nanoTime() ^ (System.currentTimeMillis() * 6364136223846793005L);
        }
        try {
            return Long.parseLong(s);
        } catch (NumberFormatException e) {
            return s.hashCode();
        }
    }

    public void update(float dt) {
        if (statusTimer > 0.0f) {
            statusTimer -= dt;
            if (statusTimer <= 0.0f) statusMessage = "";
        }
    }

    public void onCharInput(char c) {
        if (!active || currentScreen != Screen.NEW_WORLD) return;
        if (c >= 32 && c <= 126) {
            if (activeInputField == 0 && inputWorldName.length() < 24) {
                if (Character.isLetterOrDigit(c) || c == '_' || c == '-') {
                    inputWorldName.append(c);
                }
            } else if (activeInputField == 1 && inputSeed.length() < 20) {
                inputSeed.append(c);
            }
        }
    }

    public Result handleKey(int key) {
        if (!active) return Result.NONE;

        if (currentScreen == Screen.NEW_WORLD) {
            if (key == GLFW_KEY_BACKSPACE) {
                if (activeInputField == 0 && inputWorldName.length() > 0) {
                    inputWorldName.deleteCharAt(inputWorldName.length() - 1);
                } else if (activeInputField == 1 && inputSeed.length() > 0) {
                    inputSeed.deleteCharAt(inputSeed.length() - 1);
                }
                return Result.NONE;
            } else if (key == GLFW_KEY_TAB) {
                activeInputField = (activeInputField + 1) % 2;
                return Result.NONE;
            } else if (key == GLFW_KEY_ENTER || key == GLFW_KEY_KP_ENTER) {
                active = false;
                return Result.START_GAME;
            } else if (key == GLFW_KEY_ESCAPE) {
                currentScreen = Screen.TITLE;
                return Result.NONE;
            }
        } else if (currentScreen == Screen.SAVES) {
            if (key == GLFW_KEY_ESCAPE) {
                currentScreen = Screen.TITLE;
                return Result.NONE;
            } else if (key == GLFW_KEY_UP) {
                if (saveList != null && !saveList.isEmpty()) {
                    selectedSaveIndex = Math.max(0, selectedSaveIndex - 1);
                }
            } else if (key == GLFW_KEY_DOWN) {
                if (saveList != null && !saveList.isEmpty()) {
                    selectedSaveIndex = Math.min(saveList.size() - 1, selectedSaveIndex + 1);
                }
            } else if (key == GLFW_KEY_ENTER || key == GLFW_KEY_KP_ENTER) {
                if (getSelectedSave() != null) {
                    active = false;
                    return Result.START_GAME;
                }
            }
        } else if (currentScreen == Screen.TITLE) {
            if (key == GLFW_KEY_ENTER || key == GLFW_KEY_KP_ENTER) {
                active = false;
                return Result.START_GAME;
            }
        }

        return Result.NONE;
    }

    public Result handleMouseClick(double mouseX, double mouseY, int button, int screenW, int screenH) {
        if (!active || button != GLFW_MOUSE_BUTTON_LEFT) return Result.NONE;
        if (System.currentTimeMillis() < openCooldownUntil) return Result.NONE;

        float mx = (float) mouseX;
        float my = (float) mouseY;

        if (currentScreen == Screen.TITLE) {
            return handleClickTitle(mx, my, screenW, screenH);
        } else if (currentScreen == Screen.SAVES) {
            return handleClickSaves(mx, my, screenW, screenH);
        } else if (currentScreen == Screen.NEW_WORLD) {
            return handleClickNewWorld(mx, my, screenW, screenH);
        }
        return Result.NONE;
    }

    private Result handleClickTitle(float mx, float my, int screenW, int screenH) {
        float btnW = 360.0f;
        float btnH = 46.0f;
        float btnX = (screenW - btnW) * 0.5f;
        float startY = screenH * 0.44f;

        // Button 1: WYBIERZ SWIAT (Select World)
        if (mx >= btnX && mx <= btnX + btnW && my >= startY && my <= startY + btnH) {
            currentScreen = Screen.SAVES;
            refreshSaves();
            return Result.NONE;
        }
        // Button 2: OPCJE / USTAWIENIA
        startY += 58.0f;
        if (mx >= btnX && mx <= btnX + btnW && my >= startY && my <= startY + btnH) {
            return Result.OPEN_OPTIONS;
        }
        // Button 3: WYJDZ
        startY += 58.0f;
        if (mx >= btnX && mx <= btnX + btnW && my >= startY && my <= startY + btnH) {
            return Result.QUIT;
        }

        return Result.NONE;
    }

    public void onResize(int screenW, int screenH) {
        if (saveList != null) {
            float rowStep = 54.0f + 8.0f;
            float listH = screenH - 185.0f;
            float maxScroll = Math.max(0.0f, saveList.size() * rowStep - listH);
            saveScroll = Math.max(0.0f, Math.min(maxScroll, saveScroll));
        }
    }

    private Result handleClickSaves(float mx, float my, int screenW, int screenH) {
        float listW = Math.min(680.0f, screenW - 40.0f);
        float listX = (screenW - listW) * 0.5f;
        float listY = 95.0f;
        float listH = screenH - 185.0f;
        float itemH = 54.0f;
        float itemSpacing = 8.0f;
        float rowStep = itemH + itemSpacing;

        float totalH = (saveList != null ? saveList.size() : 0) * rowStep;
        float maxScroll = Math.max(0.0f, totalH - listH);

        // Click on scrollbar track to jump scroll
        if (maxScroll > 0.0f) {
            float sbX = listX + listW + 6.0f;
            if (mx >= sbX - 4.0f && mx <= sbX + 12.0f && my >= listY && my <= listY + listH) {
                saveScroll = Math.max(0.0f, Math.min(maxScroll, ((my - listY) / listH) * maxScroll));
                return Result.NONE;
            }
        }

        // Check clicking on save items (with double-click to play)
        if (saveList != null) {
            for (int i = 0; i < saveList.size(); i++) {
                float iy = listY + i * rowStep - saveScroll;
                if (iy < listY - 4.0f || iy + itemH > listY + listH + 4.0f) {
                    continue;
                }
                if (mx >= listX && mx <= listX + listW && my >= iy && my <= iy + itemH) {
                    long now = System.currentTimeMillis();
                    if (selectedSaveIndex == i && lastClickedSaveIndex == i && (now - lastSaveClickTime < 450)) {
                        active = false;
                        return Result.START_GAME;
                    }
                    selectedSaveIndex = i;
                    lastClickedSaveIndex = i;
                    lastSaveClickTime = now;
                    break;
                }
            }
        }

        // Bottom action buttons: dynamically adapted with generous widths
        float btnY = screenH - 74.0f;
        float btnH = 40.0f;
        float gap = 8.0f;
        float availW = listW - gap * 3.0f;
        float btnW1 = Math.round(availW * 0.33f);
        float btnW2 = Math.round(availW * 0.31f);
        float btnW3 = Math.round(availW * 0.21f);
        float btnW4 = availW - btnW1 - btnW2 - btnW3;

        float b1X = listX;
        float b2X = b1X + btnW1 + gap;
        float b3X = b2X + btnW2 + gap;
        float b4X = b3X + btnW3 + gap;

        // Zagraj (Play Selected World)
        if (mx >= b1X && mx <= b1X + btnW1 && my >= btnY && my <= btnY + btnH) {
            if (getSelectedSave() != null) {
                active = false;
                return Result.START_GAME;
            }
        }
        // Nowy swiat (Create New World)
        if (mx >= b2X && mx <= b2X + btnW2 && my >= btnY && my <= btnY + btnH) {
            currentScreen = Screen.NEW_WORLD;
            inputWorldName.setLength(0);
            inputWorldName.append("World_").append(saveList != null ? saveList.size() + 1 : 1);
            inputSeed.setLength(0);
            activeInputField = 0;
            return Result.NONE;
        }
        // Usun (Delete World)
        if (mx >= b3X && mx <= b3X + btnW3 && my >= btnY && my <= btnY + btnH) {
            WorldSaveManager.SaveInfo selected = getSelectedSave();
            if (selected != null) {
                WorldSaveManager.deleteSave(selected.name());
                statusMessage = I18n.t("menu.deleted", selected.name());
                statusTimer = 2.5f;
                refreshSaves();
            }
            return Result.NONE;
        }
        // Powrot (Back)
        if (mx >= b4X && mx <= b4X + btnW4 && my >= btnY && my <= btnY + btnH) {
            currentScreen = Screen.TITLE;
            return Result.NONE;
        }

        return Result.NONE;
    }

    private Result handleClickNewWorld(float mx, float my, int screenW, int screenH) {
        float boxW = 440.0f;
        float boxX = (screenW - boxW) * 0.5f;

        float f1Y = screenH * 0.38f;
        if (mx >= boxX && mx <= boxX + boxW && my >= f1Y && my <= f1Y + 36.0f) {
            activeInputField = 0;
            return Result.NONE;
        }

        float f2Y = f1Y + 68.0f;
        if (mx >= boxX && mx <= boxX + boxW && my >= f2Y && my <= f2Y + 36.0f) {
            activeInputField = 1;
            return Result.NONE;
        }

        // Action buttons
        float btnY = f2Y + 74.0f;
        float btnW = 200.0f;
        float btnH = 40.0f;

        // Stworz i graj
        float btnCreateX = boxX;
        if (mx >= btnCreateX && mx <= btnCreateX + btnW && my >= btnY && my <= btnY + btnH) {
            active = false;
            return Result.START_GAME;
        }

        // Anuluj (returns to Saves list screen)
        float btnCancelX = boxX + boxW - btnW;
        if (mx >= btnCancelX && mx <= btnCancelX + btnW && my >= btnY && my <= btnY + btnH) {
            currentScreen = Screen.SAVES;
            return Result.NONE;
        }

        return Result.NONE;
    }

    public void render(HudRenderer hud, int screenW, int screenH, double mouseX, double mouseY) {
        if (!active) return;

        glDisable(GL_DEPTH_TEST);
        glDisable(GL_CULL_FACE);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        float mx = (float) mouseX;
        float my = (float) mouseY;

        Matrix4f ortho = new Matrix4f().ortho(0, screenW, screenH, 0, -1, 1);

        // Background
        hud.getColorShader().bind();
        hud.getColorShader().setUniform("uMVP", ortho);
        hud.getColorShader().setUniform("uColor", new Vector4f(0.06f, 0.08f, 0.12f, 1.0f));
        hud.drawRect2D(0, 0, screenW, screenH);
        hud.getColorShader().unbind();

        if (currentScreen == Screen.TITLE) {
            renderTitleScreen(hud, ortho, screenW, screenH, mx, my);
        } else if (currentScreen == Screen.SAVES) {
            renderSavesScreen(hud, ortho, screenW, screenH, mx, my);
        } else if (currentScreen == Screen.NEW_WORLD) {
            renderNewWorldScreen(hud, ortho, screenW, screenH, mx, my);
        }

        glEnable(GL_CULL_FACE);
        glEnable(GL_DEPTH_TEST);
    }

    private void renderTitleScreen(HudRenderer hud, Matrix4f ortho, int screenW, int screenH, float mx, float my) {
        // Logo / Title
        String title = "JAVA VOXEL 3D";
        float tw = hud.getTextRenderer().getTextWidth(title, 4.0f);
        hud.getTextRenderer().drawTextWithShadow(ortho, title, (screenW - tw) * 0.5f, screenH * 0.18f, 4.0f,
                new Vector4f(1.0f, 0.90f, 0.35f, 1.0f));

        String sub = I18n.t("menu.subtitle");
        float subW = hud.getTextRenderer().getTextWidth(sub, 1.5f);
        hud.getTextRenderer().drawTextWithShadow(ortho, sub, (screenW - subW) * 0.5f, screenH * 0.18f + 46.0f, 1.5f,
                new Vector4f(0.70f, 0.85f, 0.95f, 0.85f));

        // Buttons: Select World, Options, Quit
        float btnW = 360.0f;
        float btnH = 46.0f;
        float btnX = (screenW - btnW) * 0.5f;
        float startY = screenH * 0.44f;

        String[] buttons = {
            I18n.t("menu.worlds"),
            I18n.t("menu.options"),
            I18n.t("menu.quit")
        };

        for (int i = 0; i < buttons.length; i++) {
            float by = startY + i * 58.0f;
            boolean hover = (mx >= btnX && mx <= btnX + btnW && my >= by && my <= by + btnH);

            hud.getColorShader().bind();
            hud.getColorShader().setUniform("uMVP", ortho);
            if (hover) {
                hud.getColorShader().setUniform("uColor", new Vector4f(1.0f, 0.85f, 0.30f, 1.0f));
                hud.drawRect2D(btnX - 2, by - 2, btnW + 4, btnH + 4);
                hud.getColorShader().setUniform("uColor", new Vector4f(0.24f, 0.30f, 0.42f, 0.98f));
            } else {
                hud.getColorShader().setUniform("uColor", new Vector4f(0.16f, 0.20f, 0.28f, 0.95f));
            }
            hud.drawRect2D(btnX, by, btnW, btnH);
            hud.getColorShader().unbind();

            String lbl = buttons[i];
            Vector4f col = hover ? new Vector4f(1.0f, 0.95f, 0.45f, 1.0f) : new Vector4f(0.92f, 0.95f, 0.98f, 1.0f);
            hud.getTextRenderer().drawTextCenteredInBox(ortho, lbl, btnX, by, btnW, btnH, 1.8f, 12.0f, col);
        }

        // Footer info
        String footer = I18n.t("menu.footer");
        float footW = hud.getTextRenderer().getTextWidth(footer, 1.2f);
        hud.getTextRenderer().drawTextWithShadow(ortho, footer, (screenW - footW) * 0.5f, screenH - 30.0f, 1.2f,
                new Vector4f(0.55f, 0.65f, 0.75f, 0.70f));
    }

    private void renderSavesScreen(HudRenderer hud, Matrix4f ortho, int screenW, int screenH, float mx, float my) {
        String title = I18n.t("menu.saves_title");
        float tw = hud.getTextRenderer().getTextWidth(title, 2.5f);
        hud.getTextRenderer().drawTextWithShadow(ortho, title, (screenW - tw) * 0.5f, 36.0f, 2.5f,
                new Vector4f(1.0f, 0.90f, 0.35f, 1.0f));

        float listW = Math.min(680.0f, screenW - 40.0f);
        float listX = (screenW - listW) * 0.5f;
        float listY = 95.0f;
        float listH = screenH - 185.0f;
        float itemH = 54.0f;
        float itemSpacing = 8.0f;
        float rowStep = itemH + itemSpacing;

        if (saveList == null || saveList.isEmpty()) {
            String empty = I18n.t("menu.empty_saves");
            float ew = hud.getTextRenderer().getTextWidth(empty, 1.6f);
            hud.getTextRenderer().drawTextWithShadow(ortho, empty, (screenW - ew) * 0.5f, screenH * 0.40f, 1.6f,
                    new Vector4f(0.8f, 0.8f, 0.8f, 0.8f));
        } else {
            float totalH = saveList.size() * rowStep;
            float maxScroll = Math.max(0.0f, totalH - listH);
            saveScroll = Math.max(0.0f, Math.min(maxScroll, saveScroll));

            for (int i = 0; i < saveList.size(); i++) {
                WorldSaveManager.SaveInfo info = saveList.get(i);
                float iy = listY + i * rowStep - saveScroll;
                if (iy + itemH < listY || iy > listY + listH) {
                    continue;
                }
                boolean selected = (i == selectedSaveIndex);
                boolean hover = (mx >= listX && mx <= listX + listW && my >= iy && my <= iy + itemH);

                hud.getColorShader().bind();
                hud.getColorShader().setUniform("uMVP", ortho);
                if (selected) {
                    hud.getColorShader().setUniform("uColor", new Vector4f(1.0f, 0.85f, 0.25f, 1.0f));
                    hud.drawRect2D(listX - 2, iy - 2, listW + 4, itemH + 4);
                    hud.getColorShader().setUniform("uColor", new Vector4f(0.24f, 0.28f, 0.38f, 0.98f));
                } else if (hover) {
                    hud.getColorShader().setUniform("uColor", new Vector4f(0.55f, 0.70f, 0.95f, 0.80f));
                    hud.drawRect2D(listX - 1, iy - 1, listW + 2, itemH + 2);
                    hud.getColorShader().setUniform("uColor", new Vector4f(0.18f, 0.22f, 0.30f, 0.95f));
                } else {
                    hud.getColorShader().setUniform("uColor", new Vector4f(0.13f, 0.16f, 0.22f, 0.92f));
                }
                hud.drawRect2D(listX, iy, listW, itemH);
                hud.getColorShader().unbind();

                String line1 = String.format("%s (Seed: %d)", info.name(), info.seed());
                String dateStr = DATE_FORMAT.format(new Date(info.lastModifiedTime()));
                String line2 = I18n.t("menu.save_details",
                        info.posX(), info.posY(), info.posZ(), info.formatName(), dateStr);

                hud.getTextRenderer().drawTextWithShadow(ortho, line1, listX + 12.0f, iy + 8.0f, 1.6f,
                        selected ? new Vector4f(1.0f, 0.92f, 0.45f, 1.0f) : new Vector4f(0.95f, 0.95f, 0.98f, 1.0f));
                hud.getTextRenderer().drawTextWithShadow(ortho, line2, listX + 12.0f, iy + 28.0f, 1.2f,
                        new Vector4f(0.68f, 0.75f, 0.85f, 0.85f));
            }

            // Scrollbar
            if (maxScroll > 0.0f) {
                float sbX = listX + listW + 6.0f;
                float sbW = 6.0f;
                hud.getColorShader().bind();
                hud.getColorShader().setUniform("uColor", new Vector4f(0.10f, 0.13f, 0.18f, 0.85f));
                hud.drawRect2D(sbX, listY, sbW, listH);

                float thumbH = Math.max(30.0f, listH * (listH / totalH));
                float thumbY = listY + (saveScroll / maxScroll) * (listH - thumbH);
                boolean sbHover = (mx >= sbX - 4.0f && mx <= sbX + sbW + 4.0f && my >= listY && my <= listY + listH);
                hud.getColorShader().setUniform("uColor", sbHover ? new Vector4f(0.35f, 0.70f, 1.0f, 0.95f) : new Vector4f(0.24f, 0.45f, 0.72f, 0.85f));
                hud.drawRect2D(sbX, thumbY, sbW, thumbH);
                hud.getColorShader().unbind();
            }
        }

        // Action buttons: dynamically adapted with generous widths
        float btnY = screenH - 74.0f;
        float btnH = 40.0f;
        float gap = 8.0f;
        float availW = listW - gap * 3.0f;
        float btnW1 = Math.round(availW * 0.33f);
        float btnW2 = Math.round(availW * 0.31f);
        float btnW3 = Math.round(availW * 0.21f);
        float btnW4 = availW - btnW1 - btnW2 - btnW3;

        float b1X = listX;
        float b2X = b1X + btnW1 + gap;
        float b3X = b2X + btnW2 + gap;
        float b4X = b3X + btnW3 + gap;

        drawButton(hud, ortho, b1X, btnY, btnW1, btnH, I18n.t("menu.play_selected"), mx, my);
        drawButton(hud, ortho, b2X, btnY, btnW2, btnH, I18n.t("menu.new_world"), mx, my);
        drawButton(hud, ortho, b3X, btnY, btnW3, btnH, I18n.t("menu.delete"), mx, my);
        drawButton(hud, ortho, b4X, btnY, btnW4, btnH, I18n.t("menu.back"), mx, my);

        if (!statusMessage.isEmpty()) {
            float sw = hud.getTextRenderer().getTextWidth(statusMessage, 1.4f);
            hud.getTextRenderer().drawTextWithShadow(ortho, statusMessage, (screenW - sw) * 0.5f, btnY - 26.0f, 1.4f,
                    new Vector4f(0.45f, 0.98f, 0.55f, 1.0f));
        }
    }

    private void renderNewWorldScreen(HudRenderer hud, Matrix4f ortho, int screenW, int screenH, float mx, float my) {
        String title = I18n.t("menu.create_title");
        float tw = hud.getTextRenderer().getTextWidth(title, 2.5f);
        hud.getTextRenderer().drawTextWithShadow(ortho, title, (screenW - tw) * 0.5f, 50.0f, 2.5f,
                new Vector4f(1.0f, 0.90f, 0.35f, 1.0f));

        float boxW = 440.0f;
        float boxX = (screenW - boxW) * 0.5f;

        // Field 1: Name
        float f1Y = screenH * 0.36f;
        hud.getTextRenderer().drawTextWithShadow(ortho, I18n.t("menu.world_name") + ":", boxX, f1Y - 20.0f, 1.4f,
                new Vector4f(0.85f, 0.90f, 0.95f, 0.95f));

        hud.getColorShader().bind();
        hud.getColorShader().setUniform("uMVP", ortho);
        if (activeInputField == 0) {
            hud.getColorShader().setUniform("uColor", new Vector4f(1.0f, 0.85f, 0.30f, 1.0f));
            hud.drawRect2D(boxX - 2, f1Y - 2, boxW + 4, 40.0f);
        }
        hud.getColorShader().setUniform("uColor", new Vector4f(0.12f, 0.15f, 0.20f, 0.98f));
        hud.drawRect2D(boxX, f1Y, boxW, 36.0f);
        hud.getColorShader().unbind();

        String nameVal = inputWorldName.toString() + (activeInputField == 0 && (System.currentTimeMillis() / 400) % 2 == 0 ? "_" : "");
        hud.getTextRenderer().drawTextWithShadow(ortho, nameVal, boxX + 10.0f, f1Y + 9.0f, 1.8f,
                new Vector4f(1.0f, 1.0f, 1.0f, 1.0f));

        // Field 2: Seed
        float f2Y = f1Y + 70.0f;
        hud.getTextRenderer().drawTextWithShadow(ortho, I18n.t("menu.world_seed") + ":", boxX, f2Y - 20.0f, 1.4f,
                new Vector4f(0.85f, 0.90f, 0.95f, 0.95f));

        hud.getColorShader().bind();
        hud.getColorShader().setUniform("uMVP", ortho);
        if (activeInputField == 1) {
            hud.getColorShader().setUniform("uColor", new Vector4f(1.0f, 0.85f, 0.30f, 1.0f));
            hud.drawRect2D(boxX - 2, f2Y - 2, boxW + 4, 40.0f);
        }
        hud.getColorShader().setUniform("uColor", new Vector4f(0.12f, 0.15f, 0.20f, 0.98f));
        hud.drawRect2D(boxX, f2Y, boxW, 36.0f);
        hud.getColorShader().unbind();

        String seedPlaceholder = I18n.t("menu.seed_random");
        String seedVal = inputSeed.toString() + (activeInputField == 1 && (System.currentTimeMillis() / 400) % 2 == 0 ? "_" : "");
        hud.getTextRenderer().drawTextWithShadow(ortho, seedVal.isEmpty() ? seedPlaceholder : seedVal, boxX + 10.0f, f2Y + 9.0f, 1.8f,
                seedVal.isEmpty() ? new Vector4f(0.5f, 0.5f, 0.5f, 0.8f) : new Vector4f(1.0f, 1.0f, 1.0f, 1.0f));

        // Action buttons
        float btnY = f2Y + 76.0f;
        float btnW = 200.0f;
        float btnH = 42.0f;

        drawButton(hud, ortho, boxX, btnY, btnW, btnH, I18n.t("menu.create"), mx, my);
        drawButton(hud, ortho, boxX + boxW - btnW, btnY, btnW, btnH, I18n.t("menu.cancel"), mx, my);
    }

    private void drawButton(HudRenderer hud, Matrix4f ortho, float x, float y, float w, float h, String text, float mx, float my) {
        boolean hover = (mx >= x && mx <= x + w && my >= y && my <= y + h);

        hud.getColorShader().bind();
        hud.getColorShader().setUniform("uMVP", ortho);
        if (hover) {
            hud.getColorShader().setUniform("uColor", new Vector4f(1.0f, 0.85f, 0.30f, 1.0f));
            hud.drawRect2D(x - 2, y - 2, w + 4, h + 4);
            hud.getColorShader().setUniform("uColor", new Vector4f(0.24f, 0.30f, 0.42f, 0.98f));
        } else {
            hud.getColorShader().setUniform("uColor", new Vector4f(0.16f, 0.20f, 0.28f, 0.95f));
        }
        hud.drawRect2D(x, y, w, h);
        hud.getColorShader().unbind();

        Vector4f col = hover ? new Vector4f(1.0f, 0.95f, 0.45f, 1.0f) : new Vector4f(0.92f, 0.95f, 0.98f, 1.0f);
        hud.getTextRenderer().drawTextCenteredInBox(ortho, text, x, y, w, h, 1.30f, 12.0f, col);
    }
}
