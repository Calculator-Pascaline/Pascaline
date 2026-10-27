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
import com.sadellie.unitto.core.common.KRoundingMode
import com.sadellie.unitto.core.common.MAX_SCALE
import com.sadellie.unitto.core.common.PhysicalUnit
import com.sadellie.unitto.core.common.PhysicalUnits
import com.sadellie.unitto.core.common.Token

/** Parsed expression. Can be evaluated many times, for example for every point of an integral. */
internal sealed interface Node {
  class Number(val value: KBigDecimal) : Node

  data object Pi : Node

  data object Euler : Node

  data object ImaginaryUnit : Node

  /** Variable for integrals, derivatives and polynomials. */
  data object X : Node

  /** Index of sums and products. */
  data object N : Node

  /** Only allowed as a bound of integrals, sums and products. */
  data object Infinity : Node

  class Negate(val node: Node) : Node

  class Binary(val operator: BinaryOperator, val left: Node, val right: Node) : Node

  class Function(val function: FunctionType, val argument: Node) : Node

  class Log(val base: Node, val argument: Node) : Node

  /** @property isNegated Factorial right after unary minus, like `−5!`. Not allowed. */
  class Factorial(val node: Node, val isNegated: Boolean) : Node

  /** Indefinite when bounds are null. Body uses [X]. */
  class Integral(val lower: Node?, val upper: Node?, val body: Node) : Node

  /** Symbolic when [at] is null. Body uses [X]. */
  class Derivative(val body: Node, val at: Node?) : Node

  /** Unit or physical constant, value is its SI factor. */
  class Unit(val unit: PhysicalUnit) : Node

  /** `value → target`, target is made of units. Only at the top of an expression. */
  class Convert(val value: Node, val target: Node) : Node

  /** Sum or product of [body] for every whole [N] from [lower] to [upper] (can be infinite). */
  class Series(val lower: Node, val upper: Node, val body: Node, val isProduct: Boolean) : Node
}

internal enum class BinaryOperator {
  PLUS,
  MINUS,
  MULTIPLY,
  DIVIDE,
  POWER,
  MODULO,
}

internal enum class FunctionType {
  SQRT,
  SIN,
  COS,
  TAN,
  ARSIN,
  ARCOS,
  ARTAN,
  LN,
  LOG,
  EXP,
  SINH,
  COSH,
  TANH,
  ARSINH,
  ARCOSH,
  ARTANH,
}

/**
 * `true` when value of this node depends on [Node.X] that is not bound by a definite integral or a
 * derivative at a point.
 */
internal fun Node.dependsOnX(): Boolean =
  when (this) {
    is Node.X -> true
    is Node.Number,
    Node.Pi,
    Node.Euler,
    Node.ImaginaryUnit,
    Node.N,
    Node.Infinity,
    is Node.Unit -> false
    is Node.Convert -> value.dependsOnX()
    is Node.Negate -> node.dependsOnX()
    is Node.Binary -> left.dependsOnX() || right.dependsOnX()
    is Node.Function -> argument.dependsOnX()
    is Node.Log -> base.dependsOnX() || argument.dependsOnX()
    is Node.Factorial -> node.dependsOnX()
    is Node.Integral ->
      if (lower == null || upper == null) true else lower.dependsOnX() || upper.dependsOnX()
    is Node.Derivative -> at?.dependsOnX() ?: true
    is Node.Series -> lower.dependsOnX() || upper.dependsOnX() || body.dependsOnX()
  }

/**
 * Expression := [ "-" ] Term { ("+" | "-") Term }
 *
 * Term := Factor { ( "*" | "/" ) Factor }
 *
 * Factor := RealNumber | "(" Expression ")" | Function | Integral | Derivative
 *
 * RealNumber := Digit{Digit} | [ Digit ] "." {Digit}
 *
 * Integral := "∫" [ Subscript Superscript ] Expression "dx"
 *
 * Derivative := "d/dx" Factor [ "|" Subscript ]
 *
 * Series := ( "Σ" | "∏" ) Subscript Superscript Factor
 */
internal class Parser(private val tokens: List<String>, private val roundingMode: KRoundingMode) {
  private val scale = MAX_SCALE
  private var cursorPosition = 0

