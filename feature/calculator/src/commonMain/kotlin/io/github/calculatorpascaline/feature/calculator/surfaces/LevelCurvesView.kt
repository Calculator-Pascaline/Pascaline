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

package io.github.calculatorpascaline.feature.calculator.surfaces

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sin

/** Colors of the map: low and high heights, curves, arrows and the point that is read. */
private class MapColors(val low: Color, val high: Color, val curve: Color, val arrow: Color, val probe: Color)

/**
 * The graph seen from above: colored by height, one curve per height, arrows of ∇f that point
 * uphill, and the critical points. A tap reads the point under the finger.
 */
@Composable
internal fun LevelCurvesView(grid: SurfaceGrid, points: List<CriticalPoint>, probe: Pair<Double, Double>?, onProbe: (Double, Double) -> Unit) {
  val scheme = MaterialTheme.colorScheme
  val colors = MapColors(scheme.surfaceContainerHighest, scheme.primaryContainer, scheme.primary, scheme.onSurface, scheme.onSurface)
  val region = grid.region
  Canvas(
    Modifier.fillMaxWidth().height(MAP_HEIGHT).pointerInput(grid) {
      detectTapGestures { tap ->
        val area = mapArea(Size(size.width.toFloat(), size.height.toFloat()), region)
        val x = region.xLow + (tap.x - area.left) / area.width * (region.xHigh - region.xLow)
        val y = region.yHigh - (tap.y - area.top) / area.height * (region.yHigh - region.yLow)
        if (region.contains(x, y)) onProbe(x, y)
      }
    }
  ) {
    val area = mapArea(size, region)
    drawHeights(grid, area, colors)
    drawCurves(grid, area, colors.curve)
    drawGradient(grid, area, colors.arrow)
    fun at(x: Double, y: Double) =
      Offset(
        area.left + ((x - region.xLow) / (region.xHigh - region.xLow)).toFloat() * area.width,
        area.top + ((region.yHigh - y) / (region.yHigh - region.yLow)).toFloat() * area.height,
      )
    points.forEach { point ->
      drawCircle(Color.White, (POINT_RADIUS + POINT_RING).dp.toPx(), at(point.x, point.y))
      drawCircle(point.kind.color, POINT_RADIUS.dp.toPx(), at(point.x, point.y))
    }
    probe?.let { (x, y) ->
      drawCircle(colors.probe, PROBE_RADIUS.dp.toPx(), at(x, y), style = Stroke(PROBE_WIDTH.dp.toPx()))
    }
  }
}

/** The region on the screen, x and y at the same scale, in the middle. */
private fun mapArea(size: Size, region: Region): Rect {
  val scale = min(size.width / (region.xHigh - region.xLow), size.height / (region.yHigh - region.yLow)).toFloat()
  val width = ((region.xHigh - region.xLow) * scale).toFloat()
  val height = ((region.yHigh - region.yLow) * scale).toFloat()
  return Rect(Offset((size.width - width) / 2, (size.height - height) / 2), Size(width, height))
}

private fun DrawScope.drawHeights(grid: SurfaceGrid, area: Rect, colors: MapColors) {
  val cell = Size(area.width / grid.steps, area.height / grid.steps)
  for (i in 0 until grid.steps) {
    for (j in 0 until grid.steps) {
      val average = (grid.values[i][j] + grid.values[i + 1][j] + grid.values[i][j + 1] + grid.values[i + 1][j + 1]) / CORNERS
      if (!average.isFinite()) continue
      val color = lerp(colors.low, colors.high, grid.share(average).toFloat())
      // A hair bigger, so that no line shows between cells
      drawRect(color, Offset(area.left + i * cell.width, area.bottom - (j + 1) * cell.height), Size(cell.width + 1, cell.height + 1))
    }
  }
}

/** Marching squares: in each cell, a line where f crosses each height. */
private fun DrawScope.drawCurves(grid: SurfaceGrid, area: Rect, color: Color) {
  if (grid.high <= grid.low) return
  val cell = Size(area.width / grid.steps, area.height / grid.steps)
  val width = CURVE_WIDTH.dp.toPx()
  fun point(i: Double, j: Double) = Offset(area.left + (i * cell.width).toFloat(), area.bottom - (j * cell.height).toFloat())
  val cells = (0 until grid.steps).flatMap { i -> (0 until grid.steps).map { j -> i to j } }
  for (level in 1..LEVELS) {
    val height = grid.low + (grid.high - grid.low) * (level - HALF) / LEVELS
    cells.forEach { (i, j) ->
      crossings(grid, i, j, height).chunked(2).filter { it.size == 2 }.forEach { (start, end) ->
        drawLine(color, point(start.first, start.second), point(end.first, end.second), width, StrokeCap.Round)
      }
    }
  }
}

