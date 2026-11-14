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
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GearKindsTest {
  private fun assertClose(expected: Double, actual: Double, tolerance: Double = 1e-9) =
    assertTrue(abs(expected - actual) <= tolerance * maxOf(1.0, abs(expected)), "expected $expected, got $actual")

  private fun train(vararg gears: GearSpec) =
    GearTrain(
      teeth = 20.0,
      pitch = 2.0,
      system = PitchSystem.MODULE,
      speed = 1200.0,
      torque = 10.0,
      efficiency = 1.0,
      gears = gears.toList(),
    )

  @Test
  fun aRingTurnsTheSameWayAndSitsHalfTheDifferenceAway() {
    val ring = train(GearSpec(60.0, Link.INTERNAL)).solve().output
    assertClose(400.0, ring.speed)
    assertClose(30.0, ring.torque)
    assertClose((120.0 - 40.0) / 2, ring.centerDistance!!)
  }

  @Test
  fun aRingSmallerThanItsGearDoesNothing() {
    assertTrue(train(GearSpec(15.0, Link.INTERNAL)).solve().output.speed.isNaN())
  }

  @Test
  fun aBeltTurnsTheSameWayWithoutCenterDistance() {
    val pulley = train(GearSpec(50.0, Link.BELT)).solve().output
    assertClose(480.0, pulley.speed)
    assertNull(pulley.centerDistance)
  }

  @Test
  fun aWormTurnsItsWheelOneToothPerThread() {
    val result = train(GearSpec(40.0, Link.WORM, starts = 2.0, efficiency = 0.5)).solve()
    val wheel = result.output
    assertClose(60.0, wheel.speed)
    assertClose(20.0, result.ratio)
    // 20 times the torque, half of it lost in the worm
    assertClose(100.0, wheel.torque)
    assertTrue(wheel.rightAngle)
    assertClose(0.5, result.efficiency)
  }

  @Test
  fun aRackMovesAsFarAsThePitchCircleRolls() {
    val result = train(GearSpec(0.0, Link.RACK)).solve()
    val rack = result.output
    assertTrue(result.endsOnRack)
    // 40 mm across at 1200 rpm: π × 0.04 m × 20 turns a second
    assertClose(PI * 0.04 * 20, rack.speed)
    // 10 N·m over a radius of 0.02 m
    assertClose(500.0, rack.torque)
    assertClose(result.inputPower, result.outputPower)
  }

  @Test
  fun nothingTurnsAfterARack() {
    val gears = train(GearSpec(0.0, Link.RACK), GearSpec(30.0, Link.MESH)).solve().gears
    assertTrue(gears.last().speed.isNaN())
  }

  @Test
  fun aSharedShaftCanStartAStageWithBiggerTeeth() {
    val gears = train(GearSpec(40.0, Link.MESH), GearSpec(15.0, Link.SHAFT, pitch = 3.0), GearSpec(45.0, Link.MESH)).solve().gears
    assertClose(45.0, gears[2].diameter)
    assertClose(3.0, gears[3].pitch)
    assertClose((45.0 + 135.0) / 2, gears[3].centerDistance!!)
    assertEquals(listOf(0, 0, 1, 1), gears.map { it.level })
  }

  @Test
  fun aStageWithoutItsOwnSizeKeepsTheOneBefore() {
    val gears = train(GearSpec(40.0, Link.MESH), GearSpec(15.0, Link.SHAFT)).solve().gears
    assertClose(2.0, gears[2].pitch)
  }

  @Test
  fun twoTwentyToothGearsHaveTheTextbookContactRatio() {
    assertClose(1.557, contactRatio(20.0, 20.0, internal = false), 1e-3)
    // A ring holds its teeth around the gear, so more of them touch
    assertTrue(contactRatio(20.0, 60.0, internal = true) > contactRatio(20.0, 60.0, internal = false))
  }

  @Test
  fun lewisFactorComesFromTheTable() {
    assertClose(0.322, lewisFactor(20.0))
    assertClose((0.331 + 0.337) / 2, lewisFactor(23.0))
    assertClose(0.245, lewisFactor(8.0))
    assertClose(0.485, lewisFactor(1000.0))
  }

  @Test
  fun stressFollowsLewisWithBarth() {
    val gearTrain = train(GearSpec(40.0, Link.MESH))
    val result = gearTrain.solve()
    val check = assertNotNull(gearTrain.check(result, 1, 20.0, Material.STEEL))
    // 10 N·m on a 40 mm gear pushes 500 N; teeth go by at π × 0.04 m × 20 /s
    val speed = PI * 0.04 * 20
    assertClose(speed, check.toothSpeed)
    assertClose((6.1 + speed) / 6.1 * 500 / (20 * 2 * 0.322), check.stress)
    assertClose(190 / check.stress, check.safety)
  }

  @Test
  fun onlyMeshesAreChecked() {
    val gearTrain = train(GearSpec(40.0, Link.BELT))
    assertNull(gearTrain.check(gearTrain.solve(), 1, 20.0, Material.STEEL))
    assertNull(gearTrain.check(gearTrain.solve(), 0, 20.0, Material.STEEL))
  }

  @Test
  fun aWormIsDrawnOnItsShaftWithItsWheelBesideIt() {
    val gears = train(GearSpec(40.0, Link.MESH), GearSpec(30.0, Link.WORM), GearSpec(20.0, Link.MESH)).solve().gears
    val placements = layoutGears(gears, 0.0)
    val before = placements[1]
    val worm = placements[2]
    val wheel = placements[3]
    assertEquals(Shape.WORM, worm.shape)
    assertEquals(before.x to before.y, worm.x to worm.y)
    assertEquals(Shape.WHEEL, wheel.shape)
    assertClose(WORM_RADIUS + 15, hypot(wheel.x - worm.x, wheel.y - worm.y))
    // Nothing after the wheel: it turns at a right angle
    assertEquals(4, placements.size)
  }

  @Test
  fun aRingIsDrawnAroundItsGear() {
    val placements = layoutGears(train(GearSpec(60.0, Link.INTERNAL)).solve().gears, 0.0)
    val (gear, ring) = placements
    assertEquals(Shape.RING, ring.shape)
    assertClose(20.0, hypot(ring.x - gear.x, ring.y - gear.y))
  }

  @Test
  fun aRingTurnsWithItsGear() {
    val gears = train(GearSpec(60.0, Link.INTERNAL)).solve().gears
    val still = layoutGears(gears, 0.0)
    val turned = layoutGears(gears, 0.3)
    assertClose(0.3 * 20 / 60, turned[1].angle - still[1].angle)
  }

  @Test
  fun aBeltRunsFromThePulleyBefore() {
    val placements = layoutGears(train(GearSpec(50.0, Link.BELT)).solve().gears, 0.0)
    assertEquals(0, placements[1].beltFrom)
    assertTrue(hypot(placements[1].x, placements[1].y) > 10.0 + 25.0)
  }

  @Test
  fun aRackSlidesUnderItsGear() {
    val gears = train(GearSpec(0.0, Link.RACK)).solve().gears
    val still = layoutGears(gears, 0.0)[1]
    val turned = layoutGears(gears, 0.5)[1]
    assertEquals(Shape.RACK, still.shape)
    assertClose(10.0, still.y)
    // Turning clockwise, the bottom of the gear goes left by the arc it rolls
    assertClose(-0.5 * 10, turned.angle - still.angle)
  }

  @Test
  fun aSearchStaysWithinItsTeeth() {
    findTrains(12.5, 2, 12, 60, 8).flatMap { it.stages }.forEach { stage ->
      assertTrue(stage.driver in 12..60 && stage.driven in 12..60, "$stage")
    }
  }

  @Test
  fun aSearchFindsAnExactTrainFirst() {
    val best = findTrains(12.5, 2, 12, 60, 8).first()
    assertClose(12.5, best.ratio)
    assertEquals(2, best.stages.size)
  }

  @Test
  fun oneStageGivesTheClosestFraction() {
    val best = findTrains(3.14159, 1, 10, 80, 3).first()
    // 44/14, which is 22/7 with enough teeth
    assertTrue(best.error < 1e-3, "${best.stages}")
  }

  @Test
  fun threeStagesShareTheRatio() {
    val best = findTrains(100.0, 3, 10, 60, 3).first()
    assertEquals(3, best.stages.size)
    assertTrue(best.error < 1e-2)
    best.stages.forEach { assertTrue(it.ratio in 2.0..8.0, "$it") }
  }

  @Test
  fun aWrongSearchFindsNothing() {
    assertEquals(emptyList(), findTrains(0.0, 2, 12, 60, 8))
    assertEquals(emptyList(), findTrains(5.0, 4, 12, 60, 8))
    assertEquals(emptyList(), findTrains(5.0, 2, 60, 12, 8))
  }

  @Test
  fun inputsReadBackTheSame() {
    val inputs =
      GearsInputs(
        mode = GearsMode.PLANETARY,
        gears = listOf(GearEntry("30", Link.WORM, starts = "2", efficiency = "45"), GearEntry("12", Link.SHAFT, pitch = "3")),
        speedUnit = SpeedUnit.HERTZ,
        units = ForceUnits.IMPERIAL,
      )
    assertEquals(inputs, decodeInputs(inputs.encode()))
    assertEquals(GearsInputs(gears = emptyList()), decodeInputs(GearsInputs(gears = emptyList()).encode()))
    assertNull(decodeInputs("something else"))
  }

  @Test
  fun everyExampleReadsBack() {
    Example.entries.forEach { assertEquals(it.inputs, decodeInputs(it.inputs.encode())) }
  }

  @Test
  fun aWormWithFewThreadsIsSlowerThanItsWheelHasTeeth() {
    val wheel = train(GearSpec(40.0, Link.WORM, starts = 0.5)).solve().output
    assertFalse(wheel.speed.isFinite())
  }
}
