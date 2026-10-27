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
import com.sadellie.unitto.core.common.Token
import com.sadellie.unitto.core.common.isEqualTo
import com.sadellie.unitto.core.common.toFormattedString

/**
 * Writes polynomials as expressions with slots (powers in superscript, fractions with a bar), so
 * they can be displayed and calculated again.
 *
 * @property precision Digits after the dot for approximate coefficients.
 */
internal class SymbolicFormatter(private val precision: Int, private val outputFormat: Int) {

  fun expanded(polynomial: Polynomial): String {
    val terms =
      (polynomial.degree downTo 0).mapNotNull { power ->
        val coefficient = polynomial[power]
        if (coefficient.isZero) return@mapNotNull null
        Term(
          negative = coefficient.sign < 0,
          magnitude = if (coefficient.abs() == Rational.ONE) null else rational(coefficient.abs()),
          power = power,
        )
      }
    return joinTerms(terms)
  }

  /**
   * Factorization over real numbers. Quadratics with real roots are split using square roots:
   * x²+6x+6 = (x+3−√3)(x+3+√3). Starts with ≈ when some coefficients are approximate.
   */
  fun factorized(factorization: Factorization): String {
    val parts = mutableListOf<String>()
    if (factorization.zeroRoots == 1) parts += Token.Calculus.X
    if (factorization.zeroRoots > 1) parts += Token.Calculus.X + power(factorization.zeroRoots)

    var constant = factorization.constant
    val factors = mutableListOf<Pair<String, Int>>()
    factorization.factors.forEach { (factor, multiplicity) ->
      when (factor) {
        is Factor.Exact -> {
          val split = factor.polynomial.splitQuadratic()
          if (split == null) {
            factors += expanded(factor.polynomial) to multiplicity
          } else {
            repeat(multiplicity) { constant *= factor.polynomial.leading }
            split.forEach { factors += it to multiplicity }
          }
        }
        is Factor.Real -> factors += real(factor.coefficients) to multiplicity
      }
    }
    val approximate = if (factorization.factors.any { it.first is Factor.Real }) APPROXIMATELY else ""

    val onlyFactor = parts.isEmpty() && factors.size == 1 && factors.first().second == 1
    if (onlyFactor && constant == Rational.ONE) return approximate + factors.first().first

    factors.forEach { (text, multiplicity) ->
      parts += "(" + text + ")" + if (multiplicity > 1) power(multiplicity) else ""
    }

    val constantText =
      when {
        parts.isEmpty() -> rational(constant.abs())
        constant.abs() == Rational.ONE -> ""
        else -> rational(constant.abs())
      }
    val sign = if (constant.sign < 0) Token.Operator.MINUS else ""
    return approximate + sign + constantText + parts.joinToString("")
  }

  /** ax²+bx+c with real irrational roots ==> `x+p−q√m` and `x+p+q√m`. null otherwise. */
  private fun Polynomial.splitQuadratic(): List<String>? {
    val quadratic = quadratic() ?: return null
    if (quadratic.isComplex) return null
    val p = rationalSigned(-quadratic.center, first = false)
    val q = surd(quadratic.spread, imaginary = false)
    return listOf(
      Token.Calculus.X + p + Token.Operator.MINUS + q,
      Token.Calculus.X + p + Token.Operator.PLUS + q,
    )
  }

  /**
   * Root lines. Exact roots: `x=2, x=−3±√3`. When some roots are irrational or complex, also
   * approximate values: `x≈−4.732, x≈−1.268`. Pairs of complex conjugate roots use ±.
   */
  fun roots(factorization: Factorization): List<String> {
    val roots = mutableListOf<Root>()
    if (factorization.zeroRoots > 0) roots += Root(ZERO_TEXT, listOf(ZERO_TEXT), isRational = true)
    factorization.factors.forEach { (factor, _) ->
      when (factor) {
        is Factor.Exact -> {
          val c = factor.polynomial.coefficients
          if (c.size == 2) {
            val root = -c[0] / c[1]
            roots += Root(rational(root), listOf(number(root.toKBigDecimal(precision))), true)
          } else {
            val quadratic = factor.polynomial.quadratic()
            if (quadratic != null) roots += quadratic.root()
          }
        }
        is Factor.Real -> {
          val c = factor.coefficients
          roots +=
            if (c.size == 2) Root(null, listOf(number(KBigDecimal.valueOf(-c[1]))), false)
            else Root(null, listOf(approximateQuadraticRoots(c[0], c[1], c[2])), false)
        }
      }
    }

    val lines = mutableListOf<String>()
    val exact = roots.mapNotNull { it.exact }
    if (exact.size == roots.size) lines += exact.joinRoots(ROOT_EQUALS)
    if (roots.any { it.needsApproximation }) {
      lines += roots.flatMap { it.approximate }.joinRoots(APPROXIMATELY)
    }
    return lines
  }

