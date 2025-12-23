package it.torvergata.bugprediction.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class ConfigLoader {

    private static final Logger LOGGER = Logger.getLogger(ConfigLoader.class.getName());
    private static final String CONFIG_FILE = "config.properties";

    // Valori di default
    private static final String DEFAULT_PROJECT = "BOOKKEEPER";
    private static final int DEFAULT_FP_WEIGHT = 1;
    private static final int DEFAULT_FN_WEIGHT = 1;

    private ConfigLoader() {
        throw new UnsupportedOperationException("Utility class");
    }


    public static String loadProjectName() {
        return loadStringProperty("PROJECT", DEFAULT_PROJECT);
    }

    public static int loadFalsePositiveWeight() {
        return loadIntProperty("FALSE_POSITIVE_WEIGHT", DEFAULT_FP_WEIGHT);
    }

    public static int loadFalseNegativeWeight() {
        return loadIntProperty("FALSE_NEGATIVE_WEIGHT", DEFAULT_FN_WEIGHT);
    }


    private static Properties loadProperties() {
        Properties properties = new Properties();

        try (InputStream is = ConfigLoader.class
                .getClassLoader()
                .getResourceAsStream(CONFIG_FILE)) {

            if (is == null) {
                LOGGER.warning(() ->
                        "[ATTENZIONE!] File di configurazione '" + CONFIG_FILE +
                                "' non trovato nel classpath. Uso valori di default.");
                return properties;
            }

            properties.load(is);

        } catch (IOException e) {
            LOGGER.log(
                    Level.WARNING,
                    "[ATTENZIONE!] Errore durante la lettura del file di configurazione. Uso valori di default.",
                    e
            );
        }

        return properties;
    }

    private static String loadStringProperty(String key, String defaultValue) {
        Properties properties = loadProperties();
        return properties.getProperty(key, defaultValue);
    }

    private static int loadIntProperty(String key, int defaultValue) {
        Properties properties = loadProperties();
        String value = properties.getProperty(key);

        if (value == null) {
            return defaultValue;
        }

        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            LOGGER.warning(() ->
                    "[ATTENZIONE!] Valore non valido per '" + key +
                            "'. Uso default: " + defaultValue);
            return defaultValue;
        }
    }

}
