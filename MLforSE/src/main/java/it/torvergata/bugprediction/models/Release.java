package it.torvergata.bugprediction.models;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

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

    public String getReleaseName() {
        return releaseName;
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

}
