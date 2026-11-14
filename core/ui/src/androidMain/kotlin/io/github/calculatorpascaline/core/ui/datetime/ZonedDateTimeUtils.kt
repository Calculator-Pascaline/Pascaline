/*
 * Unitto is a calculator for Android
 * Copyright (c) 2023-2025 Elshan Agaev
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

package io.github.calculatorpascaline.core.ui.datetime

import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.absoluteValue

/**
 * Formats date time into something like:
 *
 * 23:59 or 11:59 AM
 *
 * Depends on system preferences
 *
 * @return Formatted string
 * @see PascalineDateTimeFormatter.time24
 * @see PascalineDateTimeFormatter.time12Full
 */
fun ZonedDateTime.formatTime(locale: Locale, is24Hour: Boolean): String =
  if (is24Hour) {
    format(PascalineDateTimeFormatter.time24(locale))
  } else {
    format(PascalineDateTimeFormatter.time12Full(locale))
  }

/**
 * Formats date time into something like:
 *
 * 23 or 11
 *
 * Depends on system preferences
 *
 * @return Formatted string
 * @see PascalineDateTimeFormatter.time24Hours
 * @see PascalineDateTimeFormatter.time12Hours
 */
fun ZonedDateTime.formatTimeHours(locale: Locale, is24Hour: Boolean): String =
  if (is24Hour) {
    format(PascalineDateTimeFormatter.time24Hours(locale))
  } else {
    format(PascalineDateTimeFormatter.time12Hours(locale))
  }

/** @see PascalineDateTimeFormatter.timeMinutes */
fun ZonedDateTime.formatTimeMinutes(locale: Locale): String =
  format(PascalineDateTimeFormatter.timeMinutes(locale))

/** @see PascalineDateTimeFormatter.time12AmPm */
fun ZonedDateTime.formatTimeAmPm(locale: Locale): String =
  format(PascalineDateTimeFormatter.time12AmPm(locale))

/** @see PascalineDateTimeFormatter.zone */
fun ZonedDateTime.formatZone(locale: Locale): String = format(PascalineDateTimeFormatter.zone(locale))

/** @see PascalineDateTimeFormatter.dateDayMonthYear */
fun ZonedDateTime.formatDateDayMonthYear(locale: Locale): String =
  format(PascalineDateTimeFormatter.dateDayMonthYear(locale))

fun ZonedDateTime.formatOffset(
  currentTime: ZonedDateTime,
  hourShort: String,
  minuteLabel: String,
  tomorrowLabel: String,
  yesterdayLabel: String,
): String? {
  val offsetFixed = ChronoUnit.SECONDS.between(currentTime, this)
  var resultBuffer = ""

  // Add a positive/negative prefix symbol
  resultBuffer +=
    when {
      offsetFixed > 0 -> "+"
      offsetFixed < 0 -> "-"
      else -> return null
    }

  val absoluteOffset = offsetFixed.absoluteValue

  // Formatted hours and minutes
  val hour = absoluteOffset / 3600
  val minute = absoluteOffset % 3600 / 60

  if (hour != 0L) {
    resultBuffer += "${hour}${hourShort}"
  }

  if (minute != 0L) {
    if (hour != 0L) resultBuffer += " "
    resultBuffer += "${minute}${minuteLabel}"
  }

  // Day after time string
  val diff = this.dayOfYear - currentTime.dayOfYear
  when {
    diff > 0 -> resultBuffer += ", ${tomorrowLabel.lowercase()}"
    diff < 0 -> resultBuffer += ", ${yesterdayLabel.lowercase()}"
  }

  return resultBuffer
}
