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

// Published algorithms (Meeus, Astronomical Algorithms), their numbers read best where they are used
@file:Suppress("MagicNumber")

package com.sadellie.unitto.feature.calculator.place

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.math.sqrt

// Times are milliseconds since 1970 in UTC, angles are degrees.

/** A point on Earth, latitude north and longitude east. */
internal data class GeoPoint(val latitude: Double, val longitude: Double)

/** Ecliptic longitude and latitude of date, and the distance in km. */
internal data class Ecliptic(val longitude: Double, val latitude: Double, val distanceKm: Double)

internal data class Equatorial(val rightAscension: Double, val declination: Double)

internal data class Horizontal(val altitude: Double, val azimuth: Double)

internal fun julianDay(millis: Long): Double = millis / MILLIS_PER_DAY + UNIX_EPOCH_JD

internal fun millisOfJulianDay(jd: Double): Long = ((jd - UNIX_EPOCH_JD) * MILLIS_PER_DAY).toLong()

/** TT − UT in seconds, NASA's polynomial for 2005 to 2050, fine for a few decades around. */
private fun deltaTSeconds(jd: Double): Double {
  val year = 2000 + (jd - J2000) / 365.25
  val t = year - 2000
  return 62.92 + 0.32217 * t + 0.005589 * t * t
}

/** Julian centuries from J2000 in Terrestrial Time. */
private fun centuries(millis: Long): Double {
  val jd = julianDay(millis)
  return (jd + deltaTSeconds(jd) / SECONDS_PER_DAY - J2000) / 36525
}

/** Sun, low accuracy (0.01°), Meeus chapter 25. */
internal fun sunPosition(millis: Long): Ecliptic {
  val t = centuries(millis)
  val l0 = 280.46646 + 36000.76983 * t + 0.0003032 * t * t
  val m = 357.52911 + 35999.05029 * t - 0.0001537 * t * t
  val e = 0.016708634 - 0.000042037 * t - 0.0000001267 * t * t
  val c =
    (1.914602 - 0.004817 * t - 0.000014 * t * t) * sinD(m) + (0.019993 - 0.000101 * t) * sinD(2 * m) +
      0.000289 * sinD(3 * m)
  val trueLongitude = l0 + c
  val anomaly = m + c
  val distanceAu = 1.000001018 * (1 - e * e) / (1 + e * cosD(anomaly))
  val omega = 125.04 - 1934.136 * t
  val apparent = trueLongitude - 0.00569 - 0.00478 * sinD(omega)
  return Ecliptic(normalize(apparent), 0.0, distanceAu * KM_PER_AU)
}

/** Moon, the main terms of Meeus chapter 47, about 10″. */
internal fun moonPosition(millis: Long): Ecliptic {
  val t = centuries(millis)
  val lp = 218.3164477 + 481267.88123421 * t - 0.0015786 * t * t
  val d = 297.8501921 + 445267.1114034 * t - 0.0018819 * t * t
  val m = 357.5291092 + 35999.0502909 * t - 0.0001536 * t * t
  val mp = 134.9633964 + 477198.8675055 * t + 0.0087414 * t * t
  val f = 93.2720950 + 483202.0175233 * t - 0.0036539 * t * t
  val a1 = 119.75 + 131.849 * t
  val a2 = 53.09 + 479264.290 * t
  val a3 = 313.45 + 481266.484 * t
  val e = 1 - 0.002516 * t - 0.0000074 * t * t

  var sumL = 0.0
  var sumR = 0.0
  for (term in MOON_LONGITUDE_DISTANCE) {
    val argument = term[0] * d + term[1] * m + term[2] * mp + term[3] * f
    val factor = eccentricityFactor(e, term[1])
    sumL += term[4] * factor * sinD(argument)
    sumR += term[5] * factor * cosD(argument)
  }
  var sumB = 0.0
  for (term in MOON_LATITUDE) {
    val argument = term[0] * d + term[1] * m + term[2] * mp + term[3] * f
    sumB += term[4] * eccentricityFactor(e, term[1]) * sinD(argument)
  }
  sumL += 3958 * sinD(a1) + 1962 * sinD(lp - f) + 318 * sinD(a2)
  sumB += -2235 * sinD(lp) + 382 * sinD(a3) + 175 * sinD(a1 - f) + 175 * sinD(a1 + f) + 127 * sinD(lp - mp) -
    115 * sinD(lp + mp)

  val omega = 125.04452 - 1934.136261 * t
  val nutation = -0.004778 * sinD(omega)
  return Ecliptic(normalize(lp + sumL / 1e6 + nutation), sumB / 1e6, 385000.56 + sumR / 1000)
}

