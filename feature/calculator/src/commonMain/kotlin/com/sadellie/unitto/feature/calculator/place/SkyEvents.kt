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

// Angles of twilights, showers and eclipses, their numbers read best where they are used
@file:Suppress("MagicNumber")

package com.sadellie.unitto.feature.calculator.place

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.sqrt

/** What the Sun does on one day, null when it doesn't happen that day (polar day or night). */
internal data class SunDay(
  val sunrise: Long?,
  val sunset: Long?,
  val noon: Long,
  val noonAltitude: Double,
  /** Time with the Sun above the horizon, in milliseconds. */
  val daylight: Long,
  val civilDawn: Long?,
  val civilDusk: Long?,
  val nauticalDawn: Long?,
  val nauticalDusk: Long?,
  val astronomicalDawn: Long?,
  val astronomicalDusk: Long?,
  /** Evening golden hour, from 6° above the horizon to 4° below. */
  val goldenHourStart: Long?,
  val goldenHourEnd: Long?,
  /** Morning golden hour. */
  val morningGoldenStart: Long?,
  val morningGoldenEnd: Long?,
  /** Evening blue hour, from 4° to 6° below the horizon. */
  val blueHourEnd: Long?,
)

/** The day from [dayStart] to 24 hours later, the start of the day where the point is. */
internal fun sunDay(dayStart: Long, point: GeoPoint): SunDay {
  val end = dayStart + DAY
  val altitude = { t: Long -> sunAltitude(t, point) }
  fun first(threshold: Double, isRising: Boolean) =
    crossings(dayStart, end, STEP, threshold, altitude).firstOrNull { it.isRising == isRising }?.millis
  fun last(threshold: Double, isRising: Boolean) =
    crossings(dayStart, end, STEP, threshold, altitude).lastOrNull { it.isRising == isRising }?.millis

  val noon = maximum(dayStart, end, STEP, altitude)
  val horizon = crossings(dayStart, end, STEP, SUNRISE_ALTITUDE, altitude)
  var daylight = 0L
  var upSince: Long? = if (altitude(dayStart) > SUNRISE_ALTITUDE) dayStart else null
  for (crossing in horizon) {
    if (crossing.isRising) {
      upSince = crossing.millis
    } else if (upSince != null) {
      daylight += crossing.millis - upSince
      upSince = null
    }
  }
  if (upSince != null) daylight += end - upSince

  return SunDay(
    sunrise = first(SUNRISE_ALTITUDE, true),
    sunset = last(SUNRISE_ALTITUDE, false),
    noon = noon,
    noonAltitude = altitude(noon),
    daylight = daylight,
    civilDawn = first(CIVIL, true),
    civilDusk = last(CIVIL, false),
    nauticalDawn = first(NAUTICAL, true),
    nauticalDusk = last(NAUTICAL, false),
    astronomicalDawn = first(ASTRONOMICAL, true),
    astronomicalDusk = last(ASTRONOMICAL, false),
    goldenHourStart = last(GOLDEN_TOP, false),
    goldenHourEnd = last(GOLDEN_BOTTOM, false),
    morningGoldenStart = first(GOLDEN_BOTTOM, true),
    morningGoldenEnd = first(GOLDEN_TOP, true),
    blueHourEnd = last(CIVIL, false),
  )
}

/** The phase names, from new moon. */
internal enum class MoonPhase {
  NEW,
  WAXING_CRESCENT,
  FIRST_QUARTER,
  WAXING_GIBBOUS,
  FULL,
  WANING_GIBBOUS,
  LAST_QUARTER,
  WANING_CRESCENT,
}

internal data class MoonState(
  val phase: MoonPhase,
  /** From 0 to 1. */
  val illumination: Double,
  /** Moon minus Sun in ecliptic longitude, 0 is new, 180 full. */
  val elongation: Double,
  val distanceKm: Double,
)

