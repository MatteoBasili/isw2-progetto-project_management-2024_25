package it.torvergata.bugprediction.controllers;

import it.torvergata.bugprediction.models.ClassifierResults;
import it.torvergata.bugprediction.processors.acume.AcumeProcessor;
import it.torvergata.bugprediction.processors.weka.WekaProcessor;
import it.torvergata.bugprediction.utils.Utils;

import java.util.List;
import java.util.logging.Logger;

import static it.torvergata.bugprediction.MLSELauncher.RESULTS_DIR;

public class WekaResultsBuilder {

    private final Logger logger;
    public static final String WEKA_DIR = RESULTS_DIR + "weka/";

    public WekaResultsBuilder() {
        logger = Logger.getLogger(WekaResultsBuilder.class.getName());
    }

    /**
     * Addestra i modelli e genera i risultati per il progetto.
     * @param projectName nome del progetto
     * @return 0 se successo, 1 se errore
     */
    public int process(String projectName) {
        logger.info("Avvio\n");

        try {
            logger.info("Addestramento dei classificatori...");
            WekaProcessor wekaProcessor = new WekaProcessor(projectName);
            List<ClassifierResults> results = wekaProcessor.processClassifierResults();

            logger.info("Scrittura dei risultati...");
            wekaProcessor.writeFinalResults(projectName, results);

            logger.info("Scrittura dei file ACUME...");
            AcumeProcessor acumeProcessor = new AcumeProcessor(projectName);
            acumeProcessor.processACUMEFiles(results);

            Utils.printLine(logger);

            return 0;
        } catch (Exception e) {
            logger.severe("[ERRORE] " + e.getMessage());
            return 1;
        }
    }

}
