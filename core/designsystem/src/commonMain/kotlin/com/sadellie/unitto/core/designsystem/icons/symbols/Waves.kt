package com.sadellie.unitto.core.designsystem.icons.symbols

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/** A ball at the top of its arc, for the Waves app. */
val Symbols.Waves: ImageVector
  get() {
    if (_Waves != null) {
      return _Waves!!
    }
    _Waves = ImageVector.Builder(
      name = "Waves",
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
        moveTo(100f, 480f)
        quadTo(195f, 120f, 290f, 480f)
        quadTo(385f, 840f, 480f, 480f)
        quadTo(575f, 120f, 670f, 480f)
        quadTo(765f, 840f, 860f, 480f)
      }
    }.build()

    return _Waves!!
  }

@Suppress("ObjectPropertyName")
private var _Waves: ImageVector? = null
