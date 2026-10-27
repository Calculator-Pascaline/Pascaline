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
import com.sadellie.unitto.core.common.KRoundingMode
import com.sadellie.unitto.core.common.Token
import com.sadellie.unitto.core.common.isEqualTo

/**
 * Whole number in other forms, like NumWorks shows for integers.
 *
 * @property primeFactors `2^⟨3⟩×3` with layout slots. null for 0, ±1 and numbers too big to factor.
 */
data class IntegerInfo(
  val decimal: String,
  val hexadecimal: String,
  val binary: String,
  val primeFactors: String?,
)

/** @return null when [value] is not a whole number or doesn't fit into [Long]. */
fun integerInfo(value: KBigDecimal): IntegerInfo? {
  val rounded = value.setScale(0, KRoundingMode.HALF_EVEN)
  if (!rounded.isEqualTo(value)) return null
  val number = rounded.toPlainString().toLongOrNull() ?: return null
  val sign = if (number < 0) Token.Operator.MINUS else ""
  // Long.MIN_VALUE has no positive pair
  if (number == Long.MIN_VALUE) return null
  val magnitude = kotlin.math.abs(number)
  return IntegerInfo(
    decimal = sign + magnitude.toString(),
    hexadecimal = sign + HEX_PREFIX + magnitude.toString(HEX_RADIX).uppercase(),
    binary = sign + BINARY_PREFIX + magnitude.toString(BINARY_RADIX),
    primeFactors = primeFactors(magnitude)?.let { sign + it },
  )
}

/** Trial division, so only for numbers up to [MAX_FACTORIZED]. */
private fun primeFactors(number: Long): String? {
  if (number < 2 || number > MAX_FACTORIZED) return null
  val factors = mutableListOf<Pair<Long, Int>>()
  var rest = number
  var divisor = 2L
  while (divisor * divisor <= rest) {
    var power = 0
    while (rest % divisor == 0L) {
      rest /= divisor
      power++
    }
    if (power > 0) factors += divisor to power
    divisor += if (divisor == 2L) 1 else 2
  }
  if (rest > 1) factors += rest to 1
  return factors.joinToString(Token.Operator.MULTIPLY) { (prime, power) ->
    if (power == 1) prime.toString()
    else "$prime${Token.Operator.POWER}${Token.Layout.SUPERSCRIPT_START}$power${Token.Layout.END}"
  }
}

private const val HEX_PREFIX = "0x"
private const val BINARY_PREFIX = "0b"
private const val HEX_RADIX = 16
private const val BINARY_RADIX = 2
private const val MAX_FACTORIZED = 100_000_000_000_000L
