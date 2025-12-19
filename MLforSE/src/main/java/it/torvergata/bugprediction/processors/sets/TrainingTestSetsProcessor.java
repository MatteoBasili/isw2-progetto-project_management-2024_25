package it.torvergata.bugprediction.processors.sets;

import it.torvergata.bugprediction.enums.DatasetType;
import it.torvergata.bugprediction.enums.OutputFileType;
import it.torvergata.bugprediction.infrastructure.git.GitRepositoryMiner;
import it.torvergata.bugprediction.models.ProjectClass;
import it.torvergata.bugprediction.models.Release;
import it.torvergata.bugprediction.models.Ticket;

import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

public class TrainingTestSetsProcessor {

    public static final String NAME_OF_THIS_CLASS = TrainingTestSetsProcessor.class.getName();
    private static final Logger logger = Logger.getLogger(NAME_OF_THIS_CLASS);

    private int walkForwardIterations = 0;

    public void processWalkForwardIteration(GitRepositoryMiner gitRepoMiner, List<Release> consideringReleaseList,
                                            List<Ticket> consideringTicketList, List<Ticket> allTickets,
                                            List<ProjectClass> classList, String projName) throws IOException {
        walkForwardIterations++;

        // L’ultima release non può essere utilizzata come training set, altrimenti non ci sarebbe alcuna release da usare come test set
        List<Release> trainingSetReleaseList = consideringReleaseList.stream()
                .filter(r -> r.getNumericID() < consideringReleaseList.get(consideringReleaseList.size() - 1).getNumericID())
                .toList();

        List<Ticket> trainingSetTicketList = consideringTicketList.stream()
                .filter(t -> t.getFixedVersion().getNumericID() < consideringReleaseList.get(consideringReleaseList.size() - 1).getNumericID())
                .toList();

        List<ProjectClass> trainingSetClassList = classList.stream()
                .filter(c -> c.getRelease().getNumericID() < consideringReleaseList.get(consideringReleaseList.size() - 1).getNumericID())
                .toList();

        processTrainingSet(gitRepoMiner, trainingSetReleaseList, trainingSetTicketList, trainingSetClassList, projName);

        processTestingSet(gitRepoMiner, consideringReleaseList, allTickets, classList, projName);
    }

    private void processTrainingSet(GitRepositoryMiner gitRepoMiner, List<Release> trainingSetReleaseList, List<Ticket> trainingSetTicketList,
                                    List<ProjectClass> trainingSetClassList, String projName) throws IOException {
        String loggerString;

        // Calcola la “bugginess” usando le informazioni disponibili fino alla release corrente
        gitRepoMiner.labelClassBuggyness(trainingSetTicketList, trainingSetClassList);

        // Costruisci il set di addestramento per la release corrente
        DatasetsProcessor.writeDataset(projName, trainingSetReleaseList, trainingSetClassList,
                trainingSetReleaseList.get(trainingSetReleaseList.size() - 1).getNumericID(), DatasetType.TRAINING, OutputFileType.ARFF);
        DatasetsProcessor.writeDataset(projName, trainingSetReleaseList, trainingSetClassList,
                trainingSetReleaseList.get(trainingSetReleaseList.size() - 1).getNumericID(), DatasetType.TRAINING, OutputFileType.CSV);

        if (walkForwardIterations==1) {
            loggerString = "Training set costruito sulla prima release";
        } else {
            loggerString = "Training set costruito sulle release da 1 a " + (trainingSetReleaseList.get(trainingSetReleaseList.size() - 1).getNumericID());
        }
        logger.info("[INFO] " + loggerString);
    }

    private void processTestingSet(GitRepositoryMiner gitRepoMiner, List<Release> releaseList, List<Ticket> currentTicketList,
                                   List<ProjectClass> classList, String projName) throws IOException {
        String loggerString;

        // Ottieni la release da predire, quella successiva alla corrente
        Release predictingRelease = releaseList.get(releaseList.size() - 1);

        // Ottieni le classi da predire
        List<ProjectClass> predictingClassList = classList.stream()
                .filter(c -> c.getRelease().getNumericID() == predictingRelease.getNumericID())
                .toList();

        // Calcola la buggyness usando tutte le informazioni fino al presente
        gitRepoMiner.labelClassBuggyness(currentTicketList, predictingClassList);

        // Costruisci il set di test per la release da predire
        DatasetsProcessor.writeDataset(projName, releaseList, predictingClassList,
                predictingRelease.getNumericID() - 1,
                DatasetType.TESTING, OutputFileType.ARFF);
        DatasetsProcessor.writeDataset(projName, releaseList, predictingClassList,
                predictingRelease.getNumericID() - 1,
                DatasetType.TESTING, OutputFileType.CSV);


        loggerString = "Testing set costruito sulla release " + predictingRelease.getNumericID();
        logger.info("[INFO] " + loggerString);
    }

    public int getWalkForwardIterations() {
        return walkForwardIterations;
    }

}
