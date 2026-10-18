# Mind Maze - API Documentation

Base URL: `http://localhost:8080`. Interactive version: `/swagger-ui.html`.
All bodies are JSON. Enum values are upper-case: category `LOGIC | PATTERN | MEMORY | CODE_BREAKER | MATH`, difficulty `EASY | MEDIUM | HARD`.

## Challenges

| Method | URL | Request body | Response |
|---|---|---|---|
| POST | `/api/challenges` | `{title, category, difficulty, question, answer, hint1?, hint2?, explanation?}` | `201` + `{id, title, category, difficulty, question, points, hintsAvailable}`, `Location` header |
| GET | `/api/challenges` | - (optional `?category=&difficulty=`) | `200` list of challenge objects |
| GET | `/api/challenges/{id}` | - | `200` challenge, or `404` |
| PUT | `/api/challenges/{id}` | same as POST | `200` updated challenge, `400`, or `404` |
| DELETE | `/api/challenges/{id}` | - | `204`, or `404` (also removes its attempts) |
| GET | `/api/challenges/{id}/hint?level=1` | - | `200` `{level, hint, pointPenalty}`; `400` if level not 1 or 2; `404` if no such hint |

The answer and explanation are never included in challenge responses.

## Players

| Method | URL | Request body | Response |
|---|---|---|---|
| POST | `/api/players` | `{username (3-30 chars), email}` | `201` `{id, username, email}`; `400`; `409` if username taken |
| GET | `/api/players` | - | `200` list |
| GET | `/api/players/{id}` | - | `200` or `404` |
| PUT | `/api/players/{id}` | same as POST | `200`, `400`, `404`, `409` |
| DELETE | `/api/players/{id}` | - | `204` or `404` (also removes their attempts) |
| GET | `/api/players/{id}/next-challenge?category=` | - | `200` `{targetDifficulty, reason, challenge}`; `404` if player unknown or nothing left to solve |
| GET | `/api/players/{id}/progress` | - | `200` progress dashboard (below) |
| GET | `/api/players/{id}/attempts` | - | `200` history, newest first |

### Progress response

```json
{
  "playerId": 1, "username": "kavitha",
  "totalAttempts": 3, "correct": 2, "incorrect": 1,
  "accuracyPercent": 66.7, "totalPoints": 20,
  "strongestCategory": "LOGIC", "focusCategory": "MATH",
  "byCategory": [
    {"category": "LOGIC", "attempted": 2, "correct": 2, "accuracyPercent": 100.0, "points": 20}
  ]
}
```

`byCategory` always lists all 5 categories. `strongestCategory` / `focusCategory` are `null` until the player has attempts.

## Attempts

| Method | URL | Request body | Response |
|---|---|---|---|
| POST | `/api/attempts` | `{playerId, challengeId, answer, hintsUsed?}` (`hintsUsed` 0-2, default 0) | `201` `{attemptId, correct, pointsEarned, hintsUsed, correctAnswer, explanation}`; `400`; `404` if player/challenge unknown |

Answers are compared ignoring case and surrounding spaces.

**Scoring:** correct → `basePoints − 5 × hintsUsed` (minimum 1); wrong → 0. Base points: EASY 10, MEDIUM 20, HARD 30.

## Adaptive rule

Window = player's last 5 attempts (in the requested category if one is given).
- No attempts → `EASY`
- Fewer than 3 attempts → stay at the last difficulty
- Accuracy ≥ 80% → one level harder · accuracy ≤ 40% → one level easier · otherwise stay
- Picks an unsolved challenge at the target difficulty; if none, the closest difficulty available.

## Error format

```json
{
  "timestamp": "2026-10-18T15:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "details": { "title": "title is required" }
}
```
`details` is only filled for validation errors.
