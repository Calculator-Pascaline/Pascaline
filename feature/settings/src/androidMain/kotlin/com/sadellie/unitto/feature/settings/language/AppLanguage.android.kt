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

package com.sadellie.unitto.feature.settings.language

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

internal actual object AppLanguage {
  actual fun current(): String = AppCompatDelegate.getApplicationLocales().toLanguageTags()

  actual fun set(languageTag: String) {
    val selectedLocale =
      if (languageTag == "") {
        LocaleListCompat.getEmptyLocaleList()
      } else {
        LocaleListCompat.forLanguageTags(languageTag)
      }
    AppCompatDelegate.setApplicationLocales(selectedLocale)
  }
}
