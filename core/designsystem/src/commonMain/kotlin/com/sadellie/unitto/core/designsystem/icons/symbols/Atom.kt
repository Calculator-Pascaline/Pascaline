package com.sadellie.unitto.core.designsystem.icons.symbols

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/** Nucleus with three electron orbits, for the periodic table. */
val Symbols.Atom: ImageVector
  get() {
    if (_Atom != null) {
      return _Atom!!
    }
    _Atom = ImageVector.Builder(
      name = "Atom",
      defaultWidth = 24.dp,
      defaultHeight = 24.dp,
      viewportWidth = 960f,
      viewportHeight = 960f
    ).apply {
      group(rotate = 0f, pivotX = 480f, pivotY = 480f) {
        path(stroke = SolidColor(Color(0xFFE8EAED)), strokeLineWidth = 40f, strokeLineCap = StrokeCap.Round) {
          moveTo(100f, 480f)
          arcToRelative(380f, 150f, 0f, isMoreThanHalf = false, isPositiveArc = true, dx1 = 760f, dy1 = 0f)
          arcToRelative(380f, 150f, 0f, isMoreThanHalf = false, isPositiveArc = true, dx1 = -760f, dy1 = 0f)
          close()
        }
      }
      group(rotate = 60f, pivotX = 480f, pivotY = 480f) {
        path(stroke = SolidColor(Color(0xFFE8EAED)), strokeLineWidth = 40f, strokeLineCap = StrokeCap.Round) {
          moveTo(100f, 480f)
          arcToRelative(380f, 150f, 0f, isMoreThanHalf = false, isPositiveArc = true, dx1 = 760f, dy1 = 0f)
          arcToRelative(380f, 150f, 0f, isMoreThanHalf = false, isPositiveArc = true, dx1 = -760f, dy1 = 0f)
          close()
        }
      }
      group(rotate = 120f, pivotX = 480f, pivotY = 480f) {
        path(stroke = SolidColor(Color(0xFFE8EAED)), strokeLineWidth = 40f, strokeLineCap = StrokeCap.Round) {
          moveTo(100f, 480f)
          arcToRelative(380f, 150f, 0f, isMoreThanHalf = false, isPositiveArc = true, dx1 = 760f, dy1 = 0f)
          arcToRelative(380f, 150f, 0f, isMoreThanHalf = false, isPositiveArc = true, dx1 = -760f, dy1 = 0f)
          close()
        }
      }
      path(fill = SolidColor(Color(0xFFE8EAED))) {
        moveTo(410f, 480f)
        arcToRelative(70f, 70f, 0f, isMoreThanHalf = true, isPositiveArc = true, dx1 = 140f, dy1 = 0f)
        arcToRelative(70f, 70f, 0f, isMoreThanHalf = true, isPositiveArc = true, dx1 = -140f, dy1 = 0f)
        close()
      }
    }.build()

    return _Atom!!
  }

@Suppress("ObjectPropertyName")
private var _Atom: ImageVector? = null
