@echo off
REM ============================================================================
REM Redis Docker Quick Start Script
REM ============================================================================
REM This script starts Redis in Docker for V-App VPN access
REM Run this on the VPN server machine (100.122.105.63)
REM ============================================================================

echo.
echo ============================================================================
echo  V-App Redis Docker Setup
echo ============================================================================
echo.

REM Check if Docker is running
echo [1/5] Checking Docker status...
docker version >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] Docker is not running or not installed!
    echo Please start Docker Desktop and try again.
    pause
    exit /b 1
)
echo [OK] Docker is running
echo.

REM Navigate to project directory
echo [2/5] Navigating to project directory...
cd /d %~dp0
echo [OK] In directory: %CD%
echo.

REM Check if docker-compose.yml exists
echo [3/5] Checking docker-compose.yml...
if not exist docker-compose.yml (
    echo [ERROR] docker-compose.yml not found!
    echo Please ensure you are in the correct directory.
    pause
    exit /b 1
)
echo [OK] docker-compose.yml found
echo.

REM Start Redis
echo [4/5] Starting Redis container...
docker-compose up -d redis
if %errorlevel% neq 0 (
    echo [ERROR] Failed to start Redis!
    echo Check Docker Desktop and try again.
    pause
    exit /b 1
)
echo [OK] Redis container started
echo.

REM Wait for Redis to be ready
echo [5/5] Waiting for Redis to be ready...
timeout /t 3 /nobreak >nul

REM Verify Redis is running
echo Testing Redis connection...
docker exec v-app-redis redis-cli ping >nul 2>&1
if %errorlevel% neq 0 (
    echo [WARNING] Redis might not be ready yet. Waiting a bit more...
    timeout /t 5 /nobreak >nul
    docker exec v-app-redis redis-cli ping >nul 2>&1
    if %errorlevel% neq 0 (
        echo [ERROR] Redis is not responding!
        echo Check logs: docker logs v-app-redis
        pause
        exit /b 1
    )
)
echo [OK] Redis is responding to PING
echo.

REM Show container status
echo ============================================================================
echo  Redis Container Status
echo ============================================================================
docker ps --filter "name=v-app-redis"
echo.

REM Show Redis info
echo ============================================================================
echo  Redis Server Info
echo ============================================================================
docker exec v-app-redis redis-cli info server | findstr "redis_version uptime_in_seconds"
echo.

REM Check if Redis Commander is running
docker ps --filter "name=v-app-redis-commander" >nul 2>&1
if %errorlevel% equ 0 (
    echo ============================================================================
    echo  Redis Commander (Web UI)
    echo ============================================================================
    echo  URL: http://localhost:8081
    echo  Username: admin
    echo  Password: admin123
    echo.
)

echo ============================================================================
echo  Setup Complete!
echo ============================================================================
echo.
echo Redis is now running and accessible at:
echo   - From this machine: localhost:6379
echo   - From VPN clients: 100.122.105.63:6379
echo.
echo Next steps:
echo   1. Configure firewall (if not done):
echo      New-NetFirewallRule -DisplayName "Redis Server" -Direction Inbound -LocalPort 6379 -Protocol TCP -Action Allow
echo.
echo   2. Start V-App application
echo.
echo   3. Test Redis from VPN client:
echo      redis-cli -h 100.122.105.63 -p 6379 ping
echo.
echo Useful commands:
echo   - View logs: docker logs -f v-app-redis
echo   - Stop Redis: docker-compose stop redis
echo   - Restart Redis: docker-compose restart redis
echo.

pause
