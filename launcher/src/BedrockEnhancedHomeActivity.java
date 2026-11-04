/*
 * Bedrock Enhanced original launcher entry UI.
 * Copyright (c) 2026 Bedrock Enhanced contributors.
 *
 * Uses the LeviLaunchroid runtime (Copyright LeviMC contributors,
 * Apache License 2.0). See THIRD_PARTY_NOTICES.md and bundled notices.
 * This file is original project UI code, not copied from LeviLaunchroid.
 */
package org.levimc.launcher.ui.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.io.File;

public final class BedrockEnhancedHomeActivity extends Activity {
    private final int background = Color.rgb(14, 20, 29);
    private final int panel = Color.rgb(24, 35, 48);
    private final int accent = Color.rgb(36, 181, 140);
    private LinearLayout container;
    private TextView gameStatus;

    private int px(float dp) {
        return (int) (dp * getResources().getDisplayMetrics().density + .5f);
    }

    private GradientDrawable round(int color) {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(color);
        shape.setCornerRadius(px(14));
        return shape;
    }

    private TextView text(String value, float size, int color, boolean bold) {
        TextView label = new TextView(this);
        label.setText(value);
        label.setTextSize(size);
        label.setTextColor(color);
        if (bold) label.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        label.setPadding(0, px(4), 0, px(10));
        return label;
    }

