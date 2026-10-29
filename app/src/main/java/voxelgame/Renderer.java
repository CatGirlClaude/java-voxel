package voxelgame;

import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.FloatBuffer;
import java.util.stream.Collectors;

import static org.lwjgl.opengl.GL33.*;

public class Renderer {
    private int shaderProgram;
    private FloatBuffer buffer;

    public Renderer() {
        buffer = BufferUtils.createFloatBuffer(16);
        initShaders();
    }

    private String readShaderFile(String path) throws IOException {
        try (InputStream is = Renderer.class.getResourceAsStream(path);
             BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
            return reader.lines().collect(Collectors.joining("\n"));
        }
    }

    private void initShaders() {
        try {
            String vertexCode = readShaderFile("/shaders/vertex.glsl");
            String fragmentCode = readShaderFile("/shaders/fragment.glsl");

            int vertexShader = glCreateShader(GL_VERTEX_SHADER);
            glShaderSource(vertexShader, vertexCode);
            glCompileShader(vertexShader);
            checkCompileErrors(vertexShader, "VERTEX");

            int fragmentShader = glCreateShader(GL_FRAGMENT_SHADER);
            glShaderSource(fragmentShader, fragmentCode);
            glCompileShader(fragmentShader);
            checkCompileErrors(fragmentShader, "FRAGMENT");

            shaderProgram = glCreateProgram();
            glAttachShader(shaderProgram, vertexShader);
            glAttachShader(shaderProgram, fragmentShader);
            glLinkProgram(shaderProgram);
            checkCompileErrors(shaderProgram, "PROGRAM");

            glDeleteShader(vertexShader);
            glDeleteShader(fragmentShader);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to read shader files");
        }
    }

    private void checkCompileErrors(int shader, String type) {
        int success;
        if (!type.equals("PROGRAM")) {
            success = glGetShaderi(shader, GL_COMPILE_STATUS);
            if (success == GL_FALSE) {
                String infoLog = glGetShaderInfoLog(shader);
                System.out.println("ERROR::SHADER_COMPILATION_ERROR of type: " + type + "\n" + infoLog + "\n -- --------------------------------------------------- -- ");
            }
        } else {
            success = glGetProgrami(shader, GL_LINK_STATUS);
            if (success == GL_FALSE) {
                String infoLog = glGetProgramInfoLog(shader);
                System.out.println("ERROR::PROGRAM_LINKING_ERROR of type: " + type + "\n" + infoLog + "\n -- --------------------------------------------------- -- ");
            }
        }
    }

    public void render(Chunk chunk, Camera camera, int width, int height) {
        glUseProgram(shaderProgram);

        Matrix4f projection = new Matrix4f().perspective((float) Math.toRadians(camera.getZoom()), (float) width / height, 0.1f, 100.0f);
        Matrix4f view = camera.getViewMatrix();
        Matrix4f model = new Matrix4f();

        int projLoc = glGetUniformLocation(shaderProgram, "projection");
        glUniformMatrix4fv(projLoc, false, projection.get(buffer));

        int viewLoc = glGetUniformLocation(shaderProgram, "view");
        glUniformMatrix4fv(viewLoc, false, view.get(buffer));

        int modelLoc = glGetUniformLocation(shaderProgram, "model");
        glUniformMatrix4fv(modelLoc, false, model.get(buffer));

        glBindVertexArray(chunk.getVao());
        glDrawArrays(GL_TRIANGLES, 0, chunk.getVertexCount());
        glBindVertexArray(0);
    }
}
