# Repository Guidelines

## Project Structure & Module Organization
- `composeApp/`: Kotlin Multiplatform app.
  - `src/commonMain/`: Shared Kotlin + `composeResources/`.
  - `src/androidMain/`: Android code + `res/` resources.
  - `src/iosMain/`: iOS shims bridging to Swift UI.
- `iosApp/`: Xcode project and Swift entry points.
- Build system: `gradle/`, `gradlew*`, `build.gradle.kts`, `settings.gradle.kts`.
- Docs/screenshots: `images/` (do not place app assets here).

## Build, Test, and Development Commands
- Build Android debug: `./gradlew assembleDebug` → APK in `composeApp/build/`.
- Install on device/emulator: `./gradlew :composeApp:installDebug`.
- Run Android instrumented tests: `./gradlew connectedAndroidTest`.
- iOS debug (simulator, unsigned):
  `cd iosApp && xcodebuild -scheme iosApp -configuration Debug -destination 'platform=iOS Simulator,OS=latest,name=iPhone 16' CODE_SIGNING_ALLOWED=NO`.
- Clean: `./gradlew clean`.

## Coding Style & Naming Conventions
- Kotlin style: `kotlin.code.style=official` (see `gradle.properties`).
- Indentation: 4 spaces; prefer 100–120 column soft-wrap.
- Packages: `com.spott.*`; mirror `src/*/kotlin` folder structure.
- Filenames: PascalCase for Kotlin; Android XML uses `snake_case.xml`.
- Imports: avoid wildcards (enforced via `.editorconfig`).

## Testing Guidelines
- Android instrumented tests: `composeApp/src/androidInstrumentedTest/`.
- Frameworks: JUnit4 + Jetpack Compose Testing APIs.
- Naming: `*Test.kt` per feature; keep shared logic in `commonMain` with platform shims in `androidMain`/`iosMain`.
- Run on emulator/device with `connectedAndroidTest`.

## Commit & Pull Request Guidelines
- Commits: imperative mood; optional scope prefix (e.g., `android:`, `common:`). Include rationale and link issues (e.g., `#123`).
- PRs must include: clear description, before/after screenshots for UI changes, test coverage or manual steps, and pass CI builds.

## Security & Configuration Tips
- Do not commit secrets. `google-services.json` / `GoogleService-Info.plist` must contain non-sensitive config only.
- Keep local SDK paths in `local.properties` (ignored by Git).
- iOS CI builds are unsigned (`CODE_SIGNING_ALLOWED=NO`); configure signing locally only when needed for device testing.

