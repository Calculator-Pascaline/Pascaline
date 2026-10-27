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

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.delete
import androidx.compose.foundation.text.input.placeCursorAtEnd
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.text.TextRange
import androidx.lifecycle.SavedStateHandle
import com.sadellie.unitto.core.common.Token
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce

fun TextFieldState.addTokens(tokens: String) {
  when (tokens) {
    Token.Operator.PLUS,
    Token.Operator.MULTIPLY,
    Token.Operator.DIVIDE,
    Token.Operator.POWER,
    Token.Layout.POWER -> {
      val tokenAhead = text.toString().tokenAhead(selection.min)
      if (tokenAhead == Token.Operator.PLUS) return deleteAheadAndAdd(tokens)
      if (tokenAhead == Token.Operator.MINUS) return deleteAheadAndAdd(tokens)
      if (tokenAhead == Token.Operator.MULTIPLY) return deleteAheadAndAdd(tokens)
      if (tokenAhead == Token.Operator.DIVIDE) return deleteAheadAndAdd(tokens)
      if (tokenAhead == Token.Operator.SQRT) return deleteAheadAndAdd(tokens)
      if (tokenAhead == Token.Operator.POWER) return deleteAheadAndAdd(tokens)
      if (tokenAhead == "") return deleteTokens()
    }
    Token.Operator.MINUS -> {
      val tokenAhead = text.toString().tokenAhead(selection.min)
      if (tokenAhead == Token.Operator.PLUS) return deleteAheadAndAdd(tokens)
      if (tokenAhead == Token.Operator.MINUS) return deleteAheadAndAdd(tokens)
    }
    Token.Digit.DOT -> {
      val tokenAhead = text.toString().tokenAhead(selection.min)
      if (tokenAhead == Token.Digit.DOT) return deleteAheadAndAdd(tokens)
    }
    Token.Layout.FRACTION -> return addFraction()
  }

  this.edit {
    val insertStart = selection.min
    replace(selection.min, selection.max, tokens)
    // Templates like power: place cursor in the first empty slot. Integral starts with its body,
    // bounds are optional
    val emptySlot = tokens.indexOfEmptySlot()
    val cursor =
      when {
        tokens == Token.Layout.INTEGRAL -> tokens.length - Token.Calculus.DX.length
        emptySlot == -1 -> tokens.length
        else -> emptySlot + 1
      }
    selection = TextRange(insertStart + cursor)
  }
}

/**
 * Like NumWorks: selected text or operand before cursor (number, constant or bracket) becomes
 * numerator and cursor goes to denominator. Otherwise cursor goes to empty numerator.
 */
private fun TextFieldState.addFraction() {
  val text = text.toString()
  val numeratorStart = if (selection.collapsed) text.operandStart(selection.min) else selection.min
  val numerator = text.substring(numeratorStart, selection.max)
  if (numerator.isEmpty()) {
    edit {
      val insertStart = selection.min
      replace(selection.min, selection.max, Token.Layout.FRACTION)
      selection = TextRange(insertStart + 1)
    }
    return
  }

  val fraction =
    Token.Layout.NUMERATOR_START +
      numerator +
      Token.Layout.END +
      Token.Layout.DENOMINATOR_START +
      Token.Layout.END
  edit {
    replace(numeratorStart, selection.max, fraction)
    // inside of denominator
    selection = TextRange(numeratorStart + fraction.length - 1)
  }
}

