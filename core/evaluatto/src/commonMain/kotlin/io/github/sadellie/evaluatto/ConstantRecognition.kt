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
import com.sadellie.unitto.core.common.KBigDecimalMath
import com.sadellie.unitto.core.common.KMathContext
import com.sadellie.unitto.core.common.KRoundingMode
import com.sadellie.unitto.core.common.Token
import com.sadellie.unitto.core.common.isEqualTo

/** Has a sum, product or definite integral, results of those are worth recognizing. */
internal fun Node.hasNumericCalculus(): Boolean =
  when (this) {
    is Node.Series -> true
    is Node.Integral -> lower != null
    is Node.Negate -> node.hasNumericCalculus()
    is Node.Binary -> left.hasNumericCalculus() || right.hasNumericCalculus()
    is Node.Function -> argument.hasNumericCalculus()
    is Node.Log -> base.hasNumericCalculus() || argument.hasNumericCalculus()
    is Node.Factorial -> node.hasNumericCalculus()
    else -> false
  }

/**
 * Exact form of a calculated number: p/q·πᵏ, p/(q·πᵏ), p/q·e, p/q·ln(2) or p/q. For example
 * Σ1/n² = π²/6.
 *
 * Value must be precise (more than [MATCH_DIGITS] digits), otherwise nothing is recognized.
 *
 * @return Expression with layout slots, null when nothing matches.
 */
internal fun recognizeConstant(value: KBigDecimal): String? {
  if (value.isEqualTo(KBigDecimal.ZERO)) return null
  val mathContext = KMathContext(PRECISION, KRoundingMode.HALF_EVEN)
  val pi = KBigDecimalMath.pi(mathContext)
  val bases =
    listOf(KBigDecimal.ONE to "") +
      (1..MAX_PI_POWER).map { power -> pi.pow(power) to piPower(power) } +
      listOf(
        KBigDecimalMath.e(mathContext) to Token.Const.E,
        KBigDecimalMath.log(KBigDecimal.valueOf(2), mathContext) to Token.Func.LN + "(2)",
      )

  for ((base, text) in bases) {
    val coefficient = value.divide(base, mathContext)
    val (p, q) = coefficient.toSmallFraction() ?: continue
    // whole numbers are shown as they are
    if (text.isEmpty() && q == 1L) return null
    return format(p, q, text)
  }
  // 2/π
  for (power in 1..MAX_INVERSE_PI_POWER) {
    val coefficient = value.multiply(pi.pow(power))
    val (p, q) = coefficient.toSmallFraction() ?: continue
    val piText = piPower(power)
    val denominator = if (q == 1L) piText else "$q$piText"
    val sign = if (p < 0) Token.Operator.MINUS else ""
    return sign +
      Token.Layout.NUMERATOR_START +
      kotlin.math.abs(p) +
      Token.Layout.END +
      Token.Layout.DENOMINATOR_START +
      denominator +
      Token.Layout.END
  }
  return null
}

private fun piPower(power: Int): String =
  if (power == 1) Token.Const.PI
  else Token.Const.PI + Token.Operator.POWER + Token.Layout.SUPERSCRIPT_START + power + Token.Layout.END

/** Continued fraction: best p/q with q ≤ [MAX_DENOMINATOR] that matches [MATCH_DIGITS] digits. */
private fun KBigDecimal.toSmallFraction(): Pair<Long, Long>? {
  val mathContext = KMathContext(PRECISION, KRoundingMode.HALF_EVEN)
  val tolerance = KBigDecimal("1E-$MATCH_DIGITS").multiply(maxOf(abs(), KBigDecimal.ONE))
  var rest = abs()
  var p0 = 0L
  var q0 = 1L
  var p1 = 1L
  var q1 = 0L
  repeat(MAX_STEPS) {
    val whole = rest.setScale(0, KRoundingMode.DOWN)
    val a = whole.toPlainString().toLongOrNull() ?: return null
    val p = a * p1 + p0
    val q = a * q1 + q0
    if (q > MAX_DENOMINATOR || p > MAX_NUMERATOR) return null
    val approximation = KBigDecimal.valueOf(p).divide(KBigDecimal.valueOf(q), mathContext)
    if ((approximation - abs()).abs() <= tolerance) {
      return (if (this < KBigDecimal.ZERO) -p else p) to q
    }
    val fractional = rest - whole
    if (fractional.isEqualTo(KBigDecimal.ZERO)) return null
    rest = KBigDecimal.ONE.divide(fractional, mathContext)
    p0 = p1
    q0 = q1
    p1 = p
    q1 = q
  }
  return null
}

private fun format(p: Long, q: Long, base: String): String {
  val sign = if (p < 0) Token.Operator.MINUS else ""
  val magnitude = kotlin.math.abs(p)
  val numerator = if (magnitude == 1L && base.isNotEmpty()) base else "$magnitude$base"
  if (q == 1L) return sign + numerator
  return sign +
    Token.Layout.NUMERATOR_START +
    numerator +
    Token.Layout.END +
    Token.Layout.DENOMINATOR_START +
    q +
    Token.Layout.END
}

private const val PRECISION = 60
private const val MATCH_DIGITS = 18
private const val MAX_PI_POWER = 6
private const val MAX_INVERSE_PI_POWER = 2
private const val MAX_DENOMINATOR = 10_000L
private const val MAX_NUMERATOR = 1_000_000L
private const val MAX_STEPS = 30
