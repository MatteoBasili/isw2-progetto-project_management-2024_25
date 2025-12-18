package it.torvergata.bugprediction;

import it.torvergata.bugprediction.config.ConfigLoader;
import it.torvergata.bugprediction.config.LoggingConfig;
import it.torvergata.bugprediction.controllers.DatasetBuilder;
import it.torvergata.bugprediction.utils.Utils;

import java.util.Scanner;
import java.util.logging.Logger;

public class MLSELauncher {

    private static final Logger LOGGER = Logger.getLogger(MLSELauncher.class.getName());

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
                    exit = true;
                    LOGGER.info("Uscita dal programma. Arrivederci!");
                    break;
                default:
                    LOGGER.info("Opzione non valida. Riprova.");
            }
        }

        scanner.close();
    }

    // =========================
    // MENU
    // =========================
    private static void printMenu() {
        LOGGER.info("""
                
                ==============================
                      MLSE LAUNCHER MENU
                ==============================
                
                1. Costruisci il dataset del progetto
                0. Esci
                
                Seleziona un'opzione:""");
    }

    private static void buildDataset() {
        String projectName = ConfigLoader.loadProjectName();
        Utils.logSeparator("Costruzione del dataset di " + projectName, LOGGER);

        LOGGER.info("Avvio\n");
        DatasetBuilder datasetBuilder = new DatasetBuilder();
        int ret = datasetBuilder.build(projectName);
        if (ret == 0) {
            LOGGER.info("\nDataset costruito correttamente");
        } else {
            LOGGER.severe("\n[ERRORE] Errore durante la costruzione del dataset");
        }

        Utils.logSeparator("", LOGGER);
    }

}
