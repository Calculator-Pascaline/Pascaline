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

package com.sadellie.unitto.feature.calculator.inflation

import kotlin.math.pow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class InflationModelTest {
  private val history = PriceHistory(2000, doubleArrayOf(10.0, -10.0, 5.0))

  @Test fun sameYearKeepsTheAmount() = assertEquals(100.0, history.value(100.0, 2002, 2002), 1e-9)

  // +10 % then −10 %: 1.1 × 0.9 = 0.99
  @Test fun ratesAddUp() = assertEquals(99.0, history.value(100.0, 2000, 2002), 1e-9)

  @Test fun backInTime() = assertEquals(100.0 / 0.99, history.value(100.0, 2002, 2000), 1e-9)

  @Test fun yearlyRate() = assertEquals((1.1 * 0.9 * 1.05).pow(1.0 / 3) - 1, history.yearlyRate(2000, 2003) / 100, 1e-9)

  @Test fun rateOfAYear() = assertEquals(-10.0, history.rate(2002))

  @Test fun yearsOutsideAreClamped() = assertEquals(history.level(2003), history.level(2050), 1e-12)

  // BLS: 9.9 in 1913, 258.811 in 2020
  @Test
  fun dollarSince1913() = assertEquals(258.811 / 9.9, PriceData.UNITED_STATES.value(1.0, 1913, 2020), 0.01)

  @Test
  fun everyHistoryReaches2025() =
    (CURRENCIES.map { it.history } + COUNTRIES.map { it.history }).forEach { assertEquals(2025, it.lastYear) }

  @Test
  fun everyCurrencyHasAYear() =
    COUNTRIES.forEach { country -> country.eras.forEach { era -> assertTrue(!country.years(era).isEmpty(), "${era.symbol}") } }

  // 10 000 old francs = 100 francs = 15.24 €
  @Test
  fun francsToEuros() {
    val eras = COUNTRIES.first().eras
    assertEquals(15.2449, 10_000.0 / eras[0].perNext / eras[1].perNext, 1e-4)
  }
}
