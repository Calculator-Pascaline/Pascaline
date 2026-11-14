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

package com.sadellie.unitto.feature.calculator.surfaces

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/** A point in the view box: x and y from −1 to 1, z from −[Z_HALF] to [Z_HALF]. */
private class Point3(val x: Float, val y: Float, val z: Float)

/** A cell of the graph, its color already lit, and which of its sides are drawn as lines. */
private class Cell(val corners: List<Point3>, val color: Color, val lines: List<Pair<Int, Int>>, val edges: List<Pair<Int, Int>>)

/** Turned by [yaw] around the up direction, tilted by [pitch], seen with a little perspective. */
private class View3(val yaw: Float, val pitch: Float, val center: Offset, val reach: Float) {
  private val cy = cos(yaw)
  private val sy = sin(yaw)
  private val cp = cos(pitch)
  private val sp = sin(pitch)

  fun depth(point: Point3): Float {
    val y1 = point.x * sy + point.y * cy
    return y1 * cp - point.z * sp
  }

  fun project(point: Point3): Offset {
    val x1 = point.x * cy - point.y * sy
    val y1 = point.x * sy + point.y * cy
    val up = y1 * sp + point.z * cp
    val scale = reach / (1 + depth(point) * PERSPECTIVE)
    return Offset(center.x + x1 * scale, center.y - up * scale)
  }
}

/** Colors of the graph, from the theme. */
private class SurfaceColors(val low: Color, val high: Color, val line: Color, val edge: Color, val floor: Color, val text: Color)

/**
 * The graph of f over its region in 3D, made for reading: x and y fill the box whatever their
 * size, z has a fixed height. One finger turns it, two zoom, a double tap puts it back.
 */
@Composable
internal fun SurfaceView(grid: SurfaceGrid, points: List<CriticalPoint>, selected: Int?) {
  var yaw by rememberSaveable { mutableFloatStateOf(START_YAW) }
  var pitch by rememberSaveable { mutableFloatStateOf(START_PITCH) }
  var zoom by rememberSaveable { mutableFloatStateOf(1f) }
  val scheme = MaterialTheme.colorScheme
  val colors = SurfaceColors(scheme.primaryContainer, scheme.primary, scheme.onPrimaryContainer, scheme.primary, scheme.outlineVariant, scheme.onSurfaceVariant)
  val cells = remember(grid, colors) { cells(grid, colors) }
  val measurer = rememberTextMeasurer()
  val label = MaterialTheme.typography.labelMedium.copy(color = colors.text)
  Canvas(
    Modifier.fillMaxWidth()
      .height(VIEW_HEIGHT)
      .clipToBounds()
      .pointerInput(Unit) {
        detectTapGestures(
          onDoubleTap = {
            yaw = START_YAW
            pitch = START_PITCH
            zoom = 1f
          }
        )
      }
      .pointerInput(Unit) {
        detectTransformGestures { _, pan, gestureZoom, _ ->
          yaw -= pan.x * TURN_SPEED
          pitch = (pitch + pan.y * TURN_SPEED).coerceIn(MIN_PITCH, MAX_PITCH)
          zoom = (zoom * gestureZoom).coerceIn(MIN_ZOOM, MAX_ZOOM)
        }
      }
  ) {
    val view = View3(yaw, pitch, Offset(size.width / 2, size.height / 2 + size.height * CENTER_DROP), size.minDimension * REACH * zoom)
    drawFloor(view, colors, measurer, label)
    drawCells(cells, view, colors)
    drawPoints(grid, points, selected, view, colors)
  }
}

