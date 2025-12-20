package it.torvergata.bugprediction.models;

import java.time.LocalDate;
import java.util.*;

public class Release {

    private int numericId;

    private final String id;
    private final String name;
    private final String dateString;
    private final LocalDate dateTime;
    private final List<Commit> commitList;

    public Release(String id , String name, String dateString) {
        this.id = id;
        this.name = name;
        this.dateString = dateString;
        this.dateTime = LocalDate.parse(dateString);
        commitList = new ArrayList<>();
    }

    public int getNumericId() {
        return numericId;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDateString() {
        return dateString;
    }

    public LocalDate getDateTime() {
        return dateTime;
    }

    public List<Commit> getCommitList() {
        return commitList;
    }

    public void setNumericId(int numericId) {
        this.numericId = numericId;
    }

    public void addCommit(Commit commit) {
        commitList.add(commit);
    }

    /***
     * Trova la release con la data di rilascio più vicina (successivamente) a quella specificata
     * @param date la data di riferimento
     * @param releases elenco delle release
     * @return la prima release con data ≥ 'date' se esiste, null altrimenti
     */
    public static Optional<Release> findFirstReleaseOnOrAfter(
            LocalDate date,
            List<Release> releases) {

        return releases.stream()
                .sorted(Comparator.comparing(Release::getDateTime))
                .filter(r -> !r.getDateTime().isBefore(date))
                .findFirst();
    }

}
