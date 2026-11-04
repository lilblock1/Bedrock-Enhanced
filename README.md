# Bedrock Enhanced — Android Overlay v0.1.1

Standalone Android overlay companion for Minecraft Bedrock, targeted at Android 8+ and Samsung Galaxy S25+. This is not a modified Minecraft APK and does not inject code into Minecraft.

## Working foundation (source implementation)
- Overlay permission request, foreground notification and stop control
- Draggable BE floating bubble and expandable in-game panel
- Optional, adjustable, touch-pass-through crosshair (size and colors)
- Own server / Realms UI profile selector and saved preferences
- Battery, display refresh rate and system thermal-status diagnostics
- Launch Minecraft button

## Not yet implemented
Fly, speed, jump, no-fall, fullbright, freecam, ore highlighting, zoom, game FPS, time/weather control, waypoints, auto-tool, particles, fog, compact HUD, performance textures and performance profiles require compatible game-side resources, supported commands or authorized integrations. The menu keeps these items disabled rather than falsely claiming they work. The default planned speed/jump multipliers are 1.0x.

## Build
Push to main or run the `Build Bedrock Enhanced APK` GitHub Actions workflow. Open the successful run and download the `BedrockEnhanced-debug-apk` artifact. It contains the Android debug APK, signed by GitHub's temporary debug key; changing runners may change the signing key and require uninstall/reinstall of this companion app.

## Test
1. Install the debug APK and allow Display over other apps.
2. Start overlay and launch Minecraft.
3. Drag the floating BE button and tap it to open the panel.
4. Enable crosshair; adjust its size and color.
5. Verify Minecraft remains touch-responsive.
6. Stop overlay from companion app.

No physical-device test has yet been performed. This project contains no copyrighted Minecraft code or game files.
