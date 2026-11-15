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

package io.github.calculatorpascaline.feature.calculator.inflation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class ThenAndNowTest {
  private val france = COUNTRIES.first()
  private val uk = COUNTRIES.last()

  @Test
  fun everyCountryHasPayFrom1960To2025() =
    COUNTRIES.forEach { country ->
      val pay = assertNotNull(country.pay)
      assertEquals(1960, pay.firstYear)
      assertEquals(2025, pay.lastYear)
      (1960..2025).forEach { assertNotNull(pay[it], "${country.history.baseYear} $it") }
    }

  @Test
  fun breadYears() {
    assertEquals(1992..2025, BreadData.BAGUETTE_KG.firstYear..BreadData.BAGUETTE_KG.lastYear)
    assertEquals(1968..2024, BreadData.WHITE_LOAF_PENCE.firstYear..BreadData.WHITE_LOAF_PENCE.lastYear)
  }

  @Test fun missingYearIsNull() = assertNull(BreadData.BAGUETTE_KG[2020])

  @Test fun latestSkipsMissingYears() = assertEquals(2019 to 3.52, BreadData.BAGUETTE_KG.latest(2020))

  @Test fun latestStopsAtTheLastYear() = assertEquals(2024 to 140.0, BreadData.WHITE_LOAF_PENCE.latest(2025))

  @Test fun nothingBeforeTheFirstYear() = assertNull(BreadData.BAGUETTE_KG.latest(1991))

  // INSEE: 2.34 € a kilo in 1995, so 0.585 € a baguette; 1 000 F = 152.45 € buys 260 of them
  @Test
  fun baguettesIn1995() {
    val (then, _) = assertNotNull(france.thenAndNow(1000 / 6.55957, 1995))
    assertEquals(0.585, assertNotNull(then.loaf), 1e-9)
    assertEquals(260.6, assertNotNull(then.loaves), 0.1)
  }

  // Before 2000, the AMECO pay of 1995 (29 212 €) times the share of it taken home in 2000
  @Test
  fun shareOfPayIn1995() {
    val (then, now) = assertNotNull(france.thenAndNow(1000 / 6.55957, 1995))
    val takenHome = 18923.0 / assertNotNull(PayData.FRANCE[2000])
    assertEquals(29212.0 * takenHome / 12, assertNotNull(then.monthlyPay), 1e-9)
    assertEquals(1000 / 6.55957 / (29212.0 * takenHome / 12) * 100, assertNotNull(then.payShare), 1e-9)
    assertEquals(2025, now.year)
    assertEquals(france.history.value(1000 / 6.55957, 1995, 2025), now.amount, 1e-9)
    // Eurostat: 32 346 € taken home in 2025
    assertEquals(32346.0 / 12, assertNotNull(now.monthlyPay), 1e-9)
  }

  @Test fun takeHomePayFromEurostat() = assertEquals(25032.0, france.netPay(2010))

  @Test
  fun everyCountryHasTakeHomePayFrom1960() =
    COUNTRIES.forEach { country -> (1960..2025).forEach { assertNotNull(country.netPay(it), "$it") } }

  // Eurostat stops in 2019 for the United Kingdom: later years take home the share of 2019
  @Test
  fun ukAfter2019() {
    val takenHome = 31409.0 / assertNotNull(PayData.UNITED_KINGDOM[2019])
    assertEquals(assertNotNull(PayData.UNITED_KINGDOM[2024]) * takenHome, assertNotNull(uk.netPay(2024)), 1e-6)
  }

  @Test
  fun hourlyPayIsTakeHomePayOverHoursWorked() {
    val hours = assertNotNull(assertNotNull(france.living).hours[2010])
    assertEquals(25032.0 / hours, assertNotNull(france.hourlyPay(2010)), 1e-9)
  }

  // Hours worked stop in 2023: later years use the hours of 2023
  @Test
  fun hoursOfTheLastYearAfterwards() {
    val hours = assertNotNull(assertNotNull(france.living).hours[2023])
    assertEquals(32346.0 / hours, assertNotNull(france.hourlyPay(2025)), 1e-9)
  }

  @Test
  fun minutesOfWorkForABaguette() {
    val (then, _) = assertNotNull(france.thenAndNow(100.0, 2010))
    val loaf = assertNotNull(then.loaf)
    assertEquals(loaf / assertNotNull(france.hourlyPay(2010)) * 60, assertNotNull(then.loafMinutes), 1e-9)
    assertEquals(100.0 / assertNotNull(france.hourlyPay(2010)), assertNotNull(then.workHours), 1e-9)
  }

  // HICP of France: food 73.30 and all items 74.93 in 1996, 134.89 and 124.43 in 2025
  @Test
  fun foodAgainstAllPrices() {
    val rise = assertNotNull(france.foodRise(1996))
    assertEquals(1996 to 2025, rise.from to rise.to)
    assertEquals(134.89 / 73.30 - 1, rise.food, 1e-9)
    assertEquals(124.43 / 74.93 - 1, rise.prices, 1e-9)
  }

  @Test fun noFoodIndexBefore1996() = assertNull(france.foodRise(1995))

  @Test
  fun ukFoodIndexFrom1988() {
    assertNotNull(uk.foodRise(1988))
  }

  @Test fun noFoodRiseFromTheLastYear() = assertNull(france.foodRise(2025))

  @Test
  fun noBreadBeforeItsFirstYear() {
    val (then, now) = assertNotNull(france.thenAndNow(100.0, 1980))
    assertNull(then.loaf)
    assertNull(now.loaf)
    assertNotNull(then.monthlyPay)
  }

  @Test fun nothingWithoutPayOrBread() = assertNull(france.thenAndNow(100.0, 1950))

  @Test fun loafInPounds() = assertEquals(0.08, assertNotNull(uk.thenAndNow(1.0, 1968)).first.loaf)

  @Test
  fun toTodaysCurrency() {
    assertEquals(1 / 100.0 / 6.55957, france.toToday(france.eras[0]), 1e-12)
    assertEquals(1 / 6.55957, france.toToday(france.eras[1]), 1e-12)
    assertEquals(1.0, france.toToday(france.eras[2]))
    assertEquals(1.0, uk.toToday(uk.eras[0]))
  }

  @Test fun lsdOfALoaf() = assertEquals("1s 7d", lsd(0.08))

  @Test fun lsdWithPounds() = assertEquals("£1 2s 6d", lsd(1.125))

  @Test fun lsdWholePounds() = assertEquals("£100", lsd(100.0))

  @Test fun lsdOfNothing() = assertEquals("0d", lsd(0.0))
}
