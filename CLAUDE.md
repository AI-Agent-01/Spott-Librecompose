# claude.md — Spott Project Brain (Aligned)

> **Mission:** Build **Spott** (Airbnb for parking) **Android‑first** using **Kotlin Multiplatform + Compose Multiplatform** with a clean **expect/actual map seam** (Android actual: **MapLibre Compose** with **MapTiler** tiles; iOS actual later: **MapLibre iOS** or **MapKit** via UIKit interop). This file is the **single source of truth** for how Claude Code should respond and what to build next in this repo.
>
> **Status:** This document **supersedes any conflicting older guidance** (e.g., versions that referenced Google Maps Compose). Keep to Android‑first, KMP shared logic, and Compose Multiplatform for shared UI.

---

## 0) Repo Fit & Guardrails (Read First)

**This repository currently uses a single KMP module named `composeApp` with `commonMain`, `androidMain`, `iosMain`.** Make the following **non‑negotiable adjustments** before generating new code:

1. **Application ID & Packages**

   * Android app id: **`com.getspott.spott`**.
   * Kotlin packages must live under **`com.spott`** (e.g., `com.spott.feature.map`, `com.spott.core.model`).
   * **Replace any template namespace** (e.g., `com.jetbrains.kmpapp`) with `com.spott.*` and fix imports.

2. **Remove Template Sample**

   * Delete the template museum sample (models, API, storage, screens, DI).
   * Keep DI wiring but **replace modules** with Spott modules described below.

3. **Modules (for now)**

   * Stay **single‑module** (`composeApp`) but mirror a modular structure via **packages**:
     `com.spott.core.model`, `com.spott.core.network`, `com.spott.core.database`, `com.spott.feature.auth`, `com.spott.feature.parking`, `com.spott.feature.host`, `com.spott.feature.profile`, `com.spott.feature.notifications`, `com.spott.map`.
   * A true multi‑module split can come later; **don’t** change Gradle module topology yet.

4. **Navigation Policy**

   * **Drawer‑only**. No bottom bar, no rail.
   * Modal drawer on compact width; Permanent drawer on expanded.
   * **Search page is not in drawer**; it opens **only** by tapping the **floating SearchBar** on the Home Map.

5. **Maps on Android**

   * Use **MapLibre Compose** for all map UI.
   * Tiles/styles come from **MapTiler** using a **style JSON URL with `?key=`**.
   * Hide platform specifics behind the **map seam**. All map logic funnels through `com.spott.map`.

6. **Design System**

   * Material 3 (Compose) on Android with Spott tokens.
   * Respect **WCAG 2.2 AA**, touch targets ≥48dp, TalkBack order, contrast.

7. **Performance & StrictMode**

   * Cold start ≤ **2.5s** (MVP), map interactions **60fps**, main thread clean (no I/O).
   * Fix any StrictMode violations by offloading to Dispatchers.IO.

---

## 1) Operating Context & Constraints

* **Platform strategy:** Android now; iOS later. Shared logic, models, and **shared UI** contracts live in **`commonMain`**. Platform specifics (maps, permissions shims, payments UIs) sit behind **expect/actual**.
* **Front‑end focus:** This brain outputs **front‑end artifacts** (screens, flows, UI contracts). It may define **interfaces** that backend implements; it must not scaffold server code.
* **Libraries (Android):** Compose Material 3, **MapLibre Compose**, Google Places (for search), Coil 3, Koin 4, Kotlinx Serialization, Ktor (shared), SQLDelight (shared, optional), Accompanist/Permissions if needed.
* **Security/Privacy:** No PAN storage; minimize PII in logs; secure tokens in Keystore; OS permission prompts with pre‑education screens.

---

## 2) Golden Rules for Claude’s Responses

1. **Deliverables shape:** Start with **Next 5 Tasks** (≤5, shippable), then artifacts (contracts/composables/diffs). If scope >5, queue follow‑ups.
2. **No guessing:** If unknowns exist, set **sane defaults** with `// TODO(default)` and proceed.
3. **Android‑first fidelity:** Use Material 3 patterns and Android idioms; keep shared contracts iOS‑ready.
4. **KMP discipline:** Interfaces & models in **commonMain**. Platform APIs live in **actual**. **Never** import Android types in common.
5. **Consistency:** Stable names, packages, and routes. Every screen defines **UiState + Intent + Effect** and a **Preview**.

**Response Format (STRICT):**

