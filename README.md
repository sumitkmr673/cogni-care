# Cogni-Care

### AI-Assisted Cognitive Gaming & Memory-Support Platform

Cogni-Care is an AI-assisted cognitive gaming and memory-support platform designed for older adults and the family members and caregivers supporting them, with special attention to linguistic and cultural relevance in the North Eastern Region (NER) of India.

The platform links an accessible, offline-first Android application for elderly patients with a secure, web-based portal for caregivers. Caregivers can monitor objective gameplay engagement, track performance trends, review explainable rule-based observations, coordinate care teams, and manage daily reminders.

> Developed as a comprehensive solution for Smart India Hackathon (SIH) 2026.

---

## Important Non-Clinical Boundary

> [!IMPORTANT]
> **Cogni-Care is NOT a dementia diagnosis system.**
>
> The platform does not provide medical assessments, clinical risk scores, or diagnostic evaluations. Its analytical capabilities are designed strictly for:
> - Objective gameplay performance analysis and engagement tracking.
> - Explainable, rule-based trend observations for caregivers and families.
> - Cognitive stimulation, habit support, and future adaptive gameplay personalization.
>
> Cogni-Care supports everyday cognitive health and caregiver awareness; it does not replace professional medical advice, clinical diagnosis, or neurological care.

---

## Two Connected Experiences

```
┌─────────────────────────────────────────────────────────────┐
│                    Cogni-Care Ecosystem                     │
└─────────────────────────────────────────────────────────────┘
          │                                         │
          ▼                                         ▼
┌───────────────────────────┐             ┌───────────────────────────┐
│     PATIENT (Android)     │             │     CAREGIVER (Web)       │
├───────────────────────────┤             ├───────────────────────────┤
│ • 7 Accessible Games      │             │ • Patient Overview        │
│ • Offline-First Gameplay  │             │ • Performance Trends      │
│ • Local Result Buffer     │             │ • Rule-Based Observations │
│ • Voice & Touch Support   │             │ • Gameplay Activity Log   │
│ • Multilingual Interface  │             │ • Schedule & Reminders    │
│ • Daily Reminders View    │             │ • Care Team Management    │
└───────────────────────────┘             └───────────────────────────┘
          │                                         │
          │ HTTPS / Sync                            │ HTTPS
          ▼                                         ▼
┌─────────────────────────────────────────────────────────────┐
│                 FastAPI REST API Backend                    │
│      JWT Authentication · Scoped Authorization · RBAC       │
└─────────────────────────────────────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────┐
│                     PostgreSQL Database                     │
│         Relational Storage · Indexed Scopes · Audit         │
└─────────────────────────────────────────────────────────────┘
```

### 1. Patient Experience (Android)
- **Accessible Design**: High-contrast, large tap targets, unhurried interactions, and clear typography tailored for older adults.
- **7 Cognitive Games**: Covers memory, concentration, orientation, and recall.
- **Offline-First Play**: Games run locally on-device. Results are buffered locally and synchronized to the backend when connectivity returns.
- **Voice-Assisted Interaction**: Constrained speech recognition and spoken prompts for hands-free and low-dexterity gameplay.
- **Patient Registration & Public ID**: Simple onboarding generating a human-readable Public ID (`PT-XXXXXX`) for easy caregiver linking without exposing internal identifiers.

### 2. Caregiver Experience (Web Portal)
- **Patient Overview**: Immediate status for assigned patients, including recent gameplay, accuracy metrics, and last-sync status.
- **Performance Analysis**: Daily performance metric tracking, accuracy trends, deliberation pace, and explainable rule-based observations.
- **Activity Log**: Chronological gameplay session logs with granular telemetry (response times, mistakes, scores).
- **Schedule & Reminders**: Creator-aware reminder creation, schedule management, and active status toggling.
- **Care Team Coordination**: Multi-caregiver support with explicit primary/secondary roles, allowing families and specialists to collaborate securely.

---

## System Architecture

