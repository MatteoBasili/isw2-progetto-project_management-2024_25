package it.torvergata.bugprediction.jira;

import it.torvergata.bugprediction.model.Ticket;
import it.torvergata.bugprediction.utils.DataUtils;
import it.torvergata.bugprediction.utils.JsonUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.FileWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class JiraTicketRetriever {

    private static final Logger LOGGER = Logger.getLogger(JiraTicketRetriever.class.getName());
    private final String project;

    public JiraTicketRetriever(String project) {
        this.project = project;
    }

    public List<Ticket> retrieveTickets() {
        List<Ticket> tickets = new ArrayList<>();

        String jiraApiSearch =
                "https://issues.apache.org/jira/rest/api/2/search?jql=project=%22"
                        + project + "%22AND%22issueType%22=%22Bug%22AND(%22status%22=%22closed%22OR"
                        + "%22status%22=%22resolved%22)AND%22resolution%22=%22fixed%22&fields=key,resolutiondate,versions,created";

        String fileName = project + "_Bug_Tickets.csv";
        String outFileName = DataUtils.prepareOutputDataFilePath(fileName);

        int startAt = 0;
        int maxResults = 1000;
        boolean more = true;

        LOGGER.log(Level.INFO, "Starting ticket retrieval from JIRA for {0}", project);

        try (FileWriter fileWriter = new FileWriter(outFileName)) {
            fileWriter.append("Ticket_ID\n");

            while (more) {
                String queryUrl = jiraApiSearch + "&startAt=" + startAt + "&maxResults=" + maxResults;
                JSONObject json = JsonUtils.getJsonObject(new URL(queryUrl));
                JSONArray issues = json.getJSONArray("issues");

                for (int i = 0; i < issues.length(); i++) {
                    JSONObject issue = issues.getJSONObject(i);
                    String ticketID = issue.getString("key");
                    tickets.add(new Ticket(ticketID));
                }

                int total = json.getInt("total");
                startAt += maxResults;
                more = startAt < total;
            }

            // Sort tickets by ID
            //tickets.sort(Comparator.comparingInt(t -> extractTicketNumber(t.getId())));

            for (Ticket t : tickets) {
                fileWriter.append(t.getId()).append("\n");
            }

            LOGGER.log(Level.INFO, "Tickets saved to {0}", outFileName);

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error retrieving or writing tickets", e);
        }

        return tickets;
    }

    /**
     * Extracts the numeric part from the ticket ID (e.g. "PROJECT-123" -> 123)
     */
    private int extractTicketNumber(String ticketId) {
        try {
            String[] parts = ticketId.split("-");
            return Integer.parseInt(parts[parts.length - 1]);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Unable to parse ticket number from ID: {0}", ticketId);
            return Integer.MAX_VALUE; // Put at the bottom if not parsable
        }
    }
}
