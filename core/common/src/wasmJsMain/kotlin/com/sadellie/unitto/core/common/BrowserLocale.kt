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

package com.sadellie.unitto.core.common

/** What the browser's language says, read with the browser's own Intl. */
object BrowserLocale {
  /** Region of the browser's language, guessed when only the language is set ("fr" is France). */
  fun region(): String? = browserRegion()

  /** Whether the browser's language writes 13:00 rather than 1 PM. */
  fun is24Hour(): Boolean = browserIs24Hour()

  /** Short weekday names from Monday, null when the browser doesn't know [languageTag]. */
  fun weekdays(languageTag: String): List<String>? = names(languageTag, "weekday", DAYS_IN_WEEK)

  /** Short month names from January. */
  fun months(languageTag: String): List<String>? = names(languageTag, "month", MONTHS_IN_YEAR)

  /** What [languageTag] writes before or after the hour, like AM and PM. */
  fun amPm(languageTag: String): Pair<String, String>? {
    val names = names(languageTag, "dayPeriod", 2) ?: return null
    return names[0] to names[1]
  }

  /** How [languageTag] writes the currency [code] (ISO 4217), like "€" for EUR. */
  fun currencySymbol(languageTag: String, code: String): String? =
    browserCurrencySymbol(languageTag, code)

  private fun names(languageTag: String, kind: String, count: Int): List<String>? =
    browserNames(languageTag, kind)?.split('\n')?.takeIf { it.size == count && it.none(String::isEmpty) }
}

@JsFun(
  """() => {
  try { return new Intl.Locale(navigator.language).maximize().region || null; } catch (e) { return null; }
}"""
)
private external fun browserRegion(): String?

@JsFun(
  """() => {
  try { return !new Intl.DateTimeFormat(navigator.language, { hour: 'numeric' }).resolvedOptions().hour12; } catch (e) { return true; }
}"""
)
private external fun browserIs24Hour(): Boolean

// 1 January 2024 is a Monday, the hours are 1 AM and 1 PM
@JsFun(
  """(tag, kind) => {
  try {
    const dates = [];
    if (kind === 'weekday') for (let i = 0; i < 7; i++) dates.push(new Date(Date.UTC(2024, 0, 1 + i, 12)));
    if (kind === 'month') for (let i = 0; i < 12; i++) dates.push(new Date(Date.UTC(2024, i, 15, 12)));
    if (kind === 'dayPeriod') dates.push(new Date(Date.UTC(2024, 0, 1, 1)), new Date(Date.UTC(2024, 0, 1, 13)));
    const options = kind === 'dayPeriod'
      ? { hour: 'numeric', hour12: true, timeZone: 'UTC' }
      : { [kind]: 'short', timeZone: 'UTC' };
    const format = new Intl.DateTimeFormat(tag, options);
    return dates.map(date => {
      if (kind !== 'dayPeriod') return format.format(date);
      const part = format.formatToParts(date).find(p => p.type === 'dayPeriod');
      return part ? part.value : '';
    }).join('\n');
  } catch (e) { return null; }
}"""
)
private external fun browserNames(tag: String, kind: String): String?

@JsFun(
  """(tag, code) => {
  try {
    const part = new Intl.NumberFormat(tag, { style: 'currency', currency: code })
      .formatToParts(0).find(p => p.type === 'currency');
    return part ? part.value : null;
  } catch (e) { return null; }
}"""
)
private external fun browserCurrencySymbol(tag: String, code: String): String?

private const val DAYS_IN_WEEK = 7
private const val MONTHS_IN_YEAR = 12
