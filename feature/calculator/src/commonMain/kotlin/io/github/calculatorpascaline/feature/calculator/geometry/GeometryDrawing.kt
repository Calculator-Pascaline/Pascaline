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

package io.github.calculatorpascaline.feature.calculator.geometry

import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.rememberTextMeasurer
import io.github.calculatorpascaline.feature.calculator.electrical.drawLabel
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

internal class Point(val x: Double, val y: Double)

/** A line through [points], [dashed] for what is hidden or helps, [filled] for the face of a flat shape. */
internal class Line(val points: List<Point>, val dashed: Boolean = false, val closed: Boolean = false, val filled: Boolean = false)

internal class Label(val text: String, val at: Point)

/** A drawing in its own lengths, y going up. It is drawn to scale, as big as it fits. */
internal class Figure(val lines: List<Line>, val labels: List<Label> = emptyList())

@Composable
internal fun FigureView(figure: Figure, modifier: Modifier) {
  val measurer = rememberTextMeasurer()
  val color = MaterialTheme.colorScheme.primary
  val faded = MaterialTheme.colorScheme.onSurfaceVariant
  val fill = MaterialTheme.colorScheme.primary.copy(alpha = FILL_ALPHA)
  val style = MaterialTheme.typography.labelLarge.copy(color = MaterialTheme.colorScheme.onSurface)
  Canvas(modifier) {
    val all = figure.lines.flatMap { it.points } + figure.labels.map { it.at }
    val left = all.minOf { it.x }
    val bottom = all.minOf { it.y }
    val width = max(all.maxOf { it.x } - left, TINY)
    val height = max(all.maxOf { it.y } - bottom, TINY)
    val margin = MARGIN_DP * density
    val scale = min((size.width - 2 * margin) / width, (size.height - 2 * margin) / height)
    val dx = (size.width - width * scale) / 2
    val dy = (size.height - height * scale) / 2
    fun place(point: Point) = Offset((dx + (point.x - left) * scale).toFloat(), (size.height - dy - (point.y - bottom) * scale).toFloat())
    val dash = PathEffect.dashPathEffect(floatArrayOf(DASH_DP * density, DASH_DP * density))
    figure.lines.forEach { line ->
      val path = Path()
      line.points.forEachIndexed { index, point ->
        val at = place(point)
        if (index == 0) path.moveTo(at.x, at.y) else path.lineTo(at.x, at.y)
      }
      if (line.closed) path.close()
      if (line.filled) drawPath(path, fill)
      val stroke = Stroke(width = (if (line.dashed) THIN_DP else LINE_DP) * density, pathEffect = if (line.dashed) dash else null)
      drawPath(path, if (line.dashed) faded else color, style = stroke)
    }
    figure.labels.forEach { drawLabel(measurer, it.text, style, place(it.at)) }
  }
}

/** Labels for the corners and the sides of a polygon, a little outside of it. */
private fun outside(points: List<Point>, corners: List<String>, sides: List<String>): List<Label> {
  val size = max(points.maxOf { it.x } - points.minOf { it.x }, points.maxOf { it.y } - points.minOf { it.y })
  val cx = points.sumOf { it.x } / points.size
  val cy = points.sumOf { it.y } / points.size
  fun away(x: Double, y: Double): Point {
    val length = max(hypot(x - cx, y - cy), TINY)
    return Point(x + (x - cx) / length * size * LABEL_GAP, y + (y - cy) / length * size * LABEL_GAP)
  }
  val cornerLabels = corners.mapIndexedNotNull { i, text -> text.takeIf { it.isNotEmpty() }?.let { Label(it, away(points[i].x, points[i].y)) } }
  val sideLabels =
    sides.mapIndexedNotNull { i, text ->
      val a = points[i]
      val b = points[(i + 1) % points.size]
      text.takeIf { it.isNotEmpty() }?.let { Label(it, away((a.x + b.x) / 2, (a.y + b.y) / 2)) }
    }
  return cornerLabels + sideLabels
}

private fun polygon(points: List<Point>, extra: List<Line> = emptyList(), corners: List<String> = emptyList(), sides: List<String> = emptyList()) =
  Figure(listOf(Line(points, closed = true, filled = true)) + extra, outside(points, corners, sides))

/** Corner A at the origin, side c along the bottom, C above. */
internal fun triangleFigure(triangle: Triangle): Figure {
  val (_, b, c) = triangle.sides
  val angle = triangle.angles[0]
  val corners = listOf(Point(0.0, 0.0), Point(c, 0.0), Point(b * cos(angle), b * sin(angle)))
  // Side c is from A to B, a from B to C, b from C to A
  return polygon(corners, corners = listOf("A", "B", "C"), sides = listOf("c", "a", "b"))
}

