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
import kotlin.test.assertTrue

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
    assertTrue(flight(30.0, 44.0).range < best)
    assertTrue(flight(30.0, 46.0).range < best)
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

  private val tennis = Air(mass = 0.058, diameter = 0.067, density = 1.225)

  @Test
  fun airShortensTheFlight() {
    val launch = Launch(30.0, 45.0, 0.0, 9.81)
    val inAir = assertNotNull(launch.trajectory(tennis))
    val noAir = assertNotNull(launch.flight())
    assertTrue(inAir.range < noAir.range)
    assertTrue(inAir.apexHeight < noAir.apexHeight)
    assertTrue(inAir.impactSpeed < 30.0)
  }

  @Test
  fun thinAirIsCloseToNoAir() {
    val launch = Launch(10.0, 45.0, 0.0, 9.81)
    val thin = assertNotNull(launch.trajectory(tennis.copy(density = 1e-6)))
    assertEquals(assertNotNull(launch.flight()).range, thin.range, 0.01)
    assertEquals(assertNotNull(launch.flight()).time, thin.time, 0.01)
  }

  @Test
  fun noDensityIsThePlainFormula() {
    val flight = Launch(10.0, 45.0, 0.0, 9.81).trajectory(tennis.copy(density = 0.0))
    assertTrue(flight is Flight)
  }

  @Test
  fun fallReachesTerminalSpeed() {
    val fall = assertNotNull(Launch(0.0, 0.0, 2000.0, 9.81).trajectory(tennis))
    assertEquals(tennis.terminalSpeed(9.81), fall.impactSpeed, 0.1)
    assertEquals(90.0, fall.impactAngle, 0.5)
  }

  @Test
  fun airLowersTheBestAngle() {
    val (angle, range) = Launch(40.0, 45.0, 0.0, 9.81).bestAngle(tennis)
    assertTrue(angle < 45.0, "angle $angle")
    assertTrue(angle > 25.0, "angle $angle")
    assertTrue(range > assertNotNull(Launch(40.0, 45.0, 0.0, 9.81).trajectory(tennis)).range - 0.01)
  }

  @Test
  fun positionsFollowTheSteps() {
    val flight = assertNotNull(Launch(20.0, 30.0, 10.0, 9.81).trajectory(tennis))
    assertEquals(0.0, flight.x(0.0), 1e-9)
    assertEquals(10.0, flight.y(0.0), 1e-9)
    assertEquals(0.0, flight.y(flight.time), 0.01)
    assertEquals(flight.range, flight.x(flight.time), 1e-9)
  }

  @Test
  fun invalidAir() {
    assertNull(Launch(10.0, 45.0, 0.0, 9.81).trajectory(tennis.copy(mass = 0.0)))
    assertNull(Launch(10.0, 45.0, 0.0, 9.81).trajectory(tennis.copy(diameter = -1.0)))
  }
}
