package com.voxel.util;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.*;

public final class Logger {

    public enum Level {
        TRACE(0, "TRACE"),
        DEBUG(1, "DEBUG"),
        INFO(2, "INFO"),
        WARN(3, "WARN"),
        ERROR(4, "ERROR"),
        FATAL(5, "FATAL");

        public final int priority;
        public final String label;

        Level(int priority, String label) {
            this.priority = priority;
            this.label = label;
        }

        public static Level fromString(String name, Level defaultLevel) {
            if (name == null) return defaultLevel;
            for (Level l : values()) {
                if (l.name().equalsIgnoreCase(name.trim())) {
                    return l;
                }
            }
            return defaultLevel;
        }
    }

    private static Level minLevel = Level.INFO;
    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
    private static final SimpleDateFormat FILE_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
    private static final SimpleDateFormat CRASH_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");

    private static final Path LOGS_DIR = GamePaths.resolve("logs");
    private static final Path CRASH_DIR = GamePaths.resolve("logs", "crash-reports");
    private static final Path LATEST_LOG = LOGS_DIR.resolve("latest.log");

    private static PrintWriter fileWriter = null;
    private static final Object LOCK = new Object();
    private static final List<String> RECENT_LOGS = new LinkedList<>();
    private static final int MAX_RECENT_LOGS = 120;
    private static boolean initialized = false;

    private Logger() {}

    public static void init() {
        if (initialized) return;
        synchronized (LOCK) {
            try {
                if (!Files.exists(LOGS_DIR)) {
                    Files.createDirectories(LOGS_DIR);
                }
                if (!Files.exists(CRASH_DIR)) {
                    Files.createDirectories(CRASH_DIR);
                }

                // Rotate previous latest.log if exists and has content
                if (Files.exists(LATEST_LOG) && Files.size(LATEST_LOG) > 0) {
                    Path oldLog = LOGS_DIR.resolve("latest-old.log");
                    try {
                        Files.deleteIfExists(oldLog);
                        Files.move(LATEST_LOG, oldLog);
                    } catch (IOException ignored) {}
                }

                fileWriter = new PrintWriter(new OutputStreamWriter(
                        Files.newOutputStream(LATEST_LOG, StandardOpenOption.CREATE, StandardOpenOption.WRITE),
                        StandardCharsets.UTF_8), true);

                initialized = true;

                // Register JVM shutdown hook to flush and close
                Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                    info("System", "Shutting down logging system. Goodbye!");
                    close();
                }, "Logger-ShutdownHook"));

