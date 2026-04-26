@echo off
setlocal enabledelayedexpansion

echo ========================================
echo AI Panelist System - Start
echo ========================================
echo.

:: ========================================
:: STEP 1: Check Docker
:: ========================================
echo [1/5] Checking Docker...
docker ps >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] Docker is not running.
    echo Please start Docker Desktop and wait for it to fully start.
    echo.
    pause
    exit /b 1
)
echo [OK] Docker is running
echo.

:: ========================================
:: STEP 2: Check .env file
:: ========================================
echo [2/5] Checking configuration...
if not exist ".env" (
    if exist ".env.example" (
        copy .env.example .env >nul
        echo [OK] Created .env file
    ) else (
        echo [ERROR] .env.example not found
        pause
        exit /b 1
    )
) else (
    echo [OK] .env file exists
)
echo.

:: ========================================
:: STEP 3: Check NVIDIA API Key
:: ========================================
echo [3/5] Checking NVIDIA API Key...

:: Get current API key from .env
set KEY_FOUND=0
set CURRENT_KEY=
for /f "tokens=2 delims==" %%a in ('findstr /C:"NVIDIA_API_KEY=" .env 2^>nul') do (
    set "CURRENT_KEY=%%a"
    set KEY_FOUND=1
)

:: Remove quotes if present
if defined CURRENT_KEY (
    set CURRENT_KEY=!CURRENT_KEY:"=!
)

if !KEY_FOUND! equ 0 (
    set "CURRENT_KEY=your_nvidia_api_key_here"
)

:: Show current key status (masked)
set "MASKED_KEY=!CURRENT_KEY!"
if not "!CURRENT_KEY!"=="your_nvidia_api_key_here" (
    set "MASKED_KEY=***!CURRENT_KEY:~-4!"
)

echo Current API Key: !MASKED_KEY!
echo.

:: Ask if user wants to reconfigure
set /p RECONFIGURE="Do you want to reconfigure the API key? (y/N): "
if /i "!RECONFIGURE!"=="y" (
    echo.
    echo Please get your free API key from: https://build.nvidia.com/
    echo.
    set /p API_KEY="Enter your NVIDIA API Key: "
    if not "!API_KEY!"=="" (
        powershell -Command "$content = Get-Content .env -Raw; $content -replace 'NVIDIA_API_KEY=.*', 'NVIDIA_API_KEY=!API_KEY!' | Set-Content .env -NoNewline"
        echo [OK] API Key saved to .env
    ) else (
        echo [ERROR] API Key cannot be empty
        pause
        exit /b 1
    )
) else (
    if "!CURRENT_KEY!"=="your_nvidia_api_key_here" (
        echo.
        echo [ERROR] NVIDIA API Key is not configured!
        echo Please configure your API key first.
        pause
        exit /b 1
    )
    echo [OK] Using existing API Key
)
echo.

:: ========================================
:: STEP 4: Start Services (Docker)
:: ========================================
echo [4/5] Starting services...
docker-compose up -d --build

if %errorlevel% neq 0 (
    echo [ERROR] Failed to start services
    echo Check logs with: docker-compose logs
    pause
    exit /b 1
)
echo [OK] Services started
echo.

:: ========================================
:: STEP 5: Wait for Health Check
:: ========================================
echo [5/5] Waiting for application to be ready...
echo This may take 1-2 minutes on first run...
echo.

set HEALTH_OK=0
powershell -Command "Start-Sleep -Seconds 10; try { $result = (Invoke-WebRequest -Uri 'http://localhost:8080/actuator/health' -UseBasicParsing -TimeoutSec 5).Content; if ($result -match 'UP') { exit 0 } else { exit 1 } } catch { exit 1 }"
if %errorlevel% equ 0 set HEALTH_OK=1

if !HEALTH_OK! equ 1 (
    echo ========================================
    echo [SUCCESS] AI Panelist System is ready!
    echo ========================================
    echo.
    echo Access the application:
    echo - App: http://localhost:8080
    echo - API Docs: http://localhost:8080/swagger-ui.html
    echo - Health: http://localhost:8080/actuator/health
    echo.
    echo Useful commands:
    echo - View logs: docker-compose logs -f
    echo - Stop: docker-compose down
    echo.
    echo Opening Swagger UI...
    start http://localhost:8080/swagger-ui.html
) else (
    echo [WARNING] Health check timed out, but services may still be starting.
    echo Check status manually at: http://localhost:8080/actuator/health
    echo View logs with: docker-compose logs
    echo.
    echo Opening Swagger UI anyway...
    start http://localhost:8080/swagger-ui.html
    pause
)

endlocal
exit /b 0