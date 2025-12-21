package it.torvergata.bugprediction.datasource.git;

import it.torvergata.bugprediction.exceptions.GitException;
import it.torvergata.bugprediction.models.Commit;
import it.torvergata.bugprediction.models.ReleaseClass;
import it.torvergata.bugprediction.models.Release;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.Edit;
import org.eclipse.jgit.diff.RawTextComparator;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.*;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.util.io.DisabledOutputStream;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class GitRepositoryAnalyzer {

    private static final Logger LOGGER = Logger.getLogger(GitRepositoryAnalyzer.class.getName());
    private static final String REPO_BASE_PATH = "repos/";
    private static final String URL_BASE_PATH = "https://github.com/apache/";

    private Git git;
    private Repository repository;

    public GitRepositoryAnalyzer(String projectName) throws GitException {
        cloneOrOpenRepository(projectName.toLowerCase());
    }

    private void cloneOrOpenRepository(String projectName) throws GitException {
        Path repoPath = Paths.get(REPO_BASE_PATH, projectName);
        Path gitDir = repoPath.resolve(".git");

        try {
            if (Files.exists(gitDir)) {
                openExistingRepository(projectName, gitDir);
            } else {
                cloneRepository(projectName, repoPath);
            }
        } catch (IOException | GitAPIException e) {
            throw new GitException(
                    "Errore durante l'inizializzazione del repository: " + projectName, e
            );
        }
    }

    private void openExistingRepository(String projectName, Path gitDir) throws IOException {
        LOGGER.log(Level.INFO,
                () -> "Il repository " + projectName + " è già presente localmente. Apertura in corso..."
        );

        repository = new FileRepositoryBuilder()
                .setGitDir(gitDir.toFile())
                .build();

        git = new Git(repository);
    }

    private void cloneRepository(String projectName, Path repoPath) throws GitAPIException {
        String repoUrl = URL_BASE_PATH + projectName + ".git";

        LOGGER.log(Level.INFO, () -> "Clonazione da " + repoUrl);

        git = Git.cloneRepository()
                .setURI(repoUrl)
                .setDirectory(repoPath.toFile())
                .call();

        repository = git.getRepository();

        LOGGER.log(Level.INFO, "Clonazione completata con successo.");
    }

    // Recupera la data di una release dal repository
    public LocalDate getReleaseDate(String releaseName)
            throws GitAPIException, IOException, GitException {

        List<Ref> refs = git.tagList().call();

        for (Ref ref : refs) {
            if (ref.getName().equals("refs/tags/release-" + releaseName)) {

                try (RevWalk walk = new RevWalk(repository)) {
                    RevObject obj = walk.parseAny(ref.getObjectId());

                    Instant instant;

                    if (obj instanceof RevTag tag) {
                        instant = tag.getTaggerIdent().getWhenAsInstant();
                    } else if (obj instanceof RevCommit commit) {
                        instant = Instant.ofEpochSecond(commit.getCommitTime());
                    } else {
                        continue;
                    }

                    return instant.atZone(ZoneId.systemDefault()).toLocalDate();
                }
            }
        }

        throw new GitException(
                "Data non trovata per la release: " + releaseName + ". Release ignorata"
        );
    }

    // Rimuove tutte le versioni nell'elenco che non hanno un tag Git nel repository
    public void removeUntaggedReleases(List<Release> releases) throws GitAPIException {

        Set<String> tagNames = git.tagList().call().stream()
                .map(Ref::getName)
                .collect(Collectors.toSet());

        releases.removeIf(release ->
                !tagNames.contains("refs/tags/release-" + release.getName())
        );
    }

    public List<Commit> extractCommits(List<Release> jiraReleases)
            throws IOException, GitAPIException {

        // Ordina le release per data (fondamentale)
        jiraReleases.sort(Comparator.comparing(Release::getDateTime));

        // Estrai e ordina i commit
        List<RevCommit> revCommits = new ArrayList<>();
        git.log().all().call().forEach(revCommits::add);

        revCommits.sort(Comparator.comparingInt(RevCommit::getCommitTime));

        List<Commit> commitList = new ArrayList<>();

        int releaseIndex = 0;

        for (RevCommit revCommit : revCommits) {
            LocalDate commitDate = Instant.ofEpochSecond(revCommit.getCommitTime())
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate();

            // Avanza finché il commit supera la release corrente
            while (releaseIndex < jiraReleases.size() &&
                    commitDate.isAfter(jiraReleases.get(releaseIndex).getDateTime())) {
                releaseIndex++;
            }

            if (releaseIndex < jiraReleases.size()) {
                Release release = jiraReleases.get(releaseIndex);
                Commit commit = new Commit(revCommit, release);
                commitList.add(commit);
                release.addCommit(commit);
            }
        }

        // Rimuovi release senza commit
        jiraReleases.removeIf(r -> r.getCommitList().isEmpty());

        return commitList;
    }

    /**
     * Estrae le classi per ciascuna release e associa i commit che le hanno modificate
     */
    public List<ReleaseClass> extractClasses(
            List<Release> releaseList,
            List<Commit> commitList) throws IOException {

        List<ReleaseClass> classList = new ArrayList<>();

        // Indicizza le classi per release e nome classe
        Map<Release, Map<String, ReleaseClass>> classesByRelease = new HashMap<>();

        // Per ogni release prendi solo l'ultimo commit
        for (Release release : releaseList) {
            Commit lastCommit = release.getCommitList().stream()
                    .max(Comparator.comparingInt(c -> c.getRevCommit().getCommitTime()))
                    .orElseThrow();

            Map<String, String> classesNameCodeMap =
                    getClassesNameCodeInfos(lastCommit.getRevCommit());

            for (Map.Entry<String, String> entry : classesNameCodeMap.entrySet()) {
                ReleaseClass releaseClass =
                        new ReleaseClass(entry.getKey(), entry.getValue(), release);

                classList.add(releaseClass);

                classesByRelease
                        .computeIfAbsent(release, r -> new HashMap<>())
                        .put(entry.getKey(), releaseClass);
            }
        }

        // Associa i commit alle classi che toccano
        setTouchingClassesCommits(classesByRelease, commitList);

        // Ordina per nome classe
        classList.sort(Comparator.comparing(ReleaseClass::getName));

        return classList;
    }

    /**
     * Estrae nome e codice sorgente delle classi Java da un commit
     * (escluse le classi di test)
     */
    private Map<String, String> getClassesNameCodeInfos(RevCommit revCommit)
            throws IOException {

        Map<String, String> classes = new HashMap<>();
        RevTree tree = revCommit.getTree();

        try (TreeWalk treeWalk = new TreeWalk(repository)) {
            treeWalk.addTree(tree);
            treeWalk.setRecursive(true);

            while (treeWalk.next()) {
                String path = treeWalk.getPathString();

                if (path.endsWith(".java") && !path.contains("/src/test/")) {
                    byte[] bytes = repository
                            .open(treeWalk.getObjectId(0))
                            .getBytes();

                    classes.put(path, new String(bytes, StandardCharsets.UTF_8));
                }
            }
        }
        return classes;
    }

    /**
     * Aggiunge a ogni classe i commit che l'hanno modificata
     */
    private void setTouchingClassesCommits(
            Map<Release, Map<String, ReleaseClass>> classesByRelease,
            List<Commit> commitList) throws IOException {

        for (Commit commit : commitList) {
            Release release = commit.getRelease();
            Map<String, ReleaseClass> releaseClasses =
                    classesByRelease.get(release);

            if (releaseClasses == null) {
                continue;
            }

            List<String> touchedClasses =
                    getTouchedClassesNames(commit.getRevCommit());

            for (String className : touchedClasses) {
                ReleaseClass releaseClass = releaseClasses.get(className);
                if (releaseClass != null) {
                    releaseClass.addTouchingClassCommit(commit);
                }
            }
        }
    }

    /**
     * Ottiene i nomi delle classi Java modificate da un commit
     */
    public List<String> getTouchedClassesNames(RevCommit commit)
            throws IOException {

        // Commit iniziale senza genitore
        if (commit.getParentCount() == 0) {
            return Collections.emptyList();
        }

        List<String> touchedClasses = new ArrayList<>();

        try (DiffFormatter diffFormatter =
                     new DiffFormatter(DisabledOutputStream.INSTANCE);
             ObjectReader reader = repository.newObjectReader()) {

            diffFormatter.setRepository(repository);

            CanonicalTreeParser newTree = new CanonicalTreeParser();
            newTree.reset(reader, commit.getTree());

            CanonicalTreeParser oldTree = new CanonicalTreeParser();
            oldTree.reset(reader, commit.getParent(0).getTree());

            List<DiffEntry> diffs =
                    diffFormatter.scan(oldTree, newTree);

            for (DiffEntry entry : diffs) {
                String path = entry.getNewPath();
                if (path.endsWith(".java") && !path.contains("/src/test/")) {
                    touchedClasses.add(path);
                }
            }
        }
        return touchedClasses;
    }

    /**
     * Calcola e imposta per una classe le righe di codice aggiunte e rimosse
     * da tutti i commit che hanno modificato quella classe.
     */
    public void extractAddedAndRemovedLOC(ReleaseClass releaseClass) throws IOException {

        try (DiffFormatter diffFormatter =
                     new DiffFormatter(DisabledOutputStream.INSTANCE)) {

            diffFormatter.setRepository(repository);
            diffFormatter.setDiffComparator(RawTextComparator.DEFAULT);

            for (Commit commit : releaseClass.getTouchingClassCommitList()) {

                RevCommit revCommit = commit.getRevCommit();

                if (revCommit.getParentCount() == 0) {
                    continue;
                }

                RevCommit parentCommit = revCommit.getParent(0);
                List<DiffEntry> diffEntries =
                        diffFormatter.scan(parentCommit.getTree(), revCommit.getTree());

                for (DiffEntry diffEntry : diffEntries) {
                    if (diffEntry.getNewPath().equals(releaseClass.getName())) {
                        releaseClass.addAddedLOC(getAddedLines(diffFormatter, diffEntry));
                        releaseClass.addRemovedLOC(getDeletedLines(diffFormatter, diffEntry));
                    }
                }
            }
        }
    }

    private int getAddedLines(DiffFormatter diffFormatter, DiffEntry entry) throws IOException {
        int addedLines = 0;
        for(Edit edit : diffFormatter.toFileHeader(entry).toEditList()) {
            addedLines += edit.getEndB() - edit.getBeginB();
        }
        return addedLines;
    }

    private int getDeletedLines(DiffFormatter diffFormatter, DiffEntry entry) throws IOException {
        int deletedLines = 0;
        for(Edit edit : diffFormatter.toFileHeader(entry).toEditList()) {
            deletedLines += edit.getEndA() - edit.getBeginA();
        }
        return deletedLines;
    }

    public void close() {
        if(git != null) git.close();
        if(repository != null) repository.close();
    }
}
