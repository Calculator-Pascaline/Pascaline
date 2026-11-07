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
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FitRankingTest {
  /** A straight line with some noise, which higher curves can only follow a little better. */
  private val almostLine =
    listOf(1.0 to 2.3, 2.0 to 3.8, 3.0 to 6.4, 4.0 to 7.7, 5.0 to 10.4, 6.0 to 11.6, 7.0 to 14.5, 8.0 to 15.8)
      .map { (x, y) -> DataPoint(x, y) }

  @Test
  fun plainestCurveWinsWhenItIsEnough() {
    val fits = rankedFits(almostLine)
    assertTrue(fits.first().result.model in listOf(RegressionModel.LINEAR, RegressionModel.PROPORTIONAL))
    assertEquals(RegressionModel.entries.size, fits.size)
  }

  @Test
  fun realCurveBeatsTheLine() {
    val points = (1..8).map { DataPoint(it.toDouble(), 3 * exp(0.6 * it)) }
    assertEquals(RegressionModel.EXPONENTIAL, rankedFits(points).first().result.model)
  }

  @Test
  fun curvesThatCannotFitAreLeftOut() {
    val points = listOf(DataPoint(-1.0, 2.0), DataPoint(1.0, 3.0), DataPoint(2.0, 5.0))
    val models = rankedFits(points).map { it.result.model }
    assertTrue(RegressionModel.LOGARITHMIC !in models)
    assertTrue(RegressionModel.POWER !in models)
    assertTrue(RegressionModel.QUARTIC !in models)
  }

  @Test
  fun curveThroughEveryPointIsNotTheBest() {
    val points = listOf(DataPoint(1.0, 2.0), DataPoint(2.0, 5.0))
    assertFalse(rankedFits(points).first { it.result.model == RegressionModel.LINEAR }.isBest)
    assertTrue(rankedFits(listOf(DataPoint(1.0, 2.0))).isEmpty())
  }

  @Test
  fun bestIsCloseToTheHighestAndTheRestGoDown() {
    // Bends a lot: a straight line through the origin is far from the closest curves
    val points = listOf(1.0 to 9.0, 2.0 to 4.0, 3.0 to 2.0, 4.0 to 3.0, 5.0 to 7.0, 6.0 to 12.0).map { (x, y) -> DataPoint(x, y) }
    val fits = rankedFits(points)
    val best = fits.first()
    assertTrue(best.isBest)
    val highest = fits.maxOf { it.result.determination ?: 0.0 }
    assertTrue((best.result.determination ?: 0.0) >= highest - 0.02)
    assertTrue(best.result.model != RegressionModel.PROPORTIONAL && best.result.model != RegressionModel.LINEAR)
    val rest = fits.drop(1).map { it.result.determination ?: Double.NEGATIVE_INFINITY }
    assertEquals(rest.sortedDescending(), rest)
  }

  @Test
  fun typicalGapIsZeroOnTheCurve() {
    val points = (0..4).map { DataPoint(it.toDouble(), 2.0 * it + 1) }
    val result = regression(points, RegressionModel.LINEAR)!!
    assertTrue(abs(typicalGap(points, result)) < 1e-9)
  }

  @Test
  fun windowHoldsEveryPoint() {
    val window = fitWindow(listOf(DataPoint(2.0, 3.0), DataPoint(8.0, 9.0)))
    assertTrue(window.left < 2.0 && window.right > 8.0 && window.bottom < 3.0 && window.top > 9.0)
    val single = fitWindow(listOf(DataPoint(5.0, 5.0)))
    assertTrue(single.width > 0 && single.height > 0)
    assertEquals(FitWindow(0.0, 10.0, 0.0, 10.0), fitWindow(emptyList()))
  }

  @Test
  fun gridLinesAreRoundAndInside() {
    val window = fitWindow(emptyList())
    assertEquals(listOf(2.0, 4.0, 6.0, 8.0), window.lines(0.0, 10.0))
    assertTrue(window.lines(0.0, 37.0).all { it % 10.0 == 0.0 || it % 5.0 == 0.0 })
    assertTrue(window.lines(0.0, 0.003).isNotEmpty())
  }

  @Test
  fun pointsKeepTheirRows() {
    val rows = listOf(listOf("1", "2"), listOf("x", "3"), listOf("4", "5"))
    assertEquals(listOf(0, 2), rows.pointRows())
  }
}
