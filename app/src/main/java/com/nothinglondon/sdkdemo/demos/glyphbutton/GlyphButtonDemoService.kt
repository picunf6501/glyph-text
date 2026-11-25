package com.nothinglondon.sdkdemo.demos.glyphbutton

import android.content.Context
import com.nothing.ketchum.GlyphMatrixManager
import com.nothinglondon.sdkdemo.demos.GlyphMatrixService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class GlyphButtonDemoService : GlyphMatrixService("GlyphText") {

    companion object {
        private const val FULL_BRIGHTNESS = 2047
        private const val DIM_BRIGHTNESS = 100
        private const val DIM_DELAY_MS = 10_000L

        private const val FONT_CLASSIC = "CLASSIC"
        private const val FONT_BOLD = "BOLD"
        private const val FONT_MINI = "MINI"
    }

    private val scope = CoroutineScope(
        SupervisorJob() + Dispatchers.Main.immediate
    )

    private var animationJob: Job? = null
    private var dimJob: Job? = null

    private var isDimmed = false
    private var isPressed = false
    private var isConnected = false

    private var staticText = "GLY"
    private var scrollText = "GLYPH TEXT"
    private var fontStyle = FONT_CLASSIC

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

        fontStyle = preferences.getString("font_style", FONT_CLASSIC) ?: FONT_CLASSIC

        if (staticText.isBlank()) staticText = " "
        if (scrollText.isBlank()) scrollText = " "

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
        animationJob?.cancel()

        isDimmed = false

        animationJob = scope.launch {
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
                isDimmed = true
                showStaticText()
            }
        }
    }

    private fun showStaticText() {
        if (!isConnected) return

        val text = staticText
        val textWidth = getTextWidth(text)
        val startX = (25 - textWidth) / 2

        val brightness = if (isDimmed) {
            DIM_BRIGHTNESS
        } else {
            FULL_BRIGHTNESS
        }

        drawText(text, startX, brightness)
    }

    private fun getTextWidth(text: String): Int {
        if (text.isEmpty()) return 0

        val glyphs = text.map { getRenderedGlyph(it) }
        val widths = glyphs.sumOf { glyph ->
            if (glyph.isNotEmpty()) glyph[0].length else 0
        }

        val spacing = (text.length - 1).coerceAtLeast(0)
        return widths + spacing
    }

    private fun getRenderedGlyph(char: Char): Array<String> {
        val base = baseFont[char] ?: baseFont['?']!!

        return when (fontStyle) {
            FONT_BOLD -> embolden(base)
            FONT_MINI -> shrinkToMini(base)
            else -> base
        }
    }

    private fun embolden(rows: Array<String>): Array<String> {
        val height = rows.size
        val width = rows[0].length
        val out = Array(height) { CharArray(width + 1) { '0' } }

        for (y in 0 until height) {
            for (x in 0 until width) {
                if (rows[y][x] == '1') {
                    out[y][x] = '1'
                    if (x + 1 < width + 1) {
                        out[y][x + 1] = '1'
                    }
                }
            }
        }

        return Array(height) { String(out[it]) }
    }

    private fun shrinkToMini(rows: Array<String>): Array<String> {
        val sourceHeight = rows.size
        val sourceWidth = rows[0].length

        val targetWidth = 4
        val targetHeight = 5

        return Array(targetHeight) { y ->
            buildString {
                for (x in 0 until targetWidth) {
                    val startX = x * sourceWidth / targetWidth
                    val endX = ((x + 1) * sourceWidth / targetWidth).coerceAtMost(sourceWidth)
                    val startY = y * sourceHeight / targetHeight
                    val endY = ((y + 1) * sourceHeight / targetHeight).coerceAtMost(sourceHeight)

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

                    append(if (on * 2 >= total && total > 0) '1' else '0')
                }
            }
        }
    }

    private fun drawText(
        text: String,
        startX: Int,
        brightness: Int
    ) {
        if (!isConnected) return

        val glyphs = text.map { getRenderedGlyph(it) }
        val fontHeight = glyphs.maxOfOrNull { it.size } ?: 0
        val startY = ((25 - fontHeight) / 2).coerceAtLeast(0)

        val frame = IntArray(25 * 25)

        var cursorX = startX

        glyphs.forEach { glyph ->
            val glyphHeight = glyph.size
            val glyphWidth = if (glyph.isNotEmpty()) glyph[0].length else 0

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

            cursorX += glyphWidth + 1
        }

        glyphMatrixManager?.setMatrixFrame(frame)
    }

    override fun performOnServiceDisconnected(context: Context) {
        isConnected = false

        dimJob?.cancel()
        dimJob = null

        animationJob?.cancel()
        animationJob = null
    }

    override fun onDestroy() {
        isConnected = false

        dimJob?.cancel()
        animationJob?.cancel()

        scope.cancel()
        super.onDestroy()
    }
}