/** Start of the operand that ends at [pos]: number, constant or a closed bracket. */
private fun String.operandStart(pos: Int): Int {
  val digits = Token.Digit.allWithDot.map { it.first() }
  val previous = getOrNull(pos - 1) ?: return pos
  return when {
    previous in digits -> {
      var start = pos
      while (getOrNull(start - 1) in digits) start--
      start
    }
    previous.toString() in Token.Const.all -> pos - 1
    previous == Token.Operator.RIGHT_BRACKET.first() -> {
      var balance = 0
      var start = pos - 1
      while (start >= 0) {
        if (this[start] == Token.Operator.RIGHT_BRACKET.first()) balance++
        if (this[start] == Token.Operator.LEFT_BRACKET.first()) balance--
        if (balance == 0) break
        start--
      }
      if (start < 0) return pos
      // include function name: sin(...)
      Token.Func.all.firstOrNull { startsWith(it, start - it.length) }?.let { start -= it.length }
      start
    }
    else -> pos
  }
}

fun TextFieldState.addBracket() {
  val slotStart = text.enclosingSlotStart(selection.min)
  if (slotStart != -1) return addBracketInSlot(slotStart)

  val subStringBeforeCursor = text.substring(0..<selection.min)

  // Always open when empty in front
  if (subStringBeforeCursor.isEmpty()) {
    addTokens(Token.Operator.LEFT_BRACKET)
    return
  }

  // Always close before operator
  val openBeforeOperators =
    listOf(
      Token.Operator.MULTIPLY,
      Token.Operator.DIVIDE,
      Token.Operator.PLUS,
      Token.Operator.MINUS,
      Token.Operator.POWER,
    )
  if (text.toString().tokenAfter(selection.min) in openBeforeOperators) {
    addTokens(Token.Operator.RIGHT_BRACKET)
    return
  }

  // Always open when balanced in front
  val leftBracketChar: Char = Token.Operator.LEFT_BRACKET.first()
  val rightBracketChar: Char = Token.Operator.RIGHT_BRACKET.first()
  var balance = 0
  subStringBeforeCursor.forEach {
    if (it == leftBracketChar) balance += 1
    if (it == rightBracketChar) balance -= 1
  }
  if (balance == 0) {
    addTokens(Token.Operator.LEFT_BRACKET)
    return
  }

  // Always open after operator
  val openAfterOperators =
    listOf(
      Token.Operator.MULTIPLY,
      Token.Operator.DIVIDE,
      Token.Operator.PLUS,
      Token.Operator.MINUS,
      Token.Operator.POWER,
      Token.Operator.LEFT_BRACKET,
    )
  if (text.toString().tokenAhead(selection.min) in openAfterOperators) {
    addTokens(Token.Operator.LEFT_BRACKET)
    return
  }

  closeBracket()
}

/**
 * Closing bracket that is already there (from templates like `d/dx()`) is skipped instead of adding
 * one more. After `d/dx(...)` cursor goes into the point slot.
 */
private fun TextFieldState.closeBracket() {
  val cursor = selection.min
  // Unclosed brackets: this one belongs to them, add a new one: d/dx(cos(x|) ==> d/dx(cos(x)|)
  val unclosed =
    text.count { it == Token.Operator.LEFT_BRACKET.first() } -
      text.count { it == Token.Operator.RIGHT_BRACKET.first() }
  val isNextClosing = text.getOrNull(cursor) == Token.Operator.RIGHT_BRACKET.first()
  if (!selection.collapsed || !isNextClosing || unclosed > 0) {
    addTokens(Token.Operator.RIGHT_BRACKET)
    return
  }
  val pointSlot = Token.Calculus.EVALUATED_AT + Token.Layout.SUBSCRIPT_START
  val next = if (text.startsWith(pointSlot, cursor + 1)) cursor + 1 + pointSlot.length else cursor + 1
  edit { selection = TextRange(next) }
}

/**
 * Inside of a slot bracket button also works as a way out of it (there are no arrow keys):
 * - Opens after operators and at the start of the slot
 * - Closes if there are unclosed brackets in this slot
 * - Otherwise moves cursor out of the slot (or into the next slot of the same template)
 */
