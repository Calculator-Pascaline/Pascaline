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

package com.sadellie.unitto.feature.calculator.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sadellie.unitto.core.common.FormatterSymbols
import com.sadellie.unitto.core.common.OutputFormat
import com.sadellie.unitto.core.common.Token
import com.sadellie.unitto.core.ui.textfield.WrappedExpressionLayoutText
import io.github.sadellie.evaluatto.DiRow
import io.github.sadellie.evaluatto.DiStop
import io.github.sadellie.evaluatto.DiTable
import io.github.sadellie.evaluatto.Expression
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import unitto.core.common.generated.resources.Res
import unitto.core.common.generated.resources.calculator_di_how
import unitto.core.common.generated.resources.calculator_di_not_by_parts
import unitto.core.common.generated.resources.calculator_di_stop_integrable
import unitto.core.common.generated.resources.calculator_di_stop_integrable_text
import unitto.core.common.generated.resources.calculator_di_stop_repeats
import unitto.core.common.generated.resources.calculator_di_stop_repeats_text
import unitto.core.common.generated.resources.calculator_di_stop_zero
import unitto.core.common.generated.resources.calculator_di_stop_zero_text
import unitto.core.common.generated.resources.calculator_di_stops

/** Lines of the DI section in history: the stop, the setup, then one line per row. */
internal fun DiTable?.toMenuLines(): List<String> =
  if (this == null) {
    listOf(NO_TABLE)
  } else {
    listOf(stop.name, setup) + rows.map { listOf(it.sign, it.differentiated, it.integrated).joinToString(CELL) }
  }

/** The table saved by [toMenuLines], null when the integral isn't integrated by parts. */
internal fun diTableOf(lines: List<String>): DiTable? {
  val stop = DiStop.entries.firstOrNull { it.name == lines.firstOrNull() } ?: return null
  val setup = lines.getOrNull(1) ?: return null
  val rows =
    lines.drop(2).mapNotNull { line ->
      val cells = line.split(CELL)
      val sign = cells.firstOrNull()?.toIntOrNull()
      if (cells.size == CELLS && sign != null) DiRow(sign, cells[1], cells[2]) else null
    }
  return DiTable(rows, stop, setup)
}

/**
 * Integration by parts in the "…" menu of an integral: its DI table when it has one, then how the
 * method works and its three stops, each with an example. The stop of this integral is highlighted.
 */
