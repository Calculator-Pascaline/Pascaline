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

import com.sadellie.unitto.feature.calculator.probability.ChiSquared
import com.sadellie.unitto.feature.calculator.probability.Distribution
import com.sadellie.unitto.feature.calculator.probability.Normal
import com.sadellie.unitto.feature.calculator.probability.Student
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

/** Alternative hypothesis Hₐ, compared to the value of H₀. */
internal enum class Alternative(val symbol: String) {
  NOT_EQUAL("≠"),
  LESS("<"),
  GREATER(">"),
}

/**
 * Result of a significance test.
 *
 * @property degreesOfFreedom null for z tests.
 */
internal class TestResult(
  val statistic: Double,
  val pValue: Double,
  val degreesOfFreedom: Double?,
) {
  /** H₀ is rejected at significance [alpha] when the p-value is below it. */
  fun rejects(alpha: Double): Boolean = pValue < alpha
}

/** Confidence interval: [estimate] ± [marginOfError]. */
internal class IntervalResult(
  val estimate: Double,
  val criticalValue: Double,
  val standardError: Double,
  val degreesOfFreedom: Double?,
) {
  val marginOfError: Double
    get() = criticalValue * standardError

  val low: Double
    get() = estimate - marginOfError

  val high: Double
    get() = estimate + marginOfError
}

private val standardNormal = Normal(0.0, 1.0)

/** Probability of a statistic at least as extreme as [statistic] if H₀ is true. */
private fun pValue(distribution: Distribution, statistic: Double, alternative: Alternative): Double =
  when (alternative) {
    Alternative.LESS -> distribution.cdf(statistic)
    Alternative.GREATER -> 1 - distribution.cdf(statistic)
    Alternative.NOT_EQUAL -> (2 * min(distribution.cdf(statistic), 1 - distribution.cdf(statistic))).coerceAtMost(1.0)
  }

private fun zTest(statistic: Double, alternative: Alternative): TestResult? {
  if (!statistic.isFinite()) return null
  return TestResult(statistic, pValue(standardNormal, statistic, alternative), null)
}

private fun tTest(statistic: Double, degreesOfFreedom: Double, alternative: Alternative): TestResult? {
  if (!statistic.isFinite() || !(degreesOfFreedom > 0)) return null
  return TestResult(statistic, pValue(Student(degreesOfFreedom), statistic, alternative), degreesOfFreedom)
}

/** H₀: p = [p0], with [successes] out of [n]. */
internal fun oneProportionTest(p0: Double, successes: Double, n: Double, alternative: Alternative): TestResult? {
  if (n <= 0 || successes !in 0.0..n || p0 <= 0 || p0 >= 1) return null
  return zTest((successes / n - p0) / sqrt(p0 * (1 - p0) / n), alternative)
}

/** H₀: p₁ = p₂, with the pooled proportion. */
internal fun twoProportionsTest(
  successes1: Double,
  n1: Double,
  successes2: Double,
  n2: Double,
  alternative: Alternative,
): TestResult? {
  if (n1 <= 0 || n2 <= 0 || successes1 !in 0.0..n1 || successes2 !in 0.0..n2) return null
  val pooled = (successes1 + successes2) / (n1 + n2)
  val error = sqrt(pooled * (1 - pooled) * (1 / n1 + 1 / n2))
  return zTest((successes1 / n1 - successes2 / n2) / error, alternative)
}

/** H₀: μ = [mu0], σ is not known: Student's t with n − 1 degrees of freedom. */
internal fun oneMeanTTest(mu0: Double, mean: Double, deviation: Double, n: Double, alternative: Alternative): TestResult? {
  if (n < 2 || deviation <= 0) return null
  return tTest((mean - mu0) / (deviation / sqrt(n)), n - 1, alternative)
}

/** H₀: μ = [mu0], σ is known. */
internal fun oneMeanZTest(mu0: Double, mean: Double, sigma: Double, n: Double, alternative: Alternative): TestResult? {
  if (n <= 0 || sigma <= 0) return null
  return zTest((mean - mu0) / (sigma / sqrt(n)), alternative)
}

/** H₀: μ₁ = μ₂, σ not known and not supposed equal (Welch). */
internal fun twoMeansTTest(
  mean1: Double,
  deviation1: Double,
  n1: Double,
  mean2: Double,
  deviation2: Double,
  n2: Double,
  alternative: Alternative,
): TestResult? {
  val welch = welch(deviation1, n1, deviation2, n2) ?: return null
  return tTest((mean1 - mean2) / welch.first, welch.second, alternative)
}

