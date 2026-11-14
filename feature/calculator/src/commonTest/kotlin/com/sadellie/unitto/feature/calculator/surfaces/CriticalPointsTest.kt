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

package com.sadellie.unitto.feature.calculator.surfaces

import com.sadellie.unitto.feature.calculator.integrals.MultiFunction
import kotlin.math.PI
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private val SQUARE = Region(-2.0, 2.0, -2.0, 2.0)

private fun points(function: String) = MultiFunction(function).criticalPoints(SQUARE)

/** Kind, x, y of each point, rounded to compare. */
private fun summary(function: String) = points(function).map { Triple(it.kind, round(it.x), round(it.y)) }

private fun round(value: Double) = kotlin.math.round(value * 1000) / 1000 + 0.0

class CriticalPointsTest {
  @Test
  fun minimumAndSaddle() {
    val found = points("x³ − 3x + y²")
    assertEquals(listOf(Triple(PointKind.MINIMUM, 1.0, 0.0), Triple(PointKind.SADDLE, -1.0, 0.0)), summary("x³ − 3x + y²"))
    assertEquals(-2.0, found.first().z, 1e-9)
    assertEquals(2.0, found.last().z, 1e-9)
  }

  @Test fun saddle() = assertEquals(listOf(Triple(PointKind.SADDLE, 0.0, 0.0)), summary("x² − y²"))

  @Test fun hill() = assertEquals(listOf(Triple(PointKind.MAXIMUM, 0.0, 0.0)), summary("e^(−x² − y²)"))

  // sin(x)·cos(y): a top and a bottom at x = ±π/2, saddles at y = ±π/2
  @Test
  fun eggBox() {
    val found = summary("sin(x)·cos(y)")
    val half = round(PI / 2)
    assertTrue(Triple(PointKind.MAXIMUM, half, 0.0) in found, "$found")
    assertTrue(Triple(PointKind.MINIMUM, -half, 0.0) in found, "$found")
    assertTrue(Triple(PointKind.SADDLE, 0.0, half) in found, "$found")
    assertTrue(Triple(PointKind.SADDLE, 0.0, -half) in found, "$found")
    assertEquals(4, found.size, "$found")
  }

  // Monkey saddle: all second derivatives are 0, the test can't tell
  @Test fun monkeySaddle() = assertEquals(listOf(Triple(PointKind.FLAT, 0.0, 0.0)), summary("x³ − 3x·y²"))

  @Test fun noPoint() = assertEquals(emptyList(), summary("x + y"))
}
