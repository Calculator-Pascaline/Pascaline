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

import com.sadellie.unitto.core.common.Token
import kotlin.math.E
import kotlin.math.PI
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

private val SUP = Token.Layout.SUPERSCRIPT_START
private val SUB = Token.Layout.SUBSCRIPT_START
private val END = Token.Layout.END
private val NUM = Token.Layout.NUMERATOR_START
private val DEN = Token.Layout.DENOMINATOR_START
private val MINUS = Token.Operator.MINUS

private fun pow(n: Any) = "^$SUP$n$END"

private fun assertValue(expected: Double, input: String, x: Double, radianMode: Boolean = true) =
  assertEquals(expected, GraphFunction(input, radianMode).valueAt(x), 1e-9)

class GraphFunctionTest {
  @Test fun polynomial() = assertValue(11.0, "x${pow(2)}+2x+3", 2.0)

  @Test fun sine() = assertValue(1.0, "sin(x)", PI / 2)

  @Test fun sineDegrees() = assertValue(0.5, "sin(x)", 30.0, radianMode = false)

  @Test fun inverseDegrees() = assertValue(30.0, "asin(x)", 0.5, radianMode = false)

  @Test fun fraction() = assertValue(0.25, "${NUM}1$END${DEN}x$END", 4.0)

  @Test fun constants() = assertValue(PI * E, "πe", 0.0)

  @Test fun logBase() = assertValue(3.0, "log${SUB}2$END(x)", 8.0)

  @Test fun cubeRootOfNegative() = assertValue(-2.0, "x${pow("${NUM}1$END${DEN}3$END")}", -8.0)

  @Test fun factorial() = assertValue(120.0, "x!", 5.0)

  @Test fun undefined() = assertTrue(GraphFunction("ln(x)").valueAt(-1.0).isNaN())

  @Test fun divideByZero() = assertTrue(GraphFunction("${NUM}1$END${DEN}x$END").valueAt(0.0).isNaN())

  @Test fun derivativeAtX() = assertValue(6.0, "d/dx(x${pow(3)})", -1.4142135623730951)

  @Test fun antiderivative() = assertValue(1.0, "∫$SUB$END$SUP${END}cos(x)dx", PI / 2)

  @Test fun negativeInput() = assertValue(-3.0, "${MINUS}x", 3.0)

  @Test
  fun units() {
    assertFailsWith<ExpressionException> { GraphFunction("x×_m") }
  }
}
