package com.mindmaze.service;

import com.mindmaze.dto.PlayerRequest;
import com.mindmaze.dto.PlayerResponse;
import com.mindmaze.exception.DuplicateResourceException;
import com.mindmaze.exception.ResourceNotFoundException;
import com.mindmaze.model.Player;
import com.mindmaze.repository.AttemptRepository;
import com.mindmaze.repository.PlayerRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Business logic for player profiles. */
@Service
public class PlayerService {

    private final PlayerRepository playerRepository;
    private final AttemptRepository attemptRepository;

    public PlayerService(PlayerRepository playerRepository, AttemptRepository attemptRepository) {
        this.playerRepository = playerRepository;
        this.attemptRepository = attemptRepository;
    }

    @Transactional
    public PlayerResponse create(PlayerRequest request) {
        if (playerRepository.existsByUsername(request.username())) {
            throw new DuplicateResourceException("Username '" + request.username() + "' is already taken");
        }
        return PlayerResponse.from(playerRepository.save(new Player(request.username(), request.email())));
    }

    @Transactional(readOnly = true)
    public List<PlayerResponse> findAll() {
        return playerRepository.findAll().stream().map(PlayerResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public PlayerResponse findById(Long id) {
        return PlayerResponse.from(getEntity(id));
    }

    @Transactional
    public PlayerResponse update(Long id, PlayerRequest request) {
        Player player = getEntity(id);
        // Only a clash with a *different* player's username is an error.
        if (!player.getUsername().equals(request.username())
                && playerRepository.existsByUsername(request.username())) {
            throw new DuplicateResourceException("Username '" + request.username() + "' is already taken");
        }
        player.setUsername(request.username());
        player.setEmail(request.email());
        return PlayerResponse.from(playerRepository.save(player));
    }

    /** Deletes the player together with their attempt history. */
    @Transactional
    public void delete(Long id) {
        Player player = getEntity(id);
        attemptRepository.deleteByPlayerId(id);
        playerRepository.delete(player);
    }

    Player getEntity(Long id) {
        return playerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Player", id));
    }
}
