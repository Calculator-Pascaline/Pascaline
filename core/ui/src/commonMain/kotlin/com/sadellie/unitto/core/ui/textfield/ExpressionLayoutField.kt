/*
 * Unitto is a calculator for Android
 * Copyright (c) 2026 Elshan Agaev
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

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.KeyboardActionHandler
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.InterceptPlatformTextInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sadellie.unitto.core.common.FormatterSymbols
import com.sadellie.unitto.core.common.Token
import com.sadellie.unitto.core.designsystem.theme.LocalNumberTypography
import kotlin.math.ceil
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay

/**
 * Expression input that draws slots in two dimensions: fractions with a horizontal bar,
 * superscripts and subscripts. See [ExpressionLayout].
 *
 * Text is scaled down to fit (but not below [minRatio]), aligned to the bottom end. Tapping places
 * the cursor. A hidden [BasicTextField] keeps focus for hardware keyboard input.
 *
 * @param textStyle Text size, null for the big calculator input size.
 * @param wrapHeight Height follows the expression (taller for fractions and integrals), within the
 *   height [modifier] allows. Otherwise all the given height is used.
 */
@Composable
fun ExpressionLayoutTextField(
  modifier: Modifier,
  state: TextFieldState,
  formatterSymbols: FormatterSymbols,
  textColor: Color,
  minRatio: Float = 1f,
  alignStart: Boolean = false,
  keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
  onKeyboardAction: KeyboardActionHandler? = null,
  textStyle: TextStyle? = null,
  wrapHeight: Boolean = false,
) {
  val measurer = rememberTextMeasurer()
  val density = LocalDensity.current
  val style = textStyle ?: LocalNumberTypography.current.displayLarge
  val text = state.text.toString()
  val layout =
    remember(text, formatterSymbols, style, density) {
      measurer.layoutExpression(text, style, formatterSymbols, density)
    }
  val cursor = state.selection.max
  val focusRequester = remember { FocusRequester() }

  val caretAlpha = remember { Animatable(1f) }
  LaunchedEffect(text, cursor) {
    // Restart blinking on every change
    caretAlpha.snapTo(1f)
    while (true) {
      delay(CARET_BLINK_MS)
      caretAlpha.snapTo(0f)
      delay(CARET_BLINK_MS)
      caretAlpha.snapTo(1f)
    }
  }

  LaunchedEffect(text) { focusRequester.requestFocus() }

  val sizedModifier =
    if (!wrapHeight) {
      modifier
    } else {
      modifier.layout { measurable, constraints ->
        val height = ceil(layout.height).toInt().coerceIn(constraints.minHeight, constraints.maxHeight)
        val placeable = measurable.measure(constraints.copy(minHeight = height, maxHeight = height))
        layout(placeable.width, height) { placeable.place(0, 0) }
      }
    }
  BoxWithConstraints(modifier = sizedModifier.clipToBounds()) {
    val availableWidth = constraints.maxWidth.toFloat()
    val availableHeight = constraints.maxHeight.toFloat()
    val scale = layout.fitScale(availableWidth, availableHeight, minRatio)
    val caret = layout.caretFor(cursor)
    val caretMargin = with(density) { CARET_SCROLL_MARGIN.toPx() }

    // When too long, scroll so that cursor is visible
    val overflows = layout.width * scale > availableWidth - caretMargin * 2
    var offsetX =
      if (alignStart && !overflows) caretMargin else availableWidth - layout.width * scale - caretMargin
    if (caret != null && caret.x * scale + offsetX < caretMargin) {
      offsetX = caretMargin - caret.x * scale
    }
    // One line input (alignStart) is centered vertically, otherwise aligned to the bottom
    val baseline =
      if (alignStart) (availableHeight - layout.height * scale) / 2 + layout.ascent * scale
      else availableHeight - layout.descent * scale

    HiddenExpressionInput(
      state = state,
      formatterSymbols = formatterSymbols,
      focusRequester = focusRequester,
      keyboardOptions = keyboardOptions,
      onKeyboardAction = onKeyboardAction,
    )

    Canvas(
      modifier =
        Modifier.fillMaxSize().pointerInput(layout, scale, offsetX, baseline) {
          detectTapGestures { position ->
            val index =
              layout.indexAt((position.x - offsetX) / scale, (position.y - baseline) / scale)
                ?: return@detectTapGestures
            focusRequester.requestFocus()
            state.edit { selection = TextRange(index) }
          }
        }
    ) {
      translate(offsetX, baseline) {
        scale(scale, scale, pivot = Offset.Zero) {
          drawExpression(layout, textColor)
          if (caret != null) {
            drawLine(
              color = textColor,
              start = Offset(caret.x, caret.top),
              end = Offset(caret.x, caret.bottom),
              strokeWidth = CARET_WIDTH.toPx() / scale,
              alpha = caretAlpha.value,
            )
          }
        }
      }
    }
  }
}

