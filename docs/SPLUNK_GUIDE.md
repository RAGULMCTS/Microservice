# Learning Splunk with RentHub, from zero to advanced

This guide uses RentHub's own running data as the training ground. Nothing here is
theoretical — every search, field, index and dashboard panel below points at something
this repo actually ships and actually produces when you run `docker compose up`.

## 0. What's actually running, and why two ingestion paths

| Component | Container | What it does |
|---|---|---|
| `splunk` | `renthub-splunk` | Single-instance indexer + search head. Web UI on `:8000`, HEC on `:8088`, management API on `:8089`, forwarder receiving on `:9997`. |
| `splunk-uf` | `renthub-splunk-uf` | Universal Forwarder. Tails every service's `/var/log/renthub/<service>/app.log` and ships it to `splunk` over `:9997`. |
| 6 Spring Boot services | `renthub-*` | Each writes JSON logs to disk (for the forwarder) **and** sends curated business events + JVM metrics straight to HEC. |

Two onboarding paths on purpose:

- **Universal Forwarder → `renthub_logs`**: the full application log (every framework
  and business log line, in JSON via `logstash-logback-encoder`). This is the classic,
  most common way real production data gets into Splunk: an agent on the host tails a
  file. Config lives in `splunk/uf-apps/renthub_uf/default/{inputs,outputs}.conf`.
- **HTTP Event Collector → `renthub_events`**: a handful of curated business events
  (login, registration, property created, gateway request completed, tenant search)
  sent directly by the app over HTTP. This is the modern, app-owned way to get
  structured telemetry in — no agent needed. Code lives in each service's
  `splunk/SplunkHecClient.java`.
- **HEC metrics → `renthub_metrics`**: every service also POSTs JVM heap/thread counts
  and request/error counters every 15s, using HEC's separate *metrics* event shape.
  This is Splunk's third data type (alongside events and logs) and gets its own query
  language extension (`mstats`/`mcatalog` instead of plain `stats`).

Everything Splunk-side is checked into `splunk/etc/apps/renthub_app/` as plain
`.conf` files — "knowledge objects as code." That's deliberate: it's how real Splunk
deployments are managed (version-controlled apps, not click-ops), and it means you can
read exactly what every index/sourcetype/alert/dashboard does by opening a text file.

---

## 1. First run

```bash
docker compose up --build -d
docker compose ps              # wait until splunk/splunk-uf show "healthy"
```

Log into Splunk: **http://localhost:8000**, user `admin`, password = `SPLUNK_PASSWORD`
from your `.env`.

Exercise the app so there's data to look at: register a user, log in, create a
property, search as a tenant (via `api-gateway` on `:8080`, or hit each service
directly on its own port — see the root `README.md` for the full port table).

Then in Splunk, **Search & Reporting** app, run:

```spl
index=renthub_logs OR index=renthub_events OR index=renthub_metrics
| stats count by index, sourcetype
```

If that returns rows for all three indexes, both ingestion paths are working.

---

## 2. Splunk basics

**Core concepts**, each backed by something in this repo:

- **Index** — a physical bucket of data. RentHub has three: `renthub_logs`,
  `renthub_events`, `renthub_metrics` (`splunk/etc/apps/renthub_app/default/indexes.conf`).
  Try: `| eventcount summarize=false index=renthub_*` to see each index's size.
- **Sourcetype** — the schema Splunk applies when parsing an event. RentHub uses
  `renthub:service:log` (file logs) and `renthub:appevent` (HEC events)
  (`.../default/props.conf`). Try: `index=renthub_logs | stats count by sourcetype`.
- **Search Processing Language (SPL)** — everything after `|` is a pipeline stage.
  Start simple:
  ```spl
  index=renthub_events type=user_login_success
  ```
  Then narrow the time range (top-right picker) and add fields:
  ```spl
  index=renthub_events type=user_login_success
  | table _time username requestId
  ```
- **Fields** — `service`, `level`, `message`, `logger` (from `renthub_logs`) and
  `type`, `status`, `durationMs`, `requestId` (from `renthub_events`) all exist because
  `INDEXED_EXTRACTIONS = json` in `props.conf` told Splunk these logs/events are JSON.
  Try: click any event in the results list and expand it to see every extracted field.
- **Transforming commands** — `stats`, `top`, `timechart`, `chart`:
  ```spl
  index=renthub_logs
  | timechart span=1m count by service
  ```

**Exercise:** find every ERROR-level log line from `property-service` in the last hour:
```spl
index=renthub_logs service="property-service" level="ERROR" earliest=-60m
```

---

## 3. Sourcetypes and field extraction (index-time vs. search-time)

Open `splunk/etc/apps/renthub_app/default/props.conf`. Two things worth understanding:

