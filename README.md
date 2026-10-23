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
| Backend | Java 21, Spring Boot 3.5, Spring Security, Spring AI 1.0 |
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
- **Secrets** — only in environment variables / `.env` (gitignored); the app fails fast if `JWT_SECRET` or `ADMIN_PASSWORD` is missing or weak.
- **Deployment** — non-root containers, API not published outside the Docker network, nginx security headers (CSP, `X-Frame-Options`, `nosniff`).

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
- Java 21 (the Lombok version bundled with Spring Boot 3.5 does not compile on newer JDKs)
- Node.js 22 (Vite 7 needs at least 20.19)
- Docker (Docker Desktop on macOS/Windows)
- OpenAI API key

### 1. Clone and configure

```bash
git clone https://github.com/celcioblond/Project-Management-System.git
cd Project-Management-System
cp .env.example .env
# Fill in POSTGRES_*, JWT_SECRET (openssl rand -base64 32), OPENAI_API_KEY and ADMIN_PASSWORD
```

A single `.env` at the repository root is read by Docker Compose and by Spring Boot.

### 2a. Run everything with Docker (recommended)

```bash
docker compose up -d --build
```

Open `http://localhost:8081`. nginx serves the SPA and proxies `/api` to the backend, so the
browser talks to a single origin. Only the frontend port is published; PostgreSQL is bound to
`127.0.0.1` and the API is reachable only through the proxy.

### 2b. Run for development

```bash
docker compose up -d db                  # database only

cd backend && ./mvnw spring-boot:run     # API on http://localhost:8080

cd frontend && cp .env.example .env
npm install && npm run dev               # SPA on http://localhost:5173
```

On first startup Flyway creates the schema and, if no admin exists, one is created with the
`ADMIN_*` values from `.env`. The app refuses to start without `ADMIN_PASSWORD` and `JWT_SECRET`.

### Environment variables

| Variable | Required | Description |
|----------|----------|-------------|
| `POSTGRES_DB` / `POSTGRES_USER` / `POSTGRES_PASSWORD` | yes | Database credentials |
| `DB_HOST` / `DB_PORT` | no | Defaults to `localhost:5432` |
| `JWT_SECRET` | yes | Base64-encoded 256-bit key used to sign tokens |
| `OPENAI_API_KEY` | yes | OpenAI API key |
| `ADMIN_PASSWORD` | yes | Password for the bootstrap admin |
| `ADMIN_USERNAME` / `ADMIN_EMAIL` / `ADMIN_NAME` | no | Bootstrap admin details |
| `JWT_EXPIRATION` | no | Token lifetime as ISO-8601 duration (default `PT8H`) |
| `CORS_ALLOWED_ORIGINS` | no | Browser origins allowed to call the API (default `http://localhost:5173`) |
| `AI_RATE_LIMIT_PER_MINUTE` | no | Per-user limit on OpenAI-backed endpoints (default `10`) |
| `APP_PORT` | no | Host port for the Docker frontend (default `8081`) |
| `VITE_API_URL` | no | Frontend: backend base URL (public, bundled into the build) |

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
