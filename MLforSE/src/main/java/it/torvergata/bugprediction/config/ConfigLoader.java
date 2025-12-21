package it.torvergata.bugprediction.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class ConfigLoader {

    private static final Logger LOGGER = Logger.getLogger(ConfigLoader.class.getName());

    private static final String CONFIG_FILE = "config.properties";
    private static final String PROJECT_KEY = "PROJECT";
    private static final String DEFAULT_PROJECT = "BOOKKEEPER";

    // Costruttore privato per evitare istanziazione
    private ConfigLoader() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Carica il nome del progetto dal file di configurazione.
     * In caso di errore, utilizza il valore di default.
     *
     * @return nome del progetto
     */
    public static String loadProjectName() {
        Properties properties = new Properties();

        try (InputStream is = ConfigLoader.class
                .getClassLoader()
                .getResourceAsStream(CONFIG_FILE)) {

            if (is == null) {
                LOGGER.warning(() ->
                        "[ATTENZIONE!] File di configurazione '" + CONFIG_FILE +
                                "' non trovato nel classpath. Uso valore di default: " +
                                PROJECT_KEY + " = " +
                                DEFAULT_PROJECT);
                return DEFAULT_PROJECT;
            }

            properties.load(is);
            return properties.getProperty(PROJECT_KEY, DEFAULT_PROJECT);

        } catch (IOException e) {
            LOGGER.log(
                    Level.WARNING,
                    "[ATTENZIONE!] Errore durante la lettura del file di configurazione. Uso valore di default: "
                            + PROJECT_KEY + " = "
                            + DEFAULT_PROJECT,
                    e
            );
            return DEFAULT_PROJECT;
        }
    }
}
