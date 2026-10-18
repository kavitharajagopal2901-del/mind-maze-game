# 🧠 Mind Maze

An adaptive brain-challenge REST API built with **Spring Boot**. Players answer logic, pattern, memory, code-breaking and maths questions; the app scores each answer, explains it, adapts the next question's difficulty to recent performance, and shows a per-category performance map.

## Features

| Feature | What it does |
|---|---|
| Challenge bank (CRUD) | Create, read, update, delete challenges in 5 categories × 3 difficulties |
| Player profiles | Register players (unique username, valid email) and manage them |
| Scoring + hints | EASY 10 / MEDIUM 20 / HARD 30 points; each hint used costs 5 points (minimum 1) |
| Explain My Answer | Every attempt returns the correct answer and the reasoning |
| Adaptive engine | Looks at the last 5 attempts: ≥80% accuracy → harder, ≤40% → easier (needs ≥3 attempts) |
| Progress dashboard | Totals, accuracy, points, and a per-category map with strongest / focus category |

Categories: `LOGIC`, `PATTERN`, `MEMORY`, `CODE_BREAKER`, `MATH`. Difficulties: `EASY`, `MEDIUM`, `HARD`.

## Tech stack

Java 17+ · Spring Boot 3.3.5 (Web, Data JPA, Validation) · H2 database (file-based) · springdoc-openapi (Swagger UI) · JUnit 5 + MockMvc · Maven

## How to build and run

Requirements: JDK 17 or newer, Maven 3.8+ (or use your IDE's Maven support).

```bash
git clone <your-repo-url>
cd mind-maze
mvn spring-boot:run
```

The app starts on **http://localhost:8080** and loads 15 starter challenges on first run.

- Swagger UI: http://localhost:8080/swagger-ui.html
- H2 console: http://localhost:8080/h2-console (JDBC URL `jdbc:h2:file:./data/mindmaze`, user `sa`, empty password)

Run the tests: `mvn test`

### Database configuration

Configured in `src/main/resources/application.properties`. By default data is stored in the `./data/` folder (H2 file database), so it persists between restarts. Delete that folder to reset. To use MySQL instead, replace the datasource URL/driver/credentials and add the MySQL driver to `pom.xml`.

## Sample requests

```bash
# 1. Create a player
curl -X POST localhost:8080/api/players -H "Content-Type: application/json" \
  -d '{"username":"kavitha","email":"kavitha@example.com"}'

# 2. Get the adaptive next challenge (player id 1, optional category)
curl "localhost:8080/api/players/1/next-challenge?category=LOGIC"

# 3. Ask for a hint (costs 5 points)
curl "localhost:8080/api/challenges/1/hint?level=1"

# 4. Submit an answer
curl -X POST localhost:8080/api/attempts -H "Content-Type: application/json" \
  -d '{"playerId":1,"challengeId":1,"answer":"yes","hintsUsed":1}'

# 5. View progress
curl localhost:8080/api/players/1/progress
```

## API summary

Full table with request/response bodies: [docs/API.md](docs/API.md)

| Method | URL | Purpose | Success |
|---|---|---|---|
| POST | `/api/challenges` | Create challenge | 201 |
| GET | `/api/challenges?category=&difficulty=` | List / filter | 200 |
| GET | `/api/challenges/{id}` | Get one | 200 |
| PUT | `/api/challenges/{id}` | Update | 200 |
| DELETE | `/api/challenges/{id}` | Delete | 204 |
| GET | `/api/challenges/{id}/hint?level=1` | Staged hint | 200 |
| POST/GET/PUT/DELETE | `/api/players`, `/api/players/{id}` | Player CRUD | 201/200/200/204 |
| GET | `/api/players/{id}/next-challenge` | Adaptive recommendation | 200 |
| GET | `/api/players/{id}/progress` | Progress dashboard | 200 |
| GET | `/api/players/{id}/attempts` | Attempt history | 200 |
| POST | `/api/attempts` | Submit an answer | 201 |

Errors use one JSON shape (`timestamp`, `status`, `error`, `message`, `details`): 400 validation, 404 not found, 409 duplicate username.

## Project structure

```
com.mindmaze
├── controller   REST endpoints (@RestController)
├── service      business logic (@Service) - scoring, adaptive engine, progress
├── repository   Spring Data JPA interfaces (@Repository)
├── model        JPA entities: Challenge, Player, Attempt + enums
├── dto          request/response records with validation annotations
├── exception    custom exceptions + @ControllerAdvice handler
└── config       Swagger config, starter-data seeder
```

Design report with architecture and ER diagrams: [docs/DESIGN.md](docs/DESIGN.md)

## Spring concepts demonstrated

- **Dependency injection** - constructor injection in every controller and service
- **Stereotype annotations** - `@RestController`, `@Service`, `@Repository`, `@Component`, `@Configuration`
- **Spring Data JPA** - repositories extend `JpaRepository`, derived queries and one `@Query`
- **Bean validation** - `@Valid`, `@NotBlank`, `@Size`, `@Email`, `@Min`/`@Max`
- **Exception handling** - custom `ResourceNotFoundException` / `DuplicateResourceException` handled by `@ControllerAdvice`
- **Transactions** - `@Transactional` on services
