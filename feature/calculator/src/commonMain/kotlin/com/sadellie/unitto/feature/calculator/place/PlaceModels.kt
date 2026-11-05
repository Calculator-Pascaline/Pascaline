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

// Constants of the data files and of published models, they read best where they are used
@file:Suppress("MagicNumber")

package com.sadellie.unitto.feature.calculator.place

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.sin

// The data comes from Trail Sense (MIT, Kyle Corry): 1991–2020 temperatures of NASA MERRA-2,
// precipitation of ERA5, tides of EOT20, elevation of ETOPO 2022. The readers here are our own.

/** Pixel of [latitude] and [longitude] in a world image from 90° N and 180° W. */
internal fun worldPixel(
  latitude: Double,
  longitude: Double,
  width: Int,
  degreesPerRow: Double,
  offset: Double = 0.0,
): Pair<Double, Double> {
  val degreesPerColumn = 360.0 / width
  val x = (longitude - (-180 + degreesPerColumn * offset)) / degreesPerColumn
  val y = (90 - degreesPerRow * offset - latitude) / degreesPerRow
  return x to y
}

// Temperatures

/** Mean, then cosine and sine of 4 yearly harmonics, a polynomial of sin(latitude) taken from the mean. */
internal class TemperatureModel(val name: String, val a: DoubleArray, val b: DoubleArray, val latitudeTrend: DoubleArray)

internal val LOW_TEMPERATURES =
  TemperatureModel(
    "T2MMIN",
    doubleArrayOf(7.501739978937039, 5.413969975824311, 21.138197621731482, 29.566794657662687, 35.62256274775451,
      88.88629055054261, 99.45095993807621, 125.79888140221307, 114.07951390332093),
    doubleArrayOf(18.745699481670854, 29.572524010505866, 7.553063666579181, 2.918301145235703, 4.930826481866384,
      1.2140325705210344, 1.3450998465220136, 0.7235540946324646, 1.282492296914194),
    doubleArrayOf(26.221280651958807, -1.0207776340699977, -21.56547273042308, 4.195560445789451, -25.336009333733315),
  )

internal val HIGH_TEMPERATURES =
  TemperatureModel(
    "T2MMAX",
    doubleArrayOf(7.709349528027072, 5.189169209136491, 20.03953671972627, 28.008160806051006, 33.61514510543693,
      88.22570886867366, 101.28170061607227, 162.36093326747266, 101.82051748444593),
    doubleArrayOf(14.93543791613947, 31.90982674529993, 6.240338423615686, 3.459561824798583, 4.65315517067565,
      1.4013398885726946, 1.2748637199401842, 0.7270233829816135, 1.5395452215317444),
    doubleArrayOf(29.28949352280773, 3.2050577284753032, -14.849103847257076, 0.31804828419864367, -31.381918255889566),
  )

internal const val TEMPERATURE_WIDTH = 576
internal const val TEMPERATURE_DEGREES_PER_ROW = 0.5

/** Coefficients at sea level from the 9 pixel values (0–255) of [model]. */
internal fun temperatureCoefficients(model: TemperatureModel, pixels: DoubleArray, latitude: Double): DoubleArray {
  val coefficients = DoubleArray(pixels.size) { pixels[it] / model.a[it] - model.b[it] }
  val sinLatitude = sinD(latitude)
  var trend = 0.0
  for (i in model.latitudeTrend.indices.reversed()) trend = trend * sinLatitude + model.latitudeTrend[i]
  coefficients[0] += trend
  return coefficients
}

/** Temperature at sea level [months] after mid-January, a fraction of months works too. */
internal fun temperatureAt(coefficients: DoubleArray, months: Double): Double {
  val angle = 2 * PI * months / 12
  var value = coefficients[0]
  for (h in 1..4) value += coefficients[2 * h - 1] * cos(h * angle) + coefficients[2 * h] * sin(h * angle)
  return value
}

/** Months from mid-January to day [dayOfYear] (1 is 1 January). */
internal fun monthsFromMidJanuary(dayOfYear: Int, daysInYear: Int): Double = (dayOfYear - 15.5) * 12 / daysInYear

