/*
 * Pascaline is a calculator for Android
 * Copyright (c) 2026 Calculator-Pascaline
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.sadellie.unitto.core.common

/**
 * The language chosen in the web app's settings. The texts, numbers and dates follow the browser's
 * language: [apply] makes the browser report the chosen one, before the app starts.
 */
object WebLanguage {
  /** Language tag, empty when the app follows the browser. */
  fun saved(): String = WebStorage.get(KEY) ?: ""

  fun save(languageTag: String) = WebStorage.set(KEY, languageTag)

  fun apply() {
    val languageTag = saved()
    if (languageTag.isNotEmpty()) setBrowserLanguage(languageTag)
  }

  private const val KEY = "language"
}

@JsFun(
  """(tag) => {
  try {
    Object.defineProperty(navigator, 'language', { get: () => tag, configurable: true });
    Object.defineProperty(navigator, 'languages', { get: () => [tag], configurable: true });
  } catch (e) {}
}"""
)
private external fun setBrowserLanguage(tag: String)
