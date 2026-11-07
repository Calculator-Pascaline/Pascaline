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
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntSize
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
 * + and − buttons to zoom both, drag to move, double tap to go back to the automatic view. A tap
 * puts a point on the closest function with its value, its derivative and its tangent, and the point
 * can be dragged along the curve. With [showsKeyPoints], zeros and intersections are dots to tap.
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
  showsKeyPoints: Boolean = false,
) {
  val measurer = rememberTextMeasurer()
  val axisColor = MaterialTheme.colorScheme.onSurface
  val gridColor = MaterialTheme.colorScheme.outlineVariant
  val labelStyle = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
  val traceStyle = MaterialTheme.typography.labelLarge
  val surface = MaterialTheme.colorScheme.surfaceContainerHigh
  var trace by remember { mutableStateOf<Pair<Int, Double>?>(null) }
  var isMovingPoint by remember { mutableStateOf(false) }

  BoxWithConstraints(modifier = modifier.clipToBounds()) {
    val aspect = (constraints.maxHeight.toDouble() / constraints.maxWidth.coerceAtLeast(1))
    val autoWindow = remember(functions, points, aspect) { autoWindow(functions, points, aspect) }
    val current = window ?: autoWindow
    val currentWindow by rememberUpdatedState(current)
    val currentFunctions by rememberUpdatedState(functions)
    val keyPoints = remember(functions, current.xMin, current.xMax, showsKeyPoints) {
      if (showsKeyPoints) visibleKeyPoints(functions, current) else emptyList()
    }
    val currentKeyPoints by rememberUpdatedState(keyPoints)
    val onChange by rememberUpdatedState(onWindowChange)

    Canvas(
      modifier =
        Modifier.matchParentSize()
          .pointerInput(Unit) {
            detectPanAndAxisZoom(
              minSpan = MIN_PINCH_SPAN.toPx(),
              onDown = { offset ->
                isMovingPoint = isOnTrace(trace, currentFunctions, currentWindow, offset, size, POINT_TOUCH.toPx())
              },
            ) { centroid, pan, zoomX, zoomY ->
              if (isMovingPoint) {
                trace = trace?.copy(second = currentWindow.xMin + centroid.x / size.width * currentWindow.width)
              } else {
                onChange(currentWindow.transformed(centroid, pan, zoomX, zoomY, size.width, size.height))
              }
            }
          }
          .pointerInput(Unit) {
            detectTapGestures(
              onDoubleTap = {
                trace = null
                onChange(null)
              },
              onTap = { offset ->
                trace = traceAt(offset, currentKeyPoints, currentFunctions, currentWindow, size, KEY_POINT_TOUCH.toPx())
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

      keyPoints.forEach { drawKeyPoint(it, current, surface, axisColor) }

      val (index, x) = trace ?: return@Canvas
      val function = functions.getOrNull(index) ?: return@Canvas
      val y = function.valueAt(x)
      val slope = slopeAt(function.valueAt, x)
      if (!y.isNaN() && !slope.isNaN()) drawTangent(function.color, x, y, slope, current)
      drawTrace(function, x, y, current)
      drawTraceLabel(traceLabel(function.name, x, y, slope, formatValue), function.color, measurer, traceStyle, surface)
    }

    Column(
      modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
      verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      val colors = MaterialTheme.colorScheme
      SmallFloatingActionButton(
        onClick = { onWindowChange(current.zoomed(ZOOM_STEP)) },
        containerColor = colors.surface,
        contentColor = colors.onSurface,
      ) {
        Icon(Symbols.Add, stringResource(Res.string.functions_zoom_in))
      }
      SmallFloatingActionButton(
        onClick = { onWindowChange(current.zoomed(1 / ZOOM_STEP)) },
        containerColor = colors.surface,
        contentColor = colors.onSurface,
      ) {
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
  onDown: (Offset) -> Unit,
  onGesture: (centroid: Offset, pan: Offset, zoomX: Float, zoomY: Float) -> Unit,
) = awaitEachGesture {
  onDown(awaitFirstDown(requireUnconsumed = false).position)
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
 * x from −10 to 10 with units of the same size on both axes, so that a circle looks round. When no
 * curve would show, y is fitted to most values of the functions (ignoring asymptotes). With
 * [points], all of them are visible.
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
  if (values.isEmpty() || values.any { it in -sameScale..sameScale }) {
    return GraphWindow(AUTO_X_MIN, AUTO_X_MAX, -sameScale, sameScale)
  }

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
  val minorColor = gridColor.copy(alpha = MINOR_GRID_ALPHA)
  gridValues(window.xMin, window.xMax, stepX / MINOR_LINES).forEach { x ->
    val sx = window.toScreenX(x, size.width)
    drawLine(minorColor, Offset(sx, 0f), Offset(sx, size.height), 1f)
  }
  gridValues(window.yMin, window.yMax, stepY / MINOR_LINES).forEach { y ->
    val sy = window.toScreenY(y, size.height)
    drawLine(minorColor, Offset(0f, sy), Offset(size.width, sy), 1f)
  }

  gridValues(window.xMin, window.xMax, stepX).forEach { x ->
    val sx = window.toScreenX(x, size.width)
    val isAxis = abs(x) < stepX / 2
    drawLine(if (isAxis) axisColor else gridColor, Offset(sx, 0f), Offset(sx, size.height), if (isAxis) AXIS_WIDTH.toPx() else 1f)
    if (!isAxis) drawLabel(measurer, formatTick(x, stepX), Offset(sx + TICK_LABEL_SHIFT_X, axisY + TICK_LABEL_SHIFT_Y), labelStyle)
  }
  gridValues(window.yMin, window.yMax, stepY).forEach { y ->
    val sy = window.toScreenY(y, size.height)
    val isAxis = abs(y) < stepY / 2
    drawLine(if (isAxis) axisColor else gridColor, Offset(0f, sy), Offset(size.width, sy), if (isAxis) AXIS_WIDTH.toPx() else 1f)
    if (!isAxis) drawLabel(measurer, formatTick(y, stepY), Offset(axisX + TICK_LABEL_SHIFT_X, sy + TICK_LABEL_SHIFT_Y), labelStyle)
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

/** Whether [touch] is on the dot of [trace]. */
private fun isOnTrace(
  trace: Pair<Int, Double>?,
  functions: List<PlotFunction>,
  window: GraphWindow,
  touch: Offset,
  size: IntSize,
  reach: Float,
): Boolean {
  val (index, x) = trace ?: return false
  val plot = functions.getOrNull(index) ?: return false
  return (window.onScreen(plot, x, size) - touch).getDistance() < reach
}

/** A tap picks the dot it is on, otherwise the closest curve. */
private fun traceAt(
  tap: Offset,
  keyPoints: List<KeyPoint>,
  functions: List<PlotFunction>,
  window: GraphWindow,
  size: IntSize,
  reach: Float,
): Pair<Int, Double>? {
  val dot = keyPoints.firstOrNull { (window.onScreen(it, size) - tap).getDistance() < reach }
  return if (dot != null) dot.function to dot.x else closestFunction(functions, window, tap, size.width, size.height)
}

/** Zeros and intersections of the curves that are quick to calculate. */
private fun visibleKeyPoints(functions: List<PlotFunction>, window: GraphWindow): List<KeyPoint> {
  val fast = functions.indices.filter { functions[it].isFast }
  return keyPoints(fast.map { functions[it].valueAt }, window.xMin, window.xMax).map { it.copy(function = fast[it.function]) }
}

private fun DrawScope.drawKeyPoint(point: KeyPoint, window: GraphWindow, fill: Color, line: Color) {
  val center = Offset(window.toScreenX(point.x, size.width), window.toScreenY(point.y, size.height))
  drawCircle(fill, radius = KEY_POINT_RADIUS.toPx(), center = center)
  drawCircle(line, radius = KEY_POINT_RADIUS.toPx(), center = center, style = Stroke(KEY_POINT_STROKE.toPx()))
}

private fun GraphWindow.onScreen(point: KeyPoint, size: IntSize): Offset =
  Offset(toScreenX(point.x, size.width.toFloat()), toScreenY(point.y, size.height.toFloat()))

private fun GraphWindow.onScreen(plot: PlotFunction, x: Double, size: IntSize): Offset =
  Offset(toScreenX(x, size.width.toFloat()), toScreenY(plot.valueAt(x), size.height.toFloat()))

/**
 * Derivative of [f] at [x] from nearby values, NaN where there is none: the slopes on both sides
 * must agree, so that |x| at 0, a jump or the end of a domain don't look like a real value.
 */
internal fun slopeAt(f: (Double) -> Double, x: Double): Double {
  val h = max(abs(x), 1.0) * SLOPE_STEP
  val before = f(x - h)
  val center = f(x)
  val after = f(x + h)
  val far = f(x + 2 * h) - f(x - 2 * h)
  val left = (center - before) / h
  val right = (after - center) / h
  val slope = (8 * (after - before) - far) / (12 * h)
  // Slopes that keep growing closer to x are a vertical tangent, like the cube root at 0
  val wide = (f(x + SLOPE_WIDE * h) - f(x - SLOPE_WIDE * h)) / (2 * SLOPE_WIDE * h)
  val tolerance = SLOPE_AGREEMENT * max(1.0, abs(slope))
  return when {
    !center.isFinite() || !slope.isFinite() || !wide.isFinite() -> Double.NaN
    abs(left - right) > tolerance || abs(wide - slope) > tolerance -> Double.NaN
    abs(slope) > MAX_SLOPE -> Double.NaN
    abs(slope) < MIN_SLOPE -> 0.0
    else -> slope
  }
}

/** `f(1.5) = 2.25`, then its derivative and tangent line when it has one. */
internal fun traceLabel(name: String, x: Double, y: Double, slope: Double, format: (Double) -> String): String {
  val value = "$name(${format(x)}) = ${format(y)}"
  if (slope.isNaN() || y.isNaN()) return value
  val intercept = y - slope * x
  val sign = if (intercept < 0) "−" else "+"
  return "$value\n$name'(${format(x)}) = ${format(slope)}\ny = ${format(slope)}x $sign ${format(abs(intercept))}"
}

/** The line that touches the curve at the point, across the whole graph. */
private fun DrawScope.drawTangent(color: Color, x: Double, y: Double, slope: Double, window: GraphWindow) {
  fun end(endX: Double) = Offset(window.toScreenX(endX, size.width), window.toScreenY(y + slope * (endX - x), size.height))
  val dash = TANGENT_DASH.toPx()
  drawLine(
    color = color.copy(alpha = TANGENT_ALPHA),
    start = end(window.xMin),
    end = end(window.xMax),
    strokeWidth = TANGENT_WIDTH.toPx(),
    pathEffect = PathEffect.dashPathEffect(floatArrayOf(dash, dash)),
  )
}

/** Point on the curve. */
private fun DrawScope.drawTrace(plot: PlotFunction, x: Double, y: Double, window: GraphWindow) {
  if (y.isNaN()) return
  val point = Offset(window.toScreenX(x, size.width), window.toScreenY(y, size.height))
  drawCircle(plot.color, radius = TRACE_RADIUS.toPx(), center = point)
}

/** What the point says, in the corner. */
private fun DrawScope.drawTraceLabel(
  text: String,
  color: Color,
  measurer: TextMeasurer,
  style: TextStyle,
  background: Color,
) {
  val layout = measurer.measure(text, style.copy(color = color))
  val padding = LABEL_PADDING.toPx()
  drawRect(
    background,
    topLeft = Offset(padding, padding),
    size = Size(layout.size.width + padding * 2, layout.size.height + padding),
  )
  drawText(layout, topLeft = Offset(padding * TEXT_PADDING_X, padding * TEXT_PADDING_Y))
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
private val CURVE_WIDTH = 3.dp
private val TRACE_RADIUS = 5.dp
private const val MINOR_LINES = 5
private const val MINOR_GRID_ALPHA = 0.45f
private val POINT_TOUCH = 32.dp
private val KEY_POINT_TOUCH = 24.dp
private val KEY_POINT_RADIUS = 4.5.dp
private val KEY_POINT_STROKE = 1.5.dp
private val AXIS_WIDTH = 1.5.dp
private val TANGENT_WIDTH = 1.5.dp
private val TANGENT_DASH = 6.dp
private const val TANGENT_ALPHA = 0.7f
private const val SLOPE_STEP = 1e-4
private const val SLOPE_AGREEMENT = 0.01
private const val SLOPE_WIDE = 10
private const val MAX_SLOPE = 1e7
private const val MIN_SLOPE = 1e-7
private val POINT_RADIUS = 4.dp
private const val POINTS_MARGIN = 0.12
private val LABEL_PADDING = 6.dp
private val LABEL_HEIGHT = 16.dp

/** Tick labels next to their line */
private const val TICK_LABEL_SHIFT_X = 4f
private const val TICK_LABEL_SHIFT_Y = 2f
/** Text inside its background, in paddings */
private const val TEXT_PADDING_X = 2f
private const val TEXT_PADDING_Y = 1.5f
