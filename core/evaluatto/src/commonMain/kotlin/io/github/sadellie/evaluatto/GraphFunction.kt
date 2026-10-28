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
import kotlin.math.E
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.acosh
import kotlin.math.asin
import kotlin.math.asinh
import kotlin.math.atan
import kotlin.math.atanh
import kotlin.math.cbrt
import kotlin.math.cos
import kotlin.math.cosh
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sinh
import kotlin.math.sqrt
import kotlin.math.tan
import kotlin.math.tanh

/**
 * Function of x for graphs and tables of values, like f(x) in the Functions app of NumWorks.
 * Parsed once, then calculated with fast floating point math. Integrals, derivatives and series
 * use the exact evaluator. Undefined values are NaN: ln(−1), 1/0.
 *
 * - `d/dx(f)` is the derivative at x
 * - `∫f dx` without bounds is the antiderivative that is 0 at x = 0
 *
 * @throws ExpressionException When input can't be parsed or uses units.
 */
class GraphFunction(input: String, private val radianMode: Boolean = true) {
  private val evaluator = Evaluator(radianMode, KRoundingMode.HALF_EVEN)

  /** `false` when integrals, derivatives or series need the slow exact evaluator. */
  var isFast = true
    private set

  private val compiled: (Double) -> Double

  init {
    val tree = Parser(input.tokenize(), KRoundingMode.HALF_EVEN).parse()
    if (tree is Node.Convert || tree.units().isNotEmpty()) throw ExpressionException.BadExpression()
    compiled = DoubleCompiler<Double>(radianMode, ::compileOther).compile(tree)
  }

  /** f(x), NaN when it is not defined or not a real number. */
  fun valueAt(x: Double): Double =
    try {
      compiled(x).takeIf { it.isFinite() } ?: Double.NaN
    } catch (e: Exception) {
      Double.NaN
    }

  private fun compileOther(node: Node): (Double) -> Double =
    when (node) {
      Node.X -> { x -> x }
      // Value of the derivative at x
      is Node.Derivative -> exact(if (node.at == null) Node.Derivative(node.body, Node.X) else node)
      // Antiderivative that is 0 at x = 0
      is Node.Integral ->
        exact(if (node.lower == null) Node.Integral(Node.Number(KBigDecimal.ZERO), Node.X, node.body) else node)
      else -> exact(node)
    }

  /** Slow, but exact: for integrals, derivatives and series. */
  private fun exact(node: Node): (Double) -> Double {
    isFast = false
    return { x -> evaluator.evaluate(node, Complex(KBigDecimal.valueOf(x))).real().toDouble() }
  }
}

/**
 * Turns an expression into a fast floating point function of [V] (the values of its variables).
 * Numbers, constants, operators and functions are compiled here, everything else (variables,
 * integrals) by [compileOther].
 */
internal class DoubleCompiler<V>(
  private val radianMode: Boolean,
  private val compileOther: (Node) -> (V) -> Double,
) {
  fun compile(node: Node): (V) -> Double =
    when (node) {
      is Node.Number -> node.value.toDouble().let { value -> { _: V -> value } }
      Node.Pi -> { _ -> PI }
      Node.Euler -> { _ -> E }
      is Node.Negate -> compile(node.node).let { f -> { v -> -f(v) } }
      is Node.Binary -> compileBinary(node)
      is Node.Function -> compileFunction(node.function, compile(node.argument))
      is Node.Log -> compileLog(compile(node.base), compile(node.argument))
      is Node.Factorial -> compile(node.node).let { f -> { v -> factorial(f(v)) } }
      else -> compileOther(node)
    }

  private fun compileLog(base: (V) -> Double, argument: (V) -> Double): (V) -> Double =
    { v -> ln(argument(v)) / ln(base(v)) }

  private fun compileBinary(node: Node.Binary): (V) -> Double {
    val left = compile(node.left)
    val right = compile(node.right)
    return when (node.operator) {
      BinaryOperator.PLUS -> { v -> left(v) + right(v) }
      BinaryOperator.MINUS -> { v -> left(v) - right(v) }
      BinaryOperator.MULTIPLY -> { v -> left(v) * right(v) }
      BinaryOperator.DIVIDE -> { v -> left(v) / right(v) }
      BinaryOperator.POWER -> { v -> power(left(v), right(v)) }
      BinaryOperator.MODULO -> { v -> left(v).mod(right(v)) }
    }
  }

  private fun compileFunction(function: FunctionType, f: (V) -> Double): (V) -> Double {
    // Angles in degrees when radian mode is off
    val toRadians = if (radianMode) 1.0 else PI / DEGREES_IN_HALF_TURN
    val fromRadians = 1 / toRadians
    return when (function) {
      FunctionType.SQRT -> { v -> sqrt(f(v)) }
      FunctionType.SIN -> { v -> sin(f(v) * toRadians) }
      FunctionType.COS -> { v -> cos(f(v) * toRadians) }
      FunctionType.TAN -> { v -> tan(f(v) * toRadians) }
      FunctionType.ARSIN -> { v -> asin(f(v)) * fromRadians }
      FunctionType.ARCOS -> { v -> acos(f(v)) * fromRadians }
      FunctionType.ARTAN -> { v -> atan(f(v)) * fromRadians }
      FunctionType.LN -> { v -> ln(f(v)) }
      FunctionType.LOG -> { v -> log10(f(v)) }
      FunctionType.EXP -> { v -> exp(f(v)) }
      FunctionType.SINH -> { v -> sinh(f(v)) }
      FunctionType.COSH -> { v -> cosh(f(v)) }
      FunctionType.TANH -> { v -> tanh(f(v)) }
      FunctionType.ARSINH -> { v -> asinh(f(v)) }
      FunctionType.ARCOSH -> { v -> acosh(f(v)) }
      FunctionType.ARTANH -> { v -> atanh(f(v)) }
      FunctionType.ABS -> { v -> abs(f(v)) }
    }
  }
}

private fun power(base: Double, exponent: Double): Double {
  if (base >= 0 || exponent == floor(exponent)) return base.pow(exponent)
  // Odd roots: exponent is p/q with odd q
  for (denominator in ODD_DENOMINATORS) {
    val numerator = exponent * denominator
    if (abs(numerator - numerator.roundToInt()) < ROOT_TOLERANCE) {
      val root = if (denominator == 3) cbrt(base) else -(-base).pow(1.0 / denominator)
      return root.pow(numerator.roundToInt())
    }
  }
  return Double.NaN
}

private fun factorial(n: Double): Double {
  if (n < 0 || n != floor(n) || n > MAX_FACTORIAL) return Double.NaN
  var result = 1.0
  for (i in 2..n.toInt()) result *= i
  return result
}

private fun KBigDecimal.toDouble(): Double = toPlainString().toDouble()

private val ODD_DENOMINATORS = listOf(3, 5, 7, 9)
private const val ROOT_TOLERANCE = 1e-9
private const val MAX_FACTORIAL = 170
private const val DEGREES_IN_HALF_TURN = 180.0
