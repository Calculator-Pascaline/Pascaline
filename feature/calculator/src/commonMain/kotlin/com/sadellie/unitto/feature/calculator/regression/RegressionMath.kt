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

package com.sadellie.unitto.feature.calculator.regression

import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.sqrt

internal data class DataPoint(val x: Double, val y: Double)

/**
 * Curves of the NumWorks Regression app that are found with least squares.
 *
 * @property formula With the coefficients as letters: `y = a·x + b`.
 * @property coefficients Names of the coefficients, in the order of [RegressionResult.coefficients].
 */
internal enum class RegressionModel(val formula: String, val coefficients: List<String>) {
  LINEAR("y = a·x + b", listOf("a", "b")),
  PROPORTIONAL("y = a·x", listOf("a")),
  QUADRATIC("y = a·x² + b·x + c", listOf("a", "b", "c")),
  CUBIC("y = a·x³ + b·x² + c·x + d", listOf("a", "b", "c", "d")),
  QUARTIC("y = a·x⁴ + b·x³ + c·x² + d·x + e", listOf("a", "b", "c", "d", "e")),
  LOGARITHMIC("y = a·ln(x) + b", listOf("a", "b")),
  EXPONENTIAL("y = a·e^(b·x)", listOf("a", "b")),
  POWER("y = a·x^b", listOf("a", "b")),
}

/**
 * Fitted curve.
 *
 * @property correlation Correlation coefficient r, only for [RegressionModel.LINEAR].
 * @property determination Coefficient of determination r²: 1 when the curve goes through all points.
 */
internal class RegressionResult(
  val model: RegressionModel,
  val coefficients: List<Double>,
  val correlation: Double?,
  val determination: Double?,
) {
  fun valueAt(x: Double): Double {
    val c = coefficients
    return when (model) {
      RegressionModel.LINEAR -> c[0] * x + c[1]
      RegressionModel.PROPORTIONAL -> c[0] * x
      RegressionModel.QUADRATIC,
      RegressionModel.CUBIC,
      RegressionModel.QUARTIC -> c.fold(0.0) { value, coefficient -> value * x + coefficient }
      RegressionModel.LOGARITHMIC -> if (x > 0) c[0] * ln(x) + c[1] else Double.NaN
      RegressionModel.EXPONENTIAL -> c[0] * exp(c[1] * x)
      RegressionModel.POWER -> if (x > 0) c[0] * x.pow(c[1]) else Double.NaN
    }
  }
}

/**
 * Best curve of [model] through [points], null when there are not enough points or they don't fit
 * the model (logarithmic needs x > 0, exponential y > 0, power both).
 */
internal fun regression(points: List<DataPoint>, model: RegressionModel): RegressionResult? {
  val coefficients = fit(points, model)?.takeIf { fitted -> fitted.all { it.isFinite() } } ?: return null
  val partial = RegressionResult(model, coefficients, null, null)
  return RegressionResult(
    model = model,
    coefficients = coefficients,
    correlation = if (model == RegressionModel.LINEAR) pointStatistics(points)?.correlation else null,
    determination = determination(points, partial::valueAt),
  )
}

/** Coefficients of [model], null when the points don't fit it. */
private fun fit(points: List<DataPoint>, model: RegressionModel): List<Double>? =
  when (model) {
    RegressionModel.LINEAR -> polynomialFit(points, 1)
    RegressionModel.PROPORTIONAL -> proportionalFit(points)
    RegressionModel.QUADRATIC -> polynomialFit(points, 2)
    RegressionModel.CUBIC -> polynomialFit(points, CUBIC_DEGREE)
    RegressionModel.QUARTIC -> polynomialFit(points, QUARTIC_DEGREE)
    RegressionModel.LOGARITHMIC -> logarithmicFit(points, logX = true, logY = false)
    // ln(y) = ln(a) + b·x
    RegressionModel.EXPONENTIAL -> logarithmicFit(points, logX = false, logY = true)
    // ln(y) = ln(a) + b·ln(x)
    RegressionModel.POWER -> logarithmicFit(points, logX = true, logY = true)
  }

/**
 * Straight line through the points with ln(x) and/or ln(y), which must be above 0. With ln(y) the
 * line gives ln(a): a = e^ln(a).
 */
