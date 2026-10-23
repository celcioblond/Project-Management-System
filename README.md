# Project Management System

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

## Project Structure

```
.
├── backend/                     Spring Boot REST API
│   ├── src/main/java/com/celcioblond/pms/
│   │   ├── config/              App bootstrapping (admin seed, vector store seed, AI client)
│   │   ├── controller/          REST endpoints
│   │   ├── dto/                 Request / response records
│   │   ├── mapper/              Entity → DTO conversion
│   │   ├── model/               JPA entities
│   │   ├── repository/          Spring Data repositories
│   │   ├── security/            JWT filter, token service, Spring Security config
│   │   └── service/             Business logic, AI and vector store integration
│   ├── src/main/resources/      application.properties, prompts, Flyway migrations
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
├── docker-compose.yml           Full stack: db + backend + frontend
└── .env.example                 Backend + Docker configuration
```

## Getting Started

### Prerequisites
- Java 21 (the Lombok version bundled with Spring Boot 3.5 does not compile on newer JDKs)
- Node.js 18+
- Docker Desktop
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
