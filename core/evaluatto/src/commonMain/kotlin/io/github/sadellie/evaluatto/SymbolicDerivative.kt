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
import com.sadellie.unitto.core.common.Token
import com.sadellie.unitto.core.common.isEqualTo
import com.sadellie.unitto.core.common.toFormattedString

/** `true` when there is `d/dx` without a point or `∫` without bounds. */
internal fun Node.hasSymbolicCalculus(): Boolean =
  when (this) {
    is Node.Derivative -> at == null || body.hasSymbolicCalculus()
    is Node.Negate -> node.hasSymbolicCalculus()
    is Node.Binary -> left.hasSymbolicCalculus() || right.hasSymbolicCalculus()
    is Node.Function -> argument.hasSymbolicCalculus()
    is Node.Log -> base.hasSymbolicCalculus() || argument.hasSymbolicCalculus()
    is Node.Factorial -> node.hasSymbolicCalculus()
    is Node.Integral -> lower == null || body.hasSymbolicCalculus()
    else -> false
  }

/** Replaces every symbolic `d/dx(...)` and `∫...dx` with result. */
internal fun Node.expandCalculus(radianMode: Boolean, evaluator: Evaluator): Node {
  fun Node.expand() = expandCalculus(radianMode, evaluator)
  return when (this) {
    is Node.Derivative -> if (at == null) body.expand().derivative(radianMode).simplify() else this
    is Node.Integral ->
      if (lower == null) body.expand().antiderivative(radianMode, evaluator).simplify()
      else Node.Integral(lower, upper, body.expand())
    is Node.Negate -> Node.Negate(node.expand())
    is Node.Binary -> Node.Binary(operator, left.expand(), right.expand())
    is Node.Function -> Node.Function(function, argument.expand())
    is Node.Log -> Node.Log(base.expand(), argument.expand())
    is Node.Factorial -> Node.Factorial(node.expand(), isNegated)
    else -> this
  }
}

/** Derivative with respect to x. Not simplified. */
internal fun Node.derivative(radianMode: Boolean): Node {
  if (!dependsOnX()) return number(0)
  fun Node.d() = derivative(radianMode)

  return when (this) {
    Node.X -> number(1)
    is Node.Negate -> Node.Negate(node.d())
    is Node.Binary ->
      when (operator) {
        BinaryOperator.PLUS -> left.d() + right.d()
        BinaryOperator.MINUS -> left.d() - right.d()
        BinaryOperator.MULTIPLY -> left.d() * right + left * right.d()
        BinaryOperator.DIVIDE -> (left.d() * right - left * right.d()) / right.pow(number(2))
        BinaryOperator.POWER ->
          when {
            // x^n
            !right.dependsOnX() -> right * left.pow(right - number(1)) * left.d()
            // e^f
            left == Node.Euler -> this * right.d()
            // a^f
            !left.dependsOnX() -> this * ln(left) * right.d()
            // f^g = e^(g·ln f)
            else -> this * (right.d() * ln(left) + right * left.d() / left)
          }
        BinaryOperator.MODULO -> throw ExpressionException.BadExpression()
      }
    is Node.Function -> {
      val inner = argument.d()
      val outer: Node =
        when (function) {
          FunctionType.SQRT -> number(1) / (number(2) * this)
          FunctionType.SIN -> Node.Function(FunctionType.COS, argument).toRadians(radianMode)
          FunctionType.COS -> Node.Negate(Node.Function(FunctionType.SIN, argument)).toRadians(radianMode)
          FunctionType.TAN ->
            (number(1) / Node.Function(FunctionType.COS, argument).pow(number(2))).toRadians(radianMode)
          FunctionType.ARSIN -> (number(1) / sqrt(number(1) - argument.pow(number(2)))).toDegrees(radianMode)
          FunctionType.ARCOS ->
            Node.Negate(number(1) / sqrt(number(1) - argument.pow(number(2)))).toDegrees(radianMode)
          FunctionType.ARTAN -> (number(1) / (number(1) + argument.pow(number(2)))).toDegrees(radianMode)
          FunctionType.LN -> number(1) / argument
          FunctionType.LOG -> number(1) / (argument * ln(number(10)))
          FunctionType.EXP -> this
          FunctionType.SINH -> Node.Function(FunctionType.COSH, argument)
          FunctionType.COSH -> Node.Function(FunctionType.SINH, argument)
          FunctionType.TANH -> number(1) / Node.Function(FunctionType.COSH, argument).pow(number(2))
          FunctionType.ARSINH -> number(1) / sqrt(argument.pow(number(2)) + number(1))
          FunctionType.ARCOSH -> number(1) / sqrt(argument.pow(number(2)) - number(1))
          FunctionType.ARTANH -> number(1) / (number(1) - argument.pow(number(2)))
        }
      outer * inner
    }
    is Node.Log -> {
      if (base.dependsOnX()) throw ExpressionException.BadExpression()
      argument.d() / (argument * ln(base))
    }
    // Antiderivative
    is Node.Integral -> if (lower == null) body else throw ExpressionException.BadExpression()
    is Node.Derivative -> if (at == null) body.d().d() else throw ExpressionException.BadExpression()
    else -> throw ExpressionException.BadExpression()
  }
}

