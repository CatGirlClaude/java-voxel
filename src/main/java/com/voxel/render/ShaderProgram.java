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
            Logger.error("ShaderProgram", "Shader program linking error: %s", errorLog);
            throw new RuntimeException("Shader link error: " + errorLog);
        }

        glDeleteShader(vs);
        glDeleteShader(fs);
        Logger.debug("ShaderProgram", "Loaded and compiled shader program (ID: %d)", programId);
    }

    private int compileShader(int type, String src) {
        int id = glCreateShader(type);
        glShaderSource(id, src);
        glCompileShader(id);
        if (glGetShaderi(id, GL_COMPILE_STATUS) == GL_FALSE) {
            String errorLog = glGetShaderInfoLog(id);
            Logger.error("ShaderProgram", "Shader compilation error (type %d): %s", type, errorLog);
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
                layout (location = 4) in vec3 aColor;

                uniform mat4 uProjView;
                uniform vec3 uChunkOffset;
                uniform float uTime;
                uniform float uIsWaterPass;

                out vec2 vTexCoord;
                out vec3 vWorldPos;
                out float vSkyLight;
                out float vBlockLight;
                out float vDist;
                out vec3 vColor;
                out float vIsWater;

                void main() {
                    vec3 relPos = aPos + uChunkOffset;
                    // Strictly detect water vertices: ONLY active in water pass AND inside tile 9 UV range
                    bool isWater = (uIsWaterPass > 0.5) &&
                                   (aTexCoord.x >= 0.1265 && aTexCoord.x <= 0.2485 &&
                                    aTexCoord.y >= 0.1265 && aTexCoord.y <= 0.2485);
                    vIsWater = isWater ? 1.0 : 0.0;
                    if (isWater) {
                        float wave1 = sin(relPos.x * 1.2 + uTime * 2.0) * cos(relPos.z * 1.2 + uTime * 1.7) * 0.032;
                        float wave2 = sin((relPos.x + relPos.z) * 0.7 + uTime * 1.2) * 0.018;
                        relPos.y += wave1 + wave2;
                    }
                    gl_Position = uProjView * vec4(relPos, 1.0);
                    vTexCoord = aTexCoord;
                    vWorldPos = relPos;
                    vSkyLight = aSkyLight;
                    vBlockLight = aBlockLight;
                    vDist = length(relPos);
                    vColor = aColor;
                }
                """;

        String fs = """
                #version 330 core
                in vec2 vTexCoord;
                in vec3 vWorldPos;
                in float vSkyLight;
                in float vBlockLight;
                in float vDist;
                in vec3 vColor;
                in float vIsWater;

                uniform sampler2D uTexture;
                uniform float uSunIntensity;
                uniform vec3 uSkyColor;
                uniform vec3 uTintColor;
                uniform float uFogStart;
                uniform float uFogEnd;
                uniform float uTime;

                out vec4 FragColor;

                void main() {
                    vec2 uv = vTexCoord;
                    if (vIsWater > 0.5) {
                        // Dynamic water ripple UV displacement kept within tile 9
                        float rippleU = sin(vWorldPos.z * 2.2 + uTime * 2.2) * 0.003;
                        float rippleV = cos(vWorldPos.x * 2.2 + uTime * 2.2) * 0.003;
                        uv.x = clamp(uv.x + rippleU, 0.127, 0.248);
                        uv.y = clamp(uv.y + rippleV, 0.127, 0.248);
                    }

                    vec4 texColor = texture(uTexture, uv);

                    if (texColor.a < 0.05) {
                        discard;
                    }

                    if (vIsWater > 0.5) {
                        // Fluid azure ocean tint and sunlight specular glint
                        texColor.rgb = mix(texColor.rgb, vec3(0.12, 0.38, 0.75), 0.30);
                        float specular = pow(max(0.0, sin(vWorldPos.x * 2.5 + uTime * 2.4) * cos(vWorldPos.z * 2.5 + uTime * 1.9)), 6.0);
                        texColor.rgb += vec3(0.15 * specular * uSunIntensity);
                        texColor.a = 0.82;
                    }

                    float light = max(vSkyLight * uSunIntensity, vBlockLight);
                    vec3 litColor = texColor.rgb * vColor * light * uTintColor;

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
