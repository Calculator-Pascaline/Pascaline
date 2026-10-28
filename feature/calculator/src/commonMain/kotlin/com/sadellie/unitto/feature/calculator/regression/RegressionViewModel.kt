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

package com.sadellie.unitto.feature.calculator.regression

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sadellie.unitto.core.common.stateIn
import com.sadellie.unitto.core.datastore.UserPreferencesRepository
import com.sadellie.unitto.feature.calculator.datatable.NumberFormat
import com.sadellie.unitto.feature.calculator.datatable.SavedTable
import com.sadellie.unitto.feature.calculator.probability.toNumberOrNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/** Points and the kind of curve that fits them, like the NumWorks Regression app. */
internal class RegressionViewModel(private val userPrefsRepository: UserPreferencesRepository) : ViewModel() {
  val numberFormat =
    userPrefsRepository.calculatorPrefs
      .map { NumberFormat(it.formatterSymbols, it.precision, it.outputFormat) }
      .stateIn(viewModelScope, null)

  /** x and y of the points. */
  val table = SavedTable(DATA_KEY, columns = 2, userPrefsRepository, viewModelScope)

  var model by mutableStateOf(RegressionModel.LINEAR)
    private set

  init {
    viewModelScope.launch {
      val saved = userPrefsRepository.appData(MODEL_KEY).first()
      model = RegressionModel.entries.firstOrNull { it.name == saved } ?: RegressionModel.LINEAR
    }
  }

  fun selectModel(model: RegressionModel) {
    this.model = model
    viewModelScope.launch { userPrefsRepository.updateAppData(MODEL_KEY, model.name) }
  }
}

/** Rows where both x and y are numbers. */
internal fun List<List<String>>.dataPoints(): List<DataPoint> = mapNotNull { cells ->
  val x = cells.getOrNull(0)?.toNumberOrNull() ?: return@mapNotNull null
  val y = cells.getOrNull(1)?.toNumberOrNull() ?: return@mapNotNull null
  DataPoint(x, y)
}

private const val DATA_KEY = "REGRESSION_DATA"
private const val MODEL_KEY = "REGRESSION_MODEL"
