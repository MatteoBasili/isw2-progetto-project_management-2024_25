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
        int locAdded = 0, locDeleted = 0, nRev = 0, nFix = 0;
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

        // Upload releases
        List<Release> releases = loadReleases(dataDir + "BOOKKEEPERVersionInfo.csv");

        // Calculate half of the releases
        int half = releases.size() / 2;
        List<Release> firstHalfReleases = releases.subList(0, half);

        // Maximum allowed commit date
        LocalDateTime maxAllowedDate = firstHalfReleases.get(firstHalfReleases.size() - 1).date;

        // Upload metrics
        Map<String, Metric> map = new HashMap<>();
        CSVReader reader = new CSVReader(new FileReader(dataDir + project + "_Metrics.csv"));
        reader.readNext(); // skip header
        String[] c;

        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss Z");

        while ((c = reader.readNext()) != null) {
            String commitDateStr = c[1];
            LocalDateTime commitDate = parseDate(commitDateStr);

            // Commit filter by prime release period
            if (commitDate.isAfter(maxAllowedDate)) continue;

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
            // Update the date if it is newer
            if (m.date == null || commitDate.isAfter(m.date))
                m.date = commitDate;

            // Update derived metrics
            m.locTouched = m.locAdded + m.locDeleted;
            m.churn = m.nRev > 0 ? (double) m.locTouched / m.nRev : 0;

            map.put(file, m);
        }
        reader.close();

        // Create the CSV file
        String fileName = project + ".csv";
        String outFileName = FileWriterUtils.prepareOutputDataFilePath(fileName);
        FileWriter fileWriter = null;

        try {
            fileWriter = new FileWriter(outFileName);
            fileWriter.append("Project,Version,File,LOC_Added,LOC_Deleted,LOC_Touched,Churn,NR,NFix,NAuth,Buggy\n");

            for (String f : map.keySet()) {
                Metric m = map.get(f);
                String version = findReleaseForDate(releases, m.date);
                fileWriter.append(project).append(",")
                        .append(version).append(",")
                        .append(f).append(",")
                        .append(String.valueOf(m.locAdded)).append(",")
                        .append(String.valueOf(m.locDeleted)).append(",")
                        .append(String.valueOf(m.locTouched)).append(",")
                        .append(String.format("%.2f", m.churn)).append(",")
                        .append(String.valueOf(m.nRev)).append(",")
                        .append(String.valueOf(m.nFix)).append(",")
                        .append(String.valueOf(m.authors.size())).append(",")
                        .append(m.buggy ? "Yes" : "No").append("\n");
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error writing CSV file", e);
        } finally {
            try {
                assert fileWriter != null;
                fileWriter.flush();
                fileWriter.close();
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Error flushing/closing CSV writer", e);
            }
        }

        System.out.println("Final dataset created: " + outFileName);
    }

    // Load all releases sorted by date
    private static List<Release> loadReleases(String csvPath) throws IOException, CsvValidationException {
        List<Release> releases = new ArrayList<>();
        CSVReader reader = new CSVReader(new FileReader(csvPath));
        reader.readNext(); // skip header
        String[] r;
        while ((r = reader.readNext()) != null) {
            String name = r[2];
            String dateStr = r[3];
            LocalDateTime date = LocalDateTime.parse(dateStr);
            releases.add(new Release(name, date));
        }
        reader.close();
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
