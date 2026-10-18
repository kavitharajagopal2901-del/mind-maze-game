package com.mindmaze.config;

import static com.mindmaze.model.Category.CODE_BREAKER;
import static com.mindmaze.model.Category.LOGIC;
import static com.mindmaze.model.Category.MATH;
import static com.mindmaze.model.Category.MEMORY;
import static com.mindmaze.model.Category.PATTERN;
import static com.mindmaze.model.Difficulty.EASY;
import static com.mindmaze.model.Difficulty.HARD;
import static com.mindmaze.model.Difficulty.MEDIUM;

import com.mindmaze.model.Challenge;
import com.mindmaze.repository.ChallengeRepository;
import java.util.List;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Loads a starter bank of 15 challenges (5 categories x 3 difficulties) the first
 * time the app runs against an empty database. Skipped in the "test" profile.
 */
@Component
@Profile("!test")
public class DataSeeder implements CommandLineRunner {

    private final ChallengeRepository challengeRepository;

    public DataSeeder(ChallengeRepository challengeRepository) {
        this.challengeRepository = challengeRepository;
    }

    @Override
    public void run(String... args) {
        if (challengeRepository.count() > 0) {
            return; // already seeded - keep existing data
        }
        challengeRepository.saveAll(List.of(
                // ---- Logic Lab ----
                new Challenge("Cats and animals", LOGIC, EASY,
                        "All cats are animals. Tom is a cat. Is Tom an animal? (yes/no)", "yes",
                        "Tom belongs to the group 'cats'.", "Every cat is inside the bigger group 'animals'.",
                        "Tom is a cat, and every cat is an animal, so Tom must be an animal."),
                new Challenge("Bloops, Razzies and Lazzies", LOGIC, MEDIUM,
                        "All Bloops are Razzies and all Razzies are Lazzies. Are all Bloops definitely Lazzies? (yes/no)",
                        "yes", "Follow the chain Bloop -> Razzie.", "Then continue the chain Razzie -> Lazzie.",
                        "Bloops sit inside Razzies, which sit inside Lazzies, so Bloops are Lazzies (transitivity)."),
                new Challenge("Mislabelled boxes", LOGIC, HARD,
                        "Three boxes are labelled Apples, Oranges and Mixed, and every label is wrong. "
                                + "You take one fruit from the box labelled Mixed and it is an apple. "
                                + "What does the box labelled Mixed really contain? (one word)",
                        "apples", "The Mixed label is wrong, so the box is not mixed.",
                        "A single apple came out, so the box holds only one kind of fruit.",
                        "The label is wrong, so the box is not mixed; you drew an apple, so it holds only apples."),

                // ---- Pattern Detective ----
                new Challenge("Counting by twos", PATTERN, EASY,
                        "What comes next? 2, 4, 6, 8, ?", "10",
                        "Look at the gap between neighbours.", "The gap is always 2.",
                        "The sequence adds 2 each time, so 8 + 2 = 10."),
                new Challenge("Square numbers", PATTERN, MEDIUM,
                        "What comes next? 1, 4, 9, 16, ?", "25",
                        "Try 1x1, 2x2, 3x3...", "These are perfect squares; the next base is 5.",
                        "The terms are 1^2, 2^2, 3^2, 4^2, so the next is 5^2 = 25."),
                new Challenge("Growing gaps", PATTERN, HARD,
                        "What comes next? 2, 6, 12, 20, 30, ?", "42",
                        "Write down the differences between neighbours.", "The differences are 4, 6, 8, 10 - what is next?",
                        "Differences grow by 2 (4, 6, 8, 10, 12), so 30 + 12 = 42. They are n x (n+1)."),

                // ---- Memory Matrix ----
                new Challenge("Four digits", MEMORY, EASY,
                        "Memorise this sequence: 7 3 9 1. What was the second number?", "3",
                        "Count from the left.", "The sequence starts 7, then ...",
                        "In 7 3 9 1 the second number is 3."),
                new Challenge("Six digits, first plus last", MEMORY, MEDIUM,
                        "Memorise: 5 8 2 6 4 1. What is the sum of the first and last numbers?", "6",
                        "You only need two of the six numbers.", "The first is 5; find the last.",
                        "The first number is 5 and the last is 1, so 5 + 1 = 6."),
                new Challenge("Letter pairs", MEMORY, HARD,
                        "Memorise: A7 B3 C9 D1 E5. Which letter was paired with 9? (one letter)", "C",
                        "Scan the pairs for the number 9.", "It is early in the alphabet - one of the first three.",
                        "C was paired with 9 (C9)."),

                // ---- Code Breaker ----
                new Challenge("One step forward", CODE_BREAKER, EASY,
                        "Each letter was moved 1 step forward in the alphabet. Decode: IBM", "HAL",
                        "To decode, move each letter one step back.", "I goes back to H.",
                        "Shifting each letter back by 1: I->H, B->A, M->L gives HAL."),
                new Challenge("Number letters", CODE_BREAKER, MEDIUM,
                        "Using A=1, B=2 ... Z=26, decode: 3-1-20", "CAT",
                        "Count along the alphabet to find the 3rd letter.", "3 is C, 1 is A - find the 20th letter.",
                        "3 = C, 1 = A, 20 = T, so the word is CAT."),
                new Challenge("Caesar shift of three", CODE_BREAKER, HARD,
                        "Each letter was moved 3 steps forward in the alphabet. Decode: MDYD", "JAVA",
                        "Move each letter 3 steps back.", "M moves back to J.",
                        "Shifting back by 3: M->J, D->A, Y->V, D->A gives JAVA."),

                // ---- Math Maze ----
                new Challenge("Quick sum", MATH, EASY,
                        "What is 12 + 15?", "27",
                        "Add the tens, then the units.", "10 + 10 = 20 and 2 + 5 = 7.",
                        "12 + 15 = 27."),
                new Challenge("Percentage", MATH, MEDIUM,
                        "What is 15% of 240?", "36",
                        "10% of 240 is 24.", "5% is half of 10%.",
                        "10% = 24 and 5% = 12, so 15% = 24 + 12 = 36."),
                new Challenge("Train speed", MATH, HARD,
                        "A train travels 150 km in 2.5 hours. At the same speed, how many km does it travel in 4 hours?",
                        "240", "First find the speed in km per hour.", "Speed = 150 / 2.5 = 60 km/h.",
                        "Speed is 60 km/h, so in 4 hours it covers 60 x 4 = 240 km.")));
    }
}
