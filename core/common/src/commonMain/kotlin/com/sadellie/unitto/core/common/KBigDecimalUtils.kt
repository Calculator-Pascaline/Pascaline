/*
 * Unitto is a calculator for Android
 * Copyright (c) 2022-2025 Elshan Agaev
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

import kotlin.math.abs
import kotlin.math.max

/** Current maximum scale that will be used in app. Used in various places in code. */
const val MAX_SCALE: Int = 1_000

fun KBigDecimal.toFormattedString(scale: Int, outputFormat: Int): String {
  if (outputFormat == OutputFormat.ALLOW_ENGINEERING && isScientific()) {
    val (mantissa, exponent) = scientificMantissa()
    val sign = if (exponent < 0) "-" else "+"
    return mantissa.setMinimumRequiredScale(scale).trimZeros().toPlainString() + "E" + sign + abs(exponent)
  }
  return this.setMinimumRequiredScale(scale).trimZeros().toStringWith(outputFormat)
}

private val SCIENTIFIC_MAX = KBigDecimal("1E9")
private val SCIENTIFIC_MIN = KBigDecimal("1E-6")

/** True when this value is too big or too small to read written out: 5.5×10¹⁶, 3×10⁻⁸. */
fun KBigDecimal.isScientific(): Boolean {
  val magnitude = abs()
  return magnitude.compareTo(KBigDecimal.ZERO) != 0 && (magnitude < SCIENTIFIC_MIN || magnitude >= SCIENTIFIC_MAX)
}

/** 5.5 and 16 for 5.5×10¹⁶: the digits of this value with one before the point, and the power. */
fun KBigDecimal.scientificMantissa(): Pair<KBigDecimal, Int> {
  val magnitude = abs()
  val digits = magnitude.stripTrailingZeros().toPlainString()
  val exponent =
    if (magnitude >= KBigDecimal.ONE) {
      digits.substringBefore(".").length - 1
    } else {
      -(digits.substringAfter(".").indexOfFirst { it != '0' } + 1)
    }
  return multiply(KBigDecimal("1E${-exponent}")) to exponent
}

/** Removes all trailing zeroes. */
fun KBigDecimal.trimZeros(): KBigDecimal =
  if (this.isEqualTo(KBigDecimal.ZERO)) KBigDecimal.ZERO else this.stripTrailingZeros()

/**
 * Uses [compareTo], ignores scale differences.
 *
 * @param bd BigDecimal to which this BigDecimal is to be compared.
 * @return `true` if [compareTo] returned 0
 */
fun KBigDecimal.isEqualTo(bd: KBigDecimal): Boolean = compareTo(bd) == 0

/**
 * Uses [compareTo], ignores scale differences.
 *
 * @param bd BigDecimal to which this BigDecimal is to be compared.
 * @return `true` if [compareTo] returned 1
 */
fun KBigDecimal.isGreaterThan(bd: KBigDecimal): Boolean = compareTo(bd) == 1

/**
 * Uses [compareTo], ignores scale differences.
 *
 * @param bd BigDecimal to which this BigDecimal is to be compared.
 * @return `true` if [compareTo] returned -1
 */
fun KBigDecimal.isLessThan(bd: KBigDecimal): Boolean = compareTo(bd) == -1

/** Sets scale to [MAX_SCALE] and round using [KRoundingMode.HALF_EVEN] */
fun KBigDecimal.setMaxScale(): KBigDecimal = setScale(MAX_SCALE, KRoundingMode.HALF_EVEN)

/**
 * Sets the minimum scale that is required to get first non-zero value in fractional part
 *
 * @param[prefScale] Is the preferred scale, the one which will be compared against
 */
internal fun KBigDecimal.setMinimumRequiredScale(prefScale: Int): KBigDecimal {
  // values without scale do not need rescaling. 123 => 123 (will not add trailing zeroes)
  if (this.scale() <= 0) return this
  val absValue = this.abs()
  if (absValue.isLessThan(KBigDecimal.ONE)) {
    // remainder returns 0.01234. Need to use substring to skip "0." part
    val fractionalPart = absValue.remainder(KBigDecimal.ONE).toPlainString().substring(2)
    // indexOfFirst can give -1, but it's okay
    // +1 to show first non-zero digit in fractional part
    val scaleToShowFirstNonZeroDigit = fractionalPart.indexOfFirst { char -> char != '0' } + 1
    val scaleToSet = max(prefScale, scaleToShowFirstNonZeroDigit)
    return setScale(scaleToSet, KRoundingMode.HALF_EVEN)
  } else {
    return setScale(prefScale, KRoundingMode.HALF_EVEN)
  }
}

/** Shorthand function to use correct `toString` method according to [outputFormat]. */
private fun KBigDecimal.toStringWith(outputFormat: Int): String =
  // Setting result value using a specified OutputFormat
  when (outputFormat) {
    // Values that need a power of ten are written by toFormattedString
    OutputFormat.ALLOW_ENGINEERING -> this.toPlainString()
    OutputFormat.FORCE_ENGINEERING -> this.toEngineeringString()
    else -> this.toPlainString()
  }
