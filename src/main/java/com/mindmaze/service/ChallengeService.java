package com.mindmaze.service;

import com.mindmaze.dto.ChallengeRequest;
import com.mindmaze.dto.ChallengeResponse;
import com.mindmaze.dto.HintResponse;
import com.mindmaze.exception.ResourceNotFoundException;
import com.mindmaze.model.Category;
import com.mindmaze.model.Challenge;
import com.mindmaze.model.Difficulty;
import com.mindmaze.repository.AttemptRepository;
import com.mindmaze.repository.ChallengeRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Business logic for managing the challenge bank (CRUD, filtering, hints). */
@Service
public class ChallengeService {

    /** Points deducted from a correct answer for every hint used. */
    public static final int HINT_PENALTY = 5;

    private final ChallengeRepository challengeRepository;
    private final AttemptRepository attemptRepository;

    // Constructor injection: Spring supplies both repositories.
    public ChallengeService(ChallengeRepository challengeRepository, AttemptRepository attemptRepository) {
        this.challengeRepository = challengeRepository;
        this.attemptRepository = attemptRepository;
    }

    @Transactional
    public ChallengeResponse create(ChallengeRequest request) {
        Challenge challenge = new Challenge(request.title(), request.category(), request.difficulty(),
                request.question(), request.answer(), request.hint1(), request.hint2(), request.explanation());
        return ChallengeResponse.from(challengeRepository.save(challenge));
    }

    /** Lists challenges; both filters are optional and can be combined. */
    @Transactional(readOnly = true)
    public List<ChallengeResponse> findAll(Category category, Difficulty difficulty) {
        List<Challenge> challenges;
        if (category != null && difficulty != null) {
            challenges = challengeRepository.findByCategoryAndDifficulty(category, difficulty);
        } else if (category != null) {
            challenges = challengeRepository.findByCategory(category);
        } else if (difficulty != null) {
            challenges = challengeRepository.findByDifficulty(difficulty);
        } else {
            challenges = challengeRepository.findAll();
        }
        return challenges.stream().map(ChallengeResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public ChallengeResponse findById(Long id) {
        return ChallengeResponse.from(getEntity(id));
    }

    @Transactional
    public ChallengeResponse update(Long id, ChallengeRequest request) {
        Challenge challenge = getEntity(id);
        challenge.setTitle(request.title());
        challenge.setCategory(request.category());
        challenge.setDifficulty(request.difficulty());
        challenge.setQuestion(request.question());
        challenge.setAnswer(request.answer());
        challenge.setHint1(request.hint1());
        challenge.setHint2(request.hint2());
        challenge.setExplanation(request.explanation());
        return ChallengeResponse.from(challengeRepository.save(challenge));
    }

    /** Deletes the challenge together with the attempts recorded against it. */
    @Transactional
    public void delete(Long id) {
        Challenge challenge = getEntity(id);
        attemptRepository.deleteByChallengeId(id);
        challengeRepository.delete(challenge);
    }

    /** Returns hint {@code level} (1 = gentle nudge, 2 = stronger clue). */
    @Transactional(readOnly = true)
    public HintResponse getHint(Long id, int level) {
        if (level != 1 && level != 2) {
            throw new IllegalArgumentException("Hint level must be 1 or 2");
        }
        Challenge challenge = getEntity(id);
        String hint = level == 1 ? challenge.getHint1() : challenge.getHint2();
        if (hint == null || hint.isBlank()) {
            throw new ResourceNotFoundException("Challenge " + id + " has no hint at level " + level);
        }
        return new HintResponse(level, hint, HINT_PENALTY);
    }

    /** Package-visible so other services can reuse the lookup and the not-found rule. */
    Challenge getEntity(Long id) {
        return challengeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Challenge", id));
    }
}
