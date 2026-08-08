# 🌾 FarmaTrade

**FarmaTrade** is an online agricultural marketplace that connects **farmers and buyers directly**. Farmers can list crops, buyers can participate in live auctions, and the platform manages **payments, delivery, cold storage, and invoicing** in one place.

Think of it as an **"OLX for farm crops"**, enhanced with live bidding, logistics management, automated billing, and cloud deployment.

---

## 🧩 Microservices Architecture

FarmaTrade is built using a **microservices architecture**, where each service is responsible for a specific business function. The services communicate over a shared Docker network and are deployed on an **AWS EC2 instance**.

* **auth-service — Port 8081**

  * Handles authentication and user registration.
  * Manages JWT tokens and user roles.
  * Supports **Farmer, Buyer, and Admin** roles.

* **lot-service — Port 8082**

  * Allows farmers to create and manage crop lots.
  * Stores crop quantity, price, and market-rate details.

* **bidding-service — Port 8083**

  * Provides live crop auctions.
  * Allows buyers to place bids in real time using **WebSockets**.

* **logistics-service — Port 8084**

  * Manages truck booking for crop delivery.
  * Finds nearby cold-storage facilities.
  * Performs weather-risk checks for transportation.

* **billing-service — Port 8085**

  * Generates invoices after successful sales.
  * Handles online payments through **Razorpay**.

* **frontend**

  * React-based web application.
  * Provides the user interface for **farmers, buyers, and administrators**.
  * Production build is served using **Nginx** on AWS EC2.

---

## 🛠️ Technology Stack

### Backend

* **Java 21**
* **Spring Boot**
* **Spring Security**
* **Spring Data JPA**
* **MySQL**
* **Flyway**

### Real-Time Communication

* **WebSockets**
* **STOMP**

### Payments

* **Razorpay Test/Production Integration**

### Frontend

* **React**
* **React Router**
* **Leaflet**

### DevOps & Cloud Infrastructure

* **Docker**
* **Docker Compose**
* **AWS EC2**
* **Nginx**
* **Linux/Ubuntu**
* **Git/GitHub**

---

## ☁️ AWS EC2 Deployment

FarmaTrade is deployed on an **AWS EC2 Ubuntu instance**.

The EC2 instance hosts:

* React production frontend
* Nginx web server
* Auth Service
* Lot Service
* Bidding Service
* Logistics Service
* Billing Service
* Individual MySQL containers for each microservice

The backend microservices run as Docker containers and communicate using the shared Docker network:

```text
farmatrade-net
```

The frontend production build is served through **Nginx**.

### Production Architecture

```text
                         Internet
                            │
                            ▼
                  ┌───────────────────┐
                  │     AWS EC2       │
                  │     Ubuntu        │
                  │                   │
                  │      Nginx        │
                  │       :80         │
                  └─────────┬─────────┘
                            │
                    React Frontend
                            │
          ┌─────────────────┼─────────────────┐
          │                 │                 │
          ▼                 ▼                 ▼
     Auth :8081        Lot :8082        Bidding :8083
          │                 │                 │
          │                 │          WebSockets
          │                 │                 │
          └────────────┬────┴─────────┬───────┘
                       │              │
                       ▼              ▼
                Logistics :8084   Billing :8085
                                      │
                                      ▼
                                  Razorpay
```

---

## 🔐 AWS EC2 Security

The EC2 instance is protected using an **AWS Security Group**.

Configured inbound access includes:

* **HTTP — Port 80**
* **HTTPS — Port 443**
* **SSH — Port 22**

Backend service ports are exposed for the current deployment/testing environment:

* **8081 — Auth**
* **8082 — Lot**
* **8083 — Bidding**
* **8084 — Logistics**
* **8085 — Billing**

> For production deployment, backend ports should preferably be restricted and exposed through Nginx/API Gateway rather than publicly exposing every microservice port.

---

## 🚀 Deployment on AWS EC2

### 1. Connect to EC2

```bash
ssh -i <your-key.pem> ubuntu@<EC2_PUBLIC_IP>
```

### 2. Clone the Repository

```bash
git clone <YOUR_GITHUB_REPOSITORY_URL>
cd farmatrade-main
```

### 3. Create Shared Docker Network

```bash
docker network create farmatrade-net
```

If the network already exists, continue with the next step.

### 4. Start Auth Service

```bash
cd auth-service
docker compose -f docker-compose.dev.yml up -d --build
```

Verify:

```bash
docker compose -f docker-compose.dev.yml ps
```

Test:

```bash
curl http://localhost:8081/actuator/health
```

### 5. Start Lot Service

```bash
cd ../lot-service
docker compose up -d --build
```

Verify:

```bash
docker compose ps
```

### 6. Start Bidding Service

```bash
cd ../bidding-service
docker compose -f docker-compose.dev.yml up -d --build
```

Verify:

```bash
docker compose -f docker-compose.dev.yml ps
```

### 7. Start Logistics Service

```bash
cd ../logistics-service
docker compose up -d --build
```

Verify:

```bash
docker compose ps
```

### 8. Start Billing Service

Billing Service requires Razorpay configuration.

Configure Razorpay credentials using environment variables or a `.env` file:

```bash
RAZORPAY_KEY_ID=<your_test_key_id>
RAZORPAY_KEY_SECRET=<your_test_key_secret>
RAZORPAY_WEBHOOK_SECRET=<your_webhook_secret>
```

Start Billing:

```bash
cd ../billing-service
docker compose up -d --build
```

Verify:

```bash
docker compose ps
```

---

## 💻 Frontend Deployment

The React application is built for production:

