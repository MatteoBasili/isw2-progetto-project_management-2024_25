package it.torvergata.bugprediction.processors.weka;

import it.torvergata.bugprediction.models.ClassifierResults;
import it.torvergata.bugprediction.models.ProjectClassifier;
import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.core.Instances;
import weka.core.converters.ConverterUtils.DataSource;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static it.torvergata.bugprediction.controllers.DatasetsBuilder.DATASETS_DIR;
import static it.torvergata.bugprediction.controllers.WekaResultsBuilder.WEKA_DIR;

public class WekaProcessor {
    private final String projName;

    private final int walkForwardIterations; // È il numero di dataset per il progetto considerato

    public WekaProcessor(String projName) throws IOException {
        this.projName = projName;
        this.walkForwardIterations = countDatasets(projName);
    }

    public List<ClassifierResults> processClassifierResults() throws Exception {
        List<ClassifierResults> classifierResultsList = new ArrayList<>();
        ClassifierFactory classifierFactory = new ClassifierFactory();

        for (int i = 1; i <= walkForwardIterations; i++) {

            // Ottieni le istanze del training e del test set
            Instances trainingSetInstance = loadDataset(trainingPath(i).toString());
            Instances testingSetInstance = loadDataset(testingPath(i).toString());

            validateDataset(trainingSetInstance, "training " + i);
            validateDataset(testingSetInstance, "testing " + i);

            // Prendi la lista di tutti i classificatori da addestrare
            int classIndex = trainingSetInstance.classIndex();
            List<ProjectClassifier> projectClassifiers = classifierFactory.getClassifiers(trainingSetInstance.attributeStats(classIndex));

            for (ProjectClassifier projectClassifier : projectClassifiers) {

                // Addestra il modello sul training set
                Classifier classifier = projectClassifier.getClassifier();
                classifier.buildClassifier(trainingSetInstance);

                // Valuta il modello sul testing set
                Evaluation evaluator = new Evaluation(testingSetInstance);
                evaluator.evaluateModel(classifier, testingSetInstance);

                // Salva i risultati
                ClassifierResults classifierResults = new ClassifierResults(i, projectClassifier, evaluator);

                // Calcola la percentuale del training set
                classifierResults.setTrainingPercent(
                        100.0 * trainingSetInstance.numInstances() /
                                (trainingSetInstance.numInstances() + testingSetInstance.numInstances())
                );
                classifierResultsList.add(classifierResults);
            }
        }
        return classifierResultsList;
    }

    public void writeFinalResults(String projName, List<ClassifierResults> finalResultsList) throws IOException {
        try {
            // Crea la directory
            String directoryName = WEKA_DIR + projName.toLowerCase() + "/";
            File file = new File(directoryName);
            if (!file.exists() && !file.mkdirs()) throw new IOException();

            String pathname = directoryName + projName.toLowerCase() + "_finalResults.csv";
            file = new File(pathname);
            try (FileWriter fileWriter = new FileWriter(file)) {
                fileWriter.append("DATASET," +
                        "#TRAINING_RELEASES," +
                        "%TRAINING_INSTANCES," +
                        "CLASSIFIER," +
                        "FEATURE_SELECTION," +
                        "BALANCING," +
                        "COST_SENSITIVE," +
                        "PRECISION," +
                        "RECALL," +
                        "AREA_UNDER_ROC," +
                        "KAPPA," +
                        "TRUE_POSITIVES," +
                        "FALSE_POSITIVES," +
                        "TRUE_NEGATIVES," +
                        "FALSE_NEGATIVES").append("\n");
                for (ClassifierResults classifierResults: finalResultsList) {
                    fileWriter.append(projName).append(",")
                            .append(String.valueOf(classifierResults.getWalkForwardIteration())).append(",")
                            .append(String.valueOf(classifierResults.getTrainingPercent())).append(",")
                            .append(classifierResults.getClassifierName()).append(",");
                    if (classifierResults.hasFeatureSelection()) {
                        fileWriter.append(classifierResults.getCustomClassifier().getFeatureSelectionFilterName()).append(",");
                    } else {
                        fileWriter.append("None").append(",");
                    }
                    if (classifierResults.hasSampling()) {
                        fileWriter.append(classifierResults.getCustomClassifier().getSamplingFilterName()).append(",");
                    } else {
                        fileWriter.append("None").append(",");
                    }
                    if (classifierResults.hasCostSensitive()) {
                        fileWriter.append("SensitiveLearning").append(",");
                    } else {
                        fileWriter.append("None").append(",");
                    }
                    fileWriter.append(String.valueOf(classifierResults.getPrecision())).append(",")
                            .append(String.valueOf(classifierResults.getRecall())).append(",")
                            .append(String.valueOf(classifierResults.getAreaUnderROC())).append(",")
                            .append(String.valueOf(classifierResults.getKappa())).append(",")
                            .append(String.valueOf(classifierResults.getTruePositives())).append(",")
                            .append(String.valueOf(classifierResults.getFalsePositives())).append(",")
                            .append(String.valueOf(classifierResults.getTrueNegatives())).append(",")
                            .append(String.valueOf(classifierResults.getFalseNegatives())).append("\n");
                }
            }
        } catch (IOException e) {
            throw new IOException("Errore nella creazione del file .csv dei risultati", e);
        }
    }

    private Instances loadDataset(String path) throws Exception {
        DataSource source = new DataSource(path);
        Instances data = source.getDataSet();
        data.setClassIndex(data.numAttributes() - 1);
        return data;
    }

    private void validateDataset(Instances data, String name) {
        if (data.numInstances() == 0) {
            throw new IllegalStateException("Dataset vuoto: " + name);
        }
    }

    private Path trainingPath(int i) {
        return Path.of(DATASETS_DIR, projName.toLowerCase(),
                "arffFiles", "training",
                projName.toLowerCase() + "_trainingSet" + i + ".arff");
    }

    private Path testingPath(int i) {
        return Path.of(DATASETS_DIR, projName.toLowerCase(),
                "arffFiles", "testing",
                projName.toLowerCase() + "_testingSet" + i + ".arff");
    }

    /**
     * Conta i file ARFF presenti nella cartella training del progetto
     */
    private static int countDatasets(String projName) throws IOException {
        Path trainingDir = Path.of(DATASETS_DIR)
                .resolve(projName.toLowerCase())
                .resolve("arffFiles")
                .resolve("training");

        if (!Files.exists(trainingDir) || !Files.isDirectory(trainingDir)) {
            throw new IOException("Cartella di training non trovata: " + trainingDir.toAbsolutePath());
        }

        try (var files = Files.list(trainingDir)) {
            return (int) files
                    .filter(Files::isRegularFile)
                    .filter(f -> f.toString().endsWith(".arff"))
                    .count();
        }
    }

}