    private void button(String title, Runnable action) {
        Button b = new Button(this);
        b.setAllCaps(false);
        b.setText(title);
        b.setTextColor(Color.WHITE);
        b.setTextSize(15);
        b.setBackground(round(accent));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, px(53));
        lp.bottomMargin = px(10);
        container.addView(b, lp);
        b.setOnClickListener(v -> action.run());
    }

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(background);
        container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(px(24), px(26), px(24), px(24));
        scroll.addView(container);
        setContentView(scroll);

        TextView mark = text("BE  /  BEDROCK ENHANCED", 27, accent, true);
        container.addView(mark);
        container.addView(text("Independent Android launcher • Experimental build", 14, Color.LTGRAY, false));

        LinearLayout statusCard = new LinearLayout(this);
        statusCard.setOrientation(LinearLayout.VERTICAL);
        statusCard.setPadding(px(17), px(15), px(17), px(12));
        statusCard.setBackground(round(panel));
        gameStatus = text("", 15, Color.WHITE, false);
        statusCard.addView(gameStatus);
        statusCard.addView(text("Android " + Build.VERSION.RELEASE
            + "  •  " + (Build.SUPPORTED_ABIS.length > 0 ? Build.SUPPORTED_ABIS[0] : "unknown ABI"),
            13, Color.LTGRAY, false));
        LinearLayout.LayoutParams cardLayout = new LinearLayout.LayoutParams(-1, -2);
        cardLayout.setMargins(0, px(12), 0, px(20));
        container.addView(statusCard, cardLayout);

        button("Launch mod-capable Minecraft", this::openRuntime);
        button("Minecraft instances and native mods", this::openRuntime);
        button("Refresh Minecraft installation status", () -> {
            updateStatus();
            Toast.makeText(this, gameStatus.getText(), Toast.LENGTH_SHORT).show();
        });
        button("Repair Minecraft native library cache", this::confirmRuntimeCacheRepair);
        button("Check Bedrock Enhanced updates", () ->
            openSite("https://github.com/lilblock1/Bedrock-Enhanced/releases"));
        button("Open official Minecraft app", () -> {
            Intent installed = getPackageManager().getLaunchIntentForPackage("com.mojang.minecraftpe");
            if (installed == null) {
                Toast.makeText(this, "Install a licensed Minecraft copy from Google Play first.", Toast.LENGTH_LONG).show();
                openMinecraftStore();
            } else {
                try {
                    startActivity(installed);
                } catch (RuntimeException exception) {
                    Toast.makeText(this, "Unable to open Minecraft: " + exception.getMessage(),
                        Toast.LENGTH_LONG).show();
                }
            }
        });

        container.addView(text("Native runtime", 19, Color.WHITE, true));
        container.addView(text(
            "The mod-capable instance manager and .so native-mod loading foundation are adapted from LeviLaunchroid. "
            + "Use Launch to open the integrated runtime. Bedrock Enhanced gameplay mods are not shipped yet.",
            14, Color.LTGRAY, false));

        container.addView(text("Permissions and multiplayer", 19, Color.WHITE, true));
        container.addView(text(
            "Only use mods on servers where they are allowed. Client-side visual changes may work, "
            + "but servers can still enforce movement, damage and inventory rules.",
            14, Color.LTGRAY, false));

        button("Bedrock Enhanced project and credits", () ->
            openSite("https://github.com/lilblock1/Bedrock-Enhanced/blob/main/launcher/THIRD_PARTY_NOTICES.md"));
        button("LeviMC / original launcher credits", () ->
            openSite("https://github.com/LiteLDev/LeviLaunchroid"));
        container.addView(text(
            "Not affiliated with Mojang, Microsoft or LeviMC. "
            + "LeviLaunchroid copyright retained and Apache-2.0 attribution included. "
            + "See bundled THIRD_PARTY_NOTICES and original LICENSE.",
            12, Color.GRAY, false));

        updateStatus();
    }

    private void updateStatus() {
        boolean found = false;
        String version = "unknown";
        try {
            PackageInfo info = getPackageManager().getPackageInfo("com.mojang.minecraftpe", 0);
            found = true;
            version = info.versionName != null ? info.versionName : "installed";
        } catch (PackageManager.NameNotFoundException ignored) {}
        if (gameStatus != null) gameStatus.setText(found
            ? "Official Minecraft: detected (" + version + ")"
            : "Official Minecraft: not detected");
    }

    private void openRuntime() {
        try {
            Intent intent = new Intent();
            intent.setClassName(getPackageName(), "org.levimc.launcher.ui.activities.SplashActivity");
            startActivity(intent);
        } catch (Exception error) {
            Toast.makeText(this, "Cannot open game runtime: " + error.getClass().getSimpleName(),
                Toast.LENGTH_LONG).show();
        }
    }

    // Only clears regenerated extracted game libraries, never Minecraft worlds,
    // packs, account data, mod files, or the official Minecraft installation.
    private void confirmRuntimeCacheRepair() {
        new AlertDialog.Builder(this)
            .setTitle("Repair Minecraft startup libraries")
            .setMessage("Clear the cached extracted native libraries? The launcher will recreate them on next Play. Worlds and game data are not deleted.")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Repair cache", (dialog, which) -> repairNativeCache())
            .show();
    }

    private void repairNativeCache() {
        File cacheDir = new File(getFilesDir(), "minecraft_libs/com.mojang.minecraftpe/arm64");
        File[] entries = cacheDir.listFiles();
        if (entries == null) {
            Toast.makeText(this, "No extracted library cache exists yet.", Toast.LENGTH_LONG).show();
            return;
        }
        int cleared = 0;
        int failed = 0;
        for (File entry : entries) {
            if (entry.isFile() && (entry.getName().endsWith(".so")
                    || entry.getName().startsWith(".extraction_")
                    || entry.getName().endsWith(".ok"))) {
                if (entry.delete()) cleared++; else failed++;
            }
        }
        Toast.makeText(this, "Library cache cleared: " + cleared
            + (failed > 0 ? ", failed: " + failed : "")
            + ". Tap Play to rebuild.", Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateStatus();
    }

    private void openMinecraftStore() {
        openSite("https://play.google.com/store/apps/details?id=com.mojang.minecraftpe");
    }

    private void openSite(String url) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (ActivityNotFoundException ex) {
            Toast.makeText(this, "No web browser available", Toast.LENGTH_SHORT).show();
        }
    }
}
