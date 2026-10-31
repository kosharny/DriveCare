# DriveCare — Complete Android Implementation Prompt for Codex

Prepared on October 25, 2026. DriveCare is a working product name, not a claim of trademark or store-name availability.

## 1. Your role and the requested outcome

Act as a senior native Android engineer, thoughtful product designer, and implementation owner. Build a complete, usable Android application named DriveCare: a personal car maintenance calendar, fuel and expense journal, ownership statistics dashboard, and vehicle-aware AI assistant.

This is an implementation request. Inspect the repository, implement the application, compile it, run Android Lint and formatting checks, and report actual results. Do not stop after proposing an architecture or creating static screens. Work through the complete user journeys.

Use `$android-mvvm-compose` when available and read all applicable `AGENTS.md` instructions. If the skill is unavailable, follow the explicit architecture and engineering requirements in this prompt. Communicate progress and the final implementation summary in Russian. Use English source identifiers and documentation. The app itself must have complete English, Russian, and Ukrainian localization.

Carry forward these explicit user preferences:

- Native Kotlin, Jetpack Compose, Material 3, and MVVM.
- A separate appropriately scoped ViewModel for each meaningful screen or flow.
- Readable, conventionally formatted code. Do not compress UI trees, callbacks, coroutines, or modifier chains into single lines.
- No new or modified automated tests, test libraries, sample test classes, or test/benchmark CI tasks. Preserve existing tests. Validate with compilation, Android Lint, formatting, and manual checks where available.
- Include all actual imports, dependencies, manifest configuration, persistence, and working actions.
- Support phones and tablets through adaptive layouts.
- Use current stable, mutually compatible dependencies verified from official sources at implementation time.

Do not apply older project versions mentioned in conversation without checking the actual repository and toolchain. This is an Android app; do not introduce KMP or an iOS application as part of this task.

## 2. Product definition

The application helps a driver answer:

1. What maintenance is due next, and is anything overdue?
2. When did I last replace or service a component?
3. How much did I spend on fuel this week, month, year, or custom period?
4. What is my measured fuel consumption when the records support that calculation?
5. What potential issues should an owner of my particular vehicle investigate?
6. Which practical upgrades might fit my vehicle and needs?

Core loop:

Add a vehicle → establish maintenance reminders → log fuel and services → see spending and upcoming work → ask the AI a vehicle-specific question → optionally save a reviewed suggestion as a task or note.

Build an offline-first ownership tool. Vehicle records, maintenance, logging, statistics, and local history must work without an account or internet. Online access is required for new AI responses and purchase/restore operations.

Do not add a social feed, marketplace, advertisements, vehicle tracking, OBD connection, insurance sales, navigation, live fuel-price comparison, or automatic VIN decoding in version 1. Do not require sign-in.

## 3. Scope and supported vehicles

Version 1 supports passenger cars using petrol/gasoline, diesel, or LPG, including petrol/diesel hybrids when the user is tracking their fuel use. Fuel type must be explicit. For plug-in hybrids, clearly state that fuel records do not include charging energy or total propulsion costs. Full battery-electric vehicle charging analytics are outside version 1; do not show meaningless liters-based statistics for electric vehicles.

A make alone is not enough for precise vehicle-specific advice. Make vehicle entry easy, but capture:

- Nickname, optional.
- Make, required.
- Model, required.
- Model year, required and validated against a sensible current-year range.
- Generation/body code, optional.
- Engine description or engine code, optional but useful.
- Fuel/powertrain type, required.
- Transmission, optional.
- Vehicle market/region, optional.
- Current odometer reading and its date, required initially.
- Purchase date and purchase odometer, optional.
- Vehicle currency, selected explicitly.
- Optional local vehicle photo.

Provide helpful make suggestions from a small local list, with manual entry always available. Model, generation, and engine may use manual input. Do not fabricate a complete vehicle catalog or claim to identify specifications automatically.

License plates and VIN are unnecessary for version 1. Do not collect or transmit them.

## 4. Design direction

Create a polished, calm automotive utility with a light theme first and a complete dark theme. The visual language should feel like a trustworthy ownership dashboard.

Suggested light palette:

- Background: `#F6F7FB`.
- Surface: `#FFFFFF`.
- Primary blue: `#2563EB`.
- Main text: `#172033`.
- Secondary text: `#627084`.
- Success: an accessible green.
- Warning: an accessible amber.
- Overdue/error: an accessible red.

Map these into semantic Material 3 tokens and create appropriate dark equivalents. Verify contrast; do not use color as the only indication of state. Use restrained cards, clear typography, consistent spacing, and recognizable automotive-related vector icons. Do not add downloaded car logos, decorative stock photos, or unlicensed brand imagery. A simple local car illustration/icon is sufficient when no user photo exists.

Use concise user-facing language. Avoid exposing repository names, API providers, SQL terms, or setup details in normal product flows. Configuration failures should have a short human-readable message; detailed setup belongs in developer documentation.

Empty states must explain the next useful action. Never populate a new user's database with fake fuel records, expenses, AI insights, or service history. Preview data belongs only in preview/debug-only sources.

## 5. Navigation and main screens

Use five top-level destinations:

1. Overview.
2. Maintenance.
3. Journal.
4. Statistics.
5. AI Garage.

Place Settings in the top app bar and a vehicle switcher in the relevant shared header. A garage management screen supports adding, editing, selecting, archiving, and deleting vehicles.

Use a bottom navigation bar on compact windows and a navigation rail on wider windows. Select layouts by available window size, not by a hardcoded phone/tablet check. Preserve top-level destination state appropriately. Use supported predictive-back and back-stack restoration APIs.

Additional destinations/forms:

- Onboarding.
- Vehicle setup/edit.
- Garage.
- Add/edit fuel entry.
- Add/edit expense entry.
- Maintenance plan details/edit.
- Complete maintenance.
- Service record details/edit.
- Journal entry details.
- AI conversation/detail.
- Pro paywall.
- Settings, data backup/restore, and About/Privacy information.

Use dialogs or bottom sheets for small decisions. Use full screens for forms with several fields. Do not build a huge modal with deeply nested navigation.

## 6. Onboarding and vehicle setup

Use a short, skippable three-page introduction:

1. Keep maintenance on schedule.
2. Understand fuel and ownership costs.
3. Explore vehicle-aware AI guidance.

Do not show a paywall before the user has entered the app. After onboarding, guide the user to create their first vehicle. Make and model must be quick to enter. Additional technical details may be completed later.

