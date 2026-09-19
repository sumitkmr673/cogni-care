# Starts the voice service natively (it needs the GPU, so it does not run in Docker):
#   1. llama-server (llama.cpp's official CUDA build) serving Qwen on 127.0.0.1:8101
#   2. this FastAPI service on VOICE_HOST:VOICE_PORT, with Whisper loaded on the GPU
# Ctrl+C stops both.
#
# Usage, from cogni-care/voice_service:   powershell -ExecutionPolicy Bypass -File run.ps1
# llama-server itself is not in git: unzip llama-b<build>-bin-win-cuda-12.4-x64.zip from
# https://github.com/ggml-org/llama.cpp/releases into voice_service/llama-server/ (see README).
$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot

function Import-EnvFile([string]$path, [string[]]$onlyKeys = @()) {
    if (-not (Test-Path $path)) { return }
    Get-Content $path | ForEach-Object {
        $line = $_.Trim()
        if (-not $line -or $line.StartsWith('#') -or -not $line.Contains('=')) { return }
        $key = ($line.Substring(0, $line.IndexOf('=')).Trim()) -replace "^\xEF\xBB\xBF", ""
        $value = $line.Substring($line.IndexOf('=') + 1).Trim().Trim('"').Trim("'")
        if ($onlyKeys.Count -eq 0 -or $onlyKeys -contains $key) { Set-Item -Path "Env:\$key" -Value $value }
    }
}

# The JWT secret must match the main backend's, so take it from there; then local overrides.
Import-EnvFile "..\backend\.env" @('JWT_SECRET_KEY', 'JWT_ALGORITHM')
Import-EnvFile ".env"

$bindHost = if ($env:VOICE_HOST) { $env:VOICE_HOST } else { '127.0.0.1' }
$port = if ($env:VOICE_PORT) { $env:VOICE_PORT } else { '8100' }
$llmPort = 8101
$env:VOICE_LLM_URL = "http://127.0.0.1:$llmPort"
$model = if ($env:VOICE_LLM_PATH) { $env:VOICE_LLM_PATH } else { "..\Models\Qwen3-8B-Q4_K_M.gguf" }
$context = if ($env:VOICE_LLM_CONTEXT) { $env:VOICE_LLM_CONTEXT } else { '2048' }

$server = Get-ChildItem "llama-server" -Recurse -Filter "llama-server.exe" -ErrorAction SilentlyContinue | Select-Object -First 1
if (-not $server) { throw "llama-server.exe not found under voice_service\llama-server - see README" }
if (-not (Test-Path $model)) { throw "Qwen model not found at $model (set VOICE_LLM_PATH)" }

# CUDA 12 runtime + cuBLAS for llama-server come from the pip packages already in .venv.
$cudaBins = Get-ChildItem ".venv\Lib\site-packages\nvidia" -Directory -ErrorAction SilentlyContinue |
    ForEach-Object { Join-Path $_.FullName 'bin' } | Where-Object { Test-Path $_ }
$env:PATH = (@($cudaBins) + $server.DirectoryName -join ';') + ';' + $env:PATH

Write-Host "Starting llama-server with $model on the GPU (port $llmPort)..."
# Localhost only: nothing outside this PC talks to the model directly.
$llm = Start-Process -FilePath $server.FullName -PassThru -NoNewWindow -ArgumentList @(
    '-m', "`"$((Resolve-Path $model).Path)`"",
    '-ngl', '99',            # every layer on the GPU
    '-c', $context,
    '-np', '1',              # one request at a time; the service serialises GPU work anyway
    '--jinja',               # Qwen3's own chat template, which honours enable_thinking=false
    '--host', '127.0.0.1', '--port', "$llmPort"
)

try {
    $deadline = (Get-Date).AddMinutes(5)
    while ($true) {
        if ($llm.HasExited) { throw "llama-server exited during start-up (exit code $($llm.ExitCode))" }
        try { if ((Invoke-WebRequest "http://127.0.0.1:$llmPort/health" -UseBasicParsing -TimeoutSec 2).StatusCode -eq 200) { break } } catch {}
        if ((Get-Date) -gt $deadline) { throw "llama-server did not become ready within 5 minutes" }
        Start-Sleep -Seconds 2
    }
    Write-Host "Qwen is ready. Starting the voice service on ${bindHost}:${port}..."
    & ".venv\Scripts\python.exe" -m uvicorn app.main:app --host $bindHost --port $port
}
finally {
    if (-not $llm.HasExited) { Stop-Process -Id $llm.Id -Force -ErrorAction SilentlyContinue }
}
