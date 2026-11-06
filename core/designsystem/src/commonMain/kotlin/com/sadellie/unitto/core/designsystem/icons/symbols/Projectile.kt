package com.sadellie.unitto.core.designsystem.icons.symbols

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/** A ball at the top of its arc, for the Projectile app. */
val Symbols.Projectile: ImageVector
  get() {
    if (_Projectile != null) {
      return _Projectile!!
    }
    _Projectile = ImageVector.Builder(
      name = "Projectile",
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
        // The ground
        moveTo(120f, 800f)
        lineTo(840f, 800f)
      }
      path(
        stroke = SolidColor(Color(0xFFE8EAED)),
        strokeLineWidth = 64f,
        strokeLineCap = StrokeCap.Round,
      ) {
        // The arc
        moveTo(160f, 680f)
        quadTo(480f, -40f, 800f, 680f)
      }
      path(fill = SolidColor(Color(0xFFE8EAED))) {
        // The ball
        moveTo(390f, 320f)
        arcTo(90f, 90f, 0f, isMoreThanHalf = true, isPositiveArc = true, x1 = 570f, y1 = 320f)
        arcTo(90f, 90f, 0f, isMoreThanHalf = true, isPositiveArc = true, x1 = 390f, y1 = 320f)
        close()
      }
    }.build()

    return _Projectile!!
  }

@Suppress("ObjectPropertyName")
private var _Projectile: ImageVector? = null
