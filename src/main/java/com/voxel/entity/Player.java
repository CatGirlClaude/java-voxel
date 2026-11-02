package com.voxel.entity;

import com.voxel.block.BlockType;
import com.voxel.config.GameSettings;
import com.voxel.world.World;
import org.joml.Matrix4f;
import org.joml.Vector3d;
import org.joml.Vector3f;

import static org.lwjgl.glfw.GLFW.*;

public class Player extends LivingEntity {
    public static final double HALF_WIDTH = 0.30;
    public static final double HEIGHT = 1.80;
    public static final double SNEAK_HEIGHT = 1.50;

    // Standard Minecraft eye heights (Bedrock: 1.54, Java Classic: 1.62, Sneaking: 1.27)
    public static final double EYE_HEIGHT = 1.54;
    public static final double SNEAK_EYE_HEIGHT = 1.27;

    public boolean flying = false;
    public boolean noclip = false;
    public boolean sneaking = false;

    private boolean wasInWater = false;
    private float stepTimer = 0.0f;

    public int selectedSlot = 0;
    public final BlockType[] hotbar = java.util.Arrays.copyOf(BlockType.HOTBAR_BLOCKS, 9);

    public Player() {
        this(null);
    }

    public Player(World world) {
        super(EntityTypes.PLAYER, world);
        this.position.set(0.5, 45.0, 0.5);
        this.yaw = -90.0f;
        this.pitch = -10.0f;
        this.currentEyeHeight = EYE_HEIGHT;
    }

    @Override
    public void tick(float dt) {
        // Living entity tick
    }

    public void handleMouseLook(float dx, float dy) {
        float sensitivity = GameSettings.mouseSensitivity;
        yaw += dx * sensitivity;
        if (GameSettings.invertMouseY) {
            pitch += dy * sensitivity;
        } else {
            pitch -= dy * sensitivity;
        }
        if (pitch > 89.0f) pitch = 89.0f;
        if (pitch < -89.0f) pitch = -89.0f;
    }

    @Override
    public Vector3d getEyePosition() {
        return new Vector3d(position.x, position.y + currentEyeHeight, position.z);
    }

    @Override
    public Vector3f getLookDirection() {
        float radYaw = (float) Math.toRadians(yaw);
        float radPitch = (float) Math.toRadians(pitch);
        float cosPitch = (float) Math.cos(radPitch);
        return new Vector3f(
                (float) (Math.cos(radYaw) * cosPitch),
                (float) Math.sin(radPitch),
                (float) (Math.sin(radYaw) * cosPitch)
        ).normalize();
    }

    /**
     * Camera-relative projection-view matrix (camera at origin 0,0,0).
     * Eliminates float precision loss / vertex jitter at large world coordinates.
     */
    public Matrix4f getProjectionViewMatrix(int width, int height) {
        float aspect = (float) Math.max(1, width) / Math.max(1, height);
        Matrix4f proj = new Matrix4f().perspective((float) Math.toRadians(GameSettings.fov), aspect, 0.08f, 350.0f);
        Vector3f dir = getLookDirection();
        Matrix4f view = new Matrix4f().lookAt(0.0f, 0.0f, 0.0f, dir.x, dir.y, dir.z, 0.0f, 1.0f, 0.0f);
        return proj.mul(view);
    }

    public boolean isHeadUnderwater(World world) {
        Vector3d eye = getEyePosition();
        byte b = world.getBlock((long) Math.floor(eye.x), (long) Math.floor(eye.y), (long) Math.floor(eye.z));
        return BlockType.isWater(b) || BlockType.isUnderwaterPlant(b);
    }

    public boolean isInWater(World world) {
        long bx = (long) Math.floor(position.x);
        long bz = (long) Math.floor(position.z);
        byte b1 = world.getBlock(bx, (long) Math.floor(position.y + 0.2), bz);
        byte b2 = world.getBlock(bx, (long) Math.floor(position.y + 1.0), bz);
        return BlockType.isWater(b1) || BlockType.isWater(b2) || BlockType.isUnderwaterPlant(b1) || BlockType.isUnderwaterPlant(b2);
    }

