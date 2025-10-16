package it.university.bugprediction;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

public class CommitExtractor {

    // Regex per identificare commit bug-fix
    private static final Pattern BUGFIX_PATTERN = Pattern.compile(".*(fix|bug|issue|error|patch|resolve).*", Pattern.CASE_INSENSITIVE);

    public static void main(String[] args) throws IOException, GitAPIException {
        if (args.length != 2) {
            System.out.println("Uso: java CommitExtractor <path_repo> <output_csv>");
            return;
        }

        String repoPath = args[0];
        String outputCsv = args[1];

        FileRepositoryBuilder builder = new FileRepositoryBuilder();
        Repository repository = builder.setGitDir(new File(repoPath + "/.git"))
                .readEnvironment()
                .findGitDir()
                .build();

        Git git = new Git(repository);

        FileWriter out = new FileWriter(outputCsv);
        CSVPrinter csvPrinter = new CSVPrinter(out, CSVFormat.DEFAULT.withHeader(
                "commitHash","author","date","message","isBugFix","filePath"));

        Iterable<RevCommit> commits = git.log().call();

        for (RevCommit commit : commits) {
            boolean isBugFix = BUGFIX_PATTERN.matcher(commit.getFullMessage()).find();

            // Lista dei file modificati
            List<DiffEntry> diffs = git.diff()
                    .setOldTree(Utils.prepareTreeParser(repository, commit.getParentCount() > 0 ? commit.getParent(0) : null))
                    .setNewTree(Utils.prepareTreeParser(repository, commit))
                    .call();

            for (DiffEntry diff : diffs) {
                String filePath = diff.getNewPath();
                csvPrinter.printRecord(commit.getName(), commit.getAuthorIdent().getName(),
                        commit.getAuthorIdent().getWhen(), commit.getFullMessage(),
                        isBugFix ? 1 : 0, filePath);
            }
        }

        csvPrinter.flush();
        csvPrinter.close();
        repository.close();

        System.out.println("Estrazione commit completata. CSV salvato in: " + outputCsv);
    }
}
