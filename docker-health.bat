@echo off
setlocal enabledelayedexpansion

echo ========================================
echo AI Panelist System - Health Check
echo ========================================
echo.

:: Check if Docker is running
docker ps >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] Docker daemon is not running
    pause
    exit /b 1
)

echo Checking service status...
echo.
docker-compose ps
echo.

echo ========================================
echo Container Health Status
echo ========================================
echo.

:: Check app container
docker inspect aipanelist-app >nul 2>&1
if %errorlevel% equ 0 (
    for /f "tokens=*" %%i in ('docker inspect --format="{{.State.Health.Status}}" aipanelist-app 2^>nul') do set APP_HEALTH=%%i
    if "!APP_HEALTH!"=="healthy" (
        echo [OK] Application: healthy
    ) else if "!APP_HEALTH!"=="starting" (
        echo [WAIT] Application: starting...
    ) else (
        echo [ERROR] Application: !APP_HEALTH!
    )
) else (
    echo [ERROR] Application container not found
)

:: Check postgres container
docker inspect aipanelist-postgres >nul 2>&1
if %errorlevel% equ 0 (
    for /f "tokens=*" %%i in ('docker inspect --format="{{.State.Health.Status}}" aipanelist-postgres 2^>nul') do set DB_HEALTH=%%i
    if "!DB_HEALTH!"=="healthy" (
        echo [OK] Database: healthy
    ) else if "!DB_HEALTH!"=="starting" (
        echo [WAIT] Database: starting...
    ) else (
        echo [ERROR] Database: !DB_HEALTH!
    )
) else (
    echo [ERROR] Database container not found
)

echo.
echo ========================================
echo API Health Check
echo ========================================
echo.

curl -s http://localhost:8080/actuator/health 2>nul
if %errorlevel% equ 0 (
    echo.
    echo [OK] API is responding
) else (
    echo [ERROR] API is not responding
    echo Make sure the application is running and healthy
)

echo.
echo ========================================
echo Resource Usage
echo ========================================
echo.

docker stats --no-stream aipanelist-app aipanelist-postgres 2>nul

echo.
echo ========================================
echo Quick Actions
echo ========================================
echo.
echo 1. View logs
echo 2. Restart services
echo 3. Open shell in app container
echo 4. Open database shell
echo 5. Exit
echo.
set /p ACTION="Choose an action (1-5): "

if "%ACTION%"=="1" (
    docker-compose logs -f
) else if "%ACTION%"=="2" (
    echo Restarting services...
    docker-compose restart
    echo [OK] Services restarted
    pause
) else if "%ACTION%"=="3" (
    docker-compose exec app /bin/bash
) else if "%ACTION%"=="4" (
    docker-compose exec postgres psql -U aipanelist -d aipanelist
) else if "%ACTION%"=="5" (
    exit /b 0
) else (
    echo Invalid choice
    pause
)
