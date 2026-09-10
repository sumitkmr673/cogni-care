# Cogni-Care

### AI-Powered Cognitive Gaming & Memory Assistance Platform

Cogni-Care is an AI-powered cognitive gaming and memory assistance
platform designed to support elderly users, with a focus on the
North Eastern Region (NER) of India.

The platform combines cognitive games, caregiver progress monitoring,
reminders, and a JWT-authenticated backend. The current prototype
includes a caregiver web dashboard and patient gameplay session APIs.

> Built as a solution for Smart India Hackathon (SIH) 2026.

---

## What is implemented

The repository currently includes:

- FastAPI backend with PostgreSQL, SQLAlchemy, and Alembic
- JWT authentication and Argon2 password hashing
- Caregiver dashboard APIs, scoped by `patient_caregivers`
- Patient gameplay APIs for listing games, starting sessions, and submitting results
- Repeatable demo data seeder
- React/Vite caregiver web prototype (Overview and Performance screens)
- Docker Compose development environment

Not yet implemented: production auth extras (OAuth, refresh tokens, password reset),
offline sync, notifications, AI personalization, or a patient-facing game UI.

---

## Project goals

Cogni-Care aims to provide:

- Engaging cognitive games for memory, concentration, and attention
- Personalized difficulty based on user performance
- Caregiver support and progress monitoring
- Reminders for important daily activities
- Multilingual and voice-oriented interaction
- Offline-first functionality for areas with limited connectivity
- Cultural personalization relevant to the North Eastern Region

---

## Cognitive games

The catalog is stored in the `games` table. The demo seeder creates these five games:

| Code | Name | Category |
| --- | --- | --- |
| `DAILY_RECALL` | Daily Recall | Memory |
| `FAMILY_IDENTIFICATION` | Family Identification | Memory |
| `ORIENTATION` | Orientation | Memory |
| `OBJECT_IDENTIFICATION` | Object Identification | Concentration & attention |
| `OBJECT_MATCHING` | Object Matching | Concentration & attention |

---

## Architecture

```text
Cogni-Care
│
├── frontend/          React + Vite caregiver prototype
│                      Overview + Performance screens
│                      Proxies /api to the FastAPI backend
│
├── backend/           FastAPI REST API
│   ├── Authentication (JWT)
│   ├── Caregiver dashboard
│   ├── Patient gameplay sessions
│   ├── Game catalog, sessions, and results
│   └── Demo data seeder
│
└── PostgreSQL         Users, patients, caregivers,
                       games, sessions, results,
                       performance metrics, reminders
```

Identity always comes from the authenticated JWT:

```text
POST /auth/login  →  access token
        ↓
Authorization: Bearer <access_token>
        ↓
Caregiver APIs → current caregiver → patient_caregivers
Patient APIs   → current patient    → own sessions only
```

---

## Technology stack

### Backend

- Python 3.14
- FastAPI
- SQLAlchemy
- Alembic
- Pydantic
- Psycopg
- PyJWT
- pwdlib (Argon2)

### Frontend

- React
- Vite

### Database and infrastructure

- PostgreSQL 18
- Docker
- Docker Compose

---

## Getting started

### Prerequisites

- Docker and Docker Compose
- Node.js (for the caregiver web prototype)
- Git

### Clone and configure

```bash
git clone https://github.com/sumitkmr673/cogni-care.git
cd cogni-care
cp .env.example .env
```

`.env` is excluded from version control. Set a long random `JWT_SECRET_KEY`
before using authentication.

### Start PostgreSQL and the API

```bash
docker compose up -d --build
```

Backend: http://localhost:8000  
API docs: http://localhost:8000/docs  
Database health: http://localhost:8000/health/db

### Apply the schema and seed demo data

On a fresh database, apply the core schema and mark the migration history as current:

```bash
docker compose exec backend alembic upgrade a06f3f5966ec
docker compose exec backend alembic stamp head
docker compose exec backend python -m app.scripts.seed_demo
```

The seeder creates one demo caregiver, one demo patient, the five catalog games,
recent sessions/results, ten daily performance records, and upcoming reminders.
Re-running it replaces only those demo records.

