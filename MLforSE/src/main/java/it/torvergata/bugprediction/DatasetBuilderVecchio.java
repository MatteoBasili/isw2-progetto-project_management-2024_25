package it.torvergata.bugprediction;

import it.torvergata.bugprediction.config.ConfigLoader;
import it.torvergata.bugprediction.exceptions.GitCloneException;
import it.torvergata.bugprediction.jira.JiraReleaseRetriever;
import it.torvergata.bugprediction.jira.JiraTicketRetriever;
import it.torvergata.bugprediction.model.Release;
import it.torvergata.bugprediction.model.Ticket;
import it.torvergata.bugprediction.utils.GitUtils;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DatasetBuilderVecchio {

    private static final Logger LOGGER = Logger.getLogger(DatasetBuilderVecchio.class.getName());

    public static void main(String[] args) throws GitCloneException {
        String project = ConfigLoader.loadProjectName();

        LOGGER.log(Level.INFO, "Starting dataset building for project {0}", project);

        // 1. Clone Git repository (if not already cloned)
        String repoPath = GitUtils.cloneRepository(project);

        // 2. Retrieve releases
        JiraReleaseRetriever releaseRetriever = new JiraReleaseRetriever(project);
        List<Release> releases = releaseRetriever.retrieveReleases();

        if (releases.isEmpty()) {
            return;
        }

        // 3. Retrieve tickets
        JiraTicketRetriever ticketRetriever = new JiraTicketRetriever(project);
        List<Ticket> tickets = ticketRetriever.retrieveTickets();

        // 4. Retrieve commits and link with JIRA tickets
        /*GitCommitRetriever commitRetriever = new GitCommitRetriever(project, repoPath, tickets);
        commitRetriever.retrieveCommits();

        MetricsCalculator calculator = new MetricsCalculator();

        for (Release release : releases) {
            List<Commit> releaseCommits = ... // filtra i commit che appartengono alla release
            Set<String> buggyCommits = ...    // hash commit legati a bug
            Map<String, FileMetrics> metrics = calculator.computeMetrics(release, releaseCommits, buggyCommits, repoPath);

            // salva le metriche su CSV
        }*/

        LOGGER.log(Level.INFO, "Dataset completed for project {0}", project);
    }
}
