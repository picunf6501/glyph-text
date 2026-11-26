package com.nothinglondon.sdkdemo

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
                        preferences.getString("font_style", "CLASSIC") ?: "CLASSIC"
                    )
                }

                var autoBrightness by remember {
                    mutableStateOf(
                        preferences.getBoolean("auto_brightness", true)
                    )
                }

                var manualBrightnessLevel by remember {
                    mutableStateOf(
                        preferences.getInt("manual_brightness_level", 3)
                            .coerceIn(1, 6)
                    )
                }

                var scrollContinuation by remember {
                    mutableStateOf(
                        preferences.getBoolean("scroll_continuation", false)
                    )
                }

                var autoScroll by remember { mutableStateOf(preferences.getBoolean("auto_scroll", false)) }
                var selectedApps by remember { mutableStateOf(preferences.getStringSet("auto_scroll_apps", emptySet())!!.toSet()) }
                var showApps by remember { mutableStateOf(false) }
                val installedApps = remember {
                    packageManager.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0)
                        .filter { it.activityInfo.packageName != packageName }
                        .distinctBy { it.activityInfo.packageName }
                        .map { it.activityInfo.packageName to it.loadLabel(packageManager).toString() }
                        .sortedBy { it.second.lowercase() }
                }
                var saved by remember {
                    mutableStateOf(false)
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .verticalScroll(rememberScrollState())
                            .padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(18.dp)
                    ) {

                        Text(
                            text = "GLYPH TEXT",
                            style = MaterialTheme.typography.headlineMedium
                        )

                        Text(
                            text = "Настрой свою Glyph Matrix",
                            style = MaterialTheme.typography.bodyLarge
                        )

                        HorizontalDivider()

                        Text(
                            text = "Основная надпись",
                            style = MaterialTheme.typography.titleMedium
                        )

                        OutlinedTextField(
                            value = staticText,
                            onValueChange = {
                                staticText = it
                                saved = false
                            },
                            label = {
                                Text("Первые 3 символа")
                            },
                            supportingText = {
                                Text("На матрице отображаются первые 3 символа")
                            },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Text(
                            text = "Текст при удержании",
                            style = MaterialTheme.typography.titleMedium
                        )

                        OutlinedTextField(
                            value = scrollText,
                            onValueChange = {
                                scrollText = it
                                saved = false
                            },
                            label = {
                                Text("Текст для прокрутки")
                            },
                            supportingText = {
                                Text("Прокручивается при удержании кнопки Glyph")
                            },
                            minLines = 2,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Text(
                            text = "Шрифт",
                            style = MaterialTheme.typography.titleMedium
                        )

                        FontOption.entries.forEach { option ->

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {

                                RadioButton(
                                    selected = fontStyle == option.key,
                                    onClick = {
                                        fontStyle = option.key
                                        saved = false
                                    }
                                )

                                Text(
                                    text = option.label,
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }
                        }

                        HorizontalDivider()

                        Text(
                            text = "Яркость в режиме ожидания",
                            style = MaterialTheme.typography.titleMedium
                        )

                        Text(
                            text = "Через 10 секунд применяется выбранная яркость.",
                            style = MaterialTheme.typography.bodySmall
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {

                            Text(
                                text = "Автояркость",
                                modifier = Modifier.weight(1f)
                            )

                            Switch(
                                checked = autoBrightness,
                                onCheckedChange = {
                                    autoBrightness = it
                                    saved = false
                                }
                            )
                        }

                        if (!autoBrightness) {

                            Text(
                                text = "Ручная яркость: $manualBrightnessLevel / 6"
                            )

                            Slider(
                                value = manualBrightnessLevel.toFloat(),
                                onValueChange = {
                                    manualBrightnessLevel = it.roundToInt()
                                        .coerceIn(1, 6)
                                    saved = false
                                },
                                valueRange = 1f..6f,
                                steps = 4,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        HorizontalDivider()

                        Text(
                            text = "Прокрутка при удержании",
                            style = MaterialTheme.typography.titleMedium
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {

                            Text(
                                text = "Продолжать основную надпись",
                                modifier = Modifier.weight(1f)
                            )

                            Switch(
                                checked = scrollContinuation,
                                onCheckedChange = {
                                    scrollContinuation = it
                                    saved = false
                                }
                            )
                        }

                        Text(
                            text = if (scrollContinuation) {
                                "Основная надпись продолжает движение. Например, CUP → CUPSIZE."
                            } else {
                                "Текст начинает прокручиваться с правого края."
                            },
                            style = MaterialTheme.typography.bodySmall
                        )

                        HorizontalDivider()

                        Text("Автоматическая прокрутка", style = MaterialTheme.typography.titleMedium)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Auto Scroll Apps", modifier = Modifier.weight(1f))
                            Switch(checked = autoScroll, onCheckedChange = { autoScroll = it; saved = false })
                        }
                        if (autoScroll) {
                            Button(onClick = { startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) }) {
                                Text("Разрешить доступ к истории использования")
                            }
                            Button(onClick = { showApps = !showApps }) {
                                Text("Выбрать приложения (${selectedApps.size}/5)")
                            }
                            if (showApps) installedApps.forEach { (pkg, label) ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(checked = pkg in selectedApps,
                                        enabled = pkg in selectedApps || selectedApps.size < 5,
                                        onCheckedChange = { checked ->
                                            selectedApps = if (checked) selectedApps + pkg else selectedApps - pkg
                                            saved = false
                                        })
                                    Text(label)
                                }
                            }
                            Text("Прокрутка работает, пока выбранное приложение открыто и экран включён.")
                        }
                        HorizontalDivider()
                        Button(
                            onClick = {
                                preferences.edit()
                                    .putBoolean("auto_scroll", autoScroll)
                                    .putStringSet("auto_scroll_apps", selectedApps)
                                    .putString("static_text", staticText)
                                    .putString("scroll_text", scrollText)
                                    .putString("font_style", fontStyle)
                                    .putBoolean("auto_brightness", autoBrightness)
                                    .putInt(
                                        "manual_brightness_level",
                                        manualBrightnessLevel
                                    )
                                    .putBoolean(
                                        "scroll_continuation",
                                        scrollContinuation
                                    )
                                    .apply()

                                saved = true
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Сохранить настройки")
                        }

                        if (saved) {
                            Text(
                                text = "Настройки сохранены!",
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        HorizontalDivider()

                        Text(
                            text = "Предпросмотр",
                            style = MaterialTheme.typography.titleMedium
                        )

                        Text(
                            text = "На матрице: ${staticText.uppercase().take(3)}"
                        )

                        Text(
                            text = "При удержании: ${scrollText.uppercase()}"
                        )

                        Text(
                            text = "Шрифт: ${
                                FontOption.entries.firstOrNull {
                                    it.key == fontStyle
                                }?.label ?: "Classic"
                            }"
                        )

                        Text(
                            text = if (autoBrightness) {
                                "Яркость: автоматическая"
                            } else {
                                "Яркость: $manualBrightnessLevel / 6"
                            }
                        )

                        Text(
                            text = if (scrollContinuation) {
                                "Прокрутка: продолжение"
                            } else {
                                "Прокрутка: обычная"
                            }
                        )

                        Text(
                            text = "После сохранения переключи Glyph Toy, чтобы обновить настройки.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}
