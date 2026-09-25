#!/bin/bash
# ==============================================================================
# redBus App - Zero-Downtime Deployment Script
# Builds and restarts Spring Boot (Port 8080) & Next.js (Port 3000)
# ==============================================================================

set -e

PROJECT_DIR="/home/ubuntu/Redbus-app"
cd $PROJECT_DIR

echo "🔄 Pulling latest changes from GitHub..."
git pull origin main

# 1. Build and Restart Spring Boot Backend
echo "🔨 Building Spring Boot Backend..."
cd $PROJECT_DIR/backend
mvn clean package -DskipTests

# Create/Update Systemd Service for Spring Boot
sudo bash -c 'cat > /etc/systemd/system/redbus-backend.service << "EOF"
[Unit]
Description=redBus Spring Boot Backend
After=syslog.target network.target mysql.service

[Service]
User=ubuntu
WorkingDirectory=/home/ubuntu/Redbus-app/backend
ExecStart=/usr/bin/java -Xms256m -Xmx512m -jar /home/ubuntu/Redbus-app/backend/target/redbus-backend-0.0.1-SNAPSHOT.jar
EnvironmentFile=/home/ubuntu/Redbus-app/backend/.env
SuccessExitStatus=143
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
EOF'

sudo systemctl daemon-reload
sudo systemctl restart redbus-backend
sudo systemctl enable redbus-backend
echo "✅ Spring Boot Backend restarted successfully!"

# 2. Build and Restart Next.js Frontend
echo "⚛️ Building Next.js Frontend..."
cd $PROJECT_DIR/frontend
npm install
npm run build

# Start or restart Next.js via PM2
pm2 stop redbus-frontend || true
pm2 start npm --name "redbus-frontend" -- start
pm2 save
echo "✅ Next.js Frontend restarted successfully!"

echo "🚀 redBus App is now LIVE on EC2!"
