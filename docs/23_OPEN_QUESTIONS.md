# Open Questions & Product Owner Decision Register

> **Document ID:** QUE-001-REGISTER  
> **Status:** Stage 1 Baseline — Active Decision Queue  
> **Last Updated:** 2026-11-07  
> **Owner:** Product Owner  
> **Prepared By:** Senior Software Architect & Security Group  

---

## 1. Executive Summary

This register catalogs all open architectural, functional, policy, and legal questions requiring formal review and decision from the **Product Owner**. Per governance rules, the engineering team will not silently make high-impact product or legal determinations.

Each item includes the background context, evaluated options, trade-off analysis, engineering recommendation, and current status.

---

## 2. Active Decision Items for Product Owner Review

### ITEM-001: Released Username Policy — Reservation vs. Recycling
- **Requirement Reference:** `PRD-004`, `FUNC-USR-003`
- **Context:** When an existing user changes their unique username (e.g., changing from `@alex` to `@alexander`), what should happen to the prior handle (`@alex`)?
- **Options Under Consideration:**
  - **Option 1: Immediate Release to Public Pool.** The old handle immediately becomes available for any user to claim.
    - *Risk:* Extreme impersonation risk. Malicious actors can claim `@alex` and message old contacts who still associate `@alex` with the original human identity.
  - **Option 2: Permanent Reservation to Original Account.** The handle `@alex` is permanently bound to the original account's immutable UUIDv7. No other user can ever claim it, but the original user can reclaim it.
    - *Risk:* Consumes the namespace over time; permanently locks desirable handles even if the user abandons them.
  - **Option 3 (Engineering Recommended): 180-Day Quarantine with Release.** The abandoned handle enters a 180-day cooling-off lock during which only the original owner can reclaim it. After 180 days, it is unlocked for general registration, and any user messaging the newly claimed handle receives an in-chat system alert: *"The handle @alex is now registered to a new verified person."*
- **Status:** `[Open — Awaiting Product Owner Sign-Off]`
- **Impact Level:** High (Security, User Experience, Identity Trust)

---

### ITEM-002: Provisional Account Messaging Restrictions during 14-Day Grace Window
- **Requirement Reference:** `PRD-001`, `FUNC-ID-004`
- **Context:** A user verifies their phone number via SMS OTP and receives a provisional account. They are granted 14 days to complete sovereign government verification. Should provisional unverified accounts have full messaging capabilities, or should they be constrained to prevent spam/abuse?
- **Options Under Consideration:**
  - **Option 1: Unrestricted 1:1 Messaging for 14 Days.** User can initiate unlimited conversations. If unverified after 14 days, the account is suspended.
    - *Risk:* Spammers can burn cheap SIM cards, message hundreds of verified users for 13 days, and abandon the account.
  - **Option 2 (Engineering Recommended): Restricted Outbound Quota.** Provisional accounts can initiate conversations with a maximum of **3 unique contacts**. Once a contact accepts or replies, messaging is bidirectional, but broad cold outreach is impossible until sovereign verification succeeds.
  - **Option 3: Inbound Only.** Provisional accounts can only receive messages from verified users who initiate contact with them; they cannot initiate cold chats.
- **Status:** `[Open — Awaiting Product Owner Sign-Off]`
- **Impact Level:** High (Anti-Abuse, Network Integrity)

---

### ITEM-003: Government Identity Verification Fallback Strategy in Territories Lacking Direct Private-Sector APIs
- **Requirement Reference:** `PRD-003`, `docs/05_IDENTITY_AND_VERIFICATION.md`
- **Context:** The Product Owner requires government-operated identity verification systems only, explicitly rejecting generic commercial KYC vendors. However, sovereign verification availability varies dramatically:
  - **India:** UIDAI legally prohibits private commercial applications from accessing direct online Aadhaar Biometric/OTP authentication APIs (Supreme Court *Puttaswamy* ruling & Aadhaar Amendment Act 2019). However, **Offline Paperless e-KYC (digitally signed XML / Secure QR)** is legally permitted for private entities with explicit resident consent.
  - **United Kingdom:** The public-facing **GOV.UK One Login** is currently restricted to government digital services and is not yet open to private consumer mobile apps. Private entities must verify against the *UK Digital Identity and Attributes Trust Framework (DIATF)* certified providers who query authoritative government sources (HM Passport Office, DVLA).
  - **United States:** There is no unified federal identity API. Verification relies on state-level Mobile Driver's Licenses (ISO/IEC 18013-5 mDL) or AAMVA Driver's License Data Verification (DLDV), which requires state motor vehicle portal gateways.
