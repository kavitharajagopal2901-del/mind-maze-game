package com.mindmaze.model;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/** One answer submitted by a player for a challenge. */
@Entity
@Table(name = "attempts")
public class Attempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "player_id")
    private Player player;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "challenge_id")
    private Challenge challenge;

    private boolean correct;
    private int hintsUsed;
    private int pointsEarned;
    private LocalDateTime attemptedAt;

    protected Attempt() {
        // required by JPA
    }

    public Attempt(Player player, Challenge challenge, boolean correct, int hintsUsed, int pointsEarned) {
        this.player = player;
        this.challenge = challenge;
        this.correct = correct;
        this.hintsUsed = hintsUsed;
        this.pointsEarned = pointsEarned;
        this.attemptedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Player getPlayer() { return player; }
    public Challenge getChallenge() { return challenge; }
    public boolean isCorrect() { return correct; }
    public int getHintsUsed() { return hintsUsed; }
    public int getPointsEarned() { return pointsEarned; }
    public LocalDateTime getAttemptedAt() { return attemptedAt; }
}
