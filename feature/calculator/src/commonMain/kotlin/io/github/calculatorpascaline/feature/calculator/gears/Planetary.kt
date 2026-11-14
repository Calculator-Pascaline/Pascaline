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
import kotlin.math.sin

/** The three parts of a planetary set that can be held, turned or give the output. */
internal enum class Member {
  SUN,
  CARRIER,
  RING,
}

/**
 * A planetary set: a [sun] in the middle, planets around it that ride on a carrier, inside a [ring] with its teeth
 * inside. [fixed] is held still and [input] turns at [speed] rpm with [torque] N·m; the third part is the output.
 */
internal data class Planetary(
  val sun: Double,
  val ring: Double,
  val planets: Double,
  val fixed: Member,
  val input: Member,
  val speed: Double,
  val torque: Double,
  val efficiency: Double,
)

/** Why a planetary set can't be built as it is. */
internal enum class PlanetaryProblem {
  /** The ring needs more teeth than the sun, and a difference that splits into two whole planets. */
  PLANET_TEETH,

  /** Planets spaced evenly need sun + ring teeth that divide by their number. */
  SPACING,

  /** Neighboring planets would hit each other. */
  CROWDED,
}

/**
 * How a planetary set turns. [speeds] of each part in rpm, positive the way the input turns. [planetSpin] is how fast a
 * planet turns on its own pin, seen from the carrier. [ratio] is the input speed over the output speed, negative when
 * the output turns the other way.
 */
internal data class PlanetaryResult(
  val output: Member,
  val planetTeeth: Double,
  val speeds: Map<Member, Double>,
  val planetSpin: Double,
  val ratio: Double,
  val outputTorque: Double,
  val inputPower: Double,
  val outputPower: Double,
  val problems: List<PlanetaryProblem>,
)

/**
 * Willis's formula: seen from the carrier, the set is a plain train from the sun to the ring through the planets, so
 * (n_sun − n_carrier) · z_sun = −(n_ring − n_carrier) · z_ring, or z_sun · n_sun + z_ring · n_ring = (z_sun + z_ring) ·
 * n_carrier. With one part held and one turned, the third follows.
 */
internal fun Planetary.solve(): PlanetaryResult {
  val output = Member.entries.first { it != fixed && it != input }
  val planetTeeth = (ring - sun) / 2
  val weights =
    mapOf(Member.SUN to sun, Member.RING to ring, Member.CARRIER to -(sun + ring))
  val outputSpeed = if (fixed == input) Double.NaN else -weights.getValue(input) * speed / weights.getValue(output)
  val speeds = mapOf(fixed to 0.0, input to speed, output to outputSpeed)
  val carrier = speeds.getValue(Member.CARRIER)
  val ratio = speed / outputSpeed
  val outputTorque = torque * abs(ratio) * efficiency
  return PlanetaryResult(
    output = output,
    planetTeeth = planetTeeth,
    speeds = speeds,
    planetSpin = -(speeds.getValue(Member.SUN) - carrier) * sun / planetTeeth,
    ratio = ratio,
    outputTorque = outputTorque,
    inputPower = power(torque, speed),
    outputPower = power(outputTorque, outputSpeed),
    problems = problems(planetTeeth),
  )
}

private fun Planetary.problems(planetTeeth: Double): List<PlanetaryProblem> {
  val count = planets.takeIf { it >= 1 && it == floor(it) } ?: return listOf(PlanetaryProblem.SPACING)
  if (planetTeeth < MIN_TEETH || planetTeeth != floor(planetTeeth)) return listOf(PlanetaryProblem.PLANET_TEETH)
  val problems = mutableListOf<PlanetaryProblem>()
  if ((sun + ring) % count != 0.0) problems += PlanetaryProblem.SPACING
  // Centers of neighboring planets against the tips of two planets, for a module of 1
  val between = (sun + planetTeeth) * sin(PI / count)
  if (count > 1 && between <= planetTeeth + 2) problems += PlanetaryProblem.CROWDED
  return problems
}
