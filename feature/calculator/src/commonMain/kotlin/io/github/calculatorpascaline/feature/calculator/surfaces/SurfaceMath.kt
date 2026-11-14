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

package io.github.calculatorpascaline.feature.calculator.surfaces

import io.github.calculatorpascaline.feature.calculator.integrals.MultiFunction
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max

/** Where the graph is drawn: xLow ≤ x ≤ xHigh and yLow ≤ y ≤ yHigh. */
internal class Region(val xLow: Double, val xHigh: Double, val yLow: Double, val yHigh: Double) {
  val isValid: Boolean
    get() = listOf(xLow, xHigh, yLow, yHigh).all { it.isFinite() } && xHigh > xLow && yHigh > yLow

  val span: Double
    get() = max(xHigh - xLow, yHigh - yLow)

  fun x(i: Int, steps: Int) = xLow + (xHigh - xLow) * i / steps

  fun y(j: Int, steps: Int) = yLow + (yHigh - yLow) * j / steps

  fun contains(x: Double, y: Double): Boolean {
    val margin = span * INSIDE_MARGIN
    return x in xLow - margin..xHigh + margin && y in yLow - margin..yHigh + margin
  }
}

internal enum class PointKind {
  MAXIMUM,
  MINIMUM,
  SADDLE,

  /** D = 0: the test can't tell, like x⁴ + y⁴ at 0. */
  FLAT,
}

/**
 * Second derivatives of f at a point.
 *
 * @property determinant D = fxx·fyy − fxy²
 */
internal class Hessian(val fxx: Double, val fyy: Double, val fxy: Double) {
  val determinant: Double
    get() = fxx * fyy - fxy * fxy
}

/** A point where ∇f = 0, with the second derivatives of the test. */
internal class CriticalPoint(val x: Double, val y: Double, val z: Double, val hessian: Hessian, val kind: PointKind)

/** Values of f on a grid of [steps] × [steps] cells, with the lowest and highest ones. */
internal class SurfaceGrid(val region: Region, val steps: Int, val values: Array<DoubleArray>) {
  private val finite = values.flatMap { row -> row.filter { it.isFinite() } }
  val low: Double = finite.minOrNull() ?: 0.0
  val high: Double = finite.maxOrNull() ?: 0.0

  /** From 0 at the lowest to 1 at the highest. */
  fun share(value: Double) = if (high > low) ((value - low) / (high - low)).coerceIn(0.0, 1.0) else HALF
}

/** f on the grid, with values far above or below the rest cut, so that 1/x still shows. */
internal fun MultiFunction.grid(region: Region, steps: Int): SurfaceGrid {
  val raw = Array(steps + 1) { i -> DoubleArray(steps + 1) { j -> valueAt(region.x(i, steps), region.y(j, steps)) } }
  val sorted = raw.flatMap { row -> row.filter { it.isFinite() } }.sorted()
  if (sorted.isEmpty()) return SurfaceGrid(region, steps, raw)
  val low = sorted[(sorted.size * CUT).toInt()]
  val high = sorted[((sorted.size - 1) * (1 - CUT)).toInt()]
  val room = (high - low).takeIf { it > 0 } ?: 1.0
  val clamped = Array(steps + 1) { i -> DoubleArray(steps + 1) { j -> raw[i][j].takeIf { it.isFinite() }?.coerceIn(low - room, high + room) ?: Double.NaN } }
  return SurfaceGrid(region, steps, clamped)
}

/**
 * Points of the region where ∇f = 0. Each place where |∇f| is smaller than around it on a grid is
 * a start for Newton's method on ∇f = 0, then the Hessian test tells what the point is.
 */
internal fun MultiFunction.criticalPoints(region: Region): List<CriticalPoint> {
  val derivatives = Derivatives(this, region)
  val steps = SEARCH_STEPS
  val slopes = Array(steps + 1) { i -> DoubleArray(steps + 1) { j -> derivatives.slope(region.x(i, steps), region.y(j, steps)) } }
  val starts =
    (0..steps).flatMap { i -> (0..steps).map { j -> i to j } }.filter { (i, j) ->
      val slope = slopes[i][j]
      slope.isFinite() &&
        (-1..1).all { di -> (-1..1).all { dj -> slopes.getOrNull(i + di)?.getOrNull(j + dj)?.let { it.isNaN() || it >= slope } ?: true } }
    }
  val found = mutableListOf<CriticalPoint>()
  for ((i, j) in starts) {
    val point = derivatives.newton(region.x(i, steps), region.y(j, steps)) ?: continue
    val isNew = found.none { hypot(it.x - point.x, it.y - point.y) < region.span * SAME_POINT }
    if (isNew && found.size < MAX_POINTS) found += point
  }
  return found.sortedWith(compareBy({ it.kind.ordinal }, { it.x }, { it.y }))
}

