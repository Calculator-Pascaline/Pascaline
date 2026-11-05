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

package com.sadellie.unitto.feature.calculator.place

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.sadellie.unitto.core.designsystem.icons.symbols.Add
import com.sadellie.unitto.core.designsystem.icons.symbols.Remove
import com.sadellie.unitto.core.designsystem.icons.symbols.Symbols
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.roundToInt

/** Where the map looks: the center as a fraction of the world's width and height, and the zoom. */
private class MapView(centerX: Float, centerY: Float) {
  var centerX by mutableFloatStateOf(centerX)
  var centerY by mutableFloatStateOf(centerY)
  var zoom by mutableFloatStateOf(1f)

  /** Size of the map when it was last drawn, for the zoom buttons. */
  var size = Size.Zero

  /** Zooms around the middle of the map. */
  fun zoomCenter(factor: Float) {
    if (size.minDimension > 0) zoomBy(factor, size.center, size.width, size.height)
  }

  /** Width of the whole world in pixels for a [width] × [height] map. */
  fun worldWidth(width: Float, height: Float) = max(width, 2 * height) * zoom

  /** Fraction of the world under the screen point [x], [y]. */
  fun fractionAt(x: Float, y: Float, width: Float, height: Float): Pair<Float, Float> {
    val world = worldWidth(width, height)
    return (centerX + (x - width / 2) / world) to (centerY + (y - height / 2) / (world / 2))
  }

  /** Zooms by [factor] keeping the point under [anchor] still. */
  fun zoomBy(factor: Float, anchor: Offset, width: Float, height: Float) {
    val (fx, fy) = fractionAt(anchor.x, anchor.y, width, height)
    zoom = (zoom * factor).coerceIn(1f, MAX_ZOOM)
    val world = worldWidth(width, height)
    centerX = fx - (anchor.x - width / 2) / world
    centerY = fy - (anchor.y - height / 2) / (world / 2)
    clamp(width, height)
  }

  fun panBy(pan: Offset, width: Float, height: Float) {
    val world = worldWidth(width, height)
    centerX -= pan.x / world
    centerY -= pan.y / (world / 2)
    clamp(width, height)
  }

  /** East and west wrap around, north and south stop at the poles. */
  fun clamp(width: Float, height: Float) {
    centerX -= floor(centerX)
    val half = height / 2 / (worldWidth(width, height) / 2)
    centerY = if (half >= MIDDLE) MIDDLE else centerY.coerceIn(half, 1 - half)
  }
}

/**
 * The world from Trail Sense's land map, no network needed. Drag and pinch (or the wheel) to move
 * and zoom, a tap chooses the point.
 */
@Composable
internal fun WorldMap(land: ImageBitmap?, point: GeoPoint, onChoose: (GeoPoint) -> Unit, modifier: Modifier = Modifier) {
  val view = remember { MapView(((point.longitude + 180) / 360).toFloat(), ((90 - point.latitude) / 180).toFloat()) }
  val sea = MaterialTheme.colorScheme.surfaceContainerHighest
  val pin = MaterialTheme.colorScheme.primary
  val ring = MaterialTheme.colorScheme.onPrimary
  Box(modifier = modifier.clip(MaterialTheme.shapes.extraLarge).background(sea)) {
    Canvas(
      modifier =
        Modifier.fillMaxSize()
          .pointerInput(Unit) {
            detectTransformGestures { centroid, pan, gestureZoom, _ ->
              val w = size.width.toFloat()
              val h = size.height.toFloat()
              view.zoomBy(gestureZoom, centroid, w, h)
              view.panBy(pan, w, h)
            }
          }
          .pointerInput(Unit) {
            detectTapGestures { offset ->
              val (fx, fy) = view.fractionAt(offset.x, offset.y, size.width.toFloat(), size.height.toFloat())
              onChoose(pointOf(fx.toDouble(), fy.toDouble()))
            }
          }
          .pointerInput(Unit) {
            awaitPointerEventScope {
              while (true) {
                val event = awaitPointerEvent()
                val change = event.changes.firstOrNull()
                if (event.type == PointerEventType.Scroll && change != null) {
                  val factor = if (change.scrollDelta.y > 0) 1 / WHEEL_ZOOM else WHEEL_ZOOM
                  view.zoomBy(factor, change.position, size.width.toFloat(), size.height.toFloat())
                  change.consume()
                }
              }
            }
          }
    ) {
      view.size = size
      view.clamp(size.width, size.height)
      val world = view.worldWidth(size.width, size.height)
      val left = size.width / 2 - view.centerX * world
      val top = size.height / 2 - view.centerY * world / 2
      if (land != null) {
        // The world and its copies east and west, for the wrap around
        for (copy in -1..1) drawWorld(land, left + copy * world, top, world)
      }
      val pinX = left + ((point.longitude + 180) / 360 * world).toFloat()
      val pinY = top + ((90 - point.latitude) / 180 * world / 2).toFloat()
      for (copy in -1..1) drawPin(Offset(pinX + copy * world, pinY), pin, ring)
    }
    Column(modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp)) {
      FilledTonalIconButton(onClick = { view.zoomCenter(BUTTON_ZOOM) }) {
        Icon(Symbols.Add, null)
      }
      FilledTonalIconButton(onClick = { view.zoomCenter(1 / BUTTON_ZOOM) }) {
        Icon(Symbols.Remove, null)
      }
    }
  }
}

private fun DrawScope.drawWorld(land: ImageBitmap, left: Float, top: Float, world: Float) {
  if (left > size.width || left + world < 0) return
  drawImage(
    image = land,
    srcOffset = IntOffset.Zero,
    srcSize = IntSize(land.width, land.height),
    dstOffset = IntOffset(left.roundToInt(), top.roundToInt()),
    dstSize = IntSize(world.roundToInt(), (world / 2).roundToInt()),
    filterQuality = FilterQuality.Low,
  )
}

private fun DrawScope.drawPin(center: Offset, color: Color, ring: Color) {
  if (center.x < -PIN_RADIUS || center.x > size.width + PIN_RADIUS) return
  val radius = PIN_RADIUS.dp.toPx()
  drawCircle(ring, radius + 2.dp.toPx(), center)
  drawCircle(color, radius, center)
  drawCircle(ring, radius * PIN_DOT, center, style = Stroke(1.dp.toPx()))
}

/** Latitude and longitude at the fractions [fx] and [fy] of the world's width and height. */
internal fun pointOf(fx: Double, fy: Double): GeoPoint =
  GeoPoint(MAX_LATITUDE - fy.coerceIn(0.0, 1.0) * 2 * MAX_LATITUDE, fx * FULL_TURN - FULL_TURN / 2)

private const val MAX_ZOOM = 24f
private const val MIDDLE = 0.5f
private const val PIN_DOT = 0.33f
private const val MAX_LATITUDE = 90.0
private const val FULL_TURN = 360.0
private const val WHEEL_ZOOM = 1.25f
private const val BUTTON_ZOOM = 2f
private const val PIN_RADIUS = 8
