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
import com.sadellie.unitto.core.common.isEqualTo

/**
 * Antiderivative (without integration constant). Supports sums, constant factors and common
 * functions of a linear argument `ax+b`: powers, 1/x, eˣ, sin, cos, tan, ln, √.
 *
 * @throws ExpressionException.BadExpression For anything else, like `x·sin(x)`.
 */
internal fun Node.antiderivative(radianMode: Boolean, evaluator: Evaluator): Node {
  if (!dependsOnX()) return times(this, Node.X)
  fun Node.integrate() = antiderivative(radianMode, evaluator)

  return when (this) {
    Node.X -> divide(power(Node.X, number(2)), number(2))
    is Node.Negate -> Node.Negate(node.integrate())
    is Node.Binary ->
      when (operator) {
        BinaryOperator.PLUS -> Node.Binary(BinaryOperator.PLUS, left.integrate(), right.integrate())
        BinaryOperator.MINUS -> Node.Binary(BinaryOperator.MINUS, left.integrate(), right.integrate())
        BinaryOperator.MULTIPLY ->
          when {
            !left.dependsOnX() -> times(left, right.integrate())
            !right.dependsOnX() -> times(left.integrate(), right)
            else -> unsupported()
          }
        BinaryOperator.DIVIDE ->
          when {
            !right.dependsOnX() -> divide(left.integrate(), right)
            // c/(ax+b) = c·ln(ax+b)/a
            else -> {
              val slope = right.linearSlope(evaluator) ?: unsupported()
              if (left.dependsOnX()) unsupported()
              times(left, overSlope(ln(right), slope))
            }
          }
        BinaryOperator.POWER -> integratePower(left, right, evaluator)
        BinaryOperator.MODULO -> unsupported()
      }
    is Node.Function -> integrateFunction(this, radianMode, evaluator)
    // ∫ f'(x) dx = f(x)
    is Node.Derivative -> if (at == null) body else unsupported()
    else -> unsupported()
  }
}

/** Has `∫...dx` without bounds, so result needs integration constant. */
internal fun Node.hasIndefiniteIntegral(): Boolean =
  when (this) {
    is Node.Integral -> lower == null || body.hasIndefiniteIntegral()
    // derivative of an antiderivative has no constant
    is Node.Derivative -> false
    is Node.Negate -> node.hasIndefiniteIntegral()
    is Node.Binary -> left.hasIndefiniteIntegral() || right.hasIndefiniteIntegral()
    is Node.Function -> argument.hasIndefiniteIntegral()
    is Node.Log -> base.hasIndefiniteIntegral() || argument.hasIndefiniteIntegral()
    is Node.Factorial -> node.hasIndefiniteIntegral()
    else -> false
  }

private fun integratePower(base: Node, exponent: Node, evaluator: Evaluator): Node {
  // (ax+b)^n
  if (!exponent.dependsOnX()) {
    val slope = base.linearSlope(evaluator) ?: unsupported()
    val n = evaluator.evaluate(exponent).real()
    if (n.isEqualTo(-KBigDecimal.ONE)) return overSlope(ln(base), slope)
    val next = Node.Binary(BinaryOperator.PLUS, exponent, number(1))
    return overSlope(divide(power(base, next), next), slope)
  }
  val slope = exponent.linearSlope(evaluator) ?: unsupported()
  // e^(ax+b)
  if (base == Node.Euler) return overSlope(power(base, exponent), slope)
  // c^(ax+b) = c^(ax+b) / (a·ln c)
  if (!base.dependsOnX()) return overSlope(divide(power(base, exponent), ln(base)), slope)
  unsupported()
}

private fun integrateFunction(node: Node.Function, radianMode: Boolean, evaluator: Evaluator): Node {
  val u = node.argument
  val slope = u.linearSlope(evaluator) ?: unsupported()
  val antiderivative =
    when (node.function) {
      FunctionType.SIN -> Node.Negate(function(FunctionType.COS, u)).inDegrees(radianMode)
      FunctionType.COS -> function(FunctionType.SIN, u).inDegrees(radianMode)
      FunctionType.TAN -> Node.Negate(ln(function(FunctionType.COS, u))).inDegrees(radianMode)
      FunctionType.EXP -> node
      FunctionType.SINH -> function(FunctionType.COSH, u)
      FunctionType.COSH -> function(FunctionType.SINH, u)
      FunctionType.LN -> Node.Binary(BinaryOperator.MINUS, times(u, ln(u)), u)
      // 2u√u/3
      FunctionType.SQRT -> divide(times(number(2), times(u, node)), number(3))
      else -> unsupported()
    }
  return overSlope(antiderivative, slope)
}

/** Slope `a` of a linear `ax+b`, null when [this] is not linear in x. */
private fun Node.linearSlope(evaluator: Evaluator): Rational? {
  val polynomial =
    try {
      toPolynomial(evaluator)
    } catch (e: ExpressionException) {
      return null
    }
  if (polynomial.degree != 1) return null
  return polynomial[1]
}

/** Divides by slope of the inner function (reverse chain rule). */
private fun overSlope(node: Node, slope: Rational): Node =
  if (slope == Rational.ONE) node else times(rationalNode(Rational.ONE / slope), node)

/** ∫cos(x°)dx = sin(x°)·180/π */
private fun Node.inDegrees(radianMode: Boolean): Node =
  if (radianMode) this else times(divide(number(DEGREES_IN_HALF_TURN), Node.Pi), this)

internal fun rationalNode(value: Rational): Node {
  val numerator = Node.Number(value.numerator)
  if (value.isInteger) return numerator
  if (value.sign < 0) {
    return Node.Negate(divide(Node.Number(-value.numerator), Node.Number(value.denominator)))
  }
  return divide(numerator, Node.Number(value.denominator))
}

private fun unsupported(): Nothing = throw ExpressionException.BadExpression()

private fun number(value: Long) = Node.Number(KBigDecimal.valueOf(value))

private fun function(type: FunctionType, argument: Node) = Node.Function(type, argument)

private fun ln(node: Node) = Node.Function(FunctionType.LN, node)

private fun times(left: Node, right: Node) = Node.Binary(BinaryOperator.MULTIPLY, left, right)

private fun divide(left: Node, right: Node) = Node.Binary(BinaryOperator.DIVIDE, left, right)

private fun power(left: Node, right: Node) = Node.Binary(BinaryOperator.POWER, left, right)

private const val DEGREES_IN_HALF_TURN = 180L
