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
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sadellie.unitto.feature.calculator.physics.LiveCard
import com.sadellie.unitto.feature.calculator.physics.PhysicsScreen
import com.sadellie.unitto.feature.calculator.physics.number
import com.sadellie.unitto.feature.calculator.physics.numberFormat
import com.sadellie.unitto.feature.calculator.physics.rememberFractional
import com.sadellie.unitto.feature.calculator.probability.rounded
import com.sadellie.unitto.feature.calculator.projectile.Card
import com.sadellie.unitto.feature.calculator.projectile.Condition
import com.sadellie.unitto.feature.calculator.projectile.ConditionRow
import com.sadellie.unitto.feature.calculator.projectile.Step
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import unitto.core.common.generated.resources.Res
import unitto.core.common.generated.resources.logic_decibels
import unitto.core.common.generated.resources.logic_explain_db
import unitto.core.common.generated.resources.logic_explain_db_text
import unitto.core.common.generated.resources.logic_explain_gates
import unitto.core.common.generated.resources.logic_explain_gates_text
import unitto.core.common.generated.resources.logic_gain
import unitto.core.common.generated.resources.logic_gates
import unitto.core.common.generated.resources.logic_hint
import unitto.core.common.generated.resources.logic_milliwatts
import unitto.core.common.generated.resources.logic_power_ratio
import unitto.core.common.generated.resources.logic_title
import unitto.core.common.generated.resources.logic_truth
import unitto.core.common.generated.resources.logic_voltage_ratio
import unitto.core.common.generated.resources.projectile_explain_title

private enum class LogicMode(val title: StringResource) {
  GATES(Res.string.logic_gates),
  DECIBELS(Res.string.logic_decibels),
}

private val GAIN = Condition("G", "dB", Res.string.logic_gain, -60.0..60.0, 0.1)
private val POWER_RATIO = Condition("P₂/P₁", "", Res.string.logic_power_ratio, 0.0..1000.0, 0.1)
private val VOLTAGE_RATIO = Condition("V₂/V₁", "", Res.string.logic_voltage_ratio, 0.0..1000.0, 0.1)
private val MILLIWATTS_NOW = Condition("P", "mW", Res.string.logic_milliwatts, 0.0..1000.0, 0.1)

/** Fields that show the same gain in four ways: changing one changes the others. */
private enum class GainField(val condition: Condition) {
  DECIBELS(GAIN),
  POWER(POWER_RATIO),
  VOLTAGE(VOLTAGE_RATIO),
  MILLIWATTS(MILLIWATTS_NOW);

  fun toDecibels(value: Double) =
    when (this) {
      DECIBELS -> value
      POWER,
      MILLIWATTS -> powerDecibels(value)
      VOLTAGE -> voltageDecibels(value)
    }

  fun fromDecibels(decibels: Double) =
    when (this) {
      DECIBELS -> decibels
      POWER,
      MILLIWATTS -> powerRatio(decibels)
      VOLTAGE -> voltageRatio(decibels)
    }
}

private fun gainTexts(field: GainField, text: String, old: List<String>, decimals: (Double) -> String): List<String> {
  val decibels = field.toDecibels(text.number())
  return GainField.entries.map {
    when {
      it == field -> text
      decibels.isFinite() -> decimals(it.fromDecibels(decibels))
      else -> old[it.ordinal]
    }
  }
}

private val STARTING_GAIN = listOf("3", "2", "1.413", "2")

/** Gates that can be tried with a tap, and decibels for what comes after them on a real board. */
@Composable
internal fun LogicRoute(openDrawer: () -> Unit) {
  val fractional = rememberFractional()
  val format = numberFormat(fractional)
  var mode by rememberSaveable { mutableStateOf(LogicMode.GATES) }
  var gate by rememberSaveable { mutableStateOf(Gate.AND) }
  var a by rememberSaveable { mutableStateOf(true) }
  var b by rememberSaveable { mutableStateOf(false) }
  var gains by rememberSaveable { mutableStateOf(STARTING_GAIN) }
  val decimals = { value: Double -> value.rounded(GAIN_DECIMALS).replace(".", fractional) }

  PhysicsScreen(
    title = Res.string.logic_title,
    openDrawer = openDrawer,
    live = {
      ModeChips(LogicMode.entries, mode, { it.title }) { mode = it }
      if (mode == LogicMode.GATES) {
        GateChips(gate) { gate = it }
        GateCard(gate, a, b, { a = !a }, { b = !b })
      } else {
        Card(stringResource(Res.string.logic_decibels)) {
          GainField.entries.forEach { field ->
            ConditionRow(field.condition, gains[field.ordinal], fractional) { gains = gainTexts(field, it, gains, decimals) }
          }
        }
      }
    },
    settings = {
      if (mode == LogicMode.GATES) {
        TruthTable(gate, a, b) { newA, newB ->
          a = newA
          b = newB
        }
      }
    },
    explanation = {
      Card(stringResource(Res.string.projectile_explain_title)) {
        if (mode == LogicMode.GATES) {
          Step(Res.string.logic_explain_gates, Res.string.logic_explain_gates_text, gate.expression)
        } else {
          val gain = gains[GainField.DECIBELS.ordinal].number()
          val shown = if (gain.isFinite()) format(gain) else "—"
          Step(
            Res.string.logic_explain_db,
            Res.string.logic_explain_db_text,
            "G = 10 · log₁₀(P₂ / P₁) = 20 · log₁₀(V₂ / V₁)",
            "$shown dB = ${gains[GainField.POWER.ordinal]} × P = ${gains[GainField.VOLTAGE.ordinal]} × V",
            "$shown dBm = ${gains[GainField.MILLIWATTS.ordinal]} mW",
          )
        }
      }
    },
  )
}

