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

package io.github.calculatorpascaline.feature.calculator.gears

import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.pow

/** A gear with [driver] teeth turning one with [driven] teeth: it slows down by [driven] / [driver]. */
internal data class Stage(val driver: Int, val driven: Int) {
  val ratio: Double
    get() = driven.toDouble() / driver

  /**
   * Teeth that share no common factor meet every tooth of the other gear in turn, which spreads the wear: a hunting
   * tooth pair.
   */
  val hunting: Boolean
    get() = gcd(driver, driven) == 1
}

/** A train found for a wanted ratio: its [stages] one after the other, each on the shaft of the stage before. */
internal data class Suggestion(val stages: List<Stage>, val target: Double) {
  val ratio: Double
    get() = stages.fold(1.0) { product, stage -> product * stage.ratio }

  /** How far from the wanted ratio, as a share of it. */
  val error: Double
    get() = abs(ratio - target) / target

  val hunting: Boolean
    get() = stages.all { it.hunting }

  val teeth: Int
    get() = stages.sumOf { it.driver + it.driven }
}

/**
 * The best trains of [stages] stages, from [minTeeth] to [maxTeeth] teeth on every gear, that give [target]: the input
 * speed over the output speed. The closest come first, then those that wear evenly, then the smallest. Every stage
 * gets a fair share of the ratio, so that no gear is huge next to a tiny one.
 */
internal fun findTrains(target: Double, stages: Int, minTeeth: Int, maxTeeth: Int, count: Int): List<Suggestion> {
  val possible = target > 0 && stages in 1..MAX_STAGES
  if (!possible || minTeeth < MIN_TEETH || maxTeeth < minTeeth) return emptyList()
  val pairs =
    (minTeeth..maxTeeth)
      .flatMap { driver -> (minTeeth..maxTeeth).map { driven -> Stage(driver, driven) } }
      .sortedBy { it.ratio }
  val ratios = pairs.map { it.ratio }.toDoubleArray()
  // Each stage close to the same share: the n-th root of the target, give or take a factor
  val share = target.pow(1.0 / stages)
  val fair = share / SHARE_SPREAD..share * SHARE_SPREAD
  val band = pairs.filter { it.ratio in fair }
  val best = Best(target)
  // The last stage is the one closest to what the others leave, among the fair shares
  fun finish(chosen: List<Stage>, product: Double) {
    nearest(ratios, target / product)
      .map { pairs[it] }
      .filter { stages == 1 || it.ratio in fair }
      .forEach { best.offer(chosen + it, product * it.ratio) }
  }
  when (stages) {
    1 -> finish(emptyList(), 1.0)
    2 -> band.forEach { finish(listOf(it), it.ratio) }
    else -> band.forEach { first -> band.forEach { second -> finish(listOf(first, second), first.ratio * second.ratio) } }
  }
  return best.sorted().distinctBy { suggestion -> suggestion.stages.map { it.ratio }.sorted() }.take(count)
}

/** The best trains found so far: only those are kept, so that wide ranges of teeth don't fill the memory. */
private class Best(private val target: Double) {
  private val order = compareBy<Suggestion>({ rounded(it.error) }, { !it.hunting }, { it.teeth }, { spread(it) })
  private var found = mutableListOf<Suggestion>()
  private var worstKept = Double.MAX_VALUE

  /** Keeps [stages] that give [ratio] when it is closer than the worst kept. */
  fun offer(stages: List<Stage>, ratio: Double) {
    if (rounded(abs(ratio - target) / target) > worstKept) return
    found += Suggestion(stages, target)
    if (found.size > KEEP_WHILE_SEARCHING * 2) {
      found = found.sortedWith(order).take(KEEP_WHILE_SEARCHING).toMutableList()
      worstKept = rounded(found.last().error)
    }
  }

  fun sorted(): List<Suggestion> = found.sortedWith(order)
}

/** The indexes of the values of [sorted] just below and above [value]. */
private fun nearest(sorted: DoubleArray, value: Double): List<Int> {
  var low = 0
  var high = sorted.size
  while (low < high) {
    val middle = (low + high) / 2
    if (sorted[middle] < value) low = middle + 1 else high = middle
  }
  return listOf(low - 1, low).filter { it in sorted.indices }
}

/** Errors this close count as the same, so that the next rules decide. */
private fun rounded(error: Double): Double = kotlin.math.round(error / SAME_ERROR) * SAME_ERROR

/** How uneven the stages are: 0 when they all slow down as much. */
private fun spread(suggestion: Suggestion): Double {
  val logs = suggestion.stages.map { ln(it.ratio) }
  return logs.maxOf { it } - logs.minOf { it }
}

private tailrec fun gcd(a: Int, b: Int): Int = if (b == 0) a else gcd(b, a % b)

internal const val MAX_STAGES = 3
private const val SHARE_SPREAD = 1.6
private const val SAME_ERROR = 1e-6
private const val KEEP_WHILE_SEARCHING = 400
