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

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.unit.em
import com.sadellie.unitto.core.common.FormatterSymbols
import com.sadellie.unitto.core.common.Token
import kotlin.math.pow

// Slots are superscripts and subscripts, see Token.Layout
internal val SLOT_SUP = Token.Layout.SUPERSCRIPT_START.first()
internal val SLOT_SUB = Token.Layout.SUBSCRIPT_START.first()
internal val SLOT_END = Token.Layout.END.first()
internal val SLOT_NUM = Token.Layout.NUMERATOR_START.first()
internal val SLOT_DEN = Token.Layout.DENOMINATOR_START.first()
internal val SLOT_RAD = Token.Layout.RADICAND_START.first()

internal enum class SlotKind {
  SUPERSCRIPT,
  SUBSCRIPT,
  NUMERATOR,
  DENOMINATOR,
  RADICAND,
}

internal fun Char.toSlotKind(): SlotKind? =
  when (this) {
    SLOT_SUP -> SlotKind.SUPERSCRIPT
    SLOT_SUB -> SlotKind.SUBSCRIPT
    SLOT_NUM -> SlotKind.NUMERATOR
    SLOT_DEN -> SlotKind.DENOMINATOR
    SLOT_RAD -> SlotKind.RADICAND
    else -> null
  }

internal fun Char.isSlotStart() = toSlotKind() != null

/**
 * Slots that contain [pos], from outermost to innermost. Empty when [pos] is not inside of any
 * slot.
 */
internal fun CharSequence.slotsAt(pos: Int): List<SlotKind> {
  val slots = mutableListOf<SlotKind>()
  for (index in 0..<pos.coerceAtMost(length)) {
    val kind = this[index].toSlotKind()
    when {
      kind != null -> slots.add(kind)
      this[index] == SLOT_END -> slots.removeLastOrNull()
    }
  }
  return slots
}

/** Only superscripts and subscripts change text size and position. */
private fun List<SlotKind>.scripts(): List<Boolean> =
  mapNotNull {
    when (it) {
      SlotKind.SUPERSCRIPT -> true
      SlotKind.SUBSCRIPT -> false
      else -> null
    }
  }

/** Index of [SLOT_END] that closes slot started at [start]. -1 if not closed. */
internal fun CharSequence.slotEnd(start: Int): Int {
  var depth = 0
  for (index in start..<length) {
    when {
      this[index].isSlotStart() -> depth++
      this[index] == SLOT_END -> {
        depth--
        if (depth == 0) return index
      }
    }
  }
  return -1
}

/** Index of slot start that is closed by [SLOT_END] at [end]. -1 if not found. */
internal fun CharSequence.slotStart(end: Int): Int {
  var depth = 0
  for (index in end downTo 0) {
    when {
      this[index] == SLOT_END -> depth++
      this[index].isSlotStart() -> {
        depth--
        if (depth == 0) return index
      }
    }
  }
  return -1
}

/** Index of the start of the innermost slot that contains [pos]. -1 if not inside of a slot. */
internal fun CharSequence.enclosingSlotStart(pos: Int): Int {
  var depth = 0
  for (index in (pos - 1) downTo 0) {
    when {
      this[index] == SLOT_END -> depth++
      this[index].isSlotStart() -> {
        if (depth == 0) return index
        depth--
      }
    }
  }
  return -1
}

/**
 * Where to put cursor after inserting a template. Index of the first empty place, cursor goes
 * right after it: empty slot start, empty brackets `()` or empty integral `∫dx`. -1 if none.
 */
internal fun CharSequence.indexOfEmptySlot(): Int =
  (0..<lastIndex).firstOrNull { index ->
    (this[index].isSlotStart() && this[index + 1] == SLOT_END) ||
      startsWith(EMPTY_BRACKETS, index) ||
      startsWith(Token.Layout.INDEFINITE_INTEGRAL, index)
  } ?: -1

/**
 * Index of the integral at [cursor] that has no bounds, null when there is none. Results and old
 * calculations write `∫x dx` without the empty bounds of the keyboard.
 */
internal fun CharSequence.boundlessIntegralAt(cursor: Int): Int? =
  listOf(cursor, cursor - 1).firstOrNull { index ->
    getOrNull(index)?.toString() == Token.Calculus.INTEGRAL &&
      getOrNull(index + 1)?.toString() != Token.Layout.SUBSCRIPT_START
  }

