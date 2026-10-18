package com.mindmaze.dto;

/** Result of an attempt, including the "Explain My Answer" feedback. */
public record AttemptResponse(
        Long attemptId,
        boolean correct,
        int pointsEarned,
        int hintsUsed,
        String correctAnswer,
        String explanation) {
}