Choose UI language from the supported device locale, falling back to English. Currency, units, and language are independent preferences. Do not assume that choosing Ukrainian automatically means all records use UAH, or that choosing English means miles.

After vehicle creation, offer common maintenance task categories, such as engine oil, oil filter, air filter, cabin filter, tires, brakes, coolant, and inspection. These are planning categories, not verified service intervals. Require the user to provide or confirm intervals/due thresholds. Do not invent manufacturer-specific intervals from make and model.

The user may skip task setup and add tasks later. Never invent the vehicle's last service date or mileage.

## 7. Overview

Show the selected vehicle and:

- Latest known odometer with its recorded date.
- Next maintenance items, with date and/or remaining distance.
- Clear overdue count.
- Fuel spending for the current month in the selected record currency.
- Total recorded ownership expenses for the month, with category context.
- Measured fuel consumption only when enough valid fuel data exists.
- Quick actions: Add fuel, Add expense, Log service, Update odometer.
- A small entry point to AI Garage with vehicle-aware suggested questions.

Do not show an invented vehicle health percentage, reliability score, projected repair bill, or fake warning diagnosis.

If mileage has not been updated recently, label it as the last recorded mileage. Distance-based due status is based on that reading; the app does not know how far the user drove afterward.

## 8. Maintenance calendar and task rules

Provide an actual month calendar with event indicators, selected-day agenda, month navigation, a Today action, and an agenda/list view. Show scheduled tasks and completed services with distinct semantics. A distance-only task belongs in the mileage/agenda section unless the user explicitly provides a date. Do not fabricate a calendar date from mileage.

Support:

- One-time reminders.
- Recurring maintenance by calendar interval.
- Recurring maintenance by distance interval.
- Both time and distance, with a task becoming due when either confirmed threshold is reached.
- User-entered due dates/due odometer values when previous completion information is unknown.
- Task title, category, notes, interval configuration, notification preferences, and optional estimated cost.
- Complete, edit, delete, and pause/resume a plan.
- Complete a service without creating a recurring plan.

Use separate concepts for a maintenance plan and its completion records. Completing a task creates an immutable-history-style service record, while allowing explicit user edits/corrections. Record service date, actual odometer, description, actual cost/currency, optional workshop, notes, and attachments.

Recurring defaults:

- Next due date = actual completion date plus the configured calendar interval.
- Next due odometer = actual completion odometer plus configured distance interval.
- Month intervals use actual calendar arithmetic; one month is not a fixed 30 days.
- If a required baseline is missing, ask for it or leave that dimension unknown. Do not substitute today's date or zero mileage.
- Completion and plan recalculation must happen transactionally.
- Editing/deleting a completion must recalculate the latest baseline consistently, without duplicate or stale reminders.

Status semantics:

- Planned: known thresholds have not entered their warning window.
- Due soon: a configured warning window is reached.
- Due: threshold reached.
- Overdue: beyond a known threshold; show whether the cause is date, distance, or both.
- Unknown mileage dimension: insufficient recent/valid odometer information; show the known date information without asserting the mileage is current.

Include explanatory text such as “Due by 15 June or at 85,000 km, whichever comes first,” fully localized.

Service records, one-off maintenance, and repairs must enter the journal and statistics once. Never duplicate a service cost into both a service record and an unrelated expense that is summed again.

## 9. Fuel entry and journal

The fuel form must support:

- Vehicle and event date/time.
- Odometer.
- Fuel type.
- Fuel volume.
- Total paid and currency.
- Price per volume unit, calculated or editable with well-defined reconciliation.
- Full tank toggle.
- A flag indicating missing/unlogged refueling since the previous record.
- Station name, optional.
- Notes, optional.
- Receipt image, optional.

Make volume and total paid the normal input path; derive unit price. If the user changes unit price, update the dependent amount with explicit field ownership rather than an infinite update loop. Use decimal arithmetic and the currency's scale. Final total paid remains the authoritative monetary record after user confirmation.

Provide a clear explanation for the Full tank field because it controls consumption calculations. An initial full-tank record establishes a baseline, not a measured consumption value.

Support edit/delete of historical fuel entries and recompute affected intervals and statistics. The user must not have to re-enter subsequent records manually.

The Journal combines:

- Fuel entries.
- Service completions.
- Repairs and other expenses.
- Manual odometer readings when useful.

Provide filters for date range and record type, and search by meaningful text. Support details, edit, deletion confirmation, and useful empty states. Distinguish planned costs from paid expenses.

Other expense categories may include repair, tires, parking, washing, insurance, registration, accessories, and other. Insurance and registration are ordinary user records; do not provide legal or insurance advice. Service-completion amounts belong to their service record, not an additional duplicate entry.

## 10. Correct fuel consumption calculations

Implement the full-to-full method in plain Kotlin business logic, separate from Compose and ViewModels.

For a valid interval between full-tank records A and B:

- Distance = odometer(B) − odometer(A).
- Volume consumed = sum of compatible fuel volumes strictly after A through and including B, including all partial fills between them.
- Consumption in L/100 km = volume in liters / distance in km × 100.
- The fuel volume bought at A is excluded from that interval.

Manual verification example:

| Entry | Odometer | Fuel | Full tank? |
| --- | ---: | ---: | --- |
| A | 10,000 km | 40 L | Yes |
| P | 10,200 km | 12 L | No |
| B | 10,600 km | 30 L | Yes |

The valid measured interval is 600 km and 42 L, giving **7.0 L/100 km**. Do not use 40 + 12 + 30 liters. After A alone, show “Not enough data.”

Invalidate or exclude an interval when:

- Distance is zero or negative.
- A missing-refueling flag breaks completeness.
- Odometer history is inconsistent.
- Entries cannot be ordered unambiguously.
- Fuel streams are incompatible, or dual-fuel usage prevents a valid same-tank calculation.

Do not combine LPG and petrol as if they were one tank. Mixed/dual-fuel records may still contribute to spending, but unsupported consumption must show an explanation rather than a fabricated figure. After an invalidated/missing-data interval, a later confirmed full fill may establish a new baseline.

Aggregate consumption by total eligible volume divided by total eligible distance, not the arithmetic mean of interval percentages. Define period attribution explicitly: include complete eligible intervals by the date of their ending full fill and label this method. Do not claim an exact calendar-month consumption measure when an interval crosses month boundaries.

