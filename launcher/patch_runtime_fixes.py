#!/usr/bin/env python3
"""Bedrock Enhanced-specific Android runtime fixes on pinned Apache-2.0 LeviLaunchroid.

Original patch Copyright 2026 Bedrock Enhanced contributors.
LeviLaunchroid code remains Copyright its original contributors.
"""
from pathlib import Path
import sys


def replace_once(text: str, old: str, new: str, label: str) -> str:
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"{label}: expected one source anchor, found {count}")
    return text.replace(old, new, 1)


def patch_runtime(upstream: Path) -> None:
    path = upstream / "app/src/main/java/org/levimc/launcher/core/minecraft/GamePackageManager.kt"
    source = path.read_text(encoding="utf-8")

    # Minecraft's own installed split APKs do not always ship libc++_shared.so:
    # Gradle bundles it with our launcher. Install an independent read-only
    # copy in the runtime ABI cache before verifying required game libraries.
    source = replace_once(
        source,
        "        verifyLibraries(outputDir)",
        "        restoreLauncherCppRuntime(outputDir)\n        verifyLibraries(outputDir)",
        "extractLibrary fallback",
    )
    helper = '''
    /**
     * Use the already-bundled ARM64 C++ runtime if Minecraft APK/splits omit it.
     * This is a launcher library, not a copied proprietary Minecraft game file.
     * No external download, root, or world-file modification is involved.
     */
    private fun restoreLauncherCppRuntime(outputDir: File) {
        val destination = File(outputDir, "libc++_shared.so")
        if (destination.isFile && destination.length() > 0L) return

        val bundledDir = context.applicationInfo.nativeLibraryDir
        if (bundledDir.isNullOrBlank()) {
            report("Bundled C++ library directory unavailable")
            return
        }
        val bundled = File(bundledDir, "libc++_shared.so")
        if (!bundled.isFile || bundled.length() <= 0L) {
            report("Bundled libc++_shared.so missing at ${bundled.absolutePath}")
            return
        }
        try {
            bundled.inputStream().use { input ->
                copyStreamToReadOnlyFile(input, destination)
            }
            report("Restored libc++_shared.so from Bedrock Enhanced ARM64 runtime")
        } catch (e: Exception) {
            destination.delete()
            Log.w(TAG, "Unable to restore launcher C++ runtime", e)
            report("Launcher C++ runtime restoration failed: ${e.message}")
        }
    }

'''
    source = replace_once(
        source,
        "    private fun collectApkFiles(): List<File> {",
        helper + "    private fun collectApkFiles(): List<File> {",
        "C++ helper",
    )

    # In some Android packaging configurations libraries are memory mapped
    # directly from our APK rather than appearing under nativeLibraryDir.
    # The app's own class loader can still load a properly packaged .so.
    marker = '        val resolvedPath = resolveLibraryPath(name)'
    fallback = '''
        if (normalizedName == "c++_shared" &&
            (resolveLibraryPath(name) == null)) {
            try {
                System.loadLibrary("c++_shared")
                report("Using bundled launcher libc++_shared.so")
                return LibraryLoadResult(
                    normalizedName, fileName, "launcher packaged native library",
                    true, elapsedSince(startedAt)
                )
            } catch (e: UnsatisfiedLinkError) {
                report("Bundled libc++_shared.so load failed: ${e.message}")
            }
        }

'''
    source = replace_once(source, marker, fallback + marker, "C++ load fallback")
    path.write_text(source, encoding="utf-8")
    print("Applied C++ runtime cache recovery and APK-native load fallback")


if __name__ == "__main__":
    if len(sys.argv) != 2:
        raise SystemExit("Usage: patch_runtime_fixes.py <upstream-root>")
    patch_runtime(Path(sys.argv[1]).resolve())
