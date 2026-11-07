# Master Glossary & Technical Lexicon

> **Document ID:** GLOSS-001  
> **Status:** Stage 1 Approved Baseline  
> **Last Updated:** 2026-11-07  

---

## 1. Cryptographic & Security Terms

### Double Ratchet Algorithm
A cryptographic key-management protocol developed by Trevor Perrin and Moxie Marlinspike (Signal Foundation). It combines a symmetric-key ratchet based on cryptographic hash functions (KDF chains) with an asymmetric Diffie-Hellman (DH) ratchet based on Curve25519. It guarantees both **Forward Secrecy (FS)** and **Post-Compromise Security (PCS)** for every individual message exchanged between two parties.

### X3DH (Extended Triple Diffie-Hellman)
A cryptographic protocol that establishes a shared secret key between two parties who mutually authenticate each other using public keys, even when one party is offline at the time the session is initiated.

### Forward Secrecy (FS)
A cryptographic security property ensuring that the compromise of long-term secret keys (such as device identity keys) does not compromise past session keys, preventing an attacker from decrypting previously recorded ciphertext.

### Post-Compromise Security (PCS) / Break-in Recovery
A security property ensuring that even if an active ratcheted session key is leaked or compromised at time $t$, future messages exchanged at time $t+k$ become undecryptable to the attacker as soon as a new asymmetric Diffie-Hellman exchange occurs.

### Prekey Bundle
A collection of public keys published by an Android/iOS client to the server:
- Long-term Identity Public Key ($IK$)
- Signed Prekey ($SPK$) along with its cryptographic signature
- A pool of one-time Prekeys ($OPK_1, OPK_2, \dots, OPK_n$)
The server distributes bundles to initiators to establish asynchronous sessions without learning the resulting shared secrets.

### SQLCipher
An open-source extension to SQLite that provides transparent, 256-bit AES encryption of database files. On Android, it encrypts all Room database pages, metadata, write-ahead logs (WAL), and temp files using keys derived from hardware-backed Android Keystore.

### Android Keystore System
A hardened hardware-backed (TEE or StrongBox Keymaster) subsystem that lets applications store cryptographic keys in a container that makes extraction mathematically and physically infeasible, supporting biometric and pin-auth gates.

---

## 2. Identity, Government & Account Terms

### One-Person-One-Account (OPOA) Invariant
The strict architectural and cryptographic guarantee that a single verified living human being may possess at most one active account on the messaging network at any given time.

### Permanent Account ID (UUIDv7)
A cryptographically secure, time-ordered 128-bit Universally Unique Identifier assigned immutably to a human identity upon successful initial verification. It never changes, even if the user updates their phone number, changes their username, or migrates physical devices.

### UIDAI (Unique Identification Authority of India)
The statutory authority established under the provisions of the Aadhaar Act, 2016 in India. 

### Aadhaar Offline Paperless e-KYC (XML / QR)
A legally sanctioned, digitally signed XML document or high-density secure QR code generated directly by the resident from UIDAI official portals or the mAadhaar app. It contains resident demographics and a photo, encrypted with a resident-specified 4-digit share code and signed with UIDAI's digital certificate. Crucially, it allows offline verification without storing raw 12-digit Aadhaar numbers or transmitting biometrics.

### GOV.UK One Login
The United Kingdom government's consolidated digital identity system for public services, replacing legacy GOV.UK Verify. Operated by the Government Digital Service (GDS).

### IAL2 (Identity Assurance Level 2)
As defined by NIST SP 800-63-3, a remote or in-person identity verification standard that requires the presentation and validation of genuine, government-issued photo identity documents and biometric liveness verification against authoritative record sources.

### Blinded Identity Token (Identity Hash / Nullifier)
A cryptographically salted, irreversible one-way hash:
$$\text{Nullifier} = \text{HMAC-SHA256}(\text{ServerSalt}, \text{GovSystemID} \parallel \text{GovDocID})$$
Used exclusively for duplicate-account detection in the backend database without persisting raw government identity numbers in plaintext.

---

## 3. Product & Application Terms

### Status (Text-Only)
A persistent, text-only broadcast message (maximum 140 UTF-8 characters) published by an account to their authorized contacts. It persists indefinitely until explicitly updated or deleted by the user (unlike 24-hour ephemeral stories in legacy messengers). It supports zero multimedia.

### One Active Device Policy
A system-enforced security invariant where exactly one hardware device session holds valid authentication tokens and cryptographic ratchets for an account. Logging in on Device B automatically and irrevocably invalidates the session and ratchets on Device A via server-side session termination and high-priority push revocation.

### 365-Day Retention Engine
An automated, cryptographically auditable data lifecycle worker that strictly deletes message payloads, receipts, and associated metadata exactly 365 solar days (8,760 hours) following their creation timestamp. Deletion is enforced both centrally on server relays and locally in device Room databases.

### Optimistic UI
A frontend design pattern in Jetpack Compose where user actions (such as sending a text message) are rendered immediately in the UI (under 50 ms) with a "Pending" state, while background coroutines handle encryption, database persistence, and WebSocket socket transmission asynchronously.

---

## 4. Architectural & Protocol Terms

### MVI (Model-View-Intent)
A reactive architectural pattern used in the Android application. The UI emits immutable *Intents*, the *ViewModel* processes them through business interactors and repository layers, and yields a single stream of immutable *ViewState* objects consumed by Jetpack Compose.

### WebSocket Frame
A framed, lightweight bidirectional binary transport protocol (RFC 6455) running over TLS 1.3, used for instant message delivery, delivery/read receipts, typing indicators (if enabled), and session heartbeat ping/pong.

### FCM (Firebase Cloud Messaging) Data-Only Push
High-priority background push notifications sent to Android clients containing zero message plaintext. The payload contains only an encrypted message sequence reference or wakeup signal, prompting the Android WorkManager or local foreground service to initiate a secure background pull and decrypt within the local TEE context.
