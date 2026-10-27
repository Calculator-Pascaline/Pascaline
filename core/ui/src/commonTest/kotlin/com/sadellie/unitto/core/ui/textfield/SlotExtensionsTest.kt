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

package com.sadellie.unitto.core.ui.textfield

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.ui.graphics.Color
import com.sadellie.unitto.core.common.Token
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * `{` superscript start, `<` subscript start, `N` numerator start, `D` denominator start. `}` and
 * `>` slot end. Use [] for selection
 */
private fun slots(text: String) =
  text
    .replace("N", Token.Layout.NUMERATOR_START)
    .replace("R", Token.Layout.RADICAND_START)
    .replace("D", Token.Layout.DENOMINATOR_START)
    .replace("{", Token.Layout.SUPERSCRIPT_START)
    .replace("<", Token.Layout.SUBSCRIPT_START)
    .replace("}", Token.Layout.END)
    .replace(">", Token.Layout.END)

private fun compareSlots(expected: String, input: String, action: (TextFieldState) -> Unit) =
  compareTextStates(slots(expected), slots(input), action)

/** Same as [compareSlots], but keeps `/` for `d/dx`. */
private fun compareCalculus(expected: String, input: String, action: (TextFieldState) -> Unit) {
  fun state(text: String) =
    textState(slots(text)).apply {
      edit {
        val selection = this.selection
        replace(0, length, toString().replace(Token.Operator.DIVIDE, "/"))
        this.selection = selection
      }
    }
  val expectedState = state(expected)
  val actualState = state(input).apply(action)
  assertEquals(expectedState.text.toString(), actualState.text.toString())
  assertEquals(expectedState.selection, actualState.selection)
}

class SlotExtensionsTest {
  @Test
  fun addPower() {
    compareSlots("2^{[]}", "2[]") { it.addTokens(Token.Layout.POWER) }
    compareSlots("2^{3[]}", "2^{[]}") { it.addTokens(Token.Digit.DIGIT_3) }
    // replaces operator
    compareSlots("2^{[]}", "2+[]") { it.addTokens(Token.Layout.POWER) }
    // nested
    compareSlots("2^{3^{[]}}", "2^{3[]}") { it.addTokens(Token.Layout.POWER) }
  }

  @Test
  fun addTemplates() {
    compareSlots("e^{[]}", "[]") { it.addTokens(Token.Layout.EXP) }
    compareSlots("log<10>([]", "[]") { it.addTokens(Token.Layout.LOG_10) }
    compareSlots("∫<>{}[]dx", "[]") { it.addTokens(Token.Layout.INTEGRAL) }
  }

  @Test
  fun bracketInSlot() {
    // open at start and after operator
    compareSlots("2^{([]}", "2^{[]}") { it.addBracket() }
    compareSlots("2^{3+([]}", "2^{3+[]}") { it.addBracket() }
    // close unclosed bracket
    compareSlots("2^{(3)[]}", "2^{(3[]}") { it.addBracket() }
    // leave slot
    compareSlots("2^{3}[]", "2^{3[]}") { it.addBracket() }
    compareSlots("2^{3^{4}[]}", "2^{3^{4[]}}") { it.addBracket() }
    compareSlots("log<2>[](", "log<2[]>(") { it.addBracket() }
    // integral lower bound -> upper bound -> integrand
    compareSlots("∫<1>{[]}dx", "∫<1[]>{}dx") { it.addBracket() }
    compareSlots("∫<1>{2}[]dx", "∫<1>{2[]}dx") { it.addBracket() }
  }

  @Test
  fun deleteAfterSlot() {
    compareSlots("2^{[]}", "2^{3}[]") { it.deleteTokens() }
    compareSlots("2^{3[]}", "2^{34}[]") { it.deleteTokens() }
    compareSlots("2^{sin([]}", "2^{sin(3}[]") { it.deleteTokens() }
    compareSlots("2^{3^{4}[]}", "2^{3^{4}}[]") { it.deleteTokens() }
    // empty slot after cursor: delete whole power
    compareSlots("2[]", "2^{}[]") { it.deleteTokens() }
  }

  @Test
  fun deleteAtSlotStart() {
    compareSlots("2[]", "2^{[]}") { it.deleteTokens() }
    compareSlots("2[]^{3}", "2^{[]3}") { it.deleteTokens() }
    compareSlots("[](", "log<[]>(") { it.deleteTokens() }
    compareSlots("[]log<2>(", "log<[]2>(") { it.deleteTokens() }
    // integral
    compareSlots("[]", "∫<[]>{}dx") { it.deleteTokens() }
    compareSlots("∫<1[]>{}dx", "∫<1>{[]}dx") { it.deleteTokens() }
    compareSlots("[]∫<>{2}dx", "∫<[]>{2}dx") { it.deleteTokens() }
    compareSlots("[]x", "∫<[]>{}x") { it.deleteTokens() }
  }

