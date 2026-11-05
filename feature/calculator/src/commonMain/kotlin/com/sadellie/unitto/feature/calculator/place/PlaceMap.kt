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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import com.sadellie.unitto.core.designsystem.icons.symbols.Add
import com.sadellie.unitto.core.designsystem.icons.symbols.Remove
import com.sadellie.unitto.core.designsystem.icons.symbols.Symbols
import kotlin.math.ceil
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

  /** Cities labeled at the last drawing and where their dots are, a tap close to one picks it. */
  var labeled: List<Pair<City, Offset>> = emptyList()

  fun resize(newSize: Size) {
    size = newSize
    if (size.minDimension > 0) clamp(size.width, size.height)
  }

  /** Zooms around the middle of the map. */
  fun zoomCenter(factor: Float) {
    if (size.minDimension > 0) zoomBy(factor, size.center, size.width, size.height)
  }

  /** Centers on [point], zoomed in to at least [minZoom]. */
  fun focus(point: GeoPoint, minZoom: Float) {
    zoom = max(zoom, minZoom).coerceIn(1f, MAX_ZOOM)
    centerX = ((point.longitude + HALF_TURN) / FULL_TURN).toFloat()
    centerY = ((MAX_LATITUDE - point.latitude) / (2 * MAX_LATITUDE)).toFloat()
    if (size.minDimension > 0) clamp(size.width, size.height)
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
 * The world from the coasts and cities of Natural Earth with Trail Sense's land colors, no network
 * needed. Coasts are shapes, so they stay sharp at any zoom. Big cities are named first, more appear
 * as the map zooms in. Drag and pinch (or the wheel) to move and zoom, a tap chooses the point, or
 * the city whose dot is under the finger.
 *
 * @param focus A point to fly to and a number that changes each time, like after a search.
 * @param buttonsBottom Space under the zoom buttons, for what covers the bottom of the map.
 */
@Composable
internal fun WorldMap(
  land: LandMap?,
  cities: List<City>,
  language: String,
  point: GeoPoint,
  focus: Pair<GeoPoint, Int>?,
  onChoose: (GeoPoint) -> Unit,
  modifier: Modifier = Modifier,
  buttonsBottom: Dp = 8.dp,
) {
  val view = remember { MapView(0.5f, 0.5f).apply { focus(point, 1f) } }
  LaunchedEffect(focus) { focus?.let { view.focus(it.first, FOCUS_ZOOM) } }
  val currentOnChoose by rememberUpdatedState(onChoose)
  val sea = MaterialTheme.colorScheme.surfaceContainerHighest
  val pin = MaterialTheme.colorScheme.primary
  val ring = MaterialTheme.colorScheme.onPrimary
  val measurer = rememberTextMeasurer(cacheSize = LABEL_CACHE)
  val labelStyle =
    MaterialTheme.typography.labelSmall.copy(
      color = MaterialTheme.colorScheme.onSurface,
      shadow = Shadow(MaterialTheme.colorScheme.surface, blurRadius = HALO),
    )
  val bigLabelStyle = MaterialTheme.typography.labelLarge.merge(labelStyle)
  val dotColor = MaterialTheme.colorScheme.onSurfaceVariant
  Box(modifier = modifier.background(sea)) {
    Canvas(
      modifier =
        Modifier.fillMaxSize()
          .onSizeChanged { view.resize(it.toSize()) }
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
              val touch = TOUCH_RADIUS.dp.toPx()
              val city = view.labeled.firstOrNull { (_, dot) -> (dot - offset).getDistance() < touch }?.first
              val (fx, fy) = view.fractionAt(offset.x, offset.y, size.width.toFloat(), size.height.toFloat())
              currentOnChoose(city?.point ?: pointOf(fx.toDouble(), fy.toDouble()))
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
      val world = view.worldWidth(size.width, size.height)
      val left = size.width / 2 - view.centerX * world
      val top = size.height / 2 - view.centerY * world / 2
      if (land != null) drawLand(land, left, top, world)
      fun screen(p: GeoPoint, copy: Int) =
        Offset(
          left + copy * world + ((p.longitude + HALF_TURN) / FULL_TURN * world).toFloat(),
          top + ((MAX_LATITUDE - p.latitude) / (2 * MAX_LATITUDE) * world / 2).toFloat(),
        )
      val labels = CityLabels(measurer, labelStyle, bigLabelStyle, dotColor, language)
      val pins = (-1..1).map { screen(point, it) }
      view.labeled =
        labels.draw(this, cities, webZoom(world / density), pins) { city -> (-1..1).map { screen(city.point, it) } }
      for (copy in -1..1) drawPin(screen(point, copy), pin, ring)
    }
    Column(modifier = Modifier.align(Alignment.BottomEnd).padding(end = 8.dp, bottom = buttonsBottom)) {
      FilledTonalIconButton(onClick = { view.zoomCenter(BUTTON_ZOOM) }) { Icon(Symbols.Add, null) }
      FilledTonalIconButton(onClick = { view.zoomCenter(1 / BUTTON_ZOOM) }) { Icon(Symbols.Remove, null) }
    }
  }
}

/** Places names without overlaps, the biggest cities first, and returns those it named. */
private class CityLabels(
  private val measurer: TextMeasurer,
  private val style: TextStyle,
  private val bigStyle: TextStyle,
  private val dotColor: Color,
  private val language: String,
) {
  fun draw(
    scope: DrawScope,
    cities: List<City>,
    zoom: Float,
    pins: List<Offset>,
    positions: (City) -> List<Offset>,
  ): List<Pair<City, Offset>> =
    with(scope) {
      val taken = mutableListOf<Rect>()
      val labeled = mutableListOf<Pair<City, Offset>>()
      val bounds = Rect(Offset.Zero, size)
      val gap = LABEL_GAP.dp.toPx()
      val dot = DOT_RADIUS.dp.toPx()
      val pinRadius = (PIN_RADIUS + 2).dp.toPx()
      // The file is sorted by population, so big cities win the space
      cities
        .asSequence()
        .filter { it.minZoom <= zoom + LABEL_EARLIER }
        .mapNotNull { city -> positions(city).firstOrNull { bounds.contains(it) }?.let { city to it } }
        .takeWhile { labeled.size < MAX_LABELS }
        .forEach { (city, at) ->
          val layout = measurer.measure(city.name(language), if (city.population >= BIG_CITY) bigStyle else style)
          // A label starts after the pin when the pin sits on its city
          val start = if (pins.any { (it - at).getDistance() < pinRadius }) pinRadius else dot
          val box =
            Rect(
              at.x + start + gap,
              at.y - layout.size.height / 2f,
              at.x + start + gap + layout.size.width,
              at.y + layout.size.height / 2f,
            )
          if (taken.none { it.overlaps(box.inflate(gap)) }) {
            taken += box
            taken += Rect(at, dot * 2)
            drawCircle(dotColor, dot, at)
            drawText(layout, topLeft = box.topLeft)
            labeled += city to at
          }
        }
      labeled
    }
}

/** Land colors seen through the coast, on the world and its copies east and west for the wrap around. */
private fun DrawScope.drawLand(land: LandMap, left: Float, top: Float, world: Float) {
  val pixelsPerDegree = (world / FULL_TURN).toFloat()
  val canvas = drawContext.canvas
  canvas.save()
  canvas.translate(left, top)
  canvas.scale(pixelsPerDegree, pixelsPerDegree)
  canvas.clipPath(land.coast(coastArea(left, top, size.width, size.height, world)))
  canvas.scale(1 / pixelsPerDegree, 1 / pixelsPerDegree)
  canvas.translate(-left, -top)
  for (copy in -1..1) drawWorld(land.colors, left + copy * world, top, world)
  canvas.restore()
}

/** Draws only the part of the colors that is on screen, the world can be very wide when zoomed in. */
private fun DrawScope.drawWorld(land: ImageBitmap, left: Float, top: Float, world: Float) {
  val height = world / 2
  val visibleLeft = max(0f, -left)
  val visibleTop = max(0f, -top)
  val visibleRight = minOf(world, size.width - left)
  val visibleBottom = minOf(height, size.height - top)
  if (visibleRight <= visibleLeft || visibleBottom <= visibleTop) return
  val scale = land.width / world
  // A pixel more on each side, so that colors blend the same while panning
  val srcLeft = (floor(visibleLeft * scale).toInt() - 1).coerceIn(0, land.width - 1)
  val srcTop = (floor(visibleTop * scale).toInt() - 1).coerceIn(0, land.height - 1)
  val srcRight = (ceil(visibleRight * scale).toInt() + 1).coerceIn(srcLeft + 1, land.width)
  val srcBottom = (ceil(visibleBottom * scale).toInt() + 1).coerceIn(srcTop + 1, land.height)
  drawImage(
    image = land,
    srcOffset = IntOffset(srcLeft, srcTop),
    srcSize = IntSize(srcRight - srcLeft, srcBottom - srcTop),
    dstOffset = IntOffset((left + srcLeft / scale).roundToInt(), (top + srcTop / scale).roundToInt()),
    // One more pixel to cover the rounding where a copy of the world starts
    dstSize = IntSize(((srcRight - srcLeft) / scale).roundToInt() + 1, ((srcBottom - srcTop) / scale).roundToInt()),
    filterQuality = FilterQuality.Low,
  )
}

private fun DrawScope.drawPin(center: Offset, color: Color, ring: Color) {
  val radius = PIN_RADIUS.dp.toPx()
  if (center.x < -radius || center.x > size.width + radius) return
  drawCircle(ring, radius + 2.dp.toPx(), center)
  drawCircle(color, radius, center)
  drawCircle(ring, radius * PIN_DOT, center, style = Stroke(1.dp.toPx()))
}

/** Latitude and longitude at the fractions [fx] and [fy] of the world's width and height. */
internal fun pointOf(fx: Double, fy: Double): GeoPoint =
  GeoPoint(MAX_LATITUDE - fy.coerceIn(0.0, 1.0) * 2 * MAX_LATITUDE, fx * FULL_TURN - HALF_TURN)

private const val MAX_ZOOM = 512f
private const val FOCUS_ZOOM = 40f
private const val WHEEL_ZOOM = 1.25f
private const val BUTTON_ZOOM = 2f
private const val MIDDLE = 0.5f
private const val PIN_RADIUS = 8
private const val PIN_DOT = 0.33f
private const val MAX_LATITUDE = 90.0
private const val FULL_TURN = 360.0
private const val HALF_TURN = 180.0
private const val DOT_RADIUS = 2.5f
private const val LABEL_GAP = 3
private const val MAX_LABELS = 250
private const val LABEL_CACHE = 400
/** Natural Earth's zooms are for 256 px tiles, labels can come a bit earlier on a phone. */
private const val LABEL_EARLIER = 1.5f
private const val BIG_CITY = 5_000_000L
private const val HALO = 6f
private const val TOUCH_RADIUS = 16
