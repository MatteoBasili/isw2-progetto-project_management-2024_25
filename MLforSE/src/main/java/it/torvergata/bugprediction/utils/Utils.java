package it.torvergata.bugprediction.utils;

import java.util.logging.Logger;
import java.util.regex.Pattern;

public class Utils {

    // Costruttore privato per evitare istanziazione
    private Utils() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static void logSeparator(String title, Logger logger) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n---------------------------------------------\n");
        if (!title.isEmpty()) {
            sb.append("=== ").append(title).append(" ===\n");
        }
        sb.append("---------------------------------------------\n");
        logger.info(sb.toString());
    }

    public static void printLine(Logger logger) {
        logger.info("---------------------------------------------");
    }

    public static boolean matchRegex(String s, String regex){
        Pattern pattern = Pattern.compile(regex + "\\b");
        return pattern.matcher(s).find();
    }
}
