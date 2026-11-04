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
import com.sadellie.unitto.core.common.KBigInteger
import com.sadellie.unitto.core.common.KRoundingMode
import com.sadellie.unitto.core.common.isEqualTo
import io.github.sadellie.evaluatto.ExactFraction
import kotlin.math.min
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Tries to convert [KBigDecimal] into fractional string.
 * - 0.5 -> `1⁄2`
 * - 123.5 -> `123 1⁄2`
 * - 0.666666666 -> `2⁄3`
 * - 123 -> `Empty string`
 *
 * @return String with fractional or empty string if fractional output is impossible for [this].
 * @receiver [KBigDecimal]. Digits after [FRACTIONAL_ACCURACY] are dropped.
 */
suspend fun KBigDecimal.toFractionalString(): String =
  withContext(Dispatchers.Default) {
    val scaledDownInput = abs().setScale(min(scale(), FRACTIONAL_ACCURACY), KRoundingMode.DOWN)
    val (integral, fractional) = scaledDownInput.divideAndRemainder(KBigDecimal.ONE)
    if (fractional.isEqualTo(KBigDecimal.ZERO)) return@withContext ""
    val (numerator, denominator) = fractional.simplestFraction() ?: return@withContext ""
    // 0.999… is 1: a whole number, not a fraction
    if (denominator == KBigInteger.ONE) return@withContext ""
    val sign = if (this@toFractionalString < KBigDecimal.ZERO) "-" else ""
    val whole = if (integral.isEqualTo(KBigDecimal.ZERO)) "" else "${integral.toBigInteger()} "
    return@withContext "$sign$whole$numerator⁄$denominator"
  }

/**
 * Exact fraction in the same form as [KBigDecimal.toFractionalString]: `123 1⁄2`, empty string for
 * whole numbers and too big denominators.
 */
fun ExactFraction.toFractionalString(): String {
  if (denominator.isEqualTo(KBigDecimal.ONE)) return ""
  if (denominator.toBigInteger() > maxDenominator) return ""
  val (whole, rest) = numerator.abs().divideAndRemainder(denominator)
  val sign = if (numerator < KBigDecimal.ZERO) "-" else ""
  val wholeText = if (whole.isEqualTo(KBigDecimal.ZERO)) "" else "${whole.toBigInteger()} "
  return "$sign$wholeText${rest.toBigInteger()}⁄${denominator.toBigInteger()}"
}

/**
 * Simplest fraction whose digits start with the digits of this number (between 0 and 1): 2⁄3 for
 * 0.666666666, 333⁄500 for 0.666000 (2⁄3 would be 0.666666). Null when it needs a denominator above
 * [maxDenominator].
 *
 * The convergents of the continued fraction are the simplest fractions closer and closer to the
 * number, the first one with the same digits is taken. Exact, with whole numbers only.
 */
private fun KBigDecimal.simplestFraction(): Pair<KBigInteger, KBigInteger>? {
  val digits = scale()
  // The number is digitsValue / power exactly: 0.666 is 666 / 1000
  val power = KBigDecimal.TEN.pow(digits)
  val digitsValue = scaleByPowerOfTen(digits).setScale(0, KRoundingMode.DOWN)
  val nextDigitsValue = digitsValue + KBigDecimal.ONE
  // Numerators and denominators of the two last convergents
  var numeratorBefore = KBigDecimal.ZERO
  var numerator = KBigDecimal.ONE
  var denominatorBefore = KBigDecimal.ONE
  var denominator = KBigDecimal.ZERO
  var dividend = digitsValue
  var divisor = power
  var found: Pair<KBigInteger, KBigInteger>? = null
  var isTooBig = false
  while (found == null && !isTooBig && divisor > KBigDecimal.ZERO) {
    val (term, remainder) = dividend.divideAndRemainder(divisor)
    val nextNumerator = term * numerator + numeratorBefore
    val nextDenominator = term * denominator + denominatorBefore
    isTooBig = nextDenominator > maxDenominator.toKBigDecimal()
    // Same digits: digitsValue / power ≤ fraction < nextDigitsValue / power
    val scaled = nextNumerator * power
    if (!isTooBig && digitsValue * nextDenominator <= scaled && scaled < nextDigitsValue * nextDenominator) {
      found = nextNumerator.toBigInteger() to nextDenominator.toBigInteger()
    }
    numeratorBefore = numerator
    numerator = nextNumerator
    denominatorBefore = denominator
    denominator = nextDenominator
    dividend = divisor
    divisor = remainder
  }
  return found
}

private val maxDenominator by lazy { KBigInteger("1000000000") }
/** Digits looked at: more find fractions with bigger denominators, a few steps more each. */
private const val FRACTIONAL_ACCURACY = 30
