package it.torvergata.bugprediction;

import it.torvergata.bugprediction.utils.JsonUtils;
import it.torvergata.bugprediction.utils.FileWriterUtils;
import org.json.*;
import java.io.*;
import java.net.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GetReleaseInfo {

    private static final Logger LOGGER = Logger.getLogger(GetReleaseInfo.class.getName());
    private static final String PROJECT_KEY = "BOOKKEEPER"; // change to "STORM" for the other project
    private static final String API_URL = "https://issues.apache.org/jira/rest/api/2/project/" + PROJECT_KEY;

    private static final HashMap<LocalDateTime, String> releaseNames = new HashMap<>();
    private static final HashMap<LocalDateTime, String> releaseIDs = new HashMap<>();
    private static final ArrayList<LocalDateTime> releases = new ArrayList<>();

    public static void main(String[] args) {
        try {
            // Reads versions from the JIRA REST service
            JSONObject json = JsonUtils.getJsonObject(new URL(API_URL));
            JSONArray versions = json.getJSONArray("versions");

            for (int i = 0; i < versions.length(); i++) {
                String name = "";
                String id = "";
                JSONObject v = versions.getJSONObject(i);

                // Ignores releases with missing dates
                if (v.has("releaseDate")) {
                    if (v.has("name"))
                        name = v.get("name").toString();
                    if (v.has("id"))
                        id = v.get("id").toString();
                    addRelease(v.get("releaseDate").toString(),
                            name, id);
                }
            }

            // Sort by date
            releases.sort(Comparator.naturalOrder());

            // Use only the first half of the releases
            int half = releases.size() / 2;
            List<LocalDateTime> firstHalf = new ArrayList<>(releases.subList(0, half));

            // Update maps to contain only the first half
            HashMap<LocalDateTime, String> filteredReleaseNames = new HashMap<>();
            HashMap<LocalDateTime, String> filteredReleaseIDs = new HashMap<>();

            for (LocalDateTime dt : firstHalf) {
                filteredReleaseNames.put(dt, releaseNames.get(dt));
                filteredReleaseIDs.put(dt, releaseIDs.get(dt));
            }

            // Replace the original maps with the filtered ones
            releaseNames.clear();
            releaseNames.putAll(filteredReleaseNames);

            releaseIDs.clear();
            releaseIDs.putAll(filteredReleaseIDs);

            // Replace the release list with just the first half
            releases.clear();
            releases.addAll(firstHalf);

            // Check minimum number of versions
            if (releases.size() < 6) {
                LOGGER.log(Level.WARNING, "Number of versions too low ({0}). Interrupted.", releases.size());
                return;
            }

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error during processing", e);
        }

        // Name of CSV for output
        String fileName = PROJECT_KEY + "VersionInfo.csv";
        String outFileName = FileWriterUtils.prepareOutputDataFilePath(fileName);

        int numVersions = 0;

        try (FileWriter fileWriter = new FileWriter(outFileName)) {
            fileWriter.append("Index,VersionID,Name,Date\n");

            numVersions = releases.size();
            for (int i = 0; i < numVersions; i++) {
                int index = i + 1;
                fileWriter.append(Integer.toString(index))
                        .append(",")
                        .append(releaseIDs.get(releases.get(i)))
                        .append(",")
                        .append(releaseNames.get(releases.get(i)))
                        .append(",")
                        .append(releases.get(i).toString())
                        .append("\n");
            }

        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error writing CSV file", e);
        }

        LOGGER.log(Level.INFO, "Release info saved in {0}", outFileName);
        LOGGER.log(Level.INFO, "Total valid versions: {0}", numVersions);
    }

    /** Adds a release avoiding duplicates and managing the date */
    private static void addRelease(String dateStr, String name, String id) {
        LocalDate date = LocalDate.parse(dateStr);
        LocalDateTime dateTime = date.atStartOfDay();
        if (!releases.contains(dateTime))
            releases.add(dateTime);
        releaseNames.put(dateTime, name);
        releaseIDs.put(dateTime, id);
    }

}
