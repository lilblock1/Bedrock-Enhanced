# Bedrock Enhanced v0.2.0 — external Android overlay

**Standalone Android companion, without any server add-on or modified Minecraft APK.**

This version has been developed for the Samsung Galaxy S25+ (Android 16) with an external floating menu. It can appear over Minecraft regardless of which server you join, subject to Android overlay permissions and full-screen overlay restrictions.

## Available now in the APK

- Draggable bubble and compact tabbed overlay UI
- Crosshair overlay: enable/disable, cross/dot/ring styles, adjustable size and three colors
- Saved on-device crosshair and menu settings
- Offline manual waypoints: name, XYZ, save, delete and copy teleport command (does not read the game location)
- Operator-command clipboard tool: night vision, slow falling, creative/survival/spectator gamemode, coordinate rule, day/night, rain/clear, speed/jump potion effects
- Speed and jump default to **1.0x** (no command until you choose a potion effect level)
- Android diagnostics: battery percentage, display refresh rate, Android thermal status; **not actual Minecraft FPS**
- Profile selector for friends' servers versus Realms
- Minecraft launcher, foreground notification and stop control

## What it does NOT do

This APK cannot automatically modify Minecraft's player movement, world state, renderer, inventory or camera, or bypass a server's rules. Flight, true freecam, no-fall, X-ray/ore highlighting, native fullbright, FOV zoom, game FPS and rendering performance modifications require game-side integration and are **not** implemented. No buttons pretending to toggle those effects are active.

Clipboard command shortcuts require the Minecraft player to have the necessary operator permissions. They are **not** dispatched by the app. Do not rely on operator commands being accepted by Realms or servers unless the owner grants that permission. Creative and spectator gamemodes change how you play and can affect achievements or world behavior.

## Install / test

1. Download the APK from the latest successful GitHub Actions run under artifact `BedrockEnhanced-debug-apk`.
2. Install this **separate companion app**.
3. Grant *Display over other apps* when requested.
4. Start the overlay, then launch Minecraft.
5. Tap the BE bubble; use Visuals / Commands / Waypoints / Device tabs.
6. Open Minecraft chat and manually paste copied commands on servers where you are an operator.

Builds are debug-signed and may not update each other if GitHub CI rotates keys. Do not uninstall the companion app without exporting or recording your manual waypoints. Minecraft worlds are not modified.

## Build

Android SDK 35 / Java 17 / Gradle 8.9. GitHub Actions builds `gradle :app:assembleDebug` on push. The APK is uploaded as `BedrockEnhanced-debug-apk`.

The source compiles in GitHub CI; a physical Samsung device runtime test is still needed.
