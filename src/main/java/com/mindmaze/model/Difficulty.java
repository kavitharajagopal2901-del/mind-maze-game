package com.mindmaze.model;

/**
 * Difficulty levels. Each level carries the base points awarded for a correct
 * answer and knows its neighbouring levels, which the adaptive engine uses.
 */
public enum Difficulty {
    EASY(10),
    MEDIUM(20),
    HARD(30);

    private final int basePoints;

    Difficulty(int basePoints) {
        this.basePoints = basePoints;
    }

    public int getBasePoints() {
        return basePoints;
    }

    /** Next level up; HARD stays HARD. */
    public Difficulty harder() {
        return this == HARD ? HARD : values()[ordinal() + 1];
    }

    /** Next level down; EASY stays EASY. */
    public Difficulty easier() {
        return this == EASY ? EASY : values()[ordinal() - 1];
    }
}
