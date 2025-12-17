package it.torvergata.bugprediction;

import java.io.*;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DatasetBuilderFake {
/*
    private static final Logger LOGGER = Logger.getLogger(DatasetBuilderFake.class.getName());

    static class Metric {
        double loc = 0;
        double locTouched = 0;
        int nRev = 0;
        int nFix = 0;
        Set<String> authors = new HashSet<>();
        List<Double> locAddedList = new ArrayList<>();
        List<Double> churnList = new ArrayList<>();
        List<Integer> chgSetList = new ArrayList<>();
        LocalDateTime lastCommitDate;
        boolean buggy = false;
    }

    static class Release {
        String version;
        LocalDateTime date;

        Release(String version, LocalDateTime date) {
            this.version = version;
            this.date = date;
        }
    }

    static class BugInfo {
        String bugID;
        LocalDateTime ovDate; // opening version date
        LocalDateTime fvDate; // fixing version date
        LocalDateTime ivDate; // injected version date (calculated)
        String openingVersion; // release containing ovDate
        String fixedVersion; // release containing fvDate
    }

    public static void main(String[] args) throws Exception {
        String project = "bookkeeper";
        String dataDir = "data/";

        // Load release
        List<Release> releases = loadReleases(dataDir + "BOOKKEEPERVersionInfo.csv");
        LocalDateTime maxAllowedDate = computeMaxAllowedDate(releases);

        // Load bug commit opening and fixing dates
        Map<String, LocalDateTime> bugOpeningDates =
                loadBugOpeningDates(dataDir + project + "_Tickets.csv", maxAllowedDate);
        Map<String, LocalDateTime> bugFixingDates =
                loadBugFixingDates(dataDir + project + "_Metrics.csv", bugOpeningDates, maxAllowedDate);

        // Compute Opening Version and Fixed Version for each bug
        Map<String, BugInfo> bugInfoMap = computeBugVersions(bugOpeningDates, bugFixingDates, releases);

        // Calculate IV using Proportion Moving Window
        bugInfoMap = computeInjectedVersionsIncrement(bugOpeningDates, bugFixingDates);

        // Build metrics
        Map<String, Metric> metrics = buildMetricsMap(project, dataDir, maxAllowedDate, bugInfoMap);

        // Write final dataset
        writeDatasetCSV(project, releases, metrics);

        LOGGER.log(Level.INFO, "Final dataset created for project: {0}", project);
    }

    private static LocalDateTime computeMaxAllowedDate(List<Release> releases) {
        //int half = releases.size() / 2;
        //List<Release> firstHalf = releases.subList(0, half);
        //return firstHalf.get(firstHalf.size() - 1).date;
        return releases.get(releases.size() - 1).date;
    }

    // Load all releases sorted by date
    private static List<Release> loadReleases(String csvPath) throws IOException, CsvValidationException {
        List<Release> releases = new ArrayList<>();

        try (CSVReader reader = new CSVReader(new FileReader(csvPath))) {
            reader.readNext(); // skip header
            String[] row;
            while ((row = reader.readNext()) != null) {
                String version = row[2];
                String dateStr = row[3];
                LocalDateTime date = LocalDateTime.parse(dateStr);
                releases.add(new Release(version, date));
            }
        }

        releases.sort(Comparator.comparing(r -> r.date));
        return releases;
    }

    // Read bugID and OpeningDate from ticket CSV
    private static Map<String, LocalDateTime> loadBugOpeningDates(String ticketsCsvPath,
                                                                  LocalDateTime maxAllowedDate)
            throws IOException, CsvValidationException {
        Map<String, LocalDateTime> bugMap = new HashMap<>();
        try (CSVReader reader = new CSVReader(new FileReader(ticketsCsvPath))) {
            String[] row;
            reader.readNext(); // skip header
            while ((row = reader.readNext()) != null) {
                String bugID = row[0];
                String dateStr = row[1]; // OpeningDate
                LocalDateTime ovDate = parseDate(dateStr);
                // Consider only bugs opened before or equal to maxAllowedDate
                if (ovDate.isAfter(maxAllowedDate)) continue;
                bugMap.put(bugID, ovDate);
            }
        }
        return bugMap;
    }

    // Read fixing commit date from metric CSV
    private static Map<String, LocalDateTime> loadBugFixingDates(String metricsCsvPath,
                                                                 Map<String, LocalDateTime> bugOpeningDates,
                                                                 LocalDateTime maxAllowedDate)
            throws IOException, CsvValidationException {
        Map<String, LocalDateTime> bugFV = new HashMap<>();
        try (CSVReader reader = new CSVReader(new FileReader(metricsCsvPath))) {
            reader.readNext(); // skip header
            String[] row;
            while ((row = reader.readNext()) != null) {
                boolean isFix = Boolean.parseBoolean(row[7]);
                String commitMsg = row[2];
                LocalDateTime commitDate = parseDate(row[1]);
                if (commitDate.isAfter(maxAllowedDate)) continue;
                if (isFix) {
                    for (String bugID : bugOpeningDates.keySet()) {
                        if (commitMsg.contains(bugID)) {
                            bugFV.put(bugID, commitDate);
                        }
                    }
                }
            }
        }
        return bugFV;
    }

    private static Map<String, BugInfo> computeBugVersions(Map<String, LocalDateTime> bugOV,
                                                           Map<String, LocalDateTime> bugFV,
                                                           List<Release> releases) {
        Map<String, BugInfo> bugInfoMap = new HashMap<>();

        for (String bugID : bugOV.keySet()) {
            LocalDateTime ov = bugOV.get(bugID);
            LocalDateTime fv = bugFV.get(bugID);
            if (fv == null || ov == null || fv.isBefore(ov)) continue;

            BugInfo info = new BugInfo();
            info.bugID = bugID;
            info.ovDate = ov;
            info.fvDate = fv;
            info.openingVersion = findReleaseForDate(releases, ov);
            info.fixedVersion = findReleaseForDateAfter(releases, fv);
            bugInfoMap.put(bugID, info);
        }

        return bugInfoMap;
    }

    private static Map<String, BugInfo> computeInjectedVersionsIncrement(
            Map<String, LocalDateTime> bugOV,
            Map<String, LocalDateTime> bugFV) {

        Map<String, BugInfo> bugInfoMap = new HashMap<>();
        double cumulativeP = 0.0;
        int countP = 0;

        // Ordina i bug per data di fixing crescente
        List<String> sortedBugIDs = new ArrayList<>(bugFV.keySet());
        sortedBugIDs.sort(Comparator.comparing(bugFV::get));

        for (String bugID : sortedBugIDs) {
            LocalDateTime ov = bugOV.get(bugID);
            LocalDateTime fv = bugFV.get(bugID);

            if (ov == null || fv == null || fv.isBefore(ov)) continue;

            long deltaDays = Duration.between(ov, fv).toDays();
            if (deltaDays <= 0) continue;

            // P corrente: media incrementale
            double p = (countP == 0) ? 0.33 : cumulativeP / countP;

            // Calcola IV
            long daysIV = Math.round(deltaDays * p);
            LocalDateTime iv = fv.minusDays(daysIV);

            // Calcola la proporzione osservata e aggiorna cumulativo
            double observedP = (double) Duration.between(iv, fv).toDays() / deltaDays;
            cumulativeP += observedP;
            countP++;

            BugInfo info = new BugInfo();
            info.bugID = bugID;
            info.ovDate = ov;
            info.fvDate = fv;
            info.ivDate = iv;
            bugInfoMap.put(bugID, info);
        }

        return bugInfoMap;
    }

    private static Map<String, Metric> buildMetricsMap(String project, String dataDir,
                                                       LocalDateTime maxAllowedDate,
                                                       Map<String, BugInfo> bugInfoMap)
            throws IOException, CsvValidationException {
        Map<String, Metric> map = new HashMap<>();
        try (CSVReader reader = new CSVReader(new FileReader(dataDir + project + "_Metrics.csv"))) {
            reader.readNext(); // skip header
            String[] row;
            while ((row = reader.readNext()) != null) {
                processCommitRow(map, row, maxAllowedDate, bugInfoMap);
            }
        }
        return map;
    }

    private static void processCommitRow(Map<String, Metric> map, String[] row,
                                         LocalDateTime maxDate, Map<String, BugInfo> bugInfoMap) {
        LocalDateTime commitDate = parseDate(row[1]);
        if (commitDate.isAfter(maxDate)) return;

        String fileName = row[3];
        boolean isFix = Boolean.parseBoolean(row[7]);
        int chgSetSize = Integer.parseInt(row[8]);
        double locAdded = parseDoubleSafe(row[4]);
        double locDeleted = parseDoubleSafe(row[5]);
        double locTouched = locAdded + locDeleted;
        double churn = chgSetSize > 0 ? locTouched / chgSetSize : 0;

        Metric metric = map.getOrDefault(fileName, new Metric());
        metric.loc += locAdded;
        metric.locTouched += locTouched;
        metric.nRev++;
        if (isFix) metric.nFix++;
        metric.authors.add(row[2]);
        metric.locAddedList.add(locAdded);
        metric.churnList.add(churn);
        metric.chgSetList.add(chgSetSize);
        if (metric.lastCommitDate == null || commitDate.isAfter(metric.lastCommitDate))
            metric.lastCommitDate = commitDate;

        // Proportion Increment: labeling
        for (BugInfo bug : bugInfoMap.values()) {
            if (!commitDate.isBefore(bug.ivDate) && !commitDate.isAfter(bug.fvDate)) {
                metric.buggy = true;
                break;
            }
        }

        map.put(fileName, metric);
    }

    private static void writeDatasetCSV(String project, List<Release> releases, Map<String, Metric> map) {
        String outFileName = DataUtils.prepareOutputDataFilePath(project + "_dataset.csv");
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outFileName))) {
            writer.write("Version,File,LOC,LOC_touched,NR,NFix,NAuth,LOC_added,MAX_LOC_added,AVG_LOC_added," +
                    "Churn,MAX_Churn,AVG_Churn,ChgSetSize,MAX_ChgSet,AVG_ChgSet,Age,WeightedAge,Buggy\n");

            for (Map.Entry<String, Metric> entry : map.entrySet()) {
                String fileName = entry.getKey();
                Metric m = entry.getValue();
                String version = findReleaseForDate(releases, m.lastCommitDate);

                double avgLocAdded = m.locAddedList.stream().mapToDouble(d -> d).average().orElse(0);
                double maxLocAdded = m.locAddedList.stream().mapToDouble(d -> d).max().orElse(0);
                double avgChurn = m.churnList.stream().mapToDouble(d -> d).average().orElse(0);
                double maxChurn = m.churnList.stream().mapToDouble(d -> d).max().orElse(0);
                double avgChgSet = m.chgSetList.stream().mapToInt(i -> i).average().orElse(0);
                double maxChgSet = m.chgSetList.stream().mapToInt(i -> i).max().orElse(0);
                double age = m.lastCommitDate != null ? Duration.between(releases.get(0).date, m.lastCommitDate).toDays() : 0;
                double weightedAge = m.lastCommitDate != null ? age * m.locTouched : 0;

                writer.write(String.join(",",
                        version,
                        fileName,
                        String.valueOf(m.loc),
                        String.valueOf(m.locTouched),
                        String.valueOf(m.nRev),
                        String.valueOf(m.nFix),
                        String.valueOf(m.authors.size()),
                        String.valueOf(m.loc),
                        String.valueOf(maxLocAdded),
                        String.valueOf(avgLocAdded),
                        String.valueOf(m.churnList.stream().mapToDouble(d -> d).sum()),
                        String.valueOf(maxChurn),
                        String.valueOf(avgChurn),
                        String.valueOf(m.chgSetList.stream().mapToInt(i -> i).sum()),
                        String.valueOf(maxChgSet),
                        String.valueOf(avgChgSet),
                        String.valueOf(age),
                        String.valueOf(weightedAge),
                        m.buggy ? "Yes" : "No"));
                writer.newLine();
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error writing CSV file", e);
        }
    }

    // Find the latest release <= commit date
    private static String findReleaseForDate(List<Release> releases, LocalDateTime commitDate) {
        String version = "Pre-Release";
        for (Release r : releases) {
            if (!r.date.isAfter(commitDate)) {
                version = r.version;
            } else {
                break;
            }
        }
        return version;
    }

    private static String findReleaseForDateAfter(List<Release> releases, LocalDateTime date) {
        for (Release r : releases) {
            if (!r.date.isBefore(date)) return r.version;
        }
        return releases.get(releases.size() - 1).version; }

    // Robust parsing of commit data (git format)
    private static LocalDateTime parseDate(String dateStr) {
        try {
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX");
            return LocalDateTime.parse(dateStr, fmt);
        } catch (Exception e) {
            try {
                DateTimeFormatter fmt2 = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss Z");
                return LocalDateTime.parse(dateStr, fmt2);
            } catch (Exception ex) {
                return LocalDateTime.now();
            }
        }
    }

    private static double parseDoubleSafe(String s) {
        try {
            if (s.equals("-")) return 0;
            return Double.parseDouble(s);
        } catch (Exception e) {
            return 0;
        }
    }*/
}
