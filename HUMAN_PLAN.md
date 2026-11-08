# Oi App — Human Plan

> **Purpose:** This is the plain-language, numbered plan for the Oi app. It is the source of truth for future implementation. The numbered items combine the current product documents with the later decisions you gave; items marked **Decision needed** are not settled.
>
> **Current status: Planning only. Do not start app development until you say to start.**

## App and platforms

1. The app is called **Oi**.
2. Oi is a mobile app for people to communicate privately.
3. Build the Android and iOS apps with **Flutter**.
4. Develop and release Android first.
5. Develop iOS after Android, using the same product plan and core features.
6. Android and iOS must use the same backend and account rules.
7. Design backend APIs and data models so adding iOS does not require rebuilding backend features.
8. Each account can have only one active signed-in device at a time.
9. Keep a user signed in on their device across app restarts.
10. When the user signs in on a different device, the server ends the previous device's session and makes the new one active.
11. When the user explicitly logs out, end that device's session.
12. The server, not a local app setting, is the authority for which device session is active.

## Phone login and account setup

13. Users sign in with their phone number and a real SMS one-time password (OTP).
14. The app must connect to a real backend OTP flow; a pretend accepted code is not login.
15. Validate phone numbers in international E.164 format.
16. **Decision needed:** Confirm which countries will be supported at first release; existing documents list India (+91), the UK (+44), and the US (+1).
17. OTPs contain six digits.
18. Generate OTPs securely and unpredictably on the backend.
19. An OTP expires after five minutes.
20. Wait 60 seconds before allowing an OTP resend.
21. Allow at most three incorrect OTP attempts for one request.
22. Lock OTP verification for 15 minutes after the third incorrect attempt.
23. Send no more than three OTPs to one phone number in one hour.
24. Accept no more than ten OTP requests from one source IP in one hour.
25. Do not write OTPs, passwords, session tokens, or secrets to logs.
26. Test OTP shortcuts, if needed during development, must be isolated from production and impossible to enable in a production build.
27. After a valid OTP, the backend creates or finds the correct account and issues its session.
28. Show clear loading, resend countdown, invalid-code, expired-code, and rate-limit states.
29. The phone number is private account data, not a public profile field.

## Profile, Oi UIN, and first-version face check

30. Let a user set a display name.
31. Give every account a unique username.
32. **Decision needed:** Confirm the username rules; existing documents propose 3–30 lowercase letters, digits, or single underscores and a change cooldown.
33. Give every account a stable internal account ID that does not change when the phone number or username changes.
34. Give every account an **Oi UIN**.
35. The Oi UIN identifies an Oi account; it is not proof of a government identity or a verified real-world person.
36. In the first version, perform a face-only liveness check after phone OTP verification.
37. Explain the face check to the user and obtain consent before using the camera.
38. Do not describe a successful liveness check or an Oi UIN as government identity verification.
39. **Decision needed:** Confirm whether any government-ID verification is required in addition to the first-version face-only liveness check.
40. **Decision needed:** Confirm whether face-check processing is on-device or uses a service, and how any captured face data is handled, stored, or deleted.
41. Show users the result of the phone check and face liveness check as separate account states.
42. Allow users to complete, retry, or recover from failed onboarding without creating a false successful account.

## Chats, private groups, and safety

