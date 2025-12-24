package it.torvergata.bugprediction;

import it.torvergata.bugprediction.config.ConfigLoader;
import it.torvergata.bugprediction.config.LoggingConfig;
import it.torvergata.bugprediction.controllers.DatasetsBuilder;
import it.torvergata.bugprediction.controllers.WekaResultsBuilder;
import it.torvergata.bugprediction.utils.Utils;

import java.util.Scanner;
import java.util.logging.Logger;

public class MLSELauncher {

    private static final Logger LOGGER = Logger.getLogger(MLSELauncher.class.getName());

    public static final String RESULTS_DIR = "results/";

    public static void main(String[] args) {
        setupSystem();

        Scanner scanner = new Scanner(System.in);
        boolean exit = false;

        while (!exit) {
            printMenu();
            String input = scanner.nextLine();

            switch (input) {
                case "1":
                    buildDatasets();
                    break;
                case "2":
                    runWekaProcessing();
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
                
                1. Costruisci i dataset del progetto
                2. Addestra i modelli e genera i risultati con WEKA
                0. Esci
                
                Seleziona un'opzione:""");
    }

    private static void buildDatasets() {
        String projectName = ConfigLoader.loadProjectName();
        Utils.logSeparator("Costruzione dei dataset di " + projectName, LOGGER);

        DatasetsBuilder datasetsBuilder = new DatasetsBuilder();
        int ret = datasetsBuilder.build(projectName);
        if (ret == 0) {
            LOGGER.info("\nDataset costruiti correttamente");
        } else {
            LOGGER.severe("\n[ERRORE] Errore durante la costruzione dei dataset");
        }

        Utils.logSeparator("", LOGGER);
    }

    private static void runWekaProcessing() {
        String projectName = ConfigLoader.loadProjectName();
        Utils.logSeparator("Addestramento dei modelli e generazione dei risultati con WEKA per " + projectName, LOGGER);

        WekaResultsBuilder wekaBuilder = new WekaResultsBuilder();
        int ret = wekaBuilder.process(projectName);

        if (ret == 0) {
            LOGGER.info("\nElaborazione con WEKA completata correttamente");
        } else {
            LOGGER.severe("\n[ERRORE] Errore durante l'elaborazione con WEKA");
        }

        Utils.logSeparator("", LOGGER);
    }

    private static void setupSystem() {
        // Forza l’uso dell’implementazione Java pura --> Niente tentativi nativi
        System.setProperty("com.github.fommil.netlib.BLAS", "com.github.fommil.netlib.F2jBLAS");
        System.setProperty("com.github.fommil.netlib.LAPACK", "com.github.fommil.netlib.F2jLAPACK");
        System.setProperty("com.github.fommil.netlib.ARPACK", "com.github.fommil.netlib.F2jARPACK");

        // Configura logging per tutta l'applicazione
        LoggingConfig.configure();
    }

}
