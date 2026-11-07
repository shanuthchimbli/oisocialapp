# Architectural Decision Log & Records (ADR)

> **Document ID:** ADR-LOG-001  
> **Status:** Stage 1 Baseline — Continuous Register  
> **Last Updated:** 2026-11-07  
> **Lead Architect:** Senior Software Architect & Security Group  

---

## 1. Decision Classification Taxonomy

Every architectural decision within this repository is classified under one of the following canonical states:
- `[Confirmed]`: Formally mandated by the Product Owner or established as an immutable architectural invariant.
- `[Proposed]`: Recommended by engineering; pending Product Owner review.
- `[Open]`: Active architectural tradeoff undergoing evaluation.
- `[Blocked]`: Dependent upon external legal, statutory, or technical validation.
- `[Requires Legal Validation]`: Involves sovereign identity, data privacy, or telecommunications compliance.
- `[Requires Technical Validation]`: Involves unverified hardware crypto, latency budgets, or OS limits.

---

## 2. Architecture Decision Records

### ADR-001: Mobile Client Architecture — Native Android Kotlin & Jetpack Compose
- **Status:** `[Confirmed]`
- **Date:** 2026-11-07
- **Context:** The application demands sub-second cold starts, 60+ FPS sustained scrolling, deep hardware keystore integration (TEE/StrongBox), zero binary bloat, and minimal memory overhead. The platform strategy mandates Android first, followed by iOS in Phase 2, with zero Web client.
- **Alternatives Considered:**
  1. *Flutter / Dart:* Cross-platform code sharing, but introduces a heavy C++ Skia/Impeller rendering engine runtime (+15–30 MB baseline binary), higher cold-start latency JIT/AOT overhead, and bridge overhead for deep hardware TEE cryptographic calls.
  2. *React Native:* JavaScript runtime, bridge serialization latency, insecure key management surface, unpredictable garbage collection pauses during message list flings.
  3. *Pure Native Kotlin + Jetpack Compose:* First-class Android runtime optimization, direct integration with Android Keystore/Biometrics, smallest possible APK footprint, and zero inter-language bridge overhead.
- **Decision:** Mandate Native Android development in Kotlin using Jetpack Compose, Room (SQLCipher), and Kotlin Coroutines/Flow.
- **Consequences:** The iOS client (Phase 2) will be authored natively in Swift/SwiftUI. Architectural contracts, wire models, and cryptographic ratchets will remain strictly platform-agnostic to enable clean client parity without backend refactoring.
- **Traceability:** `PRD-012`, `ARCH-AND-001`, `docs/11_ANDROID_ARCHITECTURE.md`.

---

### ADR-002: End-to-End Encryption Protocol Selection — Signal Double Ratchet + X3DH
- **Status:** `[Confirmed]`
- **Date:** 2026-11-07
- **Context:** The application requires cryptographically verified zero-knowledge end-to-end encryption for all 1:1 text messages and status updates. The system must guarantee both Forward Secrecy (FS) and Post-Compromise Security (PCS). Custom encryption algorithms are strictly banned.
- **Alternatives Considered:**
  1. *Custom Protocol:* Strictly prohibited by security policy.
  2. *IETF MLS (Messaging Layer Security - RFC 9420):* Outstanding protocol engineered primarily for large multi-party groups (TreeKEM). Excessive overhead and complexity for pure 1:1 messaging.
  3. *Matrix Olm/Megolm:* Good protocol, but Olm is functionally a derivation of the Double Ratchet with larger metadata overhead and less mature native mobile runtime optimizations.
  4. *Signal Double Ratchet + X3DH (Curve25519, HKDF, AES-256-GCM):* The global gold standard for 1:1 asynchronous messaging. Formally proven, audited by multiple independent academic bodies, and battle-tested at billion-user scale.
- **Decision:** Standardize exclusively on the **Signal Double Ratchet Algorithm** with **X3DH** for asynchronous key exchange.
- **Consequences:** Client applications must store and manage prekey pools, signed prekeys, and ratchet states locally in hardware-encrypted SQLite. Server stores zero plaintext.
- **Traceability:** `PRD-006`, `SEC-CRYPTO-001`, `docs/07_END_TO_END_ENCRYPTION.md`.

---

### ADR-003: Single Active Device Enforcement & Session Invalidation
- **Status:** `[Confirmed]`
- **Date:** 2026-11-07
- **Context:** To prevent session hijacking, duplicate cryptographic states, and clandestine physical eavesdropping, each verified account must be restricted to exactly one active physical device.
- **Alternatives Considered:**
  1. *Multi-Device Linked Sessions (e.g., Signal/WhatsApp Web model):* Requires complex distributed ratchet synchronization, multi-party key distribution, and increases the attack surface to secondary compromised devices.
  2. *Single Active Device with Instant Server Invalidation:* When Device B completes registration and authentication, the central session broker immediately revokes Device A's session JWT, sends a high-priority silent FCM revocation push, and deletes Device A's published prekey bundles.
