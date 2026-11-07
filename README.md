# OI (Oy / Oi) — Verified Identity-Based Messaging Application

> **Document Classification:** Engineering & Architectural Specification  
> **Status:** Stage 1 — Approved Baseline (Documentation-First Phase)  
> **Target Release Platform:** Android (Native Kotlin / Jetpack Compose) | Future: iOS (Native Swift)  
> **Official Repository:** `https://github.com/shanuthchimbli/oisocialapp`  
> **Workspace Path:** `/Users/shanuthchimbli/.gemini/antigravity/scratch/oisocialapp`

---

## 1. Executive Summary & Brand Identity

**OI** (pronounced *"Oy"* / *"Oi"*, culturally inspired by the universal and colloquial calling greeting meaning *"Hey!"*) is a hyper-focused, high-performance, minimalist, secure communication platform engineered exclusively around **verified real-world human identities**.

Unlike conventional mass-market messaging applications that permit anonymous registration, multi-account burner profiles, bot spam, and multimedia distraction, **OI** enforces a strict **One-Person-One-Account** invariant backed solely by **legally recognized, government-operated identity verification systems**.

### Core Value Propositions
1. **One Verified Person = One Account:** Strict deduplication against authoritative state identity systems prevents bot nets, trolls, identity masking, and sockpuppet operations.
2. **True End-to-End Encryption (E2EE):** Cryptographically enforced zero-knowledge architecture ensures that neither platform operators nor third parties can inspect message contents or text statuses.
3. **Radical Minimalism:** Pure text-only communication. Zero images, audio messages, videos, stickers, GIFs, voice calls, video calls, or social feeds.
4. **Deterministic Ephemerality:** Enforced 365-day maximum retention lifecycle across all client and server persistence layers.
5. **Zero-Latency Feel:** Native Android-first engineering targeting sub-second cold starts, 60 FPS minimum UI rendering, and sub-50ms optimistic local message rendering.

---

## 2. Current Project Phase: Documentation-First

Per project governance rules, **NO APPLICATION SOURCE CODE, UI MOCKUPS, OR BACKEND IMPLEMENTATION ARE TO BE CREATED** until the 7-stage technical documentation suite is fully authored, cross-audited, and approved by the Product Owner.

### Execution Staging Roadmap

| Stage | Scope / Deliverables | Status |
| :--- | :--- | :--- |
| **Stage 1** | Project Charter, Product Requirements Document (PRD), Documentation Index, Master Glossary, Baseline Decision & Question Registers | **COMPLETED (Stage 1 Baseline)** |
| **Stage 2** | Functional & Non-Functional Requirements, User Flows, Identity & Account Lifecycle Specifications | *Next Stage* |
| **Stage 3** | Security Architecture, Threat Modeling, E2EE Protocol Specification, Government Verification Feasibility Study | *Scheduled* |
| **Stage 4** | System Architecture, Database Schema (PostgreSQL/Redis), API & Real-time WebSocket Protocol, Android/iOS Modular Contracts | *Scheduled* |
| **Stage 5** | QA Testing Strategy, 100+ Cataloged Formal Test Scenarios, Automated Pipeline Verification | *Scheduled* |
| **Stage 6** | Code Quality Gates, CI/CD Pipeline, Infrastructure as Code (IaC), Cost Breakdown, Deployment & Operations Runbooks | *Scheduled* |
| **Stage 7** | Comprehensive Documentation Audit, Traceability Matrix Verification, Formal Sign-Off | *Scheduled* |

---

## 3. Repository Directory Structure