/** Air cools by 6.5 °C every 1 000 m up, the standard lapse rate. */
internal fun atElevation(seaLevelCelsius: Double, elevation: Double): Double =
  seaLevelCelsius - 0.0065 * elevation.coerceAtLeast(0.0)

// Precipitation

internal const val PRECIPITATION_WIDTH = 1440
internal const val PRECIPITATION_DEGREES_PER_ROW = 0.25

/** Precipitation in mm of a month from its pixel value (0–255), a square root was stored. */
internal fun precipitation(pixel: Int, daysInMonth: Int): Double {
  val per30Days = (pixel / 4.417794704437256).let { it * it }
  return per30Days / 30 * daysInMonth
}

/** Gaussen: a month is dry when its rain in mm is less than twice its mean temperature in °C. */
internal fun isDryMonth(precipitationMm: Double, meanCelsius: Double): Boolean =
  meanCelsius > 0 && precipitationMm < 2 * meanCelsius

/** Below 0 °C it falls as snow, above 4 °C as rain, a mix between. */
internal fun snowShare(meanCelsius: Double): Double = ((4 - meanCelsius) / 4).coerceIn(0.0, 1.0)

/** Köppen–Geiger climate, from monthly mean temperatures and precipitation, January first. */
internal enum class Climate(val code: String) {
  TROPICAL_RAINFOREST("Af"),
  TROPICAL_MONSOON("Am"),
  TROPICAL_SAVANNA("Aw"),
  HOT_DESERT("BWh"),
  COLD_DESERT("BWk"),
  HOT_STEPPE("BSh"),
  COLD_STEPPE("BSk"),
  MEDITERRANEAN("Cs"),
  SUBTROPICAL_DRY_WINTER("Cw"),
  HUMID_SUBTROPICAL("Cfa"),
  OCEANIC("Cfb"),
  SUBPOLAR_OCEANIC("Cfc"),
  HOT_CONTINENTAL("Dfa"),
  CONTINENTAL("Dfb"),
  SUBARCTIC("Dfc"),
  TUNDRA("ET"),
  ICE_CAP("EF"),
}

internal fun koppen(temperatures: DoubleArray, precipitation: DoubleArray, latitude: Double): Climate =
  polar(temperatures)
    ?: arid(temperatures, precipitation, latitude)
    ?: tropical(temperatures, precipitation)
    ?: temperateOrContinental(temperatures, precipitation, latitude)

private fun polar(temperatures: DoubleArray): Climate? =
  when {
    temperatures.max() < 0 -> Climate.ICE_CAP
    temperatures.max() < 10 -> Climate.TUNDRA
    else -> null
  }

/** Summer is April to September in the north. */
private fun summerMonths(latitude: Double): List<Int> = if (latitude >= 0) (3..8).toList() else (0..2) + (9..11)

private fun arid(temperatures: DoubleArray, precipitation: DoubleArray, latitude: Double): Climate? {
  val mean = temperatures.average()
  val total = precipitation.sum()
  val summerRain = summerMonths(latitude).sumOf { precipitation[it] }
  val threshold =
    when {
      summerRain >= 0.7 * total -> 20 * mean + 280
      summerRain >= 0.3 * total -> 20 * mean + 140
      else -> 20 * mean
    }
  val isHot = mean >= 18
  return when {
    total >= threshold -> null
    total < threshold / 2 -> if (isHot) Climate.HOT_DESERT else Climate.COLD_DESERT
    else -> if (isHot) Climate.HOT_STEPPE else Climate.COLD_STEPPE
  }
}

private fun tropical(temperatures: DoubleArray, precipitation: DoubleArray): Climate? {
  val driest = precipitation.min()
  return when {
    temperatures.min() < 18 -> null
    driest >= 60 -> Climate.TROPICAL_RAINFOREST
    driest >= 100 - precipitation.sum() / 25 -> Climate.TROPICAL_MONSOON
    else -> Climate.TROPICAL_SAVANNA
  }
}

