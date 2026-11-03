package com.voxel.ui;

import com.voxel.Main;
import com.voxel.block.BlockType;
import com.voxel.config.DebugHudConfig;
import com.voxel.entity.Player;
import com.voxel.i18n.I18n;
import com.voxel.render.ShaderProgram;
import com.voxel.render.TextureAtlas;
import com.voxel.world.Biome;
import com.voxel.world.Chunk;
import com.voxel.world.World;
import com.voxel.world.gen.TerrainGenerator;
import org.joml.Matrix4f;
import org.joml.Vector3d;
import org.joml.Vector4f;
import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;
import static org.lwjgl.opengl.GL30.*;

public class HudRenderer {
    private final ShaderProgram colorShader;
    private final ShaderProgram uiTexShader;
    private final TextRenderer textRenderer;

    private final int wireVao;
    private final int wireVbo;

    private final int quadVao;
    private final int quadVbo;

    private final int texQuadVao;
    private final int texQuadVbo;

    private final String glRenderer;
    private final String glVersion;
    private final String javaInfo;
    private final String osInfo;
    private final int cpuCores;

    public boolean showDebug = true;
    public boolean showHud = true;
    private boolean hasWaypoint = false;
    private long waypointWx = 0;
    private long waypointWz = 0;

    public com.voxel.world.BenchmarkResult lastBenchmark = null;
    public float benchmarkDisplayTimer = 0.0f;
    public boolean showBenchmarkOverlay = false;

    public void toggleHud() {
        showHud = !showHud;
    }

    public void setBenchmarkResult(com.voxel.world.BenchmarkResult result) {
        this.lastBenchmark = result;
        this.benchmarkDisplayTimer = 14.0f;
    }

    public void update(float dt) {
        if (benchmarkDisplayTimer > 0.0f) {
            benchmarkDisplayTimer -= dt;
            if (benchmarkDisplayTimer < 0.0f) benchmarkDisplayTimer = 0.0f;
        }
    }

    public void setWaypoint(boolean has, long wx, long wz) {
        this.hasWaypoint = has;
        this.waypointWx = wx;
        this.waypointWz = wz;
    }

