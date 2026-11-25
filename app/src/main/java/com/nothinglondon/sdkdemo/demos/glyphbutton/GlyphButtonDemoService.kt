
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

    private val font = mapOf(
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
        '.' to arrayOf("00000","00000","00000","00000","00000","00100","00100"),
        '-' to arrayOf("00000","00000","00000","11111","00000","00000","00000"),
        '?' to arrayOf("01110","10001","00001","00010","00100","00000","00100"),
        '<' to arrayOf("00001","00010","00100","01000","00100","00010","00001"),
        '>' to arrayOf("10000","01000","00100","00010","00100","01000","10000")
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

        // Wake up the static text immediately.
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
            val textWidth = text.length * 6 - 1

            for (offset in 0..(textWidth + 25)) {
                drawText(
                    text,
                    25 - offset,
                    FULL_BRIGHTNESS
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
        val textWidth = text.length * 6 - 1
        val startX = (25 - textWidth) / 2

        val brightness = if (isDimmed) {
            DIM_BRIGHTNESS
        } else {
            FULL_BRIGHTNESS
        }

        drawText(text, startX, brightness)
    }

    private fun drawText(
        text: String,
        startX: Int,
        brightness: Int
    ) {
        if (!isConnected) return

        val frame = IntArray(25 * 25)

        text.forEachIndexed { index, letter ->
            val pattern = font[letter] ?: return@forEachIndexed

            pattern.forEachIndexed { y, row ->
                row.forEachIndexed { x, pixel ->
                    val px = startX + index * 6 + x
                    val py = 9 + y

                    if (
                        pixel == '1' &&
                        px in 0..24 &&
                        py in 0..24
                    ) {
                        frame[py * 25 + px] = brightness
                    }
                }
            }
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