/** H₀: μ₁ = μ₂, σ₁ and σ₂ are known. */
internal fun twoMeansZTest(
  mean1: Double,
  sigma1: Double,
  n1: Double,
  mean2: Double,
  sigma2: Double,
  n2: Double,
  alternative: Alternative,
): TestResult? {
  if (n1 <= 0 || n2 <= 0 || sigma1 <= 0 || sigma2 <= 0) return null
  return zTest((mean1 - mean2) / sqrt(sigma1 * sigma1 / n1 + sigma2 * sigma2 / n2), alternative)
}

/**
 * H₀: [observed] follows [expected]. χ² with k − 1 degrees of freedom, only large values are
 * against H₀.
 */
internal fun goodnessOfFitTest(observed: List<Double>, expected: List<Double>): TestResult? {
  if (observed.size != expected.size || observed.size < 2 || expected.any { it <= 0 }) return null
  val statistic = observed.zip(expected).sumOf { (o, e) -> (o - e).pow(2) / e }
  val degreesOfFreedom = observed.size - 1.0
  return TestResult(statistic, 1 - ChiSquared(degreesOfFreedom).cdf(statistic), degreesOfFreedom)
}

/** Standard error and Welch–Satterthwaite degrees of freedom. */
private fun welch(deviation1: Double, n1: Double, deviation2: Double, n2: Double): Pair<Double, Double>? {
  if (n1 < 2 || n2 < 2 || deviation1 <= 0 || deviation2 <= 0) return null
  val a = deviation1 * deviation1 / n1
  val b = deviation2 * deviation2 / n2
  val degreesOfFreedom = (a + b).pow(2) / (a * a / (n1 - 1) + b * b / (n2 - 1))
  return sqrt(a + b) to degreesOfFreedom
}

/** z* or t* that leaves (1 − [level]) / 2 in each tail. */
private fun critical(distribution: Distribution, level: Double) = distribution.quantile((1 + level) / 2)

private fun validLevel(level: Double) = level > 0 && level < 1

internal fun oneProportionInterval(successes: Double, n: Double, level: Double): IntervalResult? {
  if (n <= 0 || successes !in 0.0..n || !validLevel(level)) return null
  val p = successes / n
  return IntervalResult(p, critical(standardNormal, level), sqrt(p * (1 - p) / n), null)
}

internal fun twoProportionsInterval(
  successes1: Double,
  n1: Double,
  successes2: Double,
  n2: Double,
  level: Double,
): IntervalResult? {
  if (n1 <= 0 || n2 <= 0 || successes1 !in 0.0..n1 || successes2 !in 0.0..n2 || !validLevel(level)) return null
  val p1 = successes1 / n1
  val p2 = successes2 / n2
  val error = sqrt(p1 * (1 - p1) / n1 + p2 * (1 - p2) / n2)
  return IntervalResult(p1 - p2, critical(standardNormal, level), error, null)
}

internal fun oneMeanTInterval(mean: Double, deviation: Double, n: Double, level: Double): IntervalResult? {
  if (n < 2 || deviation <= 0 || !validLevel(level)) return null
  return IntervalResult(mean, critical(Student(n - 1), level), deviation / sqrt(n), n - 1)
}

internal fun oneMeanZInterval(mean: Double, sigma: Double, n: Double, level: Double): IntervalResult? {
  if (n <= 0 || sigma <= 0 || !validLevel(level)) return null
  return IntervalResult(mean, critical(standardNormal, level), sigma / sqrt(n), null)
}

internal fun twoMeansTInterval(
  mean1: Double,
  deviation1: Double,
  n1: Double,
  mean2: Double,
  deviation2: Double,
  n2: Double,
  level: Double,
): IntervalResult? {
  if (!validLevel(level)) return null
  val (error, degreesOfFreedom) = welch(deviation1, n1, deviation2, n2) ?: return null
  return IntervalResult(mean1 - mean2, critical(Student(degreesOfFreedom), level), error, degreesOfFreedom)
}

internal fun twoMeansZInterval(
  mean1: Double,
  sigma1: Double,
  n1: Double,
  mean2: Double,
  sigma2: Double,
  n2: Double,
  level: Double,
): IntervalResult? {
  if (n1 <= 0 || n2 <= 0 || sigma1 <= 0 || sigma2 <= 0 || !validLevel(level)) return null
  val error = sqrt(sigma1 * sigma1 / n1 + sigma2 * sigma2 / n2)
  return IntervalResult(mean1 - mean2, critical(standardNormal, level), error, null)
}
