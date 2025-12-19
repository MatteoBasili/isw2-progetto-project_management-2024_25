package it.torvergata.bugprediction.utils;

import java.io.FileWriter;
import java.io.IOException;
import java.util.logging.Logger;

public class FileWriterUtils {
    private FileWriterUtils() {
    }

    public static void flushAndCloseFW(FileWriter fileWriter, Logger logger, String className) {
        try {
            fileWriter.flush();
            fileWriter.close();
        } catch (IOException e) {
            logger.info("Errore in " + className + " durante il flush/la chiusura del fileWriter !!!");
        }
    }
}
