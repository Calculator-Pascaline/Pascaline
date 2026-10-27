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

package com.sadellie.unitto.feature.calculator.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.sadellie.unitto.core.common.FormatterSymbols
import com.sadellie.unitto.core.designsystem.icons.symbols.Delete
import com.sadellie.unitto.core.designsystem.icons.symbols.MoreVert
import com.sadellie.unitto.core.designsystem.icons.symbols.Symbols
import com.sadellie.unitto.core.designsystem.theme.LocalNumberTypography
import com.sadellie.unitto.core.model.calculator.CalculatorHistoryItem
import com.sadellie.unitto.core.ui.textfield.ExpressionLayoutText
import com.sadellie.unitto.core.ui.textfield.WrappedExpressionLayoutText
import com.sadellie.unitto.feature.calculator.HistoryResult
import com.sadellie.unitto.feature.calculator.MenuSection
import com.sadellie.unitto.feature.calculator.toInsertable
import org.jetbrains.compose.resources.StringResource
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.tan
import org.jetbrains.compose.resources.stringResource
import unitto.core.common.generated.resources.Res
import unitto.core.common.generated.resources.calculator_argument
import unitto.core.common.generated.resources.calculator_binary
import unitto.core.common.generated.resources.calculator_complex_plane
import unitto.core.common.generated.resources.calculator_decimal
import unitto.core.common.generated.resources.calculator_discriminant
import unitto.core.common.generated.resources.calculator_factorization
import unitto.core.common.generated.resources.calculator_hexadecimal
import unitto.core.common.generated.resources.calculator_imaginary_part
import unitto.core.common.generated.resources.calculator_modulus
import unitto.core.common.generated.resources.calculator_more
import unitto.core.common.generated.resources.calculator_no_history
import unitto.core.common.generated.resources.calculator_prime_factors
import unitto.core.common.generated.resources.calculator_real_part
import unitto.core.common.generated.resources.calculator_roots
import unitto.core.common.generated.resources.calculator_si_units
import unitto.core.common.generated.resources.common_delete
import unitto.core.common.generated.resources.common_error

/**
 * Calculations like on NumWorks: newest at the bottom, expression on the left and result on the
 * right (or below it when there is not enough space). Long lines are split into several lines.
 *
 * Tap expression or result to insert it. Swipe from right to left to delete. Polynomials have a
 * "…" menu with factorization and roots.
 */
@Composable
internal fun CalculationList(
  modifier: Modifier,
  items: List<CalculatorHistoryItem>,
  formatterSymbols: FormatterSymbols,
  addTokens: (String) -> Unit,
  onDelete: (CalculatorHistoryItem) -> Unit,
) {
  if (items.isEmpty()) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
      Text(
        text = stringResource(Res.string.calculator_no_history),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
    return
  }

  val state = rememberLazyListState()
  // Show newest calculation
  LaunchedEffect(items.firstOrNull()?.id) { state.scrollToItem(0) }

  LazyColumn(
    modifier = modifier,
    state = state,
    reverseLayout = true,
    contentPadding = PaddingValues(horizontal = CARD_SPACING, vertical = CARD_SPACING),
    verticalArrangement = Arrangement.spacedBy(CARD_SPACING, Alignment.Bottom),
  ) {
    items(items, { item -> item.id }) { item ->
      SwipeToDelete(modifier = Modifier.animateItem(), onDelete = { onDelete(item) }) {
        CalculationListItem(item = item, formatterSymbols = formatterSymbols, addTokens = addTokens)
      }
    }
  }
}

