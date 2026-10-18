package com.mindmaze.dto;

import com.mindmaze.model.Category;

/** One slice of the Personal Performance Map. */
public record CategoryProgress(
        Category category,
        int attempted,
        int correct,
        double accuracyPercent,
        int points) {
}
