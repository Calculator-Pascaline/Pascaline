/*
 * Unitto is a calculator for Android
 * Copyright (c) 2025-2026 Elshan Agaev
 * Copyright (c) 2026 Calculator-Pascaline (modifications)
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

package com.sadellie.unitto.core.datastore

import com.sadellie.unitto.core.common.WebStorage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/** Settings of the web app, saved in the browser. */
class UserPreferencesRepositoryImpl : SharedUserPreferencesRepository() {
  private val data = MutableStateFlow(loadPreferences())

  override val values: Flow<PreferenceValues> = data.map { MapValues(it) }

  override suspend fun edit(change: PreferenceEditor.() -> Unit) {
    data.update { old -> MapEditor(old.toMutableMap()).apply(change).preferences }
    savePreferences(data.value)
  }

  // A browser has no vibrations, can't keep the screen on and has no tools experiment
  override fun PreferenceValues.enableVibrations() = false

  override fun PreferenceValues.enableKeepScreenOn() = false

  override fun PreferenceValues.enableToolsExperiment() = false
}

private open class MapValues(private val preferences: Preferences) : PreferenceValues {
  override fun boolean(key: String) = preferences[key] as? Boolean

  override fun int(key: String) = preferences[key] as? Int

  override fun long(key: String) = preferences[key] as? Long

  override fun string(key: String) = preferences[key] as? String
}

private class MapEditor(val preferences: MutableMap<String, Any>) : MapValues(preferences), PreferenceEditor {
  override fun set(key: String, value: Boolean) {
    preferences[key] = value
  }

  override fun set(key: String, value: Int) {
    preferences[key] = value
  }

  override fun set(key: String, value: Long) {
    preferences[key] = value
  }

  override fun set(key: String, value: String) {
    preferences[key] = value
  }
}
/** Each setting is saved as key, type and value: Boolean, Int, Long or String. */
private fun savePreferences(preferences: Preferences) {
  val fields =
    preferences.flatMap { (key, value) ->
      val type =
        when (value) {
          is Boolean -> TYPE_BOOLEAN
          is Int -> TYPE_INT
          is Long -> TYPE_LONG
          else -> TYPE_STRING
        }
      listOf(key, type, value.toString())
    }
  WebStorage.set(PREFERENCES_KEY, WebStorage.encode(fields))
}

private fun loadPreferences(): Preferences =
  WebStorage.decode(WebStorage.get(PREFERENCES_KEY) ?: "")
    .chunked(PREFERENCE_FIELDS)
    .mapNotNull { fields ->
      if (fields.size != PREFERENCE_FIELDS) return@mapNotNull null
      val (key, type, text) = fields
      val value: Any =
        when (type) {
          TYPE_BOOLEAN -> text == "true"
          TYPE_INT -> text.toIntOrNull() ?: return@mapNotNull null
          TYPE_LONG -> text.toLongOrNull() ?: return@mapNotNull null
          else -> text
        }
      key to value
    }
    .toMap()

private const val PREFERENCES_KEY = "preferences"
private const val PREFERENCE_FIELDS = 3
private const val TYPE_BOOLEAN = "b"
private const val TYPE_INT = "i"
private const val TYPE_LONG = "l"
private const val TYPE_STRING = "s"

private typealias Preferences = Map<String, Any>
