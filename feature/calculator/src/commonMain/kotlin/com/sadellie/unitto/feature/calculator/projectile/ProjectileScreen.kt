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

package com.sadellie.unitto.feature.calculator.projectile

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.sadellie.unitto.core.datastore.UserPreferencesRepository
import com.sadellie.unitto.core.ui.DrawerButton
import com.sadellie.unitto.core.ui.ScaffoldWithTopBar
import com.sadellie.unitto.feature.calculator.probability.rounded
import com.sadellie.unitto.feature.calculator.probability.sliderValue
import com.sadellie.unitto.feature.calculator.probability.toNumberOrNull
import kotlin.math.max
import kotlin.math.round
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import unitto.core.common.generated.resources.Res
import unitto.core.common.generated.resources.place_tab_moon
import unitto.core.common.generated.resources.projectile_angle
import unitto.core.common.generated.resources.projectile_earth
import unitto.core.common.generated.resources.projectile_explain_apex
import unitto.core.common.generated.resources.projectile_explain_apex_text
import unitto.core.common.generated.resources.projectile_explain_impact
import unitto.core.common.generated.resources.projectile_explain_impact_text
import unitto.core.common.generated.resources.projectile_explain_motion
import unitto.core.common.generated.resources.projectile_explain_motion_text
import unitto.core.common.generated.resources.projectile_explain_range
import unitto.core.common.generated.resources.projectile_explain_range_text
import unitto.core.common.generated.resources.projectile_explain_split
import unitto.core.common.generated.resources.projectile_explain_split_text
import unitto.core.common.generated.resources.projectile_explain_time
import unitto.core.common.generated.resources.projectile_explain_time_text
import unitto.core.common.generated.resources.projectile_explain_title
import unitto.core.common.generated.resources.projectile_gravity
import unitto.core.common.generated.resources.projectile_height
import unitto.core.common.generated.resources.projectile_hint
import unitto.core.common.generated.resources.projectile_initial
import unitto.core.common.generated.resources.projectile_invalid
import unitto.core.common.generated.resources.projectile_jupiter
import unitto.core.common.generated.resources.projectile_launch
import unitto.core.common.generated.resources.projectile_mars
import unitto.core.common.generated.resources.projectile_max_height
import unitto.core.common.generated.resources.projectile_range
import unitto.core.common.generated.resources.projectile_speed
import unitto.core.common.generated.resources.projectile_time
import unitto.core.common.generated.resources.projectile_title

/** One starting condition, typed or on its slider. */
private class Condition(
  val symbol: String,
  val unit: String,
  val label: StringResource,
  val range: ClosedFloatingPointRange<Double>,
  val step: Double,
)

private val SPEED = Condition("v₀", "m/s", Res.string.projectile_speed, 0.0..100.0, 0.5)
private val ANGLE = Condition("θ", "°", Res.string.projectile_angle, -90.0..90.0, 1.0)
private val HEIGHT = Condition("h₀", "m", Res.string.projectile_height, 0.0..100.0, 0.5)
private val GRAVITY = Condition("g", "m/s²", Res.string.projectile_gravity, 0.5..30.0, 0.01)

private val PLANETS =
  listOf(
    Res.string.projectile_earth to "9.81",
    Res.string.place_tab_moon to "1.62",
    Res.string.projectile_mars to "3.71",
    Res.string.projectile_jupiter to "24.79",
  )

