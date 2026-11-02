package com.voxel.ui;

import com.voxel.block.BlockType;
import com.voxel.entity.Player;
import com.voxel.i18n.I18n;
import com.voxel.world.Biome;
import com.voxel.world.Chunk;
import com.voxel.world.World;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ChatSystem {
    public record ChatMessage(String text, Vector4f color, float age) {}
    public record Suggestion(String completionText, String label, String hint) {}

    private static final int MAX_MESSAGES = 30;
    private final List<ChatMessage> messages = new ArrayList<>();
    private final List<String> sentHistory = new ArrayList<>();
    private int historyIndex = -1;

    private boolean open = false;
    private boolean ignoreNextChar = false;
    private final StringBuilder inputBuffer = new StringBuilder();
    private int cursorPos = 0;

    // Autocompletion & suggestions
    private final List<Suggestion> suggestions = new ArrayList<>();
    private int selectedSuggestionIndex = 0;

    public static final Vector4f COLOR_INFO = new Vector4f(0.55f, 0.90f, 1.0f, 1.0f);
    public static final Vector4f COLOR_SUCCESS = new Vector4f(0.45f, 0.98f, 0.55f, 1.0f);
    public static final Vector4f COLOR_ERROR = new Vector4f(1.0f, 0.42f, 0.42f, 1.0f);
    public static final Vector4f COLOR_CHAT = new Vector4f(1.0f, 1.0f, 1.0f, 1.0f);
    public static final Vector4f COLOR_HELP = new Vector4f(1.0f, 0.88f, 0.38f, 1.0f);

    public ChatSystem() {
        addMessage(I18n.t("chat.welcome"), COLOR_INFO);
        addMessage(I18n.t("chat.help_hint"), COLOR_HELP);
    }

    public boolean isOpen() {
        return open;
    }

    public String getInputText() {
        return inputBuffer.toString();
    }

    public int getCursorPos() {
        return Math.max(0, Math.min(cursorPos, inputBuffer.length()));
    }

    public List<ChatMessage> getMessages() {
        return messages;
    }

    public void clear() {
        messages.clear();
    }

    public boolean hasSuggestions() {
        return !suggestions.isEmpty();
    }

    public List<Suggestion> getSuggestions() {
        return suggestions;
    }

    public int getSelectedSuggestionIndex() {
        return selectedSuggestionIndex;
    }

    public void navigateSuggestions(int delta) {
        if (suggestions.isEmpty()) return;
        selectedSuggestionIndex = (selectedSuggestionIndex + delta + suggestions.size()) % suggestions.size();
    }

    public void onTabComplete() {
        if (suggestions.isEmpty()) return;
        if (selectedSuggestionIndex < 0 || selectedSuggestionIndex >= suggestions.size()) {
            selectedSuggestionIndex = 0;
        }
        Suggestion sel = suggestions.get(selectedSuggestionIndex);
        inputBuffer.setLength(0);
        inputBuffer.append(sel.completionText());
        cursorPos = inputBuffer.length();
        selectedSuggestionIndex = (selectedSuggestionIndex + 1) % suggestions.size();
        updateSuggestions();
    }

    private String savedInputBuffer = "";

    public void openChat(String initialText, boolean openedViaCharKey) {
        open = true;
        inputBuffer.setLength(0);
        if (initialText != null) {
            inputBuffer.append(initialText);
        }
        cursorPos = inputBuffer.length();
        historyIndex = sentHistory.size();
        savedInputBuffer = "";
        ignoreNextChar = openedViaCharKey;
        updateSuggestions();
    }

    public void closeChat() {
        open = false;
        inputBuffer.setLength(0);
        cursorPos = 0;
        ignoreNextChar = false;
        suggestions.clear();
        selectedSuggestionIndex = 0;
    }

    public void onCharInput(char c) {
        if (!open) return;
        if (ignoreNextChar) {
            ignoreNextChar = false;
            return;
        }
        if (c >= 32 && inputBuffer.length() < 110) {
            cursorPos = getCursorPos();
            inputBuffer.insert(cursorPos, c);
            cursorPos++;
            updateSuggestions();
        }
    }

    public void onBackspace() {
        if (!open) return;
        cursorPos = getCursorPos();
        if (cursorPos > 0) {
            inputBuffer.deleteCharAt(cursorPos - 1);
            cursorPos--;
            updateSuggestions();
        }
    }

    public void onDelete() {
        if (!open) return;
        cursorPos = getCursorPos();
        if (cursorPos < inputBuffer.length()) {
            inputBuffer.deleteCharAt(cursorPos);
            updateSuggestions();
        }
    }

    public void moveCursor(int delta) {
        if (!open) return;
        cursorPos = Math.max(0, Math.min(inputBuffer.length(), cursorPos + delta));
    }

    public void moveCursorToStart() {
        if (!open) return;
        cursorPos = 0;
    }

    public void moveCursorToEnd() {
        if (!open) return;
        cursorPos = inputBuffer.length();
    }

    public void navigateHistory(int delta) {
        if (!open || sentHistory.isEmpty()) return;
        if (historyIndex == sentHistory.size() && delta < 0) {
            savedInputBuffer = inputBuffer.toString();
        }
        int newIndex = historyIndex + delta;
        if (newIndex < 0) {
            newIndex = 0;
        } else if (newIndex > sentHistory.size()) {
            newIndex = sentHistory.size();
        }
        if (newIndex == historyIndex) return;
        historyIndex = newIndex;

        inputBuffer.setLength(0);
        if (historyIndex < sentHistory.size()) {
            inputBuffer.append(sentHistory.get(historyIndex));
        } else {
            inputBuffer.append(savedInputBuffer);
        }
        cursorPos = inputBuffer.length();
        updateSuggestions();
    }

    private static final List<Suggestion> BASE_COMMANDS = List.of(
            new Suggestion("/tp ", "/tp", "<x> <y> <z> | Teleport to coordinates"),
            new Suggestion("/time ", "/time", "<day|noon|sunset|night|pause|resume>"),
            new Suggestion("/fly", "/fly", "Toggle creative flight"),
            new Suggestion("/noclip", "/noclip", "Toggle ghost movement through blocks"),
            new Suggestion("/setblock ", "/setblock", "<x> <y> <z> <block> | Place a block"),
            new Suggestion("/locate ", "/locate", "<biome> | Find nearest biome"),
            new Suggestion("/bench ", "/bench", "[chunks|live] | Run engine benchmark"),
            new Suggestion("/worldsetting ", "/worldsetting", "<water|physics> | Configure liquid & block physics"),
            new Suggestion("/reset ", "/reset", "[seed|same] | Regenerate world"),
            new Suggestion("/spawn", "/spawn", "Return to world spawn point"),
            new Suggestion("/save", "/save", "Save current world and player"),
            new Suggestion("/load", "/load", "Reload world from disk"),
            new Suggestion("/clear", "/clear", "Clear chat history"),
            new Suggestion("/debug", "/debug", "Toggle F3 debug overlay"),
            new Suggestion("/logs ", "/logs", "[count|dump] | View logs or dump report"),
            new Suggestion("/help", "/help", "Show command list")
    );

    private void updateSuggestions() {
        suggestions.clear();
        selectedSuggestionIndex = 0;
        String text = inputBuffer.toString();
        if (!text.startsWith("/")) return;

        String[] parts = text.split("\\s+", -1);
        String cmdToken = parts[0].toLowerCase();

        if (parts.length <= 1) {
            // Suggest matching commands
            for (Suggestion base : BASE_COMMANDS) {
                if (base.label().toLowerCase().startsWith(cmdToken)) {
                    suggestions.add(base);
                }
            }
        } else {
            String argToken = parts[parts.length - 1].toLowerCase();
            String prefix = text.substring(0, text.lastIndexOf(parts[parts.length - 1]));

            if (cmdToken.equals("/time")) {
                String[] timeOptions = {"day", "noon", "sunset", "night", "pause", "resume", "set"};
                for (String opt : timeOptions) {
                    if (opt.startsWith(argToken)) {
                        suggestions.add(new Suggestion(prefix + opt, opt, "Time parameter"));
                    }
                }
            } else if (cmdToken.equals("/locate")) {
                String[] biomes = {"ocean", "deep_ocean", "frozen_ocean", "beach", "plains", "forest",
                        "birch_forest", "desert", "mesa", "taiga", "snowy_tundra", "jungle", "swamp", "mountains"};
                for (String b : biomes) {
                    if (b.startsWith(argToken)) {
                        suggestions.add(new Suggestion(prefix + b, b, "Biome name"));
                    }
                }
            } else if (cmdToken.equals("/worldsetting") || cmdToken.equals("/ws")) {
                if (parts.length == 2) {
                    String[] wsOptions = {"water", "physics"};
                    for (String opt : wsOptions) {
                        if (opt.startsWith(argToken)) {
                            suggestions.add(new Suggestion(prefix + opt + " ", opt, "World setting category"));
                        }
                    }
                } else if (parts.length == 3 && (parts[1].equalsIgnoreCase("water") || parts[1].equalsIgnoreCase("woda"))) {
                    String[] waterModes = {"modern", "preclassic", "terraria", "minecraft"};
                    for (String m : waterModes) {
                        if (m.startsWith(argToken)) {
                            suggestions.add(new Suggestion(prefix + m, m, "Water simulation mode"));
                        }
                    }
                } else if (parts.length == 3 && (parts[1].equalsIgnoreCase("physics") || parts[1].equalsIgnoreCase("fizyka"))) {
                    String[] physModes = {"on", "off"};
                    for (String p : physModes) {
                        if (p.startsWith(argToken)) {
                            suggestions.add(new Suggestion(prefix + p, p, "Toggle physics"));
                        }
                    }
                }
            } else if (cmdToken.equals("/bench")) {
                String[] benchOpts = {"16", "32", "64", "128", "live"};
                for (String b : benchOpts) {
                    if (b.startsWith(argToken)) {
                        suggestions.add(new Suggestion(prefix + b, b, "Benchmark target"));
                    }
                }
            } else if (cmdToken.equals("/reset")) {
                String[] resetOpts = {"same", "random"};
                for (String r : resetOpts) {
                    if (r.startsWith(argToken)) {
                        suggestions.add(new Suggestion(prefix + r, r, "Seed reset mode"));
                    }
                }
            } else if (cmdToken.equals("/logs")) {
                String[] logOpts = {"10", "30", "50", "dump"};
                for (String l : logOpts) {
                    if (l.startsWith(argToken)) {
                        suggestions.add(new Suggestion(prefix + l, l, "Log action"));
                    }
                }
            } else if (cmdToken.equals("/setblock") && parts.length >= 5) {
                for (BlockType bt : BlockType.values()) {
                    String bName = bt.name().toLowerCase();
                    if (bName.startsWith(argToken)) {
                        suggestions.add(new Suggestion(prefix + bName, bName, bt.name));
                    }
                }
            } else if ((cmdToken.equals("/tp") || cmdToken.equals("/setblock")) && argToken.isEmpty()) {
                suggestions.add(new Suggestion(prefix + "~", "~", "Relative current coordinate"));
            }
        }
    }

    public void update(float dt) {
        for (int i = 0; i < messages.size(); i++) {
            ChatMessage m = messages.get(i);
            messages.set(i, new ChatMessage(m.text(), m.color(), m.age() + dt));
        }
    }

    public void addMessage(String text, Vector4f color) {
        messages.add(new ChatMessage(text, new Vector4f(color), 0.0f));
        if (messages.size() > MAX_MESSAGES) {
            messages.removeFirst();
        }
    }

    public void submit(World world, Player player, HudRenderer hud) {
        String raw = inputBuffer.toString().trim();
        closeChat();
        if (raw.isEmpty()) {
            return;
        }

        if (sentHistory.isEmpty() || !sentHistory.getLast().equals(raw)) {
            sentHistory.add(raw);
        }

        if (raw.startsWith("/") || raw.toLowerCase().startsWith("tp ")) {
            String cmdStr = raw.startsWith("/") ? raw.substring(1).trim() : raw;
            com.voxel.util.Logger.info("Chat", "Komenda: /%s", cmdStr);
            executeCommand(cmdStr, world, player, hud);
        } else {
            com.voxel.util.Logger.info("Chat", "<Gracz>: %s", raw);
            addMessage("<Gracz> " + raw, COLOR_CHAT);
        }
    }

    private void executeCommand(String cmdLine, World world, Player player, HudRenderer hud) {
        if (cmdLine.isEmpty()) return;
        String[] parts = cmdLine.split("\\s+");
        String cmd = parts[0].toLowerCase();

        switch (cmd) {
            case "tp", "teleport" -> handleTeleport(parts, world, player);
            case "spawn" -> {
                long sy = world.findSpawnY(0, 0);
                teleportPlayer(world, player, 0.5, sy, 0.5);
                addMessage(String.format("Przeteleportowano na spawn: (0.5, %d.0, 0.5)", sy), COLOR_SUCCESS);
            }
            case "fly" -> {
                player.flying = !player.flying;
                if (!player.flying) {
                    player.noclip = false;
                }
                player.velocity.y = 0;
                addMessage("Tryb latania: " + (player.flying ? "WLACZONY" : "WYLACZONY"), COLOR_SUCCESS);
            }
            case "noclip", "nc" -> {
                player.noclip = !player.noclip;
                if (player.noclip) {
                    player.flying = true;
                }
                player.velocity.set(0, 0, 0);
                addMessage("Tryb noclip (przenikanie przez bloki): " + (player.noclip ? "WLACZONY" : "WYLACZONY"), COLOR_SUCCESS);
            }
            case "debug" -> {
                hud.showDebug = !hud.showDebug;
                addMessage("Panel debugowania: " + (hud.showDebug ? "WLACZONY" : "WYLACZONY"), COLOR_INFO);
            }
            case "time" -> {
                if (parts.length < 2) {
                    addMessage("Uzycie: /time <day|noon|sunset|night|pause|resume|0..24>", COLOR_ERROR);
                } else {
                    String arg = parts[1].toLowerCase();
                    if (arg.equals("set") && parts.length >= 3) {
                        arg = parts[2].toLowerCase();
                    }
                    switch (arg) {
                        case "day", "morning", "rano", "dzien" -> {
                            world.timeOfDay = 0.30f;
                            addMessage("Ustawiono pore dnia na poranek (07:12).", COLOR_SUCCESS);
                        }
                        case "noon", "poludnie" -> {
                            world.timeOfDay = 0.50f;
                            addMessage("Ustawiono pore dnia na poludnie (12:00).", COLOR_SUCCESS);
                        }
                        case "sunset", "zachod" -> {
                            world.timeOfDay = 0.77f;
                            addMessage("Ustawiono pore dnia na zachod slonca (18:28).", COLOR_SUCCESS);
                        }
                        case "night", "noc", "midnight" -> {
                            world.timeOfDay = 0.0f;
                            addMessage("Ustawiono pore dnia na polnoc (00:00).", COLOR_SUCCESS);
                        }
                        case "pause", "stop", "pauza" -> {
                            world.dayCyclePaused = true;
                            addMessage("Cykl dnia i nocy: ZATRZYMANY.", COLOR_INFO);
                        }
                        case "resume", "start", "wznow" -> {
                            world.dayCyclePaused = false;
                            addMessage("Cykl dnia i nocy: WZNOWIONY.", COLOR_INFO);
                        }
                        default -> {
                            try {
                                float hour = Float.parseFloat(arg);
                                world.timeOfDay = ((hour % 24.0f) + 24.0f) % 24.0f / 24.0f;
                                addMessage(String.format("Ustawiono godzine na %.1f:00.", hour), COLOR_SUCCESS);
                            } catch (NumberFormatException e) {
                                addMessage("Nieznana opcja: " + arg + ". Uzyj day, noon, sunset, night, pause, resume lub 0..24.", COLOR_ERROR);
                            }
                        }
                    }
                }
            }
            case "benchmark", "bench", "perf" -> handleBenchmark(parts, world, player, hud);
            case "save" -> {
                if (world.saveWorld(player)) {
                    addMessage("Zapisano swiat do saves/world.dat!", COLOR_SUCCESS);
                } else {
                    addMessage("Blad podczas zapisywania swiata!", COLOR_ERROR);
                }
            }
            case "load" -> {
                if (world.loadWorld(player)) {
                    addMessage("Wczytano swiat z saves/world.dat!", COLOR_SUCCESS);
                } else {
                    addMessage("Brak zapisanego swiata (saves/world.dat)!", COLOR_ERROR);
                }
            }
            case "setblock" -> handleSetBlock(parts, world, player);
            case "reset", "new", "regen", "restart" -> handleResetWorld(parts, world, player);
            case "clear", "cls" -> messages.clear();
            case "pos", "where" -> addMessage(String.format("Twoja pozycja: X=%.1f, Y=%.1f, Z=%.1f",
                    player.position.x, player.position.y, player.position.z), COLOR_INFO);
            case "locate", "biome", "biom" -> handleLocateBiome(parts, world, player);
            case "help", "?" -> {
                addMessage("=== Dostepne komendy ===", COLOR_HELP);
                addMessage("/setblock <x> <y> <z> <blok> - Ustawia blok (wspiera ~ np. /setblock ~ ~-1 ~ stone)", COLOR_HELP);
                addMessage("/tp <x> <y> <z> - Teleportacja (wspiera ~ np. /tp ~ ~15 ~, bez limitu 30M)", COLOR_HELP);
                addMessage("/tp <x> <z>     - Teleportacja na powierzchnie terenu w punkcie (x, z)", COLOR_HELP);
                addMessage("/locate <biom>  - Znajduje najblizszy biom (ocean, deep_ocean, frozen_ocean, desert, mesa, snow, taiga, jungle, birch, forest, plains, mountains)", COLOR_HELP);
                addMessage("/reset [seed|same] - Resetuje swiat (nowy losowy seed, podany seed lub 'same')", COLOR_HELP);
                addMessage("/noclip (/nc)   - Przenikanie przez sciany i podloze", COLOR_HELP);
                addMessage("/fly            - Przelaczanie trybu latania", COLOR_HELP);
                addMessage("/spawn          - Powrot na punkt startowy (0, Y, 0)", COLOR_HELP);
                addMessage("/time <day|noon|sunset|night|pause|resume|0..24> - Pora dnia", COLOR_HELP);
                addMessage("/debug          - Pokaz/ukryj panel debugowania w lewym gornym rogu", COLOR_HELP);
                addMessage("/save | /load | /clear - Zapis, odczyt swiata, wyczyszczenie czatu", COLOR_HELP);
                addMessage("/worldsetting [water|physics] - Ustawienia fizyki plynow i grawitacji", COLOR_HELP);
                addMessage("/bench [chunki|live] - Uruchamia in-game benchmark wydajnosci lub przelacza live overlay", COLOR_HELP);
                addMessage("/logs [ile|dump] - Pokaz ostatnie logi z pliku lub wykonaj pelny zrzut awaryjny", COLOR_HELP);
            }
            case "worldsetting", "ws" -> handleWorldSetting(parts, world);
            case "logs", "log" -> {
                if (parts.length > 1 && parts[1].equalsIgnoreCase("dump")) {
                    java.util.Map<String, String> ctx = new java.util.HashMap<>();
                    ctx.put("Pozycja gracza", String.format("%.2f, %.2f, %.2f", player.position.x, player.position.y, player.position.z));
                    ctx.put("Seed swiata", String.valueOf(world.getSeed()));
                    java.nio.file.Path p = com.voxel.util.Logger.dumpCrashReport("Zrzut z komendy /logs dump", null, ctx);
                    addMessage("Zapisano raport awaryjny: " + p.getFileName(), COLOR_SUCCESS);
                } else {
                    int count = 6;
                    if (parts.length > 1) {
                        try { count = Integer.parseInt(parts[1]); } catch (Exception ignored) {}
                    }
                    addMessage("=== Ostatnie logi (logs/latest.log) ===", COLOR_HELP);
                    List<String> list = com.voxel.util.Logger.getRecentLogs(Math.min(15, count));
                    for (String l : list) {
                        Vector4f col = l.contains("/ERROR]") ? COLOR_ERROR : (l.contains("/WARN]") ? COLOR_HELP : COLOR_INFO);
                        addMessage(l.length() > 65 ? l.substring(0, 62) + "..." : l, col);
                    }
                }
            }
            default -> addMessage("Nieznana komenda: /" + cmd + ". Wpisz /help po liste komend.", COLOR_ERROR);
        }
    }

    private void handleWorldSetting(String[] parts, World world) {
        if (parts.length == 1) {
            addMessage("=== World Settings (/worldsetting) ===", COLOR_HELP);
            addMessage(String.format("Water physics mode: %s (modern / preclassic / terraria)", com.voxel.config.GameSettings.waterPhysicsMode), COLOR_INFO);
            addMessage(String.format("Block gravity physics: %s", com.voxel.config.GameSettings.blockPhysicsEnabled ? "ENABLED" : "DISABLED"), COLOR_INFO);
            addMessage("Usage: /worldsetting water <modern|preclassic|terraria> or /worldsetting physics <on|off>", COLOR_HELP);
            return;
        }

        String sub = parts[1].toLowerCase();
        if (sub.equals("water") || sub.equals("woda")) {
            if (parts.length > 2) {
                String mode = parts[2].toLowerCase();
                if (mode.equals("modern") || mode.equals("minecraft") || mode.equals("mc")) {
                    com.voxel.config.GameSettings.waterPhysicsMode = "modern";
                    com.voxel.config.GameSettings.save();
                    addMessage("Water physics mode set to: MODERN MINECRAFT (Flow levels 1..7 & sources).", COLOR_SUCCESS);
                } else if (mode.equals("preclassic") || mode.equals("classic")) {
                    com.voxel.config.GameSettings.waterPhysicsMode = "preclassic";
                    com.voxel.config.GameSettings.save();
                    addMessage("Water physics mode set to: PRE-CLASSIC (Infinite source spreading).", COLOR_SUCCESS);
                } else if (mode.equals("terraria") || mode.equals("ter")) {
                    com.voxel.config.GameSettings.waterPhysicsMode = "terraria";
                    com.voxel.config.GameSettings.save();
                    addMessage("Water physics mode set to: TERRARIA (Finite volume liquid conservation).", COLOR_SUCCESS);
                } else {
                    addMessage("Unknown water mode: " + mode + ". Choose 'modern', 'preclassic', or 'terraria'.", COLOR_ERROR);
                }
            } else {
                addMessage("Current water physics mode: " + com.voxel.config.GameSettings.waterPhysicsMode + ". Usage: /worldsetting water <modern|preclassic|terraria>", COLOR_INFO);
            }
        } else if (sub.equals("physics") || sub.equals("fizyka")) {
            if (parts.length > 2) {
                String val = parts[2].toLowerCase();
                if (val.equals("on") || val.equals("true") || val.equals("wlacz")) {
                    com.voxel.config.GameSettings.blockPhysicsEnabled = true;
                    com.voxel.config.GameSettings.save();
                    addMessage("Block physics: ENABLED.", COLOR_SUCCESS);
                } else if (val.equals("off") || val.equals("false") || val.equals("wylacz")) {
                    com.voxel.config.GameSettings.blockPhysicsEnabled = false;
                    com.voxel.config.GameSettings.save();
                    addMessage("Block physics: DISABLED.", COLOR_INFO);
                } else {
                    addMessage("Usage: /worldsetting physics <on|off>", COLOR_ERROR);
                }
            } else {
                com.voxel.config.GameSettings.blockPhysicsEnabled = !com.voxel.config.GameSettings.blockPhysicsEnabled;
                com.voxel.config.GameSettings.save();
                addMessage("Block physics: " + (com.voxel.config.GameSettings.blockPhysicsEnabled ? "ENABLED" : "DISABLED"), COLOR_INFO);
            }
        } else {
            addMessage("Unknown option: " + sub + ". Usage: /worldsetting water <modern|preclassic|terraria> or /worldsetting physics <on|off>", COLOR_ERROR);
        }
    }

    private void handleBenchmark(String[] parts, World world, Player player, HudRenderer hud) {
        int count = 512;
        if (parts.length > 1) {
            String arg = parts[1].toLowerCase();
            if (arg.equals("live") || arg.equals("hud") || arg.equals("toggle")) {
                hud.showBenchmarkOverlay = !hud.showBenchmarkOverlay;
                addMessage("Wskaznik benchmarku HUD: " + (hud.showBenchmarkOverlay ? "WLACZONY" : "WYLACZONY"), COLOR_INFO);
                return;
            }
            try {
                count = Math.max(32, Math.min(4096, Integer.parseInt(parts[1])));
            } catch (NumberFormatException ignored) {}
        }
        addMessage(String.format("Uruchamianie benchmarku [%d chunkow, %d watkow CPU]...",
                count, world.getWorkerThreadCount()), COLOR_INFO);
        final int finalCount = count;
        world.runBenchmarkAsync(finalCount, result -> {
            hud.setBenchmarkResult(result);
            addMessage("=== WYNIKI BENCHMARKU SILNIKA VOXEL ===", COLOR_HELP);
            addMessage(String.format("Przetestowano: %d chunkow | Watki CPU: %d",
                    result.chunkCount(), result.workerThreads()), COLOR_INFO);
            addMessage(String.format("Generacja terenu: %.1f ms | %.0f ch/s (%.3f ms/chunk)",
                    result.terrainGenMs(), result.terrainGenRate(), result.avgTerrainGenMs()), COLOR_SUCCESS);
            addMessage(String.format("Budowa siatek (Meshing): %.1f ms | %.0f mesh/s (%.3f ms/mesh)",
                    result.meshingMs(), result.meshingRate(), result.avgMeshingMs()), COLOR_SUCCESS);
            addMessage(String.format("Calkowite tempo: %.0f chunkow/sekunde | Czas: %.1f ms",
                    result.totalRate(), result.totalTimeMs()), COLOR_INFO);
            addMessage(String.format("Ocena wydajnosci: %s", result.performanceRating()), COLOR_HELP);
        });
    }

    private void handleSetBlock(String[] parts, World world, Player player) {
        if (parts.length < 5) {
            addMessage("Uzycie: /setblock <x> <y> <z> <blok> (np. /setblock ~ ~-1 ~ stone lub /setblock 10 30 -50 water)", COLOR_ERROR);
            return;
        }
        try {
            double targetX = parseCoordinate(parts[1], player.position.x);
            double targetY = parseCoordinate(parts[2], player.position.y);
            double targetZ = parseCoordinate(parts[3], player.position.z);
            com.voxel.block.BlockType bt = com.voxel.block.BlockType.byName(parts[4]);
            if (bt == null) {
                addMessage("Nieznany typ bloku: '" + parts[4] + "'. Dostepne np.: stone, water, air, grass, dirt, wood, glowstone...", COLOR_ERROR);
                return;
            }
            long bx = (long) Math.floor(targetX);
            long by = (long) Math.floor(targetY);
            long bz = (long) Math.floor(targetZ);
            world.setBlock(bx, by, bz, bt.id, true);
            addMessage(String.format("Ustawiono blok %s [%d] na pozycji (%d, %d, %d)", bt.name, bt.id, bx, by, bz), COLOR_SUCCESS);
        } catch (NumberFormatException e) {
            addMessage("Blad wspolrzednych! Uzyj liczb lub ~ (np. /setblock ~ ~-1 ~ stone)", COLOR_ERROR);
        }
    }

    private void handleResetWorld(String[] parts, World world, Player player) {
        long newSeed;
        if (parts.length >= 2) {
            String arg = parts[1].trim();
            if (arg.equalsIgnoreCase("same") || arg.equalsIgnoreCase("current")) {
                newSeed = world.getSeed();
            } else {
                try {
                    newSeed = Long.parseLong(arg);
                } catch (NumberFormatException e) {
                    newSeed = arg.hashCode();
                }
            }
        } else {
            newSeed = System.nanoTime() ^ (System.currentTimeMillis() * 6364136223846793005L);
        }

        world.resetWorld(player, newSeed);
        addMessage(String.format("Zresetowano swiat! Nowy seed: %d, Spawn: (0.5, %.1f, 0.5)",
                newSeed, player.position.y), COLOR_SUCCESS);
    }

    private void handleLocateBiome(String[] parts, World world, Player player) {
        long bx = (long) Math.floor(player.position.x);
        long bz = (long) Math.floor(player.position.z);
        if (parts.length < 2) {
            Biome cur = world.getBiome(bx, bz);
            addMessage("Aktualny biom: " + cur.displayName + ". Uzyj /locate <ocean|deep_ocean|frozen_ocean|desert|mesa|snow|taiga|jungle|birch|forest|plains|mountains>", COLOR_INFO);
            return;
        }
        String arg = parts[1].toLowerCase();
        Biome target = switch (arg) {
            case "ocean", "morze" -> Biome.OCEAN;
            case "deep_ocean", "deepocean", "gleboki_ocean" -> Biome.DEEP_OCEAN;
            case "frozen_ocean", "frozenocean", "zamarzniety_ocean" -> Biome.FROZEN_OCEAN;
            case "beach", "plaza" -> Biome.BEACH;
            case "desert", "pustynia" -> Biome.DESERT;
            case "mesa", "badlands", "kaniony" -> Biome.MESA;
            case "snow", "tundra", "snieg" -> Biome.SNOWY_TUNDRA;
            case "taiga", "tajga" -> Biome.TAIGA;
            case "jungle", "dzungla" -> Biome.JUNGLE;
            case "birch", "brzoza" -> Biome.BIRCH_FOREST;
            case "forest", "las" -> Biome.FOREST;
            case "plains", "rowniny" -> Biome.PLAINS;
            case "mountains", "gory" -> Biome.MOUNTAINS;
            default -> null;
        };
        if (target == null) {
            addMessage("Nieznany biom: " + arg + ". Dostepne: ocean, deep_ocean, frozen_ocean, desert, mesa, snow, taiga, jungle, birch, forest, plains, mountains", COLOR_ERROR);
            return;
        }
        long[] found = world.locateBiome(target, bx, bz);
        if (found != null) {
            long sy = world.findSpawnY(found[0], found[1]);
            addMessage(String.format("Najblizszy biom [%s] znajduje sie w: X=%d, Y=%d, Z=%d (wpisz /tp %d %d %d)",
                    target.displayName, found[0], sy, found[1], found[0], sy, found[1]), COLOR_SUCCESS);
        } else {
            addMessage("Nie znaleziono biomu " + target.displayName + " w promieniu 16000 blokow.", COLOR_ERROR);
        }
    }

    private void handleTeleport(String[] parts, World world, Player player) {
        if (parts.length == 3) {
            try {
                double targetX = parseCoordinate(parts[1], player.position.x);
                double targetZ = parseCoordinate(parts[2], player.position.z);
                long bx = (long) Math.floor(targetX);
                long bz = (long) Math.floor(targetZ);
                long surfaceY = world.findSpawnY(bx, bz);
                teleportPlayer(world, player, targetX, surfaceY, targetZ);
                addMessage(String.format("Przeteleportowano do: (%.1f, %.1f, %.1f)", targetX, (double) surfaceY, targetZ), COLOR_SUCCESS);
            } catch (NumberFormatException e) {
                addMessage("Blad wspolrzednych! Uzycie: /tp <x> <y> <z> lub /tp <x> <z>", COLOR_ERROR);
            }
        } else if (parts.length >= 4) {
            try {
                double targetX = parseCoordinate(parts[1], player.position.x);
                double targetY = parseCoordinate(parts[2], player.position.y);
                double targetZ = parseCoordinate(parts[3], player.position.z);
                teleportPlayer(world, player, targetX, targetY, targetZ);
                addMessage(String.format("Przeteleportowano do: (%.1f, %.1f, %.1f)", targetX, targetY, targetZ), COLOR_SUCCESS);
            } catch (NumberFormatException e) {
                addMessage("Blad wspolrzednych! Uzycie: /tp <x> <y> <z> (np. /tp 100 -250 -50 lub /tp ~ ~-50 ~)", COLOR_ERROR);
            }
        } else {
            addMessage("Uzycie: /tp <x> <y> <z> lub /tp <x> <z> (wspiera ~ dla relatywnych pozycji)", COLOR_ERROR);
        }
    }

    private static double parseCoordinate(String token, double currentVal) throws NumberFormatException {
        double val;
        if (token.startsWith("~")) {
            if (token.length() == 1) {
                val = currentVal;
            } else {
                val = currentVal + Double.parseDouble(token.substring(1));
            }
        } else {
            val = Double.parseDouble(token);
        }
        if (!Double.isFinite(val)) {
            throw new NumberFormatException("Infinite or NaN coordinate");
        }
        return val;
    }

    private void teleportPlayer(World world, Player player, double x, double y, double z) {
        long cx = Math.floorDiv((long) Math.floor(x), (long) Chunk.SIZE_X);
        long cy = Math.floorDiv((long) Math.floor(y), (long) Chunk.SIZE_Y);
        long cz = Math.floorDiv((long) Math.floor(z), (long) Chunk.SIZE_Z);
        world.generateInitialChunks(cx, cy, cz, 3, 2);
        player.position.set(x, y, z);
        player.velocity.set(0, 0, 0);
    }

    public void onResize(int screenW, int screenH) {
        // Chat dynamically adapts box dimensions in render(hud, screenW, screenH)
    }
}
