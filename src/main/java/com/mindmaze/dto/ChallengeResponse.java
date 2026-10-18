package com.mindmaze.dto;

import com.mindmaze.model.Category;
import com.mindmaze.model.Challenge;
import com.mindmaze.model.Difficulty;

/**
 * What a player sees. The answer and explanation are deliberately left out so
 * the question can be shown without giving the solution away.
 */
public record ChallengeResponse(
        Long id,
        String title,
        Category category,
        Difficulty difficulty,
        String question,
        int points,
        int hintsAvailable) {

    public static ChallengeResponse from(Challenge c) {
        int hints = (c.getHint1() != null ? 1 : 0) + (c.getHint2() != null ? 1 : 0);
        return new ChallengeResponse(c.getId(), c.getTitle(), c.getCategory(), c.getDifficulty(),
                c.getQuestion(), c.getDifficulty().getBasePoints(), hints);
    }
}
