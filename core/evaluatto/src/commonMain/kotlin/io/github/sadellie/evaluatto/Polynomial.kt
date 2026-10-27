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
import com.sadellie.unitto.core.common.KRoundingMode
import com.sadellie.unitto.core.common.isEqualTo

/** Exact fraction. Numerator and denominator are whole numbers, denominator is positive. */
internal class Rational private constructor(val numerator: KBigDecimal, val denominator: KBigDecimal) :
  Comparable<Rational> {

  val isZero: Boolean
    get() = numerator.isEqualTo(KBigDecimal.ZERO)

  val isInteger: Boolean
    get() = denominator.isEqualTo(KBigDecimal.ONE)

  val sign: Int
    get() = numerator.compareTo(KBigDecimal.ZERO)

  operator fun plus(other: Rational) =
    of(
      numerator.multiply(other.denominator) + other.numerator.multiply(denominator),
      denominator.multiply(other.denominator),
    )

  operator fun minus(other: Rational) = this + -other

  operator fun unaryMinus() = Rational(-numerator, denominator)

  operator fun times(other: Rational) =
    of(numerator.multiply(other.numerator), denominator.multiply(other.denominator))

  operator fun div(other: Rational): Rational {
    if (other.isZero) throw ExpressionException.DivideByZero()
    return of(numerator.multiply(other.denominator), denominator.multiply(other.numerator))
  }

  fun abs() = if (sign < 0) -this else this

  fun toKBigDecimal(scale: Int): KBigDecimal =
    numerator.divide(denominator, scale, KRoundingMode.HALF_EVEN)

  fun toDouble(): Double = toKBigDecimal(DOUBLE_SCALE).toString().toDouble()

  override fun compareTo(other: Rational): Int =
    numerator.multiply(other.denominator).compareTo(other.numerator.multiply(denominator))

  override fun equals(other: Any?): Boolean = other is Rational && compareTo(other) == 0

  override fun hashCode(): Int = toDouble().hashCode()

  companion object {
    val ZERO = Rational(KBigDecimal.ZERO, KBigDecimal.ONE)
    val ONE = Rational(KBigDecimal.ONE, KBigDecimal.ONE)

    fun of(numerator: KBigDecimal, denominator: KBigDecimal = KBigDecimal.ONE): Rational {
      if (denominator.isEqualTo(KBigDecimal.ZERO)) throw ExpressionException.DivideByZero()
      var num = numerator.setScale(0)
      var den = denominator.setScale(0)
      if (den < KBigDecimal.ZERO) {
        num = -num
        den = -den
      }
      val gcd = gcd(num, den)
      if (gcd.isEqualTo(KBigDecimal.ZERO) || gcd.isEqualTo(KBigDecimal.ONE)) return Rational(num, den)
      return Rational(num.exactDivide(gcd), den.exactDivide(gcd))
    }

    fun of(value: Long) = of(KBigDecimal.valueOf(value))

    /** Exact value of a decimal, rounded to [CONSTANT_SCALE] digits after the dot. */
    fun fromDecimal(value: KBigDecimal): Rational {
      val rounded = value.setScale(CONSTANT_SCALE, KRoundingMode.HALF_EVEN).stripTrailingZeros()
      val scale = rounded.scale()
      if (scale <= 0) return of(rounded.setScale(0))
      return of(rounded.scaleByPowerOfTen(scale).setScale(0), KBigDecimal.TEN.pow(scale))
    }
  }
}

internal fun gcd(a: KBigDecimal, b: KBigDecimal): KBigDecimal =
  a.abs().toBigInteger().gcd(b.abs().toBigInteger()).toKBigDecimal().setScale(0)

/** Both numbers are whole and [divisor] divides this. */
internal fun KBigDecimal.exactDivide(divisor: KBigDecimal): KBigDecimal =
  divide(divisor, 0, KRoundingMode.HALF_EVEN)

/** Polynomial in x. Coefficient at index i is for x^i. No trailing zeros. */
internal class Polynomial(coefficients: List<Rational>) {
  val coefficients: List<Rational> = coefficients.dropLastWhile { it.isZero }

  val degree: Int
    get() = coefficients.size - 1

  val leading: Rational
    get() = coefficients.lastOrNull() ?: Rational.ZERO

  val isZero: Boolean
    get() = coefficients.isEmpty()

  operator fun get(power: Int): Rational = coefficients.getOrNull(power) ?: Rational.ZERO

