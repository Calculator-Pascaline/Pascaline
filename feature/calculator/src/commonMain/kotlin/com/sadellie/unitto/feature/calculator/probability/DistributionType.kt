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

import kotlin.math.floor
import org.jetbrains.compose.resources.StringResource
import unitto.core.common.generated.resources.Res
import unitto.core.common.generated.resources.probability_binomial
import unitto.core.common.generated.resources.probability_chi_squared
import unitto.core.common.generated.resources.probability_deviation
import unitto.core.common.generated.resources.probability_draws
import unitto.core.common.generated.resources.probability_exponential
import unitto.core.common.generated.resources.probability_fisher
import unitto.core.common.generated.resources.probability_freedom
import unitto.core.common.generated.resources.probability_freedom_denominator
import unitto.core.common.generated.resources.probability_freedom_numerator
import unitto.core.common.generated.resources.probability_geometric
import unitto.core.common.generated.resources.probability_hypergeometric
import unitto.core.common.generated.resources.probability_lower_bound
import unitto.core.common.generated.resources.probability_mean
import unitto.core.common.generated.resources.probability_normal
import unitto.core.common.generated.resources.probability_poisson
import unitto.core.common.generated.resources.probability_population
import unitto.core.common.generated.resources.probability_population_successes
import unitto.core.common.generated.resources.probability_rate
import unitto.core.common.generated.resources.probability_student
import unitto.core.common.generated.resources.probability_success
import unitto.core.common.generated.resources.probability_trials
import unitto.core.common.generated.resources.probability_uniform
import unitto.core.common.generated.resources.probability_upper_bound

/**
 * Parameter of a distribution.
 *
 * @property isValid Check of this parameter alone, others are checked in [DistributionType.create].
 */
internal class Parameter(
  val symbol: String,
  val description: StringResource,
  val default: String,
  val isValid: (Double) -> Boolean,
)

/** Distributions of the NumWorks Probability app, in the same order. */
internal enum class DistributionType(
  val title: StringResource,
  val parameters: List<Parameter>,
  private val factory: (List<Double>) -> Distribution?,
) {
  BINOMIAL(
    Res.string.probability_binomial,
    listOf(
      Parameter("n", Res.string.probability_trials, "20") { it.isWhole() && it in 0.0..MAX_TRIALS },
      Parameter("p", Res.string.probability_success, "0.5") { it in 0.0..1.0 },
    ),
    { (n, p) -> Binomial(n.toInt(), p) },
  ),
  UNIFORM(
    Res.string.probability_uniform,
    listOf(
      Parameter("a", Res.string.probability_lower_bound, "0") { true },
      Parameter("b", Res.string.probability_upper_bound, "1") { true },
    ),
    { (a, b) -> if (a < b) Uniform(a, b) else null },
  ),
  EXPONENTIAL(
    Res.string.probability_exponential,
    listOf(Parameter("λ", Res.string.probability_rate, "1") { it > 0 }),
    { (lambda) -> Exponential(lambda) },
  ),
  NORMAL(
    Res.string.probability_normal,
    listOf(
      Parameter("μ", Res.string.probability_mean, "0") { true },
      Parameter("σ", Res.string.probability_deviation, "1") { it > 0 },
    ),
    { (mean, sigma) -> Normal(mean, sigma) },
  ),
  CHI_SQUARED(
    Res.string.probability_chi_squared,
    listOf(Parameter("k", Res.string.probability_freedom, "1") { it > 0 }),
    { (k) -> ChiSquared(k) },
  ),
  STUDENT(
    Res.string.probability_student,
    listOf(Parameter("k", Res.string.probability_freedom, "1") { it > 0 }),
    { (k) -> Student(k) },
  ),
  GEOMETRIC(
    Res.string.probability_geometric,
    listOf(Parameter("p", Res.string.probability_success, "0.5") { it > 0 && it <= 1 }),
    { (p) -> Geometric(p) },
  ),
  POISSON(
    Res.string.probability_poisson,
    listOf(Parameter("λ", Res.string.probability_mean, "4") { it > 0 && it <= MAX_TRIALS }),
    { (lambda) -> Poisson(lambda) },
  ),
  FISHER(
    Res.string.probability_fisher,
    listOf(
      Parameter("d₁", Res.string.probability_freedom_numerator, "1") { it > 0 },
      Parameter("d₂", Res.string.probability_freedom_denominator, "1") { it > 0 },
    ),
    { (d1, d2) -> Fisher(d1, d2) },
  ),
  HYPERGEOMETRIC(
    Res.string.probability_hypergeometric,
    listOf(
      Parameter("N", Res.string.probability_population, "100") { it.isWhole() && it in 0.0..MAX_TRIALS },
      Parameter("K", Res.string.probability_population_successes, "60") { it.isWhole() && it >= 0 },
      Parameter("n", Res.string.probability_draws, "50") { it.isWhole() && it >= 0 },
    ),
    { (total, successes, n) ->
      if (successes <= total && n <= total) Hypergeometric(total.toInt(), successes.toInt(), n.toInt()) else null
    },
  );

  /** Distribution with these parameter values, null when they don't fit together. */
  fun create(values: List<Double>): Distribution? {
    if (values.size != parameters.size) return null
    if (parameters.zip(values).any { (parameter, value) -> !value.isFinite() || !parameter.isValid(value) }) {
      return null
    }
    return factory(values)
  }
}

private fun Double.isWhole() = this == floor(this)

private const val MAX_TRIALS = 100_000.0
