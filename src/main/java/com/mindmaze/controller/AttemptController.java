package com.mindmaze.controller;

import com.mindmaze.dto.AttemptRequest;
import com.mindmaze.dto.AttemptResponse;
import com.mindmaze.service.AttemptService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Submit an answer and receive the score plus an explanation. */
@RestController
@RequestMapping("/api/attempts")
public class AttemptController {

    private final AttemptService attemptService;

    public AttemptController(AttemptService attemptService) {
        this.attemptService = attemptService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AttemptResponse submit(@Valid @RequestBody AttemptRequest request) {
        return attemptService.submit(request);
    }
}