  operator fun plus(other: Polynomial) =
    Polynomial(List(maxOf(coefficients.size, other.coefficients.size)) { this[it] + other[it] })

  operator fun minus(other: Polynomial) = this + -other

  operator fun unaryMinus() = Polynomial(coefficients.map { -it })

  operator fun times(other: Polynomial): Polynomial {
    if (isZero || other.isZero) return Polynomial(emptyList())
    val result = MutableList(coefficients.size + other.coefficients.size - 1) { Rational.ZERO }
    coefficients.forEachIndexed { i, a ->
      other.coefficients.forEachIndexed { j, b -> result[i + j] = result[i + j] + a * b }
    }
    return Polynomial(result)
  }

  operator fun times(constant: Rational) = Polynomial(coefficients.map { it * constant })

  fun pow(n: Int): Polynomial {
    var result = constant(Rational.ONE)
    repeat(n) { result *= this }
    return result
  }

  fun derivative() =
    Polynomial(coefficients.drop(1).mapIndexed { index, c -> c * Rational.of(index + 1L) })

  /** Without integration constant. */
  fun antiderivative() =
    Polynomial(listOf(Rational.ZERO) + coefficients.mapIndexed { i, c -> c / Rational.of(i + 1L) })

  fun evaluate(x: Rational): Rational =
    coefficients.foldRight(Rational.ZERO) { coefficient, acc -> acc * x + coefficient }

  /** @return Quotient or null when [divisor] doesn't divide this polynomial. */
  fun divideExact(divisor: Polynomial): Polynomial? {
    if (divisor.isZero) return null
    val remainder = coefficients.toMutableList()
    val quotient = MutableList(maxOf(degree - divisor.degree + 1, 0)) { Rational.ZERO }
    for (power in degree - divisor.degree downTo 0) {
      val factor = remainder[power + divisor.degree] / divisor.leading
      quotient[power] = factor
      divisor.coefficients.forEachIndexed { i, c ->
        remainder[power + i] = remainder[power + i] - factor * c
      }
    }
    return if (remainder.all { it.isZero }) Polynomial(quotient) else null
  }

  companion object {
    val X = Polynomial(listOf(Rational.ZERO, Rational.ONE))

    fun constant(value: Rational) = Polynomial(listOf(value))
  }
}

/**
 * Converts [node] into polynomial. Parts without x are calculated by [evaluator].
 *
 * @throws ExpressionException.BadExpression When [node] is not a polynomial.
 */
internal fun Node.toPolynomial(evaluator: Evaluator): Polynomial {
  if (!dependsOnX()) return Polynomial.constant(Rational.fromDecimal(evaluator.evaluate(this).real()))

  return when (this) {
    Node.X -> Polynomial.X
    is Node.Negate -> -node.toPolynomial(evaluator)
    is Node.Binary -> {
      val left = left.toPolynomial(evaluator)
      when (operator) {
        BinaryOperator.PLUS -> left + right.toPolynomial(evaluator)
        BinaryOperator.MINUS -> left - right.toPolynomial(evaluator)
        BinaryOperator.MULTIPLY -> left * right.toPolynomial(evaluator)
        BinaryOperator.DIVIDE ->
          left.divideExact(right.toPolynomial(evaluator)) ?: throw ExpressionException.BadExpression()
        BinaryOperator.POWER -> {
          if (right.dependsOnX()) throw ExpressionException.BadExpression()
          val exponent = evaluator.evaluate(right).real()
          val isWhole = exponent.remainder(KBigDecimal.ONE).isEqualTo(KBigDecimal.ZERO)
          if (!isWhole || exponent < KBigDecimal.ZERO) throw ExpressionException.BadExpression()
          if (exponent > KBigDecimal.valueOf(MAX_POLYNOMIAL_DEGREE.toLong())) {
            throw ExpressionException.TooBig()
          }
          left.pow(exponent.toInt())
        }
        BinaryOperator.MODULO -> throw ExpressionException.BadExpression()
      }
    }
    // Only symbolic ones. Bounds or point with x are not supported
    is Node.Integral ->
      if (lower == null) body.toPolynomial(evaluator).antiderivative()
      else throw ExpressionException.BadExpression()
    is Node.Derivative ->
      if (at == null) body.toPolynomial(evaluator).derivative()
      else throw ExpressionException.BadExpression()
    else -> throw ExpressionException.BadExpression()
  }
}

private const val CONSTANT_SCALE = 30
private const val DOUBLE_SCALE = 20
internal const val MAX_POLYNOMIAL_DEGREE = 100