    public void update(long window, World world, float dt) {
        dt = Math.min(dt, 0.05f);

        if (GameSettings.enableOverrides) {
            if (GameSettings.overrideFly == 1) {
                this.flying = true;
            } else if (GameSettings.overrideFly == 2) {
                this.flying = false;
            }
            if (GameSettings.overrideNoclip == 1) {
                this.noclip = true;
                this.flying = true;
            } else if (GameSettings.overrideNoclip == 2) {
                this.noclip = false;
            }
        }

        float radYaw = (float) Math.toRadians(yaw);
        Vector3f forward = new Vector3f((float) Math.cos(radYaw), 0, (float) Math.sin(radYaw)).normalize();
        Vector3f right = new Vector3f(-forward.z, 0, forward.x).normalize();

        Vector3f wishDir = new Vector3f();
        if (glfwGetKey(window, GameSettings.keyForward) == GLFW_PRESS) wishDir.add(forward);
        if (glfwGetKey(window, GameSettings.keyBack) == GLFW_PRESS) wishDir.sub(forward);
        if (glfwGetKey(window, GameSettings.keyRight) == GLFW_PRESS) wishDir.add(right);
        if (glfwGetKey(window, GameSettings.keyLeft) == GLFW_PRESS) wishDir.sub(right);
        if (wishDir.lengthSquared() > 0.0001f) {
            wishDir.normalize();
        }

        boolean sprint = glfwGetKey(window, GameSettings.keySprint) == GLFW_PRESS;
        boolean crouchPressed = glfwGetKey(window, GameSettings.keyCrouch) == GLFW_PRESS;

        if (noclip || flying) {
            this.sneaking = false;
            float flySpeed = sprint ? (noclip ? 32.0f : 22.0f) : (noclip ? 15.0f : 12.0f);
            velocity.x = wishDir.x * flySpeed;
            velocity.z = wishDir.z * flySpeed;
            velocity.y = 0.0f;
            if (glfwGetKey(window, GameSettings.keyJump) == GLFW_PRESS) {
                velocity.y += flySpeed * 0.85f;
            }
            if (crouchPressed) {
                velocity.y -= flySpeed * 0.85f;
            }
            if (noclip) {
                position.x += (double) velocity.x * dt;
                position.y += (double) velocity.y * dt;
                position.z += (double) velocity.z * dt;
                onGround = false;
            } else {
                moveWithCollisions(world, (double) velocity.x * dt, (double) velocity.y * dt, (double) velocity.z * dt);
            }
        } else {
            boolean inWater = isInWater(world);
            if (inWater && !wasInWater) {
                com.voxel.audio.SoundSystem.playSplashSound();
            }
            wasInWater = inWater;

            this.sneaking = crouchPressed && onGround && !inWater;
            float walkSpeed = (sprint ? 7.4f : (this.sneaking ? 1.75f : 4.8f)) * (inWater ? 0.65f : 1.0f);
            velocity.x = wishDir.x * walkSpeed;
            velocity.z = wishDir.z * walkSpeed;

            if (inWater) {
                velocity.y -= 9.0f * dt;
                velocity.y *= (float) Math.pow(0.85, dt * 60.0);
                if (glfwGetKey(window, GameSettings.keyJump) == GLFW_PRESS) {
                    velocity.y = 4.2f;
                }
            } else {
                velocity.y -= 26.0f * dt;
                if (velocity.y < -45.0f) velocity.y = -45.0f;
                if (onGround && glfwGetKey(window, GameSettings.keyJump) == GLFW_PRESS) {
                    velocity.y = 8.4f;
                    onGround = false;
                }
            }

            moveWithCollisions(world, (double) velocity.x * dt, (double) velocity.y * dt, (double) velocity.z * dt);

            // Footstep and wading sounds
            float hSpeed = (float) Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
            if (onGround && !inWater && hSpeed > 0.6f) {
                stepTimer += dt * (hSpeed / 4.8f);
                if (stepTimer >= 0.38f) {
                    stepTimer = 0.0f;
                    long gx = (long) Math.floor(position.x);
                    long gy = (long) Math.floor(position.y - 0.25);
                    long gz = (long) Math.floor(position.z);
                    byte groundBlock = (world != null) ? world.getBlock(gx, gy, gz) : BlockType.GRASS.id;
                    com.voxel.audio.SoundSystem.playStepSound(BlockType.fromId(groundBlock));
                }
            } else if (inWater && hSpeed > 0.6f) {
                stepTimer += dt;
                if (stepTimer >= 0.65f) {
                    stepTimer = 0.0f;
                    com.voxel.audio.SoundSystem.playSplashSound();
                }
            } else {
                stepTimer = Math.max(0.0f, stepTimer - dt * 0.5f);
            }
        }

        // Smooth camera eye height interpolation (standing <-> sneaking)
        double targetEye = (this.sneaking && !flying && !noclip) ? SNEAK_EYE_HEIGHT : (double) GameSettings.eyeHeight;
        currentEyeHeight += (targetEye - currentEyeHeight) * Math.min(1.0, (double) dt * 14.0);
    }