/** Lit cells of the graph. Light comes from above and the side, both faces are lit the same. */
private fun cells(grid: SurfaceGrid, colors: SurfaceColors): List<Cell> {
  val steps = grid.steps
  fun point(i: Int, j: Int): Point3? {
    val value = grid.values[i][j]
    if (!value.isFinite()) return null
    return Point3(
      (2f * i / steps - 1),
      (2f * j / steps - 1),
      ((grid.share(value) * 2 - 1) * Z_HALF).toFloat(),
    )
  }
  val light = normalized(LIGHT_X, LIGHT_Y, LIGHT_Z)
  val lineEvery = (steps / LINES).coerceAtLeast(1)
  return (0 until steps).flatMap { i ->
    (0 until steps).mapNotNull { j ->
      val corners = listOfNotNull(point(i, j), point(i + 1, j), point(i + 1, j + 1), point(i, j + 1))
      if (corners.size != CORNERS) return@mapNotNull null
      val average = (corners.sumOf { it.z.toDouble() } / CORNERS / Z_HALF + 1) / 2
      val normal = normalOf(corners)
      val lit = AMBIENT + (1 - AMBIENT) * abs(normal.first * light.first + normal.second * light.second + normal.third * light.third)
      val base = lerp(colors.low, colors.high, average.toFloat().coerceIn(0f, 1f))
      val color = lerp(Color.Black, base, lit).copy(alpha = 1f)
      // Sides as corner pairs: 0-1 along x at j, 1-2 along y at i + 1, 2-3 along x at j + 1, 3-0 along y at i
      val lines = buildList {
        if (j % lineEvery == 0) add(0 to 1)
        if (i % lineEvery == 0) add(3 to 0)
      }
      val edges = buildList {
        if (j == 0) add(0 to 1)
        if (i == steps - 1) add(1 to 2)
        if (j == steps - 1) add(2 to 3)
        if (i == 0) add(3 to 0)
      }
      Cell(corners, color, lines, edges)
    }
  }
}

private fun normalOf(corners: List<Point3>): Triple<Float, Float, Float> {
  val ax = corners[2].x - corners[0].x
  val ay = corners[2].y - corners[0].y
  val az = corners[2].z - corners[0].z
  val bx = corners[3].x - corners[1].x
  val by = corners[3].y - corners[1].y
  val bz = corners[3].z - corners[1].z
  return normalized(ay * bz - az * by, az * bx - ax * bz, ax * by - ay * bx)
}

private fun normalized(x: Float, y: Float, z: Float): Triple<Float, Float, Float> {
  val length = sqrt(x * x + y * y + z * z).takeIf { it > 0 } ?: 1f
  return Triple(x / length, y / length, z / length)
}

/** The square under the graph with its axes and their names. */
private fun DrawScope.drawFloor(view: View3, colors: SurfaceColors, measurer: TextMeasurer, label: TextStyle) {
  val bottom = -Z_HALF - FLOOR_GAP
  val corners = listOf(Point3(-1f, -1f, bottom), Point3(1f, -1f, bottom), Point3(1f, 1f, bottom), Point3(-1f, 1f, bottom)).map { view.project(it) }
  val square = Path().apply {
    moveTo(corners[0].x, corners[0].y)
    corners.drop(1).forEach { lineTo(it.x, it.y) }
    close()
  }
  drawPath(square, colors.floor.copy(alpha = FLOOR_ALPHA), style = Fill)
  drawPath(square, colors.floor, style = Stroke(1.dp.toPx()))
  val origin = Point3(-1f, -1f, bottom)
  listOf(Point3(1f + AXIS_MORE, -1f, bottom) to "x", Point3(-1f, 1f + AXIS_MORE, bottom) to "y", Point3(-1f, -1f, Z_HALF + AXIS_MORE) to "z")
    .forEach { (end, name) ->
      val start = view.project(origin)
      val tip = view.project(end)
      drawLine(colors.text, start, tip, AXIS_WIDTH.dp.toPx(), StrokeCap.Round)
      val layout = measurer.measure(name, label)
      drawText(layout, topLeft = tip + (tip - start) * LABEL_OUT - Offset(layout.size.width / 2f, layout.size.height / 2f))
    }
}

