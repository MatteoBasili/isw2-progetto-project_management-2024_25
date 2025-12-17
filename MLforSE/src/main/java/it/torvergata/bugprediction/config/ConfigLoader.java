package it.torvergata.bugprediction.config;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ConfigLoader {

    private static final Logger LOGGER = Logger.getLogger(ConfigLoader.class.getName());
    private static final String CONFIG_FILE = "config.properties";

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
        String PROJECT_KEY = "PROJECT";
        String DEFAULT_PROJECT = "BOOKKEEPER";
        Properties properties = new Properties();

        try (FileInputStream fis = new FileInputStream(CONFIG_FILE)) {
            properties.load(fis);
            return properties.getProperty(PROJECT_KEY, DEFAULT_PROJECT);
        } catch (IOException e) {
            LOGGER.log(
                    Level.CONFIG,
                    "Impossibile leggere il file config.properties. Verrà utilizzato il valore di default PROJECT = BOOKKEEPER",
                    e
            );
            return DEFAULT_PROJECT;
        }
    }
}
