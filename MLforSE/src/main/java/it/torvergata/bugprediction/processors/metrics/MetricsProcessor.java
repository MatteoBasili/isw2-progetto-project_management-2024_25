package it.torvergata.bugprediction.processors.metrics;

import it.torvergata.bugprediction.datasource.git.GitRepositoryAnalyzer;
import it.torvergata.bugprediction.models.*;
import org.eclipse.jgit.revwalk.RevCommit;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MetricsProcessor {

    List<Release> releaseList;
    List<Commit> ticketedCommitList;
    List<ReleaseClass> classList;
    GitRepositoryAnalyzer gitRepoMiner;
    String projName;

    public MetricsProcessor(List<Release> releaseList, List<Commit> ticketedCommitList, List<ReleaseClass> classList,
                            GitRepositoryAnalyzer gitRepoMiner, String projName) {
        this.releaseList = releaseList;
        this.ticketedCommitList = ticketedCommitList;
        this.classList = classList;
        this.gitRepoMiner = gitRepoMiner;
        this.projName = projName.toLowerCase();
    }

    public void processMetrics() throws IOException {
        processSize();
        processNumberOfRevisions();
        processNumberOfDefectFixes();
        processNumberOfAuthors();
        processLOCMetrics();
    }

    private void processSize() {
        for (ReleaseClass currentClass : classList) {
            String[] lines = currentClass.getContentOfClass().split("\r\n|\r|\n");
            currentClass.getMetrics().setSize(lines.length);
        }
    }

    private void processNumberOfRevisions() {
        for (ReleaseClass currentClass : classList) {
            currentClass.getMetrics().setNumberOfRevisions(currentClass.getTouchingClassCommitList().size());
        }
    }

    /**
     * Per ogni classe del progetto nella classList calcola il numero di correzioni di bug come il numero di commit che
     * modificano la classe
     */
    private void processNumberOfDefectFixes() {
        for (ReleaseClass releaseClass : classList) {
            // Inizializza una lista vuota di ticket che rappresentano i bug che hanno coinvolto la classe
            List<Ticket> classBugs = new ArrayList<>();

            for (Commit touchingClassCommit : releaseClass.getTouchingClassCommitList()) {
                // Se il commit fa riferimento a un ticket non ancora considerato nel conteggio dei bug
                if (!classBugs.contains(touchingClassCommit.getTicket()) && ticketedCommitList.contains(touchingClassCommit)) {
                    // Aggiungi il ticket alla lista dei bug per quella classe
                    classBugs.add(touchingClassCommit.getTicket());
                }
            }
            // Il numero di correzioni di bug per la classe è il numero di ticket che hanno coinvolto quella classe
            releaseClass.getMetrics().setNumberOfDefectFixes(classBugs.size());
        }
    }

    private void processNumberOfAuthors() {
        for (ReleaseClass releaseClass : classList) {
            List<String> authorsOfClass = new ArrayList<>();
            for (Commit commit : releaseClass.getTouchingClassCommitList()) {
                RevCommit revCommit = commit.getRevCommit();
                if (!authorsOfClass.contains(revCommit.getAuthorIdent().getName())) {
                    authorsOfClass.add(revCommit.getAuthorIdent().getName());
                }
            }
            releaseClass.getMetrics().setNumberOfAuthors(authorsOfClass.size());
        }
    }

    private void processLOCMetrics() throws IOException {
        // Le metriche max, avg e il valore attuale sono zero di default
        int i;
        for (ReleaseClass currentClass : classList) {
            LOCMetrics removedLOC = new LOCMetrics();
            LOCMetrics churnLOC = new LOCMetrics();
            LOCMetrics addedLOC = new LOCMetrics();

            // Imposta le metriche LOC aggiunte e rimosse per ogni classe
            gitRepoMiner.extractAddedAndRemovedLOC(currentClass);

            List<Integer> locAddedByClass = currentClass.getAddedLOCList();
            List<Integer> locRemovedByClass = currentClass.getRemovedLOCList();

            // Le dimensioni delle liste di LOC aggiunte e rimosse sono uguali
            for (i = 0; i < locAddedByClass.size(); i++) {

                int addedLineOfCode = locAddedByClass.get(i);
                int removedLineOfCode = locRemovedByClass.get(i);
                int churningFactor = Math.abs(locAddedByClass.get(i) - locRemovedByClass.get(i));

                // Imposta i valori
                addedLOC.addToVal(addedLineOfCode);
                removedLOC.addToVal(removedLineOfCode);
                churnLOC.addToVal(churningFactor);

                // Imposta i valori massimi
                if (addedLineOfCode > addedLOC.getMaxVal()) {
                    addedLOC.setMaxVal(addedLineOfCode);
                }
                if (removedLineOfCode > removedLOC.getMaxVal()) {
                    removedLOC.setMaxVal(removedLineOfCode);
                }
                if (churningFactor > churnLOC.getMaxVal()) {
                    churnLOC.setMaxVal(churningFactor);
                }
            }

            processAverageLOCMetrics(currentClass, locAddedByClass, locRemovedByClass,
                    addedLOC, removedLOC, churnLOC);
        }
    }

    private void processAverageLOCMetrics(ReleaseClass currentClass, List<Integer> locAddedByClass, List<Integer> locRemovedByClass,
                                          LOCMetrics addedLOC, LOCMetrics removedLOC, LOCMetrics churnLOC){
        // Imposta i valori medi
        int nRevisions = currentClass.getMetrics().getNumberOfRevisions();
        if(!locAddedByClass.isEmpty()) {
            addedLOC.setAvgVal(1.0* addedLOC.getVal()/ nRevisions);
        }
        if(!locRemovedByClass.isEmpty()) {
            removedLOC.setAvgVal(1.0* removedLOC.getVal()/ nRevisions);
        }
        if(!locAddedByClass.isEmpty() || !locRemovedByClass.isEmpty()) {
            churnLOC.setAvgVal(1.0* churnLOC.getVal()/ nRevisions);
        }
        currentClass.getMetrics().setAddedLOCMetrics(addedLOC.getVal(), addedLOC.getMaxVal(), addedLOC.getAvgVal());
        currentClass.getMetrics().setRemovedLOCMetrics(removedLOC.getVal(), removedLOC.getMaxVal(), removedLOC.getAvgVal());
        currentClass.getMetrics().setChurnMetrics(churnLOC.getVal(), churnLOC.getMaxVal(), churnLOC.getAvgVal());
    }

}
