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

package io.github.sadellie.evaluatto

import com.sadellie.unitto.core.common.KBigDecimal
import com.sadellie.unitto.core.common.OutputFormat
import com.sadellie.unitto.core.common.toFormattedString
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Result rounded to 3 digits like in the calculator is exactly the result or needs ≈. */
class ShownExactlyTest {
  private fun isExact(input: String, precision: Int = 3): Boolean {
    val result = Expression(input).evaluate(precision, OutputFormat.PLAIN) as ExpressionResult.Real
    val shown = KBigDecimal(result.value.toFormattedString(precision, OutputFormat.PLAIN))
    return isShownExactly(result.value, result.fraction, shown)
  }

  @Test fun repeatingDecimalIsRounded() = assertFalse(isExact("20÷4500"))

  @Test fun oneThirdIsRounded() = assertFalse(isExact("1÷3"))

  @Test fun moreDigitsThanShownIsRounded() = assertFalse(isExact("1÷8", precision = 2))

  @Test fun terminatingDecimalIsExact() = assertTrue(isExact("1÷8"))

  @Test fun wholeNumberIsExact() = assertTrue(isExact("123×456"))

  @Test fun calculatedAsRepeatingButWhole() = assertTrue(isExact("1÷3×3"))

  @Test fun decimalSumIsExact() = assertTrue(isExact("0.1+0.2"))

  @Test fun negativePowerIsExact() = assertTrue(isExact("2^(−3)"))

  @Test fun functionWithExactValue() = assertTrue(isExact("cos(π÷3)"))

  @Test fun logarithmWithExactValue() = assertTrue(isExact("ln(e^(2))"))

  @Test fun irrationalIsRounded() = assertFalse(isExact("sin(1)"))

  @Test fun rootIsRounded() = assertFalse(isExact("2^(0.5)"))

  @Test fun piIsRounded() = assertFalse(isExact("π"))

  @Test
  fun unitConversion() {
    assertFalse(isExact("3_km→_mi"))
    assertTrue(isExact("3_km→_m"))
  }
}
