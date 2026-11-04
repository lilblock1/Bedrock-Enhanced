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
        button("Refresh Minecraft license detection", this::updateStatus);
        button("Open official Minecraft app", () -> {
            Intent installed = getPackageManager().getLaunchIntentForPackage("com.mojang.minecraftpe");
            if (installed == null) {
                Toast.makeText(this, "Install a licensed Minecraft copy from Google Play first.", Toast.LENGTH_LONG).show();
                openMinecraftStore();
            } else startActivity(installed);
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

        button("Open source and third-party credits", () ->
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
