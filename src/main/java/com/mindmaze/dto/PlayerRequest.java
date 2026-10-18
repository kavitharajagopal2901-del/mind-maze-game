package com.mindmaze.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Request body for creating or updating a player profile. */
public record PlayerRequest(
        @NotBlank(message = "username is required")
        @Size(min = 3, max = 30, message = "username must be 3-30 characters")
        String username,

        @NotBlank(message = "email is required")
        @Email(message = "email must be a valid address")
        String email) {
}
