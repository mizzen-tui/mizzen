package mizzen.util;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Logging {

    // No logging library is a dependency - not to build, not to run. Lines go to a Backend: the
    // file mizzen.log unless the app chooses another - its own, or one of the ready-made ones below;
    // log4j and SLF4J are found by name, so only an app that uses them needs them.

    /** the log file's name unless the app chooses another backend: in the folder the app runs in */
    public static final String DEFAULT_LOG_FILE = "mizzen.log";

    /**
     * Where log lines go. Ready-made: file(..) - the default, mizzen.log - javaLogging(), log4j(),
     * slf4j(). Or the app's own, one line:
     * <pre>
     *     Logging.setBackend(Logging.slf4j());                                  // SLF4J's logger "mizzen"
     *     Logging.setBackend((thread, msg) -> System.err.println(thread + ": " + msg));
     * </pre>
     */
    @FunctionalInterface
    public interface Backend {
        /** one line, from the named thread */
        void line(String thread, String msg);

        /** when another backend takes over, and when the app ends */
        default void close() {
        }
    }

    static boolean enabled = false;
    static Object mutex = new Object();
    private static Backend backend;              // null until the first line, then the default file
    private static boolean closeOnExit;                  // the shutdown hook that closes it is in

    static {
        fromSystemProperties();
    }

    /**
     * The log set from system properties - each calls the method named, once, when Logging is first
     * used; the app's own calls after that win:
     * <pre>
     *     mizzen.log.enabled = true | false                   setEnabled(..)
     *     mizzen.log.file    = a path (a name alone: in the   setLogFile(..)
     *                          folder the app runs in)
     *     mizzen.log.backend = file | java | log4j | slf4j    setBackend(file(..) / javaLogging() / log4j() / slf4j())
     *
     *     java -Dmizzen.log.enabled=true -Dmizzen.log.backend=slf4j -jar app.jar
     * </pre>
     * Set them with -D on the java command line (or JAVA_TOOL_OPTIONS). A property in a pom.xml reaches
     * the app only when the pom passes it on as a system property: exec-maven-plugin's
     * &lt;systemProperties&gt; (mvn exec:java), surefire's &lt;systemPropertyVariables&gt; (tests).
     * A bad value fails fast, saying what is allowed.
     */
    static void fromSystemProperties() {
        String enabledValue = System.getProperty("mizzen.log.enabled");
        if (enabledValue != null) {
            setEnabled(Boolean.parseBoolean(enabledValue.trim()));
        }
        String file = System.getProperty("mizzen.log.file");
        String backendName = System.getProperty("mizzen.log.backend");
        if (backendName != null && !backendName.trim().equals("file")) {
            switch (backendName.trim()) {
            case "java":
                setBackend(javaLogging());
                break;
            case "log4j":
                setBackend(log4j());
                break;
            case "slf4j":
                setBackend(slf4j());
                break;
            default:
                throw new IllegalArgumentException("-Dmizzen.log.backend=" + backendName + ": use file, java, log4j or slf4j");
            }
            if (file != null) {
                throw new IllegalArgumentException("-Dmizzen.log.file=" + file + " goes with -Dmizzen.log.backend=file (or no mizzen.log.backend), not " + backendName);
            }
        } else if (file != null) {
            setLogFile(file.trim());
        }
    }

    /** where the lines go from now on; the one before is closed */
    public static void setBackend(Backend newBackend) {
        synchronized (mutex) {
            if (backend != null) {
                backend.close();
            }
            backend = newBackend;
        }
    }

    /** lines to this file, e.g. "/tmp/myapp.log"; a name alone goes in the folder the app runs in */
    public static void setLogFile(String fileName) {
        setBackend(file(fileName));
    }

    /** a backend: the file (appended to); a name alone goes in the folder the app runs in */
    public static Backend file(String fileName) {
        return new FileBackend(Paths.get(System.getProperty("user.dir")).resolve(fileName));
    }

    /** a backend: java.util.logging's logger "mizzen", at INFO - part of Java, nothing to add */
    public static Backend javaLogging() {
        java.util.logging.Logger logger = java.util.logging.Logger.getLogger("mizzen");
        return (thread, msg) -> logger.info(msg);
    }

    /**
     * A backend: log4j 2's logger "mizzen", at INFO. Found by name: the library does not depend
     * on log4j. Fails fast, saying what to add to the pom, if the app doesn't have it.
     */
    public static Backend log4j() {
        return byName("log4j 2", "org.apache.logging.log4j.LogManager", "org.apache.logging.log4j.Logger",
                "    <dependency>\n"
              + "        <groupId>org.apache.logging.log4j</groupId>\n"
              + "        <artifactId>log4j-core</artifactId>     <!-- brings log4j-api -->\n"
              + "        <version>2.x</version>\n"
              + "    </dependency>");
    }

    /**
     * A backend: SLF4J's logger "mizzen", at INFO - and through it Logback, log4j or whatever the
     * app binds SLF4J to. Found by name: the library does not depend on SLF4J. Fails fast, saying what
     * to add to the pom, if the app doesn't have it.
     */
    public static Backend slf4j() {
        return byName("SLF4J", "org.slf4j.LoggerFactory", "org.slf4j.Logger",
                "    <dependency>\n"
              + "        <groupId>org.slf4j</groupId>\n"
              + "        <artifactId>slf4j-api</artifactId>\n"
              + "        <version>...</version>\n"
              + "    </dependency>\n"
              + "    plus a binding that writes the lines, e.g. ch.qos.logback:logback-classic");
    }

    // a logger library's logger "mizzen", found and called by reflection: factory.getLogger("mizzen").info(msg)
    private static Backend byName(String library, String factoryClass, String loggerClass, String pomEntry) {
        try {
            Object logger = Class.forName(factoryClass).getMethod("getLogger", String.class).invoke(null, "mizzen");
            Method info = Class.forName(loggerClass).getMethod("info", String.class);
            return (thread, msg) -> {                    // the logger adds the thread and time itself
                try {
                    info.invoke(logger, msg);
                } catch (ReflectiveOperationException e) {
                    System.out.println(e.toString());
                }
            };
        } catch (ClassNotFoundException | LinkageError notThere) {
            throw new IllegalStateException(library + " is not on the classpath, so Mizzen's log can't go there."
                    + " Add it to your pom.xml:\n" + pomEntry + "\nor keep the default log file (mizzen.log).", notThere);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(library + " was found but could not be used: " + e, e);
        }
    }

    /** the file the lines go to, while they go to a file (the default) */
    public static Path getLogFile() {
        synchronized (mutex) {
            return backend instanceof FileBackend ? ((FileBackend) backend).path
                    : Paths.get(System.getProperty("user.dir"), DEFAULT_LOG_FILE);
        }
    }

    public static void setEnabled(boolean enabled) {
        Logging.enabled = enabled;
    }

    public static void setEnabledFromArgs(String args[]) {
        VeryBasicArgsHelper.onNamedBooleanArg(args,"LoggingEnabled",(value)-> setEnabled(value));
    }

    public static boolean isEnabled() {
        return Logging.enabled ;
    }

    public static void info(String msg) {
        if (enabled) {
            appendText(msg);
        }
    }

    /** logs msg even when logging is off - without switching it on (other threads keep logging, or not, as set) */
    public static void forceLogInfo(String msg) {
        appendText(msg);
    }

    private static void appendText(String msg) {
        String thread = Thread.currentThread().getName();
        synchronized (mutex) {
            if (backend == null) {
                backend = file(DEFAULT_LOG_FILE);
            }
            if (!closeOnExit) {
                closeOnExit = true;
                Runtime.getRuntime().addShutdownHook(new Thread(() -> setBackend(null), "Logging-close"));
            }
            backend.line(thread, msg);
        }
    }

    /**
     * The default backend: a file, opened on the first line and kept open - opening, appending to
     * and closing it for every line made logging slow - and flushed each line, so the log is complete
     * up to a crash.
     */
    private static class FileBackend implements Backend {
        final Path path;
        private Writer log;

        FileBackend(Path path) {
            this.path = path;
        }

        @Override
        public void line(String thread, String msg) {
            try {
                if (log == null) {
                    boolean isNew = !Files.exists(path);
                    log = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(path.toFile(), true), StandardCharsets.UTF_8));
                    if (isNew) {
                        log.write("Mizzen log\n\n");
                    }
                }
                log.write(thread + ":  " + msg + "\n");
                log.flush();
            } catch (IOException e) {
                // exception handling left as an exercise for the reader
                System.out.println(e.toString());
            }
        }

        @Override
        public void close() {
            try {
                if (log != null) {
                    log.close();
                }
            } catch (IOException e) {
                System.out.println(e.toString());
            }
            log = null;
        }
    }

    public static void main(String[] args) {

        Logging.setEnabled(true);

        Logging.info("here is a test");

    }

    public static class LogOnInterval {
        ThrottledRunnable throttled;

        public boolean isEnabled() {
            return Logging.isEnabled();
        }

        public LogOnInterval(long interval) {
            super();
            this.throttled = new ThrottledRunnable(interval);
        }

        public LogOnInterval() {
            this(5000);
        }

        public void info(String msg) {
            throttled.run(()-> Logging.info(msg));
        }

    }

}