private fun eccentricityFactor(e: Double, m: Double): Double =
  when (abs(m).toInt()) {
    1 -> e
    2 -> e * e
    else -> 1.0
  }

private fun obliquity(millis: Long): Double {
  val t = centuries(millis)
  val omega = 125.04 - 1934.136 * t
  return 23.439291 - 0.0130042 * t + 0.00256 * cosD(omega)
}

internal fun toEquatorial(position: Ecliptic, millis: Long): Equatorial {
  val epsilon = obliquity(millis)
  val ra =
    atan2D(
      sinD(position.longitude) * cosD(epsilon) - tanD(position.latitude) * sinD(epsilon),
      cosD(position.longitude),
    )
  val dec =
    asinD(
      sinD(position.latitude) * cosD(epsilon) +
        cosD(position.latitude) * sinD(epsilon) * sinD(position.longitude)
    )
  return Equatorial(normalize(ra), dec)
}

/** Greenwich mean sidereal time. */
private fun siderealTime(millis: Long): Double {
  val jd = julianDay(millis)
  val t = (jd - J2000) / 36525
  return normalize(280.46061837 + 360.98564736629 * (jd - J2000) + 0.000387933 * t * t)
}

/**
 * Altitude and azimuth (from north, clockwise) seen from [point]. [distanceKm] makes the position
 * topocentric, which matters for the Moon (up to 1°).
 */
internal fun horizontal(position: Ecliptic, millis: Long, point: GeoPoint, distanceKm: Double? = null): Horizontal {
  var equatorial = toEquatorial(position, millis)
  val hourAngle0 = siderealTime(millis) + point.longitude - equatorial.rightAscension
  if (distanceKm != null) equatorial = topocentric(equatorial, hourAngle0, point, distanceKm)
  val hourAngle = siderealTime(millis) + point.longitude - equatorial.rightAscension
  val lat = point.latitude
  val dec = equatorial.declination
  val altitude = asinD(sinD(lat) * sinD(dec) + cosD(lat) * cosD(dec) * cosD(hourAngle))
  val azimuth = atan2D(sinD(hourAngle), cosD(hourAngle) * sinD(lat) - tanD(dec) * cosD(lat)) + 180
  return Horizontal(altitude, normalize(azimuth))
}

/** Meeus chapter 40, on a round Earth at sea level. */
private fun topocentric(geocentric: Equatorial, hourAngle: Double, point: GeoPoint, distanceKm: Double): Equatorial {
  val parallax = asinD(EARTH_RADIUS_KM / distanceKm)
  val dec = geocentric.declination
  val lat = point.latitude
  val deltaRa = atan2D(-cosD(lat) * sinD(parallax) * sinD(hourAngle), cosD(dec) - cosD(lat) * sinD(parallax) * cosD(hourAngle))
  val topoDec =
    atan2D(
      (sinD(dec) - sinD(lat) * sinD(parallax)) * cosD(deltaRa),
      cosD(dec) - cosD(lat) * sinD(parallax) * cosD(hourAngle),
    )
  return Equatorial(normalize(geocentric.rightAscension + deltaRa), topoDec)
}

internal fun sunAltitude(millis: Long, point: GeoPoint): Double = horizontal(sunPosition(millis), millis, point).altitude

internal fun moonAltitude(millis: Long, point: GeoPoint): Double {
  val moon = moonPosition(millis)
  return horizontal(moon, millis, point, moon.distanceKm).altitude
}

