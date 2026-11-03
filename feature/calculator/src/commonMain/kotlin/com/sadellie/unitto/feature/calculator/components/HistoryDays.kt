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

package com.sadellie.unitto.feature.calculator.components

import com.sadellie.unitto.core.model.calculator.CalculatorHistoryItem
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

/** Row of a history list: a calculation or the day of calculations above it. */
internal sealed interface HistoryRow<out T> {
  val key: Any

  data class Calculation<T>(val item: T, override val key: Any) : HistoryRow<T>

  data class Day(val date: LocalDate) : HistoryRow<Nothing> {
    override val key: Any
      get() = "day_$date"
  }
}

/**
 * History (newest first) with a [HistoryRow.Day] after the oldest calculation of each day. The list
 * is shown reversed, so the day is above its calculations. [timestamp] is in milliseconds.
 */
internal fun <T> List<T>.withDays(
  timeZone: TimeZone,
  timestamp: (T) -> Long,
  key: (T) -> Any,
): List<HistoryRow<T>> {
  fun T.date(): LocalDate = Instant.fromEpochMilliseconds(timestamp(this)).toLocalDateTime(timeZone).date

  val rows = mutableListOf<HistoryRow<T>>()
  forEachIndexed { index, item ->
    rows += HistoryRow.Calculation(item, key(item))
    val date = item.date()
    if (getOrNull(index + 1)?.date() != date) rows += HistoryRow.Day(date)
  }
  return rows
}

internal fun List<CalculatorHistoryItem>.withDays(timeZone: TimeZone): List<HistoryRow<CalculatorHistoryItem>> =
  withDays(timeZone, { it.timestamp }, { it.id })
