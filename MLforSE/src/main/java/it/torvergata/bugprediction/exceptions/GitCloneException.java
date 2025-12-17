package it.torvergata.bugprediction.exceptions;

public class GitCloneException extends Exception {
    public GitCloneException(String message) {
        super(message);
    }

    public GitCloneException(String message, Throwable cause) {
        super(message, cause);
    }
}