private fun TextFieldState.addBracketInSlot(slotStart: Int) {
  val textBeforeCursor = text.substring(slotStart + 1, selection.min)
  val tokenAhead = text.toString().tokenAhead(selection.min)
  val openAfter =
    listOf(
      Token.Operator.MULTIPLY,
      Token.Operator.DIVIDE,
      Token.Operator.PLUS,
      Token.Operator.MINUS,
      Token.Operator.POWER,
      Token.Operator.LEFT_BRACKET,
    )

  if (textBeforeCursor.isEmpty() || tokenAhead in openAfter) {
    addTokens(Token.Operator.LEFT_BRACKET)
    return
  }

  val balance =
    textBeforeCursor.count { it == Token.Operator.LEFT_BRACKET.first() } -
      textBeforeCursor.count { it == Token.Operator.RIGHT_BRACKET.first() }
  if (balance > 0) {
    closeBracket()
    return
  }

  val slotEnd = text.slotEnd(slotStart)
  if (slotEnd == -1) {
    addTokens(Token.Operator.RIGHT_BRACKET)
    return
  }

  // Integral: from lower bound go straight into upper bound. Same for fractions
  val nextSlotStart = slotEnd + 1
  var cursor =
    if (text.getOrNull(nextSlotStart)?.isSecondSlot() == true) nextSlotStart + 1 else slotEnd + 1
  // Sum and product: from upper bound go into empty body
  if (text.startsWith(EMPTY_BRACKETS, cursor)) cursor += 1
  edit { selection = TextRange(cursor) }
}

fun TextFieldState.deleteTokens() {
  if (selection.collapsed && deleteInSlot()) return

  val deleteRangeStart =
    when (selection.max) {
      // Don't delete if at the start of the text field
      0 -> return
      // We don't have anything selected (cursor in one position)
      // like this 1234|56 => after deleting will be like this 123|56
      // Cursor moved one symbol left
      selection.min -> selection.min - text.toString().tokenLengthAhead(selection.max)
      // We have multiple symbols selected
      // like this 123[45]6 => after deleting will be like this 123|6
      // Cursor will be placed where selection start was
      else -> selection.min
    }

  this.edit { delete(deleteRangeStart, selection.max) }
}

/**
 * Backspace around slots:
 * - Right after a slot: deletes last token inside of it, cursor goes into the slot
 * - At the start of an empty slot: deletes whole template (like `^⟨⟩`)
 * - At the start of a non-empty slot: moves cursor out of it
 *
 * @return `false` if nothing was handled and regular delete should be used.
 */
private fun TextFieldState.deleteInSlot(): Boolean {
  val cursor = selection.min
  val charBefore = text.getOrNull(cursor - 1) ?: return false

  if (charBefore.isSlotStart()) {
    deleteAtSlotStart(cursor - 1)
    return true
  }

  if (charBefore == SLOT_END) {
    val slotStart = text.slotStart(cursor - 1)
    if (slotStart == -1) return false
    if (slotStart == cursor - 2) {
      // empty slot
      deleteAtSlotStart(slotStart)
    } else if (text[cursor - 2] == SLOT_END) {
      // nested slot is last, step into the outer slot first
      edit { selection = TextRange(cursor - 1) }
    } else {
      val slotEnd = cursor - 1
      val deleteLength = text.toString().tokenLengthAhead(slotEnd)
      edit {
        delete(slotEnd - deleteLength, slotEnd)
        selection = TextRange(slotEnd - deleteLength)
      }
    }
    return true
  }

  return false
}

