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
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Angle unit in a trigonometric function wins over the angle mode. */
class AngleUnitsTest {
  private fun assertBothModes(expr: String, result: String) {
    assertExpr(expr, result, radianMode = true)
    assertExpr(expr, result, radianMode = false)
  }

  // Exactly, assertExpr would round a tiny number to 0
  private fun assertExactlyZero(expr: String) {
    assertEquals(0, Expression(expr, radianMode = true).calculate().compareTo(KBigDecimal.ZERO))
    assertEquals(0, Expression(expr, radianMode = false).calculate().compareTo(KBigDecimal.ZERO))
  }

  private fun assertNoTangent(expr: String, radianMode: Boolean) =
    assertExprFail(ExpressionException.DivideByZero(), expr, radianMode)

  @Test fun tangentOfRightAngle() {
    assertNoTangent("tan(90_°)", radianMode = true)
    assertNoTangent("tan(90_°)", radianMode = false)
  }

  @Test fun tangentOfThreeRightAngles() = assertNoTangent("tan(270_°)", radianMode = true)

  @Test fun tangentOfRightAngleInRadians() = assertNoTangent("tan(π÷2_rad)", radianMode = false)

  @Test fun tangentOfRightAngleInDegreeMode() = assertNoTangent("tan(90)", radianMode = false)

  @Test fun tangentOfRightAngleInRadianMode() = assertNoTangent("tan(π÷2)", radianMode = true)

  @Test fun cosineOfRightAngleIsZero() = assertExactlyZero("cos(90_°)")

  @Test fun cosineOfThreeRightAnglesIsZero() = assertExactlyZero("cos(270_°)")

  @Test fun sineOfHalfTurnIsZero() = assertExactlyZero("sin(180_°)")

  @Test fun sineOfFullTurnIsZero() = assertExactlyZero("sin(360_°)")

  @Test fun tangentOfHalfTurnIsZero() = assertExactlyZero("tan(180_°)")

  @Test fun cosineOfDegrees() = assertBothModes("cos(45_°)", "0.7071067812")

  @Test fun sineOfDegrees() = assertBothModes("sin(30_°)", "0.5")

  @Test fun tangentOfDegrees() = assertBothModes("tan(45_°)", "1")

  @Test fun sineOfRadians() = assertBothModes("sin(π÷2_rad)", "1")

  @Test fun cosineOfRadians() = assertBothModes("cos(π_rad)", "-1")

  @Test fun unitIsForWholeArgument() = assertBothModes("cos(30+30_°)", "0.5")

  @Test fun insideOfExpression() = assertBothModes("2×sin(30_°)+1", "2")

  private fun exactOf(expr: String, radianMode: Boolean = true): String? =
    (Expression(expr, radianMode).evaluate(3, 0) as ExpressionResult.Real).exact

  private fun fractionOf(expr: String, radianMode: Boolean = true): ExactFraction? =
    (Expression(expr, radianMode).evaluate(3, 0) as ExpressionResult.Real).fraction

  @Test
  fun exactRoots() {
    assertTrue(exactOf("cos(45_°)")!!.contains("√"))
    assertTrue(exactOf("sin(60_°)")!!.contains("√"))
    assertTrue(exactOf("tan(30_°)")!!.contains("√"))
    assertTrue(exactOf("sin(π÷4_rad)", radianMode = false)!!.contains("√"))
  }

  @Test
  fun exactFractions() {
    assertEquals(fraction(1, 2), fractionOf("cos(60_°)"))
    assertEquals(fraction(-1, 2), fractionOf("sin(210_°)"))
    assertEquals(fraction(1, 1), fractionOf("tan(45_°)"))
    assertEquals(fraction(-1, 1), fractionOf("cos(π_rad)"))
    assertEquals(fraction(1, 2), fractionOf("sin(π÷6)"))
    assertEquals(fraction(1, 2), fractionOf("cos(60)", radianMode = false))
  }

  @Test
  fun notSpecialAngle() {
    assertNull(exactOf("cos(20_°)"))
    assertNull(fractionOf("cos(20_°)"))
  }

  private fun fraction(numerator: Int, denominator: Int) =
    ExactFraction(KBigDecimal(numerator.toString()), KBigDecimal(denominator.toString()))

  @Test
  fun withoutUnitAngleModeIsUsed() {
    assertExpr("cos(60)", "0.5", radianMode = false)
    assertExpr("cos(π)", "-1", radianMode = true)
  }
}
