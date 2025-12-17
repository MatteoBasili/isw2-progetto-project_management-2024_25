package it.torvergata.bugprediction.model;

import java.time.LocalDateTime;
import java.util.Objects;

public class Release implements Comparable<Release> {
    private String id;
    private String name;
    private LocalDateTime date;

    public Release(String id, String name, LocalDateTime date) {
        this.id = id;
        this.name = name;
        this.date = date;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public LocalDateTime getDate() { return date; }

    @Override
    public int compareTo(Release other) {
        return this.date.compareTo(other.date);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Release)) return false;
        Release release = (Release) o;
        return Objects.equals(date, release.date);
    }

    @Override
    public int hashCode() {
        return Objects.hash(date);
    }

    @Override
    public String toString() {
        return name + " (" + date.toLocalDate() + ")";
    }
}
