@echo off
setlocal enabledelayedexpansion

echo ========================================
echo AI Panelist System - Docker Logs
echo ========================================
echo.

:: Check if Docker is running
docker ps >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] Docker daemon is not running
    pause
    exit /b 1
)

echo Choose which logs to view:
echo 1. All services
echo 2. Application only
echo 3. Database only
echo 4. Last 100 lines (all services)
echo 5. Last 100 lines (application only)
echo.
set /p CHOICE="Enter your choice (1-5) [1]: "
if "%CHOICE%"=="" set CHOICE=1

if "%CHOICE%"=="1" (
    echo.
    echo Viewing all logs (press Ctrl+C to exit)...
    docker-compose logs -f
    
) else if "%CHOICE%"=="2" (
    echo.
    echo Viewing application logs (press Ctrl+C to exit)...
    docker-compose logs -f app
    
) else if "%CHOICE%"=="3" (
    echo.
    echo Viewing database logs (press Ctrl+C to exit)...
    docker-compose logs -f postgres
    
) else if "%CHOICE%"=="4" (
    echo.
    echo Last 100 lines from all services:
    docker-compose logs --tail=100
    pause
    
) else if "%CHOICE%"=="5" (
    echo.
    echo Last 100 lines from application:
    docker-compose logs --tail=100 app
    pause
    
) else (
    echo Invalid choice
    pause
)
