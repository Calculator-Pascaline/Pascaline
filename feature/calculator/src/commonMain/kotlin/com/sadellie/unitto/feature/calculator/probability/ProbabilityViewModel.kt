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

package com.sadellie.unitto.feature.calculator.probability

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sadellie.unitto.core.common.FormatterSymbols
import com.sadellie.unitto.core.common.KBigDecimal
import com.sadellie.unitto.core.common.KRoundingMode
import com.sadellie.unitto.core.common.Token
import com.sadellie.unitto.core.common.stateIn
import com.sadellie.unitto.core.common.trimZeros
import com.sadellie.unitto.core.datastore.UserPreferencesRepository
import kotlinx.coroutines.flow.map

/** What is calculated: P(X ≤ a), P(a ≤ X ≤ b), P(X ≥ a) or P(X = a). */
internal enum class CalculationType(val usesB: Boolean, val discreteOnly: Boolean) {
  AT_MOST(usesB = false, discreteOnly = false),
  BETWEEN(usesB = true, discreteOnly = false),
  AT_LEAST(usesB = false, discreteOnly = false),
  EXACTLY(usesB = false, discreteOnly = true),
}

internal data class NumberFormat(val formatterSymbols: FormatterSymbols, val precision: Int, val outputFormat: Int)

/**
 * Three steps like NumWorks: choose a distribution, set its parameters, then calculate
 * probabilities. Changing the probability of P(X ≤ a) or P(X ≥ a) finds a.
 */
internal class ProbabilityViewModel(userPrefsRepository: UserPreferencesRepository) : ViewModel() {
  val numberFormat =
    userPrefsRepository.calculatorPrefs
      .map { NumberFormat(it.formatterSymbols, it.precision, it.outputFormat) }
      .stateIn(viewModelScope, null)

  /** Chosen distribution, null on the first step. */
  var type by mutableStateOf<DistributionType?>(null)
    private set

  /** Parameters are set and probabilities are shown. */
  var isCalculating by mutableStateOf(false)
    private set

  val parameters = mutableStateListOf<String>()

  var calculation by mutableStateOf(CalculationType.AT_MOST)
    private set

  var a by mutableStateOf("")
    private set

  var b by mutableStateOf("")
    private set

  /** Probability typed by the user, null when it is calculated from the bounds. */
  var typedProbability by mutableStateOf<String?>(null)
    private set

  /** Distribution with the current parameters, null when they are not valid. */
  val distribution: Distribution?
    get() {
      val type = type ?: return null
      val values = parameters.map { it.toNumberOrNull() ?: return null }
      return type.create(values)
    }

  fun choose(type: DistributionType) {
    this.type = type
    parameters.clear()
    parameters.addAll(type.parameters.map { it.default })
    isCalculating = false
  }

  fun setParameter(index: Int, value: String) {
    parameters[index] = value
  }

  /** Goes to the calculation with bounds in the middle of the distribution. */
  fun calculate() {
    val distribution = distribution ?: return
    if (calculation.discreteOnly && !distribution.isDiscrete) calculation = CalculationType.AT_MOST
    a = distribution.quantile(LOWER_DEFAULT).toInput()
    b = distribution.quantile(UPPER_DEFAULT).toInput()
    typedProbability = null
    isCalculating = true
  }

  fun selectCalculation(type: CalculationType) {
    calculation = type
    typedProbability = null
  }

  fun updateA(value: String) {
    a = value
    typedProbability = null
  }

  fun updateB(value: String) {
    b = value
    typedProbability = null
  }

  /**
   * Finds a so that P(X ≤ a) or P(X ≥ a) is [value]. Discrete distributions only reach some
   * probabilities, so a is the closest bound that does not go over [value].
   */
  fun setProbability(value: String) {
    typedProbability = value
    val p = value.toNumberOrNull()?.takeIf { it in 0.0..1.0 } ?: return
    val distribution = distribution ?: return
    val bound =
      when (calculation) {
        CalculationType.AT_MOST -> if (distribution.isDiscrete) distribution.largestAtMost(p) else distribution.quantile(p)
        // P(X ≥ a) = 1 − P(X ≤ a − 1) for discrete
        CalculationType.AT_LEAST ->
          if (distribution.isDiscrete) {
            (distribution.quantile(1 - p) + 1).coerceAtMost(distribution.support.endInclusive)
          } else {
            distribution.quantile(1 - p)
          }
        else -> return
      }
    if (bound.isFinite()) a = bound.toInput()
  }

  /** Done typing the probability: discrete ones show the probability that a really gives. */
  fun finishProbability() {
    if (distribution?.isDiscrete == true) typedProbability = null
  }

  /** One step back, false when already on the first one. */
  fun back(): Boolean {
    when {
      isCalculating -> isCalculating = false
      type != null -> type = null
      else -> return false
    }
    return true
  }

  /** Probability of the current calculation, NaN when bounds are not numbers. */
  fun probability(distribution: Distribution): Double {
    val a = a.toNumberOrNull() ?: return Double.NaN
    return when (calculation) {
      CalculationType.AT_MOST -> distribution.atMost(a)
      CalculationType.AT_LEAST -> distribution.atLeast(a)
      CalculationType.EXACTLY -> distribution.exactly(a)
      CalculationType.BETWEEN -> distribution.between(a, b.toNumberOrNull() ?: return Double.NaN)
    }
  }
}

/** Accepts −, comma and dot: `−2,5` ==> −2.5. */
internal fun String.toNumberOrNull(): Double? =
  replace(Token.Operator.MINUS, "-").replace(",", ".").trim().toDoubleOrNull()

/** Bound as it is typed: 1.96, −3. */
private fun Double.toInput(): String = rounded(INPUT_DECIMALS)

/**
 * Rounded to [decimals]: tiny errors like −7×10⁻¹⁷ (instead of 0) are not shown, which
 * [toFormattedString] would do for small numbers.
 */
internal fun Double.rounded(decimals: Int): String =
  KBigDecimal.valueOf(this)
    .setScale(decimals, KRoundingMode.HALF_EVEN)
    .trimZeros()
    .toPlainString()
    .replace("-", Token.Operator.MINUS)

// Default bounds: around the middle, like NumWorks
private const val LOWER_DEFAULT = 0.5
private const val UPPER_DEFAULT = 0.84
private const val INPUT_DECIMALS = 3
