package it.torvergata.bugprediction.models;

import weka.classifiers.Classifier;

public class ProjectClassifier {

    private final Classifier classifier;
    private final String classifierName;
    private final String featureSelectionFilterName;
    private final String samplingFilterName;
    private final boolean isCostSensitive;

    public ProjectClassifier(Classifier classifier, String classifierName, String featureSelectionFilterName,
                             String samplingFilterName, boolean isCostSensitive) {
        this.classifier = classifier;
        this.classifierName = classifierName;
        this.samplingFilterName = samplingFilterName;
        this.featureSelectionFilterName = featureSelectionFilterName;
        this.isCostSensitive = isCostSensitive;
    }


    public Classifier getClassifier() {
        return classifier;
    }

    public String getClassifierName() {
        return classifierName;
    }

    public String getFeatureSelectionFilterName() {
        return featureSelectionFilterName;
    }

    public String getSamplingFilterName() {
        return samplingFilterName;
    }

    public boolean isCostSensitive() {
        return isCostSensitive;
    }

}
