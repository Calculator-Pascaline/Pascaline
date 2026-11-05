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

import com.sadellie.unitto.core.common.BrowserLocale
import kotlinx.browser.window

internal actual fun deviceZone(): String = browserZone()

internal actual fun zoneOffsetMillis(zone: String, millis: Long): Long = browserOffset(millis.toDouble(), zone).toLong()

internal actual fun knownZones(): List<String> = browserZones().split('\n').filter { '/' in it }.sorted()

internal actual fun formatTime(millis: Long, zone: String): String =
  browserFormat(millis.toDouble(), zone, window.navigator.language, "time")

internal actual fun formatDate(millis: Long, zone: String): String =
  browserFormat(millis.toDouble(), zone, window.navigator.language, "date")

internal actual fun formatShortDay(millis: Long, zone: String): String =
  browserShortDay(millis.toDouble(), zone, window.navigator.language)

internal actual fun monthName(month: Int): String =
  browserMonth(month - 1, window.navigator.language).replaceFirstChar { it.uppercaseChar() }

// The regions that use feet and °F, like Android's MeasurementSystem.US
internal actual fun placeUsesMetric(): Boolean = BrowserLocale.region() !in setOf("US", "LR", "MM")

@JsFun(
  "() => { try { return Intl.DateTimeFormat().resolvedOptions().timeZone || 'UTC'; } catch (e) { return 'UTC'; } }"
)
private external fun browserZone(): String

@JsFun(
  "() => { try { return Intl.supportedValuesOf('timeZone').join('\\n'); } catch (e) { return 'UTC'; } }"
)
private external fun browserZones(): String

// Offset from the date and hour the zone shows at that moment, read in English digits
@JsFun(
  """(millis, zone) => {
  try {
    const parts = {};
    new Intl.DateTimeFormat('en-US', {
      timeZone: zone, hourCycle: 'h23', year: 'numeric', month: 'numeric', day: 'numeric',
      hour: 'numeric', minute: 'numeric', second: 'numeric'
    }).formatToParts(new Date(millis)).forEach(p => { parts[p.type] = p.value; });
    const asUtc = Date.UTC(+parts.year, +parts.month - 1, +parts.day, +parts.hour, +parts.minute, +parts.second);
    return asUtc - (millis - (millis % 1000 + 1000) % 1000);
  } catch (e) { return 0; }
}"""
)
private external fun browserOffset(millis: Double, zone: String): Double

@JsFun(
  """(millis, zone, tag, kind) => {
  const options = kind === 'time'
    ? { timeZone: zone, hour: 'numeric', minute: '2-digit' }
    : { timeZone: zone, weekday: 'short', day: 'numeric', month: 'short', year: 'numeric' };
  try { return new Intl.DateTimeFormat(tag, options).format(new Date(millis)); }
  catch (e) { return new Date(millis).toISOString(); }
}"""
)
private external fun browserFormat(millis: Double, zone: String, tag: String, kind: String): String

@JsFun(
  """(month, tag) => {
  try { return new Intl.DateTimeFormat(tag, { month: 'long', timeZone: 'UTC' }).format(new Date(Date.UTC(2001, month, 15))); }
  catch (e) { return String(month + 1); }
}"""
)
private external fun browserMonth(month: Int, tag: String): String

@JsFun(
  """(millis, zone, tag) => {
  try { return new Intl.DateTimeFormat(tag, { weekday: 'short', day: 'numeric', timeZone: zone }).format(new Date(millis)); }
  catch (e) { return new Date(millis).toDateString().slice(0, 3); }
}"""
)
private external fun browserShortDay(millis: Double, zone: String, tag: String): String
