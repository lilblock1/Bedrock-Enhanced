# Third-party notices — Bedrock Enhanced Launcher

Bedrock Enhanced Launcher is an independent project and is **not** affiliated with, endorsed by, or maintained by LeviMC, Mojang Studios, or Microsoft.

## Core reused technology

**LeviLaunchroid / LeviLauncher (Android)**

- Upstream source: https://github.com/LiteLDev/LeviLaunchroid
- Copyright: © LeviMC contributors and other holders identified in the upstream source.
- License: Apache License 2.0 (a complete copy is provided in `launcher/licenses/LeviLaunchroid-APACHE-2.0.txt` and is bundled in the adapted APK).
- Bedrock Enhanced retrieves a *pinned* upstream revision, preserves upstream code and notices, and applies original project changes in `launcher/patch_upstream.py`. The app's original UI is `launcher/src/BedrockEnhancedHomeActivity.java`.
- Modified upstream files are `app/build.gradle`, `app/src/main/AndroidManifest.xml`, and `app/src/main/res/values/strings.xml`; the modified distribution includes a new Java class and third-party license assets.
- No claim of authorship is made over LeviMC's launcher or runtime code.

## Upstream dependencies

Upstream LeviLaunchroid uses third-party native and Java/Kotlin dependencies, including:
- LiteLDev/preloader-android (own license and copyright)
- microsoft/libHttpClient (own license and copyright)
- Other components and transitive dependencies recorded in upstream Gradle, CMake and source trees

Each retained upstream component is used subject to its own license. Check upstream copyright, LICENSE and NOTICE files before distributing a release APK; this experimental CI APK is provided for developer evaluation. Preserve all required third-party notices. 

## Minecraft

Minecraft Bedrock game binaries are *not included*. Users must install and own an official Google Play copy. This launcher is an independent unofficial adaptation. No Minecraft assets, private keys, or proprietary game code are committed in this project.

## Runtime caveats

- This project does **not** yet ship working gameplay hacks, special privileges, or guaranteed multiplayer compatibility.
- The generated debug build uses placeholder Firebase configuration only for compilation; Firebase-specific features, account integrations and game runtime must be tested before release.
- Server-authoritative mechanics may reject client-side gameplay modifications.
