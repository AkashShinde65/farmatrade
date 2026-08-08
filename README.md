# FarmaTrade
🌾 FarmaTrade

FarmaTrade is an online marketplace that connects farmers and buyers. Farmers list their crops, buyers bid on them like an auction, and the app takes care of payment, delivery, and cold storage — all in one place.

Think of it like an "OLX for farm crops," but with live bidding, delivery booking, and automatic invoices built in.

🧩 What this project is made of

FarmaTrade is not one single app. It is split into small services, where each service does one job. This style is called microservices. All services talk to each other over the network.

Service	What it does	Port
auth-service	Handles login, signup, and user roles (Farmer / Buyer / Admin). Every other service trusts this one to say "who is this user?"	8081
lot-service	Lets farmers create a "lot" (a batch of crop for sale) with details like quantity, price, and market rate.	8082
bidding-service	Runs the live auction. Buyers place bids in real time using WebSockets (like a live chat, but for bids).	8083
logistics-service	Books trucks to move the crop and finds cold storage if needed. Also checks weather risk.	8084
billing-service	Creates invoices and handles online payment (via Razorpay) once a sale is done.	8085
frontend	The website farmers and buyers actually use. Built with React.	3000

Each backend service is a separate Spring Boot (Java) application with its own database. The frontend is a separate React application.

🔑 How a user's identity works
A user logs in through auth-service and gets a secure token (JWT).
Every other service checks that token to know who is calling and what role they have (FARMER, BUYER, or ADMIN).
No other service is allowed to look directly into the auth database — they only trust the token.

This keeps login logic in one place instead of repeating it everywhere.

🔄 How a typical sale flows
A farmer creates a lot in lot-service (e.g., "500kg tomatoes, ₹20/kg").
bidding-service opens an auction for that lot. Buyers place live bids.
When bidding ends, the winning bid is recorded and the lot is marked sold.
billing-service generates an invoice and collects payment from the buyer.
logistics-service arranges a truck (and cold storage if needed) to deliver the crop to the buyer.
🛠️ Tech stack
Backend: Java, Spring Boot, Spring Security, Spring Data JPA, MySQL, Flyway (database migrations)
Real-time bidding: WebSockets (STOMP)
Payments: Razorpay
Frontend: React, React Router, Leaflet (maps)
Containers: Docker & Docker Compose
🚀 Running the project

Each service can be run individually, or together using Docker.

bash
# Clone the project
git clone <this-repo-url>
cd farmatrade

# Start everything with Docker Compose
docker-compose up --build

To run a single backend service on its own (for example auth-service):

bash
cd auth-service
./mvnw spring-boot:run

To run the frontend on its own:

bash
cd frontend
npm install
npm start

Note: Each service also has its own docker-compose.dev.yml / README.md with more setup details (like environment variables and database setup).

📁 Project structure
farmatrade/
├── auth-service/        # Login, signup, roles, JWT
├── lot-service/          # Crop lot listings
├── bidding-service/       # Live auctions
├── logistics-service/     # Truck booking & cold storage
├── billing-service/        # Invoices & payments
├── frontend/                # React website
└── docker-compose.yml