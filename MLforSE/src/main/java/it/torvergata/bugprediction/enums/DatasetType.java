package it.torvergata.bugprediction.enums;

public enum DatasetType {

    TRAINING("TRAINING"),
    TESTING("TESTING");

    private final String id;

    DatasetType(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

}
