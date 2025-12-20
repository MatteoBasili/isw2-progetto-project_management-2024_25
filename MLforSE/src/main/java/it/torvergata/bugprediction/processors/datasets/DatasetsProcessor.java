package it.torvergata.bugprediction.processors.datasets;

import it.torvergata.bugprediction.enums.DatasetType;
import it.torvergata.bugprediction.enums.OutputFileType;
import it.torvergata.bugprediction.models.ReleaseClass;
import it.torvergata.bugprediction.models.Release;
import it.torvergata.bugprediction.utils.FileWriterUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

import static it.torvergata.bugprediction.controllers.DatasetsBuilder.RESULTS_DIR;

public class DatasetsProcessor {

    public static final String NAME_OF_THIS_CLASS = DatasetsProcessor.class.getName();
    private static final Logger logger = Logger.getLogger(NAME_OF_THIS_CLASS);

    private DatasetsProcessor() {}

    public static void writeDataset(String projName, List<Release> releaseList, List<ReleaseClass> classList, int iterationNumber,
                                    DatasetType datasetType, OutputFileType extension) throws IOException {
        StringBuilder projNameDelimited = new StringBuilder(projName.toLowerCase()).append("/");
        StringBuilder datasetTypeDelimited = new StringBuilder(datasetType.getId().toLowerCase()).append("/");
        StringBuilder pathname = new StringBuilder(RESULTS_DIR).append(projNameDelimited)
                .append(extension.getId().toLowerCase()).append("Files/").append(datasetType.getId().toLowerCase());
        File file = new File(pathname.toString());
        if (!file.exists() && !file.mkdirs())  throw new IOException();

        StringBuilder fileName = new StringBuilder();
        fileName.append(projName.toLowerCase()).append("_").append(datasetType.getId().toLowerCase()).append("Set").append(iterationNumber)
                .append(".").append(extension.getId().toLowerCase());
        pathname = new StringBuilder(RESULTS_DIR).append(projNameDelimited)
                .append(extension.getId().toLowerCase()).append("Files/").append(datasetTypeDelimited).append(fileName);
        file = new File(pathname.toString());

        try(FileWriter fileWriter = new FileWriter(file)) {
            appendOnFile(releaseList, classList, extension.equals(OutputFileType.ARFF), fileName.toString(), fileWriter);
        }
    }

    private static void appendOnFile(List<Release> releaseList, List<ReleaseClass> allReleaseClasses, boolean isArff, String fileName, FileWriter fileWriter) throws IOException {
        if(isArff){
            fileWriter.append("@relation ").append(fileName).append("\n\n")
                    .append("""
                        @attribute SIZE numeric
                        @attribute LOC_ADDED numeric
                        @attribute AVG_LOC_ADDED numeric
                        @attribute MAX_LOC_ADDED numeric
                        @attribute LOC_REMOVED numeric
                        @attribute AVG_LOC_REMOVED numeric
                        @attribute MAX_LOC_REMOVED numeric
                        @attribute CHURN numeric
                        @attribute AVG_CHURN numeric
                        @attribute MAX_CHURN numeric
                        @attribute NUMBER_OF_REVISIONS numeric
                        @attribute NUMBER_OF_DEFECT_FIXES numeric
                        @attribute NUMBER_OF_AUTHORS numeric
                        @attribute IS_BUGGY {'YES', 'NO'}
                        
                        @data
                        """);
            appendByRelease(releaseList, allReleaseClasses, fileWriter, true);
        }else{
            fileWriter.append(
                    "RELEASE_ID," +
                            "FILE_NAME," +
                            "SIZE," +
                            "LOC_ADDED,AVG_LOC_ADDED,MAX_LOC_ADDED," +
                            "LOC_REMOVED,AVG_LOC_REMOVED,MAX_LOC_REMOVED," +
                            "CHURN,AVG_CHURN,MAX_CHURN," +
                            "NUMBER_OF_REVISIONS," +
                            "NUMBER_OF_DEFECT_FIXES," +
                            "NUMBER_OF_AUTHORS," +
                            "IS_BUGGY").append("\n");
            appendByRelease(releaseList, allReleaseClasses, fileWriter, false);
        }
        FileWriterUtils.flushAndCloseFW(fileWriter, logger, NAME_OF_THIS_CLASS);
    }

    private static void appendByRelease(List<Release> releaseList, List<ReleaseClass> allReleaseClasses, FileWriter fileWriter, boolean isArff) throws IOException {
        for (Release release : releaseList) {
            for (ReleaseClass releaseClass : allReleaseClasses) {
                if (releaseClass.getRelease().getNumericId() == release.getNumericId()) {
                    appendEntriesLikeCSV(fileWriter, release, releaseClass, isArff);
                }
            }
        }
    }

    private static void appendEntriesLikeCSV(FileWriter fileWriter, Release release, ReleaseClass releaseClass, boolean isArff) throws IOException {
        String releaseID = Integer.toString(release.getNumericId());
        String className = releaseClass.getName();
        String sizeOfClass = String.valueOf(releaseClass.getMetrics().getSize());
        String addedLOC = String.valueOf(releaseClass.getMetrics().getAddedLOCMetrics().getVal());
        String avgAddedLOC = String.valueOf(releaseClass.getMetrics().getAddedLOCMetrics().getAvgVal());
        String maxAddedLOC = String.valueOf(releaseClass.getMetrics().getAddedLOCMetrics().getMaxVal());
        String removedLOC = String.valueOf(releaseClass.getMetrics().getRemovedLOCMetrics().getVal());
        String avgRemovedLOC = String.valueOf(releaseClass.getMetrics().getRemovedLOCMetrics().getAvgVal());
        String maxRemovedLOC = String.valueOf(releaseClass.getMetrics().getRemovedLOCMetrics().getMaxVal());
        String churn = String.valueOf(releaseClass.getMetrics().getChurnMetrics().getVal());
        String avgChurn = String.valueOf(releaseClass.getMetrics().getChurnMetrics().getAvgVal());
        String maxChurn = String.valueOf(releaseClass.getMetrics().getChurnMetrics().getMaxVal());
        String nRevisions = String.valueOf(releaseClass.getMetrics().getNumberOfRevisions());
        String nDefectFixes = String.valueOf(releaseClass.getMetrics().getNumberOfDefectFixes());
        String nAuthors = String.valueOf(releaseClass.getMetrics().getNumberOfAuthors());
        String isClassBugged = releaseClass.getMetrics().getBuggyness() ? "YES" : "NO" ;

        if(!isArff){
            fileWriter.append(releaseID).append(",")
                    .append(className).append(",");
        }
        fileWriter.append(sizeOfClass).append(",")
                .append(addedLOC).append(",")
                .append(avgAddedLOC).append(",")
                .append(maxAddedLOC).append(",")
                .append(removedLOC).append(",")
                .append(avgRemovedLOC).append(",")
                .append(maxRemovedLOC).append(",")
                .append(churn).append(",")
                .append(avgChurn).append(",")
                .append(maxChurn).append(",")
                .append(nRevisions).append(",")
                .append(nDefectFixes).append(",")
                .append(nAuthors).append(",")
                .append(isClassBugged).append("\n");
    }

}
