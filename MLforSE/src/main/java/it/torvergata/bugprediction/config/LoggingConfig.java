package it.torvergata.bugprediction.config;

import java.util.logging.*;

public final class LoggingConfig {

    private static final Level LOG_LEVEL =
            Level.parse(System.getProperty("log.level", "INFO"));

    private LoggingConfig() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static void configure() {
        Logger rootLogger = Logger.getLogger("");

        // Rimuove handler esistenti
        for (Handler handler : rootLogger.getHandlers()) {
            rootLogger.removeHandler(handler);
        }

        ConsoleHandler handler = new ConsoleHandler();
        handler.setLevel(LOG_LEVEL);
        handler.setFormatter(new Formatter() {
            @Override
            public String format(LogRecord record) {
                return record.getMessage() + System.lineSeparator();
            }
        });

        rootLogger.addHandler(handler);
        rootLogger.setLevel(LOG_LEVEL);

        silence("javax.management");
        silence("sun.management");
        silence("com.sun");
    }

    private static void silence(String loggerName) {
        Logger.getLogger(loggerName).setLevel(Level.SEVERE);
    }
}

