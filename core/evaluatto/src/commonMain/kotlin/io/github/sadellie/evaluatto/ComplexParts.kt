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
import com.sadellie.unitto.core.common.KBigDecimalMath
import com.sadellie.unitto.core.common.KMathContext
import com.sadellie.unitto.core.common.KRoundingMode
import com.sadellie.unitto.core.common.isEqualTo

/**
 * Exact real part, imaginary part and length (|z|² = re² + im² is rational when both parts are
 * `a√m`), angle is recognized like other constants: 1+i ==> π/4.
 */
internal fun complexParts(
  exact: ExactValue?,
  value: Complex,
  formatter: SymbolicFormatter,
): ComplexParts {
  val modulus =
    exact?.let {
      val squared = it.re.squared() + it.im.squared()
      Surd.sqrt(squared)?.let(formatter::surd)
    }
  return ComplexParts(
    real = exact?.re?.let(formatter::surd),
    imaginary = exact?.im?.let(formatter::surd),
    modulus = modulus,
    argument = recognizeConstant(argument(value.re, value.im)),
  )
}

/** (c√r)² = c²·r */
private fun Surd.squared(): Rational = coefficient * coefficient * Rational.of(radicand)

/** Precise atan2. */
private fun argument(re: KBigDecimal, im: KBigDecimal): KBigDecimal {
  val mathContext = KMathContext(PRECISION, KRoundingMode.HALF_EVEN)
  val pi = KBigDecimalMath.pi(mathContext)
  val halfPi = pi.divide(KBigDecimal.valueOf(2), mathContext)
  if (re.isEqualTo(KBigDecimal.ZERO)) return if (im < KBigDecimal.ZERO) -halfPi else halfPi
  val atan = KBigDecimalMath.atan(im.divide(re, mathContext), mathContext)
  return when {
    re > KBigDecimal.ZERO -> atan
    im < KBigDecimal.ZERO -> atan - pi
    else -> atan + pi
  }
}

private const val PRECISION = 60
