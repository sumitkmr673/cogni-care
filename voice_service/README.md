# CogniCare voice service

The Android voice assistant's own backend: **Whisper medium** turns the patient's spoken answer into
text, and **Qwen3-8B** decides which of the options on screen they meant.

It is a separate service on purpose. `backend/` is shared by the web portal and the app; voice is
app-only and needs a GPU and two large models that the shared backend and its Docker image should not
carry. This service has **no database** and **never calls the main backend**. It only verifies the
app's existing access token, using the same `JWT_SECRET_KEY`.

```
Android app ──login──────────────► backend/        (shared with the web portal)
            ──voice + same token─► voice_service/  (app only, GPU machine)
```

## Endpoints

All under `/app/voice`, on port `8100` by default. Everything except `/health` needs
`Authorization: Bearer <token from POST /auth/login on the main backend>`.

### `POST /app/voice/interpret` — audio in

`multipart/form-data`:

| field     | value                                               |
|-----------|-----------------------------------------------------|
| `audio`   | the recorded answer (`.m4a`/AAC, `.wav`, `.ogg`, …), max 5 MB |
| `context` | JSON string, see below                              |

```json
{
  "language": "hi",
  "question": "Did you take your medicine?",
  "options": [
    { "id": "MEDICATION_TAKEN",   "label": "Yes, I took it" },
    { "id": "MEDICATION_NOT_YET", "label": "Not yet" }
  ]
}
```

- `language` is the app's language tag. `en`, `hi`, `bn`, `as` and `ne` are passed to Whisper;
  Manipuri, Khasi and Mizo aren't Whisper languages, so for those it auto-detects.
- `question` is null for the free-form assistant ("play a game", "go home").
- `options`: 1–6, unique `id`s. The labels also nudge Whisper towards the words the patient is
  likely to say.

### `POST /app/voice/interpret-text` — text in

For a transcript the phone already has (Android's own recogniser), so no Whisper:

```json
{ "transcript": "haan le li", "context": { …same as above… } }
```

### Response (both)

```json
{
  "transcript": "हाँ, ले ली",
  "choice": "MEDICATION_TAKEN",
  "language": "hi",
  "timings_ms": { "transcribe": 640, "interpret": 180 }
}
```

`choice` is **always one of the ids you sent, or `null`**. `null` means the answer was unclear or said
two things ("no, I took it"); the app asks again gently or lets the patient tap. The model's output is
grammar-constrained to the offered ids, and the server checks again before replying.

### `GET /app/voice/health`

No auth. Reports the configured model names.

## Running it

Needs Python 3.11 and an NVIDIA GPU. An RTX 4060 (8 GB) fits Qwen3-8B Q4 and Whisper medium together.

```powershell
cd voice_service
py -3.11 -m venv .venv
.venv\Scripts\python -m pip install -r requirements-models.txt   # several GB
powershell -ExecutionPolicy Bypass -File run.ps1
```

- The Qwen GGUF is read from `../Models/Qwen3-8B-Q4_K_M.gguf` (override with `VOICE_LLM_PATH`).
- Whisper medium (~1.5 GB) downloads from Hugging Face on the first start and is cached after that.
- `run.ps1` takes `JWT_SECRET_KEY` from `../backend/.env`, then applies `voice_service/.env`
  (see `.env.example`).
- Loading both models takes a while and needs free system RAM while the model file is copied to the GPU.

For a phone on Wi-Fi rather than the emulator, set `VOICE_HOST=0.0.0.0` and allow port 8100 through
the Windows firewall.

## Tests

The tests use stand-ins for both models, so they need no GPU and no downloads:

```powershell
.venv\Scripts\python -m pip install -r requirements-dev.txt
.venv\Scripts\python -m unittest discover -s tests -v
```
