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

package com.sadellie.unitto.feature.calculator.inference

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sadellie.unitto.core.common.stateIn
import com.sadellie.unitto.core.datastore.UserPreferencesRepository
import com.sadellie.unitto.feature.calculator.datatable.NumberFormat
import com.sadellie.unitto.feature.calculator.datatable.SavedTable
import com.sadellie.unitto.feature.calculator.datatable.column
import com.sadellie.unitto.feature.calculator.probability.toNumberOrNull
import kotlinx.coroutines.flow.map

/** Value typed for a test or an interval. */
internal enum class InferenceField(val symbol: String) {
  P0("p₀"),
  MU0("μ₀"),
  SUCCESSES("x"),
  N("n"),
  MEAN("x̄"),
  DEVIATION("s"),
  SIGMA("σ"),
  SUCCESSES_1("x₁"),
  N_1("n₁"),
  SUCCESSES_2("x₂"),
  N_2("n₂"),
  MEAN_1("x̄₁"),
  DEVIATION_1("s₁"),
  SIGMA_1("σ₁"),
  MEAN_2("x̄₂"),
  DEVIATION_2("s₂"),
  SIGMA_2("σ₂"),
}

/**
 * Tests and intervals of the NumWorks Inference app.
 *
 * @property hypothesis H₀ written with its value: `p = p₀`.
 * @property testFields Fields of the test, [intervalFields] of the interval (null when there is none).
 */
internal enum class InferenceKind(
  val hypothesis: String,
  val testFields: List<InferenceField>,
  val intervalFields: List<InferenceField>?,
) {
  ONE_PROPORTION(
    "p = p₀",
    listOf(InferenceField.P0, InferenceField.SUCCESSES, InferenceField.N),
    listOf(InferenceField.SUCCESSES, InferenceField.N),
  ),
  TWO_PROPORTIONS(
    "p₁ = p₂",
    listOf(InferenceField.SUCCESSES_1, InferenceField.N_1, InferenceField.SUCCESSES_2, InferenceField.N_2),
    listOf(InferenceField.SUCCESSES_1, InferenceField.N_1, InferenceField.SUCCESSES_2, InferenceField.N_2),
  ),
  ONE_MEAN_T(
    "μ = μ₀",
    listOf(InferenceField.MU0, InferenceField.MEAN, InferenceField.DEVIATION, InferenceField.N),
    listOf(InferenceField.MEAN, InferenceField.DEVIATION, InferenceField.N),
  ),
  ONE_MEAN_Z(
    "μ = μ₀",
    listOf(InferenceField.MU0, InferenceField.MEAN, InferenceField.SIGMA, InferenceField.N),
    listOf(InferenceField.MEAN, InferenceField.SIGMA, InferenceField.N),
  ),
  TWO_MEANS_T(
    "μ₁ = μ₂",
    listOf(
      InferenceField.MEAN_1, InferenceField.DEVIATION_1, InferenceField.N_1,
      InferenceField.MEAN_2, InferenceField.DEVIATION_2, InferenceField.N_2,
    ),
    listOf(
      InferenceField.MEAN_1, InferenceField.DEVIATION_1, InferenceField.N_1,
      InferenceField.MEAN_2, InferenceField.DEVIATION_2, InferenceField.N_2,
    ),
  ),
  TWO_MEANS_Z(
    "μ₁ = μ₂",
    listOf(InferenceField.MEAN_1, InferenceField.SIGMA_1, InferenceField.N_1, InferenceField.MEAN_2, InferenceField.SIGMA_2, InferenceField.N_2),
    listOf(InferenceField.MEAN_1, InferenceField.SIGMA_1, InferenceField.N_1, InferenceField.MEAN_2, InferenceField.SIGMA_2, InferenceField.N_2),
  ),
  GOODNESS_OF_FIT("", emptyList(), null),
}

