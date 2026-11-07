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
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
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
import kotlin.math.PI
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sin
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import unitto.core.common.generated.resources.Res
import unitto.core.common.generated.resources.electric_capacitance
import unitto.core.common.generated.resources.electric_inductance
import unitto.core.common.generated.resources.electric_resistance
import unitto.core.common.generated.resources.filters_band_pass
import unitto.core.common.generated.resources.filters_band_stop
import unitto.core.common.generated.resources.filters_bandwidth
import unitto.core.common.generated.resources.filters_corner
import unitto.core.common.generated.resources.filters_explain_corner
import unitto.core.common.generated.resources.filters_explain_corner_text
import unitto.core.common.generated.resources.filters_explain_gain
import unitto.core.common.generated.resources.filters_explain_gain_text
import unitto.core.common.generated.resources.filters_explain_resonance
import unitto.core.common.generated.resources.filters_explain_resonance_text
import unitto.core.common.generated.resources.filters_gain
import unitto.core.common.generated.resources.filters_high_pass
import unitto.core.common.generated.resources.filters_hint
import unitto.core.common.generated.resources.filters_low_pass
import unitto.core.common.generated.resources.filters_middle
import unitto.core.common.generated.resources.filters_phase
import unitto.core.common.generated.resources.filters_order
import unitto.core.common.generated.resources.filters_quality
import unitto.core.common.generated.resources.filters_test
import unitto.core.common.generated.resources.filters_title
import unitto.core.common.generated.resources.projectile_explain_title
import unitto.core.common.generated.resources.projectile_initial

private val RESISTANCE = Condition("R", "Ω", Res.string.electric_resistance, 1.0..10000.0, 1.0)
private val CAPACITANCE = Condition("C", "µF", Res.string.electric_capacitance, 0.0..10.0, 0.001)
private val INDUCTANCE = Condition("L", "mH", Res.string.electric_inductance, 0.0..1000.0, 0.1)

private val FilterKind.title: StringResource
  get() =
    when (this) {
      FilterKind.LOW_PASS -> Res.string.filters_low_pass
      FilterKind.HIGH_PASS -> Res.string.filters_high_pass
      FilterKind.BAND_PASS -> Res.string.filters_band_pass
      FilterKind.BAND_STOP -> Res.string.filters_band_stop
    }

/** Share of the width of the charts ==> hertz, three decades on each side of the corner. */
private fun Filter.frequencyAt(share: Double) = cornerFrequency * 10.0.pow((share * 2 - 1) * DECADES)

private fun Filter.shareOf(frequency: Double) = (log10(frequency / cornerFrequency) / DECADES + 1) / 2

/** What a filter lets through, frequency by frequency, with a signal to try it on. */
@Composable
internal fun FiltersRoute(openDrawer: () -> Unit) {
  val fractional = rememberFractional()
  val format = numberFormat(fractional)
  var kind by rememberSaveable { mutableStateOf(FilterKind.LOW_PASS) }
  var chosenBuild by rememberSaveable { mutableStateOf(FilterBuild.RC) }
  val build = if (chosenBuild in kind.builds) chosenBuild else kind.builds.first()
  var resistance by rememberSaveable { mutableStateOf("1000") }
  var capacitance by rememberSaveable { mutableStateOf("0.1") }
  var inductance by rememberSaveable { mutableStateOf("10") }
  var test by rememberSaveable { mutableStateOf("1000") }
  val filter = Filter(kind, build, resistance.number(), capacitance.number() / MICRO, inductance.number() / MILLI)
  val frequency = test.number()
  val isValid = filter.isValid && frequency > 0

  PhysicsScreen(
    title = Res.string.filters_title,
    openDrawer = openDrawer,
    live = {
      ModeChips(FilterKind.entries, kind, { it.title }) { kind = it }
      if (kind.builds.size > 1) NameChips(kind.builds, build, { it.label }) { chosenBuild = it }
      if (isValid) {
        FilterCard(filter, frequency, format) { test = it.rounded(if (it < FINE_BELOW) 2 else 0).replace(".", fractional) }
      } else {
        InvalidValues()
      }
    },
    settings = {
      Card(stringResource(Res.string.projectile_initial)) {
        ConditionRow(RESISTANCE, resistance, fractional) { resistance = it }
        if (build.usesCapacitor) ConditionRow(CAPACITANCE, capacitance, fractional) { capacitance = it }
        if (build.usesCoil) ConditionRow(INDUCTANCE, inductance, fractional) { inductance = it }
        val top = if (filter.isValid) max(filter.cornerFrequency * 10, 1.0) else 1.0
        ConditionRow(Condition("f", "Hz", Res.string.filters_test, 0.0..top, top / SLIDER_STEPS), test, fractional) { test = it }
      }
    },
    explanation = { if (isValid) FilterExplanation(filter, frequency, format) },
  )
}

