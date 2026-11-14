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
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.sin

/** How the size of a tooth is given: module in millimeters, or diametral pitch in teeth per inch. */
internal enum class PitchSystem {
  MODULE,
  DIAMETRAL,
}

/** A gear after the first: its teeth, and whether it turns on the shaft of the gear before it instead of meshing with it. */
internal data class GearSpec(val teeth: Double, val sameShaft: Boolean)

/**
 * A gear train. The first gear turns clockwise at [speed] rpm with [torque] N·m. [pitch] is the module in mm or the
 * diametral pitch in teeth per inch, NaN when not given: then there are no diameters. [efficiency] is kept at each mesh,
 * from 0 to 1.
 */
internal data class GearTrain(
  val teeth: Double,
  val pitch: Double,
  val system: PitchSystem,
  val speed: Double,
  val torque: Double,
  val efficiency: Double,
  val gears: List<GearSpec>,
)

/**
 * What a gear does. [speed] is in rpm, positive when it turns clockwise. [ratio] is the input speed over this speed.
 * [diameter] is the pitch diameter, in mm for a module and in inches for a diametral pitch. [centerDistance] is to the
 * gear it meshes with, null for the first gear and for one on the same shaft. [power] is in watts.
 */
internal data class GearState(
  val teeth: Double,
  val speed: Double,
  val torque: Double,
  val ratio: Double,
  val diameter: Double,
  val centerDistance: Double?,
  val power: Double,
  val sameShaft: Boolean,
)

/** The gears one after the other, with the whole train from the first to the last. */
internal data class TrainResult(val gears: List<GearState>) {
  val output: GearState
    get() = gears.last()

  val ratio: Double
    get() = output.ratio

  val inputPower: Double
    get() = gears.first().power

  val outputPower: Double
    get() = output.power

  /** Power out over power in. */
  val efficiency: Double
    get() = outputPower / inputPower
}

/** Speed and torque through every gear: a mesh turns the other way, a shaft keeps speed and torque. */
internal fun GearTrain.solve(): TrainResult {
  val first = teeth.validTeeth()
  val input =
    GearState(
      teeth = first,
      speed = speed,
      torque = torque,
      ratio = if (first.isNaN()) Double.NaN else 1.0,
      diameter = pitchDiameter(first),
      centerDistance = null,
      power = power(torque, speed),
      sameShaft = false,
    )
  val states = mutableListOf(input)
  gears.forEach { gear ->
    val before = states.last()
    val teeth = gear.teeth.validTeeth()
    val next =
      if (gear.sameShaft) {
        before.copy(teeth = teeth, diameter = pitchDiameter(teeth), centerDistance = null, sameShaft = true)
      } else {
        val step = before.teeth / teeth
        val nextSpeed = -before.speed * step
        val nextTorque = before.torque / step * efficiency
        GearState(
          teeth = teeth,
          speed = nextSpeed,
          torque = nextTorque,
          ratio = speed / abs(nextSpeed),
          diameter = pitchDiameter(teeth),
          centerDistance = (before.diameter + pitchDiameter(teeth)) / 2,
          power = power(nextTorque, nextSpeed),
          sameShaft = false,
        )
      }
    states += next
  }
  return TrainResult(states)
}

/** Pitch diameter: module × teeth in mm, or teeth / diametral pitch in inches. */
internal fun GearTrain.pitchDiameter(teeth: Double): Double =
  when (system) {
    PitchSystem.MODULE -> teeth * pitch
    PitchSystem.DIAMETRAL -> teeth / pitch
  }

/** Torque in N·m from a power in watts at a speed in rpm. */
internal fun torqueFromPower(power: Double, speed: Double): Double = power / angularSpeed(speed)

/** Power in watts of a torque in N·m at a speed in rpm. */
internal fun power(torque: Double, speed: Double): Double = torque * abs(angularSpeed(speed))

private fun angularSpeed(rpm: Double) = rpm * 2 * PI / SECONDS_PER_MINUTE

/** A whole number of teeth, at least [MIN_TEETH], or NaN. */
private fun Double.validTeeth() = if (this >= MIN_TEETH && this == floor(this)) this else Double.NaN

/**
 * Where a gear is drawn, for a module of 1: [x] and [y] of its center (y goes down), its pitch [radius] and the
 * [angle] of its first tooth in radians, clockwise from the right.
 */
internal data class Placement(val x: Double, val y: Double, val radius: Double, val teeth: Int, val angle: Double)

/**
 * The gears of [states] side by side, up and down in turn so that they take less room, with the first one turned by
 * [turn] radians. A gear that meshes has a gap where the tooth of the other one goes in, and turns the other way at
 * the speed its teeth give; one on the same shaft is drawn on top of the gear before it. Stops at the first gear
 * without a valid number of teeth.
 */
internal fun layoutGears(states: List<GearState>, turn: Double): List<Placement> {
  val placements = mutableListOf<Placement>()
  var meshes = 0
  for (state in states) {
    if (state.teeth.isNaN()) break
    val teeth = state.teeth.toInt()
    val radius = state.teeth / 2
    val before = placements.lastOrNull()
    placements +=
      when {
        before == null -> Placement(0.0, 0.0, radius, teeth, turn)
        state.sameShaft -> Placement(before.x, before.y, radius, teeth, before.angle)
        else -> {
          val distance = before.radius + radius
          val direction = clearestDirection(placements, before, radius, if (meshes % 2 == 0) -ZIGZAG else ZIGZAG)
          meshes++
          val angle = direction + PI - PI / teeth - (before.angle - direction) * before.teeth / teeth
          Placement(before.x + distance * cos(direction), before.y + distance * sin(direction), radius, teeth, angle)
        }
      }
  }
  return placements
}

/**
 * Which way from [before] a gear of [radius] goes: [preferred], unless it would cover a gear drawn earlier. Then the
 * direction to the right that keeps it the furthest from them.
 */
private fun clearestDirection(placements: List<Placement>, before: Placement, radius: Double, preferred: Double): Double {
  val others = placements.filter { it.x != before.x || it.y != before.y }
  fun clearance(direction: Double): Double {
    val distance = before.radius + radius
    val x = before.x + distance * cos(direction)
    val y = before.y + distance * sin(direction)
    return others.minOfOrNull { hypot(it.x - x, it.y - y) - it.radius - radius } ?: Double.MAX_VALUE
  }
  if (clearance(preferred) >= 0) return preferred
  return (-DIRECTIONS..DIRECTIONS).map { it * PI / 2 / DIRECTIONS }.maxBy { clearance(it) }
}

internal const val MIN_TEETH = 3
private const val DIRECTIONS = 6
private const val SECONDS_PER_MINUTE = 60
private const val ZIGZAG = PI / 9
