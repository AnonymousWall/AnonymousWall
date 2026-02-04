# Local Testing Guide - Production Mimic Environment

This guide explains how to run a local testing environment that **100% mimics production behavior** using Oracle Database.

## Overview

The production environment uses **Oracle Autonomous Database (ADB)** on OCI. For local testing that truly mimics production, we use **Oracle Database Express Edition (XE)** in Docker, which provides the same Oracle Database engine, SQL dialect, and JDBC driver as production.

## Why Oracle for Local Testing?

✅ **True Production Parity**: Same database engine as production Oracle ADB  
✅ **Same SQL Dialect**: Oracle SQL syntax and features  
✅ **Same JDBC Driver**: `oracle.jdbc.OracleDriver`  
✅ **Same Behavior**: Sequences, triggers, PL/SQL compatibility  
✅ **Catches Oracle-specific Issues**: Before they reach production  

## Quick Start

### Prerequisites

- Docker or Podman
- Docker Compose or Podman Compose
- At least 4GB free RAM (Oracle needs resources)
- At least 10GB free disk space

### Option 1: Using Docker Compose (Recommended)

```bash
# 1. Clone the repository
git clone https://github.com/AnonymousWall/AnonymousWall.git
cd AnonymousWall

# 2. Start the environment
docker-compose -f docker-compose.local.yml up -d

# 3. Wait for Oracle to initialize (first time takes 2-3 minutes)
docker-compose -f docker-compose.local.yml logs -f oracle-db

# 4. Wait for the app to start and connect
docker-compose -f docker-compose.local.yml logs -f app

# 5. Check health
curl http://localhost:8080/health

# 6. Access the API
curl http://localhost:8080/swagger-ui.html
```

### Option 2: Using Podman Compose

```bash
# Same commands, just replace docker-compose with podman-compose
podman-compose -f docker-compose.local.yml up -d
podman-compose -f docker-compose.local.yml logs -f
```

### Option 3: Running Locally (Without Docker)

If you want to run the application locally but use Docker only for databases:

```bash
# 1. Start only the databases
docker-compose -f docker-compose.local.yml up -d oracle-db redis

# 2. Wait for Oracle to be ready
docker-compose -f docker-compose.local.yml logs -f oracle-db

# 3. Run the application locally
MICRONAUT_ENVIRONMENTS=local ./mvnw mn:run

# Or build and run JAR
mvn clean package
MICRONAUT_ENVIRONMENTS=local java -jar target/anonymouswall-0.1.jar
```

## Configuration

### Environment Variables

The setup uses `.env.local` for configuration. The default values are:

| Variable | Default Value | Description |
|----------|--------------|-------------|
| `JWT_GENERATOR_SIGNATURE_SECRET` | `LocalTestingSecretKey32CharsMin!!!` | JWT signing key (min 32 chars) |
| `DATABASE_URL` | `jdbc:oracle:thin:@oracle-db:1521/XEPDB1` | Oracle connection URL |
| `DATABASE_USER` | `system` | Oracle username |
| `DATABASE_PASSWORD` | `OraclePass123!` | Oracle password |
| `REDIS_URI` | `redis://redis:6379` | Redis connection |

### Custom Configuration

To customize the setup:

```bash
# Create your own .env file
cp .env.local .env

# Edit with your values
nano .env

# Start with your configuration
docker-compose -f docker-compose.local.yml --env-file .env up -d
```

## Services

The local environment includes:

### 1. Oracle Database XE (`oracle-db`)
- **Image**: `container-registry.oracle.com/database/express:21.3.0-xe`
- **Port**: 1521 (Oracle Listener)
- **Port**: 5500 (Enterprise Manager)
- **Database**: XEPDB1 (Pluggable Database)
- **Mimics**: Production Oracle ADB

### 2. Application (`app`)
- **Port**: 8080
- **Environment**: `local` (production-like settings)
- **Connects to**: Oracle DB and Redis

### 3. Redis (`redis`)
- **Image**: `redis:7-alpine`
- **Port**: 6379
- **Same as**: Production Redis

## First Time Setup

### 1. Pull Oracle Image

Oracle XE requires accepting the license:

```bash
# Login to Oracle Container Registry (if needed)
docker login container-registry.oracle.com
# Username: Your Oracle account email
# Password: Your Oracle account password

# Pull the image
docker pull container-registry.oracle.com/database/express:21.3.0-xe
```

**Note**: If you don't have an Oracle account, create one at https://profile.oracle.com

### 2. Initial Startup (First Time Only)

The first time you start Oracle XE, it needs to:
- Initialize the database files
- Create the XEPDB1 pluggable database
- Configure listeners and services

This takes **2-3 minutes**. Be patient! 

```bash
# Start and follow logs
docker-compose -f docker-compose.local.yml up -d oracle-db
docker-compose -f docker-compose.local.yml logs -f oracle-db

# Wait for this message:
# "#########################"
# "DATABASE IS READY TO USE!"
# "#########################"
```

### 3. Schema Creation

The application uses **Liquibase** for automatic schema management. On first connection, Liquibase will:
- Create all tables
- Set up indexes and constraints
- Initialize data

This happens automatically when the app starts.

## Verification

### Check Service Health