- **Decision:** Implement **Single Active Device Enforcement**. Logging into a new device instantly terminates the prior session.
- **Consequences:** When Device A receives the revocation push (or detects token rejection on its next ping), it immediately zeroizes active ratchet session keys and transitions the UI to a locked "Session Terminated on Another Device" state.
- **Traceability:** `PRD-005`, `FUNC-DEV-001`, `docs/08_SYSTEM_ARCHITECTURE.md`.

---

### ADR-004: Government-System-Only Identity Verification & Zero Raw Data Storage
- **Status:** `[Requires Legal & Technical Validation]`
- **Date:** 2026-11-07
- **Context:** The Product Owner requires identity validation against official sovereign government systems only (UIDAI in India, GOV.UK in UK, NIST-compliant state trust frameworks in US). Direct storage of raw government identity numbers (e.g., 12-digit Aadhaar number, US Social Security Number) is legally prohibited, creates catastrophic breach liability, and violates privacy regulations.
- **Decision:**
  1. **India (+91):** Utilize **UIDAI Aadhaar Offline Paperless e-KYC (XML / Secure QR)**. The resident downloads their digitally signed XML directly from UIDAI portals with a 4-digit share code. The server verifies UIDAI's digital signature, extracts the demographic match, and derives a one-way Blinded Nullifier without storing the Aadhaar number.
  2. **United Kingdom (+44):** Evaluate **GOV.UK One Login** and Cabinet Office Trust Framework certified identity schemes.
  3. **United States (+1):** Evaluate **AAMVA DLDV / State Mobile Driver's License (mDL - ISO/IEC 18013-5)** and Login.gov IAL2 trust frameworks.
  4. **Deduplication:** A blind cryptographic nullifier ($\text{HMAC-SHA256}(\text{ServerPepper}, \text{ID})$) is stored with a database `UNIQUE` constraint to enforce One-Person-One-Account without retaining raw credentials.
- **Consequences:** Verification mechanisms vary by sovereign territory. Private application access to live government APIs is subject to rigorous statutory approval.
- **Traceability:** `PRD-001`, `PRD-003`, `docs/05_IDENTITY_AND_VERIFICATION.md`.

---

### ADR-005: Radical Elimination of Multimedia and Group Communication
- **Status:** `[Confirmed]`
- **Date:** 2026-11-07
- **Context:** Multimedia processing (image compression, video transcoding, CDN storage, EXIF stripping, virus scanning, streaming pipelines) accounts for >85% of messaging operational costs, latency bottlenecks, and abuse moderation overhead. Group messaging introduces quadratic fan-out latency and complicated multi-device state synchronization.
- **Decision:** Enforce an immutable architectural constraint: **Pure Text-Only Status Updates** and **Pure Text-Only 1:1 Messaging**.
- **Consequences:** Backend requires zero object storage (S3/GCS) for media transit. Network payloads remain under 2 KB per message frame. Infrastructure costs are reduced by orders of magnitude, and battery consumption on mobile devices is virtually eliminated.
- **Traceability:** `PRD-008`, `PRD-009`, `PRD-010`, `docs/08_SYSTEM_ARCHITECTURE.md`.

---

### ADR-006: Cryptographically Enforced 365-Day Message Purge Engine
- **Status:** `[Confirmed]`
- **Date:** 2026-11-07
- **Context:** The Product Owner requires that messages expire after 365 days. Data accumulation indefinitely creates legal discovery liability and bloats mobile client storage.
- **Decision:** Implement a dual-layer purge architecture:
  1. *Server-Side Transit Relays:* Transient encrypted messages queued for offline recipients are dropped after 30 days if undelivered. Delivered messages are dropped from server transit immediately upon recipient delivery confirmation.
  2. *Client-Side Local Storage:* Android Room database runs an automated scheduled WorkManager task every 24 hours that executes:
     `DELETE FROM messages WHERE timestamp < (strftime('%s', 'now') - 31536000);`
     followed by `VACUUM;` to reclaim pages and zeroize deleted records.
- **Consequences:** Plaintext cannot be recovered after 365 days. Users must be explicitly informed that Oi does not provide permanent archiving beyond 1 year.
- **Traceability:** `PRD-007`, `FUNC-RET-001`, `docs/09_DATABASE_DESIGN.md`.
