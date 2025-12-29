package it.torvergata.bugprediction.processors.evaluation;

import it.torvergata.bugprediction.datasource.git.GitRepositoryAnalyzer;
import it.torvergata.bugprediction.enums.DatasetType;
import it.torvergata.bugprediction.enums.OutputFileType;
import it.torvergata.bugprediction.models.Commit;
import it.torvergata.bugprediction.models.ReleaseClass;
import it.torvergata.bugprediction.models.Release;
import it.torvergata.bugprediction.models.Ticket;
import it.torvergata.bugprediction.processors.labeling.Labeler;
import it.torvergata.bugprediction.processors.metrics.MetricsProcessor;
import it.torvergata.bugprediction.processors.datasets.DatasetsProcessor;
import it.torvergata.bugprediction.services.CommitService;
import it.torvergata.bugprediction.services.ReleaseService;
import it.torvergata.bugprediction.services.TicketService;
import it.torvergata.bugprediction.utils.Utils;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;

public class WalkForwardProcessor {

    private final GitRepositoryAnalyzer gitRepoAnalyzer;
    private static final Logger LOGGER = Logger.getLogger(WalkForwardProcessor.class.getName());
    private final String projectName;

    private int walkForwardIteration = 0;

    public WalkForwardProcessor(GitRepositoryAnalyzer gitRepoAnalyzer, String projectName) {
        this.gitRepoAnalyzer = gitRepoAnalyzer;
        this.projectName = projectName;
    }

    public void executeWalkForward(List<Release> datasetReleases, List<Ticket> ticketList, List<Commit> commitList, List<Ticket> allTickets) throws IOException {
        for (Release currentRelease : datasetReleases) {
            if (currentRelease.getNumericId() == 1) continue; // Salta la prima release

            Utils.printLine(LOGGER);
            logStep(currentRelease, datasetReleases.size());

            List<Release> consideringReleases = ReleaseService.getConsideringReleases(datasetReleases, currentRelease);
            List<Ticket> consideringTickets = TicketService.getConsideringTickets(ticketList, currentRelease);
            List<Commit> consideringCommits = CommitService.getConsideringCommits(commitList, currentRelease);
            List<Commit> consideringTicketedCommits = CommitService.filterAndAssignCommitsToTickets(consideringTickets, consideringCommits);

            // Saltare la release se non c'è nessun ticket
            if (consideringTickets.isEmpty()) {
                LOGGER.info("[ATTENZIONE!] Nessun ticket disponibile fino alla release " + currentRelease.getName() + ". Release saltata");
                continue; // passa alla prossima release
            }

            // Saltare la release se nessun ticket ha AV
            boolean hasTicketsWithAV = consideringTickets.stream().anyMatch(Ticket::hasAffectedVersions);
            if (!hasTicketsWithAV) {
                LOGGER.info("[ATTENZIONE!] Nessun ticket con affected versions fino alla release "
                        + currentRelease.getName() + ". Release saltata");
                continue; // passa alla prossima release
            }

            TicketService.proportionTickets(consideringTickets, consideringReleases, projectName);
            consideringTickets.sort(Comparator.comparing(Ticket::getResolutionDate));

            List<ReleaseClass> classList = gitRepoAnalyzer.extractClasses(consideringReleases, consideringCommits);

            MetricsProcessor metricsProcessor = new MetricsProcessor(consideringReleases, consideringTicketedCommits,
                    classList, gitRepoAnalyzer, projectName);
            metricsProcessor.processMetrics();

            processWalkForwardIteration(
                    consideringReleases, consideringTickets, allTickets, classList, projectName);
        }
    }

