package com.nothinglondon.sdkdemo.demos.glyphbutton

import android.content.Context
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
        // После ожидания яркость зависит от освещения (lux), а не от фиксированного 100.
        private const val MIN_ADAPTIVE_BRIGHTNESS = 350
        private const val MAX_ADAPTIVE_BRIGHTNESS = 1600
        private const val FALLBACK_ADAPTIVE_BRIGHTNESS = 700
        private const val MIN_UPDATE_DIFFERENCE = 40

        // Уровни яркости для ручного режима, 1..6 (0..2047 в SDK).
        private val MANUAL_BRIGHTNESS = intArrayOf(250, 450, 700, 1000, 1400, 1800)

        private const val FONT_CLASSIC = "CLASSIC"
        private const val FONT_BOLD = "BOLD"
        private const val FONT_MINI = "MINI"
        private const val FONT_GRAFFITI = "GRAFFITI"
    }

    private val scope = CoroutineScope(
        SupervisorJob() + Dispatchers.Main.immediate
    )

    private var animationJob: Job? = null
    private var dimJob: Job? = null

    private var isDimmed = false
    private var isPressed = false
    private var isConnected = false

    private var sensorManager: SensorManager? = null
    private var lightSensor: Sensor? = null
    private var lightListenerRegistered = false
    private var hasLuxReading = false
    private var adaptiveBrightness = FALLBACK_ADAPTIVE_BRIGHTNESS
    private var lastRenderedBrightness = FULL_BRIGHTNESS

    private var staticText = "GLY"
    private var scrollText = "GLYPH TEXT"
    private var fontStyle = FONT_CLASSIC
    private var autoBrightness = true
    private var manualBrightnessLevel = 3
    private var scrollContinuation = false

    // Кэш избавляет анимацию от повторного построения букв на каждом кадре.
    private val glyphCache = mutableMapOf<Pair<String, Char>, Array<String>>()

    private val baseFont = mapOf(
        'A' to arrayOf("01110","10001","10001","11111","10001","10001","10001"),
        'B' to arrayOf("11110","10001","10001","11110","10001","10001","11110"),
        'C' to arrayOf("01110","10001","10000","10000","10000","10001","01110"),
        'D' to arrayOf("11110","10001","10001","10001","10001","10001","11110"),
        'E' to arrayOf("11111","10000","10000","11110","10000","10000","11111"),
        'F' to arrayOf("11111","10000","10000","11110","10000","10000","10000"),
        'G' to arrayOf("01110","10001","10000","10111","10001","10001","01110"),
        'H' to arrayOf("10001","10001","10001","11111","10001","10001","10001"),
        'I' to arrayOf("11111","00100","00100","00100","00100","00100","11111"),
        'J' to arrayOf("00111","00010","00010","00010","10010","10010","01100"),
        'K' to arrayOf("10001","10010","10100","11000","10100","10010","10001"),
        'L' to arrayOf("10000","10000","10000","10000","10000","10000","11111"),
        'M' to arrayOf("10001","11011","10101","10101","10001","10001","10001"),
        'N' to arrayOf("10001","11001","10101","10011","10001","10001","10001"),
        'O' to arrayOf("01110","10001","10001","10001","10001","10001","01110"),
        'P' to arrayOf("11110","10001","10001","11110","10000","10000","10000"),
        'Q' to arrayOf("01110","10001","10001","10001","10101","10010","01101"),
        'R' to arrayOf("11110","10001","10001","11110","10100","10010","10001"),
        'S' to arrayOf("01111","10000","10000","01110","00001","00001","11110"),
        'T' to arrayOf("11111","00100","00100","00100","00100","00100","00100"),
        'U' to arrayOf("10001","10001","10001","10001","10001","10001","01110"),
        'V' to arrayOf("10001","10001","10001","10001","10001","01010","00100"),
        'W' to arrayOf("10001","10001","10001","10101","10101","10101","01010"),
        'X' to arrayOf("10001","10001","01010","00100","01010","10001","10001"),
        'Y' to arrayOf("10001","10001","01010","00100","00100","00100","00100"),
        'Z' to arrayOf("11111","00001","00010","00100","01000","10000","11111"),
