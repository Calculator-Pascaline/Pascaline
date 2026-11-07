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
import kotlin.test.assertTrue

class DegreeIntegralTest {
  private fun antiderivative(integrand: String, radianMode: Boolean) =
    (Expression("∫${integrand}dx", radianMode).evaluate(PRECISION, OutputFormat.PLAIN) as ExpressionResult.Symbolic).expression

  private fun String.count(part: String) = windowed(part.length).count { it == part }

  /** x²·cos(x) in degrees needs 180/π once, twice and three times: written as powers. */
  @Test
  fun repeatedFactorsBecomePowers() {
    val result = antiderivative("x${Token.Operator.POWER}${Token.Layout.SUPERSCRIPT_START}2${Token.Layout.END}cos(x)", radianMode = false)
    assertEquals(3, result.count("180"), result)
    assertTrue(result.count("${Token.Operator.POWER}${Token.Layout.SUPERSCRIPT_START}2") >= 2, result)
    assertTrue(result.count("${Token.Operator.POWER}${Token.Layout.SUPERSCRIPT_START}3") == 1, result)
  }

  @Test
  fun oneFactorStaysAsItIs() {
    val result = antiderivative("cos(x)", radianMode = false)
    assertEquals(1, result.count("180"), result)
  }

  @Test
  fun radiansHaveNone() = assertEquals(0, antiderivative("cos(x)", radianMode = true).count("180"))
}

private const val PRECISION = 3