private fun temperateOrContinental(temperatures: DoubleArray, precipitation: DoubleArray, latitude: Double): Climate {
  val summer = summerMonths(latitude)
  val winter = (0..11) - summer.toSet()
  val summerDriest = summer.minOf { precipitation[it] }
  val isDrySummer = summerDriest < 40 && summerDriest < winter.maxOf { precipitation[it] } / 3
  val isDryWinter = winter.minOf { precipitation[it] } < summer.maxOf { precipitation[it] } / 10
  val warmest = temperatures.max()
  val warmMonths = temperatures.count { it >= 10 }
  val isTemperate = temperatures.min() > -3
  return when {
    isTemperate && isDrySummer -> Climate.MEDITERRANEAN
    isTemperate && isDryWinter -> Climate.SUBTROPICAL_DRY_WINTER
    isTemperate && warmest >= 22 -> Climate.HUMID_SUBTROPICAL
    isTemperate && warmMonths >= 4 -> Climate.OCEANIC
    isTemperate -> Climate.SUBPOLAR_OCEANIC
    warmest >= 22 -> Climate.HOT_CONTINENTAL
    warmMonths >= 4 -> Climate.CONTINENTAL
    else -> Climate.SUBARCTIC
  }
}

// Tides

/** EOT20 constituents in the order of the files, with the largest amplitude (cm) of each. */
internal enum class Constituent(
  val maxAmplitudeCm: Double,
  /** Doodson numbers: lunar time, s, h, p, N′, p′, then a phase in degrees. */
  val doodson: IntArray,
) {
  TWO_N2(35.562755148866074, intArrayOf(2, -2, 0, 2, 0, 0, 0)),
  J1(19.412978757674825, intArrayOf(1, 2, 0, -1, 0, 0, 90)),
  K1(293.3113265854845, intArrayOf(1, 1, 0, 0, 0, 0, 90)),
  K2(136.46533334158224, intArrayOf(2, 2, 0, 0, 0, 0, 0)),
  M2(474.8659738027007, intArrayOf(2, 0, 0, 0, 0, 0, 0)),
  M4(136.47406850713315, intArrayOf(4, 0, 0, 0, 0, 0, 0)),
  MF(26.57222851875556, intArrayOf(0, 2, 0, 0, 0, 0, 0)),
  MM(38.845984640449046, intArrayOf(0, 1, 0, -1, 0, 0, 0)),
  N2(107.99308333748937, intArrayOf(2, -1, 0, 1, 0, 0, 0)),
  O1(163.9411506321465, intArrayOf(1, -1, 0, 0, 0, 0, -90)),
  P1(282.97924579874797, intArrayOf(1, 1, -2, 0, 0, 0, -90)),
  Q1(31.17831745303195, intArrayOf(1, -2, 0, 1, 0, 0, -90)),
  S1(368.84208613374, intArrayOf(1, 1, -1, 0, 0, 0, 180)),
  S2(321.04079565433807, intArrayOf(2, 2, -2, 0, 0, 0, 0)),
  SA(360.6890664468158, intArrayOf(0, 0, 1, 0, 0, 0, 0)),
  SSA(8.62799275024558, intArrayOf(0, 0, 2, 0, 0, 0, 0)),
  T2(487.1148963104638, intArrayOf(2, 2, -3, 0, 0, 1, 0)),
}

