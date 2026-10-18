package com.mindmaze.exception;

import java.time.LocalDateTime;
import java.util.Map;

/** Uniform JSON error body returned for every failure. */
public record ErrorResponse(
        LocalDateTime timestamp,
        int status,
        String error,
        String message,
        Map<String, String> details) {
}
