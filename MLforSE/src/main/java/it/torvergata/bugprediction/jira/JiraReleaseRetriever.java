package it.torvergata.bugprediction.jira;

import it.torvergata.bugprediction.model.Release;
import it.torvergata.bugprediction.utils.DataUtils;
import it.torvergata.bugprediction.utils.JsonUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.FileWriter;
import java.net.URL;
import java.time.LocalDate;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class JiraReleaseRetriever {

    private static final Logger LOGGER = Logger.getLogger(JiraReleaseRetriever.class.getName());
    private final String project;

    public JiraReleaseRetriever(String project) {
        this.project = project;
    }

    public List<Release> retrieveReleases() {
        List<Release> releases = new ArrayList<>();
        String apiUrl = "https://issues.apache.org/jira/rest/api/2/project/" + project;

        LOGGER.log(Level.INFO, "Starting release information retrieval for {0}", project);

        try {
            JSONObject json = JsonUtils.getJsonObject(new URL(apiUrl));
            JSONArray versions = json.getJSONArray("versions");

            for (int i = 0; i < versions.length(); i++) {
                JSONObject v = versions.getJSONObject(i);
                if (v.has("releaseDate")) {
                    String name = v.optString("name", "");
                    String id = v.optString("id", "");
                    LocalDate date = LocalDate.parse(v.getString("releaseDate"));
                    releases.add(new Release(id, name, date.atStartOfDay()));
                }
            }

            Collections.sort(releases);

            if (releases.size() < 6) {
                LOGGER.log(Level.WARNING, "Number of releases too low ({0}). Aborting.", releases.size());
                return Collections.emptyList();
            }

            // Keep only the first half
            //releases = new ArrayList<>(releases.subList(0, releases.size() / 2));

            writeReleasesToCsv(releases);

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error retrieving release information", e);
        }

        return releases;
    }

    private void writeReleasesToCsv(List<Release> releases) {
        String fileName = project + "_Version_Info.csv";
        String outFileName = DataUtils.prepareOutputDataFilePath(fileName);

        try (FileWriter fileWriter = new FileWriter(outFileName)) {
            fileWriter.append("Index,Version_ID,Version_Name,Release_Date\n");

            for (int i = 0; i < releases.size(); i++) {
                Release r = releases.get(i);
                fileWriter.append(String.valueOf(i + 1)).append(",")
                        .append(r.getId()).append(",")
                        .append(r.getName()).append(",")
                        .append(r.getDate().toString()).append("\n");
            }

            LOGGER.log(Level.INFO, "Release information saved to {0}", outFileName);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error writing release CSV file", e);
        }
    }
}
