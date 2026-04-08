# Docker Deployment Guide

This comprehensive guide explains how to deploy and manage the AI Panelist System using Docker.

This comprehensive guide explains how to deploy the AI Panelist System using Docker and Docker Compose with multiple deployment modes.

## Table of Contents

- [Prerequisites](#prerequisites)
- [Quick Start](#quick-start)
- [Deployment Modes](#deployment-modes)
- [Configuration](#configuration)
- [Management Commands](#management-commands)
- [API Endpoints](#api-endpoints)
- [Monitoring and Health Checks](#monitoring-and-health-checks)
- [Troubleshooting](#troubleshooting)
- [Production Deployment](#production-deployment)
- [Backup and Restore](#backup-and-restore)
- [Architecture](#architecture)

## Prerequisites

**Only 2 things required:**

1. **Docker Desktop 20.10+** - [Download here](https://www.docker.com/products/docker-desktop)
2. **NVIDIA API Key** (free) - [Get it here](https://build.nvidia.com/)

**That's it!** No Java, Maven, PostgreSQL, Git, or any other local tools needed. Everything runs in containers.

### System Requirements

- **RAM**: 4GB minimum (8GB recommended for production)
- **Disk**: 10GB free space minimum
- **OS**: Windows 10/11, macOS 10.15+, or Linux with Docker support

### Verify Docker Installation

```bash
# Check Docker version
docker --version

# Check Docker Compose version
docker-compose --version

# Verify Docker is running
docker ps
```

If any of these commands fail, install or start Docker Desktop.

### Platform-Specific Setup

**Windows:**
```bash
# Use provided batch files
docker-start.bat
```

**Linux/macOS:**
```bash
# Use Makefile commands
make up
```

## Quick Start

### Option 1: Windows Batch Files (Easiest)

1. **Run the quick start script**
   ```bash
   docker-start.bat
   ```
   This interactive script will:
   - Check if Docker is running
   - Help you configure environment variables
   - Let you choose deployment mode
   - Start all services

2. **Check health**
   ```bash
   docker-health.bat
   ```

### Option 2: Manual Setup

1. **Create environment file**
   ```bash
   cp .env.example .env
   ```

2. **Edit `.env` and set your NVIDIA API key**
   ```env
   NVIDIA_API_KEY=your_actual_api_key_here
   ```

3. **Start the application**
   ```bash
   # Default production mode
   docker-compose up -d
   
   # Or use Makefile (Linux/macOS)
   make up
   ```

4. **Verify deployment**
   ```bash
   # Check health
   curl http://localhost:8080/actuator/health
   
   # View logs
   docker-compose logs -f app
   ```

## Deployment Modes

The system supports three deployment modes optimized for different use cases:

### 1. Default Production Mode

Standard production deployment with optimized settings.

**Start:**
```bash
# Windows
docker-start.bat
# Select option 1 (Production)

# Linux/macOS
make up
# or
docker-compose up -d
```

**Features:**
- Optimized JVM settings (512MB-2GB heap)
- INFO level logging
- Health checks enabled
- Resource limits configured
- Auto-restart on failure

**Access:**
- Application: http://localhost:8080
- Health: http://localhost:8080/actuator/health

### 2. Development Mode

Development environment with hot reload and debugging capabilities.

**Start:**
```bash
# Windows
docker-start.bat
# Select option 3 (Production with Nginx)

# Linux/macOS
make prod
```

**Features:**
- Nginx reverse proxy with rate limiting
- SSL/TLS support (configure certificates)
- Enhanced PostgreSQL tuning
- 4GB JVM heap
- Production-grade logging

**Access:**
- HTTP: http://localhost:80
- HTTPS: https://localhost:443 (after SSL setup)

**SSL Setup:**
```bash
# Place your certificates in nginx/ssl/
mkdir -p nginx/ssl
cp your-cert.crt nginx/ssl/
cp your-key.key nginx/ssl/
```

## Configuration

### Environment Variables

All configuration is managed through `.env` file. Copy from `.env.example`:

**Required:**
```env
NVIDIA_API_KEY=your_nvidia_api_key_here
```

**Database:**
```env
POSTGRES_PASSWORD=changeme          # Change in production!
POSTGRES_PORT=5432
```

**Server:**
```env
SERVER_PORT=8080
LOG_LEVEL_ROOT=INFO
LOG_LEVEL_APP=DEBUG
```

**JVM:**
```env
JAVA_OPTS=-Xms512m -Xmx2048m -XX:+UseG1GC
```

**Advanced (Optional):**
```env
# Processing
PROCESSING_CHUNK_SIZE=100000
PROCESSING_MAX_RETRIES=3
PROCESSING_RETRY_DELAY_MS=1000

# Async Thread Pool
ASYNC_CORE_POOL_SIZE=10
ASYNC_MAX_POOL_SIZE=20
ASYNC_QUEUE_CAPACITY=100

# File Upload
STORAGE_MAX_FILE_SIZE=52428800     # 50MB
```

### Volumes

The application uses persistent volumes for data storage:

| Volume | Purpose | Location |
|--------|---------|----------|
| `postgres-data` | PostgreSQL database | `/var/lib/postgresql/data` |
| `upload-data` | Uploaded documents | `/app/data` |
| `pgadmin-data` | PgAdmin settings (dev) | `/var/lib/pgadmin` |
| `maven-cache` | Maven dependencies (dev) | `/root/.m2` |

**Volume Management:**
```bash
# List volumes
docker volume ls

# Inspect volume
docker volume inspect aipanelist_postgres-data

# Backup volume
docker run --rm -v aipanelist_postgres-data:/data -v $(pwd):/backup alpine tar czf /backup/postgres-backup.tar.gz /data
```

## API Endpoints

Once running, the API is available at `http://localhost:8080`:

### Document Upload
```bash
POST /api/documents/upload
Content-Type: multipart/form-data

curl -X POST http://localhost:8080/api/documents/upload \
  -F "file=@research-paper.pdf"

Response:
{
  "documentId": "550e8400-e29b-41d4-a716-446655440000",
  "filename": "research-paper.pdf",
  "uploadedAt": "2024-01-15T10:30:00Z",
  "status": "UPLOADED"
}
```

### Check Processing Status
```bash
GET /api/documents/{documentId}/status

curl http://localhost:8080/api/documents/{documentId}/status

Response:
{
  "documentId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "PROCESSING",
  "progress": {
    "QUICK_SCREENER": "COMPLETED",
    "FACT_EXTRACTOR": "IN_PROGRESS",
    "LITERATURE_REVIEWER": "PENDING",
    "METHODOLOGY_REVIEWER": "PENDING",
    "GENERAL_ANALYST": "PENDING",
    "LEAD_ANALYST": "PENDING"
  }
}
```

### Get Consensus Report
```bash
GET /api/documents/{documentId}/results

curl http://localhost:8080/api/documents/{documentId}/results

Response:
{
  "documentId": "550e8400-e29b-41d4-a716-446655440000",
  "consensusReport": "...",
  "generatedAt": "2024-01-15T10:35:00Z"
}
```

### Get Detailed Agent Reports
```bash
GET /api/documents/{documentId}/results/detailed

curl http://localhost:8080/api/documents/{documentId}/results/detailed

Response:
{
  "documentId": "550e8400-e29b-41d4-a716-446655440000",
  "consensusReport": "...",
  "agentReports": {
    "QUICK_SCREENER": "...",
    "FACT_EXTRACTOR": "...",
    "LITERATURE_REVIEWER": "...",
    "METHODOLOGY_REVIEWER": "...",
    "GENERAL_ANALYST": "...",
    "LEAD_ANALYST": "..."
  },
  "generatedAt": "2024-01-15T10:35:00Z"
}
```

### Health Check
```bash
GET /actuator/health

curl http://localhost:8080/actuator/health

Response:
{
  "status": "UP",
  "components": {
    "db": {"status": "UP"},
    "diskSpace": {"status": "UP"},
    "ping": {"status": "UP"}
  }
}
```

## Management Commands

### Windows Batch Files

**docker-start.bat** - Interactive startup
```bash
docker-start.bat
# Choose: 1=Production, 2=Development
```

**docker-stop.bat** - Interactive shutdown
```bash
docker-stop.bat
# Choose: 1=Stop services, 2=Stop+remove volumes, 3=Full cleanup
```

**docker-logs.bat** - View logs
```bash
docker-logs.bat
# Choose: 1=All logs, 2=App only, 3=Database only, 4=Follow mode
```

**docker-health.bat** - Health check and quick actions
```bash
docker-health.bat
# Shows: Service status, health checks, resource usage
# Quick actions: Restart, logs, shell access
```

### Makefile Commands (Linux/macOS)

**Development:**
```bash
make dev          # Start development mode
make dev-logs     # View development logs
make dev-down     # Stop development environment
```

**Production:**
```bash
make prod         # Start production mode with Nginx
make prod-logs    # View production logs
make prod-down    # Stop production environment
```

**General:**
```bash
make build        # Build Docker images
make up           # Start services (default mode)
make down         # Stop all services
make restart      # Restart all services
make logs         # View all logs
make app-logs     # View application logs only
make db-logs      # View database logs only
make status       # Show service status
make health       # Check application health
make stats        # Show resource usage
```

**Container Access:**
```bash
make shell        # Open bash shell in app container
make db-shell     # Open PostgreSQL shell
make test         # Run tests in container
```

**Maintenance:**
```bash
make clean        # Remove all containers, volumes, and images
make rebuild      # Rebuild from scratch
make pull         # Pull latest base images
make backup-db    # Backup database
make restore-db FILE=backups/backup.sql  # Restore database
```

### Docker Compose Commands

**Basic Operations:**
```bash
# Start services
docker-compose up -d

# Stop services
docker-compose down

# View logs
docker-compose logs -f

# Restart specific service
docker-compose restart app

# Rebuild and restart
docker-compose up -d --build
```

**Service Management:**
```bash
# Scale application (if configured)
docker-compose up -d --scale app=3

# Execute command in container
docker-compose exec app bash
docker-compose exec postgres psql -U aipanelist

# View service status
docker-compose ps

# View resource usage
docker-compose top
```

**Development Mode:**
```bash
docker-compose -f docker-compose.yml -f docker-compose.dev.yml --profile dev up -d
```

**Production Mode:**
```bash
docker-compose -f docker-compose.yml -f docker-compose.prod.yml up -d
```

## Monitoring and Health Checks

### Application Health

The application includes comprehensive health checks:

**Endpoint:** `http://localhost:8080/actuator/health`

**Health Check Schedule:**
- Interval: 30 seconds
- Timeout: 10 seconds
- Retries: 3
- Startup grace period: 60 seconds

**Check Health:**
```bash
# Windows
docker-health.bat

# Linux/macOS
make health

# Manual
curl http://localhost:8080/actuator/health | jq
```

### Database Health

PostgreSQL includes automatic health monitoring:

**Check:**
```bash
docker-compose exec postgres pg_isready -U aipanelist
```

**Connection Test:**
```bash
docker-compose exec postgres psql -U aipanelist -d aipanelist -c "SELECT version();"
```

### Resource Monitoring

**View Resource Usage:**
```bash
# Windows
docker stats aipanelist-app aipanelist-postgres

# Linux/macOS
make stats
```

**Expected Resource Usage:**
- Application: 512MB-2GB RAM, 1-2 CPU cores
- PostgreSQL: 256MB-1GB RAM, 0.5-1 CPU core

### Log Monitoring

**View Logs:**
```bash
# All services
docker-compose logs -f

# Application only
docker-compose logs -f app

# Last 100 lines
docker-compose logs --tail=100 app

# Since timestamp
docker-compose logs --since 2024-01-15T10:00:00 app
```

**Log Levels:**
- Production: INFO (app), WARN (root)
- Development: TRACE (app), DEBUG (root)

### Monitoring Integration

**Prometheus Metrics:**
```bash
# Available at (if enabled)
http://localhost:8080/actuator/prometheus
```

**Grafana Dashboard:**
```yaml
# Add to docker-compose.yml
grafana:
  image: grafana/grafana:latest
  ports:
    - "3000:3000"
  volumes:
    - grafana-data:/var/lib/grafana
```

## Troubleshooting

### Common Issues

#### Application won't start

**Symptoms:** Container exits immediately or restarts continuously

**Solutions:**
1. Check logs for errors:
   ```bash
   docker-compose logs app
   ```

2. Verify NVIDIA API key is set:
   ```bash
   docker-compose exec app env | grep NVIDIA_API_KEY
   ```

3. Ensure PostgreSQL is healthy:
   ```bash
   docker-compose ps
   docker-compose logs postgres
   ```

4. Check for port conflicts:
   ```bash
   # Windows
   netstat -ano | findstr :8080
   
   # Linux/macOS
   lsof -i :8080
   ```

#### Database connection errors

**Symptoms:** "Connection refused" or "Unknown host" errors

**Solutions:**
1. Wait for PostgreSQL to be ready (30-60 seconds on first start):
   ```bash
   docker-compose logs postgres | grep "ready to accept connections"
   ```

2. Verify database credentials match in `.env` and `docker-compose.yml`

3. Check network connectivity:
   ```bash
   docker-compose exec app ping postgres
   docker-compose exec app nc -zv postgres 5432
   ```

4. Restart services in order:
   ```bash
   docker-compose restart postgres
   sleep 10
   docker-compose restart app
   ```

#### Out of memory errors

**Symptoms:** "OutOfMemoryError" in logs or container killed by OOM

**Solutions:**
1. Increase Docker memory limit:
   - Docker Desktop: Settings → Resources → Memory (set to 4GB+)

2. Adjust JVM heap size in `.env`:
   ```env
   JAVA_OPTS=-Xms1024m -Xmx3072m -XX:+UseG1GC
   ```

3. Reduce concurrent processing:
   ```env
   ASYNC_MAX_POOL_SIZE=10
   ASYNC_CORE_POOL_SIZE=5
   ```

4. Monitor memory usage:
   ```bash
   docker stats aipanelist-app
   ```

#### File upload failures

**Symptoms:** 413 Request Entity Too Large or upload errors

**Solutions:**
1. Check volume mount permissions:
   ```bash
   docker-compose exec app ls -la /app/data
   docker-compose exec app touch /app/data/test.txt
   ```

2. Verify file size limit in `.env`:
   ```env
   STORAGE_MAX_FILE_SIZE=52428800  # 50MB
   ```

3. Check available disk space:
   ```bash
   docker-compose exec app df -h /app/data
   ```

4. Check available disk space:
   ```bash
   docker-compose exec app df -h /app/data
   ```

#### Slow processing or timeouts

**Symptoms:** Documents stuck in PROCESSING status

**Solutions:**
1. Check NVIDIA API connectivity:
   ```bash
   docker-compose exec app curl -I https://integrate.api.nvidia.com/v1
   ```

2. Verify API key is valid:
   ```bash
   # Check logs for authentication errors
   docker-compose logs app | grep -i "nvidia\|api\|auth"
   ```

3. Increase timeout values in `.env`:
   ```env
   NVIDIA_API_TIMEOUT_MS=60000
   PROCESSING_MAX_RETRIES=5
   ```

4. Monitor agent progress:
   ```bash
   curl http://localhost:8080/api/documents/{documentId}/status
   ```

#### Docker Compose version issues

**Symptoms:** "unknown flag" or syntax errors

**Solutions:**
1. Check Docker Compose version:
   ```bash
   docker-compose version
   ```

2. Upgrade to V2 (recommended):
   ```bash
   # Install Docker Compose V2
   # Follow: https://docs.docker.com/compose/install/
   ```

3. Use V2 syntax:
   ```bash
   docker compose up -d  # Note: no hyphen
   ```

### Debug Mode

Enable detailed logging for troubleshooting:

1. **Edit `.env`:**
   ```env
   LOG_LEVEL_ROOT=DEBUG
   LOG_LEVEL_APP=TRACE
   SPRING_JPA_SHOW_SQL=true
   ```

2. **Restart services:**
   ```bash
   docker-compose restart app
   ```

3. **View detailed logs:**
   ```bash
   docker-compose logs -f app
   ```

### Getting Help

If issues persist:

1. **Collect diagnostic information:**
   ```bash
   # System info
   docker version
   docker-compose version
   
   # Service status
   docker-compose ps
   
   # Recent logs
   docker-compose logs --tail=100 > logs.txt
   
   # Environment (remove sensitive data!)
   cat .env
   ```

2. **Check GitHub issues:** Search for similar problems

3. **Open new issue:** Include diagnostic information and error messages

## Production Deployment

### Pre-Deployment Checklist

Before deploying to production:

- [ ] Change default PostgreSQL password
- [ ] Set strong NVIDIA_API_KEY
- [ ] Configure SSL/TLS certificates
- [ ] Set up firewall rules
- [ ] Configure backup strategy
- [ ] Set up monitoring and alerting
- [ ] Test disaster recovery procedures
- [ ] Review resource limits
- [ ] Configure log rotation
- [ ] Set up secrets management

### Security Hardening

#### 1. Secrets Management

**Use Docker Secrets (Swarm mode):**
```yaml
# docker-compose.prod.yml
secrets:
  nvidia_api_key:
    external: true
  postgres_password:
    external: true

services:
  app:
    secrets:
      - nvidia_api_key
      - postgres_password
    environment:
      NVIDIA_API_KEY_FILE: /run/secrets/nvidia_api_key
```

**Or use environment file with restricted permissions:**
```bash
chmod 600 .env
chown root:root .env
```

#### 2. Network Security

**Restrict exposed ports:**
```yaml
# Only expose Nginx, not app directly
services:
  app:
    ports: []  # Remove port mapping
    expose:
      - "8080"
```

**Use internal networks:**
```yaml
networks:
  frontend:
    driver: bridge
  backend:
    driver: bridge
    internal: true
```

#### 3. SSL/TLS Configuration

**Generate self-signed certificate (testing):**
```bash
mkdir -p nginx/ssl
openssl req -x509 -nodes -days 365 -newkey rsa:2048 \
  -keyout nginx/ssl/server.key \
  -out nginx/ssl/server.crt
```

**Use Let's Encrypt (production):**
```bash
# Install certbot
apt-get install certbot

# Generate certificate
certbot certonly --standalone -d yourdomain.com

# Copy to nginx/ssl/
cp /etc/letsencrypt/live/yourdomain.com/fullchain.pem nginx/ssl/
cp /etc/letsencrypt/live/yourdomain.com/privkey.pem nginx/ssl/
```

#### 4. Database Security

**Restrict PostgreSQL access:**
```yaml
postgres:
  environment:
    POSTGRES_HOST_AUTH_METHOD: scram-sha-256
  command:
    - "postgres"
    - "-c"
    - "ssl=on"
    - "-c"
    - "ssl_cert_file=/etc/ssl/certs/server.crt"
    - "-c"
    - "ssl_key_file=/etc/ssl/private/server.key"
```

### Performance Optimization

#### 1. JVM Tuning

**Production JVM settings:**
```env
JAVA_OPTS=-Xms2048m -Xmx4096m \
  -XX:+UseG1GC \
  -XX:MaxGCPauseMillis=200 \
  -XX:+UseStringDeduplication \
  -XX:+ParallelRefProcEnabled \
  -XX:MaxMetaspaceSize=512m \
  -XX:+HeapDumpOnOutOfMemoryError \
  -XX:HeapDumpPath=/app/data/heapdump.hprof
```

#### 2. PostgreSQL Tuning

**Optimize for production workload:**
```yaml
postgres:
  command:
    - "postgres"
    - "-c"
    - "max_connections=200"
    - "-c"
    - "shared_buffers=512MB"
    - "-c"
    - "effective_cache_size=2GB"
    - "-c"
    - "maintenance_work_mem=128MB"
    - "-c"
    - "checkpoint_completion_target=0.9"
    - "-c"
    - "wal_buffers=16MB"
    - "-c"
    - "default_statistics_target=100"
    - "-c"
    - "random_page_cost=1.1"
    - "-c"
    - "effective_io_concurrency=200"
```

#### 3. Connection Pooling

**Configure HikariCP:**
```env
SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE=20
SPRING_DATASOURCE_HIKARI_MINIMUM_IDLE=10
SPRING_DATASOURCE_HIKARI_CONNECTION_TIMEOUT=30000
SPRING_DATASOURCE_HIKARI_IDLE_TIMEOUT=600000
SPRING_DATASOURCE_HIKARI_MAX_LIFETIME=1800000
```

### High Availability

#### 1. Load Balancing

**Nginx upstream configuration:**
```nginx
upstream app_backend {
    least_conn;
    server app1:8080 max_fails=3 fail_timeout=30s;
    server app2:8080 max_fails=3 fail_timeout=30s;
    server app3:8080 max_fails=3 fail_timeout=30s;
}
```

#### 2. Database Replication

**PostgreSQL streaming replication:**
```yaml
postgres-primary:
  image: postgres:15-alpine
  environment:
    POSTGRES_REPLICATION_MODE: master
    POSTGRES_REPLICATION_USER: replicator
    POSTGRES_REPLICATION_PASSWORD: repl_password

postgres-replica:
  image: postgres:15-alpine
  environment:
    POSTGRES_REPLICATION_MODE: slave
    POSTGRES_MASTER_HOST: postgres-primary
```

#### 3. Health Check Monitoring

**Configure external monitoring:**
```yaml
# Add healthcheck endpoint monitoring
# Integrate with: Datadog, New Relic, Prometheus, etc.
```

### Scaling

#### Horizontal Scaling

**Scale application instances:**
```bash
# Docker Compose
docker-compose up -d --scale app=3

# Docker Swarm
docker service scale aipanelist_app=3

# Kubernetes
kubectl scale deployment aipanelist-app --replicas=3
```

#### Vertical Scaling

**Increase resources:**
```yaml
services:
  app:
    deploy:
      resources:
        limits:
          cpus: '8'
          memory: 8G
        reservations:
          cpus: '4'
          memory: 4G
```

### Monitoring and Alerting

#### 1. Prometheus Integration

**Add Prometheus scraping:**
```yaml
prometheus:
  image: prom/prometheus:latest
  volumes:
    - ./prometheus.yml:/etc/prometheus/prometheus.yml
  ports:
    - "9090:9090"
```

**prometheus.yml:**
```yaml
scrape_configs:
  - job_name: 'aipanelist'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['app:8080']
```

#### 2. Grafana Dashboards

**Add Grafana:**
```yaml
grafana:
  image: grafana/grafana:latest
  environment:
    GF_SECURITY_ADMIN_PASSWORD: admin
  ports:
    - "3000:3000"
  volumes:
    - grafana-data:/var/lib/grafana
```

#### 3. Log Aggregation

**ELK Stack integration:**
```yaml
elasticsearch:
  image: elasticsearch:8.11.0
  environment:
    - discovery.type=single-node

logstash:
  image: logstash:8.11.0
  volumes:
    - ./logstash.conf:/usr/share/logstash/pipeline/logstash.conf

kibana:
  image: kibana:8.11.0
  ports:
    - "5601:5601"
```

### Disaster Recovery

#### 1. Automated Backups

**Cron job for daily backups:**
```bash
# Add to crontab
0 2 * * * cd /path/to/project && make backup-db
0 3 * * * cd /path/to/project && docker run --rm -v aipanelist_upload-data:/data -v /backups:/backup alpine tar czf /backup/uploads-$(date +\%Y\%m\%d).tar.gz /data
```

#### 2. Backup Retention

**Implement backup rotation:**
```bash
#!/bin/bash
# Keep last 7 daily, 4 weekly, 12 monthly backups
find /backups -name "backup_*.sql" -mtime +7 -delete
```

#### 3. Disaster Recovery Testing

**Test restore procedure monthly:**
```bash
# 1. Stop services
docker-compose down

# 2. Restore database
make restore-db FILE=backups/backup_latest.sql

# 3. Restore uploads
docker run --rm -v aipanelist_upload-data:/data -v /backups:/backup alpine tar xzf /backup/uploads-latest.tar.gz -C /

# 4. Start services
docker-compose up -d

# 5. Verify
make health
```

## Backup and Restore

### Database Backup

#### Automated Backup (Makefile)

```bash
# Create backup
make backup-db

# Backup is saved to: backups/backup_YYYYMMDD_HHMMSS.sql
```

#### Manual Backup

```bash
# Using docker-compose
docker-compose exec -T postgres pg_dump -U aipanelist aipanelist > backup.sql

# With compression
docker-compose exec -T postgres pg_dump -U aipanelist aipanelist | gzip > backup.sql.gz

# Backup specific tables
docker-compose exec -T postgres pg_dump -U aipanelist -t documents -t analysis_reports aipanelist > partial_backup.sql
```

#### Scheduled Backups

**Linux/macOS (cron):**
```bash
# Edit crontab
crontab -e

# Add daily backup at 2 AM
0 2 * * * cd /path/to/aipanelist && make backup-db

# Add weekly backup with cleanup
0 3 * * 0 cd /path/to/aipanelist && make backup-db && find backups/ -name "*.sql" -mtime +30 -delete
```

**Windows (Task Scheduler):**
```powershell
# Create scheduled task
$action = New-ScheduledTaskAction -Execute "cmd.exe" -Argument "/c cd C:\path\to\aipanelist && docker-compose exec -T postgres pg_dump -U aipanelist aipanelist > backups\backup_%date:~-4,4%%date:~-10,2%%date:~-7,2%.sql"
$trigger = New-ScheduledTaskTrigger -Daily -At 2am
Register-ScheduledTask -Action $action -Trigger $trigger -TaskName "AIPanelist Backup" -Description "Daily database backup"
```

### Database Restore

#### Restore from Backup (Makefile)

```bash
# Restore specific backup file
make restore-db FILE=backups/backup_20240115_020000.sql
```

#### Manual Restore

```bash
# Stop application first
docker-compose stop app

# Restore database
docker-compose exec -T postgres psql -U aipanelist aipanelist < backup.sql

# Or from compressed backup
gunzip -c backup.sql.gz | docker-compose exec -T postgres psql -U aipanelist aipanelist

# Restart application
docker-compose start app
```

#### Restore to New Database

```bash
# Create new database
docker-compose exec postgres psql -U aipanelist -c "CREATE DATABASE aipanelist_restored;"

# Restore to new database
docker-compose exec -T postgres psql -U aipanelist aipanelist_restored < backup.sql

# Switch to restored database (update docker-compose.yml)
# POSTGRES_DB: aipanelist_restored
```

### Upload Data Backup

#### Backup Uploaded Files

```bash
# Create tar archive of upload volume
docker run --rm \
  -v aipanelist_upload-data:/data \
  -v $(pwd)/backups:/backup \
  alpine tar czf /backup/uploads-$(date +%Y%m%d).tar.gz /data

# Verify backup
tar -tzf backups/uploads-20240115.tar.gz | head
```

#### Restore Uploaded Files

```bash
# Stop application
docker-compose stop app

# Restore from tar archive
docker run --rm \
  -v aipanelist_upload-data:/data \
  -v $(pwd)/backups:/backup \
  alpine tar xzf /backup/uploads-20240115.tar.gz -C /

# Start application
docker-compose start app
```

### Complete System Backup

#### Full Backup Script

```bash
#!/bin/bash
# full-backup.sh

BACKUP_DIR="backups/$(date +%Y%m%d_%H%M%S)"
mkdir -p "$BACKUP_DIR"

echo "Starting full system backup..."

# 1. Backup database
echo "Backing up database..."
docker-compose exec -T postgres pg_dump -U aipanelist aipanelist | gzip > "$BACKUP_DIR/database.sql.gz"

# 2. Backup upload data
echo "Backing up upload data..."
docker run --rm \
  -v aipanelist_upload-data:/data \
  -v $(pwd)/$BACKUP_DIR:/backup \
  alpine tar czf /backup/uploads.tar.gz /data

# 3. Backup configuration
echo "Backing up configuration..."
cp .env "$BACKUP_DIR/env.backup"
cp docker-compose.yml "$BACKUP_DIR/"
cp -r nginx "$BACKUP_DIR/" 2>/dev/null || true

# 4. Create backup manifest
echo "Creating manifest..."
cat > "$BACKUP_DIR/manifest.txt" <<EOF
Backup Date: $(date)
Database Size: $(du -h "$BACKUP_DIR/database.sql.gz" | cut -f1)
Upload Data Size: $(du -h "$BACKUP_DIR/uploads.tar.gz" | cut -f1)
Docker Version: $(docker --version)
Compose Version: $(docker-compose --version)
EOF

echo "Backup completed: $BACKUP_DIR"
```

#### Full Restore Script

```bash
#!/bin/bash
# full-restore.sh

if [ -z "$1" ]; then
  echo "Usage: ./full-restore.sh <backup_directory>"
  exit 1
fi

BACKUP_DIR="$1"

echo "Starting full system restore from $BACKUP_DIR..."

# 1. Stop services
echo "Stopping services..."
docker-compose down

# 2. Restore configuration
echo "Restoring configuration..."
cp "$BACKUP_DIR/env.backup" .env
cp "$BACKUP_DIR/docker-compose.yml" .
cp -r "$BACKUP_DIR/nginx" . 2>/dev/null || true

# 3. Start database only
echo "Starting database..."
docker-compose up -d postgres
sleep 10

# 4. Restore database
echo "Restoring database..."
gunzip -c "$BACKUP_DIR/database.sql.gz" | docker-compose exec -T postgres psql -U aipanelist aipanelist

# 5. Restore upload data
echo "Restoring upload data..."
docker run --rm \
  -v aipanelist_upload-data:/data \
  -v $(pwd)/$BACKUP_DIR:/backup \
  alpine tar xzf /backup/uploads.tar.gz -C /

# 6. Start all services
echo "Starting all services..."
docker-compose up -d

# 7. Verify
echo "Verifying restore..."
sleep 30
curl -f http://localhost:8080/actuator/health || echo "Health check failed!"

echo "Restore completed!"
```

### Backup Best Practices

1. **3-2-1 Rule**
   - 3 copies of data
   - 2 different storage types
   - 1 offsite backup

2. **Backup Schedule**
   - Daily: Database and uploads
   - Weekly: Full system backup
   - Monthly: Long-term archive

3. **Backup Testing**
   - Test restore monthly
   - Verify backup integrity
   - Document restore procedures

4. **Backup Storage**
   - Use separate storage from production
   - Encrypt sensitive backups
   - Implement retention policy

5. **Monitoring**
   - Alert on backup failures
   - Track backup sizes
   - Monitor backup duration

### Cloud Backup Integration

#### AWS S3

```bash
# Install AWS CLI
apt-get install awscli

# Upload backup to S3
aws s3 cp backups/backup_20240115.sql.gz s3://my-bucket/aipanelist/backups/

# Automated S3 sync
aws s3 sync backups/ s3://my-bucket/aipanelist/backups/ --exclude "*" --include "*.sql.gz"
```

#### Azure Blob Storage

```bash
# Install Azure CLI
curl -sL https://aka.ms/InstallAzureCLIDeb | sudo bash

# Upload backup
az storage blob upload \
  --account-name mystorageaccount \
  --container-name backups \
  --name backup_20240115.sql.gz \
  --file backups/backup_20240115.sql.gz
```

#### Google Cloud Storage

```bash
# Install gcloud CLI
curl https://sdk.cloud.google.com | bash

# Upload backup
gsutil cp backups/backup_20240115.sql.gz gs://my-bucket/aipanelist/backups/
```

## Architecture

### System Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                         Client Layer                         │
│  (Web Browser, API Client, Mobile App, CLI Tools)          │
└────────────────────────┬────────────────────────────────────┘
                         │ HTTPS/HTTP
                         ▼
┌─────────────────────────────────────────────────────────────┐
│                    Nginx Reverse Proxy                       │
│  • SSL/TLS Termination    • Rate Limiting                   │
│  • Load Balancing         • Request Routing                 │
│  • Static Content Cache   • Security Headers                │
└────────────────────────┬────────────────────────────────────┘
                         │ HTTP
                         ▼
┌─────────────────────────────────────────────────────────────┐
│                   Application Layer (8080)                   │
│                      Spring Boot 3.2.1                       │
│  ┌─────────────────────────────────────────────────────┐   │
│  │              REST API Controllers                    │   │
│  │  • Document Upload    • Status Check                │   │
│  │  • Results Retrieval  • Health Endpoints            │   │
│  └──────────────────┬──────────────────────────────────┘   │
│                     │                                        │
│  ┌─────────────────▼──────────────────────────────────┐   │
│  │           Async Processing Service                  │   │
│  │  • Thread Pool Management (10-20 threads)           │   │
│  │  • Task Queue (100 capacity)                        │   │
│  │  • Progress Tracking                                │   │
│  └──────────────────┬──────────────────────────────────┘   │
│                     │                                        │
│  ┌─────────────────▼──────────────────────────────────┐   │
│  │          Panel Orchestrator                         │   │
│  │  • Agent Coordination  • Workflow Management        │   │
│  │  • Error Handling      • Retry Logic                │   │
│  └──────────────────┬──────────────────────────────────┘   │
│                     │                                        │
│  ┌─────────────────▼──────────────────────────────────┐   │
│  │              AI Agent Panel                         │   │
│  │  ┌──────────────────────────────────────────────┐  │   │
│  │  │ Quick Screener    │ Fact Extractor          │  │   │
│  │  │ Literature Rev.   │ Methodology Reviewer    │  │   │
│  │  │ General Analyst   │ Lead Analyst            │  │   │
│  │  └──────────────────────────────────────────────┘  │   │
│  └──────────────────┬──────────────────────────────────┘   │
│                     │                                        │
│  ┌─────────────────▼──────────────────────────────────┐   │
│  │          Consensus Engine                           │   │
│  │  • Report Aggregation  • Conflict Resolution        │   │
│  │  • Final Report Generation                          │   │
│  └──────────────────┬──────────────────────────────────┘   │
│                     │                                        │
│  ┌─────────────────▼──────────────────────────────────┐   │
│  │        Document Processing Pipeline                 │   │
│  │  • PDF Extraction    • Text Chunking (100KB)       │   │
│  │  • Metadata Parsing  • Storage Management           │   │
│  └─────────────────────────────────────────────────────┘   │
└────────────┬────────────────────────┬─────────────────────┘
             │ JDBC                   │ HTTPS
             ▼                        ▼
┌────────────────────────┐  ┌──────────────────────────────┐
│  PostgreSQL 15 (5432)  │  │    NVIDIA API Integration    │
│  • Documents           │  │  • Model: meta/llama-3.1-    │
│  • Chunks              │  │    405b-instruct             │
│  • Analysis Reports    │  │  • Rate Limiting             │
│  • Consensus Reports   │  │  • Retry Logic               │
│  • Agent Progress      │  │  • Error Handling            │
│  • Extracted Docs      │  └──────────────────────────────┘
└────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│                      Persistent Storage                      │
│  • postgres-data: Database files                            │
│  • upload-data: Uploaded PDFs and extracted content         │
│  • pgadmin-data: PgAdmin configuration (dev only)           │
│  • maven-cache: Maven dependencies (dev only)               │
└─────────────────────────────────────────────────────────────┘
```

### Component Interaction Flow

```
1. Document Upload Flow:
   Client → Nginx → API Controller → Upload Service → Storage
                                   → Database (Document record)
                                   → Async Processing Queue

2. Processing Flow:
   Async Service → Panel Orchestrator → AI Agents (parallel)
                                      → NVIDIA API (6 agents)
                → Database (progress tracking)
                → Consensus Engine
                → Database (final report)

3. Results Retrieval Flow:
   Client → Nginx → API Controller → Results Service
                                   → Database (query reports)
                                   → Response (JSON)

4. Health Check Flow:
   Client → Nginx → Actuator Endpoint → Health Indicators
                                      → Database ping
                                      → Disk space check
                                      → Response (UP/DOWN)
```

### Deployment Modes Comparison

| Feature | Default Production | Development | Production + Nginx |
|---------|-------------------|-------------|-------------------|
| JVM Heap | 512MB-2GB | 512MB-2GB | 1GB-4GB |
| Logging | INFO | TRACE | WARN |
| Hot Reload | ❌ | ✅ | ❌ |
| Debug Port | ❌ | ✅ (5005) | ❌ |
| PgAdmin | ❌ | ✅ (5050) | ❌ |
| SSL/TLS | ❌ | ❌ | ✅ |
| Rate Limiting | ❌ | ❌ | ✅ |
| PostgreSQL Tuning | Basic | Basic | Advanced |
| Resource Limits | 2 CPU, 2GB | None | 4 CPU, 4GB |
| Auto-restart | ✅ | ❌ | ✅ |
| Port | 8080 | 8080 | 80/443 |

### Technology Stack

**Application:**
- Java 21 (Eclipse Temurin)
- Spring Boot 3.2.1
- Spring Data JPA
- Spring Web
- Apache PDFBox 3.0.1
- Jackson JSON

**Database:**
- PostgreSQL 15 Alpine
- HikariCP Connection Pool

**Infrastructure:**
- Docker 20.10+
- Docker Compose V2
- Nginx Alpine (production)
- PgAdmin 4 (development)

**External Services:**
- NVIDIA API (meta/llama-3.1-405b-instruct)

**Build Tools:**
- Maven 3.9.6
- Multi-stage Docker builds

### Network Architecture

```
┌─────────────────────────────────────────────────────────┐
│                  Docker Network Bridge                   │
│                  (aipanelist-network)                    │
│                                                          │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐ │
│  │    Nginx     │  │     App      │  │  PostgreSQL  │ │
│  │  (optional)  │  │   (8080)     │  │   (5432)     │ │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘ │
│         │                 │                 │          │
│         │ HTTP            │ JDBC            │          │
│         └─────────────────┴─────────────────┘          │
│                                                          │
│  ┌──────────────┐                                       │
│  │   PgAdmin    │  (dev only)                          │
│  │   (5050)     │                                       │
│  └──────────────┘                                       │
└─────────────────────────────────────────────────────────┘
         │                 │                 │
         │ Port 80/443     │ Port 8080       │ Port 5432
         ▼                 ▼                 ▼
    Host Network      Host Network      Host Network
```

## Support

For issues and questions:

- **Documentation:** See main [README.md](README.md)
- **GitHub Issues:** Report bugs and request features
- **Docker Hub:** Official images (if published)
- **Community:** Discussions and support

### Useful Links

- [Docker Documentation](https://docs.docker.com/)
- [Docker Compose Documentation](https://docs.docker.com/compose/)
- [Spring Boot Docker Guide](https://spring.io/guides/topicals/spring-boot-docker/)
- [PostgreSQL Docker Hub](https://hub.docker.com/_/postgres)
- [NVIDIA API Documentation](https://build.nvidia.com/)
