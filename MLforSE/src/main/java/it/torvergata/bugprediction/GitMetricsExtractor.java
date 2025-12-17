package it.torvergata.bugprediction;

import it.torvergata.bugprediction.exceptions.GitCloneException;
import it.torvergata.bugprediction.utils.DataUtils;

import java.io.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GitMetricsExtractor {

    private static final Logger LOGGER = Logger.getLogger(GitMetricsExtractor.class.getName());

    public static void main(String[] args) throws Exception {
        String projectName = "bookkeeper";
        String basePath = "projects";
        File projectDir = new File(basePath, projectName);
        String projectPath = projectDir.getPath();
        String repoUrl = "https://github.com/apache/bookkeeper.git";

        cloneRepoIfNeeded(projectName, projectPath, repoUrl);

        Set<String> validTickets = loadTickets("data/" + projectName + "_Tickets.csv");
        String ticketPrefix = projectName.toUpperCase() + "-";

        String outFileName = DataUtils.prepareOutputDataFilePath(projectName + "_Metrics.csv");
        extractGitMetrics(projectPath, outFileName, validTickets, ticketPrefix);

        LOGGER.log(Level.INFO, "Metrics extracted in {0}", outFileName);
    }

    private static void cloneRepoIfNeeded(String projectName, String projectPath, String repoUrl) throws GitCloneException {
        File repoDir = new File(projectPath);
        if (!repoDir.exists()) {
            LOGGER.log(Level.INFO, "Cloning the repository {0}...", projectName);
            ProcessBuilder clonePb = new ProcessBuilder("git", "clone", repoUrl, projectPath);
            clonePb.redirectErrorStream(true);
            try {
                Process cloneProcess = clonePb.start();

                try (BufferedReader cloneReader = new BufferedReader(
                        new InputStreamReader(cloneProcess.getInputStream()))) {
                    String line;
                    while ((line = cloneReader.readLine()) != null) {
                        LOGGER.log(Level.INFO, "{0}", line);
                    }
                }

                int exitCode = cloneProcess.waitFor();
                if (exitCode != 0) {
                    throw new GitCloneException("Error cloning repository: git process exited with code " + exitCode);
                }

                LOGGER.log(Level.INFO, "Cloning completed successfully.");
            } catch (IOException e) {
                throw new GitCloneException("Error cloning repository", e);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new GitCloneException("Thread was interrupted while cloning repository", e);
            }

        } else {
            LOGGER.log(Level.INFO, "Repository already present locally. Proceeding with analysis...");
        }
    }

    private static void extractGitMetrics(String projectPath, String outFileName,
                                          Set<String> validTickets, String ticketPrefix) {
        try (FileWriter fileWriter = new FileWriter(outFileName)) {
            fileWriter.append("CommitID,Date,Author,File,LOC_Added,LOC_Deleted,TicketLinked,IsFix,ChgSetSize\n");

            ProcessBuilder pb = new ProcessBuilder("git", "-C", projectPath,
                    "log", "--numstat", "--date=iso", "--pretty=format:COMMIT:%H;%ad;%an;%s");
            pb.redirectErrorStream(true);
            Process process = pb.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                String currentCommit = "";
                String date = "";
                String author = "";
                String message;
                boolean ticketLinked = false;
                boolean isFix = false;
                List<String[]> currentFiles = new ArrayList<>();

                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("COMMIT:")) {
                        // Flush previous commit data
                        if (!currentFiles.isEmpty() && !currentCommit.isEmpty()) {
                            writeCommitData(fileWriter, currentCommit, date, author, currentFiles, ticketLinked, isFix);
                            currentFiles.clear();
                        }

                        // Start a new commit
                        String[] parts = line.split(";", 4);
                        currentCommit = parts[0].split(":")[1];
                        date = parts[1];
                        author = parts[2];
                        message = parts.length >= 4 ? parts[3].trim() : "";

                        String finalMessage = message;
                        ticketLinked = validTickets.stream()
                                .anyMatch(ticket -> finalMessage.contains(ticketPrefix + ticket));
                        isFix = message.toLowerCase().contains("fix") || ticketLinked;

                    } else if (!line.trim().isEmpty()) {
                        String[] parts = line.split("\t");
                        if (parts.length == 3 && parts[2].endsWith(".java")) {
                            currentFiles.add(parts);
                        }
                    }
                }

                // Handle last commit
                if (!currentFiles.isEmpty() && !currentCommit.isEmpty()) {
                    writeCommitData(fileWriter, currentCommit, date, author, currentFiles, ticketLinked, isFix);
                }
            }

            process.waitFor();

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error writing CSV file", e);
        }
    }

    private static void writeCommitData(FileWriter fileWriter, String commitId, String date,
                                        String author, List<String[]> files,
                                        boolean ticketLinked, boolean isFix) throws IOException {
        int chgSetSize = files.size();
        for (String[] fileData : files) {
            fileWriter.append(commitId).append(",")
                    .append(date).append(",")
                    .append("\"").append(author).append("\"").append(",")
                    .append("\"").append(fileData[2]).append("\"").append(",")
                    .append(fileData[0]).append(",")
                    .append(fileData[1]).append(",")
                    .append(String.valueOf(ticketLinked)).append(",")
                    .append(String.valueOf(isFix)).append(",")
                    .append(String.valueOf(chgSetSize)).append("\n");
        }
    }

    // Function to load all valid JIRA tickets from CSV
    private static Set<String> loadTickets(String ticketsCsvPath) {
        Set<String> tickets = new HashSet<>();

        try (BufferedReader br = new BufferedReader(new FileReader(ticketsCsvPath))) {
            br.lines()
                    .skip(1) // skip header
                    .map(String::trim) // removes leading/trailing whitespace
                    .filter(line -> !line.isEmpty()) // avoid empty lines
                    .forEach(tickets::add);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, e, () -> "Error reading tickets CSV file: " + ticketsCsvPath);
        }
        return tickets;
    }
}
