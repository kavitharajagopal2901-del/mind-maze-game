package com.mindmaze.service;

import com.mindmaze.dto.AttemptHistoryItem;
import com.mindmaze.dto.AttemptRequest;
import com.mindmaze.dto.AttemptResponse;
import com.mindmaze.model.Attempt;
import com.mindmaze.model.Challenge;
import com.mindmaze.model.Player;
import com.mindmaze.repository.AttemptRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Scores submitted answers and keeps each player's history. */
@Service
public class AttemptService {

    private final AttemptRepository attemptRepository;
    private final PlayerService playerService;
    private final ChallengeService challengeService;

    public AttemptService(AttemptRepository attemptRepository, PlayerService playerService,
                          ChallengeService challengeService) {
        this.attemptRepository = attemptRepository;
        this.playerService = playerService;
        this.challengeService = challengeService;
    }

    /**
     * Checks the answer and records the attempt.
     * Scoring: a correct answer earns the difficulty's base points minus
     * {@link ChallengeService#HINT_PENALTY} per hint (never below 1); a wrong answer earns 0.
     */
    @Transactional
    public AttemptResponse submit(AttemptRequest request) {
        Player player = playerService.getEntity(request.playerId());
        Challenge challenge = challengeService.getEntity(request.challengeId());

        int hints = request.hintsUsed() == null ? 0 : request.hintsUsed();
        boolean correct = challenge.isCorrectAnswer(request.answer());
        int points = correct ? calculatePoints(challenge, hints) : 0;

        Attempt saved = attemptRepository.save(new Attempt(player, challenge, correct, hints, points));

        // "Explain My Answer": always return the explanation so players learn from every attempt.
        return new AttemptResponse(saved.getId(), correct, points, hints,
                challenge.getAnswer(), challenge.getExplanation());
    }

    @Transactional(readOnly = true)
    public List<AttemptHistoryItem> history(Long playerId) {
        playerService.getEntity(playerId); // 404 if the player does not exist
        return attemptRepository.findByPlayerIdOrderByIdDesc(playerId).stream()
                .map(AttemptHistoryItem::from)
                .toList();
    }

    private int calculatePoints(Challenge challenge, int hintsUsed) {
        int base = challenge.getDifficulty().getBasePoints();
        return Math.max(1, base - hintsUsed * ChallengeService.HINT_PENALTY);
    }
}
