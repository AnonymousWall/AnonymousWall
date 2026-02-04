# Deployment Guide for AnonymousWall Backend

This guide explains how to deploy the AnonymousWall Micronaut backend on OCI (Oracle Cloud Infrastructure) instances.

## Overview

The application is deployed using Podman and podman-compose. The OCI infrastructure (from [AnonymousWallInfra](https://github.com/AnonymousWall/AnonymousWallInfra)) creates compute instances with:

- Podman and podman-compose pre-installed (via cloud-init)
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

### Pre-Deployment Checklist

Before deploying, ensure you have:

- [ ] OCI infrastructure deployed via Terraform
- [ ] `bastion_public_ip` from Terraform output
- [ ] `instance_private_ips` from Terraform output
- [ ] `adb_connection_strings` from Terraform output (for DATABASE_URL)
- [ ] `adb_wallet_content` from Terraform output (ADB wallet for mTLS connections)
- [ ] ADB admin password (set during Terraform deployment)
- [ ] SSH private key for OCI instances
- [ ] Generated JWT secret (min 32 characters): `openssl rand -base64 32`
- [ ] Confirmed port 8080 is open in OCI security lists
- [ ] Confirmed `/health` endpoint is configured in load balancer

**Note:** The OCI ADB infrastructure uses mTLS (mutual TLS) connections, which require a wallet for secure database access. The wallet is available via `terraform output adb_wallet_content` (base64 encoded).

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
# OCI Autonomous Database (ADB) uses Oracle Database with mTLS
# Connection URL format: jdbc:oracle:thin:@<tns-alias>?TNS_ADMIN=/path/to/wallet
#
# To use the wallet:
# 1. Extract wallet: terraform output -raw adb_wallet_content | base64 -d > wallet.zip
# 2. Unzip to a directory (e.g., /opt/anonymouswall/wallet)
# 3. Use TNS alias from the wallet's tnsnames.ora file
DATABASE_URL=jdbc:oracle:thin:@anonwalldb_high?TNS_ADMIN=/opt/anonymouswall/wallet
DATABASE_USER=ADMIN
DATABASE_PASSWORD=YourDatabasePassword

# Optional: Redis configuration (defaults to localhost:6379)
# REDIS_URI=redis://localhost:6379
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

# IMPORTANT: Verify Podman is installed
# The OCI cloud-init should have installed Podman, but verify:
podman --version
podman-compose --version

# If Podman is NOT installed, install it:
sudo dnf install -y podman
pip3 install podman-compose==1.0.6
sudo systemctl enable --now podman.socket

# Run deployment script
./deploy.sh
```

The script will:
- Build the container image using Podman
- Start the application with podman-compose
- Wait for health checks
- Display status and useful commands

#### Step 4: Verify Deployment

```bash
# Check if container is running
podman ps

# Check health endpoint
curl http://localhost:8080/health

# Expected response: {"status":"UP"}

# View logs
podman logs -f anonymouswall-backend

# Exit and test from outside
exit  # Exit from the instance

# Test via load balancer (get IP from terraform output)
LB_IP=$(cd ../AnonymousWallInfra && terraform output -raw load_balancer_ip)
curl http://$LB_IP/health

# Expected response: {"status":"UP"}
```

**Troubleshooting Failed Deployments:**

If the health check fails, check these common issues:

1. **Podman Not Installed:**
   ```bash
   # Check if Podman is installed
   podman --version
   podman-compose --version
   
   # If not installed, follow the installation steps in Step 3 above
   # After installation, ensure Podman socket is running:
   sudo systemctl status podman.socket
   ```

2. **Database Connection:**
   ```bash
   # Check if DATABASE_URL is correct
   podman exec anonymouswall-backend env | grep DATABASE
   
   # Check logs for database errors
   podman logs anonymouswall-backend | grep -i "database\|connection\|sql"
   ```

3. **Missing Environment Variables:**
   ```bash
   # Verify all required variables are set
   podman exec anonymouswall-backend env | grep -E "JWT_|DATABASE_|REDIS_"
   ```

4. **Container Issues:**
   ```bash
   # Check container status
   podman ps -a | grep anonymouswall
   
   # View full logs
   podman logs anonymouswall-backend
   
   # Restart container
   podman-compose -f docker-compose.prod.yml restart
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

# Extract and setup the ADB wallet (required for mTLS connections)
# Get the wallet from Terraform output (run this from your local machine first):
# terraform output -raw adb_wallet_content | base64 -d > wallet.zip

# On the OCI instance, create wallet directory and extract
mkdir -p /opt/anonymouswall/wallet
# Transfer wallet.zip to the instance, then:
unzip wallet.zip -d /opt/anonymouswall/wallet
chmod 600 /opt/anonymouswall/wallet/*

# Create .env file
# NOTE: TNS_ADMIN=/app/wallet is the container path
# The host path /opt/anonymouswall/wallet is mounted to /app/wallet in docker-compose.prod.yml
cat > .env << 'EOF'
JWT_GENERATOR_SIGNATURE_SECRET=your-secret-key-min-32-chars
DATABASE_URL=jdbc:oracle:thin:@anonwalldb_high?TNS_ADMIN=/app/wallet
DATABASE_USER=ADMIN
DATABASE_PASSWORD=YourDatabasePassword
# REDIS_URI=redis://localhost:6379  # Optional, defaults to localhost
EOF

# Note: DATABASE_URL connects to OCI Autonomous Database (ADB)
# ADB uses Oracle Database with mTLS, requiring the wallet
# The TNS alias (e.g., anonwalldb_high) can be found in wallet/tnsnames.ora
# Path mapping: host /opt/anonymouswall/wallet -> container /app/wallet

# Secure the .env file
chmod 600 .env
```

#### Step 5: Build and Deploy

```bash
# Build Docker image
podman build -t anonymouswall-backend:latest .

# Start application
podman-compose -f docker-compose.prod.yml up -d

# Check logs
podman logs -f anonymouswall-backend
```

## Configuration

### Environment Variable Summary

The application requires these environment variables for production deployment:

**REQUIRED:**
- `MICRONAUT_ENVIRONMENTS=prod` - Activates production profile
- `JWT_GENERATOR_SIGNATURE_SECRET` - JWT signing key (min 32 chars)
- `DATABASE_URL` - JDBC connection to OCI Autonomous Database
- `DATABASE_USER` - ADB username (typically `admin`)
- `DATABASE_PASSWORD` - ADB password (from Terraform)

**OPTIONAL (with defaults):**
- `REDIS_URI` - Redis connection (default: `redis://localhost:6379`)
- `DB_TYPE` - Database type (default: `oracle`)
- `DB_DIALECT` - SQL dialect (default: `ORACLE`)
- `DB_DRIVER` - JDBC driver (default: `oracle.jdbc.OracleDriver`)
- `LOG_DIR` - Log directory (default: `/app/logs`)
- `SMTP_*` - Email configuration (optional for email verification)

### Required Environment Variables

| Variable | Description | Example |
|----------|-------------|---------|
| `JWT_GENERATOR_SIGNATURE_SECRET` | JWT signing key (min 32 chars) | Generate with: `openssl rand -base64 32` |
| `DATABASE_URL` | JDBC connection string to OCI Autonomous Database (ADB) | `jdbc:oracle:thin:@your-adb-connection-string` |
| `DATABASE_USER` | ADB username | `ADMIN` |
| `DATABASE_PASSWORD` | ADB password | Complex password from Terraform |

**Note:** OCI Autonomous Database (ADB) uses Oracle Database, so we use the Oracle JDBC driver and protocol.

### Optional Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `REDIS_URI` | Redis connection string | `redis://localhost:6379` |
| `SMTP_HOST` | Email server host | - |
| `SMTP_PORT` | Email server port | `587` |
| `SMTP_USERNAME` | Email username | - |
| `SMTP_PASSWORD` | Email password | - |
| `LOG_DIR` | Log directory | `/app/logs` |

**Note:** Redis is optional with a sensible default. If not specified, the application will attempt to connect to Redis at `localhost:6379`.

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
podman-compose -f docker-compose.prod.yml down

# Rebuild image
podman build -t anonymouswall-backend:latest .

# Start updated application
podman-compose -f docker-compose.prod.yml up -d
```

## Monitoring and Maintenance

### View Logs

```bash
# Real-time logs
podman logs -f anonymouswall-backend

# Last 100 lines
podman logs --tail 100 anonymouswall-backend

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
podman-compose -f docker-compose.prod.yml restart
```

### Stop Application

```bash
cd /opt/anonymouswall
podman-compose -f docker-compose.prod.yml down
```

### Clean Up Old Images

```bash
# Remove unused images
podman image prune -a

# Remove unused volumes
podman volume prune
```

## Troubleshooting

### Podman Not Installed

If you get "podman: command not found" or "podman-compose: command not found":

```bash
# Check if Podman is installed
podman --version
podman-compose --version

# If not installed, install Podman on Oracle Linux 9:
sudo dnf install -y podman
pip3 install podman-compose==1.0.6

# Enable Podman socket
sudo systemctl enable --now podman.socket

# Verify installation
podman --version
podman-compose --version
```

**Note:** The OCI cloud-init script should install Podman automatically, but if the instance was created before cloud-init completed or if there was an issue, you'll need to install it manually.

### Application Won't Start

```bash
# Check logs
podman logs anonymouswall-backend

# Check if port is already in use
sudo netstat -tlnp | grep 8080

# Verify environment variables
podman exec anonymouswall-backend env | grep -E 'DATABASE|JWT'
```

### Health Check Failing

```bash
# Test health endpoint locally
podman exec anonymouswall-backend curl http://localhost:8080/health

# Check application logs
podman logs anonymouswall-backend | tail -50

# Verify database connectivity
podman exec anonymouswall-backend curl -v http://localhost:8080/health
```

### Database Connection Issues

```bash
# Test database connectivity from container
podman exec -it anonymouswall-backend sh
# Inside container, verify Oracle connection environment:
echo $DATABASE_URL

# Check OCI security lists allow traffic from backend subnet to ADB
# Verify database credentials in .env file
# Ensure ADB wallet is properly configured if using mTLS
```

### Load Balancer Can't Reach Backend

```bash
# Check if firewall allows port 8080
sudo firewall-cmd --list-all

# Verify application is listening on 8080
sudo netstat -tlnp | grep 8080

# Check Podman network
podman network inspect anonymouswall-network
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

1. **Build container image in CI pipeline**
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
podman logs -f anonymouswall-backend

# Restart application
podman-compose -f docker-compose.prod.yml restart

# Stop application
podman-compose -f docker-compose.prod.yml down

# Check health
curl http://localhost:8080/health

# Update application
git pull && ./deploy.sh
```
