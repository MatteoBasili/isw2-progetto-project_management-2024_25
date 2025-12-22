package it.torvergata.bugprediction.utils;

import java.util.logging.Level;
import java.util.logging.Logger;

public class Utils {

    private Utils() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static void logSeparator(String title, Logger logger) {
        if (!logger.isLoggable(Level.INFO)) {
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("---------------------------------------------")
                .append(System.lineSeparator());

        if (!title.isEmpty()) {
            sb.append("=== ").append(title).append(" ===")
                    .append(System.lineSeparator());
        }

        sb.append("---------------------------------------------");

        logger.info(sb.toString());
    }

    public static void printLine(Logger logger) {
        if (logger.isLoggable(Level.INFO)) {
            logger.info("---------------------------------------------");
        }
    }
}
