# AI Build Prompt — Oi

## Source of truth

Read `HUMAN_PLAN.md` before doing any implementation. It contains the current 92-point product plan and labels unresolved choices as **Decision needed**. Follow its current decisions over older or conflicting technical documents in this repository. Do not silently restore requirements from those documents that HUMAN_PLAN.md has changed or marked undecided.

## Current instruction: planning only

Do not write or modify application code, run implementation changes, select vendors, or create a release in response to this prompt alone. The user has asked to plan first. Wait until the user explicitly asks to start building.

## When the user authorizes implementation

- Build a real Flutter application for Android first and iOS afterward, with the same core features on both platforms.
- Build one platform-independent backend and shared API/data contracts; do not duplicate or rebuild backend features for iOS.
- Implement real phone-number OTP using backend verification and production SMS delivery; never use hard-coded users, unconditional OTP success, fake backend responses, or local-only substitutes as a completed feature.
- Implement the first-version face-only liveness check after OTP as specified in HUMAN_PLAN.md; keep its result distinct from Oi UIN and government identity proof.
- Treat Oi UIN as a unique Oi account identifier, not as evidence of a legal identity or verified human.
- Enforce one active session per account on the server: a new-device login invalidates the prior device session; keep a valid session across app restarts; explicit logout ends the current session.
- Implement only the approved private group behavior, with private membership and user approval; do not create public group discovery.
- Implement the approved local encrypted storage, backup, and retention behavior consistently across platforms.
- Use production-grade persistence and APIs for account, profile, session, messaging, group, and status state; never present sample or in-memory data as durable backend behavior.
- Build feature-by-feature with clear UI states, server-side validation, secure configuration, database migrations, automated tests, and documented operational requirements.
- Do not embed credentials or secrets in source code; load provider credentials from secure configuration.
- Do not claim security, encryption, liveness, backup, or identity behavior is complete without tests and appropriate expert review.

## Decisions and blockers

- Do not guess on a **Decision needed** item in HUMAN_PLAN.md when the choice affects user-visible behavior, legal compliance, security/privacy, data loss, cost, or an external service.
- Ask the user only for genuine blockers that cannot be safely resolved from the plan or repo, such as a required legal approval, unavailable provider credentials, or an unresolved high-impact policy choice.
- Do not ask the user to choose routine implementation details that can be handled safely within the approved plan.
- If an external provider or approval is unavailable, state the blocker and finish independent work without substituting a mock or claiming the production feature works.
- Before implementing, identify any remaining decision that blocks the next phase; make progress on independent approved work and preserve the decision in the plan.

## Keep the two planning files synchronized

- `HUMAN_PLAN.md` is the canonical human-readable requirements list; `AI_BUILD_PROMPT.md` is the execution instruction derived from it.
- Whenever the user provides a new app requirement or decision, append it as the next numbered point in `HUMAN_PLAN.md` and update this prompt in the same change.
- If a user decision resolves a **Decision needed** item, update the corresponding numbered point in HUMAN_PLAN.md, remove or revise the open marker, and reflect the decision here.
- Never delete or rewrite an existing requirement merely to make documents shorter; if a requirement changes, preserve its intent and make the user-directed change explicit.
- After editing either file, check that the AI instructions still match the human plan, verify the numbered-point count/sequence, and run `git diff --check`.
- Do not commit unless the user explicitly asks for a commit.
