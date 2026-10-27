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

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Probability distribution. Continuous ones have a density, discrete ones have probabilities of
 * whole values.
 */
internal sealed class Distribution {
  abstract val isDiscrete: Boolean

  /** Smallest and largest possible values, can be infinite. */
  abstract val support: ClosedRange<Double>

  /** Density for continuous, P(X = x) for discrete. */
  abstract fun density(x: Double): Double

  /** P(X ≤ x). */
  abstract fun cdf(x: Double): Double

  /** P(X ≤ a). */
  fun atMost(a: Double): Double = cdf(a).coerceIn(0.0, 1.0)

  /** P(X ≥ a). For discrete, X = a is included. */
  fun atLeast(a: Double): Double =
    (1 - if (isDiscrete) cdf(ceil(a) - 1) else cdf(a)).coerceIn(0.0, 1.0)

  /** P(a ≤ X ≤ b). */
  fun between(a: Double, b: Double): Double {
    if (b < a) return 0.0
    val below = if (isDiscrete) cdf(ceil(a) - 1) else cdf(a)
    return (cdf(b) - below).coerceIn(0.0, 1.0)
  }

  /** P(X = a): only discrete values have a probability. */
  fun exactly(a: Double): Double = if (isDiscrete && a == floor(a)) density(a) else 0.0

  /**
   * Smallest x with P(X ≤ x) ≥ [p]. Whole number for discrete distributions. Solves NumWorks style
   * questions like "P(X ≤ a) = 0.95, what is a?".
   */
  fun quantile(p: Double): Double {
    if (p <= 0) return support.start
    if (p >= 1) return support.endInclusive
    // Find bounds (only infinite sides need searching), then bisect
    var low = if (support.start.isFinite()) support.start else -1.0
    var high = if (support.endInclusive.isFinite()) support.endInclusive else max(1.0, low + 1)
    if (!support.start.isFinite()) {
      while (cdf(low) > p && low > -SEARCH_LIMIT) low = low * 2 - 1
    }
    if (!support.endInclusive.isFinite()) {
      while (cdf(high) < p && high < SEARCH_LIMIT) high = high * 2 + 1
    }
    repeat(BISECTION_STEPS) {
      val middle = (low + high) / 2
      if (cdf(middle) < p) low = middle else high = middle
    }
    if (!isDiscrete) return high
    // Whole value: smallest k with cdf(k) ≥ p
    var k = floor(high)
    while (k > support.start && cdf(k - 1) >= p) k--
    while (cdf(k) < p) k++
    return k
  }

  /** Values worth showing on a graph: almost all of the probability is between them. */
  fun visibleRange(): ClosedRange<Double> {
    var start = max(support.start, quantile(VISIBLE_TAIL))
    var end = min(support.endInclusive, quantile(1 - VISIBLE_TAIL))
    if (isDiscrete) {
      start = floor(start)
      end = ceil(end)
    }
    if (end - start < MIN_RANGE) {
      start -= 1
      end += 1
    }
    return start..end
  }
}

internal class Binomial(private val n: Int, private val p: Double) : Distribution() {
  override val isDiscrete = true
  override val support = 0.0..n.toDouble()

  override fun density(x: Double): Double {
    if (x < 0 || x > n || x != floor(x)) return 0.0
    val k = x.toInt()
    if (p == 0.0) return if (k == 0) 1.0 else 0.0
    if (p == 1.0) return if (k == n) 1.0 else 0.0
    return exp(lnChoose(n, k) + k * ln(p) + (n - k) * ln(1 - p))
  }

  override fun cdf(x: Double): Double = sumDensity(this, floor(min(x, n.toDouble())).toInt())
}

internal class Uniform(private val a: Double, private val b: Double) : Distribution() {
  override val isDiscrete = false
  override val support = a..b

  override fun density(x: Double): Double = if (x < a || x > b) 0.0 else 1 / (b - a)

  override fun cdf(x: Double): Double = ((x - a) / (b - a)).coerceIn(0.0, 1.0)
}

internal class Exponential(private val lambda: Double) : Distribution() {
  override val isDiscrete = false
  override val support = 0.0..Double.POSITIVE_INFINITY

  override fun density(x: Double): Double = if (x < 0) 0.0 else lambda * exp(-lambda * x)

  override fun cdf(x: Double): Double = if (x < 0) 0.0 else 1 - exp(-lambda * x)
}

