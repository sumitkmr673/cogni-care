# Starts the voice service natively (it needs the GPU, so it does not run in Docker).
# Usage, from cogni-care/voice_service:   powershell -ExecutionPolicy Bypass -File run.ps1
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

& ".venv\Scripts\python.exe" -m uvicorn app.main:app --host $bindHost --port $port
