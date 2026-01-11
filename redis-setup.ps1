# =============================================================================
# Redis Setup Script for v-app Development (Windows PowerShell)
# =============================================================================
# This script sets up Redis for local development on Windows
# Usage: .\redis-setup.ps1 [start|stop|restart|status|logs|clean]
# =============================================================================

param(
    [Parameter(Position=0)]
    [string]$Command = "start"
)

$REDIS_CONTAINER = "v-app-redis"
$REDIS_COMMANDER_CONTAINER = "v-app-redis-commander"

# Function to print colored messages
function Print-Info {
    param([string]$Message)
    Write-Host "[INFO] $Message" -ForegroundColor Green
}

function Print-Warn {
    param([string]$Message)
    Write-Host "[WARN] $Message" -ForegroundColor Yellow
}

function Print-Error {
    param([string]$Message)
    Write-Host "[ERROR] $Message" -ForegroundColor Red
}

# Check if Docker is running
function Check-Docker {
    try {
        docker info | Out-Null
        Print-Info "Docker is running ✓"
    } catch {
        Print-Error "Docker is not running. Please start Docker Desktop and try again."
        exit 1
    }
}

# Start Redis
function Start-Redis {
    Print-Info "Starting Redis..."
    docker-compose -f docker-compose-redis.yml up -d
    Print-Info "Waiting for Redis to be healthy..."
    Start-Sleep -Seconds 5
    
    $running = docker ps --filter "name=$REDIS_CONTAINER" --format "{{.Names}}"
    if ($running -eq $REDIS_CONTAINER) {
        Print-Info "Redis started successfully ✓"
        Print-Info "Redis is available at: localhost:6379"
        Print-Info "Redis Commander (GUI) is available at: http://localhost:8081"
    } else {
        Print-Error "Failed to start Redis"
        exit 1
    }
}

# Stop Redis
function Stop-Redis {
    Print-Info "Stopping Redis..."
    docker-compose -f docker-compose-redis.yml stop
    Print-Info "Redis stopped ✓"
}

# Restart Redis
function Restart-Redis {
    Print-Info "Restarting Redis..."
    docker-compose -f docker-compose-redis.yml restart
    Print-Info "Redis restarted ✓"
}

# Show Redis status
function Show-Status {
    Print-Info "Redis Container Status:"
    docker-compose -f docker-compose-redis.yml ps
    
    $running = docker ps --filter "name=$REDIS_CONTAINER" --format "{{.Names}}"
    if ($running -eq $REDIS_CONTAINER) {
        Print-Info ""
        Print-Info "Redis is RUNNING ✓"
        Print-Info "Testing connection..."
        docker exec $REDIS_CONTAINER redis-cli ping
    } else {
        Print-Warn "Redis is NOT running"
    }
}

# Show Redis logs
function Show-Logs {
    Print-Info "Showing Redis logs (press Ctrl+C to exit)..."
    docker-compose -f docker-compose-redis.yml logs -f redis
}

# Clean Redis data
function Clean-Redis {
    $confirm = Read-Host "This will DELETE all Redis data. Are you sure? (yes/no)"
    if ($confirm -eq "yes") {
        Print-Info "Stopping and removing Redis containers..."
        docker-compose -f docker-compose-redis.yml down -v
        Print-Info "Redis data cleaned ✓"
    } else {
        Print-Info "Operation cancelled"
    }
}

# Main script logic
Check-Docker

switch ($Command.ToLower()) {
    "start" {
        Start-Redis
    }
    "stop" {
        Stop-Redis
    }
    "restart" {
        Restart-Redis
    }
    "status" {
        Show-Status
    }
    "logs" {
        Show-Logs
    }
    "clean" {
        Clean-Redis
    }
    default {
        Write-Host "Usage: .\redis-setup.ps1 {start|stop|restart|status|logs|clean}" -ForegroundColor Cyan
        Write-Host ""
        Write-Host "Commands:"
        Write-Host "  start   - Start Redis container"
        Write-Host "  stop    - Stop Redis container"
        Write-Host "  restart - Restart Redis container"
        Write-Host "  status  - Show Redis status"
        Write-Host "  logs    - Show Redis logs"
        Write-Host "  clean   - Remove Redis containers and data"
        exit 1
    }
}