    public HudRenderer() {
        colorShader = ShaderProgram.createColorShader();
        uiTexShader = ShaderProgram.createUiTextureShader();
        textRenderer = new TextRenderer();

        String r = "Unknown";
        String v = "Unknown";
        try {
            String gr = glGetString(GL_RENDERER);
            if (gr != null && !gr.isBlank()) r = gr.trim();
            String gv = glGetString(GL_VERSION);
            if (gv != null && !gv.isBlank()) v = gv.trim();
        } catch (Exception ignored) {}
        if (r.length() > 36) r = r.substring(0, 33) + "...";
        if (v.length() > 36) v = v.substring(0, 33) + "...";
        this.glRenderer = r;
        this.glVersion = v;

        String jv = System.getProperty("java.version", "21");
        String arch = System.getProperty("sun.arch.data.model", "64");
        this.javaInfo = jv + " (" + arch + "bit)";
        this.osInfo = System.getProperty("os.name", "Windows") + " (" + System.getProperty("os.arch", "amd64") + ")";
        this.cpuCores = Runtime.getRuntime().availableProcessors();

        // 12 edges = 24 vertices for wireframe cube [0..1]^3 slightly expanded
        float e = 0.003f;
        float x0 = -e, y0 = -e, z0 = -e;
        float x1 = 1.0f + e, y1 = 1.0f + e, z1 = 1.0f + e;
        float[] wireVerts = {
            // bottom face
            x0, y0, z0,  x1, y0, z0,
            x1, y0, z0,  x1, y0, z1,
            x1, y0, z1,  x0, y0, z1,
            x0, y0, z1,  x0, y0, z0,
            // top face
            x0, y1, z0,  x1, y1, z0,
            x1, y1, z0,  x1, y1, z1,
            x1, y1, z1,  x0, y1, z1,
            x0, y1, z1,  x0, y1, z0,
            // vertical pillars
            x0, y0, z0,  x0, y1, z0,
            x1, y0, z0,  x1, y1, z0,
            x1, y0, z1,  x1, y1, z1,
            x0, y0, z1,  x0, y1, z1
        };

        wireVao = glGenVertexArrays();
        wireVbo = glGenBuffers();
        FloatBuffer wb = BufferUtils.createFloatBuffer(wireVerts.length);
        wb.put(wireVerts).flip();
        glBindVertexArray(wireVao);
        glBindBuffer(GL_ARRAY_BUFFER, wireVbo);
        glBufferData(GL_ARRAY_BUFFER, wb, GL_STATIC_DRAW);
        glVertexAttribPointer(0, 3, GL_FLOAT, false, 3 * Float.BYTES, 0);
        glEnableVertexAttribArray(0);

        // Dynamic 2D quad for solid UI rects
        quadVao = glGenVertexArrays();
        quadVbo = glGenBuffers();
        glBindVertexArray(quadVao);
        glBindBuffer(GL_ARRAY_BUFFER, quadVbo);
        glBufferData(GL_ARRAY_BUFFER, 18 * Float.BYTES, GL_DYNAMIC_DRAW);
        glVertexAttribPointer(0, 3, GL_FLOAT, false, 3 * Float.BYTES, 0);
        glEnableVertexAttribArray(0);

        // Dynamic 2D textured quad for hotbar icons
        texQuadVao = glGenVertexArrays();
        texQuadVbo = glGenBuffers();
        glBindVertexArray(texQuadVao);
        glBindBuffer(GL_ARRAY_BUFFER, texQuadVbo);
        glBufferData(GL_ARRAY_BUFFER, 24 * Float.BYTES, GL_DYNAMIC_DRAW);
        glVertexAttribPointer(0, 2, GL_FLOAT, false, 4 * Float.BYTES, 0);
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(1, 2, GL_FLOAT, false, 4 * Float.BYTES, 2L * Float.BYTES);
        glEnableVertexAttribArray(1);

        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);
    }

    public void renderBlockSelection(Matrix4f projView, Vector3d eyePos, World.RaycastHit hit) {
        if (hit == null) return;

        colorShader.bind();
        float rx = (float) (hit.x() - eyePos.x);
        float ry = (float) (hit.y() - eyePos.y);
        float rz = (float) (hit.z() - eyePos.z);
        Matrix4f mvp = new Matrix4f(projView).translate(rx, ry, rz);
        colorShader.setUniform("uMVP", mvp);
        colorShader.setUniform("uColor", new Vector4f(0.05f, 0.05f, 0.05f, 0.85f));

        glLineWidth(2.0f);
        glBindVertexArray(wireVao);
        glDrawArrays(GL_LINES, 0, 24);
        glBindVertexArray(0);
        colorShader.unbind();
    }

    public void renderHud(int screenW, int screenH, int fps, Player player, World world,
                          World.RaycastHit hit, TextureAtlas atlas, ChatSystem chat) {
        if (!showHud) {
            if (chat != null && chat.isOpen()) {
                glDisable(GL_DEPTH_TEST);
                glDisable(GL_CULL_FACE);
                Matrix4f ortho = new Matrix4f().ortho(0, screenW, screenH, 0, -1, 1);
                renderChatOverlay(ortho, screenW, screenH, screenH - 72.0f, chat);
                glEnable(GL_CULL_FACE);
                glEnable(GL_DEPTH_TEST);
            }
            return;
        }

        glDisable(GL_DEPTH_TEST);
        glDisable(GL_CULL_FACE);

        Matrix4f ortho = new Matrix4f().ortho(0, screenW, screenH, 0, -1, 1);

        // 1. Crosshair in center
        float cx = screenW * 0.5f;
        float cy = screenH * 0.5f;
        colorShader.bind();
        colorShader.setUniform("uMVP", ortho);
        colorShader.setUniform("uColor", new Vector4f(1.0f, 1.0f, 1.0f, 0.90f));
        drawRect2D(cx - 9, cy - 1, 18, 2);
        drawRect2D(cx - 1, cy - 9, 2, 18);

        // 2. Hotbar at bottom
        int slots = player.hotbar.length;
        float slotSize = 48.0f;
        float padding = 6.0f;
        float barWidth = slots * slotSize + (slots + 1) * padding;
        float barHeight = slotSize + 2 * padding;
        float barX = (screenW - barWidth) * 0.5f;
        float barY = screenH - barHeight - 9.0f;

        colorShader.setUniform("uColor", new Vector4f(0.10f, 0.12f, 0.15f, 0.78f));
        drawRect2D(barX, barY, barWidth, barHeight);

        int selectedIdx = Math.floorMod(player.selectedSlot, slots);

        for (int i = 0; i < slots; i++) {
            float sx = barX + padding + i * (slotSize + padding);
            float sy = barY + padding;

            if (i == selectedIdx) {
                colorShader.setUniform("uColor", new Vector4f(0.98f, 0.82f, 0.20f, 0.95f));
                drawRect2D(sx - 3, sy - 3, slotSize + 6, slotSize + 6);
            }

            colorShader.setUniform("uColor", new Vector4f(0.22f, 0.25f, 0.30f, 0.92f));
            drawRect2D(sx, sy, slotSize, slotSize);
        }
        colorShader.unbind();

        // 3. Draw block icons inside each slot
        uiTexShader.bind();
        uiTexShader.setUniform("uProj", ortho);
        uiTexShader.setUniform("uTexture", 0);
        atlas.bind();

        glBindVertexArray(texQuadVao);
        glBindBuffer(GL_ARRAY_BUFFER, texQuadVbo);

        for (int i = 0; i < slots; i++) {
            float sx = barX + padding + i * (slotSize + padding) + 6.0f;
            float sy = barY + padding + 6.0f;
            float iconSize = slotSize - 12.0f;

            BlockType bt = player.hotbar[i];
            if (bt == BlockType.AIR) {
                continue;
            }
            int texIdx = bt.sideTex;
            float u0 = TextureAtlas.getU0(texIdx);
            float v0 = TextureAtlas.getV0(texIdx);
            float u1 = TextureAtlas.getU1(texIdx);
            float v1 = TextureAtlas.getV1(texIdx);

            float[] verts = {
                sx,            sy,            u0, v0,
                sx + iconSize, sy,            u1, v0,
                sx + iconSize, sy + iconSize, u1, v1,
                sx + iconSize, sy + iconSize, u1, v1,
                sx,            sy + iconSize, u0, v1,
                sx,            sy,            u0, v0
            };
            FloatBuffer fb = BufferUtils.createFloatBuffer(verts.length);
            fb.put(verts).flip();
            glBufferSubData(GL_ARRAY_BUFFER, 0, fb);
            glDrawArrays(GL_TRIANGLES, 0, 6);
        }

        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);
        uiTexShader.unbind();

        // Draw AIR label for empty slots
        for (int i = 0; i < slots; i++) {
            if (player.hotbar[i] == BlockType.AIR) {
                float sx = barX + padding + i * (slotSize + padding) + 12.0f;
                float sy = barY + padding + 18.0f;
                textRenderer.drawTextWithShadow(ortho, "AIR", sx, sy, 1.3f, new Vector4f(0.6f, 0.6f, 0.6f, 0.8f));
            }
        }

        // Slot numbers & active block label above hotbar
        for (int i = 0; i < slots; i++) {
            float sx = barX + padding + i * (slotSize + padding) + 4.0f;
            float sy = barY + padding + 4.0f;
            textRenderer.drawTextWithShadow(ortho, String.valueOf(i + 1), sx, sy, 1.5f,
                    i == selectedIdx ? new Vector4f(1.0f, 0.9f, 0.3f, 1.0f) : new Vector4f(0.85f, 0.85f, 0.85f, 0.85f));
        }

        String selLabel = I18n.t("creative.block_selected", player.getSelectedBlock().getDisplayName());
        float selW = textRenderer.getTextWidth(selLabel, 2.0f);
        textRenderer.drawTextWithShadow(ortho, selLabel, (screenW - selW) * 0.5f, barY - 22.0f, 2.0f,
                new Vector4f(1.0f, 0.95f, 0.65f, 0.95f));

        // 4. Render Debug Overlay (F3)
        if (showDebug) {
            renderDebugOverlay(ortho, screenW, screenH, fps, player, world, hit);
        }

        // 4b. Render Benchmark Results Card / Live Benchmark Overlay
        if (showBenchmarkOverlay || benchmarkDisplayTimer > 0.0f) {
            renderBenchmarkCard(ortho, screenW, screenH, world);
        }

        // 5. Render Chat Overlay & Input Box
        renderChatOverlay(ortho, screenW, screenH, barY, chat);

        glEnable(GL_CULL_FACE);
        glEnable(GL_DEPTH_TEST);
    }

    private void renderDebugOverlay(Matrix4f ortho, int screenW, int screenH, int fps,
                                    Player player, World world, World.RaycastHit hit) {
        long bx = (long) Math.floor(player.position.x);
        long by = (long) Math.floor(player.position.y);
        long bz = (long) Math.floor(player.position.z);
        long chunkX = Math.floorDiv(bx, (long) Chunk.SIZE_X);
        long chunkY = Math.floorDiv(by, (long) Chunk.SIZE_Y);
        long chunkZ = Math.floorDiv(bz, (long) Chunk.SIZE_Z);
        int lx = (int) Math.floorMod(bx, (long) Chunk.SIZE_X);
        int ly = (int) Math.floorMod(by, (long) Chunk.SIZE_Y);
        int lz = (int) Math.floorMod(bz, (long) Chunk.SIZE_Z);

        float normalizedYaw = (player.yaw % 360.0f + 360.0f) % 360.0f;
        String facing;
        if (normalizedYaw >= 45.0f && normalizedYaw < 135.0f) {
            facing = I18n.t("facing.south") + " (+Z)";
        } else if (normalizedYaw >= 135.0f && normalizedYaw < 225.0f) {
            facing = I18n.t("facing.west") + " (-X)";
        } else if (normalizedYaw >= 225.0f && normalizedYaw < 315.0f) {
            facing = I18n.t("facing.north") + " (-Z)";
        } else {
            facing = I18n.t("facing.east") + " (+X)";
        }

        String targetStr;
        if (hit != null) {
            double dist = Math.sqrt(
                    Math.pow(hit.x() + 0.5 - player.position.x, 2) +
                    Math.pow(hit.y() + 0.5 - player.position.y, 2) +
                    Math.pow(hit.z() + 0.5 - player.position.z, 2)
            );
            targetStr = String.format("%s (%d, %d, %d) [%.1fm]",
                    BlockType.fromId(hit.blockId()).getDisplayName(), hit.x(), hit.y(), hit.z(), dist);
        } else {
            targetStr = I18n.t("hud.none");
        }

        Biome currentBiome = world.getBiome(bx, bz);
        double temp = TerrainGenerator.sampleTemperature(world.getNoise(), bx, bz);
        double hum = TerrainGenerator.sampleHumidity(world.getNoise(), bx, bz);

        int surfaceY = world.getSurfaceHeight(bx, bz);
        int topOpaqueY = world.getTopOpaqueY(bx, bz);
        int skyLight = (by >= topOpaqueY) ? 15 : Math.max(0, 15 - (int) (topOpaqueY - by));

        float speed = player.velocity.length();
        int sunPct = (int) (world.getSunIntensity() * 100.0f);

        // Modular Debug HUD lines based on user configuration
        List<String> leftLines = new ArrayList<>();
        List<String> rightLines = new ArrayList<>();

        double genMs = world.getAvgGenTimeMs();
        double meshMs = world.getAvgMeshTimeMs();
        double uploadMs = world.getAvgUploadTimeMs();
        int activeWorkers = world.getWorkerThreadCount();
        int qGen = world.getGeneratingChunksCount();
        int qMesh = world.getMeshingChunksCount();
        int qGpu = world.getReadyMeshesCount();
        double potentialRate = (genMs > 0 ? (1000.0 / genMs) * activeWorkers : 0);

        Runtime rt = Runtime.getRuntime();
        long maxMem = rt.maxMemory() / (1024L * 1024L);
        long totalMem = rt.totalMemory() / (1024L * 1024L);
        long freeMem = rt.freeMemory() / (1024L * 1024L);
        long usedMem = totalMem - freeMem;
        long pctUsed = totalMem > 0 ? (usedMem * 100L) / totalMem : 0;
        long pctMax = maxMem > 0 ? (usedMem * 100L) / maxMem : 0;

        for (DebugHudConfig.HudItem item : DebugHudConfig.getItems()) {
            if (!item.enabled) continue;
            List<String> targetList = (item.side == DebugHudConfig.Side.LEFT) ? leftLines : rightLines;

            switch (item.id) {
                case FPS -> targetList.add(String.format("Java Voxel 3D (Cubic 16x16x16) | FPS: %d", fps));
                case POSITION -> targetList.add(String.format("XYZ: %.2f / %.2f / %.2f  [%s: %d, %d, %d]",
                        player.position.x, player.position.y, player.position.z, I18n.t("hud.block"), bx, by, bz));
                case CHUNK -> targetList.add(String.format("Chunk 3D: (%d, %d, %d)  [%s: %d, %d, %d]",
                        chunkX, chunkY, chunkZ, I18n.t("hud.local"), lx, ly, lz));
                case FACING -> targetList.add(String.format("Facing: %s [Yaw: %.1f, Pitch: %.1f]",
                        facing, player.yaw, player.pitch));
                case CHUNKS_3D -> targetList.add(String.format("Chunks 3D: %d rendered / %d loaded",
                        world.getLastRenderedChunkCount(), world.getLoadedChunkCount()));
                case BIOME -> {
                    String bName = I18n.t("biome." + currentBiome.name().toLowerCase());
                    if (bName.startsWith("biome.")) bName = currentBiome.displayName;
                    targetList.add(String.format("Biome: %s (Temp: %.2f, Hum: %.2f)", bName, temp, hum));
                }
                case LIGHT -> targetList.add(String.format("Sky Light: %d | Top Y: %d | Surface Y: %d",
                        skyLight, topOpaqueY, surfaceY));
                case MOVEMENT -> {
                    String moveMode = player.noclip ? "NOCLIP" : (player.flying ? I18n.t("movement.flying") : I18n.t("movement.walking"));
                    String onGround = player.onGround ? I18n.t("options.on") : I18n.t("options.off");
                    targetList.add(String.format("Move: %s | Ground: %s | Speed: %.1f m/s", moveMode, onGround, speed));
                }
                case TIME -> targetList.add(String.format("Time: %s %s | Sun: %d%%",
                        Main.formatTimeOfDay(world.timeOfDay), world.dayCyclePaused ? "(" + I18n.t("time.paused") + ")" : "", sunPct));
                case TARGET -> targetList.add(String.format("Target: %s", targetStr));
                case SEED -> targetList.add(String.format("Seed: %d", world.getSeed()));
                case MEMORY -> {
                    targetList.add(String.format("RAM: %d MB / %d MB (%d%%)", usedMem, totalMem, pctUsed));
                    targetList.add(String.format("Allocated: %d MB | Max Heap: %d MB (%d%%)", totalMem, maxMem, pctMax));
                }
                case HARDWARE -> {
                    targetList.add(String.format("CPU: %d logical cores", cpuCores));
                    targetList.add(String.format("Java: %s", javaInfo));
                    targetList.add(String.format("OS: %s", osInfo));
                }
                case GPU -> {
                    targetList.add(String.format("Display: %d x %d (Scale: 2x)", screenW, screenH));
                    targetList.add(String.format("GPU: %s", glRenderer));
                    targetList.add(String.format("OpenGL: %s", glVersion));
                }
                case BENCHMARK -> {
                    targetList.add(String.format("Live Bench: Gen %.2fms | Mesh %.2fms | GPU %.2fms | %d CPU",
                            genMs, meshMs, uploadMs, activeWorkers));
                    targetList.add(String.format("Throughput: %.0f ch/s | Queues: Gen %d | Mesh %d | GPU %d",
                            potentialRate, qGen, qMesh, qGpu));
                    if (lastBenchmark != null && benchmarkDisplayTimer > 0.0f) {
                        targetList.add(String.format("Last Run [%d ch]: %.0f ch/s (Gen: %.1fms, Mesh: %.1fms) -> %s",
                                lastBenchmark.chunkCount(), lastBenchmark.totalRate(),
                                lastBenchmark.terrainGenMs(), lastBenchmark.meshingMs(),
                                lastBenchmark.performanceRating()));
                    }
                }
            }
        }

        if (hasWaypoint && leftLines.size() > 0) {
            double dist = Math.hypot(player.position.x - waypointWx, player.position.z - waypointWz);
            leftLines.add(String.format("Waypoint: [%d, %d] | Dist: %.1f m", waypointWx, waypointWz, dist));
        }

        float scale = 2.0f;
        float lineH = textRenderer.getLineHeight(scale);
        float pad = 8.0f;

        float leftMaxW = 0.0f;
        for (String line : leftLines) {
            leftMaxW = Math.max(leftMaxW, textRenderer.getTextWidth(line, scale));
        }
        float leftBoxW = leftMaxW + pad * 2.0f;
        float leftBoxH = leftLines.size() * lineH + pad * 2.0f - 2.0f;

        float rightMaxW = 0.0f;
        for (String line : rightLines) {
            rightMaxW = Math.max(rightMaxW, textRenderer.getTextWidth(line, scale));
        }
        float rightBoxW = rightMaxW + pad * 2.0f;
        float rightBoxH = rightLines.size() * lineH + pad * 2.0f - 2.0f;

        float leftStartX = 10.0f;
        float leftStartY = 10.0f;

        boolean sideBySide = (leftStartX + leftBoxW + 16.0f + rightBoxW + 10.0f <= screenW);
        float rightStartX = sideBySide ? (screenW - rightBoxW - 10.0f) : leftStartX;
        float rightStartY = sideBySide ? 10.0f : (leftStartY + leftBoxH + 6.0f);

        // Draw background boxes
        colorShader.bind();
        colorShader.setUniform("uMVP", ortho);
        colorShader.setUniform("uColor", new Vector4f(0.05f, 0.07f, 0.11f, 0.72f));
        if (!leftLines.isEmpty()) {
            drawRect2D(leftStartX, leftStartY, leftBoxW, leftBoxH);
        }
        if (!rightLines.isEmpty()) {
            drawRect2D(rightStartX, rightStartY, rightBoxW, rightBoxH);
        }
        colorShader.unbind();

        // Draw left lines
        for (int i = 0; i < leftLines.size(); i++) {
            Vector4f col = (i == 0) ? new Vector4f(0.40f, 0.95f, 0.60f, 1.0f) : new Vector4f(0.94f, 0.95f, 0.98f, 1.0f);
            textRenderer.drawTextWithShadow(ortho, leftLines.get(i), leftStartX + pad, leftStartY + pad + i * lineH, scale, col);
        }

        // Draw right lines
        Vector4f ramCol;
        if (pctMax > 80 || pctUsed > 88) {
            ramCol = new Vector4f(1.0f, 0.40f, 0.35f, 1.0f);
        } else if (pctMax > 60 || pctUsed > 70) {
            ramCol = new Vector4f(1.0f, 0.85f, 0.35f, 1.0f);
        } else {
            ramCol = new Vector4f(0.45f, 0.95f, 0.60f, 1.0f);
        }

        for (int i = 0; i < rightLines.size(); i++) {
            String l = rightLines.get(i);
            Vector4f col = l.startsWith("RAM:") ? ramCol
                    : (l.startsWith("Live Bench:") || l.startsWith("Last Run") ? new Vector4f(0.40f, 0.95f, 0.65f, 1.0f)
                    : (l.startsWith("GPU:") || l.startsWith("OpenGL:") ? new Vector4f(0.88f, 0.85f, 1.0f, 0.95f)
                    : new Vector4f(0.92f, 0.94f, 0.96f, 1.0f)));
            textRenderer.drawTextWithShadow(ortho, rightLines.get(i), rightStartX + pad, rightStartY + pad + i * lineH, scale, col);
        }
    }

    private void renderChatOverlay(Matrix4f ortho, int screenW, int screenH, float hotbarY, ChatSystem chat) {
        float scale = 2.0f;
        float lineH = textRenderer.getLineHeight(scale);
        float chatX = 12.0f;
        float inputY = hotbarY - 58.0f;

        List<ChatSystem.ChatMessage> all = chat.getMessages();
        List<ChatSystem.ChatMessage> visible = new ArrayList<>();
        boolean isOpen = chat.isOpen();
        int maxShown = isOpen ? 10 : 6;

        for (int i = all.size() - 1; i >= 0 && visible.size() < maxShown; i--) {
            ChatSystem.ChatMessage msg = all.get(i);
            if (isOpen || msg.age() < 9.0f) {
                visible.addFirst(msg);
            }
        }

        if (!visible.isEmpty()) {
            float msgStartY = inputY - visible.size() * lineH - 12.0f;
            float maxMsgW = 540.0f;
            for (ChatSystem.ChatMessage m : visible) {
                maxMsgW = Math.max(maxMsgW, textRenderer.getTextWidth(m.text(), scale) + 16.0f);
            }

            colorShader.bind();
            colorShader.setUniform("uMVP", ortho);
            colorShader.setUniform("uColor", new Vector4f(0.05f, 0.06f, 0.09f, isOpen ? 0.72f : 0.45f));
            drawRect2D(chatX, msgStartY - 6.0f, maxMsgW, visible.size() * lineH + 10.0f);
            colorShader.unbind();

            for (int i = 0; i < visible.size(); i++) {
                ChatSystem.ChatMessage m = visible.get(i);
                float alpha = 1.0f;
                if (!isOpen && m.age() > 7.0f) {
                    alpha = Math.max(0.0f, 1.0f - (m.age() - 7.0f) / 2.0f);
                }
                Vector4f c = new Vector4f(m.color().x, m.color().y, m.color().z, alpha);
                textRenderer.drawTextWithShadow(ortho, m.text(), chatX + 8.0f, msgStartY + i * lineH, scale, c);
            }
        }

        if (isOpen) {
            String rawInput = chat.getInputText();
            int curPos = chat.getCursorPos();
            String prefix = "> ";
            String fullPrompt = prefix + rawInput;
            String beforeCursor = prefix + rawInput.substring(0, curPos);

            float inputW = Math.max(540.0f, textRenderer.getTextWidth(fullPrompt, scale) + 28.0f);

            // Draw Autocompletion Suggestions Popup if available
            if (chat.hasSuggestions()) {
                List<ChatSystem.Suggestion> sugList = chat.getSuggestions();
                int selectedIdx = chat.getSelectedSuggestionIndex();
                int totalSugs = sugList.size();
                int maxVisible = Math.min(10, totalSugs);

                // Minecraft-style scrolling window: keeps selectedIdx visible at all times
                int scrollOffset = 0;
                if (selectedIdx >= maxVisible) {
                    scrollOffset = selectedIdx - maxVisible + 1;
                }
                if (scrollOffset + maxVisible > totalSugs) {
                    scrollOffset = Math.max(0, totalSugs - maxVisible);
                }

                float sugW = Math.max(inputW, 540.0f);
                float sugRowH = lineH + 2.0f;
                float sugH = maxVisible * sugRowH + 18.0f;
                float sugY = inputY - sugH - 6.0f;

                colorShader.bind();
                colorShader.setUniform("uMVP", ortho);
                colorShader.setUniform("uColor", new Vector4f(0.20f, 0.50f, 0.85f, 0.95f));
                drawRect2D(chatX - 2.0f, sugY - 2.0f, sugW + 4.0f, sugH + 4.0f);
                colorShader.setUniform("uColor", new Vector4f(0.06f, 0.08f, 0.12f, 0.96f));
                drawRect2D(chatX, sugY, sugW, sugH);

                for (int i = 0; i < maxVisible; i++) {
                    int itemIdx = scrollOffset + i;
                    if (itemIdx == selectedIdx) {
                        colorShader.setUniform("uColor", new Vector4f(0.18f, 0.35f, 0.65f, 0.90f));
                        drawRect2D(chatX + 4.0f, sugY + 4.0f + i * sugRowH, sugW - 8.0f, sugRowH);
                    }
                }
                colorShader.unbind();

                for (int i = 0; i < maxVisible; i++) {
                    int itemIdx = scrollOffset + i;
                    ChatSystem.Suggestion s = sugList.get(itemIdx);
                    float rowY = sugY + 6.0f + i * sugRowH;
                    Vector4f labelCol = (itemIdx == selectedIdx)
                            ? new Vector4f(1.0f, 0.92f, 0.40f, 1.0f)
                            : new Vector4f(0.90f, 0.92f, 0.96f, 1.0f);
                    textRenderer.drawTextWithShadow(ortho, s.label(), chatX + 10.0f, rowY, scale, labelCol);

                    if (s.hint() != null && !s.hint().isEmpty()) {
                        float labelW = textRenderer.getTextWidth(s.label(), scale);
                        textRenderer.drawTextWithShadow(ortho, "- " + s.hint(), chatX + 16.0f + labelW, rowY, scale,
                                new Vector4f(0.55f, 0.75f, 0.90f, 0.85f));
                    }
                }

                String footerText = String.format("(%d/%d)", selectedIdx + 1, totalSugs);
                textRenderer.drawTextWithShadow(ortho, footerText,
                        chatX + 10.0f, sugY + sugH - 12.0f, 1.1f, new Vector4f(0.65f, 0.75f, 0.85f, 0.75f));
            }

            colorShader.bind();
            colorShader.setUniform("uMVP", ortho);
            colorShader.setUniform("uColor", new Vector4f(0.95f, 0.80f, 0.25f, 0.90f));
            drawRect2D(chatX - 2.0f, inputY - 2.0f, inputW + 4.0f, lineH + 12.0f);
            colorShader.setUniform("uColor", new Vector4f(0.08f, 0.10f, 0.14f, 0.92f));
            drawRect2D(chatX, inputY, inputW, lineH + 8.0f);

            // Blinking vertical cursor bar at exact cursorPos
            if ((System.currentTimeMillis() / 400) % 2 == 0) {
                float cursorX = chatX + 8.0f + textRenderer.getTextWidth(beforeCursor, scale);
                colorShader.setUniform("uColor", new Vector4f(1.0f, 0.90f, 0.30f, 0.98f));
                drawRect2D(cursorX, inputY + 3.0f, 2.5f, lineH);
            }
            colorShader.unbind();

            // Render input prompt with syntax highlighting
            textRenderer.drawTextWithShadow(ortho, prefix, chatX + 8.0f, inputY + 4.0f, scale,
                    new Vector4f(1.0f, 0.85f, 0.20f, 1.0f));
            float curX = chatX + 8.0f + textRenderer.getTextWidth(prefix, scale);

            List<ChatSyntaxHighlighter.Span> spans = ChatSyntaxHighlighter.highlight(rawInput);
            for (ChatSyntaxHighlighter.Span span : spans) {
                textRenderer.drawTextWithShadow(ortho, span.text(), curX, inputY + 4.0f, scale, span.color());
                curX += textRenderer.getTextWidth(span.text(), scale);
            }
        }
    }

    public void drawRect2D(float x, float y, float w, float h) {
        float[] verts = {
            x,     y,     0.0f,
            x + w, y,     0.0f,
            x + w, y + h, 0.0f,
            x + w, y + h, 0.0f,
            x,     y + h, 0.0f,
            x,     y,     0.0f
        };
        FloatBuffer fb = BufferUtils.createFloatBuffer(verts.length);
        fb.put(verts).flip();
        glBindVertexArray(quadVao);
        glBindBuffer(GL_ARRAY_BUFFER, quadVbo);
        glBufferSubData(GL_ARRAY_BUFFER, 0, fb);
        glDrawArrays(GL_TRIANGLES, 0, 6);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);
    }

    public void drawTexturedQuad2D(float x, float y, float w, float h, float u0, float v0, float u1, float v1) {
        float[] verts = {
            x,     y,     u0, v0,
            x + w, y,     u1, v0,
            x + w, y + h, u1, v1,
            x + w, y + h, u1, v1,
            x,     y + h, u0, v1,
            x,     y,     u0, v0
        };
        FloatBuffer fb = BufferUtils.createFloatBuffer(verts.length);
        fb.put(verts).flip();
        glBindVertexArray(texQuadVao);
        glBindBuffer(GL_ARRAY_BUFFER, texQuadVbo);
        glBufferSubData(GL_ARRAY_BUFFER, 0, fb);
        glDrawArrays(GL_TRIANGLES, 0, 6);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);
    }

    private void renderBenchmarkCard(Matrix4f ortho, int screenW, int screenH, World world) {
        if (lastBenchmark == null && !showBenchmarkOverlay) return;

        float cardW = 560.0f;
        float cardH = 115.0f;
        float startX = (screenW - cardW) * 0.5f;
        float startY = 16.0f;

        colorShader.bind();
        colorShader.setUniform("uMVP", ortho);
        colorShader.setUniform("uColor", new Vector4f(0.04f, 0.06f, 0.09f, 0.92f));
        drawRect2D(startX, startY, cardW, cardH);

        Vector4f accentCol = (lastBenchmark != null && lastBenchmark.performanceRating().startsWith("A"))
                ? new Vector4f(0.40f, 0.98f, 0.55f, 1.0f)
                : new Vector4f(0.35f, 0.85f, 1.0f, 1.0f);
        colorShader.setUniform("uColor", accentCol);
        drawRect2D(startX, startY, cardW, 4.0f);
        colorShader.unbind();

        float scale = 2.0f;
        float lineH = textRenderer.getLineHeight(scale) + 1.0f;
        float pad = 10.0f;

        if (lastBenchmark != null) {
            String title = I18n.t("benchmark.card_title",
                    lastBenchmark.chunkCount(), lastBenchmark.workerThreads());
            textRenderer.drawTextWithShadow(ortho, title, startX + pad, startY + pad, scale,
                    new Vector4f(1.0f, 0.90f, 0.35f, 1.0f));

            String genStr = I18n.t("benchmark.terrain_gen",
                    lastBenchmark.terrainGenMs(), lastBenchmark.terrainGenRate(), lastBenchmark.avgTerrainGenMs());
            textRenderer.drawTextWithShadow(ortho, genStr, startX + pad, startY + pad + lineH, scale,
                    new Vector4f(0.85f, 0.95f, 1.0f, 1.0f));

            String meshStr = I18n.t("benchmark.meshing",
                    lastBenchmark.meshingMs(), lastBenchmark.meshingRate(), lastBenchmark.avgMeshingMs());
            textRenderer.drawTextWithShadow(ortho, meshStr, startX + pad, startY + pad + lineH * 2, scale,
                    new Vector4f(0.85f, 0.95f, 1.0f, 1.0f));

            String totalStr = I18n.t("benchmark.total_rate",
                    lastBenchmark.totalRate(), lastBenchmark.totalTimeMs());
            textRenderer.drawTextWithShadow(ortho, totalStr, startX + pad, startY + pad + lineH * 3, scale,
                    new Vector4f(0.40f, 0.98f, 0.60f, 1.0f));

            String ratingStr = I18n.t("benchmark.rating", lastBenchmark.performanceRating());
            float rW = textRenderer.getTextWidth(ratingStr, scale);
            textRenderer.drawTextWithShadow(ortho, ratingStr, startX + cardW - rW - pad, startY + pad, scale,
                    accentCol);
        } else {
            double genMs = world.getAvgGenTimeMs();
            double meshMs = world.getAvgMeshTimeMs();
            double uploadMs = world.getAvgUploadTimeMs();
            int workers = world.getWorkerThreadCount();
            double throughput = (genMs > 0 ? (1000.0 / genMs) * workers : 0);

            textRenderer.drawTextWithShadow(ortho, I18n.t("benchmark.live_title"), startX + pad, startY + pad, scale,
                    new Vector4f(1.0f, 0.90f, 0.35f, 1.0f));
            textRenderer.drawTextWithShadow(ortho, I18n.t("benchmark.live_gen", genMs, workers),
                    startX + pad, startY + pad + lineH, scale, new Vector4f(0.85f, 0.95f, 1.0f, 1.0f));
            textRenderer.drawTextWithShadow(ortho, I18n.t("benchmark.live_mesh", meshMs, uploadMs),
                    startX + pad, startY + pad + lineH * 2, scale, new Vector4f(0.85f, 0.95f, 1.0f, 1.0f));
            textRenderer.drawTextWithShadow(ortho, I18n.t("benchmark.live_throughput", throughput),
                    startX + pad, startY + pad + lineH * 3, scale, new Vector4f(0.40f, 0.98f, 0.60f, 1.0f));
        }
    }

    public ShaderProgram getColorShader() {
        return colorShader;
    }

    public ShaderProgram getUiTexShader() {
        return uiTexShader;
    }

    public TextRenderer getTextRenderer() {
        return textRenderer;
    }

    public void cleanup() {
        textRenderer.cleanup();
        colorShader.cleanup();
        uiTexShader.cleanup();
        glDeleteBuffers(wireVbo);
        glDeleteVertexArrays(wireVao);
        glDeleteBuffers(quadVbo);
        glDeleteVertexArrays(quadVao);
        glDeleteBuffers(texQuadVbo);
        glDeleteVertexArrays(texQuadVao);
    }
}
