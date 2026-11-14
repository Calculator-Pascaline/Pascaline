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

/**
 * A row of the DI table of integration by parts. Expressions use layout slots.
 *
 * @property sign +1 or −1, they alternate from + on the first row.
 * @property differentiated D column: the factor that is differentiated row after row.
 * @property integrated I column: the factor that is integrated row after row.
 */
data class DiRow(val sign: Int, val differentiated: String, val integrated: String)

/** Why a DI table ends: the three stops of the method. */
enum class DiStop {
  /** The D column reaches 0: the answer is the sum of the diagonal products. */
  ZERO,

  /** The product of the last row can be integrated directly, like 1/x·x²/2 = x/2. */
  INTEGRABLE,

  /** The product of the last row is the integral back: it goes to the other side of the equation. */
  REPEATS,
}

/**
 * Integration by parts as a DI table, from the first row to the one where it stops.
 *
 * @property setup What the table gives: the diagonal products with their signs, then the integral
 *   of the last row across when it doesn't stop at 0. Layout slots, like `x²eˣ−2xeˣ+2eˣ`.
 */
data class DiTable(val rows: List<DiRow>, val stop: DiStop, val setup: String)

/**
 * DI table of ∫this dx, null when it isn't integrated by parts. The factor that is differentiated
 * is the first one in LIATE order: logarithm, inverse trigonometric, algebraic, trigonometric,
 * exponential. A logarithm or an inverse function alone is that function times 1.
 */
internal fun Node.diTable(radianMode: Boolean, evaluator: Evaluator, formatter: NodeFormatter): DiTable? {
  val (differentiated, integrated) = liateSplit(evaluator) ?: return null
  val kind = differentiated.liateKind(evaluator) ?: return null
  val d = mutableListOf(differentiated)
  val i = mutableListOf(integrated)
  repeat(MAX_ROWS) {
    d += d.last().derivative(radianMode).simplify()
    i += i.last().antiderivative(radianMode, evaluator).simplify()
    val stop =
      when {
        d.last().isZeroPolynomial(evaluator) -> DiStop.ZERO
        // ln and inverse functions differentiate into fractions that multiply into polynomials
        kind <= LiateKind.INVERSE -> DiStop.INTEGRABLE
        // sin, cos and e^x come back after two rows
        d.size == 3 && kind >= LiateKind.TRIGONOMETRIC -> DiStop.REPEATS
        else -> null
      }
    if (stop != null) {
      val rows = d.indices.map { DiRow(if (it % 2 == 0) 1 else -1, formatter.format(d[it]), formatter.format(i[it])) }
      return DiTable(rows, stop, setup(d, i, stop, formatter))
    }
  }
  return null
}

/**
 * Diagonals D₀·I₁ − D₁·I₂ + …, then ± ∫Dₙ·Iₙ dx across the last row unless it stopped at 0. Each
 * product is simplified alone, so the steps stay visible: x²eˣ−2xeˣ+2eˣ, not (x²−2x+2)eˣ.
 */
private fun setup(d: List<Node>, i: List<Node>, stop: DiStop, formatter: NodeFormatter): String {
  val last = d.lastIndex
  val terms = (0 until last).map { signedProduct(if (it % 2 == 0) 1 else -1, d[it], i[it + 1], formatter) }
  val text =
    terms.mapIndexed { index, (sign, product) ->
      when {
        sign < 0 -> Token.Operator.MINUS + product
        index == 0 -> product
        else -> Token.Operator.PLUS + product
      }
    }.joinToString("")
  if (stop == DiStop.ZERO) return text
  val (sign, across) = signedProduct(if (last % 2 == 0) 1 else -1, d[last], i[last], formatter)
  val operator = if (sign < 0) Token.Operator.MINUS else Token.Operator.PLUS
  return text + operator + Token.Calculus.INTEGRAL + across + Token.Calculus.DX
}

/** [sign] and the product of [d] and [i], with a minus of the product moved into the sign. */
private fun signedProduct(sign: Int, d: Node, i: Node, formatter: NodeFormatter): Pair<Int, String> {
  val text = formatter.format(Node.Binary(BinaryOperator.MULTIPLY, d, i).simplify())
  return if (text.startsWith(Token.Operator.MINUS)) -sign to text.removePrefix(Token.Operator.MINUS) else sign to text
}

/** LIATE: what is differentiated comes first. */
private enum class LiateKind {
  LOGARITHM,
  INVERSE,
  ALGEBRAIC,
  TRIGONOMETRIC,
  EXPONENTIAL,
}

private fun Node.liateKind(evaluator: Evaluator): LiateKind? =
  when {
    this is Node.Function && function in setOf(FunctionType.LN, FunctionType.LOG) -> LiateKind.LOGARITHM
    this is Node.Function && function in setOf(FunctionType.ARSIN, FunctionType.ARCOS, FunctionType.ARTAN) ->
      LiateKind.INVERSE
    isLinearFunction(evaluator, setOf(FunctionType.SIN, FunctionType.COS)) -> LiateKind.TRIGONOMETRIC
    isExponential(evaluator) -> LiateKind.EXPONENTIAL
    (polynomialOrNull(evaluator)?.degree ?: 0) >= 1 -> LiateKind.ALGEBRAIC
    else -> null
  }

/** Differentiated and integrated factors, null when this isn't a product of two different kinds. */
private fun Node.liateSplit(evaluator: Evaluator): Pair<Node, Node>? {
  val kind = liateKind(evaluator)
  if (kind != null && kind <= LiateKind.INVERSE) return this to Node.Number(KBigDecimal.ONE)
  if (this !is Node.Binary || operator != BinaryOperator.MULTIPLY) return null
  if (!left.dependsOnX() || !right.dependsOnX()) return null
  val leftKind = left.liateKind(evaluator) ?: return null
  val rightKind = right.liateKind(evaluator) ?: return null
  return when {
    leftKind < rightKind -> left to right
    rightKind < leftKind -> right to left
    else -> null
  }
}

private fun Node.isLinearFunction(evaluator: Evaluator, functions: Set<FunctionType>): Boolean =
  this is Node.Function && function in functions && argument.polynomialOrNull(evaluator)?.degree == 1

/** e^(ax+b), exp(ax+b) and c^(ax+b). */
private fun Node.isExponential(evaluator: Evaluator): Boolean =
  when {
    this is Node.Function -> function == FunctionType.EXP && argument.polynomialOrNull(evaluator)?.degree == 1
    this is Node.Binary && operator == BinaryOperator.POWER ->
      !left.dependsOnX() && right.polynomialOrNull(evaluator)?.degree == 1
    else -> false
  }

private fun Node.isZeroPolynomial(evaluator: Evaluator) = polynomialOrNull(evaluator)?.isZero == true

/** Enough for x⁶ times eˣ, and a limit for anything that wouldn't stop. */
private const val MAX_ROWS = 8
