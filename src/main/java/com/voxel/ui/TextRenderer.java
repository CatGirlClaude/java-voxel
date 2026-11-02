package com.voxel.ui;

import com.voxel.render.ShaderProgram;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.lwjgl.BufferUtils;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;
import static org.lwjgl.opengl.GL30.*;

public class TextRenderer {
    private static final int CHAR_W = 6;
    private static final int CHAR_H = 8;
    private static final int COLS = 16;
    private static final int ROWS = 6; // ASCII 32..127 (96 chars)
    private static final int TEX_W = COLS * CHAR_W;
    private static final int TEX_H = ROWS * CHAR_H;

    private final int textureId;
    private final int vao;
    private final int vbo;
    private final ShaderProgram fontShader;

    // 5x7 font bitmaps for ASCII 32..126 (each entry has 7 rows of 5-bit patterns, MSB on left)
    private static final int[][] GLYPHS = new int[96][7];

    static {
        def(' ', 0x00,0x00,0x00,0x00,0x00,0x00,0x00);
        def('!', 0x04,0x04,0x04,0x04,0x04,0x00,0x04);
        def('"', 0x0A,0x0A,0x0A,0x00,0x00,0x00,0x00);
        def('#', 0x0A,0x0A,0x1F,0x0A,0x1F,0x0A,0x0A);
        def('$', 0x04,0x0F,0x14,0x0E,0x05,0x1E,0x04);
        def('%', 0x18,0x19,0x02,0x04,0x08,0x13,0x03);
        def('&', 0x0C,0x12,0x14,0x08,0x15,0x12,0x0D);
        def('\'',0x04,0x04,0x08,0x00,0x00,0x00,0x00);
        def('(', 0x02,0x04,0x08,0x08,0x08,0x04,0x02);
        def(')', 0x08,0x04,0x02,0x02,0x02,0x04,0x08);
        def('*', 0x00,0x04,0x15,0x0E,0x15,0x04,0x00);
        def('+', 0x00,0x04,0x04,0x1F,0x04,0x04,0x00);
        def(',', 0x00,0x00,0x00,0x00,0x04,0x04,0x08);
        def('-', 0x00,0x00,0x00,0x1F,0x00,0x00,0x00);
        def('.', 0x00,0x00,0x00,0x00,0x00,0x0C,0x0C);
        def('/', 0x01,0x01,0x02,0x04,0x08,0x10,0x10);

        def('0', 0x0E,0x11,0x13,0x15,0x19,0x11,0x0E);
        def('1', 0x04,0x0C,0x04,0x04,0x04,0x04,0x0E);
        def('2', 0x0E,0x11,0x01,0x06,0x08,0x10,0x1F);
        def('3', 0x1F,0x02,0x04,0x02,0x01,0x11,0x0E);
        def('4', 0x02,0x06,0x0A,0x12,0x1F,0x02,0x02);
        def('5', 0x1F,0x10,0x1E,0x01,0x01,0x11,0x0E);
        def('6', 0x06,0x08,0x10,0x1E,0x11,0x11,0x0E);
        def('7', 0x1F,0x01,0x02,0x04,0x08,0x08,0x08);
        def('8', 0x0E,0x11,0x11,0x0E,0x11,0x11,0x0E);
        def('9', 0x0E,0x11,0x11,0x0F,0x01,0x02,0x0C);

        def(':', 0x00,0x0C,0x0C,0x00,0x0C,0x0C,0x00);
        def(';', 0x00,0x0C,0x0C,0x00,0x0C,0x04,0x08);
        def('<', 0x02,0x04,0x08,0x10,0x08,0x04,0x02);
        def('=', 0x00,0x00,0x1F,0x00,0x1F,0x00,0x00);
        def('>', 0x08,0x04,0x02,0x01,0x02,0x04,0x08);
        def('?', 0x0E,0x11,0x01,0x02,0x04,0x00,0x04);
        def('@', 0x0E,0x11,0x17,0x15,0x17,0x10,0x0E);

        def('A', 0x0E,0x11,0x11,0x1F,0x11,0x11,0x11);
        def('B', 0x1E,0x11,0x11,0x1E,0x11,0x11,0x1E);
        def('C', 0x0E,0x11,0x10,0x10,0x10,0x11,0x0E);
        def('D', 0x1E,0x11,0x11,0x11,0x11,0x11,0x1E);
        def('E', 0x1F,0x10,0x10,0x1E,0x10,0x10,0x1F);
        def('F', 0x1F,0x10,0x10,0x1E,0x10,0x10,0x10);
        def('G', 0x0E,0x11,0x10,0x17,0x11,0x11,0x0F);
        def('H', 0x11,0x11,0x11,0x1F,0x11,0x11,0x11);
        def('I', 0x0E,0x04,0x04,0x04,0x04,0x04,0x0E);
        def('J', 0x07,0x02,0x02,0x02,0x02,0x12,0x0C);
        def('K', 0x11,0x12,0x14,0x18,0x14,0x12,0x11);
        def('L', 0x10,0x10,0x10,0x10,0x10,0x10,0x1F);
        def('M', 0x11,0x1B,0x15,0x15,0x11,0x11,0x11);
        def('N', 0x11,0x19,0x15,0x13,0x11,0x11,0x11);
        def('O', 0x0E,0x11,0x11,0x11,0x11,0x11,0x0E);
        def('P', 0x1E,0x11,0x11,0x1E,0x10,0x10,0x10);
        def('Q', 0x0E,0x11,0x11,0x11,0x15,0x12,0x0D);
        def('R', 0x1E,0x11,0x11,0x1E,0x14,0x12,0x11);
        def('S', 0x0F,0x10,0x10,0x0E,0x01,0x01,0x1E);
        def('T', 0x1F,0x04,0x04,0x04,0x04,0x04,0x04);
        def('U', 0x11,0x11,0x11,0x11,0x11,0x11,0x0E);
        def('V', 0x11,0x11,0x11,0x11,0x11,0x0A,0x04);
        def('W', 0x11,0x11,0x11,0x15,0x15,0x1B,0x11);
        def('X', 0x11,0x11,0x0A,0x04,0x0A,0x11,0x11);
        def('Y', 0x11,0x11,0x0A,0x04,0x04,0x04,0x04);
        def('Z', 0x1F,0x01,0x02,0x04,0x08,0x10,0x1F);

        def('[', 0x0E,0x08,0x08,0x08,0x08,0x08,0x0E);
        def('\\',0x10,0x10,0x08,0x04,0x02,0x01,0x01);
        def(']', 0x0E,0x02,0x02,0x02,0x02,0x02,0x0E);
        def('^', 0x04,0x0A,0x11,0x00,0x00,0x00,0x00);
        def('_', 0x00,0x00,0x00,0x00,0x00,0x00,0x1F);
        def('`', 0x08,0x04,0x02,0x00,0x00,0x00,0x00);

        def('a', 0x00,0x00,0x0E,0x01,0x0F,0x11,0x0F);
        def('b', 0x10,0x10,0x1E,0x11,0x11,0x11,0x1E);
        def('c', 0x00,0x00,0x0E,0x11,0x10,0x11,0x0E);
        def('d', 0x01,0x01,0x0F,0x11,0x11,0x11,0x0F);
        def('e', 0x00,0x00,0x0E,0x11,0x1F,0x10,0x0E);
        def('f', 0x06,0x08,0x1E,0x08,0x08,0x08,0x08);
        def('g', 0x00,0x00,0x0F,0x11,0x0F,0x01,0x0E);
        def('h', 0x10,0x10,0x1E,0x11,0x11,0x11,0x11);
        def('i', 0x04,0x00,0x0C,0x04,0x04,0x04,0x0E);
        def('j', 0x02,0x00,0x06,0x02,0x02,0x12,0x0C);
        def('k', 0x10,0x10,0x12,0x14,0x18,0x14,0x12);
        def('l', 0x0C,0x04,0x04,0x04,0x04,0x04,0x0E);
        def('m', 0x00,0x00,0x1A,0x15,0x15,0x11,0x11);
        def('n', 0x00,0x00,0x1E,0x11,0x11,0x11,0x11);
        def('o', 0x00,0x00,0x0E,0x11,0x11,0x11,0x0E);
        def('p', 0x00,0x00,0x1E,0x11,0x1E,0x10,0x10);
        def('q', 0x00,0x00,0x0F,0x11,0x0F,0x01,0x01);
        def('r', 0x00,0x00,0x16,0x19,0x10,0x10,0x10);
        def('s', 0x00,0x00,0x0F,0x10,0x0E,0x01,0x1E);
        def('t', 0x08,0x08,0x1E,0x08,0x08,0x09,0x06);
        def('u', 0x00,0x00,0x11,0x11,0x11,0x13,0x0D);
        def('v', 0x00,0x00,0x11,0x11,0x11,0x0A,0x04);
        def('w', 0x00,0x00,0x11,0x11,0x15,0x15,0x0A);
        def('x', 0x00,0x00,0x11,0x0A,0x04,0x0A,0x11);
        def('y', 0x00,0x00,0x11,0x11,0x0F,0x01,0x0E);
        def('z', 0x00,0x00,0x1F,0x02,0x04,0x08,0x1F);

        def('{', 0x02,0x04,0x04,0x08,0x04,0x04,0x02);
        def('|', 0x04,0x04,0x04,0x04,0x04,0x04,0x04);
        def('}', 0x08,0x04,0x04,0x02,0x04,0x04,0x08);
        def('~', 0x00,0x00,0x08,0x15,0x02,0x00,0x00);
    }