/** Empty bounds of an integral, they go right after its sign. */
internal const val INTEGRAL_BOUNDS =
  "${Token.Layout.SUBSCRIPT_START}${Token.Layout.END}${Token.Layout.SUPERSCRIPT_START}${Token.Layout.END}"

private const val EMPTY_BRACKETS = Token.Operator.LEFT_BRACKET + Token.Operator.RIGHT_BRACKET

/**
 * How to display slots without changing text length (so that cursor positions map one to one):
 * - Markers and power operator before superscript become invisible
 * - Empty slots get a placeholder
 * - Text inside of slots is smaller and shifted up (superscript) or down (subscript)
 */
internal class SlotRendering(
  val replacements: Map<Int, Char>,
  val styles: List<AnnotatedString.Range<SpanStyle>>,
)

internal fun CharSequence.renderSlots(placeholderColor: Color): SlotRendering {
  if (none { it.isSlotStart() || it == SLOT_END }) return SlotRendering(emptyMap(), emptyList())

  val replacements = mutableMapOf<Int, Char>()
  val styles = mutableListOf<AnnotatedString.Range<SpanStyle>>()
  val slots = mutableListOf<SlotKind>()
  var runStart = 0
  var runSlots = emptyList<Boolean>()

  fun closeRun(end: Int) {
    if (runSlots.isEmpty() || end <= runStart) return
    styles.add(AnnotatedString.Range(slotSpanStyle(runSlots), runStart, end))
  }

  for (index in indices) {
    val char = this[index]
    // slot start belongs to the slot it opens, slot end to the slot it closes
    val kind = char.toSlotKind()
    if (kind != null) {
      slots.add(kind)
      replacements[index] = kind.startReplacement()
      if (char == SLOT_SUP && getOrNull(index - 1) == POWER_CHAR) {
        replacements[index - 1] = INVISIBLE_CHAR
      }
    }

    val scripts = slots.scripts()
    if (scripts != runSlots) {
      closeRun(index)
      runStart = index
      runSlots = scripts
    }

    if (char == SLOT_END) {
      val replacement = endReplacement(index, slots.lastOrNull())
      replacements[index] = replacement
      if (replacement == PLACEHOLDER_CHAR) {
        styles.add(AnnotatedString.Range(SpanStyle(color = placeholderColor), index, index + 1))
      }
      slots.removeLastOrNull()
    }
  }
  closeRun(length)

  return SlotRendering(replacements, styles)
}

/** What a slot start is drawn as. Fractions can't be drawn here, they are shown in one line: (1)/2 */
private fun SlotKind.startReplacement(): Char =
  when (this) {
    SlotKind.NUMERATOR,
    SlotKind.RADICAND -> '('
    SlotKind.DENOMINATOR -> FRACTION_SLASH
    else -> INVISIBLE_CHAR
  }

/** What the slot end at [index] of a [kind] slot is drawn as: a bracket, the placeholder or nothing. */
private fun CharSequence.endReplacement(index: Int, kind: SlotKind?): Char =
  when {
    kind == SlotKind.RADICAND -> ')'
    kind == SlotKind.NUMERATOR && getOrNull(index - 1) != SLOT_NUM -> ')'
    getOrNull(index - 1)?.isSlotStart() == true -> PLACEHOLDER_CHAR
    else -> INVISIBLE_CHAR
  }

/** Same as [formatExpression], but with slots rendered. For read only texts. */
fun String.formatExpressionWithSlots(
  formatterSymbols: FormatterSymbols,
  placeholderColor: Color = Color.Unspecified,
): AnnotatedString {
  val formatted = formatExpression(formatterSymbols)
  val rendering = formatted.renderSlots(placeholderColor)
  val chars = formatted.toCharArray()
  rendering.replacements.forEach { (index, char) -> chars[index] = char }
  return AnnotatedString(chars.concatToString(), rendering.styles)
}

private fun slotSpanStyle(slots: List<Boolean>): SpanStyle {
  val fontScale = SLOT_FONT_SCALE.pow(slots.size)
  // Each level is shifted relative to it's parent, but BaselineShift is relative to the font size
  // of this span
  val shift =
    slots.foldIndexed(0f) { level, acc, isSuperscript ->
      acc + slotShift(isSuperscript) * SLOT_FONT_SCALE.pow(level)
    }
  return SpanStyle(fontSize = fontScale.em, baselineShift = BaselineShift(shift / fontScale))
}

private fun slotShift(isSuperscript: Boolean) =
  if (isSuperscript) SUPERSCRIPT_SHIFT else -SUBSCRIPT_SHIFT

