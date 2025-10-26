package it.torvergata.bugprediction;

import it.torvergata.bugprediction.utils.FileWriterUtils;

import java.io.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GitMetricsExtractor {

    private static final Logger LOGGER = Logger.getLogger(GitMetricsExtractor.class.getName());

    public static void main(String[] args) throws Exception {
        // Choose the project: "bookkeeper" o "storm"
        String projectName = "bookkeeper";
        String basePath = "projects";
        String projectPath = basePath + "/" + projectName;
        String repoUrl;

        // Set remote Git URL depending on the project you choose
        repoUrl = "https://github.com/apache/bookkeeper.git";

        // If the repository doesn't exist locally, clone
        File repoDir = new File(projectPath);
        if (!repoDir.exists()) {
            System.out.println("Cloning the repository " + projectName + "...");
            ProcessBuilder clonePb = new ProcessBuilder("git", "clone", repoUrl, projectPath);
            clonePb.redirectErrorStream(true);
            Process cloneProcess = clonePb.start();

            try (BufferedReader cloneReader = new BufferedReader(
                    new InputStreamReader(cloneProcess.getInputStream()))) {
                String line;
                while ((line = cloneReader.readLine()) != null) {
                    System.out.println(line);
                }
            }

            int exitCode = cloneProcess.waitFor();
            if (exitCode != 0) {
                throw new RuntimeException("Error cloning repository!");
            }
            System.out.println("Cloning completed successfully.");
        } else {
            System.out.println("Repository already present locally. I proceed with the analysis...");
        }

        // Load all valid tickets
        Set<String> validTickets = loadTickets("data/" + projectName + "_Tickets.csv");

        // Defines ticket prefix
        String ticketPrefix = projectName.toUpperCase() + "-";

        // Create the CSV file
        String fileName = projectName + "_Metrics.csv";
        String outFileName = FileWriterUtils.prepareOutputDataFilePath(fileName);
        FileWriter fileWriter = null;

        try {
            fileWriter = new FileWriter(outFileName);
            fileWriter.append("CommitID,Date,Author,File,LOC_Added,LOC_Deleted,TicketLinked\n");

            // Runs git log with details and statistics per file
            ProcessBuilder pb = new ProcessBuilder("git", "-C", projectPath,
                    "log", "--numstat", "--date=iso", "--pretty=format:COMMIT:%H;%ad;%an;%s");
            pb.redirectErrorStream(true);
            Process process = pb.start();

            // Reads the git log output
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line, currentCommit = "", date = "", author = "", message;
            boolean ticketLinked = false;

            while ((line = reader.readLine()) != null) {
                if (line.startsWith("COMMIT:")) {
                    String[] parts = line.split(";", 4);
                    currentCommit = parts[0].split(":")[1];
                    date = parts[1];
                    author = parts[2];
                    message = parts[3];

                    // Reset ticketLinked for commit
                    ticketLinked = false;
                    for (String ticket : validTickets) {
                        if (message.contains(ticketPrefix + ticket)) {
                            ticketLinked = true;
                            break;
                        }
                    }

                } else if (!line.trim().isEmpty()) {
                    String[] parts = line.split("\t");
                    if (parts.length == 3) {
                        // FILTER .java
                        if (!parts[2].endsWith(".java")) continue;

                        fileWriter.append(currentCommit).append(",")
                                .append(date).append(",")
                                .append("\"").append(author).append("\"").append(",")
                                .append("\"").append(parts[2]).append("\"").append(",")
                                .append(parts[0]).append(",")
                                .append(parts[1]).append(",")
                                .append(String.valueOf(ticketLinked)).append("\n");
                    }
                }
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

        System.out.println("Metrics extracted in " + outFileName);
    }

    // Function to load all valid JIRA tickets from CSV
    private static Set<String> loadTickets(String ticketsCsvPath) {
        Set<String> tickets = new HashSet<>();
        try (BufferedReader br = new BufferedReader(new FileReader(ticketsCsvPath))) {
            String line;
            br.readLine(); // skip header
            while ((line = br.readLine()) != null) {
                tickets.add(line.trim());
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error reading tickets CSV file: " + ticketsCsvPath, e);
        }
        return tickets;
    }
}