@Composable
private fun FilterCard(filter: Filter, frequency: Double, format: (Double) -> String, onFrequency: (Double) -> Unit) {
  val clock = rememberClock()
  val onChange by rememberUpdatedState(onFrequency)
  val line = MaterialTheme.colorScheme.primary
  val mark = MaterialTheme.colorScheme.tertiary
  val grid = MaterialTheme.colorScheme.outlineVariant
  val ghost = MaterialTheme.colorScheme.outline
  val share = filter.shareOf(frequency).coerceIn(0.0, 1.0)
  val touch =
    Modifier.pointerInput(filter.cornerFrequency) {
      detectTapGestures { onChange(filter.frequencyAt((it.x / size.width).toDouble().coerceIn(0.0, 1.0))) }
    }
      .pointerInput(filter.cornerFrequency) {
        detectHorizontalDragGestures { change, _ ->
          change.consume()
          onChange(filter.frequencyAt((change.position.x / size.width).toDouble().coerceIn(0.0, 1.0)))
        }
      }
  LiveCard {
    FilterCircuit(filter, format)
    Canvas(Modifier.fillMaxWidth().height(GAIN_HEIGHT).then(touch)) { drawGain(filter, share, line, mark, grid) }
    Canvas(Modifier.fillMaxWidth().height(PHASE_HEIGHT).then(touch)) { drawPhase(filter, share, line, mark, grid) }
    Hint(stringResource(Res.string.filters_hint))
    Canvas(Modifier.fillMaxWidth().height(SIGNAL_HEIGHT)) {
      val turn = clock.value * 2 * PI * SHOWN_TURNS_PER_SECOND
      drawSignal(turn, 1.0, 0.0, ghost, THIN_LINE)
      drawSignal(turn, filter.gain(frequency).coerceAtMost(1.0), filter.phase(frequency) * PI / HALF_TURN, line, LINE)
    }
    Answers(
      (if (filter.kind.isBand) Res.string.filters_middle else Res.string.filters_corner) to
        engineering(filter.cornerFrequency, "Hz", format),
      Res.string.filters_gain to "${format(filter.decibels(frequency))} dB",
      Res.string.filters_phase to "${format(filter.phase(frequency))}°",
    )
    AnswerList(
      listOfNotNull(
          Res.string.filters_order to filter.build.order.toString(),
          filter.quality?.let { Res.string.filters_quality to format(it) },
          (Res.string.filters_bandwidth to engineering(filter.bandwidth, "Hz", format)).takeIf { filter.kind.isBand },
      )
    )
  }
}

private fun DrawScope.drawDecades(share: Double, mark: Color, grid: Color) {
  val parts = 2 * DECADES.toInt()
  repeat(parts + 1) { index ->
    val x = size.width * index / parts
    drawLine(grid, Offset(x, 0f), Offset(x, size.height))
  }
  val x = (share * size.width).toFloat()
  drawLine(mark, Offset(x, 0f), Offset(x, size.height), strokeWidth = LINE.dp.toPx())
}

