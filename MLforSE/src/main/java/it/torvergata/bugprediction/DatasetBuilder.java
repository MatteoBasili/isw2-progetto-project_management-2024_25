package it.torvergata.bugprediction;

import com.opencsv.exceptions.CsvValidationException;
import it.torvergata.bugprediction.utils.FileWriterUtils;
import com.opencsv.CSVReader;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DatasetBuilder {

    private static final Logger LOGGER = Logger.getLogger(DatasetBuilder.class.getName());

    static class Metric {
        int locAdded = 0;
        int locDeleted = 0;
        int nRev = 0;
        int nFix = 0;
        Set<String> authors = new HashSet<>();
        boolean buggy = false;
        LocalDateTime date; // last commit date

        // derived metrics
        int locTouched = 0; // LOC_Added + LOC_Deleted
        double churn = 0;   // LOC_Touched / NR
    }

    static class Release {
        String name;
        LocalDateTime date;
        Release(String name, LocalDateTime date) {
            this.name = name;
            this.date = date;
        }
    }

    public static void main(String[] args) throws Exception {
        String project = "bookkeeper";
        String dataDir = "data/";

        List<Release> releases = loadReleases(dataDir + "BOOKKEEPERVersionInfo.csv");
        LocalDateTime maxAllowedDate = computeMaxAllowedDate(releases);

        Map<String, Metric> metrics = buildMetricsMap(project, dataDir, maxAllowedDate);
        writeDatasetCSV(project, releases, metrics);

        LOGGER.log(Level.INFO, "Final dataset created for project: {0}", project);
    }

    private static LocalDateTime computeMaxAllowedDate(List<Release> releases) {
        int half = releases.size() / 2;
        List<Release> firstHalf = releases.subList(0, half);
        return firstHalf.get(firstHalf.size() - 1).date;
    }

    private static Map<String, Metric> buildMetricsMap(String project, String dataDir, LocalDateTime maxAllowedDate)
            throws IOException, CsvValidationException {

        Map<String, Metric> map = new HashMap<>();
        try (CSVReader reader = new CSVReader(new FileReader(dataDir + project + "_Metrics.csv"))) {
            reader.readNext(); // skip header
            String[] c;
            while ((c = reader.readNext()) != null) {
                processCommitLine(map, c, maxAllowedDate);
            }
        }
        return map;
    }

    private static void processCommitLine(Map<String, Metric> map, String[] c, LocalDateTime maxAllowedDate) {
        LocalDateTime commitDate = parseDate(c[1]);
        if (commitDate.isAfter(maxAllowedDate)) return;

        String file = c[3];
        boolean isFix = c[6].equals("true");
        int added = c[4].equals("-") ? 0 : Integer.parseInt(c[4]);
        int deleted = c[5].equals("-") ? 0 : Integer.parseInt(c[5]);

        Metric m = map.getOrDefault(file, new Metric());
        m.locAdded += added;
        m.locDeleted += deleted;
        m.nRev++;
        if (isFix) {
            m.nFix++;
            m.buggy = true;
        }
        m.authors.add(c[2]);
        if (m.date == null || commitDate.isAfter(m.date))
            m.date = commitDate;

        m.locTouched = m.locAdded + m.locDeleted;
        m.churn = m.nRev > 0 ? (double) m.locTouched / m.nRev : 0;

        map.put(file, m);
    }

    private static void writeDatasetCSV(String project, List<Release> releases, Map<String, Metric> map) {
        String outFileName = FileWriterUtils.prepareOutputDataFilePath(project + ".csv");

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outFileName))) {
            writer.write("Project,Version,File,LOC_Added,LOC_Deleted,LOC_Touched,Churn,NR,NFix,NAuth,Buggy\n");

            for (Map.Entry<String, Metric> entry : map.entrySet()) {
                String f = entry.getKey();
                Metric m = entry.getValue();
                String version = findReleaseForDate(releases, m.date);

                writer.write(String.join(",",
                        project,
                        version,
                        f,
                        String.valueOf(m.locAdded),
                        String.valueOf(m.locDeleted),
                        String.valueOf(m.locTouched),
                        String.format("%.2f", m.churn),
                        String.valueOf(m.nRev),
                        String.valueOf(m.nFix),
                        String.valueOf(m.authors.size()),
                        m.buggy ? "Yes" : "No"));
                writer.newLine();
            }

        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error writing CSV file", e);
        }
    }

    // Load all releases sorted by date
    private static List<Release> loadReleases(String csvPath) throws IOException, CsvValidationException {
        List<Release> releases = new ArrayList<>();

        try (CSVReader reader = new CSVReader(new FileReader(csvPath))) {
            reader.readNext(); // skip header
            String[] r;
            while ((r = reader.readNext()) != null) {
                String name = r[2];
                String dateStr = r[3];
                LocalDateTime date = LocalDateTime.parse(dateStr);
                releases.add(new Release(name, date));
            }
        }

        releases.sort(Comparator.comparing(o -> o.date));
        return releases;
    }

    // Find the latest release <= commit date
    private static String findReleaseForDate(List<Release> releases, LocalDateTime commitDate) {
        String version = "Pre-Release";
        for (Release r : releases) {
            if (!r.date.isAfter(commitDate)) {
                version = r.name;
            } else {
                break;
            }
        }
        return version;
    }

    // Robust parsing of commit data (git format)
    private static LocalDateTime parseDate(String dateStr) {
        try {
            // Example: 2020-03-05 14:22:12 +0000
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss Z");
            return LocalDateTime.parse(dateStr, fmt);
        } catch (Exception e) {
            try {
                // fallback (ISO)
                return LocalDateTime.parse(dateStr);
            } catch (Exception ex) {
                return LocalDateTime.now();
            }
        }
    }
}
