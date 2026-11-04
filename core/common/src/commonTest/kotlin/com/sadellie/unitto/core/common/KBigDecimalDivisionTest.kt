/*
 * Unitto is a calculator for Android
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

package com.sadellie.unitto.core.common

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Division is the same on Android and on the web: rounded to the scale of the number divided. The
 * web's library divides exactly and used to fail for results that never end, like 1/3.
 */
class KBigDecimalDivisionTest {
  @Test fun neverEnding() = assertEquals("0.333", KBigDecimal("1.000").div(KBigDecimal("3")).toPlainString())

  @Test fun roundedHalfEven() = assertEquals("0.667", KBigDecimal("2.000").div(KBigDecimal("3")).toPlainString())

  @Test fun ending() = assertEquals("0.25", KBigDecimal("1.00").div(KBigDecimal("4")).toPlainString())

  // The converter divides with MAX_SCALE decimals: 1 m in astronomical units
  @Test
  fun converterScale() =
    assertEquals(
      "0.00000000000668458712226845",
      KBigDecimal("1")
        .setMaxScale()
        .div(KBigDecimal("149597870700"))
        .setScale(26, KRoundingMode.HALF_EVEN)
        .toPlainString(),
    )
}
