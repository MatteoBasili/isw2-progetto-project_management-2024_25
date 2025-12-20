package it.torvergata.bugprediction.service;

import it.torvergata.bugprediction.models.Release;
import org.json.JSONArray;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public class ReleaseService {

    /***
     * Ottieni da una risposta JSON di Jira tutte le AV ordinate per data
     * @param affectedVersionsArray oggetto JSON contenente le AV
     * @param releasesList elenco delle release
     * @return elenco delle AV ordinate per data
     */
    public static List<Release> getAffectedVersions(JSONArray affectedVersionsArray, List<Release> releasesList) {
        List<Release> existingAffectedVersions = new ArrayList<>();
        for (int i = 0; i < affectedVersionsArray.length(); i++) {
            String affectedVersionName = affectedVersionsArray.getJSONObject(i).get("name").toString();
            Release release = getReleaseByName(releasesList, affectedVersionName);

            // Se release è null significa che non è nell'elenco. Perciò, possiamo ignorarla
            if (release != null)
                existingAffectedVersions.add(release);
        }
        existingAffectedVersions.sort(Comparator.comparing(Release::getDateTime));
        return existingAffectedVersions;
    }

    /***
     * Trova una release in un elenco in base al suo nome
     * @param releaseList elenco delle release
     * @param releaseName nome della release
     * @return la release con il nome specificato se esiste, null altrimenti
     */
    private static Release getReleaseByName(List<Release> releaseList, String releaseName) {
        for (Release release : releaseList) {
            if (Objects.equals(releaseName, release.getName())) return release;
        }
        return null;
    }

    // Imposta l'ID numerico per ogni release
    public static void setReleasesNumericID(List<Release> releaseList) {
        releaseList.sort(Comparator.comparing(Release::getDateTime));
        for (int i = 0; i < releaseList.size(); i++){
            releaseList.get(i).setNumericId(i + 1);
        }
    }

    // Prendi la prima parte delle release
    public static List<Release> getFirstHalfOfReleases(List<Release> releaseList) {
        return releaseList.subList(0, releaseList.size()/2);
    }

    public static List<Release> getConsideringReleases(List<Release> jiraReleases, Release currentRelease) {
        return jiraReleases
                .stream()
                .filter(r-> r.getNumericId() <= currentRelease.getNumericId())
                .toList();
    }

}
