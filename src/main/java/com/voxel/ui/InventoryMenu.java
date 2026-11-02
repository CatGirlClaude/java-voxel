package com.voxel.ui;

import com.voxel.block.BlockType;
import com.voxel.entity.Player;
import com.voxel.i18n.I18n;
import com.voxel.render.TextureAtlas;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;

public class InventoryMenu {
    private boolean open = false;
    private int scrollRow = 0;

    public static final int COLS = 9;
    public static final int ROWS = 5;
    public static final float SLOT_SIZE = 48.0f;
    public static final float SLOT_PAD = 6.0f;

    // Hotbar preview metrics inside creative window
    public static final float HOTBAR_SLOT_SIZE = 44.0f;
    public static final float HOTBAR_SLOT_PAD = 6.0f;

    public boolean isOpen() {
        return open;
    }

    public void setOpen(boolean open) {
        this.open = open;
    }

    public void toggle() {
        this.open = !this.open;
    }

    public boolean handleScroll(double yoffset) {
        if (!open) return false;
        int totalRows = (BlockType.getCreativeBlocks().length + COLS - 1) / COLS;
        int maxScrollRow = Math.max(0, totalRows - ROWS);
        if (yoffset > 0) {
            scrollRow = Math.max(0, scrollRow - 1);
            return true;
        } else if (yoffset < 0) {
            scrollRow = Math.min(maxScrollRow, scrollRow + 1);
            return true;
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
        if (button != GLFW_MOUSE_BUTTON_LEFT) return true;

        float mx = (float) mouseX;
        float my = (float) mouseY;

        // Check creative grid blocks
        BlockType clicked = getHoveredBlock(mx, my);
        if (clicked != null) {
            player.setHotbarSlot(player.selectedSlot, clicked);
            return true;
        }

        // Check hotbar slots at bottom
        int hotbarSlot = getHoveredHotbarSlot(mx, my);
        if (hotbarSlot >= 0 && hotbarSlot < player.hotbar.length) {
            player.selectedSlot = hotbarSlot;
            return true;
        }

        return true;
    }

    public BlockType getHoveredBlock(float mx, float my) {
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
        hud.getColorShader().unbind();

        // Title
        hud.getTextRenderer().drawTextWithShadow(ortho, I18n.t("creative.title"), winX + 14.0f, winY + 9.0f, 1.6f,
                new Vector4f(1.0f, 0.92f, 0.40f, 1.0f));

        // 3. Render grid slots (9 columns)
        BlockType[] all = BlockType.getCreativeBlocks();
        float gridStartX = winX + SLOT_PAD;
        float gridStartY = winY + 44.0f;
        BlockType hoveredBlock = null;

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
                    float u0 = TextureAtlas.getU0(tex);
                    float v0 = TextureAtlas.getV0(tex);
                    float u1 = TextureAtlas.getU1(tex);
                    float v1 = TextureAtlas.getV1(tex);
                    hud.drawTexturedQuad2D(sx + 6.0f, sy + 6.0f, SLOT_SIZE - 12.0f, SLOT_SIZE - 12.0f, u0, v0, u1, v1);
                    hud.getUiTexShader().unbind();
                }
            }
        }

        // Optional scrollbar if creative rows exceed visible rows
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

        // 4. Hotbar preview at bottom - spans full 9 slots cleanly inside creative menu
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
