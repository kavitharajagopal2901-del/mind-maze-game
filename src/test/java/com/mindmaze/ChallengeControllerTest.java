package com.mindmaze;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

/** End-to-end tests of the challenge CRUD API, validation and error responses. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ChallengeControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper om;

    @Test
    @DisplayName("POST creates a challenge: 201, Location header, answer not leaked")
    void createReturns201AndHidesAnswer() throws Exception {
        Map<String, Object> body = Map.of("title", "Doubles", "category", "PATTERN", "difficulty", "EASY",
                "question", "1, 2, 4, ?", "answer", "8");

        mvc.perform(post("/api/challenges").contentType(APPLICATION_JSON).content(om.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.points").value(10))
                .andExpect(jsonPath("$.answer").doesNotExist());
    }

    @Test
    @DisplayName("POST with invalid data returns 400 listing every bad field")
    void createWithInvalidDataReturns400() throws Exception {
        Map<String, Object> body = Map.of("title", " ", "question", "", "answer", "x");

        mvc.perform(post("/api/challenges").contentType(APPLICATION_JSON).content(om.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.details.title").exists())
                .andExpect(jsonPath("$.details.category").exists())
                .andExpect(jsonPath("$.details.difficulty").exists())
                .andExpect(jsonPath("$.details.question").exists());
    }

    @Test
    @DisplayName("POST with an unknown category value returns 400, not 500")
    void createWithUnknownEnumReturns400() throws Exception {
        String json = "{\"title\":\"t\",\"category\":\"BANANA\",\"difficulty\":\"EASY\","
                + "\"question\":\"q\",\"answer\":\"a\"}";

        mvc.perform(post("/api/challenges").contentType(APPLICATION_JSON).content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET unknown id returns 404 with a clear message")
    void getUnknownReturns404() throws Exception {
        mvc.perform(get("/api/challenges/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value(containsString("Challenge with id 999999")));
    }

    @Test
    @DisplayName("GET by id returns the stored challenge")
    void getByIdReturnsChallenge() throws Exception {
        long id = TestApi.createChallenge(mvc, om, "Find me", "LOGIC", "MEDIUM", "yes");

        mvc.perform(get("/api/challenges/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Find me"))
                .andExpect(jsonPath("$.category").value("LOGIC"))
                .andExpect(jsonPath("$.points").value(20))
                .andExpect(jsonPath("$.hintsAvailable").value(2));
    }

    @Test
    @DisplayName("GET list can be filtered by category and difficulty")
    void listCanBeFiltered() throws Exception {
        TestApi.createChallenge(mvc, om, "L-easy", "LOGIC", "EASY", "a");
        TestApi.createChallenge(mvc, om, "L-hard", "LOGIC", "HARD", "a");
        TestApi.createChallenge(mvc, om, "M-easy", "MATH", "EASY", "a");

        mvc.perform(get("/api/challenges").param("category", "LOGIC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
        mvc.perform(get("/api/challenges").param("category", "LOGIC").param("difficulty", "HARD"))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title").value("L-hard"));
        mvc.perform(get("/api/challenges").param("category", "BANANA"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT replaces the challenge's fields")
    void updateChangesChallenge() throws Exception {
        long id = TestApi.createChallenge(mvc, om, "Old title", "MATH", "EASY", "1");
        Map<String, Object> body = Map.of("title", "New title", "category", "MATH", "difficulty", "HARD",
                "question", "New question", "answer", "2");

        mvc.perform(put("/api/challenges/" + id).contentType(APPLICATION_JSON).content(om.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("New title"))
                .andExpect(jsonPath("$.difficulty").value("HARD"))
                .andExpect(jsonPath("$.points").value(30));
    }

    @Test
    @DisplayName("PUT on an unknown id returns 404")
    void updateUnknownReturns404() throws Exception {
        Map<String, Object> body = Map.of("title", "T", "category", "MATH", "difficulty", "EASY",
                "question", "Q", "answer", "A");

        mvc.perform(put("/api/challenges/999999").contentType(APPLICATION_JSON).content(om.writeValueAsString(body)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE returns 204 and the challenge is gone afterwards")
    void deleteRemovesChallenge() throws Exception {
        long id = TestApi.createChallenge(mvc, om, "Temporary", "LOGIC", "EASY", "x");

        mvc.perform(delete("/api/challenges/" + id)).andExpect(status().isNoContent());
        mvc.perform(get("/api/challenges/" + id)).andExpect(status().isNotFound());
        mvc.perform(delete("/api/challenges/" + id)).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Hints: levels 1 and 2 work, level 3 is rejected")
    void hintsAreStaged() throws Exception {
        long id = TestApi.createChallenge(mvc, om, "Hinted", "LOGIC", "EASY", "x");

        mvc.perform(get("/api/challenges/" + id + "/hint"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.level").value(1))
                .andExpect(jsonPath("$.hint").value("first hint"))
                .andExpect(jsonPath("$.pointPenalty").value(5));
        mvc.perform(get("/api/challenges/" + id + "/hint").param("level", "2"))
                .andExpect(jsonPath("$.hint").value("second hint"));
        mvc.perform(get("/api/challenges/" + id + "/hint").param("level", "3"))
                .andExpect(status().isBadRequest());
    }
}
