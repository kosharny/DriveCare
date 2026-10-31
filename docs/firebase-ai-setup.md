# Firebase AI Logic setup

Verified on 2026-10-25. Provider: Firebase AI Logic Android SDK, Gemini Developer API
backend (`GenerativeBackend.googleAI()`). Selected stable text model:
`gemini-3.5-flash-lite`, confirmed in the official supported-model list. No custom
server, raw Gemini key, on-device model, authentication or analytics is included.

## Console and local configuration

1. Register the actual Android application ID (`com.example.drivecare` currently)
   in a Firebase project. Download its real `google-services.json` into `app/`.
   It is ignored by Git. The Google Services Gradle plugin is conditional, so an
   unconfigured offline build still works.
2. Enable Firebase AI Logic and select the Gemini Developer API backend. Follow
   Firebase's generated setup; do not copy a Gemini Developer API secret into source
   or BuildConfig. The Firebase app identifiers/configuration key are public config,
   not authentication. Apply documented Firebase API restrictions and Android
   application/package and signing-certificate restrictions appropriate to the APIs.
3. Register SHA fingerprints for the actual debug/release and Play App Signing
   certificates. Register App Check: debug provider for debug builds ONLY; Play
   Integrity for release. Register the debug token shown by the Firebase debug
   provider locally in the console; do not commit that token or ship it in release.
4. Verify **App Check enforcement for AI Logic in the console**. New AI Logic
   setup workflows can enforce it automatically; register the local debug token
   before requests and validate the real app. Installing the SDK alone is not
   proof that enforcement is configured. Verify release
   attestation from a signed internal Play distribution.
5. Set supported provider/project request and token quotas plus AI Logic per-user
   rate controls, monitoring and billing controls before public release. Their real
   configured limits depend on your project and selected plan; none have been
   configured from this repository. Budget alerts are notifications, not a guaranteed
   automatic spending stop. App Check is attestation, not a purchase or identity quota.

## Remote Config

| Key | Packaged default | Behavior |
| --- | --- | --- |
| `ai_enabled` | `true` | Can disable online requests |
| `ai_model` | `gemini-3.5-flash-lite` | Only this verified low-cost model is allowlisted |
| `ai_daily_allowance` | `5` | Soft successful-response count, clamped to 0–10 |

Model changes require reviewing the schema/cost and updating the allowlist in code.
An unknown remotely selected model yields unavailable; it cannot silently select a
larger paid/preview model. Defaults remain on fetch failure; requests have a bounded
timeout. Responses must conform to a typed JSON schema; required fields, enums,
response length and collection/string bounds are checked before local persistence.
No links are treated as verified citations. AI has no tools to modify records.

The per-installation counter uses the persisted reporting timezone's calendar date,
counts successful saved responses only, permits one request at a time and applies a
30-second cooldown. An identical recent request/context can use saved history.
Errors and cancellation do not consume the success allowance. A sent request can
still incur provider usage after local cancellation. Clearing/reinstalling app data
can bypass this counter: it is a UX guard, **not secure identity-based metering or a
financial cap**. Pro has the same allowance. Strict cross-device paid quotas would
require a separately designed trusted server/identity system.

## Privacy and selected plan

The repository does not configure a real Firebase project or billing plan. Therefore
no production data-processing plan has been selected or claimed verified. The setup
supports the Gemini Developer API no-billing-required eligibility for the selected
model, subject to current availability/quotas. Before release, choose the actual
plan, region and terms and record that decision here and in the privacy notice.

Under Gemini API terms, unpaid-service content can be used to improve Google products
and may be reviewed; paid-service treatment differs, and applicable regional exceptions
must be reviewed. Do not send sensitive personal data. Consult the current terms for
your project/region rather than assuming free and paid input have identical handling.
Cloud AI input leaves the device; the app never promises it stays local.

First request requires persisted explicit consent. Selected technical vehicle fields,
last recorded mileage/date, the question and only an optionally selected small exact
record summary are sent. Photos, receipt images, location, license plates and VIN are
not collected/sent. No full database/chat-history upload occurs. Consent can be revoked
in Settings; saved answers stay readable offline until deleted. User notes and question
are untrusted input under a fixed separate system instruction. Numerical totals are
calculated locally. Advice has no repair database, recall feed or live shopping grounding.

## Failure diagnosis

Missing JSON: offline app works; new AI is unavailable. Wrong package/project, missing
SHA, unregistered debug token or failed Play Integrity/enforcement: AI requests fail,
not a reason to disable attestation. Remote disabled/unsupported model: unavailable.
Malformed/oversized model JSON: recoverable error. No successful response is fabricated.
Live consent/send/cancel/quotas and malformed provider responses need configured manual
verification; see the verification document.

## Official references

- https://firebase.google.com/docs/ai-logic/get-started?platform=android
- https://firebase.google.com/docs/ai-logic/models?api=dev
- https://firebase.google.com/docs/ai-logic/generate-structured-output?platform=android
- https://firebase.google.com/docs/ai-logic/production-checklist
- https://firebase.google.com/docs/ai-logic/quotas
- https://firebase.google.com/docs/ai-logic/pricing
- https://ai.google.dev/gemini-api/terms
