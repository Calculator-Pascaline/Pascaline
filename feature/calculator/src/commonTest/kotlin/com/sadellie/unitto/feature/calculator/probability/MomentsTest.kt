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

package com.sadellie.unitto.feature.calculator.probability

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MomentsTest {
  private fun moments(type: DistributionType, vararg values: Double) = type.moments(values.toList(), ".")!!

  @Test fun uniform() = assertEquals(1.0 / 12, moments(DistributionType.UNIFORM, 0.0, 1.0).variance.value!!, 1e-12)

  @Test fun exponential() = assertEquals(0.25, moments(DistributionType.EXPONENTIAL, 2.0).variance.value!!, 1e-12)

  @Test fun geometric() = assertEquals(5.0, moments(DistributionType.GEOMETRIC, 0.2).expectedValue.value!!, 1e-12)

  @Test fun hypergeometric() =
    assertEquals(50 * 0.6 * 0.4 * 50 / 99, moments(DistributionType.HYPERGEOMETRIC, 100.0, 60.0, 50.0).variance.value!!, 1e-12)

  @Test fun hypergeometricEverything() =
    assertEquals(0.0, moments(DistributionType.HYPERGEOMETRIC, 10.0, 4.0, 10.0).variance.value!!, 1e-12)

  @Test fun studentNoMean() = assertNull(moments(DistributionType.STUDENT, 1.0).expectedValue.value)

  @Test fun studentInfiniteVariance() =
    assertEquals(Double.POSITIVE_INFINITY, moments(DistributionType.STUDENT, 2.0).variance.value)

  @Test fun student() = assertEquals(5.0 / 3, moments(DistributionType.STUDENT, 5.0).variance.value!!, 1e-12)

  @Test fun fisher() = assertEquals(10.0 / 8, moments(DistributionType.FISHER, 5.0, 10.0).expectedValue.value!!, 1e-12)

  // 2 × 100 × 13 / (5 × 64 × 6)
  @Test fun fisherVariance() =
    assertEquals(2600.0 / 1920, moments(DistributionType.FISHER, 5.0, 10.0).variance.value!!, 1e-12)

  @Test fun negativeInParentheses() =
    assertEquals("(1 − (−1))² / 12", moments(DistributionType.UNIFORM, -1.0, 1.0).variance.substituted)

  @Test fun everyLawButBinomial() =
    DistributionType.entries.forEach { type ->
      val values = type.parameters.map { it.default.toDouble() }
      assertEquals(type == DistributionType.BINOMIAL, type.moments(values, ".") == null, type.name)
    }
}
