package it.torvergata.bugprediction;

public class GitCloneException extends Exception {
    public GitCloneException(String message) {
        super(message);
    }

    public GitCloneException(String message, Throwable cause) {
        super(message, cause);
    }
}
