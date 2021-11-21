package launcher.helper;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import launcher.Launcher;
import launcher.LauncherAPI;

public final class LogHelper {
    @LauncherAPI public static final String DEBUG_PROPERTY = "launcher.debug";

    // Output settings
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm:ss", Locale.US);
    private static final AtomicBoolean DEBUG_ENABLED = new AtomicBoolean(Boolean.getBoolean(DEBUG_PROPERTY));
    private static final Set<Output> OUTPUTS = Collections.newSetFromMap(new ConcurrentHashMap<>(2));
    private static final Output STD_OUTPUT;

    private LogHelper() {
    }

    @LauncherAPI
    public static void addOutput(Output output) {
        OUTPUTS.add(Objects.requireNonNull(output, "output"));
    }

    @LauncherAPI
    public static void addOutput(Path file) throws IOException {
        addOutput(IOHelper.newWriter(file, true));
    }

    @LauncherAPI
    public static void addOutput(Writer writer) throws IOException {
        addOutput(new WriterOutput(writer));
    }

    @LauncherAPI
    public static void debug(String message) {
        if (isDebugEnabled()) {
            log(Level.DEBUG, message, false);
        }
    }

    @LauncherAPI
    public static void debug(String format, Object... args) {
        debug(String.format(format, args));
    }

    @LauncherAPI
    public static void error(Throwable exc) {
        error(isDebugEnabled() ? toString(exc) : exc.toString());
    }

    @LauncherAPI
    public static void error(String message) {
        log(Level.ERROR, message, false);
    }

    @LauncherAPI
    public static void error(String format, Object... args) {
        error(String.format(format, args));
    }

    @LauncherAPI
    public static void info(String message) {
        log(Level.INFO, message, false);
    }

    @LauncherAPI
    public static void info(String format, Object... args) {
        info(String.format(format, args));
    }

    @LauncherAPI
    public static boolean isDebugEnabled() {
        return DEBUG_ENABLED.get();
    }

    @LauncherAPI
    public static void setDebugEnabled(boolean debugEnabled) {
        DEBUG_ENABLED.set(debugEnabled);
    }

    @LauncherAPI
    public static void log(Level level, String message, boolean sub) {
        String dateTime = DATE_TIME_FORMATTER.format(LocalDateTime.now());
        println(formatLog(level, message, dateTime, sub));
    }

    @LauncherAPI
    public static void printVersion(String product) {
        println(formatVersion(product));
    }

    @LauncherAPI
    public static synchronized void println(String message) {
        for (Output output : OUTPUTS) {
            output.println(message);
        }
    }

    @LauncherAPI
    public static boolean removeOutput(Output output) {
        return OUTPUTS.remove(output);
    }

    @LauncherAPI
    public static boolean removeStdOutput() {
        return removeOutput(STD_OUTPUT);
    }

    @LauncherAPI
    public static void subDebug(String message) {
        if (isDebugEnabled()) {
            log(Level.DEBUG, message, true);
        }
    }

    @LauncherAPI
    public static void subDebug(String format, Object... args) {
        subDebug(String.format(format, args));
    }

    @LauncherAPI
    public static void subInfo(String message) {
        log(Level.INFO, message, true);
    }

    @LauncherAPI
    public static void subInfo(String format, Object... args) {
        subInfo(String.format(format, args));
    }

    @LauncherAPI
    public static void subWarning(String message) {
        log(Level.WARNING, message, true);
    }

    @LauncherAPI
    public static void subWarning(String format, Object... args) {
        subWarning(String.format(format, args));
    }

    @LauncherAPI
    public static String toString(Throwable exc) {
        try (StringWriter sw = new StringWriter()) {
            try (PrintWriter pw = new PrintWriter(sw)) {
                exc.printStackTrace(pw);
            }
            return sw.toString();
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }

    @LauncherAPI
    public static void warning(String message) {
        log(Level.WARNING, message, false);
    }

    @LauncherAPI
    public static void warning(String format, Object... args) {
        warning(String.format(format, args));
    }

    private static String formatLog(Level level, String message, String dateTime, boolean sub) {
        if (sub) {
            message = ' ' + message;
        }
        return dateTime + " [" + level.name + "] " + message;
    }

    private static String formatVersion(String product) {
        return String.format("sashok724's %s v%s (build #%s)", product, Launcher.VERSION, Launcher.BUILD);
    }

    static {
        // Add std writer
        STD_OUTPUT = System.out::println;
        addOutput(STD_OUTPUT);
    }

    @LauncherAPI
    @FunctionalInterface
    public interface Output {
        void println(String message);
    }

    @LauncherAPI
    public enum Level {
        DEBUG("DEBUG"), INFO("INFO"), WARNING("WARN"), ERROR("ERROR");
        public final String name;

        Level(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private static class WriterOutput implements Output, AutoCloseable {
        private final Writer writer;

        private WriterOutput(Writer writer) {
            this.writer = writer;
        }

        @Override
        public void close() throws IOException {
            writer.close();
        }

        @Override
        public void println(String message) {
            try {
                writer.write(message + System.lineSeparator());
                writer.flush();
            } catch (IOException ignored) {
                // Do nothing?
            }
        }
    }
}
