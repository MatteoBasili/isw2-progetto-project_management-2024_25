package it.torvergata.bugprediction.controllers;

import it.torvergata.bugprediction.infrastructure.git.GitRepositoryMiner;
import it.torvergata.bugprediction.infrastructure.jira.JiraExtractor;
import it.torvergata.bugprediction.models.Commit;
import it.torvergata.bugprediction.models.ProjectClass;
import it.torvergata.bugprediction.models.Release;
import it.torvergata.bugprediction.models.Ticket;
import it.torvergata.bugprediction.processors.metrics.MetricsProcessor;
import it.torvergata.bugprediction.processors.sets.TrainingTestSetsProcessor;
import it.torvergata.bugprediction.utils.Utils;

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
     * Costruisce i dataset del progetto.
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

            // Ottieni un clone di tutti i ticket proporzionati per l'uso nel training set
            List<Ticket> allTickets = getAllTicketsProportioned(jiraReleases, ticketList, projectName);
            applyFilters(allTickets, commitList);

            logger.info("Avvio del walk forward per costruire i set di addestramento e di test...");
            TrainingTestSetsProcessor trainingTestSetsProcessor = new TrainingTestSetsProcessor();
            for (Release currentRelease: datasetReleases){
                // Salta la prima release
                if(currentRelease.getNumericID() == 1)
                    continue;

                Utils.printLine(logger);

                // Log per indicare il passo corrente
                int step = currentRelease.getNumericID() - 1; // perché saltiamo la prima
                int totalSteps = datasetReleases.size() - 1;  // totale release da processare
                logger.info("Passo " + step + " su " + totalSteps + ": elaborazione release "
                        + currentRelease.getNumericID() + " (" + currentRelease.getReleaseName() + ")\n");

                List<Release> consideringReleases = getConsideringReleases(datasetReleases, currentRelease);
                List<Ticket> consideringTickets = getConsideringTickets(ticketList, currentRelease);
                List<Commit> consideringCommits = getConsideringCommits(commitList, currentRelease);
                List<Commit> consideringTicketedCommits = applyFilters(consideringTickets, consideringCommits);

                // Regola le informazioni dei ticket impostando le loro IV con proporzione
                Ticket.proportionTickets(consideringTickets, consideringReleases, projectName);
                consideringTickets.sort(Comparator.comparing(Ticket::getResolutionDate));

                logger.info("Estrazione delle classi toccate...");
                // Utilizza l'intero elenco di commit per non perdere l'ultimo commit di una release per leggere le relative classi
                List<ProjectClass> classList = gitRepoMiner.extractClasses(consideringReleases, consideringCommits);

                logger.info("Estrazione delle metriche...\n");
                MetricsProcessor metricsProcessor = new MetricsProcessor(consideringReleases, consideringTicketedCommits,
                        classList, gitRepoMiner, projectName);
                metricsProcessor.processMetrics();

                trainingTestSetsProcessor.processWalkForwardIteration(gitRepoMiner, consideringReleases, consideringTickets, allTickets, classList, projectName);
            }

            Utils.printLine(logger);

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

    private List<Release> getConsideringReleases(List<Release> jiraReleases, Release currentRelease) {
        return jiraReleases
                .stream()
                .filter(r-> r.getNumericID() <= currentRelease.getNumericID())
                .toList();
    }

    /**
     * L'elenco dei ticket da considerare in una determinata release è composto dalla visualizzazione dei ticket
     * che erano disponibili in quella release
     * @param ticketList tutti i ticket disponibili
     * @param currentRelease la release da considerare come punto di vista dei ticket
     * @return ticket con OV <= currentRelease
     */
    private List<Ticket> getConsideringTickets(List<Ticket> ticketList, Release currentRelease) {
        List<Ticket> consideringTicketList = ticketList
                .stream()
                .filter(t -> t.getOpeningVersion().getNumericID() <= currentRelease.getNumericID())
                .toList();

        List<Ticket> returningTicketList = new ArrayList<>();

        for (Ticket ticket: consideringTicketList) {
            Ticket newTicket = ticket.cloneTicketAtRelease(currentRelease);
            if (newTicket == null) continue;
            returningTicketList.add(newTicket);
        }

        return returningTicketList;
    }

    private List<Commit> getConsideringCommits(List<Commit> commitList, Release currentRelease) {
        List<Commit> consideringCommitList = commitList
                .stream()
                .filter(c -> c.getRelease().getNumericID() <= currentRelease.getNumericID())
                .toList();

        List<Commit> returningCommitList = new ArrayList<>();

        for (Commit commit: consideringCommitList) {
            Commit newCommit = commit.cloneCommitAtRelease(commit.getRelease());
            returningCommitList.add(newCommit);
        }

        return returningCommitList;
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
