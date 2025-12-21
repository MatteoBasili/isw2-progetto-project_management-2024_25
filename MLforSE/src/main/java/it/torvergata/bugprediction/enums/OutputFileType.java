package it.torvergata.bugprediction.enums;

public enum OutputFileType {

    ARFF("ARFF"),
    CSV("CSV");

    private final String id;

    OutputFileType(String id) {
        this.id = id;
    }

    public static OutputFileType fromString(String id) {
        for (OutputFileType type : values()) {
            if (type.getId().equals(id)) {
                return type;
            }
        }
        return null;
    }

    public String getId() {
        return id;
    }

}
