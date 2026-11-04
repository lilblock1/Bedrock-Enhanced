# Bedrock Enhanced Launcher — v0.3.3 experimental

This is a **separate Android launcher** project with an original Bedrock Enhanced entry screen and a native Minecraft loading foundation **derived from LeviLaunchroid** (Copyright LeviMC contributors, Apache 2.0). This is neither the old overlay app nor a claim that the underlying loader was written from scratch.

## Functionality being built

- Dedicated Android package: `com.bedrockenhanced.launcher`
- Original Bedrock Enhanced entry screen with installed official Minecraft version detection
- Launches the retained, capable upstream Minecraft instance / native mod manager
- Builds upstream launcher against a specific source revision, with credits and an Apache 2.0 license copy
- Preserves original Minecraft installation; no proprietary game binaries bundled
- Native ARM64 mod infrastructure from upstream; mod authoring happens separately

## Current implementation status

| Capability | Status |
| --- | --- |
| Custom entry UI and official game-detection code | Implemented in source |
| Upstream derived native mod / instance runtime | Included through pinned source checkout; build and runtime verification pending |
| Bedrock Enhanced native mod modules | Not yet implemented |
| Client-side fullbright, FOV, freecam, X-ray, player motion | Not yet implemented |
| Running on Galaxy S25+ with Bedrock | Device test pending |
| Multiplayer friend-server compatibility | Not guaranteed; depends on server rules |

## Reproduction

The launcher build is defined by `.github/workflows/launcher.yml`. It checks out:

`LiteLDev/LeviLaunchroid@cc5c7bc6eac48f1e82adf705b8b5f63ba0883247` and recursive submodules.

Then `launcher/patch_upstream.py` applies a controlled set of changes and Gradle compiles an ARM64 Android debug APK. GitHub Actions publishes an APK artifact only when compilation succeeds.

The base upstream build is complex (JDK21, Android SDK 36, NDK r28c, CMake, xmake). A complete build may fail due to missing upstream integrations; a green CI check does not certify that logging in and loading Minecraft works on your phone.

## Acknowledgement and licenses

- [LeviLaunchroid upstream](https://github.com/LiteLDev/LeviLaunchroid) — Copyright LeviMC contributors, Apache 2.0
- See [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md) and [upstream license copy](licenses/LeviLaunchroid-APACHE-2.0.txt).
- Dependencies including `preloader-android` and `microsoft/libHttpClient` carry separate licenses; preserve their notices and verify all licensing before publishing releases.
- This project is **unofficial** and is not endorsed by LeviMC, Mojang or Microsoft.

## Minecraft and servers

A licensed Minecraft Bedrock installation from Google Play is required. No copyrighted Minecraft client binary is redistributed. Native mods still cannot guarantee movement hacks on servers with authoritative movement or other enforcement; permission from a friend does not automatically bypass server checks.

## Existing overlay project

The original Bedrock Enhanced external overlay under root `app/` is retained and has its own `android.yml` workflow. The new derived launcher is built separately as a second APK with a different package name.

## v0.3.3 startup and update changes

- Launcher "Play modded Minecraft" now opens the integrated runtime and sends a one-shot auto-launch request. It follows the upstream installed-version manager, selected instance, game-file preparation, and Google Play ownership checks.
- If the game version is unavailable, the runtime offers **Import APK / APKS** or **Choose version** instead of a silent no-op.
- Launcher app updates check GitHub Releases at **lilblock1/Bedrock-Enhanced** rather than LeviMC's release feed. Only an asset named `BedrockEnhanced-Launcher-*.apk` will be considered, and its embedded package identity must match `com.bedrockenhanced.launcher`.
- The native runtime's preloader signature-rule updates are **not** the app auto-update channel; upstream compatibility rule data remains managed upstream until we implement and validate our own mirror.
- GitHub CI debug builds are not stable signed releases. Android will reject app updates signed with different keys. For installable in-place release updates, configure a protected signing keystore and the release workflow described in the source. Do not publish the private key.
- This is a source/build pipeline milestone; launching the Minecraft runtime on a physical Android device still requires a device test. No gameplay hacks are implemented yet.

## v0.3.3 — Missing C++ runtime and launcher controls

- **Native start fix:** On some licensed Google Play Minecraft versions, the extracted game ABI directory has no `libc++_shared.so`. The launcher now copies its own bundled ARM64 C++ runtime into the safe, read-only game library cache before verifying libraries. If Android stores native libraries within the APK instead of on disk, the loader also attempts `System.loadLibrary("c++_shared")`.
- **Recovery:** The Bedrock Enhanced home screen has a confirmed *Repair Minecraft native library cache* button. It clears only regenerable native-library files for the installed Minecraft profile. It does **not** erase worlds, accounts, mods, saves, resource packs or original Minecraft files.
- **Home actions:** Play and Manage use the adapted native launcher; refresh installation status displays confirmation; Minecraft launch failures show a message; project credits, upstream credits, and update releases have separate links.
- **CI verification:** The APK must contain an ARM64 `libc++_shared.so`, preloader, gxcore and the license notice.

**Verification:** Successful compilation and presence of libraries in the APK do not prove Minecraft reaches its main menu. Test on an Android device. If the next startup error references another library, provide the complete launch log. The launcher does not ship completed gameplay mods yet.

## v0.3.3 — Native library audit and multi-file diagnosis

The launcher embeds two ARM64 dependencies which some Minecraft APK/split combinations omit from their native library cache: `libc++_shared.so` and `libHttpClient.Android.so`. Both are bundled with the compiled launcher and are now restored into the cache when needed, with a fallback via Android's package native library loader.

The extraction path now checks **all** installed Minecraft split APK filenames, not only paths containing ABI tokens. For imported game versions, it stops mixing ARM32 and ARM64 native binaries in a single runtime cache. A preflight reports the complete list of missing **Minecraft-supplied** native libraries (such as `libminecraftpe.so`, `libfmod.so`, `libMediaDecoders_Android.so`, or the version-dependent `libMallocRedirect.so`) and asks for a legitimate matching ARM64 split installation or safe cache repair. We do not download or distribute proprietary Minecraft libraries.

The dedicated launcher CI validates the APK package, bundled ARM64 dependencies, embedded license notices and native-library ELF architecture. **CI is not a device test**: after installing, Minecraft must still be launched on an S25+ to confirm actual compatibility with its installed version. Any new error should include the full launch log.


## v0.3.4 — Fix game APK split detection

Minecraft's own `libminecraftpe.so`, `libfmod.so`, and `libMediaDecoders_Android.so` must come from the licensed Minecraft installation. The inherited native loader previously consulted `splitPublicSourceDirs` for installed packages, which can omit the ARM64 splits. The launcher now checks both `splitSourceDirs` and `splitPublicSourceDirs` from the package context and Android PackageManager, plus the base APK, de-duplicates paths, and checks read access. Missing or unreadable game APKs are reported explicitly; all native extraction failures are logged.

The Bedrock Enhanced home screen now includes **Diagnose Minecraft game files**, which scans the installed Minecraft base and split APKs plus Android's native lib directory for the three key game libraries, then offers a copyable report. The **Repair Minecraft native library cache** control remains limited to regenerable files. No Minecraft game files are redistributed, and ownership checks remain intact.

**Runtime launch not yet validated on a physical phone.** A green CI build confirms compilation, not successful Minecraft startup.
