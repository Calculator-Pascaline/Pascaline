package com.sadellie.unitto.core.designsystem.icons.symbols

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/** A ball at the top of its arc, for the Integrals app. */
val Symbols.Integrals: ImageVector
  get() {
    if (_Integrals != null) {
      return _Integrals!!
    }
    _Integrals = ImageVector.Builder(
      name = "Integrals",
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
        moveTo(160f, 320f)
        lineTo(480f, 160f)
        lineTo(800f, 320f)
        lineTo(800f, 640f)
        lineTo(480f, 800f)
        lineTo(160f, 640f)
        close()
      }
      path(
        stroke = SolidColor(Color(0xFFE8EAED)),
        strokeLineWidth = 64f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round,
      ) {
        moveTo(160f, 320f)
        lineTo(480f, 480f)
        lineTo(800f, 320f)
      }
      path(
        stroke = SolidColor(Color(0xFFE8EAED)),
        strokeLineWidth = 64f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round,
      ) {
        moveTo(480f, 480f)
        lineTo(480f, 800f)
      }
    }.build()

    return _Integrals!!
  }

@Suppress("ObjectPropertyName")
private var _Integrals: ImageVector? = null