/**
 * Cursor is always drawn for the whole line height. This brush hides parts of the cursor so it
 * looks smaller and shifted when it is inside of a slot.
 *
 * @param slots See [slotsAt].
 */
internal fun slotCursorBrush(color: Color, slotKinds: List<SlotKind>): Brush {
  val slots = slotKinds.scripts()
  if (slots.isEmpty()) return SolidColor(color)

  // Approximate line metrics, fractions of line height
  var baseline = LINE_BASELINE
  slots.forEachIndexed { level, isSuperscript ->
    baseline -= slotShift(isSuperscript) * LINE_BASELINE * SLOT_FONT_SCALE.pow(level)
  }
  val fontScale = SLOT_FONT_SCALE.pow(slots.size)
  val top = (baseline - LINE_BASELINE * fontScale).coerceIn(0f, 1f)
  val bottom = (baseline + (1f - LINE_BASELINE) * fontScale).coerceIn(top, 1f)

  return Brush.verticalGradient(
    0f to Color.Transparent,
    top to Color.Transparent,
    top to color,
    bottom to color,
    bottom to Color.Transparent,
    1f to Color.Transparent,
  )
}

private val POWER_CHAR = Token.Operator.POWER.first()
private const val INVISIBLE_CHAR = '\u200B' // ZERO WIDTH SPACE
private const val PLACEHOLDER_CHAR = '□'
private const val FRACTION_SLASH = '⁄'
private const val SLOT_FONT_SCALE = 0.65f
private const val SUPERSCRIPT_SHIFT = 0.45f
private const val SUBSCRIPT_SHIFT = 0.25f
private const val LINE_BASELINE = 0.79f

/** Cursor at [pos] is in lower or upper bound of ∫, Σ or ∏. */
fun CharSequence.isInCalculusBound(pos: Int): Boolean {
  val slotStart = enclosingSlotStart(pos)
  val before = if (slotStart > 0) this[slotStart - 1] else null
  // upper bound: owner⟨lower⟩⟨upper⟩
  val lowerStart = if (before == SLOT_END) slotStart(slotStart - 1) else -1
  return (before != null && before in CALCULUS_OWNERS) || (lowerStart > 0 && this[lowerStart - 1] in CALCULUS_OWNERS)
}

/** Cursor at [pos] is after unit conversion arrow: `3_km→|` */
fun CharSequence.isAfterConversion(pos: Int): Boolean =
  subSequence(0, pos.coerceAtMost(length)).contains(Token.Operator.CONVERT)

/** Cursor at [pos] is in bounds or body of Σ or ∏: `Σ⟨n=1⟩⟨∞⟩(1/n|)` */
fun CharSequence.isInSeries(pos: Int): Boolean =
  (0..<pos.coerceAtMost(length)).reversed().any { owner ->
    this[owner] in SERIES_OWNERS && seriesContains(owner, pos)
  }

/** [pos] is in the bounds or body of the Σ or ∏ at [owner], or they aren't closed yet. */
private fun CharSequence.seriesContains(owner: Int, pos: Int): Boolean {
  // lower and upper bounds
  var index = owner + 1
  var isOpen = false
  repeat(2) {
    if (!isOpen && index < length && this[index].isSlotStart()) {
      val end = slotEnd(index)
      if (end == -1) isOpen = true else index = end + 1
    }
  }
  val hasBody = !isOpen && index < length && this[index] == LEFT_BRACKET
  val bodyEnd = if (hasBody) matchingBracket(index) else -1
  return isOpen || pos < index || (hasBody && (bodyEnd == -1 || pos <= bodyEnd))
}

/** Index of the bracket that closes the one at [open], -1 when it is not closed. */
private fun CharSequence.matchingBracket(open: Int): Int {
  var depth = 0
  for (index in open..<length) {
    when (this[index]) {
      LEFT_BRACKET -> depth++
      RIGHT_BRACKET -> depth--
    }
    if (depth == 0) return index
  }
  return -1
}

private val SERIES_OWNERS = listOf(Token.Calculus.SUM, Token.Calculus.PRODUCT).map { it.first() }
private val LEFT_BRACKET = Token.Operator.LEFT_BRACKET.first()
private val RIGHT_BRACKET = Token.Operator.RIGHT_BRACKET.first()

private val CALCULUS_OWNERS =
  listOf(Token.Calculus.INTEGRAL, Token.Calculus.SUM, Token.Calculus.PRODUCT).map { it.first() }
