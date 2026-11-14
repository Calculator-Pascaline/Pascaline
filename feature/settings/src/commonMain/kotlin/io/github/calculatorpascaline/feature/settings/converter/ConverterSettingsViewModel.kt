/*
 * Unitto is a calculator for Android
 * Copyright (c) 2023-2025 Elshan Agaev
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

package io.github.calculatorpascaline.feature.settings.converter

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.calculatorpascaline.core.common.stateIn
import io.github.calculatorpascaline.core.datastore.UserPreferencesRepository
import io.github.calculatorpascaline.core.model.converter.UnitsListSorting
import kotlinx.coroutines.launch

internal class ConverterSettingsViewModel(
  private val userPrefsRepository: UserPreferencesRepository
) : ViewModel() {
  val prefs = userPrefsRepository.converterPrefs.stateIn(viewModelScope, null)

  fun updateUnitConverterFormatTime(enabled: Boolean) =
    viewModelScope.launch { userPrefsRepository.updateUnitConverterFormatTime(enabled) }

  fun updateUnitConverterSorting(sorting: UnitsListSorting) =
    viewModelScope.launch { userPrefsRepository.updateUnitConverterSorting(sorting) }

  fun updateUnitConverterShowIcons(enabled: Boolean) =
    viewModelScope.launch { userPrefsRepository.updateUnitConverterShowIcons(enabled) }
}
