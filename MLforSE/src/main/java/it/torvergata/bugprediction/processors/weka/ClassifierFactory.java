package it.torvergata.bugprediction.processors.weka;

import it.torvergata.bugprediction.config.ConfigLoader;
import it.torvergata.bugprediction.models.ProjectClassifier;
import weka.attributeSelection.BestFirst;
import weka.classifiers.meta.CostSensitiveClassifier;
import weka.classifiers.meta.FilteredClassifier;
import weka.core.SelectedTag;
import weka.filters.Filter;
import weka.filters.supervised.attribute.AttributeSelection;
import weka.filters.supervised.instance.SMOTE;
import weka.classifiers.Classifier;
import weka.classifiers.CostMatrix;
import weka.classifiers.bayes.NaiveBayes;
import weka.classifiers.lazy.IBk;
import weka.classifiers.trees.RandomForest;
import weka.core.AttributeStats;

import java.util.ArrayList;
import java.util.List;

public class ClassifierFactory {

    public static final String NO_SELECTION = "NoSelection";
    public static final String NO_SAMPLING = "NoSampling";

    public final double falsePositiveWeight;
    public final double falseNegativeWeight;

    public ClassifierFactory() {
        falsePositiveWeight = ConfigLoader.loadFalsePositiveWeight();
        falseNegativeWeight = ConfigLoader.loadFalseNegativeWeight();
    }

    public List<ProjectClassifier> getClassifiers(AttributeStats isBuggyAttributeStats) {
        // Ottieni i modelli da addestrare
        List<Classifier> classifierList = new ArrayList<>(List.of(new RandomForest(), new NaiveBayes(), new IBk()));
        List<ProjectClassifier> projectClassifiersList = new ArrayList<>();

        // Ottieni il filtro di feature selection (best first)
        AttributeSelection featureSelectionFilter = createBestFirstFilter();

        // Classificatori NO Feature Selection, NO Sampling, NO Cost Sensitive
        createNoFSNoSamplingNoCSClassifiers(classifierList, projectClassifiersList);

        // Classificatori SI Feature Selection, NO Sampling, NO Cost Sensitive
        createFSClassifiers(classifierList, featureSelectionFilter, projectClassifiersList);

        // Classificatori NO Feature Selection, NO Sampling, SI Cost Sensitive
        createCSClassifiers(classifierList, projectClassifiersList);

        // Classificatori SI Feature Selection, NO Sampling, SI Cost Sensitive
        createFSAndCSClassifiers(classifierList, featureSelectionFilter, projectClassifiersList);

        // Classificatori con sampling
        createSamplingClassifiers(isBuggyAttributeStats, classifierList, featureSelectionFilter, projectClassifiersList);

        return projectClassifiersList;
    }

    /**
     * Crea e configura un filtro di selezione attributi utilizzando l'algoritmo BestFirst.
     * @return un oggetto AttributeSelection configurato con la ricerca BestFirst bi-direzionale
     */
    private AttributeSelection createBestFirstFilter() {
        AttributeSelection bestFirstAS = new AttributeSelection();

        BestFirst bestFirst = new BestFirst();
        bestFirst.setDirection(new SelectedTag(2, bestFirst.getDirection().getTags()));  // ricerca bi-direzionale
        bestFirstAS.setSearch(bestFirst);

        return bestFirstAS;
    }

    // Crea e aggiunge alla lista di classificatori quelli senza feature selection,
    // senza sampling e senza cost sensitivity
    private void createNoFSNoSamplingNoCSClassifiers(List<Classifier> classifierList, List<ProjectClassifier> projectClassifiersList) {
        for (Classifier classifier : classifierList) {
            projectClassifiersList.add(
                    new ProjectClassifier(classifier, classifier.getClass().getSimpleName(), NO_SELECTION, NO_SAMPLING, false));
        }
    }

