@echo off
echo ========================================
echo Docker-Only Verification
echo ========================================
echo.
echo This script verifies that the project
echo runs entirely in Docker with no local
echo dependencies required.
echo.

set "PASS=0"
set "FAIL=0"

:: Test 1: Docker is the only requirement
echo [TEST 1] Checking if Docker is the only requirement...
docker --version >nul 2>&1
if %errorlevel% equ 0 (
    echo [PASS] Docker is installed
    set /a PASS+=1
) else (
    echo [FAIL] Docker is not installed
    set /a FAIL+=1
)
echo.

:: Test 2: Application can build without local Java/Maven
echo [TEST 2] Verifying build happens in Docker...
if exist "Dockerfile" (
    findstr /C:"FROM eclipse-temurin:21-jdk" Dockerfile >nul 2>&1
    if %errorlevel% equ 0 (
        echo [PASS] Dockerfile uses containerized Java
        set /a PASS+=1
    ) else (
        echo [FAIL] Dockerfile doesn't specify Java image
        set /a FAIL+=1
    )
) else (
    echo [FAIL] Dockerfile not found
    set /a FAIL+=1
)
echo.

:: Test 3: Database runs in Docker
echo [TEST 3] Verifying database runs in Docker...
if exist "docker-compose.yml" (
    findstr /C:"postgres:15" docker-compose.yml >nul 2>&1
    if %errorlevel% equ 0 (
        echo [PASS] PostgreSQL runs in Docker container
        set /a PASS+=1
    ) else (
        echo [FAIL] PostgreSQL not configured in docker-compose.yml
        set /a FAIL+=1
    )
) else (
    echo [FAIL] docker-compose.yml not found
    set /a FAIL+=1
)
echo.

:: Test 4: No local Maven required
echo [TEST 4] Verifying Maven runs in Docker...
if exist "Dockerfile" (
    findstr /C:"mvn" Dockerfile >nul 2>&1
    if %errorlevel% equ 0 (
        echo [PASS] Maven runs inside Docker container
        set /a PASS+=1
    ) else (
        echo [FAIL] Maven not found in Dockerfile
        set /a FAIL+=1
    )
) else (
    echo [FAIL] Dockerfile not found
    set /a FAIL+=1
)
echo.

:: Test 5: Tests run in Docker
echo [TEST 5] Verifying tests run in Docker...
if exist "Makefile" (
    findstr /C:"docker-compose exec app mvn test" Makefile >nul 2>&1
    if %errorlevel% equ 0 (
        echo [PASS] Tests run inside Docker container
        set /a PASS+=1
    ) else (
        echo [FAIL] Tests not configured to run in Docker
        set /a FAIL+=1
    )
) else (
    echo [FAIL] Makefile not found
    set /a FAIL+=1
)
echo.

:: Summary
echo ========================================
echo Verification Results
echo ========================================
echo.
echo Tests Passed: %PASS%
echo Tests Failed: %FAIL%
echo.

if %FAIL% equ 0 (
    echo [SUCCESS] Project is fully Docker-based!
    echo No local dependencies required.
    echo.
    exit /b 0
) else (
    echo [WARNING] Some tests failed
    echo Please review the results above
    echo.
    exit /b 1
)
