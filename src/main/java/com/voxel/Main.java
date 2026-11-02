package com.voxel;

import com.voxel.block.BlockType;
import com.voxel.config.GameSettings;
import com.voxel.entity.Player;
import com.voxel.i18n.I18n;
import com.voxel.render.ShaderProgram;
import com.voxel.render.TextureAtlas;
import com.voxel.ui.*;
import com.voxel.util.Logger;
import com.voxel.world.Chunk;
import com.voxel.world.World;
import com.voxel.world.WorldSaveManager;
import org.joml.Matrix4f;
import org.joml.Vector3d;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;

import static org.lwjgl.glfw.Callbacks.glfwFreeCallbacks;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import static org.lwjgl.opengl.GL20.GL_SHADING_LANGUAGE_VERSION;
import static org.lwjgl.system.MemoryUtil.NULL;

public class Main {
    private long window;
    private int width = 1280;
    private int height = 720;

    private boolean cursorLocked = false;
    private boolean firstMouse = true;
    private double lastMouseX;
    private double lastMouseY;
    private double currentMouseX = 0;
    private double currentMouseY = 0;

    private World world;
    private Player player;
    private TextureAtlas textureAtlas;
    private ShaderProgram chunkShader;
    private HudRenderer hudRenderer;
    private ChatSystem chatSystem;

    // Menus and Tools
    private MainMenu mainMenu;
    private PauseMenu pauseMenu;
    private OptionsMenu optionsMenu;
    private InventoryMenu inventoryMenu;
    private NoiseVisualizer noiseVisualizer;
    private String currentSaveName = "world";

    private boolean isLmbDown = false;
    private boolean isRmbDown = false;
    private float mouseHoldTimer = 0.0f;

    private int currentFps = 60;

    public static void main(String[] args) {
        new Main().run();
    }

    public void run() {
        Logger.init();
        GameSettings.load();
        Logger.info("Engine", "Starting Java Voxel 3D engine...");
        initWindow();
        initGame();
        loop();
        cleanup();
    }

    private void initWindow() {
        GLFWErrorCallback.createPrint(System.err).set();
        if (!glfwInit()) {
            Logger.fatal("Engine", "Nie udalo sie zainicjalizowac GLFW!", null);
            throw new IllegalStateException("Nie udalo sie zainicjalizowac GLFW!");
        }

        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE);

        window = glfwCreateWindow(width, height, "Java Voxel 3D", NULL, NULL);
        if (window == NULL) {
            Logger.fatal("Engine", "Nie udalo sie utworzyc okna GLFW!", null);
            throw new RuntimeException("Nie udalo sie utworzyc okna GLFW!");
        }

        GLFWVidMode vidMode = glfwGetVideoMode(glfwGetPrimaryMonitor());
        if (vidMode != null) {
            glfwSetWindowPos(window, (vidMode.width() - width) / 2, (vidMode.height() - height) / 2);
        }

        glfwSetFramebufferSizeCallback(window, (win, w, h) -> {
            if (w > 0 && h > 0) {
                width = w;
                height = h;
                glViewport(0, 0, width, height);
                recalculateGuiLayout();
            }
        });

        glfwSetCursorPosCallback(window, (win, xpos, ypos) -> {
            currentMouseX = xpos;
            currentMouseY = ypos;

            if (optionsMenu != null && optionsMenu.isOpen()) {
                float gScale = GameSettings.getEffectiveGuiScale(width, height);
                int vWidth = Math.max(1, Math.round(width / gScale));
                int vHeight = Math.max(1, Math.round(height / gScale));
                double vMouseX = currentMouseX / gScale;
                double vMouseY = currentMouseY / gScale;
                optionsMenu.handleMouseMove(vMouseX, vMouseY, vWidth, vHeight, world);
                return;
            }

            if (mainMenu != null && mainMenu.isActive()) return;

            if (noiseVisualizer != null && noiseVisualizer.isOpen()) {
                noiseVisualizer.handleMouseMove(xpos, ypos);
                return;
            }

            if ((pauseMenu != null && pauseMenu.isOpen())
                    || (inventoryMenu != null && inventoryMenu.isOpen())
                    || (chatSystem != null && chatSystem.isOpen())
                    || !cursorLocked) {
                return;
            }

            if (firstMouse) {
                lastMouseX = xpos;
                lastMouseY = ypos;
                firstMouse = false;
                return;
            }
            float dx = (float) (xpos - lastMouseX);
            float dy = (float) (ypos - lastMouseY);
            lastMouseX = xpos;
            lastMouseY = ypos;
            if (player != null) {
                player.handleMouseLook(dx, dy);
            }
        });

