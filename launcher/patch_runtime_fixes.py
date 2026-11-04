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
        "        restoreBundledDependencies(outputDir)\n        verifyLibraries(outputDir)\n        verifyGameNativeDependencies(outputDir)",
        "extractLibrary fallback",
    )
    helper = '''
    /**
     * Minecraft's installed split APKs may omit native dependencies which
     * the launcher already bundles. Restore ONLY our own native libraries.
     * Never substitute a launcher .so for a proprietary Minecraft binary.
     */
    private fun restoreBundledDependencies(outputDir: File) {
        val nativeDir = context.applicationInfo.nativeLibraryDir
        val bundledLibs = listOf("libc++_shared.so", "libHttpClient.Android.so")
        for (library in bundledLibs) {
            val target = File(outputDir, library)
            if (target.isFile && target.length() > 0L) continue
            if (nativeDir.isNullOrBlank()) {
                report("Bundled native directory unavailable for $library; trying Android class loader")
                continue
            }
            val source = File(nativeDir, library)
            if (!source.isFile || source.length() <= 0L) {
                report("No extracted $library in launcher; Android may load it directly from APK")
                continue
            }
            try {
                source.inputStream().use { input -> copyStreamToReadOnlyFile(input, target) }
                report("Restored $library from launcher native runtime")
            } catch (error: Exception) {
                target.delete()
                Log.w(TAG, "Could not restore launcher $library", error)
                report("Restore failed for $library: ${error.message}")
            }
        }
    }

    /**
     * Preflight reports ALL genuinely absent game libraries together. The
     * bundled C++ and HTTP libraries can also resolve using Android's native
     * class loader and therefore are not counted as missing game assets.
     */
    private fun verifyGameNativeDependencies(outputDir: File) {
        val providedByLauncher = setOf("libc++_shared.so", "libHttpClient.Android.so")
        val missing = getCacheRequiredLibs().filter { library ->
            library !in providedByLauncher && File(outputDir, library).let {
                !it.isFile || it.length() <= 0L
            }
        }
        if (missing.isNotEmpty()) {
            val names = missing.joinToString(", ")
            report("Preflight: missing Minecraft game libraries: $names")
            throw IllegalStateException(
                "Missing Minecraft game libraries: $names. " +
                "Check that the installed Minecraft version includes all matching " +
                "ARM64 split APKs. In Bedrock Enhanced, use Repair Minecraft " +
                "native library cache, then retry. Worlds are unaffected."
            )
        }
        report("Native game-library preflight passed")
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
        // Both dependencies ship with Bedrock Enhanced. When the installed
        // Minecraft split APKs do not contain them and the extracted cache is
        // empty, try the Android class loader before reporting an error.
        if ((normalizedName == "c++_shared" ||
                normalizedName == "HttpClient.Android") &&
            resolveLibraryPath(name) == null) {
            try {
                System.loadLibrary(normalizedName)
                report("Loaded bundled $fileName using Android class loader")
                return LibraryLoadResult(
                    normalizedName, fileName, "launcher packaged native dependency",
                    true, elapsedSince(startedAt)
                )
            } catch (error: UnsatisfiedLinkError) {
                report("Launcher packaged $fileName could not load: ${error.message}")
            }
        }

'''
    source = replace_once(source, marker, fallback + marker, "C++ load fallback")

    # Scanning only matching-ABI files prevents an ARM32 library from being
    # silently stored under arm64 and then failing as "wrong ELF class".
    source = replace_once(
        source,
        '''            if (cacheRequiredLibs.any { !File(outputDir, it).exists() }) {
                Log.w(TAG, "Primary ABI ${getDeviceAbi()} libraries missing, trying fallback ABIs")
                report("Primary ABI ${getDeviceAbi()} libraries missing, trying fallback ABIs")
                val fallbackAbis = listOf("arm64-v8a", "armeabi-v7a", "x86_64", "x86")
                fallbackAbis.filter { it != getDeviceAbi() }.forEach { abi ->
                    apkPaths.forEach { extractFromApk(it, outputDir, abi) }
                }
            }''',
        '''            // Never mix incompatible ELF architectures in an arm64 cache.
            // Missing libs are reported by the unified preflight below.''',
        "ABI-safe custom version extraction",
    )
    source = replace_once(
        source,
        '''        if (!apkPath.contains("arm") && !apkPath.contains("x86") && !apkPath.contains("base.apk")) {
            return
        }

''',
        "",
        "scan all installed split APK names",
    )
    path.write_text(source, encoding="utf-8")
    print("Applied launcher native dependency recovery and missing-game-library preflight")


if __name__ == "__main__":
    if len(sys.argv) != 2:
        raise SystemExit("Usage: patch_runtime_fixes.py <upstream-root>")
    patch_runtime(Path(sys.argv[1]).resolve())