@Composable
private fun SwipeToDelete(modifier: Modifier, onDelete: () -> Unit, content: @Composable () -> Unit) {
  val dismissState = rememberSwipeToDismissBoxState()
  LaunchedEffect(dismissState.currentValue) {
    if (dismissState.currentValue == SwipeToDismissBoxValue.EndToStart) onDelete()
  }
  SwipeToDismissBox(
    modifier = modifier.clip(CARD_SHAPE),
    state = dismissState,
    enableDismissFromStartToEnd = false,
    backgroundContent = {
      Box(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.errorContainer),
        contentAlignment = Alignment.CenterEnd,
      ) {
        Icon(
          modifier = Modifier.padding(horizontal = 24.dp),
          imageVector = Symbols.Delete,
          contentDescription = stringResource(Res.string.common_delete),
          tint = MaterialTheme.colorScheme.onErrorContainer,
        )
      }
    },
  ) {
    content()
  }
}

@Composable
/** One calculation in a rounded card: expression on the left, result on the right. */
private fun CalculationListItem(
  item: CalculatorHistoryItem,
  formatterSymbols: FormatterSymbols,
  addTokens: (String) -> Unit,
) {
  val result = remember(item.result) { HistoryResult.decode(item.result) }
  val background = MaterialTheme.colorScheme.surfaceContainer
  val textColor = MaterialTheme.colorScheme.onSurface
  val resultColor = MaterialTheme.colorScheme.primary
  val style =
    LocalNumberTypography.current.displaySmall.let { it.copy(fontSize = it.fontSize * ITEM_FONT_SCALE) }
  val extraStyle = style.copy(fontSize = style.fontSize * EXTRA_LINE_SCALE)

  Column(modifier = Modifier.fillMaxWidth().background(background)) {
    Row(
      modifier = Modifier.padding(start = 16.dp, top = 10.dp, bottom = 10.dp, end = 4.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
        ExpressionAndResult(
          expression = {
            WrappedExpressionLayoutText(
              modifier = Modifier.clickable { addTokens(item.expression) },
              value = item.expression,
              formatterSymbols = formatterSymbols,
              style = style,
              textColor = textColor,
              minScale = MIN_SCALE,
              alignEnd = false,
            )
          },
          result = {
            val insertable = result.insertable
            if (insertable == null) {
              Text(
                text = stringResource(Res.string.common_error),
                style = extraStyle,
                color = MaterialTheme.colorScheme.error,
              )
            } else {
              WrappedExpressionLayoutText(
                modifier = Modifier.clickable { addTokens(insertable) },
                value = result.lines.first(),
                formatterSymbols = formatterSymbols,
                style = style,
                textColor = resultColor,
                minScale = MIN_SCALE,
                alignEnd = true,
              )
            }
          },
        )
        // Approximation, fraction
        result.lines.drop(1).forEach { line ->
          WrappedExpressionLayoutText(
            modifier = Modifier.fillMaxWidth(),
            value = line,
            formatterSymbols = formatterSymbols,
            style = extraStyle,
            textColor = resultColor,
            minScale = MIN_SCALE,
            alignEnd = true,
          )
        }
      }
      if (result.hasMenu) {
        MoreMenu(
          result = result,
          formatterSymbols = formatterSymbols,
          style = extraStyle,
          addTokens = addTokens,
        )
      }
    }
  }
}

