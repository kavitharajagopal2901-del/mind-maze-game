package com.mindmaze.exception;

/** Thrown when a requested record does not exist (mapped to HTTP 404). */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String resource, Long id) {
        super(resource + " with id " + id + " was not found");
    }

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
