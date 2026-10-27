/*
 * Unitto is a calculator for Android
 * Copyright (c) 2026 Elshan Agaev
 * Copyright (c) 2026 Calculator-Pascaline (modifications)
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
import com.sadellie.unitto.core.common.MAX_SCALE
import com.sadellie.unitto.core.common.isEqualTo

/** Expression has a variable or a symbolic operation, can't be calculated as a number. */
internal class SymbolicExpressionException : Exception("Expression is not a number")

/** Complex number. Most calculations are real, so real-only fast paths are kept. */
internal class Complex(val re: KBigDecimal, val im: KBigDecimal = KBigDecimal.ZERO) {
  val isReal: Boolean
    get() = im.isEqualTo(KBigDecimal.ZERO)

  /** Real value, throws for numbers with imaginary part. */
  fun real(): KBigDecimal = if (isReal) re else throw ExpressionException.BadExpression()

  fun isZero() = isReal && re.isEqualTo(KBigDecimal.ZERO)

  operator fun plus(other: Complex) =
    if (isReal && other.isReal) Complex(re + other.re) else Complex(re + other.re, im + other.im)

  operator fun minus(other: Complex) =
    if (isReal && other.isReal) Complex(re - other.re) else Complex(re - other.re, im - other.im)

  operator fun unaryMinus() = if (isReal) Complex(-re) else Complex(-re, -im)

  operator fun times(other: Complex): Complex {
    if (isReal && other.isReal) return Complex(re.multiply(other.re))
    return Complex(
      re.multiply(other.re) - im.multiply(other.im),
      re.multiply(other.im) + im.multiply(other.re),
    )
  }

  fun divide(other: Complex, scale: Int, roundingMode: KRoundingMode): Complex {
    if (other.isZero()) throw ExpressionException.DivideByZero()
    if (isReal && other.isReal) return Complex(re.divide(other.re, scale, roundingMode))
    val denominator = other.re.multiply(other.re) + other.im.multiply(other.im)
    return Complex(
      (re.multiply(other.re) + im.multiply(other.im)).divide(denominator, scale, roundingMode),
      (im.multiply(other.re) - re.multiply(other.im)).divide(denominator, scale, roundingMode),
    )
  }

  /** Round both parts to [scale] to stop numbers from growing in long calculations. */
  fun rounded(scale: Int) =
    Complex(re.setScale(scale, KRoundingMode.HALF_EVEN), im.setScale(scale, KRoundingMode.HALF_EVEN))
}

