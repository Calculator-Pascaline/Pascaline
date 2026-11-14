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
import kotlin.math.floor

/** How the size of a tooth is given: module in millimeters, or diametral pitch in teeth per inch. */
internal enum class PitchSystem {
  MODULE,
  DIAMETRAL,
}

/** How a gear is turned by the one before it. */
internal enum class Link {
  /** Teeth on teeth from the outside: it turns the other way. */
  MESH,

  /** On the shaft of the gear before it: same speed and torque. It can start a stage with bigger or smaller teeth. */
  SHAFT,

  /** A ring with its teeth inside, around the gear before it: it turns the same way. */
  INTERNAL,

  /** A belt or a chain from the gear before it, at any distance: it turns the same way. */
  BELT,

  /** A wheel turned by a worm on the shaft of the gear before it, at a right angle. */
  WORM,

  /** A toothed bar that the gear before it pushes in a straight line. Nothing comes after it. */
  RACK,
}

/**
 * A gear after the first. [teeth] of the gear, unused for a rack. [starts] is the number of threads of a worm and
 * [efficiency] the worm's own, from 0 to 1. [pitch] is the tooth size of a stage that starts on a shared shaft, NaN to
 * keep the one before.
 */
internal data class GearSpec(
  val teeth: Double,
  val link: Link,
  val starts: Double = 1.0,
  val efficiency: Double = Double.NaN,
  val pitch: Double = Double.NaN,
)

/**
 * A gear train. The first gear turns clockwise at [speed] rpm with [torque] N·m. [pitch] is the module in mm or the
 * diametral pitch in teeth per inch, NaN when not given: then there are no diameters. [efficiency] is kept at each
 * mesh, belt and rack, from 0 to 1.
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
 * [diameter] is the pitch diameter, in mm for a module and in inches for a diametral pitch, and [pitch] the size of
 * its teeth. [centerDistance] is to the gear before it, null when it has none. [power] is in watts. [level] counts the
 * shafts shared before it: gears on a higher level are in front. [rightAngle] when it turns on an axis at a right
 * angle to the first one, after a worm.
 *
 * A rack has no turning speed: [speed] is then how fast it moves, in m/s, and [torque] the force that pushes it, in N.
 */
internal data class GearState(
  val teeth: Double,
  val link: Link?,
  val speed: Double,
  val torque: Double,
  val ratio: Double,
  val diameter: Double,
  val pitch: Double,
  val centerDistance: Double?,
  val power: Double,
  val level: Int,
  val rightAngle: Boolean,
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

  /** Whether the train ends on a rack that moves in a straight line. */
  val endsOnRack: Boolean
    get() = output.link == Link.RACK
}

/** Speed and torque through every gear: a mesh turns the other way, a shaft keeps speed and torque. */
internal fun GearTrain.solve(): TrainResult {
  val first = teeth.validTeeth()
  val input =
    GearState(
      teeth = first,
      link = null,
      speed = speed,
      torque = torque,
      ratio = if (first.isNaN()) Double.NaN else 1.0,
      diameter = pitchDiameter(first, pitch),
      pitch = pitch,
      centerDistance = null,
      power = power(torque, speed),
      level = 0,
      rightAngle = false,
    )
  val states = mutableListOf(input)
  gears.forEach { gear ->
    val before = states.last()
    states += if (before.link == Link.RACK) INVALID.copy(level = before.level) else next(before, gear)
  }
  return TrainResult(states)
}

/** The gear after [before]. */
private fun GearTrain.next(before: GearState, gear: GearSpec): GearState {
  val teeth = gear.teeth.validTeeth()
  return when (gear.link) {
    Link.SHAFT -> {
      val pitch = gear.pitch.takeIf { it.isFinite() && it > 0 } ?: before.pitch
      before.copy(
        teeth = teeth,
        link = Link.SHAFT,
        diameter = pitchDiameter(teeth, pitch),
        pitch = pitch,
        centerDistance = null,
        level = before.level + 1,
      )
    }
    Link.MESH -> turned(before, teeth, Link.MESH, before.teeth / teeth, -1.0, efficiency)
    Link.INTERNAL -> {
      // A ring goes around the gear before it, so it needs more teeth
      val ring = teeth.takeIf { it > before.teeth } ?: Double.NaN
      turned(before, ring, Link.INTERNAL, before.teeth / ring, 1.0, efficiency)
    }
    Link.BELT -> turned(before, teeth, Link.BELT, before.teeth / teeth, 1.0, efficiency)
    Link.WORM -> {
      val starts = gear.starts.takeIf { it >= 1 && it == floor(it) } ?: Double.NaN
      val wormEfficiency = gear.efficiency.takeIf { it in 0.0..1.0 } ?: efficiency
      turned(before, teeth, Link.WORM, starts / teeth, 1.0, wormEfficiency).copy(rightAngle = true)
    }
    Link.RACK -> rack(before)
  }
}