```text
oisocialapp/
├── README.md                           # Master Project Overview & Quick Start
├── DOCUMENTATION_INDEX.md              # Documentation Directory & Traceability Index
├── GLOSSARY.md                         # Standard Terminology, Acronyms & Mathematical Symbols
├── CONTRIBUTING.md                     # Documentation Governance & Contribution Standards
├── CHANGELOG.md                        # Version Control & Iteration Log
└── docs/
    ├── 00_PROJECT_CHARTER.md           # [Stage 1] Vision, Governance, Scope, Boundaries & RACI
    ├── 01_PRODUCT_REQUIREMENTS.md      # [Stage 1] Master PRD, Personas & System Invariants
    ├── 02_FUNCTIONAL_REQUIREMENTS.md   # [Stage 2] Comprehensive Functional Specifications
    ├── 03_USER_FLOWS.md                # [Stage 2] User Journeys & State Transition Diagrams
    ├── 04_NON_FUNCTIONAL_REQUIREMENTS.md# [Stage 2] Performance, Scalability & Reliability SLOs
    ├── 05_IDENTITY_AND_VERIFICATION.md # [Stage 2/3] Government Identity Verification Systems (IN, UK, US)
    ├── 06_SECURITY_AND_PRIVACY.md      # [Stage 3] Security Controls, Privacy & Data Minimization
    ├── 07_END_TO_END_ENCRYPTION.md     # [Stage 3] Cryptographic Protocol, Key Management & Storage
    ├── 08_SYSTEM_ARCHITECTURE.md       # [Stage 4] Distributed Systems Design, Caching & Real-time Core
    ├── 09_DATABASE_DESIGN.md           # [Stage 4] Relational Data Models, Constraints & Indexing
    ├── 10_API_AND_REALTIME_PROTOCOL.md # [Stage 4] REST APIs, WebSocket Framing & Error Contracts
    ├── 11_ANDROID_ARCHITECTURE.md      # [Stage 4] Kotlin, Jetpack Compose, Room & Offline Sync
    ├── 12_IOS_PORTABILITY.md           # [Stage 4] Swift/Compose Multiplatform Boundaries & Parity
    ├── 13_TESTING_STRATEGY.md          # [Stage 5] End-to-End Quality Engineering Framework
    ├── 14_TEST_CASE_CATALOG.md         # [Stage 5] 100+ Detailed Executable Test Scenarios
    ├── 15_PERFORMANCE_TESTING.md       # [Stage 5] Latency, Throughput, Memory & Battery Testing
    ├── 16_CODE_QUALITY.md              # [Stage 6] Static Analysis, Linting, Architecture Fitness Rules
    ├── 17_CI_CD_PIPELINE.md            # [Stage 6] GitHub Actions Automation, Security Scanning & Gates
    ├── 18_INFRASTRUCTURE_AND_COSTS.md  # [Stage 6] IaC, Cloud Topology, Operational Cost Ledger
    ├── 19_DEPLOYMENT_AND_RELEASE.md    # [Stage 6] Release Engineering, Signing, Staging & Rollouts
    ├── 20_MONITORING_AND_MAINTENANCE.md# [Stage 6] Telemetry, Observability, Alerting & Incident Ops
    ├── 21_THREAT_MODEL_AND_RISKS.md    # [Stage 3] STRIDE Threat Model & Attack Surface Mitigation
    ├── 22_DECISION_LOG.md              # [Continuous] Architecture Decision Records (ADRs)
    └── 23_OPEN_QUESTIONS.md            # [Continuous] Product Owner Clarification Queue
```

---

## 4. Requirement Identifier Convention

| Prefix | Domain | Description |
| :--- | :--- | :--- |
| `PRD-xxx` | Product Requirements | High-level product and business rules |
| `FUNC-xxx` | Functional Specs | Granular functional behavior and features |
| `NFR-xxx` | Non-Functional | Availability, reliability, and architectural constraints |
| `PERF-xxx` | Performance Targets | Explicit latency, throughput, and hardware resource limits |
| `SEC-xxx` | Security & Cryptography | Security controls, threat mitigations, and crypto invariants |
| `ID-xxx` | Identity & Verification | Government ID integration, validation, and deduplication rules |
| `ARCH-xxx` | System Architecture | Architectural decisions, subsystem boundaries, and topologies |
| `DB-xxx` | Database Design | Schema constraints, indexing, and migration invariants |
| `API-xxx` | Protocol & Interfaces | HTTP/REST and WebSocket contract specifications |
| `TEST-xxx` | Test Scenarios | Cataloged test cases spanning all verification layers |
| `DEVOPS-xxx`| DevOps & Operations | CI/CD automation, IaC, deployment, and monitoring standards |

---

## 5. Development Restrictions & Guidelines

1. **Pure Documentation Enforcement:** Until formal sign-off of Stage 7, no build artifacts, Gradle scripts, Kotlin files, or database schemas may be deployed or committed to code repositories.
2. **Authority of the Product Owner:** All architectural tradeoffs with product or legal implications (e.g., username reservation policies, government integration models, retention purge timing) must be logged in [`docs/23_OPEN_QUESTIONS.md`](file:///Users/shanuthchimbli/.gemini/antigravity/scratch/oisocialapp/docs/23_OPEN_QUESTIONS.md) and explicitly signed off before implementation.
3. **No Backdoors:** Cryptographic isolation is absolute. There are no lawful interception backdoors, employee override master keys, or unencrypted telemetry pipelines.
4. **Zero Vanity Features:** Any proposed feature outside Text Status, 1:1 Text Chat, and Profile/Settings is rejected by default.
