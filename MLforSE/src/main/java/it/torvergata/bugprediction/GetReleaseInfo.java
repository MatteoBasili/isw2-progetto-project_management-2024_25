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
        int i;
        int numVersions = 0;

        try {
            // Reads versions from the JIRA REST service
            JSONObject json = JsonUtils.getJsonObject(new URL(API_URL));
            JSONArray versions = json.getJSONArray("versions");

            for (i = 0; i < versions.length(); i++) {
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

            numVersions = releases.size();

            // Sort by date
            releases.sort(Comparator.naturalOrder());

            // Check minimum number of versions
            if (numVersions < 6) {
                System.out.println("Number of versions too low (" + numVersions + "). Interrupted.");
                return;
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error during processing", e);
        }

        // Name of CSV for output
        String fileName = PROJECT_KEY + "VersionInfo.csv";
        String outFileName = FileWriterUtils.prepareOutputDataFilePath(fileName);

        FileWriter fileWriter = null;
        try {
            fileWriter = new FileWriter(outFileName);
            fileWriter.append("Index,VersionID,Name,Date");
            fileWriter.append("\n");

            for (i = 0; i < numVersions; i++) {
                int index = i + 1;
                fileWriter.append(Integer.toString(index));
                fileWriter.append(",");
                fileWriter.append(releaseIDs.get(releases.get(i)));
                fileWriter.append(",");
                fileWriter.append(releaseNames.get(releases.get(i)));
                fileWriter.append(",");
                fileWriter.append(releases.get(i).toString());
                fileWriter.append("\n");
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

        System.out.println("Release info saved in " + outFileName);
        System.out.println("Total valid versions: " + numVersions);
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
