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

package com.sadellie.unitto.feature.calculator.physics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.unit.dp
import com.sadellie.unitto.feature.calculator.projectile.Answer
import com.sadellie.unitto.feature.calculator.projectile.Card
import com.sadellie.unitto.feature.calculator.projectile.Condition
import com.sadellie.unitto.feature.calculator.projectile.ConditionRow
import com.sadellie.unitto.feature.calculator.projectile.Step
import kotlin.math.floor
import kotlin.math.max
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import unitto.core.common.generated.resources.Res
import unitto.core.common.generated.resources.projectile_explain_title
import unitto.core.common.generated.resources.projectile_initial
import unitto.core.common.generated.resources.waves_amplitude
import unitto.core.common.generated.resources.waves_angular
import unitto.core.common.generated.resources.waves_beat
import unitto.core.common.generated.resources.waves_beats
import unitto.core.common.generated.resources.waves_explain_basics
import unitto.core.common.generated.resources.waves_explain_basics_text
import unitto.core.common.generated.resources.waves_explain_beats
import unitto.core.common.generated.resources.waves_explain_beats_text
import unitto.core.common.generated.resources.waves_explain_motion
import unitto.core.common.generated.resources.waves_explain_motion_text
import unitto.core.common.generated.resources.waves_explain_standing
import unitto.core.common.generated.resources.waves_explain_standing_text
import unitto.core.common.generated.resources.waves_frequency
import unitto.core.common.generated.resources.waves_nodes
import unitto.core.common.generated.resources.waves_period
import unitto.core.common.generated.resources.waves_second_frequency
import unitto.core.common.generated.resources.waves_speed
import unitto.core.common.generated.resources.waves_standing
import unitto.core.common.generated.resources.waves_title
import unitto.core.common.generated.resources.waves_travelling
import unitto.core.common.generated.resources.waves_wavelength

private val AMPLITUDE = Condition("A", "m", Res.string.waves_amplitude, 0.0..5.0, 0.1)
private val WAVELENGTH = Condition("λ", "m", Res.string.waves_wavelength, 0.5..10.0, 0.1)
private val FREQUENCY = Condition("f", "Hz", Res.string.waves_frequency, 0.1..20.0, 0.05)
private val SECOND_FREQUENCY = Condition("f₂", "Hz", Res.string.waves_second_frequency, 0.1..20.0, 0.05)

private val WaveMode.title: StringResource
  get() =
    when (this) {
      WaveMode.TRAVELLING -> Res.string.waves_travelling
      WaveMode.STANDING -> Res.string.waves_standing
      WaveMode.BEATS -> Res.string.waves_beats
    }

/** A string that shows what the numbers of a wave do, alone or with a second one. */
@Composable
internal fun WavesRoute(openDrawer: () -> Unit) {
  val fractional = rememberFractional()
  val format = numberFormat(fractional)
  var mode by rememberSaveable { mutableStateOf(WaveMode.TRAVELLING) }
  var amplitude by rememberSaveable { mutableStateOf("1") }
  var wavelength by rememberSaveable { mutableStateOf("3") }
  var frequency by rememberSaveable { mutableStateOf("1") }
  var second by rememberSaveable { mutableStateOf("1.25") }
  val wave = Wave(amplitude.number(), wavelength.number(), frequency.number())
  val isValid = wave.isValid && (mode != WaveMode.BEATS || second.number() > 0)
  val other = if (isValid && second.number() > 0) wave.atFrequency(second.number()) else wave

  PhysicsScreen(
    title = Res.string.waves_title,
    openDrawer = openDrawer,
    live = {
      ModeChips(mode) { mode = it }
      if (isValid) WaveCard(wave, other, mode, format) else InvalidValues()
    },
    settings = {
      Card(stringResource(Res.string.projectile_initial)) {
        ConditionRow(AMPLITUDE, amplitude, fractional) { amplitude = it }
        ConditionRow(WAVELENGTH, wavelength, fractional) { wavelength = it }
        ConditionRow(FREQUENCY, frequency, fractional) { frequency = it }
        if (mode == WaveMode.BEATS) ConditionRow(SECOND_FREQUENCY, second, fractional) { second = it }
      }
    },
    explanation = { if (isValid) WaveExplanation(wave, other, mode, format) },
  )
}

@Composable
private fun ModeChips(mode: WaveMode, onChoose: (WaveMode) -> Unit) {
  Row(
    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    WaveMode.entries.forEach {
      FilterChip(selected = it == mode, onClick = { onChoose(it) }, label = { Text(stringResource(it.title)) })
    }
  }
}

