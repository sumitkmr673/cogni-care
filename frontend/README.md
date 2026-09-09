# Cogni-Care caregiver web prototype

This React/Vite frontend contains the two Phase B prototype screens:

- **Overview**: caregiver dashboard with patient snapshot, activity, reminders, and compact trend.
- **Performance**: game-by-game results and the full performance history chart.

It reads all displayed patient data from the existing FastAPI APIs. The demo
caregiver UUID is configured in `.env` and defaults to the UUID created by the
backend demo seeder.

## Run locally

Start PostgreSQL and the backend first, then seed the demo data:

```bash
docker compose up -d --build
docker compose exec backend python -m app.scripts.seed_demo
```

In a second terminal:

```bash
cd frontend
cp .env.example .env
npm install
npm run dev
```

Open <http://localhost:5173>. Vite proxies `/api` requests to
`http://localhost:8000`, so the frontend can call the existing backend without
adding authentication or changing the backend CORS policy.

To use another backend URL, set `VITE_API_BASE_URL` in `frontend/.env`.