/**
 * Read only version of [ExpressionLayoutTextField] that wraps its content.
 *
 * @param minScale When content is wider than available space it is scaled down, but not below this.
 */
@Composable
fun ExpressionLayoutText(
  modifier: Modifier = Modifier,
  value: String,
  formatterSymbols: FormatterSymbols,
  style: TextStyle,
  textColor: Color,
  minScale: Float = 1f,
) {
  val measurer = rememberTextMeasurer()
  val density = LocalDensity.current
  val layout =
    remember(value, formatterSymbols, style, density) {
      measurer.layoutExpression(value, style, formatterSymbols, density)
    }
  Spacer(
    modifier =
      modifier
        .layout { measurable, constraints ->
          var scale = 1f
          if (constraints.hasBoundedWidth && layout.width > constraints.maxWidth) {
            scale = (constraints.maxWidth / layout.width).coerceAtLeast(minScale)
          }
          val width = ceil(layout.width * scale).toInt()
          val height = ceil(layout.height * scale).toInt()
          val placeable = measurable.measure(Constraints.fixed(width, height))
          layout(width, height) { placeable.place(0, 0) }
        }
        .drawBehind {
          val scale = if (layout.width > 0f) size.width / layout.width else 1f
          scale(scale, scale, pivot = Offset.Zero) {
            translate(0f, layout.ascent) { drawExpression(layout, textColor) }
          }
        }
  )
}

/**
 * Read only expression that goes onto several lines instead of getting too small. Lines are split
 * between terms (before + and −), between factors `(...)(...)` and after commas.
 *
 * @param minScale Lines are scaled down to fit, but not below this. Too long lines are split.
 * @param alignEnd Align lines to the end, otherwise to the start.
 */
@Composable
fun WrappedExpressionLayoutText(
  modifier: Modifier = Modifier,
  value: String,
  formatterSymbols: FormatterSymbols,
  style: TextStyle,
  textColor: Color,
  minScale: Float,
  alignEnd: Boolean,
  maxWidth: Dp? = null,
) {
  // Known width is needed inside of components that use intrinsic measurements, like menus
  if (maxWidth != null) {
    val width = with(LocalDensity.current) { maxWidth.toPx() }
    WrappedLines(modifier, value, formatterSymbols, style, textColor, minScale, alignEnd, width)
    return
  }
  BoxWithConstraints(
    modifier = modifier,
    contentAlignment = if (alignEnd) Alignment.TopEnd else Alignment.TopStart,
  ) {
    val width =
      if (constraints.hasBoundedWidth) constraints.maxWidth.toFloat() else Float.POSITIVE_INFINITY
    WrappedLines(Modifier, value, formatterSymbols, style, textColor, minScale, alignEnd, width)
  }
}

@Composable
private fun WrappedLines(
  modifier: Modifier,
  value: String,
  formatterSymbols: FormatterSymbols,
  style: TextStyle,
  textColor: Color,
  minScale: Float,
  alignEnd: Boolean,
  maxWidth: Float,
) {
  val measurer = rememberTextMeasurer()
  val density = LocalDensity.current
  val lines =
    remember(value, formatterSymbols, style, density, maxWidth, minScale) {
      measurer.wrapExpression(value, style, formatterSymbols, density, maxWidth / minScale)
    }
  Column(modifier = modifier, horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start) {
    lines.forEach { line ->
      ExpressionLayoutText(
        value = line,
        formatterSymbols = formatterSymbols,
        style = style,
        textColor = textColor,
        minScale = minScale,
      )
    }
  }
}

private fun TextMeasurer.wrapExpression(
  text: String,
  style: TextStyle,
  formatterSymbols: FormatterSymbols,
  density: Density,
  maxWidth: Float,
): List<String> {
  fun width(start: Int, end: Int) =
    layoutExpression(text.substring(start, end), style, formatterSymbols, density).width

  if (width(0, text.length) <= maxWidth) return listOf(text)
  val breaks = text.lineBreaks()
  val lines = mutableListOf<String>()
  var start = 0
  while (start < text.length) {
    var end = text.length
    if (width(start, end) > maxWidth) {
      val candidates = breaks.filter { it > start }
      // longest line that fits, or the shortest possible one
      end = candidates.lastOrNull { width(start, it) <= maxWidth } ?: candidates.firstOrNull() ?: end
    }
    lines += text.substring(start, end).trim()
    start = end
  }
  return lines
}

