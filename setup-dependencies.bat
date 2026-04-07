@echo off
setlocal enabledelayedexpansion

echo ========================================
echo AI Panelist System - Dependency Check
echo ========================================
echo.
echo This project runs entirely in Docker.
echo Only Docker Desktop is required!
echo.

set "ERRORS=0"

:: Check Docker
echo [1/2] Checking Docker Desktop...
docker --version >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] Docker is not installed
    echo.
    echo Please install Docker Desktop from:
    echo https://www.docker.com/products/docker-desktop
    echo.
    set /a ERRORS+=1
) else (
    for /f "tokens=3" %%i in ('docker --version 2^>^&1') do (
        echo [OK] Docker found: %%i
    )
    
    :: Check if Docker daemon is running
    docker ps >nul 2>&1
    if %errorlevel% neq 0 (
        echo [ERROR] Docker daemon is not running
        echo Please start Docker Desktop
        echo.
        set /a ERRORS+=1
    ) else (
        echo [OK] Docker daemon is running
    )
)
echo.

:: Check NVIDIA API Key
echo [2/2] Checking NVIDIA API Key...
if not exist ".env" (
    echo [WARNING] .env file not found
    echo Run quick-start.bat to create it from .env.example
    echo.
) else (
    findstr /C:"NVIDIA_API_KEY=" .env >nul 2>&1
    if %errorlevel% equ 0 (
        findstr /C:"NVIDIA_API_KEY=your" .env >nul 2>&1
        if %errorlevel% equ 0 (
            echo [WARNING] NVIDIA_API_KEY not configured in .env
            echo Please set your API key in .env file
            echo Get your free API key at: https://build.nvidia.com/
            echo.
        ) else (
            echo [OK] NVIDIA_API_KEY is configured
        )
    ) else (
        echo [WARNING] NVIDIA_API_KEY not found in .env
        echo Please add NVIDIA_API_KEY=your-key-here to .env
        echo.
    )
)
echo.

:: Summary
echo ========================================
echo Summary
echo ========================================
echo.

if %ERRORS% equ 0 (
    echo [SUCCESS] All dependencies are ready!
    echo.
    echo Next steps:
    echo 1. Get NVIDIA API key: https://build.nvidia.com/
    echo 2. Run: quick-start.bat
    echo 3. Access API at: http://localhost:8080
    echo.
    echo Everything runs in Docker - no local Java, Maven, or PostgreSQL needed!
    echo.
    exit /b 0
) else (
    echo [ERROR] Setup failed with %ERRORS% error(s)
    echo.
    echo Required:
    echo 1. Install Docker Desktop: https://www.docker.com/products/docker-desktop
    echo 2. Start Docker Desktop
    echo 3. Run this script again
    echo.
    exit /b 1
)
