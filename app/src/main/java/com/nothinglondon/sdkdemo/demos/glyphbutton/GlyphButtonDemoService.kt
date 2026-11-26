package com.nothinglondon.sdkdemo.demos.glyphbutton

import android.content.Context
import android.app.usage.UsageStatsManager
import android.app.usage.UsageEvents
import android.os.PowerManager
import android.app.KeyguardManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.nothing.ketchum.GlyphMatrixManager
import com.nothinglondon.sdkdemo.demos.GlyphMatrixService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.roundToInt

class GlyphButtonDemoService : GlyphMatrixService("GlyphText"), SensorEventListener {

    companion object {
        private const val FULL_BRIGHTNESS = 2047
        private const val DIM_DELAY_MS = 10_000L

        private const val MIN_ADAPTIVE_BRIGHTNESS = 350
        private const val MAX_ADAPTIVE_BRIGHTNESS = 1600
        private const val FALLBACK_ADAPTIVE_BRIGHTNESS = 700
        private const val MIN_UPDATE_DIFFERENCE = 40

        private val MANUAL_BRIGHTNESS = intArrayOf(
            250, 450, 700, 1000, 1400, 1800
        )

        private const val FONT_CLASSIC = "CLASSIC"
        private const val FONT_BOLD = "BOLD"
        private const val FONT_MINI = "MINI"
        private const val FONT_GRAFFITI = "GRAFFITI"
    }

    private val scope = CoroutineScope(
        SupervisorJob() + Dispatchers.Main.immediate
    )

    private var animationJob: Job? = null
    private var appMonitorJob: Job? = null
    private var autoJob: Job? = null
    private var foregroundPackage: String? = null
    private var lastEventTime = 0L

    private fun startAppMonitor(context: Context) {
        appMonitorJob?.cancel()
        autoJob?.cancel()
        autoJob = null
        val prefs = context.getSharedPreferences("glyph_settings", Context.MODE_PRIVATE)
        val usage = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val power = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val keyguard = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        foregroundPackage = null
        lastEventTime = System.currentTimeMillis() - 60_000L
        appMonitorJob = scope.launch {
            while (isConnected) {
                val now = System.currentTimeMillis()
                val enabled = prefs.getBoolean("auto_scroll", false)
                if (enabled) {
                    try {
                        val events = usage.queryEvents(lastEventTime, now)
                        val event = UsageEvents.Event()
                        while (events != null && events.hasNextEvent()) {
                            events.getNextEvent(event)
                            if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) foregroundPackage = event.packageName
                            if (event.eventType == UsageEvents.Event.ACTIVITY_PAUSED && foregroundPackage == event.packageName) foregroundPackage = null
                        }
                    } catch (_: SecurityException) { foregroundPackage = null }
                } else foregroundPackage = null
                lastEventTime = now
                val active = enabled && power.isInteractive && !keyguard.isKeyguardLocked &&
                    prefs.getStringSet("auto_scroll_apps", emptySet())?.contains(foregroundPackage) == true
                if (active && !isPressed && animationJob?.isActive != true && autoJob?.isActive != true) {
                    dimJob?.cancel()
                    stopLightSensor()
                    isDimmed = false
                    autoJob = scope.launch {
                        while (true) {
                            val text = scrollText
                            for (offset in 0..(getTextWidth(text) + 25)) {
                                drawText(text, 25 - offset, FULL_BRIGHTNESS)
                                delay(70)
                            }
                            delay(1000)
                        }