/** Back to front, so that near cells hide far ones. Grid lines and the outer edge go with their cell. */
private fun DrawScope.drawCells(cells: List<Cell>, view: View3, colors: SurfaceColors) {
  val thin = Stroke(HAIRLINE.dp.toPx())
  val line = LINE_WIDTH.dp.toPx()
  val edge = EDGE_WIDTH.dp.toPx()
  cells
    .sortedByDescending { cell -> cell.corners.sumOf { view.depth(it).toDouble() } }
    .forEach { cell ->
      val points = cell.corners.map { view.project(it) }
      val path = Path().apply {
        moveTo(points[0].x, points[0].y)
        points.drop(1).forEach { lineTo(it.x, it.y) }
        close()
      }
      drawPath(path, cell.color)
      // The same color around it hides the hair of background between cells
      drawPath(path, cell.color, style = thin)
      cell.lines.forEach { (a, b) -> drawLine(colors.line.copy(alpha = LINE_ALPHA), points[a], points[b], line) }
      cell.edges.forEach { (a, b) -> drawLine(colors.edge, points[a], points[b], edge, StrokeCap.Round) }
    }
}

/** Each point where ∇f = 0 with a dashed line down to the floor, the chosen one bigger. */
private fun DrawScope.drawPoints(grid: SurfaceGrid, points: List<CriticalPoint>, selected: Int?, view: View3, colors: SurfaceColors) {
  val region = grid.region
  val dash = PathEffect.dashPathEffect(floatArrayOf(DASH.dp.toPx(), DASH.dp.toPx()))
  points.forEachIndexed { index, point ->
    val x = ((point.x - region.xLow) / (region.xHigh - region.xLow) * 2 - 1).toFloat()
    val y = ((point.y - region.yLow) / (region.yHigh - region.yLow) * 2 - 1).toFloat()
    val z = ((grid.share(point.z) * 2 - 1) * Z_HALF).toFloat()
    val top = view.project(Point3(x, y, z))
    val floor = view.project(Point3(x, y, -Z_HALF - FLOOR_GAP))
    drawLine(colors.text, top, floor, 1.dp.toPx(), pathEffect = dash)
    drawCircle(point.kind.color.copy(alpha = SHADOW_ALPHA), FLOOR_DOT.dp.toPx(), floor)
    val radius = (if (index == selected) BIG_POINT else POINT).dp.toPx()
    drawCircle(Color.White, radius + RING.dp.toPx(), top)
    drawCircle(point.kind.color, radius, top)
  }
}

private const val CORNERS = 4
private const val Z_HALF = 0.62f
private const val FLOOR_GAP = 0.08f
private const val PERSPECTIVE = 0.14f
private const val START_YAW = -0.65f
private const val START_PITCH = 0.55f
private const val MIN_PITCH = 0.05f
private const val MAX_PITCH = 1.5f
private const val MIN_ZOOM = 0.5f
private const val MAX_ZOOM = 3f
private const val TURN_SPEED = 0.01f
private const val REACH = 0.3f
private const val CENTER_DROP = -0.03f
private const val LINES = 8
private const val LIGHT_X = -0.45f
private const val LIGHT_Y = -0.55f
private const val LIGHT_Z = 0.8f
private const val AMBIENT = 0.62f
private const val LINE_ALPHA = 0.22f
private const val FLOOR_ALPHA = 0.35f
private const val SHADOW_ALPHA = 0.5f
private const val HAIRLINE = 0.7f
private const val LINE_WIDTH = 0.8f
private const val EDGE_WIDTH = 2.2f
private const val AXIS_WIDTH = 1.2f
private const val AXIS_MORE = 0.25f
private const val LABEL_OUT = 0.12f
private const val DASH = 3f
private const val FLOOR_DOT = 3f
private const val POINT = 6f
private const val BIG_POINT = 8.5f
private const val RING = 2f
private val VIEW_HEIGHT = 340.dp
