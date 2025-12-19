package it.torvergata.bugprediction.controllers;

import it.torvergata.bugprediction.infrastructure.git.GitRepositoryMiner;
import it.torvergata.bugprediction.infrastructure.jira.JiraExtractor;
import it.torvergata.bugprediction.models.Commit;
import it.torvergata.bugprediction.models.Release;
import it.torvergata.bugprediction.models.Ticket;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Pattern;

public class DatasetBuilder {

    private final Logger logger;
    public static final String RESULT_DIRECTORY_NAME = "results/";

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

            // Prendi metà delle release
            List<Release> datasetReleases = jiraReleases.subList(0, jiraReleases.size()/2);

            // Get a clone of all tickets proportioned to use in the training set
            List<Ticket> allTickets = getAllTicketsProportioned(jiraReleases, ticketList, projectName);
            applyFilters(allTickets, commitList);

            // CONTINUA

            //TrainingTestSetsProcessor setsProcessor = new TrainingTestSetsProcessor();

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
        releaseList.sort(Comparator.comparing(Release::getReleaseDateTime));
        for (int i = 0; i < releaseList.size(); i++){
            releaseList.get(i).setNumericID(i + 1);
        }
    }

    private List<Ticket> getAllTicketsProportioned(List<Release> jiraReleases, List<Ticket> ticketList,
                                                   String projName) throws IOException {
        List<Ticket> allTickets = new ArrayList<>();
        for (Ticket t: ticketList) {
            Ticket newTicket = t.cloneTicketAtRelease(jiraReleases.get(jiraReleases.size() - 1));
            allTickets.add(newTicket);
        }
        Ticket.proportionTickets(allTickets, jiraReleases, projName);

        return allTickets;
    }

    /**
     * Filtra i commit che hanno un ID ticket nel messaggio, impostando il ticket di un commit e l'elenco di
     * commit per ogni ticket e rimuovendo i ticket senza un commit
     * @param commitList commit da filtrare
     * @param ticketList ticket da cui ottenere gli ID
     * @return un elenco di commit che fanno riferimento a un ticket
     */
    public List<Commit> applyFilters(List<Ticket> ticketList, List<Commit> commitList) {
        List<Commit> filteredCommitList = new ArrayList<>();
        for (Commit commit : commitList) {
            String commitFullMessage = commit.getRevCommit().getFullMessage();
            for (Ticket ticket : ticketList) {
                String ticketKey = ticket.getKey();
                if (matchRegex(commitFullMessage, ticketKey)) {
                    filteredCommitList.add(commit);
                    ticket.addCommit(commit);
                    commit.setTicket(ticket);
                }
            }
        }

        // Se un ticket non ha commit significa che non è stato risolto, quindi non ci interessa
        ticketList.removeIf(ticket -> ticket.getCommitList().isEmpty());

        return filteredCommitList;
    }

    private boolean matchRegex(String s, String regex){
        Pattern pattern = Pattern.compile(regex + "\\b");
        return pattern.matcher(s).find();
    }

}
