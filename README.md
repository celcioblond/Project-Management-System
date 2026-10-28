# Project Management System

[![CI](https://github.com/celcioblond/Project-Management-System/actions/workflows/ci.yml/badge.svg)](https://github.com/celcioblond/Project-Management-System/actions/workflows/ci.yml)

A full-stack project management application supporting multi-user collaboration with role-based access control, AI-powered features, and real-time project tracking.

## Features

- **Role-based access** — Admin and Employee roles with separate dashboards
- **Project & Task management** — Full CRUD with priority, status, deadlines, and assignees
- **AI description generation** — GPT-4o generates project descriptions from name and type
- **AI diagram generation** — DALL-E 3 generates project architecture diagrams
- **RAG chatbot** — pgvector-powered assistant that answers questions about your real project data
- **JWT authentication** — Stateless auth with secure token handling
- **Comments** — Project and task-level commenting system

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Backend | Java 21, Spring Boot 3.5, Spring Security, Spring AI 1.1 |
| Database | PostgreSQL 16, pgvector, Flyway migrations |
| Frontend | React 18, TypeScript, Vite, Tailwind CSS, DaisyUI |
| AI | OpenAI GPT-4o, text-embedding-3-small, DALL-E 3 |
| Auth | JWT (jjwt 0.12), BCrypt, role-based access control |
| Infra | Docker, Docker Compose, nginx |

## Security Model

- **Authentication** — stateless JWT (HS256, 256-bit secret, 8 h default lifetime). Invalid or expired tokens get `401` and the SPA logs the user out.
- **Authorization** — `ADMIN` manages users, projects, tasks and AI generation. `EMPLOYEE` can only read projects and tasks they are assigned to, comment on them, and edit or delete their own comments. The acting user always comes from the token, never from the request body.
- **RAG isolation** — every vector document is tagged with its project; employee searches are filtered to their projects, so the chatbot cannot leak other teams' data regardless of the prompt.
- **Input validation** — Bean Validation on every request DTO; errors are returned as RFC 7807 problem details without stack traces.
- **Passwords** — BCrypt (cost 12), 8–72 characters; changing your own password requires the current one.
- **Abuse protection** — per-user rate limit on the OpenAI-backed endpoints (`429` + `Retry-After`).
- **Secrets** — only in environment variables / `.env` (gitignored); the app refuses to start if `JWT_SECRET` is missing or shorter than 256 bits, or if `ADMIN_PASSWORD` is missing (and, when the admin is first created, if it is not 8–72 characters).
- **Deployment** — non-root containers with health checks, API not published outside the Docker network, PostgreSQL bound to `127.0.0.1`, nginx security headers (CSP, `X-Frame-Options`, `nosniff`).

## Project Structure

```
.
├── backend/                     Spring Boot REST API
│   ├── src/main/java/com/celcioblond/pms/
│   │   ├── config/              App bootstrapping (admin seed, vector store seed, AI client)
│   │   ├── controller/          REST endpoints
│   │   ├── dto/                 Request / response records with validation
│   │   ├── exception/           Domain exceptions and global error handler
│   │   ├── mapper/              Entity → DTO conversion
│   │   ├── model/               JPA entities
│   │   ├── repository/          Spring Data repositories
│   │   ├── security/            JWT, Spring Security rules, access policy, rate limiting
│   │   └── service/             Business logic, AI and vector store integration
│   ├── src/main/resources/      application.properties, prompts, Flyway migrations
│   ├── src/test/                Unit, web-layer and Testcontainers integration tests
│   └── Dockerfile               Multi-stage build → non-root JRE image
├── frontend/                    React + Vite SPA
│   ├── src/
│   │   ├── components/          UI components grouped by feature
│   │   ├── context/             Auth context and provider
│   │   ├── hooks/               Custom hooks (useAuth)
│   │   ├── pages/               Route-level views
│   │   ├── services/            Axios client and API calls
│   │   └── types/               API contracts shared across the app
│   ├── nginx/                   SPA + /api reverse proxy config and security headers
│   ├── Dockerfile               Multi-stage build → nginx (unprivileged) image
│   └── .env.example
├── .github/                     CI workflow and Dependabot
├── docker-compose.yml           Full stack: db + backend + frontend
└── .env.example                 Backend + Docker configuration
```

## Getting Started

### Prerequisites

- **To run the app:** Docker with Docker Compose v2 (Docker Desktop on macOS/Windows). Nothing else.
- **To develop or run the tests:** also Java 21 (the Lombok version bundled with Spring Boot 3.5
  does not compile on newer JDKs) and Node.js 22 (Vite 7 needs at least 20.19).
- **For the AI features:** an [OpenAI API key](https://platform.openai.com/api-keys). Without a
  valid key everything else works; the chatbot and the AI generate buttons answer
  "The AI service is unavailable".

### 1. Clone and configure

```bash
git clone https://github.com/celcioblond/Project-Management-System.git
cd Project-Management-System
cp .env.example .env
```

A single `.env` at the repository root is read by Docker Compose and by Spring Boot. Open it and
set at least:

| Variable | What to put there |
|----------|-------------------|
| `JWT_SECRET` | A random 256-bit key, e.g. the output of `openssl rand -base64 32` (no OpenSSL? `docker run --rm alpine sh -c "head -c 32 /dev/urandom \| base64"`) |
| `POSTGRES_PASSWORD` | Any password for the database |
| `ADMIN_PASSWORD` | The password of the first admin account (8–72 characters) |
| `OPENAI_API_KEY` | Your OpenAI key, or leave the placeholder to run without AI |

Do this **before the first start**: PostgreSQL stores the `POSTGRES_*` credentials when its data
volume is created and ignores later changes (see Troubleshooting).

### 2a. Run everything with Docker (recommended)

```bash
docker compose up -d --build
```

The first build downloads dependencies and takes a few minutes; later starts take seconds.
The command returns once the database and the API are healthy. Then open
**http://localhost:8081** (or the `APP_PORT` you set).

nginx serves the SPA and proxies `/api` to the backend, so the browser talks to a single origin.
Only the frontend port is published; PostgreSQL is bound to `127.0.0.1` and the API is reachable
only through the proxy.

```bash
docker compose ps                 # all three services should be "healthy"
docker compose logs -f backend    # API logs
docker compose down               # stop (data is kept in the pgvector_data volume)
docker compose down -v            # stop and delete all data
```

### 2b. Run for development

Use three terminals from the repository root:

```bash
docker compose up -d db                     # 1. database only, on 127.0.0.1:5432

cd backend && ./mvnw spring-boot:run        # 2. API on http://localhost:8080 (Windows: mvnw.cmd)

cd frontend && cp .env.example .env         # 3. SPA on http://localhost:5173
npm install && npm run dev
```

On first startup Flyway creates the schema and, if no admin exists, one is created with the
`ADMIN_*` values from `.env`.

### 3. First login and daily use

1. Sign in with `ADMIN_USERNAME` (default `admin`) and `ADMIN_PASSWORD`. Admins land on the
   **admin dashboard**.
2. **Create users** (role `EMPLOYEE` or `ADMIN`) with *Create User*. People can also sign up
   themselves on the *Register* page; self-registered accounts are always `EMPLOYEE`.
3. **Create projects** with *Add Project* and assign employees to them. Name, type, description,
   status and dates are required.
4. **Create tasks** with *Add Task*: pick the project, status, priority, due date and assignees.
5. Employees sign in and see only the projects they belong to: project overview, team, tasks,
   comments, and the AI assistant (bottom-right button) that answers questions about their own
   projects.

The `ADMIN_*` values are only used the first time, to create the bootstrap admin. Changing them
later has no effect; to start over from an empty database run `docker compose down -v`.

### Troubleshooting

| Symptom | Fix |
|---------|-----|
| `docker compose up` ends with `container ... backend ... is unhealthy` | Run `docker compose logs backend` and look for the first error. Usually `JWT_SECRET` still has the placeholder (`JWT_SECRET must be a Base64-encoded key of at least 256 bits`) or `ADMIN_PASSWORD` is not 8–72 characters. Fix `.env` and start again with `docker compose down -v && docker compose up -d` (`-v` also resets the database, see next row). |
| Backend log shows `password authentication failed for user` | `POSTGRES_*` changed after the database volume was created. Run `docker compose down -v && docker compose up -d` (this deletes the stored data). |
| `port is already allocated` / `address already in use` | Another program uses the port. Set a different `APP_PORT` (web app) or `DB_PORT` (database) in `.env`. |
| Login says `Invalid username or password` with the admin you just set in `.env` | The database already had an admin from an earlier run; the new values were ignored. Use the old credentials, or reset with `docker compose down -v`. |
| The chatbot or *AI Generate* says `The AI service is unavailable` | `OPENAI_API_KEY` is missing, invalid or out of quota. Set a valid key and run `docker compose up -d`. |

### Environment variables

| Variable | Required | Description |
|----------|----------|-------------|
| `POSTGRES_DB` / `POSTGRES_USER` / `POSTGRES_PASSWORD` | yes | Database credentials |
| `JWT_SECRET` | yes | Base64-encoded key of at least 256 bits used to sign tokens |
| `ADMIN_PASSWORD` | yes | Password for the bootstrap admin (8–72 characters) |
| `OPENAI_API_KEY` | yes (any value) | OpenAI API key; the AI features need a valid one |
| `ADMIN_USERNAME` / `ADMIN_EMAIL` / `ADMIN_NAME` | no | Bootstrap admin details (defaults `admin`, `admin@example.com`, `System Administrator`) |
| `DB_HOST` / `DB_PORT` | no | Database address for a locally run API (default `localhost:5432`); `DB_PORT` is also the host port Docker publishes PostgreSQL on |
| `APP_PORT` | no | Host port for the Docker web app (default `8081`) |
| `JWT_EXPIRATION` | no | Token lifetime as ISO-8601 duration (default `PT8H`) |
| `CORS_ALLOWED_ORIGINS` | no | Browser origins allowed to call the API directly (default `http://localhost:5173`, the dev server) |
| `AI_RATE_LIMIT_PER_MINUTE` | no | Per-user limit on OpenAI-backed endpoints (default `10`) |
| `OPENAI_CHAT_MODEL` | no | Chat model (default `gpt-4o`) |
| `VITE_API_URL` | no | Frontend dev server only (`frontend/.env`): backend base URL, public and bundled into the build |

## Testing

```bash
cd backend && ./mvnw verify     # unit + web-layer + integration tests (needs Docker for Testcontainers)
cd frontend && npm test         # Vitest + Testing Library
```

- **Backend** — JWT signing and expiry, rate limiting, the access policy and user rules are unit-tested;
  `SecurityRulesWebTest` checks the role rules per endpoint; `ApiIntegrationTest` runs the real
  migrations against PostgreSQL + pgvector in a container and exercises the API end to end
  (OpenAI is mocked).
- **Frontend** — the HTTP client (token handling, 401 logout, error messages), route guards and the
  login flow.

## Continuous Integration

GitHub Actions (`.github/workflows/ci.yml`) runs on every push to `main` and on pull requests:
backend `verify`, frontend lint + tests + build, Docker image builds and a gitleaks secret scan.
Dependabot keeps Maven, npm, Docker and Actions dependencies up to date.
