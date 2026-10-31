# Verification record

Implementation/check date: 2026-10-25. No automated test sources/dependencies were
added or modified. No unit/instrumentation/test/benchmark execution tasks were run.
Lint's existing `generate...TestLintModel` / `lintAnalyze...Test` tasks inspect preserved
source sets; they are not test executions.

## Logo, startup and estimated-mileage revision — 2026-10-25

Current debug source check: `./gradlew spotlessApply spotlessCheck :app:assembleDebug
:app:lintDebug` — **BUILD SUCCESSFUL**, final pass 35 seconds, 59 tasks (17 executed,
42 up-to-date). Lint: **0 errors, 20 warnings**. Warnings remain on preserved legacy
resources/labels, manifest metadata and a newer Gradle suggestion. No suppression,
new dependency, test source change or automated test execution was introduced.

- Created original SVG logo masters, a visually reviewed 512×512 PNG render,
  adaptive foreground/background/monochrome launcher resources and animated Compose
  loading with the same mark. Reviewed actual recorded startup frames; progress
  arc moves between frames. Intro timing follows the native splash exit on API 31+.
  Final layout uses an opaque base to prevent the native window logo showing through.
  Overview waits for its own loaded state rather than briefly showing an empty garage.
  Rechecked the final recorded frames: no duplicate background logo, visible rotating
  arc, then continued loading until Overview data is available.
- Read available logs from the user's connected phone without installing or modifying
  its app. Current DriveCare process was 14395. The retained relevant lines at 20:17
  and 20:20 contain `Auth not registered, skipping`; source inspection of Firebase AI
  18.1.0 confirms that this refers to absent optional Firebase Authentication. The
  old App Check API-disabled 403 was not in that retained fragment. This is not a
  claim of verified successful generation. No question/answer body was read.
  The temporary full device-log capture was removed after filtering. New debug
  `DriveCareAI` and `DriveCareBilling` messages contain status/error metadata only.
- Reused existing Pixel_10a (`emulator-5554`) with no snapshot rollback or downloads.
  Before update: Room v1, one vehicle, two records, one plan. Updated in place;
  Room v2 migration succeeded with those rows intact. Manually edited its existing
  40 L / 2,400 UAH fill, enabled estimate mode and entered 8 L/100 km. The form showed
  500 km; overview showed **≈ 10,500 km** with verified **10,000 km** separately.
- Saved a new actual odometer reading **10,450 km**; overview returned to actual
  10,450 km and excluded the old fuel estimate. Stopped-app SQLite inspection:
  schema 2, one vehicle, three records (the extra actual verification), one plan;
  fuel remains `odometerKm=NULL`, `liters="40"`, `amountMinor=240000`, assumed
  consumption `"8"`, and an anchor reference. Original 10,000 km and new 10,450 km
  remain separate actual records. Financial values and plan data stayed intact.
- All 316 locale resource names match across EN/RU/UK. Prior 17 user-added WebP
  assets were compared with the before-edit snapshot and remain byte-identical.
  Existing Firebase JSON is preserved and was not printed. Schema v1 history remains.

Unverified: store-signed releases, real RevenueCat/Play configuration and payments,
all purchase/pending/refund/restore scenarios, final CSV/PDF/backup round trips,
same-minute anchor ordering on a device, exhaustive edit/delete/partial estimates,
API 26 startup, dark mode/large font/TalkBack/rotation combinations. These require
manual checks; the new Russian purchase guide gives exact setup and expected results.
The unsigned release APK from earlier passes predates this revision; deliver the
current debug APK or build a newly signed release from current sources.

## Latest UX revision — 2026-10-25

This section describes the current source and debug APK. Earlier sections below
record the original implementation pass; its release artifact predates this revision.

- Final command: `./gradlew spotlessApply spotlessCheck :app:assembleDebug :app:lintDebug`.
  **BUILD SUCCESSFUL**, 58 tasks (18 executed, 40 up-to-date), 27 seconds.
- Current Debug Lint: **0 errors, 18 warnings**. Remaining warnings cover existing
  manifest/launcher cleanup, unused colors/strings (including replaced form labels)
  and a newer Gradle suggestion. No checks were suppressed or disabled.
- All 301 string/plural names match across English/Russian/Ukrainian; XML parsed,
  duplicate resource names rejected by a structural inspection. All 17 artwork slots
  have bundled vector fallbacks and corresponding WebP filenames in the image brief.
- No dependencies, database schema or test sources were changed in this UX revision.
  No automated test execution was requested or run.

Manual runtime checks used the already installed **Pixel_10a**, Android 17/API 37,
`emulator-5554`. No device/image was created or downloaded. No physical phone was
modified. The existing emulator boot snapshot opened onboarding; its pre-existing
preferences/data were not cleared to force onboarding.

1. Visually reviewed all three onboarding slides, their illustrations, text and
   next/get-started actions. Completed onboarding.
