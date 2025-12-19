package it.torvergata.bugprediction.models;

import org.eclipse.jgit.revwalk.RevCommit;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

public class Commit {

    private final RevCommit revCommit;
    private Ticket ticket;
    private final Release release;

    private final String message;
    private final LocalDate date;

    public Commit(RevCommit revCommit, Release release) {
        this.revCommit = revCommit;
        this.release = release;
        this.message = revCommit.getFullMessage();
        this.date = Instant.ofEpochSecond(revCommit.getCommitTime())
                .atZone(ZoneId.systemDefault())
                .toLocalDate();
        ticket = null;
    }

    public RevCommit getRevCommit() {
        return revCommit;
    }

    public Ticket getTicket() {
        return ticket;
    }

    public Release getRelease() {
        return release;
    }

    public void setTicket(Ticket ticket) {
        this.ticket = ticket;
    }

    public Commit cloneCommitAtRelease(Release release) {
        Commit commit = new Commit(revCommit, release);
        commit.setTicket(ticket);

        return commit;
    }

}
