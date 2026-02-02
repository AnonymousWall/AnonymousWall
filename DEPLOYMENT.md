# Deployment Guide for AnonymousWall Backend

This guide explains how to deploy the AnonymousWall Micronaut backend on OCI (Oracle Cloud Infrastructure) instances.

## Overview

The application is deployed using Docker and Docker Compose. The OCI infrastructure (from [AnonymousWallInfra](https://github.com/AnonymousWall/AnonymousWallInfra)) creates compute instances with:

- Docker and Docker Compose pre-installed
- Firewall configured for port 8080
- Application directory at `/opt/anonymouswall`
- Load balancer with health checks on `/health`

## Prerequisites

1. **OCI Infrastructure Deployed**: Follow the [AnonymousWallInfra QUICKSTART.md](https://github.com/AnonymousWall/AnonymousWallInfra/blob/main/QUICKSTART.md)
2. **Terraform Outputs**: Get the following from `terraform output`:
   - `bastion_public_ip`: For SSH access
   - `instance_private_ips`: Backend instance IPs
   - `adb_connection_strings`: Database connection details
3. **SSH Key**: The private key used in Terraform deployment

## Deployment Methods

### Method 1: Automated Deployment (Recommended)

#### Step 1: Prepare Deployment Package

On your local machine:

```bash
# Clone the repository
git clone https://github.com/AnonymousWall/AnonymousWall.git
cd AnonymousWall

# Create the environment file
cp .env.example .env
nano .env  # Edit with your actual values
```

**Update `.env` with your values**:

```bash
# Generate a secure JWT secret
JWT_GENERATOR_SIGNATURE_SECRET=$(openssl rand -base64 32)

# Get from Terraform output: adb_connection_strings
# OCI Autonomous Database (ADB) is MySQL-compatible
DATABASE_URL=jdbc:mysql://your-adb-host:3306/anonymous_wall
DATABASE_USER=admin
DATABASE_PASSWORD=YourDatabasePassword

# If using separate Redis (optional - can use default)
REDIS_URI=redis://localhost:6379
```

#### Step 2: Transfer Files to OCI Instance

```bash
# Set variables
BASTION_IP=$(cd ../AnonymousWallInfra && terraform output -raw bastion_public_ip)
INSTANCE_IP=$(cd ../AnonymousWallInfra && terraform output -json instance_private_ips | jq -r '.[0]')
SSH_KEY=~/.ssh/oci_instance_key

# Create a deployment tarball
tar czf anonymouswall-deploy.tar.gz \
  Dockerfile \
  .dockerignore \
  docker-compose.prod.yml \
  deploy.sh \
  .env \
  pom.xml \
  mvnw \
  mvnw.bat \
  .mvn \
  src \
  aot-jar.properties \
  aot-native-image.properties \
  micronaut-cli.yml

# Transfer to bastion
scp -i $SSH_KEY anonymouswall-deploy.tar.gz opc@$BASTION_IP:/tmp/

# Transfer from bastion to backend instance
ssh -i $SSH_KEY opc@$BASTION_IP "scp /tmp/anonymouswall-deploy.tar.gz opc@$INSTANCE_IP:/tmp/"
```

#### Step 3: Deploy on OCI Instance

```bash
# SSH to the backend instance via bastion
ssh -i $SSH_KEY -J opc@$BASTION_IP opc@$INSTANCE_IP

# Extract files
sudo mkdir -p /opt/anonymouswall
sudo chown opc:opc /opt/anonymouswall
cd /opt/anonymouswall
tar xzf /tmp/anonymouswall-deploy.tar.gz

# Run deployment script
./deploy.sh
```

The script will:
- Build the Docker image
- Start the application
- Wait for health checks
- Display status and useful commands

#### Step 4: Verify Deployment

```bash
# Check if container is running
docker ps

# Check health endpoint
curl http://localhost:8080/health

# View logs
docker logs -f anonymouswall-backend

# Exit and test from outside
exit  # Exit from the instance

# Test via load balancer (get IP from terraform output)
LB_IP=$(cd ../AnonymousWallInfra && terraform output -raw load_balancer_ip)
curl http://$LB_IP/health
```

### Method 2: Manual Deployment

If you prefer manual control:

#### Step 1: SSH to Instance

```bash
ssh -i ~/.ssh/oci_instance_key -J opc@<bastion-ip> opc@<instance-ip>
```

#### Step 2: Prepare Application Directory

```bash
sudo mkdir -p /opt/anonymouswall
sudo chown opc:opc /opt/anonymouswall
cd /opt/anonymouswall
```

#### Step 3: Transfer Application Files

From your local machine:

```bash
# Option A: Using rsync through bastion
rsync -avz -e "ssh -i ~/.ssh/oci_instance_key -J opc@<bastion-ip>" \
  ./{Dockerfile,docker-compose.prod.yml,.dockerignore,pom.xml,mvnw,mvnw.bat,.mvn,src,*.properties,*.yml} \
  opc@<instance-ip>:/opt/anonymouswall/

# Option B: Using git (if repository is public or you have access)
git clone https://github.com/AnonymousWall/AnonymousWall.git .
```

#### Step 4: Configure Environment

```bash
cd /opt/anonymouswall

# Create .env file
cat > .env << 'EOF'
JWT_GENERATOR_SIGNATURE_SECRET=your-secret-key-min-32-chars
DATABASE_URL=jdbc:mysql://your-adb-host:3306/anonymous_wall
DATABASE_USER=admin
DATABASE_PASSWORD=YourDatabasePassword
REDIS_URI=redis://localhost:6379
EOF

# Note: DATABASE_URL connects to OCI Autonomous Database (ADB)
# ADB is MySQL-compatible, so we use the MySQL JDBC driver

# Secure the .env file
chmod 600 .env
```

#### Step 5: Build and Deploy

```bash
# Build Docker image
docker build -t anonymouswall-backend:latest .

# Start application
docker-compose -f docker-compose.prod.yml up -d

# Check logs
docker logs -f anonymouswall-backend
```

## Configuration

### Required Environment Variables

| Variable | Description | Example |
|----------|-------------|---------|
| `JWT_GENERATOR_SIGNATURE_SECRET` | JWT signing key (min 32 chars) | Generate with: `openssl rand -base64 32` |
| `DATABASE_URL` | JDBC connection string to OCI Autonomous Database (ADB) | `jdbc:mysql://adb-host:3306/anonymous_wall` |
| `DATABASE_USER` | ADB username | `admin` |
| `DATABASE_PASSWORD` | ADB password | Complex password from Terraform |

**Note:** OCI Autonomous Database (ADB) is MySQL-compatible, so we use the MySQL JDBC driver and protocol.

### Optional Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `REDIS_URI` | Redis connection string | `redis://localhost:6379` |
| `SMTP_HOST` | Email server host | - |
| `SMTP_PORT` | Email server port | `587` |
| `SMTP_USERNAME` | Email username | - |
| `SMTP_PASSWORD` | Email password | - |
| `LOG_DIR` | Log directory | `/app/logs` |

## Updating the Application

### Using Deployment Script

```bash
cd /opt/anonymouswall

# Pull latest changes (if using git)
git pull

# Or transfer new files via SCP
# ...

# Redeploy
./deploy.sh
```

### Manual Update

```bash
cd /opt/anonymouswall

# Stop current application
docker-compose -f docker-compose.prod.yml down

# Rebuild image
docker build -t anonymouswall-backend:latest .

# Start updated application
docker-compose -f docker-compose.prod.yml up -d
```

## Monitoring and Maintenance

### View Logs

```bash
# Real-time logs
docker logs -f anonymouswall-backend

# Last 100 lines
docker logs --tail 100 anonymouswall-backend

# Application logs (on host)
tail -f /opt/anonymouswall/logs/anonymouswall.log
```

### Check Health

```bash
# Local health check
curl http://localhost:8080/health

# Via load balancer
curl http://<load-balancer-ip>/health
```

### Restart Application

```bash
cd /opt/anonymouswall
docker-compose -f docker-compose.prod.yml restart
```

### Stop Application

```bash
cd /opt/anonymouswall
docker-compose -f docker-compose.prod.yml down
```

### Clean Up Old Images

```bash
# Remove unused images
docker image prune -a

# Remove unused volumes
docker volume prune
```

## Troubleshooting

### Application Won't Start

```bash
# Check logs
docker logs anonymouswall-backend

# Check if port is already in use
sudo netstat -tlnp | grep 8080

# Verify environment variables
docker exec anonymouswall-backend env | grep -E 'DATABASE|JWT'
```

### Health Check Failing

```bash
# Test health endpoint locally
docker exec anonymouswall-backend curl http://localhost:8080/health

# Check application logs
docker logs anonymouswall-backend | tail -50

# Verify database connectivity
docker exec anonymouswall-backend curl -v http://localhost:8080/health
```

### Database Connection Issues

```bash
# Test database connectivity from container
docker exec -it anonymouswall-backend sh
# Inside container:
curl -v jdbc:mysql://your-db-host:3306/

# Check OCI security lists allow traffic from backend subnet to database subnet
# Verify database credentials in .env file
```

### Load Balancer Can't Reach Backend

```bash
# Check if firewall allows port 8080
sudo firewall-cmd --list-all

# Verify application is listening on 8080
sudo netstat -tlnp | grep 8080

# Check Docker network
docker network inspect anonymouswall-network
```

## Security Best Practices

1. **Secrets Management**
   - Never commit `.env` file
   - Use OCI Vault for production secrets (future enhancement)
   - Rotate JWT secret periodically

2. **Network Security**
   - Backend instances have no public IPs (behind load balancer)
   - Access via bastion host only
   - Database in private subnet

3. **Container Security**
   - Application runs as non-root user
   - Minimal Alpine-based images
   - Regular security updates

4. **Access Control**
   - Limit SSH access to bastion (via `ssh_allowed_cidrs`)
   - Use SSH keys, not passwords
   - Audit logs regularly

## Scaling

### Horizontal Scaling

To add more backend instances:

```bash
# Update Terraform configuration
cd ../AnonymousWallInfra
nano terraform.tfvars  # Increase instance_count

# Apply changes
terraform apply

# Deploy to new instances following the same steps above
```

The load balancer will automatically distribute traffic to all healthy instances.

### Vertical Scaling

To increase instance resources:

```bash
# Update Terraform configuration
cd ../AnonymousWallInfra
nano terraform.tfvars  # Increase instance_ocpus and instance_memory_in_gbs

# Apply changes (will recreate instances)
terraform apply
```

## CI/CD Integration (Future)

For automated deployments, consider:

1. **Build Docker image in CI pipeline**
2. **Push to OCI Container Registry**
3. **Deploy via SSH or OCI DevOps**
4. **Run health checks**
5. **Rollback on failure**

## Support

- **Infrastructure Issues**: See [AnonymousWallInfra Troubleshooting](https://github.com/AnonymousWall/AnonymousWallInfra/blob/main/ARCHITECTURE.md#troubleshooting)
- **Application Issues**: Check application logs and README.md
- **OCI Documentation**: https://docs.oracle.com/en-us/iaas/

## Quick Reference

```bash
# Deploy application
./deploy.sh

# View logs
docker logs -f anonymouswall-backend

# Restart application
docker-compose -f docker-compose.prod.yml restart

# Stop application
docker-compose -f docker-compose.prod.yml down

# Check health
curl http://localhost:8080/health

# Update application
git pull && ./deploy.sh
```