Support kilometers/miles and liters/US gallons. Explicitly label US gallons; do not silently treat them as Imperial gallons. Store sufficient canonical precision, avoid repeated lossy conversions, and retain the input unit where needed. Do not infer measured distance from elapsed days.

Fuel bought during a period is not necessarily fuel consumed during that period. Do not present purchased-fuel spend divided by unrelated odometer differences as an exact running cost per kilometer. Version 1 can omit that metric instead of showing a misleading calculation.

## 11. Money, dates, odometer integrity, and statistics

Store monetary values as integer minor units with ISO currency codes, or an equally exact documented decimal representation. Use `BigDecimal` for calculations; never binary floating-point for monetary arithmetic. Display localized amounts with the correct currency precision.

Each record retains its currency. Changing settings must not reinterpret past amounts. Default future entries to the vehicle's selected currency. For imported or manually entered mixed-currency data, show separate totals or a selected currency filter. Do not add 100 UAH and 100 EUR into a total of 200. There is no live foreign-exchange service in version 1.

Use timestamp/Instant semantics for fuel and expense events, and LocalDate semantics for maintenance dates. Define a persisted reporting time zone so period grouping is deterministic. Use half-open date ranges from the beginning of the first day to the beginning of the day after the selected end date. Handle month boundaries, daylight saving, and locale changes intentionally.

Odometer integrity:

- Keep dated readings and derive the latest known reading consistently.
- A backdated historical entry must not lower the displayed latest mileage.
- Validate a historical reading against its chronological neighbors, not merely against the current mileage.
- Allow legitimate historical insertion.
- Offer explicit correction for mistaken readings; do not silently reject all lower historical values.
- Warn and require resolution for impossible chronology. Recalculate reminders and statistics after corrections.
- Use transactions when a save creates a financial record plus an odometer reading.

Statistics must include, free of charge for the primary vehicle:

- This month, previous month, current year, all time, and custom date range.
- Total fuel spend.
- Fuel volume purchased.
- Number of fuel entries.
- Volume-weighted average paid fuel price = total compatible fuel spend / total compatible volume.
- A monthly fuel-spending chart and accessible textual summary.
- Total recorded expenses, with fuel/service/other subtotals.
- Valid measured consumption and a clear explanation when unavailable.

Pro may add longer comparative analyses and richer charts, but must not hide the basic fuel-spending-by-period requirement behind a paywall.

Use real repository data. Provide accessible chart labels, units, meaningful selection feedback, and text alternatives. Implement simple charts with Compose Canvas when that is enough; do not add a large chart dependency without a concrete benefit.

Manual money example: 45 L × 60 UAH = 2,700 UAH; 30 L × 62 UAH = 1,860 UAH. If both entries are in the selected period, fuel spending is 4,560 UAH, purchased volume is 75 L, and weighted unit price is 60.8 UAH/L before display rounding.

## 12. AI choice: Gemini through Firebase AI Logic

Use the official Firebase AI Logic Android SDK with the Gemini Developer API backend. Do not create a custom AI server for version 1 and do not embed a raw Gemini Developer API key in the APK. Do not require on-device Gemini Nano/ML Kit capabilities, a particular flagship device, or a large local model download.

At the time this prompt was prepared, Firebase's official supported-model list identifies `gemini-3.5-flash-lite` as a stable Flash-Lite option. Start with this cost-conscious text model if it remains supported at implementation time. Verify the official list again before implementation. If it has changed, select the current stable compatible Flash-Lite model and document the actual choice. Do not silently select a preview model or a larger paid model.

Keep the model identifier and AI feature-enabled switch in Firebase Remote Config, with safe packaged defaults. A remote model switch must remain compatible with the request schema and SDK. Do not silently increase cost by switching model families.

Use a clean boundary:

- `AiRepository` describes product operations and returns typed results.
- `FirebaseAiDataSource` owns Firebase SDK interaction.
- `AiContextBuilder` selects the minimum relevant local information.
- Plain models represent insight cards, follow-up questions, and draft actions.
- The AI ViewModel owns interaction state and cancellation, not SDK configuration.

Firebase/App Check setup:

- Use the current stable Firebase BOM and supported `firebase-ai` dependency; verify imports/APIs against official docs.
- Configure Firebase App Check from the beginning.
- Use the debug provider only in debug builds, and document token registration.
- Use Play Integrity or the documented appropriate production attestation provider for release. Never ship debug attestation tokens/provider in release.
- Enforce App Check for AI Logic in the console; merely adding the SDK is insufficient.
- Restrict Firebase project API keys as documented. Firebase configuration identifiers are not a substitute for authorization; raw Gemini keys and service-account secrets must never be packaged.
- Configure provider-side request limits/quotas, monitoring, and the relevant billing controls before release.
- Do not add Analytics, Firestore, Firebase Auth, Cloud Functions, or Firebase Messaging unless independently required. There is no required account or cloud record sync in version 1.

Cloud AI is not permanently free. Document the selected plan, model eligibility, request/token limits, and external billing setup. Ordinary budget alerts notify; they do not automatically guarantee a spending stop. If using separately configured spend caps, verify current availability, scope, and limitations, rather than promising an absolute cap.

## 13. AI Garage: actual user experiences

Provide four modes as prompt shortcuts within one coherent AI feature:

### A. Potential weak points

User action: “What should I watch for on my car?”

Pass make, model, year, generation/engine/transmission/market when known, and latest known mileage. Explain that recommendations depend on configuration. If essential details are missing, ask concise follow-up questions before giving configuration-specific claims.

Return useful cards with:

- Component or topic.
- Why it may deserve attention.
- Signs the owner may notice.
- What to ask a workshop to inspect.
- Relevant missing details.
- Evidence status: general guidance or model-specific claim requiring verification.

Do not assert that the user's car has a failure or that every example of that model has the same problem. Never invent failure rates, recall campaigns, manufacturer bulletins, or an inspected condition.

### B. Maintenance planning help

Explain existing user-entered tasks, help organize an owner's manual recommendation that the user provides, or produce editable draft tasks for review. Without verified documentation, numeric intervals must be labeled as general suggestions and require confirmation. AI-generated suggestions must never overwrite confirmed schedules automatically.

### C. Practical upgrades

Support questions such as “Which multimedia upgrade would be useful for my car?” or “What should I check before adding a reverse camera?”

Prioritize practical categories: Android Auto/CarPlay head unit, reversing camera, parking assistance, dashcam, cabin comfort, and appropriate non-invasive convenience improvements.

For each suggestion, explain:

