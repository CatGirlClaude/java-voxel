package com.voxel.render;

import com.voxel.util.Logger;
import org.lwjgl.BufferUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Random;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE;

public class TextureAtlas {
    public static final int TILE_SIZE = 16;
    public static final int GRID_SIZE = 8; // 8x8 tiles = 64 tiles total
    public static final int ATLAS_SIZE = TILE_SIZE * GRID_SIZE;

    public static final String[] TILE_NAMES = {
        "grass_block_top",      // 0
        "grass_block_side",     // 1
        "dirt",                 // 2
        "stone",                // 3
        "cobblestone",          // 4
        "oak_log",              // 5
        "oak_log_top",          // 6
        "oak_leaves",           // 7
        "sand",                 // 8
        "water",                // 9
        "oak_planks",           // 10
        "brick",                // 11
        "glass",                // 12
        "snow",                 // 13
        "glowstone",            // 14
        "sandstone",            // 15
        "cactus_side",          // 16
        "cactus_top",           // 17
        "birch_log",            // 18
        "birch_log_top",        // 19
        "spruce_log",           // 20
        "spruce_leaves",        // 21
        "ice",                  // 22
        "red_sand",             // 23
        "terracotta",           // 24
        "snowy_grass_side",     // 25
        "taiga_grass_top",      // 26
        "taiga_grass_side",     // 27
        "spruce_log_top",       // 28
        "birch_leaves",         // 29
        "jungle_log",           // 30
        "jungle_log_top",       // 31
        "jungle_leaves",        // 32
        "jungle_grass_top",     // 33
        "jungle_grass_side",    // 34
        "tall_grass",           // 35
        "vine",                 // 36
        "orchid",               // 37
        "rose",                 // 38
        "dandelion",            // 39
        "blue_orchid",          // 40
        "allium",               // 41
        "sugar_cane",           // 42
        "seagrass",             // 43
        "kelp",                 // 44
        "coral",                // 45
        "gravel",               // 46
        "clay",                 // 47
        "missing"               // 48 (Fallback texture)
    };

    public static final int MISSING_TEXTURE_TILE = 48;

    public static final Path RUN_TEXTURES_DIR = com.voxel.util.GamePaths.resolve("assets/textures/block");
    public static final Path SRC_TEXTURES_DIR = Paths.get("src/main/resources/assets/textures/block");

    private final int textureId;

