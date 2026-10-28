package com.sadellie.unitto.core.designsystem.icons.symbols

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/** Axes with terms of a sequence going up, for the Sequences app. */
val Symbols.Sequence: ImageVector
  get() {
    if (_Sequence != null) {
      return _Sequence!!
    }
    _Sequence = ImageVector.Builder(
      name = "Sequence",
      defaultWidth = 24.dp,
      defaultHeight = 24.dp,
      viewportWidth = 960f,
      viewportHeight = 960f
    ).apply {
      path(stroke = SolidColor(Color(0xFFE8EAED)), strokeLineWidth = 48f, strokeLineCap = StrokeCap.Round) {
        // Axes
        moveTo(120f, 120f)
        lineTo(120f, 840f)
        lineTo(840f, 840f)
      }
      path(fill = SolidColor(Color(0xFFE8EAED))) {
        moveTo(224f, 680f)
        arcToRelative(56f, 56f, 0f, isMoreThanHalf = true, isPositiveArc = true, dx1 = 112f, dy1 = 0f)
        arcToRelative(56f, 56f, 0f, isMoreThanHalf = true, isPositiveArc = true, dx1 = -112f, dy1 = 0f)
        close()
        moveTo(384f, 560f)
        arcToRelative(56f, 56f, 0f, isMoreThanHalf = true, isPositiveArc = true, dx1 = 112f, dy1 = 0f)
        arcToRelative(56f, 56f, 0f, isMoreThanHalf = true, isPositiveArc = true, dx1 = -112f, dy1 = 0f)
        close()
        moveTo(544f, 400f)
        arcToRelative(56f, 56f, 0f, isMoreThanHalf = true, isPositiveArc = true, dx1 = 112f, dy1 = 0f)
        arcToRelative(56f, 56f, 0f, isMoreThanHalf = true, isPositiveArc = true, dx1 = -112f, dy1 = 0f)
        close()
        moveTo(704f, 200f)
        arcToRelative(56f, 56f, 0f, isMoreThanHalf = true, isPositiveArc = true, dx1 = 112f, dy1 = 0f)
        arcToRelative(56f, 56f, 0f, isMoreThanHalf = true, isPositiveArc = true, dx1 = -112f, dy1 = 0f)
        close()
      }
    }.build()

    return _Sequence!!
  }

@Suppress("ObjectPropertyName")
private var _Sequence: ImageVector? = null
