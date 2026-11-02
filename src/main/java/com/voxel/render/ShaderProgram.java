package com.voxel.render;

import com.voxel.util.Logger;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryStack;

import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.opengl.GL20.*;

public class ShaderProgram {
    private final int programId;
    private final Map<String, Integer> uniforms = new HashMap<>();

    public ShaderProgram(String vertexSrc, String fragmentSrc) {
        int vs = compileShader(GL_VERTEX_SHADER, vertexSrc);
        int fs = compileShader(GL_FRAGMENT_SHADER, fragmentSrc);

        programId = glCreateProgram();
        glAttachShader(programId, vs);
        glAttachShader(programId, fs);
        glLinkProgram(programId);

        if (glGetProgrami(programId, GL_LINK_STATUS) == GL_FALSE) {
            String errorLog = glGetProgramInfoLog(programId);
            Logger.error("ShaderProgram", "Blad linkowania programu shaderow: %s", errorLog);
            throw new RuntimeException("Shader link error: " + errorLog);
        }

        glDeleteShader(vs);
        glDeleteShader(fs);
        Logger.debug("ShaderProgram", "Zaladowano i skompilowano shader program (ID: %d)", programId);
    }

    private int compileShader(int type, String src) {
        int id = glCreateShader(type);
        glShaderSource(id, src);
        glCompileShader(id);
        if (glGetShaderi(id, GL_COMPILE_STATUS) == GL_FALSE) {
            String errorLog = glGetShaderInfoLog(id);
            Logger.error("ShaderProgram", "Blad kompilacji shadera (typ %d): %s", type, errorLog);
            throw new RuntimeException("Shader compile error: " + errorLog);
        }
        return id;
    }

    private int getLocation(String name) {
        return uniforms.computeIfAbsent(name, n -> glGetUniformLocation(programId, n));
    }

    public void bind() {
        glUseProgram(programId);
    }

    public void unbind() {
        glUseProgram(0);
    }

    public void setUniform(String name, Matrix4f mat) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer fb = stack.mallocFloat(16);
            mat.get(fb);
            glUniformMatrix4fv(getLocation(name), false, fb);
        }
    }

    public void setUniform(String name, Vector3f vec) {
        glUniform3f(getLocation(name), vec.x, vec.y, vec.z);
    }

    public void setUniform(String name, Vector4f vec) {
        glUniform4f(getLocation(name), vec.x, vec.y, vec.z, vec.w);
    }

    public void setUniform(String name, float val) {
        glUniform1f(getLocation(name), val);
    }

    public void setUniform(String name, int val) {
        glUniform1i(getLocation(name), val);
    }

    public void cleanup() {
        glDeleteProgram(programId);
    }

    public static ShaderProgram createChunkShader() {
        String vs = """
                #version 330 core
                layout (location = 0) in vec3 aPos;
                layout (location = 1) in vec2 aTexCoord;
                layout (location = 2) in float aSkyLight;
                layout (location = 3) in float aBlockLight;

                uniform mat4 uProjView;
                uniform vec3 uChunkOffset;

                out vec2 vTexCoord;
                out vec3 vWorldPos;
                out float vSkyLight;
                out float vBlockLight;
                out float vDist;

                void main() {
                    vec3 relPos = aPos + uChunkOffset;
                    gl_Position = uProjView * vec4(relPos, 1.0);
                    vTexCoord = aTexCoord;
                    vWorldPos = relPos;
                    vSkyLight = aSkyLight;
                    vBlockLight = aBlockLight;
                    vDist = length(relPos);
                }
                """;

        String fs = """
                #version 330 core
                in vec2 vTexCoord;
                in vec3 vWorldPos;
                in float vSkyLight;
                in float vBlockLight;
                in float vDist;

                uniform sampler2D uTexture;
                uniform float uSunIntensity;
                uniform vec3 uSkyColor;
                uniform vec3 uTintColor;
                uniform float uFogStart;
                uniform float uFogEnd;
                uniform float uTime;

                out vec4 FragColor;

                void main() {
                    vec2 coord = vTexCoord;
                    // Detect water tile in atlas (Tile 9: strictly inside [0.126, 0.249] to prevent neighbor bleeding)
                    bool isWater = (coord.x >= 0.126 && coord.x <= 0.249 && coord.y >= 0.126 && coord.y <= 0.249);
                    vec4 texColor;
                    if (isWater) {
                        // World-space continuous wave coordinates across all adjacent water blocks
                        vec2 worldXZ = vWorldPos.xz;
                        float w1 = sin(worldXZ.y * 1.5 + worldXZ.x * 0.7 + uTime * 2.8) * 0.04;
                        float w2 = cos(worldXZ.x * 1.4 - worldXZ.y * 0.7 + uTime * 2.1) * 0.04;
                        vec2 continuousUV = fract(worldXZ + vec2(w1, w2));
                        continuousUV = clamp(continuousUV, 0.02, 0.98);
                        texColor = texture(uTexture, vec2(0.125, 0.125) + continuousUV * 0.125);
                        // Wave crest specular and shimmering sunlight
                        float shimmer = sin((worldXZ.x + worldXZ.y) * 2.2 + uTime * 3.4) * 0.09 + cos(worldXZ.x * 1.6 - uTime * 1.8) * 0.05;
                        texColor.rgb = clamp(texColor.rgb + vec3(0.06, 0.14, 0.24) * shimmer, 0.0, 1.0);
                        texColor.a = 0.74;
                    } else {
                        texColor = texture(uTexture, coord);
                    }

                    if (texColor.a < 0.05) {
                        discard;
                    }
                    float light = max(vSkyLight * uSunIntensity, vBlockLight);
                    vec3 litColor = texColor.rgb * light * uTintColor;

                    float fogFactor = clamp((uFogEnd - vDist) / (uFogEnd - uFogStart), 0.0, 1.0);
                    vec3 finalColor = mix(uSkyColor, litColor, fogFactor);
                    FragColor = vec4(finalColor, texColor.a);
                }
                """;
        return new ShaderProgram(vs, fs);
    }

    public static ShaderProgram createColorShader() {
        String vs = """
                #version 330 core
                layout (location = 0) in vec3 aPos;
                uniform mat4 uMVP;
                void main() {
                    gl_Position = uMVP * vec4(aPos, 1.0);
                }
                """;
        String fs = """
                #version 330 core
                uniform vec4 uColor;
                out vec4 FragColor;
                void main() {
                    FragColor = uColor;
                }
                """;
        return new ShaderProgram(vs, fs);
    }

    public static ShaderProgram createUiTextureShader() {
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
                out vec4 FragColor;
                void main() {
                    FragColor = texture(uTexture, vTexCoord);
                }
                """;
        return new ShaderProgram(vs, fs);
    }
}
