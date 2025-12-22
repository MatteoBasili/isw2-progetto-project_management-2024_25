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

    private Labeler() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static void labelClassesBuggyness(GitRepositoryAnalyzer gitRepoAnalyzer,
                                             List<Ticket> ticketList,
                                             List<ReleaseClass> classList) throws IOException {

        classList.forEach(rc -> rc.getMetrics().setBuggyness(false));

        Map<String, List<ReleaseClass>> classesByName = classList.stream()
                .collect(Collectors.groupingBy(ReleaseClass::getName));

        for (Ticket ticket : ticketList) {
            processTicket(ticket, classesByName, gitRepoAnalyzer);
        }
    }

    private static void processTicket(Ticket ticket,
                                      Map<String, List<ReleaseClass>> classesByName,
                                      GitRepositoryAnalyzer gitRepoAnalyzer) throws IOException {

        for (Commit commit : ticket.getCommitList()) {
            if (!isCommitValidForTicket(commit, ticket)) {
                continue;
            }

            markBuggyClasses(commit, ticket, classesByName, gitRepoAnalyzer);
        }
    }

    private static boolean isCommitValidForTicket(Commit commit, Ticket ticket) {
        LocalDate commitDate = Instant.ofEpochSecond(
                        commit.getRevCommit().getCommitTime())
                .atZone(ZoneId.systemDefault())
                .toLocalDate();

        return !commitDate.isBefore(ticket.getCreationDate())
                && !commitDate.isAfter(ticket.getResolutionDate());
    }

    private static void markBuggyClasses(Commit commit,
                                         Ticket ticket,
                                         Map<String, List<ReleaseClass>> classesByName,
                                         GitRepositoryAnalyzer gitRepoAnalyzer) throws IOException {

        LocalDate injectedDate = ticket.getInjectedVersion().getDateTime();
        LocalDate fixedDate = ticket.getFixedVersion().getDateTime();

        List<String> modifiedClasses =
                gitRepoAnalyzer.getTouchedClassesNames(commit.getRevCommit());

        for (String className : modifiedClasses) {
            for (ReleaseClass rc : classesByName.getOrDefault(className, Collections.emptyList())) {
                if (isClassAffectedInRelease(rc, injectedDate, fixedDate)) {
                    rc.getMetrics().setBuggyness(true);
                }
            }
        }
    }

    private static boolean isClassAffectedInRelease(ReleaseClass rc,
                                                    LocalDate injectedDate,
                                                    LocalDate fixedDate) {

        LocalDate releaseDate = rc.getRelease().getDateTime();
        return !releaseDate.isBefore(injectedDate)
                && !releaseDate.isAfter(fixedDate);
    }
}
