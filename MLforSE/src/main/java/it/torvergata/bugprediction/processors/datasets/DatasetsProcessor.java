package it.torvergata.bugprediction.processors.datasets;

import it.torvergata.bugprediction.enums.DatasetType;
import it.torvergata.bugprediction.enums.OutputFileType;
import it.torvergata.bugprediction.models.ReleaseClass;
import it.torvergata.bugprediction.models.Release;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

import static it.torvergata.bugprediction.controllers.DatasetsBuilder.DATASETS_DIR;

public class DatasetsProcessor {

    private DatasetsProcessor() {}

    public static void writeDataset(String projName,
                                    List<Release> releaseList,
                                    List<ReleaseClass> classList,
                                    int iterationNumber,
                                    DatasetType datasetType,
                                    OutputFileType extension) throws IOException {

        // Directory dei file
        Path datasetDir = Path.of(DATASETS_DIR)
                .resolve(projName.toLowerCase())
                .resolve(extension.getId().toLowerCase() + "Files")
                .resolve(datasetType.getId().toLowerCase());

        if (!Files.exists(datasetDir)) {
            Files.createDirectories(datasetDir);
        }

        // Nome file
        String fileName = projName.toLowerCase() + "_" + datasetType.getId().toLowerCase()
                + "Set" + iterationNumber + "." + extension.getId().toLowerCase();
        Path filePath = datasetDir.resolve(fileName);

        try (BufferedWriter writer = Files.newBufferedWriter(filePath, StandardCharsets.UTF_8)) {
            appendDataset(writer, releaseList, classList, extension == OutputFileType.ARFF, fileName);
        }
    }

    private static void appendDataset(BufferedWriter writer, List<Release> releases,
                                      List<ReleaseClass> classes, boolean isArff,
                                      String fileName) throws IOException {

        String lineSep = System.lineSeparator();

        if (isArff) {
            writer.write("@relation " + fileName + lineSep + lineSep);
            for (String attr : List.of(
                    "SIZE numeric",
                    "LOC_ADDED numeric",
                    "AVG_LOC_ADDED numeric",
                    "MAX_LOC_ADDED numeric",
                    "LOC_REMOVED numeric",
                    "AVG_LOC_REMOVED numeric",
                    "MAX_LOC_REMOVED numeric",
                    "CHURN numeric",
                    "AVG_CHURN numeric",
                    "MAX_CHURN numeric",
                    "NUMBER_OF_REVISIONS numeric",
                    "NUMBER_OF_DEFECT_FIXES numeric",
                    "NUMBER_OF_AUTHORS numeric",
                    "IS_BUGGY {'YES','NO'}")) {
                writer.write("@attribute " + attr + lineSep);
            }
            writer.write(lineSep + "@data" + lineSep);
        } else {
            writer.write(String.join(",", List.of(
                    "RELEASE_ID","FILE_NAME","SIZE","LOC_ADDED","AVG_LOC_ADDED","MAX_LOC_ADDED",
                    "LOC_REMOVED","AVG_LOC_REMOVED","MAX_LOC_REMOVED","CHURN","AVG_CHURN","MAX_CHURN",
                    "NUMBER_OF_REVISIONS","NUMBER_OF_DEFECT_FIXES","NUMBER_OF_AUTHORS","IS_BUGGY"))
                    + lineSep);
        }

        // Indice rapido delle classi per release
        Map<Integer, List<ReleaseClass>> classesByRelease = classes.stream()
                .collect(Collectors.groupingBy(rc -> rc.getRelease().getNumericId()));

        for (Release r : releases) {
            List<ReleaseClass> releaseClasses = classesByRelease.getOrDefault(r.getNumericId(), Collections.emptyList());
            for (ReleaseClass rc : releaseClasses) {
                appendClassRow(writer, r, rc, isArff, lineSep);
            }
        }
    }

    private static void appendClassRow(BufferedWriter writer, Release r, ReleaseClass rc,
                                       boolean isArff, String lineSep) throws IOException {

        List<String> values = new ArrayList<>();
        if (!isArff) values.add(String.valueOf(r.getNumericId()));
        if (!isArff) values.add(rc.getName());
        values.add(String.valueOf(rc.getMetrics().getSize()));
        values.add(String.valueOf(rc.getMetrics().getAddedLOCMetrics().getVal()));
        values.add(String.valueOf(rc.getMetrics().getAddedLOCMetrics().getAvgVal()));
        values.add(String.valueOf(rc.getMetrics().getAddedLOCMetrics().getMaxVal()));
        values.add(String.valueOf(rc.getMetrics().getRemovedLOCMetrics().getVal()));
        values.add(String.valueOf(rc.getMetrics().getRemovedLOCMetrics().getAvgVal()));
        values.add(String.valueOf(rc.getMetrics().getRemovedLOCMetrics().getMaxVal()));
        values.add(String.valueOf(rc.getMetrics().getChurnMetrics().getVal()));
        values.add(String.valueOf(rc.getMetrics().getChurnMetrics().getAvgVal()));
        values.add(String.valueOf(rc.getMetrics().getChurnMetrics().getMaxVal()));
        values.add(String.valueOf(rc.getMetrics().getNumberOfRevisions()));
        values.add(String.valueOf(rc.getMetrics().getNumberOfDefectFixes()));
        values.add(String.valueOf(rc.getMetrics().getNumberOfAuthors()));
        values.add(rc.getMetrics().getBuggyness() ? "YES" : "NO");

        writer.write(String.join(",", values) + lineSep);
    }


}
