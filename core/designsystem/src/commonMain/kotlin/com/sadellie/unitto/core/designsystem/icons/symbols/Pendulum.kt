package com.sadellie.unitto.core.designsystem.icons.symbols

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/** A ball at the top of its arc, for the Pendulum app. */
val Symbols.Pendulum: ImageVector
  get() {
    if (_Pendulum != null) {
      return _Pendulum!!
    }
    _Pendulum = ImageVector.Builder(
      name = "Pendulum",
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
        moveTo(240f, 160f)
        lineTo(720f, 160f)
      }
      path(
        stroke = SolidColor(Color(0xFFE8EAED)),
        strokeLineWidth = 64f,
        strokeLineCap = StrokeCap.Round,
      ) {
        moveTo(480f, 160f)
        lineTo(640f, 640f)
      }
      path(fill = SolidColor(Color(0xFFE8EAED))) {
        moveTo(540f, 700f)
        arcTo(120f, 120f, 0f, isMoreThanHalf = true, isPositiveArc = true, x1 = 780f, y1 = 700f)
        arcTo(120f, 120f, 0f, isMoreThanHalf = true, isPositiveArc = true, x1 = 540f, y1 = 700f)
        close()
      }
    }.build()

    return _Pendulum!!
  }

@Suppress("ObjectPropertyName")
private var _Pendulum: ImageVector? = null
