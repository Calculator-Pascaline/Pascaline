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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.sadellie.unitto.feature.calculator.physics.InvalidValues
import com.sadellie.unitto.feature.calculator.physics.LiveCard
import com.sadellie.unitto.feature.calculator.physics.PhysicsScreen
import com.sadellie.unitto.feature.calculator.physics.number
import com.sadellie.unitto.feature.calculator.physics.numberFormat
import com.sadellie.unitto.feature.calculator.physics.rememberClock
import com.sadellie.unitto.feature.calculator.physics.rememberFractional
import com.sadellie.unitto.feature.calculator.probability.rounded
import com.sadellie.unitto.feature.calculator.projectile.Card
import com.sadellie.unitto.feature.calculator.projectile.Condition
import com.sadellie.unitto.feature.calculator.projectile.ConditionRow
import com.sadellie.unitto.feature.calculator.projectile.Step
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import unitto.core.common.generated.resources.Res
import unitto.core.common.generated.resources.charging_capacitor
import unitto.core.common.generated.resources.charging_charge
import unitto.core.common.generated.resources.charging_current_then
import unitto.core.common.generated.resources.charging_discharge
import unitto.core.common.generated.resources.charging_duty
import unitto.core.common.generated.resources.charging_explain_curve
import unitto.core.common.generated.resources.charging_explain_curve_text
import unitto.core.common.generated.resources.charging_explain_inductor_text
import unitto.core.common.generated.resources.charging_explain_tau
import unitto.core.common.generated.resources.charging_explain_tau_text
import unitto.core.common.generated.resources.charging_explain_timer
import unitto.core.common.generated.resources.charging_explain_timer_text
import unitto.core.common.generated.resources.charging_first_resistor
import unitto.core.common.generated.resources.charging_full
import unitto.core.common.generated.resources.charging_high
import unitto.core.common.generated.resources.charging_inductor
import unitto.core.common.generated.resources.charging_low
import unitto.core.common.generated.resources.charging_second_resistor
import unitto.core.common.generated.resources.charging_time
import unitto.core.common.generated.resources.charging_time_constant
import unitto.core.common.generated.resources.charging_timer
import unitto.core.common.generated.resources.charging_title
import unitto.core.common.generated.resources.charging_voltage_then
import unitto.core.common.generated.resources.electric_capacitance
import unitto.core.common.generated.resources.electric_frequency
import unitto.core.common.generated.resources.electric_inductance
import unitto.core.common.generated.resources.electric_resistance
import unitto.core.common.generated.resources.electric_voltage
import unitto.core.common.generated.resources.projectile_explain_title
import unitto.core.common.generated.resources.projectile_initial

private enum class ChargePart(val title: StringResource) {
  CAPACITOR(Res.string.charging_capacitor),
  INDUCTOR(Res.string.charging_inductor),
  TIMER(Res.string.charging_timer),
}

private enum class ChargeDirection(val title: StringResource) {
  CHARGE(Res.string.charging_charge),
  DISCHARGE(Res.string.charging_discharge),
}

private val VOLTAGE = Condition("V", "V", Res.string.electric_voltage, 0.0..48.0, 0.1)
private val RESISTANCE = Condition("R", "kΩ", Res.string.electric_resistance, 0.0..100.0, 0.5)
private val COIL_RESISTANCE = Condition("R", "Ω", Res.string.electric_resistance, 0.0..1000.0, 1.0)
private val CAPACITANCE = Condition("C", "µF", Res.string.electric_capacitance, 0.0..1000.0, 1.0)
private val INDUCTANCE = Condition("L", "mH", Res.string.electric_inductance, 0.0..1000.0, 1.0)
private val FIRST = Condition("R₁", "kΩ", Res.string.charging_first_resistor, 0.0..100.0, 0.5)
private val SECOND = Condition("R₂", "kΩ", Res.string.charging_second_resistor, 0.0..100.0, 0.5)

/** Share of the way from empty to full, after [time] seconds. */
private fun shareAt(time: Double, timeConstant: Double, isCharging: Boolean) =
  chargedShare(time, timeConstant).let { if (isCharging) it else 1 - it }

