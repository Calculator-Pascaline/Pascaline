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

package com.sadellie.unitto.feature.calculator.vectors

import com.sadellie.unitto.feature.calculator.integrals.MultiFunction
import kotlin.math.abs
import kotlin.math.exp
import kotlin.test.Test
import kotlin.test.assertTrue

class FieldMathTest {
  private fun f(text: String) = MultiFunction(text)

  private fun close(expected: Double, actual: Double, tolerance: Double = 1e-5) =
    assertTrue(abs(expected - actual) <= tolerance * maxOf(1.0, abs(expected)), "expected $expected, got $actual")

  @Test
  fun swirlTurnsWithoutSpreading() {
    val values = vectorAt(f("−y"), f("x"), 0.7, -1.2)
    close(0.0, values.divergence)
    close(2.0, values.rotation)
  }

  @Test
  fun sourceSpreadsWithoutTurning() {
    val values = vectorAt(f("x"), f("y"), 2.0, 3.0)
    close(2.0, values.divergence)
    close(0.0, values.rotation)
    close(kotlin.math.sqrt(13.0), values.length)
  }

  @Test
  fun saddleHasNoLaplacian() {
    val values = scalarAt(f("x² − y²"), 1.5, -0.5)
    close(3.0, values.gradientX)
    close(1.0, values.gradientY)
    close(0.0, values.laplacian, 1e-4)
  }

  @Test
  fun bowlAndHill() {
    close(4.0, scalarAt(f("x² + y²"), 0.3, 0.4).laplacian, 1e-4)
    val hill = scalarAt(f("e^(−x² − y²)"), 0.0, 0.0)
    close(1.0, hill.value)
    close(0.0, hill.slope)
    close(-4.0, hill.laplacian, 1e-4)
    close(exp(-1.0), scalarAt(f("e^(−x² − y²)"), 1.0, 0.0).value)
  }
}