/** "…" button with factorization, roots, other forms of a number and so on. */
@Composable
private fun MoreMenu(
  result: HistoryResult,
  formatterSymbols: FormatterSymbols,
  style: TextStyle,
  addTokens: (String) -> Unit,
) {
  var expanded by remember { mutableStateOf(false) }
  Box {
    IconButton(onClick = { expanded = true }) {
      Icon(
        modifier = Modifier.rotate(90f),
        imageVector = Symbols.MoreVert,
        contentDescription = stringResource(Res.string.calculator_more),
      )
    }
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
      Column(modifier = Modifier.padding(horizontal = MENU_PADDING)) {
        val hasPlane = result.menu.any { it.first == MenuSection.COMPLEX_PLANE }
        fun sectionLines(section: MenuSection) =
          result.menu.firstOrNull { it.first == section }?.second.orEmpty()
        result.menu.forEach { (section, lines) ->
          // Everything is drawn on the complex plane
          if (hasPlane && section != MenuSection.COMPLEX_PLANE) return@forEach
          MenuTitle(stringResource(section.title()))
          lines.forEach { line ->
            val modifier =
              if (section.isInsertable) {
                Modifier.clickable {
                  addTokens(line.toInsertable())
                  expanded = false
                }
              } else {
                Modifier
              }
            if (section == MenuSection.COMPLEX_PLANE) {
              val angle = sectionLines(MenuSection.ARGUMENT)
              ComplexPlane(
                modifier = Modifier.size(COMPLEX_PLANE_SIZE),
                point = line,
                realLabel = sectionLines(MenuSection.REAL_PART).firstOrNull(),
                imaginaryLabel = sectionLines(MenuSection.IMAGINARY_PART).firstOrNull(),
                lengthLabel = sectionLines(MenuSection.MODULUS).firstOrNull(),
                angleLabel = angle.firstOrNull(),
                degreesLabel = angle.lastOrNull()?.takeIf { angle.size > 1 },
                formatterSymbols = formatterSymbols,
                style = style.copy(fontSize = style.fontSize * PLANE_LABEL_SCALE),
                onLabelClick = { value ->
                  addTokens(value)
                  expanded = false
                },
              )
            } else if (section.isExpression) {
              WrappedExpressionLayoutText(
                modifier = modifier,
                value = line,
                formatterSymbols = formatterSymbols,
                style = style,
                textColor = MaterialTheme.colorScheme.onSurface,
                minScale = MIN_SCALE,
                alignEnd = false,
                maxWidth = MENU_CONTENT_WIDTH,
              )
            } else {
              // Binary and hexadecimal numbers must not get grouping symbols
              Text(
                modifier = modifier.widthIn(max = MENU_CONTENT_WIDTH),
                text = line,
                style = style,
                color = MaterialTheme.colorScheme.onSurface,
              )
            }
          }
        }
      }
    }
  }
}

private fun MenuSection.title(): StringResource =
  when (this) {
    MenuSection.FACTORIZATION -> Res.string.calculator_factorization
    MenuSection.ROOTS -> Res.string.calculator_roots
    MenuSection.DISCRIMINANT -> Res.string.calculator_discriminant
    MenuSection.DECIMAL -> Res.string.calculator_decimal
    MenuSection.HEXADECIMAL -> Res.string.calculator_hexadecimal
    MenuSection.BINARY -> Res.string.calculator_binary
    MenuSection.PRIME_FACTORS -> Res.string.calculator_prime_factors
    MenuSection.COMPLEX_PLANE -> Res.string.calculator_complex_plane
    MenuSection.REAL_PART -> Res.string.calculator_real_part
    MenuSection.IMAGINARY_PART -> Res.string.calculator_imaginary_part
    MenuSection.MODULUS -> Res.string.calculator_modulus
    MenuSection.ARGUMENT -> Res.string.calculator_argument
    MenuSection.SI_UNITS -> Res.string.calculator_si_units
  }

/**
 * Axes, unit circle and a vector from origin to [point] (`real,imaginary`). Real and imaginary
 * parts are written at projections on axes, length next to the vector, angle with degrees next to
 * its arc.
 */