- Intended benefit.
- Compatibility questions: generation, dashboard/head-unit size, factory amplifier, CAN bus, steering-wheel controls, factory camera, wiring, and adapter needs when relevant.
- Relative installation effort and professional installation needs.
- What must be verified before buying.

Do not guarantee an exact head-unit fit from the make alone. Do not invent current prices, stock, live links, or exact part compatibility. Do not suggest disabling airbags, emissions equipment, safety alerts, or tampering with vehicle security. Ask before discussing performance modifications; keep version 1 focused on practical ownership upgrades.

### D. Questions about the user's records

Examples:

- “Summarize my recorded fuel expenses this month.”
- “What services have I recorded recently?”
- “What should I ask the mechanic about this maintenance item?”

For numerical questions, compute totals deterministically in the app. Supply those exact totals to the model only for explanation. Do not delegate financial sums, date filtering, fuel formulas, or due-status calculation to the language model.

AI conversation UX:

- Display the selected vehicle context before sending.
- Suggested question chips in the selected UI language.
- Clear sending/loading, success, cancelled, rate-limited, unavailable, offline, and error states.
- Prevent duplicate submissions and allow cancellation.
- Save successful conversations locally, with vehicle ID, language, timestamp, and a context snapshot/version sufficient to identify stale advice after a vehicle edit.
- Existing saved responses remain readable offline.
- Permit deleting a conversation and all chat history.
- Do not regenerate a response every time a screen is opened or recomposed.
- “Save as note” and “Create maintenance draft” must open an editable confirmation flow. Only explicit confirmation writes a real record/task.

## 14. AI grounding, privacy, output format, and usage limits

Version 1 does not have a licensed automotive repair database, an official recall feed, live shopping search, or web-grounded citations. Make this limitation clear in product wording and developer docs. Never render generated URLs or citations as verified sources. Do not generate fake references. Where exact specifications matter, direct the user to the owner's manual, manufacturer information, or a qualified workshop.

Use a fixed system instruction separate from user content. Treat record notes, pasted text, and user input as untrusted data, not instructions to override the app's rules. AI has no direct database write/delete, purchase, navigation, or external-execution tools.

Provide this behavior in the system instruction, phrased naturally:

“You are DriveCare's automotive ownership assistant. Use only the vehicle details and recorded facts supplied to you. Ask about missing generation, engine, or market information before making configuration-specific claims. Distinguish general guidance, user-recorded facts, and unverified model-specific knowledge. Never diagnose a vehicle remotely or invent verified schedules, recalls, compatibility, prices, citations, or statistics. Respond in the requested language and use the requested units. Use app-computed totals exactly as supplied. Produce useful next steps and editable suggestions; never imply that a record or reminder has already been changed. For braking, steering, overheating, fuel leaks, or similar urgent concerns, prioritize an appropriate safety response and professional assessment over upgrade advice.”

Use structured JSON output and a schema supported by the selected Firebase SDK/model for insight generation. A possible typed contract:

```text
VehicleAdvice {
  mode: enum,
  language: en | ru | uk,
  summary: string,
  missingDetails: string[],
  items: AdviceItem[],
  draftActions: DraftAction[]
}

AdviceItem {
  title: string,
  explanation: string,
  signsToWatch: string[],
  nextSteps: string[],
  compatibilityChecks: string[],
  evidenceStatus: generalGuidance | suppliedUserFact | requiresVerification
}
```

Extend only where needed. Validate response size, allowed enum values, required fields, and safe bounds before using it. Unknown/malformed output is a recoverable AI error, never an application crash. Do not parse arbitrary prose by fragile substring matching. Follow current model documentation for supported generation parameters; do not copy obsolete temperature/top-K settings from old snippets.

Privacy:

- Before the first online AI request, explain briefly that selected vehicle details, the question, and any deliberately included relevant record summary are sent to Google's AI service. Let the user continue or cancel.
- Share no receipt image, vehicle photo, exact location, contact, plate, or VIN by default.
- Share only required structured vehicle fields and a small relevant record summary. Do not upload the entire database or all historical chats.
- Persist the user's consent choice; provide a way to review/revoke it. Consent is separate from notification permission and purchasing.
- Keep chat/records out of application logs and crash messages.
- Document the actual provider's selected-plan data processing terms; do not claim all AI input stays on the phone.

Version 1 AI usage design:

- AI is an optional, fairly limited feature available to free and Pro users on the same terms.
- Default to a local fair-use allowance of 5 successful responses per installation per local calendar day, plus a short cooldown and a one-request-at-a-time rule.
- Errors/cancellations must not consume the local successful-response allowance. A request already sent may still incur provider usage even if cancelled.
- Persist successful-response count/day and cache results to avoid accidental repeat requests. Remote Config may adjust the soft allowance and disable AI.
- Clearly document that a client-side daily allowance is a UX/anti-accidental-use mechanism. It is resettable/bypassable and is not secure per-person metering, not a financial spending cap, and not enforced by App Check.
- Configure service-side quotas and supported rate controls for actual abuse/cost management. Attestation helps establish authentic clients; it does not prove a paid entitlement or prevent every abuse scenario.
- Do not advertise “5 guaranteed free answers every day,” because upstream availability and quotas can prevent responses.
- If strict cross-device paid AI quotas are later required, that needs a server-controlled identity/entitlement/quota mechanism. Do not disguise a local counter as that mechanism or add that backend to version 1 silently.

Keep fixed offline educational guidance separate from AI answers, labeled “General guide.” Never pretend a bundled answer is a generated model-specific assessment.

## 15. Monetization: DriveCare Pro, one-time purchase

Implement a meaningful one-time permanent Pro unlock. Do not introduce subscriptions or consumable AI credits in version 1. Do not sell unlimited lifetime cloud inference.

| Capability | Free | Pro |
| --- | --- | --- |
| One active vehicle | Yes | Yes |
| Multiple active vehicles | No | Yes |
| Unlimited local fuel/service/expense records for allowed vehicles | Yes | Yes |
| Maintenance calendar and reminders | Yes | Yes |
| Basic fuel spending for all supported periods/custom range | Yes | Yes |
| Basic ownership totals and valid consumption | Yes | Yes |
| Basic CSV export and full local backup/restore | Yes | Yes |
| Advanced comparisons across periods and expense categories | No | Yes |
| Multi-vehicle comparisons, separated by currency | No | Yes |
| Detailed workshop/service and fuel-station spending breakdowns | No | Yes |
| Formatted PDF ownership/service report | No | Yes |
| Optional online AI with the same fair-use/availability limits | Yes | Yes |

