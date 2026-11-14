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

package io.github.calculatorpascaline.feature.calculator.gears

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PlanetaryTest {
  private fun assertClose(expected: Double, actual: Double, tolerance: Double = 1e-9) =
    assertTrue(abs(expected - actual) <= tolerance * maxOf(1.0, abs(expected)), "expected $expected, got $actual")

  // A screwdriver: a 12 tooth sun in a 48 tooth ring, three planets of 18
  private fun set(fixed: Member, input: Member) =
    Planetary(sun = 12.0, ring = 48.0, planets = 3.0, fixed = fixed, input = input, speed = 1200.0, torque = 0.5, efficiency = 1.0)

  @Test
  fun ringHeldSunInCarrierOut() {
    val result = set(Member.RING, Member.SUN).solve()
    assertEquals(Member.CARRIER, result.output)
    assertClose(5.0, result.ratio)
    assertClose(240.0, result.speeds.getValue(Member.CARRIER))
    assertClose(2.5, result.outputTorque)
    assertClose(18.0, result.planetTeeth)
    assertEquals(emptyList(), result.problems)
  }

  @Test
  fun carrierHeldTheRingTurnsBackwards() {
    val result = set(Member.CARRIER, Member.SUN).solve()
    assertEquals(Member.RING, result.output)
    assertClose(-4.0, result.ratio)
    assertClose(-300.0, result.speeds.getValue(Member.RING))
  }

  @Test
  fun sunHeldRingInCarrierOut() {
    val result = set(Member.SUN, Member.RING).solve()
    assertClose(1 + 12.0 / 48, result.ratio)
  }

  @Test
  fun carrierInGoesFaster() {
    val result = set(Member.RING, Member.CARRIER).solve()
    assertClose(1 / 5.0, result.ratio)
    assertClose(6000.0, result.speeds.getValue(Member.SUN))
  }

  @Test
  fun willisHoldsWhateverIsHeld() {
    Member.entries.forEach { fixed ->
      Member.entries.filter { it != fixed }.forEach { input ->
        val speeds = set(fixed, input).solve().speeds
        val sun = speeds.getValue(Member.SUN)
        val ring = speeds.getValue(Member.RING)
        val carrier = speeds.getValue(Member.CARRIER)
        assertClose(0.0, 12 * sun + 48 * ring - 60 * carrier, 1e-6)
      }
    }
  }

  @Test
  fun planetsSpinAgainstTheSunSeenFromTheCarrier() {
    val result = set(Member.RING, Member.SUN).solve()
    // The sun goes 960 rpm faster than the carrier, the planets turn back 12/18 of that
    assertClose(-640.0, result.planetSpin)
  }

  @Test
  fun planetsMustBeWholeAndFitAround() {
    assertEquals(listOf(PlanetaryProblem.PLANET_TEETH), set(Member.RING, Member.SUN).copy(ring = 47.0).solve().problems)
    assertEquals(listOf(PlanetaryProblem.SPACING), set(Member.RING, Member.SUN).copy(ring = 50.0).solve().problems)
    assertTrue(PlanetaryProblem.CROWDED in set(Member.RING, Member.SUN).copy(planets = 6.0).solve().problems)
  }
}