  @Test
  fun addFraction() {
    // empty: cursor in numerator
    compareSlots("2+N[]}D}", "2+[]") { it.addTokens(Token.Layout.FRACTION) }
    // number before cursor becomes numerator, cursor in denominator
    compareSlots("2+N12}D[]}", "2+12[]") { it.addTokens(Token.Layout.FRACTION) }
    compareSlots("Nπ}D[]}", "π[]") { it.addTokens(Token.Layout.FRACTION) }
    compareSlots("Nsin(2)}D[]}", "sin(2)[]") { it.addTokens(Token.Layout.FRACTION) }
    // selection becomes numerator
    compareSlots("2+N3*4}D[]}", "2+[3*4]") { it.addTokens(Token.Layout.FRACTION) }
  }

  @Test
  fun fractionNavigation() {
    compareSlots("N1}D[]}", "N1[]}D}") { it.addBracket() }
    compareSlots("N1}D2}[]", "N1}D2[]}") { it.addBracket() }
    // power right before a fraction: don't jump into it
    compareSlots("2^{3}[]N}D}", "2^{3[]}N}D}") { it.addBracket() }
    compareSlots("N1[]}D}", "N1}D[]}") { it.deleteTokens() }
    compareSlots("[]", "N[]}D}") { it.deleteTokens() }
    compareSlots("[]N1}D}", "N[]1}D}") { it.deleteTokens() }
    compareSlots("N1}D[]}", "N1}D2}[]") { it.deleteTokens() }
  }

  @Test
  fun squareRoot() {
    compareSlots("2√R[]}", "2[]") { it.addTokens(Token.Layout.SQRT) }
    compareSlots("√R5}[]", "√R5[]}") { it.addBracket() }
    compareSlots("[]", "√R[]}") { it.deleteTokens() }
    compareSlots("√R[]}", "√R5}[]") { it.deleteTokens() }
  }

  @Test
  fun calculusTemplates() {
    compareSlots("∫[]dx", "[]") { it.addTokens(Token.Layout.INDEFINITE_INTEGRAL) }
    compareCalculus("d/dx([])", "[]") { it.addTokens(Token.Layout.DERIVATIVE) }
    compareCalculus("d/dx([])|<>", "[]") { it.addTokens(Token.Layout.DERIVATIVE_AT) }
    compareCalculus("d/dx(x)[]", "d/dx(x)|<[]>") { it.deleteTokens() }
    compareCalculus("[]", "d/dx[]") { it.deleteTokens() }
    // existing closing bracket is skipped, then cursor goes to the point
    compareCalculus("d/dx(x)|<[]>", "d/dx(x[])|<>") { it.addBracket() }
    compareCalculus("d/dx(x)[]", "d/dx(x[])") { it.addBracket() }
    compareCalculus("d/dx(cos(x)[])", "d/dx(cos(x[])") { it.addBracket() }
  }

  @Test
  fun sumTemplate() {
    compareSlots("Σ<[]>{}()", "[]") { it.addTokens(Token.Layout.SUM) }
    // lower bound -> upper bound -> body
    compareSlots("Σ<1>{[]}()", "Σ<1[]>{}()") { it.addBracket() }
    compareSlots("Σ<1>{5}([])", "Σ<1>{5[]}()") { it.addBracket() }
    // delete empty template
    compareSlots("[]", "Σ<[]>{}()") { it.deleteTokens() }
    compareSlots("[]", "∏<[]>{}()") { it.deleteTokens() }
    compareSlots("[]Σ<1>{}()", "Σ<[]1>{}()") { it.deleteTokens() }
  }

  @Test
  fun units() {
    // whole unit is deleted
    compareSlots("3[]", "3_km[]") { it.deleteTokens() }
    compareSlots("3_m[]", "3_m_mol[]") { it.deleteTokens() }
    // cursor can't be inside of a unit
    assertEquals(1, "3_km".fixCursor(2))
    assertEquals(4, "3_km".fixCursor(4))
  }

  @Test
  fun deleteDx() = compareSlots("∫<1>{2}x[]", "∫<1>{2}xdx[]") { it.deleteTokens() }

  @Test
  fun fixCursor() {
    assertEquals(1, slots("2^{3}").fixCursor(2))
    assertEquals(0, slots("log<10>(").fixCursor(2))
    assertEquals(0, slots("∫<1>{2}").fixCursor(1))
    assertEquals(3, slots("∫<1>{2}").fixCursor(4))
  }

  @Test
  fun slotsAt() {
    val text = slots("2^{3<4>}5")
    assertEquals(emptyList(), text.slotsAt(2))
    assertEquals(listOf(SlotKind.SUPERSCRIPT), text.slotsAt(3))
    assertEquals(listOf(SlotKind.SUPERSCRIPT, SlotKind.SUBSCRIPT), text.slotsAt(6))
    assertEquals(listOf(SlotKind.SUPERSCRIPT), text.slotsAt(7))
    assertEquals(emptyList(), text.slotsAt(8))
  }

  @Test
  fun renderSlots() {
    val rendering = slots("2^{3}+e^{}").renderSlots(Color.Red)
    // "^", "{" and "}" are hidden, empty slot gets a placeholder
    assertEquals(setOf(1, 2, 4, 7, 8), rendering.replacements.filterValues { it == '\u200B' }.keys)
    assertEquals('□', rendering.replacements[9])
    // "{3}" and "{}" are superscript, placeholder is colored
    val ranges = rendering.styles.map { it.start to it.end }
    assertEquals(listOf(2 to 5, 9 to 10, 8 to 10), ranges)
  }
}
