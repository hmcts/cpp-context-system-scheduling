# system-scheduling — J25 parity checklist

Per CTP parity guide (Confluence 1990371020) and users-groups reference (PEG-3336). J17 = source of
truth. See `J25-PARITY-FINDINGS.md` for full reasoning.

- [x] **Scan for BC triggers** — Quartz-backed (no JPA), 1 Drools kbase (`COMMAND_API`), 1
      liquibase.properties, no `add(k,null)` sites, no production source change J17→J25.
- [x] **BC-07** liquibase.hub.mode — removed from `systemscheduling-viewstore-liquibase` (J25 only).
- [x] **BC-20** Drools 0-rule guard — `AccessControlRuleCountTest` on kbase `COMMAND_API` (both).
- [x] **BC-01/02/04/05/06** JPA/Hibernate family — N/A (Quartz persistence, no `@Entity`).
- [x] **BC-11** JsonObjectBuilder null-add — N/A (empty builder / filtered builder only; parity per guide v6).
- [x] **BC-24** pgjdbc / Quartz JDBC — covered by ITs; no context source adapted to Quartz/driver.
- [x] **Golden master** — test JSON unchanged; existing suite proves parity.
- [ ] **J17 rit green** (JDK17 + cpp-developers-docker java-17).
- [ ] **J25 rit green** (JDK25 + cpp-developers-docker java-25).
- [ ] **Two PRs open** — J17 test-only vs `main`; J25 tests+fix vs `team/25.104.x`.
