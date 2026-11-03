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

package com.sadellie.unitto.feature.calculator

import com.sadellie.unitto.core.common.Token
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class HistoryResultTest {
  private fun operand(result: String, operator: String) =
    HistoryResult(lines = listOf(result)).operandFor(operator)

  @Test
  fun numberGoesAsIs() {
    assertEquals("12", operand("12", Token.Operator.PLUS))
    assertEquals("12", operand("12", Token.Operator.MINUS))
    assertEquals("12", operand("12", Token.Operator.MULTIPLY))
    assertEquals("12", operand("12", Token.Layout.FRACTION))
    assertEquals("−1.5", operand("−1.5", Token.Operator.MULTIPLY))
  }

  @Test
  fun approximationWithoutSign() = assertEquals("0.333", operand("≈0.333", Token.Operator.PLUS))

  @Test
  fun bracketsForMultiplyAndDivide() {
    assertEquals("1+√2", operand("1+√2", Token.Operator.PLUS))
    assertEquals("1+√2", operand("1+√2", Token.Operator.MINUS))
    assertEquals("(1+√2)", operand("1+√2", Token.Operator.MULTIPLY))
    assertEquals("(1+√2)", operand("1+√2", Token.Operator.DIVIDE))
    assertEquals("(1+√2)", operand("1+√2", Token.Layout.FRACTION))
  }

  @Test
  fun otherKeysDontUseResult() {
    assertNull(operand("12", Token.Digit.DIGIT_5))
    assertNull(operand("12", Token.Operator.LEFT_BRACKET))
  }

  @Test
  fun errorHasNoResult() =
    assertNull(HistoryResult(lines = emptyList()).operandFor(Token.Operator.PLUS))

  @Test
  fun repeatableOperation() {
    assertEquals("+3", "2+3".repeatableOperation())
    assertEquals("−0.5", "−12−0.5".repeatableOperation())
    assertEquals("×2", "5×2".repeatableOperation())
    assertEquals("÷4", "10÷4".repeatableOperation())
    assertNull("2+3×4".repeatableOperation())
    assertNull("12".repeatableOperation())
    assertNull("√2+3".repeatableOperation())
  }
}
