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

package com.sadellie.unitto.feature.calculator.integrals

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class MultipleIntegralTest {
  private fun f(text: String) = MultiFunction(text)

  private fun close(expected: Double, actual: Double, tolerance: Double = 1e-9) =
    assertTrue(abs(expected - actual) <= tolerance * maxOf(1.0, abs(expected)), "expected $expected, got $actual")

  @Test
  fun readsWhatIsTyped() {
    close(8.0, f("x·y + 2").valueAt(2.0, 3.0))
    close(11.0, f("2x² + 3").valueAt(2.0))
    close(-4.0, f("-x^2").valueAt(2.0))
    close(512.0, f("2^3^2").valueAt(0.0))
    close(0.6, f("√(1 − x² − y²)").valueAt(0.0, 0.8))
    close(sin(1.0) * 2, f("2sin(x)").valueAt(1.0))
    close(6.0, f("x y z").valueAt(1.0, 2.0, 3.0))
    close(2 * PI, f("2π").valueAt(0.0))
    close(1.5, f("3/2").valueAt(0.0))
    close(1.5, f("1,5").valueAt(0.0))
    close(2.0, f("(x + 1)(x − 1)").valueAt(3.0) / 4.0)
  }

  @Test
  fun badInputIsRefused() {
    listOf("", "x +", "(x", "w", "2 ) 3").forEach { text -> assertFailsWith<IllegalArgumentException>(text) { f(text) } }
  }

  @Test
  fun gaussRuleIsExactForPolynomials() {
    val rule = GaussRule(5)
    close(1.0, rule.weights.sum())
    close(1.0 / 9, rule.points.indices.sumOf { rule.weights[it] * rule.points[it].let { x -> x * x * x * x * x * x * x * x } })
  }

  @Test
  fun doubleIntegralUnderAParabola() {
    // ∫₀² ∫₀^(x²) (x·y + 2) dy dx = 64/12 + 16/3
    val result = IntegralSetup(f("x·y + 2"), 0.0, 2.0, Bounds(f("0"), f("x²")), null).integrate()
    close(64.0 / 12 + 16.0 / 3, result.value)
    close(8.0 / 3, result.size)
  }

  @Test
  fun tripleIntegralOverABox() {
    val result = IntegralSetup(f("x + y + z"), 0.0, 1.0, Bounds(f("0"), f("1")), Bounds(f("0"), f("1"))).integrate()
    close(1.5, result.value)
    close(1.0, result.size)
    close(1.5, result.average)
  }

  @Test
  fun ballAndCone() {
    val ball = IntegralSetup(f("1"), -1.0, 1.0, Bounds(f("-√(1 − x²)"), f("√(1 − x²)")), Bounds(f("-√(1 − x² − y²)"), f("√(1 − x² − y²)"))).integrate()
    close(4 * PI / 3, ball.value, 1e-3)
    val cone = IntegralSetup(f("1"), -1.0, 1.0, Bounds(f("-√(1 − x²)"), f("√(1 − x²)")), Bounds(f("√(x² + y²)"), f("1"))).integrate()
    close(PI / 3, cone.value, 1e-3)
  }

  @Test
  fun facesSkipWhereItIsNotDefined() {
    val setup = IntegralSetup(f("4 − x² − y²"), -1.0, 1.0, Bounds(f("-√(1 − x²)"), f("√(1 − x²)")), null)
    val faces = setup.faces(8)
    assertEquals(8 * 8 * 2 + 4 * 8, faces.size)
    assertTrue(faces.all { face -> face.corners.all { it.shade in 0.0..1.0 } })
  }
}
