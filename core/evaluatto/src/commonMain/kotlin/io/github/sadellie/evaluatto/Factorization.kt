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

package io.github.sadellie.evaluatto

import com.sadellie.unitto.core.common.KBigDecimal
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.roundToLong

/**
 * Factorization of a polynomial: `constant · x^zeroRoots · factors`.
 *
 * Exact factors have whole coefficients and can't be factored further over rational numbers.
 * Irreducible factors of degree 3 and more are split into [RealFactor]s with approximate
 * coefficients (factorization over real numbers).
 */
internal class Factorization(
  val constant: Rational,
  val zeroRoots: Int,
  val factors: List<Pair<Factor, Int>>,
)

internal sealed interface Factor {
  class Exact(val polynomial: Polynomial) : Factor

  /** Monic `x + b` or `x² + bx + c`, coefficients from highest power. */
  class Real(val coefficients: List<Double>) : Factor
}

internal fun Polynomial.factorize(): Factorization {
  if (degree <= 0) return Factorization(leading, 0, emptyList())

  var (constant, primitive) = primitive()
  var zeroRoots = 0
  while (primitive[0].isZero) {
    primitive = Polynomial(primitive.coefficients.drop(1))
    zeroRoots++
  }

  val irreducible =
    if (primitive.isSmall()) primitive.squareFree().factorSquareFree()
    else listOf(primitive) // too big for exact factorization

  val factors = mutableListOf<Pair<Factor, Int>>()
  var remaining = primitive
  for (factor in irreducible.sortedBy { it.degree }) {
    if (factor.degree <= 0) continue
    var multiplicity = 0
    while (true) {
      remaining = remaining.divideExact(factor) ?: break
      multiplicity++
    }
    if (multiplicity == 0) multiplicity = 1

    if (factor.degree <= 2) {
      factors += Factor.Exact(factor) to multiplicity
      continue
    }

    // Approximate factorization over real numbers
    repeat(multiplicity) { constant *= factor.leading }
    factor.realFactors().forEach { factors += it to multiplicity }
  }
  return Factorization(constant, zeroRoots, factors)
}

/** @return Rational constant and polynomial with whole coprime coefficients, positive leading. */
private fun Polynomial.primitive(): Pair<Rational, Polynomial> {
  var lcm = KBigDecimal.ONE
  coefficients.forEach { c ->
    lcm = lcm.multiply(c.denominator).exactDivide(gcd(lcm, c.denominator))
  }
  val whole = coefficients.map { (it * Rational.of(lcm)).numerator }
  var content = whole.fold(KBigDecimal.ZERO) { acc, c -> gcd(acc, c) }
  if (whole.last() < KBigDecimal.ZERO) content = -content
  val constant = Rational.of(content, lcm)
  return constant to Polynomial(whole.map { Rational.of(it.exactDivide(content)) })
}

private fun Polynomial.isSmall() =
  coefficients.all { it.numerator.abs() <= MAX_EXACT_COEFFICIENT && it.isInteger }

/** Removes repeated factors: p / gcd(p, p'). */
private fun Polynomial.squareFree(): Polynomial {
  val divisor = polynomialGcd(this, derivative())
  if (divisor.degree <= 0) return this
  return (divideExact(divisor) ?: this).primitive().second
}

private fun polynomialGcd(a: Polynomial, b: Polynomial): Polynomial {
  var x = a
  var y = b
  while (!y.isZero) {
    val remainder = x.remainder(y)
    x = y
    y = remainder
  }
  return x
}

private fun Polynomial.remainder(divisor: Polynomial): Polynomial {
  val remainder = coefficients.toMutableList()
  for (power in degree - divisor.degree downTo 0) {
    val factor = remainder[power + divisor.degree] / divisor.leading
    divisor.coefficients.forEachIndexed { i, c ->
      remainder[power + i] = remainder[power + i] - factor * c
    }
  }
  return Polynomial(remainder.take(maxOf(divisor.degree, 0)))
}

/** Irreducible factors (over rational numbers) of square free primitive polynomial. */
private fun Polynomial.factorSquareFree(): List<Polynomial> {
  val factors = mutableListOf<Polynomial>()
  var remaining = this

  // Rational roots p/q: p divides constant term, q divides leading coefficient
  val constantTerm = remaining[0].numerator.abs()
  val leadingTerm = remaining.leading.numerator.abs()
  if (constantTerm <= MAX_DIVISORS_OF && leadingTerm <= MAX_DIVISORS_OF) {
    for (p in divisors(constantTerm.toWholeLong())) {
      for (q in divisors(leadingTerm.toWholeLong())) {
        for (sign in listOf(1L, -1L)) {
          val linear = Polynomial(listOf(Rational.of(-sign * p), Rational.of(q)))
          val quotient = remaining.divideExact(linear) ?: continue
          factors += linear.primitive().second
          remaining = quotient
        }
      }
    }
  }

  // Factors of higher degree: try products of approximate roots with whole coefficients
  while (remaining.degree >= 4) {
    val factor = remaining.findFactorFromRoots() ?: break
    factors += factor
    remaining = remaining.divideExact(factor)!!.primitive().second
  }

  if (remaining.degree > 0) factors += remaining.primitive().second
  return factors
}

