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

package com.sadellie.unitto.feature.calculator.integrals

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.sadellie.unitto.feature.calculator.electrical.drawLabel
import kotlin.math.cos
import kotlin.math.sin

/**
 * Where the drawing is in x, y and z, to fit it in the view whatever its size. Proportions are
 * kept, except that no direction is less than half of the longest, so that thin shapes show.
 */
private class Box3(corners: List<Corner>) {
  private val low = doubleArrayOf(corners.minOf { it.x }, corners.minOf { it.y }, minOf(0.0, corners.minOf { it.z }))
  private val high = doubleArrayOf(corners.maxOf { it.x }, corners.maxOf { it.y }, maxOf(0.0, corners.maxOf { it.z }))
  private val longest = (0..2).maxOf { high[it] - low[it] }.takeIf { it > 0 } ?: 1.0

  /** Half of the size of each direction on the screen, the longest is 1. */
  val extent = DoubleArray(3) { maxOf(high[it] - low[it], longest * SHORTEST) / longest }

  private fun scaled(value: Double, axis: Int): Double {
    val span = high[axis] - low[axis]
    return if (span > 0) ((value - low[axis]) / span * 2 - 1) * extent[axis] else 0.0
  }

  fun normal(x: Double, y: Double, z: Double) = Triple(scaled(x, 0), scaled(y, 1), scaled(z, 2))
}

/** Turned by [yaw] around the up direction, then tilted by [pitch] towards the viewer. */
private class Camera(val yaw: Float, val pitch: Float, val center: Offset, val reach: Float) {
  /** Where it is on the screen, and how far: the far faces are drawn first. */
  fun project(point: Triple<Double, Double, Double>): Pair<Offset, Double> {
    val (x, y, z) = point
    val x1 = x * cos(yaw) - y * sin(yaw)
    val y1 = x * sin(yaw) + y * cos(yaw)
    val depth = y1 * cos(pitch) - z * sin(pitch)
    val up = y1 * sin(pitch) + z * cos(pitch)
    return Offset(center.x + (x1 * reach).toFloat(), center.y - (up * reach).toFloat()) to depth
  }
}

private class ViewColors(val low: Color, val high: Color, val floor: Color, val wall: Color, val line: Color, val axis: Color)

/** The graph over its region, or the solid, in 3D. A finger turns it. */
@Composable
internal fun IntegralView(faces: List<Face>, rough: List<Face>) {
  var yaw by rememberSaveable { mutableFloatStateOf(START_YAW) }
  var pitch by rememberSaveable { mutableFloatStateOf(START_PITCH) }
  // While a finger turns it, a lighter drawing without lines keeps up with the finger
  var isTurning by remember { mutableStateOf(false) }
  val box = remember(faces) { faces.takeIf { it.isNotEmpty() }?.let { shown -> Box3(shown.flatMap { it.corners }) } }
  val fine = remember(faces, box) { box?.let { prepare(faces, it) }.orEmpty() }
  val light = remember(rough, box) { box?.let { prepare(rough, it) }.orEmpty() }
  val measurer = rememberTextMeasurer()
  val scheme = MaterialTheme.colorScheme
  val colors =
    ViewColors(
      low = scheme.primaryContainer,
      high = scheme.primary,
      floor = scheme.outline.copy(alpha = FLOOR_ALPHA),
      wall = scheme.tertiary.copy(alpha = WALL_ALPHA),
      line = scheme.onSurface.copy(alpha = LINE_ALPHA),
      axis = scheme.onSurfaceVariant,
    )
  val label = MaterialTheme.typography.labelLarge.copy(color = scheme.onSurfaceVariant)
  Canvas(
    Modifier.fillMaxWidth().height(VIEW_HEIGHT).pointerInput(Unit) {
      detectDragGestures(
        onDragStart = { isTurning = true },
        onDragEnd = { isTurning = false },
        onDragCancel = { isTurning = false },
      ) { change, drag ->
        change.consume()
        yaw -= drag.x * TURN_SPEED
        pitch = (pitch + drag.y * TURN_SPEED).coerceIn(-MAX_PITCH, MAX_PITCH)
      }
    }
  ) {
    val shownBox = box ?: return@Canvas
    val camera = Camera(yaw, pitch, center, size.minDimension * REACH)
    drawAxes(shownBox, camera, colors.axis, measurer, label)
    val isLight = isTurning && light.isNotEmpty()
    drawFaces(if (isLight) light else fine, camera, colors, withLines = !isLight)
  }
}

/** A face with its corners already fitted in the box: turning only projects them. */
private class PlacedFace(val face: Face, val corners: List<Triple<Double, Double, Double>>, val shade: Float)

private fun prepare(faces: List<Face>, box: Box3) =
  faces.map { face ->
    PlacedFace(face, face.corners.map { box.normal(it.x, it.y, it.z) }, (face.corners.sumOf { it.shade } / face.corners.size).toFloat())
  }

private fun DrawScope.drawFaces(faces: List<PlacedFace>, camera: Camera, colors: ViewColors, withLines: Boolean) {
  val line = Stroke(width = LINE_WIDTH.dp.toPx())
  faces
    .map { placed ->
      val projected = placed.corners.map { camera.project(it) }
      Triple(placed, projected.map { it.first }, projected.sumOf { it.second } / projected.size)
    }
    .sortedByDescending { it.third }
    .forEach { (placed, points, _) ->
      val face = placed.face
      val path = Path()
      path.moveTo(points[0].x, points[0].y)
      points.drop(1).forEach { path.lineTo(it.x, it.y) }
      path.close()
      val color =
        when (face.kind) {
          FaceKind.SURFACE -> lerp(colors.low, colors.high, placed.shade)
          FaceKind.FLOOR -> colors.floor
          FaceKind.WALL -> colors.wall
        }
      drawPath(path, color)
      if (withLines && face.kind != FaceKind.WALL) drawPath(path, colors.line, style = line)
    }
}

/** The three axes from the lowest corner of the box, with their names at the end. */
private fun DrawScope.drawAxes(box: Box3, camera: Camera, color: Color, measurer: TextMeasurer, label: TextStyle) {
  val (dx, dy, dz) = Triple(box.extent[0], box.extent[1], box.extent[2])
  val origin = camera.project(Triple(-dx, -dy, -dz)).first
  listOf(
      Triple(dx + AXIS_MORE, -dy, -dz) to "x",
      Triple(-dx, dy + AXIS_MORE, -dz) to "y",
      Triple(-dx, -dy, dz + AXIS_MORE) to "z",
    )
    .forEach { (end, name) ->
      val point = camera.project(end).first
      drawLine(color, origin, point, strokeWidth = AXIS_WIDTH.dp.toPx())
      drawLabel(measurer, name, label, point + (point - origin) * LABEL_OUT)
    }
}

private const val START_YAW = -0.7f
private const val START_PITCH = 0.45f
private const val MAX_PITCH = 1.5f
private const val TURN_SPEED = 0.01f
private const val REACH = 0.27f
private const val FLOOR_ALPHA = 0.25f
private const val WALL_ALPHA = 0.3f
private const val LINE_ALPHA = 0.18f
private const val LINE_WIDTH = 0.6f
private const val AXIS_WIDTH = 1.5f
private const val AXIS_MORE = 0.3
private const val SHORTEST = 0.5
private const val LABEL_OUT = 0.12f
private val VIEW_HEIGHT = 320.dp
