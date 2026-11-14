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

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GearMathTest {
  private fun assertClose(expected: Double, actual: Double, tolerance: Double = 1e-9) =
    assertTrue(abs(expected - actual) <= tolerance * maxOf(1.0, abs(expected)), "expected $expected, got $actual")

  // 20 teeth at 1500 rpm and 10 N·m, then 40 meshing, 15 on its shaft and 45 meshing with that one
  private val compound =
    GearTrain(
      teeth = 20.0,
      pitch = 2.0,
      system = PitchSystem.MODULE,
      speed = 1500.0,
      torque = 10.0,
      efficiency = 1.0,
      gears = listOf(GearSpec(40.0, false), GearSpec(15.0, true), GearSpec(45.0, false)),
    )

  @Test
  fun meshingGearsTurnTheOtherWayAtTheRatioOfTheirTeeth() {
    val gears = compound.solve().gears
    assertClose(1500.0, gears[0].speed)
    assertClose(-750.0, gears[1].speed)
    assertClose(-750.0, gears[2].speed)
    assertClose(250.0, gears[3].speed)
  }

  @Test
  fun torqueGrowsAsSpeedDrops() {
    val result = compound.solve()
    assertClose(10.0, result.gears[0].torque)
    assertClose(20.0, result.gears[1].torque)
    assertClose(20.0, result.gears[2].torque)
    assertClose(60.0, result.output.torque)
    assertClose(6.0, result.ratio)
    assertClose(2.0, result.gears[2].ratio)
  }

  @Test
  fun withoutLossesPowerIsTheSameEverywhere() {
    val result = compound.solve()
    assertClose(1500 * 2 * PI / 60 * 10, result.inputPower)
    result.gears.forEach { assertClose(result.inputPower, it.power) }
    assertClose(1.0, result.efficiency)
  }

  @Test
  fun eachMeshLosesItsShare() {
    val result = compound.copy(efficiency = 0.98).solve()
    assertClose(19.6, result.gears[1].torque)
    assertClose(19.6, result.gears[2].torque)
    assertClose(57.624, result.output.torque)
    // Two meshes, the shaft loses nothing
    assertClose(0.98 * 0.98, result.efficiency)
    assertClose(250.0, result.output.speed)
  }

  @Test
  fun diametersAndCenterDistancesComeFromTheModule() {
    val gears = compound.solve().gears
    assertEquals(listOf(40.0, 80.0, 30.0, 90.0), gears.map { it.diameter })
    assertNull(gears[0].centerDistance)
    assertClose(60.0, gears[1].centerDistance!!)
    assertNull(gears[2].centerDistance)
    assertClose(60.0, gears[3].centerDistance!!)
  }

  @Test
  fun diametralPitchGivesInches() {
    val train = compound.copy(pitch = 10.0, system = PitchSystem.DIAMETRAL)
    val gears = train.solve().gears
    assertClose(2.0, gears[0].diameter)
    assertClose(4.0, gears[1].diameter)
    assertClose(3.0, gears[1].centerDistance!!)
  }

  @Test
  fun withoutPitchThereAreNoSizesButStillSpeeds() {
    val gears = compound.copy(pitch = Double.NaN).solve().gears
    assertTrue(gears.all { it.diameter.isNaN() })
    assertTrue(gears[1].centerDistance!!.isNaN())
    assertClose(250.0, gears[3].speed)
  }

  @Test
  fun idlerChangesTheDirectionOnly() {
    val train = compound.copy(gears = listOf(GearSpec(33.0, false), GearSpec(20.0, false)))
    val output = train.solve().output
    assertClose(1500.0, output.speed)
    assertClose(10.0, output.torque)
  }

  @Test
  fun powerGivesTheTorque() {
    assertClose(10.0, torqueFromPower(power(10.0, 1500.0), 1500.0))
    assertClose(1000.0, power(torqueFromPower(1000.0, 3000.0), -3000.0))
  }

  @Test
  fun teethMustBeAWholeNumberOfAtLeastThree() {
    val train = compound.copy(gears = listOf(GearSpec(2.0, false), GearSpec(40.5, false)))
    val gears = train.solve().gears
    assertTrue(gears[1].speed.isNaN())
    assertTrue(gears[2].teeth.isNaN())
    assertTrue(compound.copy(teeth = 12.5).solve().ratio.isNaN())
  }

  @Test
  fun aTrainOfOneGearIsTheInput() {
    val result = compound.copy(gears = emptyList()).solve()
    assertClose(1.0, result.ratio)
    assertClose(10.0, result.output.torque)
  }

  @Test
  fun meshingGearsTouchAndShaftGearsShareTheirCenter() {
    val placements = layoutGears(compound.solve().gears, 0.0)
    assertEquals(4, placements.size)
    val (first, second, onShaft) = placements
    val last = placements.last()
    assertClose(30.0, hypot(second.x - first.x, second.y - first.y))
    assertEquals(second.x to second.y, onShaft.x to onShaft.y)
    assertClose(30.0, hypot(last.x - onShaft.x, last.y - onShaft.y))
  }

  @Test
  fun aMeshingGearTurnsBackAtTheRatioOfTheTeeth() {
    val gears = compound.solve().gears
    val still = layoutGears(gears, 0.0)
    val turned = layoutGears(gears, 0.1)
    assertClose(-0.1 * 20 / 40, turned[1].angle - still[1].angle)
    assertClose(turned[1].angle - still[1].angle, turned[2].angle - still[2].angle)
    // 20 → 40, then 15 → 45: a sixth of the turn, the same way as the first
    assertClose(0.1 / 6, turned[3].angle - still[3].angle)
  }

  @Test
  fun layoutStopsAtTheFirstGearWithoutTeeth() {
    val train = compound.copy(gears = listOf(GearSpec(40.0, false), GearSpec(1.0, false), GearSpec(20.0, false)))
    assertEquals(2, layoutGears(train.solve().gears, 0.0).size)
  }
}
