# 🌾 FarmaTrade

**FarmaTrade** is an online agricultural marketplace that connects **farmers and buyers directly**. Farmers can list crops, buyers can participate in live auctions, and the platform manages **payments, delivery, cold storage, and invoicing** in one place.

Think of it as an **"OLX for farm crops"**, enhanced with live bidding, logistics management, and automated billing.

---

## 🧩 Microservices Architecture

FarmaTrade is built using a **microservices architecture**, where each service is responsible for a specific business function. The services communicate with each other over a shared Docker network.

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

* **frontend — Port 3000**

  * React-based web application.
  * Provides the user interface for **farmers, buyers, and administrators**.

Each backend service is an independent **Spring Boot application** with its own database.

---

## 🔑 Authentication & Authorization

Authentication is centralized through `auth-service`.

1. A user registers or logs in through `auth-service`.
2. The service generates a secure **JWT**.
3. Other microservices validate the JWT to identify the user.
4. Role-based access control determines whether the user is a **FARMER**, **BUYER**, or **ADMIN**.
5. Other services do not directly access the Auth database.

This keeps authentication and authorization centralized while allowing the other services to remain independent.

---

## 🔄 Typical Sale Flow

```text
Farmer
   │
   ▼
Lot Service
Create Crop Lot
   │
   ▼
Bidding Service
Live Auction
   │
   ▼
Winning Bid
   │
   ▼
Billing Service
Invoice + Payment
   │
   ▼
Logistics Service
Truck + Cold Storage
   │
   ▼
Crop Delivered
```

### Example

A farmer creates a lot:

> **500 kg Tomatoes — ₹20/kg**

The typical flow is:

1. `lot-service` creates the crop lot.
2. `bidding-service` opens the auction.
3. Buyers place live bids.
4. The highest bid wins.
5. `billing-service` generates the invoice and processes payment.
6. `logistics-service` arranges transportation and cold storage if required.
7. The crop is delivered to the buyer.

---

## 🛠️ Technology Stack

### Backend

* **Java**
* **Spring Boot**
* **Spring Security**
* **Spring Data JPA**
* **MySQL**
* **Flyway**

### Real-Time Communication

* **WebSockets**
* **STOMP**

### Payments

* **Razorpay**

### Frontend

* **React**
* **React Router**
* **Leaflet**

### DevOps & Infrastructure

* **Docker**
* **Docker Compose**

---

## 🚀 Running the Project

### Prerequisites

Make sure the following are installed:

* Java 21
* Maven
* Node.js
* npm
* Docker Desktop
* Git

---

## 🐳 Run Using Docker

FarmaTrade uses Docker Compose for running the individual microservices and their databases.

### 1. Clone the Repository

```bash
git clone <YOUR_GITHUB_REPOSITORY_URL>
cd farmatrade-main
```

---

### 2. Create the Shared Docker Network

Create the shared network once:

```bash
docker network create farmatrade-net
```

If the network already exists, you can continue.

---

## 🔐 3. Start Auth Service

Auth Service should be started first because the other services validate JWT tokens against it.

```bash
cd auth-service
docker compose -f docker-compose.dev.yml up -d --build
```

Check the service:

```bash
docker compose -f docker-compose.dev.yml ps
```

Verify the health endpoint:

```bash
curl http://localhost:8081/actuator/health
```

Expected response:

```json
{
  "status": "UP"
}
```

---

## 🌾 4. Start Lot Service

```bash
cd ../lot-service
docker compose up -d --build
```

Check:

```bash
docker compose ps
```

---

## 🔨 5. Start Bidding Service

```bash
cd ../bidding-service
docker compose -f docker-compose.dev.yml up -d --build
```

Check:

```bash
docker compose -f docker-compose.dev.yml ps
```

---

## 🚚 6. Start Logistics Service

```bash
cd ../logistics-service
docker compose up -d --build
```

Check:

```bash
docker compose ps
```

---

## 💳 7. Start Billing Service

Billing Service requires Razorpay configuration.

Set your **Razorpay Test Mode** credentials in your local environment:

```bash
export RAZORPAY_KEY_ID="your_test_key_id"
export RAZORPAY_KEY_SECRET="your_test_key_secret"
export RAZORPAY_WEBHOOK_SECRET="your_webhook_secret"
```

Then start Billing:

```bash
cd ../billing-service
docker compose up -d --build
```

Check:

```bash
docker compose ps
```

> **Important:** Do not commit Razorpay credentials, passwords, private keys, or other secrets to GitHub.

---

## 💻 8. Start Frontend

The frontend runs separately from Docker.

```bash
cd ../frontend
npm install
npm start
```

Open the application:

```text
http://localhost:3000
```

---

## 🔍 Verify All Services

From the project root:

```bash
cd ~/farmatrade-main
docker ps
```

The application services use the following ports:

* **Auth Service:** `8081`
* **Lot Service:** `8082`
* **Bidding Service:** `8083`
* **Logistics Service:** `8084`
* **Billing Service:** `8085`
* **Frontend:** `3000`

---

## 🔌 Service Communication

The backend services communicate through the shared `farmatrade-net` Docker network.

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
     │    :8082     │   │   Service    │   │   Service    │
     │              │   │    :8083     │   │    :8084     │
     └──────────────┘   └──────┬───────┘   └──────────────┘
                                │
                                ▼
                       ┌─────────────────┐
                       │ Billing Service │
                       │      :8085      │
                       └─────────────────┘

                       ┌─────────────────┐
                       │ React Frontend  │
                       │      :3000      │
                       └─────────────────┘
```

---

## 🗄️ Database Configuration

Each backend service uses its own MySQL database.

* **Auth Service MySQL:** Host port `3307`
* **Logistics Service MySQL:** Host port `3308`
* **Billing Service MySQL:** Host port `3309`
* **Lot Service MySQL:** Host port `3310`
* **Bidding Service MySQL:** Host port `3311`

This separation keeps each microservice's data independent.

---

## 🔐 Environment Variables

Sensitive configuration should be provided through environment variables rather than committed to source control.

Examples include:

```text
RAZORPAY_KEY_ID
RAZORPAY_KEY_SECRET
RAZORPAY_WEBHOOK_SECRET
INTERNAL_SERVICE_TOKEN
DB_USERNAME
DB_PASSWORD
AUTH_RSA_PRIVATE_KEY
```

For local development, configure these variables in your terminal or local `.env` files.

**Never commit actual secrets to GitHub.**

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

## 📌 Port Reference

* **Frontend:** `3000`
* **Auth Service:** `8081`
* **Lot Service:** `8082`
* **Bidding Service:** `8083`
* **Logistics Service:** `8084`
* **Billing Service:** `8085`

---

## 🔒 Security

FarmaTrade uses centralized authentication and role-based authorization.

* JWT-based authentication
* Role-based access control
* RSA-based JWT signing
* Service-to-service authentication
* Environment-based configuration
* Docker secrets for sensitive credentials
* Separate databases for each microservice

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
* 🗄️ Independent database per service

---

## 🎯 Project Goal

FarmaTrade aims to provide a **transparent, technology-driven agricultural marketplace** that reduces dependency on traditional commission-agent systems and gives farmers and buyers a direct platform for trading agricultural produce.

---

## 👨‍💻 Project Architecture

**FarmaTrade — Full-Stack Agricultural Marketplace**

Built using:

**Java • Spring Boot • Spring Security • React • MySQL • WebSockets • Docker • Docker Compose • Razorpay**
