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

package com.sadellie.unitto.feature.calculator.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sadellie.unitto.core.common.stateIn
import com.sadellie.unitto.core.datastore.UserPreferencesRepository
import com.sadellie.unitto.feature.calculator.datatable.NumberFormat
import com.sadellie.unitto.feature.calculator.datatable.SavedTable
import com.sadellie.unitto.feature.calculator.probability.toNumberOrNull
import kotlinx.coroutines.flow.map

/** One list of values with their frequencies, like the NumWorks Statistics app. */
internal class StatisticsViewModel(userPrefsRepository: UserPreferencesRepository) : ViewModel() {
  val numberFormat =
    userPrefsRepository.calculatorPrefs
      .map { NumberFormat(it.formatterSymbols, it.precision, it.outputFormat) }
      .stateIn(viewModelScope, null)

  /** Values and frequencies. */
  val table = SavedTable(DATA_KEY, columns = 2, userPrefsRepository, viewModelScope)
}

/** Rows with a number, frequency is 1 when it is empty. Rows with a wrong cell are left out. */
internal fun List<List<String>>.weightedValues(): List<WeightedValue> = mapNotNull { cells ->
  val value = cells.getOrNull(0)?.toNumberOrNull() ?: return@mapNotNull null
  val frequencyText = cells.getOrNull(1).orEmpty()
  val frequency = if (frequencyText.isBlank()) 1.0 else frequencyText.toNumberOrNull() ?: return@mapNotNull null
  if (frequency < 0) null else WeightedValue(value, frequency)
}

private const val DATA_KEY = "STATISTICS_DATA"