/** The names of gates are the same in every language. */
@Composable
private fun GateChips(selected: Gate, onChoose: (Gate) -> Unit) {
  Row(
    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    Gate.entries.forEach {
      FilterChip(selected = it == selected, onClick = { onChoose(it) }, label = { Text(it.name) })
    }
  }
}

private class GateColors(val off: Color, val on: Color, val body: Color, val text: TextStyle, val label: TextStyle)

@Composable
private fun GateCard(gate: Gate, a: Boolean, b: Boolean, onA: () -> Unit, onB: () -> Unit) {
  val measurer = rememberTextMeasurer()
  val scheme = MaterialTheme.colorScheme
  val style = MaterialTheme.typography.titleMedium
  val colors = GateColors(scheme.outline, scheme.tertiary, scheme.primary, style.copy(color = scheme.onTertiary), style.copy(color = scheme.onSurface))
  val tapA by rememberUpdatedState(onA)
  val tapB by rememberUpdatedState(onB)
  val hasTwoInputs by rememberUpdatedState(gate.hasTwoInputs)
  LiveCard {
    Canvas(
      Modifier.fillMaxWidth().height(GATE_HEIGHT).pointerInput(Unit) {
        detectTapGestures { position ->
          if (position.x < size.width * TAP_SHARE) {
            if (!hasTwoInputs || position.y < size.height / 2) tapA() else tapB()
          }
        }
      }
    ) {
      drawGateScene(gate, a, b, colors, measurer)
    }
    Hint(stringResource(Res.string.logic_hint))
    Text(gate.expression, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, style = MaterialTheme.typography.titleLarge)
  }
}

private fun DrawScope.drawGateScene(gate: Gate, a: Boolean, b: Boolean, colors: GateColors, measurer: TextMeasurer) {
  val middle = size.height / 2
  val rise = if (gate.hasTwoInputs) size.height * INPUT_RISE else 0f
  val left = size.width * SWITCH_SHARE
  val bodyLeft = size.width * BODY_LEFT
  val bodyRight = size.width * BODY_RIGHT
  val output = gate.output(a, b)
  val width = WIRE.dp.toPx()
  fun wire(from: Offset, to: Offset, isOn: Boolean) = drawLine(if (isOn) colors.on else colors.off, from, to, strokeWidth = width)
  val inputs = if (gate.hasTwoInputs) listOf(Triple("A", a, middle - rise), Triple("B", b, middle + rise)) else listOf(Triple("A", a, middle))
  val wireEnd = if (gate in CURVED_BACK) bodyLeft + (bodyRight - bodyLeft) * CURVE_DEPTH else bodyLeft
  inputs.forEach { (_, isOn, y) -> wire(Offset(left, y), Offset(wireEnd, y), isOn) }
  wire(Offset(bodyRight, middle), Offset(size.width * LAMP_SHARE, middle), output)
  drawGateBody(gate, Offset(bodyLeft, middle - size.height * BODY_RISE), Size(bodyRight - bodyLeft, 2 * size.height * BODY_RISE), colors.body)
  inputs.forEach { (name, isOn, y) ->
    drawCircle(if (isOn) colors.on else colors.off, BUTTON.dp.toPx(), Offset(left, y))
    drawLabel(measurer, if (isOn) "1" else "0", colors.text, Offset(left, y))
    drawLabel(measurer, name, colors.label, Offset(left - BUTTON.dp.toPx() * LABEL_OUT, y))
  }
  val lamp = Offset(size.width * LAMP_SHARE, middle)
  if (output) drawCircle(colors.on.copy(alpha = GLOW), BUTTON.dp.toPx() * 2, lamp)
  drawCircle(if (output) colors.on else colors.off, BUTTON.dp.toPx(), lamp)
  drawLabel(measurer, if (output) "1" else "0", colors.text, lamp)
}

