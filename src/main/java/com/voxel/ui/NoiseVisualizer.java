package com.voxel.ui;

import com.voxel.entity.Player;
import com.voxel.math.SimplexNoise;
import com.voxel.world.Biome;
import com.voxel.world.Chunk;
import com.voxel.world.World;
import com.voxel.world.gen.TerrainGenerator;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.lwjgl.BufferUtils;

import java.nio.ByteBuffer;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;

public class NoiseVisualizer {
    public enum Mode {
        BIOME("Biomy", "Podzial na biomy (kolory biomow)"),
        HEIGHT("Wysokosc terenu", "Hypsometria: niebieski ocean, zielen, braz, snieg"),
        TEMPERATURE("Temperatura", "Klimat termiczny: niebieski zimbny -> czerwony goracy"),
        HUMIDITY("Wilgotnosc", "Klimat wilgotnosciowy: braz/zolc suchy -> blekit wilgotny"),
        CONTINENTAL("Kontynentalnosc", "Formowanie ladow i oceanow"),
        MOUNTAINS("Pasma gorskie", "Rozklad lancuchow gorskich");

        public final String title;
        public final String description;

        Mode(String title, String description) {
            this.title = title;
            this.description = description;
        }
    }

    private static final int MAP_SIZE = 192; // 192x192 pixels texture
    private int mapTextureId = 0;
    private final ByteBuffer pixelBuffer = BufferUtils.createByteBuffer(MAP_SIZE * MAP_SIZE * 4);

    private boolean open = false;
    private Mode currentMode = Mode.BIOME;

    // Map navigation
    private long centerWx = 0;
    private long centerWz = 0;
    private int blocksPerPixel = 2; // 1, 2, 4, 8, 16, 32
    private boolean textureDirty = true;
    private long lastGeneratedSeed = Long.MIN_VALUE;

    // Mouse drag state
    private boolean isDragging = false;
    private double lastDragMouseX = 0;
    private double lastDragMouseY = 0;

    // Render layout cache
    private float lastScreenW = 1280;
    private float lastScreenH = 720;
    private float mapScreenX = 0;
    private float mapScreenY = 0;
    private float mapScreenW = 0;
    private float mapScreenH = 0;

    // Context menu state (RCM on map)
    private boolean contextMenuOpen = false;
    private float contextMenuX = 0;
    private float contextMenuY = 0;
    private long contextWx = 0;
    private long contextWz = 0;

    // Waypoint state
    private boolean hasWaypoint = false;
    private long waypointWx = 0;
    private long waypointWz = 0;

    public static final String[] CONTEXT_MENU_ITEMS = {
        "1. Teleportuj gracza tutaj",
        "2. Wycentruj widok mapy",
        "3. Ustaw cel (Waypoint)",
        "4. Zbadaj biom i wysokosc",
        "5. Usun aktywny waypoint",
        "6. Anuluj / Zamknij"
    };

    public boolean hasWaypoint() { return hasWaypoint; }
    public long getWaypointWx() { return waypointWx; }
    public long getWaypointWz() { return waypointWz; }
    public void clearWaypoint() { hasWaypoint = false; }

    public boolean isOpen() {
        return open;
    }

    public void setOpen(boolean open) {
        this.open = open;
        if (open) {
            textureDirty = true;
        } else {
            contextMenuOpen = false;
        }
    }

    public void toggle() {
        setOpen(!open);
    }

    public void setMode(Mode mode) {
        if (this.currentMode != mode) {
            this.currentMode = mode;
            this.textureDirty = true;
        }
    }

    public void centerOnPlayer(Player player) {
        if (player != null) {
            this.centerWx = (long) Math.floor(player.position.x);
            this.centerWz = (long) Math.floor(player.position.z);
            this.textureDirty = true;
        }
    }

    public void zoomIn() {
        if (blocksPerPixel > 1) {
            blocksPerPixel /= 2;
            textureDirty = true;
        }
    }

    public void zoomOut() {
        if (blocksPerPixel < 32) {
            blocksPerPixel *= 2;
            textureDirty = true;
        }
    }

    public void pan(long dWx, long dWz) {
        this.centerWx += dWx;
        this.centerWz += dWz;
        this.textureDirty = true;
    }

    public boolean handleKey(int key, Player player) {
        if (!open) return false;

        if (key == GLFW_KEY_ESCAPE) {
            if (contextMenuOpen) {
                contextMenuOpen = false;
                return true;
            }
            open = false;
            return true;
        }

        if (key == GLFW_KEY_M || key == GLFW_KEY_F4) {
            open = false;
            contextMenuOpen = false;
            return true;
        }

        // Layer selection 1..6
        if (key == GLFW_KEY_1) { setMode(Mode.BIOME); return true; }
        if (key == GLFW_KEY_2) { setMode(Mode.HEIGHT); return true; }
        if (key == GLFW_KEY_3) { setMode(Mode.TEMPERATURE); return true; }
        if (key == GLFW_KEY_4) { setMode(Mode.HUMIDITY); return true; }
        if (key == GLFW_KEY_5) { setMode(Mode.CONTINENTAL); return true; }
        if (key == GLFW_KEY_6) { setMode(Mode.MOUNTAINS); return true; }

        // Zoom controls
        if (key == GLFW_KEY_EQUAL || key == GLFW_KEY_KP_ADD) { zoomIn(); return true; }
        if (key == GLFW_KEY_MINUS || key == GLFW_KEY_KP_SUBTRACT) { zoomOut(); return true; }

        // Center on player
        if (key == GLFW_KEY_C) { centerOnPlayer(player); return true; }

        // Pan with arrows or WASD
        long panStep = (long) (blocksPerPixel * 16);
        if (key == GLFW_KEY_LEFT || key == GLFW_KEY_A) { pan(-panStep, 0); return true; }
        if (key == GLFW_KEY_RIGHT || key == GLFW_KEY_D) { pan(panStep, 0); return true; }
        if (key == GLFW_KEY_UP || key == GLFW_KEY_W) { pan(0, -panStep); return true; }
        if (key == GLFW_KEY_DOWN || key == GLFW_KEY_S) { pan(0, panStep); return true; }

        return true;
    }

    public boolean handleScroll(double yoffset) {
        if (!open) return false;
        if (yoffset > 0) {
            zoomIn();
        } else if (yoffset < 0) {
            zoomOut();
        }
        return true;
    }

    public boolean handleMouseButton(int button, int action, double mouseX, double mouseY, Player player) {
        return handleMouseButton(button, action, mouseX, mouseY, player, null, null);
    }

