# Alternative Deployment Methods

This document describes alternative ways to deploy AnonymousWall beyond the recommended Docker approach.

## Method 1: Systemd Service (Direct JAR)

For those who prefer running the JAR directly without Docker.

### Prerequisites
- Java 21 or higher installed on the system
- MySQL and Redis running (locally or remotely)

### Step 1: Build the Application

```bash
cd /opt/anonymouswall
mvn clean package -DskipTests
```

### Step 2: Create Environment File

```bash
cat > /opt/anonymouswall/app.env << 'EOF'
MICRONAUT_ENVIRONMENTS=prod
JWT_GENERATOR_SIGNATURE_SECRET=your-secret-key-min-32-chars
DATABASE_URL=jdbc:mysql://your-db-host:3306/anonymous_wall
DATABASE_USER=admin
DATABASE_PASSWORD=YourDatabasePassword
REDIS_URI=redis://localhost:6379
LOG_DIR=/var/log/anonymouswall
EOF

chmod 600 /opt/anonymouswall/app.env
```

### Step 3: Create Systemd Service

```bash
sudo tee /etc/systemd/system/anonymouswall.service > /dev/null << 'EOF'
[Unit]
Description=AnonymousWall Backend Service
After=network.target

[Service]
Type=simple
User=opc
Group=opc
WorkingDirectory=/opt/anonymouswall

# Environment file
EnvironmentFile=/opt/anonymouswall/app.env

# Run the application
ExecStart=/usr/bin/java -jar /opt/anonymouswall/target/anonymouswall-0.1.jar

# Restart policy
Restart=on-failure
RestartSec=10s

# Logging
StandardOutput=journal
StandardError=journal
SyslogIdentifier=anonymouswall

# Resource limits (adjust as needed)
LimitNOFILE=65536
LimitNPROC=4096

[Install]
WantedBy=multi-user.target
EOF
```

### Step 4: Enable and Start Service

```bash
# Reload systemd
sudo systemctl daemon-reload

# Enable service to start on boot
sudo systemctl enable anonymouswall.service

# Start the service
sudo systemctl start anonymouswall.service

# Check status
sudo systemctl status anonymouswall.service

# View logs
sudo journalctl -u anonymouswall.service -f
```

### Step 5: Configure Firewall

```bash
sudo firewall-cmd --permanent --add-port=8080/tcp
sudo firewall-cmd --reload
```

### Management Commands

```bash
# Start service
sudo systemctl start anonymouswall.service

# Stop service
sudo systemctl stop anonymouswall.service

# Restart service
sudo systemctl restart anonymouswall.service

# Check status
sudo systemctl status anonymouswall.service

# View logs (last 100 lines)
sudo journalctl -u anonymouswall.service -n 100

# Follow logs
sudo journalctl -u anonymouswall.service -f

# Check if enabled
sudo systemctl is-enabled anonymouswall.service
```

### Updating the Application

```bash
# Stop service
sudo systemctl stop anonymouswall.service

# Update code
cd /opt/anonymouswall
git pull

# Rebuild
mvn clean package -DskipTests

# Start service
sudo systemctl start anonymouswall.service
```

---

## Method 2: Screen/Tmux Session

For development or temporary deployments.

### Using Screen

```bash
# Install screen if not available
sudo yum install screen

# Create a screen session
screen -S anonymouswall

# Navigate to app directory
cd /opt/anonymouswall

# Set environment variables
export MICRONAUT_ENVIRONMENTS=prod
export JWT_GENERATOR_SIGNATURE_SECRET="your-secret"
export DATABASE_URL="jdbc:mysql://..."
export DATABASE_USER="admin"
export DATABASE_PASSWORD="password"

# Run the application
java -jar target/anonymouswall-0.1.jar

# Detach from screen: Press Ctrl+A then D

# Reattach to screen
screen -r anonymouswall

# List screens
screen -ls

# Kill screen session
screen -X -S anonymouswall quit
```

### Using Tmux

```bash
# Install tmux if not available
sudo yum install tmux

# Create a tmux session
tmux new -s anonymouswall

# Navigate and run (same as screen)
cd /opt/anonymouswall
export MICRONAUT_ENVIRONMENTS=prod
# ... set other variables ...
java -jar target/anonymouswall-0.1.jar

# Detach from tmux: Press Ctrl+B then D

# Reattach to tmux
tmux attach -t anonymouswall

# List sessions
tmux ls

# Kill session
tmux kill-session -t anonymouswall
```

---

## Method 3: Docker Without Compose

For those who prefer direct Docker commands.

### Build Image

```bash
cd /opt/anonymouswall
docker build -t anonymouswall-backend:latest .
```

### Run Container

```bash
docker run -d \
  --name anonymouswall-backend \
  --restart unless-stopped \
  -p 8080:8080 \
  -e MICRONAUT_ENVIRONMENTS=prod \
  -e JWT_GENERATOR_SIGNATURE_SECRET="your-secret" \
  -e DATABASE_URL="jdbc:mysql://your-db:3306/anonymous_wall" \
  -e DATABASE_USER="admin" \
  -e DATABASE_PASSWORD="password" \
  -e REDIS_URI="redis://your-redis:6379" \
  -v /opt/anonymouswall/logs:/app/logs \
  anonymouswall-backend:latest
```

