package com.voxel.ui;

import com.voxel.block.BlockType;
import com.voxel.crafting.CraftingManager;
import com.voxel.entity.Player;
import com.voxel.i18n.I18n;
import com.voxel.render.TextureAtlas;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import java.util.List;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;

public class InventoryMenu {
    private boolean open = false;
    private int currentTab = 0; // 0 = Creative, 1 = Crafting
    private int craftingMode = 0; // 0 = 2x2 Grid, 1 = 3x3 Table
    private int scrollRow = 0;
    private int recipeScroll = 0;

    // 3x3 Crafting grid state
    private final BlockType[][] craftGrid = new BlockType[3][3];
    private BlockType selectedIngredient = BlockType.WOOD;

    public static final int COLS = 9;
    public static final int ROWS = 5;
    public static final float SLOT_SIZE = 48.0f;
    public static final float SLOT_PAD = 6.0f;

    // Hotbar preview metrics inside window
    public static final float HOTBAR_SLOT_SIZE = 44.0f;
    public static final float HOTBAR_SLOT_PAD = 6.0f;

    // Quick ingredients palette for crafting
    public static final BlockType[] CRAFT_PALETTE = {
            BlockType.WOOD,
            BlockType.PLANKS,
            BlockType.COBBLESTONE,
            BlockType.STONE,
            BlockType.DIRT,
            BlockType.SAND,
            BlockType.RED_SAND,
            BlockType.CLAY,
            BlockType.SNOW,
            BlockType.SUGAR_CANE,
            BlockType.GLASS,
            BlockType.CORAL,
            BlockType.CACTUS
    };

    public InventoryMenu() {
        clearCraftGrid();
    }

    public boolean isOpen() {
        return open;
    }

    public void setOpen(boolean open) {
        this.open = open;
    }

    public void toggle() {
        this.open = !this.open;
    }

