package it.torvergata.bugprediction.models;

public class AcumeInstance {

    private int id;
    private int size;
    private double predicted;
    private String actual;

    public AcumeInstance(int id, int size, double predicted, String actual) {
        if(predicted < 0 || predicted > 1)
            throw new IllegalArgumentException("Il valore predetto deve essere compreso tra 0 e 1");

        this.id = id;
        this.size = size;
        this.predicted = predicted;
        this.actual = actual;
    }

    public int getId() {
        return id;
    }
    public int getSize() {
        return size;
    }
    public double getPredicted() {
        return predicted;
    }

    public String getActual() {
        return actual;
    }

}