- `INDEXED_EXTRACTIONS = json` means fields are pulled out **at index time** — before
  the data is even written to disk — rather than re-parsed on every search. Faster
  search, more disk (extra metadata per event). This is the right tradeoff for
  high-volume structured logs like RentHub's.
- `TIME_PREFIX = "timestamp":"` + `TIME_FORMAT` tell Splunk where to find the event's
  real timestamp inside the raw JSON (written by each service's
  `logback-spring.xml` — note the JSON field is named plainly `timestamp`, not the
  logstash-logback-encoder default `@timestamp`, specifically so this line reads
  simply). Without this, Splunk would timestamp every event with "when it was
  indexed," which drifts from "when it actually happened" under any real load or
  forwarder lag.

**Exercise:** `renthub:appevent` (HEC events) doesn't need `TIME_PREFIX` — why? Because
HEC lets the sender set `time` directly in the payload's envelope; when it's absent
(as in RentHub's `SplunkHecClient`), Splunk just uses "time received." Compare
`_time` vs. `_indextime` for a few `renthub_events` results
(`| eval idx=strftime(_indextime,"%c")` ) to see the difference on `renthub_logs`
(forwarder-delayed) vs `renthub_events` (HEC, near-real-time).

**Follow-up exercise (do this yourself):** right now `service` comes from a field
inside the JSON, not the file path. Add a second field, `service_from_path`, extracted
from `source` using a regex in `transforms.conf` + a `REPORT-*` line in `props.conf`
(the file path is `/var/log/renthub/<service>/app.log`). This is the more traditional
"onboard messy data" pattern you'll hit constantly in real deployments.

---

## 4. Eventtypes, tags, and macros

`splunk/etc/apps/renthub_app/default/eventtypes.conf` defines three reusable
classifications:

- `renthub_error` — any ERROR-level log or event
- `renthub_http_5xx` — gateway requests that failed with 5xx
- `renthub_auth_failure` — failed logins

`tags.conf` attaches semantic tags (`error`, `security`) to those eventtypes, so you
can search `tag=security` without remembering every underlying eventtype.

**Exercise:**
```spl
eventtype=renthub_error
| stats count by service
```
vs. the plain search you'd otherwise have to remember:
```spl
(index=renthub_logs OR index=renthub_events) level=ERROR
| stats count by service
```

`macros.conf` ships two macros:

- `` `renthub_errors` `` — expands to the eventtype search above
- `` `renthub_by_service("user-service")` `` — a **parameterized** macro; try changing
  the argument to `"property-service"` in the dashboard's "Auth failures by username"
  panel search and see it re-scope

**Exercise:** write your own macro `renthub_slow_requests(1)` that takes a millisecond
threshold and expands to `index=renthub_events type=gateway_request_completed durationMs>$ms$`.

---

## 5. Lookups

`splunk/etc/apps/renthub_app/lookups/renthub_services.csv` maps each service to its
port, owning team, and tier. The lookup definition is in `transforms.conf`
(`[renthub_services_lookup]`).

**Exercise:**
```spl
index=renthub_logs
| stats count by service
| lookup renthub_services_lookup service OUTPUT owner_team, tier
```
Now you can answer "which team owns the noisiest service" without a join.

---

## 6. Correlation searches — the payoff for `X-Request-Id`

`api-gateway`'s `CorrelationIdGlobalFilter` generates (or trusts) an `X-Request-Id` and
forwards it to whichever downstream service handles the request. Every one of that
service's log lines (via MDC) and every HEC event it sends carries the same id.

**Exercise:** grab a `requestId` from a recent `renthub_events` result (e.g. a
`user_login_success` event), then:
```spl
(index=renthub_logs OR index=renthub_events) requestId="<paste-it-here>"
| sort _time
| table _time index service level type message status durationMs
```
That single search reconstructs one HTTP request's full path through
`api-gateway → user-service` (or property/tenant), across two different indexes fed by
two different ingestion mechanisms. The dashboard's "Trace one request" panel does
exactly this, driven by a text-input token.

**Advanced exercise:** try `transaction requestId` instead of a plain filter+sort, and
compare the output shape (`duration`, `eventcount` fields transaction adds) to what you
built manually with `stats`.

---

## 7. Metrics and `mstats`

`renthub_metrics` is a **metric index** (`datatype = metric` in `indexes.conf`) — a
different storage format optimized for high-cardinality numeric time series. You query
it with `mstats`/`mcatalog`, not `stats`/`search`:

```spl
| mstats avg("jvm.memory.heap.used") as heap_used WHERE index=renthub_metrics BY service span=1m
```

```spl
| mcatalog values(metric_name) WHERE index=renthub_metrics
```
(shows every metric name any service has ever published — a quick way to discover
what's available without guessing).

**Exercise:** the metrics publisher (`SplunkMetricsPublisher.java` in each service)
sends `jvm.thread.count`, `http.request.count`, and `http.error.count` (gateway calls
them `gateway.request.count`/`gateway.error.count`). Build an `mstats` search that
computes an error **rate** (errors / requests) per service over the last 15 minutes.

---

## 8. Alerts and reports

`splunk/etc/apps/renthub_app/default/savedsearches.conf` ships three:

- **RentHub - High Error Rate by Service** — scheduled every 5 min, fires when any
  service logs more than 5 errors in the trailing 5-minute window.
- **RentHub - Auth Failure Spike** — same cadence, fires on more than 3 failed logins
  for one username — a basic brute-force detector.
  **Try it:** POST a few bad logins to `/api/auth/login` in a row and watch
  **Activity → Triggered Alerts** in Splunk.
- **RentHub - Daily Request Volume by Route** — a scheduled *report*, not an alert
  (no trigger condition) — the distinction between "runs and notifies" vs. "just runs
  and saves results" is worth sitting with.

**Exercise:** open one of these in Splunk's UI (Settings → Searches, reports, and
alerts) and add a real trigger action (e.g. "Add to Triggered Alerts" is already
implied by `alert.track=1`; try adding a webhook action pointed at a local test
endpoint).

---

## 9. Dashboards (Simple XML)

Open the **RentHub Service Health** dashboard (app nav, or
`splunk/etc/apps/renthub_app/default/data/ui/views/renthub_service_health.xml`
directly as text). It demonstrates, panel by panel:

1. **Time range + text inputs as tokens** (`$tr.earliest$`, `$svc_filter$`,
   `$requestId$`) — inputs aren't just UI, they're string substitution into SPL.
2. **`timechart` + stacked column** for events-by-service-over-time.
3. **A derived metric in SPL** (`error_pct`) computed with `eval`, then charted.
4. **Drilldown**: click a bar in the first panel and watch `$svc_filter$` update and
   every other panel that uses it re-run — `<drilldown><set token="..."/></drilldown>`.
5. **The correlation-search panel** from section 6, wired to a text input.
6. **`mstats`-backed panels** reading from the metric index.

**Exercise:** add a 7th panel: top 5 slowest gateway routes, using
`index=renthub_events type=gateway_request_completed | stats avg(durationMs) as avg_ms by path | sort -avg_ms | head 5`.

**Next step beyond this repo:** Splunk's newer **Dashboard Studio** (JSON-based,
pixel-precise, not XML) is the modern alternative to Simple XML for new dashboards —
worth a look once Simple XML feels comfortable, but not required to use everything
above.