    // Crea e aggiunge alla lista di classificatori quelli con un filtro di selezione delle feature
    private void createFSClassifiers(List<Classifier> classifierList, AttributeSelection featureSelectionFilter, List<ProjectClassifier> projectClassifiersList) {
        for (Classifier classifier : classifierList) {
            FilteredClassifier filteredClassifier = new FilteredClassifier();
            filteredClassifier.setClassifier(classifier);
            filteredClassifier.setFilter(featureSelectionFilter);

            projectClassifiersList.add(new ProjectClassifier(filteredClassifier, classifier.getClass().getSimpleName(),
                    featureSelectionFilter.getSearch().getClass().getSimpleName(), NO_SAMPLING, false));
        }
    }

    // Crea e aggiunge alla lista di classificatori quelli con un filtro
    // di classificazione sensibile ai costi
    private void createCSClassifiers(List<Classifier> classifierList, List<ProjectClassifier> projectClassifiersList) {
        for (Classifier classifier : classifierList) {
            CostSensitiveClassifier costSensitiveClassifier = getCostSensitiveFilterClassifier();
            costSensitiveClassifier.setClassifier(classifier);

            projectClassifiersList.add(new ProjectClassifier(costSensitiveClassifier, classifier.getClass().getSimpleName(),
                    NO_SELECTION, NO_SAMPLING, true));
        }
    }

    // Crea e aggiunge alla lista di classificatori quelli con un filtro di selezione delle feature
    // e un filtro di classificazione sensibile ai costi
    private void createFSAndCSClassifiers(List<Classifier> classifierList, AttributeSelection featureSelectionFilter,
                                          List<ProjectClassifier> projectClassifiersList) {
        for (Classifier classifier : classifierList) {
            CostSensitiveClassifier costSensitiveClassifier = getCostSensitiveFilterClassifier();
            costSensitiveClassifier.setClassifier(classifier);

            FilteredClassifier filteredCostSensitiveClassifier = new FilteredClassifier();
            filteredCostSensitiveClassifier.setFilter(featureSelectionFilter);
            filteredCostSensitiveClassifier.setClassifier(costSensitiveClassifier);

            projectClassifiersList.add(new ProjectClassifier(filteredCostSensitiveClassifier, classifier.getClass().getSimpleName(),
                    featureSelectionFilter.getSearch().getClass().getSimpleName(), NO_SAMPLING, true));
        }
    }

    private CostSensitiveClassifier getCostSensitiveFilterClassifier() {
        CostSensitiveClassifier costSensitiveClassifier = new CostSensitiveClassifier();
        costSensitiveClassifier.setMinimizeExpectedCost(false);
        CostMatrix costMatrix = getCostMatrix();
        costSensitiveClassifier.setCostMatrix(costMatrix);
        return costSensitiveClassifier;
    }

    private CostMatrix getCostMatrix() {
        CostMatrix costMatrix = new CostMatrix(2);
        costMatrix.setCell(0, 0, 0.0);
        costMatrix.setCell(1, 0, falsePositiveWeight);
        costMatrix.setCell(0, 1, falseNegativeWeight);
        costMatrix.setCell(1, 1, 0.0);
        return costMatrix;
    }

    private void createSamplingClassifiers(AttributeStats isBuggyAttributeStats, List<Classifier> classifierList,
                                           AttributeSelection featureSelectionFilter,
                                           List<ProjectClassifier> projectClassifiersList) {
        int majorityClassSize = isBuggyAttributeStats.nominalCounts[1];
        int minorityClassSize = isBuggyAttributeStats.nominalCounts[0];
        SMOTE samplingFilter = getSamplingFilter(majorityClassSize, minorityClassSize);

        // Classificatori NO Feature Selection, SI Sampling, NO Cost Sensitive
        createOnlySamplingClassifiers(classifierList, samplingFilter, projectClassifiersList);

        // Classificatori SI Feature Selection, SI Sampling, NO Cost Sensitive
        createFSAndSamplingClassifiers(classifierList, featureSelectionFilter, samplingFilter, projectClassifiersList);

        // Classificatori NO Feature Selection, SI Sampling, SI Cost Sensitive
        createCSAndSamplingClassifiers(classifierList, samplingFilter, projectClassifiersList);

        // Classificatori SI Feature Selection, SI Sampling, SI Cost Sensitive
        createFSSamplingAndCSClassifiers(classifierList, featureSelectionFilter, samplingFilter, projectClassifiersList);
    }

