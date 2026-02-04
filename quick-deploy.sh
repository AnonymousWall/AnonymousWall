#!/bin/bash
# Quick Deployment Reference for AnonymousWall Backend
# Usage: Run this script on the OCI instance to prepare and deploy

set -e

echo "========================================="
echo "AnonymousWall Backend - Quick Deploy"
echo "========================================="

# Step 1: Prepare directories
echo "[1/5] Preparing application directories..."
sudo mkdir -p /opt/anonymouswall/logs /opt/anonymouswall/wallet
# Set ownership for logs directory to match container user (UID 1001 - micronaut)
sudo chown 1001:1001 /opt/anonymouswall/logs
sudo chmod 755 /opt/anonymouswall/logs
# Wallet directory only needs read access (handled by root in container during init)
sudo chmod 755 /opt/anonymouswall/wallet
echo "✓ Directories prepared"

# Step 2: Extract deployment files
echo ""
echo "[2/5] Extracting deployment files..."
cd /opt/anonymouswall
if [ -f /tmp/anonymouswall-deploy.tar.gz ]; then
    tar xzf /tmp/anonymouswall-deploy.tar.gz
    echo "✓ Files extracted"
else
    echo "✗ ERROR: /tmp/anonymouswall-deploy.tar.gz not found"
    exit 1
fi

# Step 3: Verify Podman
echo ""
echo "[3/5] Verifying Podman installation..."
if ! command -v podman &> /dev/null; then
    echo "⚠ Podman not found. Installing..."
    sudo dnf install -y podman
    pip3 install podman-compose==1.0.6
    sudo systemctl enable --now podman.socket
fi
podman --version && podman-compose --version
echo "✓ Podman verified"

# Step 4: Verify .env file
echo ""
echo "[4/5] Verifying configuration..."
if [ ! -f .env ]; then
    echo "✗ ERROR: .env file not found!"
    echo "  Please create .env with required variables:"
    echo "  - JWT_GENERATOR_SIGNATURE_SECRET"
    echo "  - DATABASE_URL"
    echo "  - DATABASE_USER"
    echo "  - DATABASE_PASSWORD"
    exit 1
fi
echo "✓ Configuration file found"

# Step 5: Deploy application
echo ""
echo "[5/5] Deploying application..."
if [ -f ./deploy.sh ]; then
    chmod +x ./deploy.sh
    ./deploy.sh
else
    echo "✗ ERROR: deploy.sh not found!"
    exit 1
fi

echo ""
echo "========================================="
echo "Deployment complete!"
echo "========================================="
echo ""
echo "Verify deployment:"
echo "  - Check status: podman ps | grep anonymouswall"
echo "  - View logs:    podman logs -f anonymouswall-backend"
echo "  - Test health:  curl http://localhost:8080/health"
echo ""
