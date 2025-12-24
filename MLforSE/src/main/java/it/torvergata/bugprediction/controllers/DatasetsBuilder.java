package it.torvergata.bugprediction.controllers;

import it.torvergata.bugprediction.datasource.git.GitRepositoryAnalyzer;
import it.torvergata.bugprediction.datasource.jira.JiraClient;
import it.torvergata.bugprediction.models.Commit;
import it.torvergata.bugprediction.models.Release;
import it.torvergata.bugprediction.models.Ticket;
import it.torvergata.bugprediction.processors.evaluation.WalkForwardProcessor;
import it.torvergata.bugprediction.services.CommitService;
import it.torvergata.bugprediction.services.ReleaseService;
import it.torvergata.bugprediction.services.TicketService;
import it.torvergata.bugprediction.utils.Utils;

import java.util.List;
import java.util.logging.Logger;

import static it.torvergata.bugprediction.MLSELauncher.RESULTS_DIR;

public class DatasetsBuilder {

    private final Logger logger;
    public static final String DATASETS_DIR = RESULTS_DIR + "datasets/";

    public DatasetsBuilder() {
        logger = Logger.getLogger(DatasetsBuilder.class.getName());
    }

    /**
     * Costruisce i dataset del progetto.
     * @param projectName nome del progetto
     * @return 0 se successo, 1 se errore
     */
    public int build(String projectName) {
        logger.info("Avvio\n");
        GitRepositoryAnalyzer gitRepoAnalyzer = null;

        try {
            logger.info("Clonazione del repository...");
            gitRepoAnalyzer = new GitRepositoryAnalyzer(projectName);

            logger.info("Estrazione delle release...");
            JiraClient jiraClient = new JiraClient(projectName);
            List<Release> jiraReleases = jiraClient.extractReleases(gitRepoAnalyzer);

            logger.info("Estrazione dei commit...");
            List<Commit> commitList = gitRepoAnalyzer.extractCommits(jiraReleases);

            logger.info("Estrazione dei ticket...");
            List<Ticket> ticketList = jiraClient.extractTickets(jiraReleases);

            // Imposta l'id numerico alle release
            ReleaseService.setReleasesNumericID(jiraReleases);

            // Prendi metà delle release
            List<Release> datasetReleases = ReleaseService.getFirstHalfOfReleases(jiraReleases);

            // Ottieni un clone di tutti i ticket secondo la logica di Proportion
            List<Ticket> allTickets = TicketService.getAllTicketsProportioned(jiraReleases, ticketList, projectName);

            // Filtra i commit rilevanti e li collega ai ticket, eliminando i ticket senza commit
            CommitService.filterAndAssignCommitsToTickets(allTickets, commitList);

            logger.info("Avvio del walk forward per costruire i set di addestramento e di test...");
            WalkForwardProcessor walkForwardProcessor = new WalkForwardProcessor(
                    gitRepoAnalyzer,
                    projectName
            );
            walkForwardProcessor.executeWalkForward(datasetReleases, ticketList, commitList, allTickets);

            Utils.printLine(logger);

            return 0;

        } catch (Exception e) {
            logger.severe("[ERRORE] " + e.getMessage());
            return 1;
        } finally {
            if (gitRepoAnalyzer != null) {
                gitRepoAnalyzer.close(); // chiudi Git e Repository
            }
        }
    }

}