    public TextureAtlas() {
        ByteBuffer buffer = BufferUtils.createByteBuffer(ATLAS_SIZE * ATLAS_SIZE * 4);
        int[] pixels = new int[ATLAS_SIZE * ATLAS_SIZE];

        ensurePngAssetsExist();

        int pngLoadedCount = 0;
        for (int tile = 0; tile < TILE_NAMES.length; tile++) {
            if (loadTileFromPng(pixels, tile)) {
                pngLoadedCount++;
            } else {
                generateTile(pixels, tile);
            }
        }

        for (int i = 0; i < pixels.length; i++) {
            int argb = pixels[i];
            buffer.put((byte) ((argb >> 16) & 0xFF)); // R
            buffer.put((byte) ((argb >> 8) & 0xFF));  // G
            buffer.put((byte) (argb & 0xFF));         // B
            buffer.put((byte) ((argb >> 24) & 0xFF)); // A
        }
        buffer.flip();

        textureId = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, textureId);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, ATLAS_SIZE, ATLAS_SIZE, 0, GL_RGBA, GL_UNSIGNED_BYTE, buffer);
        glBindTexture(GL_TEXTURE_2D, 0);
        Logger.info("TextureAtlas", "Loaded texture atlas (%dx%d, %d tiles, %d loaded from PNG files, OpenGL ID: %d)",
                ATLAS_SIZE, ATLAS_SIZE, TILE_NAMES.length, pngLoadedCount, textureId);
    }

    public static void ensurePngAssetsExist() {
        try {
            if (!Files.exists(RUN_TEXTURES_DIR)) {
                Files.createDirectories(RUN_TEXTURES_DIR);
            }
            boolean srcDirExists = Files.exists(SRC_TEXTURES_DIR.getParent());
            if (srcDirExists && !Files.exists(SRC_TEXTURES_DIR)) {
                Files.createDirectories(SRC_TEXTURES_DIR);
            }

            int[] tempPixels = new int[ATLAS_SIZE * ATLAS_SIZE];
            for (int t = 0; t < TILE_NAMES.length; t++) {
                String name = (t < TILE_NAMES.length) ? TILE_NAMES[t] : ("tile_" + t);
                Path runFile = RUN_TEXTURES_DIR.resolve(name + ".png");
                Path srcFile = srcDirExists ? SRC_TEXTURES_DIR.resolve(name + ".png") : null;

                if (!Files.exists(runFile) || (srcFile != null && !Files.exists(srcFile))) {
                    generateTile(tempPixels, t);
                    int tileX = (t % GRID_SIZE) * TILE_SIZE;
                    int tileY = (t / GRID_SIZE) * TILE_SIZE;
                    BufferedImage img = new BufferedImage(TILE_SIZE, TILE_SIZE, BufferedImage.TYPE_INT_ARGB);
                    for (int y = 0; y < TILE_SIZE; y++) {
                        for (int x = 0; x < TILE_SIZE; x++) {
                            img.setRGB(x, y, tempPixels[(tileY + y) * ATLAS_SIZE + tileX + x]);
                        }
                    }
                    if (!Files.exists(runFile)) {
                        ImageIO.write(img, "png", runFile.toFile());
                    }
                    if (srcFile != null && !Files.exists(srcFile)) {
                        ImageIO.write(img, "png", srcFile.toFile());
                    }
                }
            }
        } catch (Exception e) {
            Logger.warn("TextureAtlas", "Failed to export default PNG textures: %s", e.getMessage());
        }
    }

    private boolean loadTileFromPng(int[] pixels, int tileIndex) {
        String name = (tileIndex < TILE_NAMES.length) ? TILE_NAMES[tileIndex] : ("tile_" + tileIndex);
        Path runFile = RUN_TEXTURES_DIR.resolve(name + ".png");
        BufferedImage img = null;
        try {
            if (Files.exists(runFile)) {
                img = ImageIO.read(runFile.toFile());
            } else {
                try (InputStream is = TextureAtlas.class.getResourceAsStream("/assets/textures/block/" + name + ".png")) {
                    if (is != null) {
                        img = ImageIO.read(is);
                    }
                }
            }
            if (img != null) {
                int tileX = (tileIndex % GRID_SIZE) * TILE_SIZE;
                int tileY = (tileIndex / GRID_SIZE) * TILE_SIZE;
                for (int y = 0; y < TILE_SIZE; y++) {
                    for (int x = 0; x < TILE_SIZE; x++) {
                        int argb = (x < img.getWidth() && y < img.getHeight()) ? img.getRGB(x, y) : 0;
                        pixels[(tileY + y) * ATLAS_SIZE + tileX + x] = argb;
                    }
                }
                return true;
            }
        } catch (Exception e) {
            Logger.warn("TextureAtlas", "Failed to load PNG tile '%s': %s", name, e.getMessage());
        }
        return false;
    }

    public static void generateTile(int[] pixels, int tileIndex) {
        int tileX = (tileIndex % GRID_SIZE) * TILE_SIZE;
        int tileY = (tileIndex / GRID_SIZE) * TILE_SIZE;
        Random rng = new Random(1337L + tileIndex * 997L);

        for (int y = 0; y < TILE_SIZE; y++) {
            for (int x = 0; x < TILE_SIZE; x++) {
                int r = 255, g = 0, b = 255, a = 255;
                int n = rng.nextInt(21) - 10;

                switch (tileIndex) {
                    case 0 -> { // Grass top (neutral luminance for dynamic biome tinting)
                        int lum = clamp(192 + n);
                        r = lum;
                        g = lum;
                        b = lum;
                    }
                    case 1 -> { // Grass side
                        int grassDepth = 3 + ((x * 7 + y * 3) % 3);
                        if (y < grassDepth) {
                            r = clamp(88 + n);
                            g = clamp(165 + n);
                            b = clamp(56 + n);
                        } else {
                            r = clamp(134 + n);
                            g = clamp(96 + n);
                            b = clamp(67 + n);
                        }
                    }
                    case 2 -> { // Dirt
                        r = clamp(134 + n);
                        g = clamp(96 + n);
                        b = clamp(67 + n);
                        if (rng.nextInt(12) == 0) {
                            r = clamp(r - 18);
                            g = clamp(g - 18);
                            b = clamp(b - 18);
                        }
                    }
                    case 3 -> { // Stone
                        int sn = rng.nextInt(18) - 9 + ((y % 4 == 0 && x % 5 != 0) ? -8 : 0);
                        r = clamp(128 + sn);
                        g = clamp(128 + sn);
                        b = clamp(132 + sn);
                    }
                    case 4 -> { // Cobblestone
                        boolean mortar = ((x + (y / 4) * 3) % 5 == 0) || (y % 4 == 0);
                        int cn = mortar ? -28 : (rng.nextInt(24) - 10);
                        r = clamp(118 + cn);
                        g = clamp(118 + cn);
                        b = clamp(122 + cn);
                    }
                    case 5 -> { // Oak Wood bark (side)
                        int stripe = ((x % 4 == 0) ? -14 : 4) + (rng.nextInt(12) - 6);
                        r = clamp(104 + stripe);
                        g = clamp(78 + stripe);
                        b = clamp(46 + stripe);
                    }
                    case 6 -> { // Wood rings (top/bottom)
                        boolean border = x == 0 || x == 15 || y == 0 || y == 15;
                        int dist = Math.max(Math.abs(x - 7), Math.abs(y - 7));
                        if (border) {
                            r = clamp(104 + n);
                            g = clamp(78 + n);
                            b = clamp(46 + n);
                        } else {
                            int ring = (dist % 2 == 0) ? -10 : 6;
                            r = clamp(168 + ring + n / 2);
                            g = clamp(136 + ring + n / 2);
                            b = clamp(86 + ring + n / 2);
                        }
                    }
                    case 7 -> { // Leaves (Oak/Jungle)
                        int ln = rng.nextInt(32) - 16;
                        r = clamp(52 + ln);
                        g = clamp(132 + ln);
                        b = clamp(42 + ln);
                    }
                    case 8 -> { // Sand
                        int san = rng.nextInt(16) - 8;
                        r = clamp(222 + san);
                        g = clamp(208 + san);
                        b = clamp(152 + san);
                    }
                    case 9 -> { // Water (Minecraft-style clean semi-transparent blue with seamless periodic ripples)
                        double fx = x * (2.0 * Math.PI / 16.0);
                        double fy = y * (2.0 * Math.PI / 16.0);
                        int wave = (int) (Math.sin(fx + fy) * 12.0 + Math.sin(fx * 2.0 - fy) * 8.0 + Math.cos(fx - fy * 2.0) * 6.0);
                        r = clamp(42 + wave / 3);
                        g = clamp(88 + wave / 2);
                        b = clamp(215 + wave);
                        a = 185;
                    }
                    case 10 -> { // Wooden Planks
                        boolean seam = (y % 4 == 0) || ((x + (y / 4) * 7) % 16 == 0);
                        int pn = seam ? -24 : (rng.nextInt(14) - 7);
                        r = clamp(178 + pn);
                        g = clamp(142 + pn);
                        b = clamp(88 + pn);
                    }
                    case 11 -> { // Brick
                        boolean mortarH = (y % 4 == 0);
                        boolean mortarV = ((x + (y / 4) * 4) % 8 == 0);
                        if (mortarH || mortarV) {
                            r = clamp(185 + n / 2);
                            g = clamp(180 + n / 2);
                            b = clamp(172 + n / 2);
                        } else {
                            r = clamp(168 + n);
                            g = clamp(72 + n);
                            b = clamp(58 + n);
                        }
                    }
                    case 12 -> { // Glass
                        boolean frame = x == 0 || x == 15 || y == 0 || y == 15;
                        boolean streak = (x + y == 5) || (x + y == 6 && x > 1 && x < 5);
                        if (frame) {
                            r = 215;
                            g = 238;
                            b = 248;
                            a = 240;
                        } else if (streak) {
                            r = 240;
                            g = 250;
                            b = 255;
                            a = 180;
                        } else {
                            r = 180;
                            g = 220;
                            b = 240;
                            a = 45;
                        }
                    }
                    case 13 -> { // Snow
                        int swn = rng.nextInt(10) - 5;
                        r = clamp(242 + swn);
                        g = clamp(248 + swn);
                        b = clamp(255 + swn);
                    }
                    case 14 -> { // Glowstone
                        int dist = Math.abs(x - 7) + Math.abs(y - 7);
                        int gn = rng.nextInt(36) - 12 + (dist < 6 ? 25 : 0);
                        r = clamp(245 + gn);
                        g = clamp(205 + gn);
                        b = clamp(95 + gn);
                    }
                    case 15 -> { // Sandstone
                        int band = (y == 3 || y == 11) ? -14 : ((y < 3) ? 8 : 0);
                        r = clamp(216 + band + n / 2);
                        g = clamp(198 + band + n / 2);
                        b = clamp(142 + band + n / 2);
                    }
                    case 16 -> { // Cactus side
                        boolean rib = (x % 4 == 1);
                        boolean spine = (x % 4 == 0 && y % 4 == 2);
                        if (spine) {
                            r = 220; g = 215; b = 185;
                        } else {
                            int cn = rib ? -18 : 6;
                            r = clamp(46 + cn + n / 2);
                            g = clamp(128 + cn + n / 2);
                            b = clamp(42 + cn + n / 2);
                        }
                    }
                    case 17 -> { // Cactus top
                        int dist = Math.max(Math.abs(x - 7), Math.abs(y - 7));
                        int cn = (dist > 5) ? -16 : 8;
                        r = clamp(56 + cn + n / 2);
                        g = clamp(142 + cn + n / 2);
                        b = clamp(48 + cn + n / 2);
                    }
                    case 18 -> { // Birch wood side (white bark with dark horizontal notches)
                        boolean darkKnot = (y % 5 == 2 && ((x + y * 3) % 7 < 3));
                        if (darkKnot) {
                            r = clamp(60 + n);
                            g = clamp(58 + n);
                            b = clamp(54 + n);
                        } else {
                            r = clamp(228 + n / 2);
                            g = clamp(224 + n / 2);
                            b = clamp(214 + n / 2);
                        }
                    }
                    case 19 -> { // Birch wood top
                        boolean border = x == 0 || x == 15 || y == 0 || y == 15;
                        if (border) {
                            r = 225; g = 220; b = 210;
                        } else {
                            r = clamp(194 + n / 2);
                            g = clamp(178 + n / 2);
                            b = clamp(132 + n / 2);
                        }
                    }
                    case 20 -> { // Spruce wood side (dark brown bark)
                        int stripe = ((x % 4 == 0) ? -12 : 4) + (rng.nextInt(10) - 5);
                        r = clamp(64 + stripe);
                        g = clamp(44 + stripe);
                        b = clamp(26 + stripe);
                    }
                    case 21 -> { // Spruce leaves (cooler, dark pine needles)
                        int ln = rng.nextInt(24) - 12;
                        r = clamp(36 + ln);
                        g = clamp(92 + ln);
                        b = clamp(54 + ln);
                    }
                    case 22 -> { // Ice (semi-transparent)
                        boolean streak = ((x + y) % 7 == 0);
                        int in = streak ? 18 : (rng.nextInt(10) - 5);
                        r = clamp(145 + in);
                        g = clamp(195 + in);
                        b = clamp(248 + in);
                        a = 210;
                    }
                    case 23 -> { // Red Sand (Mesa)
                        int rsn = rng.nextInt(16) - 8;
                        r = clamp(196 + rsn);
                        g = clamp(106 + rsn);
                        b = clamp(44 + rsn);
                    }
                    case 24 -> { // Terracotta (Mesa layered strata)
                        int rowBand = (y / 3) % 4;
                        int[] br = {178, 204, 146, 188};
                        int[] bg = {96,  126, 78,  84};
                        int[] bb = {62,  72,  56,  52};
                        r = clamp(br[rowBand] + n / 2);
                        g = clamp(bg[rowBand] + n / 2);
                        b = clamp(bb[rowBand] + n / 2);
                    }
                    case 25 -> { // Snowy Grass side
                        int snowDepth = 3 + ((x * 5 + y * 3) % 3);
                        if (y < snowDepth) {
                            r = clamp(242 + n / 2);
                            g = clamp(248 + n / 2);
                            b = clamp(255 + n / 2);
                        } else {
                            r = clamp(134 + n);
                            g = clamp(96 + n);
                            b = clamp(67 + n);
                        }
                    }
                    case 26 -> { // Taiga Grass top
                        r = clamp(68 + n);
                        g = clamp(136 + n);
                        b = clamp(82 + n);
                    }
                    case 27 -> { // Taiga Grass side
                        int grassDepth = 3 + ((x * 7 + y * 3) % 3);
                        if (y < grassDepth) {
                            r = clamp(68 + n);
                            g = clamp(136 + n);
                            b = clamp(82 + n);
                        } else {
                            r = clamp(120 + n);
                            g = clamp(86 + n);
                            b = clamp(58 + n);
                        }
                    }
                    case 28 -> { // Spruce wood rings (top/bottom)
                        boolean border = x == 0 || x == 15 || y == 0 || y == 15;
                        if (border) {
                            r = clamp(64 + n / 2);
                            g = clamp(44 + n / 2);
                            b = clamp(26 + n / 2);
                        } else {
                            int dist = Math.max(Math.abs(x - 7), Math.abs(y - 7));
                            int ring = (dist % 2 == 0) ? -10 : 6;
                            r = clamp(122 + ring + n / 2);
                            g = clamp(92 + ring + n / 2);
                            b = clamp(58 + ring + n / 2);
                        }
                    }
                    case 29 -> { // Birch Leaves (bright, airy spring-green)
                        int ln = rng.nextInt(28) - 14;
                        r = clamp(86 + ln);
                        g = clamp(170 + ln);
                        b = clamp(50 + ln);
                    }
                    case 30 -> { // Jungle Wood bark (side - warm reddish brown with moss patches)
                        int stripe = ((x % 4 == 0) ? -14 : 4) + (rng.nextInt(10) - 5);
                        boolean moss = ((x + y * 2) % 11 < 2);
                        if (moss) {
                            r = clamp(58 + n);
                            g = clamp(118 + n);
                            b = clamp(44 + n);
                        } else {
                            r = clamp(94 + stripe);
                            g = clamp(66 + stripe);
                            b = clamp(36 + stripe);
                        }
                    }
                    case 31 -> { // Jungle Wood rings (top/bottom)
                        boolean border = x == 0 || x == 15 || y == 0 || y == 15;
                        if (border) {
                            r = clamp(94 + n / 2);
                            g = clamp(66 + n / 2);
                            b = clamp(36 + n / 2);
                        } else {
                            int dist = Math.max(Math.abs(x - 7), Math.abs(y - 7));
                            int ring = (dist % 2 == 0) ? -10 : 8;
                            r = clamp(152 + ring + n / 2);
                            g = clamp(102 + ring + n / 2);
                            b = clamp(54 + ring + n / 2);
                        }
                    }
                    case 32 -> { // Jungle Leaves (deep emerald with tropical flower specks)
                        int ln = rng.nextInt(30) - 15;
                        if (rng.nextInt(16) == 0) {
                            r = 230; g = 60; b = 110; // Orchid pink blossom
                        } else {
                            r = clamp(30 + ln);
                            g = clamp(140 + ln);
                            b = clamp(36 + ln);
                        }
                    }
                    case 33 -> { // Jungle Grass top (ultra-lush tropical emerald)
                        r = clamp(48 + n);
                        g = clamp(186 + n + (rng.nextInt(12) - 6));
                        b = clamp(38 + n);
                    }
                    case 34 -> { // Jungle Grass side
                        int grassDepth = 3 + ((x * 7 + y * 3) % 3);
                        if (y < grassDepth) {
                            r = clamp(48 + n);
                            g = clamp(186 + n);
                            b = clamp(38 + n);
                        } else {
                            r = clamp(96 + n);
                            g = clamp(68 + n);
                            b = clamp(46 + n);
                        }
                    }
                    case 35 -> { // Tall Grass (dense, multi-tufted cutout foliage blades)
                        boolean blade = (x == 1 && y >= 7 && y <= 11)
                                || (x == 2 && y >= 5 && y <= 13)
                                || (x == 3 && y >= 3)
                                || (x == 4 && y >= 4)
                                || (x == 5 && y >= 2)
                                || (x == 6 && y >= 1)
                                || (x == 7 && y >= 0)
                                || (x == 8 && y >= 1)
                                || (x == 9 && y >= 2)
                                || (x == 10 && y >= 3)
                                || (x == 11 && y >= 3)
                                || (x == 12 && y >= 4)
                                || (x == 13 && y >= 6 && y <= 14)
                                || (x == 14 && y >= 8 && y <= 12)
                                || (y >= 10 && x >= 2 && x <= 13)
                                || (y >= 13 && x >= 1 && x <= 14);
                        if (blade) {
                            int vShade = (y >= 12) ? clamp(130 + n / 2) : (y >= 6 ? clamp(180 + n) : clamp(220 + n));
                            r = vShade;
                            g = vShade;
                            b = vShade;
                            a = 255;
                        } else {
                            a = 0; r = 0; g = 0; b = 0;
                        }
                    }
                    case 36 -> { // Vine (cutout hanging vines)
                        boolean stem = (x == 3 && y <= 14) || (x == 8 && y <= 15) || (x == 12 && y <= 13)
                                || (x == 4 && y >= 6 && y <= 8) || (x == 7 && y >= 10 && y <= 12);
                        boolean leaf = ((x == 2 || x == 4) && (y % 4 == 1))
                                || ((x == 7 || x == 9) && (y % 4 == 2))
                                || ((x == 11 || x == 13) && (y % 4 == 0));
                        if (stem) {
                            r = clamp(52 + n);
                            g = clamp(108 + n);
                            b = clamp(36 + n);
                            a = 255;
                        } else if (leaf) {
                            r = clamp(36 + n);
                            g = clamp(144 + n);
                            b = clamp(40 + n);
                            a = 255;
                        } else {
                            a = 0; r = 0; g = 0; b = 0;
                        }
                    }
                    case 37 -> { // Flower / Jungle Orchid (cutout flower)
                        boolean fStem = (x == 7 || x == 8) && (y >= 6 && y <= 15);
                        boolean fLeaf = ((x == 5 || x == 6) && (y >= 11 && y <= 13)) || ((x == 9 || x == 10) && (y >= 10 && y <= 12));
                        boolean fPetal = (x >= 5 && x <= 10 && y >= 2 && y <= 7) && !(x == 5 && y == 2) && !(x == 10 && y == 2) && !(x == 5 && y == 7) && !(x == 10 && y == 7);
                        boolean fCenter = (x >= 7 && x <= 8 && y >= 4 && y <= 5);
                        if (fCenter) {
                            r = 255; g = 220; b = 40; a = 255;
                        } else if (fPetal) {
                            r = clamp(235 + n / 2);
                            g = clamp(48 + n / 2);
                            b = clamp(118 + n / 2);
                            a = 255;
                        } else if (fLeaf || fStem) {
                            r = clamp(54 + n);
                            g = clamp(146 + n);
                            b = clamp(42 + n);
                            a = 255;
                        } else {
                            a = 0; r = 0; g = 0; b = 0;
                        }
                    }
                    case 38 -> { // Rose / Red Tulip (crimson petals, yellow pistil, stem)
                        boolean fStem = (x == 7 || x == 8) && (y >= 7 && y <= 15);
                        boolean fLeaf = ((x == 4 || x == 5) && (y >= 10 && y <= 12)) || ((x == 9 || x == 10) && (y >= 11 && y <= 13));
                        boolean fPetal = (x >= 5 && x <= 10 && y >= 3 && y <= 8) && !(x == 5 && y == 3) && !(x == 10 && y == 3);
                        boolean fCenter = (x >= 7 && x <= 8 && y >= 5 && y <= 6);
                        if (fCenter) {
                            r = 255; g = 210; b = 30; a = 255;
                        } else if (fPetal) {
                            int shade = (y <= 4) ? 20 : (y >= 7 ? -25 : 0);
                            r = clamp(225 + shade + n / 2);
                            g = clamp(28 + shade / 2 + n / 2);
                            b = clamp(36 + shade / 2 + n / 2);
                            a = 255;
                        } else if (fLeaf || fStem) {
                            r = clamp(48 + n);
                            g = clamp(138 + n);
                            b = clamp(38 + n);
                            a = 255;
                        } else {
                            a = 0; r = 0; g = 0; b = 0;
                        }
                    }
                    case 39 -> { // Dandelion (golden yellow bloom, leafy base)
                        boolean fStem = (x == 7 || x == 8) && (y >= 6 && y <= 15);
                        boolean fLeaf = ((x == 4 || x == 5) && (y >= 12 && y <= 14)) || ((x == 10 || x == 11) && (y >= 11 && y <= 13));
                        boolean fPetal = (x >= 4 && x <= 11 && y >= 2 && y <= 7) && !((x == 4 || x == 11) && (y == 2 || y == 7));
                        boolean fCenter = (x >= 7 && x <= 8 && y >= 4 && y <= 5);
                        if (fCenter) {
                            r = 255; g = 175; b = 15; a = 255;
                        } else if (fPetal) {
                            int shade = (x + y) % 2 == 0 ? 12 : -12;
                            r = clamp(250 + shade);
                            g = clamp(220 + shade + n / 2);
                            b = clamp(30 + shade / 2);
                            a = 255;
                        } else if (fLeaf || fStem) {
                            r = clamp(60 + n);
                            g = clamp(152 + n);
                            b = clamp(42 + n);
                            a = 255;
                        } else {
                            a = 0; r = 0; g = 0; b = 0;
                        }
                    }
                    case 40 -> { // Blue Orchid (azure / sky blue petals, cyan heart)
                        boolean fStem = (x == 7 || x == 8) && (y >= 5 && y <= 15);
                        boolean fLeaf = ((x == 5 || x == 6) && (y >= 10 && y <= 12)) || ((x == 9 || x == 10) && (y >= 9 && y <= 11));
                        boolean fPetal = (x >= 4 && x <= 11 && y >= 2 && y <= 8) && !((x == 4 || x == 11) && (y == 2 || y == 8));
                        boolean fCenter = (x >= 7 && x <= 8 && y >= 4 && y <= 5);
                        if (fCenter) {
                            r = 230; g = 250; b = 255; a = 255;
                        } else if (fPetal) {
                            int shade = (y <= 4) ? 20 : (y >= 7 ? -20 : 0);
                            r = clamp(35 + shade + n / 2);
                            g = clamp(145 + shade + n / 2);
                            b = clamp(245 + shade);
                            a = 255;
                        } else if (fLeaf || fStem) {
                            r = clamp(42 + n);
                            g = clamp(135 + n);
                            b = clamp(45 + n);
                            a = 255;
                        } else {
                            a = 0; r = 0; g = 0; b = 0;
                        }
                    }
                    case 41 -> { // Allium (lavender / violet / purple blossom cluster)
                        boolean fStem = (x == 7 || x == 8) && (y >= 7 && y <= 15);
                        boolean fLeaf = ((x == 5 || x == 6) && (y >= 12 && y <= 14));
                        int distC = (x - 7) * (x - 7) + (y - 5) * (y - 5);
                        boolean fPetal = distC <= 12;
                        if (fPetal) {
                            int shade = ((x + y * 2) % 3 == 0) ? 25 : (((x * 2 + y) % 3 == 1) ? -20 : 0);
                            r = clamp(165 + shade + n / 2);
                            g = clamp(90 + shade / 2 + n / 2);
                            b = clamp(215 + shade);
                            a = 255;
                        } else if (fLeaf || fStem) {
                            r = clamp(50 + n);
                            g = clamp(140 + n);
                            b = clamp(40 + n);
                            a = 255;
                        } else {
                            a = 0; r = 0; g = 0; b = 0;
                        }
                    }
                    case 42 -> { // Sugar Cane: bamboo/reed vertical stalks with segmented nodes
                        boolean stalk1 = (x >= 2 && x <= 4);
                        boolean stalk2 = (x >= 7 && x <= 9);
                        boolean stalk3 = (x >= 12 && x <= 14);
                        boolean isNode = (y == 3 || y == 8 || y == 13);
                        if (stalk1 || stalk2 || stalk3) {
                            int baseG = isNode ? 145 : (180 + n);
                            r = clamp(isNode ? 130 : 90 + n / 2);
                            g = clamp(baseG);
                            b = clamp(isNode ? 40 : 35 + n / 3);
                            a = 255;
                        } else if ((x == 5 && (y == 5 || y == 10)) || (x == 11 && (y == 6 || y == 11))) {
                            r = clamp(70 + n / 2);
                            g = clamp(170 + n);
                            b = clamp(30);
                            a = 255;
                        } else {
                            a = 0; r = 0; g = 0; b = 0;
                        }
                    }
                    case 43 -> { // Seagrass: underwater wavy marine grass blades
                        int wave1 = (int) (Math.sin(y * 0.5) * 1.5);
                        int wave2 = (int) (Math.cos(y * 0.4) * 1.5);
                        boolean blade1 = (x == 4 + wave1 || x == 5 + wave1) && y >= 3;
                        boolean blade2 = (x == 10 + wave2 || x == 11 + wave2) && y >= 1;
                        boolean blade3 = (x == 7 || x == 8) && y >= 6;
                        if (blade1 || blade2 || blade3) {
                            r = clamp(30 + n / 2);
                            g = clamp(155 + n + (15 - y) * 2);
                            b = clamp(55 + n / 3);
                            a = 230;
                        } else {
                            a = 0; r = 0; g = 0; b = 0;
                        }
                    }
                    case 44 -> { // Kelp: tall olive seaweed stalk with side leaves
                        boolean stalk = (x == 7 || x == 8);
                        boolean frondL = (x >= 3 && x <= 6) && ((y % 4 == 1) || (y % 4 == 2));
                        boolean frondR = (x >= 9 && x <= 12) && ((y % 4 == 3) || (y % 4 == 0));
                        if (stalk || frondL || frondR) {
                            r = clamp(45 + n / 2);
                            g = clamp(125 + n + (frondL || frondR ? 20 : 0));
                            b = clamp(35 + n / 3);
                            a = 240;
                        } else {
                            a = 0; r = 0; g = 0; b = 0;
                        }
                    }
                    case 45 -> { // Coral: vibrant warm ocean polyp reef
                        int coralPattern = (x * 7 ^ y * 13 ^ n) & 7;
                        if (coralPattern < 3) {
                            r = clamp(235 + n / 2);
                            g = clamp(70 + n / 2);
                            b = clamp(140 + n);
                        } else if (coralPattern < 6) {
                            r = clamp(40 + n / 3);
                            g = clamp(200 + n);
                            b = clamp(210 + n / 2);
                        } else {
                            r = clamp(240 + n);
                            g = clamp(180 + n / 2);
                            b = clamp(40);
                        }
                        a = 255;
                    }
                    case 46 -> { // Gravel: textured pebbles and slate
                        int pebble = (x * 3 + y * 7 + (x ^ y)) % 5;
                        int gVal = 130 + n + pebble * 14;
                        r = clamp(gVal - 5);
                        g = clamp(gVal);
                        b = clamp(gVal + 8);
                        a = 255;
                    }
                    case 47 -> { // Clay: smooth earthy light blue-gray
                        int cVal = 160 + n / 2;
                        r = clamp(cVal - 10);
                        g = clamp(cVal - 4);
                        b = clamp(cVal + 12);
                        a = 255;
                    }
                    case 48 -> { // Missing texture fallback: classic 16x16 purple/black checkerboard
                        boolean isPurple = ((x < 8 && y < 8) || (x >= 8 && y >= 8));
                        r = isPurple ? 240 : 16;
                        g = isPurple ? 20 : 16;
                        b = isPurple ? 240 : 16;
                        a = 255;
                    }
                    default -> {
                        boolean isPurple = ((x < 8 && y < 8) || (x >= 8 && y >= 8));
                        r = isPurple ? 240 : 16;
                        g = isPurple ? 20 : 16;
                        b = isPurple ? 240 : 16;
                        a = 255;
                    }
                }

                int px = tileX + x;
                int py = tileY + y;
                pixels[py * ATLAS_SIZE + px] = ((a & 0xFF) << 24) | ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);
            }
        }
    }

    private static int clamp(int v) {
        return Math.max(0, Math.min(255, v));
    }

    public static float getU0(int tileIndex) {
        int tx = tileIndex % GRID_SIZE;
        return (tx * TILE_SIZE + 0.25f) / ATLAS_SIZE;
    }

    public static float getV0(int tileIndex) {
        int ty = tileIndex / GRID_SIZE;
        return (ty * TILE_SIZE + 0.25f) / ATLAS_SIZE;
    }

    public static float getU1(int tileIndex) {
        int tx = tileIndex % GRID_SIZE;
        return ((tx + 1) * TILE_SIZE - 0.25f) / ATLAS_SIZE;
    }

    public static float getV1(int tileIndex) {
        int ty = tileIndex / GRID_SIZE;
        return ((ty + 1) * TILE_SIZE - 0.25f) / ATLAS_SIZE;
    }

    public void bind() {
        glBindTexture(GL_TEXTURE_2D, textureId);
    }

    public void cleanup() {
        glDeleteTextures(textureId);
    }
}
