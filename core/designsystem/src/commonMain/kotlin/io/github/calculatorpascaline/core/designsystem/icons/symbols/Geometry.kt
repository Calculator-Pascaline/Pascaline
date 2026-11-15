package io.github.calculatorpascaline.core.designsystem.icons.symbols

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/** A ball at the top of its arc, for the Geometry app. */
val Symbols.Geometry: ImageVector
  get() {
    if (_Geometry != null) {
      return _Geometry!!
    }
    _Geometry = ImageVector.Builder(
      name = "Geometry",
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
        moveTo(480f, 160f)
        lineTo(840f, 800f)
        lineTo(120f, 800f)
        close()
      }
      path(
        stroke = SolidColor(Color(0xFFE8EAED)),
        strokeLineWidth = 64f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round,
      ) {
        moveTo(690f, 590f)
        lineTo(683f, 644f)
        lineTo(662f, 695f)
        lineTo(628f, 738f)
        lineTo(585f, 772f)
        lineTo(534f, 793f)
        lineTo(480f, 800f)
        lineTo(426f, 793f)
        lineTo(375f, 772f)
        lineTo(332f, 738f)
        lineTo(298f, 695f)
        lineTo(277f, 644f)
        lineTo(270f, 590f)
        lineTo(277f, 536f)
        lineTo(298f, 485f)
        lineTo(332f, 442f)
        lineTo(375f, 408f)
        lineTo(426f, 387f)
        lineTo(480f, 380f)
        lineTo(534f, 387f)
        lineTo(585f, 408f)
        lineTo(628f, 442f)
        lineTo(662f, 485f)
        lineTo(683f, 536f)
        lineTo(690f, 590f)
      }
    }.build()

    return _Geometry!!
  }

@Suppress("ObjectPropertyName")
private var _Geometry: ImageVector? = null
