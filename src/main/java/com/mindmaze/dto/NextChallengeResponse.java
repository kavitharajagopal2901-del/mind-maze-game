package com.mindmaze.dto;

import com.mindmaze.model.Difficulty;

/** The adaptive engine's recommendation, with the reason behind it. */
public record NextChallengeResponse(
        Difficulty targetDifficulty,
        String reason,
        ChallengeResponse challenge) {
}
