package com.mindmaze.dto;

import com.mindmaze.model.Category;
import java.util.List;

/** Progress dashboard for one player. */
public record ProgressResponse(
        Long playerId,
        String username,
        int totalAttempts,
        int correct,
        int incorrect,
        double accuracyPercent,
        int totalPoints,
        Category strongestCategory,
        Category focusCategory,
        List<CategoryProgress> byCategory) {
}