/** Removes zeros, ones, double negations and calculates numbers. */
internal fun Node.simplify(): Node =
  when (this) {
    is Node.Negate -> {
      when (val inner = node.simplify()) {
        is Node.Negate -> inner.node
        is Node.Number -> Node.Number(-inner.value)
        else -> Node.Negate(inner)
      }
    }
    is Node.Binary -> simplifyBinary(operator, left.simplify(), right.simplify()).mergeLikeTerms()
    is Node.Function -> Node.Function(function, argument.simplify())
    is Node.Log -> Node.Log(base.simplify(), argument.simplify())
    else -> this
  }

private fun simplifyBinary(operator: BinaryOperator, left: Node, right: Node): Node {
  val l = (left as? Node.Number)?.value
  val r = (right as? Node.Number)?.value
  return when (operator) {
    BinaryOperator.PLUS ->
      when {
        l != null && r != null -> Node.Number(l + r)
        l.isZero() -> right
        r.isZero() -> left
        right is Node.Negate -> simplifyBinary(BinaryOperator.MINUS, left, right.node)
        r != null && r < KBigDecimal.ZERO -> Node.Binary(BinaryOperator.MINUS, left, Node.Number(-r))
        else -> Node.Binary(operator, left, right)
      }
    BinaryOperator.MINUS ->
      when {
        l != null && r != null -> Node.Number(l - r)
        r.isZero() -> left
        l.isZero() -> Node.Negate(right).simplify()
        right is Node.Negate -> simplifyBinary(BinaryOperator.PLUS, left, right.node)
        else -> Node.Binary(operator, left, right)
      }
    BinaryOperator.MULTIPLY -> simplifyProduct(left, right)
    BinaryOperator.DIVIDE ->
      when {
        l.isZero() -> number(0)
        r.isOne() -> left
        l != null && r != null && !r.isZero() && l.remainder(r).isZero() ->
          Node.Number(l.divide(r, 0, KRoundingMode.HALF_EVEN))
        left is Node.Negate -> Node.Negate(simplifyBinary(operator, left.node, right))
        else -> Node.Binary(operator, left, right)
      }
    BinaryOperator.POWER ->
      when {
        r.isZero() -> number(1)
        r.isOne() -> left
        else -> Node.Binary(operator, left, right)
      }
    BinaryOperator.MODULO -> Node.Binary(operator, left, right)
  }
}

/**
 * Adds up terms of a sum that differ only by a number: `sin(x)+sin(x)` ==> `2sin(x)`,
 * `3x×cos(x)−x×cos(x)+1+2` ==> `2x×cos(x)+3`. Sums without such terms are returned as they are.
 */
