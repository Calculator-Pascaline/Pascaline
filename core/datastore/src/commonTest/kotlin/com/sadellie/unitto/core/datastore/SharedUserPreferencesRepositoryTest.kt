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

package com.sadellie.unitto.core.datastore

import com.sadellie.unitto.core.datastore.UserPreferencesRepository.Defaults
import com.sadellie.unitto.core.model.converter.UnitGroup
import io.github.sadellie.themmo.core.ThemingMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest

/** The settings shared by Android and the web, on a store kept in memory. */
class SharedUserPreferencesRepositoryTest {
  private class InMemory : SharedUserPreferencesRepository() {
    val data = MutableStateFlow<Map<String, Any>>(emptyMap())

    override val values: Flow<PreferenceValues> = data.map { Values(it) }

    override suspend fun edit(change: PreferenceEditor.() -> Unit) {
      data.value = Editor(data.value.toMutableMap()).apply(change).map
    }

    private open class Values(private val map: Map<String, Any>) : PreferenceValues {
      override fun boolean(key: String) = map[key] as? Boolean

      override fun int(key: String) = map[key] as? Int

      override fun long(key: String) = map[key] as? Long

      override fun string(key: String) = map[key] as? String
    }

    private class Editor(val map: MutableMap<String, Any>) : Values(map), PreferenceEditor {
      override fun set(key: String, value: Boolean) = map.set(key, value)

      override fun set(key: String, value: Int) = map.set(key, value)

      override fun set(key: String, value: Long) = map.set(key, value)

      override fun set(key: String, value: String) = map.set(key, value)
    }
  }

  @Test
  fun defaultsWhenNothingIsSaved() = runTest {
    val prefs = InMemory().calculatorPrefs.first()
    assertEquals(Defaults.digitsPrecision, prefs.precision)
    assertEquals(Defaults.formatterSymbols, prefs.formatterSymbols)
  }

  @Test
  fun savedValueIsRead() = runTest {
    val repository = InMemory()
    repository.updateThemingMode(ThemingMode.FORCE_DARK)
    repository.updateDigitsPrecision(7)
    assertEquals(ThemingMode.FORCE_DARK, repository.appPrefs.first().themingMode)
    assertEquals(7, repository.formattingPrefs.first().digitsPrecision)
  }

  // A broken value (an old app version, another type) falls back to the default
  @Test
  fun brokenValueFallsBack() = runTest {
    val repository = InMemory()
    repository.data.value = mapOf(PrefKeys.THEMING_MODE_PREF_KEY to "NOT_A_MODE")
    assertEquals(Defaults.themingMode, repository.appPrefs.first().themingMode)
  }

  @Test
  fun sameGroupingAndFractionalAreIgnored() = runTest {
    val repository = InMemory()
    repository.updateFormatterSymbols(grouping = ",", fractional = ",", indian = false)
    assertEquals(Defaults.formatterSymbols, repository.calculatorPrefs.first().formatterSymbols)
  }

  @Test
  fun unitGroupsAddedAndRemoved() = runTest {
    val repository = InMemory()
    repository.updateShownUnitGroups(listOf(UnitGroup.LENGTH))
    repository.addShownUnitGroup(UnitGroup.MASS)
    assertEquals(listOf(UnitGroup.LENGTH, UnitGroup.MASS), repository.unitGroupsPrefs.first().shownUnitGroups)
    repository.removeShownUnitGroup(UnitGroup.LENGTH)
    assertEquals(listOf(UnitGroup.MASS), repository.unitGroupsPrefs.first().shownUnitGroups)
  }
}
