package it.torvergata.bugprediction.service;

import it.torvergata.bugprediction.models.Release;
import org.json.JSONArray;

import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ReleaseService {

    /***
     * Trova la release con la data di rilascio più vicina (successivamente) a quella specificata
     */
    public static Optional<Release> findFirstReleaseOnOrAfter(LocalDate date, List<Release> releases) {
        // releases deve essere già ordinata
        for (Release r : releases) {
            if (!r.getDateTime().isBefore(date)) {
                return Optional.of(r);
            }
        }
        return Optional.empty();
    }

    /***
     * Ottieni da una risposta JSON di Jira tutte le AV ordinate per data
     */
    public static List<Release> getAffectedVersions(JSONArray affectedVersionsArray,
                                                    Map<String, Release> releaseNameMap) {
        List<Release> existingAffectedVersions = new ArrayList<>();
        for (int i = 0; i < affectedVersionsArray.length(); i++) {
            String affectedVersionName = affectedVersionsArray.getJSONObject(i).getString("name");
            Release release = releaseNameMap.get(affectedVersionName);
            if (release != null) existingAffectedVersions.add(release);
        }
        // Se releasesList è già ordinata, existingAffectedVersions si ordina correttamente
        existingAffectedVersions.sort(Comparator.comparing(Release::getDateTime));
        return existingAffectedVersions;
    }

    // Map per accesso rapido per nome
    public static Map<String, Release> buildReleaseNameMap(List<Release> releases) {
        return releases.stream()
                .collect(Collectors.toMap(Release::getName, Function.identity()));
    }

    // Imposta l'ID numerico per ogni release
    public static void setReleasesNumericID(List<Release> releaseList) {
        if (releaseList == null || releaseList.isEmpty()) return;

        releaseList.sort(Comparator.comparing(Release::getDateTime));

        for (int i = 0; i < releaseList.size(); i++){
            releaseList.get(i).setNumericId(i + 1);
        }
    }

    // Prendi la prima parte delle release
    public static List<Release> getFirstHalfOfReleases(List<Release> releaseList) {
        if (releaseList == null || releaseList.isEmpty()) {
            return Collections.emptyList();
        }
        return new ArrayList<>(releaseList.subList(0, releaseList.size() / 2));
    }

    public static List<Release> getConsideringReleases(List<Release> jiraReleases, Release currentRelease) {
        return jiraReleases
                .stream()
                .filter(r-> r.getNumericId() <= currentRelease.getNumericId())
                .toList();
    }

}
