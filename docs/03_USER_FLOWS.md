# User Flows & State Transition Specifications: Oi

> **Document ID:** FLOW-003-CORE  
> **Status:** Confirmed Baseline  
> **Last Updated:** 2026-11-07  
> **Project Repository:** `https://github.com/shanuthchimbli/oisocialapp`  
> **Author:** Mobile UX Architect & Lead Systems Engineer  
> **Traceability Upstream:** [`docs/01_PRODUCT_REQUIREMENTS.md`](file:///Users/shanuthchimbli/.gemini/antigravity/scratch/oisocialapp/docs/01_PRODUCT_REQUIREMENTS.md), [`docs/02_FUNCTIONAL_REQUIREMENTS.md`](file:///Users/shanuthchimbli/.gemini/antigravity/scratch/oisocialapp/docs/02_FUNCTIONAL_REQUIREMENTS.md)  

---

## 1. Executive Summary & Flow Index

This document maps all user journeys, state transitions, interactive edge cases, and failure recoveries for **Oi**. Each flow defines:
1. **Entry Criteria & Preconditions**
2. **Step-by-Step Interactions & UI State Transitions**
3. **Happy Path Sequence**
4. **Error & Edge-Case Branches**
5. **Post-Conditions & Cryptographic Side-Effects**

### Master Flow Directory
- **Flow 1:** New User Registration & SMS Phone Authentication
- **Flow 2:** Sovereign Identity Verification (India, UK, USA)
- **Flow 3:** 14-Day Grace Window & Provisional Account Constraints
- **Flow 4:** Text-Only Status Management (Publish, Update, Delete)
- **Flow 5:** Real-Time 1:1 End-to-End Encrypted Chat
- **Flow 6:** Offline Message Queuing & Reconnection Synchronization
- **Flow 7:** Controlled Username Modification & Quarantine
- **Flow 8:** Single Device Login & Remote Device Invalidation
- **Flow 9:** Lost Phone Number & Account Recovery via Re-Verification
- **Flow 10:** User Blocking & Abuse Reporting
- **Flow 11:** 365-Day Message Lifecycle Purge
- **Flow 12:** Permanent Account Deletion & Cryptographic Teardown

---

## 2. Flow 1: New User Registration & Phone Verification

```mermaid
stateDiagram-v2
    [*] --> Splash: Cold Launch
    Splash --> PhoneInput: User not authenticated
    PhoneInput --> OTPWaiting: Valid E.164 (+91, +44, +1) entered
    PhoneInput --> PhoneInput: Invalid format / Carrier error
    
    OTPWaiting --> ProfileSetup: Valid 6-digit OTP entered
    OTPWaiting --> OTPWaiting: Invalid OTP (Attempts 1-2)
    OTPWaiting --> PhoneInput: 3 Failed attempts / Locked 15 mins
    
    ProfileSetup --> ProvisionalHome: Display name & Unique username chosen
    ProvisionalHome --> [*]
```

### Flow 1 Specification

| Phase | User Action / Trigger | System Response & UI State | Backend / Cryptographic Action |
| :--- | :--- | :--- | :--- |
| **1.1 Input** | User enters mobile number with country prefix (`+91`, `+44`, `+1`). Taps *Continue*. | Validates E.164 pattern. Disables button; shows loading spinner. | `POST /v1/auth/phone/otp-request`. Redis checks IP and phone rate limits. Generates 6-digit CSPRNG OTP. Dispatches SMS via regional carrier. |
| **1.2 Verify**| User inputs 6-digit OTP received via SMS. | Auto-advances focus on digit input; submits automatically on 6th digit. | `POST /v1/auth/phone/otp-verify`. Validates OTP against Redis hash. Generates initial session JWT. |
| **1.3 Profile**| User enters Display Name (1–50 chars) and desired Username (3–30 chars). | Real-time debounced username availability check ($\le 200\text{ ms}$). Shows green checkmark if available. | `GET /v1/users/username-check?u=handle`. Verifies against PostgreSQL `citext` index and reserved blacklist. |
| **1.4 Keys** | User taps *Complete Registration*. | Client generates cryptographic identity keys in hardware Keystore. | Generates Curve25519 Identity Key ($IK$), Signed Prekey ($SPK$), and 100 One-Time Prekeys ($OPK$). Uploads public prekey bundle to server. Account state set to `PROVISIONAL`. |

---

## 3. Flow 2: Sovereign Identity Verification

```mermaid
flowchart TD
    A["Provisional Account (Home Screen)"] --> B["Tap 'Verify Identity' Banner"]
    B --> C{"Select Country Jurisdiction"}
    
    C -->|India +91| D["UIDAI Offline e-KYC (XML Upload / Scan)"]
    C -->|UK +44| E["GOV.UK / DIATF Certified Gateway"]
    C -->|USA +1| F["mDL (ISO/IEC 18013-5) / AAMVA Gateway"]
    
    D --> G["Resident inputs 4-digit Share Code"]
    G --> H["Verify Digital Signature of Sovereign Body"]
    E --> H
    F --> H
    
    H --> I{"Signature & Timestamp Valid?"}
    I -->|No| J["Display Specific Verification Error (Retry allowed)"]
    I -->|Yes| K["Compute HMAC-SHA256 Blinded Nullifier"]
    
    K --> L{"Nullifier Exists in DB?"}
    L -->|Yes| M["Reject: ERR_IDENTITY_DUPLICATE (1-Person-1-Account Violation)"]
    L -->|No| N["Save Nullifier. Set State to VERIFIED. Grant Verified Badge."]
```

### Flow 2 Error Handling & Edge Cases
- **Tampered XML Package:** Server verifies UIDAI's RSA-2048 public certificate. If any byte is altered, verification fails immediately with `ERR_SIGNATURE_VERIFICATION_FAILED`.
- **Stale XML (>72 hours old):** Rejected with `ERR_DOCUMENT_EXPIRED`. User is prompted to download a fresh XML from `myaadhaar.uidai.gov.in`.
- **Duplicate Human Identity:** If the computed nullifier matches an existing account, the user is notified: *"This government identity is already registered to an existing Oi account. If you lost access to your prior account, please initiate Account Recovery."*

---

## 4. Flow 3: 14-Day Grace Window & Provisional Account Constraints

```mermaid
stateDiagram-v2
    [*] --> ProvisionalState: Registration Completed
    ProvisionalState --> VerifiedState: Completed Sovereign KYC
    
    state ProvisionalState {
        [*] --> OutboundQuotaCheck
        OutboundQuotaCheck --> AllowMessage: Unique Contacts < 3
        OutboundQuotaCheck --> BlockMessage: Unique Contacts >= 3
    }
    
    ProvisionalState --> SuspendedState: t > 14 Days (336 Hours)
    SuspendedState --> VerifiedState: KYC Completed during Suspension
    SuspendedState --> ClosedState: t > 30 Days in Suspension
    ClosedState --> 5DayCooldown: Account Purged
    5DayCooldown --> [*]: Can Register Anew
```

### Flow 3 Rules
1. **Outbound Messaging Cap:** A provisional user can initiate conversations with a maximum of **3 unique contacts**. Once a recipient replies, conversation is unlocked. Cold outbound outreach to a 4th contact triggers an in-app prompt: *"Complete Identity Verification to message more people."*
2. **Countdown Banner:** A persistent top-bar banner displays: *"Verified Identity required in X days. Tap to verify."*
3. **Suspension Lockdown:** At $t = 336\text{ hours}$, background cron transitions state to `SUSPENDED`. WebSocket connection drops; all message inputs are locked. Only the *Verify Identity* screen remains accessible.

---

## 5. Flow 4: Text-Only Status Management

```mermaid
sequenceDiagram
    autonumber
    actor User as Android Client
    participant App as Oi App UI
    participant Store as Local Room DB
    participant API as Status Microservice
    participant Redis as Redis Cache

    User->>App: Opens Status Tab -> Taps "Set Status"
    App->>App: Displays 140-char text input with remaining character counter
    User->>App: Types "Working remotely today - text only" (40 chars)
    User->>App: Taps "Post Status"
    App->>Store: Saves status locally with timestamp
    App->>API: PUT /v1/status { text: "...", privacy: "CONTACTS_ONLY" }
    API->>Redis: Set user status key (indefinite TTL)
    API-->>App: 200 OK { updatedAt: "..." }
    App-->>User: Status displayed on profile & contacts' feeds
```

### Status Deletion Flow
- User taps *Clear Status*.
- App requests confirmation modal: *"Remove your current status?"*
- Upon confirmation: `DELETE /v1/status`.
- Redis erases the key; local Room DB clears the record. Status vanishes from all contacts' screens upon their next sync.

---

## 6. Flow 5: Real-Time 1:1 End-to-End Encrypted Chat

```mermaid
sequenceDiagram
    autonumber
    actor Alice as Alice (Sender)
    participant AliceApp as Alice's Oi Client
    participant Server as Oi WebSocket Relay
    participant BobApp as Bob's Oi Client
    actor Bob as Bob (Recipient)

    Alice->>AliceApp: Types "Meeting at 4pm?" -> Taps Send
    AliceApp->>AliceApp: (1) Render optimistically in UI (clock icon, <50ms)
    AliceApp->>AliceApp: (2) Ratchet encryption: Double Ratchet with Bob's ratchet key
    AliceApp->>AliceApp: (3) Persist encrypted payload to Room (state: SENDING)
    AliceApp->>Server: (4) WS Frame: MSG_SUBMIT { msgId: UUIDv7, ciphertext: "...", seq: 42 }
    Server-->>AliceApp: (5) WS Frame: MSG_ACK { msgId, serverSeq: 1098 } -> UI updates to SENT (single tick)
    
    Server->>BobApp: (6) WS Frame: MSG_DELIVER { msgId, sender: Alice, ciphertext: "..." }
    BobApp->>BobApp: (7) Decrypt via Double Ratchet ratchet key
    BobApp->>BobApp: (8) Persist decrypted message to Room (SQLCipher)
    BobApp->>Server: (9) WS Frame: MSG_DELIVERED_ACK { msgId }
    Server->>AliceApp: (10) WS Frame: MSG_DELIVERED_RECEIPT { msgId } -> UI updates to DELIVERED (double tick)
    
    Bob->>BobApp: Opens conversation with Alice
    BobApp->>Server: (11) WS Frame: MSG_READ_ACK { msgId }
    Server->>AliceApp: (12) WS Frame: MSG_READ_RECEIPT { msgId } -> UI updates to READ (solid/colored double tick)
```

---

## 7. Flow 6: Offline Message Queuing & Reconnection

```mermaid
flowchart TD
    A["User taps Send while in Airplane Mode"] --> B["Generate UUIDv7 & Message Timestamp"]
    B --> C["Encrypt via active local ratchet session"]
    C --> D["Persist to local Room DB with state: SENDING"]
    D --> E["Optimistic UI draws bubble with Clock icon (<50ms)"]
    
    E --> F{"Network Monitor detects connectivity"}
    F -->|Offline| E
    F -->|Connected| G["Establish TLS 1.3 WebSocket with Server"]
    G --> H["Authenticate WS Session with short-lived Token"]
    H --> I["Worker queries Room: SELECT * WHERE state = 'SENDING' ORDER BY timestamp ASC"]
    I --> J["Transmit queued frames sequentially over WebSocket"]
    J --> K["Receive MSG_ACK for each frame"]
    K --> L["Update Room DB state: SENDING -> SENT"]
    L --> M["UI updates Clock icon to Single Checkmark"]
```

---

## 8. Flow 7: Controlled Username Modification & Quarantine

```mermaid
flowchart TD
    A["User initiates Username Change in Settings"] --> B["Input New Username: @new_handle"]
    B --> C{"Check Client Regex: ^[a-z0-9][a-z0-9_]{1,28}[a-z0-9]$"}
    C -->|Invalid| D["Display format error"]
    C -->|Valid| E["API: GET /v1/users/username-check?u=new_handle"]
    
    E --> F{"Check 30-Day Cooldown"}
    F -->|Last changed < 30 days ago| G["Reject: ERR_USERNAME_CHANGE_COOLDOWN"]
    F -->|Cooldown Expired| H{"Check Reserved Blacklist & DB Citext"}
    
    H -->|Taken / Reserved| I["Reject: ERR_USERNAME_UNAVAILABLE"]
    H -->|Available| J["Show Confirmation Modal: 'You cannot change this again for 30 days.'"]
    
    J --> K["User Confirms Change"]
    K --> L["Atomic DB Transaction: Update handle; Move @old_handle to Quarantine Table (TTL: 180 Days)"]
    L --> M["Notify active chat contacts of handle update in conversation thread"]
```

---

## 9. Flow 8: Single Device Login & Remote Invalidation

```mermaid
sequenceDiagram
    autonumber
    actor User as User on Device B (New Device)
    participant DevB as Device B (Android)
    participant Server as Oi Backend Core
    participant FCM as Firebase Cloud Messaging
    participant DevA as Device A (Old Device)

    User->>DevB: Completes SMS OTP & Authenticates
    DevB->>Server: POST /v1/auth/device-register { deviceId: "dev_B", newPrekeys: [...] }
    
    Note over Server: Server terminates active session for Device A
    Server->>Server: Invalidate Device A JWT & Refresh tokens in Redis
    Server->>Server: Delete Device A published prekeys
    Server->>FCM: High-Priority Data Push to Device A { action: "SESSION_TERMINATED" }
    Server-->>DevB: 200 OK { session: "jwt_dev_B", status: "ACTIVE" }
    
    FCM->>DevA: Wakes background receiver
    DevA->>DevA: (1) Zeroize cryptographic ratchet keys in RAM
    DevA->>DevA: (2) Clear cached session tokens
    DevA->>DevA: (3) Lock UI with non-dismissible modal: "Logged out on another device"
```

---

## 10. Flow 9: Account Recovery (Lost Phone Number)

```mermaid
flowchart TD
    A["User enters New Phone Number on new SIM"] --> B["Complete SMS OTP on New Number"]
    B --> C["User taps 'Recover Existing Account'"]
    C --> D["Submit Sovereign Identity Verification (UIDAI / DIATF / mDL)"]
    D --> E["Server validates sovereign digital signature"]
    E --> F["Compute Blinded Identity Nullifier"]
    
    F --> G{"Nullifier matches existing account in DB?"}
    G -->|No Match| H["Error: No account found matching this verified identity. Registration permitted as new user."]
    G -->|Match Found| I["Confirm Account Ownership (UUIDv7 retrieved)"]
    
    I --> J["Atomic Transaction: Map new phone number to existing UUIDv7; Disassociate old phone; Revoke all prior device sessions"]
    J --> K["Download User Profile (Display Name, Username maintained)"]
    K --> L["Device generates fresh E2EE prekey bundle and registers with server"]
    L --> M["Account restored to full VERIFIED status"]
```

---

## 11. Flow 10: User Blocking & Abuse Reporting

### 11.1 Blocking Flow
1. User Alice opens conversation settings with Bob $\to$ Taps *Block User*.
2. Modal confirmation: *"Block @bob? They cannot message you or see your status."*
3. Alice confirms $\to$ Local Room DB inserts record into `blocked_users` table; API called `POST /v1/users/block { targetUserId: "uuid_bob" }`.
4. Subsequent messages from Bob are dropped silently by server router. Bob's UI remains at single tick (`SENT`) with zero delivery receipt.

### 11.2 Abuse Reporting Flow
1. User Alice taps *Report Contact* $\to$ Selects reason (Spam, Harassment, Impersonation).
2. Client app prompts: *"Submit abuse report? The server will review reported user handle and metadata."*
3. Client creates cryptographic report package: `{ reporter: AliceUUID, reported: BobUUID, reason: "HARASSMENT", timestamp: now }` signed by Alice's identity key.
4. Plaintext message contents are **never** transmitted to ensure strict zero-knowledge E2EE guarantees.

---

## 12. Flow 11: 365-Day Message Lifecycle Purge

```mermaid
sequenceDiagram
    autonumber
    participant Worker as Android WorkManager (Daily 03:00)
    participant Room as Local Encrypted Room DB
    participant Server as Server Cleanup Cron (Daily 03:00 UTC)
    participant Pg as PostgreSQL Message Relay Store

    Note over Worker,Room: Client-Side Local Purge
    Worker->>Room: SELECT COUNT(*) FROM messages WHERE created_at < (NOW() - 365 Days)
    Room-->>Worker: Found N expired records
    Worker->>Room: DELETE FROM messages WHERE created_at < (NOW() - 365 Days)
    Worker->>Room: VACUUM; (Reclaim disk pages & zeroize storage)
    
    Note over Server,Pg: Server-Side Transit Purge
    Server->>Pg: DELETE FROM transit_messages WHERE delivered = TRUE OR created_at < (NOW() - 30 Days)
    Server->>Pg: VACUUM ANALYZE transit_messages;
```

---

## 13. Flow 12: Permanent Account Deletion

1. User opens Settings $\to$ *Account* $\to$ *Delete Account*.
2. Security Warning: *"This action is permanent. All active sessions, chats, and handles will be deleted. Messages on your device will be purged immediately."*
3. Re-authentication: User confirms with biometric/PIN prompt and SMS OTP.
4. Server action:
   - Purges user profile, active prekeys, and session tokens.
   - Moves Blinded Identity Nullifier to `deleted_identity_tombstones` with a 90-day anti-abuse cooling period.
   - Releases username to quarantine.
5. Client action:
   - Executes Android Keystore `keyStore.deleteEntry(alias)` for all identity and ratchet keys.
   - Deletes encrypted Room database files and shared preferences.
   - Resets app back to initial Splash/Registration screen.
