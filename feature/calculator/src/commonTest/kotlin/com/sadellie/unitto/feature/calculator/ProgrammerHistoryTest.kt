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

package com.sadellie.unitto.feature.calculator

import com.sadellie.unitto.feature.calculator.programmer.ProgrammerHistoryEntry
import com.sadellie.unitto.feature.calculator.programmer.decodeProgrammerHistory
import com.sadellie.unitto.feature.calculator.programmer.encode
import io.github.sadellie.evaluatto.programmer.NumberBase
import io.github.sadellie.evaluatto.programmer.WordSize
import kotlin.test.Test
import kotlin.test.assertEquals

class ProgrammerHistoryTest {
  private val entry =
    ProgrammerHistoryEntry(
      timestamp = 1,
      tokens = listOf("FF", "AND", "0F"),
      base = NumberBase.HEX,
      wordSize = WordSize.BYTE,
      result = 15,
      label = "Mask",
    )

  @Test fun savedAndLoaded() = assertEquals(listOf(entry), decodeProgrammerHistory(listOf(entry).encode()))

  @Test
  fun brokenSaveIsEmpty() {
    assertEquals(emptyList(), decodeProgrammerHistory(""))
    assertEquals(emptyList(), decodeProgrammerHistory("not json"))
  }

  @Test fun expressionText() = assertEquals("FF AND 0F", entry.expressionText)

  @Test fun expressionInOtherBase() = assertEquals(listOf("255", "AND", "15"), entry.tokensIn(NumberBase.DEC))

  @Test
  fun resultInOtherBase() {
    assertEquals(listOf("1111"), entry.resultTokensIn(NumberBase.BIN))
    val negative = entry.copy(result = -1)
    assertEquals(listOf("−", "1"), negative.resultTokensIn(NumberBase.DEC))
    assertEquals(listOf("FF"), negative.resultTokensIn(NumberBase.HEX))
  }
}