2. Created Toyota Corolla 2021 through the two-step form, searched Toyota, selected
   Corolla, selected the year and entered actual 10,000 km. The model selector also
   showed remotely sourced 4Runner, beyond the offline Toyota seed. Manual entry and
   regional-coverage explanation are available; every catalogue/locale edge is not
   represented as a completed runtime check.
3. Opened the native date picker, cancelled it, selected “in a month” and observed
   **2026-11-25**. Selected date-or-mileage and “another 5,000 km”; observed **15,000 km**.
   Saved Engine oil with both thresholds and collapsed optional fields.
4. Saved a **40 L**, **2,400 UAH**, full-tank fuel record without enabling actual
   mileage. Overview displayed 2,400 UAH fuel spending and retained **10,000 km**.
   Record details explicitly displayed “Odometer not recorded”.
5. Read the stopped app's Room database manually: initial record `ODOMETER`,
   `odometerKm="10000"`; fuel `odometerKm=NULL`, `liters="40"`, `amountMinor=240000`,
   `currency="UAH"`, `fullTank=1`. Plan initial date `2026-11-25`, initial distance
   `"15000"`, no repeat intervals. No estimated distance was stored.
6. Updated the APK in place and relaunched. The vehicle and records remained
   available. Visually checked overview header and AI Garage without any hero image;
   unavailable AI state correctly reflects the absent Firebase JSON.
7. Resized the same emulator temporarily to 2424×1080. Found and fixed oversized
   artwork: its explicit height now depends on width and window height, with Fit
   content. Visually confirmed the full illustration; scrolled the navigation rail
   and confirmed Statistics and AI Garage remain reachable. Restored original
   1080×2424 resolution. No AndroidRuntime fatal exception appeared in inspected logs.
   The emulator's unrelated system UWB service reports native failures; the app
   journey remained usable.

Still unverified in this revision: real Firebase replies/App Check configuration,
final user-supplied WebP artwork, exhaustive offline catalogue/cache/error branches,
all locale/large-font/TalkBack combinations, repeat-plan completion and backup round
trip containing a new nullable fuel reading. The broader console/device checklist
below remains applicable. See `docs/ai-chat-setup-ru.md` and the root
`DriveCare_Image_Briefs_RU.txt` for configuration and artwork handoff.

## Build and structural checks

- `:app:assembleDebug`: passed during implementation and again with integrated UI.
- `:app:assembleRelease`: passed; produces an unsigned release APK. No publishing or
  signing credentials were supplied.
- `:app:lintDebug`: passed, with warnings recorded in the generated report; no rule
  was disabled to hide failures.
- `spotlessApply` / `spotlessCheck`: passed with ktlint 1.8.0; tests/generated/build
  files are excluded. All remaining final command outcomes are recorded below.
- English/Russian/Ukrainian resource-key equality and XML parsing: passed, 255 keys
  per locale including the plural resource. Android resource compilation also passed.
- Room version-1 schema is exported and indexed foreign keys are present. No
  destructive migration fallback, raw Gemini key, service-account configuration,
  fake production records or local Pro override was added.

## Runtime environment and executed scenarios

Physical Android device: installation was attempted but the device returned
`INSTALL_FAILED_USER_RESTRICTED` / install cancelled; no physical-device run passed.
The already-existing Pixel_10a AVD was reused headlessly. No AVD or system image was
created/downloaded. The emulator reports API 37. Its existing snapshot was loaded without saving a replacement
snapshot on exit.

Executed on that emulator:

1. Installed/launched the offline debug APK with no Firebase/RevenueCat config.
2. Viewed the three-page introduction and used Skip. The fresh garage had no vehicles,
   fuel entries, expenses, service history or artificial AI results.
3. Opened vehicle setup and manually entered Toyota Corolla, model year 2020,
   10,000 km, explicit UAH default and initial date 2026-10-25. Optional technical
   details were left empty. Saved successfully.
4. Observed the common maintenance category offer with a user-confirmation/manual
   interval explanation. Skipped it.
5. Force-stopped and relaunched the process without clearing data. The vehicle,
   10,000 km dated reading and empty financial history remained.
6. Inspected the compact overview visually. App startup/these operations produced
   no AndroidRuntime fatal exception in the inspected log.

7. Added a manually entered full fill: 2026-10-25 10:00, 10,000 km, 40 L,
   2,400 UAH. The form calculated 60 UAH/L. Overview showed fuel and ownership totals
   of 2,400 UAH, not a duplicated amount.
8. Opened Statistics. Observed 1 fuel entry, 40 L purchased, 60 UAH/L weighted price,
   2,400 UAH fuel, zero service/other subtotals. Scrolled to consumption and observed
   “Not enough data” with the full-to-full attribution explanation after the first full fill.

9. Changed per-app language to Russian in Settings. The Activity recreated and
   retained Settings navigation; labels/body copy changed to Russian while UAH,
   kilometers, liters and Europe/Kyiv remained unchanged. Repeated with Ukrainian
   and observed Ukrainian settings/backup copy with the same stored preferences.

