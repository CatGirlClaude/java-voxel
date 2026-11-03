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
    public static final Vector4f COLOR_WARN = new Vector4f(1.0f, 0.78f, 0.28f, 1.0f);
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

    public void clearMessages() {
        clear();
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

    // Command Registry for modular extensible commands
    private final com.voxel.command.CommandRegistry commandRegistry = new com.voxel.command.CommandRegistry();

    public com.voxel.command.CommandRegistry getCommandRegistry() {
        return commandRegistry;
    }

    private void updateSuggestions() {
        suggestions.clear();
        selectedSuggestionIndex = 0;
        String text = inputBuffer.toString();
        if (!text.startsWith("/")) return;

        suggestions.addAll(commandRegistry.getSuggestions(text, null, null, null, this));
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
            com.voxel.util.Logger.info("Chat", "Command: /%s", cmdStr);
            executeCommand(cmdStr, world, player, hud);
        } else {
            com.voxel.util.Logger.info("Chat", "<Player>: %s", raw);
            addMessage("<Player> " + raw, COLOR_CHAT);
        }
    }

    public void executeCommand(String cmdLine, World world, Player player, HudRenderer hud) {
        commandRegistry.execute(cmdLine, world, player, hud, this);
    }

    public void onResize(int screenW, int screenH) {
        // Chat dynamically adapts box dimensions in render(hud, screenW, screenH)
    }
}
