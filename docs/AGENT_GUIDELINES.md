# Agent Guidelines & Engineering Playbook (AGENT_GUIDELINES.md)

Welcome, Agent. This document is your primary onboarding manual and operational guide for the **Java Voxel 3D** engine. It provides the essential architectural context, technical invariants, and developer workflows so you can implement features and refactor code without having to scan the entire codebase every session.

---

## 1. Project Philosophy & Workflow

### The User-Agent Partnership
- **The User** does **not** touch or edit the `docs/` directory. The user maintains a high-level scratchpad for ideas and bug reports in [`developer todo.md`](file:///c:/Users/september/Desktop/ja/antigravity/java%20voxel/developer%20todo.md).
- **The Agent** is responsible for:
  1. Reading raw ideas from `developer todo.md`.
  2. Refining them with technical specifications into [`docs/TODO.md`](file:///c:/Users/september/Desktop/ja/antigravity/java%20voxel/docs/TODO.md).
  3. Implementing the solution. You are granted **broad architectural autonomy**—multi-file refactorings and deep modular restructuring are fully allowed and encouraged.
  4. Verifying that the project builds cleanly via `mvn compile`.
  5. Marking completed items as `[x]` in both `developer todo.md` and `docs/TODO.md`, keeping the documentation synchronized as the Single Source of Truth.
  6. **Mandatory Git Commit & Push:** After every successful `mvn compile` and verification, the agent MUST immediately commit all staged changes with a clear, descriptive message and push them to GitHub (`origin main`). Uncommitted changes must never be left behind.

---

## 2. Tech Stack & Execution Commands

| Component | Technology | Details |
| :--- | :--- | :--- |
| **Language** | Java 21 | Modern switch expressions, records, pattern matching |
| **Graphics & Windowing** | LWJGL 3.3.4 | GLFW + OpenGL 3.3 Core Profile |
| **Math** | JOML 1.10.5 | Vectors, matrices, SIMD Frustum Intersection |
| **Build Tool** | Apache Maven | Compiler target Java 21, `exec-maven-plugin` |
| **VCS & Remote** | Git / GitHub | Remote: `origin` (`main`) |

### Core CLI Commands
- **Compile project:**
  ```bash
  mvn compile
  ```
  *(Must always pass with exit code 0).*
- **Commit & Push to GitHub (Required after every successful compile):**
  ```bash
  git add .
  git commit -m "feat/fix: descriptive summary of implemented task"
  git push origin main
  ```
- **Run the game:**
  ```bash
  run.bat
  # or via Maven:
  mvn compile exec:java
  ```
- **Run developer verification tests (when needed):**
  ```bash
  # Using built test classpath:
  mvn test-compile
  mvn exec:java -Dexec.mainClass="com.voxel.DeveloperFeaturesTest" -Dexec.classpathScope=test
  ```

---

## 3. Critical Invariants & Rules (NEVER BREAK)

### A. The OpenGL Thread is Sacred
- **Never call OpenGL (`gl*`, VAO/VBO manipulation, texture creation) on background threads.**
- The engine uses an asynchronous worker pool (`ExecutorService workerPool`, 2–8 threads).
- Worker threads MUST execute exclusively on CPU data structures (`FloatList`, arrays, bitmath).
- The main render thread pulls completed meshes from `readyMeshes` and uploads them to the GPU under a strict frame time budget (2.5 ms, up to 64 meshes/frame).

### B. Zero-GC Allocation in Hot Paths
- Chunk generation (`TerrainGenerator`) and chunk meshing (`ChunkMesher`) run at throughputs of **2,000 – 5,600+ chunks per second**.
- **Do not allocate heap objects (no `new Object()`, no multi-dimensional arrays `double[][][]`) inside chunk loops!**
- Always reuse thread-local scratch contexts:
  - `ThreadLocal<GeneratorContext>` in `TerrainGenerator`
  - `ThreadLocal<MesherContext>` in `ChunkMesher`
- Use flat 1D primitive arrays indexed with bitwise coordinates: `(lx << 8) | (ly << 4) | lz`.

### C. Coordinate System & World Constants
- Right-handed 3D coordinate system ($+Y$ is up, 1 unit = 1 meter = 1 block).
- **Cubic Chunks:** Chunks are 3D cubes of $16 \times 16 \times 16$ blocks (`Chunk.SIZE_X = 16`, `SIZE_Y = 16`, `SIZE_Z = 16`).
- **Sea Level:** Fixed at `SEA_LEVEL = 28`.
  - Water only generates when `wy > height && wy <= SEA_LEVEL`.
  - A block at `height == 28` is dry land. Water basins must have seabed `height <= 27`.

### D. Client Tick vs. World Tick Separation
- In [`Main.java`](file:///c:/Users/september/Desktop/ja/antigravity/java%20voxel/src/main/java/com/voxel/Main.java), the game loop strictly differentiates between:
  - `clientTick(dt)`: Runs every frame. Processes UI, menus, chat, local mouse look, camera, HUD.
  - `worldTick(dt)`: Advances world simulation (block physics, time of day, day/night cycle).
- When the game is paused (Pause Menu / Options from Pause), `worldTick(0.0f)` is called, freezing world time while keeping UI active.
- When Chat is opened, player movement is paused, but `worldTick(dt)` continues ticking in the background (Minecraft behavior).

### E. Storage Formats & Save Data Integrity
- The engine supports two storage formats:
  - **Format A (`.dat`, `VOX4`/`VOX5`):** Bit-per-block diffs of modified blocks.
  - **Format B (`.mca`, `VOXR` / `MCAV` v1):** Compressed Anvil region files ($32 \times 32$ chunk columns, Deflate level 6).
- All saves must use **atomic writes**: save to `.tmp` first, then replace via `Files.move(..., ATOMIC_MOVE)` to prevent world corruption.
- When refactoring blocks or chunk data, never break backward compatibility with existing worlds.

### F. Default Project Language: English Only
- **Primary Language Standard:** The entire codebase and engineering ecosystem must be written strictly in **English by default**:
  - **Code Identifiers:** All classes, methods, fields, interfaces, enums, records, and packages must be named in English.
  - **Comments & Documentation:** All inline code comments, JavaDoc blocks, and newly created technical documents must be written in English.
  - **Logs & Diagnostics:** All `Logger.*` calls, crash dumps, profiler messages, and debug console outputs must be emitted in English.
  - **Git Commits:** All Git commit messages, branch names, and summaries must be in English following standard Conventional Commits (`feat:`, `fix:`, `refactor:`, `docs:`).
  - **In-Game Strings & I18n:** English (`en_us.json`) is the baseline primary language. Polish (`pl_pl.json`) is supported strictly as a secondary localization dictionary loaded through `I18n.t(...)`. Never hardcode non-English strings in the engine.

---

## 4. How to Extend the Engine

### Adding a New Block
1. Currently defined in [`BlockType.java`](file:///c:/Users/september/Desktop/ja/antigravity/java%20voxel/src/main/java/com/voxel/block/BlockType.java) (moving to modular `Block` / `BlockState` in task #24).
2. Assign an ID, define opacity/solidity/cross-model properties.
3. Add localization keys in `assets/lang/en_us.json` and `assets/lang/pl_pl.json` (`block.<name>`).
4. Ensure texture coordinates are defined in [`TextureAtlas.java`](file:///c:/Users/september/Desktop/ja/antigravity/java%20voxel/src/main/java/com/voxel/render/TextureAtlas.java) (or PNG asset in task #23).

### Adding a New Chat Command
1. Edit [`ChatSystem.java`](file:///c:/Users/september/Desktop/ja/antigravity/java%20voxel/src/main/java/com/voxel/ui/ChatSystem.java).
2. Register the command label and argument descriptors in the suggestion list for auto-completion.
3. Handle execution inside `executeCommand()`.

### Adding a New Setting / Gamerule
1. Add field and default value in [`GameSettings.java`](file:///c:/Users/september/Desktop/ja/antigravity/java%20voxel/src/main/java/com/voxel/config/GameSettings.java).
2. Add persistence in `load()` and `save()` (`options.properties`).
3. Add control widget (button or slider) in [`OptionsMenu.java`](file:///c:/Users/september/Desktop/ja/antigravity/java%20voxel/src/main/java/com/voxel/ui/OptionsMenu.java).
4. Add localization keys (`options.<name>`).

---

## 5. Documentation Map for Agents

When you need deep details on specific subsystems, consult the following docs:
- **Architecture & System Roadmap:** [`docs/ARCHITECTURE.md`](file:///c:/Users/september/Desktop/ja/antigravity/java%20voxel/docs/ARCHITECTURE.md)
- **World Generation, Biomes & Caves:** [`docs/WORLD_GENERATION.md`](file:///c:/Users/september/Desktop/ja/antigravity/java%20voxel/docs/WORLD_GENERATION.md)
- **Region Storage & Anvil MCA Format:** [`docs/REGION_STORAGE_FORMAT.md`](file:///c:/Users/september/Desktop/ja/antigravity/java%20voxel/docs/REGION_STORAGE_FORMAT.md)
- **Pending Tasks & Technical Specs:** [`docs/TODO.md`](file:///c:/Users/september/Desktop/ja/antigravity/java%20voxel/docs/TODO.md)
- **User's Scratchpad:** [`developer todo.md`](file:///c:/Users/september/Desktop/ja/antigravity/java%20voxel/developer%20todo.md)
