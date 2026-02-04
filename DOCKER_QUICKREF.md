# Quick Reference: Docker Compose Configurations

## 🚀 Quick Start Commands

### MySQL Development (Fast)
```bash
docker compose up -d                    # Start
docker compose logs -f                  # Watch logs
docker compose down                     # Stop
```

### Oracle Local Testing (Production Mimic)
```bash
./start-local-test.sh                   # Automated setup
# or
docker compose -f docker-compose.local.yml up -d

./stop-local-test.sh                    # Interactive stop
```

### Production (OCI)
```bash
podman-compose -f docker-compose.prod.yml up -d
```

---

## 📊 Configuration Comparison

| Feature | MySQL (dev) | Oracle (local) | Oracle (prod) |
|---------|------------|---------------|---------------|
| **Startup** | 30 sec | 2-3 min | 60 sec |
| **Memory** | 700 MB | 2.5 GB | Varies |
| **Disk** | 2 GB | 10 GB | Minimal |
| **Prod Match** | 40% | 95% | 100% |
| **Best For** | Development | Testing | Production |

---

## 🎯 When to Use

### Use MySQL (`docker-compose.yml`)
- ✅ Daily development
- ✅ Quick iterations
- ✅ Feature prototyping
- ✅ Limited resources

### Use Oracle (`docker-compose.local.yml`)
- ✅ Pre-deployment testing
- ✅ Oracle SQL validation
- ✅ Migration testing
- ✅ Final QA

### Use Production (`docker-compose.prod.yml`)
- ✅ OCI deployment
- ✅ Staging environment
- ✅ Production

---

## 🔧 Common Commands

### Check Status
```bash
docker compose ps                       # MySQL
docker compose -f docker-compose.local.yml ps  # Oracle
```

### View Logs
```bash
docker compose logs -f app              # Application
docker compose logs -f db               # Database
```

### Access Database
```bash
# MySQL
docker exec -it anonymouswall-db mysql -u anonymouswall -p

# Oracle
docker exec -it anonymouswall-oracle-local sqlplus system/OraclePass123!@//localhost:1521/XEPDB1
```

### Health Check
```bash
curl http://localhost:8080/health
```

### Swagger UI
```bash
open http://localhost:8080/swagger-ui.html
```

---

## 🛑 Stop & Cleanup

### MySQL
```bash
docker compose stop                     # Stop (keep data)
docker compose down                     # Remove containers (keep data)
docker compose down -v                  # Remove everything
```

### Oracle
```bash
./stop-local-test.sh                    # Interactive options
# or
docker compose -f docker-compose.local.yml stop
docker compose -f docker-compose.local.yml down
docker compose -f docker-compose.local.yml down -v
```

---

## 📝 Port Reference

| Service | Port | URL |
|---------|------|-----|
| Application | 8080 | http://localhost:8080 |
| Swagger UI | 8080 | http://localhost:8080/swagger-ui.html |
| Health | 8080 | http://localhost:8080/health |
| MySQL | 3306 | localhost:3306 |
| Oracle | 1521 | localhost:1521/XEPDB1 |
| Oracle EM | 5500 | https://localhost:5500/em |
| Redis | 6379 | localhost:6379 |

---

## 🔐 Default Credentials

### MySQL
```
Host: localhost:3306
Database: anonymous_wall
User: anonymouswall
Password: anonymouswall
```

### Oracle
```
Host: localhost:1521
Service: XEPDB1
User: system
Password: OraclePass123!
```

### Redis
```
Host: localhost:6379
Password: (none)
```

---

## 📚 Documentation

- **[LOCAL_TESTING.md](LOCAL_TESTING.md)** - Full Oracle setup guide
- **[DOCKER_COMPOSE_COMPARISON.md](DOCKER_COMPOSE_COMPARISON.md)** - Detailed comparison
- **[DEPLOYMENT.md](DEPLOYMENT.md)** - Production deployment
- **[README.md](README.md)** - Main documentation

---

## ⚠️ Troubleshooting

### Port Already in Use
```bash
docker compose down
docker ps -a | grep anonymous
docker rm -f <container-id>
```

### Oracle Won't Start
```bash
# Check Docker resources (need 4GB+ RAM)
# Check disk space (need 10GB+)
docker compose -f docker-compose.local.yml logs oracle-db
```

### Can't Connect to Database
```bash
# Wait for health check
docker compose -f docker-compose.local.yml ps

# Check logs
docker compose -f docker-compose.local.yml logs -f
```

### Reset Everything
```bash
docker compose down -v
docker compose -f docker-compose.local.yml down -v
docker system prune -af
```

---

## 🚀 Recommended Workflow

1. **Develop** with MySQL (fast)
   ```bash
   docker compose up -d
   ```

2. **Test** with Oracle before merging
   ```bash
   docker compose down
   ./start-local-test.sh
   mvn test
   ```

3. **Deploy** to production
   ```bash
   # On OCI
   podman-compose -f docker-compose.prod.yml up -d
   ```

---

## 💡 Tips

- Use **MySQL** for daily work (fast, lightweight)
- Use **Oracle** before PRs (catch issues early)
- Keep both configs ready to switch quickly
- Run full tests on Oracle before deploying

---

**Need help?** See the full documentation in LOCAL_TESTING.md
