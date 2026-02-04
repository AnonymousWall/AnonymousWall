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

## Podman Operations (OCI Production)

```bash
# Build image
podman build -t anonymouswall-backend:latest .

# Start production config
podman-compose -f docker-compose.prod.yml up -d

# View logs
podman logs -f anonymouswall-backend

# Stop application
podman-compose -f docker-compose.prod.yml down

# Restart
podman-compose -f docker-compose.prod.yml restart
```

## Local Development with Docker

```bash
# Build image
docker build -t anonymouswall-backend:latest .

# Start full stack (local dev with MySQL)
docker-compose up -d

# View logs
docker logs -f anonymouswall-backend

# Stop application
docker-compose down
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
podman logs -f anonymouswall-backend

# Restart application
podman-compose -f docker-compose.prod.yml restart

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
podman exec anonymouswall-backend curl http://localhost:8080/health
```

## Troubleshooting

```bash
# Check container status
podman ps

# View recent logs
podman logs --tail 100 anonymouswall-backend

# Check environment variables
podman exec anonymouswall-backend env | grep -E 'DATABASE|JWT|REDIS'

# Check port binding
netstat -tlnp | grep 8080

# Check firewall
sudo firewall-cmd --list-all

# Rebuild and restart
podman-compose -f docker-compose.prod.yml down
podman build -t anonymouswall-backend:latest .
podman-compose -f docker-compose.prod.yml up -d
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
podman exec -it anonymouswall-backend sh
echo $DATABASE_URL

# Verify credentials
podman exec -it anonymouswall-backend sh
# Inside container, check environment
```

**Health check fails:**
```bash
# Check application logs
podman logs anonymouswall-backend

# Test internally
podman exec anonymouswall-backend curl http://localhost:8080/health
```

## Maintenance

```bash
# Update to latest version
cd /opt/anonymouswall
git pull
podman-compose -f docker-compose.prod.yml down
podman build -t anonymouswall-backend:latest .
podman-compose -f docker-compose.prod.yml up -d

# Clean up old images
podman image prune -a

# Clean up volumes
podman volume prune

# Backup logs
tar czf logs-backup-$(date +%Y%m%d).tar.gz /opt/anonymouswall/logs/
```

## Monitoring

```bash
# CPU and memory usage
podman stats anonymouswall-backend

# Disk usage
df -h

# Container health
podman inspect anonymouswall-backend | grep Health -A 10

# Application metrics (if enabled)
curl http://localhost:8080/metrics
```

## Support

- **Deployment Guide**: [DEPLOYMENT.md](DEPLOYMENT.md)
- **Application README**: [README.md](README.md)
- **Infrastructure Docs**: [AnonymousWallInfra](https://github.com/AnonymousWall/AnonymousWallInfra)
