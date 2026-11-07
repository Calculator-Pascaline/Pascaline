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

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Density
import com.sadellie.unitto.core.common.FormatterSymbols
import com.sadellie.unitto.core.common.PhysicalUnit
import com.sadellie.unitto.core.common.PhysicalUnits
import com.sadellie.unitto.core.common.Token
import kotlin.math.abs
import kotlin.math.max

/**
 * Two-dimensional layout of an expression with slots (like NumWorks Epsilon): superscripts and
 * subscripts are smaller and shifted, fractions are drawn with a horizontal bar. All coordinates
 * are in pixels relative to the baseline of the expression (y = 0), negative y is up.
 *
 * @property carets Every position in raw text where cursor can be placed.
 */
internal data class ExpressionLayout(
  val width: Float,
  val ascent: Float,
  val descent: Float,
  val texts: List<PlacedText>,
  val bars: List<Rect>,
  val placeholders: List<PlacedPlaceholder>,
  val strokes: List<PlacedStroke>,
  val carets: List<Caret>,
) {
  val height: Float
    get() = ascent + descent

  /** Caret for cursor at [index]. Illegal positions use the closest legal one. */
  fun caretFor(index: Int): Caret? =
    carets.firstOrNull { it.index == index }
      ?: carets.minByOrNull { abs(it.index - index) }

  /** Index in raw text for a tap at [x], [y]. */
  fun indexAt(x: Float, y: Float): Int? =
    carets
      .minByOrNull { caret ->
        val dy =
          when {
            y < caret.top -> caret.top - y
            y > caret.bottom -> y - caret.bottom
            else -> 0f
          }
        abs(caret.x - x) + dy * VERTICAL_TAP_WEIGHT
      }
      ?.index

  /** Index of the integral without bounds in [text] whose ∫ sign is under [x], [y]. */
  fun boundlessIntegralAt(text: CharSequence, x: Float, y: Float): Int? {
    val integral = indexAt(x, y)?.let(text::boundlessIntegralAt) ?: return null
    val left = caretFor(integral)?.x ?: return null
    val right = caretFor(integral + 1)?.x ?: return null
    return integral.takeIf { x in left..right }
  }
}

internal class PlacedText(val layout: TextLayoutResult, val x: Float, val baseline: Float)

/**
 * Soft key: empty slot, or angle box of a trigonometric function when [angleBoxIndex] is the
 * index of its unit in raw text.
 */
internal class PlacedPlaceholder(val rect: Rect, val strokeWidth: Float, val angleBoxIndex: Int? = null)

/** Line through [points], for example square root sign. */
internal class PlacedStroke(val points: List<Offset>, val width: Float)

internal class Caret(val index: Int, val x: Float, val top: Float, val bottom: Float)

internal fun TextMeasurer.layoutExpression(
  text: String,
  style: TextStyle,
  formatterSymbols: FormatterSymbols,
  density: Density,
): ExpressionLayout {
  val fontSize = with(density) { style.fontSize.toPx() }
  val builder = LayoutBuilder(this, style, formatterSymbols, density)
  val (nodes, _) = text.parseLayout(0)
  val box = builder.layoutRow(nodes, 0, text.length, fontSize, isTopLevel = true)
  return ExpressionLayout(
    width = box.width,
    ascent = box.ascent,
    descent = box.descent,
    texts = box.texts,
    bars = box.bars,
    placeholders = box.placeholders,
    strokes = box.strokes,
    // cursor can't be placed in some positions (inside of "sin(" or between "^" and superscript)
    carets = box.carets.filter { text.fixCursor(it.index) == it.index }.distinctBy { it.index },
  )
}

/** Expression tree. Indices point to positions in raw text. */
private sealed interface LayoutNode {
  class Text(val start: Int, val text: String) : LayoutNode

  /** Power operator before superscript or square root before radicand, never drawn as text. */
  class Hidden(val index: Int) : LayoutNode

  /** `d/dx`, drawn as a small fraction. */
  class Derivative(val start: Int) : LayoutNode

