/*
 * Unitto is a calculator for Android
 * Copyright (c) 2023-2025 Elshan Agaev
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
import com.sadellie.unitto.core.common.Token

sealed class ExpressionException(override val message: String) : Exception(message) {
  class DivideByZero : ExpressionException("Can't divide by zero")

  class FactorialCalculation :
    ExpressionException("Can calculate factorial of non-negative real numbers only")

  class BadExpression :
    ExpressionException("Invalid expression. Operator lacks argument or missing closing bracket")

  class TooBig : ExpressionException("Value is too big")
}

/**
 * Exact parts of a complex number, with layout slots. null when part can't be written exactly.
 *
 * @property argument Angle in radians, like `π/4`.
 */
data class ComplexParts(
  val real: String?,
  val imaginary: String?,
  val modulus: String?,
  val argument: String?,
)

/** Number with unit (layout slots), like 5000 and `_m`. */
data class UnitValue(val value: KBigDecimal, val unit: String)

/** Result of [Expression.evaluate]. */
sealed interface ExpressionResult {
  /** @property exact Exact value with square roots, like `2√2`. null when value is rational. */
  data class Real(
    val value: KBigDecimal,
    val exact: String? = null,
    /** Unit of [value] with layout slots, like `_km` or `⟨_m⟩⟨_s⟩`. */
    val unit: String? = null,
    /** Same value in SI units without prefix: 5 km ==> 5000 m. */
    val si: UnitValue? = null,
    /** SI base units when they differ from [si] unit: N ==> kg×m/s². Value is the same. */
    val baseUnits: String? = null,
  ) : ExpressionResult

  /** @property exact Exact value with square roots, like `√5i`. */
  data class Complex(
    val real: KBigDecimal,
    val imaginary: KBigDecimal,
    val exact: String? = null,
    val parts: ComplexParts? = null,
  ) : ExpressionResult

  /**
   * Expression with x, like expanded polynomial or derivative. Uses layout slots, see
   * [com.sadellie.unitto.core.common.Token.Layout].
   *
   * @property factorization Factorized polynomial, null when [expression] is not a polynomial.
   * @property roots Lines with exact and approximate roots of a polynomial.
   * @property discriminant `Δ=12` for quadratic polynomials.
   */
  data class Symbolic(
    val expression: String,
    val factorization: String? = null,
    val roots: List<String> = emptyList(),
    val discriminant: String? = null,
  ) : ExpressionResult
}

class Expression(
  input: String,
  radianMode: Boolean = true,
  roundingMode: KRoundingMode = KRoundingMode.HALF_EVEN,
) {
  private val tree = Parser(input.tokenize(), roundingMode).parse()
  private val evaluator = Evaluator(radianMode, roundingMode)
  private val radianMode = radianMode

  /**
   * Real result only.
   *
   * @throws ExpressionException.BadExpression When result is complex or has variables.
   */
  fun calculate(): KBigDecimal {
    try {
      return evaluator.evaluate(tree).real()
    } catch (e: SymbolicExpressionException) {
      throw ExpressionException.BadExpression()
    }
  }

  /**
   * Calculates expression. Expressions with x are polynomials: derivatives and integrals are
   * expanded, everything else is factorized.
   *
   * @param precision Digits after the dot for approximate coefficients in symbolic results.
   * @param outputFormat Format for numbers in symbolic results.
   */
  fun evaluate(precision: Int, outputFormat: Int): ExpressionResult {
    if (tree is Node.Convert || tree.units().isNotEmpty()) {
      return evaluateUnits(tree, evaluator, NodeFormatter(precision, outputFormat))
    }
    val value =
      try {
        evaluator.evaluate(tree)
      } catch (e: SymbolicExpressionException) {
        return evaluateSymbolic(precision, outputFormat)
      }
    val exact =
      tree.exactValue()?.takeIf { it.hasSurd }?.let { SymbolicFormatter(precision, outputFormat).exact(it) }
        // Σ1/n² = π²/6
        ?: if (value.isReal && tree.hasNumericCalculus()) recognizeConstant(value.re) else null
    if (value.isReal) return ExpressionResult.Real(value.re, exact)
    return ExpressionResult.Complex(
      real = value.re,
      imaginary = value.im,
      exact = exact,
      parts = complexParts(tree.exactValue(), value, SymbolicFormatter(precision, outputFormat)),
    )
  }

  /**
   * Polynomials are factorized (with roots), derivatives and integrals of polynomials are expanded.
   * Other derivatives and integrals are calculated symbolically: d/dx(cos(x)) = −sin(x). Other
   * expressions with x are simplified.
   */
  private fun evaluateSymbolic(precision: Int, outputFormat: Int): ExpressionResult.Symbolic {
    // Integration constant
    val constant =
      if (tree.hasIndefiniteIntegral()) {
        Token.Operator.PLUS + Token.DisplayOnly.INTEGRATION_CONSTANT
      } else {
        ""
      }
    val polynomial =
      try {
        tree.toPolynomial(evaluator)
      } catch (e: ExpressionException.BadExpression) {
        // Not a polynomial, like x³+cos(x): show it simplified
        val result = tree.expandCalculus(radianMode, evaluator).simplify()
        return ExpressionResult.Symbolic(NodeFormatter(precision, outputFormat).format(result) + constant)
      }
    if (polynomial.degree > MAX_POLYNOMIAL_DEGREE) throw ExpressionException.TooBig()
    val formatter = SymbolicFormatter(precision, outputFormat)
    val expanded = formatter.expanded(polynomial) + constant
    if (constant.isNotEmpty() || tree is Node.Derivative || polynomial.degree < 1) {
      return ExpressionResult.Symbolic(expanded)
    }
    val factorization = polynomial.factorize()
    return ExpressionResult.Symbolic(
      expression = expanded,
      factorization = formatter.factorized(factorization),
      roots = formatter.roots(factorization),
      discriminant = formatter.discriminant(polynomial),
    )
  }
}
