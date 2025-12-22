package it.torvergata.bugprediction.service;

import it.torvergata.bugprediction.models.Commit;
import it.torvergata.bugprediction.models.Release;
import it.torvergata.bugprediction.models.Ticket;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class CommitService {

    private CommitService() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Filtra i commit che hanno un ID ticket nel messaggio, impostando il ticket di un commit
     * e l'elenco di commit per ogni ticket, rimuovendo i ticket senza un commit.
     *
     * @param ticketList lista dei ticket da considerare
     * @param commitList lista dei commit da filtrare
     * @return elenco dei commit che fanno riferimento a un ticket
     */
    public static List<Commit> filterAndAssignCommitsToTickets(List<Ticket> ticketList, List<Commit> commitList) {
        if (ticketList == null || commitList == null) return Collections.emptyList();

        // Costruisce una mappa ticketKey -> Ticket per accesso rapido
        Map<String, Ticket> ticketMap = ticketList.stream()
                .collect(Collectors.toMap(Ticket::getKey, t -> t));

        Set<Commit> filteredCommitSet = new LinkedHashSet<>(); // evita duplicati e mantiene ordine

        // Per ogni commit, cerca se contiene uno dei ticketKey
        for (Commit commit : commitList) {
            String message = commit.getRevCommit().getFullMessage();
            for (Map.Entry<String, Ticket> entry : ticketMap.entrySet()) {
                String key = entry.getKey();
                Ticket ticket = entry.getValue();

                Pattern pattern = Pattern.compile(Pattern.quote(key) + "\\b");
                if (pattern.matcher(message).find()) {
                    commit.setTicket(ticket);
                    ticket.addCommit(commit);
                    filteredCommitSet.add(commit);
                    break; // assume un commit appartiene al primo ticket trovato
                }
            }
        }

        // Crea lista finale dei ticket con commit
        List<Ticket> ticketsWithCommits = ticketList.stream()
                .filter(t -> !t.getCommitList().isEmpty())
                .toList();

        ticketList.clear();
        ticketList.addAll(ticketsWithCommits);

        // Restituisce la lista filtrata di commit
        return new ArrayList<>(filteredCommitSet);
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