```bash
cd ../frontend
npm install
npm run build
```

The generated production files are copied to the Nginx web directory:

```bash
sudo rm -rf /var/www/farmatrade
sudo mkdir -p /var/www/farmatrade
sudo cp -r ~/farmatrade/frontend/build/* /var/www/farmatrade/
```

Nginx is configured to serve the React application:

```bash
sudo nginx -t
sudo systemctl reload nginx
```

The application can then be accessed through the EC2 public IP:

```text
http://<EC2_PUBLIC_IP>
```

---

## 🔍 Verify Services on AWS EC2

Check all running containers:

```bash
docker ps
```

Expected application ports:

| Component         | Port |
| ----------------- | ---: |
| Frontend / Nginx  |   80 |
| Auth Service      | 8081 |
| Lot Service       | 8082 |
| Bidding Service   | 8083 |
| Logistics Service | 8084 |
| Billing Service   | 8085 |

Health checks:

```bash
curl http://localhost:8081/actuator/health
curl http://localhost:8082/actuator/health
curl http://localhost:8083/actuator/health
curl http://localhost:8084/actuator/health
curl http://localhost:8085/actuator/health
```

Some services may return `401` for Actuator endpoints when Spring Security protects the endpoint. In that case, verify the service through its Docker status and application logs.

---

## 🗄️ Database Configuration

Each backend service uses its own MySQL database.

| Service           | MySQL Host Port |
| ----------------- | --------------: |
| Auth Service      |            3307 |
| Logistics Service |            3308 |
| Billing Service   |            3309 |
| Lot Service       |            3310 |
| Bidding Service   |            3311 |

Each MySQL instance runs in its own Docker container, maintaining database isolation between microservices.

---

## 🔐 Environment Variables

Sensitive configuration is provided through environment variables rather than committed to source control.

Examples include:

```text
RAZORPAY_KEY_ID
RAZORPAY_KEY_SECRET
RAZORPAY_WEBHOOK_SECRET
INTERNAL_SERVICE_TOKEN
DB_USERNAME
DB_PASSWORD
AUTH_RSA_PRIVATE_KEY
AUTH_CORS_ALLOWED_ORIGINS
```

**Never commit actual credentials, private keys, database passwords, or API secrets to GitHub.**

---

## 📁 Project Structure

```text
farmatrade-main/
│
├── auth-service/
│   ├── src/
│   ├── Dockerfile
│   ├── docker-compose.dev.yml
│   └── secrets/
│
├── lot-service/
│   ├── src/
│   ├── Dockerfile
│   └── docker-compose.yml
│
├── bidding-service/
│   ├── src/
│   ├── Dockerfile
│   └── docker-compose.dev.yml
│
├── logistics-service/
│   ├── src/
│   ├── Dockerfile
│   └── docker-compose.yml
│
├── billing-service/
│   ├── src/
│   ├── Dockerfile
│   └── docker-compose.yml
│
├── frontend/
│   ├── src/
│   ├── public/
│   └── package.json
│
├── .gitignore
└── README.md
```

---

## 🔌 Service Communication

The backend services communicate through the shared Docker network:

```text
                       ┌─────────────────┐
                       │   Auth Service  │
                       │      :8081      │
                       └────────┬────────┘
                                │
                              JWT/JWKS
                                │
             ┌──────────────────┼──────────────────┐
             │                  │                  │
             ▼                  ▼                  ▼
     ┌──────────────┐   ┌──────────────┐   ┌──────────────┐
     │ Lot Service  │   │   Bidding    │   │  Logistics   │
     │    :8082     │   │   :8083      │   │    :8084     │
     └──────────────┘   └──────┬───────┘   └──────────────┘
                               │
                               ▼
                       ┌─────────────────┐
                       │ Billing Service │
                       │      :8085      │
                       └────────┬────────┘
                                │
                                ▼
                           Razorpay API

                       ┌─────────────────┐
                       │ React Frontend  │
                       │   Nginx :80     │
                       └─────────────────┘
                                │
                                ▼
                           AWS EC2
```

---

## 🔒 Security

FarmaTrade implements multiple security mechanisms:

* JWT-based authentication
* Role-based access control
* RSA-based JWT signing
* JWKS-based token validation
* Service-to-service authentication
* Environment-based configuration
* Docker network isolation
* Separate databases for each microservice
* AWS EC2 Security Groups
* Razorpay secure payment integration
* CORS configuration for frontend-to-backend communication

---

## 🌱 Key Features

* 👨‍🌾 Farmer registration and crop listing
* 🛒 Buyer registration and crop discovery
* 🔨 Live crop auctions
* ⚡ Real-time bidding using WebSockets
* 🧾 Automatic invoice generation
* 💳 Razorpay payment integration
* 🚚 Truck booking
* ❄️ Cold-storage discovery
* 🌦️ Weather-risk checking
* 🔐 JWT authentication and role-based authorization
* 🐳 Dockerized microservices
* ☁️ AWS EC2 cloud deployment
* 🌐 Nginx-based frontend deployment
* 🗄️ Independent database per microservice

---

## 🎯 Project Goal

FarmaTrade aims to provide a **transparent, technology-driven agricultural marketplace** that reduces dependency on traditional commission-agent systems and gives farmers and buyers a direct platform for trading agricultural produce.

---

## 👨‍💻 Project Architecture

**FarmaTrade — Full-Stack Agricultural Marketplace**

Built using:

**Java • Spring Boot • Spring Security • React • MySQL • WebSockets • Docker • Docker Compose • Razorpay • AWS EC2 • Nginx • Linux**