/** A throw without air: the conditions, the flight it gives and how that is worked out. */
@Composable
internal fun ProjectileRoute(openDrawer: () -> Unit) {
  val preferences = koinInject<UserPreferencesRepository>()
  val symbols by
    remember(preferences) { preferences.calculatorPrefs.map { it.formatterSymbols.fractional } }
      .collectAsState(initial = ".")
  var speed by rememberSaveable { mutableStateOf("25") }
  var angle by rememberSaveable { mutableStateOf("50") }
  var height by rememberSaveable { mutableStateOf("5") }
  var gravity by rememberSaveable { mutableStateOf("9.81") }
  fun number(value: String) = value.toNumberOrNull() ?: Double.NaN
  val flight =
    remember(speed, angle, height, gravity) {
      Launch(number(speed), number(angle), number(height), number(gravity)).flight()
    }
  val format = { value: Double -> value.rounded(DECIMALS).replace(".", symbols) }

  ScaffoldWithTopBar(
    title = { Text(stringResource(Res.string.projectile_title)) },
    navigationIcon = { DrawerButton(onClick = openDrawer) },
    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
  ) { paddingValues ->
    val conditions: @Composable () -> Unit = {
      Card(stringResource(Res.string.projectile_initial)) {
        ConditionRow(SPEED, speed, symbols) { speed = it }
        ConditionRow(ANGLE, angle, symbols) { angle = it }
        ConditionRow(HEIGHT, height, symbols) { height = it }
        ConditionRow(GRAVITY, gravity, symbols) { gravity = it }
        PlanetChips(gravity) { gravity = it.replace(".", symbols) }
      }
    }
    val flightView: @Composable () -> Unit = {
      if (flight == null) {
        Text(stringResource(Res.string.projectile_invalid), color = MaterialTheme.colorScheme.error)
      } else {
        FlightCard(flight, format) { newSpeed, newAngle ->
          speed = (round(newSpeed.coerceIn(SPEED.range) * 2) / 2).rounded(1).replace(".", symbols)
          angle = round(newAngle.coerceIn(ANGLE.range)).rounded(0)
        }
      }
    }
    val explanation: @Composable () -> Unit = { flight?.let { Explanation(it, format) } }
    BoxWithConstraints(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
      val spacing = Arrangement.spacedBy(12.dp)
      if (maxWidth >= WIDE_LAYOUT_WIDTH) {
        Row(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp), horizontalArrangement = spacing) {
          Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = spacing) {
            flightView()
            conditions()
          }
          Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = spacing) {
            explanation()
          }
        }
      } else {
        Column(
          modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
          verticalArrangement = spacing,
        ) {
          flightView()
          conditions()
          explanation()
        }
      }
    }
  }
}

@Composable
private fun Card(title: String, content: @Composable () -> Unit) {
  Column(
    modifier =
      Modifier.fillMaxWidth().clip(CARD_SHAPE).background(MaterialTheme.colorScheme.surfaceContainer).padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
    content()
  }
}

/** The animation, the time slider and the answers. It plays when opened and on Launch. */
@Composable
private fun FlightCard(flight: Flight, format: (Double) -> String, onAimChange: (Double, Double) -> Unit) {
  val progress = remember { Animatable(0f) }
  val scope = rememberCoroutineScope()
  val duration = (flight.time * MILLIS_PER_SECOND).toInt().coerceIn(MIN_ANIMATION, MAX_ANIMATION)
  fun play() {
    scope.launch {
      progress.snapTo(0f)
      progress.animateTo(1f, tween(duration, easing = LinearEasing))
      progress.animateTo(1f + LANDING_PART, tween(LANDING_MILLIS, easing = LinearEasing))
    }
  }
  LaunchedEffect(Unit) { play() }
  // New conditions show their whole flight at once
  LaunchedEffect(flight.launch) { if (progress.value > 0f) progress.snapTo(1f + LANDING_PART) }

  Column(
    modifier =
      Modifier.fillMaxWidth().clip(CARD_SHAPE).background(MaterialTheme.colorScheme.surfaceContainer).padding(12.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    ProjectileScene(Modifier.fillMaxWidth().height(SCENE_HEIGHT), flight, progress.value, format, onAimChange)
    Text(
      stringResource(Res.string.projectile_hint),
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    val time = flight.time * progress.value.coerceAtMost(1f)
    Text(
      "t = ${format(time)} s · x = ${format(flight.x(time))} m · y = ${format(flight.y(time))} m · v = ${format(flight.speed(time))} m/s",
      style = MaterialTheme.typography.labelMedium,
    )
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      Slider(
        modifier = Modifier.weight(1f),
        value = progress.value.coerceAtMost(1f),
        onValueChange = { scope.launch { progress.snapTo(it) } },
      )
      Button(onClick = ::play) { Text(stringResource(Res.string.projectile_launch)) }
    }
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      Answer(Modifier.weight(1f), Res.string.projectile_range, "${format(flight.range)} m")
      Answer(Modifier.weight(1f), Res.string.projectile_max_height, "${format(flight.apexHeight)} m")
      Answer(Modifier.weight(1f), Res.string.projectile_time, "${format(flight.time)} s")
    }
  }
}

@Composable
private fun Answer(modifier: Modifier, label: StringResource, value: String) {
  val color = MaterialTheme.colorScheme.onPrimaryContainer
  Column(modifier.clip(TILE_SHAPE).background(MaterialTheme.colorScheme.primaryContainer).padding(10.dp)) {
    Text(stringResource(label), style = MaterialTheme.typography.labelSmall, color = color, maxLines = 1)
    Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = color, maxLines = 1)
  }
}