    public boolean handleMouseButton(int button, int action, double mouseX, double mouseY,
                                     Player player, World world, ChatSystem chat) {
        if (!open) return false;

        float mx = (float) mouseX;
        float my = (float) mouseY;

        // 1. If Context Menu is open, process clicks on it first
        if (contextMenuOpen) {
            float cmW = 270.0f;
            float itemH = 26.0f;
            float headerH = 28.0f;
            float padding = 6.0f;
            float cmH = headerH + CONTEXT_MENU_ITEMS.length * itemH + padding * 2.0f;
            float cmX = Math.max(10.0f, Math.min(contextMenuX, lastScreenW - cmW - 10.0f));
            float cmY = Math.max(10.0f, Math.min(contextMenuY, lastScreenH - cmH - 10.0f));

            if (button == GLFW_MOUSE_BUTTON_LEFT && action == GLFW_PRESS) {
                for (int i = 0; i < CONTEXT_MENU_ITEMS.length; i++) {
                    float iy = cmY + headerH + padding + i * itemH;
                    if (mx >= cmX + 4.0f && mx <= cmX + cmW - 4.0f && my >= iy && my <= iy + itemH - 2.0f) {
                        executeContextMenuAction(i, player, world, chat);
                        return true;
                    }
                }
                // Clicked outside context menu: dismiss it
                contextMenuOpen = false;
                return true;
            } else if (button == GLFW_MOUSE_BUTTON_RIGHT && action == GLFW_PRESS) {
                // If right-clicked on map again, relocate context menu to new coordinate
                if (mx >= mapScreenX && mx <= mapScreenX + mapScreenW && my >= mapScreenY && my <= mapScreenY + mapScreenH) {
                    float fracX = (mx - mapScreenX) / mapScreenW;
                    float fracZ = (my - mapScreenY) / mapScreenH;
                    long totalSpan = (long) MAP_SIZE * blocksPerPixel;
                    long halfSpan = totalSpan / 2;
                    long startWx = centerWx - halfSpan;
                    long startWz = centerWz - halfSpan;

                    contextWx = startWx + (long) (fracX * totalSpan);
                    contextWz = startWz + (long) (fracZ * totalSpan);
                    contextMenuX = mx;
                    contextMenuY = my;
                    contextMenuOpen = true;
                    return true;
                } else {
                    contextMenuOpen = false;
                    return true;
                }
            }
        }

        // 2. Right click on Map Viewport -> Open Context Menu at clicked world coordinate
        if (button == GLFW_MOUSE_BUTTON_RIGHT && action == GLFW_PRESS) {
            if (mx >= mapScreenX && mx <= mapScreenX + mapScreenW && my >= mapScreenY && my <= mapScreenY + mapScreenH) {
                float fracX = (mx - mapScreenX) / mapScreenW;
                float fracZ = (my - mapScreenY) / mapScreenH;
                long totalSpan = (long) MAP_SIZE * blocksPerPixel;
                long halfSpan = totalSpan / 2;
                long startWx = centerWx - halfSpan;
                long startWz = centerWz - halfSpan;

                contextWx = startWx + (long) (fracX * totalSpan);
                contextWz = startWz + (long) (fracZ * totalSpan);
                contextMenuX = mx;
                contextMenuY = my;
                contextMenuOpen = true;
                return true;
            }
        }

        // 3. Left click interactions (drag, buttons)
        if (button == GLFW_MOUSE_BUTTON_LEFT) {
            if (action == GLFW_PRESS) {
                // Check if clicked inside map viewport for dragging
                if (mx >= mapScreenX && mx <= mapScreenX + mapScreenW && my >= mapScreenY && my <= mapScreenY + mapScreenH) {
                    isDragging = true;
                    lastDragMouseX = mouseX;
                    lastDragMouseY = mouseY;
                    return true;
                }

                // Check mode buttons
                Mode[] modes = Mode.values();
                float btnY = mapScreenY + mapScreenH + 10.0f;
                float btnW = 76.0f;
                float btnH = 26.0f;
                for (int i = 0; i < modes.length; i++) {
                    float bx = mapScreenX + i * (btnW + 6.0f);
                    if (mx >= bx && mx <= bx + btnW && my >= btnY && my <= btnY + btnH) {
                        setMode(modes[i]);
                        return true;
                    }
                }

                // Check action buttons: [ZOOM +] [ZOOM -] [CENTRUJ] [ZAMKNIJ]
                float actY = btnY + btnH + 8.0f;
                float bZInX = mapScreenX;
                float bZOutX = bZInX + 70.0f;
                float bCentX = bZOutX + 70.0f;
                if (mx >= bZInX && mx <= bZInX + 64.0f && my >= actY && my <= actY + 24.0f) {
                    zoomIn();
                    return true;
                }
                if (mx >= bZOutX && mx <= bZOutX + 64.0f && my >= actY && my <= actY + 24.0f) {
                    zoomOut();
                    return true;
                }
                if (mx >= bCentX && mx <= bCentX + 110.0f && my >= actY && my <= actY + 24.0f) {
                    centerOnPlayer(player);
                    return true;
                }

                // Close button top right
                float closeBtnX = lastScreenW - 140.0f;
                float closeBtnY = 20.0f;
                if (mx >= closeBtnX && mx <= closeBtnX + 110.0f && my >= closeBtnY && my <= closeBtnY + 28.0f) {
                    open = false;
                    contextMenuOpen = false;
                    return true;
                }
            } else if (action == GLFW_RELEASE) {
                isDragging = false;
            }
            return true;
        }

        return true;
    }

