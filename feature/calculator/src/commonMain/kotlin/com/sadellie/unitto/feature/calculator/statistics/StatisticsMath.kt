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

import com.sadellie.unitto.feature.calculator.functions.niceStep
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.sqrt

/** Value and how many times it was seen. Frequencies don't have to be whole. */
internal data class WeightedValue(val value: Double, val frequency: Double)

/**
 * Same values as the Stats page of the NumWorks Statistics app.
 *
 * @property deviation Population standard deviation σ.
 * @property sampleDeviation Sample standard deviation s, null for a total frequency of 1 or less.
 */
internal class Statistics(
  val totalFrequency: Double,
  val minimum: Double,
  val maximum: Double,
  val mean: Double,
  val deviation: Double,
  val variance: Double,
  val sampleDeviation: Double?,
  val firstQuartile: Double,
  val median: Double,
  val thirdQuartile: Double,
  val sum: Double,
  val sumOfSquares: Double,
) {
  val range: Double
    get() = maximum - minimum

  val interquartileRange: Double
    get() = thirdQuartile - firstQuartile
}

/** Statistics of [values], null when there is nothing to count (total frequency is 0). */
internal fun statisticsOf(values: List<WeightedValue>): Statistics? {
  val data = values.filter { it.frequency > 0 && it.value.isFinite() && it.frequency.isFinite() }.sortedBy { it.value }
  val total = data.sumOf { it.frequency }
  if (data.isEmpty() || total <= 0) return null

  val sum = data.sumOf { it.value * it.frequency }
  val mean = sum / total
  val squaredDistances = data.sumOf { (it.value - mean) * (it.value - mean) * it.frequency }
  val variance = squaredDistances / total
  return Statistics(
    totalFrequency = total,
    minimum = data.first().value,
    maximum = data.last().value,
    mean = mean,
    deviation = sqrt(variance),
    variance = variance,
    sampleDeviation = if (total > 1) sqrt(squaredDistances / (total - 1)) else null,
    firstQuartile = quantile(data, total, QUARTER),
    median = median(data, total),
    thirdQuartile = quantile(data, total, THREE_QUARTERS),
    sum = sum,
    sumOfSquares = data.sumOf { it.value * it.value * it.frequency },
  )
}

/** Smallest value with at least [share] of the total frequency at or below it, like NumWorks. */
private fun quantile(sorted: List<WeightedValue>, total: Double, share: Double): Double {
  var cumulated = 0.0
  for (item in sorted) {
    cumulated += item.frequency
    if (cumulated >= total * share - SAME_FREQUENCY) return item.value
  }
  return sorted.last().value
}

/**
 * Middle value. When exactly half of the frequency is at or below a value, it is the mean of that
 * value and the next one: 1, 2, 3, 4 ==> 2.5.
 */
private fun median(sorted: List<WeightedValue>, total: Double): Double {
  val half = total / 2
  var cumulated = 0.0
  for ((index, item) in sorted.withIndex()) {
    cumulated += item.frequency
    if (abs(cumulated - half) < SAME_FREQUENCY) {
      val next = sorted.getOrNull(index + 1) ?: return item.value
      return (item.value + next.value) / 2
    }
    if (cumulated > half) return item.value
  }
  return sorted.last().value
}

/** Bar of a histogram: values from [start] (included) to [end] (excluded). */
internal data class HistogramBar(val start: Double, val end: Double, val frequency: Double)

/** Bars of [width] from [start] that cover all values, like `[0; 2[`, `[2; 4[`. */
internal fun histogram(values: List<WeightedValue>, start: Double, width: Double): List<HistogramBar> {
  val data = values.filter { it.frequency > 0 && it.value >= start }
  if (data.isEmpty() || width <= 0 || !width.isFinite()) return emptyList()
  val maximum = data.maxOf { it.value }
  val count = (floor((maximum - start) / width).toInt() + 1).coerceIn(1, MAX_BARS)
  val frequencies = DoubleArray(count)
  data.forEach { item ->
    val index = floor((item.value - start) / width).toInt().coerceAtMost(count - 1)
    if (index >= 0) frequencies[index] += item.frequency
  }
  return (0 until count).map { HistogramBar(start + it * width, start + (it + 1) * width, frequencies[it]) }
}

/**
 * About [BARS] bars of a round width: 1, 2 or 5 times a power of ten. Whole values get at least 1,
 * so that there are no empty bars between them.
 */
internal fun autoBarWidth(values: List<WeightedValue>): Double {
  val data = values.filter { it.frequency > 0 }
  if (data.isEmpty()) return 1.0
  val range = data.maxOf { it.value } - data.minOf { it.value }
  val width = if (range <= 0) 1.0 else niceStep(range / BARS)
  val areWhole = data.all { it.value == floor(it.value) }
  return if (areWhole) maxOf(width, 1.0) else width
}

/** Round start just at or below the minimum, so that bars start on multiples of [width]. */
internal fun autoBarStart(values: List<WeightedValue>, width: Double): Double {
  val minimum = values.filter { it.frequency > 0 }.minOfOrNull { it.value } ?: return 0.0
  return floor(minimum / width) * width
}

private const val QUARTER = 0.25
private const val THREE_QUARTERS = 0.75
private const val SAME_FREQUENCY = 1e-9
private const val BARS = 8.0
private const val MAX_BARS = 500
