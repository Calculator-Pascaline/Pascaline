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

import kotlin.test.Test

private const val SUP = "\uE000"
private const val SUB = "\uE001"
private const val END = "\uE002"
private const val NUM = "\uE003"
private const val DEN = "\uE004"

class ExpressionLayoutTest {
  @Test fun power() = assertExpr("2^${SUP}3$END", "8")

  @Test fun powerWithExpression() = assertExpr("2^${SUP}1+2$END×2", "16")

  @Test fun powerMissingMultiply() = assertExpr("2^${SUP}3${END}4", "32")

  @Test fun powerNested() = assertExpr("2^${SUP}3^${SUP}2$END$END", "512")

  @Test fun exp() = assertExpr("e^${SUP}0$END", "1")

  @Test fun log10() = assertExpr("log${SUB}10$END(1000)", "3")

  @Test fun log2() = assertExpr("log${SUB}2$END(8)", "3")

  @Test fun logBaseExpression() = assertExpr("log${SUB}1+1$END(8)", "3")

  @Test fun lexPower() = assertLex("2^(3)", "2^${SUP}3$END")

  @Test fun lexLog() = assertLex("log${SUB}2${END}(8)", "log${SUB}2${END}(8)")

  @Test fun emptyPowerIgnored() = assertExpr("2^$SUP${END}+1", "3")

  @Test fun emptyExp() = assertExpr("e^$SUP$END", "2.7182818285")

  @Test fun fraction() = assertExpr("${NUM}1$END${DEN}4$END", "0.25")

  @Test fun fractionWithExpressions() = assertExpr("${NUM}1+2$END${DEN}2+4$END", "0.5")

  @Test fun fractionMissingMultiply() = assertExpr("2$NUM${"1"}$END${DEN}2$END", "1")

  @Test fun fractionThenNumber() = assertExpr("${NUM}1$END${DEN}2${END}4", "2")

  @Test
  fun fractionNested() = assertExpr("${NUM}1$END$DEN${NUM}1$END${DEN}2$END$END", "2")

  @Test fun fractionInPower() = assertExpr("4^$SUP${NUM}1$END${DEN}2$END$END", "2")

  @Test
  fun fractionEmptyDenominator() =
    assertExprFail(ExpressionException.BadExpression(), "${NUM}1$END$DEN$END")

  @Test
  fun fractionDivideByZero() =
    assertExprFail(ExpressionException.DivideByZero(), "${NUM}1$END${DEN}0$END")

  @Test
  fun emptyLogBase() =
    assertExprFail(ExpressionException.BadExpression(), "log$SUB$END(8)")

  @Test
  fun logBaseOne() = assertExprFail(ExpressionException.DivideByZero(), "log${SUB}1$END(8)")
}