/** Amplitudes too large for the images, kept apart in the data: constituent, x, y, amplitude (cm). */
internal val LARGE_AMPLITUDES: List<LargeAmplitude> =
  listOf(
    LargeAmplitude(Constituent.P1, 60, 29, 1101.0), LargeAmplitude(Constituent.P1, 61, 29, 891.0),
    LargeAmplitude(Constituent.P1, 64, 29, 774.0), LargeAmplitude(Constituent.P1, 65, 29, 688.0),
    LargeAmplitude(Constituent.P1, 66, 29, 909.0), LargeAmplitude(Constituent.S1, 60, 29, 1598.0),
    LargeAmplitude(Constituent.S1, 61, 29, 1828.0), LargeAmplitude(Constituent.S1, 62, 29, 867.0),
    LargeAmplitude(Constituent.S1, 64, 29, 619.0), LargeAmplitude(Constituent.S1, 65, 29, 1150.0),
    LargeAmplitude(Constituent.S1, 66, 29, 1612.0), LargeAmplitude(Constituent.S1, 41, 95, 3480.0),
    LargeAmplitude(Constituent.S1, 42, 95, 1946.0), LargeAmplitude(Constituent.S1, 43, 95, 1329.0),
    LargeAmplitude(Constituent.S1, 46, 95, 508.0), LargeAmplitude(Constituent.S1, 53, 95, 3233.0),
    LargeAmplitude(Constituent.S1, 54, 95, 4575.0), LargeAmplitude(Constituent.S1, 57, 95, 2436.0),
    LargeAmplitude(Constituent.S1, 58, 95, 883.0), LargeAmplitude(Constituent.S2, 60, 29, 3101.0),
    LargeAmplitude(Constituent.S2, 61, 29, 3030.0), LargeAmplitude(Constituent.S2, 62, 29, 1238.0),
    LargeAmplitude(Constituent.S2, 64, 29, 1650.0), LargeAmplitude(Constituent.S2, 65, 29, 2097.0),
    LargeAmplitude(Constituent.S2, 66, 29, 2851.0), LargeAmplitude(Constituent.S2, 41, 95, 2911.0),
    LargeAmplitude(Constituent.S2, 42, 95, 2112.0), LargeAmplitude(Constituent.S2, 43, 95, 2626.0),
    LargeAmplitude(Constituent.S2, 46, 95, 516.0), LargeAmplitude(Constituent.S2, 53, 95, 2636.0),
    LargeAmplitude(Constituent.S2, 54, 95, 2509.0), LargeAmplitude(Constituent.S2, 55, 95, 1184.0),
    LargeAmplitude(Constituent.S2, 57, 95, 1681.0), LargeAmplitude(Constituent.S2, 58, 95, 559.0),
    LargeAmplitude(Constituent.SA, 60, 29, 1361.0), LargeAmplitude(Constituent.SA, 61, 29, 1069.0),
    LargeAmplitude(Constituent.SA, 64, 29, 996.0), LargeAmplitude(Constituent.SA, 65, 29, 843.0),
    LargeAmplitude(Constituent.SA, 66, 29, 1106.0), LargeAmplitude(Constituent.T2, 60, 29, 2687.0),
    LargeAmplitude(Constituent.T2, 61, 29, 2115.0), LargeAmplitude(Constituent.T2, 62, 29, 652.0),
    LargeAmplitude(Constituent.T2, 64, 29, 1959.0), LargeAmplitude(Constituent.T2, 65, 29, 1662.0),
    LargeAmplitude(Constituent.T2, 66, 29, 2186.0), LargeAmplitude(Constituent.T2, 67, 29, 710.0),
    LargeAmplitude(Constituent.T2, 54, 95, 628.0), LargeAmplitude(Constituent.T2, 55, 95, 503.0),
  )

internal data class LargeAmplitude(val constituent: Constituent, val x: Int, val y: Int, val amplitudeCm: Double)

/** Amplitude in meters and Greenwich phase lag in degrees. */
internal data class TideHarmonic(val constituent: Constituent, val amplitude: Double, val phase: Double)

internal const val TIDE_INDEX_WIDTH = 1440
internal const val TIDE_DEGREES_PER_ROW = 0.25
internal const val TIDE_INDEX_OFFSET = 0.25
internal const val TIDE_SEARCH = 8

/** Tides of the sea cell closest to a point, [distanceKm] away. */
internal data class TideSite(val harmonics: List<TideHarmonic>, val distanceKm: Double)

/** Middle of the pixel [x], [y] of the tide index. */
internal fun tidePixelPoint(x: Int, y: Int): GeoPoint =
  GeoPoint(
    90 - TIDE_DEGREES_PER_ROW * (y + TIDE_INDEX_OFFSET),
    wrapLongitude(-180 + TIDE_DEGREES_PER_ROW * (x + TIDE_INDEX_OFFSET)),
  )

/** Farther than this the tides of the sea don't tell much about the point. */
internal const val MAX_TIDE_DISTANCE_KM = 40.0