private fun TextFieldState.deleteAtSlotStart(slotStart: Int) {
  val text = text.toString()
  val slotEnd = text.slotEnd(slotStart)
  val isEmpty = slotEnd == slotStart + 1
  val charBefore = text.getOrNull(slotStart - 1)

  // Integral upper bound or denominator, go to the end of previous slot
  if (charBefore == SLOT_END && text[slotStart].isSecondSlot()) {
    edit { selection = TextRange(slotStart - 1) }
    return
  }

  // Owner of this slot, for example "^" for power. Deleted together with an empty slot
  val ownerLength =
    when {
      text[slotStart] == SLOT_SUP && charBefore == Token.Operator.POWER.first() ->
        Token.Operator.POWER.length
      text[slotStart] == SLOT_SUB && text.startsWith(Token.Func.LOG, slotStart - Token.Func.LOG.length) ->
        Token.Func.LOG.length
      text[slotStart] == SLOT_SUB && charBefore?.isPairOwner() == true -> 1
      text[slotStart] == SLOT_SUB && charBefore == Token.Calculus.EVALUATED_AT.first() ->
        Token.Calculus.EVALUATED_AT.length
      text[slotStart] == SLOT_RAD && charBefore == Token.Operator.SQRT.first() ->
        Token.Operator.SQRT.length
      else -> 0
    }
  val ownerStart = slotStart - ownerLength

  if (!isEmpty || slotEnd == -1) {
    edit { selection = TextRange(ownerStart) }
    return
  }

  var deleteEnd = slotEnd + 1
  val isPair = charBefore?.isPairOwner() == true || text[slotStart] == SLOT_NUM
  if (isPair) {
    // Integral and fraction are deleted only when both slots are empty
    val upperStart = slotEnd + 1
    if (text.getOrNull(upperStart)?.isSlotStart() == true) {
      if (text.slotEnd(upperStart) != upperStart + 1) {
        edit { selection = TextRange(ownerStart) }
        return
      }
      deleteEnd = upperStart + 2
    }
    // Remove "dx" too if there is nothing to integrate
    val isIntegral = charBefore == Token.Calculus.INTEGRAL.first()
    if (isIntegral && text.startsWith(Token.Calculus.DX, deleteEnd)) {
      deleteEnd += Token.Calculus.DX.length
    }
    // Empty body of sum or product
    val isSeries = charBefore?.isPairOwner() == true && !isIntegral
    if (isSeries && text.startsWith(EMPTY_BRACKETS, deleteEnd)) {
      deleteEnd += EMPTY_BRACKETS.length
    }
  }

  edit {
    delete(ownerStart, deleteEnd)
    selection = TextRange(ownerStart)
  }
}

/** ∫, Σ and ∏ have two slots (bounds) deleted together. */
private fun Char.isPairOwner() =
  this == Token.Calculus.INTEGRAL.first() ||
    this == Token.Calculus.SUM.first() ||
    this == Token.Calculus.PRODUCT.first()

private const val EMPTY_BRACKETS = "()"

/** Second slot of a template: integral upper bound or fraction denominator. */
private fun Char.isSecondSlot() = this == SLOT_SUP || this == SLOT_DEN

fun TextFieldState.placeCursorAtTheEnd() = edit { placeCursorAtEnd() }

/**
 * Tries to get a [TextFieldState]. Places cursor at the end.
 *
 * @param key Key to find
 * @return [TextFieldState] with cursor at the end.
 * @receiver [SavedStateHandle] Where to look for.
 */
fun SavedStateHandle.getTextFieldState(key: String): TextFieldState =
  with(get(key) ?: "") { TextFieldState(this, TextRange(this.length)) }

@OptIn(FlowPreview::class)
fun TextFieldState.observe(debounceMs: Long = TEXT_FIELD_STATE_DEBOUNCE_MS) =
  snapshotFlow { this.text }.debounce(debounceMs)

/** <b>!!! Recursive !!!</b> (one wrong step and you are dead 💀) */
private fun TextFieldState.deleteAheadAndAdd(tokens: String) {
  // For cases like: "12+[34]" and "*" symbol is being added. Will delete selected tokens
  if (!selection.collapsed) {
    this.edit { delete(this.selection.min, this.selection.max) }
  }
  this.deleteTokens()
  this.addTokens(tokens)
}

private const val TEXT_FIELD_STATE_DEBOUNCE_MS = 50L
