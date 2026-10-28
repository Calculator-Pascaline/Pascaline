package com.sadellie.unitto.core.designsystem.icons.symbols

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/** Points and the line that fits them, for the Regression app. */
val Symbols.ScatterPlot: ImageVector
  get() {
    if (_ScatterPlot != null) {
      return _ScatterPlot!!
    }
    _ScatterPlot = ImageVector.Builder(
      name = "ScatterPlot",
      defaultWidth = 24.dp,
      defaultHeight = 24.dp,
      viewportWidth = 960f,
      viewportHeight = 960f
    ).apply {
      path(stroke = SolidColor(Color(0xFFE8EAED)), strokeLineWidth = 44f, strokeLineCap = StrokeCap.Round) {
        moveTo(120f, 800f)
        lineTo(840f, 160f)
      }
      path(fill = SolidColor(Color(0xFFE8EAED))) {
        moveTo(160f, 600f)
        arcToRelative(60f, 60f, 0f, isMoreThanHalf = true, isPositiveArc = true, dx1 = 120f, dy1 = 0f)
        arcToRelative(60f, 60f, 0f, isMoreThanHalf = true, isPositiveArc = true, dx1 = -120f, dy1 = 0f)
        close()
        moveTo(360f, 560f)
        arcToRelative(60f, 60f, 0f, isMoreThanHalf = true, isPositiveArc = true, dx1 = 120f, dy1 = 0f)
        arcToRelative(60f, 60f, 0f, isMoreThanHalf = true, isPositiveArc = true, dx1 = -120f, dy1 = 0f)
        close()
        moveTo(500f, 300f)
        arcToRelative(60f, 60f, 0f, isMoreThanHalf = true, isPositiveArc = true, dx1 = 120f, dy1 = 0f)
        arcToRelative(60f, 60f, 0f, isMoreThanHalf = true, isPositiveArc = true, dx1 = -120f, dy1 = 0f)
        close()
        moveTo(700f, 340f)
        arcToRelative(60f, 60f, 0f, isMoreThanHalf = true, isPositiveArc = true, dx1 = 120f, dy1 = 0f)
        arcToRelative(60f, 60f, 0f, isMoreThanHalf = true, isPositiveArc = true, dx1 = -120f, dy1 = 0f)
        close()
      }
    }.build()

    return _ScatterPlot!!
  }

@Suppress("ObjectPropertyName")
private var _ScatterPlot: ImageVector? = null
