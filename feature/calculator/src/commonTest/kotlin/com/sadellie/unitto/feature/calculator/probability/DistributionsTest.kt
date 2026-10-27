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

import kotlin.math.E
import kotlin.test.Test
import kotlin.test.assertEquals

/** Expected values are from statistical tables. */
class DistributionsTest {
  @Test fun normalAtMost() = assertEquals(0.9750021, Normal(0.0, 1.0).atMost(1.96), 1e-7)

  @Test fun normalShifted() = assertEquals(0.8413447, Normal(10.0, 2.0).atMost(12.0), 1e-7)

  @Test fun normalBetween() = assertEquals(0.6826895, Normal(0.0, 1.0).between(-1.0, 1.0), 1e-7)

  @Test fun normalQuantile() = assertEquals(1.959964, Normal(0.0, 1.0).quantile(0.975), 1e-6)

  @Test fun binomialExactly() = assertEquals(0.24609375, Binomial(10, 0.5).exactly(5.0), 1e-12)

  @Test fun binomialAtMost() = assertEquals(0.623046875, Binomial(10, 0.5).atMost(5.0), 1e-12)

  @Test fun binomialAtLeastIncludesValue() = assertEquals(0.623046875, Binomial(10, 0.5).atLeast(5.0), 1e-12)

  @Test fun binomialQuantile() = assertEquals(5.0, Binomial(10, 0.5).quantile(0.5))

  @Test fun uniformBetween() = assertEquals(0.3, Uniform(0.0, 10.0).between(2.0, 5.0), 1e-12)

  @Test fun exponentialAtLeast() = assertEquals(1 / E, Exponential(0.5).atLeast(2.0), 1e-12)

  @Test fun chiSquared() = assertEquals(1 - 1 / E, ChiSquared(2.0).atMost(2.0), 1e-10)

  @Test fun chiSquaredTable() = assertEquals(0.95, ChiSquared(5.0).atMost(11.0705), 1e-5)

  @Test fun student() = assertEquals(0.975, Student(10.0).atMost(2.228139), 1e-6)

  @Test fun studentNegative() = assertEquals(0.025, Student(10.0).atMost(-2.228139), 1e-6)

  @Test fun geometric() = assertEquals(0.128, Geometric(0.2).exactly(3.0), 1e-12)

  @Test fun geometricAtMost() = assertEquals(0.488, Geometric(0.2).atMost(3.0), 1e-12)

  @Test fun poisson() = assertEquals(0.4231900811, Poisson(3.0).atMost(2.0), 1e-9)

  @Test fun poissonExactly() = assertEquals(0.2240418077, Poisson(3.0).exactly(2.0), 1e-9)

  @Test fun fisher() = assertEquals(0.95, Fisher(5.0, 10.0).atMost(3.325835), 1e-6)

  @Test fun hypergeometric() = assertEquals(0.4313372, Hypergeometric(50, 5, 10).exactly(1.0), 1e-6)

  @Test fun hypergeometricTotal() = assertEquals(1.0, Hypergeometric(50, 5, 10).atMost(5.0), 1e-12)

  // Support starts at 1: searching must not loop forever
  @Test fun geometricQuantile() = assertEquals(1.0, Geometric(0.5).quantile(0.001))

  @Test fun geometricQuantileHigh() = assertEquals(10.0, Geometric(0.5).quantile(0.999))

  @Test
  fun everyDefaultHasRange() {
    DistributionType.entries.forEach { type ->
      val distribution = type.create(type.parameters.map { it.default.toDouble() })!!
      val range = distribution.visibleRange()
      kotlin.test.assertTrue(range.start < range.endInclusive, type.name)
    }
  }

  @Test fun pascalRow() = assertEquals(listOf(1L, 6L, 15L, 20L, 15L, 6L, 1L), (0..6).map { binomialCoefficient(6, it) })

  @Test fun lnGammaOfFactorial() = assertEquals(kotlin.math.ln(120.0), lnGamma(6.0), 1e-12)
}