/**
 * One curve of the tool: a capacitor holds a voltage and a coil holds a current.
 *
 * @property full Value when fully charged.
 * @property unit Of [full].
 */
private class ChargeCurve(val part: ChargePart, val full: Double, val unit: String, val timeConstant: Double, val isCharging: Boolean) {
  val isValid: Boolean
    get() = full > 0 && full.isFinite() && timeConstant > 0 && timeConstant.isFinite()

  fun valueAt(time: Double) = full * shareAt(time, timeConstant, isCharging)
}

private fun ChargeCurve.firstTime(fractional: String) =
  if (isValid) timeConstant.rounded(TIME_DECIMALS).replace(".", fractional) else "0"

/** A coil in [ohms] and millihenries on [voltage] volts. */
private fun coilCurve(voltage: Double, ohms: Double, millihenries: Double, isCharging: Boolean) =
  ChargeCurve(ChargePart.INDUCTOR, voltage / ohms, "A", millihenries / MILLI / ohms, isCharging)

/** How a capacitor or a coil fills and empties, and the 555 timer that is built on that. */
@Composable
internal fun ChargingRoute(openDrawer: () -> Unit) {
  val fractional = rememberFractional()
  val format = numberFormat(fractional)
  var part by rememberSaveable { mutableStateOf(ChargePart.CAPACITOR) }
  var direction by rememberSaveable { mutableStateOf(ChargeDirection.CHARGE) }
  var voltage by rememberSaveable { mutableStateOf("9") }
  var resistance by rememberSaveable { mutableStateOf("10") }
  var capacitance by rememberSaveable { mutableStateOf("100") }
  var coilResistance by rememberSaveable { mutableStateOf("100") }
  var inductance by rememberSaveable { mutableStateOf("500") }
  var first by rememberSaveable { mutableStateOf("10") }
  var second by rememberSaveable { mutableStateOf("47") }
  var time by rememberSaveable { mutableStateOf<String?>(null) }
  val isCharging = direction == ChargeDirection.CHARGE
  val curve =
    if (part == ChargePart.INDUCTOR) {
      coilCurve(voltage.number(), coilResistance.number(), inductance.number(), isCharging)
    } else {
      ChargeCurve(part, voltage.number(), "V", resistance.number() * KILO * capacitance.number() / MICRO, isCharging)
    }
  val timer = Timer555(first.number() * KILO, second.number() * KILO, capacitance.number() / MICRO)
  // Starts at one time constant, which is a different number for every circuit
  val timeText = time ?: curve.firstTime(fractional)

  PhysicsScreen(
    title = Res.string.charging_title,
    openDrawer = openDrawer,
    live = {
      ModeChips(ChargePart.entries, part, { it.title }) {
        part = it
        time = null
      }
      if (part == ChargePart.TIMER) {
        TimerSection(timer, format)
      } else {
        ModeChips(ChargeDirection.entries, direction, { it.title }) { direction = it }
        CurveSection(curve, timeText.number(), format)
      }
    },
    settings = {
      Card(stringResource(Res.string.projectile_initial)) {
        if (part == ChargePart.TIMER) {
          ConditionRow(FIRST, first, fractional) { first = it }
          ConditionRow(SECOND, second, fractional) { second = it }
        } else {
          ConditionRow(VOLTAGE, voltage, fractional) { voltage = it }
        }
        if (part == ChargePart.CAPACITOR) ConditionRow(RESISTANCE, resistance, fractional) { resistance = it }
        if (part == ChargePart.INDUCTOR) {
          ConditionRow(COIL_RESISTANCE, coilResistance, fractional) { coilResistance = it }
          ConditionRow(INDUCTANCE, inductance, fractional) { inductance = it }
        } else {
          ConditionRow(CAPACITANCE, capacitance, fractional) { capacitance = it }
        }
        if (part != ChargePart.TIMER && curve.isValid) {
          val end = curve.timeConstant * SHOWN_CONSTANTS
          ConditionRow(Condition("t", "s", Res.string.charging_time, 0.0..end, end / SLIDER_STEPS), timeText, fractional) { time = it }
        }
      }
    },
    explanation = { ChargeExplanation(curve, timer, format) },
  )
}

