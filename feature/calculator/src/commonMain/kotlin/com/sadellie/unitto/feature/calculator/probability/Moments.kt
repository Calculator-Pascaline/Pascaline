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

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import kotlin.math.pow
import kotlin.math.sqrt
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import unitto.core.common.generated.resources.Res
import unitto.core.common.generated.resources.probability_chi_squared_mean
import unitto.core.common.generated.resources.probability_chi_squared_variance
import unitto.core.common.generated.resources.probability_exponential_mean
import unitto.core.common.generated.resources.probability_exponential_variance
import unitto.core.common.generated.resources.probability_fisher_mean
import unitto.core.common.generated.resources.probability_fisher_variance
import unitto.core.common.generated.resources.probability_geometric_mean
import unitto.core.common.generated.resources.probability_geometric_variance
import unitto.core.common.generated.resources.probability_hypergeometric_mean
import unitto.core.common.generated.resources.probability_hypergeometric_variance
import unitto.core.common.generated.resources.probability_normal_mean
import unitto.core.common.generated.resources.probability_normal_variance
import unitto.core.common.generated.resources.probability_poisson_mean
import unitto.core.common.generated.resources.probability_poisson_variance
import unitto.core.common.generated.resources.probability_student_mean
import unitto.core.common.generated.resources.probability_student_variance
import unitto.core.common.generated.resources.probability_undefined
import unitto.core.common.generated.resources.probability_uniform_mean
import unitto.core.common.generated.resources.probability_uniform_variance

/**
 * E(X) or V(X) of a distribution: `E(X) = (a + b) / 2 = (0 + 1) / 2 = 0.5`.
 *
 * @property formula With the parameter names, null when it is just a number.
 * @property substituted Formula with the entered values, null when it would repeat the value.
 * @property value Null when it does not exist, infinite when it is infinite.
 */
internal class Moment(
  val symbol: String,
  val formula: String?,
  val substituted: String?,
  val value: Double?,
  val explanation: StringResource,
)

internal class Moments(val expectedValue: Moment, val variance: Moment)

/** Expected value and variance for the entered [values], null for the binomial that has its own cards. */
internal fun DistributionType.moments(values: List<Double>, fractional: String): Moments? {
  // Negative numbers in parentheses: b − (−1)
  val term = { x: Double -> x.shown(fractional).let { if (x < 0) "($it)" else it } }
  return when (this) {
    DistributionType.BINOMIAL -> null
    DistributionType.UNIFORM -> uniformMoments(values, term)
    DistributionType.EXPONENTIAL -> exponentialMoments(values, term)
    DistributionType.NORMAL -> normalMoments(values, term)
    DistributionType.CHI_SQUARED -> chiSquaredMoments(values, term)
    DistributionType.STUDENT -> studentMoments(values, term)
    DistributionType.GEOMETRIC -> geometricMoments(values, term)
    DistributionType.POISSON -> poissonMoments(values)
    DistributionType.FISHER -> fisherMoments(values, term)
    DistributionType.HYPERGEOMETRIC -> hypergeometricMoments(values, term)
  }
}

private fun uniformMoments(values: List<Double>, term: (Double) -> String): Moments {
  val (a, b) = values
  return Moments(
    Moment("E(X)", "(a + b) / 2", "(${term(a)} + ${term(b)}) / 2", (a + b) / 2, Res.string.probability_uniform_mean),
    Moment(
      "V(X)",
      "(b − a)² / 12",
      "(${term(b)} − ${term(a)})² / 12",
      (b - a).pow(2) / UNIFORM_VARIANCE_DIVISOR,
      Res.string.probability_uniform_variance,
    ),
  )
}

private fun exponentialMoments(values: List<Double>, term: (Double) -> String): Moments {
  val (lambda) = values
  return Moments(
    Moment("E(X)", "1 / λ", "1 / ${term(lambda)}", 1 / lambda, Res.string.probability_exponential_mean),
    Moment(
      "V(X)",
      "1 / λ²",
      "1 / ${term(lambda)}²",
      1 / lambda.pow(2),
      Res.string.probability_exponential_variance,
    ),
  )
}

private fun normalMoments(values: List<Double>, term: (Double) -> String): Moments {
  val (mean, sigma) = values
  return Moments(
    Moment("E(X)", "μ", null, mean, Res.string.probability_normal_mean),
    Moment("V(X)", "σ²", "${term(sigma)}²", sigma.pow(2), Res.string.probability_normal_variance),
  )
}

private fun chiSquaredMoments(values: List<Double>, term: (Double) -> String): Moments {
  val (k) = values
  return Moments(
    Moment("E(X)", "k", null, k, Res.string.probability_chi_squared_mean),
    Moment("V(X)", "2k", "2 × ${term(k)}", 2 * k, Res.string.probability_chi_squared_variance),
  )
}