@Composable
private fun ComplexPlane(
  modifier: Modifier,
  point: String,
  realLabel: String?,
  imaginaryLabel: String?,
  lengthLabel: String?,
  angleLabel: String?,
  degreesLabel: String?,
  formatterSymbols: FormatterSymbols,
  style: TextStyle,
  onLabelClick: (String) -> Unit,
) {
  val (re, im) = point.split(",").mapNotNull { it.toDoubleOrNull() }.takeIf { it.size == 2 } ?: return
  val axisColor = MaterialTheme.colorScheme.outline
  val vectorColor = MaterialTheme.colorScheme.primary
  val textColor = MaterialTheme.colorScheme.onSurface
  val axisStyle = MaterialTheme.typography.labelSmall.copy(color = axisColor)
  val measurer = rememberTextMeasurer()
  val realAxisLabel = remember(axisStyle) { measurer.measure("Re", axisStyle) }
  val imaginaryAxisLabel = remember(axisStyle) { measurer.measure("Im", axisStyle) }
  val length = hypot(re, im)
  val angle = atan2(im, re)

  @Composable
  fun Label(value: String?, color: Color = textColor) {
    if (value == null) {
      Spacer(Modifier)
    } else {
      ExpressionLayoutText(
        // Tap to insert value, without ≈ and units: "π/4 rad" ==> π/4, "≈65.905°" ==> 65.905
        modifier = Modifier.clickable { onLabelClick(value.toPlaneValue()) },
        value = value,
        formatterSymbols = formatterSymbols,
        style = style,
        textColor = color,
      )
    }
  }

  Layout(
    modifier = modifier,
    content = {
      Canvas(modifier = Modifier.clipToBounds()) {
        val geometry = PlaneGeometry(size.width, size.height, PLANE_MARGIN.toPx(), re, im)
        val center = geometry.center
        val labelGap = 2.dp.toPx()
        // axes
        drawLine(axisColor, Offset(0f, center.y), Offset(size.width, center.y))
        drawLine(axisColor, Offset(center.x, 0f), Offset(center.x, size.height))
        drawText(
          realAxisLabel,
          topLeft =
            Offset(
              size.width - realAxisLabel.size.width - labelGap,
              center.y - realAxisLabel.size.height - labelGap,
            ),
        )
        drawText(imaginaryAxisLabel, topLeft = Offset(center.x + labelGap * 2, labelGap))
        if (length == 0.0) {
          drawCircle(vectorColor, 4.dp.toPx(), center)
          return@Canvas
        }
        drawCircle(axisColor.copy(alpha = UNIT_CIRCLE_ALPHA), geometry.radius, center, style = Stroke(1.dp.toPx()))
        val end = geometry.end
        // projections on axes
        val dash = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 4.dp.toPx()))
        drawLine(axisColor, end, Offset(end.x, center.y), pathEffect = dash)
        drawLine(axisColor, end, Offset(center.x, end.y), pathEffect = dash)
        drawCircle(axisColor, 2.5.dp.toPx(), Offset(end.x, center.y))
        drawCircle(axisColor, 2.5.dp.toPx(), Offset(center.x, end.y))
        // vector
        drawLine(vectorColor, center, end, strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
        drawCircle(vectorColor, 4.dp.toPx(), end)
        // angle
        val arc = geometry.arcRadius
        drawArc(
          color = vectorColor.copy(alpha = ANGLE_ARC_ALPHA),
          startAngle = 0f,
          sweepAngle = (-angle * 180 / PI).toFloat(),
          useCenter = false,
          topLeft = Offset(center.x - arc, center.y - arc),
          size = Size(arc * 2, arc * 2),
          style = Stroke(1.5.dp.toPx()),
        )
      }
      Label(realLabel)
      Label(imaginaryLabel)
      Label(angleLabel, vectorColor)
      Label(degreesLabel, vectorColor)
      Label(lengthLabel, vectorColor)
    },
  ) { measurables, constraints ->
    val width = constraints.maxWidth
    val height = constraints.maxHeight
    val canvas = measurables[0].measure(Constraints.fixed(width, height))
    val loose = Constraints(maxWidth = width, maxHeight = height)
    val (real, imaginary, angleText, degrees, lengthText) =
      measurables.drop(1).map { it.measure(loose) }
    val geometry = PlaneGeometry(width.toFloat(), height.toFloat(), PLANE_MARGIN.toPx(), re, im)
    val gap = LABEL_GAP.roundToPx()

    fun Placeable.clampX(x: Float) = x.roundToInt().coerceIn(0, (width - this.width).coerceAtLeast(0))

    fun Placeable.clampY(y: Float) = y.roundToInt().coerceIn(0, (height - this.height).coerceAtLeast(0))

    layout(width, height) {
      canvas.place(0, 0)
      if (length == 0.0) return@layout
      val center = geometry.center
      val end = geometry.end
      // real part under (or over) its projection on real axis, away from the vector
      val realY = if (im >= 0) center.y + gap else center.y - gap - real.height
      real.place(real.clampX(end.x - real.width / 2f), real.clampY(realY))
      // imaginary part next to its projection on imaginary axis, on the other side of the vector
      val imaginaryX = if (re >= 0) center.x - gap - imaginary.width else center.x + gap
      imaginary.place(imaginary.clampX(imaginaryX), imaginary.clampY(end.y - imaginary.height / 2f))
      // angle next to its arc: inside of the angle, just over (or under) real axis. Small angles
      // don't have space there, label goes over the vector instead
      val blockHeight = angleText.height + degrees.height
      val isNegative = im < 0
      val isSmall = abs(angle) < SMALL_ANGLE
      val left: Float
      val top: Float
      if (isSmall) {
        val direction = angle + (if (isNegative) -SMALL_ANGLE_OFFSET else SMALL_ANGLE_OFFSET)
        val distance = geometry.arcRadius + gap
        left = center.x + (cos(direction) * distance).toFloat()
        val anchorY = center.y - (sin(direction) * distance).toFloat()
        top = if (isNegative) anchorY else anchorY - blockHeight
      } else {
        // far enough so that the vector is above the whole label
        val underVector =
          if (abs(angle) < PI / 2) (blockHeight + gap * 2) / tan(abs(angle)).toFloat() else 0f
        var start = center.x + maxOf(geometry.arcRadius + gap, underVector)
        // don't cross dashed projection of the vector, go to the other side of it
        val blockWidth = maxOf(angleText.width, degrees.width)
        if (start < end.x + gap && start + blockWidth > end.x - gap) start = end.x + gap
        left = start
        top = if (isNegative) center.y + gap else center.y - gap - blockHeight
      }
      angleText.place(angleText.clampX(left), angleText.clampY(top))
      degrees.place(degrees.clampX(left), degrees.clampY(top + angleText.height))
      // length next to the middle of the vector, on the side away from the angle
      val normalX = if (angle >= 0) -sin(angle) else sin(angle)
      val normalY = if (angle >= 0) -cos(angle) else cos(angle)
      val offset = gap + maxOf(lengthText.width, lengthText.height) / 2f
      val lengthCenterX = (center.x + end.x) / 2 + (normalX * offset).toFloat()
      val lengthCenterY = (center.y + end.y) / 2 + (normalY * offset).toFloat()
      lengthText.place(
        lengthText.clampX(lengthCenterX - lengthText.width / 2f),
        lengthText.clampY(lengthCenterY - lengthText.height / 2f),
      )
    }
  }
}