@Composable
private fun TimerSection(timer: Timer555, format: (Double) -> String) {
  if (timer.isValid) TimerCard(timer, format) else InvalidValues()
}

@Composable
private fun CurveSection(curve: ChargeCurve, time: Double, format: (Double) -> String) {
  if (curve.isValid && time >= 0) CurveCard(curve, time, format) else InvalidValues()
}

@Composable
private fun ChargeExplanation(curve: ChargeCurve, timer: Timer555, format: (Double) -> String) {
  when {
    curve.part == ChargePart.TIMER -> if (timer.isValid) TimerExplanation(timer, format)
    curve.isValid -> CurveExplanation(curve, format)
  }
}

private class CurveColors(val line: Color, val dot: Color, val grid: Color, val mark: Color)

@Composable
private fun CurveCard(curve: ChargeCurve, time: Double, format: (Double) -> String) {
  val clock = rememberClock()
  val measurer = rememberTextMeasurer()
  val label = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
  val scheme = MaterialTheme.colorScheme
  val colors = CurveColors(scheme.primary, scheme.tertiary, scheme.outlineVariant, scheme.secondary)
  LiveCard {
    Canvas(Modifier.fillMaxWidth().height(CURVE_HEIGHT)) {
      drawChargeCurve(clock.value, curve.isCharging, colors, measurer, label)
      val share = (time / (curve.timeConstant * SHOWN_CONSTANTS)).coerceIn(0.0, 1.0).toFloat()
      drawLine(colors.mark, Offset(share * size.width, 0f), Offset(share * size.width, size.height), pathEffect = PathEffect.dashPathEffect(floatArrayOf(DASH, DASH)))
    }
    Answers(
      Res.string.charging_time_constant to engineering(curve.timeConstant, "s", format),
      Res.string.charging_full to engineering(curve.timeConstant * SHOWN_CONSTANTS, "s", format),
      (if (curve.part == ChargePart.INDUCTOR) Res.string.charging_current_then else Res.string.charging_voltage_then) to
        engineering(curve.valueAt(time), curve.unit, format),
    )
  }
}

private fun DrawScope.drawChargeCurve(seconds: Double, isCharging: Boolean, colors: CurveColors, measurer: TextMeasurer, label: TextStyle) {
  val bottom = size.height - LABEL_SPACE.dp.toPx()
  fun yOf(share: Double) = (bottom - share * bottom * CURVE_REACH).toFloat()
  repeat(SHOWN_CONSTANTS + 1) { index ->
    val x = size.width * index / SHOWN_CONSTANTS
    drawLine(colors.grid, Offset(x, 0f), Offset(x, bottom))
    if (index > 0) drawLabel(measurer, "${index}τ", label, Offset(x - LABEL_INSET.dp.toPx(), size.height - LABEL_SPACE.dp.toPx() / 2))
  }
  drawLine(colors.grid, Offset(0f, yOf(1.0)), Offset(size.width, yOf(1.0)))
  drawLine(colors.grid, Offset(0f, bottom), Offset(size.width, bottom))
  drawLineGraph(colors.line, LINE, SAMPLES) { yOf(shareAt(it * SHOWN_CONSTANTS, 1.0, isCharging)) }
  val progress = (seconds.mod(LOOP_SECONDS) / PLAY_SECONDS).coerceAtMost(1.0)
  val dot = Offset((progress * size.width).toFloat(), yOf(shareAt(progress * SHOWN_CONSTANTS, 1.0, isCharging)))
  drawCircle(colors.dot, DOT_RADIUS.dp.toPx(), dot)
}

