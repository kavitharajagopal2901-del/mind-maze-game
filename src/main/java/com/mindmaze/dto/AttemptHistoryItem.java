package com.mindmaze.dto;

import com.mindmaze.model.Attempt;
import com.mindmaze.model.Category;
import java.time.LocalDateTime;

/** One row of a player's attempt history. */
public record AttemptHistoryItem(
        Long attemptId,
        Long challengeId,
        String challengeTitle,
        Category category,
        boolean correct,
        int hintsUsed,
        int pointsEarned,
        LocalDateTime attemptedAt) {

    public static AttemptHistoryItem from(Attempt a) {
        return new AttemptHistoryItem(a.getId(), a.getChallenge().getId(), a.getChallenge().getTitle(),
                a.getChallenge().getCategory(), a.isCorrect(), a.getHintsUsed(), a.getPointsEarned(),
                a.getAttemptedAt());
    }
}
