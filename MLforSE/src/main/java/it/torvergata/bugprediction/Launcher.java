package it.torvergata.bugprediction;

import it.torvergata.bugprediction.config.ConfigLoader;
import it.torvergata.bugprediction.config.LoggingConfig;
import it.torvergata.bugprediction.controllers.DatasetBuilder;
import it.torvergata.bugprediction.utils.Utils;

import java.util.Scanner;
import java.util.logging.Logger;

public class Launcher {

    private static final Logger LOGGER = Logger.getLogger(Launcher.class.getName());

    public static void main(String[] args) {
        // Configura logging per tutta l'applicazione
        LoggingConfig.configure();

        Scanner scanner = new Scanner(System.in);
        boolean exit = false;

        while (!exit) {
            printMenu();
            String input = scanner.nextLine();

            switch (input) {
                case "1":
                    buildDataset();
                    break;
                case "0":
                    LOGGER.info("Uscita dal programma. Arrivederci!");
                    exit = true;
                    break;
                default:
                    LOGGER.warning("[ATTENZIONE!] Opzione non valida. Riprova.");
            }
        }

        scanner.close();
    }

    // =========================
    // MENU
    // =========================
    private static void printMenu() {
        LOGGER.info("""
                
                =========================
                      LAUNCHER MENU
                =========================
                
                1. Costruisci il dataset del progetto
                0. Esci
                
                Seleziona un'opzione:""");
    }

    private static void buildDataset() {
        String projectName = ConfigLoader.loadProjectName();
        Utils.logSeparator("Costruzione del dataset di " + projectName, LOGGER);

        DatasetBuilder datasetBuilder = new DatasetBuilder();
        datasetBuilder.build(projectName);

        LOGGER.info("Dataset costruito correttamente!");
        Utils.logSeparator("", LOGGER);
    }

}