/** Angle between two points of the sky given by ecliptic coordinates. */
internal fun separation(a: Ecliptic, b: Ecliptic): Double =
  acosD(
    (sinD(a.latitude) * sinD(b.latitude) + cosD(a.latitude) * cosD(b.latitude) * cosD(a.longitude - b.longitude))
      .coerceIn(-1.0, 1.0)
  )

/** Angular radius of the Sun and of the Moon. */
internal fun sunRadius(sun: Ecliptic): Double = asinD(SUN_RADIUS_KM / sun.distanceKm)

internal fun moonRadius(moon: Ecliptic): Double = asinD(MOON_RADIUS_KM / moon.distanceKm)

/** When [f] crosses [threshold], going up (true) or down (false). */
internal data class Crossing(val millis: Long, val isRising: Boolean)

/**
 * Every time [f] crosses [threshold] between [start] and [end], found every [step] and refined to
 * a few seconds.
 */
internal fun crossings(start: Long, end: Long, step: Long, threshold: Double, f: (Long) -> Double): List<Crossing> {
  val result = mutableListOf<Crossing>()
  var t0 = start
  var v0 = f(t0) - threshold
  while (t0 < end) {
    val t1 = minOf(t0 + step, end)
    val v1 = f(t1) - threshold
    if ((v0 < 0) != (v1 < 0)) result += Crossing(bisect(t0, t1, v0 < 0) { f(it) - threshold }, v0 < 0)
    t0 = t1
    v0 = v1
  }
  return result
}

private fun bisect(low: Long, high: Long, isRising: Boolean, f: (Long) -> Double): Long {
  var a = low
  var b = high
  while (b - a > BISECT_PRECISION_MILLIS) {
    val middle = (a + b) / 2
    val isAbove = f(middle) >= 0
    if (isAbove == isRising) b = middle else a = middle
  }
  return (a + b) / 2
}

/** Time of the highest value of [f] between [start] and [end]. */
internal fun maximum(start: Long, end: Long, step: Long, f: (Long) -> Double): Long {
  var best = start
  var bestValue = f(start)
  var t = start
  while (t <= end) {
    val v = f(t)
    if (v > bestValue) {
      best = t
      bestValue = v
    }
    t += step
  }
  // Golden section around the best sample
  var a = maxOf(start, best - step)
  var b = minOf(end, best + step)
  while (b - a > BISECT_PRECISION_MILLIS) {
    val m1 = a + (b - a) / 3
    val m2 = b - (b - a) / 3
    if (f(m1) < f(m2)) a = m1 else b = m2
  }
  return (a + b) / 2
}

// Angle helpers in degrees
internal fun normalize(degrees: Double): Double = degrees - 360 * floor(degrees / 360)

internal fun sinD(x: Double) = sin(x * DEG)

internal fun cosD(x: Double) = cos(x * DEG)

private fun tanD(x: Double) = kotlin.math.tan(x * DEG)

internal fun asinD(x: Double) = asin(x) / DEG

internal fun acosD(x: Double) = acos(x) / DEG

internal fun atan2D(y: Double, x: Double) = atan2(y, x) / DEG

internal fun hypot(x: Double, y: Double) = sqrt(x * x + y * y)

private const val DEG = PI / 180
internal const val MILLIS_PER_DAY = 86_400_000.0
internal const val MILLIS_PER_HOUR = 3_600_000L
internal const val MILLIS_PER_MINUTE = 60_000L
private const val SECONDS_PER_DAY = 86_400.0
private const val UNIX_EPOCH_JD = 2440587.5
private const val J2000 = 2451545.0
private const val KM_PER_AU = 149_597_870.7
internal const val EARTH_RADIUS_KM = 6378.14
private const val SUN_RADIUS_KM = 696_000.0
private const val MOON_RADIUS_KM = 1737.4
private const val BISECT_PRECISION_MILLIS = 5_000L

