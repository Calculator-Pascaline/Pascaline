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

package com.sadellie.unitto.feature.calculator.statistics

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class StatisticsMathTest {
  private fun values(vararg pairs: Pair<Number, Number>) =
    pairs.map { (value, frequency) -> WeightedValue(value.toDouble(), frequency.toDouble()) }

  private fun ones(vararg numbers: Number) = values(*numbers.map { it to 1 }.toTypedArray())

  @Test fun mean() = assertEquals(2.5, statisticsOf(ones(1, 2, 3, 4))!!.mean, 1e-12)

  @Test fun weightedMean() = assertEquals(2.75, statisticsOf(values(2 to 1, 3 to 3))!!.mean, 1e-12)

  // σ² = ((−1.5)² + (−0.5)² + 0.5² + 1.5²) / 4 = 1.25
  @Test fun deviation() = assertEquals(1.25, statisticsOf(ones(1, 2, 3, 4))!!.variance, 1e-12)

  @Test fun sampleDeviation() =
    assertEquals(kotlin.math.sqrt(5.0 / 3), statisticsOf(ones(1, 2, 3, 4))!!.sampleDeviation!!, 1e-12)

  @Test fun noSampleDeviationForOneValue() = assertNull(statisticsOf(ones(7))!!.sampleDeviation)

  @Test fun evenMedian() = assertEquals(2.5, statisticsOf(ones(4, 1, 3, 2))!!.median, 1e-12)

  @Test fun oddMedian() = assertEquals(3.0, statisticsOf(ones(1, 2, 3, 4, 5))!!.median, 1e-12)

  // Smallest value with a quarter of the values at or below it
  @Test fun firstQuartile() = assertEquals(3.0, statisticsOf(ones(1, 2, 3, 4, 5, 6, 7, 8, 9, 10))!!.firstQuartile, 1e-12)

  @Test fun thirdQuartile() = assertEquals(8.0, statisticsOf(ones(1, 2, 3, 4, 5, 6, 7, 8, 9, 10))!!.thirdQuartile, 1e-12)

  @Test
  fun sums() {
    val statistics = statisticsOf(values(2 to 3, 5 to 1))!!
    assertEquals(11.0, statistics.sum, 1e-12)
    assertEquals(37.0, statistics.sumOfSquares, 1e-12)
    assertEquals(4.0, statistics.totalFrequency, 1e-12)
  }

  @Test fun nothingToCount() = assertNull(statisticsOf(values(3 to 0)))

  // Bars include their start and exclude their end: 6 goes into [6; 8[
  @Test
  fun bars() =
    assertEquals(
      listOf(
        HistogramBar(0.0, 2.0, 2.0),
        HistogramBar(2.0, 4.0, 1.0),
        HistogramBar(4.0, 6.0, 1.0),
        HistogramBar(6.0, 8.0, 2.0),
      ),
      histogram(values(0 to 1, 1.5 to 1, 2 to 1, 4 to 1, 6 to 2), start = 0.0, width = 2.0),
    )

  @Test fun roundBarWidth() = assertEquals(2.0, autoBarWidth(ones(0, 17)), 1e-12)

  // 4 / 8 would be 0.5, with empty bars between whole values
  @Test fun wholeValuesBarWidth() = assertEquals(1.0, autoBarWidth(ones(1, 2, 4, 5)), 1e-12)

  @Test fun decimalBarWidth() = assertEquals(0.05, autoBarWidth(ones(0.1, 0.5)), 1e-12)

  @Test fun barsStartOnMultiples() = assertEquals(-4.0, autoBarStart(ones(-3, 9), 2.0), 1e-12)
}
