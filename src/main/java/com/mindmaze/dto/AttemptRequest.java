package com.mindmaze.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** A player's answer to a challenge. */
public record AttemptRequest(
        @NotNull(message = "playerId is required")
        Long playerId,

        @NotNull(message = "challengeId is required")
        Long challengeId,

        @NotBlank(message = "answer is required")
        String answer,

        @Min(value = 0, message = "hintsUsed cannot be negative")
        @Max(value = 2, message = "hintsUsed cannot be more than 2")
        Integer hintsUsed) {
}
