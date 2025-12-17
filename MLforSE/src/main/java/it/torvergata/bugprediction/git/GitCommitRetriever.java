package it.torvergata.bugprediction.git;

import it.torvergata.bugprediction.model.Commit;
import it.torvergata.bugprediction.model.Ticket;
import it.torvergata.bugprediction.utils.DataUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extracts Git commits related to JIRA tickets.
 * Scans the entire repository and matches commits by regex pattern (PROJECT-XXXX).
 * Saves the mapping to a CSV file.
 */
public class GitCommitRetriever {

    private static final Logger LOGGER = Logger.getLogger(GitCommitRetriever.class.getName());

    private final String project;      // e.g., "BOOKKEEPER"
    private final String repoPath;     // local path of the cloned repository
    private final List<Ticket> tickets;

    public GitCommitRetriever(String project, String repoPath, List<Ticket> tickets) {
        this.project = project;
        this.repoPath = repoPath;
        this.tickets = tickets;
    }

    /**
     * Extracts commits related to JIRA tickets and saves them to a CSV file.
     */
    public void retrieveCommits() {
        String fileName = project + "_Commit_BugMapping.csv";
        String outFileName = DataUtils.prepareOutputDataFilePath(fileName);

        LOGGER.log(Level.INFO, "Starting commit retrieval for project {0}", project);

        Map<String, List<Commit>> ticketToCommits = findAllTicketCommits();

        // Sort commit lists by date (oldest to newest)
        List<Map.Entry<String, Commit>> allCommits = new ArrayList<>();
        for (Map.Entry<String, List<Commit>> entry : ticketToCommits.entrySet()) {
            for (Commit c : entry.getValue()) {
                allCommits.add(new AbstractMap.SimpleEntry<>(entry.getKey(), c));
            }
        }
        allCommits.sort(Comparator.comparing(entry -> parseIsoDateSafely(entry.getValue())));

        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(outFileName), StandardCharsets.UTF_8))) {

            writer.write("Ticket_ID,Commit_Hash,Author,Date,Message,Changed_Files\n");

            for (Map.Entry<String, Commit> entry : allCommits) {
                Commit c = entry.getValue();
                String ticketID = entry.getKey();

                writer.write(String.join(",",
                        ticketID,
                        c.getHash(),
                        escapeCsv(c.getAuthor()),
                        c.getDate(),
                        escapeCsv(c.getMessage()),
                        escapeCsv(String.join(";", c.getChangedFiles()))
                ));
                writer.write("\n");
            }

            LOGGER.log(Level.INFO, "Commit mapping saved to {0}", outFileName);

        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error writing commit mapping file", e);
        }
    }

    /**
     * Scans the entire Git history once and builds a map ticketID → list of commits.
     */
    private Map<String, List<Commit>> findAllTicketCommits() {
        Map<String, List<Commit>> mapping = new HashMap<>();

        // Regex to find JIRA-style IDs (case-insensitive)
        Pattern ticketPattern = Pattern.compile(project + "-\\d+", Pattern.CASE_INSENSITIVE);

        try {
            List<String> command = Arrays.asList(
                    "git", "log", "--date=iso", "--name-only",
                    "--pretty=format:%H|||%an|||%ad|||%s"
            );

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.directory(new File(repoPath));
            pb.redirectErrorStream(true);

            Process process = pb.start();

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {

                String line;
                Commit current = null;

                while ((line = reader.readLine()) != null) {
                    if (line.trim().isEmpty()) {
                        if (current != null) {
                            // Extract ticket IDs from message
                            Matcher m = ticketPattern.matcher(current.getMessage());
                            while (m.find()) {
                                String ticketID = m.group().toUpperCase();
                                mapping.computeIfAbsent(ticketID, k -> new ArrayList<>()).add(current);
                            }
                            current = null;
                        }
                        continue;
                    }

                    if (line.contains("|||")) {
                        String[] parts = line.split("\\|\\|\\|", 4);
                        if (parts.length == 4) {
                            current = new Commit(parts[0], parts[1], parts[2], parts[3]);
                        }
                    } else if (current != null) {
                        current.getChangedFiles().add(line.trim());
                    }
                }
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                LOGGER.log(Level.WARNING, "Git command exited with code {0}", exitCode);
            }

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error while reading git log", e);
        }

        // Filter commits: keep only those related to tickets in the provided list
        if (tickets != null && !tickets.isEmpty()) {
            Set<String> validTicketIds = new HashSet<>();
            for (Ticket t : tickets) {
                validTicketIds.add(t.getId().toUpperCase());
            }
            mapping.keySet().retainAll(validTicketIds);
        }

        return mapping;
    }

    /**
     * Escapes commas and newlines for CSV safety.
     */
    private String escapeCsv(String text) {
        if (text == null) return "";
        String escaped = text.replace("\"", "\"\"").replace("\n", " ").replace("\r", " ");
        if (escaped.contains(",") || escaped.contains("\"")) {
            escaped = "\"" + escaped + "\"";
        }
        return escaped;
    }

    private static Date parseIsoDateSafely(Commit commit) {
        try {
            // Example: 2020-05-14 10:45:23 +0000
            return new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z").parse(commit.getDate());
        } catch (Exception e) {
            return new Date(0); // if not parsable, put at the beginning
        }
    }
}
