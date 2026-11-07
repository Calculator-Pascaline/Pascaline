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

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Where a satellite is, from the center of the planet, in meters: x points to its lowest point.
 *
 * @property anomaly Eccentric anomaly in radians, tells which way it is going.
 */
internal data class OrbitPoint(val x: Double, val y: Double, val distance: Double, val anomaly: Double)

/**
 * An ellipse around a planet, in meters and seconds.
 *
 * @property radius Of the planet.
 * @property mu G · M of the planet.
 * @property lowest Height of the lowest point above the ground.
 * @property highest Height of the highest point above the ground.
 */
internal data class Orbit(val radius: Double, val mu: Double, val lowest: Double, val highest: Double) {
  val isValid: Boolean
    get() = lowest >= 0 && highest >= lowest && radius > 0 && mu > 0

  val periapsis: Double
    get() = radius + lowest

  val apoapsis: Double
    get() = radius + highest

  /** a = (r₁ + r₂) / 2 */
  val semiMajorAxis: Double
    get() = (periapsis + apoapsis) / 2

  /** e = (r₂ − r₁) / (r₂ + r₁), 0 for a circle. */
  val eccentricity: Double
    get() = (apoapsis - periapsis) / (apoapsis + periapsis)

  val semiMinorAxis: Double
    get() = semiMajorAxis * sqrt(1 - eccentricity * eccentricity)

  /** T = 2π √(a³ / μ) */
  val period: Double
    get() = 2 * PI * sqrt(semiMajorAxis * semiMajorAxis * semiMajorAxis / mu)

  /** v = √(μ (2 / r − 1 / a)) */
  fun speedAt(distance: Double) = sqrt(mu * (2 / distance - 1 / semiMajorAxis))

  val fastest: Double
    get() = speedAt(periapsis)

  val slowest: Double
    get() = speedAt(apoapsis)

  /** Speed that leaves the planet for good from the lowest point: √(2 μ / r). */
  val escapeSpeed: Double
    get() = sqrt(2 * mu / periapsis)

  /** Where it is after [part] of the period, from the lowest point. Time isn't the angle: M = E − e sin E. */
  fun at(part: Double): OrbitPoint {
    val mean = 2 * PI * part
    var anomaly = mean
    repeat(NEWTON_STEPS) {
      anomaly -= (anomaly - eccentricity * sin(anomaly) - mean) / (1 - eccentricity * cos(anomaly))
    }
    val x = semiMajorAxis * (cos(anomaly) - eccentricity)
    val y = semiMinorAxis * sin(anomaly)
    return OrbitPoint(x, y, semiMajorAxis * (1 - eccentricity * cos(anomaly)), anomaly)
  }
}

private const val NEWTON_STEPS = 12
