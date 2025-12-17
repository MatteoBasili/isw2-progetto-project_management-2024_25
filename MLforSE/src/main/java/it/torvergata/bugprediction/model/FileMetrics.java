package it.torvergata.bugprediction.model;

import java.util.HashSet;
import java.util.Set;

/**
 * Container for class-level metrics for a single file.
 */
public class FileMetrics {

    private final String filePath;

    // Metrics
    private int LOC;
    private int LOC_touched;
    private int NR;
    private int NFix;
    private final Set<String> authors = new HashSet<>();

    private int LOC_added;
    private int Max_LOC_added;
    private double Avg_LOC_added;

    private int Churn;
    private int Max_Churn;
    private double Avg_Churn;

    public FileMetrics(String filePath) {
        this.filePath = filePath;
    }

    // === Metric updates ===
    public void incrementNR() { NR++; }
    public void incrementNFix() { NFix++; }
    public void addAuthor(String a) { authors.add(a); }
    public void addLOCAdded(int n) { LOC_added += n; }
    public void updateMaxLOCAdded(int n) { if (n > Max_LOC_added) Max_LOC_added = n; }
    public void updateAvgLOCAdded() { Avg_LOC_added = NR > 0 ? (double) LOC_added / NR : 0.0; }

    public void addChurn(int n) { Churn += n; }
    public void updateMaxChurn(int n) { if (n > Max_Churn) Max_Churn = n; }
    public void updateAvgChurn() { Avg_Churn = NR > 0 ? (double) Churn / NR : 0.0; }

    public void addLOCTouched(int n) { LOC_touched += n; }
    public void setLOC(int n) { LOC = n; }

    // === Getters ===
    public String getFilePath() { return filePath; }
    public int getLOC() { return LOC; }
    public int getLOC_touched() { return LOC_touched; }
    public int getNR() { return NR; }
    public int getNFix() { return NFix; }
    public int getNAuth() { return authors.size(); }
    public int getLOC_added() { return LOC_added; }
    public int getMax_LOC_added() { return Max_LOC_added; }
    public double getAvg_LOC_added() { return Avg_LOC_added; }
    public int getChurn() { return Churn; }
    public int getMax_Churn() { return Max_Churn; }
    public double getAvg_Churn() { return Avg_Churn; }
}
