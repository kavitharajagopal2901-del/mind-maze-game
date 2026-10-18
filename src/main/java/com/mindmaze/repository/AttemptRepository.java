package com.mindmaze.repository;

import com.mindmaze.model.Attempt;
import com.mindmaze.model.Category;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AttemptRepository extends JpaRepository<Attempt, Long> {

    /** Full history of a player, newest first. */
    List<Attempt> findByPlayerIdOrderByIdDesc(Long playerId);

    /** The player's last five attempts (all categories) - used by the adaptive engine. */
    List<Attempt> findTop5ByPlayerIdOrderByIdDesc(Long playerId);

    /** The player's last five attempts inside one category. */
    List<Attempt> findTop5ByPlayerIdAndChallengeCategoryOrderByIdDesc(Long playerId, Category category);

    /** Ids of challenges the player has already answered correctly. */
    @Query("select distinct a.challenge.id from Attempt a where a.player.id = :playerId and a.correct = true")
    List<Long> findSolvedChallengeIds(@Param("playerId") Long playerId);

    void deleteByPlayerId(Long playerId);

    void deleteByChallengeId(Long challengeId);
}
