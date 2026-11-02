package com.voxel.render;

import com.voxel.util.Logger;
import org.lwjgl.BufferUtils;

import java.nio.ByteBuffer;
import java.util.Random;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE;

public class TextureAtlas {
    public static final int TILE_SIZE = 16;
    public static final int GRID_SIZE = 8; // 8x8 tiles = 64 tiles total
    public static final int ATLAS_SIZE = TILE_SIZE * GRID_SIZE;

    private final int textureId;

    public TextureAtlas() {
        ByteBuffer buffer = BufferUtils.createByteBuffer(ATLAS_SIZE * ATLAS_SIZE * 4);
        int[] pixels = new int[ATLAS_SIZE * ATLAS_SIZE];

        for (int tile = 0; tile <= 41; tile++) {
            generateTile(pixels, tile);
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
        Logger.info("TextureAtlas", "Generated procedural texture atlas (%dx%d, 42 tiles, OpenGL ID: %d)", ATLAS_SIZE, ATLAS_SIZE, textureId);
    }

    private void generateTile(int[] pixels, int tileIndex) {
        int tileX = (tileIndex % GRID_SIZE) * TILE_SIZE;
        int tileY = (tileIndex / GRID_SIZE) * TILE_SIZE;
        Random rng = new Random(1337L + tileIndex * 997L);

        for (int y = 0; y < TILE_SIZE; y++) {
            for (int x = 0; x < TILE_SIZE; x++) {
                int r = 255, g = 0, b = 255, a = 255;
                int n = rng.nextInt(21) - 10;

                switch (tileIndex) {
                    case 0 -> { // Grass top
                        r = clamp(88 + n);
                        g = clamp(168 + n + (rng.nextInt(12) - 6));
                        b = clamp(56 + n);
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
                    case 9 -> { // Water (semi-transparent with wave contours)
                        int wave1 = (int) (Math.sin((x + y) * 0.7) * 16.0);
                        int wave2 = (int) (Math.cos((x - y) * 0.8) * 12.0);
                        int wave = wave1 + wave2;
                        r = clamp(34 + wave / 2);
                        g = clamp(105 + wave / 2);
                        b = clamp(225 + wave);
                        a = 180;
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
                            if (y >= 12) {
                                // Shaded, deep root green
                                r = clamp(50 + n / 2);
                                g = clamp(130 + n / 2);
                                b = clamp(32 + n / 2);
                            } else if (y >= 6) {
                                // Vibrant meadow green
                                r = clamp(78 + n);
                                g = clamp(174 + n);
                                b = clamp(48 + n);
                            } else {
                                // Sunlit bright tips
                                r = clamp(102 + n);
                                g = clamp(202 + n);
                                b = clamp(58 + n);
                            }
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
