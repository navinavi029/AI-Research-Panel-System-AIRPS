@echo off
setlocal enabledelayedexpansion

echo ========================================
echo AI Panelist System - Quick Start
echo ========================================
echo.
echo This script runs everything in Docker.
echo No local Java, Maven, or PostgreSQL needed!
echo.

:: Check if Docker is running
echo Checking Docker...
docker ps >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] Docker is not running
    echo.
    echo Please start Docker Desktop and try again.
    echo Download from: https://www.docker.com/products/docker-desktop
    echo.
    pause
    exit /b 1
)
echo [OK] Docker is running
echo.

:: Check if .env file exists
if not exist ".env" (
    if exist ".env.example" (
        echo Creating .env file from template...
        copy .env.example .env
        echo.
        echo [IMPORTANT] Please set your NVIDIA_API_KEY in .env file
        echo Get your free API key at: https://build.nvidia.com/
        echo.
        echo Press any key to open .env file in notepad...
        pause >nul
        notepad .env
        echo.
        echo Have you set your NVIDIA_API_KEY? (Y/N)
        set /p API_KEY_SET=
        if /i "!API_KEY_SET!" neq "Y" (
            echo Please set NVIDIA_API_KEY in .env and run this script again
            pause
            exit /b 1
        )
    ) else (
        echo [ERROR] .env.example not found
        pause
        exit /b 1
    )
)

:: Ask user which mode to run
echo ========================================
echo Choose Deployment Mode
echo ========================================
echo.
echo 1. Production (Recommended for first-time users)
echo 2. Development (Hot reload, debugging, PgAdmin)
echo 3. Production with Nginx (SSL/TLS ready)
echo 4. Exit
echo.
set /p MODE_CHOICE="Enter your choice (1-4): "

if "%MODE_CHOICE%"=="1" (
    echo.
    echo Starting in Production mode...
    echo Building and starting containers...
    docker-compose up -d --build
    
) else if "%MODE_CHOICE%"=="2" (
    echo.
    echo Starting in Development mode...
    echo This includes hot reload, debugging on port 5005, and PgAdmin on port 5050
    echo Building and starting containers...
    docker-compose -f docker-compose.yml -f docker-compose.dev.yml --profile dev up -d --build
    
) else if "%MODE_CHOICE%"=="3" (
    echo.
    echo Starting in Production with Nginx mode...
    echo Building and starting containers...
    docker-compose -f docker-compose.yml -f docker-compose.prod.yml up -d --build
    
) else if "%MODE_CHOICE%"=="4" (
    echo Exiting...
    exit /b 0
    
) else (
    echo Invalid choice
    pause
    exit /b 1
)

if %errorlevel% equ 0 (
    echo.
    echo ========================================
    echo [SUCCESS] Application started!
    echo ========================================
    echo.
    
    if "%MODE_CHOICE%"=="1" (
        echo API: http://localhost:8080
        echo Health: http://localhost:8080/actuator/health
    ) else if "%MODE_CHOICE%"=="2" (
        echo API: http://localhost:8080
        echo Health: http://localhost:8080/actuator/health
        echo PgAdmin: http://localhost:5050
        echo Debug Port: 5005
    ) else if "%MODE_CHOICE%"=="3" (
        echo HTTP: http://localhost:80
        echo HTTPS: https://localhost:443 (after SSL setup)
        echo Health: http://localhost:8080/actuator/health
    )
    
    echo.
    echo Useful commands:
    echo - View logs: docker-compose logs -f
    echo - Check status: docker-compose ps
    echo - Stop: docker-compose down
    echo - Health check: docker-health.bat
    echo.
    echo Waiting for application to start...
    timeout /t 15 /nobreak >nul
    
    echo Testing API health...
    curl -s http://localhost:8080/actuator/health
    echo.
    echo.
    echo Application is ready!
) else (
    echo.
    echo [ERROR] Failed to start containers
    echo Check logs with: docker-compose logs
    echo.
)

pause
