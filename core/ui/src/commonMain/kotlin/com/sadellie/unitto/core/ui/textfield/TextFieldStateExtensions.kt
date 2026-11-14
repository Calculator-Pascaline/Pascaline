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
  if (tokens == Token.Layout.FRACTION) return addFraction()
  when (actionBefore(tokens)) {
    TokenAction.REPLACE_AHEAD -> deleteAheadAndAdd(tokens)
    TokenAction.DELETE -> deleteTokens()
    TokenAction.INSERT -> insertTokens(tokens)
  }
}

/** What typing [tokens] does with what is before the cursor. */
private enum class TokenAction {
  /** An operator before is replaced: 2+ then × ==> 2× */
  REPLACE_AHEAD,

  /** An operator at the start deletes instead */
  DELETE,
  INSERT,
}

private fun TextFieldState.actionBefore(tokens: String): TokenAction {
  val tokenAhead = text.toString().tokenAhead(selection.min)
  val replaced =
    when (tokens) {
      in BINARY_OPERATORS -> REPLACED_BY_OPERATOR
      Token.Operator.MINUS -> REPLACED_BY_MINUS
      Token.Digit.DOT -> setOf(Token.Digit.DOT)
      else -> emptySet()
    }
  return when {
    tokenAhead in replaced -> TokenAction.REPLACE_AHEAD
    tokenAhead == "" && tokens in BINARY_OPERATORS -> TokenAction.DELETE
    else -> TokenAction.INSERT
  }
}

/** Operators that need something before them */
private val BINARY_OPERATORS =
  setOf(
    Token.Operator.PLUS,
    Token.Operator.MULTIPLY,
    Token.Operator.DIVIDE,
    Token.Operator.POWER,
    Token.Layout.POWER,
  )

private val REPLACED_BY_OPERATOR =
  setOf(
    Token.Operator.PLUS,
    Token.Operator.MINUS,
    Token.Operator.MULTIPLY,
    Token.Operator.DIVIDE,
    Token.Operator.SQRT,
    Token.Operator.POWER,
  )

private val REPLACED_BY_MINUS = setOf(Token.Operator.PLUS, Token.Operator.MINUS)

private fun TextFieldState.insertTokens(tokens: String) {
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
  val previous = getOrNull(pos - 1)
  return when {
    previous == null -> pos
    previous in digits -> {
      var start = pos
      while (getOrNull(start - 1) in digits) start--
      start
    }
    previous.toString() in Token.Const.all -> pos - 1
    previous == Token.Operator.RIGHT_BRACKET.first() -> bracketOperandStart(pos)
    else -> pos
  }
}

/** Start of the brackets that end at [pos], with the function they belong to: sin(…). */
private fun String.bracketOperandStart(pos: Int): Int {
  var balance = 0
  var start = pos - 1
  while (start >= 0) {
    if (this[start] == Token.Operator.RIGHT_BRACKET.first()) balance++
    if (this[start] == Token.Operator.LEFT_BRACKET.first()) balance--
    if (balance == 0) break
    start--
  }
  val function = Token.Func.all.firstOrNull { start >= 0 && startsWith(it, start - it.length) }
  return if (start < 0) pos else start - (function?.length ?: 0)
}

fun TextFieldState.addBracket() {
  val slotStart = text.enclosingSlotStart(selection.min)
  val textBeforeCursor = text.substring(0..<selection.min)
  // Opened when balanced in front, or after an operator or a function
  val opens =
    textBeforeCursor.bracketBalance() == 0 || text.toString().tokenAhead(selection.min) in OPEN_AFTER
  when {
    slotStart != -1 -> addBracketInSlot(slotStart)
    // Always open when empty in front
    textBeforeCursor.isEmpty() -> addTokens(Token.Operator.LEFT_BRACKET)
    // Always close before operator
    text.toString().tokenAfter(selection.min) in CLOSE_BEFORE -> addTokens(Token.Operator.RIGHT_BRACKET)
    opens -> addTokens(Token.Operator.LEFT_BRACKET)
    else -> closeBracket()
  }
}

/** Opened brackets minus closed ones. */
private fun CharSequence.bracketBalance(): Int =
  count { it == Token.Operator.LEFT_BRACKET.first() } - count { it == Token.Operator.RIGHT_BRACKET.first() }

private val CLOSE_BEFORE =
  listOf(
    Token.Operator.MULTIPLY,
    Token.Operator.DIVIDE,
    Token.Operator.PLUS,
    Token.Operator.MINUS,
    Token.Operator.POWER,
  )

private val OPEN_AFTER by lazy { CLOSE_BEFORE + Token.Operator.LEFT_BRACKET + Token.Func.allWithOpeningBracket }

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
  val slotEnd = text.slotEnd(slotStart)
  when {
    textBeforeCursor.isEmpty() || tokenAhead in OPEN_AFTER -> addTokens(Token.Operator.LEFT_BRACKET)
    textBeforeCursor.bracketBalance() > 0 -> closeBracket()
    slotEnd == -1 -> addTokens(Token.Operator.RIGHT_BRACKET)
    else -> leaveSlot(slotEnd)
  }
}

/** Cursor after the slot that ends at [slotEnd], or in the next slot of the same template. */
private fun TextFieldState.leaveSlot(slotEnd: Int) {
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
  val charBefore = text.getOrNull(cursor - 1)
  val slotStart = if (charBefore == SLOT_END) text.slotStart(cursor - 1) else -1
  val isAtSlotStart = charBefore?.isSlotStart() == true
  when {
    isAtSlotStart -> deleteAtSlotStart(cursor - 1)
    slotStart == -1 -> Unit
    // empty slot
    slotStart == cursor - 2 -> deleteAtSlotStart(slotStart)
    // nested slot is last, step into the outer slot first
    text[cursor - 2] == SLOT_END -> edit { selection = TextRange(cursor - 1) }
    else -> deleteLastInSlot(slotEnd = cursor - 1)
  }
  return isAtSlotStart || slotStart != -1
}

