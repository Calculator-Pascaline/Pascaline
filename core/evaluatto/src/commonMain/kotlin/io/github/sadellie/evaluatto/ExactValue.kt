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
import com.sadellie.unitto.core.common.PhysicalUnits

/** `coefficient·√radicand`, radicand is square free. Radicand 1 means rational number. */
internal class Surd(val coefficient: Rational, val radicand: Long) {
  val isZero: Boolean
    get() = coefficient.isZero

  val isRational: Boolean
    get() = radicand == 1L || isZero

  operator fun unaryMinus() = Surd(-coefficient, radicand)

  /** null when radicands are different: √2+√3 can't be simplified. */
  operator fun plus(other: Surd): Surd? =
    when {
      isZero -> other
      other.isZero -> this
      radicand == other.radicand -> Surd(coefficient + other.coefficient, radicand).normalized()
      else -> null
    }

  operator fun times(other: Surd): Surd? {
    if (isZero || other.isZero) return ZERO
    // √a·√b = g·√(ab/g²) where g = gcd(a, b) (both square free)
    val gcd = gcd(radicand, other.radicand)
    val product = (radicand / gcd).timesOrNull(other.radicand / gcd) ?: return null
    return Surd(coefficient * other.coefficient * Rational.of(gcd), product)
  }

  private fun normalized() = if (coefficient.isZero) ZERO else this

  companion object {
    val ZERO = Surd(Rational.ZERO, 1)

    fun rational(value: Rational) = Surd(value, 1)

    /** √value for positive rational. null when numbers are too big. */
    fun sqrt(value: Rational): Surd? {
      // √(p/q) = √(pq)/q
      val p = value.numerator.toLongOrNull() ?: return null
      val q = value.denominator.toLongOrNull() ?: return null
      val pq = p.timesOrNull(q) ?: return null
      if (pq > MAX_RADICAND) return null
      var square = 1L
      var rest = pq
      var factor = 2L
      while (factor * factor <= rest) {
        while (rest % (factor * factor) == 0L) {
          rest /= factor * factor
          square *= factor
        }
        factor++
      }
      return Surd(Rational.of(square) / Rational.of(q), rest)
    }
  }
}

/** Exact complex number: real and imaginary parts are [Surd]s. */
internal class ExactValue(val re: Surd, val im: Surd = Surd.ZERO) {
  val isRealRational: Boolean
    get() = im.isZero && re.isRational

  /** Has a square root that decimals can't show exactly. */
  val hasSurd: Boolean
    get() = !re.isRational || !im.isRational

  operator fun unaryMinus() = ExactValue(-re, -im)

  operator fun plus(other: ExactValue): ExactValue? {
    val re = (re + other.re) ?: return null
    val im = (im + other.im) ?: return null
    return ExactValue(re, im)
  }

  operator fun minus(other: ExactValue) = this + -other

  operator fun times(other: ExactValue): ExactValue? {
    // (a+bi)(c+di) = (ac−bd) + (ad+bc)i
    val ac = (re * other.re) ?: return null
    val bd = (im * other.im) ?: return null
    val ad = (re * other.im) ?: return null
    val bc = (im * other.re) ?: return null
    return ExactValue((ac + -bd) ?: return null, (ad + bc) ?: return null)
  }

  fun divide(other: ExactValue): ExactValue? {
    val divisor = other.re
    if (other.im.isZero && !divisor.isZero) {
      // x / (c√r) = x·√r / (c·r)
      val inverse = Surd(Rational.ONE / (divisor.coefficient * Rational.of(divisor.radicand)), divisor.radicand)
      return this * ExactValue(inverse)
    }
    if (other.re.isRational && other.im.isRational) {
      // multiply by conjugate: x / (a+bi) = x·(a−bi) / (a²+b²)
      val a = other.re.coefficient
      val b = other.im.coefficient
      val norm = a * a + b * b
      if (norm.isZero) throw ExpressionException.DivideByZero()
      val conjugate = ExactValue(Surd.rational(a / norm), Surd.rational(-b / norm))
      return this * conjugate
    }
    return null
  }

  companion object {
    val ONE = ExactValue(Surd.rational(Rational.ONE))
  }
}

/**
 * Exact value of [this] when it uses only numbers, `i`, + − × ÷, whole powers, square roots and
 * sin, cos, tan of special angles (multiples of 30° and 45°). null otherwise (π alone, other
 * functions, x and so on).
 *
 * @param radianMode Angle mode for sin, cos and tan without an angle box.
 */
internal fun Node.exactValue(radianMode: Boolean = true): ExactValue? =
  when (this) {
    is Node.Number -> ExactValue(Surd.rational(Rational.fromDecimal(value)))
    Node.ImaginaryUnit -> ExactValue(Surd.ZERO, Surd.rational(Rational.ONE))
    is Node.Negate -> node.exactValue(radianMode)?.let { -it }
    is Node.Binary -> {
      val left = left.exactValue(radianMode)
      val right = right.exactValue(radianMode)
      if (left == null || right == null) {
        null
      } else {
        when (operator) {
          BinaryOperator.PLUS -> left + right
          BinaryOperator.MINUS -> left - right
          BinaryOperator.MULTIPLY -> left * right
          BinaryOperator.DIVIDE -> left.divide(right)
          BinaryOperator.POWER -> left.pow(right)
          BinaryOperator.MODULO -> null
        }
      }
    }
    is Node.Function ->
      when (function) {
        FunctionType.SQRT -> argument.exactValue(radianMode)?.sqrt()
        FunctionType.SIN,
        FunctionType.COS,
        FunctionType.TAN -> exactTrigonometric(radianMode)
        else -> null
      }
    else -> null
  }

