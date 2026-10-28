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

package com.sadellie.unitto.feature.calculator.prices

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class PriceMathTest {
  private fun compared(vararg offers: Offer?) = assertIs<ComparisonResult.Compared>(compare(offers.toList())).comparison

  @Test fun unitPrice() = assertEquals(2.5 / 0.3, Offer(2.5, 300.0, QuantityUnit.GRAM).unitPrice, 1e-12)

  // 2.50 for 300 g is 8.33 per kg, 1.75 for 150 g is 11.67 per kg
  @Test
  fun cheapest() {
    val comparison = compared(Offer(2.5, 300.0, QuantityUnit.GRAM), Offer(1.75, 150.0, QuantityUnit.GRAM))
    assertEquals(0, comparison.cheapest)
    assertEquals(10.0 / 3, comparison.savingPerUnit(1), 1e-9)
    // 3.33 / 11.67
    assertEquals(2.0 / 7, comparison.savingShare(1), 1e-9)
  }

  @Test
  fun differentUnits() {
    // 1 kg for 4 is cheaper than 500 g for 2.10
    val comparison = compared(Offer(2.1, 500.0, QuantityUnit.GRAM), Offer(4.0, 1.0, QuantityUnit.KILOGRAM))
    assertEquals(1, comparison.cheapest)
  }

  @Test
  fun liters() {
    val comparison = compared(Offer(1.2, 75.0, QuantityUnit.CENTILITER), Offer(1.5, 1.0, QuantityUnit.LITER))
    assertEquals(QuantityKind.VOLUME, comparison.kind)
    assertEquals(1.6, comparison.unitPrices[0], 1e-12)
  }

  @Test
  fun incompleteOffersAreLeftOut() {
    val comparison = compared(Offer(3.0, 2.0, QuantityUnit.PIECE), null, Offer(1.0, 1.0, QuantityUnit.PIECE))
    assertEquals(2, comparison.cheapest)
    assertTrue(comparison.unitPrices[1].isNaN())
  }

  @Test
  fun tie() = assertTrue(compared(Offer(1.0, 1.0, QuantityUnit.KILOGRAM), Offer(0.5, 500.0, QuantityUnit.GRAM)).isTie)

  @Test
  fun notEnough() =
    assertEquals(
      ComparisonProblem.NOT_ENOUGH_OFFERS,
      assertIs<ComparisonResult.Problem>(compare(listOf(Offer(1.0, 1.0, QuantityUnit.GRAM), null))).problem,
    )

  @Test
  fun gramsAndLiters() =
    assertEquals(
      ComparisonProblem.DIFFERENT_KINDS,
      assertIs<ComparisonResult.Problem>(compare(listOf(Offer(1.0, 1.0, QuantityUnit.GRAM), Offer(1.0, 1.0, QuantityUnit.LITER))))
        .problem,
    )

  @Test
  fun zeroQuantityIsLeftOut() {
    assertIs<ComparisonResult.Problem>(compare(listOf(Offer(1.0, 0.0, QuantityUnit.GRAM), Offer(1.0, 1.0, QuantityUnit.GRAM))))
  }

  @Test fun pound() = assertEquals(0.45359237, QuantityUnit.POUND.factor, 1e-12)
}
