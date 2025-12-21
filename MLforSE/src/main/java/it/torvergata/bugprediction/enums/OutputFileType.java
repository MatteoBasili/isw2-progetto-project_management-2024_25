package it.torvergata.bugprediction.enums;

public enum OutputFileType {

    ARFF("ARFF"),
    CSV("CSV");

    private final String id;

    OutputFileType(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

}