No ads in either tier. Do not claim “remove ads” as a Pro benefit.

Use RevenueCat's official native Android SDK as the single purchase/entitlement integration over Google Play. This keeps purchase validation and entitlement management in a managed service instead of building a custom billing server. Do not also maintain a parallel direct BillingClient purchase implementation.

Proposed configuration identifiers:

- Google Play one-time product: `drivecare_pro_lifetime`.
- RevenueCat entitlement: `pro`.
- RevenueCat offering: `default`.
- Lifetime package attached to the one-time Pro product.

Verify current RevenueCat/Google Play support and dashboard terminology. Configure the product as a non-consumable permanent unlock in RevenueCat. Do not accidentally configure it as a consumable or consume the purchase. Check current documentation on restoration with anonymous users and Play Billing versions; backup of an anonymous ID is not a reliable substitute for a restorable non-consumed purchase.

Use RevenueCat's public Android SDK key from build configuration. It is a public app identifier, not an administrative secret. Never package a RevenueCat secret API key, Google Play service-account JSON, or signing credentials. Document RevenueCat's current service pricing separately; do not assume any managed service stays free at every scale.

Implement:

- Load the real offering and real localized store price.
- Display a localized custom Compose paywall with exact included benefits.
- Launch the real Google Play purchase flow through RevenueCat.
- Handle cancelled, pending, failed, already-owned, successful, and unavailable states.
- Grant access only when the `pro` entitlement is active in current SDK CustomerInfo. A callback or purchase-button click alone is insufficient.
- Let RevenueCat handle its documented purchase acknowledgment/validation workflow; do not double-process the transaction.
- User-initiated Restore purchases, with clear results. Do not invoke a restore action that may show account prompts automatically on every launch.
- Refresh CustomerInfo appropriately and observe entitlement changes.
- Define cached offline entitlement behavior using SDK-supported verification/caching; do not revoke a known unlock just because a refresh failed.
- Configure the relevant Google Play server notifications to RevenueCat for refunds/revocations, and document this external setup.
- If entitlement is revoked, preserve all records and exports. Ask the user to choose one active free vehicle; keep others archived/readable and exportable. Never delete or trap existing data behind the paywall.

Free users may archive a vehicle and select another; they cannot maintain multiple active vehicles. Restore/import of backups containing multiple vehicles must preserve all data, then apply the active-vehicle policy without destructive truncation.

Do not hardcode a purchase price. A provisional business idea is a modest local-market one-time price, but the actual amount must be set in Google Play Console and displayed from the live store product.

Paywall entry points: add a second active vehicle, open an advanced report, generate a Pro PDF, and a clearly labeled Settings entry. Existing basic statistics and reminders remain usable.

## 16. Complete English, Russian, and Ukrainian localization

Implement real Android resource localization, not a manually maintained in-memory dictionary:

- `res/values/strings.xml`: complete English default resources.
- `res/values-ru/strings.xml`: complete Russian resources.
- `res/values-uk/strings.xml`: complete Ukrainian resources.
- `res/xml/locales_config.xml`: supported language declaration, generated or explicit according to the selected supported Android setup.

Settings must offer System default, English, Русский, Українська. Use the official per-app language APIs with supported backward compatibility. Keep locale handling in the appropriate platform layer. Do not use deprecated global `Locale.setDefault()` hacks, manually patch resource contexts throughout the app, or change only the Activity title while leaving the rest untranslated.

Localize everything user-facing:

- Onboarding, navigation, screen titles, buttons, form labels, hints, validation.
- Calendar month/day labels, empty/error states, snackbars, dialogs, accessibility descriptions.
- Maintenance categories/status, fuel/expense categories, unit labels, explanation text.
- Notification channels/content/actions and calendar event descriptions.
- Paywall, purchase/restore messages, price surrounding text, permanent-unlock language.
- AI consent, suggested prompts, error states, input hints, and structured advice headings.
- PDF/CSV export headings and report explanatory text.
- Settings, privacy summary, backup/restore, deletion confirmations.

Use resource placeholders and plurals rather than concatenated fragments. Preserve positional placeholder types across translations. Russian and Ukrainian require proper plural handling. Use locale-aware numeric/date/currency formatting, and accept the selected locale's decimal separator in forms. Language changes must not alter stored vehicle records, currency, or units.

Persist custom titles/notes exactly as entered; do not automatically translate user data. Persist built-in category enum IDs rather than localized strings. Old AI responses keep their original language; new responses use the currently selected app language. Do not regenerate history automatically after a language change.

Use this terminology consistently:

| English | Russian | Ukrainian |
| --- | --- | --- |
| Overview | Обзор | Огляд |
| Maintenance | ТО | ТО |
| Journal | Журнал | Журнал |
| Statistics | Статистика | Статистика |
| AI Garage | ИИ-гараж | ШІ-гараж |
| My vehicles | Мои автомобили | Мої автомобілі |
| Add vehicle | Добавить автомобиль | Додати автомобіль |
| Make | Марка | Марка |
| Model | Модель | Модель |
| Model year | Год выпуска | Рік випуску |
| Engine | Двигатель | Двигун |
| Odometer | Пробег | Пробіг |
| Last recorded mileage | Последний указанный пробег | Останній зазначений пробіг |
| Add fuel | Добавить заправку | Додати заправку |
| Fuel volume | Объём топлива | Об’єм пального |
| Fuel spending | Расходы на топливо | Витрати на пальне |
| Fuel consumption | Расход топлива | Витрата пального |
| Full tank | Полный бак | Повний бак |
| Total paid | Оплачено | Сплачено |
| Add expense | Добавить расход | Додати витрату |
| Log service | Записать обслуживание | Записати обслуговування |
| Next maintenance | Следующее ТО | Наступне ТО |
| Due soon | Скоро срок | Незабаром термін |
| Due | Пора выполнить | Час виконати |
| Overdue | Просрочено | Прострочено |
| Mark as completed | Отметить выполненным | Позначити виконаним |
| This month | Этот месяц | Цей місяць |
| Previous month | Прошлый месяц | Минулий місяць |
| This year | Этот год | Цей рік |
| All time | За всё время | За весь час |
| Custom period | Выбрать период | Вибрати період |
| Not enough data | Недостаточно данных | Недостатньо даних |
| Potential weak points | Возможные слабые места | Можливі слабкі місця |
| Upgrade ideas | Идеи улучшений | Ідеї покращень |
| Compatibility needs checking | Совместимость нужно проверить | Сумісність потрібно перевірити |
| General guide | Общая памятка | Загальна пам’ятка |
| Save as note | Сохранить как заметку | Зберегти як нотатку |
| Create maintenance draft | Создать черновик ТО | Створити чернетку ТО |
| Unlock Pro | Открыть Pro | Відкрити Pro |
| One-time purchase | Разовая покупка | Одноразова покупка |
| Restore purchases | Восстановить покупки | Відновити покупки |
| Purchase pending | Покупка ожидает подтверждения | Покупка очікує підтвердження |
| Language | Язык | Мова |
| System default | Как в системе | Як у системі |
| Export data | Экспорт данных | Експорт даних |
| Back up data | Создать резервную копию | Створити резервну копію |
| Restore backup | Восстановить из копии | Відновити з копії |
| Delete all local data | Удалить все локальные данные | Видалити всі локальні дані |

