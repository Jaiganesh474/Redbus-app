#!/bin/bash
# ==============================================================================
# redBus App - AWS EC2 Free Tier All-in-One Setup Script
# Installs: MySQL 8, OpenJDK 17, Node.js 20, PM2, Git, Nginx & Configures Swap
# ==============================================================================

set -e

echo "🚀 Starting AWS EC2 Setup for redBus App..."

# 1. Update and Upgrade Packages
sudo apt-get update && sudo apt-get upgrade -y

# 2. Setup 2GB Swap Memory (Crucial for EC2 t2.micro / t3.micro 1GB RAM)
if [ ! -f /swapfile ]; then
    echo "🧠 Configuring 2GB Swap Memory..."
    sudo fallocate -l 2G /swapfile
    sudo chmod 600 /swapfile
    sudo mkswap /swapfile
    sudo swapon /swapfile
    echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab
    echo "✅ Swap Memory configured successfully!"
fi

# 3. Install Java 17 / 21 & Maven
echo "☕ Installing Java & Maven..."
sudo apt-get install -y openjdk-17-jdk openjdk-21-jdk maven

# 4. Install Node.js 20 & PM2
echo "📦 Installing Node.js 20 & PM2..."
curl -fsSL https://deb.nodesource.com/setup_20.x | sudo -E bash -
sudo apt-get install -y nodejs
sudo npm install -g pm2

# 5. Install MySQL 8
echo "🗄️ Installing MySQL Server..."
sudo apt-get install -y mysql-server
sudo systemctl start mysql
sudo systemctl enable mysql

# Create redBus Database and User
sudo mysql -e "CREATE DATABASE IF NOT EXISTS redbus_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
sudo mysql -e "CREATE USER IF NOT EXISTS 'redbus_user'@'localhost' IDENTIFIED BY 'RedbusSecurePass2026!';"
sudo mysql -e "GRANT ALL PRIVILEGES ON redbus_db.* TO 'redbus_user'@'localhost';"
sudo mysql -e "FLUSH PRIVILEGES;"
echo "✅ MySQL Database 'redbus_db' created!"

# 6. Install & Configure Nginx
echo "🌐 Installing Nginx..."
sudo apt-get install -y nginx

# Nginx Configuration for Next.js (port 3000) & Spring Boot (port 8080)
sudo bash -c 'cat > /etc/nginx/sites-available/default << "EOF"
server {
    listen 80;
    server_name _;

    # Frontend (Next.js)
    location / {
        proxy_pass http://127.0.0.1:3000;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_set_header Host $host;
        proxy_cache_bypass $http_upgrade;
    }

    # Backend API (Spring Boot)
    location /api/ {
        proxy_pass http://127.0.0.1:8080/api/;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
EOF'

sudo nginx -t
sudo systemctl restart nginx
sudo systemctl enable nginx

echo "🎉 AWS EC2 Setup Complete! You are ready to deploy your Redbus App."
