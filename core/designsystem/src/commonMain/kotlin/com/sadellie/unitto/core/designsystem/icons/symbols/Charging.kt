package com.sadellie.unitto.core.designsystem.icons.symbols

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/** A ball at the top of its arc, for the Charging app. */
val Symbols.Charging: ImageVector
  get() {
    if (_Charging != null) {
      return _Charging!!
    }
    _Charging = ImageVector.Builder(
      name = "Charging",
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
        moveTo(160f, 160f)
        lineTo(160f, 800f)
        lineTo(820f, 800f)
      }
      path(
        stroke = SolidColor(Color(0xFFE8EAED)),
        strokeLineWidth = 64f,
        strokeLineCap = StrokeCap.Round,
      ) {
        moveTo(160f, 800f)
        quadTo(300f, 240f, 800f, 220f)
      }
    }.build()

    return _Charging!!
  }

@Suppress("ObjectPropertyName")
private var _Charging: ImageVector? = null