@Composable
private fun TimerCard(timer: Timer555, format: (Double) -> String) {
  val clock = rememberClock()
  val capacitor = MaterialTheme.colorScheme.primary
  val output = MaterialTheme.colorScheme.tertiary
  val grid = MaterialTheme.colorScheme.outlineVariant
  LiveCard {
    Canvas(Modifier.fillMaxWidth().height(TIMER_HEIGHT)) {
      val start = clock.value * timer.period / SHOWN_PERIOD_SECONDS
      val height = size.height
      val top = height * OUTPUT_SHARE
      fun timeAt(share: Double) = start + (share - 1) * SHOWN_PERIODS * timer.period
      listOf(1.0 / THIRDS, 2.0 / THIRDS).forEach {
        val y = (height - (height - top) * it).toFloat()
        drawLine(grid, Offset(0f, y), Offset(size.width, y))
      }
      drawLineGraph(capacitor, LINE, TIMER_SAMPLES) { (height - (height - top) * timer.capacitorAt(timeAt(it))).toFloat() }
      drawLineGraph(output, LINE, TIMER_SAMPLES) { if (timer.isHigh(timeAt(it))) top * OUTPUT_HIGH else top * OUTPUT_LOW }
    }
    Answers(
      Res.string.electric_frequency to engineering(timer.frequency, "Hz", format),
      Res.string.charging_duty to "${format(timer.duty * PERCENT)}%",
    )
    Answers(
      Res.string.charging_high to engineering(timer.highTime, "s", format),
      Res.string.charging_low to engineering(timer.lowTime, "s", format),
    )
  }
}

@Composable
private fun CurveExplanation(curve: ChargeCurve, format: (Double) -> String) {
  val steps = (1..SHOWN_CONSTANTS).joinToString(" · ") { "${it}τ: ${format(chargedShare(it.toDouble(), 1.0) * PERCENT)}%" }
  val isCoil = curve.part == ChargePart.INDUCTOR
  val value = if (isCoil) "I(t)" else "V(t)"
  val full = if (isCoil) "V / R" else "V"
  Card(stringResource(Res.string.projectile_explain_title)) {
    Step(
      Res.string.charging_explain_tau,
      if (isCoil) Res.string.charging_explain_inductor_text else Res.string.charging_explain_tau_text,
      "${if (isCoil) "τ = L / R" else "τ = R · C"} = ${engineering(curve.timeConstant, "s", format)}",
    )
    Step(
      Res.string.charging_explain_curve,
      Res.string.charging_explain_curve_text,
      if (curve.isCharging) "$value = $full · (1 − e^(−t / τ))" else "$value = $full · e^(−t / τ)",
      "$full = ${engineering(curve.full, curve.unit, format)}",
      steps,
    )
  }
}

@Composable
private fun TimerExplanation(timer: Timer555, format: (Double) -> String) {
  Card(stringResource(Res.string.projectile_explain_title)) {
    Step(
      Res.string.charging_explain_timer,
      Res.string.charging_explain_timer_text,
      "t(high) = ln 2 · (R₁ + R₂) · C = ${engineering(timer.highTime, "s", format)}",
      "t(low) = ln 2 · R₂ · C = ${engineering(timer.lowTime, "s", format)}",
      "f = 1 / (t(high) + t(low)) = ${engineering(timer.frequency, "Hz", format)}",
    )
  }
}

private const val KILO = 1e3
private const val MILLI = 1e3
private const val TIME_DECIMALS = 6
private const val MICRO = 1e6
private const val PERCENT = 100
private const val SHOWN_CONSTANTS = 5
private const val SLIDER_STEPS = 200
private const val PLAY_SECONDS = 5.0
private const val LOOP_SECONDS = 6.5
private const val SHOWN_PERIODS = 3.0
private const val SHOWN_PERIOD_SECONDS = 2.0
private const val THIRDS = 3
private const val OUTPUT_SHARE = 0.34f
private const val OUTPUT_HIGH = 0.1f
private const val OUTPUT_LOW = 0.8f
private const val CURVE_REACH = 0.9
private const val SAMPLES = 120
private const val TIMER_SAMPLES = 360
private const val LINE = 3f
private const val DASH = 10f
private const val LABEL_SPACE = 18
private const val LABEL_INSET = 10
private const val DOT_RADIUS = 7
private val CURVE_HEIGHT = 200.dp
private val TIMER_HEIGHT = 200.dp
