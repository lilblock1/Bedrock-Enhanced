#!/usr/bin/env python3
"""Apply Bedrock Enhanced's original UI and attribution to pinned LeviLaunchroid source.

LeviLaunchroid copyright remains with LeviMC contributors (Apache-2.0).
This patch script is original Bedrock Enhanced code.
"""
from __future__ import annotations

import argparse
from pathlib import Path
import re
import shutil
import sys


def replace_once(path: Path, old: str, new: str) -> None:
    content = path.read_text(encoding="utf-8")
    if content.count(old) != 1:
        raise SystemExit(f"Expected exactly one occurrence of {old!r} in {path}")
    path.write_text(content.replace(old, new, 1), encoding="utf-8")


def patch_upstream(project_root: Path, upstream_root: Path) -> None:
    app = upstream_root / "app"
    if not (app / "build.gradle").is_file():
        raise SystemExit("Upstream LeviLaunchroid checkout is missing app/build.gradle")

    replace_once(
        app / "build.gradle",
        'applicationId "org.levimc.launcher"',
        'applicationId "com.bedrockenhanced.launcher"',
    )
    replace_once(
        app / "build.gradle",
        'versionName getGitTag()',
        'versionName "0.3.0-experimental"',
    )
    replace_once(
        app / "src/main/res/values/strings.xml",
        '<string name="app_name">LeviLauncher</string>',
        '<string name="app_name">Bedrock Enhanced Launcher</string>',
    )
    manifest_path = app / "src/main/AndroidManifest.xml"
    manifest = manifest_path.read_text(encoding="utf-8")

    splash_marker = '<activity\n            android:name="org.levimc.launcher.ui.activities.SplashActivity"'
    splash_start = manifest.find(splash_marker)
    if splash_start == -1:
        raise SystemExit("Cannot find upstream SplashActivity manifest entry")
    splash_end = manifest.find("</activity>", splash_start)
    intent_start = manifest.find("<intent-filter>", splash_start, splash_end)
    intent_end = manifest.find("</intent-filter>", intent_start, splash_end)
    if intent_start < 0 or intent_end < 0:
        raise SystemExit("Cannot locate original launcher intent filter")
    intent_end += len("</intent-filter>")
    manifest = manifest[:intent_start] + manifest[intent_end:]

    home_activity = """<activity
            android:name="org.levimc.launcher.ui.activities.BedrockEnhancedHomeActivity"
            android:exported="true"
            android:screenOrientation="sensorLandscape">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
        """
    app_marker = '<application\n'
    if manifest.count(app_marker) != 1:
        raise SystemExit("Cannot find exactly one application in upstream manifest")
    insert_point = manifest.find("<activity\n", manifest.find(app_marker))
    if insert_point < 0:
        raise SystemExit("Cannot find upstream activity insertion point")
    manifest = manifest[:insert_point] + home_activity + manifest[insert_point:]
    # Do not reuse upstream's Google Play Games application ID in our own package.
    manifest = re.sub(
        r'\s*<meta-data\s+android:name="com\.google\.android\.gms\.games\.APP_ID"'
        r'\s+android:value="[^"]*"\s*/>',
        "",
        manifest,
        count=1,
    )
    # Default to a neutral system icon instead of upstream LeviMC branding.
    if 'android:icon="@mipmap/ic_launcher"' not in manifest:
        raise SystemExit("Cannot find upstream app icon declaration")
    manifest = manifest.replace(
        'android:icon="@mipmap/ic_launcher"',
        'android:icon="@android:drawable/ic_menu_manage"',
        1,
    )
    manifest_path.write_text(manifest, encoding="utf-8")

    original_ui = project_root / "launcher/src/BedrockEnhancedHomeActivity.java"
    java_target = (
        app / "src/main/java/org/levimc/launcher/ui/activities/BedrockEnhancedHomeActivity.java"
    )
    java_target.parent.mkdir(parents=True, exist_ok=True)
    shutil.copyfile(original_ui, java_target)
    assets = app / "src/main/assets"
    assets.mkdir(parents=True, exist_ok=True)
    shutil.copyfile(
        project_root / "launcher/THIRD_PARTY_NOTICES.md",
        assets / "BEDROCK_ENHANCED_THIRD_PARTY_NOTICES.txt",
    )
    shutil.copyfile(
        project_root / "launcher/licenses/LeviLaunchroid-APACHE-2.0.txt",
        assets / "LEVI_LAUNCHROID_APACHE_2_LICENSE.txt",
    )
    print("Patched pinned LeviLaunchroid source: original BE home, app ID, credits")
    print("Upstream: LeviMC/LeviLaunchroid copyright retained; no Minecraft assets added")


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("upstream", type=Path)
    args = parser.parse_args()
    patch_upstream(Path(__file__).resolve().parents[1], args.upstream.resolve())
