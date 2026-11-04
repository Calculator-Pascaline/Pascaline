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
import androidx.compose.foundation.text.input.delete
import androidx.compose.ui.text.TextRange
import com.sadellie.unitto.core.common.PhysicalUnits
import com.sadellie.unitto.core.common.Token

/*
 * Angle box of trigonometric functions: `sin(45_°)` is drawn as sin(45 [deg]), `sin(π÷2_rad)` as
 * sin(π/2 [rad]). The unit is the last thing in the brackets, the angle is calculated in it
 * whatever the angle mode is.
 */

/** Functions that get an angle box. */
val ANGLE_BOX_FUNCTIONS = listOf(Token.Func.SIN_BRACKET, Token.Func.COS_BRACKET, Token.Func.TAN_BRACKET)

val DEGREE_BOX: String = PhysicalUnits.degree.token
val RADIAN_BOX: String = PhysicalUnits.radian.token

/**
 * Types [function] with an angle box and the closing bracket after it, cursor goes before the box:
 * `sin(|_°)`. Bracket key jumps over the box and the bracket, see [skipAngleBox].
 *
 * @param radians Box is rad, otherwise deg.
 */
fun TextFieldState.addFunctionWithAngleBox(function: String, radians: Boolean) {
  val box = if (radians) RADIAN_BOX else DEGREE_BOX
  addTokens(function + box + Token.Operator.RIGHT_BRACKET)
  edit { selection = TextRange(selection.min - box.length - Token.Operator.RIGHT_BRACKET.length) }
}

/**
 * Cursor right before an angle box and its closing bracket goes after them: `sin(45|_°)` ==>
 * `sin(45_°)|`.
 *
 * @return `false` if there is no angle box at the cursor.
 */
fun TextFieldState.skipAngleBox(): Boolean {
  val cursor = selection.min
  val box = listOf(DEGREE_BOX, RADIAN_BOX).firstOrNull { text.startsWith(it, cursor) }
  val skips =
    selection.collapsed && box != null && text.startsWith(Token.Operator.RIGHT_BRACKET, cursor + box.length)
  if (skips && box != null) edit { selection = TextRange(cursor + box.length + Token.Operator.RIGHT_BRACKET.length) }
  return skips
}

/**
 * Range of the angle box of the trigonometric function [cursor] is in, `null` when there is none.
 * Nested brackets are skipped: `sin(2(1+|3)_°)` finds `_°`.
 */
fun CharSequence.angleBoxAt(cursor: Int): IntRange? {
  // Brackets around the cursor, from the inside out: where they open and close
  val brackets =
    generateSequence(cursor) { position -> closingBracketAfter(position).takeIf { it < length }?.plus(1) }
      .map { position -> openingBracketBefore(position) to closingBracketAfter(position) }
      .takeWhile { (open, _) -> open != -1 }
  // The bracket of a trigonometric function
  val (_, close) =
    brackets.firstOrNull { (open, _) -> ANGLE_BOX_FUNCTIONS.any { startsWith(it, open + 1 - it.length) } }
      ?: return null
  val box = listOf(DEGREE_BOX, RADIAN_BOX).firstOrNull { endsWithAt(it, close) }
  return box?.let { close - it.length until close }
}

/** Switches the angle box in [range] between deg and rad. */
fun TextFieldState.switchAngleBox(range: IntRange) {
  val box = text.substring(range.first, range.last + 1)
  val other = if (box == DEGREE_BOX) RADIAN_BOX else DEGREE_BOX
  val cursor = selection
  edit {
    replace(range.first, range.last + 1, other)
    // Cursor stays where it was, it moves with the text after the box
    val shift = other.length - box.length
    fun moved(position: Int) = if (position > range.first) position + shift else position
    selection = TextRange(moved(cursor.start), moved(cursor.end))
  }
}

/**
 * Picks the angle box like on paper after [typed] was typed: π means radians, an angle that starts
 * with a number like 45 means degrees. A box switched by tapping is kept when more is typed. Does
 * nothing when the cursor is not in a function with an angle box.
 */
fun TextFieldState.chooseAngleUnit(typed: String) {
  val range = text.angleBoxAt(selection.min) ?: return
  val box = text.substring(range.first, range.last + 1)
  val open = text.openingBracketBefore(range.first)
  val argument = text.substring(open + 1, range.first)
  val wanted =
    when {
      Token.Const.PI in argument -> RADIAN_BOX
      // First thing typed in the brackets
      argument == typed && typed.firstOrNull()?.isDigit() == true -> DEGREE_BOX
      else -> return
    }
  if (box != wanted) switchAngleBox(range)
}

/**
 * Backspace right after a trigonometric function with nothing in it removes its angle box and
 * closing bracket too: `sin(|_°)` ==> `|`.
 *
 * @return `false` if there is no such function and regular delete should be used.
 */
fun TextFieldState.deleteFunctionWithEmptyAngleBox(): Boolean {
  val cursor = selection.min
  val function = ANGLE_BOX_FUNCTIONS.firstOrNull { text.endsWithAt(it, cursor) }
  val box = listOf(DEGREE_BOX, RADIAN_BOX).firstOrNull { text.startsWith(it, cursor) }
  if (function == null || box == null) return false
  val closing = if (text.startsWith(Token.Operator.RIGHT_BRACKET, cursor + box.length)) 1 else 0
  edit {
    delete(cursor - function.length, cursor + box.length + closing)
    selection = TextRange(cursor - function.length)
  }
  return true
}

private fun CharSequence.endsWithAt(suffix: String, end: Int): Boolean =
  end >= suffix.length && substring(end - suffix.length, end) == suffix

/** Index of the closing bracket of the brackets [pos] is in, text length when it is not closed. */
private fun CharSequence.closingBracketAfter(pos: Int): Int {
  var depth = 0
  for (index in pos until length) {
    when (this[index]) {
      LEFT -> depth++
      RIGHT -> if (depth == 0) return index else depth--
    }
  }
  return length
}

/** Index of the opening bracket that [pos] is in, -1 when there is none. */
private fun CharSequence.openingBracketBefore(pos: Int): Int {
  var depth = 0
  for (index in pos - 1 downTo 0) {
    when (this[index]) {
      RIGHT -> depth++
      LEFT -> if (depth == 0) return index else depth--
    }
  }
  return -1
}

private val LEFT = Token.Operator.LEFT_BRACKET.first()
private val RIGHT = Token.Operator.RIGHT_BRACKET.first()
