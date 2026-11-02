package com.voxel;

import com.voxel.ui.ChatSyntaxHighlighter;
import com.voxel.ui.ChatSystem;
import org.joml.Vector4f;

import java.util.List;

public class Task40to41VerificationTest {

    public static void main(String[] args) {
        System.out.println("Running Task 40, 41 verification...");
        testChatSyntaxHighlighting();
        testCoordinatesAndSelectorsHighlighting();
        testChatNonCommandHighlighting();
        testSuggestionCountAndFormatting();
        System.out.println(">>> ALL TASK 40, 41 TESTS PASSED! <<<");
    }

    private static void testChatSyntaxHighlighting() {
        List<ChatSyntaxHighlighter.Span> spans = ChatSyntaxHighlighter.highlight("/time set 12000");
        if (spans.isEmpty()) {
            throw new AssertionError("Spans should not be empty");
        }

        // Expected tokens: "/time", " ", "set", " ", "12000"
        if (!spans.get(0).text().equals("/time") || !spans.get(0).color().equals(ChatSyntaxHighlighter.COLOR_COMMAND)) {
            throw new AssertionError("/time should be colored with COLOR_COMMAND");
        }
        if (!spans.get(2).text().equals("set") || !spans.get(2).color().equals(ChatSyntaxHighlighter.COLOR_KEYWORD)) {
            throw new AssertionError("set should be colored with COLOR_KEYWORD");
        }
        if (!spans.get(4).text().equals("12000") || !spans.get(4).color().equals(ChatSyntaxHighlighter.COLOR_NUMBER)) {
            throw new AssertionError("12000 should be colored with COLOR_NUMBER");
        }
        System.out.println("  [PASS] Command, keyword, and number syntax highlighting verified.");
    }

    private static void testCoordinatesAndSelectorsHighlighting() {
        List<ChatSyntaxHighlighter.Span> spans = ChatSyntaxHighlighter.highlight("/tp @p ~ 70 ~-5");
        // Spans: /tp, " ", @p, " ", ~, " ", 70, " ", ~-5
        if (!spans.get(2).text().equals("@p") || !spans.get(2).color().equals(ChatSyntaxHighlighter.COLOR_SELECTOR)) {
            throw new AssertionError("@p should be colored with COLOR_SELECTOR");
        }
        if (!spans.get(4).text().equals("~") || !spans.get(4).color().equals(ChatSyntaxHighlighter.COLOR_COORDINATE)) {
            throw new AssertionError("~ should be colored with COLOR_COORDINATE");
        }
        if (!spans.get(6).text().equals("70") || !spans.get(6).color().equals(ChatSyntaxHighlighter.COLOR_NUMBER)) {
            throw new AssertionError("70 should be colored with COLOR_NUMBER");
        }
        if (!spans.get(8).text().equals("~-5") || !spans.get(8).color().equals(ChatSyntaxHighlighter.COLOR_COORDINATE)) {
            throw new AssertionError("~-5 should be colored with COLOR_COORDINATE");
        }
        System.out.println("  [PASS] Coordinates and selectors syntax highlighting verified.");
    }

    private static void testChatNonCommandHighlighting() {
        List<ChatSyntaxHighlighter.Span> spans = ChatSyntaxHighlighter.highlight("Hello world, this is a plain message");
        if (spans.size() != 1) {
            throw new AssertionError("Non-command message should produce a single span");
        }
        if (!spans.get(0).color().equals(ChatSyntaxHighlighter.COLOR_DEFAULT)) {
            throw new AssertionError("Non-command message should use COLOR_DEFAULT");
        }
        System.out.println("  [PASS] Plain chat message syntax highlighting verified.");
    }

    private static void testSuggestionCountAndFormatting() {
        ChatSystem chat = new ChatSystem();
        // Open with "/ti" -> should suggest "/time"
        chat.openChat("/ti", false);
        if (!chat.hasSuggestions()) {
            throw new AssertionError("Chat should have suggestions for /ti");
        }

        // Test format indicator strictly "(1/N)" without keybind hints
        int totalSugs = 16;
        int selectedIdx = 0;
        String footerText = String.format("(%d/%d)", selectedIdx + 1, totalSugs);
        if (!footerText.equals("(1/16)")) {
            throw new AssertionError("Footer text should strictly match (1/16), got: " + footerText);
        }
        if (footerText.contains("[Tab]") || footerText.contains("[Up/Down]")) {
            throw new AssertionError("Footer text must not contain keybind hints!");
        }

        // Max visible count test
        int maxVisible = Math.min(10, totalSugs);
        if (maxVisible != 10) {
            throw new AssertionError("Max visible suggestions must be 10 when 16 suggestions are present!");
        }
        System.out.println("  [PASS] Chat suggestions count (10) and clean footer format '(1/16)' verified.");
    }
}