```text
Patient Android App (Kotlin / Jetpack Compose)
       │
       │ HTTPS / REST / Token Auth
       ├──────────────────────────────────────────────┐
       │                                              │
       ▼                                              ▼
FastAPI Main Backend (Port 8000)             Voice Service (Port 8100)
  ├── Authentication & Scoped RBAC             ├── Dedicated GPU Microservice
  ├── Caregiver Dashboard Endpoints            ├── Whisper ASR (Audio to Text)
  ├── Patient Gameplay & Sync Endpoints        ├── Qwen LLM (Choice Resolution)
  ├── Daily Metric Aggregation Service         └── Stateless JWT Token Auth
  ├── Rule-Based Analysis Engine               (No direct DB access)
  └── Care Team & Reminder Logic
       │
       │ SQLAlchemy 2.0 / psycopg3
       ▼
PostgreSQL Relational Database
  ├── users & patients (PT-XXXXXX)
  ├── caregivers (CG-XXXXXX) & patient_caregivers
  ├── games, game_sessions & game_results
  ├── performance_metrics (daily aggregated)
  └── reminders
       ▲
       │ HTTPS / REST / Bearer Token
       │
Caregiver Web Application (React 19 / Vite 8)
  ├── Landing Page (Interactive storytelling & product narrative)
  ├── Unified Caregiver Authentication (Login & Two-Step Registration)
  └── Caregiver Dashboard (Overview, Performance, Activity, Reminders, Care Team)
```

The **FastAPI backend** acts as the strict authorization gatekeeper between client applications and the **PostgreSQL database**. Identity is derived solely from verified JSON Web Tokens (JWT).

The **Voice Service** is decoupled as an independent microservice for machine learning inference. It verifies the client's JWT using the shared signature key, performing speech transcription and constrained option interpretation without requiring direct database access.

---

## Current Technology Stack

| Layer | Technologies | Version / Details |
| --- | --- | --- |
| **Backend** | Python, FastAPI, Pydantic | Python `>=3.14,<3.15`, FastAPI `0.141.1`, Pydantic `2.13.5` |
| **Database ORM** | SQLAlchemy, Psycopg, Alembic | SQLAlchemy `2.0.52`, Psycopg `3.3.5`, Alembic `1.19.2` |
| **Auth & Security** | PyJWT, pwdlib (Argon2) | PyJWT `2.14.0`, pwdlib[argon2] `0.3.0` |
| **ASGI Server** | Uvicorn | Uvicorn `0.52.4` |
| **Frontend** | React, Vite, React Router | React `19`, Vite `8.2.2`, React Router `7.18.3` |
| **Frontend Styling** | Modern Vanilla CSS | Responsive layouts, CSS tokens, Google Fonts (DM Sans, Manrope, Caveat) |
| **Android Client** | Kotlin, Jetpack Compose | Kotlin `2.3.20`, Android Gradle Plugin `8.13.2`, Compose BOM `2026.06.01` |
| **Android Architecture**| Hilt, DataStore, Security Crypto | Hilt `2.58`, Navigation Compose `2.9.8`, DataStore `1.2.1`, Security Crypto `1.1.0-alpha06` |
| **Android Networking** | Retrofit, OkHttp, Kotlinx Serialization | Retrofit `2.11.0`, OkHttp `4.12.0`, Kotlinx Serialization `1.11.0` |
| **Voice Service** | FastAPI, faster-whisper, llama-cpp-python | FastAPI `>=0.115`, faster-whisper `>=1.1`, llama-cpp-python `>=0.3.4`, CUDA 12 |
| **Database** | PostgreSQL | PostgreSQL `18.6` |
| **Containerization** | Docker, Docker Compose | Compose file version 3+ for local backend & database services |

---

## Authentication & Role Model

### Global Roles
The database strictly enforces two global user roles via check constraint `role IN ('PATIENT', 'CAREGIVER')`:
- **`PATIENT`**: End users playing cognitive games and viewing daily schedules.
- **`CAREGIVER`**: Family members, professional caregivers, and healthcare professionals monitoring patients.

> **Note on Doctor Role**: In Cogni-Care, a doctor is **not** a separate global user role. Healthcare professionals register as caregivers with a specialized subtype: `caregiver_type = 'DOCTOR'` (supported subtypes: `FAMILY`, `DOCTOR`, `PROFESSIONAL_CAREGIVER`, `OTHER`).

### Scoped Relationship Model
- **Multiple Caregivers per Patient**: A patient can be supported by multiple caregivers forming a collaborative care circle.
- **Exactly One Primary Caregiver**: Enforced via PostgreSQL partial unique index `ix_patient_caregivers_primary_patient` where `is_primary = TRUE`.
- **Primary Caregiver Permissions**:
  - Full access to patient dashboard, metrics, sessions, and reminders.
  - Can invite and link secondary caregivers using Caregiver Public IDs.
  - Can transfer primary status to another assigned caregiver.
  - Can remove secondary caregivers from the care circle.
  - Cannot be removed from the care team without first transferring primary status.
