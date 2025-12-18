package it.torvergata.bugprediction.infrastructure.git;

import it.torvergata.bugprediction.exceptions.GitException;
import it.torvergata.bugprediction.models.Commit;
import it.torvergata.bugprediction.models.Release;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GitRepositoryMiner {

    private final Logger logger;
    private static final String REPO_BASE_PATH = "repos/";
    private static final String URL_BASE_PATH = "https://github.com/apache/";

    private final Git git;
    private final Repository repository;

    /**
     * Clona il repository se non presente, oppure apre quello esistente
     */
    public GitRepositoryMiner(String projectName) throws GitException {
        logger = Logger.getLogger(GitRepositoryMiner.class.getName());

        String projectLower = projectName.toLowerCase();
        File repoDir = new File(REPO_BASE_PATH + projectLower);

        try {
            if (repoDir.exists()) {
                logger.log(
                        Level.INFO,
                        String.format("Il repository %s è già presente localmente. Apertura in corso...", projectName)
                );
                repository = new FileRepositoryBuilder()
                        .setGitDir(new File(repoDir, ".git"))
                        .build();
                git = new Git(repository);
            } else {
                String repoUrl = URL_BASE_PATH + projectLower + ".git";
                logger.log(Level.INFO, String.format("Clonazione da %s", repoUrl));
                git = Git.cloneRepository()
                        .setURI(repoUrl)
                        .setDirectory(repoDir)
                        .call();
                repository = git.getRepository();
                logger.log(Level.INFO, "Clonazione completata con successo.");
            }
        } catch (GitAPIException | IOException e) {
            throw new GitException("Errore durante l'inizializzazione del repository", e);
        }
    }

    public LocalDate getReleaseDate(String releaseName)
            throws GitAPIException, IOException, GitException {

        try (RevWalk walk = new RevWalk(repository)) {

            List<Ref> refs = git.tagList().call();

            for (Ref ref : refs) {
                if (ref.getName().equals("refs/tags/release-" + releaseName)) {

                    Instant tagInstant = walk
                            .parseTag(ref.getObjectId())
                            .getTaggerIdent()
                            .getWhenAsInstant();

                    return tagInstant
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate();
                }
            }
        }

        throw new GitException(
                "Data non trovata per la release con tag: " + releaseName + ". Release ignorata"
        );
    }

    public List<Commit> extractCommits(List<Release> jiraReleases) throws IOException, GitAPIException {

        // Tutti i commit estratti
        List<RevCommit> revCommitList = new ArrayList<>();

        // Tutti i commit con le informazioni che ci interessano
        List<Commit> commitList = new ArrayList<>();

        Iterable<RevCommit> iterableCommits = git.log().all().call();
        iterableCommits.forEach(revCommitList::add);

        // Ordina i commit per tempo
        revCommitList.sort(Comparator.comparing(RevCommit::getCommitTime));

        // Imposta tutti i commit per una release e imposta la release di un commit
        for (RevCommit revCommit : revCommitList) {
            // Prendi la data del commit
            LocalDate commitDate = Instant.ofEpochSecond(revCommit.getCommitTime())
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate();

            LocalDate previusReleaseDate = LocalDate.of(1970, 1, 1);  // limite inferiore iniziale
            for (Release release: jiraReleases){
                // Prendi la data della release
                LocalDate nextReleaseDate = release.getReleaseDateTime();

                // Se la data di un commit è dopo l’ultima release considerata e prima della prossima release,
                // allora aggiungilo alla prossima release considerata.
                if (commitDate.isAfter(previusReleaseDate) && !commitDate.isAfter(nextReleaseDate)) {
                    Commit newCommit = new Commit(revCommit, release);
                    commitList.add(newCommit);
                    release.addCommit(newCommit);
                    break;
                }

                previusReleaseDate = nextReleaseDate;
            }
        }

        // Rimuovi una release se non ha nessun commit
        jiraReleases.removeIf(release -> release.getCommitList().isEmpty());

        // Ordina i commit per data
        commitList.sort(Comparator.comparing(commit -> commit.getRevCommit().getCommitTime()));

        return commitList;

    }

    // Rimuove tutte le versioni nell'elenco che non hanno un tag Git
    public void filterTaggedReleases(List<Release> releases) throws GitAPIException {
        List<Ref> tags = git.tagList().call();
        List<String> tagNames = tags.stream().map(Ref::getName).toList();
        List<Release> releaseToRemove = new ArrayList<>();

        for(Release release: releases) {
            if(!tagNames.contains("refs/tags/release-" + release.getReleaseName()))
                releaseToRemove.add(release);
        }

        releases.removeAll(releaseToRemove);
    }

    public void close() {
        if(git != null) git.close();
        if(repository != null) repository.close();
    }
}
