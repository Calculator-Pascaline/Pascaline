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

package com.sadellie.unitto.feature.calculator.finance

import com.sadellie.unitto.feature.calculator.finance.CompoundValue.FUTURE_VALUE
import com.sadellie.unitto.feature.calculator.finance.CompoundValue.N
import com.sadellie.unitto.feature.calculator.finance.CompoundValue.PAYMENT
import com.sadellie.unitto.feature.calculator.finance.CompoundValue.PRESENT_VALUE
import com.sadellie.unitto.feature.calculator.finance.CompoundValue.RATE
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class FinanceMathTest {
  private val monthly = CompoundSettings(12.0, 12.0)

  // 1000 at 5 % for 73 days on a 365 day year: 10
  @Test
  fun simpleInterest() =
    assertEquals(
      10.0,
      simpleInterest(mapOf(SimpleValue.PRINCIPAL to 1000.0, SimpleValue.RATE to 5.0, SimpleValue.DAYS to 73.0), SimpleValue.INTEREST, 365)!!,
      1e-9,
    )

  @Test
  fun simpleRate() =
    assertEquals(
      5.0,
      simpleInterest(mapOf(SimpleValue.PRINCIPAL to 1000.0, SimpleValue.INTEREST to 10.0, SimpleValue.DAYS to 72.0), SimpleValue.RATE, 360)!!,
      1e-9,
    )

  @Test
  fun simpleNoDays() =
    assertNull(
      simpleInterest(mapOf(SimpleValue.PRINCIPAL to 1000.0, SimpleValue.INTEREST to 10.0, SimpleValue.DAYS to 0.0), SimpleValue.RATE, 360)
    )

  // Loan of 200 000 over 30 years at 6 %: payments of 1199.10 a month
  @Test
  fun mortgagePayment() =
    assertEquals(
      -1199.101050,
      compoundInterest(mapOf(N to 360.0, RATE to 6.0, PRESENT_VALUE to 200_000.0, FUTURE_VALUE to 0.0), PAYMENT, monthly)!!,
      1e-5,
    )

  @Test
  fun mortgageRate() =
    assertEquals(
      6.0,
      compoundInterest(mapOf(N to 360.0, PAYMENT to -1199.10105, PRESENT_VALUE to 200_000.0, FUTURE_VALUE to 0.0), RATE, monthly)!!,
      1e-6,
    )

  @Test
  fun mortgageLength() =
    assertEquals(
      360.0,
      compoundInterest(mapOf(RATE to 6.0, PAYMENT to -1199.10105, PRESENT_VALUE to 200_000.0, FUTURE_VALUE to 0.0), N, monthly)!!,
      1e-6,
    )

  @Test
  fun loanAmount() =
    assertEquals(
      200_000.0,
      compoundInterest(mapOf(N to 360.0, RATE to 6.0, PAYMENT to -1199.10105, FUTURE_VALUE to 0.0), PRESENT_VALUE, monthly)!!,
      1e-3,
    )

  // 1000 placed at 5 % a year for 10 years: 1628.89
  @Test
  fun savings() =
    assertEquals(
      -1628.894627,
      compoundInterest(mapOf(N to 10.0, RATE to 5.0, PRESENT_VALUE to 1000.0, PAYMENT to 0.0), FUTURE_VALUE, CompoundSettings(1.0, 1.0))!!,
      1e-6,
    )

  // Payments at the start earn one more period: × 1.005
  @Test
  fun paymentsAtStart() {
    val end = compoundInterest(mapOf(N to 12.0, RATE to 6.0, PRESENT_VALUE to 0.0, PAYMENT to -100.0), FUTURE_VALUE, monthly)!!
    val start =
      compoundInterest(mapOf(N to 12.0, RATE to 6.0, PRESENT_VALUE to 0.0, PAYMENT to -100.0), FUTURE_VALUE, CompoundSettings(12.0, 12.0, true))!!
    assertEquals(end * 1.005, start, 1e-9)
  }

  @Test
  fun noInterest() =
    assertEquals(-50.0, compoundInterest(mapOf(N to 20.0, RATE to 0.0, PRESENT_VALUE to 1000.0, FUTURE_VALUE to 0.0), PAYMENT, monthly)!!, 1e-12)

  // Paying more than borrowed with no time can't happen
  @Test
  fun noRate() = assertNull(compoundInterest(mapOf(N to 12.0, PAYMENT to 100.0, PRESENT_VALUE to 1000.0, FUTURE_VALUE to 0.0), RATE, monthly))
}