- **Specific Decision Required:**
  If a sovereign government does not provide an open, direct, public REST API to private applications, does the Product Owner approve:
  - **Path A:** Authorized Offline Signed Credential Verification (e.g., verifying UIDAI digitally signed XML uploaded/scanned by the resident)?
  - **Path B:** Accredited Trust Framework Integrations that query sovereign government registries via legally certified gateway agents (e.g., UK DIATF)?
  - **Path C:** Restricting launch solely to jurisdictions where 100% direct citizen-downloaded signed credentials exist?
- **Engineering Recommendation:** Approve Path A (India) and Path B (UK/US Trust Framework Gateways), which strictly authenticate against official sovereign records without storing raw credentials.
- **Status:** `[Requires Legal & Technical Validation — Awaiting Product Owner Review]`
- **Impact Level:** Critical (Go-to-Market Viability, Legal Compliance)

---

### ITEM-004: Blinded Identity Nullifier Retention on Account Deletion
- **Requirement Reference:** `PRD-001`, `FUNC-ACT-005`
- **Context:** If a verified user permanently deletes their account, what happens to their Blinded Identity Nullifier ($\text{HMAC-SHA256}(\text{ServerPepper}, \text{GovID})$)?
- **Options Under Consideration:**
  - **Option 1: Immediate Purge of Nullifier.** The hash is deleted immediately from the database alongside all user data.
    - *Risk:* A bad actor who abuses the network, harasses users, or violates community standards could delete their account, instantly re-register with the same identity, and bypass bans or cooldown periods.
  - **Option 2 (Engineering Recommended): 90-Day Anti-Fraud Tainted Nullifier Tombstone.** When an account is deleted, all personal data, usernames, and cryptographic keys are erased immediately. The blinded hash is moved to a temporary `deleted_identity_tombstones` table for 90 days with an encrypted deletion timestamp. Any registration attempt within 90 days triggers a cooling-off restriction, after which the tombstone is purged.
  - **Option 3: Permanent Nullifier Blacklist.** The blinded hash is retained permanently as a record that this human identity has registered before.
    - *Risk:* May conflict with strict interpretation of GDPR "Right to Erasure" (Article 17).
- **Status:** `[Open — Awaiting Product Owner Sign-Off]`
- **Impact Level:** High (GDPR Privacy vs. Anti-Abuse Protection)

---

### ITEM-005: 365-Day Retention Execution Timing — Hourly Micro-Batches vs. Daily Scheduled Sweep
- **Requirement Reference:** `PRD-007`, `FUNC-RET-002`
- **Context:** Messages must mathematically expire after 365 days. Should the purge be executed continuously at the microsecond timestamp level, or through scheduled batch sweeps?
- **Options Under Consideration:**
  - **Option 1: Continuous Dynamic Deletion.** Any query filtering messages includes `WHERE created_at >= NOW() - INTERVAL '365 days'`, and background triggers purge records every minute.
    - *Risk:* High database CPU overhead; continuous table locking and index churn.
  - **Option 2 (Engineering Recommended): Daily 03:00 UTC Scheduled Sweep with Query Filter.**
    1. All API queries enforce `WHERE created_at >= NOW() - INTERVAL '365 days'`, ensuring expired messages are never readable, even by a fraction of a second.
    2. A background worker runs daily during low-traffic hours (03:00 UTC) to execute hard physical deletes and reclaim storage.
    3. Mobile clients execute Room WorkManager sweeps daily upon device idle/charging.
- **Status:** `[Proposed — Awaiting Product Owner Concurrence]`
- **Impact Level:** Medium (Database Performance & Maintenance)

---

## 3. Register Summary & Decision Matrix

| Question ID | Topic Area | Core Conflict | Proposed Recommendation | PO Decision |
| :---: | :--- | :--- | :--- | :---: |
| **ITEM-001** | Username Governance | Namespace availability vs. Contact Impersonation | 180-Day Quarantine Period | Pending |
| **ITEM-002** | Provisional Accounts | Frictionless Onboarding vs. Bot Infiltration | Max 3 Cold Outbound Contacts | Pending |
| **ITEM-003** | Sovereign Identity Routes | Private API Barriers vs. Government-Only Mandate | Offline XML (IN) + Trust Frameworks (UK/US)| Pending |
| **ITEM-004** | Account Deletion | GDPR Right to Erasure vs. Re-Registration Abuse | 90-Day Blinded Tombstone | Pending |
| **ITEM-005** | 365-Day Purge Engine | Real-time CPU Thrash vs. Daily Physical Prune | Query Barrier + Daily Prune Batch | Pending |