    private SMOTE getSamplingFilter(int majorityClassSize, int minorityClassSize) {
        double percentSMOTE;
        if (minorityClassSize == 0 || minorityClassSize > majorityClassSize) {
            percentSMOTE = 0;
        } else {
            percentSMOTE = (100.0 * (majorityClassSize-minorityClassSize))/minorityClassSize;
        }

        SMOTE smote = new SMOTE();
        smote.setClassValue("1");
        smote.setPercentage(percentSMOTE);

        return smote;
    }

    // Crea e aggiunge alla lista di classificatori quelli con un filtro di sampling
    private void createOnlySamplingClassifiers(List<Classifier> classifierList, Filter samplingFilter, List<ProjectClassifier> projectClassifiersList) {
        for (Classifier classifier : classifierList) {
            FilteredClassifier filteredClassifier = new FilteredClassifier();
            filteredClassifier.setClassifier(classifier);
            filteredClassifier.setFilter(samplingFilter);

            projectClassifiersList.add(new ProjectClassifier(filteredClassifier, classifier.getClass().getSimpleName(),NO_SELECTION, samplingFilter.getClass().getSimpleName(), false));
        }
    }

    // Crea e aggiunge alla lista di classificatori quelli con un filtro di feature selection
    // e un filtro di sampling
    private void createFSAndSamplingClassifiers(List<Classifier> classifierList, AttributeSelection featureSelectionFilter, Filter samplingFilter, List<ProjectClassifier> projectClassifiersList) {
        for (Classifier classifier : classifierList) {

            FilteredClassifier innerClassifier = new FilteredClassifier();
            innerClassifier.setClassifier(classifier);
            innerClassifier.setFilter(featureSelectionFilter);

            FilteredClassifier externalClassifier = new FilteredClassifier();
            externalClassifier.setFilter(samplingFilter);
            externalClassifier.setClassifier(innerClassifier);

            projectClassifiersList.add(new ProjectClassifier(externalClassifier, classifier.getClass().getSimpleName(),
                    featureSelectionFilter.getSearch().getClass().getSimpleName(), samplingFilter.getClass().getSimpleName(), false));
        }
    }

    // Crea e aggiunge alla lista di classificatori quelli con un filtro di
    // classificazione sensibile ai costi e un filtro di sampling
    private void createCSAndSamplingClassifiers(List<Classifier> classifierList, Filter samplingFilter,
                                                List<ProjectClassifier> projectClassifiersList) {
        CostSensitiveClassifier costSensitiveClassifier = getCostSensitiveFilterClassifier();
        for (Classifier classifier : classifierList) {
            FilteredClassifier innerClassifier = new FilteredClassifier();
            innerClassifier.setClassifier(classifier);
            innerClassifier.setFilter(samplingFilter);

            costSensitiveClassifier.setClassifier(innerClassifier);
            projectClassifiersList.add(new ProjectClassifier(costSensitiveClassifier, classifier.getClass().getSimpleName(),
                    NO_SELECTION, samplingFilter.getClass().getSimpleName(), true));
        }
    }

    // Crea e aggiunge alla lista di classificatori quelli con un filtro di feature selection,
    // un filtro di classificazione sensibile ai costi e un filtro di sampling
    private void createFSSamplingAndCSClassifiers(List<Classifier> classifierList, AttributeSelection featureSelectionFilter,
                                                  Filter samplingFilter, List<ProjectClassifier> projectClassifiersList) {
        CostSensitiveClassifier costSensitiveClassifier = getCostSensitiveFilterClassifier();
        for (Classifier classifier : classifierList) {
            FilteredClassifier innerClassifier = new FilteredClassifier();
            innerClassifier.setClassifier(classifier);
            innerClassifier.setFilter(featureSelectionFilter);

            FilteredClassifier externalClassifier = new FilteredClassifier();
            externalClassifier.setFilter(samplingFilter);
            externalClassifier.setClassifier(innerClassifier);

            costSensitiveClassifier.setClassifier(externalClassifier);
            projectClassifiersList.add(new ProjectClassifier(costSensitiveClassifier, classifier.getClass().getSimpleName(),
                    featureSelectionFilter.getSearch().getClass().getSimpleName(), samplingFilter.getClass().getSimpleName(), true));
        }
    }

}