                // Register Default Uncaught Exception Handler
                Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
                    dumpCrashReport("Uncaught exception in thread [" + thread.getName() + "]", throwable, null);
                });

            } catch (Exception e) {
                System.err.println("[Logger] Failed to initialize file logging: " + e.getMessage());
            }
        }

        info("System", "=== Java Voxel 3D - Game Session Started ===");
        info("System", "Log file: %s", LATEST_LOG.toAbsolutePath());
    }

    public static void setMinLevel(Level level) {
        if (level != null) {
            minLevel = level;
            info("Logger", "Changed minimum log level to: %s", level.label);
        }
    }

    public static Level getMinLevel() {
        return minLevel;
    }

    public static void trace(String tag, String format, Object... args) {
        log(Level.TRACE, tag, format, args);
    }

    public static void debug(String tag, String format, Object... args) {
        log(Level.DEBUG, tag, format, args);
    }

    public static void info(String tag, String format, Object... args) {
        log(Level.INFO, tag, format, args);
    }

    public static void warn(String tag, String format, Object... args) {
        log(Level.WARN, tag, format, args);
    }

    public static void error(String tag, String format, Object... args) {
        log(Level.ERROR, tag, format, args);
    }

    public static void error(String tag, String message, Throwable t) {
        log(Level.ERROR, tag, message);
        if (t != null) {
            StringWriter sw = new StringWriter();
            t.printStackTrace(new PrintWriter(sw));
            log(Level.ERROR, tag, sw.toString());
        }
    }

    public static void fatal(String tag, String message, Throwable t) {
        log(Level.FATAL, tag, message);
        if (t != null) {
            StringWriter sw = new StringWriter();
            t.printStackTrace(new PrintWriter(sw));
            log(Level.FATAL, tag, sw.toString());
        }
    }

    private static void log(Level level, String tag, String format, Object... args) {
        if (!initialized) {
            init();
        }

        if (level.priority < minLevel.priority) {
            return;
        }

        String message;
        try {
            message = (args != null && args.length > 0) ? String.format(Locale.ROOT, format, args) : format;
        } catch (Exception e) {
            message = format + " [Formatting error: " + e.getMessage() + "]";
        }

        String timestamp = TIME_FORMAT.format(new Date());
        String threadName = Thread.currentThread().getName();
        String logEntry = String.format("[%s] [%s/%s] [%s]: %s", timestamp, threadName, level.label, tag, message);

        synchronized (LOCK) {
            // 1. Console Output
            if (level.priority >= Level.WARN.priority) {
                System.err.println(logEntry);
            } else {
                System.out.println(logEntry);
            }

            // 2. File Output
            if (fileWriter != null) {
                fileWriter.println(logEntry);
                fileWriter.flush();
            }

            // 3. In-memory buffer
            RECENT_LOGS.add(logEntry);
            if (RECENT_LOGS.size() > MAX_RECENT_LOGS) {
                RECENT_LOGS.remove(0);
            }
        }
    }

    public static List<String> getRecentLogs() {
        synchronized (LOCK) {
            return new ArrayList<>(RECENT_LOGS);
        }
    }

    public static List<String> getRecentLogs(int count) {
        synchronized (LOCK) {
            int start = Math.max(0, RECENT_LOGS.size() - count);
            return new ArrayList<>(RECENT_LOGS.subList(start, RECENT_LOGS.size()));
        }
    }

    public static void clearRecentLogs() {
        synchronized (LOCK) {
            RECENT_LOGS.clear();
        }
    }

    /**
     * Creates a detailed crash or diagnostic dump report with hardware, JVM, OpenGL and game state.
     */
    public static Path dumpCrashReport(String reason, Throwable throwable, Map<String, String> extraContext) {
        String timestamp = CRASH_DATE_FORMAT.format(new Date());
        String filename = "crash-" + timestamp + ".txt";
        Path reportFile = CRASH_DIR.resolve(filename);

        StringBuilder sb = new StringBuilder();
        sb.append("================================================================================\n");
        sb.append("                       JAVA VOXEL 3D - CRASH REPORT / DIAGNOSTIC               \n");
        sb.append("================================================================================\n\n");
        sb.append("Report Reason:    ").append(reason).append("\n");
        sb.append("Timestamp:        ").append(TIME_FORMAT.format(new Date())).append("\n\n");

        if (throwable != null) {
            sb.append("----------------------------- STACK TRACE --------------------------------------\n");
            StringWriter sw = new StringWriter();
            throwable.printStackTrace(new PrintWriter(sw));
            sb.append(sw.toString()).append("\n");
        }

        sb.append("----------------------------- SYSTEM & HARDWARE INFO ---------------------------\n");
        Runtime rt = Runtime.getRuntime();
        long maxMem = rt.maxMemory() / (1024L * 1024L);
        long totalMem = rt.totalMemory() / (1024L * 1024L);
        long freeMem = rt.freeMemory() / (1024L * 1024L);
        long usedMem = totalMem - freeMem;

        sb.append(String.format("Operating System:  %s (Version: %s, Architecture: %s)\n",
                System.getProperty("os.name"), System.getProperty("os.version"), System.getProperty("os.arch")));
        sb.append(String.format("Java Runtime:      %s (%s, version: %s)\n",
                System.getProperty("java.runtime.name"), System.getProperty("java.vendor"), System.getProperty("java.version")));
        sb.append(String.format("Virtual Machine:   %s (%s)\n",
                System.getProperty("java.vm.name"), System.getProperty("java.vm.info")));
        sb.append(String.format("CPU Cores:         %d\n", rt.availableProcessors()));
        sb.append(String.format("Heap RAM:          Used: %d MB / Allocated: %d MB / Max: %d MB\n\n",
                usedMem, totalMem, maxMem));

        if (extraContext != null && !extraContext.isEmpty()) {
            sb.append("----------------------------- GAME & WORLD CONTEXT -----------------------------\n");
            for (Map.Entry<String, String> entry : extraContext.entrySet()) {
                sb.append(String.format("%-22s: %s\n", entry.getKey(), entry.getValue()));
            }
            sb.append("\n");
        }

        sb.append("----------------------------- RECENT LOGS --------------------------------------\n");
        List<String> recent = getRecentLogs(60);
        for (String line : recent) {
            sb.append(line).append("\n");
        }
        sb.append("================================================================================\n");

        String content = sb.toString();

        // Print to stderr
        System.err.println(content);

        // Write to crash file
        try {
            if (!Files.exists(CRASH_DIR)) {
                Files.createDirectories(CRASH_DIR);
            }
            Files.writeString(reportFile, content, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
            log(Level.FATAL, "CrashReport", "Crash report saved to: %s", reportFile.toAbsolutePath());
        } catch (IOException e) {
            System.err.println("[Logger] Nie udalo sie zapisac raportu crash dump: " + e.getMessage());
        }

        return reportFile;
    }

    public static void close() {
        synchronized (LOCK) {
            if (fileWriter != null) {
                fileWriter.flush();
                fileWriter.close();
                fileWriter = null;
            }
        }
    }
}
