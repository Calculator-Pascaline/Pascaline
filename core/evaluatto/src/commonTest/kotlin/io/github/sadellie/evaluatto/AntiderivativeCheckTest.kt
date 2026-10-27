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
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.fail

private val SUP = Token.Layout.SUPERSCRIPT_START
private val END = Token.Layout.END
private val NUM = Token.Layout.NUMERATOR_START
private val DEN = Token.Layout.DENOMINATOR_START
private val RAD = Token.Layout.RADICAND_START
private val MINUS = Token.Operator.MINUS

private fun pow(n: Any) = "^$SUP$n$END"

private fun frac(top: String, bottom: String) = "$NUM$top$END$DEN$bottom$END"

private fun root(inner: String) = "√$RAD$inner$END"

/**
 * Every antiderivative is checked by differentiating it numerically: (F(x+h) − F(x−h))/2h must be
 * f(x). Works whatever form the result is written in.
 */
class AntiderivativeCheckTest {
  private fun assertAntiderivative(
    integrand: String,
    points: List<Double> = listOf(0.3, 0.55, 0.8),
    radianMode: Boolean = true,
  ) {
    val result =
      (Expression("∫${integrand}dx", radianMode).evaluate(PRECISION, OutputFormat.PLAIN) as? ExpressionResult.Symbolic)
        ?.expression ?: fail("No antiderivative for $integrand")
    val antiderivative = GraphFunction(result.removeSuffix("+${Token.DisplayOnly.INTEGRATION_CONSTANT}"), radianMode)
    val function = GraphFunction(integrand, radianMode)
    for (x in points) {
      val slope = (antiderivative.valueAt(x + H) - antiderivative.valueAt(x - H)) / (2 * H)
      val expected = function.valueAt(x)
      assertTrue(
        abs(slope - expected) < TOLERANCE * maxOf(1.0, abs(expected)),
        "∫$integrand: F′($x) = $slope, f($x) = $expected, F = $result",
      )
    }
  }

  // Powers of trigonometric functions
  @Test fun sinSquared() = assertAntiderivative("sin(x)${pow(2)}")

  @Test fun cosCubed() = assertAntiderivative("cos(x)${pow(3)}")

  @Test fun sinFourthLinear() = assertAntiderivative("sin(2x+1)${pow(4)}")

  @Test fun tanSquared() = assertAntiderivative("tan(x)${pow(2)}")

  @Test fun tanCubed() = assertAntiderivative("tan(x)${pow(3)}")

  @Test fun secSquared() = assertAntiderivative(frac("1", "cos(x)${pow(2)}"))

  @Test fun cscSquared() = assertAntiderivative(frac("1", "sin(x)${pow(2)}"))

  // Products with different arguments
  @Test fun sinCos2x() = assertAntiderivative("sin(x)×cos(2x)")

  @Test fun cos3xSin5x() = assertAntiderivative("cos(3x)×sin(5x)")

  @Test fun sinSin() = assertAntiderivative("sin(2x)×sin(3x)")

  @Test fun cosCos() = assertAntiderivative("cos(x)×cos(4x)")

  // Square roots of quadratics
  @Test fun circle() = assertAntiderivative(root("1${MINUS}x${pow(2)}"))

  @Test fun circleRadius2() = assertAntiderivative(root("4${MINUS}x${pow(2)}"))

  @Test fun rootShifted() = assertAntiderivative(root("3+2x${MINUS}x${pow(2)}"))

  @Test fun hyperbola() = assertAntiderivative(root("x${pow(2)}+1"))

  // Inverse and other functions
  @Test fun atan() = assertAntiderivative("atan(x)")

  @Test fun atanLinear() = assertAntiderivative("atan(2x)")

  @Test fun asin() = assertAntiderivative("asin(x)")

  @Test fun acos() = assertAntiderivative("acos(x)")

  @Test fun asinh() = assertAntiderivative("asinh(x)")

  @Test fun acosh() = assertAntiderivative("acosh(x)", points = listOf(1.5, 2.0, 3.0))

  @Test fun atanh() = assertAntiderivative("atanh(x)")

  @Test fun log10() = assertAntiderivative("log(x)")

  @Test fun tanh() = assertAntiderivative("tanh(x)")

  // Earlier forms, checked the same way
  @Test fun byParts() = assertAntiderivative("x${pow(2)}×e${pow("x")}")

  @Test fun substitution() = assertAntiderivative("2x×cos(x${pow(2)})")

  @Test fun expSin() = assertAntiderivative("e${pow("x")}×sin(x)")

  @Test fun quadratic() = assertAntiderivative(frac("x+1", "x${pow(2)}+1"))

  @Test fun lnAbsNegative() = assertAntiderivative(frac("1", "x"), points = listOf(-2.0, -0.5, 1.5))

  // Degrees
  private val degrees = listOf(20.0, 35.0, 50.0)

  @Test fun sinSquaredDegrees() = assertAntiderivative("sin(x)${pow(2)}", degrees, radianMode = false)

  @Test fun cosCubedDegrees() = assertAntiderivative("cos(x)${pow(3)}", degrees, radianMode = false)

