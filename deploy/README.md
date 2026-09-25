# 🚀 AWS Free Tier Deployment Guide for redBus App

This guide explains how to deploy your **Frontend (Next.js)**, **Backend (Spring Boot)**, and **Database (MySQL)** onto an **AWS EC2 Free Tier (`t2.micro` or `t3.micro`)** instance with automatic GitHub Actions CI/CD and CrewAI automated reminders.

---

## 🏗️ Step 1: Launch EC2 Free Tier Instance

1. Go to [AWS EC2 Console](https://console.aws.amazon.com/ec2/).
2. Click **Launch instance**:
   - **Name:** `redbus-production`
   - **OS Image:** **Ubuntu Server 24.04 LTS (HVM)**
   - **Instance type:** `t2.micro` or `t3.micro` (**Free tier eligible**)
   - **Key pair:** Create or select an existing `.pem` key pair (e.g. `redbus-key.pem`).
3. **Network & Security Group Inbound Rules**:
   - `22` (SSH) - `Anywhere` (`0.0.0.0/0` or `My IP`)
   - `80` (HTTP) - `Anywhere` (`0.0.0.0/0`)
   - `3000` (Next.js Frontend) - `Anywhere` (`0.0.0.0/0`)
   - `8080` (Spring Boot Backend) - `Anywhere` (`0.0.0.0/0`)
4. Click **Launch instance**.

---

## ⚡ Step 2: One-Command EC2 Setup

Connect to your EC2 instance via SSH:
```bash
ssh -i "your-key.pem" ubuntu@<YOUR_EC2_PUBLIC_IP>
```

Run the automated setup script:
```bash
curl -fsSL https://raw.githubusercontent.com/Jaiganesh474/Redbus-app/main/deploy/ec2_setup.sh | bash
```

> **Why Swap Memory?** Free Tier instances have 1GB RAM. The setup script automatically creates a **2GB Swap space**, preventing out-of-memory crashes while building Next.js and running Spring Boot.

---

## 🔑 Step 3: Configure GitHub Actions Secrets

Go to your repository on GitHub:
👉 **Settings** -> **Secrets and variables** -> **Actions** -> **New repository secret**

Add these secrets:

| Secret Name | Description / Value |
| :--- | :--- |
| `EC2_HOST` | Your EC2 Public IPv4 Address (e.g., `13.233.xxx.xxx`) |
| `EC2_USERNAME` | `ubuntu` |
| `EC2_SSH_KEY` | Entire content of your `.pem` private key file |
| `GEMINI_API_KEY` | Your Google Gemini API Key |
| `SMTP_USERNAME` | Brevo / SMTP login username |
| `SMTP_PASSWORD` | Brevo / SMTP password |

---

## 🔄 Step 4: Automatic CI/CD & CrewAI

Every `git push origin main` will now automatically:
1. Compile and test the Spring Boot backend & Next.js frontend.
2. Deploy the latest code to EC2 with zero-downtime restarts.
3. Trigger the CrewAI Multi-Agent Journey Reminder & Dropping Point Planner.

Your application is live at:
- **Frontend:** `http://<YOUR_EC2_PUBLIC_IP>` (or `:3000`)
- **Backend API:** `http://<YOUR_EC2_PUBLIC_IP>/api` (or `:8080/api`)
