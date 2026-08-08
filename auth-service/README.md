# FarmaTrade Auth Service

P1 Auth Service owns FarmaTrade user registration, authentication, JWT issuance, profile lookup, Admin user management, and security audit records.

## Technology Stack

- Java 21
- Spring Boot 3.3
- Spring Web, Security, Data JPA, Validation, Actuator
- MySQL 8
- Flyway
- Nimbus JOSE JWT for RS256 JWT/JWKS
- Maven
- Docker

## Folder Structure

```text
auth-service/
├── Dockerfile
├── docker-compose.dev.yml
├── pom.xml
├── .env.example
├── postman_collection.json
├── postman_environment.json
├── INTEGRATION.md
└── src/
    ├── main/java/com/farmatrade/auth/
    │   ├── config/
    │   ├── controller/
    │   ├── dto/
    │   ├── entity/
    │   ├── exception/
    │   ├── repository/
    │   ├── security/
    │   └── service/
    └── main/resources/db/migration/
```

## Prerequisites

- Java 21+
- Maven 3.9+
- MySQL 8+
- Docker, optional for container build and Testcontainers

## Local MySQL Setup

Create a local database and user:

```sql
CREATE DATABASE farmatrade_auth_db;
CREATE USER 'farmatrade_auth_user'@'%' IDENTIFIED BY 'replace-with-local-password';
GRANT ALL PRIVILEGES ON farmatrade_auth_db.* TO 'farmatrade_auth_user'@'%';
FLUSH PRIVILEGES;
```

## Environment Setup

Use `.env.example` as the placeholder reference. Do not commit `.env`, private keys, generated tokens, real passwords, or real Aadhaar values.

Required production-style values:

- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`
- `AADHAAR_HASH_PEPPER`
- `AUTH_RSA_PRIVATE_KEY_FILE` or `AUTH_RSA_PRIVATE_KEY_PEM`
- `AUTH_JWT_KID`
- `AUTH_JWT_ISSUER`
- `AUTH_JWT_AUDIENCE`
- `AUTH_JWT_ACCESS_TOKEN_MINUTES`
- `AUTH_CORS_ALLOWED_ORIGINS`
- `AUTH_LOGIN_MAX_FAILED_ATTEMPTS`
- `AUTH_LOGIN_LOCK_MINUTES`

## RSA Development Keys

Generate temporary development keys outside Git:

```sh
mkdir -p secrets
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out secrets/dev-auth-private.pem
openssl rsa -in secrets/dev-auth-private.pem -pubout -out secrets/dev-auth-public.pem
```

The service can derive the public key from a PKCS#8 private key. `AUTH_RSA_PUBLIC_KEY_FILE` and `AUTH_RSA_PUBLIC_KEY_PEM` are optional.

## Build And Test

```sh
mvn clean verify
```

The MySQL Testcontainers test runs when Docker is available. If Docker is not running, that test is skipped by Testcontainers.

## Local Run

```sh
export SERVER_PORT=8081
export DB_URL='jdbc:mysql://localhost:3306/farmatrade_auth_db?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC'
export DB_USERNAME=farmatrade_auth_user
export DB_PASSWORD=replace-with-local-password
export AADHAAR_HASH_PEPPER=replace-with-at-least-32-random-characters
export AUTH_RSA_PRIVATE_KEY_FILE="$PWD/secrets/dev-auth-private.pem"
mvn spring-boot:run
```

Swagger UI: `http://localhost:8081/swagger-ui.html`

Health: `http://localhost:8081/actuator/health`

## Docker

Build:

```sh
docker build -t farmatrade-auth-service:latest .
```

Run with environment variables:

```sh
docker run --rm -p 8081:8081 \
  -e DB_URL='jdbc:mysql://mysql:3306/farmatrade_auth_db?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC' \
  -e DB_USERNAME=farmatrade_auth_user \
  -e DB_PASSWORD=replace-with-password \
  -e AADHAAR_HASH_PEPPER=replace-with-at-least-32-random-characters \
  -e AUTH_RSA_PRIVATE_KEY_FILE=/run/secrets/farmatrade_auth_private_key.pem \
  farmatrade-auth-service:latest
```

Development Compose is available in `docker-compose.dev.yml`. It creates only Auth Service and MySQL and does not modify the root FarmaTrade Compose file.

## Endpoint Summary

Public:

- `POST /api/auth/register/farmer`
- `POST /api/auth/register/buyer`
- `POST /api/auth/login/farmer`
- `POST /api/auth/login/buyer`
- `POST /api/auth/login/admin`
- `GET /.well-known/jwks.json`
- `GET /actuator/health`
- `GET /swagger-ui.html`

Authenticated:

- `GET /api/auth/me`

ADMIN only:

- `POST /api/auth/register/admin`
- `GET /api/admin/users`
- `PATCH /api/admin/users/{id}/status`
- `GET /api/admin/audit-events`

## Security Design

- Passwords are validated for strength and stored with BCrypt.
- Aadhaar-format values are validated with Verhoeff checksum and stored only as HMAC-SHA-256 hashes.
- JWTs are RS256 signed with issuer `farmatrade-auth-service`, audience `farmatrade-api`, and 15 minute expiry.
- Tokens include `sub`, `role`, `email`, `iat`, `exp`, and `jti`.
- Tokens never include passwords, Aadhaar values, Aadhaar hashes, peppers, or private keys.
- Failed login lockout is configurable and defaults to 5 attempts and 15 minutes.
- Service security is stateless; server-side HTTP sessions, form login, HTTP Basic, and logout are disabled.
- Admins cannot disable themselves or the last active Admin.

## Mock Aadhaar Disclaimer

Aadhaar handling is format/checksum validation only. It is not UIDAI eKYC, government identity verification, or proof that a number belongs to a person. Use only mock checksum-valid values in development and tests.

## Initial Admin Bootstrap

When no Admin exists, the first Admin can be created at startup from:

- `INITIAL_ADMIN_NAME`
- `INITIAL_ADMIN_EMAIL`
- `INITIAL_ADMIN_MOBILE`
- `INITIAL_ADMIN_PASSWORD`
- `INITIAL_ADMIN_AADHAAR`

There are no hard-coded Admin credentials. Disable or remove bootstrap values after creating the first Admin.

## Creating Another Admin

1. Log in through `POST /api/auth/login/admin`.
2. Use the returned bearer token.
3. Call `POST /api/auth/register/admin`.

The service assigns `ADMIN` server-side and records the creator Admin ID in `createdByAdminId`.

## Troubleshooting

- `Configure AUTH_RSA_PRIVATE_KEY...`: provide `AUTH_RSA_PRIVATE_KEY_FILE` or `AUTH_RSA_PRIVATE_KEY_PEM`.
- `AADHAAR_HASH_PEPPER must be configured`: set a random pepper of at least 32 characters.
- MySQL connection failure in Docker: ensure `DB_URL` uses the Compose service hostname, usually `mysql`, not `localhost`.
- `401`: token is missing, invalid, expired, disabled, locked, or credentials are wrong.
- `403`: token is valid but the role is not allowed.
- Testcontainers skipped: start Docker and rerun `mvn clean verify`.
