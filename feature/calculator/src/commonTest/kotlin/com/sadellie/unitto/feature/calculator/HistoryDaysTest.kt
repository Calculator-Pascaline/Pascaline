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

package com.sadellie.unitto.feature.calculator

import com.sadellie.unitto.core.model.calculator.CalculatorHistoryItem
import com.sadellie.unitto.feature.calculator.components.HistoryRow
import com.sadellie.unitto.feature.calculator.components.withDays
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals

class HistoryDaysTest {
  private fun item(id: Int, isoTime: String) =
    CalculatorHistoryItem(id, "1+1", "2", kotlin.time.Instant.parse(isoTime).toEpochMilliseconds())

  @Test
  fun dayAfterOldestOfEachDay() {
    val newest = item(3, "2026-11-02T20:00:00Z")
    val sameDay = item(2, "2026-11-02T08:00:00Z")
    val dayBefore = item(1, "2026-11-01T23:00:00Z")
    val rows = listOf(newest, sameDay, dayBefore).withDays(TimeZone.UTC)
    assertEquals(
      listOf(
        HistoryRow.Calculation(newest),
        HistoryRow.Calculation(sameDay),
        HistoryRow.Day(LocalDate(2026, 11, 2)),
        HistoryRow.Calculation(dayBefore),
        HistoryRow.Day(LocalDate(2026, 11, 1)),
      ),
      rows,
    )
  }

  @Test fun empty() = assertEquals(emptyList(), emptyList<CalculatorHistoryItem>().withDays(TimeZone.UTC))
}