/** Last token in the slot that ends at [slotEnd], the cursor stays in the slot. */
private fun TextFieldState.deleteLastInSlot(slotEnd: Int) {
  val deleteLength = text.toString().tokenLengthAhead(slotEnd)
  edit {
    delete(slotEnd - deleteLength, slotEnd)
    selection = TextRange(slotEnd - deleteLength)
  }
}

private fun TextFieldState.deleteAtSlotStart(slotStart: Int) {
  if (deleteEmptyNumerator(slotStart)) return
  val text = text.toString()
  val slotEnd = text.slotEnd(slotStart)
  val isEmpty = slotEnd == slotStart + 1
  val charBefore = text.getOrNull(slotStart - 1)
  // Empty denominator: fraction goes away, numerator stays. ⟨5⟩⟨|⟩ ==> 5|
  val numeratorStart =
    if (charBefore == SLOT_END && text[slotStart] == SLOT_DEN && isEmpty) text.slotStart(slotStart - 1) else -1
  val ownerStart = slotStart - text.slotOwnerLength(slotStart, charBefore)
  // Where an empty template ends, null when it can't go (a slot isn't empty)
  val deleteEnd = if (isEmpty && slotEnd != -1) text.emptyTemplateEnd(slotStart, slotEnd, charBefore) else null
  when {
    numeratorStart != -1 ->
      edit {
        delete(slotStart - 1, slotEnd + 1)
        delete(numeratorStart, numeratorStart + 1)
        selection = TextRange(slotStart - 2)
      }
    // Integral upper bound or denominator, go to the end of previous slot
    charBefore == SLOT_END && text[slotStart].isSecondSlot() -> edit { selection = TextRange(slotStart - 1) }
    deleteEnd == null -> edit { selection = TextRange(ownerStart) }
    else ->
      edit {
        delete(ownerStart, deleteEnd)
        selection = TextRange(ownerStart)
      }
  }
}

/**
 * A fraction with nothing on top loses its bar and the denominator stays, from the start of the
 * denominator or from the empty numerator: `N}D|2}` and `N|}D2}` become `|2`.
 *
 * @return `false` when the slot at [slotStart] isn't part of such a fraction.
 */
private fun TextFieldState.deleteEmptyNumerator(slotStart: Int): Boolean {
  val text = text.toString()
  val numeratorStart = if (text[slotStart] == SLOT_DEN) slotStart - 2 else slotStart
  val denominatorStart = numeratorStart + 2
  val isEmptyNumerator =
    text.getOrNull(numeratorStart) == SLOT_NUM &&
      text.getOrNull(numeratorStart + 1) == SLOT_END &&
      text.getOrNull(denominatorStart) == SLOT_DEN
  val denominatorEnd = if (isEmptyNumerator) text.slotEnd(denominatorStart) else -1
  // An empty denominator too: the whole fraction goes, like other empty templates
  if (denominatorEnd == -1 || denominatorEnd == denominatorStart + 1) return false
  edit {
    delete(denominatorEnd, denominatorEnd + 1)
    delete(numeratorStart, denominatorStart + 1)
    selection = TextRange(numeratorStart)
  }
  return true
}

/** Owner of the slot at [slotStart], for example "^" for power. Deleted together with an empty slot. */
private fun String.slotOwnerLength(slotStart: Int, charBefore: Char?): Int =
  when {
    this[slotStart] == SLOT_SUP && charBefore == Token.Operator.POWER.first() -> Token.Operator.POWER.length
    this[slotStart] == SLOT_SUB && startsWith(Token.Func.LOG, slotStart - Token.Func.LOG.length) ->
      Token.Func.LOG.length
    this[slotStart] == SLOT_SUB && charBefore?.isPairOwner() == true -> 1
    this[slotStart] == SLOT_SUB && charBefore == Token.Calculus.EVALUATED_AT.first() ->
      Token.Calculus.EVALUATED_AT.length
    this[slotStart] == SLOT_RAD && charBefore == Token.Operator.SQRT.first() -> Token.Operator.SQRT.length
    else -> 0
  }

/**
 * End of the template of the empty slot from [slotStart] to [slotEnd]. Integral, sum, product and
 * fraction go only when both slots are empty: null when the other one isn't.
 */
private fun String.emptyTemplateEnd(slotStart: Int, slotEnd: Int, charBefore: Char?): Int? {
  var deleteEnd = slotEnd + 1
  val isPair = charBefore?.isPairOwner() == true || this[slotStart] == SLOT_NUM
  val upperStart = slotEnd + 1
  val hasUpper = isPair && getOrNull(upperStart)?.isSlotStart() == true
  if (hasUpper && slotEnd(upperStart) != upperStart + 1) return null
  if (hasUpper) deleteEnd = upperStart + 2
  // Remove "dx" too if there is nothing to integrate
  val isIntegral = charBefore == Token.Calculus.INTEGRAL.first()
  if (isPair && isIntegral && startsWith(Token.Calculus.DX, deleteEnd)) deleteEnd += Token.Calculus.DX.length
  // Empty body of sum or product
  val isSeries = charBefore?.isPairOwner() == true && !isIntegral
  if (isPair && isSeries && startsWith(EMPTY_BRACKETS, deleteEnd)) deleteEnd += EMPTY_BRACKETS.length
  return deleteEnd
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