    private void moveWithCollisions(World world, double dx, double dy, double dz) {
        double currentHeight = (sneaking && onGround && !flying && !noclip) ? SNEAK_HEIGHT : HEIGHT;

        // Sneaking edge protection: prevent walking off ledges
        if (sneaking && onGround && !flying && !noclip) {
            if (dx != 0.0 && !hasGroundUnder(world, position.x + dx, position.y, position.z)) {
                dx = 0.0;
            }
            if (dz != 0.0 && !hasGroundUnder(world, position.x, position.y, position.z + dz)) {
                dz = 0.0;
            }
        }

        // Y-axis swept collision
        double originalDy = dy;
        if (dy < 0.0) {
            long bMinX = (long) Math.floor(position.x - HALF_WIDTH + 0.001);
            long bMaxX = (long) Math.floor(position.x + HALF_WIDTH - 0.001);
            long bMinZ = (long) Math.floor(position.z - HALF_WIDTH + 0.001);
            long bMaxZ = (long) Math.floor(position.z + HALF_WIDTH - 0.001);
            long bMinY = (long) Math.floor(position.y + dy);
            long bMaxY = (long) Math.floor(position.y);

            for (long y = bMinY; y <= bMaxY; y++) {
                for (long z = bMinZ; z <= bMaxZ; z++) {
                    for (long x = bMinX; x <= bMaxX; x++) {
                        if (BlockType.fromId(world.getBlock(x, y, z)).solid) {
                            double blockTop = (double) (y + 1L);
                            if (blockTop <= position.y + 0.001) {
                                double maxDy = blockTop - position.y;
                                if (maxDy > dy) {
                                    dy = maxDy;
                                }
                            }
                        }
                    }
                }
            }

            position.y += dy;
            if (dy > originalDy || Math.abs(dy) < 1e-9) {
                onGround = true;
                velocity.y = 0;
            } else {
                onGround = false;
            }
        } else if (dy > 0.0) {
            long bMinX = (long) Math.floor(position.x - HALF_WIDTH + 0.001);
            long bMaxX = (long) Math.floor(position.x + HALF_WIDTH - 0.001);
            long bMinZ = (long) Math.floor(position.z - HALF_WIDTH + 0.001);
            long bMaxZ = (long) Math.floor(position.z + HALF_WIDTH - 0.001);
            long bMinY = (long) Math.floor(position.y + currentHeight - 0.001);
            long bMaxY = (long) Math.floor(position.y + currentHeight + dy);

            for (long y = bMinY; y <= bMaxY; y++) {
                for (long z = bMinZ; z <= bMaxZ; z++) {
                    for (long x = bMinX; x <= bMaxX; x++) {
                        if (BlockType.fromId(world.getBlock(x, y, z)).solid) {
                            double blockBottom = (double) y;
                            if (blockBottom >= position.y + currentHeight - 0.001) {
                                double maxDy = blockBottom - (position.y + currentHeight);
                                if (maxDy < dy) {
                                    dy = Math.max(0.0, maxDy);
                                }
                            }
                        }
                    }
                }
            }

            position.y += dy;
            if (dy < originalDy) {
                velocity.y = 0;
            }
        }

        // X-axis swept collision
        double originalDx = dx;
        if (dx > 0.0) {
            long bMinY = (long) Math.floor(position.y + 0.001);
            long bMaxY = (long) Math.floor(position.y + currentHeight - 0.001);
            long bMinZ = (long) Math.floor(position.z - HALF_WIDTH + 0.001);
            long bMaxZ = (long) Math.floor(position.z + HALF_WIDTH - 0.001);
            long bMinX = (long) Math.floor(position.x + HALF_WIDTH - 0.001);
            long bMaxX = (long) Math.floor(position.x + HALF_WIDTH + dx);

            for (long y = bMinY; y <= bMaxY; y++) {
                for (long z = bMinZ; z <= bMaxZ; z++) {
                    for (long x = bMinX; x <= bMaxX; x++) {
                        if (BlockType.fromId(world.getBlock(x, y, z)).solid) {
                            double blockLeft = (double) x;
                            if (blockLeft >= position.x + HALF_WIDTH - 0.001) {
                                double maxDx = blockLeft - (position.x + HALF_WIDTH);
                                if (maxDx < dx) {
                                    dx = Math.max(0.0, maxDx);
                                }
                            }
                        }
                    }
                }
            }
            position.x += dx;
            if (dx < originalDx) {
                velocity.x = 0;
            }
        } else if (dx < 0.0) {
            long bMinY = (long) Math.floor(position.y + 0.001);
            long bMaxY = (long) Math.floor(position.y + currentHeight - 0.001);
            long bMinZ = (long) Math.floor(position.z - HALF_WIDTH + 0.001);
            long bMaxZ = (long) Math.floor(position.z + HALF_WIDTH - 0.001);
            long bMinX = (long) Math.floor(position.x - HALF_WIDTH + dx);
            long bMaxX = (long) Math.floor(position.x - HALF_WIDTH + 0.001);

            for (long y = bMinY; y <= bMaxY; y++) {
                for (long z = bMinZ; z <= bMaxZ; z++) {
                    for (long x = bMinX; x <= bMaxX; x++) {
                        if (BlockType.fromId(world.getBlock(x, y, z)).solid) {
                            double blockRight = (double) (x + 1L);
                            if (blockRight <= position.x - HALF_WIDTH + 0.001) {
                                double maxDx = blockRight - (position.x - HALF_WIDTH);
                                if (maxDx > dx) {
                                    dx = Math.min(0.0, maxDx);
                                }
                            }
                        }
                    }
                }
            }
            position.x += dx;
            if (dx > originalDx) {
                velocity.x = 0;
            }
        }

        // Z-axis swept collision
        double originalDz = dz;
        if (dz > 0.0) {
            long bMinY = (long) Math.floor(position.y + 0.001);
            long bMaxY = (long) Math.floor(position.y + currentHeight - 0.001);
            long bMinX = (long) Math.floor(position.x - HALF_WIDTH + 0.001);
            long bMaxX = (long) Math.floor(position.x + HALF_WIDTH - 0.001);
            long bMinZ = (long) Math.floor(position.z + HALF_WIDTH - 0.001);
            long bMaxZ = (long) Math.floor(position.z + HALF_WIDTH + dz);

            for (long y = bMinY; y <= bMaxY; y++) {
                for (long z = bMinZ; z <= bMaxZ; z++) {
                    for (long x = bMinX; x <= bMaxX; x++) {
                        if (BlockType.fromId(world.getBlock(x, y, z)).solid) {
                            double blockNear = (double) z;
                            if (blockNear >= position.z + HALF_WIDTH - 0.001) {
                                double maxDz = blockNear - (position.z + HALF_WIDTH);
                                if (maxDz < dz) {
                                    dz = Math.max(0.0, maxDz);
                                }
                            }
                        }
                    }
                }
            }
            position.z += dz;
            if (dz < originalDz) {
                velocity.z = 0;
            }
        } else if (dz < 0.0) {
            long bMinY = (long) Math.floor(position.y + 0.001);
            long bMaxY = (long) Math.floor(position.y + currentHeight - 0.001);
            long bMinX = (long) Math.floor(position.x - HALF_WIDTH + 0.001);
            long bMaxX = (long) Math.floor(position.x + HALF_WIDTH - 0.001);
            long bMinZ = (long) Math.floor(position.z - HALF_WIDTH + dz);
            long bMaxZ = (long) Math.floor(position.z - HALF_WIDTH + 0.001);

            for (long y = bMinY; y <= bMaxY; y++) {
                for (long z = bMinZ; z <= bMaxZ; z++) {
                    for (long x = bMinX; x <= bMaxX; x++) {
                        if (BlockType.fromId(world.getBlock(x, y, z)).solid) {
                            double blockFar = (double) (z + 1L);
                            if (blockFar <= position.z - HALF_WIDTH + 0.001) {
                                double maxDz = blockFar - (position.z - HALF_WIDTH);
                                if (maxDz > dz) {
                                    dz = Math.min(0.0, maxDz);
                                }
                            }
                        }
                    }
                }
            }
            position.z += dz;
            if (dz > originalDz) {
                velocity.z = 0;
            }
        }

        // Failsafe unstuck check: if penetrated into ground (e.g. chunk generation or sudden teleport)
        if (collidesWithWorld(world, position.x, position.y, position.z)) {
            position.y = Math.floor(position.y + 1.0);
            velocity.y = 0;
            onGround = true;
        }
    }

