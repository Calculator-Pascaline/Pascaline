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
import com.sadellie.unitto.core.common.OutputFormat
import com.sadellie.unitto.core.common.Token
import com.sadellie.unitto.core.common.toFormattedString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

private val SUP = Token.Layout.SUPERSCRIPT_START
private val SUB = Token.Layout.SUBSCRIPT_START
private val END = Token.Layout.END
private val NUM = Token.Layout.NUMERATOR_START
private val DEN = Token.Layout.DENOMINATOR_START
private val RAD = Token.Layout.RADICAND_START
private val MINUS = Token.Operator.MINUS

private fun pow(n: Any) = "^$SUP$n$END"

private fun evaluate(expr: String, precision: Int = 3) =
  Expression(expr).evaluate(precision, OutputFormat.PLAIN)

private fun assertSymbolic(expected: String, expr: String, precision: Int = 3) =
  assertEquals(expected, (evaluate(expr, precision) as ExpressionResult.Symbolic).expression)

private fun assertFactorization(expected: String, expr: String) =
  assertEquals(expected, (evaluate(expr) as ExpressionResult.Symbolic).factorization)

private fun assertRoots(expected: List<String>, expr: String) =
  assertEquals(expected, (evaluate(expr) as ExpressionResult.Symbolic).roots)

private fun assertComplex(re: String, im: String, expr: String) {
  val result = evaluate(expr) as ExpressionResult.Complex
  assertEquals(
    re to im,
    result.real.toFormattedString(10, OutputFormat.PLAIN) to
      result.imaginary.toFormattedString(10, OutputFormat.PLAIN),
  )
}

class ExpressionCalculusTest {
  // Complex
  @Test fun sqrtNegative() = assertComplex("0", "2.2360679775", "√(${MINUS}5)")

  @Test fun sqrtNegativeSlot() = assertComplex("0", "2", "√$RAD${MINUS}4$END")

  @Test fun complexArithmetic() = assertComplex("3", "2", "3+√(${MINUS}4)")

  @Test fun complexSquare() = assertExpr("√(${MINUS}5)×√(${MINUS}5)", "-5")

  @Test fun imaginaryUnit() = assertExpr("i${pow(2)}", "-1")

  @Test fun complexDivision() = assertComplex("0", "-1", "1÷i")

  @Test fun sqrtSlot() = assertExpr("√${RAD}16$END+1", "5")

  @Test
  fun realOnlyFunctionOfComplex() =
    assertExprFail(ExpressionException.BadExpression(), "sin(√(${MINUS}1))")

  // Integrals
  @Test fun integralPolynomial() = assertExpr("∫${SUB}0$END${SUP}3${END}x${pow(2)}dx", "9")

  @Test fun integralSin() = assertExpr("∫${SUB}0$END${SUP}π${END}sin(x)dx", "2")

  @Test fun integralReversed() = assertExpr("∫${SUB}1$END${SUP}0${END}2xdx", "-1")

  @Test fun integralExp() = assertExpr("∫${SUB}0$END${SUP}1${END}e${pow("x")}dx", "1.7182818285")

  @Test fun integralTimesNumber() = assertExpr("2∫${SUB}0$END${SUP}1${END}xdx", "1")

  @Test
  fun integralMissingBound() =
    assertExprFail(ExpressionException.BadExpression(), "∫${SUB}$END${SUP}1${END}xdx")

  // Derivatives
  @Test fun derivativeAtPoint() = assertExpr("d/dx(x${pow(3)})|${SUB}2$END", "12")

  @Test fun derivativeSinAtZero() = assertExpr("d/dx(sin(x))|${SUB}0$END", "1")

  @Test fun derivativeLn() = assertExpr("d/dx(ln(x))|${SUB}4$END", "0.25")

  @Test fun derivativeSymbolic() = assertSymbolic("3x${pow(2)}+2x", "d/dx(x${pow(3)}+x${pow(2)}+1)")

  @Test fun derivativeOfConstant() = assertSymbolic("0", "d/dx(5)")

  @Test
  fun indefiniteIntegral() =
    assertSymbolic("${NUM}1${END}${DEN}3${END}x${pow(3)}+C", "∫x${pow(2)}dx")

  @Test fun indefiniteIntegralDecimal() = assertSymbolic("0.5x${pow(2)}+C", "∫xdx")

  // Factorization
  @Test fun factorQuadratic() = assertFactorization("(x${MINUS}2)(x+3)", "x${pow(2)}+x${MINUS}6")

  @Test fun factorCommon() = assertFactorization("2x(x+2)", "2x${pow(2)}+4x")

  @Test fun factorRepeated() = assertFactorization("(x+1)${pow(2)}", "x${pow(2)}+2x+1")

  @Test fun factorRationalRoot() = assertFactorization("(x+1)(2x${MINUS}1)", "2x${pow(2)}+x${MINUS}1")

