# Docker Management

---

## Containers

The project runs two containers:

| Container | Image | Port | Purpose |
|-----------|-------|------|---------|
| `aipanelist-app` | Built from `Dockerfile` | `8080` | Spring Boot application |
| `aipanelist-postgres` | `postgres:15-alpine` | `5432` | PostgreSQL database |

Both containers are connected via a Docker bridge network named `aipanelist-network`.

---

## Volumes

Data is persisted in Docker named volumes so it survives container restarts:

| Volume | Mounted at | Contains |
|--------|-----------|---------|
| `postgres-data` | `/var/lib/postgresql/data` | All database data |
| `upload-data` | `/app/data` | Uploaded PDF files |

---

## Start and Stop

**Start the project:**
```cmd
start.bat
```

**Stop the project (containers preserved, data intact):**
```cmd
stop.bat
```

The difference between `stop` and `down`:
- `docker-compose stop` — pauses containers, keeps them and their data
- `docker-compose down` — removes containers (volumes and data are kept)
- `docker-compose down -v` — removes containers AND all volumes (full reset)

---

## Common Commands

### Viewing Logs

```bash
# Live logs from the application
docker-compose logs -f app

# Live logs from the database
docker-compose logs -f postgres

# Last 100 lines from the application
docker-compose logs --tail=100 app

# All logs since container started
docker-compose logs app
```

### Container Management

```bash
# Check container status and health
docker-compose ps

# Restart the application only (database stays running)
docker-compose restart app

# Remove containers (data preserved in volumes)
docker-compose down

# Full reset — removes containers AND all data
docker-compose down -v
```

### Rebuilding After Code Changes

```bash
# Rebuild and restart the application container
docker-compose up -d --build app
```

This triggers a full Maven build inside Docker. The first rebuild after a dependency change will be slow (Maven re-downloads changed dependencies). Subsequent rebuilds are faster.

### Accessing the Database

```bash
# Open a psql shell inside the postgres container
docker-compose exec postgres psql -U aipanelist -d aipanelist

# Run a single query
docker-compose exec postgres psql -U aipanelist -d aipanelist \
  -c "SELECT document_id, status, total_pages FROM documents ORDER BY uploaded_at DESC LIMIT 10;"
```

### Accessing the Application Container

```bash
# Open a shell inside the app container
docker-compose exec app /bin/sh

# Check application health from inside the container
docker-compose exec app wget -qO- http://localhost:8080/actuator/health
```

---

## Health Checks

Both containers have health checks configured:

**Application (`aipanelist-app`):**
```yaml
test: ["CMD", "wget", "--no-verbose", "--tries=1", "--spider", "http://localhost:8080/actuator/health"]
interval: 30s
timeout: 10s
retries: 3
start_period: 60s
```

**Database (`aipanelist-postgres`):**
```yaml
test: ["CMD-SHELL", "pg_isready -U aipanelist"]
interval: 10s
timeout: 5s
retries: 5
start_period: 10s
```

The application container waits for the database to be healthy before starting (`depends_on: condition: service_healthy`).

---

## Resource Limits

The application container has resource limits configured in `docker-compose.yml`:

| Resource | Limit | Reservation |
|----------|-------|-------------|
| CPU | 2 cores | 1 core |
| Memory | 2GB | 512MB |

Adjust these in `docker-compose.yml` if needed for your machine.

---

## Dockerfile

The application uses a multi-stage build:

**Stage 1 — Build:**
- Base: `maven:3.9-eclipse-temurin-21-alpine`
- Copies `pom.xml` and `src/`
- Runs `mvn clean package -DskipTests`

**Stage 2 — Runtime:**
- Base: `eclipse-temurin:21-jre-alpine`
- Creates a non-root user (`appuser`) for security
- Copies the built JAR from Stage 1
- Creates `/app/data/uploads` directory
- Exposes port 8080
- Runs with configurable `JAVA_OPTS`

The multi-stage build keeps the final image small — only the JRE and the application JAR, not Maven or the full JDK.
