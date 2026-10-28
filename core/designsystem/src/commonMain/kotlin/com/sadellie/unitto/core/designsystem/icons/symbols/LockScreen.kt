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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/** Padlock, for showing the app on the lock screen. */
val Symbols.LockScreen: ImageVector
  get() {
    if (_LockScreen != null) {
      return _LockScreen!!
    }
    _LockScreen = ImageVector.Builder(
      name = "LockScreen",
      defaultWidth = 24.dp,
      defaultHeight = 24.dp,
      viewportWidth = 960f,
      viewportHeight = 960f
    ).apply {
      path(stroke = SolidColor(Color(0xFFE8EAED)), strokeLineWidth = 64f, strokeLineCap = StrokeCap.Round) {
        // Shackle
        moveTo(320f, 440f)
        lineTo(320f, 300f)
        arcToRelative(160f, 160f, 0f, isMoreThanHalf = false, isPositiveArc = true, dx1 = 320f, dy1 = 0f)
        lineTo(640f, 440f)
        // Body
        moveTo(240f, 440f)
        lineTo(720f, 440f)
        lineTo(720f, 840f)
        lineTo(240f, 840f)
        close()
      }
      path(fill = SolidColor(Color(0xFFE8EAED))) {
        // Keyhole
        moveTo(440f, 640f)
        arcToRelative(40f, 40f, 0f, isMoreThanHalf = true, isPositiveArc = true, dx1 = 80f, dy1 = 0f)
        arcToRelative(40f, 40f, 0f, isMoreThanHalf = true, isPositiveArc = true, dx1 = -80f, dy1 = 0f)
        close()
      }
    }.build()

    return _LockScreen!!
  }

@Suppress("ObjectPropertyName")
private var _LockScreen: ImageVector? = null