    private boolean hasGroundUnder(World world, double px, double py, double pz) {
        long minX = (long) Math.floor(px - HALF_WIDTH + 0.001);
        long maxX = (long) Math.floor(px + HALF_WIDTH - 0.001);
        long minZ = (long) Math.floor(pz - HALF_WIDTH + 0.001);
        long maxZ = (long) Math.floor(pz + HALF_WIDTH - 0.001);
        long checkY = (long) Math.floor(py - 0.1);
        for (long x = minX; x <= maxX; x++) {
            for (long z = minZ; z <= maxZ; z++) {
                if (BlockType.fromId(world.getBlock(x, checkY, z)).solid) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean collidesWithWorld(World world, double px, double py, double pz) {
        double currentHeight = (sneaking && onGround && !flying && !noclip) ? SNEAK_HEIGHT : HEIGHT;
        long minX = (long) Math.floor(px - HALF_WIDTH + 0.001);
        long maxX = (long) Math.floor(px + HALF_WIDTH - 0.001);
        long minY = (long) Math.floor(py + 0.001);
        long maxY = (long) Math.floor(py + currentHeight - 0.001);
        long minZ = (long) Math.floor(pz - HALF_WIDTH + 0.001);
        long maxZ = (long) Math.floor(pz + HALF_WIDTH - 0.001);

        for (long y = minY; y <= maxY; y++) {
            for (long z = minZ; z <= maxZ; z++) {
                for (long x = minX; x <= maxX; x++) {
                    BlockType bt = BlockType.fromId(world.getBlock(x, y, z));
                    if (bt.solid) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public boolean intersectsBlock(long bx, long by, long bz) {
        double currentHeight = (sneaking && onGround && !flying && !noclip) ? SNEAK_HEIGHT : HEIGHT;
        double pMinX = position.x - HALF_WIDTH;
        double pMaxX = position.x + HALF_WIDTH;
        double pMinY = position.y;
        double pMaxY = position.y + currentHeight;
        double pMinZ = position.z - HALF_WIDTH;
        double pMaxZ = position.z + HALF_WIDTH;

        return pMaxX > bx && pMinX < bx + 1.0
                && pMaxY > by && pMinY < by + 1.0
                && pMaxZ > bz && pMinZ < bz + 1.0;
    }

    public BlockType getSelectedBlock() {
        int idx = Math.floorMod(selectedSlot, hotbar.length);
        return hotbar[idx];
    }

    public void setHotbarSlot(int slot, BlockType block) {
        if (slot >= 0 && slot < hotbar.length && block != null) {
            hotbar[slot] = block;
        }
    }
}
