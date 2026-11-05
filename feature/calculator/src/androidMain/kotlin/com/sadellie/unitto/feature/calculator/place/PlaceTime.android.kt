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

import android.icu.util.LocaleData
import android.icu.util.ULocale
import android.os.Build
import java.text.DateFormatSymbols
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

internal actual fun deviceZone(): String = TimeZone.getDefault().id

internal actual fun zoneOffsetMillis(zone: String, millis: Long): Long =
  TimeZone.getTimeZone(zone).getOffset(millis).toLong()

internal actual fun knownZones(): List<String> = TimeZone.getAvailableIDs().filter { '/' in it }.sorted()

internal actual fun formatTime(millis: Long, zone: String): String =
  format(java.text.DateFormat.getTimeInstance(java.text.DateFormat.SHORT, Locale.getDefault()), millis, zone)

internal actual fun formatDate(millis: Long, zone: String): String {
  val pattern = android.text.format.DateFormat.getBestDateTimePattern(Locale.getDefault(), "EEEdMMMyyyy")
  return format(SimpleDateFormat(pattern, Locale.getDefault()), millis, zone)
}

internal actual fun formatShortDay(millis: Long, zone: String): String {
  val pattern = android.text.format.DateFormat.getBestDateTimePattern(Locale.getDefault(), "EEEd")
  return format(SimpleDateFormat(pattern, Locale.getDefault()), millis, zone)
}

private fun format(format: java.text.DateFormat, millis: Long, zone: String): String {
  format.timeZone = TimeZone.getTimeZone(zone)
  return format.format(Date(millis))
}

internal actual fun monthName(month: Int): String =
  DateFormatSymbols.getInstance(Locale.getDefault()).months[month - 1].replaceFirstChar { it.titlecase(Locale.getDefault()) }

internal actual fun placeUsesMetric(): Boolean {
  if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return true
  return LocaleData.getMeasurementSystem(ULocale.getDefault()) != LocaleData.MeasurementSystem.US
}
