package com.mindmaze.service;

import com.mindmaze.dto.ChallengeResponse;
import com.mindmaze.dto.NextChallengeResponse;
import com.mindmaze.exception.ResourceNotFoundException;
import com.mindmaze.model.Attempt;
import com.mindmaze.model.Category;
import com.mindmaze.model.Challenge;
import com.mindmaze.model.Difficulty;
import com.mindmaze.repository.AttemptRepository;
import com.mindmaze.repository.ChallengeRepository;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Adaptive Challenge Engine. Uses simple, explainable rules (no machine learning):
 * look at the player's last five attempts and move difficulty up or down.
 */
@Service
public class AdaptiveService {

    /** Minimum recent attempts before difficulty is allowed to change. */
    static final int MIN_ATTEMPTS = 3;
    static final double LEVEL_UP_ACCURACY = 0.8;
    static final double LEVEL_DOWN_ACCURACY = 0.4;

    private final AttemptRepository attemptRepository;
    private final ChallengeRepository challengeRepository;
    private final PlayerService playerService;

    public AdaptiveService(AttemptRepository attemptRepository, ChallengeRepository challengeRepository,
                           PlayerService playerService) {
        this.attemptRepository = attemptRepository;
        this.challengeRepository = challengeRepository;
        this.playerService = playerService;
    }

    /**
     * Picks the next unsolved challenge, in the requested category if given,
     * at the difficulty suggested by the player's recent performance.
     */
    @Transactional(readOnly = true)
    public NextChallengeResponse nextChallenge(Long playerId, Category category) {
        playerService.getEntity(playerId); // 404 if the player does not exist

        List<Attempt> recent = category == null
                ? attemptRepository.findTop5ByPlayerIdOrderByIdDesc(playerId)
                : attemptRepository.findTop5ByPlayerIdAndChallengeCategoryOrderByIdDesc(playerId, category);

        Difficulty target = decideDifficulty(recent);
        Set<Long> solved = new HashSet<>(attemptRepository.findSolvedChallengeIds(playerId));

        List<Challenge> pool = category == null
                ? challengeRepository.findAll()
                : challengeRepository.findByCategory(category);

        // Prefer the target difficulty; if it has run out, take the closest one available.
        Challenge pick = pool.stream()
                .filter(c -> !solved.contains(c.getId()))
                .min(Comparator
                        .comparingInt((Challenge c) -> Math.abs(c.getDifficulty().ordinal() - target.ordinal()))
                        .thenComparing(Challenge::getId))
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No unsolved challenges left" + (category == null ? "" : " in " + category)));

        return new NextChallengeResponse(target, explain(recent, target), ChallengeResponse.from(pick));
    }

    /**
     * The core rule. With fewer than {@value #MIN_ATTEMPTS} recent attempts the level stays put
     * (EASY for a brand-new player). Otherwise accuracy of the recent window decides:
     * &gt;= 80% moves up, &lt;= 40% moves down, anything between stays.
     *
     * @param recent recent attempts, newest first
     */
    static Difficulty decideDifficulty(List<Attempt> recent) {
        if (recent.isEmpty()) {
            return Difficulty.EASY;
        }
        Difficulty current = recent.get(0).getChallenge().getDifficulty();
        if (recent.size() < MIN_ATTEMPTS) {
            return current;
        }
        double accuracy = accuracyOf(recent);
        if (accuracy >= LEVEL_UP_ACCURACY) {
            return current.harder();
        }
        if (accuracy <= LEVEL_DOWN_ACCURACY) {
            return current.easier();
        }
        return current;
    }

    private static double accuracyOf(List<Attempt> attempts) {
        long correct = attempts.stream().filter(Attempt::isCorrect).count();
        return (double) correct / attempts.size();
    }

    private String explain(List<Attempt> recent, Difficulty target) {
        if (recent.isEmpty()) {
            return "New here - starting at " + target;
        }
        return String.format("Accuracy %.0f%% over your last %d attempts -> %s",
                accuracyOf(recent) * 100, recent.size(), target);
    }
}