### Management Commands

```bash
# View logs
docker logs -f anonymouswall-backend

# Stop container
docker stop anonymouswall-backend

# Start container
docker start anonymouswall-backend

# Restart container
docker restart anonymouswall-backend

# Remove container
docker rm -f anonymouswall-backend

# Check health
docker exec anonymouswall-backend curl http://localhost:8080/health
```

---

## Method 4: Kubernetes/K8s

For Kubernetes deployments.

### Deployment YAML

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: anonymouswall-backend
  labels:
    app: anonymouswall
spec:
  replicas: 2
  selector:
    matchLabels:
      app: anonymouswall
  template:
    metadata:
      labels:
        app: anonymouswall
    spec:
      containers:
      - name: backend
        image: anonymouswall-backend:latest
        imagePullPolicy: Always
        ports:
        - containerPort: 8080
          name: http
        env:
        - name: MICRONAUT_ENVIRONMENTS
          value: "prod"
        - name: JWT_GENERATOR_SIGNATURE_SECRET
          valueFrom:
            secretKeyRef:
              name: anonymouswall-secrets
              key: jwt-secret
        - name: DATABASE_URL
          valueFrom:
            secretKeyRef:
              name: anonymouswall-secrets
              key: database-url
        - name: DATABASE_USER
          valueFrom:
            secretKeyRef:
              name: anonymouswall-secrets
              key: database-user
        - name: DATABASE_PASSWORD
          valueFrom:
            secretKeyRef:
              name: anonymouswall-secrets
              key: database-password
        - name: REDIS_URI
          value: "redis://redis-service:6379"
        livenessProbe:
          httpGet:
            path: /health
            port: 8080
          initialDelaySeconds: 60
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /health
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 5
        resources:
          requests:
            memory: "512Mi"
            cpu: "500m"
          limits:
            memory: "1Gi"
            cpu: "1000m"
---
apiVersion: v1
kind: Service
metadata:
  name: anonymouswall-backend
spec:
  selector:
    app: anonymouswall
  ports:
  - protocol: TCP
    port: 8080
    targetPort: 8080
  type: LoadBalancer
---
apiVersion: v1
kind: Secret
metadata:
  name: anonymouswall-secrets
type: Opaque
stringData:
  jwt-secret: "your-secret-key-min-32-chars"
  database-url: "jdbc:mysql://your-db-host:3306/anonymous_wall"
  database-user: "admin"
  database-password: "YourDatabasePassword"
```

### Deploy to Kubernetes

```bash
# Create secret
kubectl create secret generic anonymouswall-secrets \
  --from-literal=jwt-secret="your-secret" \
  --from-literal=database-url="jdbc:mysql://..." \
  --from-literal=database-user="admin" \
  --from-literal=database-password="password"

# Deploy application
kubectl apply -f k8s-deployment.yaml

# Check status
kubectl get pods -l app=anonymouswall
kubectl get services anonymouswall-backend

# View logs
kubectl logs -f deployment/anonymouswall-backend

# Scale deployment
kubectl scale deployment/anonymouswall-backend --replicas=3
```

---

## Method 5: Standalone JAR (Development)

For quick testing without any service management.

```bash
# Set environment variables
export MICRONAUT_ENVIRONMENTS=prod
export JWT_GENERATOR_SIGNATURE_SECRET="test-secret-for-development-only"
export DATABASE_URL="jdbc:mysql://localhost:3306/anonymous_wall"
export DATABASE_USER="root"
export DATABASE_PASSWORD=""
export REDIS_URI="redis://localhost:6379"

# Build and run
cd /opt/anonymouswall
mvn clean package -DskipTests
java -jar target/anonymouswall-0.1.jar

# Or run in background with nohup
nohup java -jar target/anonymouswall-0.1.jar > app.log 2>&1 &

# Check process
ps aux | grep anonymouswall

# Kill process
pkill -f "anonymouswall-0.1.jar"
```

---

## Comparison

| Method | Pros | Cons | Use Case |
|--------|------|------|----------|
| **Docker Compose** | Easy, portable, includes dependencies | Requires Docker | Recommended for production |
| **Systemd Service** | Native Linux integration, auto-restart | Manual dependency management | Traditional Linux deployments |
| **Screen/Tmux** | Simple, interactive | Not production-ready | Development/debugging |
| **Docker CLI** | Fine-grained control | More commands to manage | Custom Docker setups |
| **Kubernetes** | Highly scalable, enterprise-ready | Complex setup | Large-scale deployments |
| **Standalone JAR** | Simplest, no overhead | No auto-restart, manual management | Quick testing |

---

## Recommendations

1. **Production (OCI)**: Use Docker Compose (recommended) or Systemd service
2. **Development**: Use Docker Compose or standalone JAR
3. **Enterprise**: Use Kubernetes
4. **Testing**: Use Screen/Tmux or standalone JAR

---

## Support

For the primary deployment method (Docker), see [DEPLOYMENT.md](DEPLOYMENT.md).
