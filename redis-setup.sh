#!/bin/bash

# =============================================================================
# Redis Setup Script for v-app Development
# =============================================================================
# This script sets up Redis for local development
# Usage: ./redis-setup.sh [start|stop|restart|status|logs|clean]
# =============================================================================

set -e

REDIS_CONTAINER="v-app-redis"
REDIS_COMMANDER_CONTAINER="v-app-redis-commander"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to print colored messages
print_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

print_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Check if Docker is running
check_docker() {
    if ! docker info > /dev/null 2>&1; then
        print_error "Docker is not running. Please start Docker Desktop and try again."
        exit 1
    fi
    print_info "Docker is running ✓"
}

# Start Redis
start_redis() {
    print_info "Starting Redis..."
    docker-compose -f docker-compose-redis.yml up -d
    print_info "Waiting for Redis to be healthy..."
    sleep 5
    
    if docker ps | grep -q "$REDIS_CONTAINER"; then
        print_info "Redis started successfully ✓"
        print_info "Redis is available at: localhost:6379"
        print_info "Redis Commander (GUI) is available at: http://localhost:8081"
    else
        print_error "Failed to start Redis"
        exit 1
    fi
}

# Stop Redis
stop_redis() {
    print_info "Stopping Redis..."
    docker-compose -f docker-compose-redis.yml stop
    print_info "Redis stopped ✓"
}

# Restart Redis
restart_redis() {
    print_info "Restarting Redis..."
    docker-compose -f docker-compose-redis.yml restart
    print_info "Redis restarted ✓"
}

# Show Redis status
show_status() {
    print_info "Redis Container Status:"
    docker-compose -f docker-compose-redis.yml ps
    
    if docker ps | grep -q "$REDIS_CONTAINER"; then
        print_info "\nRedis is RUNNING ✓"
        print_info "Testing connection..."
        docker exec $REDIS_CONTAINER redis-cli ping
    else
        print_warn "Redis is NOT running"
    fi
}

# Show Redis logs
show_logs() {
    print_info "Showing Redis logs (press Ctrl+C to exit)..."
    docker-compose -f docker-compose-redis.yml logs -f redis
}

# Clean Redis data
clean_redis() {
    read -p "This will DELETE all Redis data. Are you sure? (yes/no): " confirm
    if [ "$confirm" == "yes" ]; then
        print_info "Stopping and removing Redis containers..."
        docker-compose -f docker-compose-redis.yml down -v
        print_info "Redis data cleaned ✓"
    else
        print_info "Operation cancelled"
    fi
}

# Main script logic
main() {
    check_docker
    
    case "${1:-start}" in
        start)
            start_redis
            ;;
        stop)
            stop_redis
            ;;
        restart)
            restart_redis
            ;;
        status)
            show_status
            ;;
        logs)
            show_logs
            ;;
        clean)
            clean_redis
            ;;
        *)
            echo "Usage: $0 {start|stop|restart|status|logs|clean}"
            echo ""
            echo "Commands:"
            echo "  start   - Start Redis container"
            echo "  stop    - Stop Redis container"
            echo "  restart - Restart Redis container"
            echo "  status  - Show Redis status"
            echo "  logs    - Show Redis logs"
            echo "  clean   - Remove Redis containers and data"
            exit 1
            ;;
    esac
}

main "$@"
