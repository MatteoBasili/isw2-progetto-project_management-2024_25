package it.torvergata.bugprediction.processors.acume;

import com.sun.istack.NotNull;
import it.torvergata.bugprediction.models.AcumeInstance;
import it.torvergata.bugprediction.models.ClassifierResults;
import weka.classifiers.Evaluation;
import weka.core.Instances;
import weka.core.converters.ConverterUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static it.torvergata.bugprediction.MLSELauncher.RESULTS_DIR;
import static it.torvergata.bugprediction.controllers.DatasetsBuilder.DATASETS_DIR;

public class AcumeProcessor {

    private final String directoryName;
    private final String projName;

    public AcumeProcessor(String projName) throws IOException {
        this.projName = projName;
        this.directoryName = buildDirectoryName(projName);
        createDirectory(directoryName);
    }

    public void processACUMEFiles(List<ClassifierResults> results) throws Exception {
        for (ClassifierResults result : results) {
            Instances training = loadDataset(result, true);
            Instances testing = loadDataset(result, false);

            trainAndEvaluate(result, training, testing);

            List<AcumeInstance> acumeInstances = buildACUMEInstances(result, testing);
            processACUMEFile(acumeInstances, result);
        }
    }

    private Instances loadDataset(ClassifierResults result, boolean training) throws Exception {
        String type = training ? "training" : "testing";
        String suffix = training ? "_trainingSet" : "_testingSet";

        String path = DATASETS_DIR + projName.toLowerCase()
                + "/arffFiles/" + type + "/"
                + projName.toLowerCase() + suffix
                + result.getWalkForwardIteration() + ".arff";

        Instances data = new ConverterUtils.DataSource(path).getDataSet();
        data.setClassIndex(data.numAttributes() - 1);
        return data;
    }

    private void trainAndEvaluate(ClassifierResults result,
                                  Instances training,
                                  Instances testing) throws Exception {
        var classifier = result.getCustomClassifier().getClassifier();
        classifier.buildClassifier(training);
        Evaluation eval = new Evaluation(testing);
        eval.evaluateModel(classifier, testing);
    }

    private List<AcumeInstance> buildACUMEInstances(ClassifierResults result,
                                                    Instances testing) throws Exception {
        List<AcumeInstance> list = new ArrayList<>();

        int sizeIndex = testing.attribute("SIZE").index();
        int isBuggyIndex = testing.classAttribute().index();
        int yesBuggyIndex = testing.classAttribute().indexOfValue("YES");

        if (yesBuggyIndex == -1) return list;

        var classifier = result.getCustomClassifier().getClassifier();

        for (int i = 0; i < testing.numInstances(); i++) {
            int sizeValue = (int) testing.instance(i).value(sizeIndex);
            int valueIndex = (int) testing.instance(i).value(isBuggyIndex);
            String buggyness =  testing.attribute(isBuggyIndex).value(valueIndex);
            double[] distribution = classifier.distributionForInstance(testing.instance(i));
            AcumeInstance acumeInstance = new AcumeInstance(i, sizeValue, distribution[yesBuggyIndex], buggyness);
            list.add(acumeInstance);
        }
        return list;
    }

    private void processACUMEFile(List<AcumeInstance> acumeInstances, ClassifierResults classifierResult) throws IOException {
        File file = getFile(classifierResult);

        try(FileWriter fileWriter = new FileWriter(file)) {
            fileWriter.write("ID, Size, Predicted, Actual\n");

            for(AcumeInstance acumeInstance : acumeInstances) {
                fileWriter.write(acumeInstance.getId() + ",");
                fileWriter.write(acumeInstance.getSize() + ",");
                fileWriter.write(acumeInstance.getPredicted() + ",");
                fileWriter.write(acumeInstance.getActual() + "\n");
            }
        }

    }

    private @NotNull File getFile(ClassifierResults classifierResults) {
        String costSensitive = classifierResults.getCustomClassifier().isCostSensitive() ? "yesCostSensitive" : "noCostSensitive";
        String filename =  directoryName + projName.toLowerCase() +
                "_" + classifierResults.getClassifierName() +
                "_" + classifierResults.getCustomClassifier().getFeatureSelectionFilterName() +
                "_" + classifierResults.getCustomClassifier().getSamplingFilterName() +
                "_" + costSensitive +
                "_" + classifierResults.getWalkForwardIteration() +
                ".csv";

        return new File(filename);
    }

    private String buildDirectoryName(String projName) {
        String name = projName.toLowerCase();
        return RESULTS_DIR + name + "/AcumeFiles/";
    }

    private void createDirectory(String directoryName) throws IOException {
        File file = new File(directoryName);
        if (!file.exists() && !file.mkdirs()) {
            throw new IOException("Impossibile creare la directory: " + directoryName);
        }
    }

}
