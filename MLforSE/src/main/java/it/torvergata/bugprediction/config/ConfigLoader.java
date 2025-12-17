package it.torvergata.bugprediction.config;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ConfigLoader {

    private static final Logger LOGGER = Logger.getLogger(ConfigLoader.class.getName());

    public static String loadProjectName() {
        Properties properties = new Properties();
        String project;
        try (FileInputStream fis = new FileInputStream("config.properties")) {
            properties.load(fis);
            project = properties.getProperty("PROJECT", "BOOKKEEPER");
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Unable to read config.properties. Using default PROJECT: BOOKKEEPER", e);
            project = "BOOKKEEPER";
        }
        return project;
    }
}
