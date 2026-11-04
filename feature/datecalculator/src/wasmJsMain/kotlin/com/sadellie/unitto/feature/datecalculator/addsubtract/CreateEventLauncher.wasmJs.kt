/*
 * Unitto is a calculator for Android
 * Copyright (c) 2026 Elshan Agaev
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

package com.sadellie.unitto.feature.datecalculator.addsubtract

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.sadellie.unitto.core.common.WebFiles
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/** The browser has no calendar: the event is saved as a calendar file that calendar apps open. */
@Composable
actual fun rememberCreateEventLauncher(): CreateEventLauncher = remember {
  object : CreateEventLauncher {
    override fun launch(start: Instant, end: Instant) {
      val now = Clock.System.now()
      val event =
        listOf(
            "BEGIN:VCALENDAR",
            "VERSION:2.0",
            "PRODID:-//Calculator-Pascaline//Pascaline//EN",
            "BEGIN:VEVENT",
            "UID:${now.toEpochMilliseconds()}@pascaline",
            "DTSTAMP:${now.toIcs()}",
            "DTSTART:${start.toIcs()}",
            "DTEND:${end.toIcs()}",
            "TRANSP:OPAQUE",
            "END:VEVENT",
            "END:VCALENDAR",
          )
          // The format wants CRLF line endings
          .joinToString("\r\n", postfix = "\r\n")
      WebFiles.save("event.ics", "text/calendar", event)
    }
  }
}

/** UTC time as calendar files write it, like 20261104T134500Z. */
private fun Instant.toIcs(): String {
  val time = toLocalDateTime(TimeZone.UTC)
  fun Int.twoDigits() = toString().padStart(2, '0')
  return "${time.year.toString().padStart(YEAR_DIGITS, '0')}${time.month.ordinal.plus(1).twoDigits()}" +
    "${time.day.twoDigits()}T${time.hour.twoDigits()}${time.minute.twoDigits()}" +
    "${time.second.twoDigits()}Z"
}

private const val YEAR_DIGITS = 4
