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

package com.sadellie.unitto.feature.calculator.projectile

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class ProjectileMathTest {
  private fun flight(speed: Double, angle: Double, height: Double = 0.0, gravity: Double = 9.81) =
    assertNotNull(Launch(speed, angle, height, gravity).flight())

  @Test
  fun fromTheGround_landsAsFastAsItLeft() {
    val flight = flight(20.0, 45.0)
    assertEquals(40.775, flight.range, 0.001)
    assertEquals(2.883, flight.time, 0.001)
    assertEquals(10.194, flight.apexHeight, 0.001)
    assertEquals(20.0, flight.impactSpeed, 1e-9)
    assertEquals(45.0, flight.impactAngle, 1e-9)
  }

  @Test
  fun fortyFiveDegreesGoesFurthestFromTheGround() {
    val best = flight(30.0, 45.0).range
    assertEquals(best, 30.0 * 30.0 / 9.81, 1e-9)
    assert(flight(30.0, 44.0).range < best)
    assert(flight(30.0, 46.0).range < best)
  }

  @Test
  fun droppedFromAHeight() {
    val flight = flight(0.0, 0.0, height = 20.0, gravity = 10.0)
    assertEquals(2.0, flight.time, 1e-9)
    assertEquals(0.0, flight.range, 1e-9)
    assertEquals(20.0, flight.impactSpeed, 1e-9)
    assertEquals(20.0, flight.apexHeight, 1e-9)
  }

  @Test
  fun thrownDownwards_topIsTheStart() {
    val flight = flight(10.0, -30.0, height = 15.0)
    assertEquals(0.0, flight.apexTime)
    assertEquals(15.0, flight.apexHeight, 1e-9)
    assertEquals(0.0, flight.y(flight.time), 1e-9)
  }

  @Test
  fun moonGravityFliesLonger() {
    assertEquals(flight(20.0, 45.0).range * 9.81 / 1.62, flight(20.0, 45.0, gravity = 1.62).range, 1e-6)
  }

  @Test
  fun invalidLaunches() {
    assertNull(Launch(10.0, 45.0, 0.0, 0.0).flight())
    assertNull(Launch(-1.0, 45.0, 0.0, 9.81).flight())
    assertNull(Launch(10.0, 100.0, 0.0, 9.81).flight())
    assertNull(Launch(10.0, 45.0, -1.0, 9.81).flight())
  }
}
