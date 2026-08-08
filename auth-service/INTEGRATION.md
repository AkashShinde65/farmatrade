# FarmaTrade P2-P5 Auth Integration

Other FarmaTrade services must validate access tokens locally using Auth Service JWKS. They must never query or depend on the Auth database directly.

## Service Location

- Docker hostname: `auth-service`
- Internal port: `8081`
- JWKS URL: `http://auth-service:8081/.well-known/jwks.json`

## JWT Contract

- Algorithm: `RS256`
- Issuer: `farmatrade-auth-service`
- Audience: `farmatrade-api`
- User ID claim: `sub`
- Role claim: `role`
- Email claim: `email`
- Roles: `FARMER`, `BUYER`, `ADMIN`
- Expiry: 15 minutes
- Key selection: use JWT header `kid` to choose the matching JWKS key

Expected header:

```text
Authorization: Bearer <access-token>
```

## Required Validation

Every P2-P5 service must validate:

- JWT signature using the JWKS public key
- `kid`
- `iss`
- `aud`
- `exp`
- RS256 algorithm

The user ID and role must come from the verified JWT, not from request body fields, query parameters, or client-supplied headers.

## JWKS Caching

Services may cache the JWKS response for performance. Cache by `kid` and refresh when an unknown `kid` appears or when the cache expires. Do not hard-code public keys in service code.

## 401 vs 403

- Return `401` when the token is missing, malformed, expired, signed by an unknown key, has wrong issuer/audience, or otherwise fails authentication.
- Return `403` when the token is valid but the verified `role` is not allowed for the operation.

## Data Boundary

P2-P5 must never access `farmatrade_auth_db` directly. Treat Auth Service as the source of identity truth, and treat verified JWT claims as the request identity.