This table seeds vocabulary; it is not the complete string list. Create and translate all additional required strings. Use natural Ukrainian phrasing, not Russian text in Ukrainian resource folders. Check apostrophes, XML escaping, and Android string-resource escaping.

## 17. Architecture and packages

Start in one `:app` Gradle module, with feature packages. Avoid premature Gradle modularization and empty architecture layers. Recommended structure, adapted to actual repository conventions:

```text
app/
  src/main/java/<existing-or-chosen-package>/
    app/
      DriveCareApplication.kt
      MainActivity.kt
      DriveCareApp.kt
      navigation/
    core/
      model/
      ui/
        components/
        theme/
      formatting/
      time/
      notifications/
    data/
      local/
        database/
        dao/
        entity/
        preferences/
        attachments/
      remote/
        ai/
      repository/
      billing/
    domain/
      fuel/
      maintenance/
      statistics/
      export/
    di/
    feature/
      onboarding/
      garage/
      overview/
      maintenance/
      journal/
      fuelentry/
      expenseentry/
      statistics/
      aigarage/
      paywall/
      settings/
  src/main/res/
    values/
    values-ru/
    values-uk/
    xml/
docs/
```

The tree is a responsibility map, not a mandate to create unused files. Do not introduce mechanical one-line use cases for every DAO operation.

Data flow:

UI action → screen ViewModel → repository/domain calculation → Room or SDK data source → repository Flow → immutable UiState → Compose.

Screen contract:

- `FeatureRoute` obtains/scopes the ViewModel, collects state using `collectAsStateWithLifecycle()`, and wires navigation.
- `FeatureScreen` takes immutable state and callbacks and can be previewed without Hilt, a database, purchases, or a live AI connection.
- Keep MutableStateFlow private; expose read-only StateFlow.
- Use SavedStateHandle for appropriate small restoration inputs and record IDs, not full databases or giant chat objects.
- Keep durable save/purchase completion outcomes in acknowledged state; do not rely on a lossy transient event as the only record of success.
- Keep Activity, navigation controllers, Views, and UI Context out of ViewModels. Platform-only services belong behind injected boundaries.
- Scope jobs with structured concurrency and preserve CancellationException. Do not catch cancellation as an ordinary user-visible failure.
- Use Room as the source of truth for local records. Avoid separate mutable lists that diverge from persisted data.

## 18. Stack selection and dependency compatibility

Before edits, inspect the existing project, AGENTS instructions, Gradle wrapper, version catalog, plugins, current SDK/JDK, source sets, manifest, formatter, and UI conventions. Preserve a sound existing setup.

For a new project, use verified stable versions of:

- Kotlin, Compose, Material 3, Activity Compose.
- Lifecycle ViewModel, lifecycle-runtime-compose, Coroutines/Flow.
- Stable Navigation 3 for a new Compose-only app, with its current saveable state and ViewModel-store integration. Preserve existing working Navigation Compose in an existing project unless a migration is actually required.
- Hilt with KSP for this multi-screen app.
- Room with KSP and exported schemas.
- DataStore for preferences.
- WorkManager for delay-tolerant maintenance notification work.
- Firebase AI Logic, App Check, and Remote Config with the supported stable Firebase BOM.
- RevenueCat native Android SDK.
- Kotlin serialization when needed for AI contracts and portable backups.
- Coil only if needed to display local selected images asynchronously.
- One supported formatter integration, preferably the existing ktlint/Spotless setup.

Do not add Retrofit/OkHttp just to duplicate the official Firebase SDK, RxJava, multiple DI frameworks, multiple persistence stacks, or unnecessary SDKs.

Check AGP/Gradle/JDK compatibility, effective Kotlin compiler version, Compose compiler plugin alignment, KSP/Room/Hilt support, and dependency minimum SDKs. AGP 9+ may use built-in Kotlin: do not blindly add `org.jetbrains.kotlin.android` on top of built-in Kotlin. Compose BOM does not select the Kotlin compiler, AGP, Room, Hilt, or Navigation versions.

Choose minSdk from actual requirements; API 24 is a candidate only if every selected stable dependency and feature supports it. If newer dependencies require a higher minimum, document and explain the choice instead of forcing unsupported versions. Set compile/target SDK to the current stable, compatible requirements verified at implementation time.

Use Gradle Kotlin DSL, a checked-in Gradle Wrapper, and `gradle/libs.versions.toml` with pinned versions. Do not use dynamic `+` versions or snapshots. Document the verified stack, date, compatibility notes, and official source links in `docs/android-stack.md`.

## 19. Persistence model and integrity

Use UUID or another stable, collision-safe record ID. Keep vehicle ownership relations explicit and use indexed foreign keys. Suggested persisted concepts:

- Vehicle.
- Dated odometer reading, with originating record ID/type where relevant.
- Fuel entry.
- Maintenance plan.
- Maintenance completion/service record.
- Other expense entry.
- Attachment metadata with a safe app-private relative file reference.
- AI conversation and messages/structured advice.
- Settings/preferences in DataStore.

Define timestamps, date semantics, currency codes, units, full/missing-fill flags, and stable category IDs explicitly. Do not use localized labels as primary keys or enum storage values.

Use Room transactions for related writes, plan completion, backup restore, and delete operations affecting several tables. Preserve database schema history and implement real migrations as the schema evolves. No destructive-migration fallback in production.

Ask for confirmation before deleting a vehicle and all linked records. Explain what will be removed. Archive is a separate reversible action. Remove orphaned attachment files after successful transactions; failed writes must not leave inconsistent references.