  @Test fun tanSquaredDegrees() = assertAntiderivative("tan(x)${pow(2)}", degrees, radianMode = false)

  @Test fun expSinDegrees() = assertAntiderivative("e${pow("x")}×sin(x)", listOf(0.3, 1.0, 2.0), radianMode = false)

  @Test fun secDegrees() = assertAntiderivative(frac("1", "cos(x)"), degrees, radianMode = false)

  // sec, csc, cot and their powers
  @Test fun sec() = assertAntiderivative(frac("1", "cos(x)"))

  @Test fun csc() = assertAntiderivative(frac("1", "sin(x)"))

  @Test fun cot() = assertAntiderivative(frac("1", "tan(x)"))

  @Test fun secCubed() = assertAntiderivative(frac("1", "cos(x)${pow(3)}"))

  @Test fun cscCubed() = assertAntiderivative(frac("1", "sin(x)${pow(3)}"))

  @Test fun secFourthLinear() = assertAntiderivative(frac("1", "cos(2x)${pow(4)}"))

  @Test fun cotSquared() = assertAntiderivative(frac("1", "tan(x)${pow(2)}"))

  // Products of powers
  @Test fun sin2cos3() = assertAntiderivative("sin(x)${pow(2)}×cos(x)${pow(3)}")

  @Test fun sin3cos2() = assertAntiderivative("sin(x)${pow(3)}×cos(x)${pow(2)}")

  @Test fun sin2cos2() = assertAntiderivative("sin(x)${pow(2)}×cos(x)${pow(2)}")

  @Test fun sin4cos2Linear() = assertAntiderivative("sin(2x)${pow(4)}×cos(2x)${pow(2)}")

  @Test fun constantInsideProduct() = assertAntiderivative("2sin(x)${pow(2)}×cos(x)${pow(3)}")

  // Polynomial × inverse function
  @Test fun xAtan() = assertAntiderivative("x×atan(x)")

  @Test fun x2Atan() = assertAntiderivative("x${pow(2)}×atan(x)")

  @Test fun xAsin() = assertAntiderivative("x×asin(x)")

  @Test fun x2Acos() = assertAntiderivative("x${pow(2)}×acos(x)")

  @Test fun xAsinh() = assertAntiderivative("x×asinh(x)")

  @Test fun xAcosh() = assertAntiderivative("x×acosh(x)", points = listOf(1.5, 2.0, 3.0))

  @Test fun xAtanh() = assertAntiderivative("x×atanh(x)")

  @Test fun xAtanDegrees() = assertAntiderivative("x×atan(x)", radianMode = false)

  // Fractions and roots that by parts leads to
  @Test fun longDivision() = assertAntiderivative(frac("x${pow(3)}", "x${pow(2)}+1"))

  @Test fun longDivisionLinear() = assertAntiderivative(frac("x${pow(2)}+3", "x+1"))

  @Test fun polynomialOverRoot() = assertAntiderivative(frac("x${pow(2)}", root("1${MINUS}x${pow(2)}")))

  @Test fun cubicOverRoot() = assertAntiderivative(frac("x${pow(3)}+x", root("x${pow(2)}+4")))

  @Test fun polynomialTimesRoot() = assertAntiderivative("x${pow(2)}×${root("1${MINUS}x${pow(2)}")}")

  @Test fun acoshForm() = assertAntiderivative(frac("1", root("x${pow(2)}${MINUS}1")), points = listOf(1.5, 2.0, 3.0))

  @Test fun rootAcosh() = assertAntiderivative(root("x${pow(2)}${MINUS}1"), points = listOf(1.5, 2.0, 3.0))

  // Partial fractions
  @Test fun cubicSum() = assertAntiderivative(frac("1", "x${pow(3)}+1"))

  @Test fun cubicDifference() = assertAntiderivative(frac("x", "x${pow(3)}${MINUS}1"))

  @Test fun threeLinearFactors() = assertAntiderivative(frac("1", "x${pow(3)}${MINUS}x"))

  @Test fun repeatedLinear() = assertAntiderivative(frac("x${pow(2)}+1", "x${pow(3)}${MINUS}3x+2"))

  @Test fun repeatedQuadratic() = assertAntiderivative(frac("1", "x${pow(4)}+2x${pow(2)}+1"))

  @Test fun repeatedQuadraticLinearTop() = assertAntiderivative(frac("x+3", "x${pow(4)}+2x${pow(2)}+1"))

  @Test fun quartic() = assertAntiderivative(frac("1", "x${pow(4)}${MINUS}1"))

  @Test fun divisionThenPartialFractions() = assertAntiderivative(frac("x${pow(4)}", "x${pow(3)}+1"))

  @Test fun scaledBottom() = assertAntiderivative(frac("5", "2x${pow(3)}+2"))

  @Test fun logOfCubic() = assertAntiderivative(frac("3x${pow(2)}", "x${pow(3)}+1"))

  private companion object {
    const val PRECISION = 10
    const val H = 1e-5
    const val TOLERANCE = 1e-5
  }
}
