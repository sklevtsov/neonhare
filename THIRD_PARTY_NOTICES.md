# Third-party notices

The project MIT licence does not replace dependency or tool licences. This file inventories the current build; check upstream terms when changing dependencies or distributing additional components.

| Component | Use | Upstream / terms |
|---|---|---|
| Gradle wrapper and Gradle | Build bootstrap/tool | [Gradle](https://github.com/gradle/gradle), Apache-2.0; wrapper scripts retain their notices |
| Android Gradle Plugin | Build only | [Android tools](https://android.googlesource.com/platform/tools/base/), Apache-2.0 |
| google-java-format | Developer formatter only | [google-java-format](https://github.com/google/google-java-format), Apache-2.0; transitive notices in its all-deps distribution |
| Android SDK/build-tools | External developer installation | [Android SDK terms](https://developer.android.com/studio/terms); not redistributed in this repository |
| GitHub Actions used by workflows | CI only | Respective pinned upstream actions; see `.github/workflows/` |

The app currently declares no external runtime library dependency. Android platform APIs are supplied by the device. Build tooling is not an in-game dependency.

Artworks have separate [asset terms](ASSETS-LICENSE.md). Current game artwork and the promotional banner were generated and prepared for this project; provenance notes record the generation prompts and subsequent processing. External game copies and reference screenshots are excluded from Git and from the APK. Story research links do not grant rights to the referenced material.