        glfwSetScrollCallback(window, (win, xoffset, yoffset) -> {
            if (optionsMenu != null && optionsMenu.isOpen()) {
                if (optionsMenu.handleScroll(yoffset)) return;
            }
            if (mainMenu != null && mainMenu.isActive()) {
                mainMenu.handleScroll(yoffset);
                return;
            }
            if (inventoryMenu != null && inventoryMenu.isOpen()) {
                if (inventoryMenu.handleScroll(yoffset)) return;
            }
            if (noiseVisualizer != null && noiseVisualizer.isOpen()) {
                noiseVisualizer.handleScroll(yoffset);
                return;
            }
            if ((pauseMenu != null && pauseMenu.isOpen())
                    || (chatSystem != null && chatSystem.isOpen())) {
                return;
            }
            if (player != null && yoffset != 0) {
                int step = yoffset > 0 ? -1 : 1;
                player.selectedSlot = Math.floorMod(player.selectedSlot + step, player.hotbar.length);
            }
        });

        glfwSetMouseButtonCallback(window, (win, button, action, mods) -> {
            float gScale = GameSettings.getEffectiveGuiScale(width, height);
            int vWidth = Math.max(1, Math.round(width / gScale));
            int vHeight = Math.max(1, Math.round(height / gScale));
            double vMouseX = currentMouseX / gScale;
            double vMouseY = currentMouseY / gScale;

            if (optionsMenu != null && optionsMenu.isOpen()) {
                if (action == GLFW_RELEASE && button == GLFW_MOUSE_BUTTON_LEFT) {
                    optionsMenu.handleMouseRelease();
                }
                if (action == GLFW_PRESS) {
                    optionsMenu.handleMouseClick(vMouseX, vMouseY, button, vWidth, vHeight, world, player);
                    if (!optionsMenu.isOpen()) {
                        if (optionsMenu.getReturnSource() == OptionsMenu.Source.MAIN_MENU) {
                            mainMenu.setActive(true);
                        } else {
                            pauseMenu.setOpen(true);
                        }
                    }
                }
                return;
            }

            if (mainMenu != null && mainMenu.isActive()) {
                if (action == GLFW_PRESS) {
                    MainMenu.Result res = mainMenu.handleMouseClick(vMouseX, vMouseY, button, vWidth, vHeight);
                    handleMainMenuResult(res);
                }
                return;
            }

            if (pauseMenu != null && pauseMenu.isOpen()) {
                if (action == GLFW_PRESS) {
                    PauseMenu.Action act = pauseMenu.handleMouseClick(vMouseX, vMouseY, button, vWidth, vHeight);
                    handlePauseMenuAction(act);
                }
                return;
            }

            if (inventoryMenu != null && inventoryMenu.isOpen()) {
                if (action == GLFW_PRESS) {
                    inventoryMenu.handleMouseClick(vMouseX, vMouseY, button, player);
                }
                return;
            }

            if (noiseVisualizer != null && noiseVisualizer.isOpen()) {
                noiseVisualizer.handleMouseButton(button, action, vMouseX, vMouseY, player, world, chatSystem);
                if (!noiseVisualizer.isOpen()) {
                    setCursorLocked(true);
                }
                return;
            }

            if (chatSystem != null && chatSystem.isOpen()) return;

            if (!cursorLocked) {
                if (action == GLFW_PRESS) {
                    setCursorLocked(true);
                }
                return;
            }

            if (world == null || player == null) return;

            if (button == GLFW_MOUSE_BUTTON_LEFT) {
                if (action == GLFW_PRESS) {
                    isLmbDown = true;
                    mouseHoldTimer = GameSettings.mouseHoldDelay;
                    handleBlockBreak();
                } else if (action == GLFW_RELEASE) {
                    isLmbDown = false;
                }
            } else if (button == GLFW_MOUSE_BUTTON_RIGHT) {
                if (action == GLFW_PRESS) {
                    isRmbDown = true;
                    mouseHoldTimer = GameSettings.mouseHoldDelay;
                    handleBlockPlace();
                } else if (action == GLFW_RELEASE) {
                    isRmbDown = false;
                }
            } else if (button == GLFW_MOUSE_BUTTON_MIDDLE && action == GLFW_PRESS) {
                World.RaycastHit hit = world.raycast(player.getEyePosition(), player.getLookDirection(), 6.5f);
                if (hit != null) {
                    BlockType picked = BlockType.fromId(hit.blockId());
                    player.setHotbarSlot(player.selectedSlot, picked);
                }
            }
        });

