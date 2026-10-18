# Mind Maze - Design Report

## 1. Problem statement

Practising reasoning skills with a fixed list of puzzles is either too easy or too hard for most people. **Mind Maze** is a backend service that serves brain challenges (logic, patterns, memory, codes, maths), scores answers, explains every solution, and adapts the difficulty to how the player has been doing recently. A progress dashboard shows which categories are strong and which need practice.

## 2. Architecture (layered)

```
   Client (Swagger UI / Postman / curl)
                 │  HTTP + JSON
                 ▼
 ┌───────────────────────────────────────┐
 │ Controller layer  (@RestController)   │  ChallengeController, PlayerController, AttemptController
 │ - maps URLs, validates input (@Valid) │
 └───────────────┬───────────────────────┘
                 ▼
 ┌───────────────────────────────────────┐
 │ Service layer  (@Service)             │  ChallengeService, PlayerService, AttemptService,
 │ - business rules, @Transactional      │  AdaptiveService, ProgressService
 └───────────────┬───────────────────────┘
                 ▼
 ┌───────────────────────────────────────┐
 │ Repository layer (JpaRepository)      │  ChallengeRepository, PlayerRepository, AttemptRepository
 └───────────────┬───────────────────────┘
                 ▼
 ┌───────────────────────────────────────┐
 │ H2 database (file)  - JPA / Hibernate │  tables: challenges, players, attempts
 └───────────────────────────────────────┘

 Cross-cutting: GlobalExceptionHandler (@ControllerAdvice) converts exceptions to JSON errors.
```

**Request flow example** - `POST /api/attempts`: the controller validates the body → `AttemptService.submit` loads the player and challenge through their services, checks the answer, computes points, saves an `Attempt` → returns the result with the explanation. If anything is missing, `ResourceNotFoundException` is thrown and `GlobalExceptionHandler` returns a 404 JSON body.

## 3. ER diagram

```
 ┌───────────────┐       ┌────────────────────┐       ┌────────────────────┐
 │    players    │ 1   * │      attempts      │ *   1 │     challenges     │
 ├───────────────┤───────├────────────────────┤───────├────────────────────┤
 │ id (PK)       │       │ id (PK)            │       │ id (PK)            │
 │ username (UQ) │       │ player_id (FK)     │       │ title              │
 │ email         │       │ challenge_id (FK)  │       │ category           │
 └───────────────┘       │ correct            │       │ difficulty         │
                         │ hints_used         │       │ question           │
                         │ points_earned      │       │ answer             │
                         │ attempted_at       │       │ hint1, hint2       │
                         └────────────────────┘       │ explanation        │
                                                      └────────────────────┘
```

### Entity description

- **Challenge** - one question with its answer, up to two staged hints and an explanation. `category` and `difficulty` are enums stored as strings.
- **Player** - a profile with a unique username and an email.
- **Attempt** - links a player to a challenge and records whether the answer was correct, how many hints were used, the points earned and when. Progress and adaptivity are derived from this table, so nothing is stored twice.

Relationships: a player has many attempts; a challenge has many attempts. Deleting a player or challenge also deletes its attempts.

## 4. Key design decisions

- **DTOs (Java records)** keep entities private and make sure answers are never sent with a question.
- **Rule-based adaptivity** (accuracy over the last 5 attempts) is simple, explainable and easy to test; no machine learning needed.
- **Constructor injection** everywhere, so dependencies are explicit and classes are easy to unit-test.
- **Single error shape** produced in one place (`GlobalExceptionHandler`) so every failure looks the same to API clients.
- **H2 file database** keeps setup to zero while still persisting data; switching to MySQL/PostgreSQL only needs a configuration change.

## 5. Possible extensions

Level-unlock "Challenge Path", Spring Security + JWT login, a front-end page, Docker image.
