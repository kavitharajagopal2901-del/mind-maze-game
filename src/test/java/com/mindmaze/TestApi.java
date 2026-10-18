package com.mindmaze;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/** Small helpers that call the real REST API so tests read like the user story. */
final class TestApi {

    private TestApi() {
    }

    static long createChallenge(MockMvc mvc, ObjectMapper om, String title, String category,
                                String difficulty, String answer) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("title", title);
        body.put("category", category);
        body.put("difficulty", difficulty);
        body.put("question", "Question for " + title);
        body.put("answer", answer);
        body.put("hint1", "first hint");
        body.put("hint2", "second hint");
        body.put("explanation", "Because " + answer);
        MvcResult result = mvc.perform(post("/api/challenges").contentType(APPLICATION_JSON)
                        .content(om.writeValueAsString(body)))
                .andExpect(status().isCreated()).andReturn();
        return om.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    static long createPlayer(MockMvc mvc, ObjectMapper om, String username) throws Exception {
        Map<String, Object> body = Map.of("username", username, "email", username + "@example.com");
        MvcResult result = mvc.perform(post("/api/players").contentType(APPLICATION_JSON)
                        .content(om.writeValueAsString(body)))
                .andExpect(status().isCreated()).andReturn();
        return om.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    static JsonNode attempt(MockMvc mvc, ObjectMapper om, long playerId, long challengeId,
                            String answer, int hintsUsed) throws Exception {
        Map<String, Object> body = Map.of("playerId", playerId, "challengeId", challengeId,
                "answer", answer, "hintsUsed", hintsUsed);
        MvcResult result = mvc.perform(post("/api/attempts").contentType(APPLICATION_JSON)
                        .content(om.writeValueAsString(body)))
                .andExpect(status().isCreated()).andReturn();
        return om.readTree(result.getResponse().getContentAsString());
    }
}
