package com.sadellie.unitto.core.designsystem.icons.symbols

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/** Price tag with a hole, for comparing prices. */
val Symbols.PriceTag: ImageVector
  get() {
    if (_PriceTag != null) {
      return _PriceTag!!
    }
    _PriceTag = ImageVector.Builder(
      name = "PriceTag",
      defaultWidth = 24.dp,
      defaultHeight = 24.dp,
      viewportWidth = 960f,
      viewportHeight = 960f
    ).apply {
      path(fill = SolidColor(Color(0xFFE8EAED)), pathFillType = PathFillType.EvenOdd) {
        // Tag pointing to the bottom left
        moveTo(120f, 520f)
        lineTo(480f, 160f)
        lineTo(800f, 160f)
        lineTo(800f, 480f)
        lineTo(440f, 840f)
        close()
        // Hole
        moveTo(620f, 320f)
        arcToRelative(60f, 60f, 0f, isMoreThanHalf = true, isPositiveArc = true, dx1 = 120f, dy1 = 0f)
        arcToRelative(60f, 60f, 0f, isMoreThanHalf = true, isPositiveArc = true, dx1 = -120f, dy1 = 0f)
        close()
      }
    }.build()

    return _PriceTag!!
  }

@Suppress("ObjectPropertyName")
private var _PriceTag: ImageVector? = null
