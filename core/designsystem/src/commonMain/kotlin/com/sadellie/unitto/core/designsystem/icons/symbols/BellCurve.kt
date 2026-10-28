package com.sadellie.unitto.core.designsystem.icons.symbols

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/** Bell curve with its right tail filled, for tests of the Inference app. */
val Symbols.BellCurve: ImageVector
  get() {
    if (_BellCurve != null) {
      return _BellCurve!!
    }
    _BellCurve = ImageVector.Builder(
      name = "BellCurve",
      defaultWidth = 24.dp,
      defaultHeight = 24.dp,
      viewportWidth = 960f,
      viewportHeight = 960f
    ).apply {
      path(stroke = SolidColor(Color(0xFFE8EAED)), strokeLineWidth = 44f, strokeLineCap = StrokeCap.Round) {
        // Axis and the curve
        moveTo(80f, 800f)
        lineTo(880f, 800f)
        moveTo(100f, 780f)
        curveTo(300f, 760f, 360f, 160f, 480f, 160f)
        curveTo(600f, 160f, 660f, 760f, 860f, 780f)
      }
      path(fill = SolidColor(Color(0xFFE8EAED))) {
        // Right tail, the p-value
        moveTo(660f, 800f)
        lineTo(660f, 600f)
        curveTo(720f, 720f, 780f, 770f, 860f, 780f)
        lineTo(860f, 800f)
        close()
      }
    }.build()

    return _BellCurve!!
  }

@Suppress("ObjectPropertyName")
private var _BellCurve: ImageVector? = null
