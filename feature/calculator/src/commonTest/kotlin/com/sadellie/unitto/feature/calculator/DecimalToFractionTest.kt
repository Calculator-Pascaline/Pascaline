/*
 * Unitto is a calculator for Android
 * Copyright (c) 2023-2026 Elshan Agaev
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

package com.sadellie.unitto.feature.calculator

import com.sadellie.unitto.core.common.KBigDecimal
import io.github.sadellie.evaluatto.ExactFraction
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest

class DecimalToFractionTest {
  @Test fun testNoDecimal1() = assertFractional("", "100")

  @Test fun testNoDecimal2() = assertFractional("", "100.000000000")

  @Test fun testSimpleDecimal1() = assertFractional("1/4", "0.25")

  @Test fun testSimpleDecimal2() = assertFractional("100 1/4", "100.25")

  @Test fun testRepeating1() = assertFractional("2/3", "0.666666666")

  @Test fun testRepeating2() = assertFractional("4 2/3", "4.666666666")

  @Test fun testRepeating3() = assertFractional("11/14", "0.78571428571428571428")

  @Test fun testRepeating4() = assertFractional("66 11/14", "66.78571428571428571428")

  @Test fun testRepeating5() = assertFractional("333/500", "0.666000")

  @Test fun repeatingNinesAreWhole() = assertFractional("", "0.99999999999999999999")

  // Repeats every 96 digits, more than the 30 looked at
  @Test fun longPeriod() = assertFractional("1/97", "0.010309278350515463917525773195876288659793814")

  @Test fun repeatingSeven() = assertFractional("2/7", "0.285714285714285714285714285714")

  @Test fun negativeMixedNumber() = assertFractional("-1 1/2", "-1.5")

  // No fraction with a denominator up to a billion has these digits
  @Test fun notAFraction() = assertFractional("", "0.123456789012345678901234567890")

  @Test fun piIsNotAFraction() = assertFractional("", "3.141592653589793238462643383279")

  @Test fun exactFraction() = assertExact("1/225", "1", "225")

  @Test fun exactMixedNumber() = assertExact("4 2/3", "14", "3")

  @Test fun exactNegative() = assertExact("-1 1/2", "-3", "2")

  @Test fun exactWholeNumberHasNoFraction() = assertExact("", "1", "1")

  private fun assertExact(expected: String, numerator: String, denominator: String) =
    assertEquals(
      expected,
      ExactFraction(KBigDecimal(numerator), KBigDecimal(denominator)).toFractionalString().replace("⁄", "/"),
    )

  private fun assertFractional(expected: String, actual: String) = runTest {
    assertEquals(expected, KBigDecimal(actual).toFractionalString().replace("⁄", "/"))
  }
}
