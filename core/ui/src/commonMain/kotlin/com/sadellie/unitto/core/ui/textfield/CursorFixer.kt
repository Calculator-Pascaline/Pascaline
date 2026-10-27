/*
 * Unitto is a calculator for Android
 * Copyright (c) 2023-2025 Elshan Agaev
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

package com.sadellie.unitto.core.ui.textfield

import androidx.compose.foundation.text.input.TextFieldBuffer
import androidx.compose.ui.text.TextRange
import com.sadellie.unitto.core.common.PhysicalUnit
import com.sadellie.unitto.core.common.PhysicalUnits
import com.sadellie.unitto.core.common.Token
import kotlin.math.abs

fun CharSequence.fixCursor(pos: Int): Int {
  if (isEmpty()) return pos

  // Best position if we move cursor left
  var leftCursor = pos
  while (this.isPlacedIllegallyAt(leftCursor)) leftCursor--

  // Best position if we move cursor right
  var rightCursor = pos
  while (this.isPlacedIllegallyAt(rightCursor)) rightCursor++

  return listOf(leftCursor, rightCursor).minBy { abs(it - pos) }
}

fun TextFieldBuffer.fixTextRange(): TextRange {
  val text = this.asCharSequence()
  return TextRange(text.fixCursor(selection.min), text.fixCursor(selection.max))
}

/**
 * Same as [String.tokenAhead], but more efficient. We only need a number, not string. Don't
 * replace!
 */
fun String.tokenLengthAhead(pos: Int): Int {
  Token.Func.allWithOpeningBracket.forEach { if (pos.isAfterToken(this, it)) return it.length }
  unitEndingAt(pos)?.let { return it.token.length }
  if (pos.isAfterToken(this, Token.Calculus.DERIVATIVE)) return Token.Calculus.DERIVATIVE.length
  if (pos.isAfterToken(this, Token.Calculus.DX)) return Token.Calculus.DX.length

  // We default to 1 here. It means that cursor is not placed after illegal token. Just a number
  // or a binary operator or something else, can delete by one symbol.
  return 1
}

fun String.tokenAhead(pos: Int): String {
  Token.Func.allWithOpeningBracket.forEach { if (pos.isAfterToken(this, it)) return it }

  return substring((pos - 1).coerceAtLeast(0), pos)
}

/**
 * - `123,456+c|os(8)` return `true`
 * - `123,456+|cos(8)` return `false`
 * - `123,|456+cos(8)` return `false` (impossible in UI. See [ExpressionOutputTransformation])
 */
private fun CharSequence.isPlacedIllegallyAt(pos: Int): Boolean {
  Token.Func.allWithOpeningBracket.forEach { if (pos.isAtToken(this, it)) return true }
  Token.Layout.inseparable.forEach { if (pos.isAtToken(this, it)) return true }
  if (isInsideOfUnit(pos)) return true

  return false
}

private fun Int.isAtToken(str: CharSequence, token: String): Boolean {
  val checkBound = (token.length - 1).coerceAtLeast(1)
  return str
    .substring(
      startIndex = (this - checkBound).coerceAtLeast(0),
      endIndex = (this + checkBound).coerceAtMost(str.length),
    )
    .contains(token)
}

private fun Int.isAfterToken(str: String, token: String): Boolean {
  return str.substring((this - token.length).coerceAtLeast(0), this).contains(token)
}

// This can also make [TextFieldState.addTokens] better by checking tokens both ways. Needs more
// tests
fun String.tokenAfter(pos: Int): String {
  Token.Func.allWithOpeningBracket.forEach { if (pos.isBeforeToken(this, it)) return it }

  return substring(pos, (pos + 1).coerceAtMost(this.length))
}

private fun Int.isBeforeToken(str: String, token: String): Boolean {
  return str.substring(this, (this + token.length).coerceAtMost(str.length)).contains(token)
}

/** Unit token that ends right before [pos], like `_km` in `3_km|`. */
private fun CharSequence.unitEndingAt(pos: Int): PhysicalUnit? {
  for (start in (pos - 1) downTo (pos - MAX_UNIT_TOKEN_LENGTH).coerceAtLeast(0)) {
    val unit = PhysicalUnits.tokenAt(this, start) ?: continue
    if (start + unit.token.length == pos) return unit
  }
  return null
}

/** `_k|m` is not a valid cursor position. */
private fun CharSequence.isInsideOfUnit(pos: Int): Boolean {
  for (start in (pos - 1) downTo (pos - MAX_UNIT_TOKEN_LENGTH).coerceAtLeast(0)) {
    val unit = PhysicalUnits.tokenAt(this, start) ?: continue
    if (pos < start + unit.token.length) return true
  }
  return false
}

private val MAX_UNIT_TOKEN_LENGTH by lazy { PhysicalUnits.tokens.maxOf { it.length } }