  /** `Δ=b²−4ac` for quadratic polynomials, null otherwise. */
  fun discriminant(polynomial: Polynomial): String? {
    if (polynomial.degree != 2) return null
    val value = polynomial[1] * polynomial[1] - Rational.of(4) * polynomial[2] * polynomial[0]
    return DISCRIMINANT + ROOT_EQUALS + rational(value)
  }

  /** Exact value with square roots and i, like `√5i` or `1+2√3`. */
  fun exact(value: ExactValue): String {
    val re = if (value.re.isZero) "" else surd(value.re, imaginary = false)
    if (value.im.isZero) return re.ifEmpty { ZERO_TEXT }
    val imaginary = surd(Surd(value.im.coefficient.abs(), value.im.radicand), imaginary = true)
    val sign =
      when {
        value.im.coefficient.sign < 0 -> Token.Operator.MINUS
        re.isEmpty() -> ""
        else -> Token.Operator.PLUS
      }
    return re + sign + imaginary
  }

  /** Real number with square root, like `2√3`. */
  fun surd(value: Surd): String = if (value.isZero) ZERO_TEXT else surd(value, imaginary = false)

  /** `−3/4`, `2√3`, `√5i`, `i` */
  private fun surd(value: Surd, imaginary: Boolean): String {
    val sign = if (value.coefficient.sign < 0) Token.Operator.MINUS else ""
    val magnitude = value.coefficient.abs()
    val hasRoot = value.radicand > 1
    val coefficientText =
      if (magnitude == Rational.ONE && (hasRoot || imaginary)) "" else rational(magnitude)
    val root =
      if (hasRoot) {
        Token.Operator.SQRT + Token.Layout.RADICAND_START + value.radicand + Token.Layout.END
      } else {
        ""
      }
    return sign + coefficientText + root + if (imaginary) Token.Const.I else ""
  }

  /** Rational with sign. Empty for zero when not [first]. */
  private fun rationalSigned(value: Rational, first: Boolean): String =
    when {
      value.isZero -> if (first) ZERO_TEXT else ""
      value.sign < 0 -> Token.Operator.MINUS + rational(value.abs())
      first -> rational(value)
      else -> Token.Operator.PLUS + rational(value)
    }

  /** Roots of ax²+bx+c: center ± spread (times i when complex). */
  private class Quadratic(val center: Rational, val spread: Surd, val isComplex: Boolean)

  private fun Polynomial.quadratic(): Quadratic? {
    if (degree != 2) return null
    val a = this[2]
    val b = this[1]
    val c = this[0]
    val discriminant = b * b - Rational.of(4) * a * c
    val root = Surd.sqrt(discriminant.abs()) ?: return null
    val twoA = Rational.of(2) * a
    return Quadratic(
      center = -b / twoA,
      spread = Surd(root.coefficient / twoA.abs(), root.radicand),
      isComplex = discriminant.sign < 0,
    )
  }

  private fun Quadratic.root(): Root {
    val center = rationalSigned(center, first = true).takeIf { !this.center.isZero } ?: ""
    val exact = center + PLUS_MINUS + surd(spread, imaginary = isComplex)
    val centerValue = this.center.toDouble()
    val spreadValue = spread.coefficient.toDouble() * kotlin.math.sqrt(spread.radicand.toDouble())
    val approximate =
      if (isComplex) {
        listOf(approximateCenterSpread(centerValue, spreadValue, imaginary = true))
      } else {
        listOf(centerValue - spreadValue, centerValue + spreadValue).map {
          number(KBigDecimal.valueOf(it))
        }
      }
    return Root(exact, approximate, isRational = false)
  }

