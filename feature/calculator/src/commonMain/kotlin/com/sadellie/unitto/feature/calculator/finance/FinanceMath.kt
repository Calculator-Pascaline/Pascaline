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

import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.pow

/** Values of simple interest: I = P × r × n / year. */
internal enum class SimpleValue {
  /** Interest */
  INTEREST,

  /** Annual rate, in percent */
  RATE,

  /** Principal */
  PRINCIPAL,

  /** Number of days */
  DAYS,
}

/**
 * The [SimpleValue] that is not in [known], found from the others, like the NumWorks Finance app.
 *
 * @param yearDays 360 or 365.
 * @return null when it can't be found, like a rate for no days.
 */
internal fun simpleInterest(known: Map<SimpleValue, Double>, unknown: SimpleValue, yearDays: Int): Double? {
  fun value(key: SimpleValue) = known[key]
  val result =
    when (unknown) {
      SimpleValue.INTEREST -> value(SimpleValue.PRINCIPAL)!! * value(SimpleValue.RATE)!! / 100 * value(SimpleValue.DAYS)!! / yearDays
      SimpleValue.RATE ->
        value(SimpleValue.INTEREST)!! * 100 * yearDays / (value(SimpleValue.PRINCIPAL)!! * value(SimpleValue.DAYS)!!)
      SimpleValue.PRINCIPAL ->
        value(SimpleValue.INTEREST)!! * 100 * yearDays / (value(SimpleValue.RATE)!! * value(SimpleValue.DAYS)!!)
      SimpleValue.DAYS ->
        value(SimpleValue.INTEREST)!! * 100 * yearDays / (value(SimpleValue.PRINCIPAL)!! * value(SimpleValue.RATE)!!)
    }
  return result.takeIf { it.isFinite() }
}

/** Values of compound interest, the same as on NumWorks and most financial calculators. */
internal enum class CompoundValue {
  /** Number of payments */
  N,

  /** Annual rate, in percent */
  RATE,

  /** Present value */
  PRESENT_VALUE,

  /** Payment */
  PAYMENT,

  /** Future value */
  FUTURE_VALUE,
}

/**
 * Compound interest: money received is positive, money paid is negative. A loan of 10 000 is
 * PV = 10 000 with payments below 0 and FV = 0.
 *
 * @property paymentsPerYear P/Y
 * @property compoundingsPerYear C/Y
 * @property paysAtStart Payments at the start of each period instead of the end.
 */
internal class CompoundSettings(
  val paymentsPerYear: Double = 12.0,
  val compoundingsPerYear: Double = 12.0,
  val paysAtStart: Boolean = false,
)

/** The [CompoundValue] that is not in [known], null when there is no solution. */
internal fun compoundInterest(known: Map<CompoundValue, Double>, unknown: CompoundValue, settings: CompoundSettings): Double? {
  if (settings.paymentsPerYear <= 0 || settings.compoundingsPerYear <= 0) return null
  val n = known[CompoundValue.N]
  val pv = known[CompoundValue.PRESENT_VALUE]
  val pmt = known[CompoundValue.PAYMENT]
  val fv = known[CompoundValue.FUTURE_VALUE]
  val result =
    if (unknown == CompoundValue.RATE) {
      rate(n!!, pv!!, pmt!!, fv!!, settings)
    } else {
      val i = periodRate(known[CompoundValue.RATE]!!, settings)
      val start = if (settings.paysAtStart) 1.0 else 0.0
      // Without interest: PV + Pmt × N + FV = 0
      if (abs(i) < NO_RATE) {
        when (unknown) {
          CompoundValue.N -> -(pv!! + fv!!) / pmt!!
          CompoundValue.PRESENT_VALUE -> -(pmt!! * n!! + fv!!)
          CompoundValue.PAYMENT -> -(pv!! + fv!!) / n!!
          CompoundValue.FUTURE_VALUE -> -(pv!! + pmt!! * n!!)
          CompoundValue.RATE -> error("rate is found above")
        }
      } else {
        // Payments are worth (1 + i × start) more when they are made at the start
        val adjusted = { payment: Double -> payment * (1 + i * start) / i }
        when (unknown) {
          CompoundValue.N -> {
            val ratio = (adjusted(pmt!!) - fv!!) / (adjusted(pmt) + pv!!)
            if (ratio <= 0) null else ln(ratio) / ln(1 + i)
          }
          CompoundValue.PRESENT_VALUE -> {
            val growth = (1 + i).pow(n!!)
            -(fv!! + adjusted(pmt!!) * (growth - 1)) / growth
          }
          CompoundValue.PAYMENT -> {
            val growth = (1 + i).pow(n!!)
            -(fv!! + pv!! * growth) / ((1 + i * start) * (growth - 1) / i)
          }
          CompoundValue.FUTURE_VALUE -> {
            val growth = (1 + i).pow(n!!)
            -(pv!! * growth + adjusted(pmt!!) * (growth - 1))
          }
          CompoundValue.RATE -> error("rate is found above")
        }
      }
    }
  return result?.takeIf { it.isFinite() }
}

/** Rate of one payment period from the annual rate in percent, with C/Y compoundings a year. */
private fun periodRate(annualPercent: Double, settings: CompoundSettings): Double =
  (1 + annualPercent / 100 / settings.compoundingsPerYear).pow(settings.compoundingsPerYear / settings.paymentsPerYear) - 1

/** What is left of the cash flows at [annualPercent]: 0 for the right rate. */
private fun balance(annualPercent: Double, n: Double, pv: Double, pmt: Double, fv: Double, settings: CompoundSettings): Double {
  val i = periodRate(annualPercent, settings)
  if (abs(i) < NO_RATE) return pv + pmt * n + fv
  val growth = (1 + i).pow(n)
  val start = if (settings.paysAtStart) 1.0 else 0.0
  return pv * growth + pmt * (1 + i * start) * (growth - 1) / i + fv
}

/** Annual rate in percent: a change of sign of the balance is found, then narrowed by bisection. */
private fun rate(n: Double, pv: Double, pmt: Double, fv: Double, settings: CompoundSettings): Double? {
  if (n <= 0) return null
  val f = { rate: Double -> balance(rate, n, pv, pmt, fv, settings) }
  // Rates from −99 % to 10 000 %, more steps close to 0 where most rates are
  val candidates = (-RATE_STEPS..RATE_STEPS).map { step -> MAX_RATE * (step.toDouble() / RATE_STEPS).pow(3) }.filter { it > MIN_RATE }
  for ((low, high) in candidates.zipWithNext()) {
    val fLow = f(low)
    val fHigh = f(high)
    if (!fLow.isFinite() || !fHigh.isFinite()) continue
    if (fLow == 0.0) return low
    if (fLow * fHigh < 0) {
      var a = low
      var b = high
      var fa = fLow
      repeat(BISECTION_STEPS) {
        val middle = (a + b) / 2
        val fm = f(middle)
        if (fa * fm <= 0) b = middle else {
          a = middle
          fa = fm
        }
      }
      return (a + b) / 2
    }
  }
  return null
}

private const val NO_RATE = 1e-12
private const val MAX_RATE = 10_000.0
private const val MIN_RATE = -99.0
private const val RATE_STEPS = 400
private const val BISECTION_STEPS = 200
