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

package io.github.calculatorpascaline.core.common

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FormatterSymbolsTest {
  @Test
  fun apostropheIsTheSymbolSwissPeopleUse() {
    assertEquals("'", Token.APOSTROPHE)
  }

  @Test
  fun canChooseFractional() {
    assertTrue(FormatterSymbols(Token.SPACE, Token.COMMA, false).canChooseFractional)
    assertTrue(FormatterSymbols(Token.APOSTROPHE, Token.PERIOD, false).canChooseFractional)
    assertFalse(FormatterSymbols(Token.PERIOD, Token.COMMA, false).canChooseFractional)
    assertFalse(FormatterSymbols(Token.COMMA, Token.PERIOD, false).canChooseFractional)
  }

  @Test
  fun withFractional_keepsApostropheAndSpace() {
    assertEquals(
      FormatterSymbols(Token.APOSTROPHE, Token.COMMA, true),
      FormatterSymbols(Token.APOSTROPHE, Token.PERIOD, true).withFractional(Token.COMMA),
    )
    assertEquals(
      FormatterSymbols(Token.SPACE, Token.PERIOD, false),
      FormatterSymbols(Token.SPACE, Token.COMMA, false).withFractional(Token.PERIOD),
    )
  }

  @Test
  fun withFractional_periodAndCommaBecomeSpace() {
    // The grouping symbol can't be the decimal symbol too
    assertEquals(
      FormatterSymbols(Token.SPACE, Token.PERIOD, false),
      FormatterSymbols(Token.PERIOD, Token.COMMA, false).withFractional(Token.PERIOD),
    )
  }
}
