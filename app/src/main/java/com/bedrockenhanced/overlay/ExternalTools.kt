package com.bedrockenhanced.overlay

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import org.json.JSONArray
import org.json.JSONObject

/**
 * Tools that work WITHOUT injecting into Minecraft or accessing its process.
 * Server commands are copied to the clipboard for deliberate, manual use by an operator.
 */
class ExternalTools(
    private val context: Context,
    private val prefs: SharedPreferences,
    private val refresh: () -> Unit
) {
    private val density get() = context.resources.displayMetrics.density
    private fun dp(v: Int) = (v * density).toInt()

    private fun label(parent: LinearLayout, value: String, size: Float = 13f, color: Int = Color.LTGRAY) {
        parent.addView(TextView(context).apply {
            text = value
            textSize = size
            setTextColor(color)
            setPadding(0, dp(5), 0, dp(8))
        })
    }

    private fun heading(parent: LinearLayout, value: String) {
        label(parent, value, 17f, Color.WHITE)
    }

    private fun copy(value: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Bedrock command", value))
        Toast.makeText(context, "Copied. Paste into Minecraft chat if authorized.", Toast.LENGTH_LONG).show()
    }

    private fun action(parent: LinearLayout, title: String, onTap: () -> Unit) {
        parent.addView(Button(context).apply {
            text = title
            isAllCaps = false
            setOnClickListener { onTap() }
        })
    }

    fun addCommandTools(parent: LinearLayout) {
        heading(parent, "Command shortcuts (operator-only)")
        label(parent, "These buttons COPY commands. Nothing is sent to Minecraft automatically. On a friend's server, the owner must grant operator/command permissions. Some commands also require cheats.")
        action(parent, "Copy: Night Vision (fullbright alternative)") {
            copy("/effect @s night_vision 999999 0 true")
        }
        action(parent, "Copy: Slow Falling (fall safety)") {
            copy("/effect @s slow_falling 600 0 true")
        }
        action(parent, "Copy: Creative flight") { copy("/gamemode creative @s") }
        action(parent, "Copy: Survival mode") { copy("/gamemode survival @s") }
        action(parent, "Copy: Spectator camera") { copy("/gamemode spectator @s") }
        action(parent, "Copy: Show coordinates") { copy("/gamerule showcoordinates true") }
        action(parent, "Copy: Set day") { copy("/time set day") }
        action(parent, "Copy: Set night") { copy("/time set night") }
        action(parent, "Copy: Clear weather") { copy("/weather clear") }
        action(parent, "Copy: Rain") { copy("/weather rain") }

        heading(parent, "Movement effects")
        label(parent, "Default values: Speed 1.0× and Jump 1.0× (normal). Effect levels are Minecraft potion levels, not exact movement multipliers.")
        effectSlider(parent, "Speed", "speed", "speed_level")
        effectSlider(parent, "Jump", "jump_boost", "jump_level")
    }

    private fun effectSlider(parent: LinearLayout, display: String, effect: String, key: String) {
        val initial = prefs.getInt(key, 0).coerceIn(0, 4)
        val value = TextView(context).apply {
            textSize = 14f
            setTextColor(Color.WHITE)
            text = "$display: " + if (initial == 0) "1.0× (normal)" else "Effect level $initial"
        }
        parent.addView(value)
        val seek = SeekBar(context).apply {
            max = 4
            progress = initial
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(bar: SeekBar?, progress: Int, fromUser: Boolean) {
                    value.text = "$display: " + if (progress == 0) "1.0× (normal)" else "Effect level $progress"
                    if (fromUser) prefs.edit().putInt(key, progress).apply()
                }
                override fun onStartTrackingTouch(bar: SeekBar?) {}
                override fun onStopTrackingTouch(bar: SeekBar?) {}
            })
        }
        parent.addView(seek)
        action(parent, "Copy $display command") {
            val level = seek.progress
            if (level == 0) {
                Toast.makeText(context, "$display is normal: no command needed", Toast.LENGTH_SHORT).show()
            } else {
                copy("/effect @s $effect 600 ${level - 1} true")
            }
        }
    }

    private fun editable(hint: String, numeric: Boolean = false): EditText =
        EditText(context).apply {
            this.hint = hint
            textSize = 14f
            setTextColor(Color.WHITE)
            setHintTextColor(Color.GRAY)
            setSingleLine(true)
            inputType = if (numeric) {
                InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_SIGNED or InputType.TYPE_NUMBER_FLAG_DECIMAL
            } else InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        }

    fun addWaypoints(parent: LinearLayout) {
        heading(parent, "Manual waypoints")
        label(parent, "Save coordinates you enter yourself. An external overlay cannot read your position from Bedrock. Waypoints are stored only on this phone.")
        val name = editable("Waypoint name")
        parent.addView(name)
        val coords = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL }
        val x = editable("X", true)
        val y = editable("Y", true)
        val z = editable("Z", true)
        for (field in listOf(x, y, z)) {
            coords.addView(field, LinearLayout.LayoutParams(0, dp(48), 1f))
        }
        parent.addView(coords)
        action(parent, "Save waypoint") {
            val n = name.text.toString().trim().take(30)
            val xVal = x.text.toString().trim().toDoubleOrNull()
            val yVal = y.text.toString().trim().toDoubleOrNull()
            val zVal = z.text.toString().trim().toDoubleOrNull()
            if (n.isBlank() || xVal == null || yVal == null || zVal == null ||
                !xVal.isFinite() || !yVal.isFinite() || !zVal.isFinite()) {
                Toast.makeText(context, "Enter a name and valid X, Y, Z coordinates", Toast.LENGTH_LONG).show()
                return@action
            }
            val list = readWaypoints()
            if (list.length() >= 30) {
                Toast.makeText(context, "Maximum 30 saved waypoints", Toast.LENGTH_SHORT).show()
                return@action
            }
            list.put(JSONObject().put("name", n).put("x", xVal).put("y", yVal).put("z", zVal))
            prefs.edit().putString("waypoints", list.toString()).apply()
            refresh()
        }

        val saved = readWaypoints()
        if (saved.length() == 0) label(parent, "No waypoints saved yet.")
        for (i in 0 until saved.length()) {
            val wp = saved.optJSONObject(i) ?: continue
            val title = wp.optString("name")
            val xx = wp.optDouble("x")
            val yy = wp.optDouble("y")
            val zz = wp.optDouble("z")
            label(parent, "$title  •  $xx, $yy, $zz", 13f, Color.WHITE)
            val actions = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
            actions.addView(Button(context).apply {
                text = "Copy /tp"
                isAllCaps = false
                setOnClickListener { copy("/tp @s $xx $yy $zz") }
            }, LinearLayout.LayoutParams(0, dp(48), 1f))
            actions.addView(Button(context).apply {
                text = "Delete"
                isAllCaps = false
                setOnClickListener {
                    val current = readWaypoints()
                    val replacement = JSONArray()
                    for (index in 0 until current.length()) if (index != i) replacement.put(current.get(index))
                    prefs.edit().putString("waypoints", replacement.toString()).apply()
                    refresh()
                }
            }, LinearLayout.LayoutParams(0, dp(48), 1f))
            parent.addView(actions)
        }
    }

    private fun readWaypoints(): JSONArray = try {
        JSONArray(prefs.getString("waypoints", "[]") ?: "[]")
    } catch (_: Exception) {
        JSONArray()
    }
}