internal class Normal(private val mean: Double, private val sigma: Double) : Distribution() {
  override val isDiscrete = false
  override val support = Double.NEGATIVE_INFINITY..Double.POSITIVE_INFINITY

  override fun density(x: Double): Double {
    val z = (x - mean) / sigma
    return exp(-z * z / 2) / (sigma * sqrt(2 * PI))
  }

  override fun cdf(x: Double): Double = (1 + erf((x - mean) / (sigma * sqrt(2.0)))) / 2
}

internal class ChiSquared(private val k: Double) : Distribution() {
  override val isDiscrete = false
  override val support = 0.0..Double.POSITIVE_INFINITY

  override fun density(x: Double): Double {
    if (x < 0) return 0.0
    if (x == 0.0) return if (k == 2.0) 0.5 else if (k < 2) Double.POSITIVE_INFINITY else 0.0
    return exp((k / 2 - 1) * ln(x) - x / 2 - k / 2 * ln(2.0) - lnGamma(k / 2))
  }

  override fun cdf(x: Double): Double = if (x <= 0) 0.0 else lowerGammaRegularized(k / 2, x / 2)
}

internal class Student(private val nu: Double) : Distribution() {
  override val isDiscrete = false
  override val support = Double.NEGATIVE_INFINITY..Double.POSITIVE_INFINITY

  override fun density(x: Double): Double =
    exp(lnGamma((nu + 1) / 2) - lnGamma(nu / 2) - ln(nu * PI) / 2 - (nu + 1) / 2 * ln(1 + x * x / nu))

  override fun cdf(x: Double): Double {
    val tail = betaRegularized(nu / (nu + x * x), nu / 2, 0.5) / 2
    return if (x >= 0) 1 - tail else tail
  }
}

/** Number of trials until the first success, like NumWorks: X ≥ 1. */
internal class Geometric(private val p: Double) : Distribution() {
  override val isDiscrete = true
  override val support = 1.0..Double.POSITIVE_INFINITY

  override fun density(x: Double): Double =
    if (x < 1 || x != floor(x)) 0.0 else (1 - p).pow(x - 1) * p

  override fun cdf(x: Double): Double = if (x < 1) 0.0 else 1 - (1 - p).pow(floor(x))
}

internal class Poisson(private val lambda: Double) : Distribution() {
  override val isDiscrete = true
  override val support = 0.0..Double.POSITIVE_INFINITY

  override fun density(x: Double): Double =
    if (x < 0 || x != floor(x)) 0.0 else exp(x * ln(lambda) - lambda - lnGamma(x + 1))

  // P(X ≤ k) = Q(k + 1, λ)
  override fun cdf(x: Double): Double =
    if (x < 0) 0.0 else 1 - lowerGammaRegularized(floor(x) + 1, lambda)
}

internal class Fisher(private val d1: Double, private val d2: Double) : Distribution() {
  override val isDiscrete = false
  override val support = 0.0..Double.POSITIVE_INFINITY

  override fun density(x: Double): Double {
    if (x <= 0) return 0.0
    val logDensity =
      (d1 / 2) * ln(d1 / d2) + (d1 / 2 - 1) * ln(x) - (d1 + d2) / 2 * ln(1 + d1 * x / d2) - lnBeta(d1 / 2, d2 / 2)
    return exp(logDensity)
  }

  override fun cdf(x: Double): Double = if (x <= 0) 0.0 else betaRegularized(d1 * x / (d1 * x + d2), d1 / 2, d2 / 2)
}

/** [n] draws without replacement from [total] items, [successes] of them are successes. */
internal class Hypergeometric(private val total: Int, private val successes: Int, private val n: Int) :
  Distribution() {
  override val isDiscrete = true
  override val support = max(0, n - (total - successes)).toDouble()..min(n, successes).toDouble()

  override fun density(x: Double): Double {
    if (x < support.start || x > support.endInclusive || x != floor(x)) return 0.0
    val k = x.toInt()
    return exp(lnChoose(successes, k) + lnChoose(total - successes, n - k) - lnChoose(total, n))
  }

  override fun cdf(x: Double): Double =
    sumDensity(this, floor(min(x, support.endInclusive)).toInt())
}

/** P(X ≤ last) of a discrete distribution with finite support. */
private fun sumDensity(distribution: Distribution, last: Int): Double {
  val first = distribution.support.start.toInt()
  if (last < first) return 0.0
  var sum = 0.0
  for (k in first..last) sum += distribution.density(k.toDouble())
  return sum.coerceAtMost(1.0)
}