/** From 6 dB down to −60 dB. */
private fun DrawScope.drawGain(filter: Filter, share: Double, line: Color, mark: Color, grid: Color) {
  fun yOf(decibels: Double) = ((TOP_DECIBELS - decibels) / (TOP_DECIBELS - BOTTOM_DECIBELS) * size.height).toFloat()
  drawDecades(share, mark, grid)
  val dashes = PathEffect.dashPathEffect(floatArrayOf(DASH.dp.toPx(), DASH.dp.toPx()))
  drawLine(grid, Offset(0f, yOf(0.0)), Offset(size.width, yOf(0.0)))
  drawLine(mark, Offset(0f, yOf(CORNER_DECIBELS)), Offset(size.width, yOf(CORNER_DECIBELS)), pathEffect = dashes)
  clipRect {
    drawLineGraph(line, LINE, SAMPLES) { yOf(filter.decibels(filter.frequencyAt(it)).coerceAtLeast(LOWEST_DECIBELS)) }
    drawCircle(mark, MARK_RADIUS.toPx(), Offset((share * size.width).toFloat(), yOf(filter.decibels(filter.frequencyAt(share)).coerceAtLeast(LOWEST_DECIBELS))))
  }
}

/** From 180° down to −180°. */
private fun DrawScope.drawPhase(filter: Filter, share: Double, line: Color, mark: Color, grid: Color) {
  fun yOf(degrees: Double) = ((HALF_TURN - degrees) / (2 * HALF_TURN) * size.height).toFloat()
  drawDecades(share, mark, grid)
  drawLine(grid, Offset(0f, yOf(0.0)), Offset(size.width, yOf(0.0)))
  drawLineGraph(line, LINE, SAMPLES) { yOf(filter.phase(filter.frequencyAt(it))) }
}

private fun DrawScope.drawSignal(turn: Double, amplitude: Double, shift: Double, color: Color, width: Float) {
  val middle = size.height / 2
  drawLineGraph(color, width, SAMPLES) { share ->
    middle - (amplitude * middle * SIGNAL_REACH * sin(share * SIGNAL_TURNS * 2 * PI - turn + shift)).toFloat()
  }
}

@Composable
private fun FilterExplanation(filter: Filter, frequency: Double, format: (Double) -> String) {
  val corner = format(filter.cornerFrequency)
  Card(stringResource(Res.string.projectile_explain_title)) {
    Step(
      Res.string.filters_explain_corner,
      Res.string.filters_explain_corner_text,
      when (filter.build) {
        FilterBuild.RC -> "f(c) = 1 / (2π · R · C) = $corner Hz"
        FilterBuild.RL -> "f(c) = R / (2π · L) = $corner Hz"
        FilterBuild.TWO_RC -> "f₀ = 1 / (2π · R · C) = $corner Hz"
        FilterBuild.RLC -> "f₀ = 1 / (2π · √(L · C)) = $corner Hz"
      },
    )
    Step(
      Res.string.filters_explain_gain,
      Res.string.filters_explain_gain_text,
      "|H| = ${format(filter.gain(frequency))}",
      "G = 20 · log₁₀|H| = ${format(filter.decibels(frequency))} dB",
    )
    filter.quality?.let { quality ->
      Step(
        Res.string.filters_explain_resonance,
        Res.string.filters_explain_resonance_text,
        if (filter.build == FilterBuild.RLC) "Q = √(L / C) / R = ${format(quality)}" else "Q = 1 / 3",
        if (filter.kind.isBand) "Δf = f₀ / Q = ${format(filter.bandwidth)} Hz" else "H(f₀) = Q = ${format(quality)}",
      )
    }
  }
}

private const val MICRO = 1e6
private const val FINE_BELOW = 10
private const val MILLI = 1e3
private const val DECADES = 3.0
private const val HALF_TURN = 180.0
private const val TOP_DECIBELS = 6.0
private const val BOTTOM_DECIBELS = -60.0
private const val LOWEST_DECIBELS = -80.0
private const val CORNER_DECIBELS = -3.0
private const val SHOWN_TURNS_PER_SECOND = 0.5
private const val SIGNAL_TURNS = 3
private const val SIGNAL_REACH = 0.9
private const val SAMPLES = 180
private const val SLIDER_STEPS = 200
private const val LINE = 3f
private const val THIN_LINE = 1.5f
private const val DASH = 4
private val GAIN_HEIGHT = 170.dp
private val PHASE_HEIGHT = 90.dp
private val SIGNAL_HEIGHT = 70.dp
private val MARK_RADIUS = 6.dp
