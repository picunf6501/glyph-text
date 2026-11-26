
Загрузка более ранних сообщений…
делаем
Погнали, коллега! 🔥 Готовим Glyph Text v1.3.0 — Auto Scroll Apps.

Реализуем следующие функции:

Переключатель автоматической прокрутки.

Список реально установленных приложений с их названиями и иконками.

Выбор максимум пяти приложений.

Бесконечная прокрутка с паузой в одну секунду между повторами.

Возврат к статичной надписи при выходе из выбранного приложения.

Сохранение всех предыдущих настроек.

Какие файлы будем менять
Файл

Изменения

MainActivity.kt

Выбор приложений и переключатель

GlyphButtonDemoService.kt

Определение активного приложения и автоматическая прокрутка

AndroidManifest.xml

Разрешение на доступ к истории использования

Для начала пришли мне актуальный AndroidManifest.xml из нашего репозитория.

Открыть AndroidManifest.xml на GitHub

Остальные два файла у нас есть в переписке. Подготовлю полные коды прямо текстом, как в прошлый раз.

Единственное ограничение: пока не проверим на телефоне, неизвестно, позволит ли Nothing OS автоматически выводить анимацию без нажатия Glyph Button при открытом приложении. Начнём с тестовой сборки, а потом уже релиз.

лучше все скинь
Давай, коллега! Сделаем сразу все три файла для Glyph Text v1.3.0.

Сохраняем все функции предыдущей версии и добавляем выбор до пяти приложений, в которых надпись будет автоматически прокручиваться с паузой в одну секунду.

Для определения открытого приложения используем Android UsageStatsManager — без root и без необходимости включать специальные возможности.

Но есть один момент: GitHub не отдал актуальный манифест. Возьмём за основу нашу предыдущую рабочую версию.

Чтобы не переписывать заново несколько сотен строк и случайно не испортить Graffiti или адаптивную яркость, пришли сюда ZIP актуального репозитория.

Скачать исходники Glyph Text с GitHub

Просто закинь архив сюда, распаковывать не нужно.

Я подготовлю три полных кода прямо текстом:

MainActivity.kt

GlyphButtonDemoService.kt

AndroidManifest.xml

С прямыми ссылками на редакторы GitHub, как раньше.

Так мы сохраним все рабочие функции v1.2.0 и добавим Auto Scroll Apps поверх них. Автоматическую прокрутку при включённом экране пока считаем экспериментальной функцией — её поддержку со стороны Nothing OS проверим на твоём телефоне.


glyph-text-main.zip
Файл

Обработка заняла 38s
Архив получил, коллега! 🔥 Внутри есть все актуальные исходники Glyph Text.

Будем добавлять Auto Scroll Apps поверх рабочей v1.2.0, сохраняя четыре шрифта, автояркость, шесть ручных уровней яркости и бесшовную прокрутку. Иконку не трогаем.

1. Готовые коды
Скачать все три обновлённых файла ZIP

Готовый код

Куда вставлять

MainActivity.kt

Редактор GitHub

GlyphButtonDemoService.kt

Редактор GitHub

AndroidManifest.xml

Редактор GitHub

Кода получилось много — 1348 строк суммарно. Поэтому пока приложила полные файлы.

2. Что реализовано
Включение и выключение Auto Scroll Apps.

Выбор до пяти установленных приложений.

Проверка активного приложения каждые 500 мс.

Бесконечная прокрутка с паузой в одну секунду.

Остановка автоматической прокрутки при блокировке телефона.

Возврат статичной надписи при выходе из приложения.

Сохранение выбранных приложений.

3. Как тестируем
После замены трёх файлов и успешной сборки:

Устанавливаем APK.

Открываем Glyph Text.

Включаем Auto Scroll Apps.

Разрешаем доступ к истории использования.

Выбираем, например, Spotify.

Сохраняем настройки и переключаем Glyph Toy.

Открываем Spotify.

Надпись должна начать прокручиваться без нажатия кнопки.

Пока это экспериментальная сборка. Полную компиляцию Android я не запускала. Также Nothing OS может отключать наш Glyph Toy при включённом экране — это главный момент, который предстоит проверить. Список приложений пока текстовый, без иконок.

Переходим в GitHub Actions.

Погнали тестировать! 🔥


MainActivity.kt
Код

GlyphButtonDemoService.kt
Код

AndroidManifest.xml
Код




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