No production records should depend on a transient URI grant. Use the system photo/document picker, copy accepted attachments into app-private storage with generated filenames, and persist durable references. Avoid broad storage permissions.

## 20. Notifications

Maintenance reminders are date-level, delay-tolerant reminders, not alarm-clock alarms. Use supported WorkManager scheduling/notification patterns and document that device power management can delay background delivery.

- Create localized notification channels.
- Request notification permission only after the user enables reminders or understands the benefit, following current platform requirements.
- Handle denial and disabled channels gracefully; in-app due status still works.
- Reschedule/reconcile work after plan edits, completion, vehicle changes, app upgrades, relevant timezone changes, and restored data as appropriate.
- Use stable IDs and idempotent work so the same task/day does not generate repeated notifications.
- Deep-link a tapped reminder to the correct vehicle/task.
- Include appropriate immutable PendingIntent flags and exported-component declarations.
- Do not request exact-alarm permission without a new product requirement that justifies it.
- Recalculate mileage-based warnings after actual new odometer readings. Do not claim background driving detection or request location permission.

## 21. Tablet, accessibility, and configuration changes

Support compact, medium, and expanded windows:

- Constrain form widths.
- Use multi-column dashboard/statistics layouts when space permits.
- Use list-detail maintenance/journal presentation on expanded windows where practical.
- Keep important actions reachable without stretching them across a tablet.
- Handle portrait, landscape, split-screen, and resizing without forced orientation.
- Preserve selected vehicle, selected date/range, edit drafts, and navigation state appropriately.
- Respect edge-to-edge system bars, display cutouts, keyboard/IME insets, and large font scales.
- Use accessible touch targets, TalkBack descriptions, and state semantics.
- Charts and calendar indicators must have usable text equivalents.

There is no universal manifest “tablet support” switch. Do not mark ordinary optional device hardware as required or prevent tablet installation accidentally. Verify current large-screen platform behavior rather than assuming a phone-sized fixed layout is adequate.

Provide previews for primary screen states, forms, empty states, and representative expanded layouts. Preview-only fixtures must not leak into production repositories.

## 22. Backup, export, and data control

Free data portability:

- CSV export of actual fuel/service/expense records through Storage Access Framework.
- Full versioned local backup/restore using a documented portable archive containing a JSON manifest/data and included attachments.
- Preserve original dates, units/currencies, IDs, relationships, and schema version. Stable enum IDs remain language independent.
- Explain that the exported backup contains the user's records and selected attachments. Let the user choose where it is saved.
- Restore validates archive paths, size limits, required fields, foreign keys, and data version before modifying live data. Prevent path traversal and resource exhaustion.
- Version 1 restore may replace all local vehicle/journal data after explicit confirmation. Do not merge silently or duplicate records. Keep preferences and entitlement handling deliberate.
- Never restore a `pro=true` flag from the archive. Purchase entitlement comes from RevenueCat/Google Play, not a backup field.
- Reject invalid/unsupported backups without partial deletion or partial replacement. Use staging plus a transaction and clean file handling.
- Do not export billing tokens, App Check tokens, secrets, SDK cache state, or cloud configuration credentials in the portable user backup.

Pro PDF report:

- Generate a real paginated PDF for a selected vehicle, period, and currency.
- Include vehicle identity, period, fuel and expense totals, service history, relevant units, and calculated-data explanations.
- Clearly separate paid records, estimated/planned costs, and incomplete consumption data.
- Render English, Russian, and Ukrainian characters correctly, including Ukrainian letters and apostrophes.
- Handle long notes, large histories, page breaks, and repeated table headings without clipped content.
- Export/share through supported platform APIs/FileProvider with limited URI grants.

Settings must also offer individual record deletion, clear AI history, and a clearly confirmed Delete all local data action. Deleting local data is not a refund and must not be presented as cancelling a purchase.

## 23. Missing external configuration and honest build behavior

Implement the real Firebase and RevenueCat integration code, but do not fabricate project IDs, keys, offerings, production entitlements, or successful AI responses/purchases.

The offline app must build and run even when the developer has not supplied external configuration yet:

- Guard Firebase/Google Services setup appropriately in the build and runtime using documented supported patterns.
- Provide explicit NotConfigured states behind the same repository interfaces. This is a truthful unavailable state, not a fake production service.
- Do not invoke Firebase or RevenueCat as if configured and crash on startup.
- Disable purchase submission if no live product/price is available, and show a useful unavailable state.
- Keep developer-only diagnostics out of ordinary product copy.
- Use build properties/environment for public SDK configuration where appropriate; never check administrative secrets into the repo.
- Clearly document what a missing `google-services.json`, unregistered SHA fingerprint, missing debug App Check token, missing offering, wrong package ID, or unsigned/unpublished store build prevents.

Do not remove security controls, bypass entitlement verification, or use a fake purchase button to make the app appear complete. A debug-only manual entitlement override may exist only if explicitly separated by source set/build variant, labeled, and impossible in release; it does not verify real purchasing. It is not required if it adds unnecessary scope.

External console actions require real accounts/configuration. Finish all implementation possible in the repository, then list exact remaining setup steps and which capabilities are unavailable until they are completed. Do not claim live integrations were verified when they were not.

## 24. Source readability and formatting

Readability is an acceptance requirement:

- Four-space indentation and a preferred 100-column width, except literals/URLs that should not be broken artificially.
- Multi-line Compose builders, event handlers, lambdas, and coroutine bodies, including single-statement handlers.
- One statement per line; no semicolon-packed code.
- One Compose modifier per line.
- Multi-step Flow/collection chains split before continuation calls, while ordinary namespace/property access stays intact.
- Wrapped function declarations and calls use one parameter/argument per line, named arguments where helpful, and consistent closing delimiters.
- Extract named components/helpers when nesting obscures intent. Do not create giant screens containing all business logic.
- English descriptive names and explicit meaningful types.
- Complete correct imports; follow the formatter/project's wildcard-import policy.

Reuse one canonical formatter. If none exists, add the smallest supported pinned integration and `.editorconfig` compatible with the selected engine. Verify actual task names. Exclude generated/vendor/build files and do not incidentally reformat existing tests or unrelated files.

Run the apply task and its non-mutating check. Then inspect the diff for compressed callbacks/chains that the formatter may not enforce. Formatting and Android Lint are separate checks. Never report a formatter command as successful unless it actually ran.

## 25. Implementation sequence

Complete the application in working vertical slices, keeping the build healthy:

