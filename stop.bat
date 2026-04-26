@echo off
echo Stopping AI Panelist System...
docker-compose stop
echo.

set /p STOP_DOCKER="Stop Docker Desktop too? (y/N): "
if /i "%STOP_DOCKER%"=="y" (
    echo Shutting down Docker Desktop...
    "C:\Program Files\Docker\Docker\Docker Desktop.exe" --quit >nul 2>&1
    taskkill /f /im "Docker Desktop.exe" >nul 2>&1
    echo Docker Desktop stopped.
)

echo Done.
pause