  /** Unit token like `_km`, drawn as `km`. */
  class UnitToken(val index: Int, val unit: PhysicalUnit) : LayoutNode

  /** Σ or ∏ before its bounds, drawn big with bounds above and below. */
  class BigOperator(val index: Int, val symbol: String) : LayoutNode

  /** ∫, drawn taller than text around it. */
  class IntegralSign(val index: Int) : LayoutNode

  /**
   * @property start Index of slot start marker.
   * @property end Index of slot end marker (text length if slot is not closed).
   */
  class Slot(val kind: SlotKind, val start: Int, val end: Int, val content: List<LayoutNode>) :
    LayoutNode
}

/** @return Nodes and index where parsing stopped (slot end or text length). */
private fun String.parseLayout(from: Int): Pair<List<LayoutNode>, Int> {
  val nodes = mutableListOf<LayoutNode>()
  var index = from
  var textStart = from
  while (index < length && this[index] != SLOT_END) {
    val special = specialNodeAt(index)
    if (special == null) {
      index++
    } else {
      // Text before it is a node too
      if (index > textStart) nodes.add(LayoutNode.Text(textStart, substring(textStart, index)))
      nodes.add(special.first)
      index = special.second
      textStart = index
    }
  }
  index = index.coerceAtMost(length)
  if (index > textStart) nodes.add(LayoutNode.Text(textStart, substring(textStart, index)))
  return nodes to index
}

/** Node that is not plain text at [index], with the index after it. Null for plain text. */
private fun String.specialNodeAt(index: Int): Pair<LayoutNode, Int>? {
  val char = this[index]
  val kind = char.toSlotKind()
  val unit = PhysicalUnits.tokenAt(this, index)
  val isHidden =
    (char == POWER_CHAR && getOrNull(index + 1) == SLOT_SUP) ||
      (char == SQRT_CHAR && getOrNull(index + 1) == SLOT_RAD)
  return when {
    kind != null -> {
      val (content, end) = parseLayout(index + 1)
      LayoutNode.Slot(kind, index, end, content) to end + 1
    }
    startsWith(Token.Calculus.DERIVATIVE, index) ->
      LayoutNode.Derivative(index) to index + Token.Calculus.DERIVATIVE.length
    unit != null -> LayoutNode.UnitToken(index, unit) to index + unit.token.length
    char == INTEGRAL_CHAR -> LayoutNode.IntegralSign(index) to index + 1
    char in BIG_OPERATORS && getOrNull(index + 1) == SLOT_SUB ->
      LayoutNode.BigOperator(index, char.toString()) to index + 1
    isHidden -> LayoutNode.Hidden(index) to index + 1
    else -> null
  }
}

/** Measured part of the expression. Same coordinates as [ExpressionLayout]. */
private class Box(
  val width: Float,
  val ascent: Float,
  val descent: Float,
  val texts: List<PlacedText> = emptyList(),
  val bars: List<Rect> = emptyList(),
  val placeholders: List<PlacedPlaceholder> = emptyList(),
  val strokes: List<PlacedStroke> = emptyList(),
  val carets: List<Caret> = emptyList(),
) {
  fun moved(dx: Float, dy: Float) =
    Box(
      width = width,
      ascent = ascent - dy,
      descent = descent + dy,
      texts = texts.map { PlacedText(it.layout, it.x + dx, it.baseline + dy) },
      bars = bars.map { it.translate(dx, dy) },
      placeholders =
        placeholders.map { PlacedPlaceholder(it.rect.translate(dx, dy), it.strokeWidth, it.angleBoxIndex) },
      strokes = strokes.map { stroke -> PlacedStroke(stroke.points.map { it + Offset(dx, dy) }, stroke.width) },
      carets = carets.map { Caret(it.index, it.x + dx, it.top + dy, it.bottom + dy) },
    )
}

