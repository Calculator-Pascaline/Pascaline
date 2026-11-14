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
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * What a gear material takes at the root of a tooth, in MPa: typical allowable bending stresses for steady loads.
 * Real designs use the data of their own material.
 */
internal enum class Material(val allowable: Double) {
  HARDENED_STEEL(HARDENED_STEEL_MPA),
  STEEL(STEEL_MPA),
  CAST_IRON(CAST_IRON_MPA),
  BRONZE(BRONZE_MPA),
  ACETAL(ACETAL_MPA),
  NYLON(NYLON_MPA),
}

/** What the teeth of a gear and the one before it go through where they touch. */
internal data class MeshCheck(
  /** Teeth in contact on average: below 1.2 the gears run rough. */
  val contactRatio: Double,
  /** How fast the pitch circles go, in m/s. */
  val toothSpeed: Double,
  /** Bending stress at the root of the weaker tooth, in MPa. */
  val stress: Double,
  /** The material's allowable stress over [stress]. */
  val safety: Double,
)

/**
 * The checks of the mesh between gear [index] and the one before it, for teeth [faceWidth] mm wide in [material].
 * Null when the gear doesn't mesh with teeth (input, shaft, belt, worm, rack) or its numbers are missing.
 */
internal fun GearTrain.check(result: TrainResult, index: Int, faceWidth: Double, material: Material): MeshCheck? {
  val gear = result.gears.getOrNull(index) ?: return null
  val before = result.gears.getOrNull(index - 1) ?: return null
  if (gear.link != Link.MESH && gear.link != Link.INTERNAL) return null
  if (gear.teeth.isNaN() || before.teeth.isNaN()) return null
  val internal = gear.link == Link.INTERNAL
  val module = moduleOf(gear.pitch)
  val toothSpeed = PI * before.diameter * metersPer() * abs(before.speed) / SECONDS_PER_MINUTE
  // The same force pushes both teeth, the one with fewer teeth has the weaker root
  val force = 2 * before.torque / (before.diameter * metersPer())
  val weaker = if (internal) before.teeth else min(before.teeth, gear.teeth)
  val stress = speedFactor(toothSpeed) * force / (faceWidth * module * lewisFactor(weaker))
  return MeshCheck(
    contactRatio = contactRatio(before.teeth, gear.teeth, internal),
    toothSpeed = toothSpeed,
    stress = stress,
    safety = material.allowable / stress,
  )
}

/**
 * Teeth in contact on average for standard 20° teeth, whatever their size: the length of the line where they touch
 * over the distance between teeth on it. A ring has its teeth inside.
 */
internal fun contactRatio(driver: Double, driven: Double, internal: Boolean): Double {
  val angle = PRESSURE_ANGLE_DEGREES * PI / HALF_TURN_DEGREES
  val r1 = driver / 2
  val r2 = driven / 2
  // Base circles, from which the tooth profiles unroll
  val base1 = r1 * cos(angle)
  val base2 = r2 * cos(angle)
  val approach1 = sqrt((r1 + 1) * (r1 + 1) - base1 * base1)
  return if (internal) {
    val approach2 = sqrt((r2 - 1) * (r2 - 1) - base2 * base2)
    (approach1 - approach2 + (r2 - r1) * sin(angle)) / (PI * cos(angle))
  } else {
    val approach2 = sqrt((r2 + 1) * (r2 + 1) - base2 * base2)
    (approach1 + approach2 - (r1 + r2) * sin(angle)) / (PI * cos(angle))
  }
}

/**
 * Lewis form factor of a 20° full depth tooth with [teeth] teeth, read between the values of the usual table: how
 * strong the shape of the tooth is.
 */
internal fun lewisFactor(teeth: Double): Double {
  if (teeth <= LEWIS_TABLE.first().first) return LEWIS_TABLE.first().second
  val above = LEWIS_TABLE.indexOfFirst { it.first >= teeth }
  if (above < 0) return LEWIS_RACK
  val (teeth1, factor1) = LEWIS_TABLE[above - 1]
  val (teeth2, factor2) = LEWIS_TABLE[above]
  return factor1 + (factor2 - factor1) * (teeth - teeth1) / (teeth2 - teeth1)
}

/** Barth's factor for cut teeth: faster teeth hit harder. [speed] in m/s. */
internal fun speedFactor(speed: Double): Double = (BARTH_SPEED + speed) / BARTH_SPEED

// Lewis form factor Y for 20° full depth teeth (Shigley, table 14-2)
private val LEWIS_TABLE =
  listOf(
    12.0 to 0.245,
    13.0 to 0.261,
    14.0 to 0.277,
    15.0 to 0.290,
    16.0 to 0.296,
    17.0 to 0.303,
    18.0 to 0.309,
    19.0 to 0.314,
    20.0 to 0.322,
    21.0 to 0.328,
    22.0 to 0.331,
    24.0 to 0.337,
    26.0 to 0.346,
    28.0 to 0.353,
    30.0 to 0.359,
    34.0 to 0.371,
    38.0 to 0.384,
    43.0 to 0.397,
    50.0 to 0.409,
    60.0 to 0.422,
    75.0 to 0.435,
    100.0 to 0.447,
    150.0 to 0.460,
    300.0 to 0.472,
    400.0 to 0.480,
  )

/** Fewer teeth than this get cut away at their root when made with a 20° rack. */
internal const val UNDERCUT_TEETH = 17
internal const val ROUGH_CONTACT = 1.2
internal const val LOW_SAFETY = 1.5
private const val LEWIS_RACK = 0.485
private const val BARTH_SPEED = 6.1
private const val PRESSURE_ANGLE_DEGREES = 20.0
private const val HALF_TURN_DEGREES = 180.0
private const val HARDENED_STEEL_MPA = 380.0
private const val STEEL_MPA = 190.0
private const val CAST_IRON_MPA = 60.0
private const val BRONZE_MPA = 40.0
private const val ACETAL_MPA = 35.0
private const val NYLON_MPA = 30.0