    private static void def(char c, int r0, int r1, int r2, int r3, int r4, int r5, int r6) {
        int idx = c - 32;
        if (idx >= 0 && idx < 96) {
            GLYPHS[idx] = new int[]{r0, r1, r2, r3, r4, r5, r6};
        }
    }

    public TextRenderer() {
        String vs = """
                #version 330 core
                layout (location = 0) in vec2 aPos;
                layout (location = 1) in vec2 aTexCoord;
                uniform mat4 uProj;
                out vec2 vTexCoord;
                void main() {
                    gl_Position = uProj * vec4(aPos, 0.0, 1.0);
                    vTexCoord = aTexCoord;
                }
                """;
        String fs = """
                #version 330 core
                in vec2 vTexCoord;
                uniform sampler2D uTexture;
                uniform vec4 uColor;
                out vec4 FragColor;
                void main() {
                    vec4 sample = texture(uTexture, vTexCoord);
                    if (sample.a < 0.1) discard;
                    FragColor = vec4(uColor.rgb, sample.a * uColor.a);
                }
                """;
        fontShader = new ShaderProgram(vs, fs);

        ByteBuffer buf = BufferUtils.createByteBuffer(TEX_W * TEX_H * 4);
        for (int py = 0; py < TEX_H; py++) {
            int rowIdx = py / CHAR_H;
            int ly = py % CHAR_H;
            for (int px = 0; px < TEX_W; px++) {
                int colIdx = px / CHAR_W;
                int lx = px % CHAR_W;
                int charIdx = rowIdx * COLS + colIdx;

                boolean lit = false;
                if (charIdx >= 0 && charIdx < 96 && lx < 5 && ly < 7) {
                    int rowBits = GLYPHS[charIdx][ly];
                    lit = ((rowBits >> (4 - lx)) & 1) != 0;
                }
                if (lit) {
                    buf.put((byte) 255).put((byte) 255).put((byte) 255).put((byte) 255);
                } else {
                    buf.put((byte) 0).put((byte) 0).put((byte) 0).put((byte) 0);
                }
            }
        }
        buf.flip();

        textureId = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, textureId);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, TEX_W, TEX_H, 0, GL_RGBA, GL_UNSIGNED_BYTE, buf);
        glBindTexture(GL_TEXTURE_2D, 0);

        vao = glGenVertexArrays();
        vbo = glGenBuffers();
        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, 4096 * Float.BYTES, GL_DYNAMIC_DRAW);
        glVertexAttribPointer(0, 2, GL_FLOAT, false, 4 * Float.BYTES, 0);
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(1, 2, GL_FLOAT, false, 4 * Float.BYTES, 2L * Float.BYTES);
        glEnableVertexAttribArray(1);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);
    }

    public static String normalizePolish(String text) {
        if (text == null) return "";
        StringBuilder sb = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            char mapped = switch (c) {
                case 'ą' -> 'a';
                case 'ć' -> 'c';
                case 'ę' -> 'e';
                case 'ł' -> 'l';
                case 'ń' -> 'n';
                case 'ó' -> 'o';
                case 'ś' -> 's';
                case 'ź', 'ż' -> 'z';
                case 'Ą' -> 'A';
                case 'Ć' -> 'C';
                case 'Ę' -> 'E';
                case 'Ł' -> 'L';
                case 'Ń' -> 'N';
                case 'Ó' -> 'O';
                case 'Ś' -> 'S';
                case 'Ź', 'Ż' -> 'Z';
                default -> (c >= 32 && c <= 126) ? c : '?';
            };
            sb.append(mapped);
        }
        return sb.toString();
    }

    public float getTextWidth(String text, float scale) {
        if (text == null || text.isEmpty()) return 0;
        return text.length() * CHAR_W * scale;
    }

    public float getLineHeight(float scale) {
        return (CHAR_H + 2) * scale;
    }

    public void drawTextWithShadow(Matrix4f orthoProj, String rawText, float x, float y, float scale, Vector4f color) {
        Vector4f shadow = new Vector4f(0.05f, 0.05f, 0.07f, color.w * 0.9f);
        drawText(orthoProj, rawText, x + scale, y + scale, scale, shadow);
        drawText(orthoProj, rawText, x, y, scale, color);
    }

    public void drawTextCenteredInBox(Matrix4f orthoProj, String rawText, float x, float y, float w, float h, float baseScale, Vector4f color) {
        drawTextCenteredInBox(orthoProj, rawText, x, y, w, h, baseScale, 16.0f, color);
    }

    public void drawTextCenteredInBox(Matrix4f orthoProj, String rawText, float x, float y, float w, float h, float baseScale, float padding, Vector4f color) {
        if (rawText == null || rawText.isEmpty()) return;
        // Global button text fitting: ensure at least 14px horizontal breathing room so text never touches button borders
        float effectivePadding = Math.max(padding, 14.0f);
        float maxAllowedW = Math.max(8.0f, w - effectivePadding);
        float maxAllowedH = Math.max(8.0f, h - 4.0f);

        float textW = getTextWidth(rawText, baseScale);
        float scale = baseScale;

        // Auto-scale down smoothly if text exceeds allowed width or height
        if (textW > maxAllowedW && textW > 0.0f) {
            scale = baseScale * (maxAllowedW / textW);
        }
        if (CHAR_H * scale > maxAllowedH) {
            scale = maxAllowedH / CHAR_H;
        }
        // Allow crisp downscaling down to 0.45f before resorting to truncation
        scale = Math.max(0.45f, scale);

        String textToDraw = rawText;
        float curW = getTextWidth(textToDraw, scale);
        if (curW > maxAllowedW && textToDraw.length() > 3) {
            while (textToDraw.length() > 2 && getTextWidth(textToDraw + "...", scale) > maxAllowedW) {
                textToDraw = textToDraw.substring(0, textToDraw.length() - 1);
            }
            textToDraw = textToDraw + "...";
            curW = getTextWidth(textToDraw, scale);
        }

        // Center within box with safety bounds so text never bleeds outside button borders
        float tx = x + (w - curW) * 0.5f;
        if (tx < x + 3.0f) tx = x + 3.0f;
        float ty = y + (h - CHAR_H * scale) * 0.5f;
        drawTextWithShadow(orthoProj, textToDraw, tx, ty, scale, color);
    }

    public void drawText(Matrix4f orthoProj, String rawText, float x, float y, float scale, Vector4f color) {
        String text = normalizePolish(rawText);
        if (text.isEmpty()) return;

        int len = text.length();
        float[] verts = new float[len * 24];
        int vi = 0;

        float cw = CHAR_W * scale;
        float ch = CHAR_H * scale;
        float curX = x;

        for (int i = 0; i < len; i++) {
            char c = text.charAt(i);
            int idx = c - 32;
            if (idx < 0 || idx >= 96) idx = '?' - 32;

            int col = idx % COLS;
            int row = idx / COLS;

            float u0 = (float) (col * CHAR_W) / TEX_W;
            float v0 = (float) (row * CHAR_H) / TEX_H;
            float u1 = (float) ((col + 1) * CHAR_W) / TEX_W;
            float v1 = (float) ((row + 1) * CHAR_H) / TEX_H;

            float x0 = curX;
            float y0 = y;
            float x1 = curX + cw;
            float y1 = y + ch;

            verts[vi++] = x0; verts[vi++] = y0; verts[vi++] = u0; verts[vi++] = v0;
            verts[vi++] = x1; verts[vi++] = y0; verts[vi++] = u1; verts[vi++] = v0;
            verts[vi++] = x1; verts[vi++] = y1; verts[vi++] = u1; verts[vi++] = v1;

            verts[vi++] = x1; verts[vi++] = y1; verts[vi++] = u1; verts[vi++] = v1;
            verts[vi++] = x0; verts[vi++] = y1; verts[vi++] = u0; verts[vi++] = v1;
            verts[vi++] = x0; verts[vi++] = y0; verts[vi++] = u0; verts[vi++] = v0;

            curX += cw;
        }

        fontShader.bind();
        fontShader.setUniform("uProj", orthoProj);
        fontShader.setUniform("uTexture", 0);
        fontShader.setUniform("uColor", color);

        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, textureId);
        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);

        FloatBuffer fb = BufferUtils.createFloatBuffer(vi);
        fb.put(verts, 0, vi).flip();
        glBufferData(GL_ARRAY_BUFFER, fb, GL_DYNAMIC_DRAW);
        glDrawArrays(GL_TRIANGLES, 0, vi / 4);

        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);
        glBindTexture(GL_TEXTURE_2D, 0);
        fontShader.unbind();
    }

    public void cleanup() {
        fontShader.cleanup();
        glDeleteTextures(textureId);
        glDeleteBuffers(vbo);
        glDeleteVertexArrays(vao);
    }
}