/** sin, cos and tan of multiples of 30° and 45°: cos(45°) = √2/2, sin(π/6) = 1/2. */
private fun Node.Function.exactTrigonometric(radianMode: Boolean): ExactValue? {
  val degrees =
    when {
      angle == PhysicalUnits.degree -> argument.exactRational(radianMode)
      angle == PhysicalUnits.radian -> argument.piMultiple(radianMode)?.times(HALF_TURN)
      angle != null -> null
      radianMode -> argument.piMultiple(radianMode)?.times(HALF_TURN)
      else -> argument.exactRational(radianMode)
    } ?: return null
  if (!degrees.isInteger) return null
  val whole = degrees.numerator.toLongOrNull() ?: return null
  val sin = exactSine(whole) ?: return null
  val cos = exactSine(whole + RIGHT_ANGLE) ?: return null
  return when (function) {
    FunctionType.SIN -> sin
    FunctionType.COS -> cos
    // tan(90°) does not exist
    else -> if (cos.re.isZero) null else sin.divide(cos)
  }
}

/** sin of [degrees] when it is a multiple of 30° or 45°. */
private fun exactSine(degrees: Long): ExactValue? {
  val angle = ((degrees % FULL_TURN) + FULL_TURN) % FULL_TURN
  // sin(180°+a) = −sin(a), sin(180°−a) = sin(a)
  val sign = if (angle >= 180) -1 else 1
  val inHalf = angle % 180
  val firstQuadrant = if (inHalf > RIGHT_ANGLE) 180 - inHalf else inHalf
  val surd =
    when (firstQuadrant) {
      0L -> Surd.ZERO
      30L -> Surd.rational(Rational.ONE / TWO)
      45L -> Surd(Rational.ONE / TWO, 2)
      60L -> Surd(Rational.ONE / TWO, 3)
      90L -> Surd.rational(Rational.ONE)
      else -> return null
    }
  return ExactValue(if (sign < 0) -surd else surd)
}

/** Rational value, null when it is not one. */
private fun Node.exactRational(radianMode: Boolean): Rational? =
  exactValue(radianMode)?.takeIf { it.isRealRational }?.re?.coefficient

/** k when [this] is k·π with a rational k: π/2, 2π, π+π/3. */
private fun Node.piMultiple(radianMode: Boolean): Rational? =
  when (this) {
    Node.Pi -> Rational.ONE
    is Node.Negate -> node.piMultiple(radianMode)?.let { -it }
    is Node.Binary ->
      when (operator) {
        BinaryOperator.MULTIPLY ->
          left.piMultiple(radianMode)?.let { pi -> right.exactRational(radianMode)?.let { pi * it } }
            ?: right.piMultiple(radianMode)?.let { pi -> left.exactRational(radianMode)?.let { pi * it } }
        BinaryOperator.DIVIDE -> {
          val pi = left.piMultiple(radianMode)
          val divisor = right.exactRational(radianMode)
          if (pi == null || divisor == null || divisor.isZero) null else pi / divisor
        }
        BinaryOperator.PLUS ->
          left.piMultiple(radianMode)?.let { a -> right.piMultiple(radianMode)?.let { a + it } }
        BinaryOperator.MINUS ->
          left.piMultiple(radianMode)?.let { a -> right.piMultiple(radianMode)?.let { a - it } }
        else -> null
      }
    else -> null
  }

private val HALF_TURN = Rational.of(KBigDecimal("180"))
private val TWO = Rational.of(KBigDecimal("2"))
private const val RIGHT_ANGLE = 90L
private const val FULL_TURN = 360L

private fun ExactValue.sqrt(): ExactValue? {
  if (!isRealRational) return null
  val value = re.coefficient
  return when {
    value.isZero -> this
    value.sign > 0 -> ExactValue(Surd.sqrt(value) ?: return null)
    else -> ExactValue(Surd.ZERO, Surd.sqrt(-value) ?: return null)
  }
}

private fun ExactValue.pow(exponent: ExactValue): ExactValue? {
  if (!exponent.isRealRational) return null
  val power = exponent.re.coefficient
  if (!power.isInteger) return null
  val n = power.numerator.toLongOrNull() ?: return null
  if (n !in -MAX_EXACT_POWER..MAX_EXACT_POWER) return null
  var result = ExactValue.ONE
  repeat(kotlin.math.abs(n).toInt()) { result = (result * this) ?: return null }
  return if (n < 0) ExactValue.ONE.divide(result) else result
}

private fun KBigDecimal.toLongOrNull(): Long? = setScale(0).toPlainString().toLongOrNull()

private fun Long.timesOrNull(other: Long): Long? {
  if (this == 0L || other == 0L) return 0L
  val result = this * other
  return if (result / other == this) result else null
}

private fun gcd(a: Long, b: Long): Long {
  var x = kotlin.math.abs(a)
  var y = kotlin.math.abs(b)
  while (y != 0L) {
    val t = x % y
    x = y
    y = t
  }
  return x
}

private const val MAX_RADICAND = 1_000_000_000_000L
private const val MAX_EXACT_POWER = 32L
