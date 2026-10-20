package com.mindmaze;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/** Players, scoring, the adaptive engine and the progress dashboard working together. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class GameplayFlowTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper om;

    // ---------- players ----------

    @Test
    @DisplayName("Duplicate username is rejected with 409")
    void duplicateUsernameReturns409() throws Exception {
        TestApi.createPlayer(mvc, om, "kavi");
        Map<String, Object> body = Map.of("username", "kavi", "email", "other@example.com");

        mvc.perform(post("/api/players").contentType(APPLICATION_JSON).content(om.writeValueAsString(body)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(containsString("already taken")));
    }

    @Test
    @DisplayName("Invalid player data is rejected with 400")
    void invalidPlayerReturns400() throws Exception {
        Map<String, Object> body = Map.of("username", "ab", "email", "not-an-email");

        mvc.perform(post("/api/players").contentType(APPLICATION_JSON).content(om.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.username").exists())
                .andExpect(jsonPath("$.details.email").exists());
    }

    @Test
    @DisplayName("Deleting a player also removes their history")
    void deletePlayerRemovesHistory() throws Exception {
        long player = TestApi.createPlayer(mvc, om, "temp-user");
        long challenge = TestApi.createChallenge(mvc, om, "C", "LOGIC", "EASY", "yes");
        TestApi.attempt(mvc, om, player, challenge, "yes", 0);

        mvc.perform(delete("/api/players/" + player)).andExpect(status().isNoContent());
        mvc.perform(get("/api/players/" + player)).andExpect(status().isNotFound());
    }

    // ---------- scoring ----------

    @Test
    @DisplayName("Correct answer earns the base points and explains itself")
    void correctAnswerEarnsPoints() throws Exception {
        long player = TestApi.createPlayer(mvc, om, "scorer");
        long challenge = TestApi.createChallenge(mvc, om, "Easy one", "MATH", "EASY", "27");

        JsonNode result = TestApi.attempt(mvc, om, player, challenge, "  27 ", 0);

        assertTrue(result.get("correct").asBoolean());
        assertEquals(10, result.get("pointsEarned").asInt());
        assertEquals("Because 27", result.get("explanation").asText());
    }

    @Test
    @DisplayName("Answers are case-insensitive")
    void answersIgnoreCase() throws Exception {
        long player = TestApi.createPlayer(mvc, om, "caser");
        long challenge = TestApi.createChallenge(mvc, om, "Word", "CODE_BREAKER", "EASY", "HAL");

        assertTrue(TestApi.attempt(mvc, om, player, challenge, "hal", 0).get("correct").asBoolean());
    }

    @Test
    @DisplayName("Each hint costs 5 points; points never drop below 1")
    void hintsReducePoints() throws Exception {
        long player = TestApi.createPlayer(mvc, om, "hinter");
        long easy = TestApi.createChallenge(mvc, om, "E", "LOGIC", "EASY", "a");
        long hard = TestApi.createChallenge(mvc, om, "H", "LOGIC", "HARD", "a");

        assertEquals(5, TestApi.attempt(mvc, om, player, easy, "a", 1).get("pointsEarned").asInt());
        assertEquals(1, TestApi.attempt(mvc, om, player, easy, "a", 2).get("pointsEarned").asInt());
        assertEquals(20, TestApi.attempt(mvc, om, player, hard, "a", 2).get("pointsEarned").asInt());
    }

    @Test
    @DisplayName("Wrong answer earns 0 but still reveals the explanation")
    void wrongAnswerEarnsNothing() throws Exception {
        long player = TestApi.createPlayer(mvc, om, "wrong");
        long challenge = TestApi.createChallenge(mvc, om, "Tricky", "LOGIC", "MEDIUM", "yes");

        JsonNode result = TestApi.attempt(mvc, om, player, challenge, "no", 0);

        assertFalse(result.get("correct").asBoolean());
        assertEquals(0, result.get("pointsEarned").asInt());
        assertEquals("yes", result.get("correctAnswer").asText());
        assertEquals("Because yes", result.get("explanation").asText());
    }

    @Test
    @DisplayName("Attempts with unknown player or challenge return 404; bad hint count returns 400")
    void attemptValidation() throws Exception {
        long player = TestApi.createPlayer(mvc, om, "checker");
        long challenge = TestApi.createChallenge(mvc, om, "C", "LOGIC", "EASY", "a");

        mvc.perform(post("/api/attempts").contentType(APPLICATION_JSON).content(
                        om.writeValueAsString(Map.of("playerId", 999999, "challengeId", challenge, "answer", "a"))))
                .andExpect(status().isNotFound());
        mvc.perform(post("/api/attempts").contentType(APPLICATION_JSON).content(
                        om.writeValueAsString(Map.of("playerId", player, "challengeId", 999999, "answer", "a"))))
                .andExpect(status().isNotFound());
        mvc.perform(post("/api/attempts").contentType(APPLICATION_JSON).content(
                        om.writeValueAsString(Map.of("playerId", player, "challengeId", challenge,
                                "answer", "a", "hintsUsed", 5))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.hintsUsed").exists());
    }

    // ---------- adaptive engine ----------

    @Test
    @DisplayName("A brand-new player is offered an EASY challenge")
    void newPlayerStartsEasy() throws Exception {
        long player = TestApi.createPlayer(mvc, om, "newbie");
        TestApi.createChallenge(mvc, om, "Hard first", "LOGIC", "HARD", "a");
        TestApi.createChallenge(mvc, om, "Easy second", "LOGIC", "EASY", "a");

        mvc.perform(get("/api/players/" + player + "/next-challenge"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.targetDifficulty").value("EASY"))
                .andExpect(jsonPath("$.challenge.title").value("Easy second"));
    }

    @Test
    @DisplayName("Three correct answers in a row move the player up to MEDIUM")
    void doingWellMovesUp() throws Exception {
        long player = TestApi.createPlayer(mvc, om, "climber");
        long e1 = TestApi.createChallenge(mvc, om, "E1", "LOGIC", "EASY", "a");
        long e2 = TestApi.createChallenge(mvc, om, "E2", "LOGIC", "EASY", "a");
        long e3 = TestApi.createChallenge(mvc, om, "E3", "LOGIC", "EASY", "a");
        TestApi.createChallenge(mvc, om, "Medium one", "LOGIC", "MEDIUM", "a");
        TestApi.attempt(mvc, om, player, e1, "a", 0);
        TestApi.attempt(mvc, om, player, e2, "a", 0);
        TestApi.attempt(mvc, om, player, e3, "a", 0);

        mvc.perform(get("/api/players/" + player + "/next-challenge").param("category", "LOGIC"))
                .andExpect(jsonPath("$.targetDifficulty").value("MEDIUM"))
                .andExpect(jsonPath("$.challenge.title").value("Medium one"))
                .andExpect(jsonPath("$.reason").value(containsString("100%")));
    }

    @Test
    @DisplayName("Struggling on MEDIUM moves the player back down to EASY")
    void strugglingMovesDown() throws Exception {
        long player = TestApi.createPlayer(mvc, om, "struggler");
        long m1 = TestApi.createChallenge(mvc, om, "M1", "MATH", "MEDIUM", "a");
        long m2 = TestApi.createChallenge(mvc, om, "M2", "MATH", "MEDIUM", "a");
        long m3 = TestApi.createChallenge(mvc, om, "M3", "MATH", "MEDIUM", "a");
        TestApi.createChallenge(mvc, om, "Easy rescue", "MATH", "EASY", "a");
        TestApi.attempt(mvc, om, player, m1, "wrong", 0);
        TestApi.attempt(mvc, om, player, m2, "wrong", 0);
        TestApi.attempt(mvc, om, player, m3, "wrong", 0);

        mvc.perform(get("/api/players/" + player + "/next-challenge").param("category", "MATH"))
                .andExpect(jsonPath("$.targetDifficulty").value("EASY"))
                .andExpect(jsonPath("$.challenge.title").value("Easy rescue"));
    }

    @Test
    @DisplayName("Already-solved challenges are not offered again; falls back to nearest difficulty")
    void solvedChallengesAreSkipped() throws Exception {
        long player = TestApi.createPlayer(mvc, om, "repeater");
        long only = TestApi.createChallenge(mvc, om, "Only easy", "LOGIC", "EASY", "a");
        TestApi.createChallenge(mvc, om, "Only medium", "LOGIC", "MEDIUM", "a");
        TestApi.attempt(mvc, om, player, only, "a", 0);

        mvc.perform(get("/api/players/" + player + "/next-challenge").param("category", "LOGIC"))
                .andExpect(jsonPath("$.challenge.title").value("Only medium"));
    }

    @Test
    @DisplayName("exclude=<ids> skips those challenges (a failed question is not repeated at once)")
    void excludedChallengesAreSkipped() throws Exception {
        long player = TestApi.createPlayer(mvc, om, "skipper");
        long first = TestApi.createChallenge(mvc, om, "First", "LOGIC", "EASY", "a");
        TestApi.createChallenge(mvc, om, "Second", "LOGIC", "EASY", "a");
        TestApi.attempt(mvc, om, player, first, "wrong", 0); // failed, so still "unsolved"

        mvc.perform(get("/api/players/" + player + "/next-challenge")
                        .param("exclude", String.valueOf(first)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.challenge.title").value("Second"));
    }

    @Test
    @DisplayName("When nothing is left to solve the API returns 404")
    void nothingLeftReturns404() throws Exception {
        long player = TestApi.createPlayer(mvc, om, "finisher");
        long only = TestApi.createChallenge(mvc, om, "Only one", "LOGIC", "EASY", "a");
        TestApi.attempt(mvc, om, player, only, "a", 0);

        mvc.perform(get("/api/players/" + player + "/next-challenge"))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/players/999999/next-challenge"))
                .andExpect(status().isNotFound());
    }

    // ---------- progress dashboard ----------

    @Test
    @DisplayName("Progress shows totals, per-category accuracy, strongest and focus categories")
    void progressSummarisesPerformance() throws Exception {
        long player = TestApi.createPlayer(mvc, om, "tracker");
        long logic1 = TestApi.createChallenge(mvc, om, "L1", "LOGIC", "EASY", "a");
        long logic2 = TestApi.createChallenge(mvc, om, "L2", "LOGIC", "EASY", "a");
        long math1 = TestApi.createChallenge(mvc, om, "M1", "MATH", "MEDIUM", "a");
        TestApi.attempt(mvc, om, player, logic1, "a", 0);      // correct, 10 pts
        TestApi.attempt(mvc, om, player, logic2, "a", 0);      // correct, 10 pts
        TestApi.attempt(mvc, om, player, math1, "nope", 0);    // wrong, 0 pts

        mvc.perform(get("/api/players/" + player + "/progress"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("tracker"))
                .andExpect(jsonPath("$.totalAttempts").value(3))
                .andExpect(jsonPath("$.correct").value(2))
                .andExpect(jsonPath("$.incorrect").value(1))
                .andExpect(jsonPath("$.accuracyPercent").value(66.7))
                .andExpect(jsonPath("$.totalPoints").value(20))
                .andExpect(jsonPath("$.strongestCategory").value("LOGIC"))
                .andExpect(jsonPath("$.focusCategory").value("MATH"))
                .andExpect(jsonPath("$.byCategory", hasSize(5)));
    }

    @Test
    @DisplayName("Progress for a player with no attempts is all zeros")
    void emptyProgress() throws Exception {
        long player = TestApi.createPlayer(mvc, om, "fresh");

        mvc.perform(get("/api/players/" + player + "/progress"))
                .andExpect(jsonPath("$.totalAttempts").value(0))
                .andExpect(jsonPath("$.accuracyPercent").value(0.0))
                .andExpect(jsonPath("$.strongestCategory").doesNotExist());
    }

    @Test
    @DisplayName("Attempt history lists newest first")
    void historyIsNewestFirst() throws Exception {
        long player = TestApi.createPlayer(mvc, om, "historian");
        long first = TestApi.createChallenge(mvc, om, "First", "LOGIC", "EASY", "a");
        long second = TestApi.createChallenge(mvc, om, "Second", "LOGIC", "EASY", "a");
        TestApi.attempt(mvc, om, player, first, "a", 0);
        TestApi.attempt(mvc, om, player, second, "a", 0);

        mvc.perform(get("/api/players/" + player + "/attempts"))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].challengeTitle").value("Second"))
                .andExpect(jsonPath("$[1].challengeTitle").value("First"));
    }
}
