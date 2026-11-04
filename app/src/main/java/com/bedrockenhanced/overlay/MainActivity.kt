package com.bedrockenhanced.overlay

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    private lateinit var permissionStatus: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 48, 32, 24)
            setBackgroundColor(Color.rgb(20, 25, 33))
        }
        fun label(text: String, size: Float = 16f): TextView = TextView(this).apply {
            this.text = text
            textSize = size
            setTextColor(Color.WHITE)
            setPadding(0, 10, 0, 16)
        }
        root.addView(label("BEDROCK ENHANCED  •  0.1.1", 23f))
        root.addView(label("Standalone Android utility overlay. The floating bubble and crosshair are implemented. Multiplayer hacks are NOT connected to Minecraft or Realms.", 14f))
        permissionStatus = label("", 14f)
        root.addView(permissionStatus)
        root.addView(Button(this).apply {
            text = "1. Allow display over other apps"
            setOnClickListener { openOverlayPermission() }
        })
        root.addView(Button(this).apply {
            text = "2. Start floating overlay"
            setOnClickListener {
                if (Settings.canDrawOverlays(this@MainActivity)) {
                    startForegroundService(Intent(this@MainActivity, OverlayService::class.java))
                    Toast.makeText(this@MainActivity, "Bedrock Enhanced started", Toast.LENGTH_SHORT).show()
                } else openOverlayPermission()
            }
        })
        root.addView(Button(this).apply {
            text = "3. Open Minecraft Bedrock"
            setOnClickListener {
                val launch = packageManager.getLaunchIntentForPackage("com.mojang.minecraftpe")
                if (launch != null) startActivity(launch)
                else Toast.makeText(this@MainActivity, "Minecraft Bedrock not found", Toast.LENGTH_LONG).show()
            }
        })
        root.addView(Button(this).apply {
            text = "Stop overlay"
            setOnClickListener { stopService(Intent(this@MainActivity, OverlayService::class.java)) }
        })
        root.addView(label("Only use additional gameplay modifications on servers you control or where you have permission. This APK does not inject code into Minecraft.", 12f))
        setContentView(root)
    }

    private fun openOverlayPermission() {
        startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
    }

    override fun onResume() {
        super.onResume()
        if (::permissionStatus.isInitialized) {
            permissionStatus.text = if (Settings.canDrawOverlays(this)) "Overlay permission: granted" else "Overlay permission: not granted"
        }
    }
}
