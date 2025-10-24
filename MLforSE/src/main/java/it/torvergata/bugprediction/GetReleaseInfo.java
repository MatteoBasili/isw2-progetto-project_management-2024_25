package it.torvergata.bugprediction;

import org.json.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
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
        try {
            JSONArray versions = getVersions();

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

            // Sort by date
            releases.sort(Comparator.naturalOrder());

            // Check minimum number of versions
            if (releases.size() < 6) {
                System.out.println("Number of versions too low (" + releases.size() + "). Interrupted.");
                return;
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error during processing", e);
        }

        File outDir = new File("data");

        // Name of CSV for output
        String fileName = PROJECT_KEY + "VersionInfo.csv";
        String outFileName;
        if (!outDir.exists()) {
            boolean created = outDir.mkdirs();
            if (!created) {
                LOGGER.warning("Could not create directory 'data'. Using current folder instead.");
                outFileName = fileName; // fallback to the current folder
            } else {
                outFileName = "data/" + fileName;
            }
        } else {
            outFileName = "data/" + fileName;
        }

        FileWriter fileWriter = null;
        int numVersions = releases.size();
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

    /** Reads versions from the JIRA REST service */
    private static JSONArray getVersions() throws IOException, JSONException {
        URL url = new URL(API_URL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.connect();

        if (conn.getResponseCode() != 200) { throw new IOException("HTTP Error: " + conn.getResponseCode()); }

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
            reader.close();

            JSONObject json = new JSONObject(sb.toString());
            return json.getJSONArray("versions");
        } finally {
            conn.disconnect();
        }
    }
}