/** Harmonic from the amplitude and phase pixels (0–255) of the cell [x], [y] of the files. */
internal fun tideHarmonic(constituent: Constituent, amplitudePixel: Int, phasePixel: Int, x: Int, y: Int): TideHarmonic {
  val large = LARGE_AMPLITUDES.firstOrNull { it.constituent == constituent && it.x == x && it.y == y }
  val amplitudeCm = large?.amplitudeCm ?: (amplitudePixel / 255.0 * constituent.maxAmplitudeCm)
  val phase = normalize(phasePixel / 255.0 * 360 - 180)
  return TideHarmonic(constituent, amplitudeCm / 100, phase)
}

/**
 * Water level in meters from mean sea level at [millis]: the sum of the constituents with their
 * astronomical arguments (Doodson) and the nodal corrections of the Moon's orbit.
 */
internal fun tideHeight(harmonics: List<TideHarmonic>, millis: Long): Double {
  val t = (julianDay(millis) - 2451545.0) / 36525
  val s = 218.3164 + 481267.8812 * t
  val h = 280.4661 + 36000.7698 * t
  val p = 83.3535 + 4069.0137 * t
  val n = 125.0445 - 1934.1363 * t
  val pp = 282.9384 + 1.7195 * t
  val hoursUt = (millis.mod(MILLIS_PER_DAY.toLong())) / MILLIS_PER_HOUR.toDouble()
  val tau = 15 * hoursUt + h - s
  var total = 0.0
  for (harmonic in harmonics) {
    val k = harmonic.constituent.doodson
    val (f, u) = nodal(harmonic.constituent, n)
    val argument = k[0] * tau + k[1] * s + k[2] * h + k[3] * p + k[4] * -n + k[5] * pp + k[6]
    total += f * harmonic.amplitude * cosD(argument + u - harmonic.phase)
  }
  return total
}

/** Amplitude factor f and phase u of [constituent] for the longitude [node] of the Moon's node. */
private fun nodal(constituent: Constituent, node: Double): Pair<Double, Double> {
  val c = cosD(node)
  val s = sinD(node)
  val m2 = (1 - 0.037 * c) to (-2.1 * s)
  return when (constituent) {
    Constituent.M2,
    Constituent.N2,
    Constituent.TWO_N2 -> m2
    Constituent.M4 -> m2.first * m2.first to 2 * m2.second
    Constituent.K2 -> (1.024 + 0.286 * c) to (-17.7 * s)
    Constituent.K1 -> (1.006 + 0.115 * c) to (-8.9 * s)
    Constituent.O1,
    Constituent.Q1 -> (1.009 + 0.187 * c) to (10.8 * s)
    Constituent.J1 -> (1.013 + 0.168 * c) to (-12.9 * s)
    Constituent.MF -> (1.043 + 0.414 * c) to (-23.7 * s)
    Constituent.MM -> (1 - 0.130 * c) to 0.0
    else -> 1.0 to 0.0
  }
}

internal data class TideExtreme(val millis: Long, val height: Double, val isHigh: Boolean)

/** High and low waters between [start] and [end]. */
internal fun tideExtremes(harmonics: List<TideHarmonic>, start: Long, end: Long): List<TideExtreme> {
  val step = 5 * MILLIS_PER_MINUTE
  val result = mutableListOf<TideExtreme>()
  var previous = tideHeight(harmonics, start - step)
  var current = tideHeight(harmonics, start)
  var t = start
  while (t < end) {
    val next = tideHeight(harmonics, t + step)
    if (current > previous && current >= next) result += TideExtreme(t, current, true)
    if (current < previous && current <= next) result += TideExtreme(t, current, false)
    previous = current
    current = next
    t += step
  }
  return result
}

/** Spring tides come a day or two after new and full moons, when the Sun and Moon pull together. */
internal fun isSpringTide(elongation: Double): Boolean {
  val fromSyzygy = minOf(abs(normalize(elongation - 25)), abs(normalize(elongation - 205)))
  return minOf(fromSyzygy, 360 - fromSyzygy) < 30
}

// Elevation

