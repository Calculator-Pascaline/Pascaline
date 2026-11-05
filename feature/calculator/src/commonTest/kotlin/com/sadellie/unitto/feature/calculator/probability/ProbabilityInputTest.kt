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
import kotlin.test.assertTrue

class ProbabilityInputTest {
  @Test fun upperBoundIsAboveTheLowerOne() = assertTrue(Normal(0.0, 1.0).upperBoundAbove(1.53) > 1.53)

  @Test fun discreteBoundsAreWhole() = assertEquals("7", snapBound(6.6, 20.0, isDiscrete = true))

  @Test fun boundsFollowTheVisibleWidth() {
    assertEquals("1.5", snapBound(1.5004, 6.0, isDiscrete = false))
    assertEquals("120", snapBound(123.4, 1000.0, isDiscrete = false))
  }

  @Test fun negativeBounds() = assertEquals("−1.25", snapBound(-1.2523, 6.0, isDiscrete = false))

  @Test fun sliderHasNoRoundingErrors() = assertEquals("0.3", sliderValue(0.30000001, 0.01, "."))

  @Test fun sliderWholeSteps() = assertEquals("42", sliderValue(41.7, 1.0, "."))

  @Test fun sliderUsesTheDecimalSeparator() = assertEquals("0,5", sliderValue(0.5, 0.1, ","))
}