internal fun moonState(millis: Long): MoonState {
  val sun = sunPosition(millis)
  val moon = moonPosition(millis)
  val elongation = normalize(moon.longitude - sun.longitude)
  val psi = separation(sun, moon)
  val phaseAngle = atan2D(sun.distanceKm * sinD(psi), moon.distanceKm - sun.distanceKm * cosD(psi))
  val phase = MoonPhase.entries[(normalize(elongation + 22.5) / 45).toInt() % 8]
  return MoonState(phase, (1 + cosD(phaseAngle)) / 2, elongation, moon.distanceKm)
}

internal data class MoonDay(val moonrise: Long?, val moonset: Long?)

internal fun moonDay(dayStart: Long, point: GeoPoint): MoonDay {
  val events =
    crossings(dayStart, dayStart + DAY, STEP, MOONRISE_ALTITUDE) { moonAltitude(it, point) }
  return MoonDay(events.firstOrNull { it.isRising }?.millis, events.firstOrNull { !it.isRising }?.millis)
}

/** Next time after [after] the Moon is [angle]° ahead of the Sun: 0 new, 90 first quarter, 180 full. */
internal fun nextPhase(after: Long, angle: Double): Long {
  val offset = { t: Long -> normalize(moonPosition(t).longitude - sunPosition(t).longitude - angle + 180) - 180 }
  var t0 = after
  var v0 = offset(t0)
  while (true) {
    val t1 = t0 + PHASE_STEP
    val v1 = offset(t1)
    // From behind to ahead, not the jump of ±180°
    if (v0 < 0 && v1 >= 0 && v1 - v0 < 90) return bisectAngle(t0, t1, offset)
    t0 = t1
    v0 = v1
  }
}

private fun bisectAngle(low: Long, high: Long, f: (Long) -> Double): Long {
  var a = low
  var b = high
  while (b - a > 5_000) {
    val m = (a + b) / 2
    if (f(m) < 0) a = m else b = m
  }
  return (a + b) / 2
}

/** A full moon closer than 360 000 km, the usual meaning of supermoon. */
internal fun isSupermoon(fullMoon: Long): Boolean = moonPosition(fullMoon).distanceKm < SUPERMOON_KM

internal enum class Season {
  MARCH_EQUINOX,
  JUNE_SOLSTICE,
  SEPTEMBER_EQUINOX,
  DECEMBER_SOLSTICE,
}

internal data class SeasonEvent(val season: Season, val millis: Long)

/** Next equinox or solstice after [after], when the Sun's longitude is a multiple of 90°. */
internal fun nextSeason(after: Long): SeasonEvent {
  val start = sunPosition(after).longitude
  val target = (floor90(start) + 90) % 360
  val millis = nextSunLongitude(after, target)
  return SeasonEvent(Season.entries[(target / 90).toInt()], millis)
}

private fun floor90(longitude: Double) = (longitude / 90).toInt() * 90.0

/** Next time the Sun reaches the ecliptic [longitude]. */
internal fun nextSunLongitude(after: Long, longitude: Double): Long {
  val offset = { t: Long -> normalize(sunPosition(t).longitude - longitude + 180) - 180 }
  var t0 = after
  var v0 = offset(t0)
  while (true) {
    val t1 = t0 + DAY
    val v1 = offset(t1)
    if (v0 < 0 && v1 >= 0) return bisectAngle(t0, t1, offset)
    t0 = t1
    v0 = v1
  }
}

/** Meteor showers of the IMO calendar: peak as the Sun's longitude, radiant declination, rate. */
internal enum class MeteorShower(val solarLongitude: Double, val radiantDeclination: Double, val hourlyRate: Int) {
  QUADRANTIDS(283.15, 49.0, 110),
  LYRIDS(32.32, 33.0, 18),
  ETA_AQUARIIDS(45.5, -1.0, 50),
  DELTA_AQUARIIDS(127.0, -16.0, 25),
  PERSEIDS(140.0, 58.0, 100),
  ORIONIDS(208.0, 16.0, 20),
  LEONIDS(235.27, 22.0, 15),
  GEMINIDS(262.2, 33.0, 150),
  URSIDS(270.7, 75.0, 10),
}

internal data class ShowerEvent(
  val shower: MeteorShower,
  val peak: Long,
  /** Highest the radiant gets, below 0 it is never seen from there. */
  val radiantMaxAltitude: Double,
  /** How much of the Moon is lit at the peak, a bright Moon hides faint meteors. */
  val moonIllumination: Double,
)