/** One tile of the elevation model: its file, the decoding and the box it covers. */
internal data class ElevationTile(
  val file: Int,
  val a: Double,
  val b: Double,
  val north: Double,
  val south: Double,
  val west: Double,
  val east: Double,
)

/** Tiles of `dem/index.json`: id, a, b, latitude, longitude, latitude, longitude, width, height. */
internal fun elevationTiles(rows: List<List<Double>>): List<ElevationTile> =
  rows.map {
    ElevationTile(
      file = it[0].roundToInt(),
      a = it[1],
      b = it[2],
      north = maxOf(it[3], it[5]),
      south = minOf(it[3], it[5]),
      west = minOf(it[4], it[6]),
      east = maxOf(it[4], it[6]),
    )
  }

internal fun List<ElevationTile>.tileAt(point: GeoPoint): ElevationTile? = firstOrNull {
  point.latitude in it.south..it.north && point.longitude in it.west..it.east
}

/** Pixel of [point] in the [width] × [height] image of [tile]. */
internal fun ElevationTile.pixel(point: GeoPoint, width: Int, height: Int): Pair<Int, Int> {
  val x = ((point.longitude - west) / (east - west) * width - 0.5).roundToInt().coerceIn(0, width - 1)
  val y = ((north - point.latitude) / (north - south) * height - 0.5).roundToInt().coerceIn(0, height - 1)
  return x to y
}

/** Meters from the red value of a pixel, never below the sea in this model. */
internal fun ElevationTile.elevation(pixel: Int): Double = (pixel / a - b).coerceAtLeast(0.0)

// Time zones

internal data class ZoneCity(val zone: String, val point: GeoPoint)

/** Rows of IANA's zone1970.tab: countries, ±DDMM±DDDMM (or with seconds), zone. */
internal fun parseZones(text: String): List<ZoneCity> =
  text.lineSequence()
    .filter { it.isNotBlank() && !it.startsWith("#") }
    .mapNotNull { line ->
      val columns = line.split('\t')
      val point = parseIso6709(columns.getOrNull(1) ?: return@mapNotNull null) ?: return@mapNotNull null
      ZoneCity(columns.getOrNull(2) ?: return@mapNotNull null, point)
    }
    .toList()

private fun parseIso6709(text: String): GeoPoint? {
  val split = text.indexOfAny(charArrayOf('+', '-'), startIndex = 1)
  val latitude = if (split > 0) sexagesimal(text.substring(0, split), 2) else null
  val longitude = if (split > 0) sexagesimal(text.substring(split), 3) else null
  return if (latitude != null && longitude != null) GeoPoint(latitude, longitude) else null
}

private fun sexagesimal(text: String, degreeDigits: Int): Double? {
  val sign = if (text[0] == '-') -1 else 1
  val digits = text.substring(1)
  val degrees = digits.take(degreeDigits).toIntOrNull() ?: return null
  val minutes = digits.drop(degreeDigits).take(2).toIntOrNull() ?: 0
  val seconds = digits.drop(degreeDigits + 2).take(2).toIntOrNull() ?: 0
  return sign * (degrees + minutes / 60.0 + seconds / 3600.0)
}

/** Zone of the city closest to [point], by the angle between them. */
internal fun nearestZone(zones: List<ZoneCity>, point: GeoPoint): String? =
  zones.minByOrNull { greatCircle(it.point, point) }?.zone

internal fun greatCircle(a: GeoPoint, b: GeoPoint): Double =
  acosD(
    (sinD(a.latitude) * sinD(b.latitude) + cosD(a.latitude) * cosD(b.latitude) * cosD(a.longitude - b.longitude))
      .coerceIn(-1.0, 1.0)
  )

/** Days in each month of [year], January first. */
internal fun daysInMonths(year: Int): IntArray {
  val isLeap = (year % 4 == 0 && year % 100 != 0) || year % 400 == 0
  return intArrayOf(31, if (isLeap) 29 else 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
}

/** Wraps a longitude to −180..180. */
internal fun wrapLongitude(longitude: Double): Double = longitude - 360 * floor((longitude + 180) / 360)
