package com.mindmaze.exception;

/** Thrown when a unique value (e.g. username) is already taken (mapped to HTTP 409). */
public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String message) {
        super(message);
    }
}
