.PHONY: help build up down restart logs clean test dev prod status health

# Default target
help:
	@echo "AI Panelist System - Docker Commands"
	@echo ""
	@echo "Development:"
	@echo "  make dev          - Start in development mode with hot reload"
	@echo "  make dev-logs     - View development logs"
	@echo "  make dev-down     - Stop development environment"
	@echo ""
	@echo "Production:"
	@echo "  make prod         - Start in production mode"
	@echo "  make prod-logs    - View production logs"
	@echo "  make prod-down    - Stop production environment"
	@echo ""
	@echo "General:"
	@echo "  make build        - Build Docker images"
	@echo "  make up           - Start services (default mode)"
	@echo "  make down         - Stop all services"
	@echo "  make restart      - Restart all services"
	@echo "  make logs         - View logs"
	@echo "  make status       - Show service status"
	@echo "  make health       - Check application health"
	@echo "  make shell        - Open shell in app container"
	@echo "  make db-shell     - Open PostgreSQL shell"
	@echo "  make test         - Run tests in container"
	@echo "  make clean        - Remove all containers, volumes, and images"
	@echo "  make backup-db    - Backup database"
	@echo "  make restore-db   - Restore database from backup"

# Build Docker images
build:
	docker-compose build

# Start services (default mode)
up:
	docker-compose up -d
	@echo "Services started. Access the application at http://localhost:8080"
	@echo "Run 'make logs' to view logs"

# Stop services
down:
	docker-compose down

# Restart services
restart:
	docker-compose restart

# View logs
logs:
	docker-compose logs -f

# Development mode
dev:
	docker-compose -f docker-compose.yml -f docker-compose.dev.yml --profile dev up -d
	@echo "Development environment started with hot reload"
	@echo "Application: http://localhost:8080"
	@echo "PgAdmin: http://localhost:5050"
	@echo "Debug port: 5005"

dev-logs:
	docker-compose -f docker-compose.yml -f docker-compose.dev.yml logs -f

dev-down:
	docker-compose -f docker-compose.yml -f docker-compose.dev.yml --profile dev down

# Production mode (same as default 'up' command)
prod:
	docker-compose up -d
	@echo "Production environment started"
	@echo "Application: http://localhost:8080"

prod-logs:
	docker-compose logs -f

prod-down:
	docker-compose down

# Show service status
status:
	docker-compose ps

# Check application health
health:
	@echo "Checking application health..."
	@curl -s http://localhost:8080/actuator/health | jq . || echo "Application not responding"

# Open shell in app container
shell:
	docker-compose exec app /bin/bash

# Open PostgreSQL shell
db-shell:
	docker-compose exec postgres psql -U aipanelist -d aipanelist

# Run tests in container
test:
	docker-compose exec app mvn test

# Run specific test
test-class:
	@if [ -z "$(CLASS)" ]; then echo "Usage: make test-class CLASS=DocumentControllerTest"; exit 1; fi
	docker-compose exec app mvn test -Dtest=$(CLASS)

# Clean everything
clean:
	docker-compose down -v --rmi all
	@echo "All containers, volumes, and images removed"

# Backup database
backup-db:
	@mkdir -p backups
	docker-compose exec -T postgres pg_dump -U aipanelist aipanelist > backups/backup_$$(date +%Y%m%d_%H%M%S).sql
	@echo "Database backed up to backups/"

# Restore database (usage: make restore-db FILE=backups/backup_20240101_120000.sql)
restore-db:
	@if [ -z "$(FILE)" ]; then echo "Usage: make restore-db FILE=backups/backup_file.sql"; exit 1; fi
	docker-compose exec -T postgres psql -U aipanelist aipanelist < $(FILE)
	@echo "Database restored from $(FILE)"

# View application logs only
app-logs:
	docker-compose logs -f app

# View database logs only
db-logs:
	docker-compose logs -f postgres

# Rebuild and restart
rebuild:
	docker-compose down
	docker-compose build --no-cache
	docker-compose up -d
	@echo "Services rebuilt and restarted"

# Pull latest images
pull:
	docker-compose pull

# Show resource usage
stats:
	docker stats aipanelist-app aipanelist-postgres
