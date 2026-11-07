package com.sadellie.unitto.core.designsystem.icons.symbols

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/** A ball at the top of its arc, for the VectorFields app. */
val Symbols.VectorFields: ImageVector
  get() {
    if (_VectorFields != null) {
      return _VectorFields!!
    }
    _VectorFields = ImageVector.Builder(
      name = "VectorFields",
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
        moveTo(160f, 800f)
        lineTo(400f, 560f)
      }
      path(
        stroke = SolidColor(Color(0xFFE8EAED)),
        strokeLineWidth = 64f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round,
      ) {
        moveTo(280f, 560f)
        lineTo(400f, 560f)
        lineTo(400f, 680f)
      }
      path(
        stroke = SolidColor(Color(0xFFE8EAED)),
        strokeLineWidth = 64f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round,
      ) {
        moveTo(560f, 400f)
        lineTo(800f, 160f)
      }
      path(
        stroke = SolidColor(Color(0xFFE8EAED)),
        strokeLineWidth = 64f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round,
      ) {
        moveTo(680f, 160f)
        lineTo(800f, 160f)
        lineTo(800f, 280f)
      }
      path(
        stroke = SolidColor(Color(0xFFE8EAED)),
        strokeLineWidth = 64f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round,
      ) {
        moveTo(160f, 400f)
        lineTo(400f, 160f)
      }
      path(
        stroke = SolidColor(Color(0xFFE8EAED)),
        strokeLineWidth = 64f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round,
      ) {
        moveTo(280f, 160f)
        lineTo(400f, 160f)
        lineTo(400f, 280f)
      }
      path(
        stroke = SolidColor(Color(0xFFE8EAED)),
        strokeLineWidth = 64f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round,
      ) {
        moveTo(560f, 800f)
        lineTo(800f, 560f)
      }
      path(
        stroke = SolidColor(Color(0xFFE8EAED)),
        strokeLineWidth = 64f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round,
      ) {
        moveTo(680f, 560f)
        lineTo(800f, 560f)
        lineTo(800f, 680f)
      }
    }.build()

    return _VectorFields!!
  }

@Suppress("ObjectPropertyName")
private var _VectorFields: ImageVector? = null