/** Positions where expression can be split into lines: outside of slots and brackets. */
private fun String.lineBreaks(): List<Int> {
  val breaks = mutableListOf<Int>()
  // Lists (like roots) are split only after commas
  val isList = contains(',')
  var depth = 0
  forEachIndexed { index, char ->
    val isOpening = char.isSlotStart() || char == LEFT_BRACKET
    if (depth == 0 && index > 0) {
      val previous = this[index - 1]
      val canBreak =
        if (isList) {
          char != ' ' && previous == ' '
        } else {
          val isOperator = (char == PLUS_CHAR || char == MINUS_CHAR) && previous !in NO_BREAK_AFTER
          val isNextFactor = char == LEFT_BRACKET && previous == RIGHT_BRACKET
          isOperator || isNextFactor
        }
      if (canBreak) breaks += index
    }
    when {
      isOpening -> depth++
      char == SLOT_END || char == RIGHT_BRACKET -> depth = (depth - 1).coerceAtLeast(0)
    }
  }
  return breaks
}

private val LEFT_BRACKET = Token.Operator.LEFT_BRACKET.first()
private val RIGHT_BRACKET = Token.Operator.RIGHT_BRACKET.first()
private val PLUS_CHAR = Token.Operator.PLUS.first()
private val MINUS_CHAR = Token.Operator.MINUS.first()

/** Sign belongs to the number after these: x≈−1, 2±−3 */
private const val NO_BREAK_AFTER = "=≈±"

/** Read only expression that is scaled down to fit its container, aligned to the bottom end. */
@Composable
fun FittedExpressionLayoutText(
  modifier: Modifier,
  value: String,
  formatterSymbols: FormatterSymbols,
  style: TextStyle,
  textColor: Color,
) {
  val measurer = rememberTextMeasurer()
  val density = LocalDensity.current
  val layout =
    remember(value, formatterSymbols, style, density) {
      measurer.layoutExpression(value, style, formatterSymbols, density)
    }
  Canvas(modifier = modifier.clipToBounds()) {
    val scale = layout.fitScale(size.width, size.height, minRatio = 0f)
    translate(size.width - layout.width * scale, size.height - layout.descent * scale) {
      scale(scale, scale, pivot = Offset.Zero) { drawExpression(layout, textColor) }
    }
  }
}

/** Expression contains slots that need [ExpressionLayoutText] to be displayed properly. */
fun String.hasSlots(): Boolean = any { it.isSlotStart() || it == SLOT_END }

private fun DrawScope.drawExpression(layout: ExpressionLayout, color: Color) {
  layout.texts.forEach {
    drawText(
      textLayoutResult = it.layout,
      color = color,
      topLeft = Offset(it.x, it.baseline - it.layout.firstBaseline),
    )
  }
  layout.bars.forEach { drawRect(color = color, topLeft = it.topLeft, size = it.size) }
  layout.strokes.forEach { stroke ->
    val path =
      Path().apply {
        moveTo(stroke.points.first().x, stroke.points.first().y)
        stroke.points.drop(1).forEach { lineTo(it.x, it.y) }
      }
    drawPath(
      path = path,
      color = color,
      style = Stroke(width = stroke.width, cap = StrokeCap.Round, join = StrokeJoin.Round),
    )
  }
  layout.placeholders.forEach {
    drawRect(
      color = color.copy(alpha = color.alpha * PLACEHOLDER_ALPHA),
      topLeft = it.rect.topLeft,
      size = it.rect.size,
      style = Stroke(it.strokeWidth),
    )
  }
}

private fun ExpressionLayout.fitScale(width: Float, height: Float, minRatio: Float): Float {
  var scale = 1f
  if (this.width > 0f) scale = minOf(scale, width / this.width)
  if (this.height > 0f) scale = minOf(scale, height / this.height)
  return scale.coerceIn(minRatio, 1f)
}

/** Invisible text field: keeps focus, handles hardware keyboard and paste. */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun HiddenExpressionInput(
  state: TextFieldState,
  formatterSymbols: FormatterSymbols,
  focusRequester: FocusRequester,
  keyboardOptions: KeyboardOptions,
  onKeyboardAction: KeyboardActionHandler?,
) {
  // Never show software keyboard, app has it's own
  InterceptPlatformTextInput(interceptor = { _, _ -> awaitCancellation() }) {
    Box(Modifier.size(1.dp).alpha(0f)) {
      BasicTextField(
        state = state,
        modifier = Modifier.focusRequester(focusRequester),
        inputTransformation = ExpressionInputTransformation(formatterSymbols),
        textStyle = TextStyle(color = Color.Transparent, fontSize = 1.sp),
        keyboardOptions = keyboardOptions,
        onKeyboardAction = onKeyboardAction,
        lineLimits = TextFieldLineLimits.SingleLine,
        cursorBrush = SolidColor(Color.Transparent),
      )
    }
  }
}

private val CARET_WIDTH = 2.dp
private val CARET_SCROLL_MARGIN = 4.dp
private const val CARET_BLINK_MS = 500L
private const val PLACEHOLDER_ALPHA = 0.4f