    private void executeContextMenuAction(int actionIndex, Player player, World world, ChatSystem chat) {
        if (world == null || player == null) return;
        SimplexNoise noise = world.getNoise();

        switch (actionIndex) {
            case 0 -> { // Teleportuj gracza tutaj
                int surfaceY = (world != null) ? world.getSurfaceHeight(contextWx, contextWz)
                        : TerrainGenerator.computeSurfaceHeight(noise, contextWx, contextWz);
                player.position.set((double) contextWx + 0.5, (double) surfaceY + 1.0, (double) contextWz + 0.5);
                player.velocity.set(0, 0, 0);
                player.onGround = true;
                if (chat != null) {
                    chat.addMessage(String.format("Przeteleportowano do: X=%d Y=%d Z=%d", contextWx, surfaceY + 1, contextWz),
                            ChatSystem.COLOR_SUCCESS);
                }
                open = false;
                contextMenuOpen = false;
            }
            case 1 -> { // Wycentruj widok mapy
                centerWx = contextWx;
                centerWz = contextWz;
                textureDirty = true;
                contextMenuOpen = false;
                if (chat != null) {
                    chat.addMessage(String.format("Wycentrowano widok mapy na: X=%d Z=%d", contextWx, contextWz),
                            ChatSystem.COLOR_INFO);
                }
            }
            case 2 -> { // Ustaw cel (Waypoint)
                waypointWx = contextWx;
                waypointWz = contextWz;
                hasWaypoint = true;
                contextMenuOpen = false;
                double dist = Math.hypot(player.position.x - contextWx, player.position.z - contextWz);
                if (chat != null) {
                    chat.addMessage(String.format("Ustawiono punkt docelowy: X=%d Z=%d (Dystans: %.1f m)",
                            contextWx, contextWz, dist), ChatSystem.COLOR_INFO);
                }
            }
            case 3 -> { // Zbadaj biom i wysokosc
                Biome biome = TerrainGenerator.computeBiome(noise, contextWx, contextWz);
                int h = (world != null) ? world.getSurfaceHeight(contextWx, contextWz)
                        : TerrainGenerator.computeSurfaceHeight(noise, contextWx, contextWz);
                double temp = TerrainGenerator.sampleTemperature(noise, contextWx, contextWz);
                double hum = TerrainGenerator.sampleHumidity(noise, contextWx, contextWz);
                double cont = TerrainGenerator.sampleContinental(noise, contextWx, contextWz);
                if (chat != null) {
                    chat.addMessage(String.format("Punkt [%d, %d]: Biom: %s | Y=%d | Temp: %+.2f | Wilg: %+.2f | Kont: %+.2f",
                            contextWx, contextWz, biome.displayName, h, temp, hum, cont), ChatSystem.COLOR_INFO);
                }
                contextMenuOpen = false;
            }
            case 4 -> { // Usun aktywny waypoint
                if (hasWaypoint) {
                    hasWaypoint = false;
                    if (chat != null) chat.addMessage("Usunieto aktywny punkt docelowy (Waypoint).", ChatSystem.COLOR_HELP);
                } else {
                    if (chat != null) chat.addMessage("Brak aktywnego waypointa.", ChatSystem.COLOR_HELP);
                }
                contextMenuOpen = false;
            }
            case 5 -> { // Anuluj / Zamknij
                contextMenuOpen = false;
            }
        }
    }

    public void handleMouseMove(double mouseX, double mouseY) {
        if (!open) return;
        if (isDragging) {
            double dx = mouseX - lastDragMouseX;
            double dy = mouseY - lastDragMouseY;
            lastDragMouseX = mouseX;
            lastDragMouseY = mouseY;

            // Convert screen drag deltas to world coordinate offset
            double blocksPerScreenPx = (double) (MAP_SIZE * blocksPerPixel) / mapScreenW;
            long dWx = (long) Math.round(-dx * blocksPerScreenPx);
            long dWz = (long) Math.round(-dy * blocksPerScreenPx);
            if (dWx != 0 || dWz != 0) {
                centerWx += dWx;
                centerWz += dWz;
                textureDirty = true;
            }
        }
    }

