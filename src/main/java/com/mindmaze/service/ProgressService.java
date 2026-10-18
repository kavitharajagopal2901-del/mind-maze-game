package com.mindmaze.service;

import com.mindmaze.dto.CategoryProgress;
import com.mindmaze.dto.ProgressResponse;
import com.mindmaze.model.Attempt;
import com.mindmaze.model.Category;
import com.mindmaze.model.Player;
import com.mindmaze.repository.AttemptRepository;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Builds the Progress Dashboard / Personal Performance Map for a player. */
@Service
public class ProgressService {

    private final AttemptRepository attemptRepository;
    private final PlayerService playerService;

    public ProgressService(AttemptRepository attemptRepository, PlayerService playerService) {
        this.attemptRepository = attemptRepository;
        this.playerService = playerService;
    }

    @Transactional(readOnly = true)
    public ProgressResponse progressFor(Long playerId) {
        Player player = playerService.getEntity(playerId);
        List<Attempt> attempts = attemptRepository.findByPlayerIdOrderByIdDesc(playerId);

        // One entry for every category (even untouched ones) so the map is always complete.
        List<CategoryProgress> byCategory = java.util.Arrays.stream(Category.values())
                .map(cat -> summarise(cat, attempts))
                .toList();

        int total = attempts.size();
        int correct = (int) attempts.stream().filter(Attempt::isCorrect).count();
        int points = attempts.stream().mapToInt(Attempt::getPointsEarned).sum();

        List<CategoryProgress> attempted = byCategory.stream().filter(c -> c.attempted() > 0).toList();
        Category strongest = attempted.stream()
                .max(Comparator.comparingDouble(CategoryProgress::accuracyPercent))
                .map(CategoryProgress::category).orElse(null);
        Category focus = attempted.stream()
                .min(Comparator.comparingDouble(CategoryProgress::accuracyPercent))
                .map(CategoryProgress::category).orElse(null);

        return new ProgressResponse(player.getId(), player.getUsername(), total, correct, total - correct,
                percent(correct, total), points, strongest, focus, byCategory);
    }

    private CategoryProgress summarise(Category category, List<Attempt> all) {
        List<Attempt> inCategory = all.stream()
                .filter(a -> a.getChallenge().getCategory() == category)
                .toList();
        int correct = (int) inCategory.stream().filter(Attempt::isCorrect).count();
        int points = inCategory.stream().mapToInt(Attempt::getPointsEarned).sum();
        return new CategoryProgress(category, inCategory.size(), correct,
                percent(correct, inCategory.size()), points);
    }

    private double percent(int part, int whole) {
        return whole == 0 ? 0.0 : Math.round(part * 1000.0 / whole) / 10.0;
    }
}
