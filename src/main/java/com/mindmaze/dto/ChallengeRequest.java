package com.mindmaze.dto;

import com.mindmaze.model.Category;
import com.mindmaze.model.Difficulty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Request body for creating or updating a challenge. */
public record ChallengeRequest(
        @NotBlank(message = "title is required")
        @Size(max = 100, message = "title must be at most 100 characters")
        String title,

        @NotNull(message = "category is required")
        Category category,

        @NotNull(message = "difficulty is required")
        Difficulty difficulty,

        @NotBlank(message = "question is required")
        @Size(max = 1000, message = "question must be at most 1000 characters")
        String question,

        @NotBlank(message = "answer is required")
        @Size(max = 255, message = "answer must be at most 255 characters")
        String answer,

        @Size(max = 255, message = "hint1 must be at most 255 characters")
        String hint1,

        @Size(max = 255, message = "hint2 must be at most 255 characters")
        String hint2,

        @Size(max = 1000, message = "explanation must be at most 1000 characters")
        String explanation) {
}
