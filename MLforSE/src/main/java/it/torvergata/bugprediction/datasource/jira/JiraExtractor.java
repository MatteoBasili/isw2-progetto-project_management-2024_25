package it.torvergata.bugprediction.datasource.jira;

import it.torvergata.bugprediction.exceptions.GitException;
import it.torvergata.bugprediction.datasource.git.GitRepositoryAnalyzer;
import it.torvergata.bugprediction.models.Release;
import it.torvergata.bugprediction.models.Ticket;
import it.torvergata.bugprediction.service.ReleaseService;
import it.torvergata.bugprediction.utils.JsonUtils;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

import static it.torvergata.bugprediction.utils.JsonUtils.readJsonFromUrl;

public class JiraExtractor {

    private final Logger logger;
    private final String projectName;

    public JiraExtractor(String projectName) {
        logger = Logger.getLogger(JiraExtractor.class.getName());
        this.projectName = projectName.toUpperCase();
    }

    public List<Release> extractReleases(GitRepositoryAnalyzer gitRepoAnalyzer) throws IOException, GitAPIException {
        String url = "https://issues.apache.org/jira/rest/api/2/project/" + projectName;

        List<Release> releases = new ArrayList<>();

        JSONObject json = readJsonFromUrl(url);
        JSONArray versions = json.getJSONArray("versions");

        // Crea la lista delle release
        for (int i = 0; i < versions.length(); i++) {
            String releaseID = null;
            String releaseName = null;
            String releaseDateString;
            JSONObject version = versions.getJSONObject(i);
            if (version.has("id")) releaseID = version.getString("id");
            if (version.has("name")) releaseName = version.getString("name");
            if (version.has("releaseDate")) {
                releaseDateString = version.getString("releaseDate");
            } else {
                try {
                    releaseDateString = String.valueOf(gitRepoAnalyzer.getReleaseDate(releaseName));
                } catch (GitException e) {
                    logger.info("[INFO] " + e.getMessage());
                    continue;
                }
            }

            Release newRelease = new Release(releaseID, releaseName, releaseDateString);
            releases.add(newRelease);

        }

        // Ordina la lista delle release per data
        releases.sort(Comparator.comparing(Release::getDateTime));

        // Elimina le release che non rappresentano una vera versione del codice
        gitRepoAnalyzer.removeUntaggedReleases(releases);

        return releases;
    }

    public List<Ticket> extractTickets(List<Release> releasesList) throws IOException {
        int total;
        int j;
        int i = 0;
        List<Ticket> ticketList = new ArrayList<>();

        do {
            // Ottieni 1000 ticket alla volta
            j = i + 1000;
            String url = "https://issues.apache.org/jira/rest/api/2/search?jql=project=%22"
                    + this.projectName + "%22AND%22issueType%22=%22Bug%22AND" +
                    "(%22status%22=%22Closed%22OR%22status%22=%22Resolved%22)" +
                    "AND%22resolution%22=%22Fixed%22&fields=key,versions,created,resolutiondate&startAt="
                    + i + "&maxResults=" + j;
            JSONObject json = JsonUtils.readJsonFromUrl(url);
            JSONArray issues = json.getJSONArray("issues");
            total = json.getInt("total");

            // Itera su ogni bug
            for (; i < total && i < j; i++) {
                // La chiave è il nome del problema segnalato, ad esempio: "BOOKKEEPER-1105"
                String key = issues.getJSONObject(i % 1000).get("key").toString();

                // Ottieni le date di creazione e risoluzione
                JSONObject fields = issues.getJSONObject(i % 1000).getJSONObject("fields");
                String creationDateString = fields.get("created").toString();
                String resolutionDateString = fields.get("resolutiondate").toString();
                LocalDate creationDate = LocalDate.parse(creationDateString.substring(0,10));
                LocalDate resolutionDate = LocalDate.parse(resolutionDateString.substring(0,10));

                // Ottieni le versioni affette dal bug (AV)
                JSONArray affectedVersionsArray = fields.getJSONArray("versions");

                // Ottieni la Opening Version del bug (OV)
                Optional<Release> openingVersion = Release.findFirstReleaseOnOrAfter(creationDate, releasesList);

                // Ottieni la Fixed version del bug (FV)
                Optional<Release> fixedVersion = Release.findFirstReleaseOnOrAfter(resolutionDate, releasesList);

                // Ottieni la lista delle AV ordinata per data
                List<Release> affectedVersionsList = ReleaseService.getAffectedVersions(affectedVersionsArray, releasesList);

                // Controlli di consistenza:
                if (openingVersion.isEmpty() || fixedVersion.isEmpty() ||   // Se non ci sono OV o FV, il ticket non è necessario
                        openingVersion.get().getDateTime().isAfter(fixedVersion.get().getDateTime()) ||   // OV <= FV
                        (!affectedVersionsList.isEmpty() &&
                                (openingVersion.get().getDateTime().isBefore(affectedVersionsList.get(0).getDateTime()) ||   // AV1 <= OV
                                        !fixedVersion.get().getDateTime().isAfter(affectedVersionsList.get(affectedVersionsList.size() - 1).getDateTime()))   // AVN < FV
                                )
                ) continue;

                ticketList.add(new Ticket(key, creationDate, resolutionDate, openingVersion.orElse(null), fixedVersion.orElse(null), affectedVersionsList));
            }
        } while (i < total);

        // Ordina i ticket per data di risoluzione
        ticketList.sort(Comparator.comparing(Ticket::getResolutionDate));

        return ticketList;
    }

}