  /** Both roots of ax²+bx+c as `p±q` (real) or `p±qi` (complex). */
  private fun approximateQuadraticRoots(a: Double, b: Double, c: Double): String {
    val discriminant = b * b - 4 * a * c
    val center = -b / (2 * a)
    val spread = kotlin.math.sqrt(kotlin.math.abs(discriminant)) / (2 * kotlin.math.abs(a))
    return approximateCenterSpread(center, spread, imaginary = discriminant < 0)
  }

  private fun approximateCenterSpread(center: Double, spread: Double, imaginary: Boolean): String {
    val centerText = number(KBigDecimal.valueOf(center))
    var spreadText = number(KBigDecimal.valueOf(spread))
    if (imaginary) spreadText = (if (spreadText == "1") "" else spreadText) + Token.Const.I
    val prefix = if (centerText == ZERO_TEXT) "" else centerText
    return prefix + PLUS_MINUS + spreadText
  }

  private fun List<String>.joinRoots(relation: String) =
    joinToString(ROOT_SEPARATOR) { Token.Calculus.X + relation + it }

  /** @property exact null when root can't be written exactly. */
  private class Root(val exact: String?, val approximate: List<String>, isRational: Boolean) {
    /** Exact form has square roots or fractions, or there is no exact form. */
    val needsApproximation =
      !isRational &&
        (exact == null ||
          Token.Operator.SQRT in exact ||
          Token.Layout.NUMERATOR_START in exact)
  }

  /** Monic factor with approximate coefficients, from highest power. */
  private fun real(coefficients: List<Double>): String {
    val degree = coefficients.size - 1
    val terms =
      coefficients.mapIndexedNotNull { index, coefficient ->
        val power = degree - index
        val magnitude = number(KBigDecimal.valueOf(kotlin.math.abs(coefficient)))
        if (index > 0 && magnitude == ZERO_TEXT) return@mapIndexedNotNull null
        Term(
          negative = coefficient < 0,
          magnitude = if (index == 0) null else magnitude,
          power = power,
        )
      }
    return joinTerms(terms)
  }

  /** Whole number, decimal (when exact) or a fraction. */
  private fun rational(value: Rational): String {
    if (value.sign < 0) return Token.Operator.MINUS + rational(value.abs())
    if (value.isInteger) return number(value.numerator)
    if (value.denominator.hasOnlyFactors2And5()) return number(value.toKBigDecimal(precision))
    return Token.Layout.NUMERATOR_START +
      number(value.numerator) +
      Token.Layout.END +
      Token.Layout.DENOMINATOR_START +
      number(value.denominator) +
      Token.Layout.END
  }

  private fun number(value: KBigDecimal): String =
    value.toFormattedString(precision, outputFormat).replace("-", Token.Operator.MINUS)

  private fun power(n: Int) =
    Token.Operator.POWER + Token.Layout.SUPERSCRIPT_START + n + Token.Layout.END

  private fun joinTerms(terms: List<Term>): String {
    if (terms.isEmpty()) return ZERO_TEXT
    return buildString {
      terms.forEachIndexed { index, term ->
        when {
          term.negative -> append(Token.Operator.MINUS)
          index > 0 -> append(Token.Operator.PLUS)
        }
        when {
          term.magnitude != null -> append(term.magnitude)
          term.power == 0 -> append("1")
        }
        when (term.power) {
          0 -> Unit
          1 -> append(Token.Calculus.X)
          else -> append(Token.Calculus.X + power(term.power))
        }
      }
    }
  }

  /** @property magnitude Absolute value of coefficient, null when it is 1. */
  private class Term(val negative: Boolean, val magnitude: String?, val power: Int)
}

private fun KBigDecimal.hasOnlyFactors2And5(): Boolean {
  var value = this
  for (factor in listOf(2L, 5L)) {
    val divisor = KBigDecimal.valueOf(factor)
    while (!value.isEqualTo(KBigDecimal.ONE) &&
      value.remainder(divisor).isEqualTo(KBigDecimal.ZERO)) {
      value = value.exactDivide(divisor)
    }
  }
  return value.isEqualTo(KBigDecimal.ONE)
}

private const val ZERO_TEXT = "0"
private const val ROOT_EQUALS = "="
private const val PLUS_MINUS = "±"
private const val APPROXIMATELY = "≈"
private const val DISCRIMINANT = "Δ"
private const val ROOT_SEPARATOR = ",  "