* **Section A — Next 5 Tasks** (checklist)
* **Section B — Artifacts** (UI contracts, composables, routes, diffs)
* **Section C — Acceptance Criteria** (bullet list)
* **Section D — Notes/Assumptions** (short)

---

## 3) Map Integration Strategy (expect/actual seam)

**Common API (`commonMain`)**

```kotlin
package com.spott.map

import androidx.compose.runtime.Composable

// Core map surface
expect fun SpotMapView(
    props: SpotMapProps,
    onEvent: (SpotMapEvent) -> Unit
)

data class SpotMapProps(
    val camera: CameraModel,                 // target or bounds
    val myLocationEnabled: Boolean,
    val markers: List<SpotMarker>,           // price markers & destination
    val polylines: List<MapPolyline> = emptyList(),
    val clustersEnabled: Boolean = true,
)

data class CameraModel(
    val target: LatLng? = null,
    val bounds: LatLngBounds? = null,
    val paddingDp: Int = 48,
    val zoom: Float? = null,
)

data class SpotMarker(
    val id: String,
    val position: LatLng,
    val type: MarkerType,                    // PRICE, DESTINATION, PARKED, TOP_SPOTT
    val label: String? = null,               // e.g., "$7/h"
    val draggable: Boolean = false
)

enum class MarkerType { PRICE, DESTINATION, PARKED, TOP_SPOTT }


data class MapPolyline(
    val id: String,
    val points: List<LatLng>,
    val style: PolyStyle = PolyStyle.Driving
)

enum class PolyStyle { Driving, Walking }


data class LatLng(val lat: Double, val lng: Double)

data class LatLngBounds(val southWest: LatLng, val northEast: LatLng)

sealed interface SpotMapEvent {
    data class OnMapReady(val visibleRegion: LatLngBounds?): SpotMapEvent
    data class OnCameraIdle(val bounds: LatLngBounds): SpotMapEvent
    data class OnMarkerClick(val id: String): SpotMapEvent
    data class OnLongPress(val position: LatLng): SpotMapEvent
    data class OnMarkerDragEnd(val id: String, val position: LatLng): SpotMapEvent
}
```

**Android actual (`androidMain`)**

* Implement `SpotMapView` using **MapLibre Compose** (`MaplibreMap`, `BaseStyle`, `rememberCameraState`).
* Style source: **MapTiler** style JSON URL with API key from `BuildConfig` (see §14 MapTiler Integration).
* Render **price pins** / **Top Spott** via MapLibre annotations; cluster where needed.
* Translate Compose map callbacks → `SpotMapEvent`.
* Ornaments & attribution: use `material3` helpers (e.g., compass, scale bar, attribution button). Attribution must be visible.
* Keep **all Android types** out of common; only touch them in `actual`.

**iOS actual (`iosMain`)**

* Implement `SpotMapView` using **MapLibre iOS** (`Mapbox`-style GL) or **MapKit** as a fallback.
* Mirror events to `SpotMapEvent`. Use the same style URL pattern for MapTiler where applicable.

---

## 4) Application Architecture (high‑level)

* **Packages:**

  * `com.spott.core.model` — domain models/entities, types, utilities (common)
  * `com.spott.core.network` — Ktor client, JSON, interceptors (common)
  * `com.spott.core.database` — SQLDelight setup (optional in MVP) (common)
  * `com.spott.map` — seam API (common) + platform actuals
  * `com.spott.feature.auth` — auth flows
  * `com.spott.feature.parking` — driver (search, booking, session)
  * `com.spott.feature.host` — host onboarding & hub
  * `com.spott.feature.profile` — profile, vehicles, payments UI bridges
  * `com.spott.feature.notifications` — push + in‑app center (MVP later)

* **Pattern:** Clean Architecture + **MVI** (UiState, Intent, Reducer).

* **Design System:** Material 3 only; Spott tokens centralized.

* **DI:** **Koin 4**; **typed** ViewModel factories for Compose.

---

## 5) Navigation & Routes (drawer‑only)

* **Top‑level drawer destinations** (visibility may depend on auth/role):
  `home.map`, `bookings.list`, `host.hub`, `profile.home`, `legal.help`, `notifications.center`
  *(Do **not** place Search in drawer; it is launched from Home’s floating SearchBar.)*

