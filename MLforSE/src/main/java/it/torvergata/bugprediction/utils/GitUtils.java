package it.torvergata.bugprediction.utils;

import it.torvergata.bugprediction.exceptions.GitCloneException;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.time.LocalDate;
import java.util.logging.Level;
import java.util.logging.Logger;

import static it.torvergata.bugprediction.utils.JsonUtils.readUrl;

public class GitUtils {

    private static final Logger LOGGER = Logger.getLogger(GitUtils.class.getName());
    private static final String REPO_BASE_PATH = "projects/";

    public static String cloneRepository(String project) throws GitCloneException {
        String projectName = project.toLowerCase();
        File repoDir = new File(REPO_BASE_PATH + projectName);
        if (!repoDir.exists()) {
            LOGGER.log(Level.INFO, "Cloning the repository {0}...", project);
            String repoUrl = "https://github.com/apache/" + projectName + ".git";
            ProcessBuilder clonePb = new ProcessBuilder("git", "clone", "--branch", "master",
                    repoUrl, repoDir.getPath());
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

        return repoDir.getAbsolutePath();
    }

    /**
     * Gets the release/tag date from GitHub for a specific project.
     */
    public static LocalDate getTagDate(String project, String tagName) {
        try {
            String apiUrl = "https://api.github.com/repos/apache/" + project.toLowerCase() + "/tags";
            String jsonStr = readUrl(new URL(apiUrl));
            JSONArray tags = new JSONArray(jsonStr);

            for (int i = 0; i < tags.length(); i++) {
                JSONObject tag = tags.getJSONObject(i);
                if (tag.getString("name").equalsIgnoreCase(tagName)) {
                    String commitUrl = tag.getJSONObject("commit").getString("url");
                    JSONObject commitJson = JsonUtils.getJsonObject(new URL(commitUrl));
                    String dateStr = commitJson.getJSONObject("commit").getJSONObject("committer").getString("date");
                    return LocalDate.parse(dateStr.substring(0, 10));
                }
            }
        } catch (Exception e) {
            LOGGER.warning("Error retrieving tag date for " + project + " / " + tagName + ": " + e.getMessage());
        }
        return null;
    }

    /**
     * Checks whether a tag exists on GitHub.
     */
    public static boolean tagExists(String project, String tagName) {
        return getTagDate(project, tagName) != null;
    }

    /**
     * Checks whether a release has commits (simplified: true if tag exists).
     */
    public static boolean hasCommits(String project, String tagName) {
        return tagExists(project, tagName);
    }
}
