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

package com.sadellie.unitto.feature.calculator.place

import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sin

/**
 * Where a solar panel looks: [tilt] is its angle from flat in degrees, [energy] what it gets under a
 * clear sky in kWh/m².
 */
internal data class PanelAim(val facesSouth: Boolean, val tilt: Double, val energy: Double)

/** The Sun seen from the ground: [power] in kW/m², then the direction of the Sun. */
private class SunSample(val power: Double, val east: Double, val north: Double, val up: Double)

/**
 * Best fixed panel for the day starting at [dayStart], null when the Sun stays down. Uses the clear
 * sky radiation model of Trail Sense (MIT, Kyle Corry), which only counts sunlight hitting the panel.
 */
internal fun dayPanelAim(point: GeoPoint, dayStart: Long, dayOfYear: Int): PanelAim? {
  val samples = List(DAY_SAMPLES) { sunSample(point, dayStart + it * SAMPLE_MILLIS, dayOfYear) }
  return bestAim(samples, HOURS_PER_SAMPLE)
}

/** Best fixed panel for the year starting at [yearStart], from every fifth day of it. */
internal fun yearPanelAim(point: GeoPoint, yearStart: Long): PanelAim? {
  val samples =
    (0 until DAYS_IN_YEAR step YEAR_DAY_STEP).flatMap { day ->
      List(DAY_SAMPLES) { sunSample(point, yearStart + day * DAY_MILLIS + it * SAMPLE_MILLIS, day + 1) }
    }
  return bestAim(samples, HOURS_PER_SAMPLE * YEAR_DAY_STEP)
}

private fun sunSample(point: GeoPoint, millis: Long, dayOfYear: Int): SunSample {
  val sun = horizontal(sunPosition(millis), millis, point)
  val altitude = sun.altitude.toRadians()
  val azimuth = sun.azimuth.toRadians()
  return SunSample(
    power = clearSkyPower(sun.altitude, dayOfYear),
    east = cos(altitude) * sin(azimuth),
    north = cos(altitude) * cos(azimuth),
    up = sin(altitude),
  )
}

/** kW/m² on a surface facing the Sun, with the air the light goes through. */
internal fun clearSkyPower(altitude: Double, dayOfYear: Int): Double {
  if (altitude <= 0) return 0.0
  val airMass = 1 / sin(altitude.toRadians())
  val sunDistance = 1 + ORBIT_EFFECT * cos((FULL_TURN_DEGREES * (dayOfYear - PERIHELION_DAY) / YEAR_LENGTH).toRadians())
  return sunDistance * SOLAR_CONSTANT * AIR_TRANSMISSION.pow(airMass.pow(AIR_MASS_POWER))
}

/**
 * The energy of all samples comes from one direction, which is close to the best aim, so only the
 * tilt is then improved, as sunlight from behind the panel is lost.
 */
private fun bestAim(samples: List<SunSample>, hoursPerSample: Double): PanelAim? {
  val lit = samples.filter { it.power > 0 }
  if (lit.isEmpty()) return null
  val east = lit.sumOf { it.power * it.east }
  val north = lit.sumOf { it.power * it.north }
  val up = lit.sumOf { it.power * it.up }
  val azimuth = atan2(east, north)
  fun energy(tilt: Double): Double {
    val flat = cos(tilt.toRadians())
    val eastward = sin(tilt.toRadians()) * sin(azimuth)
    val northward = sin(tilt.toRadians()) * cos(azimuth)
    return lit.sumOf { it.power * max(0.0, eastward * it.east + northward * it.north + flat * it.up) }
  }
  var tilt = (atan2(hypot(east, north), up) * DEGREES_PER_RADIAN).coerceIn(0.0, MAX_TILT)
  for (step in listOf(1.0, -1.0)) {
    while (tilt + step in 0.0..MAX_TILT && energy(tilt + step) > energy(tilt)) tilt += step
  }
  return PanelAim(facesSouth = cos(azimuth) < 0, tilt = tilt, energy = energy(tilt) * hoursPerSample)
}

private fun Double.toRadians() = this / DEGREES_PER_RADIAN

private const val DEGREES_PER_RADIAN = 180 / PI
private const val FULL_TURN_DEGREES = 360.0
private const val MAX_TILT = 90.0
private const val DAY_MILLIS = 86_400_000L
private const val DAY_SAMPLES = 48
private const val SAMPLE_MILLIS = DAY_MILLIS / DAY_SAMPLES
private const val HOURS_PER_SAMPLE = 0.5
private const val DAYS_IN_YEAR = 365
private const val YEAR_DAY_STEP = 5

// Radiation model
private const val ORBIT_EFFECT = 0.033
private const val PERIHELION_DAY = 2
private const val YEAR_LENGTH = 365.0
private const val SOLAR_CONSTANT = 1.353
private const val AIR_TRANSMISSION = 0.7
private const val AIR_MASS_POWER = 0.678
