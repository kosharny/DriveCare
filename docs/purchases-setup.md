# DriveCare Pro purchase setup

Russian step-by-step setup and manual scenarios: [purchases-setup-ru.md](purchases-setup-ru.md).

Verified on 2026-10-25. Native RevenueCat Android SDK is the only billing integration;
no parallel BillingClient client, subscriptions, consumable credits or cloud-inference
promise is included. RevenueCat validates/acknowledges the purchase using its supported
workflow. The app never consumes the product or acknowledges it a second time.

## Required external setup

1. Create/configure the real app in Google Play Console with the exact application ID.
   Configure signing and a one-time product `drivecare_pro_lifetime` with a permanent
   non-consumable unlock. Set actual local prices in Play; no price is hardcoded.
2. Configure the Android app in RevenueCat and the Google Play connection/service
   credentials in its dashboard. Administrative credentials stay in that service,
   never in the repository or APK. Configure the product's non-consumable behavior:
   RevenueCat supports non-consumable Google Play purchases in native SDK 7.11.0+.
   Review dashboard settings and avoid treating it as consumable.
3. Map the product to entitlement `pro`, offering `default`, lifetime package. The
   repository intentionally accepts only that offering's lifetime package whose
   product ID matches `drivecare_pro_lifetime`.
4. Supply the public Android SDK key via `REVENUECAT_PUBLIC_KEY` in private Gradle
   properties (`goog_...`). This is a public client identifier, not an administrative
   secret. Never supply an `sk_...` secret, service-account JSON or signing key.
5. Configure Google Play real-time developer notifications with RevenueCat according
   to its dashboard instructions, so refunds/revocations reach entitlement handling.
6. Publish a signed internal-testing build, add eligible license testers, install from
   Play and verify buy/cancel/pending/already-owned/error/restore flows. Google also supports eligible license testers using matching-package sideloaded
   debug builds after Play setup; the recommended baseline remains the internal Play
   track. Missing offerings or a local mock unlock do not verify real purchases.

The custom localized Compose paywall displays the actual store product price when
available. Missing configuration/offering disables purchase submission. Pro is granted
only by active `CustomerInfo.entitlements["pro"]`; neither a button click nor a callback
without that entitlement grants access. Pending/cancel/error states remain distinct.

CustomerInfo is refreshed on app foreground and entitlement changes are observed.
SDK-supported cached CustomerInfo can preserve a known unlock offline; a refresh error
does not overwrite an existing in-memory Pro grant. No separate editable local
`pro=true` preference exists. Backups never import entitlement or SDK cache fields.
The Activity uses the default standard launch mode so a banking-app payment detour
can return to the purchase flow.

## Restore and revocation

Restore is user initiated in the paywall, never automatically invoked each launch.
RevenueCat/Google Play restore a non-consumed eligible purchase for the store account.
Review RevenueCat's restore/transfer behavior for anonymous app users; copying an
anonymous app-user ID is not a substitute for that store purchase. Verify reinstall
and another eligible device on the same store account with the real configuration.

Free permits one active vehicle; users can archive it and add/activate another. Pro
permits multiple. If a known entitlement changes to free while several remain active,
the app asks which one to use. Activating one archives the others. All records remain
readable/exportable; no vehicle is deleted or hidden behind an export paywall. The
same selection flow follows restoring a backup containing multiple active vehicles.
Deleting local data is not a purchase refund.

## Service pricing

RevenueCat is not assumed permanently free. Its public pricing page on 2026-10-25
advertises no charge up to USD 2,500 monthly tracked revenue, then 1% of tracked
revenue at the threshold, with enterprise terms available. Verify the applicable
contract and definition of tracked revenue before launch. Google Play fees and
optional AI provider costs are separate from RevenueCat service charges.

## Official references

- https://www.revenuecat.com/docs/getting-started/installation/android
- https://github.com/RevenueCat/purchases-android/releases/tag/10.29.0
- https://www.revenuecat.com/docs/platform-resources/non-subscriptions
- https://www.revenuecat.com/docs/getting-started/making-purchases
- https://www.revenuecat.com/docs/getting-started/restoring-purchases
- https://www.revenuecat.com/pricing
- https://developer.android.com/google/play/billing/integrate
