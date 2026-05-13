# Auraspark 微光智能笔记平台

## Tech stack

- **Java 23**, Spring Boot 3.5.14, Maven multi-module (wrapper at root)
- **MyBatis-Plus 3.5.9** + **PostgreSQL** (PGVector for vector store)
- **Spring AI 1.1.6** — OpenAI-compatible chat + PGVector + JDBC chat memory
- **Lombok 1.18.38**

## Build system

Root `pom.xml` is an **aggregator** only — each module inherits directly from `spring-boot-starter-parent`, NOT the root POM. Cross-module versions are explicit in each `pom.xml`. All 5 sub-modules must be built through the root.

```bash
./mvnw clean install                                              # build everything
./mvnw test -pl auraspark-ai -Dtest="Chat*Test"                  # fast unit tests (no DB)
./mvnw test -pl auraspark-test -am                               # integration tests (needs PostgreSQL)
./mvnw spring-boot:run -pl auraspark-launcher                    # run the app
```

## Modules

```
auraspark (aggregator POM)
 ├── auraspark-common   — shared lib, no DB/AI deps (Lombok only)
 ├── auraspark-core     — business logic: controller/, entity/, mapper/, service/, service/impl/
 ├── auraspark-ai       — AI features: controller/, dto/, domain/, service/, service/impl/; mapper/ is scaffold (empty dir, unused)
 ├── auraspark-launcher — sole deployable aggregator ← ACTUAL ENTRYPOINT
 └── auraspark-test     — integration tests only (needs PostgreSQL; no main sources)
```

Only `auraspark-launcher` should be run. It aggregates core+ai via:
- `@SpringBootApplication(scanBasePackages = "com.auraspark.note")`
- `@MapperScan({"com.auraspark.note.core.mapper", "com.auraspark.note.ai.mapper"})`

Other modules have their own `@SpringBootApplication` — for isolated testing only.

## Configuration

All real config is in `auraspark-launcher/src/main/resources/application.yml`:
- PostgreSQL: `jdbc:postgresql://localhost:5432/auraspark_note`
- OpenAI: configurable `base-url` + `api-key` (OpenAI, Azure, or proxy-compatible)
- MyBatis: `classpath*:mapper/**/*.xml`; type-aliases `com.auraspark.note.core.entity`; `map-underscore-to-camel-case: true`
- Server port: 8080
- Spring AI JDBC chat memory: `initialize-schema: always`

Other modules have `spring.application.name`-only or empty config.

## API endpoints

| Method | Path | Module | Notes |
|--------|------|--------|-------|
| `GET` | `/api/test/hello` | core | Health check |
| `GET` | `/api/test/db` | core | DB connection test (needs PostgreSQL) |
| `POST` | `/api/ai/chat` | ai | Body: `{provider, apiKey, model, message, baseUrl}`. `apiKey` + `message` required. Providers: `deepseek`, `openai`. |
| `GET` | `/api/ai/chat/providers` | ai | Lists available providers |

## Testing quirks

- **`@WebMvcTest(ChatController.class)`** in `auraspark-ai` mocks `ChatService` — no DB needed, runs standalone
- **`@SpringBootTest`** tests consolidated into `auraspark-test` only — needs PostgreSQL running locally
- Lombok `1.18.38` in all modules (JDK 23 compatibility)

## Conventions

- **Constructor injection** (no Lombok `@RequiredArgsConstructor`) — both controllers use explicit constructor DI
- **Service interface + impl** pattern used in both `core` and `ai`
- **No XML mappers** exist yet — all queries use MyBatis-Plus `BaseMapper<T>` or `@Select` annotations. XML support is still pre-configured but unused
- **No lint/format/typecheck** tooling configured (default Maven compiler only)
- **No CI workflows**

## Environment requirements

- PostgreSQL must be running locally for app start or `@SpringBootTest` tests
- `spring-milestones` repo is configured in root POM (needed for Spring AI deps)
