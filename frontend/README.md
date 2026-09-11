# Cogni-Care caregiver web prototype

React/Vite frontend for the two caregiver screens:

- **Overview**: patient snapshot, activity, reminders, and compact trend
- **Performance**: game-by-game results and full performance history

The prototype signs in through the real `POST /auth/login` endpoint, stores the
access token for the current browser, and sends it on protected API requests.
Demo credentials can be provided through environment variables to prefill the
login form.

The screens use:

- `GET /patients`
- `GET /patients/{patient_id}/dashboard`
- `GET /patients/{patient_id}/trends`
- `GET /patients/{patient_id}/sessions`
- `GET /patients/{patient_id}/reminders`
- `POST /patients/{patient_id}/reminders`

## Run locally

Use the existing project Docker environment and database:

```bash
docker compose up -d
```

Do not reset the database or rerun migrations for frontend development. If the
existing database does not contain demo data, follow the backend seeding
instructions in the repository README without changing the Compose services or
database volume.

In a second terminal:

```bash
cd frontend
cp .env.example .env
npm install
npm run dev
```

Open http://localhost:5173. Vite proxies `/api` to `http://localhost:8000`.
Sign in with a caregiver account assigned to at least one patient. The seeded
demo caregiver can be used for the presentation.

## Demo access

Development/testing credentials from `backend/app/scripts/seed_demo.py`:

```text
Email:    demo.caregiver@cogni-care.example
Password: DemoCaregiverOnly-2026!
```

These credentials are intentionally for local demonstration only. The login
page's **Use demo account** action fills them into the real sign-in form; it
does not bypass backend authentication.

Optional `frontend/.env` values:

```text
VITE_API_BASE_URL=/api
VITE_DEMO_CAREGIVER_EMAIL=demo.caregiver@cogni-care.example
VITE_DEMO_CAREGIVER_PASSWORD=DemoCaregiverOnly-2026!
```
