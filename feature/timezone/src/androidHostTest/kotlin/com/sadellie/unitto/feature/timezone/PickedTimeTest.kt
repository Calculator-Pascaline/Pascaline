/*
 * Unitto is a calculator for Android
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

package com.sadellie.unitto.feature.timezone

import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.test.Test
import kotlin.test.assertEquals

/** "Select time" on a favorite zone: the hour picked there, as the user's time. */
class PickedTimeTest {
  private val paris = ZoneId.of("Europe/Paris")
  private val newYork = ZoneId.of("America/New_York")

  // Summer: Paris is UTC+2 and New York UTC−4, 6 hours apart (5 with the offsets without summer time)
  @Test
  fun summerTime() {
    val parisTime = ZonedDateTime.of(2026, 7, 15, 10, 0, 0, 0, paris)
    val picked = pickedInUserZone(parisTime, paris, hour = 18, minute = 30, userZone = newYork)
    assertEquals(ZonedDateTime.of(2026, 7, 15, 12, 30, 0, 0, newYork), picked)
  }

  @Test
  fun winterTime() {
    val parisTime = ZonedDateTime.of(2026, 1, 15, 10, 0, 0, 0, paris)
    val picked = pickedInUserZone(parisTime, paris, hour = 18, minute = 30, userZone = newYork)
    assertEquals(ZonedDateTime.of(2026, 1, 15, 12, 30, 0, 0, newYork), picked)
  }

  // Tokyo is a day ahead: 8:00 there is the evening before in New York
  @Test
  fun dayBefore() {
    val tokyo = ZoneId.of("Asia/Tokyo")
    val tokyoTime = ZonedDateTime.of(2026, 7, 16, 3, 0, 0, 0, tokyo)
    val picked = pickedInUserZone(tokyoTime, tokyo, hour = 8, minute = 0, userZone = newYork)
    assertEquals(ZonedDateTime.of(2026, 7, 15, 19, 0, 0, 0, newYork), picked)
  }
}
