# RentHub Microservices

This project was converted from a single-module Spring Boot monolith (preserved for reference under
`legacy-monolith/`) into six independently deployable Spring Boot / Spring Cloud services.

## Services

| Service | Port | Purpose | Database |
|---|---|---|---|
| `eureka-server` | 8761 | Service discovery | none |
| `config-server` | 8888 | Centralized configuration (native/classpath backend under `config-server/src/main/resources/config-repo`) | none |
| `api-gateway` | 8080 | Single entry point, routes requests to the services below, CORS | none |
| `user-service` | 8091 | Registration, login (issues JWT), profile view/update | `renthub_user` |
| `property-service` | 8092 | Landlord property CRUD; also exposes internal `/api/internal/properties/**` for tenant-service | `renthub_property` |
| `tenant-service` | 8093 | Tenant browse/search; calls `property-service` via a Feign client (`lb://property-service`), no DB of its own | none |

## Request routing (via api-gateway, port 8080)

- `/api/auth/**`, `/api/v1/public/**`, `/api/v1/user/**` -> `user-service`
- `/api/landlord/**` -> `property-service`
- `/api/v1/tenant/**` -> `tenant-service`

Each service can also be called directly on its own port for local testing/Swagger UI
(`/swagger-ui.html` on each service).

## Running locally

1. Ensure MySQL is running on `localhost:3306`, and set `DB_PASSWORD` in the environment (as in the
   original monolith). `user-service` and `property-service` will auto-create their databases
   (`renthub_user`, `renthub_property`) via `createDatabaseIfNotExist=true`.
2. Start in this order (each waits on the previous):
   1. `eureka-server`
   2. `config-server`
   3. `user-service`, `property-service`, `tenant-service` (any order, once config-server/eureka are up)
   4. `api-gateway`
3. Build from the repo root: `mvn clean install` (parent aggregator POM builds all six modules).

## What changed vs. the monolith

- **Split by domain**: user/auth, property listings, and tenant browsing/search are now separate
  services with separate databases (`ownerId` on `Property` remains a plain cross-service reference
  to `User.id`, same as in the monolith — no DB-level FK existed before either).
- **JWT validation without cross-service DB calls**: `user-service` now embeds the user's roles as a
  JWT claim at login. `property-service` and `tenant-service` verify the token signature/expiry and
  read roles directly from the claim, instead of querying the user database on every request (which
  they no longer have access to). This is an internal implementation detail — login/response bodies
  and role-based access control behavior are unchanged.
- **Two pre-existing bugs were fixed** (per explicit request):
  1. `SecurityConfig` previously restricted `/api/tenant/**` to `ROLE_TENANT`, but `TenantController`
     is actually mapped at `/api/v1/tenant/**`, so the role check never applied. Now `tenant-service`'s
     `SecurityConfig` correctly restricts `/api/v1/tenant/**`.
  2. `UserUtil.convertUserToUserDto` mapped `email` from `user.getFirstName()` instead of
     `user.getEmail()`. Fixed in `user-service`.
- **Everything else preserved as-is**, including pre-existing quirks not covered by the two fixes above:
  the `city`/`minPrice`/`maxPrice` parameter-order swap between `TenantService` and the repository
  search query, `EntityMapper` never setting `ownerId` when mapping a `PropertyDto` to a `Property`,
  and `PropertyDto` having no `propertyId` field.

## Not verified

This environment has no Java/Maven installed, so the modules could not be compiled or run here.
Please run `mvn clean install` and smoke-test the endpoints locally before relying on this.
