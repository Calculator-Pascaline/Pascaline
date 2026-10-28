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

package com.sadellie.unitto.feature.calculator.functions

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.sadellie.unitto.core.common.KBigDecimal
import com.sadellie.unitto.core.common.OutputFormat
import com.sadellie.unitto.core.common.Token
import com.sadellie.unitto.core.common.toFormattedString
import com.sadellie.unitto.core.designsystem.icons.symbols.Add
import com.sadellie.unitto.core.designsystem.icons.symbols.Remove
import com.sadellie.unitto.core.designsystem.icons.symbols.Symbols
import io.github.sadellie.evaluatto.GraphFunction
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.pow
import org.jetbrains.compose.resources.stringResource
import unitto.core.common.generated.resources.Res
import unitto.core.common.generated.resources.functions_zoom_in
import unitto.core.common.generated.resources.functions_zoom_out

/**
 * Curve ready to be drawn.
 *
 * @property isFast `false` when [valueAt] is slow, then fewer points are computed.
 */
internal class PlotFunction(
  val name: String,
  val color: Color,
  val valueAt: (Double) -> Double,
  val isFast: Boolean = true,
) {
  constructor(name: String, color: Color, function: GraphFunction) : this(name, color, function::valueAt, function.isFast)
}

/** Point drawn as a dot, like data of the Regression app or terms of a sequence. */
internal class PlotPoint(val x: Double, val y: Double, val color: Color)

/** Visible part of the plane. */
internal data class GraphWindow(val xMin: Double, val xMax: Double, val yMin: Double, val yMax: Double) {
  val width: Double
    get() = xMax - xMin

  val height: Double
    get() = yMax - yMin
}

/**
 * Graph of [functions] like the Functions app of NumWorks. Pinch along an axis to zoom it, or use
 * + and − buttons to zoom both, drag to move, tap to read a value of the closest function, double tap to go back to the automatic
 * view.
 *
 * @param window Visible part, null for automatic: x from −10 to 10 and y fitted to the functions,
 *   or fitted to [points] when there are some.
 */