43. Support private one-to-one text messaging.
44. Support private groups, as approved by the user.
45. Group membership must be private and based on invitations or user approval, not public join-by-discovery.
46. Do not create public groups or a public group directory.
47. **Decision needed:** Confirm group creation, invitation, membership approval, owner/admin, member removal, and group-size rules.
48. Keep messages text-only in the initial scope unless the user approves more.
49. Do not add images, video, audio messages, file sharing, stickers, or voice/video calls to the current plan.
50. **Decision needed:** Confirm the message-length limit; existing documents propose 4,096 characters.
51. Show message state clearly: waiting to send, sent, delivered, and read if read receipts are enabled.
52. Send messages through the backend so they can reach another signed-in user.
53. Save outgoing messages locally while offline and send them when the network returns.
54. Retry queued messages in order and avoid creating duplicate messages when a request is retried.
55. Keep messages in the correct chronological order across reconnects and devices.
56. Allow users to search their own locally stored conversations without sending search terms to the server.
57. **Decision needed:** Confirm that end-to-end encryption is required for the first release and approve its protocol and recovery behavior before implementation.
58. If end-to-end encryption is approved, encrypt and decrypt message contents on users' devices; the server must not read message plaintext.
59. Let users block another account and prevent the blocked account from contacting them or seeing private information.
60. Let users report abuse without silently uploading private conversation contents.
61. Keep group messages private to approved members and apply the approved block/report rules to groups.
62. **Decision needed:** Confirm whether online/presence indicators and read receipts should be included.
63. Do not add an algorithmic public feed, public profile discovery, or public content timeline.
64. **Decision needed:** Confirm whether any other communication feature belongs in the first release.

## Text status

65. Let users publish, edit, and delete a short text status.
66. Existing documents propose a limit of 140 characters; confirm the limit before implementation.
67. Existing documents say a status remains until the user changes or deletes it; confirm that it should not expire automatically.
68. Let users control who can see their status.
69. **Decision needed:** Confirm status audiences (for example, approved contacts only) and whether any exceptions are needed.
70. Do not allow status photos, video, audio, attachments, or rich-media posts.
71. A blocked account must not see the blocker's status.
72. Show the user's current status in the agreed profile or contact screens.

## Local data, sessions, backup, and retention

73. Store chat history locally so the app can show it and support agreed offline behavior.
74. Encrypt sensitive local app data on both Android and iOS.
75. Keep encryption keys in the operating system's secure key storage, using the appropriate platform mechanism.
76. Keep locally saved sign-in credentials separate from the local chat database.
77. Restore the signed-in state after app restart unless the user logs out or the server replaces that device's session.
78. **Decision needed:** Confirm whether local chat history stays encrypted or is erased when the user logs out.
79. **Decision needed:** Confirm whether the previous device's local chat history stays encrypted or is erased when a new device signs in.
80. **Decision needed:** Decide whether chat backups are allowed, where they live, and whether they include messages or encryption keys.
81. If backups are allowed, define encryption, user restore steps, and access controls before implementation.
82. Make backup deletion and expiry obey the approved message-retention policy; a backup must not silently keep expired messages.
83. Existing documents propose deleting local messages after 365 days; confirm the final message-retention period and exact behavior.
84. Existing documents propose deleting undelivered server message ciphertext after 30 days and removing it sooner after delivery; confirm the final server-queue retention.
85. Keep backup retention, local-message retention, and server-queue retention consistent and explain them clearly to users.

## Backend, data, and delivery

86. Use one selected, authoritative backend implementation; the repository currently contains competing Go and Node.js relay scaffolding.
87. Persist accounts, usernames, sessions, messages/queues, groups, and statuses in a real backend data store; do not make a local-only app appear complete.
88. **Decision needed:** Choose the backend implementation, database, and minimum supporting services for the first release; existing documents propose PostgreSQL and sometimes Redis.
89. Use the same versioned API contracts and core account/data rules for Flutter on Android and iOS.
90. Use real SMS delivery in production; **Decision needed:** select launch countries and an SMS provider and supply its credentials through secure configuration.
91. Test complete real flows—including OTP, session replacement, private groups, offline message sync, storage/backup rules, and Android/iOS API compatibility—before release.
92. Keep this work at the planning stage until the user explicitly asks to begin implementation.

## How to maintain this plan

- When you give a new requirement or decision, add it as the next numbered point here and update `AI_BUILD_PROMPT.md` so both documents agree.
- Keep unresolved choices marked **Decision needed**; do not convert an old technical proposal into an approved product rule without confirmation.
- `AI_BUILD_PROMPT.md` is for the AI/developer; this file is the human-readable source of truth.
