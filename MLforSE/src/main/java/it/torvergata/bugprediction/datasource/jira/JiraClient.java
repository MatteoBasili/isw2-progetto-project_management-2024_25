package it.torvergata.bugprediction.datasource.jira;

import it.torvergata.bugprediction.exceptions.GitException;
import it.torvergata.bugprediction.datasource.git.GitRepositoryAnalyzer;
import it.torvergata.bugprediction.models.Release;
import it.torvergata.bugprediction.models.Ticket;
import it.torvergata.bugprediction.services.ReleaseService;
import it.torvergata.bugprediction.utils.JsonUtils;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.*;
import java.util.logging.Logger;

import static it.torvergata.bugprediction.utils.JsonUtils.readJsonFromUrl;

public class JiraClient {

    private final Logger logger;
    private final String projectName;

    // Formatter per date Jira tipo "2017-08-09T18:25:48.000+0000"
    private static final DateTimeFormatter JIRA_DATE_FORMATTER = new DateTimeFormatterBuilder()
            .appendPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
            .toFormatter();

    public JiraClient(String projectName) {
        this.logger = Logger.getLogger(JiraClient.class.getName());
        this.projectName = Objects.requireNonNull(projectName)
                .toUpperCase(Locale.ROOT);
    }

    public List<Release> extractReleases(GitRepositoryAnalyzer gitRepoAnalyzer) throws IOException, GitAPIException {

        String url = "https://issues.apache.org/jira/rest/api/2/project/" + projectName;

        JSONObject json = readJsonFromUrl(url);
        JSONArray versions = json.getJSONArray("versions");

        List<Release> releases = new ArrayList<>();

        // Crea la lista delle release
        for (int i = 0; i < versions.length(); i++) {
            JSONObject version = versions.getJSONObject(i);

            String id = version.optString("id", null);
            String name = version.optString("name", null);
            String date = version.optString("releaseDate", null);

            if (date == null) {
                try {
                    date = gitRepoAnalyzer.getReleaseDate(name).toString();
                } catch (GitException e) {
                    logger.info("[INFO] " + e.getMessage());
                    continue;
                }
            }

            releases.add(new Release(id, name, date));

        }

        // Ordina la lista delle release per data
        releases.sort(Comparator.comparing(Release::getDateTime));

        // Elimina le release che non rappresentano una vera versione del codice
        gitRepoAnalyzer.removeUntaggedReleases(releases);

        return releases;
    }

    public List<Ticket> extractTickets(List<Release> releasesList) throws IOException {
        // Ordina release e crea mappa nome -> Release
        releasesList.sort(Comparator.comparing(Release::getDateTime));
        Map<String, Release> releaseNameMap = ReleaseService.buildReleaseNameMap(releasesList);

        List<Ticket> ticketList = new ArrayList<>();
        int startAt = 0;
        final int batchSize = 1000;
        int total;

        do {
            String jql = String.format(
                    "project=\"%s\" AND issueType=\"Bug\" AND (status=\"Closed\" OR status=\"Resolved\") AND resolution=\"Fixed\"",
                    this.projectName
            );
            String encodedJql = URLEncoder.encode(jql, StandardCharsets.UTF_8);

            String url = String.format(
                    "https://issues.apache.org/jira/rest/api/2/search?jql=%s&fields=key,versions,created,resolutiondate&startAt=%d&maxResults=%d",
                    encodedJql, startAt, batchSize
            );

            JSONObject json = JsonUtils.readJsonFromUrl(url);
            JSONArray issues = json.getJSONArray("issues");
            total = json.getInt("total");

            // Itera sui ticket
            for (int k = 0; k < issues.length(); k++) {
                JSONObject issue = issues.getJSONObject(k);
                JSONObject fields = issue.getJSONObject("fields");

                String key = issue.getString("key");

                LocalDate creationDate = OffsetDateTime.parse(fields.getString("created"), JIRA_DATE_FORMATTER)
                        .toLocalDate();
                LocalDate resolutionDate = OffsetDateTime.parse(fields.getString("resolutiondate"), JIRA_DATE_FORMATTER)
                        .toLocalDate();

                // OV e FV
                Release openingVersion = ReleaseService.findFirstReleaseOnOrAfter(creationDate, releasesList).orElse(null);
                Release fixedVersion = ReleaseService.findFirstReleaseOnOrAfter(resolutionDate, releasesList).orElse(null);

                // AV
                JSONArray affectedVersionsArray = fields.getJSONArray("versions");
                List<Release> affectedVersionsList = ReleaseService.getAffectedVersions(affectedVersionsArray, releaseNameMap);

                // Controllo di consistenza
                if (!isTicketConsistent(openingVersion, fixedVersion, affectedVersionsList)) continue;

                // Aggiungi ticket
                ticketList.add(new Ticket(
                        key,
                        creationDate,
                        resolutionDate,
                        openingVersion,
                        fixedVersion,
                        affectedVersionsList
                ));
            }

            startAt += batchSize;
        } while (startAt < total);

        // Ordina per data di risoluzione
        ticketList.sort(Comparator.comparing(Ticket::getResolutionDate));
        return ticketList;
    }

    private boolean isTicketConsistent(Release ov, Release fv, List<Release> avList) {
        if (ov == null || fv == null) return false;

        LocalDate ovDate = ov.getDateTime();
        LocalDate fvDate = fv.getDateTime();

        if (ovDate.isAfter(fvDate)) return false;

        if (!avList.isEmpty()) {
            LocalDate avFirst = avList.get(0).getDateTime();
            LocalDate avLast = avList.get(avList.size() - 1).getDateTime();
            return !ovDate.isBefore(avFirst) && fvDate.isAfter(avLast);
        }

        return true;
    }

}
