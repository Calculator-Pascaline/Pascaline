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

package com.sadellie.unitto.feature.calculator.functions

import kotlin.math.abs
import kotlin.math.max

/** A point worth a dot on the graph: a zero, or where two functions meet. */
internal data class KeyPoint(val x: Double, val y: Double, val function: Int)

/** Zeros of every function between [from] and [to], and where each pair of functions meets. */
internal fun keyPoints(functions: List<(Double) -> Double>, from: Double, to: Double): List<KeyPoint> {
  val zeros =
    functions.flatMapIndexed { index, f -> rootsOf(f, from, to).map { KeyPoint(it, 0.0, index) } }
  val meetings =
    functions.indices.flatMap { a ->
      (a + 1 until functions.size).flatMap { b ->
        val f = functions[a]
        val g = functions[b]
        rootsOf({ f(it) - g(it) }, from, to).map { KeyPoint(it, f(it), a) }
      }
    }
  return (zeros + meetings).take(MAX_POINTS)
}

/**
 * Roots of [f] between [from] and [to]: where it changes sign, and where it only touches 0 like x²
 * does. A sign change where the values blow up is an asymptote, like tan(x), not a root.
 */
internal fun rootsOf(f: (Double) -> Double, from: Double, to: Double): List<Double> {
  val step = (to - from) / SAMPLES
  val xs = List(SAMPLES + 1) { from + it * step }
  val ys = xs.map(f)
  val roots = mutableListOf<Double>()
  for (i in 1..SAMPLES) {
    val before = ys[i - 1]
    val after = ys[i]
    val root =
      when {
        !before.isFinite() || !after.isFinite() -> null
        before == 0.0 -> xs[i - 1]
        before * after < 0 -> bisect(f, xs[i - 1], xs[i]).takeIf { abs(f(it)) < closeToZero(before, after) }
        i < SAMPLES && touchesZero(before, after, ys[i + 1]) -> lowestPoint(f, xs[i - 1], xs[i + 1])
        else -> null
      }
    if (root != null && roots.none { abs(it - root) < step }) roots += root
  }
  return roots
}

/** The middle one is the smallest in size, with the same sign and close to 0. */
private fun touchesZero(before: Double, middle: Double, after: Double): Boolean =
  after.isFinite() && before * middle > 0 && middle * after > 0 && abs(middle) < abs(before) &&
    abs(middle) <= abs(after) && abs(middle) < TOUCH_RANGE * max(abs(before), abs(after))

private fun closeToZero(before: Double, after: Double) = ROOT_TOLERANCE * max(1.0, max(abs(before), abs(after)))

private fun bisect(f: (Double) -> Double, from: Double, to: Double): Double {
  var low = from
  var high = to
  val isRising = f(low) < 0
  repeat(ITERATIONS) {
    val middle = (low + high) / 2
    if ((f(middle) < 0) == isRising) low = middle else high = middle
  }
  return (low + high) / 2
}

/** Where |[f]| is smallest between [from] and [to], or null when it doesn't reach 0. */
private fun lowestPoint(f: (Double) -> Double, from: Double, to: Double): Double? {
  var low = from
  var high = to
  repeat(ITERATIONS) {
    val third = (high - low) / 3
    if (abs(f(low + third)) < abs(f(high - third))) high -= third else low += third
  }
  val x = (low + high) / 2
  return x.takeIf { abs(f(it)) < TOUCH_TOLERANCE }
}

private const val SAMPLES = 400
private const val ITERATIONS = 60
private const val MAX_POINTS = 60
private const val ROOT_TOLERANCE = 1e-6
private const val TOUCH_TOLERANCE = 1e-9
private const val TOUCH_RANGE = 0.5
