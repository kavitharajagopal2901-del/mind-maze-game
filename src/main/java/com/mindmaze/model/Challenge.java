package com.mindmaze.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** A single brain challenge (question, answer, staged hints and an explanation). */
@Entity
@Table(name = "challenges")
public class Challenge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Category category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Difficulty difficulty;

    @Column(nullable = false, length = 1000)
    private String question;

    @Column(nullable = false)
    private String answer;

    private String hint1;
    private String hint2;

    /** Shown after an attempt ("Explain My Answer"). */
    @Column(length = 1000)
    private String explanation;

    protected Challenge() {
        // required by JPA
    }

    public Challenge(String title, Category category, Difficulty difficulty, String question,
                     String answer, String hint1, String hint2, String explanation) {
        this.title = title;
        this.category = category;
        this.difficulty = difficulty;
        this.question = question;
        this.answer = answer;
        this.hint1 = hint1;
        this.hint2 = hint2;
        this.explanation = explanation;
    }

    /** Case-insensitive, whitespace-tolerant answer check. */
    public boolean isCorrectAnswer(String submitted) {
        return submitted != null && answer.trim().equalsIgnoreCase(submitted.trim());
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }
    public Difficulty getDifficulty() { return difficulty; }
    public void setDifficulty(Difficulty difficulty) { this.difficulty = difficulty; }
    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }
    public String getAnswer() { return answer; }
    public void setAnswer(String answer) { this.answer = answer; }
    public String getHint1() { return hint1; }
    public void setHint1(String hint1) { this.hint1 = hint1; }
    public String getHint2() { return hint2; }
    public void setHint2(String hint2) { this.hint2 = hint2; }
    public String getExplanation() { return explanation; }
    public void setExplanation(String explanation) { this.explanation = explanation; }
}
