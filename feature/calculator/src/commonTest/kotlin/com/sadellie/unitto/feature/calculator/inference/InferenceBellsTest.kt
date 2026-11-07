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

import kotlin.math.sqrt
import kotlin.test.Test
import kotlin.test.assertEquals

class InferenceBellsTest {
  @Test
  fun densityOfTheStandardCurve() {
    assertEquals(0.39894, Bell(0.0, 1.0).density(0.0), 1e-4)
    assertEquals(0.24197, Bell(0.0, 1.0).density(1.0), 1e-4)
    assertEquals(Bell(5.0, 2.0).density(3.0), Bell(5.0, 2.0).density(7.0), 1e-12)
  }

  @Test
  fun thePValueIsTheShadedAreaOfTheNullCurve() {
    val (first, second) = oneProportionBells(0.5, 62.0, 100.0)
    val test = oneProportionTest(0.5, 62.0, 100.0, Alternative.NOT_EQUAL)!!
    assertEquals(test.statistic, (second.center - first.center) / first.spread, 1e-9)
    assertEquals(0.0164, test.pValue, 1e-3)
  }

  @Test
  fun curvesOfMeansNarrowWithTheSample() {
    val (first, second) = oneMeanBells(100.0, 104.0, 15.0, 25.0)
    assertEquals(Bell(100.0, 3.0), first)
    assertEquals(Bell(104.0, 3.0), second)
    assertEquals(15 / sqrt(100.0), twoMeansBells(1.0, 15.0, 100.0, 2.0, 15.0, 100.0).first.spread, 1e-9)
  }

  @Test
  fun allOrNothingStillHasACurve() {
    val (first, second) = twoProportionsBells(0.0, 10.0, 10.0, 10.0)
    assertEquals(true, first.spread > 0 && second.spread > 0)
    assertEquals(oneProportionBells(0.5, 100.0, 100.0).first.spread, oneProportionBells(0.5, 100.0, 100.0).second.spread, 1e-12)
  }
}
