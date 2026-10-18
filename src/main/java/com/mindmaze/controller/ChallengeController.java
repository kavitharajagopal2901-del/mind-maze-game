package com.mindmaze.controller;

import com.mindmaze.dto.ChallengeRequest;
import com.mindmaze.dto.ChallengeResponse;
import com.mindmaze.dto.HintResponse;
import com.mindmaze.model.Category;
import com.mindmaze.model.Difficulty;
import com.mindmaze.service.ChallengeService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** REST endpoints for the challenge bank (full CRUD) plus staged hints. */
@RestController
@RequestMapping("/api/challenges")
public class ChallengeController {

    private final ChallengeService challengeService;

    public ChallengeController(ChallengeService challengeService) {
        this.challengeService = challengeService;
    }

    /** Create: 201 Created with a Location header pointing at the new challenge. */
    @PostMapping
    public ResponseEntity<ChallengeResponse> create(@Valid @RequestBody ChallengeRequest request) {
        ChallengeResponse created = challengeService.create(request);
        return ResponseEntity.created(URI.create("/api/challenges/" + created.id())).body(created);
    }

    /** Read all, optionally filtered: GET /api/challenges?category=LOGIC&amp;difficulty=EASY. */
    @GetMapping
    public List<ChallengeResponse> getAll(@RequestParam(required = false) Category category,
                                          @RequestParam(required = false) Difficulty difficulty) {
        return challengeService.findAll(category, difficulty);
    }

    @GetMapping("/{id}")
    public ChallengeResponse getById(@PathVariable Long id) {
        return challengeService.findById(id);
    }

    @PutMapping("/{id}")
    public ChallengeResponse update(@PathVariable Long id, @Valid @RequestBody ChallengeRequest request) {
        return challengeService.update(id, request);
    }

    /** Delete: 204 No Content. */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        challengeService.delete(id);
        return ResponseEntity.noContent().build();
    }

    /** Staged hints: level 1 is a nudge, level 2 is a stronger clue. Each costs points. */
    @GetMapping("/{id}/hint")
    public HintResponse getHint(@PathVariable Long id, @RequestParam(defaultValue = "1") int level) {
        return challengeService.getHint(id, level);
    }
}