/**
 * A gear turned by [before] with [step] times its speed, the same way when [direction] is 1 and the other way when it
 * is −1, keeping [efficiency] of the power.
 */
private fun GearTrain.turned(
  before: GearState,
  teeth: Double,
  link: Link,
  step: Double,
  direction: Double,
  efficiency: Double,
): GearState {
  val speed = before.speed * step * direction
  val torque = before.torque / step * efficiency
  val diameter = pitchDiameter(teeth, before.pitch)
  return GearState(
    teeth = teeth,
    link = link,
    speed = speed,
    torque = torque,
    ratio = this.speed / abs(speed),
    diameter = diameter,
    pitch = before.pitch,
    centerDistance =
      when (link) {
        Link.MESH -> (before.diameter + diameter) / 2
        Link.INTERNAL -> (diameter - before.diameter) / 2
        else -> null
      },
    power = power(torque, speed),
    level = before.level,
    rightAngle = before.rightAngle,
  )
}

/** A rack pushed by [before]: its speed in m/s is the speed of the pitch circle, its force the torque over the radius. */
private fun GearTrain.rack(before: GearState): GearState {
  val meters = before.diameter * metersPer()
  val speed = PI * meters * before.speed / SECONDS_PER_MINUTE
  val force = 2 * before.torque / meters * efficiency
  return GearState(
    teeth = Double.NaN,
    link = Link.RACK,
    speed = speed,
    torque = force,
    ratio = Double.NaN,
    diameter = Double.NaN,
    pitch = before.pitch,
    centerDistance = null,
    power = force * abs(speed),
    level = before.level,
    rightAngle = before.rightAngle,
  )
}

/** Meters in a unit of diameter: a millimeter or an inch. */
internal fun GearTrain.metersPer(): Double = if (system == PitchSystem.MODULE) METERS_PER_MM else METERS_PER_INCH

/** Pitch diameter: module × teeth in mm, or teeth / diametral pitch in inches. */
internal fun GearTrain.pitchDiameter(teeth: Double, pitch: Double = this.pitch): Double =
  when (system) {
    PitchSystem.MODULE -> teeth * pitch
    PitchSystem.DIAMETRAL -> teeth / pitch
  }

/** The module in mm of a tooth size given in this train's system. */
internal fun GearTrain.moduleOf(pitch: Double): Double =
  if (system == PitchSystem.MODULE) pitch else MM_PER_INCH / pitch

/** Torque in N·m from a power in watts at a speed in rpm. */
internal fun torqueFromPower(power: Double, speed: Double): Double = power / angularSpeed(speed)

/** Power in watts of a torque in N·m at a speed in rpm. */
internal fun power(torque: Double, speed: Double): Double = torque * abs(angularSpeed(speed))

private fun angularSpeed(rpm: Double) = rpm * 2 * PI / SECONDS_PER_MINUTE

/** A whole number of teeth, at least [MIN_TEETH], or NaN. */
internal fun Double.validTeeth() = if (this >= MIN_TEETH && this == floor(this)) this else Double.NaN

/** A gear after a rack, which nothing can turn. */
private val INVALID =
  GearState(
    teeth = Double.NaN,
    link = null,
    speed = Double.NaN,
    torque = Double.NaN,
    ratio = Double.NaN,
    diameter = Double.NaN,
    pitch = Double.NaN,
    centerDistance = null,
    power = Double.NaN,
    level = 0,
    rightAngle = false,
  )

internal const val MIN_TEETH = 3
internal const val SECONDS_PER_MINUTE = 60
internal const val MM_PER_INCH = 25.4
private const val METERS_PER_MM = 0.001
private const val METERS_PER_INCH = 0.0254
