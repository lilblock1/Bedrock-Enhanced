package com.bedrockenhanced.overlay

import android.app.*
import android.content.*
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.BatteryManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.provider.Settings
import android.view.*
import android.widget.*
import kotlin.math.abs

class OverlayService : Service() {
    private lateinit var wm: WindowManager
    private var bubble: View? = null
    private var panel: View? = null
    private var crosshair: View? = null
    private val prefs by lazy { getSharedPreferences("bedrock_enhanced", MODE_PRIVATE) }

    private fun dp(n: Int) = (n * resources.displayMetrics.density).toInt()
    private fun bg(color: Int, radius: Int = 14) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(radius).toFloat()
    }
    private fun params(width: Int, height: Int, flags: Int = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE) =
        WindowManager.LayoutParams(width, height, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY, flags, PixelFormat.TRANSLUCENT).apply {
            gravity = Gravity.TOP or Gravity.START
            x = prefs.getInt("bubble_x", dp(16))
            y = prefs.getInt("bubble_y", dp(130))
        }

    override fun onCreate() {
        super.onCreate()
        if (!Settings.canDrawOverlays(this)) { stopSelf(); return }
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        startAsForeground()
        try {
            showBubble()
            if (prefs.getBoolean("crosshair", false)) showCrosshair()
        } catch (e: Exception) {
            android.util.Log.e("BedrockEnhanced", "Overlay creation failed", e)
            stopSelf()
        }
    }

    private fun startAsForeground() {
        val channelId = "bedrock_enhanced_overlay"
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(NotificationChannel(channelId, "Bedrock Enhanced", NotificationManager.IMPORTANCE_LOW))
        val pending = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val notification = Notification.Builder(this, channelId)
            .setContentTitle("Bedrock Enhanced is active")
            .setContentText("Tap to control or stop the floating menu")
            .setSmallIcon(android.R.drawable.ic_menu_manage)
            .setContentIntent(pending)
            .build()
        startForeground(1, notification)
    }

    private fun showBubble() {
        val view = TextView(this).apply {
            text = "BE"
            textSize = 17f
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            background = bg(Color.rgb(28, 116, 84), 27)
            elevation = dp(6).toFloat()
        }
        val lp = params(dp(54), dp(54))
        wm.addView(view, lp)
        bubble = view
        var startX = 0; var startY = 0; var touchX = 0f; var touchY = 0f; var moved = false
        view.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    startX = lp.x; startY = lp.y
                    touchX = event.rawX; touchY = event.rawY
                    moved = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - touchX).toInt()
                    val dy = (event.rawY - touchY).toInt()
                    if (abs(dx) > dp(7) || abs(dy) > dp(7)) moved = true
                    val metrics = resources.displayMetrics
                    lp.x = (startX + dx).coerceIn(0, (metrics.widthPixels - dp(54)).coerceAtLeast(0))
                    lp.y = (startY + dy).coerceIn(0, (metrics.heightPixels - dp(54)).coerceAtLeast(0))
                    wm.updateViewLayout(view, lp)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    prefs.edit().putInt("bubble_x", lp.x).putInt("bubble_y", lp.y).apply()
                    if (!moved) togglePanel()
                    true
                }
                else -> false
            }
        }
    }

    private fun title(text: String, content: LinearLayout) {
        content.addView(TextView(this).apply {
            this.text = text
            setTextColor(Color.WHITE)
            textSize = 17f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, dp(14), 0, dp(5))
        })
    }

    private fun info(text: String, content: LinearLayout) {
        content.addView(TextView(this).apply {
            this.text = text
            setTextColor(Color.LTGRAY)
            textSize = 13f
            setPadding(0, dp(2), 0, dp(6))
        })
    }

    private fun planned(name: String, content: LinearLayout) {
        content.addView(Switch(this).apply {
            text = "$name · needs integration"
            textSize = 13f
            setTextColor(Color.GRAY)
            isEnabled = false
            isChecked = false
        })
    }

    private fun togglePanel() {
        if (panel != null) { remove(panel); panel = null; return }
        val scroll = ScrollView(this).apply {
            background = bg(Color.rgb(22, 29, 38))
            elevation = dp(9).toFloat()
            isFillViewport = false
        }
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(15), dp(12), dp(15), dp(16))
        }
        scroll.addView(content)
        title("BEDROCK ENHANCED  •  v0.1.1", content)
        info("Floating Android utility panel — no game injection", content)
        val realms = prefs.getBoolean("realms_profile", false)
        content.addView(Button(this).apply {
            text = if (realms) "Mode: Realms (tap for own server)" else "Mode: Own server (tap for Realms)"
            setOnClickListener {
                prefs.edit().putBoolean("realms_profile", !realms).apply()
                remove(panel); panel = null; togglePanel()
            }
        })
        title("Visuals", content)
        content.addView(Switch(this).apply {
            text = "Custom crosshair (working)"
            textSize = 14f
            setTextColor(Color.WHITE)
            isChecked = prefs.getBoolean("crosshair", false)
            setOnCheckedChangeListener { _, active ->
                prefs.edit().putBoolean("crosshair", active).apply()
                if (active) showCrosshair() else hideCrosshair()
            }
        })
        info("Crosshair size", content)
        content.addView(SeekBar(this).apply {
            max = 26; progress = prefs.getInt("crosshair_size", 12) - 4
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(bar: SeekBar?, value: Int, fromUser: Boolean) {
                    if (fromUser) { prefs.edit().putInt("crosshair_size", value + 4).apply(); crosshair?.invalidate() }
                }
                override fun onStartTrackingTouch(bar: SeekBar?) {}
                override fun onStopTrackingTouch(bar: SeekBar?) {}
            })
        })
        content.addView(Button(this).apply {
            fun name() = when (prefs.getInt("crosshair_color", 0)) { 1 -> "Lime"; 2 -> "Cyan"; else -> "White" }
            text = "Crosshair color: ${name()}"
            setOnClickListener {
                val n = (prefs.getInt("crosshair_color", 0) + 1) % 3
                prefs.edit().putInt("crosshair_color", n).apply()
                crosshair?.invalidate()
                text = "Crosshair color: ${name()}"
            }
        })
        listOf("Fullbright", "Ore highlighting", "Custom zoom", "Reduce particles", "Reduced fog", "Compact HUD", "Performance textures").forEach { planned(it, content) }
        title("Player", content)
        listOf("Fly", "Speed (1.0× default)", "Jump (1.0× default)", "No fall damage", "Freecam", "Auto tool").forEach { planned(it, content) }
        title("World & utilities", content)
        listOf("Time control", "Weather control", "Waypoints").forEach { planned(it, content) }
        title("Device diagnostics", content)
        val battery = getSystemService(BATTERY_SERVICE) as BatteryManager
        val pct = battery.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        val refresh = wm.defaultDisplay.refreshRate
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        val thermal = if (Build.VERSION.SDK_INT >= 29) pm.currentThermalStatus.toString() else "N/A"
        info("Battery: ${if (pct in 0..100) "$pct%" else "unavailable"} | Display refresh: ${"%.0f".format(refresh)} Hz | Thermal status: $thermal", content)
        info("Actual Minecraft FPS cannot be read by a standard overlay. FPS integration is pending.", content)
        content.addView(Button(this).apply { text = "Close menu"; setOnClickListener { togglePanel() } })
        val metrics = resources.displayMetrics
        val width = minOf(dp(325), (metrics.widthPixels - dp(25)).coerceAtLeast(dp(180)))
        val height = minOf(dp(545), (metrics.heightPixels * .85f).toInt())
        val lp = params(width, height)
        lp.x = (metrics.widthPixels - width - dp(12)).coerceAtLeast(0)
        lp.y = dp(44)
        wm.addView(scroll, lp)
        panel = scroll
    }

    private fun showCrosshair() {
        if (crosshair != null) return
        val view = object : View(this) {
            private val pen = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                strokeWidth = dp(2).toFloat()
                style = Paint.Style.STROKE
            }
            override fun onDraw(canvas: Canvas) {
                super.onDraw(canvas)
                pen.color = when (prefs.getInt("crosshair_color", 0)) { 1 -> Color.GREEN; 2 -> Color.CYAN; else -> Color.WHITE }
                val radius = dp(prefs.getInt("crosshair_size", 12)).toFloat()
                val gap = dp(3).toFloat()
                val x = width / 2f; val y = height / 2f
                canvas.drawLine(x - radius, y, x - gap, y, pen)
                canvas.drawLine(x + gap, y, x + radius, y, pen)
                canvas.drawLine(x, y - radius, x, y - gap, pen)
                canvas.drawLine(x, y + gap, x, y + radius, pen)
            }
        }
        // A small window with touch pass-through helps avoid Android 12+ touch blocking.
        val flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
        val lp = params(dp(80), dp(80), flags).apply {
            gravity = Gravity.CENTER
            x = 0; y = 0
            alpha = 0.7f
        }
        wm.addView(view, lp)
        crosshair = view
    }

    private fun hideCrosshair() { remove(crosshair); crosshair = null }
    private fun remove(view: View?) { if (view != null && ::wm.isInitialized) try { wm.removeView(view) } catch (_: Exception) {} }
    override fun onDestroy() {
        remove(panel); panel = null
        remove(bubble); bubble = null
        hideCrosshair()
        super.onDestroy()
    }
    override fun onBind(intent: Intent?): IBinder? = null
}