* **Routes (concise)**

  * **Pre‑Auth:** `splash`, `auth.entry`, `auth.verify`, `auth.reset`
  * **Setup:** `setup.path`, `setup.vehicle`, `setup.payment`
  * **Driver Core:** `home.map`, `home.markerDetails`, `booking.sheet`, `booking.confirm`, `nav.route`, `session.extend`, `session.end`, `bookings.list`, `booking.detail`
  * **Comms & Ratings:** `inbox`, `chat.thread`, `rate.modal`
  * **Profile:** `profile.home`, `profile.payments`, `profile.vehicles`, `profile.security`, `profile.notifications`, `legal.help`
  * **Host:** `host.entry`, `host.wizard.location`, `host.wizard.details`, `host.wizard.availability`, `host.wizard.verify`, `host.created`, `host.hub`, `host.edit`
  * **System:** `notifications.center`, `system.error`, `system.empty`, `system.update`

---

## 6) Mapping Flow — Android Spec (authoritative)

**Intent:** Tap floating **SearchBar** → pick destination (Places) → draggable destination pin / free‑pan + **Search this area** → marker tap opens **Listing Bottom Sheet** → set duration via **ruler slider** → **Book Now** → **Routing Overlay** (Drive → Walk) → **Active Session Banner** persists.

**UI Elements:**

* **FindParkingScreen**:

  * Floating **SearchBar** (opens `SearchDestinationScreen`)
  * **SpotMapView** filling screen
  * **FABs:** My Location, **Search‑This‑Area** pill (debounced appear on camera move)
  * **DurationChip** (default: *Now • 1h*)
  * **TopSpottMiniCard** when present

* **SearchDestinationScreen**:

  * Material **SearchBar** + **Places Autocomplete**
  * Row: **Use current location**

* **Listing Bottom Sheet**:

  * Header: price/hr, Top Spott badge, rating, distance/time
  * Address & entry hint, optional mini gallery
  * **Duration ruler** (15‑min steps) + quick chips (+15m/+30m/+1h)
  * **Book Now** → **Confirm** (one‑tap)

* **Routing Overlay**:

  * Drive polyline (user → **entrance coords**) + ETA badge; **deep link fallback** to external maps
  * Mode chips (Drive/Walk); after “I’ve parked”, switch to **Walking** (entrance → destination)
  * **Active Session Banner** with **Extend / Cancel / Navigate / Walk**

**Key events:** `OnMapReady`, `OnCameraIdle(bounds)`, `OnMarkerClick(id)`, `OnLongPress(latlng)`, `OnMarkerDragEnd(id, latlng)`.

**Acceptance:** Smooth camera fits **user + destination**; destination marker **draggable**; clustered prices at low zoom; top spott distinct; bottom sheet shows **total price** for selected duration; routing uses **entrance** coords; active banner persists app‑wide.

---

## 7) Feature Buckets (MVP focus first)

* **Auth (Firebase)**: Google + Email/Password, email verify, reset.
* **Driver:** Places search, map browse, listing sheet, booking, navigation, active session, bookings list/detail, ratings.
* **Host:** Become Host entry, 4‑step wizard (Location / Details / Availability / Verify & Publish), Host Hub, Edit Listing.
* **Profile & Wallet:** Payments UI bridge (Stripe later), Vehicles, Sessions/Security, Notifications, Legal/Help.
* **System:** Notification Center, Error/Maintenance, Empty/Skeletons, Update prompt.

---

## 8) UI Contracts Template (use before coding any screen)

```kotlin
// UiState
data class XyzState(
  val isLoading: Boolean = false,
  // screen-specific fields …
)

// Intents (one-way)
sealed interface XyzIntent {
  data object OnAppear: XyzIntent
  data class OnClick(val id: String): XyzIntent
  // …
}

// Effects (one-time signals)
sealed interface XyzEffect {
  data class Toast(val message: String): XyzEffect
  data object NavigateUp: XyzEffect
}
```

---

## 9) DoR / DoD (binding)

**Definition of Ready**

* Route ID(s) defined; entry points clear
* UiState, Intents, Effects drafted
* Acceptance Criteria listed
* Strings & icons identified
* Analytics events chosen

**Definition of Done**

* Previews render; TalkBack labels; touch targets ≥48dp
* Loading / Empty / Error / Offline states implemented
* Animations smooth; frame jank <5ms long frames on test device
* Screen events instrumented (analytics)
* No PII in logs; basic tests in place

---

## 10) Naming & Files

* Packages mirror features: `com.spott.feature.map`, `…booking`, `…auth`, `…host`, `…profile`.
* Core composables: `FindParkingScreen`, `SearchDestinationScreen`, `ListingBottomSheet`, `RoutingOverlay`, `ActiveSessionBanner`.
* Map seam in `com.spott.map`.