- **Secondary Caregiver Permissions**:
  - Read access to assigned patient's overview, performance history, and activity logs.
  - Can create reminders and edit/delete reminders they created.
  - Restricted from care team administration (cannot add or remove caregivers).
- **Public ID Addressing**:
  - Patients are assigned a Public ID: `PT-XXXXXX` (e.g., `PT-928410`).
  - Caregivers are assigned a Public ID: `CG-XXXXXX` (e.g., `CG-847291`).
  - Allows cross-account linking without revealing database UUIDs or personal email addresses.

---

## Cognitive Games Suite

The platform includes seven cognitive games designed for accessibility, concentration, and memory stimulation:

| Game | Category | Gameplay Description | Telemetry Captured |
| --- | --- | --- | --- |
| **Memory Match** | Memory & Association | Unhurried visual card-matching grid where patients uncover pairs of familiar symbols and items. | Accuracy, completed pairs, deliberation time, mistakes |
| **Pattern Recall** | Working Memory | Sequenced pattern reproduction prompting patients to recall flashing tile sequences. | Accuracy, span length, reaction speed, trial errors |
| **Daily Recall** | Episodic Memory | Questions referencing routine events, meals, and familiar daily occurrences. | Score, correct answers, response time |
| **Voice Quiz** | Cognitive Language | Voice-interactive question sessions where patients speak answers aloud or tap choices. | Speech recognition accuracy, response latency, choice accuracy |
| **Orientation** | Temporal Orientation | Gentle questions regarding current day, time of day, season, and immediate surroundings. | Temporal accuracy, deliberation pace |
| **Object Naming** | Semantic Memory | Picture-based object identification presenting everyday domestic objects for naming. | Identification accuracy, synonym recognition |
| **Family Identification** | Social Recognition | Image-based recognition of family members and loved ones to reinforce familiar social connections. | Association accuracy, hesitation time |

---

## Gameplay Telemetry & Aggregation

### Session Telemetry
When a patient plays a game, the client tracks non-clinical interaction events:
- **Accuracy**: Percentage of correct choices relative to total interactions.
- **Response Latency**: Average response time per interaction in milliseconds (`response_time_ms`).
- **Mistakes & Errors**: Number of incorrect attempts before resolution.
- **Completion Timestamps**: Start and completion timestamps recording session duration.

### Daily Aggregation Service
Upon session submission via `POST /games/sessions/{session_id}/result`, the backend triggers `record_daily_performance_metric`:
1. Resolves the patient's local timezone (defaults to `Asia/Kolkata` if unspecified).
2. Calculates the patient's local calendar day boundaries in UTC.
3. Automatically computes or updates the day's record in `performance_metrics`:
   - Daily average accuracy
   - Memory score index (derived from memory-category games)
   - Attention score index (derived from concentration-category games)
   - Average deliberation pace (response time in ms)
   - Total games completed today

---

## Rule-Based Performance Analysis Engine

Cogni-Care implements a **deterministic, explainable rule-based analysis engine** (`backend/app/services/analysis.py`) accessible via `GET /patients/{patient_id}/analysis`.

### Methodology
1. **Data Sufficiency Check**: Requires a minimum of **3 active gameplay days** (`MIN_ACTIVE_DAYS_FOR_ANALYSIS = 3`). If insufficient, returns an explanatory status rather than speculative metrics.
2. **Window Segmentation**:
   - **Recent Window**: The latest active days (up to 3 days).
   - **Baseline Window**: Prior active history (up to 10 days) establishing the patient's typical performance profile.
3. **Deterministic Heuristic Evaluation**:
   - **Accuracy**: Significant if delta exceeds `±5.0%` points (`IMPROVING` vs `LOWER` vs `STABLE`).
   - **Memory Score**: Significant if delta exceeds `±5.0%` points.
   - **Attention Score**: Significant if delta exceeds `±5.0%` points.
   - **Response Time Pace**: Significant if deliberation pace changes by `-400ms` (faster) or `+500ms` (slower).
   - **Engagement & Consistency**: Evaluates session frequency, variance, and routine stability.

```
Gameplay Sessions
       │
       ▼
Daily Metrics Aggregation (Accuracy · Response Time · Category Indices)
       │
       ▼
Deterministic Heuristic Comparison (Recent 3-Day Window vs. Prior Baseline)
       │
       ▼
Structured Performance Observations (Non-clinical caregiver insights)
```