/** Derivatives of f from its values close by, with steps that follow the size of the region. */
private class Derivatives(private val f: MultiFunction, private val region: Region) {
  private val h = region.span * FIRST_STEP
  private val h2 = region.span * SECOND_STEP

  fun fx(x: Double, y: Double) = (f.valueAt(x + h, y) - f.valueAt(x - h, y)) / (2 * h)

  fun fy(x: Double, y: Double) = (f.valueAt(x, y + h) - f.valueAt(x, y - h)) / (2 * h)

  fun slope(x: Double, y: Double) = hypot(fx(x, y), fy(x, y))

  fun fxx(x: Double, y: Double) = (f.valueAt(x + h2, y) - 2 * f.valueAt(x, y) + f.valueAt(x - h2, y)) / (h2 * h2)

  fun fyy(x: Double, y: Double) = (f.valueAt(x, y + h2) - 2 * f.valueAt(x, y) + f.valueAt(x, y - h2)) / (h2 * h2)

  fun fxy(x: Double, y: Double) =
    (f.valueAt(x + h2, y + h2) - f.valueAt(x + h2, y - h2) - f.valueAt(x - h2, y + h2) + f.valueAt(x - h2, y - h2)) / (CORNERS * h2 * h2)

  /** ∇f = 0 from (x, y) by Newton's method, null when it doesn't get there inside the region. */
  fun newton(startX: Double, startY: Double): CriticalPoint? {
    val (x, y) = steps(startX, startY) ?: return null
    val z = f.valueAt(x, y)
    val scale = max(1.0, abs(z)) / region.span
    val hessian = Hessian(fxx(x, y), fyy(x, y), fxy(x, y))
    val isPoint = z.isFinite() && region.contains(x, y) && slope(x, y) <= scale * FLAT_SLOPE
    return if (isPoint) CriticalPoint(snap(x), snap(y), z, hessian, kind(hessian, scale / region.span)) else null
  }

  /** Where Newton's method ends, null when it leaves the region. */
  private fun steps(startX: Double, startY: Double): Pair<Double, Double>? {
    var x = startX
    var y = startY
    repeat(MAX_STEPS) {
      val gx = fx(x, y)
      val gy = fy(x, y)
      val a = fxx(x, y)
      val b = fxy(x, y)
      val c = fyy(x, y)
      val det = a * c - b * b
      if (!det.isFinite() || abs(det) < TINY) return@repeat
      val dx = (-gx * c + gy * b) / det
      val dy = (-gy * a + gx * b) / det
      x += dx
      y += dy
      if (!region.contains(x, y)) return null
      if (hypot(dx, dy) < region.span * DONE) return@repeat
    }
    return x to y
  }

  private fun kind(hessian: Hessian, scale: Double): PointKind {
    val size = maxOf(abs(hessian.fxx), abs(hessian.fyy), abs(hessian.fxy))
    val det = hessian.determinant
    return when {
      size < scale * FLAT_SLOPE || abs(det) < size * size * FLAT_TEST -> PointKind.FLAT
      det < 0 -> PointKind.SADDLE
      hessian.fxx < 0 -> PointKind.MAXIMUM
      else -> PointKind.MINIMUM
    }
  }

  /** 0.999999999 is 1: Newton's method stops a hair away from round answers. */
  private fun snap(value: Double): Double {
    val round = kotlin.math.round(value * SNAP) / SNAP
    return if (abs(value - round) < region.span * SNAP_DISTANCE) round else value
  }
}

private const val HALF = 0.5

/** The 4 corners around a point in the mixed derivative. */
private const val CORNERS = 4
private const val CUT = 0.02
private const val INSIDE_MARGIN = 1e-3
private const val SEARCH_STEPS = 32
private const val SAME_POINT = 1e-3
private const val MAX_POINTS = 24
private const val FIRST_STEP = 1e-5
private const val SECOND_STEP = 1e-3
private const val MAX_STEPS = 40
private const val TINY = 1e-300
private const val DONE = 1e-12
private const val FLAT_SLOPE = 1e-5
private const val FLAT_TEST = 1e-4
private const val SNAP = 1e6
private const val SNAP_DISTANCE = 1e-7
