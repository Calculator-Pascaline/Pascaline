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

package com.sadellie.unitto.feature.calculator.finance

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sadellie.unitto.core.common.stateIn
import com.sadellie.unitto.core.datastore.UserPreferencesRepository
import com.sadellie.unitto.feature.calculator.datatable.NumberFormat
import com.sadellie.unitto.feature.calculator.probability.toNumberOrNull
import kotlinx.coroutines.flow.map

/** Simple or compound interest, the value to find and the others. Kept while the app is open. */
internal class FinanceViewModel(userPrefsRepository: UserPreferencesRepository) : ViewModel() {
  val numberFormat =
    userPrefsRepository.calculatorPrefs
      .map { NumberFormat(it.formatterSymbols, it.precision, it.outputFormat) }
      .stateIn(viewModelScope, null)

  var isCompound by mutableStateOf(false)

  var simpleUnknown by mutableStateOf(SimpleValue.INTEREST)

  var compoundUnknown by mutableStateOf(CompoundValue.PAYMENT)

  val simpleValues = mutableStateMapOf<SimpleValue, String>()

  val compoundValues = mutableStateMapOf<CompoundValue, String>()

  var yearDays by mutableStateOf(DAYS_365)

  var paymentsPerYear by mutableStateOf("12")

  var compoundingsPerYear by mutableStateOf("12")

  var paysAtStart by mutableStateOf(false)

  /** The unknown value, null until all the others are typed. */
  fun simpleResult(): Double? {
    val known = SimpleValue.entries.filter { it != simpleUnknown }.associateWith { simpleValues[it]?.toNumberOrNull() ?: return null }
    return simpleInterest(known, simpleUnknown, yearDays)
  }

  fun compoundResult(): Double? {
    val known =
      CompoundValue.entries.filter { it != compoundUnknown }.associateWith { compoundValues[it]?.toNumberOrNull() ?: return null }
    val settings =
      CompoundSettings(
        paymentsPerYear = paymentsPerYear.toNumberOrNull() ?: return null,
        compoundingsPerYear = compoundingsPerYear.toNumberOrNull() ?: return null,
        paysAtStart = paysAtStart,
      )
    return compoundInterest(known, compoundUnknown, settings)
  }
}

internal const val DAYS_360 = 360
internal const val DAYS_365 = 365
