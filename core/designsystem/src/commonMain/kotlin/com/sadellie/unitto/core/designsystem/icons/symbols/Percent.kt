package com.sadellie.unitto.core.designsystem.icons.symbols

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/** Percent sign, for the Finance app. */
val Symbols.Percent: ImageVector
  get() {
    if (_Percent != null) {
      return _Percent!!
    }
    _Percent = ImageVector.Builder(
      name = "Percent",
      defaultWidth = 24.dp,
      defaultHeight = 24.dp,
      viewportWidth = 960f,
      viewportHeight = 960f
    ).apply {
      path(stroke = SolidColor(Color(0xFFE8EAED)), strokeLineWidth = 64f, strokeLineCap = StrokeCap.Round) {
        moveTo(760f, 200f)
        lineTo(200f, 760f)
      }
      path(stroke = SolidColor(Color(0xFFE8EAED)), strokeLineWidth = 56f, strokeLineCap = StrokeCap.Round) {
        moveTo(200f, 300f)
        arcToRelative(100f, 100f, 0f, isMoreThanHalf = true, isPositiveArc = true, dx1 = 200f, dy1 = 0f)
        arcToRelative(100f, 100f, 0f, isMoreThanHalf = true, isPositiveArc = true, dx1 = -200f, dy1 = 0f)
        close()
        moveTo(560f, 660f)
        arcToRelative(100f, 100f, 0f, isMoreThanHalf = true, isPositiveArc = true, dx1 = 200f, dy1 = 0f)
        arcToRelative(100f, 100f, 0f, isMoreThanHalf = true, isPositiveArc = true, dx1 = -200f, dy1 = 0f)
        close()
      }
    }.build()

    return _Percent!!
  }

@Suppress("ObjectPropertyName")
private var _Percent: ImageVector? = null