---

## 11) How Claude Should Plan Work

Always:

1. Output **Next 5 Tasks** (scoped, shippable)
2. Provide **UI contracts** + skeleton composables
3. Provide **navigation wiring** (route IDs & args)
4. Provide **Preview(s)** with fake data
5. List **Acceptance Criteria**

If spanning multiple features, **chunk** into sequential packs of 5 tasks and stop.

---

## 12) Analytics & Performance (must‑haves)

* Instrument key events: `search_started`, `inventory_shown`, `top_spott_viewed`, `booking_initiated`, `payment_succeeded`, `booking_failed`, `session_started`, `session_extended`, `session_ended`.
* Targets: cold start ≤2.5s; search p95 latency ≤500ms (client‑perceived); 60fps map interactions.

---

## 13) Ready‑to‑Start Backlog (Android slice #1)

1. **Implement map seam** in `commonMain` + **Android actual** via **MapLibre Compose** (callbacks → `SpotMapEvent`).
2. **FindParkingScreen shell:** SpotMapView, floating SearchBar, My Location FAB, debounced “Search this area” pill, DurationChip.
3. **SearchDestinationScreen** with Places predictions and “Use current location”.
4. **Listing Bottom Sheet** with price, entry hint, duration ruler, Book Now (stub actions).
5. **Routing Overlay** with fake polyline + Active Session Banner.

> After slice #1: wire Places, inventory fetching, and Directions polyline; then bookings/payment bridges.

---

## 14) Implementation Notes — **MapLibre + MapTiler (Android)**

**Dependencies**

* Add `maplibre-compose` to Android source set.
* Ensure MapLibre GL native transitive is resolved by the compose artifact.

**API key management**

* Put `MAPTILER_API_KEY` in **`gradle.properties`** (project root).
* In **app module `build.gradle.kts`**:

  * `buildFeatures { buildConfig = true }`
  * `buildConfigField("String", "MAPTILER_API_KEY", "\"${providers.gradleProperty("MAPTILER_API_KEY").get()}\"")`
* **Never** hard‑code the key in source.

**Centralized Map config**

```kotlin
package com.spott.map

object MapConfig {
  const val DEFAULT_STYLE_ID = "streets-v2"
  fun mapTilerStyleUrl(styleId: String = DEFAULT_STYLE_ID, key: String = BuildConfig.MAPTILER_API_KEY): String =
    "https://api.maptiler.com/maps/$styleId/style.json?key=$key"
}
```

**Android actual — minimal usage**

```kotlin
@Composable
actual fun SpotMapView(
    props: SpotMapProps,
    onEvent: (SpotMapEvent) -> Unit
) {
  val cameraState = rememberCameraState(
    firstPosition = props.camera.target?.let { CameraPosition(it.lat, it.lng, props.camera.zoom ?: 14f) }
  )

  MaplibreMap(
    baseStyle = BaseStyle.Uri(MapConfig.mapTilerStyleUrl()),
    cameraState = cameraState,
    modifier = Modifier.fillMaxSize()
  )
  // TODO: render markers, polylines, and hook events → SpotMapEvent
  // TODO: mount ornaments (compass, scale, attribution) and ensure attribution is always visible
}
```

**Ornaments & attribution**

* Use MapLibre Compose Material 3 helpers for compass, scale bar, attribution.
* Attribution must be present (legal requirement).

**Error handling**

* If style fails to load, surface a non‑blocking message (snackbar/toast) and allow retry.

**Permissions**

* Ensure `INTERNET` permission; location permissions requested via pre‑education flow.

---

## 15) Risks & Mitigations (frontend)

* **Attribution compliance:** Keep attribution button visible; don’t obscure with overlays.
* **State churn:** Hoist camera/markers states; avoid recreating style URLs every recomposition.
* **Performance on low‑end:** Cluster at low zoom; reuse bitmaps; debounce camera‑idle fetches.
* **Offline/poor networks:** Provide cached view where possible; graceful error messaging.

---

### Appendix — Drawer Content (visibility rules)

* **Visible to all (signed‑in):** Home Map, Bookings, Profile, Help
* **Hosts only:** Host Hub (and contextual Host Wizard deep links)
* **System:** Notifications Center when implemented
* **Hidden:** SearchDestinationScreen (always launched from Home SearchBar)

---

**End of brain.** This file is authoritative for Claude Code responses in this repo. If something conflicts elsewhere, follow this. Update here first, then implement.
