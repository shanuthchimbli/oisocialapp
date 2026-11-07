# Comprehensive Automated Test Case Catalog: Oi (100+ Scenarios)

> **Document ID:** TEST-CATALOG-014  
> **Status:** Confirmed Specification  
> **Last Updated:** 2026-11-07  
> **Project Repository:** `https://github.com/shanuthchimbli/oisocialapp`  
> **Author:** QA Architect & SDET Group  
> **Coverage Scope:** 106 Formal Verification Scenarios across 24 Engineering Disciplines (A through X)  
> **Traceability Upstream:** [`docs/01_PRODUCT_REQUIREMENTS.md`](file:///Users/shanuthchimbli/.gemini/antigravity/scratch/oisocialapp/docs/01_PRODUCT_REQUIREMENTS.md), [`docs/02_FUNCTIONAL_REQUIREMENTS.md`](file:///Users/shanuthchimbli/.gemini/antigravity/scratch/oisocialapp/docs/02_FUNCTIONAL_REQUIREMENTS.md), [`docs/13_TESTING_STRATEGY.md`](file:///Users/shanuthchimbli/.gemini/antigravity/scratch/oisocialapp/docs/13_TESTING_STRATEGY.md)  

---

## 1. Domain A: Unit Testing (10 Scenarios)

| Test ID | Scenario Description & Prerequisites | Execution Action | Expected Result | Severity | Automation |
| :--- | :--- | :--- | :--- | :---: | :---: |
| `TEST-UNIT-001` | **Phone Number Normalization**<br/>Prereq: Clean JVM. | Pass various raw inputs: `+91 98765-43210`, `09876543210`, `(415) 555-0199`. | Normalized to strict E.164: `+919876543210`, `+14155550199`. | High | Automated |
| `TEST-UNIT-002` | **Phone Regex Boundary Rejection**<br/>Prereq: Clean JVM. | Pass malformed numbers: `+91123`, `abc`, `+99912345678`. | Throws `InvalidPhoneException`. | High | Automated |
| `TEST-UNIT-003` | **Username Regex Validation**<br/>Prereq: Clean JVM. | Pass valid handles: `aarav_99`, `oi_user`, `john`. | Passes validation without error. | Critical | Automated |
| `TEST-UNIT-004` | **Username Regex Invalid Characters**<br/>Prereq: Clean JVM. | Pass invalid: `_admin`, `user_`, `us__er`, `user@name`. | Throws `InvalidUsernameException`. | Critical | Automated |
| `TEST-UNIT-005` | **Status Text Length Validation**<br/>Prereq: Clean JVM. | Pass 0 chars, 140 chars, and 141 UTF-8 chars. | 0 and 141 chars fail; 140 chars succeeds. | High | Automated |
| `TEST-UNIT-006` | **Message Character Length Boundary**<br/>Prereq: Clean JVM. | Pass payload of 4,096 chars and 4,097 chars. | 4,096 chars accepted; 4,097 chars rejected. | High | Automated |
| `TEST-UNIT-007` | **UUIDv7 Monotonic Ordering**<br/>Prereq: Clean JVM. | Generate 1,000 sequential UUIDv7 instances. | Each subsequent UUID is strictly greater than prior. | Critical | Automated |
| `TEST-UNIT-008` | **Ratchet Sequence Counter Wrap-around**<br/>Prereq: Active ratchet. | Advance symmetric ratchet counter to $2^{31}-1$. | Handles increment cleanly without overflow. | Medium | Automated |
| `TEST-UNIT-009` | **Memory Buffer Zeroization**<br/>Prereq: Byte array buffer. | Call `CryptoUtils.wipe(buffer)`. | All bytes in array overwrite to `0x00`. | Critical | Automated |
| `TEST-UNIT-010` | **ViewState Monotonic State Emission**<br/>Prereq: MVI ViewModel. | Emit `Loading`, `Success`, `Error` intents. | UI StateFlow emits correct immutable states in order. | High | Automated |

---

## 2. Domain B: Integration Testing (5 Scenarios)

| Test ID | Scenario Description & Prerequisites | Execution Action | Expected Result | Severity | Automation |
| :--- | :--- | :--- | :--- | :---: | :---: |
| `TEST-INT-001` | **Room Encrypted Read/Write**<br/>Prereq: SQLCipher initialized. | Insert message entity; read back from SQLite. | Message matches plaintext; raw db file is encrypted. | Critical | Automated |
| `TEST-INT-002` | **Room Auto-Migration Test**<br/>Prereq: MigrationTestHelper. | Migrate schema from version 1 to 2. | Data preserved; new column indexed correctly. | High | Automated |
| `TEST-INT-003` | **Repository Offline Cache Flow**<br/>Prereq: Mock network offline. | Save message via Repository. | Message persists in Room with state `SENDING`. | Critical | Automated |
| `TEST-INT-004` | **Repository Network Sync Flush**<br/>Prereq: Queued local records. | Simulate network reconnect callback. | Repository reads queued items and sends over WS. | Critical | Automated |
| `TEST-INT-005` | **Redis Token Bucket Integration**<br/>Prereq: Testcontainers Redis. | Trigger 4 requests within 1 second. | 4th request returns rate-limit breach. | High | Automated |

---

## 3. Domain C: API Testing (6 Scenarios)

| Test ID | Scenario Description & Prerequisites | Execution Action | Expected Result | Severity | Automation |
| :--- | :--- | :--- | :--- | :---: | :---: |
| `TEST-API-001` | **OTP Request Contract**<br/>Prereq: API server running. | `POST /v1/auth/phone/otp-request` with valid phone. | Returns HTTP 200 with `requestId` and cooldown. | Critical | Automated |
| `TEST-API-002` | **OTP Verification Contract**<br/>Prereq: Active OTP request. | `POST /v1/auth/phone/otp-verify` with valid OTP. | Returns HTTP 200 with JWT and user state. | Critical | Automated |
| `TEST-API-003` | **Username Check Available**<br/>Prereq: Unclaimed handle. | `GET /v1/users/username-check?u=newuser`. | Returns HTTP 200 `{ "isAvailable": true }`. | High | Automated |
| `TEST-API-004` | **Username Check Taken**<br/>Prereq: Existing handle `@aarav`. | `GET /v1/users/username-check?u=aarav`. | Returns HTTP 200 `{ "isAvailable": false }`. | High | Automated |
| `TEST-API-005` | **Status Update Contract**<br/>Prereq: Authenticated session. | `PUT /v1/status` with 50-char text. | Returns HTTP 200 `{ "statusText": "..." }`. | High | Automated |
| `TEST-API-006` | **Status Delete Contract**<br/>Prereq: Existing status update. | `DELETE /v1/status`. | Returns HTTP 204 No Content. | High | Automated |

---

## 4. Domain D: Database Testing (6 Scenarios)

| Test ID | Scenario Description & Prerequisites | Execution Action | Expected Result | Severity | Automation |
| :--- | :--- | :--- | :--- | :---: | :---: |
| `TEST-DB-001` | **PostgreSQL Citext Case Uniqueness**<br/>Prereq: Postgres running. | Insert `@alpha`; attempt insert `@ALPHA`. | Second insert fails with unique violation. | Critical | Automated |
| `TEST-DB-002` | **Single Active Device Partial Index**<br/>Prereq: Device 1 active. | Insert Device 2 for same user with `is_active=TRUE`. | Fails on `idx_single_active_device` unique index. | Critical | Automated |
| `TEST-DB-003` | **Identity Nullifier Uniqueness**<br/>Prereq: Existing nullifier. | Insert duplicate 64-byte nullifier hash. | Fails on `identity_nullifier` unique constraint. | Critical | Automated |
| `TEST-DB-004` | **Transit Message 30-Day Purge Query**<br/>Prereq: Records dated >30 days. | Run transit purge query. | Older records deleted; fresh records untouched. | High | Automated |
| `TEST-DB-005` | **PostgreSQL Foreign Key Cascade**<br/>Prereq: User with devices. | Delete user record. | Associated devices cascade-deleted. | High | Automated |
| `TEST-DB-006` | **Username Quarantine Table Expiry**<br/>Prereq: Handle quarantined. | Query eligible handles where `eligible_release_at <= NOW()`. | Identifies expired quarantined handles. | High | Automated |

---

## 5. Domain E: Android UI Functional Testing (6 Scenarios)

| Test ID | Scenario Description & Prerequisites | Execution Action | Expected Result | Severity | Automation |
| :--- | :--- | :--- | :--- | :---: | :---: |
| `TEST-UI-001` | **Phone Input Screen Form Validation**<br/>Prereq: Fresh launch. | Type 5 digits; inspect *Continue* button. | Button remains disabled until full valid number. | High | Automated |
| `TEST-UI-002` | **OTP Auto-Advancing Focus**<br/>Prereq: OTP screen open. | Enter 6 digits sequentially. | Each digit advances focus; 6th triggers submit. | High | Automated |
| `TEST-UI-003` | **Conversation List Rendering**<br/>Prereq: 3 conversations in DB. | Open Home Activity. | 3 conversation items rendered in order. | Critical | Automated |
| `TEST-UI-004` | **Message Input Character Counter**<br/>Prereq: Chat open. | Type 100 characters in message bar. | Counter updates dynamically; input scrolls. | Medium | Automated |
| `TEST-UI-005` | **Status Input Length Warning**<br/>Prereq: Status edit screen. | Type 140 chars; attempt 141st char. | 141st character prevented by text field filter. | High | Automated |
| `TEST-UI-006` | **FLAG_SECURE Screen Shield**<br/>Prereq: Chat activity active. | Call Android `getWindowManager().flags`. | `FLAG_SECURE` bit is asserted. | Critical | Automated |

---

## 6. Domain F: End-to-End Testing (5 Scenarios)

| Test ID | Scenario Description & Prerequisites | Execution Action | Expected Result | Severity | Automation |
| :--- | :--- | :--- | :--- | :---: | :---: |
| `TEST-E2E-001` | **Full 1:1 Message Delivery Lifecycle**<br/>Prereq: Alice & Bob registered. | Alice sends "Hello Bob"; Bob online. | Alice UI transitions: `SENDING` $\to$ `SENT` $\to$ `DELIVERED`. | Critical | Automated |
| `TEST-E2E-002` | **Read Receipt Lifecycle**<br/>Prereq: Message delivered to Bob. | Bob opens conversation with Alice. | Alice UI transitions from `DELIVERED` to `READ` (Blue tick). | Critical | Automated |
| `TEST-E2E-003` | **Asynchronous Offline Delivery**<br/>Prereq: Bob offline. | Alice sends message; Bob launches app later. | Bob receives message on sync; Alice gets `DELIVERED`. | Critical | Automated |
| `TEST-E2E-004` | **Blocked Contact Message Rejection**<br/>Prereq: Alice blocked Bob. | Bob sends message to Alice. | Bob sees single check; Alice receives zero message. | Critical | Automated |
| `TEST-E2E-005` | **Status Update Propagation**<br/>Prereq: Alice & Bob mutual contacts. | Alice updates status; Bob opens status tab. | Bob sees Alice's new text status. | High | Automated |

---

## 7. Domain G: Authentication Testing (6 Scenarios)

| Test ID | Scenario Description & Prerequisites | Execution Action | Expected Result | Severity | Automation |
| :--- | :--- | :--- | :--- | :---: | :---: |
| `TEST-AUTH-001` | **OTP Expiry Enforcement**<br/>Prereq: OTP created at $t=0$. | Submit OTP at $t = 301\text{ seconds}$. | Rejected with HTTP 401 `ERR_OTP_EXPIRED`. | Critical | Automated |
| `TEST-AUTH-002` | **3 Failed OTP Attempts Lockout**<br/>Prereq: Active OTP session. | Submit 3 wrong OTP codes consecutively. | 3rd fails; 4th rejected with `ERR_MAX_ATTEMPTS`. | Critical | Automated |
| `TEST-AUTH-003` | **Resend Cooldown Enforcement**<br/>Prereq: OTP dispatched. | Request resend at $t = 30\text{ seconds}$. | Rejected with HTTP 429 (Cooldown 30s remaining). | High | Automated |
| `TEST-AUTH-004` | **Hourly Phone Rate Limiting**<br/>Prereq: Clean phone number. | Request 4 OTPs within 60 minutes. | 4th attempt rejected with `ERR_RATE_LIMIT`. | High | Automated |
| `TEST-AUTH-005` | **JWT Token Expiration**<br/>Prereq: Token issued with 15m TTL. | Call protected API with 16-minute old token. | Returns HTTP 401 Unauthorized. | Critical | Automated |
| `TEST-AUTH-006` | **Token Refresh Flow**<br/>Prereq: Valid refresh token. | Call `/v1/auth/token/refresh`. | Returns fresh 15-minute access token. | High | Automated |

---

## 8. Domain H: Identity Verification Testing (8 Scenarios)

| Test ID | Scenario Description & Prerequisites | Execution Action | Expected Result | Severity | Automation |
| :--- | :--- | :--- | :--- | :---: | :---: |
| `TEST-ID-001` | **Valid UIDAI XML Verification**<br/>Prereq: Genuine signed XML. | Submit XML with valid 4-digit share code. | Signature validates; account set to `VERIFIED`. | Critical | Automated |
| `TEST-ID-002` | **Tampered UIDAI XML Signature**<br/>Prereq: Alter 1 byte in XML. | Submit tampered XML. | Fails with `ERR_UIDAI_SIGNATURE_INVALID`. | Critical | Automated |
| `TEST-ID-003` | **Expired UIDAI XML Package**<br/>Prereq: XML dated 80 hours ago. | Submit stale XML package. | Rejected with `ERR_DOCUMENT_EXPIRED`. | High | Automated |
| `TEST-ID-004` | **Duplicate Identity Rejection (OPOA)**<br/>Prereq: Citizen ID registered. | Submit identical XML for new account. | Rejected with `ERR_IDENTITY_DUPLICATE`. | Critical | Automated |
| `TEST-ID-005` | **14-Day Grace Countdown**<br/>Prereq: Provisional account at day 10. | Check account state via API. | State is `PROVISIONAL`; 4 days remaining. | High | Automated |
| `TEST-ID-006` | **Grace Period Expiry Suspension**<br/>Prereq: Account at day 14 + 1 sec. | Background cron runs. | Account state transitions to `SUSPENDED`. | Critical | Automated |
| `TEST-ID-007` | **Provisional 3-Contact Outbound Limit**<br/>Prereq: Provisional user. | Initiate chat with 4th distinct user. | 4th message blocked with `ERR_PROVISIONAL_LIMIT`. | Critical | Automated |
| `TEST-ID-008` | **5-Day Cooldown Post Deletion**<br/>Prereq: Unverified account closed. | Register same phone on day 3. | Fails with `ERR_REGISTRATION_COOLDOWN_ACTIVE`. | High | Automated |

---

## 9. Domain I: Encryption & Protocol Testing (8 Scenarios)

| Test ID | Scenario Description & Prerequisites | Execution Action | Expected Result | Severity | Automation |
| :--- | :--- | :--- | :--- | :---: | :---: |
| `TEST-CRYPTO-001`| **X3DH Shared Secret Agreement**<br/>Prereq: Prekey bundle fetched. | Alice & Bob execute X3DH derivation. | Both derive identical 32-byte master secret $SK$. | Critical | Automated |
| `TEST-CRYPTO-002`| **Double Ratchet Forward Secrecy**<br/>Prereq: 10 messages sent. | Leak message key $MK_{10}$; inspect $MK_1 \dots MK_9$. | Past message keys cannot be computed from $MK_{10}$. | Critical | Automated |
| `TEST-CRYPTO-003`| **Post-Compromise Security Recovery**<br/>Prereq: Ratchet state leaked. | Execute asymmetric DH turn; send new msg. | New message cannot be decrypted with leaked state. | Critical | Automated |
| `TEST-CRYPTO-004`| **AES-256-GCM Tamper Detection**<br/>Prereq: Encrypted frame. | Flip single bit in ciphertext payload. | Decryption fails with authentication tag mismatch. | Critical | Automated |
| `TEST-CRYPTO-005`| **Associated Data Binding Tamper**<br/>Prereq: Valid frame. | Modify sender UUID in frame header. | GCM tag verification fails; packet dropped. | Critical | Automated |
| `TEST-CRYPTO-006`| **Replay Attack Rejection**<br/>Prereq: Frame delivered. | Inject identical frame back into WebSocket. | Client discards packet on sequence deduplication. | Critical | Automated |
| `TEST-CRYPTO-007`| **Out-of-Order Frame Reassembly**<br/>Prereq: Frames 1, 2, 3 sent. | Deliver frames in order: 3, 1, 2. | Client queues and reassembles in correct order. | High | Automated |
| `TEST-CRYPTO-008`| **Prekey Exhaustion Replenishment**<br/>Prereq: OPK pool down to 10. | Client syncs with server. | Client automatically generates and uploads 90 OPKs. | High | Automated |

---

## 10. Domain J: Security Testing (6 Scenarios)

| Test ID | Scenario Description & Prerequisites | Execution Action | Expected Result | Severity | Automation |
| :--- | :--- | :--- | :--- | :---: | :---: |
| `TEST-SEC-001` | **Certificate Pinning Failure**<br/>Prereq: Mock proxy CA cert. | Connect Android client via proxy. | OkHttp terminates connection with SSLPeerUnverified. | Critical | Automated |
| `TEST-SEC-002` | **Cleartext Traffic Rejection**<br/>Prereq: Client app build. | Attempt `http://` unencrypted request. | Blocked by Android manifest network policy. | Critical | Automated |
| `TEST-SEC-003` | **SQL Injection via Username Search**<br/>Prereq: Search endpoint. | Query `?u=' OR '1'='1`. | Parameterized query handles input as literal string. | Critical | Automated |
| `TEST-SEC-004` | **XSS in Status Update**<br/>Prereq: Status endpoint. | Submit `<script>alert(1)</script>`. | Text stored and displayed as harmless literal text. | High | Automated |
| `TEST-SEC-005` | **Root / Substrate Tamper Detection**<br/>Prereq: Rooted test emulator. | Launch app. | Displays root warning / blocks keystore access. | High | Automated |
| `TEST-SEC-006` | **Android Keystore Extraction Block**<br/>Prereq: Physical device ADB. | Attempt `keyStore.getEntry()` without auth. | Key private material is non-exportable from TEE. | Critical | Automated |

---

## 11. Domain K: Privacy & Data Minimization (4 Scenarios)

| Test ID | Scenario Description & Prerequisites | Execution Action | Expected Result | Severity | Automation |
| :--- | :--- | :--- | :--- | :---: | :---: |
| `TEST-PRIV-001` | **Zero Plaintext on Server DB**<br/>Prereq: 100 messages routed. | Execute full text search on PostgreSQL. | Zero matches for conversational plaintext words. | Critical | Automated |
| `TEST-PRIV-002` | **Zero Raw Aadhaar Storage**<br/>Prereq: 50 users verified. | Query `users` table for 12-digit patterns. | Zero Aadhaar numbers found; only nullifier hashes. | Critical | Automated |
| `TEST-PRIV-003` | **Server Log Heap Inspection**<br/>Prereq: Debug log level. | Inspect container stdout logs. | Logs contain zero phone numbers, IDs, or text. | Critical | Automated |
| `TEST-PRIV-004` | **Third-Party Tracker Absence**<br/>Prereq: APK build artifact. | Scan APK DEX files for known tracker SDKs. | Zero trackers (Google Ads, Facebook, Adjust) found. | High | Automated |

---

## 12. Domain L: Performance Benchmarking (6 Scenarios)

| Test ID | Scenario Description & Prerequisites | Execution Action | Expected Result | Severity | Automation |
| :--- | :--- | :--- | :--- | :---: | :---: |
| `TEST-PERF-001` | **Cold App Launch Under 1s**<br/>Prereq: Process killed (Pixel 4a). | Measure icon click to first interactive frame. | Cold launch duration $< 800\text{ ms}$ (Cap: 1,000ms). | Critical | Automated |
| `TEST-PERF-002` | **Warm Launch Under 500ms**<br/>Prereq: App backgrounded. | Resume app to foreground. | Warm launch duration $< 250\text{ ms}$ (Cap: 500ms). | High | Automated |
| `TEST-PERF-003` | **Cached Conversation List Render**<br/>Prereq: 50 cached threads. | Render Home Screen. | Compose drawing time $< 50\text{ ms}$ (Cap: 100ms). | High | Automated |
| `TEST-PERF-004` | **Conversation Screen Load**<br/>Prereq: 1,000 cached messages. | Open conversation screen. | Render duration $< 50\text{ ms}$ (Cap: 100ms). | High | Automated |
| `TEST-PERF-005` | **Optimistic UI Appearance**<br/>Prereq: Chat open. | Tap Send button. | Clock bubble renders in UI in $< 25\text{ ms}$ (Cap: 50ms). | Critical | Automated |
| `TEST-PERF-006` | **60 FPS Scrolling Rate**<br/>Prereq: 500 messages list. | Execute 5,000px fast fling scroll. | 0 dropped frames; frame time $\le 16.6\text{ ms}$. | High | Automated |

---

## 13. Domains M through X: Infrastructure, Retention, Resilience & Ops (35 Scenarios)

### Domain M: Load Testing (4 Scenarios)
- `TEST-LOAD-001`: **100k Concurrent WebSockets:** Single gateway pod sustains 100,000 connected idle sockets with $<200\text{ MB}$ RAM.
- `TEST-LOAD-002`: **5,000 Messages/Sec Throughput:** Server relays 5,000 msg/sec with p95 transit latency $<350\text{ ms}$.
- `TEST-LOAD-003`: **Redis Pub/Sub Fan-out:** Broadcast to 10 gateway nodes delivers within 5 ms.
- `TEST-LOAD-004`: **Database Connection Pool Saturation:** 1,000 concurrent API requests execute cleanly via PgBouncer without timeouts.

### Domain N: Stress Testing (3 Scenarios)
- `TEST-STR-001`: **Gateway CPU Exhaustion (95% Load):** Gateway queues traffic gracefully without dropping connected sockets.
- `TEST-STR-002`: **Redis Node Failover:** Master Redis node terminates; cluster promotes replica within 3 seconds with zero message loss.
- `TEST-STR-003`: **OOM Killer Recovery:** Gateway pod killed by Kubernetes; replica schedules and absorbs traffic in $\le 5\text{ s}$.

### Domain O: Network Interruption Testing (4 Scenarios)
- `TEST-NET-001`: **Sudden Airplane Mode:** User sends message while switching to airplane mode; message queues cleanly in Room with state `SENDING`.
- `TEST-NET-002`: **Cellular to Wi-Fi Handover:** IP switch during message transmit re-establishes TLS 1.3 in $<500\text{ ms}$ without dropping frame.
- `TEST-NET-003`: **High Packet Loss (30% Loss):** WebSocket TCP retransmissions deliver frames without session collapse.
- `TEST-NET-004`: **Jittered Exponential Reconnect:** Disconnected client reconnects with random jitter preventing thundering herd.

### Domain P: Offline Synchronization Testing (4 Scenarios)
- `TEST-SYNC-001`: **10 Queued Offline Messages:** Client goes offline, queues 10 messages, reconnects; all 10 transmit sequentially in FIFO order.
- `TEST-SYNC-002`: **Sync ACK Resolution:** Client receives ACKs upon reconnect, updating all 10 messages from `SENDING` to `SENT`.
- `TEST-SYNC-003`: **Conflict-Free Sequence:** Interleaving offline sent messages with inbound received messages maintains strict chronological order.
- `TEST-SYNC-004`: **Corrupted Cache Recovery:** App restarts cleanly if local WAL file is truncated abruptly during power cut.

### Domain Q: Battery Testing (3 Scenarios)
- `TEST-BAT-001`: **24-Hour Idle Battery Consumption:** Android app in background for 24 hours consumes $<1.5\%$ total battery.
- `TEST-BAT-002`: **Doze Mode Compliance:** App sleeps during Doze Mode; wakes up only for high-priority FCM silent push.
- `TEST-BAT-003`: **Zero WakeLock Leakage:** Battery Historian confirms zero lingering partial wakelocks after message send.

### Domain R: Memory Testing (3 Scenarios)
- `TEST-MEM-001`: **Active Chat Memory Cap:** Chat with 1,000 items consumes $<50\text{ MB}$ RSS on low-end test device.
- `TEST-MEM-002`: **Zero LeakCanary Alerts:** Navigating between 20 conversation screens generates zero Activity/ViewModel memory leaks.
- `TEST-MEM-003`: **Bitmap Zero Memory:** Standardized initial avatars consume negligible memory compared to image avatars.

### Domain S: Device Compatibility Testing (3 Scenarios)
- `TEST-DEV-001`: **Minimum SDK (Android 8.0 / API 26):** App launches, authenticates, and exchanges encrypted messages on Android 8.0 emulator.
- `TEST-DEV-002`: **Target SDK (Android 14 / API 34):** Predictive back gesture and Android 14 notification permissions operate correctly.
- `TEST-DEV-003`: **Small Screen Layout (320dp Width):** Compose UI scales gracefully without horizontal text truncation on narrow displays.

### Domain T: Upgrade and Migration Testing (3 Scenarios)
- `TEST-MIG-001`: **App Update Without Data Loss:** Upgrading APK version preserves all local encrypted messages and prekeys.
- `TEST-MIG-002`: **Database Schema Upgrade:** Room SQLite migrates columns seamlessly without triggering destructive fallback.
- `TEST-MIG-003`: **Server API Backward Compatibility:** Server v1.1 accepts requests from mobile client v1.0 without errors.

### Domain U: Message Retention Testing (4 Scenarios)
- `TEST-RET-001`: **Local 365-Day Automatic Purge:** Messages created at $t = \text{now} - 366\text{ days}$ are deleted by daily WorkManager sweep.
- `TEST-RET-002`: **Client Query Barrier:** Local DAO query excludes records older than 365 days even before vacuum sweep runs.
- `TEST-RET-003`: **Server Transit Drop at 30 Days:** Server purge cron erases undelivered transit messages exceeding 30 days.
- `TEST-RET-004`: **SQLite Vacuum Execution:** `VACUUM` reclaims storage pages and zeroizes unallocated space post-purge.

### Domain V: Account Recovery Testing (4 Scenarios)
- `TEST-REC-001`: **Lost SIM Recovery Happy Path:** User with lost SIM enters new phone, proves identity via Aadhaar XML, and recovers account.
- `TEST-REC-002`: **Recovery Revokes Old Device:** Completing recovery immediately sends revocation push and kills old device session.
- `TEST-REC-003`: **Username Retained on Recovery:** User's unique handle remains bound to their permanent UUIDv7 post-recovery.
- `TEST-REC-004`: **Identity Mismatch Rejection:** Attempting recovery with a different government document fails to unlock the account.

### Domain W: Deployment Testing (2 Scenarios)
- `TEST-DEP-001`: **Zero-Downtime Rolling Update:** Deploying new gateway pods maintains active WebSocket sessions without disconnect spikes.
- `TEST-DEP-002`: **Database Migration Rollback:** Liquibase/Flyway schema rollback executes cleanly in staging pipeline.

### Domain X: Backup and Restore Testing (2 Scenarios)
- `TEST-BKP-001`: **PostgreSQL Point-in-Time Recovery (PITR):** Database restored to exact state 10 minutes prior with zero transaction corruption.
- `TEST-BKP-002`: **Disaster Recovery RTO Verification:** Standby database cluster promotes to primary within 5 minutes.
