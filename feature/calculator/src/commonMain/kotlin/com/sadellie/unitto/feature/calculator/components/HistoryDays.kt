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

/** Row of the history list: a calculation or the day of calculations above it. */
internal sealed interface HistoryRow {
  val key: Any

  data class Calculation(val item: CalculatorHistoryItem) : HistoryRow {
    override val key: Any
      get() = item.id
  }

  data class Day(val date: LocalDate) : HistoryRow {
    override val key: Any
      get() = "day_$date"
  }
}

/**
 * History (newest first) with a [HistoryRow.Day] after the oldest calculation of each day. The list
 * is shown reversed, so the day is above its calculations.
 */
internal fun List<CalculatorHistoryItem>.withDays(timeZone: TimeZone): List<HistoryRow> {
  val rows = mutableListOf<HistoryRow>()
  forEachIndexed { index, item ->
    rows += HistoryRow.Calculation(item)
    val date = item.date(timeZone)
    if (getOrNull(index + 1)?.date(timeZone) != date) rows += HistoryRow.Day(date)
  }
  return rows
}

private fun CalculatorHistoryItem.date(timeZone: TimeZone): LocalDate =
  Instant.fromEpochMilliseconds(timestamp).toLocalDateTime(timeZone).date
