package it.torvergata.bugprediction;

import it.torvergata.bugprediction.utils.JsonUtils;
import it.torvergata.bugprediction.utils.FileWriterUtils;
import org.json.*;
import java.io.*;
import java.net.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RetrieveTicketsID {

    private static final Logger LOGGER = Logger.getLogger(RetrieveTicketsID.class.getName());
    private static final String PROJECT_KEY = "BOOKKEEPER"; // o "STORM"
    private static final String JIRA_API_SEARCH =
            "https://issues.apache.org/jira/rest/api/2/search?jql=project=%22"
                    + PROJECT_KEY + "%22AND%22issueType%22=%22Bug%22AND(%22status%22=%22closed%22OR"
                    + "%22status%22=%22resolved%22)AND%22resolution%22=%22fixed%22&fields=key,resolutiondate,versions,created";

    public static void main(String[] args) {
        String fileName = PROJECT_KEY + "_Tickets.csv";
        String outFileName = FileWriterUtils.prepareOutputDataFilePath(fileName);

        int startAt = 0;
        int maxResults = 1000;
        boolean more = true;

        try (FileWriter fileWriter = new FileWriter(outFileName)) {
            fileWriter.append("TicketID\n");

            // Only gets a max of 1000 at a time, so must do this multiple times if bugs > 1000
            while (more) {
                // Get JSON API for closed bugs w/ AV in the project
                String queryUrl = JIRA_API_SEARCH + "&startAt=" + startAt + "&maxResults=" + maxResults;
                JSONObject json = JsonUtils.getJsonObject(new URL(queryUrl));
                JSONArray issues = json.getJSONArray("issues");

                for (int i = 0; i < issues.length(); i++) {
                    fileWriter.append(issues.getJSONObject(i).getString("key")).append("\n");
                }

                int total = json.getInt("total");
                startAt += maxResults;
                more = startAt < total;
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error writing CSV file", e);
        }

        LOGGER.log(Level.INFO, "Tickets saved in {0}", outFileName);
    }

}
