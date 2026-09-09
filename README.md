# Cogni-Care

### AI-Powered Cognitive Gaming & Memory Assistance Platform

Cogni-Care is an AI-powered cognitive gaming and memory assistance
platform designed to support elderly users, with a focus on the
North Eastern Region (NER) of India.

The platform combines cognitive games, personalized gameplay,
memory assistance, caregiver support, reminders, and
multilingual/voice-oriented interaction in an offline-first system.

> Built as a solution for Smart India Hackathon (SIH) 2026.

---

## 🎯 Project Goals

Cogni-Care aims to provide:

- 🧠 Engaging cognitive games for memory, concentration, and attention
- 📈 Personalized difficulty based on user performance
- 👨‍👩‍👧 Caregiver support and progress monitoring
- ⏰ Reminders for important daily activities
- 🗣️ Multilingual and voice-oriented interaction
- 🌐 Offline-first functionality for areas with limited connectivity
- 🏔️ Cultural personalization relevant to the North Eastern Region

---

## 🎮 Cognitive Games

### Memory Games

- Daily Recall & Sequence Recall
- Family Identification by Pictures
- Orientation Game

### Concentration & Attention

- Object Identification
- Object Matching

---

## 🏗️ Project Architecture

The project is being developed as a full-stack application.

```text
Cogni-Care
│
├── Frontend
│   └── User & Caregiver Interfaces
│
├── Backend
│   ├── REST API
│   ├── Authentication
│   ├── Patient & Caregiver Management
│   ├── Cognitive Games
│   ├── Game Sessions & Results
│   ├── Performance Analytics
│   ├── Personalization
│   └── Reminders
│
└── Database
    └── PostgreSQL
````

---

## 🛠️ Technology Stack

### Backend

* Python
* FastAPI
* SQLAlchemy
* Alembic
* Pydantic
* Psycopg

### Database

* PostgreSQL

### Infrastructure

* Docker
* Docker Compose

---

## 🚀 Getting Started

### Prerequisites

Make sure you have:

* Docker
* Docker Compose
* Git

### Clone the repository

```bash
git clone <repository-url>
cd cogni-care
```

### Configure environment variables

Create your local environment file:

```bash
cp .env.example .env
```

> `.env` is intentionally excluded from version control.

### Start the development environment

```bash
docker compose up -d
```

### Check the backend

The API will be available at:

```text
http://localhost:8000
```

Interactive API documentation:

```text
http://localhost:8000/docs
```

Database health check:

```text
http://localhost:8000/health/db
```

### Seed caregiver dashboard demo data

After the Compose services are running, seed the repeatable dashboard dataset:

```bash
docker compose exec backend python -m app.scripts.seed_demo
```

The seeder creates one clearly labelled demo caregiver and patient, all five
catalog games, recent sessions/results, ten daily performance records, and
upcoming reminders. Re-running the command replaces only those demo caregiver
and patient records; it does not create duplicates or modify the schema.

For local authentication testing only, the seeded demo credentials are:

```text
demo.caregiver@cogni-care.example / DemoCaregiverOnly-2026!
demo.patient@cogni-care.example   / DemoPatientOnly-2026!
```

These are development-only credentials and must not be reused in production.

Use the printed caregiver UUID with the prototype dashboard endpoints:

```text
GET /patients?demo_caregiver_id=<caregiver-uuid>
GET /patients/<patient-uuid>/dashboard?demo_caregiver_id=<caregiver-uuid>
GET /patients/<patient-uuid>/performance?demo_caregiver_id=<caregiver-uuid>
```

---

## 📁 Project Structure

```text
cogni-care/
│
├── backend/
│   ├── app/
│   │   ├── db/
│   │   ├── migrations/
│   │   ├── main.py
│   │   └── __init__.py
│   │
│   ├── alembic.ini
│   ├── Dockerfile
│   └── pyproject.toml
│
├── .env.example
├── .gitignore
├── docker-compose.yml
├── LICENSE
└── README.md
```

---

## 🔒 Privacy & Safety

Cogni-Care is intended as a supportive cognitive gaming and
memory-assistance platform.

It is **not intended to replace professional medical diagnosis,
clinical assessment, or treatment**.

User and caregiver data should be handled with appropriate
privacy and security practices throughout development.

---

## 📌 Project Status

🚧 **Active Development**

Current development includes:

* Backend foundation
* Dockerized development environment
* PostgreSQL database
* SQLAlchemy integration
* Alembic migration system

More functionality will be added incrementally as development
progresses.
