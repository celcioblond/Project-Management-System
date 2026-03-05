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
| Frontend | React 18, TypeScript, Tailwind CSS, DaisyUI |
| AI | OpenAI GPT-4o, text-embedding-3-small, DALL-E 3 |
| Auth | JWT (jjwt 0.11.5) |

## Getting Started

### Prerequisites
- Java 21+
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
```bash
cd Project-Management/src/main/resources
cp application.properties.example application.properties
# Fill in your DB credentials, JWT secret, and OpenAI API key
```

**3. Configure Docker**
```bash
cd Project-Management
cp .env.example .env
# Fill in your PostgreSQL credentials (must match application.properties)
```

**4. Start the database**
```bash
docker-compose up -d
```

**5. Run the backend**
```bash
./mvnw spring-boot:run
```

**6. Run the frontend**
```bash
cd frontend/project-management-system
npm install
npm run dev
```

The app will be available at `http://localhost:5173`.
A default admin account is created automatically on first startup using the credentials set in `application.properties`.
