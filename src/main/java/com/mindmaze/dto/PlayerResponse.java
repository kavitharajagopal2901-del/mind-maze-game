package com.mindmaze.dto;

import com.mindmaze.model.Player;

public record PlayerResponse(Long id, String username, String email) {

    public static PlayerResponse from(Player p) {
        return new PlayerResponse(p.getId(), p.getUsername(), p.getEmail());
    }
}
