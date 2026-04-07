@echo off
setlocal enabledelayedexpansion

echo ========================================
echo AI Panelist System - Docker Start
echo ========================================
echo.

:: Check if Docker is running
docker ps >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] Docker daemon is not running
    echo Please start Docker Desktop and try again
    pause
    exit /b 1
)

:: Check if .env file exists
if not exist ".env" (
    if exist ".env.example" (
        echo Creating .env file from .env.example...
        copy .env.example .env
        echo.
        echo [WARNING] Please edit .env file and set your NVIDIA_API_KEY
        echo.
        set /p EDIT_ENV="Would you like to edit .env now? (Y/N): "
        if /i "!EDIT_ENV!"=="Y" (
            notepad .env
        )
    ) else (
        echo [ERROR] .env.example not found
        pause
        exit /b 1
    )
)

:: Ask for deployment mode
echo.
echo Choose deployment mode:
echo 1. Production (default)
echo 2. Development (with hot reload and debug)
echo 3. Production with Nginx
echo.
set /p MODE="Enter your choice (1-3) [1]: "
if "%MODE%"=="" set MODE=1

if "%MODE%"=="1" (
    echo.
    echo Starting in production mode...
    docker-compose up -d
    
) else if "%MODE%"=="2" (
    echo.
    echo Starting in development mode...
    docker-compose -f docker-compose.yml -f docker-compose.dev.yml --profile dev up -d
    echo.
    echo Development environment started:
    echo - Application: http://localhost:8080
    echo - PgAdmin: http://localhost:5050
    echo - Debug port: 5005
    
) else if "%MODE%"=="3" (
    echo.
    echo Starting in production mode with Nginx...
    docker-compose -f docker-compose.yml -f docker-compose.prod.yml up -d
    echo.
    echo Production environment started:
    echo - Application: http://localhost:80
    echo - HTTPS: http://localhost:443 (if SSL configured)
    
) else (
    echo Invalid choice
    pause
    exit /b 1
)

if %errorlevel% equ 0 (
    echo.
    echo [SUCCESS] Services started successfully!
    echo.
    echo Waiting for services to be ready...
    timeout /t 15 /nobreak >nul
    
    echo.
    echo Checking health...
    curl -s http://localhost:8080/actuator/health
    echo.
    echo.
    echo Useful commands:
    echo - View logs: docker-compose logs -f
    echo - Stop services: docker-compose down
    echo - Restart: docker-compose restart
    echo - View status: docker-compose ps
    echo.
) else (
    echo.
    echo [ERROR] Failed to start services
    echo Check logs with: docker-compose logs
)

pause