    private void processWalkForwardIteration(List<Release> consideringReleaseList,
                                             List<Ticket> consideringTicketList, List<Ticket> allTickets,
                                             List<ReleaseClass> classList, String projName) throws IOException {
        walkForwardIteration++;

        // L’ultima release non può essere utilizzata come training set, altrimenti non ci sarebbe alcuna release da usare come test set
        List<Release> trainingSetReleaseList = consideringReleaseList.stream()
                .filter(r -> r.getNumericId() < consideringReleaseList.get(consideringReleaseList.size() - 1).getNumericId())
                .toList();

        List<Ticket> trainingSetTicketList = consideringTicketList.stream()
                .filter(t -> t.getFixedVersion().getNumericId() < consideringReleaseList.get(consideringReleaseList.size() - 1).getNumericId())
                .toList();

        List<ReleaseClass> trainingSetClassList = classList.stream()
                .filter(c -> c.getRelease().getNumericId() < consideringReleaseList.get(consideringReleaseList.size() - 1).getNumericId())
                .toList();

        processTrainingSet(trainingSetReleaseList, trainingSetTicketList, trainingSetClassList, projName);

        processTestingSet(consideringReleaseList, allTickets, classList, projName);
    }

    private void processTrainingSet(List<Release> trainingSetReleaseList, List<Ticket> trainingSetTicketList,
                                    List<ReleaseClass> trainingSetClassList, String projName) throws IOException {
        String loggerString;

        // Calcola la “bugginess” usando le informazioni disponibili fino alla release corrente
        Labeler.labelClassesBuggyness(gitRepoAnalyzer, trainingSetTicketList, trainingSetClassList);

        // Costruisci il set di addestramento per la release corrente
        DatasetsProcessor.writeDataset(projName, trainingSetReleaseList, trainingSetClassList,
                walkForwardIteration, DatasetType.TRAINING, OutputFileType.ARFF);
        DatasetsProcessor.writeDataset(projName, trainingSetReleaseList, trainingSetClassList,
                walkForwardIteration, DatasetType.TRAINING, OutputFileType.CSV);

        if (trainingSetReleaseList.size() == 1) {
            loggerString = "Training set costruito sulla prima release";
        } else {
            loggerString = "Training set costruito sulle release da 1 a " + (trainingSetReleaseList.get(trainingSetReleaseList.size() - 1).getNumericId());
        }
        if (LOGGER.isLoggable(java.util.logging.Level.INFO)) {
            LOGGER.info(String.format("%s", loggerString));
        }
    }

    private void processTestingSet(List<Release> releaseList, List<Ticket> currentTicketList,
                                   List<ReleaseClass> classList, String projName) throws IOException {
        String loggerString;

        // Ottieni la release da predire
        Release predictingRelease = releaseList.get(releaseList.size() - 1);

        // Ottieni le classi da predire
        List<ReleaseClass> predictingClassList = classList.stream()
                .filter(c -> c.getRelease().getNumericId() == predictingRelease.getNumericId())
                .toList();

        // Calcola la buggyness usando tutte le informazioni fino al presente
        Labeler.labelClassesBuggyness(gitRepoAnalyzer, currentTicketList, predictingClassList);

        // Costruisci il set di test per la release da predire
        DatasetsProcessor.writeDataset(projName, releaseList, predictingClassList,
                walkForwardIteration,
                DatasetType.TESTING, OutputFileType.ARFF);
        DatasetsProcessor.writeDataset(projName, releaseList, predictingClassList,
                walkForwardIteration,
                DatasetType.TESTING, OutputFileType.CSV);


        loggerString = "Testing set costruito sulla release " + predictingRelease.getNumericId();
        if (LOGGER.isLoggable(java.util.logging.Level.INFO)) {
            LOGGER.info(String.format("%s", loggerString));
        }
    }

    private void logStep(Release currentRelease, int totalReleases) {
        int step = currentRelease.getNumericId() - 1;
        int totalSteps = totalReleases - 1;
        if (LOGGER.isLoggable(java.util.logging.Level.INFO)) {
            LOGGER.info(String.format(
                    "Passo %d su %d: elaborazione release %d (%s)",
                    step,
                    totalSteps,
                    currentRelease.getNumericId(),
                    currentRelease.getName()
            ));
        }
    }

}
