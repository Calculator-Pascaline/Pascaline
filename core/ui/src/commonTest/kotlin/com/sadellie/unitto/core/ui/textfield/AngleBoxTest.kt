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

import com.sadellie.unitto.core.common.Token
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AngleBoxTest {
  @Test
  fun insertWithBox() {
    compareTextStates("sin([]_°)", "[]") { it.addFunctionWithAngleBox(Token.Func.SIN_BRACKET, radians = false) }
    compareTextStates("2+cos([]_rad)", "2+[]") {
      it.addFunctionWithAngleBox(Token.Func.COS_BRACKET, radians = true)
    }
  }

  @Test
  fun findBox() {
    assertEquals(6..7, "sin(45_°)".angleBoxAt(4))
    assertEquals(6..7, "sin(45_°)".angleBoxAt(6))
    // nested brackets
    assertEquals(11..12, "sin(2×(1+3)_°)".angleBoxAt(8))
    // not trigonometric, not in a function
    assertNull("ln(45_°)".angleBoxAt(4))
    assertNull("45_°".angleBoxAt(1))
  }

  @Test
  fun switchBox() {
    compareTextStates("sin(45[]_rad)", "sin(45[]_°)") { it.switchAngleBox(it.text.angleBoxAt(6)!!) }
    compareTextStates("sin(45[]_°)", "sin(45[]_rad)") { it.switchAngleBox(it.text.angleBoxAt(6)!!) }
  }

  @Test
  fun piMeansRadians() {
    compareTextStates("sin(π[]_rad", "sin(π[]_°") { it.chooseAngleUnit(Token.Const.PI) }
    compareTextStates("sin(2π[]_rad", "sin(2π[]_°") { it.chooseAngleUnit(Token.Const.PI) }
  }

  @Test
  fun bracketKeyLeavesFunction() {
    compareTextStates("cos(45_°)[]", "cos(45[]_°)") { it.addBracket() }
    // after an operator a bracket is opened, the box stays
    compareTextStates("cos(45+([]_°)", "cos(45+[]_°)") { it.addBracket() }
  }

  @Test
  fun numberMeansDegrees() {
    compareTextStates("cos(4[]_°", "cos(4[]_rad") { it.chooseAngleUnit("4") }
    // not the first thing typed: tapped box is kept
    compareTextStates("cos(45[]_rad", "cos(45[]_rad") { it.chooseAngleUnit("5") }
  }

  @Test
  fun deleteEmptyFunctionWithBox() {
    compareTextStates("2+[]", "2+sin([]_°)") { it.deleteTokens() }
    compareTextStates("2+[]", "2+sin([]_°") { it.deleteTokens() }
    // something in it: regular delete
    compareTextStates("sin(4[]_°", "sin(45[]_°") { it.deleteTokens() }
  }
}
