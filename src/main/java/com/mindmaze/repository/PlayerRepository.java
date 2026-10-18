package com.mindmaze.repository;

import com.mindmaze.model.Player;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PlayerRepository extends JpaRepository<Player, Long> {

    boolean existsByUsername(String username);
}