  @Test
  fun factorQuartic() =
    assertFactorization("(x${pow(2)}+2)(x${pow(2)}+1)", "x${pow(4)}+3x${pow(2)}+2")

  @Test fun factorIrreducibleQuadratic() = assertFactorization("x${pow(2)}+1", "x${pow(2)}+1")

  @Test
  fun factorQuinticOverReals() =
    assertFactorization(
      "≈(x+1.528)(x${pow(2)}+0.727x+1.831)(x${pow(2)}${MINUS}2.255x+2.145)",
      "x${pow(5)}+x${pow(2)}+6",
    )

  @Test fun factorNegative() = assertFactorization("${MINUS}(x${MINUS}1)", "1${MINUS}x")

  @Test fun variableOnly() = assertSymbolic("x", "x")

  @Test fun notPolynomialSin() = assertSymbolic("sin(x)", "sin(x)")

  @Test fun calculateWithVariable() = assertExprFail(ExpressionException.BadExpression(), "x+1")

  @Test
  fun realResult() =
    assertEquals(
      "4",
      (evaluate("2+2") as ExpressionResult.Real).value.toFormattedString(0, OutputFormat.PLAIN),
    )

  // Symbolic derivatives of any function
  @Test
  fun derivativeWithCos() =
    assertSymbolic("6x${pow(5)}+6x${pow(2)}${MINUS}sin(x)", "d/dx(x${pow(6)}+2x${pow(3)}+cos(x))")

  @Test fun derivativeSin() = assertSymbolic("cos(x)", "d/dx(sin(x))")

  @Test fun derivativeChain() = assertSymbolic("${MINUS}2sin(2x)", "d/dx(cos(2x))")

  @Test fun derivativeExp() = assertSymbolic("e${pow("x")}", "d/dx(e${pow("x")})")

  @Test fun derivativeLnSymbolic() = assertSymbolic("${NUM}1$END${DEN}x$END", "d/dx(ln(x))")

  @Test
  fun derivativeProduct() = assertSymbolic("sin(x)+x×cos(x)", "d/dx(xsin(x))")

  // Roots
  @Test
  fun rootsQuintic() =
    assertRoots(
      listOf("x≈${MINUS}1.528,  x≈${MINUS}0.364±1.303i,  x≈1.128±0.934i"),
      "x${pow(5)}+x${pow(2)}+6",
    )

  @Test fun rootsRational() = assertRoots(listOf("x=2,  x=${MINUS}3"), "x${pow(2)}+x${MINUS}6")

  @Test fun rootsImaginary() = assertRoots(listOf("x=±i"), "x${pow(2)}+1")

  @Test fun rootsIrrational() = assertRoots(listOf("x=±√${RAD}2$END", "x≈${MINUS}1.414,  x≈1.414"), "x${pow(2)}${MINUS}2")

  @Test fun rootsZero() = assertRoots(listOf("x=0,  x=${MINUS}2"), "x${pow(2)}+2x")

  // Exact answers
  @Test
  fun factorQuadraticWithSquareRoots() =
    assertFactorization("(x+3${MINUS}√${RAD}3$END)(x+3+√${RAD}3$END)", "x${pow(2)}+6x+6")

  @Test
  fun rootsQuadraticWithSquareRoots() =
    assertRoots(listOf("x=${MINUS}3±√${RAD}3$END", "x≈${MINUS}4.732,  x≈${MINUS}1.268"), "x${pow(2)}+6x+6")

  @Test
  fun rootsComplexWithSquareRoot() =
    assertRoots(listOf("x=${MINUS}1±√${RAD}2${END}i", "x≈${MINUS}1±1.414i"), "x${pow(2)}+2x+3")

  @Test
  fun exactSqrtNegative() =
    assertEquals("√${RAD}5${END}i", (evaluate("√(${MINUS}5)") as ExpressionResult.Complex).exact)

  @Test
  fun exactSqrt8() = assertEquals("2√${RAD}2$END", (evaluate("√(8)") as ExpressionResult.Real).exact)

  @Test fun exactRational() = assertEquals(null, (evaluate("√(9)") as ExpressionResult.Real).exact)

  @Test
  fun exactComplexSum() =
    assertEquals("1+√${RAD}3${END}i", (evaluate("1+√(${MINUS}3)") as ExpressionResult.Complex).exact)

  // Symbolic integrals
  @Test fun integralCos() = assertSymbolic("sin(x)+C", "∫cos(x)dx")

  // Expressions with x that are not polynomials
  @Test fun notPolynomial() = assertSymbolic("x${pow(3)}+cos(x)", "x${pow(3)}+cos(x)")

  @Test fun notPolynomialFunction() = assertSymbolic("ln(x)", "ln(x)")

  // Like terms are added up
  @Test fun likeTerms() = assertSymbolic("2sin(x)", "sin(x)+sin(x)")

  @Test fun likeTermsCoefficients() = assertSymbolic("2x×cos(x)", "3x×cos(x)${MINUS}x×cos(x)")

