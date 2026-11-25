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

    private val glyphCache =
        mutableMapOf<Pair<String, Char>, Array<String>>()

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

        '0' to arrayOf("01110","10011","10101","10101","11001","10001","01110"),
        '1' to arrayOf("00100","01100","00100","00100","00100","00100","01110"),
        '2' to arrayOf("01110","10001","00001","00010","00100","01000","11111"),
        '3' to arrayOf("11110","00001","00001","01110","00001","00001","11110"),
        '4' to arrayOf("00010","00110","01010","10010","11111","00010","00010"),
        '5' to arrayOf("11111","10000","10000","11110","00001","00001","11110"),
        '6' to arrayOf("01110","10000","10000","11110","10001","10001","01110"),
        '7' to arrayOf("11111","00001","00010","00100","01000","01000","01000"),
        '8' to arrayOf("01110","10001","10001","01110","10001","10001","01110"),
        '9' to arrayOf("01110","10001","10001","01111","00001","00001","01110"),

        ' ' to arrayOf("00000","00000","00000","00000","00000","00000","00000"),
        '!' to arrayOf("00100","00100","00100","00100","00100","00000","00100"),
        '"' to arrayOf("01010","01010","00100","00000","00000","00000","00000"),
        '#' to arrayOf("01010","11111","01010","01010","11111","01010","00000"),
        '$' to arrayOf("00100","01111","10100","01110","00101","11110","00100"),
        '%' to arrayOf("11001","11010","00100","01000","10110","00110","00000"),
        '&' to arrayOf("01100","10010","10100","01000","10101","10010","01101"),
        '\'' to arrayOf("00100","00100","00010","00000","00000","00000","00000"),
        '(' to arrayOf("00010","00100","01000","01000","01000","00100","00010"),
        ')' to arrayOf("01000","00100","00010","00010","00010","00100","01000"),
        '*' to arrayOf("00000","10101","01110","11111","01110","10101","00000"),
        '+' to arrayOf("00000","00100","00100","11111","00100","00100","00000"),
        ',' to arrayOf("00000","00000","00000","00000","00100","00100","01000"),
        '-' to arrayOf("00000","00000","00000","11111","00000","00000","00000"),
        '.' to arrayOf("00000","00000","00000","00000","00000","00100","00100"),
        '/' to arrayOf("00001","00010","00100","01000","10000","00000","00000"),
        ':' to arrayOf("00000","00100","00100","00000","00100","00100","00000"),
        ';' to arrayOf("00000","00100","00100","00000","00100","00100","01000"),
        '<' to arrayOf("00001","00010","00100","01000","00100","00010","00001"),
        '=' to arrayOf("00000","11111","00000","11111","00000","00000","00000"),
        '>' to arrayOf("10000","01000","00100","00010","00100","01000","10000"),
        '?' to arrayOf("01110","10001","00001","00010","00100","00000","00100"),
        '@' to arrayOf("01110","10001","10111","10101","10111","10000","01110"),
        '[' to arrayOf("01110","01000","01000","01000","01000","01000","01110"),
        '\\' to arrayOf("10000","01000","00100","00010","00001","00000","00000"),
        ']' to arrayOf("01110","00010","00010","00010","00010","00010","01110"),
        '^' to arrayOf("00100","01010","10001","00000","00000","00000","00000"),
        '_' to arrayOf("00000","00000","00000","00000","00000","00000","11111"),
        '{' to arrayOf("00010","00100","00100","01000","00100","00100","00010"),
        '|' to arrayOf("00100","00100","00100","00100","00100","00100","00100"),
        '}' to arrayOf("01000","00100","00100","00010","00100","00100","01000")
    )

    override fun performOnServiceConnected(
        context: Context,
        glyphMatrixManager: GlyphMatrixManager
    ) {
        val preferences = context.getSharedPreferences(
            "glyph_settings",
            Context.MODE_PRIVATE
        )

        staticText = (
            preferences.getString("static_text", "GLY") ?: "GLY"
        ).uppercase().take(3)

        scrollText = (
            preferences.getString("scroll_text", "GLYPH TEXT") ?: "GLYPH TEXT"
        ).uppercase()

        fontStyle = preferences.getString(
            "font_style",
            FONT_CLASSIC
        ) ?: FONT_CLASSIC

        autoBrightness = preferences.getBoolean(
            "auto_brightness",
            true
        )

        manualBrightnessLevel = preferences.getInt(
            "manual_brightness_level",
            3
        ).coerceIn(1, 6)

        scrollContinuation = preferences.getBoolean(
            "scroll_continuation",
            false
        )

        if (staticText.isBlank()) staticText = " "
        if (scrollText.isBlank()) scrollText = " "

        stopLightSensor()

        sensorManager = context.getSystemService(
            Context.SENSOR_SERVICE
        ) as? SensorManager

        lightSensor = sensorManager?.getDefaultSensor(
            Sensor.TYPE_LIGHT
        )

        adaptiveBrightness = FALLBACK_ADAPTIVE_BRIGHTNESS
        hasLuxReading = false

        isConnected = true
        isPressed = false
        isDimmed = false

        animationJob?.cancel()
        animationJob = null

        dimJob?.cancel()
        dimJob = null

        showStaticText()
        restartDimTimer()
    }

    override fun onTouchPointPressed() {
        if (!isConnected) return

        isPressed = true
        dimJob?.cancel()
        stopLightSensor()
        isDimmed = false

        if (animationJob?.isActive != true) {
            showStaticText()
        }
    }

    override fun onTouchPointReleased() {
        if (!isConnected) return

        isPressed = false
        restartDimTimer()
    }

    override fun onTouchPointLongPress() {
        if (!isConnected) return

        dimJob?.cancel()
        stopLightSensor()
        animationJob?.cancel()
        isDimmed = false

        animationJob = scope.launch {

            if (scrollContinuation) {

                val prefix = staticText

                val fullText = if (scrollText.startsWith(prefix)) {
                    scrollText
                } else {
                    prefix + scrollText
                }

                val firstX = (25 - getTextWidth(prefix)) / 2

                val nextLetterX =
                    firstX + getTextWidth(prefix) + getLetterSpacing()

                val tailPadding =
                    (25 - nextLetterX).coerceAtLeast(0)

                val totalWidth =
                    getTextWidth(fullText) + tailPadding

                for (offset in 0..(firstX + totalWidth)) {

                    drawText(
                        text = fullText,
                        startX = firstX - offset,
                        brightness = FULL_BRIGHTNESS,
                        gapAfterIndex = prefix.length - 1,
                        extraGap = tailPadding
                    )

                    delay(70)
                }

            } else {

                val text = scrollText
                val textWidth = getTextWidth(text)

                for (offset in 0..(textWidth + 25)) {

                    drawText(
                        text = text,
                        startX = 25 - offset,
                        brightness = FULL_BRIGHTNESS
                    )

                    delay(70)
                }
            }

            animationJob = null
            showStaticText()
            restartDimTimer()
        }
    }

    private fun restartDimTimer() {
        dimJob?.cancel()
        dimJob = null

        if (!isConnected) return
        if (isPressed) return
        if (animationJob?.isActive == true) return

        dimJob = scope.launch {

            delay(DIM_DELAY_MS)

            if (
                isConnected &&
                !isPressed &&
                animationJob?.isActive != true
            ) {
                enterAdaptiveMode()
            }
        }
    }

    private fun enterAdaptiveMode() {

        if (
            !isConnected ||
            isPressed ||
            animationJob?.isActive == true
        ) return

        isDimmed = true
        hasLuxReading = false

        if (!autoBrightness) {

            stopLightSensor()

            adaptiveBrightness =
                MANUAL_BRIGHTNESS[manualBrightnessLevel - 1]

            showStaticText()
            return
        }

        adaptiveBrightness = FALLBACK_ADAPTIVE_BRIGHTNESS
        showStaticText()

        val sensor = lightSensor ?: return

        lightListenerRegistered = sensorManager?.registerListener(
            this,
            sensor,
            SensorManager.SENSOR_DELAY_NORMAL
        ) == true
    }

    private fun stopLightSensor() {

        if (lightListenerRegistered) {
            sensorManager?.unregisterListener(this)
            lightListenerRegistered = false
        }
    }

    override fun onSensorChanged(event: SensorEvent) {

        if (
            !isConnected ||
            !isDimmed ||
            isPressed ||
            !autoBrightness
        ) return

        if (animationJob?.isActive == true) return
        if (event.sensor.type != Sensor.TYPE_LIGHT) return

        val lux = event.values.firstOrNull() ?: return

        if (!lux.isFinite() || lux < 0f) return

        val target = brightnessFromLux(lux)

        adaptiveBrightness = if (!hasLuxReading) {
            target
        } else {
            (
                adaptiveBrightness * 0.75 +
                target * 0.25
            ).roundToInt()
        }

        val firstReading = !hasLuxReading
        hasLuxReading = true

        if (
            firstReading ||
            abs(
                adaptiveBrightness - lastRenderedBrightness
            ) >= MIN_UPDATE_DIFFERENCE
        ) {
            showStaticText()
        }
    }

    override fun onAccuracyChanged(
        sensor: Sensor?,
        accuracy: Int
    ) = Unit

    private fun brightnessFromLux(lux: Float): Int {

        val safeLux = lux.coerceIn(
            0f,
            5000f
        ).toDouble()

        val ratio = ln(1.0 + safeLux) / ln(5001.0)

        return (
            MIN_ADAPTIVE_BRIGHTNESS +
            ratio * (
                MAX_ADAPTIVE_BRIGHTNESS -
                MIN_ADAPTIVE_BRIGHTNESS
            )
        ).roundToInt().coerceIn(
            MIN_ADAPTIVE_BRIGHTNESS,
            MAX_ADAPTIVE_BRIGHTNESS
        )
    }

    private fun showStaticText() {

        if (!isConnected) return

        val text = staticText
        val textWidth = getTextWidth(text)
        val startX = (25 - textWidth) / 2

        val brightness = if (isDimmed) {
            adaptiveBrightness
        } else {
            FULL_BRIGHTNESS
        }

        lastRenderedBrightness = brightness

        drawText(
            text,
            startX,
            brightness
        )
    }

    private fun getTextWidth(text: String): Int {

        if (text.isEmpty()) return 0

        val totalWidth = text.sumOf {
            getRenderedGlyph(it)[0].length
        }

        return totalWidth +
            (text.length - 1) * getLetterSpacing()
    }

    private fun getLetterSpacing(): Int {

        return if (fontStyle == FONT_GRAFFITI) {
            2
        } else {
            1
        }
    }

    private fun getRenderedGlyph(
        char: Char
    ): Array<String> {

        val key = fontStyle to char

        return glyphCache.getOrPut(key) {

            val base = baseFont[char] ?: baseFont['?']!!

            when (fontStyle) {
                FONT_BOLD -> embolden(base)
                FONT_MINI -> shrinkToMini(base)
                FONT_GRAFFITI -> makeGraffiti(char, base)
                else -> base
            }
        }
    }

    private fun embolden(
        rows: Array<String>
    ): Array<String> {

        val height = rows.size
        val width = rows[0].length

        val result = Array(height) {
            CharArray(width + 1) { '0' }
        }

        for (y in 0 until height) {
            for (x in 0 until width) {

                if (rows[y][x] == '1') {
                    result[y][x] = '1'
                    result[y][x + 1] = '1'
                }
            }
        }

        return Array(height) {
            String(result[it])
        }
    }

    private fun shrinkToMini(
        rows: Array<String>
    ): Array<String> {

        val sourceHeight = rows.size
        val sourceWidth = rows[0].length

        val targetWidth = 4
        val targetHeight = 5

        return Array(targetHeight) { y ->

            buildString {

                for (x in 0 until targetWidth) {

                    val startX =
                        x * sourceWidth / targetWidth

                    val endX = (
                        (x + 1) * sourceWidth / targetWidth
                    ).coerceAtMost(sourceWidth)

                    val startY =
                        y * sourceHeight / targetHeight

                    val endY = (
                        (y + 1) * sourceHeight / targetHeight
                    ).coerceAtMost(sourceHeight)

                    var total = 0
                    var on = 0

                    for (sy in startY until endY) {
                        for (sx in startX until endX) {

                            total++

                            if (rows[sy][sx] == '1') {
                                on++
                            }
                        }
                    }

                    append(
                        if (on * 2 >= total && total > 0) {
                            '1'
                        } else {
                            '0'
                        }
                    )
                }
            }
        }
    }

    private fun makeGraffiti(
        char: Char,
        rows: Array<String>
    ): Array<String> {

        val width = 7
        val height = 11

        val sourceWidth = rows[0].length
        val sourceHeight = rows.size

        val original = Array(height) { y ->

            CharArray(width) { x ->

                rows[
                    y * sourceHeight / height
                ][
                    x * sourceWidth / width
                ]
            }
        }

        val result = Array(height) { y ->
            original[y].copyOf()
        }

        val isHeavyGlyph =
            char in 'A'..'Z' || char in '0'..'9'

        if (isHeavyGlyph) {

            val chips =
                mutableListOf<Triple<Int, Int, Int>>()

            val extraPixels =
                mutableListOf<Triple<Int, Int, Int>>()

            for (y in 1 until height - 1) {
                for (x in 0 until width) {

                    val left = if (
                        x > 0 &&
                        original[y][x - 1] == '1'
                    ) 1 else 0

                    val right = if (
                        x < width - 1 &&
                        original[y][x + 1] == '1'
                    ) 1 else 0

                    val up = if (
                        original[y - 1][x] == '1'
                    ) 1 else 0

                    val down = if (
                        original[y + 1][x] == '1'
                    ) 1 else 0

                    val neighbors =
                        left + right + up + down

                    val score = (
                        char.code * 31 +
                        x * 47 +
                        y * 71 +
                        (x + 3) * (y + 5) * 11
                    ) % 97

                    if (
                        original[y][x] == '1' &&
                        neighbors in 2..3
                    ) {

                        chips.add(
                            Triple(score, x, y)
                        )

                    } else if (
                        original[y][x] == '0' &&
                        neighbors >= 2
                    ) {

                        extraPixels.add(
                            Triple(score, x, y)
                        )
                    }
                }
            }

            val chosen =
                mutableListOf<Pair<Int, Int>>()

            for ((_, x, y) in chips.sortedBy { it.first }) {

                if (
                    chosen.all { (cx, cy) ->
                        abs(cx - x) + abs(cy - y) > 3
                    }
                ) {

                    result[y][x] = '0'
                    chosen.add(x to y)
                }

                if (chosen.size == 3) break
            }

            val extra = extraPixels.minByOrNull {
                it.first
            }

            if (extra != null) {
                result[extra.third][extra.second] = '1'
            }
        }

        return Array(height) {
            String(result[it])
        }
    }

    private fun drawText(
        text: String,
        startX: Int,
        brightness: Int,
        gapAfterIndex: Int = -1,
        extraGap: Int = 0
    ) {

        if (!isConnected) return

        val glyphs = text.map {
            getRenderedGlyph(it)
        }

        val fontHeight = glyphs.maxOfOrNull {
            it.size
        } ?: 0

        val startY =
            ((25 - fontHeight) / 2).coerceAtLeast(0)

        val spacing = getLetterSpacing()

        val frame = IntArray(25 * 25)

        var cursorX = startX

        for ((index, glyph) in glyphs.withIndex()) {

            val glyphHeight = glyph.size
            val glyphWidth = glyph[0].length

            for (y in 0 until glyphHeight) {
                for (x in 0 until glyphWidth) {

                    val px = cursorX + x
                    val py = startY + y

                    if (
                        glyph[y][x] == '1' &&
                        px in 0..24 &&
                        py in 0..24
                    ) {

                        frame[py * 25 + px] = brightness
                    }
                }
            }

            cursorX += glyphWidth + spacing

            if (index == gapAfterIndex) {
                cursorX += extraGap
            }
        }

        glyphMatrixManager?.setMatrixFrame(frame)
    }

    override fun performOnServiceDisconnected(
        context: Context
    ) {

        stopLightSensor()

        isConnected = false

        sensorManager = null
        lightSensor = null

        dimJob?.cancel()
        dimJob = null

        animationJob?.cancel()
        animationJob = null
    }

    override fun onDestroy() {

        stopLightSensor()

        isConnected = false

        dimJob?.cancel()
        animationJob?.cancel()

        scope.cancel()

        super.onDestroy()
    }
}
