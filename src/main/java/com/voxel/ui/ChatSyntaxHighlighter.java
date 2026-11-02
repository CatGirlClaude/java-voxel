package com.voxel.ui;

import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ChatSyntaxHighlighter {

    public record Span(String text, Vector4f color) {}

    public static final Vector4f COLOR_COMMAND = new Vector4f(0.35f, 0.88f, 1.0f, 1.0f);     // Cyan
    public static final Vector4f COLOR_KEYWORD = new Vector4f(0.75f, 0.65f, 1.0f, 1.0f);     // Violet / Purple
    public static final Vector4f COLOR_NUMBER = new Vector4f(0.45f, 0.95f, 0.55f, 1.0f);      // Green
    public static final Vector4f COLOR_COORDINATE = new Vector4f(1.0f, 0.80f, 0.35f, 1.0f);  // Amber
    public static final Vector4f COLOR_SELECTOR = new Vector4f(1.0f, 0.45f, 0.65f, 1.0f);    // Pink
    public static final Vector4f COLOR_STRING = new Vector4f(1.0f, 0.65f, 0.40f, 1.0f);      // Orange
    public static final Vector4f COLOR_DEFAULT = new Vector4f(0.92f, 0.94f, 0.96f, 1.0f);     // Light Gray / White

    private static final Set<String> KEYWORDS = new HashSet<>();

    static {
        String[] kw = {
            "set", "add", "query", "get", "day", "night", "noon", "midnight",
            "survival", "creative", "adventure", "spectator",
            "clear", "rain", "thunder", "live", "start", "stop",
            "same", "random", "dump", "physics", "water", "fog",
            "modern", "preclassic", "terraria", "minecraft",
            "on", "off", "true", "false", "toggle",
            "info", "warn", "error", "debug"
        };
        for (String k : kw) {
            KEYWORDS.add(k.toLowerCase());
        }
    }

    private static final Pattern TOKEN_PATTERN = Pattern.compile("(\\s+)|([^\\s]+)");
    private static final Pattern NUMBER_PATTERN = Pattern.compile("^[+-]?\\d+(\\.\\d+)?$");

    public static List<Span> highlight(String input) {
        List<Span> spans = new ArrayList<>();
        if (input == null || input.isEmpty()) {
            return spans;
        }

        // Non-command chat messages remain default color
        if (!input.startsWith("/")) {
            spans.add(new Span(input, COLOR_DEFAULT));
            return spans;
        }

        Matcher matcher = TOKEN_PATTERN.matcher(input);
        boolean firstToken = true;

        while (matcher.find()) {
            String whitespace = matcher.group(1);
            String token = matcher.group(2);

            if (whitespace != null) {
                spans.add(new Span(whitespace, COLOR_DEFAULT));
            } else if (token != null) {
                if (firstToken) {
                    firstToken = false;
                    spans.add(new Span(token, COLOR_COMMAND));
                } else {
                    spans.add(new Span(token, resolveTokenColor(token)));
                }
            }
        }

        return spans;
    }

    private static Vector4f resolveTokenColor(String token) {
        if (token == null || token.isEmpty()) {
            return COLOR_DEFAULT;
        }

        // Coordinates: ~ or ^
        if (token.startsWith("~") || token.startsWith("^")) {
            return COLOR_COORDINATE;
        }

        // Numbers: 10, -5, 3.14
        if (NUMBER_PATTERN.matcher(token).matches()) {
            return COLOR_NUMBER;
        }

        // Target selectors: @p, @a, @e, @s, @r
        if (token.startsWith("@")) {
            return COLOR_SELECTOR;
        }

        // Strings
        if ((token.startsWith("\"") && token.endsWith("\"")) || (token.startsWith("'") && token.endsWith("'"))) {
            return COLOR_STRING;
        }

        // Subcommands and keywords
        if (KEYWORDS.contains(token.toLowerCase())) {
            return COLOR_KEYWORD;
        }

        return COLOR_DEFAULT;
    }

    private ChatSyntaxHighlighter() {}
}
