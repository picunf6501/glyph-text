package com.nothinglondon.sdkdemo

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import com.nothinglondon.sdkdemo.ui.theme.NothingAndroidSDKDemoTheme

enum class FontOption(val key: String, val label: String) {
    CLASSIC("CLASSIC", "Classic"),
    BOLD("BOLD", "Bold"),
    MINI("MINI", "Mini"),
    GRAFFITI("GRAFFITI", "Graffiti")
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val preferences = getSharedPreferences(
            "glyph_settings",
            Context.MODE_PRIVATE
        )

        setContent {
            NothingAndroidSDKDemoTheme {
                var staticText by remember {
                    mutableStateOf(
                        preferences.getString("static_text", "GLY") ?: "GLY"
                    )
                }

                var scrollText by remember {
                    mutableStateOf(
                        preferences.getString("scroll_text", "GLYPH TEXT") ?: "GLYPH TEXT"
                    )
                }

                var fontStyle by remember {
                    mutableStateOf(
                        preferences.getString("font_style", FontOption.CLASSIC.key)
                            ?: FontOption.CLASSIC.key
                    )
                }

                // Новые настройки имеют значения по умолчанию для старых установок.
                var autoBrightness by remember {
                    mutableStateOf(preferences.getBoolean("auto_brightness", true))
                }
                var manualBrightnessLevel by remember {
                    mutableStateOf(
                        preferences.getInt("manual_brightness_level", 3).coerceIn(1, 6)
                    )
                }
                var scrollContinuation by remember {
                    mutableStateOf(preferences.getBoolean("scroll_continuation", false))
                }

                var saved by remember { mutableStateOf(false) }

                Scaffold(
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .verticalScroll(rememberScrollState())
