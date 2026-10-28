package com.sadellie.unitto.core.designsystem.icons.symbols

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/** Box plot: whiskers, box and median, for the Statistics app. */
val Symbols.BoxPlot: ImageVector
  get() {
    if (_BoxPlot != null) {
      return _BoxPlot!!
    }
    _BoxPlot = ImageVector.Builder(
      name = "BoxPlot",
      defaultWidth = 24.dp,
      defaultHeight = 24.dp,
      viewportWidth = 960f,
      viewportHeight = 960f
    ).apply {
      path(stroke = SolidColor(Color(0xFFE8EAED)), strokeLineWidth = 48f, strokeLineCap = StrokeCap.Round) {
        // Whiskers with their ends
        moveTo(100f, 480f)
        lineTo(280f, 480f)
        moveTo(680f, 480f)
        lineTo(860f, 480f)
        moveTo(100f, 380f)
        lineTo(100f, 580f)
        moveTo(860f, 380f)
        lineTo(860f, 580f)
        // Box from the first to the third quartile
        moveTo(280f, 300f)
        lineTo(680f, 300f)
        lineTo(680f, 660f)
        lineTo(280f, 660f)
        close()
        // Median
        moveTo(440f, 300f)
        lineTo(440f, 660f)
      }
    }.build()

    return _BoxPlot!!
  }

@Suppress("ObjectPropertyName")
private var _BoxPlot: ImageVector? = null
