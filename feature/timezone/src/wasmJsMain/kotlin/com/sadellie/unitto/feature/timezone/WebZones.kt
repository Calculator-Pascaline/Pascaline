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

import kotlinx.browser.window

/**
 * Time zones of the browser (Intl), the web app's replacement for Android's ICU. Times are
 * milliseconds since 1970 (UTC), a zone turns them into a date and an hour, with its summer time.
 */
internal object WebZones {
  /** The browser's zone, like "Europe/Paris". */
  fun userZone(): String = browserUserZone()

  /** Every zone the browser knows. */
  fun all(): List<String> = browserZones().split('\n').filter(String::isNotEmpty)

  /** Whether the browser knows [zone] (an old or a mistyped one doesn't work). */
  fun exists(zone: String): Boolean = browserZoneExists(zone)

  /** City of [zone]: "America/Argentina/Buenos_Aires" is "Buenos Aires". */
  fun city(zone: String): String = zone.substringAfterLast('/').replace('_', ' ')

  /** Name of the zone's time in the app's language, like "Central European Time". */
  fun name(zone: String): String = browserZoneName(zone, language()) ?: zone

  /** Where [zone] is: its continent and city, like "Europe, Paris". */
  fun region(zone: String): String =
    if ('/' in zone) "${zone.substringBefore('/')}, ${city(zone)}" else zone

  /** Date and hour at [millis] in [zone]. */
  fun local(millis: Double, zone: String): LocalTime {
    // Year, month, day, hour and minute, in this order
    val parts = browserLocalParts(millis, zone).split(',').map(String::toInt).iterator()
    return LocalTime(parts.next(), parts.next(), parts.next(), parts.next(), parts.next())
  }

  /** The moment it is [time] in [zone]. Summer time is found again for the new moment. */
  fun millisOf(time: LocalTime, zone: String): Double {
    val asUtc = utc(time.year, time.month - 1, time.day, time.hour, time.minute)
    var millis = asUtc - offsetMillis(asUtc, zone)
    // The offset at the guess can differ from the one at the moment (summer time change)
    millis = asUtc - offsetMillis(millis, zone)
    return millis
  }

  /** How far [zone] is ahead of UTC at [millis]. */
  fun offsetMillis(millis: Double, zone: String): Double {
    val time = local(millis, zone)
    val asUtc = utc(time.year, time.month - 1, time.day, time.hour, time.minute)
    // Seconds and milliseconds don't count, only whole minutes
    return asUtc - (millis - millis.mod(MILLIS_IN_MINUTE))
  }

  /** Like "13:45" or "1:45 PM", as the browser's language writes it. */
  fun formatTime(millis: Double, zone: String, is24Hour: Boolean): String =
    browserFormat(millis, zone, language(), if (is24Hour) "time24" else "time12")

  /** Hours only, minutes only and AM or PM, for the big clock. */
  fun formatHours(millis: Double, zone: String, is24Hour: Boolean): String =
    browserFormat(millis, zone, language(), if (is24Hour) "hours24" else "hours12")

  fun formatMinutes(millis: Double, zone: String): String =
    browserFormat(millis, zone, language(), "minutes").padStart(2, '0')

  fun formatAmPm(millis: Double, zone: String): String =
    browserFormat(millis, zone, language(), "dayPeriod")

  /** Like "Wednesday, 4 November 2026". */
  fun formatDate(millis: Double, zone: String): String =
    browserFormat(millis, zone, language(), "date")

  fun now(): Double = browserNow()

  private fun language(): String = window.navigator.language
}

/** A date and an hour, without a zone. [month] is from 1. */
internal data class LocalTime(
  val year: Int,
  val month: Int,
  val day: Int,
  val hour: Int,
  val minute: Int,
) {
  /** Days from [other]: 1 is the day after. */
  fun daysFrom(other: LocalTime): Int =
    ((utc(year, month - 1, day, 0, 0) - utc(other.year, other.month - 1, other.day, 0, 0)) /
        MILLIS_IN_DAY)
      .toInt()
}

@JsFun("(year, month, day, hour, minute) => Date.UTC(year, month, day, hour, minute)")
private external fun utc(year: Int, month: Int, day: Int, hour: Int, minute: Int): Double

@JsFun("() => Date.now()") private external fun browserNow(): Double

@JsFun(
  "() => { try { return Intl.DateTimeFormat().resolvedOptions().timeZone || 'UTC'; } catch (e) { return 'UTC'; } }"
)
private external fun browserUserZone(): String

@JsFun(
  "() => { try { return Intl.supportedValuesOf('timeZone').join('\\n'); } catch (e) { return 'UTC'; } }"
)
private external fun browserZones(): String

@JsFun(
  "(zone) => { try { new Intl.DateTimeFormat('en', { timeZone: zone }); return true; } catch (e) { return false; } }"
)
private external fun browserZoneExists(zone: String): Boolean

@JsFun(
  """(zone, tag) => {
  try {
    const part = new Intl.DateTimeFormat(tag, { timeZone: zone, timeZoneName: 'longGeneric' })
      .formatToParts(new Date()).find(p => p.type === 'timeZoneName');
    return part ? part.value : null;
  } catch (e) { return null; }
}"""
)
private external fun browserZoneName(zone: String, tag: String): String?

// Numbers in English, any language would write its own digits
@JsFun(
  """(millis, zone) => {
  const parts = {};
  new Intl.DateTimeFormat('en-US', {
    timeZone: zone, hourCycle: 'h23', year: 'numeric', month: 'numeric', day: 'numeric',
    hour: 'numeric', minute: 'numeric'
  }).formatToParts(new Date(millis)).forEach(p => { parts[p.type] = p.value; });
  return [parts.year, parts.month, parts.day, parts.hour, parts.minute].join(',');
}"""
)
private external fun browserLocalParts(millis: Double, zone: String): String

@JsFun(
  """(millis, zone, tag, kind) => {
  const options = { timeZone: zone };
  switch (kind) {
    case 'time24': Object.assign(options, { hour: '2-digit', minute: '2-digit', hourCycle: 'h23' }); break;
    case 'time12': Object.assign(options, { hour: 'numeric', minute: '2-digit', hour12: true }); break;
    case 'hours24': Object.assign(options, { hour: '2-digit', hourCycle: 'h23' }); break;
    case 'hours12': Object.assign(options, { hour: 'numeric', hour12: true }); break;
    case 'minutes': Object.assign(options, { minute: '2-digit' }); break;
    case 'dayPeriod': Object.assign(options, { hour: 'numeric', hour12: true }); break;
    case 'date': Object.assign(options, { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' }); break;
  }
  try {
    const format = new Intl.DateTimeFormat(tag, options);
    const parts = format.formatToParts(new Date(millis));
    const only = type => (parts.find(p => p.type === type) || { value: '' }).value;
    if (kind === 'hours12' || kind === 'hours24') return only('hour');
    if (kind === 'minutes') return only('minute');
    if (kind === 'dayPeriod') return only('dayPeriod');
    return format.format(new Date(millis));
  } catch (e) { return ''; }
}"""
)
private external fun browserFormat(millis: Double, zone: String, tag: String, kind: String): String

private const val MILLIS_IN_MINUTE = 60_000.0
private const val MILLIS_IN_DAY = 86_400_000.0
