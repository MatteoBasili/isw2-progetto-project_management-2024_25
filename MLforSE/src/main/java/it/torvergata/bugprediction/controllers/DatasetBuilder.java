package it.torvergata.bugprediction.controllers;

import it.torvergata.bugprediction.infrastructure.git.GitRepositoryMiner;
import it.torvergata.bugprediction.infrastructure.jira.JiraExtractor;
import it.torvergata.bugprediction.models.Commit;
import it.torvergata.bugprediction.models.Release;
import it.torvergata.bugprediction.models.Ticket;

import java.util.List;
import java.util.logging.Logger;

public class DatasetBuilder {

    private final Logger logger;

    public DatasetBuilder() {
        logger = Logger.getLogger(DatasetBuilder.class.getName());
    }

    /**
     * Costruisce il dataset del progetto.
     * @param projectName nome del progetto
     * @return 0 se successo, 1 se errore
     */
    public int build(String projectName){
        GitRepositoryMiner gitRepoMiner = null;
        try {
            logger.info("Clonazione del repository...");
            gitRepoMiner = new GitRepositoryMiner(projectName);

            logger.info("Estrazione delle release...");
            JiraExtractor jiraExtractor = new JiraExtractor(projectName);
            List<Release> jiraReleases = jiraExtractor.extractReleases(gitRepoMiner);

            logger.info("Estrazione dei commit...");
            List<Commit> commitList = gitRepoMiner.extractCommits(jiraReleases);

            logger.info("Estrazione dei ticket...");
            List<Ticket> ticketList = jiraExtractor.extractTickets(jiraReleases);

            setReleasesNumericID(jiraReleases);

            // CONTINUA

            return 0;

        } catch (Exception e) {
            logger.severe("[ERRORE] " + e.getMessage());
            return 1;
        } finally {
            if (gitRepoMiner != null) {
            gitRepoMiner.close(); // chiudi Git e Repository
            }
        }
    }

    private void setReleasesNumericID(List<Release> releaseList) {
        for (int i = 0; i < releaseList.size(); i++){
            releaseList.get(i).setNumericID(i + 1);
        }
    }
}
