@echo off
setlocal enabledelayedexpansion

echo ========================================
echo AI Panelist System - Docker Stop
echo ========================================
echo.

:: Check if Docker is running
docker ps >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] Docker daemon is not running
    pause
    exit /b 1
)

echo Choose what to stop:
echo 1. Stop services (keep data)
echo 2. Stop and remove containers (keep data)
echo 3. Stop and remove everything (including data)
echo 4. Cancel
echo.
set /p CHOICE="Enter your choice (1-4): "

if "%CHOICE%"=="1" (
    echo.
    echo Stopping services...
    docker-compose stop
    echo [OK] Services stopped
    
) else if "%CHOICE%"=="2" (
    echo.
    echo Stopping and removing containers...
    docker-compose down
    echo [OK] Containers removed (data preserved)
    
) else if "%CHOICE%"=="3" (
    echo.
    echo [WARNING] This will delete all data including database and uploaded files!
    set /p CONFIRM="Are you sure? Type 'yes' to confirm: "
    if /i "!CONFIRM!"=="yes" (
        echo.
        echo Stopping and removing everything...
        docker-compose down -v
        echo [OK] All containers and volumes removed
    ) else (
        echo Cancelled
    )
    
) else if "%CHOICE%"=="4" (
    echo Cancelled
    
) else (
    echo Invalid choice
)

echo.
pause
