package it.torvergata.bugprediction.utils;

import java.util.logging.Logger;

public class Utils {

    // Costruttore privato per evitare istanziazione
    private Utils() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static void logSeparator(String title,  Logger logger) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n---------------------------------------------\n");
        if (!title.isEmpty()) {
            sb.append("=== ").append(title).append(" ===\n");
        }
        sb.append("---------------------------------------------\n");
        logger.info(sb.toString());
    }
}
