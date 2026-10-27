/*
 * Unitto is a calculator for Android
 * Copyright (c) 2025 Elshan Agaev
 * Copyright (c) 2026 Calculator-Pascaline (modifications)
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

object Token {
  const val SPACE = " "
  const val PERIOD = "."
  const val COMMA = ","

  object Digit {
    const val DIGIT_1 = "1"
    const val DIGIT_2 = "2"
    const val DIGIT_3 = "3"
    const val DIGIT_4 = "4"
    const val DIGIT_5 = "5"
    const val DIGIT_6 = "6"
    const val DIGIT_7 = "7"
    const val DIGIT_8 = "8"
    const val DIGIT_9 = "9"
    const val DIGIT_0 = "0"
    const val DOT = "."

    val all by lazy { listOf(DIGIT_1, DIGIT_2, DIGIT_3, DIGIT_4, DIGIT_5, DIGIT_6, DIGIT_7, DIGIT_8, DIGIT_9, DIGIT_0) }
    val allWithDot by lazy { all + DOT }
  }

  object Letter {
    const val LETTER_A = "A"
    const val LETTER_B = "B"
    const val LETTER_C = "C"
    const val LETTER_D = "D"
    const val LETTER_E = "E"
    const val LETTER_F = "F"

    val all by lazy { listOf(LETTER_A, LETTER_B, LETTER_C, LETTER_D, LETTER_E, LETTER_F) }
  }

  object Operator {
    const val PLUS = "+"
    const val MINUS = "−" // MINUS SIGN, not a regular minus (HYPHEN-MINUS)
    const val MULTIPLY = "×"
    const val DIVIDE = "÷"
    const val LEFT_BRACKET = "("
    const val RIGHT_BRACKET = ")"
    const val POWER = "^"
    const val FACTORIAL = "!"
    const val MODULO = "#"
    const val PERCENT = "%"
    const val SQRT = "√"
    /** Unit conversion: `3_km→_m` */
    const val CONVERT = "→"

    val all by lazy {
      listOf(
        PLUS,
        MINUS,
        MULTIPLY,
        DIVIDE,
        LEFT_BRACKET,
        RIGHT_BRACKET,
        POWER,
        FACTORIAL,
        MODULO,
        PERCENT,
        SQRT,
      )
    }
  }

  object Func {
    const val SIN = "sin"
    const val SIN_BRACKET = "$SIN("
    const val COS = "cos"
    const val COS_BRACKET = "$COS("
    const val TAN = "tan"
    const val TAN_BRACKET = "$TAN("
    const val ARSIN = "asin"
    const val ARSIN_BRACKET = "$ARSIN("
    const val ARCOS = "acos"
    const val ARCOS_BRACKET = "$ARCOS("
    const val ACTAN = "atan"
    const val ACTAN_BRACKET = "$ACTAN("
    const val LN = "ln"
    const val LN_BRACKET = "$LN("
    const val LOG = "log"
    const val LOG_BRACKET = "$LOG("
    const val EXP = "exp"
    const val EXP_BRACKET = "$EXP("
    const val SINH = "sinh"
    const val SINH_BRACKET = "$SINH("
    const val COSH = "cosh"
    const val COSH_BRACKET = "$COSH("
    const val TANH = "tanh"
    const val TANH_BRACKET = "$TANH("
    const val ARSINH = "asinh"
    const val ARSINH_BRACKET = "$ARSINH("
    const val ARCOSH = "acosh"
    const val ARCOSH_BRACKET = "$ARCOSH("
    const val ARTANH = "atanh"
    const val ARTANH_BRACKET = "$ARTANH("
    const val ABS = "abs"
    const val ABS_BRACKET = "$ABS("

    val all by lazy {
      listOf(
          ARSINH,
          ARCOSH,
          ARTANH,
          SINH,
          COSH,
          TANH,
          ARSIN,
          ARCOS,
          ACTAN,
          SIN,
          COS,
          TAN,
          LOG,
          EXP,
          ABS,
          LN,
        )
        .sortedByDescending { it.length }
    }

    /** Longest first, so that `sinh(` is not taken for `sin(`. */
    val allWithOpeningBracket by lazy {
      listOf(
        ARSINH_BRACKET,
        ARCOSH_BRACKET,
        ARTANH_BRACKET,
        SINH_BRACKET,
        COSH_BRACKET,
        TANH_BRACKET,
        ARSIN_BRACKET,
        ARCOS_BRACKET,
        ACTAN_BRACKET,
        SIN_BRACKET,
        COS_BRACKET,
        TAN_BRACKET,
        LOG_BRACKET,
        EXP_BRACKET,
        ABS_BRACKET,
        LN_BRACKET,
      )
    }
  }

  object Const {
    const val PI = "π"
    const val E = "e"
    /** Imaginary unit */
    const val I = "i"
    /** Only for bounds of integrals, sums and products */
    const val INFINITY = "∞"

    val all by lazy { listOf(PI, E, I, INFINITY) }
  }

  object Calculus {
    const val INTEGRAL = "∫"
    /** Derivative: `d/dx(body)` or evaluated at a point `d/dx(body)|⟨point⟩` */
    const val DERIVATIVE = "d/dx"
    const val EVALUATED_AT = "|"
    const val DX = "dx"
    const val X = "x"
    /** `Σ⟨lower⟩⟨upper⟩(body)`, index is [N] */
    const val SUM = "Σ"
    /** `∏⟨lower⟩⟨upper⟩(body)`, index is [N] */
    const val PRODUCT = "∏"
    /** Index of sums and products */
    const val N = "n"

    val all by lazy { listOf(INTEGRAL, DERIVATIVE, EVALUATED_AT, DX, X, SUM, PRODUCT, N) }
  }

  /**
   * Invisible markers for two-dimensional input (like NumWorks Epsilon). A slot starts with
   * [SUPERSCRIPT_START] or [SUBSCRIPT_START] and ends with [END]. Slots can be nested.
   * - Power: `2^⟨3⟩` is `2` + [POWER]
   * - Logarithm base: `log⟨10⟩(2)` is [LOG_10] + `2)`
   * - Definite integral: `∫⟨lower⟩⟨upper⟩ ... dx` is [INTEGRAL]
   * - Fraction: `⟨numerator⟩⟨denominator⟩` is [FRACTION], drawn with a horizontal bar
   * - Square root: `√⟨radicand⟩` is [SQRT], drawn with a bar over radicand
   */
  object Layout {
    const val SUPERSCRIPT_START = "\uE000"
    const val SUBSCRIPT_START = "\uE001"
    const val END = "\uE002"
    const val NUMERATOR_START = "\uE003"
    const val DENOMINATOR_START = "\uE004"
    const val RADICAND_START = "\uE005"

    const val POWER = "${Operator.POWER}$SUPERSCRIPT_START$END"
    const val EXP = "${Const.E}$POWER"
    const val POWER_10 = "${Digit.DIGIT_1}${Digit.DIGIT_0}$POWER"
    const val LOG_10 =
      "${Func.LOG}$SUBSCRIPT_START${Digit.DIGIT_1}${Digit.DIGIT_0}$END${Operator.LEFT_BRACKET}"
    const val INTEGRAL =
      "${Calculus.INTEGRAL}$SUBSCRIPT_START$END$SUPERSCRIPT_START$END${Calculus.DX}"

    const val FRACTION = "$NUMERATOR_START$END$DENOMINATOR_START$END"
    const val SQRT = "${Operator.SQRT}$RADICAND_START$END"
    const val INDEFINITE_INTEGRAL = "${Calculus.INTEGRAL}${Calculus.DX}"
    const val SUM =
      "${Calculus.SUM}$SUBSCRIPT_START$END$SUPERSCRIPT_START$END${Operator.LEFT_BRACKET}${Operator.RIGHT_BRACKET}"
    const val PRODUCT =
      "${Calculus.PRODUCT}$SUBSCRIPT_START$END$SUPERSCRIPT_START$END${Operator.LEFT_BRACKET}${Operator.RIGHT_BRACKET}"
    const val DERIVATIVE = "${Calculus.DERIVATIVE}${Operator.LEFT_BRACKET}${Operator.RIGHT_BRACKET}"
    const val DERIVATIVE_AT =
      "$DERIVATIVE${Calculus.EVALUATED_AT}$SUBSCRIPT_START$END"

    val all by lazy {
      listOf(
        SUPERSCRIPT_START,
        SUBSCRIPT_START,
        END,
        NUMERATOR_START,
        DENOMINATOR_START,
        RADICAND_START,
      )
    }

    /** Sequences the cursor can't be placed inside of. */
    val inseparable by lazy {
      listOf(
        "${Operator.POWER}$SUPERSCRIPT_START",
        "${Func.LOG}$SUBSCRIPT_START",
        "${Calculus.INTEGRAL}$SUBSCRIPT_START",
        "${Calculus.SUM}$SUBSCRIPT_START",
        "${Calculus.PRODUCT}$SUBSCRIPT_START",
        "$END$SUPERSCRIPT_START",
        "$END$DENOMINATOR_START",
        "${Operator.SQRT}$RADICAND_START",
        "${Calculus.EVALUATED_AT}$SUBSCRIPT_START",
        Calculus.DERIVATIVE,
        Calculus.DX,
      )
    }
  }

  /** Units and physical constants, see [PhysicalUnits]. */
  object Units {
    /** Units are written with this prefix: `_km` */
    const val PREFIX = "_"
  }

  // Used only in formatter, don't use internally
  object DisplayOnly {
    const val ENGINEERING_E = "E"
    const val MINUS = "−"
    const val FRACTION = "⁄"
    /** Constant after indefinite integral: sin(x)+C */
    const val INTEGRATION_CONSTANT = "C"
  }

  val expressionTokens by lazy {
    Digit.allWithDot +
      Operator.all +
      Func.all +
      Const.all +
      DisplayOnly.ENGINEERING_E +
      Layout.all +
      Calculus.all
  }

  val numberBaseTokens by lazy { Digit.all + Letter.all }

  val sexyToUgly by lazy {
    mapOf(
      Operator.MINUS to listOf("-", "–", "—", "—"),
      Operator.DIVIDE to listOf("/"),
      Operator.MULTIPLY to listOf("*", "•"),
      Func.ARSIN to listOf("arsin", "sin⁻¹"),
      Func.ARCOS to listOf("arcos", "cos⁻¹"),
      Func.ACTAN to listOf("actan", "tan⁻¹"),
      Func.ARSINH to listOf("sinh⁻¹"),
      Func.ARCOSH to listOf("cosh⁻¹"),
      Func.ARTANH to listOf("tanh⁻¹"),
    )
  }
}