private fun Polynomial.findFactorFromRoots(): Polynomial? {
  val leadingTerm = leading.numerator.abs()
  if (leadingTerm > MAX_DIVISORS_OF) return null
  val groups = realRootGroups()
  if (groups.size > MAX_ROOT_GROUPS) return null
  val leadingDivisors = divisors(leadingTerm.toWholeLong())

  // Smallest factors first
  val subsets =
    (1..<(1 shl groups.size))
      .map { mask -> groups.filterIndexed { i, _ -> mask and (1 shl i) != 0 } }
      .filter { subset -> subset.sumOf { it.degree } in 2..degree / 2 }
      .sortedBy { subset -> subset.sumOf { it.degree } }

  for (subset in subsets) {
    val monic = subset.fold(listOf(1.0)) { acc, group -> multiply(acc, group.monicAscending()) }
    for (scale in leadingDivisors) {
      val scaled = monic.map { it * scale }
      val isWhole = scaled.all { abs(it - it.roundToLong()) < WHOLE_TOLERANCE * maxOf(1.0, abs(it)) }
      if (!isWhole) continue
      val candidate = Polynomial(scaled.map { Rational.of(it.roundToLong()) })
      if (divideExact(candidate) != null) return candidate.primitive().second
    }
  }
  return null
}

private fun Polynomial.realFactors(): List<Factor.Real> =
  realRootGroups()
    .sortedBy { it.re }
    .map { group ->
      if (group.isReal) Factor.Real(listOf(1.0, -group.re))
      else Factor.Real(listOf(1.0, -2 * group.re, group.re * group.re + group.im * group.im))
    }

/** Real root or a pair of complex conjugate roots. */
private class RootGroup(val re: Double, val im: Double) {
  val isReal: Boolean
    get() = im == 0.0

  val degree: Int
    get() = if (isReal) 1 else 2

  /** Coefficients from lowest power. */
  fun monicAscending(): List<Double> =
    if (isReal) listOf(-re, 1.0) else listOf(re * re + im * im, -2 * re, 1.0)
}

private fun Polynomial.realRootGroups(): List<RootGroup> {
  val roots = numericRoots(coefficients.map { it.toDouble() })
  val groups = mutableListOf<RootGroup>()
  val used = BooleanArray(roots.size)
  roots.forEachIndexed { i, (re, im) ->
    if (used[i]) return@forEachIndexed
    used[i] = true
    if (abs(im) < REAL_ROOT_TOLERANCE * (1 + abs(re))) {
      groups += RootGroup(re, 0.0)
      return@forEachIndexed
    }
    // pair with closest conjugate
    val conjugate =
      roots.indices.filter { !used[it] }.minByOrNull { hypot(roots[it].first - re, roots[it].second + im) }
    if (conjugate != null) used[conjugate] = true
    groups += RootGroup(re, abs(im))
  }
  return groups
}

/** Durand–Kerner method. [ascending] coefficients from lowest power. */
private fun numericRoots(ascending: List<Double>): List<Pair<Double, Double>> {
  val degree = ascending.size - 1
  val monic = ascending.map { it / ascending.last() }
  var roots = List(degree) { k -> complexPow(SEED_RE, SEED_IM, k) }

  repeat(ROOT_ITERATIONS) {
    val next =
      roots.mapIndexed { i, (re, im) ->
        var (numRe, numIm) = evaluateComplex(monic, re, im)
        var denRe = 1.0
        var denIm = 0.0
        roots.forEachIndexed { j, (otherRe, otherIm) ->
          if (i == j) return@forEachIndexed
          val dRe = re - otherRe
          val dIm = im - otherIm
          val newRe = denRe * dRe - denIm * dIm
          denIm = denRe * dIm + denIm * dRe
          denRe = newRe
        }
        val norm = denRe * denRe + denIm * denIm
        val qRe = (numRe * denRe + numIm * denIm) / norm
        val qIm = (numIm * denRe - numRe * denIm) / norm
        numRe = re - qRe
        numIm = im - qIm
        numRe to numIm
      }
    val change = next.indices.maxOf { hypot(next[it].first - roots[it].first, next[it].second - roots[it].second) }
    roots = next
    if (change < ROOT_CONVERGENCE) return roots
  }
  return roots
}

private fun evaluateComplex(ascending: List<Double>, re: Double, im: Double): Pair<Double, Double> {
  var resultRe = 0.0
  var resultIm = 0.0
  for (coefficient in ascending.asReversed()) {
    val newRe = resultRe * re - resultIm * im + coefficient
    resultIm = resultRe * im + resultIm * re
    resultRe = newRe
  }
  return resultRe to resultIm
}

private fun complexPow(re: Double, im: Double, n: Int): Pair<Double, Double> {
  var resultRe = 1.0
  var resultIm = 0.0
  repeat(n) {
    val newRe = resultRe * re - resultIm * im
    resultIm = resultRe * im + resultIm * re
    resultRe = newRe
  }
  return resultRe to resultIm
}

private fun multiply(a: List<Double>, b: List<Double>): List<Double> {
  val result = MutableList(a.size + b.size - 1) { 0.0 }
  a.forEachIndexed { i, x -> b.forEachIndexed { j, y -> result[i + j] += x * y } }
  return result
}

private fun KBigDecimal.toWholeLong(): Long = setScale(0).toPlainString().toLong()

private fun divisors(n: Long): List<Long> {
  if (n == 0L) return listOf(1L)
  val small = mutableListOf<Long>()
  val large = mutableListOf<Long>()
  var i = 1L
  while (i * i <= n) {
    if (n % i == 0L) {
      small += i
      if (i != n / i) large += n / i
    }
    i++
  }
  return small + large.asReversed()
}

private val MAX_EXACT_COEFFICIENT = KBigDecimal("1E30")
private val MAX_DIVISORS_OF = KBigDecimal("1E12")
private const val MAX_ROOT_GROUPS = 12
private const val WHOLE_TOLERANCE = 1e-6
private const val REAL_ROOT_TOLERANCE = 1e-7
private const val ROOT_ITERATIONS = 2000
private const val ROOT_CONVERGENCE = 1e-14
private const val SEED_RE = 0.4
private const val SEED_IM = 0.9