        glfwSetCharCallback(window, (win, codepoint) -> {
            if (mainMenu != null && mainMenu.isActive()) {
                mainMenu.onCharInput((char) codepoint);
                return;
            }
            if (chatSystem != null && chatSystem.isOpen()) {
                chatSystem.onCharInput((char) codepoint);
            }
        });

        glfwSetKeyCallback(window, (win, key, scancode, action, mods) -> {
            if (optionsMenu != null && optionsMenu.isOpen()) {
                if (action == GLFW_PRESS) {
                    optionsMenu.handleKey(key, world, player);
                    if (!optionsMenu.isOpen()) {
                        if (optionsMenu.getReturnSource() == OptionsMenu.Source.MAIN_MENU) {
                            mainMenu.setActive(true);
                        } else {
                            pauseMenu.setOpen(true);
                        }
                    }
                }
                return;
            }

            if (mainMenu != null && mainMenu.isActive()) {
                if (action == GLFW_PRESS || action == GLFW_REPEAT) {
                    MainMenu.Result res = mainMenu.handleKey(key);
                    handleMainMenuResult(res);
                }
                return;
            }

            if (chatSystem != null && chatSystem.isOpen()) {
                if (action == GLFW_PRESS || action == GLFW_REPEAT) {
                    if (key == GLFW_KEY_BACKSPACE) chatSystem.onBackspace();
                    else if (key == GLFW_KEY_DELETE) chatSystem.onDelete();
                    else if (key == GLFW_KEY_LEFT) chatSystem.moveCursor(-1);
                    else if (key == GLFW_KEY_RIGHT) chatSystem.moveCursor(1);
                    else if (key == GLFW_KEY_HOME) chatSystem.moveCursorToStart();
                    else if (key == GLFW_KEY_END) chatSystem.moveCursorToEnd();
                    else if (key == GLFW_KEY_UP) {
                        if (chatSystem.hasSuggestions()) chatSystem.navigateSuggestions(-1);
                        else chatSystem.navigateHistory(-1);
                    }
                    else if (key == GLFW_KEY_DOWN) {
                        if (chatSystem.hasSuggestions()) chatSystem.navigateSuggestions(1);
                        else chatSystem.navigateHistory(1);
                    }
                }
                if (action == GLFW_PRESS) {
                    if (key == GLFW_KEY_TAB) {
                        chatSystem.onTabComplete();
                    } else if (key == GLFW_KEY_ENTER || key == GLFW_KEY_KP_ENTER) {
                        chatSystem.submit(world, player, hudRenderer);
                        setCursorLocked(true);
                    } else if (key == GLFW_KEY_ESCAPE) {
                        chatSystem.closeChat();
                        setCursorLocked(true);
                    }
                }
                return;
            }

            if (noiseVisualizer != null && noiseVisualizer.isOpen()) {
                if (action == GLFW_PRESS) {
                    noiseVisualizer.handleKey(key, player);
                    if (!noiseVisualizer.isOpen()) {
                        setCursorLocked(true);
                    }
                }
                return;
            }

            if (inventoryMenu != null && inventoryMenu.isOpen()) {
                if (action == GLFW_PRESS) {
                    inventoryMenu.handleKey(key, currentMouseX, currentMouseY, player);
                    if (!inventoryMenu.isOpen()) {
                        setCursorLocked(true);
                    }
                }
                return;
            }

            if (pauseMenu != null && pauseMenu.isOpen()) {
                if (action == GLFW_PRESS && key == GLFW_KEY_ESCAPE) {
                    pauseMenu.setOpen(false);
                    setCursorLocked(true);
                }
                return;
            }

            if (action != GLFW_PRESS) return;

            // In-game key shortcuts
            if (key == GameSettings.keyPause || key == GLFW_KEY_ESCAPE) {
                pauseMenu.setOpen(true);
                setCursorLocked(false);
            } else if (key == GameSettings.keyInventory || key == GLFW_KEY_E) {
                inventoryMenu.setOpen(true);
                setCursorLocked(false);
            } else if (key == GameSettings.keyNoiseMap || key == GLFW_KEY_M || key == GLFW_KEY_F4) {
                noiseVisualizer.setOpen(true);
                noiseVisualizer.centerOnPlayer(player);
                setCursorLocked(false);
            } else if (key == GameSettings.keyDebug || key == GLFW_KEY_F3) {
                hudRenderer.showDebug = !hudRenderer.showDebug;
            } else if (key == GameSettings.keyToggleHud || key == GLFW_KEY_F1) {
                hudRenderer.toggleHud();
            } else if (key == GameSettings.keyChat || key == GLFW_KEY_T) {
                chatSystem.openChat("", true);
                setCursorLocked(false);
            } else if (key == GLFW_KEY_ENTER || key == GLFW_KEY_KP_ENTER) {
                chatSystem.openChat("", false);
                setCursorLocked(false);
            } else if (key == GameSettings.keyCommand || key == GLFW_KEY_SLASH) {
                chatSystem.openChat("/", true);
                setCursorLocked(false);
            } else if (key >= GLFW_KEY_1 && key <= GLFW_KEY_9) {
                int slot = key - GLFW_KEY_1;
                if (slot < player.hotbar.length) {
                    player.selectedSlot = slot;
                }
            }
        });