/** Test or interval, its values and its result. Values are kept while the app is open. */
internal class InferenceViewModel(userPrefsRepository: UserPreferencesRepository) : ViewModel() {
  val numberFormat =
    userPrefsRepository.calculatorPrefs
      .map { NumberFormat(it.formatterSymbols, it.precision, it.outputFormat) }
      .stateIn(viewModelScope, null)

  /** Test when true, confidence interval when false. */
  var isTest by mutableStateOf(true)

  var kind by mutableStateOf(InferenceKind.ONE_PROPORTION)
    private set

  var alternative by mutableStateOf(Alternative.NOT_EQUAL)

  var significance by mutableStateOf("0.05")

  var confidence by mutableStateOf("0.95")

  /** Values of every field, shared by tests and intervals: x̄ typed for one is there for the other. */
  val values = mutableStateMapOf<InferenceField, String>()

  /** Observed and expected counts of the goodness of fit test. */
  val goodnessOfFit = SavedTable(GOODNESS_OF_FIT_KEY, columns = 2, userPrefsRepository, viewModelScope)

  fun select(kind: InferenceKind) {
    this.kind = kind
  }

  fun selectTest(isTest: Boolean) {
    this.isTest = isTest
    // Goodness of fit has no interval
    if (!isTest && kind.intervalFields == null) kind = InferenceKind.ONE_PROPORTION
  }

  /** Result of the test, null until every value is typed and valid. */
  fun test(): TestResult? {
    if (kind == InferenceKind.GOODNESS_OF_FIT) {
      val rows = goodnessOfFit.rows ?: return null
      val observed = rows.column(0)
      val expected = rows.column(1)
      if (observed.any { it == null } || expected.any { it == null }) return null
      return goodnessOfFitTest(observed.filterNotNull(), expected.filterNotNull())
    }
    val v = numbers(kind.testFields) ?: return null
    return when (kind) {
      InferenceKind.ONE_PROPORTION -> oneProportionTest(v[0], v[1], v[2], alternative)
      InferenceKind.TWO_PROPORTIONS -> twoProportionsTest(v[0], v[1], v[2], v[3], alternative)
      InferenceKind.ONE_MEAN_T -> oneMeanTTest(v[0], v[1], v[2], v[3], alternative)
      InferenceKind.ONE_MEAN_Z -> oneMeanZTest(v[0], v[1], v[2], v[3], alternative)
      InferenceKind.TWO_MEANS_T -> twoMeansTTest(v[0], v[1], v[2], v[3], v[4], v[5], alternative)
      InferenceKind.TWO_MEANS_Z -> twoMeansZTest(v[0], v[1], v[2], v[3], v[4], v[5], alternative)
      InferenceKind.GOODNESS_OF_FIT -> null
    }
  }

  fun interval(): IntervalResult? {
    val fields = kind.intervalFields ?: return null
    val v = numbers(fields) ?: return null
    val level = confidence.toNumberOrNull() ?: return null
    return when (kind) {
      InferenceKind.ONE_PROPORTION -> oneProportionInterval(v[0], v[1], level)
      InferenceKind.TWO_PROPORTIONS -> twoProportionsInterval(v[0], v[1], v[2], v[3], level)
      InferenceKind.ONE_MEAN_T -> oneMeanTInterval(v[0], v[1], v[2], level)
      InferenceKind.ONE_MEAN_Z -> oneMeanZInterval(v[0], v[1], v[2], level)
      InferenceKind.TWO_MEANS_T -> twoMeansTInterval(v[0], v[1], v[2], v[3], v[4], v[5], level)
      InferenceKind.TWO_MEANS_Z -> twoMeansZInterval(v[0], v[1], v[2], v[3], v[4], v[5], level)
      InferenceKind.GOODNESS_OF_FIT -> null
    }
  }

  private fun numbers(fields: List<InferenceField>): List<Double>? = fields.map { values[it]?.toNumberOrNull() ?: return null }
}

private const val GOODNESS_OF_FIT_KEY = "INFERENCE_GOODNESS_OF_FIT"
