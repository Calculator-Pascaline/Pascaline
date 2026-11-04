/*
 * Unitto is a calculator for Android
 * Copyright (c) 2022-2026 Elshan Agaev
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

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

/** Settings of the Android app, in a DataStore: each with its type, the same keys as before. */
class UserPreferencesRepositoryImpl(private val dataStore: DataStore<Preferences>) :
  SharedUserPreferencesRepository() {
  override val values: Flow<PreferenceValues> =
    dataStore.data
      .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
      .map { DataStoreValues(it) }

  override suspend fun edit(change: PreferenceEditor.() -> Unit) {
    dataStore.edit { preferences -> DataStoreEditor(preferences).change() }
  }
}

private open class DataStoreValues(private val preferences: Preferences) : PreferenceValues {
  override fun boolean(key: String) = preferences[booleanPreferencesKey(key)]

  override fun int(key: String) = preferences[intPreferencesKey(key)]

  override fun long(key: String) = preferences[longPreferencesKey(key)]

  override fun string(key: String) = preferences[stringPreferencesKey(key)]
}

private class DataStoreEditor(private val preferences: MutablePreferences) :
  DataStoreValues(preferences), PreferenceEditor {
  override fun set(key: String, value: Boolean) {
    preferences[booleanPreferencesKey(key)] = value
  }

  override fun set(key: String, value: Int) {
    preferences[intPreferencesKey(key)] = value
  }

  override fun set(key: String, value: Long) {
    preferences[longPreferencesKey(key)] = value
  }

  override fun set(key: String, value: String) {
    preferences[stringPreferencesKey(key)] = value
  }
}
