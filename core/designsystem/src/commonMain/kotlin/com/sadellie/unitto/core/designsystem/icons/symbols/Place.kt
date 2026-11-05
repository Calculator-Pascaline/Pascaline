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

package com.sadellie.unitto.core.designsystem.icons.symbols

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/** Map pin with a dot, for the Place app. */
val Symbols.Place: ImageVector
  get() {
    if (_Place != null) {
      return _Place!!
    }
    _Place = ImageVector.Builder(
      name = "Place",
      defaultWidth = 24.dp,
      defaultHeight = 24.dp,
      viewportWidth = 960f,
      viewportHeight = 960f
    ).apply {
      path(
        stroke = SolidColor(Color(0xFFE8EAED)),
        strokeLineWidth = 64f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round,
      ) {
        // The pin, a circle ending in a point
        moveTo(480f, 860f)
        curveTo(380f, 730f, 250f, 580f, 250f, 400f)
        arcTo(230f, 230f, 0f, isMoreThanHalf = false, isPositiveArc = true, x1 = 710f, y1 = 400f)
        curveTo(710f, 580f, 580f, 730f, 480f, 860f)
        close()
      }
      path(fill = SolidColor(Color(0xFFE8EAED))) {
        // The dot
        moveTo(400f, 400f)
        arcTo(80f, 80f, 0f, isMoreThanHalf = true, isPositiveArc = true, x1 = 560f, y1 = 400f)
        arcTo(80f, 80f, 0f, isMoreThanHalf = true, isPositiveArc = true, x1 = 400f, y1 = 400f)
        close()
      }
    }.build()

    return _Place!!
  }

@Suppress("ObjectPropertyName")
private var _Place: ImageVector? = null