internal class Evaluator(
  private val radianMode: Boolean,
  private val roundingMode: KRoundingMode,
) {
  private val scale = MAX_SCALE
  private val mathContext = KMathContext(TRIG_SCALE, roundingMode)

  /** Division scale. Lower inside of integrals and series, they evaluate thousands of points. */
  private var workingScale = scale

  /** Value of [Node.N] in current sum or product. */
  private var index: Complex? = null

  /** Evaluations left for integrals and series. Nested ones can take forever. */
  private var evaluationsLeft = MAX_EVALUATIONS

  private fun spendEvaluation() {
    evaluationsLeft--
    if (evaluationsLeft < 0) throw ExpressionException.TooBig()
  }

  /**
   * @param x Value of [Node.X]. `null` when expression is not inside of an integral or derivative.
   * @throws SymbolicExpressionException When [Node.X] is used without value.
   */
  fun evaluate(node: Node, x: Complex? = null): Complex =
    when (node) {
      is Node.Number -> Complex(node.value)
      Node.Pi -> Complex(KBigDecimalMath.pi(mathContext))
      Node.Euler -> Complex(KBigDecimalMath.e(mathContext))
      Node.ImaginaryUnit -> Complex(KBigDecimal.ZERO, KBigDecimal.ONE)
      Node.X -> x ?: throw SymbolicExpressionException()
      Node.N -> index ?: throw ExpressionException.BadExpression()
      // Only allowed in bounds, see bound()
      Node.Infinity -> throw ExpressionException.BadExpression()
      // °C and °F only in simple conversions, see Expression
      is Node.Unit ->
        if (node.unit.isAffine) throw ExpressionException.BadExpression()
        else Complex(KBigDecimal(node.unit.factor))
      // Only at the top, see Expression
      is Node.Convert -> throw ExpressionException.BadExpression()
      is Node.Negate -> -evaluate(node.node, x)
      is Node.Binary -> evaluateBinary(node, x)
      is Node.Function -> evaluateFunction(node.function, evaluate(node.argument, x))
      is Node.Log -> evaluateLog(evaluate(node.base, x).real(), evaluate(node.argument, x).real())
      is Node.Factorial -> {
        if (node.isNegated) throw ExpressionException.FactorialCalculation()
        Complex(evaluate(node.node, x).real().factorial())
      }
      is Node.Integral -> {
        val lower = node.lower ?: throw SymbolicExpressionException()
        val upper = node.upper ?: throw SymbolicExpressionException()
        withLowerScale { integrate(node.body, bound(lower, x), bound(upper, x)) }
      }
      is Node.Series -> withLowerScale { evaluateSeries(node, x) }
      is Node.Derivative -> {
        val at = node.at ?: throw SymbolicExpressionException()
        differentiate(node.body, evaluate(at, x).real())
      }
    }

  private fun evaluateBinary(node: Node.Binary, x: Complex?): Complex {
    val left = evaluate(node.left, x)
    val right = evaluate(node.right, x)
    return when (node.operator) {
      BinaryOperator.PLUS -> left + right
      BinaryOperator.MINUS -> left - right
      BinaryOperator.MULTIPLY -> left * right
      BinaryOperator.DIVIDE -> left.divide(right, workingScale, roundingMode)
      BinaryOperator.POWER -> power(left, right)
      BinaryOperator.MODULO -> Complex(left.real().remainder(right.real()))
    }
  }

  private fun power(base: Complex, exponent: Complex): Complex {
    if (base.isReal && exponent.isReal) {
      // mathematicians made up this controversy because reasons
      if (base.isZero() && exponent.isZero()) throw ExpressionException.BadExpression()
      return Complex(KBigDecimalMath.pow(base.re, exponent.re, mathContext))
    }

    // Complex base: only whole exponents
    val power = exponent.real()
    if (!power.remainder(KBigDecimal.ONE).isEqualTo(KBigDecimal.ZERO)) {
      throw ExpressionException.BadExpression()
    }
    val n = power.toInt()
    if (n !in -MAX_COMPLEX_POWER..MAX_COMPLEX_POWER) throw ExpressionException.TooBig()
    var result = Complex(KBigDecimal.ONE)
    var square = base
    var remaining = kotlin.math.abs(n)
    while (remaining > 0) {
      if (remaining % 2 == 1) result = (result * square).rounded(COMPLEX_SCALE)
      square = (square * square).rounded(COMPLEX_SCALE)
      remaining /= 2
    }
    return if (n < 0) Complex(KBigDecimal.ONE).divide(result, scale, roundingMode) else result
  }

  private fun evaluateFunction(function: FunctionType, argument: Complex): Complex {
    if (function == FunctionType.SQRT) return sqrt(argument)
    if (function == FunctionType.ABS) {
      if (argument.isReal) return Complex(argument.re.abs())
      val square = argument.re.multiply(argument.re) + argument.im.multiply(argument.im)
      return Complex(KBigDecimalMath.sqrt(square, mathContext))
    }

    val value = argument.real()
    val result =
      when (function) {
        FunctionType.SQRT,
        FunctionType.ABS -> error("handled above")
        FunctionType.SIN -> value.sin(radianMode, mathContext).rescaleTrig()
        FunctionType.COS -> value.cos(radianMode, mathContext).rescaleTrig()
        FunctionType.TAN -> value.tan(radianMode, mathContext).rescaleTrig()
        FunctionType.ARSIN -> value.arsin(radianMode, mathContext).rescaleTrig()
        FunctionType.ARCOS -> value.arcos(radianMode, mathContext).rescaleTrig()
        FunctionType.ARTAN -> value.artan(radianMode, mathContext).rescaleTrig()
        FunctionType.LN -> value.ln(mathContext)
        FunctionType.LOG -> value.log(mathContext)
        FunctionType.EXP -> value.exp(mathContext)
        FunctionType.SINH -> (value.exp(mathContext) - (-value).exp(mathContext)).half()
        FunctionType.COSH -> (value.exp(mathContext) + (-value).exp(mathContext)).half()
        FunctionType.TANH -> {
          val plus = value.exp(mathContext)
          val minus = (-value).exp(mathContext)
          (plus - minus).divide(plus + minus, mathContext)
        }
        // ln(x+√(x²+1))
        FunctionType.ARSINH ->
          (value + KBigDecimalMath.sqrt(value.multiply(value) + KBigDecimal.ONE, mathContext))
            .ln(mathContext)
        // ln(x+√(x²−1)), x ≥ 1
        FunctionType.ARCOSH -> {
          if (value < KBigDecimal.ONE) throw ExpressionException.BadExpression()
          (value + KBigDecimalMath.sqrt(value.multiply(value) - KBigDecimal.ONE, mathContext))
            .ln(mathContext)
        }
        // ln((1+x)/(1−x))/2, |x| < 1
        FunctionType.ARTANH -> {
          if (value.abs() >= KBigDecimal.ONE) throw ExpressionException.BadExpression()
          (KBigDecimal.ONE + value)
            .divide(KBigDecimal.ONE - value, mathContext)
            .ln(mathContext)
            .half()
        }
      }
    return Complex(result)
  }

  /** Principal square root. Negative numbers give imaginary results: √−4 = 2i */
  private fun sqrt(value: Complex): Complex {
    if (value.isReal) {
      if (value.re >= KBigDecimal.ZERO) return Complex(KBigDecimalMath.sqrt(value.re, mathContext))
      return Complex(KBigDecimal.ZERO, KBigDecimalMath.sqrt(-value.re, mathContext))
    }
    // √(a+bi) = √((|z|+a)/2) + i·sign(b)·√((|z|−a)/2)
    val two = KBigDecimal.valueOf(2)
    val modulus =
      KBigDecimalMath.sqrt(value.re.multiply(value.re) + value.im.multiply(value.im), mathContext)
    val re = KBigDecimalMath.sqrt((modulus + value.re).divide(two, mathContext), mathContext)
    val im = KBigDecimalMath.sqrt((modulus - value.re).divide(two, mathContext), mathContext)
    return Complex(re, if (value.im < KBigDecimal.ZERO) -im else im)
  }

  private fun evaluateLog(base: KBigDecimal, argument: KBigDecimal): Complex {
    if (base.isEqualTo(KBigDecimal.TEN)) return Complex(argument.log(mathContext))
    val lnBase = base.ln(mathContext)
    if (lnBase.isEqualTo(KBigDecimal.ZERO)) throw ExpressionException.DivideByZero()
    return Complex(argument.ln(mathContext).divide(lnBase, workingScale, roundingMode))
  }

  private sealed interface Bound {
    class Finite(val value: KBigDecimal) : Bound

    data object PlusInfinity : Bound

    data object MinusInfinity : Bound
  }

  /** Bound of integral or series: a number, ∞ or −∞. */
  private fun bound(node: Node, x: Complex?): Bound =
    when {
      node == Node.Infinity -> Bound.PlusInfinity
      node is Node.Negate && node.node == Node.Infinity -> Bound.MinusInfinity
      else -> Bound.Finite(evaluate(node, x).real())
    }

  private inline fun <T> withLowerScale(block: () -> T): T {
    val previous = workingScale
    workingScale = minOf(previous, POINT_SCALE)
    try {
      return block()
    } finally {
      workingScale = previous
    }
  }

  private fun integrate(body: Node, lower: Bound, upper: Bound): Complex {
    fun f(point: KBigDecimal) = evaluate(body, Complex(point.setScale(POINT_SCALE, KRoundingMode.HALF_EVEN)))

    return when {
      lower is Bound.Finite && upper is Bound.Finite -> integrate(lower.value, upper.value, ::f)
      lower is Bound.Finite && upper == Bound.PlusInfinity -> integrateToInfinity(lower.value, 1, ::f)
      lower == Bound.MinusInfinity && upper is Bound.Finite ->
        integrateToInfinity(upper.value, -1, ::f)
      lower == Bound.MinusInfinity && upper == Bound.PlusInfinity ->
        integrateToInfinity(KBigDecimal.ZERO, 1, ::f) + integrateToInfinity(KBigDecimal.ZERO, -1, ::f)
      // reversed bounds
      else -> -integrate(body, upper, lower)
    }
  }

  /**
   * ∫ from [start] to ±∞ ([direction]) with substitution x = start ± t/(1−t), t from 0 to 1. Gauss–
   * Legendre nodes never reach t = 1.
   */
  private fun integrateToInfinity(
    start: KBigDecimal,
    direction: Int,
    f: (KBigDecimal) -> Complex,
  ): Complex {
    val result =
      integrate(KBigDecimal.ZERO, KBigDecimal.ONE) { t ->
        val rest = KBigDecimal.ONE - t
        val offset = t.divide(rest, mathContext)
        val point = if (direction > 0) start + offset else start - offset
        val jacobian = KBigDecimal.ONE.divide(rest.multiply(rest), mathContext)
        f(point) * Complex(jacobian)
      }
    return result
  }

  /**
   * Composite Gauss–Legendre quadrature. Number of intervals is doubled until two results agree.
   */
  private fun integrate(lower: KBigDecimal, upper: KBigDecimal, f: (KBigDecimal) -> Complex): Complex {
    if (lower.isEqualTo(upper)) return Complex(KBigDecimal.ZERO)

    var previous = gaussLegendre(lower, upper, INITIAL_INTERVALS, f)
    var intervals = INITIAL_INTERVALS * 2
    while (intervals <= MAX_INTERVALS) {
      val current = gaussLegendre(lower, upper, intervals, f)
      val difference = (current - previous).abs()
      val magnitude = maxOf(current.abs(), KBigDecimal.ONE)
      if (difference <= magnitude.multiply(INTEGRAL_TOLERANCE)) return current
      previous = current
      intervals *= 2
    }
    return previous
  }

  private fun gaussLegendre(
    lower: KBigDecimal,
    upper: KBigDecimal,
    intervals: Int,
    f: (KBigDecimal) -> Complex,
  ): Complex {
    val two = KBigDecimal.valueOf(2)
    val width = (upper - lower).divide(KBigDecimal.valueOf(intervals.toLong()), mathContext)
    val halfWidth = width.divide(two, mathContext)
    var sum = Complex(KBigDecimal.ZERO)
    for (interval in 0..<intervals) {
      val middle = lower + width.multiply(KBigDecimal.valueOf(interval.toLong())) + halfWidth
      for ((node, weight) in gaussLegendreNodes) {
        val offset = halfWidth.multiply(node)
        for (point in listOf(middle - offset, middle + offset)) {
          spendEvaluation()
          sum = (sum + f(point) * Complex(weight)).rounded(POINT_SCALE)
        }
      }
    }
    return (sum * Complex(halfWidth)).rounded(POINT_SCALE)
  }

  /**
   * Σ or ∏ of body for n from lower to upper. Infinite series use partial results for 2, 4, 8, …
   * terms and Richardson extrapolation (they behave like S + a/N + b/N² + …), so slow series like
   * Σ1/n² still give many correct digits.
   */
  private fun evaluateSeries(node: Node.Series, x: Complex?): Complex {
    val lower = bound(node.lower, x) as? Bound.Finite ?: throw ExpressionException.BadExpression()
    val start = lower.value.wholeOrNull() ?: throw ExpressionException.BadExpression()
    val upper = bound(node.upper, x)
    val previousIndex = index
    try {
      var result = Complex(if (node.isProduct) KBigDecimal.ONE else KBigDecimal.ZERO)
      fun addTerm(n: Long): Complex {
        spendEvaluation()
        index = Complex(KBigDecimal.valueOf(n))
        val term = evaluate(node.body, x)
        result =
          (if (node.isProduct) result * term else result + term).rounded(POINT_SCALE)
        return term
      }

      when (upper) {
        is Bound.Finite -> {
          val end = upper.value.wholeOrNull() ?: throw ExpressionException.BadExpression()
          if (end - start > MAX_SERIES_TERMS) throw ExpressionException.TooBig()
          for (n in start..end) addTerm(n)
          return result
        }
        Bound.PlusInfinity -> Unit
        Bound.MinusInfinity -> throw ExpressionException.BadExpression()
      }

      // Infinite
      val partial = mutableListOf<Complex>()
      var n = start
      var tinyTerms = 0
      for (level in 0..SERIES_LEVELS) {
        val count = 1L shl level
        while (n < start + count) {
          val term = addTerm(n)
          val change = if (node.isProduct) (term - Complex(KBigDecimal.ONE)).abs() else term.abs()
          tinyTerms = if (change <= SERIES_TINY.multiply(maxOf(result.abs(), KBigDecimal.ONE))) tinyTerms + 1 else 0
          n++
          // converged (fast series like Σ1/n!)
          if (tinyTerms >= SERIES_TINY_TERMS) return result
        }
        partial += result
      }
      return extrapolate(partial.drop(SERIES_SKIPPED_LEVELS))
    } finally {
      index = previousIndex
    }
  }

  /** Richardson extrapolation for results at N, 2N, 4N, … */
  private fun extrapolate(values: List<Complex>): Complex {
    // Diverges when differences don't get smaller (like Σ1/n)
    val last = (values[values.size - 1] - values[values.size - 2]).abs()
    val beforeLast = (values[values.size - 2] - values[values.size - 3]).abs()
    if (last > SERIES_TINY && last > beforeLast.multiply(DIVERGENCE_RATIO)) {
      throw ExpressionException.BadExpression()
    }

    var row = values
    var factor = KBigDecimal.ONE
    val two = KBigDecimal.valueOf(2)
    while (row.size > 1) {
      factor = factor.multiply(two)
      val denominator = Complex(factor - KBigDecimal.ONE)
      row =
        row.zipWithNext { previous, next ->
          (next * Complex(factor) - previous).divide(denominator, POINT_SCALE, roundingMode)
        }
    }
    return row.first().rounded(SERIES_RESULT_SCALE)
  }

  private fun KBigDecimal.wholeOrNull(): Long? {
    val whole = setScale(0, KRoundingMode.HALF_EVEN)
    if (!whole.isEqualTo(this)) return null
    return whole.toPlainString().toLongOrNull()
  }

  private fun KBigDecimal.half() = divide(KBigDecimal.valueOf(2), mathContext)

  /** Central difference. Calculations are very precise, so a tiny step works. */
  private fun differentiate(body: Node, at: KBigDecimal): Complex {
    val step = DERIVATIVE_STEP
    val after = evaluate(body, Complex(at + step))
    val before = evaluate(body, Complex(at - step))
    return (after - before)
      .divide(Complex(step.multiply(KBigDecimal.valueOf(2))), scale, roundingMode)
      .rounded(DERIVATIVE_SCALE)
  }

  private fun Complex.abs(): KBigDecimal = if (isReal) re.abs() else re.abs() + im.abs()

  // rescale to avoid precision loss when evaluating special cases in trigonometry
  private fun KBigDecimal.rescaleTrig(): KBigDecimal =
    this.setScale(mathContext.precision, KRoundingMode.HALF_EVEN)
}

