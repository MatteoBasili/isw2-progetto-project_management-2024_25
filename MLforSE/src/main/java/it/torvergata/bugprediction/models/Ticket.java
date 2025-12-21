package it.torvergata.bugprediction.models;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class Ticket {

    private final String key;

    private final LocalDate creationDate;
    private final LocalDate resolutionDate;
    private Release injectedVersion;
    private final Release openingVersion;
    private final Release fixedVersion;
    private List<Release> affectedVersions;
    private final List<Commit> commitList;

    /***
     *
     * @param key il nome del ticket
     * @param creationDate data di creazione del ticket
     * @param resolutionDate data di risoluzione del ticket
     * @param openingVersion la prima release affetta dal bug segnalato
     * @param fixedVersion la prima release dopo OV non più affetta dal bug
     * @param affectedVersions l'elenco delle release affette dal bug segnalato
     */
    public Ticket(String key, LocalDate creationDate, LocalDate resolutionDate, Release openingVersion,
                  Release fixedVersion, List<Release> affectedVersions) {
        this.key = key;
        this.creationDate = creationDate;
        this.resolutionDate = resolutionDate;
        if (affectedVersions.isEmpty()) {
            // I ticket con IV nullo saranno quelli da predire
            injectedVersion = null;
        } else{
            // IV = AV[0] per definizione
            injectedVersion = affectedVersions.get(0);

        }
        this.openingVersion = openingVersion;
        this.fixedVersion = fixedVersion;
        this.affectedVersions = affectedVersions;
        commitList = new ArrayList<>();
    }

    public String getKey() {
        return key;
    }

    public LocalDate getCreationDate() {
        return creationDate;
    }

    public LocalDate getResolutionDate() {
        return resolutionDate;
    }

    public Release getInjectedVersion() {
        return injectedVersion;
    }

    public Release getOpeningVersion() {
        return openingVersion;
    }

    public Release getFixedVersion() {
        return fixedVersion;
    }

    public List<Release> getAffectedVersions() {
        return affectedVersions;
    }

    public List<Commit> getCommitList(){
        return commitList;
    }

    public void setInjectedVersion(Release injectedVersion) {
        this.injectedVersion = injectedVersion;
    }

    public void setAffectedVersions(List<Release> affectedVersions) {
        this.affectedVersions = affectedVersions;
    }

    public boolean hasAffectedVersions() {
        return getAffectedVersions() != null && !getAffectedVersions().isEmpty();
    }

    public void addCommit(Commit newCommit) {
        if(!commitList.contains(newCommit)){
            commitList.add(newCommit);
        }
    }

    public Ticket cloneAtRelease(Release release) {
        List<Release> newAffectedVersions = affectedVersions == null
                ? List.of()
                : affectedVersions.stream()
                .filter(av -> av.getNumericId() <= release.getNumericId())
                .toList();
        Release newFixedVersion = fixedVersion.getNumericId() <= release.getNumericId() ? fixedVersion : null;
        if (newFixedVersion == null) return null;

        return new Ticket(key, creationDate, resolutionDate, openingVersion, newFixedVersion, newAffectedVersions);
    }

}