> **Current vs. Future Evolution**:
> - **CURRENT**: Deterministic rule-based observations comparing objective gameplay telemetry against a rolling personal baseline.
> - **FUTURE / ROADMAP**: Machine-learning personalization and dynamic game difficulty scaling based on longitudinal gameplay patterns.

---

## Reminders System

The platform includes a creator-aware scheduling and reminder system:
- **Caregiver Creation**: Caregivers can schedule medication prompts, game sessions, hydration cues, and appointments with customizable recurrence.
- **Permission Matrix**:
  - **Primary Caregivers**: Can create, update, deactivate, and delete any reminder for their assigned patient.
  - **Secondary Caregivers**: Can create reminders, but may only update or delete reminders they personally created.
  - **Patients**: Receive read-only access to their scheduled reminders.
- **Status Lifecycle**: Reminders can be activated or paused via dedicated status endpoints (`PATCH /patients/{patient_id}/reminders/{reminder_id}/status`).

---

## Caregiver Web Portal Structure

The caregiver web application (`frontend/`) provides a clean, modern interface organized into dedicated workspaces:

1. **Public Product Landing Page (`/`)**:
   - Editorial product storytelling explaining patient and caregiver workflows.
   - Interactive smooth-scroll anchor navigation (`How it works`, `For patients`, `For caregivers`).
   - Magazine-style featured games showcase (Memory Match spotlight).
   - Visual breakdown of the offline activity buffer and data synchronization flow.
2. **Unified Authentication Pages (`/login`, `/register`)**:
   - Shares the exact landing page navigation bar with a compact `Back to home` action.
   - Single brand lockup (zero redundant interior logos).
   - Balanced two-column editorial layout featuring domestic care vector artwork (`ConnectedCareVisual`).
   - **Caregiver Registration**: Compact two-step flow (`Step 1: Account Details` $\to$ `Step 2: Caregiver Role & Details`) with Public ID generation.
3. **Authenticated Patient Portal (`/app`)**:
   - **Patient Switcher**: Toggle between assigned patients or link new patients via Public ID (`PT-XXXXXX`).
   - **Overview**: High-level summary of today's activities, window accuracy, deliberation pace, and care circle members.
   - **Performance**: Interactive 7/14/30-day performance trends, memory vs. attention charts, and rule-based observations.
   - **Activity**: Granular session history with status pills, scores, and duration filtering.
   - **Reminders**: Schedule view with creator-aware edit controls and status toggling.
   - **Care Team**: Circle management displaying member designations, primary badge, and transfer/removal actions.

---

## Multilingual Support

Cogni-Care is designed with a strong focus on linguistic accessibility in the North Eastern Region of India. Language availability is explicitly structured in the codebase:

### Available Now
| Language | Code | Script | Status |
| --- | --- | --- | --- |
| **English** | `en` | Latin | Fully translated UI, voice prompts, and cognitive content |
| **Hindi** | `hi` | Devanagari | Fully translated UI, voice prompts, and cognitive content |
| **Bengali** | `bn` | Bengali | Fully translated UI, voice prompts, and cognitive content |

### Roadmap / In Development
| Language | Code | Region | Implementation Status |
| --- | --- | --- | --- |
| **Assamese** | `as` | Assam | UI translation in development, ASR auto-detection |
| **Manipuri** | `mni` | Manipur | Script localization in progress |
| **Nepali** | `ne` | Sikkim / Gorkha | Locale structure established |
| **Khasi** | `kha` | Meghalaya | Phonetic and vocabulary modeling in development |
| **Mizo** | `lus` | Mizoram | Vocabulary modeling in development |

---

## Offline-First Architecture

Because rural and hilly regions across Northeast India often experience intermittent connectivity, Cogni-Care adopts an offline-first strategy for patient gameplay:

```text
[Patient Plays Game on Device]
             │
             ├── If Online  ──► Instant Sync ──► [PostgreSQL Backend]
             │
             └── If Offline ──► Cached in Local Storage (SyncStatus = PENDING)
                                     │
                                     ▼ (Connectivity Returns)
                               Background Sync Worker
                                     │
                                     ▼
                               [PostgreSQL Backend Updates Caregiver Dashboard]
```

1. **Local Execution**: Cognitive game engines run entirely on the client device without requiring round-trip API latency.
2. **Local Result Buffering**: Completed session metrics are stored in encrypted local storage (`DataStore` / `EncryptedSharedPreferences`) with status `PENDING`.
3. **Automatic Synchronization**: When the Android client detects network connectivity, pending session payloads are dispatched to `POST /games/sessions/{session_id}/result`.
4. **Backend Reconciliation**: The backend processes buffered timestamps, aggregates daily metrics, and updates caregiver visibility.