  fun parse(): Node {
    if (tokens.isEmpty()) return Node.Number(KBigDecimal.ZERO.setScale(scale))
    val expression = parseExpression()
    if (!moveIfMatched(Token.Operator.CONVERT)) return expression
    return Node.Convert(expression, parseExpression())
  }

  // Null when at the end of expression
  private fun peek() = tokens.getOrNull(cursorPosition) ?: ""

  private fun moveIfMatched(token: String): Boolean {
    if (peek() == token) {
      // Move cursor
      cursorPosition++
      return true
    }
    return false
  }

  private fun expect(token: String) {
    if (!moveIfMatched(token)) throw ExpressionException.BadExpression()
  }

  // Expression := [ "-" ] Term { ("+" | "-") Term }
  private fun parseExpression(): Node {
    var expression = parseTerm()

    while (peek() in listOf(Token.Operator.PLUS, Token.Operator.MINUS)) {
      when {
        moveIfMatched(Token.Operator.PLUS) ->
          expression = Node.Binary(BinaryOperator.PLUS, expression, parseTerm())
        moveIfMatched(Token.Operator.MINUS) ->
          expression = Node.Binary(BinaryOperator.MINUS, expression, parseTerm())
      }
    }
    return expression
  }

  // Term       := Factor { ( "*" | "/" ) Factor }
  private fun parseTerm(): Node {
    var expression = parseFactor()

    while (peek() in listOf(Token.Operator.MULTIPLY, Token.Operator.DIVIDE)) {
      when {
        moveIfMatched(Token.Operator.MULTIPLY) ->
          expression = Node.Binary(BinaryOperator.MULTIPLY, expression, parseFactor())
        moveIfMatched(Token.Operator.DIVIDE) ->
          expression = Node.Binary(BinaryOperator.DIVIDE, expression, parseFactor())
      }
    }
    return expression
  }

  private fun parseFuncParentheses(): Node {
    return if (moveIfMatched(Token.Operator.LEFT_BRACKET)) {
      // Parse in parentheses
      val res = parseExpression()

      // Check if parentheses is closed
      expect(Token.Operator.RIGHT_BRACKET)
      res
    } else {
      parseFactor()
    }
  }

