# Android stack

Checked against official release pages on 2026-10-25. Existing AGP 9.4.1,
Gradle 9.6.0, SDK 37 and project package are preserved. Kotlin compiler was raised
from the starter's 2.2.10 to 2.4.21 because stable 2026 AndroidX dependencies resolve
Kotlin 2.4 metadata. Compose and serialization compiler plugins use the same version;
AGP's built-in Kotlin remains enabled and `org.jetbrains.kotlin.android` is absent.
The plugin resolution graph selects KGP 2.4.21 through those aligned compiler plugins.

| Component | Pinned version | Official source |
| --- | --- | --- |
| AGP | 9.4.1 | https://developer.android.com/build/releases/agp-9-4-0-release-notes |
| Gradle wrapper | 9.6.0 | https://developer.android.com/build/releases/agp-9-4-0-release-notes |
| Kotlin / Compose compiler / serialization plugin | 2.4.21 | https://kotlinlang.org/docs/releases.html |
| Compose BOM | 2026.09.00 | https://developer.android.com/develop/ui/compose/bom |
| Core | 1.19.1 | https://developer.android.com/jetpack/androidx/releases/core |
| Activity Compose | 1.13.0 | https://developer.android.com/jetpack/androidx/releases/activity |
| Lifecycle / Navigation 3 ViewModel integration | 2.11.0 | https://developer.android.com/jetpack/androidx/releases/lifecycle |
| Navigation 3 | 1.2.0 | https://developer.android.com/jetpack/androidx/releases/navigation3 |
| Hilt / compiler | 2.60.1 | https://github.com/google/dagger/releases/tag/dagger-2.60.1 |
| AndroidX Hilt Compose | 1.4.0 | https://developer.android.com/jetpack/androidx/releases/hilt |
| KSP | 2.3.12 | https://github.com/google/ksp/releases/tag/2.3.12 |
| Room / compiler / Gradle plugin | 2.8.5 | https://developer.android.com/jetpack/androidx/releases/room |
| DataStore | 1.3.0 | https://developer.android.com/jetpack/androidx/releases/datastore |
| WorkManager | 2.12.0 | https://developer.android.com/jetpack/androidx/releases/work |
| AppCompat | 1.8.0 | https://developer.android.com/jetpack/androidx/releases/appcompat |
| Firebase BOM | 35.1.0 | https://firebase.google.com/support/release-notes/android |
| Google Services plugin | 4.5.0 | https://firebase.google.com/support/release-notes/android |
| RevenueCat | 10.29.0 | https://github.com/RevenueCat/purchases-android/releases/tag/10.29.0 |
| Serialization JSON | 1.12.0 | https://github.com/Kotlin/kotlinx.serialization/releases/tag/v1.12.0 |
| Coroutines / Google Tasks bridge | 1.11.0 | https://github.com/Kotlin/kotlinx.coroutines/releases |
| Coil Compose, local files only | 3.6.3 | https://coil-kt.github.io/coil/getting_started/ |
| Spotless / ktlint | 8.10.4 / 1.8.0 | https://github.com/diffplug/spotless/releases |

AGP 9.4 supports SDK 37 and requires Gradle 9.6.0 or newer, JDK 17 or newer.
The local Studio installation is 2026.2.1, bundled JBR 25.0.3, and the existing daemon
criteria request Java 25. Build commands use that installed JBR. Java bytecode target
11 is preserved; runtime JDK and application bytecode targets need not match.

minSdk 26 permits native `java.time` semantics without core-library desugaring and
meets selected library minimums. compile/target SDK 37 use the installed stable
platform. No SDK/runtime/emulator images were downloaded. Stable releases were
selected rather than alpha versions shown in some documentation snippets.

Room schema version 1 is exported in `app/schemas`. There was no existing production
schema to migrate. Future changes must include a real migration and schema history;
there is no destructive fallback. The initial project has no Git metadata; changes
were compared against a saved pre-implementation source snapshot. Existing test
sources and their dependencies were preserved.

Spotless uses stable ktlint 1.8.0 with Android style, 100-column preference and an
explicit Compose/domain wildcard-import policy. Multi-line callbacks are preserved.
Source: https://github.com/ktlint/ktlint/releases/tag/1.8.0