private fun String.toPlaneValue(): String =
  toInsertable().removeSuffix(RADIANS_UNIT).removeSuffix(DEGREES_UNIT).trim()

/**
 * Positions on the complex plane, vector has length of the unit circle. Plane is zoomed in on the
 * quadrant with the vector: 1+i shows only the upper right quarter of the circle.
 */
private class PlaneGeometry(width: Float, height: Float, margin: Float, re: Double, im: Double) {
  private val length = hypot(re, im)
  private val cos = if (length == 0.0) 0.0 else re / length
  private val sin = if (length == 0.0) 0.0 else im / length

  // Visible part of the plane in unit circle coordinates
  private val xMin = if (cos < -AXIS_EPSILON) -1 - PLANE_PADDING else -PLANE_SIDE
  private val xMax = if (cos > AXIS_EPSILON) 1 + PLANE_PADDING else PLANE_SIDE
  private val yMin = if (sin < -AXIS_EPSILON) -1 - PLANE_PADDING else -PLANE_SIDE
  private val yMax = if (sin > AXIS_EPSILON) 1 + PLANE_PADDING else PLANE_SIDE

  val radius =
    if (length == 0.0) minOf(width, height) / 2 - margin
    else
      minOf((width - margin * 2) / (xMax - xMin), (height - margin * 2) / (yMax - yMin)).toFloat()

