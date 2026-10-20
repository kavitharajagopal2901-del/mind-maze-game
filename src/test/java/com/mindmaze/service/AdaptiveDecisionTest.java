package com.mindmaze.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.mindmaze.model.Attempt;
import com.mindmaze.model.Category;
import com.mindmaze.model.Challenge;
import com.mindmaze.model.Difficulty;
import com.mindmaze.model.Player;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Plain unit tests (no Spring, no database) for the adaptive rule:
 * look at the recent attempts, newest first, and decide the next difficulty.
 */
class AdaptiveDecisionTest {

    private static Attempt attempt(Difficulty difficulty, boolean correct) {
        Challenge challenge = new Challenge("t", Category.LOGIC, difficulty, "q", "a", null, null, null);
        int points = correct ? difficulty.getBasePoints() : 0;
        return new Attempt(new Player("tester", "t@example.com"), challenge, correct, 0, points);
    }

    @Test
    @DisplayName("No history: start at EASY")
    void newPlayerStartsEasy() {
        assertEquals(Difficulty.EASY, AdaptiveService.decideDifficulty(List.of()));
    }

    @Test
    @DisplayName("Fewer than 3 attempts: level stays where it was")
    void tooFewAttemptsKeepsLevel() {
        List<Attempt> recent = List.of(attempt(Difficulty.MEDIUM, true), attempt(Difficulty.MEDIUM, true));
        assertEquals(Difficulty.MEDIUM, AdaptiveService.decideDifficulty(recent));
    }

    @Test
    @DisplayName("80% or better moves one level up")
    void highAccuracyMovesUp() {
        List<Attempt> threeOfThree = List.of(attempt(Difficulty.EASY, true), attempt(Difficulty.EASY, true),
                attempt(Difficulty.EASY, true));
        assertEquals(Difficulty.MEDIUM, AdaptiveService.decideDifficulty(threeOfThree));

        List<Attempt> fourOfFive = List.of(attempt(Difficulty.EASY, true), attempt(Difficulty.EASY, true),
                attempt(Difficulty.EASY, false), attempt(Difficulty.EASY, true), attempt(Difficulty.EASY, true));
        assertEquals(Difficulty.MEDIUM, AdaptiveService.decideDifficulty(fourOfFive));
    }

    @Test
    @DisplayName("40% or worse moves one level down")
    void lowAccuracyMovesDown() {
        List<Attempt> zeroOfThree = List.of(attempt(Difficulty.MEDIUM, false), attempt(Difficulty.MEDIUM, false),
                attempt(Difficulty.MEDIUM, false));
        assertEquals(Difficulty.EASY, AdaptiveService.decideDifficulty(zeroOfThree));

        List<Attempt> twoOfFive = List.of(attempt(Difficulty.HARD, false), attempt(Difficulty.HARD, true),
                attempt(Difficulty.HARD, false), attempt(Difficulty.HARD, true), attempt(Difficulty.HARD, false));
        assertEquals(Difficulty.MEDIUM, AdaptiveService.decideDifficulty(twoOfFive));
    }

    @Test
    @DisplayName("In between (e.g. 60%): level stays")
    void middleAccuracyStays() {
        List<Attempt> threeOfFive = List.of(attempt(Difficulty.MEDIUM, true), attempt(Difficulty.MEDIUM, false),
                attempt(Difficulty.MEDIUM, true), attempt(Difficulty.MEDIUM, false), attempt(Difficulty.MEDIUM, true));
        assertEquals(Difficulty.MEDIUM, AdaptiveService.decideDifficulty(threeOfFive));
    }

    @Test
    @DisplayName("Difficulty never goes above HARD or below EASY")
    void limitsAreRespected() {
        List<Attempt> hardAllRight = List.of(attempt(Difficulty.HARD, true), attempt(Difficulty.HARD, true),
                attempt(Difficulty.HARD, true));
        assertEquals(Difficulty.HARD, AdaptiveService.decideDifficulty(hardAllRight));

        List<Attempt> easyAllWrong = List.of(attempt(Difficulty.EASY, false), attempt(Difficulty.EASY, false),
                attempt(Difficulty.EASY, false));
        assertEquals(Difficulty.EASY, AdaptiveService.decideDifficulty(easyAllWrong));
    }
}