---

## Voice Service Architecture

The voice assistant is hosted as a dedicated microservice (`voice_service/`):
- **Port**: `8100` (by default).
- **Core Models**:
  - **Speech-to-Text**: `faster-whisper` (Whisper model) for robust multilingual transcription.
  - **Option Interpretation**: `llama-cpp-python` (Qwen3-8B GGUF) for mapping spoken utterances to valid on-screen choices.
- **Decoupled Security**: Does not connect to the database. Authenticates incoming requests using the shared backend `JWT_SECRET_KEY`.

### Voice Endpoints
- `POST /app/voice/interpret`: Accepts multipart audio (`.m4a`, `.wav`, `.ogg`), transcribes speech, and resolves the option index.
- `POST /app/voice/interpret-text`: Accepts pre-transcribed text (e.g., from on-device speech recognizer) and resolves option choices directly.

---

## Project Directory Structure

```text
cogni-care/
├── app/                        # Android Patient Application (Kotlin / Jetpack Compose)
│   ├── src/main/java/com/example/cognicare/
│   │   ├── core/               # Locale provider, voice intents, constants
│   │   ├── data/               # DataStore preferences, security crypto, DTOs
│   │   ├── repository/         # RemoteCareRepository, sync management
│   │   ├── ui/                 # Composables, screens (patient, games, caregiver), theme
│   │   └── viewmodel/          # StateFlow ViewModels (Games, Orientation, Session, etc.)
│   └── build.gradle.kts
│
├── backend/                    # FastAPI Backend Application
│   ├── app/
│   │   ├── api/                # Endpoints (auth, dashboard, gameplay, care_team)
│   │   ├── db/                 # Database engine and session lifecycle
│   │   ├── migrations/         # Alembic database migration revisions
│   │   ├── models/             # SQLAlchemy ORM models (User, Patient, Caregiver, etc.)
│   │   ├── schemas/            # Pydantic validation request/response schemas
│   │   ├── scripts/            # Database seed script (seed_demo.py)
│   │   ├── services/           # Analysis engine and performance aggregation
│   │   ├── security.py         # Password hashing (Argon2) and JWT handling
│   │   └── main.py             # FastAPI entrypoint, middleware, and router mounting
│   ├── tests/                  # Backend unit and integration test suite
│   ├── alembic.ini
│   ├── Dockerfile
│   └── pyproject.toml
│
├── frontend/                   # Caregiver Web Portal (React 19 / Vite 8)
│   ├── src/
│   │   ├── components/         # Reusable widgets, illustrations, AuthNavbar, Icons
│   │   ├── pages/              # Landing Page, Login, Register, Caregiver Workspaces
│   │   ├── hooks/              # Data fetching and state hooks
│   │   ├── api.js              # API client and authentication token management
│   │   └── styles.css          # Comprehensive design system and responsive styles
│   ├── package.json
│   └── vite.config.js
│
├── voice_service/              # Dedicated Voice ML Microservice (FastAPI / Whisper / Qwen)
│   ├── app/                    # Voice interpretation API and CUDA loader
│   ├── requirements.txt
│   ├── requirements-models.txt
│   └── README.md
│
├── docker-compose.yml          # Container configuration for backend & PostgreSQL
├── .env.example                # Example backend environment variables
└── README.md                   # Repository documentation
```

---

## Getting Started & Local Development

### Prerequisites
- **Git**
- **Docker & Docker Compose**
- **Python 3.14** (if running backend outside Docker)
- **Node.js 20+ & npm** (for Caregiver Web Portal)
- **Android Studio Ladybug or newer with JDK 17+** (for Android client)

---

### 1. Backend & Database Setup (Docker Compose)

Clone the repository and configure environment variables:
```bash
git clone https://github.com/sumitkmr673/cogni-care.git
cd cogni-care
cp .env.example .env
```

Review `.env` and set a secure `JWT_SECRET_KEY`:
```env
APP_ENV=development
POSTGRES_DB=sih2026
POSTGRES_USER=sih2026
POSTGRES_PASSWORD=change_me
POSTGRES_HOST=db
POSTGRES_PORT=5432
JWT_SECRET_KEY=replace_with_a_long_random_development_secret
JWT_ALGORITHM=HS256
ACCESS_TOKEN_EXPIRE_MINUTES=30
CORS_ALLOWED_ORIGINS=http://localhost:5173,http://127.0.0.1:5173
```