private fun logarithmicFit(points: List<DataPoint>, logX: Boolean, logY: Boolean): List<Double>? {
  val fits = points.all { (!logX || it.x > 0) && (!logY || it.y > 0) }
  val line =
    if (fits) {
      polynomialFit(points.map { DataPoint(if (logX) ln(it.x) else it.x, if (logY) ln(it.y) else it.y) }, 1)
    } else {
      null
    }
  return if (logY) line?.let { (b, lnA) -> listOf(exp(lnA), b) } else line
}

private const val CUBIC_DEGREE = 3
private const val QUARTIC_DEGREE = 4

/** Coefficients from the highest power: `a·x² + b·x + c` ==> a, b, c. */
private fun polynomialFit(points: List<DataPoint>, degree: Int): List<Double>? {
  // Different x are needed to find degree + 1 coefficients
  if (points.map { it.x }.distinct().size <= degree) return null
  val size = degree + 1
  // Normal equations: Σ x^(i+j) · c_j = Σ x^i · y, powers from 0 to degree
  val matrix = Array(size) { row -> DoubleArray(size) { column -> points.sumOf { it.x.pow(row + column) } } }
  val vector = DoubleArray(size) { row -> points.sumOf { it.x.pow(row) * it.y } }
  return solve(matrix, vector)?.reversed()
}

private fun proportionalFit(points: List<DataPoint>): List<Double>? {
  val squares = points.sumOf { it.x * it.x }
  if (points.isEmpty() || squares == 0.0) return null
  return listOf(points.sumOf { it.x * it.y } / squares)
}

/** Gaussian elimination with partial pivoting, null when there is no single solution. */
private fun solve(matrix: Array<DoubleArray>, vector: DoubleArray): List<Double>? {
  val size = vector.size
  val a = Array(size) { matrix[it].copyOf() }
  val b = vector.copyOf()
  val scale = a.maxOf { row -> row.maxOf { abs(it) } }
  var isSingular = scale <= 0
  var column = 0
  while (!isSingular && column < size) {
    val pivot = (column until size).maxBy { abs(a[it][column]) }
    isSingular = abs(a[pivot][column]) < SINGULAR * scale
    if (!isSingular) {
      a[column] = a[pivot].also { a[pivot] = a[column] }
      b[column] = b[pivot].also { b[pivot] = b[column] }
      eliminateBelow(a, b, column)
    }
    column++
  }
  if (isSingular) return null
  val solution = DoubleArray(size)
  for (row in size - 1 downTo 0) {
    val known = (row + 1 until size).sumOf { a[row][it] * solution[it] }
    solution[row] = (b[row] - known) / a[row][row]
  }
  return solution.toList()
}

/** Subtracts row [column] from the rows below it, so that they have 0 in [column]. */
private fun eliminateBelow(a: Array<DoubleArray>, b: DoubleArray, column: Int) {
  for (row in column + 1 until b.size) {
    val factor = a[row][column] / a[column][column]
    for (k in column until b.size) a[row][k] -= factor * a[column][k]
    b[row] -= factor * b[column]
  }
}

/** 1 − (squared distances to the curve) / (squared distances to the mean of y). */
private fun determination(points: List<DataPoint>, curve: (Double) -> Double): Double? {
  if (points.size < 2) return null
  val meanY = points.sumOf { it.y } / points.size
  val total = points.sumOf { (it.y - meanY).pow(2) }
  if (total == 0.0) return null
  val residual = points.sumOf { (it.y - curve(it.x)).pow(2) }
  return 1 - residual / total
}

/** Means, standard deviations (σ) and covariance of the points, like the NumWorks Stats page. */
internal class PointStatistics(
  val count: Int,
  val meanX: Double,
  val meanY: Double,
  val deviationX: Double,
  val deviationY: Double,
  val covariance: Double,
) {
  /** Pearson's r, NaN when x or y doesn't change. */
  val correlation: Double
    get() = covariance / (deviationX * deviationY)
}

internal fun pointStatistics(points: List<DataPoint>): PointStatistics? {
  if (points.isEmpty()) return null
  val count = points.size
  val meanX = points.sumOf { it.x } / count
  val meanY = points.sumOf { it.y } / count
  return PointStatistics(
    count = count,
    meanX = meanX,
    meanY = meanY,
    deviationX = sqrt(points.sumOf { (it.x - meanX).pow(2) } / count),
    deviationY = sqrt(points.sumOf { (it.y - meanY).pow(2) } / count),
    covariance = points.sumOf { (it.x - meanX) * (it.y - meanY) } / count,
  )
}

private const val SINGULAR = 1e-12
