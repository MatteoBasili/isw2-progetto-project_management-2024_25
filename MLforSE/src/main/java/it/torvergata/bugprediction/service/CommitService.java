package it.torvergata.bugprediction.service;

import it.torvergata.bugprediction.models.Commit;
import it.torvergata.bugprediction.models.Release;
import it.torvergata.bugprediction.models.Ticket;
import it.torvergata.bugprediction.utils.Utils;

import java.util.ArrayList;
import java.util.List;

public class CommitService {

    /**
     * Filtra i commit che hanno un ID ticket nel messaggio, impostando il ticket di un commit e l'elenco di
     * commit per ogni ticket e rimuovendo i ticket senza un commit
     * @param commitList commit da filtrare
     * @param ticketList ticket da cui ottenere gli ID
     * @return un elenco di commit che fanno riferimento a un ticket
     */
    public static List<Commit> filterAndAssignCommitsToTickets(List<Ticket> ticketList, List<Commit> commitList) {
        List<Commit> filteredCommitList = new ArrayList<>();
        for (Commit commit : commitList) {
            String commitFullMessage = commit.getRevCommit().getFullMessage();
            for (Ticket ticket : ticketList) {
                String ticketKey = ticket.getKey();
                if (Utils.matchRegex(commitFullMessage, ticketKey)) {
                    filteredCommitList.add(commit);
                    ticket.addCommit(commit);
                    commit.setTicket(ticket);
                }
            }
        }

        // Se un ticket non ha commit significa che non è stato risolto, quindi non ci interessa
        ticketList.removeIf(ticket -> ticket.getCommitList().isEmpty());

        return filteredCommitList;
    }

    public static List<Commit> getConsideringCommits(List<Commit> commitList, Release currentRelease) {
        List<Commit> consideringCommitList = commitList
                .stream()
                .filter(c -> c.getRelease().getNumericId() <= currentRelease.getNumericId())
                .toList();

        List<Commit> returningCommitList = new ArrayList<>();

        for (Commit commit: consideringCommitList) {
            Commit newCommit = commit.cloneAtRelease(commit.getRelease());
            returningCommitList.add(newCommit);
        }

        return returningCommitList;
    }

}