  @Test fun likeTermsCancel() = assertSymbolic("1", "sin(x)+1${MINUS}sin(x)")

  @Test fun likeTermsMixed() = assertSymbolic("3cos(x)+ln(x)", "cos(x)+ln(x)+2cos(x)")

  // Same template as definite integral, bounds left empty
  @Test fun integralEmptyBounds() = assertSymbolic("sin(x)+C", "∫$SUB$END$SUP${END}cos(x)dx")

  @Test
  fun integralOneBound() {
    assertFailsWith<ExpressionException.BadExpression> { evaluate("∫${SUB}1$END$SUP${END}cos(x)dx") }
  }

  @Test fun integralSinSymbolic() = assertSymbolic("${MINUS}cos(x)+C", "∫sin(x)dx")

  @Test fun integralCosLinear() = assertSymbolic("${NUM}1$END${DEN}2${END}sin(2x)+C", "∫cos(2x)dx")

  @Test
  fun integralMixed() =
    assertSymbolic("${NUM}x${pow(3)}$END${DEN}3$END+sin(x)+C", "∫(x${pow(2)}+cos(x))dx")

  @Test fun integralExpSymbolic() = assertSymbolic("e${pow("x")}+C", "∫e${pow("x")}dx")

  @Test fun integralOneOverX() = assertSymbolic("ln(x)+C", "∫${NUM}1$END${DEN}x${END}dx")

  @Test fun integralConstantTimes() = assertSymbolic("3sin(x)+C", "∫3cos(x)dx")

  @Test
  fun integralByPartsNotSupported() {
    assertFailsWith<ExpressionException.BadExpression> { evaluate("∫xsin(x)dx") }
  }

  // Polynomials are expanded
  @Test
  fun expandProduct() = assertSymbolic("x${pow(2)}+9x+18", "(x+6)(x+3)")

  @Test fun expandKeepsOrder() = assertSymbolic("x${pow(6)}+x", "x${pow(6)}+x")

  @Test fun factorizationOfProduct() = assertFactorization("(x+3)(x+6)", "(x+6)(x+3)")

  @Test fun derivativeOfIntegralHasNoConstant() = assertSymbolic("cos(x)", "d/dx(∫cos(x)dx)")

  // Discriminant
  @Test
  fun discriminant() =
    assertEquals("Δ=12", (evaluate("x${pow(2)}+6x+6") as ExpressionResult.Symbolic).discriminant)

  @Test
  fun discriminantNegative() =
    assertEquals("Δ=${MINUS}4", (evaluate("x${pow(2)}+1") as ExpressionResult.Symbolic).discriminant)

  @Test
  fun noDiscriminantForCubic() =
    assertEquals(null, (evaluate("x${pow(3)}+1") as ExpressionResult.Symbolic).discriminant)

  // Integers
  @Test
  fun integerInfoSix() =
    assertEquals(IntegerInfo("6", "0x6", "0b110", "2×3"), integerInfo(KBigDecimal("6")))

  @Test
  fun integerInfoPowers() =
    assertEquals(IntegerInfo("360", "0x168", "0b101101000", "2${pow(3)}×3${pow(2)}×5"), integerInfo(KBigDecimal("360")))

  @Test
  fun integerInfoNegativePrime() =
    assertEquals(IntegerInfo("${MINUS}7", "${MINUS}0x7", "${MINUS}0b111", "${MINUS}7"), integerInfo(KBigDecimal("-7")))

  @Test fun integerInfoOne() = assertEquals(null, integerInfo(KBigDecimal("1"))?.primeFactors)

  @Test fun integerInfoDecimal() = assertEquals(null, integerInfo(KBigDecimal("2.5")))

  @Test fun integerInfoScaled() = assertEquals("0x6", integerInfo(KBigDecimal("6.000"))?.hexadecimal)

  // Hyperbolic calculus
  @Test fun derivativeSinh() = assertSymbolic("cosh(x)", "d/dx(sinh(x))")

  @Test fun integralCosh() = assertSymbolic("sinh(x)+C", "∫cosh(x)dx")

  // Exact parts of complex numbers
  private fun parts(expr: String) = (evaluate(expr) as ExpressionResult.Complex).parts

  @Test
  fun complexPartsWithSquareRoot() =
    assertEquals(
      ComplexParts("1", "√${RAD}5$END", "√${RAD}6$END", null),
      parts("√(${MINUS}5)+1"),
    )

  @Test
  fun complexPartsAngle() =
    assertEquals(ComplexParts("1", "1", "√${RAD}2$END", "${NUM}π$END${DEN}4$END"), parts("1+i"))

  @Test
  fun complexPartsImaginary() =
    assertEquals(ComplexParts("0", "2", "2", "${NUM}π$END${DEN}2$END"), parts("√(${MINUS}4)"))

  @Test
  fun complexPartsNotExact() = assertEquals(null, parts("√(${MINUS}2)×π")?.modulus)
}
