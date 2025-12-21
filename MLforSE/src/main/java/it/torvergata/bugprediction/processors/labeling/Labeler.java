package it.torvergata.bugprediction.processors.labeling;

import it.torvergata.bugprediction.datasource.git.GitRepositoryAnalyzer;
import it.torvergata.bugprediction.models.Commit;
import it.torvergata.bugprediction.models.ReleaseClass;
import it.torvergata.bugprediction.models.Ticket;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Labeler {

    public static void labelClassesBuggyness(GitRepositoryAnalyzer gitRepoAnalyzer,
                                             List<Ticket> ticketList,
                                             List<ReleaseClass> classList) throws IOException {
        // inizializza tutte le classi come non buggy
        classList.forEach(rc -> rc.getMetrics().setBuggyness(false));

        // mappa delle classi per nome per accesso rapido
        Map<String, List<ReleaseClass>> classesByName = classList.stream()
                .collect(Collectors.groupingBy(ReleaseClass::getName));

        for (Ticket ticket : ticketList) {
            List<Commit> ticketCommits = ticket.getCommitList();
            LocalDate injectedDate = ticket.getInjectedVersion().getDateTime();
            LocalDate fixedDate = ticket.getFixedVersion().getDateTime();

            for (Commit commit : ticketCommits) {
                // ottieni la data del commit come LocalDate
                LocalDate commitDate = Instant.ofEpochSecond(
                                commit.getRevCommit().getCommitTime())
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate();

                // verifica che il commit sia tra creationDate e resolutionDate
                if (commitDate.isBefore(ticket.getCreationDate()) || commitDate.isAfter(ticket.getResolutionDate())) {
                    continue;
                }

                List<String> modifiedClasses = gitRepoAnalyzer.getTouchedClassesNames(commit.getRevCommit());

                for (String className : modifiedClasses) {
                    List<ReleaseClass> classVersions = classesByName.getOrDefault(className, Collections.emptyList());
                    for (ReleaseClass rc : classVersions) {
                        LocalDate releaseDate = rc.getRelease().getDateTime();
                        if (!releaseDate.isBefore(injectedDate) && !releaseDate.isAfter(fixedDate)) {
                            rc.getMetrics().setBuggyness(true);
                        }
                    }
                }
            }
        }
    }

}