        glfwMakeContextCurrent(window);
        glfwSwapInterval(GameSettings.vsync ? 1 : 0);
        glfwShowWindow(window);

        GL.createCapabilities();
        Logger.info("OpenGL", "OpenGL graphic context initialized successfully.");
        Logger.info("OpenGL", "Karta graficzna: %s", glGetString(GL_RENDERER));
        Logger.info("OpenGL", "Producent: %s | Wersja: %s", glGetString(GL_VENDOR), glGetString(GL_VERSION));
        Logger.info("OpenGL", "Wersja GLSL: %s", glGetString(GL_SHADING_LANGUAGE_VERSION));

        glEnable(GL_DEPTH_TEST);
        glEnable(GL_CULL_FACE);
        glCullFace(GL_BACK);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
    }

    private void handlePauseMenuAction(PauseMenu.Action act) {
        if (act == null || act == PauseMenu.Action.NONE) return;
        switch (act) {
            case RESUME -> {
                pauseMenu.setOpen(false);
                setCursorLocked(true);
            }
            case OPEN_INVENTORY -> {
                pauseMenu.setOpen(false);
                inventoryMenu.setOpen(true);
            }
            case OPEN_NOISE_MAP -> {
                pauseMenu.setOpen(false);
                noiseVisualizer.setOpen(true);
                noiseVisualizer.centerOnPlayer(player);
            }
            case OPEN_OPTIONS -> {
                pauseMenu.setOpen(false);
                optionsMenu.setOpen(true, OptionsMenu.Source.PAUSE_MENU);
            }
            case SAVE -> {
                if (world.saveWorld(player, currentSaveName)) {
                    pauseMenu.setStatusMessage(I18n.t("pause.saved", currentSaveName));
                } else {
                    pauseMenu.setStatusMessage(I18n.t("pause.save_error"));
                }
            }
            case RESET_WORLD -> {
                world.resetWorld(player, world.getSeed());
                pauseMenu.setStatusMessage(I18n.t("pause.reset_done"));
            }
            case MAIN_MENU -> {
                world.saveWorld(player, currentSaveName);
                pauseMenu.setOpen(false);
                mainMenu.setActive(true);
                mainMenu.refreshSaves();
                setCursorLocked(false);
            }
            case QUIT -> {
                world.saveWorld(player, currentSaveName);
                glfwSetWindowShouldClose(window, true);
            }
        }
    }

    private void handleMainMenuResult(MainMenu.Result res) {
        if (res == null || res == MainMenu.Result.NONE) return;
        if (res == MainMenu.Result.QUIT) {
            glfwSetWindowShouldClose(window, true);
            return;
        }
        if (res == MainMenu.Result.OPEN_OPTIONS) {
            mainMenu.setActive(false);
            optionsMenu.setOpen(true, OptionsMenu.Source.MAIN_MENU);
            return;
        }
        if (res == MainMenu.Result.START_GAME) {
            MainMenu.Screen screen = mainMenu.getCurrentScreen();
            if (screen == MainMenu.Screen.NEW_WORLD) {
                String newName = mainMenu.getNewWorldName();
                long newSeed = mainMenu.getNewWorldSeed();
                startWorld(newName, newSeed, true);
            } else if (screen == MainMenu.Screen.SAVES) {
                WorldSaveManager.SaveInfo selected = mainMenu.getSelectedSave();
                if (selected != null) {
                    startWorld(selected.name(), selected.seed(), false);
                } else {
                    startWorld("world", 20261031L, false);
                }
            } else {
                WorldSaveManager.SaveInfo selected = mainMenu.getSelectedSave();
                if (selected != null) {
                    startWorld(selected.name(), selected.seed(), false);
                } else {
                    startWorld("world", 20261031L, false);
                }
            }
            mainMenu.setActive(false);
            setCursorLocked(true);
        }
    }

    private void startWorld(String saveName, long seed, boolean forceReset) {
        this.currentSaveName = saveName;
        if (world != null) {
            world.cleanup();
        }
        chatSystem.clear();
        world = new World(seed);
        world.setRenderDistance(GameSettings.renderDistance);
        if (!forceReset && world.hasSaveFile(saveName) && world.loadWorld(player, saveName)) {
            Logger.info("World", "Loaded existing save: %s [Seed: %d]", saveName, seed);
            chatSystem.addMessage(I18n.t("chat.save_loaded", saveName), ChatSystem.COLOR_SUCCESS);
        } else {
            world.resetWorld(player, seed);
            world.saveWorld(player, saveName);
            Logger.info("World", "Created new save: %s [Seed: %d]", saveName, seed);
            chatSystem.addMessage(I18n.t("chat.world_created", saveName, seed), ChatSystem.COLOR_SUCCESS);
        }
    }

    private void handleBlockBreak() {
        if (world == null || player == null) return;
        World.RaycastHit hit = world.raycast(player.getEyePosition(), player.getLookDirection(), 6.5f);
        if (hit != null) {
            world.setBlock(hit.x(), hit.y(), hit.z(), BlockType.AIR.id, true);
        }
    }

    private void handleBlockPlace() {
        if (world == null || player == null) return;
        World.RaycastHit hit = world.raycast(player.getEyePosition(), player.getLookDirection(), 6.5f);
        if (hit == null) return;

        long px = hit.prevX();
        long py = hit.prevY();
        long pz = hit.prevZ();
        BlockType toPlace = player.getSelectedBlock();
        if (toPlace == BlockType.AIR) {
            world.setBlock(hit.x(), hit.y(), hit.z(), BlockType.AIR.id, true);
        } else {
            BlockType hitBlock = BlockType.fromId(hit.blockId());
            if (toPlace.crossModel) {
                long targetPlaceX = hitBlock.crossModel ? hit.x() : px;
                long targetPlaceY = hitBlock.crossModel ? hit.y() : py;
                long targetPlaceZ = hitBlock.crossModel ? hit.z() : pz;
                byte groundBlock = world.getBlock(targetPlaceX, targetPlaceY - 1, targetPlaceZ);
                if (groundBlock == BlockType.STONE.id || groundBlock == BlockType.COBBLESTONE.id
                        || groundBlock == BlockType.SAND.id || groundBlock == BlockType.SANDSTONE.id
                        || groundBlock == BlockType.TERRACOTTA.id || groundBlock == BlockType.RED_SAND.id
                        || groundBlock == BlockType.AIR.id || groundBlock == BlockType.WATER.id
                        || groundBlock == BlockType.ICE.id) {
                    return;
                }
            }
            if (hitBlock.crossModel && (!toPlace.solid || !player.intersectsBlock(hit.x(), hit.y(), hit.z()))) {
                world.setBlock(hit.x(), hit.y(), hit.z(), toPlace.id, true);
            } else if (!toPlace.solid || !player.intersectsBlock(px, py, pz)) {
                world.setBlock(px, py, pz, toPlace.id, true);
            }
        }
    }

    private void setCursorLocked(boolean locked) {
        cursorLocked = locked;
        if (!locked) {
            isLmbDown = false;
            isRmbDown = false;
        }
        firstMouse = true;
        glfwSetInputMode(window, GLFW_CURSOR, locked ? GLFW_CURSOR_DISABLED : GLFW_CURSOR_NORMAL);
    }

    private void initGame() {
        Logger.info("Engine", "Initializing game systems (Atlas, Shaders, HUD, Menus)...");
        textureAtlas = new TextureAtlas();
        chunkShader = ShaderProgram.createChunkShader();
        hudRenderer = new HudRenderer();
        chatSystem = new ChatSystem();

        player = new Player();
        if (GameSettings.flyByDefault) {
            player.flying = true;
        }
        mainMenu = new MainMenu();
        pauseMenu = new PauseMenu();
        optionsMenu = new OptionsMenu();
        inventoryMenu = new InventoryMenu();
        noiseVisualizer = new NoiseVisualizer();

        mainMenu.refreshSaves();

        // Launch in Title Screen Menu (do not preload save into chat beforehand)
        mainMenu.setActive(true);
        setCursorLocked(false);
    }

    private void loop() {
        double lastTime = glfwGetTime();
        double fpsTimer = 0.0;
        int frames = 0;

        while (!glfwWindowShouldClose(window)) {
            double now = glfwGetTime();
            float dt = (float) (now - lastTime);
            lastTime = now;

            frames++;
            fpsTimer += dt;
            if (fpsTimer >= 0.25) {
                currentFps = (int) Math.round(frames / fpsTimer);
                frames = 0;
                fpsTimer = 0.0;
            }

            boolean isWorldPaused = pauseMenu.isOpen()
                    || (optionsMenu.isOpen() && optionsMenu.getReturnSource() == OptionsMenu.Source.PAUSE_MENU)
                    || (chatSystem != null && chatSystem.isOpen());

            // Separate game loop into Client Tick (input, menus, UI, local player) and World Tick (simulation)
            clientTick(dt);
            worldTick(isWorldPaused ? 0.0f : dt);

            render();

            glfwSwapBuffers(window);
            glfwPollEvents();

            if (GameSettings.maxFps > 0) {
                double targetFrame = 1.0 / GameSettings.maxFps;
                while (glfwGetTime() - now < targetFrame) {
                    Thread.yield();
                }
            }
        }

        // Auto-save on exit
        if (world != null && player != null) {
            world.saveWorld(player, currentSaveName);
        }
    }

    /**
     * Client tick: handles local player input, camera, UI screens, chat, and HUD.
     * When chat is open, chat consumes input and player movement is stopped,
     * but the world continues ticking independently in worldTick().
     */
    private void clientTick(float dt) {
        if (mainMenu.isActive()) {
            mainMenu.update(dt);
            return;
        }
        if (optionsMenu.isOpen() && optionsMenu.getReturnSource() == OptionsMenu.Source.MAIN_MENU) {
            optionsMenu.update(dt);
            return;
        }

        if (pauseMenu.isOpen()) {
            pauseMenu.update(dt);
        } else if (optionsMenu.isOpen()) {
            optionsMenu.update(dt);
        } else if (chatSystem.isOpen()) {
            // Player movement paused while typing in chat, mouse interactions suppressed
            isLmbDown = false;
            isRmbDown = false;
        } else if (cursorLocked && !inventoryMenu.isOpen() && !noiseVisualizer.isOpen()) {
            player.update(window, world, dt);
            if (isLmbDown || isRmbDown) {
                mouseHoldTimer -= dt;
                if (mouseHoldTimer <= 0.0f) {
                    mouseHoldTimer = GameSettings.mouseHoldDelay;
                    if (isLmbDown) {
                        handleBlockBreak();
                    } else {
                        handleBlockPlace();
                    }
                }
            }
        } else {
            isLmbDown = false;
            isRmbDown = false;
        }

        chatSystem.update(dt);
        hudRenderer.update(dt);
    }

    /**
     * World tick: updates world simulation, time of day, block physics, and chunk streaming.
     * Unpaused when chat is open (runs at full speed), paused only when in pause menu.
     */
    private void worldTick(float dt) {
        if (world != null && player != null) {
            world.update(player.position, dt);
        }
    }

    private void recalculateGuiLayout() {
        float gScale = GameSettings.getEffectiveGuiScale(width, height);
        int vWidth = Math.max(1, Math.round(width / gScale));
        int vHeight = Math.max(1, Math.round(height / gScale));

        if (mainMenu != null) mainMenu.onResize(vWidth, vHeight);
        if (optionsMenu != null) optionsMenu.onResize(vWidth, vHeight);
        if (pauseMenu != null) pauseMenu.onResize(vWidth, vHeight);
        if (inventoryMenu != null) inventoryMenu.onResize(vWidth, vHeight);
        if (chatSystem != null) chatSystem.onResize(vWidth, vHeight);
        if (noiseVisualizer != null) noiseVisualizer.onResize(vWidth, vHeight);
    }

    private void render() {
        float gScale = GameSettings.getEffectiveGuiScale(width, height);
        int vWidth = Math.max(1, Math.round(width / gScale));
        int vHeight = Math.max(1, Math.round(height / gScale));
        double vMouseX = currentMouseX / gScale;
        double vMouseY = currentMouseY / gScale;

        if (mainMenu.isActive()) {
            glClearColor(0.05f, 0.07f, 0.11f, 1.0f);
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
            glDisable(GL_DEPTH_TEST);
            glDisable(GL_CULL_FACE);
            glEnable(GL_BLEND);
            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
            mainMenu.render(hudRenderer, vWidth, vHeight, vMouseX, vMouseY);
            glEnable(GL_CULL_FACE);
            glEnable(GL_DEPTH_TEST);
            return;
        }

        if (optionsMenu.isOpen() && optionsMenu.getReturnSource() == OptionsMenu.Source.MAIN_MENU) {
            glClearColor(0.05f, 0.07f, 0.11f, 1.0f);
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
            optionsMenu.render(hudRenderer, vWidth, vHeight, vMouseX, vMouseY, world, player);
            return;
        }

        Vector3d eyePos = player.getEyePosition();
        boolean underwater = player.isHeadUnderwater(world);
        Vector3f skyColor = world.getSkyColor();
        if (underwater) {
            skyColor = new Vector3f(0.06f, 0.22f, 0.48f);
        } else {
            int surfY = world.getSurfaceHeight((long) Math.floor(eyePos.x), (long) Math.floor(eyePos.z));
            double depthBelowSurface = surfY - eyePos.y;
            if (depthBelowSurface > 4.0) {
                float depthFactor = (float) Math.max(0.0, Math.min(1.0, (depthBelowSurface - 4.0) / 10.0));
                skyColor = new Vector3f(skyColor).lerp(new Vector3f(0.06f, 0.06f, 0.08f), depthFactor);
            }
        }

        glClearColor(skyColor.x, skyColor.y, skyColor.z, 1.0f);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        Matrix4f projView = player.getProjectionViewMatrix(width, height);

        chunkShader.bind();
        chunkShader.setUniform("uProjView", projView);
        chunkShader.setUniform("uTexture", 0);
        chunkShader.setUniform("uSunIntensity", world.getSunIntensity() * GameSettings.brightness);
        chunkShader.setUniform("uSkyColor", skyColor);
        chunkShader.setUniform("uTintColor", underwater ? new Vector3f(0.55f, 0.78f, 1.0f) : new Vector3f(1.0f, 1.0f, 1.0f));
        float maxDist = World.renderDistance * Chunk.SIZE_X;
        float fogStart = underwater ? 8.0f : (GameSettings.fogMode == 0 ? maxDist * 3.0f : (GameSettings.fogMode == 1 ? maxDist * 0.40f : (GameSettings.fogMode == 3 ? maxDist * 0.85f : maxDist * 0.62f)));
        float fogEnd = underwater ? 28.0f : (GameSettings.fogMode == 0 ? maxDist * 4.0f : maxDist * 0.96f);
        chunkShader.setUniform("uFogStart", fogStart);
        chunkShader.setUniform("uFogEnd", fogEnd);
        chunkShader.setUniform("uTime", (float) glfwGetTime());

        glActiveTexture(GL_TEXTURE0);
        textureAtlas.bind();

        world.renderChunks(player.position, eyePos, projView, chunkShader);
        chunkShader.unbind();

        // Render targeted block wireframe when playing and HUD is visible (F1)
        World.RaycastHit hit = null;
        if (cursorLocked && !pauseMenu.isOpen() && !inventoryMenu.isOpen() && !noiseVisualizer.isOpen() && !optionsMenu.isOpen()) {
            hit = world.raycast(eyePos, player.getLookDirection(), 6.5f);
            if (hudRenderer.showHud) {
                hudRenderer.renderBlockSelection(projView, eyePos, hit);
            }
        }

        // Render active UI screen / overlay
        glDisable(GL_DEPTH_TEST);
        glDisable(GL_CULL_FACE);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        if (optionsMenu.isOpen()) {
            optionsMenu.render(hudRenderer, vWidth, vHeight, vMouseX, vMouseY, world, player);
        } else if (noiseVisualizer.isOpen()) {
            noiseVisualizer.render(hudRenderer, vWidth, vHeight, vMouseX, vMouseY, player, world);
        } else if (inventoryMenu.isOpen()) {
            inventoryMenu.render(hudRenderer, vWidth, vHeight, vMouseX, vMouseY, player, textureAtlas);
        } else if (pauseMenu.isOpen()) {
            pauseMenu.render(hudRenderer, vWidth, vHeight, vMouseX, vMouseY, world, player);
        } else {
            if (noiseVisualizer != null) {
                hudRenderer.setWaypoint(noiseVisualizer.hasWaypoint(), noiseVisualizer.getWaypointWx(), noiseVisualizer.getWaypointWz());
            }
            hudRenderer.renderHud(vWidth, vHeight, currentFps, player, world, hit, textureAtlas, chatSystem);
        }
        glEnable(GL_CULL_FACE);
        glEnable(GL_DEPTH_TEST);
    }

    public static String formatTimeOfDay(float t) {
        int totalMinutes = (int) (t * 24 * 60) % (24 * 60);
        int hour = totalMinutes / 60;
        int min = totalMinutes % 60;
        String phase;
        if (hour >= 6 && hour < 10) phase = I18n.t("time.morning");
        else if (hour >= 10 && hour < 18) phase = I18n.t("time.day");
        else if (hour >= 18 && hour < 21) phase = I18n.t("time.sunset");
        else phase = I18n.t("time.night");
        return String.format("%02d:%02d (%s)", hour, min, phase);
    }

    private void cleanup() {
        Logger.info("Engine", "Shutting down engine and releasing resources...");
        GameSettings.save();
        if (world != null && player != null) {
            world.saveWorld(player, currentSaveName);
        }
        if (world != null) world.cleanup();
        if (hudRenderer != null) hudRenderer.cleanup();
        if (chunkShader != null) chunkShader.cleanup();
        if (textureAtlas != null) textureAtlas.cleanup();
        if (noiseVisualizer != null) noiseVisualizer.cleanup();

        glfwFreeCallbacks(window);
        glfwDestroyWindow(window);
        glfwTerminate();
        GLFWErrorCallback cb = glfwSetErrorCallback(null);
        if (cb != null) cb.free();
        Logger.info("Engine", "Pomyslnie zamknieto gre. Do widzenia!");
        Logger.close();
    }
}