/** Where f crosses [height] on the edges of the cell at (i, j), in grid steps. */
private fun crossings(grid: SurfaceGrid, i: Int, j: Int, height: Double): List<Pair<Double, Double>> {
  val corners = listOf(i to j, i + 1 to j, i + 1 to j + 1, i to j + 1).map { (a, b) -> Triple(a, b, grid.values[a][b] - height) }
  if (corners.any { !it.third.isFinite() }) return emptyList()
  return (0 until CORNERS).mapNotNull { edge ->
    val (a0, b0, v0) = corners[edge]
    val (a1, b1, v1) = corners[(edge + 1) % CORNERS]
    if ((v0 < 0) == (v1 < 0)) return@mapNotNull null
    val t = v0 / (v0 - v1)
    (a0 + (a1 - a0) * t) to (b0 + (b1 - b0) * t)
  }
}

/** Arrows of ∇f on a coarser grid, longer where the slope is steeper. */
private fun DrawScope.drawGradient(grid: SurfaceGrid, area: Rect, color: Color) {
  val every = grid.steps / ARROWS
  val dx = (grid.region.xHigh - grid.region.xLow) / grid.steps
  val dy = (grid.region.yHigh - grid.region.yLow) / grid.steps
  fun gradient(i: Int, j: Int): Pair<Double, Double> =
    (grid.values[i + 1][j] - grid.values[i - 1][j]) / (2 * dx) to (grid.values[i][j + 1] - grid.values[i][j - 1]) / (2 * dy)
  val cells = (1 until ARROWS).flatMap { a -> (1 until ARROWS).map { b -> a * every to b * every } }
  val gradients = cells.associateWith { (i, j) -> gradient(i, j) }.filterValues { (gx, gy) -> gx.isFinite() && gy.isFinite() }
  val steepest = gradients.values.maxOfOrNull { (gx, gy) -> hypot(gx, gy) }?.takeIf { it > 0 } ?: return
  val longest = area.width / ARROWS * ARROW_SHARE
  val width = ARROW_WIDTH.dp.toPx()
  gradients.forEach { (cell, value) ->
    val (gx, gy) = value
    val length = (hypot(gx, gy) / steepest).toFloat().coerceAtLeast(SHORTEST) * longest
    val angle = atan2(gy, gx).toFloat()
    val center = Offset(area.left + cell.first * area.width / grid.steps, area.bottom - cell.second * area.height / grid.steps)
    val tip = center + Offset(cos(angle), -sin(angle)) * (length / 2)
    val tail = center - Offset(cos(angle), -sin(angle)) * (length / 2)
    drawLine(color.copy(alpha = ARROW_ALPHA), tail, tip, width, StrokeCap.Round)
    val head = length * HEAD_SHARE
    listOf(-1, 1).forEach { side ->
      val wing = angle + kotlin.math.PI.toFloat() - side * HEAD_ANGLE
      drawLine(color.copy(alpha = ARROW_ALPHA), tip, tip + Offset(cos(wing), -sin(wing)) * head, width, StrokeCap.Round)
    }
  }
}

/** Coral maxima, blue minima, amber saddles, gray when the test can't tell. */
internal val PointKind.color: Color
  get() =
    when (this) {
      PointKind.MAXIMUM -> MAXIMUM_COLOR
      PointKind.MINIMUM -> MINIMUM_COLOR
      PointKind.SADDLE -> SADDLE_COLOR
      PointKind.FLAT -> FLAT_COLOR
    }

private val MAXIMUM_COLOR = Color(0xFFD85A30)
private val MINIMUM_COLOR = Color(0xFF378ADD)
private val SADDLE_COLOR = Color(0xFFBA7517)
private val FLAT_COLOR = Color(0xFF888780)
private const val CORNERS = 4
private const val LEVELS = 12
private const val HALF = 0.5
private const val ARROWS = 12
private const val ARROW_SHARE = 0.85f
private const val SHORTEST = 0.25f
private const val HEAD_SHARE = 0.35f
private const val HEAD_ANGLE = 0.5f
private const val ARROW_ALPHA = 0.7f
private const val CURVE_WIDTH = 1.2f
private const val ARROW_WIDTH = 1.4f
private const val POINT_RADIUS = 6f
private const val POINT_RING = 2f
private const val PROBE_RADIUS = 9f
private const val PROBE_WIDTH = 2f
private val MAP_HEIGHT = 320.dp
