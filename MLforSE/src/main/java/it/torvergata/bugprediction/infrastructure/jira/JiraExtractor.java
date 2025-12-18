package it.torvergata.bugprediction.infrastructure.jira;

import it.torvergata.bugprediction.exceptions.GitException;
import it.torvergata.bugprediction.infrastructure.git.GitRepositoryMiner;
import it.torvergata.bugprediction.models.Release;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;

import static it.torvergata.bugprediction.utils.JsonUtils.readJsonFromUrl;

public class JiraExtractor {

    private final Logger logger;
    private final String projectName;

    public JiraExtractor(String projectName) {
        logger = Logger.getLogger(JiraExtractor.class.getName());
        this.projectName = projectName.toUpperCase();
    }

    public List<Release> extractReleases(GitRepositoryMiner gitRepoMiner) throws IOException, GitAPIException {
        String url = "https://issues.apache.org/jira/rest/api/2/project/" + projectName;

        List<Release> releases = new ArrayList<>();

        JSONObject json = readJsonFromUrl(url);
        JSONArray versions = json.getJSONArray("versions");

        // Crea la lista delle release
        for(int i = 0; i < versions.length(); i++) {
            String releaseID = null;
            String releaseName = null;
            String releaseDateString;
            JSONObject version = versions.getJSONObject(i);
            if(version.has("id")) releaseID = version.getString("id");
            if(version.has("name")) releaseName = version.getString("name");
            if(version.has("releaseDate")) {
                releaseDateString = version.getString("releaseDate");
            } else {
                try {
                    releaseDateString = String.valueOf(gitRepoMiner.getReleaseDate(releaseName));
                } catch(GitException e) {
                    logger.info("[INFO] " + e.getMessage());
                    continue;
                }
            }

            Release newRelease = new Release(releaseID, releaseName, releaseDateString);
            releases.add(newRelease);

        }

        // Ordina la lista delle release per data
        releases.sort(Comparator.comparing(Release::getReleaseDateTime));

        // Elimina le release che non rappresentano una vera versione del codice
        gitRepoMiner.filterTaggedReleases(releases);

        // Imposta l'ultima release
        //gitRepoMiner.setLastRelease(releases.getLast());

        return releases;
    }

}
