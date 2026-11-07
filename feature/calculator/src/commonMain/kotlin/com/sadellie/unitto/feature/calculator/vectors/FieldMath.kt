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

package com.sadellie.unitto.feature.calculator.vectors

import com.sadellie.unitto.feature.calculator.integrals.MultiFunction
import kotlin.math.hypot

/**
 * F = (P, Q) at a point, and how it changes around it.
 *
 * @property divergence ∂P/∂x + ∂Q/∂y: above 0 it spreads out like a source.
 * @property rotation ∂Q/∂x − ∂P/∂y: above 0 it turns counterclockwise.
 */
internal class VectorValues(val p: Double, val q: Double, val divergence: Double, val rotation: Double) {
  val length: Double
    get() = hypot(p, q)
}

/**
 * f at a point, its gradient ∇f = (∂f/∂x, ∂f/∂y) and its Laplacian Δf = ∂²f/∂x² + ∂²f/∂y².
 */
internal class ScalarValues(val value: Double, val gradientX: Double, val gradientY: Double, val laplacian: Double) {
  val slope: Double
    get() = hypot(gradientX, gradientY)
}

private fun MultiFunction.dx(x: Double, y: Double) = (valueAt(x + STEP, y) - valueAt(x - STEP, y)) / (2 * STEP)

private fun MultiFunction.dy(x: Double, y: Double) = (valueAt(x, y + STEP) - valueAt(x, y - STEP)) / (2 * STEP)

internal fun vectorAt(p: MultiFunction, q: MultiFunction, x: Double, y: Double) =
  VectorValues(p.valueAt(x, y), q.valueAt(x, y), p.dx(x, y) + q.dy(x, y), q.dx(x, y) - p.dy(x, y))

internal fun scalarAt(f: MultiFunction, x: Double, y: Double): ScalarValues {
  val center = f.valueAt(x, y)
  // A bigger step for second derivatives: the difference of differences loses digits
  val h = SECOND_STEP
  val laplacian =
    (f.valueAt(x + h, y) + f.valueAt(x - h, y) + f.valueAt(x, y + h) + f.valueAt(x, y - h) - 4 * center) / (h * h)
  return ScalarValues(center, f.dx(x, y), f.dy(x, y), laplacian)
}

/**
 * What the view shows of a field, in the square from −[reach] to [reach] in x and y.
 *
 * @property arrow The arrow at a point: F, or ∇f.
 * @property color The value under the arrows, whose sign gives the color.
 */
internal class FieldPicture(val reach: Double, val arrow: (Double, Double) -> Pair<Double, Double>, val color: (Double, Double) -> Double)

private const val STEP = 1e-5
private const val SECOND_STEP = 1e-3
