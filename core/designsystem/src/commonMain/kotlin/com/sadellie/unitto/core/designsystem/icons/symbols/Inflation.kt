package com.sadellie.unitto.core.designsystem.icons.symbols

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/** A ball at the top of its arc, for the Inflation app. */
val Symbols.Inflation: ImageVector
  get() {
    if (_Inflation != null) {
      return _Inflation!!
    }
    _Inflation = ImageVector.Builder(
      name = "Inflation",
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
        moveTo(160f, 720f)
        lineTo(360f, 520f)
        lineTo(520f, 640f)
        lineTo(800f, 320f)
      }
      path(
        stroke = SolidColor(Color(0xFFE8EAED)),
        strokeLineWidth = 64f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round,
      ) {
        moveTo(600f, 320f)
        lineTo(800f, 320f)
        lineTo(800f, 520f)
      }
    }.build()

    return _Inflation!!
  }

@Suppress("ObjectPropertyName")
private var _Inflation: ImageVector? = null
