package com.sadellie.unitto.core.designsystem.icons.symbols

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/** A ball at the top of its arc, for the Surfaces app. */
val Symbols.Surfaces: ImageVector
  get() {
    if (_Surfaces != null) {
      return _Surfaces!!
    }
    _Surfaces = ImageVector.Builder(
      name = "Surfaces",
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
        moveTo(160f, 640f)
        lineTo(300f, 520f)
        lineTo(420f, 320f)
        lineTo(540f, 320f)
        lineTo(660f, 520f)
        lineTo(800f, 640f)
      }
      path(
        stroke = SolidColor(Color(0xFFE8EAED)),
        strokeLineWidth = 64f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round,
      ) {
        moveTo(160f, 640f)
        lineTo(480f, 800f)
        lineTo(800f, 640f)
      }
      path(
        stroke = SolidColor(Color(0xFFE8EAED)),
        strokeLineWidth = 64f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round,
      ) {
        moveTo(300f, 520f)
        lineTo(480f, 600f)
        lineTo(660f, 520f)
      }
    }.build()

    return _Surfaces!!
  }

@Suppress("ObjectPropertyName")
private var _Surfaces: ImageVector? = null
