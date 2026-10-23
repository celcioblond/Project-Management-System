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
| Database | PostgreSQL 16 (Docker), pgvector |
| Frontend | React 18, TypeScript, Vite, Tailwind CSS, DaisyUI |
| AI | OpenAI GPT-4o, text-embedding-3-small, DALL-E 3 |
| Auth | JWT (jjwt 0.11.5) |

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
│   └── src/main/resources/      application.properties, prompts, SQL
├── frontend/                    React + Vite SPA
│   ├── src/
│   │   ├── components/          UI components grouped by feature
│   │   ├── context/             Auth context and provider
│   │   ├── hooks/               Custom hooks (useAuth)
│   │   ├── pages/               Route-level views
│   │   ├── services/            Axios client and API calls
│   │   └── types/               API contracts shared across the app
│   └── .env.example
├── docker-compose.yml           PostgreSQL + pgvector (full stack in the Docker step)
└── .env.example                 Backend + Docker configuration
```

## Getting Started

### Prerequisites
- Java 21 (the Lombok version bundled with Spring Boot 3.5 does not compile on newer JDKs)
- Node.js 18+
- Docker Desktop
- OpenAI API key

### Setup

**1. Clone the repo**
```bash
git clone https://github.com/celcioblond/Project-Management-System.git
cd Project-Management-System
```

**2. Configure the backend**

All configuration lives in environment variables. A single `.env` file at the repository root is read by both Docker Compose (database) and Spring Boot (app).
```bash
cp .env.example .env
# Fill in POSTGRES_*, JWT_SECRET (openssl rand -base64 32), OPENAI_API_KEY and ADMIN_PASSWORD
```

**3. Start the database**
```bash
docker compose up -d
```

**4. Run the backend**
```bash
cd backend
./mvnw spring-boot:run
```

**5. Configure and run the frontend**
```bash
cd frontend
cp .env.example .env
npm install
npm run dev
```

The app will be available at `http://localhost:5173`.
On first startup, if no admin exists, one is created with the `ADMIN_*` values from `.env`. The app will not start without `ADMIN_PASSWORD`.

### Environment variables

| Variable | Required | Description |
|----------|----------|-------------|
| `POSTGRES_DB` / `POSTGRES_USER` / `POSTGRES_PASSWORD` | yes | Database credentials |
| `DB_HOST` / `DB_PORT` | no | Defaults to `localhost:5432` |
| `JWT_SECRET` | yes | Base64-encoded 256-bit key used to sign tokens |
| `OPENAI_API_KEY` | yes | OpenAI API key |
| `ADMIN_PASSWORD` | yes | Password for the bootstrap admin |
| `ADMIN_USERNAME` / `ADMIN_EMAIL` / `ADMIN_NAME` | no | Bootstrap admin details |
| `VITE_API_URL` | no | Frontend: backend base URL (public, bundled into the build) |