/** The usual shapes: a D, a shield, or a triangle, and a small circle when the output is inverted. */
private fun DrawScope.drawGateBody(gate: Gate, topLeft: Offset, area: Size, color: Color) {
  val stroke = Stroke(width = WIRE.dp.toPx())
  val bubble = if (gate.isInverted) BUBBLE.dp.toPx() else 0f
  val right = topLeft.x + area.width - 2 * bubble
  val bottom = topLeft.y + area.height
  val middle = topLeft.y + area.height / 2
  val path = Path()
  path.moveTo(topLeft.x, topLeft.y)
  when (gate) {
    Gate.NOT -> path.lineTo(right, middle)
    Gate.AND,
    Gate.NAND -> {
      val shoulder = topLeft.x + area.width * SHOULDER
      // The middle of this curve is 3/4 of the way to the handles, and it should reach right
      val handle = right + (right - shoulder) / THIRDS
      path.lineTo(shoulder, topLeft.y)
      path.cubicTo(handle, topLeft.y, handle, bottom, shoulder, bottom)
      path.lineTo(topLeft.x, bottom)
    }
    Gate.OR,
    Gate.NOR,
    Gate.XOR,
    Gate.XNOR -> {
      path.quadraticTo(topLeft.x + area.width * SHOULDER, topLeft.y, right, middle)
      path.quadraticTo(topLeft.x + area.width * SHOULDER, bottom, topLeft.x, bottom)
    }
  }
  if (gate in CURVED_BACK) {
    path.quadraticTo(topLeft.x + area.width * BACK_CURVE, middle, topLeft.x, topLeft.y)
  } else {
    if (gate == Gate.NOT) path.lineTo(topLeft.x, bottom)
    path.close()
  }
  drawPath(path, color, style = stroke)
  if (gate == Gate.XOR || gate == Gate.XNOR) {
    val back = Path()
    val x = topLeft.x - XOR_GAP.dp.toPx()
    back.moveTo(x, topLeft.y)
    back.quadraticTo(x + area.width * BACK_CURVE, middle, x, bottom)
    drawPath(back, color, style = stroke)
  }
  if (gate.isInverted) drawCircle(color, bubble, Offset(right + bubble, middle), style = stroke)
}

private val CURVED_BACK = setOf(Gate.OR, Gate.NOR, Gate.XOR, Gate.XNOR)

/** All the inputs and what comes out. The row of the switches is lit, and a tap flips them to that row. */
@Composable
private fun TruthTable(gate: Gate, a: Boolean, b: Boolean, onChoose: (Boolean, Boolean) -> Unit) {
  val scheme = MaterialTheme.colorScheme
  val rows =
    if (gate.hasTwoInputs) {
      listOf(false, true).flatMap { first -> listOf(false, true).map { first to it } }
    } else {
      listOf(false to false, true to false)
    }
  Card(stringResource(Res.string.logic_truth)) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
      val titles = if (gate.hasTwoInputs) listOf("A", "B", "Y") else listOf("A", "Y")
      titles.forEach {
        Text(it, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, style = MaterialTheme.typography.titleSmall, color = scheme.primary)
      }
    }
    rows.forEach { (first, second) ->
      val isCurrent = first == a && (!gate.hasTwoInputs || second == b)
      val cells = listOfNotNull(first, second.takeIf { gate.hasTwoInputs }, gate.output(first, second))
      Row(
        Modifier.fillMaxWidth()
          .clip(ROW_SHAPE)
          .background(if (isCurrent) scheme.secondaryContainer else Color.Transparent)
          .clickable { onChoose(first, second) }
          .padding(horizontal = 12.dp, vertical = 8.dp)
      ) {
        cells.forEach { Text(if (it) "1" else "0", modifier = Modifier.weight(1f), textAlign = TextAlign.Center) }
      }
    }
  }
}

private const val GAIN_DECIMALS = 3
private const val INPUT_RISE = 0.24f
private const val SWITCH_SHARE = 0.14f
private const val TAP_SHARE = 0.34f
private const val BODY_LEFT = 0.34f
private const val BODY_RIGHT = 0.68f
private const val BODY_RISE = 0.38f
private const val CURVE_DEPTH = 0.08f
private const val THIRDS = 3
private const val LAMP_SHARE = 0.88f
private const val LABEL_OUT = 2f
private const val GLOW = 0.25f
private const val SHOULDER = 0.45f
private const val BACK_CURVE = 0.2f
private const val WIRE = 3
private const val BUTTON = 16
private const val BUBBLE = 7
private const val XOR_GAP = 9
private val GATE_HEIGHT = 170.dp
private val ROW_SHAPE = RoundedCornerShape(10.dp)
