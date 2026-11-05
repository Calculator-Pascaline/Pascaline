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

import androidx.compose.ui.graphics.ImageBitmap
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.jetbrains.compose.resources.decodeToImageBitmap
import unitto.feature.calculator.generated.resources.Res

/** Climate of a point at sea level: coefficients of the daily lows and highs. */
internal data class TemperatureClimate(val lows: DoubleArray, val highs: DoubleArray) {
  override fun equals(other: Any?): Boolean =
    other is TemperatureClimate && lows.contentEquals(other.lows) && highs.contentEquals(other.highs)

  override fun hashCode(): Int = 31 * lows.contentHashCode() + highs.contentHashCode()
}

/**
 * Reads the bundled data images (WebP in the resources) at a point. Images are decoded by Compose,
 * so the same code works on Android and in the browser. Large ones are dropped once read.
 */
internal class PlaceData {
  private val mutex = Mutex()
  private val kept = mutableMapOf<String, ImageBitmap>()
  private var elevationTiles: List<ElevationTile>? = null
  private var zones: List<ZoneCity>? = null
  private var cities: List<City>? = null

  /** Read once, the view model keeps it. */
  suspend fun landMap(): LandMap =
    LandMap(
      colors = image(LAND_COLORS),
      simple = CoastTiles(Res.readBytes("$ROOT/$SIMPLE_COAST")),
      detailed = CoastTiles(Res.readBytes("$ROOT/$DETAILED_COAST")),
    )

  suspend fun temperatures(point: GeoPoint): TemperatureClimate =
    TemperatureClimate(temperature(LOW_TEMPERATURES, point), temperature(HIGH_TEMPERATURES, point))

  private suspend fun temperature(model: TemperatureModel, point: GeoPoint): DoubleArray {
    val (x, y) = worldPixel(point.latitude, point.longitude, TEMPERATURE_WIDTH, TEMPERATURE_DEGREES_PER_ROW)
    val pixels = DoubleArray(model.a.size) { bilinearRed(image("temperatures/${model.name}-$it.webp"), x, y) }
    return temperatureCoefficients(model, pixels, point.latitude)
  }

  /** Millimeters of each month, January first, for [year]'s month lengths. */
  suspend fun precipitation(point: GeoPoint, year: Int): DoubleArray {
    val (x, y) = worldPixel(point.latitude, point.longitude, PRECIPITATION_WIDTH, PRECIPITATION_DEGREES_PER_ROW)
    val days = daysInMonths(year)
    return DoubleArray(MONTHS) { month ->
      val image = image("precipitation/precipitation-${month + 1}.webp")
      precipitation(red(image.pixel(x.roundToInt(), y.roundToInt())), days[month])
    }
  }

  /** Ground elevation in meters, 0 at sea. */
  suspend fun elevation(point: GeoPoint): Double {
    val tiles = mutex.withLock {
      elevationTiles ?: parseElevationIndex(Res.readBytes("$ROOT/dem/index.json").decodeToString()).also {
        elevationTiles = it
      }
    }
    val tile = tiles.tileAt(point) ?: return 0.0
    val image = image("dem/${tile.file}.webp")
    val (x, y) = tile.pixel(point, image.width, image.height)
    return tile.elevation(red(image.pixel(x, y)))
  }

  /** Tide constituents of the closest sea cell within 2°, and how far it is. Null inland. */
  suspend fun tides(point: GeoPoint): TideSite? {
    val index = image(TIDE_INDEX, keep = true)
    val (fx, fy) =
      worldPixel(point.latitude, point.longitude, TIDE_INDEX_WIDTH, TIDE_DEGREES_PER_ROW, TIDE_INDEX_OFFSET)
    val (cell, sea) = nearestCell(index, fx.roundToInt(), fy.roundToInt()) ?: return null
    val x = red(cell) - 1
    val y = green(cell) - 1
    val harmonics = Constituent.entries.chunked(PER_FILE).flatMapIndexed { group, constituents ->
      val first = group * PER_FILE + 1
      val amplitudes = image("tides/tide-amplitudes-$first-${first + PER_FILE - 1}.webp", keep = true).pixel(x, y)
      val phases = image("tides/tide-phases-$first-${first + PER_FILE - 1}.webp", keep = true).pixel(x, y)
      constituents.mapIndexed { channel, constituent ->
        tideHarmonic(constituent, channel(amplitudes, channel), channel(phases, channel), x, y)
      }
    }
    return TideSite(harmonics, greatCircle(point, sea) * KM_PER_DEGREE)
  }

