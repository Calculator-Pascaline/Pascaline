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
internal class ExpressionLayout(
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
}

internal class PlacedText(val layout: TextLayoutResult, val x: Float, val baseline: Float)

internal class PlacedPlaceholder(val rect: Rect, val strokeWidth: Float)

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

  fun flushText() {
    if (index > textStart) nodes.add(LayoutNode.Text(textStart, substring(textStart, index)))
  }

  while (index < length) {
    val char = this[index]
    if (char == SLOT_END) break

    val kind = char.toSlotKind()
    if (kind != null) {
      flushText()
      val (content, end) = parseLayout(index + 1)
      nodes.add(LayoutNode.Slot(kind, index, end, content))
      index = end + 1
      textStart = index
      continue
    }

    if (startsWith(Token.Calculus.DERIVATIVE, index)) {
      flushText()
      nodes.add(LayoutNode.Derivative(index))
      index += Token.Calculus.DERIVATIVE.length
      textStart = index
      continue
    }

    val unit = PhysicalUnits.tokenAt(this, index)
    if (unit != null) {
      flushText()
      nodes.add(LayoutNode.UnitToken(index, unit))
      index += unit.token.length
      textStart = index
      continue
    }

    if (char == INTEGRAL_CHAR) {
      flushText()
      nodes.add(LayoutNode.IntegralSign(index))
      index++
      textStart = index
      continue
    }

    if (char in BIG_OPERATORS && getOrNull(index + 1) == SLOT_SUB) {
      flushText()
      nodes.add(LayoutNode.BigOperator(index, char.toString()))
      index++
      textStart = index
      continue
    }

    val isHidden =
      (char == POWER_CHAR && getOrNull(index + 1) == SLOT_SUP) ||
        (char == SQRT_CHAR && getOrNull(index + 1) == SLOT_RAD)
    if (isHidden) {
      flushText()
      nodes.add(LayoutNode.Hidden(index))
      index++
      textStart = index
      continue
    }

    index++
  }
  index = index.coerceAtMost(length)
  flushText()
  return nodes to index
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
      placeholders = placeholders.map { PlacedPlaceholder(it.rect.translate(dx, dy), it.strokeWidth) },
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
    val caretTop = -size * CARET_ASCENT
    val caretBottom = size * CARET_DESCENT

    if (nodes.isEmpty()) {
      val caret = Caret(rowStart, 0f, caretTop, caretBottom)
      if (isTopLevel) return Box(0f, size * TEXT_ASCENT, size * TEXT_DESCENT, carets = listOf(caret))

      // Empty slot
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

    val texts = mutableListOf<PlacedText>()
    val bars = mutableListOf<Rect>()
    val placeholders = mutableListOf<PlacedPlaceholder>()
    val strokes = mutableListOf<PlacedStroke>()
    val carets = mutableListOf(Caret(rowStart, 0f, caretTop, caretBottom))
    var x = 0f
    var ascent = size * TEXT_ASCENT
    var descent = size * TEXT_DESCENT
    // Metrics of the previous element, superscripts and subscripts are placed relative to it
    var previousAscent = size * TEXT_ASCENT
    var previousDescent = size * TEXT_DESCENT

    fun add(box: Box) {
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

    var nodeIndex = 0
    while (nodeIndex < nodes.size) {
      when (val node = nodes[nodeIndex]) {
        is LayoutNode.Text -> {
          val box = layoutText(node, size, caretTop, caretBottom)
          add(box)
          previousAscent = box.ascent
          previousDescent = box.descent
        }
        is LayoutNode.Hidden -> carets += Caret(node.index, x, caretTop, caretBottom)
        is LayoutNode.IntegralSign -> {
          val box = layoutIntegralSign(node, size, caretTop, caretBottom)
          add(box)
          // bounds go to the top and the bottom of the sign
          previousAscent = box.ascent
          previousDescent = box.descent
        }
        is LayoutNode.BigOperator -> {
          carets += Caret(node.index, x, caretTop, caretBottom)
          val lower = nodes.getOrNull(nodeIndex + 1) as? LayoutNode.Slot
          val upper = nodes.getOrNull(nodeIndex + 2) as? LayoutNode.Slot
          val box =
            if (lower?.kind == SlotKind.SUBSCRIPT && upper?.kind == SlotKind.SUPERSCRIPT) {
              nodeIndex += 2
              layoutBigOperator(node.symbol, lower, upper, size)
            } else {
              layoutText(LayoutNode.Text(node.index, node.symbol), size, caretTop, caretBottom)
            }
          add(box)
          if (upper != null && box.carets.isNotEmpty()) {
            carets += Caret(upper.end + 1, x, caretTop, caretBottom)
          }
          previousAscent = box.ascent
          previousDescent = box.descent
        }
        is LayoutNode.UnitToken -> {
          carets += Caret(node.index, x, caretTop, caretBottom)
          val box = layoutUnit(node.unit, size)
          add(box)
          carets += Caret(node.index + node.unit.token.length, x, caretTop, caretBottom)
          previousAscent = box.ascent
          previousDescent = box.descent
        }
        is LayoutNode.Derivative -> {
          carets += Caret(node.start, x, caretTop, caretBottom)
          val box = layoutDerivative(size)
          add(box)
          carets += Caret(node.start + Token.Calculus.DERIVATIVE.length, x, caretTop, caretBottom)
          previousAscent = box.ascent
          previousDescent = box.descent
        }
        is LayoutNode.Slot -> {
          carets += Caret(node.start, x, caretTop, caretBottom)
          val next = nodes.getOrNull(nodeIndex + 1) as? LayoutNode.Slot
          var last = node
          val box =
            when {
              node.kind == SlotKind.NUMERATOR && next?.kind == SlotKind.DENOMINATOR -> {
                last = next
                nodeIndex++
                layoutFraction(node, next, size)
              }
              node.kind == SlotKind.SUBSCRIPT && next?.kind == SlotKind.SUPERSCRIPT -> {
                // integral bounds, one above the other. Empty ones are bigger, easy to tap
                last = next
                nodeIndex++
                fun boundScale(bound: LayoutNode.Slot) =
                  if (bound.content.isEmpty()) EMPTY_BOUND_SCALE else SCRIPT_SCALE
                layoutScripts(
                  next,
                  node,
                  size,
                  previousAscent,
                  previousDescent,
                  superscriptScale = boundScale(next),
                  subscriptScale = boundScale(node),
                )
              }
              node.kind == SlotKind.SUPERSCRIPT ->
                layoutScripts(node, null, size, previousAscent, previousDescent)
              node.kind == SlotKind.SUBSCRIPT ->
                layoutScripts(null, node, size, previousAscent, previousDescent)
              // Broken fraction, draw content as is
              node.kind == SlotKind.RADICAND -> layoutRadical(node, size)
              else -> layoutRow(node.content, node.start + 1, node.end, size)
            }
          add(box)
          // ∫ with nothing before dx: empty box for the function, with the cursor in it
          val isEmptyIntegrand =
            node.kind == SlotKind.SUBSCRIPT &&
              last.kind == SlotKind.SUPERSCRIPT &&
              (nodes.getOrNull(nodeIndex + 1) as? LayoutNode.Text)?.text?.startsWith(Token.Calculus.DX) == true
          if (isEmptyIntegrand) add(layoutRow(emptyList(), last.end + 1, last.end + 1, size))
          carets += Caret(last.end + 1, x, caretTop, caretBottom)
          if (node.kind == SlotKind.NUMERATOR || node.kind == SlotKind.RADICAND) {
            previousAscent = box.ascent
            previousDescent = box.descent
          }
        }
      }
      nodeIndex++
    }
    carets += Caret(rowEnd, x, caretTop, caretBottom)

    return Box(
      width = x,
      ascent = ascent,
      descent = descent,
      texts = texts,
      bars = bars,
      placeholders = placeholders,
      strokes = strokes,
      carets = carets,
    )
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
  private fun layoutUnit(unit: PhysicalUnit, size: Float): Box {
    val layout = measure(unit.symbol, size)
    val gap = size * UNIT_GAP
    return Box(
      width = layout.size.width + gap,
      ascent = size * TEXT_ASCENT,
      descent = size * TEXT_DESCENT,
      texts = listOf(PlacedText(layout, gap, 0f)),
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
