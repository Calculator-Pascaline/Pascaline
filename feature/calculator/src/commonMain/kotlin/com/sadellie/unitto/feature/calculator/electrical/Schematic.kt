/*
 * Pascaline is a calculator for Android
 * Copyright (c) 2026 Calculator-Pascaline
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.sadellie.unitto.feature.calculator.electrical

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp

/** Colors of a circuit drawing, and the card under it that hides the wire behind a part. */
internal class SchematicColors(val wire: Color, val part: Color, val accent: Color, val background: Color)

/** What a circuit drawing needs: where it is, its colors and how its labels are written. */
internal class Schematic(val scope: DrawScope, val colors: SchematicColors, val measurer: TextMeasurer, val label: TextStyle)

/** A canvas for a circuit drawing in the colors of the theme. */
@Composable
internal fun SchematicCanvas(draw: Schematic.() -> Unit) {
  val scheme = MaterialTheme.colorScheme
  val colors = SchematicColors(scheme.outline, scheme.primary, scheme.tertiary, scheme.surfaceContainer)
  val measurer = rememberTextMeasurer()
  val label = MaterialTheme.typography.labelMedium.copy(color = scheme.onSurface)
  Canvas(Modifier.fillMaxWidth().height(SCHEMATIC_HEIGHT)) { Schematic(this, colors, measurer, label).draw() }
}

/** The way a part goes along its wire, and the way across it. */
private fun along(isVertical: Boolean) = if (isVertical) Offset(0f, 1f) else Offset(1f, 0f)

private fun across(isVertical: Boolean) = if (isVertical) Offset(1f, 0f) else Offset(0f, 1f)

internal fun Schematic.wire(from: Offset, to: Offset, color: Color = colors.wire) =
  scope.drawLine(color, from, to, strokeWidth = WIRE.px(scope))

private fun Number.px(scope: DrawScope) = toFloat() * scope.density

/** Hides the wire where a part is drawn. */
private fun Schematic.erase(center: Offset, isVertical: Boolean, half: Float) {
  val way = along(isVertical) * half
  scope.drawLine(colors.background, center - way, center + way, strokeWidth = ERASE.px(scope))
}

internal fun Schematic.label(text: String, center: Offset) = scope.drawLabel(measurer, text, label, center)

internal fun Schematic.resistor(center: Offset, isVertical: Boolean) {
  val half = PART.px(scope) / 2
  erase(center, isVertical, half)
  val way = along(isVertical)
  val side = across(isVertical) * TOOTH.px(scope)
  val path = Path()
  val start = center - way * half
  path.moveTo(start.x, start.y)
  repeat(TEETH) { tooth ->
    val point = start + way * (2 * half * (tooth + HALF) / TEETH) + side * (if (tooth % 2 == 0) 1f else -1f)
    path.lineTo(point.x, point.y)
  }
  val end = center + way * half
  path.lineTo(end.x, end.y)
  scope.drawPath(path, colors.part, style = Stroke(width = PART_LINE.px(scope)))
}

/** Two plates across the wire. */
internal fun Schematic.capacitor(center: Offset, isVertical: Boolean) {
  val gap = GAP.px(scope)
  erase(center, isVertical, gap)
  val way = along(isVertical) * gap
  val side = across(isVertical) * PLATE.px(scope)
  listOf(center - way, center + way).forEach {
    scope.drawLine(colors.part, it - side, it + side, strokeWidth = PLATE_LINE.px(scope))
  }
}

/** Four loops of wire, up on a flat wire and to the right on a standing one. */
internal fun Schematic.inductor(center: Offset, isVertical: Boolean) {
  val half = PART.px(scope) / 2
  erase(center, isVertical, half)
  val loop = 2 * half / LOOPS
  val start = center - along(isVertical) * half
  repeat(LOOPS) { index ->
    val topLeft =
      if (isVertical) Offset(start.x - loop / 2, start.y + index * loop) else Offset(start.x + index * loop, start.y - loop / 2)
    scope.drawArc(
      colors.part,
      startAngle = if (isVertical) THREE_QUARTERS else HALF_TURN,
      sweepAngle = HALF_TURN,
      useCenter = false,
      topLeft = topLeft,
      size = Size(loop, loop),
      style = Stroke(width = PART_LINE.px(scope)),
    )
  }
}

/** The long plate is the + side, before [center] along the wire. */
internal fun Schematic.battery(center: Offset, isVertical: Boolean) {
  val gap = GAP.px(scope)
  erase(center, isVertical, gap)
  val way = along(isVertical) * gap
  val side = across(isVertical)
  scope.drawLine(colors.part, center - way - side * PLATE.px(scope), center - way + side * PLATE.px(scope), strokeWidth = PART_LINE.px(scope))
  scope.drawLine(colors.part, center + way - side * SHORT_PLATE.px(scope), center + way + side * SHORT_PLATE.px(scope), strokeWidth = PLATE_LINE.px(scope))
}

/** An open end of the circuit, where something can be connected. */
internal fun Schematic.terminal(at: Offset, color: Color = colors.wire) {
  scope.drawCircle(colors.background, TERMINAL.px(scope), at)
  scope.drawCircle(color, TERMINAL.px(scope), at, style = Stroke(width = WIRE.px(scope)))
}

/** A dot where wires join or a switch can touch. */
internal fun Schematic.joint(at: Offset, color: Color = colors.wire) = scope.drawCircle(color, JOINT.px(scope), at)

private const val WIRE = 2f
private const val ERASE = 8
private const val PART = 44
private const val TOOTH = 7
private const val TEETH = 6
private const val HALF = 0.5f
private const val PART_LINE = 2.5f
private const val GAP = 4
private const val PLATE = 13
private const val SHORT_PLATE = 7
private const val PLATE_LINE = 3.5f
private const val LOOPS = 4
private const val HALF_TURN = 180f
private const val THREE_QUARTERS = 270f
private const val TERMINAL = 5
private const val JOINT = 3.5f
private val SCHEMATIC_HEIGHT = 140.dp
