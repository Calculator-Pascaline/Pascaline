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

import com.sadellie.unitto.core.common.OutputFormat
import com.sadellie.unitto.core.common.Token
import com.sadellie.unitto.core.common.toFormattedString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

private val SUP = Token.Layout.SUPERSCRIPT_START
private val END = Token.Layout.END
private val NUM = Token.Layout.NUMERATOR_START
private val DEN = Token.Layout.DENOMINATOR_START

private fun pow(n: Any) = "^$SUP$n$END"

/** Value with 6 digits and unit */
private fun withUnit(expr: String): Pair<String, String?> {
  val result = Expression(expr).evaluate(6, OutputFormat.PLAIN) as ExpressionResult.Real
  return result.value.toFormattedString(6, OutputFormat.PLAIN) to result.unit
}

class ExpressionUnitsTest {
  @Test fun sameUnit() = assertEquals("5" to "_km", withUnit("3_km+2_km"))

  @Test fun usedUnit() = assertEquals("3.002" to "_km", withUnit("3_km+2_m"))

  @Test fun numberWithoutMultiply() = assertEquals("6" to "_kg", withUnit("2×3_kg"))

  @Test fun newton() = assertEquals("2" to "_N", withUnit("2_kg×1_m÷_s${pow(2)}"))

  @Test fun prefix() = assertEquals("1" to "_kJ", withUnit("1000_kg×_m${pow(2)}÷_s${pow(2)}"))

  @Test fun area() = assertEquals("6" to "_m${pow(2)}", withUnit("2_m×3_m"))

  @Test fun squareRoot() = assertEquals("2" to "_m", withUnit("√(4_m${pow(2)})"))

  @Test fun convert() = assertEquals("3000" to "_m", withUnit("3_km→_m"))

  @Test
  fun convertSpeed() =
    assertEquals("27.777778" to "${NUM}_m$END${DEN}_s$END", withUnit("100_km÷_h→_m÷_s"))

  @Test fun convertImperial() = assertEquals("1.609344" to "_km", withUnit("1_mi→_km"))

  @Test fun convertTime() = assertEquals("2" to "_h", withUnit("7200_s→_h"))

  @Test fun constant() = assertEquals("299792.458" to "_km", withUnit("_c×1_s"))

  @Test fun constantConvert() = assertEquals("9.80665" to "${NUM}_m$END${DEN}_s${pow(2)}$END", withUnit("_g0→_m÷_s${pow(2)}"))

  @Test fun dimensionless() = assertEquals("1000" to null, withUnit("(1_km)÷(1_m)"))

  @Test fun degrees() = assertEquals("0.5" to null, withUnit("sin(30_°)"))

  @Test fun celsiusToFahrenheit() = assertEquals("68" to "_°F", withUnit("20_°C→_°F"))

  @Test fun kelvinToCelsius() = assertEquals("26.85" to "_°C", withUnit("300_K→_°C"))

  @Test fun negativeFahrenheit() = assertEquals("-40" to "_°C", withUnit("${Token.Operator.MINUS}40_°F→_°C"))

  @Test fun celsiusAlone() = assertEquals("20" to "_°C", withUnit("20_°C"))

  @Test
  fun celsiusArithmeticNotAllowed() {
    assertFailsWith<ExpressionException.BadExpression> { withUnit("20_°C+1_°C") }
  }

  @Test
  fun addingDifferentDimensions() {
    assertFailsWith<ExpressionException.BadExpression> { withUnit("1_m+1_s") }
  }

  @Test
  fun convertToWrongDimension() {
    assertFailsWith<ExpressionException.BadExpression> { withUnit("3_km→_s") }
  }

  @Test fun lexLongestUnit() = assertLex(listOf("2", "×", "_mmol"), "2_mmol")

  @Test fun lexConversion() = assertLex(listOf("1", "×", "_h", "→", "_min"), "1_h→_min")

  // Easy to read units
  @Test fun gigahertz() = assertEquals("1" to "_GHz", withUnit("1000000000_Hz"))

  @Test fun meterToKilometer() = assertEquals("5" to "_km", withUnit("5000_m"))

  @Test fun smallLength() = assertEquals("2" to "_mm", withUnit("0.002_m"))

  @Test fun imperialStays() = assertEquals("5280" to "_mi", withUnit("5280_mi"))

  @Test fun secondsToHours() = assertEquals("2" to "_h", withUnit("7200_s"))

  @Test fun petahertz() = assertEquals("55" to "_PHz", withUnit("55×10${pow(15)}_Hz"))

  @Test fun terawatt() = assertEquals("3" to "_TW", withUnit("3×10${pow(12)}_W"))

  @Test fun nanonewton() = assertEquals("4" to "_nN", withUnit("4×10${pow("−9")}_N"))

  @Test fun femtometer() = assertEquals("2" to "_fm", withUnit("2×10${pow("−15")}_m"))

  @Test fun nanosecond() = assertEquals("5" to "_ns", withUnit("5×10${pow("−9")}_s"))

  // Big masses stay in kg
  @Test fun bigMass() = assertEquals("5000" to "_kg", withUnit("5000_kg"))

  @Test fun bigLength() = assertEquals("5000" to "_km", withUnit("5000000_m"))

  @Test fun typedPrefix() = assertEquals("2" to "_THz", withUnit("2000_GHz"))

  // SI value in menu
  private fun si(expr: String): Pair<String, String>? =
    (Expression(expr).evaluate(6, OutputFormat.PLAIN) as ExpressionResult.Real).si?.let {
      it.value.toFormattedString(6, OutputFormat.PLAIN) to it.unit
    }

  @Test fun siKilometer() = assertEquals("5000" to "_m", si("5_km"))

  @Test fun siGram() = assertEquals("0.5" to "_kg", si("500_g"))

  @Test fun siNewton() = assertEquals("2000" to "_N", si("2_kN"))

  @Test
  fun baseUnitsOfNewton() =
    assertEquals(
      "${NUM}_kg×_m$END${DEN}_s${pow(2)}$END",
      (Expression("2_N").evaluate(6, OutputFormat.PLAIN) as ExpressionResult.Real).baseUnits,
    )
}
