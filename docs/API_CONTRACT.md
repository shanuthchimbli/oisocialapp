# Oi mobile API contract (proposed)

The Flutter client currently targets a versioned JSON API under `/v1`. This is
an integration contract for the mobile implementation, **not an approved
backend technology choice or an existing service**. The repository has no
backend server. The eventual backend must enforce all authentication,
verification, rate-limit, and account-state rules; client timers and checks
are only user-interface guidance.

## Transport and errors

- Production API origins must use HTTPS. The app allows HTTP only for
  loopback development addresses.
- Requests and JSON responses use UTF-8 and `application/json`.
- Authenticated requests send `Authorization: Bearer <access_token>`.
- Do not log request OTPs, verification codes, access tokens, phone numbers,
  or other secrets.
- Errors use this envelope; `retry_after_seconds` is optional:

  ```json
  {
    "error": {
      "code": "rate_limited",
      "message": "Please wait before requesting another code.",
      "retry_after_seconds": 60
    }
  }
  ```

- Send HTTP `429` for throttling and `Retry-After` where appropriate. The app
  recognizes `invalid_otp`, `otp_expired`, `otp_locked`, and `rate_limited`;
  other error messages are shown as supplied by the service.
- A success response that does not match the documented JSON shape is treated
  as a protocol error, never as success.
- Success status codes are exact: `202` for OTP requests, `200` for OTP
  verification/session read/profile update, and `204` for session deletion.

## Request an SMS code

`POST /v1/otp/requests`

```json
{ "phone_number": "+14155552671" }
```

The phone number is E.164. The server sends a real SMS only after enforcing
the approved country support, five-minute expiry, 60-second resend wait, three
verification attempts, 15-minute lockout, three sends per phone per hour, and
ten requests per source IP per hour. OTPs are six digits and generated
securely on the server.

Success is `202 Accepted`:

```json
{
  "request_id": "opaque-challenge-id",
  "expires_at": "2026-11-08T10:55:00Z",
  "resend_available_at": "2026-11-08T10:51:00Z"
}
```

The app's countdowns are advisory; the server remains authoritative.

## Verify the code and create a session

`POST /v1/otp/verifications`

```json
{ "request_id": "opaque-challenge-id", "code": "123456" }
```

On successful verification, the server atomically invalidates any previous
device session and returns `200 OK`:

```json
{
  "access_token": "opaque-secret-session-token",
  "account": {
    "id": "stable-account-id",
    "display_name": null,
    "username": null,
    "uin": "oi-account-identifier",
    "phone_verified": true,
    "liveness_status": "not_started"
  },
  "next_step": "profile_setup"
}
```

The token is an opaque bearer credential. The server defines and enforces its
lifetime and revocation; the app stores it only in iOS Keychain / Android
secure storage and never in logs or ordinary preferences. The phone number is
not part of public account data. `next_step` is one of `profile_setup`,
`liveness_required`, or `ready`.

## Restore and end the active session

- `GET /v1/session` requires a bearer token and returns `200 OK` with
  `{ "account": { ... }, "next_step": "..." }`. Return `401` when the session
  is expired, revoked, or replaced by a login on another device. The client
  clears the local token on `401`; temporary network/server errors preserve it
  and show a retry state.
- `DELETE /v1/session` requires a bearer token and returns `204 No Content`
  only after ending that session. If the device is offline, the app clears its
  local credential for the user's logout intent but reports that remote
  revocation could not be confirmed. A `401` also confirms the session is
  already inactive.

## Save the initial profile

`PATCH /v1/profile`

```json
{ "display_name": "Ari", "username": "ari" }
```

The server validates username availability and all username policy because
those rules are still undecided. Success returns the same
`{ "account": { ... }, "next_step": "..." }` envelope used by `GET /v1/session`.
Do not return `next_step: "ready"` until phone verification and the required
face-liveness check have both actually succeeded. The mobile app blocks the
account at `liveness_required`; it contains no mock or unapproved liveness
implementation.

## Current integration boundary

The mobile app implements the endpoints above but this repository does not
yet implement a server, database, SMS delivery, liveness provider, or
deployment. Backend technology, hosting, supported launch countries, SMS
provider/credentials, username policy, liveness processing/privacy, and
deployment secrets still require approval or provisioning. Messaging,
groups, status, blocking/reporting, backup, and retention endpoints remain
unimplemented until their decisions and server design are settled.
