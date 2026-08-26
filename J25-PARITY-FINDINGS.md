# system-scheduling — J17 → J25 behavioural parity findings

Audit against the CTP J17→J25 parity guide (Confluence 1990371020, 24-BC catalogue) and the
users-groups reference (PEG-3336, PRs #217/#219). Method: **Java 17 (`main`) is the source of
truth** — each parity test is authored green on J17 first, then the identical assertion is carried
to J25 (`team/25.104.x`).

## Context shape

system-scheduling is a **Quartz-backed scheduler**. Its viewstore is the **Quartz schema** itself
(`001-initial-quarz_schema` + `002-force-job-data-type` + `003-drop-process-event-index`) — there is
**no JPA/Hibernate layer** (no `@Entity`, no `EntityManager`, no `persistence.xml`, no repository
classes). Scheduling state is persisted by Quartz over JDBC to Postgres. Command API + event
processor/listener + domain; no query module.

**No production Java source changed J17→J25** beyond `javax`→`jakarta` import churn (verified: the
non-import source diff between `main` and `team/25.104.x` is empty).

## BC catalogue disposition

| BC | Area | Present? | Disposition |
|----|------|----------|-------------|
| BC-01/02 | JPA finder null↔throw | No | N/A — no JPA; Quartz owns persistence |
| BC-04 | NULL → primitive int | No | N/A — no JPA entity mapping |
| BC-05 | JPQL `!= null` | No | N/A — no JPQL |
| BC-06 | Lazy-init | No | N/A — no JPA associations |
| BC-07 | `liquibase.hub.mode` removed in Liquibase 5 | **Yes** | **Fixed** — removed the key from `systemscheduling-viewstore-liquibase/…/liquibase.properties`. Valid on Liquibase 4 (J17), hard deploy failure on Liquibase 5 (J25). J25 change only. |
| BC-11 | `JsonObjectBuilder.add(k, null)` | Builder used, no null-add | N/A — `ScheduleJobProcessor` uses `createObjectBuilder().build()` (empty) + `createObjectBuilderWithFilter(...)`; no `add(key, null)` site, and per guide v6 that pattern is parity anyway |
| BC-20 | Drools 0-rule vacuous deny | **Yes** (1 kbase) | **Guarded** — added `AccessControlRuleCountTest` asserting the `COMMAND_API` kbase compiles ≥1 rule. Both branches. |
| BC-24 | pgjdbc / Quartz JDBC JobStore | Runtime | Covered by ITs — no context source adapted to any Quartz/driver change, so parity rides on the existing scheduler integration suite against real Postgres. Quartz version is BOM-managed (framework tier). |

## Golden-master baseline

The existing test golden JSON is **unchanged** J17→J25 (0 files differ). The existing suite therefore
already proves message/response-shape parity for free.

## Changes in this branch

- **BC-07:** removed `liquibase.hub.mode: off` from the viewstore liquibase properties (J25 only).
- **BC-20:** `systemscheduling-command-api/.../accesscontrol/AccessControlRuleCountTest.java` —
  rule-count guard for kbase `COMMAND_API` (both branches).

## Two-PR structure

- **J17 (`main`)** — test-only: `AccessControlRuleCountTest`. Proves the guard is green on the
  source-of-truth stack.
- **J25 (`team/25.104.x`)** — same test **plus** the BC-07 liquibase fix and these findings docs.