    public void clearCraftGrid() {
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                craftGrid[r][c] = BlockType.AIR;
            }
        }
    }

    public CraftingManager.CraftResult getActiveCraftResult() {
        if (craftingMode == 0) {
            // 2x2 mode
            BlockType[][] sub = new BlockType[2][2];
            for (int r = 0; r < 2; r++) {
                for (int c = 0; c < 2; c++) {
                    sub[r][c] = craftGrid[r][c];
                }
            }
            return CraftingManager.findMatchingRecipe(sub);
        } else {
            // 3x3 mode
            return CraftingManager.findMatchingRecipe(craftGrid);
        }
    }

    public boolean handleScroll(double yoffset) {
        if (!open) return false;
        if (currentTab == 0) {
            int totalRows = (BlockType.getCreativeBlocks().length + COLS - 1) / COLS;
            int maxScrollRow = Math.max(0, totalRows - ROWS);
            if (yoffset > 0) {
                scrollRow = Math.max(0, scrollRow - 1);
                return true;
            } else if (yoffset < 0) {
                scrollRow = Math.min(maxScrollRow, scrollRow + 1);
                return true;
            }
        } else {
            List<CraftingManager.RecipeInfo> recipes = CraftingManager.getAllRecipes();
            int maxRecipeScroll = Math.max(0, recipes.size() - 7);
            if (yoffset > 0) {
                recipeScroll = Math.max(0, recipeScroll - 1);
                return true;
            } else if (yoffset < 0) {
                recipeScroll = Math.min(maxRecipeScroll, recipeScroll + 1);
                return true;
            }
        }
        return false;
    }

    public boolean handleKey(int key, double mouseX, double mouseY, Player player) {
        if (!open) return false;

        if (key == GLFW_KEY_E || key == GLFW_KEY_ESCAPE) {
            open = false;
            return true;
        }

        int targetHotbarSlot = com.voxel.config.GameSettings.getHotbarSlotForKey(key);
        if (targetHotbarSlot >= 0 && targetHotbarSlot < player.hotbar.length) {
            BlockType hovered = getHoveredBlock((float) mouseX, (float) mouseY);
            if (hovered != null) {
                player.setHotbarSlot(targetHotbarSlot, hovered);
                player.selectedSlot = targetHotbarSlot;
                return true;
            }
        }
        return false;
    }

    public boolean handleMouseClick(double mouseX, double mouseY, int button, Player player) {
        if (!open) return false;

        float mx = (float) mouseX;
        float my = (float) mouseY;

        float winW = COLS * SLOT_SIZE + (COLS + 1) * SLOT_PAD;
        float winH = ROWS * SLOT_SIZE + (ROWS + 1) * SLOT_PAD + 120.0f;
        float winX = (lastScreenW - winW) * 0.5f;
        float winY = (lastScreenH - winH) * 0.5f;

        // 1. Header Tab Buttons
        float tabW = 96.0f;
        float tabH = 24.0f;
        float tab0X = winX + winW - 204.0f;
        float tab1X = winX + winW - 102.0f;
        float tabY = winY + 6.0f;

        if (button == GLFW_MOUSE_BUTTON_LEFT && my >= tabY && my <= tabY + tabH) {
            if (mx >= tab0X && mx <= tab0X + tabW) {
                currentTab = 0;
                com.voxel.audio.SoundSystem.playClickSound();
                return true;
            }
            if (mx >= tab1X && mx <= tab1X + tabW) {
                currentTab = 1;
                com.voxel.audio.SoundSystem.playClickSound();
                return true;
            }
        }

        // 2. Hotbar Click (Shared between tabs)
        int hotbarSlot = getHoveredHotbarSlot(mx, my);
        if (hotbarSlot >= 0 && hotbarSlot < player.hotbar.length) {
            if (button == GLFW_MOUSE_BUTTON_LEFT) {
                player.selectedSlot = hotbarSlot;
                BlockType hb = player.hotbar[hotbarSlot];
                if (hb != null && hb != BlockType.AIR) {
                    selectedIngredient = hb;
                }
            }
            return true;
        }

        // 3. Tab 0: Creative Mode Clicks
        if (currentTab == 0) {
            if (button == GLFW_MOUSE_BUTTON_LEFT) {
                BlockType clicked = getHoveredBlock(mx, my);
                if (clicked != null) {
                    player.setHotbarSlot(player.selectedSlot, clicked);
                    selectedIngredient = clicked;
                    return true;
                }
            }
            return true;
        }

        // 4. Tab 1: Crafting Workshop Clicks
        if (currentTab == 1) {
            // Mode toggle button: 2x2 / 3x3
            float modeBtnX = winX + 16.0f;
            float modeBtnY = winY + 44.0f;
            float modeBtnW = 94.0f;
            float modeBtnH = 22.0f;
            if (button == GLFW_MOUSE_BUTTON_LEFT && mx >= modeBtnX && mx <= modeBtnX + modeBtnW &&
                    my >= modeBtnY && my <= modeBtnY + modeBtnH) {
                craftingMode = 1 - craftingMode;
                com.voxel.audio.SoundSystem.playClickSound();
                return true;
            }

            // Clear button
            float clearBtnX = winX + 116.0f;
            float clearBtnY = winY + 44.0f;
            float clearBtnW = 60.0f;
            float clearBtnH = 22.0f;
            if (button == GLFW_MOUSE_BUTTON_LEFT && mx >= clearBtnX && mx <= clearBtnX + clearBtnW &&
                    my >= clearBtnY && my <= clearBtnY + clearBtnH) {
                clearCraftGrid();
                com.voxel.audio.SoundSystem.playClickSound();
                return true;
            }

            // Crafting grid slots
            int gridSize = (craftingMode == 0) ? 2 : 3;
            float cSlotSize = (craftingMode == 0) ? 46.0f : 36.0f;
            float cSlotPad = (craftingMode == 0) ? 6.0f : 5.0f;
            float gridStartX = winX + 16.0f;
            float gridStartY = winY + 74.0f;

            for (int r = 0; r < gridSize; r++) {
                for (int c = 0; c < gridSize; c++) {
                    float sx = gridStartX + c * (cSlotSize + cSlotPad);
                    float sy = gridStartY + r * (cSlotSize + cSlotPad);
                    if (mx >= sx && mx <= sx + cSlotSize && my >= sy && my <= sy + cSlotSize) {
                        if (button == GLFW_MOUSE_BUTTON_LEFT) {
                            // Place selected ingredient or cycle
                            craftGrid[r][c] = selectedIngredient;
                        } else if (button == GLFW_MOUSE_BUTTON_RIGHT) {
                            // Clear single slot
                            craftGrid[r][c] = BlockType.AIR;
                        }
                        return true;
                    }
                }
            }

            // Output Result Slot
            float outSlotSize = 54.0f;
            float outX = gridStartX + gridSize * (cSlotSize + cSlotPad) + 36.0f;
            float outY = gridStartY + (gridSize * (cSlotSize + cSlotPad) - outSlotSize) * 0.5f;

            if (mx >= outX && mx <= outX + outSlotSize && my >= outY && my <= outY + outSlotSize) {
                if (button == GLFW_MOUSE_BUTTON_LEFT) {
                    CraftingManager.CraftResult res = getActiveCraftResult();
                    if (res != null) {
                        player.setHotbarSlot(player.selectedSlot, res.result());
                        com.voxel.audio.SoundSystem.playCraftSound();
                        // Consume 1 of each ingredient from active grid
                        for (int r = 0; r < gridSize; r++) {
                            for (int c = 0; c < gridSize; c++) {
                                craftGrid[r][c] = BlockType.AIR;
                            }
                        }
                    }
                }
                return true;
            }

            // Ingredients Palette clicks
            float palStartX = winX + 16.0f;
            float palStartY = winY + 224.0f;
            float palSlotSize = 25.0f;
            float palSlotPad = 4.0f;

            for (int i = 0; i < CRAFT_PALETTE.length; i++) {
                float px = palStartX + i * (palSlotSize + palSlotPad);
                if (mx >= px && mx <= px + palSlotSize && my >= palStartY && my <= palStartY + palSlotSize) {
                    if (button == GLFW_MOUSE_BUTTON_LEFT) {
                        selectedIngredient = CRAFT_PALETTE[i];
                    }
                    return true;
                }
            }

            // Recipe Book click to auto-fill
            float bookX = winX + 242.0f;
            float bookY = winY + 44.0f;
            float bookW = winW - 258.0f;
            List<CraftingManager.RecipeInfo> recipes = CraftingManager.getAllRecipes();

            for (int i = 0; i < 7; i++) {
                int rIdx = recipeScroll + i;
                if (rIdx >= recipes.size()) break;
                CraftingManager.RecipeInfo recipe = recipes.get(rIdx);
                float rx = bookX;
                float ry = bookY + 24.0f + i * 27.0f;
                float rw = bookW;
                float rh = 25.0f;

                if (mx >= rx && mx <= rx + rw && my >= ry && my <= ry + rh) {
                    if (button == GLFW_MOUSE_BUTTON_LEFT) {
                        // Apply recipe to craft grid
                        clearCraftGrid();
                        BlockType[][] preview = recipe.previewGrid();
                        int pRows = Math.min(3, preview.length);
                        int pCols = Math.min(3, preview[0].length);
                        if (pRows > 2 || pCols > 2) {
                            craftingMode = 1; // switch to 3x3 table
                        }
                        for (int r = 0; r < pRows; r++) {
                            for (int c = 0; c < pCols; c++) {
                                craftGrid[r][c] = preview[r][c] != null ? preview[r][c] : BlockType.AIR;
                            }
                        }
                    }
                    return true;
                }
            }

            return true;
        }

        return true;
    }

    public BlockType getHoveredBlock(float mx, float my) {
        if (currentTab != 0) return null;
        float winW = COLS * SLOT_SIZE + (COLS + 1) * SLOT_PAD;
        float winH = ROWS * SLOT_SIZE + (ROWS + 1) * SLOT_PAD + 120.0f;
        float startX = (lastScreenW - winW) * 0.5f + SLOT_PAD;
        float startY = (lastScreenH - winH) * 0.5f + 44.0f;

        BlockType[] all = BlockType.getCreativeBlocks();
        for (int r = 0; r < ROWS; r++) {
            for (int col = 0; col < COLS; col++) {
                int i = (scrollRow + r) * COLS + col;
                if (i >= all.length) break;
                float sx = startX + col * (SLOT_SIZE + SLOT_PAD);
                float sy = startY + r * (SLOT_SIZE + SLOT_PAD);
                if (mx >= sx && mx <= sx + SLOT_SIZE && my >= sy && my <= sy + SLOT_SIZE) {
                    return all[i];
                }
            }
        }
        return null;
    }

    public int getHoveredHotbarSlot(float mx, float my) {
        float winW = COLS * SLOT_SIZE + (COLS + 1) * SLOT_PAD;
        float winH = ROWS * SLOT_SIZE + (ROWS + 1) * SLOT_PAD + 120.0f;
        float dialogX = (lastScreenW - winW) * 0.5f;
        float dialogY = (lastScreenH - winH) * 0.5f;

        float totalHotbarW = 9 * HOTBAR_SLOT_SIZE + 8 * HOTBAR_SLOT_PAD;
        float hStartX = dialogX + (winW - totalHotbarW) * 0.5f;
        float hStartY = dialogY + winH - 58.0f;

        for (int i = 0; i < 9; i++) {
            float sx = hStartX + i * (HOTBAR_SLOT_SIZE + HOTBAR_SLOT_PAD);
            if (mx >= sx && mx <= sx + HOTBAR_SLOT_SIZE && my >= hStartY && my <= hStartY + HOTBAR_SLOT_SIZE) {
                return i;
            }
        }
        return -1;
    }

    private float lastScreenW = 1280;
    private float lastScreenH = 720;

    public void onResize(int screenW, int screenH) {
        this.lastScreenW = screenW;
        this.lastScreenH = screenH;
    }

    public void render(HudRenderer hud, int screenW, int screenH, double mouseX, double mouseY,
                       Player player, TextureAtlas atlas) {
        if (!open) return;

        glDisable(GL_DEPTH_TEST);
        glDisable(GL_CULL_FACE);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        lastScreenW = screenW;
        lastScreenH = screenH;
        float mx = (float) mouseX;
        float my = (float) mouseY;

        Matrix4f ortho = new Matrix4f().ortho(0, screenW, screenH, 0, -1, 1);

        // 1. Dim background
        hud.getColorShader().bind();
        hud.getColorShader().setUniform("uMVP", ortho);
        hud.getColorShader().setUniform("uColor", new Vector4f(0.02f, 0.04f, 0.07f, 0.72f));
        hud.drawRect2D(0, 0, screenW, screenH);

        // 2. Window container
        float winW = COLS * SLOT_SIZE + (COLS + 1) * SLOT_PAD;
        float winH = ROWS * SLOT_SIZE + (ROWS + 1) * SLOT_PAD + 120.0f;
        float winX = (screenW - winW) * 0.5f;
        float winY = (screenH - winH) * 0.5f;

        // Outer border
        hud.getColorShader().setUniform("uColor", new Vector4f(0.85f, 0.72f, 0.25f, 0.95f));
        hud.drawRect2D(winX - 2, winY - 2, winW + 4, winH + 4);

        // Window background
        hud.getColorShader().setUniform("uColor", new Vector4f(0.10f, 0.12f, 0.16f, 0.96f));
        hud.drawRect2D(winX, winY, winW, winH);

        // Header bar
        hud.getColorShader().setUniform("uColor", new Vector4f(0.16f, 0.19f, 0.25f, 0.98f));
        hud.drawRect2D(winX, winY, winW, 36.0f);

        // Header tabs: [ Creative ] and [ Crafting ]
        float tabW = 96.0f;
        float tabH = 24.0f;
        float tab0X = winX + winW - 204.0f;
        float tab1X = winX + winW - 102.0f;
        float tabY = winY + 6.0f;

        // Tab 0
        boolean hTab0 = (mx >= tab0X && mx <= tab0X + tabW && my >= tabY && my <= tabY + tabH);
        hud.getColorShader().setUniform("uColor", currentTab == 0 ? new Vector4f(0.85f, 0.72f, 0.25f, 1.0f) :
                (hTab0 ? new Vector4f(0.40f, 0.44f, 0.55f, 0.9f) : new Vector4f(0.22f, 0.25f, 0.32f, 0.9f)));
        hud.drawRect2D(tab0X, tabY, tabW, tabH);
        hud.getColorShader().setUniform("uColor", currentTab == 0 ? new Vector4f(0.12f, 0.14f, 0.20f, 1.0f) : new Vector4f(0.16f, 0.18f, 0.24f, 1.0f));
        hud.drawRect2D(tab0X + 1, tabY + 1, tabW - 2, tabH - 2);

        // Tab 1
        boolean hTab1 = (mx >= tab1X && mx <= tab1X + tabW && my >= tabY && my <= tabY + tabH);
        hud.getColorShader().setUniform("uColor", currentTab == 1 ? new Vector4f(0.85f, 0.72f, 0.25f, 1.0f) :
                (hTab1 ? new Vector4f(0.40f, 0.44f, 0.55f, 0.9f) : new Vector4f(0.22f, 0.25f, 0.32f, 0.9f)));
        hud.drawRect2D(tab1X, tabY, tabW, tabH);
        hud.getColorShader().setUniform("uColor", currentTab == 1 ? new Vector4f(0.12f, 0.14f, 0.20f, 1.0f) : new Vector4f(0.16f, 0.18f, 0.24f, 1.0f));
        hud.drawRect2D(tab1X + 1, tabY + 1, tabW - 2, tabH - 2);

        hud.getColorShader().unbind();

        // Title and Tab labels
        String title = (currentTab == 0) ? I18n.t("creative.title") : I18n.t("crafting.title");
        hud.getTextRenderer().drawTextWithShadow(ortho, title, winX + 14.0f, winY + 9.0f, 1.5f,
                new Vector4f(1.0f, 0.92f, 0.40f, 1.0f));

        hud.getTextRenderer().drawTextCenteredInBox(ortho, I18n.t("inventory.tab.creative"), tab0X, tabY, tabW, tabH, 1.2f,
                currentTab == 0 ? new Vector4f(1.0f, 0.92f, 0.40f, 1.0f) : new Vector4f(0.80f, 0.85f, 0.92f, 0.95f));
        hud.getTextRenderer().drawTextCenteredInBox(ortho, I18n.t("inventory.tab.crafting"), tab1X, tabY, tabW, tabH, 1.2f,
                currentTab == 1 ? new Vector4f(1.0f, 0.92f, 0.40f, 1.0f) : new Vector4f(0.80f, 0.85f, 0.92f, 0.95f));

        // 3. Tab Specific Content
        BlockType hoveredBlock = null;

        if (currentTab == 0) {
            // ================== TAB 0: CREATIVE GRID ==================
            BlockType[] all = BlockType.getCreativeBlocks();
            float gridStartX = winX + SLOT_PAD;
            float gridStartY = winY + 44.0f;

            int totalRows = (all.length + COLS - 1) / COLS;
            int maxScrollRow = Math.max(0, totalRows - ROWS);
            scrollRow = Math.max(0, Math.min(maxScrollRow, scrollRow));

            for (int r = 0; r < ROWS; r++) {
                for (int col = 0; col < COLS; col++) {
                    int i = (scrollRow + r) * COLS + col;
                    if (i >= all.length) break;
                    float sx = gridStartX + col * (SLOT_SIZE + SLOT_PAD);
                    float sy = gridStartY + r * (SLOT_SIZE + SLOT_PAD);

                    boolean isHover = (mx >= sx && mx <= sx + SLOT_SIZE && my >= sy && my <= sy + SLOT_SIZE);
                    if (isHover) {
                        hoveredBlock = all[i];
                    }

                    hud.getColorShader().bind();
                    hud.getColorShader().setUniform("uMVP", ortho);
                    if (isHover) {
                        hud.getColorShader().setUniform("uColor", new Vector4f(1.0f, 0.85f, 0.25f, 1.0f));
                        hud.drawRect2D(sx - 2, sy - 2, SLOT_SIZE + 4, SLOT_SIZE + 4);
                    }
                    hud.getColorShader().setUniform("uColor", new Vector4f(0.20f, 0.23f, 0.29f, 0.95f));
                    hud.drawRect2D(sx, sy, SLOT_SIZE, SLOT_SIZE);
                    hud.getColorShader().unbind();

                    // Draw block icon
                    BlockType bt = all[i];
                    if (bt == BlockType.AIR) {
                        hud.getTextRenderer().drawTextWithShadow(ortho, "AIR", sx + 12.0f, sy + 20.0f, 1.5f,
                                new Vector4f(0.8f, 0.4f, 0.4f, 0.9f));
                    } else {
                        hud.getUiTexShader().bind();
                        hud.getUiTexShader().setUniform("uProj", ortho);
                        hud.getUiTexShader().setUniform("uTexture", 0);
                        atlas.bind();

                        int tex = bt.sideTex;
                        hud.drawTexturedQuad2D(sx + 6.0f, sy + 6.0f, SLOT_SIZE - 12.0f, SLOT_SIZE - 12.0f,
                                TextureAtlas.getU0(tex), TextureAtlas.getV0(tex), TextureAtlas.getU1(tex), TextureAtlas.getV1(tex));
                        hud.getUiTexShader().unbind();
                    }
                }
            }

            // Scrollbar
            if (maxScrollRow > 0) {
                float sbX = winX + winW - 8.0f;
                float sbY = gridStartY;
                float sbH = ROWS * (SLOT_SIZE + SLOT_PAD) - SLOT_PAD;
                hud.getColorShader().bind();
                hud.getColorShader().setUniform("uMVP", ortho);
                hud.getColorShader().setUniform("uColor", new Vector4f(0.12f, 0.15f, 0.20f, 0.85f));
                hud.drawRect2D(sbX, sbY, 5.0f, sbH);
                float thumbH = Math.max(20.0f, sbH * ((float) ROWS / totalRows));
                float thumbY = sbY + ((float) scrollRow / maxScrollRow) * (sbH - thumbH);
                hud.getColorShader().setUniform("uColor", new Vector4f(0.85f, 0.72f, 0.25f, 0.95f));
                hud.drawRect2D(sbX, thumbY, 5.0f, thumbH);
                hud.getColorShader().unbind();
            }

        } else {
            // ================== TAB 1: CRAFTING WORKSHOP ==================
            // Left Side: Mode button & Clear button
            float modeBtnX = winX + 16.0f;
            float modeBtnY = winY + 44.0f;
            float modeBtnW = 94.0f;
            float modeBtnH = 22.0f;
            boolean hMode = (mx >= modeBtnX && mx <= modeBtnX + modeBtnW && my >= modeBtnY && my <= modeBtnY + modeBtnH);

            float clearBtnX = winX + 116.0f;
            float clearBtnY = winY + 44.0f;
            float clearBtnW = 60.0f;
            float clearBtnH = 22.0f;
            boolean hClear = (mx >= clearBtnX && mx <= clearBtnX + clearBtnW && my >= clearBtnY && my <= clearBtnY + clearBtnH);

            hud.getColorShader().bind();
            hud.getColorShader().setUniform("uMVP", ortho);
            // Mode toggle
            hud.getColorShader().setUniform("uColor", hMode ? new Vector4f(0.35f, 0.40f, 0.52f, 1.0f) : new Vector4f(0.20f, 0.23f, 0.30f, 0.95f));
            hud.drawRect2D(modeBtnX, modeBtnY, modeBtnW, modeBtnH);
            // Clear
            hud.getColorShader().setUniform("uColor", hClear ? new Vector4f(0.60f, 0.25f, 0.25f, 1.0f) : new Vector4f(0.30f, 0.18f, 0.18f, 0.95f));
            hud.drawRect2D(clearBtnX, clearBtnY, clearBtnW, clearBtnH);
            hud.getColorShader().unbind();

            String modeLabel = (craftingMode == 0) ? I18n.t("crafting.mode_2x2") : I18n.t("crafting.mode_3x3");
            hud.getTextRenderer().drawTextCenteredInBox(ortho, modeLabel, modeBtnX, modeBtnY, modeBtnW, modeBtnH, 1.1f,
                    new Vector4f(1.0f, 1.0f, 1.0f, 1.0f));
            hud.getTextRenderer().drawTextCenteredInBox(ortho, I18n.t("crafting.clear"), clearBtnX, clearBtnY, clearBtnW, clearBtnH, 1.1f,
                    new Vector4f(1.0f, 0.7f, 0.7f, 1.0f));

            // Crafting Grid rendering
            int gridSize = (craftingMode == 0) ? 2 : 3;
            float cSlotSize = (craftingMode == 0) ? 46.0f : 36.0f;
            float cSlotPad = (craftingMode == 0) ? 6.0f : 5.0f;
            float gridStartX = winX + 16.0f;
            float gridStartY = winY + 74.0f;

            for (int r = 0; r < gridSize; r++) {
                for (int c = 0; c < gridSize; c++) {
                    float sx = gridStartX + c * (cSlotSize + cSlotPad);
                    float sy = gridStartY + r * (cSlotSize + cSlotPad);
                    boolean isHover = (mx >= sx && mx <= sx + cSlotSize && my >= sy && my <= sy + cSlotSize);

                    hud.getColorShader().bind();
                    hud.getColorShader().setUniform("uMVP", ortho);
                    if (isHover) {
                        hud.getColorShader().setUniform("uColor", new Vector4f(1.0f, 0.85f, 0.25f, 1.0f));
                        hud.drawRect2D(sx - 2, sy - 2, cSlotSize + 4, cSlotSize + 4);
                    }
                    hud.getColorShader().setUniform("uColor", new Vector4f(0.18f, 0.21f, 0.28f, 0.95f));
                    hud.drawRect2D(sx, sy, cSlotSize, cSlotSize);
                    hud.getColorShader().unbind();

                    BlockType bt = craftGrid[r][c];
                    if (bt != null && bt != BlockType.AIR) {
                        hud.getUiTexShader().bind();
                        hud.getUiTexShader().setUniform("uProj", ortho);
                        hud.getUiTexShader().setUniform("uTexture", 0);
                        atlas.bind();
                        int tex = bt.sideTex;
                        hud.drawTexturedQuad2D(sx + 4.0f, sy + 4.0f, cSlotSize - 8.0f, cSlotSize - 8.0f,
                                TextureAtlas.getU0(tex), TextureAtlas.getV0(tex), TextureAtlas.getU1(tex), TextureAtlas.getV1(tex));
                        hud.getUiTexShader().unbind();
                    }
                    if (isHover && bt != null && bt != BlockType.AIR) {
                        hoveredBlock = bt;
                    }
                }
            }

            // Arrow -> pointing to output
            float arrowX = gridStartX + gridSize * (cSlotSize + cSlotPad) + 10.0f;
            float arrowY = gridStartY + (gridSize * (cSlotSize + cSlotPad)) * 0.5f - 8.0f;
            hud.getTextRenderer().drawTextWithShadow(ortho, "->", arrowX, arrowY, 1.8f, new Vector4f(0.85f, 0.72f, 0.25f, 1.0f));

            // Output Result Slot
            float outSlotSize = 54.0f;
            float outX = gridStartX + gridSize * (cSlotSize + cSlotPad) + 36.0f;
            float outY = gridStartY + (gridSize * (cSlotSize + cSlotPad) - outSlotSize) * 0.5f;
            boolean hOut = (mx >= outX && mx <= outX + outSlotSize && my >= outY && my <= outY + outSlotSize);

            CraftingManager.CraftResult craftResult = getActiveCraftResult();

            hud.getColorShader().bind();
            hud.getColorShader().setUniform("uMVP", ortho);
            if (hOut && craftResult != null) {
                hud.getColorShader().setUniform("uColor", new Vector4f(0.35f, 1.0f, 0.45f, 1.0f));
                hud.drawRect2D(outX - 2, outY - 2, outSlotSize + 4, outSlotSize + 4);
            } else {
                hud.getColorShader().setUniform("uColor", new Vector4f(0.85f, 0.72f, 0.25f, 0.95f));
                hud.drawRect2D(outX - 1, outY - 1, outSlotSize + 2, outSlotSize + 2);
            }
            hud.getColorShader().setUniform("uColor", new Vector4f(0.12f, 0.16f, 0.22f, 0.98f));
            hud.drawRect2D(outX, outY, outSlotSize, outSlotSize);
            hud.getColorShader().unbind();

            if (craftResult != null) {
                hud.getUiTexShader().bind();
                hud.getUiTexShader().setUniform("uProj", ortho);
                hud.getUiTexShader().setUniform("uTexture", 0);
                atlas.bind();
                int tex = craftResult.result().sideTex;
                hud.drawTexturedQuad2D(outX + 6.0f, outY + 6.0f, outSlotSize - 12.0f, outSlotSize - 12.0f,
                        TextureAtlas.getU0(tex), TextureAtlas.getV0(tex), TextureAtlas.getU1(tex), TextureAtlas.getV1(tex));
                hud.getUiTexShader().unbind();

                hud.getTextRenderer().drawTextWithShadow(ortho, "x" + craftResult.count(), outX + outSlotSize - 18.0f, outY + outSlotSize - 13.0f, 1.2f,
                        new Vector4f(1.0f, 0.95f, 0.40f, 1.0f));
                if (hOut) {
                    hoveredBlock = craftResult.result();
                }
            }

            // Ingredients Palette Bar below crafting grid
            float palStartX = winX + 16.0f;
            float palStartY = winY + 224.0f;
            float palSlotSize = 25.0f;
            float palSlotPad = 4.0f;

            hud.getTextRenderer().drawTextWithShadow(ortho, I18n.t("crafting.ingredients"), palStartX, palStartY - 14.0f, 1.1f,
                    new Vector4f(0.85f, 0.88f, 0.95f, 0.9f));

            for (int i = 0; i < CRAFT_PALETTE.length; i++) {
                float px = palStartX + i * (palSlotSize + palSlotPad);
                boolean isHover = (mx >= px && mx <= px + palSlotSize && my >= palStartY && my <= palStartY + palSlotSize);
                boolean isSelected = (CRAFT_PALETTE[i] == selectedIngredient);

                hud.getColorShader().bind();
                hud.getColorShader().setUniform("uMVP", ortho);
                if (isSelected) {
                    hud.getColorShader().setUniform("uColor", new Vector4f(1.0f, 0.85f, 0.20f, 1.0f));
                    hud.drawRect2D(px - 1, palStartY - 1, palSlotSize + 2, palSlotSize + 2);
                } else if (isHover) {
                    hud.getColorShader().setUniform("uColor", new Vector4f(0.6f, 0.7f, 0.9f, 0.9f));
                    hud.drawRect2D(px - 1, palStartY - 1, palSlotSize + 2, palSlotSize + 2);
                }
                hud.getColorShader().setUniform("uColor", new Vector4f(0.20f, 0.23f, 0.29f, 0.95f));
                hud.drawRect2D(px, palStartY, palSlotSize, palSlotSize);
                hud.getColorShader().unbind();

                hud.getUiTexShader().bind();
                hud.getUiTexShader().setUniform("uProj", ortho);
                hud.getUiTexShader().setUniform("uTexture", 0);
                atlas.bind();
                int tex = CRAFT_PALETTE[i].sideTex;
                hud.drawTexturedQuad2D(px + 2.0f, palStartY + 2.0f, palSlotSize - 4.0f, palSlotSize - 4.0f,
                        TextureAtlas.getU0(tex), TextureAtlas.getV0(tex), TextureAtlas.getU1(tex), TextureAtlas.getV1(tex));
                hud.getUiTexShader().unbind();

                if (isHover) {
                    hoveredBlock = CRAFT_PALETTE[i];
                }
            }

            // Right Side: Quick Recipe Book List
            float bookX = winX + 242.0f;
            float bookY = winY + 44.0f;
            float bookW = winW - 258.0f;
            float bookH = 205.0f;

            hud.getColorShader().bind();
            hud.getColorShader().setUniform("uMVP", ortho);
            hud.getColorShader().setUniform("uColor", new Vector4f(0.08f, 0.10f, 0.14f, 0.92f));
            hud.drawRect2D(bookX, bookY, bookW, bookH);
            hud.getColorShader().setUniform("uColor", new Vector4f(0.18f, 0.22f, 0.28f, 0.95f));
            hud.drawRect2D(bookX, bookY, bookW, 20.0f);
            hud.getColorShader().unbind();

            hud.getTextRenderer().drawTextWithShadow(ortho, I18n.t("crafting.recipes"), bookX + 8.0f, bookY + 4.0f, 1.2f,
                    new Vector4f(1.0f, 0.92f, 0.40f, 1.0f));

            List<CraftingManager.RecipeInfo> recipes = CraftingManager.getAllRecipes();
            int maxRecipeScroll = Math.max(0, recipes.size() - 7);
            recipeScroll = Math.max(0, Math.min(maxRecipeScroll, recipeScroll));

            for (int i = 0; i < 7; i++) {
                int rIdx = recipeScroll + i;
                if (rIdx >= recipes.size()) break;
                CraftingManager.RecipeInfo recipe = recipes.get(rIdx);
                float rx = bookX + 3.0f;
                float ry = bookY + 23.0f + i * 26.0f;
                float rw = bookW - 6.0f;
                float rh = 24.0f;

                boolean isHover = (mx >= rx && mx <= rx + rw && my >= ry && my <= ry + rh);

                hud.getColorShader().bind();
                hud.getColorShader().setUniform("uMVP", ortho);
                hud.getColorShader().setUniform("uColor", isHover ? new Vector4f(0.24f, 0.28f, 0.38f, 0.95f) : new Vector4f(0.13f, 0.15f, 0.20f, 0.90f));
                hud.drawRect2D(rx, ry, rw, rh);
                hud.getColorShader().unbind();

                // Draw result icon
                hud.getUiTexShader().bind();
                hud.getUiTexShader().setUniform("uProj", ortho);
                hud.getUiTexShader().setUniform("uTexture", 0);
                atlas.bind();
                int rTex = recipe.result().sideTex;
                hud.drawTexturedQuad2D(rx + 3.0f, ry + 2.0f, 20.0f, 20.0f,
                        TextureAtlas.getU0(rTex), TextureAtlas.getV0(rTex), TextureAtlas.getU1(rTex), TextureAtlas.getV1(rTex));
                hud.getUiTexShader().unbind();

                // Draw recipe text
                String desc = recipe.description();
                hud.getTextRenderer().drawTextWithShadow(ortho, desc, rx + 28.0f, ry + 5.0f, 1.05f,
                        isHover ? new Vector4f(1.0f, 0.95f, 0.40f, 1.0f) : new Vector4f(0.85f, 0.88f, 0.92f, 0.95f));
            }
        }

        // 4. Hotbar preview at bottom - spans full 9 slots cleanly across both tabs
        float totalHotbarW = 9 * HOTBAR_SLOT_SIZE + 8 * HOTBAR_SLOT_PAD;
        float hStartX = winX + (winW - totalHotbarW) * 0.5f;
        float hStartY = winY + winH - 58.0f;

        hud.getColorShader().bind();
        hud.getColorShader().setUniform("uMVP", ortho);
        hud.getColorShader().setUniform("uColor", new Vector4f(0.08f, 0.10f, 0.14f, 0.92f));
        hud.drawRect2D(hStartX - 4.0f, hStartY - 4.0f, totalHotbarW + 8.0f, HOTBAR_SLOT_SIZE + 8.0f);

        for (int i = 0; i < 9; i++) {
            float sx = hStartX + i * (HOTBAR_SLOT_SIZE + HOTBAR_SLOT_PAD);
            if (i == player.selectedSlot) {
                hud.getColorShader().setUniform("uColor", new Vector4f(1.0f, 0.85f, 0.20f, 1.0f));
                hud.drawRect2D(sx - 2, hStartY - 2, HOTBAR_SLOT_SIZE + 4, HOTBAR_SLOT_SIZE + 4);
            }
            hud.getColorShader().setUniform("uColor", new Vector4f(0.22f, 0.25f, 0.32f, 0.95f));
            hud.drawRect2D(sx, hStartY, HOTBAR_SLOT_SIZE, HOTBAR_SLOT_SIZE);
        }
        hud.getColorShader().unbind();

        for (int i = 0; i < 9; i++) {
            float sx = hStartX + i * (HOTBAR_SLOT_SIZE + HOTBAR_SLOT_PAD);
            BlockType bt = player.hotbar[i];
            if (bt != null && bt != BlockType.AIR) {
                hud.getUiTexShader().bind();
                hud.getUiTexShader().setUniform("uProj", ortho);
                hud.getUiTexShader().setUniform("uTexture", 0);
                atlas.bind();
                int tex = bt.sideTex;
                hud.drawTexturedQuad2D(sx + 5.0f, hStartY + 5.0f, HOTBAR_SLOT_SIZE - 10.0f, HOTBAR_SLOT_SIZE - 10.0f,
                        TextureAtlas.getU0(tex), TextureAtlas.getV0(tex), TextureAtlas.getU1(tex), TextureAtlas.getV1(tex));
                hud.getUiTexShader().unbind();
            } else if (bt == BlockType.AIR) {
                hud.getTextRenderer().drawTextWithShadow(ortho, "AIR", sx + 10.0f, hStartY + 16.0f, 1.1f,
                        new Vector4f(0.6f, 0.6f, 0.6f, 0.8f));
            }
            hud.getTextRenderer().drawTextWithShadow(ortho, String.valueOf(i + 1), sx + 3.0f, hStartY + 3.0f, 1.1f,
                    new Vector4f(1.0f, 1.0f, 0.5f, 0.85f));
        }

        // 5. Tooltip on hover: concise block name
        if (hoveredBlock != null) {
            String blockName = I18n.t("block." + hoveredBlock.name().toLowerCase());
            if (blockName.startsWith("block.")) {
                blockName = hoveredBlock.name;
            }
            float titleW = hud.getTextRenderer().getTextWidth(blockName, 1.4f);
            float tooltipW = titleW + 16.0f;
            float tooltipH = 26.0f;

            float tx = Math.min(screenW - tooltipW - 10, mx + 16.0f);
            float ty = Math.min(screenH - tooltipH - 10, my + 16.0f);

            hud.getColorShader().bind();
            hud.getColorShader().setUniform("uMVP", ortho);
            hud.getColorShader().setUniform("uColor", new Vector4f(0.95f, 0.85f, 0.30f, 0.95f));
            hud.drawRect2D(tx - 1, ty - 1, tooltipW + 2, tooltipH + 2);
            hud.getColorShader().setUniform("uColor", new Vector4f(0.06f, 0.08f, 0.12f, 0.95f));
            hud.drawRect2D(tx, ty, tooltipW, tooltipH);
            hud.getColorShader().unbind();

            hud.getTextRenderer().drawTextWithShadow(ortho, blockName, tx + 8.0f, ty + 5.0f, 1.4f,
                    new Vector4f(1.0f, 0.92f, 0.40f, 1.0f));
        }

        glEnable(GL_CULL_FACE);
        glEnable(GL_DEPTH_TEST);
    }
}
