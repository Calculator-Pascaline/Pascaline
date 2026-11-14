package io.github.calculatorpascaline.core.designsystem.icons.symbols

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** A big cogwheel meshing with a small one, for the Gears app. */
val Symbols.Gears: ImageVector
  get() {
    if (_Gears != null) {
      return _Gears!!
    }
    _Gears = ImageVector.Builder(
      name = "Gears",
      defaultWidth = 24.dp,
      defaultHeight = 24.dp,
      viewportWidth = 960f,
      viewportHeight = 960f
    ).apply {
      path(fill = SolidColor(Color(0xFFE8EAED)), pathFillType = PathFillType.EvenOdd) {
        cog(x = 370f, y = 590f, root = 230f, tip = 310f, hole = 95f, teeth = 9, turn = 0f)
        cog(x = 735f, y = 245f, root = 125f, tip = 185f, hole = 50f, teeth = 6, turn = 0.5f)
      }
    }.build()

    return _Gears!!
  }

/** Teeth around a circle and a round hole in the middle. [turn] is in teeth. */
private fun PathBuilder.cog(x: Float, y: Float, root: Float, tip: Float, hole: Float, teeth: Int, turn: Float) {
  val half = PI / teeth
  fun to(radius: Float, angle: Double, first: Boolean) {
    val px = x + radius * cos(angle).toFloat()
    val py = y + radius * sin(angle).toFloat()
    if (first) moveTo(px, py) else lineTo(px, py)
  }
  repeat(teeth) { tooth ->
    val middle = (tooth + turn) * 2 * half
    to(root, middle - 0.6 * half, tooth == 0)
    to(tip, middle - 0.4 * half, false)
    to(tip, middle + 0.4 * half, false)
    to(root, middle + 0.6 * half, false)
  }
  close()
  moveTo(x - hole, y)
  arcTo(hole, hole, 0f, isMoreThanHalf = true, isPositiveArc = true, x1 = x + hole, y1 = y)
  arcTo(hole, hole, 0f, isMoreThanHalf = true, isPositiveArc = true, x1 = x - hole, y1 = y)
  close()
}

@Suppress("ObjectPropertyName")
private var _Gears: ImageVector? = null
