# Docker Compose Configurations Comparison

This document explains the differences between the three Docker Compose configurations available in this repository.

## Overview

| Configuration | Purpose | Database | Use Case | Production Parity |
|--------------|---------|----------|----------|-------------------|
| **docker-compose.yml** | Quick local development | MySQL 8.0 | Rapid iteration, quick starts | Low (~40%) |
| **docker-compose.local.yml** | Local testing | Oracle XE 21c | Pre-production testing | High (~95%) |
| **docker-compose.prod.yml** | Production deployment | Oracle ADB (cloud) | OCI deployment | 100% |

## docker-compose.yml - Quick Development

**File:** `docker-compose.yml`

### Purpose
Fast local development with minimal setup time and resource usage.

### Database
- MySQL 8.0
- Quick to start (10-15 seconds)
- Lightweight (~200MB RAM)
- SQL syntax differences from production

### When to Use
✅ Rapid development and iteration  
✅ Testing basic application logic  
✅ Quick feature prototyping  
✅ Limited system resources  
✅ Need fast startup times  

❌ Testing Oracle-specific SQL  
❌ Pre-production validation  
❌ Performance testing  

### Startup
```bash
# Quickest startup
docker compose up -d

# Services ready in ~30 seconds
```

### Pros
- ✅ Fast startup time
- ✅ Low resource usage
- ✅ Simple configuration
- ✅ Well-known MySQL
- ✅ Great for development

### Cons
- ❌ Different SQL dialect than production
- ❌ Different database engine
- ❌ May miss Oracle-specific issues
- ❌ Not representative of production

### Configuration
```yaml
database:
  image: mysql:8.0
  environment:
    MYSQL_DATABASE: anonymous_wall
    MYSQL_USER: anonymouswall
    MYSQL_PASSWORD: anonymouswall

app:
  environment:
    DATABASE_URL: jdbc:mysql://db:3306/anonymous_wall
    DB_TYPE: mysql
    DB_DIALECT: MYSQL
    DB_DRIVER: com.mysql.cj.jdbc.Driver
```

---

## docker-compose.local.yml - Production Mimic

**File:** `docker-compose.local.yml`  
**Documentation:** [LOCAL_TESTING.md](LOCAL_TESTING.md)

### Purpose
Local testing that mimics production behavior for pre-deployment validation.

### Database
- Oracle Database Express Edition (XE) 21c
- Same engine as production Oracle ADB
- Takes 2-3 minutes to initialize
- Requires ~2GB RAM
- 100% Oracle SQL compatibility

### When to Use
✅ Pre-production testing  
✅ Validating Oracle-specific SQL  
✅ Testing migration scripts  
✅ Performance testing  
✅ Integration testing  
✅ Final validation before deployment  

❌ Quick iterations  
❌ Simple feature development  
❌ Limited system resources  

### Startup
```bash
# Automated setup
./start-local-test.sh

# Or manual
docker compose -f docker-compose.local.yml up -d

# First time: 2-3 minutes for Oracle initialization
# Subsequent starts: ~60 seconds
```

### Pros
- ✅ **Same database engine as production (Oracle)**
- ✅ **Same SQL dialect**
- ✅ **Same JDBC driver**
- ✅ **Catches Oracle-specific issues**
- ✅ High confidence before deployment
- ✅ Comprehensive documentation
- ✅ Convenience scripts included

### Cons
- ❌ Slower startup (2-3 min first time)
- ❌ Higher resource usage (~2GB RAM)
- ❌ Requires Oracle image download
- ❌ Larger disk footprint (~8GB)

### Configuration
```yaml
oracle-db:
  image: container-registry.oracle.com/database/express:21.3.0-xe
  environment:
    ORACLE_PWD: OraclePass123!
    ORACLE_CHARACTERSET: AL32UTF8
  shm_size: 1gb

app:
  environment:
    DATABASE_URL: jdbc:oracle:thin:@oracle-db:1521/XEPDB1
    DB_TYPE: oracle
    DB_DIALECT: ORACLE
    DB_DRIVER: oracle.jdbc.OracleDriver
```

### Key Differences from Development
| Aspect | Development (MySQL) | Local Testing (Oracle) |
|--------|-------------------|----------------------|
| SQL Dialect | MySQL | Oracle |
| Sequences | AUTO_INCREMENT | SEQUENCE |
| String Concat | CONCAT() | \|\| operator |
| Dual Table | Not needed | Required for SELECT |
| Case Sensitivity | Varies | Uppercase by default |
| Date Functions | NOW() | SYSDATE |
| Limit Clause | LIMIT/OFFSET | FETCH FIRST/OFFSET |

---

## docker-compose.prod.yml - Production Deployment

**File:** `docker-compose.prod.yml`  
**Documentation:** [DEPLOYMENT.md](DEPLOYMENT.md)

### Purpose
Production deployment on OCI (Oracle Cloud Infrastructure).

### Database
- Oracle Autonomous Database (ADB)
- Managed cloud service
- Auto-scaling
- Automatic backups
- mTLS security (requires wallet)
- High availability

### When to Use
✅ Production deployment  
✅ OCI infrastructure  
✅ High availability required  
✅ Auto-scaling needed  
✅ Managed database preferred  

❌ Local development  
❌ Local testing  