/** Name, typing box and slider. A typed number out of the usual range stays on the slider. */
@Composable
private fun ConditionRow(condition: Condition, value: String, fractional: String, onValueChange: (String) -> Unit) {
  val number = value.toNumberOrNull()
  val start = condition.range.start
  val end = max(condition.range.endInclusive, number ?: 0.0)
  Column {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Text(
        condition.symbol,
        modifier = Modifier.width(SYMBOL_WIDTH),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
      )
      Text(stringResource(condition.label), modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
      OutlinedTextField(
        modifier = Modifier.width(FIELD_WIDTH),
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        isError = number == null,
        suffix = { Text(condition.unit) },
        textStyle = MaterialTheme.typography.bodyMedium,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
      )
    }
    Slider(
      value = (number ?: start).coerceIn(start, end).toFloat(),
      onValueChange = { onValueChange(sliderValue(it.toDouble(), condition.step, fractional)) },
      valueRange = start.toFloat()..end.toFloat(),
    )
  }
}

@Composable
private fun PlanetChips(gravity: String, onChoose: (String) -> Unit) {
  Row(
    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    PLANETS.forEach { (name, value) ->
      FilterChip(
        selected = gravity.toNumberOrNull() == value.toDouble(),
        onClick = { onChoose(value) },
        label = { Text(stringResource(name)) },
      )
    }
  }
}

/** Every step of the calculation with the numbers of this flight. */
@Composable
private fun Explanation(flight: Flight, format: (Double) -> String) {
  val launch = flight.launch
  val g = format(launch.gravity)
  val h = format(launch.height)
  val vx = format(flight.vx)
  val vy = format(flight.vy)
  val v = format(launch.speed)
  val angle = format(launch.angle)
  Card(stringResource(Res.string.projectile_explain_title)) {
    Step(
      Res.string.projectile_explain_split,
      Res.string.projectile_explain_split_text,
      "vx = v₀ · cos θ = $v · cos $angle° = $vx m/s",
      "vy = v₀ · sin θ = $v · sin $angle° = $vy m/s",
    )
    Step(
      Res.string.projectile_explain_motion,
      Res.string.projectile_explain_motion_text,
      "x(t) = vx · t = $vx · t",
      "y(t) = h₀ + vy · t − ½ · g · t² = $h + $vy · t − ½ · $g · t²",
    )
    Step(
      Res.string.projectile_explain_time,
      Res.string.projectile_explain_time_text,
      "t = (vy + √(vy² + 2 · g · h₀)) / g",
      "t = ($vy + √($vy² + 2 · $g · $h)) / $g = ${format(flight.time)} s",
    )
    Step(
      Res.string.projectile_explain_range,
      Res.string.projectile_explain_range_text,
      "x = vx · t = $vx · ${format(flight.time)} = ${format(flight.range)} m",
    )
    Step(
      Res.string.projectile_explain_apex,
      Res.string.projectile_explain_apex_text,
      "t = vy / g = $vy / $g = ${format(flight.apexTime)} s",
      "y = ${format(flight.y(flight.apexTime))} m",
    )
    Step(
      Res.string.projectile_explain_impact,
      Res.string.projectile_explain_impact_text,
      "v = √(vx² + vy²) = ${format(flight.impactSpeed)} m/s",
      "θ = ${format(flight.impactAngle)}°",
    )
  }
}

@Composable
private fun Step(title: StringResource, text: StringResource, vararg formulas: String) {
  Column(modifier = Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
    Text(stringResource(title), style = MaterialTheme.typography.titleSmall)
    Text(
      stringResource(text),
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    formulas.forEach { formula ->
      Text(
        formula,
        modifier =
          Modifier.fillMaxWidth().clip(TILE_SHAPE).background(MaterialTheme.colorScheme.surfaceContainerHighest).padding(8.dp),
        style = MaterialTheme.typography.bodyMedium,
      )
    }
  }
}

private const val DECIMALS = 2
private const val MILLIS_PER_SECOND = 1000
private const val MIN_ANIMATION = 1500
private const val MAX_ANIMATION = 6000
private const val LANDING_MILLIS = 450
private val SCENE_HEIGHT = 260.dp
private val WIDE_LAYOUT_WIDTH = 720.dp
private val FIELD_WIDTH = 132.dp
private val SYMBOL_WIDTH = 36.dp
private val CARD_SHAPE = RoundedCornerShape(24.dp)
private val TILE_SHAPE = RoundedCornerShape(12.dp)
