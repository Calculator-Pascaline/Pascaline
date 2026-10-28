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

import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.pow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RegressionMathTest {
  private fun points(vararg pairs: Pair<Number, Number>) = pairs.map { DataPoint(it.first.toDouble(), it.second.toDouble()) }

  private fun assertCoefficients(expected: List<Double>, result: RegressionResult?) {
    assertEquals(expected.size, result!!.coefficients.size)
    expected.zip(result.coefficients).forEach { (e, a) -> assertEquals(e, a, 1e-9) }
  }

  @Test
  fun linearThroughPoints() =
    assertCoefficients(listOf(2.0, 1.0), regression(points(0 to 1, 1 to 3, 2 to 5), RegressionModel.LINEAR))

  // Least squares of (0, 0), (1, 1), (2, 1): a = 0.5, b = 1/6
  @Test
  fun linearBestFit() =
    assertCoefficients(listOf(0.5, 1.0 / 6), regression(points(0 to 0, 1 to 1, 2 to 1), RegressionModel.LINEAR))

  @Test
  fun linearCorrelation() =
    assertEquals(1.0, regression(points(0 to 1, 1 to 3, 2 to 5), RegressionModel.LINEAR)!!.correlation!!, 1e-12)

  @Test
  fun proportional() =
    assertCoefficients(listOf(2.0), regression(points(1 to 2, 2 to 4, 3 to 6), RegressionModel.PROPORTIONAL))

  // y = x² − 2x + 3
  @Test
  fun quadratic() =
    assertCoefficients(
      listOf(1.0, -2.0, 3.0),
      regression(points(-1 to 6, 0 to 3, 1 to 2, 2 to 3, 3 to 6), RegressionModel.QUADRATIC),
    )

  // y = x³ − x
  @Test
  fun cubic() =
    assertCoefficients(
      listOf(1.0, 0.0, -1.0, 0.0),
      regression(points(-2 to -6, -1 to 0, 0 to 0, 1 to 0, 2 to 6), RegressionModel.CUBIC),
    )

  @Test
  fun quartic() {
    val result = regression((-2..2).map { DataPoint(it.toDouble(), it.toDouble().let { x -> x * x * x * x }) }, RegressionModel.QUARTIC)
    assertCoefficients(listOf(1.0, 0.0, 0.0, 0.0, 0.0), result)
  }

  // y = 3·ln(x) + 2
  @Test
  fun logarithmic() =
    assertCoefficients(
      listOf(3.0, 2.0),
      regression(listOf(1.0, 2.0, 5.0).map { DataPoint(it, 3 * ln(it) + 2) }, RegressionModel.LOGARITHMIC),
    )

  // y = 2·e^(0.5·x)
  @Test
  fun exponential() =
    assertCoefficients(
      listOf(2.0, 0.5),
      regression(listOf(0.0, 1.0, 2.0, 4.0).map { DataPoint(it, 2 * exp(0.5 * it)) }, RegressionModel.EXPONENTIAL),
    )

  // y = 4·x^1.5
  @Test
  fun power() =
    assertCoefficients(
      listOf(4.0, 1.5),
      regression(listOf(1.0, 4.0, 9.0).map { DataPoint(it, 4 * it.pow(1.5)) }, RegressionModel.POWER),
    )

  @Test fun logarithmNeedsPositiveX() = assertNull(regression(points(0 to 1, 1 to 2), RegressionModel.LOGARITHMIC))

  @Test fun exponentialNeedsPositiveY() = assertNull(regression(points(0 to -1, 1 to 2), RegressionModel.EXPONENTIAL))

  @Test fun notEnoughPoints() = assertNull(regression(points(1 to 1, 2 to 2), RegressionModel.QUADRATIC))

  @Test fun sameX() = assertNull(regression(points(1 to 1, 1 to 2, 1 to 3), RegressionModel.LINEAR))

  @Test
  fun perfectFit() =
    assertEquals(1.0, regression(points(0 to 1, 1 to 3, 2 to 5), RegressionModel.LINEAR)!!.determination!!, 1e-12)

  @Test
  fun valueOnCurve() = assertEquals(9.0, regression(points(0 to 1, 1 to 3, 2 to 5), RegressionModel.LINEAR)!!.valueAt(4.0), 1e-9)

  @Test
  fun stats() {
    val statistics = pointStatistics(points(1 to 2, 2 to 4, 3 to 9))!!
    assertEquals(2.0, statistics.meanX, 1e-12)
    assertEquals(5.0, statistics.meanY, 1e-12)
    // ((−1)(−3) + 0 + 1·4) / 3
    assertEquals(7.0 / 3, statistics.covariance, 1e-12)
  }
}