  // Factor     := RealNumber | "(" Expression ")"
  private fun parseFactor(negative: Boolean = false): Node {
    // This will throw Exception if some function lacks argument, for example: "cos()" or "600^"

    // Unary plus
    if (moveIfMatched(Token.Operator.PLUS)) return parseFactor()

    // Unary minus
    if (moveIfMatched(Token.Operator.MINUS)) return Node.Negate(parseFactor(true))

    val possibleNumber = peek()
    var expr: Node? =
      when {
        moveIfMatched(Token.Operator.LEFT_BRACKET) -> {
          val inside = parseExpression()
          expect(Token.Operator.RIGHT_BRACKET)
          inside
        }
        // We know that if next token starts with a digit or dot, it can be converted into
        // BigDecimal
        possibleNumber.isNotEmpty() &&
          Token.Digit.allWithDot.contains(possibleNumber.first().toString()) -> {
          cursorPosition++
          Node.Number(KBigDecimal(possibleNumber).setScale(scale, roundingMode))
        }
        moveIfMatched(Token.Const.PI) -> Node.Pi
        moveIfMatched(Token.Const.E) -> Node.Euler
        moveIfMatched(Token.Const.I) -> Node.ImaginaryUnit
        moveIfMatched(Token.Calculus.X) -> Node.X
        moveIfMatched(Token.Calculus.N) -> Node.N
        possibleNumber.startsWith(Token.Units.PREFIX) -> {
          cursorPosition++
          Node.Unit(PhysicalUnits.bySymbol(possibleNumber.removePrefix(Token.Units.PREFIX)) ?: throw ExpressionException.BadExpression())
        }
        moveIfMatched(Token.Const.INFINITY) -> Node.Infinity
        moveIfMatched(Token.Operator.SQRT) -> function(FunctionType.SQRT)
        moveIfMatched(Token.Func.ARSINH) -> function(FunctionType.ARSINH)
        moveIfMatched(Token.Func.ARCOSH) -> function(FunctionType.ARCOSH)
        moveIfMatched(Token.Func.ARTANH) -> function(FunctionType.ARTANH)
        moveIfMatched(Token.Func.SINH) -> function(FunctionType.SINH)
        moveIfMatched(Token.Func.COSH) -> function(FunctionType.COSH)
        moveIfMatched(Token.Func.TANH) -> function(FunctionType.TANH)
        moveIfMatched(Token.Func.SIN) -> function(FunctionType.SIN)
        moveIfMatched(Token.Func.COS) -> function(FunctionType.COS)
        moveIfMatched(Token.Func.TAN) -> function(FunctionType.TAN)
        moveIfMatched(Token.Func.ARSIN) -> function(FunctionType.ARSIN)
        moveIfMatched(Token.Func.ARCOS) -> function(FunctionType.ARCOS)
        moveIfMatched(Token.Func.ACTAN) -> function(FunctionType.ARTAN)
        moveIfMatched(Token.Func.LN) -> function(FunctionType.LN)
        moveIfMatched(Token.Func.LOG) -> parseLog()
        moveIfMatched(Token.Func.EXP) -> function(FunctionType.EXP)
        moveIfMatched(Token.Calculus.INTEGRAL) -> parseIntegral()
        moveIfMatched(Token.Calculus.DERIVATIVE) -> parseDerivative()
        moveIfMatched(Token.Calculus.SUM) -> parseSeries(isProduct = false)
        moveIfMatched(Token.Calculus.PRODUCT) -> parseSeries(isProduct = true)
        else -> null
      }

    // Power
    if (moveIfMatched(Token.Operator.POWER)) {
      val factor = parseFactor()
      if (expr == null) throw ExpressionException.BadExpression()
      expr = Node.Binary(BinaryOperator.POWER, expr, factor)
    }

    // Modulo
    if (moveIfMatched(Token.Operator.MODULO)) {
      if (expr == null) throw ExpressionException.BadExpression()
      expr = Node.Binary(BinaryOperator.MODULO, expr, parseFactor())
    }

    // Factorial
    if (moveIfMatched(Token.Operator.FACTORIAL)) {
      if (expr == null) throw ExpressionException.BadExpression()
      expr = Node.Factorial(expr, negative)
    }

    return expr ?: throw ExpressionException.BadExpression()
  }

  private fun function(type: FunctionType) = Node.Function(type, parseFuncParentheses())

  private fun parseLog(): Node {
    if (!moveIfMatched(Token.Layout.SUBSCRIPT_START)) return function(FunctionType.LOG)
    // log with base in subscript: log⟨base⟩(argument)
    val base = parseExpression()
    expect(Token.Layout.END)
    return Node.Log(base, parseFuncParentheses())
  }

  /** ∫⟨a⟩⟨b⟩f(x)dx is definite, bounds can be left empty for indefinite integral: ∫⟨⟩⟨⟩f(x)dx */
  private fun parseIntegral(): Node {
    var lower: Node? = null
    var upper: Node? = null
    if (moveIfMatched(Token.Layout.SUBSCRIPT_START)) {
      lower = if (moveIfMatched(Token.Layout.END)) null else parseExpression().also { expect(Token.Layout.END) }
      expect(Token.Layout.SUPERSCRIPT_START)
      upper = if (moveIfMatched(Token.Layout.END)) null else parseExpression().also { expect(Token.Layout.END) }
      // Only one bound
      if ((lower == null) != (upper == null)) throw ExpressionException.BadExpression()
    }
    val body = parseExpression()
    expect(Token.Calculus.DX)
    return Node.Integral(lower, upper, body)
  }

  private fun parseSeries(isProduct: Boolean): Node {
    expect(Token.Layout.SUBSCRIPT_START)
    val lower = parseExpression()
    expect(Token.Layout.END)
    expect(Token.Layout.SUPERSCRIPT_START)
    val upper = parseExpression()
    expect(Token.Layout.END)
    return Node.Series(lower, upper, parseFuncParentheses(), isProduct)
  }

  private fun parseDerivative(): Node {
    val body = parseFuncParentheses()
    if (!moveIfMatched(Token.Calculus.EVALUATED_AT)) return Node.Derivative(body, null)
    expect(Token.Layout.SUBSCRIPT_START)
    val at = parseExpression()
    expect(Token.Layout.END)
    return Node.Derivative(body, at)
  }
}
