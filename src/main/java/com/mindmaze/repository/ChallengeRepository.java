package com.mindmaze.repository;

import com.mindmaze.model.Category;
import com.mindmaze.model.Challenge;
import com.mindmaze.model.Difficulty;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChallengeRepository extends JpaRepository<Challenge, Long> {

    List<Challenge> findByCategory(Category category);

    List<Challenge> findByDifficulty(Difficulty difficulty);

    List<Challenge> findByCategoryAndDifficulty(Category category, Difficulty difficulty);
}