internal fun quadFigure(quad: Quad, x: List<Double>): Figure =
  when (quad) {
    Quad.SQUARE -> box(x[0], x[0], "a", "a")
    Quad.RECTANGLE -> box(x[0], x[1], "a", "b")
    Quad.PARALLELOGRAM -> slanted(x[0], x[1], x[2])
    Quad.RHOMBUS -> slanted(x[0], x[0], x[1])
    Quad.TRAPEZOID -> trapezoidFigure(x[0], x[1], x[2], x.takeLast(2).first())
    Quad.KITE -> kiteFigure(x[0], x[1], x[2])
  }

private fun box(width: Double, height: Double, bottom: String, right: String): Figure {
  val corners = listOf(Point(0.0, 0.0), Point(width, 0.0), Point(width, height), Point(0.0, height))
  return polygon(corners, listOf(Line(listOf(corners[0], corners[2]), dashed = true)), sides = listOf(bottom, right, "", ""))
}

private fun slanted(base: Double, side: Double, angle: Double): Figure {
  val dx = side * cos(angle)
  val dy = side * sin(angle)
  val corners = listOf(Point(0.0, 0.0), Point(base, 0.0), Point(base + dx, dy), Point(dx, dy))
  val diagonals = listOf(Line(listOf(corners[0], corners[2]), dashed = true), Line(listOf(corners[1], corners[3]), dashed = true))
  return polygon(corners, diagonals, corners = listOf("α", "", "", ""), sides = listOf("a", "b", "", ""))
}

/** Isosceles unless the left [leg] is known. */
private fun trapezoidFigure(bottom: Double, top: Double, height: Double, leg: Double): Figure {
  val shift = if (leg.isFinite() && leg >= height) sqrt(leg * leg - height * height) else (bottom - top) / 2
  val corners = listOf(Point(0.0, 0.0), Point(bottom, 0.0), Point(shift + top, height), Point(shift, height))
  val heightLine = Line(listOf(corners[3], Point(shift, 0.0)), dashed = true)
  return polygon(corners, listOf(heightLine), sides = listOf("a", "d", "b", "c"))
}

/** The long diagonal stands up; the short one crosses it where the [side] fits, or at a quarter. */
private fun kiteFigure(long: Double, short: Double, side: Double): Figure {
  val half = short / 2
  val cross = if (side.isFinite() && side > half) min(sqrt(side * side - half * half), long) else long / KITE_CROSS
  val corners = listOf(Point(0.0, 0.0), Point(half, cross), Point(0.0, long), Point(-half, cross))
  val diagonals = listOf(Line(listOf(corners[0], corners[2]), dashed = true), Line(listOf(corners[1], corners[3]), dashed = true))
  return polygon(corners, diagonals, sides = listOf("a", "b", "", ""))
}

private fun arc(cx: Double, cy: Double, rx: Double, ry: Double, from: Double, to: Double): List<Point> =
  (0..STEPS).map { i ->
    val t = from + (to - from) * i / STEPS
    Point(cx + rx * cos(t), cy + ry * sin(t))
  }

internal fun circleFigure(radius: Double, angle: Double): Figure {
  val circle = Line(arc(0.0, 0.0, radius, radius, 0.0, 2 * PI), closed = true, filled = !angle.isFinite())
  val center = Point(0.0, 0.0)
  if (!angle.isFinite()) {
    return Figure(listOf(circle, Line(listOf(center, Point(radius, 0.0)), dashed = true)), listOf(Label("r", Point(radius / 2, radius * LABEL_GAP))))
  }
  val sector = Line(listOf(center) + arc(0.0, 0.0, radius, radius, 0.0, angle), closed = true, filled = true)
  val chord = Line(listOf(Point(radius, 0.0), Point(radius * cos(angle), radius * sin(angle))), dashed = true)
  val labels =
    listOf(Label("r", Point(radius / 2, -radius * LABEL_GAP)), Label("θ", Point(radius * LABEL_GAP * 2 * cos(angle / 2), radius * LABEL_GAP * 2 * sin(angle / 2))))
  return Figure(listOf(circle, sector, chord), labels)
}

/** Depth goes up and to the right, shortened. */
private fun oblique(x: Double, y: Double, z: Double) = Point(x + z * DEPTH * cos(DEPTH_ANGLE), y + z * DEPTH * sin(DEPTH_ANGLE))

internal fun solidFigure(kind: SolidKind, x: List<Double>): Figure =
  when (kind) {
    SolidKind.CUBE -> boxFigure(x[0], x[0], x[0])
    SolidKind.CUBOID -> boxFigure(x[0], x[1], x[2])
    SolidKind.SPHERE -> sphereFigure(x[0])
    SolidKind.CYLINDER -> roundFigure(x[0], x[1], pointed = false)
    SolidKind.CONE -> roundFigure(x[0], x[1], pointed = true)
    SolidKind.PYRAMID -> pyramidFigure(x[0], x[1])
  }