/** Showers peaking after [after], soonest first. */
internal fun nextShowers(after: Long, point: GeoPoint): List<ShowerEvent> =
  MeteorShower.entries
    .map { shower ->
      val peak = nextSunLongitude(after, shower.solarLongitude)
      ShowerEvent(shower, peak, 90 - abs(point.latitude - shower.radiantDeclination), moonState(peak).illumination)
    }
    .sortedBy { it.peak }

internal enum class EclipseKind {
  PENUMBRAL,
  PARTIAL,
  TOTAL,
  ANNULAR,
}

internal data class Eclipse(
  val isSolar: Boolean,
  val kind: EclipseKind,
  /** When it is deepest from the point. */
  val peak: Long,
  /** Of the Sun's area covered for a solar eclipse, of the Moon's width in shadow for a lunar one. */
  val magnitude: Double,
)

/**
 * Next lunar eclipse seen from [point], with the Moon above the horizon at its peak. Meeus chapter
 * 54 radii of the Earth's shadow.
 */
internal fun nextLunarEclipse(after: Long, point: GeoPoint, years: Int = SEARCH_YEARS): Eclipse? =
  phases(after, 180.0, years).firstNotNullOfOrNull { lunarEclipse(it, point) }

/** Full moons, or new ones for [angle] 0, from [after] for [years]. */
private fun phases(after: Long, angle: Double, years: Int): Sequence<Long> {
  val end = after + (years * YEAR).toLong()
  return generateSequence(nextPhase(after, angle)) { nextPhase(it + DAY, angle) }.takeWhile { it < end }
}

/** The lunar eclipse at the full moon [full] if there is one and the Moon is up at [point]. */
private fun lunarEclipse(full: Long, point: GeoPoint): Eclipse? {
  if (abs(moonPosition(full).latitude) > LUNAR_ECLIPSE_LATITUDE) return null
  val peak = minimum(full - 6 * HOUR, full + 6 * HOUR) { shadowDistance(it) }
  val moon = moonPosition(peak)
  val sun = sunPosition(peak)
  val moonRadius = moonRadius(moon)
  val moonParallax = asinD(EARTH_RADIUS_KM / moon.distanceKm)
  val sunParallax = 8.794 / 3600 * (KM_PER_AU_ROUND / sun.distanceKm)
  val sunRadius = sunRadius(sun)
  val umbra = 1.02 * (moonParallax - sunRadius + sunParallax)
  val penumbra = 1.02 * (moonParallax + sunRadius + sunParallax)
  val gap = shadowDistance(peak)
  val kind =
    when {
      gap + moonRadius <= umbra -> EclipseKind.TOTAL
      gap - moonRadius < umbra -> EclipseKind.PARTIAL
      gap - moonRadius < penumbra -> EclipseKind.PENUMBRAL
      else -> null
    }
  val isUp = moonAltitude(peak, point) > 0
  return if (kind != null && isUp) Eclipse(false, kind, peak, (umbra + moonRadius - gap) / (2 * moonRadius)) else null
}

/** Angle from the Moon to the center of the Earth's shadow. */
private fun shadowDistance(millis: Long): Double {
  val sun = sunPosition(millis)
  val antiSun = Ecliptic(normalize(sun.longitude + 180), 0.0, sun.distanceKm)
  return separation(moonPosition(millis), antiSun)
}

/** Next solar eclipse seen from [point] with the Sun up, from the Moon's place seen from there. */
internal fun nextSolarEclipse(after: Long, point: GeoPoint, years: Int = SEARCH_YEARS): Eclipse? =
  phases(after, 0.0, years).firstNotNullOfOrNull { solarEclipse(it, point) }

