package voxelgame;

import org.joml.Matrix4f;
import org.joml.Vector3f;

public class Camera {

    public enum CameraMovement {
        FORWARD,
        BACKWARD,
        LEFT,
        RIGHT,
        UP,
        DOWN
    }

    private Vector3f position;
    private Vector3f front;
    private Vector3f up;
    private Vector3f right;
    private Vector3f worldUp;

    private float yaw;
    private float pitch;

    private float movementSpeed;
    private float mouseSensitivity;
    private float zoom;

    public Camera() {
        this.position = new Vector3f(0.0f, 0.0f, 3.0f);
        this.worldUp = new Vector3f(0.0f, 1.0f, 0.0f);
        this.yaw = -90.0f;
        this.pitch = 0.0f;
        this.front = new Vector3f(0.0f, 0.0f, -1.0f);
        this.movementSpeed = 2.5f;
        this.mouseSensitivity = 0.1f;
        this.zoom = 45.0f;
        updateCameraVectors();
    }

    public Matrix4f getViewMatrix() {
        return new Matrix4f().lookAt(position, new Vector3f(position).add(front), up);
    }

    public void processKeyboard(CameraMovement direction, float deltaTime) {
        float velocity = movementSpeed * deltaTime;
        if (direction == CameraMovement.FORWARD) {
            position.add(new Vector3f(front).mul(velocity));
        }
        if (direction == CameraMovement.BACKWARD) {
            position.sub(new Vector3f(front).mul(velocity));
        }
        if (direction == CameraMovement.LEFT) {
            position.sub(new Vector3f(right).mul(velocity));
        }
        if (direction == CameraMovement.RIGHT) {
            position.add(new Vector3f(right).mul(velocity));
        }
        if (direction == CameraMovement.UP) {
            position.add(new Vector3f(up).mul(velocity));
        }
        if (direction == CameraMovement.DOWN) {
            position.sub(new Vector3f(up).mul(velocity));
        }
    }

    public void processMouseMovement(float xoffset, float yoffset) {
        xoffset *= mouseSensitivity;
        yoffset *= mouseSensitivity;

        yaw += xoffset;
        pitch += yoffset;

        if (pitch > 89.0f) {
            pitch = 89.0f;
        }
        if (pitch < -89.0f) {
            pitch = -89.0f;
        }

        updateCameraVectors();
    }

    private void updateCameraVectors() {
        Vector3f newFront = new Vector3f();
        newFront.x = (float) (Math.cos(Math.toRadians(yaw)) * Math.cos(Math.toRadians(pitch)));
        newFront.y = (float) Math.sin(Math.toRadians(pitch));
        newFront.z = (float) (Math.sin(Math.toRadians(yaw)) * Math.cos(Math.toRadians(pitch)));
        front = newFront.normalize();

        right = new Vector3f(front).cross(worldUp).normalize();
        up = new Vector3f(right).cross(front).normalize();
    }

    public float getZoom() {
        return zoom;
    }
}
