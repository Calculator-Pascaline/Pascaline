/*
 * Unitto is a calculator for Android
 * Copyright (c) 2024-2025 Elshan Agaev
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

package com.sadellie.unitto.core.common

import kotlin.test.Test
import kotlin.test.assertEquals

class KBigDecimalUtilsTest {
  @Test
  fun toFormattedString_test() {
    val defaultPrecision = 3

    // zero
    var input = KBigDecimal("0")
    assertEquals("0", input.toFormattedString(defaultPrecision, OutputFormat.PLAIN))
    assertEquals("0", input.toFormattedString(defaultPrecision, OutputFormat.ALLOW_ENGINEERING))
    assertEquals("0", input.toFormattedString(defaultPrecision, OutputFormat.FORCE_ENGINEERING))

    // zero with scale
    input = KBigDecimal("0.0000")
    assertEquals("0", input.toFormattedString(defaultPrecision, OutputFormat.PLAIN))
    assertEquals("0", input.toFormattedString(defaultPrecision, OutputFormat.ALLOW_ENGINEERING))
    assertEquals("0", input.toFormattedString(defaultPrecision, OutputFormat.FORCE_ENGINEERING))

    // negative with no scale
    input = KBigDecimal("-123")
    assertEquals("-123", input.toFormattedString(defaultPrecision, OutputFormat.PLAIN))
    assertEquals("-123", input.toFormattedString(defaultPrecision, OutputFormat.ALLOW_ENGINEERING))
    assertEquals("-123", input.toFormattedString(defaultPrecision, OutputFormat.FORCE_ENGINEERING))

    // negative with some trailing zeros
    input = KBigDecimal("-123.0000")
    assertEquals("-123", input.toFormattedString(defaultPrecision, OutputFormat.PLAIN))
    assertEquals("-123", input.toFormattedString(defaultPrecision, OutputFormat.ALLOW_ENGINEERING))
    assertEquals("-123", input.toFormattedString(defaultPrecision, OutputFormat.FORCE_ENGINEERING))

    // negative with some scale
    input = KBigDecimal("-123.4567")
    assertEquals("-123.457", input.toFormattedString(defaultPrecision, OutputFormat.PLAIN))
    assertEquals(
      "-123.457",
      input.toFormattedString(defaultPrecision, OutputFormat.ALLOW_ENGINEERING),
    )
    assertEquals(
      "-123.457",
      input.toFormattedString(defaultPrecision, OutputFormat.FORCE_ENGINEERING),
    )

    // positive with no scale
    input = KBigDecimal("123")
    assertEquals("123", input.toFormattedString(defaultPrecision, OutputFormat.PLAIN))
    assertEquals("123", input.toFormattedString(defaultPrecision, OutputFormat.ALLOW_ENGINEERING))
    assertEquals("123", input.toFormattedString(defaultPrecision, OutputFormat.FORCE_ENGINEERING))

    // positive with some trailing zeros
    input = KBigDecimal("123.0000")
    assertEquals("123", input.toFormattedString(defaultPrecision, OutputFormat.PLAIN))
    assertEquals("123", input.toFormattedString(defaultPrecision, OutputFormat.ALLOW_ENGINEERING))
    assertEquals("123", input.toFormattedString(defaultPrecision, OutputFormat.FORCE_ENGINEERING))

    // positive with some scale
    input = KBigDecimal("123.4567")
    assertEquals("123.457", input.toFormattedString(defaultPrecision, OutputFormat.PLAIN))
    assertEquals(
      "123.457",
      input.toFormattedString(defaultPrecision, OutputFormat.ALLOW_ENGINEERING),
    )
    assertEquals(
      "123.457",
      input.toFormattedString(defaultPrecision, OutputFormat.FORCE_ENGINEERING),
    )
  }

  @Test
  fun toFormattedString_autoNotation() {
    val auto = OutputFormat.ALLOW_ENGINEERING
    // Readable numbers stay plain, round ones too
    assertEquals("1200", KBigDecimal("1200").toFormattedString(3, auto))
    assertEquals("0.001", KBigDecimal("0.001").toFormattedString(3, auto))
    assertEquals("999999999", KBigDecimal("999999999").toFormattedString(3, auto))
    assertEquals("0.000001", KBigDecimal("0.000001").toFormattedString(6, auto))
    // Too long to read: E notation with the precision applied to the digits before it
    assertEquals("1E+9", KBigDecimal("1000000000").toFormattedString(3, auto))
    assertEquals("1.235E+11", KBigDecimal("123456789012").toFormattedString(3, auto))
    assertEquals("9.4607304725808E+24", KBigDecimal("9460730472580800000000000").toFormattedString(15, auto))
    assertEquals("1.235E-7", KBigDecimal("0.000000123456").toFormattedString(3, auto))
    assertEquals("-5E-9", KBigDecimal("-0.000000005").toFormattedString(3, auto))
    assertEquals("1.057E-25", KBigDecimal("1.0570008340246E-25").toFormattedString(3, auto))
  }

  @Test
  fun smallValuesKeepThreeDigits() {
    val plain = OutputFormat.ALLOW_ENGINEERING
    // Written out, with 3 digits from the first one that isn't zero
    assertEquals("0.00345", KBigDecimal("0.003451234").toFormattedString(3, plain))
    assertEquals("-0.0123", KBigDecimal("-0.012345").toFormattedString(3, plain))
    assertEquals("0.0000457", KBigDecimal("0.0000456789").toFormattedString(3, plain))
    // A higher precision still wins, and round values stay short
    assertEquals("0.003451", KBigDecimal("0.003451234").toFormattedString(6, plain))
    assertEquals("0.003", KBigDecimal("0.003").toFormattedString(3, plain))
    assertEquals("0.123", KBigDecimal("0.123456").toFormattedString(3, plain))
  }

  @Test
  fun trimZeros_test() {
    // zero
    var input = KBigDecimal("0")
    assertEquals(KBigDecimal("0"), input.trimZeros())
    assertEquals(KBigDecimal("0"), input.trimZeros())
    assertEquals(KBigDecimal("0"), input.trimZeros())

    // zero with scale
    input = KBigDecimal("0.0000")
    assertEquals(KBigDecimal("0"), input.trimZeros())
    assertEquals(KBigDecimal("0"), input.trimZeros())
    assertEquals(KBigDecimal("0"), input.trimZeros())

    // negative with no scale
    input = KBigDecimal("-123")
    assertEquals(KBigDecimal("-123"), input.trimZeros())
    assertEquals(KBigDecimal("-123"), input.trimZeros())
    assertEquals(KBigDecimal("-123"), input.trimZeros())

    // negative with some trailing zeros
    input = KBigDecimal("-123.0000")
    assertEquals(KBigDecimal("-123"), input.trimZeros())
    assertEquals(KBigDecimal("-123"), input.trimZeros())
    assertEquals(KBigDecimal("-123"), input.trimZeros())

    // negative with some scale
    input = KBigDecimal("-123.4567")
    assertEquals(KBigDecimal("-123.4567"), input.trimZeros())
    assertEquals(KBigDecimal("-123.4567"), input.trimZeros())
    assertEquals(KBigDecimal("-123.4567"), input.trimZeros())

    // positive with no scale
    input = KBigDecimal("123")
    assertEquals(KBigDecimal("123"), input.trimZeros())
    assertEquals(KBigDecimal("123"), input.trimZeros())
    assertEquals(KBigDecimal("123"), input.trimZeros())

    // positive with some trailing zeros
    input = KBigDecimal("123.0000")
    assertEquals(KBigDecimal("123"), input.trimZeros())
    assertEquals(KBigDecimal("123"), input.trimZeros())
    assertEquals(KBigDecimal("123"), input.trimZeros())

    // positive with some scale
    input = KBigDecimal("123.4567")
    assertEquals(KBigDecimal("123.4567"), input.trimZeros())
    assertEquals(KBigDecimal("123.4567"), input.trimZeros())
    assertEquals(KBigDecimal("123.4567"), input.trimZeros())
  }

  @Test
  fun isEqualTo_test() {
    var leftSide = KBigDecimal("0")
    var rightSide = KBigDecimal("0")
    assertEquals(true, leftSide.isEqualTo(rightSide))
    assertEquals(false, leftSide.isGreaterThan(rightSide))
    assertEquals(false, leftSide.isLessThan(rightSide))

    // scale should not matter
    leftSide = KBigDecimal("0.000")
    rightSide = KBigDecimal("0.000000")
    assertEquals(true, leftSide.isEqualTo(rightSide))
    assertEquals(false, leftSide.isGreaterThan(rightSide))
    assertEquals(false, leftSide.isLessThan(rightSide))
    leftSide = KBigDecimal("0.1234")
    rightSide = KBigDecimal("0.1234000")
    assertEquals(true, leftSide.isEqualTo(rightSide))
    assertEquals(false, leftSide.isGreaterThan(rightSide))
    assertEquals(false, leftSide.isLessThan(rightSide))

    // scale should not matter
    leftSide = KBigDecimal("0.1234")
    rightSide = KBigDecimal("0.1234567")
    assertEquals(false, leftSide.isEqualTo(rightSide))
    assertEquals(false, leftSide.isGreaterThan(rightSide))
    assertEquals(true, leftSide.isLessThan(rightSide))

    // scale should not matter
    leftSide = KBigDecimal("0.1234567")
    rightSide = KBigDecimal("0.1234")
    assertEquals(false, leftSide.isEqualTo(rightSide))
    assertEquals(true, leftSide.isGreaterThan(rightSide))
    assertEquals(false, leftSide.isLessThan(rightSide))
  }

  @Test
  fun setMinimumRequiredScale_test() {
    // prefer scale of 2, which is too low: 3 digits from the first one that isn't zero
    var bd = KBigDecimal("0.000000123456")
    assertEquals("0.000000123", bd.setMinimumRequiredScale(2).toPlainString())

    // prefer scale of 15, which is too high
    bd = KBigDecimal("0.000000123456")
    assertEquals("0.000000123456000", bd.setMinimumRequiredScale(15).toPlainString())

    // prefer scale of 9, which will cut this value
    bd = KBigDecimal("0.000000123456")
    assertEquals("0.000000123", bd.setMinimumRequiredScale(9).toPlainString())

    // small, but has no leading zeroes in fractional part, should use preferred scale
    bd = KBigDecimal("0.123456")
    assertEquals("0.123", bd.setMinimumRequiredScale(3).toPlainString())

    // big value
    bd = KBigDecimal("123.000000123456")
    assertEquals("123.000000123", bd.setMinimumRequiredScale(9).toPlainString())

    // zero without scale
    bd = KBigDecimal("0")
    assertEquals("0", bd.setMinimumRequiredScale(3).toPlainString())

    // big value without scale
    bd = KBigDecimal("123")
    assertEquals("123", bd.setMinimumRequiredScale(3).toPlainString())

    // big value without negative scale
    bd = KBigDecimal("0").setScale(-190, KRoundingMode.HALF_EVEN)
    println(bd.scale())
    assertEquals("0", bd.setMinimumRequiredScale(3).toPlainString())
  }
}