    private void ensureTexture() {
        if (mapTextureId == 0) {
            mapTextureId = glGenTextures();
            glBindTexture(GL_TEXTURE_2D, mapTextureId);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, MAP_SIZE, MAP_SIZE, 0, GL_RGBA, GL_UNSIGNED_BYTE, pixelBuffer);
            glBindTexture(GL_TEXTURE_2D, 0);
        }
    }

    private void updateTexture(World world) {
        if (world == null || (!textureDirty && lastGeneratedSeed == world.getSeed())) {
            return;
        }
        ensureTexture();

        SimplexNoise noise = world.getNoise();
        long halfSpan = (long) (MAP_SIZE / 2) * blocksPerPixel;
        long startWx = centerWx - halfSpan;
        long startWz = centerWz - halfSpan;

        pixelBuffer.clear();

        for (int py = 0; py < MAP_SIZE; py++) {
            long wz = startWz + (long) py * blocksPerPixel;
            for (int px = 0; px < MAP_SIZE; px++) {
                long wx = startWx + (long) px * blocksPerPixel;

                int r = 0, g = 0, b = 0;
                switch (currentMode) {
                    case BIOME -> {
                        Biome biome = TerrainGenerator.computeBiome(noise, wx, wz);
                        int rgb = getBiomeColor(biome);
                        r = (rgb >> 16) & 0xFF;
                        g = (rgb >> 8) & 0xFF;
                        b = rgb & 0xFF;
                    }
                    case HEIGHT -> {
                        int h = TerrainGenerator.computeSurfaceHeight(noise, wx, wz);
                        if (h < Chunk.SEA_LEVEL) {
                            float t = Math.max(0.0f, Math.min(1.0f, (h - (Chunk.SEA_LEVEL - 25.0f)) / 25.0f));
                            r = (int) (15 + t * 25);
                            g = (int) (40 + t * 95);
                            b = (int) (110 + t * 125);
                        } else if (h == Chunk.SEA_LEVEL) {
                            r = 215; g = 205; b = 145;
                        } else if (h < 40) {
                            float t = (h - Chunk.SEA_LEVEL) / 12.0f;
                            r = (int) (70 + t * 35);
                            g = (int) (160 - t * 20);
                            b = (int) (45 - t * 10);
                        } else if (h < 50) {
                            float t = (h - 40) / 10.0f;
                            r = (int) (105 + t * 30);
                            g = (int) (140 - t * 30);
                            b = (int) (35 + t * 40);
                        } else if (h < 56) {
                            float t = (h - 50) / 6.0f;
                            r = (int) (135 + t * 25);
                            g = (int) (110 + t * 50);
                            b = (int) (75 + t * 85);
                        } else {
                            r = 235; g = 240; b = 250;
                        }
                    }
                    case TEMPERATURE -> {
                        double temp = TerrainGenerator.sampleTemperature(noise, wx, wz);
                        float t = (float) Math.max(0.0, Math.min(1.0, (temp + 0.45) / 0.90));
                        int rgb = getThermalColor(t);
                        r = (rgb >> 16) & 0xFF;
                        g = (rgb >> 8) & 0xFF;
                        b = rgb & 0xFF;
                    }
                    case HUMIDITY -> {
                        double hum = TerrainGenerator.sampleHumidity(noise, wx, wz);
                        float t = (float) Math.max(0.0, Math.min(1.0, (hum + 0.45) / 0.90));
                        int rgb = getHydrologicalColor(t);
                        r = (rgb >> 16) & 0xFF;
                        g = (rgb >> 8) & 0xFF;
                        b = rgb & 0xFF;
                    }
                    case CONTINENTAL -> {
                        double cont = TerrainGenerator.sampleContinental(noise, wx, wz);
                        if (cont < -0.22) {
                            r = 18; g = 32; b = 92;
                        } else if (cont < -0.05) {
                            float t = (float) ((cont + 0.22) / 0.17);
                            r = (int) (22 + t * 30);
                            g = (int) (55 + t * 80);
                            b = (int) (125 + t * 80);
                        } else if (cont < 0.01) {
                            float t = (float) ((cont + 0.05) / 0.06);
                            r = (int) (52 + t * 40);
                            g = (int) (135 + t * 35);
                            b = (int) (205 - t * 140);
                        } else {
                            float t = (float) Math.min(1.0, (cont - 0.01) / 0.35);
                            r = (int) (92 + t * 90);
                            g = (int) (170 + t * 30);
                            b = (int) (65 + t * 135);
                        }
                    }
                    case MOUNTAINS -> {
                        double mount = TerrainGenerator.sampleMountain(noise, wx, wz);
                        float val = (float) Math.max(0.0, Math.min(1.0, (mount + 0.30) / 0.80));
                        if (mount > 0.27) {
                            r = (int) (180 + (val - 0.7f) * 250);
                            g = (int) (185 + (val - 0.7f) * 230);
                            b = (int) (205 + (val - 0.7f) * 165);
                        } else {
                            r = (int) (val * 110);
                            g = (int) (val * 125);
                            b = (int) (val * 140);
                        }
                    }
                }

                pixelBuffer.put((byte) Math.max(0, Math.min(255, r)));
                pixelBuffer.put((byte) Math.max(0, Math.min(255, g)));
                pixelBuffer.put((byte) Math.max(0, Math.min(255, b)));
                pixelBuffer.put((byte) 255);
            }
        }

        pixelBuffer.flip();
        glBindTexture(GL_TEXTURE_2D, mapTextureId);
        glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, MAP_SIZE, MAP_SIZE, GL_RGBA, GL_UNSIGNED_BYTE, pixelBuffer);
        glBindTexture(GL_TEXTURE_2D, 0);

        textureDirty = false;
        lastGeneratedSeed = world.getSeed();
    }

    private static int getBiomeColor(Biome biome) {
        return switch (biome) {
            case DEEP_OCEAN -> 0x0C1F63;
            case OCEAN -> 0x1E52A8;
            case FROZEN_OCEAN -> 0x5D9CD4;
            case BEACH -> 0xF6D87C;
            case PLAINS -> 0x68B237;
            case FOREST -> 0x24742B;
            case BIRCH_FOREST -> 0x54A85C;
            case DESERT -> 0xDAC76C;
            case MESA -> 0xC25B2D;
            case TAIGA -> 0x2A6251;
            case SNOWY_TUNDRA -> 0xE1ECF5;
            case MOUNTAINS -> 0x767D87;
            case JUNGLE -> 0x1A9438;
            case SWAMP -> 0x335C3D;
        };
    }

    private static int getThermalColor(float t) {
        // Deep blue -> Cyan -> Green -> Yellow -> Red
        if (t < 0.25f) {
            float k = t / 0.25f;
            return ((int) (20 * (1 - k) + 30 * k) << 16) | ((int) (40 * (1 - k) + 160 * k) << 8) | ((int) (180 * (1 - k) + 240 * k));
        } else if (t < 0.50f) {
            float k = (t - 0.25f) / 0.25f;
            return ((int) (30 * (1 - k) + 60 * k) << 16) | ((int) (160 * (1 - k) + 200 * k) << 8) | ((int) (240 * (1 - k) + 50 * k));
        } else if (t < 0.75f) {
            float k = (t - 0.50f) / 0.25f;
            return ((int) (60 * (1 - k) + 230 * k) << 16) | ((int) (200 * (1 - k) + 200 * k) << 8) | ((int) (50 * (1 - k) + 25 * k));
        } else {
            float k = (t - 0.75f) / 0.25f;
            return ((int) (230 * (1 - k) + 240 * k) << 16) | ((int) (200 * (1 - k) + 35 * k) << 8) | ((int) (25 * (1 - k) + 25 * k));
        }
    }

    private static int getHydrologicalColor(float t) {
        // Arid warm brown/yellow -> Pale green -> Cyan -> Deep rich blue
        if (t < 0.33f) {
            float k = t / 0.33f;
            return ((int) (190 * (1 - k) + 175 * k) << 16) | ((int) (140 * (1 - k) + 190 * k) << 8) | ((int) (50 * (1 - k) + 65 * k));
        } else if (t < 0.66f) {
            float k = (t - 0.33f) / 0.33f;
            return ((int) (175 * (1 - k) + 40 * k) << 16) | ((int) (190 * (1 - k) + 170 * k) << 8) | ((int) (65 * (1 - k) + 190 * k));
        } else {
            float k = (t - 0.66f) / 0.34f;
            return ((int) (40 * (1 - k) + 18 * k) << 16) | ((int) (170 * (1 - k) + 65 * k) << 8) | ((int) (190 * (1 - k) + 235 * k));
        }
    }

    public void render(HudRenderer hud, int screenW, int screenH, double mouseX, double mouseY,
                       Player player, World world) {
        if (!open || world == null) return;

        glDisable(GL_DEPTH_TEST);
        glDisable(GL_CULL_FACE);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        lastScreenW = screenW;
        lastScreenH = screenH;
        updateTexture(world);

        Matrix4f ortho = new Matrix4f().ortho(0, screenW, screenH, 0, -1, 1);

        // 1. Dim background
        hud.getColorShader().bind();
        hud.getColorShader().setUniform("uMVP", ortho);
        hud.getColorShader().setUniform("uColor", new Vector4f(0.02f, 0.04f, 0.08f, 0.88f));
        hud.drawRect2D(0, 0, screenW, screenH);

        // 2. Main Window Dimensions
        float winMargin = 28.0f;
        float winW = screenW - winMargin * 2.0f;
        float winH = screenH - winMargin * 2.0f;
        float winX = winMargin;
        float winY = winMargin;

        // Outer border and container
        hud.getColorShader().setUniform("uColor", new Vector4f(0.24f, 0.68f, 0.95f, 0.95f));
        hud.drawRect2D(winX - 2, winY - 2, winW + 4, winH + 4);
        hud.getColorShader().setUniform("uColor", new Vector4f(0.08f, 0.11f, 0.16f, 0.98f));
        hud.drawRect2D(winX, winY, winW, winH);

        // Header bar
        hud.getColorShader().setUniform("uColor", new Vector4f(0.12f, 0.17f, 0.25f, 0.98f));
        hud.drawRect2D(winX, winY, winW, 46.0f);
        hud.getColorShader().unbind();

        // Header title and close shortcut
        hud.getTextRenderer().drawTextWithShadow(ortho, "DIAGNOSTYKA GENERATORA SWIATA I BIOMOW (2D NOISE INSPECTOR)", winX + 16.0f, winY + 10.0f, 1.8f,
                new Vector4f(0.40f, 0.85f, 1.0f, 1.0f));
        hud.getTextRenderer().drawTextWithShadow(ortho, "Warstwa: " + currentMode.title + " | " + currentMode.description, winX + 16.0f, winY + 28.0f, 1.2f,
                new Vector4f(0.78f, 0.86f, 0.94f, 0.85f));

        // Close button top right
        float closeBtnX = winX + winW - 130.0f;
        float closeBtnY = winY + 8.0f;
        hud.getColorShader().bind();
        hud.getColorShader().setUniform("uColor", new Vector4f(0.70f, 0.20f, 0.20f, 0.95f));
        hud.drawRect2D(closeBtnX, closeBtnY, 118.0f, 30.0f);
        hud.getColorShader().unbind();
        hud.getTextRenderer().drawTextWithShadow(ortho, "ZAMKNIJ [ESC]", closeBtnX + 12.0f, closeBtnY + 8.0f, 1.3f,
                new Vector4f(1.0f, 1.0f, 1.0f, 1.0f));

        // 3. Left side: Map Viewport
        mapScreenX = winX + 18.0f;
        mapScreenY = winY + 58.0f;
        mapScreenH = Math.min(winH - 140.0f, winW * 0.52f);
        mapScreenW = mapScreenH; // Square map

        // Map Border
        hud.getColorShader().bind();
        hud.getColorShader().setUniform("uColor", new Vector4f(0.40f, 0.50f, 0.65f, 0.90f));
        hud.drawRect2D(mapScreenX - 2, mapScreenY - 2, mapScreenW + 4, mapScreenH + 4);

        // Render Textured Map Quad
        hud.getUiTexShader().bind();
        hud.getUiTexShader().setUniform("uMVP", ortho);
        hud.getUiTexShader().setUniform("uColor", new Vector4f(1.0f, 1.0f, 1.0f, 1.0f));
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, mapTextureId);
        hud.drawTexturedQuad2D(mapScreenX, mapScreenY, mapScreenW, mapScreenH, 0.0f, 0.0f, 1.0f, 1.0f);
        glBindTexture(GL_TEXTURE_2D, 0);
        hud.getUiTexShader().unbind();

        // 4. Overlays on Map: Player location marker & Cursor inspector marker
        long halfSpan = (long) (MAP_SIZE / 2) * blocksPerPixel;
        long startWx = centerWx - halfSpan;
        long startWz = centerWz - halfSpan;
        long totalSpan = (long) MAP_SIZE * blocksPerPixel;

        long playerWx = (long) Math.floor(player.position.x);
        long playerWz = (long) Math.floor(player.position.z);

        // Draw Player Marker (Red diamond/crosshair)
        if (playerWx >= startWx && playerWx <= startWx + totalSpan && playerWz >= startWz && playerWz <= startWz + totalSpan) {
            float pmx = mapScreenX + (float) (playerWx - startWx) / totalSpan * mapScreenW;
            float pmy = mapScreenY + (float) (playerWz - startWz) / totalSpan * mapScreenH;

            hud.getColorShader().bind();
            hud.getColorShader().setUniform("uColor", new Vector4f(1.0f, 0.15f, 0.15f, 1.0f));
            hud.drawRect2D(pmx - 6, pmy - 1, 13, 3);
            hud.drawRect2D(pmx - 1, pmy - 6, 3, 13);
            hud.getColorShader().setUniform("uColor", new Vector4f(1.0f, 1.0f, 1.0f, 1.0f));
            hud.drawRect2D(pmx - 1, pmy - 1, 3, 3);
            hud.getColorShader().unbind();
        }

        // Draw Active Waypoint Marker (Cyan diamond/cross)
        if (hasWaypoint && waypointWx >= startWx && waypointWx <= startWx + totalSpan
                && waypointWz >= startWz && waypointWz <= startWz + totalSpan) {
            float wpx = mapScreenX + (float) (waypointWx - startWx) / totalSpan * mapScreenW;
            float wpy = mapScreenY + (float) (waypointWz - startWz) / totalSpan * mapScreenH;

            hud.getColorShader().bind();
            hud.getColorShader().setUniform("uColor", new Vector4f(0.0f, 0.95f, 1.0f, 0.95f));
            hud.drawRect2D(wpx - 5, wpy - 5, 11, 11);
            hud.getColorShader().setUniform("uColor", new Vector4f(0.05f, 0.12f, 0.20f, 1.0f));
            hud.drawRect2D(wpx - 3, wpy - 3, 7, 7);
            hud.getColorShader().setUniform("uColor", new Vector4f(1.0f, 1.0f, 1.0f, 1.0f));
            hud.drawRect2D(wpx - 1, wpy - 1, 3, 3);
            hud.getColorShader().unbind();

            hud.getTextRenderer().drawTextWithShadow(ortho, "CEL", wpx + 8.0f, wpy - 5.0f, 1.1f,
                    new Vector4f(0.0f, 0.95f, 1.0f, 1.0f));
        }

        // Draw Selected Context Menu Target Pin (Amber/Orange crosshair)
        if (contextMenuOpen && contextWx >= startWx && contextWx <= startWx + totalSpan
                && contextWz >= startWz && contextWz <= startWz + totalSpan) {
            float pinX = mapScreenX + (float) (contextWx - startWx) / totalSpan * mapScreenW;
            float pinY = mapScreenY + (float) (contextWz - startWz) / totalSpan * mapScreenH;

            hud.getColorShader().bind();
            hud.getColorShader().setUniform("uColor", new Vector4f(1.0f, 0.60f, 0.10f, 1.0f));
            hud.drawRect2D(pinX - 8, pinY - 1, 17, 3);
            hud.drawRect2D(pinX - 1, pinY - 8, 3, 17);
            hud.getColorShader().setUniform("uColor", new Vector4f(1.0f, 1.0f, 1.0f, 1.0f));
            hud.drawRect2D(pinX - 2, pinY - 2, 5, 5);
            hud.getColorShader().unbind();
        }

        // Determine inspection coordinate (cursor if hovered over map, else player)
        float mx = (float) mouseX;
        float my = (float) mouseY;
        boolean mouseOverMap = (mx >= mapScreenX && mx <= mapScreenX + mapScreenW && my >= mapScreenY && my <= mapScreenY + mapScreenH);

        long inspectWx, inspectWz;
        if (mouseOverMap) {
            float fracX = (mx - mapScreenX) / mapScreenW;
            float fracZ = (my - mapScreenY) / mapScreenH;
            inspectWx = startWx + (long) (fracX * totalSpan);
            inspectWz = startWz + (long) (fracZ * totalSpan);
        } else {
            inspectWx = playerWx;
            inspectWz = playerWz;
        }

        // 5. Mode Switch Buttons below Map
        Mode[] modes = Mode.values();
        float btnY = mapScreenY + mapScreenH + 10.0f;
        float btnW = 76.0f;
        float btnH = 26.0f;

        for (int i = 0; i < modes.length; i++) {
            float bx = mapScreenX + i * (btnW + 6.0f);
            boolean isCur = (modes[i] == currentMode);

            hud.getColorShader().bind();
            if (isCur) {
                hud.getColorShader().setUniform("uColor", new Vector4f(0.20f, 0.60f, 0.95f, 0.95f));
            } else {
                hud.getColorShader().setUniform("uColor", new Vector4f(0.16f, 0.20f, 0.28f, 0.90f));
            }
            hud.drawRect2D(bx, btnY, btnW, btnH);
            hud.getColorShader().unbind();

            hud.getTextRenderer().drawTextWithShadow(ortho, "[" + (i + 1) + "] " + modes[i].name().substring(0, Math.min(5, modes[i].name().length())),
                    bx + 6.0f, btnY + 7.0f, 1.2f, isCur ? new Vector4f(1.0f, 1.0f, 1.0f, 1.0f) : new Vector4f(0.75f, 0.82f, 0.90f, 0.85f));
        }

        // Navigation Action Buttons below Mode Buttons
        float actY = btnY + btnH + 8.0f;
        float bZInX = mapScreenX;
        float bZOutX = bZInX + 70.0f;
        float bCentX = bZOutX + 70.0f;

        drawMiniButton(hud, ortho, bZInX, actY, 64.0f, 24.0f, "ZOOM +");
        drawMiniButton(hud, ortho, bZOutX, actY, 64.0f, 24.0f, "ZOOM -");
        drawMiniButton(hud, ortho, bCentX, actY, 110.0f, 24.0f, "[C] GRACZ");

        // Map scale / span indicator
        hud.getTextRenderer().drawTextWithShadow(ortho, "Skala: 1px = " + blocksPerPixel + " bl. | Obszar: " + totalSpan + "x" + totalSpan + " blokow",
                bCentX + 120.0f, actY + 6.0f, 1.2f, new Vector4f(0.70f, 0.82f, 0.95f, 0.90f));

        // 6. Right side: Detailed Inspector Panel
        float inspX = mapScreenX + mapScreenW + 18.0f;
        float inspY = mapScreenY;
        float inspW = winX + winW - inspX - 18.0f;
        float inspH = winH - 74.0f;

        // Container
        hud.getColorShader().bind();
        hud.getColorShader().setUniform("uColor", new Vector4f(0.12f, 0.15f, 0.22f, 0.95f));
        hud.drawRect2D(inspX, inspY, inspW, inspH);
        hud.getColorShader().setUniform("uColor", new Vector4f(0.30f, 0.40f, 0.55f, 0.85f));
        hud.drawRect2D(inspX, inspY, inspW, 30.0f);
        hud.getColorShader().unbind();

        hud.getTextRenderer().drawTextWithShadow(ortho, "INSPEKTOR PARAMETROW NOISE", inspX + 12.0f, inspY + 8.0f, 1.4f,
                new Vector4f(1.0f, 0.90f, 0.35f, 1.0f));

        // Evaluate exact parameters at inspection point
        SimplexNoise noise = world.getNoise();
        Biome biome = TerrainGenerator.computeBiome(noise, inspectWx, inspectWz);
        int surfaceY = TerrainGenerator.computeSurfaceHeight(noise, inspectWx, inspectWz);
        double temp = TerrainGenerator.sampleTemperature(noise, inspectWx, inspectWz);
        double humidity = TerrainGenerator.sampleHumidity(noise, inspectWx, inspectWz);
        double continental = TerrainGenerator.sampleContinental(noise, inspectWx, inspectWz);
        double mountain = TerrainGenerator.sampleMountain(noise, inspectWx, inspectWz);

        long distFromPlayer = (long) Math.hypot(inspectWx - playerWx, inspectWz - playerWz);
        long chunkX = Math.floorDiv(inspectWx, (long) Chunk.SIZE_X);
        long chunkZ = Math.floorDiv(inspectWz, (long) Chunk.SIZE_Z);

        float lineY = inspY + 42.0f;
        float lineStep = 20.0f;

        // Coordinates & Position
        drawSectionHeader(hud, ortho, inspX + 10.0f, lineY, inspW - 20.0f, "1. WSPOLRZEDNE");
        lineY += 26.0f;
        hud.getTextRenderer().drawTextWithShadow(ortho, String.format("Pozycja swiata: X = %d, Z = %d", inspectWx, inspectWz),
                inspX + 16.0f, lineY, 1.3f, new Vector4f(1.0f, 1.0f, 1.0f, 1.0f));
        lineY += lineStep;
        hud.getTextRenderer().drawTextWithShadow(ortho, String.format("Cubic Chunk 2D: (%d, %d) | Odleglosc od gracza: %d m", chunkX, chunkZ, distFromPlayer),
                inspX + 16.0f, lineY, 1.2f, new Vector4f(0.70f, 0.85f, 0.95f, 0.85f));
        lineY += lineStep + 6.0f;

        // Biome and Elevation
        drawSectionHeader(hud, ortho, inspX + 10.0f, lineY, inspW - 20.0f, "2. BIOM I ELEWACJA");
        lineY += 26.0f;

        // Biome Color Swatch
        int bColor = getBiomeColor(biome);
        float br = ((bColor >> 16) & 0xFF) / 255.0f;
        float bg = ((bColor >> 8) & 0xFF) / 255.0f;
        float bb = (bColor & 0xFF) / 255.0f;

        hud.getColorShader().bind();
        hud.getColorShader().setUniform("uColor", new Vector4f(br, bg, bb, 1.0f));
        hud.drawRect2D(inspX + 16.0f, lineY + 2.0f, 14.0f, 14.0f);
        hud.getColorShader().unbind();

        hud.getTextRenderer().drawTextWithShadow(ortho, "Biom: " + biome.displayName,
                inspX + 38.0f, lineY, 1.4f, new Vector4f(1.0f, 0.95f, 0.45f, 1.0f));
        lineY += lineStep;

        int seaDelta = surfaceY - Chunk.SEA_LEVEL;
        String seaDeltaStr = seaDelta >= 0 ? ("+" + seaDelta) : String.valueOf(seaDelta);
        hud.getTextRenderer().drawTextWithShadow(ortho, String.format("Wysokosc terenu: Y = %d (Poziom morza: %d, Delta: %s bl.)",
                        surfaceY, Chunk.SEA_LEVEL, seaDeltaStr),
                inspX + 16.0f, lineY, 1.3f, new Vector4f(0.85f, 0.92f, 1.0f, 0.90f));
        lineY += lineStep + 6.0f;

        // Climate & Noise layers
        drawSectionHeader(hud, ortho, inspX + 10.0f, lineY, inspW - 20.0f, "3. WARTOSCI SZUMOW (WARSTWY KLIMATYCZNE)");
        lineY += 26.0f;

        drawNoiseMetric(hud, ortho, inspX + 16.0f, lineY, "Temperatura (T):", temp, getTemperatureDesc(temp));
        lineY += lineStep;
        drawNoiseMetric(hud, ortho, inspX + 16.0f, lineY, "Wilgotnosc (W):", humidity, getHumidityDesc(humidity));
        lineY += lineStep;
        drawNoiseMetric(hud, ortho, inspX + 16.0f, lineY, "Kontynentalnosc (K):", continental, getContinentalDesc(continental));
        lineY += lineStep;
        drawNoiseMetric(hud, ortho, inspX + 16.0f, lineY, "Pasma gorskie (M):", mountain, getMountainDesc(mountain));
        lineY += lineStep + 10.0f;

        // Diagnostic Reason why this biome was chosen
        drawSectionHeader(hud, ortho, inspX + 10.0f, lineY, inspW - 20.0f, "4. ANALIZA LOGIKI WYBORU BIOMU");
        lineY += 26.0f;

        String diagReason = getBiomeDiagnosticReason(biome, continental, temp, humidity, mountain);
        hud.getTextRenderer().drawTextWithShadow(ortho, diagReason, inspX + 16.0f, lineY, 1.25f,
                new Vector4f(0.55f, 1.0f, 0.65f, 1.0f));
        lineY += lineStep + 4.0f;

        // Keyboard & Mouse help footer
        float helpY = inspY + inspH - 64.0f;
        hud.getColorShader().bind();
        hud.getColorShader().setUniform("uColor", new Vector4f(0.08f, 0.10f, 0.15f, 0.95f));
        hud.drawRect2D(inspX + 10.0f, helpY, inspW - 20.0f, 54.0f);
        hud.getColorShader().unbind();

        hud.getTextRenderer().drawTextWithShadow(ortho, "STEROWANIE: [M / F4 / ESC] Zamknij | [1-6] Zmiana warstwy szumu",
                inspX + 16.0f, helpY + 8.0f, 1.15f, new Vector4f(0.95f, 0.85f, 0.35f, 0.95f));
        hud.getTextRenderer().drawTextWithShadow(ortho, "PRZESUWANIE: Przeciagnij mysza lub strzalki/WASD | ZOOM: Scroll / +/-",
                inspX + 16.0f, helpY + 22.0f, 1.15f, new Vector4f(0.75f, 0.85f, 0.95f, 0.85f));
        hud.getTextRenderer().drawTextWithShadow(ortho, "INSPEKCJA: Najedz kursorem na mape | PPM: Menu opcji (Teleport, Cel, Biom)",
                inspX + 16.0f, helpY + 36.0f, 1.15f, new Vector4f(1.0f, 0.90f, 0.40f, 0.95f));

        // 7. Context Menu Popup (if open via Right Mouse Click on Map)
        if (contextMenuOpen) {
            float cmW = 270.0f;
            float itemH = 26.0f;
            float headerH = 28.0f;
            float padding = 6.0f;
            float cmH = headerH + CONTEXT_MENU_ITEMS.length * itemH + padding * 2.0f;

            float cmX = Math.max(10.0f, Math.min(contextMenuX, screenW - cmW - 10.0f));
            float cmY = Math.max(10.0f, Math.min(contextMenuY, screenH - cmH - 10.0f));

            // Outer border shadow / glow
            hud.getColorShader().bind();
            hud.getColorShader().setUniform("uColor", new Vector4f(0.30f, 0.60f, 0.95f, 0.98f));
            hud.drawRect2D(cmX - 2.0f, cmY - 2.0f, cmW + 4.0f, cmH + 4.0f);

            // Background
            hud.getColorShader().setUniform("uColor", new Vector4f(0.06f, 0.09f, 0.14f, 0.98f));
            hud.drawRect2D(cmX, cmY, cmW, cmH);

            // Header background
            hud.getColorShader().setUniform("uColor", new Vector4f(0.14f, 0.22f, 0.35f, 0.98f));
            hud.drawRect2D(cmX, cmY, cmW, headerH);
            hud.getColorShader().unbind();

            // Header text
            String title = String.format("OPCJE MAPY: [%d, %d]", contextWx, contextWz);
            hud.getTextRenderer().drawTextWithShadow(ortho, title, cmX + 8.0f, cmY + 7.0f, 1.25f,
                    new Vector4f(0.40f, 0.85f, 1.0f, 1.0f));

            // Menu Items
            for (int i = 0; i < CONTEXT_MENU_ITEMS.length; i++) {
                float iy = cmY + headerH + padding + i * itemH;
                boolean hovered = (mx >= cmX + 4.0f && mx <= cmX + cmW - 4.0f && my >= iy && my <= iy + itemH - 2.0f);

                if (hovered) {
                    hud.getColorShader().bind();
                    hud.getColorShader().setUniform("uColor", new Vector4f(0.22f, 0.44f, 0.72f, 0.88f));
                    hud.drawRect2D(cmX + 4.0f, iy, cmW - 8.0f, itemH - 2.0f);
                    hud.getColorShader().unbind();
                }

                Vector4f textColor;
                if (i == 0) {
                    textColor = hovered ? new Vector4f(0.50f, 1.0f, 0.60f, 1.0f) : new Vector4f(0.30f, 0.92f, 0.45f, 0.95f);
                } else if (i == 4) {
                    textColor = hovered ? new Vector4f(1.0f, 0.60f, 0.50f, 1.0f) : new Vector4f(0.90f, 0.45f, 0.45f, 0.85f);
                } else if (i == CONTEXT_MENU_ITEMS.length - 1) {
                    textColor = hovered ? new Vector4f(0.80f, 0.80f, 0.80f, 1.0f) : new Vector4f(0.65f, 0.65f, 0.65f, 0.80f);
                } else {
                    textColor = hovered ? new Vector4f(1.0f, 1.0f, 1.0f, 1.0f) : new Vector4f(0.85f, 0.90f, 0.96f, 0.90f);
                }

                hud.getTextRenderer().drawTextWithShadow(ortho, CONTEXT_MENU_ITEMS[i], cmX + 12.0f, iy + 6.0f, 1.15f, textColor);
            }
        }

        glEnable(GL_CULL_FACE);
        glEnable(GL_DEPTH_TEST);
    }

    private void drawSectionHeader(HudRenderer hud, Matrix4f ortho, float x, float y, float w, String title) {
        hud.getColorShader().bind();
        hud.getColorShader().setUniform("uColor", new Vector4f(0.18f, 0.24f, 0.35f, 0.90f));
        hud.drawRect2D(x, y, w, 22.0f);
        hud.getColorShader().unbind();

        hud.getTextRenderer().drawTextWithShadow(ortho, title, x + 8.0f, y + 5.0f, 1.25f,
                new Vector4f(0.40f, 0.80f, 1.0f, 1.0f));
    }

    private void drawNoiseMetric(HudRenderer hud, Matrix4f ortho, float x, float y, String name, double value, String desc) {
        String formatted = String.format("%-18s %+.3f  (%s)", name, value, desc);
        hud.getTextRenderer().drawTextWithShadow(ortho, formatted, x, y, 1.25f,
                new Vector4f(0.85f, 0.90f, 0.98f, 0.90f));
    }

    private void drawMiniButton(HudRenderer hud, Matrix4f ortho, float x, float y, float w, float h, String text) {
        hud.getColorShader().bind();
        hud.getColorShader().setUniform("uColor", new Vector4f(0.18f, 0.24f, 0.34f, 0.95f));
        hud.drawRect2D(x, y, w, h);
        hud.getColorShader().unbind();

        hud.getTextRenderer().drawTextWithShadow(ortho, text, x + 6.0f, y + 6.0f, 1.15f,
                new Vector4f(1.0f, 1.0f, 1.0f, 1.0f));
    }

    private static String getTemperatureDesc(double temp) {
        if (temp < -0.28) return "Polarna mrozna (Snieg / Zamarzniety)";
        if (temp < -0.16) return "Subpolarna chlodna (Tajga)";
        if (temp < 0.05) return "Umiarkowana chlodna";
        if (temp < 0.24) return "Umiarkowana ciepla (Lasy)";
        return "Tropikalna goraca (Pustynia / Mesa / Jungla)";
    }

    private static String getHumidityDesc(double hum) {
        if (hum < -0.14) return "Ekstremalnie sucha (Pustynia)";
        if (hum < -0.03) return "Aridowa sucha (Mesa / Badlands)";
        if (hum < 0.06) return "Niska wilgotnosc (Rowniny / Plains)";
        if (hum < 0.16) return "Umiarkowanie wilgotna (Lasy brzozowe)";
        return "Wysoka wilgotnosc (Gest las / Jungla)";
    }

    private static String getContinentalDesc(double cont) {
        if (cont < -0.22) return "Gleboki basen oceaniczny";
        if (cont < -0.05) return "Ocean szelfowy";
        if (cont < 0.01) return "Strefa wybrzeza / linia brzegowa";
        return "Srodladzie kontynentalne";
    }

    private static String getMountainDesc(double mount) {
        if (mount > 0.27) return "Lancuch gorski (Mountains peak)";
        if (mount > 0.10) return "Wzgorza i przedgorza";
        return "Niziny i plaskowyze";
    }

    private static String getBiomeDiagnosticReason(Biome biome, double cont, double temp, double hum, double mount) {
        if (cont < -0.22) {
            return String.format("K (%.3f) < -0.22 -> Gleboki Ocean", cont);
        }
        if (cont < -0.05) {
            return (temp < -0.16)
                    ? String.format("K (%.3f) < -0.05 && T (%.3f) < -0.16 -> Zamarzniety Ocean", cont, temp)
                    : String.format("K (%.3f) < -0.05 -> Zwykly Ocean", cont);
        }
        if (biome == Biome.BEACH) {
            return String.format("Strefa przybrzezna oceanu: K (%.3f) && Woda <= 3 bl. -> Plaza morska", cont);
        }
        if (mount > 0.27) {
            return String.format("Szum gorski M (%.3f) > 0.27 -> Lancuch gorski (Gory)", mount);
        }
        if (temp < -0.16) {
            return (hum < -0.02)
                    ? String.format("Klimat mrozny: T (%.3f) < -0.16 && W (%.3f) < -0.02 -> Sniezna tundra", temp, hum)
                    : String.format("Klimat mrozny: T (%.3f) < -0.16 && W (%.3f) >= -0.02 -> Tajga iglasta", temp, hum);
        }
        if (temp > 0.24) {
            if (hum < -0.14) return String.format("Goracy arid: T (%.3f) > 0.24 && W (%.3f) < -0.14 -> Pustynia", temp, hum);
            if (hum < -0.03) return String.format("Goracy suchy: T (%.3f) > 0.24 && W (%.3f) < -0.03 -> Mesa (Badlands)", temp, hum);
            if (hum < 0.12) return String.format("Goracy bufor: T (%.3f) > 0.24 && W (%.3f) < 0.12 -> Rownina (Plains)", temp, hum);
            return String.format("Goracy wilgotny: T (%.3f) > 0.24 && W (%.3f) >= 0.12 -> Jungla", temp, hum);
        }
        // Temperate
        if (hum < 0.06) {
            return String.format("Klimat umiarkowany: W (%.3f) < 0.06 -> Rownina (Plains)", hum);
        }
        if (hum < 0.16) {
            return String.format("Klimat umiarkowany: W (%.3f) < 0.16 -> Las brzozowy", hum);
        }
        return String.format("Klimat umiarkowany: W (%.3f) >= 0.16 -> Las debowy", hum);
    }

    public void onResize(int screenW, int screenH) {
        // Minimap and diagnostic panels adapt dynamically during render and click
    }

    public void cleanup() {
        if (mapTextureId != 0) {
            glDeleteTextures(mapTextureId);
            mapTextureId = 0;
        }
    }
}
