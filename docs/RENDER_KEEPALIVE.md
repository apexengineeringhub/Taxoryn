# Render Keep-Alive & Health Check

## Why `/api/health` exists

Render's **Free** Web Service plan spins the instance down after a period of inactivity, and
the next request pays a "cold start" penalty while Render boots the container back up. While
Taxoryn is in its MVP / free-user stage we're not paying for an always-on instance, so we rely
on two things instead:

1. **Render's own health check** (`healthCheckPath`) - used by Render to decide whether the
   deploy is healthy and whether the service is ready to receive traffic.
2. **An external uptime monitor** that pings the service periodically, which keeps it warm and
   gives us an alert if it ever stops responding.

Both of these need an endpoint that is:

- Public (no JWT required - a monitor has no user session).
- Fast and dependency-free, so it doesn't add load or become a false-negative if the database
  or an external API is briefly slow.

`GET /api/health` (`com.taxoryn.core.health.HealthController`) is that endpoint. It:

- Returns `HTTP 200` with `{"status": "UP"}` whenever the Spring Boot process is running.
- Does **not** query PostgreSQL and does **not** call any external service - it only proves the
  embedded servlet container and Spring context are alive.
- Is excluded from the API rate limiter (`RateLimitingFilter`) so monitoring traffic never
  contends with real API traffic or gets throttled.

This is intentionally separate from Spring Boot Actuator's `/actuator/health`, which is still
enabled and still public (`/actuator/health`, `/actuator/info`) for local Docker health checks
and deeper diagnostics. Actuator's default health group aggregates indicators - including a
datasource indicator that runs a validation query against PostgreSQL - which is exactly the
per-ping DB load we want to avoid for a keep-alive/monitoring endpoint that may be called every
few minutes, 24/7.

## Configuring the Render health-check path

`render.yaml` at the repository root already sets:

```yaml
healthCheckPath: /api/health
```

If you're managing the service through the Render dashboard instead of a Blueprint sync:

1. Open the service in the Render dashboard.
2. Go to **Settings → Health & Alerts** (sometimes shown under the general **Settings** page).
3. Set **Health Check Path** to `/api/health`.
4. Save. Render will use this path both to gate deploys (traffic isn't routed to a new instance
   until it responds `200`) and, on paid plans, for zero-downtime deploy checks.

No code change is needed if you're already syncing `render.yaml` as a Blueprint - Render will
pick this up on the next deploy.

## Render Free services can spin down

Even with a correct health check, a **Free** Web Service on Render will spin down after roughly
15 minutes without incoming HTTP traffic. The health check path only affects deploy/readiness
checks - it does **not** by itself prevent spin-down. That's what the external monitor below is
for.

## Using an external uptime monitor

Point any uptime monitor (e.g. UptimeRobot, Better Stack, Cron-Job.org, Freshping) at:

```
GET https://<your-render-service>.onrender.com/api/health
```

Replace `<your-render-service>` with your actual Render service subdomain (or custom domain, if
configured).

**Recommended interval: ~10 minutes.**

- This is frequent enough to keep the free instance from ever fully idling out, without being an
  aggressive polling interval that wastes the monitor's quota or adds needless load.
- Do not poll more often than every few minutes "just to be safe" - it doesn't meaningfully
  improve availability and just generates noise.

## What is intentionally *not* done

- **No `@Scheduled` self-ping inside the Spring Boot app.** A scheduled task that calls the
  app's own endpoint only runs while the JVM process is alive. If Render suspends the process
  due to inactivity, the scheduler is suspended right along with it - so it cannot reliably wake
  the service back up. Keep-alive has to come from *outside* the process.
- **No frontend-driven keep-alive.** The Vercel-hosted frontend should not be responsible for
  keeping the Render backend warm (e.g. via a `setInterval` fetch from the browser). That only
  works while a user's tab happens to be open, and it couples an infra concern to the client
  bundle. Use an external monitor instead.

## Reference

- Endpoint: `com.taxoryn.core.health.HealthController` / `HealthStatusResponse`
- Security: permitted in `com.taxoryn.core.config.SecurityConfig` (`/api/health` → `permitAll()`)
- Rate limiting: excluded in `com.taxoryn.core.filter.RateLimitingFilter#isExcludedPath`
- Tests: `com.taxoryn.core.health.HealthControllerIntegrationTest`
- Render config: `render.yaml` (`healthCheckPath: /api/health`)
- Docker-level health check: `Dockerfile` (`HEALTHCHECK` now targets `/api/health`)
