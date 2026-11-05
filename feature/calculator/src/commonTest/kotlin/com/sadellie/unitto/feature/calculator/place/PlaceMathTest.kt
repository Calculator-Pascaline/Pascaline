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

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.UtcOffset
import kotlinx.datetime.toInstant

/**
 * Expected values: Meeus' worked examples, published sunrise, phase, season and eclipse times, and
 * the tide times Trail Sense tests its own model with.
 */
class PlaceMathTest {
  @Test
  fun moonPosition_meeusExample47a() {
    // 1992 April 12 at 0h TD, about a minute later in TD than in UT
    val moon = moonPosition(millisOfJulianDay(2448724.5) - 60_000)
    assertEquals(133.162655, moon.longitude, 0.01)
    assertEquals(-3.229126, moon.latitude, 0.01)
    assertEquals(368409.7, moon.distanceKm, 30.0)
  }

  @Test
  fun sunPosition_meeusExample25a() {
    val sun = sunPosition(millisOfJulianDay(2448908.5) - 60_000)
    assertEquals(199.90988, sun.longitude, 0.01)
    assertEquals(0.99766 * 149_597_870.7, sun.distanceKm, 20_000.0)
  }

  @Test
  fun sunDay_parisAtMidsummer() {
    val day = sunDay(utc("2026-06-20T22:00"), PARIS)
    // 05:47 and 21:58 in Paris (UTC+2)
    assertNear(utc("2026-06-21T03:47"), day.sunrise!!, 3)
    assertNear(utc("2026-06-21T19:58"), day.sunset!!, 3)
    assertEquals(64.5, day.noonAltitude, 0.3)
    assertTrue(day.civilDusk!! > day.sunset!!)
    assertTrue(day.nauticalDusk!! > day.civilDusk!!)
    // No astronomical night in Paris in June
    assertNull(day.astronomicalDusk)
  }

  @Test
  fun sunDay_polarNight() {
    val day = sunDay(utc("2026-12-21T00:00"), GeoPoint(78.22, 15.65))
    assertNull(day.sunrise)
    assertNull(day.sunset)
    assertEquals(0L, day.daylight)
  }

  @Test
  fun nextPhase_fullMoonOfMarch2026() {
    assertNear(utc("2026-03-03T11:38"), nextPhase(utc("2026-02-20T00:00"), 180.0), 10)
  }

  @Test
  fun moonState_fullIsLit() {
    val full = moonState(utc("2026-03-03T11:38"))
    assertEquals(MoonPhase.FULL, full.phase)
    assertTrue(full.illumination > 0.99)
    assertEquals(MoonPhase.NEW, moonState(nextPhase(utc("2026-03-04T00:00"), 0.0)).phase)
  }

  @Test
  fun nextSeason_marchEquinox2026() {
    val season = nextSeason(utc("2026-02-01T00:00"))
    assertEquals(Season.MARCH_EQUINOX, season.season)
    assertNear(utc("2026-03-20T14:46"), season.millis, 10)
  }

  @Test
  fun lunarEclipse_march2026FromHonolulu() {
    val eclipse = assertNotNull(nextLunarEclipse(utc("2026-01-01T00:00"), GeoPoint(21.31, -157.86), years = 1))
    assertEquals(EclipseKind.TOTAL, eclipse.kind)
    assertNear(utc("2026-03-03T11:33"), eclipse.peak, 20)
  }

  @Test
  fun solarEclipse_august2026FromParis() {
    val eclipse = assertNotNull(nextSolarEclipse(utc("2026-01-01T00:00"), PARIS, years = 2))
    assertEquals(EclipseKind.PARTIAL, eclipse.kind)
    assertNear(utc("2026-08-12T18:17"), eclipse.peak, 20)
    assertTrue(eclipse.magnitude > 0.7, "covered ${eclipse.magnitude}")
  }

  @Test
  fun solarEclipse_totalInLuxor2027() {
    val eclipse = assertNotNull(nextSolarEclipse(utc("2026-09-01T00:00"), GeoPoint(25.69, 32.64), years = 2))
    assertEquals(EclipseKind.TOTAL, eclipse.kind)
    assertNear(utc("2027-08-02T10:07"), eclipse.peak, 20)
  }

  @Test
  fun overlapFraction_discs() {
    assertEquals(0.0, overlapFraction(0.27, 0.26, 0.6))
    assertEquals(1.0, overlapFraction(0.26, 0.27, 0.005))
    assertEquals(0.5, overlapFraction(1.0, 1.0, 0.8079), 0.01)
  }

