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

// Time zones and how dates and hours are written, from Android or from the browser (Intl).

/** The device's or browser's zone, like "Europe/Paris". */
internal expect fun deviceZone(): String

/** How far [zone] is ahead of UTC at [millis]. */
internal expect fun zoneOffsetMillis(zone: String, millis: Long): Long

/** Every zone the platform knows. */
internal expect fun knownZones(): List<String>

/** Like "21:58" or "9:58 PM" in the person's language. */
internal expect fun formatTime(millis: Long, zone: String): String

/** Like "Sun, 21 Jun 2026" in the person's language. */
internal expect fun formatDate(millis: Long, zone: String): String

/** Short weekday and day, like Fri 6. */
internal expect fun formatShortDay(millis: Long, zone: String): String

/** Full name of [month] (1 is January) in the person's language. */
internal expect fun monthName(month: Int): String

/** Whether the person's region uses meters and °C. */
internal expect fun placeUsesMetric(): Boolean
