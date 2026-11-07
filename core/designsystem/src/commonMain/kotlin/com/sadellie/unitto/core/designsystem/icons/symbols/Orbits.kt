package com.sadellie.unitto.core.designsystem.icons.symbols

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/** A ball at the top of its arc, for the Orbits app. */
val Symbols.Orbits: ImageVector
  get() {
    if (_Orbits != null) {
      return _Orbits!!
    }
    _Orbits = ImageVector.Builder(
      name = "Orbits",
      defaultWidth = 24.dp,
      defaultHeight = 24.dp,
      viewportWidth = 960f,
      viewportHeight = 960f
    ).apply {
      path(fill = SolidColor(Color(0xFFE8EAED))) {
        moveTo(350f, 480f)
        arcTo(130f, 130f, 0f, isMoreThanHalf = true, isPositiveArc = true, x1 = 610f, y1 = 480f)
        arcTo(130f, 130f, 0f, isMoreThanHalf = true, isPositiveArc = true, x1 = 350f, y1 = 480f)
        close()
      }
      path(
        stroke = SolidColor(Color(0xFFE8EAED)),
        strokeLineWidth = 64f,
        strokeLineCap = StrokeCap.Round,
      ) {
        moveTo(140f, 480f)
        arcTo(340f, 340f, 0f, isMoreThanHalf = true, isPositiveArc = true, x1 = 820f, y1 = 480f)
        arcTo(340f, 340f, 0f, isMoreThanHalf = false, isPositiveArc = true, x1 = 720f, y1 = 720f)
      }
      path(fill = SolidColor(Color(0xFFE8EAED))) {
        moveTo(160f, 720f)
        arcTo(80f, 80f, 0f, isMoreThanHalf = true, isPositiveArc = true, x1 = 320f, y1 = 720f)
        arcTo(80f, 80f, 0f, isMoreThanHalf = true, isPositiveArc = true, x1 = 160f, y1 = 720f)
        close()
      }
    }.build()

    return _Orbits!!
  }

@Suppress("ObjectPropertyName")
private var _Orbits: ImageVector? = null