  @Test
  fun meteorShowers_perseidsInAugust() {
    val showers = nextShowers(utc("2026-08-01T00:00"), PARIS)
    assertEquals(MeteorShower.PERSEIDS, showers.first().shower)
    assertNear(utc("2026-08-13T00:00"), showers.first().peak, 24 * 60)
    assertTrue(showers.first().radiantMaxAltitude > 60)
  }

  @Test
  fun tides_rhodeIsland() {
    val harmonics =
      harmonics(
        54,
        32,
        intArrayOf(8, 5, 5, 6, 25, 6, 7, 5, 26, 7, 2, 7, 0, 7, 2, 15, 0),
        intArrayOf(96, 3, 249, 137, 126, 136, 88, 128, 113, 16, 1, 255, 203, 140, 64, 210, 196),
      )
    assertTides(harmonics, "2024-11-25", "-05:00", listOf("H 02:21", "L 08:33", "H 14:42", "L 21:02"))
  }

  @Test
  fun tides_hongKong() {
    val harmonics =
      harmonics(
        102,
        139,
        intArrayOf(14, 19, 30, 9, 21, 9, 7, 5, 18, 44, 10, 43, 1, 12, 5, 2, 1),
        intArrayOf(140, 10, 254, 170, 149, 34, 167, 168, 139, 226, 253, 214, 215, 165, 99, 214, 172),
      )
    assertTides(harmonics, "2024-11-27", "+08:00", listOf("L 01:10", "H 06:47", "L 12:34", "H 19:03"))
  }

  @Test
  fun tides_perthHasOneTideADay() {
    val harmonics =
      harmonics(
        112,
        130,
        intArrayOf(4, 12, 15, 2, 3, 1, 3, 2, 3, 19, 5, 21, 1, 3, 4, 4, 0),
        intArrayOf(175, 25, 2, 152, 168, 218, 140, 20, 206, 252, 251, 251, 108, 170, 229, 8, 61),
      )
    assertTides(harmonics, "2024-12-03", "+08:00", listOf("L 06:42", "H 21:51"))
  }

  @Test
  fun tidePixelPoint_middleOfThePixel() {
    val corner = tidePixelPoint(0, 0)
    assertEquals(89.9375, corner.latitude, 1e-9)
    assertEquals(-179.9375, corner.longitude, 1e-9)
    // Le Havre is a sea cell, Paris is 180 km from it: too far for its tides
    assertTrue(greatCircle(GeoPoint(48.86, 2.35), GeoPoint(49.49, 0.11)) * 111.2 > MAX_TIDE_DISTANCE_KM)
  }

  @Test
  fun temperatureAt_harmonics() {
    // Mean 10, one yearly wave of ±8 at its warmest in mid-July
    val coefficients = doubleArrayOf(10.0, -8.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0)
    assertEquals(2.0, temperatureAt(coefficients, 0.0), 1e-9)
    assertEquals(18.0, temperatureAt(coefficients, 6.0), 1e-9)
    assertEquals(0.0, monthsFromMidJanuary(15, 365), 0.05)
    assertEquals(13.5, atElevation(20.0, 1000.0), 1e-9)
    assertEquals(20.0, atElevation(20.0, -50.0), 1e-9)
  }

  @Test
  fun precipitation_squareRootStored() {
    assertEquals(0.0, precipitation(0, 31), 1e-9)
    assertEquals(31.0 / 30 * (100 / 4.417794704437256).let { it * it }, precipitation(100, 31), 1e-9)
    assertTrue(isDryMonth(20.0, 25.0))
    assertTrue(!isDryMonth(80.0, 25.0))
    assertEquals(1.0, snowShare(-5.0))
    assertEquals(0.0, snowShare(6.0))
  }

