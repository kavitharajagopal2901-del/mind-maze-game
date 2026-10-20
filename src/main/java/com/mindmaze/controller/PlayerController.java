package com.mindmaze.controller;

import com.mindmaze.dto.AttemptHistoryItem;
import com.mindmaze.dto.NextChallengeResponse;
import com.mindmaze.dto.PlayerRequest;
import com.mindmaze.dto.PlayerResponse;
import com.mindmaze.dto.ProgressResponse;
import com.mindmaze.model.Category;
import com.mindmaze.service.AdaptiveService;
import com.mindmaze.service.AttemptService;
import com.mindmaze.service.PlayerService;
import com.mindmaze.service.ProgressService;
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

/** Player profiles (CRUD) plus each player's adaptive next challenge, progress and history. */
@RestController
@RequestMapping("/api/players")
public class PlayerController {

    private final PlayerService playerService;
    private final AdaptiveService adaptiveService;
    private final ProgressService progressService;
    private final AttemptService attemptService;

    public PlayerController(PlayerService playerService, AdaptiveService adaptiveService,
                            ProgressService progressService, AttemptService attemptService) {
        this.playerService = playerService;
        this.adaptiveService = adaptiveService;
        this.progressService = progressService;
        this.attemptService = attemptService;
    }

    @PostMapping
    public ResponseEntity<PlayerResponse> create(@Valid @RequestBody PlayerRequest request) {
        PlayerResponse created = playerService.create(request);
        return ResponseEntity.created(URI.create("/api/players/" + created.id())).body(created);
    }

    @GetMapping
    public List<PlayerResponse> getAll() {
        return playerService.findAll();
    }

    @GetMapping("/{id}")
    public PlayerResponse getById(@PathVariable Long id) {
        return playerService.findById(id);
    }

    @PutMapping("/{id}")
    public PlayerResponse update(@PathVariable Long id, @Valid @RequestBody PlayerRequest request) {
        return playerService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        playerService.delete(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Adaptive Challenge Engine: the best next question for this player.
     * Optional {@code exclude=1,2,3} skips those challenge ids (used by the game screen).
     */
    @GetMapping("/{id}/next-challenge")
    public NextChallengeResponse nextChallenge(@PathVariable Long id,
                                               @RequestParam(required = false) Category category,
                                               @RequestParam(required = false) List<Long> exclude) {
        return adaptiveService.nextChallenge(id, category, exclude);
    }

    /** Progress dashboard / Personal Performance Map. */
    @GetMapping("/{id}/progress")
    public ProgressResponse progress(@PathVariable Long id) {
        return progressService.progressFor(id);
    }

    /** Attempt history, newest first. */
    @GetMapping("/{id}/attempts")
    public List<AttemptHistoryItem> attempts(@PathVariable Long id) {
        return attemptService.history(id);
    }
}