---

## 10. Administration topics

These don't have a dedicated file in this repo, but you can practice them directly
against RentHub's app/config:

- **`.conf` precedence & `btool`**: RentHub's `renthub_app` only ships `default/`
  stanzas (no `local/` overrides yet). Exec into the container and run
  `docker exec renthub-splunk /opt/splunk/bin/splunk btool props list renthub:service:log --debug`
  to see exactly which `.conf` file contributed which setting — the foundation of
  debugging any real Splunk deployment.
- **Index lifecycle**: `frozenTimePeriodInSecs = 1209600` (14 days) in `indexes.conf`
  controls when RentHub's buckets age from hot → warm → cold → frozen (deleted, since
  no `coldToFrozenDir` is configured). Shrink it to `600` (10 minutes), restart the
  `splunk` container, and watch bucket counts change under
  **Settings → Indexes → renthub_logs**.
- **Roles & users**: create a read-only role scoped to just `renthub_events` (not
  `renthub_logs`) under **Settings → Access controls** and confirm a user in that role
  can't search the other index.
- **`default.meta` / permissions**: `splunk/etc/apps/renthub_app/metadata/default.meta`
  sets `export = system` so every knowledge object here is globally visible. Change one
  stanza's export to `export = none` and watch it disappear from other apps' search bar
  autocomplete.

---

## 11. Where to go from here

- Add a second Universal Forwarder input for MySQL's error log (bind-mount it the same
  way `renthub-logs` is mounted) to practice onboarding *unstructured* data — compare
  the props.conf work required to what RentHub's already-JSON logs needed.
- Try Splunk's **Common Information Model (CIM)** by mapping `renthub_events`'
  `gateway_request_completed` events onto the `Web` data model — this is what lets
  Splunk's prebuilt CIM-aware dashboards and Enterprise Security-style content work
  against arbitrary data.
- Explore **Dashboard Studio** and **Data Model / Pivot** as the next layer once
  everything in this guide feels natural.
