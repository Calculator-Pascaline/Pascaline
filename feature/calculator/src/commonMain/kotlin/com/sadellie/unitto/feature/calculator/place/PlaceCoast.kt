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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import kotlin.math.ceil
import kotlin.math.floor

/**
 * Land shapes cut into 10° tiles, from scripts/make_coast.py. Points are in degrees from the north
 * west corner of the world, x to the east and y to the south.
 */
internal class CoastTiles(private val bytes: ByteArray) {
  /** Rings of a tile, each one as x, y, x, y… */
  fun rings(column: Int, row: Int): List<FloatArray> {
    val reader = VarintReader(bytes, offset(row * COLUMNS + column))
    val left = column * TILE_DEGREES
    val top = row * TILE_DEGREES
    return List(reader.next()) {
      val ring = FloatArray(reader.next() * 2)
      var x = 0
      var y = 0
      for (i in ring.indices step 2) {
        x += reader.nextSigned()
        y += reader.nextSigned()
        ring[i] = left + x / UNITS_PER_DEGREE
        ring[i + 1] = top + y / UNITS_PER_DEGREE
      }
      ring
    }
  }

  /** Little endian position of a tile in the table at the start of the file. */
  private fun offset(tile: Int): Int =
    (0 until OFFSET_BYTES).fold(0) { offset, i ->
      offset or ((bytes[tile * OFFSET_BYTES + i].toInt() and BYTE) shl (i * BITS_PER_BYTE))
    }
}

private class VarintReader(private val bytes: ByteArray, private var position: Int) {
  fun next(): Int {
    var value = 0
    var shift = 0
    while (true) {
      val byte = bytes[position++].toInt()
      value = value or ((byte and VARINT_BITS) shl shift)
      if (byte and VARINT_MORE == 0) return value
      shift += VARINT_SHIFT
    }
  }

  /** Zigzag: 0, −1, 1, −2 are stored as 0, 1, 2, 3. */
  fun nextSigned(): Int {
    val value = next()
    return (value ushr 1) xor -(value and 1)
  }
}

/** Tiles on the map. Columns go past the world's edges where the map wraps around. */
internal data class CoastArea(
  val detailed: Boolean,
  val firstColumn: Int,
  val lastColumn: Int,
  val firstRow: Int,
  val lastRow: Int,
)

/**
 * Tiles around the screen, for a map of [world] pixels wide with its corner at [left], [top]. Wide
 * views take half worlds at a time, so that panning doesn't rebuild their long path each frame.
 */
internal fun coastArea(left: Float, top: Float, width: Float, height: Float, world: Float): CoastArea {
  val tile = world / COLUMNS
  val detailed = world / WORLD_DEGREES >= DETAILED_PIXELS_PER_DEGREE
  val step = if (detailed) 1 else COLUMNS / 2
  val firstColumn = floor(-left / tile / step).toInt() * step
  val lastColumn = ceil((width - left) / tile / step).toInt() * step - 1
  val firstRow = if (detailed) floor(-top / tile).toInt().coerceAtLeast(0) else 0
  val lastRow = if (detailed) ceil((height - top) / tile).toInt().coerceAtMost(ROWS) - 1 else ROWS - 1
  return CoastArea(detailed, firstColumn, lastColumn, firstRow, lastRow)
}

/** Sharp land of the map: colors to show through its shapes, simple ones for wide views. */
internal class LandMap(val colors: ImageBitmap, private val simple: CoastTiles, private val detailed: CoastTiles) {
  private var area: CoastArea? = null
  private var path = Path()

  /**
   * One path for all of [area], in degrees. A path per tile or world would leave thin lines where
   * they meet. Only rebuilt when the tiles change, as this runs for each frame.
   */
  fun coast(area: CoastArea): Path {
    if (area != this.area) {
      this.area = area
      path = buildPath(if (area.detailed) detailed else simple, area)
    }
    return path
  }

  private fun buildPath(tiles: CoastTiles, area: CoastArea): Path {
    val path = Path().apply { fillType = PathFillType.EvenOdd }
    for (column in area.firstColumn..area.lastColumn) {
      val wrapped = column.mod(COLUMNS)
      val shift = (column - wrapped) * TILE_DEGREES
      for (row in area.firstRow..area.lastRow) {
        tiles.rings(wrapped, row).forEach { ring -> path.addRing(ring, shift) }
      }
    }
    return path
  }

  private fun Path.addRing(ring: FloatArray, shift: Float) {
    moveTo(ring[0] + shift, ring[1])
    for (i in 2 until ring.size step 2) lineTo(ring[i] + shift, ring[i + 1])
    close()
  }
}

private const val WORLD_DEGREES = 360f
private const val COLUMNS = 36
private const val ROWS = 18
private const val TILE_DEGREES = 10f
private const val UNITS_PER_DEGREE = 2000f
private const val OFFSET_BYTES = 4
private const val BITS_PER_BYTE = 8
private const val BYTE = 0xFF
private const val VARINT_BITS = 0x7F
private const val VARINT_MORE = 0x80
private const val VARINT_SHIFT = 7

/** The simple shapes are good for about 0.03°, a pixel at this scale. */
private const val DETAILED_PIXELS_PER_DEGREE = 30f