private class LayoutBuilder(
  private val measurer: TextMeasurer,
  private val style: TextStyle,
  private val formatterSymbols: FormatterSymbols,
  private val density: Density,
) {
  fun layoutRow(
    nodes: List<LayoutNode>,
    rowStart: Int,
    rowEnd: Int,
    size: Float,
    isTopLevel: Boolean = false,
  ): Box {
    if (nodes.isEmpty()) return emptyRow(rowStart, size, isTopLevel)
    val row = RowBuilder(size, rowStart)
    var nodeIndex = 0
    // A node can take the next ones with it: a numerator its denominator
    while (nodeIndex < nodes.size) nodeIndex += 1 + row.place(nodes, nodeIndex)
    row.caret(rowEnd)
    return row.box()
  }

  /** Nothing at the top, an empty slot elsewhere. */
  private fun emptyRow(rowStart: Int, size: Float, isTopLevel: Boolean): Box {
    val caret = Caret(rowStart, 0f, -size * CARET_ASCENT, size * CARET_DESCENT)
    if (isTopLevel) return Box(0f, size * TEXT_ASCENT, size * TEXT_DESCENT, carets = listOf(caret))
    val stroke = size * PLACEHOLDER_STROKE
    val rect =
      Rect(
        left = size * PLACEHOLDER_MARGIN,
        top = -size * PLACEHOLDER_HEIGHT,
        right = size * (PLACEHOLDER_MARGIN + PLACEHOLDER_WIDTH),
        bottom = 0f,
      )
    return Box(
      width = size * (PLACEHOLDER_WIDTH + PLACEHOLDER_MARGIN * 2),
      ascent = size * PLACEHOLDER_HEIGHT + stroke,
      descent = stroke,
      placeholders = listOf(PlacedPlaceholder(rect, stroke)),
      carets = listOf(caret.copy(x = size * PLACEHOLDER_MARGIN)),
    )
  }

  /** A row being placed from left to right. */
  private inner class RowBuilder(private val size: Float, rowStart: Int) {
    private val caretTop = -size * CARET_ASCENT
    private val caretBottom = size * CARET_DESCENT
    private val texts = mutableListOf<PlacedText>()
    private val bars = mutableListOf<Rect>()
    private val placeholders = mutableListOf<PlacedPlaceholder>()
    private val strokes = mutableListOf<PlacedStroke>()
    private val carets = mutableListOf(Caret(rowStart, 0f, caretTop, caretBottom))
    private var x = 0f
    private var ascent = size * TEXT_ASCENT
    private var descent = size * TEXT_DESCENT
    // Metrics of the previous element, superscripts and subscripts are placed relative to it
    private var previousAscent = size * TEXT_ASCENT
    private var previousDescent = size * TEXT_DESCENT

    fun caret(index: Int) {
      carets += Caret(index, x, caretTop, caretBottom)
    }

    fun box() =
      Box(
        width = x,
        ascent = ascent,
        descent = descent,
        texts = texts,
        bars = bars,
        placeholders = placeholders,
        strokes = strokes,
        carets = carets,
      )

    private fun add(box: Box) {
      val moved = box.moved(x, 0f)
      texts += moved.texts
      bars += moved.bars
      placeholders += moved.placeholders
      strokes += moved.strokes
      carets += moved.carets
      ascent = max(ascent, moved.ascent)
      descent = max(descent, moved.descent)
      x += box.width
    }

    /** Adds [box], what comes next is placed relative to it. */
    private fun addAndFollow(box: Box) {
      add(box)
      previousAscent = box.ascent
      previousDescent = box.descent
    }

    /** Places [nodes] at [index]. @return How many nodes after it were placed with it. */
    fun place(nodes: List<LayoutNode>, index: Int): Int =
      when (val node = nodes[index]) {
        is LayoutNode.BigOperator -> placeBigOperator(node, nodes, index)
        is LayoutNode.Slot -> placeSlot(node, nodes, index)
        else -> {
          placeSimple(node)
          0
        }
      }

    private fun placeSimple(node: LayoutNode) {
      when (node) {
        is LayoutNode.Text -> addAndFollow(layoutText(node, size, caretTop, caretBottom))
        is LayoutNode.Hidden -> caret(node.index)
        // bounds go to the top and the bottom of the sign
        is LayoutNode.IntegralSign -> addAndFollow(layoutIntegralSign(node, size, caretTop, caretBottom))
        is LayoutNode.UnitToken -> {
          caret(node.index)
          addAndFollow(layoutUnit(node.unit, size, node.index))
          caret(node.index + node.unit.token.length)
        }
        is LayoutNode.Derivative -> {
          caret(node.start)
          addAndFollow(layoutDerivative(size))
          caret(node.start + Token.Calculus.DERIVATIVE.length)
        }
        is LayoutNode.BigOperator,
        is LayoutNode.Slot -> Unit
      }
    }

    private fun placeBigOperator(node: LayoutNode.BigOperator, nodes: List<LayoutNode>, index: Int): Int {
      caret(node.index)
      val lower = nodes.getOrNull(index + 1) as? LayoutNode.Slot
      val upper = nodes.getOrNull(index + 2) as? LayoutNode.Slot
      val hasBounds =
        lower != null && upper != null && lower.kind == SlotKind.SUBSCRIPT && upper.kind == SlotKind.SUPERSCRIPT
      val box =
        if (hasBounds && lower != null && upper != null) {
          layoutBigOperator(node.symbol, lower, upper, size)
        } else {
          layoutText(LayoutNode.Text(node.index, node.symbol), size, caretTop, caretBottom)
        }
      addAndFollow(box)
      if (upper != null && box.carets.isNotEmpty()) caret(upper.end + 1)
      return if (hasBounds) 2 else 0
    }

    private fun placeSlot(node: LayoutNode.Slot, nodes: List<LayoutNode>, index: Int): Int {
      caret(node.start)
      val next = nodes.getOrNull(index + 1) as? LayoutNode.Slot
      // A fraction, or integral bounds one above the other
      val pair =
        next?.takeIf {
          (node.kind == SlotKind.NUMERATOR && it.kind == SlotKind.DENOMINATOR) ||
            (node.kind == SlotKind.SUBSCRIPT && it.kind == SlotKind.SUPERSCRIPT)
        }
      val last = pair ?: node
      val box = slotBox(node, pair)
      add(box)
      val taken = if (pair != null) 1 else 0
      // ∫ with nothing before dx: empty box for the function, with the cursor in it
      val isEmptyIntegrand =
        node.kind == SlotKind.SUBSCRIPT &&
          last.kind == SlotKind.SUPERSCRIPT &&
          (nodes.getOrNull(index + taken + 1) as? LayoutNode.Text)?.text?.startsWith(Token.Calculus.DX) == true
      if (isEmptyIntegrand) add(layoutRow(emptyList(), last.end + 1, last.end + 1, size))
      caret(last.end + 1)
      if (node.kind == SlotKind.NUMERATOR || node.kind == SlotKind.RADICAND) {
        previousAscent = box.ascent
        previousDescent = box.descent
      }
      return taken
    }

    private fun slotBox(node: LayoutNode.Slot, pair: LayoutNode.Slot?): Box {
      // Empty bounds are bigger, easy to tap
      fun boundScale(bound: LayoutNode.Slot) = if (bound.content.isEmpty()) EMPTY_BOUND_SCALE else SCRIPT_SCALE
      return when {
        pair != null && node.kind == SlotKind.NUMERATOR -> layoutFraction(node, pair, size)
        pair != null ->
          layoutScripts(
            pair,
            node,
            size,
            previousAscent,
            previousDescent,
            superscriptScale = boundScale(pair),
            subscriptScale = boundScale(node),
          )
        node.kind == SlotKind.SUPERSCRIPT -> layoutScripts(node, null, size, previousAscent, previousDescent)
        node.kind == SlotKind.SUBSCRIPT -> layoutScripts(null, node, size, previousAscent, previousDescent)
        node.kind == SlotKind.RADICAND -> layoutRadical(node, size)
        // Broken fraction, draw content as is
        else -> layoutRow(node.content, node.start + 1, node.end, size)
      }
    }
  }

  private fun layoutText(node: LayoutNode.Text, size: Float, caretTop: Float, caretBottom: Float): Box {
    val display = node.text.formatExpression(formatterSymbols)
    val layout =
      measurer.measure(
        text = AnnotatedString(display),
        style = style.copy(fontSize = with(density) { size.toSp() }),
        softWrap = false,
        maxLines = 1,
        density = density,
      )
    val offsets = displayOffsets(node.text, display)
    val carets =
      offsets.mapIndexed { rawOffset, displayOffset ->
        Caret(
          index = node.start + rawOffset,
          x = layout.getHorizontalPosition(displayOffset, true),
          top = caretTop,
          bottom = caretBottom,
        )
      }
    return Box(
      width = layout.size.width.toFloat(),
      ascent = size * TEXT_ASCENT,
      descent = size * TEXT_DESCENT,
      texts = listOf(PlacedText(layout, 0f, 0f)),
      carets = carets,
    )
  }

  /**
   * ∫ taller than the text and centered on it like on paper, so that its bounds are further apart
   * and easy to tap.
   */
  private fun layoutIntegralSign(
    node: LayoutNode.IntegralSign,
    size: Float,
    caretTop: Float,
    caretBottom: Float,
  ): Box {
    val signSize = size * INTEGRAL_SCALE
    val sign = layoutText(LayoutNode.Text(node.index, Token.Calculus.INTEGRAL), signSize, caretTop, caretBottom)
    // Scaled from the baseline, so it is moved down to keep the middle of the text
    val moved = sign.moved(0f, (signSize - size) * INTEGRAL_CENTER)
    return Box(
      width = moved.width,
      ascent = moved.ascent,
      descent = moved.descent,
      texts = moved.texts,
      // Cursor stays as tall as the text
      carets =
        listOf(
          Caret(node.index, 0f, caretTop, caretBottom),
          Caret(node.index + 1, moved.width, caretTop, caretBottom),
        ),
    )
  }

  /** Position of every raw offset in formatted text (grouping symbols are inserted). */
  private fun displayOffsets(raw: String, display: String): List<Int> {
    val grouping = formatterSymbols.grouping.firstOrNull()
    val fractional = formatterSymbols.fractional.firstOrNull()
    val offsets = mutableListOf<Int>()
    var displayIndex = 0
    for (char in raw) {
      while (displayIndex < display.length) {
        val displayChar = display[displayIndex]
        val isSame = displayChar == char || (char == DOT_CHAR && displayChar == fractional)
        if (!isSame && displayChar == grouping) displayIndex++ else break
      }
      offsets.add(displayIndex.coerceAtMost(display.length))
      if (displayIndex < display.length) displayIndex++
    }
    offsets.add(display.length)
    return offsets
  }

  private fun layoutScripts(
    superscript: LayoutNode.Slot?,
    subscript: LayoutNode.Slot?,
    size: Float,
    previousAscent: Float,
    previousDescent: Float,
    superscriptScale: Float = SCRIPT_SCALE,
    subscriptScale: Float = SCRIPT_SCALE,
  ): Box {
    val superBox =
      superscript?.let {
        val box = layoutRow(it.content, it.start + 1, it.end, size * superscriptScale)
        // above the middle of the previous element
        val shift = max(size * SUPERSCRIPT_SHIFT, previousAscent - box.ascent * SCRIPT_OVERLAP)
        box.moved(0f, -shift)
      }
    val subBox =
      subscript?.let {
        val box = layoutRow(it.content, it.start + 1, it.end, size * subscriptScale)
        val shift = max(size * SUBSCRIPT_SHIFT, previousDescent - box.descent * SCRIPT_OVERLAP)
        box.moved(0f, shift)
      }
    val boxes = listOfNotNull(superBox, subBox)
    return Box(
      width = boxes.maxOf { it.width },
      ascent = boxes.maxOf { it.ascent },
      descent = boxes.maxOf { it.descent },
      texts = boxes.flatMap { it.texts },
      bars = boxes.flatMap { it.bars },
      placeholders = boxes.flatMap { it.placeholders },
      strokes = boxes.flatMap { it.strokes },
      carets = boxes.flatMap { it.carets },
    )
  }

  private fun layoutFraction(
    numerator: LayoutNode.Slot,
    denominator: LayoutNode.Slot,
    size: Float,
  ): Box {
    val numeratorBox = layoutRow(numerator.content, numerator.start + 1, numerator.end, size)
    val denominatorBox =
      layoutRow(denominator.content, denominator.start + 1, denominator.end, size)
    val padding = size * FRACTION_PADDING
    val width = max(numeratorBox.width, denominatorBox.width) + padding * 2
    val thickness = size * FRACTION_BAR_THICKNESS
    val barTop = -size * FRACTION_BAR_POSITION - thickness / 2
    val gap = size * FRACTION_GAP

    val movedNumerator =
      numeratorBox.moved(
        dx = (width - numeratorBox.width) / 2,
        dy = barTop - gap - numeratorBox.descent,
      )
    val movedDenominator =
      denominatorBox.moved(
        dx = (width - denominatorBox.width) / 2,
        dy = barTop + thickness + gap + denominatorBox.ascent,
      )
    val bar = Rect(padding / 2, barTop, width - padding / 2, barTop + thickness)

    return Box(
      width = width,
      ascent = movedNumerator.ascent,
      descent = movedDenominator.descent,
      texts = movedNumerator.texts + movedDenominator.texts,
      bars = movedNumerator.bars + movedDenominator.bars + bar,
      placeholders = movedNumerator.placeholders + movedDenominator.placeholders,
      strokes = movedNumerator.strokes + movedDenominator.strokes,
      carets = movedNumerator.carets + movedDenominator.carets,
    )
  }

  /** Square root sign with a bar that is as long as the radicand. */
  private fun layoutRadical(node: LayoutNode.Slot, size: Float): Box {
    val content = layoutRow(node.content, node.start + 1, node.end, size)
    val stroke = size * RADICAL_STROKE
    val signWidth = size * RADICAL_SIGN_WIDTH
    val padding = size * RADICAL_PADDING
    val top = -(content.ascent + size * RADICAL_GAP)
    val bottom = content.descent
    val height = bottom - top
    val sign =
      PlacedStroke(
        points =
          listOf(
            Offset(0f, top + height * RADICAL_TICK_START),
            Offset(signWidth * RADICAL_TICK_WIDTH, top + height * RADICAL_TICK_END),
            Offset(signWidth * RADICAL_BOTTOM_X, bottom),
            Offset(signWidth, top),
            Offset(signWidth + content.width + padding * 2, top),
          ),
        width = stroke,
      )
    val moved = content.moved(signWidth + padding, 0f)
    return Box(
      width = signWidth + content.width + padding * 3,
      ascent = -top + stroke,
      descent = bottom + stroke,
      texts = moved.texts,
      bars = moved.bars,
      placeholders = moved.placeholders,
      strokes = moved.strokes + sign,
      carets = moved.carets,
    )
  }

  /** Big Σ or ∏ with upper bound above it and `n=lower` below. */
  private fun layoutBigOperator(
    symbol: String,
    lower: LayoutNode.Slot,
    upper: LayoutNode.Slot,
    size: Float,
  ): Box {
    val symbolSize = size * BIG_OPERATOR_SCALE
    val boundSize = size * SCRIPT_SCALE
    val symbolLayout = measure(symbol, symbolSize)
    val upperBox = layoutRow(upper.content, upper.start + 1, upper.end, boundSize)
    val lowerContent = layoutRow(lower.content, lower.start + 1, lower.end, boundSize)
    // index is always n
    val prefix = measure(Token.Calculus.N + "=", boundSize)
    val lowerBox =
      lowerContent.moved(prefix.size.width.toFloat(), 0f).let { content ->
        Box(
          width = prefix.size.width + content.width,
          ascent = content.ascent,
          descent = content.descent,
          texts = content.texts + PlacedText(prefix, 0f, 0f),
          bars = content.bars,
          placeholders = content.placeholders,
          strokes = content.strokes,
          carets = content.carets,
        )
      }

    val width = maxOf(symbolLayout.size.width.toFloat(), upperBox.width, lowerBox.width) + size * FRACTION_PADDING
    val symbolBaseline = size * BIG_OPERATOR_BASELINE
    val symbolTop = symbolBaseline - symbolSize * TEXT_ASCENT
    val symbolBottom = symbolBaseline + symbolSize * TEXT_DESCENT
    val gap = size * FRACTION_GAP
    val movedUpper = upperBox.moved((width - upperBox.width) / 2, symbolTop - gap - upperBox.descent)
    val movedLower = lowerBox.moved((width - lowerBox.width) / 2, symbolBottom + gap + lowerBox.ascent)
    val symbolText =
      PlacedText(symbolLayout, (width - symbolLayout.size.width) / 2, symbolBaseline)

    return Box(
      width = width,
      ascent = movedUpper.ascent,
      descent = movedLower.descent,
      texts = movedUpper.texts + movedLower.texts + symbolText,
      bars = movedUpper.bars + movedLower.bars,
      placeholders = movedUpper.placeholders + movedLower.placeholders,
      strokes = movedUpper.strokes + movedLower.strokes,
      carets = movedUpper.carets + movedLower.carets,
    )
  }

  /** Unit symbol with a small gap before it: 3 km. */
  private fun layoutUnit(unit: PhysicalUnit, size: Float, index: Int): Box {
    if (unit == PhysicalUnits.degree || unit == PhysicalUnits.radian) return layoutAngleBox(unit, size, index)
    val layout = measure(unit.symbol, size)
    val gap = size * UNIT_GAP
    return Box(
      width = layout.size.width + gap,
      ascent = size * TEXT_ASCENT,
      descent = size * TEXT_DESCENT,
      texts = listOf(PlacedText(layout, gap, 0f)),
    )
  }

  /** Degrees and radians in a small rounded box, `deg` or `rad`, that can be tapped to switch. */
  private fun layoutAngleBox(unit: PhysicalUnit, size: Float, index: Int): Box {
    val labelSize = size * ANGLE_BOX_TEXT_SCALE
    val label = measure(if (unit == PhysicalUnits.degree) ANGLE_BOX_DEGREES else ANGLE_BOX_RADIANS, labelSize)
    val gap = size * UNIT_GAP
    val padding = labelSize * ANGLE_BOX_PADDING
    val height = labelSize * ANGLE_BOX_HEIGHT
    // Centered on the middle of digits, like operators
    val center = -size * FRACTION_BAR_POSITION
    val rect =
      Rect(
        left = gap,
        top = center - height / 2,
        right = gap + label.size.width + padding * 2,
        bottom = center + height / 2,
      )
    return Box(
      width = rect.right + gap,
      ascent = maxOf(size * TEXT_ASCENT, -rect.top),
      descent = maxOf(size * TEXT_DESCENT, rect.bottom),
      texts = listOf(PlacedText(label, gap + padding, center + labelSize * ANGLE_BOX_BASELINE)),
      placeholders = listOf(PlacedPlaceholder(rect, size * ANGLE_BOX_STROKE, angleBoxIndex = index)),
    )
  }

  /** `d/dx` as a small fraction. Cursor can't be placed inside. */
  private fun layoutDerivative(size: Float): Box {
    val textSize = size * DERIVATIVE_SCALE
    val numerator = measure(DERIVATIVE_NUMERATOR, textSize)
    val denominator = measure(Token.Calculus.DX, textSize)
    val padding = size * FRACTION_PADDING
    val width = maxOf(numerator.size.width, denominator.size.width) + padding * 2
    val thickness = size * FRACTION_BAR_THICKNESS
    val barTop = -size * FRACTION_BAR_POSITION - thickness / 2
    val gap = size * FRACTION_GAP
    val numeratorBaseline = barTop - gap - textSize * TEXT_DESCENT
    val denominatorBaseline = barTop + thickness + gap + textSize * TEXT_ASCENT
    return Box(
      width = width,
      ascent = -numeratorBaseline + textSize * TEXT_ASCENT,
      descent = denominatorBaseline + textSize * TEXT_DESCENT,
      texts =
        listOf(
          PlacedText(numerator, (width - numerator.size.width) / 2, numeratorBaseline),
          PlacedText(denominator, (width - denominator.size.width) / 2, denominatorBaseline),
        ),
      bars = listOf(Rect(padding / 2, barTop, width - padding / 2, barTop + thickness)),
    )
  }

  private fun measure(text: String, size: Float): TextLayoutResult =
    measurer.measure(
      text = AnnotatedString(text),
      style = style.copy(fontSize = with(density) { size.toSp() }),
      softWrap = false,
      maxLines = 1,
      density = density,
    )
}