Start the PostgreSQL database and FastAPI backend:
```bash
docker compose up -d --build
```

Apply database migrations and seed development demo data:
```bash
docker compose exec backend alembic upgrade head
docker compose exec backend python -m app.scripts.seed_demo
```

- **Backend API**: `http://localhost:8000`
- **Interactive Swagger Docs**: `http://localhost:8000/docs`
- **Database Health Check**: `http://localhost:8000/health/db`

---

### 2. Caregiver Web Portal Setup

Navigate to the `frontend/` directory, install dependencies, and start the Vite dev server:
```bash
cd frontend
cp .env.example .env
npm install
npm run dev
```

Open `http://localhost:5173` in your browser.
- The web app proxies `/api` calls directly to the FastAPI backend running at `http://localhost:8000`.
- To verify production builds:
  ```bash
  npm run build
  ```

---

### 3. Android Client Setup

1. Open the repository root in **Android Studio**.
2. Sync Gradle dependencies (managed by Gradle Version Catalog in `gradle/libs.versions.toml`).
3. Verify the backend connection endpoint in `RemoteCareRepository.kt` points to your local machine IP or deployed backend URL.
4. Run the app on an Android Emulator or physical Android device (API 26+).

---

### 4. Voice Service Setup (Optional / GPU Machine)

For full voice interaction testing on a machine with an NVIDIA GPU and CUDA 12:
```bash
cd voice_service
python -m venv .venv
source .venv/bin/activate  # On Windows: .venv\Scripts\Activate.ps1
pip install -r requirements-models.txt
uvicorn app.main:app --host 0.0.0.0 --port 8100
```

---

## Testing

### Backend Test Suite
The backend includes an automated test suite covering authentication, RBAC, gameplay session logging, metric aggregation, reminders, and the rule-based analysis engine:

```bash
# From repository root with backend environment active:
python -m unittest discover -s backend/tests -v
```

Test modules include:
- `test_auth.py`: User and caregiver registration, login, and JWT validation.
- `test_rbac.py`: Scoped patient-caregiver access rules and role boundary enforcement.
- `test_dashboard.py`: Overview metrics, recent session retrieval, and patient listings.
- `test_gameplay.py`: Session lifecycle, score calculation, and telemetry recording.
- `test_performance_metrics.py`: Automated daily metric aggregation across categories.
- `test_analysis_engine.py`: Data sufficiency checks, window comparisons, and rule-based heuristic generation.
- `test_reminders.py`: Creator-aware permissions, primary/secondary caregiver access, and schedule lifecycles.
- `test_patient_workflow.py`: End-to-end integration workflows from gameplay to dashboard reflection.

### Frontend Build Verification
Verify syntax, assets, and component compilation:
```bash
cd frontend
npm run build
```

---

## Deployment Architecture

Cogni-Care is structured for modular cloud deployment:

- **API Backend**: Containerized FastAPI service deployed on container platforms (e.g., Render, Railway, or AWS ECS).
- **Relational Database**: Managed PostgreSQL 18 with persistent volume storage and automated backups.
- **Caregiver Web Portal**: Static asset bundle compiled by Vite and served via edge CDN / static hosting (e.g., Cloudflare Pages, Vercel).
- **Voice Service**: Dedicated GPU compute node running faster-whisper and quantized Qwen inference models.
- **Patient Android App**: Native APK/AAB compiled via Gradle and configured to communicate with the deployed API over HTTPS.

---

## Product Boundaries & Future Roadmap

Cogni-Care is dedicated to dignified, accessible cognitive support and caregiver peace of mind.

### Current Capabilities
- Simple, unhurried cognitive gameplay for older adults.
- Offline gameplay with automatic local result caching and network synchronization.
- Transparent, non-clinical caregiver oversight with daily performance trends.
- Deterministic, explainable rule-based performance observations.
- Collaborative care-team access with primary/secondary caregiver roles.
- Creator-aware daily reminder scheduling.

### Future Roadmap
- **Broader Northeast Language Localization**: Complete translation and speech modeling for Assamese, Manipuri, Khasi, Mizo, and Nepali.
- **Adaptive Machine Learning Personalization**: Longitudinal ML models to dynamically tune game difficulty and symbol variety based on patient comfort.
- **Wearable & Sensor Integration**: Correlating gameplay engagement patterns with sleep and ambient routine data.
- **Exportable Care Summary**: Formatted clinical discussion summaries to facilitate informed conversations between families and healthcare specialists.

---

## License

This project is licensed under the terms specified in the [LICENSE](LICENSE) file.
