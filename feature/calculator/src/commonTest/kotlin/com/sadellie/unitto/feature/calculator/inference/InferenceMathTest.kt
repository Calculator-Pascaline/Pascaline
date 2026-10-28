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

package com.sadellie.unitto.feature.calculator.inference

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Expected values from textbook tables and R (prop.test without correction, t.test). */
class InferenceMathTest {
  // 60 heads out of 100 flips, H₀: p = 0.5
  @Test
  fun oneProportion() {
    val result = oneProportionTest(0.5, 60.0, 100.0, Alternative.NOT_EQUAL)!!
    assertEquals(2.0, result.statistic, 1e-12)
    assertEquals(0.0455003, result.pValue, 1e-6)
    assertTrue(result.rejects(0.05))
    assertFalse(result.rejects(0.01))
  }

  @Test
  fun oneSided() =
    assertEquals(0.0227501, oneProportionTest(0.5, 60.0, 100.0, Alternative.GREATER)!!.pValue, 1e-6)

  @Test
  fun lessIsTheOtherSide() =
    assertEquals(0.9772499, oneProportionTest(0.5, 60.0, 100.0, Alternative.LESS)!!.pValue, 1e-6)

  // Pooled p = 0.55, z = 0.1/√(0.55·0.45·(2/100))
  @Test
  fun twoProportions() =
    assertEquals(1.421338, twoProportionsTest(60.0, 100.0, 50.0, 100.0, Alternative.NOT_EQUAL)!!.statistic, 1e-6)

  // t = (5.2 − 5) / (0.5/√25) = 2, 24 degrees of freedom
  @Test
  fun oneMeanT() {
    val result = oneMeanTTest(5.0, 5.2, 0.5, 25.0, Alternative.NOT_EQUAL)!!
    assertEquals(2.0, result.statistic, 1e-12)
    assertEquals(24.0, result.degreesOfFreedom!!, 1e-12)
    assertEquals(0.0569398, result.pValue, 1e-6)
  }

  @Test
  fun oneMeanZ() = assertEquals(2.0, oneMeanZTest(100.0, 103.0, 15.0, 100.0, Alternative.GREATER)!!.statistic, 1e-12)

  // Welch: equal sizes and deviations give 2n − 2 degrees of freedom
  @Test
  fun welchDegrees() =
    assertEquals(18.0, twoMeansTTest(10.0, 2.0, 10.0, 8.0, 2.0, 10.0, Alternative.NOT_EQUAL)!!.degreesOfFreedom!!, 1e-9)

  @Test
  fun twoMeansZ() = assertEquals(2.0, twoMeansZTest(10.0, 3.0, 9.0, 8.0, 0.0001, 1e9, Alternative.GREATER)!!.statistic, 1e-6)

  // Fair die rolled 60 times: 5, 8, 9, 8, 10, 20 against 10 each ⇒ χ² = 13.4, 5 degrees of freedom
  @Test
  fun goodnessOfFit() {
    val result = goodnessOfFitTest(listOf(5.0, 8.0, 9.0, 8.0, 10.0, 20.0), List(6) { 10.0 })!!
    assertEquals(13.4, result.statistic, 1e-12)
    assertEquals(5.0, result.degreesOfFreedom!!, 1e-12)
    assertEquals(0.0199, result.pValue, 1e-4)
  }

  @Test
  fun wrongProportion() = assertNull(oneProportionTest(0.5, 120.0, 100.0, Alternative.NOT_EQUAL))

  // 0.6 ± 1.959964 · √(0.24/100)
  @Test
  fun proportionInterval() {
    val interval = oneProportionInterval(60.0, 100.0, 0.95)!!
    assertEquals(1.959964, interval.criticalValue, 1e-6)
    assertEquals(0.5039818, interval.low, 1e-6)
    assertEquals(0.6960182, interval.high, 1e-6)
  }

  // t* for 24 degrees of freedom at 95 % is 2.063899
  @Test
  fun meanInterval() {
    val interval = oneMeanTInterval(5.2, 0.5, 25.0, 0.95)!!
    assertEquals(2.063899, interval.criticalValue, 1e-5)
    assertEquals(0.2063899, interval.marginOfError, 1e-5)
  }

  @Test
  fun zInterval() = assertEquals(2.575829, oneMeanZInterval(0.0, 1.0, 1.0, 0.99)!!.criticalValue, 1e-6)

  @Test fun wrongLevel() = assertNull(oneMeanZInterval(0.0, 1.0, 1.0, 95.0))

  @Test
  fun twoMeansInterval() =
    assertEquals(2.0, twoMeansTInterval(10.0, 2.0, 10.0, 8.0, 2.0, 10.0, 0.95)!!.estimate, 1e-12)
}