  /** Origin, visible part is centered. */
  val center =
    if (length == 0.0) Offset(width / 2, height / 2)
    else
      Offset(
        ((width - (xMax - xMin) * radius) / 2 - xMin * radius).toFloat(),
        ((height - (yMax - yMin) * radius) / 2 + yMax * radius).toFloat(),
      )
  val arcRadius = radius * ANGLE_ARC_SIZE
  val end = Offset(center.x + (cos * radius).toFloat(), center.y - (sin * radius).toFloat())
}

@Composable
private fun MenuTitle(text: String) {
  Text(
    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
    text = text,
    style = MaterialTheme.typography.labelLarge,
    color = MaterialTheme.colorScheme.primary,
  )
}

/** Expression and result in one row when they fit, otherwise result goes below. */
@Composable
private fun ExpressionAndResult(
  expression: @Composable () -> Unit,
  result: @Composable () -> Unit,
) {
  Layout(contents = listOf(expression, result)) { (expressionMeasurables, resultMeasurables), constraints ->
    val looseConstraints = constraints.copy(minWidth = 0, minHeight = 0)
    val gap = ROW_GAP.roundToPx()
    val expressionPlaceable = expressionMeasurables.first().measure(looseConstraints)
    val resultPlaceable = resultMeasurables.first().measure(looseConstraints)
    val width = constraints.maxWidth
    val fitsInRow = expressionPlaceable.width + gap + resultPlaceable.width <= width

    if (fitsInRow) {
      val height = maxOf(expressionPlaceable.height, resultPlaceable.height)
      layout(width, height) {
        expressionPlaceable.place(0, (height - expressionPlaceable.height) / 2)
        resultPlaceable.place(width - resultPlaceable.width, (height - resultPlaceable.height) / 2)
      }
    } else {
      val height = expressionPlaceable.height + resultPlaceable.height
      layout(width, height) {
        expressionPlaceable.place(0, 0)
        resultPlaceable.place(width - resultPlaceable.width, expressionPlaceable.height)
      }
    }
  }
}

private val ROW_GAP = 16.dp
private val MENU_PADDING = 16.dp
private val COMPLEX_PLANE_SIZE = 340.dp
/** Space between unit circle and edges for labels */
private val PLANE_MARGIN = 8.dp
private val LABEL_GAP = 4.dp
private const val PLANE_LABEL_SCALE = 0.95f
private const val RADIANS_UNIT = " rad"
private const val DEGREES_UNIT = "°"
private const val UNIT_CIRCLE_ALPHA = 0.4f
private const val ANGLE_ARC_SIZE = 0.28f
/** Space after the circle, in radiuses */
private const val PLANE_PADDING = 0.12
/** Space on the other side of an axis, for labels */
private const val PLANE_SIDE = 0.3
/** Vector this close to an axis lies on it */
private const val AXIS_EPSILON = 1e-9
/** Radians, angles smaller than this have no space for label inside of them */
private const val SMALL_ANGLE = 0.6
private const val SMALL_ANGLE_OFFSET = 0.35
private const val ANGLE_ARC_ALPHA = 0.7f
private val MENU_CONTENT_WIDTH = 288.dp
private const val ITEM_FONT_SCALE = 0.8f
private const val EXTRA_LINE_SCALE = 0.75f
private const val MIN_SCALE = 0.8f
private val CARD_SPACING = 8.dp
private val CARD_SHAPE = RoundedCornerShape(20.dp)
