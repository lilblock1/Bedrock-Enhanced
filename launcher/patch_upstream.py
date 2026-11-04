#!/usr/bin/env python3
"""Apply Bedrock Enhanced's original UI and attribution to pinned LeviLaunchroid source.

LeviLaunchroid copyright remains with LeviMC contributors (Apache-2.0).
This patch script is original Bedrock Enhanced code.
"""
from __future__ import annotations

import argparse
import os
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

    version_name = os.environ.get("BE_VERSION", "0.3.1").lstrip("v")
    if re.fullmatch(r"\d+\.\d+\.\d+", version_name) is None:
        raise SystemExit("BE_VERSION must be major.minor.patch, e.g. 0.3.1")

    replace_once(
        app / "build.gradle",
        'applicationId "org.levimc.launcher"',
        'applicationId "com.bedrockenhanced.launcher"',
    )
    replace_once(
        app / "build.gradle",
        'versionName getGitTag()',
        'versionName "' + version_name + '"',
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

    # Keep the Bedrock Enhanced app's update channel separate from upstream
    # LeviMC releases. Native runtime-signature data remains upstream-managed
    # because that is compatibility data, NOT an app auto-update source.
    main_path = app / "src/main/java/org/levimc/launcher/ui/activities/MainActivity.java"
    settings_path = app / "src/main/java/org/levimc/launcher/ui/activities/SettingsActivity.java"
    upstream_updater_path = app / "src/main/java/org/levimc/launcher/util/GithubReleaseUpdater.java"
    replace_once(
        main_path,
        'new GithubReleaseUpdater(this, "LiteLDev", "LeviLaunchroid", permissionResultLauncher)',
        'new GithubReleaseUpdater(this, "lilblock1", "Bedrock-Enhanced", permissionResultLauncher)',
    )
    replace_once(
        settings_path,
        'new GithubReleaseUpdater(this, "LiteLDev", "LeviLaunchroid", permissionResultLauncher)',
        'new GithubReleaseUpdater(this, "lilblock1", "Bedrock-Enhanced", permissionResultLauncher)',
    )
    settings_content = settings_path.read_text(encoding="utf-8")
    settings_content = settings_content.replace(
        'https://github.com/LiteLDev/LeviLaunchroid',
        'https://github.com/lilblock1/Bedrock-Enhanced',
        1,
    )
    settings_path.write_text(settings_content, encoding="utf-8")

    # The upstream updater picked the first *.apk; our repository also builds
    # the old overlay, so filter strictly for our native launcher artifact.
    updater = upstream_updater_path.read_text(encoding="utf-8")
    old_asset_check = 'if (name.endsWith(APK_ASSET_KEYWORD)) {'
    new_asset_check = ('if (name.startsWith("BedrockEnhanced-Launcher-") '
                       '&& name.endsWith(APK_ASSET_KEYWORD) '
                       '&& !name.toLowerCase(java.util.Locale.ROOT).contains("unsigned")) {')
    if updater.count(old_asset_check) != 2:
        raise SystemExit("Unexpected upstream GitHub updater asset selection")
    updater = updater.replace(old_asset_check, new_asset_check)
    # A repository with no published release returns 404. A 404 must not be
    # mistaken for a parseable GitHub release or silently install another APK.
    response_marker = 'String body = response.body().string();'
    if updater.count(response_marker) != 2:
        raise SystemExit("Unexpected upstream GitHub updater response parsing")
    updater = updater.replace(
        response_marker,
        'if (!response.isSuccessful() || response.body() == null) { '
        'Log.w(TAG, "No Bedrock Enhanced release available (HTTP " + response.code() + ")"); return; }\n'
        '                    ' + response_marker,
    )
    # Check that a downloaded package is actually Bedrock Enhanced before
    # handing it to Android Package Installer, not the unrelated overlay.
    install_marker = 'private void installApk(File apkFile) {'
    if updater.count(install_marker) != 1:
        raise SystemExit("Missing updater install method")
    updater = updater.replace(
        install_marker,
        install_marker + """
        android.content.pm.PackageInfo archive =
                activity.getPackageManager().getPackageArchiveInfo(apkFile.getAbsolutePath(), 0);
        if (archive == null || !"com.bedrockenhanced.launcher".equals(archive.packageName)) {
            activity.runOnUiThread(() ->
                    Toast.makeText(activity,
                            "Update rejected: not a Bedrock Enhanced Launcher APK",
                            Toast.LENGTH_LONG).show());
            apkFile.delete();
            return;
        }
""",
    )
    upstream_updater_path.write_text(updater, encoding="utf-8")

    # A launcher 'Play' request should enter the official Minecraft launching
    # pipeline, not just show the upstream navigation screen. The upstream
    # pipeline still checks version, imported game files, and Play Store license.
    main_content = main_path.read_text(encoding="utf-8")
    ready_marker = """        refreshContentCounts();
    }

    private void handleVersionDependentIntent()"""
    if main_content.count(ready_marker) != 1:
        raise SystemExit("Cannot locate upstream version-manager ready callback")
    main_content = main_content.replace(
        ready_marker,
        """        refreshContentCounts();
        handleBedrockEnhancedLaunchRequest();
    }

    private void handleBedrockEnhancedLaunchRequest() {
        Intent request = getIntent();
        if (request == null || !request.getBooleanExtra("BE_AUTO_LAUNCH", false)) return;
        // Consume BEFORE launching to prevent accidental repeated launches.
        request.removeExtra("BE_AUTO_LAUNCH");
        setIntent(request);
        if (versionManager == null || versionManager.getSelectedVersion() == null) {
            new CustomAlertDialog(this)
                    .setTitleText("Minecraft version required")
                    .setMessage("Select your licensed Minecraft installation or import an official APK/APKS. The launcher cannot start an uninstalled version.")
                    .setPositiveButton("Import APK / APKS", v -> startApkFilePicker())
                    .setNegativeButton("Choose version", v -> showVersionSelectDialog())
                    .show();
            return;
        }
        if (binding != null) binding.getRoot().post(this::launchGame);
    }

    private void handleVersionDependentIntent()""",
        1,
    )
    main_path.write_text(main_content, encoding="utf-8")

    # Our home screen must pass the deliberate launch intent to MainActivity.
    home_target = app / "src/main/java/org/levimc/launcher/ui/activities/BedrockEnhancedHomeActivity.java"
    home = home_target.read_text(encoding="utf-8")
    home = home.replace('button("Launch mod-capable Minecraft", this::openRuntime);',
                        'button("Play modded Minecraft", () -> openRuntime(true));')
    home = home.replace('button("Minecraft instances and native mods", this::openRuntime);',
                        'button("Manage versions and native mods", () -> openRuntime(false));')
    home = home.replace('private void openRuntime() {', 'private void openRuntime(boolean play) {')
    home = home.replace(
        'intent.setClassName(getPackageName(), "org.levimc.launcher.ui.activities.SplashActivity");',
        'intent.setClassName(getPackageName(), "org.levimc.launcher.ui.activities.MainActivity");\n'
        '            if (play) intent.putExtra("BE_AUTO_LAUNCH", true);',
    )
    if 'BE_AUTO_LAUNCH' not in home or 'this::openRuntime' in home:
        raise SystemExit("Home launcher direct-play patch failed")
    home_target.write_text(home, encoding="utf-8")

    # Own product branding, without deleting upstream copyright/credits.
    strings_path = app / "src/main/res/values/strings.xml"
    strings = strings_path.read_text(encoding="utf-8")
    substitutions = {
        'eula_title': 'Bedrock Enhanced Launcher License Agreement',
        'about_subtitle': 'Bedrock Enhanced — Android Minecraft Bedrock Launcher',
        'about_github_repo': 'GitHub · Bedrock Enhanced',
        'about_contribute_desc': 'Contributions to Bedrock Enhanced are welcome via our GitHub repository.',
        'changelog_version': 'Bedrock Enhanced %1$s',
        'news_subtitle': 'Updates and announcements from Bedrock Enhanced.',
        'news_notification_channel_description': 'Bedrock Enhanced announcements',
    }
    for key, value in substitutions.items():
        pat = re.compile(r'(<string name="' + re.escape(key) + r'">)(.*?)(</string>)')
        strings, count = pat.subn(lambda m: m.group(1) + value + m.group(3), strings, count=1)
        if count != 1:
            raise SystemExit("Missing upstream string resource: " + key)
    strings = strings.replace('Welcome to LeviLauncher', 'Welcome to Bedrock Enhanced Launcher')
    # Preserve the LeviMC 'copyright', authors, and original notices; we
    # cannot pretend their work was authored by the new project.
    strings_path.write_text(strings, encoding="utf-8")

    print("Patched pinned LeviLaunchroid source: original BE home, app ID, credits")
    print("Upstream: LeviMC/LeviLaunchroid copyright retained; no Minecraft assets added")


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("upstream", type=Path)
    args = parser.parse_args()
    patch_upstream(Path(__file__).resolve().parents[1], args.upstream.resolve())
