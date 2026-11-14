package io.github.calculatorpascaline.core.designsystem.icons.symbols

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val Symbols.DevicesOther: ImageVector
  get() {
    if (_DevicesOther != null) {
      return _DevicesOther!!
    }
    _DevicesOther = ImageVector.Builder(
      name = "DevicesOther",
      defaultWidth = 24.dp,
      defaultHeight = 24.dp,
      viewportWidth = 960f,
      viewportHeight = 960f,
    ).apply {
      path(fill = SolidColor(Color(0xFFE3E3E3))) {
        moveTo(400f, 380f)
        close()
        moveTo(160f, 800f)
        quadToRelative(-33f, 0f, -56.5f, -23.5f)
        reflectiveQuadTo(80f, 720f)
        verticalLineToRelative(-480f)
        quadToRelative(0f, -33f, 23.5f, -56.5f)
        reflectiveQuadTo(160f, 160f)
        horizontalLineToRelative(680f)
        quadToRelative(17f, 0f, 28.5f, 11.5f)
        reflectiveQuadTo(880f, 200f)
        quadToRelative(0f, 17f, -11.5f, 28.5f)
        reflectiveQuadTo(840f, 240f)
        lineTo(160f, 240f)
        verticalLineToRelative(480f)
        horizontalLineToRelative(40f)
        quadToRelative(17f, 0f, 28.5f, 11.5f)
        reflectiveQuadTo(240f, 760f)
        quadToRelative(0f, 17f, -11.5f, 28.5f)
        reflectiveQuadTo(200f, 800f)
        horizontalLineToRelative(-40f)
        close()
        moveTo(800f, 720f)
        verticalLineToRelative(-320f)
        lineTo(640f, 400f)
        verticalLineToRelative(320f)
        horizontalLineToRelative(160f)
        close()
        moveTo(620f, 800f)
        quadToRelative(-25f, 0f, -42.5f, -17.5f)
        reflectiveQuadTo(560f, 740f)
        verticalLineToRelative(-360f)
        quadToRelative(0f, -25f, 17.5f, -42.5f)
        reflectiveQuadTo(620f, 320f)
        horizontalLineToRelative(200f)
        quadToRelative(25f, 0f, 42.5f, 17.5f)
        reflectiveQuadTo(880f, 380f)
        verticalLineToRelative(360f)
        quadToRelative(0f, 25f, -17.5f, 42.5f)
        reflectiveQuadTo(820f, 800f)
        lineTo(620f, 800f)
        close()
        moveTo(720f, 500f)
        quadToRelative(13f, 0f, 21.5f, -9f)
        reflectiveQuadToRelative(8.5f, -21f)
        quadToRelative(0f, -13f, -8.5f, -21.5f)
        reflectiveQuadTo(720f, 440f)
        quadToRelative(-12f, 0f, -21f, 8.5f)
        reflectiveQuadToRelative(-9f, 21.5f)
        quadToRelative(0f, 12f, 9f, 21f)
        reflectiveQuadToRelative(21f, 9f)
        close()
        moveTo(332f, 771f)
        lineToRelative(-12f, -41f)
        quadToRelative(-19f, -17f, -29.5f, -40f)
        reflectiveQuadTo(280f, 640f)
        quadToRelative(0f, -27f, 10.5f, -50f)
        reflectiveQuadToRelative(29.5f, -40f)
        lineToRelative(12f, -41f)
        quadToRelative(4f, -13f, 14f, -21f)
        reflectiveQuadToRelative(24f, -8f)
        horizontalLineToRelative(60f)
        quadToRelative(14f, 0f, 24f, 8f)
        reflectiveQuadToRelative(14f, 21f)
        lineToRelative(12f, 41f)
        quadToRelative(19f, 17f, 29.5f, 40f)
        reflectiveQuadToRelative(10.5f, 50f)
        quadToRelative(0f, 27f, -10.5f, 50f)
        reflectiveQuadTo(480f, 730f)
        lineToRelative(-12f, 41f)
        quadToRelative(-4f, 13f, -14f, 21f)
        reflectiveQuadToRelative(-24f, 8f)
        horizontalLineToRelative(-60f)
        quadToRelative(-14f, 0f, -24f, -8f)
        reflectiveQuadToRelative(-14f, -21f)
        close()
        moveTo(400f, 700f)
        quadToRelative(26f, 0f, 43f, -17.5f)
        reflectiveQuadToRelative(17f, -42.5f)
        quadToRelative(0f, -25f, -18f, -42.5f)
        reflectiveQuadTo(400f, 580f)
        quadToRelative(-24f, 0f, -42f, 17f)
        reflectiveQuadToRelative(-18f, 43f)
        quadToRelative(0f, 26f, 17f, 43f)
        reflectiveQuadToRelative(43f, 17f)
        close()
        moveTo(720f, 560f)
        close()
      }
    }.build()

    return _DevicesOther!!
  }

@Suppress("ObjectPropertyName")
private var _DevicesOther: ImageVector? = null