10. Created a ZIP backup through the system document picker. The app acknowledged
    success. Inspected `manifest.json`: one vehicle, two journal records, no billing
    or SDK credentials. This revealed that default serialization omitted the backup
    version; the contract was corrected to make version mandatory. The original
    trial archive without a version was then selected for restore and was rejected
    with a localized error before replacing live data. A corrected re-export was inspected: explicit `version: 1`, one vehicle,
    two records (`ODOMETER`, `FUEL`) and original UAH. Selected that corrected archive in the system picker and completed restore;
    the app acknowledged success (“Готово”). No attachment or multi-vehicle restore
    was exercised in this run.

11. Temporarily resized the same emulator to 2560×1600. The app switched to a
    navigation rail. Overview and Statistics displayed summary cards in two columns;
    the Ukrainian wide statistics screen was visually inspected. It still showed
    one fuel entry, 40 L and 2,400 UAH after restore/update. Restored the original
    screen size afterwards. Large-font/TalkBack, split-screen and other edge cases
    remain unverified.

12. Switched appearance to Dark in Settings. The explicit theme preference persisted.
    The final theme also updates status/navigation icon appearance to match the
    selected theme. The final dark record-details screen was visually inspected;
    text, controls and system icons were readable.

13. Edited the initial 10,000 km odometer reading, changed its draft distance unit
    to miles (displayed `6213.71192237`) and saved without changing the number. Save
    succeeded with a later fuel reading at the same 10,000 km. Read the debug Room
    database manually: the initial record retained exact canonical `odometerKm =
    "10000"`, retained input unit `mi`, while fuel stayed `"10000"`, 40 L, 240000 minor
    UAH. This confirms no conversion drift or false chronology conflict in that journey.

Runtime fixtures exist only on the reused emulator; no fake records are bundled in
production. Scenarios are never considered passed merely because code compiles.

## Remaining device / console checklist

Mark each scenario executed only after using the actual current APK/configuration:

- Full-to-full 7.0 L/100 km example; first full baseline, partial fills, missing-fill
  exclusion/recovery, historical edit/delete recomputation, mixed/LPG explanation.
- 4,560 UAH / 75 L / 60.8 UAH/L money example; mixed currencies; all date presets,
  custom range and reporting-zone midnight/DST boundaries.
- Valid historical odometer insertion, chronology conflict and explicit correction;
  units/language/currency independence.
- Calendar month navigation, day selection, mileage-only plans, date/either-threshold
  completion, calendar-month recurrence, completion correction/deletion and cost counted once.
- Notification denial and later grant; no same-day duplicates; completed/paused plan
  notification removal, deep links, upgrade/time-zone reconciliation and power-delay behavior.
- AI consent/revocation, minimum context, cancellation, timeout/offline/rate limits,
  malformed JSON, successful-response allowance/cache, local history and reviewed drafts.
- Real Play/RevenueCat buy/cancel/pending/already-owned/restore, reinstall/other-device
  restore, refund/revocation and retained multi-vehicle archives. Requires real consoles,
  signed Play internal build and license testers; never simulated as a passed purchase.
- Backup/replace-restore with photos, receipts, PDF attachments and several vehicles;
  invalid archive/path/size rejection without live-data replacement; CSV all languages.
- Pro PDF in English/Russian/Ukrainian with long notes, many pages, glyphs and no
  clipped text. Requires a real Pro entitlement; there is no production bypass.
- Compact/expanded resize, portrait/landscape, split-screen, large font, TalkBack,
  IME avoidance, predictive/system back, process-restored drafts and tab scroll/filter state.

## Artifacts

Debug APK: `app/build/outputs/apk/debug/app-debug.apk`.
Release APK: `app/build/outputs/apk/release/app-release-unsigned.apk`.
Lint report: `app/build/reports/lint-results-debug.html` (and text/SARIF companions).
External setup still required: actual Firebase Android app/JSON, SHA registration,
App Check enforcement/tokens/Play Integrity, provider quotas/billing/privacy plan;
RevenueCat public Android key/product/entitlement/offering, Play product/signing,
server notifications, internal distribution and license testers.

## Final command results

Final combined command executed successfully:

```sh
./gradlew spotlessCheck :app:assembleDebug :app:lintDebug :app:assembleRelease
```

`BUILD SUCCESSFUL` on the final source pass (110 tasks; 33 executed, 77 up-to-date,
3m 20s). Formatting was also applied and checked successfully.
`spotlessApply` also passed beforehand. Debug Lint: **0 errors, 13 warnings**. Warnings
include SDK-gated locale manifest metadata, existing launcher/resource cleanup,
redundant label, and a newer compatible Gradle suggestion. They were not suppressed.
Release is compiled/optimized and unsigned; it was not published or installed as a
store-signed production build. App Check debug/release source separation compiled.

The final debug APK was updated onto the existing emulator successfully. The final
Ukrainian overview still showed Toyota Corolla, 10,000 km and 2,400 UAH after the
update; no AndroidRuntime fatal exception was found in the inspected log.
All remaining console-dependent and exhaustive edge-case scenarios above remain
explicitly unverified; passing compilation is not their runtime verification.
