# Bedrock Enhanced — Android Minecraft Bedrock Launcher

**Unofficial, independently branded Android launcher and native-mod project.** This repository is controlled by its Bedrock Enhanced maintainers, including the code and artwork they originally contribute. It **does not** transfer ownership of reused LeviMC or other third-party components.

## Main project: Native launcher v0.3.1

Development is in [launcher/](launcher). It adapts the Apache-2.0 licensed [LeviLaunchroid](https://github.com/LiteLDev/LeviLaunchroid) native-loader foundation and adds Bedrock Enhanced's original launcher UI, package ID `com.bedrockenhanced.launcher`, an explicit Play button, and an app updater targeting GitHub Releases at **lilblock1/Bedrock-Enhanced**.

- [Launcher source and setup](launcher/README.md)
- [Third-party credits](launcher/THIRD_PARTY_NOTICES.md)
- [Native launcher builds](https://github.com/lilblock1/Bedrock-Enhanced/actions/workflows/launcher.yml)
- [Official Bedrock Enhanced releases](https://github.com/lilblock1/Bedrock-Enhanced/releases)

A license-verified Google Play Minecraft installation is required. The launcher does not bundle Minecraft game binaries. The startup flow intentionally uses the inherited runtime version-selection, compatibility rules, Play Store verification, and Minecraft loading activity. It has not yet been confirmed on a physical S25+.

The **gameplay mod menu and real mods are still development work**. Compilation of a native launcher is not proof that Fullbright, Fly, X-ray or other features exist or function on multiplayer servers.

## GitHub updater

The adapted launcher now looks up releases from this repository, **not** the LeviMC release feed, and only accepts `BedrockEnhanced-Launcher-*.apk` assets with the expected Android package identity.

To publish **in-place installable** updates, keep a single private Android signing key across releases. The [launcher build workflow](.github/workflows/launcher.yml) requires these GitHub Actions secrets before publishing an APK from a `vX.Y.Z` tag:

- `BEDROCK_SIGNING_KEYSTORE_BASE64` — Base64-encoded Java keystore
- `BEDROCK_SIGNING_STORE_PASSWORD` — keystore password
- `BEDROCK_SIGNING_KEY_ALIAS` — key alias
- `BEDROCK_SIGNING_KEY_PASSWORD` — key password

**Never put a keystore or passwords in this public repository.** Without these secrets the workflow intentionally refuses to publish a release. GitHub Actions debug APKs are for testing; they may have different signatures and cannot be relied on for updates.

Once a signed versioned APK is published through GitHub Releases, the app can offer downloads of future versions. Android Package Installer still requires user confirmation and enforces signature consistency.

## Legacy app: external utility overlay

The separate original Android overlay remains in [app/](app) and still has its own [build workflow](.github/workflows/android.yml). It does not modify Minecraft's internal engine or provide movement hacks.

## Licensing and attribution

LeviLaunchroid original code © LeviMC contributors, Apache License 2.0. See [THIRD_PARTY_NOTICES.md](launcher/THIRD_PARTY_NOTICES.md) and the full [license](launcher/licenses/LeviLaunchroid-APACHE-2.0.txt), plus the upstream third-party notices. Bedrock Enhanced's original project-specific additions are distinct work. All trademarks belong to their respective owners.

**Minecraft is © Mojang / Microsoft. Not affiliated with or endorsed by Mojang, Microsoft or LeviMC.**
