package com.voxel.i18n;

import com.voxel.util.Logger;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public final class I18n {

    public static final String DEFAULT_LANG = "en_us";
    private static String currentLang = DEFAULT_LANG;

    private static final Map<String, String> defaultTranslations = new HashMap<>();
    private static final Map<String, String> currentTranslations = new HashMap<>();

    public static final String[] SUPPORTED_LANGS = {"en_us", "pl_pl"};
    public static final String[] SUPPORTED_LANG_NAMES = {"English (US)", "Polski"};

    static {
        loadDefault();
        load(DEFAULT_LANG);
    }

    private I18n() {}

    public static String getCurrentLang() {
        return currentLang;
    }

    public static String getLanguageDisplayName(String code) {
        for (int i = 0; i < SUPPORTED_LANGS.length; i++) {
            if (SUPPORTED_LANGS[i].equalsIgnoreCase(code)) {
                return SUPPORTED_LANG_NAMES[i];
            }
        }
        return code;
    }

    public static String getNextLang(String current) {
        for (int i = 0; i < SUPPORTED_LANGS.length; i++) {
            if (SUPPORTED_LANGS[i].equalsIgnoreCase(current)) {
                return SUPPORTED_LANGS[(i + 1) % SUPPORTED_LANGS.length];
            }
        }
        return DEFAULT_LANG;
    }

    private static void loadDefault() {
        defaultTranslations.clear();
        Map<String, String> map = readLangFile(DEFAULT_LANG);
        if (map != null) {
            defaultTranslations.putAll(map);
        }
    }

    public static void load(String langCode) {
        if (langCode == null || langCode.isBlank()) {
            langCode = DEFAULT_LANG;
        }
        currentLang = langCode.toLowerCase().trim();
        currentTranslations.clear();

        if (defaultTranslations.isEmpty()) {
            loadDefault();
        }

        Map<String, String> map = readLangFile(currentLang);
        if (map != null) {
            currentTranslations.putAll(map);
            Logger.info("I18n", "Loaded language '%s' (%d entries)", currentLang, currentTranslations.size());
        } else {
            Logger.warn("I18n", "Could not load language '%s', falling back to '%s'", currentLang, DEFAULT_LANG);
            currentLang = DEFAULT_LANG;
        }
    }

    public static String t(String key, Object... args) {
        String val = currentTranslations.get(key);
        if (val == null) {
            val = defaultTranslations.get(key);
        }
        if (val == null) {
            return key;
        }
        if (args != null && args.length > 0) {
            try {
                return String.format(Locale.ROOT, val, args);
            } catch (Exception e) {
                try {
                    String fallback = val.replaceAll("%[0-9.]*[df]", "%s");
                    return String.format(Locale.ROOT, fallback, args);
                } catch (Exception ignored) {
                    return val;
                }
            }
        }
        return val;
    }

    private static Map<String, String> readLangFile(String langCode) {
        String fileName = langCode + ".json";

        // 1. Try disk path (run/assets/lang/<lang>.json or assets/lang/<lang>.json)
        Path diskPath = com.voxel.util.GamePaths.resolve("assets", "lang", fileName);
        if (Files.exists(diskPath)) {
            try (BufferedReader reader = Files.newBufferedReader(diskPath, StandardCharsets.UTF_8)) {
                return parseJsonMap(reader);
            } catch (Exception e) {
                Logger.warn("I18n", "Error reading disk lang file %s: %s", diskPath, e.getMessage());
            }
        }

        // 2. Try classpath resource (/assets/lang/<lang>.json)
        String resPath = "/assets/lang/" + fileName;
        try (InputStream in = I18n.class.getResourceAsStream(resPath)) {
            if (in != null) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                    return parseJsonMap(reader);
                }
            }
        } catch (Exception e) {
            Logger.warn("I18n", "Error reading classpath resource %s: %s", resPath, e.getMessage());
        }

        return null;
    }

    /**
     * Minimalist, robust zero-dependency JSON string-to-string dictionary parser.
     */
    public static Map<String, String> parseJsonMap(BufferedReader reader) throws Exception {
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            // Strip single-line comments if any
            String trimmed = line.trim();
            if (trimmed.startsWith("//") || trimmed.startsWith("#")) continue;
            sb.append(line).append('\n');
        }
        String json = sb.toString();

        Map<String, String> result = new HashMap<>();
        int len = json.length();
        int i = 0;

        // Skip to opening '{'
        while (i < len && json.charAt(i) != '{') i++;
        if (i >= len) return result;
        i++; // skip '{'

        while (i < len) {
            // Find key string
            while (i < len && json.charAt(i) != '"' && json.charAt(i) != '}') i++;
            if (i >= len || json.charAt(i) == '}') break;
            i++; // skip open quote of key

            int keyStart = i;
            StringBuilder keySb = new StringBuilder();
            while (i < len && json.charAt(i) != '"') {
                if (json.charAt(i) == '\\' && i + 1 < len) {
                    i++;
                    char esc = json.charAt(i);
                    if (esc == 'n') keySb.append('\n');
                    else if (esc == 't') keySb.append('\t');
                    else keySb.append(esc);
                } else {
                    keySb.append(json.charAt(i));
                }
                i++;
            }
            if (i >= len) break;
            i++; // skip closing quote of key
            String key = keySb.toString();

            // Find colon ':'
            while (i < len && json.charAt(i) != ':') i++;
            if (i >= len) break;
            i++; // skip colon

            // Find open quote of value
            while (i < len && json.charAt(i) != '"') i++;
            if (i >= len) break;
            i++; // skip open quote of value

            StringBuilder valSb = new StringBuilder();
            while (i < len && json.charAt(i) != '"') {
                if (json.charAt(i) == '\\' && i + 1 < len) {
                    i++;
                    char esc = json.charAt(i);
                    if (esc == 'n') valSb.append('\n');
                    else if (esc == 't') valSb.append('\t');
                    else valSb.append(esc);
                } else {
                    valSb.append(json.charAt(i));
                }
                i++;
            }
            if (i < len) i++; // skip closing quote of value
            String value = valSb.toString();

            result.put(key, value);

            // Find next comma or closing brace
            while (i < len && json.charAt(i) != ',' && json.charAt(i) != '}') i++;
            if (i < len && json.charAt(i) == '}') break;
            if (i < len && json.charAt(i) == ',') i++;
        }

        return result;
    }
}