private fun Node.mergeLikeTerms(): Node {
  if (this !is Node.Binary || (operator != BinaryOperator.PLUS && operator != BinaryOperator.MINUS)) {
    return this
  }
  val terms = mutableListOf<Pair<KBigDecimal, Node?>>()
  fun collect(node: Node, negative: Boolean) {
    when {
      node is Node.Binary && node.operator == BinaryOperator.PLUS -> {
        collect(node.left, negative)
        collect(node.right, negative)
      }
      node is Node.Binary && node.operator == BinaryOperator.MINUS -> {
        collect(node.left, negative)
        collect(node.right, !negative)
      }
      node is Node.Negate -> collect(node.node, !negative)
      else -> {
        val (coefficient, rest) = node.splitCoefficient()
        terms += (if (negative) -coefficient else coefficient) to rest
      }
    }
  }
  collect(this, false)

  // Group by everything but the number, keep order of first appearance
  val groups = LinkedHashMap<String, Pair<KBigDecimal, Node?>>()
  for ((coefficient, rest) in terms) {
    val key = rest?.key() ?: ""
    val previous = groups[key]
    groups[key] = ((previous?.first ?: KBigDecimal.ZERO) + coefficient) to rest
  }
  if (groups.size == terms.size) return this

  val merged =
    groups.values
      .filterNot { it.first.isZero() }
      .map { (coefficient, rest) -> if (rest == null) Node.Number(coefficient) else simplifyProduct(Node.Number(coefficient), rest) }
  if (merged.isEmpty()) return number(0)
  return merged.drop(1).fold(merged.first()) { sum, term ->
    when {
      term is Node.Negate -> Node.Binary(BinaryOperator.MINUS, sum, term.node)
      term is Node.Number && term.value < KBigDecimal.ZERO ->
        Node.Binary(BinaryOperator.MINUS, sum, Node.Number(-term.value))
      else -> Node.Binary(BinaryOperator.PLUS, sum, term)
    }
  }
}

/** `3x` ==> 3 and `x`, `5` ==> 5 and null, `−sin(x)` ==> −1 and `sin(x)`. */
private fun Node.splitCoefficient(): Pair<KBigDecimal, Node?> =
  when {
    this is Node.Number -> value to null
    this is Node.Negate -> node.splitCoefficient().let { (coefficient, rest) -> -coefficient to rest }
    this is Node.Binary && operator == BinaryOperator.MULTIPLY && left is Node.Number ->
      (left as Node.Number).value to right
    else -> KBigDecimal.ONE to this
  }

/** Same text for nodes with the same structure, used to compare them. */
private fun Node.key(): String =
  when (this) {
    is Node.Number -> value.stripTrailingZeros().toPlainString()
    is Node.Negate -> "-(${node.key()})"
    is Node.Binary -> "$operator(${left.key()},${right.key()})"
    is Node.Function -> "$function(${argument.key()})"
    is Node.Log -> "log(${base.key()},${argument.key()})"
    is Node.Factorial -> "!(${node.key()})"
    is Node.Integral -> "int(${lower?.key()},${upper?.key()},${body.key()})"
    is Node.Derivative -> "d(${body.key()},${at?.key()})"
    is Node.Unit -> "unit(${unit.symbol})"
    is Node.Convert -> "to(${value.key()},${target.key()})"
    is Node.Series -> "series($isProduct,${lower.key()},${upper.key()},${body.key()})"
    else -> toString()
  }

