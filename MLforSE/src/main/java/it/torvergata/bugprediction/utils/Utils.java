package it.torvergata.bugprediction.utils;

import java.util.logging.Level;
import java.util.logging.Logger;

public class Utils {

    private static final String SEPARATOR = "-".repeat(80);

    private Utils() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static void logSeparator(String title, Logger logger) {
        if (!logger.isLoggable(Level.INFO)) {
            return;
        }

        StringBuilder sb = new StringBuilder();

        if (!title.isEmpty()) {
            // calcola la lunghezza necessaria per la linea sopra/sotto il titolo
            int lineLength = title.length() + 8;  // 8 = "=== " + " ==="
            String dynamicLine = "-".repeat(lineLength);

            sb.append(dynamicLine).append(System.lineSeparator());
            sb.append("=== ").append(title).append(" ===").append(System.lineSeparator());
            sb.append(dynamicLine);
        } else {
            sb.append(SEPARATOR);
        }

        logger.info(sb.toString());
    }

    public static void printLine(Logger logger) {
        if (logger.isLoggable(Level.INFO)) {
            logger.info(SEPARATOR);
        }
    }
}
