@echo off
setlocal enabledelayedexpansion

echo ========================================
echo AI Panelist System - Docker Installer
echo ========================================
echo.
echo This project only requires Docker Desktop.
echo No Java, Maven, PostgreSQL, or other tools needed!
echo.
pause

:: Check if Docker is already installed
docker --version >nul 2>&1
if %errorlevel% equ 0 (
    echo [OK] Docker is already installed
    docker --version
    echo.
    
    :: Check if Docker is running
    docker ps >nul 2>&1
    if %errorlevel% equ 0 (
        echo [OK] Docker is running
        echo.
        echo You're all set! Run quick-start.bat to start the application.
    ) else (
        echo [WARNING] Docker is installed but not running
        echo Please start Docker Desktop and try again.
    )
    echo.
    pause
    exit /b 0
)

:: Docker not installed - provide installation instructions
echo Docker Desktop is not installed.
echo.
echo ========================================
echo Installation Instructions
echo ========================================
echo.
echo 1. Download Docker Desktop:
echo    https://www.docker.com/products/docker-desktop
echo.
echo 2. Run the installer
echo.
echo 3. Restart your computer if prompted
echo.
echo 4. Start Docker Desktop
echo.
echo 5. Run setup-dependencies.bat to verify installation
echo.
echo ========================================
echo.

:: Ask if user wants to open download page
echo Would you like to open the Docker Desktop download page? (Y/N)
set /p OPEN_DOWNLOAD=
if /i "!OPEN_DOWNLOAD!"=="Y" (
    start https://www.docker.com/products/docker-desktop
    echo.
    echo Opening download page in your browser...
    echo.
    echo After installing Docker Desktop:
    echo 1. Restart your computer
    echo 2. Start Docker Desktop
    echo 3. Run setup-dependencies.bat
    echo.
)

pause