/** ln Γ(x), Lanczos approximation. */
internal fun lnGamma(x: Double): Double {
  if (x < 0.5) return ln(PI / abs(sin(PI * x))) - lnGamma(1 - x)
  val z = x - 1
  var sum = LANCZOS[0]
  for (i in 1 until LANCZOS.size) sum += LANCZOS[i] / (z + i)
  val t = z + LANCZOS_G + 0.5
  return 0.5 * ln(2 * PI) + (z + 0.5) * ln(t) - t + ln(sum)
}

private fun lnBeta(a: Double, b: Double) = lnGamma(a) + lnGamma(b) - lnGamma(a + b)

private fun lnChoose(n: Int, k: Int): Double =
  lnGamma(n + 1.0) - lnGamma(k + 1.0) - lnGamma(n - k + 1.0)

/** erf(x) through the incomplete gamma function. */
internal fun erf(x: Double): Double {
  val value = lowerGammaRegularized(0.5, x * x)
  return if (x < 0) -value else value
}

/** P(a, x): regularized lower incomplete gamma function. */
internal fun lowerGammaRegularized(a: Double, x: Double): Double {
  if (x <= 0) return 0.0
  val logPrefix = a * ln(x) - x - lnGamma(a)
  if (x < a + 1) {
    // Series
    var term = 1 / a
    var sum = term
    var n = a
    repeat(MAX_ITERATIONS) {
      n += 1
      term *= x / n
      sum += term
      if (abs(term) < abs(sum) * EPSILON) return (sum * exp(logPrefix)).coerceIn(0.0, 1.0)
    }
    return (sum * exp(logPrefix)).coerceIn(0.0, 1.0)
  }
  // Continued fraction for Q(a, x)
  var b = x + 1 - a
  var c = 1 / TINY
  var d = 1 / b
  var h = d
  for (i in 1..MAX_ITERATIONS) {
    val an = -i * (i - a)
    b += 2
    d = an * d + b
    if (abs(d) < TINY) d = TINY
    c = b + an / c
    if (abs(c) < TINY) c = TINY
    d = 1 / d
    val delta = d * c
    h *= delta
    if (abs(delta - 1) < EPSILON) break
  }
  return (1 - exp(logPrefix) * h).coerceIn(0.0, 1.0)
}

/** I_x(a, b): regularized incomplete beta function. */
internal fun betaRegularized(x: Double, a: Double, b: Double): Double {
  if (x <= 0) return 0.0
  if (x >= 1) return 1.0
  val front = exp(a * ln(x) + b * ln(1 - x) - lnBeta(a, b))
  // Continued fraction converges fast below this point, use symmetry above it
  return if (x < (a + 1) / (a + b + 2)) front * betaFraction(x, a, b) / a
  else 1 - front * betaFraction(1 - x, b, a) / b
}

private fun betaFraction(x: Double, a: Double, b: Double): Double {
  var c = 1.0
  var d = 1 - (a + b) * x / (a + 1)
  if (abs(d) < TINY) d = TINY
  d = 1 / d
  var h = d
  for (m in 1..MAX_ITERATIONS) {
    val m2 = 2 * m
    var aa = m * (b - m) * x / ((a + m2 - 1) * (a + m2))
    d = 1 + aa * d
    if (abs(d) < TINY) d = TINY
    c = 1 + aa / c
    if (abs(c) < TINY) c = TINY
    d = 1 / d
    h *= d * c
    aa = -(a + m) * (a + b + m) * x / ((a + m2) * (a + m2 + 1))
    d = 1 + aa * d
    if (abs(d) < TINY) d = TINY
    c = 1 + aa / c
    if (abs(c) < TINY) c = TINY
    d = 1 / d
    val delta = d * c
    h *= delta
    if (abs(delta - 1) < EPSILON) break
  }
  return h
}

private const val LANCZOS_G = 7.0
private val LANCZOS =
  doubleArrayOf(
    0.99999999999980993,
    676.5203681218851,
    -1259.1392167224028,
    771.32342877765313,
    -176.61502916214059,
    12.507343278686905,
    -0.13857109526572012,
    9.9843695780195716e-6,
    1.5056327351493116e-7,
  )
private const val MAX_ITERATIONS = 300
private const val EPSILON = 1e-15
private const val TINY = 1e-300
private const val SEARCH_LIMIT = 1e12
private const val BISECTION_STEPS = 200
private const val VISIBLE_TAIL = 0.001
private const val MIN_RANGE = 1e-9