### Start the caregiver web prototype

```bash
cd frontend
cp .env.example .env
npm install
npm run dev
```

Open http://localhost:5173. Vite proxies `/api` to `http://localhost:8000`.
The prototype logs in with the demo caregiver credentials and sends
`Authorization: Bearer <access_token>` on dashboard requests.

---

## Demo credentials

Development only. Do not reuse in production.

```text
demo.caregiver@cogni-care.example / DemoCaregiverOnly-2026!
demo.patient@cogni-care.example   / DemoPatientOnly-2026!
```

---

## Authentication

All protected endpoints expect:

```text
Authorization: Bearer <access_token>
```

| Method | Path | Access |
| --- | --- | --- |
| `POST` | `/auth/login` | Public. Returns `{ access_token, token_type }` |
| `GET` | `/auth/me` | Authenticated user |

Missing, invalid, or expired tokens return HTTP 401.

Roles are `CAREGIVER` and `PATIENT`. Do not send a client-supplied caregiver or
patient UUID as the source of identity.

---

## API reference

### Health

| Method | Path | Notes |
| --- | --- | --- |
| `GET` | `/` | Service check |
| `GET` | `/health/db` | PostgreSQL connectivity |

### Caregiver dashboard

Requires role `CAREGIVER`. Identity is the authenticated user, mapped to a
`caregivers` row. Only patients linked through `patient_caregivers` are visible.
Unrelated patients return HTTP 404. Non-caregivers receive HTTP 403.

| Method | Path |
| --- | --- |
| `GET` | `/patients` |
| `GET` | `/patients/{patient_id}/dashboard` |
| `GET` | `/patients/{patient_id}/performance` |

`demo_caregiver_id` is not used for authorization.

### Patient gameplay

`GET /games` requires any authenticated user. Starting a session and submitting
a result require role `PATIENT`. The patient is taken from the JWT. Another
patient's session returns HTTP 404.

| Method | Path | Body |
| --- | --- | --- |
| `GET` | `/games` | — |
| `POST` | `/games/{game_id}/sessions` | `{ "difficulty_level": 1 }` (1–3, default 1) |
| `POST` | `/games/sessions/{session_id}/result` | `{ "score": 88.5, "accuracy": 92.0, "correct_answers": 11, "total_questions": 12, "response_time_ms": 980, "mistakes": 1 }` |

Completed sessions are stored as `GameSession` (`status=COMPLETED`) plus `GameResult`.
They appear in the caregiver dashboard recent-activity list. Daily
`performance_metrics` rows are not auto-aggregated yet.

---

## Tests

From the repository root, with backend dependencies installed and PostgreSQL
environment variables available:

```bash
python -m unittest discover -s backend/tests -v
```

The suite covers authentication, caregiver authorization, and patient
gameplay sessions.

---

## Project structure

```text
cogni-care/
├── backend/
│   ├── app/
│   │   ├── api/            auth, dashboard, gameplay
│   │   ├── db/             SQLAlchemy engine and session
│   │   ├── migrations/    Alembic revisions
│   │   ├── models/         domain tables
│   │   ├── schemas/        Pydantic request/response models
│   │   ├── scripts/        seed_demo
│   │   ├── security.py     JWT and password hashing
│   │   └── main.py
│   ├── tests/
│   ├── alembic.ini
│   ├── Dockerfile
│   └── pyproject.toml
├── frontend/
│   ├── src/                caregiver prototype
│   ├── package.json
│   └── vite.config.js
├── docker-compose.yml
├── .env.example
└── README.md
```

---

## Privacy and safety

Cogni-Care is a supportive cognitive gaming and memory-assistance platform.

It is **not intended to replace professional medical diagnosis,
clinical assessment, or treatment**.

User and caregiver data should be handled with appropriate privacy and
security practices throughout development.

---

## Project status

**Active development** for SIH 2026.

Working today:

- Dockerized backend and PostgreSQL
- Domain schema and demo seeder
- JWT authentication
- Caregiver dashboard APIs and web prototype
- Patient gameplay session APIs
