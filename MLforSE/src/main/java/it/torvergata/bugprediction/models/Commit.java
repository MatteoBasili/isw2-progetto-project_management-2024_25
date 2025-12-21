package it.torvergata.bugprediction.models;

import org.eclipse.jgit.revwalk.RevCommit;

public class Commit {

    private final RevCommit revCommit;
    private Ticket ticket;
    private final Release release;

    public Commit(RevCommit revCommit, Release release) {
        this.revCommit = revCommit;
        this.release = release;
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

    public Commit cloneAtRelease(Release release) {
        Commit cloned = new Commit(revCommit, release);
        cloned.setTicket(ticket);
        return cloned;
    }

}