// D, M, M′, F, then longitude (1e-6°) and distance (1e-3 km), Meeus table 47.A
private val MOON_LONGITUDE_DISTANCE =
  arrayOf(
    doubleArrayOf(0.0, 0.0, 1.0, 0.0, 6288774.0, -20905355.0),
    doubleArrayOf(2.0, 0.0, -1.0, 0.0, 1274027.0, -3699111.0),
    doubleArrayOf(2.0, 0.0, 0.0, 0.0, 658314.0, -2955968.0),
    doubleArrayOf(0.0, 0.0, 2.0, 0.0, 213618.0, -569925.0),
    doubleArrayOf(0.0, 1.0, 0.0, 0.0, -185116.0, 48888.0),
    doubleArrayOf(0.0, 0.0, 0.0, 2.0, -114332.0, -3149.0),
    doubleArrayOf(2.0, 0.0, -2.0, 0.0, 58793.0, 246158.0),
    doubleArrayOf(2.0, -1.0, -1.0, 0.0, 57066.0, -152138.0),
    doubleArrayOf(2.0, 0.0, 1.0, 0.0, 53322.0, -170733.0),
    doubleArrayOf(2.0, -1.0, 0.0, 0.0, 45758.0, -204586.0),
    doubleArrayOf(0.0, 1.0, -1.0, 0.0, -40923.0, -129620.0),
    doubleArrayOf(1.0, 0.0, 0.0, 0.0, -34720.0, 108743.0),
    doubleArrayOf(0.0, 1.0, 1.0, 0.0, -30383.0, 104755.0),
    doubleArrayOf(2.0, 0.0, 0.0, -2.0, 15327.0, 10321.0),
    doubleArrayOf(0.0, 0.0, 1.0, 2.0, -12528.0, 0.0),
    doubleArrayOf(0.0, 0.0, 1.0, -2.0, 10980.0, 79661.0),
    doubleArrayOf(4.0, 0.0, -1.0, 0.0, 10675.0, -34782.0),
    doubleArrayOf(0.0, 0.0, 3.0, 0.0, 10034.0, -23210.0),
    doubleArrayOf(4.0, 0.0, -2.0, 0.0, 8548.0, -21636.0),
    doubleArrayOf(2.0, 1.0, -1.0, 0.0, -7888.0, 24208.0),
    doubleArrayOf(2.0, 1.0, 0.0, 0.0, -6766.0, 30824.0),
    doubleArrayOf(1.0, 0.0, -1.0, 0.0, -5163.0, -8379.0),
    doubleArrayOf(1.0, 1.0, 0.0, 0.0, 4987.0, -16675.0),
    doubleArrayOf(2.0, -1.0, 1.0, 0.0, 4036.0, -12831.0),
    doubleArrayOf(2.0, 0.0, 2.0, 0.0, 3994.0, -10445.0),
    doubleArrayOf(4.0, 0.0, 0.0, 0.0, 3861.0, -11650.0),
    doubleArrayOf(2.0, 0.0, -3.0, 0.0, 3665.0, 14403.0),
    doubleArrayOf(0.0, 1.0, -2.0, 0.0, -2689.0, -7003.0),
    doubleArrayOf(2.0, 0.0, -1.0, 2.0, -2602.0, 0.0),
    doubleArrayOf(2.0, -1.0, -2.0, 0.0, 2390.0, 10056.0),
    doubleArrayOf(1.0, 0.0, 1.0, 0.0, -2348.0, 6322.0),
    doubleArrayOf(2.0, -2.0, 0.0, 0.0, 2236.0, -9884.0),
    doubleArrayOf(0.0, 1.0, 2.0, 0.0, -2120.0, 5751.0),
    doubleArrayOf(0.0, 2.0, 0.0, 0.0, -2069.0, 0.0),
    doubleArrayOf(2.0, -2.0, -1.0, 0.0, 2048.0, -4950.0),
    doubleArrayOf(2.0, 0.0, 1.0, -2.0, -1773.0, 4130.0),
    doubleArrayOf(2.0, 0.0, 0.0, 2.0, -1595.0, 0.0),
    doubleArrayOf(4.0, -1.0, -1.0, 0.0, 1215.0, -3958.0),
    doubleArrayOf(0.0, 0.0, 2.0, 2.0, -1110.0, 0.0),
    doubleArrayOf(3.0, 0.0, -1.0, 0.0, -892.0, 3258.0),
    doubleArrayOf(2.0, 1.0, 1.0, 0.0, -810.0, 2616.0),
    doubleArrayOf(4.0, -1.0, -2.0, 0.0, 759.0, -1897.0),
    doubleArrayOf(0.0, 2.0, -1.0, 0.0, -713.0, -2117.0),
    doubleArrayOf(2.0, 2.0, -1.0, 0.0, -700.0, 2354.0),
    doubleArrayOf(2.0, 1.0, -2.0, 0.0, 691.0, 0.0),
    doubleArrayOf(2.0, -1.0, 0.0, -2.0, 596.0, 0.0),
    doubleArrayOf(4.0, 0.0, 1.0, 0.0, 549.0, -1423.0),
    doubleArrayOf(0.0, 0.0, 4.0, 0.0, 537.0, -1117.0),
    doubleArrayOf(4.0, -1.0, 0.0, 0.0, 520.0, -1571.0),
    doubleArrayOf(1.0, 0.0, -2.0, 0.0, -487.0, -1739.0),
  )