1. Inspect the repository and establish/document the compatible stack.
2. Set up theme, localization, navigation, Room/DataStore, DI, and formatting.
3. Implement vehicle creation/selection and persistent garage management.
4. Implement fuel entry, journal, exact money/unit/date handling, and real basic statistics.
5. Implement maintenance plans, calendar, completion history, and reminders.
6. Implement optional AI integration, consent, typed advice, saved conversations, and reviewed draft actions.
7. Implement Pro entitlement, real offering/paywall, purchase, restore, and feature gates.
8. Implement free portability and Pro PDF reports.
9. Complete tablet layouts, accessibility, all translations, and error/empty states.
10. Run formatting, compile/lint, and available manual runtime checks. Fix failures within scope.

Do not ask for confirmation between routine reversible implementation steps. Do not create a second app/repository if the current repository is already the intended project. Preserve unrelated user edits.

## 26. Required manual verification scenarios

Do not turn these into automated tests under the current user preference. Exercise them manually when a runtime is available, and otherwise provide a precise checklist marked not executed.

1. Fresh install contains no fake records. Finish onboarding, add a vehicle, restart the app, and confirm persistence.
2. Skip optional engine/generation fields; AI asks for relevant details instead of assuming a specific engine.
3. Add partial and full fills; reproduce the 7.0 L/100 km example and verify the first full fill alone is insufficient.
4. Mark an unlogged fill; the affected interval is excluded, with a useful explanation, and later valid intervals can recover.
5. Edit/delete a historical fill; spending and consumption recompute.
6. Verify the 4,560 UAH / 75 L / 60.8 UAH per liter money example.
7. Check custom ranges, month/year boundaries, and a record near midnight in the reporting time zone.
8. Change units, language, and default currency independently; stored amounts/distances are not reinterpreted.
9. Insert a valid older odometer record; latest current mileage does not decrease. Resolve an impossible reading explicitly.
10. Complete maintenance overdue by date, by distance, and by either threshold. Confirm correct next cycle and no double-counted cost.
11. Deny notification permission; in-app maintenance remains functional. Enable it later and verify no duplicate reminder burst.
12. Confirm AI consent, cancellation, offline handling, saved-history access, malformed-response recovery, and local allowance behavior.
13. Confirm AI cannot write a maintenance plan before an explicit reviewed save.
14. Confirm no secret Gemini/service-account/RevenueCat administrative key is packaged or logged.
15. Verify actual Google Play/RevenueCat purchase, cancel, pending, already-owned, and restore scenarios with configured license testers/internal distribution. If unavailable, mark them unverified, not passed.
16. After reinstall or another eligible device, verify restoration of the non-consumed one-time product using the same store account and documented RevenueCat behavior.
17. Exercise free/Pro feature gates. Simulated revocation preserves existing vehicle data and export access.
18. Export and restore a backup with attachments and multiple vehicles; no paid entitlement is imported from the archive.
19. Export a Pro PDF in all three languages with long notes/multiple pages; verify readable characters and no clipping.
20. Check compact/expanded windows, portrait/landscape, split-screen, large text, TalkBack, keyboard avoidance, system back, process restoration, and repeated tab switching.

## 27. Build checks and handoff

Run the appropriate project/variant equivalents of:

```bash
./gradlew :app:assembleDebug :app:lintDebug
```

Run the discovered formatter apply/check pair as well. Do not invoke new test tasks or add automated test dependencies. Preserve existing checks without hiding failures. If a JDK, SDK, emulator, network dependency, signing credential, or external console configuration is missing, state precisely what was blocked and what succeeded. Do not label a build command that never ran as passed.

Deliver concise documentation:

- `README.md`: run instructions, actual implemented scope, offline/online behavior, languages, units, feature tiers.
- `docs/android-stack.md`: verified pinned versions and compatibility sources/date.
- `docs/calculations.md`: full-to-full logic, invalid intervals, weighting, currencies, period/timezone rules, odometer integrity, and maintenance recurrence.
- `docs/firebase-ai-setup.md`: actual model/provider, Firebase setup, Remote Config keys/defaults, App Check debug/release setup, enforced quotas, billing controls, privacy, and the limits of the local fair-use counter.
- `docs/purchases-setup.md`: Play product, RevenueCat non-consumable product/entitlement/offering mapping, public-key setup, server notifications, restore behavior, license testing/internal distribution, and release checks.
- `docs/manual-verification.md`: scenarios executed, results, and remaining unverified items.

End with a Russian summary stating what works, which commands actually passed or failed, what manual verification actually occurred, and the concrete external configuration still needed. Do not stop at a design proposal or hide unfinished features behind decorative buttons.

## 28. Official documentation to verify during implementation

These links are starting points, not permission to copy outdated snippets or unverified version numbers:

- Android architecture: https://developer.android.com/topic/architecture/recommendations
- Navigation 3: https://developer.android.com/guide/navigation/navigation-3
- Navigation 3 releases: https://developer.android.com/jetpack/androidx/releases/navigation3
- Android build releases: https://developer.android.com/build/releases
- Built-in Kotlin migration: https://developer.android.com/build/migrate-to-built-in-kotlin
- Compose BOM: https://developer.android.com/develop/ui/compose/bom
- Hilt: https://developer.android.com/training/dependency-injection/hilt-android
- Room: https://developer.android.com/training/data-storage/room
- WorkManager: https://developer.android.com/develop/background-work/background-tasks/persistent
- Per-app languages: https://developer.android.com/guide/topics/resources/app-languages
- Firebase AI Logic getting started: https://firebase.google.com/docs/ai-logic/get-started?platform=android
- Firebase-supported models: https://firebase.google.com/docs/ai-logic/models?api=dev
- Firebase AI production checklist: https://firebase.google.com/docs/ai-logic/production-checklist
- Firebase AI quotas: https://firebase.google.com/docs/ai-logic/quotas
- RevenueCat Android installation: https://www.revenuecat.com/docs/getting-started/installation/android
- RevenueCat non-subscription purchases: https://www.revenuecat.com/docs/platform-resources/non-subscriptions
- RevenueCat restore behavior: https://www.revenuecat.com/docs/getting-started/restoring-purchases
- Google Play Billing integration/lifecycle reference: https://developer.android.com/google/play/billing/integrate
- ktlint configuration: https://ktlint.github.io/ktlint/latest/rules/configuration-ktlint/
- Spotless Gradle: https://github.com/diffplug/spotless/blob/main/plugin-gradle/README.md
