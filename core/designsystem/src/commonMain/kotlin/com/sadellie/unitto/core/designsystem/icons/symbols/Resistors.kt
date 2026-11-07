package com.sadellie.unitto.core.designsystem.icons.symbols

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/** A ball at the top of its arc, for the Resistors app. */
val Symbols.Resistors: ImageVector
  get() {
    if (_Resistors != null) {
      return _Resistors!!
    }
    _Resistors = ImageVector.Builder(
      name = "Resistors",
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
        moveTo(80f, 480f)
        lineTo(230f, 480f)
        lineTo(300f, 300f)
        lineTo(420f, 660f)
        lineTo(540f, 300f)
        lineTo(660f, 660f)
        lineTo(730f, 480f)
        lineTo(880f, 480f)
      }
    }.build()

    return _Resistors!!
  }

@Suppress("ObjectPropertyName")
private var _Resistors: ImageVector? = null