```bash
# Check all services
docker-compose -f docker-compose.local.yml ps

# All should show "healthy"
```

### Check Database Connection

```bash
# Connect to Oracle directly
docker exec -it anonymouswall-oracle-local sqlplus system/OraclePass123!@//localhost:1521/XEPDB1

# In SQL*Plus, run:
SQL> SELECT table_name FROM user_tables;
SQL> EXIT;
```

### Check Application Health

```bash
# Health endpoint
curl http://localhost:8080/health

# Should return:
# {"status":"UP"}
```

### Access API Documentation

Open in browser:
- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI Spec: http://localhost:8080/swagger/anonymouswall-0.0.yml

## Troubleshooting

### Oracle Container Won't Start

**Issue**: Container exits immediately

**Solution**: 
```bash
# Check logs
docker-compose -f docker-compose.local.yml logs oracle-db

# Common issues:
# 1. Not enough memory (need 4GB+)
# 2. Not enough disk space (need 10GB+)
# 3. Port 1521 already in use

# Fix port conflict:
docker-compose -f docker-compose.local.yml down
docker ps -a | grep oracle
docker rm -f <container-id>
```

### Application Can't Connect to Database

**Issue**: App logs show connection errors

**Solution**:
```bash
# 1. Ensure Oracle is fully started
docker-compose -f docker-compose.local.yml logs oracle-db | grep "READY TO USE"

# 2. Test connection manually
docker exec -it anonymouswall-oracle-local sqlplus system/OraclePass123!@//localhost:1521/XEPDB1

# 3. Check network
docker network ls
docker network inspect anonymouswall-network
```

### Slow Performance

**Issue**: Oracle is slow or unresponsive

**Solution**:
```bash
# Increase Docker resources in Docker Desktop:
# Settings → Resources → Memory: 6GB+
# Settings → Resources → Disk: 20GB+

# Or in Docker daemon config (/etc/docker/daemon.json):
{
  "default-shm-size": "1G"
}
```

### Reset Everything

```bash
# Stop all services
docker-compose -f docker-compose.local.yml down

# Remove volumes (deletes all data)
docker-compose -f docker-compose.local.yml down -v

# Start fresh
docker-compose -f docker-compose.local.yml up -d
```

## Differences from Production

While this setup closely mimics production, there are some differences:

| Aspect | Local Testing | Production (OCI) |
|--------|--------------|------------------|
| Database | Oracle XE 21c | Oracle ADB (19c/21c) |
| Connection | Direct JDBC | mTLS with Wallet |
| Scaling | Single instance | Auto-scaling |
| Backups | Manual | Automatic |
| Monitoring | Docker logs | OCI Monitoring |
| SSL/TLS | Optional | Required |

## Advanced Usage

### Connecting with SQL Developer

1. Download [Oracle SQL Developer](https://www.oracle.com/tools/downloads/sqldev-downloads.html)
2. Create new connection:
   - Name: Anonymous Wall Local
   - Username: `system`
   - Password: `OraclePass123!`
   - Hostname: `localhost`
   - Port: `1521`
   - Service name: `XEPDB1`

### Accessing Enterprise Manager

Oracle XE includes Enterprise Manager:
- URL: https://localhost:5500/em
- Username: `system`
- Password: `OraclePass123!`

### Custom SQL Scripts

Place `.sql` files in `init-scripts/` to run them on database initialization:

```sql
-- init-scripts/02-custom.sql
CREATE TABLE my_custom_table (
    id NUMBER GENERATED ALWAYS AS IDENTITY,
    name VARCHAR2(100),
    PRIMARY KEY (id)
);
```

### Running Tests

```bash
# Build the application
mvn clean package

# Run tests (uses H2 by default, not Oracle)
mvn test

# Run with local Oracle (if configured)
MICRONAUT_ENVIRONMENTS=local mvn test
```

## Stopping the Environment

```bash
# Stop all services (preserves data)
docker-compose -f docker-compose.local.yml stop

# Start again later
docker-compose -f docker-compose.local.yml start

# Stop and remove containers (preserves data volumes)
docker-compose -f docker-compose.local.yml down

# Stop and remove everything including data
docker-compose -f docker-compose.local.yml down -v
```

## Resource Usage

Expected resource usage:

| Service | CPU | Memory | Disk |
|---------|-----|--------|------|
| Oracle XE | 10-20% | 1.5-2GB | 8GB |
| Application | 5-10% | 500MB | 100MB |
| Redis | <1% | 50MB | 100MB |
| **Total** | **~20%** | **~2.5GB** | **~8GB** |

## Next Steps

- ✅ Environment is running
- ✅ Database is initialized
- ✅ Application is healthy

You can now:
1. Test API endpoints via Swagger UI
2. Register test users
3. Create posts and test functionality
4. Run integration tests
5. Debug issues before deploying to production

## Support

For issues or questions:
- Check [README.md](README.md) for API documentation
- Check [DEPLOYMENT.md](DEPLOYMENT.md) for production deployment
- Review application logs: `docker-compose -f docker-compose.local.yml logs app`
- Review database logs: `docker-compose -f docker-compose.local.yml logs oracle-db`

---

**Remember**: This is a local testing environment. Never use these credentials or settings in production!
