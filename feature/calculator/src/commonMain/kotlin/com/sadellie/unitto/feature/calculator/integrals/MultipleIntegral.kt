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

package com.sadellie.unitto.feature.calculator.integrals

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos

/** Points in [0, 1] and their weights, for an exact sum of polynomials up to degree 2n − 1. */
internal class GaussRule(count: Int) {
  val points = DoubleArray(count)
  val weights = DoubleArray(count)

  init {
    for (i in 0 until count) {
      // Newton's method on the Legendre polynomial, from a close first guess
      var root = cos(PI * (i + THREE_QUARTERS) / (count + HALF))
      var slope: Double
      do {
        val (value, derivative) = legendre(count, root)
        slope = derivative
        val step = value / derivative
        root -= step
      } while (abs(step) > PRECISION)
      points[i] = (1 - root) / 2
      weights[i] = 1 / ((1 - root * root) * slope * slope)
    }
  }

  /** P(n) at [x] and its derivative. */
  private fun legendre(n: Int, x: Double): Pair<Double, Double> {
    var current = 1.0
    var previous = 0.0
    for (j in 1..n) {
      val older = previous
      previous = current
      current = ((2 * j - 1) * x * previous - (j - 1) * older) / j
    }
    return current to n * (x * current - previous) / (x * x - 1)
  }
}

/** From [low] to [high], which can use the outer letters: y from 0 to x². */
internal class Bounds(val low: MultiFunction, val high: MultiFunction) {
  fun lowAt(x: Double, y: Double = 0.0) = low.valueAt(x, y)

  fun highAt(x: Double, y: Double = 0.0) = high.valueAt(x, y)
}

/** ∫ from [xLow] to [xHigh], ∫ along [y], and for a triple integral ∫ along [z], of [function]. */
internal class IntegralSetup(
  val function: MultiFunction,
  val xLow: Double,
  val xHigh: Double,
  val y: Bounds,
  val z: Bounds?,
) {
  val isTriple: Boolean
    get() = z != null
}

/**
 * @property value The integral of f.
 * @property size Area of the region (double) or volume of the solid (triple): the integral of 1.
 */
internal class IntegralResult(val value: Double, val size: Double) {
  /** f on average over the region. */
  val average: Double
    get() = value / size

  val isValid: Boolean
    get() = value.isFinite() && size.isFinite()
}

/** Inner integrals first, at the points of [rule] in every direction. */
internal fun IntegralSetup.integrate(rule: GaussRule = GaussRule(GAUSS_POINTS)): IntegralResult {
  var value = 0.0
  var size = 0.0
  val xWidth = xHigh - xLow
  for (i in rule.points.indices) {
    val x = xLow + xWidth * rule.points[i]
    val y0 = y.lowAt(x)
    val yWidth = y.highAt(x) - y0
    for (j in rule.points.indices) {
      val weight = rule.weights[i] * rule.weights[j] * xWidth * yWidth
      val (columnValue, columnSize) = column(x, y0 + yWidth * rule.points[j], rule)
      value += weight * columnValue
      size += weight * columnSize
    }
  }
  return IntegralResult(value, size)
}

/** Along z at (x, y), or just f there for a double integral. */
private fun IntegralSetup.column(x: Double, y: Double, rule: GaussRule): Pair<Double, Double> {
  val z = z ?: return function.valueAt(x, y) to 1.0
  val z0 = z.lowAt(x, y)
  val zWidth = z.highAt(x, y) - z0
  val value = rule.points.indices.sumOf { rule.weights[it] * function.valueAt(x, y, z0 + zWidth * rule.points[it]) }
  return value * zWidth to zWidth
}

/** A point of the drawing, and how big f is there from 0 to 1 for its color. */
internal class Corner(val x: Double, val y: Double, val z: Double, var shade: Double = 0.0)

internal enum class FaceKind {
  /** The graph of f, or the top and the bottom of a solid */
  SURFACE,

  /** The region under the graph */
  FLOOR,

  /** Around the edge of the region */
  WALL,
}

internal class Face(val corners: List<Corner>, val kind: FaceKind)

/** The point at (u, v) of the region, from 0 to 1 both, on the top or the bottom of the drawing. */
private fun IntegralSetup.at(u: Double, v: Double, top: Boolean): Corner {
  val x = xLow + (xHigh - xLow) * u
  val y0 = y.lowAt(x)
  val y = y0 + (y.highAt(x) - y0) * v
  val z = z
  return when {
    z == null -> Corner(x, y, if (top) function.valueAt(x, y) else 0.0)
    top -> Corner(x, y, z.highAt(x, y))
    else -> Corner(x, y, z.lowAt(x, y))
  }
}

/** A little inside the edges: √(1 − x²) at x = 1 is often √(−0.000…1). */
private fun step(index: Int, steps: Int) = EDGE + (1 - 2 * EDGE) * index / steps

/** Where the drawing is: the graph over its region, or the solid between its two surfaces. */
internal fun IntegralSetup.faces(steps: Int): List<Face> =
  (tops(steps) + walls(steps))
    .filter { face -> face.corners.all { it.x.isFinite() && it.y.isFinite() && it.z.isFinite() } }
    .also { shade(it) }

private fun IntegralSetup.tops(steps: Int): List<Face> =
  (0 until steps).flatMap { i ->
    (0 until steps).flatMap { j ->
      val u0 = step(i, steps)
      val u1 = step(i + 1, steps)
      val v0 = step(j, steps)
      val v1 = step(j + 1, steps)
      listOf(true, false).map { top ->
        Face(listOf(at(u0, v0, top), at(u1, v0, top), at(u1, v1, top), at(u0, v1, top)), if (top || isTriple) FaceKind.SURFACE else FaceKind.FLOOR)
      }
    }
  }

/** From the bottom to the top along the 4 edges of the region. */
private fun IntegralSetup.walls(steps: Int): List<Face> {
  val edge = (0..steps).map { step(it, steps) }
  val first = step(0, steps)
  val last = step(steps, steps)
  return listOf(edge.map { it to first }, edge.map { last to it }, edge.reversed().map { it to last }, edge.reversed().map { first to it })
    .flatMap { side ->
      side.zipWithNext().map { (a, b) ->
        Face(listOf(at(a.first, a.second, false), at(b.first, b.second, false), at(b.first, b.second, true), at(a.first, a.second, true)), FaceKind.WALL)
      }
    }
}

/** Colors from low to high: the height of the graph, or f inside a solid. */
private fun IntegralSetup.shade(faces: List<Face>) {
  val corners = faces.filter { it.kind == FaceKind.SURFACE }.flatMap { it.corners }
  val values = corners.map { if (isTriple) function.valueAt(it.x, it.y, it.z) else it.z }
  val finite = values.filter { it.isFinite() }
  val low = finite.minOrNull() ?: 0.0
  val span = ((finite.maxOrNull() ?: 0.0) - low).takeIf { it > 0 } ?: 1.0
  corners.zip(values).forEach { (corner, value) -> corner.shade = if (value.isFinite()) (value - low) / span else 0.0 }
}

internal const val GAUSS_POINTS = 32
private const val THREE_QUARTERS = 0.75
private const val HALF = 0.5
private const val PRECISION = 1e-15
private const val EDGE = 1e-6