private fun boxFigure(length: Double, width: Double, height: Double): Figure {
  fun p(a: Double, b: Double, c: Double) = oblique(a * length, b * height, c * width)
  val front = Line(listOf(p(0.0, 0.0, 0.0), p(1.0, 0.0, 0.0), p(1.0, 1.0, 0.0), p(0.0, 1.0, 0.0)), closed = true, filled = true)
  val top = Line(listOf(p(0.0, 1.0, 0.0), p(0.0, 1.0, 1.0), p(1.0, 1.0, 1.0), p(1.0, 1.0, 0.0)))
  val side = Line(listOf(p(1.0, 0.0, 0.0), p(1.0, 0.0, 1.0), p(1.0, 1.0, 1.0)))
  val hidden = Line(listOf(p(0.0, 1.0, 1.0), p(0.0, 0.0, 1.0), p(1.0, 0.0, 1.0)), dashed = true)
  val hiddenDepth = Line(listOf(p(0.0, 0.0, 0.0), p(0.0, 0.0, 1.0)), dashed = true)
  val gap = max(length, height) * LABEL_GAP
  val labels =
    listOf(Label("l", Point(length / 2, -gap)), Label("h", Point(-gap, height / 2)), Label("w", oblique(length + gap, 0.0, width / 2)))
  return Figure(listOf(front, top, side, hidden, hiddenDepth), labels)
}

private fun sphereFigure(radius: Double): Figure {
  val flat = radius * FLATTEN
  val outline = Line(arc(0.0, 0.0, radius, radius, 0.0, 2 * PI), closed = true, filled = true)
  val front = Line(arc(0.0, 0.0, radius, flat, PI, 2 * PI))
  val back = Line(arc(0.0, 0.0, radius, flat, 0.0, PI), dashed = true)
  val radiusLine = Line(listOf(Point(0.0, 0.0), Point(radius, 0.0)), dashed = true)
  return Figure(listOf(outline, front, back, radiusLine), listOf(Label("r", Point(radius / 2, flat / 2))))
}

/** A cylinder, or a cone when [pointed]. */
private fun roundFigure(radius: Double, height: Double, pointed: Boolean): Figure {
  val flat = radius * FLATTEN
  val lines =
    mutableListOf(
      Line(arc(0.0, 0.0, radius, flat, PI, 2 * PI)),
      Line(arc(0.0, 0.0, radius, flat, 0.0, PI), dashed = true),
      Line(listOf(Point(0.0, 0.0), Point(radius, 0.0)), dashed = true),
      Line(listOf(Point(0.0, 0.0), Point(0.0, height)), dashed = true),
    )
  if (pointed) {
    lines += Line(listOf(Point(-radius, 0.0), Point(0.0, height), Point(radius, 0.0)))
  } else {
    lines += Line(arc(0.0, height, radius, flat, 0.0, 2 * PI), closed = true, filled = true)
    lines += Line(listOf(Point(-radius, 0.0), Point(-radius, height)))
    lines += Line(listOf(Point(radius, 0.0), Point(radius, height)))
  }
  val gap = max(radius, height) * LABEL_GAP
  return Figure(lines, listOf(Label("r", Point(radius / 2, gap)), Label("h", Point(gap, height / 2))))
}

private fun pyramidFigure(side: Double, height: Double): Figure {
  fun p(a: Double, c: Double) = oblique(a * side, 0.0, c * side)
  val apex = oblique(side / 2, height, side / 2)
  val base = listOf(p(0.0, 0.0), p(1.0, 0.0), p(1.0, 1.0), p(0.0, 1.0))
  val front = Line(listOf(base[0], base[1], apex), closed = true, filled = true)
  val right = Line(listOf(base[1], base[2], apex))
  val hidden = Line(listOf(base[0], base[3], base[2]), dashed = true)
  val hiddenEdge = Line(listOf(base[3], apex), dashed = true)
  val axis = Line(listOf(oblique(side / 2, 0.0, side / 2), apex), dashed = true)
  val gap = side * LABEL_GAP
  return Figure(listOf(front, right, hidden, hiddenEdge, axis), listOf(Label("a", Point(side / 2, -gap)), Label("h", oblique(side / 2 + gap, height / 2, side / 2))))
}

private const val FILL_ALPHA = 0.12f
private const val MARGIN_DP = 20f
private const val DASH_DP = 5f
private const val LINE_DP = 2.5f
private const val THIN_DP = 1.5f
private const val TINY = 1e-9
private const val LABEL_GAP = 0.08
private const val KITE_CROSS = 4
private const val STEPS = 72
private const val DEPTH = 0.5
private const val DEPTH_ANGLE = PI / 6
private const val FLATTEN = 0.3
