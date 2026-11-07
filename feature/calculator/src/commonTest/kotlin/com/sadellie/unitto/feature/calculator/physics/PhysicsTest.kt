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

package com.sadellie.unitto.feature.calculator.physics

import kotlin.math.abs
import kotlin.math.hypot
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PhysicsTest {
  @Test
  fun smallSwingsFollowTheSchoolFormula() {
    val pendulum = Pendulum(1.0, 1.0, 9.81, 0.0)
    assertEquals(2.006, pendulum.smallAnglePeriod, 0.001)
    assertEquals(pendulum.smallAnglePeriod, pendulum.period, 0.001)
  }

  @Test
  fun wideSwingsTakeLonger() {
    val pendulum = Pendulum(1.0, 90.0, 9.81, 0.0)
    assertEquals(1.18034, pendulum.period / pendulum.smallAnglePeriod, 0.0001)
  }

  @Test
  fun theSimulationComesBackAfterOnePeriod() {
    val pendulum = Pendulum(2.0, 120.0, 9.81, 0.0)
    val motion = PendulumMotion(pendulum)
    repeat((pendulum.period * 60).toInt()) { motion.advance(1.0 / 60) }
    motion.advance(pendulum.period - (pendulum.period * 60).toInt() / 60.0)
    assertEquals(pendulum.radians, motion.angle, 0.001)
  }

  @Test
  fun energyOnlyChangesForm() {
    val pendulum = Pendulum(1.5, 60.0, 9.81, 0.0)
    val motion = PendulumMotion(pendulum)
    repeat(200) {
      motion.advance(1.0 / 60)
      assertEquals(1.0, pendulum.potentialShare(motion.angle) + pendulum.kineticShare(motion.speed), 0.001)
    }
  }

  @Test
  fun dampingStopsIt() {
    val pendulum = Pendulum(1.0, 45.0, 9.81, 1.0)
    val motion = PendulumMotion(pendulum)
    repeat(3000) { motion.advance(1.0 / 60) }
    assertTrue(abs(motion.angle) < 0.01)
  }

  @Test
  fun nothingHangsWithoutARod() {
    assertFalse(Pendulum(0.0, 30.0, 9.81, 0.0).isValid)
    assertFalse(Pendulum(1.0, 175.0, 9.81, 0.0).isValid)
    assertFalse(Pendulum(Double.NaN, 30.0, 9.81, 0.0).isValid)
  }

  @Test
  fun waveValues() {
    val wave = Wave(1.0, 4.0, 0.5)
    assertEquals(2.0, wave.speed)
    assertEquals(2.0, wave.period)
    assertEquals(1.0, wave.right(1.0, 0.0), 1e-9)
    assertEquals(0.0, wave.right(1.0, 0.5), 1e-9)
  }

  @Test
  fun standingWaveNeverMovesAtItsNodes() {
    val wave = Wave(2.0, 3.0, 1.0)
    listOf(0.0, 0.1, 0.37, 2.0).forEach { t ->
      assertEquals(0.0, wave.height(WaveMode.STANDING, wave, 1.5, t), 1e-9)
    }
    assertEquals(4.0, wave.height(WaveMode.STANDING, wave, 0.75, 0.0), 1e-9)
  }

  @Test
  fun secondWaveKeepsTheSpeedOfTheString() {
    val wave = Wave(1.0, 3.0, 1.0)
    val other = wave.atFrequency(1.25)
    assertEquals(wave.speed, other.speed, 1e-9)
    assertEquals(0.25, wave.beats(other), 1e-9)
  }

  @Test
  fun spaceStationGoesAroundInAnHourAndAHalf() {
    val orbit = Orbit(6_371_000.0, 3.986004e14, 400_000.0, 400_000.0)
    assertEquals(92.4, orbit.period / 60, 0.1)
    assertEquals(7670.0, orbit.fastest, 10.0)
    assertEquals(orbit.fastest, orbit.slowest, 1e-6)
  }

  @Test
  fun geostationaryOrbitTakesOneDay() {
    val orbit = Orbit(6_371_000.0, 3.986004e14, 35_786_000.0, 35_786_000.0)
    assertEquals(86_164.0, orbit.period, 100.0)
  }

  @Test
  fun escapeSpeedFromTheGround() {
    val orbit = Orbit(6_371_000.0, 3.986004e14, 0.0, 0.0)
    assertEquals(11_186.0, orbit.escapeSpeed, 5.0)
  }

  @Test
  fun ellipseGoesFromLowestToHighest() {
    val orbit = Orbit(6_371_000.0, 3.986004e14, 500_000.0, 39_800_000.0)
    assertEquals(orbit.periapsis, orbit.at(0.0).x, 1.0)
    assertEquals(-orbit.apoapsis, orbit.at(0.5).x, 1.0)
    assertEquals(orbit.apoapsis, orbit.at(0.5).distance, 1.0)
  }

  @Test
  fun fasterLowGoesFurtherInTheSameTime() {
    val orbit = Orbit(6_371_000.0, 3.986004e14, 500_000.0, 39_800_000.0)
    // Conservation of angular momentum
    assertEquals(orbit.periapsis * orbit.fastest, orbit.apoapsis * orbit.slowest, 1e6)
    fun travelled(from: Double): Double {
      val start = orbit.at(from)
      val end = orbit.at(from + 0.05)
      return hypot(end.x - start.x, end.y - start.y)
    }
    assertTrue(travelled(0.0) > 2 * travelled(0.5))
  }
}