@Composable
private fun WaveCard(wave: Wave, other: Wave, mode: WaveMode, format: (Double) -> String) {
  val clock = rememberClock()
  val line = MaterialTheme.colorScheme.primary
  val ghost = MaterialTheme.colorScheme.outline
  val mark = MaterialTheme.colorScheme.tertiary
  LiveCard {
    Canvas(Modifier.fillMaxWidth().height(SCENE_HEIGHT)) {
      // Fast waves play slowly, or they would only be a blur
      val t = clock.value / max(1.0, wave.frequency)
      val top = max(LOWEST_TOP, wave.amplitude * TOP_PER_AMPLITUDE)
      clipRect {
        drawAxis(ghost)
        if (mode != WaveMode.TRAVELLING) drawComponents(wave, other, mode, t, top, ghost)
        drawString(line, top) { x -> wave.height(mode, other, x, t) }
        drawMarks(wave, other, mode, t, top, mark)
      }
    }
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      Answer(Modifier.weight(1f), Res.string.waves_speed, "${format(wave.speed)} m/s")
      Answer(Modifier.weight(1f), Res.string.waves_period, "${format(wave.period)} s")
      when (mode) {
        WaveMode.TRAVELLING -> Answer(Modifier.weight(1f), Res.string.waves_angular, "${format(wave.angularFrequency)} rad/s")
        WaveMode.STANDING -> Answer(Modifier.weight(1f), Res.string.waves_nodes, "${format(wave.wavelength / 2)} m")
        WaveMode.BEATS -> Answer(Modifier.weight(1f), Res.string.waves_beat, "${format(wave.beats(other))} Hz")
      }
    }
  }
}

private fun DrawScope.xOf(x: Double) = (x / WINDOW * size.width).toFloat()

/** Two waves added up fit under [top], the height in meters of the top of the screen. */
private fun DrawScope.yOf(height: Double, top: Double) = (size.height / 2 - height / top * size.height / 2).toFloat()

private fun DrawScope.drawAxis(color: Color) =
  drawLine(color, Offset(0f, size.height / 2), Offset(size.width, size.height / 2))

private fun DrawScope.drawString(color: Color, top: Double, width: Float = LINE_WIDTH, height: (Double) -> Double) {
  val path = Path()
  repeat(SAMPLES + 1) { index ->
    val x = index.toDouble() / SAMPLES * WINDOW
    val y = yOf(height(x), top)
    if (index == 0) path.moveTo(xOf(x), y) else path.lineTo(xOf(x), y)
  }
  drawPath(path, color, style = Stroke(width = width.dp.toPx()))
}

/** The waves that are added up. */
private fun DrawScope.drawComponents(wave: Wave, other: Wave, mode: WaveMode, t: Double, top: Double, color: Color) {
  drawString(color, top, GHOST_WIDTH) { x -> wave.right(x, t) }
  drawString(color, top, GHOST_WIDTH) { x -> if (mode == WaveMode.STANDING) wave.left(x, t) else other.right(x, t) }
}

/** A crest going along, the nodes that never move, or a point that goes up and down. */
private fun DrawScope.drawMarks(wave: Wave, other: Wave, mode: WaveMode, t: Double, top: Double, color: Color) {
  val radius = MARK_RADIUS.toPx()
  when (mode) {
    WaveMode.TRAVELLING -> {
      val span = max(wave.wavelength, floor(WINDOW / wave.wavelength) * wave.wavelength)
      val x = (wave.wavelength / QUARTER + wave.speed * t).mod(span)
      drawCircle(color, radius, Offset(xOf(x), yOf(wave.amplitude, top)))
    }
    WaveMode.STANDING ->
      generateSequence(0.0) { it + wave.wavelength / 2 }
        .takeWhile { it <= WINDOW }
        .forEach { drawCircle(color, radius, Offset(xOf(it), size.height / 2)) }
    WaveMode.BEATS -> {
      val x = WINDOW / 2
      drawCircle(color, radius, Offset(xOf(x), yOf(wave.height(mode, other, x, t), top)))
    }
  }
}

@Composable
private fun WaveExplanation(wave: Wave, other: Wave, mode: WaveMode, format: (Double) -> String) {
  val a = format(wave.amplitude)
  val l = format(wave.wavelength)
  val f = format(wave.frequency)
  val k = format(wave.waveNumber)
  val w = format(wave.angularFrequency)
  Card(stringResource(Res.string.projectile_explain_title)) {
    Step(
      Res.string.waves_explain_basics,
      Res.string.waves_explain_basics_text,
      "v = λ · f = $l · $f = ${format(wave.speed)} m/s",
      "T = 1 / f = ${format(wave.period)} s",
    )
    Step(
      Res.string.waves_explain_motion,
      Res.string.waves_explain_motion_text,
      "k = 2π / λ = $k rad/m · ω = 2π · f = $w rad/s",
      "y(x, t) = A · sin(k · x − ω · t) = $a · sin($k · x − $w · t)",
    )
    if (mode == WaveMode.STANDING) {
      Step(
        Res.string.waves_explain_standing,
        Res.string.waves_explain_standing_text,
        "y(x, t) = 2A · sin(k · x) · cos(ω · t)",
        "λ / 2 = ${format(wave.wavelength / 2)} m",
      )
    }
    if (mode == WaveMode.BEATS) {
      Step(
        Res.string.waves_explain_beats,
        Res.string.waves_explain_beats_text,
        "λ₂ = v / f₂ = ${format(other.wavelength)} m",
        "f(beat) = |f − f₂| = |$f − ${format(other.frequency)}| = ${format(wave.beats(other))} Hz",
      )
    }
  }
}

/** Meters of string on the screen. */
private const val WINDOW = 10.0

private const val LOWEST_TOP = 2.4
private const val TOP_PER_AMPLITUDE = 2.2
private const val QUARTER = 4
private const val SAMPLES = 180
private const val LINE_WIDTH = 3f
private const val GHOST_WIDTH = 1.5f
private val SCENE_HEIGHT = 220.dp
private val MARK_RADIUS = 7.dp
