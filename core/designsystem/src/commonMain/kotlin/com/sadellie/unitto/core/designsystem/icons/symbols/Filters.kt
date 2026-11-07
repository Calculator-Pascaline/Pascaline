package com.sadellie.unitto.core.designsystem.icons.symbols

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/** A ball at the top of its arc, for the Filters app. */
val Symbols.Filters: ImageVector
  get() {
    if (_Filters != null) {
      return _Filters!!
    }
    _Filters = ImageVector.Builder(
      name = "Filters",
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
        moveTo(120f, 240f)
        lineTo(500f, 240f)
      }
      path(
        stroke = SolidColor(Color(0xFFE8EAED)),
        strokeLineWidth = 64f,
        strokeLineCap = StrokeCap.Round,
      ) {
        moveTo(500f, 240f)
        quadTo(620f, 240f, 840f, 760f)
      }
    }.build()

    return _Filters!!
  }

@Suppress("ObjectPropertyName")
private var _Filters: ImageVector? = null