private fun Caret.copy(x: Float) = Caret(index, x, top, bottom)

private val POWER_CHAR = Token.Operator.POWER.first()
private val SQRT_CHAR = Token.Operator.SQRT.first()
private val INTEGRAL_CHAR = Token.Calculus.INTEGRAL.first()
private val BIG_OPERATORS = listOf(Token.Calculus.SUM.first(), Token.Calculus.PRODUCT.first())
private const val DERIVATIVE_NUMERATOR = "d"
private val DOT_CHAR = Token.Digit.DOT.first()

// Fractions of font size
private const val TEXT_ASCENT = 0.8f
private const val TEXT_DESCENT = 0.22f
private const val CARET_ASCENT = 0.82f
private const val CARET_DESCENT = 0.22f
private const val SCRIPT_SCALE = 0.65f
/** Empty integral bound is as big as the text, so that it is easy to tap */
private const val EMPTY_BOUND_SCALE = 1f
private const val SUPERSCRIPT_SHIFT = 0.45f
private const val SUBSCRIPT_SHIFT = 0.22f
private const val SCRIPT_OVERLAP = 0.55f
private const val FRACTION_PADDING = 0.12f
private const val FRACTION_BAR_THICKNESS = 0.055f
private const val FRACTION_BAR_POSITION = 0.3f
private const val FRACTION_GAP = 0.1f
private const val DERIVATIVE_SCALE = 0.7f
private const val UNIT_GAP = 0.12f
// Angle box: label size compared to text, padding and height compared to label
private const val ANGLE_BOX_TEXT_SCALE = 0.55f
private const val ANGLE_BOX_PADDING = 0.35f
private const val ANGLE_BOX_HEIGHT = 1.35f
/** Baseline of the label under the middle of the box */
private const val ANGLE_BOX_BASELINE = 0.36f
private const val ANGLE_BOX_STROKE = 0.04f
private const val ANGLE_BOX_DEGREES = "deg"
private const val ANGLE_BOX_RADIANS = "rad"
private const val BIG_OPERATOR_SCALE = 1.4f
private const val INTEGRAL_SCALE = 1.8f
/** Middle of text above the baseline, in text sizes */
private const val INTEGRAL_CENTER = 0.35f
/** Baseline of big operator, so that it is centered around the middle of a line */
private const val BIG_OPERATOR_BASELINE = 0.25f
private const val RADICAL_STROKE = 0.06f
private const val RADICAL_SIGN_WIDTH = 0.5f
private const val RADICAL_PADDING = 0.06f
private const val RADICAL_GAP = 0.1f
private const val RADICAL_TICK_START = 0.62f
private const val RADICAL_TICK_WIDTH = 0.22f
private const val RADICAL_TICK_END = 0.55f
private const val RADICAL_BOTTOM_X = 0.5f
private const val PLACEHOLDER_WIDTH = 0.45f
private const val PLACEHOLDER_HEIGHT = 0.62f
private const val PLACEHOLDER_MARGIN = 0.06f
private const val PLACEHOLDER_STROKE = 0.05f
private const val VERTICAL_TAP_WEIGHT = 2f
