# Quick Reference - AnonymousWall Deployment

## Local Development

```bash
# Start application
MICRONAUT_ENVIRONMENTS=dev ./mvnw mn:run

# Run tests
mvn test

# Build JAR
mvn clean package
```

## Docker Operations

```bash
# Build image
docker build -t anonymouswall-backend:latest .

# Start full stack (local dev)
docker-compose up -d

# Start production config
docker-compose -f docker-compose.prod.yml up -d

# View logs
docker logs -f anonymouswall-backend

# Stop application
docker-compose down

# Restart
docker-compose restart
```

## OCI Deployment

```bash
# SSH to instance via bastion
ssh -i ~/.ssh/oci_instance_key -J opc@<bastion-ip> opc@<instance-ip>

# Deploy application
cd /opt/anonymouswall
./deploy.sh

# Check health
curl http://localhost:8080/health

# View logs
docker logs -f anonymouswall-backend

# Restart application
docker-compose -f docker-compose.prod.yml restart

# Update application
git pull
./deploy.sh
```

## Health Checks

```bash
# Local
curl http://localhost:8080/health

# Via load balancer
curl http://<load-balancer-ip>/health

# Detailed health (inside container)
docker exec anonymouswall-backend curl http://localhost:8080/health
```

## Troubleshooting

```bash
# Check container status
docker ps

# View recent logs
docker logs --tail 100 anonymouswall-backend

# Check environment variables
docker exec anonymouswall-backend env | grep -E 'DATABASE|JWT|REDIS'

# Check port binding
netstat -tlnp | grep 8080

# Check firewall
sudo firewall-cmd --list-all

# Rebuild and restart
docker-compose down
docker build -t anonymouswall-backend:latest .
docker-compose up -d
```

## Configuration

```bash
# Create environment file
cp .env.example .env

# Generate JWT secret
openssl rand -base64 32

# Edit configuration
nano .env
```

## Common Issues

**Port already in use:**
```bash
sudo lsof -i :8080
sudo kill -9 <PID>
```

**Database connection fails:**
```bash
# For local MySQL development:
telnet <db-host> 3306

# For production Oracle ADB, verify environment:
docker exec -it anonymouswall-backend sh
echo $DATABASE_URL

# Verify credentials
docker exec -it anonymouswall-backend sh
# Inside container, check environment
```

**Health check fails:**
```bash
# Check application logs
docker logs anonymouswall-backend

# Test internally
docker exec anonymouswall-backend curl http://localhost:8080/health
```

## Maintenance

```bash
# Update to latest version
cd /opt/anonymouswall
git pull
docker-compose -f docker-compose.prod.yml down
docker build -t anonymouswall-backend:latest .
docker-compose -f docker-compose.prod.yml up -d

# Clean up old images
docker image prune -a

# Clean up volumes
docker volume prune

# Backup logs
tar czf logs-backup-$(date +%Y%m%d).tar.gz /opt/anonymouswall/logs/
```

## Monitoring

```bash
# CPU and memory usage
docker stats anonymouswall-backend

# Disk usage
df -h

# Container health
docker inspect anonymouswall-backend | grep Health -A 10

# Application metrics (if enabled)
curl http://localhost:8080/metrics
```

## Support

- **Deployment Guide**: [DEPLOYMENT.md](DEPLOYMENT.md)
- **Application README**: [README.md](README.md)
- **Infrastructure Docs**: [AnonymousWallInfra](https://github.com/AnonymousWall/AnonymousWallInfra)