/** Numbers go first and are multiplied together: `2·x·3` ==> `6x`. */
private fun simplifyProduct(left: Node, right: Node): Node {
  val factors = mutableListOf<Node>()
  fun collect(node: Node) {
    if (node is Node.Binary && node.operator == BinaryOperator.MULTIPLY) {
      collect(node.left)
      collect(node.right)
    } else {
      factors += node
    }
  }
  collect(left)
  collect(right)

  var coefficient = KBigDecimal.ONE
  val rest = mutableListOf<Node>()
  for (factor in factors) {
    when (factor) {
      is Node.Number -> coefficient = coefficient.multiply(factor.value)
      is Node.Negate -> {
        coefficient = -coefficient
        rest += factor.node
      }
      else -> rest += factor
    }
  }
  if (coefficient.isZero()) return number(0)
  val product = rest.reduceOrNull { acc, node -> Node.Binary(BinaryOperator.MULTIPLY, acc, node) }
  return when {
    product == null -> Node.Number(coefficient)
    coefficient.isOne() -> product
    (-coefficient).isOne() -> Node.Negate(product)
    coefficient < KBigDecimal.ZERO ->
      Node.Negate(Node.Binary(BinaryOperator.MULTIPLY, Node.Number(-coefficient), product))
    else -> Node.Binary(BinaryOperator.MULTIPLY, Node.Number(coefficient), product)
  }
}

/**
 * Writes expression with slots (powers in superscript, fractions with a bar), so it can be
 * displayed and calculated again.
 */
internal class NodeFormatter(private val precision: Int, private val outputFormat: Int) {
  fun format(node: Node): String =
    when (node) {
      is Node.Number -> number(node.value)
      Node.Pi -> Token.Const.PI
      Node.Euler -> Token.Const.E
      Node.ImaginaryUnit -> Token.Const.I
      Node.X -> Token.Calculus.X
      Node.N -> Token.Calculus.N
      Node.Infinity -> Token.Const.INFINITY
      is Node.Unit -> node.unit.token
      is Node.Convert -> format(node.value) + Token.Operator.CONVERT + format(node.target)
      is Node.Series -> throw ExpressionException.BadExpression()
      is Node.Negate -> Token.Operator.MINUS + wrap(node.node, PRECEDENCE_PRODUCT)
      is Node.Binary -> formatBinary(node)
      is Node.Function ->
        when (node.function) {
          FunctionType.SQRT ->
            Token.Operator.SQRT + Token.Layout.RADICAND_START + format(node.argument) + Token.Layout.END
          else -> functionName(node.function) + "(" + format(node.argument) + ")"
        }
      is Node.Log ->
        Token.Func.LOG + Token.Layout.SUBSCRIPT_START + format(node.base) + Token.Layout.END +
          "(" + format(node.argument) + ")"
      is Node.Factorial -> wrap(node.node, PRECEDENCE_ATOM) + Token.Operator.FACTORIAL
      is Node.Integral -> throw ExpressionException.BadExpression()
      is Node.Derivative -> throw ExpressionException.BadExpression()
    }

  private fun formatBinary(node: Node.Binary): String {
    val left = node.left
    val right = node.right
    return when (node.operator) {
      BinaryOperator.PLUS -> format(left) + Token.Operator.PLUS + format(right)
      BinaryOperator.MINUS ->
        format(left) + Token.Operator.MINUS + wrap(right, PRECEDENCE_PRODUCT)
      BinaryOperator.MULTIPLY -> {
        val leftText = wrap(left, PRECEDENCE_PRODUCT)
        val rightText = wrap(right, PRECEDENCE_PRODUCT)
        // 6x, 2cos(x), ⅓x³, but 2×3 and x×sin(x)
        val isCoefficient = left is Node.Number || left.isNumberFraction()
        val juxtapose = isCoefficient && rightText.firstOrNull()?.isDigit() != true
        leftText + (if (juxtapose) "" else Token.Operator.MULTIPLY) + rightText
      }
      BinaryOperator.DIVIDE ->
        Token.Layout.NUMERATOR_START + format(left) + Token.Layout.END +
          Token.Layout.DENOMINATOR_START + format(right) + Token.Layout.END
      BinaryOperator.POWER ->
        wrap(left, PRECEDENCE_ATOM) + Token.Operator.POWER + Token.Layout.SUPERSCRIPT_START +
          format(right) + Token.Layout.END
      BinaryOperator.MODULO ->
        wrap(left, PRECEDENCE_ATOM) + Token.Operator.MODULO + wrap(right, PRECEDENCE_ATOM)
    }
  }

