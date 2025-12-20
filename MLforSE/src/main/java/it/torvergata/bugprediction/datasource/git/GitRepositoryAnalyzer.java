package it.torvergata.bugprediction.datasource.git;

import it.torvergata.bugprediction.exceptions.GitException;
import it.torvergata.bugprediction.models.Commit;
import it.torvergata.bugprediction.models.ReleaseClass;
import it.torvergata.bugprediction.models.Release;
import it.torvergata.bugprediction.models.Ticket;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.Edit;
import org.eclipse.jgit.diff.RawTextComparator;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.util.io.DisabledOutputStream;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GitRepositoryAnalyzer {

    private static final Logger LOGGER = Logger.getLogger(GitRepositoryAnalyzer.class.getName());
    private static final String REPO_BASE_PATH = "repos/";
    private static final String URL_BASE_PATH = "https://github.com/apache/";

    private Git git;
    private Repository repository;

    public GitRepositoryAnalyzer(String projectName) throws GitException {
        String projectLower = projectName.toLowerCase();
        cloneRepo(projectLower);
    }

    /**
     * Clona il repository se non presente, oppure apre quello esistente
     */
    private void cloneRepo(String projectName) throws GitException {
        File repoDir = new File(REPO_BASE_PATH + projectName);

        try {
            if (repoDir.exists()) {
                LOGGER.log(
                        Level.INFO,
                        String.format("Il repository %s è già presente localmente. Apertura in corso...", projectName)
                );
                repository = new FileRepositoryBuilder()
                        .setGitDir(new File(repoDir, ".git"))
                        .build();
                git = new Git(repository);
            } else {
                String repoUrl = URL_BASE_PATH + projectName + ".git";
                LOGGER.log(Level.INFO, String.format("Clonazione da %s", repoUrl));
                git = Git.cloneRepository()
                        .setURI(repoUrl)
                        .setDirectory(repoDir)
                        .call();
                repository = git.getRepository();
                LOGGER.log(Level.INFO, "Clonazione completata con successo.");
            }
        } catch (GitAPIException | IOException e) {
            throw new GitException("Errore durante l'inizializzazione del repository", e);
        }
    }

    // Recupera la data di una release dal repository
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

    // Rimuove tutte le versioni nell'elenco che non hanno un tag Git nel repository
    public void removeUntaggedReleases(List<Release> releases) throws GitAPIException {
        List<Ref> tags = git.tagList().call();
        List<String> tagNames = tags.stream().map(Ref::getName).toList();
        List<Release> releaseToRemove = new ArrayList<>();

        for(Release release: releases) {
            if(!tagNames.contains("refs/tags/release-" + release.getName()))
                releaseToRemove.add(release);
        }

        releases.removeAll(releaseToRemove);
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
                LocalDate nextReleaseDate = release.getDateTime();

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

    public List<ReleaseClass> extractClasses(List<Release> releaseList,
                                             List<Commit> commitList) throws IOException {
        List<ReleaseClass> classList = new ArrayList<>();
        List<Commit> lastCommitsList = new ArrayList<>();

        // Per ogni release vogliamo prendere tutte le sue classi, quindi controlliamo il loro ultimo commit
        for (Release release : releaseList) {
            // Ordina ogni elenco di commit per ogni release
            release.getCommitList().sort(Comparator.comparing(commit -> commit.getRevCommit().getCommitterIdent().getWhen()));
            lastCommitsList.add(release.getCommitList().get(release.getCommitList().size() - 1));
        }

        for(Commit lastCommit: lastCommitsList){
            // Ottieni una mappa del nome della classe al codice della classe per la versione effettiva
            Map<String, String> classesNameCodeMap = getClassesNameCodeInfos(lastCommit.getRevCommit());
            for(Map.Entry<String, String> classInfo : classesNameCodeMap.entrySet()){
                classList.add(new ReleaseClass(classInfo.getKey(), classInfo.getValue(), lastCommit.getRelease()));
            }
        }

        // Imposta l'elenco dei commit che tocca la classe per ogni classe
        setTouchingClassesCommits(classList, commitList);

        // Ordina le classi per nome
        classList.sort(Comparator.comparing(ReleaseClass::getName));

        return classList;
    }

    /**
     * Estrae da un commit il nome e il codice sorgente delle classi Java, escludendo le classi di test
     * @param revCommit il commit da cui estrarre le classi
     * @return una mappa che associa il nome della classe al relativo codice sorgente
     * @throws IOException dovuta all'utilizzo delle API di JGit
     */
    private Map<String, String> getClassesNameCodeInfos(RevCommit revCommit) throws IOException {
        Map<String, String> allClasses = new HashMap<>();
        RevTree tree = revCommit.getTree();
        TreeWalk treeWalk = new TreeWalk(repository);
        treeWalk.addTree(tree);
        treeWalk.setRecursive(true);
        while(treeWalk.next()) {
            if(treeWalk.getPathString().contains(".java") && !treeWalk.getPathString().contains("/src/test/")) {
                allClasses.put(treeWalk.getPathString(), new String(repository.open(treeWalk.getObjectId(0)).getBytes(), StandardCharsets.UTF_8));
            }
        }
        treeWalk.close();
        return allClasses;
    }

    /**
     * Aggiunge ogni commit alla lista dei commit che hanno modificato ciascuna classe
     * @param classList lista delle classi su cui impostare la lista dei commit che le toccano
     * @param commitList lista dei commit che hanno modificato le classi in classList
     * @throws IOException se si verifica un errore durante l'estrazione dei nomi delle classi modificate
     */
    private void setTouchingClassesCommits(List<ReleaseClass> classList, List<Commit> commitList) throws IOException {
        List<ReleaseClass> tempProjClasses;

        for(Commit commit: commitList){
            Release release = commit.getRelease();
            tempProjClasses = new ArrayList<>(classList);

            // Ottieni la lista delle classi appartenenti alla release del commit corrente
            tempProjClasses.removeIf(tempProjClass -> !tempProjClass.getRelease().equals(release));

            // Ottieni le classi modificate dal commit corrente
            List<String> modifiedClassesNames = getTouchedClassesNames(commit.getRevCommit());

            // Per ogni classe modificata dal commit corrente,
            // aggiungi il commit alla sua lista di commit che toccano la classe
            for(String modifiedClass: modifiedClassesNames){
                for(ReleaseClass releaseClass : tempProjClasses){
                    if(releaseClass.getName().equals(modifiedClass) && !releaseClass.getTouchingClassCommitList().contains(commit)) {
                        releaseClass.addTouchingClassCommit(commit);
                    }
                }
            }
        }
    }

    /**
     * Ottiene i nomi delle classi toccate da un commit
     *
     * @param commit il commit che ha modificato le classi
     * @return una lista dei nomi delle classi modificate
     * @throws IOException se si verifica un errore durante la lettura delle classi
     */
    private List<String> getTouchedClassesNames(RevCommit commit) throws IOException  {
        List<String> touchedClassesNames = new ArrayList<>();

        // Il DiffFormatter formatta le differenze tra due commit
        try (DiffFormatter diffFormatter = new DiffFormatter(DisabledOutputStream.INSTANCE);
            ObjectReader reader = repository.newObjectReader()) {
            RevCommit commitParent = commit.getParent(0);
            diffFormatter.setRepository(repository);

            // Ottieni l'albero (tree) del commit corrente
            CanonicalTreeParser newTreeIter = new CanonicalTreeParser();
            ObjectId newTree = commit.getTree();
            newTreeIter.reset(reader, newTree);

            // Ottieni l'albero del commit genitore
            CanonicalTreeParser oldTreeIter = new CanonicalTreeParser();
            ObjectId oldTree = commitParent.getTree();
            oldTreeIter.reset(reader, oldTree);

            // Ottieni i nomi delle classi modificate
            List<DiffEntry> entries = diffFormatter.scan(oldTreeIter, newTreeIter);
            for (DiffEntry entry : entries) {
                if (entry.getNewPath().contains(".java") && !entry.getNewPath().contains("/src/test/")) {
                    touchedClassesNames.add(entry.getNewPath());
                }
            }
        } catch (ArrayIndexOutOfBoundsException ignored) {
            // ignorato quando non viene trovato nessun genitore
        }
        return touchedClassesNames;
    }

    /**
     * Imposta per ogni classe le metriche di LOC aggiunti e rimossi
     * @param releaseClass le classi su cui impostare le metriche LOC
     * @throws IOException in caso di errori durante l'uso del diff formatter
     */
    public void extractAddedAndRemovedLOC(ReleaseClass releaseClass) throws IOException {
        for(Commit commit : releaseClass.getTouchingClassCommitList()) {
            RevCommit revCommit = commit.getRevCommit();

            // Ottieni il diff formatter con lo stream di output disabilitato perché non è necessario stampare nulla
            try(DiffFormatter diffFormatter = new DiffFormatter(DisabledOutputStream.INSTANCE)) {

                // Prendi il primo genitore del commit
                RevCommit parentCommit = revCommit.getParent(0);
                diffFormatter.setRepository(repository);

                // Il comparatore di default confronta il testo senza alcun trattamento speciale
                diffFormatter.setDiffComparator(RawTextComparator.DEFAULT);

                // Ottieni le differenze tra i file
                List<DiffEntry> diffEntries = diffFormatter.scan(parentCommit.getTree(), revCommit.getTree());
                for(DiffEntry diffEntry : diffEntries) {
                    if(diffEntry.getNewPath().equals(releaseClass.getName())) {
                        releaseClass.addAddedLOC(getAddedLines(diffFormatter, diffEntry));
                        releaseClass.addRemovedLOC(getDeletedLines(diffFormatter, diffEntry));
                    }
                }
            } catch(ArrayIndexOutOfBoundsException ignored) {
                // ignora quando non viene trovato nessun genitore
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

    /**
     * Inizializza l'attributo "buggyness" a false, prende i commit che toccano ogni classe e con essi imposta l'attributo buggy
     * @param ticketList i ticket da cui prendere le informazioni
     * @param classList le classi su cui impostare le informazioni
     */
    public void labelClassBuggyness(List<Ticket> ticketList, List<ReleaseClass> classList) throws IOException {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");

        // Inizializza l'attributo "buggyness" a false
        for(ReleaseClass releaseClass : classList){
            releaseClass.getMetrics().setBuggyness(false);
        }

        // Per ogni ticket, ottiene i commit e la versione iniettata (IV)
        for(Ticket ticket: ticketList) {
            List<Commit> ticketCommits = ticket.getCommitList();
            Release injectedVersion = ticket.getInjectedVersion();

            for (Commit commit : ticketCommits) {
                RevCommit revCommit = commit.getRevCommit();
                LocalDate commitDate = LocalDate.parse(formatter.format(revCommit.getCommitterIdent().getWhen()));

                // Ottiene la lista dei nomi delle classi modificate dal commit
                List<String> modifiedClassesNames = getTouchedClassesNames(revCommit);

                // Se la data del commit è compresa tra la creazione e la risoluzione del ticket, allora è valido
                if (!commitDate.isAfter(ticket.getResolutionDate()) && !commitDate.isBefore(ticket.getCreationDate())) {
                    // Ottiene la release di quel commit
                    Release releaseOfCommit = commit.getRelease();
                    for (String modifiedClass : modifiedClassesNames) {
                        // Imposta l'attributo "buggyness" di ogni classe modificata
                        labelBuggyClasses(modifiedClass, injectedVersion, releaseOfCommit, classList);
                    }
                }
            }
        }
    }

    /**
     * Imposta come buggy le classi comprese tra la versione iniettata (IV) e la versione corretta (FV) di un bug
     * @param modifiedClass Il nome della classe modificata
     * @param injectedVersion La release IV
     * @param fixedVersion La release FV
     * @param classList Tutte le classi del progetto
     */
    private static void labelBuggyClasses(String modifiedClass, Release injectedVersion,
                                          Release fixedVersion, List<ReleaseClass> classList) {
        for(ReleaseClass releaseClass : classList){
            if( // Ottieni la classe con il nome corretto
                    !releaseClass.getName().equals(modifiedClass) ||
                            // Verifica che la release della classe sia precedente alla FV
                            releaseClass.getRelease().getDateTime().isAfter(fixedVersion.getDateTime()) ||
                            // Verifica che la release della classe sia successiva alla IV
                            releaseClass.getRelease().getDateTime().isBefore(injectedVersion.getDateTime())
            ) continue;

            // Se tutte le condizioni sono soddisfatte, allora la classe è buggy
            releaseClass.getMetrics().setBuggyness(true);
        }
    }

    public void close() {
        if(git != null) git.close();
        if(repository != null) repository.close();
    }
}