// D, M, M′, F, then latitude (1e-6°), Meeus table 47.B
private val MOON_LATITUDE =
  arrayOf(
    doubleArrayOf(0.0, 0.0, 0.0, 1.0, 5128122.0),
    doubleArrayOf(0.0, 0.0, 1.0, 1.0, 280602.0),
    doubleArrayOf(0.0, 0.0, 1.0, -1.0, 277693.0),
    doubleArrayOf(2.0, 0.0, 0.0, -1.0, 173237.0),
    doubleArrayOf(2.0, 0.0, -1.0, 1.0, 55413.0),
    doubleArrayOf(2.0, 0.0, -1.0, -1.0, 46271.0),
    doubleArrayOf(2.0, 0.0, 0.0, 1.0, 32573.0),
    doubleArrayOf(0.0, 0.0, 2.0, 1.0, 17198.0),
    doubleArrayOf(2.0, 0.0, 1.0, -1.0, 9266.0),
    doubleArrayOf(0.0, 0.0, 2.0, -1.0, 8822.0),
    doubleArrayOf(2.0, -1.0, 0.0, -1.0, 8216.0),
    doubleArrayOf(2.0, 0.0, -2.0, -1.0, 4324.0),
    doubleArrayOf(2.0, 0.0, 1.0, 1.0, 4200.0),
    doubleArrayOf(2.0, 1.0, 0.0, -1.0, -3359.0),
    doubleArrayOf(2.0, -1.0, -1.0, 1.0, 2463.0),
    doubleArrayOf(2.0, -1.0, 0.0, 1.0, 2211.0),
    doubleArrayOf(2.0, -1.0, -1.0, -1.0, 2065.0),
    doubleArrayOf(0.0, 1.0, -1.0, -1.0, -1870.0),
    doubleArrayOf(4.0, 0.0, -1.0, -1.0, 1828.0),
    doubleArrayOf(0.0, 1.0, 0.0, 1.0, -1794.0),
    doubleArrayOf(0.0, 0.0, 0.0, 3.0, -1749.0),
    doubleArrayOf(0.0, 1.0, -1.0, 1.0, -1565.0),
    doubleArrayOf(1.0, 0.0, 0.0, 1.0, -1491.0),
    doubleArrayOf(0.0, 1.0, 1.0, 1.0, -1475.0),
    doubleArrayOf(0.0, 1.0, 1.0, -1.0, -1410.0),
    doubleArrayOf(0.0, 1.0, 0.0, -1.0, -1344.0),
    doubleArrayOf(1.0, 0.0, 0.0, -1.0, -1335.0),
    doubleArrayOf(0.0, 0.0, 3.0, 1.0, 1107.0),
    doubleArrayOf(4.0, 0.0, 0.0, -1.0, 1021.0),
    doubleArrayOf(4.0, 0.0, -1.0, 1.0, 833.0),
  )
