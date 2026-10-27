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

import androidx.compose.ui.geometry.Offset
import kotlin.test.Test
import kotlin.test.assertEquals

private val window = GraphWindow(xMin = -10.0, xMax = 10.0, yMin = -5.0, yMax = 5.0)
private val center = Offset(500f, 1000f)

private fun assertWindow(expected: GraphWindow, actual: GraphWindow) {
  assertEquals(expected.xMin, actual.xMin, 1e-6)
  assertEquals(expected.xMax, actual.xMax, 1e-6)
  assertEquals(expected.yMin, actual.yMin, 1e-6)
  assertEquals(expected.yMax, actual.yMax, 1e-6)
}

class GraphWindowTest {
  @Test
  fun zoomXOnly() =
    assertWindow(GraphWindow(-5.0, 5.0, -5.0, 5.0), window.transformed(center, Offset.Zero, 2f, 1f, 1000, 2000))

  @Test
  fun zoomYOnly() =
    assertWindow(GraphWindow(-10.0, 10.0, -2.5, 2.5), window.transformed(center, Offset.Zero, 1f, 2f, 1000, 2000))

  @Test
  fun zoomBoth() =
    assertWindow(GraphWindow(-5.0, 5.0, -2.5, 2.5), window.transformed(center, Offset.Zero, 2f, 2f, 1000, 2000))

  // Dragging right and down shows what is left and up
  @Test
  fun pan() =
    assertWindow(GraphWindow(-12.0, 8.0, -4.0, 6.0), window.transformed(center, Offset(100f, 200f), 1f, 1f, 1000, 2000))
}