@Composable
internal fun FunctionGraph(
  modifier: Modifier,
  functions: List<PlotFunction>,
  window: GraphWindow?,
  onWindowChange: (GraphWindow?) -> Unit,
  formatValue: (Double) -> String,
  points: List<PlotPoint> = emptyList(),
) {
  val measurer = rememberTextMeasurer()
  val axisColor = MaterialTheme.colorScheme.onSurface
  val gridColor = MaterialTheme.colorScheme.outlineVariant
  val labelStyle = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
  val traceStyle = MaterialTheme.typography.labelLarge
  val surface = MaterialTheme.colorScheme.surfaceContainerHigh
  var trace by remember { mutableStateOf<Pair<Int, Double>?>(null) }

  BoxWithConstraints(modifier = modifier.clipToBounds()) {
    val aspect = (constraints.maxHeight.toDouble() / constraints.maxWidth.coerceAtLeast(1))
    val autoWindow = remember(functions, points, aspect) { autoWindow(functions, points, aspect) }
    val current = window ?: autoWindow
    val currentWindow by rememberUpdatedState(current)
    val currentFunctions by rememberUpdatedState(functions)
    val onChange by rememberUpdatedState(onWindowChange)

    Canvas(
      modifier =
        Modifier.matchParentSize()
          .pointerInput(Unit) {
            detectPanAndAxisZoom(minSpan = MIN_PINCH_SPAN.toPx()) { centroid, pan, zoomX, zoomY ->
              onChange(currentWindow.transformed(centroid, pan, zoomX, zoomY, size.width, size.height))
            }
          }
          .pointerInput(Unit) {
            detectTapGestures(
              onDoubleTap = {
                trace = null
                onChange(null)
              },
              onTap = { offset ->
                trace = closestFunction(currentFunctions, currentWindow, offset, size.width, size.height)
              },
            )
          }
    ) {
      drawGrid(current, gridColor, axisColor, measurer, labelStyle)
      functions.forEach { drawFunction(it, current) }
      points.forEach { point ->
        val center = Offset(current.toScreenX(point.x, size.width), current.toScreenY(point.y, size.height))
        drawCircle(point.color, radius = POINT_RADIUS.toPx(), center = center)
      }

      val (index, x) = trace ?: return@Canvas
      val function = functions.getOrNull(index) ?: return@Canvas
      drawTrace(function, x, current, measurer, traceStyle, surface, formatValue)
    }

    Column(
      modifier = Modifier.align(Alignment.CenterEnd).padding(end = 16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      SmallFloatingActionButton(onClick = { onWindowChange(current.zoomed(ZOOM_STEP)) }) {
        Icon(Symbols.Add, stringResource(Res.string.functions_zoom_in))
      }
      SmallFloatingActionButton(onClick = { onWindowChange(current.zoomed(1 / ZOOM_STEP)) }) {
        Icon(Symbols.Remove, stringResource(Res.string.functions_zoom_out))
      }
    }
  }
}

/** Window zoomed around its center, [factor] above 1 zooms in. */
private fun GraphWindow.zoomed(factor: Double): GraphWindow {
  val centerX = (xMin + xMax) / 2
  val centerY = (yMin + yMax) / 2
  val halfWidth = width / 2 / factor
  val halfHeight = height / 2 / factor
  return GraphWindow(centerX - halfWidth, centerX + halfWidth, centerY - halfHeight, centerY + halfHeight)
}

/**
 * Drag with one finger to move, pinch with two to zoom. Each axis zooms on its own: fingers moving
 * apart sideways zoom x only, up and down zoom y only, diagonally zoom both.
 *
 * @param minSpan Fingers closer than this along an axis don't zoom that axis.
 */
private suspend fun PointerInputScope.detectPanAndAxisZoom(
  minSpan: Float,
  onGesture: (centroid: Offset, pan: Offset, zoomX: Float, zoomY: Float) -> Unit,
) = awaitEachGesture {
  awaitFirstDown(requireUnconsumed = false)
  var moved = Offset.Zero
  var isDragging = false
  do {
    val event = awaitPointerEvent()
    val fingers = event.changes.filter { it.pressed }
    when {
      fingers.size >= 2 -> {
        val (a, b) = fingers
        fun zoom(now: Float, before: Float) = if (now > minSpan && before > minSpan) now / before else 1f
        val zoomX = zoom(abs(a.position.x - b.position.x), abs(a.previousPosition.x - b.previousPosition.x))
        val zoomY = zoom(abs(a.position.y - b.position.y), abs(a.previousPosition.y - b.previousPosition.y))
        val centroid = (a.position + b.position) / 2f
        val pan = centroid - (a.previousPosition + b.previousPosition) / 2f
        onGesture(centroid, pan, zoomX, zoomY)
        isDragging = true
        fingers.forEach { it.consume() }
      }
      fingers.size == 1 -> {
        val finger = fingers.first()
        val pan = finger.positionChange()
        moved += pan
        // Small moves are taps
        if (!isDragging && moved.getDistance() > viewConfiguration.touchSlop) isDragging = true
        if (isDragging && pan != Offset.Zero) {
          onGesture(finger.position, pan, 1f, 1f)
          finger.consume()
        }
      }
    }
  } while (event.changes.any { it.pressed })
}

/** Window after zooming around [centroid] and dragging by [pan] (pixels). */
internal fun GraphWindow.transformed(
  centroid: Offset,
  pan: Offset,
  zoomX: Float,
  zoomY: Float,
  widthPx: Int,
  heightPx: Int,
): GraphWindow {
  val focusX = xMin + centroid.x / widthPx * width
  val focusY = yMax - centroid.y / heightPx * height
  val scaleX = 1.0 / zoomX
  val scaleY = 1.0 / zoomY
  val dx = -pan.x / widthPx * width
  val dy = pan.y / heightPx * height
  return GraphWindow(
    xMin = focusX + (xMin - focusX) * scaleX + dx,
    xMax = focusX + (xMax - focusX) * scaleX + dx,
    yMin = focusY + (yMin - focusY) * scaleY + dy,
    yMax = focusY + (yMax - focusY) * scaleY + dy,
  )
}

/**
 * x from −10 to 10, y fitted to most values of the functions (ignoring asymptotes). Without
 * functions, units on both axes have the same size. With [points], all of them are visible.
 */
private fun autoWindow(functions: List<PlotFunction>, points: List<PlotPoint>, aspect: Double): GraphWindow {
  if (points.isNotEmpty()) return pointsWindow(points)
  val values =
    functions
      .flatMap { plot ->
        val samples = if (plot.isFast) AUTO_SAMPLES else SLOW_SAMPLES
        (0..samples).map { plot.valueAt(AUTO_X_MIN + (AUTO_X_MAX - AUTO_X_MIN) * it / samples) }
      }
      .filter { it.isFinite() }
      .sorted()
  val sameScale = (AUTO_X_MAX - AUTO_X_MIN) * aspect / 2
  if (values.isEmpty()) return GraphWindow(AUTO_X_MIN, AUTO_X_MAX, -sameScale, sameScale)

  var low = values[(values.size * OUTLIER_SHARE).toInt()]
  var high = values[((values.size - 1) * (1 - OUTLIER_SHARE)).toInt()]
  if (high - low < MIN_RANGE) {
    low -= 1
    high += 1
  }
  val margin = (high - low) * Y_MARGIN
  return GraphWindow(AUTO_X_MIN, AUTO_X_MAX, low - margin, high + margin)
}

/** All points with a margin around them, and the axes when they are close. */
private fun pointsWindow(points: List<PlotPoint>): GraphWindow {
  fun range(values: List<Double>): Pair<Double, Double> {
    var low = values.min()
    var high = values.max()
    if (high - low < MIN_RANGE) {
      low -= 1
      high += 1
    }
    val margin = (high - low) * POINTS_MARGIN
    return low - margin to high + margin
  }
  val (xMin, xMax) = range(points.map { it.x })
  val (yMin, yMax) = range(points.map { it.y })
  return GraphWindow(xMin, xMax, yMin, yMax)
}

/** Function whose curve is closest to the tap, and x of the tap. */
private fun closestFunction(
  functions: List<PlotFunction>,
  window: GraphWindow,
  tap: Offset,
  widthPx: Int,
  heightPx: Int,
): Pair<Int, Double>? {
  val x = window.xMin + tap.x / widthPx * window.width
  return functions
    .mapIndexedNotNull { index, plot ->
      val y = plot.valueAt(x)
      if (y.isNaN()) null else index to abs(window.toScreenY(y, heightPx.toFloat()) - tap.y)
    }
    .minByOrNull { it.second }
    ?.let { it.first to x }
}

private fun GraphWindow.toScreenX(x: Double, widthPx: Float) = ((x - xMin) / width * widthPx).toFloat()

private fun GraphWindow.toScreenY(y: Double, heightPx: Float) = ((yMax - y) / height * heightPx).toFloat()

private fun DrawScope.drawGrid(
  window: GraphWindow,
  gridColor: Color,
  axisColor: Color,
  measurer: TextMeasurer,
  labelStyle: TextStyle,
) {
  val stepX = niceStep(window.width / GRID_LINES)
  val stepY = niceStep(window.height / GRID_LINES * size.width / size.height)
  // Labels stick to the axes, or to the edges when an axis is out of view
  val axisX = window.toScreenX(0.0, size.width).coerceIn(0f, size.width - LABEL_PADDING.toPx())
  val axisY = window.toScreenY(0.0, size.height).coerceIn(0f, size.height - LABEL_HEIGHT.toPx())

  gridValues(window.xMin, window.xMax, stepX).forEach { x ->
    val sx = window.toScreenX(x, size.width)
    val isAxis = abs(x) < stepX / 2
    drawLine(if (isAxis) axisColor else gridColor, Offset(sx, 0f), Offset(sx, size.height), if (isAxis) 2f else 1f)
    if (!isAxis) drawLabel(measurer, formatTick(x, stepX), Offset(sx + 4f, axisY + 2f), labelStyle)
  }
  gridValues(window.yMin, window.yMax, stepY).forEach { y ->
    val sy = window.toScreenY(y, size.height)
    val isAxis = abs(y) < stepY / 2
    drawLine(if (isAxis) axisColor else gridColor, Offset(0f, sy), Offset(size.width, sy), if (isAxis) 2f else 1f)
    if (!isAxis) drawLabel(measurer, formatTick(y, stepY), Offset(axisX + 4f, sy + 2f), labelStyle)
  }
}

private fun DrawScope.drawLabel(measurer: TextMeasurer, text: String, topLeft: Offset, style: TextStyle) {
  val layout = measurer.measure(text, style)
  val x = topLeft.x.coerceAtMost(size.width - layout.size.width)
  val y = topLeft.y.coerceAtMost(size.height - layout.size.height)
  drawText(layout, topLeft = Offset(x, y))
}

/**
 * Curve of [plot], one point every few pixels. The line is broken where the function is not
 * defined and at jumps (asymptotes of tan(x) or 1/x).
 */
private fun DrawScope.drawFunction(plot: PlotFunction, window: GraphWindow) {
  val samples = if (plot.isFast) (size.width / PIXELS_PER_SAMPLE).toInt() else SLOW_SAMPLES
  val path = Path()
  var previous: Float? = null
  for (i in 0..samples) {
    val x = window.xMin + window.width * i / samples
    val y = plot.valueAt(x)
    val sx = size.width * i / samples
    if (y.isNaN()) {
      previous = null
      continue
    }
    val sy = window.toScreenY(y, size.height).coerceIn(-size.height * OFF_SCREEN, size.height * (1 + OFF_SCREEN))
    if (previous == null || abs(sy - previous) > size.height * MAX_JUMP) path.moveTo(sx, sy) else path.lineTo(sx, sy)
    previous = sy
  }
  drawPath(path, plot.color, style = Stroke(width = CURVE_WIDTH.toPx()))
}

/** Point on the curve and its coordinates in the corner: `f(1.5) = 2.25`. */
private fun DrawScope.drawTrace(
  plot: PlotFunction,
  x: Double,
  window: GraphWindow,
  measurer: TextMeasurer,
  style: TextStyle,
  background: Color,
  formatValue: (Double) -> String,
) {
  val y = plot.valueAt(x)
  if (!y.isNaN()) {
    val point = Offset(window.toScreenX(x, size.width), window.toScreenY(y, size.height))
    drawCircle(plot.color, radius = TRACE_RADIUS.toPx(), center = point)
  }
  val text = "${plot.name}(${formatValue(x)}) = ${formatValue(y)}"
  val layout = measurer.measure(text, style.copy(color = plot.color))
  val padding = LABEL_PADDING.toPx()
  drawRect(
    background,
    topLeft = Offset(padding, padding),
    size = Size(layout.size.width + padding * 2, layout.size.height + padding),
  )
  drawText(layout, topLeft = Offset(padding * 2, padding * 1.5f))
}

/** 1, 2 or 5 times a power of ten, close to [rough]. */
internal fun niceStep(rough: Double): Double {
  if (rough <= 0 || !rough.isFinite()) return 1.0
  val magnitude = 10.0.pow(floor(log10(rough)))
  val fraction = rough / magnitude
  val nice = when {
    fraction < 1.5 -> 1.0
    fraction < 3.5 -> 2.0
    fraction < 7.5 -> 5.0
    else -> 10.0
  }
  return nice * magnitude
}

internal fun gridValues(from: Double, to: Double, step: Double): List<Double> {
  val first = ceil(from / step).toLong()
  val last = floor(to / step).toLong()
  if (last - first > MAX_GRID_LINES) return emptyList()
  return (first..last).map { it * step }
}

/** As many decimals as the step needs: 0.5, 1, 1.5. */
internal fun formatTick(value: Double, step: Double): String {
  val decimals = max(0, -floor(log10(step)).toInt())
  return KBigDecimal.valueOf(value)
    .toFormattedString(decimals, OutputFormat.PLAIN)
    .replace("-", Token.Operator.MINUS)
}

/** Colors of functions, readable on light and dark backgrounds. */
internal val FUNCTION_COLORS =
  listOf(
    Color(0xFFE53935),
    Color(0xFF1E88E5),
    Color(0xFF43A047),
    Color(0xFFFB8C00),
    Color(0xFF8E24AA),
    Color(0xFF00897B),
    Color(0xFFD81B60),
    Color(0xFF795548),
  )

private const val ZOOM_STEP = 1.5
private val MIN_PINCH_SPAN = 48.dp
private const val AUTO_X_MIN = -10.0
private const val AUTO_X_MAX = 10.0
private const val AUTO_SAMPLES = 400
private const val SLOW_SAMPLES = 60
private const val OUTLIER_SHARE = 0.05
private const val MIN_RANGE = 1e-9
private const val Y_MARGIN = 0.1
private const val GRID_LINES = 8.0
private const val MAX_GRID_LINES = 200
private const val PIXELS_PER_SAMPLE = 2f
private const val OFF_SCREEN = 2f
private const val MAX_JUMP = 1.5f
private val CURVE_WIDTH = 2.5.dp
private val TRACE_RADIUS = 5.dp
private val POINT_RADIUS = 4.dp
private const val POINTS_MARGIN = 0.12
private val LABEL_PADDING = 6.dp
private val LABEL_HEIGHT = 16.dp
