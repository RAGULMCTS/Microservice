# RentHub

RentHub is a Spring Boot microservices demo for a rental-property platform, plus a
full Splunk observability stack around it. It's meant to be run end-to-end with
`docker compose` and used as a hands-on environment for learning Splunk — see
[`docs/SPLUNK_GUIDE.md`](docs/SPLUNK_GUIDE.md) for the curriculum.

## Architecture

```
                        ┌────────────────┐
                        │   api-gateway   │  :8080  (edge, Spring Cloud Gateway)
                        └───────┬────────┘
             ┌──────────────────┼──────────────────┐
             ▼                  ▼                  ▼
    ┌────────────────┐ ┌────────────────┐ ┌────────────────┐
    │  user-service   │ │ property-service│ │ tenant-service │
    │     :8091       │ │     :8092       │ │     :8093      │
    └────────┬────────┘ └────────┬────────┘ └────────┬────────┘
             │                   │                    │ (Feign → property-service)
             ▼                   ▼
        MySQL :3306 (renthub_user / renthub_property databases)

    eureka-server :8761  — service discovery, all services register here
    config-server :8888  — centralized config, served from config-repo/*.yml
```

Every service also writes JSON logs to a shared volume and sends business
events + JVM metrics to Splunk over HTTP (see "Observability" below).

| Service | Port | Role |
|---|---|---|
| `eureka-server` | 8761 | Service discovery |
| `config-server` | 8888 | Centralized config (native profile, `config-repo/`) |
| `api-gateway` | 8080 | Public entry point, routes to the services below |
| `user-service` | 8091 | Auth, registration, profiles (MySQL: `renthub_user`) |
| `property-service` | 8092 | Landlord property listings (MySQL: `renthub_property`) |
| `tenant-service` | 8093 | Tenant-facing browsing/search (calls property-service via Feign) |
| `mysql` | 3306 | Shared MySQL instance, one schema per data-owning service |
| `splunk` | 8000 / 8088 / 8089 / 9997 | Web UI / HEC / management API / forwarder receiving |
| `splunk-uf` | — | Universal Forwarder, tails app logs into `splunk` |

## Running it

1. Copy `.env.example` to `.env` and fill in real secrets (or keep the generated
   ones already in `.env` if you're working from a clone that has it — see the
   comments in that file; it's git-ignored either way).
2. Build and start everything:
   ```bash
   docker compose up --build -d
   ```
3. Watch health status until everything settles:
   ```bash
   docker compose ps
   ```
4. Hit the app through the gateway on `http://localhost:8080`, or any service
   directly on its own port from the table above. Swagger UI is available on each
   Spring service at `/swagger-ui.html`.

To tear down (keeping data): `docker compose down`. To also wipe MySQL/Splunk data:
`docker compose down -v`.

## Observability: Splunk

Splunk's web UI is at **http://localhost:8000** (`admin` / the `SPLUNK_PASSWORD` value
in `.env`). Data lands in three indexes:

- `renthub_logs` — full application logs, tailed from disk by the Universal Forwarder
- `renthub_events` — curated business events (logins, registrations, property
  created, gateway requests, tenant searches), sent directly over Splunk's HTTP Event
  Collector
- `renthub_metrics` — JVM + request metrics from every service, also over HEC

All the Splunk-side configuration (indexes, sourcetypes, field extraction, eventtypes,
tags, macros, a lookup table, alerts, and a "RentHub Service Health" dashboard) lives
as plain `.conf`/XML files in `splunk/etc/apps/renthub_app/`, checked into this repo.

**Start here:** [`docs/SPLUNK_GUIDE.md`](docs/SPLUNK_GUIDE.md) — a guided path from
Splunk basics (search, indexes, sourcetypes) through advanced topics (HEC, metrics,
correlation searches, dashboards, alerts, admin/`.conf` precedence), using this exact
running system as the dataset.

## Configuration

Everything sensitive is environment-driven via `.env` (never committed — see
`.env.example` for the documented list): MySQL credentials, the JWT signing secret,
Eureka/config-server hostnames, CORS origin, and the Splunk admin password + HEC token.
Non-secret, cross-cutting config for the Spring services (DB URLs, JWT expiry, Eureka
URLs, Splunk HEC URL) is centralized in `config-server/src/main/resources/config-repo/`.

## History: monolith → microservices

This project was converted from a single-module Spring Boot monolith into the six
independently deployable services listed above. Two pre-existing bugs from that
monolith were fixed during the split:

1. `tenant-service`'s `SecurityConfig` originally restricted `/api/tenant/**` to
   `ROLE_TENANT`, but `TenantController` is actually mapped at `/api/v1/tenant/**`, so
   the role check never applied. It now correctly restricts `/api/v1/tenant/**`.
2. `UserUtil.convertUserToUserDto` mapped `email` from `user.getFirstName()` instead of
   `user.getEmail()`. Fixed in `user-service`.

JWT validation was also changed to avoid cross-service DB calls: `user-service` embeds
the user's roles as a JWT claim at login, and `property-service`/`tenant-service`
verify the token and read roles from that claim instead of querying the user database
directly (which they no longer have access to). Login/response bodies and
role-based access behavior are otherwise unchanged. Everything else — including
pre-existing quirks not covered by the two fixes above (the `city`/`minPrice`/
`maxPrice` parameter-order handling between `TenantService` and its repository search,
and `EntityMapper` not setting `ownerId` when mapping a `PropertyDto` to a `Property`)
— was preserved as-is.

## Verification status

The Splunk stack, logging pipeline, and correlation-ID wiring in this repo were built
and statically reviewed (config/YAML/Dockerfile syntax, cross-file env var names,
index/sourcetype consistency) in an environment without Docker installed, so none of
it has actually been run end-to-end yet. Before relying on it, run
`docker compose up --build -d`, confirm every container reports healthy via
`docker compose ps`, and walk through the verification steps in
[`docs/SPLUNK_GUIDE.md`](docs/SPLUNK_GUIDE.md) section 1.
