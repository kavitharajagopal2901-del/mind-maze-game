package com.mindmaze;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.mindmaze.model.Difficulty;
import org.junit.jupiter.api.Test;

/** Plain unit test (no Spring): difficulty stepping used by the adaptive engine. */
class DifficultyTest {

    @Test
    void harderStepsUpAndStopsAtHard() {
        assertEquals(Difficulty.MEDIUM, Difficulty.EASY.harder());
        assertEquals(Difficulty.HARD, Difficulty.MEDIUM.harder());
        assertEquals(Difficulty.HARD, Difficulty.HARD.harder());
    }

    @Test
    void easierStepsDownAndStopsAtEasy() {
        assertEquals(Difficulty.MEDIUM, Difficulty.HARD.easier());
        assertEquals(Difficulty.EASY, Difficulty.MEDIUM.easier());
        assertEquals(Difficulty.EASY, Difficulty.EASY.easier());
    }

    @Test
    void basePointsGrowWithDifficulty() {
        assertEquals(10, Difficulty.EASY.getBasePoints());
        assertEquals(20, Difficulty.MEDIUM.getBasePoints());
        assertEquals(30, Difficulty.HARD.getBasePoints());
    }
}