/** 10 point Gauss–Legendre nodes (positive half) and weights. */
private val gaussLegendreNodes =
  listOf(
      "0.97390652851717172007796401208445" to "0.06667134430868813759356880989333",
      "0.86506336668898451073209668842349" to "0.14945134915058059314577633965770",
      "0.67940956829902440623432736511487" to "0.21908636251598204399553493422816",
      "0.43339539412924719079926594316578" to "0.26926671930999635509122692156947",
      "0.14887433898163121088482600112972" to "0.29552422471475287017389299465134",
    )
    .map { (node, weight) -> KBigDecimal(node) to KBigDecimal(weight) }

private val INTEGRAL_TOLERANCE = KBigDecimal("1E-25")
private val DERIVATIVE_STEP = KBigDecimal("1E-20")
private const val INITIAL_INTERVALS = 2
private const val MAX_INTERVALS = 64
private const val POINT_SCALE = 60
private const val DERIVATIVE_SCALE = 40
private const val COMPLEX_SCALE = 100
private const val MAX_COMPLEX_POWER = 10_000
private const val MAX_SERIES_TERMS = 100_000L
private const val MAX_EVALUATIONS = 200_000
/** Infinite series use up to 2^SERIES_LEVELS terms. */
private const val SERIES_LEVELS = 12
/** First partial results are too far from asymptotic behaviour. */
private const val SERIES_SKIPPED_LEVELS = 2
private const val SERIES_TINY_TERMS = 8
private const val SERIES_RESULT_SCALE = 40
private val SERIES_TINY = KBigDecimal("1E-45")
private val DIVERGENCE_RATIO = KBigDecimal("0.75")
internal const val TRIG_SCALE = 100