  /**
   * The index pixel closest to ([x], [y]) that points to a cell, in rings of growing size, and
   * where that pixel is.
   */
  private fun nearestCell(index: ImageBitmap, x: Int, y: Int): Pair<Int, GeoPoint>? =
    (0..TIDE_SEARCH).firstNotNullOfOrNull { radius ->
      ring(radius)
        .filter { (_, dy) -> y + dy in 0 until index.height }
        .map { (dx, dy) -> Triple(index.pixel((x + dx).mod(index.width), y + dy), dx, dy) }
        .filter { (pixel, _, _) -> red(pixel) > 0 || green(pixel) > 0 }
        .minByOrNull { (_, dx, dy) -> dx * dx + dy * dy }
        ?.let { (pixel, dx, dy) -> pixel to tidePixelPoint(x + dx, y + dy) }
    }

  /** Natural Earth's cities, the biggest first. */
  suspend fun cities(): List<City> = mutex.withLock {
    cities ?: parseCities(Res.readBytes("$ROOT/cities.tsv").decodeToString()).also { cities = it }
  }

  suspend fun zones(): List<ZoneCity> = mutex.withLock {
    zones ?: parseZones(Res.readBytes("$ROOT/zone1970.tab").decodeToString()).also { zones = it }
  }

  private suspend fun image(path: String, keep: Boolean = false): ImageBitmap {
    mutex.withLock { kept[path] }?.let { return it }
    val image = Res.readBytes("$ROOT/$path").decodeToImageBitmap()
    if (keep) mutex.withLock { kept[path] = image }
    return image
  }

  private fun bilinearRed(image: ImageBitmap, x: Double, y: Double): Double {
    val x0 = floor(x).toInt()
    val y0 = floor(y).toInt()
    val fx = x - x0
    val fy = y - y0
    fun at(px: Int, py: Int) = red(image.pixel(px.mod(image.width), py.coerceIn(0, image.height - 1))).toDouble()
    val top = at(x0, y0) * (1 - fx) + at(x0 + 1, y0) * fx
    val bottom = at(x0, y0 + 1) * (1 - fx) + at(x0 + 1, y0 + 1) * fx
    return top * (1 - fy) + bottom * fy
  }
}

private fun ImageBitmap.pixel(x: Int, y: Int): Int {
  val buffer = IntArray(1)
  readPixels(buffer, x.mod(width), y.coerceIn(0, height - 1), 1, 1)
  return buffer[0]
}

/** Offsets on the square ring at [radius] around a pixel. */
private fun ring(radius: Int): List<Pair<Int, Int>> =
  (-radius..radius)
    .flatMap { dy -> (-radius..radius).map { dx -> dx to dy } }
    .filter { (dx, dy) -> maxOf(kotlin.math.abs(dx), kotlin.math.abs(dy)) == radius }

private fun red(argb: Int) = (argb shr RED_SHIFT) and BYTE

private fun green(argb: Int) = (argb shr GREEN_SHIFT) and BYTE

private fun blue(argb: Int) = argb and BYTE

private fun channel(argb: Int, index: Int) =
  when (index) {
    0 -> red(argb)
    1 -> green(argb)
    else -> blue(argb)
  }

internal fun parseElevationIndex(json: String): List<ElevationTile> =
  elevationTiles(
    Json.parseToJsonElement(json).jsonObject.getValue("f").jsonArray.map { row ->
      row.jsonArray.map { it.jsonPrimitive.content.toDouble() }
    }
  )

private const val ROOT = "files/place"
private const val LAND_COLORS = "land_fill.webp"
private const val SIMPLE_COAST = "coast_low.dat"
private const val DETAILED_COAST = "coast_high.dat"
private const val TIDE_INDEX = "tides/tide-indices-1-2.webp"
private const val PER_FILE = 3
private const val MONTHS = 12
private const val KM_PER_DEGREE = 111.2
private const val RED_SHIFT = 16
private const val GREEN_SHIFT = 8
private const val BYTE = 0xFF
