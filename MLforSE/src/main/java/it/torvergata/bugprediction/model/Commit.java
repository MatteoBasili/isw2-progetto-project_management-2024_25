package it.torvergata.bugprediction.model;

import java.util.ArrayList;
import java.util.List;

public class Commit {

    private String hash;
    private String author;
    private String date;
    private String message;
    private List<String> changedFiles;

    public Commit(String hash, String author, String date, String message) {
        this.hash = hash;
        this.author = author;
        this.date = date;
        this.message = message;
        this.changedFiles = new ArrayList<>();
    }

    public String getHash() {
        return hash;
    }

    public String getAuthor() {
        return author;
    }

    public String getDate() {
        return date;
    }

    public String getMessage() {
        return message;
    }

    public List<String> getChangedFiles() {
        return changedFiles;
    }

    @Override
    public String toString() {
        return "Commit{" +
                "hash='" + hash + '\'' +
                ", author='" + author + '\'' +
                ", date='" + date + '\'' +
                ", message='" + message + '\'' +
                ", changedFiles=" + changedFiles +
                '}';
    }
}
