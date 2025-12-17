package it.torvergata.bugprediction.config;

import java.util.logging.*;

public final class LoggingConfig {

    // Costruttore privato: classe utility, non istanziabile
    private LoggingConfig() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Configura il logger root per tutta l'applicazione:
     * - scrive su stdout
     * - output pulito (solo messaggio)
     * - flush immediato
     * - livello ALL
     */
    public static void configure() {
        Logger rootLogger = Logger.getLogger("");

        // Rimuove handler di default (ConsoleHandler su stderr)
        for (Handler handler : rootLogger.getHandlers()) {
            rootLogger.removeHandler(handler);
        }

        // Handler custom su stdout con flush immediato
        Handler stdoutHandler = new StreamHandler(System.out, new Formatter() {
            @Override
            public String format(LogRecord record) {
                return record.getMessage() + System.lineSeparator();
            }
        }) {
            @Override
            public synchronized void publish(LogRecord record) {
                super.publish(record);
                flush();
            }
        };

        stdoutHandler.setLevel(Level.ALL);
        rootLogger.addHandler(stdoutHandler);
        rootLogger.setLevel(Level.ALL);
    }
}