@Composable
internal fun DiMethodMenu(lines: List<String>, formatterSymbols: FormatterSymbols, style: TextStyle, width: Dp) {
  val table = remember(lines) { diTableOf(lines) }
  val colors = MaterialTheme.colorScheme
  Column(modifier = Modifier.width(width), verticalArrangement = Arrangement.spacedBy(6.dp)) {
    if (table == null) {
      Text(stringResource(Res.string.calculator_di_not_by_parts), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
    } else {
      DiTableView(table, formatterSymbols, style)
      ExpressionLine(EQUALS + table.setup, formatterSymbols, style, colors.primary, EXPRESSION_WIDTH)
      Text(stringResource(table.stop.title()), style = MaterialTheme.typography.labelLarge, color = colors.primary)
    }
    Text(
      stringResource(Res.string.calculator_di_stops),
      modifier = Modifier.padding(top = 8.dp),
      style = MaterialTheme.typography.labelLarge,
      color = colors.primary,
    )
    Text(stringResource(Res.string.calculator_di_how), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
    DiStop.entries.forEach { stop -> StopExample(stop, isThisOne = stop == table?.stop, formatterSymbols, style) }
  }
}

/** One stop: what it is, how to finish, and an example table. */
@Composable
private fun StopExample(stop: DiStop, isThisOne: Boolean, formatterSymbols: FormatterSymbols, style: TextStyle) {
  val integral = EXAMPLES.getValue(stop)
  val example = remember(stop) { Expression(integral).diTable(EXAMPLE_PRECISION, OutputFormat.PLAIN) } ?: return
  val colors = MaterialTheme.colorScheme
  Column(
    modifier =
      Modifier.fillMaxWidth()
        .clip(CARD_SHAPE)
        .background(if (isThisOne) colors.primaryContainer else colors.surfaceContainerHighest)
        .padding(10.dp),
    verticalArrangement = Arrangement.spacedBy(4.dp),
  ) {
    Text(stringResource(stop.title()), style = MaterialTheme.typography.titleSmall)
    Text(stringResource(stop.rule()), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
    ExpressionLine(integral, formatterSymbols, style, colors.onSurface, CARD_EXPRESSION_WIDTH)
    DiTableView(example, formatterSymbols, style)
    ExpressionLine(EQUALS + example.setup, formatterSymbols, style, colors.primary, CARD_EXPRESSION_WIDTH)
  }
}

/**
 * Signs, D column and I column. Where it stops is highlighted: the 0, or the last row that is
 * multiplied across.
 */
@Composable
private fun DiTableView(table: DiTable, formatterSymbols: FormatterSymbols, style: TextStyle) {
  val colors = MaterialTheme.colorScheme
  val highlight = colors.tertiaryContainer
  val arrowColor = colors.primary
  // Where each D and I is drawn, to join them with arrows: row × 2 for D, + 1 for I
  val cells = remember(table) { mutableStateMapOf<Int, LayoutCoordinates>() }
  var tableCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
  Column(
    modifier =
      Modifier.onGloballyPositioned { tableCoordinates = it }
        .drawWithContent {
          drawContent()
          val parent = tableCoordinates ?: return@drawWithContent
          drawDiArrows(table, arrowColor) { key -> cells[key]?.takeIf { it.isAttached }?.let { parent.localBoundingBoxOf(it) } }
        },
    verticalArrangement = Arrangement.spacedBy(ROW_GAP),
  ) {
    Row(modifier = Modifier.fillMaxWidth()) {
      Text("", modifier = Modifier.width(SIGN_WIDTH))
      listOf("D", "I").forEach { header ->
        Text(
          header,
          modifier = Modifier.weight(1f),
          style = MaterialTheme.typography.labelLarge,
          color = colors.primary,
          textAlign = TextAlign.Center,
        )
      }
    }
    table.rows.forEachIndexed { index, row ->
      val isLast = index == table.rows.lastIndex
      val isAcross = isLast && table.stop != DiStop.ZERO
      Row(
        modifier = Modifier.fillMaxWidth().clip(CELL_SHAPE).background(if (isAcross) highlight else Color.Transparent),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          if (row.sign > 0) Token.Operator.PLUS else Token.Operator.MINUS,
          modifier = Modifier.width(SIGN_WIDTH),
          style = MaterialTheme.typography.titleMedium,
          color = colors.primary,
          textAlign = TextAlign.Center,
        )
        val isZero = isLast && table.stop == DiStop.ZERO
        Cell(
          row.differentiated,
          Modifier.weight(1f).clip(CELL_SHAPE).background(if (isZero) highlight else Color.Transparent),
          formatterSymbols,
          style,
        ) { cells[index * 2] = it }
        Cell(row.integrated, Modifier.weight(1f), formatterSymbols, style) { cells[index * 2 + 1] = it }
      }
    }
  }
}

/**
 * Each D to the I one row below it. The last row across is shown by its highlight, not an arrow.
 * [bounds] gives where a cell is: row × 2 for D, + 1 for I.
 */
private fun DrawScope.drawDiArrows(table: DiTable, color: Color, bounds: (Int) -> Rect?) {
  val gap = ARROW_GAP.toPx()
  for (row in 0 until table.rows.lastIndex) {
    val from = bounds(row * 2)
    val to = bounds((row + 1) * 2 + 1)
    if (from != null && to != null) {
      drawArrow(Offset(from.right + gap, from.center.y), Offset(to.left - gap, to.center.y), color)
    }
  }
}

/** A line with a head at [to]. */
private fun DrawScope.drawArrow(from: Offset, to: Offset, color: Color) {
  val width = ARROW_WIDTH.toPx()
  drawLine(color, from, to, width, StrokeCap.Round)
  val angle = atan2(to.y - from.y, to.x - from.x)
  val head = ARROW_HEAD.toPx()
  listOf(-1, 1).forEach { side ->
    val wing = angle + PI.toFloat() - side * ARROW_HEAD_ANGLE
    drawLine(color, to, Offset(to.x + head * cos(wing), to.y + head * sin(wing)), width, StrokeCap.Round)
  }
}

@Composable
private fun Cell(
  value: String,
  modifier: Modifier,
  formatterSymbols: FormatterSymbols,
  style: TextStyle,
  onPlaced: (LayoutCoordinates) -> Unit,
) {
  Row(modifier = modifier.padding(horizontal = 4.dp, vertical = 2.dp), horizontalArrangement = Arrangement.Center) {
    WrappedExpressionLayoutText(
      modifier = Modifier.onGloballyPositioned(onPlaced),
      value = value,
      formatterSymbols = formatterSymbols,
      style = style,
      textColor = MaterialTheme.colorScheme.onSurface,
      minScale = CELL_MIN_SCALE,
      alignEnd = false,
      maxWidth = CELL_WIDTH,
    )
  }
}

@Composable
private fun ExpressionLine(value: String, formatterSymbols: FormatterSymbols, style: TextStyle, color: Color, maxWidth: Dp) =
  WrappedExpressionLayoutText(
    value = value,
    formatterSymbols = formatterSymbols,
    style = style,
    textColor = color,
    minScale = CELL_MIN_SCALE,
    alignEnd = false,
    maxWidth = maxWidth,
  )

private fun DiStop.title(): StringResource =
  when (this) {
    DiStop.ZERO -> Res.string.calculator_di_stop_zero
    DiStop.INTEGRABLE -> Res.string.calculator_di_stop_integrable
    DiStop.REPEATS -> Res.string.calculator_di_stop_repeats
  }

private fun DiStop.rule(): StringResource =
  when (this) {
    DiStop.ZERO -> Res.string.calculator_di_stop_zero_text
    DiStop.INTEGRABLE -> Res.string.calculator_di_stop_integrable_text
    DiStop.REPEATS -> Res.string.calculator_di_stop_repeats_text
  }

private fun power(base: String, exponent: String) =
  base + Token.Operator.POWER + Token.Layout.SUPERSCRIPT_START + exponent + Token.Layout.END

/** One integral for each stop: x²eˣ reaches 0, x·ln(x) has a row to integrate, eˣ·sin(x) comes back. */
private val EXAMPLES: Map<DiStop, String> by lazy {
  val x = Token.Calculus.X
  val ex = power(Token.Const.E, x)
  fun integral(body: String) = Token.Calculus.INTEGRAL + body + Token.Calculus.DX
  mapOf(
    DiStop.ZERO to integral(power(x, "2") + ex),
    DiStop.INTEGRABLE to integral(x + Token.Func.LN_BRACKET + x + Token.Operator.RIGHT_BRACKET),
    DiStop.REPEATS to integral(ex + Token.Func.SIN_BRACKET + x + Token.Operator.RIGHT_BRACKET),
  )
}

private const val CELL = "\t"
private const val CELLS = 3
private const val EQUALS = "="
private const val NO_TABLE = "none"
private const val EXAMPLE_PRECISION = 3
private const val CELL_MIN_SCALE = 0.6f
private val SIGN_WIDTH = 24.dp

/** Room between rows for the arrows to slope. */
private val ROW_GAP = 8.dp
private val ARROW_GAP = 6.dp
private val ARROW_WIDTH = 1.5.dp
private val ARROW_HEAD = 6.dp
private const val ARROW_HEAD_ANGLE = 0.45f
private val CELL_WIDTH = 124.dp
private val EXPRESSION_WIDTH = 288.dp

/** Inside a card: the menu width less the padding of the card. */
private val CARD_EXPRESSION_WIDTH = 264.dp
private val CARD_SHAPE = RoundedCornerShape(12.dp)
private val CELL_SHAPE = RoundedCornerShape(6.dp)
