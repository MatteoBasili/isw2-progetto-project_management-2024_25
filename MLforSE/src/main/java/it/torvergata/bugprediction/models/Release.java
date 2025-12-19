package it.torvergata.bugprediction.models;

import org.json.JSONArray;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public class Release {

    private int numericID;
    private final String releaseID;
    private final String releaseName;
    private final String releaseDateString;
    private final LocalDate releaseDateTime;
    private final List<Commit> commitList;

    public Release(String releaseID , String releaseName, String releaseDateString) {
        this.numericID = 0;
        this.releaseID = releaseID;
        this.releaseName = releaseName;
        this.releaseDateString = releaseDateString;
        this.releaseDateTime = LocalDate.parse(releaseDateString);
        commitList = new ArrayList<>();
    }

    public int getNumericID() {
        return numericID;
    }

    public String getReleaseID() {
        return releaseID;
    }

    public String getReleaseName() {
        return releaseName;
    }

    public String getReleaseDateString() {
        return releaseDateString;
    }

    public LocalDate getReleaseDateTime() {
        return releaseDateTime;
    }

    public List<Commit> getCommitList() {
        return commitList;
    }

    public void setNumericID(int numericID) {
        this.numericID = numericID;
    }

    public void addCommit(Commit commit) {
        commitList.add(commit);
    }

    /***
     * Trova la release con la data successiva più vicina a quella specificata
     * @param specificDate la data da confrontare con la data delle release
     * @param releases elenco delle release da confrontare
     * @return la release con la data successiva più vicina se esiste, null altrimenti
     */
    public static Release getReleaseAfterOrEqualDate(LocalDate specificDate, List<Release> releases) {
        releases.sort(Comparator.comparing(Release::getReleaseDateTime));
        for (Release release : releases) {
            if (!release.getReleaseDateTime().isBefore(specificDate)) {
                return release;
            }
        }
        return null;
    }

    /***
     * Ottieni da una risposta JSON di Jira tutte le AV ordinate per data
     * @param affectedVersionsArray oggetto JSON contenente le AV
     * @param releasesList elenco delle release
     * @return elenco delle AV ordinate per data
     */
    public static List<Release> getValidAffectedVersions(JSONArray affectedVersionsArray, List<Release> releasesList) {
        List<Release> existingAffectedVersions = new ArrayList<>();
        for (int i = 0; i < affectedVersionsArray.length(); i++) {
            String affectedVersionName = affectedVersionsArray.getJSONObject(i).get("name").toString();
            Release release = getReleaseByName(releasesList, affectedVersionName);

            // Se release è null significa che non è nell'elenco. Perciò, possiamo ignorarla
            if (release != null)
                existingAffectedVersions.add(release);
        }
        existingAffectedVersions.sort(Comparator.comparing(Release::getReleaseDateTime));
        return existingAffectedVersions;
    }

    /***
     * Trova una release in un elenco in base al suo nome
     * @param releaseList elenco delle release
     * @param releaseName nome della release
     * @return la release con il nome desiderato se esiste, null altrimenti
     */
    private static Release getReleaseByName(List<Release> releaseList, String releaseName) {
        for (Release release : releaseList) {
            if (Objects.equals(releaseName, release.getReleaseName())) return release;
        }
        return null;
    }

}
