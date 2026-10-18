package com.mindmaze.dto;

/** A staged hint. Using hints reduces the points earned for the question. */
public record HintResponse(int level, String hint, int pointPenalty) {
}
