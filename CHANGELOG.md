# Documentation Changelog & Milestone Register

All notable technical revisions, architectural baselines, and milestone deliverables for the **Oi** messaging application are documented in this register.

---

## [1.0.0-docs] - 2026-11-07 — Complete Master Technical Documentation Suite

### Added
- **Full 24-Document Architecture Suite:**
  - `docs/00_PROJECT_CHARTER.md`: Vision, scope boundaries, RACI matrix, risk assessment.
  - `docs/01_PRODUCT_REQUIREMENTS.md`: PRD, 12 differentiators, user personas, performance SLOs.
  - `docs/02_FUNCTIONAL_REQUIREMENTS.md`: Complete functional specs across all 3 primary areas, phone auth, OTP rules, 14-day grace, username quarantine.
  - `docs/03_USER_FLOWS.md`: 12 detailed end-to-end user journeys and state transition diagrams.
  - `docs/04_NON_FUNCTIONAL_REQUIREMENTS.md`: Latency budgets, memory, battery, extreme network conditions, availability SLOs.
  - `docs/05_IDENTITY_AND_VERIFICATION.md`: Sovereign government identity architectures for India (UIDAI Offline XML), UK (DIATF), USA (mDL/AAMVA), and Blinded Identity Nullifier deduplication.
  - `docs/06_SECURITY_AND_PRIVACY.md`: OWASP MASVS v2.0 compliance, SQLCipher, Android Keystore StrongBox, data minimization.
  - `docs/07_END_TO_END_ENCRYPTION.md`: Mathematical specification for Signal Double Ratchet, X3DH key agreement, KDF chains, AES-256-GCM framing, zero backdoor guarantee.
  - `docs/08_SYSTEM_ARCHITECTURE.md`: Distributed systems design, stateless WebSocket gateways (100k conns/node), Redis pub/sub, gRPC service mesh.
  - `docs/09_DATABASE_DESIGN.md`: PostgreSQL 16 schema, citext unique usernames, partial indexes, Room SQLite local schema, 365-day retention purge engine.
  - `docs/10_API_AND_REALTIME_PROTOCOL.md`: REST endpoints and bidirectional WebSocket protocol frames (RFC 6455).
  - `docs/11_ANDROID_ARCHITECTURE.md`: Pure Native Kotlin 2.x, Jetpack Compose, Clean Architecture + MVI, WorkManager, Macrobenchmark.
  - `docs/12_IOS_PORTABILITY.md`: Cross-platform contract parity for Phase 2 native Swift/SwiftUI client without backend refactoring.
  - `docs/13_TESTING_STRATEGY.md`: Automated quality engineering pyramid across 24 test disciplines.
  - `docs/14_TEST_CASE_CATALOG.md`: **106 distinct, formal automated test scenarios** with IDs, prerequisites, actions, expected results, severity, and automation status.
  - `docs/15_PERFORMANCE_TESTING.md`: Android Macrobenchmark harness, cold launch profiling ($<800\text{ ms}$), distributed k6 load testing.
  - `docs/16_CODE_QUALITY.md`: Konsist architecture fitness tests, Spotless/ktlint, Detekt, SonarCloud gates.
  - `docs/17_CI_CD_PIPELINE.md`: GitHub Actions automated pipeline from commit to signed Google Play release.
  - `docs/18_INFRASTRUCTURE_AND_COSTS.md`: AWS multi-environment topology, Terraform IaC, financial cost model ledger ($291/mo baseline).
  - `docs/19_DEPLOYMENT_AND_RELEASE.md`: Play Store signing, upload keys, staged rollouts, zero-downtime database migrations.
  - `docs/20_MONITORING_AND_MAINTENANCE.md`: SRE telemetry, Prometheus/Grafana dashboards, incident runbooks.
  - `docs/21_THREAT_MODEL_AND_RISKS.md`: Comprehensive STRIDE threat model covering SIM swaps, stolen IDs, database dumps, MitM, and mitigations.
  - `docs/22_DECISION_LOG.md`: Formal Architectural Decision Records (`ADR-001` through `ADR-006`).
  - `docs/23_OPEN_QUESTIONS.md`: Active Product Owner clarification queue.
- **Repository Metadata:**
  - `README.md`, `DOCUMENTATION_INDEX.md`, `GLOSSARY.md`, `CONTRIBUTING.md`.