  /** Adds brackets when [node] binds weaker than [precedence]. */
  private fun wrap(node: Node, precedence: Int): String {
    val text = format(node)
    return if (node.precedence() < precedence) "($text)" else text
  }

  private fun Node.precedence(): Int =
    when (this) {
      is Node.Binary ->
        when (operator) {
          BinaryOperator.PLUS,
          BinaryOperator.MINUS -> PRECEDENCE_SUM
          BinaryOperator.MULTIPLY,
          BinaryOperator.MODULO -> PRECEDENCE_PRODUCT
          // fractions and powers are drawn as blocks
          BinaryOperator.DIVIDE,
          BinaryOperator.POWER -> PRECEDENCE_ATOM
        }
      is Node.Negate -> PRECEDENCE_SUM
      is Node.Number -> if (value < KBigDecimal.ZERO) PRECEDENCE_SUM else PRECEDENCE_ATOM
      else -> PRECEDENCE_ATOM
    }

  private fun number(value: KBigDecimal): String =
    value.toFormattedString(precision, outputFormat).replace("-", Token.Operator.MINUS)

  private fun functionName(function: FunctionType) =
    when (function) {
      FunctionType.SQRT -> Token.Operator.SQRT
      FunctionType.SIN -> Token.Func.SIN
      FunctionType.COS -> Token.Func.COS
      FunctionType.TAN -> Token.Func.TAN
      FunctionType.ARSIN -> Token.Func.ARSIN
      FunctionType.ARCOS -> Token.Func.ARCOS
      FunctionType.ARTAN -> Token.Func.ACTAN
      FunctionType.LN -> Token.Func.LN
      FunctionType.LOG -> Token.Func.LOG
      FunctionType.EXP -> Token.Func.EXP
      FunctionType.SINH -> Token.Func.SINH
      FunctionType.COSH -> Token.Func.COSH
      FunctionType.TANH -> Token.Func.TANH
      FunctionType.ARSINH -> Token.Func.ARSINH
      FunctionType.ARCOSH -> Token.Func.ARCOSH
      FunctionType.ARTANH -> Token.Func.ARTANH
    }
}

private fun number(value: Long) = Node.Number(KBigDecimal.valueOf(value))

private fun Node.isNumberFraction() =
  this is Node.Binary &&
    operator == BinaryOperator.DIVIDE &&
    left is Node.Number &&
    right is Node.Number

private fun ln(node: Node) = Node.Function(FunctionType.LN, node)

private fun sqrt(node: Node) = Node.Function(FunctionType.SQRT, node)

private operator fun Node.plus(other: Node) = Node.Binary(BinaryOperator.PLUS, this, other)

private operator fun Node.minus(other: Node) = Node.Binary(BinaryOperator.MINUS, this, other)

private operator fun Node.times(other: Node) = Node.Binary(BinaryOperator.MULTIPLY, this, other)

private operator fun Node.div(other: Node) = Node.Binary(BinaryOperator.DIVIDE, this, other)

private fun Node.pow(other: Node) = Node.Binary(BinaryOperator.POWER, this, other)

/** Trigonometry in degrees: d/dx sin(x°) = cos(x°)·π/180 */
private fun Node.toRadians(radianMode: Boolean): Node =
  if (radianMode) this else this * (Node.Pi / number(DEGREES_IN_HALF_TURN))

/** Inverse trigonometry in degrees: result is in degrees, times 180/π */
private fun Node.toDegrees(radianMode: Boolean): Node =
  if (radianMode) this else this * (number(DEGREES_IN_HALF_TURN) / Node.Pi)

private fun KBigDecimal?.isZero() = this != null && isEqualTo(KBigDecimal.ZERO)

private fun KBigDecimal?.isOne() = this != null && isEqualTo(KBigDecimal.ONE)

private const val PRECEDENCE_SUM = 1
private const val PRECEDENCE_PRODUCT = 2
private const val PRECEDENCE_ATOM = 3
private const val DEGREES_IN_HALF_TURN = 180L