/** The solar eclipse around the new moon [new] seen from [point], if any. */
private fun solarEclipse(new: Long, point: GeoPoint): Eclipse? {
  // Too far from the Sun to touch it from anywhere on Earth
  if (separation(sunPosition(new), moonPosition(new)) > SOLAR_ECLIPSE_LIMIT) return null
  val covered = { m: Long -> if (sunAltitude(m, point) > SUNRISE_ALTITUDE) coveredFraction(m, point) else 0.0 }
  val peak = maximum(new - 5 * HOUR, new + 5 * HOUR, 5 * MILLIS_PER_MINUTE, covered)
  val fraction = covered(peak)
  if (fraction <= 0) return null
  val sunRadius = sunRadius(sunPosition(peak))
  val (gap, moonRadius) = topocentricGap(peak, point)
  val kind =
    when {
      moonRadius >= sunRadius && gap <= moonRadius - sunRadius -> EclipseKind.TOTAL
      moonRadius < sunRadius && gap <= sunRadius - moonRadius -> EclipseKind.ANNULAR
      else -> EclipseKind.PARTIAL
    }
  return Eclipse(true, kind, peak, fraction)
}

/** Angle between the Sun and the Moon seen from [point], and the Moon's radius from there. */
private fun topocentricGap(millis: Long, point: GeoPoint): Pair<Double, Double> {
  val sun = sunPosition(millis)
  val moon = moonPosition(millis)
  val sunSky = horizontal(sun, millis, point)
  val moonSky = horizontal(moon, millis, point, moon.distanceKm)
  val gap =
    acosD(
      (sinD(sunSky.altitude) * sinD(moonSky.altitude) +
          cosD(sunSky.altitude) * cosD(moonSky.altitude) * cosD(sunSky.azimuth - moonSky.azimuth))
        .coerceIn(-1.0, 1.0)
    )
  // Seen from the surface the Moon is a bit closer than from the center
  val moonRadius = asinD(MOON_RADIUS_KM_ROUND / (moon.distanceKm - EARTH_RADIUS_KM * sinD(moonSky.altitude)))
  return gap to moonRadius
}

/** Part of the Sun's disc the Moon covers, from the area of two overlapping circles. */
internal fun coveredFraction(millis: Long, point: GeoPoint): Double {
  val sunRadius = sunRadius(sunPosition(millis))
  val (gap, moonRadius) = topocentricGap(millis, point)
  return overlapFraction(sunRadius, moonRadius, gap)
}

internal fun overlapFraction(sunRadius: Double, moonRadius: Double, gap: Double): Double {
  val r = sunRadius
  val m = moonRadius
  return when {
    gap >= r + m -> 0.0
    gap <= abs(m - r) -> if (m >= r) 1.0 else m * m / (r * r)
    else -> {
      val a = r * r * acos(((gap * gap + r * r - m * m) / (2 * gap * r)).coerceIn(-1.0, 1.0))
      val b = m * m * acos(((gap * gap + m * m - r * r) / (2 * gap * m)).coerceIn(-1.0, 1.0))
      val c = 0.5 * sqrt(((-gap + r + m) * (gap + r - m) * (gap - r + m) * (gap + r + m)).coerceAtLeast(0.0))
      (a + b - c) / (PI * r * r)
    }
  }
}

private fun minimum(start: Long, end: Long, f: (Long) -> Double): Long = maximum(start, end, 10 * MILLIS_PER_MINUTE) { -f(it) }

private const val DAY = 86_400_000L
private const val HOUR = MILLIS_PER_HOUR
private const val YEAR = 365.25 * DAY
private const val STEP = 10 * MILLIS_PER_MINUTE
private const val PHASE_STEP = 6 * HOUR
private const val SEARCH_YEARS = 25
internal const val SUNRISE_ALTITUDE = -0.833
private const val MOONRISE_ALTITUDE = -0.833
private const val CIVIL = -6.0
private const val NAUTICAL = -12.0
private const val ASTRONOMICAL = -18.0
private const val GOLDEN_TOP = 6.0
private const val GOLDEN_BOTTOM = -4.0
private const val SUPERMOON_KM = 360_000.0
private const val LUNAR_ECLIPSE_LATITUDE = 1.6
private const val SOLAR_ECLIPSE_LIMIT = 1.6
private const val KM_PER_AU_ROUND = 149_597_870.7
private const val MOON_RADIUS_KM_ROUND = 1737.4
