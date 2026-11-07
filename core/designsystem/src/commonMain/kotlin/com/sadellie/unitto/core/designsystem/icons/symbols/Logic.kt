package com.sadellie.unitto.core.designsystem.icons.symbols

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/** A ball at the top of its arc, for the Logic app. */
val Symbols.Logic: ImageVector
  get() {
    if (_Logic != null) {
      return _Logic!!
    }
    _Logic = ImageVector.Builder(
      name = "Logic",
      defaultWidth = 24.dp,
      defaultHeight = 24.dp,
      viewportWidth = 960f,
      viewportHeight = 960f
    ).apply {
      path(
        stroke = SolidColor(Color(0xFFE8EAED)),
        strokeLineWidth = 64f,
        strokeLineCap = StrokeCap.Round,
      ) {
        moveTo(340f, 200f)
        lineTo(500f, 200f)
        arcTo(280f, 280f, 0f, isMoreThanHalf = false, isPositiveArc = true, x1 = 500f, y1 = 760f)
        lineTo(340f, 760f)
        close()
      }
      path(
        stroke = SolidColor(Color(0xFFE8EAED)),
        strokeLineWidth = 64f,
        strokeLineCap = StrokeCap.Round,
      ) {
        moveTo(120f, 360f)
        lineTo(340f, 360f)
      }
      path(
        stroke = SolidColor(Color(0xFFE8EAED)),
        strokeLineWidth = 64f,
        strokeLineCap = StrokeCap.Round,
      ) {
        moveTo(120f, 600f)
        lineTo(340f, 600f)
      }
      path(
        stroke = SolidColor(Color(0xFFE8EAED)),
        strokeLineWidth = 64f,
        strokeLineCap = StrokeCap.Round,
      ) {
        moveTo(780f, 480f)
        lineTo(880f, 480f)
      }
    }.build()

    return _Logic!!
  }

@Suppress("ObjectPropertyName")
private var _Logic: ImageVector? = null