private fun studentMoments(values: List<Double>, term: (Double) -> String): Moments {
  val (k) = values
  // Heavy tails: no mean for k ≤ 1, infinite variance for 1 < k ≤ 2
  val variance =
    when {
      k > 2 -> k / (k - 2)
      k > 1 -> Double.POSITIVE_INFINITY
      else -> null
    }
  return Moments(
    Moment("E(X)", null, null, if (k > 1) 0.0 else null, Res.string.probability_student_mean),
    Moment(
      "V(X)",
      "k / (k − 2)",
      if (k > 2) "${term(k)} / (${term(k)} − 2)" else null,
      variance,
      Res.string.probability_student_variance,
    ),
  )
}

private fun geometricMoments(values: List<Double>, term: (Double) -> String): Moments {
  val (p) = values
  return Moments(
    Moment("E(X)", "1 / p", "1 / ${term(p)}", 1 / p, Res.string.probability_geometric_mean),
    Moment(
      "V(X)",
      "(1 − p) / p²",
      "(1 − ${term(p)}) / ${term(p)}²",
      (1 - p) / p.pow(2),
      Res.string.probability_geometric_variance,
    ),
  )
}

private fun poissonMoments(values: List<Double>): Moments {
  val (lambda) = values
  return Moments(
    Moment("E(X)", "λ", null, lambda, Res.string.probability_poisson_mean),
    Moment("V(X)", "λ", null, lambda, Res.string.probability_poisson_variance),
  )
}

private fun fisherMoments(values: List<Double>, term: (Double) -> String): Moments {
  val (d1, d2) = values
  val variance =
    when {
      d2 > 4 -> 2 * d2.pow(2) * (d1 + d2 - 2) / (d1 * (d2 - 2).pow(2) * (d2 - 4))
      d2 > 2 -> Double.POSITIVE_INFINITY
      else -> null
    }
  return Moments(
    Moment(
      "E(X)",
      "d₂ / (d₂ − 2)",
      if (d2 > 2) "${term(d2)} / (${term(d2)} − 2)" else null,
      if (d2 > 2) d2 / (d2 - 2) else null,
      Res.string.probability_fisher_mean,
    ),
    Moment(
      "V(X)",
      "2d₂²(d₁ + d₂ − 2) / (d₁(d₂ − 2)²(d₂ − 4))",
      if (d2 > FISHER_VARIANCE_MIN_D2) {
        "2 × ${term(d2)}² × (${term(d1)} + ${term(d2)} − 2) / " +
          "(${term(d1)} × (${term(d2)} − 2)² × (${term(d2)} − 4))"
      } else {
        null
      },
      variance,
      Res.string.probability_fisher_variance,
    ),
  )
}

private fun hypergeometricMoments(values: List<Double>, term: (Double) -> String): Moments {
  val (total, successes, n) = values
  val p = successes / total
  // Drawing everything of a population of one leaves nothing to vary
  val correction = if (total > 1) (total - n) / (total - 1) else 0.0
  return Moments(
    Moment(
      "E(X)",
      "n × K / N",
      "${term(n)} × ${term(successes)} / ${term(total)}",
      n * p,
      Res.string.probability_hypergeometric_mean,
    ),
    Moment(
      "V(X)",
      "n × K/N × (1 − K/N) × (N − n)/(N − 1)",
      "${term(n)} × ${term(p)} × ${term(1 - p)} × ${term(total - n)}/${term(total - 1)}",
      n * p * (1 - p) * correction,
      Res.string.probability_hypergeometric_variance,
    ),
  )
}

/** E(X) and V(X) buttons for every distribution but the binomial. */
@Composable
internal fun DistributionMoments(type: DistributionType, values: List<Double>, fractional: String) {
  val moments = type.moments(values, fractional) ?: return
  MomentButtons(
    expectedValue = { MomentContent(moments.expectedValue, fractional, withDeviation = false) },
    variance = { MomentContent(moments.variance, fractional, withDeviation = true) },
  )
}

/** `V(X) = σ² = 2² = 4`, σ for the variance, then why. */
@Composable
private fun MomentContent(moment: Moment, fractional: String, withDeviation: Boolean) {
  val value = moment.value
  if (value == null) {
    MomentFormula("${moment.symbol} ", stringResource(Res.string.probability_undefined))
  } else {
    val steps = listOfNotNull(moment.symbol, moment.formula, moment.substituted).joinToString(" = ")
    MomentFormula("$steps = ", if (value.isInfinite()) "∞" else value.shown(fractional))
    if (withDeviation) {
      MomentFormula(
        "σ(X) = √V(X) ${if (value.isInfinite()) "=" else "≈"} ",
        if (value.isInfinite()) "∞" else sqrt(value).shown(fractional, DEVIATION_DECIMALS),
      )
    }
  }
  Text(stringResource(moment.explanation), style = MaterialTheme.typography.bodyMedium)
}

private const val DEVIATION_DECIMALS = 4

/** V(X) = (b − a)² / 12 */
private const val UNIFORM_VARIANCE_DIVISOR = 12
/** The variance of Fisher's distribution needs d₂ > 4 */
private const val FISHER_VARIANCE_MIN_D2 = 4