### Deployment
```bash
# On OCI instances
podman-compose -f docker-compose.prod.yml up -d

# Requires:
# - OCI infrastructure deployed
# - ADB wallet configured
# - Environment variables set
```

### Configuration
```yaml
app:
  image: anonymouswall-backend:latest  # Pre-built image
  environment:
    DATABASE_URL: ${DATABASE_URL}  # ADB connection string
    DATABASE_USER: ${DATABASE_USER}
    DATABASE_PASSWORD: ${DATABASE_PASSWORD}
    DB_TYPE: oracle
    DB_DIALECT: ORACLE
  volumes:
    - /opt/anonymouswall/wallet:/app/wallet  # ADB mTLS wallet
```

### Key Features
- ✅ Connects to cloud ADB
- ✅ Uses pre-built Docker images
- ✅ mTLS security with wallet
- ✅ No local database container
- ✅ Production-grade configuration
- ✅ Optimized for OCI

---

## Comparison Matrix

### Resource Requirements

| Metric | docker-compose.yml | docker-compose.local.yml | docker-compose.prod.yml |
|--------|-------------------|-------------------------|------------------------|
| **CPU** | ~10% | ~20% | Varies (OCI) |
| **Memory** | ~700MB | ~2.5GB | Varies (OCI) |
| **Disk** | ~2GB | ~10GB | Minimal (cloud DB) |
| **Startup Time** | 30 seconds | 2-3 minutes | 60 seconds |
| **Network** | Bridge | Bridge | OCI VCN |

### Database Features

| Feature | MySQL (dev) | Oracle XE (local) | Oracle ADB (prod) |
|---------|------------|------------------|------------------|
| **SQL Dialect** | MySQL | Oracle | Oracle |
| **JDBC Driver** | MySQL Connector | Oracle JDBC | Oracle JDBC |
| **Sequences** | AUTO_INCREMENT | SEQUENCE | SEQUENCE |
| **PL/SQL** | No | Yes | Yes |
| **Scalability** | Single instance | Single instance | Auto-scaling |
| **Backups** | Manual | Manual | Automatic |
| **HA/DR** | No | No | Yes |
| **Encryption** | Optional | Optional | Always (mTLS) |

### Development Workflow

| Stage | Recommended Configuration | Why |
|-------|--------------------------|-----|
| **Active Development** | docker-compose.yml | Fast iteration, low resources |
| **Feature Complete** | docker-compose.local.yml | Validate Oracle compatibility |
| **Pre-Deployment** | docker-compose.local.yml | Final testing before prod |
| **Staging** | docker-compose.prod.yml | Test in prod-like environment |
| **Production** | docker-compose.prod.yml | Deploy to OCI |

---

## Recommended Usage

### For Daily Development
```bash
# Use MySQL for speed
docker compose up -d

# Develop features
# Write tests
# Iterate quickly
```

### Before Merging to Main
```bash
# Stop MySQL environment
docker compose down

# Start Oracle environment
./start-local-test.sh

# Run full test suite
mvn clean test

# Test critical flows manually
# Verify database migrations
# Check Oracle-specific SQL
```

### Before Deploying
```bash
# Ensure local Oracle tests pass
./start-local-test.sh
# ... run tests ...

# Deploy to staging (OCI)
# ... use docker-compose.prod.yml ...

# Final validation in staging
# Deploy to production
```

---

## Migration Path

### From MySQL to Oracle Testing

If you've been developing with MySQL and want to test with Oracle:

1. **Save your work**
   ```bash
   docker compose down
   # Volumes are preserved
   ```

2. **Start Oracle environment**
   ```bash
   ./start-local-test.sh
   ```

3. **Check for issues**
   - SQL syntax differences
   - Case sensitivity
   - Sequence usage
   - Date/time functions

4. **Fix any Oracle-specific issues**
   ```bash
   # Make fixes
   # Test
   # Repeat
   ```

5. **Return to MySQL for development**
   ```bash
   ./stop-local-test.sh
   docker compose up -d
   ```

---

## Summary

### Choose docker-compose.yml when:
- 🚀 You need speed
- 💻 You're actively developing
- 📦 Resources are limited
- 🎯 Testing basic functionality

### Choose docker-compose.local.yml when:
- ✅ You need production parity
- 🔍 Testing Oracle-specific features
- 🚢 Preparing for deployment
- 🛡️ Final validation needed

### Use docker-compose.prod.yml for:
- ☁️ Production deployment
- 🏢 OCI infrastructure
- 🔐 Enterprise requirements
- 📈 High availability needs

---

## Questions?

- **How do I switch between configurations?**  
  Stop one with `docker compose down`, start another with `docker compose -f <file> up -d`

- **Can I run them simultaneously?**  
  No, they use the same ports (8080, 6379). Stop one before starting another.

- **Which should I use for CI/CD?**  
  Use docker-compose.local.yml for CI testing to match production.

- **Do I need both?**  
  MySQL (docker-compose.yml) is optional but recommended for faster development. Oracle (docker-compose.local.yml) is essential for pre-production testing.

For more details, see:
- [LOCAL_TESTING.md](LOCAL_TESTING.md) - Oracle local testing guide
- [DEPLOYMENT.md](DEPLOYMENT.md) - Production deployment guide
- [README.md](README.md) - General documentation
