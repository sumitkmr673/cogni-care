# Cogni-Care caregiver web prototype

React/Vite frontend for the two caregiver screens:

- **Overview**: patient snapshot, activity, reminders, and compact trend
- **Performance**: game-by-game results and full performance history

The prototype reads patient data from the FastAPI caregiver dashboard APIs.
It logs in with the seeded demo caregiver credentials and sends
`Authorization: Bearer <access_token>` on every dashboard request.

## Run locally

Start PostgreSQL and the backend first, then seed the demo data:

```bash
docker compose up -d --build
docker compose exec backend alembic upgrade a06f3f5966ec
docker compose exec backend alembic stamp head
docker compose exec backend python -m app.scripts.seed_demo
```

In a second terminal:

```bash
cd frontend
cp .env.example .env
npm install
npm run dev
```

Open http://localhost:5173. Vite proxies `/api` to `http://localhost:8000`.

Optional `frontend/.env` values:

```text
VITE_API_BASE_URL=/api
VITE_DEMO_CAREGIVER_EMAIL=demo.caregiver@cogni-care.example
VITE_DEMO_CAREGIVER_PASSWORD=DemoCaregiverOnly-2026!
```
