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

import com.sadellie.unitto.core.common.OutputFormat
import com.sadellie.unitto.core.common.Token
import kotlin.test.Test
import kotlin.test.assertEquals

private val SUP = Token.Layout.SUPERSCRIPT_START
private val SUB = Token.Layout.SUBSCRIPT_START
private val END = Token.Layout.END
private val NUM = Token.Layout.NUMERATOR_START
private val DEN = Token.Layout.DENOMINATOR_START
private val MINUS = Token.Operator.MINUS

private fun pow(n: Any) = "^$SUP$n$END"

private fun fraction(top: String, bottom: String) = "$NUM$top$END$DEN$bottom$END"

private fun sum(lower: String, upper: String, body: String) = "Σ$SUB$lower$END$SUP$upper$END($body)"

private fun product(lower: String, upper: String, body: String) =
  "∏$SUB$lower$END$SUP$upper$END($body)"

private fun integral(lower: String, upper: String, body: String) =
  "∫$SUB$lower$END$SUP$upper$END${body}dx"

private fun exact(expr: String) =
  (Expression(expr).evaluate(3, OutputFormat.PLAIN) as ExpressionResult.Real).exact

class ExpressionSeriesTest {
  @Test fun finiteSum() = assertExpr(sum("1", "10", "n"), "55")

  @Test fun finiteSumOfSquares() = assertExpr(sum("1", "4", "n${pow(2)}"), "30")

  @Test fun finiteProduct() = assertExpr(product("1", "5", "n"), "120")

  @Test fun sumTimesNumber() = assertExpr("2${sum("1", "3", "n")}", "12")

  @Test fun baselProblem() = assertExpr(sum("1", "∞", fraction("1", "n${pow(2)}")), "1.6449340668")

  @Test
  fun baselProblemExact() =
    assertEquals(fraction("π${pow(2)}", "6"), exact(sum("1", "∞", fraction("1", "n${pow(2)}"))))

  @Test
  fun sumOfInverseFactorials() =
    assertEquals("e", exact(sum("0", "∞", fraction("1", "n!"))))

  @Test
  fun alternatingHarmonic() =
    assertEquals(
      "ln(2)",
      exact(sum("1", "∞", fraction("(${MINUS}1)${pow("n+1")}", "n"))),
    )

  @Test fun geometric() = assertExpr(sum("0", "∞", fraction("1", "2${pow("n")}")), "2")

  @Test fun geometricIsNotRecognized() = assertEquals(null, exact(sum("0", "∞", fraction("1", "2${pow("n")}"))))

  @Test
  fun zetaFour() =
    assertEquals(fraction("π${pow(4)}", "90"), exact(sum("1", "∞", fraction("1", "n${pow(4)}"))))

  @Test
  fun wallisProduct() =
    assertEquals(
      fraction("2", "π"),
      exact(product("1", "∞", "1${MINUS}${fraction("1", "4n${pow(2)}")}")),
    )

  @Test
  fun harmonicDiverges() =
    assertExprFail(ExpressionException.BadExpression(), sum("1", "∞", fraction("1", "n")))

  @Test
  fun indexOutsideOfSum() = assertExprFail(ExpressionException.BadExpression(), "n+1")

  @Test
  fun nestedInfiniteSumsStop() =
    assertExprFail(
      ExpressionException.TooBig(),
      sum("1", "∞", sum("1", "∞", fraction("1", "n${pow(2)}"))),
    )

  @Test fun infinityAlone() = assertExprFail(ExpressionException.BadExpression(), "∞")

  // Integrals with infinite bounds
  @Test fun integralExpDecay() = assertExpr(integral("0", "∞", "e${pow("${MINUS}x")}"), "1")

  @Test fun integralInverseSquare() = assertExpr(integral("1", "∞", fraction("1", "x${pow(2)}")), "1")

  @Test
  fun gaussianIntegral() =
    assertExpr(integral("${MINUS}∞", "∞", "e${pow("${MINUS}x${pow(2)}")}"), "1.7724538509")

  @Test
  fun integralFromMinusInfinity() = assertExpr(integral("${MINUS}∞", "0", "e${pow("x")}"), "1")

  @Test
  fun integralOfLorentzianIsPi() =
    assertEquals("π", exact(integral("${MINUS}∞", "∞", fraction("1", "1+x${pow(2)}"))))

  // Hyperbolic functions
  @Test fun sinh() = assertExpr("sinh(1)", "1.1752011936")

  @Test fun cosh() = assertExpr("cosh(1)", "1.5430806348")

  @Test fun tanh() = assertExpr("tanh(1)", "0.761594156")

  @Test fun arsinh() = assertExpr("asinh(1)", "0.881373587")

  @Test fun arcosh() = assertExpr("acosh(2)", "1.3169578969")

  @Test fun artanh() = assertExpr("atanh(0.5)", "0.5493061443")

  @Test fun arcoshBelowOne() = assertExprFail(ExpressionException.BadExpression(), "acosh(0.5)")

  @Test fun lexSinhNotSin() = assertLex(listOf("sinh", "(", "1", ")"), "sinh(1)")
}
