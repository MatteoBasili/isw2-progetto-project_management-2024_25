package it.torvergata.bugprediction.metrics;

import it.torvergata.bugprediction.model.Commit;
import it.torvergata.bugprediction.model.FileMetrics;
import it.torvergata.bugprediction.model.Release;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.Edit;
import org.eclipse.jgit.diff.EditList;
import org.eclipse.jgit.diff.RawTextComparator;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Computes file-level metrics for each release using Git history.
 * <p>
 * Selected metrics (10):
 *  1. LOC
 *  2. LOC_touched
 *  3. NR
 *  4. NFix
 *  5. NAuth
 *  6. LOC_added
 *  7. Max_LOC_added
 *  8. Avg_LOC_added
 *  9. Churn
 * 10. Max_Churn
 */
public class MetricsCalculator {

    private static final Logger LOGGER = Logger.getLogger(MetricsCalculator.class.getName());
    private static final String JAVA_EXT = ".java";

    /**
     * Compute metrics for each file in a given release.
     *
     * @param release         release considered
     * @param commits         list of commits belonging to the release
     * @param buggyCommits    set of commit hashes linked to bug-fix tickets
     * @param repoPath        path to the local Git repository
     * @return                map: file path → computed metrics
     */
    public Map<String, FileMetrics> computeMetrics(
            Release release,
            List<Commit> commits,
            Set<String> buggyCommits,
            String repoPath) {

        Map<String, FileMetrics> metricsByFile = new HashMap<>();

        try (Repository repository = new FileRepositoryBuilder()
                .setGitDir(new File(repoPath, ".git"))
                .readEnvironment()
                .findGitDir()
                .build();
             Git git = new Git(repository);
             RevWalk revWalk = new RevWalk(repository)) {

            for (Commit commit : commits) {
                RevCommit revCommit = revWalk.parseCommit(ObjectId.fromString(commit.getHash()));
                RevCommit parent = revCommit.getParentCount() > 0 ? revCommit.getParent(0) : null;

                boolean isBugFix = buggyCommits.contains(commit.getHash());
                String author = commit.getAuthor();

                if (parent == null) continue;

                try (DiffFormatter diffFormatter = new DiffFormatter(new ByteArrayOutputStream())) {
                    diffFormatter.setRepository(repository);
                    diffFormatter.setDiffComparator(RawTextComparator.DEFAULT);
                    diffFormatter.setDetectRenames(true);

                    CanonicalTreeParser oldTreeIter = new CanonicalTreeParser();
                    oldTreeIter.reset(repository.newObjectReader(), parent.getTree());
                    CanonicalTreeParser newTreeIter = new CanonicalTreeParser();
                    newTreeIter.reset(repository.newObjectReader(), revCommit.getTree());

                    List<DiffEntry> diffs = diffFormatter.scan(oldTreeIter, newTreeIter);

                    for (DiffEntry diff : diffs) {
                        String filePath = diff.getNewPath();
                        if (!filePath.endsWith(JAVA_EXT)) continue; // consider only Java files

                        FileMetrics m = metricsByFile.computeIfAbsent(filePath, FileMetrics::new);
                        m.incrementNR();
                        m.addAuthor(author);
                        if (isBugFix) m.incrementNFix();

                        // Calculate added/deleted lines
                        int added = 0, deleted = 0;
                        EditList edits = diffFormatter.toFileHeader(diff).toEditList();
                        for (Edit edit : edits) {
                            added += edit.getEndB() - edit.getBeginB();
                            deleted += edit.getEndA() - edit.getBeginA();
                        }

                        m.addLOCAdded(added);
                        m.updateMaxLOCAdded(added);
                        m.updateAvgLOCAdded();

                        int churn = added + deleted;
                        m.addChurn(churn);
                        m.updateMaxChurn(churn);
                        m.updateAvgChurn();

                        m.addLOCTouched(churn);
                    }
                }
            }

            // Final LOC estimation for each file
            for (String file : metricsByFile.keySet()) {
                int loc = estimateLOC(new File(repoPath, file));
                metricsByFile.get(file).setLOC(loc);
            }

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error computing metrics for release " + release.getName(), e);
        }

        LOGGER.log(Level.INFO, "Computed metrics for {0} files in release {1}",
                new Object[]{metricsByFile.size(), release.getName()});

        return metricsByFile;
    }

    /**
     * Estimate LOC (Lines of Code) for a given file.
     */
    private int estimateLOC(File file) {
        if (!file.exists()) return 0;
        try {
            return (int) Files.lines(file.toPath())
                    .filter(line -> !line.trim().isEmpty() && !line.trim().startsWith("//"))
                    .count();
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Unable to estimate LOC for file: " + file.getPath(), e);
            return 0;
        }
    }
}