  @Test
  fun koppen_knownClimates() {
    assertEquals(
      Climate.OCEANIC,
      koppen(
        doubleArrayOf(5.0, 6.0, 9.0, 12.0, 15.0, 19.0, 21.0, 20.0, 17.0, 13.0, 8.0, 5.0),
        doubleArrayOf(51.0, 41.0, 48.0, 52.0, 63.0, 50.0, 62.0, 52.0, 48.0, 62.0, 51.0, 58.0),
        48.9,
      ),
    )
    assertEquals(
      Climate.HOT_DESERT,
      koppen(
        doubleArrayOf(13.0, 15.0, 18.0, 22.0, 27.0, 32.0, 35.0, 34.0, 31.0, 24.0, 17.0, 12.0),
        doubleArrayOf(23.0, 23.0, 25.0, 7.0, 3.0, 1.0, 26.0, 25.0, 18.0, 15.0, 17.0, 23.0),
        33.4,
      ),
    )
    assertEquals(
      Climate.TROPICAL_RAINFOREST,
      koppen(
        DoubleArray(12) { 27.0 },
        doubleArrayOf(234.0, 114.0, 176.0, 154.0, 171.0, 130.0, 158.0, 176.0, 170.0, 194.0, 256.0, 288.0),
        1.35,
      ),
    )
    assertEquals(
      Climate.CONTINENTAL,
      koppen(
        doubleArrayOf(-6.5, -6.7, -1.0, 6.7, 13.0, 17.0, 19.0, 17.0, 11.3, 5.6, -1.2, -5.2),
        doubleArrayOf(52.0, 41.0, 35.0, 37.0, 49.0, 80.0, 85.0, 82.0, 68.0, 71.0, 55.0, 52.0),
        55.8,
      ),
    )
    assertEquals(
      Climate.MEDITERRANEAN,
      koppen(
        doubleArrayOf(8.0, 9.0, 11.0, 14.0, 18.0, 22.0, 25.0, 25.0, 21.0, 17.0, 12.0, 9.0),
        doubleArrayOf(67.0, 73.0, 58.0, 81.0, 53.0, 34.0, 19.0, 37.0, 73.0, 113.0, 111.0, 81.0),
        41.9,
      ),
    )
  }

  @Test
  fun zones_nearestCity() {
    val zones =
      parseZones(
        """
        # comment
        FR,MC	+4852+00220	Europe/Paris
        GB,GG,IM,JE	+513030-0000731	Europe/London
        US	+404251-0740023	America/New_York	Eastern (most areas)
        """
          .trimIndent()
      )
    assertEquals(3, zones.size)
    assertEquals(51.508, zones[1].point.latitude, 0.01)
    assertEquals(-0.125, zones[1].point.longitude, 0.01)
    assertEquals("Europe/Paris", nearestZone(zones, GeoPoint(45.76, 4.84)))
    assertEquals("America/New_York", nearestZone(zones, GeoPoint(42.36, -71.06)))
  }

  @Test
  fun elevation_tiles() {
    val tiles = parseElevationIndex("""{"c":"8-bit","f":[[5,0.5,0,-45,0,-60,15,219,360]]}""")
    val tile = tiles.single()
    assertEquals(-45.0, tile.north)
    assertEquals(-60.0, tile.south)
    assertEquals(0.0, tile.west)
    assertEquals(15.0, tile.east)
    assertEquals(tile, tiles.tileAt(GeoPoint(-50.0, 7.0)))
    assertNull(tiles.tileAt(GeoPoint(50.0, 7.0)))
    assertEquals(200.0, tile.elevation(100))
    assertEquals(0 to 0, tile.pixel(GeoPoint(-45.0, 0.0), 219, 360))
  }

  private fun harmonics(x: Int, y: Int, amplitudes: IntArray, phases: IntArray) =
    Constituent.entries.mapIndexed { i, c -> tideHarmonic(c, amplitudes[i], phases[i], x, y) }

  private fun assertTides(harmonics: List<TideHarmonic>, date: String, offset: String, expected: List<String>) {
    val start = LocalDateTime.parse("${date}T00:00").toInstant(UtcOffset.parse(offset)).toEpochMilliseconds()
    val extremes = tideExtremes(harmonics, start - 3 * MILLIS_PER_HOUR, start + 27 * MILLIS_PER_HOUR)
    for (tide in expected) {
      val isHigh = tide.startsWith("H")
      val want =
        LocalDateTime.parse("${date}T${tide.drop(2)}").toInstant(UtcOffset.parse(offset)).toEpochMilliseconds()
      val closest = extremes.filter { it.isHigh == isHigh }.minBy { abs(it.millis - want) }
      assertNear(want, closest.millis, 65)
    }
  }

  private fun assertNear(expected: Long, actual: Long, minutes: Int) {
    val difference = (actual - expected) / 60_000.0
    assertTrue(abs(difference) <= minutes, "off by $difference minutes")
  }

  private fun utc(text: String): Long = LocalDateTime.parse(text).toInstant(TimeZone.UTC).toEpochMilliseconds()

  private companion object {
    val PARIS = GeoPoint(48.8566, 2.3522)
  }
}
