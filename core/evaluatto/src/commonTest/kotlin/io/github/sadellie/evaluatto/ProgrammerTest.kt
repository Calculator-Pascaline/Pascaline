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

import io.github.sadellie.evaluatto.programmer.NumberBase
import io.github.sadellie.evaluatto.programmer.WordSize
import io.github.sadellie.evaluatto.programmer.digits
import io.github.sadellie.evaluatto.programmer.programmerCalculate
import io.github.sadellie.evaluatto.programmer.programmerConvertNumber
import io.github.sadellie.evaluatto.programmer.programmerFormat
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ProgrammerTest {
  /** Tokens are separated by spaces */
  private fun calc(expr: String, base: NumberBase = NumberBase.DEC, wordSize: WordSize = WordSize.QWORD) =
    programmerCalculate(expr.split(" "), base, wordSize)

  @Test
  fun arithmetic() {
    assertEquals(7, calc("1 + 2 × 3"))
    assertEquals(9, calc("( 1 + 2 ) × 3"))
    assertEquals(3, calc("10 ÷ 3"))
    assertEquals(1, calc("10 mod 3"))
    assertEquals(-5, calc("− 5"))
  }

  @Test
  fun bitwise() {
    assertEquals(0b1000, calc("1100 AND 1010", NumberBase.BIN))
    assertEquals(0b1110, calc("1100 OR 1010", NumberBase.BIN))
    assertEquals(0b0110, calc("1100 XOR 1010", NumberBase.BIN))
    assertEquals(-1, calc("NOT 0"))
    assertEquals(0b11110111.toByte().toLong(), calc("1100 NAND 1010", NumberBase.BIN, WordSize.BYTE))
    assertEquals(0b11110001.toByte().toLong(), calc("1100 NOR 1010", NumberBase.BIN, WordSize.BYTE))
  }

  @Test
  fun precedenceLikeC() {
    // + before <<, << before AND, AND before XOR, XOR before OR
    assertEquals(8, calc("1 + 1 << 2"))
    assertEquals(1 or (2 xor 3), calc("1 OR 2 XOR 3"))
    assertEquals(1 xor (3 and 2), calc("1 XOR 3 AND 2"))
  }

  @Test
  fun shiftsAndRotations() {
    assertEquals(16, calc("1 << 4"))
    assertEquals(4, calc("16 >> 2"))
    // rotation in a byte: 1000 0001 ==> 0000 0011
    assertEquals(0b11, calc("10000001 RoL 1", NumberBase.BIN, WordSize.BYTE))
    assertEquals(0b11000000.toByte().toLong(), calc("10000001 RoR 1", NumberBase.BIN, WordSize.BYTE))
  }

  @Test
  fun wordSizeWraps() {
    assertEquals(-128, calc("127 + 1", wordSize = WordSize.BYTE))
    assertEquals(0, calc("FFFF + 1", NumberBase.HEX, WordSize.WORD))
    assertEquals(-1, calc("FFFFFFFFFFFFFFFF", NumberBase.HEX))
  }

  @Test
  fun formatInBases() {
    assertEquals("FF", programmerFormat(-1, NumberBase.HEX, WordSize.BYTE))
    assertEquals("377", programmerFormat(-1, NumberBase.OCT, WordSize.BYTE))
    assertEquals("11111111", programmerFormat(-1, NumberBase.BIN, WordSize.BYTE))
    assertEquals("−1", programmerFormat(-1, NumberBase.DEC, WordSize.BYTE))
    assertEquals("2A", programmerFormat(42, NumberBase.HEX, WordSize.QWORD))
  }

  @Test
  fun convertNumbers() {
    assertEquals("FF", programmerConvertNumber("255", NumberBase.DEC, NumberBase.HEX, WordSize.QWORD))
    assertEquals("1010", programmerConvertNumber("A", NumberBase.HEX, NumberBase.BIN, WordSize.QWORD))
  }

  @Test
  fun digitsOfBase() {
    assertEquals(listOf("1", "0"), NumberBase.BIN.digits().sortedDescending())
    assertEquals(16, NumberBase.HEX.digits().size)
  }

  @Test
  fun errors() {
    assertFailsWith<ExpressionException.DivideByZero> { calc("1 ÷ 0") }
    assertFailsWith<ExpressionException> { calc("1 +") }
    assertFailsWith<ExpressionException> { calc("2", NumberBase.BIN) }
  }

  @Test fun bracketsClosedAtTheEnd() = assertEquals(6, calc("2 × ( 1 + 2"))
}
