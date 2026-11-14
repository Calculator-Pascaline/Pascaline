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
import kotlin.test.assertNull

private val SUP = Token.Layout.SUPERSCRIPT_START
private val END = Token.Layout.END

private fun pow(n: Any) = "^$SUP$n$END"

private fun table(integrand: String) = Expression("∫${integrand}dx").diTable(3, OutputFormat.PLAIN)

/** D column, I column and the stop, to compare in one go. */
private fun summary(integrand: String): Triple<List<String>, List<String>, DiStop>? =
  table(integrand)?.let { table -> Triple(table.rows.map { it.differentiated }, table.rows.map { it.integrated }, table.stop) }

class DiMethodTest {
  // Stop 1: x² is differentiated down to 0
  @Test
  fun derivativeReachesZero() {
    val result = summary("x${pow(2)}e${pow("x")}")
    assertEquals(DiStop.ZERO, result?.third)
    assertEquals(4, result?.first?.size)
    assertEquals("0", result?.first?.last())
  }

  // Stop 2: ln(x) differentiates into 1/x, which times x²/2 is x/2
  @Test
  fun rowCanBeIntegrated() {
    val result = summary("xln(x)")
    assertEquals(DiStop.INTEGRABLE, result?.third)
    assertEquals(2, result?.first?.size)
    assertEquals("ln(x)", result?.first?.first())
  }

  // Stop 3: e^x times sin(x) comes back after two rows
  @Test
  fun integralComesBack() {
    val result = summary("e${pow("x")}sin(x)")
    assertEquals(DiStop.REPEATS, result?.third)
    assertEquals(3, result?.first?.size)
    assertEquals("sin(x)", result?.first?.first())
  }

  // The steps stay visible: x²eˣ−2xeˣ+2eˣ
  @Test
  fun setupKeepsSteps() =
    assertEquals("x${pow(2)}×e${pow("x")}−2x×e${pow("x")}+2e${pow("x")}", table("x${pow(2)}e${pow("x")}")?.setup)

  // The integral comes back with a minus
  @Test
  fun setupEndsWithTheIntegral() =
    assertEquals("sin(x)×e${pow("x")}−cos(x)×e${pow("x")}−∫sin(x)×e${pow("x")}dx", table("e${pow("x")}sin(x)")?.setup)

  // ln alone is ln(x)×1
  @Test
  fun lnAlone() = assertEquals(DiStop.INTEGRABLE, table("ln(x)")?.stop)

  @Test
  fun signsAlternate() = assertEquals(listOf(1, -1, 1, -1), table("x${pow(3)}sin(x)")?.rows?.dropLast(1)?.map { it.sign })

  @Test
  fun polynomialTimesTrigStopsAtZero() = assertEquals(DiStop.ZERO, table("x${pow(3)}sin(x)")?.stop)

  @Test fun notAProduct() = assertNull(table("x${pow(2)}"))

  @Test fun sameKinds() = assertNull(table("sin(x)cos(x)"))

  @Test fun definiteIntegral() = assertNull(Expression("∫${Token.Layout.SUBSCRIPT_START}0$END${SUP}1${END}xe${pow("x")}dx").diTable(3, OutputFormat.PLAIN))
}

